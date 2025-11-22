package edu.asu.jmars.layer.threed;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import java.awt.Color;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import edu.asu.jmars.LoginWindow2;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

public class ThreeDFactory extends LViewFactory {
	private static DebugLog log = DebugLog.instance();
	private static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	private static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.THREE_D_LAYER_IMG
			                                   .withDisplayColor(imgLayerColor)));	
	
	{
		if (LoginWindow2.getInitialize3DFlag()) {
			check3DAvailability();
		}
	}
	
	private static boolean check3DAvailability() {
		boolean available = false;
		try {
			// Test for the availability of the vector math libraries
			Class.forName("javax.vecmath.Vector3f", false, ThreeDFactory.class.getClassLoader());
			// Test for the availability of the JOGL 3D libraries
			Class.forName("com.jogamp.opengl.GLCapabilities", false, ThreeDFactory.class.getClassLoader());
			Class.forName("com.jogamp.opengl.util.texture.Texture", false, ThreeDFactory.class.getClassLoader());
			// Test for the availability of the JOGL native 3D libraries
	        GLCapabilities capabilities = new GLCapabilities(GLProfile.getDefault());	
	        available = true;
		} catch (Exception e) {
			edu.asu.jmars.util.DebugLog.instance().aprint(e.getMessage());
			display3DError("The 3D Layer is not available due to missing 3D class files");
			edu.asu.jmars.util.DebugLog.instance().aprint("The 3D Layer is not available due to missing 3D class files");
		} catch (Error er) {
			// we really don't want to catch any Errors except this one
			if (er instanceof java.lang.UnsatisfiedLinkError) {
				display3DError("The 3D Layer is not available due to missing 3D library files");
				edu.asu.jmars.util.DebugLog.instance().aprint("The 3D Layer is not available due to missing 3D library files");
			}
		}
		return available;
	}
	
	protected static void display3DError(final String s) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
					Util.showMessageDialog(
							s,
							"3D Layer Error",
							JOptionPane.ERROR_MESSAGE);
			}
		});
	}
	
	private StartupParameters settings = new StartupParameters();

	public ThreeDFactory() {
		type = "3d";
	}

	/** Create a default 3D layer - currently the user must manually add this layer */
	public Layer.LView createLView() {
		return null;
	}
	
	// Supply the proper name and description.
	public String getName() {
		return "3D Layer";
	}
	
	public String getDesc() {
		return "A layer for rendering a scene in 3D";
	}
	
	/** Create a new 3D layer  - uses MOLA topographic elevation by default */
	public void createLView(boolean async, LayerParameters l) {
		LView view = createLView(new StartupParameters());
		if(view != null){
			view.setLayerParameters(l);
			LManager.receiveNewLView(view);
		}

	}
	
	/** Restore a 3D layer if parmBlock has the right type, otherwise creates a new default layer */
	public Layer.LView recreateLView(SerializedParameters parmBlock) {
		return createLView((StartupParameters)parmBlock);
	}
	
	/** Creates a 3D layer and view from the given startup parameters */
	private Layer.LView createLView(StartupParameters parms) {
		// Don't create a 3D Layer if the necessary libraries aren't available
		if (!ThreeDFactory.check3DAvailability()) {
			return null;
		}
		// create a new layer to be shared by all the views
		//if the parms are null though, just create a new 3d layer with the default elevation source
		if(parms == null){
			parms = new StartupParameters();
			//Let the user know this isn't necessarily what was saved in the old session
			log.aprintln("The original 3D layer was unable to restore properly. This 3D layer contains " +
							"default settings (for elevation, exaggeration, lighting, etc.).");
		}
		//if the parms aren't null, check the scaling mode...old sessions used an int, which is 
		// obsolete...new versions use a string, populate this string if it is null
		else{
			//if there is no scaleMode (old params) and there is a scaling mode
			// (should be set to 0,1,2) then convert from int to string
			if(parms.scaleMode == null && parms.scalingMode>-1){
				parms.scaleMode = ThreeDCanvas.scaleIntToString.get(parms.scalingMode);
			}
			//if there is still no mode (not in map, or there was no int greater than
			// -1 to begin with), then set the mode to the default (auto scale)
			if(parms.scaleMode == null){
				parms.scaleMode = ThreeDCanvas.SCALE_MODE_AUTO_SCALE;
			}
		}
		ThreeDLayer layer = new ThreeDLayer(parms);
		// create the main view
		ThreeDLView view = new ThreeDLView(layer, parms);
		// by default, the main view provides the viewing area
		layer.setActiveView(view); // <- JNN: My F5 refreshing problem is here
		// silliness for session serializing
		view.originatingFactory = this;
		// cause an immediate view change
		view.setVisible(true);
		return view;
	}
	
	@Override
	public Icon getLayerIcon() {
	    return layerTypeIcon;
	}
}
