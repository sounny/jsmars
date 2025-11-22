package edu.asu.jmars.layer.util.features.hsql;

public class FloatToDoubleConverter implements HsqlColumnConverter {

	public Object hsqlToJava(Object in) {
		if (in == null)
			return null;
		
		return new Float(((Double)in).floatValue());
	}

	public Object javaToHsql(Object in) {
		if (in == null)
			return null;
		
		return new Double(((Float)in).doubleValue());
	}

}
