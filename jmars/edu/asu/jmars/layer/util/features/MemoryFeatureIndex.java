package edu.asu.jmars.layer.util.features;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.ProjectionEvent;
import edu.asu.jmars.layer.ProjectionListener;
import edu.asu.jmars.util.Util;

/**
 * An in-memory spatial index over a given FeatureCollection.  Queries may return
 * records that are not strictly within the requested area, so for exact results,
 * a subsequent overlap test should be performed.
 * 
 * This index is threadsafe. Any number of queries may run separately, but
 * modification will block until all queries finish, and then block subsequent
 * queries until the changes are finished.
 * 
 * This index adds itself as a listener to the given feature collection and keeps
 * itself up to date with changes.  This does prevent querying while FeatureEvent
 * objects are being dispatched, and disconnect() should be called when this index
 * is no longer in use so it may be garbage collected.
 */
public final class MemoryFeatureIndex implements FeatureIndex, WorldCache, FeatureListener, ProjectionListener {
	public static final Iterator<Feature> emptyIter = new ArrayList<Feature>(0).iterator();
	private final FeatureCollection fc;
	private final Style<FPath> geomStyle;
	private Quadtree tree;
	
	public MemoryFeatureIndex(Style<FPath> geomStyle, FeatureCollection fc) {
		this.fc = fc;
		this.geomStyle = geomStyle;
		
		fc.addListener(this);
		Main.addProjectionListener(this);
	}
	
	public void disconnect() {
		fc.removeListener(this);
		Main.removeProjectionListener(this);
	}
	
	/**
	 * Index of feature to FPath, maintained to ensure the spatial index
	 * elements can always be removed.
	 */
	private final Map<Feature,FPath> feat2path = new HashMap<Feature,FPath>();
	
	public FPath getWorldPath(Feature f) {
		try {
			startReader();
			return feat2path.get(f);
		} finally {
			stopReader();
		}
	}
	
	private volatile int numReaders = 0;
	private volatile int numWriters = 0;
	
