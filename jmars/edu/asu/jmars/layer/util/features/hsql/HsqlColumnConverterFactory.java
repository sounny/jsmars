package edu.asu.jmars.layer.util.features.hsql;

import java.awt.Color;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.util.BidiMap;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.LineType;

public class HsqlColumnConverterFactory {
	private static final DebugLog log = DebugLog.instance();
	
	/*
	 * Pseudo Java Type Names recognized.
	 * These are made-up names.
	 */
	public static final String PJT_FPATH = "fpath";
	public static final String PJT_LINE_TYPE = "line_type";
	public static final String PJT_COLOR = "color";
	public static final String PJT_INT = "integer";
	public static final String PJT_SHORT = "short";
	public static final String PJT_BYTE = "byte";
	public static final String PJT_FLOAT = "float";
	public static final String PJT_DOUBLE = "double";
	public static final String PJT_DATE = "date";
	public static final String PJT_SQL_DATE = "sql_date";
	public static final String PJT_SQL_TIME = "sql_time";
	public static final String PJT_SQL_TIMESTAMP = "sql_timestamp";
	public static final String PJT_STRING = "string";
	public static final String PJT_BOOLEAN = "boolean";
	
	/*
	 * SQL type names that we support.
	 * These must be legal HSQL type names.
	 */
	public static final String SQT_VARCHAR = "varchar";
	public static final String SQT_BOOLEAN = "boolean";
	public static final String SQT_TINYINT = "tinyint";
	public static final String SQT_SMALLINT = "smallint";
	public static final String SQT_INT = "integer";
	public static final String SQT_REAL = "real";
	public static final String SQT_DOUBLE = "double";
	public static final String SQT_DOUBLE_PREC = "double precision";
	public static final String SQT_DATE = "date";
	public static final String SQT_TIME = "time";
	public static final String SQT_TIMESTAMP = "timestamp";
	
	
	
	private static HsqlColumnConverterFactory instance = null;
	
	
	// forward-mapping: pseudo-java-type -> (sql-type -> hsql-converter)
	private Map<String, Map<String, HsqlColumnConverter>> fwd = new LinkedHashMap<String, Map<String, HsqlColumnConverter>>();
	
	// reverse-mapping: sql-type -> (pseudo-java-type -> hsql-converter)
	private Map<String, Map<String, HsqlColumnConverter>> rev = new LinkedHashMap<String, Map<String, HsqlColumnConverter>>();
	
	// 1-1 mapping: class <-> pseudo-java-type 
	private BidiMap classToPJT = new BidiMap();
	
	public static HsqlColumnConverterFactory instance(){
		if (instance == null)
			instance = new HsqlColumnConverterFactory();
		return instance;
	}
	
	public HsqlColumnConverterFactory(){
		/*
		 * CAUTION: Order matters here for default reverse type mapping.
		 * See getSqlTypeEquivJavaType()
		 */
		addConverter(PJT_STRING, SQT_VARCHAR, new SimplePassthruConverter(String.class));
		setMapping(String.class, PJT_STRING);
		
		addConverter(PJT_INT, SQT_INT, new SimplePassthruConverter(Integer.class));
		setMapping(Integer.class, PJT_INT);
		
		addConverter(PJT_SHORT, SQT_SMALLINT, new SimplePassthruConverter(Short.class));
		setMapping(Short.class, PJT_SHORT);
		
		addConverter(PJT_BYTE, SQT_TINYINT, new SimplePassthruConverter(Byte.class));
		setMapping(Byte.class, PJT_BYTE);
		
		addConverter(PJT_FLOAT, SQT_REAL, new FloatToDoubleConverter());
		setMapping(Float.class, PJT_FLOAT);
		
		addConverter(PJT_DOUBLE, SQT_DOUBLE, new SimplePassthruConverter(Double.class));
		addConverter(PJT_DOUBLE, SQT_DOUBLE_PREC, new SimplePassthruConverter(Double.class));
		setMapping(Double.class, PJT_DOUBLE);
		
		addConverter(PJT_BOOLEAN, SQT_BOOLEAN, new SimplePassthruConverter(Boolean.class));
		setMapping(Boolean.class, PJT_BOOLEAN);
		
		addConverter(PJT_SQL_DATE, SQT_DATE, new SimplePassthruConverter(java.sql.Date.class));
		setMapping(java.sql.Date.class, PJT_SQL_DATE);
		
		addConverter(PJT_SQL_TIME, SQT_TIME, new SimplePassthruConverter(Time.class));
		setMapping(Time.class, PJT_SQL_TIME);
		
		addConverter(PJT_SQL_TIMESTAMP, SQT_TIMESTAMP, new SimplePassthruConverter(Timestamp.class));
		setMapping(Timestamp.class, PJT_SQL_TIMESTAMP);
		
		addConverter(PJT_FPATH, SQT_VARCHAR, new FPathToStringConverter());
		setMapping(FPath.class, PJT_FPATH);
		
		addConverter(PJT_LINE_TYPE, SQT_INT, new LineTypeToIntConverter());
		setMapping(LineType.class, PJT_LINE_TYPE);
		
		addConverter(PJT_COLOR, SQT_VARCHAR, new ColorToStringConverter());
		setMapping(Color.class, PJT_COLOR);
		
		addConverter(PJT_DATE, SQT_TIMESTAMP, new DateToTimestampConverter());
		setMapping(Date.class, PJT_DATE);
	}
	
