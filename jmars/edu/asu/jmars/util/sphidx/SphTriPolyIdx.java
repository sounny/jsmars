package edu.asu.jmars.util.sphidx;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.asu.jmars.util.HVector;

public class SphTriPolyIdx<T> {
	/** Default maximum depth of the index is {@value} */
	public static final int DEFAULT_MAX_DEPTH=7;
	/** Default maximum overlap-testing depth is {@value} */
	public static final int DEFAULT_MAX_OVERLAP_DEPTH=0;
	/** Default minimum overlap area to consider a full overlap is {@value} */
	public static final double DEFAULT_MAX_OVERLAP_AREA=3.0/4.0;

	/** Number of bits used to specify a triangle within the current level. */
	public static final int locBits = 2;
	public static final int locBitMask = (1<<locBits)-1;
	/** Number of bits used to specify depth of current location in a location descriptor */
	public static final int depthBits = 4;
	public static final int depthBitMask = (1<<depthBits)-1;
	/** Bit-location of the depth value within the location descriptor */
	public static final int depthBitLoc = Integer.SIZE-depthBits;
	
	int maxDepth;
	
	/**
	 * Maximum depth to test for area-overlap testing. A negative or zero value
	 * disables the area overlap testing. A depth of 1 will test only the sub-triangles
	 * of the current spherical-index triangle (being considered) for a full overlap.
	 */
	int maxOverlapDepth;
	/** Minimum approximate overlap area to call the overlap a full overlap. */
	double minOverlapArea;
	
	/**
	 * Map from encoded location to a set of ids.
	 * The encoded location has depth encoded in the highest 4 bits
	 * and 2xdepth bits for location within each depth-level. The
	 * 2-bits (at each depth level) encode for 4 possible triangles
	 * except for depth=1 where there are only two hemispheres.
	 * All locations have at least depth of 2. For convenience, all
	 * triangles at depth 2 or higher have their first coordinate
	 * towards the north/south vertex and the sub-triangles are
	 * designated 0..2 as being coincident with vertices 0..2, while
	 * sub-triangle 3 is the middle sub-triangle (in the opposite
	 * direction). 
	 * @see #locBits
	 * @see #depthBits
	 * @see #depthBitLoc
	 */
	SortedMap<Integer,Set<T>> data;
	
	/** top-level triangles 2-hemispheres, 4-triangles each */
	SphTri topLevelTri[][];
	
	/**
	 * Constructs a spherical-polygon index with default constraints.
	 * @see #SphTriPolyIdx(int, int, double)
	 */
	public SphTriPolyIdx(){
		this(DEFAULT_MAX_DEPTH, DEFAULT_MAX_OVERLAP_DEPTH, DEFAULT_MAX_OVERLAP_AREA);
	}

	/**
	 * Constructs a spherical-polygon index based on the specified constraints.
	 * @param maxDepth Maximum depth of the index, min depth is 2, depth=0 selects
	 *    the hemisphere, depth=1 selects the quadrant within the 
	 *    hemisphere.
	 * @see #SphTriPolyIdx(int, int, double)
	 */
	public SphTriPolyIdx(int maxDepth){
		this(maxDepth, DEFAULT_MAX_OVERLAP_DEPTH, DEFAULT_MAX_OVERLAP_AREA);
	}
	
	/**
	 * Constructs a spherical-polygon index based on the following constraints.
	 * @param maxDepth Maximum depth of the index, min depth is 2, depth=0 selects
	 *    the hemisphere, depth=1 selects the quadrant within the 
	 *    hemisphere.
	 * @param maxOverlapDepth Maximum depth to test for area-overlap testing. A positive value
	 *    enables area overlap testing.
	 * @param minOverlapArea Minimum approximate area of overlap that the index-node must have
	 *    common with the polygon being inserted. This option has no effect when area-overlap
	 *    testing is disabled.
	 * @see #DEFAULT_MAX_DEPTH
	 * @see #DEFAULT_MAX_OVERLAP_DEPTH
	 * @see #DEFAULT_MAX_OVERLAP_AREA
	 */
	public SphTriPolyIdx(int maxDepth, int maxOverlapDepth, double minOverlapArea){
		this.maxDepth = maxDepth;
		this.maxOverlapDepth = maxOverlapDepth;
		this.minOverlapArea = minOverlapArea;
		this.data = new TreeMap<Integer,Set<T>>(new LocComparator());
		this.topLevelTri = new SphTri[2][4];
		for(int h=0; h<2; h++)
			for(int i=0; i<4; i++)
				topLevelTri[h][i] = getTri(2<<depthBitLoc | i<<locBits | h);
	}
	
