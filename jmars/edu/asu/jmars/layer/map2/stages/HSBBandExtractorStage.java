package edu.asu.jmars.layer.map2.stages;

import java.awt.Color;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Arrays;

import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;


public class HSBBandExtractorStage extends BandExtractorStage implements Cloneable, Serializable {
	private static final long serialVersionUID = -6025236593295295143L;
	
	public HSBBandExtractorStage(HSBBandExtractorStageSettings settings){
		super(settings);
	}
	
	public MapData process(int inputNumber, MapData inputData, Area changedArea){
		HSBBandExtractorStageSettings settings = (HSBBandExtractorStageSettings)getSettings();
		String band = settings.getSelectedBand();
		int index = Arrays.asList(settings.getBands()).indexOf(band);
		if (index < 0)
			throw new IllegalArgumentException("Invalid band \""+band+"\" encountered.");
		
		BufferedImage image = inputData.getImage();
		int w = image.getWidth();
		int h = image.getHeight();
		BufferedImage outImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
		float[] hsbvals = new float[3];
		for(int j=0; j<h; j++){
			for(int i=0; i<w; i++){
				int rgb = image.getRGB(i, j);
				Color.RGBtoHSB((rgb & 0xff0000) >> 16, (rgb & 0xff00) >> 8, (rgb & 0xff), hsbvals);
				rgb = (int)(hsbvals[index]*255+0.5);
				outImage.setRGB(i, j, rgb << 16 | rgb << 8 | rgb);
			}
		}
		
		return inputData.getDeepCopyShell(outImage, null);
	}
	
	public MapAttr[] consumes(int inputNumber){
		return new MapAttr[]{ MapAttr.COLOR, MapAttr.GRAY };
	}
	
	public MapAttr produces(){
		return MapAttr.GRAY;
	}
	
	public String getStageName(){
		return getSettings().getStageName();
	}
}
