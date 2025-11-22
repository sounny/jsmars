package edu.asu.jmars.ui.image.factory;

import java.awt.Image;
import java.awt.image.BufferedImage;

public class ImageFactory {	
	
	public static Image createImage(ImageDescriptor descriptor) {		  
		return convertFromSVG(descriptor);	
	}	
	
	private static BufferedImage convertFromSVG(ImageDescriptor descriptor) {
		return SvgConverter.valueOf(descriptor.getConversionFormat())
			  .convert(descriptor);
	}
}
