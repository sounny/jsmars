package edu.asu.jmars.layer.shape2.xb.autocomplete.policy;

import java.util.List;

public interface IPolicy {
	List<IDataRule> getRules();
	void addRule(IDataRule rule);
	IDataRule getRule();
	PolicyID getID();
	void removeRule();
}
