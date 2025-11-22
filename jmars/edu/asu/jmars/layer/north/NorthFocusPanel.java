package edu.asu.jmars.layer.north;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.swing.ColorButton;
import edu.asu.jmars.util.Util;

public class NorthFocusPanel extends FocusPanel{
	private JCheckBox leftChk;
	private JCheckBox rightChk;
	private ColorButton arrowCBtn;
	private JComboBox<Integer> sizeBx;
	private JComboBox<Float> outlineBx;
	private JCheckBox textChk;
	private ColorButton textCBtn;
	private JComboBox<Integer> fontBx;
	
	private int pad = 1;
	private Insets in = new Insets(pad, pad, pad, pad);
	
	private NorthSettings settings;
	
	

	public NorthFocusPanel(NorthLView parent) {
		super(parent, true);
		
		settings = ((NorthLayer)parent.getLayer()).getSettings();
		
		//create settings panel
		add("Settings", createSettingsPanel());
	}
	
	private JPanel createSettingsPanel(){
		JPanel panel = new JPanel();		
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		//Arrow Settings
		JPanel arrowPnl = new JPanel(new GridBagLayout());
		arrowPnl.setBorder(new TitledBorder("Arrow Settings"));
		leftChk = new JCheckBox(leftAct);
		leftChk.setSelected(settings.fillLeft);
		rightChk = new JCheckBox(rightAct);
		rightChk.setSelected(settings.fillRight);
		arrowCBtn = new ColorButton("Arrow Color", settings.arrowColor, true);
		arrowCBtn.addPropertyChangeListener(colorListener);
		JLabel sizeLbl = new JLabel("Size:");
		sizeBx = new JComboBox<Integer>();
		sizeBx.addItem(1);
		sizeBx.addItem(2);
		sizeBx.addItem(3);
		sizeBx.addItem(4);
		sizeBx.addItem(5);
		sizeBx.addActionListener(sizeListener);
		//set the selected size based off settings
		sizeBx.setSelectedItem(settings.arrowSize);
		JPanel sizePnl = new JPanel();
		sizePnl.add(sizeLbl);
		sizePnl.add(sizeBx);
		JLabel outlineLbl = new JLabel("Outline Size: ");
		outlineBx = new JComboBox<Float>();
		outlineBx.addItem(2f);
		outlineBx.addItem(3f);
		outlineBx.addItem(4f);
		outlineBx.addItem(5f);
		outlineBx.addActionListener(sizeListener);
		//set the selected size based off settings
		outlineBx.setSelectedItem(settings.outlineSize);
		JPanel outlinePnl = new JPanel();
		outlinePnl.add(outlineLbl);
		outlinePnl.add(outlineBx);
		int row = 0;
		arrowPnl.add(leftChk, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		arrowPnl.add(rightChk, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		arrowPnl.add(arrowCBtn, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		arrowPnl.add(sizePnl, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		arrowPnl.add(outlinePnl, new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		
		//Text settings
		JPanel textPnl = new JPanel(new GridBagLayout());
		textPnl.setBorder(new TitledBorder("Text Settings"));
		textChk = new JCheckBox(textAct);
		textChk.setSelected(settings.showText);
		textCBtn = new ColorButton("'N' Color", settings.textColor, true);
		textCBtn.addPropertyChangeListener(colorListener);
		JLabel fontLbl = new JLabel("Font Size: ");
		fontBx = new JComboBox<Integer>();
		fontBx.addItem(16);
		fontBx.addItem(20);
		fontBx.addItem(24);
		fontBx.addItem(28);
		fontBx.addItem(32);
		fontBx.addActionListener(sizeListener);
		//set the selected size based off settings
		fontBx.setSelectedItem(settings.fontSize);
		JPanel fontPnl = new JPanel();
		fontPnl.add(fontLbl);
		fontPnl.add(fontBx);
		row = 0;
		textPnl.add(textChk, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		textPnl.add(textCBtn, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		textPnl.add(fontPnl, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));

		//add all to main panel
		panel.add(arrowPnl);
		panel.add(Box.createVerticalStrut(5));
		panel.add(textPnl);
		
		return panel;
	}
	
	
	private AbstractAction leftAct = new AbstractAction("Fill Left Side") {
		public void actionPerformed(ActionEvent e) {
			//update the left fill setting based on the checkbox
			settings.fillLeft = leftChk.isSelected();
			//repaint lview
			parent.repaint();
		}
	};
	
	private AbstractAction rightAct = new AbstractAction("Fill Right Side") {
		public void actionPerformed(ActionEvent e) {
			//update the right fill setting based on the checkbox
			settings.fillRight = rightChk.isSelected();
			//repaint lview
			parent.repaint();
		}
	};
	
	private PropertyChangeListener colorListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			//if this is from the arrow color button, update arrow color
			if(evt.getSource() == arrowCBtn){
				settings.arrowColor = arrowCBtn.getColor();
			}
			//if this is from text color button, update text
			if(evt.getSource() == textCBtn){
				settings.textColor = textCBtn.getColor();
			}
			//repaint lview
			parent.repaint();
			
		}
	};
	
	private ActionListener sizeListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			//if this is from arrow size combobox
			if(e.getSource() == sizeBx){
				settings.arrowSize = (int)sizeBx.getSelectedItem();
			}
			//if this is from arrow outline combobox
			if(e.getSource() == outlineBx){
				settings.outlineSize = (float)outlineBx.getSelectedItem();
			}
			//if this is from font size combobox
			if(e.getSource() == fontBx){
				settings.fontSize = (int)fontBx.getSelectedItem();
			}
			//repaint lview
			parent.repaint();
		}
	};

	
	private AbstractAction textAct = new AbstractAction("Show 'N'") {
		public void actionPerformed(ActionEvent e) {
			//update the show text boolean
			settings.showText = textChk.isSelected();
			//repaint lview
			parent.repaint();
		}
	};
}
