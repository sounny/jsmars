package edu.asu.jmars.layer.util.features.hsql;

import java.util.ArrayList;
import java.util.List;

import edu.asu.jmars.layer.util.features.Field;

public class HsqlMinConstraint implements HsqlConstraint, Cloneable {
	public final Field filterField;
	public final Object minVal;
	
	public HsqlMinConstraint(Field filterField, Object minVal){
		this.filterField = filterField;
		this.minVal = minVal;
		
		if (filterField == null || minVal == null)
			throw new IllegalArgumentException("Neither of the parameters may be null.");
	}
	
	public Field getFilterField(){
		return filterField;
	}
	
	public Object getMinValue(){
		return minVal;
	}
	
	public String getPreparedSqlSnippet(){
		return HsqlUtil.quote(filterField.name)+">=?";
	}
	
	public List<Object> getPreparedSqlParams(HsqlColumnConverterFactory converterFactory) {
		HsqlColumnConverter columnConverter = converterFactory.getConverter(filterField.type, converterFactory.getSqlType(filterField.type));
		List<Object> paramObjs = new ArrayList<Object>(1);
		paramObjs.add(columnConverter.javaToHsql(getMinValue()));
		return paramObjs;
	}
	
	public String toString(){
		return filterField.name+">="+minVal.toString();
	}
}
