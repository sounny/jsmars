package edu.asu.jmars.samples.layer.threshold;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.map2.MapChannel;
import edu.asu.jmars.layer.map2.MapChannelReceiver;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.Pipeline;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.stages.composite.SingleComposite;
import edu.asu.jmars.layer.map2.stages.composite.SingleCompositeSettings;
import edu.asu.jmars.samples.layer.map2.stages.threshold.ThresholdFocusPanel;
import edu.asu.jmars.samples.layer.map2.stages.threshold.ThresholdStage;
import edu.asu.jmars.util.Util;

public class ThresholdLView extends LView implements MapChannelReceiver {
	MapChannel ch;
	ThresholdStage thresholdStage;
	ThresholdFocusPanel focusPanel = null;
	
	public ThresholdLView(ThresholdLayer layer){
		super(layer);
		ch = new MapChannel(new Rectangle2D.Double(), 1, Main.PO, new Pipeline[0]);
		ch.addReceiver(this);
		thresholdStage = new ThresholdStage(((ThresholdLayer)getLayer()).getThresholdSettings());
		thresholdStage.getSettings().addPropertyChangeListener(new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent evt) {
				ch.reprocess();
			}
		});
	}
	
	public FocusPanel getFocusPanel() {
		if (focusPanel == null)
			focusPanel = new ThresholdFocusPanel(this);
		
		return focusPanel;
	}

	protected LView _new() {
		// Create a copy of ourself for use in the panner-view.
		return new ThresholdLView((ThresholdLayer)getLayer());
	}

	protected Object createRequest(Rectangle2D where) {
		// Clear the off screen buffer, it will be repainted when we receive data from the layer.
		clearOffScreen();
		// Build a request object for the layer.
		return where;
	}

	public void receiveData(Object layerData) {
		// Process the data returned by the layer.
		Rectangle2D viewExtent = getProj().getWorldWindow();
		double y1 = Math.max(-90, viewExtent.getMinY());
		double y2 = Math.min(90, viewExtent.getMaxY());
		viewExtent.setRect(viewExtent.getMinX(), y1, viewExtent.getWidth(), y2-y1);

		// Push the new view-extent, ppd and projection on the map channel.
		ch.setMapWindow(viewExtent, getProj().getPPD(), Main.PO);
		
		// Push a new pipeline onto the map channel based on new image data.
		ThresholdLayer.LayerData data = (ThresholdLayer.LayerData)layerData;
		Pipeline[] pipe = new Pipeline[]{
				new Pipeline(new StaticImageMapSource(data.image, data.name),
						new Stage[]{ thresholdStage },
						new SingleComposite(new SingleCompositeSettings()))
		};
		ch.setPipeline(pipe);
	}
	
	public String getName() {
		return "Threshold";
	}

	public void mapChanged(MapData mapData) {
		BufferedImage image = mapData.getImage();
		Graphics2D g2 = getOffScreenG2Raw();
		AffineTransform at = Util.image2world(image.getWidth(), image.getHeight(), mapData.getRequest().getExtent());
		g2.drawImage(image, at, null);
		repaint();
	}
}
