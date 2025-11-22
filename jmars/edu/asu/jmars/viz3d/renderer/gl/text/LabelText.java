package edu.asu.jmars.viz3d.renderer.gl.text;

public class LabelText extends BasicText {
	
	/**
	 * This class is intended to allow text labels to be applied to a specific location on the surface of a shape model.
	 */
	
	private float lat = Float.MAX_VALUE;
	private float lon = Float.MAX_VALUE;

	/**
	 * Constructor
	 * @param text a String representing the text to be drawn
	 * @param color color vector (R, G, B, A) with each color band and opacity independently normalized
	 * @param latitude the latitude of the location on the shape model this label maps onto
	 * @param longitude the longitude WEST-LEADING of the location on the shape model this label maps onto
	 */
	public LabelText(String text, float[] color, float latitude, float longitude) throws IllegalArgumentException {
		this.text = text;
		this.color = color;
		if (latitude < -90f || latitude > 90f) {
			throw new IllegalArgumentException("Invalid Latitude value for Labeltext: "+latitude);
		}
		if (longitude < -180f || longitude > 180f) {
			throw new IllegalArgumentException("Invalid Longitude value for Labeltext: "+longitude);
		}
		lat = latitude;
		lon = longitude;
	}
	
	/**
	 * @return the latitude
	 */
	public float getLat() {
		return lat;
	}

	/**
	 * @return the longitude
	 */
	public float getLon() {
		return lon;
	}



}
