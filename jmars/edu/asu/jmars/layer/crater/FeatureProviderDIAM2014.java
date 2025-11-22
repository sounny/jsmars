package edu.asu.jmars.layer.crater;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;

import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureProvider;

/**
 * Provides support for exporting the 2014 format Craterstats DIAM files. Even though it is an implementation of the 
 * FeatureProvider interface, it is not available for general use as a feature provider for other parts
 * of the Crater layer, or for other layers.
 * 
 * FeatureProviderDIAM only implements FeatureProvider to leverage the existing FileChooser class and
 * simplify the overall implementation of this new functionality.
 */

public class FeatureProviderDIAM2014 implements FeatureProvider {
	public String getDescription() {
		return "DIAM file for Craterstats";
	}
	
	public File[] getExistingSaveToFiles(FeatureCollection fc, String baseName) {
		String[] names = {baseName, baseName + getExtension()};
		for (String name: names) {
			File f = new File(name);
			if (f.exists()) {
				return new File[]{f};
			}
		}
		return new File[]{};
	}
	
	public String getExtension() {
		return ".diam";
	}
	
	public boolean isFileBased() {
		return true;
	}
	
	public boolean isRepresentable(FeatureCollection fc) {
		return true;
	}
		
	// Do NOT make this static.  If it is static, it is no longer thread-safe, due to the use of the ParsePosition parameter
	private final DecimalFormat fmt = new DecimalFormat("#.#####");

	private String headerTitle;
	private double craterArea;
	
	public int save(FeatureCollection fc, String fileName) {
		
		BufferedWriter fWriter = null;
		try {
			// write header
			fWriter = new BufferedWriter(new FileWriter(fileName));

            String[] headerRow = {
			"#"+this.headerTitle,
			"# area, km^2",
			String.format("area = %.3f", this.craterArea),
            "# crater = {diameter, lon, lat",
            "crater = {diameter, lon, lat"
            };

            for (String row : headerRow) {
                fWriter.write(row);
                fWriter.newLine();            	
            }
									
			// write rows
			int rows = 0;
			for (Feature f: fc.getFeatures()) {
				
				FPath path = f.getPath().getSpatialEast();
				Point2D p = path.getCenter();

				Object diameter = f.getAttribute(Crater.DIAMETER);

				double diamKm = ((Double)diameter) / 1000.0;
				String longitudeStr = fmt.format(p.getX());
				String latitudeStr = fmt.format(p.getY());
								
				fWriter.write(""+diamKm+" "+longitudeStr+ " " + latitudeStr);
	            fWriter.newLine();
				rows ++;
			}
			fWriter.write("}");
			fWriter.newLine();
			fWriter.close();
			return rows;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
    
    public void setHeaderInfo(String title, double area) {
    	this.headerTitle = title;
    	this.craterArea = area;
    }

	@Override
	public FeatureCollection load(String fileName) {
		// Not used in this feature provider.
		return null;
	}
}
