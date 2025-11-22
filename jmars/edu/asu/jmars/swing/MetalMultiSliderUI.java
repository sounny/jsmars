package edu.asu.jmars.swing;

import edu.asu.jmars.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;

/**
 ** A Java L&F implementation of SliderUI intended to be used for
 ** MultiSlider. As per what appears to be swing convention, you
 ** should instantiate this class through the method {@link
 ** #createUI}.
 **/
public class MetalMultiSliderUI extends MetalSliderUI
 {
	private static DebugLog log = DebugLog.instance();

	protected MultiSlider mslider;
	protected Rectangle extraThumbRect = null;

    public static ComponentUI createUI(JComponent c)
	 {
        return new MetalMultiSliderUI();
	 }

    public void installUI(JComponent c)
	 {
		extraThumbRect = new Rectangle();
		mslider = (MultiSlider) c;

        super.installUI( c );
	 }

	/**
	 ** Implements control-arrow tab-hopping behavior.
	 **/
	private class MultiKeyListener extends KeyAdapter
	 {
		public void keyPressed(KeyEvent e)
		 {
			if(e.isControlDown())
			 {
				int mod = mslider.getValueCount();
				int curr = mslider.getActiveTab() + mod;
				switch(e.getKeyCode())
				 {
				 case KeyEvent.VK_UP:
				 case KeyEvent.VK_RIGHT:
					mslider.setActiveTab(++curr % mod);
					break;

				 case KeyEvent.VK_DOWN:
				 case KeyEvent.VK_LEFT:
					mslider.setActiveTab(--curr % mod);
					break;
				 }
			 }
		 }
	 }
	private KeyListener multiKeys = new MultiKeyListener();
    protected void installKeyboardActions(JSlider slider)
	 {
		super.installKeyboardActions(slider);
		slider.addKeyListener(multiKeys);
	 }
    protected void uninstallKeyboardActions(JSlider slider)
	 {
		slider.removeKeyListener(multiKeys);
		super.uninstallKeyboardActions(slider);
	 }

	/**
	 ** Adds to {@link TrackListener} the ability to select a new
	 ** active tab with the mouse. Also adds middle-mouse-button
	 ** dragging.
	 **/
	protected class MultiTrackListener extends TrackListener
	 {
		int[] vals;

        public void mousePressed(MouseEvent e)
		 {
            if(!slider.isEnabled()  ||  SwingUtilities.isRightMouseButton(e))
                return;

            currentMouseX = e.getX();
            currentMouseY = e.getY();

            slider.requestFocus();

			int tab = locationToTab(e.getPoint().x);
			if(tab == -1)
				return;

			if(tab != mslider.getActiveTab())
			 {
				mslider.setValueIsAdjusting(true);
				mslider.setActiveTab(tab);
			 }

			super.mousePressed(e);

			if(SwingUtilities.isMiddleMouseButton(e))
				vals = mslider.getValues();
		 }

		public void mouseReleased(MouseEvent e)
		 {
			if(!slider.isEnabled())
                return;
			super.mouseReleased(e);
			vals = null;
		 }

		public void mouseDragged(MouseEvent e)
		 {
			if(!slider.isEnabled())
                return;
			super.mouseDragged(e);
			if(vals == null)
				return;

			// Bunches of convenience values
			int count = vals.length;
			int last = count-1;
			int active = mslider.getActiveTab();
			int oldVal = vals[active];
			// We want the user's INTENDED value, not the model's value
			int newVal = valueForXPosition(thumbRect.x + thumbRect.width/2);

			// Determine whether we're stretching or shifting, and set
			// appropriate bounds for the active tab.
			boolean stretch = count != 2  &&  (active == 0 || active == last);
			int minVal =
				stretch && active == last ? vals[0] : mslider.getMinimum();
			int maxVal =
				stretch && active == 0 ? vals[last] : mslider.getMaximum();
			// Back-off to leave enough room from the bound for the
			// other sliders to fit in.
			newVal = Util.bound(minVal + active,
								newVal,
								maxVal + active-last);

			// Create the new values we'd like to have
			int[] newvals = new int[count];
			if(stretch)
			 {
				// Stretch
				int base = vals[active == 0 ? last : 0];
				float scale = (float) (newVal - base) / (oldVal - base);
				for(int i=0; i<count; i++)
					newvals[i] = Math.round((vals[i]-base) * scale) + base;
			 }
			else
			 {
				// Shift
				int offset = newVal - oldVal;
				for(int i=0; i<count; i++)
					newvals[i] = Util.bound(minVal, vals[i] + offset, maxVal);
			 }
			newvals[active] = newVal;

			// Fix overlapping values before active
			for(int i=1; i<active; i++)
				if(newvals[i] <= newvals[i-1]) newvals[i] = newvals[i-1] + 1;
			for(int i=active-1; i>=0; i--)
				if(newvals[i] >= newvals[i+1]) newvals[i] = newvals[i+1] - 1;

			// Fix overlapping values after active
			for(int i=last-1; i>active; i--)
				if(newvals[i] >= newvals[i+1]) newvals[i] = newvals[i+1] - 1;
			for(int i=active+1; i<=last; i++)
				if(newvals[i] <= newvals[i-1]) newvals[i] = newvals[i-1] + 1;

			// Done!
			mslider.setValues(newvals);
		 }
	 }
    protected TrackListener createTrackListener(JSlider slider)
	 {
        return new MultiTrackListener();
	 }

	/**
	 ** Returns the number of pixels of "padding" between the left
	 ** side of the track and the edge of the component itself.
	 **/
	public int getLeftPadding()
	 {
		return  trackRect.x;
	 }

	/**
	 ** Returns the number of pixels of "padding" between the right
	 ** side of the track and the edge of the component itself.
	 **/
	public int getRightPadding()
	 {
		return  mslider.getWidth() - trackRect.x - trackRect.width;
	 }

	/**
	 ** Returns the tab number for a horizontal point in component
	 ** coordinates.  Returns -1 if the point doesn't fall on any tab.
	 **/
	public int locationToTab(int x)
	 {
		int y = (int) getThumbRect(0).getCenterY();
		for(int i=0; i<mslider.getValueCount(); i++)
			if(getThumbRect(i).contains(x, y))
				return  i;
		return  -1;
	 }

	/**
	 ** Returns the "value" on the track for a horizontal point in
	 ** component coordinates. Returns -1 if the point doesn't fall
	 ** within the track area.
	 **/
	public int locationToValue(int x)
	 {
		if(trackRect.x <= x  &&  x < trackRect.x+trackRect.width)
			return  (x - trackRect.x) * 256 / trackRect.width;
		return  -1;
	 }

	/**
	 ** Returns the horizontal location of the current tab.
	 **/
	public int getTabLocation()
	 {
		Rectangle thumbRect = getThumbRect(mslider.getActiveTab());
		int x = thumbRect.x + thumbRect.width/2;
		return  x;
	 }

    public void paint( Graphics g, JComponent c )  
	 {
        recalculateIfInsetsChanged();
		recalculateIfOrientationChanged();
		Rectangle clip = g.getClipBounds();

		if ( slider.getPaintTrack() && clip.intersects( trackRect ) )
		   paintTrack( g );

        if ( slider.getPaintTicks() && clip.intersects( tickRect ) )
		   paintTicks( g );

        if ( slider.getPaintLabels() && clip.intersects( labelRect ) )
		   paintLabels( g );

		if ( slider.hasFocus() && clip.intersects( focusRect ) )
		   paintFocus( g );      

		for(int i=mslider.getValueCount()-1; i>=0; i--)
		 {
			Rectangle thumbRect = getThumbRect(i);
			if(clip.intersects(thumbRect))
				paintThumb(g, thumbRect, i == mslider.getActiveTab());
		 }
	 }

    protected void calculateThumbSize()
	 {
		super.calculateThumbSize();
		extraThumbRect.setSize(thumbRect.width, thumbRect.height);
	 }

	private Rectangle getThumbRect(int tab)
	 {
		if(tab != mslider.getActiveTab())
			return  getThumbRect(tab, mslider.getValue(tab));

		// I know it seems silly, but we have to implement the
		// intra-slider bounds HERE in order to enforce them while the
		// user is dragging with the mouse.
		if(tab != 0)
		 {
			Rectangle min = getThumbRect(-1, mslider.getMin(tab));
			if(slider.getOrientation() == JSlider.HORIZONTAL)
			 {
				if(thumbRect.x < min.x)
					thumbRect.x = min.x;
			 }
			else
			 {
				if(thumbRect.y < min.y)
					thumbRect.y = min.y;
			 }
		 }
		if(tab != mslider.getValueCount()-1)
		 {
			Rectangle max = getThumbRect(-1, mslider.getMax(tab));
			if(slider.getOrientation() == JSlider.HORIZONTAL)
			 {
				if(thumbRect.x > max.x)
					thumbRect.x = max.x;
			 }
			else
			 {
				if(thumbRect.y > max.y)
					thumbRect.y = max.y;
			 }
		 }
		return  thumbRect;
	 }

	/**
	 ** Mostly derived from BasicSliderUI.calculateThumbLocation().
	 **/
	private Rectangle getThumbRect(int tab, int sliderValue)
	 {
		Rectangle thumbRect = extraThumbRect;
		int sliderValueOrig = sliderValue;
		if ( slider.getSnapToTicks() )
		 {
			int snappedValue = sliderValue; 
			int majorTickSpacing = slider.getMajorTickSpacing();
			int minorTickSpacing = slider.getMinorTickSpacing();
			int tickSpacing = 0;
		
			if ( minorTickSpacing > 0 )
			 {
				tickSpacing = minorTickSpacing;
			 }
			else if ( majorTickSpacing > 0 )
			 {
				tickSpacing = majorTickSpacing;
			 }

			if ( tickSpacing != 0 )
			 {
				// If it's not on a tick, change the value
				if ( (sliderValue - slider.getMinimum()) % tickSpacing != 0 )
				 {
					float temp = (float)(sliderValue - slider.getMinimum())
						/ (float)tickSpacing;
					int whichTick = Math.round( temp );
					snappedValue = slider.getMinimum()
						+ (whichTick * tickSpacing);
				 }
		
				if( snappedValue != sliderValue )
				 { 
					if(tab != -1)
						mslider.setValue( tab, snappedValue );
				 }
			 }
		 }
	
		if ( slider.getOrientation() == JSlider.HORIZONTAL )
		 {
			int valuePosition = xPositionForValue(
				tab != -1
				? mslider.getValue(tab)
				: sliderValueOrig);

			thumbRect.x = valuePosition - (thumbRect.width / 2);
			thumbRect.y = trackRect.y;
		 }
		else
		 {
			int valuePosition = yPositionForValue(
				tab != -1
				? mslider.getValue(tab)
				: sliderValueOrig);
		
			thumbRect.x = trackRect.x;
			thumbRect.y = valuePosition - (thumbRect.height / 2);
		 }

		return  extraThumbRect;
	 }

	/**
	 ** @deprecated No longer meaningful to paint "the" thumb, since
	 ** we now have multiple thumbs.
	 **/
    public void paintThumb(Graphics g) 
	 {
		log.aprintStack(-1);
		throw  new Error("THIS METHOD IS DEPRECATED");
	 }

	/**
	 ** New version of paintThumb that's parameterized to allow for
	 ** multiple-tab painting.
	 **/
    public void paintThumb(Graphics g, Rectangle knobBounds, boolean activeTab)
	 {
        g.translate(knobBounds.x, knobBounds.y);

        if(slider.getOrientation() == JSlider.HORIZONTAL)
            horizThumbIcon.paintIcon(getFakeSlider(activeTab), g, 0, 0);
        else
            vertThumbIcon.paintIcon(getFakeSlider(activeTab), g, 0, 0);

        g.translate(-knobBounds.x, -knobBounds.y);
	 }

	/**
	 ** This method returns a slider that vaguely "looks like" a
	 ** slider for a tab on our MultiSlider. The thumb icons paint to
	 ** a slider differently depending on whether it's enabled and/or
	 ** in focus, so this method is used to return a slider that fakes
	 ** them out.
	 **/
	private JSlider getFakeSlider(boolean forActiveTab)
	 {
		boolean focus = forActiveTab  &&  mslider.hasFocus();

		if(mslider.isEnabled()) return focus ? FakeSlider.EF : FakeSlider.Ex;
		else                    return focus ? FakeSlider.xF : FakeSlider.xx;
	 }

	/**
	 ** Used solely by {@link #getFakeSlider}.
	 **/
	private static class FakeSlider extends JSlider
	 {
		boolean enabled;
		boolean focused;
		FakeSlider(boolean enabled, boolean focused)
		 {
			this.enabled = enabled;
			this.focused = focused;
		 }

		public boolean isEnabled()
		 {
			return  enabled;
		 }

		public boolean hasFocus()
		 {
			return  focused;
		 }

		// We create four sliders, one for each possible combination
		// of enabled/focused.
		static JSlider xx = new FakeSlider(false, false);
		static JSlider Ex = new FakeSlider(true,  false);
		static JSlider EF = new FakeSlider(true,  true );
		static JSlider xF = new FakeSlider(false, true );
	 }

 }
