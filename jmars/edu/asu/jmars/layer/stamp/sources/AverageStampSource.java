package edu.asu.jmars.layer.stamp.sources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.stamp.StampLayerSettings;
import edu.asu.jmars.layer.stamp.StampServer;

public final class AverageStampSource extends NumericStampSource implements MapSource, Serializable {
    private static final long serialVersionUID = -1840073561231935949L;
	
	public AverageStampSource(StampLayerSettings settings) {
		super(settings);
		
		nameRoot = "mean";
	}
				
	public double processValues(ArrayList<Double> values) {
		double avg = 0.0;
		for (Double val : values) {
			avg += val;
		}
		
		avg /= values.size();
		return avg;
	}
}