package edu.asu.jmars.layer.util.features;

import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.ReferenceMap;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;

import edu.asu.jmars.util.FillStyle;
import edu.asu.jmars.util.LineType;
import edu.asu.jmars.util.Util;

/**
 * A FeatureCollection implementation backed by a temporary Java Berkeley
 * database. The database files only store overflow that won't fit in memory.
 * Access to feature values is sped up with an object cache. The Feature
 * attributes map returns immutable key and entry sets.
 */
public final class BdbFC extends AbstractFeatureCollection {
	private static final EnvironmentConfig envConfig = new EnvironmentConfig();
	static {
	    envConfig.setAllowCreate(true);
	    envConfig.setSharedCache(true);
	    envConfig.setCachePercent(10);
	    envConfig.setTransactional(false);
	}
	
	private static final DatabaseConfig dbConfig = new DatabaseConfig();
	static {
		dbConfig.setAllowCreate(true);
		dbConfig.setTemporary(true);
	}
	
	private static final TupleBinding<Color> colorBinding = new TupleBinding<Color>() {
		public void objectToEntry(Color c, TupleOutput output) {
			output.write(c.getRGB());
		}
		public Color entryToObject(TupleInput input) {
			return new Color(input.readInt());
		}
	};
	
	private static final TupleBinding<LineType> lineBinding = new TupleBinding<LineType>() {
		public void objectToEntry(LineType line, TupleOutput output) {
			output.writeInt(line.getType());
		}
		public LineType entryToObject(TupleInput input) {
			return new LineType(input.readInt());
		}
	};
	
	private static final TupleBinding<FillStyle> fillStyleBinding = new TupleBinding<FillStyle>() {
		public void objectToEntry(FillStyle fillStyle, TupleOutput output) {
			output.writeString(fillStyle.getType());
		}
		public FillStyle entryToObject(TupleInput input) {
			return new FillStyle(input.readString());
		}		
	};
	
	// [pathCount, for each pathCount: [closed, vertexCount, for each vertexCount: [westlon, ocentric lat]]]
	private static final TupleBinding<FPath> pathBinding = new TupleBinding<FPath>() {
		public void objectToEntry(FPath path, TupleOutput output) {
			path = path.getSpatialWest();
			int pathCount = path.getPathCount();
			output.writeInt(pathCount);
			for (int i = 0; i < pathCount; i++) {
				output.writeBoolean(path.getClosed(i));
				double[] coords = path.getCoords(i, false);
				output.writeInt(coords.length/2);
				for (double coord: coords) {
					output.writeDouble(coord);
				}
			}
		}
		public FPath entryToObject(TupleInput input) {
			int pathCount = input.readInt();
			GeneralPath path = new GeneralPath();
			for (int i = 0; i < pathCount; i++) {
				boolean closed = input.readBoolean();
				int size = input.readInt();
				for (int j = 0; j < size; j++) {
					if (j == 0) {
						path.moveTo(input.readFloat(), input.readFloat());
					} else {
						path.lineTo(input.readFloat(), input.readFloat());
					}
				}
				if (closed) {
					path.closePath();
				}
			}
			return new FPath(path, FPath.SPATIAL_WEST);
		}
	};
	
	private static final Map<Class<?>,TupleBinding<?>> bindings = new HashMap<Class<?>,TupleBinding<?>>();
	static {
		bindings.put(LineType.class, lineBinding);
		bindings.put(Color.class, colorBinding);
		bindings.put(FPath.class, pathBinding);
		bindings.put(FillStyle.class, fillStyleBinding);
	}
	
	private static DatabaseEntry index2entry (int index) {
		DatabaseEntry entry = new DatabaseEntry();
		TupleBinding.getPrimitiveBinding(Integer.class).objectToEntry(index, entry);
		return entry;
	}
	
	@SuppressWarnings("unchecked")
	private static final class ColumnInfo {
		/** The database handle for this column */
		private final Database db;
		
		/** The converter for this column */
		private final TupleBinding binding;
		
