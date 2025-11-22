package edu.asu.jmars.layer.investigate;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import edu.asu.jmars.Main;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.InvestigateDisplay;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;

public class DrawingListener implements MouseInputListener {
	InvestigateLView myLView;
	InvestigateLayer myLayer;
	
	public DrawingListener(InvestigateLView ilv){
		myLView = ilv;
		myLayer = (InvestigateLayer) myLView.getLayer();
	}
	

	public void mouseClicked(MouseEvent e) {
		//Only update the investigate layer if in investigate mode
		if(ToolManager.getToolMode()!=ToolManager.INVESTIGATE){
			return;
		}
		//Don't do anything if it's a right click
		if(SwingUtilities.isRightMouseButton(e)){
			return;
		}
		//Only save charts if they exist using single click
		if(e.getClickCount() == 1 && InvestigateDisplay.isDataSpike){
			
			//get point in spatial coords
			Point2D dataPoint = myLView.getProj().screen.toSpatial(e.getPoint());
			//default name is the size of the dataspike array
			String name = "Spike " + myLayer.getDataSpikes().size();
			//display naming dialog
			String newName = Util.showInputDialog("Would you like to name this dataspike? \nIf not, the default name for this " 
					+"spike will be '"+name+"'.", name);
			//if name is blank, don't continue
			if(newName==null || newName.equals("")){
				return;
			}
			
			//Cycle through names...make sure new name is unique
			for(DataSpike ds : myLayer.getDataSpikes()){
				if(newName.equals(ds.getName())){
					Util.showMessageDialog("Must enter a unique name for dataspike",
													"Name Duplicate Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
			//set name
			name = newName;

			//create new spike with information
			DataSpike spike = new DataSpike(InvestigateDisplay.getChart(), dataPoint, InvestigateDisplay.getInstance().getInvData(), name);	
			//add spike to arraylist of dataspikes
			myLayer.addDataSpike(spike);
			//create a new focus panel for this specific spike
			((InvestigateFocus)myLView.focusPanel).addSpikePanel(spike);
			
			myLView.repaint();
			if(ThreeDManager.isReady()){
				ThreeDManager.getInstance().updateDecalsForLView(myLView, true);
			}
			
		}
	}

	public void mousePressed(MouseEvent e) {
//		System.out.println("this is pressed");
		
	}

	public void mouseReleased(MouseEvent e) {
//		System.out.println("this is released");
		
	}

	public void mouseEntered(MouseEvent e) {
//		System.out.println("e");
		
	}

	public void mouseExited(MouseEvent e) {
//		System.out.println("e");
		
	}

	public void mouseDragged(MouseEvent e) {
//		System.out.println("e");
		
	}

	public void mouseMoved(MouseEvent e) {
//		System.out.println("e");
		
	}

}
