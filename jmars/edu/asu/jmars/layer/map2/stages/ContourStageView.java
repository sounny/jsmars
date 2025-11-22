package edu.asu.jmars.layer.map2.stages;

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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.asu.jmars.layer.map2.StageSettings;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.swing.ColorCombo;

public class ContourStageView implements StageView, PropertyChangeListener {
	ContourStageSettings settings;
	JPanel stagePanel;
	JTextField baseValField, stepValField;
	ColorCombo colorField;
	JComboBox thicknessField;
	
	DecimalFormat nf = new DecimalFormat("###0.########");
	
	public ContourStageView(ContourStageSettings settings){
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
		baseValField = new JTextField(5);
		updateBaseFieldFromSettings();
		baseValField.setFocusable(true);
		baseValField.addFocusListener(new FocusAdapter(){
			public void focusLost(FocusEvent e){
				updateSettingsFromBaseField();
			}
		});
		baseValField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				updateSettingsFromBaseField();
			}
		});
		
		stepValField = new JTextField(5);
		updateStepFieldFromSettings();
		stepValField.setFocusable(true);
		stepValField.addFocusListener(new FocusAdapter(){
			public void focusLost(FocusEvent e){
				updateSettingsFromStepField();
			}
		});
		stepValField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				updateSettingsFromStepField();
			}
		});
		
		colorField = new ColorCombo();
		colorField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				updateSettingsFromColorField();
			}
		});
		updateColorFieldFromSettings();

		Object lineChoices[] = {1,2,3};
		thicknessField = new JComboBox(lineChoices);
		thicknessField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				updateSettingsFromLineThicknessField();
			}
		});
		updateLineThicknessFromSettings();
		
		Box baseStepBox = Box.createHorizontalBox();
		baseStepBox.add(new JLabel("Base:", JLabel.RIGHT));
		baseStepBox.add(baseValField);
		baseStepBox.add(Box.createHorizontalStrut(5));
		baseStepBox.add(new JLabel("Step:", JLabel.RIGHT));
		baseStepBox.add(stepValField);
		baseStepBox.add(Box.createHorizontalStrut(5));
		baseStepBox.add(new JLabel("Color:", JLabel.RIGHT));
		baseStepBox.add(colorField);
		baseStepBox.add(Box.createHorizontalStrut(5));
		baseStepBox.add(new JLabel("Line Thickness:", JLabel.RIGHT));
		baseStepBox.add(thicknessField);
		
		JPanel slim = new JPanel(new BorderLayout());
		slim.add(baseStepBox, BorderLayout.NORTH);
		
		return slim;
	}
	
	private void updateBaseFieldFromSettings(){
		setFieldValue(baseValField, settings.getBase());
	}
	
	private void updateStepFieldFromSettings(){
		setFieldValue(stepValField, settings.getStep());
	}
	
	private void updateColorFieldFromSettings(){
		colorField.setColor(settings.getColor());
	}

	private void updateLineThicknessFromSettings(){
		thicknessField.setSelectedItem(settings.getLineThickness());
	}

	private void updateSettingsFromBaseField(){
		try {
			settings.setBase(getFieldValue(baseValField));
		}
		catch(ParseException ex){
			baseValField.selectAll();
			baseValField.requestFocus();
		}
	}

	private void updateSettingsFromStepField(){
		try {
			settings.setStep(getFieldValue(stepValField));
		}
		catch(ParseException ex){
			stepValField.selectAll();
			stepValField.requestFocus();
		}
	}
	
	private void updateSettingsFromColorField(){
		if (!settings.getColor().equals(colorField.getColor()))
			settings.setColor(colorField.getColor());
	}
	
	private void updateSettingsFromLineThicknessField() {
		if (settings.getLineThickness()!=(Integer)thicknessField.getSelectedItem())
			settings.setLineThickness((Integer)thicknessField.getSelectedItem());
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
		
		if (prop.equals(ContourStageSettings.propBase))
			updateBaseFieldFromSettings();
		else if (prop.equals(ContourStageSettings.propStep))
			updateStepFieldFromSettings();
		else if (prop.equals(ContourStageSettings.propColor))
			updateColorFieldFromSettings();
		else if (prop.equals(ContourStageSettings.propLineThickness))
			updateLineThicknessFromSettings();
	}

}
