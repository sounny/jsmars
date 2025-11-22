package edu.asu.jmars.layer.util.features;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.ReferenceMap;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeocentricCRS;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import edu.asu.jmars.util.BidiMap;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.FillStyle;
import edu.asu.jmars.util.LineType;
import edu.asu.jmars.util.ListType;
import edu.asu.jmars.util.Util;

public class FeatureProviderSHP implements FeatureProvider {
	private static DebugLog log = DebugLog.instance();
	
// This boolean is only used in this feature provider because all of our readOnly shape
// files are loaded with this.  The boolean is used to set the columns as uneditable.
// This class is called within the FeatureProviderReadOnly with isEditable=false;	
	private boolean isEditable=true;
	
	private static CoordinateReferenceSystem jmarsCRS, defaultOutputCRS;
	static {
		try {
			jmarsCRS = FeatureUtil.getJMarsCRS();
		} catch (FactoryException e) {
			e.printStackTrace();
			log.println(e);
			jmarsCRS = null;
		}
		
		try {
			defaultOutputCRS = FeatureUtil.getDefaultOutputCRS();
			log.println("Default output CRS:"+defaultOutputCRS.toWKT());
		}
		catch(FactoryException ex){
			ex.printStackTrace();
			log.println(ex);
			defaultOutputCRS = null;
		}
	}
	
	public static final BidiMap<String,String> intToExtNames = new BidiMap<String,String>();
	static {
		intToExtNames.add(Field.FIELD_SHOW_LABEL.name, "showlbl");
		intToExtNames.add(Field.FIELD_LABEL_COLOR.name, "lblclr");
		intToExtNames.add(Field.FIELD_DRAW_COLOR.name, "lineclr");
		intToExtNames.add(Field.FIELD_FILL_COLOR.name, "fillclr");
		intToExtNames.add(Field.FIELD_LINE_WIDTH.name, "lwidth");
		intToExtNames.add(Field.FIELD_POINT_SIZE.name, "ptsize");
		intToExtNames.add(Field.FIELD_LINE_DIRECTED.name, "ldir");
		intToExtNames.add(Field.FIELD_LINE_DASH.name, "ltype");
		intToExtNames.add(Field.FIELD_FILL_POLYGON.name, "filled");
		intToExtNames.add(Field.FIELD_FILL_STYLE.name, "fstyle");
	};
	
	/** shp files end with this suffix */
	private static final String shpSuffix = ".shp";
	/** prj files end with this suffix */
	private static final String prjSuffix = ".prj";
	/** Known suffixes that are part of ESRI shapefiles. */
	private static final String[] suffixes = {shpSuffix, ".dbf", ".shx", ".qix", ".shx", prjSuffix};
	/** Index of projection file suffx */
	private static final int prjIndex = Arrays.asList(suffixes).indexOf(prjSuffix);
	/** Offsets into the {@link #suffixes} array required for the reader to succeed, and the first entry is also the suffix used by the writer. */
	private static final int[] requiredSuffixes = {0, 1};
	
	public String getDescription() {
		return "Geotools ESRI .shp file provider";
	}
	
	public File[] getExistingSaveToFiles(FeatureCollection fc, String baseName) {
		List<Integer> typeIndices = getTypeIndices(fc);
		if (typeIndices.isEmpty()) {
			return new File[]{};
		}
		
		List<String> bases = new ArrayList<String>();
		String base = getUnsuffixed(baseName, suffixes);
		if (typeIndices.size() == 1) {
			bases.add(base);
		} else {
			for (int typeIndex: typeIndices) {
				bases.add(base + types[typeIndex].fileSuffix);
			}
		}
		
		List<File> files = new ArrayList<File>();
		for (String typedBase: bases) {
			for (String file: exists(typedBase, suffixes)) {
				if (file != null) {
					files.add(new File(file));
				}
			}
		}
		return files.toArray(new File[files.size()]);
	}
	
	public String getExtension() {
		return shpSuffix;
	}
	
	public boolean isFileBased() {
		return true;
	}
	
