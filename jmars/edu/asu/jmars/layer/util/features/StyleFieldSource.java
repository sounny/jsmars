package edu.asu.jmars.layer.util.features;

import java.util.Collections;
import java.util.Set;

/**
 * Provides style values directly from attributes on the style, or from a
 * given default when the style attribute is not defined
 */
public final class StyleFieldSource<E extends Object> implements StyleSource<E> {
	private static final long serialVersionUID = 1L;
	private final Field f;
	private final E defaultValue;
	public StyleFieldSource(Field f, E defaultValue) {
		this.f = f;
		this.defaultValue = defaultValue;
	}
	public E getValue(Feature feature) {
		if (feature == null) {
			return defaultValue;
		} else {
			Object out = feature.getAttribute(f);
			if (out != null) {
				return (E)out;
			} else {
				return defaultValue;
			}
		}
	}
	public Set<Field> getFields() {
		return Collections.singleton(f);
	}
}

