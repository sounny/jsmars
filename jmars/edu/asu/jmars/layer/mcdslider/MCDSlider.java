package edu.asu.jmars.layer.mcdslider;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.asu.jmars.layer.stamp.StampCache;

/**
 * This class is meant to be a GUI component that contains a slider,
 * label, and preload data button.
 * The slider is used to traverse through a particular axes in a data
 * set (ex. hour of day, day of year, height from surface, etc.).
 * The label is a readout of the current value (hour, elevation, etc).
 * The preload button is used to preload all of the data for that
 * axis (ex. load all the hours/days/heights/etc that are available).
 * These three components are laid out on a panel (this class) that
 * has the slider on top, oriented horizontally, with the label and
 * button positioned beneath the slider.
 */
public class MCDSlider extends JPanel {

	private JSlider slider; 
	private int min;
	private int max;
	private String prompt;
	private JLabel label;
	private JButton preloadButton;
	private boolean dataPreloaded;
	private boolean loadData;
	
	//keeps track of the index this particular slider is in the
	// focus layer array for sliders
	private int sliderIndex;
	
	private int step = -1; // Optional
	private HashMap<Integer,Integer> posToValue = null;
	private DecimalFormat df = new java.text.DecimalFormat("###,###.##############");

	// UI constants
	private int pad = 0;
	
	private MCDSliderFocusPanel myFocus;
	private MCDLayerSettings settings;
	
	public MCDSlider(int sliderIndex, MCDSliderFocusPanel fpanel, String newPrompt, int newMin, int newMax) {
		super();
		this.sliderIndex = sliderIndex;
		myFocus = fpanel;
		settings = myFocus.settings;
		prompt = newPrompt;
		min = newMin;
		max = newMax;
		
		dataPreloaded = false;
		loadData = true;
		
		//if slider position array isn't larger than slider index,
		// then add it by setting the value to 0
		if(!(settings.sliderPositions.size()>sliderIndex)){
			settings.sliderPositions.add(sliderIndex, 0);
		}
		
		buildUI();
	}
	
