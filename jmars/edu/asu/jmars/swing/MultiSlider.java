package edu.asu.jmars.swing;

import edu.asu.jmars.util.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;
import javax.swing.plaf.*;

/**
 ** A {@link JSlider}-like control that allows for multiple sliders on
 ** a single track.
 **
 ** <p>This is a nearly-100%-perfectly-kosher implementation. To avoid
 ** any weirdness, though, you should avoid the following methods:
 ** <ul>
 ** <li>{@link #setMinimum}</li>
 ** <li>{@link #setMaximum}</li>
 ** <li>{@link #setModel}</li>
 ** </ul>
 **
 ** <p>Those methods have not yet been 100% properly handled.
 ** Additionally, you should avoid any setter methods on the model
 ** that affect the value, minimum, or maximum. These deficiencies are
 ** fixable easily, I just haven't bothered to do so yet.
 **
 ** <p>Also beware of supplying out-of-order or out-of-range values
 ** for tab values in value arrays, as well as supplying invalid tab
 ** numbers.
 **
 ** <p><b>TEMPORARY PROBLEM: Some recent quick additions to the class
 ** may be incompatible with vertical orientation: the
 ** getXxxxPadding() routines, the locationToXxxx() and
 ** getTabLocation() routines, and middle-click-dragging.</b>
 **
 ** <p>Aside from these specific deficiencies (which are actually
 ** quite limited), EVERYTHING in this class works according to the
 ** JSlider spec. Pretty cool, eh?
 **/
