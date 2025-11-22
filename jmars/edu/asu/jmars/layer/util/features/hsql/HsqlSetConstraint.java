package edu.asu.jmars.layer.util.features.hsql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import edu.asu.jmars.layer.util.features.Field;

public class HsqlSetConstraint implements Cloneable, HsqlConstraint {
	private final Field filterField;
	private final Set<Object> valueSet;
	
	public HsqlSetConstraint(Field filterField, Set valueSet){
		this.filterField = filterField;
		this.valueSet = valueSet == null? null: new HashSet(valueSet);
		
		if (filterField == null || valueSet == null)
			throw new IllegalArgumentException("Neither of the parameters may be null.");
	}
	
	public Field getFilterField(){
		return filterField;
	}
	
	public Set getValueSet(){
		return Collections.unmodifiableSet(valueSet);
	}

	public String getPreparedSqlSnippet(){
		StringBuffer valBuf = new StringBuffer();
		for(Object val: valueSet){
			if (valBuf.length() > 0)
				valBuf.append(",");
			valBuf.append("?");
		}
		valBuf.append(")");
		valBuf.insert(0, HsqlUtil.quote(filterField.name)+" in (");
		
		return valBuf.toString();
	}
	
	public List<Object> getPreparedSqlParams(HsqlColumnConverterFactory converterFactory) {
		HsqlColumnConverter columnConverter = converterFactory.getConverter(filterField.type, converterFactory.getSqlType(filterField.type));
		List<Object> paramObjs = new ArrayList<Object>(valueSet.size());
		for(Object val: valueSet)
			paramObjs.add(columnConverter.javaToHsql(val));
		return paramObjs;
	}

	public String toString(){
		return filterField.name+" in ["+valueSet.toString()+"]";
	}
}
