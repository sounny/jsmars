package edu.asu.jmars.swing;

import java.awt.Color;
import java.io.Serializable;

public abstract class ColorInterp implements Cloneable, Serializable
 {
	private final String title;
	private final String desc;
	private final String keyword;
	private final boolean identity;

	public ColorInterp(String title,
					   String keyword,
					   String desc)
	 {
		this(title, keyword, desc, true);
	 }

	public ColorInterp(String title,
					   String keyword,
					   String desc,
					   boolean identity)
	 {
		this.title = title;
		this.keyword = keyword;
		this.desc = desc;
		this.identity = identity;
	 }

	public final String getTitle()
	 {
		return  title;
	 }

	public final String getDesc()
	 {
		return  desc;
	 }

	public final String getKeyword()
	 {
		return  keyword;
	 }

	public final boolean canBeIdentity()
	 {
		return  identity;
	 }

	public abstract Color mixColor(Color c0, Color c1, float mix1);

	public final Color[] createColorMap(int[] values,
										Color[] colors)
	 {
		Color[] colorMap = new Color[256];

		int idx1 = 0;
		Color col0 = colors[0];
		Color col1 = colors[0];
		int val0 = values[0];
		int val1 = values[0];
		for(int i=0; i<256; i++)
		 {
			if(idx1 < values.length)
				if(values[idx1] == i)
				 {
					col0 = col1;
					val0 = val1;
					++idx1;
					if(idx1 < values.length)
					 {
						col1 = colors[idx1];
						val1 = values[idx1];
					 }
				 }
			if(val0 == val1)
				colorMap[i] = col0;
			else
				colorMap[i] = mixColor(col0,
									   col1,
									   (i-val0) / (float) (val1-val0)
					);
		 }
		return  colorMap;
	 }

	public static ColorInterp forKeyword(String keyword)
	 {
		for(int i=0; i<ALL.length; i++)
			if(ALL[i].keyword.equalsIgnoreCase(keyword))
				return  ALL[i];
		return  null;
	 }

	////////////////////////////////////////////////
	/////////////// USEFUL INSTANCES ///////////////
	////////////////////////////////////////////////

	////// HUE INTERPOLATION ////////////////////////////////////////////

	public static abstract class HueInterp extends ColorInterp
	 {
		public HueInterp(String title, String keyword, String desc)
		 {
			super(title, keyword, desc);
		 }

		public abstract float mixHue(float h0, float h1,
									 float mix0, float mix1);

		public final Color mixColor(Color c0, Color c1, float mix1)
		 {
			// Convenience: mix1 is the amount of c1, mix0 is the amount of c0
			float mix0 = 1 - mix1;

			// Get the HSB values of each color
			float hsb0[] = Color.RGBtoHSB(c0.getRed(),
										  c0.getGreen(),
										  c0.getBlue(),
										  null);
			float hsb1[] = Color.RGBtoHSB(c1.getRed(),
										  c1.getGreen(),
										  c1.getBlue(),
										  null);

			// If we have a grayscale, its hue is actually
			// meaningless... so to prevent its "random" value from
			// causing trouble, we fix the grayscale's hue to the color's
			// hue for smooth fading.
			Color result;
			
			if(hsb0[1] == 0){
				result = Color.getHSBColor(hsb1[0],
										  mix0*hsb0[1] + mix1*hsb1[1],
										  mix0*hsb0[2] + mix1*hsb1[2]);
			}
			else if(hsb1[1] == 0.0){
				result = Color.getHSBColor(hsb0[0],
										  mix0*hsb0[1] + mix1*hsb1[1],
										  mix0*hsb0[2] + mix1*hsb1[2]);
			}
			else{
				result =  Color.getHSBColor(mixHue(hsb0[0], hsb1[0], mix0, mix1),
										  mix0*hsb0[1] + mix1*hsb1[1],
										  mix0*hsb0[2] + mix1*hsb1[2]);
			}
			//Don't forget to take the alpha into consideration
			int alpha = (int) Math.round(c0.getAlpha()*mix0 + c1.getAlpha()*mix1);
			
			return new Color(result.getRed(), result.getGreen(), result.getBlue(), alpha);
		 }
	 }


	public static final ColorInterp LIN_HSB_SHORT =
	new HueInterp("Linear HSB, shortest hue path",
				  "lin_hsb_short",
				  null)
	 {
		private static final long serialVersionUID = -67026687977845992L;

		public final float mixHue(float h0, float h1,
								  float mix0, float mix1)
		 {
			float h = mix0*h0 + mix1*h1;
			if(Math.abs(h0 - h1) > 0.5)
			 {
				// Fix for hues separated by > 180 degrees
				h = 1 - (Math.max(h0,h1)-Math.min(h0,h1));
				h *= (h1 > h0 ? mix0 : mix1);
				h += Math.max(h0,h1);
				if(h > 1.0)
					h -= 1.0;
			 }
			return  h;
		 }
	 }
	;

	public static final ColorInterp LIN_HSB_DIRECT =
	new HueInterp("Linear HSB, direct hue path",
				  "lin_hsb_direct",
				  null)
	 {
		private static final long serialVersionUID = 8331322330812343372L;

		public final float mixHue(float h0, float h1,
								  float mix0, float mix1)
		 {
			return  mix0*h0 + mix1*h1;
		 }
	 }
	;

	public static final ColorInterp LIN_HSB_INCR =
	new HueInterp("Linear HSB, increasing hue",
				  "lin_hsb_incr",
				  null)
	 {
		private static final long serialVersionUID = -6614702138673606503L;

		public final float mixHue(float h0, float h1,
								  float mix0, float mix1)
		 {
			float distance = (1 + h1 - h0) % 1;
			if(distance == 0)
				distance = 1;
			float h = 1 + h0 + mix1 * distance;
			h %= 1;
			return  h;
		 }
	 }
	;

	public static final ColorInterp LIN_HSB_DECR =
	new HueInterp("Linear HSB, decreasing hue",
				  "lin_hsb_decr",
				  null)
	 {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2769760886442885799L;

		public final float mixHue(float h0, float h1,
								  float mix0, float mix1)
		 {
			float distance = (1 + h0 - h1) % 1;
			if(distance == 0)
				distance = 1;
			float h = 1 + h0 - mix1 * distance;
			h %= 1;
			return  h;
		 }
	 }
	;

	////// RGB INTERPOLATION ////////////////////////////////////////////

	public static final ColorInterp LIN_RGB =
	new ColorInterp("Linear RGB",
					"lin_rgb",
					null)
	 {
		private static final long serialVersionUID = 8339637876130448029L;

		public final Color mixColor(Color c0, Color c1, float mix1)
		 {
			// Convenience: mix1 is the amount of c1, mix0 is the amount of c0
			double mix0 = 1 - mix1;

			int r = (int) Math.round(mix0*c0.getRed()   + mix1*c1.getRed()  );
			int g = (int) Math.round(mix0*c0.getGreen() + mix1*c1.getGreen());
			int b = (int) Math.round(mix0*c0.getBlue()  + mix1*c1.getBlue() );

			int a = (int) Math.round(mix0*c0.getAlpha() + mix1*c1.getAlpha());
			
			return  new Color(r, g, b, a);
		 }
	 }
	;

	////// STAIR-STEPPING ///////////////////////////////////////////////

	public static final ColorInterp STEP_LEFT =
	new ColorInterp("None, stair-step from the left",
					"step_left",
					null,
					false)
	 {
		private static final long serialVersionUID = -9047837977276023426L;

		public final Color mixColor(Color c0, Color c1, float mix1)
		 {
			return  c0;
		 }
	 }
	;

	public static final ColorInterp STEP_CENTER =
	new ColorInterp("None, stair-step centered",
					"step_right",
					null,
					false)
	 {
		private static final long serialVersionUID = -9072608901456814639L;

		public final Color mixColor(Color c0, Color c1, float mix1)
		 {
			if(mix1 > 0.5)
				return  c1;
			else
				return  c0;
		 }
	 }
	;

	public static final ColorInterp STEP_RIGHT =
	new ColorInterp("None, stair-step from the right",
					"step_right",
					null,
					false)
	 {
		private static final long serialVersionUID = 1677645931392935396L;

		public final Color mixColor(Color c0, Color c1, float mix1)
		 {
			return  c1;
		 }
	 }
	;

	////// COLLECTIN OF ALL /////////////////////////////////////////////

	public static final ColorInterp[] ALL =
	{
		LIN_HSB_SHORT,
		LIN_HSB_DIRECT,
		LIN_HSB_INCR,
		LIN_HSB_DECR,
		LIN_RGB,
		STEP_LEFT,
		STEP_CENTER,
		STEP_RIGHT
	};
 }
