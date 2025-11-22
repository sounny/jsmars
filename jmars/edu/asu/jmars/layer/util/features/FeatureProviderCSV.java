package edu.asu.jmars.layer.util.features;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

import edu.asu.jmars.util.FillStyle;
import edu.asu.jmars.util.LineType;
import edu.asu.jmars.util.ListType;
import edu.asu.jmars.util.Util;

public class FeatureProviderCSV implements FeatureProvider {
	public String getDescription() {
		return "CSV shape file";
	}
	
	public File[] getExistingSaveToFiles(FeatureCollection fc, String baseName) {
		String[] names = {baseName, baseName + getExtension()};
		for (String name: names) {
			File f = new File(name);
			if (f.exists()) {
				return new File[]{f};
			}
		}
		return new File[]{};
	}
	
	public String getExtension() {
		return ".csv";
	}
	
	public boolean isFileBased() {
		return true;
	}
	
	public boolean isRepresentable(FeatureCollection fc) {
		return true;
	}
	
	/** the names of longitude columns, in ascending order of preference */
	private static final List<String> lonAliases = Arrays.asList("lon","long","longitude (deg e)","longitude (e)","longitude");
	/** the names of latitude columns, in ascending order of preference */
	private static final List<String> latAliases = Arrays.asList("lat","latitude (deg n)","latitude (n)","latitude");
	/** the names of full geometry columns, one of which takes the place of lon/lat columns */
	private static final List<String> geomAliases = Arrays.asList("wkt","geometry");
	/** the names under which polar radius can be found */
	private static final String[] polarRadiusLabels = {"polar_radius", "c_axis_radius"};
	/** the names under which equat radius can be found */
	private static final String[] equatRadiusLabels = {"equat_radius", "a_axis_radius", "b_axis_radius"};
	/** the names under which longitude direction can be found */
	private static final String[] lonDirLabels = {"lon_dir", "longitude_direction", "positive_longitude_direction"};
	
	private enum Type {
		INT(Integer.class),
		LONG(Long.class),
		DBL(Double.class),
		STR(String.class),
		BOO(Boolean.class),
		LIN(LineType.class),
		COL(Color.class),
		FST(FillStyle.class);
		
		public final Class<?> c;
		public final String suffix;
		Type(Class<?> c) {
			this.c = c;
			String s = c.getName();
			int idx = s.lastIndexOf('.');
			if (idx >= 0) {
				s = s.substring(idx+1);
			}
			suffix = s.toLowerCase();
		}
		public Object fromString(String s) {
			switch (this) {
			case BOO: return new Scanner(s).nextBoolean();
			case INT: return new Scanner(s).nextInt();
			case LONG: return new Scanner(s).nextLong();
			case DBL: return new Scanner(s).nextDouble();
			case STR: return s;
			case LIN: return new LineType(new Scanner(s).nextInt());
			case COL:
				Scanner sc = new Scanner(s);
				return new Color(
					sc.nextInt(), sc.nextInt(), sc.nextInt(),
					sc.hasNext() ? sc.nextInt() : 255);
			case FST: return new FillStyle(new Scanner(s).next());
			default: return null;
			}
		}
		public String toString(Object value) {
			switch (this) {
			case BOO:
			case INT:
			case LONG:
			case DBL:
			case STR: return value.toString();
			case LIN: return ""+((LineType)value).getType();
			case COL:
				Color col = (Color)value;
				return col.getRed() + " " +
					col.getGreen() + " " +
					col.getBlue() +
					(col.getAlpha() == 255 ? "" : " " + col.getAlpha());
			case FST: return ""+((FillStyle)value).getType();
			default: return null;
			}
		}
		public static Type getMinType(String cell) {
			Scanner s = new Scanner(cell);
			Type type;
			if (s.hasNextBoolean()) {
				type = Type.BOO;
			} else if (s.hasNextInt()) {
				type = Type.INT;
			} else if (s.hasNextLong()) {
				type = Type.LONG;
			} else if (s.hasNextDouble()) {
				type = Type.DBL;
			} else {
				type = Type.STR;
			}
			// LineType and Color are not primitive types, but are rather
			// another interpretation of existing types, so there is no
			// point in checking for them here.
			return type;
		}
	}
	
