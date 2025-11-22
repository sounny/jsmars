package edu.asu.jmars.layer.stamp;

import edu.asu.jmars.layer.stamp.sources.StampSource;

/**
 * THIS IS AN OBSOLETE CLASS that remains only to allow old session files to restore successfully.
 * 
 * This class should be causing old versions to resolve into an instance of the NumericStampSource contained
 * in the stamp.sources subpackage.
 */
public final class NumericStampSource  {
    private static final long serialVersionUID = -1840073561231935949L;

	private final StampLayerSettings settings=null;
	private long id = -1L; // Retained for de-serialization purposes
	private double maxPPD = -1.0; // Retained for de-serialization purposes
	private double[] ignore = null; // Retained for de-serialization purposes
	
	private Object readResolve() {
		// RESOLVE INTO A NEW NUMERIC STAMP SOURCE, NOT AN OLD ONE		
		StampSource out = new edu.asu.jmars.layer.stamp.sources.NumericStampSource(settings);
		
		return out;		
	}
}

