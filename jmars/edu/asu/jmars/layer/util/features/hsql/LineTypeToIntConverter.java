package edu.asu.jmars.layer.util.features.hsql;

import edu.asu.jmars.util.LineType;

public final class LineTypeToIntConverter implements HsqlColumnConverter {

	public Object hsqlToJava(Object in) {
		if (in == null)
			return null;
		
		return new LineType(((Integer)in).intValue());
	}

	public Object javaToHsql(Object in) {
		if (in == null)
			return null;
		
		return new Integer(((LineType)in).getType());
	}

}