public class MultiSlider extends JSlider
 {
	private static DebugLog log = DebugLog.instance();

	protected int[] values;
	protected int activeTab;

	public MultiSlider()
	 {
		this(HORIZONTAL, 0, 100, new int[] { 50 });
	 }

	public MultiSlider(int orientation)
	 {
		this(orientation, 0, 100, new int[] { 50 });
	 }

	public MultiSlider(int min, int max)
	 {
		this(HORIZONTAL, min, max, new int[] { (min+max) / 2 });
	 }

	public MultiSlider(int min, int max, int tabCount)
	 {
		this(HORIZONTAL, min, max, createValueArray(min, max, tabCount));
	 }

	public MultiSlider(int orientation, int min, int max, int tabCount)
	 {
		this(orientation, min, max, createValueArray(min, max, tabCount));
	 }

	public MultiSlider(int min, int max, int[] values)
	 {
		this(HORIZONTAL, min, max, values);
	 }

	public MultiSlider(int orientation, int min, int max, int[] values)
	 {
		super(orientation, min, max, values[0]);

		this.values = (int[]) values.clone();
		this.activeTab = 0;

		setSnapToTicks(true);
		updateSingleValue();
	 }

	/**
	 ** Internal convenience method for generating
	 ** <code>tabCount</code> tab values between min and max
	 ** (inclusive).
	 **/
	private static int[] createValueArray(int min, int max, int tabCount)
	 {
		if(tabCount == 1)
			return  new int[] { (min + max) / 2 };

		int[] values = new int[tabCount];
		--tabCount;
		for(int i=0; i<tabCount; i++)
			values[i] = ( (tabCount-i)*min + i*max ) / tabCount;
		values[tabCount] = max;
		return  values;
	 }

	/**
	 ** Returns the number of pixels of "padding" between the left
	 ** side of the track and the edge of the component itself.
	 **/
	public int getLeftPadding()
	 {
		return  ((NonMetalMultiSliderUI) ui).getLeftPadding();
	 }

	/**
	 ** Returns the number of pixels of "padding" between the right
	 ** side of the track and the edge of the component itself.
	 **/
	public int getRightPadding()
	 {
		return  ((NonMetalMultiSliderUI) ui).getRightPadding();
	 }

	/**
	 ** Returns the tab number for a horizontal point in component
	 ** coordinates.  Returns -1 if the point doesn't fall on any tab.
	 **/
	public int locationToTab(int x)
	 {
		return  ((NonMetalMultiSliderUI) ui).locationToTab(x);
	 }

	/**
	 ** Returns the "value" on the track for a horizontal point in
	 ** component coordinates. Returns -1 if the point doesn't fall
	 ** within the track area.
	 **/
	public int locationToValue(int x)
	 {
		return  ((NonMetalMultiSliderUI) ui).locationToValue(x);
	 }

	/**
	 ** Returns the horizontal location of the current tab.
	 **/
	public int getTabLocation()
	 {
		return  ((NonMetalMultiSliderUI) ui).getTabLocation();
	 }

	/**
	 ** Overridden to return the value of the current active tab.
	 **
	 ** @see #setActiveTab
	 **/
	public int getValue()
	 {
		return  super.getValue();
	 }

	/**
	 ** Gets a particular tab's value.
	 **/
	public int getValue(int tab)
	 {
		return  values[tab];
	 }

	/**
	 ** Overridden to set the value of the current active tab.
	 **
	 ** @see #setActiveTab
	 **/
	public void setValue(int value)
	 {
		setValue(activeTab, value);
	 }

	/**
	 ** Sets a particular tab's value.
	 **/
	public void setValue(int tab, int value)
	 {
		value = forceValueRange(tab, value);
		values[tab] = value;
		if(tab == activeTab)
			updateSingleValue(); ////////// Does this trigger a redraw??????
	 }

	/**
	 ** Returns an array containing a copy of the entire set of tab
	 ** values.
	 **/
	public int[] getValues()
	 {
		return  (int[]) values.clone();
	 }

	/**
	 ** Replaces the current set of tabs/values with a copy of the
	 ** supplied array. The active tab is reset to zero.
	 **
	 ** <p>The values must be in strictly ascending order; any
	 ** duplicate or out-of-order values may produce unpredictable
	 ** behavior. Any values outside the min/max range of the slider
	 ** may produce unpredictable behavior.
	 **/
	public void setValues(int[] values)
	 {
		this.values = (int[]) values.clone();
		activeTab = Math.min(values.length-1, activeTab);
		updateSingleValue();
	 }

	/**
	 ** Given a potential value for a tab, bounds it to within the
	 ** min/max of the slider and the values of the surrounding tabs.
	 **/
	private int forceValueRange(int tab, int value)
	 {
		int min = tab == 0               ? getMinimum() : values[tab-1] + 1;
		int max = tab == values.length-1 ? getMaximum() : values[tab+1] - 1;

		if(value < min) return min;
		if(value > max) return max;
		return  value;
	 }

	/**
	 ** Internal method to update the base slider's "pretend" single
	 ** value. This ensures that the proper notifications and redraws
	 ** occur.
	 **/
	private void updateSingleValue()
	 {
		boolean mustFireManually = values[activeTab] == getValue();
		super.setValue(values[activeTab]);
		if(mustFireManually)
			fireStateChanged();
		repaint();
	 }

	/**
	 ** Returns the current active tab, which is the one implemented
	 ** by the base slider's "pretend" single value.
	 **/
	public int getActiveTab()
	 {
		return  activeTab;
	 }

	/**
	 ** Sets the current active tab, which is the one implemented by
	 ** the base slider's "pretend" single value.
	 **/
	public void setActiveTab(int activeTab)
	 {
		this.activeTab = activeTab;
		updateSingleValue();
	 }

	/**
	 ** Returns the number of values/tabs represented by this
	 ** MultiSlider.
	 **/
	public int getValueCount()
	 {
		return  values.length;
	 }

	/**
	 ** Returns the max allowable value for a particular tab. This is
	 ** determined by the max of the slider as well as the next tab's
	 ** value (if there is one).
	 **/
	public int getMax(int tab)
	 {
		if(tab < 0)
			tab = 0;

		if(tab >= values.length-1)
			return  getMaximum();
		return  values[tab+1]-1;
	 }

	/**
	 ** Returns the min allowable value for a particular tab. This is
	 ** determined by the min of the slider as well as the previous
	 ** tab's value (if there is one).
	 **/
	public int getMin(int tab)
	 {
		if(tab > values.length-1)
			throw  new IllegalArgumentException("Invalid tab number");

		if(tab <= 0)
			return  getMinimum();
		return  values[tab-1]+1;
	 }

	
    public void updateUI()
	 {
        updateLabelUIs();
        setUI(NonMetalMultiSliderUI.createUI(this));
	 }
 }
