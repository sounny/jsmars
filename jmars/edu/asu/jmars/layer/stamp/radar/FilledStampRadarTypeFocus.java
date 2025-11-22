package edu.asu.jmars.layer.stamp.radar;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.MouseInputAdapter;

import edu.asu.jmars.layer.stamp.FilledStamp;
import edu.asu.jmars.layer.stamp.StampImage;
import edu.asu.jmars.layer.stamp.StampImageFactory;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampShape;
import edu.asu.jmars.layer.stamp.focus.FilledStampFocus;
import edu.asu.jmars.util.Util;

public class FilledStampRadarTypeFocus extends FilledStampFocus{

	public FilledStampRadarTypeFocus(StampLView parent) {
		super(parent);
		
		listStamps.addMouseListener(radarMouseAdapter);
		
		listStamps.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}
	
	private MouseInputAdapter radarMouseAdapter = new MouseInputAdapter() {
		public void mouseClicked(MouseEvent e) {
			int selectedIndex = listStamps.locationToIndex(new Point(e.getX(), e.getY()));
			FilledStampRadarType fs = (FilledStampRadarType)getFilled(selectedIndex);
			parent.getFocusPanel().getRadarView().setFullResImage(fs.getFullResImage(), true);
			parent.getFocusPanel().getRadarView().setHorizons(fs.getHorizons());
		}
	};
	
	
	
	protected FilledStamp getFilled(StampShape s, FilledStamp.State state, String type){
    	if (type==null && state!=null) {
    		type = state.getImagetype();
    	}
    	
        StampImage pdsi = StampImageFactory.load(s, stampLayer.getSettings().getInstrument(), type);
            
        if (pdsi == null){
           return  null;
        }
		
		return new FilledStampRadarType(s, pdsi, state);
	}
	
	public void addStamp(StampShape s, String type, FilledStampRadarType.State state){
		Enumeration stamps = listModel.elements();
	    
    	while (stamps.hasMoreElements()) {
    		FilledStamp stamp = (FilledStamp)stamps.nextElement();
    		if (stamp.stamp==s && stamp.pdsi.getImageType().equalsIgnoreCase(type)) {
    			return;        			
    		}
    	}
    	
        FilledStamp fs = getFilled(s, state, type);
        
        if (fs == null || fs.pdsi == null){
        	Util.showMessageDialog(
        			"Unable to load " + s, "PDS LOAD ERROR",
        			JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        listModel.insertElementAt(fs, 0);

        listStamps.addSelectionInterval(0, 0);
        
        if (!stampLayer.isSelected(s)) {
        	stampLayer.addSelectedStamp(s);
        }
        
        enableEverything();
        
        redrawTriggered();
	}

	public FilledStampRadarType getFilledStamp(){
		return (FilledStampRadarType)getFilledSingle();
	}
	
	public void selectionsChanged(){
		super.selectionsChanged();
		
		FilledStampRadarType fs = getFilledStamp();
		if(fs!=null){
			parent.getFocusPanel().getRadarView().setFullResImage(fs.getFullResImage(), true);
			parent.getFocusPanel().getRadarView().setHorizons(fs.getHorizons());
		}
	}
}
