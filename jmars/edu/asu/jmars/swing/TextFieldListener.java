package edu.asu.jmars.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public abstract class TextFieldListener implements FocusListener, ActionListener {

	public void focusGained(FocusEvent e) {
		// No logic here
	}

	public void focusLost(FocusEvent e) {
		updateEvent();
	}

	public void actionPerformed(ActionEvent e) {
		updateEvent();
	}

	public abstract void updateEvent();
}
