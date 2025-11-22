package edu.asu.jmars.layer.map2.stages;

import edu.asu.jmars.layer.map2.Stage;

public class HSBBandExtractorStageSettings extends BandExtractorStageSettings {
	public static final String stageName = "HSB Band Extractor";
	public static final String[] bandNames = new String[]{ "Hue", "Saturation", "Value" };

	public HSBBandExtractorStageSettings(String selectedBand){
		super(bandNames, bandName(selectedBand));
	}

	private static String bandName(String inputName){
		if (inputName.toLowerCase().startsWith("h"))
			return bandNames[0];
		if (inputName.toLowerCase().startsWith("s"))
			return bandNames[1];
		if (inputName.toLowerCase().startsWith("v") || inputName.toLowerCase().startsWith("b") || inputName.toLowerCase().startsWith("i"))
			return bandNames[2];
		return null;
	}

	public Stage createStage(){
		return new HSBBandExtractorStage(this);
	}
	
	public String getStageName(){
		return stageName;
	}
}
