package edu.asu.jmars.layer.util.features.hsql;

/**
 * Converts between a JAVA-type and an SQL-type.
 */
public interface HsqlColumnConverter {
	public Object hsqlToJava(final Object in);
	public Object javaToHsql(final Object in);
}