		/** A cache of objects used by this column */
		private final Map<Integer,Object> cache = new ReferenceMap(ReferenceMap.SOFT, ReferenceMap.HARD);
		
		/** Create a new column in this environment for the given field. */
		public ColumnInfo(Environment env, Field field) {
			TupleBinding b = bindings.get(field.type);
			if (b == null) {
				b = TupleBinding.getPrimitiveBinding(field.type);
			}
			if (b == null) {
				throw new IllegalArgumentException("Field " + field + " has unsupported type " + field.type);
			}
			binding = b;
			db = env.openDatabase(null, field.name + "_"+field.type, dbConfig);
		}
		
		/** Insert new items into cache and database. */
		public final void put(int index, Object value, DatabaseEntry keyEntry, DatabaseEntry valueEntry) {
			binding.objectToEntry(value, valueEntry);
			db.put(null, keyEntry, valueEntry);
			cache.put(index, value);
		}
		
		/** Remove item from the database and object cache. */
		public void delete(int index, DatabaseEntry keyEntry) {
			db.delete(null, keyEntry);
			cache.remove(index);
		}
		
		/** Get the value of the given index in this column */
		public Object get(int index) {
			if (cache.containsKey(index)) {
				return cache.get(index);
			} else {
				DatabaseEntry entry = new DatabaseEntry();
				db.get(null, index2entry(index), entry, LockMode.READ_UNCOMMITTED);
				Object value = binding.entryToObject(entry);
				cache.put(index, value);
				return value;
			}
		}
		
		/** Close this temporary database, which also removes any files used by it. */
		public void delete() {
			db.close();
		}
	}
	
