package edu.asu.jmars.util;

import java.io.Serializable;
import java.util.ArrayList;

public class ListType implements Serializable, Comparable<String> {
	private ArrayList<String> values;
	
	public ListType(ArrayList<String> entries){
		values = entries;
	}
	
	public ArrayList<String> getValues(){
		return values;
	}
	
	public void add(String s){
		values.add(s);
	}
	
	public void remove(String s){
		values.remove(s);
	}

	@Override
	public int compareTo(String o) {
		return this.compareTo(o);
	}
	
	
}
