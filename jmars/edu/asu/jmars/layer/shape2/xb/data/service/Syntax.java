package edu.asu.jmars.layer.shape2.xb.data.service;

import java.util.HashSet;
import java.util.Set;
import edu.asu.jmars.Main;

public enum Syntax {
	
	SERVICE;
	
	private Set<String> data = new HashSet<>();
	private static String PATH_TO_AUTO = "resources/xb/java.auto.txt";
	
	private Syntax() {}

	private void loadData() {
		String allfile = "";
		data.clear();
		allfile = Main.getResourceAsString(PATH_TO_AUTO);
		String[] words = allfile.trim().split("\n");
		for (String word : words) {
			data.add(word);
		}
	}

	public Set<String> getData() {
		if (data.isEmpty()) {
			loadData();
		}
		return data;
	}

}
