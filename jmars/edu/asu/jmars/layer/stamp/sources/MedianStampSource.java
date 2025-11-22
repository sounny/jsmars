package edu.asu.jmars.layer.stamp.sources;

import java.io.Serializable;
import java.util.ArrayList;

import edu.asu.jmars.layer.stamp.StampImage;
import edu.asu.jmars.layer.stamp.StampLayerSettings;
import edu.emory.mathcs.backport.java.util.Collections;

public final class MedianStampSource extends NumericStampSource implements Serializable {
    private static final long serialVersionUID = -1840073561231935949L;
		
	public MedianStampSource(StampLayerSettings settings) {
		super(settings);
		double ignoreVal[] = new double[1];
		ignoreVal[0]=StampImage.IGNORE_VALUE;
		 
		setIgnoreValue(ignoreVal);
		
		nameRoot = "median";
	}
						
	public double processValues(ArrayList<Double> values) {
		Collections.sort(values);
		
		return values.get(values.size()/2);
	}

}

