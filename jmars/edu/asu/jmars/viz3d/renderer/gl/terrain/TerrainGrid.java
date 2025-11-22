/**
 * 
 */
package edu.asu.jmars.viz3d.renderer.gl.terrain;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.viz3d.core.geometry.OctTree;
import edu.asu.jmars.viz3d.core.geometry.Polygon;
import edu.asu.jmars.viz3d.core.geometry.Ray;
import edu.asu.jmars.viz3d.core.geometry.Triangle;
import edu.asu.jmars.viz3d.renderer.textures.GlobalDecalFactory;

/**
 * Class to generate a global set of TerrainTiles based on PPD
 *
 * Intended to be used to provide all the underlying geometry needed to fit a full coverage set of Decals to the body
 */
public class TerrainGrid {
	
	private TerrainTile[][] grid;
	private int ppd;
	private int rowSize;
	private int colSize;
	private float degPerTile;
	private float latDegPerTile;
	private float lonDegPerTile;
	private float tilePad;
	private Rectangle2D currentExtent = new Rectangle2D.Float();
	private static final int X = 0;
	private static final int Y = 1;
	private static final int Z = 2;	
	private static DebugLog log = DebugLog.instance();

	
	/**
	 * Constructor that creates the grid of TerrainTiles
	 * @param ppd the desired PPD (determines the size of each TerrainTile
	 * @param oct OctTree representation of the body the tiles will be fit to
	 */
	public TerrainGrid(int ppd, OctTree oct) {
		log.aprintln("Constructing the TerrainGrid.");
		this.ppd = ppd;
		float scaleVal = oct.getWORLD_SIZE()/2;
		// TILE_SIZE and PPD must be powers of 2
//		degPerTile = TerrainTile.TILE_SIZE/ppd;
		latDegPerTile = 10f;
		lonDegPerTile = 45f;
		degPerTile = 10;
		tilePad = latDegPerTile / 2;
		float fRow = (160f / latDegPerTile) + 1 ;
		rowSize = (int)fRow;
		// add any partial tiles at the end
		rowSize += (fRow - rowSize > 0) ? 1 : 0;
		
		float fCol = 360f / lonDegPerTile;
		colSize = (int)fCol;
		colSize += (fCol - colSize > 0) ? 1 : 0;
		
		grid = new TerrainTile[rowSize][colSize];
		float lat = 90f;
		float lon = -160f;
		float lonDeg = lonDegPerTile;
		float latDeg = latDegPerTile;
		float lonCenter = 45f;

		for (int i=0; i<rowSize; i++) {
			lon = -180f;
			float latCenter = lat - (latDeg / 2); 
			Projection_OC proj = new ProjObj.Projection_OC(lonCenter, latCenter); // start at Lon of 20 so we can overlap tiles without crossing the world prime meridian
			double worldLonCenter = proj.getCenterLon();
			double worldLatCenter = 0;

			for (int j=0; j<colSize; j++) {
//				float[] tileCorners = new float[12]; 
				TerrainTile tile = new TerrainTile();
				float worldPad = 3 * tilePad;
				
				// extent in world coords
				Rectangle2D extent = new Rectangle2D.Float((float)((worldLonCenter+(j*lonDegPerTile))-45), (float)(worldLatCenter+worldPad), (float)((worldLonCenter+(j*lonDegPerTile))+45), (float)(worldLatCenter-worldPad));
				if (extent.getX() > extent.getWidth()) {
					tile.setMeridian(true);
				}	
				
				Point2D[] extExtent = new Point2D[4];
				extExtent[0] = new Point2D.Double(extent.getX(), extent.getY());
				extExtent[1] = new Point2D.Double(extent.getX(), extent.getHeight());
				extExtent[2] = new Point2D.Double(extent.getWidth(), extent.getHeight());
				extExtent[3] = new Point2D.Double(extent.getWidth(), extent.getY());
				Point2D[] polyPts = extExtent;
				
				float[] xyzCorners = convWorldToXYZ(polyPts, proj);

				float[] ll = worldLonLatToSpatial(polyPts, proj);				

//				tileCorners = lonLatToXyz(ll, 1f);	
				
				tile.setExtent(extent);
				tile.setCorners(xyzCorners);
				tile.setLonLats(ll);
				tile.setProjection(proj);
				tile.setProjLonCenter(lonCenter);
				tile.setProjLatCenter(latCenter);
				grid[i][j] = tile;

			lon += lonDegPerTile;
			}
			lat -= latDegPerTile;
		}
	}
	
