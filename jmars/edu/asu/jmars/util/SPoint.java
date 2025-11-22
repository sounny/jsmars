package edu.asu.jmars.util;

import gnu.trove.*;
import java.text.*;
import java.util.*;

public class SPoint
 {
    private static final DebugLog log = DebugLog.instance();
    private static final DecimalFormat f = new DecimalFormat("0.000");

    private final HVector v;
    private final float lon;
    private final float lat;

    private static final SPoint_indexed[] SPOINT_I0 = new SPoint_indexed[0];
    private static final SPoint        [] SPOINT_0  = new SPoint        [0];
    private static final PointPair[] POINTPAIR_0 = new PointPair[0];

    private final static class SPoint_indexed extends SPoint
     {
	private final SPoint p;
	private final int k;
	SPoint_indexed(SPoint p, int k)
	 {
	    super(p);
	    this.p = p;
	    this.k = k;
	 }
     }

    private SPoint(SPoint p)
     {
	this.v = p.v;
	this.lon = p.lon;
	this.lat = p.lat;
     }

    public SPoint(HVector v)
     {
	this.v = v.unit();
	this.lon = (float) v.lon();
	this.lat = (float) v.lat();
     }

    public SPoint(double x, double y, double z)
     {
	this.v = new HVector(x, y, z);
	v.normalize();
	this.lon = (float) v.lon();
	this.lat = (float) v.lat();
     }

    public SPoint(double lon, double lat)
     {
	this.v = newHVector(lon, lat);
	lon -= Math.floor(lon / 360) * 360;
	this.lon = (float) lon;
	this.lat = (float) lat;
     }

    public String toString()
     {
	return  lon + "," + lat;
     }

    public double getLon()
     {
	return  lon;
     }

    public double getLat()
     {
	return  lat;
     }

    public static HVector newHVector(double lon, double lat)
     {
	lon = Math.toRadians(lon);
	lat = Math.toRadians(lat);
	return  new HVector(Math.cos(lat) * Math.cos(lon),
			    Math.cos(lat) * Math.sin(lon),
			    Math.sin(lat));
     }

    public static final class Index
     {
	private final SPolygon[][] binOutlines;

	public final int LONBIN_COUNT;
	public final int LATBIN_COUNT;
	public final int LONBIN_SZ;
	public final int LATBIN_SZ;

	SPoint_indexed[][][] bins;
	SPoint_indexed[] allPoints;

	public Index(List points, int LONBIN_COUNT, int LATBIN_COUNT)
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

	    binOutlines = SPolygon.createBinOutlines(LONBIN_COUNT,
						     LATBIN_COUNT);
	    bins = new SPoint_indexed[LONBIN_COUNT][LATBIN_COUNT][];

	    allPoints = new SPoint_indexed[points.size()];
	    for(int i=0; i<allPoints.length; i++)
		allPoints[i] = new SPoint_indexed((SPoint) points.get(i), i);

	    List[][] binsTmp = new List[LONBIN_COUNT][LATBIN_COUNT];
	    for(int i=0; i<allPoints.length; i++)
	     {
		SPoint p = allPoints[i];
		int bx = (int) Math.floor(p.lon / LONBIN_SZ);
		int by = (int) Math.floor(p.lat / LATBIN_SZ) + LATBIN_COUNT/2;
		if(by >= LATBIN_COUNT) by = LATBIN_COUNT-1;
		if(binsTmp[bx][by] == null)
		    binsTmp[bx][by] = new ArrayList();
//		log.println("Adding " + p + " to " + bx + ":" + by);
		binsTmp[bx][by].add(p);
	     }

	    for(int i=0; i<bins.length; i++)
		for(int j=0; j<bins[i].length; j++)
		    if(binsTmp[i][j] == null)
			bins[i][j] = SPOINT_I0;
		    else
			bins[i][j] = (SPoint_indexed[])
			    binsTmp[i][j].toArray(SPOINT_I0);
	 }

	/**
	 * Returns dynamically-allocated array of points from the
	 * index that are within a given radius of a given point.
	 */
	public List findPoints(SPoint p, float radiusDegs, List intersections)
	 {
	    if(true)
		throw new Error("Unoptimized");
	    if(intersections == null)
		intersections = new ArrayList();

	    final double radiusRads = Math.toRadians(radiusDegs);
	    final float maxDist = 1 - (float) Math.cos(radiusRads);

	    float minLat = p.lat - radiusDegs;
	    float maxLat = p.lat + radiusDegs;
	    float lonRadius = (float) Math.toDegrees(
		Math.acos(radiusRads / Math.sqrt(p.v.x*p.v.x + p.v.y*p.v.y)));
	    float minLon = (p.lon - lonRadius + 360) % 360;
	    float maxLon = (p.lon + lonRadius      ) % 360;

	    // Perform the hit tests
	    int minx = (int) Math.floor(minLon / LONBIN_SZ);
	    int maxx = (int) Math.floor(maxLon / LONBIN_SZ);
	    if(maxx < minx) maxx += LONBIN_COUNT;
	    int miny = (int)Math.floor(minLat/LATBIN_SZ)+LATBIN_COUNT/2;
	    int maxy = (int)Math.floor(maxLat/LATBIN_SZ)+LATBIN_COUNT/2;
	    if     (miny < 0)             miny = 0;
	    else if(miny >= LATBIN_COUNT) miny = LATBIN_COUNT-1;
	    if     (maxy >= LATBIN_COUNT) maxy = LATBIN_COUNT-1;

	    // Optimized case: the search covers only a single bin
// 	    if(minx == maxx  &&  miny == maxy)
// 	     {
// 		SPolygon[] bin = bins[minx][miny];
// 		for(int j=0; j<bin.length; j++)
// 		    if(p.v.dot(bin[j].v) < maxDist)
// 			intersections.add(bin[j]);
// 	     }
	    // Non-optimized: search covers multiple bins
// 	    else
// 	     {
		for(int bx=minx; bx<=maxx; bx++)
		    for(int by=miny; by<=maxy; by++)
		     {
			SPoint[] bin = bins[bx][by];
			for(int j=0; j<bin.length; j++)
			    if(1 - (float) p.v.dot(bin[j].v) < maxDist)
				intersections.add(bin[j]);
		     }
// 	     }

	    return intersections;
	 }

	public SPoint[][] findClusters(double radiusDegs)
	 {
	    // Get all pairs within the radius
	    log.println("Finding candidate pairs...");
	    TLongFloatHashMap map = findPairMapping(radiusDegs);

	    // Create a distance-sorted list of all pairs within the radius
	    log.println("Sorting pairs...");
	    PointPair[] pairs = convertPairMappingToArray(map);
	    Arrays.sort(pairs, PointPair.compareByDistance);

/*
	    for(int i=0; i<pairs.length; i++)
	     {
		long msb = pointsToIndexes.get(pairs[i].a);
		long key = (msb << 32) | pointsToIndexes.get(pairs[i].b);
		log.println(pairs[i].a + "\t" + pairs[i].b + "\t" +
			    f.format(Math.toDegrees(pairs[i].d)) + "\t" +
			    Long.toHexString(key) + " = " +
			    f.format(Math.toDegrees(map.get(key))));
	     }
*/

	    // Group the pairs into clusters, in order of closest to
	    // furthest pairings.
	    log.println("Grouping into clusters...");
	    List clusterList = createClusters(pairs, radiusDegs);

	    // Convert the cluster list into an array of arrays.
	    log.println("Converting clusters to arrays...");
	    SPoint[][] clusterArray = new SPoint[clusterList.size()][];
	    for(int i=0; i<clusterArray.length; i++)
	     {
		List clust = (List) clusterList.get(i);
		clusterArray[i] = (SPoint[]) clust.toArray(SPOINT_0);
		for(int j=0; j<clusterArray[i].length; j++)
		    clusterArray[i][j] =
			((SPoint_indexed) clusterArray[i][j]).p;
	     }

	    return  clusterArray;
	 }

	private List createClusters(PointPair[] pairs, final double radiusDegs)
	 {
	    final double radiusRads = Math.toRadians(radiusDegs);
	    final float maxDist = 1 - (float) Math.cos(radiusRads);

	    log.println("Looking for dist <= " + maxDist);

	    List clusterList = new ArrayList(); // list of List<SPoint>
	    Map  clusterMap  = new HashMap(); // maps SPoint to List<SPoint>

	    for(int i=0; i<pairs.length; i++)
	     {
		PointPair pp = pairs[i];
		List clustA = (List) clusterMap.get(pp.a);
		List clustB = (List) clusterMap.get(pp.b);
		if(clustA == null  &&  clustB == null)
		 {
//		    log.println("Creating " + pp.a + " " + pp.b + "\t" +
//				Math.toDegrees(pp.d));
		    // Establish a new cluster with this pairing.
		    clusterList.add(clustA = clustB = new ArrayList());
		    clustA.add(pp.a); clusterMap.put(pp.a, clustA);
		    clustB.add(pp.b); clusterMap.put(pp.b, clustB);
		 }
		else if(clustA != null  &&  clustB != null)
		 {
		    // Both of them are in a cluster... if they're
		    // different clusters, then we need to merge them.
		    if(clustA != clustB)
		     {
			// It's faster to merge the smaller one into
			// the larger one.
			if(clustA.size() < clustB.size())
			 {
			    List tmp = clustA;
			    clustA = clustB;
			    clustB = tmp;
			 }
			for(Iterator j=clustB.iterator(); j.hasNext(); )
			    clusterMap.put(j.next(), clustA);
			clustA.addAll(clustB);
			clusterList.remove(clustB);
		     }
		 }
		else
		 {
		    // Exactly one of them is in a cluster, add the
		    // new point to that cluster.
		    final SPoint point;
		    final List   clust;
		    if(clustA == null  &&
		       clustB != null) { point = pp.a; clust = clustB; }
		    else               { point = pp.b; clust = clustA; }

		    // Finally, add the new member to the existing cluster.
//		    log.println("Adding " + point + " to " +
//				(pp.a==point ? pp.b : pp.a) + "\t" +
//				Math.toDegrees(pp.d));
		    clust.add(point);
		    clusterMap.put(point, clust);
		 }
	     }

	    return  clusterList;
	 }

	public PointPair[] findPairs(double radiusDegs)
	 {
	    TLongFloatHashMap map = findPairMapping(radiusDegs);
	    return  convertPairMappingToArray(map);
	 }

	private PointPair[] convertPairMappingToArray(TLongFloatHashMap map)
	 {
	    final PointPair[] pairs = new PointPair[map.size()];

	    TLongFloatIterator iter = map.iterator();
	    for(int i=0; i<pairs.length; i++)
	     {
		iter.advance();
		long key = iter.key();
		pairs[i] = new PointPair(allPoints[(int) (key >> 32)],
					 allPoints[(int)  key       ],
					 iter.value());
	     }

	    return  pairs;
	 }

	private static double[] createLonRadii(final double radiusRads)
	 {
	    double[] lonRadii = new double[90];
	    for(int i=0; i<lonRadii.length; i++)
	     {
		double lat = Math.toRadians(i + 1);
		double dist = Math.cos(lat);
		if(dist > radiusRads)
		    lonRadii[i] = Math.toDegrees(Math.acos(radiusRads / dist));
		else
		    lonRadii[i] = 180;
	     }

	    return  lonRadii;
	 }

	public TLongFloatHashMap findPairMapping(final double radiusDegs)
	 {
	    double[] lonRadii = createLonRadii(radiusDegs);

	    final double radiusRads = Math.toRadians(radiusDegs);
	    final float maxDist = 1 - (float) Math.cos(radiusRads);

	    // Construct a set of all pairs of points within the given radius
	    TLongFloatHashMap map = new TLongFloatHashMap();
	    for(int i=0; i<allPoints.length; i++)
	     {
		final SPoint p = allPoints[i];
		final long msb = (long)i << 32;

		double minLat = p.lat - radiusDegs;
		double maxLat = p.lat + radiusDegs;
		int latIdx = (int) Math.ceil(Math.abs(p.lat));
		double lonRadius;
		if(latIdx != 90  &&  lonRadii[latIdx] < 180)
		    lonRadius = lonRadii[latIdx];
		else
		    lonRadius = 180;

		double minLon = (p.lon - lonRadius + 360) % 360;
		double maxLon = (p.lon + lonRadius      ) % 360;
		if(lonRadius == 180)
		    maxLon = minLon + 360;

		// Perform the hit tests
		int minx = (int) Math.floor(minLon / LONBIN_SZ);
		int maxx = (int) Math.floor(maxLon / LONBIN_SZ);
		if(maxx < minx) maxx += LONBIN_COUNT;
		int miny = (int)Math.floor(minLat/LATBIN_SZ)+LATBIN_COUNT/2;
		int maxy = (int)Math.floor(maxLat/LATBIN_SZ)+LATBIN_COUNT/2;
		if     (miny < 0)             miny = 0;
		else if(miny >= LATBIN_COUNT) miny = LATBIN_COUNT-1;
		if     (maxy >= LATBIN_COUNT) maxy = LATBIN_COUNT-1;

		for(int bx=minx; bx<=maxx; bx++)
		    for(int by=miny; by<=maxy; by++)
		     {
			SPoint_indexed[] bin = bins[bx % LONBIN_COUNT][by];
			for(int j=0; j<bin.length; j++)
			 {
			    SPoint_indexed q = bin[j];
//			    int k = pointsToIndexes.get(q);
			    int k = q.k;
			    if(k > i)
			     {
				float d = 1 - (float) p.v.dot(((SPoint)q).v);
				if(d < maxDist)
				 {
				    long key = msb | k;
//				    log.println(p + "\t" + q + "\t= " +
//						Math.toDegrees(d) +
//						"\t" + Long.toHexString(key));
				    map.put(key, d);
				 }
			     }
			 }
		     }
	     }

	    return  map;
	 }
     }

    public static final class PointPair
     {
	public final SPoint a;
	public final SPoint b;
	public final float d;

	private PointPair(SPoint a, SPoint b, float d)
	 {
	    this.a = a;
	    this.b = b;
	    this.d = d;
	 }

	public int hashCode()
	 {
	    return  a.hashCode() ^ b.hashCode();
	 }

	public boolean equals(Object o)
	 {
	    return  o instanceof PointPair // false for null
		&&  ((PointPair)o).a == a
		&&  ((PointPair)o).b == b;
	 }

	private static Comparator compareByDistance =
	new Comparator()
	 {
	    public int compare(Object o1, Object o2)
	     {
		PointPair p1 = (PointPair) o1;
		PointPair p2 = (PointPair) o2;
		if(p1.d < p2.d) return  -1;
		if(p1.d > p2.d) return  +1;
		return  0;
	     }
	 }
	;
     }
 }
