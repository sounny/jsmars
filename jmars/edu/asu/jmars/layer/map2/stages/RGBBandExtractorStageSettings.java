package edu.asu.jmars.layer.map2.stages;

import edu.asu.jmars.layer.map2.Stage;

public class RGBBandExtractorStageSettings extends BandExtractorStageSettings {
	public static final String stageName = "RGB Band Extractor";
	public static final String[] bandNames = new String[]{ "Red", "Green", "Blue" };

	public RGBBandExtractorStageSettings(String selectedBand){
		super(bandNames, bandName(selectedBand));
	}

	private static String bandName(String inputName){
		if (inputName.toLowerCase().startsWith("r"))
			return bandNames[0];
		if (inputName.toLowerCase().startsWith("g"))
			return bandNames[1];
		if (inputName.toLowerCase().startsWith("b"))
			return bandNames[2];
		return null;
	}
	
	public Stage createStage(){
		return new RGBBandExtractorStage(this);
	}
	
	public String getStageName(){
		return stageName;
	}
}
