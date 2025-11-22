package edu.asu.jmars.layer.util.features;

import java.util.Collections;
import java.util.Set;

/** Provides style values from a constant source that ignores feature attributes */
public final class StyleGlobalSource<E> implements StyleSource<E> {
	private static final long serialVersionUID = 1L;
	private final E value;
	public StyleGlobalSource(E value) {
		this.value = value;
	}
	public E getValue(Feature feature) {
		return value;
	}
	public Set<Field> getFields() {
		return Collections.emptySet();
	}
}

