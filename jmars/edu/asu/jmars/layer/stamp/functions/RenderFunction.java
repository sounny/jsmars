package edu.asu.jmars.layer.stamp.functions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.SwingUtilities;

import edu.asu.jmars.layer.stamp.FilledStamp;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.StampShape;
import edu.asu.jmars.layer.stamp.focus.FilledStampFocus;

public class RenderFunction implements ActionListener {
	
	private StampLayer stampLayer;
	private FilledStampFocus filledStampFocus;
	private String imageType;
	
	public RenderFunction(StampLayer newLayer, FilledStampFocus newFilledFocus, String newType) {
		stampLayer=newLayer;
		filledStampFocus=newFilledFocus;
		imageType=newType;		
	}
	
	public void actionPerformed(ActionEvent e) {
	    Runnable runme = new Runnable() {
	        public void run() {
				List<StampShape> selectedStamps = stampLayer.getSelectedStamps();
	        	
	        	StampShape stampsToAdd[] = new StampShape[selectedStamps.size()];
	        	String types[] = new String[selectedStamps.size()];
	        	FilledStamp.State states[] = new FilledStamp.State[selectedStamps.size()];
	        	
	        	int cnt=0;
	        	
	        	for (StampShape stamp : selectedStamps) {
	        		stampsToAdd[cnt]=stamp;
	        		states[cnt]=null;
	        		
	        		List validTypes=stamp.getSupportedTypes();

	        		if (imageType.equalsIgnoreCase("ABR / BTR")) {
	        			if (stamp.getId().startsWith("I")) {
	        				types[cnt]="BTR";
	        			} else {
	        				types[cnt]="ABR";
	        			}
	        		} else if (!validTypes.contains(imageType)) {
	        			stampsToAdd[cnt]=null;
	        			types[cnt]=null;
	        			continue;	        			
	        		}else {								        			
						types[cnt]=imageType;
	        		}
	        			        		
	        		cnt++;
	        	}
	        	
	        	filledStampFocus.addStamps(stampsToAdd, states, types);
	        	
	        }
	    };
	
	    SwingUtilities.invokeLater(runme);
	}
}
