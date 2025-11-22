package edu.asu.jmars.layer.map2.stages;

import java.awt.Color;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.RasterOp;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import edu.asu.jmars.layer.map2.AbstractStage;
import edu.asu.jmars.layer.map2.BinRasterOp;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;

public class ContourStage extends AbstractStage implements Cloneable, Serializable {
	public ContourStage(ContourStageSettings settings){
		super(settings);
	}
	
	public MapAttr[] consumes(int inputNumber) {
		if (inputNumber != 0)
			throw new IllegalArgumentException();
		
		return new MapAttr[]{ MapAttr.SINGLE_BAND };
	}

	public int getInputCount() {
		return 1;
	}

	public MapAttr produces() {
		return MapAttr.COLOR;
	}
	
	public ContourStageSettings getSettings(){
		return ((ContourStageSettings)super.getSettings());
	}
	
	public MapData process(int inputNumber, MapData data, Area changedArea) {
		if (inputNumber != 0)
			throw new IllegalArgumentException();
		
		double start = getSettings().getBase();
		double step = getSettings().getStep();
		Color    color = getSettings().getColor();
		int lineThickness = getSettings().getLineThickness();
		
		RasterOp binOp = new BinRasterOp(start, step);
		WritableRaster binnedRaster = binOp.filter(data.getImage().getRaster(), null);
		
		int w = binnedRaster.getWidth();
		int h = binnedRaster.getHeight();
		int b = binnedRaster.getNumBands();

		BufferedImage outImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		//WritableRaster outRaster = WritableRaster.createBandedRaster(DataBuffer.TYPE_FLOAT, w, h, b, null);
		
		double[] dArray = new double[9];
		boolean filled;
		
		for(int k=0; k<1; k++){
			for(int j=0; j<h; j++){
				for(int i=0; i<w; i++){
					int x1 = Math.max(0, i-1), x2 = Math.min(w-1, i+1);
					int y1 = Math.max(0, j-1), y2 = Math.min(h-1, j+1);
					int lw = x2-x1+1, lh = y2-y1+1;
					int lcloc = lw * (j-y1) + (i-x1);
					
					binnedRaster.getSamples(x1, y1, lw, lh, k, dArray);
					filled = false;
					for(int y=0; !filled && y<lh; y++){
						for(int x=0; !filled && x<lw; x++){
							if ((x+x1) == i && (y+y1) == j)
								continue;
							
							if (dArray[lcloc] < dArray[lw*y+x])
								filled = true;
						}
					}
					
					if (filled) {
						switch(lineThickness) {
						case 1:
							outImage.setRGB(i, j, color.getRGB());
							break;
						case 2:
							outImage.setRGB(i, j, color.getRGB());
							if (i<w-1) 
								outImage.setRGB(i+1, j, color.getRGB());
							if (j<h-1) 
								outImage.setRGB(i, j+1, color.getRGB());
							
							break;	
						case 3:
							outImage.setRGB(i, j, color.getRGB());
							if (i<w-1) 
								outImage.setRGB(i+1, j, color.getRGB());
							if (i>0)
								outImage.setRGB(i-1, j, color.getRGB());
							if (j<h-1) 
								outImage.setRGB(i, j+1, color.getRGB());
							if (j>0)
								outImage.setRGB(i, j-1, color.getRGB());							
							break;
						default:
							outImage.setRGB(i, j, color.getRGB());
							break;
						}
					}
						//outRaster.setSample(i, j, k, dArray[lcloc]);
				}
			}
		}
		
		//ColorModel outCM = new EmptyColorModel();
		//BufferedImage outImage = new BufferedImage(outCM, outRaster, outCM.isAlphaPremultiplied(), null);
		
		changedArea.reset();
		changedArea.add(new Area(data.getRequest().getExtent()));
		
		return data.getDeepCopyShell(outImage, null);
	}

	public String getStageName(){
		return getSettings().getStageName();
	}
	
	public Object clone() throws CloneNotSupportedException {
		ContourStage stage = (ContourStage)super.clone();
		return stage;
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}

}
