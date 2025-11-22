package edu.asu.jmars.layer.north;

import java.awt.Color;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.layer.SerializedParameters;

public class NorthFactory extends LViewFactory{
	
	static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	static final Icon layerTypeIcon =  new ImageIcon(ImageFactory.createImage(ImageCatalogItem.NORTH_LAYER_IMG
        .withDisplayColor(imgLayerColor).withStrokeColor(imgLayerColor)));	
	
	public NorthFactory(){
		type = "north_arrow";
	}

	@Override
	public LView recreateLView(SerializedParameters parmBlock) {
		if (parmBlock instanceof NorthSettings) {
			return buildLView((NorthSettings)parmBlock);
		} else {
			return buildLView(new NorthSettings());
		}
	}

	public String getName(){
		return "North Arrow";
	}
	
	
	public LView buildLView(NorthSettings parmBlock){
		NorthLayer layer = new NorthLayer(parmBlock);
		//set the initialLayerData object to be the NorthSettings object
		// so that during a session save, the settings are written out properly
		layer.initialLayerData = parmBlock;
		NorthLView3D lview3d = new NorthLView3D(layer);
		LView lview = new NorthLView(layer, lview3d);
		lview.originatingFactory = this;
		lview.setOverlayId(OVERLAY_ID_NORTH);
		return lview;
	}
	
	public void createLView(boolean async, LayerParameters lp){
		LView lview = buildLView(new NorthSettings());
		lview.setLayerParameters(lp);
		LManager.receiveNewLView(lview);
	}
	
	private Layer.LView realCreateLView() {
        NorthLayer layer = new NorthLayer(new NorthSettings());
        NorthLView3D lview3d = new NorthLView3D(layer);
        Layer.LView view = new NorthLView(layer, lview3d);
        view.originatingFactory = this;
        view.setVisible(true);
        return  view;
     }
	
    @Override
    public LView showByDefault() {
        return null;
    }

    @Override
    public LView showDefaultCartographyLView() {
    	LView view = this.realCreateLView();
		view.setOverlayId(OVERLAY_ID_NORTH);
    	view.setOverlayFlag(true);
    	String configSetting = Config.get(view.getOverlayId(), null);
    	if (configSetting != null && "off".equalsIgnoreCase(configSetting)) {
    		view.setVisible(false);
    	}
        return view;
    }
    
    @Override 
    public Icon getLayerIcon() {
         return layerTypeIcon;
    }    
	
}
