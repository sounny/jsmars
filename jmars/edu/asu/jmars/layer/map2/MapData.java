package edu.asu.jmars.layer.map2;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.PackedColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.PolyArea;
import edu.asu.jmars.util.Util;

/**
 * Holds the current data for one Pipeline of a MapChannel request.
 * 
 * The request properties are immutable.
 * 
 * The finishedArea, fuzzyArea, finished, and image properties are mutable.
 * The getDeepCopyShell() method copies all properties except image, which
 * is highly mutable. It is strongly suggested that either getImageCopy()
 * be used, and an operation work against the copy, or the calling code
 * synchronize on the result of getImage().
 */
public final class MapData {
	private static DebugLog log = DebugLog.instance();
	
	private final MapRequest request;
	private Area finishedArea;
	private Area fuzzyArea;
	/**
	 * Updates to the 'image' field should be done through
	 * {@link #setImage(BufferedImage, double[])} to ensure the null tester is
	 * also updated properly.
	 */
	private BufferedImage image;
	private double[] nullPixel;
	private boolean finished;
	private NullTest nullTester;
	
	public MapData(MapRequest request) {
		this(request, null, null);
	}
	
	private MapData(MapRequest request, BufferedImage image, double[] nullPixel) {
		this.request = request;
		setImage(image, nullPixel);
		finishedArea = new Area();
		fuzzyArea = new Area();
		finished = false;
	}
	
	private void setImage(BufferedImage image, double[] nullPixel) {
		this.image = image;
		this.nullPixel = nullPixel;
		if (image == null) {
			nullTester = null;
		} else if (nullPixel != null && (nullPixel.length == 1 ||nullPixel.length == image.getColorModel().getNumColorComponents())) {
			nullTester = new IgnoreTest(image, nullPixel);
		} else if (image.getAlphaRaster() != null) {
			nullTester = new AlphaTest(image.getAlphaRaster());
		} else {
			nullTester = new DefaultTest();
		}
	}
	
	public MapRequest getRequest() {
		return request;
	}
	
	public boolean isFinished() {		
		return finished;
	}
	
	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public Area getFinishedArea() {
		return finishedArea;
	}

	public Area getFuzzyArea() {
		return fuzzyArea;
	}
	
	/** Returns a new Area that contains both fuzzy and finished areas. */
	public Area getValidArea() {
		Area valid = new Area();
		valid.add(finishedArea);
		valid.add(fuzzyArea);
		return valid;
	}
	
	/** Returns the image used in this MapData object, without making a copy */
	public BufferedImage getImage() {
		return image;
	}
	
	/**
	 * @return a raster that shares the given image's databuffer but only
	 *         provides access to the color bands, complementing
	 *         {@link BufferedImage#getAlphaRaster()}.
	 */
	public static Raster getColorRaster(BufferedImage bi) {
		return getColorRaster(bi.getColorModel(), bi.getRaster());
	}
	
	public static Raster getColorRaster(ColorModel cm, Raster wr) {
		// use all bands but the alpha band (we assume alpha has the highest band index)
		int[] bandFilter = new int[cm.getNumColorComponents()];
		for (int i = 0; i < bandFilter.length; i++) {
			bandFilter[i] = i;
		}
		return wr.createChild(0, 0, wr.getWidth(), wr.getHeight(), 0, 0, bandFilter);
	}
	
	/**
	 * Returns a portion of this MapData's WritableRaster covering the given
	 * world extent. May throw all manner of exceptions if the image isn't
	 * present. If the given extent does not intersect this MapData at all, null
	 * is returned.
	 */
	public WritableRaster getRasterForWorld(Rectangle2D unwrappedWorld) {
		return getRasterForWorld(image.getRaster(), request.getExtent(), unwrappedWorld);
	}
	
	/**
	 * Returns a WritableRaster for the part of the input image (which covers
	 * inputExtent) that lies within outputExtent. If there is no intersection,
	 * this returns null. Any pixel touched by the intersection is in the returned
	 * Raster. The input image and input extent must have the same aspect ratio.
	 * The returned raster's origin is at (0,0), regardless of what part of the
	 * image it is cropped from.
	 */
	public static WritableRaster getRasterForWorld(WritableRaster raster, Rectangle2D inputExtent, Rectangle2D outputExtent) {
		Rectangle overlap = getRasterBoundsForWorld(raster, inputExtent, outputExtent);
		if (overlap.isEmpty())
			return null;
		
		return raster.createWritableChild(overlap.x, overlap.y, overlap.width, overlap.height, 0, 0, null);
	}
	
