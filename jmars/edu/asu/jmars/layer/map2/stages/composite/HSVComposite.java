package edu.asu.jmars.layer.map2.stages.composite;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import edu.asu.jmars.layer.map2.AlphaCombinerOp;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.StageUtil;
import edu.asu.jmars.util.PolyArea;
import edu.asu.jmars.util.Util;

public class HSVComposite extends CompositeStage implements Cloneable, Serializable {
	private static final long serialVersionUID = 2L;
	
	public static final String[] inputNames = new String[] {"Hue", "Saturation", "Value"};
	transient private Extractor[] extractor;
	
	/** @deprecated No longer contaminating the stage with an LManager problem! */
	private int myCnt=0;
	
	public HSVComposite(HSVCompositeSettings settings) {
		super(settings);
		reset();
	}
	
	private void reset() {
		extractor = new Extractor[3];
	}
	
	public MapData process(int input, MapData inputData, Area changedArea) {
		MapData data = super.process(input, inputData, changedArea);
		BufferedImage output = data.getImage();
		extractor[input] = Extractor.create(inputData.getImage());
		
		if (output.getWidth() != inputData.getImage().getWidth()
		||  output.getHeight() != inputData.getImage().getHeight()) {
			throw new IllegalArgumentException("Invalid image sizes, huh?");
		}
		
		// narrow changed area down to the valid area
		changedArea.intersect(data.getValidArea());
		
		AffineTransform at = StageUtil.getExtentTransform(output.getWidth(), output.getHeight(), data.getRequest().getExtent());
		float[] alphas = new float[3];
		for (Rectangle2D region: new PolyArea(changedArea).getRectangles()) {
			region = at.createTransformedShape(region).getBounds2D();
			final int x1 = (int)region.getMinX();
			final int y1 = (int)region.getMinY();
			final int x2 = x1 + (int)Math.ceil(region.getWidth());
			final int y2 = y1 + (int)Math.ceil(region.getHeight());
			for (int i = x1; i < x2; i++) {
				for (int j = y1; j < y2; j++) {
					// convert HSB values to RGB for this output location
					// and always enable alpha on pixels we set here
					for(int k=0; k<3; k++)
						alphas[k] = extractor[k].getAlpha(i, j);

					int outAlpha = (int)AlphaCombinerOp.alphaCombine(alphas) * 255;
					output.setRGB(i, j, (outAlpha << 24) | 
							(Color.HSBtoRGB(
									extractor[0].getHue(i, j),
									extractor[1].getSat(i, j),
									extractor[2].getVal(i, j)) & 0x00FFFFFF)
						);
				}
			}
		}
		
		return data;
	}
	
	public BufferedImage makeBufferedImage(int width, int height) {
		reset();
		return Util.newBufferedImage(width, height);
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
		HSVComposite stage = (HSVComposite)super.clone();
		stage.reset();
		return stage;
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		reset();
	}
}

abstract class Extractor {
	protected final BufferedImage input;
	protected final float[] hsb = new float[3];
	public Extractor(BufferedImage input) {
		this.input = input;
	}
	public static Extractor create(BufferedImage bi) {
		int bands = bi.getColorModel().getNumColorComponents();
		int type = bi.getRaster().getTransferType();
		if (type == DataBuffer.TYPE_BYTE && bands == 1)
			return new GrayByteExtractor(bi);
		return new ColorExtractor(bi);
	}
	public abstract float getHue(int i, int j);
	public abstract float getSat(int i, int j);
	public abstract float getVal(int i, int j);
	public abstract float getAlpha(int i, int j);
}

final class GrayByteExtractor extends Extractor {
	final Raster raster;
	public GrayByteExtractor(BufferedImage input) {
		super(input);
		raster = input.getRaster();
	}
	public float getHue(int i, int j) {
		return (float)raster.getSample(i, j, 0) / 255.0f;
	}
	public float getSat(int i, int j) {
		return (float)raster.getSample(i, j, 0) / 255.0f;
	}
	public float getVal(int i, int j) {
		return (float)raster.getSample(i, j, 0) / 255.0f;
	}
	public float getAlpha(int i, int j) {
		if (raster.getNumBands() == 2)
			return raster.getSampleFloat(i, j, 1) / 255.0f;
		return 1.0f;
	}
}

final class ColorExtractor extends Extractor {
	private final ColorModel cm;
	private final Raster raster;
	private Object pixel;
	public ColorExtractor (BufferedImage input) {
		super(input);
		cm = input.getColorModel();
		raster = input.getRaster();
	}
	public float getHue(int i, int j) {
		pixel = raster.getDataElements(i, j, pixel);
		Color.RGBtoHSB(cm.getRed(pixel), cm.getGreen(pixel), cm.getBlue(pixel), hsb);
		return hsb[0];
	}
	public float getSat(int i, int j) {
		pixel = raster.getDataElements(i, j, pixel);
		Color.RGBtoHSB(cm.getRed(pixel), cm.getGreen(pixel), cm.getBlue(pixel), hsb);
		return hsb[1];
	}
	public float getVal(int i, int j) {
		pixel = raster.getDataElements(i, j, pixel);
		Color.RGBtoHSB(cm.getRed(pixel), cm.getGreen(pixel), cm.getBlue(pixel), hsb);
		return hsb[2];
	}
	public float getAlpha(int i, int j) {
		pixel = raster.getDataElements(i, j, pixel);
		return cm.getAlpha(pixel) / 255.0f;
	}
}

