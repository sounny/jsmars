package edu.asu.jmars.layer.stamp;

import java.util.Comparator;

public class StampComparator implements Comparator<StampShape> {
	int compareCol;
	boolean reverse;
	public StampComparator(int colNum, boolean reverse) {
		compareCol=colNum;
		this.reverse=reverse;
	}
	@Override
	public int compare(StampShape o1, StampShape o2) {
		Comparable c1 = (Comparable)o1.getData(compareCol);
		Comparable c2 = (Comparable)o2.getData(compareCol);
		
		// These 3 lines taken from a stack overflow answer				
		if (c1==null && c2==null) return 0;										
		if (c1==null) return 1;
		if (c2==null) return -1;
		
		int result = c1.compareTo(c2);
		
		if (reverse) {
			result = result * -1;
		}
		
		return result;									
	}
};
