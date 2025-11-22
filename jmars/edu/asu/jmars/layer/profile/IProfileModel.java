package edu.asu.jmars.layer.profile;

import java.awt.Shape;
import java.beans.PropertyChangeListener;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import edu.asu.jmars.layer.profile.manager.ProfileManagerMode;


public interface IProfileModel {	
	 
	 String NEW_PROFILEDATA_EVENT = "IProfileModel.NEW_PROFILEDATA";
	 String CHART_ONLY_PROFILEDATA_EVENT = "IProfileModel.CHART_ONLY_PROFILEDATA";
	 String SELECTED_TO_PLOT_PROFILEDATA_EVENT = "IProfileModel.SELECTED_TO_PLOT_PROFILEDATA";
	 String DELETE_PROFILE_EVENT = "IProfileModel.DELETE_PROFILE";	
	 String PLOT_DATA_EVENT = "IProfileModel.PLOT_DATA";
	 String CHART_SAVE_AS_TEXT_EVENT = "IProfileModel.CHART_SAVE_AS_TEXT";
	 String NEW_PIPELINE_EVENT = "IProfileModel.NEW_PIPELINE_EVENT";
	 String NEW_MAPSOURCE_EVENT = "IProfileModel.NEW_MAPSOURCE_EVENT";
	 String REGISTER_SHAPE_EVENT = "IProfileModel.REGISTER_SHAPE_EVENT";
	 String VIEW_CHART_FOR_PROFILE = "IProfileModel.VIEW_CHART_FOR_PROFILE";
	 String UPDATE_CROSSHAIR_FOR_PROFILE = "IProfileModel.UPDATE_CROSSHAIR_FOR_PROFILE";
	 String UPDATE_CUE_FOR_PROFILE = "IProfileModel.UPDATE_CUE_FOR_PROFILE";
	 String REQUEST_CHART_CONFIG_CHANGED = "IProfileModel.REQUEST_CHART_CONFIG_CHANGED";
	 String CREATE_CHART = "IProfileModel.CREATE_CHART";
	 String REQUEST_PROFILE_LINE_WIDTH_CHANGED = "IProfileModel.REQUEST_PROFILE_LINE_WIDTH_CHANGED";
	 
	 void addProfileLine(Map<Integer,Shape> lines);  
	 
	 void addPropertyChangeListener(PropertyChangeListener listener);
	 
	 void removePropertyChangeListener(PropertyChangeListener listener);
	 
	 void addSelectedProfilesToChart(Map<Integer, Shape> profileLines, ProfileManagerMode mode);

	 void addProfileLineChartOnly(Map<Integer, Shape> selectedprofiles);

	 void requestConfigChanged();	 	
	 
	 void lineWidthChanged(Pair<ProfileLView,Integer> pair);
	 
}
