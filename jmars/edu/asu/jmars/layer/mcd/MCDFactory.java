package edu.asu.jmars.layer.mcd;

import java.awt.Color;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;

public class MCDFactory extends LViewFactory{
	
	static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.MCD_LAYER_IMG
       .withDisplayColor(imgLayerColor)));	

	public MCDFactory() {
		type = "mcd_point";
	}
	
	/**
	 * Is called from the default block in the AddLayer dialog
	 * in the createButton method
	 */
	public void createLView(boolean async, LayerParameters lp){
		MCDLView lview = buildLView();
		lview.setLayerParameters(lp);
		LManager.receiveNewLView(lview);
	}
	
	private MCDLView buildLView(){
		MCDLayer layer = new MCDLayer();
		MCDLView3D lview3d = new MCDLView3D(layer);
		MCDLView lview = new MCDLView(layer, true, lview3d);
		lview.originatingFactory = this;
		
		return lview;
	}
	
	
	/**
	 * Called when restoring sessions
	 */
	public LView recreateLView(SerializedParameters parmBlock) {
		return null;
	}
	
	  @Override 
	  public Icon getLayerIcon() {
	    return layerTypeIcon;
	   }	

}
