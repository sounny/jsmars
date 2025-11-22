package edu.asu.jmars.layer.stamp.radar;

import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.StampLayerSettings;

public class RadarHorizon implements Serializable{
	//TODO probably need to add some identifier of what radargram this belongs to
	private String radarStampID;
	
	//X pixel locations in the full resolution image
	private int[] fullResXPts;
	//Y pixel locations in the full resolution image
	private int[] fullResYPts;
	private ArrayList<Point2D> spatialPts;
	private String myName;
	private Color myColor;
	private String myNote;
	private int lviewWidth;
	private int fullResWidth;
	private int browseWidth;
	private int myId;
	private boolean showMe;
	private ArrayList<HorizonDifference> horizonDifferences;
	private ArrayList<PixelPoint> pixelPoints;
	private double samplingRate;
	
	private DecimalFormat df = new DecimalFormat("##0.00");
	
	public RadarHorizon(String radarID, ArrayList<Integer> xPoints, ArrayList<Integer> yPoints, ArrayList<Point2D> spatialPoints, double sampleRate, int id, StampLayerSettings settings){
		radarStampID = radarID;
		fullResXPts = new int[xPoints.size()];
		fullResYPts = new int[yPoints.size()];
		for(int i=0; i<xPoints.size(); i++){
			fullResXPts[i] = xPoints.get(i);
			fullResYPts[i] = yPoints.get(i);
		}
		
		spatialPts = spatialPoints;
		samplingRate = sampleRate;
		
		myId = id;
		
		pixelPoints = new ArrayList<PixelPoint>();
		
		horizonDifferences = new ArrayList<HorizonDifference>();
		
		//set the defaults
		myName = "";
		myColor = settings.getHorizonColor();
		lviewWidth = settings.getLViewWidth();
		fullResWidth = settings.getFullResWidth();
		browseWidth = settings.getBrowseWidth();
		showMe = true;
	}
	
	
	public int[] getXPoints(){
		return fullResXPts;
	}
	
	public int[] getYPoints(){
		return fullResYPts;
	}
	
	public int[] getXPointsForStartingX(int startX){
		
		int[] result = new int[fullResXPts.length];
		for(int i=0; i<fullResXPts.length; i++){
			result[i] = fullResXPts[i] - startX;
		}
		
		return result;
	}
	
	public int[] getYPointsForStartingY(int startY){
		
		int[] result = new int[fullResYPts.length];
		for(int i=0; i<fullResYPts.length; i++){
			result[i] = fullResYPts[i] - startY;
		}
		
		return result;
	}
	
	public ArrayList<Point2D> getSpatialPoints(){
		return spatialPts;
	}
	
	public int getNumberOfPoints(){
		return fullResYPts.length;
	}
	
	public Color getColor(){
		return myColor;
	}
	
	public String getName(){
		return myName;
	}
	
	/**
	 * Converts the spatialPts array into world coordinates and then
	 * creates a general path which is used to highlight a section
	 * of the footprint (ground track) in the lview where this 
	 * highlight is located.
	 * @param proj  The current projection.
	 * @return  A general path where this horizon is transposed onto 
	 * the surface of the planet.
	 */
	public GeneralPath getWorldPathForProj(ProjObj proj){
		GeneralPath worldPath = new GeneralPath();
		
		//first convert point array to world coords
		ArrayList<Point2D> worldPts = new ArrayList<Point2D>();
		for(Point2D pt : spatialPts){
			worldPts.add(proj.convSpatialToWorld(pt));
		}
		//then create the general path
		for(Point2D pt : worldPts){
			//Start the path
			if(worldPath.getCurrentPoint() == null){
				worldPath.moveTo(pt.getX(), pt.getY());
			}
			//continue the path
			else{
				worldPath.lineTo(pt.getX(), pt.getY());
			}
		}
		
		return worldPath;
	}
	
	
	public void setName(String name){
		myName = name;
	}
	
	public void setColor(Color color){
		myColor = color;
	}
	
	public void setNote(String note){
		myNote = note;
	}
	
	/**
	 * @return A string in the format: 
	 * "lon.##°W, lat.##°N to lon.##°W, lat.##°N"
	 * The first set of coordinates comes from the first
	 * point in the spatialPts array, and the last coordinate
	 * comes from the last point in the spatialPts array.
	 */
	public String getLocationText(){
		String startLon = df.format(spatialPts.get(0).getX());
		String startLat = df.format(spatialPts.get(0).getY());
		String endLon = df.format(spatialPts.get(spatialPts.size()-1).getX());
		String endLat = df.format(spatialPts.get(spatialPts.size()-1).getY());
		
		return startLon+"°W, "+startLat+"°N to "+endLon+"°W, "+endLat+"°N";
	}
	
	public String getNote(){
		return myNote;
	}
	
	public int getFullResWidth(){
		return fullResWidth;
	}
	
	public int getBrowseWidth(){
		return browseWidth;
	}
	
	public int getLViewWidth(){
		return lviewWidth;
	}
	
	public int getID(){
		return myId;
	}
	
	public boolean isVisible(){
		return showMe;
	}
	
	
	public void setFullResWidth(int width){
		fullResWidth = width;
	}
	
	public void setBrowseWidth(int width){
		browseWidth = width;
	}
	
	public void setLViewWidth(int width){
		lviewWidth = width;
	}
	
