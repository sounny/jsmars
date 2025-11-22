package edu.asu.jmars.layer.util.features;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A set of attributes for a specific feature and a reference to the owning
 * FeatureCollection. The FeatureCollection contains the schema, event
 * listeners, and history logging tools for every Feature.
 * <p>The Feature class contains a map of attributes; for each unique Field,
 * an Object can be stored.
 * <p>This class also contains a back reference to the collection that
 * owns it. Feature instances are contained in exactly one FeatureCollection.
 * This tight coupling between container and contained element greatly
 * simplifies three needs of Feature:
 * <ul>
 * <li>Schema; each Feature in a FeatureCollection is homogeneous, so the
 * schema is stored on the collection instead of on the feature.
 * <li>Events: each change to a Feature is sent to listeners registered on the
 * FeatureCollection. Changes to the collection's Features or the schema are
 * also sent to the listeners.
 * <li>History: Feature instances share a single History instance through the
 * FeatureCollection, ensuring the entire collection and contents are
 * versioned correctly.
 * </ul>
 */
public class Feature {
	// package-private collection that 'owns' this Feature
	private final FeatureCollection owner;
	// package-private map of Field to Object
	public Map<Field,Object> attributes = new LinkedHashMap<Field,Object>();

	private transient boolean hidden = false;
	
	public void setHidden(boolean b) {
		hidden = b;
	}
	public boolean isHidden() {
		return hidden;
	}
	
	/**
	 * Default constructor creates a Feature object with no defined attributes.
	 */
	public Feature () {
		owner = null;
	}
	
	public Feature(FeatureCollection fc) {
		this.owner = fc;
	}
	
	/**
	 * Return the collection that owns this Feature object.
	 */
	public FeatureCollection getOwner(){
		return owner;
	}

	/**
	 * Return a read-only view of the Set of keys to the attribute map.
	 */
	public Set<Field> getKeys () {
		return Collections.unmodifiableSet (attributes.keySet ());
	}

	/**
	 * Return the attribute for the given Field.
	 */
	public Object getAttribute (Field f) {
		return attributes.get (f);
	}
	
	/**
	 * Set the value of a given attribute. If the given Field is not defined
	 * on the containing collection's schema, it will be added.
	 */
	public void setAttribute (Field f, Object value) {
		if (owner != null) {
			owner.setAttributes(this, Collections.singletonMap(f, value));
		} else {
			setAttributeQuiet (f, value);
		}
	}
	
	/**
	 * Set the value of a given attribute without notifying. Useful if
	 * multiple attributes are going to be set on a single Feature, since the
	 * last call can be directed at setAttribute() to send one event for the
	 * many changes (although generally such an operation should be done with
	 * FeatureCollection's setAttributes() method.
	 * <p>Note that if an owner is defined and the given Field is not defined
	 * on the owner's schema, a FIELDADDED event is still sent.
	 */
	public void setAttributeQuiet (Field f, Object value) {
		attributes.put (f, value);
		if (owner != null) {
			// if this field isn't already in the schema, add it and notify
			if (! owner.getSchema ().contains (f))
				owner.addField (f);
		}
	}
	
	/**
	 * Clones the attribute map <i>only</i>; the Feature must be attached to a
	 * FeatureCollection to set the owner.
	 */
	public Feature clone () {
		Feature f = new Feature();
		f.attributes.putAll(this.attributes);
		return f;
	}
	
	/**
	 * Returns true if the Feature is equal to another Feature.
	 * <bold>CAUTION:</bold>Exposing this as the Object.equals(Object o)
	 * makes a whole bunch of stuff slower.
	 */
	public boolean equals(Feature o){
		if (!(o instanceof Feature))
			return false;
		
		Feature f = (Feature)o;
		return f.owner == owner && f.attributes.equals(attributes);
	}
	
	/**
	 * Since the path is used so frequently, this particular resource has get/set
	 * properties.
	 */
	public final FPath getPath() {
		return (FPath)attributes.get(Field.FIELD_PATH);
	}

	/**
	 * Since the path is used so frequently, this particular resource has get/set
	 * properties.
	 */
	public final void setPath (FPath path) {
		setAttribute(Field.FIELD_PATH, path);
	}
}
