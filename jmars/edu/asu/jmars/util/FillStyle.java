package edu.asu.jmars.util;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.imageio.ImageIO;

import edu.asu.jmars.Main;

/**
 * A class for handling types of fill patterns in a SortingTable.
 * This had to be encoded for both the SortingTable 
 * which can handle FillStyle as an editable attribute as well as the Feature
 * object which needs it.
 * 
 * This class is implemented using LineType as a starting point... for better or worse
 * 
 * @see edu.asu.jmars.layer.util.features.Feature
 * 
 */
public class FillStyle implements Serializable, Comparable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3416687566950470564L;
	private static String[] imageList = {"102-R.png","428-K.png","429-K.png","427-K.png","214-K.png","134-R.png","430-K.png","327-K.png","136-K.png",
			"434-K.png","435-K.png","116-K.png","119-K.png","137-K.png","436-K.png","429-C.png"};
	String paintId;
	
	transient BufferedImage myImage;

	public static HashMap<String, BufferedImage> id2Image = new LinkedHashMap<String, BufferedImage>();
	
	static {
		id2Image.put("None", null);
		String basePatternDir = "resources/geologic-patterns-1.0/";
		try {
			for (String file : imageList) {
				BufferedImage bi = ImageIO.read(Main.getResourceAsStream(basePatternDir+file));
				if (bi==null) continue;
			
				id2Image.put(file.substring(0,file.lastIndexOf(".")), bi);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (id2Image.size() == 0){
			System.out.println("Pattern data doesn't exist");
		}
	}
		
	
	public static HashMap<String, PlanetaryFill> id2PlanetaryFill = new LinkedHashMap<String, PlanetaryFill>();
	
	public static class PlanetaryFill {
		public String description;
		String symbolId;
		public String tooltip;
		PlanetaryFill(String newDesc, String tooltip, String newId) {
			description=newDesc;
			symbolId=newId;
			this.tooltip = tooltip;
			id2PlanetaryFill.put(newId, this);
		}
	}
	
	static {
		new PlanetaryFill("Dark-colored<br>ejecta","Dark-colored ejecta", "428-K");		
		new PlanetaryFill("Light-colored<br>ejecta / halo","Light-colored ejecta / halo", "429-K");
		new PlanetaryFill("Terrace deposits","Terrace deposits", "427-K");
		new PlanetaryFill("Dark-colored<br>mantling material","Dark-colored mantling material", "214-K");
		new PlanetaryFill("Secondary crater<br>field","Secondary crater field", "102-R");
		new PlanetaryFill("Diffuse highland-<br>lowland boundary...","Diffuse highland-lowland boundary scarp", "134-R");
		new PlanetaryFill("Joint or fracture<br>pattern","Joint or fracture pattern", "430-K");
		new PlanetaryFill("Area of reticulate<br>grooves","Area of reticulate grooves", "327-K");
		new PlanetaryFill("Low albedo,<br>smooth material","Low albedo, smooth material", "136-K");
		new PlanetaryFill("Airburst spot","Airburst spot", "434-K");
		new PlanetaryFill("Mantling material,<br>Light-colored","Mantling material, Light-colored", "435-K");
		new PlanetaryFill("Splotch","Splotch", "116-K");
		new PlanetaryFill("Reticulate pattern<br>on plains","Reticulate pattern on plains", "119-K");
		new PlanetaryFill("Fracture zone","Fracture zone", "137-K");
		new PlanetaryFill("Superficial crater <br>material having...","Superficial crater material having weak radar back-scatter coefficient", "436-K");
		new PlanetaryFill("Halo without <br>associated crater","Halo without associated crater", "429-C");
	}
	
	/**
	 * Default constructor. Creates a solid line pattern.
	 */
	public FillStyle() {
		paintId = "None";
		myImage = null;
	}
	
	/**
	 * Constructor which takes a paint-id.
	 *  
	 * @param paintId One of the supported paint-ids.
	 */
	public FillStyle(String paintId){
		this.paintId = paintId;
		findMyImage();
	}
		
	/**
	 * Returns the paint-id.
	 * 
	 * @return Paint id.
	 */
	public String getType(){
		return paintId;
	}
			
	// When we restore from a session, the serialized object knows the name of the image, but the image itself has gone away
	// We want to run this ONCE after deserialization, to re-establish our myImage variable
	private transient boolean imageSearched=false;
	private BufferedImage findMyImage() {
		if (myImage!=null || imageSearched) return myImage;
		
		BufferedImage bi = id2Image.get(paintId);
		if (bi==null) {
			bi=id2Image.get(paintId+".png");
			if (bi==null) {
				bi=id2Image.get(paintId+".tif");
			}
		}
		
		imageSearched=true;
		myImage = bi;
		
		return myImage;
	}
	
	public Paint getPaint(int ppd) {
		findMyImage();
		
		if (myImage==null) {
			return null;
		}
						
		double ratio = 0.5;
		
		return new TexturePaint(myImage, new Rectangle2D.Double(0,0,ratio*myImage.getWidth()/ppd, ratio*myImage.getHeight()/ppd));		
	}
	
	public Paint getReversedPaint(int ppd) {
		findMyImage();
		
		if (myImage==null) {
			return null;
		}
						
        AffineTransform at = new AffineTransform();
        at.concatenate(AffineTransform.getScaleInstance(1, -1));
        at.concatenate(AffineTransform.getTranslateInstance(0, -myImage.getHeight()));
        
        BufferedImage newImage = new BufferedImage(
        		myImage.getWidth(), myImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = newImage.createGraphics();
        g.transform(at);
        g.drawImage(myImage, 0, 0, null);
        g.dispose();
		
		double ratio = 0.5;
		
		return new TexturePaint(newImage, new Rectangle2D.Double(0,0,ratio*newImage.getWidth()/ppd, ratio*newImage.getHeight()/ppd));		
	}
	
	/**
	 * Return some format of textual representation for this object.
	 */
	public String toString(){
		return paintId;
	}

	@Override
	public int compareTo(Object o) {
		if (o instanceof FillStyle) {
			FillStyle otherType = (FillStyle)o;
			return paintId.compareTo(otherType.paintId);
		}
		return 0;
	}
}