	public int getMaxDepth(){
		return maxDepth;
	}
	
	public final SphTri getTri(Integer location){
		int loc = location;
		int depth = (loc >> depthBitLoc) & depthBitMask;
		
		if (depth <= 0)
			throw new IllegalArgumentException("Illegal depth value ("+depth+").");
		
		
		int hemisphere = (int)(loc & locBitMask);
		loc >>= locBits; --depth;
		int triNum = (int)(loc & locBitMask);
		loc >>= locBits; --depth;
		
		SphTri tri = null;
		
		switch(hemisphere){
		case 0: // top
			/*
			tri = new SphTri(new HVector[]{
					HVectorUtil.fromSpatialE(triNum    *90, 90),
					HVectorUtil.fromSpatialE(triNum    *90,  0),
					HVectorUtil.fromSpatialE((triNum+1)*90,  0),
			});
			*/
			switch(triNum){
			case 0: tri = new SphTri(new HVector(0,0,1), new HVector(1,0,0), new HVector(0,1,0)); break;
			case 1: tri = new SphTri(new HVector(0,0,1), new HVector(0,1,0), new HVector(-1,0,0)); break;
			case 2: tri = new SphTri(new HVector(0,0,1), new HVector(-1,0,0), new HVector(0,-1,0)); break;
			case 3: tri = new SphTri(new HVector(0,0,1), new HVector(0,-1,0), new HVector(1,0,0)); break;
			}
			break;
		case 1: // bot
			/*
			tri = new SphTri(new HVector[]{
					HVectorUtil.fromSpatialE(triNum    *90,-90),
					HVectorUtil.fromSpatialE((triNum+1)*90,  0),
					HVectorUtil.fromSpatialE(triNum    *90,  0),
			});
			*/
			switch(triNum){
			case 0: tri = new SphTri(new HVector(0,0,-1), new HVector(0,1,0), new HVector(1,0,0)); break;
			case 1: tri = new SphTri(new HVector(0,0,-1), new HVector(-1,0,0), new HVector(0,1,0)); break;
			case 2: tri = new SphTri(new HVector(0,0,-1), new HVector(0,-1,0), new HVector(-1,0,0)); break;
			case 3: tri = new SphTri(new HVector(0,0,-1), new HVector(1,0,0), new HVector(0,-1,0)); break;
			}
			break;
		default:
			throw new IllegalArgumentException("Illegal hemisphere value "+hemisphere);
		}
		
		while(--depth >= 0){
			triNum = (int)(loc & locBitMask);

			HVector[] pts = tri.getPts();
			HVector m02 = pts[0].add(pts[2]).divEq(2).unit();
			HVector m21 = pts[2].add(pts[1]).divEq(2).unit();
			HVector m10 = pts[1].add(pts[0]).divEq(2).unit();

			switch(triNum){
			case 0: tri = new SphTri(pts[0], m10,    m02   ); break;
			case 1: tri = new SphTri(m10,    pts[1], m21   ); break;
			case 2: tri = new SphTri(m02,    m21,    pts[2]); break;
			case 3: tri = new SphTri(m21,    m02,    m10   ); break;
			}
			
			loc >>= locBits;
		}
		
		return tri;
	}
	
	public final SphTri getSubTri(SphTri tri, int triNum){
		HVector[] pts = tri.getPts();
		HVector m02 = pts[0].add(pts[2]).divEq(2).unit();
		HVector m21 = pts[2].add(pts[1]).divEq(2).unit();
		HVector m10 = pts[1].add(pts[0]).divEq(2).unit();

		SphTri subTri = null;
		switch(triNum){
		case 0: subTri = new SphTri(new HVector[]{ pts[0], m10,    m02    }); break;
		case 1: subTri = new SphTri(new HVector[]{ m10,    pts[1], m21    }); break;
		case 2: subTri = new SphTri(new HVector[]{ m02,    m21,    pts[2] }); break;
		case 3: subTri = new SphTri(new HVector[]{ m21,    m02,    m10    }); break;
		}
		return subTri;
	}
	
