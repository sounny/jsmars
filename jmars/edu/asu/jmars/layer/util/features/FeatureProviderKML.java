package edu.asu.jmars.layer.util.features;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Data;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.ExtendedData;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LabelStyle;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.LineStyle;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Pair;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.PolyStyle;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.Schema;
import de.micromata.opengis.kml.v_2_2_0.SchemaData;
import de.micromata.opengis.kml.v_2_2_0.SimpleData;
import de.micromata.opengis.kml.v_2_2_0.SimpleField;
import de.micromata.opengis.kml.v_2_2_0.Style;
import de.micromata.opengis.kml.v_2_2_0.StyleMap;
import de.micromata.opengis.kml.v_2_2_0.StyleSelector;
import de.micromata.opengis.kml.v_2_2_0.StyleState;
import de.micromata.opengis.kml.v_2_2_0.TimePrimitive;
import de.micromata.opengis.kml.v_2_2_0.TimeSpan;
import de.micromata.opengis.kml.v_2_2_0.TimeStamp;
import edu.asu.jmars.util.Util;

// TODO: highlight styles should be mapped to shape layer selection styles... which we can
// after FeatureProviders can provide a style map along with their features.

/**
 * Reads KML files. Does not support writing.  Does not support the following KML elements:
 * <ul>
 * <li>styleUrl - the local '#' references work, but external URLs do not.
 * <li>NetworkLink - these elements are ignored.
 * <li>colorMode - these elements are ignored ('normal' is assumed.)
 * <li>MultiGeometry - these placemarks are ignored.
 * <li>Model - these elements are skipped.
 * <li>Polygon with inner rings - the outer ring is used and the inner ring is ignored.
 * <li>Tour - these elements are ignored.
 * <li>StyleMap - these elements are ignored.
 * <li>Style - these elements must be defined earlier in the document than all references to them.
 * </ul>
 */
public final class FeatureProviderKML implements FeatureProvider {
	private static final Field nameField = new Field("Name", String.class);
	private static final Field descField = new Field("Description", String.class);
	private static final Field startField = new Field("Start Time", String.class);
	private static final Field endField = new Field("End Time", String.class);
	private static final Field whenField = new Field("Timestamp", String.class);
	
	private CoordinateReferenceSystem jmarsCRS;
	private CoordinateReferenceSystem kmlCRS;
	private MathTransform toJMars, fromJMars;

	public String getDescription() {
		return "Experimental KML Support";
	}
	
	public String getExtension() {
		return ".kml";
	}
	
	public boolean isFileBased() {
		return true;
	}
	
	public File[] getExistingSaveToFiles(FeatureCollection fc, String baseName) {
		return null;
	}
	
	public boolean isRepresentable(FeatureCollection fc) {
		return false;
	}
	
	public FeatureCollection load(String fileName) {
		try {
			kmlCRS = FeatureUtil.getKmlCRS();
			jmarsCRS = FeatureUtil.getJMarsCRS();
			toJMars = CRS.findMathTransform(kmlCRS, jmarsCRS, true);
			fromJMars = CRS.findMathTransform(jmarsCRS, kmlCRS, true);
		}
		catch(FactoryException ex){
			throw new RuntimeException("Could not initialize CRS/tranform (kmlCRS="+kmlCRS+"," +
					"jmarsCRS="+jmarsCRS+",toJMars="+toJMars+",fromJMars="+fromJMars+")", ex);
		}
		
		String content;
		try {
			content = Util.readResponse(new BufferedInputStream(new FileInputStream(new File(fileName))));
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("KML file could not be opened.", e);
		}
		if (content.contains("http://earth.google.com/kml/2.2")) {
			content = content.replaceAll("http://earth\\.google\\.com/kml/2.2","http://earth\\.google\\.com/kml/2.1"); //http://www.google.com/kml/ext/2.2");
		}
		Kml kml = Kml.unmarshal(content);
		FeatureCollection fc = new SingleFeatureCollection();
		if (kml == null) {
			throw new IllegalArgumentException("KML file could not be parsed, improper header or non-standard features in use?");
		}
		Map<String,Map<Field,Object>> styleMap = new HashMap<String,Map<Field,Object>>();
		Map<String,Map<String,Field>> schemaTypes = new HashMap<String,Map<String,Field>>();
		addElements(fc, kml.getFeature(), styleMap, schemaTypes);
		return fc;
	}
	
