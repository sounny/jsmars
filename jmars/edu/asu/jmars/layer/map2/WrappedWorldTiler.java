package edu.asu.jmars.layer.map2;

import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts between wrapped world rectangles and grid positions in a tiled
 * coordinate system where the tiles at each ppd are the given height and
 * width.
 */
public class WrappedWorldTiler {
	private final int pixelWidth, pixelHeight;
	/**
	 * @param pixelWidth The width of each tile in pixels
	 * @param pixelHeight The height of each tile in pixels
	 */
	public WrappedWorldTiler(int pixelWidth, int pixelHeight) {
		this.pixelWidth = pixelWidth;
		this.pixelHeight = pixelHeight;
	}
	public int getPixelWidth() {
		return pixelWidth;
	}
	public int getPixelHeight() {
		return pixelHeight;
	}
	/**
	 * Computes tile [x,y] positions that cover the given extent.
	 * @param extent The extent in wrapped world coordinates (x axis strictly
	 * between 0 and 360 inclusive, y axis strictly between -90 and 90)
	 * @param ppd The pixels per degree (must be power of 2.)
	 * @return The list of tile points that covers the given rectangle at
	 * the given ppd scale.
	 */
	public List<Point> getTiles(Rectangle2D extent, int ppd) {
		double xstep = (double)pixelWidth / ppd;
		double ystep = (double)pixelHeight / ppd;
		double pixelWidth = 1.0 / ppd;
		
		double x = extent.getX();
		double y = extent.getY();
		double width = extent.getWidth();
		double height = extent.getHeight();
		
		// get tile range of this wrapped piece
		int xtileStart = (int) Math.floor(x / xstep); 
		int ytileStart = (int) Math.floor((y + 90.0) / ystep);
		
		// Subtract the width of 1 pixel from the calculation to avoid
		// getting an extra column of tiles when the width is the same as the xstep
		int xtileEnd = (int) ((x + width - pixelWidth) / xstep);			
		int ytileEnd = (int) ((y + 90.0 + height-pixelWidth) / ystep);
		
		// Add the tiles
		List<Point> tiles = new ArrayList<Point>();
		for (int xtile = xtileStart; xtile <= xtileEnd; xtile ++) {
			for (int ytile = ytileStart; ytile <= ytileEnd; ytile ++) {
				tiles.add(new Point(xtile,ytile));
			}
		}
		
		return tiles;
	}
	
	/** Returns the wrapped world extent of the given tile position at the given ppd */
	public Rectangle2D getExtent(Point tile, int ppd) {
		double xstep = (double)pixelWidth / ppd;
		double ystep = (double)pixelHeight / ppd;
		double x = tile.x * xstep;
		double y = tile.y * ystep - 90;
		return new Rectangle2D.Double(x, y, xstep, ystep);
	}
}

