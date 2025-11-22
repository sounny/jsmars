package edu.asu.jmars.layer.util.features;

import java.io.Serializable;

/**
 * Pairs a human readable style name with the source of the style, and
 * provides convenience constructors and methods to simplify code where there
 * does not need to be a difference; note however that there IS a difference,
 * as a source can be replaced with e.g. auto-calculate functionality.
 */
public final class Style<E extends Object> implements Serializable {
	private final String name;
	private StyleSource<E> source;
	/** copy constructor */
	public Style(Style<E> style) {
		this(style.name, style.source);
	}
	public Style(String name, StyleSource<E> source) {
		this.name = name;
		this.source = source;
	}
	public Style(String name, Field field, E defaultValue) {
		this(name, new StyleFieldSource<E>(field, defaultValue));
	}
	public Style(String name, E override) {
		this(name, new StyleGlobalSource<E>(override));
	}
	/** Return a human-readable name for this style */
	public String getName() {
		return name;
	}
	/** Return a provider for this style */
	public StyleSource<E> getSource() {
		return source;
	}
	/** Sets the style source to the given source */
	public void setSource(StyleSource<E> source) {
		this.source = source;
	}
	/**
	 * Convenience method to set the style source to the given constant,
	 * which must be of type <E>.
	 */
	public void setConstant(Object constant) {
		this.source = new StyleGlobalSource<E>((E)constant);
	}
	/**
	 * Convenience method to set the style source to the given field and
	 * constant, which must be of type <E>.
	 */
	public void setSource(Field f, Object constant) {
		this.source = new StyleFieldSource<E>(f, (E)constant);
	}
	
	/** Convenience method that calls source.getValue(Feature) */
	public E getValue(Feature f) {
		return source.getValue(f);
	}
	
	public String toString() {
		return name;
	}
	public int hashCode() {
		return name.hashCode();
	}
	public boolean equals(Object o) {
		return o instanceof Style && ((Style)o).name.equals(name);
	}
}

