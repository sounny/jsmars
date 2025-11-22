package edu.asu.jmars.util;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

/**
 * Provides a convenient way to create a sample model of a given size and
 * data type, with the common variations in internal pixel sample
 * organization.
 */
public enum ImageOrg {
	/** Band interleaved by line */
	BIL,
	/** Band interleaved by pixel */
	BIP,
	/** Band sequential */
	BSQ;
	/** Returns a sample model for this organization */
	SampleModel createSampleModel(int dataType, int x, int y, int z) {
		int[] bandOffsets = new int[z];
		switch (this) {
		case BIP:
			for(int i=0; i<z; i++) {
				bandOffsets[i] = i;
			}
			return new PixelInterleavedSampleModel(dataType, x, y, z, x*z, bandOffsets);
		case BIL:
			for(int i=0; i < z; i++) {
				bandOffsets[i] = i*x;
			}
			return new PixelInterleavedSampleModel(dataType, x, y, 1, x*z, bandOffsets);
		case BSQ:
			for(int i=0; i < z; i++) {
				bandOffsets[i] = i*x*y;
			}
			return new PixelInterleavedSampleModel(dataType, x, y, 1, x, bandOffsets);
		default:
			throw new IllegalStateException("Unimplemented organization type");
		}
	}
	/** Returns a new image with the given sample type, pixel volume and organization, and color model. */
	public BufferedImage createImage(int dataType, int width, int height, int numBands, ColorModel cm) {
		SampleModel sm = this.createSampleModel(dataType, width, height, numBands);
		WritableRaster wr = WritableRaster.createWritableRaster(sm, sm.createDataBuffer(), null);
		return new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
	}
}

