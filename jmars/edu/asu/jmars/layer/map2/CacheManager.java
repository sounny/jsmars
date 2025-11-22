package edu.asu.jmars.layer.map2;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import org.apache.commons.collections.ReferenceMap;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

/**
 * We have a separate thread pool for fuzzy tiles because we don't want to ever
 * queue up an actual cache hit behind a check for fuzzy tiles.
 */
public class CacheManager {
	private static final int NUM_TILE_THREADS = Config.get("map.cache.tileThreadCount", 10);	
	private static final int NUM_FUZZY_THREADS = Config.get("map.cache.fuzzyThreadCount", 5);
	
	private static final DebugLog log = DebugLog.instance();
	/** Helper log methods so the user must only declare one DebugLog line */
	private static final void log(String msg) {
		log.println(msg);
	}
	/** Helper log methods so the user must only declare one DebugLog line */
	private static final void log(Throwable e) {
		log.println(e);
	}
	
	private static String cacheDir = Main.getJMarsPath() + "cache" + File.separator;
	public static String getCacheDir() {
		return cacheDir;
	}
	public static void setCacheDir(String cacheDir) {
		CacheManager.cacheDir = cacheDir;
	}
	
	private static ExecutorService pool;
	private static ExecutorService fuzzyPool;
	static Map<String,BufferedImage> memoryCache;
	
	static {
		memoryCache = new ReferenceMap (ReferenceMap.SOFT, ReferenceMap.SOFT);
		pool = Executors.newFixedThreadPool(NUM_TILE_THREADS, new MapThreadFactory("Tile Cache Loader"));
		fuzzyPool = Executors.newFixedThreadPool(NUM_FUZZY_THREADS, new MapThreadFactory("Fuzzy Tile Cache Loader"));
	}
	
	private static void addTileToMemCache(String key, BufferedImage tile) {
		synchronized (memoryCache) {
			memoryCache.put(key, tile);
		}
	}
	
	private static BufferedImage getTileFromMemCache(String key) {
		BufferedImage tile = null;
		
		synchronized (memoryCache) {
			if (memoryCache.containsKey(key)) {
				tile = memoryCache.get(key);
				if (tile == null) {
					log("Tile is null! Total memory is: " + 
							Runtime.getRuntime().maxMemory() +
							" Free memory is: " + Runtime.getRuntime().freeMemory() +
					" Max - total is: " + (Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory())		
					);
				}
			}
		}
		
		return tile;
	}
	
	private static String getTileName(String sourceName, ProjObj projection, int ppd) {
		Point2D up = Util.getJmars1Up((Projection_OC)projection, null);
		// Windows doesn't allow : in filenames, so we use JMARS_1 instead of JMARS:1
		String projectionName = "JMARS_1,"+up.getX()+","+up.getY();
		return cacheDir+sourceName+"/"+projectionName+"/"+ppd+"ppd/";
	}
	
	private static String getTileName(String sourceName, ProjObj projection, int ppd, int xTile, int yTile, boolean isNumeric) {
		return getTileName(sourceName, projection, ppd) +xTile+"x"+yTile+(isNumeric?".vic":".png");
	}
	
	public static void getTiles(MapRetriever retriever, MapTile tiles[]) {
		if (tiles==null || tiles.length==0) {
			return;
		}
		
		for (int i = 0; i<tiles.length; i++) {
			pool.execute(new TileLoader(retriever, tiles[i]));
		}
	}
	
	public static void getFuzzyTiles(MapRetriever retriever, MapTile tiles[]) {
		if (tiles==null || tiles.length==0) {
			return;
		}
		
		for (int i = 0; i<tiles.length; i++) {
			fuzzyPool.execute(new FuzzyTileLoader(retriever, tiles[i]));
		}
	}
	
	// TODO: move this to a more appropriate class!
	// TODO: determine why AffineTransformOp does not work
	public static BufferedImage scaleImage(BufferedImage image, double scale){
		WritableRaster inRaster = image.getRaster();
		
		int w = (int)Math.round(image.getWidth() * scale);
		int h = (int)Math.round(image.getHeight() * scale);
		WritableRaster outRaster = inRaster.createCompatibleWritableRaster(w, h);
		
		Object outData = null;
		for(int j=0; j<h; j++){
			for(int i=0; i<w; i++){
				outData = inRaster.getDataElements((int)(i/scale), (int)(j/scale), outData);
				outRaster.setDataElements(i, j, outData);
			}
		}
		
		BufferedImage outImage = new BufferedImage(image.getColorModel(), outRaster, image.isAlphaPremultiplied(), null);
		return outImage;
	}
	
