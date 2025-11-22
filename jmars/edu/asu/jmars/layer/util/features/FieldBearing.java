package edu.asu.jmars.layer.util.features;

import java.awt.Shape;
import java.util.Collections;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.asu.jmars.layer.shape2.ColumnEditor;
import edu.asu.jmars.layer.shape2.ShapeLayer;

public class FieldBearing extends CalculatedField {
	private static final long serialVersionUID = 1L;
	private static final Set<Field> fields = Collections.singleton(Field.FIELD_PATH);
	public FieldBearing(String name) {
		super(name, Double.class);
	}
	public Set<Field> getFields() {
		return fields;
	}
	public Object getValue(ShapeLayer layer, Feature f) {
		return FeatureUtil.calculateLineAngle(f.getPath().getSpatialEast());
	}
	public static class Factory extends FieldFactory<FieldBearing> {
		public Factory() {
			super("Line Direction", FieldBearing.class, Double.class);
		}
		public JPanel createEditor(ColumnEditor editor, Field f) {
			JPanel out = new JPanel();
			out.add(new JLabel("<html>Computes azimuth of a line<br>in degrees east of north.</html>"));
			return out;
		}
		public FieldBearing createField(Set<Field> fields) {
			return new FieldBearing(getName());
		}
	}
}
