package edu.asu.jmars.layer.map2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.asu.jmars.layer.map2.stages.ColorStretcherStageSettings;
import edu.asu.jmars.layer.map2.stages.ContourStageSettings;
import edu.asu.jmars.layer.map2.stages.GrayscaleStageSettings;
import edu.asu.jmars.layer.map2.stages.LowPassFilterStageSettings;
import edu.asu.jmars.layer.map2.stages.ShadeStageSettings;
import edu.asu.jmars.layer.map2.stages.SigmaStretchStageSettings;
import edu.asu.jmars.layer.map2.stages.ThresholdSettings;

/**
 * Factory to create instances of Stages.
 */
public final class StageFactory {
	static StageFactory instance;
	private final Map<String,Stage> singleIOStages;
	
	public StageFactory() {
		singleIOStages = new LinkedHashMap<String,Stage>();
		singleIOStages.put(ColorStretcherStageSettings.stageName,(new ColorStretcherStageSettings()).createStage());
		singleIOStages.put(GrayscaleStageSettings.stageName,(new GrayscaleStageSettings()).createStage());
		singleIOStages.put(ContourStageSettings.stageName,(new ContourStageSettings()).createStage());
		singleIOStages.put(ThresholdSettings.stageName,(new ThresholdSettings()).createStage());
		singleIOStages.put(ShadeStageSettings.stageName,(new ShadeStageSettings()).createStage());
		singleIOStages.put(LowPassFilterStageSettings.stageName,(new LowPassFilterStageSettings()).createStage());
		singleIOStages.put(SigmaStretchStageSettings.stageName,(new SigmaStretchStageSettings()).createStage());
	}
	
	/**
	 * Returns an unmodifiable list of single input single output
	 * Stages.
	 */
	public List<Stage> getAllSingleIOStages() {
		return Collections.unmodifiableList(new ArrayList<Stage>(singleIOStages.values()));
	}
	
	/**
	 * Returns a new instance of a Stage given the stage name.
	 * The stage name comes from the static class variable from
	 * within the stages. Only a handful of single input/output 
	 * stages are supported at this time.
	 */
	public Stage getStageInstance(String name) {
		Stage returnStage = singleIOStages.get(name);
		return returnStage;
	}
}
