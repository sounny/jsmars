package edu.asu.jmars.layer.map2;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.util.Arrays;

import edu.asu.jmars.graphics.EmptyColorModel;
import edu.asu.jmars.util.ImageOrg;
import edu.asu.jmars.util.Util;

/**
 * Describes a type of map data. The data can consist of any number of planes,
 * but each plane must have the same type of data.
 * 
 * Number of bands is the most important property of a MapAttr. Image data of
 * an unexpected data type can always be scaled (e.g. convert float to byte),
 * but there is no standard answer for unexpected band count.
 * 
 * In the worst-case the only way to divine the number of bands will be to
 * actually request a sample and create a MapAttr from it.
 */
public class MapAttr {
	/** Predefined single band grayscale image MapAttr */
	public static final MapAttr GRAY        = new MapAttr(DataBuffer.TYPE_BYTE, 1, null);
	/** Predefined three band color image MapAttr */
	public static final MapAttr COLOR       = new MapAttr(DataBuffer.TYPE_BYTE, 3, null);
	/** Predefined single band image MapAttr */
	public static final MapAttr SINGLE_BAND = new MapAttr(null, 1, null);
	/** Predefined any configuration image MapAttr */
	public static final MapAttr ANY         = new MapAttr(null, null, null);
	
	/** Data type of each plane, or null if the data type does not matter */
	private final Integer dataType;
	
	/** Number of bands, or null if the number does not matter */
	private final Integer numBands;
	
	/** Number of bands, or null if the alpha state does not matter */
	private final Boolean hasAlpha;
	
	/** True if the map source was initialized with an image sample that was bad */
	private final boolean failed;
	
	/** Create a MapAttr with the given data type, color band count, and alpha setting */
	public MapAttr(Integer dataType, Integer numBands, Boolean hasAlpha) {
		this.dataType = dataType;
		this.numBands = numBands;
		this.hasAlpha = hasAlpha;
		this.failed = false;
	}
	
	/** Creates a MapAttr by inspecting bands, color model, and raster data type */
	public MapAttr(BufferedImage sample) {
		boolean result;
		Boolean hasAlpha;
		Integer dataType;
		Integer numBands;
		try {
			numBands = sample.getColorModel().getNumColorComponents();
			hasAlpha = sample.getColorModel().hasAlpha();
			dataType = sample.getSampleModel().getDataType();
			result = false;
		} catch (Exception e) {
			numBands = null;
			hasAlpha = null;
			dataType = null;
			result = true;
		}
		this.numBands = numBands;
		this.hasAlpha = hasAlpha;
		this.dataType = dataType;
		this.failed = result;
	}
	
	/**
	 * @return Null if there is no specified data type, otherwise one of the values returned by
	 * {@link java.awt.image.Raster.DataBuffer#getDataType DataBuffer.getDataType()}.
	 */
	public Integer getDataType() {
		return dataType;
	}
	
	/** @return Null if there is no defined alpha, otherwise the Boolean for the presence of alpha */ 
	public Boolean hasAlpha() {
		return hasAlpha;
	}
	
	/** @return Null if there is no defined number of color bands, otherwise the count of color bands */
	public Integer getNumColorComp() {
		return numBands;
	}
	
	/** When this is true, callers should NOT attempt to get maps through this MapSource */
	public boolean isFailed() {
		return failed;
	}
	
	/**
	 * Returns <code>true</code> if this <code>MapAttr</code> is compatible with
	 * any of the given <code>MapAttr</code>s.
	 */
	public boolean isCompatible(MapAttr[] tgtAttrs){
		for(int i=0; i<tgtAttrs.length; i++)
			if (isCompatible(tgtAttrs[i]))
				return true;
		return false;
	}
	
	public BufferedImage createCompatibleImage(Dimension size) {
		return createCompatibleImage(size.width, size.height);
	}
	
	/**
	 * @return <code>true</code> if the given target type can handle data of this
	 *         type, meaning the target data type is undefined or equal, the
	 *         target color band count is undefined or equal, and the target
	 *         alpha value is undefined or equal.
	 */
	public boolean isCompatible(MapAttr tgtAttr) {
		return (tgtAttr.getDataType() == null || tgtAttr.getDataType().equals(getDataType())) &&
			(tgtAttr.getNumColorComp() == null || tgtAttr.getNumColorComp().equals(getNumColorComp())) &&
			(tgtAttr.hasAlpha() == null || tgtAttr.hasAlpha().equals(hasAlpha()));
	}
	
	/**
	 * Creates an image that will be compatible with this MapAttr; this routine
	 * should only be called on a fully-defined MapAttr, that does not return null
	 * from any of the get methods.
	 */
	public BufferedImage createCompatibleImage(int width, int height) {
		if (failed) {
			throw new IllegalStateException("Cannot create an image from a failed MapAttr");
		}
		if (dataType == null) {
			throw new IllegalStateException("Cannot create an image without a data type");
		}
		if (numBands == null) {
			throw new IllegalStateException("Cannot create an image without a number of bands");
		}
		if (hasAlpha == null) {
			throw new IllegalStateException("Cannot create an image without an alpha requirement");
		}
		if (isColor()) {
			if (hasAlpha) {
				return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			} else {
				return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			}
		} else if (isGray()) {
			if (hasAlpha) {
				int[] bits = new int[numBands];
				Arrays.fill(bits, DataBuffer.getDataTypeSize(dataType));
				ColorModel cm = new ComponentColorModel(Util.getLinearGrayColorSpace(),
						bits, hasAlpha, false, hasAlpha? ColorModel.TRANSLUCENT: ColorModel.OPAQUE, dataType);
				return ImageOrg.BSQ.createImage(dataType, width, height, 2, cm);
			} else {
				return new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			}
		} else {
			if (hasAlpha) {
				throw new IllegalStateException("Unable to create numeric image with an alpha band");
			} else {
				return ImageOrg.BIP.createImage(dataType, width, height, numBands, new EmptyColorModel());
			}
		}
	}
	
	public boolean isColor() {
		return !failed && numBands != null && dataType != null && numBands == 3 && dataType == DataBuffer.TYPE_BYTE;
	}
	
	public boolean isGray() {
		return !failed && numBands != null && dataType != null && numBands == 1 && dataType == DataBuffer.TYPE_BYTE;
	}
	
	public boolean isNumeric() {
		return !failed && !isColor() && !isGray();
	}
	
	public String toString(){
		String typeName;
		if (dataType == null) {
			typeName = "*";
		} else {
			switch(getDataType()){
			case DataBuffer.TYPE_BYTE:   typeName = "byte";   break;
			case DataBuffer.TYPE_SHORT:  typeName = "short";  break;
			case DataBuffer.TYPE_USHORT: typeName = "ushort"; break;
			case DataBuffer.TYPE_INT:    typeName = "int";    break;
			case DataBuffer.TYPE_FLOAT:  typeName = "float";  break;
			case DataBuffer.TYPE_DOUBLE: typeName = "double"; break;
			default:                     typeName = "*"; break;
			}
		}
		
		return "(" + typeName + "," + (getNumColorComp() == null ? "*": Integer.toString(getNumColorComp())) + ")";
	}
}
