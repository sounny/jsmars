package edu.asu.jmars.layer.north;

import java.awt.Color;

import edu.asu.jmars.layer.SerializedParameters;

public class NorthSettings implements SerializedParameters{
	private static final long serialVersionUID = 1L;
	
	protected boolean fillLeft = false;
	protected boolean fillRight = true;
	protected int arrowSize = 1;
	protected float outlineSize = 2f;
	protected Color arrowColor = Color.BLACK;
	protected Color textColor = Color.BLACK;
	protected int fontSize = 16;
	protected boolean showText = true;
}
