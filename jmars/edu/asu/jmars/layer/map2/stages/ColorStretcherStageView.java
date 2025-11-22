package edu.asu.jmars.layer.map2.stages;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.StageSettings;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.swing.ColorMapper;
import edu.asu.jmars.swing.FancyColorMapper;

public class ColorStretcherStageView implements StageView, PropertyChangeListener {
	
	ColorStretcherStageSettings settings;
	JPanel stagePanel;
	FancyColorMapper fcm;
//	JComboBox stageSelection = null;
    JPanel slim, mapperPanel,legendPanel;
    JLabel minValue, maxValue;
    DecimalFormat f;
    MapAttr at ;
    MapData data; 
    JFrame legFrame;
    String legendName = null;
    
	public ColorStretcherStageView(ColorStretcherStageSettings settings){
		this.settings = settings;
		this.stagePanel = createUI();
		this.settings.addPropertyChangeListener(this);
	}
	public void closeLegendFrame() {
		if (this.legFrame != null) {
			this.legFrame.dispose();
			legFrame = null;
		}
	}
	public ColorMapper getColorMapper() {
		return this.fcm;
	}
	public StageSettings getSettings() {
		return settings;
	}

	public JPanel getStagePanel() {
		return stagePanel;
	}

	private JPanel createUI(){
		fcm = new FancyColorMapper(this.settings);
		fcm.setState(settings.getColorMapperState());
		fcm.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				if (!fcm.isAdjusting()){
					updateColorMapperState();
				}
			}
		});
		
		Border blackBorder = BorderFactory.createLineBorder(Color.BLACK);
		
		//set up outter JPanel with the layout  to add the fcm to
		slim = new JPanel(new BorderLayout());
		//create a panel that has the fancy color mapper with the min and max values
		mapperPanel = new JPanel(new BorderLayout());
		
		//only set up the min / max details in the panel IF the map is numberic
		if (settings.getMapSource()!=null && settings.getMapSource().hasNumericKeyword()){
			//set format to be shown in the panel
			f = new DecimalFormat("##.00");
			
			//set vales for the JLabel
			String min = f.format(this.settings.getMinValue());
			String max = f.format(this.settings.getMaxValue());
			
			//create min and max JLabels
			minValue = new JLabel(min+" ");
			maxValue = new JLabel(max+" ");
			
			//add the JLabels to the panel to be shown to the user 
			mapperPanel.add(minValue, BorderLayout.WEST);
			mapperPanel.add(maxValue, BorderLayout.EAST);
		}
		//add the actual fancy color mapper to the main info panel
		mapperPanel.add(fcm, BorderLayout.CENTER);
		//add mapperpanel to the larger jpanel 
		slim.add(mapperPanel, BorderLayout.NORTH);

		//create the panel to store the legend options which is for when the user wants to
		//have a pop out of the fcm instead of in the info panel, the stageCombo box will possibly 
		//be added back in when the criteria are further defined
		
		legendPanel = new JPanel(new FlowLayout());
		final JButton legendButton = new JButton("Pop out Stretcher".toUpperCase());
		
	//	JLabel elevationSourceLabel = new JLabel("Numeric source: ");
	//	legendPanel.add(elevationSourceLabel);
	//	buildStageComboBox();
	//	legendPanel.add(this.stageSelection);
	
		final JButton refreshButton = new JButton("Refresh".toUpperCase());
		refreshButton.addActionListener(new ActionListener() {
			
			//set up the refresh button actions
			public void actionPerformed(ActionEvent e) {
	//			String selectedName = (String) stageSelection.getSelectedItem();
	//			legendPanel.remove(stageSelection);
	//			buildStageComboBox();
	//			legendPanel.add(stageSelection, BorderLayout.WEST);
	//			stageSelection.setSelectedItem(selectedName);
				
				//update the getters with updated values that was passed along from the GSS
				ColorStretcherStageView.this.fcm.getColorScale().setColorSettingsMax(ColorStretcherStageView.this.settings.getMaxValue());
				ColorStretcherStageView.this.fcm.getColorScale().setColorSettingsMin(ColorStretcherStageView.this.settings.getMinValue());
				ColorStretcherStageView.this.fcm.setColorSettingsMax(ColorStretcherStageView.this.settings.getMaxValue());
				ColorStretcherStageView.this.fcm.setColorSettingsMin(ColorStretcherStageView.this.settings.getMinValue());
				//only update the values if the map is numeric 
				if (settings.getMapSource()!=null && settings.getMapSource().hasNumericKeyword()){			
					minValue.setText(String.valueOf(f.format(ColorStretcherStageView.this.settings.getMinValue())));
					maxValue.setText(String.valueOf(f.format(ColorStretcherStageView.this.settings.getMaxValue())));			
				}
				stagePanel.revalidate();	
			}
		});
		
		//add a listener for the pop out color legend
		legendButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				legendFrame();				
			}
		});
		
		//add the refresh and the pop out legend button to the JPanel
		legendPanel.add(refreshButton, BorderLayout.CENTER);
		legendPanel.add(legendButton, BorderLayout.EAST);
		
		slim.add(legendPanel, BorderLayout.CENTER);
		this.settings.setColorMapper(this.fcm);

		return slim;
	}
	
	private void legendFrame(){
		//create the pop out window with the fcm from the info window	
		// always match the focus panel title with the pop up window

		//find the parent frame for this stagePanel
		Component parent = stagePanel.getParent();
		while (parent != null && !(parent instanceof JFrame)) {
			parent = parent.getParent();
		}
		if (parent != null) {
			String focusTitle = ((JFrame)parent).getTitle();
			if (focusTitle != null){
				int indexOptions = focusTitle.lastIndexOf(FocusPanel.FOCUS_TITLE);
				if (indexOptions != -1){
					legendName = focusTitle.substring(0, indexOptions);
					legendName = legendName.trim() + " Legend";
				} 
			} 
		}
		if (legendName == null) {
			legendName = "Map Legend" ;
		}
		legFrame = new JFrame(legendName);
		JPanel legendPanelSetup = new JPanel(new BorderLayout());
		JPanel legPan = new JPanel(new BorderLayout());
		
		//hide the auto, paste, copy etc from the legend window in case the user 
		// doesn't want to see them 
		final JCheckBox hide = new JCheckBox("Hide Buttons");
		hide.setSelected(false);
		hide.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (hide.isSelected()){
					fcm.togglePanel(false);
				}else{
					fcm.togglePanel(true);
					hide.setSelected(false);
				}	
			}
		});
		//set the labels in the new pop out window for max and min values
		if (settings.getMapSource()!=null && settings.getMapSource().hasNumericKeyword()){
			legPan.add(new JLabel(f.format(this.settings.getMaxValue())+" "), BorderLayout.EAST);
			legPan.add(new JLabel(f.format(this.settings.getMinValue())+" "), BorderLayout.WEST);
		}
		//add necessary JPanel details for the layout 
		legPan.add(hide,BorderLayout.SOUTH);
		legPan.add(fcm, BorderLayout.CENTER);
		legendPanelSetup.add(legPan, BorderLayout.NORTH);
		legFrame.setSize(500, 150);
		legFrame.setVisible(true);
		legFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		//a window listener was added to make sure the toggle was turned back off which 
		//hides buttons as well as adding the fcm back to the info window
		legFrame.addWindowListener((new WindowListener() {
			
			@Override
			public void windowOpened(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void windowIconified(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void windowDeiconified(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void windowDeactivated(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void windowClosing(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void windowClosed(WindowEvent e) {
				slim.setVisible(true);
				mapperPanel.add(fcm);
				fcm.togglePanel(true);
			}
			
			@Override
			public void windowActivated(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
		}));
		
		legFrame.add(legPan);
		slim.setVisible(false);
		
		
		/*
		the following code is for the stageSelection drop down list. For now
		the code is being commented out but will possibly be used in the future 
		when we define the criteria for being able to select different stage levels
     	*/	
//	}

//	private void buildStageComboBox() {
//		ArrayList<String> stageNameList = new ArrayList<>();
//		Stage[] stageList = this.settings.getStages();
//		stageNameList.add("Map Source");
//		for (int x=0; x<stageList.length; x++) {
//			if (stageList[x].produces() == MapAttr.SINGLE_BAND) {;
//				stageNameList.add(stageList[x].getStageName());
//				System.out.println("first one:" + stageList[x]);
//			
//			}
//			if (stageList[x] instanceof ColorStretcherStage) {
//				ColorStretcherStage cStage = (ColorStretcherStage) stageList[x];
//				ColorStretcherStageSettings csSettings = (ColorStretcherStageSettings) cStage.getSettings();
//				System.out.println("second one: " + stageList[x]);
//			}
//		}
//		String[] stageNameListSt = stageNameList.toArray(new String[stageNameList.size()]);
//		this.stageSelection = new JComboBox(stageNameListSt);
//		this.stageSelection.addActionListener(new ActionListener() {
//			
//			public void actionPerformed(ActionEvent e) {
//				//do something and then implement an item listener on this for when a new stage is added
//				int selectedIndex = stageSelection.getSelectedIndex();
//				System.out.println("in actionPerformed, StageIndex: " + selectedIndex);
//				System.out.println("item name: " + stageSelection.getSelectedItem());
//				
//			}
//		});
//		this.stageSelection.repaint();
	}
	
	private boolean updatingState = false;
	private void updateColorMapperState(){
		if (updatingState)
			return;
		
		updatingState = true;
		try {
			settings.setColorMapperState(fcm.getState());
		}
		finally {
			updatingState = false;
		}
	}

	public void propertyChange(PropertyChangeEvent e) {
		if (updatingState)
			return;
		
		updatingState = true;
		try {
			if (ColorStretcherStageSettings.propCmap.equals(e.getPropertyName())){
				fcm.setState((ColorMapper.State)e.getNewValue());
			}
		}
		finally {
			updatingState = false;
		}
	}
}