	public void addConverter(String pseudoJavaTypeName, String onDiskJavaType, HsqlColumnConverter converter){
		if (converter == null)
			throw new IllegalArgumentException("Converter must not be null.");
		
		Map<String, HsqlColumnConverter> fwdMap = fwd.get(pseudoJavaTypeName);
		if (fwdMap == null)
			fwd.put(pseudoJavaTypeName, fwdMap = new LinkedHashMap<String, HsqlColumnConverter>());
		fwdMap.put(onDiskJavaType, converter);
		
		Map<String, HsqlColumnConverter> revMap = rev.get(onDiskJavaType);
		if (revMap == null)
			rev.put(onDiskJavaType, revMap = new LinkedHashMap<String, HsqlColumnConverter>());
		revMap.put(pseudoJavaTypeName, converter);
	}
	
	public void setMapping(Class<?> clazz, String pseudoJavaTypeName){
		if (fwd.get(pseudoJavaTypeName).isEmpty())
			throw new IllegalArgumentException("No converters have been added for Pseudo Java Type named "+pseudoJavaTypeName+".");
		
		classToPJT.add(clazz, pseudoJavaTypeName);
	}
	
	/**
	 * Returns the SQL-type equivalent Java-type for the given Java-type.
	 * For example, if objects of FPath class are going to be output as an
	 * SQL varchar, then this method will return the class String.
	 * @param clazz Input java-type of the data.
	 * @return Output java-type of the data.
	 */
	public Class<?> getSqlTypeEquivJavaType(Class<?> clazz){
		return getJavaClass(getSqlType(clazz));
	}
	
	public String getSqlType(String pseudoJavaTypeName){
		return fwd.get(pseudoJavaTypeName).keySet().iterator().next();
	}
	
	public String getSqlType(Class<?> clazz){
		return getSqlType((String)classToPJT.getLeft(clazz));
	}
	
	public String getPseudoJavaType(Class<?> clazz){
		return (String)classToPJT.getLeft(clazz);
	}
	
	public Class<?> getJavaClassFromPseudoJavaType(String pseudoJavaType){
		return (Class<?>)classToPJT.getRight(pseudoJavaType);
	}
	
	public Class<?> getJavaClass(String sqlType){
		try {
			return (Class<?>)classToPJT.getRight(rev.get(sqlType).keySet().iterator().next());
		}
		catch(RuntimeException ex){
			ex.printStackTrace();
			log.aprintln(ex.toString()+" while converting sqlType: "+sqlType);
			throw ex;
		}
	}
	
	public HsqlColumnConverter getConverter(String pseudoJavaTypeName, String sqlType){
		Map<String, HsqlColumnConverter> map = fwd.get(pseudoJavaTypeName);
		if (map == null)
			return null;
		
		return map.get(sqlType);
	}
	
	public HsqlColumnConverter getConverter(Class<?> clazz, String sqlType){
		return getConverter((String)classToPJT.getLeft(clazz), sqlType);
	}
}
