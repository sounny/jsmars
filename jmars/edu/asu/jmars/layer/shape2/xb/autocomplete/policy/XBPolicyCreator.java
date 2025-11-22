package edu.asu.jmars.layer.shape2.xb.autocomplete.policy;

import static edu.asu.jmars.layer.shape2.xb.autocomplete.policy.PolicyID.XB_SYNTAX_AND_FIELDS;
import java.util.Observable;
import java.util.Set;
import edu.asu.jmars.layer.shape2.xb.data.service.Data;
import edu.asu.jmars.layer.shape2.xb.data.service.DataServiceEvent;
import edu.asu.jmars.layer.shape2.xb.data.service.IDataServiceEventListener;
import edu.asu.jmars.layer.shape2.xb.data.service.Syntax;
import edu.asu.jmars.layer.util.features.Field;

public class XBPolicyCreator implements IDataServiceEventListener {
	
	private XBAutoCompletePolicy policy;
	private static PolicyChangedObservable policyObservable = null;

	private static final XBPolicyCreator instance = new XBPolicyCreator();
	private final Syntax syntaxservice;
	
	public static XBPolicyCreator Instance()
	{
		return instance;
	}

	public IPolicy getPolicy() {
		return this.policy;
	}
	
	public PolicyChangedObservable policyObservable() {
		return policyObservable;
	}	

	private XBPolicyCreator()
	{
		syntaxservice = Syntax.SERVICE;
		buildXBAutocompletePolicy();
		Data.SERVICE.addDataEventListener(this);
		policyObservable = new PolicyChangedObservable();	
	}

	private void buildXBAutocompletePolicy() {
		this.policy = new XBAutoCompletePolicy(XB_SYNTAX_AND_FIELDS);

		// Allow constant data set that is java syntax, functions like "cos", "sin"
		Set<String> syntaxdata = syntaxservice.getData();
		IDataRule staticDataRule = new StaticData(syntaxdata);
		this.policy.addRule(staticDataRule);
		
		//allow dynamic data - this is Schema <Fields>
		removeOldDynamicRules(this.policy);
		addNewDynamicRule(this.policy);
	}

	@Override
	public void handleDataServiceEvent(DataServiceEvent ev) {
		removeOldDynamicRules(this.policy);
		addNewDynamicRule(this.policy);
		policyObservable.policyChanged(this);
	}

	private void addNewDynamicRule(XBAutoCompletePolicy policy) {
		Set<Field> dynamicdata = Data.SERVICE.getData();
		IDataRule dynamicDataRule = new DynamicData(dynamicdata);
		policy.addRule(dynamicDataRule);		
	}

	private void removeOldDynamicRules(XBAutoCompletePolicy policy) {
		policy.removeRule();
	}
	
	public static class PolicyChangedObservable extends Observable { //observable for policy changes

		PolicyChangedObservable() {
			super();
		}

		void policyChanged(Object data) {
			setChanged();
			notifyObservers(data);
		}
	}		
	
}