	/**
	 * Converts east leading longitude and latitude to a 3 dimensional unit vector
	 *
	 * @param lonLat
	 * @return 3D unit vector
	 */
	public static float[] lonLatToXyz(float[] lonLat) {
		HVector hv = new HVector(360f-lonLat[0], lonLat[1]);
		
		return new float[]{(float)hv.x, (float)hv.y, (float)hv.z};
	}
	
	/**
	 * Converts east leading longitude and latitude to a 3 dimensional vector scaled to be larger than the max dimension of the OctTree
	 *
	 * @param lonLat
	 * @return 3D vector
	 */
	public static float[] lonLatToXyz(float[] lonLat, float scalar) {
		
		float[] ret = new float[lonLat.length / 2 * 3];
		int idx = 0;
		
		for (int i=0; i<lonLat.length; i+=2) {
			HVector hv = new HVector(360f-lonLat[i], lonLat[i+1]);
			
			float[] pt = new float[]{(float)hv.x, (float)hv.y, (float)hv.z};
			
			if (scalar > 1f) {
				pt = VectorUtil.scaleVec3(new float[3], pt, scalar);
			}
			ret[idx++] = pt[0];
			ret[idx++] = pt[1];
			ret[idx++] = pt[2];
		}
		
		return ret;
	}
	
	/**
	 * Converts a 3D vector to an East leading Longitude/Latitude pair 
	 * @param point 3D vector to convert
	 * @return Lon/Lat
	 */
	public static float[] xyzToLonLat(float[] point) {
		HVector hv = new HVector(point[0], point[1], point[2]);
		
		return new float[]{(float)hv.lonE(), (float)hv.lat()};
	}
	
	/**
	 * Method to return the first tile in the grid
	 * @return
	 */
	public TerrainTile getFirstTile() {
		if (grid != null) {
			return grid[0][0];
		} else {
			return null;
		}
	}
	
	/**
	 * Method to return the entire grip
	 * @return
	 */
	public TerrainTile[][] getGrid() {
		return grid;
	}
	
	/** 
	 * Method to return all the tiles that are all or partially covered by the extent
	 * @param extent
	 * @return
	 */
	public TerrainTile[][] getTilesByExtent(Rectangle2D extent) {
		if (extent == null) {
			return null;
		}
		//TODO get any tiles that fall within the requested extent		
		return grid;
	}
	
	/**
	 * Method to convert one or more East leading Longitude/Latitude pairs to world coordinates as found in an equatorial projection
	 * @param lonlat array of interleaved LonLat pairs
	 * @return interleaved array (U, V) of world coordinates
	 */
    public double[] lonLatToEquatorialWorldHVector(float[] lonlat) {
        double[] worlds = new double[lonlat.length];
    		int idx = 0;
        ProjObj proj = GlobalDecalFactory.getEquProjection();  
        double longitude = 0.0;
        double latitude = 0.0;
        	
        for (int i=0; i<lonlat.length; i+=2) {
        		latitude = lonlat[i+1];
        		longitude = lonlat[i];
        		Point2D spw = proj.convSpatialToWorld(360.0-(longitude), latitude);
			worlds[idx++] = spw.getX();
			worlds[idx++] = spw.getY();      	
        }
        
        return worlds;
    }
    
    public double[] lonLatToWorldHVector(float[] lonlat, ProjObj proj) {
        double[] worlds = new double[lonlat.length];
    	int idx = 0;
        double longitude = 0.0;
        double latitude = 0.0;
        	
        for (int i=0; i<lonlat.length; i+=2) {
    		latitude = lonlat[i+1];
    		longitude = lonlat[i];
    		Point2D spw = proj.convSpatialToWorld(360.0-(longitude), latitude);
			worlds[idx++] = spw.getX();
			worlds[idx++] = spw.getY();      	
        }
        
        return worlds;
    }
    
    public double[] lonLatToPolarWorldHVector(float[] lonlat) {
        double[] worlds = new double[lonlat.length];
    		int idx = 0;
        ProjObj proj = GlobalDecalFactory.getPolarProjection();  
        double longitude = 0.0;
        double latitude = 0.0;
        	
        for (int i=0; i<lonlat.length; i+=2) {
        		latitude = lonlat[i+1];
        		longitude = lonlat[i];
        		Point2D spw = proj.convSpatialToWorld(360.0-(longitude), latitude);
			worlds[idx++] = spw.getX();
			worlds[idx++] = spw.getY();      	
        }
        
        return worlds;
    }
    
