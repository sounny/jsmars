package edu.asu.jmars.ui.image.factory;

import java.awt.Color;
import java.util.Optional;

public interface ImageDescriptor {	

	public Optional<Color> getDisplayColor();	
	
	public Optional<Color> getStrokeColor();
	
	public int getWidth();
	
	public int getHeight();
	
	public String getConversionFormat();
	
	public String getImageFilePath();
	
	public ImageDescriptor as(SvgConverter format);

	public ImageDescriptor withDisplayColor(Color imgDisplayColor);
	
	public ImageDescriptor withStrokeColor(Color imgStrokeColor);
	
	public ImageDescriptor withWidth(int width);
	
	public ImageDescriptor withHeight(int height);	

}
