package edu.asu.jmars.layer.map2.stages.composite;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import edu.asu.jmars.layer.map2.MapAttr;

public class NoComposite extends CompositeStage implements Cloneable, Serializable {
	private static final long serialVersionUID = 2L;
	
	public NoComposite(NoCompositeSettings settings){
		super(settings);
	}
	
	public BufferedImage makeBufferedImage(int width, int height) {
		return null;
	}

	public MapAttr[] consumes(int inputNumber){
		return new MapAttr[0];
	}
	
	public MapAttr produces() {
		return null;
	}
	
	public String getStageName(){
		return getSettings().getStageName();
	}
	
	public int getInputCount(){
		return 0;
	}
	
	public String getInputName(int inputNumber){
		throw new UnsupportedOperationException();
	}
	
	public String[] getInputNames(){
		return new String[0];
	}
	
	public Object clone() throws CloneNotSupportedException {
		NoComposite stage = (NoComposite)super.clone();
		return stage;
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
}

