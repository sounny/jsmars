package edu.asu.jmars.layer.profile;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.PROFILE_LAYER_IMG;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.apache.commons.lang3.tuple.Pair;
import org.jfree.data.Range;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.profile.chart.ProfileChartView;
import edu.asu.jmars.layer.profile.config.ConfigureChartView;
import edu.asu.jmars.layer.profile.manager.ProfileManagerMode;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;



/**
 * The Controller implements PropertyChangeListener interface. 
 * The PropertyChangeListener interface allows the controller to receive events from the model (ProfileLayer). 
 * The Controller takes a reference to the model as a constructor parameter and uses it to register itself with 
 * the model. Views register themselves via the addView method.
 * Events are passed to registered views via the overridden propertyChange method from  
 * PropertyChangeListener interface
 */
public class ProfileFactory extends LViewFactory implements IProfileController, PropertyChangeListener  {

	static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	static final Icon layerTypeIcon = new ImageIcon(
			ImageFactory.createImage(PROFILE_LAYER_IMG.withStrokeColor(imgLayerColor)));
	 private List<IProfileModelEventListener> views = new ArrayList<>();
	 private ProfileLayer model;
	 ProfileLView3D profileLView3d;
	 ProfileLView profileLView;
	 ProfileChartView profileChartView;
	 ConfigureChartView configureChartView;
	 ProfileLView.SavedParams savedParams = null;
	 private static DebugLog log = DebugLog.instance();
	 private static LayerParameters layerparams;
	 

	public ProfileFactory() {			
		type = "profile";
		layerparams = new LayerParameters("Profile", "", getLayerDescription(), "", "https://jmars.mars.asu.edu/profile-lines");
	}
  
	@Override
	public Layer.LView showByDefault() {
		return null;// this is being turned off because it is added as an overlay layer by default
	}

  
	// Implement the main factory entry point.
	public void createLView(boolean async, LayerParameters l) {		
		LView view = realCreateLView();		
		LManager.receiveNewLView(view);
	}

	private Layer.LView realCreateLView() {	
		this.removeAllRegisteredViews();		
		createNewProfileLayer();
		createProfileLView3D();
		createProfileLView();			
		profileLView.setLayerParameters(ProfileFactory.layerparams);			
		MapServerFactory.whenMapServersReady(new Runnable() {
			@Override
			public void run() { 
				profileLView.initEventReceiver();
				ProfileFactory.this.addView(profileLView);				
				model.init(profileLView); // init model, which is this view's layer
				model.addPropertyChangeListener(ProfileFactory.this);
				profileLView.createProfileManagerFocusPanel();
				createProfileChartView();
				createConfigureChartView(); //default config
				if (savedParams == null) {
					profileChartView.init(); //gets default numeric source
				}
				if (savedParams != null) {
					profileLView.updatedProfileLines(savedParams);
					configureChartView.notifyRestoredFromSession(savedParams); //restore Config from session
				    ((ProfileFocusPanel)(profileLView.focusPanel)).notifyRestoredFromSession(savedParams);
				}
				
				profileLView.createChartFocusPanel(profileChartView, configureChartView, savedParams);
				ProfileFactory.this.addView((IProfileModelEventListener) profileLView.getFocusPanel());				
			}
		});
		return profileLView;
	}	

	 private void createNewProfileLayer() {	
	    model = new ProfileLayer();		
	}	
	
	private void createProfileLView() {
		profileLView = new ProfileLView(model, profileLView3d);	
		profileLView.originatingFactory = this;
		profileLView.setVisible(true);
		profileLView.setOverlayId(OVERLAY_ID_PROFILE);			
	}
	

	private void createProfileLView3D() {
		profileLView3d = new ProfileLView3D(model);			
	}

	
	private void createProfileChartView() {
		try {
			profileChartView = new ProfileChartView(profileLView, this);
			this.addView(profileChartView);
		} catch (Exception e) {
			e.printStackTrace();
			log.aprintln("Failed to initialize profile chart view (possibly capabilities took too long to load). ");
		}
	}
	
