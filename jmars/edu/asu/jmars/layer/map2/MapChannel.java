package edu.asu.jmars.layer.map2;

import java.awt.geom.Rectangle2D;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import javax.swing.SwingUtilities;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.util.DebugLog;

/**
 * Produces processed MapData, given a projection, extent in that projection,
 * scale, and an array of processing steps to perform.
 * 
 * Multiple updates are sent to each receiver as data is produced. Typically
 * data arrives a piece at a time so receivers should cope with MapData elements
 * that are not finished. This interface does guarrantee that by timeout or
 * error, there will always eventually be a MapData update sent to all receivers
 * that is finished.
 * 
 * MapData is always sent to receivers while running in the AWT thread.
 */
public class MapChannel {
	private final DebugLog log = DebugLog.instance();
	private ProjObj proj;
	private Rectangle2D extent;
	private int ppd;
	private Pipeline[] pipe;
	/** MapSourceListeners may rely on being the first listener, so this should remain a list */
	private List<MapChannelReceiver> receivers = new LinkedList<MapChannelReceiver>();
	
	public MapChannel() {}
	
	public MapChannel(Rectangle2D newExtent, int newPpd, ProjObj newProj, Pipeline[] newPipe) {
		extent = newExtent;
		ppd = newPpd;
		proj= newProj;
		setPipeline(newPipe);
	}
	
	/**
	 * Add a new receiver to this channel. Updates are dispatched to receivers in
	 * the order they are added, so a channel owner can guarrantee first
	 * delivery by adding itself as a listener immediately after construction.
	 */
	public synchronized void addReceiver(MapChannelReceiver receiver) {
		receivers.add(receiver);
	}
	
	public synchronized void removeReceiver(MapChannelReceiver receiver) {
		receivers.remove(receiver);
	}
	
	public synchronized void setPipeline(Pipeline[] newPipe) {
		pipe = newPipe;
		if (pipe == null)
			pipe = new Pipeline[0];
		reprocess();
	}
	
	/** Sets up the channel to return the raw extent of the given source */
	public synchronized void setRequest(MapRequest request) {
		pipe = new Pipeline[]{new Pipeline(request.getSource(), new Stage[0])};
		extent = request.getExtent();
		ppd = request.getPPD();
		proj = request.getProjection();
		reprocess();
	}
	
	private long sequence = 0;
	private long startTime;
	private MapRequest[] mapRequests = {};
	
	/**
	 * Makes a request to MapProcessor to reprocess the data through the
	 * pipeline. This becomes necessary when one of settings in a stage changes.
	 */
	public synchronized void reprocess() {
		for (int i = 0; i < mapRequests.length; i++) {
			mapRequests[i].cancelRequest();
		}
		mapRequests = new MapRequest[0];
		
		if (pipe.length > 0 && proj != null && ppd > 0 && extent != null) {
			log.println("Channel[" + super.hashCode() + "] started");
			sequence ++;
			startTime = System.currentTimeMillis();
			mapRequests = new MapRequest[pipe.length];
			// create a new request+runner for each pipeline
			for (int i = 0; i < pipe.length; i++) {
				mapRequests[i] = new MapRequest(pipe[i].getSource(), extent, ppd, proj);
				new MapProcessor(mapRequests[i], pipe[i], i, this);
			}
		}
	}
	
	public synchronized Pipeline[] getPipeline() {
		return pipe;
	}
	
	public synchronized ProjObj getProjection() {
		return proj;
	}
	
	public synchronized int getPPD() {
		return ppd;
	}
	
	public synchronized Rectangle2D getExtent() {
		return extent;
	}
	
	public synchronized void setMapWindow(Rectangle2D newExtent, int newPpd, ProjObj newProj) {
		extent = newExtent;
		ppd = newPpd;
		proj = newProj;
		reprocess();
	}
	
	private synchronized long getSequence(){
		return sequence;
	}
	
	private void log(String msg) {
		log.println(MessageFormat.format("Channel [{0,number,#}] {1}", hashCode(), msg));
	}
	
	public void receiveMap(final MapData newData) {
		// Dispatch map update events to listeners on the AWT thread so the
		// views, which typically update AWT components, don't have to worry
		// about being on the AWT thread. Also avoids sending updates for older
		// versions of the MapChannel by looking at the change time as a sort of
		// serial number.
		final long sequence = getSequence();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (sequence != getSequence()) {
					log("Skipping update for invalid sequence");
				} else if (newData.getRequest().isCancelled()) {
					log("Skipping update for canceled request");
				} else {
					if (newData.isFinished()) {
						log("Finished after " + (System.currentTimeMillis()-startTime) + " ms");
					}
					// dispatching could take awhile, so sync on the same lock that
					// the listener mutators use, and copy the listeners so adding a listener
					// mid-dispatch doesn't cause concurrent modification exceptions.
					List<MapChannelReceiver> rcopy;
					synchronized(MapChannel.this) {
						rcopy = new LinkedList<MapChannelReceiver>();
						rcopy.addAll(receivers);
					}
					for (MapChannelReceiver r: rcopy) {
						r.mapChanged(newData);
					}
				}
			}
		});
	}
}
