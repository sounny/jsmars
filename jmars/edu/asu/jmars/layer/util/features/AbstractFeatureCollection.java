package edu.asu.jmars.layer.util.features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.TreeMap;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.History;
import edu.asu.jmars.util.Versionable;

/**
 * This class maintains the provider, filename, and listener properties required
 * by a FeatureCollection, as well as ArrayList<Feature> and ArrayList<Field>
 * structures to maintain the ordering of the features and fields.
 * 
 * Implementations must implement the abstract methods for adding and removing
 * fields and features from the storage mechanism used behind the feature
 * collection.  The abstract methods provide the semantics of a Collection for
 * fields and features, without the implementation needing to do more than
 * insert/update/delete Feature attributes at the appropriate times.  The
 * Feature attribute map should be replaced by the implementation to ensure
 * that all cell accesses or updates process properly and using the current data.
 */
public abstract class AbstractFeatureCollection implements FeatureCollection, Versionable {
	private static final DebugLog log = DebugLog.instance();
	
	/** @return an ordered set of fields that appear on any of the given features */
	private static Collection<Field> mergeFields(Collection<? extends Feature> features) {
		Set<Field> fields = new LinkedHashSet<Field>();
		for (Feature f: features) {
			fields.addAll(f.attributes.keySet());
		}
		return fields;
	}
	
	/**
	 * The list of features; the attributes map on each Feature instance should
	 * be suitable for accessing cells with the scheme of the concrete feature
	 * collection.
	 */
	private final List<Feature> features = new ArrayList<Feature>();
	
	/**
	 * Orders the Field elements in the <code>schema</code> field by
	 * their position in the schema. This comparator is used to
	 * restore List order to fields after they have been accumulated
	 * in an unordered container like a Set.
	 */
	private final Comparator<Field> schemaComp = new Comparator<Field> () {
		public boolean equals(Object o) {
			return this.equals(o);
		}
		public int compare (Field o1, Field o2) {
			int i1 = schema.indexOf(o1);
			int i2 = schema.indexOf(o2);
			if (i1 < i2) {
				return -1;
			} else if (i1 > i2) {
				return 1;
			} else {
				return 0;
			}
		}
	};
	
	/**
	 * The list of fields; should always be a superset of the fields used on all
	 * features.
	 */
	private final List<Field> schema = new ArrayList<Field>();
	
	// Abstract methods that must actually modify the the features and schema lists
	
	/**
	 * Signals to the storage mechanism of this feature collection that the given
	 * field may be used on some Feature instances.
	 */
	protected abstract void addFields_impl(Collection<Field> f);
	
	/**
	 * Signals to the storage mechanism of this feature collection that the given
	 * field will no longer be used on any Feature instances.  When possible,
	 * implementations should change the data used by the Feature attribute maps
	 * owned by this FeatureCollection such that {@link Feature#getKeys()} will
	 * not contain any of the given fields.
	 */
	protected abstract void removeFields_impl(Collection<Field> f);
	
	/**
	 * Creates and returns a copy of the given map, backed by the storage
	 * mechanism of this FeatureCollection.
	 */
	protected abstract Map<Field,Object> addData_impl(Map<Field,Object> data);
	
	/**
	 * Removes the given map from this FeatureCollection, typically by
	 * freeing any resources it is using in the storage mechanism of this
	 * FeatureCollection.
	 */
	protected abstract void removeData_impl(Map<Field,Object> data);
	
	// AbstractFeatureCollection methods
	
	/**
	 * The given Feature is simply returned if already owned by this
	 * FeatureCollection.  Otherwise the Feature will be cloned if it
	 * has an owner, and regardless the attributes will be replaced
	 * with the result of passing the old attributes to
	 * {@link #addData_impl(Map)}, which should inject the data into
	 * the storage mechanism of this FeatureCollection.
	 * @return A Feature owned by this FeatureCollection.
	 */
	private Feature getInternal(Feature f) {
		if (f.getOwner() != this) {
			Feature f2 = new Feature(this);
			f2.attributes = addData_impl(f.attributes);
			f = f2;
		}
		return f;
	}
	
