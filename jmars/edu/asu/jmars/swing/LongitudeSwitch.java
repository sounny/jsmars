package edu.asu.jmars.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import edu.asu.jmars.parsers.gis.CoordinatesParser.LongitudeSystem;
import edu.asu.jmars.util.Config;


public class LongitudeSwitch implements ActionListener {
	static LongitudeSwitchObservable observable = null;

	public LongitudeSwitch() {
		observable = new LongitudeSwitchObservable();
	}

	public void addObserver(Observer observer) {
		observable.addObserver(observer);
	}

	public void actionPerformed(ActionEvent ev) {
		String selectedlon = ev.getActionCommand();
		for (LongitudeSystem lonsystem : LongitudeSystem.values()) {
			if (lonsystem.getCoordinatesRange().equalsIgnoreCase(selectedlon)) {
				switchLongitudeCoordSystem(lonsystem);
			}
		}

	}

	private void switchLongitudeCoordSystem(LongitudeSystem lonsystem) {
		// persist user choice in Config
		Config.set(Config.CONFIG_LON_SYSTEM, lonsystem.getName());
		observable.changeData(lonsystem);
	}

	public class LongitudeSwitchObservable extends Observable {

		LongitudeSwitchObservable() {
			super();
		}

		void changeData(Object data) {
			setChanged();
			notifyObservers(data);
		}
	}

}
