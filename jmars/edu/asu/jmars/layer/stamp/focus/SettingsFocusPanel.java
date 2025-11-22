package edu.asu.jmars.layer.stamp.focus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.StampLayerSettings;
import edu.asu.jmars.layer.stamp.radar.HorizonColorDisplayPanel;
import edu.asu.jmars.swing.ColorCombo;
import edu.asu.jmars.viz3d.ThreeDManager;

public class SettingsFocusPanel extends JPanel {
    
	public SettingsFocusPanel(final StampLView stampLView)
	{
		JLabel borderColorLbl = new JLabel("Outline color:");
		final ColorCombo borderColor = new ColorCombo();
		//we will get the settings from the LView for the first time
		StampLayerSettings settings = stampLView.getSettings();
		borderColor.setColor(settings.getUnselectedStampColor());
		final ColorCombo fillColor = new ColorCombo();
		JLabel fillColorLbl = new JLabel("Fill color:");
		fillColor.setColor(new Color(settings.getFilledStampColor().getRGB() & 0xFFFFFF, false));
		
		ActionListener fillColorListener = new ActionListener() {
          	public void actionPerformed(ActionEvent e) {
          		//in order to make sure that the LView and the focus panel are using the same settings object, we will get the 
          		//settings object out of the LView again. Making the above settings final and using them here does not work
          		StampLayerSettings layerSettings = stampLView.getSettings();
          		//border color
          		layerSettings.setUnselectedStampColor(borderColor.getColor());
           		//end border color
           		//fill color
           		int alpha = layerSettings.getFilledStampColor().getAlpha();
				layerSettings.setFilledStampColor(
					new Color( (alpha<<24) | (fillColor.getColor().getRGB() & 0xFFFFFF), true));
				
				if (!layerSettings.hideOutlines() || alpha != 0) {
					//if nothing is being shown, don't redraw, otherwise, redraw everything
					stampLView.redrawEverything(true);
				}

				if (ThreeDManager.isReady()) {
					//update the 3d view if has lview3d enabled
		    		LView3D view3d = stampLView.getLView3D();
		    		if(view3d.isEnabled()){
		    			ThreeDManager mgr = ThreeDManager.getInstance();
		    			//	If the 3d is already visible, update it
		    			if(view3d.isVisible()){
		    				mgr.updateDecalsForLView(stampLView, true);
		    			}
		    		}
				}
          	}
        };
		borderColor.addActionListener(fillColorListener);
		fillColor.addActionListener(fillColorListener);
		
		JLabel alphaLbl = new JLabel("Fill alpha:");
		final JSlider alpha = new JSlider(0, 255, 0);
		alpha.setValue(settings.getFilledStampColor().getAlpha());
	    alpha.addChangeListener(new ChangeListener() {
	    	public void stateChanged(ChangeEvent e) {
	    		if (alpha.getValueIsAdjusting()) {
	    			return;
	    		}
	    		//in order to make sure that the LView and the focus panel are using the same settings object, we will get the 
          		//settings object out of the LView instead of using the final instance that is passed in
          		StampLayerSettings layerSettings = stampLView.getSettings();
	    		int alphaVal = alpha.getValue();
	    		int color = layerSettings.getFilledStampColor().getRGB() & 0xFFFFFF;
	    		layerSettings.setFilledStampColor(new Color((alphaVal<<24) | color, true));
	    		layerSettings.filledAlphaVal=alphaVal;
	    		stampLView.stampLayer.increaseStateId(0);
	    		stampLView.redrawEverything(true);
	    		
	    		if (ThreeDManager.isReady()) {
					//update the 3d view if has lview3d enabled
		    		LView3D view3d = stampLView.getLView3D();
		    		if(view3d.isEnabled()){
		    			ThreeDManager mgr = ThreeDManager.getInstance();
		    			//	If the 3d is already visible, update it
		    			if(view3d.isVisible()){
		    				mgr.updateDecalsForLView(stampLView, true);
		    			}
		    		}
				}
	    	}
	    });
					    
	    // Wind Vector related
		JLabel magnitudeLabel = new JLabel("Magnitude Scale Factor:");
		final JTextField magnitudeField = new JTextField(""+settings.getMagnitude());
		magnitudeField.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				//in order to make sure that the LView and the focus panel are using the same settings object, we will get the 
          		//settings object out of the LView instead of using the final instance that is passed in
          		StampLayerSettings layerSettings = stampLView.getSettings();
				layerSettings.setMagnitude(Double.parseDouble(magnitudeField.getText()));
           		StampLView child = (StampLView) stampLView.getChild();

       			stampLView.drawOutlines();
      			child.drawOutlines();
			}
		});

		JLabel originColorLbl = new JLabel("Origin color:");
		final ColorCombo originColor = new ColorCombo();
		originColor.setColor(settings.getOriginColor());
		originColor.addActionListener(new ActionListener() {
          	public void actionPerformed(ActionEvent e) {
          	//in order to make sure that the LView and the focus panel are using the same settings object, we will get the 
          		//settings object out of the LView instead of using the final instance that is passed in
          		StampLayerSettings layerSettings = stampLView.getSettings();		
           		layerSettings.setOriginColor(originColor.getColor());
           		StampLView child = (StampLView) stampLView.getChild();

       			stampLView.drawOutlines();
      			child.drawOutlines();
          	}
        });

		JLabel originMagnitudeLabel = new JLabel("Origin Scale Factor:");
		final JTextField originMagnitudeField = new JTextField(""+settings.getOriginMagnitude());
		originMagnitudeField.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent e) {
				//in order to make sure that the LView and the focus panel are using the same settings object, we will get the 
          		//settings object out of the LView instead of using the final instance that is passed in
          		StampLayerSettings layerSettings = stampLView.getSettings();
				layerSettings.setOriginMagnitude(Double.parseDouble(originMagnitudeField.getText()));
           		StampLView child = (StampLView) stampLView.getChild();
       			stampLView.drawOutlines();
      			child.drawOutlines();
			}
		});	    
	    
		final JCheckBox mosaicCheckBox = new JCheckBox("Enable Mosaic-Select (resource intensive)");
		mosaicCheckBox.setToolTipText("<html>When enabled, drag-selection will NOT select any outlines that are completely covered.<p>"+
		    "This can be used to exclude any observations that would not be visible when creating a mosaic.<p>"+
		    "This process can be very slow and resource intensive.<p><b>It is recommended to only be used when necessary.</b></html>");

		mosaicCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				StampLayerSettings layerSettings = stampLView.getSettings();
				layerSettings.selectTopStampsOnly = mosaicCheckBox.isSelected();
			}
		});
		
		final JCheckBox boresightToggle = new JCheckBox("Display Boresight", false);
		boresightToggle.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
          		StampLayerSettings layerSettings = stampLView.getSettings();
				layerSettings.setShowBoresight(boresightToggle.isSelected());
           		StampLView child = (StampLView) stampLView.getChild();
           		stampLView.stampLayer.increaseStateId(StampLayer.OUTLINES_BUFFER);
           		stampLView.stampLayer.increaseStateId(StampLayer.SELECTIONS_BUFFER);
       			stampLView.drawOutlines();
      			child.drawOutlines();
      			
   				//update the 3d view if has lview3d enabled
				LView3D view3d = stampLView.getLView3D();
				if(view3d.isEnabled()){
					ThreeDManager mgr = ThreeDManager.getInstance();
					//If the 3d is already visible, update it
					if(mgr.getFrame().isVisible() && view3d.isVisible()){
						mgr.updateDecalsForLView(stampLView, true);
					}
				}
			}
		});
			
		final JCheckBox drawAsRingToggle = new JCheckBox("Draw Spots as Rings (do not fill)", false);
		drawAsRingToggle.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
          		StampLayerSettings layerSettings = stampLView.getSettings();
				layerSettings.setDrawAsRing(drawAsRingToggle.isSelected());
           		StampLView child = (StampLView) stampLView.getChild();
           		stampLView.stampLayer.increaseStateId(StampLayer.OUTLINES_BUFFER);
           		stampLView.stampLayer.increaseStateId(StampLayer.SELECTIONS_BUFFER);
       			stampLView.drawOutlines();
      			child.drawOutlines();
      			
   				//update the 3d view if has lview3d enabled
				LView3D view3d = stampLView.getLView3D();
				if(view3d.isEnabled()){
					ThreeDManager mgr = ThreeDManager.getInstance();
					//If the 3d is already visible, update it
					if(mgr.getFrame().isVisible() && view3d.isVisible()){
						mgr.updateDecalsForLView(stampLView, true);
					}
				}
			}
		});		

		// Ring line width combo box
		Float[] widthChoices = { 0.25f,0.5f,1.0f,2.0f,3.0f,4.0f,5.0f};
		
		final JComboBox<Float> ringWidthCombo = new JComboBox<Float>(widthChoices);
		ringWidthCombo.setSelectedItem(settings.getRingWidth());
		ringWidthCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
          		StampLayerSettings layerSettings = stampLView.getSettings();
				layerSettings.setRingWidth((Float)ringWidthCombo.getSelectedItem());
           		StampLView child = (StampLView) stampLView.getChild();
           		stampLView.stampLayer.increaseStateId(StampLayer.OUTLINES_BUFFER);
           		stampLView.stampLayer.increaseStateId(StampLayer.SELECTIONS_BUFFER);
       			stampLView.drawOutlines();
      			child.drawOutlines();
      			
   				//update the 3d view if has lview3d enabled
				LView3D view3d = stampLView.getLView3D();
				if(view3d.isEnabled()){
					ThreeDManager mgr = ThreeDManager.getInstance();
					//If the 3d is already visible, update it
					if(mgr.getFrame().isVisible() && view3d.isVisible()){
						mgr.updateDecalsForLView(stampLView, true);
					}
				}
			}
		});		

	    JPanel settingsP = new JPanel();
	    settingsP.setLayout(new GridBagLayout());
	    settingsP.setBorder(new EmptyBorder(4,4,4,4));
	    int row = 0;
	    int pad = 4;
	    Insets in = new Insets(pad,pad,pad,pad);
	    
	    StampLayer stampLayer = stampLView.stampLayer;
	    
	    if (stampLayer.vectorShapes()) {  // Wind vectors
		    settingsP.add(borderColorLbl, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    settingsP.add(borderColor, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    row++;
			settingsP.add(magnitudeLabel, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			settingsP.add(magnitudeField, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    row++;
		    settingsP.add(originColorLbl, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    settingsP.add(originColor, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    row++;
		    settingsP.add(originMagnitudeLabel, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    settingsP.add(originMagnitudeField, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));				    	
		    row++;
	    } else if (stampLayer.pointShapes()) {  // MOLA Shots
//		    settingsP.add(originColorLbl, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
//		    settingsP.add(originColor, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
//		    row++;
//		    settingsP.add(originMagnitudeLabel, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
//		    settingsP.add(originMagnitudeField, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));				    		    	
	    } else if (stampLayer.lineShapes()){ //Radar (SHARAD)
	    	JLabel colorLbl = new JLabel("Default Radar Horizon Color:");
	    	JLabel fullResLbl = new JLabel("Default Full Resolution Horizon Width:");
	    	JLabel browseLbl = new JLabel("Default Browse Image Horizon Width:");
	    	JLabel lviewLbl = new JLabel("Default LView Horizon Width:");
	    	final ColorCombo horizonColor = new ColorCombo(settings.getHorizonColor());
	    	horizonColor.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					StampLayerSettings settings = stampLView.getSettings();
					settings.setHorizonColor(horizonColor.getColor());
				}
			});
	    	Integer[] widthOptions = {1,2,3,4,5,6,7,8};
	    	final JComboBox<Integer> fullResBx = new JComboBox<Integer>(widthOptions);
	    	fullResBx.setSelectedItem(settings.getFullResWidth());
	    	fullResBx.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					StampLayerSettings settings = stampLView.getSettings();
					settings.setFullResWidth((int)fullResBx.getSelectedItem());
				}
			});
	    	final JComboBox<Integer> browseBx = new JComboBox<Integer>(widthOptions);
	    	browseBx.setSelectedItem(settings.getBrowseWidth());
	    	browseBx.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					StampLayerSettings settings = stampLView.getSettings();
					settings.setBrowseWidth((int)browseBx.getSelectedItem());
				}
			});
	    	final JComboBox<Integer> lviewBx = new JComboBox<Integer>(widthOptions);
	    	lviewBx.setSelectedItem(settings.getLViewWidth());
	    	lviewBx.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					StampLayerSettings settings = stampLView.getSettings();
					settings.setLViewWidth((int)lviewBx.getSelectedItem());
				}
			});
	    	
	    	
	    	settingsP.add(colorLbl, new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	settingsP.add(horizonColor, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	settingsP.add(fullResLbl, new GridBagConstraints(0, ++row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	settingsP.add(fullResBx, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	settingsP.add(browseLbl, new GridBagConstraints(0, ++row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	settingsP.add(browseBx, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	settingsP.add(lviewLbl, new GridBagConstraints(0, ++row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	settingsP.add(lviewBx, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
		    settingsP.add(new JLabel("Footprint Color:"), new GridBagConstraints(0,++row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    settingsP.add(borderColor, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
	    	settingsP.add(new HorizonColorDisplayPanel(stampLView), new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		    
	    } else if (stampLayer.spectraData()) {
	    	alpha.setValue(255);
	    	
			settingsP.add(alphaLbl, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			settingsP.add(alpha, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			row++;
			settingsP.add(drawAsRingToggle, new GridBagConstraints(1,row,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			row++;
			
			settingsP.add(new JLabel("Ring thickness:"), new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			settingsP.add(ringWidthCombo, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			row++;
			
		    if (stampLView.stampLayer.getParam(StampLayer.INCLUDES_BORESIGHT).equalsIgnoreCase("true")) {
			    settingsP.add(boresightToggle, new GridBagConstraints(1,row,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			    row++;
		    }
		    
		    settingsP.add(new JLabel(),new GridBagConstraints(0,row,2,1,1,1,GridBagConstraints.WEST,GridBagConstraints.BOTH,in,pad,pad));
	    } else {  // All other stamp layers
		    settingsP.add(borderColorLbl, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    settingsP.add(borderColor, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    row++;
		    
		    if (!stampLayer.lineShapes()) {
			    settingsP.add(fillColorLbl, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			    settingsP.add(fillColor, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			    row++;
			    settingsP.add(alphaLbl, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			    settingsP.add(alpha, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			    row++;
		    }		                
		    		    
		    settingsP.add(mosaicCheckBox, new GridBagConstraints(0,row,2,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    row++;

		    if (stampLView.stampLayer.getParam(StampLayer.INCLUDES_BORESIGHT).equalsIgnoreCase("true")) {
			    settingsP.add(boresightToggle, new GridBagConstraints(1,row,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
			    row++;
		    }
		    settingsP.add(new JLabel(),new GridBagConstraints(0,row,2,1,1,1,GridBagConstraints.WEST,GridBagConstraints.BOTH,in,pad,pad));
		} 
	    
	    JScrollPane displayPane = new JScrollPane(settingsP);
	    	    
	    setLayout(new BorderLayout());
	    add(displayPane, BorderLayout.CENTER);
	}
}
