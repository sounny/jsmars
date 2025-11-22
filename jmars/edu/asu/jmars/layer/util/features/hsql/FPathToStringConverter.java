package edu.asu.jmars.layer.util.features.hsql;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.util.Util;

public final class FPathToStringConverter implements HsqlColumnConverter {
	private static final boolean latsFirst = false;
	private final NumberFormat nf;
	
	public FPathToStringConverter(){
		nf = new DecimalFormat("###0.0##");
		nf.setGroupingUsed(false);
	}
	
	public Object hsqlToJava(Object in) {
		if (in == null)
			return null;
		
		if (in instanceof String){
			String[] pcs = Util.splitOnCommas((String)in);
			float[] coords = new float[pcs.length];
			for(int i=0; i<pcs.length; i++)
				coords[i] = Float.parseFloat(pcs[i]);
			
			boolean closed = (coords.length >= 4 && 
					coords[coords.length-2] == coords[0] && 
					coords[coords.length-1] == coords[1]);
			
			if (closed){ // get rid of the duplicate point that closes the path
				float[] tmp = new float[coords.length-2];
				System.arraycopy(coords, 0, tmp, 0, tmp.length);
				coords = tmp;
			}
			return new FPath(coords, latsFirst, FPath.SPATIAL_EAST, closed);
		}
		
		throw new RuntimeException("Invalid input type "+in.getClass().getCanonicalName());
	}

	public Object javaToHsql(Object in) {
		if (in == null)
			return null;

		if (in instanceof FPath){
			FPath fpath = ((FPath)in).convertTo(FPath.SPATIAL_EAST);
			double[] coords = fpath.getCoords(latsFirst);
			StringBuffer sbuf = new StringBuffer();
			for(int i=0; i<coords.length; i++){
				if (i > 0)
					sbuf.append(",");
				sbuf.append(nf.format(coords[i]));
			}
			if (fpath.getClosed() && coords.length >= 2)
				sbuf.append(","+coords[0]+","+coords[1]);
			return sbuf.toString();
		}
		
		throw new RuntimeException("Invalid input type "+in.getClass().getCanonicalName());
	}

}
