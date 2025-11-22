package edu.asu.jmars.layer.map2;

import java.awt.geom.Area;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.PolyArea;

/**
 * MapProcessor manages all stages of data requesting and processing for a
 * single path through the image processing tree. Raw MapSource data at the root
 * of the tree is requested from MapRetriever, and as each update is received,
 * it is sent to each Stage. After the last stage has processed the data, the
 * result is sent to the MapChannel.
 * 
 * MapProcessor sits idle while the asynchronous request to MapRetriever is
 * downloading images or loading them from disk. When receiveUpdate() is called,
 * the MapProcessor is being notified that the requested data has changed in
 * some fashion. It will add itself to a work thread and some time later, start
 * processing.
 * 
 * MapProcessor will not process all updates sent by MapRetriever. It only keeps
 * track of the most recent update while processing is happening, and when
 * current processing is done, it starts processing the most recent update at
 * that point.
 * 
 * MapProcessor's single constructor sets the MapRetriever's receiver to itself
 * as the very last step, which is vital since once that hook is completed, the
 * MapRetreiver could send data back at any moment.
 */
public final class MapProcessor implements Runnable {
	static ExecutorService pool;
	private static final DebugLog log = DebugLog.instance();
	static void dbgmsg(MapChannel c, MapRequest r, String msg) {
		synchronized(log) {
			log.println("Channel " + (c==null?"null":""+c.hashCode())
				+ " request " + r.hashCode() + ": "
				+ msg);
		}
	}
	
	private final Pipeline pipe;
	private final int pipeIndex;
	private final MapChannel destination;
	private final MapRetriever myRetriever;
	
	private volatile boolean requestFinished;
	private volatile boolean pendingUpdate;
	private volatile boolean updateProcessing;
	
	public MapProcessor(MapRequest request, Pipeline pipe, int pipelineIndex, MapChannel channel) {
		this.pipe = pipe;
		this.pipeIndex = pipelineIndex;
		this.destination = channel;
		this.myRetriever = new MapRetriever(request);
		this.myRetriever.setReceiver(this);
	}
	
	public MapChannel getChannel() {
		return destination;
	}
	
	public void receiveUpdate() {
		if (requestFinished) {
			// It's against the design to receive multiple finished updates, so report it
			log.aprintln("Update received for finished processor");
		} else if (myRetriever.getRequest().isCancelled()) {
			log.println("Ignoring update for cancelled request");
		} else if (pipe == null || pipe.getStageCount() == 0) {
			// If there is no Pipeline, send updates to the channel immediately
			destination.receiveMap(myRetriever.getData());
		} else {
			pendingUpdate = true;
			queueProcessing();
		}
	}
	
	private void queueProcessing() {
		synchronized (this) {
			if (pool == null) {
				int procs = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);
				log.println("Creating pool with " + procs + " processors");
				pool = Executors.newFixedThreadPool(procs, new MapThreadFactory("Map Processor"));
			}
			if (pendingUpdate && !updateProcessing) {
				updateProcessing = true;
				pool.execute(this);
			}
		}
	}
	
	private PolyArea lastFinalArea = new PolyArea();
	private PolyArea lastFuzzyArea = new PolyArea();
	
	public void run() {
		MapData retrievedData;
		
		pendingUpdate = false;
		
		// completely duplicate the current state, at the last possible moment
		MapData result = retrievedData = myRetriever.getData();
		
		if (result.isFinished()) {
			requestFinished = true;
		}
		
		// This is retrieved entirely for dbgmsg purposes
		MapRequest request = myRetriever.getRequest();
		
		// Create the changed area as current valid area minus last valid area.
		PolyArea fuzzyArea = new PolyArea(retrievedData.getFuzzyArea());
		PolyArea finalArea = new PolyArea(retrievedData.getFinishedArea());
		PolyArea changedArea = fuzzyArea.newSub(lastFuzzyArea).newAdd(finalArea.newSub(lastFinalArea));
		changedArea.intersect(new Area(request.getExtent()));
		lastFuzzyArea = fuzzyArea;
		lastFinalArea = finalArea;
		
		if (result.getImage() != null) {
			// process all of the stages
			dbgmsg(destination, request, "starting processing");
			Stage[] stages = pipe.getProcessing();
			for (int i = 0; i < stages.length; i++) {
				try {
					if (i == (stages.length-1)){
						synchronized(stages[i]){
							result = stages[i].process(pipeIndex, result, changedArea);
							if (stages[i].getInputCount() > 1 && result != null)
								result = result.getDeepCopy();
						}
					}
					else {
						result = stages[i].process(0, result, changedArea);
					}
					
					if (result == null) {
						throw new IllegalStateException("Stage returned null");
					}
				} catch (Exception e) {
					log.aprintln("Error in processing stage " +
						stages[i].getClass().getName() +
						": " + e.getMessage());
					log.aprintln(e);
					log.printStack(10);
					result = null;
					break;
				}
			}
			dbgmsg(destination, request, "finished processing");
		} else {
			dbgmsg(destination, request, "no image, skipping processing");
		}
		
		// send the data to its destination
		destination.receiveMap(result == null ? retrievedData.getDeepCopyShell(null, null) : result);
		
		// mark that we are done and start up next unit of work
		updateProcessing = false;
		queueProcessing();
	}
	
	/**
	 * Should be called after the last map2 layer is removed to release
	 * resources (e.g. thread pools)
	 */
	public static void close() {
		if (pool != null) {
			log.println("Stopping MapProcessor ExecutorService");
			pool.shutdownNow();
			pool = null;
		}
	}
}