	/**
	 * Blocks until there are no write operations, then starts a read operation
	 * and if the tree has not been created, it creates a new one and inserts
	 * all features in the fc.
	 */
	private synchronized void startReader() {
		while (numWriters > 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		if (tree == null) {
			// If the tree has yet to be created then start a writer thread
			// tolerant of the reader we already have, and insert all the
			// features.
			startWriter();
			try {
				tree = new Quadtree();
				for (Feature f: fc.getFeatures()) {
					add(f);
				}
			} catch (ConcurrentModificationException e) {
				tree = null;
				feat2path.clear();
				throw e;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				stopWriter();
			}
		}
		numReaders ++;
	}
	
	/** Stops a read operation and notifies all waiting operations */
	private synchronized void stopReader() {
		numReaders --;
		notifyAll();
	}
	
	/**
	 * Blocks until there are no write or read operations, then starts a write
	 * operation. May set 'one' to true to allow one reader to remain, which
	 * should only be done by a thread which has previously called
	 * {@link #startReader()} and not yet called {@link #stopReader()}.
	 */
	private synchronized void startWriter() {
		while (numReaders > 0 || numWriters > 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		numWriters ++;
	}
	
	/** Stops a write operation and notifies all waiting operations */
	private synchronized void stopWriter() {
		numWriters --;
		notifyAll();
	}
	
	private void add(Feature f) {
		FPath path = geomStyle.getValue(f).getWorld();
		feat2path.put(f, path);
		for (Envelope env: Util.rect2env(path.getShape().getBounds2D())) {
			tree.insert(env, f);
		}
	}
	
	private void remove(Feature f) {
		FPath path = feat2path.remove(f);
		for (Envelope env: Util.rect2env(path.getShape().getBounds2D())) {
			tree.remove(env, f);
		}
	}
	
	/**
	 * Performs a brute force search through the feature collection looking for
	 * records that overlap the query in world coordinates, and returns an
	 * iterator over a collection of the results.
	 */
	public Iterator<Feature> queryUnwrappedWorld(Rectangle2D rect) {
		try {
			startReader();
			
			// get possible matches
			Set<Feature> matches = new HashSet<Feature>();
			for (Envelope env: Util.rect2env(rect)) {
				matches.addAll((List<Feature>)tree.query(env));
			}
			
			// if we have any possible matches, get them into FeatureCollection
			// order and ensure we have exact hits
			List<Feature> results = new ArrayList<Feature>();
			if (!matches.isEmpty()) {
				for (Feature f: fc.getFeatures()) {
					// The second part of this check is REQUIRED because the quad tree query returns many potential hits that don't actually
					// intersect our bounding box.  Quad tree javadoc says the client is required to filter out these extra results.
					if (matches.contains(f) && feat2path.get(f).intersects(rect)) {
						results.add(f);
					}
				}
			}
			return results.iterator();
		} finally {
			stopReader();
		}
	}
	
	/**
	 * This method was copied from queryUnwrappedWorld(Rectangle2D rect) method
	 * in this class. It should be used by the ShapeLView3D class and differs by
	 * means of using a specified ProjObj to convert the FPath on Features to 
	 * see if they intersect with the passed in rectangle (of the same world
	 * projection).
	 */
	public Iterator<Feature> queryUnwrappedWorld(Rectangle2D rect, ProjObj po) {
		try {
			startReader();

			//TODO: Code cycling through Envelopes and populating 'matches' has been
			// removed because the tree.query(env) line does not return the same features 
			// if the mainview gets reprojected.  The mainviews projection is not relevant 
			// to the 3D view because it is passing in a projection and rectangles of its 
			// own use. The below code no longer uses "matches" at all.
			
			List<Feature> results = new ArrayList<Feature>();
			for (Feature f: fc.getFeatures()) {
				//get the path using geom style in case it's a circle (otherwise
				// it would just return the point not the entire footprint of the 
				// circle) then convert to world coords for the passed in ProjObj
				FPath wPath = geomStyle.getValue(f).convertToSpecifiedWorld(po);
				
				//if that world path is in the world rect, add it to the return list
				if(wPath.intersects(rect)){
					results.add(f);
				}
			}
			return results.iterator();
			
		} finally {
			stopReader();
		}
	}
	
	public void receive(final FeatureEvent e) {
		Runnable task = null;
		switch (e.type) {
		case FeatureEvent.ADD_FEATURE:
			task = new Runnable() {
				public void run() {
					if (tree != null) {
						for (Feature f: e.features) {
							add(f);
						}
					}
				}
			};
			break;
		case FeatureEvent.REMOVE_FEATURE:
			task = new Runnable() {
				public void run() {
					// It's faster to recreate the tree if the feature
					// collection has fewer remaining features than were just
					// removed
					if (fc.getFeatureCount() < e.features.size()) {
						tree = null;
						feat2path.clear();
					} else if (tree != null) {
						for (Feature f: e.features) {
							remove(f);
						}
					}
				}
			};
			break;
		case FeatureEvent.CHANGE_FEATURE:
			if (!Collections.disjoint(e.fields, geomStyle.getSource().getFields())) {
				task = new Runnable() {
					public void run() {
						// It's faster to recreate the tree if more than
						// half of the features were just affected
						if (e.valuesBefore == null || fc.getFeatureCount() / 2 < e.valuesBefore.size()) {
							tree = null;
							feat2path.clear();
						} else if (tree != null) {
							for (Feature f: e.valuesBefore.keySet()) {
								remove(f);
							}
							for (Feature f: e.valuesBefore.keySet()) {
								add(f);
							}
						}
					}
				};
			}
			break;
		}
		if (task != null) {
			try {
				startWriter();
				task.run();
			} finally {
				stopWriter();
			}
		}
	}
	
	public void reindex() {
		try {
			startWriter();
			tree = null;
			feat2path.clear();
		} finally {
			stopWriter();
		}
	}
	
	public void projectionChanged(ProjectionEvent e) {
		reindex();
	}
}

