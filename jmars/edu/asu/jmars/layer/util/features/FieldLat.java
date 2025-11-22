package edu.asu.jmars.layer.util.features;

import java.awt.FlowLayout;
import java.util.Collections;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.asu.jmars.layer.shape2.ColumnEditor;
import edu.asu.jmars.layer.shape2.ShapeLayer;

public class FieldLat extends CalculatedField {
	private static final long serialVersionUID = 1L;
	private static final Set<Field> fields = Collections.singleton(Field.FIELD_PATH);
	public FieldLat(String name) {
		super(name, Double.class);
	}
	public Set<Field> getFields() {
		return fields;
	}
	public Object getValue(ShapeLayer layer, Feature f) {
		return f.getPath().getSpatialEast().getCenter().getY();
	}
	public static class Factory extends FieldFactory<FieldLat> {
		public Factory() {
			super("Center Latitude", FieldLat.class, Double.class);
		}
		public JPanel createEditor(ColumnEditor editor, Field f) {
			JPanel out = new JPanel(new FlowLayout());
			out.add(new JLabel("<html>Computes geocentric latitude<br>of the center of the shape in<br>degrees north of the equator.</html>"));
			return out;
		}
		public FieldLat createField(Set<Field> fields) {
			return new FieldLat(getName());
		}
	}
}

