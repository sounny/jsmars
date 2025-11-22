package edu.asu.jmars.layer.streets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HttpRequestType;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.MovableList;
import edu.asu.jmars.util.Util;


public class OpenStreetMapTiles {

	private int maxTileX1 = -1;
	private int minTileX1 = 100000000;
	private int maxTileY1 = -1;
	private int minTileY1 = 100000000;
	private int nTilesX = 0;
	private int nTilesY = 0;
	private final int tileSize = 256;
	private int zoom1 = 0;
	private double xTileLonW = 0.0;
	private double xTileLonE = 0.0;
	private double odetic2ocenN1 = 0.0;
	private double odetic2ocenS1 = 0.0;
	private int xTile = 0;
	private int yTile = 0;
	private BufferedImage scaledImage1 = null;
	private int northLatMinusSouth = 0;
	private int westLonMinusEast = 0;
	private int height = 0;
	private int width = 0;
	private Point2D southPoint;
	private Point2D northPoint;
	String threadUrl = null;
	static protected ExecutorService threadPool;
	protected CountDownLatch countDownStart;
	protected CountDownLatch countDownEnd;
	BufferedImage outputImage = null;
	MediaTracker tracker = null;
	StreetLayer layer = null;
	private int osmTypeInt;
	private String tileType;
	private double equatRadius;
	private double polarRadius;
	private double earthFlattening;
	private double odeticToOcenScale;
	private double deg2Rad;
	private double rad2Deg;
	private String osmUrl;
	private static final String tempOSMFile = Main.getJMarsPath() + "cache" + File.separator + "osmCachedTiles";

	
	private static DebugLog log = DebugLog.instance();
	
	public OpenStreetMapTiles(int osmType) {
		this.osmTypeInt = osmType;
		String equatRadiusSt = Config.get(Util.getProductBodyPrefix()+ "equat_radius");
		String polarRadiusSt = Config.get(Util.getProductBodyPrefix()+ "polar_radius");
		this.equatRadius = Double.parseDouble(equatRadiusSt);
		this.polarRadius = Double.parseDouble(polarRadiusSt);
		this.earthFlattening = 1.0 - (polarRadius/equatRadius);
		this.odeticToOcenScale = (1.0-earthFlattening)*(1.0-earthFlattening);
		this.osmUrl = Config.get(Util.getProductBodyPrefix()+ "osm_url");
		this.deg2Rad = Math.PI/180;
		this.rad2Deg = 180.0/Math.PI;	
		
		if (osmTypeInt == 0 ){
			tileType = "sat";
		}else if (osmTypeInt== 1){
			tileType = "map";
		}		
	}
	  public OpenStreetMapTiles() {
	  }

