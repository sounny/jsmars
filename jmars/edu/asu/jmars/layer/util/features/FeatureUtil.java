package edu.asu.jmars.layer.util.features;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

/**
 * Helper methods that several classes in the Feature Framework might make use
 * of.
 */

public class FeatureUtil  {
	private static DebugLog log = DebugLog.instance();
	
	/**
	 * This is to prevent some ne'er-do-well from coming in and trying to
	 * instanciate what is supposed to be class of nothing but static methods.
	 */
	private FeatureUtil(){}

	/**
	 * Returns the rectangle of the given width centered on the given point.
	 */
	public static Rectangle2D getClickBox( Point2D p, int width){
		Rectangle2D r =  new Rectangle2D.Float();
		r.setRect( p.getX() - width/2, 
			   p.getY() - width/2, 
			   (double)width, 
			   (double)width);
		return r;
	}

	/**
	 * Return the Feature types represented in this FeatureCollection.
	 * @return An non-null array of Feature types corresponding to their
	 *         representation in this collection.
	 */
	public static int[] getRepresentedFeatureTypes(Collection features){
		int[] rep = new int[4];
		
		for(Iterator i=features.iterator(); i.hasNext(); ){
			Feature f = (Feature)i.next();
			int pathType = f.getPath().getType();
			
			if (pathType < 0 || pathType >= rep.length)
				throw new Error("Unhandled pathType encountered "+pathType);
			
			rep[pathType]++;
		}
		
		int count = 0;
		for(int i=0; i<rep.length; i++)
			if (rep[i] > 0)
				count ++;
		
		int[] repTypes = new int[count];
		for(int i=0; i<rep.length; i++)
			if (rep[i] > 0)
				repTypes[--count] = i;
		
		return repTypes;
	}

	/**
	 * Returns the count of Features that are of the specified type.
	 * @param features List of Feature objects.
	 * @param featureType Type of feature as returned by {@link Feature#getType()}.
	 * @return Total number of features of the specified featureType found in the
	 *         given collection.
	 */
	public static int countFeatures(Collection features, int featureType){
		int count = 0;
		for(Iterator i=features.iterator(); i.hasNext(); ){
			Feature f = (Feature)i.next();
			if (f.getPath().getType() == featureType)
				count++;
		}
		return count;
	}

	/**
	 * Returns the index of every Feature in the features as it is listed in
	 * featureList. This is equivalent to:
	 * <code>
	 * for each feature in (features){
	 *     feature2IndexMap.put(feature, featureList.indexOf(feature))
	 * }
	 * </code>
	 * @param within Indices within this list are returned.
	 * @param of Feature objects for which indices are to be determined.
	 * @return Map<Feature,Integer>. 
	 */
	public static Map<Feature,Integer> getFeatureIndices(List<Feature> within, Collection<Feature> of){
		Set<Feature> featureSet = new HashSet<Feature>(of);
		Map<Feature,Integer> featureIndices = new HashMap<Feature,Integer>();
		int i = 0;
		for(Feature feat: within) {
			if (featureSet.contains(feat)) {
				featureIndices.put(feat, i);
			}
			i++;
		}
		return featureIndices;
	}
	
	/**
	 * Returns the index of every Field in the fields as it is listed in 
	 * the fieldList. This is equivalent to:
	 * <code>
	 * for each field in (fields){
	 *    field2IndexMap.put(field, fieldList.indexOf(field))
	 * }
	 * </code>
	 * @param within Indices within this list are returned.
	 * @param of Fields for which indices are to be determined.
	 * @return Map<Field,Integer>.
	 */
	public static Map<Field,Integer> getFieldIndices(List<Field> within, Collection<Field> of){
		Set<Field> fieldSet = new HashSet<Field>(of);
		Map<Field,Integer> fieldIndices = new HashMap<Field,Integer>();
		int i = 0;
		for(Field field: within) {
			if (fieldSet.contains(field)) {
				fieldIndices.put(field, i);
			}
			i++;
		}
		return fieldIndices;
	}

	/**
	 * Splits a string into floats using any of the characters in the delim
	 * string as delimiters.
	 */
	public static float[] stringToFloats(String s, String delim){
		StringTokenizer tokenizer = new StringTokenizer(s, delim);
		
		ArrayList list = new ArrayList();
		while(tokenizer.hasMoreTokens())
			list.add(new Float(tokenizer.nextToken()));
		
		float[] floats = new float[list.size()];
		for(int i=0; i<list.size(); i++)
			floats[i] = ((Float)list.get(i)).floatValue();
		
		return floats;
	}
	