	public void setVisible(boolean show){
		showMe = show;
	}
	
	
	public ArrayList<HorizonDifference> getHorizonDifferences(){
		return horizonDifferences;
	}
	
	public void addHorizonDifference(HorizonDifference newDiff){
		horizonDifferences.add(newDiff);
	}
	
	public void removeHorizonDifference(HorizonDifference hdToRemove){
		horizonDifferences.remove(hdToRemove);
	}
	/**
	 * @return A list of PixelPoints that contains the x, y values in 
	 * the image, as well as the spatial values for each pixel.
	 */
	public ArrayList<PixelPoint> getPixelPoints() throws Exception{
		if(pixelPoints.size() == 0){
			calculateExportPoints();
		}
		return pixelPoints;
	}
	
	private void calculateExportPoints() throws Exception{
		//populate the exportInfo list with PixelPoint objects 
		// that have their x and y attributes populated
		calculateAllPixels();
		
		//populate those pixel points with their spatial values
		calculateSpatialPointsForPixels();
		
		//populate those pixel points with their image values
		calculateValuesForPixels();
	}
	
	private void calculateAllPixels(){
		//clear the arraylist incase this has been called already
		pixelPoints.clear();
		
		for(int i=0; i<fullResXPts.length; i++){
			double x = fullResXPts[i];
			double y = fullResYPts[i];
			
			//create a pixel point for the first of the pair
			pixelPoints.add(new PixelPoint( (int)x, (int)y));
			
			//find next point and generate all pixel points in between
			if(i+1<fullResXPts.length){
				double x2 = fullResXPts[i+1];
				double y2 = fullResYPts[i+1];
				
				double ydiff = y2-y;
				double xdiff = x2-x;
				
				//if the x difference is 0 (meaning a vertical line),
				// then simply use the same x value for all the y's between these two points
				if(xdiff == 0){
					//cycle through the y's
					int dir = 1;
					if(y2<y){
						dir = -1;
					}
					//if the y values are exactly the same, then 
					// the direction is 0, which will cause the next
					// for loop to just add one point and then exit
					// out of the loop correctly
					if(y2==y){
						dir = 0;
					}
					
					for(int j=(int)y+dir; j!=(int)y2; j = j+dir){
						pixelPoints.add(new PixelPoint((int)x, j));
					}
				}
				//otherwise calculate the slope and extrapolate pixel points in between
				else{
					double m = ydiff/xdiff;
					double b = y-(m*x);
					
					//need to know whether to increase or decrease to get from x to x2
					int dir = 1;
					if(x2<x){
						dir = -1;
					}
					//for every x pixel between x and x2, calculate the y pixel
					for(int k=(int)x+dir; k!=x2; k=k+dir){
						double newYDbl = m*k+b;
						//round the y pixel (ex. 104.### always equals 104, regardless of the decimal value)
						int newY = (int)newYDbl;
						pixelPoints.add(new PixelPoint(k, newY));
					}
				}
			}
		}
	}
	
	/**
	 *  There MAY NOT be a spatial point for each unique x pixel.
	 *  This is why the samplingRate value is used.
	 *	It is necessary to figure out what the smallest x value is and 
	 *	use that as a subtraction map to the spatialpoints 
	 *	array.  
	 *
	 *	ie. If the lowest x pixel value is 322, that
	 *	means that index 0 of the spatial list maps to x pixel
	 *	322.  So use get the spatial value of x pixel 355, it would
	 *  be located at spatial index 355-322=33.
	 */
	private void calculateSpatialPointsForPixels(){		
		//First create a copy of the exportInfo array,
		// and sort it based of the x pixel location
		ArrayList<PixelPoint> exportCopy = new ArrayList<PixelPoint>(pixelPoints);
		Collections.sort(exportCopy, new Comparator<PixelPoint>() {
			public int compare(PixelPoint o1, PixelPoint o2) {
				return ((Integer)o1.getX()).compareTo(o2.getX());
			}
		});
		
		//now record the smallest x value
		int xStart = exportCopy.get(0).getX();
		
		//cycle through all the pixel points and set their spatial point
		for(PixelPoint pp : pixelPoints){
			int xVal = pp.getX();
			int spatialIndex = xVal - xStart;
			
			//account for the fact that the points may not be 1:1 ratio
			// with the pixels (sharad is 1:1, marsis is not)
			spatialIndex = (int)Math.round(spatialIndex*samplingRate);
			
			pp.setSpatialPoint(spatialPts.get(spatialIndex));
		}
	}
	
	
	private void calculateValuesForPixels() throws Exception{
		//convert the list of PixelPoints into a comma separated string
		// to pass to the backend.
		StringBuffer sb = new StringBuffer();
		for(PixelPoint pp : pixelPoints){
			sb.append(pp.getX());
			sb.append(",");
			sb.append(pp.getY());
			sb.append(",");
		}
		//remove the last comma
		String pointStr = sb.substring(0, sb.length()-1);
		
		//create the url string
		String urlStr = "FetchPixelValues?id="+radarStampID+"&instrument=SHARAD&imageType=SHARAD_NUM&pixelValues="+pointStr;
		
		//access the stamp server and cast the result
		ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(urlStr));
		double vals[] = (double[])ois.readObject();
		
		//assign each value to each pixel
		for(int i=0; i<vals.length; i++){
			pixelPoints.get(i).setValue(vals[i]);
		}
		
		ois.close();
	}
}