	private void buildUI(){
		slider = new JSlider(min, max);
		slider.addChangeListener(sliderListener);
		
		//set the initial value from the settings
		int val = settings.sliderPositions.get(sliderIndex);
		slider.setValue(val);
		
		Integer value = getEffectiveValue();

		label = new JLabel(prompt+": " + df.format(value));
		
		preloadButton = new JButton(loadAct);
		
		setLayout(new GridBagLayout());

		int col = 0;
		int row = 0;
		add(slider, new GridBagConstraints(col, row, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(pad, pad, 0, pad), pad, pad));
		row++;
		add(label, new GridBagConstraints(col, row, 1, 1, 1, 0, GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(0, pad, pad, pad), pad, pad));
		add(preloadButton, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, pad, pad, pad), pad, pad));
	}

	public MCDSlider(int sliderIndex, MCDSliderFocusPanel fpanel, String newPrompt, int newMin, int newMax, int newStep) {
		this(sliderIndex, fpanel, newPrompt, newMin, newMax);
		step=newStep;
	}

	public MCDSlider(int sliderIndex, MCDSliderFocusPanel fpanel, String newPrompt, int newMin, int newMax, HashMap<Integer,Integer> newPosToValues) {
		this(sliderIndex, fpanel, newPrompt, newMin, newMax);
		posToValue=newPosToValues;
	}

	
	/**
	 * @return The prompt which is the name of the axis 
	 * (ex. Hour, Elevation, etc)
	 */
	public String getPrompt() {
		return prompt;
	}
	
	
	/**
	 * @return The index of the current slider position
	 */
	public int getValue(){
		return slider.getValue();
	}
	
	/**
	 * @return The minimum index of the slider
	 */
	public int getMinimum(){
		return min;
	}
	
	/**
	 * @return The maximum index of the slider
	 */
	public int getMaximum(){
		return max;
	}
	
	/**
	 * @return The value that corresponds to the current slider
	 * index.  For some axes this may be the same number (hour
	 * is this way), but for others there may be a mapping
	 * between slider index and actual value (elevation can be
	 * this way).
	 */
	public int getEffectiveValue() {
		int sliderVal = slider.getValue();
		if (posToValue!=null) {
			return posToValue.get(sliderVal);
		} else if (step!=-1) {
			return sliderVal*step;
		} else {
			return sliderVal;
		}
	}
	
	
	/**
	 * Set the value on the slider
	 * @param value
	 */
	public void setValue(int value){
		slider.setValue(value);
	}
	
	/**
	 * Check to see if this combination of values has the
	 * data preloaded for it.  If so, the dataPreloaded 
	 * boolean stays true, if any data is not cached locally
	 * then the boolean is false.  The button is then updated
	 * to reflect this (disabled if all data is local, enabled
	 * if at least some data is missing).
	 */
	public void resetDataPreloaded(){
		//test to see if the maps for this combination of 
		// variables has already been cached locally
		
		//get the current slider value, because it needs to 
		// be reset to this value at then end
		int currentSliderVal = slider.getValue();
		//during this process disable the functionality in the
		// slider listener (don't want to be loading maps)
		loadData = false;
		//this is set to true, if it makes it through the entire
		// loop it stays true, if the loop ever breaks, this will
		// be false (meaning some of the data for the current 
		// combination of values isn't cached)
		dataPreloaded = true;
		//cycle through the slider values
		for(int i=min; i<=max; i++){
			slider.setValue(i);
			//get the cache filepath for this combination of values
			String path = myFocus.calculateFilePath();
			boolean exists = StampCache.fileExists(path);
			//if the file doesn't exist, don't need to finish loop,
			// set the dataPreloaded to false, and break loop
			if(exists == false){
				dataPreloaded = false;
				break;
			}
		}
		//return the slider back to the original value
		slider.setValue(currentSliderVal);
		//turn the slider listener functionality back on
		loadData = true;

		//enabled/disable the preload button 
		updatePreloadButton();
	}
	
	private void updatePreloadButton(){
		preloadButton.setEnabled(!dataPreloaded);
	}
	
	/**
	 * @return The prompt
	 */
	public String toString() {
		return prompt;
	}
	
	private AbstractAction loadAct = new AbstractAction("Preload") {
		public void actionPerformed(ActionEvent arg0) {	
			//change cursor for some user feedback
			myFocus.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			for (int i=min; i<=max; i++) {
				slider.setValue(i);
			}
			
			//now that data is preloaded, update button appearance
			dataPreloaded = true;
			updatePreloadButton();
			
			//work is done, return to normal cursor
			myFocus.setCursor(Cursor.getDefaultCursor());
		}
	};
	
	private ChangeListener sliderListener = new ChangeListener() {
		Timer timer = new Timer(250, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				timer.stop();
				//if all the sliders are done moving, and are not null, pull new map
				if(slider!=null){  // Seems unnecessary?
					myFocus.setMapPath();
				}
				
				//reset preload boolean on sliders
				myFocus.resetSliderPreloads();				
			}
		});

		public void stateChanged(ChangeEvent e) {
			//only perform logic if the loadData boolean is true
			if(loadData){
				Integer value = getEffectiveValue();
				
				//set the setting value
				settings.sliderPositions.set(sliderIndex, value);

				//set the label, and update the map if needed
				if(value != null && label != null && prompt != null){
					label.setText(prompt+": "+df.format(value));

					// Without this, we get three updates per slider change
					if (slider.getValueIsAdjusting()) {
						timer.restart();
						return;
					}
										
					//if all the sliders are done moving, and are not null, pull new map
					if(slider!=null){  // Seems unnecessary?
						myFocus.setMapPath();
					}
					
					//reset preload boolean on sliders
					myFocus.resetSliderPreloads();
				}
			}
		}
	};
}
