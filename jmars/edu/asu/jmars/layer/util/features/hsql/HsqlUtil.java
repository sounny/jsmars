package edu.asu.jmars.layer.util.features.hsql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.SingleFeatureCollection;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

public class HsqlUtil {
	private static final DebugLog log = DebugLog.instance();
	
	public static final String FC_TABLE_NAME = "collections";
	public static final Field  FC_NAME_FIELD = new Field("name", String.class);
	public static final Field  FC_ID_FIELD = new Field("id", Integer.class);
	
	public static final String CM_TABLE_NAME = "column_mapping";
	public static final Field  CM_FC_ID_FIELD = new Field("collection id", FC_ID_FIELD.type);
	public static final Field  CM_COL_NAME_FIELD = new Field("column name", String.class);
	public static final Field  CM_COL_JAVA_TYPE_NAME_FIELD = new Field("java type", String.class);
	public static final Field  CM_COL_SQL_TYPE_NAME_FIELD = new Field("sql type", String.class);
	
	/**
	 * Constructs and returns a Field object from the current row of the ResultSet.
	 * @param rs ResultSet object as obtained from {@link DatabaseMetaData#getColumns(String, String, String, String)}.
	 * @return Field object built with the column name and type as read from the database.
	 *         The SQL-type is converted to Java-type by using {@link Util#jdbc2java(String)}.
	 * @throws SQLException In case of a database error.
	 */
	public static Field getField(ResultSet rs) throws SQLException {
		String colName = rs.getString("COLUMN_NAME");
		String sqlType = rs.getString("TYPE_NAME");
		
		return new Field(colName, HsqlColumnConverterFactory.instance().getJavaClass(sqlType.toLowerCase()));
	}

	/**
	 * Returns an ordered set of Fields corresponding to the columns in the specified table.
	 * @param meta Database meta-data to pull the column information from.
	 * @param tableName Table for which the column names are to be pulled. The table-name is
	 *                  case-fixed using {@link #fixCase(String, DatabaseMetaData)}.
	 * @return An ordered set of Fields. 
	 * @throws SQLException In case of a database error.
	 */
	public static LinkedHashSet<Field> getFields(DatabaseMetaData meta, String tableName) throws SQLException {
		LinkedHashSet<Field> schema = new LinkedHashSet<Field>(); 
		ResultSet rs = meta.getColumns(null, null, tableName, "%");
		while(rs.next())
			schema.add(getField(rs));
		
		return schema;
	}

	/**
	 * Returns <code>true</code> if a table with the specified name exists in the database connected
	 * to via the specified Connection.
	 * @param c Connection to the database.
	 * @param tableName Name of the table to be checked for existence. The table name is
	 *                  case-fixed using {@link #fixCase(String, DatabaseMetaData)}.
	 * @return <code>true</code> if the table exists.
	 * @throws SQLException In case of a database error.
	 */
	public static boolean tableExists(Connection c, String tableName) throws SQLException {
		DatabaseMetaData meta = c.getMetaData();
		ResultSet rs = meta.getTables(null, null, tableName, new String[]{ "TABLE" });
		boolean found = rs.next();
		rs.close();
		
		return found;
	}
	
	/**
	 * Update the specified table's structure such that it contains only the fields 
	 * specified in the <code>fields</code> parameter. This may result in loss of
	 * data if a column is removed. Field's java-type to sql-type mapping is done
	 * via {@link HsqlColumnConverterFactory}.
	 * @param c Connection to database.
	 * @param tableName Name of the table to modify. The name is properly case converted
	 *                  before use.
	 * @param fields Final set of fields that you want the table to end up with.
	 *               Field names are case converted before use.
	 * @throws SQLException In case of a database error.
	 */
	public static void updateTableStructure(Connection c, String tableName, Set<Field> fields) throws SQLException {
		DatabaseMetaData meta = c.getMetaData();
		//tableName = fixCase(tableName, meta);
		LinkedHashSet<Field> existing = getFields(meta, tableName);
		Set<Field> remove = new HashSet<Field>(existing);
		remove.removeAll(fields);
		Set<Field> add = new HashSet<Field>(fields);
		add.removeAll(existing);
		
		HsqlColumnConverterFactory converterFactory = HsqlColumnConverterFactory.instance();
		Statement s = c.createStatement();
		for(Field f: remove){
			s.executeUpdate("alter table "+quote(tableName)+" drop column "+quote(f.name));
		}
		
		for(Field f: add){
			s.executeUpdate("alter table "+quote(tableName)+" add column "+quote(f.name)+" "+converterFactory.getSqlType(f.type));
		}
		
		/*
		int[] results = s.executeBatch();
		for(int i=0; i<results.length; i++)
			if (results[i] != Statement.SUCCESS_NO_INFO)
				throw new RuntimeException("An SQL command failed while executing batch.");
				*/
		s.close();
	}
	
