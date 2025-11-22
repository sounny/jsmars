/**
 * gets information about the altitude of the scene.  This info is taken from the Numback layer.
 *
 * @author: James Winburn MSFF-ASU 11/04
 */
package edu.asu.jmars.layer.threed;

import java.awt.image.Raster;

import edu.asu.jmars.util.DebugLog;

public class Elevation {
	private static DebugLog log = DebugLog.instance();
	protected int row = 0;
	protected int col = 0;
	protected float[][] map = null;
	private double sigmaValue;
	private double meanZ;
	//private double medianZ; // JNN: added
	private double minZ;	// JNN: added
	private double maxZ;	// JNN: added
	private double standardDeviation; // JNN: added
	private float ignore[] = {Float.NaN};	

	/**
	 * build the elevation from a Raster.  This is used by the layer when NumBack layer
	 * data is used for altitude information.
	 */
	public Elevation(Raster data, double[] ignoreValues) {
		if (data == null) {
			log.aprintln("Hey! There's no Elevation data!");
			return;
		}

		if (ignoreValues != null 
				&& ignoreValues.length > 0) {
			ignore = new float[ignoreValues.length];		
			for (int i=0; i< ignoreValues.length; i++) {	
				if (ignoreValues[0] >= -Float.MAX_VALUE || ignoreValues[0] <= Float.MAX_VALUE) {
					ignore[i] = (float)ignoreValues[i];
				} else {
					ignore[i] = 0f; // TODO should we do this?
				}
			}
		} else {
			log.aprintln("There are no ignore value(s) for the elevation data.");
		}

		col = data.getWidth();
		row = data.getHeight();

		sigmaValue = 0.0;
		float[] buffer = data.getPixels(0, 0, col, row, (float[]) null);
		double sigmaSquared = 0f;

		map = null;
		map = new float[row][col];
		
		minZ = Float.MAX_VALUE;	
		maxZ = -Float.MAX_VALUE;	
		
		int validPixels = 0;
		for (int r = 0; r < row; r++) {
			for (int c = 0; c < col; c++) {
				float value = buffer[r * col + c];
				if (!this.isIgnoreValue(value)) {
				// since this is elevation data set all ignore values to the 
				// value of the elevation reference sphere, i.e. zero elevation
				// so we don't skew the mean Z value
					minZ = (value < minZ) ? value : minZ;	// JNN: added
					maxZ = (value > maxZ) ? value : maxZ;	// JNN: added
					sigmaValue += value;
					validPixels++;
					sigmaSquared += (value * value);
				}
				map[row - r - 1][c] = value;
			}
		}
		meanZ = sigmaValue / (double) validPixels;
		standardDeviation = Math.sqrt(sigmaSquared/validPixels - (meanZ * meanZ));
		
		// set ignore and NaN values to minZ 
		// TODO: for radius data this needs ignore values to be set to radial mean
		for (int r = 0; r < row; r++) {
			for (int c = 0; c < col; c++) {
				float value = map[row - r - 1][c];
				if (isIgnoreValue(value)) {
					map[row - r - 1][c] = (float)minZ;
				}
			}
		}
	}

	/**
	 * returns the altitude of the scene as a 2D float array.
	 */
	public float[][] getPixelArrayFloat() {
		return map;
	}

	/**
	 * returns the width of the scene.
	 */
	public int getWidth() {
		return col;
	}

	/**
	 * returns the height of the scene.
	 */
	public int getHeight() {
		return row;
	}

	/** 
	 * returns the average altitude of the scene.
	 */
	public float getMean() {
		float meanz = (float) meanZ;
		return (Float.isNaN(meanz)) ? 0.0f : meanz;
	}

	public float[] getIgnore() {
		return ignore;
	}

	public void setIgnore(float[] ignore) {
		this.ignore = ignore;
	}
	
	private boolean isIgnoreValue(float value) {
		if (Float.isNaN(value)) { // TODO is this really what should happen here?
			return true;
		}
		for (int i=0; i<ignore.length; i++) {
			if (Float.compare(value, ignore[i]) == 0) {
				return true;
			}
		}
		return false;
	}

	///* // JNN: added
	/**
	 * @return minimum z value
	 */
	public float getMinAltitude()
	{
		return (float) minZ;
	}
	
	/**
	 * @return maximum z value
	 */
	public float getMaxAltitude()
	{
		return (float) maxZ;
	}

	/**
	 * standard deviation is calculated from variance
	 * and should have been calculated from the constructor
	 * @return standard deviation of z values
	 */
	public double getStandardDeviation()
	{
		return (Double.isNaN(standardDeviation)) ? 0.0 : standardDeviation;
	}
	
