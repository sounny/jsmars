package edu.asu.jmars.layer.util.features;

import java.awt.Shape;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.GML;
import edu.asu.jmars.util.Util;

// this means that the GML.java class needs to be moved to edu.asu.jmars.util
//import edu.asu.jmars.layer.shape.*;
        

/**
 * A class for saving or loading features to a GML shape file. * The file to
 * either save or load to is fetched within the methods themselves.
 */
public class FeatureProviderGML implements FeatureProvider {
	public String getExtension () {
		return ".gml";
	}

	public String getDescription () {
		return "GML Files";
	}

	public boolean isFileBased () {
		return true;
	}

	private DebugLog log = DebugLog.instance();

	public static final Field ID = new Field( "id", String.class);

	public FeatureCollection load(String name) {
		// Build a new FeatureProvider inside a new FeatureCollection
		SingleFeatureCollection fc = new SingleFeatureCollection();

		// Build the List of Features for the FeatureCollection.
		final File file = filterFile(name);
		GML.Feature[] gmlFeatures = GML.File.read(file.getAbsolutePath());
		if ( gmlFeatures == null || gmlFeatures.length < 1) {
			return null;
		} else {
			List featureList = new ArrayList();
			for (int i=0; i < gmlFeatures.length; i++) {
				GML.Feature gmlFeature = gmlFeatures[i];
				Feature     newFeature = new Feature();
				try {
					String featureTypeString = FeatureUtil.getFeatureTypeString(gmlFeatureTypeToFeatureType(gmlFeature.getType()));
					Shape shape = gmlFeature.getShape();
					FPath path = new FPath(shape, FPath.SPATIAL_EAST);
					newFeature.setAttributeQuiet( ID,                       gmlFeature.getId());
					newFeature.setAttributeQuiet( Field.FIELD_LABEL,        gmlFeature.getDescription());
					newFeature.setAttributeQuiet( Field.FIELD_FEATURE_TYPE, featureTypeString);
					newFeature.setAttributeQuiet( Field.FIELD_PATH,         path.getSpatialWest());

					// Add the feature to the list.
					featureList.add( newFeature);
				}
				catch(Exception ex){
					log.aprintln("Feature "+i+" generated an error. Ignoring. Message: "+ex.getMessage());
				}
			}
			fc.addFeatures( featureList);
			return fc;
		}
	}

	public int gmlFeatureTypeToFeatureType(int gmlFeatureType){
		switch(gmlFeatureType){
			case GML.POINT: return FPath.TYPE_POINT;
			case GML.LINE: return FPath.TYPE_POLYLINE;
			case GML.POLYGON: return FPath.TYPE_POLYGON;
		}
		return FPath.TYPE_NONE;
	}

	public int featureTypeToGmlFeatureType(int featureType){
		switch(featureType){
			case FPath.TYPE_POINT: return GML.POINT;
			case FPath.TYPE_POLYLINE: return GML.LINE;
			case FPath.TYPE_POLYGON: return GML.POLYGON;
		}
		return GML.NONE;
	}

	public boolean isRepresentable(FeatureCollection fc){
		return true;
	}
	public File[] getExistingSaveToFiles(FeatureCollection fc, String name){
		File file = filterFile(name);
		if (file.exists())
			return new File[]{file};
		return new File[]{};
	}

	// writes out the specified features to the specified file.
	public int save(FeatureCollection fc, String name) {
		if (fc==null || fc.getFeatures()==null || fc.getFeatures().size()<1){
			Util.showMessageDialog("Selection contains no rows. Save aborted.");
			return 0;
		}

		File file = filterFile(name);

		// convert the shape framework Features to GML.Feature objects.
		List gmlFeatures = new ArrayList();
		int i=1;
		for (Iterator fi = fc.getFeatures().iterator(); fi.hasNext(); i++){
			Feature f = (Feature)fi.next();

			int gmlShape = featureTypeToGmlFeatureType(f.getPath().getType());

			FPath path = (FPath)f.getAttribute( Field.FIELD_PATH);
			Shape gp = path.getSpatialEast().getShape();

			String id = (String) f.getAttribute(ID);

			String desc = (String) f.getAttribute(Field.FIELD_LABEL);
			GML.Feature feature = new GML.Feature(gmlShape, gp, id, desc);
			gmlFeatures.add(feature);
		}
		// write the features out.
		int result = GML.File.write((GML.Feature[])gmlFeatures.toArray(new GML.Feature[0]), file.getAbsolutePath());
		if (result>0){
			Util.showMessageDialog("Wrote " + result + " features to " + file.toString() + "\n");
		} 
		return result;
	}
	
	
	// If the file does not end with the file type extention, it should
	// be added to the end of the file name.
	private File filterFile( String fileName){
		if (!fileName.endsWith(getExtension()))
			fileName += getExtension();
		return new File(fileName);
	}

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
}
 


