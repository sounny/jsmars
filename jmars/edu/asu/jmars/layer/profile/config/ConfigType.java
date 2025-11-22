package edu.asu.jmars.layer.profile.config;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum ConfigType {	
	ONENUMSOURCE("Compare Profile Lines") {
	},
	MANYNUMSOURCES("Compare Numeric Sources") {
	};

	private String configType;
	private static Map<String, ConfigType> configurations = new ConcurrentHashMap<>();

	ConfigType(String type) {
		configType = type;
	}

	public String asString() {
		return configType;
	}

	static {
		Map<String, ConfigType> map = new ConcurrentHashMap<>();
		for (ConfigType instance : ConfigType.values()) {
			map.put(instance.asString(), instance);
		}
		configurations = Collections.unmodifiableMap(map);
	}

	public static ConfigType get(String name) {
		return configurations.get(name);
	}	
}