	/**
	 * Compute the amount of overlap that the given {@link SphPoly} <code>t</code>
	 * has with the index node (represented by the <code>nodeTri</code>).
	 * @param nodeTri Node of the index.
	 * @param t Polygon that is being inserted.
	 * @param location Location of the node in the index.
	 * @param localDepth A value of <code>0</code> means current level only, higher values
	 *        mean how many levels under this node should be checked.
	 * @return
	 */
	private double computeOverlap(SphTri nodeTri, Collection<SphTri> tRelevant, int loc, int localDepth){
		int depth = (loc >> depthBitLoc) & depthBitMask;
		
		double overlap = 0;
		for(int i=0; i<4; i++){
			// Copy location from current level, replace the depth and index
			int subLoc = loc;
			subLoc &= ~(depthBitMask << depthBitLoc);
			subLoc |= (depth+1)<<depthBitLoc;
			subLoc |= i<<(locBits*depth);
			
			SphTri subTri = getSubTri(nodeTri, i);
			Set<SphTri> subtRelevant = null;
			if (SphTri.contains(tRelevant, subTri)){
				overlap += 1/4.0;
			}
			else if (localDepth > 0 && !(subtRelevant = SphTri.intersects(tRelevant, subTri)).isEmpty()){
				overlap += (1/4.0) * computeOverlap(subTri, subtRelevant, subLoc, localDepth-1);
			}
		}
		
		return overlap;
	}
	
	private void recursiveInsert(SphTri nodeTri, Collection<SphTri> tRelevant, T id, int loc){
		int depth = (loc >> depthBitLoc) & depthBitMask;
		
		//System.err.println("loc: "+loc+" "+ decodeLoc(loc)+ " t.contains("+nodeTri+"): "+ t.contains(nodeTri));
		if (depth >= maxDepth || SphTri.contains(tRelevant, nodeTri) || 
				(maxOverlapDepth > 0 && computeOverlap(nodeTri, tRelevant, loc, maxOverlapDepth) > minOverlapArea)){
			// Index node is fully contained within the input triangle "t"
			// so, add "t" here.
			Set<T> ids = data.get(loc);
			if (ids == null)
				data.put(loc, ids = new HashSet<T>());
			ids.add(id);
			return;
		}
		
		boolean allFourCovered = true; // See if all four sub-nodes got covered due to depth constraints
		for(int i=0; i<4; i++){
			// Copy location from current level, replace the depth and index
			int subLoc = loc;
			subLoc &= ~(depthBitMask << depthBitLoc);
			subLoc |= (depth+1)<<depthBitLoc;
			subLoc |= i<<(locBits*depth);
			
			SphTri subTri = getSubTri(nodeTri, i);
			//System.err.println("loc: "+subLoc+" "+ decodeLoc(subLoc)+ " t.intersects("+subTri+"): "+ t.intersects(subTri));
			Set<SphTri> subtRelevant = SphTri.intersects(tRelevant, subTri);
			if (!subtRelevant.isEmpty()) {
				recursiveInsert(subTri, subtRelevant, id, subLoc);
				if (!data.containsKey(subLoc))
					allFourCovered = false;
			}
			else
				allFourCovered = false;
		}
		
		allFourCovered = false;
		if (allFourCovered){ // All four sub-nodes got covered due to max depth constraints
			// Remove polygon from the sub-levels
			for(int i=0; i<4; i++){
				// Copy location from current level, replace the depth and index
				int subLoc = loc;
				subLoc &= ~(depthBitMask << depthBitLoc);
				subLoc |= (depth+1)<<depthBitLoc;
				subLoc |= i<<(locBits*depth);
				
				Set<T> ids = data.get(subLoc);
				if (ids.size() == 1) // Remove sub-node if this was the only object in there
					data.remove(subLoc);
				else
					ids.remove(id);
			}
			
			// Insert polygon at the current level
			Set<T> ids = data.get(loc);
			if (ids == null)
				data.put(loc, ids = new HashSet<T>());
			ids.add(id);
		}
	}
	
