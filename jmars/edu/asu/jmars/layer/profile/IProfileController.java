package edu.asu.jmars.layer.profile;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.jfree.data.Range;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.profile.config.Config;
import edu.asu.jmars.layer.profile.manager.ProfileManagerMode;


public interface IProfileController {
	
	 void requestDataUpdate(Map<Integer,Shape> newViewExtents, Range spanPerPlot, int ppd);					
			
	 void addNewProfile(Map<Integer,Shape> profileLines);
	
	 void selectedProfiles(Map<Integer,Shape> profileLines, ProfileManagerMode mode);
		
	 void requestChartData();
	
	 void userAddedMapSource(List<MapSource> userSelectedSources);	

     void registerShape(Pair<Integer, String> pair);
    
     void viewChartForProfile(int ID);

	 void cueChanged(int profileID, Point2D midWorld);

	 void crosshairChanged(Pair<Integer, Point2D> pair, ProfileLView view);

	 void addDrawingShapeChartOnly(Map<Integer, Shape> selectedprofiles);

	 void configChanged();

	 void createChartFromConfiguration(Config chartconfig);

	 void lineWidthChanged(Pair<ProfileLView, Integer> pair);
}


