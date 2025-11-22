package edu.asu.jmars.layer.map2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.asu.jmars.layer.map2.stages.composite.CompositeStage;
import edu.asu.jmars.layer.map2.stages.composite.HSVCompositeSettings;
import edu.asu.jmars.layer.map2.stages.composite.NoCompositeSettings;
import edu.asu.jmars.layer.map2.stages.composite.RGBCompositeSettings;
import edu.asu.jmars.layer.map2.stages.composite.SingleCompositeSettings;

/**
 * Factory of Composition Pipeline Stages.
 */
public class CompStageFactory {
	private static CompStageFactory instance;

	private List<CompositeStage> compStages;
	private Map<String,CompositeStage> stagesByName;
	
	protected CompStageFactory(){
		compStages = new ArrayList<CompositeStage>();
		compStages.add((CompositeStage)new SingleCompositeSettings().createStage());
		compStages.add((CompositeStage)new NoCompositeSettings().createStage());
		compStages.add((CompositeStage)new RGBCompositeSettings().createStage());
		compStages.add((CompositeStage)new HSVCompositeSettings().createStage());
		
		stagesByName = new HashMap<String,CompositeStage>();
		for(CompositeStage s: compStages) {
			stagesByName.put(s.getStageName(), s);
		}
	}
	
	public static CompStageFactory instance(){
		if (instance == null){
			instance = new CompStageFactory();
		}
		
		return instance;
	}
	
	public String[] getSupportedNames() {
		List<String> names = new LinkedList<String>();
		for (CompositeStage stage: compStages) {
			names.add(stage.getStageName());
		}
		return (String[])names.toArray(new String[0]);
	}
	
	/**
	 * Retruns stage instance given the input labels and the stage name.
	 * @return A Stage object on success, or null on failure.
	 */
	public CompositeStage getStageInstance(String name) {
		if (name.equals(RGBCompositeSettings.stageName))
			return (CompositeStage)(new RGBCompositeSettings()).createStage();
		else if (name.equals(HSVCompositeSettings.stageName))
			return (CompositeStage)(new HSVCompositeSettings()).createStage();
		else if (name.equals(SingleCompositeSettings.stageName))
			return (CompositeStage)(new SingleCompositeSettings()).createStage();
		else if (name.equals(NoCompositeSettings.stageName))
			return (CompositeStage)(new NoCompositeSettings()).createStage();
		
		return null;
	}
	
	public List<CompositeStage> getCompStages(){
		return Collections.unmodifiableList(compStages);
	}
	
	public CompositeStage getStageByName(String stageName){
		return (CompositeStage)stagesByName.get(stageName);
	}
	
	/**
	 * Compares Stages by their names.
	 */
	public static class StageComparatorByName implements Comparator<CompositeStage> {
		public int compare(CompositeStage s1, CompositeStage s2) {
			if (s1 == null && s2 != null)
				return 1;
			if (s1 != null && s2 == null)
				return -1;
			return s1.getStageName().compareTo(s2.getStageName());
		}
	}
	
}

