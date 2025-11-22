package edu.asu.jmars.swing.sm.events;

import java.util.EventObject;
import fr.lri.swingstates.sm.StateMachine;

public class DeleteLine extends EventObject {
	
	private int ID = -1;

	public DeleteLine(StateMachine sm, int id) {
			super(sm);		
			this.ID = id;
	}
	
	public int getDeletedLineID()
	{
		return this.ID;
	}
}