	public boolean isRepresentable(FeatureCollection fc) {
		return true;
	}
	
	/** Load shapes into a new {@link SingleFeatureCollection}. */
	public FeatureCollection load(String fileName) {
		FeatureCollection fc = new SingleFeatureCollection();
		load(fileName, fc);
		return fc;
	}
	
	/**
	 * This defines a type mapper capable of converting column definitions
	 * (JMARS Field versus Geotools AttributeDescriptor) and non-null
	 * Object instances for each cell in that column.
	 */
	private interface TypeMap {
		/** @return An equivalent Field for this descriptor, or null if this type mapper is not for this descriptor. */
		Field getField(AttributeDescriptor desc);
		/** @return An equivalent descriptor for this Field, or null if this type mampper is not for this field. */
		AttributeDescriptor getDesc(Field field);
		/** The given instance is converted from a JMARS type to an ESRI shapefile type, and the new ESRI instance is returned. */
		Object getEsri(Object jmars);
		/** The given instance is converted from an ESRI type to a JMARS type, and the new JMARS instance is returned. */
		Object getJmars(Object esri);
	}
	
	/** @return the given file name without any of the given case-insensitive suffixes at the end. */
	private static String getUnsuffixed(String fileName, String ... suffixes) {
		for (String suffix: suffixes) {
			int offset = fileName.length() - suffix.length();
			if (offset > 0) {
				String end = fileName.substring(offset);
				if (end.equalsIgnoreCase(suffix)) {
					return fileName.substring(0, offset);
				}
			}
		}
		return fileName;
	}

