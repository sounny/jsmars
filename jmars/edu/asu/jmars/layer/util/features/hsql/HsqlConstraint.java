package edu.asu.jmars.layer.util.features.hsql;

import java.util.List;

public interface HsqlConstraint {
	public String getPreparedSqlSnippet();
	public List<Object> getPreparedSqlParams(HsqlColumnConverterFactory converterFactory);
}
