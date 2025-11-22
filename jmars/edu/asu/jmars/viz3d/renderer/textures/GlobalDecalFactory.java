/**
 * 
 */
package edu.asu.jmars.viz3d.renderer.textures;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import com.jogamp.opengl.math.FloatUtil;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.MultiKeyHashMap;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDException;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.asu.jmars.viz3d.core.geometry.OctTree;
import edu.asu.jmars.viz3d.core.geometry.Triangle;
import edu.asu.jmars.viz3d.renderer.gl.terrain.TerrainCache;
import edu.asu.jmars.viz3d.renderer.gl.terrain.TerrainGrid;
import edu.asu.jmars.viz3d.renderer.gl.terrain.TerrainTile;
import edu.asu.jmars.viz3d.renderer.gl.terrain.TerrainTileFactory;

/**
 * This class will create a set of global (in terms of a planetary body) set of Decals based on the desired pixels per degree (PPD)
 *
 */
public class GlobalDecalFactory {
	
	private static DebugLog log = DebugLog.instance();

	private static GlobalDecalFactory factory = null;
	private static Projection_OC equProjection = new ProjObj.Projection_OC(0.0, 0.0);	// create a equatorial projection
	private static Projection_OC polarProjection = new ProjObj.Projection_OC(0.0, 90.0);	// create a polar projection - not currently used
	private static int ppd = 8; // this needs to be dynamic in the near future
	
	private MultiKeyHashMap<String, Integer, ArrayList<DecalParameter>> extents = new MultiKeyHashMap<String, Integer, ArrayList<DecalParameter>>();
	
	private boolean SERIALIZE = false;	// do NOT enable this...it's BROKEN
	
	private GlobalDecalFactory() {
	}
	
	/**
	 * Single instance "construction" method
	 * @return single instance of GlobalDecalFactory
	 */
	public static synchronized GlobalDecalFactory getInstance() {
		if (factory == null) {
			factory = new GlobalDecalFactory();
		}
		return factory;
	}
	
	public static Point2D[] myNormalize360(Point2D points[]) {
		Point2D[] normalized = Util.normalize360(points);
		
		double shift = 0;
		for(int i=0; i<normalized.length; i++) {
			if (normalized[i].getX() < 0) {
				shift = 360;
				break;
			}
		}
		
		if (normalized.length > 0 && shift != 0) {
			for(int i=0; i<normalized.length; i++) {
				normalized[i].setLocation(normalized[i].getX()+shift, normalized[i].getY());
			}
		}
		return normalized;
	}
	
	public static GeneralPath myPointsToPolygon(Point2D points[]) {
		GeneralPath gp = new GeneralPath();
		for(int i=0; i<points.length; i++) {
			if (i == 0) {
				gp.moveTo(points[i].getX(), points[i].getY());
			}
			else {
				gp.lineTo(points[i].getX(), points[i].getY());
			}
		}
		gp.closePath();
		return gp;
	}
	