	public static String quote(String identifier){
		return "\""+identifier+"\"";
	}
	
	/**
	 * Creates a table with the specified fields.
	 * @param c Connection to database.
	 * @param tableName Name of the table to create. The name is properly case
	 *                  converted before use.
	 * @param fields Set of fields to create in the table.
	 * @param auto Identity fields that are auto-generated as a sequence. This may
	 *             be passed as <code>null</code>.
	 * @throws SQLException In case of a database error.
	 */
	public static void createTable(Connection c, String tableName, Set<Field> fields, Set<Field> auto) throws SQLException {
		StringBuffer sql = new StringBuffer();
		for(Field f: fields){
			if (sql.length() > 0)
				sql.append(",");
			sql.append(quote(f.name)+" "+HsqlColumnConverterFactory.instance().getSqlType(f.type));
			if (auto != null && auto.contains(f)){
				sql.append(" generated by default as identity");
			}
		}
		sql.insert(0, "create cached table "+quote(tableName)+" (");
		sql.append(")");
		
		Statement s = c.createStatement();
		s.executeUpdate(sql.toString());
		s.close();
	}
	
	public static void createIndex(Connection c, String tableName, Set<Field> indexFields, boolean unique) throws SQLException {
		StringBuffer indexName = new StringBuffer();
		indexName.append(tableName);
		for(Field f: indexFields){
			indexName.append("_");
			indexName.append(f.name);
		}
		
		StringBuffer sql = new StringBuffer();
		for(Field f: indexFields){
			if (sql.length() > 0)
				sql.append(",");
			sql.append(quote(f.name));
		}
		sql.insert(0, "create "+(unique?"unique":"")+" index "+
				quote(indexName.toString())+" on "+quote(tableName)+" (");
		sql.append(")");
		
		Statement s = c.createStatement();
		s.executeUpdate(sql.toString());
		s.close();
	}

	/**
	 * Drop the specified table.
	 * @param c Connection to the database.
	 * @param tableName Name of the table to drop. The name is properly case
	 *                  converted before use.
	 * @throws SQLException In case of a database error.
	 */
	public static void dropTable(Connection c, String tableName) throws SQLException {
		Statement s = c.createStatement();
		s.executeUpdate("drop table "+quote(tableName));
		s.close();
	}
	
	/**
	 * Removes all rows from the specified table.
	 * @param c Connection to the database.
	 * @param tableName Name of the table to truncate.
	 * @throws SQLException
	 */
	public static void truncateTable(Connection c, String tableName) throws SQLException {
		Statement s = c.createStatement();
		s.executeUpdate("truncate "+quote(tableName));
		s.close();
	}
	
	/**
	 * Returns the set of collections currently listed in the collections table.
	 * @param c Connection to the database.
	 * @return A set of collection names.
	 * @throws SQLException
	 */
	public static Set<String> getCollectionNames(Connection c) throws SQLException {
		Set<String> collections = new HashSet<String>();
		if (tableExists(c, FC_TABLE_NAME)){
			Statement s = c.createStatement();
			ResultSet rs = s.executeQuery("select "+quote(FC_NAME_FIELD.name)+" from "+quote(FC_TABLE_NAME));
			while(rs.next())
				collections.add(rs.getString(FC_NAME_FIELD.name));
			s.close();
		}
		
		return collections;
	}
	