	/**
	 * Returns a Rectangle (in raster coordinates) for the part of the input image 
	 * (which covers inputExtent) that lies within outputExtent. If there is no intersection,
	 * this returns an empty Rectangle. Any pixel touched by the intersection is in the returned
	 * Raster. The input image and input extent must have the same aspect ratio.
	 */
	public static Rectangle getRasterBoundsForWorld(Raster raster, Rectangle2D inputExtent, Rectangle2D outputExtent){
		Rectangle2D overlap = new Rectangle2D.Double();
		Rectangle2D.intersect(inputExtent, outputExtent, overlap);
		if (overlap.isEmpty()) {
			return new Rectangle();
		} else {
			double xPPD = raster.getWidth() / inputExtent.getWidth();
			double yPPD = raster.getHeight() / inputExtent.getHeight();
			if (Math.abs(xPPD - yPPD) > .00001) {
				log.aprintln("Axes have unequal scale, "
					+ "image=" + raster.getWidth() + "," + raster.getHeight()
					+ " extent=" + inputExtent.getWidth() + "," + inputExtent.getHeight()
					+ " ppd="+xPPD+","+yPPD);
			}
			int width = (int)Math.ceil(overlap.getWidth() * xPPD);
			int height = (int)Math.ceil(overlap.getHeight() * yPPD);
			int x = (int)Math.floor((overlap.getMinX() - inputExtent.getMinX()) * xPPD);
			int y = (int)Math.floor((inputExtent.getMaxY() - overlap.getMaxY()) * yPPD);
			return new Rectangle(x, y, width, height);
		}
	}
	
	public Rectangle getRasterBoundsForWorld(Rectangle2D outputExtent){
		return getRasterBoundsForWorld(getImage().getRaster(), getRequest().getExtent(), outputExtent);
	}
	
	/**
	 * @param mapTile
	 *            The tile to insert into this MapData; the bounds should
	 *            intersect the bounds of this MapData's request, and
	 *            {@link MapTile#getFuzzyImage()} or {@link MapTile#getImage()}
	 *            <b>must</b> be non-null.
	 */
	public synchronized void addTile(MapTile mapTile) {
		MapRequest tileRequest = mapTile.getTileRequest();
		// prefer to work with the final image, but if there isn't one yet, fall back to the fuzzy
		BufferedImage tileImage = mapTile.getImage();
		if (tileImage == null) {
			tileImage = mapTile.getFuzzyImage();
		}
		if (tileImage == null) {
			throw new IllegalArgumentException("Tile has no image");
		}
		
		Dimension size = request.getImageSize();
		
		// create the target image to be compatible with the given image, if necessary
		if (image == null) {
			setImage(Util.createCompatibleImage(tileImage, size.width, size.height), tileRequest.getSource().getIgnoreValue());
		}
		
		// create image when the first tile is received
		Rectangle2D worldExtent = request.getExtent();
		int ppd = request.getPPD();
		
		Point2D offset = request.getSource().getOffset();
		
		// get an extent that includes all the pixels in 'image'
		// Use the offset values (which default to 0.0 for an unnudged map)
		Rectangle2D fixedExtent = new Rectangle2D.Double(worldExtent.getMinX()+offset.getX(),
				worldExtent.getMinY()+offset.getY(), size.width / (double)ppd, size.height / (double)ppd);
		
		int outBands = image.getRaster().getNumBands();
		int inBands = tileImage.getRaster().getNumBands();
		if (outBands != inBands) {
			log.aprintln("Wrong number of bands received for source " + tileRequest.getSource().getName() +"! Expected " + outBands + ", got " + inBands);
			return;
		}
		
		// for each occurrence of this tile in the requested unwrapped world extent
		Rectangle2D[] worldExtents = Util.toUnwrappedWorld(tileRequest.getExtent(), fixedExtent);
		for (int i = 0; i < worldExtents.length; i++) {
			Rectangle2D worldTile = worldExtents[i];
			
			WritableRaster source = getRasterForWorld(tileImage.getRaster(), worldTile, fixedExtent);
			
			worldTile = new Rectangle2D.Double(worldExtents[i].getMinX()-offset.getX(), 
					worldExtents[i].getY()-offset.getY(), worldExtents[i].getWidth(),
					worldExtents[i].getHeight());
			
			Rectangle2D unShiftedExtent = new Rectangle2D.Double(worldExtent.getMinX(),
					worldExtent.getMinY(), size.width / (double)ppd, size.height / (double)ppd);
			
			WritableRaster target = getRasterForWorld(image.getRaster(), unShiftedExtent, worldTile);
			
			// get source and target rasters based on world coordinates
			log.println("Updating request " + rect(fixedExtent) + " with tile " + rect(tileRequest.getExtent()));
			
			if (target == null || source == null) {
				log.println("Unable to extract intersecting area from both images, aborting tile update");
				return;
			}
			
			// copy tile into place
			target.setRect(source);
			
			// update areas
			// This can potentially be reached with cancelled/errored mapTiles... is this safe?
			Area usedTileArea = new Area(worldTile.createIntersection(unShiftedExtent));
			if (!mapTile.getRequest().isCancelled() && !mapTile.hasError()) {
				(mapTile.isFinal() ? getFinishedArea() : getFuzzyArea()).add(new Area(usedTileArea));
			}
		}
	}
	
