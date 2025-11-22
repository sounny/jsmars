package edu.asu.jmars.layer.util;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import edu.asu.jmars.layer.map2.MyVicarReaderWriter;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.VicarException;

/**
 * This class provides an {@link ElevationSource} through an image file, loaded
 * from a particular URL specified in the jmars.config file, and cached in
 * ~/jmars.
 */
public final class InMemElevationSource extends AbstractElevationSource {
	private final BufferedImage elevationImage;
	private final Raster elevationData;
	private final double base, multiplier;
	private final int ppd;
	private final int minVal, maxVal;
	
	public InMemElevationSource(String altitudeFileUrlString, double base, double scale)
			throws IOException, VicarException, URISyntaxException {
		File altFile;
		if (altitudeFileUrlString.toLowerCase().startsWith("file:")) {
			altFile = new File(new URI(altitudeFileUrlString));
		} else {
			altFile = Util.getCachedFile(altitudeFileUrlString, true);
		}
		elevationImage = MyVicarReaderWriter.read(altFile);
		elevationData = elevationImage.getRaster();
		this.base = base;
		this.multiplier = scale;
		ppd = elevationImage.getWidth()/360;
		minVal = min(elevationData);
		maxVal = max(elevationData);
	}
	
	private static int min(Raster r){
		final int w = r.getWidth();
		final int h = r.getHeight();
		
		int minVal = Integer.MAX_VALUE;
		for(int y = 0; y < h; y++)
			for(int x = 0; x < w; x++)
				minVal = Math.min(minVal, r.getSample(x, y, 0));
		
		return minVal;
	}
	
	private static int max(Raster r){
		final int w = r.getWidth();
		final int h = r.getHeight();
		
		int maxVal = Integer.MIN_VALUE;
		for(int y = 0; y < h; y++)
			for(int x = 0; x < w; x++)
				maxVal = Math.max(maxVal, r.getSample(x, y, 0));
		
		return maxVal;
	}
	
	public int getPPD(){
		return ppd;
	}
	
	public double getMinElevation(){
		return (minVal * multiplier) + base;
	}
	
	public double getMaxElevation(){
		return (maxVal * multiplier) + base;
	}
	
	public double getElevation(HVector v) {
		double lon = v.lonE() % 360;
		double lat = v.latC();
		int x = Math.min(elevationImage.getWidth() - 1, (int) Math.round(lon * ppd));
		int y = Math.min(elevationImage.getHeight() - 1, (int) Math.round((90-lat)* ppd));
		return elevationData.getSampleDouble(x, y, 0) * multiplier + base;
	}
}
