package edu.asu.jmars.layer.map2.stages;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.asu.jmars.layer.map2.StageSettings;
import edu.asu.jmars.layer.map2.StageView;

public class DummyStageView implements StageView, PropertyChangeListener {
	StageSettings settings;
	JPanel stagePanel;
	
	public DummyStageView(StageSettings settings){
		this.settings = settings;
		this.stagePanel = createUI();
		this.settings.addPropertyChangeListener(this);
	}
	
	public StageSettings getSettings() {
		return settings;
	}
	
	private JPanel createUI(){
		JPanel p = new JPanel(new BorderLayout());
		p.add(new JLabel());
		return p;
	}

	public JPanel getStagePanel() {
		return stagePanel;
	}

	public void propertyChange(PropertyChangeEvent e){
	}
}
