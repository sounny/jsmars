package edu.asu.jmars.layer.shape2.xb.autocomplete.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import edu.asu.jmars.layer.shape2.xb.autocomplete.policy.IDataRule;

public interface ITrieService {
	void build(Set<String> words);
    List<String> prefixSearch(String word);
    Map<IDataRule, Set<String>> getData();
}
