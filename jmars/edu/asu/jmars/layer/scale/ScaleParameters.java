package edu.asu.jmars.layer.scale;

import java.awt.Color;
import java.awt.Font;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.ui.looknfeel.ThemeFont;

public class ScaleParameters implements SerializedParameters {
	public enum HorizontalAlignment {
		Left("Left"), Center("Center"), Right("Right");
		public final String label;
		HorizontalAlignment(String label) {
			this.label = label;
		}
	}
	public enum VerticalAlignment {
		Above("Above Scalebar"), Below("Below Scalebar"),;
		public final String label;
		VerticalAlignment(String label) {
			this.label = label;
			
		}
	}

	private static final long serialVersionUID = 1L;
	public int offsetX = 10;
	public int offsetY = -15;
	public boolean isMetric;
	public Color fontOutlineColor;
	public int fontOutlineColorint;
	public Color fontFillColor;
	public Color barColor;
	public Color tickColor;
	public int numberOfTicks;
	public int width;
	public Font labelFont;
	public HorizontalAlignment h_alignment;
	public VerticalAlignment v_alignment;	
	
	// added only for backwards compatibility for old saved layers. It is declared transient so that xstream ignores when deserializing
	transient public HorizontalAlignment alignment;  

	public ScaleParameters() {
		isMetric = true;
		fontOutlineColor = null;
    	fontFillColor = Color.black;
		barColor = Color.black;
		tickColor = Color.black;
		numberOfTicks = 0;
		width = 20;
		labelFont = ThemeFont.getBold().deriveFont(16f);
		h_alignment = HorizontalAlignment.Center;
		v_alignment=VerticalAlignment.Above;
	}
	
	private Object readResolve() {
		if (h_alignment == null) {
			h_alignment = HorizontalAlignment.Left;
		}
		if (v_alignment==null) {
			v_alignment=VerticalAlignment.Above;
		}
		return this;
	}
}
