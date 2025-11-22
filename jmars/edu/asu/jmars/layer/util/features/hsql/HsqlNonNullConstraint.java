package edu.asu.jmars.layer.util.features.hsql;

import java.util.Collections;
import java.util.List;

import edu.asu.jmars.layer.util.features.Field;

public class HsqlNonNullConstraint implements Cloneable, HsqlConstraint {
	private final Field filterField;
	
	public HsqlNonNullConstraint(Field filterField){
		this.filterField = filterField;
		
		if (filterField == null)
			throw new IllegalArgumentException("Neither of the parameters may be null.");
	}
	
	public Field getFilterField(){
		return filterField;
	}
	
	public String getPreparedSqlSnippet() {
		return HsqlUtil.quote(filterField.name)+" is not null";
	}

	public List<Object> getPreparedSqlParams(HsqlColumnConverterFactory converterFactory) {
		return Collections.EMPTY_LIST;
	}

	public String toString(){
		return filterField.name+" is not null";
	}
}