	private static void processStyle(Map<String,Map<Field,Object>> styleMap, String id, StyleSelector sel) {
		if (sel instanceof StyleMap) {
			StyleMap map = (StyleMap)sel;
			List<Pair> pairs = map.getPair();
			for (Pair pair: pairs) {
				if (pair.getKey().equals(StyleState.NORMAL)) {
					processStyle(styleMap, map.getId(), pair.getStyleSelector());
				}
			}
		} else if (sel instanceof Style) {
			Style s = (Style)sel;
			Map<Field,Object> fieldValues = new LinkedHashMap<Field,Object>();
			LabelStyle label = s.getLabelStyle();
			if (label != null) {
				Color c = makeColor(label.getColor());
				fieldValues.put(Field.FIELD_LABEL_COLOR, c);
				fieldValues.put(Field.FIELD_LABEL_BORDER_COLOR, getFurthest(c, Color.black, Color.white));
				fieldValues.put(Field.FIELD_LABEL_SIZE, label.getScale()*12);
			}
			LineStyle line = s.getLineStyle();
			if (line != null) {
				if (line.getColor() != null) {
					fieldValues.put(Field.FIELD_DRAW_COLOR, makeColor(line.getColor()));
				}
				if (line.getWidth() != 0) {
					fieldValues.put(Field.FIELD_LINE_WIDTH, line.getWidth());
				}
			}
			PolyStyle poly = s.getPolyStyle();
			if (poly != null) {
				if (poly.getColor() != null) {
					fieldValues.put(Field.FIELD_FILL_COLOR, makeColor(poly.getColor()));
				}
				fieldValues.put(Field.FIELD_FILL_POLYGON, poly.isFill());
				fieldValues.put(Field.FIELD_DRAW_OUTLINE, poly.isOutline());
			}
			styleMap.put(id, fieldValues);
		}
	}
	
