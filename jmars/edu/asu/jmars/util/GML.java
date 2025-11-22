package edu.asu.jmars.util;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



/**
 * A generalized class for parsing out GML files to an array of GML.Features and for writing out an array of 
 * GML.Features to GML files.
 *
 * @author James Winburn MSFF-ASU 3/22/06
 */
  
public class GML {
        
	// type indicators
	public static final int     NONE    =0;
	public static final int     POINT   =1;
	public static final int     LINE    =2;
	public static final int     POLYGON =3;
	


	/**
	 * converts a java-like string which doesn't care about characters like &'s and <'s 
	 * into a html string, which cares about them a great deal.
	 */
 	public static String javaStringToHtml( String str){
		StringBuffer oldStr = new StringBuffer(str);
		StringBuffer newStr = new StringBuffer();

		for (int i=0; i< oldStr.length(); i++){
			if (oldStr.charAt(i)=='"'){
				newStr.append("&quot;");
			} else if (oldStr.charAt(i)=='&'){
				newStr.append("&amp;");
			} else if (oldStr.charAt(i)=='<'){
				newStr.append("&lt;");
			} else if (oldStr.charAt(i)=='>'){
				newStr.append("&gt;");
			} else {
				newStr.append( oldStr.charAt(i));
			}
		}
		return newStr.toString();
	}



	/**
	 * a class for maintaining the Features gleened from an html file.
	 * Note that although this class has a facility for storing a 
	 * description, the field is not currently used in JMARS.
	 */
	public static class Feature {
		
		// fields
		private int         type;
		private Shape       path;
		private String      id;
		private String      description;
		
		// single constructor
		public Feature( int t, Shape p, String i, String d){
			type        = t;
			path        = p;
			id          = i;
			description = d;
		}
		
		// returns the type of the feature.
		public int getType(){
			return type;
		}
		
		// returns the general path of the feature.
		public Shape getShape(){
			return path;
		}
		
		// returns the description of the feature
		// returns null if there was no description
		public String getDescription(){
			return description;
		}
		
		// returns the id of the feature
		// returns null if there was no id
		public String getId(){
			return id;
		}
	}


        

	/**
	 * a class for handling the reading and writing of GML files.
	 */
	public static class File {

