package edu.asu.jmars.layer.map2.stages;

import edu.asu.jmars.layer.map2.AbstractStageSettings;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.util.DebugLog;

public class BandExtractorStageSettings extends AbstractStageSettings {
	public static DebugLog log = DebugLog.instance();
	
	public static final String stageName = "Band Extractor";
	public static final String propBand = "band";
	
	private String[] bands;
	private String selectedBand;
	
	public BandExtractorStageSettings(String[] bands, String selectedBand){
		this.bands = (String[])bands.clone();
		this.selectedBand = selectedBand;
	}

	public BandExtractorStageSettings(int nBands, int selectedBand){
		this.bands = buildBandNames(nBands);
		this.selectedBand = bands[selectedBand];
	}
	
	public synchronized String getSelectedBand(){
		return selectedBand;
	}
	
	public String[] getBands(){
		return bands;
	}
	
	public synchronized void setSelectedBand(String newSelectedBand){
		String oldBand = selectedBand;
		selectedBand = newSelectedBand;
		log.println("Selected band changed from "+oldBand+" to "+newSelectedBand);
		firePropertyChangeEvent(propBand, oldBand, newSelectedBand);
	}

	public Stage createStage() {
		return new BandExtractorStage(this);
	}

	public StageView createStageView() {
		return new BandExtractorStageView(this);
	}

	public String getStageName() {
		return stageName;
	}
	
	public static String[] buildBandNames(int n){
		String[] bandNames = new String[n];
		for(int i=0; i<n; i++)
			bandNames[i] = "band_"+i;
		return bandNames;
	}
	
	public Object clone() throws CloneNotSupportedException {
		BandExtractorStageSettings settings = (BandExtractorStageSettings)super.clone();
		settings.bands = (String[])bands.clone();
		return settings;
	}
}
