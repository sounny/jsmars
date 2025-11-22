package edu.asu.jmars.layer.profile;

import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.profile.ProfileLView.SavedParams;
import edu.asu.jmars.layer.profile.manager.ProfileManager;
import edu.asu.jmars.layer.profile.manager.ProfileManagerMode;


public class ProfileFocusPanel extends FocusPanel implements IProfileModelEventListener {
	private static final long serialVersionUID = 1L;		
	final ProfileLView profileLView;
	final ProfileFactory controller;
	final ProfileManager profileManager;
	private static  Map<String, Supplier<IChartEventHandler>> EVENT_HANDLER;

	
	private ProfileFocusPanel(Builder builder) {
		super(builder.profileLView);
		this.parentFrame.setTitle("Manage Profiles in Main View");
		this.profileLView = builder.profileLView;
		this.controller = builder.controller;
		ProfileManager.createCommonSettings(this.profileLView);
		this.profileManager = new ProfileManager(ProfileManagerMode.MANAGE);
		add("PROFILES", this.profileManager.getContentPane());
		add("ADJUSTMENTS", new ProfileSettings(this.profileLView, this.controller));
		initEventHandlers();		   
	}
	

	private void initEventHandlers() {
		final Map<String, Supplier<IChartEventHandler>> handlers = new HashMap<>();		
		handlers.put(IProfileModel.NEW_PROFILEDATA_EVENT, NewProfileEventHandler::new);		
		EVENT_HANDLER = Collections.unmodifiableMap(handlers);		
	}
	

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private ProfileLView profileLView;
		private ProfileFactory controller;
		
		private Builder() {
		}

		public Builder withProfileLView(ProfileLView profileLView) {
			this.profileLView = profileLView;
			return this;
		}
		
		public ProfileFocusPanel build() {
			return new ProfileFocusPanel(this);
		}

		public Builder withController(ProfileFactory control) {
			this.controller = control;
			return this;
		}
		
	}

	@Override
	public void modelPropertyChange(PropertyChangeEvent evt) {
		String propName = evt.getPropertyName();
		Object newVal = evt.getNewValue();
		Supplier<IChartEventHandler> handler = EVENT_HANDLER.get(propName);
		if (handler != null) {
		    handler.get().handleEvent(newVal);
		}		
	}
	
	private class NewProfileEventHandler implements IChartEventHandler {
		@Override
		public void handleEvent(Object newVal) {
			profileManager.manage();
		}
	}
	
	public void updateProfiles() {
		profileManager.manage();
	}

	public void notifyRestoredFromSession(SavedParams savedParams) {
		profileManager.manage();
	}


}

