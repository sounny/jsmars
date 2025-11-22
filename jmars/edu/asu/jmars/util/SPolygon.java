package edu.asu.jmars.util;

import gnu.trove.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;

import java.util.List;

public final class SPolygon
 {
    private static final DebugLog log = DebugLog.instance();

    private static final HVector[] HVECTOR_0 = new HVector[0];
    private static final HalfPolygon[] HALFPOLYGON_0 = new HalfPolygon[0];
    private static final SPolygon[] SPOLYGON_0 = new SPolygon[0];

    /**
     ** The amount of area returned for intersections that do in fact
     ** exist, but whose area calculations fall through due to chance
     ** degenerate geometry details (such as coincident vertices).
     **/
    private static final double NO_AREA = 0.01;

    /**
     ** The tolerance for slightly negative areas (seems to occur in
     ** semi-degenerate "glancing" or "tangent" intersections... rare but
     ** not an error). Such areas are converted to positive values.
     **/
    private static final double ZERO_TOL = 1e-6;

    public static HVector newHVector(double lon, double lat)
     {
	lon = Math.toRadians(lon);
	lat = Math.toRadians(lat);
	return  new HVector(Math.cos(lat) * Math.cos(lon),
			    Math.cos(lat) * Math.sin(lon),
			    Math.sin(lat));
     }

    /**
     ** Provides the interpolation of a and b in a modulo 360 space,
     ** spanning from r=0 at the a side of the minimum segment from a
     ** to b.
     **
     ** <ul>
     ** <li>r=0 returns a
     ** <li>r=0.5 return average360(a, b)
     ** <li>r=1 returns b
     **
     ** <p>Assumes that fabs(a-b)<=720 [DESPITE the 360*3 in the
     ** implementation].
     **/
    public static double interpolate360(double a, double b, double r)
     {
	if(increasing360(a, b)) return  (a + (b-a+360*3)%360 * r) % 360;
	else                    return  (b + (a-b+360*3)%360 * r) % 360;
     }

    /**
     ** Provides the average of a and b in a modulo 360 space, that is
     ** within the minimum segment from a to b.
     **
     ** <p>The expression (a+b)/2 is not sufficient for this. For
     ** example, it would fail for as an attempt at the average of 1
     ** and 359, which should be zero, but would instead come out as
     ** 180.
     **
     ** <p>Assumes that fabs(a-b)<=720 [DESPITE the 360*3 in the
     ** implementation].
     **/
    private static double average360(double a, double b)
     {
	if(increasing360(a, b)) return  (a + (b-a+360*3)%360 / 2) % 360;
	else                    return  (b + (a-b+360*3)%360 / 2) % 360;
     }

    /**
     ** If the shortest path from a to b (modulo 360) is in the
     ** increasing direction, returns 1. For the opposite direction,
     ** returns 0. If a==b, returns 1.
     **
     ** Excluding the modulo 360, this is logistically equivalent to a<=b.
     **
     ** Assumes that fabs(a-b)<=720 [DESPITE the 360*3 in the
     ** implementation].
     **/
    private static boolean increasing360(double a, double b)
     {
	return  (b - a + 360*3) % 360 < 180;
     }

    /**
     ** If the shortest path from a to b (modulo 360) is in the
     ** decreasing direction, returns 1. For the opposite direction,
     ** returns 0. If a==b, returns 1.
     **
     ** Excluding the modulo 360, this is logistically equivalent to a>=b.
     **
     ** Assumes that fabs(a-b)<=720 [DESPITE the 360*3 in the
     ** implementation].
     **
     ** This function does _NOT_ actually return !increasing360(a,b)
     **/
    private static boolean decreasing360(double a, double b)
     {
	return  (a - b + 360*3) % 360 < 180;
     }

    /**
     ** If the two 1-dimensional ranges [a,b] and [e,f] overlap (modulo
     ** 360), returns 1. Otherwise, returns 0.
     **
     ** Excluding the modulo (and potential direction inversion), this is
     ** logistically equivalent to a<=f && e<=b.
     **/
    public static boolean overlap360(double a, double b, double e, double f)
     {
	return  increasing360(a, f)
	    ==  increasing360(e, b);
     }

    /**
     ** Returns, as lon/lat, the approximate centroid of the polygon
     ** (really just the average of all the vertices).
     **/
    public double[] getCentroidLL()
     {
	HVector sum = new HVector();
	for(int i=0; i<v.length; i++)
	    sum.addEq(v[i]);
	sum.normalize();
	return  new double[] { sum.lon(), sum.lat() };
     }

    public static SPolygon[][] createBinOutlines(final int LONBIN_COUNT,
						 final int LATBIN_COUNT)
     {
	final int LONBIN_SZ = (360/LONBIN_COUNT);
	final int LATBIN_SZ = (180/LATBIN_COUNT);

	SPolygon[][] binOuts = new SPolygon[LONBIN_COUNT][LATBIN_COUNT];
	for(int i=0; i<LONBIN_COUNT; i++)
	    for(int j=0; j<LATBIN_COUNT; j++)
	     {
		double minLon = LONBIN_SZ *  i  ;
		double maxLon = LONBIN_SZ * (i+1);
		double midLon = (minLon + maxLon) / 2;
    
		double minLat = LATBIN_SZ *  j    - 90;
		double maxLat = LATBIN_SZ * (j+1) - 90;
		double midLat = Math.min(Math.abs(minLat),
					 Math.abs(maxLat));
		midLat = Math.asin(
		    newHVector(minLon+180, 90-midLat).cross(
			newHVector(maxLon+180, 90-midLat) ).unit().z
		    );
		midLat = Math.abs(Math.toDegrees(midLat));
    
		SPolygon p = binOuts[i][j] = new SPolygon();
    
		// Bottom half
		p                         .addPoint(minLon,  minLat);
		if(j > LATBIN_COUNT/2  ) p.addPoint(midLon,  midLat); // N
		if(j != 0              ) p.addPoint(maxLon,  minLat);
    
		// Top half
		p                         .addPoint(maxLon,  maxLat);
		if(j < LATBIN_COUNT/2-1) p.addPoint(midLon, -midLat); // S
		if(j != LATBIN_COUNT-1 ) p.addPoint(minLon,  maxLat);
    
		p.initClassic();
		p.name = "BIN " + i + " " + j;
	     }
	return  binOuts;
     }

    public static final class Index
     {
	private final SPolygon[][] binOutlines;

	public final int LONBIN_COUNT;
	public final int LATBIN_COUNT;
	public final int LONBIN_SZ;
	public final int LATBIN_SZ;

	SPolygon[][][] bins;

	public Index(List polys, int LONBIN_COUNT, int LATBIN_COUNT)
	 {
	    if(360 % LONBIN_COUNT != 0  ||
	       180 % LATBIN_COUNT != 0  ||
	       LONBIN_COUNT % 2 != 0  ||
	       LATBIN_COUNT % 2 != 0)
		throw  new IllegalArgumentException(
		    "The lon/lat bin counts must be even numbers!");

	    this.LONBIN_COUNT = LONBIN_COUNT;
	    this.LATBIN_COUNT = LATBIN_COUNT;
	    this.LONBIN_SZ = (360/LONBIN_COUNT);
	    this.LATBIN_SZ = (180/LATBIN_COUNT);

	    binOutlines = createBinOutlines(LONBIN_COUNT, LATBIN_COUNT);
	    bins = new SPolygon[LONBIN_COUNT][LATBIN_COUNT][];

	    List[][] binsTmp = new List[LONBIN_COUNT][LATBIN_COUNT];
	    for(Iterator i=polys.iterator(); i.hasNext(); )
		add(binsTmp, (SPolygon) i.next());

	    for(int i=0; i<bins.length; i++)
		for(int j=0; j<bins[i].length; j++)
			bins[i][j] =
			    binsTmp[i][j] == null
			    ? SPOLYGON_0
			    : (SPolygon[]) binsTmp[i][j].toArray(SPOLYGON_0);
	 }

	private void add(List[][] binsTmp, SPolygon p)
	 {
	    Point[] fallsIn = findBins(p);
	    
	    for(int k=0; k<fallsIn.length; k++){
	    	int xx = fallsIn[k].x;
	    	int y = fallsIn[k].y;
	    	if (binsTmp[xx][y] == null)
	    		binsTmp[xx][y] = new ArrayList();
	    	binsTmp[xx][y].add(p);
	    }
	 }
	
	/**
	 * Add an SPolygon to the existing index. Not an efficient way of doing things.
	 */
	public void add(SPolygon p){
		Point[] fallsIn = findBins(p);
		
		for(int k=0; k<fallsIn.length; k++){
			int i = fallsIn[k].x;
			int j = fallsIn[k].y;
			
			if (bins[i][j] == SPOLYGON_0){
				bins[i][j] = new SPolygon[] { p };
			}
			else {
				SPolygon[] newArray = new SPolygon[bins[i][j].length+1];
				System.arraycopy(bins[i][j], 0, newArray, 0, newArray.length-1);
				newArray[newArray.length-1] = p;
				bins[i][j] = newArray;
			}
		}
	}

	/**
	 * Find bins that the specified SPolygon falls into.
	 * 
	 * @param p Non-null SPolygon.
	 * @return An array of (x,y) coordinates as that this polygon falls into.
	 *         The (x,y) will be used to index bins as bins[x][y].
	 */
	public Point[] findBins(SPolygon p){
		if(p.self_intersecting)
			return new Point[0];

		int minx = (int) Math.floor(p.minLon / LONBIN_SZ);
		int maxx = (int) Math.floor(p.maxLon / LONBIN_SZ);
		if(maxx < minx) maxx += LONBIN_COUNT;
		int miny = (int) Math.floor(p.minLat / LATBIN_SZ) + LATBIN_COUNT/2;
		int maxy = (int) Math.floor(p.maxLat / LATBIN_SZ) + LATBIN_COUNT/2;
		if(miny >= LATBIN_COUNT) miny = LATBIN_COUNT-1;
		if(maxy == LATBIN_COUNT) maxy = LATBIN_COUNT-1;

		List binIndices = new LinkedList();

		if(minx == maxx  ||  miny == maxy){
			for(int x=minx; x<=maxx; x++){
				int xx = x % LONBIN_COUNT;
				for(int y=miny; y<=maxy; y++)
					binIndices.add(new Point(xx,y));
			}
		}
		else {
			for(int x=minx; x<=maxx; x++){
				int xx = x % LONBIN_COUNT;
				for(int y=miny; y<=maxy; y++)
					if(hitTest(binOutlines[xx][y], p))
						binIndices.add(new Point(xx,y));
			}
		}
		
		return (Point[])binIndices.toArray(new Point[0]);
	}
	
	/**
	 ** Returns an array of arrays of co-overlapping polygons.
	 ** Singletons are excluded from this list, and there are
	 ** never any nulls.
	 **/
	public SPolygon[][] findOverlappingPolys()
	 {
	    Map poly2group = new IdentityHashMap(); // poly -> set of poly
	    TLongHashSet checked = new TLongHashSet();
	    for(int x=0; x<LONBIN_COUNT; x++)
		for(int y=0; y<LATBIN_COUNT; y++)
		    for(int i=bins[x][y].length-1; i>0; i--) // i>=0 SLOWER
		     {
			SPolygon p = bins[x][y][i];
			for(int j=i-1; j>=0; j--)
			 {
			    SPolygon q = bins[x][y][j];
			    long id = p.uid < q.uid
				? (((long)p.uid<<32) | q.uid)
				: (((long)q.uid<<32) | p.uid);
			    if(checked.contains(id))
				continue;

			    checked.add(id);
			    Set groupP = (Set) poly2group.get(p);
			    Set groupQ = (Set) poly2group.get(q);

			    if((groupP != null  &&  groupP.contains(q))  ||
			       (groupQ != null  &&  groupQ.contains(p))  ||
			       hitTest(p, q))
			     {
				if(groupP == null  &&  groupQ == null)
				 {
				    // Create a group for both
				    groupP = new HashSet();
				    groupP.add(q);
				    groupP.add(p);
				    poly2group.put(p, groupP);
				    poly2group.put(q, groupP);
				 }
				// Unless they already share an
				// overlap, they will have
				// different groups.
				else if(groupP != groupQ)
				 {
				    // P isn't grouped yet, map it to Q's
				    if(groupP == null)
				     {
					poly2group.put(p, groupQ);
					groupQ.add(p);
				     }
				    // Q isn't grouped yet, map it to P's
				    else if(groupQ == null)
				     {
					poly2group.put(q, groupP);
					groupP.add(q);
				     }
				    // They're both grouped, merge them.
				    else
				     {
					// P is bigger, add/map Q to it.
					if(groupP.size() > groupQ.size())
					 {
					    groupP.addAll(groupQ);
					    poly2group.put(q, groupP);
					 }
					// Q is bigger, add/map P to it.
					else
					 {
					    groupQ.addAll(groupP);
					    poly2group.put(p, groupQ);
					 }
				     }
				 }
			     }
			 }
		     }

	    Set groups = new HashSet(poly2group.values());
	    SPolygon[][] polys = new SPolygon[groups.size()][];
	    Iterator iter = groups.iterator();
	    for(int i=0; i<polys.length; i++)
	     {
		Set group = (Set) iter.next();
		group.toArray(polys[i] = new SPolygon[group.size()]);
	     }

	    return  polys;
	 }

	/**
	 * Returns dynamically-allocated array of polygon intersections
	 * between the specified polygon list and polygon index, i.e.,
	 * these should be separate data sets.
	 *
	 * Within each intersection, the first polygon is from the
	 * polygon array while the second is from the polygon index.
	 */
	public List findIntersections(List p1)
	 {
	    List intersections = new ArrayList(); //Intersection

	    // Perform the hit tests
	    int progress = 0;
	    int next_print = 0;
	    int count_opt = 0;
	    int count_not = 0;
	    List hits; //SPolygon
    
	    for(int i=0; i<p1.size(); i++)
	     {
		progress = (int) (100 * i / (double) p1.size());
		if(progress >= next_print)
		 {
		    int optimized = count_opt+count_not!=0
			? (int)(100 * count_opt/(double)(count_opt+count_not))
			: 0;
		    announce_update(progress + "% " + optimized + "%o");
		    ++next_print;
		 }
     
		SPolygon p = (SPolygon) p1.get(i);
		int minx = (int) Math.floor(p.minLon / LONBIN_SZ);
		int maxx = (int) Math.floor(p.maxLon / LONBIN_SZ);
		if(maxx < minx) maxx += LONBIN_COUNT;
		int miny = (int)Math.floor(p.minLat/LATBIN_SZ)+LATBIN_COUNT/2;
		int maxy = (int)Math.floor(p.maxLat/LATBIN_SZ)+LATBIN_COUNT/2;
		if(miny >= LATBIN_COUNT) miny = LATBIN_COUNT-1;
		if(maxy == LATBIN_COUNT) maxy = LATBIN_COUNT-1;

		// Optimized case: the poly in p1 covers only a single bin
		if(minx == maxx  &&  miny == maxy)
		 {
		    ++count_opt;
		    SPolygon[] bin = bins[minx][miny];
		    for(int j=0; j<bin.length; j++)
			if(hitTest(p, bin[j]))
			 {
			    Intersection hit = new Intersection(p, bin[j]);
			    intersections.add(hit);
			 }
		 }
		// Non-optimized: poly in p1 covers multiple bins, gotta merge
		else
		 {
		    ++count_not;

		    int[][] idx = new int[LONBIN_COUNT][LATBIN_COUNT];

		    // Might really only intersect some bins in the range
		    if(minx != maxx  &&  miny != maxy)
			for(int x=minx; x<=maxx; x++)
			    for(int y=miny; y<=maxy; y++)
			     {
				int xx = x % LONBIN_COUNT;
				if(!hitTest(p, binOutlines[xx][y]))
				    idx[xx][y] = bins[xx][y].length;
			     }

		    for(;;)
		     {
			// Pick the next bin
			SPolygon best = null;
			for(int x=minx; x<=maxx; x++)
			    for(int y=miny; y<=maxy; y++)
			     {
				int xx = x % LONBIN_COUNT;
				if(idx[xx][y] < bins[xx][y].length)
				 {
				    SPolygon b = bins[xx][y][idx[xx][y]];
				    if(best == null  ||  best.uid > b.uid)
					best = b;
				 }
			     }
			// No more polys to choose from, we're done looping
			if(best == null)
			    break;
			// We've got our "best" poly, now pop it from all bins
			for(int x=minx; x<=maxx; x++)
			    for(int y=miny; y<=maxy; y++)
			     {
				int xx = x % LONBIN_COUNT;
				if(idx[xx][y] < bins[xx][y].length)
				 {
				    SPolygon b = bins[xx][y][idx[xx][y]];
				    if(best == b)
					idx[xx][y]++;
				 }
			     }
			// FINALLY, hit test our best poly
			if(hitTest(p, best))
			 {
			    Intersection hit = new Intersection(p, best);
			    intersections.add(hit);
			 }
		     }
		 }
	     }
    
	    return intersections;
	 }

	/**
	 * Returns dynamically-allocated array of polygons from the
	 * index intersecting the specified polygon.
	 */
	public List findIntersections(SPolygon p, List intersections)
	 {
	    if(intersections == null)
		intersections = new ArrayList();

	    // Perform the hit tests
	    int minx = (int) Math.floor(p.minLon / LONBIN_SZ);
	    int maxx = (int) Math.floor(p.maxLon / LONBIN_SZ);
	    if(maxx < minx) maxx += LONBIN_COUNT;
	    int miny = (int)Math.floor(p.minLat/LATBIN_SZ)+LATBIN_COUNT/2;
	    int maxy = (int)Math.floor(p.maxLat/LATBIN_SZ)+LATBIN_COUNT/2;
	    if(miny >= LATBIN_COUNT) miny = LATBIN_COUNT-1;
	    if(maxy == LATBIN_COUNT) maxy = LATBIN_COUNT-1;

	    // Optimized case: the poly in p1 covers only a single bin
	    if(minx == maxx  &&  miny == maxy)
	     {
		SPolygon[] bin = bins[minx][miny];
		for(int j=0; j<bin.length; j++)
		    if(hitTest(p, bin[j]))
			intersections.add(bin[j]);
	     }
	    // Non-optimized: poly in p1 covers multiple bins, gotta merge
	    else
	     {
		int[][] idx = new int[LONBIN_COUNT][LATBIN_COUNT];

		// Might really only intersect some bins in the range
		if(minx != maxx  &&  miny != maxy)
		    for(int x=minx; x<=maxx; x++)
			for(int y=miny; y<=maxy; y++)
			 {
			    int xx = x % LONBIN_COUNT;
			    if(!hitTest(p, binOutlines[xx][y]))
				idx[xx][y] = bins[xx][y].length;
			 }

		for(;;)
		 {
		    // Pick the next bin
		    SPolygon best = null;
		    for(int x=minx; x<=maxx; x++)
			for(int y=miny; y<=maxy; y++)
			 {
			    int xx = x % LONBIN_COUNT;
			    if(idx[xx][y] < bins[xx][y].length)
			     {
				SPolygon b = bins[xx][y][idx[xx][y]];
				if(best == null  ||  best.uid > b.uid)
				    best = b;
			     }
			 }
		    // No more polys to choose from, we're done looping
		    if(best == null)
			break;
		    // We've got our "best" poly, now pop it from all bins
		    for(int x=minx; x<=maxx; x++)
			for(int y=miny; y<=maxy; y++)
			 {
			    int xx = x % LONBIN_COUNT;
			    if(idx[xx][y] < bins[xx][y].length)
			     {
				SPolygon b = bins[xx][y][idx[xx][y]];
				if(best == b)
				    idx[xx][y]++;
			     }
			 }
		    // FINALLY, hit test our best poly
		    if(hitTest(p, best))
			intersections.add(best);
		 }
	     }
    
	    return intersections;
	 }

	/**
	 * Returns dynamically-allocated array of polygons from the
	 * index intersecting the specified lon/lat line.
	 */
	public List findIntersections(Line2D lonlatLine, List intersections)
	 {
	    if(intersections == null)
		intersections = new ArrayList();

	    SPolygon p = new SPolygon(lonlatLine);

	    // Perform the hit tests
	    int minx = (int) Math.floor(p.minLon / LONBIN_SZ);
	    int maxx = (int) Math.floor(p.maxLon / LONBIN_SZ);
	    if(maxx < minx) maxx += LONBIN_COUNT;
	    int miny = (int)Math.floor(p.minLat/LATBIN_SZ)+LATBIN_COUNT/2;
	    int maxy = (int)Math.floor(p.maxLat/LATBIN_SZ)+LATBIN_COUNT/2;
	    if(miny >= LATBIN_COUNT) miny = LATBIN_COUNT-1;
	    if(maxy == LATBIN_COUNT) maxy = LATBIN_COUNT-1;

	    // Optimized case: the poly in p1 covers only a single bin
	    if(minx == maxx  &&  miny == maxy)
	     {
		SPolygon[] bin = bins[minx][miny];
		for(int j=0; j<bin.length; j++)
		    if(hitTestLine(p, bin[j]))
			intersections.add(bin[j]);
	     }
	    // Non-optimized: poly in p1 covers multiple bins, gotta merge
	    else
	     {
		int[][] idx = new int[LONBIN_COUNT][LATBIN_COUNT];

		// Might really only intersect some bins in the range
		if(minx != maxx  &&  miny != maxy)
		    for(int x=minx; x<=maxx; x++)
			for(int y=miny; y<=maxy; y++)
			 {
			    int xx = x % LONBIN_COUNT;
			    if(!hitTestLine(p, binOutlines[xx][y]))
				idx[xx][y] = bins[xx][y].length;
			 }

		for(;;)
		 {
		    // Pick the next bin
		    SPolygon best = null;
		    for(int x=minx; x<=maxx; x++)
			for(int y=miny; y<=maxy; y++)
			 {
			    int xx = x % LONBIN_COUNT;
			    if(idx[xx][y] < bins[xx][y].length)
			     {
				SPolygon b = bins[xx][y][idx[xx][y]];
				if(best == null  ||  best.uid > b.uid)
				    best = b;
			     }
			 }
		    // No more polys to choose from, we're done looping
		    if(best == null)
			break;
		    // We've got our "best" poly, now pop it from all bins
		    for(int x=minx; x<=maxx; x++)
			for(int y=miny; y<=maxy; y++)
			 {
			    int xx = x % LONBIN_COUNT;
			    if(idx[xx][y] < bins[xx][y].length)
			     {
				SPolygon b = bins[xx][y][idx[xx][y]];
				if(best == b)
				    idx[xx][y]++;
			     }
			 }
		    // FINALLY, hit test our best poly
		    if(hitTestLine(p, best))
			intersections.add(best);
		 }
	     }

	    return intersections;
	 }
     }

    // Functions - polygons

    /**
     ** Returns the absolute area of intersection between p1 and p2 on a
     ** unit sphere.
     **/
    public static double area(SPolygon p1, SPolygon p2)
     {
	// Holds the number of intersection points found so far.
	int found = 0;
	// Holds the sum of the interior angles of the intersections.
	double angles = 0;

	// Check for intersections between p1 and p2
	for(int x=0; x<p1.h.length; x++)
	    for(int y=0; y<p2.h.length; y++)
	     {
		HalfPolygon h1 = p1.h[x];
		HalfPolygon h2 = p2.h[y];
		int i = h1.n-2;
		int j = h2.n-2;
    
		while(i >= 0  &&  j >= 0)
		 {
		    if(overlap360(h1.lons[i], h1.lons[i+1],
				  h2.lons[j], h2.lons[j+1]))
			if(edgesIntersect(h1.v[i], h1.v[i+1], h1.u[i],
					  h2.v[j], h2.v[j+1], h2.u[j]))
			 {
			    double a = Math.acos(h1.u[i].dot(h2.u[j]));
			    if(h1.inside == h2.inside) a = Math.PI - a;

			    found++;
			    angles += a;
			 }
    
		    if(decreasing360(h1.lons[i], h2.lons[j]))
			--i;
		    else
			--j;
		 }
	     }

	// It's "all or nothing" containment if there were no half-poly
	// intersections... either one is contained in the other, or else
	// they're disjoint.
	if(found == 0)
	 {
	    if(p2.contains(p1.v[0])) return  p1.getArea();
	    if(p1.contains(p2.v[0])) return  p2.getArea();
	    return  0;
	 }

	// Check for p1 vertices contained in p2
	for(int i=0; i<p1.n; i++)
	    if(p2.contains(p1.v[i]))
	     {
		found++;
		double a = Math.PI - p1.angles[i];
		angles += a;
	     }

	// Check for p2 vertices contained in p1
	for(int i=0; i<p2.n; i++)
	    if(p1.contains(p2.v[i]))
	     {
		found++;
		double a = Math.PI - p2.angles[i];
		angles += a;
	     }

	if(found < 3)
	 {
	    log.aprintln("DEGENERATE INTERSECTION GEOMETRY FOR " + p1.name +
			 " AND " + p2.name);
	    return  NO_AREA;
	 }

	double area = angles - Math.PI*(found-2);
	if(area < 0)
	    if(area > -ZERO_TOL)
		area = -area;
	    else
	     {
		if(area < -(found/3 * Math.PI * 2))
		    log.aprintln("NEGATIVE AREA FOUND FOR " + p1.name +
				 " AND " + p2.name);
		area -= Math.floor(area / Math.PI / 2) * Math.PI * 2;
		if(area - Math.min(p1.getArea(), p2.getArea()) > ZERO_TOL)
		    log.aprintln("SUSPICIOUS INTERSECTION AREA " + p1.name +
				 " " + p2.name);
	     }
	return  area;
     }

    /**
     ** If DO_AREA is 0, returns the constant value 1.
     ** If DO_AREA is 1, returns proportion of p2's area covered by p1.
     ** If DO_AREA is 2, returns proportion of p1's area covered by p2.
     **/
    private static double area(SPolygon p1, SPolygon p2, int DO_AREA)
     {
	switch(DO_AREA)
	 {
	 case 0: return  1;
	 case 1: return  area(p1, p2) / p1.getArea();
	 case 2: return  area(p1, p2) / p2.getArea();
	 }
     
	return 0;
     }

    public static final class Intersection
     {
	public final SPolygon first;
	public final SPolygon second;

	private Intersection(SPolygon first, SPolygon second)
	 {
	    this.first = first;
	    this.second = second;
	 }
     }

    private static final class HalfPolygon
     {
	HVector[] v;
	HVector[] u;
	double[] lons;
	int n;
	int inside;

	HalfPolygon(int _n)
	 {
	    this.v = new HVector[_n];
	    this.u = new HVector[_n];
	    this.lons = new double[_n];
	    this.n = _n;
	 }
     }


    private SPolygon()
     {
	this.angles = null;
	this.v = null;
	this.u = null;
	this.uid = ++UID;
	this.name = null;
     }

    private SPolygon(String name)
     {
	this();
	this.name = name;
     }

    private SPolygon(Line2D lonlatLine)
     {
	this();

	double x1 = lonlatLine.getX1();
	double y1 = lonlatLine.getY1();
	double x2 = lonlatLine.getX2();
	double y2 = lonlatLine.getY2();

	addPoint(x1, y1);
	addPoint(average360(x1, x2), (y1+y2)/2 + 0.1);
	addPoint(x2, y2);
	addPoint(average360(x1, x2), (y1+y2)/2 - 0.1);
	initClassic();
     }

    public SPolygon(Shape lonlatShape)
     {
	this.uid = ++UID;

	PathIterator pi = lonlatShape.getPathIterator(null);
	double[] coords = new double[6];
	while(!pi.isDone())
	 {
	    switch(pi.currentSegment(coords))
	     {
	     case PathIterator.SEG_MOVETO:
	     case PathIterator.SEG_LINETO:
		addPoint(coords[0], coords[1]);
		break;

	     case PathIterator.SEG_CLOSE:
		break;

	     default:
		throw  new IllegalArgumentException(
		    "NON-LINEAR POLYGON SEGMENTS NOT ALLOWED!");
	     }
	    pi.next();
	 }
	initClassic();
     }

    private void addPoint(double lon, double lat)
     {
	lonsTmp.add(lon);
	latsTmp.add(lat);
	lon *= Math.PI / 180;
	lat *= Math.PI / 180;
	vvTmp.add(new HVector(Math.cos(lat) * Math.cos(lon),
			      Math.cos(lat) * Math.sin(lon),
			      Math.sin(lat) )
	    );
     }

    public double getArea()
     {
	if(area == -1)
	 {
	    area = 0;
	    for(int i=0; i<n; i++)
		area += Math.PI - angles[i];
	    area -= (n-2) * Math.PI;
	    if(area < 0)                           
		log.aprintln("FOUND NEGATIVE AREA " + area + " FOR " + name);
	 }
	return  area;
     }

    /** Roughly sign(x) == sign(y) */
    private static boolean same_signs(double x, double y)
     {
	if(x == 0)
	    return y == 0;

	if(x > 0)
	    return y > 0;

	return  y < 0;
     }

    public static boolean edgesIntersect(HVector aa,
					 HVector bb,
					 HVector ee,
					 HVector ff)
     {
	HVector ab = aa.cross(bb);
	if(same_signs(ab.dot(ee), ab.dot(ff)))
	    return false;

	HVector ef = ee.cross(ff);
	if(same_signs(ef.dot(aa), ef.dot(bb)))
	    return false;

	return  true;
     }

    private static boolean edgesIntersect(HVector aa,
					  HVector bb,
					  HVector ab,
					  HVector ee,
					  HVector ff,
					  HVector ef)
     {
	if(same_signs(ab.dot(ee), ab.dot(ff))) return false;
	if(same_signs(ef.dot(aa), ef.dot(bb))) return false;
	return  true;
     }

    /**
     ** Returns 1 if the half-polygons intersect, 0 if they don't.
     **/
    private static boolean hitTest(HalfPolygon h1, HalfPolygon h2)
     {
	int i = 0;
	int j = 0;

	while(i+1 < h1.n &&
	      j+1 < h2.n )
	 {
	    if(overlap360(h1.lons[i], h1.lons[i+1],
			  h2.lons[j], h2.lons[j+1]))
		if(edgesIntersect(h1.v[i], h1.v[i+1], h1.u[i],
				  h2.v[j], h2.v[j+1], h2.u[j]))
		    return true;
    
	    if(increasing360(h1.lons[i+1], h2.lons[j+1]))
		++i;
	    else
		++j;
	 }

	return  false;
     }

    /**
     ** Returns 1 if the polygons intersect, 0 if they don't.
     **/
    public static boolean hitTest(SPolygon p1, SPolygon p2)
     {
	if(!overlap360(p1.minLon, p1.maxLon,
		       p2.minLon, p2.maxLon))
	    return false;

	if(p1.self_intersecting || p2.self_intersecting)
	    return false;

	// See if any of the edges intersect in any of the half-polygons.
	for(int i=p1.h.length-1; i>=0; i--)
	    for(int j=p2.h.length-1; j>=0; j--)
		if(hitTest(p1.h[i],
			   p2.h[j]))
		    return true;

	// See if either of the polys completely contains the other (since
	// there are no edge intersections, this can only occur if one of
	// the polygons contains ONE of the other polygon's vertices).
	return  p1.contains(p2.v[0])
	    ||  p2.contains(p1.v[0]);
     }

    /**
     ** Returns 1 if the polygons intersect, 0 if they don't.
     ** Specialized test for line polygons (p1 should be the line).
     **/
    private static boolean hitTestLine(SPolygon p1, SPolygon p2)
     {
	if(!overlap360(p1.minLon, p1.maxLon,
		       p2.minLon, p2.maxLon))
	    return false;

	if(p1.self_intersecting || p2.self_intersecting)
	    return false;

	// See if any of the edges intersect in any of the half-polygons.
	for(int j=p2.h.length-1; j>=0; j--)
	    if(hitTest(p1.h[0],
		       p2.h[j]))
		return true;

	// See if either of the polys completely contains the other (since
	// there are no edge intersections, this can only occur if one of
	// the polygons contains ONE of the other polygon's vertices).
	return  p2.contains(p1.v[0]);
     }

    /**
     ** Returns the first and last intersections with this polygon
     ** along the given line. The intersections are scaled to the
     ** range [0:1], with 0 representing lonlatLine.pt1 and 1
     ** representing lonlatLine.pt2; any values in between are
     ** proprortionally in between the two endpoints of the line.
     **
     ** @return null if the line doesn't intersect this poly's outline
     **/
    public double[] getIntersectExtent(Line2D lonlatLine)
     {
	double lon0 = lonlatLine.getX1(); double lat0 = lonlatLine.getY1();
	double lon1 = lonlatLine.getX2(); double lat1 = lonlatLine.getY2();

// 	log.println("Attempting\t" + lon0 + "\t" + lat0 + "\n\t\t" +
// 		    lon1 + "\t" + lat1);

	HVector pt0 = newHVector(lon0, lat0);
	HVector pt1 = newHVector(lon1, lat1);
	HVector ptu = pt0.cross(pt1).unit();

	if(lon1 - lon0 > 180)
	 {
	    double tmp = lon0;
	    lon0 = lon1;
	    lon1 = tmp;
	 }

	double min = Double.POSITIVE_INFINITY;
	double max = Double.NEGATIVE_INFINITY;

	for(int i=0; i<n; i++)
	    if(overlap360(lon0, lon1, lons[i], lons[(i+1)%n]))
//		if(edgesIntersect(pt0, pt1, ptu, v[i], v[(i+1)%n], u[i]))
		if(edgesIntersect(pt0, pt1, v[i], v[(i+1)%n]))
		 {
		    HVector intersect = ptu.cross(u[i]).unit();
		    double angle = pt0.separation(intersect);
		    if(angle > Math.PI/2) angle = Math.PI - angle;
		    double val = angle / pt0.separation(pt1);
// 		    log.println(val);
		    if(val < min) min = val;
		    if(val > max) max = val;
		 }
// 		else
// 		 {
// 		    log.println("no intersect\t" +
//				pt0 + "\t" + pt1 + "\t" +
//				v[i] + "\t" + v[(i+1)%n]);
//		    log.println(edgesIntersect(pt0, pt1, v[i], v[(i+1)%n]));
// 				v[i].lonE() + "\t" + v[i].lat() + "\n\t\t" +
// 				v[(i+1)%n].lonE() + "\t" + v[(i+1)%n].lat());
// 		 }
// 	    else
// 		log.println("no overlap\t" +
// 			    v[i].lonE() + "\t" + v[i].lat() + "\n\t\t" +
// 			    v[(i+1)%n].lonE() + "\t" + v[(i+1)%n].lat());
// 	log.println(min);
// 	log.println(max);
	return  max < 0
	    ? null
	    : new double[] { min, max };
     }

    /**
     ** Returns 1 if this polygon contains the given point. Returns 0
     ** otherwise. Assumes pt is NOT the north or south pole.
     **/
    public boolean contains(HVector pt)
     {
	if(pt.z == 1  ||  pt.z == -1)
	    throw new IllegalArgumentException(
		"Can't use north/south pole in contains() test!");
	HVector north = HVector.Z_AXIS;
	HVector norm = pt.cross(north);
    
	// Does the segment pt->north intersect an odd number of poly edges?
	boolean odd = false;
/*
  for(int i=0; i<n; i++)
  odd ^= edgesIntersect(pt, north,
  v[i], v[(i+1)%n]);
*/    
	for(int i=0; i<h.length; i++)
	    for(int j=0; j+1<h[i].n; j++)
		if(edgesIntersect(pt, north, norm,
				  h[i].v[j], h[i].v[j+1], h[i].u[j]))
		 {
		    odd = !odd;
		    break; // from inner loop only
		 }

	return odd;
	//MCW must eliminate reliance on north pole... switch to south
	//pole on the fly, maybe? Or do the anti-polar point of one of
	//the poly vertices.
     }
    
    private String getName()
     {
	return name;
     }

    private int getOrbit()
     {
	return orbit;
     }

    // Populated in sort
    private double minLon;
    private double maxLon;
    private double minLat;
    private double maxLat;

    private int bandCount;
    private int mask;

    private static int UID;
    private int uid;
    private int orbit;
    private List vvTmp = new ArrayList(); //HVector
    private HVector[] vv;
    private TDoubleArrayList lonsTmp = new TDoubleArrayList();
    private TDoubleArrayList latsTmp = new TDoubleArrayList();
    private double[] lons;
    private double[] lats;

    private double area = -1;
    private double[] angles;

    private HVector[] v;
    private HVector[] u; // cached normals
    private int n;
    private String name;

    private List hTmp = new ArrayList(); //HalfPolygon
    private HalfPolygon[] h;
    private boolean self_intersecting;

    /**
     * SA: Ensures that the normals to v[i]-O-v[i+1] point inward by flipping
     * the point order, if necessary.
     */
    private void ensureInsideDir()
     {
	double angle = 0;
	for(int i=0; i<n; i++)
	    angle += angles[i];

	// Needs flipping
	// SA: The angle can be some -ve number other than -0
	// SA: A -ve number means that the data points are winding in a direction reverse
	//     of what we would like it to be. So, they need flipping.
	if(angle < -Math.PI/2)
	 {
	    int n = vv.length;
	    for(int i=n/2-1; i>=0; i--)
	     {
		HVector tmpV;
		double tmpD;
		int j = n-i-1;
		tmpV =   vv[i];   vv[i] =   vv[j];   vv[j] = tmpV;
		tmpD = lons[i]; lons[i] = lons[j]; lons[j] = tmpD;
		tmpD = lats[i]; lats[i] = lats[j]; lats[j] = tmpD;
		tmpD = angles[i]; angles[i] = -angles[j]; angles[j] = -tmpD;
	     }
	 }
     }

    /**
     ** Returns the signed turning angle that two edges make at a
     ** particular vertex. The turning angle is the "standard"
     ** geometry angle known as the "external angle" at that
     ** vertex. The sign of the angle corresponds to the rotation
     ** around +Z necessary to traverse the vertex from the prior edge
     ** to the next edge in the polygon.
     **/
    private void calcAnglesAndNorms()
     {
	// Calculate normals
	u = new HVector[n];
	for(int i=0; i<n; i++)
	    u[i] = v[i].cross(v[(i+1)%n]).unit();
	for(int vertex=0; vertex<n; vertex++)
	 {
	    HVector nextVertex = v[(vertex+1  )%n];
	    HVector prevNorm   = u[(vertex-1+n)%n];
	    HVector nextNorm   = u[ vertex       ];

	    if(nextVertex.dot(prevNorm) >= 0)
		angles[vertex] =  Math.acos(prevNorm.dot(nextNorm));
	    else
		angles[vertex] = -Math.acos(prevNorm.dot(nextNorm));
	 }
     }

    private static double extremeLat(HVector v, HVector u, HVector norm)
     {
	double bigz = Math.abs(v.z) > Math.abs(u.z) ? v.z : u.z;
	int signz = bigz>0 ? +1 : -1;
	HVector POLE = new HVector(0, 0, signz);
	HVector sideways = norm.cross(POLE);
	double vs = sideways.dot(v);
	double us = sideways.dot(u);
	if(vs >= 0  &&  us >= 0  ||
	   vs <= 0  &&  us <= 0)
	    return  Double.NaN;
	return  Math.acos(Math.abs(norm.z)) * 180 / Math.PI * signz;
     }

    /**
     ** Initializes the "classic" variables, required for basic hit
     ** testing.
     **/
    void initClassic()
     {
	// Convert our java dynamic arrays into plain arrays
	vv = (HVector[]) vvTmp.toArray(HVECTOR_0);
	vvTmp = null;

	lons = lonsTmp.toNativeArray();
	lonsTmp = null;

	lats = latsTmp.toNativeArray();
	latsTmp = null;

	n = vv.length;
	v = vv;
	angles = new double[n];

	calcAnglesAndNorms();
	ensureInsideDir();
	calcAnglesAndNorms(); // Should be a better way to do this than just
	// repeating ourselves... don't care right now.

	// Only good for well-named stamp polygons... but very handy for
	// some very specific code in poly_coverage.
	if(name != null  &&  name.length() != 0)
	    orbit = Integer.parseInt(name.substring(1, 6));

	/**
	 ** Separates the polygon into two separate "sorted" half-polys,
	 ** each of which is increasing in longitude (modulo 360).
	 **/

	// Determine indexes of the points with the minimum- and
	// maximum- extent longitudes. Note that longitude is cyclic,
	// so we really mean eastest and westest extents. This
	// requires some rather slippery comparisons.
	int min = 0;
	int max = 0;
	minLat = lats[0];
	maxLat = lats[0];
	for(int i=1; i<n; i++)
	 {
	    double extLat = extremeLat(v[i], v[(i+1)%n], u[i]);
	    if(extLat  < minLat) minLat = extLat;
	    if(extLat  > maxLat) maxLat = extLat;
	    if(lats[i] < minLat) minLat = lats[i];
	    if(lats[i] > maxLat) maxLat = lats[i];
	    // Polar points don't contribute a longitude. Don't use
	    // polygons that surround the pole... but the pole as a
	    // polygon vertex is OK.
	    if(Math.abs(lats[i]) == 90) continue;
	    if(  increasing360(lons[max], lons[i]) )  max = i;
	    if( !increasing360(lons[min], lons[i]) )  min = i;
	 }
	minLon = lons[min];
	maxLon = lons[max];

	// Create all the half-polygons
	// SA: Half-polygons are either completely longitudanlly increasing or decreasing.
	//     They are not mixed. A new half-polygon is constructed at every transition in
	//     the direction of longitude.
	// SA: The following code ensures that all the longitudes of the points within the
	//     half-polygons increases from the lowest index to the highest index. See (1).
	//
	//         o-----o                    o-----o         
	//        /       \                  /       \     
	//       o         \                o     o   \    
	//        \         o                      \   o    o
	//         \       /                        \      /
	//          \     o          =>              \    o    o
	//           \     \                          \         \
	//            \     o                          \     o   o 
	//             \   /                            \   /      
	//              \ /                              \ /      
	//               o                                o      
	int start = min;
	boolean decrease = false;
	for(int i=0; i<=n; i++)
	 {
	    // This conditional isn't coded as "decrease ? inc() : dec()"
	    // because that wouldn't handle curr==next correctly.
	    double curr = lons[(min+i  ) % n];
	    double next = lons[(min+i+1) % n];
	    // SA: (2) Change in longitude direction detected here.
	    if(i == n  ||  (decrease
			    ? !decreasing360(curr,next)
			    : !increasing360(curr,next)))
	     {
		int nn = (min+i - start + n) % n + 1;
		HalfPolygon hp = new HalfPolygon(nn);
		for(int j=0; j<hp.n; j++)
			// SA: (1) Points of the half-polyons are kept in increasing order from 
			//     the lowest to the highest index.
		    if(decrease)
		     {
			hp.inside  = -1;
			hp.v[j]    =    v[(start+hp.n-1-j)%n];
			hp.lons[j] = lons[(start+hp.n-1-j)%n];
		     }
		    else
		     {
			hp.inside  = +1;
			hp.v[j]    =    v[(start+j)%n];
			hp.lons[j] = lons[(start+j)%n];
		     }
                
		for(int j=0; j+1<hp.n; j++)
		    hp.u[j] = hp.v[j].cross(hp.v[j+1]).unit();
		hTmp.add(hp);
		start = (min+i) % n;
		decrease = !decrease;
	     }
	 }

	// Convert java dynamic arrays to native arrays
	h = (HalfPolygon[]) hTmp.toArray(HALFPOLYGON_0);
	hTmp = null;

	// Last but not least, an obvious requirement.
	if(n < 3)
	 {
	    self_intersecting = true;
	    return;
	 }

	// Check to ensure that the polygon isn't self-intersecting.
	// For latitudinally convex polygons, runs in O(n) time.
	// Degrades to O(n^2) in the face of extremely non-convex
	// polygons (again, only care about the latitude direction).
	self_intersecting = false;
	for(int x=0; x<h.length; x++)
	    for(int y=x+1; y<h.length; y++)
	     {
		HalfPolygon h1 = h[x];
		HalfPolygon h2 = h[y];
		int i = h1.n-2;
		int j = h2.n-2;
	    
		if(i < 0  ||  j < 0)
		    log.aprintln("ODD INDEXING!!!!!!!!!!!!!!!!!!\n");

		while(i >= 0  &&  j >= 0)
		 {
		    if(overlap360(h1.lons[i], h1.lons[i+1],
				  h2.lons[j], h2.lons[j+1]))
			// I know it looks awful... I'm unable to
			// reduce this to anything smaller, and it
			// would take several pages to completely
			// comment. So just deal, and/or see me. The
			// idea behind it is simple, the details of
			// implementation messy.
			if(!( h.length != 2
				 // SA: For more/?less? than two segmented closed polygon, skip the
				 //     junction point segments of two adjacent half-polygons.
			      ? ((y == x+1 || y-x+1 == h.length)  && 
				 ((x&1)!=0 || y-x+1 == h.length
				  ? i == 0      && j == 0
				  : i == h1.n-2 && j == h2.n-2))
				  // SA: For a two segmented closed polygon, skip the starting
				  //     and ending points of the two half-polygons since they will
				  //     always intersect.
			      : ((i==0      && j==0     )  ||
				 (i==h1.n-2 && j==h2.n-2)  )
			       ))
			    if(edgesIntersect(h1.v[i], h1.v[i+1],
					      h2.v[j], h2.v[j+1]))
			     {
				System.out.println(
				    "IGNORING POLY, SELF-INTERSECTING:\t" +
				    name + " " + h.length);
				self_intersecting = true;
				return;
			     }

		    if(decreasing360(h1.lons[i], h2.lons[j]))
			--i;
		    else
			--j;
		 }
	     }

	// Check for truly degenerate polys, which don't seem to be
	// caught correctly by any other method (including the
	// "exhaustive" one below).
	for(int i=0; i<n; i++)
	    if(v[i].equals(v[(i+1) % n]))
	     {
		System.out.println("IGNORING POLY, DUPLICATE VERTEX " +
				   (i+1) + ":\t" + name);
		self_intersecting = true;
		return;
	     }

/* OLD EXHAUSTIVE METHOD, uncomment this to see if the fancier methods
 * are missing something.
 for(int i=0; i<n; i++)
 for(int j=i+2; j<n && (j+1)%n!=i; j++)
 if(edgesIntersect(v[i], v[(i+1)%n],
 v[j], v[(j+1)%n]))
 {
 cerr << "IGNORING POLY, ** SELF-INTERSECT SLIPPED THRU ** "
 << name << " " << n << " " << i << " " << j << endl;
 self_intersecting = 1;
 goto BREAK;
 }
*/

     }

    /**
     ** This is a stream-like class that breaks an underlying reader's
     ** data into whitespace-separated tokens, much like the C++
     ** iostreams do.
     **/
    private static class WSTokenizer
     {
	private final BufferedReader br;
	private String[] tokens = new String[0];
	private int nextTokenIdx;

	public WSTokenizer(BufferedReader br)
	 {
	    this.br = br;
	 }

	public void close()
	 throws IOException
	 {
	    br.close();
	 }

	/**
	 ** Retrieve the next token, removing it from the stream.
	 ** Returns null if EOF is encountered.
	 **/
	public String next()
	 throws IOException
	 {
	    if(tokens == null)
		return  null;
	    if(nextTokenIdx < tokens.length)
		return  tokens[nextTokenIdx++];

	    do
	     {
		String line = br.readLine();
		if(line == null)
		 {
		    tokens = null;
		    return  null;
		 }

		tokens = line.trim().split("\\s+");
	     }
	    while(tokens.length == 0  ||  tokens[0].length() == 0);

	    nextTokenIdx = 1;
	    return  tokens[0];
	 }

	/**
	 ** Return the next token WITHOUT removing it from the stream.
	 ** Returns null if EOF is encountered.
	 **/
	public String peek()
	 throws IOException
	 {
	    if(tokens == null)
		return  null;

	    if(nextTokenIdx < tokens.length)
		return  tokens[nextTokenIdx];

	    String t = next();
	    nextTokenIdx = 0;
	    return  t;
	 }
     }


    /**
     * Adds onto pre-existing array of polygons.
     */
    private static List readPolygonList(BufferedReader br, String note)
     throws IOException
     { 
	announce("   Reading " + note);

	WSTokenizer wst = new WSTokenizer(br);

	List polys = new ArrayList();

	// Read them all in
	String tok;
	SPolygon last_poly = null;
	int n = 0;
	while((tok = wst.next()) != null)
	    if(tok.charAt(0) == '#')
	     {
		polys.add(last_poly = new SPolygon(tok.substring(1)));
		if(++n % 1000 == 0)
		    announce_update(n/1000 + "k polys");
	     }
	    else
	     {
		double lon = Double.parseDouble(tok);
		if((tok = wst.next()) == null)
		    throw  new EOFException("Early EOF in " + note);
		double lat = Double.parseDouble(tok);
		last_poly.addPoint(lon, lat);
	     }

	// Initialize the "classic" variables in the polygons
	announce("Processing " + note);
	for(Iterator i=polys.iterator(); i.hasNext(); )
	    ((SPolygon) i.next()).initClassic();

	return  polys;
     }
    /**
     ** Open a file and return a stream to it. The filename "-" is special,
     ** and returns stdin. A filename that starts with a hash sign "#" is
     ** interpreted as inline file-contents... a stream that reads the
     ** filename string itself is returned.
     **
     ** If the file fails to open, an error message is printed and exit()
     ** is called.
     **/
    private static BufferedReader cmdline_file(String fname)
     throws IOException
     {
	if(fname.equals("-"))
	    return  new BufferedReader(new InputStreamReader(System.in));

	if(fname.charAt(0) == '#')
	    return  new BufferedReader(new StringReader(fname));

	return  new BufferedReader(new FileReader(fname));
     }







//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////







    private static final PrintStream err = System.err;
    private static final PrintStream out = System.out;

    private static final int INT_BITS = 32;
    private static final String dbUrl
    = "jdbc:mysql://mapserver1.mars.asu.edu/themis2?";

    private static void errusageTC()
     {
	System.err.println(
	    "\n"
	    +"USAGE: USER PASS [-t PREFIX] [CACHE_SPEC] [-only METRIC] COUNT [TABLE]\n"
	    +"\n"
	    +"The global coverage statistics for THEMIS are updated (table\n"
	    +"TABLE in themis2), including all work necessary to retrieve\n"
	    +"current geometry (using themis3), calculate coverage, and\n"
	    +"update the statistics records. If TABLE is omitted, no database\n"
	    +"update is performed (but all other processing is).\n"
	    +"\n"
	    +"      ---  OPTIONS MUST OCCUR IN THE GIVEN ORDER!!!  ---\n"
	    +"\n"
	    +"The COUNT argument controls how many sample points are used.\n"
	    +"\n"
	    +"If -t is present, 'test' mode is engaged. This mode speeds up\n"
	    +"database operations by limiting the query to orbits with the\n"
	    +"given prefix. For example, '-t 00' restricts things to orbits\n"
	    +"below 1000. Test mode disables the calculation of n_* metrics.\n"
	    +"\n"
	    +"CACHE_SPEC can be either '-incache FNAME' or '-outcache FNAME'.\n"
	    +"The -incache option uses the specified file in lieu of real\n"
	    +"database geometry. The -outcache option should be used to\n"
	    +"generate such a file, after which normal processing still\n"
	    +"occurs. In either case, if the cache filename ends in '.gz'\n"
	    +"it is created/read as a gzip file.\n"
	    +"\n"
	    +"CONTROL-C IS DISABLED WHILE ACTUAL COVERAGE CALCULATIONS ARE\n"
	    +"TAKING PLACE! Instead, a ctrl-c triggers a dump of the current\n"
	    +"data. The program may only be killed using the process control\n"
	    +"commands such as 'kill', or by hitting ctrl-c twice in\n"
	    +"quick succession.\n"
	    +"\n");
	System.exit(-1);
     }

    private static void exiterr(String msg)
     {
	err.println("\n" + msg + "\n");
	System.exit(-1);
     }

    private static void exiterr_db(Connection conn, String msg)
     {
	err.println("\n" + msg + "\n");
	System.exit(-1);
     }

    /**
     ** Assuming a single bit in the input is set, returns the bit number
     ** of that bit, with zero being the least-significant bit. Returns -1
     ** if no bits are set.
     **/
    private static int bit_n(int n)
     {
	for(int i=0; i<INT_BITS; i++)
	    if(n>>i == 1)
		return  i;
	return  -1;
     }

    private String revbinary(int x)
     {
	if(x == 0) return  "_";
	StringBuffer buff = new StringBuffer();
	int i=0;
	do
	 {
	    buff.append((x&1)!=0 ? '0'+i%10 : '_');
	    x >>= 1;
	    ++i;
	 }
	while(x != 0);
	return  buff.toString();
     }

    /**
     ** Useless placeholders.
     **/
    private static final boolean USE_RESULT = false;
    private static final boolean STORE_RESULT = true;

    /**
     ** In C this did some error-checking... in Java with exceptions,
     ** this is just an empty wrapper. I'm leaving it in just because
     ** it makes for less typing to remove it.
     **/
    private static ResultSet safe_query(Connection conn,
					boolean storeResult,
					String sql)
     throws SQLException
     {
	return  conn.createStatement().executeQuery(sql);
     }

    /**
     ** Replacement for mysql library function.
     **/
    private static int mysql_num_rows(ResultSet res)
     throws SQLException
     {
	res.last();
	int count = res.getRow();
	res.beforeFirst();
	return  count;
     }

    /**
     ** Replacement for mysql library function.
     **/
    private static boolean mysql_fetch_row(ResultSet res)
     throws SQLException
     {
	return  res.next();
     }

    /**
     ** Replacement for mysql library function.
     **/
    private static void mysql_free_result(ResultSet res)
     throws SQLException
     {
	res.close();
     }

    /**
     ** Replacement libc library function.
     **/
    private static BufferedReader open_r(String fname)
     throws IOException
     {
	if(fname.endsWith(".gz"))
	    return  new BufferedReader(
		new InputStreamReader(
		    new GZIPInputStream(
			new BufferedInputStream(
			    new FileInputStream(fname)))));
	else
	    return  new BufferedReader(new FileReader(fname));
     }

    /**
     ** Replacement libc library function.
     **/
    private static PrintWriter open_w(String fname)
     throws IOException
     {
	if(fname.endsWith(".gz"))
	    return  new PrintWriter(
		new OutputStreamWriter(
		    new GZIPOutputStream(
			new BufferedOutputStream(
			    new FileOutputStream(fname)))));
	else
	    return  new PrintWriter(new BufferedWriter(new FileWriter(fname)));
     }

    private static String announce_msg = null;
    private static String announce_buff = null;
    private static long announce_time;

    private static void announce(String msg)
     {
	long next = System.currentTimeMillis();

	if(announce_msg != null)
	 {
	    double secs = (next - announce_time) / 1000.0;
	    DecimalFormat f = new DecimalFormat("0.0");
	    err.println("\r" + pad40(announce_buff) +
			 (secs >= 0.1
			  ? "DONE, took " + pad6(f.format(secs)) + " secs."
			  : "DONE.    "));
	 }

	if(msg != null)
	 {
	    announce_buff = msg + "...";
	    err.print("\r" + pad40(announce_buff));
	 }
	announce_msg = msg;
	announce_time = next;
     }

    private static void announce_update(String update)
     {
	err.print("\r" + pad40(announce_buff + " (" + update + ") ") + " ");
     }

    private static String pad40 = "                                        ";
    private static String pad40(String s)
     {
	int len = s.length();
	return  len < 40
	    ? s + pad40.substring(s.length())
	    : s;
     }

    private static String pad6 = "      ";
    private static String pad6(String s)
     {
	int len = s.length();
	return  len < 6
	    ? pad6.substring(s.length()) + s
	    : s;
     }

    /**
     ** Returns a comparison between a and b as Arrays of Polygons,
     ** according to the orbit number of the first polygon in the
     ** Array. Used to sort the Arrays in REVERSE by orbit number.
     **/
    private static final Comparator compareByOrbit =
    new Comparator()
     {
	public int compare(Object a, Object b)
	 {
	    SPolygon[] aa = (SPolygon[]) a;
	    SPolygon[] bb = (SPolygon[]) b;
	    return  bb[0].getOrbit()
		-   aa[0].getOrbit();
	 }
     };

    public static void coverage_main(String[] av)
     throws Throwable
     {
	out.print("RAN WITH:");
	for(int i=0; i<av.length; i++)
	    out.print(' ' + av[i]);
	out.println("");

	final boolean debug = false;
	String tmp;

	if(av.length < 3
	   ||  av[0].equals("-help")
	   ||  av[0].equals("--help")
	   ||  av[0].equals("-h")
	   ||  av[0].equals("-?"))
	    errusageTC();
	int ac = 0;

	// Retrieve the username and password
	String USER = av[0];
	String PASS = av[1];
	ac += 2;

	//
	// Retrieve the -t flag, if present
	//
	String testmode = "";
	if(av[ac].equals("-t"))
	 {
	    testmode = av[ac+1];
	    ac += 2;
	 }

	SobSeq ss = new SobSeq();

	//
	// Retrieve the cache file specification, if present
	//

	String incache = null;
	String outcache = null;
	boolean gzcache = false;
	if(ac < av.length  &&
	   (av[ac].equals("-incache")  ||  av[ac].equals("-outcache")))
	 {
	    if(av[ac].charAt(1) == 'i') incache = av[ac+1];
	    else                       outcache = av[ac+1];

	    if(av[ac+1].endsWith(".gz"))
		gzcache = true;
	    ac += 2;
	 }

	//
	// Retrieve the -only flag, if present.
	//

	String only = null;
	if(ac < av.length  &&  av[ac].equals("-only"))
	 {
	    only = av[ac+1];
	    ac -= 2;
	 }

	//
	// Retrieve the sample count
	//

	if(ac >= av.length)
	    errusageTC();

	int maxpointcount = Integer.parseInt(av[ac]);
	++ac;

	//
	// Retrieve the table name
	//

	String table = null;
	if(ac < av.length)
	 {
	    table = av[ac];
	    ++ac;
	 }

	if(ac != av.length)
	 {
	    err.println("Don't understand args, starting with " + av[ac]);
	    exiterr("Usage error");
	 }

	announce("Connecting to the database");

	//
	// Open a database connection
	//

	Util.loadSqlDrivers();
	Connection conn = DriverManager.getConnection(dbUrl, USER, PASS);
	String sql;
	boolean row;
	String sqlbuff;
	ResultSet res;

	announce("Retrieving coverage metric types");

	//
	// Determine the metric types and their minimum band counts
	//

	String[] metrics;          // Contains { "ir_all", "ir_day", ... }
	int[] metrics_minbands; // Contains minimum band count for each metric
	sql = "select type, max(min_bands) as min_bands from coverage_stats_groups inner join coverage_stats_sql on find_in_set(keyword,replace(type,'_',',')) where type not like 'n_%' group by type order by type-1";
	res = safe_query(conn, STORE_RESULT, sql);
	int metrics_count = mysql_num_rows(res);
	metrics = new String[metrics_count];
	metrics_minbands = new int[metrics_count];
	if(metrics_count > INT_BITS)
	    err.println(
		"\n\n\n-----> WARNING: TOO MANY METRICS!!! <------\n\n\n");
	int tmpMask = 0;
	for(int i=0; i<metrics_count; i++)
	    if(row = mysql_fetch_row(res))
	     {
		metrics[i] = res.getString(1);
		metrics_minbands[i] = res.getInt(2);
		if(metrics_minbands[i] != 0)
		    tmpMask |= 1<<i;
	     }
	    else
		exiterr_db(conn, "While retrieving the metrics");
	int metrics_bandmask = tmpMask; // Masks the multi-band metrics
	mysql_free_result(res);

	//
	// Determine the n_metric types and their corresponding percentage
	// type masks.
	//

	int   n_metrics_count; // How many n_foo metrics do we have.
	int[] n_metrics_code;  // The MySQL enum value for the metric
	int[] n_metrics_mask;  // The single-bit mask for the 'foo' version of n_foo
	int  n_metrics_any = 0;
	sql = "select a.type+0, 1<<(b.type-1) from coverage_stats_groups a inner join coverage_stats_groups b on a.type=concat('n_',b.type)";
	res = safe_query(conn, STORE_RESULT, sql);
	n_metrics_count = mysql_num_rows(res);
	n_metrics_code = new int[n_metrics_count];
	n_metrics_mask = new int[n_metrics_count];
	for(int i=0; i<n_metrics_count; i++)
	    if(row = mysql_fetch_row(res))
	     {
		n_metrics_code[i] = res.getInt(1);
		n_metrics_mask[i] = res.getInt(2);
		n_metrics_any |= n_metrics_mask[i];
	     }
	    else
		exiterr_db(conn, "While retrieving the metrics");
	mysql_free_result(res);

	//
	// Determine the metric types mask expression
	//

	String metrics_mask_expr; // Single MySQL expression for the total mask
	int notzero = (1 << metrics_count) - 1;
	sql = "select condition, bit_or(1 << (type-1)) as mask from coverage_stats_groups inner join coverage_stats_sql on find_in_set(keyword,replace(type,'_',',')) where condition is not null group by keyword having mask";
	res = safe_query(conn, STORE_RESULT, sql);
	tmp = "";
	for(int i=0; row=mysql_fetch_row(res); i++)
	    tmp += (i!=0 ? " & if(" : " if(") + res.getString(1)
		+ ", " + notzero
		+ ", ~" + res.getInt(2) + ")";
	metrics_mask_expr = tmp;
	mysql_free_result(res);

	tmp = "select concat(file_id, '.', band), lon, lat, "
	    + metrics_mask_expr + " as mask, bit_count(bandcfg) as bandcount"
	    + " from themis3.frmgeom"
	    + " inner join themis2.obs use index (poly_retrieve)"
	    + " on filename=file_id"
	    + " where poly_idx is not null "
	    + " and (file_id like 'I" + testmode
	    + "%' or file_id like 'V" + testmode + "%')"
	    + " order by file_id, band, poly_idx"
	    ;

	if(true)//debug)
	 {
	    out.println("\n" + tmp);
	    for(int i=0; i<metrics_count; i++)
		out.println(i + ": " + metrics[i]);
	 }
 
	//
	// Finally, submit the query to retrieve all the polygons and masks
	//
 
	BufferedReader cacheR = null;
	PrintWriter cacheW = null;

	if(incache != null)
	 {
	    announce("Polygon query: reading from cache");

	    cacheR = open_r(incache);
	 }
	else
	 {
	    announce("Polygon query: executing on server");
 
	    res = safe_query(conn, USE_RESULT, tmp);

	    // Set up to spit out cache file, if requested
	    if(outcache == null)
		announce("Polygon query: reading from server");
	    else
	     {
		announce("Polygon query: reading from server (and caching)");

		cacheW = open_w(outcache);
	     }
	 }

	//
	// Retrieve the results of the query
	//

	List polysTmp = new ArrayList(); //SPolygon
	TIntArrayList masksTmp = new TIntArrayList();
	TIntArrayList masks_idxTmp = new TIntArrayList();

//	SPolygon[] polys; // Raw list of polys read from database
	int[] masks;      // Raw list of polygon POINT masks from query
	int[] masks_idx;  // Parallel array to polys, indexing into masks
	SPolygon last_poly = null;

	String cacheline = null; // temporary buffer
	while(incache!=null
	      ? null != (cacheline = cacheR.readLine())
	      : (row = mysql_fetch_row(res)) )
	 {
	    String row_0;
	    double row_1;
	    double row_2;
	    int row_3;
	    int row_4;

	    if(incache != null)
	     {
		if(cacheline.length() == 0)
		    continue;
		String[] cacherow = cacheline.split("\t");
		if(cacherow.length != 5)
		    exiterr("FOUND INVALID CACHE ROW!! [" + cacheline + "]");
		row_0 = cacherow[0];
		row_1 = Double.parseDouble(cacherow[1]);
		row_2 = Double.parseDouble(cacherow[2]);
		row_3 = Integer.parseInt(cacherow[3]);
		row_4 = Integer.parseInt(cacherow[4]);
	     }
	    else
	     {
		if(outcache != null)
		    cacheW.println(res.getString(1) + "\t" +
				   res.getString(2) + "\t" +
				   res.getString(3) + "\t" +
				   res.getString(4) + "\t" +
				   res.getString(5));
		row_0 = res.getString(1);
		row_1 = res.getDouble(2);
		row_2 = res.getDouble(3);
		row_3 = res.getInt(4);
		row_4 = res.getInt(5);
	     }

	    if(last_poly == null  ||  !last_poly.getName().equals(row_0))
	     {
		if(last_poly != null)
		    last_poly.initClassic();
		polysTmp.add(last_poly = new SPolygon(row_0));
		masks_idxTmp.add(masksTmp.size());
		last_poly.bandCount = row_4;

		if(polysTmp.size() % 1000 == 0)
		    announce_update(polysTmp.size() + " polys");
	     }
	    last_poly.addPoint(row_1, row_2);
	    masksTmp.add(row_3);
	 }
	if(last_poly == null)
	    exiterr("NO POLYGONS FOUND IN QUERY RESULTS!!!");
	last_poly.initClassic();

	if(cacheW != null) cacheW.close();
	if(cacheR != null) cacheR.close();
	else               mysql_free_result(res);

	// Convert dynamic lists into regular arrays

	masks = masksTmp.toNativeArray();
	masksTmp = null;

	masks_idx = masks_idxTmp.toNativeArray();
	masks_idxTmp = null;

	out.println("Read " + polysTmp.size() + " polygons.");

	//
	// Assign polygon bit masks, and in the process find and break
	// those with conflicting masks into separate polygons. Also
	// determine the min and max orbit numbers.
	//
	// LIMITATION: THIS IMPLEMENTATION DOESN'T ALLOW FOR SPLITTING ON
	// MASK BITS THAT REQUIRE BAND-GROUPINGS... ONLY SINGLE-BAND
	// METRICS ARE HANDLED WITH THIS APPROACH TO SPLITTING! It's due
	// to the order in which the split polygons are appended to the
	// list: metric-major instead of band-major.
	//

	announce("Polygon query: sorting results");

	announce_update("splitting");

	int minOrbit = Integer.MAX_VALUE;
	int maxOrbit = 0;
	int orig_poly_len = polysTmp.size();
	for(int i=0; i<orig_poly_len; i++)
	 {
	    SPolygon pi = (SPolygon) polysTmp.get(i);

	    // Update orbit min/max
	    int orbit = pi.getOrbit();
	    if(orbit != 0  &&  orbit <= 99999)
	     {
		if(orbit < minOrbit) minOrbit = orbit;
		if(orbit > maxOrbit) maxOrbit = orbit;
	     }
	    else
		out.println("Bad orbit in name: " + pi.getName());

	    // Determine poly mask from point masks
	    int minmask = ~0;
	    int maxmask =  0;
	    for(int j=0; j<pi.n; j++)
	     {
		minmask &= masks[masks_idx[i]+j];
		maxmask |= masks[masks_idx[i]+j];
	     }
	    pi.mask = minmask;
	    if(minmask == maxmask)
		continue;

	    // We need to split this polygon, due to one or more bits in the mask
	    for(int bit=1, k=0; bit!=0; bit<<=1, k++)
	     {
		if((bit & minmask) == (bit & maxmask))
		    continue;
		// We need to split this polygon on THIS bit of the mask
		tmp = pi.getName().toLowerCase() + ".bit" + bit_n(bit);
		SPolygon p2 = new SPolygon(tmp);
		for(int j=0; j<pi.n; j++)
		    if((masks[masks_idx[i]+j] & bit) != 0)
			p2.addPoint(pi.lons[j], pi.lats[j]);
		p2.mask = bit;
		p2.bandCount = pi.bandCount;
		p2.initClassic();
		polysTmp.add(p2);
	     }
	 }
	out.println("Found orbits " + minOrbit + " to " + maxOrbit);
	if(minOrbit == 0)
	    exiterr("Didn't find any orbit numbers! Were there any polygons?");
	if(maxOrbit > 99999)
	    exiterr("Something funny going on with the orbit numbers!");

	announce_update("purging");

	if(debug)
	 {
	    announce("Writing out filenames");

	    out.println("Filenames...");

	    for(Iterator i=polysTmp.iterator(); i.hasNext(); )
		out.println(((SPolygon) i.next()).getName());
	 }

	//
	// Index the polygons
	//

	announce_update("indexing");

	final int LONBIN_COUNT = 180;
	final int LATBIN_COUNT = 90;

	Index single_index = new Index(polysTmp,
				       LONBIN_COUNT,
				       LATBIN_COUNT);

	final int LONBIN_SZ = single_index.LONBIN_SZ;
	final int LATBIN_SZ = single_index.LATBIN_SZ;

	polysTmp = null;

	announce_update("purging");

// 	if(debug)
// 	 {
// 	    announce("Dumping non-grouped bins");

// 	    cout << "Non-grouped filename dump...\n";

// 	    for(int x=0; x<LONBIN_COUNT; x++)
// 		for(int y=0; y<LATBIN_COUNT; y++)
// 		 {
// 		    cout << "-- BIN " << x << ' ' << y << " --\n";
// 		    for(int g=0; g<single_index[x][y].len; g++)
// 			cout << single_index[x][y][g].getName() << endl;
// 		 }
// 	 }

	//
	// Group all bands of an image.
	//

	announce_update("grouping");

	final SPolygon[][][][] group_index
	    = new SPolygon[LONBIN_COUNT][LATBIN_COUNT][][];
	//group_index[0][0] is a List of List of SPolygon
	List       obin = new ArrayList();//List of List of SPolygon
	for(int x=0; x<LONBIN_COUNT; x++)
	    for(int y=0; y<LATBIN_COUNT; y++)
	     {
		String old_name = "abcd56789";
		SPolygon[] ibin = single_index.bins[x][y];
		obin.clear();
		List obin_last = null;
		for(int i=0; i<ibin.length; i++)
		 {
		    String new_name = ibin[i].getName().substring(0, 9);
		    if(Character.isLowerCase(new_name.charAt(0))  ||
		       !new_name.equals(old_name))
		     {
			obin.add(obin_last = new ArrayList());
			old_name = new_name;
		     }
		    obin_last.add(ibin[i]);
		 }

		// Convert dynamic list of list to array of array
		group_index[x][y] = new SPolygon[obin.size()][];
		Iterator i=obin.iterator();
		for(int z=0; z<group_index[x][y].length; z++)
		    group_index[x][y][z] =
			(SPolygon[]) ((List) i.next()).toArray(SPOLYGON_0);
	     }
	obin = null;
	single_index = null;

	if(debug)
	 {
	    announce("Dumping UN-sorted bins");
	    out.println("Unsorted filename dump...");
	    for(int x=0; x<LONBIN_COUNT; x++)
		for(int y=0; y<LATBIN_COUNT; y++)
		 {
		    out.println("-- BIN " + x + ' ' + y + " --");
		    for(int g=0; g<group_index[x][y].length; g++)
		     {
			for(int i=0; i<group_index[x][y][g].length; i++)
			    out.println(group_index[x][y][g][i].getName()+" ");
			out.println("");
		     }
		 }
	 }

	//
	// Re-sort groups to guarantee they're sorted by orbit
	//

	announce_update("group sort");

	for(int x=0; x<LONBIN_COUNT; x++)
	    for(int y=0; y<LATBIN_COUNT; y++)
		Arrays.sort(group_index[x][y], compareByOrbit);

	if(debug)
	 {
	    announce("Dumping sorted bins");
	    out.println("Sorted filename dump...");
	    for(int x=0; x<LONBIN_COUNT; x++)
		for(int y=0; y<LATBIN_COUNT; y++)
		 {
		    out.println("-- BIN " + x + " " + y + " --");
		    for(int g=0; g<group_index[x][y].length; g++)
		     {
			for(int i=0; i<group_index[x][y][g].length; i++)
			    out.println(group_index[x][y][g][i].getName()+" ");
			out.println("");
		     }
		 }
	 }

// 	if(false)//!*testmode)
// 	 {
// 	    //
// 	    // Actual count calculation
// 	    //

// 	    announce("Calculating counts");

// 	    int counts[maxOrbit+1][n_metrics_count];
// 	    bzero(counts, sizeof(int) * (maxOrbit+1) * n_metrics_count);

// 	    String last = "";
// 	    for(int i=0; i<orig_poly_len; i++)
// 	     {
// 		Polygon * p = polys[i];
// 		if(p.mask & n_metrics_any
// 		   &&  strncmp(last, p.getName(), 9)
// 		   &&  !p.self_intersecting)
// 		 {
// 		    counts[p.getOrbit()][0]++;
// 		    for(int i=n_metrics_count-1; i>=1; i--)
// 			if(p.mask & n_metrics_mask[i])
// 			    counts[p.getOrbit()][i]++;
// 		    last = p.getName();
// 		 }
// 	     }

// 	    announce("Saving counts");

// 	    announce_update("local");

// 	    char fname[1000] = "/tmp/themis_coverage.tmp.0";
// 	    FILE *fp = fopen(fname, "w");
// 	    if(!fp)
// 	     {
// 		perror(fname);
// 		exiterr("Failed to open tmp file");
// 	     }

// 	    int sums[metrics_count];
// 	    bzero(sums, sizeof(int) * metrics_count);

// 	    for(int i=minOrbit; i<=maxOrbit; i++)
// 		for(int j=0; j<n_metrics_count; j++)
// 		 {
// 		    sums[j] += counts[i][j];
// 		    fprintf(fp, "%d\t%d\t%d\n",
// 			    n_metrics_code[j], i, sums[j]);
// 		 }

// 	    fclose(fp);

// 	    if(table)
// 	     {
// 		announce_update("database");

// 		sql = sqlbuff;
// 		sprintf(sql,
// 			"load data local infile '%s' replace into"
// 			" table %s (type, orbit, cumulative)",
// 			fname, table);
// 		printf("Trying %s\n", sql);
// 		if(mysql_query(conn, sql))
// 		 {
// 		    strcat(announce_buff, "  [FAILURE]");
// 		    out.println("Failure during n_* database commit:" + "\n"
// 			 + "\t" + sql + "\n"
// 			 + "\t" + mysql_error(conn));
// 		 }
// 	     }
// 	 }

	//
	// Except for intermittent data dumps, we're done with our MySQL
	// connection. Close it to conserve server+client resources.
	//

	conn.close();
	conn = null;

	//
	// Actual coverage calculation
	//

	announce("Calculating coverage");

	int dumpcount = 0;

	// Actual book-keeping for final output... the monte carlo numbers
	int trials = 0;
	int[][] hits = new int[maxOrbit+1][metrics_count];

	double[] quasi = new double[2];
	while(++trials > 0)
	 {
	    // We want a quasi-random uniformly-distributed point on the sphere
	    ss.next(quasi);
	    double z   = quasi[0] * 2 - 1;
	    double lon = quasi[1] * 2 * Math.PI;

	    HVector pt = new HVector(Math.sqrt(1-z*z) * Math.cos(lon),
				     Math.sqrt(1-z*z) * Math.sin(lon),
				     z);
	    double lat = Math.asin(z);

	    // Convert to degrees
	    lon *= 180/Math.PI;
	    lat *= 180/Math.PI;

	    // Calculate the indexing bin corresponding to the random point
	    int x = (int) Math.floor(lon / LONBIN_SZ);
	    int y = (int) Math.floor(lat / LATBIN_SZ) + LATBIN_COUNT/2;
	    if(y >= LATBIN_COUNT) y = LATBIN_COUNT-1;

	    // Contains a bit mask of the metrics that we're still
	    // searching for AND that we can conceivably find.
	    int needMask = notzero;

	    final SPolygon[][] bin = group_index[x][y];
	    for(int i=bin.length-1; i>=0; i--)
	     {
		final SPolygon[] group = bin[i];

		// A bit mask indicating all the metrics that could be
		// newly-satisfied by this group.
		int groupMask = group[0].mask & needMask;

		// If we only have part of the group (due to indexing
		// splitting things), then we can't do multi-band metrics
		// (since they require every band to be hit). This
		// conditional always succeeds for IR images, but we don't
		// really care -- there are no multi-band IR metrics.
		if(group.length != group[0].bandCount)
		    groupMask &= ~metrics_bandmask;

		// If there's nothing to gain from this group, skip it.
		if(groupMask == 0)
		    continue;

		// A bit mask indicating the wanted metrics that could be
		// newly-satisfied if the point hits anywhere in current
		// group.
		int anyMask = groupMask & ~metrics_bandmask;

		// A bit mask indicating the wanted metric that could be
		// newly-satisfied if the point hits everything in the
		// current group.
		int allMask = groupMask & metrics_bandmask;

		// A bit mask representing what metrics were actually hit.
		int gotMask = 0;

		for(int j=group.length-1; j>=0; j--)
		 {
		    SPolygon p = group[j];

		    if(increasing360(p.minLon, lon)  &&
		       increasing360(lon, p.maxLon)  && /////MCW: lat range check?
		       p.contains(pt))
		     {
			gotMask = anyMask;
			groupMask = 0;
			if(allMask == 0) // If we're not seeking "all", end the loop
			    break;
		     }
		    else
		     {
			allMask = 0;
			if(groupMask == 0)
			    break;
		     }
		 }

		if(groupMask == 0)
		 {
		    gotMask |= allMask;
		    needMask &= ~gotMask;
		    int orbit = group[0].getOrbit();

		    // Record all of this group's hits' orbit numbers
		    for(int k=0; gotMask!=0; k++,gotMask>>=1)
			if((gotMask & 1) != 0)
			    ++hits[orbit][k];

		    if(needMask == 0)
			break;
		 }
	     }

	    if(trials % 10000 == 0)
	     {
		String report = trials/1000 + "k sample pts";

		announce_update(report);

		// !( n & (n-1)) <-- implies n is an exact power of 2

		// Checkpoint at final count, exact powers of ten, and signals
		if(trials >= maxpointcount  ||
		   trials == Math.round(
		       Math.pow(10,Math.round(Math.log(trials)/Math.log(10)))))
		 {
		    ++dumpcount;

		    String msg = "Saving checkpoint #" + dumpcount +
			" at " + trials + " pts";
		    announce(msg);

		    String fname =
			"/tmp/jthemis_coverage.tmp." + dumpcount + "." +trials;

		    out.println(msg + " into file " + fname);

		    PrintWriter fout = new PrintWriter(
			new BufferedWriter(new FileWriter(fname)));

		    announce_update("local");

		    DecimalFormat f = new DecimalFormat("0.000000");
		    int[] sums = new int[metrics_count];
		    for(int i=minOrbit; i<=maxOrbit; i++)
			for(int j=0; j<metrics_count; j++)
			 {
			    sums[j] += hits[i][j];
			    fout.println(
				(j+1) + "\t" + i + "\t" +
				f.format(sums[j] / (double) trials * 100));
			 }
		    fout.close();

		    if(only != null)
		     {
			for(int i=0; i<metrics_count; i++)
			    if(metrics[i].equalsIgnoreCase(only))
			     {
				err.println(
				    "\n" + metrics[i] + " = " +
				    f.format(sums[i] / (double) trials * 100));
				break;
			     }
		     }
		    else if(table != null)
		     {

			announce_update("connecting");

			// allocates space, free'd by mysql_close
			conn = DriverManager.getConnection(dbUrl, USER, PASS);
			sql = "load data local infile '" + fname +
			    "' replace into table " + table +
			    " (type, orbit, cumulative)";
			out.println("Trying " + sql);

			announce_update("database");

			conn.createStatement().execute(sql);
			conn.close();
		     }

		    if(trials < maxpointcount)
		     {
			announce("Continuing coverage");
		     }
		    else
		     {
			announce(null);
			System.exit(0);
		     }

		 } // end of if(want to dump data)

	     } // end of if(trials % 10k)

	 } // end of while(not overflow)

	log.aprintln("\nABORTED DUE TO OVERFLOW!\n");
	System.exit(-1);
     }




//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////



    private static void errusageI()
     {
	System.err.println(
	    "\n"
	    +"USAGE: [-area1|-area2] FILE1 FILE2\n"
	    +"\n"
	    +"SUMMARY\n"
	    +"The polygons in FILE1 are intersected with the polygons in\n"
	    +"FILE2. The program then outputs one line for every intersection\n"
	    +"found, containing three whitespace-separated items: first the\n"
	    +"name of the polygon from FILE1, then the name of the polygon\n"
	    +"from FILE2, then finally the fraction of area overlap between\n"
	    +"the two polygons.\n"
	    +"\n"
	    +"The program is biased to perform better if FILE1's polygons are\n"
	    +"relatively 'small'.\n"
	    +"\n"
	    +"ARGUMENTS\n"
	    +"One of FILE1 or FILE2 can be '-' to denote STDIN. If either\n"
	    +"filename contains a hash character (#), it is treated as the\n"
	    +"contents of a polygon file instead of the name of a file. This\n"
	    +"allows you to pass polygons in right on the command-line. You\n"
	    +"will likely need quotes on such an argument for the shell to\n"
	    +"treat it as a single argument.\n"
	    +"\n"
	    +"The output area is expressed as a fraction of the total area of one\n"
	    +"of the intersecting polygons. If -area1 is supplied, it's the\n"
	    +"fraction of the area from the polygon in FILE1. Likewise -area2\n"
	    +"makes it relative to FILE2's polygon. If neither -area flag is\n"
	    +"specified, an area of '1' is always output (this runs SIGNIFICANTLY\n"
	    +"faster).\n"
	    +"\n"
	    +"INPUT FILE FORMAT\n"
	    +"The input files should contain whitespace-separated polygons.\n"
	    +"A polygon starts with a hash sign (#), immediately followed by\n"
	    +"a 'name' for the polygon which cannot contain whitespace. The\n"
	    +"name can be empty, and can't be longer than 511 characters. This\n"
	    +"is then followed by whitespace and a whitespace-separated list\n"
	    +"of the polygon's vertices. A polygon vertex consists of east\n"
	    +"longitude in degrees, then whitespace, then latitude in\n"
	    +"degrees.\n");
	System.exit(-1);
     }

    private static int areaflag(String arg)
     {
	if(arg.equals("-area1")) return  1;
	if(arg.equals("-area2")) return  2;
	return  0;
     }

    public static void intersect_main(String[] av)
     throws Throwable
     {
	if(av.length >= 1    &&
	   (av[0].equals("-help")  ||
	    av[0].equals("--help")  ||
	    av[0].equals("-h")  ||
	    av[0].equals("-?")))
	    errusageI();

	int ac = 0;

	final int DO_AREA = av.length-ac>1 ? areaflag(av[ac]) : 0;
	if(DO_AREA != 0)
	    ++ac;

	if(av.length-ac != 2)
	    errusageI();

	// Open the files, or use STDIN, or read command-line arg AS the file
	BufferedReader file1 = cmdline_file(av[ac+0]);
	BufferedReader file2 = cmdline_file(av[ac+1]);

	// Read the polys
	List p1 = readPolygonList(file1, "FILE1");
	List p2 = readPolygonList(file2, "FILE2");

	announce("  Indexing FILE2");
	Index bins2 = new Index(p2, 180, 90);

	announce("Converting FILE1");
	SPolygon[] p1a = (SPolygon[]) p1.toArray(SPOLYGON_0);
	p1 = null;

	announce("Finding/writing intersections");

	StringBuffer buff = new StringBuffer();
	List intersects = new ArrayList();
	int next_print = 0;
	DecimalFormat f = new DecimalFormat("0.000000");
	for(int i=0; i<p1a.length; i++)
	 {
	    int progress = (int) (100 * i / (float) p1a.length);
	    if(progress == next_print)
	     {
		announce_update(progress + "%");
		++next_print;
	     }
	    intersects.clear();
	    bins2.findIntersections(p1a[i], intersects);
	    String pName = p1a[i].getName();
	    for(Iterator j=intersects.iterator(); j.hasNext(); )
	     {
		SPolygon q = (SPolygon) j.next();
		buff.setLength(0);
		buff.append(pName);
		buff.append('\t');
		buff.append(q.getName());
		if(DO_AREA == 0)
		    buff.append("\t1");
		else
		 {
		    buff.append('\t');
		    buff.append(f.format(area(p1a[i], q, DO_AREA)));
		 }
		out.println(buff);
	     }
	}
     }





//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////






    public static void main(String[] av)
     throws Throwable
     {
	try
	 {
	    if(av.length > 0  &&  av[0].equals("themis_coverage"))
		coverage_main((String[]) Util.cloneArray(av, 1, av.length));
	    else if(av.length > 0  &&  av[0].equals("intersect"))
		intersect_main((String[]) Util.cloneArray(av, 1, av.length));
	    else
	     {
		log.aprintln("Run with: themis_coverage | intersect");
	     }
	 }
	finally
	 {
	    announce(null);
	 }
     }
 }
