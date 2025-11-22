package edu.asu.jmars.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Provides a one dimensional range index in the 'double' coordinate space. Each
 * range is associated with an Object, and to the extent that the bin size
 * ideally suites the dataset, all operations are constant-time. Sparse and
 * packed datasets have the same memory requirements.
 */
public class RangeIndex<T> {
	private double binSize;
	private boolean useHashMap;
	private Map<Integer, Map<T,Tuple<T>>> bins = new HashMap<Integer, Map<T,Tuple<T>>> ();
	private Map<T, Tuple<T>> values;

	/**
	 * Constructs a new empty index.
	 * 
	 * @param binSize
	 *            Indicates the size of each cell bin. This value is the major
	 *            way to control the performance of the index.
	 * @param useHashMap
	 *            If true, the index will use a HashMap internally. If false,
	 *            the index will use an IdentityHashMap internally. Hashing is
	 *            used to ensure near-optimal lookups. If the values inserted
	 *            into the index do not implement hashCode and equals as defined
	 *            by the Java API, then this argument must be false, and
	 *            remove() must be called with the same object reference that
	 *            was passed to add().
	 */
	public RangeIndex (double binSize, boolean useHashMap) {
		this.binSize = binSize;
		this.useHashMap = useHashMap;
		this.values = makeMap();
	}

	/**
	 * Inserts the given value into all cells under the given extent.
	 * 
	 * @param min
	 *            Beginning of this value's extent.
	 * @param max
	 *            End of this value's extent.
	 * @param value
	 *            Value to insert into the index.
	 */
	public void add (double min, double max, T value) {
		Tuple<T> t = new Tuple<T> (min, max, value);
		int start = getBin(t.min, false);
		int end = getBin(t.max, true);

		values.put(value, t);

		for (int i = start; i <= end; i++) {
			Integer binNum = new Integer (i);
			Map<T,Tuple<T>> bin = bins.get(binNum);
			if (bin == null)
				bins.put(binNum, bin = makeMap());
			bin.put(value,t);
		}
	}

	/**
	 * Removes the given reference from the index. If 'false' was passed to the
	 * useHashMap argument of the constructor, then this must be the same
	 * reference that was passed to add().
	 * 
	 * @param value
	 *            The reference to remove.
	 */
	public void remove (T value) {
		Tuple<T> t = values.get(value);
		if (t == null)
			return;
		int start = getBin(t.min, false);
		int end = getBin(t.max, true);
		for (int i = start; i <= end; i++) {
			Integer bin = new Integer(i);
			Map<T,Tuple<T>> contents = bins.get(bin);
			if (contents.size() > 1)
				contents.remove(value);
			else
				bins.remove(bin);
		}
		values.remove(value);
	}

	/**
	 * Searches only the bins in the index that might contain data for the
	 * requested region and returns the results.
	 * 
	 * @param min
	 *            Minimum extent of the query region.
	 * @param max
	 *            Maximum extent of the query region.
	 * @return A set of all values in the queried extent, in no particular
	 *         order.
	 */
	public Set<T> query (double min, double max) {
		Map<T,Tuple<T>> results = makeMap();
		int start = getBin(min, false);
		int end = getBin(max, true);
		for (int i = start; i <= end; i++) {
			Map<T,Tuple<T>> bin = bins.get(i);
			if (bin != null) {
				for (Tuple<T> t: bin.values()) {
					if (t.max >= min && t.min <= max)
						results.put(t.value,null);
				}
			}
		}
		return results.keySet();
	}

	/**
	 * @return Returns the number of elements that have been inserted into the
	 *         index.
	 */
	public int size () {
		return values.size();
	}

	/**
	 * Clears the index of all previously-inserted values.
	 */
	public void clear () {
		bins.clear();
		values.clear();
	}

	/**
	 * Maps a position to an int bin, rounding up iff up==true.
	 */
	private int getBin (double pos, boolean up) {
		return (int)(up ? Math.ceil(pos / binSize) : Math.floor(pos / binSize));
	}

	/**
	 * Returns a Map suitable for hashing the caller's values.
	 */
	private Map<T,Tuple<T>> makeMap () {
		if (useHashMap)
			return new HashMap<T,Tuple<T>>();
		else
			return new IdentityHashMap<T,Tuple<T>>();
	}

	/**
	 * Simple container for the extent and value.
	 */
	private static class Tuple<TT> {
		public Tuple(double min, double max, TT value) {
			this.min = min; this.max = max; this.value = value;
		}
		double min;
		double max;
		TT value;
	}

	static class Test {
		public static void main (String[] args) {
			testResults();
			testStats();
		}
		private static void testResults() {
			RangeIndex<String> index = new RangeIndex<String> (10, true);
			index.add(0, 5, "A");
			index.add(3, 7, "B");
			index.add(5, 10, "C");
			index.add(7, 15, "D");
			dump ("Size: " + index.size(), null);
			dump ("AB", index.query(-2,4));
			dump ("ABC", index.query(5,5));
			dump ("BC", index.query(6,6));
			dump ("D", index.query(15,20));
			index.remove ("B");
			dump ("Size: " + index.size(), null);
			dump ("A", index.query(-2,4));
			dump ("AC", index.query(5,5));
			dump ("C", index.query(6,6));
			dump ("D", index.query(15,20));
			index.remove ("C");
			index.remove ("D");
			dump ("A", index.query(-2,4));
			dump ("A", index.query(5,5));
			dump ("was C, now empty", index.query(6,6));
			dump ("was D, now empty", index.query(15,20));
		}
		private static void testStats() {
			Integer[] values = new Integer[100000];
			for (int i = 0; i < values.length; i++)
				values[i] = new Integer(i);
			double baseWidth = 50;
			RangeIndex<Integer> index = new RangeIndex<Integer>(baseWidth*1.5, false);
			int expectedCellSizeAvg = 10;
			double maxPos = values.length/expectedCellSizeAvg*baseWidth;
			for (int i = 0; i < values.length; i++) {
				double pos = Math.random()*maxPos;
				double a = pos + Math.random()*baseWidth - baseWidth/2;
				double b = pos + Math.random()*baseWidth - baseWidth/2;
				index.add(Math.min(a,b), Math.max(a,b), values[i]);
			}
			Set<Integer> distinctItems = new HashSet<Integer>();
			int count = 0;
			int sum = 0;
			for (double pos = 0; pos <= maxPos; pos += baseWidth) {
				Set<Integer> contents = index.query(pos, pos+baseWidth);
				distinctItems.addAll(contents);
				count ++;
				sum += contents.size();
			}
			dump(count + " queries results in " + sum + " items, " + distinctItems.size() + " distinct", null);
		}
		private static void dump (String msg, Set<?> set) {
			System.out.println(msg + (set != null ? ":\n\t" + Util.join("\n\t", set) : ""));
		}
	}
}