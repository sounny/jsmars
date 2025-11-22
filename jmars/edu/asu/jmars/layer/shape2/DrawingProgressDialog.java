package edu.asu.jmars.layer.shape2;

import java.awt.Component;
import java.awt.Frame;

import javax.swing.*;
import edu.asu.jmars.util.DebugLog;

public class DrawingProgressDialog {
	private static DebugLog log = DebugLog.instance();
	private volatile static long count = 1;
	
	JProgressBar pm = new JProgressBar();
	long timeToPopup = 500L;
	boolean hidden = false;
	JDialog window = null;
	long myid = count++;
	Frame ownerFrame;
	Component centerOn;
	
	/**
	 * Constructs a ProgressDialog based on the specified parameters.
	 * 
	 * @param ownerFrame Onwer frame of the window containing the progress bar.
	 * @param centerOn Component to center the progress bar on.
	 * @param timeToPopup Milliseconds to wait before popping up the progress dialog.
	 */
	public DrawingProgressDialog(Frame ownerFrame, Component centerOn, long timeToPopup){
		this.ownerFrame = ownerFrame;
		this.centerOn = centerOn;
		this.timeToPopup = timeToPopup;
		
		pm.setMinimum(0);
		pm.setIndeterminate(true);
		pm.setStringPainted(true);
		pm.setString("");
	}
	
	public void setMinimum(int min){
		pm.setMinimum(min);
	}
	public void setMaximum(int max){
		pm.setMaximum(max);
		pm.setIndeterminate(false);
		pm.setString(null);
	}

	public int getMinimum(){
		return pm.getMinimum();
	}
	
    public int getMaximum(){
    	return pm.getMaximum();
    }

    public void setValue(int i){
    	pm.setValue(i);
    }

    public void incValue(){
	    pm.setValue(pm.getValue()+1);
    }

    public int getValue(){
    	return pm.getValue();
    }
    
    public void show(){
    	if (window == null) {
    		window = new JDialog(ownerFrame); // Main.mainFrame
    		window.setUndecorated(true);
    		window.setContentPane(pm);
    		window.pack();
    		window.setLocationRelativeTo(centerOn); // Main.testDriver.mainWindow
    	}
    	if (!window.isVisible()) {
    		window.setVisible(true);
    	}
    }
    public void hide(){
    	log.println(getClass().getName()+"["+myid+"] hide() called");
    	hidden = true;
    	if (window == null){
    		log.println(getClass().getName()+"["+myid+"] hide() called with null window");
    		return;
    	}
    	log.println(getClass().getName()+"["+myid+"] hide() actually hiding window");
    	window.setVisible(false);
    }
}
    


