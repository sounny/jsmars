package edu.asu.jmars.layer.map2;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import edu.asu.jmars.ProjObj;

public class MapServerTest {
	private static class Arg {
		public final String name;
		public final String desc;
		public Object value;
		public Arg(String name, String desc, Object defaultValue) {
			this.name = name;
			this.desc = desc;
			this.value = defaultValue;
		}
	}
	private static class Bound extends Rectangle2D.Double {
		public static Bound parseBound(String value) {
			String[] bits = value.split(",");
			return new Bound(
				java.lang.Double.parseDouble(bits[0]),
				java.lang.Double.parseDouble(bits[1]),
				java.lang.Double.parseDouble(bits[2]),
				java.lang.Double.parseDouble(bits[3])
			);
		}
		public Bound(Rectangle2D rect) {
			this(rect.getMinX(), rect.getMinY(), rect.getMaxX(), rect.getMaxY());
		}
		public Bound(double x1, double y1, double x2, double y2) {
			super();
			double xmin = Math.min(x1,x2);
			double xmax = Math.max(x1,x2);
			double ymin = Math.min(y1,y2);
			double ymax = Math.max(y1,y2);
			setRect(xmin,ymin,xmax-xmin,ymax-ymin);
		}
		public String toString () {
			return MessageFormat.format(
				"{0,number,#.###},{1,number,#.###},{2,number,#.###},{3,number,#.###}",
				x, y, x+width, y+height);
		}
	}
	
	private Map<String,Arg> args = new LinkedHashMap<String,Arg>();
	{
		Arg[] arr = {
			new Arg("server","(required) URL of server to test","<none>"),
			new Arg("cache","Path to client tile cache directory, or <none> to not cache","<none>"),
			new Arg("timeout","ms to wait before timing out a request",10000),
			new Arg("maps","Comma-separated list of maps to test","<all>"),
			new Arg("clients","How many clients to simultaneously hammer at the server",50),
			new Arg("requests","Max requests per client, or -1 for no limit",5),
			new Arg("width","Width of each client's screen",1024),
			new Arg("height","Height of each client's screen",400),
			new Arg("view","Bounds of the view",new Bound(0,-90,360,90)),
			new Arg("proj","Bounds of the projection",new Bound(-180,-90,180,90)),
			new Arg("caps","Get capabilities and force -maps to be in map list",true),
			new Arg("seed","Randomizing seed - change this to change each client's requests",0),
		};
		for (Arg a: arr) {
			args.put(a.name, a);
		}
	}
	
	private <E> E getArg(String name, Class<E> type) {
		return type.cast(args.get(name).value);
	}
	
	private void setArg(String name, String value) {
		if (args.containsKey(name)) {
			Arg a = args.get(name);
			if (a.value instanceof String) {
				a.value = value;
			} else if (a.value instanceof Integer) {
				a.value = Integer.parseInt(value);
			} else if (a.value instanceof Double) {
				a.value = Double.parseDouble(value);
			} else if (a.value instanceof Boolean) {
				a.value = Boolean.parseBoolean(value);
			} else if (a.value instanceof Bound) {
				a.value = Bound.parseBound(value);
			} else {
				throw new IllegalArgumentException("Value does not have supported type");
			}
		} else {
			throw new IllegalArgumentException("Name not in list of supported arguments");
		}
	}
	
	private void usage() {
		System.out.println("MapServerTest takes the following arguments:");
		for (Arg a: args.values()) {
			System.out.println(MessageFormat.format("\t-{0}: {1}  (default ''{2}'')", a.name, a.desc, a.value));
		}
		System.out.println("\nThe result is a table of reports from each client and a summary at the end.\n");
		System.out.println("Each column shows the following:");
		System.out.println(" 1. Milliseconds before client gets first tile response.");
		System.out.println(" 2. Milliseconds before client gets last tile response.");
		System.out.println(" 3. Average number of tiles per second after first tile arrives.");
		System.out.println(" 4. Number of retryable failures (dropped connection, etc.)");
		System.out.println(" 5. Number of permanent failures (bad request, unrecognized layer ID, etc.)");
		System.out.println(" 6. Number of tiles that timed out.");
		System.out.println(" 7. The WMS name of the map that was requested.");
		System.out.println(" 8. The pixels per degree of the requested tiles.");
		System.out.println(" 9. The bounding box in which tiles were requested.");
		System.out.println("10. The pole of the oeqc projection used.\n");
	}
	