	public void insert(SphPoly t, T id){
		for(int h=0; h<2; h++){
			for(int i=0; i<4; i++){
				int loc = 2<<depthBitLoc | (i<<locBits | h);
				SphTri topTri = topLevelTri[h][i];
				
				Set<SphTri> tRelevant = SphTri.intersects(Arrays.asList(t.triangulate()), topTri);
				if (!tRelevant.isEmpty())
					recursiveInsert(topTri, tRelevant, id, loc);
			}
		}
	}

	public Set<Integer> getLocs(){
		return Collections.unmodifiableSet(data.keySet());
	}
	
	public Set<Integer> getLocsFromTop(){
		Set<Integer> filledLocs = data.keySet();
		Set<Integer> locs = new HashSet<Integer>(filledLocs);
		
		for(Integer loc: filledLocs){
			int depth;
			
			while ((depth = (loc >> depthBitLoc) & depthBitMask) > 2){
				loc = parentLoc(loc);
				if (!locs.contains(loc))
					locs.add(loc);
			}
		}
		
		return locs;
	}
	
	public Set<T> getContents(Integer loc){
		return data.get(loc);
	}
	
	private int nextLoc(int loc){
		int depth   = (loc >> depthBitLoc) & depthBitMask;
		int nextLoc = loc;
		boolean carry = false;
		do {
			int i    = (loc >> ((depth-1)*locBits) & locBitMask);
			nextLoc &= ~(depthBitMask << depthBitLoc);
			nextLoc |= (depth << depthBitLoc);
			nextLoc &= ~(locBitMask << ((depth-1)*locBits));
			if ((i+1) > 3){
				i = 0;
				carry = true;
			}
			else {
				carry = false;
			}
			nextLoc |= (i+1) << ((depth-1)*locBits);
			depth--;
		} while(carry);
		
		return nextLoc;
	}
	
	private int parentLoc(int loc){
		int depth     = (loc >> depthBitLoc) & depthBitMask;
		int parentLoc = loc;
		parentLoc    &= ~(depthBitMask << depthBitLoc);
		parentLoc    |= ((depth-1) << depthBitLoc);
		return parentLoc;
	}
	
	public Set<T> find(SphPoly sphPoly, List<SphTri> hit){
		SphTriPolyIdx<SphPoly> idx = new SphTriPolyIdx<SphPoly>(getMaxDepth());
		idx.insert(sphPoly, sphPoly);
		
		Set<Integer> foundLocs = new HashSet<Integer>();
		Set<T> lst = new HashSet<T>();
		
		Set<Integer> findLocSet = idx.getLocs();
		for(Integer findLoc: findLocSet){
			// Find everything that falls in the node corresponding to the
			// current "findLoc" or in its children in the data.
			int depth   = (findLoc >> depthBitLoc) & depthBitMask;
			int nextLoc = nextLoc(findLoc);
			
			SortedMap<Integer,Set<T>> subSet = data.subMap(findLoc, nextLoc); //.tailMap(findLoc).headMap(nextLoc);
			foundLocs.addAll(subSet.keySet());
			for(Set<T> contents: subSet.values())
				lst.addAll(contents);
			
			// Find everything in the nodes above the data node
			// corresponding to the "findLoc"
			while(depth > 2){
				findLoc = parentLoc(findLoc);
				foundLocs.add(findLoc);
				if (data.containsKey(findLoc))
					lst.addAll(data.get(findLoc));
				depth = (findLoc >> depthBitLoc) & depthBitMask;
			}
		}
		
		if (hit != null){
			for(Integer loc: foundLocs)
				hit.add(getTri(loc));
		}
		
		return lst;
	}
	
	/**
	 * Location comparator, compares location by the 2-bit
	 * location encoding used in the index. The bits are compared
	 * from depth 0 through the minimum common depth between the
	 * two locations. If they match upto that point, then the
	 * tie is broken their depths.
	 */
	static class LocComparator implements Comparator<Integer> {
		public int compare(Integer o1, Integer o2) {
			int i1 = o1, i2 = o2;
			int i1Depth = (i1 >> depthBitLoc) & depthBitMask;
			int i2Depth = (i2 >> depthBitLoc) & depthBitMask;
			int minDepth = Math.min(i1Depth, i2Depth);
			
			for(int i=0; i<minDepth; i++){
				int i1Loc = (i1 >> (i*locBits)) & locBitMask;
				int i2Loc = (i2 >> (i*locBits)) & locBitMask;
				int diffLoc = i1Loc-i2Loc;
				
				if (diffLoc != 0)
					return diffLoc;
			}
			
			return i1Depth-i2Depth;
		}
	}
	