	/**
	 * Add Fields at the specified indices. Both indices and fields are parallel
	 * arrays.
	 */
	private void addFields(Integer[] indices, Field[] fields){
		if (indices == null || fields == null || indices.length != fields.length)
			return;
		
		Map<Integer,Field> indexToField = new TreeMap<Integer,Field>();
		for(int i=0; i<indices.length; i++)
			indexToField.put(indices[i], fields[i]);
		
		List<Field> addedFields = new ArrayList<Field>();
		for(int index: indexToField.keySet()) {
			Field field = indexToField.get(index);
			if (index < 0 || index > schema.size()) {
				log.println("Field "+field+" is beyond ("+index+") schema bounds ("+schema.size()+")");
				continue;
			}
			if (schema.contains(field)) {
				log.println("Field "+field+" is already in the schema, cannot add it again");
				continue;
			}
			schema.add(index, field);
			addedFields.add(field);
		}
		
		addFields_impl(addedFields);
		notifyAddFields(addedFields);
	}
	
	/**
	 * Add features at the specified indices. Both indices and features are
	 * parallel arrays.
	 */
	private void addFeatures(Integer[] indices, Feature[] features) {
		if (indices == null || features == null || indices.length != features.length)
			return;
		
		TreeMap<Integer,Feature> indexToFeature = new TreeMap<Integer,Feature>();
		for(int i=0; i<indices.length; i++) {
			indexToFeature.put(indices[i], features[i]);
		}
		
		addFields(mergeFields(indexToFeature.values()));
		
		for(int index: indexToFeature.keySet()) {
			if (index < 0 || index > this.features.size()) {
				log.println("index " + index + " out of bounds of list with size " + this.features.size()+")");
				continue;
			}
			this.features.add(index, getInternal(indexToFeature.get(index)));
		}
		
		notifyAddFeatures(indexToFeature.values());
	}
	
	// FeatureCollection implementation
	
	private void addFields(Collection<Field> c) {
		Set<Field> added = null;
		for (Field f: c) {
			if (!schema.contains(f)) {
				if (added == null) {
					added = new LinkedHashSet<Field>();
				}
				added.add(f);
				schema.add(f);
			}
		}
		if (added != null) {
			addFields_impl(added);
			notifyAddFields(added);
		}
	}
	
	public void addField(Field f) {
		addFields(Collections.singleton(f));
	}
	
	public void removeField(Field f) {
		if (schema.contains(f)) {
			// Create the event before changing the 'schema' list since the
			// FeatureEvent ctor uses the list to calculate the affected indices.
			Map<Feature,Map<Field,Object>> valuesBefore = new HashMap<Feature,Map<Field,Object>>();
			Collection<Field> fields = Collections.singleton(f);
			for (Feature feat: features) {
				valuesBefore.put(feat, extract(feat, fields));
			}
			FeatureEvent e = new FeatureEvent(FeatureEvent.REMOVE_FIELD, this, null, valuesBefore, Arrays.asList(f));
			schema.remove(f);
			removeFields_impl(Collections.singleton(f));
			notify(e);
		}
	}
	
	/**
	 * Adds any new fields needed by any feature in the collection and then adds
	 * all features in the collection.
	 * 
	 * If a Feature is already owned by this FeatureCollection, it will not be
	 * added again.  This ensures that the {@link FeatureEvent#featureIndices
	 * is consistent for all updates; otherwise e.g. a FIELD_ADDED event could describe
	 * newly-added features with the wrong indices.
	 */
	public void addFeatures(Collection<? extends Feature> c) {
		addFields(mergeFields(c));
		List<Feature> added = new ArrayList<Feature>();
		for (Feature f: c) {
			if (f.getOwner() != this) {
				f = getInternal(f);
				features.add(f);
				added.add(f);
			}
		}
		notifyAddFeatures(added);
	}
	
	/** @see {@link #addFeatures(Collection)}. */
	public void addFeature(Feature f) {
		addFeatures(Collections.singleton(f));
	}
	
	/**
	 * Removes all features owned by this FeatureCollection, replacing their
	 * data maps with memory-based HashMaps.
	 */
	public void removeFeatures(Collection<? extends Feature> c) {
		Set<Feature> featSet = new HashSet<Feature>();
		for (Feature f: c) {
			if (f.getOwner() == this) {
				featSet.add(f);
			}
		}
		// Create the event before changing the 'features' list since the
		// FeatureEvent ctor uses the list to calculate the affected indices.
		List<Feature> featList = c instanceof RandomAccess && c instanceof List<?> ? (List<Feature>)c : new ArrayList<Feature>(c);
		FeatureEvent fe = new FeatureEvent (FeatureEvent.REMOVE_FEATURE, this, featList, null, null);
		features.removeAll(featSet);
		for (Feature f: featSet) {
			Map<Field,Object> data = new HashMap<Field,Object>(f.attributes);
			removeData_impl(f.attributes);
			f.attributes = data;
			// f.owner = null;
		}
		notify(fe);
	}
	
