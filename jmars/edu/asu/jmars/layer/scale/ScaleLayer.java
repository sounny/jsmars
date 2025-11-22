package edu.asu.jmars.layer.scale;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.MultiProjection;

public class ScaleLayer extends Layer {
	private final ScaleParameters parms;
	private Rectangle rulerBox;
	private ArrayList<Rectangle> tickBoxes = new ArrayList<Rectangle>();
	private MultiProjection proj = Main.testDriver.mainWindow.getProj();
	private String fontString;
	
	public ScaleLayer(ScaleParameters params) {
		this.parms = params;
	}

	public ScaleParameters getParameters(){
		return parms;
	}
	
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
	}
	
	public ArrayList<Rectangle> getTickBoxes(){
		return tickBoxes;
	}
	
	public Rectangle2D getRulerBox() {
		return rulerBox;
	}
	
	public void setRulerBox(Rectangle rect){
		rulerBox = rect;
	}
	
	public void addTickBox(Rectangle box){
		tickBoxes.add(box);
	}
	
	public void clearTickBoxes(){
		tickBoxes.clear();
	}
	
	public String getFontString(){
		return fontString;
	}
	
	public void setFontString(String label){
		fontString = label;
	}
}
