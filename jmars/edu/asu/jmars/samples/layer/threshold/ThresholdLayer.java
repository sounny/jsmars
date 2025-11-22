package edu.asu.jmars.samples.layer.threshold;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.map2.MyVicarReaderWriter;
import edu.asu.jmars.samples.layer.map2.stages.threshold.ThresholdSettings;
import edu.asu.jmars.util.Config;

/**
 * Example of a layer that makes use of thresholding.
 */
public class ThresholdLayer extends Layer {
	String imageFileName;
	BufferedImage image;
	ThresholdSettings thresholdSettings;
	
	public ThresholdLayer() {
		try {
			imageFileName = Config.get("samples.threshold.mola64", "resources/mola64.png");
			image = ImageIO.read(new File(imageFileName));
			thresholdSettings = new ThresholdSettings();
		}
		catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}
	
	public ThresholdSettings getThresholdSettings(){
		return thresholdSettings;
	}
	
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
		broadcast(new LayerData(imageFileName, image));
	}

	/*
	 * Data that the layer distributes in response to receiving a data request.
	 */
	public static class LayerData {
		final public String name;
		final public BufferedImage image;
		
		public LayerData(String name, BufferedImage image){
			this.name = name;
			this.image = image;
		}
	}
}
