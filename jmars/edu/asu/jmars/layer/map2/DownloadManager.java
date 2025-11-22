package edu.asu.jmars.layer.map2;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;

/**
 * <p>
 * This class manages pairs of map tiles and the map retriever that will receive
 * the data. The tile/receiver pairs are put in a shared list and a pool of
 * threads pulls each tile, calls the map source's fetchTile method, and sends
 * the result (image or error) back to the receiver.
 * 
 * <p>
 * Each tile request will immediately start a new download thread, unless there
 * are already as many download threads as the {@link MapServer} allows. In that
 * case the tile is added to the schedule and one of the existing download
 * threads will eventually get to it.
 * 
 * <p>
 * Within a map server, the requests are round robin scheduled on the map
 * source. This provides a more even filling in the of the map views, especially
 * with composite maps which typically won't display a tile until the underlying
 * tiles for all inputs have arrived.
 * 
 * <p>
 * A request for a tile while that tile is already downloading will not start a
 * new download; rather both requests will wait on the single download and the
 * data will be sent to both at the end. This greatly improves the performance
 * of numeric layers where both viewing and plotting at the same ppd is common.
 * 
 * <p>
 * New requests are put at the top of the source schedule. This provides a more
 * responsive user interface in the common case of a large download for the main
 * and panner views needing to be superseded for something small happening in
 * another part of the interface, like the advanced dialog.
 * 
 * <p>
 * The download threads will wait forever for a given request to finish, with
 * the exception that when a new request is made by calling
 * {@link #addDownload(MapRetriever, MapTile)}, all servers are checked for any
 * responses within the server timeout, and if the server does appear to have
 * stalled, that server's download threads are interrupted, and all remaining
 * previously scheduled receivers should get a failure notice. This cleaning
 * policy provides something similar to a TCP SO timeout, but over all downloads
 * to that server instead of one stream.
 */
public class DownloadManager {
	private DebugLog log = DebugLog.instance();
    /** Provides the number of times to retry retryable exceptions when downloading tiles */
	private static final int DOWNLOAD_RETRIES = Config.get("map.download.retries", 3);
	/** Provides a pool of threads for concurrent downloading */
	private ExecutorService pool = Executors.newCachedThreadPool(new MapThreadFactory("Tile Downloader"));
	/** Tiles and destinations to download, head of list is next source to download */
	private Map<String,Server> servers = new HashMap<String,Server>();
	/** Total requests made so far */
	private int requests = 0;
	/** Total responses delivered so far */
	private int responses = 0;
	/** Last time the cleanup method was called */
	private long lastClean = System.currentTimeMillis();
	
	/** Request a tile be downloaded and sent to the indicated receiver. */
	public synchronized void addDownload(MapRetriever receiver, MapTile mapTile) {
		// trigger a clean each time we get a new request
		cleanupServers();
		
		// get or create Server
		MapServer mapServer = mapTile.getRequest().getSource().getServer();
		String host = mapServer.getMapURI().toString();
		final Server server;
		if (servers.containsKey(host)) {
			server = servers.get(host);
		} else {
			server = new Server(host);
			log.println(MessageFormat.format("Creating server {0}", server.hashCode()));
			servers.put(host, server);
		}
		
		// create new Source or remove existing one from server
		MapSource mapSource = mapTile.getTileRequest().getSource();
		Source source = null;
		for (Source src: server.sources) {
			if (src.source.equals(mapSource)) {
				// if found, reuse it
				source = src;
				server.sources.remove(src);
				break;
			}
		}
		if (source == null) {
			// if not found, create new source
			source = new Source(server, mapSource);
			log.println(MessageFormat.format("Creating source {0}", source.hashCode()));
		}
		
		// insert the Source at the head of the schedule
		server.sources.add(0, source);
		
		// get or create tile, destroying stale tiles rather than join the wait
		// since that case commonly never completes or times out after a very
		// long time
		Tile tile = source.tiles.get(mapTile);
		if (tile == null) {
			// create new tile
			source.tiles.put(mapTile, tile = new Tile(source, mapTile));
			log.println(MessageFormat.format("Creating tile {0}", tile.hashCode()));
		}
		
		// add to request queue; the MapTile is part of each map entry
		// because the tiles can be canceled individually
		tile.requests.add(new Request(mapTile, receiver));
		log.println(MessageFormat.format("Tile {0} got request {1}", tile.hashCode(), mapTile.hashCode()));
		
		// add a tile for this server, and if we're under the parallelism
		// limit, add a new thread for the server
		if (server.futures.size() < mapServer.getMaxRequests()) {
			DownloadThread thread = new DownloadThread(server);
			Future<?> future = pool.submit(thread);
			server.futures.add(future);
			thread.setFuture(future);
		}
		
		requests ++;
	}
	