	public FeatureCollection load(String fileName) {
		CsvReader csv = null;
		try {
			int lonCol = -1;
			int latCol = -1;
			int geomCol = -1;
			String[] names = null;
			int[] otherFields = null;
			int otherCount = 0;
			
			// try various delimiters to find the lon and lat columns
			char delim = '\0';
			for (char d: new char[]{',','\t','|'}) {
				lonCol = latCol = -1;
				otherCount = 0;
				csv = new CsvReader(new BufferedReader(new FileReader(fileName)), d);
				csv.setUseComments(true);
				csv.readHeaders();
				names = csv.getHeaders();
				otherFields = new int[names.length];
				
				// find most articulate lon and lat column names
				for (int i = 0; i < names.length; i++) {
					String name = names[i].trim().toLowerCase();
					int sep = name.indexOf(':');
					if (sep >= 0) {
						name = name.substring(0, sep);
					}
					if (lonAliases.contains(name)) {
						if (lonCol < 0 || lonAliases.indexOf(csv.getHeader(lonCol)) < lonAliases.indexOf(name)) {
							lonCol = i;
						}
					} else if (latAliases.contains(name)) {
						if(latCol < 0 || latAliases.indexOf(csv.getHeader(latCol)) < latAliases.indexOf(name)) {
							latCol = i;
						}
					} else if (geomAliases.contains(name)) {
						if (geomCol < 0 || geomAliases.indexOf(csv.getHeader(geomCol)) < geomAliases.indexOf(name)) {
							geomCol = i;
						}
					} else {
						otherFields[otherCount++] = i;
					}
				}
				csv.close();
				
				// if we found our geometry, then stop, otherwise try another delimiter
				if (geomCol >= 0 || (lonCol >= 0 && latCol >= 0)) {
					delim = d;
					break;
				}
			}
			
			if (delim == '\0') {
				throw new IllegalArgumentException("Could not find geometry columns");
			}
			
			// only use one or the other of geomCol and lonCol/latCol
			if (geomCol >= 0) {
				if (lonCol >= 0) {
					otherFields[otherCount++] = lonCol;
					lonCol = -1;
				}
				if (latCol >= 0) {
					otherFields[otherCount++] = latCol;
					latCol = -1;
				}
			}
			
			// parse the header comments and types in each cell
			Type[] types = new Type[otherCount];
			Arrays.fill(types, null);
			csv = new CsvReader(new FileReader(fileName), delim);
			double polarRadius = 1;
			double equatRadius = 1;
			boolean west = false;
			while (true) {
				csv.readHeaders();
				int cols = csv.getHeaderCount();
				String row = csv.getRawRecord().trim().toLowerCase().replaceAll("^[ \\t#]+", "");
				Double dvalue = null;
				String svalue = null;
				if (null != (dvalue = getDoubleLabel(polarRadiusLabels, row))) {
					polarRadius = dvalue.doubleValue();
				} else if (null != (dvalue = getDoubleLabel(equatRadiusLabels, row))) {
					if (equatRadius == 1) {
						equatRadius = dvalue.doubleValue();
					} else if (equatRadius != dvalue.doubleValue()) {
						throw new IllegalArgumentException("Two equatorial radii must be equal");
					}
				} else if (null != (svalue = getStringLabel(lonDirLabels, row))) {
					svalue = svalue.trim().toLowerCase();
					if (svalue.equals("w") || svalue.equals("west")) {
						west = true;
					} else if (svalue.equals("e") || svalue.equals("east")) {
						west = false;
					}
				} else if (cols == names.length) {
					// found header record, break out to start parsing normal records
					break;
				}
			}
			
			final double scalar;
			if (polarRadius == equatRadius) {
				scalar = 1;
			} else {
				scalar = Math.pow(polarRadius / equatRadius, 2);
			}
			
			// check header fields for types as well as names
			for (int i = 0; i < otherCount; i++) {
				// parse column names like name:type to avoid guessing types later
				String name = names[otherFields[i]];
				String[] bits = name.split(":");
				if (bits.length == 2) {
					String type = bits[1].toLowerCase();
					for (Type t: Type.values()) {
						if (t.suffix.equals(type)) {
							types[i] = t;
							names[otherFields[i]] = bits[0];
							break;
						}
					}
				}
			}
			
			// count unknown types so we can avoid investigating records if we don't have to
			int unknownTypes = 0;
			for (Type type: types) {
				if (type == null) {
					unknownTypes ++;
				}
			}
			
			// read data records, ignoring all comments
			csv.setUseComments(true);
			if (unknownTypes > 0) {
				while (csv.readRecord()) {
					if (names.length != csv.getColumnCount()) {
						throw new IllegalArgumentException("CSV header count does not match row " + csv.getCurrentRecord());
					}
					for (int i = 0; i < otherCount; i++) {
						Type lastType = types[i];
						// if no known type yet, or this column is more
						// restrictive than a string so we must still check
						// values to make sure none violate that restriction
						if (lastType == null || lastType.compareTo(Type.STR) < 0) {
							Type type = Type.getMinType(csv.get(otherFields[i]).trim());
							if (lastType == null || lastType.compareTo(type) < 0) {
								types[i] = type;
							}
						}
					}
				}
			}
			csv.close();
			
			// create the fields
			FeatureCollection fc = new SingleFeatureCollection();
			for (int i = 0; i < otherCount; i++) {
				fc.addField(new Field(names[otherFields[i]], types[i].c));
			}
			
			Field[] fields = fc.getSchema().toArray(new Field[fc.getSchema().size()]);
			List<Feature> features = new ArrayList<Feature>();
			
			/** hackery to modify the latitude values without copying the shape yet again */
			AffineTransform at = scalar == 1 ? null : new AffineTransform() {
				public void transform(float[] srcPts, int srcOff,
						float[] dstPts, int dstOff,
						int numPts) {
					// move up one to address the latitude values
					dstOff += 1;
					srcOff += 1;
					for (int i = 0; i < numPts; i++) {
						// convert geographic values to geocentric
						dstPts[dstOff] = (float)Util.atanD(Util.tanD(srcPts[srcOff]) * scalar);
						// move up to the next latitude value
						dstOff += 2;
						srcOff += 2;
					}
				}
			};
			
			// create the reader only if we actually need it, since it will trigger a lot of classes to init
			WKTReader wktReader = null;
			FPath.GeometryAdapter adapter = null;
			
			csv = new CsvReader(new FileReader(fileName), delim);
			csv.setUseComments(true);
			csv.readHeaders();
			while (csv.readRecord()) {
				String[] row = csv.getValues();
				Feature f = new Feature();
				for (int i = 0; i < otherCount; i++) {
					String cell = row[otherFields[i]].trim();
					if (cell.length() > 0) {
						Object o = types[i].fromString(cell);
						f.setAttribute(fields[i], o);
					}
				}
				if (geomCol >= 0) {
					if (wktReader == null) {
						wktReader = new WKTReader(new GeometryFactory());
						adapter = new FPath.GeometryAdapter();
					}
					Geometry geom = wktReader.read(row[geomCol]);
					Path2D.Double path = adapter.getPath(geom);
					if (at != null) {
						path.transform(at);
					}
					f.setPath(new FPath(path, FPath.SPATIAL_EAST).getSpatialWest());
				} else {
					double[] coords = {
						parseLon(row[lonCol], west),
						parseLat(row[latCol], true, scalar)};
					f.setPath(new FPath(coords, false, FPath.SPATIAL_EAST, false));
				}
				features.add(f);
			}
			fc.addFeatures(features);
			return fc;
		} catch (Exception e) {
			throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
		} finally {
			if (csv != null) {
				csv.close();
			}
		}
	}
	