	public static String decodeLoc(int location){
		StringBuffer sbuf = new StringBuffer();
		
		int depth = (location >> depthBitLoc) & depthBitMask;
		sbuf.append("d="); sbuf.append(depth); sbuf.append(" loc=");
		for(int i=0; i<depth; i++){
			if (i > 0)
				sbuf.append(".");
			int loc = (location >> (i*locBits)) & locBitMask;
			sbuf.append(loc);
		}
		return sbuf.toString();
	}
	
	public static List<Point2D> toLatLon(List<HVector> vecs){
		List<Point2D> pts = new ArrayList<Point2D>();
		for(HVector v: vecs){
			Point2D pt = new Point2D.Double();
			v.toLonLat(pt);
			pts.add(pt);
		}
		return pts;
	}
	
	public static SphPoly[] getSearchAreas(){
		SphPoly r1 = new SphPoly(new HVector[]{
				HVectorUtil.fromSpatialE(0,80),
				HVectorUtil.fromSpatialE(90,80),
				HVectorUtil.fromSpatialE(180,80),
				HVectorUtil.fromSpatialE(270,80),
		});
		
		SphPoly r2 = new SphPoly(new HVector[]{
				HVectorUtil.fromSpatialE(5,5),
				HVectorUtil.fromSpatialE(25,5),
				HVectorUtil.fromSpatialE(25,25),
				HVectorUtil.fromSpatialE(5,25),
		});
		
		return new SphPoly[]{ r1, r2 };
	}
	
	public static SphPoly[] getPolysToBeIndexed(){
		SphPoly poly = new SphPoly(new HVector[]{
				HVectorUtil.fromSpatialE(0, 0),
				HVectorUtil.fromSpatialE(10, 0),
				HVectorUtil.fromSpatialE(10, 10),
				HVectorUtil.fromSpatialE(5, 10),
				HVectorUtil.fromSpatialE(0, 10),
				HVectorUtil.fromSpatialE(1, 9),
				HVectorUtil.fromSpatialE(0, 8),
				HVectorUtil.fromSpatialE(2, 7),
				HVectorUtil.fromSpatialE(0, 6),
				HVectorUtil.fromSpatialE(1, 5),
				HVectorUtil.fromSpatialE(0, 4),
				HVectorUtil.fromSpatialE(1, 3),
				HVectorUtil.fromSpatialE(0, 2),
				HVectorUtil.fromSpatialE(1, 1),
		});
		
		SphPoly poly2 = new SphPoly(new HVector[]{
			HVectorUtil.fromSpatialE(6,0),
			HVectorUtil.fromSpatialE(7,1),
			HVectorUtil.fromSpatialE(5,3),
			HVectorUtil.fromSpatialE(9,1),
			HVectorUtil.fromSpatialE(10,5),
			HVectorUtil.fromSpatialE(11,1),
			HVectorUtil.fromSpatialE(15,3),
			HVectorUtil.fromSpatialE(13,1),
			HVectorUtil.fromSpatialE(14,0),
			HVectorUtil.fromSpatialE(16,4),
			HVectorUtil.fromSpatialE(12,2),
			HVectorUtil.fromSpatialE(10,6),
			HVectorUtil.fromSpatialE(8,2),
			HVectorUtil.fromSpatialE(4,4),
		});
		
		SphPoly poly3 = new SphPoly(new HVector[]{
				HVectorUtil.fromSpatialE(0,0),
				HVectorUtil.fromSpatialE(45,0),
				HVectorUtil.fromSpatialE(0,45),
		});
		
		SphPoly poly4 = new SphPoly(new HVector[]{
				HVectorUtil.fromSpatialE(0, 85),
				HVectorUtil.fromSpatialE(90, 85),
				HVectorUtil.fromSpatialE(90, 90)
		});
		
		/* flipped / clock-wise polys */
		SphPoly poly40 = new SphPoly(new HVector[]{
				HVectorUtil.fromSpatialE(5,5),
				HVectorUtil.fromSpatialE(40,5),
				HVectorUtil.fromSpatialE(5,40),
		});
		
		SphPoly poly50 = new SphPoly(new HVector[]{
				HVectorUtil.fromSpatialE(4,4),
				HVectorUtil.fromSpatialE(8,2),
				HVectorUtil.fromSpatialE(10,6),
				HVectorUtil.fromSpatialE(12,2),
				HVectorUtil.fromSpatialE(16,4),
				HVectorUtil.fromSpatialE(14,0),
				HVectorUtil.fromSpatialE(13,1),
				HVectorUtil.fromSpatialE(15,3),
				HVectorUtil.fromSpatialE(11,1),
				HVectorUtil.fromSpatialE(10,5),
				HVectorUtil.fromSpatialE(9,1),
				HVectorUtil.fromSpatialE(5,3),
				HVectorUtil.fromSpatialE(7,1),
				HVectorUtil.fromSpatialE(6,0),
			});
		
		SphPoly poly60 = new SphPoly(new HVector[]{
				HVectorUtil.fromSpatialE(5,40),
				HVectorUtil.fromSpatialE(40,5),
				HVectorUtil.fromSpatialE(5,5),
		});

		SphPoly[] polys = new SphPoly[]{
				poly, poly2, poly3, poly4, 
		};
		
		return polys;
	}
	
