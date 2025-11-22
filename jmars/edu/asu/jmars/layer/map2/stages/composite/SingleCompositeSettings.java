package edu.asu.jmars.layer.map2.stages.composite;

import java.io.IOException;
import java.io.ObjectInputStream;

import edu.asu.jmars.layer.map2.AbstractStageSettings;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.layer.map2.stages.DummyStageView;

public class SingleCompositeSettings extends AbstractStageSettings {
	public static final String stageName = "Single";

	public Stage createStage() {
		return new SingleComposite(this);
	}

	public StageView createStageView() {
		return new DummyStageView(this);
	}

	public String getStageName() {
		return stageName;
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
}