	public static void main(String[] args) {
		MapServerTest killer = new MapServerTest();
		try {
			for (int i = 0; i < args.length; i++) {
				if (args[i].trim().startsWith("-")) {
					killer.setArg(args[i].trim().substring(1), args[++i].trim());
				} else {
					System.out.println("Unrecognized switch: " + args[i].trim());
					killer.usage();
					System.exit(1);
				}
			}
			if (killer.getArg("server",String.class).equals("<none>")) {
				killer.usage();
				System.exit(1);
			}
			killer.go();
		} catch (Exception e) {
			e.printStackTrace();
			killer.usage();
			System.exit(1);
		}
	}
	
	private void go() {
		// fix up common user error entering URLs
		String url = getArg("server",String.class).trim();
		if (!url.toLowerCase().startsWith("http://")) {
			url = "http://" + url;
		}
		
		// make sure cache goes where the user wants it to
		String cache = getArg("cache",String.class).trim();
		if (!cache.equals("<none>")) {
			if (!cache.endsWith(""+File.separatorChar)) {
				cache = cache + File.separatorChar;
			}
			CacheManager.setCacheDir(cache);
		}
		
		// echo the arguments
		System.out.println("Arguments");
		for (Arg a: args.values()) {
			System.out.println(MessageFormat.format("\t{0} = {1}", a.name, a.value.toString()));
		}
		
		// build the list of map sources
		List<MapSource> sources;
		long capTime = System.currentTimeMillis();
		final int clients = getArg("clients",Integer.class);
		int timeout = getArg("timeout",Integer.class);
		int maxRequests = 10000;
		String maps = getArg("maps",String.class);
		if (getArg("caps",Boolean.class)) {
			// fetch capabilities from server, ensures requested maps are supported by server
			MapServer server = new WMSMapServer(url, timeout, maxRequests);
			System.out.println("Got capabilities document in " + (System.currentTimeMillis() - capTime) + " ms");
			if (maps.equals("<all>")) {
				sources = server.getMapSources();
			} else {
				sources = new LinkedList<MapSource>();
				for (String s: maps.split(",")) {
					MapSource source = server.getSourceByName(s.trim());
					if (source == null) {
						throw new IllegalArgumentException("Map source not found named " + s.trim());
					} else {
						sources.add(source);
					}
				}
			}
		} else if (maps.equals("<all>")) {
			throw new IllegalArgumentException("Must ask for specific maps if '-caps false' is used");
		} else {
			// build up fake maps in a fake server
			MapServer server = new SlyMapServer(url);
			sources = new LinkedList<MapSource>();
			String[][] cat = {{}};
			Rectangle2D world = new Rectangle2D.Double(-180,-90,360,180);
			for (String s: maps.split(",")) {
				boolean numeric = s.toLowerCase().endsWith("numeric");
				boolean elevation = false;
				boolean geologic = false;
				String owner = "";
				sources.add(new WMSMapSource(
					s, s, "", null, cat, server, numeric, elevation, geologic,world, null, Double.NaN, owner));
			}
		}

		if (sources.isEmpty()) {
			throw new IllegalStateException("No sources found or specified.");
		}
		
		// randomly generate the requests for each client
		Random random = new Random((long) getArg("seed",Integer.class));
		final List<Callable<Stats>> tasks = new ArrayList<Callable<Stats>>(clients);
		for (int j = 0; j < clients; j++) {
			// ppd always uniform power of 2 in [8,512]
			int ppd = 1 << (random.nextInt(7) + 3);
			
			// bbox center is uniform in supplied bounds
			Bound viewBounds = getArg("view",Bound.class);
			double width = getArg("width",Integer.class)/ppd;
			double height = getArg("height",Integer.class)/ppd;
			double xCenter = random.nextDouble() * viewBounds.width + viewBounds.getMinX();
			double yCenter = random.nextDouble() * viewBounds.height + viewBounds.getMinY();
			Rectangle2D bbox = new Rectangle2D.Double(xCenter-width/2, yCenter-height/2, width, height);
			
			// proj center uniform in supplied bounds
			Bound projBounds = getArg("proj",Bound.class);
			double xProj= random.nextDouble() * projBounds.width + projBounds.getMinX();
			double yProj = random.nextDouble() * projBounds.height + projBounds.getMinY();
			ProjObj proj = new ProjObj.Projection_OC(xProj,yProj);
			
			// uniform map selection
			MapSource source = sources.get(random.nextInt(sources.size()));
			
			tasks.add(new ClientThread(new MapRequest(source, bbox, ppd, proj)));
		}
		
		// print static header
		System.out.println(Stats.header());
		final Stats totals = new Stats();
		totals.startTime = totals.endTime = 0;
		
		// execute tasks all at once
		ExecutorService pool = Executors.newCachedThreadPool();
		final CyclicBarrier barrier = new CyclicBarrier(tasks.size() + 1);
		for (final Callable<Stats> task: tasks) {
			final Future<Stats> future = pool.submit(task);
			Thread t = new Thread(new Runnable() {
				public void run() {
					try {
						// wait for stats, update totals, and print this task's stats
						Stats s = future.get();
						synchronized(MapServerTest.this) {
							totals.startTime = totals.startTime == 0 ? s.startTime : Math.min(totals.startTime, s.startTime);
							totals.endTime = Math.max(totals.endTime, s.endTime);
							totals.tileRate += s.tileRate / tasks.size();
							totals.hard += s.hard;
							totals.soft += s.soft;
							totals.tout += s.tout;
							System.out.println(s.toString());
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							barrier.await();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
			t.setDaemon(true);
			t.start();
		}
		
		// wait for all tasks to finish
		pool.shutdown();
		try {
			barrier.await();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		// finish off with the totals
		System.out.println("min\tmax\tavg\tsum\tsum\tsum");
		System.out.println(totals.toString());
	}
	
	private static class Stats {
		/** Map request behind this Stats object */
		public MapRequest request;
		/** Tiles per second from first tile received to last tile */
		public double tileRate;
		/** ms from time request was sent to first tile received */
		public double startTime = Double.POSITIVE_INFINITY;
		/** ms from time request was sent to last tile received */
		public double endTime = Double.NEGATIVE_INFINITY;
		/** count of unretryable failures */
		public int hard = 0;
		/** count of retryable failures */
		public int soft = 0;
		/** count of timeout failures */
		public int tout = 0;
		public String toString() {
			// print client id, average time between request
			// and first response, average time between request and last
			// response, average tiles/second, total failed tiles, and the map, ppd, bounds, and projection used
			if (request != null) {
				return MessageFormat.format(
					"{0,number,#.###}\t{1,number,#.###}\t{2,number,#.###}\t{3}\t{4}\t{5}\t{6}\t{7}\t{8}\t{9,number,#.###},{10,number,#.###}",
					startTime, endTime, tileRate, soft, hard, tout,
					request.getSource().getName(), request.getPPD(), new Bound(request.getExtent()).toString(),
					request.getProjection().getCenterLon(), request.getProjection().getCenterLat());
			} else {
				return MessageFormat.format(
					"{0,number,#.###}\t{1,number,#.###}\t{2,number,#.###}\t{3}\t{4}\t{5}",
					startTime, endTime, tileRate, soft, hard, tout);
			}
		}
		public static String header() {
			return "start\tend\trate\tsoft\thard\ttout\tmap\tppd\tbbox\tproj";
		}
	}
	
	private class ClientThread implements Callable<Stats> {
		private final MapRequest request;
		
		public ClientThread(MapRequest request) {
			this.request = request;
		}
		
		public Stats call() throws Exception {
			Stats stats = new Stats();
			stats.request = request;
			
			// make request
			List<Event> times = doRequest(request, getArg("requests",Integer.class), !getArg("cache",String.class).equals("<none>"));
			
			// pull stats into [first,last,tiles] array
			int count = 0;
			long base = 0;
			for (Event event: times) {
				switch(event.type) {
				case start:
					base = event.time;
					break;
				case tileok:
					count ++;
					stats.startTime = Math.min(stats.startTime, event.time);
					stats.endTime = Math.max(stats.endTime, event.time);
					break;
				case tilesoftfail:
					stats.soft++; break;
				case tilehardfail:
					stats.hard ++; break;
				case sotout:
					stats.tout++; break;
				}
			}
			stats.startTime -= base;
			stats.endTime -= base;
			stats.tileRate = count / stats.endTime * 1000;
			
			return stats;
		}
	}
	
	private static class Event {
		enum Type {start, tileok, tilesoftfail, tilehardfail, end, sotout};
		public Type type;
		public long time;
		public Event(long time, Type type) {
			this.time = time;
			this.type = type;
		}
	}
	
	private static List<Event> doRequest(final MapRequest request, int maxRequests, final boolean cache) {
		final List<Event> events = new LinkedList<Event>();
		events.add(new Event(System.currentTimeMillis(), Event.Type.start));
		
		ExecutorService pool = (maxRequests == -1 ? Executors.newCachedThreadPool() : Executors.newFixedThreadPool(maxRequests));
		Set<MapTile> tiles = MapRetriever.createTiles(request);
		
		for (final MapTile tile: tiles) {
			pool.execute(new Runnable() {
				public void run() {
					MapRequest tileRequest = tile.getTileRequest();
					
					try {
						tile.setImage(request.getSource().fetchTile(tileRequest));
						events.add(new Event(System.currentTimeMillis(), Event.Type.tileok));
						if (cache) {
							CacheManager.storeMapData(tile);
						}
					} catch (RetryableException e) {
						events.add(new Event(System.currentTimeMillis(), Event.Type.tilesoftfail));
					} catch (NonRetryableException e) {
						if (e.getCause() instanceof SocketTimeoutException) {
							events.add(new Event(System.currentTimeMillis(), Event.Type.sotout));
						} else {
							events.add(new Event(System.currentTimeMillis(), Event.Type.tilehardfail));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		}
		
		pool.shutdown();
		try {
			pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		events.add(new Event(System.currentTimeMillis(), Event.Type.end));
		
		return events;
	}
	
	private static class SlyMapServer implements MapServer {
		private final URI uri;
		private final List<MapSource> sources = new LinkedList<MapSource>();
		public SlyMapServer(String uri) {
			try {
				this.uri = new URI(uri);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Badly formatted URL", e);
			}
		}
		public List<MapSource> getMapSources() {
			return sources;
		}
		public URI getMapURI() {
			return uri;
		}
		public int getMaxRequests() {
			return 10000;
		}
		public String getName() {
			return "sly_server";
		}
		public MapSource getSourceByName(String name) {
			for (MapSource s: sources) {
				if (s.getName().equals(name)) {
					return s;
				}
			}
			boolean isNumeric = name.toLowerCase().contains("numeric");
			boolean hasElevation = name.toLowerCase().contains("elevation");
			boolean hasGeologic = name.toLowerCase().contains("geologic");
			WMSMapSource source = new WMSMapSource(
				name,name,null,null,new String[0][],this,isNumeric,hasElevation,hasGeologic,null,null,Double.POSITIVE_INFINITY, null);
			sources.add(source);
			return source;
		}
		public int getTimeout() {
			return 10000;
		}
		public String getTitle() {
			return "server [" + getMapURI() + "]";
		}
		public URI getURI() {
			return uri;
		}
		public boolean isUserDefined() {
			return true;
		}
		public void add(MapSource source) {
			sources.add(source);
		}
		
		/* bs methods that shouldn't be on the MapServer interface */
		public void addListener(MapServerListener l) {
		}
		public void delete() {
		}
		public void load(String serverName) {
		}
		public void remove(String name) {
		}
		public void removeListener(MapServerListener l) {
		}
		public void save() {
		}
		public void loadCapabilities(boolean cached) {
		}
	}
}