	/**
	 * Returns the collection id of the named collection. The collection must
	 * already exist prior to calling this method.
	 * @param c Connection to the database.
	 * @param name Name of the collection to search for.
	 * @return The auto-generated identifier of the collection with the given name.
	 * @throws SQLException
	 */
	public static int getCollectionId(Connection c, String name) throws SQLException {
		PreparedStatement s = c.prepareStatement("select "+quote(FC_ID_FIELD.name)+" from "+quote(FC_TABLE_NAME)+" where "+quote(FC_NAME_FIELD.name)+"="+"?");
		s.setString(1, name);
		ResultSet rs = s.executeQuery();
		rs.next();
		int id = rs.getInt(1);
		s.close();
		return id;
	}
	
	
	private static Set<Field> translateSchema(Set<Field> in){
		Set<Field> out = new LinkedHashSet<Field>(in.size());
		HsqlColumnConverterFactory colConverter = HsqlColumnConverterFactory.instance();
		for(Field inf: in){
			Class<?> sqlEquivJavaType =  colConverter.getSqlTypeEquivJavaType(inf.type);
			Field outf = new Field(inf.name, sqlEquivJavaType, inf.editable);
			out.add(outf);
		}
		return out;
	}

	/*
	private static Set<Field> untranslateSchema(Set<Field> in, Map<String, ColumnMappingEntry> cmeMap){
		// Do something else with the column-mapping table
		Set<Field> out = new LinkedHashSet<Field>(in.size());
		HsqlColumnConverterFactory colConverter = HsqlColumnConverterFactory.instance();
		for(Field inf: in){
			cmeMap.get(inf.name).pseudoJavaTypeName
			Field outf = new Field(inf.name, colConverter.getSqlTypeEquivJavaType(inf.type), inf.editable);
			out.add(outf);
		}
		return out;
	}
	*/
	
	/**
	 * Creates a table to store collection data for the collection specified 
	 * by the <code>name</code> argument.
	 * @param c Connection object to use.
	 * @param name Name of the collection to be created.
	 * @param schema Schema of the collection.
	 * @throws SQLException Database exceptions that may get thrown for various reasons.
	 */
	public static void createCollection(Connection c, String name, Set<Field> schema) throws SQLException {
		if (!HsqlUtil.tableExists(c, FC_TABLE_NAME)){
			log.println("Creating collections table ("+CM_TABLE_NAME+") since it does not exist already.");
			createCollectionsTable(c);
		}
		if (!HsqlUtil.tableExists(c, CM_TABLE_NAME)){
			log.println("Creating column mapping table ("+CM_TABLE_NAME+") since it does not exist already.");
			createColumnMappingTable(c);
		}
		
		// translate schema such that it uses database friendly Java types.
		Set<Field> sqlSchema = translateSchema(schema);
		
		log.println("Creating table for collection "+name);
		createTable(c, name, sqlSchema, null);
		for(Field f: sqlSchema){
			log.println("Creating index for field \""+f.name+"\" for collection \""+name+"\"");
			createIndex(c, name, Collections.singleton(f), false);
		}
		
		log.println("Creating an entry for \""+name+"\" in the collections table ("+FC_TABLE_NAME+").");
		PreparedStatement s = c.prepareStatement("insert into "+quote(FC_TABLE_NAME)+"("+quote(FC_NAME_FIELD.name)+") values (?)");
		s.setString(1, name);
		s.executeUpdate();
		
		int fcId = getCollectionId(c, name);
		
		log.println("Adding columns to the column mapping table ("+CM_TABLE_NAME+").");
		addColumnMappingEntries(c, fcId, schema);
		
		s.close();
	}
	
	/**
	 * Drops the collection table, its entry in the collections table and all the
	 * column mappings for the specified collection.
	 * @param c Connection to the database.
	 * @param name Name of the collection to remove.
	 * @throws SQLException
	 */
	public static void dropCollection(Connection c, String name) throws SQLException {
		dropTable(c, name);
		int fcId = getCollectionId(c, name);
		
		PreparedStatement s = c.prepareStatement("delete from "+quote(CM_TABLE_NAME)+" where "+quote(CM_FC_ID_FIELD.name)+" = ?");
		s.setInt(1, fcId);
		s.executeUpdate();
		
		s = c.prepareStatement("delete from "+quote(FC_TABLE_NAME)+" where "+quote(FC_ID_FIELD.name)+" = ?");
		s.setInt(1, fcId);
		s.executeUpdate();
		
		s.close();
	}
	