	/**
	 * Clean up servers that have had no activity on any download thread within
	 * the timeout duration, at most once per second
	 */
	private synchronized void cleanupServers() {
		long now = System.currentTimeMillis();
		if (now - lastClean > 1000) {
			lastClean = now;
			for (Server server: new ArrayList<Server>(servers.values())) {
				// a server with no requests has a zero timeout so it is always
				// eligible for cleaning
				if (now - server.lastUpdate > server.getLongestTimeout()) {
					// cancel all threads for the server if we can
					boolean all = true;
					for (Future<?> f: server.futures) {
						all &= f.cancel(true);
					}
					if (!all) {
						// as per Future.cancel(boolean), seeing this message
						// could mean all is normal, as long as it isn't coming
						// up often
						log.println(MessageFormat.format("Unable to cancel all threads for server {0}", server.host));
					}
					// actually remove the server data structures
					if (servers.remove(server.host) != server) {
						log.println(MessageFormat.format("Error removing server {0} from the list of servers", server.host));
					}
				}
			}
		}
	}
	
	/**
	 * Each instance of this Runnable runs in the {@link DownloadManager}'s
	 * thread pool, and is guaranteed to continue running until both the
	 * download operation has finished, and {@link #setFuture(Future)} has been
	 * called so this thread can clean up after itself.
	 */
	private class DownloadThread implements Runnable {
		private final Server server;
		private Future<?> future = null;
		private final int thread;
		private final Semaphore sem = new Semaphore(1, false);
		/**
		 * Create a new thread with the given server; note that execution of this thread
		 * @param server
		 */
		public DownloadThread(Server server) {
			this.server = server;
			// guaranteed to succeed right away
			sem.acquireUninterruptibly();
			synchronized(DownloadManager.this) {
				thread = server.nextThreadID++;
			}
		}
		/**
		 * Called on the {@link DownloadManager} submit thread to inform this
		 * pool thread of its own Future so it can clean up after itself
		 */
		public void setFuture(Future<?> future) {
			this.future = future;
			sem.release();
		}
		public void run () {
			try {
				log.println(MessageFormat.format("Server {0} thread {1} starting", server.host, thread));
				while(true) {
					Tile tile = getNextTile(server);
					if (tile == null) {
						break;
					} else {
						log.println(MessageFormat.format("Server {0} thread {1} getting tile {2}", server.host, thread, tile.hashCode()));
						processTile(tile);
					}
					synchronized(DownloadManager.this) {
						server.lastUpdate = System.currentTimeMillis();
					}
				}
			} finally {
				// guaranteed to block until after the 'future' field has been set
				sem.acquireUninterruptibly();
				synchronized(DownloadManager.this) {
					server.futures.remove(future);
				}
				log.println(MessageFormat.format("Server {0} thread {1} closing", server.host, thread));
			}
		}
	}
	
	/**
	 * Synchronously gets the next tile, asynchronously downloads the tile,
	 * synchronously cleans up after the tile and gets the list of receivers,
	 * and asynchronously dispatches the tile to registered receivers.
	 * 
	 * Called on a download pool thread.
	 */
	private void processTile(Tile tile) {
		// if nothing needs this tile yet, then cleanup instead of downloading
		boolean active;
		synchronized(this) {
			active = tile.isTileActive();
			if (! active) {
				cleanup(tile);
			}
		}
		
		if (active) {
			long start = System.currentTimeMillis();
			downloadTile(tile);
			log.println(MessageFormat.format("Tile {0} downloaded in {1} ms", tile.hashCode(), System.currentTimeMillis()-start));
			
			// synchronously clean up data structures AFTER downloading, to
			// maximize the amount of time other requests have to join the
			// receiver list
			cleanup(tile);
		}
		
		// respond to each request by sending an empty tile if cancelled,
		// otherwise copying the downloaded tile into the request tile
		log.println(MessageFormat.format("Tile {0} has {1} requests active", tile.hashCode(), tile.requests.size()));
		for (Request req: tile.requests) {
			if (req.tile.getRequest().isCancelled()) {
				log.println(MessageFormat.format("Request {0} canceled during download", req.tile.hashCode()));
			} else {
				req.tile.setImage(tile.tile.getImage());
				req.tile.setException(tile.tile.getException());
			}
			
			log.println(MessageFormat.format("Sending tile {0} in response to request {1}", tile.hashCode(), req.tile.hashCode()));
			req.receiver.downloadResponse(req.tile);
		}
		
		log.println(MessageFormat.format("Processed {0} requests and {1} responses", requests, responses));
	}
	
