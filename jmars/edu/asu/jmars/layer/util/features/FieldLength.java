package edu.asu.jmars.layer.util.features;

import java.awt.FlowLayout;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.asu.jmars.layer.shape2.ColumnEditor;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.util.HVector;

public class FieldLength extends CalculatedField {
	private static final long serialVersionUID = 1L;
	private static final Set<Field> fields = Collections.singleton(Field.FIELD_PATH);
	public FieldLength(String name) {
		super(name, Double.class);
	}
	public Set<Field> getFields() {
		return fields;
	}
	public Object getValue(ShapeLayer layer, Feature f) {
		Point2D[] points = f.getPath().getSpatialWest().getVertices();
		double km = 0;
		for (int i = 1; i < points.length; i++) {
			HVector a = HVector.intersectMars(HVector.ORIGIN, new HVector(points[i-1]));
			HVector b = HVector.intersectMars(HVector.ORIGIN, new HVector(points[i]));
			km += b.sub(a).norm();
		}
		if (f.getPath().getType() == FPath.TYPE_POLYGON) {
            //The Points2D array does not contain a final entry to close the polygon. Here, we will 
            //add the final segment to get the correct perimeter calculation.
            HVector a = HVector.intersectMars(HVector.ORIGIN, new HVector(points[points.length-1]));
            HVector b = HVector.intersectMars(HVector.ORIGIN, new HVector(points[0]));
            km += b.sub(a).norm();
        }
		return km;
	}
	public static class Factory extends FieldFactory<FieldLength> {
		public Factory() {
			super("Perimeter", FieldLength.class, Double.class);
		}
		public JPanel createEditor(ColumnEditor editor, Field f) {
			JPanel out = new JPanel(new FlowLayout());
			out.add(new JLabel("<html>Computes length of all edges<br>in kilometers.</html>"));
			return out;
		}
		public FieldLength createField(Set<Field> fields) {
			return new FieldLength(getName());
		}
	}
}