	/**
	 * Adds the specified column mapping entries to the column mapping table
	 * for the specified feature collection.
	 * @param c Connection to the database.
	 * @param fcId Collection to which these column mappings belong.
	 * @param fields Fields for which column mappings will be added.
	 * @throws SQLException
	 */
	public static void addColumnMappingEntries(Connection c, int fcId, Set<Field> fields) throws SQLException {
		HsqlColumnConverterFactory colConverter = HsqlColumnConverterFactory.instance();
		
		PreparedStatement s = c.prepareStatement("insert into "+quote(CM_TABLE_NAME)+" ("+
				quote(CM_FC_ID_FIELD.name)+","+
				quote(CM_COL_NAME_FIELD.name)+","+
				quote(CM_COL_JAVA_TYPE_NAME_FIELD.name)+
				") values (?,?,?)");
		for(Field f: fields){
			s.setInt(1, fcId);
			s.setString(2, f.name);
			s.setString(3, colConverter.getPseudoJavaType(f.type));
			s.addBatch();
		}
		s.executeBatch();
	}
	
	/**
	 * Removes the specified column mapping entries from the column mapping
	 * table for the specified feature collection.
	 * @param c Connection to the database.
	 * @param fcId Collection to which these column mappings belong.
	 * @param fields Fields for which column mappings will be removed.
	 * @throws SQLException
	 */
	public static void removeColumnMappingEntries(Connection c, int fcId, Set<Field> fields) throws SQLException {
		PreparedStatement s = c.prepareStatement("delete from "+quote(CM_TABLE_NAME)+" where "+
				quote(CM_FC_ID_FIELD.name)+" = ? and "+quote(CM_COL_NAME_FIELD.name)+" = ?");
		for(Field f: fields){
			s.setInt(1, fcId);
			s.setString(2, f.name);
			s.addBatch();
		}
		s.executeBatch();
	}
	
	public static Map<String, ColumnMappingEntry> getColumnMappingEntries(Connection c, String fcName) throws SQLException {
		int fcId = getCollectionId(c, fcName);
		
		PreparedStatement s = c.prepareStatement("select "+
				quote(CM_COL_NAME_FIELD.name)+","+
				quote(CM_COL_JAVA_TYPE_NAME_FIELD.name)+
				" from "+
				quote(CM_TABLE_NAME)+
				" where "+
				quote(CM_COL_NAME_FIELD.name)+"="+"?");
		s.setInt(1, fcId);
		
		ResultSet cmrs = s.executeQuery();
		DatabaseMetaData meta = c.getMetaData();
		
		Map<String, ColumnMappingEntry> cmeMap = new LinkedHashMap<String, ColumnMappingEntry>();
		while(cmrs.next()){
			String colName = cmrs.getString(CM_COL_NAME_FIELD.name);
			String colJavaPseudoType = cmrs.getString(CM_COL_JAVA_TYPE_NAME_FIELD.name);
			
			ResultSet mdrs = meta.getColumns(null, null, fcName, colName);
			mdrs.next();
			String colSqlType = mdrs.getString("TYPE_NAME");
			
			ColumnMappingEntry cme = new ColumnMappingEntry(colName, colJavaPseudoType, colSqlType);
			cmeMap.put(cme.columnName, cme);
		}
		
		return cmeMap;
	}
	
