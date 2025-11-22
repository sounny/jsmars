package edu.asu.jmars.swing;

import edu.asu.jmars.layer.map2.stages.ColorStretcherStageSettings;
import edu.asu.jmars.util.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class ColorScale extends JPanel
 {
	private static DebugLog log = DebugLog.instance();

	private EventListenerList listenerList = new EventListenerList();
	private ColorInterp interpolation = ColorInterp.LIN_HSB_SHORT;
	private int[] values;
	private Color[] colors;
	private float boundLo = 0;
	private float boundHi = 255;
	private double colorSettingsMax;
	
	public double getColorSettingsMax() {
		return colorSettingsMax;
	}

	public void setColorSettingsMax(double colorSettingsMax) {
		this.colorSettingsMax = colorSettingsMax;
	}

	private double colorSettingsMin;	

	
	
	public double getColorSettingsMin() {
		return colorSettingsMin;
	}

	public void setColorSettingsMin(double colorSettingsMin) {
		this.colorSettingsMin = colorSettingsMin;
	}

	public ColorScale(final MultiSlider slider, Color[] colors, ColorStretcherStageSettings settings) {
		this(slider.getValues(), colors, settings);

		slider.addChangeListener(
			new ChangeListener()
			 {
				public void stateChanged(ChangeEvent e)
				 {
					setColors(slider.getValues());
					repaint();
				 }
			 }
			);
	}
	
	public ColorScale(final MultiSlider slider, Color[] colors)
	 {
		this(slider, colors, null);
		
	 }

	public ColorScale(int[] values, Color[] colors)
	 {
		this(values,colors,null);
	 }
	
	public ColorScale(int[] values, Color[] colors, ColorStretcherStageSettings settings)
	 {
		setBorder(BorderFactory.createLineBorder(Color.yellow, 50));
		setColors(values, colors);
		setToolTipText("");
		
		if (settings != null) {
			colorSettingsMax = settings.getMaxValue();
			colorSettingsMin = settings.getMinValue();
		}
	 }
	
	public void addChangeListener(ChangeListener l)
	 {
		listenerList.add(ChangeListener.class, l);
	 }

	public void removeChangeListener(ChangeListener l)
	 {
		listenerList.remove(ChangeListener.class, l);
	 }

	private ChangeEvent changeEvent = new ChangeEvent(this);
	protected void fireChangeEvent()
	 {
		colorMap = null;
		Object[] list = listenerList.getListenerList();
		for(int i=list.length-2; i>=0; i-=2)
			( (ChangeListener) list[i+1] ).stateChanged(changeEvent);
	 }

	public void setInterpolation(ColorInterp interpolation)
	 {
		this.interpolation = interpolation;
		fireChangeEvent();
		repaint();
	 }

	public ColorInterp getInterpolation()
	 {
		return  interpolation;
	 }

	public boolean isIdentity()
	 {
		return  values.length == 2
			&&  values[0] == 0
			&&  values[1] == 255
			&&  interpolation.canBeIdentity()
			&&  colors[0].equals(Color.black)
			&&  colors[1].equals(Color.white);
	 }

	public int getColorCount()
	 {
		return  values.length;
	 }

	public int getColorVal(int n)
	 {
		return  values[n];
	 }

	public Color getColor(int n)
	 {
		return  colors[n];
	 }

	public void setColor(int n, int value)
	 {
		values[n] = value;
		fireChangeEvent();
	 }

	public void setColor(int n, Color color)
	 {
		colors[n] = color;
		fireChangeEvent();
	 }

	public void setColor(int n, int value, Color color)
	 {
		values[n] = value;
		colors[n] = color;
		fireChangeEvent();
	 }

	public int[] getColorVals()
	 {
		return  (int[]) values.clone();
	 }

	public Color[] getColors()
	 {
		return  (Color[]) colors.clone();
	 }

	public void setColors(int[] values,
						  Color[] colors,
						  ColorInterp interpolation)
	 {
		this.values = (int[]) values.clone();
		this.colors = (Color[]) colors.clone();
		this.interpolation = interpolation;
		fireChangeEvent();
	 }

	public void setColors(int[] values, Color[] colors)
	 {
		this.values = (int[]) values.clone();
		this.colors = (Color[]) colors.clone();
		fireChangeEvent();
	 }

	public void setColors(Color[] colors)
	 {
		this.values = (int[]) values.clone();
		this.colors = (Color[]) colors.clone();
		fireChangeEvent();
	 }

	public void setColors(int[] values)
	 {
		this.values = (int[]) values.clone();
		this.colors = (Color[]) colors.clone();
		fireChangeEvent();
	 }

	public Color locationToColor(int x)
	 {
		return  getColorMap()[Math.round(x * 255f / getWidth())];
	 }

	private Color[] colorMap = null;
	public Color[] getColorMap()
	 {
		if(colorMap == null)
			colorMap = interpolation.createColorMap(values, colors);

		return  colorMap;
	 }

	public String getToolTipText(MouseEvent e)
	 {
		int x = e.getPoint().x;

		Insets insets = getInsets();
		if(x < insets.left  ||  x >= getWidth() - insets.right)
			return  null;

		int index =
			Math.round(255f * (x - insets.left)
				/ (getWidth() - insets.right - insets.left));
		Color col = getColorMap()[index]; 
		double inc = (getColorSettingsMax() - getColorSettingsMin()) / 255.0 ;
		double value = (inc * index) + colorSettingsMin;
		return  "Value: " + NumberFormat.getInstance().format(value) + " / DN " + index + " / RGB #" + hex24(col.getRGB() & 0xFFFFFF);
		
	 }

	private String hex24(int val)
	 {
		String s = "00000" + Integer.toHexString(val).toUpperCase();
		return  s.substring(s.length()-6);
	 }

	public void paintComponent(Graphics g)
	 {
		super.paintComponent(g);

		Color[] map = getColorMap();

		Insets insets = getInsets();
		int x = insets.left;
		int y = insets.top;
		int w = getWidth() - insets.left - insets.right;
		int h = getHeight() - insets.top - insets.bottom;
		for(int i=0; i<w; i++)
		 {
			int index = Math.round(255f * i / w);
			g.setColor(map[index]);
			g.drawLine(x+i, y, x+i, y+h-1);
		 }
	 }


	/**
	 ** Sets the lower bound for the color mapping scale.
	 **/
	public void setBoundLo(double lo)
	 {
		boundLo = (float) lo;
		fireChangeEvent();
	 }

	/**
	 ** Sets the upper bound for the color mapping scale.
	 **/
	public void setBoundHi(double hi)
	 {
		boundHi = (float) hi;
		fireChangeEvent();
	 }

	/**
	 ** Retrieves the lower bound for the color mapping scale.
	 **/
	public double getBoundLo()
	 {
		return  boundLo;
	 }

	/**
	 ** Retrieves the upper bound for the color mapping scale.
	 **/
	public double getBoundHi()
	 {
		return  boundHi;
	 }

	/**
	 ** Given an input pixel value, returns its mapped color as an
	 ** object.
	 **/
	public Color mapToColor(byte pixel)
	 {
		return  mapToColor((float) pixel);
	 }

	/**
	 ** Given an input pixel value, returns its mapped color as an
	 ** object.
	 **/
	public Color mapToColor(short pixel)
	 {
		return  mapToColor((float) pixel);
	 }

	/**
	 ** Given an input pixel value, returns its mapped color as an
	 ** object.
	 **/
	public Color mapToColor(double pixel)
	 {
		return  mapToColor((float) pixel);
	 }

	/**
	 ** Given an input pixel value, returns its mapped color as an
	 ** integer value. (Bits 24-31 are alpha, 16-23 are red, 8-15 are
	 ** green, 0-7 are blue)
	 **/
	public int mapToARGB(byte pixel)
	 {
		return  mapToColor(pixel).getRGB();
	 }

	/**
	 ** Given an input pixel value, returns its mapped color as an
	 ** integer value. (Bits 24-31 are alpha, 16-23 are red, 8-15 are
	 ** green, 0-7 are blue)
	 **/
	public int mapToARGB(short pixel)
	 {
		return  mapToColor(pixel).getRGB();
	 }

	/**
	 ** Given an input pixel value, returns its mapped color as an
	 ** integer value. (Bits 24-31 are alpha, 16-23 are red, 8-15 are
	 ** green, 0-7 are blue)
	 **/
	public int mapToARGB(float pixel)
	 {
		return  mapToColor(pixel).getRGB();
	 }

	/**
	 ** Given an input pixel value, returns its mapped color as an
	 ** integer value. (Bits 24-31 are alpha, 16-23 are red, 8-15 are
	 ** green, 0-7 are blue)
	 **/
	public int mapToARGB(double pixel)
	 {
		return  mapToColor(pixel).getRGB();
	 }

	/**
	 ** Given an input pixel value, returns its mapped color as an
	 ** object.
	 **/
	public Color mapToColor(float pixel)
	 {
		// Find the idx of the next-lowest slider to this pixel's value
		int val = (int) ( (pixel - boundLo) / (boundHi - boundLo) * 255 );
		int idx = Arrays.binarySearch(values, val);
		if(idx < 0) idx = -idx-2; // see binarySearch javadocs

		// Special cases for values that are outside the slider range
		if(idx == -1             ) return colors[0];
		if(idx == colors.length-1) return colors[idx];

		// Mix the colors
		float mix = (pixel - values[idx]) / (values[idx+1] - values[idx]);
		if(!Util.between(0, mix, 1))
			log.aprintln("STRANGENESS: mix = " + mix);
		return  interpolation.mixColor(colors[idx], colors[idx+1], mix);
	 }
 }
