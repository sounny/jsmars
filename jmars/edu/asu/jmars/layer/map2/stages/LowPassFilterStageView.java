package edu.asu.jmars.layer.map2.stages;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.asu.jmars.layer.map2.StageSettings;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.util.DebugLog;

public class LowPassFilterStageView implements StageView, PropertyChangeListener {
	private static final DebugLog log = DebugLog.instance();
	
	private LowPassFilterStageSettings settings;
	private JTextField gridSizeField;
	private JPanel stagePanel;
	
	public LowPassFilterStageView(LowPassFilterStageSettings settings){
		this.settings = settings;
		stagePanel = buildUI();
		this.settings.addPropertyChangeListener(this);
	}
	
	private JPanel buildUI(){
		gridSizeField = new JTextField(4);
		gridSizeField.setFocusable(true);
		updateHeightFieldFromSettings();
		gridSizeField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateGridSizeFromField();
			}
		});
		
		JLabel gridSizeLbl = new JLabel("Kernel Grid Size:  ");
		FlowLayout flow = new FlowLayout(FlowLayout.CENTER);
		JPanel out = new JPanel(flow);
		out.add(gridSizeLbl);
		out.add(gridSizeField);
		
		
		
		return out;
	}
	
	private void updateGridSizeFromField(){
		try {
			settings.setGridSize(Integer.parseInt(gridSizeField.getText()));
		}
		catch(NumberFormatException ex){
			log.println(ex);
			gridSizeField.selectAll();
			gridSizeField.requestFocusInWindow();
		}
	}
	
	private void updateHeightFieldFromSettings(){
		gridSizeField.setText(String.valueOf(settings.getGridSize()));
		gridSizeField.setCaretPosition(0);
	}
	
	
	public void propertyChange(PropertyChangeEvent e) {
		final String prop = e.getPropertyName();
		if (prop.equals(LowPassFilterStageSettings.propKernelSize)){
			updateHeightFieldFromSettings();
		}
	}

	public JPanel getStagePanel() {
		return stagePanel;
	}

	public StageSettings getSettings() {
		return settings;
	}

}
