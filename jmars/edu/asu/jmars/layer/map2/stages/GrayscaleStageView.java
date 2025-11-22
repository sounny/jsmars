package edu.asu.jmars.layer.map2.stages;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.ParseException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.asu.jmars.layer.map2.StageSettings;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.util.DebugLog;

public class GrayscaleStageView implements StageView, PropertyChangeListener {
	private static final DebugLog log = DebugLog.instance();
	private static final String VAL_UNKNOWN = "unknown";
	private static final DecimalFormat nf = new DecimalFormat("###0.########");
	private static final int gap = 4;
	
	private GrayscaleStageSettings settings;
	private JTextField minValField;
	private JTextField maxValField;
	private JCheckBox autoMinMaxCheckBox;
	private JButton resetButton;
	private JPanel stagePanel;
	private boolean minValueFlag = false;
	private boolean maxValueFlag = false;
	
	
	public boolean isMinValueFlag() {
		return minValueFlag;
	}

	public void setMinValueFlag(boolean minValueFlag) {
		this.minValueFlag = minValueFlag;
	}

	public boolean isMaxValueFlag() {
		return maxValueFlag;
	}

	public void setMaxValueFlag(boolean maxValueFlag) {
		this.maxValueFlag = maxValueFlag;
	}

	public GrayscaleStageView(GrayscaleStageSettings settings){
		this.settings = settings;
		stagePanel = buildUI();
		settings.addPropertyChangeListener(this);
	}
	
	private void updateMinValFromField(){
		try {
			settings.setMinValue(getFieldValue(minValField, VAL_UNKNOWN, Double.POSITIVE_INFINITY));
			this.minValueFlag = true;
		} catch(ParseException ex){
			log.println(ex);
			minValField.selectAll();
			minValField.requestFocusInWindow();
		}
	}
	
	private void updateMaxValFromField(){
		try {
			settings.setMaxValue(getFieldValue(maxValField, VAL_UNKNOWN, Double.NEGATIVE_INFINITY));
			this.maxValueFlag = true;
		}
		catch(ParseException ex){
			log.println(ex);
			maxValField.selectAll();
			maxValField.requestFocusInWindow();
		}
	}
	
	private void updateAutoMinMaxFromCheckBox(){
		settings.setAutoMinMax(autoMinMaxCheckBox.isSelected());
	}
	
	private JPanel buildUI() {
		minValField = new JTextField(6);
		minValField.setFocusable(true);
		updateMinFieldFromSettings();
		minValField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				updateMinValFromField();
			}
		});
		minValField.addFocusListener(new FocusAdapter(){
			public void focusLost(FocusEvent e) {
				updateMinValFromField();
			}
		});
		
		maxValField = new JTextField(6);
		maxValField.setFocusable(true);
		updateMaxFieldFromSettings();
		maxValField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				updateMaxValFromField();
			}
		});
		maxValField.addFocusListener(new FocusAdapter(){
			public void focusLost(FocusEvent e) {
				updateMaxValFromField();
			}
		});
		
		autoMinMaxCheckBox = new JCheckBox("Autodetect min/max values");
		autoMinMaxCheckBox.setSelected(settings.getAutoMinMax());
		autoMinMaxCheckBox.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				updateAutoMinMaxFromCheckBox();
			}
		});
		
		resetButton = new JButton("Reset min/max".toUpperCase());
		resetButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				settings.setMinValue(Double.POSITIVE_INFINITY);
				settings.setMaxValue(Double.NEGATIVE_INFINITY);
			}
		});
		
		JLabel minLbl = new JLabel("Min:");
		JLabel maxLbl = new JLabel("Max:");
		
		JPanel out = new JPanel(new GridBagLayout());
		Insets in = new Insets(gap,gap,gap,gap);
		out.add(autoMinMaxCheckBox, new GridBagConstraints(0,0,2,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,gap,gap));
		out.add(minLbl, new GridBagConstraints(0,1,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,gap,gap));
		out.add(minValField, new GridBagConstraints(1,1,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,gap,gap));
		out.add(maxLbl, new GridBagConstraints(0,2,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,gap,gap));
		out.add(maxValField, new GridBagConstraints(1,2,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,gap,gap));
		out.add(resetButton, new GridBagConstraints(1,3,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,gap,gap));
		return out;
	}
	
	public StageSettings getSettings() {
		return settings;
	}
	
	public JPanel getStagePanel() {
		return stagePanel;
	}
	
	private void setFieldValue(JTextField textField, double val){
		if (Double.isInfinite(val)) {
			textField.setText(textField.isEnabled()? "": VAL_UNKNOWN);
		} else synchronized(nf) {
			textField.setText(nf.format(val));
		}
	}
	
	private void updateMaxFieldFromSettings(){
		maxValField.setEnabled(!settings.getAutoMinMax());
		setFieldValue(maxValField, settings.getMaxValue());
		maxValField.setCaretPosition(0);
	}
	
	private void updateMinFieldFromSettings(){
		minValField.setEnabled(!settings.getAutoMinMax());
		setFieldValue(minValField, settings.getMinValue());
		minValField.setCaretPosition(0);
	}
	
	private static double getFieldValue(JTextField textField, String unknownString, double unknownValue) throws ParseException {
		String text = textField.getText().trim();
		if (text.isEmpty() || unknownString.equals(text)) {
			return unknownValue;
		} else synchronized(nf) {
			return nf.parse(text).doubleValue();
		}
	}
	
	public void propertyChange(final PropertyChangeEvent e) {
		final String prop = e.getPropertyName();
		if (prop.equals(GrayscaleStageSettings.propMin)) {
			updateMinFieldFromSettings();
		} else if (prop.equals(GrayscaleStageSettings.propMax)) {
			updateMaxFieldFromSettings();
		} else if (prop.equals(GrayscaleStageSettings.propAutoMinMax)) {
			autoMinMaxCheckBox.setSelected(((Boolean)e.getNewValue()).booleanValue());
			updateMinFieldFromSettings();
			updateMaxFieldFromSettings();
		}
	}
}