	public static MapTile[][] checkCache(MapTile tiles[]) {
		if (tiles==null || tiles.length==0) {
			return new MapTile[2][0];
		}
		Vector<MapTile> cachedTiles = new Vector<MapTile>(tiles.length);
		Vector<MapTile> nonCachedTiles = new Vector<MapTile>(tiles.length);

		MapSource source = tiles[0].getRequest().getSource();
		
		String sourceName = source.getName();
		ProjObj proj = tiles[0].getRequest().getProjection();
		int ppd = tiles[0].getRequest().getPPD();
		
		for (int i=0; i<tiles.length; i++) {
			File tileFile = new File(getTileName(sourceName, proj, ppd, tiles[i].getXtile(), tiles[i].getYtile(), source.hasNumericKeyword()));
			if (tileFile.canRead()) {
				cachedTiles.add(tiles[i]);
			} else {
				nonCachedTiles.add(tiles[i]);
			}
		}
		
		MapTile checkedTiles[][]=new MapTile[2][];
		
		checkedTiles[0]=(MapTile[])cachedTiles.toArray(new MapTile[cachedTiles.size()]);
		checkedTiles[1]=(MapTile[])nonCachedTiles.toArray(new MapTile[nonCachedTiles.size()]);
		
		return checkedTiles;
	}
	
	
	public static BufferedImage getTile(MapSource source, String tileName) {		
		BufferedImage tile = null;
		
		tile = CacheManager.getTileFromMemCache(tileName);
		
		if (tile!=null) {
			return tile;
		}

		log("Memory cache miss!");

		
		File tileFile = new File(tileName);
		
		// We've technically already done the check for whether the file exists/can be read already
		// prior to this point... but testing shows that we can usually perform 30 of these checks
		// in a few milliseconds, regardless of whether we're reading from local disk, CD, or netapp,
		// so it shouldn't hurt to do this check a second time.
		if (tileFile.canRead()) {
			for (int i=0; i<3; i++) {
				try {
					if (source.hasNumericKeyword()) {
						tile = MyVicarReaderWriter.read(tileFile);
					} else {
						tile = ImageIO.read(tileFile);
						tile = Util.replaceWithLinearGrayCS(tile);
					}
					
					if (tile==null) {
						//System.out.println("TILE " + xtile + ":" +ytile + " at " + ppd + " ppd is null!");
						return null;
					}							
				} catch (Exception e) {
					log("Exception loading tile: " + tileName);
					log("Retrying...");
				}
				break;
			}
		} else { // tile not found
			//System.out.println("TILE " + xtile + ":" +ytile + " at " + ppd + " ppd not found!");					
		}

		if (tile!=null) {
			synchronized (memoryCache) {
				log("Memory Cache size prePut = " + memoryCache.size());
				CacheManager.addTileToMemCache(tileName, tile);
				log("Memory Cache size postPut = " + memoryCache.size());
			}
		}
		
		return tile;
	}
	
	public static void storeMapData(MapTile tile) {
		MapSource source = tile.getRequest().getSource();
		String sourceName = source.getName();

		// Occasionally the netapp will report back that a file or directory doesn't exist, despite the
		// fact that we've explicitly just created it.  Because of this occasional error, we attempt
		// the write as many as three times before actually giving up.  This seems to solve this 
		// particular rare issue with the netapp.
		for (int i=0; i<3; i++) {
			createDirectories(sourceName,tile.getRequest().getProjection(), tile.getRequest().getPPD());

			String tileName = getTileName(
				sourceName,
				tile.getRequest().getProjection(),
				tile.getRequest().getPPD(),
				tile.getXtile(),
				tile.getYtile(),
				source.hasNumericKeyword());
			
			File tileFile = new File(tileName);
		
			try {
				if (tileFile.createNewFile()) { // if already existed, assume we don't need to recache
					if (source.hasNumericKeyword()) {
						MyVicarReaderWriter.write(tile.getImage(), tileFile);
					} else {
						ImageIO.write(tile.getImage(), "PNG", tileFile);
					}
					
					CacheManager.addTileToMemCache(tileName, tile.getImage());
				}
				break;
			} catch (Exception e) {
				log(e);
			}
			log("Retrying tile storage....");
		}		
	}
	