    public static final String TYPE_STRING_POINT = "point";
    public static final String TYPE_STRING_POLYLINE = "polyline";
    public static final String TYPE_STRING_POLYGON = "polygon";
    public static final String TYPE_STRING_ELLIPSE = "ellipse";
    public static final String TYPE_STRING_INVALID = "invalid";
    
    /**
     * Returns string giving the type of Feature given the FPath.TYPE_*
     * value as input.
     * 
     * @param type One of the FPath.TYPE_* values.
     * @return String representations of the TYPE_*.
     */
    public static String getFeatureTypeString(int type){
    	switch(type){
    		case FPath.TYPE_POINT: return TYPE_STRING_POINT;
    		case FPath.TYPE_POLYLINE: return TYPE_STRING_POLYLINE;
    		case FPath.TYPE_POLYGON: return TYPE_STRING_POLYGON;
    		case FPath.TYPE_ELLIPSE: return TYPE_STRING_ELLIPSE;
    	}
    	return TYPE_STRING_INVALID;
    }

    /**
     * Returns a longitude in the interval [0.0, 360.0).
     * This method should not be used on longitudes that are
     * more than 360 degrees outside the included interval;
     * it will return the expected result, but performance will
     * be very poor.
     */
	public static final double lonNorm (double lon) {
		while (lon < 0.0) lon += 360.0;
		while (lon >= 360.0) lon -= 360.0;
		return lon;
	}
	
	/**
	 * @return true if the given feature is stored as a point, and has a
	 * non-zero circle size attribute value.
	 */
	public static final boolean isCircle(Style<FPath> geomStyle, Feature f) {
		FPath path = f.getPath();
		if (path.getType() != FPath.TYPE_POINT) {
			return false;
		}
		StyleSource<?> source = geomStyle.getSource();
		if (!(source instanceof GeomSource)) {
			return false;
		}
		Object size = f.getAttribute(((GeomSource)source).getRadiusField());
		return size instanceof Number && ((Number)size).doubleValue() > 0;
	}
	
	public static boolean isEllipseSource(GeomSource gs, Feature f){
		if(f.getAttribute(gs.getAAxisField()) != null && f.getAttribute(gs.getBAxisField()) != null 
			&& f.getAttribute(gs.getAngleField()) != null && f.getAttribute(gs.getLatField()) != null
			&& f.getAttribute(gs.getLonField()) != null){
			return true;
		}
		return false;
	}

	/**
	 * Projection stuff common to many feature-providers
	 */
	private static final String KEY_MEAN_RADIUS = "mean_radius";
	private static final String KEY_EQUAT_RADIUS = "equat_radius";
	private static final String KEY_POLAR_RADIUS = "polar_radius";
	private static final String KEY_BODY_NAME = "bodyname";
	private static final String KEY_DEFAULT_OUTPUT_CRS_WKT = "shape.output.crs.wkt";
	
	/**
	 * @return "WGS84" CRS for Earth products or the JMARS CRS
	 * from {@link #getJMarsCRS()} otherwise.
	 */
	public static CoordinateReferenceSystem getKmlCRS() throws FactoryException {
		String body = Config.get(Util.getProductBodyPrefix()+KEY_BODY_NAME);
		if ("earth".equalsIgnoreCase(body))
			return CRS.decode("EPSG:4326");
		else
			return getJMarsCRS();
	}
	
	/**
	 * @return The geographical coordinate system on a sphere of radius
	 * from the {@value #KEY_MEAN_RADIUS} jmars.config key. If the key
	 * does not exist or has an invalid value a default mean radius of 
	 * 180/pi is assumed.
	 */
	public static CoordinateReferenceSystem getJMarsCRS() throws FactoryException {
		CoordinateReferenceSystem jmarsCRS = null;
		
		String meanRadiusStr = Config.get(Util.getProductBodyPrefix()+KEY_MEAN_RADIUS);
		double meanRadius;
		if (meanRadiusStr != null){
			log.println("Found "+KEY_MEAN_RADIUS+"="+meanRadiusStr);
			meanRadius = Double.parseDouble(meanRadiusStr)*1000; // convert to meters
		}
		else {
			meanRadius = 180.0/Math.PI; 
			log.aprintln(KEY_MEAN_RADIUS+" was not found in the config. Assuming: "+meanRadius);
		}

		// TODO On going conversation as to whether using the mean-radius is the right
		// thing to do or whether we should specify towgs84 parameters here.
		jmarsCRS = CRS.parseWKT(
				"GEOGCS[\"unknown\",DATUM[\"unknown\",SPHEROID[\"unknown\"," +
						new DecimalFormat("#.#########").format(meanRadius) +
						",0]],PRIMEM[\"unknown\",0],UNIT[\"Degree\"," +
						new DecimalFormat("#.###############").format(Math.PI/180) + "]]");

		return jmarsCRS;
	}
	
