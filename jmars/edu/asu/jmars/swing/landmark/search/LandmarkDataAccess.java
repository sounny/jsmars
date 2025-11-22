package edu.asu.jmars.swing.landmark.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView.MarsFeature;

public class LandmarkDataAccess {
	private static List<MarsFeature> features = new ArrayList<>();

	public static List<MarsFeature> getLandmarks() {
		features.clear();
		for (LView view : LManager.getLManager().getViewList()) {
			if (view instanceof NomenclatureLView && view.isOverlay()) {
				features.addAll(((NomenclatureLView) view).getLandmarks());
				break;
			}
		}
		Collections.sort(features, new SortBasedOnLandmarkName());
		return features;
	}

	private static class SortBasedOnLandmarkName implements Comparator {
		@Override
		public int compare(Object o1, Object o2) {
			MarsFeature mf1 = (MarsFeature) o1;
			MarsFeature mf2 = (MarsFeature) o2;
			return mf1.name.compareToIgnoreCase(mf2.name);
		}
	}
}
