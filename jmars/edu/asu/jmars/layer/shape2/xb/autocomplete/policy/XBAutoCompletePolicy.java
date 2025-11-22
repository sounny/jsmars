package edu.asu.jmars.layer.shape2.xb.autocomplete.policy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class XBAutoCompletePolicy implements IPolicy {
	private PolicyID policyID;
	private List<IDataRule> rules;
	

	public XBAutoCompletePolicy(PolicyID policyID) {
		this.policyID = policyID;
		rules = new ArrayList<>();
	}

	@Override
	public void addRule(IDataRule datarule) {
		rules.add(datarule);
	}

	@Override
	public List<IDataRule> getRules() {
		List<IDataRule> policyrules = new ArrayList<>();
		policyrules.addAll(rules);		
		return policyrules;
	}

	@Override
	public IDataRule getRule() {
		return null;
	}

	@Override
	public PolicyID getID() {
		return policyID;
	}

	@Override
	public void removeRule() {
		for (Iterator<IDataRule> it = rules.iterator(); it.hasNext();) {
			IDataRule rule = it.next();
			if (rule.isRemovable()) {
				it.remove();
			}
		}
	}

}
