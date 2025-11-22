package edu.asu.jmars.places;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeSet;
import org.apache.commons.text.StringEscapeUtils;
import com.thoughtworks.xstream.XStream;
import edu.asu.jmars.Main;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

/**
 * Care should be taken not to open more than one XmlPlaceStore, since each one
 * takes control of the data store.
 */
public class XmlPlaceStore extends AbstractSet<Place> implements PlaceStore {
	
	private static PlaceChangedObservable placechangedobservable = null; 
	
	public static final Comparator<Place> defaultNameComparator = new Comparator<Place>() {
		public int compare(Place p1, Place p2) {
			return p1.getName().compareToIgnoreCase(p2.getName());
		}
	};
	private static final String PLACE_EXT = ".jpf";
	private static File placesRoot = new File(Main.getJMarsPath() + "places" + File.separatorChar + Util.toProperCase(Main.getCurrentBody()));//@since change bodies, add body to path and remove final so that it can be changed

	/**
	 * Update the places root after a body change
	 * @since body change
	 */
	public static void updatePlacesRoot() {
		placesRoot = new File(Main.getJMarsPath() + "places" + File.separatorChar + Util.toProperCase(Main.getCurrentBody()));
	}
	/** Returns the file for a given place */
	private static File getFile(Place place) {
		if (place.getName().contains(File.separator)) {
			throw new IllegalArgumentException("Name may not contain path separator '" + File.separatorChar + "'");
		}
		return new File(placesRoot.getAbsolutePath() + File.separatorChar + place.getName() + PLACE_EXT);
	}
	
	private DebugLog log = DebugLog.instance();
	private TreeSet<Place> places = new TreeSet<Place>(defaultNameComparator);
	
	public XmlPlaceStore() {
		super();
		placechangedobservable = new PlaceChangedObservable();
		load();
	}
	
	public void addObserver(Observer observer) {
		placechangedobservable.addObserver(observer);		
	}	
	
	public void setComparator(Comparator<Place> sorter) {
		TreeSet<Place> newTree = new TreeSet<Place>(sorter);
		newTree.addAll(places);
		places = newTree;
	}
	
	public Comparator<? super Place> getComparator() {
		return places.comparator();
	}
	
	public void reloadPlaces() {
		updatePlacesRoot();
		load();
	}
	
	private void load() {
		if (placesRoot.exists()) {
			if (!placesRoot.isDirectory()) {
				throw new IllegalStateException(
					placesRoot.getAbsolutePath() +
					" exists, but is not a directory, please rename and try creating places again.");
			}
		} else {
			placesRoot.mkdirs();
		}
		places.clear();
		XStream xstream = new XStream() {
			protected boolean useXStream11XmlFriendlyMapper() {
				return true;
			}
		};
		Place pl;
		String unescape;
		for (File placeFile: placesRoot.listFiles()) {
			if (placeFile.isFile() && placeFile.canRead() && placeFile.getName().endsWith(PLACE_EXT)) {
				try {
					pl = (Place)xstream.fromXML(new FileInputStream(placeFile));
					unescape = StringEscapeUtils.unescapeHtml4(pl.getName());
					pl.setName(unescape);
					convertToWestLeading(pl);
					places.add(pl);
				} catch (Exception e) {
					log.aprintln("Error reading place: " + placeFile);
					e.printStackTrace();
				}
			}
		}
	}
	
	private void convertToWestLeading(Place pl) {
		//we store Place coordinates in XML in East-leading, but need it in West-leading for internal JMARS use
		Point2D latlonfromxml = pl.getLonLat(); //east-leading
		Point2D latlonwest = Place.flipLon(latlonfromxml); //to west-leading
		pl.setLonLat(latlonwest);
		Point2D projfromxml = pl.getProjCenterLonLat(); //east-leading
		Point2D projwest =  Place.flipLon(projfromxml); //to west-leading
		pl.setProjCenterLonLat(projwest);
	}
	
	
	public Iterator<Place> iterator() {
		return new XmlPlaceIterator(places.iterator());
	}
	
	public boolean add(Place place) {	
		// remove the place if it already exists
		boolean has = places.contains(place);
		if (has) {
			try {
				removeFile(place);
				places.remove(place);
			} catch (RuntimeException e) {
				log.aprintln("Unable to update place " + place);
				throw e;
			}
		}
		// try to create the file, to make sure the name is acceptable
		File placeFile = getFile(place);
		try {
			if (!placeFile.createNewFile()) {
				throw new IllegalStateException("Couldn't create file for place " + place + ", bad filename?");
			}
		} catch (IOException e) {
			throw new IllegalStateException("Error saving place " + place, e);
		}
		// add the place
		XStream xstream = new XStream() {
			protected boolean useXStream11XmlFriendlyMapper() {
				return true;
			}
		};
		String placename;
		String escaped;
		try {
			FileOutputStream fos = new FileOutputStream(placeFile);		
			placename = place.getName();
			Point2D latlonwest = place.getLonLat();
			Point2D projwest = place.getProjCenterLonLat();
			escaped = StringEscapeUtils.escapeHtml4(placename);
			place.setName(escaped);
			convertToEastLeading(place);
			xstream.toXML(place, fos);
			place.setName(placename);
			place.setLonLat(latlonwest);
			place.setProjCenterLonLat(projwest);
			places.add(place);
			fos.close();
			placechangedobservable.changeData(place);	
		} catch (Exception e) {
			throw new IllegalStateException("Error saving place " + place, e);
		}
		return has;
	}
	
	private void convertToEastLeading(Place place) {
		// we store Place coordinates internally in west-leading in JMARS but need East-leading in XML
		Point2D latlonfromplace = place.getLonLat(); // west-leading
		Point2D latloneast = Place.flipLon(latlonfromplace); // to east-leading
		place.setLonLat(latloneast);
		Point2D projfromplace = place.getProjCenterLonLat(); // west-leading
		Point2D projeast = Place.flipLon(projfromplace); // to east-leading
		place.setProjCenterLonLat(projeast);
	}
	
	public int size() {
		return places.size();
	}
	
	/**
	 * The remove method ALSO removes the associated file from disk. If anything
	 * goes wrong, in-memory changes are discarded and this store is reloaded
	 * from disk.
	 */
	private void removeFile(Place place) {
		try {
			File placeFile = getFile(place);
			if (!placeFile.delete()) {
				throw new IllegalStateException("Unable to delete " + placeFile);
			}
			placechangedobservable.changeData(place);	
		} catch (RuntimeException e) {
			log.aprintln("Failure removing place, reloading from disk");
			load();
			throw e;
		}
	}

	/**
	 * Wraps a given place iterator, but defers the remove method to
	 * {@link XmlPlaceStore#removeFile(Place)}.
	 */
	private class XmlPlaceIterator implements Iterator<Place> {
		private final Iterator<Place> it;
		private Place last;
		public XmlPlaceIterator(Iterator<Place> it) {
			this.it = it;
		}
		public boolean hasNext() {
			return it.hasNext();
		}
		public Place next() {
			return last = it.next();
		}
		public void remove() {
			removeFile(last);
			it.remove();
			last = null;
		}
	}
	
	public class PlaceChangedObservable extends Observable {

		PlaceChangedObservable() {
			super();
		}

		void changeData(Object data) {
			setChanged();
			notifyObservers(data);
		}
	}	
}
