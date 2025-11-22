package edu.asu.jmars.layer.util.features.hsql;

import java.util.ArrayList;
import java.util.List;

import edu.asu.jmars.layer.util.features.Field;

public class HsqlMaxConstraint implements HsqlConstraint, Cloneable {
	public final Field filterField;
	public final Object maxVal;
	
	public HsqlMaxConstraint(Field filterField, Object maxVal){
		this.filterField = filterField;
		this.maxVal = maxVal;
		
		if (filterField == null || maxVal == null)
			throw new IllegalArgumentException("Neither of the parameters may be null.");
	}
	
	public Field getFilterField(){
		return filterField;
	}
	
	public Object getMaxValue(){
		return maxVal;
	}
	
	public String getPreparedSqlSnippet(){
		return HsqlUtil.quote(filterField.name)+"<=?";
	}
	
	public List<Object> getPreparedSqlParams(HsqlColumnConverterFactory converterFactory) {
		HsqlColumnConverter columnConverter = converterFactory.getConverter(filterField.type, converterFactory.getSqlType(filterField.type));
		List<Object> paramObjs = new ArrayList<Object>(1);
		paramObjs.add(columnConverter.javaToHsql(getMaxValue()));
		return paramObjs;
	}

	public String toString(){
		return filterField.name+"<="+maxVal.toString();
	}
}
