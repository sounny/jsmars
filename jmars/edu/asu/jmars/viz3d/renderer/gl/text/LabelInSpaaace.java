package edu.asu.jmars.viz3d.renderer.gl.text;

import edu.asu.jmars.util.DebugLog;

public class LabelInSpaaace extends BasicText {
	
	private float X;
	private float Y;
	private float Z;
	private boolean isScalable = true;
	private boolean hasBeenScaled = false;
    private static DebugLog log = DebugLog.instance();
	
	public LabelInSpaaace(String text, float[] color, float xCoordinate, float yCoordinate, float zCoordinate, float scaleFactor) throws IllegalArgumentException {
		super();
		if (text == null) {
			throw new IllegalArgumentException("Invalid Text Label, cannot be null: "+text);
		}
		if (color == null) {
			throw new IllegalArgumentException("Text Label color is null");
		}
		if (color.length != 3) {
			throw new IllegalArgumentException("Text Label color is in invalid format: array length must be three elements");
		}
		this.text = text;
		this.color = color;
		X = xCoordinate;
		Y = yCoordinate;
		Z = zCoordinate;

	}
	
	public float[] getlocation() {
		return new float[]{X, Y, Z};
	}
	
	public void setLocation(float[] loc) throws IllegalArgumentException {
		if (loc == null) {
			throw new IllegalArgumentException("Label location is null");
		}
		if (loc.length != 3) {
			throw new IllegalArgumentException("Label is in invalid format: array length must be three elements");
		}
		
		X = loc[0];
		Y = loc[1];
		Z = loc[2];
		
		hasBeenScaled = false;
	}
	
	@Override
	public boolean isScalable() {
		return isScalable;
	}

	@Override
	public void scaleToShapeModel(boolean canScale) {
		isScalable = canScale;		
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#scaleByDivision(float)
	 */
	@Override
	public void scaleByDivision(float scalar) {
		if (Float.compare(scalar, 0f) == 0) {
			log.aprintln("Attempting to scale a GLRenderable by dividing by zero.");
			return;
		}
		if (hasBeenScaled) {
			//NOP
			return;
		}
		float scaleFactor = 1f / scalar;
		X *= scaleFactor;
		Y *= scaleFactor;
		Z *= scaleFactor;
		hasBeenScaled = true;
	}
	
	@Override
	public boolean isScaled() {
		return hasBeenScaled;
	}
}
