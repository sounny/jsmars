package edu.asu.jmars.layer.util.features;

import java.awt.FlowLayout;
import java.util.Collections;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.asu.jmars.layer.shape2.ColumnEditor;
import edu.asu.jmars.layer.shape2.ShapeLayer;

public class FieldLon extends CalculatedField {
	private static final long serialVersionUID = 1L;
	private static final Set<Field> fields = Collections.singleton(Field.FIELD_PATH);
	public FieldLon(String name) {
		super(name, Double.class);
	}
	public Set<Field> getFields() {
		return fields;
	}
	public Object getValue(ShapeLayer layer, Feature f) {
		return f.getPath().getSpatialEast().getCenter().getX();
	}
	public static class Factory extends FieldFactory<FieldLon> {
		public Factory() {
			super("Center Longitude", FieldLon.class, Double.class);
		}
		public JPanel createEditor(ColumnEditor editor, Field f) {
			JPanel out = new JPanel(new FlowLayout());
			out.add(new JLabel("<html>Computes longitude of the<br>center of the shape in degrees<br>east of the prime meridian.</html>"));
			return out;
		}
		public FieldLon createField(Set<Field> fields) {
			return new FieldLon(getName());
		}
	}
}
