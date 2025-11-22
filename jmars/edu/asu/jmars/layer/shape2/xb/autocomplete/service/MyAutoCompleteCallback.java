package edu.asu.jmars.layer.shape2.xb.autocomplete.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import de.codesourcery.swing.autocomplete.AutoCompleteBehaviour.DefaultAutoCompleteCallback;
import edu.asu.jmars.layer.shape2.xb.autocomplete.policy.DynamicData;
import edu.asu.jmars.layer.shape2.xb.data.service.Data;

public class MyAutoCompleteCallback extends DefaultAutoCompleteCallback {
	private AutoCompleteService autocompleteEXPRservice;

		public MyAutoCompleteCallback(AutoCompleteService autocomplete) {
			this.autocompleteEXPRservice = autocomplete;
	}

		/*
		 * reformat suggestedWords list, such that all entries that are columns (or field names)
		 * appear at the top of the suggestions list, followed by
		 * entries that do not have that substring in them, for ex. Math functions
		 */
		@Override
		public List<String> getProposals(String input) {
			List<String> suggestedWords = new ArrayList<>();
			List<String> suggestedWordsUserFirst = new ArrayList<>();
			if (input == null || input.length() < 1) {
				return Collections.emptyList();
			}
			suggestedWords = autocompleteEXPRservice.prefixSearch(input.toLowerCase());
			for (String word : suggestedWords) {
				if (word.contains(DynamicData.USER_DATA_IDENTIFIER)) {
					suggestedWordsUserFirst.add(word);
				}
			}
			for (String word : suggestedWords) {
				if (!word.contains(DynamicData.USER_DATA_IDENTIFIER)) {
					suggestedWordsUserFirst.add(word);
				}
			}			
			
			return suggestedWordsUserFirst;
		}
		@Override
		public String getStringToInsert(Object value) {
			String s = (String) value;
			if (s == null) { return ""; }
			int ind = s.indexOf(":");
			if (ind != -1 && ((ind - 1) != -1)) {
				s = s.substring(0, (ind-1)); //return from start to ":", including space before it
			}
			if (("pi").equalsIgnoreCase(s) || ("e").equalsIgnoreCase(s)) {
				s = s.toUpperCase();    //return constants in upper case for JEL
			}
			//if Java system function, replace values in parenthesis with 0, like java.Math does
			if (! ((String) value).contains(DynamicData.USER_DATA_IDENTIFIER)) {
				s = Data.MATH_FUNCTION_VALIDATOR.validateIdentifier(s);
			}
			String valid = checkForJavaValidity(s);    //check for valid java identifier (for ex, if s = "Show Points"  then remove space
		    return valid;
		}

		private String checkForJavaValidity(String s) {
			 String validname = Data.SERVICE.getAliasForName(s);
			 //if suggestion is user column name, surround it with DELIM
             validname = (validname == null) ? s : Data.ALIAS_DELIM + s + Data.ALIAS_DELIM;   			
             return validname;
		}
			
}