	public void main(String [] args){
		
		//Stand alone test for boundingBox
		  try {
				BoundingBoxDataObject testBox = new BoundingBoxDataObject(null, null, -115.00, -109.00, 37.00, 31.00, 2);

		  } catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
		  }
	}

	public void callStatus (StreetLayer layer) {
		this.layer = layer;
	}
	
	private void getTileNumbers(BoundingBoxDataObject boundingBox, int zoom) {
			
		//Grab bounding points from the JMARS main view window
		double lonDegW = boundingBox.getWestLon();
		double latDegN = boundingBox.getNorthLat();
		double lonDegE = boundingBox.getEastLon();
		double latDegS = boundingBox.getSouthLat();
		
		//Convert North and South latitude to Odetic 
		double ocen2odeticN = ((rad2Deg)*Math.atan(Math.tan((deg2Rad)*(latDegN))/(odeticToOcenScale)));    
		double ocen2odeticS = ((rad2Deg)*Math.atan(Math.tan(deg2Rad*(latDegS))/(odeticToOcenScale)));    		
		
		//Calculate the tiles that cover that area from each corner. Defined by OSM
		int xTile = (int)Math.floor( (lonDegW + 180) / 360 * (1<<zoom) ) ; 
		int yTile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(ocen2odeticN)) + 1 / Math.cos(Math.toRadians(ocen2odeticN))) / Math.PI) / 2 * (1<<zoom) ) ;
		compareMinMaxValues(xTile, yTile);
		
		xTile = (int)Math.floor( (lonDegW + 180) / 360 * (1<<zoom) ) ;
		yTile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(ocen2odeticS)) + 1 / Math.cos(Math.toRadians(ocen2odeticS))) / Math.PI) / 2 * (1<<zoom) ) ;
		compareMinMaxValues(xTile, yTile);
		
		xTile = (int)Math.floor( (lonDegE + 180) / 360 * (1<<zoom) ) ;
		yTile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(ocen2odeticS)) + 1 / Math.cos(Math.toRadians(ocen2odeticS))) / Math.PI) / 2 * (1<<zoom) ) ;
		compareMinMaxValues(xTile, yTile);
		
		xTile = (int)Math.floor( (lonDegE + 180) / 360 * (1<<zoom) ) ;
		yTile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(ocen2odeticN)) + 1 / Math.cos(Math.toRadians(ocen2odeticN))) / Math.PI) / 2 * (1<<zoom) ) ;
		compareMinMaxValues(xTile, yTile);
		

		this.zoom1 = zoom;
		
		//Define the number of tiles for x and y, then add one to accommodate for the re-scaling (we're scaling down) 
		nTilesX = (maxTileX1 - minTileX1)+1;
		nTilesY = (maxTileY1 - minTileY1)+1;	
		
		
	}
	
	  private void tile2Lat() {
		  
		  	// After you have the Tiles, calculate the lat value of the UL corner on the min and max tile, as defined by OSM	  
		    double n = Math.PI - (2.0 * Math.PI * minTileY1) / Math.pow(2.0, zoom1);
		    double n1 = Math.PI - (2.0 * Math.PI * (maxTileY1+1)) / Math.pow(2.0, zoom1);

		    double yTileLat = Math.toDegrees(Math.atan(Math.sinh(n)));
		    double southTileLat = Math.toDegrees(Math.atan(Math.sinh(n1)));
					
			//Convert back to ocentric lat values because OSM and JMARS are not both ocentric 
		    double odetic2ocenN = (rad2Deg)*(Math.atan(odeticToOcenScale*Math.tan(deg2Rad*yTileLat)));
		    double odetic2ocenS = (rad2Deg)*(Math.atan(odeticToOcenScale*Math.tan(deg2Rad*southTileLat)));
		    
		    //Save North and South boundary in ocentric to instance variables
			this.odetic2ocenN1 = odetic2ocenN;
			this.odetic2ocenS1 = odetic2ocenS;
			
			//Convert corner points to pixel points. Accommodate for prime meridian and the equator 		    
			if ((odetic2ocenN1>=0) && (odetic2ocenS1>=0)){
		    	this.northPoint = Main.testDriver.mainWindow.getProj().spatial.toScreen(xTileLonW,odetic2ocenN1);
				this.southPoint = Main.testDriver.mainWindow.getProj().spatial.toScreen(xTileLonW, odetic2ocenS1);
			}if ((odetic2ocenN1<=0) && (odetic2ocenS1<=0)){
			    double alteredOdetic2ocenN1 = Math.abs(odetic2ocenN1);
			    this.northPoint = Main.testDriver.mainWindow.getProj().spatial.toScreen(xTileLonW,alteredOdetic2ocenN1);
			    double alteredOdetic2ocenS1 = Math.abs(odetic2ocenS1);
			    this.southPoint = Main.testDriver.mainWindow.getProj().spatial.toScreen(xTileLonW,alteredOdetic2ocenS1);
			}if ((odetic2ocenN1>=0) && (odetic2ocenS1<=0)){
		    	this.northPoint = Main.testDriver.mainWindow.getProj().spatial.toScreen(xTileLonW,odetic2ocenN1);
			    this.southPoint = Main.testDriver.mainWindow.getProj().spatial.toScreen(xTileLonW,odetic2ocenS1);
			}

			int north = (int) northPoint.getY();
			int south = (int) southPoint.getY();
			this.northLatMinusSouth = north - south;

		  }
	  

	private void tile2Lon() {
		
			//Calculate lon value for west and east boundary, defined by OSM
		    this.xTileLonW = minTileX1 / Math.pow(2.0, zoom1) * 360.0 - 180; 
		    this.xTileLonE = (maxTileX1 +1) / Math.pow(2.0, zoom1) * 360.0 - 180;
		    
		    //Convert lon values to pixel points to calculate width
		    Point2D lonPointsE = Main.testDriver.mainWindow.getProj().spatial.toScreen(xTileLonE,odetic2ocenN1);
		    Point2D lonPointsW = Main.testDriver.mainWindow.getProj().spatial.toScreen(xTileLonW,odetic2ocenS1);		    
		    int west = (int) lonPointsW.getX();
		    int east = (int) lonPointsE.getX();  
		    this.westLonMinusEast = west - east;

		   }
	  
	  private void compareMinMaxValues (int xTile, int yTile){
		 
		  //This calculates which tiles cover the intended area on the screen, saves
		  //out the corner point tile values.
			if (xTile < minTileX1){
				minTileX1 = xTile;  
				this.xTile = xTile;
			}if (xTile > maxTileX1){
				maxTileX1 =xTile;
				this.xTile = xTile;
			}if (yTile < minTileY1){
				minTileY1 = yTile;
				this.yTile = yTile;
			}if (yTile > maxTileY1){
				maxTileY1 = yTile;
				this.yTile = yTile;
			}
	  }
	  
	  

	  public void getTileImages(BoundingBoxDataObject boundingBox, int zoom, Double jmarsPpdDouble, double equaCircumference) {  

		  getTileNumbers(boundingBox, zoom); 
		  	  
		  //Creates area to save the tiles out to(caches in the users JMARS folder)
		  String tempOSMFile = Main.getJMarsPath() + "cache" + File.separator + "osmCachedTiles";
		  File dir = null;
		  try {
			  dir = new File (tempOSMFile);
			  dir.mkdir();
		  }catch (Exception ex){
			// pop up informational dialog to the user
			  Util.showMessageDialog("Can't save to JMARS cache folder. Please check to make sure there is enough " +
			  		"memory and try again. If the problem continues, send a Problem Report to the JMARS" +
			  		"team" , "Cache Directory Error!", JOptionPane.WARNING_MESSAGE);
		  }
		  
		  BufferedImage img = null;
		  
		  //tiles are always 256x256, calculates the pixels in both the x and y direction as well as totalTiles
		  this.width = 256*nTilesX;
		  this.height = 256 *nTilesY;
		  int totalTiles = nTilesX * nTilesY;
		  int ytrial = 0;
		  int xtrial= 0;

		  //Create buffered image with the calculated width and height to save the tiles into
		  BufferedImage outputImage = new BufferedImage (this.width, height, Image.SCALE_FAST); 
		  Graphics2D g2 = outputImage.createGraphics();
		  
		  //If PPD is less than 256 don't draw, it is just blue marble.
		  if (boundingBox.getJmarsPpd() < 256){						
				return;
		  }				
		  // if ppd is not less than 256, then draw
			if (boundingBox.getJmarsPpd()> 256){ 
				
				//set up progress bar for the user to see progress on downloading tiles
				int max = totalTiles;
				int min = 0;
				final JProgressBar pb = new JProgressBar();
				JFrame frame = new JFrame("Loading Tiles...");
				pb.setMaximum(max);
				pb.setMinimum(min);
				pb.setStringPainted(true);
				frame.setLayout(new FlowLayout());
				frame.getContentPane().add(pb, BorderLayout.CENTER);
				frame.setSize(275, 75);
				frame.setVisible(true);
				GraphicsDevice screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
				
				//grab center of screen locations so the progress bar goes in the middle
				int xCenter = (screenSize.getDisplayMode().getWidth())/2;
				int yCenter = (screenSize.getDisplayMode().getHeight()/2);
				frame.setLocation(xCenter,yCenter);
				
			
				try {
					/* Calls the tiles for the area shown in the JMARS window from OSM. N is in the Y direction
						M is in the x direction. If the tiles are not in the cache, it kicks off a thread. If the 
						tile is in the cache, that tile is grabbed from there. Organized in the cache by zoom, 
						x tile number, y tile number (tile numbers are OSM's tile numbers from their database)
					*/	

					//creates a thread group to keep track of the tiles being called
					ThreadGroup osmTileGroup = new ThreadGroup("osmTiles");

					//loops through the the Y and x direction, determines if those tiles need to be called
					for(int n = minTileY1; n <= maxTileY1; n++){	  		  
						for (int m = minTileX1; m<= maxTileX1; m++) {		
							layer.setStatus(Color.yellow); 
							String tileFile = tileType + "_"+ zoom + "_" + m + "_" + n ;
							File cacheDir = new File (dir,tileFile);
							
							//if the tile is not in the cache, a thread is kicked off to start the download process
							if (!cacheDir.exists()){ 
								try {
									Thread t1 = new Thread(osmTileGroup, new ExecuteThread(m, n, zoom, tileFile, dir, cacheDir, outputImage, xtrial, ytrial, totalTiles));								
									t1.start();
									while (osmTileGroup.activeCount() > 0){ 
										Thread.sleep(100);
									}
								} catch (Exception e) {									
									if (e instanceof java.lang.InterruptedException){								
									}else {
										log.aprintln("Error with a Thread: " + e.getMessage());
									}
									
								} 

							}	else if (cacheDir.exists()){ 								
								try {
									img = ImageIO.read(cacheDir);
									drawImage(img, m, n, zoom, outputImage, xtrial,ytrial, tileSize, totalTiles, pb, frame, frame, null);
								} catch (Exception e) {				
									//e.printStackTrace(); send excep message to log here
									log.aprintln("Can't load image: "+ e.getMessage());
								}									
							} 																				
							
							/* Checks to see if it is the last tile and if there's enough tiles to cover the area.
								If not, an additional tile is added. This check has to be done because of the difference between
								ocentric and ographic shifts between OSM and JMARS. It does the check in both the x AND the
						 		y direction for re-scaling purposes.
						 	*/
							
							int l = m + 1; 
							if ( !(l <= maxTileX1)){
								if (xtrial < (ytrial)){
									this.maxTileX1 = this.maxTileX1+1;
									this.width = this.width + 256;
									this.nTilesX = this.nTilesX +1;
									totalTiles = nTilesX * nTilesY;
								}
							}	

							xtrial += 256;
						} 
						int y = n + 1; 
						if ( !(y <= maxTileY1)){
							if (ytrial < (xtrial-256)){
								this.maxTileY1 = this.maxTileY1+1;
								this.height = this.height + 256;
								this.nTilesY = this.nTilesY + 1 ;
								totalTiles = nTilesX * nTilesY;
							
							}
						}		
						xtrial = 0;
						ytrial += 256;
					}
					
					//System.out.println("totalTiles being gathered:"+ totalTiles);
					
				} catch (Exception e) {
					log.aprintln("Tile Error: "+ e.getMessage());
				} finally { for (int b = 0; b < max; b++){ //The progress bar increases as each tile is downloaded
					final int currentValue = b+1;
					try{
						SwingUtilities.invokeAndWait(new Runnable(){
							public void run() {
								pb.setValue(currentValue);
							}
						});
						java.lang.Thread.sleep(90);
					} catch (Exception e) {
						Util.showMessageDialog(e.getMessage());
						break;
					} 
				} 
				
				tile2Lon();
				tile2Lat();									
				//System.out.println("About to rescale......");
				scaleImage(outputImage, boundingBox, zoom, jmarsPpdDouble, equaCircumference, totalTiles, g2);	
				frame.dispose();
				}
				
			}
	  }
	

	public class ExecuteThread implements Runnable  {

		URL url=null;
		int m = 0;
		int n = 0;
		int zoom = 0;
		int counter = 0;
		String tileFile = null;
		File dir = null;		  
		BufferedImage img = null;
		String urlStr = null;
		BufferedImage outputImage = null;
		File cacheDir = null;
		int xtrial = 0;
		int ytrial = 0;
		int totalTiles = 0;

		

		//sets up the thread that was called in a previous method.
		
		public ExecuteThread(int m, int n, int zoom, String tileFile, File dir, File cacheDir, BufferedImage outputImage, 
				int xtrial, int ytrial, int totalTiles){
			this.m = m;
			this.n = n;
			this.zoom = zoom;
			this.tileFile = tileFile;
			this.dir = dir;
			this.cacheDir = cacheDir;
			this.outputImage = outputImage;
			this.xtrial = xtrial;
			this.ytrial = ytrial;
			this.totalTiles = totalTiles;
			
		}
		
		
		  public void run() {
			
				//This does the actual calling from the OSM URL and writes it to the users cache
			  try {
				  if (osmTypeInt == 0){
					  urlStr =  osmUrl + tileType +"/"+ zoom + "/"+ m + "/"+ n + ".png";
				  }else if (osmTypeInt == 1){
					  urlStr = osmUrl + tileType+ "/"+zoom+ "/"+ m + "/"+n + ".png";
				  }
				  
				  url = new URL(urlStr);  // use this to check for valid URL symtax

	              InputStream in = null;
				  JmarsHttpRequest request = new JmarsHttpRequest(urlStr, HttpRequestType.GET);
	              boolean status = request.send();
	              if (!status) {
	                  int httpStatus = request.getStatus();
	                  System.out.println("Error retrieving image. Http Status: "+httpStatus);
	              } else {
	                  in = request.getResponseAsStream();
	              }
				  
				  img = ImageIO.read(in);
                  request.close();
				  
//				  img = ImageIO.read(url);
				  ImageIO.write(img, "png", cacheDir);
				  
				  if (img !=null){
					  SwingUtilities.invokeAndWait(new Runnable() {
						  public void run() {
							  if (img!= null){						 						  							 
							  drawImage(img, m, n, zoom, outputImage, xtrial,ytrial, tileSize, totalTiles, null, null, null, null);							 
							  }	else {
							  System.err.println("Error, OpenStreetMap is giving a null image.");
							  } 
						  }
					  });			
				  } 
			  } catch (Exception e){  
				  if (e instanceof IOException){
					  log.aprintln("IOException e: " + e.getMessage());
					  Util.showMessageDialog("Error, the OpenStreetMap data is " +
					  		"unavailable at this time. Please try again later, thank you. ", "Error Message", JOptionPane.WARNING_MESSAGE);
				  }else if(e instanceof InterruptedException){
					  //Error is being handled through other notifications.
				  }else if (e instanceof InvocationTargetException){
					  log.aprintln("InvocationTargetException e: " + e.getMessage());
					  Util.showMessageDialog("Error: Serious system error, please submit a problem report " +
					  		"to the JMARS team.", "Error Message", JOptionPane.ERROR_MESSAGE);					
				  }
			  }
		  }		
	}		
	
	
	public synchronized Graphics2D drawImage(BufferedImage img, int m, int n, int zoom, BufferedImage outputImage, 
			int xtrial, int ytrial, int tileSize, int totalTiles, final JProgressBar pb, JFrame frame, JFrame frame2, JPanel picPanel ) {	
		
		
		//Draws the tile from either OSM or the users cache into the original buffered image that was 
		//created in a previous method
		Graphics2D g2 = outputImage.createGraphics();
		g2.drawImage(img, xtrial, ytrial, xtrial + tileSize, ytrial + tileSize, 0, 0, img.getWidth(), img.getHeight(), null);
		
//		try {
//			ImageIO.write(outputImage, "png", new File ("/mars/u/mesmith/openstreetmap"));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		Thread.yield();
		g2.dispose();
		return g2;	
	}





	  private void scaleImage(BufferedImage outputImage, BoundingBoxDataObject boundingBox, int zoom, Double jmarsMeters, double equaCircumference, int totalTiles, Graphics2D g2)  {  
		  
		  /*This method draws a new bufferedImage but it is rescaled. This is to accommodate for the
			  differences between OSM and JMARS. JMARS is in PPD while OSM is in MPP.
		  */
		  
		  int newWidth = 0;
		  int newHeight = 0;
		  double finalScaleMeters = 0.00;
		  double openStreetMpp= 0;		 		  
		  Double jmarsMPP = jmarsMeters;
		  double deg2rad=0;
		  deg2rad = (Math.PI*2)/360;	
		  
		  //calculates the distance in OSM represented by one pixel, as defined by OSM
		  openStreetMpp = equaCircumference*(Math.cos(0*deg2rad)/(Math.pow(2,zoom+8))); 
		  
		  //OSM zoom in meters/ppd into meters
		  finalScaleMeters = (((openStreetMpp) /(jmarsMPP))) ; 
		  
		  //accommodate for equator and prime mer. differences by using absolute value
		  double eqDifference = Math.abs((double)westLonMinusEast/northLatMinusSouth);
		  double finalScaleFactor = Math.abs((double)finalScaleMeters/eqDifference);

		  //Finally, calculate the new width and height of the scaledImage using the calculations above 
		  newWidth = (int)Math.ceil(this.width*finalScaleMeters);
		  newHeight = (int) (Math.ceil(this.height)*finalScaleFactor);
		  BufferedImage scaledImage = new BufferedImage (newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
		  Graphics2D g2D = scaledImage.createGraphics();				

		  //Now do the actual transformation 
		  AffineTransform affineTrans = AffineTransform.getScaleInstance (finalScaleMeters,finalScaleFactor);
		  g2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		  g2D.drawImage(outputImage, affineTrans, null);

		  
		  //Save out the bufferedImage for debugging purposes
		  
//		  String tempOSMFile = "/mars/u/mesmith/scaledImage";
//		  try {
//			  ImageIO.write(scaledImage, "png", new File (tempOSMFile));
//		  } catch (IOException e) {
//			  // TODO Auto-generated catch block
//			  e.printStackTrace();
//		  } 
		  
		  this.scaledImage1 = scaledImage;
		  

	  };
		

	  public static void reprojectMessage (){
		  
		  //Currently, OSM does not re-project. This is a message to the user if they try to re-project. 
		  // In the future, we may change osm to draw pixel to pixel instead of using bufferedImages
		  
			StreetLView myLView = null;
			MovableList<LView> al = Main.testDriver.mainWindow.viewList;
			for (LView lView : al) {
				if (lView instanceof StreetLView) {
					myLView = (StreetLView) lView;
					if (myLView.isAlive()){
						Util.showMessageDialog("Sorry, OpenStreetMap does not re-project at this time.","Message", JOptionPane.ERROR_MESSAGE);				

					}
				}
			}
			
		}
	  
	  public double getxTileLonW() {
		  return xTileLonW;
	  }
	  public double getOdetic2ocenN1() {
		  return odetic2ocenN1;
	  }
	  public BufferedImage getScaledImage1() {
		  return scaledImage1;
	  }

	
}