	// Do NOT make this static.  If it is static, it is no longer thread-safe, due to the use of the ParsePosition parameter
	private final DecimalFormat fmt = new DecimalFormat("#.#####");
	
	/**
	 * returns the east longitude from the given string, handling 'E' and 'W'
	 * suffixes, and using the given value of 'west' as the default when no
	 * suffix is present.
	 */
	private float parseLon(String lon, boolean west) {
		lon = lon.toLowerCase();
		ParsePosition pos = new ParsePosition(0);
		float f = fmt.parse(lon, pos).floatValue();
		if (west && lon.indexOf("e", pos.getIndex()) >= 0) {
			west = false;
		} else if (!west && lon.indexOf("w", pos.getIndex()) >= 0) {
			west = true;
		}
		if (west) {
			f = -f;
		}
		return f;
	}
	
	/**
	 * returns the ocentric latitude from the given string, handling 'N' and 'S'
	 * suffixes if present, converting the value from ographic to ocentric with
	 * the given ellipsoidal scalar.
	 */
	private float parseLat(String lat, boolean north, double scalar) {
		lat = lat.toLowerCase();
		ParsePosition pos = new ParsePosition(0);
		float f = fmt.parse(lat, pos).floatValue();
		if (north && lat.indexOf("s", pos.getIndex()) >= 0) {
			north = false;
		} else if (!north && lat.indexOf("n", pos.getIndex()) >= 0) {
			north = true;
		}
		if (!north) {
			f = -f;
		}
		if (scalar != 1) {
			f = (float)Util.atanD(Util.tanD(f) * scalar);
		}
		return f;
	}

