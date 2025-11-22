package edu.asu.jmars.layer.groundtrack;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.HttpRequestType;
import edu.asu.jmars.util.JmarsHttpRequest;

public class GroundTrackLayer
 extends Layer
 {
	private static final DebugLog log = DebugLog.instance();

	private static final String server =
		Config.get("groundtrack") + "format=c&";

	private HashMap sections = new HashMap();
	private static final int sectionSize = Integer.parseInt(
		Config.get("groundtrack.chunksize"));

	private int instrumentId;

	public GroundTrackLayer(int instrumentId, SerializedParameters sp)
	 {
		this.instrumentId = instrumentId;
		this.initialLayerData = sp;
	 }

	public synchronized void receiveRequest(Object layerRequest,
											DataReceiver requester)
	 {
		if(layerRequest == null)
			return;

		if(!(layerRequest instanceof Request))
		 {
			log.aprintln("UNKNOWN REQUEST TYPE RECEIVED: " +
						 layerRequest.getClass().getName());
			log.aprintStack(-1);
			return;
		 }

		Request req = (Request) layerRequest;
		requester.receiveData(getTrack(req.begET,
									   req.endET,
									   req.delta)
			);
	 }

	/**
	 ** Returns an array of groundtrack line segments satisfying the
	 ** given time range, each of delta seconds in length in time. The
	 ** actual size of the array will be ceil((endET - begET) / delta).
	 **/
	private synchronized Line2D[] getTrack(long begET,
										  long endET,
										  int delta)
	 {
		setStatus(Color.red);

		int sectionSecs = sectionSize * delta;
		long begSect = (long)Math.floor(begET / (double)sectionSecs);
		long endSect = (long)Math.floor(endET / (double)sectionSecs);
		int begIdx = (int) ((begET-begSect*sectionSecs) % sectionSecs) / delta;
		int endIdx = (int) ((endET-endSect*sectionSecs) % sectionSecs) / delta;
		int count = (int) (endET/delta*delta - begET/delta*delta) / delta;

		Line2D[] segs = new Line2D[count];
		int segsIdx = 0;

		for(long s=begSect; s<=endSect; s++)
		 {
			Line2D[] sect = getSection(s, delta);

			// We may only need part of the data from the
			// starting/ending sections. We'll need all of the data
			// from any middle sections.
			int sectIdx = (s == begSect ? begIdx : 0);
			int copyCount = (s == endSect ? endIdx : sectionSize) - sectIdx;

			try
			 {
				System.arraycopy(sect, sectIdx,
								 segs, segsIdx,
								 copyCount);
			 }
			catch(ArrayIndexOutOfBoundsException e)
			 {
				log.aprintln("begET = " + begET);
				log.aprintln("endET = " + endET);
				log.aprintln("delta = " + delta);
				log.aprintln("sectionSecs = " + sectionSecs);
				log.aprintln("begSect = " + begSect);
				log.aprintln("endSect = " + endSect);
				log.aprintln("begIdx = " + begIdx);
				log.aprintln("endIdx = " + endIdx);
				log.aprintln("count = " + count);
				log.aprintln("s = " + s);
				log.aprintln("sect.length = " + sect.length);
				log.aprintln("segs.length = " + segs.length);
				log.aprintln("sectIdx = " + sectIdx);
				log.aprintln("segsIdx = " + segsIdx);
				log.aprintln("copyCount = " + copyCount);
				log.aprintln(e);
				setStatus(Color.blue);
				throw  e;
			 }
			segsIdx += copyCount;
		 }

		if(segsIdx != count)
			log.aprintln("WRONG NUMBER OF SEGMENTS!!!!!!!!!!!!!!!!!!!!!!!!!!");

		return  segs;
	 }

	/**
	 ** Returns an array of {@link #sectionSize} groundtrack line
	 ** segments, each of which is <code>delta</code> seconds in
	 ** length in time.
	 **/
	private Line2D[] getSection(long sect, int delta)
	 {
		SectionKey key = new SectionKey(sect, delta);
		Line2D[] section = (Line2D[]) sections.get(key);

		if(section == null)
		 {
			URL url = getRemoteUrl(key);

			if(url == null)
				throw  new Error("CAN'T FORM PROPER GROUNDTRACK URL");

			sections.put(key, section = readSection(url));
		 }

		return  section;
	 }

	/**
	 ** Returns a url to a local file containing {@link
	 ** #sectionSize}+1 grid points for constructing groundtrack
	 ** segments.
	 **/
	private static URL getLocalUrl(SectionKey key)
	 {
		String fname = "GRIDS/t_" + key.section + "_" + key.delta;

		try
		 {
			return new File(fname).toURL();
		 }
		catch(MalformedURLException e)
		 {
			log.aprintln("Java didn't like this filename: " + fname);
			return  null;
		 }
	 }

	/**
	 ** Returns a url to a server cgi that will supply {@link
	 ** #sectionSize}+1 grid points for constructing groundtrack
	 ** segments.
	 **/
	private URL getRemoteUrl(SectionKey key)
	 {
		String query =
			"xmin=" + key.section * sectionSize * key.delta +
			"&xcount=" + (sectionSize+1) +
			"&xdelta=" + key.delta +
			"&ymin=" + 0 +
			"&ycount=" + 1 +
			"&ydelta=" + 1 + // doesn't matter
			"&ship=" + instrumentId +
			"&key=" + Main.KEY;

		try
		 {
			return  new URL(server + query);
		 }
		catch(MalformedURLException e)
		 {
			log.aprintln("Java didn't like this URL: " + server + query);
			return  null;
		 }
	 }

	/**
	 ** Used in {@link #newZeroSection} below.
	 **/
	private static Line2D[] zeroSection;
	static
	 {
		Line2D zeroSegment = new Line2D.Double();
		zeroSection = new Line2D.Double[sectionSize];
		for(int i=0; i<sectionSize; i++)
			zeroSection[i] = null;
	 }

	/**
	 ** Returns an array of {@link #sectionSize} line segments, all of
	 ** which refer to the same { (0,0) (0,0) } line segment.
	 **/
	private static Line2D[] newZeroSection()
	 {
		return  (Line2D[]) zeroSection.clone();
	 }

	/**
	 ** Returns an array of {@link #sectionSize} line segments, read
	 ** from the given url location. Returns null on error.
	 **/
	private synchronized Line2D[] readSection(URL url)
	 {
		log.println("Using url " + url);
		

		// Create a connection to that url
		BufferedReader fin;
		try
		 {
		     String urlStr = url.getProtocol() + "://" + url.getHost() + url.getPath();		     
		     String params = url.getQuery();
             JmarsHttpRequest request = new JmarsHttpRequest(urlStr, HttpRequestType.GET);
//             request.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
             request.addOutputData(params);
             request.setConnectionTimeout(10*1000);
             request.setReadTimeout(1*10*1000);
             boolean status = request.send();
             if (!status) {
                 int httpStatus = request.getStatus();
                 System.out.println(httpStatus);
             }
		     
//			 fin = new BufferedReader(new InputStreamReader(url.openStream()));
             fin = new BufferedReader(new InputStreamReader(request.getResponseAsStream()));
		 }
		catch(Exception e)
		 {
			log.aprintln("ERROR OPENING URL " + url);
			log.print(e);
			return  null;
		 }

		Line2D[] segs = newZeroSection();

		Point2D.Double lastPt = new Point2D.Double();

		// Read the data
		HVector v = HVector.read(fin);
		if(v == null)
		 {
			log.aprintln("DIDN'T RECEIVE ANY DATA FROM URL " + url);
			return  null;
		 }
		v.toLonLat(lastPt);
		for(int i=0; i<sectionSize; i++)
		 {
			v = HVector.read(fin);
			if(v == null)
			 {
				log.aprintln("RECEIVED SOME BAD DATA FROM URL " + url);
				return  null;
			 }

			// Java args are evaluated left-to-right (JLS 15.7.4)
			segs[i] = new Line2D.Double(lastPt.x,
										lastPt.y,
										lastPt.x = v.lon(),
										lastPt.y = v.lat());
		 }

		return  segs;
	 }

	final static class Request
	 {
		final long begET;
		final long endET;
		final int delta;

		Request(double begET, double endET, int delta)
		 {
			if(endET < begET)
				throw  new Error("Illegal groundtrack request parameters");
			this.begET = (long) Math.floor(begET / delta) * delta;
			this.endET = (long) Math.ceil(endET / delta) * delta;
			this.delta = delta;
		 }

		public boolean equals(Object obj)
		 {
			if(obj != null  &&  obj instanceof Request)
			 {
				Request req = (Request) obj;
				return  req.begET == begET
					&&  req.endET == endET
					&&  req.delta == delta;
			 }
			else
				return  false;
		 }

		public int hashCode()
		 {
			return  (int) (begET ^ endET ^ delta);
		 }
	 }

	private static class SectionKey
	 {
		final long section;
		final int delta;

		SectionKey(long section, int delta)
		 {
			this.section = section;
			this.delta = delta;
		 }

		public int hashCode()
		 {
			return  (int) section ^ delta;
		 }

		public boolean equals(Object obj)
		 {
			if(obj != null  &&  obj instanceof SectionKey)
			 {
				SectionKey key = (SectionKey) obj;
				return  key.section == section
					&&  key.delta == delta;
			 }

			return  false;
		 }
	 }
 }
