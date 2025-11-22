package edu.asu.jmars.layer.stamp.sources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

import edu.asu.jmars.layer.stamp.StampImage;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayerSettings;
import edu.asu.jmars.layer.stamp.StampServer;

public final class CountStampSource extends NumericStampSource implements Serializable {
    private static final long serialVersionUID = -1840073561231935949L;
	
	public CountStampSource(StampLayerSettings settings) {
		super(settings);
		double ignoreVal[] = new double[1];
		ignoreVal[0]=StampImage.IGNORE_VALUE;
		 
		setIgnoreValue(ignoreVal);
		
		nameRoot = "count";
	}
	
	/**
	 * CountStampSource always has units of number of entries
	 */
	public String getUnits() {
		return "observations";
	}
						
	public double processValues(ArrayList<Double> values) {
		return values.size();
	}

}

