package edu.asu.jmars.layer.map2;

import java.awt.geom.Area;
import java.io.Serializable;

public interface Stage extends Cloneable, Serializable {
	
	/**
	 * Returns the number of inputs taken by this stage.
	 */
	public int getInputCount();
	
	/**
	 * Returns the name of this stage.
	 */
	public String getStageName();
	
	/**
	 * Returns the {@link StageSettings} associated with this stage.
	 * It contains all the settings shared between Stages.
	 */
	public StageSettings getSettings();
	
	/**
	 * Process data through this stage returning the result.
	 * This method must be reentrant.
	 * @param inputNumber The serial number of the input at which the
	 *                    data has arrived.
	 * @param data Data to process through this stage.
	 * @param changedArea Area within the extents (or span) of data that
	 *        has changed in this data object. Subsequent calls will have
	 *        different areas that changed. Contents of the changedArea are
	 *        updated to reflect the new changed area on return from this call.
	 */
	public MapData process(int inputNumber, MapData data, Area changedArea);
	
	/**
	 * Checks to see if this stage can accept MapData described by
	 * attributes passed in.
	 * @param inputNumber The serial number at which this mapAttr will arrive.
	 * @return <code>true</code> if this stage can consume MapData
	 *         conforming to the specifications of MapAttr.
	 */
	public boolean canTake(int inputNumber, MapAttr mapAttr);
	
	/**
	 * Returns what does this stage takes as input at a particular input number.
	 * @param inputNumber 
	 * @return A non-null array of MapAttr objects that this stage can consume.
	 */
	public MapAttr[] consumes(int inputNumber);

	/**
	 * Returns the specifications of MapData objects produced by this
	 * stage.
	 */
	public MapAttr produces();
	
	public Object clone() throws CloneNotSupportedException;
}

