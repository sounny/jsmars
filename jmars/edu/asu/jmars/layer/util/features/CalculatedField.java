package edu.asu.jmars.layer.util.features;

import java.util.Collection;
import java.util.Set;

import edu.asu.jmars.layer.shape2.ShapeLayer;

/**
 * Computes values using the abstract {@link #getValue(Feature)} method, either
 * by lazily computing them in calls to {@link #get(Feature)} or in a call to
 * {@link #update(FeatureCollection, Collection)}. Once computed, values are
 * stored on the Feature and not recomputed, so to adjust values when dependent
 * fields change, the subclass should return the fields it depends on in
 * {@link #getFields()} and the instance of this class should be added as a
 * listener on the FeatureCollection.
 */
public abstract class CalculatedField extends Field {
	private static final long serialVersionUID = 1L;
	public CalculatedField(String name, Class<?> type) {
		super(name, type, false);
	}
	/** Returns the fields used by this computed field */
	public abstract Set<Field> getFields();
	public abstract Object getValue(ShapeLayer layer, Feature f);
}

