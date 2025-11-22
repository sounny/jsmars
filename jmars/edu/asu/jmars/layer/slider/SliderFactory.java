package edu.asu.jmars.layer.slider;

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

public class SliderFactory extends LViewFactory {
	
	static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.SLIDER_LAYER_IMG
       .withDisplayColor(imgLayerColor)));	
	
	public SliderFactory(){
		type = "timeslider";
	}
	
	public LView createLView() {
		return null;
	}

	public void createLView(LayerParameters lp) {
		LView view = new SliderLView(new SliderLayer(), lp);
		// TODO: How dumb is this?
		view.originatingFactory=this;
		LManager.receiveNewLView(view);
	}
	
	public LView recreateLView(SerializedParameters parmBlock) {
		return null;
	}

	public String getName() {
		return "Map Time-Slider";
	}
	
	 @Override 
	 public Icon getLayerIcon() {
            return layerTypeIcon;
	   }	
}
