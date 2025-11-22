package edu.asu.jmars.layer.profile.config;

import java.awt.Shape;
import java.util.List;
import java.util.Map;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.Pipeline;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Config {

	private String configName = "ChartX"; // assigned by user, if any
	private ConfigType configType;	
	private Map<Integer, Shape> profilesToChart = new HashMap<>();
	private List<MapSource> numsourcesToChart = new ArrayList<>();
	private Pipeline[] pipeline = null;
	private String identifier = "Chart "; // created internally
	private int ID;
	private static int uniqueID = 1;

	public Config() {
		this.ID = uniqueID;
		this.identifier = this.identifier + this.ID;
		uniqueID++;
	}
	
	public void withConfigName(String configName) {
		this.configName = configName;
	}

	public void withConfigType(ConfigType configType) {
		this.configType = configType;
	}

	public void withProfilesToChart(Map<Integer, Shape> varProfiles) {
		this.profilesToChart.clear();
		this.profilesToChart.putAll(varProfiles);
	}

	public void withNumsourcesToChart(List<MapSource> varSources) {
		this.numsourcesToChart.clear();
		this.numsourcesToChart.addAll(varSources);
	}
	
	public void withPipeline(Pipeline[] data) {
		this.pipeline =  Arrays.stream(data).toArray(Pipeline[]::new);
	}

	public String getConfigName() {
		return configName;
	}

	public ConfigType getConfigType() {
		return configType;
	}
	
	public Pipeline[] getPipeline() {
		if (pipeline == null) return null;
		else return Arrays.stream(pipeline).toArray(Pipeline[]::new);
	}

	public Map<Integer, Shape> getProfilesToChart() {
		Map<Integer, Shape> varProfilesToChart = new HashMap<>();
		varProfilesToChart.putAll(this.profilesToChart);
		return varProfilesToChart;
	}

	public List<MapSource> getNumsourcesToChart() {
		List<MapSource> varNumsourcesToChart = new ArrayList<>();
		varNumsourcesToChart.addAll(this.numsourcesToChart);
		return varNumsourcesToChart;
	}

	public String getConfigInternalIdentifier() {
		return identifier;
	}

	public int getConfigID() {
		return this.ID;
	}

}