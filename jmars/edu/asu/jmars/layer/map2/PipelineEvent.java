package edu.asu.jmars.layer.map2;

/** Describes a change in a processing pipeline */
public class PipelineEvent {
	private static final long serialVersionUID = 1L;
	public final PipelineProducer source;
	/**
	 * If true, the user has deliberately changed the pipeline, otherwise an
	 * automated change has occurred.
	 */
	public final boolean userInitiated;
	/**
	 * If true, the change is limited to a stage settings change, otherwise
	 * there may have been structural modification and/or settings changes and
	 * any operations that rely on either should reprocess the entire pipeline.
	 */
	public final boolean settingsChange;
	/**
	 * @param source
	 *            The source of the pipeline event, capable of creating a
	 *            duplicate of the pipeline for the receiver's use
	 * @param userInitiated
	 *            If true, the user has deliberately changed the pipeline,
	 *            otherwise an automated change has occurred.
	 * @param settingsChange
	 *            If true, the change is limited to a stage settings change,
	 *            otherwise there may have been structural modification and/or
	 *            settings changes and any operations that rely on either should
	 *            reprocess the entire pipeline.
	 */
	public PipelineEvent(PipelineProducer source, boolean userInitiated, boolean settingsChange) {
		this.source = source;
		this.userInitiated = userInitiated;
		this.settingsChange = settingsChange;
	}
}
