/**
 * builds the 3D scene and allows for parameters of that scene to be modified either directly
 * (via attribute writing methods) or indirectly (via rebuilding the scene).
 *
 * @author James Winburn MSFF-ASU  11/04
 */
package edu.asu.jmars.layer.threed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.HashMap;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;

import javax.swing.JPanel;

import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

public class ThreeDCanvas extends JPanel {
	private static DebugLog log = DebugLog.instance();
	
	// final attributes.
	private final Dimension initialSize = new Dimension(400, 400);
	
	//one instance of the scale mode enumerations that is used across classes
	public static final String SCALE_MODE_AUTO_SCALE = "Auto-Scale";
	public static final String SCALE_MODE_ST_DEV = "Standard Deviation";
	public static final String SCALE_MODE_RANGE = "Range of Values";
	public static final String SCALE_MODE_ABSOLUTE = "Absolute Scaling";
	//this map is to preserve the restoring of old sessions
	public static HashMap<Integer, String> scaleIntToString = new HashMap<Integer, String>();
	static{
		scaleIntToString.put(0, SCALE_MODE_AUTO_SCALE);
		scaleIntToString.put(1, SCALE_MODE_ST_DEV);
		scaleIntToString.put(2, SCALE_MODE_RANGE);
		scaleIntToString.put(3, SCALE_MODE_ABSOLUTE);
	}
	
	private String currentScaleMode;
	
	// instance attributes
	private ThreeDLayer myLayer;
	private LView parent;
	ThreeDPanel canvas;
	
	private JPanel holdingPanel;
	private Object altitudeSource = null;
	private Elevation elevation;
	private StartupParameters settings = null;

	// Orientation properties of the platform: this is held separately because 
	// some of the scene modification methods require that the entire scene be
	// rebuilt and we want the orientation to remain consistent from one rebuild
	// to the next.  This also makes sure that the viewers of duplicate 3D layers 
	// aren't linked.

