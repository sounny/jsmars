package edu.asu.jmars.layer.shape2.xb.autocomplete.policy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class StaticData implements IDataRule {
	private Set<String> mydata = new HashSet<>();
	private  Multimap<String, String> wordToLowerCase = HashMultimap.create();

	public StaticData(Set<String> mydata) {
		this.mydata.addAll(mydata);
	}

	@Override
	public boolean isRemovable() {	//java syntax is constant data, java.Math functions, loaded from file java.auto.txt; doesn't change
		return false;
	}

	@Override
	public boolean isCaseSensitive() {	//java Math functions are case sensitive
		return true;
	}

	@Override
	public boolean isRequireJavaValidityCheck() { //not required, as we added them to java.auto.txt exactly as in java.Math
		return false;
	}

	@Override
	public Set<String> getData() {
		Set<String> s = new HashSet<>();
		s.addAll(mydata);
		return s;
	}

	@Override
	public Set<String> toLowerCase() {
		Set<String> s = new HashSet<>();
		wordToLowerCase.clear();
		for(String word : mydata) {
			wordToLowerCase.put(word.toLowerCase(), word);
			s.add(word.toLowerCase());
		}
		return s;
	}

	@Override
	public boolean isDataSource() {  //provides data for autocomplete
		return true;
	}
	

	@Override
	public List<String> lowerCaseToOrig(String lowcaseword) {
		List<String> origcase = new ArrayList<>();
		Iterator<String> iterator = wordToLowerCase.get(lowcaseword).iterator();
		while (iterator.hasNext()) {
			origcase.add(iterator.next());
		}
		return origcase;	
	}	
}
