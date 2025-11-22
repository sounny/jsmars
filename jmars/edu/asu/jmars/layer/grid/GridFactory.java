package edu.asu.jmars.layer.grid;

import java.awt.Color;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.grid.GridLayer;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.Config;

public class GridFactory extends LViewFactory {
	
	 static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	 static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.GRID_LAYER_IMG
			    .withDisplayColor(imgLayerColor)));
    
	public GridFactory(){
		type = "llgrid";
	}
	
    public void createLView(boolean async, LayerParameters l) {
        LView view = realCreateLView(new GridParameters());
        view.setLayerParameters(l);
        LManager.receiveNewLView(view);
    }
    
	// used to restore a view from a save state
	public Layer.LView recreateLView(SerializedParameters parmBlock) {
        if (parmBlock instanceof GridParameters) {
            return realCreateLView((GridParameters)parmBlock);
        } else {
            return realCreateLView(new GridParameters()); // use defaults
        }
	}

	public String getName() {
		return "Lat/Lon Grid";
	}

	public String getDesc() {
		return "An adjustable grid of latitude/longitude lines.";
	}
	
    private Layer.LView realCreateLView(GridParameters params) {
        GridLayer layer = new GridLayer();
        layer.initialLayerData = params;
        GridLView3D lview3d = new GridLView3D(layer);
        Layer.LView view = new GridLView(true, layer, params, lview3d);
        view.originatingFactory = this;
		view.setOverlayId(OVERLAY_ID_GRID);
        view.setVisible(true);
        return view;
    }
    
    @Override
    public LView showDefaultCartographyLView() {
    	GridParameters gp = new GridParameters();
    	Layer.LView view = this.realCreateLView(gp);
    	view.setOverlayId(OVERLAY_ID_GRID);
    	view.setOverlayFlag(true);
        return view;
    }
    
    @Override 
    public Icon getLayerIcon() {
 		return layerTypeIcon;
    }    

}
