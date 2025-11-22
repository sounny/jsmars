package edu.asu.jmars.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import edu.asu.jmars.parsers.gis.CoordinatesParser.LatitudeSystem;
import edu.asu.jmars.util.Config;


public class LatitudeSwitch implements ActionListener {
	static LatitudeSwitchObservable observable = null;

	public LatitudeSwitch() {
		observable = new LatitudeSwitchObservable();
	}

	public void addObserver(Observer observer) {
		observable.addObserver(observer);
	}

	public void actionPerformed(ActionEvent ev) {
		String selectedlat = ev.getActionCommand();
		for (LatitudeSystem latsystem : LatitudeSystem.values()) {
			if (latsystem.getName().equalsIgnoreCase(selectedlat)) {
				switchLatitudeCoordSystem(latsystem);
			}
		}
	}

	private void switchLatitudeCoordSystem(LatitudeSystem latsystem) {
		// persist user choice in Config
		Config.set(Config.CONFIG_LAT_SYSTEM, latsystem.getName());
		observable.changeData(latsystem);
	}

	public class LatitudeSwitchObservable extends Observable {

		LatitudeSwitchObservable() {
			super();
		}

		void changeData(Object data) {
			setChanged();
			notifyObservers(data);
		}
	}

}

