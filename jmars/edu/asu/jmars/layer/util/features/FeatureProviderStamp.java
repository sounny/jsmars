package edu.asu.jmars.layer.util.features;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import edu.asu.jmars.layer.stamp.StampShape;

/**
 * This class provides support for creating a shape layer from a stamp outline in the stamp layer.
 */
public class FeatureProviderStamp implements FeatureProvider {
	
	private List<StampShape> myShapes;
	private String instrument = "stamp outlines";
	public FeatureProviderStamp(List<StampShape> stamps, String instr) {
		myShapes = stamps;
		if (instr != null && instr.length() > 1) {
		    this.instrument = instr +" "+ instrument;
		}
	}
	
	public FeatureProviderStamp() {
		
	}
	
	public String getDescription() {
		return this.instrument;
	}
	
	public File[] getExistingSaveToFiles(FeatureCollection fc, String baseName) {
		return null;
	}
	
	public String getExtension() {
		return null;
	}
	
	public boolean isFileBased() {
		return false;
	}

	public boolean isRepresentable(FeatureCollection fc) {
		return false;
	}
		
	public FeatureCollection load(String fileName) {
		FeatureCollection fc = new SingleFeatureCollection();
		
		StampShape firstShape = myShapes.get(0);
				
		String[] headers = firstShape.getVisibleColumns();
				
		Field[] others = new Field[headers.length];
		List<Feature> features = new LinkedList<Feature>();
		for (StampShape thisShape : myShapes) {
			Feature f = new Feature();
			
			try {
				f.setAttribute(Field.FIELD_LABEL, thisShape.getId());

				double points[] = thisShape.getStamp().getPoints();
						
				double[] fcoords = new double[points.length];
				for (int k = 0; k < points.length; k++) {
					fcoords[k] = points[k];
				}
						
				FPath path = new FPath(fcoords, false, FPath.SPATIAL_WEST, true);
				f.setAttribute(Field.FIELD_PATH, path);

				for (int j = 0; j < headers.length; j++) {
					Object value = thisShape.getVal(headers[j]);
					
					if (value==null) {
						value = "";
					}

					// Despite containing only numbers and - signs, a value like '2010-07-16' blows up on the parseDouble test
					try {
						if (others[j] == null) {
							others[j] = new Field(headers[j], Double.class);
						}
						f.setAttribute(others[j], Double.parseDouble(""+value));
					} catch (Exception e) {
						others[j] = new Field(headers[j], String.class);
						f.setAttribute(others[j], ""+value);
					}
				}
				features.add(f);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		fc.addFeatures(features);
		return fc;
	}
	
	public int save(FeatureCollection fc, String fileName) {
		return 0;
	}

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return true;
    }
}
