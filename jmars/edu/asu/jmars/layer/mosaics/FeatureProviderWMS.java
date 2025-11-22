package edu.asu.jmars.layer.mosaics;

import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.WMSMapServer;
import edu.asu.jmars.layer.map2.WMSMapSource;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureProvider;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.SingleFeatureCollection;

public class FeatureProviderWMS implements FeatureProvider {
	public static final Field FIELD_ABSTRACT = new Field("Abstract", String.class, false);
	public static final Field FIELD_MAP_SOURCE = new Field("Map Source", MapSource.class, false);
	
	private Map<Feature,MapSource> featToMapSrc = new HashMap<Feature,MapSource>();
	
	public String getDescription() {
		return "Mosaics";
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

	public FeatureCollection load(String urlString) {
		WMSMapServer ms = new WMSMapServer(urlString, 0, 2);
		FeatureCollection fc = new SingleFeatureCollection();
		featToMapSrc.clear();
		
		List<MapSource> mapSources = ms.getMapSources();
		
		for(MapSource s: mapSources){
			Rectangle2D bbox = ((WMSMapSource)s).getLatLonBoundingBox();
			if (bbox == null)
				continue; // skip features without a (lat,lon) bounding box.

			// TODO: Convert the bbox into ocentric coordinates.
			Feature f = new Feature();
			f.setPath(new FPath(new GeneralPath(bbox), FPath.SPATIAL_EAST));
			f.setAttribute(Field.FIELD_LABEL, s.getTitle());
			f.setAttribute(FIELD_ABSTRACT, s.getAbstract());
			f.setAttribute(FIELD_MAP_SOURCE, s);
			
			fc.addFeature(f);
			featToMapSrc.put(f, s);
		}
		
		return fc;
	}
	
	public Map<Feature,MapSource> getFeatureToMapSourceMap(){
		return Collections.unmodifiableMap(featToMapSrc);
	}

	public int save(FeatureCollection fc, String fileName) {
		throw new UnsupportedOperationException();
	}

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
}