	/**
	 * @return Array of files found that start with the given fileName and end
	 *         with any capitalization of each suffix, in the same order as the
	 *         elements in the suffix array. Each position in the returned array
	 *         will be null if that file was not found.
	 */
	private static String[] exists(final String fileName, final String ... suffixes) {
		final String[] found = new String[suffixes.length];
		Arrays.fill(found, null);
		File parent = new File(fileName).getAbsoluteFile().getParentFile();
		final String baseName = new File(fileName).getName();
		parent.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				for (int i = 0; i < suffixes.length; i++) {
					if (name.equalsIgnoreCase(baseName + suffixes[i])) {
						found[i] = dir.getAbsolutePath() + File.separator + name;
						return true;
					}
				}
				return false;
			}
		});
		return found;
	}
	
	private static String getPrefixedName(String prefix, String humanType, String name) {
		if (prefix.length() >= name.length() || !name.substring(0, prefix.length()).equalsIgnoreCase(prefix)) {
			name = prefix + name;
		}
		if (name.length() > 10) {
			name = name.substring(0, 10);
		}
		return name;
	}
	
	private static String getUnprefixedName(String prefix, String name) {
		if (name.length() > prefix.length() && name.substring(0, prefix.length()).equalsIgnoreCase(prefix)) {
			return name.substring(prefix.length());
		} else {
			return null;
		}
	}
	
	/**
	 * If a column has a name too long for the ESRI dbase file,
	 * try to substitute it with one of a small set of
	 * predefined name changes.
	 */
	private static String getJmarsName(String name) {
		String jmars = intToExtNames.getRight(name);
		return jmars != null ? jmars : name;
	}
	
	/**
	 * If a column name was too long when the dbase file was
	 * saved, then when rereading the dbase file, convert the
	 * column names back using the predefined name changes.
	 */
	private static String getEsriName(String name) {
		String esri = intToExtNames.getLeft(name);
		return esri != null ? esri : name;
	}
	
	/**
	 * Because of body switching, this needs to be updated for 
	 * shapefiles. This is done so it saves out the current body,
	 * not the body that was originally opened.
	 */
	public static void resetValues() {
		try {
			jmarsCRS = FeatureUtil.getJMarsCRS();
		} catch (FactoryException e) {
			e.printStackTrace();
			log.println(e);
			jmarsCRS = null;
		}
		
		try {
			defaultOutputCRS = FeatureUtil.getDefaultOutputCRS();
			log.println("Default output CRS:"+defaultOutputCRS.toWKT());
		}
		catch(FactoryException ex){
			ex.printStackTrace();
			log.println(ex);
			defaultOutputCRS = null;
		}
	}
	
	private static final List<Filter> noRestrictions = Collections.emptyList();
	
	// FPath is stored as a geometry in the shp file
	private static final class PathConverter implements TypeMap {
		private final FPath.GeometryAdapter adapter = new FPath.GeometryAdapter();
		private final Class<?> geomType;
		private final CoordinateReferenceSystem esriCRS;
		private MathTransform toEsri, toJmars;
		public PathConverter(Class<?> geomType, CoordinateReferenceSystem esriCRS) {
			this.geomType = geomType;
			this.esriCRS = esriCRS;
			MathTransform a = null, b = null;
			if (esriCRS != null && jmarsCRS != null && !CRS.equalsIgnoreMetadata(jmarsCRS, esriCRS)) {
				try {
					a = CRS.findMathTransform(jmarsCRS, esriCRS, true);
					b = a.inverse();
					// if both a and b computed without exception, assign them both
					toEsri = a;
					toJmars = b;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		public AttributeDescriptor getDesc(Field field) {
			if (FPath.class.isAssignableFrom(field.type)) {
				Name name = new NameImpl(field.name);
				GeometryType at = new GeometryTypeImpl(name, geomType,
					esriCRS, false, false, noRestrictions, null, null);
				return new GeometryDescriptorImpl(at, name, 1, 1, true, null);
			} else {
				return null;
			}
		}
		public Field getField(AttributeDescriptor desc) {
			if (Geometry.class.isAssignableFrom(desc.getType().getBinding())) {
				return Field.FIELD_PATH;
			} else {
				return null;
			}
		}
		public Object getEsri(Object jmars) {
			FPath path = ((FPath)jmars).getSpatialEast();
			Geometry geom = adapter.getGeometry(path);
			if (toEsri != null) {
				try {
					geom = JTS.transform(geom, toEsri);
				} catch (Exception e) {
					throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
				}
			}
			return geom;
		}
		public Object getJmars(Object esri) {
			Geometry geom = (Geometry)esri;
			if (toJmars != null) {
				try {
					geom = JTS.transform(geom, toJmars);
				} catch (Exception e) {
					throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
				}
			}
			return new FPath(adapter.getPath(geom), FPath.SPATIAL_EAST).getSpatialWest();
		}
	}
	
	// Color is stored as a 15 digit char, of space-separated color components in rgba order
	private static final class ColorConverter implements TypeMap {
		private static final String prefix = "jC_";
		public AttributeDescriptor getDesc(Field field) {
			if (field.type != Color.class) {
				return null;
			}
			String name = getPrefixedName(prefix, "Color", getEsriName(field.name));
			return new AttributeTypeBuilder().binding(String.class).length(15).buildDescriptor(name);
		}
		public Field getField(AttributeDescriptor desc) {
			String unprefixed = getUnprefixedName(prefix, desc.getLocalName());
			Class<?> type = desc.getType().getBinding();
			if (unprefixed == null || type != String.class) {
				return null;
			} else {
				return new Field(getJmarsName(unprefixed), Color.class);
			}
		}
		public Object getEsri(Object jmars) {
			Color color = (Color)jmars;
			return color.getRed() + " " + color.getGreen() + " " + color.getBlue() + " " + color.getAlpha();
		}
		public Object getJmars(Object esri) {
			String[] bits = esri.toString().split(" ");
			return new Color(Integer.parseInt(bits[0]), Integer.parseInt(bits[1]), Integer.parseInt(bits[2]), Integer.parseInt(bits[3]));
		}
	}
	
	// LineType is stored as a 2 digit char
	private static final class LineTypeConverter implements TypeMap {
		private static final String prefix = "jL_";
		public AttributeDescriptor getDesc(Field field) {
			if (field.type != LineType.class) {
				return null;
			}
			String name = getPrefixedName(prefix, "Line type", getEsriName(field.name));
			return new AttributeTypeBuilder().binding(String.class).length(2).buildDescriptor(name);
		}
		public Field getField(AttributeDescriptor desc) {
			String unprefixed = getUnprefixedName(prefix, desc.getLocalName());
			Class<?> type = desc.getType().getBinding();
			if (unprefixed == null || type != String.class) {
				return null;
			} else {
				return new Field(getJmarsName(unprefixed), LineType.class);
			}
		}
		public Object getEsri(Object jmars) {
			return ""+((LineType)jmars).getType();
		}
		public Object getJmars(Object esri) {
			return new LineType(Integer.parseInt((String)esri));
		}
	}

	// FillStyle is stored as a String based on the USGS style key
	private static final class FillStyleConverter implements TypeMap {
		private static final String prefix = "jF_";
		public AttributeDescriptor getDesc(Field field) {
			if (field.type != FillStyle.class) {
				return null;
			}
			String name = getPrefixedName(prefix, "Fill style", getEsriName(field.name));
			return new AttributeTypeBuilder().binding(String.class).length(8).buildDescriptor(name);
		}
		public Field getField(AttributeDescriptor desc) {
			String unprefixed = getUnprefixedName(prefix, desc.getLocalName());
			Class<?> type = desc.getType().getBinding();
			if (unprefixed == null || type != String.class) {
				return null;
			} else {
				return new Field(getJmarsName(unprefixed), FillStyle.class);
			}
		}
		public Object getEsri(Object jmars) {
			return ""+((FillStyle)jmars).getType();
		}
		public Object getJmars(Object esri) {
			return new FillStyle((String)esri);
		}
	}

	// ListType translates into a simple String for save/export
	private static final class ListTypeConverter implements TypeMap {
		public AttributeDescriptor getDesc(Field field) {
			if (field.type != ListType.class) {
				return null;
			}
			String name = getEsriName(field.name);
			return new AttributeTypeBuilder().binding(String.class).length(8).buildDescriptor(name);
		}
		public Field getField(AttributeDescriptor desc) {
			String unprefixed = desc.getLocalName();
			Class<?> type = desc.getType().getBinding();
			if (unprefixed == null || type != String.class) {
				return null;
			} else {
				return new Field(getJmarsName(unprefixed), String.class);
			}
		}
		public Object getEsri(Object jmars) {
			return ""+jmars;
		}
		public Object getJmars(Object esri) {
			return ""+esri;
		}
	}

	// Boolean - reads legacy JMARS shapefile approach to saving booleans.
	private static final class BoolConverter implements TypeMap {
		private static final String prefix = "jB_";
		/** Always fails since we now write booleans with the native dbf type map */
		public AttributeDescriptor getDesc(Field field) {
			return null;
		}
		/** But we are willing to read back 5 char string with the proper prefix as booleans, since that is how JMARS used to do it. */
		public Field getField(AttributeDescriptor desc) {
			String unprefixed = getUnprefixedName(prefix, desc.getLocalName());
			Class<?> type = desc.getType().getBinding();
			if (unprefixed == null || type != String.class) {
				return null;
			} else {
				return new Field(getJmarsName(unprefixed), Boolean.class);
			}
		}
		public Object getEsri(Object jmars) {
			throw new UnsupportedOperationException("Writing with this type mapper is not supported");
		}
		public Object getJmars(Object esri) {
			return Boolean.parseBoolean((String)esri) ? Boolean.TRUE : Boolean.FALSE;
		}
	}
	
	// Simple -- no real value conversion is necessary
	private static final class DefaultConverter implements TypeMap {
		private final FeatureCollection fc;
		public DefaultConverter(FeatureCollection fc) {
			this.fc = fc;
		}
		public AttributeDescriptor getDesc(Field field) {
			if (Byte.class.isAssignableFrom(field.type) ||
					Short.class.isAssignableFrom(field.type) ||
					Integer.class.isAssignableFrom(field.type) ||
					Long.class.isAssignableFrom(field.type) ||
					Float.class.isAssignableFrom(field.type) ||
					Double.class.isAssignableFrom(field.type) ||
					Boolean.class.isAssignableFrom(field.type)) {
				// primitive types are just named and typed
				return new AttributeTypeBuilder()
					.binding(field.type)
					.buildDescriptor(getEsriName(field.name));
			} else if (CharSequence.class.isAssignableFrom(field.type)) {
				// strings are sized to see how much space is actually needed
				int length = 0;
				for (Feature f: fc.getFeatures()) {
					Object value = f.getAttribute(field);
					if (value instanceof String) {
						length = Math.max(length, ((String)value).length());
					}
				}
				return new AttributeTypeBuilder()
					.binding(field.type)
					.length(Math.max(length,1)) // GeoTools ESRI writer fails for field-length <= 0
					.buildDescriptor(getEsriName(field.name));
			} else {
				return null;
			}
		}
		public Field getField(AttributeDescriptor desc) {
			Class<?> type = desc.getType().getBinding();
			if (Byte.class.isAssignableFrom(type) ||
					Short.class.isAssignableFrom(type) ||
					Integer.class.isAssignableFrom(type) ||
					Long.class.isAssignableFrom(type) ||
					Float.class.isAssignableFrom(type) ||
					Double.class.isAssignableFrom(type) ||
					Boolean.class.isAssignableFrom(type) ||
					String.class.isAssignableFrom(String.class)) {
				return new Field(getJmarsName(desc.getLocalName()), type);
			} else {
				return null;
			}
		}
		public Object getEsri(Object jmars) {
			return jmars;
		}
		public Object getJmars(Object esri) {
			return esri;
		}
	}
	
	/** Takes arguments necessary to set up all TypeMap implementations and creates them. */
	private static List<TypeMap> getTypeConverters(
			FeatureCollection fc,
			Class<?> geomType,
			CoordinateReferenceSystem esriCRS) {
		return Arrays.asList(
			new PathConverter(geomType, esriCRS),
			new ColorConverter(),
			new LineTypeConverter(),
			new FillStyleConverter(),
			new BoolConverter(),
			new ListTypeConverter(),
			new DefaultConverter(fc));
	}
	
	private static final class Converter {
		private final TypeMap type;
		private final Field field;
		private final AttributeDescriptor desc;
		public Field getField() {
			return field;
		}
		public AttributeDescriptor getAttribute() {
			return desc;
		}
		public TypeMap getTypeMap() {
			return type;
		}
		public Converter(Field field, AttributeDescriptor desc, TypeMap type) {
			this.field = field;
			this.type = type;
			this.desc = desc;
		}
	}
	
	/** Load shapes into the given feature collection. */
	public void load(String fileName, FeatureCollection fc) {
		// make sure we have all of the necessary input files
		String base = getUnsuffixed(fileName, suffixes);
		String[] files = exists(base, suffixes);
		for (int idx: requiredSuffixes) {
			if (files[idx] == null) {
				throw new IllegalArgumentException("Could not find " + base + suffixes[idx]);
			}
		}
		
		CoordinateReferenceSystem esriCRS = getCRS(files, true);
		CoordCache ccache = CoordCache.instance(fc);
		
		FeatureIterator<SimpleFeature> iterator = null;
		FeatureSource<SimpleFeatureType, SimpleFeature> source = null;
		try {
			// open FeatureSource to read from
			File file = new File(fileName);
			ShapefileDataStore dataStore = new ShapefileDataStore(file.toURI().toURL(), true);
			source = dataStore.getFeatureSource(dataStore.getTypeNames()[0]);
			
			// get JMARS schema from Geotools attributes
			List<AttributeDescriptor> attributes = source.getSchema().getAttributeDescriptors();
			Converter[] converters = new Converter[attributes.size()];
			List<TypeMap> types = getTypeConverters(fc, null, esriCRS);
			for (int i = 0; i < converters.length; i++) {
				AttributeDescriptor desc = attributes.get(i);
				for (TypeMap type: types) {
					Field field = type.getField(desc);
					if (field != null) {
						converters[i] = new Converter(field, desc, type);
						//if is a read only layer, cells will not be editable
						field.setEditable(isEditable);
						break;
					}
				}
				if (converters[i] == null) {
					throw new IllegalArgumentException("Unrecognized type");
				}
			}
			
			// use converters for each column on all cells of that column
			for (iterator = source.getFeatures().features(); iterator.hasNext(); ) {
				SimpleFeature feature = iterator.next();
				Feature f = new Feature();
				for (int i = 0; i < converters.length; i++) {
					Object value = feature.getAttribute(i);
					if (value != null) {
						f.setAttribute(converters[i].getField(), converters[i].getTypeMap().getJmars(value));
						if (converters[i].getField() == Field.FIELD_PATH){
							ccache.put(f.getPath(), esriCRS, (Geometry)value);
						}
					}
				}
				fc.addFeature(f);
			}
		} catch (Exception e) {
			throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
		} finally {
			if (iterator != null) {
				iterator.close();
			}
			if (source != null) {
				source.getDataStore().dispose();
			}
		}
	}
	
	/**
	 * ESRI shapefiles require distinct geometry types to be in separate files,
	 * so we group together each such distinct type with the FPath type and file
	 * suffix we use to distinguish the files.
	 */
	private static class Type {
		public final int fpathType;
		public final String fileSuffix;
		public final Class<?> esriGeomType;
		public Type(int fpathType, String fileSuffix, Class<?> esriGeomType) {
			this.fpathType = fpathType;
			this.fileSuffix = fileSuffix;
			this.esriGeomType = esriGeomType;
		}
	}
	
	/**
	 * Associates each FPath type with a filename suffix and JTS Geometry type
	 * to use in the shp file
	 */
	private static final Type[] types = {
		new Type(FPath.TYPE_POINT, ".point", Point.class),
		new Type(FPath.TYPE_POLYLINE, ".line", LineString.class),
		new Type(FPath.TYPE_POLYGON, ".polygon", Polygon.class)
	};
	
	/**
	 * @return a list of type indices into {@link #pathTypes} and
	 *         {@link #typeNames}, so callers can easily identify which feature
	 *         types are in the given feature collection and in what order.
	 */
	private static List<Integer> getTypeIndices(FeatureCollection fc) {
		// accumulate unique results in order of discovery
		Set<Integer> typeIndices = new LinkedHashSet<Integer>();
		for (Feature f: fc.getFeatures()) {
			switch (f.getPath().getType()) {
			case FPath.TYPE_POINT: typeIndices.add(0); break;
			case FPath.TYPE_POLYLINE: typeIndices.add(1); break;
			case FPath.TYPE_POLYGON: typeIndices.add(2); break;
			}
			if (typeIndices.size() == types.length) {
				// we found one of each type, so stop early
				break;
			}
		}
		return new ArrayList<Integer>(typeIndices);
	}
	
	/**
	 * Will save features into the named file, projecting to the projection of
	 * the target file if it exists and has a projection.
	 */
	public int save(FeatureCollection fc, String fileName) {
		String fileType = suffixes[requiredSuffixes[0]];
		List<Integer> typeIndices = getTypeIndices(fc);
		try {
			// get the unsuffixed base name, and find the shp file if it exists
			String base = getUnsuffixed(fileName, suffixes);
			String savedName = exists(base, fileType)[0];
			int writeCount = 0;
			for (int typeIndex: typeIndices) {
				// Create the full file name as base.geomtype.filetype, where:
				// base is the filename the user provided without any of the known suffixes,
				// geomtype is a string to tell the user what kind of geometry is in the file, and
				// filetype tells the user whether this is a shp, dbf, etc.
				String geomType = "";
				if (typeIndices.size() > 1 && (savedName == null || typeIndex != typeIndices.get(0))) {
					// We only need the geometry type in the filename if the user is saving a new file,
					// or replacing an existing file that has a different kind of geometry than what we
					// are saving now.
					geomType = types[typeIndex].fileSuffix;
				}
				String fullName = base + geomType + fileType;
				writeCount += save(fc, typeIndex, fullName);
			}
			return writeCount;
		} catch (Exception e) {
			throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
		}
	}
	
	/**
	 * Given the results of calling {@link exists()} with the suffixes array,
	 * returns the CRS from the prj file is it can, a default jmarsCRS if it
	 * appears there isn't one, and throws an IllegalArgumentException otherwise.
	 */
	private static CoordinateReferenceSystem getCRS(String[] files, boolean load) {
		// if the prj file is missing, return default CRS
		if (files[prjIndex] == null) {
			return load? jmarsCRS: defaultOutputCRS;
		}
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(files[prjIndex]);
			String wkt = Util.join(" ", Util.readLines(fis));
			CoordinateReferenceSystem crs = CRS.parseWKT(wkt);
			return crs;
		} catch (Exception e) {
			String baseName = new File(files[prjIndex]).getName();
			throw new IllegalArgumentException("Cannot read projection in " + baseName, e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
    * @return <code>true</code> if the specified collection has shapes
	* with more than one paths in them.
	*/
	private static boolean hasMultiGeometryPolygons(FeatureCollection fc){
		for(Feature f: fc.getFeatures()){
			if (f.getPath().getPathCount() > 1)
				return true;
		}
		return false;
	}
	
	private static Converter[] getConverters(FeatureCollection fc, int typeIndex, CoordinateReferenceSystem esriCRS) {
		// create converters for a schema with this shape type, reusing the
		// existing projection if it is defined
		Converter[] converters = new Converter[fc.getSchema().size()];
		Class<?> esriGeomType = types[typeIndex].esriGeomType;
		if (esriGeomType == Polygon.class && hasMultiGeometryPolygons(fc))
			esriGeomType = MultiPolygon.class;
		List<TypeMap> typeMaps = getTypeConverters(fc, esriGeomType, esriCRS);
		for (int i = 0; i < converters.length; i++) {
			Field field = fc.getSchema().get(i);
			for (TypeMap type: typeMaps) {
				AttributeDescriptor desc = type.getDesc(field);
				if (desc != null) {
					converters[i] = new Converter(field, desc, type);
					break;
				}
			}
			if (converters[i] == null) {
				throw new IllegalArgumentException("Unrecognized type");
			}
		}
		return converters;
	}
	
	private static int save(FeatureCollection fc, int typeIndex, String fileName) throws IOException {
		String[] files = exists(getUnsuffixed(fileName, suffixes), suffixes);
		CoordinateReferenceSystem esriCRS = getCRS(files, false);
		Converter[] converters = getConverters(fc, typeIndex, esriCRS);
		CoordCache ccache = CoordCache.instance(fc);

		for (String file: files) {
			if (file != null) {
				new File(file).delete();
			}
		}
		
		ShapefileDataStore dataStore = null;
		FeatureWriter<SimpleFeatureType,SimpleFeature> aWriter = null;
		
		try {
			// Create the shape store
			dataStore = new ShapefileDataStore(new File(fileName).toURI().toURL(), true);
			
			// Create and set the FeatureType, which will delete any existing files
			SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
			builder.setName("shaperow");
			for (Converter c: converters) {
				builder.add(c.getAttribute());
			}
			System.out.println("22");
			dataStore.createSchema(builder.buildFeatureType());
			System.out.println("33");
			
			// Write each feature
			aWriter = dataStore.getFeatureWriter(Transaction.AUTO_COMMIT);
			int count = 0;
			for (Feature f: fc.getFeatures()) {
				if (f.getPath().getType() == types[typeIndex].fpathType) {
					SimpleFeature feature = aWriter.next();
					for (int i = 0; i < converters.length; i++) {
						Object value = f.getAttribute(converters[i].getField());
						if (value != null) {
							if (converters[i].getField() == Field.FIELD_PATH){
								Geometry converted = null;
								if (ccache != null)
									converted = ccache.get((FPath)value, esriCRS);
								if (converted != null)
									log.println("Using cached value "+converted+" for "+value);
								else
									converted = (Geometry)converters[i].getTypeMap().getEsri(value);
								feature.setAttribute(i, converted);
							}
							else {
								feature.setAttribute(i, converters[i].getTypeMap().getEsri(value));
							}
						}
					}
					aWriter.write();
					count ++;
				}
			}
			return count;
		} finally {
			if (aWriter != null) {
				aWriter.close();
			}
			if (dataStore != null) {
				dataStore.dispose();
			}
		}
	}

	
//These constructors are to set columns editable or uneditable for readonly 
// shape files.	
	public FeatureProviderSHP(boolean ie){
		this.isEditable=ie;
	}
// This one is here because a default constructor gets called, and with the 
// constructor above it would not default to this anymore.	
	public FeatureProviderSHP(){
	}

	/**
	 * Weak coordinate cache to keep source coordinates to avoid introducing imprecisions
	 * due to back and forth reprojection.
	 * 
	 * Reprojection of coordinates from external Shape file to JMARS 'ocentric coordinates
	 * introduces errors due to the use of sin/cos. When these coordinates are written 
	 * back out the errors multiply due to arc-tans etc. In an effort to avoid introducing
	 * errors due to back and forth reprojection we are saving source coordinates in the
	 * source Coordinate Reference System.
	 * 
	 * The cache instances are keyed on FeatureCollection. When the FeatureCollection goes
	 * away, the garbage collector should free the coordinate cache associated with it.
	 * 
	 * Every time the Feature in a FeatureCollection changes, a new FPath is constructed.
	 * As a result, if the coordinates of a point change by the user editing the feature,
	 * the cache returns <code>null</code>.
	 */
	private static class CoordCache {
		// instances: FeatureCollection -> CoordCache
		static private Map instances = new ReferenceMap(ReferenceMap.WEAK,ReferenceMap.HARD);
		
		// cache = <fc:<fpath:<esriCRS:Geometry>>>
		private Map<FPath,Map<CoordinateReferenceSystem,Geometry>> cache;

		/**
		 * Optionally creates and returns an instance of {@link CoordCache} associated with 
		 * the specified {@link FeatureCollection}.
		 * @param key {@link FeatureCollection} for which {@link CoordCache} is desired
		 * @return {@link CoordCache} associated with the given {@link FeatureCollection}.
		 */
		public static CoordCache instance(FeatureCollection key){
			CoordCache instance = (CoordCache)instances.get(key);
			if (instance == null){
				instances.put(key, instance = new CoordCache());
				log.println("CoordCache "+instance.hashCode()+" created.");
			}
			return instance;
		}
		
		protected CoordCache(){
			cache = new HashMap<FPath,Map<CoordinateReferenceSystem,Geometry>>();
		}
		
		/**
		 * Saves the source (esriCRS,geom) pair for the fpath, replacing any existing
		 * source for the fpath.
		 * @param fpath {@link FPath} generated from the other two inputs
		 * @param esriCRS source {@link CoordinateReferenceSystem} used to generate {@link FPath}
		 * @param geom source {@link Geometry} used to generate {@link FPath}
		 */
		public void put(FPath fpath, CoordinateReferenceSystem esriCRS, Geometry geom){
			// There is only one source Geometry+CRS pair that produced the FPath in JMARS
			cache.put(fpath, Collections.singletonMap(esriCRS, geom));
		}
		
		/**
		 * Returns the source geometry coordinates that were used to generate fpath
		 * given the source esriCRS.
		 * @param fpath {@link FPath} for which source {@link Geometry} is desired
		 * @param esriCRS source {@link CoordinateReferenceSystem} used to generate {@link FPath}
		 * @return <code>null</code> if the fpath does not have an associated source
		 * (esriCRS,geom) pair or the geom if it does.
		 */
		public Geometry get(FPath fpath, CoordinateReferenceSystem esriCRS){
			Map<CoordinateReferenceSystem,Geometry> fpathCache = cache.get(fpath);
			if (fpathCache == null)
				return null;
			Geometry geom = fpathCache.get(esriCRS);
			return geom;
		}
		
		public void finalize(){
			log.println("CoordCache "+this.hashCode()+" finalized.");
		}
	}	

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
}
