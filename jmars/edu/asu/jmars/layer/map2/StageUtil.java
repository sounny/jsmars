package edu.asu.jmars.layer.map2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.text.NumberFormat;

public class StageUtil {
	/**
	 * Returns the raster mask obtained by drawing the given area on a raster with given
	 * width and height of the specified extent.
	 */
	public static Raster getMask(int width, int height, final Rectangle2D extent, final Area area){
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g2 = image.createGraphics();
		g2.transform(getExtentTransform(width, height, extent));
		g2.setColor(Color.WHITE);
		g2.fill(area);
		g2.dispose();
		
		return image.getRaster();
	}

	/**
	 * Returns a transform that maps from extent's coordinate space to screen (pixel) coordinates.
	 */
	// TODO: is this just the same as Util.image2world?
	public static AffineTransform getExtentTransform(int width, int height, final Rectangle2D extent){
		AffineTransform at = new AffineTransform();
		
		at.scale(1, -1);
		at.translate(0, -height);
		at.scale(width/extent.getWidth(), height/extent.getHeight());
		at.translate(-extent.getX(),-extent.getY());
		
		return at;
	}
	
	/**
	 * Returns the rectangle covered by the pixel at position (i,j) w.r.t. mapData.
	 * The input pixRect is filled with the output value and returned. If the input
	 * pixRect is null, a new rectangle object is allocated filled and returned.
	 */
	public static Rectangle2D getPixRect(final MapData mapData, int i, int j, Rectangle2D pixRect){
		if (pixRect == null)
			pixRect = new Rectangle2D.Double();
		
		Rectangle2D extent = mapData.getRequest().getExtent();
		double topLeftX = extent.getMinX();
		double topLeftY = extent.getMaxY();
		int w = mapData.getImage().getWidth();
		int h = mapData.getImage().getHeight();
		double xStep = extent.getWidth() / w;
		double yStep = -extent.getHeight() / h;
		
		pixRect.setFrameFromDiagonal(topLeftX+i*xStep, topLeftY+j*yStep, topLeftX+(i+1)*xStep, topLeftY+(j+1)*yStep);
		return pixRect;
	}

	/**
	 * Returns the pixel boundary of the specified subExtent in the given extent. Partial pixels
	 * are dealt with by expanding the boundary to include them.
	 */
	/*
	public static Rectangle getSubExtentPixelBounds(int width, int height, final Rectangle2D extent, final Rectangle2D subExtent){
		Rectangle pixelBounds = new Rectangle();
		
		AffineTransform at = getExtentTransform(width, height, extent);
		Point2D p1 = at.transform(new Point2D.Double(subExtent.getMinX(), subExtent.getMinY()), null);
		Point2D p2 = at.transform(new Point2D.Double(subExtent.getMaxX(), subExtent.getMaxY()), null);
		pixelBounds.setFrameFromDiagonal(Math.floor(p1.getX()), Math.ceil(p1.getY()), Math.ceil(p2.getX()), Math.floor(p2.getY()));
		
		return pixelBounds;
	}
	*/
	
	public static StringBuffer dumpMask(Raster r){
		NumberFormat nf = NumberFormat.getIntegerInstance();
		nf.setMaximumIntegerDigits(1);
		StringBuffer sbuf = new StringBuffer();
		int w = r.getWidth();
		int h = r.getHeight();
		int b = r.getNumBands();
		
		for(int k=0; k<b; k++){
			for(int j=0; j<h; j++){
				for(int i=0; i<w; i++){
					sbuf.append(nf.format(r.getSample(i, j, k)/255));
				}
				sbuf.append("\n");
			}
			sbuf.append("\n");
		}
		
		return sbuf;
	}
}
