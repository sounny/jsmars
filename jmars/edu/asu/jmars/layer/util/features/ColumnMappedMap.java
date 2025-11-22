package edu.asu.jmars.layer.util.features;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ColumnMappedMap<K,V> extends AbstractMap<K,V> {
	private final int index;
	private final Map<K,List> columns;
	public ColumnMappedMap(int index, Map<K,List> columns) {
		this.index = index;
		this.columns = columns;
	}
	public boolean equals(Object o) {
		return o instanceof ColumnMappedMap && ((ColumnMappedMap<?, ?>)o).index == index;
	}
	public int hashCode() {
		return index;
	}
	public int size() {
		return columns.size();
	}
	public boolean isEmpty() {
		return columns.isEmpty();
	}
	public boolean containsKey(Object key) {
		return columns.containsKey(key);
	}
	public V get(Object column) {
		List<V> rows = columns.get(column);
		if (rows == null) {
			return null;
		} else {
			return rows.get(index);
		}
	}
	public V put(K key, V value) {
		List list = columns.get(key);
		if (list.size() == index) {
			list.add(value);
			return null;
		} else {
			return (V)list.set(index, value);
		}
	}
	/** @return The single set of headers; modifications to this set affect all rows */
	public Set<K> keySet() {
		return columns.keySet();
	}
	public Set<Map.Entry<K,V>> entrySet() {
		return new EntrySet<K,V>(this);
	}
	private static final class EntrySet<K,V> extends AbstractSet<Map.Entry<K,V>> {
		private final ColumnMappedMap<K,V> row;
		public EntrySet(ColumnMappedMap<K,V> row) {
			this.row = row;
		}
		public int size() {
			return row.columns.size();
		}
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator<K,V>(row);
		}
	}
	private static final class EntryIterator<K,V> implements Iterator<Map.Entry<K,V>> {
		private final ColumnMappedMap<K,V> row;
		private final Iterator<K> it;
		public EntryIterator(ColumnMappedMap<K,V> row) {
			this.row = row;
			this.it = row.columns.keySet().iterator();
		}
		public boolean hasNext() {
			return it.hasNext();
		}
		public Map.Entry<K,V> next() {
			return new Entry<K,V>(row, it.next());
		}
		public void remove() {
			it.remove();
		}
	}
	private static final class Entry<K,V> implements Map.Entry<K,V> {
		private final ColumnMappedMap<K,V> row;
		private final K key;
		public Entry(ColumnMappedMap<K,V> row, K key) {
			this.row = row;
			this.key = key;
		}
		public K getKey() {
			return key;
		}
		public V getValue() {
			return (V)row.columns.get(key).get(row.index);
		}
		public V setValue(V value) {
			return (V)row.columns.get(key).set(row.index, value);
		}
	}
}