	/**
	 * returns the string value of the given label, as with
	 * {@link #getDoubleValue()} but where value may be any string after the
	 * delimiter.
	 */
	private static String getStringLabel(String[] aliases, String row) {
		Scanner s = new Scanner(row);
		try {
			for (String alias: aliases) {
				if (null != s.findInLine(alias)) {
					if (null != s.findInLine("[\\t =]+")) {
						return s.nextLine();
					}
				}
			}
		} catch (Exception e) {
		}
		return null;
	}
	
	/**
	 * returns the Double value of the given label, which should be in the form
	 * {a}{sep}{value}, where {a} can be any of the identities given by
	 * <code>aliases</code>, {sep} is typically whitespace or an '=' sign, and
	 * {value} is the floating point value of the label.
	 */
	private static Double getDoubleLabel(String[] aliases, String row) {
		Scanner s = new Scanner(row);
		try {
			for (String alias: aliases) {
				if (null != s.findInLine(alias)) {
					if (null != s.findInLine("[\\t =]+")) {
						return s.nextDouble();
					}
				}
			}
		} catch (Exception e) {
		}
		return null;
	}
	
	public int save(FeatureCollection fc, String fileName) {
		CsvWriter csv = null;
		try {
			List<Field> fields = new ArrayList<Field>(fc.getSchema());
			fields.remove(Field.FIELD_PATH);
			
			// check whether we can get away with lon/lat columns (preferred) or must use wkt
			boolean allPoints = true;
			for (Feature f: fc.getFeatures()) {
				if (f.getPath().getType() != FPath.TYPE_POINT) {
					allPoints = false;
					break;
				}
			}
			
			// write header
			csv = new CsvWriter(new BufferedWriter(new FileWriter(fileName)), ',');
			int geomCount = allPoints ? 2 : 1;
			String[] otherFields = new String[geomCount + fields.size()];
			if (allPoints) {
				otherFields[0] = lonAliases.get(lonAliases.size()-1);
				otherFields[1] = latAliases.get(latAliases.size()-1);
			} else {
				otherFields[0] = geomAliases.get(geomAliases.size()-1);
			}
			for (int i = 0; i < fields.size(); i++) {
				Field f = fields.get(i);
				otherFields[i+geomCount] = f.name;
				for (Type type: Type.values()) {
					if (type.c == f.type) {
						otherFields[i+geomCount] += ":" + type.suffix;
						break;
					}
				}
			}
			csv.writeRecord(otherFields);
			
			// get types for each field, defaulting to string
			Type[] types = new Type[fields.size()];
			Arrays.fill(types, Type.STR);
			int typeCount = 0;
			for (Field f: fields) {
				for (Type type: Type.values()) {
					if (type.c == f.type) {
						types[typeCount++] = type;
						break;
					}
				}
				if (f.type == ListType.class) {
					types[typeCount++] = Type.STR;
				}
			}
			
			FPath.GeometryAdapter adapter = null;
			WKTWriter writer = null;
			if (!allPoints) {
				adapter = new FPath.GeometryAdapter();
				writer = new WKTWriter(2);
			}
			
			// write rows
			int rows = 0;
			for (Feature f: fc.getFeatures()) {
				int column = 0;
				FPath path = f.getPath().getSpatialEast();
				if (allPoints) {
					Point2D p = path.getCenter();
					otherFields[column++] = fmt.format(p.getX());
					otherFields[column++] = fmt.format(p.getY());
				} else {
					Geometry geom = adapter.getGeometry(path);
					otherFields[column++] = writer.write(geom);
				}
				for (Field field: fields) {
					Object o = f.getAttribute(field);
					otherFields[column] = o == null ? "" : types[column-geomCount].toString(o);
					column++;
				}
				csv.writeRecord(otherFields);
				rows ++;
			}
			return rows;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (csv != null) {
				csv.close();
			}
		}
	}

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
}
