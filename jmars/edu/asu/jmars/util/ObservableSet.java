package edu.asu.jmars.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Proxies all method calls to an underlying {@link Set} provided in the
 * constructor, and notifies all registered {@link ObservableSetListener}s with
 * the set of affected items AFTER the change has been made.
 * 
 * Note that many of the methods forcibly cast to the generic type, even though
 * the Set interface takes Object (e.g. {@link #remove(Object)}.)
 */
public class ObservableSet<E> implements Set<E> {
	private Set<ObservableSetListener<E>> listeners = new HashSet<ObservableSetListener<E>>();
	public void addListener(ObservableSetListener<E> listener) {
		listeners.add(listener);
	}
	public void removeListener(ObservableSetListener<E> listener) {
		listeners.remove(listener);
	}
	private void dispatch(Set<E> added, Set<E> removed) {
		for (ObservableSetListener<E> l: listeners) {
			l.change(added, removed);
		}
	}
	private final Set<E> data;
	public ObservableSet(Set<E> set) {
		data = set;
		if (data == null) {
			throw new IllegalArgumentException("Must provide a non-null set to proxy to.");
		}
	}
	class ObservableIterator implements Iterator<E> {
		private final Iterator<E> iter;
		private E current;
		public ObservableIterator(Iterator<E> iter) {
			this.iter = iter;
		}
		public boolean hasNext() {
			return iter.hasNext();
		}
		public E next() {
			return current = iter.next();
		}
		public void remove() {
			iter.remove();
			dispatch(null, Collections.singleton(current));
		}
	}
	public Iterator<E> iterator() {
		return new ObservableIterator(data.iterator());
	}
	public int size() {
		return data.size();
	}
	public boolean removeAll(Collection<?> c) {
		Set<E> removed = new HashSet<E>();
		for (Object o: c) {
			if (data.contains(o)) {
				removed.add((E)o);
			}
		}
		if (!removed.isEmpty()) {
			data.removeAll(removed);
			dispatch(null, removed);
			return true;
		} else {
			return false;
		}
	}
	public boolean add(E o) {
		boolean mod = data.add(o);
		if (mod) {
			dispatch(Collections.singleton(o), null);
		}
		return mod;
	}
	public boolean addAll(Collection<? extends E> c) {
		Set<E> added = new HashSet<E>();
		for (E item: c) {
			if (!data.contains(item)) {
				added.add(item);
			}
		}
		if (!added.isEmpty()) {
			data.addAll(added);
			dispatch(added, null);
			return true;
		} else {
			return false;
		}
	}
	public void clear() {
		if (!data.isEmpty()) {
			Set<E> toRemove = new HashSet<E>(data);
			data.clear();
			dispatch(null, toRemove);
		}
	}
	public boolean remove(Object o) {
		boolean mod = data.remove(o);
		if (mod) {
			dispatch(null, Collections.singleton((E)o));
		}
		return mod;
	}
	public boolean retainAll(Collection<?> c) {
		Iterator<E> it = data.iterator();
		Set<E> toRemove = new HashSet<E>();
		while (it.hasNext()) {
			E item = it.next();
			if (!c.contains(item)) {
				it.remove();
				toRemove.add(item);
			}
		}
		if (toRemove.isEmpty()) {
			return false;
		} else {
			dispatch(null, toRemove);
			return true;
		}
	}
	public boolean contains(Object o) {
		return data.contains(o);
	}
	public boolean containsAll(Collection<?> c) {
		return data.containsAll(c);
	}
	public boolean isEmpty() {
		return data.isEmpty();
	}
	public Object[] toArray() {
		return data.toArray();
	}
	public <T> T[] toArray(T[] a) {
		return data.toArray(a);
	}
}
