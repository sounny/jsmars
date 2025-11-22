package edu.asu.jmars.layer.util.features;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView;
import edu.asu.jmars.util.DebugLog;

/**
 * A class for saving or loading features to a ASCII shape file.
 */
public class FeatureProviderNomenclature implements FeatureProvider {
	private static DebugLog log = DebugLog.instance();

	public String getExtension () {
		return null;
	}

	public String getDescription () {
		return "Nomenclature";
	}

	public boolean isFileBased () {
		return false;
	}

	static String tokenDelimiters = "\t";

	ProjObj po;

	/**
	 * Constructs a feature provider for the nomenclature format. The
	 * name is ignored.
	 */
	public FeatureProviderNomenclature() {
		// Junit testing does not run inside JMARS. The following
		// is needed to get the proper spatial<->world conversions.
		if (Main.PO == null) {
			po = new ProjObj.Projection_OC(0, 0);
		} else {
			po = Main.PO;
		}
	}

	private class MarsFile {
		final static int LANDMARK_TYPE = 0;
		final static int NAME = 1;
		final static int LAT = 2;
		final static int LON = 3;
		final static int DIAMETER = 4;
		final static int ORIGIN = 5;
		final static int NUMBER_OF_FIELDS = 6;
	}

	public static final Field LABEL = new Field(Field.FIELD_LABEL.name, Field.FIELD_LABEL.type, false);
	public static final Field LANDMARK_TYPE = new Field("landmark_type", String.class, false);
	public static final Field DIAMETER = new Field("diameter", Double.class, false);
	public static final Field ORIGIN_FIELD = new Field("Name Origin", String.class, false);

	public FeatureCollection load(String name) {
		// Build a FeatureCollection.
		SingleFeatureCollection fc = new SingleFeatureCollection();

		// Setup default schema.
		fc.addField(Field.FIELD_FEATURE_TYPE);
		fc.addField(Field.FIELD_PATH);
		fc.addField(LABEL);
		fc.addField(LANDMARK_TYPE);
		fc.addField(DIAMETER);
		fc.addField(ORIGIN_FIELD);

		// Build the List of Features for the FeatureCollection.
		java.util.List featureList = new  ArrayList();

		// Fill the lists with the features and stuff.
		try {
			// @since change bodies
			File nomenclatureFile = NomenclatureLView.getFeaturesFile();
			BufferedReader inStream = new BufferedReader(new FileReader(nomenclatureFile));
			// end change bodies

			for (String lineIn = inStream.readLine();
				lineIn != null && lineIn.compareToIgnoreCase("STOP") != 0;
				lineIn = inStream.readLine())
			{
				String [] tok   = lineIn.split( tokenDelimiters);
				if (tok.length != MarsFile.NUMBER_OF_FIELDS)
					continue;

				// build the feature.
				Feature feature                 = new Feature();
				
				// set the path
				float x   = (float)Double.parseDouble(tok[MarsFile.LON]);
				float y   = (float)Double.parseDouble(tok[MarsFile.LAT]);
				
				Point2D[] vertices = new Point2D[] {new Point2D.Float(x,y)};
				FPath path = new FPath (vertices, FPath.SPATIAL_EAST, false);
				feature.setAttributeQuiet( Field.FIELD_FEATURE_TYPE,  FeatureUtil.TYPE_STRING_POINT);
				feature.setAttributeQuiet( Field.FIELD_PATH,          path.getSpatialWest());

				// set the other fields.
				feature.setAttributeQuiet( LABEL,                     tok[MarsFile.NAME]);
				feature.setAttributeQuiet( LANDMARK_TYPE,             tok[MarsFile.LANDMARK_TYPE]);
				feature.setAttributeQuiet( DIAMETER,                  getDouble (tok[MarsFile.DIAMETER]));
				feature.setAttributeQuiet( ORIGIN_FIELD,              tok[MarsFile.ORIGIN]);

				// Add the feature to the list.
				featureList.add( feature);
			}
			inStream.close();
		} catch ( NoSuchElementException ne ) {
			// ignore
		} catch (IOException e) {
			log.println("Error reading nomenclature file. " + e.getMessage());
			log.printStack(-1);
		}
		
		fc.addFeatures( featureList);
		return fc;
	}

	private Double getDouble (String value) {
		try {
			return new Double (value);
		} catch (NumberFormatException e) {
			log.aprintln ("Invalid nomenclature point, unable to convert");
			return new Double ((double)0.0);
		}
	}

	public boolean isRepresentable(FeatureCollection fc) {
		return false;
	}

	public File[] getExistingSaveToFiles(FeatureCollection fc, String name) {
		return new File[]{};
	}

	/** Saving is undefined for this module. */
	public int save(FeatureCollection fc, String name) {
		throw new UnsupportedOperationException();
	}

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
}
