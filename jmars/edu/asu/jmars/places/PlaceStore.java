package edu.asu.jmars.places;

import java.util.Comparator;
import java.util.Set;

/**
 * Manages a collection of places and provides for adding (which can also be
 * used to replace) and removing elements.
 * 
 * Provides Set access to places, since other data structures are not
 * implementable by most stores.
 */
public interface PlaceStore extends Set<Place> {
	/**
	 * Set the sorter for ordering elements in the store; the default is by name
	 */
	void setComparator(Comparator<Place> sorter);
}