	private static void createDirectories(String sourceName, ProjObj projection, int ppd) {
		try {
			new File(getTileName(sourceName, projection, ppd)).mkdirs();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/** Removes all tiles for the given map source from the memory and disk caches */
	public static void removeMap(MapSource source) {
		synchronized(memoryCache) {
			memoryCache.clear();
		}
		Util.recursiveRemoveDir(new File(cacheDir + source.getName()));
	}
	
	private static class TileLoader implements Runnable {
		MapRetriever myRetriever = null;
		MapTile tile = null;
		
		TileLoader(MapRetriever retriever, MapTile mapTile) {
			myRetriever=retriever;
			tile=mapTile;
		}
		
		public void run() {
			fetchTile();
		}
		
		private void fetchTile() {
			if (tile.getRequest().isCancelled()) {
				return;
			}
			
			MapSource source = tile.getRequest().getSource();
			ProjObj proj = tile.getRequest().getProjection();
			int ppd = tile.getRequest().getPPD();
			
			String tileName = CacheManager.getTileName(source.getName(), proj, ppd, tile.getXtile(), tile.getYtile(), source.hasNumericKeyword());
			
			BufferedImage tileImage = CacheManager.getTile(source, tileName);
			
			// This image MAY be null, it is MapRetreivers job to check
			myRetriever.cacheResponse(tile, tileImage);
		}
	}
	
	private static class FuzzyTileLoader implements Runnable {
		MapRetriever myRetriever = null;
		MapTile tile = null;
		
		FuzzyTileLoader(MapRetriever retriever, MapTile mapTile) {
			myRetriever=retriever;
			tile=mapTile;
		}
		
		public void run() {
			fetchTile();
		}
		
		private void fetchTile() {		
			// If this request has been cancelled, or already fulfilled (downloadmanager was fast), don't
			// bother doing this work
			if (tile.getRequest().isCancelled() || tile.isFinal()) {
				return;
			}
			
			MapSource source = tile.getRequest().getSource();
			ProjObj proj = tile.getRequest().getProjection();
			final int ppd = tile.getRequest().getPPD();
			
			for (int fuzzyPPD = ppd/2 ; fuzzyPPD > 1 ; fuzzyPPD /= 2){				
				int ratio = ppd / fuzzyPPD;
				
				// A tile has 256 pixels on a side.
				// Using this method, we can't upscale data to be fuzzy if it's more than 256 times larger 
				// than the area we are looking to fill.  Upscaling data more than 16 times seems unlikely to provide
				// any real benefit to the user, so we will quit our search at this point
				if (ratio>16) break;
				
				String tileName = CacheManager.getTileName(
					source.getName(), proj, fuzzyPPD,
					tile.getXtile()/ratio, tile.getYtile()/ratio,
					source.hasNumericKeyword());
				
				BufferedImage tileImage = CacheManager.getTile(source, tileName);
				
				if (tileImage==null) {								
					continue;
				}
				
				int xtileindex = tile.getXtile() % ratio;
				int ytileindex = ratio - (tile.getYtile() % ratio) - 1;
				
				double fuzzyxstep = MapRetriever.tiler.getPixelWidth() / ratio;
				double fuzzyystep = MapRetriever.tiler.getPixelHeight() / ratio;
				
				try {
					tileImage = tileImage.getSubimage((int)(xtileindex * fuzzyxstep), (int)(ytileindex * fuzzyystep), (int)fuzzyxstep, (int)fuzzyystep);
				} catch (Exception e) {
					log(e);
					log("xtileindex:"+xtileindex+" fuzzyxstep:"+fuzzyxstep+" ytileindex:"+ytileindex+" fuzzyystep:"+fuzzyystep);
					break;
				}
				
				long outputImageSize = (tileImage.getWidth()*ratio)*(tileImage.getHeight()*ratio);
				if (outputImageSize <= 0 || outputImageSize > Integer.MAX_VALUE){
					log("Scaling tile from "+fuzzyPPD+" to "+ppd+" will result in image size overflow."+
							" Stopping further search for lower PPDs for this tile.");
					break;
				}
				
				tileImage = CacheManager.scaleImage(tileImage, ratio);
				
				myRetriever.fuzzyResponse(tile, tileImage);
			}						
		}
	}
}