	private final void addElements(FeatureCollection fc, Feature f, Map<String,Map<Field,Object>> styleMap, Map<String,Map<String,Field>> schemaMap) {
		for (StyleSelector sel: f.getStyleSelector()) {
			processStyle(styleMap, sel.getId(), sel);
		}
		if (f instanceof Document) {
			Document d = (Document)f;
			for (Schema s: d.getSchema()) {
				String schemaId = s.getId();
				for (SimpleField sf: s.getSimpleField()) {
					String fieldName = sf.getName();
					String fieldType = sf.getType();
					Class<?> fieldClass = null;
					if (fieldType.equalsIgnoreCase("string")) {
						fieldClass = String.class;
					} else if (fieldType.equalsIgnoreCase("int")) {
						fieldClass = Integer.class;
					} else if (fieldType.equalsIgnoreCase("uint")) {
						fieldClass = Long.class;
					} else if (fieldType.equalsIgnoreCase("short")) {
						fieldClass = Short.class;
					} else if (fieldType.equalsIgnoreCase("ushort")) {
						fieldClass = Integer.class;
					} else if (fieldType.equalsIgnoreCase("float")) {
						fieldClass = Float.class;
					} else if (fieldType.equalsIgnoreCase("double")) {
						fieldClass = Double.class;
					} else if (fieldType.equalsIgnoreCase("bool")) {
						fieldClass = Boolean.class;
					} else {
						// unrecognized type, skip this field
					}
					if (fieldClass != null) {
						Map<String,Field> map = schemaMap.get(schemaId);
						if (map == null) {
							schemaMap.put(schemaId, map = new HashMap<String,Field>());
						}
						Field oldField = map.get(fieldName);
						if (oldField == null) {
							map.put(fieldName, new Field(fieldName, fieldClass));
						} // else it is already there, skip redefining it
					} // else unrecognized type, skip
				}
			}
			for (Feature child: d.getFeature()) {
				addElements(fc, child, styleMap, schemaMap);
			}
		} else if (f instanceof Folder) {
			Folder d = (Folder)f;
			for (Feature child: d.getFeature()) {
				addElements(fc, child, styleMap, schemaMap);
			}
		} else if (f instanceof Placemark) {
			Placemark p = (Placemark)f;
			// since we don't internally support MultiGeometry, combine the attributes in 'row' with each Geometry we find
			List<FPath> paths = new ArrayList<FPath>();
			edu.asu.jmars.layer.util.features.Feature row = new edu.asu.jmars.layer.util.features.Feature();
			row.setAttribute(nameField, p.getName());
			row.setAttribute(descField, p.getDescription());
			TimePrimitive time = p.getTimePrimitive();
			if (time instanceof TimeSpan) {
				TimeSpan span = (TimeSpan)time;
				row.setAttribute(startField, span.getBegin());
				row.setAttribute(endField, span.getEnd());
			} else if (time instanceof TimeStamp) {
				TimeStamp ts = (TimeStamp)time;
				row.setAttribute(whenField, ts.getWhen());
			}
			List<Geometry> geomList = new ArrayList<Geometry>();
			addGeometry(geomList, p.getGeometry());
			for (Geometry geom: geomList) {
				if (geom instanceof Point) {
					Point g = (Point)geom;
					paths.add(makePath(g.getCoordinates(), false));
				} else if (geom instanceof LineString) {
					LineString line = (LineString)geom;
					paths.add(makePath(line.getCoordinates(), false));
				} else if (geom instanceof LinearRing) {
					paths.add(getRing((LinearRing)geom));
				} else if (geom instanceof Polygon) {
					Polygon poly = (Polygon)geom;
					paths.add(getRing(poly.getOuterBoundaryIs().getLinearRing()));
				} else {
					// ignore types we don't understand
				}
			}
			String styleUrl = p.getStyleUrl();
			if (styleUrl != null && styleUrl.startsWith("#")) {
				styleUrl = styleUrl.substring(1);
				if (styleMap.containsKey(styleUrl)) {
					row.attributes.putAll(styleMap.get(styleUrl));
				}
			}
			ExtendedData edata = p.getExtendedData();
			if (edata != null) {
				for (Data d: edata.getData()) {
					String dName = d.getName();
					String dValue = d.getValue();
					// the empty string represents the schema of Data elements
					Map<String,Field> map = schemaMap.get("");
					if (map == null) {
						schemaMap.put("", map = new HashMap<String,Field>());
					}
					Field fld = map.get(dName);
					if (fld == null) {
						map.put(dName, fld = new Field(dName, String.class));
					}
					row.setAttribute(fld, dValue);
				}
				for (SchemaData sd: edata.getSchemaData()) {
					String schemaId = sd.getSchemaUrl();
					if (schemaId.startsWith("#")) {
						schemaId = schemaId.substring(1);
						Map<String,Field> schemaFields = schemaMap.get(schemaId);
						if (schemaFields != null) {
							for (SimpleData sdata: sd.getSimpleData()) {
								String name = sdata.getName();
								String value = sdata.getValue();
								Field fld = schemaFields.get(name);
								if (fld != null) {
									Object cellValue = null;
									try {
										if (fld.type.equals(String.class)) {
											cellValue = value;
										} else if (fld.type.equals(Integer.class)) {
											cellValue = Integer.valueOf(value);
										} else if (fld.type.equals(Long.class)) {
											cellValue = Long.valueOf(value);
										} else if (fld.type.equals(Boolean.class)) {
											if (value.equalsIgnoreCase("true") || value.equals("1")) {
												cellValue = Boolean.TRUE;
											} else if (value.equalsIgnoreCase("false") || value.equals("0")) {
												cellValue = Boolean.FALSE;
											} // else unrecognized boolean value, skip this field
										} else if (fld.type.equals(Float.class)) {
											cellValue = Float.valueOf(value);
										} else if (fld.type.equals(Double.class)) {
											cellValue = Double.valueOf(value);
										} // else skip this field since we don't know how to convert it
									} catch (Exception e) {
										// any kind of parse exception causes us to skip this field
									}
									if (cellValue != null) {
										row.setAttribute(fld, cellValue);
									}
								} // else skip this SchemaData since we can't find a Field for it
							}
						} // else skip this SchemaData since we can't find the schema for it
					} // else skip this SchemaData since we only handle internal schemaUri values
				}
			}
			for (int i = 0; i < paths.size(); i++) {
				row.setPath(paths.get(i));
				fc.addFeature(row);
				if (i+1 < paths.size()) {
					row = row.clone();
				}
			}
		}
	}
	
