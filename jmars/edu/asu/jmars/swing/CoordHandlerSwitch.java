package edu.asu.jmars.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import edu.asu.jmars.parsers.gis.CoordinatesParser.Ordering;
import edu.asu.jmars.util.Config;
import java.util.Observable;
import java.util.Observer;



public class CoordHandlerSwitch implements ActionListener {

	static CoordSwitcObservable observable = null;

	public CoordHandlerSwitch() {
		observable = new CoordSwitcObservable();
	}


	public void addObserver(Observer observer) {
		observable.addObserver(observer);		
	}	

	public void actionPerformed(ActionEvent ev) {
		String selectedorder = ev.getActionCommand();
		if (Ordering.LAT_LON.getOrderingLabel().equals(selectedorder))
			switchCoordOrder(Ordering.LAT_LON);
		else
			switchCoordOrder(Ordering.LON_LAT);
	}

	private void switchCoordOrder(Ordering ordering) {
		// persist user choice in Config
		Config.set(Config.CONFIG_LAT_LON, ordering.asString());
		observable.changeData(ordering);	
	}

public	class CoordSwitcObservable extends Observable {

		CoordSwitcObservable() {
			super();
		}

		void changeData(Object data) {
			setChanged();
			notifyObservers(data);
		}
	}
}
