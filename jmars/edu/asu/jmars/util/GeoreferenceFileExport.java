package edu.asu.jmars.util;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFileChooser;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.streets.BoundingBoxDataObject;

public class GeoreferenceFileExport {

    String filename = null;
    JFileChooser fc = null;
    
	double nLat = 0.00;
	double sLat = 0.00;
	double eLon = 0.00;
	double wLon = 0.00;
	double width = 0.00;
	double height = 0.00;
	
	private void saveGeoImage(){
        fc = Main.getFileChooser(".tif", "TIF File (*.tif)");
        if (fc == null)
            return;
        fc.setDialogTitle("Capture to TIF File");
        if (fc.showSaveDialog(Main.mainFrame) != JFileChooser.APPROVE_OPTION)
            return;
        if ( fc.getSelectedFile() != null )
            filename = fc.getSelectedFile().getPath();
        
        if(filename == null)
                return;
        else{
        	Main.testDriver.dumpMainLViewManagerTif(filename);
        }
        saveTextFile();
	}
	
	private void saveProjFile(){
		try{
			FileWriter wr = new FileWriter(filename + ".prj"); 
			// Mean Radius should be in Meters....Example: Mars should be 3386000.0
			double MEAN_RADIUS = Util.MEAN_RADIUS*1000;
			String body = Main.getBody();
			wr.write("GEOGCS[\"GCS_" + body + "\",DATUM[\"D_" + body + "\",SPHEROID[\"S_"+ body +"\"," + MEAN_RADIUS + ",0.0]],PRIMEM[\"Reference_Meridian\",0.0],"
					+ "UNIT[\"degree\",0.0174532925199433]]");
			wr.flush();
			wr.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}
	
	private void saveTextFile(){  
		filename = fc.getSelectedFile().getPath();
		try {
			//write out the link table 
			FileWriter wr = new FileWriter(filename + ".txt");
			//upper left 
			wr.write( "0.0000\t" + height + "\t" + wLon + "\t" + nLat + "\n");		
			//lower left 
			wr.write("0.000\t" + "0.0000\t" + wLon + "\t" + sLat + "\n");
			//upper right 
			wr.write(width + "\t" + height +"\t"+ eLon + "\t" + nLat + "\n");	
			//lower right
			wr.write(width + "\t0.0000\t" + eLon + "\t" + sLat);
			wr.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		saveProjFile();
	}
	
	public void calculateBbox(){
		int jmarsZoom = Main.testDriver.mainWindow.getProj().getPPD();
		Rectangle2D here = Main.testDriver.mainWindow.getProj().getWorldWindow();	
		Rectangle screenHere = Main.testDriver.mainWindow.getProj().getScreenWindow();	
		
		//Get the spatial points of each corner
		Point2D spatialNW = Main.testDriver.mainWindow.getProj().world.toSpatial(here.getMinX(),
				here.getMaxY());		
		
		Point2D spatialSE = Main.testDriver.mainWindow.getProj().world.toSpatial(here.getMaxX(),
				here.getMinY());

		Point2D spatialCenter = Main.testDriver.mainWindow.getProj().world.toSpatial(here.getCenterX(), here.getCenterY());

		
		double westLon = 360 - spatialNW.getX();
		westLon = (westLon > 180) ? westLon - 360.0 : westLon;

		double eastLon = 360 - spatialSE.getX();
		eastLon = (eastLon > 180) ? eastLon - 360.0 : eastLon;
		
		double northLat = spatialNW.getY();
		double southLat = spatialSE.getY();		
		
		//Request the bounding box info
		BoundingBoxDataObject bBox = new BoundingBoxDataObject(screenHere, spatialCenter, westLon, eastLon, northLat, southLat, jmarsZoom);
		nLat = bBox.getNorthLat();
		sLat = bBox.getSouthLat();
		eLon = bBox.getEastLon();
		wLon = bBox.getWestLon();
		width = Main.testDriver.mainWindow.getBounds().getWidth();
		height = Main.testDriver.mainWindow.getBounds().getHeight();
	
		saveGeoImage();
		
	}
 
}