	private static void addGeometry(List<Geometry> geomList, Geometry geom) {
		if (geom instanceof MultiGeometry) {
			MultiGeometry mgeom = (MultiGeometry)geom;
			for (Geometry g: mgeom.getGeometry()) {
				addGeometry(geomList, g);
			}
		} else {
			geomList.add(geom);
		}
	}
	
	private static Color makeColor(String kmlColor) {
		int pos = 0;
		int a = 255;
		if (kmlColor.length() == 8) {
			a = Integer.parseInt(kmlColor.substring(pos, pos + 2), 16);
			pos += 2;
		}
		int b = Integer.parseInt(kmlColor.substring(pos, pos + 2), 16);
		pos += 2;
		int g = Integer.parseInt(kmlColor.substring(pos, pos + 2), 16);
		pos += 2;
		int r = Integer.parseInt(kmlColor.substring(pos, pos + 2), 16);
		pos += 2;
		return new Color(r, g, b, a);
	}
	
	/** @return the choice furthest from the given base color as measured by euclidian distance in HSB space. */
	private static Color getFurthest(Color base, Color ... choices) {
		float[] baseBits = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
		double bestDist = 0;
		Color bestColor = base;
		float[] choiceBits = new float[baseBits.length];
		for (Color c: choices) {
			Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), choiceBits);
			double dist = 0;
			for (int i = 0; i < baseBits.length; i++) {
				dist += (choiceBits[i]-baseBits[i])*(choiceBits[i]-baseBits[i]);
			}
			if (dist > bestDist) {
				bestColor = c;
				bestDist = dist;
			}
		}
		return bestColor;
	}
	
	private final FPath makePath(List<Coordinate> coords, boolean closed) {
		return new FPath(toJMars(getArr(coords)), false, FPath.SPATIAL_EAST, closed);
	}
	
	private final FPath getRing(LinearRing ring) {
		List<Coordinate> coords = ring.getCoordinates();
		return makePath(coords.subList(0, coords.size()-1), true);
	}

	/**
	 * Converts coordinates from ographic (KML) to ocentric (JMARS) coordinates. 
	 * @param coords
	 * @return
	 */
	private final float[] toJMars(float coords[]){
		try {
			toJMars.transform(coords, 0, coords, 0, coords.length/2);
			return coords;
		}
		catch(TransformException ex){
			throw new RuntimeException(ex);
		}
	}
	
	private static float[] getArr(List<Coordinate> coords) {
		float[] arr = new float[coords.size()*2];
		int pos = 0;
		for (Coordinate coord: coords) {
			arr[pos++] = (float)coord.getLongitude();
			arr[pos++] = (float)coord.getLatitude();
		}
		return arr;
	}
	
	public int save(FeatureCollection fc, String fileName) {
		throw new UnsupportedOperationException();
	}

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
}
