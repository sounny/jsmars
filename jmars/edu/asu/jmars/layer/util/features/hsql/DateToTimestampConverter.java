package edu.asu.jmars.layer.util.features.hsql;

import java.sql.Timestamp;
import java.util.Date;

public final class DateToTimestampConverter implements HsqlColumnConverter {

	public Object hsqlToJava(Object in) {
		if (in == null)
			return null;
		
		return new Date(((Timestamp)in).getTime());
	}

	public Object javaToHsql(Object in) {
		if (in == null)
			return null;
		
		return new Timestamp(((Date)in).getTime());
	}
}
