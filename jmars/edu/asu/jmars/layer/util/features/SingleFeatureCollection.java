package edu.asu.jmars.layer.util.features;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides a FeatureCollection where data is stored in a separate HashMap for each row.
 * 
 * Since many values will be null, key/value pairs where the value is null are not stored
 * in the individual hash maps.
 * 
 * Since the standard HashMap implementation is used, there is no connection between the
 * individual row hash maps and the columns.  Thus setting values on all rows and then
 * removing the column of those values does not actually free up any memory.
 */
public class SingleFeatureCollection extends AbstractFeatureCollection {
	protected HashMap<Field, Object> addData_impl(Map<Field, Object> data) {
		HashMap<Field,Object> hashMap;
		if (data instanceof HashMap<?,?>) {
			hashMap = (HashMap<Field,Object>)data;
		} else {
			hashMap = new HashMap<Field,Object>();
			for (Map.Entry<Field, Object> entry: data.entrySet()) {
				Object value = entry.getValue();
				if (value != null) {
					hashMap.put(entry.getKey(), value);
				}
			}
		}
		return hashMap;
	}
	protected void addFields_impl(Collection<Field> f) {
	}
	protected void removeData_impl(Map<Field, Object> data) {
	}
	protected void removeFields_impl(Collection<Field> f) {
		for (Feature feat: getFeatures()) {
			feat.attributes.keySet().removeAll(f);
		}
	}
}
