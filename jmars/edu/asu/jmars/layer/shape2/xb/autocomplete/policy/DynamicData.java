package edu.asu.jmars.layer.shape2.xb.autocomplete.policy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import edu.asu.jmars.layer.util.features.Field;

public class DynamicData implements IDataRule {
	private Set<String> mydata = new HashSet<>();
	private  Multimap<String, String> wordToLowerCase = HashMultimap.create();
	public static final String USER_DATA_IDENTIFIER = "User column";

	public DynamicData(Set<Field> dynamicdata) {
		mydata.addAll(convertFromFieldToString(dynamicdata));
	}

	private Set<String> convertFromFieldToString(Set<Field> dynamicdata) {
		String word = "";
		Set<String> newwords = new HashSet<>();
		for (Field field : dynamicdata) {
			//intentionally add extra space after ":" to avoid clash with java.auto.txt
			word = field.name + " :   " + field.type.getSimpleName() + " - " + USER_DATA_IDENTIFIER;
			newwords.add(word);
		}
		return newwords;
	}

	@Override
	public boolean isDataSource() {	//provides data for autocomplete
		return true;
	}

	@Override
	public boolean isRemovable() {    //this is schema "Fields", which are changable per user choice of Custom Layer and a file in layer's Focus panel
		return true;
	}

	@Override
	public boolean isCaseSensitive() { //assume that jmars Fields are case-sensitive
		return true;
	}

	@Override
	public boolean isRequireJavaValidityCheck() {    //required, as colum names may not be compliant with java identifier rules for compilation by JEL
		return true;
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
			//we use MultiMap, so same key can map to a multiple values.
			//for ex key=cos(double) :integer ->values(cos(double) : integer and COS(double) : integer)
			wordToLowerCase.put(word.toLowerCase(), word);  
			s.add(word.toLowerCase());
		}
		return s;
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
