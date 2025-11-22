package edu.asu.jmars.samples.layer.map2.stages.threshold;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.ParseException;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.asu.jmars.layer.map2.StageSettings;
import edu.asu.jmars.layer.map2.StageView;

public class ThresholdStageView implements StageView, PropertyChangeListener {
	ThresholdSettings settings;
	JPanel stagePanel;
	JTextField thresholdValField;
	DecimalFormat nf = new DecimalFormat("###0.########");
	
	public ThresholdStageView(ThresholdSettings settings){
		this.settings = settings;
		stagePanel = buildUI();
		settings.addPropertyChangeListener(this);
	}
	
	public StageSettings getSettings() {
		return settings;
	}

	public JPanel getStagePanel() {
		return stagePanel;
	}

	private JPanel buildUI(){
		thresholdValField = new JTextField(5);
		updateThresholdFieldFromSettings();
		thresholdValField.setFocusable(true);
		thresholdValField.addFocusListener(new FocusAdapter(){
			public void focusLost(FocusEvent e){
				updateSettingsFromThresholdField();
			}
		});
		thresholdValField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				updateSettingsFromThresholdField();
			}
		});
		
		Box baseStepBox = Box.createHorizontalBox();
		baseStepBox.add(new JLabel("Threshold:", JLabel.RIGHT));
		baseStepBox.add(thresholdValField);
		baseStepBox.add(Box.createHorizontalStrut(5));
		
		JPanel slim = new JPanel(new BorderLayout());
		slim.add(baseStepBox, BorderLayout.NORTH);
		
		return slim;
	}
	
	private void updateThresholdFieldFromSettings(){
		setFieldValue(thresholdValField, settings.getThreshold());
	}
	
	private void updateSettingsFromThresholdField(){
		try {
			settings.setThreshold(getFieldValue(thresholdValField));
		}
		catch(ParseException ex){
			thresholdValField.selectAll();
			thresholdValField.requestFocus();
		}
	}

	private void setFieldValue(JTextField textField, double val){
		textField.setText(nf.format(val));
	}
	
	private double getFieldValue(JTextField textField) throws ParseException {
		String text = textField.getText();
		return nf.parse(text).doubleValue();
	}

	public void propertyChange(final PropertyChangeEvent e) {
		final String prop = e.getPropertyName();
		
		if (prop.equals(ThresholdSettings.propThresholdValue))
			updateThresholdFieldFromSettings();
	}

}
