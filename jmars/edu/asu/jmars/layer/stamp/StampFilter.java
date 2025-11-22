package edu.asu.jmars.layer.stamp;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.SliderUI;

import edu.asu.msff.DataField;

public class StampFilter {

	boolean filterActive=false;
	String columnName;
	
	private int currentValue;
	
	
	// Initially set to the values specified by the Datafield, and used to set the 
	// start/end points of the slider.  These values can be overriden by the user.
	private int minAllowedValue;
	private int maxAllowedValue;
	
	private DataField myDataField;
	
	String extent;
	
	// How many values +/- do we show from the currentValue?
	int spread=5;
	
	int dataIndex=-1;
	int dataIndex2=-1;
	
	boolean isSimplifiedExpertRange=false;
	
	// JSlider works on integers, but some of the data we want to slide on is floats
	// so we apply a factor to convert floats to/from integers, retaining some fraction
	// of the total precision (just for slide increments, the data precision is unaltered)
	public static final int FLOAT_FACTOR = 1;
	
	JSlider mySlider;
	JCheckBox myCheckBox;
	JTextField minField;
	JTextField maxField;
	JTextField spreadField;
	JTextField currentField;

    JLabel showBetween;
    JLabel and;
    JLabel showValues;
    JLabel from;
	
	private void enableAll() {
		boolean b = true;
		mySlider.setEnabled(b);
		myCheckBox.setSelected(b);
		minField.setEnabled(b);
		maxField.setEnabled(b);
		spreadField.setEnabled(b);
		currentField.setEnabled(b);
		showBetween.setEnabled(b);
		and.setEnabled(b);
		showValues.setEnabled(b);
		from.setEnabled(b);
	}

	private void disableAll() {
		boolean b = false;
		mySlider.setEnabled(b);
		myCheckBox.setSelected(b);
		minField.setEnabled(b);
		maxField.setEnabled(b);
		spreadField.setEnabled(b);
		currentField.setEnabled(b);
		showBetween.setEnabled(b);
		and.setEnabled(b);
		showValues.setEnabled(b);
		from.setEnabled(b);
	}

	public StampFilter(DataField df) {
		myDataField = df;
		columnName=df.getColumnName();	
	}
	
	public String toString() {
		return myDataField.getDisplayName() + " filter";
	}
	
	JPanel focusSettings;
	
