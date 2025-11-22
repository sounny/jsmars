package edu.asu.jmars.layer.util.features.hsql;

import java.util.ArrayList;
import java.util.List;

import edu.asu.jmars.layer.util.features.Field;

public class HsqlEqualConstraint implements Cloneable, HsqlConstraint {
	private final Field filterField;
	private final Object val;
	
	public HsqlEqualConstraint(Field filterField, Object val){
		this.filterField = filterField;
		this.val = val;
		
		if (filterField == null || val == null)
			throw new IllegalArgumentException("Neither of the parameters may be null.");
	}
	
	public Field getFilterField() {
		return filterField;
	}

	public Object getValue(){
		return val;
	}
	
	public String getPreparedSqlSnippet(){
		return HsqlUtil.quote(filterField.name)+"=?";
	}
	
	public List<Object> getPreparedSqlParams(HsqlColumnConverterFactory converterFactory) {
		HsqlColumnConverter columnConverter = converterFactory.getConverter(filterField.type, converterFactory.getSqlType(filterField.type));
		List<Object> paramObjs = new ArrayList<Object>(1);
		paramObjs.add(columnConverter.javaToHsql(getValue()));
		return paramObjs;
	}

	public String toString(){
		return filterField.name+"=="+val.toString();
	}
}
