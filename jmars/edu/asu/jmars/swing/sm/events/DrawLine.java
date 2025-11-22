package edu.asu.jmars.swing.sm.events;

import java.awt.Graphics2D;
import java.util.EventObject;
import fr.lri.swingstates.sm.StateMachine;

public class DrawLine extends EventObject {
	
	private Graphics2D g2 = null;

	public DrawLine(StateMachine sm, Graphics2D graphics) {
			super(sm);		
			g2 = graphics;
	}
	
	public Graphics2D getDrawingGraphics()
	{
		return g2;
	}
}