package edu.asu.jmars.layer.stamp;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * This comparator takes a list of StampComparators which is order
 * sensitive, and compares stamps using those Comparators, in order.
 */
public class StampGroupComparator implements Comparator<StampShape> {

	private ArrayList<StampComparator> comparators;
	
	public StampGroupComparator(ArrayList<StampComparator> comparatorList){
		comparators = comparatorList;
	}

	public int compare(StampShape o1, StampShape o2) {
		for(StampComparator sc : comparators){
			int result = sc.compare(o1, o2);
			if(result != 0) {
				return result;
			}
		}
		return 0;
	}	
}
