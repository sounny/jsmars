package edu.asu.jmars.layer.map2;

import java.util.EventListener;

public interface PipelineEventListener extends EventListener {
	public void pipelineEventOccurred(PipelineEvent e);
}
