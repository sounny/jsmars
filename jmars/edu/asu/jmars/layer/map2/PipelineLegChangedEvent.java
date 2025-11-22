package edu.asu.jmars.layer.map2;

import java.util.EventObject;

public class PipelineLegChangedEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	public PipelineLegChangedEvent(Object source) {
		super(source);
	}
}
