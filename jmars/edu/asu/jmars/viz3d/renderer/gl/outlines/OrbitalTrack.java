package edu.asu.jmars.viz3d.renderer.gl.outlines;

import edu.asu.jmars.viz3d.ThreeDManager;

public class OrbitalTrack extends OutLine {

	public OrbitalTrack(int idNumber, float[] points, float[] lineColor, int lineWidth, boolean closedLoop, boolean onBody) {
		super(idNumber, points, lineColor, lineWidth, closedLoop, onBody);
	}
	
	public OrbitalTrack(int idNumber, float[] points, float[] lineColor, int lineWidth, boolean closedLoop, short dashPattern, int multFactor) {	
		super(idNumber, points, lineColor, lineWidth, closedLoop, dashPattern, multFactor);
	}
	
	boolean processed;

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
    
    @Override
    public void dispose() {
//        ThreeDManager.getInstance().removeOrbitalTrack(); 
    }
}
