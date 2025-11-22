package edu.asu.jmars.layer.shape2.drawingpalette.swing;

import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JToggleButton;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.shape2.ShapeLView;
import edu.asu.jmars.layer.shape2.drawingpalette.DrawingPalette;
import edu.asu.jmars.layer.util.features.FeatureMouseHandler;


public class DrawAction extends AbstractAction {
	private AbstractButton actionButton = new JToggleButton(); 
	private String command;
	private static PaletteObservable observable = null;

	static {
		observable = new PaletteObservable();		
	}
	
	public DrawAction(AbstractButton btn, String text) {
		actionButton = btn;
		command = text;
	}
	
	public String commandAsString() {
		return this.command;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		 processSelect();
		 Object data = DrawActionEnum.get(this.command);
		 observable.changeData(data);	
	}
	
	private void processSelect() {
		if (this.command.equals(DrawActionEnum.SELECT.asString())) {
			if (actionButton.isSelected()) { // this means SELECT mode, no draw
				disableDrawGroup();
			} else {
				enableDrawGroup();
			}
		}
	}

	private void enableDrawGroup() {
		DrawingPalette.INSTANCE.enableDraw();
	}

	private void disableDrawGroup() {
		DrawingPalette.INSTANCE.deselectAllDraw();
	}

	public static class PaletteObservable extends Observable {

		PaletteObservable() {
			super();
		}

		void changeData(Object data) {
			setChanged();
			Observer observer = getActiveCustomShape();
			deleteObservers();  //assure only 1 observer-instance of FeatureMouseHandler per shape view
			if (observer != null) {
			    addObserver(observer);	
			    notifyObservers(data);
 			}
		}
		
		private Observer getActiveCustomShape() {
			FeatureMouseHandler fmh = null;
			Layer.LView shapeview = LManager.getLManager().getActiveLView();
			if (isCustomShapeInstance(shapeview)) {
				fmh = ((ShapeLView) shapeview).getFeatureMouseHandler();
			}
			return fmh;
		}

		private boolean isCustomShapeInstance(LView view) {
			boolean isInstance = false;
			if (view != null) {
				if (view instanceof ShapeLView && (view.getName().toLowerCase().contains("shape"))) {
					isInstance = true;
				}
			}
			return isInstance;
		}
	}
}
