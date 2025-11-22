package edu.asu.jmars.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This is a utility class for expressing computation on each element in a
 * Collection<K> in the form of a map, where each corresponding value V is the
 * result of passing the key K to a function that returns any V given any K in
 * the collection.
 * 
 * By providing computation on a Collection<K> as a Map<K,V>, the Java
 * collections libraries can be used to filter, sort, and select based on the
 * results of an arbitrary user-defined function.
 * 
 * Note that this class provides no caching of the value, it is calculated each
 * time. The EntrySet implementation within this map does create Entry<K,V>
 * elements in large groups, however, to provide some temporal locality to the
 * calls into the given function.
 */
public final class FunctionMap<K,V> extends AbstractMap<K,V> {
	public interface Function<K,V> {
		V calculate(K key);
	}
	
	private final Collection<K> keys;
	private final Function<K,V> function;
	private final int maxSize;
	private Entries entries;
	
	/** Create a function map with an entrySet() buffer size of 1000 elements */
	public FunctionMap(Collection<K> keys, Function<K,V> function) {
		this(keys, function, 1000);
	}
	
	/** Create a function map with the given keys, function to map entrySet() values from each key, and buffer size for the entrySet(). */
	public FunctionMap(Collection<K> keys, Function<K,V> function, int maxSize) {
		this.keys = keys;
		this.function = function;
		this.maxSize = maxSize;
	}
	
	public Set<Entry<K, V>> entrySet() {
		if (entries == null) {
			entries = new Entries();
		}
		return entries;
	}
	
	private final class Entries extends AbstractSet<Entry<K,V>> {
		public Iterator<Entry<K, V>> iterator() {
			return new EntryIt();
		}
		public int size() {
			return keys.size();
		}
	}
	
	private final class EntryIt implements Iterator<Entry<K,V>> {
		private final Iterator<K> keyIt = keys.iterator();
		private final List<Entry<K,V>> entryCache = new ArrayList<Entry<K,V>>(maxSize);
		private int index = 0;
		public boolean hasNext() {
			return index < entryCache.size() || keyIt.hasNext();
		}
		public Entry<K, V> next() {
			if (index == entryCache.size()) {
				index = 0;
				entryCache.clear();
				while (keyIt.hasNext() && entryCache.size() < maxSize) {
					K key = keyIt.next();
					entryCache.add(new FuncEntry<K,V>(key, function.calculate(key)));
				}
			}
			return index < entryCache.size() ? entryCache.get(index++) : null;
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	private static final class FuncEntry<K,V> implements Entry<K,V> {
		private final K key;
		private final V value;
		public FuncEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}
		public K getKey() {
			return key;
		}
		public V getValue() {
			return value;
		}
		public V setValue(V value) {
			throw new UnsupportedOperationException();
		}
	};
}

