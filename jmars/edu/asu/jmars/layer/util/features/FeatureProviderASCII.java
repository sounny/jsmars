package edu.asu.jmars.layer.util.features;

import java.awt.geom.*;
import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import javax.swing.*;

import edu.asu.jmars.*;
import edu.asu.jmars.util.Util;
	

/** 
 * A class for saving or loading features to a ASCII shape file.
 * Note that only polygons are saved.  If there any points or lines to save,
 * they are they are tacitly ignored.
 **/
public class FeatureProviderASCII implements FeatureProvider{
	public String getExtension () {
		return ".txt";
	}

	public String getDescription () {
		return "ASCII File";
	}

	public boolean isFileBased () {
		return true;
	}

	// Various input output delimiters.
	static String INPUT_DELIM = " \t,;";
	static String OUTPUT_PATH_DELIM = ",";
	static String OUTPUT_REC_DELIM = "\n";

	// Projection object to convert back and forth between world and spatial coordinates.
	ProjObj po;

	/**
	 * The name of the source is specified.  The type is immutable.
	 */
	public FeatureProviderASCII() {
		// Junit testing does not run inside JMARS.  The following 
		// is needed to get the proper spatial<->world conversions. 
		if (Main.PO == null){
			po = new ProjObj.Projection_OC( 0,0);
		} else {
			po = Main.PO;
		}
	}

	public  FeatureCollection load(String fileName) {
		// Build a FeatureCollection.
		SingleFeatureCollection fc = new SingleFeatureCollection();

		// Setup default schema
		fc.addField(Field.FIELD_FEATURE_TYPE);
		fc.addField(Field.FIELD_PATH);

		// Build the List of Features for the FeatureCollection.
		List featureList = new  ArrayList();

		// Read the tokens from the file and build up a Generalpath
		try {
			BufferedReader  inStream        = new BufferedReader(new FileReader( filterFile(fileName) ));
			String          inputLine       = null;

			do {
				inputLine = inStream.readLine();
				if (inputLine != null && inputLine.length() > 0) {
					float[] lonLats = FeatureUtil.stringToFloats(inputLine, INPUT_DELIM);
					FPath path = new FPath (lonLats, false, FPath.SPATIAL_EAST, true).getSpatialWest();

					Feature feature = new Feature();
					feature.setAttributeQuiet( Field.FIELD_FEATURE_TYPE, FeatureUtil.TYPE_STRING_POLYGON);
					feature.setAttributeQuiet( Field.FIELD_PATH, path);
					featureList.add( feature);
				}
			} while (inputLine != null);
		} catch (Exception e) {
			Util.showMessageDialog("Error loading ASCII file: " + fileName + "\n");
			return null;
		}
		fc.addFeatures( featureList);

		// finished.  Just need to return what we gots.
		return fc;
	}

	public boolean isRepresentable(FeatureCollection fc){
		int[] featTypes = FeatureUtil.getRepresentedFeatureTypes(fc.getFeatures());
		return (featTypes.length == 1 && featTypes[0] == FPath.TYPE_POLYGON);
	}

	public File[] getExistingSaveToFiles(FeatureCollection fc, String fileName) {
		File file = filterFile(fileName);
		if (file.exists())
			return new File[]{file};
		return new File[]{};
	}

	// writes out the specified features to the specified file.
	public int save( FeatureCollection fc, String fileName){
		File file = filterFile(fileName);
		int saved = 0;
		int notSavedCount = 0;
		NumberFormat nf = DecimalFormat.getNumberInstance();
		nf.setMaximumFractionDigits(3);

		try {
			BufferedWriter  outStream = new BufferedWriter(new FileWriter(file.toString()));
			Iterator fi = fc.getFeatures().iterator();
			while (fi.hasNext()){
				Feature     shape = (Feature)fi.next();
				
				if ( shape.getPath().getType() == FPath.TYPE_POLYGON){
					FPath path = (FPath) shape.getAttribute(Field.FIELD_PATH);
					Point2D[] vertices = path.getSpatialEast().getVertices();

					StringBuffer outputLine = new StringBuffer();
					for(int j=0; j<vertices.length; j++){
						if (j>0)
							outputLine.append(OUTPUT_PATH_DELIM);
						outputLine.append(nf.format(vertices[j].getX())+
								OUTPUT_PATH_DELIM+
								nf.format(vertices[j].getY()));
					}
					outputLine.append(OUTPUT_REC_DELIM);
					outStream.write( outputLine.toString());
					saved ++;
				}
				else {
					notSavedCount++;
				}
			}
			outStream.flush();
			outStream.close();
			Util.showMessageDialog(
					"Saved " + saved + " polygons to " + file.toString() + ".\n"+
					(notSavedCount>0?("Ignored "+ notSavedCount +" non polygon features."):""));
		}
		catch (Exception e) {
			Util.showMessageDialog("Error saving ASCII file: " + file.toString() + "\n");
			return 0;
		}
		
		return saved;
	}

	/**
	 * If the file does not end with the file type extention, it should
	 * be added to the end of the file name.
	 */
	private File filterFile( String fileName) {
		if (!fileName.endsWith(getExtension()))
			fileName += getExtension();
		return new File( fileName);
	}

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
}
 


