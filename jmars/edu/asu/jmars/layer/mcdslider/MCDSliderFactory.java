package edu.asu.jmars.layer.mcdslider;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.SerializedParameters;

/**
 * 
 * @author srcheese
 *
 */
public class MCDSliderFactory extends LViewFactory{
	
	public MCDSliderFactory(){
		type = "map_slider";
	}
	
	/**
	 * Is called from the default block in the AddLayer dialog
	 * in the createButton method
	 */
	public void createLView(boolean async, LayerParameters lp){
		String instrument = lp.options.get(0);
		//get the version string to pass to the layer
		String vStr = lp.options.get(1);
		MCDLayerSettings settings = new MCDLayerSettings();
		settings.instrument = instrument;
		settings.name = lp.name;
		MCDSliderLayer layer = new MCDSliderLayer(vStr, settings);
		MCDSliderLView lview = new MCDSliderLView(layer, false);
		layer.setViewToUpdate(lview);
		lview.originatingFactory = this;
		lview.setLayerParameters(lp);
		LManager.receiveNewLView(lview);
	}

	@Override
	public LView recreateLView(SerializedParameters parmBlock) {
		MCDLayerSettings settings = (MCDLayerSettings) parmBlock;
		MCDSliderLayer layer = new MCDSliderLayer(settings.versionString, settings);
		MCDSliderLView lview = new MCDSliderLView(layer, false);
		layer.setViewToUpdate(lview);
		lview.originatingFactory = this;
		return lview;
	}

}
