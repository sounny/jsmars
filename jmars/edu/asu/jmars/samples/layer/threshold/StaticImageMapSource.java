package edu.asu.jmars.samples.layer.threshold;


import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;

import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapAttrReceiver;
import edu.asu.jmars.layer.map2.MapRequest;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.MapSourceListener;
import edu.asu.jmars.layer.map2.NonRetryableException;
import edu.asu.jmars.layer.map2.RetryableException;

import edu.asu.jmars.ProjObj;

class StaticImageMapSource implements MapSource {
    private static final long serialVersionUID = -6919721959279725735L;
	BufferedImage image;
	MapAttr mapAttr;
	Point2D offset;
	NullMapServer mapServer;
	String name;
	double[] ignoreValues;
	private transient List<MapSourceListener> listeners;
	
	public StaticImageMapSource(BufferedImage image, String name){
		this.image = image;
		this.name = name;
		this.mapAttr = new MapAttr(image);
		Arrays.fill(ignoreValues = new double[image.getRaster().getNumBands()], 0);
		this.offset = new Point2D.Double();
		this.mapServer = new NullMapServer();
		this.mapServer.add(this);
	}
	
	public BufferedImage fetchTile(MapRequest mapTileRequest) throws RetryableException, NonRetryableException {
		Rectangle2D r = mapTileRequest.getExtent();
		int reqPpd = mapTileRequest.getPPD();
		ProjObj projObj = mapTileRequest.getProjection();

		if (r.getWidth() < 0 || r.getHeight() < 0)
			return null;
		
		int ow = (int)(reqPpd * r.getWidth());
		int oh = (int)(reqPpd * r.getHeight());
		
		WritableRaster outRaster = image.getRaster().createCompatibleWritableRaster(ow, oh);
		BufferedImage outImage = new BufferedImage(image.getColorModel(), outRaster, image.isAlphaPremultiplied(), null);
		
		double imagePpdX = image.getWidth()/360.0;
		double imagePpdY = image.getHeight()/180.0;
		
		Raster inRaster = image.getRaster();
		double[] dArray = null;
		
		for(int i=0; i<ow; i++){
			for(int j=0; j<oh; j++){
				Point2D sp = projObj.convWorldToSpatial(
						r.getMinX() + offset.getX() + ((double)i)/(double)reqPpd,
						r.getMaxY() - offset.getY() - ((double)j)/(double)reqPpd);
				double lon = (360-sp.getX())%360;
				double lat = sp.getY();
				
				if (lon >= 0 && lon < 360 && lat > -90 && lat <= 90){
					int x = (int)(lon * imagePpdX);
					int y = (int)((90-lat) * imagePpdY);
					
					try {
						dArray = inRaster.getPixel(x, y, dArray);
					}
					catch(Exception ex){
						ex.printStackTrace();
					}
					outRaster.setPixel(i, j, dArray);
				}
			}
		}
		
		return outImage;
	}

	public String getAbstract() {
		return name;
	}

	public String[][] getCategories() {
		return new String[0][0];
	}

	public MapAttr getMapAttr() {
		return mapAttr;
	}

	public void getMapAttr(final MapAttrReceiver receiver) {
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				receiver.receive(mapAttr);
			}
		});
	}

	public String getMimeType() {
		return hasNumericKeyword()? "image/vicar": "image/png";
	}

	public String getName() {
		return name;
	}

	public MapServer getServer() {
		return mapServer;
	}

	public String getTitle() {
		return name;
	}

	public String getUnits() {
		return name;
	}
	
	public boolean hasNumericKeyword() {
		return image.getType() == BufferedImage.TYPE_CUSTOM;
	}

	public boolean hasElevationKeyword() {
//		return image.getType() == BufferedImage.TYPE_CUSTOM;
		return false;
	}
	
	public boolean hasGeologicKeyword() {
		return false;
	}
	
	public String getOwner() {
		return null;
	}

	public boolean isMovable() {
		return true;
	}

	/**
	 * Sets the ignore value to a new double array.
	 * @param newIgnoreValue a double array.
	 */
	public void setIgnoreValue(double[] newIgnoreValue){
		this.ignoreValues = newIgnoreValue;
	}
	
	public double[] getIgnoreValue() {
		return ignoreValues;
	}

	public Point2D getOffset() {
		return (Point2D)offset.clone();
	}
	
	public void setOffset(Point2D offset) {
		offset.setLocation(offset);
	}
	
	public Rectangle2D getLatLonBoundingBox() {
		return new Rectangle2D.Double(0,-90,360,180);
	}
	
	/**
	 * Throws a runtime exception if called.  This is just here to meet the MapSource interface requirement.
	 * @param newMaxPPD
	 */
	public void setMaxPPD(double newMaxPPD){
		throw new RuntimeException("setMaxPPD can not be called on a StaticImageMapSource.");
	}
	
	public double getMaxPPD() {
		return Math.max(image.getWidth() / 360d, image.getHeight() / 180d);
	}
	
	public void addListener(MapSourceListener l) {
		listeners.add(l);
	}
	
	public void removeListener(MapSourceListener l) {
		listeners.remove(l);
	}
	
	private void changed() {
		for (MapSourceListener l: new ArrayList<MapSourceListener>(listeners)) {
			l.changed(this);
		}
	}
}

