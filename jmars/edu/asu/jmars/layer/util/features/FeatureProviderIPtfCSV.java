package edu.asu.jmars.layer.util.features;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import com.csvreader.CsvReader;

import edu.asu.jmars.util.Util;


/** 
 * A TEST (read-only) FeatureProvider to load/display output from Ptf2Shp.
 **/
public class FeatureProviderIPtfCSV implements FeatureProvider{
	public String getExtension () {
		return ".csv";
	}

	public String getDescription () {
		return "Ptf2Shp CSV File";
	}

	public boolean isFileBased () {
		return true;
	}

	/**
	 * The name of the source is specified.  The type is immutable.
	 */
	public FeatureProviderIPtfCSV() {
	}

	public  FeatureCollection load(String fileName) {
		try {
			CsvReader r = new CsvReader(fileName, '\t', Charset.forName("ISO-8859-1"));
			r.setTrimWhitespace(false);
			r.readHeaders();
			String[] hdr = r.getHeaders();
			Field[] fields = new Field[hdr.length];
			for(int i=1; i<hdr.length; i++)
				fields[i] = new Field(hdr[i], String.class, false);

			// Build the List of Features for the FeatureCollection.
			List<Feature> featureList = new  ArrayList<Feature>();

			// Read the tokens from the file and build up a Generalpath
			while(r.readRecord()){
				String[] vals = r.getValues();
				float[] lonLats = FeatureUtil.stringToFloats(vals[0], ",");

				FPath path = new FPath (lonLats, false, FPath.SPATIAL_EAST, true).getSpatialWest();

				Feature feature = new Feature();
				feature.setAttributeQuiet( Field.FIELD_FEATURE_TYPE, FeatureUtil.TYPE_STRING_POLYGON);
				feature.setAttributeQuiet( Field.FIELD_PATH, path);
				for(int i=1; i<hdr.length; i++)
					feature.setAttributeQuiet(fields[i], "".equals(vals[i])? null: vals[i]);
				featureList.add( feature);
			}
			
			// Build a FeatureCollection.
			SingleFeatureCollection fc = new SingleFeatureCollection();
			
			// Add read features to the feature collection
			fc.addFeatures( featureList);
			return fc;
		} catch (Exception e) {
			Util.showMessageDialog("Error loading ASCII file: " + fileName + "\n");
			return null;
		}
	}

	public boolean isRepresentable(FeatureCollection fc){
		return false;
	}

	public File[] getExistingSaveToFiles(FeatureCollection fc, String fileName) {
		return new File[]{};
	}

	// writes out the specified features to the specified file.
	public int save( FeatureCollection fc, String fileName){
		throw new UnsupportedOperationException();
	}

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
}
 


