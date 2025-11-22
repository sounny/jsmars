package edu.asu.jmars.layer.map2.stages;

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.asu.jmars.layer.map2.StageSettings;
import edu.asu.jmars.layer.map2.StageView;

public class BandExtractorStageView implements StageView, PropertyChangeListener {
	private BandExtractorStageSettings settings;
	private JPanel stagePanel;
	private JComboBox bandComboBox;
	
	public BandExtractorStageView(BandExtractorStageSettings settings){
		this.settings = settings;
		this.stagePanel = createUI();
		
		settings.addPropertyChangeListener(this);
	}

	public StageSettings getSettings() {
		return settings;
	}

	public JPanel getStagePanel() {
		return stagePanel;
	}
	
	protected JPanel createUI(){
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JLabel("Select band:"), BorderLayout.WEST);
		bandComboBox = new JComboBox(settings.getBands());
		panel.add(bandComboBox, BorderLayout.CENTER);
		bandComboBox.setSelectedItem(settings.getSelectedBand());
		bandComboBox.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					updateBandSelection();
			}
			
		});
		return panel;
	}
	
	protected synchronized void updateBandSelection(){
		String selectedBand = (String)bandComboBox.getSelectedItem();
		settings.setSelectedBand(selectedBand);
	}

	public void propertyChange(PropertyChangeEvent e) {
		if (BandExtractorStageSettings.propBand.equals(e.getPropertyName()))
			bandComboBox.setSelectedItem(e.getNewValue());
	}
}
