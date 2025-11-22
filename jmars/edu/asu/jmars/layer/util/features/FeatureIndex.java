package edu.asu.jmars.layer.util.features;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 * Describes a 'world coordinate' spatial index over a collection of features,
 * and expose the FPath instances used in the index.
 */
public interface FeatureIndex {
	Iterator<Feature> queryUnwrappedWorld(Rectangle2D rect);
}
