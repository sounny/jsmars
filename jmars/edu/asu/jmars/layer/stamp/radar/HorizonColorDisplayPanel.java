package edu.asu.jmars.layer.stamp.radar;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.swing.ColorCombo;

/**
 * A panel that is 9xn rows (currently 9x2 as of 11/22/2017), based
 * on the number of colors in a ColorCombo object.
 * Each color is represented with a checkbox and a colored panel next
 * to it.  This display panel is used to regulate which horizons
 * are displayed in the Lviews, based on color (regardless of which 
 * radargram they are from).
 */
public class HorizonColorDisplayPanel extends JPanel{
	
	private StampLView lview;
	
	public HorizonColorDisplayPanel(StampLView sLview){
		lview = sLview;
		
		//build layout
		setBorder(new TitledBorder("Display LView Horizons Based On Color"));
		setLayout(new GridBagLayout());
		
		int row = 0;
		int col = 0;
		int pad = 1;
		Insets in = new Insets(pad,pad,pad,pad);
		//create an IndexCheckPanel for each color in the colorcombo
		for(Color c : ColorCombo.getColorList()){
			IndexCheckPanel icp = new IndexCheckPanel(c);
			
			add(icp, new GridBagConstraints(col, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
			
			//create rows of 9 icps
			col++;
			if(col>8){
				col = 0;
				row++;
			}
		}
		
	}
	
	
	/**
	 * Each one of these panels contains a checkbox and a colored JPanel
	 * to display to the user.  It is used to toggle horizons on and off
	 * in the LView based on their color.
	 */
	class IndexCheckPanel extends JPanel{
		private JCheckBox chk;
		private Color color;
		private JPanel colorPnl;
		
		IndexCheckPanel(Color c){
			color = c;
			buildLayout();
		}
		
		private void buildLayout(){
			chk = new JCheckBox(checkboxAct);
			//set selected based on the settings object
			chk.setSelected(lview.getSettings().getHorizonColorDisplayMap().get(color));
			
			colorPnl = new JPanel();
			colorPnl.add(Box.createHorizontalStrut(15));
			colorPnl.setBackground(color);
			
			addMouseListener(new MouseListener() {
				public void mouseReleased(MouseEvent e) {
				}
				public void mousePressed(MouseEvent e) {
				}
				public void mouseExited(MouseEvent e) {
				}
				public void mouseEntered(MouseEvent e) {
				}
				public void mouseClicked(MouseEvent e) {
					//if the any of this panel is clicked, 
					// treat it like the checkbox was clicked
					chk.setSelected(!chk.isSelected());
					updateCheckBox();
				}
			});
			
			add(chk);
			add(colorPnl);
		}
		
		private AbstractAction checkboxAct = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				updateCheckBox();
			}
		};
		
		private void updateCheckBox(){
			lview.getSettings().getHorizonColorDisplayMap().put(color, chk.isSelected());
			lview.repaint();
		}
	}
}