    public double[] polarWorldToSpatialLonLat(float[] lonlat) {
        double[] spatial = new double[lonlat.length];
    		int idx = 0;
        ProjObj proj = GlobalDecalFactory.getPolarProjection();  
        double longitude = 0.0;
        double latitude = 0.0;
        	
        for (int i=0; i<lonlat.length; i+=2) {
        		latitude = lonlat[i+1];
        		longitude = lonlat[i];
        		Point2D spw = proj.convWorldToSpatial(longitude, latitude);
			spatial[idx++] = spw.getX();
			spatial[idx++] = spw.getY();      	
        }
        
        return spatial;
    }
  
    public double[] worldToSpatialLonLat(float[] lonlat, ProjObj proj) {
        double[] spatial = new double[lonlat.length];
    	int idx = 0;
        double longitude = 0.0;
        double latitude = 0.0;
        	
        for (int i=0; i<lonlat.length; i+=2) {
        		latitude = lonlat[i+1];
        		longitude = lonlat[i];
        		Point2D spw = proj.convWorldToSpatial(longitude, latitude);
			spatial[idx++] = spw.getX();
			spatial[idx++] = spw.getY();      	
        }
        
        return spatial;
    }
    
    public float[] worldLonLatToSpatial(Point2D[] lonlat, ProjObj proj) {
        float[] spatial = new float[lonlat.length * 2];
    	int idx = 0;
        double longitude = 0.0;
        double latitude = 0.0;
        	
        for (int i=0; i<lonlat.length; i++) {
    		latitude = lonlat[i].getY();
    		longitude = lonlat[i].getX();
    		Point2D spw = proj.convWorldToSpatial(longitude, latitude);
			spatial[idx++] = (float)spw.getX();
			spatial[idx++] = (float)spw.getY();      	
        }
        
        return spatial;
    }
    
    public float[] convWorldToXYZ(Point2D[] lonlat, ProjObj proj) {
        float[] xyz = new float[lonlat.length * 3];
    	int idx = 0;
    	for (Point2D p : lonlat) {
    		HVector v = HVector.fromSpatial(proj.convWorldToSpatial(p));
    		xyz[idx++] = (float)v.x;
    		xyz[idx++] = (float)v.y;
    		xyz[idx++] = (float)v.z;    	
    	}
    	return xyz;
    }
    
    public HVector[] convWorldToXYZVecs(Point2D[] lonlat, ProjObj proj) {
        HVector[] xyz = new HVector[lonlat.length];
    	int idx = 0;
    	for (Point2D p : lonlat) {
    		xyz[idx++] = HVector.fromSpatial(proj.convWorldToSpatial(p));
    	}
    	return xyz;
    }
  
    public float[] convWorldToSpatial(Point2D[] lonlat, ProjObj proj) {
        float[] spatial = new float[lonlat.length * 2];
    	int idx = 0;
    	for (Point2D p : lonlat) {
    		Point2D pt = proj.convWorldToSpatial(p);
    		spatial[idx++] = (float)pt.getX();
    		spatial[idx++] = (float)pt.getY();
    	}
    	return spatial;
    }

    public Point2D[] interpolateWorldPolygon(Point2D[] in, int numPoints) {
    	Point2D[] ret = new Point2D[in.length+(in.length*numPoints)];
    	int idx = 0;
    	for (int i=0; i<in.length-1; i++) {
    		ret[idx++] = in[i];
    		double lonInc = (in[i+1].getX() - in[i].getX()) / (numPoints + 1);
    		double latInc = (in[i+1].getY() - in[i].getY()) / (numPoints + 1);
    		for (int j=1; j<=numPoints; j++) {
    			ret[idx++] = new Point2D.Double(in[i].getX()+(j*lonInc), in[i].getY()+(j*latInc));
    		}
    	}
    	// interpolate between the last point and the first point
		ret[idx++] = in[in.length-1];
		double lonInc = (in[0].getX() - in[in.length-1].getX()) / (numPoints + 1);
		double latInc = (in[0].getY() - in[in.length-1].getY()) / (numPoints + 1);
		for (int j=1; j<=numPoints; j++) {
			ret[idx++] = new Point2D.Double(in[in.length-1].getX()+(j*lonInc), in[in.length-1].getY()+(j*latInc));
		}
    	    	
    	return ret;
    }
    

    /**
     * Generic method to release the grid resources
     */
	public void dispose() {
		currentExtent = null;
		for (int i=0; i<grid.length; i++) {
			for (int j=0; j<grid[0].length; j++) {
				grid[i][j].dispose();
			}
		}
		grid = null;
	}
}
