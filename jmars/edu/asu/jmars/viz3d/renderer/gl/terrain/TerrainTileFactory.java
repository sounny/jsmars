package edu.asu.jmars.viz3d.renderer.gl.terrain;


import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.MultiKeyHashMap;
import edu.asu.jmars.viz3d.ThreeDException;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.asu.jmars.viz3d.core.geometry.OctTree;
import edu.asu.jmars.viz3d.core.geometry.TriangleMesh;

/**
 * Class to provide a singleton TerrainTile Factory of sorts
 *
 */
public class TerrainTileFactory {
	
	private MultiKeyHashMap<String, Integer, TerrainGrid> grids = new MultiKeyHashMap<String, Integer, TerrainGrid>();
	
	private static DebugLog log = DebugLog.instance();
	
	/**
	 * Default 8 PPD constructor
	 * TODO should go away in the near future
	 * @throws ThreeDException
	 */
	public TerrainTileFactory() throws ThreeDException {
		ThreeDManager mgr = ThreeDManager.getInstance();
		if (mgr.hasShapeModel()) {
			TriangleMesh mesh = mgr.getShapeModel();
			OctTree oct = mesh.getOctTree();
			TerrainGrid grid = new TerrainGrid(8, oct);
			grids.put(mesh.getMeshName(), 8, grid);
		} else {
			log.println("Unable to initialize new TerrainTileFactory without a Shape Model.");
			throw new ThreeDException("Unable to initialize new TerrainTileFactory without a Shape Model.");
		}
	}

	/**
	 * Primary preferred constructor
	 * @param ppd Pixels per Degree to be used in sizing tiles MUST be a power of 2
	 * @throws ThreeDException
	 * @throws IllegalArgumentException
	 */
	public TerrainTileFactory(int ppd) throws ThreeDException, IllegalArgumentException {
		if (ppd != 2 && !isPowerOf2(ppd)) {
			log.println("Cannot create a TerrainGrid with PPD  ("+ppd+") that is not a power of 2!");
			throw new IllegalArgumentException("Cannot create a TerrainGrid with PPD ("+ppd+") that is not a power of 2!");
		}

		ThreeDManager mgr = ThreeDManager.getInstance();
		if (mgr.hasShapeModel()) {
			TriangleMesh mesh = mgr.getShapeModel();
			OctTree oct = mesh.getOctTree();
			TerrainGrid grid = new TerrainGrid(ppd, oct);
//			grid.printGrid();
			grids.put(mesh.getMeshName(), ppd, grid);
		} else {
			log.println("Unable to initialize new TerrainTileFactory without a Shape Model.");
			throw new ThreeDException("Unable to initialize new TerrainTileFactory without a Shape Model.");
		}
	}
	
	/**
	 * Method to return the TerrainTile grid for a specific PPD
	 * @param ppd Pixels per Degree
	 * @return 2D array of TerrainTiles
	 * @throws IllegalArgumentException
	 */
	public TerrainGrid getGrid(String shapeModel, int ppd) throws IllegalArgumentException {
		if (ppd != 2 && !isPowerOf2(ppd)) {
			log.println("Cannot create a TerrainGrid with PPD  ("+ppd+") that is not a power of 2!");
			throw new IllegalArgumentException("Cannot create a TerrainGrid with PPD ("+ppd+") that is not a power of 2!");
		}
		
		if (grids.containsKey(shapeModel, ppd)) {
			return grids.get(shapeModel, ppd);
		}
		else {
			return makeGrid(ppd);
		}
	}
	
	private TerrainGrid makeGrid(int ppd) {
		ThreeDManager mgr = ThreeDManager.getInstance();
		if (mgr.hasShapeModel()) {
			TriangleMesh mesh = mgr.getShapeModel();
			OctTree oct = mesh.getOctTree();
			TerrainGrid grid = new TerrainGrid(ppd, oct);
			grids.put(mesh.getMeshName(), ppd, grid);
			return grid;
		} else {
			return null;
		}
	}
	
	private static boolean isPowerOf2(int i) {
		return i > 2 && ((i&-i)==i);
	}	
}
