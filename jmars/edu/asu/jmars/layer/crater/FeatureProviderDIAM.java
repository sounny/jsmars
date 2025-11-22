package edu.asu.jmars.layer.crater;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureProvider;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.util.LineType;

/**
 * Provides support for exporting Craterstats DIAM files. Even though it is an implementation of the 
 * FeaturePRovider interface, it is not available for general use as a feature provider for other parts
 * of the Crater layer, or for other layers.
 * 
 * FeatureProviderDIAM only implements FeatureProvider to leverage the existing FileChooser class and
 * simplify the overall implementation of this new functionality.
 */

public class FeatureProviderDIAM implements FeatureProvider {
	public String getDescription() {
		return "DIAM file for Craterstats";
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
		return ".diam";
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
	
	private enum Type {
		INT(Integer.class),
		DBL(Double.class),
		STR(String.class),
		BOO(Boolean.class),
		LIN(LineType.class),
		COL(Color.class);
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
			case DBL: return new Scanner(s).nextDouble();
			case STR: return s;
			case LIN: return new LineType(new Scanner(s).nextInt());
			case COL:
				Scanner sc = new Scanner(s);
				return new Color(
					sc.nextInt(), sc.nextInt(), sc.nextInt(),
					sc.hasNext() ? sc.nextInt() : 255);
			default: return null;
			}
		}
		public String toString(Object value) {
			switch (this) {
			case BOO:
			case INT:
			case DBL:
			case STR: return value.toString();
			case LIN: return ""+((LineType)value).getType();
			case COL:
				Color col = (Color)value;
				return col.getRed() + " " +
					col.getGreen() + " " +
					col.getBlue() +
					(col.getAlpha() == 255 ? "" : " " + col.getAlpha());
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
	
	
	// Do NOT make this static.  If it is static, it is no longer thread-safe, due to the use of the ParsePosition parameter
	private final DecimalFormat fmt = new DecimalFormat("#.#####");

	private String headerTitle;
	private double craterArea;
	
	public int save(FeatureCollection fc, String fileName) {
		
		BufferedWriter fWriter = null;
		try {
			List<Field> fields = new ArrayList<Field>(fc.getSchema());
			fields.remove(Field.FIELD_PATH);
			
			boolean allPoints = false;
			
			// write header
			fWriter = new BufferedWriter(new FileWriter(fileName));
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

            String[] headerRow = {
			"#"+this.headerTitle,
			"#",
			String.format("Area <km^2> = %.3f", this.craterArea),
            "#diameter, km" };

            for (String row : headerRow) {
                fWriter.write(row);
                fWriter.newLine();            	
            }
			
			// get types for each field, defaulting to string
			Type[] types = new Type[1];
			Arrays.fill(types, Type.STR);
			Field field = fields.get(0);
			for (Type type: Type.values()) {
				if (type.c == field.type) {
					types[0] = type;
					break;
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
			String[] row = new String[1];
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
				Object o = f.getAttribute(field);
				otherFields[column] = o == null ? "" : types[column-geomCount].toString(o);
				column++;
				int diam = 1;
				double diamKm = Double.parseDouble(otherFields[diam]) / 1000;
				row[0] = Double.toString(diamKm);
				fWriter.write(row[0]);
	            fWriter.newLine();
				rows ++;
			}
			fWriter.close();
			return rows;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
    
    public void setHeaderInfo(String title, double area) {
    	this.headerTitle = title;
    	this.craterArea = area;
    }

	@Override
	public FeatureCollection load(String fileName) {
		// Not used in this feature provider.
		return null;
	}
}
