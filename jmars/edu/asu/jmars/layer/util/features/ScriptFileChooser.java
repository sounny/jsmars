package edu.asu.jmars.layer.util.features;

import java.io.File;

import edu.asu.jmars.layer.shape2.FileChooser;

public class ScriptFileChooser extends FileChooser {
	FeatureProvider fp = new FeatureProviderScript();
	public ScriptFileChooser() {
		this.addFilter(fp);
	}
	public FeatureProvider getProvider () {
		return fp;
	}
	public static class FeatureProviderScript implements FeatureProvider {
		public String getDescription() {
			return "Shape Script File (*" + getExtension() + ")";
		}

		public File[] getExistingSaveToFiles(FeatureCollection fc, String name) {
			return new File[]{};
		}

		public String getExtension() {
			return ".ssf";
		}

		public boolean isFileBased() {
			throw new UnsupportedOperationException("isFileBased() has no value, this is only a filter");
		}

		public FeatureCollection load(String name) {
			throw new UnsupportedOperationException("Can't load, this is only a filter");
		}

		public int save(FeatureCollection fc, String name) {
			throw new UnsupportedOperationException("Can't save, this is only a filter");
		}

		public boolean isRepresentable(FeatureCollection fc) {
			throw new UnsupportedOperationException("Can't validate, this is only a filter");
		}

	    @Override
	    public boolean setAsDefaultFeatureCollection() {
	        return false;
	    }
	}
}