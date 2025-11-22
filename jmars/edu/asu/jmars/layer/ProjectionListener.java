package edu.asu.jmars.layer;

import java.util.EventListener;

public interface ProjectionListener extends EventListener
 {
	public void projectionChanged(ProjectionEvent e);
 }
