package edu.asu.jmars.ui.looknfeel;


import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Utilities {
	
	public static Image scaleImageToHeight(Image img, int height){
		BufferedImage bImg = (BufferedImage) img;
		double scale = (height*1.0)/(bImg.getHeight()*1.0);
		double dWidth = bImg.getWidth()*scale;
		int width = (int)Math.round(dWidth);
		return bImg.getScaledInstance(width, height, Image.SCALE_SMOOTH);
	}
	
	public static Image scaleImageToWidth(Image img, int width){
		BufferedImage bImg = (BufferedImage) img;
		double scale = (width*1.0)/(bImg.getWidth()*1.0);
		double dHeight = bImg.getHeight()*scale;
		int height = (int)Math.round(dHeight);
		return bImg.getScaledInstance(width, height, Image.SCALE_SMOOTH);
	}	
	
	public static String getColorAsBrowserHex(Color color) {
		 String rgb = Integer.toHexString(color.getRGB());		 
	     return "#" + rgb.substring(2, rgb.length());	     
	}

}
