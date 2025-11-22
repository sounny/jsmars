package edu.asu.jmars.layer.util.features.hsql;

import java.util.Collections;
import java.util.List;

import edu.asu.jmars.layer.util.features.Field;

public class HsqlLikeConstraint implements HsqlConstraint {
	private final Field filterField;
	private final String likeStatement;
	
	public HsqlLikeConstraint(Field filterField, String likeStatement) {
		this.filterField = filterField;
		this.likeStatement = likeStatement;
	}
	
	public Field getField() {
		return filterField;
	}
	
	public String getLikeStatement() {
		return likeStatement;
	}
	
	public String getPreparedSqlSnippet() {
		return HsqlUtil.quote(filterField.name) + " LIKE ?";
	}
	
	public List<Object> getPreparedSqlParams(HsqlColumnConverterFactory converterFactory) {
		HsqlColumnConverter columnConverter = converterFactory.getConverter(filterField.type, converterFactory.getSqlType(filterField.type));
		return Collections.singletonList(columnConverter.javaToHsql(likeStatement));
	}
}