	public JPanel getUI(final StampLayer stampLayer) {
		if (focusSettings==null) {
	        focusSettings = new JPanel(new GridBagLayout());
	
	        showBetween = new JLabel("Show values between: ");
	        and = new JLabel("and");
	        showValues = new JLabel("Show values +/- ");
	        from = new JLabel("from");
	
			mySlider = new JSlider() {
				{
					String minVal = myDataField.getMinAllowedValue();
					String maxVal = myDataField.getMaxAllowedValue();
					
					minAllowedValue=0;
					maxAllowedValue=100;
					
					try {
						minAllowedValue = (int) (FLOAT_FACTOR * Float.parseFloat(minVal));
						maxAllowedValue = (int) (FLOAT_FACTOR * Float.parseFloat(maxVal));							
					} catch (NumberFormatException nfe) {
						System.out.println("minVal of " + minVal + " is not an Float");
					}
					
					
	                setValue(minAllowedValue);
	                setMaximum(maxAllowedValue);
	                setMinimum(minAllowedValue);
	                                
	                if ((maxAllowedValue-minAllowedValue)==360) {
	                	setMajorTickSpacing(45);
	                	setMinorTickSpacing(5);
	                } else if ((maxAllowedValue-minAllowedValue)==180) {
	                	setMajorTickSpacing(45);
	                	setMinorTickSpacing(4);
	                } else if ((maxAllowedValue-minAllowedValue)>20000) {
	                	setMajorTickSpacing(5000);
	                	setMinorTickSpacing(1000);                	
					} else if ((maxAllowedValue-minAllowedValue)>8) {
	                	setMajorTickSpacing((int)Math.round((maxAllowedValue-minAllowedValue)/8));
	                	setMinorTickSpacing((int)Math.round((maxAllowedValue-minAllowedValue)/72));
	                } else {
	                	setMajorTickSpacing(1);
	                }
	                
	               // setSnapToTicks(true);
	
	                setPaintLabels(true);
	//                setPaintTicks(true);
	                setExtent(0);
	                setEnabled(false);
	                
	                addChangeListener(new ChangeListener() {
	                	public void stateChanged(ChangeEvent e) {
	                		currentValue = getValue();
	//                		Dictionary<Integer, JLabel> newDict = new Hashtable<Integer, JLabel>();
	//                		//newDict.put(new Integer(currentValue), new JLabel(""+currentValue));
	//                		
	//                		int minVal = getMinValueToMatch();
	//                		int maxVal = getMaxValueToMatch();
	//                		
	//                		JLabel minLabel = new JLabel(""+minVal);
	//                		minLabel.setForeground(Color.BLUE);
	//
	//                		JLabel maxLabel = new JLabel(""+maxVal);
	//                		maxLabel.setForeground(Color.BLUE);
	//
	//                		newDict.put(new Integer(minVal), minLabel);
	//                		newDict.put(new Integer(maxVal), maxLabel);
	//                		
	//                		setLabelTable(newDict);
	                		updateDisplayFields();
	                		stampLayer.updateVisibleStamps();
	                	}
	                });
	            }
			};
			
			final JPopupMenu popup = new JPopupMenu();
			
			JMenuItem expand = new JMenuItem("Expand Range");
			
			expand.addActionListener(new ActionListener() {
			
				public void actionPerformed(ActionEvent e) {
					mySlider.setMaximum(mySlider.getMaximum()*2);
				}
			});
	
			JMenuItem reset = new JMenuItem("Reset Range");
			
			reset.addActionListener(new ActionListener() {
			
				public void actionPerformed(ActionEvent e) {
					mySlider.setMinimum(Integer.parseInt(myDataField.getMinAllowedValue()));
					mySlider.setMaximum(Integer.parseInt(myDataField.getMaxAllowedValue()));
				}
			});
	
			
			
			popup.add(expand);
			popup.add(reset);
			
			mySlider.add(popup);
					
			myCheckBox = new JCheckBox("Filter by "+myDataField.getDisplayName(),false);
			myCheckBox.addActionListener(new ActionListener() {
			
				public void actionPerformed(ActionEvent e) {
					if (myCheckBox.isSelected()) {
						filterActive = true;
						enableAll();
					} else {
						filterActive = false;
						disableAll();
					}
					currentValue = mySlider.getValue();
					updateDisplayFields();
					stampLayer.updateVisibleStamps();
				}			
			});
			
			mySlider.addMouseListener(new MouseListener() {
			
				public void mouseReleased(MouseEvent e) {
				}
			
				public void mousePressed(MouseEvent e) {
				}
			
				public void mouseExited(MouseEvent e) {
				}
			
				public void mouseEntered(MouseEvent e) {
				}
			
				public void mouseClicked(MouseEvent e) {
					if (e.getButton()==MouseEvent.BUTTON3) {
						popup.show(mySlider, e.getX(),e.getY());
						e.consume();
						return;
					}
				}
			});
	
			minField = new JTextField("");
			minField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int min = Integer.parseInt(minField.getText());
					int max = getMaxValueToMatch();
					spread = (max - min)/2;
					currentValue = min + spread;
					
					updateDisplayFields();
				}
			});
	
			maxField = new JTextField("");
			maxField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int min = getMinValueToMatch();
					int max = Integer.parseInt(maxField.getText());
					
					spread = (max - min)/2;
					currentValue = min + spread;
					
					updateDisplayFields();
				}
			});
			
			spreadField = new JTextField(""+spread);
			spreadField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					spread=Integer.parseInt(spreadField.getText());
					updateDisplayFields();
					stampLayer.updateVisibleStamps();
				}
			});
	
			currentField = new JTextField(""+currentValue);
			currentField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					currentValue=Integer.parseInt(currentField.getText());
					mySlider.setValue(Integer.parseInt(currentField.getText()));
					updateDisplayFields();
				}
			});
			
			
	        focusSettings.setBorder(new EmptyBorder(4,4,4,4));
	        int row = 0;
	        int pad = 4;
	        Insets in = new Insets(pad,pad,pad,pad);
	        
	        focusSettings.add(myCheckBox, new GridBagConstraints(0,row,2,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
	        focusSettings.add(mySlider, new GridBagConstraints(2,row,2,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
	        row++;            	
	        
	        focusSettings.add(showBetween, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,in,pad,pad));
	        focusSettings.add(minField, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
	        focusSettings.add(and, new GridBagConstraints(2,row,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,in,pad,pad));
	        focusSettings.add(maxField, new GridBagConstraints(3,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
	        row++;
	        
	        focusSettings.add(showValues, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.EAST,GridBagConstraints.HORIZONTAL,in,pad,pad));
	        focusSettings.add(spreadField, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
	        focusSettings.add(from, new GridBagConstraints(2,row,1,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,in,pad,pad));
	        focusSettings.add(currentField, new GridBagConstraints(3,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
	        
	        row++;
	        
	        focusSettings.add(new JLabel(),new GridBagConstraints(0,row,2,1,1,1,GridBagConstraints.WEST,GridBagConstraints.BOTH,in,pad,pad));
	        
	        focusSettings.setBorder(new TitledBorder(myDataField.getDisplayName() + " filter"));
	        
	        disableAll();
	        
			}
		return focusSettings;
		
	}
	
	public int getMinValueToMatch() {
		return Math.max(minAllowedValue, currentValue - spread);
	}
	
	public int getMaxValueToMatch() {
		return Math.min(maxAllowedValue, currentValue + spread);
	}
	
	private void updateDisplayFields() {
		minField.setText(""+getMinValueToMatch());
		maxField.setText(""+getMaxValueToMatch());
		spreadField.setText(""+spread);
		currentField.setText(""+currentValue);
		mySlider.setValue(currentValue);		
	}
}
