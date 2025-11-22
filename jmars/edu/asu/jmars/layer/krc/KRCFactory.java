package edu.asu.jmars.layer.krc;

import java.awt.Color;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.krc.KRCLView.KRCParams;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;

public class KRCFactory extends LViewFactory{
	
	static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.KRC_LAYER_IMG
       .withDisplayColor(imgLayerColor)));	

	private static DebugLog log = DebugLog.instance();
	
	public KRCFactory() {
		type = "krc";
	}
	
	/**
	 * Is called from the default block in the AddLayer dialog
	 * in the createButton method
	 */
	public void createLView(boolean async, LayerParameters lp){
		//parse out the map source info for default values
		//should be in the following order:
		// elevation, albedo, ti, slope, azimuth
		ArrayList<String> sourceNames = lp.options;

		KRCLView lview = buildLView(sourceNames);
		lview.setLayerParameters(lp);
		LManager.receiveNewLView(lview);
	}
	
	
	/**
	 * Called when restoring sessions and layers
	 */
	public LView recreateLView(SerializedParameters parmBlock) {
		
		KRCLView lview;
		if(parmBlock instanceof KRCParams){
			KRCParams params = (KRCParams)parmBlock;
			//build the lview and layer
			lview = buildLView(params.sourceNames);
			//add all the krcdatapoints to the layer
			KRCLayer layer = (KRCLayer)lview.getLayer();
			for(KRCDataPoint dp : params.dataPoints){
				layer.addDataPoint(dp);
			}
		}else{
			lview = null;
			System.err.println("KRC parameters not recognized.  Failed to load layer.");
		}
		return lview;
	}
	
	
	//Logic needed when creating layer from scratch, and when
	// restoring layer.  Builds the layer and lview based on 
	// the map source names and returns the lview.
	private KRCLView buildLView(ArrayList<String> sourceNames){
		ArrayList<MapSource> sources = new ArrayList<MapSource>();
		MapServerFactory.whenMapServersReady(new Runnable() {
			@Override
			public void run() {
				MapServer server = MapServerFactory.getServerByName("default");
				for(String name : sourceNames){
					try {
						sources.add(server.getSourceByName(name));
					} catch (Exception e) {
						//log the Exception and continue
						log.println("Failed to load source "+name+" in KRC buildLView.");
					}
				}
			}
		});
		
		KRCLayer layer = new KRCLayer(sources);
		KRCLView3D lview3d = new KRCLView3D(layer);
		
		KRCLView lview = new KRCLView(layer, true, lview3d);
		lview.originatingFactory = this;
		
		return lview;
	}
	
	 @Override 
	 public Icon getLayerIcon() {
	    return layerTypeIcon;
	   }	

}