	/**
	 * Returns the collection's schema as stored in the {@link #CM_TABLE_NAME} table.
	 * @param c Connection to the database.
	 * @param fcName Collection name.
	 * @return A Set of fields built using the {@link #CM_COL_NAME_FIELD} and 
	 *         {@link #CM_COL_JAVA_TYPE_NAME_FIELD} columns.
	 * @throws SQLException
	 */
	public static Set<Field> getCollectionSchema(Connection c, String fcName) throws SQLException {
		int fcId = getCollectionId(c, fcName);
		
		PreparedStatement s = c.prepareStatement("select "+
				quote(CM_COL_NAME_FIELD.name)+","+
				quote(CM_COL_JAVA_TYPE_NAME_FIELD.name)+
				" from "+
				quote(CM_TABLE_NAME)+
				" where "+
				quote(CM_FC_ID_FIELD.name)+"="+"?");
		s.setInt(1, fcId);
		
		ResultSet cmrs = s.executeQuery();
		HsqlColumnConverterFactory columnConverter = HsqlColumnConverterFactory.instance();
		
		Set<Field> schema = new LinkedHashSet<Field>();
		while(cmrs.next()){
			String colName = cmrs.getString(CM_COL_NAME_FIELD.name);
			String colPseudoJavaType = cmrs.getString(CM_COL_JAVA_TYPE_NAME_FIELD.name);
			
			Field f = new Field(colName, columnConverter.getJavaClassFromPseudoJavaType(colPseudoJavaType));
			schema.add(f);
		}
		
		return schema;
	}
	
	/**
	 * Constructs a map from field name to field from the input collection
	 * of fields.
	 * @param schema Input schema.
	 * @return Map from field name to field object.
	 */
	private static Map<String, Field> schemaToMap(Set<Field> schema){
		Map<String, Field> map = new LinkedHashMap<String, Field>(schema.size());
		
		for(Field f: schema)
			map.put(f.name, f);
		
		return map;
	}
	
	/**
	 * Updates the collection table's structure to reflect the specified updated list of fields.
	 * Entries are also added/removed from the {@value #CM_TABLE_NAME} table as required.
	 * @param c Connection to the database.
	 * @param name Name of the collection.
	 * @param fields Updated set of fields.
	 * @throws SQLException
	 */
	public static void updateCollectionStructure(Connection c, String name, Set<Field> fields) throws SQLException {
		Set<Field> existingJavaSchema = getCollectionSchema(c, name);
		Set<Field> added = new HashSet<Field>(fields);
		added.removeAll(existingJavaSchema);
		Set<Field> removed = new HashSet<Field>(existingJavaSchema);
		removed.removeAll(fields);
		
		Map<String, Field> existingSqlSchemaMap = schemaToMap(getFields(c.getMetaData(), name));
		Set<Field> updatedSqlSchema = new LinkedHashSet<Field>();
		HsqlColumnConverterFactory columnConverter = HsqlColumnConverterFactory.instance();
		for(Field f: fields){
			if (added.contains(f))
				f = new Field(f.name, columnConverter.getSqlTypeEquivJavaType(f.type), f.editable);
			else
				f = existingSqlSchemaMap.get(f.name);
			updatedSqlSchema.add(f);
		}
		updateTableStructure(c, name, updatedSqlSchema);
		
		for(Field f: added){
			log.println("Creating index for field \""+f.name+"\" for collection \""+name+"\"");
			createIndex(c, name, Collections.singleton(f), false);
		}

		int fcId = getCollectionId(c, name);
		
		log.println("Removing columns from the column mapping table ("+CM_TABLE_NAME+").");
		removeColumnMappingEntries(c, fcId, removed);
		
		log.println("Adding columns to the column mapping table ("+CM_TABLE_NAME+").");
		addColumnMappingEntries(c, fcId, added);
	}
	
	/**
	 * Checks if the a collection with the specified name exists.
	 * @param c Connection to the database.
	 * @param name Name of the collection.
	 * @return <code>true</code> if the specified collection name
	 *         exists in the {@value #FC_TABLE_NAME} table.
	 * @throws SQLException
	 */
	public static boolean collectionExists(Connection c, String name) throws SQLException {
		Set<String> collections = getCollectionNames(c);
		return (collections.contains(name) && tableExists(c, name));
	}
	
