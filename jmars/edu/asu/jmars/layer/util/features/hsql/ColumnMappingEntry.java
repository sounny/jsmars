package edu.asu.jmars.layer.util.features.hsql;

public class ColumnMappingEntry {
	public final String columnName;
	public final String pseudoJavaTypeName;
	public final String sqlTypeName;
	
	public ColumnMappingEntry(String columnName, String pseudoJavaTypeName, String sqlTypeName){
		this.columnName = columnName;
		this.pseudoJavaTypeName = pseudoJavaTypeName;
		this.sqlTypeName = sqlTypeName;
	}
	
	public boolean equals(Object other){
		if (other == null || !(other instanceof ColumnMappingEntry))
			return false;
		
		ColumnMappingEntry o = (ColumnMappingEntry)other;
		return columnName.equals(o.columnName) && pseudoJavaTypeName.equals(o.pseudoJavaTypeName) && sqlTypeName.equals(o.sqlTypeName);
	}
	
	public int hashCode(){
		return columnName.hashCode() ^ pseudoJavaTypeName.hashCode() ^ sqlTypeName.hashCode();
	}
}
