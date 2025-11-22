package edu.asu.jmars.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class Range implements Cloneable {
	abstract public double getMin();
	abstract public double getMax();
	public boolean isEmpty(){
		return getMin() == getMax();
	}
	
	public boolean isBetween(double val) {
		return val >= getMin() && val <= getMax();
	}
	
	public boolean isStrictlyBetween(double val){
		return val > getMin() && val < getMax();
	}
	
	public static Range narrow(Range r1, Range r2){
		double min = Math.max(r1.getMin(), r2.getMin());
		double max = Math.min(r1.getMax(), r2.getMax());
		
		if (max < min){
			min = max = 0;
		}
		
		return new Range.Double(min, max);
	}
	
	public Range clone(){
		try {
			return (Range)super.clone();
		}
		catch(CloneNotSupportedException ex){
			throw new RuntimeException(ex);
		}
	}
	
	public boolean equals(Object obj){
		if (obj instanceof Range){
			Range other = (Range)obj;
			return getMin() == other.getMin() && getMax() == other.getMax();
		}
		return false;
	}
	
	public static List<Range> coalesce(Collection<? extends Range> inRanges){
		List<Range> out = new ArrayList<Range>(inRanges);
		Collections.sort(out, new RangeComparator());
		
		for(int i=1; i<out.size(); ){
			Range rp = out.get(i-1);
			Range r = out.get(i);

			if (r.getMin() <= rp.getMax()){
				out.set(i, new Range.Double(Math.min(rp.getMin(), r.getMin()), Math.max(rp.getMax(), r.getMax())));
				out.remove(i-1);
			}
			else {
				i++;
			}
		}

		return out;
	}
	
	static class RangeComparator implements Comparator<Range> {
		public int compare(Range o1, Range o2) {
			if (o1 == null && o2 == null)
				return 0;
			if (o1 == null && o2 != null)
				return -1;
			if (o1 != null && o2 == null)
				return 1;
			
			double diff = o1.getMin() - o2.getMin();
			if (diff == 0)
				diff =  o1.getMax() - o2.getMax();
			
			return (int)Math.signum(diff);
		}
		
	}
	
	public static class Double extends Range implements Cloneable {
		public final double min, max;
		
		public Double(double min, double max){
			this.min = min;
			this.max = max;
		}
		public double getMin(){ return min; }
		public double getMax(){ return max; }
		
		public String toString(){ return ""+min+":"+max; }
		
		public Range clone(){
			Range clone = super.clone();
			return clone;
		}
	}

}

