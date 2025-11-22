package edu.asu.jmars.layer.map2.stages;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.ParseException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;

import edu.asu.jmars.layer.map2.StageSettings;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.util.DebugLog;

public class ShadeStageView implements StageView, PropertyChangeListener {
	private static final DebugLog log = DebugLog.instance();
	private static final DecimalFormat nf = new DecimalFormat("###0.########");
	
	private ShadeStageSettings settings;
	private JTextField azField;
	private JTextField elField;
	private JPanel stagePanel;
	
	public ShadeStageView(ShadeStageSettings settings){
		this.settings = settings;
		stagePanel = buildUI();
		this.settings.addPropertyChangeListener(this);
	}
	
	private JPanel buildUI(){
		azField = new JTextField(6);
		azField.setFocusable(true);
		updateAzFieldFromSettings();
		azField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateAzFromField();
			}
		});
		
		elField = new JTextField(6);
		elField.setFocusable(true);
		updateElFieldFromSettings();
		elField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				updateElFromField();
			}
		});
		
		JLabel azLbl = new JLabel("Azimuth:");
		JLabel elLbl = new JLabel("Elevation:");
		
		JPanel out = new JPanel(new GridLayout(2, 2));
		out.add(azLbl);
		out.add(azField);
		out.add(elLbl);
		out.add(elField);
		
		return out;
	}
	
	private void updateAzFromField(){
		try {
			settings.setAz(nf.parse(azField.getText()).doubleValue());
		}
		catch(ParseException ex){
			log.println(ex);
			azField.selectAll();
			azField.requestFocusInWindow();
		}
	}
	
	private void updateElFromField(){
		try {
			settings.setEl(nf.parse(elField.getText()).doubleValue());
		}
		catch(ParseException ex){
			log.println(ex);
			elField.selectAll();
			elField.requestFocusInWindow();
		}
	}
	
	private void updateAzFieldFromSettings(){
		azField.setText(nf.format(settings.getAz()));
		azField.setCaretPosition(0);
	}
	
	private void updateElFieldFromSettings(){
		elField.setText(nf.format(settings.getEl()));
		elField.setCaretPosition(0);
	}
	
	public void propertyChange(PropertyChangeEvent e) {
		final String prop = e.getPropertyName();
		if (prop.equals(ShadeStageSettings.propAz)){
			updateAzFieldFromSettings();
		}
		else if (prop.equals(ShadeStageSettings.propEl)){
			updateElFieldFromSettings();
		}
	}

	public JPanel getStagePanel() {
		return stagePanel;
	}

	public StageSettings getSettings() {
		return settings;
	}

}
