package edu.asu.jmars.layer.map2.msd;

public interface PipelineLegModelListener {
	public void stagesAdded(PipelineLegModelEvent e);
	public void stagesRemoved(PipelineLegModelEvent e);
	public void stagesReplaced(PipelineLegModelEvent e);
	public void stageParamsChanged(PipelineLegModelEvent e);
}
