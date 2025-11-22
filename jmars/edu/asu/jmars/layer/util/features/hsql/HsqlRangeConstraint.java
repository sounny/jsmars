package edu.asu.jmars.layer.util.features.hsql;

import java.util.ArrayList;
import java.util.List;

import edu.asu.jmars.layer.util.features.Field;

public class HsqlRangeConstraint implements HsqlConstraint, Cloneable {
	private final Field filterField;
	private final Object minVal;
	private final Object maxVal;
	
	public HsqlRangeConstraint(Field filterField, Object minVal, Object maxVal){
		this.filterField = filterField;
		this.minVal = minVal;
		this.maxVal = maxVal;

		if (filterField == null || minVal == null || maxVal == null)
			throw new IllegalArgumentException("Neither of the parameters may be null.");
	}
	
	public Field getFilterField(){
		return filterField;
	}
	
	public Object getMinValue(){
		return minVal;
	}
	
	public Object getMaxValue(){
		return maxVal;
	}
	
	public String getPreparedSqlSnippet(){
		return "("+HsqlUtil.quote(filterField.name)+">=? AND "+HsqlUtil.quote(filterField.name)+"<=?"+")";
	}
	
	public List<Object> getPreparedSqlParams(HsqlColumnConverterFactory converterFactory) {
		HsqlColumnConverter columnConverter = converterFactory.getConverter(filterField.type, converterFactory.getSqlType(filterField.type));
		List<Object> paramObjs = new ArrayList<Object>(2);
		paramObjs.add(columnConverter.javaToHsql(getMinValue()));
		paramObjs.add(columnConverter.javaToHsql(getMaxValue()));
		return paramObjs;
	}
	
	public String toString(){
		return "("+minVal.toString()+"<="+filterField.name+"<="+maxVal.toString()+")";
	}
}
