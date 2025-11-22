package edu.asu.jmars.swing.landmark.search.popup;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView;
import edu.asu.jmars.swing.landmark.search.LandmarkSearchPanel;

public class CommandReceiver {

	public CommandReceiver() {
	}

	public void bookmarkCurrentPlace() {
		Main.places.editBookmark();
	}

	public void exploreNomenclature() {
		for (LView view : LManager.getLManager().getViewList()) {
			if (view instanceof NomenclatureLView && view.isOverlay()) {
				view.getFocusPanel().showInFrame();
				break;
			}
		}
	}

	public void searchPlaces() {
		Main.testDriver.locMgr.initSearch();
		LandmarkSearchPanel.searchBookmarks();
	}
	
	public void searchLandmarks() {
		Main.testDriver.locMgr.initSearch();
		LandmarkSearchPanel.searchLandmarks();
	}	
}

