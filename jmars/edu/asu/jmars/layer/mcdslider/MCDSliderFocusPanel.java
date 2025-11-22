package edu.asu.jmars.layer.mcdslider;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.PAUSE;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.PLAY;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.STEP_NEXT;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.STEP_PREV;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.swing.BoundsPopupMenuListener;
import edu.asu.jmars.swing.ColorScale;
import edu.asu.jmars.swing.FancyColorMapper;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeText;
import edu.asu.jmars.util.Util;

/**
 * 
 * @author srcheese
 * @author sdickens
 *
 */
public class MCDSliderFocusPanel extends FocusPanel {
	private MCDSliderLView myLView;
	private MCDSliderLayer myLayer;
	MCDLayerSettings settings;	
	private JComboBox<String> typeBx;

	
	/* Used to map between the string sin the type box, and the string
	 * used on disc to store the map */
	private HashMap<String, String> typeToString;
	
	private int sigFigs = 4;

	//parts to the map path
	private String typeStr;
	
	private JCheckBox molaChk;
	
	private String minPrompt = "Min Value: ";
	private String maxPrompt = "Max Value: ";
	private JLabel minLbl;
	private JLabel maxLbl;
	
	public JTextField userMinVal = new JTextField("");
	public JTextField userMaxVal = new JTextField("");
	
	ArrayList<MCDSlider> sliders=new ArrayList<MCDSlider>();
	public FancyColorMapper colorMapper;
	
	// UI constants
	private int pad = 2;
	private Insets in = new Insets(pad,pad,pad,pad);
	
	private static final Color img = ((ThemeImages) GUITheme.get("images")).getFill();
	private static final Color disabledtext = ((ThemeText) GUITheme.get("text")).getTextDisabled();
	private static final ImageIcon play = new ImageIcon(ImageFactory.createImage(PLAY.withDisplayColor(img)));
	private static final ImageIcon playD = new ImageIcon(ImageFactory.createImage(PLAY.withDisplayColor(disabledtext)));
	
    private static final ImageIcon pause = new ImageIcon(ImageFactory.createImage(PAUSE.withDisplayColor(img)));
    private static final ImageIcon pauseD = new ImageIcon(ImageFactory.createImage(PAUSE.withDisplayColor(disabledtext)));	
	
    private static final ImageIcon prev = new ImageIcon(ImageFactory.createImage(STEP_PREV.withDisplayColor(img)));    
    private static final ImageIcon prevD = new ImageIcon(ImageFactory.createImage(STEP_PREV.withDisplayColor(disabledtext)));	
	
    private static final ImageIcon next = new ImageIcon(ImageFactory.createImage(STEP_NEXT.withDisplayColor(img)));
    private static final ImageIcon nextD = new ImageIcon(ImageFactory.createImage(STEP_NEXT.withDisplayColor(disabledtext)));	
		
	private JButton playButton;
	private JComboBox selectedSlider;
	private JSpinner movieSpinner;
	
	
	public MCDSliderFocusPanel(MCDSliderLView parent) {
		super(parent, false);
		myLView = parent;
		myLayer = (MCDSliderLayer)myLView.getLayer();
		settings = myLayer.settings;
		String sliderNames[] = myLayer.getParam(MCDSliderLayer.SLIDER_NAMES).split(",");

		if(settings.displayType == null){
			settings.displayType = sliderNames[0];
		}
				
		for (int i  = 0; i<sliderNames.length ; i++) {
			String sliderName = sliderNames[i];
			
			String sliderValues[]=myLayer.getParam("SLIDER_VALUES_"+sliderNames[i]).split(",");
			
			if (sliderValues.length==3) {
				int minValue = Integer.parseInt(sliderValues[0].trim());
				int maxValue = Integer.parseInt(sliderValues[1].trim());
				int stepValue = Integer.parseInt(sliderValues[2].trim());
				sliders.add(new MCDSlider(i, this, sliderName, minValue, maxValue, stepValue));
			} else if (sliderValues.length>3) {
				HashMap<Integer, Integer> valueToElevation = new HashMap<Integer, Integer>();
				
				for (int indx = 0; indx < sliderValues.length; indx++) {
					valueToElevation.put(indx, Integer.parseInt(sliderValues[indx].trim()));
				}

				// TODO: Do this MUCH safer, type checking, length checking, etc
				sliders.add(new MCDSlider(i, this, sliderName, 0, sliderValues.length-1, valueToElevation));
			} else {
				System.out.println("Error occurred....");
			}
			
		}
		
		typeToString = new LinkedHashMap<String, String>();

		String displayTypes[] = myLayer.getParam(MCDSliderLayer.DISPLAY_TYPE_MAP).split(",");
		
		for (int i=0; i<displayTypes.length; i+=2) {
			typeToString.put(displayTypes[i].trim(), displayTypes[i+1].trim());
		}
			
		add("Data", createDataTab());
		
		//all the focusPanel UI is finished, set the mapPath based off the
		// initial values and repaint
		setMapPath();
	}
	
