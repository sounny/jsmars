package edu.asu.jmars.layer.stamp.radar;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import edu.asu.jmars.util.Util;

public class DifferenceCreationDialog extends JDialog{

	private RadarHorizon myHorizon;
	private ArrayList<RadarHorizon> allHorizons;
	private HorizonPanel ownerPnl;
	private JComboBox<String> horizonBx;
	private JTextField constantTf;
	private JButton addBtn;
	private JButton cancelBtn;
	
	public DifferenceCreationDialog(Frame owner, Component showHere, RadarHorizon horizon, ArrayList<RadarHorizon> horizonList, HorizonPanel ownerPanel){
		super(owner, "Create New Horizon Difference", true);
		setLocationRelativeTo(showHere);
		
		myHorizon = horizon;
		//create a copy of the list and store it
		allHorizons = new ArrayList<RadarHorizon>(horizonList);
		ownerPnl = ownerPanel;
		
		//build the ui;
		buildLayout();
		pack();
		setVisible(true);
	}
	
	private void buildLayout(){
		JLabel listLbl = new JLabel("Choose a horizon to subtract:");
		
		//remove the selected horizon from the horizon list
		allHorizons.remove(myHorizon);
		
		//convert the list into string names
		Vector<String> horizonIDs = new Vector<String>();
		for(RadarHorizon h : allHorizons){
			horizonIDs.add(h.getID()+"");
		}
			
		//make the combo box of horizon choices
		horizonBx = new JComboBox<String>(horizonIDs);
		
		//label for the constant
		JLabel constantLbl = new JLabel("Set a dielectric constant:");
		//field for constant
		constantTf = new JTextField(8);
		//set the default entry to 1
		constantTf.setText("1");
				
		addBtn = new JButton(addAct);
		cancelBtn = new JButton(cancelAct);
		
		int pad = 3;
		Insets in =  new Insets(pad,pad,pad,pad);
		JPanel top = new JPanel(new GridBagLayout());
		top.setBorder(new EmptyBorder(5, 5, 5, 5));		
		top.add(listLbl, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		top.add(horizonBx, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		top.add(constantLbl, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		top.add(constantTf, new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		JPanel bot = new JPanel();		
		bot.add(addBtn);
		bot.add(Box.createHorizontalStrut(5));
		bot.add(cancelBtn);
		
		JPanel mainPnl = new JPanel(new BorderLayout());
		mainPnl.setBorder(new EmptyBorder(10, 10, 10, 10));
		mainPnl.add(top, BorderLayout.CENTER);
		mainPnl.add(bot, BorderLayout.SOUTH);
		
		setContentPane(mainPnl);
		getRootPane().setDefaultButton(addBtn);
	}
	
	/**
	 * Checks whether the user's input for the dielectric constant is an
	 * actual number.  If the input is not valid, false is returned.
	 * @return True if the user's input is valid.
	 */
	private boolean checkInputValidity(){
		try{
			double num = getDielectricConstant();
			//number cannot be less than 1
			if(num<1){
				return false;
			}
		}catch(NumberFormatException e){
			return false;
		}
		return true;
	}
	
	
	/**
	 * Parses the input from the constant text field and tries to cast
	 * it to double.
	 * @return Returns the double value of the input.
	 * @throws NumberFormatException  If the input is not a valid double,
	 * 								  this exception is thrown.
	 */
	private double getDielectricConstant() throws NumberFormatException{
		return Double.parseDouble(constantTf.getText());
	}
		
	
	private AbstractAction addAct = new AbstractAction("Create Difference".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			if(checkInputValidity()){
				HorizonDifference hd = new HorizonDifference(myHorizon, getSelectedHorizon(), getDielectricConstant());
				myHorizon.addHorizonDifference(hd);
				DifferenceCreationDialog.this.dispose();
				//update the horizon table
				ownerPnl.refreshHorizonDifferenceTable();
			}else{
				Util.showMessageDialog("Invalid entry for dielectric constant.\n"
						+ "Must be a number greater than or equal to 1.", 
						"Invalid Entry",
						JOptionPane.ERROR_MESSAGE);
			}
		}
	};
	
	private AbstractAction cancelAct = new AbstractAction("Cancel".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			DifferenceCreationDialog.this.dispose();
		}
	};
	
	private RadarHorizon getSelectedHorizon(){
		int id = Integer.parseInt((String)horizonBx.getSelectedItem());
		for(RadarHorizon r : allHorizons){
			if(r.getID() == id){
				return r;
			}
		}
		return null;
	}
}
