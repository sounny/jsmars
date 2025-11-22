package edu.asu.jmars.layer.map2;

/** all registered listeners will be notified when the custom maps list changes */
public interface MapServerListener {
	/**
	 * <ul>
	 * <li>ADDED: a map source was added
	 * <li>REMOVED: a map source was removed
	 * <li>UPDATED: a property on an existing map source was changed
	 * </ul>
	 */
	enum Type {ADDED, REMOVED, UPDATED};
	/**
	 * Called when a custom map is added or removed. This only signals that a
	 * change occurred, it does not describe the change.
	 */
	void mapChanged (MapSource source, Type changeType);
}
