package edu.asu.jmars.layer.map2;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.map2.stages.SigmaStretchStage;
import edu.asu.jmars.layer.map2.stages.SigmaStretchStageSettings;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

/**
 * Provides the features of a MapChannel but is much more efficient in terms of
 * memory and processing when the {@link MapRequest} is large, because
 * downloading, processing, and responses are sent in tile-sized pieces.
 * 
 * Because responses are sent one tile at a time, it is the responsibility of
 * the {@link MapChannelReceiver} to aggregate the results in whatever way is
 * most appropriate.
 * 
 * The setRequest() method causes a separate {@link MapChannel} to be created
 * for each tile in the requested extent. The updates from each channel are
 * received by this channel and handed back to the single
 * {@link MapChannelReceiver} given to the constructor.
 * 
 * Each {@link MapStage} in the processing graph is cloned so it has a separate
 * working area for cache purposes. The original {@link StageSettings} reference
 * is, however, shared amongst all of the channels.
 */
public class MapChannelTiled {
	private final DebugLog log = DebugLog.instance();
	
	private final MapChannelReceiver receiver;
	private Pipeline[] pipes = new Pipeline[0];
	private int ppd;
	private ProjObj po;
	private Rectangle2D extent;
	private List<MapChannel> channels = new ArrayList<MapChannel>();
	int channelCount;
	
	public ProjObj getProjection() {
		return po;
	}
	public Rectangle2D getExtent() {
		return (Rectangle2D)extent;
	}
	public int getPPD() {
		return ppd;
	}
	public Pipeline[] getPipelines() {
		return pipes;
	}
	
	private final Comparator<Point> pointComp = new Comparator<Point>() {
		public int compare(Point o1, Point o2) {
			int diff = o1.x - o2.x;
			if (diff < 0) return -1;
			if (diff > 0) return 1;
			diff = o1.y - o2.y;
			if (diff < 0) return -1;
			if (diff > 0) return 1;
			return 0;
		}
	};
	
	/**
	 * @param receiver The destination of each individual tile's worth of data
	 */
	public MapChannelTiled(MapChannelReceiver receiver) {
		this.receiver = receiver;
	}
	
	public boolean isFinished() {
		return channels.isEmpty();
	}
	
	public synchronized void cancel() {
		for (MapChannel ch: channels) {
			ch.setPipeline(null);
		}
		
		channels.clear();
	}
	
	/**
	 * Cancels all existing channels, and if po, extent, and pipes are not
	 * null, ppd > 0, and there is at least one pipeline in
	 * <code>pipes</code>, it will start new channels for each tile in
	 * the current view.
	 */
	public synchronized void setRequest(ProjObj po, Rectangle2D extent, int ppd, Pipeline[] pipes) {
		this.po = po;
		this.extent = extent;
		this.ppd = ppd;
		this.pipes = pipes;
		
		cancel();
		
		if (po != null && extent != null && !extent.isEmpty() && ppd != 0 && pipes != null && pipes.length != 0) {
			Set<Point> tiles = new TreeSet<Point>(pointComp);
			
			boolean processAllAtOnce = false;
			
			for (Pipeline pipe : pipes) {
				for (int i=0; i<pipe.getStageCount(); i++) {
					if (pipe.getStageAt(i) instanceof SigmaStretchStage) {
						SigmaStretchStage sss = (SigmaStretchStage)pipe.getStageAt(i);
						SigmaStretchStageSettings ssss = (SigmaStretchStageSettings)sss.getSettings();
						if (ssss.getMinPPD()<=ppd) {
							processAllAtOnce = true;
						}
					}
				}
			}
			
			if (processAllAtOnce) {				
				channelCount = 1;
				
				final MapChannel newChannel = new MapChannel();
				newChannel.addReceiver(new MapChannelReceiver() {
					public void mapChanged(MapData mapData) {
						boolean sendIt = false;
						synchronized(MapChannelTiled.this) {
							if (channels.contains(newChannel)) {
								if (mapData.isFinished()) {
									channels.remove(newChannel);
								} else {
									return;
								}
								if (mapData.isFinished() && !mapData.getRequest().isCancelled()) {
									sendIt = true;
								} else {
								}
							} else {
							}
						}
						if (sendIt) {
							// this does not return quickly or without side
							// effects, so we call it outside the lock on
							// MapChannelTiled.this
							receiver.mapChanged(mapData);
						}
					}
				});
				
				newChannel.setPipeline(Pipeline.getStageCopy(pipes));
				newChannel.setMapWindow(extent, ppd, po);
				channels.add(newChannel);

			} else {
				for (Rectangle2D wrappedExtent: Util.toWrappedWorld(extent)) {
					for (Point tile: MapRetriever.tiler.getTiles(wrappedExtent, ppd)) {
						tiles.add(tile);
					}
				}
				channelCount = tiles.size();
				for (final Point p: tiles) {
					final MapChannel newChannel = new MapChannel();
					log(MessageFormat.format("tile[{0},{1}] started", p.x, p.y));
					newChannel.addReceiver(new MapChannelReceiver() {
						public void mapChanged(MapData mapData) {
							boolean sendIt = false;
							synchronized(MapChannelTiled.this) {
								if (channels.contains(newChannel)) {
									if (mapData.isFinished()) {
										channels.remove(newChannel);
										log(p, MessageFormat.format(
											"finished, {0}/{1} channels done  ",
											(channelCount-channels.size()), channelCount));
									} else {
										log(p, "updated");
									}
									if (!mapData.getRequest().isCancelled()) {
										log(p, "Sending update");
										sendIt = true;
									} else {
										log(p, "Skipping cancelled update");
									}
								} else {
									log(p, "updated, but not in channels list");
								}
							}
							if (sendIt) {
								// this does not return quickly or without side
								// effects, so we call it outside the lock on
								// MapChannelTiled.this
								receiver.mapChanged(mapData);
							}
						}
					});
					
					newChannel.setPipeline(Pipeline.getStageCopy(pipes));
					newChannel.setMapWindow(MapRetriever.tiler.getExtent(p, ppd), ppd, po);
					channels.add(newChannel);
				}
			}
		}
	}
	
	private void log(Point p, String msg) {
		log(MessageFormat.format("tile[{0},{1}] {2}", p.x, p.y, msg));
	}
	private void log(String msg) {
		log.println("[MapChannelTiled] " + msg);
	}
}

