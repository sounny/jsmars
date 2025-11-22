package edu.asu.jmars.layer.util.features;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.FillStyle;
import edu.asu.jmars.util.LineType;

/**
 * A class for manipulating Features via SQL-like commands.
 * Public methods are:
 *
 *     // parses out the line and runs the appropriate command.
 *     // statusBar may be null.
 *     public void FeatureSQL( String line, FeatureCollection fc, JLabel statusBar);
 * 
 *     // moves the selected rows by the delta specified in endPoint.
 *     public static void move( FeatureCollection fc, Point2D endPoint, String where) {
 *
 *     // selects the features that correspond to the inputted predicate.
 *     public static void select( FeatureCollection fc, String where) {
 *
 *     // "updates" the rows according to setString, which consists of both
 *     // set AND predicate.  The whereClause is the just the predicate, although
 *     // this is a bit of a duplication.
 *     public static void update( FeatureCollection fc, String setString, String whereClause) {
 *
 *     // deletes the features dictated by the predicate from the FeatureTableModel.
 *     public static void delete( FeatureCollection fc, String where) {
 *
 */
public class FeatureSQL {
	private static DebugLog log = DebugLog.instance();
	
	static {
		// causes the driver to register itself with jdbc
		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch(ClassNotFoundException cnfe){
			log.aprintln(cnfe);
		}
	}
	private static final String featureTable = "foobar";
	private static final String keyColumnName = "key";
	
	private static final int SET_COMMAND    = 1;
	private static final int MOVE_COMMAND   = 2;
	private static final int SELECT_COMMAND = 3;
	private static final int DELETE_COMMAND = 4;
	
	private final String dbUrl;
	{
		String file = "test";
		try {
			file = File.createTempFile("jmars", ".hsqldb").getAbsolutePath();
		} catch (IOException e) {
			log.aprintln(e);
		}
		dbUrl = "jdbc:hsqldb:" + file;
	}
	
	private LexicalAnalyzer  lex          = null;
	private String resultStr;
	private String statusStr;
	
	public String getResultString() {
		return resultStr;
	}
	
	public String getStatusString() {
		return statusStr;
	}
	
	/**
	 * the calling program must define how result  messages are displayed.
	 */
	void printResult(String message) {
		statusStr = message;
	}
	
	/**
	 * constructor: class attributes are set, the line is parsed out, and the 
	 * actions defined by the line performed.
	 */
	public FeatureSQL(String line, FeatureCollection fc,
			MemoryFeatureIndex index, Set<Feature> selections) {
		if (line!=null && fc!=null){
			this.lex  = new LexicalAnalyzer( line);
			parseLine(fc, index, selections);
		}
	}
	
	private static void cleanup(Connection conn) {
		try {
			cleanTable(conn);
		} catch (Exception e) {
			log.println(e);
		}
		try {
			conn.close();
		} catch (Exception e){
			log.println(e);
		}
	}
	
	/** moves the selected rows by the given world coordinate offset. */
	public void move(FeatureCollection fc, MemoryFeatureIndex index, Point2D delta, String where) {
		Connection conn = null;
		
		try {
			AffineTransform at = AffineTransform.getTranslateInstance(delta.getX(), delta.getY());
			
			conn = DriverManager.getConnection(dbUrl);
			Statement  st = conn.createStatement();
			
			makeTempTable(conn, st, fc);
			
			// run the query
			String query = "select "+keyColumnName+" from "+featureTable+" "+ filterSQL( where);
			
			ResultSet rs = st.executeQuery( query );
			
			// get the resulting key values and move the appropriate features.
			Map<Feature,Object> featureValues = new HashMap<Feature,Object>();
			while (rs.next()) {
				int key = rs.getInt(keyColumnName);
				Feature feature = fc.getFeature(key);
				FPath path = index.getWorldPath(feature);
				GeneralPath nudged = new GeneralPath();
				nudged.append(path.getShape().getPathIterator(at), false);
				FPath nudgedPath = new FPath(nudged,FPath.WORLD).convertTo(path.getCoordSystem());
				featureValues.put(feature, nudgedPath);
			}
			
			// do all the setting of the path fields at once.
			fc.setAttributes(Field.FIELD_PATH, featureValues);
			
			resultStr = featureValues.size() + " rows moved";
		}  catch (SQLException sqle){
			resultStr = "SQL error moving rows: " + sqle.getMessage();
			System.out.println( resultStr );
		} catch (Exception e){
			resultStr = "Error moving rows: " + e.getMessage();
			System.out.println( resultStr );
		} finally {
			if (conn != null) {
				cleanup(conn);
			}
		}
	}
	
