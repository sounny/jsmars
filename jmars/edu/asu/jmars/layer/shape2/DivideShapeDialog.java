package edu.asu.jmars.layer.shape2;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;

import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.swing.DocumentCharFilter;

public class DivideShapeDialog extends JDialog{
	ShapeLayer shapeLayer;
	
	private JTextField rowNumField;
	private JTextField colNumField;
	private JButton createBtn;
	private JButton cancelBtn;
		
	public DivideShapeDialog(Frame owner, JComponent relTo, ShapeLayer shapeLayer){
		super(owner, "Shape Divider Tool", true);
		setLocationRelativeTo(relTo);
		
		this.shapeLayer = shapeLayer;
		
		//build UI if the frame and owner aren't null
		// if they are null, then this is being run from
		// a unit test, and we don't need ui
		if(owner != null){
			buildUI();
			pack();
		}
	}
	
	private void buildUI(){
		//First part of the dialog -- select columns
		JPanel selPnl = new JPanel();
		selPnl.setBorder(new TitledBorder("Divide selected shape into:"));
		
		rowNumField=new JTextField(5);
		colNumField=new JTextField(5);
		
		DocumentFilter filter = getNumberFilter();
		((AbstractDocument)rowNumField.getDocument()).setDocumentFilter(filter);
		((AbstractDocument)colNumField.getDocument()).setDocumentFilter(filter);
		
		selPnl.add(new JLabel("Rows:"));
		selPnl.add(rowNumField);
		selPnl.add(Box.createHorizontalStrut(5));
		selPnl.add(new JLabel("Columns:"));
		selPnl.add(colNumField);
								
		//last panel -- save file
		JPanel createPnl = new JPanel();		
		createBtn = new JButton(createAct);
		cancelBtn = new JButton(cancelAct); 
		createPnl.add(createBtn);
		createPnl.add(Box.createHorizontalStrut(5));
		createPnl.add(cancelBtn);
		
		//make an imbedded panel for most of the content
		JPanel innerPnl = new JPanel();		
		innerPnl.setLayout(new BorderLayout());
		innerPnl.add(selPnl, BorderLayout.NORTH);
		
		//add everything to main panel
		JPanel mainPnl = new JPanel();		
		mainPnl.setLayout(new BorderLayout());
		mainPnl.setBorder(new EmptyBorder(10, 5, 5, 5));
		
		mainPnl.add(innerPnl, BorderLayout.CENTER);
		mainPnl.add(createPnl, BorderLayout.SOUTH);
		
		setContentPane(mainPnl);
	}
	
	private DocumentFilter getNumberFilter(){
		ArrayList<Character> filterList = new ArrayList<Character>();
		filterList.add('0');
		filterList.add('1');
		filterList.add('2');
		filterList.add('3');
		filterList.add('4');
		filterList.add('5');
		filterList.add('6');
		filterList.add('7');
		filterList.add('8');
		filterList.add('9');
		
		return new DocumentCharFilter(filterList, true);
	}
	
	private AbstractAction createAct = new AbstractAction("Create shapes".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			ArrayList<Feature> toAdd = new ArrayList<Feature>();

			for (Feature f : shapeLayer.getSelections()) {
				Rectangle2D bounds = f.getPath().getWorld().getShape().getBounds2D();
					
				int rows;
				int cols;
				try {
					rows = Integer.parseInt(rowNumField.getText());
					cols = Integer.parseInt(colNumField.getText());
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(DivideShapeDialog.this, "Inputs must each be between 1 and 50", "Invalid number of rows or columns", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				if (rows<1 || rows>50 || cols<1 || cols>50) {
					JOptionPane.showMessageDialog(DivideShapeDialog.this, "Inputs must each be between 1 and 50", "Invalid number of rows or columns", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				double startx = bounds.getMinX();
				double starty = bounds.getMinY();
				
				double xDelta = bounds.getWidth()/cols;
				double yDelta = bounds.getHeight()/rows;
					
				for (int x = 0; x<cols; x++) {
					for (int y=0; y<rows; y++) {
						double vertices[]=new double[8];
						
						vertices[0] = startx + xDelta*x;
						vertices[1] = starty + yDelta*y;
						
						vertices[2] = startx + xDelta*(x+1);;
						vertices[3] = starty + yDelta*y;
						
						vertices[4] = startx + xDelta*(x+1);;
						vertices[5] = starty + yDelta*(y+1);
						
						vertices[6] = startx + xDelta*x;;
						vertices[7] = starty + yDelta*(y+1);
						
						FPath newPath = new FPath(vertices, false, FPath.WORLD, true).getSpatialWest();
						Feature newFeature = new Feature();
						newFeature.setPath(newPath);
						toAdd.add(newFeature);
					}
				}					
			}
			
			if (toAdd.size()>0) {
				// Make a new history frame.
				if (shapeLayer.getHistory() != null) {
					shapeLayer.getHistory().mark();
				}
				shapeLayer.getFeatureCollection().addFeatures(toAdd);
			}
				
			setVisible(false);
			DivideShapeDialog.this.dispose();
		}
	};	
	
	private AbstractAction cancelAct = new AbstractAction("Cancel".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			DivideShapeDialog.this.dispose();
		}
	};
	
}