	public static SphPoly[] loadPolys(String fileName) throws IOException, FileNotFoundException {
		List<SphPoly> lst = new ArrayList<SphPoly>();
		BufferedReader rdr = new BufferedReader(new FileReader(fileName));
		
		String line;
		int lineNum = 0;
		while((line = rdr.readLine()) != null){
			lineNum++;
			line = line.trim();
			if (!line.contains("POLYGON"))
				continue;
			
			String col[] = line.split("\\s*\\|\\s*");
			col[2] = col[2].replaceAll("POLYGON\\(\\(|\\)\\)", "");
			String ptPcs[] = col[2].split("\\s*,\\s*");
			HVector pts[] = new HVector[ptPcs.length-1]; // first point is repeated as last point
			for(int i=0; i<pts.length; i++){
				String[] pcs = ptPcs[i].split("\\s+");
				pts[i] = HVectorUtil.fromSpatialE(Double.parseDouble(pcs[0]), Double.parseDouble(pcs[1]));
			}
			/*
			int lturns = 0;
			for(int i=0; i<pts.length; i++){
				if (pts[i].cross(pts[(i+2)%pts.length]).dot(pts[(i+1)%pts.length]) < 0)
					lturns++;
				else 
					lturns--;
			}
			if (lturns < 0){
			*/
			if (col[1].trim().equals("(0.0,1.0,0.0)")){
				// clock-wise polygon, flip points
				for(int i=0; i<(pts.length/2); i++){
					HVector t = pts[i];
					pts[i] = pts[pts.length-1-i];
					pts[pts.length-1-i] = t;
				}
			}
			lst.add(new SphPoly(pts));
		}
		
		return lst.toArray(new SphPoly[0]);
	}
	
	public static void main(String[] args) throws Exception {
		//SphPoly[] polys = getPolysToBeIndexed();
		SphPoly[] polys = SphTriPolyIdx.loadPolys("/u/saadat/test2.txt");
		SphPoly[] subset = new SphPoly[Math.min(polys.length, 10)];
		System.arraycopy(polys, 30, subset, 0, subset.length);
		polys = subset;

		SphTriPolyIdx<SphPoly> idx = new SphTriPolyIdx<SphPoly>();
		long t0 = System.currentTimeMillis();
		for(int i=0; i<polys.length; i++){
			if (i%100 == 0)
				System.err.format("%6.2g in %d ms.\n", (i*100.0/polys.length), (System.currentTimeMillis()-t0));
			idx.insert(polys[i], polys[i]);
		}
		long t1 = System.currentTimeMillis();
		System.err.format("Indexed %d polygons in %d ms.\n", polys.length, (t1-t0));
		
		SphPoly[] searchPolys = getSearchAreas();
		long searchTime = 0;
		for(int i=0; i<searchPolys.length; i++){
			t0 = System.currentTimeMillis();
			List<SphTri> hit = new ArrayList<SphTri>();
			Set<SphPoly> found = idx.find(searchPolys[i], hit);
			t1 = System.currentTimeMillis();
			searchTime += (t1-t0);

			System.out.println(searchPolys[i]+"=>"+found.size()+" polys, "+hit.size()+" idx-nodes hit.");
		}
	}
}