	private static Map<BdbFC,Object> instances = Collections.synchronizedMap(new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.HARD));
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				// The close() method takes awhile, so repeat this loop in case
				// something is in the process of being added while other collections
				// are being removed.
				while (!instances.isEmpty()) {
					// Work with a copy for thread safety, and to avoid CMEs since
					// the close() method removes elements from 'instances'.
					for (BdbFC fc: instances.keySet().toArray(new BdbFC[0])) {
						fc.close();
					}
				}
			}
		}));
	}
	
	private static File mktemp() throws IOException {
		File file = File.createTempFile("shape", "");
		file.delete();
		file = new File(file.getAbsoluteFile().getParent()+File.separator+file.getName());
		file.mkdirs();
		return file;
	}
	
	private int lastKey = -1;
	private Environment environment;
	
	private synchronized int newKey() {
		return ++lastKey;
	}
	
	/**
	 * The main collection in this feature collection, a map from each feature
	 * collection Field to the info for that column.
	 */
	private final Map<Field,ColumnInfo> columns = new HashMap<Field,ColumnInfo>();
	
	/** A read-only view of the fields defined on this feature collection. */
	private final Set<Field> externalColumns = Collections.unmodifiableSet(columns.keySet());
	
	/** Create a temporary database in the user tmp directory. */
	public BdbFC() throws IOException {
		try {
			environment = new Environment(mktemp(), envConfig);
			instances.put(this, 0);
		} catch (DatabaseException dbe) {
			throw new RuntimeException(dbe);
		}
	}
	
	/** Closes the database when all references to the collection have gone away. */
	public void finalize() {
		close();
	}
	
	/**
	 * Clean up the column databases, close the associated environment, and
	 * remove this instance from the list of statically held instances.
	 */
	public void close() {
		try {
			// clean up the column databases, then the environment, then clear
			// out the objects
			for (ColumnInfo col: columns.values()) {
				col.delete();
			}
			columns.clear();
			if (environment != null) {
				File file = environment.getHome();
				environment.close();
				Util.recursiveRemoveDir(file);
				environment = null;
			}
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			// even if an error occurs during the close, ensure we only try to close once
			// so the shutdown hook doesn't get into a recursive tail-spin of death.
			instances.remove(this);
		}
	}
	
	protected void addFields_impl(Collection<Field> f) {
		for (Field field: f) {
			if (!columns.containsKey(field)) {
				columns.put(field, new ColumnInfo(environment, field));
			}
		}
	}
	
	protected void removeFields_impl(Collection<Field> fields) {
		for (Field field: fields) {
			ColumnInfo column = columns.get(field);
			if (column != null) {
				column.delete();
				columns.remove(field);
			}
		}
	}
	
	protected Map<Field, Object> addData_impl(Map<Field, Object> data) {
		Map<Field,Object> out = new FeatureMap(newKey(), this);
		out.putAll(data);
		return out;
	}
	
	protected void removeData_impl(Map<Field, Object> data) {
		if (data instanceof FeatureMap) {
			FeatureMap map = (FeatureMap)data;
			DatabaseEntry keyEntry = index2entry(map.index);
			for (ColumnInfo column: columns.values()) {
				column.delete(map.index, keyEntry);
			}
		}
	}
	
	private static final class FeatureMap extends AbstractMap<Field,Object> {
		private final int index;
		private final BdbFC owner;
		public FeatureMap(int index, BdbFC owner) {
			this.index = index;
			this.owner = owner;
		}
		public boolean equals(Object o) {
			if (o instanceof FeatureMap) {
				FeatureMap map = FeatureMap.class.cast(o);
				return map.index == index && map.owner == owner;
			} else {
				return false;
			}
		}
		public int hashCode() {
			return index;
		}
		public int size() {
			return owner.columns.size();
		}
		public boolean isEmpty() {
			return owner.columns.isEmpty();
		}
		public boolean containsKey(Object key) {
			return owner.columns.containsKey(key);
		}
		public Object get(Object field) {
			try {
				ColumnInfo column = owner.columns.get(field);
				if (column != null) {
					return column.get(index);
				} else {
					return null;
				}
			} catch (Exception e) {
				return null;
			}
		}
		public Object put(Field key, Object value) {
			ColumnInfo column = owner.columns.get(key);
			if (column != null) {
				Object out = column.get(index);
				DatabaseEntry keyEntry = index2entry(index);
				if (value != null) {
					column.put(index, value, keyEntry, new DatabaseEntry());
				} else if (out != null) {
					column.delete(index, keyEntry);
				}
				return out;
			} else {
				return null;
			}
		}
		public void putAll(Map<? extends Field, ? extends Object> values) {
			DatabaseEntry keyEntry = index2entry(index);
			DatabaseEntry entry = new DatabaseEntry();
			for (Field key: values.keySet()) {
				ColumnInfo column = owner.columns.get(key);
				if (column != null) {
					Object value = values.get(key);
					if (value != null) {
						column.put(index, value, keyEntry, entry);
					} else {
						column.delete(index, keyEntry);
					}
				}
			}
		}
		public Object remove(Object key) {
			ColumnInfo column = owner.columns.get(key);
			if (column != null) {
				Object out = column.get(index);
				column.delete(index, index2entry(index));
				return out;
			} else {
				return null;
			}
		}
		/** @return The single set of headers; modifications to this set affect all rows */
		public Set<Field> keySet() {
			return owner.externalColumns;
		}
		public Set<Map.Entry<Field,Object>> entrySet() {
			return new EntrySet();
		}
		private final class EntrySet extends AbstractSet<Map.Entry<Field,Object>> {
			public int size() {
				return FeatureMap.this.size();
			}
			public Iterator<Map.Entry<Field,Object>> iterator() {
				return new EntryIterator();
			}
		}
		private final class EntryIterator implements Iterator<Map.Entry<Field,Object>> {
			private final Iterator<Field> it;
			public EntryIterator() {
				this.it = keySet().iterator();
			}
			public boolean hasNext() {
				return it.hasNext();
			}
			public Map.Entry<Field,Object> next() {
				return new Entry(it.next());
			}
			public void remove() {
				it.remove();
			}
		}
		private final class Entry implements Map.Entry<Field,Object> {
			private final Field key;
			public Entry(Field key) {
				this.key = key;
			}
			public Field getKey() {
				return key;
			}
			public Object getValue() {
				return get(key);
			}
			public Object setValue(Object value) {
				return put(key, value);
			}
		}
	}
}
