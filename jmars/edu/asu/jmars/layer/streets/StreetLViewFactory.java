package edu.asu.jmars.layer.streets;

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
import edu.asu.jmars.util.Util;

public class StreetLViewFactory extends LViewFactory {

	private int osmType;
	private static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	private static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.STREETS_LAYER_IMG
       .withDisplayColor(imgLayerColor)));		

	public StreetLViewFactory() {
		type = "open_street_map";
	}

	public void createLView(int osmType, LayerParameters l) {
		StreetLayer layer = new StreetLayer(osmType);
		this.osmType = osmType;
		layer.setOsmType(osmType);
		StreetLView lview = new StreetLView(layer,l);
		lview.getLayer().setStatus(Util.darkRed);
		lview.originatingFactory = this;
		LManager.receiveNewLView(lview);

	}

	public LView recreateLView(SerializedParameters parmBlock) {
		StreetLViewSettings settings = (StreetLViewSettings)parmBlock;
		StreetLView lview = new StreetLView(new StreetLayer(osmType), settings.layerParams);
		lview.getLayer().setStatus(Util.darkRed);
		lview.getLayer().setOsmType(settings.osmType);
		lview.originatingFactory = this;

		return lview;
	}

	public String getDesc() {
		return "A Layer that calls OpenStreetMap tiles";
	}
	
	 @Override 
	 public Icon getLayerIcon() {
	       return layerTypeIcon;
	   }	

}