	/**
	 * Returns a WritableRaster for each rectangle in the changed area.
	 */
	public WritableRaster[] getChangedRasters(Area changedArea) {
		Rectangle2D[] rects = new PolyArea(changedArea).getRectangles();
		WritableRaster[] out = new WritableRaster[rects.length];
		int pos = 0;
		for (Rectangle2D rect: rects) {
			out[pos++] = MapData.getRasterForWorld(getImage().getRaster(), getRequest().getExtent(), rect);
		}
		return out;
	}
	
	public static String rect(Rectangle2D r) {
		return r.getMinX() + "," + r.getMinY() + " to " + r.getMaxX() + "," + r.getMaxY();
	}
	
	/** Returns a deep copy of this object, including the image */
	public synchronized MapData getDeepCopy() {
		return getDeepCopyShell(image == null ? null : copyImage(image), nullPixel);
	}
	
	/** Returns a new MapData object with clones of this MapData's properties, but the given image instead. */
	public synchronized MapData getDeepCopyShell(BufferedImage image, double[] nullPixel) {
		MapData md = new MapData(request, image, nullPixel);
		md.finished = isFinished();
		md.finishedArea = (Area)getFinishedArea().clone();
		md.fuzzyArea = (Area)getFuzzyArea().clone();
		return md;
	}
	
	/**
	 * Returns <code>this</code> if this image is null or already using
	 * {@link ComponentColorModel}, otherwise returns a new MapData with a new
	 * {@link BufferedImage} that does use a {@link ComponentColorModel}.
	 */
	public MapData convertToCCM() {
		if (image == null || image.getColorModel() instanceof ComponentColorModel) {
			return this;
		} else {
			ColorModel srcCm = image.getColorModel();
			ColorModel dstCm = new ComponentColorModel(
				srcCm.getColorSpace(),
				srcCm.getComponentSize(),
				srcCm.hasAlpha(),
				false,
				srcCm.getTransparency(),
				DataBuffer.TYPE_BYTE);
			
			WritableRaster dstRaster = dstCm.createCompatibleWritableRaster(image.getWidth(), image.getHeight());
			BufferedImage dst = new BufferedImage(dstCm, dstRaster, dstCm.isAlphaPremultiplied(), null);
			Graphics2D g2 = dst.createGraphics();
			g2.drawImage(image, null, 0, 0);
			g2.dispose();
			return getDeepCopyShell(dst, nullPixel);
		}
	}
	
