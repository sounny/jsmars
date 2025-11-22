package edu.asu.jmars.swing.landmark.search;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import edu.asu.jmars.layer.nomenclature.NomenclatureLView;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView.MarsFeature;
import edu.asu.jmars.parsers.gis.CoordinatesParser.Ordering;
import edu.asu.jmars.places.Place;
import edu.asu.jmars.places.XmlPlaceStore;
import edu.asu.jmars.util.Config;

public class BookmarksDataAccess {
	private static List<MarsFeature> bookmarks = new ArrayList<>();
	private static Map<MarsFeature, Place> mf_place = new HashMap<>();
	private static NomenclatureLView nomenclatureView = null;
	private static Comparator<MarsFeature> sorting = new SortBasedOnLandmarkName();

	public static List<MarsFeature> getBookmarks(NomenclatureLView nomView, XmlPlaceStore placeStore) {
		List<MarsFeature> books = new ArrayList<>();
		bookmarks.clear();
		mf_place.clear();

		nomenclatureView = nomView;

		if (nomenclatureView != null) {
			placeStore.reloadPlaces();
			loadBookmarks(placeStore);
		}

		Collections.sort(bookmarks, sorting);
		books.addAll(bookmarks);
		return books;
	}
	
	public static NomenclatureLView nomenclatureView() {
		return nomenclatureView;
	}

	private static void loadBookmarks(XmlPlaceStore placeStore) {
		Iterator<Place> it = placeStore.iterator();
		List<String> labels = new ArrayList<>();
		StringBuilder strLabels = new StringBuilder();
		while (it.hasNext()) {
			Place place = it.next();
			MarsFeature mf = nomenclatureView.new MarsFeature();
			labels.clear();
			labels.addAll(place.getLabels());
			strLabels.setLength(0);
			for (String label : labels) {
				strLabels.append(label);
				strLabels.append(StringUtils.SPACE);
			}
			if (strLabels.length() > 0) {
				mf.landmarkType = strLabels.toString();
			}
			mf.name = place.getName();
			Point2D lonlat = place.getLonLat();
			mf.longitude = lonlat.getX();
			mf.latitude = lonlat.getY();
			mf.collectAllInfo();
			
			//add additional Place data to "mf.everything"
			Point2D ll = place.getLonLat();
			Point2D proj = place.getProjCenterLonLat();
			String placeMsg = getCoordOrdering().formatPlace(ll, place, proj);	
			
			mf.everything = mf.everything + StringUtils.SPACE + placeMsg;
			
			bookmarks.add(mf);
			mf_place.put(mf, place);
		}
	}
	
	public static Place of(MarsFeature mf) {
		Place pl = null;
		pl = mf_place.get(mf);
		return pl;
	}
	
	public static Place of(String bookmarkname) {
		Place pl = null;
		for (int i = 0; i < bookmarks.size(); i++) {
			MarsFeature mf = (MarsFeature) bookmarks.get(i);
			if (mf.name.trim().equals(bookmarkname.trim())) {
				pl = mf_place.get(mf);
				break;
			}
		}
		return pl;
	}
	
	public static MarsFeature of(Place place) {
		MarsFeature mf = null;
		for (Entry<MarsFeature, Place> entry : mf_place.entrySet()) {
			if (entry.getValue().equals(place)) {
				mf = entry.getKey();
				break;
			}
		}
		return mf;
	}

	private static class SortBasedOnLandmarkName implements Comparator<MarsFeature> {
		@Override
		public int compare(MarsFeature o1, MarsFeature o2) {
			MarsFeature mf1 = (MarsFeature) o1;
			MarsFeature mf2 = (MarsFeature) o2;
			return mf1.name.compareToIgnoreCase(mf2.name);
		}
	}
	
	private static Ordering getCoordOrdering() {
		String coordOrdering = Config.get(Config.CONFIG_LAT_LON,Ordering.LAT_LON.asString());
		Ordering ordering = Ordering.get(coordOrdering);
		return ordering;
	}	 	
}
