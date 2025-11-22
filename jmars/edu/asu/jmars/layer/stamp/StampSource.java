package edu.asu.jmars.layer.stamp;

import java.io.Serializable;

import edu.asu.jmars.layer.map2.MapSourceDefault;

/**
 * THIS IS AN OBSOLETE CLASS that remains only to allow old session files to restore successfully.
 * 
 * This class should be causing old versions to resolve into an instance of the StampSource contained
 * in the stamp.sources subpackage.
 */
public final class StampSource extends MapSourceDefault implements Serializable {
    private static final long serialVersionUID = -4275867253020220221L;
    
	private final StampLayerSettings settings=null;
	private long id = -1L; // Retained for de-serialization purposes
	private double maxPPD = -1.0; // Retained for de-serialization purposes
	private double[] ignore = null; // Retained for de-serialization purposes
	
	private Object readResolve() {		
		// RESOLVE INTO A NEW NUMERIC STAMP SOURCE, NOT AN OLD ONE
		edu.asu.jmars.layer.stamp.sources.StampSource out = new edu.asu.jmars.layer.stamp.sources.StampSource(settings);
		
		return out;		
	}
	
}

