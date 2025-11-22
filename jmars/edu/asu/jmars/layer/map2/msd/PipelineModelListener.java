package edu.asu.jmars.layer.map2.msd;

public interface PipelineModelListener {
	public void compChanged(PipelineModelEvent e);
	public void childrenAdded(PipelineModelEvent e);
	public void childrenRemoved(PipelineModelEvent e);
	public void childrenChanged(PipelineModelEvent e);
	public void forwardedEventOccurred(PipelineModelEvent e);
}

