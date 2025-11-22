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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.asu.jmars.layer.map2.StageSettings;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.util.DebugLog;

public class SigmaStretchStageView implements StageView, PropertyChangeListener {
	private static final DebugLog log = DebugLog.instance();
	private static final String VAL_UNKNOWN = "unknown";
	private static final DecimalFormat nf = new DecimalFormat("###0.########");
	private static final int gap = 4;
	
	private SigmaStretchStageSettings settings;
	private JTextField varianceValField;
	private JTextField minPPDField;
	private JPanel stagePanel;
	
	
	public SigmaStretchStageView(SigmaStretchStageSettings settings){
		this.settings = settings;
		stagePanel = buildUI();
		settings.addPropertyChangeListener(this);
	}
	
	private void updateVarianceValFromField(){
		try {
			settings.setVariance(getFieldValue(varianceValField, VAL_UNKNOWN, Double.POSITIVE_INFINITY));
		} catch(ParseException ex){
			log.println(ex);
			varianceValField.selectAll();
			varianceValField.requestFocusInWindow();
		}
	}

	private void updateMinPPDFromField(){
		try {
			settings.setMinPPD(getFieldValue(minPPDField, VAL_UNKNOWN, Double.POSITIVE_INFINITY));
		} catch(ParseException ex){
			log.println(ex);
			minPPDField.selectAll();
			minPPDField.requestFocusInWindow();
		}
	}

	private JPanel buildUI() {
		varianceValField = new JTextField(6);
		varianceValField.setFocusable(true);
		updateVarianceFromSettings();
		varianceValField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				updateVarianceValFromField();
			}
		});
		varianceValField.addFocusListener(new FocusAdapter(){
			public void focusLost(FocusEvent e) {
				updateVarianceValFromField();
			}
		});
						
		JLabel varianceLbl = new JLabel("Variance:");
		
		minPPDField = new JTextField(6);
		minPPDField.setFocusable(true);
		updateMinPPDFromSettings();
		minPPDField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				updateMinPPDFromField();
			}
		});
		minPPDField.addFocusListener(new FocusAdapter(){
			public void focusLost(FocusEvent e) {
				updateMinPPDFromField();
			}
		});
						
		JLabel minPPDLbl = new JLabel("Min PPD:");
		minPPDLbl.setToolTipText("Minimum Zoom level this stage will affect.  At lower PPDs, no stretch will be applied");
		
		JPanel out = new JPanel(new GridBagLayout());
		Insets in = new Insets(gap,gap,gap,gap);
		int row=0;
		out.add(varianceLbl, new GridBagConstraints(0,row,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,gap,gap));
		out.add(varianceValField, new GridBagConstraints(1,row++,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,gap,gap));

		out.add(minPPDLbl, new GridBagConstraints(0,row,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,gap,gap));
		out.add(minPPDField, new GridBagConstraints(1,row++,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,gap,gap));

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
		
	private void updateVarianceFromSettings() {
		setFieldValue(varianceValField, settings.getVariance());
		varianceValField.setCaretPosition(0);
	}

	private void updateMinPPDFromSettings() {
		setFieldValue(minPPDField, settings.getMinPPD());
		minPPDField.setCaretPosition(0);
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
		if (prop.equals(SigmaStretchStageSettings.propVar)) {
			updateVarianceFromSettings();
		}
	}
}
