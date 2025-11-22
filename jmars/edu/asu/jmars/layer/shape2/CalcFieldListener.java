package edu.asu.jmars.layer.shape2;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;

import edu.asu.jmars.layer.shape2.ShapeLayer.LEDStateProcessing;
import edu.asu.jmars.layer.util.features.CalculatedField;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureEvent;
import edu.asu.jmars.layer.util.features.FeatureListener;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.util.History;
import edu.asu.jmars.util.Versionable;

/**
 * Manages the calculated fields for the shape layer, both handling version
 * changes in the field => calcfield mapping, and updating features when
 * dependent fields change
 */
public class CalcFieldListener implements Versionable, FeatureListener {
	private final FeatureCollection fc;
	private final Map<Field,CalculatedField> calculatedFields;
	private final ShapeLayer layer;
	public CalcFieldListener(FeatureCollection fc, ShapeLayer layer) {
		this(fc, layer, new HashMap<Field,CalculatedField>());
	}
	public CalcFieldListener(FeatureCollection fc, ShapeLayer layer, Map<Field,CalculatedField> calcFields) {
		this.fc = fc;
		this.layer = layer;
		this.calculatedFields = calcFields;
	}
	public void setCalculatedFields(Map<Field,CalculatedField> calcFields) {
		Map<Boolean,Map<Field,CalculatedField>> change = new HashMap<Boolean,Map<Field,CalculatedField>>();
		change.put(false, new HashMap<Field,CalculatedField>(calculatedFields));
		change.put(true, calcFields);
		layer.getHistory().addChange(this, change);
		redo(change);
	}
	public Map<Field,CalculatedField> getCalculatedFields() {
		return calculatedFields;
	}
	/** unused, history is passed to ctor */
	public void setHistory(History history) {
	}
	public void undo(Object obj) {
		handle(obj, false);
	}
	public void redo(Object obj) {
		handle(obj, true);
	}
	private void handle(Object obj, boolean after) {
		Map<Boolean,Map<Field,CalculatedField>> change = (Map<Boolean,Map<Field,CalculatedField>>)obj;
		calculatedFields.clear();
		calculatedFields.putAll(change.get(after));
	}
	public void receive(FeatureEvent e) {
		Collection<Field> fields = (e.type == FeatureEvent.ADD_FEATURE ? e.source.getSchema() : e.fields);
		if (fields != null && e.features != null) {
			Map<Field,CalculatedField> toUpdate = null;
			for (Field f: calculatedFields.keySet()) {
				CalculatedField c = calculatedFields.get(f);
				if (!Collections.disjoint(fields, c.getFields()) ||
						(c.getFields().contains(Field.FIELD_PATH) &&
						 !Collections.disjoint(
							fields,
							layer.getStylesLive().geometry.getSource().getFields()))) {
					if (toUpdate == null) {
						toUpdate = new HashMap<Field,CalculatedField>();
					}
					toUpdate.put(f, c);
				}
			}
			if (toUpdate != null) {
				updateValues(e.features, toUpdate);
			}
		}
	}
	
	private Thread workingThread;
	private final List<Collection<Feature>> workingFeatures = new LinkedList<Collection<Feature>>();
	private final List<Map<Field,CalculatedField>> workingFields = new LinkedList<Map<Field,CalculatedField>>();
	
	/**
	 * Applies auto calculations to the given features and fields. The
	 * auto-calculations can take a long time, and may require a lot of memory,
	 * so we set the status LED, get off the AWT event thread, and set values
	 * once every second or so. When all values have been set, the status LED is
	 * cleared. If a FeatureEvent arrives in the middle of processing that
	 * invalidates any previously submitted results, those Feature instances are
	 * re-scheduled for computation.
	 */
	public synchronized void updateValues(Collection<Feature> features, Map<Field,CalculatedField> fieldMap) {
		if (features.isEmpty() || fieldMap.isEmpty()) {
			return;
		}
		
		workingFeatures.add(features);
		workingFields.add(fieldMap);
		
		if (workingThread == null) {
			workingThread = new Thread(new Runnable() {
				private final Map<Feature,Map<Field,Object>> feat2fld2val = new HashMap<Feature,Map<Field,Object>>();
				private final LEDStateProcessing led = new ShapeLayer.LEDStateProcessing();
				private long lastTime = System.currentTimeMillis();
				
				/**
				 * Will remove and run calculations from the
				 * workingFeatures/workingFields parallel lists until there is
				 * no more work to do.
				 */
				public void run() {
					layer.begin(led);
					while (true) {
						try {
							if (!run_impl()) {
								break;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					layer.end(led);
				}
				
				/** @return false if there is nothing to do */
				private boolean run_impl() {
					// remove the next set of features/fields to process, or
					// return false when finished
					Collection<Feature> features;
					Map<Field,CalculatedField> fieldMap;
					synchronized(CalcFieldListener.this) {
						if (workingFeatures.isEmpty()) {
							workingThread = null;
							return false;
						}
						features = workingFeatures.remove(0);
						fieldMap = workingFields.remove(0);
					}
					
					// Dispatch results to the feature collection every half a
					// second, or every 10 seconds if one of the fields is used
					// in shape rendering.
					int delay = Collections.disjoint(layer.getStyles().getFields(), fieldMap.keySet()) ? 500 : 10000;
					
					for (Feature feature: features) {
						Map<Field,Object> values;
						if (fieldMap.size() == 1) {
							Entry<Field,CalculatedField> entry = fieldMap.entrySet().iterator().next();
							values = Collections.singletonMap(
								entry.getKey(),
								entry.getValue().getValue(layer, feature));
						} else {
							values = new HashMap<Field,Object>();
							for (Entry<Field,CalculatedField> entry: fieldMap.entrySet()) {
								values.put(entry.getKey(), entry.getValue().getValue(layer, feature));
							}
						}
						feat2fld2val.put(feature, values);
						// Send event after 50,000 cells or enough time has passed
						if (feat2fld2val.size() > 50000/fieldMap.size() || System.currentTimeMillis() - lastTime > delay) {
							send();
						}
					}
					if (!feat2fld2val.isEmpty()) {
						send();
					}
					return true;
				}
				
				/** dispatch results to the feature collection */
				private void send() {
					try {
						final Map<Feature,Map<Field,Object>> copy = new HashMap<Feature,Map<Field,Object>>(feat2fld2val);
						// get onto the AWT thread to report the current results
						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								fc.setAttributes(copy);
							}
						});
					} catch (Exception e) {
						// interruption or invocation errors shouldn't
						// occur, but let's print them in case they do
						e.printStackTrace();
					}
					feat2fld2val.clear();
					lastTime = System.currentTimeMillis();
				}
			});
			workingThread.setDaemon(true);
			workingThread.setPriority(Thread.MIN_PRIORITY);
			workingThread.setName("CalcFieldThread-" + System.currentTimeMillis());
			workingThread.start();
		}
	}
}

