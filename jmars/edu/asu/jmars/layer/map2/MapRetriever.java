package edu.asu.jmars.layer.map2;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

/**
 * Receives and processes MapData requests by filling in the Image property.
 * If an error occurs, the MapData image will be null.
 * If an intermediate update occurs, image will be non-null and the finished flag will be clear.
 * If a final update occurs, image will be non-null and the finished flag will be set.
 */
public class MapRetriever implements Runnable {
	public static final WrappedWorldTiler tiler = new WrappedWorldTiler(256,256);
	private static final int NUM_RETRIEVER_THREADS = Config.get("map.retriever.threadCount", 5);
	private static ExecutorService pool = Executors.newFixedThreadPool(NUM_RETRIEVER_THREADS, new MapThreadFactory("Map Retriever"));
	private static DebugLog log = DebugLog.instance();
	private static DownloadManager dman = new DownloadManager();
	
	private final MapRequest originalRequest;
	private final MapData fetchedData;
	private Collection<MapTile> unFinishedMapTiles;
	private Collection<MapTile> cachedMapTiles;
	private Collection<MapTile> nonCachedMapTiles;
	private MapTile requestTiles[];
	private boolean finishedDataSent=false;
	private int downloadedTilesReceived=0;
	private MapProcessor receiver;
	
	public MapRetriever(MapRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Request must not be null");
		}
		originalRequest = request;
		if (request.getSource().getMaxPPD() < request.getPPD()) {
			// request data at not more than twice the resolution of the dataset
			int maxPPD = 1 << Math.max(0, (int)Math.ceil(Math.log(request.getSource().getMaxPPD()) / Math.log(2)));
			request = new MapRequest(request.getSource(), request.getExtent(), maxPPD, request.getProjection());
		}
		
