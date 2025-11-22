package edu.asu.jmars.places;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.parsers.gis.CoordinatesParser.Ordering;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;

/**
 * Defines a location, projection, ppd, map offsets, and labels necessary
 * for JMARS to bookmark a surface position.
 * 
 * The hashCode and equals methods define equality based on the name, so name
 * must be unique. Furthermore the name may have constraints from the
 * {@link PlaceStore} in use, so care should be taken in a user interface to
 * control the values set on the name property.
 */
public class Place {
	private String name;
	private Point2D lonLat;
	private Point2D projCenterLonLat;
	private Integer ppd;
	private final List<MapOffset> offsets = new ArrayList<MapOffset>();
	private final List<String> labels = new ArrayList<String>();
	
	/**
	 * Restores the view to a given place; always sets the location, will only
	 * restore the projection, ppd, and maps if instructed to
	 */
	public void gotoPlace(boolean restoreProj, boolean restorePPD, boolean restoreMaps) {
		if (restoreProj) {
			ProjObj po = new ProjObj.Projection_OC(projCenterLonLat.getX(), projCenterLonLat.getY());
			Main.setProjection(po);
			if (restoreMaps) {
				for (MapOffset offset: offsets) {
					MapServer server = MapServerFactory.getServerByName(offset.serverName);
					if (server != null) {
						MapSource source = server.getSourceByName(offset.mapName);
						if (source != null) {
							source.setOffset(offset.worldDelta);
						}
					}
				}
				if (offsets.size() > 0) {
					Main.testDriver.repaint();
				}
			}
		}
		Point2D worldPoint = Main.PO.convSpatialToWorld(lonLat);
		Main.testDriver.locMgr.setLocation(worldPoint, true);
		if (restorePPD) {
			Main.testDriver.mainWindow.getZoomManager().setZoomPPD(ppd, true);
		}
	}
	
	/**
	 * Creates a place from the current location, capturing a default name, the
	 * ppd and projection, and of course the location.
	 */
	public Place() {
		lonLat = Main.PO.convWorldToSpatial(Main.testDriver.locMgr.getLoc());
		projCenterLonLat = Main.PO.getProjectionCenter();
		ppd = Main.testDriver.mainWindow.getZoomManager().getZoomPPD();
		String coordOrdering = Config.get(Config.CONFIG_LAT_LON,Ordering.LAT_LON.asString());
		Ordering ordering = Ordering.get(coordOrdering);	
		name = ordering.formatNoMath(lonLat);	
		if (MapServerFactory.getMapServers() != null) {
			for (MapServer server: MapServerFactory.getMapServers()) {
				for (MapSource source: server.getMapSources()) {
					Point2D delta = source.getOffset();
					if (delta.getX() != 0 || delta.getY() != 0) {
						offsets.add(new MapOffset(server.getName(), source.getName(), delta));
					}
				}
			}
		}
	}
	
	/** Returns a new point with the lon = 360 - lon0 */
	public static Point2D flipLon(Point2D p) {
		return new Point2D.Double(Util.mod360(360-p.getX()),p.getY());
	}
	
	/**
	 * Returns the internal list of map offsets for this place;
	 * <strong>warning</strong> Changing the returned list changes the contents
	 * of this place!
	 */
	public List<MapOffset> getMapOffsets() {
		return offsets;
	}

	/**
	 * Returns the internal list of labels for this place.
	 * <strong>warning</strong> Changing the returned list changes the contents
	 * of this place!
	 */
	public List<String> getLabels() {
		return labels;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/** Returns the location in west-leading longitude, ocentric latitude */
	public Point2D getLonLat() {
		return lonLat;
	}

	/** Sets the location in west-leading longitude, ocentric latitude */
	public void setLonLat(Point2D lonLat) {
		this.lonLat = lonLat;
	}

	/** Returns the projection center in west longitude, ocentric latitude */
	public Point2D getProjCenterLonLat() {
		return projCenterLonLat;
	}

	/** Sets the projection center in west longitude, ocentric latitude */
	public void setProjCenterLonLat(Point2D projCenterLonLat) {
		this.projCenterLonLat = projCenterLonLat;
	}

	public Integer getPpd() {
		return ppd;
	}

	public void setPpd(Integer ppd) {
		this.ppd = ppd;
	}
	
	/** Returns the name */
	public String toString() {
		return name;
	}
	
	/** Returns the hashcode of the name */
	public int hashCode() {
		return name.hashCode();
	}
	
	/** Equals if o is a Place with the same name as this place */
	public boolean equals(Object o) {
		return o instanceof Place && name.equals(((Place)o).name);
	}

}
