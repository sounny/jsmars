package edu.asu.jmars.layer.shape2.drawingpalette.swing;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public enum DrawActionEnum {

	RECTANGLE("rectangle") {
	},
	CIRCLE("circle") {
	},
	ELLIPSE("ellipse") {
	},
	ELLIPSE5("e5") {
	},
	POINT("point") {
	},
	LINE("line") {
	},
	POLYGON("polygon") {
	},
	FREEHAND("freehand") {
	},
	SELECT("select") {
	}, 
	MULTI("points/lines/poly");

	private String drawAction;
	private static Map<String, DrawActionEnum> drawactions = new ConcurrentHashMap<>();

	DrawActionEnum(String type) {
		drawAction = type;
	}

	public String asString() {
		return drawAction;
	}

	static {
		Map<String, DrawActionEnum> map = new ConcurrentHashMap<>();
		for (DrawActionEnum instance : DrawActionEnum.values()) {
			map.put(instance.asString(), instance);
		}
		drawactions = Collections.unmodifiableMap(map);
	}

	public static DrawActionEnum get(String name) {
		return drawactions.get(name);
	}	
}

