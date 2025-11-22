package edu.asu.jmars.layer.krc;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;

@SuppressWarnings("serial")
public class AddDataPointDialog extends JDialog{
	private JTextField nameTF;
	private JTextField lonTF;
	private JTextField latTF;
	private JButton addBtn;
	private JButton editBtn;
	private JButton cancelBtn;
	private boolean fromEdit = false;
	
	private KRCLView myLView;
	private KRCLayer myLayer;
	private KRCDataPoint myDP;
	private String name;
	private Double lat;
	private Double lon;
	
	private int pad = 2;
	private Insets in = new Insets(pad,pad,pad,pad);
	private DecimalFormat locFormat = new DecimalFormat("#.#####");
	
	public AddDataPointDialog(Frame owner, KRCLView lview, String name, Double lat, Double lon, KRCDataPoint dp, boolean editing){
		super(owner, "Add KRC Data Point Dialog", true);
		setLocationRelativeTo(owner);
		
		myLView = lview;
		myLayer = (KRCLayer)myLView.getLayer();
		
		
		
		if(editing){
			myDP = dp;
			fromEdit = true;
			setTitle("Edit KRC Data Point Dialog");
		}else{
			this.name = name;
			this.lat = lat;
			this.lon = lon;
		}
		
		buildLayout();
		pack();
		setVisible(true);
	}
	
	
	private void buildLayout(){
		JPanel mainPnl = new JPanel(new GridBagLayout());		
		mainPnl.setBorder(new EmptyBorder(5, 5, 0, 5));
		
		//inputs
		JLabel nameLbl = new JLabel("Name:");
		nameTF = new JTextField();
		JLabel lonLbl = new JLabel("Longitude (E):");
		lonTF = new JTextField(8);
		JLabel latLbl = new JLabel("Latitude (N):");
		latTF = new JTextField(8);
		
		//buttons
		JPanel buttonPnl = new JPanel();		
		cancelBtn = new JButton(cancelAct);
		if(!fromEdit){
			addBtn = new JButton(addAct);
			buttonPnl.add(addBtn);
			getRootPane().setDefaultButton(addBtn);
		}else{
			editBtn = new JButton(editAct);
			buttonPnl.add(editBtn);
			getRootPane().setDefaultButton(editBtn);
		}
		
		buttonPnl.add(Box.createHorizontalStrut(5));
		buttonPnl.add(cancelBtn);
		
		mainPnl.add(nameLbl, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		mainPnl.add(nameTF, new GridBagConstraints(1, 0, 3, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, in, pad, pad));
		mainPnl.add(lonLbl, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		mainPnl.add(lonTF, new GridBagConstraints(1, 1, 1, 1, 0.5, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, in, pad, pad));
		mainPnl.add(latLbl, new GridBagConstraints(2, 1, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		mainPnl.add(latTF, new GridBagConstraints(3, 1, 1, 1, 0.5, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, in, pad, pad));
		mainPnl.add(buttonPnl, new GridBagConstraints(0, 2, 4, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		
		setContentPane(mainPnl);
		
		populateFields();
	}
	
	private void populateFields(){
		if(myDP!=null){
			nameTF.setText(myDP.getName());
			lonTF.setText(myDP.getLon()+"");
			latTF.setText(myDP.getLat()+"");
		}else{
			nameTF.setText(name);
			if(lon!=null && lat!=null){
				lonTF.setText(locFormat.format(lon));
				latTF.setText(locFormat.format(lat));
			}
		}
	}
	
	private KRCDataPoint createDataPoint(){
		//get coords
		double lat;
		double lon;
		try{
			lat = Double.parseDouble(latTF.getText());
			lon = Double.parseDouble(lonTF.getText());
			
			String name = nameTF.getText();
			if(name.equals("") || (fromEdit == false && !myLayer.isUniqueName(name))){
				Util.showMessageDialog("KRC data point creation input not valid.\nName must be unique.",
						"Invalid Name", JOptionPane.ERROR_MESSAGE);
				return null;
			}

			//create krc data point, set the datapoint in degrees E
			KRCDataPoint krcdp = new KRCDataPoint(myLayer, name, lat, lon);

			return krcdp;
			
		}catch(NumberFormatException ex){
			Util.showMessageDialog("KRC data point creation input not valid.\nMust be valid numbers.",
					"Invalid Location", JOptionPane.ERROR_MESSAGE);
			return null;
		}
	}
	
	private AbstractAction addAct = new AbstractAction("Add".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			KRCDataPoint dp = createDataPoint();
			
			if(dp!=null){
				//add to layer
				myLayer.addDataPoint(dp);
				//update focus panel
				myLView.getFocusPanel().refreshDataTables();
				//refresh lview
				myLView.repaint();
				
				//update state ids
				myLayer.increaseStateId(KRCLayer.IMAGES_BUFFER);
				myLayer.increaseStateId(KRCLayer.LABELS_BUFFER);
				//refresh 3D
				if(ThreeDManager.isReady()){
					ThreeDManager.getInstance().updateDecalsForLView(myLView, true);
				}
				//close this dialog
				AddDataPointDialog.this.dispose();
			}
		}
	};
	
	private AbstractAction editAct = new AbstractAction("Edit".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			KRCDataPoint dp = createDataPoint();
			
			if(dp!=null){
				//replace on layer
				myLayer.replaceDataPoint(myDP, dp);
				//update focus panel
				myLView.getFocusPanel().refreshDataTables();
				//refresh lview
				myLView.repaint();
				
				//update state ids
				myLayer.increaseStateId(KRCLayer.IMAGES_BUFFER);
				myLayer.increaseStateId(KRCLayer.LABELS_BUFFER);
				//refresh 3D
				if(ThreeDManager.isReady()){
					ThreeDManager.getInstance().updateDecalsForLView(myLView, true);
				}
				//close this dialog
				AddDataPointDialog.this.dispose();
			}
		}
	};
	
	private AbstractAction cancelAct = new AbstractAction("Cancel".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			AddDataPointDialog.this.dispose();
		}
	};
}
