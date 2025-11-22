package edu.asu.jmars.layer.stamp.spectra;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SpectraPixelDialog extends JDialog {
	
	private int pad = 2;
	private Insets in = new Insets(pad, pad, pad, pad);

	private JLabel nameLabel;
	private JLabel rowLabel;
	private JLabel colLabel;
	private JLabel sizeLabel;
	
	private JTextField nameTF;
	private JTextField rowTF;
	private JTextField colTF;
	private JTextField sizeTF;
	
	private JButton okBtn;
	private JButton cancelBtn;

	String lineStr;
	String sampleStr;
	
	public String name;
	public int line;
	public int sample;
	public int xsize;
	public int ysize;
	
	public boolean okayClicked = false;
	
	
	public SpectraPixelDialog(JFrame parent, short line, short sample, JComponent showHere){
		super(parent, "Pixel Selection", true);
		
		lineStr = ""+line;
		sampleStr = ""+sample;
		
		setupDialog(showHere);
	}
	
	public void setupDialog(JComponent showHere) {
		//set the location relative to but centered on the component passed in
		Point pt = MouseInfo.getPointerInfo().getLocation();
		setLocation(pt.x, pt.y);
		
		setSize(500,500);
		
		//build UI
		buildUI();
		pack();
		setVisible(true);
	}
	
	private void buildUI() {				
		nameLabel = new JLabel("Spectra name: ");
		rowLabel = new JLabel("Row / Line: ");
		colLabel = new JLabel("Column / Sample: ");
		sizeLabel = new JLabel("Size: ");
		
		nameTF = new JTextField(15);
		nameTF.setText("Line: " + lineStr + " Sample: " + sampleStr);
		rowTF = new JTextField(8);
		rowTF.setText(lineStr);
		colTF = new JTextField(8);
		colTF.setText(sampleStr);
		
		sizeTF = new JTextField(8);
		sizeTF.setText("3x3");
		
		
		okBtn = new JButton(okAct);
		cancelBtn = new JButton(cancelAct);
				
		JPanel mainPnl = new JPanel();
		mainPnl.setLayout(new BorderLayout());
		
		JPanel fieldPanel = new JPanel();
		
		fieldPanel.setLayout(new GridBagLayout());
		
		int col=0;
		int row=0;
		
		fieldPanel.add(nameLabel, new GridBagConstraints(col, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		fieldPanel.add(nameTF, new GridBagConstraints(col+1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		
		fieldPanel.add(rowLabel, new GridBagConstraints(col, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		fieldPanel.add(rowTF, new GridBagConstraints(col+1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		
		fieldPanel.add(colLabel, new GridBagConstraints(col, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		fieldPanel.add(colTF, new GridBagConstraints(col+1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		
		fieldPanel.add(sizeLabel, new GridBagConstraints(col, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		fieldPanel.add(sizeTF, new GridBagConstraints(col+1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		
		mainPnl.add(fieldPanel, BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		
		buttonPanel.add(okBtn);
		buttonPanel.add(cancelBtn);
		
		mainPnl.add(buttonPanel, BorderLayout.SOUTH);
				
		setContentPane(mainPnl);
		pack();
	
	}
	
	
	private AbstractAction okAct = new AbstractAction("OK".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			name = nameTF.getText();
			line = Integer.parseInt(rowTF.getText().trim());
			sample = Integer.parseInt(colTF.getText().trim());
			
			String sizeString = sizeTF.getText().trim();
			
			xsize = Integer.parseInt(sizeString.substring(0, sizeString.indexOf("x")));
			ysize = Integer.parseInt(sizeString.substring(sizeString.indexOf("x")+1));
			
			okayClicked = true;
			
			SpectraPixelDialog.this.setVisible(false);
		}
	};
	
	private AbstractAction cancelAct = new AbstractAction("Cancel".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			
			okayClicked = false;
			
			SpectraPixelDialog.this.setVisible(false);
		}
	};

}