	private boolean buildGlobalExtent() throws IllegalArgumentException {
		log.aprintln("Building Global Extent.");
		ArrayList<DecalParameter> decals = new ArrayList<>();
		OctTree oct = null;
		String shapeModel = null;
		if (ThreeDManager.getInstance().hasShapeModel()) {
			shapeModel = ThreeDManager.getInstance().getShapeModel().getMeshName();
			oct = ThreeDManager.getInstance().getShapeModel().getOctTree();
		} else {
			log.aprintln("No available Shape Model to build Decal set.");
			return false;
		}
		if (oct == null) {
			log.aprintln("No OctTree associated with the Shape Model.");
			return false;
		}
		
		 //check for serialized DecalParameters for this shape model and ppd
		 //if so load them
		if (SERIALIZE) {
			decals = TerrainCache.readGrid(shapeModel, ppd);
		}
		if (decals != null && decals.size() > 0) {
			log.aprintln("Decal geometry retrieved from cache.");
			extents.put(shapeModel, ppd, decals);
			return true;
		} else {
			decals = new ArrayList<>();
		}

		TerrainTileFactory tileFactory = null;
		try {
			tileFactory = new TerrainTileFactory(ppd);
		} catch (IllegalArgumentException e) {
			log.aprintln(e.getMessage());
			e.printStackTrace();
			return false;
		} catch (ThreeDException e) {
			log.aprintln(e.getMessage());
			e.printStackTrace();
			return false;
		}
		
		log.aprintln("Starting Decal geometry construction.");

		TerrainGrid grid = null;
		if (tileFactory != null) { // TODO add threadpool
			grid = tileFactory.getGrid(shapeModel, ppd);
			// TODO need to handle custom (non-global) extents!!
			TerrainTile[][] tiles = grid.getTilesByExtent(new Rectangle2D.Float());	
			log.aprintln("Total tiles: "+(tiles.length * tiles[0].length));			

			float[] verts = ThreeDManager.getInstance().getShapeModel().getVertices(); 	// mesh vertices
			int[] ind = ThreeDManager.getInstance().getShapeModel().getIndices();		// mesh indices
			double[] lonlats = new double[verts.length * 2 / 3];
			
			vertsToLonLat(verts, ind, lonlats);
			
			generateTileExtents(verts, ind, tiles, lonlats);
			
			mapTilesToGeometry(tiles, decals);
			
			lonlats = null;
		}

		if (SERIALIZE) {
			TerrainCache.writeGrid(shapeModel, ppd, decals);			
		}
		extents.put(shapeModel, ppd, decals);
		return true;
	}
	
	class TileExtent implements Runnable {
		int[] idx = new int[] {0,3};
		
		public TileExtent(int[] arg) {
			idx = arg;
		}

		@Override
		public void run() {
		}
		
		public int[] getIndices() {
			return idx;
		}
		
	}
	
	class ThreeDLonLat implements Runnable {
		int[] idx = new int[] {0,3};
		
		public ThreeDLonLat(int[] arg) {
			idx = arg;
		}

		@Override
		public void run() {
		}
		
		public int[] getIndices() {
			return idx;
		}
		
	}
	
	private void vertsToLonLat(final float[] verts, final int[] ind, final double[] lonlats) {
		// calculate the number of threads
		int numThreads = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);
		
		int chunkSize = (ind.length / 3) / numThreads; 
		
		if (ind.length % chunkSize != 0) numThreads++;
		
		Thread[] threads = new Thread[numThreads];
		
		for (int j=0; j<threads.length; j++) {
			ThreeDLonLat tdll = new ThreeDLonLat(new int[] {j*chunkSize*3, (((j*chunkSize*3)+chunkSize*3) < ind.length ? ((j*chunkSize*3)+chunkSize*3) : ind.length)}) {
				@Override
				public void run() {
			    	double[] lls = new double[(idx[1] - idx[0]) * 2];
			    	int index = 0;
					for (int i=idx[0]; i<idx[1] && index < lls.length; i++) {
			            HVector temp = new HVector();
			            temp = temp.set(verts[ind[i]*3], verts[ind[i]*3 + 1], verts[ind[i]*3 + 2]);
			            lls[index++] = temp.lonW();
			            lls[index++] = temp.lat();
					}
					System.arraycopy(lls, 0, lonlats, ind[idx[0]]*2, lls.length);
				}
			};
			threads[j] = new Thread(tdll);
			threads[j].setName("VertexToLatLon "+j);
			threads[j].setPriority(Thread.MIN_PRIORITY);
			threads[j].setDaemon(true);
			threads[j].start();
		}
		
