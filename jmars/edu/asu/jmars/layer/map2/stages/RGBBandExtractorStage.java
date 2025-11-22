package edu.asu.jmars.layer.map2.stages;

import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Arrays;

import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;

public class RGBBandExtractorStage extends BandExtractorStage implements Cloneable, Serializable {
	private static final long serialVersionUID = -6723372818363997617L;
	
	public RGBBandExtractorStage(RGBBandExtractorStageSettings settings){
		super(settings);
	}
	
	public MapData process(int inputNumber, MapData mapData, Area changedArea){
		RGBBandExtractorStageSettings settings = (RGBBandExtractorStageSettings)getSettings();
		
		String band = settings.getSelectedBand();
		int index = Arrays.asList(settings.getBands()).indexOf(band);
		if (index < 0)
			throw new IllegalArgumentException("Invalid band \""+band+"\" encountered.");
		
		int shift = (2-index)*8;
		BufferedImage image = mapData.getImage();
		int w = image.getWidth();
		int h = image.getHeight();
		BufferedImage outImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
		for(int j=0; j<h; j++){
			for(int i=0; i<w; i++){
				int rgb = image.getRGB(i, j);
				//int alpha = rgb & 0xff000000;
				rgb = (rgb >> shift) & 0xff;
				//rgb = alpha | rgb << 16 | rgb << 8 | rgb;
				//outImage.setRGB(i, j, rgb);
				outImage.getRaster().setSample(i, j, 0, rgb);
			}
		}
		
		return mapData.getDeepCopyShell(outImage, null);
	}
	
	public boolean canTake(int inputNumber, MapAttr mapAttr){
		if (mapAttr.isColor() || mapAttr.isGray())
			return true;
		
		return false;
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