	public ThreeDCanvas(LView parent, StartupParameters settings) {
		myLayer = (ThreeDLayer)parent.getLayer();
		this.parent = parent;
		
		if(Float.compare(settings.scaleOffset, 0f) == 0) {
			settings.scaleOffset = (float) Config.get(Util.getProductBodyPrefix()+Config.CONFIG_THREED_SCALE_OFFSET, -0.002f);
		}
		
		initSettings(settings);
		
		holdingPanel = new JPanel();
		holdingPanel.setLayout(new BorderLayout());
		holdingPanel.setBackground(Color.black);
		holdingPanel.setPreferredSize(initialSize);
		setLayout(new BorderLayout());
		add(holdingPanel, BorderLayout.CENTER);
		currentScaleMode = settings.scaleMode; 
		
		// Because we repack the frame when we refresh, we need to set the 
		// the preferred size so that refreshing does not set the frame to its
		// initial size as well.
		holdingPanel.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				Dimension d = holdingPanel.getSize();
				holdingPanel.setPreferredSize(d);
			}
		});
	}

	public void initSettings(StartupParameters settings) {	
		this.settings = settings;
	}

	/**
	 * return to the orientation of the scene when it was first created.
	 */
	public void goHome(StartupParameters settings) {
		initSettings(settings);
		setScale(Float.parseFloat(settings.zScaleString)); // JNN: WARNING: zScaleString changes based off elevation data
		// Luckily, settings.zScaleString was reset to originalExaggeration ("1.0") in ThreeDFocus before goHome() is called
		if (canvas != null) {
			canvas.setSettings(this.settings);
			canvas.refresh();
		}		
		this.refresh();
	}
	
	public StartupParameters getSettings() {
		if (canvas != null) {
			return canvas.getSettings();
		} else if(settings != null) {
			return settings;
		} else {
			return null;
		}
	}
	
	public void setRestoreSettings(StartupParameters settings) {
		this.initSettings(settings);
	}

	/**
	 * This is called by the focus panel whenever new elevation data becomes available.
	 * This method was renamed from updateElevationMOLA to better reflect allowing
	 * other sources of elevation data in addition to MOLA
	 */
	public void updateElevationSource() {
		Raster data = myLayer.getElevationData();
		elevation = null;
		elevation = new Elevation(data, myLayer.getElevationSource().getIgnoreValue());
	}
	
	public Elevation getElevation(){
		return elevation;
	}


	public void createCanvas () {
		if (elevation == null) {
			log.aprintln("elevation is null.  huh?");
			return;
		}

		canvas = buildCanvas(elevation, parent.viewman.copyMainWindow(), Float.parseFloat(settings.zScaleString), this);

		holdingPanel.removeAll();
		
		holdingPanel.add(canvas, BorderLayout.CENTER);
		canvas.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				canvas.requestFocusInWindow();
			}
		});
        canvas.requestFocusInWindow();
	}
	
	/**
	 * Some attributes of the scene cannot be maintained dynamically.  Changing any of these
	 * attributes require the scene to be completely rebuilt.
	 */
	public void refresh() {
		if (elevation == null) {
			log.aprintln("elevation is null.  huh?");
			return;
		}
		// build the new canvas
		myLayer.setStatus(Color.yellow);
		
		if (canvas != null) {
			canvas.setElevation(elevation);
			canvas.setDecalImage(parent.viewman.copyMainWindow());
			canvas.setScaleString(settings.zScaleString);
			canvas.setExaggeration(settings.scaleOffset);
			canvas.setDirection(settings.directionalLightDirection);
			canvas.setDirectionalLightColor(settings.directionalLightColor);
			canvas.setDirectionalLightEnabled(settings.directionalLightBoolean);
			canvas.setBackgroundColor(settings.backgroundColor);
			canvas.refresh();
		} else {
			createCanvas();
		}	
		myLayer.setStatus(Util.darkGreen);		
	}

	/**
	 * sets the source of altitude data.  The scene must be rebuilt.
	 */
	public void setAltitudeSource(Object s) {
		altitudeSource = s;
	}

	public Object getAltitudeSource() {
		return altitudeSource;
	}

	/**
	 * set the scale mode of the scene: 
	 * @param mode:
	 * 			SCALE_MODE_ST_DEV = Standard Deviation
	 * 			SCALE_MODE_RANGE = Range
	 * 			SCALE_MODE_AUTO_SCALE = Mars's Radius
	 * @param currentExaggeration: The current exaggeration string that was in the exaggeration text field
	 */
	public void setScaleMode(String mode, String currentExaggeration)
	{
		if((!mode.equals(SCALE_MODE_ST_DEV))
				&& (!mode.equals(SCALE_MODE_RANGE))
				&& (!mode.equals(SCALE_MODE_ABSOLUTE))
				&& (!mode.equals(SCALE_MODE_AUTO_SCALE)))
		{
			// something weird happened, set to default
			currentScaleMode = SCALE_MODE_AUTO_SCALE;
		}
		else
		{
			currentScaleMode = mode;			
		}
		// the scale mode had changed, reset the scale
		setScale(Float.parseFloat(currentExaggeration));
	}
	
	/**
	 * set the altitudinal "scale" of the scene.  Since scene must be rebuilt, we must refresh.
	 */
	public void setScale(float f)
	{
		float toScale = settings.scaleOffsetThisBody;
		
		if(currentScaleMode.equals(SCALE_MODE_AUTO_SCALE))
		{
			settings.scaleOffset = f * toScale;
		}
		else if(currentScaleMode.equals(SCALE_MODE_RANGE))
		{
			toScale = -1.0f;
			// scale is negative because of direction camera is looking
			// i.e. negative z is backwards towards user
			// positive z is forward deeper inside computer
			if(elevation != null)
			{
				double range = elevation.getMaxAltitude() - elevation.getMinAltitude();

				// to avoid division by zero, multiply 100 to get 100 percent
				toScale = (Double.compare(range, 0.0) == 0) ? 0.0f : (float)(toScale * 100f / range);
			}
			settings.scaleOffset = f * toScale;
		}
		else if(currentScaleMode.equals(SCALE_MODE_ST_DEV))
		{
			toScale = -1.0f;
			if(elevation != null)
			{
				double stdDev = elevation.getStandardDeviation();
				if (Double.isNaN(stdDev)) {
					stdDev = 0.0;
				}
				// to off-chance of division by zero
				toScale = (Double.compare(stdDev,0.0) == 0) ? -1.0f : (float)(toScale / stdDev);
			}
			settings.scaleOffset = f * toScale;			
		}
		else // if(currentScaleMode == ABSOLUTE) // default
		{
			float factor = -1;
			if (settings.scaleUnitsInKm) {
				factor = -0.001f;				
			}
			settings.scaleOffset = f * factor;
		}
	}

	/**
	 * Set whether or not to use the directional light.  Because of the problem of the material,
	 * we cannot add or not add the light dynamically.  The scene has to be rebuild.
	 */
	public void enableDirectionalLight(boolean b) {
		settings.directionalLightBoolean = b;
	}

	/**
	 * sets the direction of the DirectionalLight.  This can be done dynamically.
	 */
	public void setDirectionalLightDirection(float x, float y, float z) {
		settings.directionalLightDirection.set(x, y, z);
	}

	/**
	 * sets the color of the DirectionalLight. This can be done dynamically.
	 */
	public void setDirectionalLightColor(Color c) {
		settings.directionalLightColor = c;
	}

	/**
	 * sets the color of the background.  This can be done dynamically.
	 */
	public void setBackgroundColor(Color c) {
		settings.backgroundColor = c;
		if (canvas != null) {
			canvas.setBackgroundColor(settings.backgroundColor);
		}
	}

	public void enableBackplane(boolean b) {
		settings.backplaneBoolean = b;
		if (canvas != null) {
			canvas.setBackplaneEnabled(b);
		}
	}

	/**
	 * dumps the contents of the image to an external and user specified PNG file.
	 */
	public void savePNG(final String filePath) {
		// put the file name/path in the GL Queue to be executed
		canvas.addToQueue(new ThreeDAction() {
	         public void execute(GL target)
	         {
	     		IntBuffer view = IntBuffer.allocate(4);
	    		target.glGetIntegerv(GL2.GL_VIEWPORT, view);
	    		AWTGLReadBufferUtil readerUtil = new AWTGLReadBufferUtil(target.getGLProfile(),
	                    true);
	     		final BufferedImage img = readerUtil.readPixelsToBufferedImage(target, true) ;
	     		// save the file off of the current GL Context
	     		(new Runnable() {
	     			public void run() {
	    	    		Util.saveAsPng(img, filePath);
	     			}
	     		}).run();
	         }
	      }
		);	
		canvas.refresh();
	}
	
	
	public void saveBinarySTL(String path, String name, float baseThickness) throws IllegalArgumentException, IOException{
		canvas.saveBinarySTL(path, name, baseThickness);
	}
	
	// build the scene and display it.
	private ThreeDPanel buildCanvas(Elevation elevation, BufferedImage image, float scale, ThreeDCanvas panel) {
		return new ThreeDPanel(elevation, image, settings, panel); 
	}
	
	/**
	 * This should be called before the threeDCanvas is destroyed to make sure
	 * the SimpleUniverse is cleaned up.
	 */
	public void cleanup(){
		if (canvas != null){
			holdingPanel.removeAll();
			canvas.setVisible(false);
			canvas.destroy();
			canvas = null;
		}
	}
}