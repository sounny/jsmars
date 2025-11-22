package edu.asu.jmars.layer.nomenclature;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.NOMENCLATURE_LAYER_IMG;
import java.awt.Color;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;



public class NomenclatureFactory extends LViewFactory {
	
	static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(NOMENCLATURE_LAYER_IMG
			      .withDisplayColor(imgLayerColor)));
	
	public NomenclatureFactory(){
		type = "nomenclature";
	}
	
	private static DebugLog log = DebugLog.instance();

    	/**
    	 * Overriding the superclass method
    	 * 
	 * @since change bodies
    	 */
    public Layer.LView showByDefault() {
        return null;//this is being turned off because it is added as an overlay layer by default
    }

	// Implement the main factory entry point.
	public void createLView(boolean async, LayerParameters l) {
        // Create a default set of parameters
        LView view = realCreateLView();
        view.setLayerParameters(l);
		LManager.receiveNewLView(view);
	 }

	// Internal utility method
	private Layer.LView realCreateLView() {
		NomenclatureLayer layer = new NomenclatureLayer();
		NomenclatureLView3D lview3d = new NomenclatureLView3D(layer);
		Layer.LView view = new NomenclatureLView(layer, lview3d);
		view.originatingFactory = this;
		view.setVisible(true);
		view.setOverlayId(OVERLAY_ID_NOMENCLATURE);
		return  view;
	 }

    //used to restore a view from a save state
    public Layer.LView recreateLView(SerializedParameters parmBlock) {
        return realCreateLView();
    }

	// Supply the proper name and description.
	public String getName() {
		return ("Nomenclature");
	 }
	public String getDesc() {
		return("A layer which provides geographic nomenclature");
	 }

    @Override
    public LView showDefaultCartographyLView() {
    	Layer.LView view = this.realCreateLView();
    	view.setOverlayId(OVERLAY_ID_NOMENCLATURE);
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
