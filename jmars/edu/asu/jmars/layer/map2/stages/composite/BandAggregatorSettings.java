package edu.asu.jmars.layer.map2.stages.composite;


import edu.asu.jmars.layer.map2.AbstractStageSettings;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.layer.map2.stages.DummyStageView;

public class BandAggregatorSettings extends AbstractStageSettings {
	public static final String stageName = "Aggregator";
	public static final String PROP_INPUT_COUNT = "inputCount";
	
	private int numInputs;
	private String[] inputNames;

	public BandAggregatorSettings(int inputCount){
		this.numInputs = inputCount;
		this.inputNames = makeNames(this.numInputs);
	}
	
	public void setInputCount(int numInputs){
		this.numInputs = numInputs;
		this.inputNames = makeNames(this.numInputs);
		firePropertyChangeEvent(PROP_INPUT_COUNT, new Integer(numInputs), new Integer(this.numInputs));
	}

	public int getInputCount() {
		return numInputs;
	}

	public String getInputName(int inputNumber) {
		return inputNames[inputNumber];
	}

	public String[] getInputNames() {
		return inputNames;
	}
	
	private static String[] makeNames(int bandCount) {
		String[] names = new String[bandCount];
		for (int i = 0; i < names.length; i++)
			names[i] = "input_" + i;
		return names;
	}
	
	public Stage createStage() {
		return new BandAggregator(this);
	}

	public StageView createStageView() {
		return new DummyStageView(this);
	}

	public String getStageName() {
		return stageName;
	}

	public Object clone() throws CloneNotSupportedException {
		BandAggregatorSettings s = (BandAggregatorSettings)super.clone();
		s.inputNames = (String[])inputNames.clone();
		return s;
	}
}
