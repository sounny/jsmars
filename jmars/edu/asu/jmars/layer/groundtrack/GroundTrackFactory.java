package edu.asu.jmars.layer.groundtrack;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;

public class GroundTrackFactory extends LViewFactory {
	
	static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
    static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.GROUNDTRACK_LAYER_IMG
			    .withDisplayColor(imgLayerColor)));
	
	static final LayerParams[] SPACECRAFT = {
			new LayerParams(-53, "ODY", "Mars Odyssey"),
			new LayerParams(-94, "MGS", "Mars Global Surveyor"),
			new LayerParams(-41, "MEX", "Mars Express") };

	public GroundTrackFactory() {
		super("Groundtracks", "Orbital tracks showing the sub-spacecraft path");
		type = "groundtrack";
	}

	/**
	 ** Maps Strings (spacecraft names) to GroundTrackLayers.
	 **/
	private static Map layers;

	public Layer.LView createLView() {
		return null;
	}

	public void createLView(boolean async, LayerParameters l) {
		LView view = recreateLView(SPACECRAFT[0]);
		view.setLayerParameters(l);
		LManager.receiveNewLView(view);
	}

	public Layer.LView recreateLView(SerializedParameters sp) {
		LayerParams lp = (LayerParams) sp;

		GroundTrackLView3D lview3d = new GroundTrackLView3D();
		GroundTrackLView view = new GroundTrackLView(lp, lview3d);
		view.originatingFactory = this;
		return view;
	}

	static class LayerParams implements SerializedParameters {
		int id;
		String craft;
		String desc;

		LayerParams(int id, String craft, String desc) {
			this.id = id;
			this.craft = craft;
			this.desc = desc;
		}

		GroundTrackLayer getLayer() {
			if (layers == null)
				layers = new HashMap();

			GroundTrackLayer layer = (GroundTrackLayer) layers.get(craft);
			if (layer == null)
				layers.put(craft, layer = new GroundTrackLayer(id, this));

			return layer;
		}

		public String toString() {
			return craft + " - " + desc;
		}
	}
	
	@Override 
	public Icon getLayerIcon() {
 	    return layerTypeIcon;
    }    	
}