		for (Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				log.aprintln(e.getMessage());
			}
		}	
		
	}
	
	private void generateTileExtents(final float[] verts, final int[] ind, final TerrainTile[][] tiles, final double[] lonlats) {
		
		// calculate the number of threads
		int numThreads = Math.max(1, Runtime.getRuntime().availableProcessors() * 2);
		
		int chunkSize = (ind.length / 3) / numThreads; 
		
		if (ind.length % chunkSize != 0) numThreads++;
		
		Thread[] threads = new Thread[numThreads];
		
		for (int j=0; j<threads.length; j++) {
			TileExtent te = new TileExtent(new int[] {j*chunkSize*3, (((j*chunkSize*3)+chunkSize*3) < ind.length ? ((j*chunkSize*3)+chunkSize*3) : ind.length)}) {
				@Override
				public void run() {
					nextTri:			
						for (int i=idx[0]; i<idx[1]; i+=3) {

							final float[] v1 = new float[] {verts[ind[i]*3], verts[ind[i]*3 + 1], verts[ind[i]*3 + 2]}; 		// first vertex x,y,z
							final float[] v2 = new float[] {verts[ind[i+1]*3], verts[ind[i+1]*3 + 1], verts[ind[i+1]*3 + 2]};	// second vertex x,y,z
							final float[] v3 = new float[] {verts[ind[i+2]*3], verts[ind[i+2]*3 + 1], verts[ind[i+2]*3 + 2]};	// third vertex x,y,z

							ProjObj lastProj = null;
							Shape gpe360 = null;
							for (int x = 0; x < tiles.length; x++) {
								for (int y = 0; y < tiles[0].length; y++) {
									final TerrainTile t = tiles[x][y];
									if (t != null) {

										if (lastProj != t.getProjection()) {

											Point2D[] tri = new Point2D[] {
													t.getProjection().convSpatialToWorld(lonlats[ind[i]*2], lonlats[ind[i]*2 + 1]),
													t.getProjection().convSpatialToWorld(lonlats[ind[i+1]*2], lonlats[ind[i+1]*2 + 1]),
													t.getProjection().convSpatialToWorld(lonlats[ind[i+2]*2], lonlats[ind[i+2]*2 + 1])
											};
											Point2D[] normTri = myNormalize360(tri);
											gpe360 = myPointsToPolygon(normTri);
										}
										lastProj = t.getProjection();

										Rectangle2D ext = t.getExtent();
										Point2D[] box = new Point2D[] {new Point2D.Double(ext.getX(), ext.getY()), new Point2D.Double(ext.getX(), ext.getHeight()), new Point2D.Double(ext.getWidth(), ext.getHeight()), new Point2D.Double(ext.getWidth(), ext.getY())};

										Point2D[] normBox = myNormalize360(box);								
										Rectangle2D ext360 = myPointsToPolygon(normBox).getBounds2D();

										boolean fullyContained;
										if (ext360.contains(gpe360.getBounds2D())) {								
											fullyContained = true;
										} else {
											fullyContained = false;
											Shape ep360 = ext360; 

											int result = Util.contains360(ep360.getBounds2D(), new Shape[] {gpe360}).length;
											fullyContained = result > 0;
											if (fullyContained) {
												Util.contains360(ep360.getBounds2D(), new Shape[] {gpe360});
											}

										}

										if (fullyContained) {
											Triangle ft = new Triangle(new float[][] {v1, v2, v3});
											t.addFacet(ft);
											continue nextTri;
										}								
									}
								}
							}
						}						
				}
			};
			threads[j] = new Thread(te);
			threads[j].setName("TileExtent "+j);
			threads[j].setPriority(Thread.MIN_PRIORITY);
			threads[j].setDaemon(true);
			threads[j].start();
		}
		
		for (Thread t : threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				log.aprintln(e.getMessage());
			}
		}
	}
	
	private void mapTilesToGeometry(TerrainTile[][] tiles, ArrayList<DecalParameter> decals) {
		log.aprintln("Starting Tile mapping to geometry.");
		int discardedTiles = 0;
		for (int x = 0; x < tiles.length; x++) {
			for (int y = 0; y < tiles[0].length; y++) {
				TerrainTile t = tiles[x][y];
				if (t != null && t.getFacets().size() > 0) {
					ArrayList<Triangle> fills = t.getFacets();		
					// create indices, vertices, and texture coords for the geometry
					float[] vertices = new float[fills.size() * 9];
					float[] texCoords = new float[(vertices.length * 2) / 3];
					int[] indices = new int[vertices.length / 3];
					boolean shiftLongitude = false;
					double shiftAmount = 0.0;
					Rectangle2D.Double r1 = null;
					Rectangle2D.Double r2 = null;
					Rectangle2D rect = null; 
					rect = new Rectangle2D.Double(t.getExtent().getX(), t.getExtent().getHeight(), 
							t.getExtent().getWidth()-t.getExtent().getX(), t.getExtent().getY()-t.getExtent().getHeight());

					if (rect.getWidth() < 0) {
						System.err.format("Fixing rect: %g,%g,%g,%g\n",rect.getMinX(), rect.getMinY(), rect.getMaxX(), rect.getMaxY());
						rect.setFrameFromDiagonal(rect.getMinX(), rect.getMinY(), rect.getMaxX()+360, rect.getMaxY());
					}
					// Normalize the original rectangle
					r1 = new Rectangle2D.Double();
					r1.setFrame(rect);
					if(r1.width > 180)
					{
						r1.width -= Math.floor(r1.width/360) * 360;
						r1.width = 360 - r1.width;
						r1.x -= r1.width;
					}
					r1.x -= Math.floor(r1.x / 360) * 360;

					// Create the second rectangle, to catch shapes that cross 0/360
					r2 = (Rectangle2D.Double) r1.clone();
					r2.x += r2.x<180 ? 360 : -360;

					float minX = (float) t.getExtent().getX();
					float maxX = (float) t.getExtent().getWidth();
					float maxY = (float) t.getExtent().getY();
					float minY = (float) t.getExtent().getHeight();

					Rectangle2D newExtent = new Rectangle2D.Float(minX, minY, maxX, maxY);

					int vIdx = 0;
					int iIdx = 0;

					for (Triangle tri : fills) {
						indices[iIdx] = iIdx;
						iIdx++;
						vertices[vIdx++] = tri.points[0][0];
						vertices[vIdx++] = tri.points[0][1];
						vertices[vIdx++] = tri.points[0][2];

						indices[iIdx] = iIdx;
						iIdx++;
						vertices[vIdx++] = tri.points[1][0];
						vertices[vIdx++] = tri.points[1][1];
						vertices[vIdx++] = tri.points[1][2];

						indices[iIdx] = iIdx;
						iIdx++;
						vertices[vIdx++] = tri.points[2][0];
						vertices[vIdx++] = tri.points[2][1];
						vertices[vIdx++] = tri.points[2][2];
					}

					// calculate the texture coords

					double[] wv = vertsToWorldHVector(vertices, newExtent, t.getProjection(), shiftLongitude, shiftAmount);
					for (int j = 0; j < wv.length; j += 6) {
						GeneralPath gp = new GeneralPath();
						gp.moveTo(wv[j], wv[j+1]);
						gp.lineTo(wv[j+2], wv[j+3]);
						gp.lineTo(wv[j+4], wv[j+5]);
						gp.closePath();

						Shape tmpTri = Util.normalize360(gp);
						float coords1[] = new float[2];
						float coords2[] = new float[2];
						float coords3[] = new float[2];
						PathIterator pi = tmpTri.getPathIterator(null);
						pi.currentSegment(coords1); pi.next();
						pi.currentSegment(coords2); pi.next();
						pi.currentSegment(coords3); pi.next();
						Rectangle2D.Double r = null;
						if (r1.contains(tmpTri.getBounds2D())) {
							r = r1;										
						} else if (r2.contains(tmpTri.getBounds2D())) {
							r = r2;
						}
						double xLen = r.getMaxX() - r.getMinX();
						double yLen = r.getMaxY() - r.getMinY();
						texCoords[j] = (float) ((coords1[0] - r.getMinX()) / xLen);
						texCoords[j + 1] = (float) ((coords1[1] - r.getMinY()) / yLen);
						texCoords[j + 2] = (float) ((coords2[0] - r.getMinX()) / xLen);
						texCoords[j + 3] = (float) ((coords2[1] - r.getMinY()) / yLen);
						texCoords[j + 4] = (float) ((coords3[0] - r.getMinX()) / xLen);
						texCoords[j + 5] = (float) ((coords3[1] - r.getMinY()) / yLen);
					}

					t.setIndices(indices);
					t.setVerts(vertices);
					t.setTexs(texCoords);
					t.getFacets().clear(); // we don't want to serialize unnecessary data
				}
				
				if (t == null || t.getVerts() == null || t.getVerts().length < 1) {
					discardedTiles++;
					continue;
				}
				Point2D start = new Point2D.Double(t.getExtent().getX(), t.getExtent().getY());
				Point2D end = new Point2D.Double(t.getExtent().getWidth(), t.getExtent().getHeight());
				if ((end.getX()-start.getX()) < 0) {
					end.setLocation(end.getX()+360, end.getY());
				}
				double mx = (t.getExtent().getWidth() - t.getExtent().getX()) / 2.0 + t.getExtent().getX();
				double my = (t.getExtent().getY() - t.getExtent().getHeight()) / 2.0
						+ t.getExtent().getHeight();
				Point2D mid = new Point2D.Double(mx, my);
				DecalParameter param = new DecalParameter(start, end, mid, /*equProjection,*/ ppd, t);
				decals.add(param);

			}
		}
		log.aprintln("Discarding "+discardedTiles+" empty tiles.");
	}

	
	public synchronized ArrayList<Decal> getGlobalDecals(String shapeModel) {
		ArrayList<Decal> decals = new ArrayList<>();
		boolean haveDecals = false;
		if (!extents.containsKey(shapeModel, ppd)) {
			haveDecals = buildGlobalExtent();
			if (haveDecals) {
				for (DecalParameter param : extents.get(shapeModel, ppd)) {
					decals.add(new Decal(param));
				}
			}
		} else {
			for (DecalParameter param : extents.get(shapeModel, ppd)) {
				decals.add(new Decal(param));
			}
		}
		return decals;
	}

	/**
	 * @return the equProjection
	 */
	public static ProjObj getEquProjection() {
		getInstance();
		return equProjection;
	}

	/**
	 * @return the polarProjection
	 */
	public static ProjObj getPolarProjection() {
		if (factory == null) {
			factory = new GlobalDecalFactory();
		}
		return polarProjection;
	}
	
	/**
     * Method to generate world coordinates from a list of 3D vectors and an extent
	 * @param verts input array of mapped 3D (X, Y, Z...) vectors/points
	 * @param extent a {@link Rectangle2D} NOTE: mapped as follows X,Y-upper left corner 
	 *        width, height-lower right corner NOT delta values as was intended by the
	 *        authors of Rectangle2D
	 * @return a mapped 2D array (U, V...) of world coordinates
	 */
    public double[] vertsToWorldHVector(float[] verts, Rectangle2D extent, ProjObj proj, boolean shiftLongitude, double shiftAmt) {
        double[] lonlats = new double[verts.length * 2 / 3];
        double latitude = 0.0;
        double longitude = 0.0;
    		int idx = 0;
        HVector temp = new HVector();
        for (int i=0; i<verts.length; i+=3) {
	        	temp = temp.set(verts[i], verts[i+1], verts[i+2]);
	        	latitude = temp.lat();
	        	longitude = shiftLongitude ? (temp.lonE() + shiftAmt) : temp.lonE();
	        	
	        	Point2D spw = proj.convSpatialToWorld(360.0-(longitude), latitude);
        		// Silly special case hack for the case where there is a shared vertex exactly at +/- 90
        		// to eliminate cases where the projection can return seemly random X values, X is set
        		// to the mid-range of the extent
	        	if (shiftLongitude) {
	        		spw = new Point2D.Double(spw.getX() + shiftAmt, spw.getY());
	        	}
        		if ((spw.getX() < extent.getX() || spw.getX() > extent.getWidth()) 
        				&& (FloatUtil.isEqual((float)spw.getY(), 90f, OctTree.MINI_EPSILON /*1e-5f*/) || FloatUtil.isEqual((float)spw.getY(), -90f, OctTree.MINI_EPSILON /*1e-5f*/))) {
        			lonlats[idx++] = ((extent.getWidth() - extent.getX()) * 0.5) + extent.getX();
        		} else {
        			lonlats[idx++] = spw.getX();
        		}
			lonlats[idx++] = spw.getY();      	
        }
        
        return lonlats;
    }
    
    public double[] vertsToWorldHVector(float[] verts, ProjObj proj) {
        double[] lonlats = new double[verts.length * 2 / 3];
        double latitude = 0.0;
        double longitude = 0.0;
    		int idx = 0;
        HVector temp = new HVector();
        
        for (int i=0; i<verts.length; i+=3) {
        	temp = temp.set(verts[i], verts[i+1], verts[i+2]);
        	latitude = temp.lat();
        	// seemingly random use of West-leading Longitude
        	longitude = temp.lonW();
        	
        	Point2D spw = proj.convSpatialToWorld(longitude, latitude);
			lonlats[idx++] = spw.getX();
			lonlats[idx++] = spw.getY();      	
        }
        
        return lonlats;
    }

    /**
     * Method to generate world coordinates from a list of 3D vectors
     * @param verts input array of mapped 3D (X, Y, Z...) vectors/points 
     * @return a mapped 2D array (U, V...) of world coordinates
     */
    public double[] vertsToEquatorialWorldHVector(float[] verts) {
        double[] lonlats = new double[verts.length * 2 / 3];
        double latitude = 0.0;
        double longitude = 0.0;
    		int idx = 0;
        HVector temp = new HVector();
        ProjObj proj = GlobalDecalFactory.getEquProjection();
        
        for (int i=0; i<verts.length; i+=3) {
        	temp = temp.set(verts[i], verts[i+1], verts[i+2]);
        	latitude = temp.lat();
        	// seemingly random use of West-leading Longitude
        	longitude = temp.lonW();
        	
        	Point2D spw = proj.convSpatialToWorld(longitude, latitude);
			lonlats[idx++] = spw.getX();
			lonlats[idx++] = spw.getY();      	
        }
        
        return lonlats;
    }
    public double[] vertsToPolarWorldHVector(float[] verts) {
        double[] lonlats = new double[verts.length * 2 / 3];
        double latitude = 0.0;
        double longitude = 0.0;
    		int idx = 0;
        HVector temp = new HVector();
        ProjObj proj = GlobalDecalFactory.getPolarProjection();
        
        for (int i=0; i<verts.length; i+=3) {
        	temp = temp.set(verts[i], verts[i+1], verts[i+2]);
        	latitude = temp.lat();
        	// seemingly random use of West-leading Longitude
        	longitude = temp.lonW();
        	
        	Point2D spw = proj.convSpatialToWorld(longitude, latitude);
			lonlats[idx++] = spw.getX();
			lonlats[idx++] = spw.getY();      	
        }
        
        return lonlats;
    }

	/**
	 * Method to generate world coordinates from a list of East-leading
	 * longitude/latitude pairs.
	 * @param lonlat mapped array (Lon, Lat...) of Lon/Lat values
	 * @return a mapped 2D array (U, V...) of world coordinated 
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

    
    /**
     * Generic method to release all resources
     */
    public void clearDecalSets() {  
    		if (extents != null) {
	    		for (ArrayList<DecalParameter> dList : extents.getAllItems()) {
	    			for (DecalParameter dp : dList) {
	    				dp.dispose();
	    			}  			
	    		}
    		}
    		extents.clear();
    }

}
