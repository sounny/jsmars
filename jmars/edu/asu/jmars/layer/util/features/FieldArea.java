package edu.asu.jmars.layer.util.features;

import java.util.Collections;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.asu.jmars.layer.shape2.ColumnEditor;
import edu.asu.jmars.layer.shape2.ShapeLayer;

public class FieldArea extends CalculatedField {
	private static final long serialVersionUID = 1L;
	private static final Set<Field> fields = Collections.singleton(Field.FIELD_PATH);
	public FieldArea(String name) {
		super(name, Double.class);
	}
	public Set<Field> getFields() {
		return fields;
	}
	public Object getValue(ShapeLayer layer, Feature f) {
		return new Double (layer.getStylesLive().geometry.getValue(f).getSpatialWest().getArea());
	}
	public static class Factory extends FieldFactory<FieldArea> {
		public Factory() {
			super("Enclosed Area", FieldArea.class, Double.class);
		}
		public JPanel createEditor(ColumnEditor editor, Field f) {
			JPanel out = new JPanel();
			out.add(new JLabel("<html>Computes polygonal area<br>in square kilometers</html>"));
			return out;
		}
		public FieldArea createField(Set<Field> fields) {
			return new FieldArea(getName());
		}
	}
}