	private JPanel createDataTab(){
		JPanel panel = new JPanel();
				
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		panel.add(createDataControlsPanel());
		panel.add(Box.createVerticalStrut(5));
		panel.add(createDisplaySettingsPanel());
		panel.add(Box.createVerticalStrut(5));
		panel.add(createPlayPanel());
		
		return panel;
	}
	
	private JPanel createDataControlsPanel(){
		JPanel innerPnl = new JPanel();
		innerPnl.setBorder(new TitledBorder("Data Controls"));
		innerPnl.setLayout(new GridBagLayout());
		
		JLabel typeLbl = new JLabel("Display Type: ");
		Vector<String> typeVec = new Vector<String>();
		
		for (String type : typeToString.keySet()) {
			typeVec.add(type);
		}
		
		//TODO: how do we know units?  Should we display that somewhere in the UI? 
		typeBx = new JComboBox<String>(typeVec);
		typeBx.addActionListener(typeListener);
		typeBx.setSelectedItem(settings.displayType);
		
	    //set a preferred size, and popup listener so the width can be shorter than the contents
		// have the width match the widest part of the focus panel (fancy color mapper)
		BoundsPopupMenuListener cbListener = new BoundsPopupMenuListener(true, false);
	    typeBx.setPreferredSize(new Dimension(180, typeBx.getPreferredSize().height));
	    typeBx.setMinimumSize(new Dimension(180, typeBx.getPreferredSize().height));
	    typeBx.addPopupMenuListener(cbListener);
		JPanel typePnl = new JPanel();
		typePnl.add(typeLbl);
		typePnl.add(typeBx);
		
		molaChk = new JCheckBox("Blend with MOLA Shaded Relief");
		molaChk.setSelected(settings.blendMola);
		
		molaChk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				settings.blendMola = molaChk.isSelected();
				myLView.repaint();
			}
		});
		
		JPanel typeAndBlendPnl = new JPanel(new BorderLayout());
		typeAndBlendPnl.add(typePnl, BorderLayout.CENTER);
		//Add an empty box on the left too so the type box and label can be
		// in the center of the panel (regardless of the checkbox on the right)
		typeAndBlendPnl.add(molaChk, BorderLayout.EAST);
		typeAndBlendPnl.add(Box.createHorizontalStrut(molaChk.getPreferredSize().width), BorderLayout.WEST);
		
	    int row = 0;
		int col = 0;
		innerPnl.add(typeAndBlendPnl, new GridBagConstraints(col, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		col = 0; row++;
		
		for (MCDSlider sliderPnl : sliders) {
			sliderPnl.setValue(0);
			
			col=0;
			innerPnl.add(sliderPnl, new GridBagConstraints(col, row, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
			row++;
		}
		
		return innerPnl;
	}
	
	private JPanel createDisplaySettingsPanel(){
		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder("Display Settings"));
		panel.setLayout(new GridBagLayout());
		
		//TODO: This should probably access the UI Theme to define the label font and style
		Font headerFont = new Font("Dialog", Font.PLAIN, 14);
		
		JLabel detectedLbl = new JLabel("<html><u>Detected Range</u></html>");
		detectedLbl.setFont(headerFont);
		minLbl = new JLabel(minPrompt);
		maxLbl = new JLabel(maxPrompt);
		
		JLabel stretchLbl = new JLabel("<html><u>Stretch Range</u></html>");
		stretchLbl.setFont(headerFont);
		JLabel stretchMinLbl = new JLabel("Min Value:");
		JLabel stretchMaxLbl = new JLabel("Max Value:");
		userMinVal = new JTextField(6);
		userMaxVal = new JTextField(6);
		if(settings.stretchMin != null){
			userMinVal.setText(settings.stretchMin+"");
		}
		if(settings.stretchMax != null){
			userMaxVal.setText(settings.stretchMax+"");
		}
		userMinVal.addActionListener(stretchActionListener);
		userMinVal.addFocusListener(stretchFocusListener);
		userMaxVal.addActionListener(stretchActionListener);
		userMaxVal.addActionListener(stretchActionListener);
		
		colorMapper = new FancyColorMapper();
		if(settings.mapperColors != null && settings.mapperVals != null){
			colorMapper.setColors(settings.mapperVals, settings.mapperColors);
		}
		colorMapper.addChangeListener(mapperChangeListener);
		
		int row = 0;
		panel.add(detectedLbl, new GridBagConstraints(0, row, 4, 1, 0, 0.5, GridBagConstraints.SOUTH, GridBagConstraints.NONE, in, pad, pad));
		row++;
		panel.add(minLbl, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(pad, 3*pad, pad, pad), pad, pad));
		row++;
		panel.add(maxLbl, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(pad, 3*pad, pad, pad), pad, pad));
		row++;
		panel.add(stretchLbl, new GridBagConstraints(0, row, 4, 1, 0, 0.5, GridBagConstraints.SOUTH, GridBagConstraints.NONE, in, pad, pad));
		row++;
		panel.add(stretchMinLbl, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(pad, 3*pad, pad, pad), pad, pad));
		panel.add(userMinVal, new GridBagConstraints(1, row, 1, 1, 0.5, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, in, pad, pad));
		row++;
		panel.add(stretchMaxLbl, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(pad, 3*pad, pad, pad), pad, pad));
		panel.add(userMaxVal, new GridBagConstraints(1, row, 1, 1, 0.5, 0, GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, in, pad, pad));
		row++;
		panel.add(colorMapper, new GridBagConstraints(0, row, 4, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		
		return panel;
	}
	
	private ChangeListener mapperChangeListener = new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
			//update mapper settings
			ColorScale scale = colorMapper.getColorScale();
			settings.mapperVals = scale.getColorVals();
			settings.mapperColors = scale.getColors();
			
			myLView.repaint();
		}
	};
	
	private FocusListener stretchFocusListener = new FocusListener() {
		public void focusLost(FocusEvent e) {
			updateStretch();
		}
		public void focusGained(FocusEvent e) {
		}
	};
	private ActionListener stretchActionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			updateStretch();
		}
	};	
	
	private void updateStretch(){
		//update the settings for min and max if they are different
		// than the current settings values
		if(userMinVal.getText().equals("")){
			if(settings.stretchMin != null){
				settings.stretchMin = null;
			}			
		}else{
			Double minVal = Double.parseDouble(userMinVal.getText());
			if(minVal != settings.stretchMin){
				settings.stretchMin = minVal;
			}
		}
		if(userMaxVal.getText().equals("")){
			if(settings.stretchMax != null){
				settings.stretchMax = null;
			}
		}else{
			Double maxVal = Double.parseDouble(userMaxVal.getText());
			if(maxVal != settings.stretchMax){
				settings.stretchMax = maxVal;
			}
		}
		
		//refresh the lview
		myLView.repaint();
	}

		private JPanel createPlayPanel() {
		// Code stolen from SliderFocusPanel
		
		// This next block of code creates the buttons for the PlayPanel-----------
		//--------  and defines their actionListeners------------------------------	
				
		final JButton prevButton = new JButton("Prev");
		setButton(prevButton, prev);
		prevButton.setDisabledIcon(prevD);
		prevButton.addActionListener(new ActionListener() {	
			public void actionPerformed(ActionEvent e) {
				MCDSlider slider = (MCDSlider)selectedSlider.getSelectedItem();
				int nextVal = nextSelection(false);
				slider.setValue(nextVal);
			}
		});
			
		playButton = new JButton("Play");
		setButton(playButton, play);
		playButton.setDisabledIcon(playD);
		playButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
            	playButton.setSelected(!playButton.isSelected());
            	if (playButton.isSelected()) {
            		playButton.setText("Pause");
            		playButton.setIcon(pause);
            		playButton.setDisabledIcon(pauseD);
            	} else {
            		playButton.setText("Play");
            		playButton.setIcon(play);
            	}
            	TimerTask timerTask = new TimerTask() {	
         			public void run() {
         				if (playButton.isSelected()) {
         					int nextVal = nextSelection(true);
         					
         					MCDSlider slider = (MCDSlider)selectedSlider.getSelectedItem();

         					final MCDSlider theSlider = slider;
         					final int theNextVal = nextVal+1;
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									theSlider.setValue(theNextVal);
								}
							});
         				} else {
         					cancel();
         				}
        			}			
        		};	
            	Timer playTimer=new Timer();
            	
            	playTimer.schedule(timerTask, settings.timeDelay, settings.timeDelay);
			}
		});

		final JButton nextButton = new JButton("Next");
		setButton(nextButton, next);
		nextButton.setDisabledIcon(nextD);
		nextButton.addActionListener(new ActionListener() {	
			public void actionPerformed(ActionEvent e) {
				MCDSlider slider = (MCDSlider)selectedSlider.getSelectedItem();
				int nextVal = nextSelection(true);
				slider.setValue(nextVal+1);
			}
		});
		//-----------------------------------------------------------------------------	
				
		// The playPanel contains the three control buttons, is added after slider
		JPanel playPanel = new JPanel();
		playPanel.add(Box.createRigidArea(new Dimension(3,0)));
		playPanel.add(prevButton);
		playPanel.add(Box.createRigidArea(new Dimension(3,0)));
		playPanel.add(playButton);
		playPanel.add(Box.createRigidArea(new Dimension(3,0)));
		playPanel.add(nextButton);	
		playPanel.add(Box.createRigidArea(new Dimension(3,0)));		
		// Creates the spinner and label go with it, they get added to the spinnerPanel		
		SpinnerNumberModel movieSpeedSpinner = new SpinnerNumberModel((settings.timeDelay/1000), 0.5, 5.0, 0.5);
	    movieSpinner = new JSpinner(movieSpeedSpinner);
	    movieSpinner.setValue((double)(settings.timeDelay/1000f));
	    movieSpinner.setEditor(new JSpinner.DefaultEditor(movieSpinner));
	    movieSpinner.setToolTipText("Use this value to control movie  speed");
	    //some wild guesses for size and location
	    movieSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
	    movieSpinner.setAlignmentY(Component.CENTER_ALIGNMENT);
	    movieSpinner.setPreferredSize(new Dimension(60,26));
	    movieSpinner.setMaximumSize(new Dimension(60,26));
	    movieSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				// TODO: time delay doesn't repopulate correctly when performing a save/restore.  Something related to the fractional value being saved
				
				settings.timeDelay=(int)(((Double)movieSpinner.getModel().getValue()).floatValue() * 1000);
				if (movieIsPlaying()) {
					pauseMovie();
				}
				myLView.repaint();
				if (myLView.getChild().isAlive()) {
					((MCDSliderLView)myLView.getChild()).repaint();
				}
			}
		});
	    JLabel spinLabel1 = new JLabel("Time delay:   ");
	    spinLabel1.setFont(new Font("Dialog",Font.BOLD,12));
	    JLabel spinLabel2 = new JLabel("  seconds");
	    spinLabel2.setFont(new Font("Dialog",Font.BOLD,12));
		// The spinnerPanel contain the spinner and it's label, this is added to the controlpanel
	    JPanel spinnerPanel = new JPanel();
	    spinnerPanel.setLayout(new BoxLayout(spinnerPanel,BoxLayout.LINE_AXIS));
	    spinnerPanel.add(spinLabel1);
	    spinnerPanel.add(movieSpinner);
	    spinnerPanel.add(spinLabel2);
		
		Vector sliderOptions = new Vector<MCDSlider>();
		for (MCDSlider slider : sliders) {
			sliderOptions.add(slider);
		}
		
		JLabel sliderLbl = new JLabel("Animation Variable:");
		selectedSlider = new JComboBox(sliderOptions);
		//set the initial settings
		if(settings.animateType == null){
			settings.animateType = selectedSlider.getSelectedItem().toString();
		}else{
			selectedSlider.setSelectedItem(settings.animateType);
		}
		//add a listener on the slider which updates the settings appropriately
		selectedSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//update settings
				settings.animateType = selectedSlider.getSelectedItem().toString();
			}
		});
		JPanel sliderPnl = new JPanel();
		sliderPnl.add(sliderLbl);
		sliderPnl.add(selectedSlider);
		
		// The animation panel contains a title, combobox for choosing
	    // the animation variable, and the playPanel (complete with buttons)		
		JPanel controlPanel = new JPanel();
		controlPanel.setBorder(new TitledBorder("Animate Controls"));
		controlPanel.setLayout(new GridBagLayout());

		controlPanel.add(sliderPnl, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		controlPanel.add(playPanel, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		controlPanel.add(spinnerPanel, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		
		return controlPanel;
	}

	private int nextSelection(boolean moveForward) {
		MCDSlider slider = (MCDSlider)selectedSlider.getSelectedItem();

		int curVal = slider.getValue();
		
		if (moveForward) {
			return  (curVal +1 > slider.getMaximum()) ? slider.getMinimum()-1 : curVal;
		} else { // the user is backing up the list
			if (curVal -1 < slider.getMinimum()) {
				return slider.getMaximum();
			} else {
				return curVal-1;
			}
		}
	}

	final protected boolean movieIsPlaying() {
		return playButton.isSelected();
	}
	
	final synchronized protected void pauseMovie() {
		playButton.doClick();
	}

	private void setButton(JButton b, ImageIcon i) {
		b.setIcon(i);
		b.setVerticalTextPosition(SwingConstants.BOTTOM);
		b.setHorizontalTextPosition(SwingConstants.CENTER);
	}

	private ActionListener typeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			//update settings
			settings.displayType = typeBx.getSelectedItem().toString();
			//reset the map path, to get the proper map to display
			setMapPath();
			//reset the preloads because data type has changed
			resetSliderPreloads();
		}
	};
		
	
	/**
	 * Calculates the filepath for the stamp cache for the map which
	 * corresponds to current type and slider values.
	 * This method is used when trying to set the state of preload 
	 * buttons.
	 * 
	 * @return The full file path for the cached stamp file for the
	 * map which corresponds to the selected type and slider values.
	 */
	public String calculateFilePath(){
		//set the vals
		typeStr = typeToString.get((String)typeBx.getSelectedItem());
				
		String fileName = "imageType="+typeStr+"&id=";
		
		for (MCDSlider slider : sliders) {
			fileName+=slider.getPrompt()+"_"+slider.getEffectiveValue()+",";
		}
		
		String fullPath = fileName+"version_"+myLayer.getVersionString();
		
		return myLayer.getImageUrlString(fullPath);
	}
	
	/**
	 * This method has the default package visibility so it can be called
	 * from MCDSlider objects when their slider value changes.  
	 * This method creates the proper map path string to be used to 
	 * display the proper image for the combination of current slider values
	 */
	void setMapPath(){
		//set the vals
		typeStr = typeToString.get((String)typeBx.getSelectedItem());
				
		String fileName = "imageType="+typeStr+"&id=";
		
		for (MCDSlider slider : sliders) {
			fileName+=slider.getPrompt()+"_"+slider.getEffectiveValue()+",";
		}
		
		String fullPath = fileName+"version_"+myLayer.getVersionString();

		
		myLayer.setPath(fullPath);
		//update the lview
		myLView.repaint();
		if (myLView.getChild()!=null) {
			myLView.getChild().repaint();
		}
	}
	
	public String getTypeString(){
		return typeStr;
	}
	
	private String formatNumberString(double number){
		String result = "";
		
		//check for NaN
		if(Double.isNaN(number)){
			result = "NaN";
		}
		else{
			//find the absolute val
			double absVal = Math.abs(number);
			
			//if the absolute value is 10,000 or larger, change
			// the number of sigfigs based on the power of 10
			int digits = sigFigs;
			if(absVal > 9999){
				digits = (int) Math.log(absVal);
			}
			
			//reduce the number based on significant figures
			result = String.format("%."+digits+"G", number);
			//remove all trailing zeroes
			//this line of code was found online (https://stackoverflow.com/questions/14984664/remove-trailing-zero-in-java)
			// added the second conditional in case of very small numbers (E-10)
			result = result.contains(".") && !result.contains("E") ? result.replaceAll("0*$","").replaceAll("\\.$","") : result;
			
			//use the decimal formatter to add commas, to make 
			// large numbers easier to read
			//TODO: removed the use of formatter so that small numbers use scientific notation
//			result = df.format(Double.parseDouble(result));
		}
		
		return result;
	}
	
	/**
	 * Set the label for the minimum value found on the 
	 * currently loaded map.  This sets a formatted number
	 * in the label, and the full value as a tooltip.
	 * @param val  The map's minimum value
	 */
	public void setMinValue(double val){
		minLbl.setText(minPrompt+formatNumberString(val));
		minLbl.setToolTipText(""+val);
	}
	
	/**
	 * Set the label for the maximum value found on the 
	 * currently loaded map.  This sets a formatted number
	 * in the label, and the full value as a tooltip.
	 * @param val  The map's maximum value
	 */
	public void setMaxValue(double val){
		maxLbl.setText(maxPrompt+formatNumberString(val));
		maxLbl.setToolTipText(""+val);
	}
	
	/**
	 * Call a reset on each of the sliders for their 
	 * preload button.  Each slider will check to see if
	 * they now do not have all the necessary maps loaded
	 * in the cache, and will properly enable the buttons
	 * when needed.
	 */
	public void resetSliderPreloads(){
		for (MCDSlider ms : sliders){
			ms.resetDataPreloaded();
		}
	}

}
