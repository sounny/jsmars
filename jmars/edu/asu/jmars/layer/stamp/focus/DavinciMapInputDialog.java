package edu.asu.jmars.layer.stamp.focus;

import java.awt.Dialog;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class DavinciMapInputDialog extends Dialog {

	private JTextField lowerLeftLonTf = null;
	private JTextField upperLeftLonTf = null;
	private JTextField lowerRightLonTf = null;
	private JTextField upperRightLonTf = null;
	private JTextField lowerLeftLatTf = null;
	private JTextField upperLeftLatTf = null;
	private JTextField lowerRightLatTf = null;
	private JTextField upperRightLatTf = null;
	private JTextField nameTf = null;
	private JTextField ignoreTf = null;
	private JLabel nameLbl = null;
	private JLabel ignoreLbl = null;
	private JLabel lowerLeftLonLbl = null;
	private JLabel lowerRightLonLbl = null;
	private JLabel upperLeftLonLbl = null;
	private JLabel upperRightLonLbl = null;
	private JLabel lowerLeftLatLbl = null;
	private JLabel lowerRightLatLbl = null;
	private JLabel upperLeftLatLbl = null;
	private JLabel upperRightLatLbl = null;
	private JButton cancelBtn = null;
	private JButton clearBtn = null;
	private JButton okBtn = null;
	
	private JPanel mainPanel = null;

	public DavinciMapInputDialog(Dialog owner) {
		super(owner);
		initComponents();
		layoutDialog();
	}
	
	private void initComponents() {
		lowerLeftLonTf = new JTextField();
		lowerRightLonTf = new JTextField();
		upperLeftLonTf = new JTextField();
		upperRightLonTf = new JTextField();
		lowerLeftLatTf = new JTextField();
		lowerRightLatTf = new JTextField();
		upperLeftLatTf = new JTextField();
		upperRightLatTf = new JTextField();
		nameTf = new JTextField();
		ignoreTf = new JTextField();
		nameLbl = new JLabel("Name: ");
		ignoreLbl = new JLabel("Ignore Value: ");
		lowerLeftLonLbl = new JLabel("Lower Left Lon: ");
		lowerLeftLatLbl = new JLabel("Lower Left Lat: ");
		upperLeftLonLbl = new JLabel("Upper Left Lon: ");
		upperLeftLatLbl = new JLabel("Upper Left Lat: ");
		lowerRightLonLbl = new JLabel("Lower Right Lon: ");
		lowerRightLatLbl = new JLabel("Lower Right Lat: ");
		upperRightLonLbl = new JLabel("Upper Right Lon: ");
		upperRightLatLbl = new JLabel("Upper Right Lat: ");
		
		cancelBtn = new JButton(cancelAction);
		clearBtn = new JButton(clearAction);
		okBtn = new JButton(okayAction);
		
		mainPanel = new JPanel();
		
		
	}
	private AbstractAction cancelAction = new AbstractAction("CANCEL") {

		@Override
		public void actionPerformed(ActionEvent e) {
			clearEntries();
			setVisible(false);
		}
		
	};
	private AbstractAction clearAction = new AbstractAction("CLEAR") {

		@Override
		public void actionPerformed(ActionEvent e) {
			clearEntries();
		}
		
	};
	private AbstractAction okayAction = new AbstractAction("OKAY") {

		@Override
		public void actionPerformed(ActionEvent e) {
			setVisible(false);
		}
		
	};
	private void clearEntries() {
		lowerLeftLatTf.setText("");
		lowerRightLatTf.setText("");
		lowerLeftLonTf.setText("");
		lowerRightLonTf.setText("");
		upperLeftLatTf.setText("");
		upperRightLatTf.setText("");
		upperLeftLonTf.setText("");
		upperRightLonTf.setText("");
		nameTf.setText("");
		ignoreTf.setText("");
	}
	public String getLowerLeftLat() {
		return lowerLeftLatTf.getText();
	}
	public String getLowerLeftLon() {
		return lowerLeftLonTf.getText();
	}
	public String getLowerRightLat() {
		return lowerRightLatTf.getText();
	}
	public String getLowerRightLon() {
		return lowerRightLonTf.getText();
	}
	public String getUpperLeftLat() {
		return upperLeftLatTf.getText();
	}
	public String getUpperLeftLon() {
		return upperLeftLonTf.getText();
	}
	public String getUpperRightLat() {
		return upperRightLatTf.getText();
	}
	public String getUpperRightLon() {
		return upperRightLonTf.getText();
	}
	public String getName() {
		return nameTf.getText();
	}
	public String getIgnoreValue() {
		return ignoreTf.getText();
	}
	private void layoutDialog() {
		GroupLayout layout = new GroupLayout(mainPanel);
		mainPanel.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
			.addGroup(layout.createSequentialGroup()
				.addComponent(nameLbl)
				.addComponent(nameTf,80,80,80)
				.addGap(20)
				.addComponent(ignoreLbl)
				.addComponent(ignoreTf,50,50,50))
			.addGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(upperLeftLatLbl)
					.addComponent(upperLeftLonLbl))
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(upperLeftLatTf,50,50,50)
					.addComponent(upperLeftLonTf,50,50,50))
				.addGap(50)
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(upperRightLatLbl)
					.addComponent(upperRightLonLbl))
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(upperRightLatTf,50,50,50)
					.addComponent(upperRightLonTf,50,50,50)))
			.addGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(lowerLeftLatLbl)
					.addComponent(lowerLeftLonLbl))
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(lowerLeftLonTf,50,50,50)
					.addComponent(lowerLeftLatTf,50,50,50))
				.addGap(50)
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(lowerRightLatLbl)
					.addComponent(lowerRightLonLbl))
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(lowerRightLatTf,50,50,50)
					.addComponent(lowerRightLonTf,50,50,50)))
			.addGroup(layout.createSequentialGroup()
				.addComponent(cancelBtn)
				.addComponent(clearBtn)
				.addComponent(okBtn)));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup(Alignment.LEADING)
				.addComponent(nameLbl)
				.addComponent(nameTf,20,20,20)
				.addComponent(ignoreLbl)
				.addComponent(ignoreTf,20,20,20))
			.addGap(30)
			.addGroup(layout.createParallelGroup(Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addComponent(upperLeftLatLbl)
						.addComponent(upperLeftLatTf,20,20,20))
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addComponent(upperLeftLonLbl)
						.addComponent(upperLeftLonTf,20,20,20)))
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addComponent(upperRightLatLbl)
						.addComponent(upperRightLatTf,20,20,20))
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addComponent(upperRightLonLbl)
						.addComponent(upperRightLonTf,20,20,20))))
			.addGap(50)
			.addGroup(layout.createParallelGroup(Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addComponent(lowerLeftLatLbl)
						.addComponent(lowerLeftLatTf,20,20,20))
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addComponent(lowerLeftLonLbl)
						.addComponent(lowerLeftLonTf,20,20,20)))
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addComponent(lowerRightLatLbl)
						.addComponent(lowerRightLatTf,20,20,20))
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addComponent(lowerRightLonLbl)
						.addComponent(lowerRightLonTf,20,20,20))))
			.addGap(30)
			.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(cancelBtn)
					.addComponent(clearBtn)
					.addComponent(okBtn)));
		
		add(mainPanel);
		setTitle("Resampling Information");
		setResizable(false);
		setModal(true);
		pack();
		
	}

	public static void main(String[] args) {
		DavinciMapInputDialog rd = new DavinciMapInputDialog(null);
		rd.setVisible(true);
		System.out.println("Name: "+rd.getName());
		System.out.println("Ignore: "+rd.getIgnoreValue());
	}

}
