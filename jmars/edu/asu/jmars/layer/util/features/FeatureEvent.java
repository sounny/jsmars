package edu.asu.jmars.layer.util.features;

import java.util.*;

import edu.asu.jmars.util.History;

/**
 * Describes a change to a FeatureCollection. Features can be added, removed,
 * or changed, and Fields can be added or removed. The type and affected class
 * variables are final, so once set by the constructor they cannot be changed.
 * 
 * The FeatureEvent is a dual purpose object. It is also used as the 
 * history state in the {@link History} object. Thus, it is supposed to contain
 * a complete representation of the change being made. This is currently not
 * true.
 */
public class FeatureEvent {
	/**
	 * Features were added.
	 */
	public static final int ADD_FEATURE = 0;

	/**
	 * Features were removed.
	 */
	public static final int REMOVE_FEATURE = 1;

	/**
	 * Features were changed.
	 */
	public static final int CHANGE_FEATURE = 2;

	/**
	 * Fields were added.
	 */
	public static final int ADD_FIELD = 3;

	/**
	 * Fields were removed.
	 */
	public static final int REMOVE_FIELD = 4;

	/**
	 * One of the above enumerated types.
	 */
	public final int type;
	
	/**
	 * The source FeatureCollection which contains/contained the
	 * Features/Fields.
	 */
	public final FeatureCollection source;

	/**
	 * List of Feature instances affected by this change.
	 */
	public final List<Feature> features;

	/**
	 * Indices of Features affected by this change. For removed Features
	 * these are the indices when all of the removed Features were still 
	 * in the list. While for added Features these are the indices after
	 * the Features were added.
	 */
	public final Map<Feature,Integer> featureIndices;

	/**
	 * Map<Feature,Feature.clone> before the Features were updated.
	 */
	public final Map<Feature,Map<Field,Object>> valuesBefore;

	/**
	 * List of Field instances affected by this change.
	 */
	public final List<Field> fields;
	
	/**
	 * List of Field indices affected by this change. For removed Fields
	 * these are the indices when all of the removed Fields were still
	 * in the schema. While for added Fields these are the indices after
	 * the Fields were added.
	 */
	public final Map<Field,Integer> fieldIndices;
	
	/**
	 * Create a new FeatureEvent with the given type and affected lists.
	 * These are publically accessible fields, but are final and so cannot
	 * be modified once they are set here.
	 */
	public FeatureEvent (int type, FeatureCollection source, List<Feature> features, Map<Feature,Map<Field,Object>> valuesBefore, List<Field> fields) {
		this.type = type;
		this.source = source;
		this.features = features;
		this.valuesBefore = valuesBefore;
		this.fields = fields;
		
		// Populate the feature and field indices if the FeatureCollection is set.
		if (source != null && features != null)
			featureIndices = FeatureUtil.getFeatureIndices(source.getFeatures(), features);
		else
			featureIndices = null;

		if (source != null && fields != null)
			fieldIndices = FeatureUtil.getFieldIndices(source.getSchema(), fields);
		else
			fieldIndices = null;
	}
	
	/**
	 * A simple display of the event.
	 */
	public String toString(){
		String result = "FeatureEvent: ";
		switch (type) {
		case ADD_FEATURE:
			result += "ADD_FEATURE\n";
			break;
		case REMOVE_FEATURE:
			result += "REMOVE_FEATURE\n";
			break;
		case CHANGE_FEATURE:
			result += "CHANGE_FEATURE\n";
			break;
		case ADD_FIELD:
			result += "ADD_FIELD\n";
			break;
		case REMOVE_FIELD:
			result += "REMOVE_FIELD\n";
			break;
		}
		
		if (source != null){
			result += "  source:\n";
			result += "    "+source+"\n";
		}
		
		if (features != null) {
			result += "  features:\n";
			for (Feature f: features)
				result += "    " + f + "\n";
		}

		if (fields != null) {
			result += "  fields:\n";
			for (Field f: fields)
				result += "    " + f + "\n";
		}

		return result;
	}
}


