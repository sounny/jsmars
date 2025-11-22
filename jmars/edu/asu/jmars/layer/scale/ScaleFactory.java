package edu.asu.jmars.layer.scale;

import java.awt.Color;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.Config;

public class ScaleFactory extends LViewFactory {
	
	static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.SCALE_BAR_IMG
        .withDisplayColor(imgLayerColor)));	
	
	public ScaleFactory(){
		type = "scalebar";
	}
	

	public void createLView(boolean async, LayerParameters l) {
		LView view = realCreateLView(new ScaleParameters());
		view.setLayerParameters(l);
		LManager.receiveNewLView(view);
	}

	private Layer.LView realCreateLView(ScaleParameters params) {
		ScaleLayer layer = new ScaleLayer(params);
		layer.initialLayerData = params;
		ScaleLView3D lview3d = new ScaleLView3D(layer);
		Layer.LView view = new ScaleLView(true, layer, params, lview3d);
		view.originatingFactory = this;
		view.setOverlayId(OVERLAY_ID_SCALE);
		view.setVisible(true);
		return view;
	}

	public String getName() {
		return ("Map Scalebar");
	}

	public String getDesc() {
		return ("A layer which provides a scale marker on the map");
	}

	public LView recreateLView(SerializedParameters parmBlock) {
		if (parmBlock instanceof ScaleParameters) {
			return realCreateLView((ScaleParameters)parmBlock);
		} else {
			return realCreateLView(new ScaleParameters());
		}
	}
	
	@Override
    public LView showByDefault() {
        return null;
    }


    @Override
    public LView showDefaultCartographyLView() {
		Layer.LView view = this.realCreateLView(new ScaleParameters());
		view.setOverlayId(OVERLAY_ID_SCALE);
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
