package edu.asu.jmars.layer.shape2;

import java.io.Serializable;

import edu.asu.jmars.layer.util.features.FPath;

/**
 * Provides a serializable shape for the shape layer to put into and read from
 * sessions.
 * 
 * It currently converts to and from {@link FPath}, storing the closed flag and
 * the coordinates as an array of east-lon ocentric-lat float values.
 * 
 * This is a separate class precisely so allow switching the session path
 * storage to store other things for e.g. support of multi-part shapes.
 */
public final class ShapePath implements Serializable {
	private static final long serialVersionUID = 7859835588616991463L;
	private boolean closed;
	private float[] coords;
	private double[] coordsDouble;
	public ShapePath(FPath path) {
		this.closed = path.getClosed();
		this.coordsDouble = path.getSpatialEast().getCoords(false);
	}
	public FPath getPath() {
		
		if (coords != null && coords.length > 0) {
			//coords could be set if there were old sessions files that used the float[] being loaded into 
			//the newer version of JMARS
			coordsDouble = new double[coords.length];
			for(int i=0; i<coords.length; i++) {
				coordsDouble[i] = coords[i];
			}
			coords = null;
		} 
		return new FPath(coordsDouble, false, FPath.SPATIAL_EAST, closed);
	}
}

