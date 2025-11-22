package edu.asu.jmars.layer.stamp.sources;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferFloat;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapAttrReceiver;
import edu.asu.jmars.layer.map2.MapRequest;
import edu.asu.jmars.layer.map2.NonRetryableException;
import edu.asu.jmars.layer.map2.RetryableException;
import edu.asu.jmars.layer.stamp.FilledStamp;
import edu.asu.jmars.layer.stamp.FilledStampImageType;
import edu.asu.jmars.layer.stamp.RenderProgress;
import edu.asu.jmars.layer.stamp.StampImage;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayerSettings;
import edu.asu.jmars.layer.stamp.StampServer;
import edu.asu.jmars.layer.stamp.StampShape;
import edu.asu.jmars.util.Util;
import edu.emory.mathcs.backport.java.util.Collections;
import edu.emory.mathcs.backport.java.util.concurrent.Semaphore;

public final class StdDevStampSource extends NumericStampSource implements Serializable {
    private static final long serialVersionUID = -1840073561231935949L;
    
	public StdDevStampSource(StampLayerSettings settings) {
		super(settings);
		double ignoreVal[] = new double[1];
		ignoreVal[0]=StampImage.IGNORE_VALUE;
		 
		setIgnoreValue(ignoreVal);
		nameRoot = "stddev";
	}
						
	public double processValues(ArrayList<Double> values) {
		double avg = 0.0;
		for (Double val : values) {
			avg += val;
		}
		
		avg /= values.size();

		double topSum = 0;

		for (Double val : values) {
			topSum += Math.pow(val-avg, 2);
		}

		double stdDev = Math.sqrt(topSum / values.size());
				
		return stdDev;
	}
}