	/** @see {@link #removeFeatures(Collection)}. */
	public void removeFeature(Feature f) {
		removeFeatures(Collections.singleton(f));
	}
	
	public void setAttributes(Map<Feature, Map<Field,Object>> features) {
		// add any missing fields first
		Set<Field> fields = new LinkedHashSet<Field>();
		for (Map<Field,Object> row: features.values()) {
			fields.addAll(row.keySet());
		}
		addFields(fields);
		
		// then insert the new values onto the attribute map of each feature
		// owned by this collection
		List<Feature> modifiedFeatures = new ArrayList<Feature>();
		Set<Field> modifiedFields = new HashSet<Field>();
		Map<Feature,Map<Field,Object>> valuesBefore = new LinkedHashMap<Feature,Map<Field,Object>>();
		for (Feature feature: features.keySet()) {
			if (feature.getOwner() == this) {
				Map<Field,Object> values = features.get(feature);
				valuesBefore.put(feature, extract(feature, values.keySet()));
				modifiedFeatures.add(feature);
				modifiedFields.addAll(values.keySet());
				feature.attributes.putAll(values);
			}
		}
		modifiedFields.retainAll(schema);
		notifySetCells(modifiedFeatures, modifiedFields, valuesBefore);
	}
	
	private static Map<Field,Object> emptyMap = Collections.emptyMap();
	
	/**
	 * @return a map of the present values of each of the given fields in the
	 *         given feature, optimized to return Collections.emptyMap() or
	 *         Collections.singletonMap() where applicable, since those are such
	 *         common cases.
	 */
	private static Map<Field,Object> extract(Feature feature, Collection<Field> fields) {
		if (fields.isEmpty()) {
			return emptyMap;
		} else  if (fields.size() == 1) {
			Field f = fields.iterator().next();
			Object value = feature.attributes.get(f);
			if (value == null) {
				return emptyMap;
			} else {
				return Collections.singletonMap(f, value);
			}
		} else {
			Map<Field,Object> map = new HashMap<Field,Object>();
			for (Field f: fields) {
				Object value = feature.attributes.get(f);
				if (value != null) {
					map.put(f, value);
				}
			}
			if (map.isEmpty()) {
				return emptyMap;
			} else {
				return map;
			}
		}
	}
	
	public void setAttributes(Feature feature, Map<Field,Object> fields) {
		if (feature.getOwner() == this) {
			addFields(fields.keySet());
			Map<Field,Object> oldValues = extract(feature, fields.keySet());
			feature.attributes.putAll(fields);
			notifySetCells(Collections.singletonList(feature), fields.keySet(), Collections.singletonMap(feature, oldValues));
		}
	}
	
	public void setAttributes(Field field, Map<? extends Feature,Object> features) {
		boolean notifiedFields = false;
		List<Feature> modifiedFeatures = new ArrayList<Feature>(features.size());
		Map<Feature,Map<Field,Object>> valuesBefore = new HashMap<Feature,Map<Field,Object>>();
		for (Feature feature: features.keySet()) {
			if (feature.getOwner() == this) {
				if (!notifiedFields) {
					notifiedFields = true;
					addField(field);
				}
				valuesBefore.put(feature, Collections.singletonMap(field, feature.attributes.get(field)));
				modifiedFeatures.add(feature);
				feature.attributes.put(field, features.get(feature));
			}
		}
		notifySetCells(modifiedFeatures, Collections.singletonList(field), valuesBefore);
	}
	
	// field access
	
	public List<Field> getSchema() {
		return Collections.unmodifiableList(schema);
	}
	
	// feature access
	
	public List<Feature> getFeatures() {
		return Collections.unmodifiableList(features);
	}
	public Feature getFeature(int pos) {
		return features.get(pos);
	}
	public int getFeatureCount() {
		return features.size();
	}
	
	// listeners
	
	private Set<FeatureListener> listeners = new LinkedHashSet<FeatureListener>();
	public void addListener(FeatureListener l) {
		listeners.add(l);
	}
	public void removeListener(FeatureListener l) {
		listeners.remove(l);
	}

