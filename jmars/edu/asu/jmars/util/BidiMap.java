package edu.asu.jmars.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BidiMap<A,B> {
	private Map<A,B> left = new HashMap<A,B> ();
	private Map<B,A> right = new HashMap<B,A> ();
	/**
	 * Add a new association to the bidi map.
	 * @param left The left key.
	 * @param right The right key.
	 */
	public void add (A left, B right) {
		this.left.put(left, right);
		this.right.put(right, left);
	}
	/**
	 * Remove an association by its left key.
	 * @param left The key in the left map of the association to remove.
	 */
	public void removeLeft (A left) {
		right.remove(this.left.remove (left));
	}
	/**
	 * Remove an association by its right key.
	 * @param right The key in the right map of the association to remove.
	 */
	public void removeRight (B right) {
		left.remove(this.right.remove(right));
	}
	/**
	 * Returns right key from the left key.
	 */
	public B getLeft (A left) {
		return this.left.get(left);
	}
	/**
	 * Return the left key from the right key.
	 */
	public A getRight (B right) {
		return this.right.get(right);
	}
	/**
	 * Get the left keys as a set.
	 */
	public Set<A> leftKeys () {
		return left.keySet();
	}
	/**
	 * Get the right keys as a set.
	 */
	public Set<B> rightKeys () {
		return right.keySet();
	}
	/**
	 * Get the size of the map.
	 */
	public int size() {
		return left.size(); // always == right.size()
	}
}
