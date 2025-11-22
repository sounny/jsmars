package edu.asu.jmars.layer;

import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.layer.stamp.StampLayer;

/*
 * interface to allow various layers that are asked to provide their data (schema)
 * For ex, ShapeLayer needs to provide its schema for expression builder;
 * ditto for StampLayer
 */

public interface ILayerSchemaProvider {
	void doSchema(ShapeLayer shape);
	void doSchema (StampLayer stamp);
}