	private void createConfigureChartView() {
		configureChartView = new ConfigureChartView(profileLView, this);		
		this.addView(configureChartView);
	}	
	
	private String getLayerDescription() {
		StringBuilder desc = new StringBuilder();
		desc.append("Profile layer works jointly with \"Draw profile\" tool to draw cross-sectional view along a line drawn through a portion of a map. ");
		desc.append("\"Draw profile\" tool is located at the main JMARS toolbar and works very similar to how you would construct a profile using paper map, ");
		desc.append("i.e. just find some part(s) of the map that you are interested in, so that you get useful information, and draw a line. ");
		desc.append("Using \"Draw profile\" tool and Profile layer you can draw multiple profiles at various locations on a map. ");
		desc.append("Once profiles are drawn, the chart is created. ");
		desc.append("To view the chart, click \"Chart\" tool located at \"QuickAccess\" bar or select \"View chart\" from profile's context menu.");
		return desc.toString();
	}
	

	// used to restore a view from saved state
	@Override
	public Layer.LView recreateLView(SerializedParameters parmBlock) {
		if (!(parmBlock instanceof ProfileLView.SavedParams))
			return null;
		this.savedParams = (ProfileLView.SavedParams) parmBlock;
		return realCreateLView();
	}
	

	@Override
	public LView showDefaultCartographyLView() {
		Layer.LView view = this.realCreateLView();
		view.setOverlayFlag(true);
		String configSetting = Config.get(view.getOverlayId(), null);
		if (configSetting != null && "off".equalsIgnoreCase(configSetting)) {
			view.setVisible(false);
		}
		return view;
	}

	@Override
	public String getName() {
		return ("Profile");
	}

	@Override
	public String getDesc() {
		return ("Topographic profiles drawing layer");
	}

	@Override
	public Icon getLayerIcon() {
		return layerTypeIcon;
	}
	
	public void addView(IProfileModelEventListener view) {
          this.views.add(view);
   }
	
	public void removeAllRegisteredViews() {
		this.views.clear();
	}
     
   
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		 for (IProfileModelEventListener view : this.views) {
	          view.modelPropertyChange(evt);
	     }		
	}
	
	@Override
	public void requestDataUpdate(Map<Integer, Shape> newViewExtents, Range span, int ppd) {
		model.requestDataUpdate(newViewExtents, span, ppd);		
	}	
	
	@Override
	public void addNewProfile(Map<Integer,Shape> profileLines) {
		model.addProfileLine(profileLines);		
	}
	
	@Override
	public void addDrawingShapeChartOnly(Map<Integer, Shape> selectedprofiles) {
		model.addProfileLineChartOnly(selectedprofiles);	
	}		

	@Override
	public void selectedProfiles(Map<Integer, Shape> profileLines, ProfileManagerMode mode) {
		model.addSelectedProfilesToChart(profileLines, mode);
	}	
	
	@Override
	public void requestChartData() {
		model.requestMapSourceData();		
	}

	@Override
	public void userAddedMapSource(List<MapSource> userSelectedSources) {
		model.newMapSource(userSelectedSources);		
	}

	@Override
	public void registerShape(Pair<Integer, String> pair) {
		model.registerShape(pair);		
	}

	@Override
	public void viewChartForProfile(int ID) {
		model.requestViewChartForProfile(ID);		
	}
	
	@Override
	public void cueChanged(int profileID, Point2D midWorld) {
		model.requestChartCrosshairUpdateForProfile(profileID, midWorld);		
	}	

	@Override
	public void crosshairChanged(Pair<Integer, Point2D> pair, ProfileLView view) {
		model.requestCueUpdateForProfile(pair, view);
		
	}
	
	@Override
	public void configChanged() {
		model.requestConfigChanged();		
	}
	
	@Override
	public void createChartFromConfiguration(edu.asu.jmars.layer.profile.config.Config chartconfig) {
		model.createChart(chartconfig);
	}
	
	@Override
	public void lineWidthChanged(Pair<ProfileLView, Integer> pair) {
		model.lineWidthChanged(pair);		
	}

}
