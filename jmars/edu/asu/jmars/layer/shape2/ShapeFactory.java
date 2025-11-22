package edu.asu.jmars.layer.shape2;

import java.awt.Color;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.shape2.ShapeLView.ShapeParams;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;

public class ShapeFactory extends LViewFactory {
	
	static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.SHAPE_LAYER_IMG
       .withDisplayColor(imgLayerColor)));	
	
	public ShapeFactory() {
		super("Shapes", "Features shape drawing, loading, and saving.");
		type = "shape";
	}

	public ShapeFactory(String name, String desc) {
		super(name, desc);
		type = "shape";
	}

	public LView createLView() {
		return null;
	}
	
	public ShapeLView newInstance(boolean isReadOnly, LayerParameters lp) {
		ShapeLayer layer = new ShapeLayer(isReadOnly);
		ShapeLView3D lview3d = new ShapeLView3D(layer);
		ShapeLView lview = new ShapeLView(layer, true, isReadOnly, lp, lview3d);
		lview.originatingFactory = this;
		return lview;
	}
	
	public ShapeLView newInstance(LayerParameters lp) {
	// the true passed into the shapelayer means it is a read only shape file	
		boolean isReadOnly = false;
		if (lp.name!=null)
			isReadOnly = true;
		ShapeLayer layer = new ShapeLayer(isReadOnly);
		ShapeLView3D lview3d = new ShapeLView3D(layer);
		ShapeLView lview = new ShapeLView(layer, true, isReadOnly, lp, lview3d);
		lview.originatingFactory = this;
		return lview;
	}
	
	public void createLView(boolean async, LayerParameters lp) {
		LManager.receiveNewLView(newInstance(false, lp));
	}
	
	public LView recreateLView(SerializedParameters parmBlock) {
		boolean isReadOnly=false;
		String name = "";
		LayerParameters lp = null;
		if (parmBlock instanceof ShapeParams){
			lp = ((ShapeParams)parmBlock).layerParams;
			if(lp!=null)
				name = lp.name;

			if (!name.equals("Custom Shape Layer") && !name.equals("")){
				isReadOnly=true;
			}
		}
		ShapeLView slv = newInstance(isReadOnly, lp);
		slv.setName(name);
		return slv;
	}

	 @Override 
	 public Icon getLayerIcon() {
	        return layerTypeIcon;
	   }	
	
}
