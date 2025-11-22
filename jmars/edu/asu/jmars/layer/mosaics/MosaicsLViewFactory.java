package edu.asu.jmars.layer.mosaics;

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

public class MosaicsLViewFactory extends LViewFactory {
	
	static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.MOSAICS_LAYER_IMG
       .withDisplayColor(imgLayerColor)));	
	
	
	public MosaicsLViewFactory(){
		super("Mosaics", "Displays blended mosaics of many images over areas of interest");
		type = "mosaic";
	}
	
	public LView createLView() {
		return null;
	}

	public void createLView(boolean async, LayerParameters l) {
		String dataUrl = "";
		// If there is a dataURL provided with the layerParameters, use it.
		// Current examples: THEMIS Mosaics, HRSC Mosaics
		if (l!=null && l.options!=null && l.options.size()>0) {
			dataUrl = l.options.get(0);
		}
		MosaicsLView lview = buildLView(dataUrl,null);
		lview.setLayerParameters(l);
		// If there's a name provided by the layer parameters, use it
		if (l!=null && l.name!=null) {
			lview.setName(l.name);
		}
		LManager.receiveNewLView(lview);
	}

	public LView recreateLView(SerializedParameters parmBlock) {
		String url = "";
		MosaicsLView.InitialParams params = null;
		
		// If there is a url saved in the parmBlock (coming from saved session, etc), use it
		if (parmBlock!=null) {
			params = (MosaicsLView.InitialParams) parmBlock;
			url = params.url;
		}
		MosaicsLView lview = buildLView(url,params);
		if (params!=null && params.name!=null) {
			lview.setName(params.name);
		}
		return lview;
	}
	
	private MosaicsLView buildLView(String dataUrl, MosaicsLView.InitialParams params){
		MosaicsLayer layer = new MosaicsLayer(dataUrl,params);
		MosaicsLView3D lview3d = new MosaicsLView3D(layer);
		MosaicsLView lview = new MosaicsLView(layer, lview3d);
		lview.originatingFactory = this;
		return lview;
	}
	
	  @Override 
	  public Icon getLayerIcon() {
		  return layerTypeIcon;	
	   }	

}