	/**
	 * Adds a foreign-key constraint to the specified field of the
	 * specified table.
	 * @param c Connection object to use.
	 * @param tableName Name of the table that uses a foreign-key.
	 * @param fkeyField Name of the field that contains data corresponding to a foreign-key.
	 * @param refTableName Referenced table name.
	 * @param refField Referenced field.
	 * @throws SQLException
	 */
	public static void addForeignKeyConstraint(Connection c, String tableName, Field fkeyField, String refTableName, Field refField) throws SQLException {
		StringBuffer sqlBuf = new StringBuffer();
		sqlBuf.append("alter table ");
		sqlBuf.append(quote(tableName));
		sqlBuf.append(" add constraint ");
		sqlBuf.append(quote("fk_"+tableName+"_"+fkeyField.name+"_"+refTableName+"_"+refField.name));
		sqlBuf.append(" foreign key (");
		sqlBuf.append(quote(fkeyField.name));
		sqlBuf.append(") references ");
		sqlBuf.append(quote(refTableName));
		sqlBuf.append("(");
		sqlBuf.append(quote(refField.name));
		sqlBuf.append(")");
		
		Statement s = c.createStatement();
		s.executeUpdate(sqlBuf.toString());
		s.close();
	}

	/**
	 * Creates the collections table as given by {@link #FC_TABLE_NAME}.
	 * This table has the following fields:
	 * <list>
	 * <li> {@value #FC_ID_FIELD} which is auto-generated and auto-incremented.
	 * <li> {@value #FC_NAME_FIELD}
	 * </list>
	 * Both {@value #FC_ID_FIELD} and {@value #FC_NAME_FIELD} are unique.
	 * @param c Connection object to use.
	 * @throws SQLException
	 */
	public static void createCollectionsTable(Connection c) throws SQLException {
		Set<Field> fcFields = new LinkedHashSet<Field>(2);
		fcFields.add(FC_ID_FIELD);
		fcFields.add(FC_NAME_FIELD);
		HsqlUtil.createTable(c, FC_TABLE_NAME, fcFields, Collections.singleton(FC_ID_FIELD));
		
		HsqlUtil.createIndex(c, FC_TABLE_NAME, Collections.singleton(FC_ID_FIELD), true);
		HsqlUtil.createIndex(c, FC_TABLE_NAME, Collections.singleton(FC_NAME_FIELD), true);
	}
	
	/**
	 * Creates the column mapping table as given by {@link #CM_TABLE_NAME}.
	 * This table has the following fields:
	 * <list>
	 * <li> {@value #CM_FC_ID_FIELD} references the {@value #FC_ID_FIELD} as a foreign-key.
	 * <li> {@value #CM_COL_NAME_FIELD}
	 * <li> {@value #CM_COL_JAVA_TYPE_NAME_FIELD}
	 * </list>
	 * @param c Connection object to use.
	 * @throws SQLException
	 */
	public static void createColumnMappingTable(Connection c) throws SQLException {
		Set<Field> cmFields = new LinkedHashSet<Field>(3);
		cmFields.add(CM_FC_ID_FIELD);
		cmFields.add(CM_COL_NAME_FIELD);
		cmFields.add(CM_COL_JAVA_TYPE_NAME_FIELD);
		HsqlUtil.createTable(c, CM_TABLE_NAME, cmFields, null);
		
		addForeignKeyConstraint(c, CM_TABLE_NAME, CM_FC_ID_FIELD, FC_TABLE_NAME, FC_ID_FIELD);
		
		Set<Field> cmIdxFields = new LinkedHashSet<Field>(2);
		cmIdxFields.add(CM_FC_ID_FIELD);
		cmIdxFields.add(CM_COL_NAME_FIELD);
		HsqlUtil.createIndex(c, CM_TABLE_NAME, cmIdxFields, true);
	}
	
	public static Feature getFeature(ResultSet rs, Set<Field> schema) throws SQLException {
		Feature feat = new Feature();
		for(Field f: schema){
			feat.setAttribute(f, rs.getObject(f.name));
		}
		
		return feat;
	}
	