	/**
	 *  TODO: probably want to delete this
	 * computes the variance
	 * @param isSample is false if we want the variance of a population
	 * @return [sigma(from i=0 to n)(x_i - mu)^2] / (n - 1) when isSample is true
	 * 		or [sigma(from i=0 to n)(x_i - mu)^2] / n when isSample is false
	 */
	public double getVariance(boolean isSample)
	{
		double sum = 0;
		int n = 0;
		for (int y = 0; y < row; y++)
		{
			for (int x = 0; x < col; x++)
			{
				float value = map[y][x];
				// only calculate variance for valid numeric data
				if (!isIgnoreValue(value)) {
					sum += ((value - meanZ) * (value - meanZ));
					n++;
				}
			}
		}
		
		if((n == 0) || (n == 1))
		{
			return -1; // undefined
		}
		
		if(isSample)
		{
			return (sum / (n - 1)); // sample
		}
		else
		{
			return (sum / n); // population
		}
	}
		
	/*// JNN: me playing around to find the best algorithm
	public float getBestExageration()
	{
		///float scaleOffset = (float) Config.get(Util.getProductBodyPrefix()+Config.CONFIG_THREED_SCALE_OFFSET, -0.002f);
		double radius = Config.get(Util.getProductBodyPrefix()+Config.CONFIG_MEAN_RADIUS, 3386.0f);
		
		// scaleOffset is inversely proportional to the range of values
		// i.e. the smaller the range of values, the greater the scaling
		// the larger the range of values, the smaller the scaling
		double range = (maxZ - minZ);
		
		// range shouldn't be zero, negative okay
		range = (range == 0) ? 0.0000000000001f : range;

		//double diffMax = (maxZ - meanZ);
		//double diffMin = (meanZ - minZ);
		
		// 1 percent of range
		double temp = range / 100;
		
		// scale is negative because of direction camera is looking
		// i.e. negative z is backwards towards user
		// positive z is forward deeper inside computer
		//float scaleOffset = (float) -( ((radius - meanZ)/(radius / 10.0f)) / range);
		float scaleOffset = (float) -(100.0f / range); // I like this: -1 / (1% of range)
		//float scaleOffset = (float) -(meanZ / range);
		
		System.out.println("planetary radius: " + radius + "km");
		System.out.println("mean: " + meanZ + "\tactual mean: " + (radius + meanZ));
		System.out.println("maximum: " + maxZ + "\t\tactual maximum: " + (radius + maxZ));
		System.out.println("minimum: " + minZ + "\t\tactual minimum: " + (radius + minZ));
		System.out.println("range: " + range + "\t\t1 Percent of Range: " + temp);
		
		System.out.println("\nmars.threed.scaleOffset for MOLA_128ppd_shad_ne: -0.002");
		System.out.println("calculated scale offset: " + scaleOffset);
		return scaleOffset;
		//return 1.0f; // if initial
	} //*/
		
	public void adjustValuesToMean(double distanceFromMean)
	{
		// distanceFromMean best at 10.0f IMHO
		adjustValues(meanZ, maxZ, minZ, distanceFromMean);
	}
	
	private void adjustValues(double zero, double max, double min, double distance)
	{
		// to prevent off chance of division by zero:
		double temp1 = ((zero - min) == 0) ?
					1.0f //0.0000000000001f
					: -distance / (zero - min);
		
		double temp2 = ((max - zero) == 0) ?
					1.0f //0.0000000000001f
					: -distance / (max - zero);
		// negative due to direction camera is looking
		// i.e. negative z is backwards towards user
		// positive z is forward deeper inside computer
		
		for (int y = 0; y < row; y++)
		{
			for (int x = 0; x < col; x++)
			{
				if(map[y][x] == zero)
				{
					map[y][x] = 0;
				}
				else
				{
					map[y][x] = (map[y][x] < zero) ?
							(float)(map[y][x]*temp1)
							: (float)(map[y][x]*temp2);
				}
			}
		}
	}
	
	protected float[][] getUpperSide(float exaggeration) {
		float[][] ret = new float[map[0].length][3];
		int j=0;
		for(int i=map[0].length-1; i>=0; i--) {
			ret[j++] = new float[]{i, 0f, exaggeration * map[0][i]};
		}
		return ret;
	}
	
	protected float[][] getLowerSide(float exaggeration) {
		float[][] ret = new float[map[row-1].length][3];
		int j=0;
		for(int i=0; i<map[row-1].length; i++) {
			ret[j++] = new float[]{i, row-1, exaggeration * map[row-1][i]};
		}
		return ret;
	}
	
	protected float[][] getLeftSide(float exaggeration) {
		float[][] ret = new float[map.length][3];
		int j=0;
		for (int i=0; i<map.length; i++) {
			ret[j++] = new float[]{0, i, exaggeration * map[i][0]};
		}
		return ret;
	}

	protected float[][] getRightSide(float exaggeration) {
		float[][] ret = new float[map.length][3];
		int j=0;
		for (int i=map.length-1; i>=0; i--) {
			ret[j++] = new float[]{map[i].length-1, i, exaggeration * map[i][map[i].length-1]};
		}
		return ret;
	}
}