	/**
	 * Returns the first non-loading tile for this server, scheduling the
	 * sources in round-robin fashion.
	 * 
	 * Called on a download pool thread.
	 */
	private synchronized Tile getNextTile(Server server) {
		// mark the first non-loading tile as loading, move the source to the
		// tail of the schedule, and return the tile
		for (Source source: server.sources) {
			for (Tile tile: source.tiles.values()) {
				if (! tile.loading) {
					tile.loading = true;
					server.sources.remove(source);
					server.sources.add(source);
					return tile;
				}
			}
		}
		// or return null if there are no tiles, or all tiles are loading
		return null;
	}
	
	/**
	 * Downloads the given tile, sets the image or exception onto the given
	 * Tile, and updates the server's lastUpdate field when the process is done
	 */
	private void downloadTile(Tile tile) {
		MapTile mapTile = tile.tile;
		try {
			final MapRequest tileRequest = mapTile.getTileRequest();
			for (int i = 0; i < DOWNLOAD_RETRIES; i++) {
				try {
					mapTile.setImage(tileRequest.getSource().fetchTile(tileRequest));
					mapTile.setException(null);
					break;
				} catch (RetryableException re) {
					mapTile.setException(re);
					log.println(MessageFormat.format(
						"Retryable failure {0} of {1} downloading tile {2}: {3}",
						i+1, DOWNLOAD_RETRIES, tile.hashCode(), re.getMessage()));
				}
			}
		} catch (Exception ex) {
			mapTile.setException(ex);
			log.println(MessageFormat.format("Unretryable failure downloading tile {0}: {1}",
				tile.hashCode(), ex.getMessage()));
			log.println(ex);
		}
	}
	
	/**
	 * Cleans up the tile, the tile's containing source if empty, and the
	 * source's containing server if empty. Once this method has been called,
	 * the requests on a tile are disconnected from further calls to
	 * {@link #addDownload(MapRetriever, MapTile)}, which allows safely
	 * dispatching the download results to the receivers.
	 * 
	 * Called on a download pool thread.
	 */
	private synchronized void cleanup(Tile tile) {
		tile.source.tiles.remove(tile.tile);
		log.println(MessageFormat.format("Tile {0} removed from source {1}", tile.hashCode(), tile.source.hashCode()));
		
		if (tile.source.tiles.size() == 0) {
			tile.source.server.sources.remove(tile.source);
			log.println(MessageFormat.format("Removing empty source {0} from server {1}", tile.source.hashCode(), tile.source.server.hashCode()));
		}
		
		if (tile.source.server.sources.size() == 0) {
			servers.remove(tile.source.server.host);
			log.println(MessageFormat.format("Removing empty server {0}", tile.source.server.hashCode()));
		}
		
		responses += tile.requests.size();
	}
	
	/** Each server has a number of threads that pull tiles in round robin fashion from a list of sources */
	private static class Server {
		public int nextThreadID = 0;
		public final List<Future<?>> futures = new LinkedList<Future<?>>();
		public final String host;
		public final List<Source> sources = new LinkedList<Source>();
		public long lastUpdate = System.currentTimeMillis();
		public Server(String host) {
			this.host = host;
		}
		/**
		 * Returns the longest timeout of any {@link MapServer} from any
		 * {@link MapRequest}, or 0 if there are no requests
		 */
		public long getLongestTimeout() {
			MapServer longest = null;
			for (Source source: sources) {
				MapServer mapServer = source.source.getServer();
				if (longest == null || mapServer.getTimeout() > longest.getTimeout()) {
					longest = mapServer;
				}
			}
			return longest == null ? 0 : longest.getTimeout();
		}
	}
	
	/** Each source's tiles */
	private static class Source {
		public final Server server;
		public final MapSource source;
		public final Map<MapTile,Tile> tiles = new LinkedHashMap<MapTile,Tile>();
		public Source(Server server, MapSource source) {
			this.server = server;
			this.source = source;
		}
	}
	
	/**
	 * Preserves the association between the original MapTile and the original
	 * retriever, since the original tile's cancel flag will only be for the
	 * first request for that tile.
	 */
	private static class Tile {
		public final Source source;
		public final MapTile tile;
		public boolean loading = false;
		public final List<Request> requests = new LinkedList<Request>();
		public Tile(Source source, MapTile tile) {
			this.source = source;
			this.tile = tile;
		}
		/**
		 * Returns true if at least one request is still active, false if all
		 * requests have been canceled or there are no requests
		 */
		public boolean isTileActive() {
			for (Request req: requests) {
				if (! req.tile.getRequest().isCancelled()) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * Pairs the map tile with the receiver of the results
	 */
	private static class Request {
		public final MapTile tile;
		public final MapRetriever receiver;
		public Request(MapTile tile, MapRetriever retriever) {
			this.tile = tile;
			this.receiver = retriever;
		}
	}
}
