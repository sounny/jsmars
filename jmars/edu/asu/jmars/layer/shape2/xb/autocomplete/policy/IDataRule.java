package edu.asu.jmars.layer.shape2.xb.autocomplete.policy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface IDataRule {
	boolean isDataSource();
	boolean isRemovable();
	boolean isCaseSensitive();
	boolean isRequireJavaValidityCheck();
	Set<String> getData();
	Set<String> toLowerCase();
	List<String> lowerCaseToOrig(String lowcaseword);
	default Set<String> toUpperCase() {
		Set<String> s = new HashSet<>();
		return s;		
	}
	default String upperCaseToOrig(String uppercasseword) {
		String word = "";
		return word;
	}
}