		// a formatter for output float values.
		private static DecimalFormat    decimal         = new DecimalFormat("#.######");
		
		
		/**
		 * parses the inputted file and returns an array of GML.Features
		 */
		public static GML.Feature [] read( String file){
			GetGML getGML = new GetGML( file);
			if (getGML==null){
				return null;
			} else {
				return getGML.getFeatures();
			}
		}
		
		
		/**
		 * takes an array of GML.Features and writes out GML code to the inputted file.
		 */
		public static int write( GML.Feature [] features, String file)
		{
			// flag to control whether we need to write the outer "Polygon" tag.
			boolean writingPolygons = false;
			
			int count=0;
			
			try{
				BufferedWriter  outStream = new BufferedWriter(new FileWriter( file));
				outStream.write("<JMARS srsName=\"EPSG:4326\">\n");
				for (int i=0; i< features.length; i++){
					GML.Feature f = features[i];
					Shape shape = f.getShape();
					AffineTransform at = new AffineTransform();
					PathIterator pathIterator = shape.getPathIterator(at);
					switch (f.getType())
						{
						case GML.POINT: 
							if (writingPolygons==true) {
								outStream.write("\t</gml:Polygon>\n");
								writingPolygons=false;
							}
							outStream.write("\t<gml:Point ");
							if (f.getId() != null) {
								outStream.write( " gml:id=\"" + javaStringToHtml(f.getId()) + "\" " );
							}
							if (f.getDescription() != null) {
								outStream.write( " gml:description=\"" + javaStringToHtml(f.getDescription()) + "\" " );
							}
							outStream.write(">\n");
							outputPos( outStream, pathIterator);
							outStream.write("\t</gml:Point>\n");
							count++;
							break;
						case GML.LINE:
							if (writingPolygons==true) {
								outStream.write("\t</gml:Polygon>\n");
								writingPolygons=false;
							}
							outStream.write("\t<gml:LineString ");
							if (f.getId() != null) {
								outStream.write( " gml:id=\"" + javaStringToHtml(f.getId()) + "\" " );
							}
							if (f.getDescription() != null) {
								outStream.write( " gml:description=\"" + javaStringToHtml(f.getDescription()) + "\" " );
							}
							outStream.write(">\n");
							outputPos( outStream, pathIterator);
							outStream.write("\t</gml:LineString>\n");
							count++;
							break;
						case GML.POLYGON:
							if (writingPolygons==false) {
								outStream.write("\t<gml:Polygon>\n");
								writingPolygons=true;
							}
							outStream.write("\t\t<gml:LinearRing ");
							if (f.getId() != null) {
								outStream.write( " gml:id=\"" + javaStringToHtml(f.getId()) + "\" " );
							}
							if (f.getDescription() != null) {
								outStream.write( " gml:description=\"" + javaStringToHtml(f.getDescription()) + "\" " );
							}
							outStream.write(">\n");
							outputPos( outStream, pathIterator);
							outStream.write("\t\t</gml:LinearRing>\n");
							count++;
							break;
						default: 
							break;
						}
				} 
				if (writingPolygons==true) {
					outStream.write("\t</gml:Polygon>\n");
					writingPolygons=false;
				}
				outStream.write("</JMARS>\n");
				outStream.flush();
				outStream.close();
			} catch (IOException e){
				System.out.println("error writing file " + file);
			}

			return count;
		}
		
		
		// writes out the points of the General Path as "pos" objects.
		// note: unlike posList, only a single x-y pair is written out for each pos object.
		private  static void outputPos( BufferedWriter outStream, PathIterator pathIterator){
			float[] coords          = new float[6];
			float [] beginPt        = null;
			while (!pathIterator.isDone()) {
				int segResult = pathIterator.currentSegment( coords);
				if (segResult == PathIterator.SEG_MOVETO) {
					beginPt = new float[2];
					beginPt[0]= coords[0];
					beginPt[1]=coords[1];
					try{
						outStream.write("\t\t\t<gml:pos>" + 
								decimal.format( coords[0]) +"," + decimal.format( coords[1]) + 
								"</gml:pos>\n");
					} catch (java.io.IOException jio){}
				} else if (segResult==PathIterator.SEG_LINETO ){
					try{
						outStream.write("\t\t\t<gml:pos>" + 
								decimal.format( coords[0]) +"," + decimal.format( coords[1]) + 
								"</gml:pos>\n");
					} catch (java.io.IOException jio){}
				} else if (segResult==PathIterator.SEG_CLOSE && beginPt!=null) {
					try{
						outStream.write("\t\t\t<gml:pos>" + 
								decimal.format( beginPt[0]) +"," + decimal.format( beginPt[1]) + 
								"</gml:pos>\n");
					} catch (java.io.IOException jio){}
				}
				pathIterator.next();
			}
		}
		
		
		// parses a GML file to get a list of features.
		// Note that because of the hideous complexity of GML, we only concern ourselves
		// with the basic feature types (point, line, and polygon).  Anything else in 
		// the file is tacitly ignored.
		private static class GetGML extends DefaultHandler {
			
			
			/**
			 * @param file - InputStream of the GML to be parsed.
			 */
		public GetGML( String fileName){
				
				try {
					SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
					
					//  Parse the capabilities file.
					try {
						FileReader r = new FileReader( fileName);
						if (r!=null){
							parser.parse(new InputSource( r), this);
						} //removed dead code
					} catch (Exception e){
						Util.showMessageDialog("Error loading GML file: " + fileName + "\n");
	
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			
			/**
			 * returns the features that were parsed out of the file.
			 * Note: GetGML() must be called before this or it will return null.
			 */
			public GML.Feature []  getFeatures(){
				return (GML.Feature[])features.toArray( new GML.Feature[0]);
			}
			
			
			
			
			// What we are trying to gleen from the documents by parsing them out.
			private ArrayList  features      = new ArrayList();
			
			// parser objects.
			private StringBuffer  content    = new StringBuffer();
			
			
			
			// The remaining methods are standard SAX methods and
			// will not be documented.
			
			public void characters( char[] chars, int start, int len) throws SAXException {
				content.append( chars, start, len);
			}
			
			
			public void startElement ( String uri, String localName, String qName, Attributes attrs) 
			{
				// This is a  quick hack to make this code continue to work.  Ideally, we should write
				// a handler that fully supports GML, or utilize one of the existing GML libraries out there.
				if (localName.length()==0 && qName.startsWith("gml:")) {
					localName = qName.substring(4);
				}
				
				if (localName.equals("Point")){
					shapeType = GML.POINT;
				}  
				else if (localName.equals("LineString")){
					shapeType = GML.LINE;
				}
				else if (localName.equals("Polygon")){
					shapeType = GML.POLYGON;
				}
				else if (localName.equals("LinearRing")){
					// A linear-ring can only exist within a polygon.
					if (shapeType != GML.POLYGON){
						shapeType = GML.NONE;
					}
				}
				// Load up the id and the description, but only if the element is a shape.
				if (localName.equals("Point") || 
				    localName.equals("LineString") ||
				    localName.equals("Polygon") ||
				    localName.equals("LinearRing")){
					description = null;
					id          = null;
					if (attrs.getLength() > 0){
						description = attrs.getValue("gml:description");
						id          = attrs.getValue("gml:id");
					}
				}

				// reset the content.
				content.setLength(0);
			} 
			

			GeneralPath generalPath;
			int         pointCount;
			int         shapeType   = GML.NONE;
			String      description;
			String      id;
			
			public void endElement( String uri, String localName, String qName) 
			{
				// This is a  quick hack to make this code continue to work.  Ideally, we should write
				// a handler that fully supports GML, or utilize one of the existing GML libraries out there.
				if (localName.length()==0 && qName.startsWith("gml:")) {
					localName = qName.substring(4);
				}
				
				if (localName.equals("posList") ||
				    localName.equals("coords") || localName.equals("coordinates")  // these last two are deprecated as of GML 3.1
				    ){
					String [] token = content.toString().trim().split("[ \t,;\n]+");
					pointCount  = 0;
					generalPath = null;
					generalPath = new GeneralPath();
					generalPath.moveTo( new Float(token[0]).floatValue(), new Float(token[1]).floatValue());
					pointCount++;
					for (int i=2; i<token.length && i+1<token.length; i=i+2){
						generalPath.lineTo( new Float(token[i]).floatValue(), new Float(token[i+1]).floatValue());
						pointCount++;
					}
				}
				else if (localName.equals("pos")) {
					String [] token = content.toString().trim().split("[ \t,;\n]+");
					if (generalPath==null){
						generalPath = new GeneralPath();
						generalPath.moveTo( new Float(token[0]).floatValue(), new Float(token[1]).floatValue());
						pointCount=1;
					} else {
						generalPath.lineTo( new Float(token[0]).floatValue(), new Float(token[1]).floatValue());
						pointCount++;
					}
				}
				else if (localName.equals("Point")){
					// pointList can be null if there was not a valid list of floats in content.
					if (shapeType==GML.POINT && generalPath!=null && pointCount==1){ 
						features.add( new GML.Feature( GML.POINT, generalPath, id, description));
					}
					shapeType = GML.NONE;
					pointCount = 0;
					generalPath = null;
				}
				else if (localName.equals("LineString")){
					// pointList can be null if there was not a valid list of floats in content.
					if (shapeType==GML.LINE && generalPath!=null && pointCount>=2){
						features.add( new GML.Feature( GML.LINE, generalPath, id, description));
					}
					shapeType = GML.NONE;
					pointCount = 0;
					generalPath = null;
				}
				// A polygon can consist of several rings, so when we get one ring, we
				// have to allow for the definition of more rings. 
				// Note that JMARS does not distinguish between interior and exterior rings.  
				else if (localName.equals("LinearRing")) {
					// pointList can be null if there was not a valid list of floats in content.
					if (shapeType==GML.POLYGON && generalPath!=null && pointCount>=3){ 
						generalPath.closePath();
						features.add( new GML.Feature( GML.POLYGON, generalPath, id, description));
						pointCount = 0;
						generalPath = null;
					}
				}
				else if (localName.equals("Polygon")){
					if (shapeType==GML.POLYGON){ 
						shapeType = GML.NONE;
					}
				}
			} //end: endElement()
			

		} // end: class GetFeatureTable
		
	} // end: class File

}