	/**
	 * Returns <code>this</code> if this {@link MapData}'s request is equal to
	 * the given request, otherwise a new {@link MapData} object is created with
	 * the new request, the finished and fuzzy areas are cropped to the extent
	 * of the new request, and the image is cropped and scaled to the new
	 * request.
	 */
	public MapData convertToRequest(MapRequest newRequest) {
		final long start = System.currentTimeMillis();
		final BufferedImage outImage;
		final int oldPPD = request.getPPD();
		final Rectangle2D oldExtent = request.getExtent();
		final int newPPD = newRequest.getPPD();
		final Rectangle2D newExtent = newRequest.getExtent();
		
		if (request.equals(newRequest)) {
			// already conforms to this request so just return self
			return this;
		} else if (!oldExtent.contains(newExtent)) {
			// this implementation does not handle filling areas in the output
			// raster that do not exist in the input raster
			log.aprintln("New request extent must be entirely inside old request extent");
			outImage = null;
		} else if (image == null) {
			// just forward a null image
			outImage = null;
		} else if (!oldExtent.equals(newExtent) || oldPPD != newPPD) {
			// must transform this image to the scale of the 'newRequest'
			// and crop to the extent at that scale
			final int w = (int)Math.round(newExtent.getWidth() * newPPD);
			final int h = (int)Math.round(newExtent.getHeight() * newPPD);
			final Raster inRaster = image.getRaster();
			final WritableRaster outRaster = inRaster.createCompatibleWritableRaster(w, h);
			final ColorModel cm = image.getColorModel();
			outImage = new BufferedImage(cm, outRaster, cm.isAlphaPremultiplied(), null);
			
			// transform from pixel position in outRaster to world coordinates
			// to pixel position in inRaster
			final AffineTransform out2world = Util.image2world(w, h, newExtent);
			final AffineTransform world2in = Util.world2image(oldExtent, image.getWidth(), image.getHeight());
			final AffineTransform at = new AffineTransform(world2in);
			at.concatenate(out2world);
			
			try {
				// the op uses the transform to map the input rectangle onto
				// the output rectangle, and we set the clip boundaries by
				// explicitly creating the output image
				new AffineTransformOp(at.createInverse(),AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(inRaster, outRaster);
			} catch (Exception e) {
				// the op fails for many common cases, so when it doesn't work,
				// we use this slower technique instead; fortunately there isn't
				// much cost to always first trying the fast way, since there are
				// no stated limitations on AffineTransformOp to tell us where it
				// should work.
				// TODO: this is a good start on a generalized FixedAffineTransformOp,
				// so if we don't find another better alternative in e.g. JAI we should
				// port this up to a utility package.
				log.println("AffineTransformOp error, trying slower technique");
				final Point2D.Double p = new Point2D.Double();
				final Point2D.Double q = new Point2D.Double();
				Object pixel = null;
				try {
					for (int row = 0; row < h; row++) {
						p.y = row;
						for (int col = 0; col < w; col++) {
							p.x = col;
							at.transform(p, q);
							// TODO: this is an order of magnitude more expensive than
							// the rest of this manual affine operator!
							pixel = inRaster.getDataElements((int)q.x, (int)q.y, pixel);
							outRaster.setDataElements(col, row, pixel);
						}
					}
				} catch (Exception e2) {
					log.aprintln("Out of bounds: " + p.x + ", " + p.y);
				}
			}
		} else {
			outImage = copyImage(image);
		}
		
		final MapData outdata = new MapData(newRequest, outImage, nullPixel);
		outdata.finishedArea = new Area(finishedArea);
		outdata.fuzzyArea = new Area(fuzzyArea);
		outdata.finished = finished;
		// clip the areas down to the new request extent
		final Area clip = new Area(newExtent);
		outdata.finishedArea.intersect(clip);
		outdata.fuzzyArea.intersect(clip);
		
		log.println("Finished in " + (System.currentTimeMillis()-start));
		return outdata;
	}
	
	private static BufferedImage copyImage(BufferedImage image) {
		return new BufferedImage(
			image.getColorModel(),
			image.copyData(null),
			image.isAlphaPremultiplied(), null);
	}
	
	/**
	 * @return True if the given location in this image contains a null pixel;
	 *         common reasons for a null pixel are a transparent alpha value or
	 *         that pixel is equal to the ignore value
	 */
	public boolean isNull(int x, int y) {
		return nullTester.isNull(x, y);
	}
	
	public double[] getNullPixel() {
		return nullPixel;
	}
	
	/**
	 * Used to test if individual pixels are null, and to get Composite
	 * implementations suitable for drawing MapData objects.
	 */
	interface NullTest {
		boolean isNull(int x, int y);
	}
	
	/** The default null tester handles images that have no null pixels */ 
	private static final class DefaultTest implements NullTest {
		public boolean isNull(int x, int y) {
			return false;
		}
	}
	
	/** The alpha null tester handles images that have an alpha band */
	private static final class AlphaTest implements NullTest {
		private final Raster alphaBand;
		public AlphaTest(Raster alphaBand) {
			this.alphaBand = alphaBand;
			if (alphaBand == null) {
				throw new IllegalArgumentException("The alpha band must not be null");
			}
		}
		public boolean isNull(int x, int y) {
			return alphaBand.getSample(x, y, 0) == 0;
		}
	}
	
	/**
	 * The ignore null tester handles images that have an ignore value. Testing
	 * is done by getting the transfer type of the image, and casting it to an
	 * array of the image's transfer type. Comparison of packed pixels is done by
	 * converting the ignore value into the same packed representation and
	 * masking off an alpha band if applicable.
	 */
	private static final class IgnoreTest implements NullTest {
		private final Raster raster;
		private double[] ignore;
		private double[] pixel;
		public IgnoreTest(BufferedImage bi, double[] ignore) {
			// check for a match between non-alpha band count and ignore count
			final int numColors = bi.getColorModel().getNumColorComponents();
			if (ignore.length != 1 && numColors != ignore.length) {
				throw new IllegalArgumentException("Unable to create ignore test, given " +
					ignore.length + " samples for an image with " + numColors + " bands");
			}
			
			this.raster = getColorRaster(bi);
			
			if (ignore.length == 1 && numColors != 1) {
				double tmp = ignore[0];
				ignore = new double[numColors];
				Arrays.fill(ignore, tmp);
			}
			
			this.ignore = ignore;
		}
		/**
		 * This method is not thread safe and should be guarded by a lock
		 * outside the loop over the x/y range being tested.
		 * @return true if the given position in the image contains no data.
		 */
		public boolean isNull(int x, int y) {
			pixel = raster.getPixel(x, y, pixel);
			for (int i = 0; i < ignore.length; i++) {
				if (ignore[i] != pixel[i]) {
					return false;
				}
			}
			return true;
		}
	}
	
	/**
	 * @return an operator that will add an alpha band if necessary and set
	 *         alpha=0 where source pixel values equal a given ignore value,
	 *         or null if the image already has an alpha band.
	 */
	public BufferedImageOp getOperator() {
		if (nullPixel == null) {
			return null;
		} else {
			return new IgnoreOp(nullTester);
		}
	}
	
	private static final class IgnoreOp implements BufferedImageOp {
		private final NullTest test;
		public IgnoreOp(NullTest nullTester) {
			this.test = nullTester;
		}
		public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
			if (!destCM.hasAlpha()) {
				throw new IllegalArgumentException("Destination color model must have an alpha band");
			}
			return new BufferedImage(
				destCM,
				destCM.createCompatibleWritableRaster(src.getWidth(), src.getHeight()),
				false, null);
		}
		public BufferedImage filter(BufferedImage src, BufferedImage dest) {
			final WritableRaster alpha = dest.getAlphaRaster();
			final int[] hidden = {0};
			final int[] pixels = new int[dest.getWidth()];
			for (int j = 0; j < dest.getHeight(); j++) {
				src.getRGB(0, j, pixels.length, 1, pixels, 0, pixels.length);
				for (int i = 0; i < pixels.length; i++) {
					if (test.isNull(i, j)) {
						alpha.setPixel(i, j, hidden);
					} else {
						dest.setRGB(i, j, pixels[i]);
					}
				}
			}
			return dest;
		}
		public Rectangle2D getBounds2D(BufferedImage src) {
			return src.getRaster().getBounds();
		}
		public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
			throw new UnsupportedOperationException();
		}
		public RenderingHints getRenderingHints() {
			return null;
		}
	}
}
