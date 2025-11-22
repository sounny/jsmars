package edu.asu.jmars.tool.strategy;

import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.shape2.ShapeLView;
import edu.asu.jmars.layer.shape2.drawingpalette.DrawingPalette;
import edu.asu.jmars.layer.shape2.drawingpalette.UserPrompt;
import edu.asu.jmars.lmanager.MainPanel;
import edu.asu.jmars.lmanager.Row;

public class ShapesToolStrategy implements ToolStrategy {
	
	private static boolean isNewCustomShapeLayer = false;

	@Override
	public void doMode(int newmode, int oldmode) {
		Map<Integer, LView> shapes = new HashMap<>();
		shapes = findLoadedCustomShapeLayers();
		if (shapes.isEmpty()) {
			askToLoadCustomShape(oldmode);
		} else {
			Layer.LView activecustomshapeview = findActiveCustomShapeView(shapes);
			if (activecustomshapeview == null) {
				promptUserToSelectCustomShape(oldmode, shapes);
			} else {
				showDrawingPaletteForView(activecustomshapeview);
			}
		}
	}
	
	@Override
	public void preMode(int newmode, int oldmode) {
		if (!isNewCustomShapeLayer) {
			resetDrawModeIfSELECT(newmode);
		}
		if (oldmode == ToolManager.SHAPES && newmode == ToolManager.SHAPES) {
			// this is request to see the palette, even if user closed "globally"
			DrawingPalette.INSTANCE.clearClose();
		}
	}

	@Override
	public void postMode(int newmode, int oldmode) {
	}
	
	public static void setToolModeWhenNewCustomShapeCreated() {
		isNewCustomShapeLayer = true;
		ToolManager.setToolMode(ToolManager.SEL_HAND);
		isNewCustomShapeLayer = false;
	}	
	
	
	private static void resetDrawModeIfSELECT(int newmode) {
		if (newmode == ToolManager.SEL_HAND) {
			DrawingPalette dp = DrawingPalette.INSTANCE;
			LView activeview = LManager.getLManager().getActiveLView();
			if (isCustomShapeInstance(activeview)) {
				dp.resetDrawMode(activeview);
			}
		}
	}
	
	private static boolean isCustomShapeInstance(LView view) {
		boolean isInstance = false;
		if (view != null) {
			if (view instanceof ShapeLView && (view.getName().toLowerCase().contains("shape"))) {
				isInstance = true;
			}
		}
		return isInstance;
	}

	private static void askToLoadCustomShape(int previousmode) {
		int returnVal = edu.asu.jmars.util.Util.showConfirmDialog(
				UserPrompt.ADD_CUSTOMSHAPE_LAYER.asString(),
				"Add Custom Shape Layer", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		switch (returnVal) {
		case JOptionPane.YES_OPTION:
			edu.asu.jmars.swing.quick.add.layer.CommandReceiver.addCustomShapesLayer();
			ToolManager.setToolMode(ToolManager.SHAPES);
			break;
		case JOptionPane.NO_OPTION:
			ToolManager.setToolMode(previousmode);
			hideDrawingPalette();
			break;
		default:
			break;
		}
	}	

	private static void promptUserToSelectCustomShape(int previousmode, Map<Integer, LView> shapes2) {
		edu.asu.jmars.util.Util.showConfirmDialog(UserPrompt.SELECT_CUSTOMSHAPE_TO_DRAW.asString(),
				"Select 'Custom Shape' layer", JOptionPane.PLAIN_MESSAGE);
		ToolManager.setToolMode(previousmode); // do not change toolmode
		hideDrawingPalette();
	}

	private static void hideDrawingPalette() {
		DrawingPalette.INSTANCE.hide();
	}

	
	private static Layer.LView findActiveCustomShapeView(Map<Integer, LView> shapes) {
		Layer.LView activecustomshapeview = null;
		LView activeview = LManager.getLManager().getActiveLView();
		for (Map.Entry<Integer, LView> entry : shapes.entrySet()) {
			// check if any of these custom shape layers is active then show palette
			if (activeview == entry.getValue()) {
				activecustomshapeview = activeview;
				if (!activeview.isVisible()) {
					promptWhenActiveButNotVisisble(activeview);
				}
				break;
			}
		}
		return activecustomshapeview;
	}

	private static void showDrawingPaletteForView(Layer.LView view) {
		DrawingPalette.INSTANCE.show(view);
	}

	public static void promptWhenActiveButNotVisisble(LView activeview) {
		MainPanel mainpanel = LManager.getLManager().getMainPanel();
		Row rowpanel = (Row) mainpanel.getComponentForLView(activeview);
		if (rowpanel != null) {
			JButton btnM = rowpanel.getM();
			if (btnM != null) {
				ToolManager.showCallout(btnM, UserPrompt.ACTIVE_NOT_VISIBLE.asString(), UserPrompt.LONG_TIME);
			}
		}
	}

	private static Map<Integer, LView> findLoadedCustomShapeLayers() {
		Map<Integer, LView> shapes = new HashMap<>();
		for (LView lv : LManager.getLManager().viewList) {
			if (isCustomShapeInstance(lv)) {
				int idx = LManager.getLManager().viewList.indexOf(lv);
				if (idx != -1) {
					shapes.put(idx, lv);
				}
			}
		}
		return shapes;
	}

}
