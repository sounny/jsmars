package edu.asu.jmars.graphics;

import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;

/** Returns transparent black for all pixels */
public final class EmptyColorModel extends ColorModel {
	public EmptyColorModel() {
		super(32);
	}

	public boolean isCompatibleRaster(Raster raster) {
		return true;
	}

	public boolean isCompatibleSampleModel(SampleModel sm) {
		return true;
	}

	public int getAlpha(int pixel) {
		return 0;
	}

	public int getBlue(int pixel) {
		return 0;
	}

	public int getGreen(int pixel) {
		return 0;
	}

	public int getRed(int pixel) {
		return 0;
	}
}