	/** 
	 * returns an array of row indices that correspond to the inputted where clause.
	 */
	public void select( FeatureCollection fc, Set<Feature> selections, String where) {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(dbUrl);
			Statement  st = conn.createStatement();

			makeTempTable(conn, st, fc);

			// run the query
			String query = "select "+keyColumnName+" from "+featureTable+" "+ filterSQL( where);
			ResultSet rs = st.executeQuery( query );

			// first off, set all the Features to unselected.
			selections.clear();

			// get the resulting key values and set selection on for those rows
			Set<Feature> selectedFeatures = new HashSet<Feature>(); 
			while(rs.next()) {
				int key = rs.getInt(keyColumnName);
				Feature feature = fc.getFeature( key);
				selectedFeatures.add(feature);
			}
			selections.addAll(selectedFeatures);
			
			resultStr = selectedFeatures.size() + " rows selected";
		} catch (SQLException sqle) {
			resultStr = "SQL error selecting rows: " + sqle.getMessage();
			System.out.println(resultStr);
		} catch (Exception e) {
			resultStr = "error selecting rows: " + e.getMessage();
			log.println(resultStr);
		}
		finally {
			if (conn != null) {
				cleanup(conn);
			}
		}
	}


	/**
	 * runs the inputted "update" command on the inputted table.
	 */
	public void update(FeatureCollection fc, String setString) {
		Connection conn = null;
		Map featureMap = new HashMap();

		Field field = null;
		Class cl = null;
		String name = null;

		try {
			conn = DriverManager.getConnection(dbUrl);

			Statement  st = conn.createStatement();

			// make temp table 
			makeTempTable(conn, st, fc);

			// run the update
			String updateString = "update "+featureTable+" set " + filterSQL( setString);
			int rowsChanged = st.executeUpdate( updateString);

			// get every row and check if a change has been made.
			// Note that a query after an update is apparently not stable.
			String query  = "select * from "+featureTable;
			ResultSet rs     = st.executeQuery( query);

			// We can't do a selection then an update on JUST THAT SELECTION.
			// The update will be on the entire table. So we do the update
			// with the predicate then iterate through ALL the rows adding them
			// to the feature-changed list.

			// TODO: Only add features to the feature-changed list if
			// at least one attribute was changed in the update command.
			List  schema = fc.getSchema();
			while(rs.next()){
				int row = rs.getInt(keyColumnName);
				Feature feature = fc.getFeature( row);
				Map fieldMap = new HashMap();
				Iterator si = schema.iterator();
				while (si.hasNext()){
					field = (Field)si.next();
					name = getSQLname( field.name);
					if (!field.editable || field == Field.FIELD_PATH)
						continue;

					cl = field.type;
					if (cl==Integer.class || 
							cl==Double.class  ||
							cl==String.class){
						fieldMap.put( field, rs.getObject(name));
					} 
					else if (cl==Float.class){
						fieldMap.put(field, Float.valueOf(rs.getFloat(name)));
					}
					else if (cl==Color.class){
						Integer result = (Integer)rs.getObject(name);
						if (result==null){
							fieldMap.put( field, null);
						} else {
							fieldMap.put( field, new Color( result.intValue()));
						}
					}
					else if (cl==LineType.class){
						Integer result = (Integer)rs.getObject(name);
						if (result==null){
							fieldMap.put( field, null);
						} else {
							fieldMap.put( field, new LineType( result.intValue()));
						}
					}
					else if (cl==Boolean.class){
						Boolean b = (Boolean)rs.getBoolean(name);
						if (b==null){
							fieldMap.put( field, null);
						} else {
							fieldMap.put( field, b);
						} 
					}
					else if (cl==FillStyle.class) {
						Integer result = (Integer)rs.getObject(name);
						if (result==null){
							fieldMap.put( field, null);
						} else {
							fieldMap.put( field, new FillStyle( result.toString()));
						}						
					}
				}
				featureMap.put( feature, fieldMap);
			}

			// everything is fine. Put all the stuff into the fc.
			fc.setAttributes( featureMap);
			resultStr = rowsChanged + " rows updated";
		} catch (SQLException sqle) {
			resultStr = "SQL error while updating: " + sqle.getMessage();
			log.aprintln( resultStr);
		} catch (Exception e) {
			resultStr = "Error while updating: " + e.getMessage();
			if (field != null)
				resultStr += " [field="+field.name+","+"class="+field.type.getClass().getName()+"]";
			
			log.aprintln( resultStr);
		} finally {
			if (conn != null) {
				cleanup(conn);
			}
		}
	}

	/** 
	 * returns an array of row indices that correspond to the inputted where clause.
	 */
	public void delete( FeatureCollection fc, String where) {
		Connection conn = null;

		try {
			conn = DriverManager.getConnection(dbUrl);
			Statement  st = conn.createStatement();

			makeTempTable(conn, st, fc);

			// run the query
			String query = "select "+keyColumnName+" from "+featureTable+" "+ filterSQL( where);
			ResultSet rs = st.executeQuery( query );

			// get the resulting key values and set selection on for those rows
			List<Feature> removeRows = new ArrayList<Feature>();
			while(rs.next()){
				int key = rs.getInt(keyColumnName);
				removeRows.add(fc.getFeature(key));
			}
			fc.removeFeatures( removeRows);

			resultStr = removeRows.size() + " rows deleted";
		} catch (SQLException sqle) {
			resultStr = "SQL error selecting rows: " +   sqle.getMessage();
			System.out.println( resultStr);
		} catch (Exception e) {
			resultStr = "error selecting rows: " + e.getMessage();
			log.println( resultStr);
		} finally {
			if (conn != null) {
				cleanup(conn);
			}
		}
	
	}
	
	//  Make a temporary table that is a copy of the feature table for the sql statements 
	// to work off of.
	private static void makeTempTable(Connection conn, Statement st, FeatureCollection fc) throws SQLException {
		cleanTable(conn);
		
		// make temporary table
		String createSql = "create table "+featureTable+" (";
		String insertSql = "insert into "+featureTable+" values (";
		List<Field> schema = fc.getSchema();
		for (Field field: schema) {
			String columnName = getSQLname( field.name);
			String columnType = getSQLtype( field.type);
			createSql += columnName + " " + columnType + ", ";
			insertSql += "?,";
		}
		createSql += " "+keyColumnName + " " + getSQLtype(Integer.class) + ")";
		insertSql += "?)";
		st.execute(createSql);
		
		// populate the temp table
		int[] errorCounts = new int[schema.size()];
		Throwable[] errors = new Exception[schema.size()];
		PreparedStatement ps = conn.prepareStatement(insertSql);
		try {
			for (int i=0; i< fc.getFeatures().size(); i++) {
				ps.clearParameters();
				Feature f = fc.getFeature(i);
				for (int j = 1; j <= schema.size(); j++) {
					Field field = schema.get(j-1);
					Object val = f.getAttribute(field);
					try {
						if (field.type == FPath.class) {
							// do nothing
						} else if (val == null) {
							// the prepared statement should default all fields to null, so do nothing
						} else if ( field.type == Integer.class) {
							ps.setInt(j, (Integer)val);
						} else if ( field.type == Float.class ) {
							ps.setFloat(j, (Float)val);
						} else if ( field.type == Double.class ) {
							ps.setDouble(j, (Double)val);
						} else if ( field.type == Color.class ) {
							Color colorObj = (Color)val;
							ps.setInt(j, colorObj.getRGB());
						} else if ( field.type == LineType.class ) {
							LineType lineObj = (LineType)val;
							ps.setInt(j, lineObj.getType());
						} else if ( field.type == Boolean.class ) {
							ps.setBoolean(j, (Boolean)val);
						} else if ( field.type == String.class ) {
							ps.setString(j, (String)val);
						} else if ( field.type == FillStyle.class ) {
							FillStyle fillStyle = (FillStyle)val;
							ps.setString(j,  fillStyle.getType());
						} else {
							throw new IllegalStateException("Unrecognized field class " + field.type);
						}
					} catch (Exception e) {
						errors[j-1] = e;
						errorCounts[j-1] ++;
					}
				}
				ps.setInt(schema.size()+1, i);
				ps.execute();
			}
		} finally {
			ps.close();
		}
		for (int i = 0; i < schema.size(); i++) {
			if (errors[i] != null) {
				while(errors[i].getCause() != null) {
					errors[i] = errors[i].getCause();
				}
				log.aprintln("Field " + i + " (" + schema.get(i).name + ") could not be set " + errorCounts[i] + " times, last error was:");
				log.aprintln(errors[i]);
			}
		}
	}
	
	// returns an SQL-compatible version of the inputted ID.
	private static String getSQLname( String name){
		return name.replaceAll(" ","_").replaceAll("\\[", " ").replaceAll("\\]", " ");
	}
	
	private static void cleanTable(Connection conn) throws SQLException {
		conn.createStatement().execute("drop table " + featureTable + " if exists cascade");
	}
	
	// returns the SQL-compatible string of the inputted class.
	private static String getSQLtype(Class<?> cl) {
		if (cl == Integer.class) {
			return "int";
		} else if (cl == Float.class) {
			return "float";
		} else if (cl == Double.class) {
			return "double precision";
		} else if (cl == Color.class) {
			return "int";
		} else if (cl == LineType.class) {
			return "int";
		} else if (cl == Boolean.class) {
			return "boolean";
		} else if (cl == FillStyle.class) {
			return "varchar";
		} else {
			return "varchar";
		}
	}

	// replaces invalid IDs with valid ones in the inputted string.
	private static String filterSQL( String where){
		StringBuffer    str = new StringBuffer();
		
		if (where!=null && where.length() >0){
			LexicalAnalyzer lex = new LexicalAnalyzer( where);
			
			for ( LexicalAnalyzer.Token token = lex.getNextToken();
			      token.value != LexicalAnalyzer.EOL;
			      token = lex.getNextToken()){
				if (token.value == LexicalAnalyzer.ID){
					str.append( " " + getSQLname(token.str));
				} 
				else {
					str.append( " " + token.str);
				}
			}
		}
		return str.toString();
	}

	// parses out the line that was input and runs the specified action.
	private void parseLine(FeatureCollection fc, MemoryFeatureIndex index, Set<Feature> selections){
		Point2D param;
		String endClause;
		int command = getCommand();
		
		switch (command) {
			case SET_COMMAND:
				endClause = getEndClause();
				if (endClause == null){
					return;
				}
				update( fc, endClause);
				printResult( getResultString());
				return;
			case SELECT_COMMAND:
				endClause = getEndClause();
				if (endClause == null){
					return;
				}
				select(fc, selections, endClause);
				printResult( getResultString());
				return;
			case MOVE_COMMAND:
				param = getMoveParameters();
				if (param == null){
					return;
				}
				endClause = getEndClause();
				if (endClause == null){
					return;
				}
				move(fc, index, param, endClause);
				printResult( getResultString());
				return;
			case DELETE_COMMAND:
				endClause = getEndClause();
				if (endClause == null){
					return;
				}
				delete( fc, endClause);
				printResult( getResultString());
				return;
				
			default:
				// not needed: lower methods take care of this.
				// printResult( "Unrecognized Command.");
				return; 
			}
	}
	
	// get the entire remaining line, replacing any color specification with
	// its integer equivalent.
	private String getEndClause(){
		StringBuffer str = new StringBuffer();
		for ( FeatureSQL.LexicalAnalyzer.Token token = lex.getNextToken();
		      token.value != FeatureSQL.LexicalAnalyzer.EOL;
		      token = lex.getNextToken()){
			if (token.value == LexicalAnalyzer.ID && (token.str.equals("color") || token.str.equals("rgb"))){
				int colorSpec = getColorSpec();
				if (colorSpec == 0){
					return null;
				}
				str.append( colorSpec + " ");
			} else {
				str.append( token.str + " ");
			}
		}
		return str.toString();
	}

	// returns the command of the line.
	private int getCommand(){
		LexicalAnalyzer.Token token = lex.getNextToken();
		if (token.value == LexicalAnalyzer.ID){
			if ( token.str.toLowerCase().startsWith("upd")) {
				token = lex.getNextToken();
				if (token.value == LexicalAnalyzer.ID && token.str.toLowerCase().startsWith("set")){
					return SET_COMMAND;
				}
				else {
					printResult("Update command must be followed by 'set'");
					return 0;
				}
			} else if ( token.str.toLowerCase().startsWith("sel")) {
				token = lex.getNextToken();
				if (token.value == LexicalAnalyzer.ID && token.str.toLowerCase().startsWith("row")){
					return SELECT_COMMAND;
				}
				else if (token.value == LexicalAnalyzer.MULTIPLY){
					return SELECT_COMMAND;
				}
				else {
					printResult("Select command must be followed by 'rows' or '*'");
					return 0;
				}
			} else if ( token.str.toLowerCase().startsWith("mov")) {
				token = lex.getNextToken();
				if (token.value == LexicalAnalyzer.ID && token.str.toLowerCase().startsWith("row")){
					return MOVE_COMMAND;
				}
				else {
					printResult("Move command must be followed by 'rows'");
					return 0;
				}
			} else if ( token.str.toLowerCase().startsWith("del")) {
				token = lex.getNextToken();
				if (token.value == LexicalAnalyzer.ID && token.str.toLowerCase().startsWith("row")){
					return DELETE_COMMAND;
				}
				else {
					printResult("Delete command must be followed by 'rows'");
					return 0;
				}
			}
		}
		printResult("Unknown command: " + token.str + "  (should be either 'move' 'select rows' or 'update set')");
		return 0;
	}
	
	private int getColorSpec(){
		LexicalAnalyzer.Token token;
		
		// get a color spec.
		token = lex.getNextToken();
		if (token.value != LexicalAnalyzer.LEFT_PAREN){
			printResult("Bad color specification(()");
			return 0;
		}
		token = lex.getNextToken();
		if (token.value != LexicalAnalyzer.NUMBER){
			printResult("Bad color specification(red)");
			return 0;
		}
		int red = Integer.parseInt( token.str);
		if (red <0 || red > 255) {
			printResult("Bad value for red");
			return 0;
		}
		
		token = lex.getNextToken();
		if (token.value != LexicalAnalyzer.COMMA){
			printResult("Bad color specification(r,g)");
			return 0;
		}
		token = lex.getNextToken();
		if (token.value != LexicalAnalyzer.NUMBER){
			printResult("Bad color specification(green)");
			return 0;
		}
		int green = Integer.parseInt( token.str);
		if (green <0 || green > 255) {
			printResult("Bad value for green");
			return 0;
		}
		
		
		token = lex.getNextToken();
		if (token.value != LexicalAnalyzer.COMMA){
			printResult("Bad color specification(g,b)");
			return 0;
		}
		token = lex.getNextToken();
		if (token.value != LexicalAnalyzer.NUMBER){
			printResult("Bad color specification(blue)");
			return 0;
		}
		int blue = Integer.parseInt( token.str);
		if (blue <0 || blue> 255) {
			printResult("Bad value for blue");
			return 0;
		}
		
		token = lex.getNextToken();
		if (token.value != LexicalAnalyzer.RIGHT_PAREN){
			printResult("Bad color specification())");
			return 0;
		}
		
		return (new Color( red, green, blue)).getRGB();
	} // end: getColorSpec()
	


	private Point2D getMoveParameters() {
		FeatureSQL.LexicalAnalyzer.Token token;
		
		token = lex.getNextToken();
		if (token.value != LexicalAnalyzer.LEFT_PAREN){
			printResult("Bad move parameter specification(()");
			return null;
		}
		token = lex.getNextToken();
		if (token.value != LexicalAnalyzer.NUMBER){
			printResult("Bad move parameter specification(lon)");
			return null;
		}
		double lon =  new Double( token.str).doubleValue();
		
		token = lex.getNextToken();
		if (token.value != LexicalAnalyzer.COMMA){
			printResult("Bad move parameter specification(lon,lat)");
			return null;
		}
		token = lex.getNextToken();
		if (token.value != LexicalAnalyzer.NUMBER){
			printResult("Bad move parameter specification(lat)");
			return null;
		}
		double lat = new Double( token.str).doubleValue();
		
		token = lex.getNextToken();
		if (token.value != LexicalAnalyzer.RIGHT_PAREN){
			printResult("Bad move parameter specification())");
			return null;
		}
		
		return new Point2D.Double( lon, lat);
	}



	// breaks the inputted line into easily digestible bits.
	private static class LexicalAnalyzer {

		static final int EOL        = 0;
		static final int BAD_TOKEN  = 1;
		static final int LEFT_PAREN = 101;
		static final int RIGHT_PAREN= 102;
		static final int COMMA      = 103;
		static final int ID         = 104;
		static final int STRING     = 105;
		static final int NUMBER     = 106;
		
		// relationals
		static final int EQUALS     = 111;
		static final int GT         = 112;
		static final int LT         = 113;
		static final int GTE        = 114;
		static final int LTE        = 115;
		static final int NOT_EQUAL  = 116;
		
		// arithmetic 
		static final int ADD        = 121;
		static final int SUBTRACT   = 122;
		static final int MULTIPLY   = 123;
		static final int DIVIDE     = 124;
		
		
		private String  line;
		private int     count;
		
		/**
		 * constructor: the line to be tokenized is set.
		 */
		public LexicalAnalyzer( String line){
			this.line = new String( line);
			count = 0;
		}
		
		private char get(){
			if (count >= line.length()){
				return (char)EOL;
			} else {
				return line.charAt( count++ );
			}
		}
		
		private void unget(){
			if (count>0){
				count--;
			}
		}
		
		
		public Token getNextToken() {
			StringBuffer id = new StringBuffer();
			
			char ch = get();
			while (ch != EOL){
				id = new StringBuffer();
				switch (ch)
					{
					case '=':
						return new Token( EQUALS, "=");
					case '(':
						return new Token( LEFT_PAREN, "(");
					case ')':
						return new Token( RIGHT_PAREN, ")");
					case ',':
						return new Token( COMMA, ",");
						
					case '>':
						ch = get();
						if (ch == '='){
							return new Token( GTE, ">=");
						}
						else {
							unget();
							return new Token( GT, ">");
						}
					case '<':
						ch = get();
						if (ch == '='){
							return new Token( LTE, "<=");
						}
						else {
							unget();
							return new Token( LT, "<");
						}
					case '!':
						ch = get();
						if (ch == '='){
							return new Token( NOT_EQUAL, "!=");
						}
						else {
							return new Token( BAD_TOKEN, null);
						}
						
					case '/':
						return new Token( DIVIDE, "/");
					case '*':
						return new Token( MULTIPLY, "*");
					case '+':
						return new Token( ADD, "+");
					case '-':
						char ch1 = get();
						if (Character.isDigit(ch1) || ch1=='.'){
							String num = getNumber(ch1);
							return new Token( NUMBER, String.valueOf(ch) + num);
						} else {
							return new Token( SUBTRACT, "-");
						} 
					case '.':
						char ch2 = get();
						if (Character.isDigit(ch2)){
							String num = getNumber(ch2);
							return new Token( NUMBER, String.valueOf(ch) + num);
						} else { // has to be a dangling '.', and that's bad.
							unget();
							return new Token( BAD_TOKEN, ".");
						}
						
					case '\'':  // strings are single-quotation delimited.
						id.append( ch);
						do {
							ch = get();
							id.append( ch);
						} while (ch != '\'' && ch!=EOL);
						if (ch =='\''){
							String idString = id.toString();
							id = new StringBuffer();
							return new Token( STRING, idString);
						} else {
							return new Token( BAD_TOKEN, null);
						}
						
					case '"':  // get a string, but delimit it with single-quotes.
						id.append( '\'');
						do {
							ch = get();
							if (ch == '"'){
								id.append('\'');
							} else {
								id.append( ch);
							}
						} while (ch != '"' && ch!=EOL);
						if (ch =='"'){
							String idString = id.toString();
							id = new StringBuffer();
							return new Token( STRING, idString);
						} else {
							return new Token( BAD_TOKEN, null);
						}
						
					case '[':  // get a column.
						id.append( ch);
						do {
							ch = get();
							id.append( ch);
						} while (ch != ']' && ch!=EOL);
						if (ch ==']'){
							String idString = id.toString();
							id = new StringBuffer();
							return new Token( ID, idString);
						} else {
							return new Token( BAD_TOKEN, null);
						}
						
					default: 
						// get an ID.
						if ( Character.isLowerCase(ch)  || 
						     Character.isUpperCase( ch) || 
						     ch=='_'){
							do {
								id.append( ch);
								ch = get();
							} while ( ch!=EOL &&
								  (Character.isLowerCase(ch)  || 
								   Character.isUpperCase( ch) ||
								   Character.isDigit(ch)      ||
								   ch=='_' )
								  );
							if (ch!=EOL){
								unget();
							}
							return new Token( ID, id.toString());
						}
						
						// take a number
						if (Character.isDigit(ch)){
							String num1 = getNumber(ch);
							return new Token( NUMBER, num1);
						}
						break;
					}
				ch = get();
			}  // end:while
			
			// we have reached the end of the line.
			return new Token( EOL, "");
		}
		
		String getNumber( char ch){
			char chr = ch;
			StringBuffer buff = new StringBuffer();
			if (chr!='.'){
				do {
					buff.append( chr);
					chr = get();
				} while ( Character.isDigit(chr));
				if (chr!='.'){
					if (chr !=EOL){
						unget();
					}
					return buff.toString();
				}
			}
			do {
				buff.append( chr);
				chr = get();
			} while ( Character.isDigit(chr));
			if (chr != EOL){
				unget();
			}
			return buff.toString();
		}
		
		
		public class Token {
			int    value;
			String str;
			
			public Token( int v, String s){
				value = v;
				str   = s;
			}
		}
		
	} // end: static class LexicalAnalyzer
	

}  // end: class SQL

