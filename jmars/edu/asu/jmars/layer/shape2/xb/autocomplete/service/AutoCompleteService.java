package edu.asu.jmars.layer.shape2.xb.autocomplete.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Set;
import edu.asu.jmars.layer.shape2.xb.autocomplete.impl.Dictionary;
import edu.asu.jmars.layer.shape2.xb.autocomplete.impl.ITrie;
import edu.asu.jmars.layer.shape2.xb.autocomplete.policy.IPolicy;
import edu.asu.jmars.layer.shape2.xb.autocomplete.policy.IPolicyChangedObserver;
import edu.asu.jmars.layer.shape2.xb.autocomplete.policy.IDataRule;
import edu.asu.jmars.layer.shape2.xb.autocomplete.policy.XBPolicyCreator;
import edu.asu.jmars.layer.shape2.xb.autocomplete.policy.XBPolicyCreator.PolicyChangedObservable;

public class AutoCompleteService implements ITrieService, IPolicyChangedObserver {
	private ITrie dictionary;
	private XBPolicyCreator policyCreator;
	private IPolicy policy;
	private Map<IDataRule, Set<String>> dictionarydata;

	public AutoCompleteService(XBPolicyCreator xbpolicycreator) {
		this.dictionary = new Dictionary();
		this.policyCreator = xbpolicycreator;
		this.policy = policyCreator.getPolicy();
		this.dictionarydata = new HashMap<>();
		this.policyCreator.policyObservable().addObserver(this);
		initPopulateDictionary();
	}

	@Override
	public void build(Set<String> words) {
		for (String word : words) {
			this.dictionary.insert(word);
		}
	}

	@Override
	public List<String> prefixSearch(String word) {
		List<String> suggestions = new ArrayList<>();
		suggestions = dictionary.startsWith(word);
		updateCaseToOrig(suggestions);
		return suggestions;
	}

	@Override
	public Map<IDataRule, Set<String>> getData() {
		Map<IDataRule, Set<String>> mymap = new HashMap<>();
		mymap.putAll(this.dictionarydata);
		return mymap;
	}
	
	// populate Dictionary based on Policy - such as constant and dynamic parts
		private void initPopulateDictionary() {
			Set<String> data = new HashSet<>();
			List<IDataRule> rules = this.policy.getRules();
			for (IDataRule rule : rules) {
				if (rule.isDataSource()) {
					Set<String> dd = rule.getData();
					this.dictionarydata.put(rule, dd); // add data as-is
					if (this.dictionary.isLowercase()) {
						dd = rule.toLowerCase();
					}
					data.addAll(dd);
				}
			}
			build(data);
		}

		private void updatePopulateDictionary() {
			removeOldDynamicData();
			aldNewDynamicData();
		}
	

	private void aldNewDynamicData() {
		Set<String> data = new HashSet<>();
		List<IDataRule> rules = this.policy.getRules();
		for (IDataRule rule : rules) {
			if (rule.isDataSource() && rule.isRemovable()) {
				Set<String> dd = rule.getData();
				this.dictionarydata.put(rule, dd); // add data as-is
				if (this.dictionary.isLowercase()) {
					dd = rule.toLowerCase();
				}
				data.addAll(dd);
			}
		}
		build(data);
	}

	private void removeOldDynamicData() { // remove from dictionarydata and corresponding words from Dictionary
		List<IDataRule> policyrules = this.policy.getRules();
		Iterator<Entry<IDataRule, Set<String>>> iterator = this.dictionarydata.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<IDataRule, Set<String>> ddentry = iterator.next();
			IDataRule rule = ddentry.getKey();
			if (rule.isRemovable() && !policyrules.contains(rule)) {
				removeFromDictionary(rule);
				iterator.remove();
			}
		}
	}

	private void removeFromDictionary(IDataRule rule) {
		Set<String> s = new HashSet<>();
		s.addAll(rule.getData());		
		if (this.dictionary.isLowercase()) {
				s.clear();
				s.addAll(rule.toLowerCase());
		}
		for (String word : s) {
			this.dictionary.remove(word);
		}	
	}

	private void updateCaseToOrig(List<String> suggestions) {
		List<String> orig = new ArrayList<>();
		List<String> copy = new ArrayList<>(suggestions);
		for (String suggestion : copy) {
			orig.clear();
			for (Map.Entry<IDataRule, Set<String>> ddentry : this.dictionarydata.entrySet()) {
				orig.clear();
				IDataRule rule = ddentry.getKey();
				orig.addAll(rule.lowerCaseToOrig(suggestion));
				if (orig.isEmpty()) {
					continue;
				} else {
					int ind = suggestions.indexOf(suggestion);
					if (ind != -1) {
						suggestions.set(ind, orig.get(0));
						//plus add other possible original strings for this suggestion
						if (orig.size() > 1) {
							for(int i =1; i < orig.size(); i++) {
								suggestions.add(orig.get(i));
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg == null)
			return;
		if (o instanceof PolicyChangedObservable) {
			if (arg instanceof XBPolicyCreator) {
				updatePopulateDictionary();
			}
		}
	}

}
