package edu.asu.jmars.layer.map2;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BandedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RasterOp;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

public class BinRasterOp implements RasterOp {
	private final double base;
	private final double step;
	
	public BinRasterOp(double base, double step){
		this.base = base;
		this.step = step;
	}

	public WritableRaster createCompatibleDestRaster(Raster src) {
		SampleModel outSm = new BandedSampleModel(DataBuffer.TYPE_FLOAT, src.getWidth(), src.getHeight(), src.getNumBands());
		return WritableRaster.createWritableRaster(outSm, null);
	}

	public WritableRaster filter(Raster src, WritableRaster dest) {
		if (dest == null)
			dest = createCompatibleDestRaster(src);

		int w = src.getWidth();
		int h = src.getHeight();
		
		double[] dArray = null;
		
		for(int y=0; y<h; y++){
			for(int x=0; x<w; x++){
				dArray = src.getPixel(x, y, (double[])dArray);
				for(int z=0; z<dArray.length; z++)
					dArray[z] = (Math.rint((dArray[z] - base - (step/2.0))/step)) * step;
				
				dest.setPixel(x, y, dArray);
			}
		}
		
		return dest;
	}

	public Rectangle2D getBounds2D(Raster src) {
		return src.getBounds();
	}

	public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
		if (dstPt == null)
			dstPt = new Point2D.Double();
		
		dstPt.setLocation(srcPt);
		
		return srcPt;
	}

	public RenderingHints getRenderingHints() {
		return null;
	}

}
