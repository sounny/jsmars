package edu.asu.jmars.layer.crater;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CRATER_COUNT_IMG;
import java.awt.Color;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;

public class CraterFactory extends LViewFactory {
	
    static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(CRATER_COUNT_IMG
			  .withStrokeColor(imgLayerColor)));
	
	public CraterFactory(){
		type = "crater_counting";
	}
	
	public LView createLView() {
		return null;
	}

	public void createLView(boolean async, LayerParameters l) {
		// Create LView with defaults
		CraterLayer layer = new CraterLayer();
		CraterLView3D lview3d = new CraterLView3D(layer);
		CraterLView lview = new CraterLView(layer, lview3d);
		lview.setLayerParameters(l);
		lview.originatingFactory = this;
		LManager.receiveNewLView(lview);
	}

	public LView recreateLView(SerializedParameters parmBlock) {
		CraterLayer craterLayer;
		
        if (parmBlock != null &&
                parmBlock instanceof CraterSettings) {
                CraterSettings settings = (CraterSettings) parmBlock;
                craterLayer = new CraterLayer(settings);
        } else {
        	craterLayer = new CraterLayer();
        }
	
        CraterLView3D lview3d = new CraterLView3D(craterLayer);
		CraterLView lview = new CraterLView(craterLayer, lview3d);
		lview.setLayerParameters(((CraterSettings)parmBlock).myLP);
		lview.originatingFactory = this;
		return lview;
	}

	public String getDesc() {
		return "Crater Counting";
	}

	public String getName() {
		return "Crater Counting";
	}

	@Override
	public Icon getLayerIcon() {
		return layerTypeIcon;
	}

}
