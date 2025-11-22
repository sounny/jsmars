package edu.asu.jmars.layer.util.features;
import java.awt.Container;
import java.awt.Frame;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JDialog;

import edu.asu.jmars.util.HVector;

/**
 * This function takes a FeatureCollection, a Field to get diameter info from,
 * and a flag to indicate the scale and units of the values in the field, and it
 * will replace all of the paths on the feature with circles of the appropriate
 * size centered on each point.
 * 
 * This is particularly useful to convert point datasets with some kind of
 * radius indicator into circular polygons centered on those points, such as
 * planetary nomenclature or a csv file saved by the crater layer.
 */
public class Circularize {
	/**
	 * Replaces all of the geometry objects on the given feature collection with circles centered at each geometry center.
	 * The circles are approximated with polygons having the given number of vertices at a distance given by the value of
	 * the given field expressed in the given number of units.
	 * @param fc The collection to replace geometry on.
	 * @param field The field to 
	 * @param toKmRadius The circle radius must be known in kilometers, but the value of <code>field</code> may be in
	 * some other measurement. This value is a scalar that each field value will be multiplied with that should yield
	 * radius in kilometers.
	 * @param vertexCount The number of vertices.
	 */
	public void circularize(FeatureCollection fc, Field field, double toKmRadius, int vertexCount) {
		Map<Feature,Object> paths = new LinkedHashMap<Feature,Object>();
		for (Feature f: fc.getFeatures()) {
			// radius in kilometers, converted from whatever units it already is in
			double kmRadius = ((Number)f.getAttribute(field)).doubleValue() * toKmRadius;
			paths.put(f, getCirclePath(f.getPath(), kmRadius, vertexCount));
		}
		fc.setAttributes(Field.FIELD_PATH, paths);
	}
	
	/**
	 * Given an FPath, returns a circular polygon with the given number of
	 * vertices the given kilometer distance from the center of the given path.
	 */
	public static FPath getCirclePath(FPath path, double kmRadius, int vertexCount) {
		// get center of the feature in special west coordinates
		HVector center = new HVector(path.getSpatialWest().getCenter()).unit();
		// get the point of intersection between the ellipsoid and a ray from the center of mass toward the center of the feature
		HVector hit = HVector.intersectMars(HVector.ORIGIN, center);
		// convert radius from kilometers to radians by dividing out the magnitude in kilometers of the ellipsoid hit
		kmRadius /= hit.norm();
		// a point 'radius' radians away from 'center'
		HVector point = center.rotate(center.cross(HVector.Z_AXIS).unit(), kmRadius).unit();
		// rotate 'point' around 'center' once per vertex
		float[] coords = new float[vertexCount*2];
		for (int i = 0; i < vertexCount; i++) {
			double omega = Math.toRadians(i*5);
			HVector vertex = point.rotate(center, omega);
			coords[2*i] = (float)vertex.lonE();
			coords[2*i+1] = (float)vertex.latC();
		}
		return new FPath(coords, false, FPath.SPATIAL_EAST, true);
	}
	
	/** Shows the user a user interface to pick the field from the available numeric columns, and the type from the available types. */
	public static class CircularizeGui {
		/** Provides converters from common methods of describing circle size to the one internal form that we need. */
		public enum Units {
			DiameterMeters(.0005, "Diameter (m)"),
			DiameterKm(.5, "Diameter (km)"),
			RadiusKm(1, "Radius (km)"),
			RadiusMeters(.001, "Radius (m)");
			private final double scale;
			private final String name;
			private Units(double scale, String name) {
				this.scale = scale;
				this.name = name;
			}
			public double getScale() {
				return scale;
			}
			public String toString() {
				return name;
			}
		}
		
		/**
		 * Construct and show a new Frame, owned by and centered on the given
		 * parent. When the user hits OK on the dialog, the geoetry of all
		 * features in the given collection will be replaced with circles of a
		 * radius determined by the chosen field and units.
		 */
		public void view(Frame owner, FeatureCollection fc) {
			JDialog dialog = new JDialog(owner, "Replace geometry with circles centered on points", true);
			Container parent = dialog.getContentPane();
			
		}
	}
}
