package edu.asu.jmars.layer.util.features;

import java.io.Serializable;
import java.util.Set;

/** Provides a style value for each feature */
public interface StyleSource<E> extends Serializable {
	/**
	 * @return the style for this feature, or a default if the reference is
	 * null or the feature cannot otherwise be used to produce a value, but
	 * NEVER null.
	 */
	E getValue(Feature f);
	/**
	 * @return the fields this style depends on, or the emtpy set if no
	 * fields are used by this style source.
	 */
	Set<Field> getFields();
}