		fetchedData = new MapData(request);
	}
	
	public void setReceiver(MapProcessor recv) {
		receiver = recv;
		pool.execute(this);
	}
	
	/*
	 * Step 1: Ask CacheManager what tiles are cached
	 * Step 2: In parallel: Ask CacheManager for cached tiles
	 *                      Ask CacheManager for fuzzy non-cached tiles
	 *                      Ask DownloadManager for non-cached tiles
	 * Step 3: Track status of CacheManager and DownloadManager
	 *    As tiles are returned, notify MapProcessor
	 * Step 4: When CacheManager done, if not all cached tiles were retrieved, request them from DLManager
	 * Step 5: When DownloadManager done, send any successfully downloaded tiles to CacheManager so they
	 *    can be written to disk
	 * Step 6: Profit
	 * 
	 */
	
	/** Calculates tiles from a map request, shifting each tile's extent by the request source's map offset */
	public static Set<MapTile> createTiles(MapRequest request) {
		// Apply the offset (defaults to 0.0) to handle nudged maps
		Point2D offset = request.getSource().getOffset();
		Rectangle2D extent = request.getExtent();
		Rectangle2D offsetExtent = new Rectangle2D.Double(
			extent.getMinX() + offset.getX(),
			extent.getMinY() + offset.getY(),
			extent.getWidth(), extent.getHeight());
		
		// Create unique set of tiles that cover each wrapped rectangle in the
		// request's unwrapped extent
		int ppd = request.getPPD();
		Set<MapTile> tiles = new LinkedHashSet<MapTile>();
		for (Rectangle2D wrappedOffsetExtent: Util.toWrappedWorld(offsetExtent)) {
			for (Point tilePoint: tiler.getTiles(wrappedOffsetExtent, ppd)) {
				Rectangle2D tileExtent = MapRetriever.tiler.getExtent(tilePoint, ppd);
				MapRequest tileRequest = new MapRequest(request.getSource(), tileExtent, ppd, request.getProjection());
				tiles.add(new MapTile(request, tileRequest, tilePoint));
			}
		}
		
		return tiles;
	}
	
	private MapTile[] getIncompleteTiles() {
		Vector<MapTile> incompleteTiles = new Vector<MapTile>();
		
		for (int i = 0; i<requestTiles.length; i++) {
			if (requestTiles[i].isMissing() || requestTiles[i].isFuzzy()) {
				incompleteTiles.add(requestTiles[i]);
			}
		}
		
		if (incompleteTiles.size()==0)	return null;

		return (MapTile[])incompleteTiles.toArray(new MapTile[incompleteTiles.size()]);
	}
	
	private void fetchMapData() {
		requestTiles = createTiles(fetchedData.getRequest()).toArray(new MapTile[0]);
		
		MapTile tiles[]=getIncompleteTiles();
		
		MapTile checkedTiles[][]=CacheManager.checkCache(tiles);
		MapTile cachedTiles[]=checkedTiles[0];
		MapTile nonCachedTiles[]=checkedTiles[1];
		
		cachedMapTiles = new ArrayList<MapTile>(Arrays.asList(cachedTiles));
		nonCachedMapTiles = new ArrayList<MapTile>(Arrays.asList(nonCachedTiles));
		unFinishedMapTiles = new ArrayList<MapTile>(checkedTiles.length);
		unFinishedMapTiles.addAll(Arrays.asList(cachedTiles));
		unFinishedMapTiles.addAll(Arrays.asList(nonCachedTiles));
		
		CacheManager.getTiles(this, cachedTiles);
		CacheManager.getFuzzyTiles(this, nonCachedTiles);
		
		for (MapTile tile: nonCachedTiles) {
			dman.addDownload(this, tile);
		}
	}
	
	private boolean allTilesFetched() {
		return (cachedMapTiles.isEmpty() && nonCachedMapTiles.isEmpty());
	}
	
	public synchronized void cacheResponse(MapTile tile, BufferedImage image) {
		if (tile.getRequest().isCancelled()) {
			return;
		}
		
		if (cachedMapTiles.contains(tile)) {
			cachedMapTiles.remove(tile);
			
			if (image==null) {
				nonCachedMapTiles.add(tile);
				
				log.println("CacheManager returned a null image, trying to download image instead");
				
				dman.addDownload(this, tile);
				return;
			}
			
			tile.setImage(image);
			fetchedData.addTile(tile);
			
			if (cachedMapTiles.isEmpty()) {
				sendUpdate();
			}
		} else {
			log.aprintln("MapRetriever update error");
		}
	}
	
	public synchronized void fuzzyResponse(MapTile tile, BufferedImage image) {
		if (tile.getRequest().isCancelled()) {
			return;
		}
		
		if (nonCachedMapTiles.contains(tile)) {
			tile.setFuzzyImage(image);
			fetchedData.addTile(tile);

			sendUpdate();
		} else {
			// This could be fine - it just means we may have received the downloadResponse
			// first.  Should we separate these into two different outstanding queues
			// for ultimate clarity?
		}
	}
	
	public synchronized void downloadResponse(MapTile tile) {
		if (tile.getRequest().isCancelled()) {
			log.println("Received cancelled downloaded response");
			return;
		}
		
		if (nonCachedMapTiles.contains(tile)) {
			downloadedTilesReceived++;
			
			// specifically look at the final image, not the fuzzy one
			BufferedImage image = tile.getImage();
			
			if (image==null && !tile.hasError()) {
				// error
				log.aprintln("Null Tile in downloadResponse but not marked as an error");
				return;
			}
			
			nonCachedMapTiles.remove(tile);
			
			if (image!=null) {
				tile.setImage(image);				
				fetchedData.addTile(tile);
			}
			
			sendUpdate();
			
			if (image!=null && tile.getRequest().getSource().getMimeType() != null) {
				CacheManager.storeMapData(tile);
			}
		} else {
			log.println("Received downloaded tile not requested");
			throw new IllegalStateException("Receiver got tile it didn't ask for");
		}
	}
	
	/**
	 * Returns a copy of the data that is safe to modify without worry about
	 * subsequent updates from the cache or download systems, and ensures that
	 * the image is a component color model.
	 */
	public synchronized MapData getData() {
		MapData out = fetchedData.convertToCCM().convertToRequest(originalRequest);
		// ensure at least one copy is made
		if (out == fetchedData) {
			out = out.getDeepCopy();
		}
		return out;
	}
	
	public MapRequest getRequest() {
		return originalRequest;
	}
	
	private synchronized void sendUpdate() {
		if (finishedDataSent==true) {
			log.println("MapRetriever received an update after sending finished data!.");
			return;
		}
		
		if (originalRequest.isCancelled()) {
			return;
		}
		
		if (allTilesFetched()) {
			if (!fetchedData.isFinished()) {
				log.println("Setting data to finished");	
				fetchedData.setFinished(true);
			}
		}
		
		if (fetchedData.isFinished()) {
			finishedDataSent=true;
		}
		
		receiver.receiveUpdate();
	}
	
	public void run() {
		fetchMapData();
	}
}