	/**
	 * @return Returns a copy of the list of FeatureListeners instances
	 *         registered with this FeatureCollection.
	 */
	public List<FeatureListener> getListeners() {
		return new ArrayList<FeatureListener>(listeners);
	}
	public void notify(FeatureEvent e) {
		if (history != null) {
			history.addChange(this, e);
		}
		for (FeatureListener l: getListeners()) {
			l.receive(e);
		}
	}
	private void notify(int code, Collection<Feature> features, Collection<Field> fields, Map<Feature,Map<Field,Object>> new2old) {
		if (listeners.isEmpty() ||
				((features == null || features.isEmpty()) &&
				 (fields == null || fields.isEmpty()))) {
			return;
		}
		
		// Cast or copy features into a List
		List<Feature> featList = null;
		if (features instanceof List<?>) {
			featList = (List<Feature>)features;
		} else if (features != null) {
			featList = new ArrayList<Feature>(features);
		}
		
		// Cast or copy fields into a List
		List<Field> fieldList = null;
		if (fields instanceof List<?>) {
			// cast to List and use existing order
			fieldList = (List<Field>)fields;
		} else if (fields != null) {
			// copy into new List and ensure order is by the schemaComp comparator
			fieldList = new ArrayList<Field>(fields);
			Collections.sort(fieldList, schemaComp);
		}
		
		notify(new FeatureEvent(code, this, featList, new2old, fieldList));
	}
	private void notifyAddFields(Collection<Field> fields) {
		notify(FeatureEvent.ADD_FIELD, null, fields, null);
	}
	private void notifyAddFeatures(Collection<Feature> features) {
		notify(FeatureEvent.ADD_FEATURE, features, null, null);
	}
	private void notifySetCells(Collection<Feature> features, Collection<Field> fields, Map<Feature,Map<Field,Object>> new2old) {
		notify(FeatureEvent.CHANGE_FEATURE, features, fields, new2old);
	}
	
	// filename
	
	private String filename;
	public String getFilename() {
		return filename;
	}
	public void setFilename(String fileName) {
		this.filename = fileName;
	}
	
	// provider
	
	private FeatureProvider provider;
	public void setProvider(FeatureProvider provider) {
		this.provider = provider;
	}
	public FeatureProvider getProvider() {
		return provider;
	}
	
	// versionable implementation
	
	private History history;
	
	/**
	 * Sets the history log where all history events should go to.
	 */
	public void setHistory(History history) {
		this.history = history;
	}
	
	public void redo(Object obj) {
		throw new UnsupportedOperationException("redo not implemented");
	}
	
	public void undo(Object obj) {
		FeatureEvent e = (FeatureEvent)obj;
		switch(e.type){
		case FeatureEvent.ADD_FEATURE:
			// Remove Features.
			removeFeatures(e.features);
			break;
		case FeatureEvent.REMOVE_FEATURE:
			// Add features back to the same location they were deleted from.
			Collection<Integer> indices = e.featureIndices.values();
			addFeatures(
				indices.toArray(new Integer[indices.size()]),
				e.features.toArray(new Feature[e.features.size()]));
			break;
		case FeatureEvent.CHANGE_FEATURE:
			// Set the old value of each feature/field pair in the event's valuesBefore map
			Map<Feature,Map<Field,Object>> attrMap = new LinkedHashMap<Feature,Map<Field,Object>>();
			for (Feature newfeat: e.valuesBefore.keySet()) {
				Map<Field,Object> oldatts = e.valuesBefore.get(newfeat);
				// This will be a copy of the attributes, only created and used if necessary
				Map<Field,Object> mutableAtts = null;
				for (Field f: newfeat.attributes.keySet()) {
					if (e.fields.contains(f) && !oldatts.containsKey(f)) {
						if (mutableAtts == null) {
							mutableAtts = new HashMap<Field,Object>(oldatts);
						}
						mutableAtts.put(f, null);
					}
				}
				attrMap.put(newfeat, mutableAtts == null ? oldatts : mutableAtts);
			}
			setAttributes(attrMap);
			break;
		case FeatureEvent.ADD_FIELD:
			// Remove Fields.
			for(Field field: e.fields) {
				removeField(field);
			}
			break;
		case FeatureEvent.REMOVE_FIELD:
			// Add Fields from wherever they were removed from.
			Collection<Integer> fieldIndices = e.fieldIndices.values();
			addFields(
				fieldIndices.toArray(new Integer[fieldIndices.size()]),
				e.fields.toArray(new Field[e.fields.size()]));
			// Restore deleted values also
			for (Feature f: e.valuesBefore.keySet()) {
				f.attributes.putAll(e.valuesBefore.get(f));
			}
			break;
		default:
			log.println("Unhandled FeatureEvent type "+e.type+" encountered. Ignoring!");
		}
	}
}

