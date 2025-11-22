package edu.asu.jmars.layer.map2.stages.composite;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import edu.asu.jmars.layer.map2.AlphaCombinerOp;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.PolyArea;
import edu.asu.jmars.util.Util;

public class RGBComposite extends CompositeStage implements Cloneable, Serializable {
	private static final long serialVersionUID = 2L;

	private static DebugLog log = DebugLog.instance();
	
	public static final String[] inputNames = new String[]{ "Red", "Green", "Blue" };
	
	private BufferedImage alphas = null;
	private AlphaCombinerOp alphaCombiner = null;
	
	/** @deprecated No longer contaminating the stage with an LManager problem! */
	private int myCnt=0;
	
	public RGBComposite(RGBCompositeSettings settings) {
		super(settings);
		alphaCombiner = new AlphaCombinerOp();
	}
	
	public synchronized MapData process(int input, MapData inputData, Area changedArea) {
		// setup source; get input band (if color, red from red input, green
		// from green, blue from blue; if gray, always band 0
		int inputBand = (new MapAttr(inputData.getImage()).isColor() ? input : 0);
		WritableRaster source = Util.getBands(inputData.getImage(), inputBand);
		
		// setup target; use input stage number as band number for color,
		// hardcoded '3' for alpha
		MapData output = super.process(input, inputData, changedArea);
		WritableRaster target = Util.getBands(output.getImage(), input);
		
		// for each changed rectangle, copy source to destination and set alpha
		for (Rectangle2D r: new PolyArea(changedArea).getRectangles()) {
			Raster rectSource = MapData.getRasterForWorld(source, inputData.getRequest().getExtent(), r);
			WritableRaster rectTarget = MapData.getRasterForWorld(target, output.getRequest().getExtent(), r);
			rectTarget.setRect(rectSource);
			
			if (inputData.getImage().getAlphaRaster() != null){
				WritableRaster srcAlphaRaster = MapData.getRasterForWorld(inputData.getImage().getAlphaRaster(), inputData.getRequest().getExtent(), r);
				WritableRaster tgtAlphaRaster = MapData.getRasterForWorld(Util.getBands(alphas, input), output.getRequest().getExtent(), r);
				tgtAlphaRaster.setRect(srcAlphaRaster);
			}
		}
		
		// narrow changed area down to the aggregate valid area
		changedArea.intersect(output.getValidArea());
		
		// set opaque alpha for each piece of the changed area
		WritableRaster outAlpha = Util.getBands(output.getImage(), 3);
		for (Rectangle2D r: new PolyArea(changedArea).getRectangles()) {
			WritableRaster inAlphaRect = MapData.getRasterForWorld(alphas.getRaster(), output.getRequest().getExtent(), r);
			inAlphaRect = alphaCombiner.filter(inAlphaRect, null);
			WritableRaster outAlphaRect = MapData.getRasterForWorld(outAlpha, output.getRequest().getExtent(), r);
			outAlphaRect.setRect(inAlphaRect.createChild(0, 0, outAlphaRect.getWidth(), outAlphaRect.getHeight(), 0, 0, new int[]{ 0 }));
		}
		
		return output;
	}
	
	public BufferedImage makeBufferedImage(int width, int height) {
		log.println(hashCode() + ": Creating image");
		
		// Create a new alpha channel image and fill it with all 255s
		alphas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2alphas = alphas.createGraphics();
		g2alphas.setColor(new Color(0xFFFFFFFF));
		g2alphas.fill(new Rectangle(0,0,width,height));
		
		// Create an image that will correspond to BandIndex's bands.
		return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}
	
	public MapAttr[] consumes(int inputNumber){
		return new MapAttr[]{ MapAttr.COLOR, MapAttr.GRAY };
	}
	
	public MapAttr produces(){
		return MapAttr.COLOR;
	}
	
	public String getStageName(){
		return getSettings().getStageName();
	}
	
	public int getInputCount(){
		return inputNames.length;
	}
	
	public String getInputName(int inputNumber){
		return inputNames[inputNumber];
	}
	
	public String[] getInputNames(){
		return (String[])inputNames.clone();
	}
	
	public Object clone() throws CloneNotSupportedException {
		RGBComposite stage = (RGBComposite)super.clone();
		return stage;
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
}
