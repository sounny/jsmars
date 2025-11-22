package edu.asu.jmars.layer.util.features.hsql;

import java.awt.Color;

public final class ColorToStringConverter implements HsqlColumnConverter {

	public Object hsqlToJava(Object in) {
		if (in == null)
			return null;
		
		return new Color(Integer.parseInt((String)in, 16));
	}

	public Object javaToHsql(Object in) {
		if (in == null)
			return null;
		
		return Integer.toHexString(((Color)in).getRGB());
	}
}