	public static Iterator<Feature> getFeatures(final ResultSet rs, final Set<Field> javaSchema, final Set<Field> sqlSchema) {
		final Map<Field,HsqlColumnConverter> converters = new HashMap<Field,HsqlColumnConverter>(javaSchema.size()*3/2);
		HsqlColumnConverterFactory converterFactory = HsqlColumnConverterFactory.instance();
		Map<String,Field> sqlSchemaMap = schemaToMap(sqlSchema);
		for(Field f: javaSchema) {
			converters.put(f, converterFactory.getConverter(f.type, converterFactory.getSqlType(sqlSchemaMap.get(f.name).type)));
		}
		return new Iterator<Feature>() {
			private boolean read = false;
			private boolean done = false;
			private boolean advance() {
				if (!done && !read) {
					try {
						read = rs.next();
						done = !read;
					} catch (SQLException e) {
						done = true;
						e.printStackTrace();
					}
					if (done) {
						try {
							rs.close();
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				}
				return !done;
			}
			public boolean hasNext() {
				return advance();
			}
			public Feature next() {
				advance();
				Feature feat = new Feature();
				for(Field f: converters.keySet()) {
					Object cell;
					try {
						cell = rs.getObject(f.name);
					} catch (SQLException e) {
						cell = null;
						e.printStackTrace();
					}
					feat.setAttribute(f, converters.get(f).hsqlToJava(cell));
				}
				read = false;
				return feat;
			}
			public void remove() {
				throw new UnsupportedOperationException("Unable to remove from database stream");
			}
		};
	}
	
	public static Iterator<Feature> getFeatures(Connection c, String fcName) throws SQLException {
		return getFeatures(c, fcName, null);
	}
	
	public static Iterator<Feature> getFeatures(Connection c, String fcName, HsqlConstraint filter) throws SQLException {
		Set<Field> javaSchema = getCollectionSchema(c, fcName);
		Set<Field> sqlSchema = getFields(c.getMetaData(), fcName);
		StringBuffer sqlBuf = new StringBuffer();
		for(Field f: javaSchema){
			if (sqlBuf.length() > 0)
				sqlBuf.append(",");
			sqlBuf.append(quote(f.name));
		}
		sqlBuf.insert(0, "select ");
		sqlBuf.append(" from "+quote(fcName));
		
		List<Object> paramObjs = new ArrayList<Object>();
		
		if (filter != null){
			HsqlColumnConverterFactory converterFactory = HsqlColumnConverterFactory.instance();
			StringBuffer whereBuf = new StringBuffer(filter.getPreparedSqlSnippet());
			paramObjs.addAll(filter.getPreparedSqlParams(converterFactory));
			
			if (whereBuf.length() > 0){
				whereBuf.insert(0, "where ");
				sqlBuf.append(" ");
				sqlBuf.append(whereBuf);
			}
		}
		
		PreparedStatement ps = c.prepareStatement(sqlBuf.toString());
		for(int i=0; i<paramObjs.size(); i++)
			ps.setObject(i+1, paramObjs.get(i));
		
		ResultSet rs = ps.executeQuery();
		return getFeatures(rs, javaSchema, sqlSchema);
	}
	
	public static void addFeatures(Connection c, String fcName, Collection<Feature> features, Set<Field> javaSchema) throws SQLException {
		Set<Field> sqlSchema = getCollectionSchema(c, fcName);
		HsqlColumnConverterFactory converterFactory = HsqlColumnConverterFactory.instance();
		Map<String,Field> sqlSchemaMap = schemaToMap(sqlSchema);
		
		List<String> fieldNames = new ArrayList<String>(javaSchema.size());
		for(Field f: javaSchema)
			fieldNames.add(HsqlUtil.quote(f.name));
		
		String sql = "insert into "+HsqlUtil.quote(fcName)+"("+
		Util.join(",",fieldNames)+") values ("+
		Util.join(",",Collections.nCopies(javaSchema.size(), "?"))+")";
		
		Map<Field, HsqlColumnConverter> converterMap = new HashMap<Field, HsqlColumnConverter>(javaSchema.size());
		try {
			for(Field f: javaSchema) {
				Field sqlField = sqlSchemaMap.get(f.name);
				if (sqlField == null) {
					throw new IllegalArgumentException("Can't map field "+f.name+" from Java schema to SQL schema.");
				}
				Class<?> sqlType = sqlField.type;
				String pseudoSqlType = converterFactory.getSqlType(sqlType);
				HsqlColumnConverter converter = converterFactory.getConverter(f.type, pseudoSqlType);
				if (converter == null) {
					throw new IllegalArgumentException("Can't map field "+f.type+" to "+pseudoSqlType);
				}
				converterMap.put(f, converter);
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			System.out.println("\nSchema appears to have changed, may need to rebuild database.");
			c.close();
			System.exit(1);
		}
		
		PreparedStatement s = c.prepareStatement(sql);
		for(Feature feat: features){
			int i=1;
			for(Field f: javaSchema){
				//HsqlColumnConverter columnConverter = converterFactory.getConverter(f.type, converterFactory.getSqlType(sqlSchemaMap.get(f.name).type));
				HsqlColumnConverter columnConverter = converterMap.get(f);
				s.setObject(i++, columnConverter.javaToHsql(feat.getAttribute(f)));
			}
			s.addBatch();
		}
		s.executeBatch();
		s.close();
	}
	
	public static void main(String[] args) throws Exception {
		Class.forName("org.hsqldb.jdbcDriver");
		Connection c = DriverManager.getConnection("jdbc:hsqldb:file:test.db", "sa", "");
		
		Field path = new Field("path", FPath.class);
		Field label = new Field("label", String.class);
		Field execTime = new Field("exec_time", Date.class);
		Field testField = new Field("test field", Integer.class);
		
		SingleFeatureCollection fc = new SingleFeatureCollection();
		Feature f1 = new Feature();
		f1.setAttribute(path, new FPath(new float[]{0,0, 1,0, 1,1}, false, FPath.SPATIAL_EAST, true));
		f1.setAttribute(label, "feature 1");
		Feature f2 = new Feature();
		f2.setAttribute(path, new FPath(new float[]{10,10, 12,10, 12,12, 10,12}, false, FPath.SPATIAL_EAST, false));
		fc.addFeature(f1);
		fc.addFeature(f2);
		
		String fcName = "test collection";
		if (collectionExists(c, fcName))
			dropCollection(c, fcName);
		createCollection(c, fcName, new LinkedHashSet<Field>(fc.getSchema()));
		
		Feature f3 = new Feature();
		f3.setAttribute(path, new FPath(new float[]{0,0, -1,0, -1,-1}, false, FPath.SPATIAL_EAST, true));
		f2.setAttribute(label, "feature 3");
		f1.setAttribute(execTime, new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01"));
		fc.addFeature(f3);
		updateCollectionStructure(c, fcName, new LinkedHashSet<Field>(fc.getSchema()));
		
		addFeatures(c, fcName, fc.getFeatures(), new LinkedHashSet<Field>(fc.getSchema()));
		
		fc.addField(testField);
		updateCollectionStructure(c, fcName, new LinkedHashSet<Field>(fc.getSchema()));
		
		System.err.println("Features:");
		for(Iterator<Feature> it = getFeatures(c, fcName); it.hasNext(); ) {
			Feature feat = it.next();
			Set<Field> fields = feat.getKeys();
			List<String> vals = new ArrayList<String>();
			for(Field f: (Set<Field>)feat.getKeys()){
				vals.add(f.name+"="+feat.getAttribute(f));
			}
			System.err.println(Util.join(",", vals));
		}
		System.err.println("***");
		
		fc.removeField(testField);
		updateCollectionStructure(c, fcName, new LinkedHashSet<Field>(fc.getSchema()));
		
		System.err.println("Features:");
		for(Iterator<Feature> it = getFeatures(c, fcName); it.hasNext(); ) {
			Feature feat = it.next();
			List<String> vals = new ArrayList<String>();
			for(Field f: (Set<Field>)feat.getKeys()){
				vals.add(f.name+"="+feat.getAttribute(f));
			}
			System.err.println(Util.join(",", vals));
		}
		System.err.println("***");
		
		c.createStatement().execute("shutdown");
		c.close();
	}
}
