package edu.asu.jmars.swing;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.Map;

/**
 * In Java 1.4 rotated/scaled fonts don't tend to come out as desired.
 * The Font.deriveFont(AffineTransform) is broken. This utility class
 * contains methods that provide a workaround to this problem. Instead
 * of rendering derived fonts, we render the base font (i.e. font 
 * without any transforms). The rendered font is obtained as an
 * outline Shape. This Shape is filled to produce the draw the input
 * text string on the Graphics2D. 
 * 
 * @author saadat
 *
 */
public class FontUtil {
	public FontUtil() {}
	
    public static Font getFontWithoutTransform(Font font){
     	Map attr = font.getAttributes();
     	attr.remove(TextAttribute.TRANSFORM);
     	return Font.getFont(attr);
     }

     public static void drawStringAsShape(Graphics2D g2, String str, float x, float y){
    	 Font f = g2.getFont();
    	 Font baseFont = getFontWithoutTransform(f);
    	 AffineTransform baseFontTransform = f.getTransform();
    	 
    	 drawStringAsShape(g2, baseFont, baseFontTransform, str, x, y);
     }
     
     public static void drawStringAsShape(
    		 Graphics2D g2,
    		 Font untransformedFont,
    		 AffineTransform fontTransform,
    		 String str,
    		 float x,
    		 float y
     ){
    	 // TextLayout does not like Zero Length Strings.
    	 if (str.length() == 0)
    		 return;
    	 
    	 TextLayout tl = new TextLayout(str, untransformedFont, g2.getFontRenderContext());
    	 AffineTransform t = AffineTransform.getTranslateInstance(x,y);
    	 t.concatenate(fontTransform);
    	 GeneralPath gp = new GeneralPath(tl.getOutline(t));
    	 g2.setStroke(thinnestStroke);
    	 /* save hints */
    	 Object antiAliasingHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    	 Object fracMetricsHint = g2.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS);
    	 /* set our hints */
    	 g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    	 g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    	 /* draw */
    	 g2.fill(gp);
    	 /* restore hints */
    	 g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAliasingHint);
    	 g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fracMetricsHint);
     }
     
     static Stroke thinnestStroke = new BasicStroke(0);
}
