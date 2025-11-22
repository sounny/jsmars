package edu.asu.jmars.layer.map2.stages;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.ParseException;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import edu.asu.jmars.layer.map2.StageSettings;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.layer.map2.stages.ThresholdSettings.ThresholdMode;

public class ThresholdStageView implements StageView, PropertyChangeListener {
	ThresholdSettings settings;
	JPanel stagePanel;
	JTextField thresholdValField;
	JRadioButton rbModeGE, rbModeGT, rbModeLE, rbModeLT, rbModeEQ, rbModeNE;
	ButtonGroup rbGroup;
	JCheckBox cbBinary;
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

		
		ActionListener modeActionListener = new ActionListener(){
			public void actionPerformed(ActionEvent e){
				updateSettingsFromModeGroup();
			}
		};
		
		rbGroup = new ButtonGroup();
		rbModeGE = new JRadioButton(">=");
		rbModeGE.addActionListener(modeActionListener);
		rbGroup.add(rbModeGE);
		rbModeGT = new JRadioButton(">");
		rbModeGT.addActionListener(modeActionListener);
		rbGroup.add(rbModeGT);
		rbModeLE = new JRadioButton("<=");
		rbModeLE.addActionListener(modeActionListener);
		rbGroup.add(rbModeLE);
		rbModeLT = new JRadioButton("<");
		rbModeLT.addActionListener(modeActionListener);
		rbGroup.add(rbModeLT);
		rbModeEQ = new JRadioButton("=");
		rbModeEQ.addActionListener(modeActionListener);
		rbGroup.add(rbModeEQ);
		rbModeNE = new JRadioButton("<>");
		rbModeNE.addActionListener(modeActionListener);
		rbGroup.add(rbModeNE);
		
		JPanel modeBox = new JPanel(new GridLayout(3,2));
		modeBox.add(rbModeGE);
		modeBox.add(rbModeGT);
		modeBox.add(rbModeLE);
		modeBox.add(rbModeLT);
		modeBox.add(rbModeEQ);
		modeBox.add(rbModeNE);
		
		updateModeGroupFromSettings();
		
		cbBinary = new JCheckBox("Binary Output");
		updateBinOutputCheckBoxFromSettings();
		cbBinary.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				updateBinOutputFromCheckBox();
			}
		});

		Box baseStepBox = Box.createHorizontalBox();
		baseStepBox.add(Box.createHorizontalGlue());
		baseStepBox.add(Box.createHorizontalStrut(5));
		baseStepBox.add(modeBox);
		JPanel valFieldBox = new JPanel(new GridLayout(3,1));
		valFieldBox.add(new JLabel("Value"));
		valFieldBox.add(thresholdValField);
		valFieldBox.add(Box.createVerticalGlue());
		baseStepBox.add(valFieldBox);
		baseStepBox.add(Box.createHorizontalStrut(20));
		baseStepBox.add(cbBinary);
		baseStepBox.add(Box.createHorizontalGlue());
		
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
	
	private void updateModeGroupFromSettings(){
		switch(settings.getMode()){
		case MODE_GE:
			rbModeGE.setSelected(true);
			break;
		case MODE_GT:
			rbModeGT.setSelected(true);
			break;
		case MODE_LE:
			rbModeLE.setSelected(true);
			break;
		case MODE_LT:
			rbModeLT.setSelected(true);
			break;
		case MODE_EQ:
			rbModeEQ.setSelected(true);
			break;
		case MODE_NE:
			rbModeNE.setSelected(true);
			break;
		}
	}
	
	private ThresholdMode getModeFromModeGroup(){
		if (rbModeGE.isSelected())
			return ThresholdMode.MODE_GE;
		else if (rbModeGT.isSelected())
			return ThresholdMode.MODE_GT;
		else if (rbModeLE.isSelected())
			return ThresholdMode.MODE_LE;
		else if (rbModeLT.isSelected())
			return ThresholdMode.MODE_LT;
		else if (rbModeEQ.isSelected())
			return ThresholdMode.MODE_EQ;
		else if (rbModeNE.isSelected())
			return ThresholdMode.MODE_NE;
		return null;
	}
	
	private void updateSettingsFromModeGroup(){
		ThresholdMode mode = getModeFromModeGroup();
		if (settings.getMode() != mode)
			settings.setMode(mode);
	}

	private void updateBinOutputCheckBoxFromSettings(){
		cbBinary.setSelected(settings.getBinaryOutput());
	}
	
	private void updateBinOutputFromCheckBox(){
		settings.setBinaryOutput(cbBinary.isSelected());
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
		else if (prop.equals(ThresholdSettings.propModeValue))
			updateModeGroupFromSettings();
		else if (prop.equals(ThresholdSettings.propBinOutputValue))
			updateBinOutputCheckBoxFromSettings();
	}

}
