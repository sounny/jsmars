package edu.asu.jmars.swing.quick.add.layer;

import java.io.File;
import java.io.IOException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.map2.MapLViewFactory;
import edu.asu.jmars.layer.map2.custom.CM_Manager;
import edu.asu.jmars.layer.profile.ProfileLView;
import edu.asu.jmars.layer.shape2.ShapeFactory;
import edu.asu.jmars.layer.shape2.ShapeLView;
import edu.asu.jmars.layer.threed.ThreeDLView;
import edu.asu.jmars.lmanager.AddLayerDialog;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HttpRequestType;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.asu.jmars.viz3d.core.geometry.TriangleMesh;


public class CommandReceiver {

	public CommandReceiver() {
	}

	public static void addCustomShapesLayer() {
		LayerParameters l = LayerParameters.lParameters.stream().filter(
				lparam -> "shape".equalsIgnoreCase(lparam.type) 
				&& "Custom Shape Layer".equalsIgnoreCase(lparam.name))
				.findAny().orElse(null);
		if (l != null) {
			ShapeLView shpLView = (ShapeLView) new ShapeFactory().newInstance(false, l);
			LManager.receiveNewLView(shpLView);
			LManager.getLManager().repaint();
		}
		else {
			Util.showMessageDialog(
					"Unable to add the selected layer.  Please update to the latest version of JMARS or contact the support team.",
					"Error", JOptionPane.ERROR_MESSAGE);
		}		
	}
	
	public void loadCustomShapesLayer() {
		LayerParameters l = LayerParameters.lParameters.stream().filter(
				lparam -> "shape".equalsIgnoreCase(lparam.type) 
				&& "Custom Shape Layer".equalsIgnoreCase(lparam.name))
				.findAny().orElse(null);
		if (l != null) {
			ShapeLView shpLView = (ShapeLView) new ShapeFactory().newInstance(false, l);
			LManager.receiveNewLView(shpLView);
			LManager.getLManager().repaint();
		}
		else {
			Util.showMessageDialog(
					"Unable to add the selected layer.  Please update to the latest version of JMARS or contact the support team.",
					"Error", JOptionPane.ERROR_MESSAGE);
		}		
	}
	
	
	public void loadCraterCount() {
		LayerParameters l = LayerParameters.lParameters.stream().filter(
				lparam -> "crater_counting".equalsIgnoreCase(lparam.type) 
				&& "Crater Counting".equalsIgnoreCase(lparam.name))
				.findAny().orElse(null);				
		LViewFactory factory = LViewFactory.findFactoryType("crater_counting");
		if (l != null && factory != null) {
			factory.createLView(false, l);
			LManager.getLManager().repaint();
		} else {
			Util.showMessageDialog(
					"Unable to add the selected layer.  Please update to the latest version of JMARS or contact the support team.",
					"Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	

	public void loadCustomMaps() {
		CM_Manager mgr = CM_Manager.getInstance();
		mgr.setLocationRelativeTo(Main.mainFrame);
		mgr.setVisible(true);
		mgr.setSelectedTab(CM_Manager.TAB_UPLOAD);
	}
	

	public void load3DLayer(boolean forceLoad) {
		boolean loadNewLayer = true;
		if (!forceLoad) {
			//quick access icon will not load a new 3d layer if one is already loaded.
			LView threeDView = null;
			for (LView view : LManager.getLManager().getViewList()) {
				if (view instanceof ThreeDLView) {
					threeDView = view;
					break;
				}
			}
			if (threeDView != null) {
				loadNewLayer = false;
				LManager.getLManager().setActiveLView(threeDView);
				LManager.getLManager().accessSelectedOptions(false,0);//open up the 3d focus panel
			}
		}
		if (loadNewLayer) {
			LayerParameters l = LayerParameters.lParameters.stream().filter(lparam -> "3d".equalsIgnoreCase(lparam.type))
					.findAny().orElse(null);
			if (l != null) {
				LViewFactory factory = LViewFactory.findFactoryType(l.type);
				if (factory != null) {
					factory.createLView(false, l);
					LManager.getLManager().repaint();
				}
				LManager.getLManager().accessSelectedOptions(false,0);//open up the 3d focus panel
			}
			else {
				Util.showMessageDialog(
						"Unable to add the selected layer.  Please update to the latest version of JMARS or contact the support team.",
						"Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	public void load3DView() {
		String shapeModelUrl = Config.get("default_shape_model_url", Main.DEFAULT_SHAPE_MODEL_URL);
		shapeModelUrl += Main.getCurrentBody();
		ThreeDManager mgr = ThreeDManager.getInstance();
	    File meshFile;
	    String[] modelResponse = Util.getTextHttpResponse(shapeModelUrl, HttpRequestType.GET);
	    String selectedShapeModel = modelResponse[0];
    	meshFile = Util.getCachedFile(selectedShapeModel, true);
    	try {
    		boolean isUnitSphere = false;
    		if(selectedShapeModel.contains("UnitSphere")){
    			isUnitSphere = true;
    		}
    		TriangleMesh mesh = new TriangleMesh(meshFile, Main.getCurrentBody(), isUnitSphere);
    		mgr.applyShapeModel(mesh);
    		mgr.generateExtents();
		} catch (IOException ioe) {
			DebugLog.instance().aprintln("Error: Could not create 3D view window.");
			DebugLog.instance().aprintln(ioe);
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				mgr.show();
			}
		});
		
	}
	public void loadVR() {
		LView threeDView = null;
		for (LView view : LManager.getLManager().getViewList()) {
			if (view instanceof ThreeDLView) {
				threeDView = view;
			}
		}
		if (threeDView == null) {
			load3DLayer(true);
		} else {
			LManager.getLManager().setActiveLView(threeDView);
		}
		LManager.getLManager().accessSelectedOptions(false,1);
		
	}
	
	public void loadChartsView() { //Chart Manager goes here
		for (LView view : LManager.getLManager().getViewList()) {
			if (view instanceof ProfileLView) {
				if (((ProfileLView) view).chartFocusPanel != null) {
					int tabs = ((ProfileLView) view).chartFocusPanel.getTabCount();
					if (tabs >= 2) {
						((ProfileLView) view).chartFocusPanel.setSelectedIndex(1); // Chart tab
					} else {
						((ProfileLView) view).chartFocusPanel.setSelectedIndex(0);
					}
					if (!((ProfileLView) view).chartFocusPanel.isShowing()) {
						((ProfileLView) view).chartFocusPanel.showInFrame();
					}
				}
				break;
			}
		}
	}

	public static void closeChartsView() { // close Chart Manager when Load Session or Body Switch
		for (LView view : LManager.getLManager().getViewList()) {
			if (view instanceof ProfileLView) {
				ProfileLView profileview = ((ProfileLView) view);
				if (profileview.chartFocusPanel != null) {
					profileview.chartFocusPanel.close();
				}
				break;
			}
		}
	}

	public void loadAdvancedMap() {
		new MapLViewFactory().createLView(true, Main.mainFrame);
		LManager.getLManager().repaint();		
	}
	
	public void showFavorites() {
		AddLayerDialog.getInstance().showFavoriteTab();
	}
}
