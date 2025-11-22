package edu.asu.jmars.util;

import java.util.Set;

public interface ObservableSetListener<E> {
	void change(Set<E> added, Set<E> removed);
}