	/**
	 * @return The default output CRS as specified in the jmars.config file.
	 * If there is none, one is built by using the mean radius 
	 * (from {@value #KEY_MEAN_RADIUS}).
	 */
	public static CoordinateReferenceSystem getDefaultOutputCRS() throws FactoryException {
		CoordinateReferenceSystem defaultOutputCRS = null;

		String outputCrsWkt = Config.get(Util.getProductBodyPrefix()+KEY_DEFAULT_OUTPUT_CRS_WKT);
		if (outputCrsWkt != null){
			defaultOutputCRS = CRS.parseWKT(outputCrsWkt);
		}
		else {
			// Get the body name for this version of JMARS
			String jmarsBody = Config.get(Util.getProductBodyPrefix()+KEY_BODY_NAME);
			
			// Retrieve mean radius. It is specified in kilometers in the jmars.config
			double a = Config.get(Util.getProductBodyPrefix()+KEY_MEAN_RADIUS, 1.0) * 1000.0;
			double b = Config.get(Util.getProductBodyPrefix()+KEY_MEAN_RADIUS, 1.0) * 1000.0;
			double invFlattening = Math.abs(a-b) < 1E-6? 0: a/(a-b);
			String equatRadiusStr = new DecimalFormat("#.######").format(a);
			String invFlatteningStr = new DecimalFormat("#.###############").format(invFlattening);
			defaultOutputCRS = CRS.parseWKT(
					"GEOGCS[\"GCS_"+jmarsBody+"\",DATUM[\"D_"+jmarsBody+"\","+
							"SPHEROID[\"S_"+jmarsBody+"\","+equatRadiusStr+","+invFlatteningStr+"]],"+
					"PRIMEM[\"Reference_Meridian\",0.0],UNIT[\"Degree\",0.0174532925199433]]");
		}

		return defaultOutputCRS;
	}
	
	public static Object calculateLineAngle(FPath path) {
		Shape gp = path.getShape();
		float[] coords = new float[6];
		float[] a = new float[2];
		float[] b = new float[2];
		int i = 0;
		for (PathIterator pi = gp.getPathIterator(null); !pi.isDone(); pi.next()) {
			switch (pi.currentSegment(coords)) {
			case PathIterator.SEG_MOVETO:
			case PathIterator.SEG_LINETO:
				if (i > 0) {
					System.arraycopy(b, 0, a, 0, 2);
				}
				System.arraycopy(coords, 0, b, 0, 2);
				i++;
				if (i > 2) {
					return null;
				}
				break;
			case PathIterator.SEG_CLOSE:
				break;
			default:
				return null;
			}
		}
		
		if (i == 2) {
//			System.out.println("a[0] / b[0]: "+a[0]+" / "+ b[0]);
			// the formula for initial compass bearing is:
			// theta = atan2(sin(deltalong)*cos(lat2),cos(lat1)*sin(lat2)-sin(lat1)*cos(lat2)*cos(deltalong))
			// from initial(a to b), final bearing is (initial(b to a)+180)%360
			double dlon = Math.toRadians(a[0] - b[0]);
//			System.out.println("dlon: "+dlon);
			double sin1 = Math.sin(Math.toRadians(b[1]));
//			System.out.println("sin1: "+sin1);
			double cos1 = Math.cos(Math.toRadians(b[1]));
//			System.out.println("cos1: "+cos1);
			double sin2 = Math.sin(Math.toRadians(a[1]));
//			System.out.println("sin2: "+sin2);
			double cos2 = Math.cos(Math.toRadians(a[1]));
//			System.out.println("cos2: "+cos2);
			double theta = Math.atan2(Math.sin(dlon)*cos2, cos1*sin2-sin1*cos2*Math.cos(dlon));
//			System.out.println("theta: "+theta);
			return (Math.toDegrees(theta) + 180)%360;
		} else {
			return null;
		}
	}
	
} // end: class FeatureUtil
