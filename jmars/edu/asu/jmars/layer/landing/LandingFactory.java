package edu.asu.jmars.layer.landing;

import java.awt.Color;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;

public class LandingFactory extends LViewFactory {
	
	static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.LANDING_SITE_IMG
       .withDisplayColor(imgLayerColor)));
	
	public LandingFactory(){
		type = "landing_site";
	}
	
	public LView createLView() {
		return null;
	}

	
	public void createLView(boolean async, LayerParameters l, String layerName, String configEntry){
		// Create LView with defaults
		LandingLayer layer = new LandingLayer(l, layerName, configEntry);
		LandingLView3D lview3d = new LandingLView3D(layer);
		LandingLView lview = new LandingLView(layer, true, lview3d);
		lview.setLayerParameters(l);
		lview.originatingFactory = this;
		LManager.receiveNewLView(lview);
	}
	
	public void createLView(boolean async, LayerParameters l) {
		createLView(async, l, null, "landing2020");
	}

	public LView recreateLView(SerializedParameters parmBlock) {
		LandingLayer landingLayer;
		
        if (parmBlock != null &&
                parmBlock instanceof LandingSiteSettings) {
                LandingSiteSettings settings = (LandingSiteSettings) parmBlock;
                landingLayer = new LandingLayer(settings);
        } else {
        	landingLayer = new LandingLayer();
        }
	
        LandingLView3D lview3d = new LandingLView3D(landingLayer);
		LandingLView lview = new LandingLView(landingLayer, true, lview3d);
		lview.setLayerParameters(((LandingSiteSettings)parmBlock).myLP);
		lview.originatingFactory = this;
		return lview;
	}

	public String getDesc() {
		return "Landing Site";
	}

	public String getName() {
		return "Landing Site";
	}
	
	 @Override 
	 public Icon getLayerIcon() {
		 return layerTypeIcon;	
	 }	

}
