/**
 * 
 */
package edu.asu.jmars.layer.grid;

import java.awt.Color;
import java.awt.Font;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FONTS;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeLabel;
import edu.asu.jmars.util.Config;
import mdlaf.utils.MaterialColors;

/**
 * User selections from the LL Grid panel to be saved/loaded with sessions/layers.
 *
 * This is a serializable object so that it can be saved during a SAVE SESSION or
 * SAVE LAYER operation by the user.  It can then be loaded in a similar fashion
 * by LOAD operations.
 *
 */

public class GridParameters implements SerializedParameters {

    private static final long serialVersionUID = 1687855554601081823L;

    Integer majorColor         = 0;   // RGB value
    Double  majorSpacing       = 0.0;
    boolean majorMainVisible   = true;
    boolean majorPannerVisible = true;
    boolean major3DVisible 	   = true;
    Integer majorWidth         = 1;
    Integer majorLabelColor    = 0;   // RGB value
    MyLabelFont majorLabelFont;
    boolean majorMainLabelVisible = true;
    boolean majorPannerLabelVisible = false;

    Integer minorColor         = 0;   // RGB value
    Double  minorSpacing       = 0.0;
    boolean minorMainVisible   = true;
    boolean minorPannerVisible = true;
    boolean minor3DVisible	   = false;
    Integer minorWidth         = 1;
    Integer minorLabelColor    = 0;   // RGB value
    MyLabelFont minorLabelFont;
    boolean minorMainLabelVisible = false;
    boolean minorPannerLabelVisible = false;
    
    private static Font defaultFont = ThemeFont.getRegular().deriveFont(FONTS.ROBOTO.fontSize());
    private Font defaultFontMinor = ThemeFont.getRegular().deriveFont(FONTS.ROBOTO_CHART_SMALL.fontSize());
    
    public GridParameters() {
       majorColor         = new Integer(new Color(Config.get("gridcolor",  0)).getRGB());
       majorSpacing       = new Double(10.0);
       majorMainVisible   = new Boolean(true);
       majorPannerVisible = new Boolean(true);
       major3DVisible	  = new Boolean(true);
       majorWidth         = new Integer(1);
       majorLabelColor    = new Integer(new Color(Config.get("gridlabelcolor",  0)).getRGB());
       majorLabelFont = new MyLabelFont();
       majorMainLabelVisible = true;
       majorPannerLabelVisible = false;

       minorColor         = new Integer(Color.gray.getRGB());
       minorSpacing       = new Double(2.0);
       minorMainVisible   = new Boolean(false);
       minorPannerVisible = new Boolean(false);
       minor3DVisible	  = new Boolean(false);
       minorWidth         = new Integer(1);
       minorLabelColor    = new Integer(Color.gray.getRGB());
       minorLabelFont = new MyLabelFont(defaultFontMinor, MaterialColors.COSMO_MEDIUM_GRAY, null);
       minorMainLabelVisible = false;
       minorPannerLabelVisible = false;
    }

    public synchronized Integer getMajorLabelColor() {
   		if (majorLabelColor==null) {
   			majorLabelColor=0;
   		}
    	return majorLabelColor;
    }
    
    public synchronized Integer getMinorLabelColor() {
   		if (minorLabelColor==null) {
   			minorLabelColor=Color.gray.getRGB();
   		}
    	return minorLabelColor;
    }
    
    
    
    static class MyLabelFont implements SerializedParameters {
 
		private static final long serialVersionUID = 7471773289560321224L;
		private Color fontOutlineColor;
    	private Color fontFillColor;
    	private Font labelFont;   
    	
    	public MyLabelFont() {
    		this.labelFont =  ThemeFont.getRegular();
    		this.fontFillColor = ((ThemeLabel)GUITheme.get("label")).getForeground();
    		this.fontOutlineColor = null;
    	}

		public MyLabelFont(Font myfont, Color labeltextcolor, Color outlinecolor) {
			this.labelFont =  myfont;
			this.fontFillColor = labeltextcolor;	
			this.fontOutlineColor = outlinecolor;
 		}

		public synchronized Color getFontOutlineColor() {
			return fontOutlineColor;
		}

		public synchronized void withFontOutlineColor(Color fontOutlineColor) {
			this.fontOutlineColor = fontOutlineColor;
		}

		public synchronized Color getFontFillColor() {
			return this.fontFillColor;
		}

		public synchronized void withFontFillColor(Color fontFillColor) {
			this.fontFillColor = fontFillColor;
		}

		public synchronized Font getLabelFont() {
			if (this.labelFont == null)
			{
				this.labelFont = defaultFont;
			}
			return this.labelFont;
		}

		public synchronized void withLabelFont(Font labelFont) {
			this.labelFont = labelFont;
		}
    	
    }

}
