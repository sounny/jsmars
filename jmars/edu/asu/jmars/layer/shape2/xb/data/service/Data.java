package edu.asu.jmars.layer.shape2.xb.data.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import edu.asu.jmars.layer.ILayerSchemaProvider;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.layer.shape2.drawingpalette.swing.ViewChangedObserver;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.util.features.CalculatedField;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureEvent;
import edu.asu.jmars.layer.util.features.FeatureListener;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.filetable.FileTable;

public enum Data implements ILayerSchemaProvider, ViewChangedObserver, ListSelectionListener, FeatureListener {

	SERVICE;
	
	private Set<Field> schemaFields = new LinkedHashSet<Field>();
	private Map<String,String> aliasMap=new HashMap<String,String>();
	
	private Map<String,String> aliasDeprecatedMap=new HashMap<String,String>();
	
	
	private List<IDataServiceEventListener> listeners = new ArrayList<>();
	private Layer.LView activeview;
	private Layer activelayer;	
	public static final String ALIAS_DELIM = ":";
	
	public static final IValidator<String> JAVA_IDENTIFIER_VALIDATOR  = new JavaIdentifierValidator();
	public static final IValidator<String> SCHEMA_COLUMN_VALIDATOR = new SchemaColumnValidator();
	public static final IValidator<String> MATH_FUNCTION_VALIDATOR = new MathFunctionValidator();
	
	public static Map<String, Map<String,String>> aliasFromSessionMap=new HashMap<String,Map<String,String>>();
	
	private Data() {}
	
	public void init() {
		LManager.getLManager().getActiveViewObservable().addObserver(this);
	}

	public void addDataEventListener(IDataServiceEventListener listener) {
		listeners.add(listener);
	}
	
	public void removeDataEventListener(IDataServiceEventListener listener) {
		listeners.remove(listener);
	}	
	
	public Set<Field> getData() { // retrieve schema from currently loaded Shape layer
		Set<Field> s = new LinkedHashSet<>();
		if (schemaFields == null) {
			schemaFields = new LinkedHashSet<>();
		}
		s.addAll(schemaFields);
		return s;
	}
	
	public Map<String, String> getAliases() { // retrieve aliases for field names that don't comply with Java identifier
		Map<String, String> m = new HashMap<>();
		m.putAll(aliasMap);
		return m;
	}
	
	public Map<String, String> getAliasesDeprecated() { // retrieve aliases that were created by users by-hand, prior to these changes
		Map<String, String> m = new HashMap<>();
		m.putAll(aliasDeprecatedMap);
		return m;
	}	
	
	public String getAliasForName(String name) {
		return this.aliasMap.get(name);
	}
	
/*
 * for backward compatibility with formulas that used aliases created by the users.
 * iterate over aliasFromSessionMap. 
 * For each entry in aliasFromSessionMap check: get value from entry, which is map <alias :field>.
 * check if that map contains entry with key = possibledeprecatedalias.
 * if found, iterate over that map, and
 * find an entry in that map with  key = possibledeprecatedalias; retrieve its value, which is 'field name'.
 * Next check alaisMap to see if there is entry with the key equal to 'field name'.
 * If such entry is found in aliasMap, update aliasMap by putting key-value pair, like so
 * key = 'field name' value = possibledeprecatedalias and return true.
 * Else return false.
 * 
 * Map entry for this deprecatedaliasmap  is <alias : field.name>, so 'alias' is key
 * Map entry for this aliasMap  is <field.name : alias>, so 'alias' is value
 */
public boolean isDeprecatedAlias(String possibledeprecatedalias) {
	for (Map.Entry<String, Map<String, String>> entry : aliasFromSessionMap.entrySet()) {
		Map<String, String> deprecatedaliasmap = entry.getValue();
		if (!deprecatedaliasmap.containsKey(possibledeprecatedalias)) {
			continue;
		}
		String fieldname = deprecatedaliasmap.get(possibledeprecatedalias);
		if (aliasMap.get(fieldname) != null) {
			aliasMap.put(fieldname, possibledeprecatedalias);
			return true;
		}
	}
	return false;
}


	@Override
	public void doSchema(ShapeLayer shapelayer) {
		Map<Field, CalculatedField> calculatedFields = new LinkedHashMap<Field, CalculatedField>();
		int[] rows = shapelayer.getFileTable().getSelectedRows();
		if (rows == null || rows.length != 1) { return; }

		FeatureCollection fc = shapelayer.getFileTable().getFileTableModel().get(rows[0]);
		Set<Field> stylescollection = (shapelayer.getStyles().getFields());
		calculatedFields.clear();
		calculatedFields.putAll(shapelayer.calcFieldMap.get(fc).getCalculatedFields());

		schemaFields.clear();
		schemaFields.addAll(fc.getSchema());
		schemaFields.addAll(stylescollection);
		schemaFields.remove(Field.FIELD_PATH);
		schemaFields.addAll(calculatedFields.keySet());
		
		aliasMap.clear();    //alaisMap is <field name : alias> validjava a.k.a 'alias'
		for (Field field : schemaFields) {
			String fieldname = field.name;
			String validjava = Data.JAVA_IDENTIFIER_VALIDATOR.validateIdentifier(fieldname);
			aliasMap.put(fieldname, validjava); // key - field.name; value - alias
		}
		
		aliasDeprecatedMap.clear();
		if (!aliasFromSessionMap.isEmpty()) {
			populateAliasFromSession(shapelayer);
		}
		
		if (!aliasDeprecatedMap.isEmpty()) {  //// Map entry for this aliasDeprecatedMap  is <alias : field.name>, so 'alias' is key
			for (Map.Entry<String, String> entry : aliasDeprecatedMap.entrySet()) {
			     String alias = entry.getKey(); 
				 String field = entry.getValue();
				 aliasMap.put(field, alias); //if this overwrites entry from line#109, it's ok
			}
		}
		
		DataServiceEvent dse = new DataServiceEvent();
		for (IDataServiceEventListener listener: listeners) {
			listener.handleDataServiceEvent(dse);
		}
	}

	// for backward compatibility with formulas that used aliases created by the
	// users
	private void populateAliasFromSession(ShapeLayer shapelayer) {
		int r = shapelayer.getFileTable().getSelectedRow();
		String fileName = (String) shapelayer.getFileTable().getValueAt(r, 1);

		if ((aliasFromSessionMap.containsKey(fileName))) {
			for (Map.Entry<String, String> temporaryMapOfAliases : aliasFromSessionMap.get(fileName).entrySet()) {
				String alias = temporaryMapOfAliases.getKey();
				String field = temporaryMapOfAliases.getValue();
				aliasDeprecatedMap.put(alias, field);
			}
		}
	}

	@Override
	public void doSchema(StampLayer stamp) {
	}

	@Override
	public void update(Observable o, Object arg) {
		if (!(arg instanceof Layer.LView)) return;		
		activeview = (Layer.LView)arg;
		activelayer = activeview.getLayer();
		activelayer.provideSchema(this);
	}
	
	/*
	 * update schema when user selects a different row in CustomShapes-FileTable
	 */
	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			if (e.getSource() == shapelayerfiletableselectionmodel()) {
				if (activelayer != null) {
					activelayer.provideSchema(this);
				}
			}
		}
	}
	
	public FeatureCollection getSelectedFeatureCollection() {
		FeatureCollection selectedfc = null;
		if (activelayer != null && activelayer instanceof ShapeLayer) {
			ShapeLayer shapelayer = (ShapeLayer) activelayer;
			int[] rows = shapelayer.getFileTable().getSelectedRows();
			if (rows == null || rows.length != 1) {
				return null;
			}
			selectedfc = shapelayer.getFileTable().getFileTableModel().get(rows[0]);
		}
		return selectedfc;
	}
	
	private ListSelectionModel shapelayerfiletableselectionmodel() {
		ListSelectionModel listselectionmodel = null;
		if (activelayer instanceof ShapeLayer) {
			ShapeLayer l = (ShapeLayer) activelayer;
			FileTable filetable = l.getFileTable();
			if (filetable != null) {
				listselectionmodel = filetable.getSelectionModel();
			}
		}
		return listselectionmodel;
	}
	
	private static class JavaIdentifierValidator implements IValidator<String> {
			List<String> javakeywords = Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
					"const", "continue", "default", "do", "double", "else", "enum", "extends", "false", "final",
					"finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface",
					"long", "native", "new", "null", "package", "private", "protected", "public", "return", "short",
					"static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient",
					"true", "try", "void", "volatile", "while");
			
		@Override
		public String validateIdentifier(String identifier) {
			if (identifier == null || identifier.isEmpty()) {
				return null;
			}
			// Check if identifier is a Java keyword
			if (javakeywords.contains(identifier)) {
				return "_" + identifier;
			}
			StringBuilder sb = new StringBuilder();
			// Check if first character is valid Java identifier start character
			if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
				identifier = "_" + identifier;
			}
			// Construct new identifier by replacing invalid characters with underscores
			for (int i = 0; i < identifier.length(); i++) {
				char c = identifier.charAt(i);
				if (Character.isJavaIdentifierPart(c)) {
					sb.append(c);
				} else {
					sb.append("_");
				}
			}
			return sb.toString();
		}
	}
	
	
	private static class SchemaColumnValidator implements IValidator<String> {
		@Override
		public String validateIdentifier(String input) {
			StringBuilder output = new StringBuilder();
			int startIndex = 0;
			while (true) {
				int colonIndex1 = input.indexOf(ALIAS_DELIM, startIndex);
				if (colonIndex1 == -1) {
					output.append(input.substring(startIndex));
					break;
				}
				int colonIndex2 = input.indexOf(ALIAS_DELIM, colonIndex1 + 1);
				if (colonIndex2 == -1) {
					output.append(input.substring(startIndex));
					break;
				}
				// substring - is possibly a column name, sense the surrounding delimiters
				String substring = input.substring(colonIndex1 + 1, colonIndex2);
				String maybealias = Data.SERVICE.getAliasForName(substring);

				if (maybealias != null) {
					output.append(input.substring(startIndex, colonIndex1));
					output.append(maybealias);
					startIndex = colonIndex2 + 1;
				} else {
					output.append(input.substring(startIndex, colonIndex1));
					output.append(ALIAS_DELIM);
					output.append(substring);
					output.append(ALIAS_DELIM);
					startIndex = colonIndex2 + 1;
				}
			}
			return output.toString();
		}
		
		@Override
		public List<String> validateAsArray(String input) {
			List<String> output = new ArrayList<>();			
    
			int startIndex = 0;
			while (true) {
				int colonIndex1 = input.indexOf(ALIAS_DELIM, startIndex);
				if (colonIndex1 == -1) {
					output.add(input.substring(startIndex));
					break;
				}
				int colonIndex2 = input.indexOf(ALIAS_DELIM, colonIndex1 + 1);
				if (colonIndex2 == -1) {
					output.add(input.substring(startIndex));
					break;
				}
				// substring - is possibly a column name, sense the surrounding delimiters
				String substring = input.substring(colonIndex1 + 1, colonIndex2);
				String maybealias = Data.SERVICE.getAliasForName(substring);

				if (maybealias != null) {
					output.add(input.substring(startIndex, colonIndex1));
					output.add(maybealias);
					startIndex = colonIndex2 + 1;
				} else {
					output.add(input.substring(startIndex, colonIndex1));
					output.add(ALIAS_DELIM);
					output.add(substring);
					output.add(ALIAS_DELIM);
					startIndex = colonIndex2 + 1;
				}
			}
			return output;
		}		
		
	}
	
	
	//replace values in parenthesis with "0", like java.Math in Eclipse does
	//for ex, input=cos(double) - output=cos(0); input=pow(double,double) - output=pow(0,0)
	private static class MathFunctionValidator implements IValidator<String> {
		@Override
		public String validateIdentifier(String input) {
			if (input == null || input.isEmpty()) {
				return input;
			}

			int start = input.indexOf('(');
			int end = input.indexOf(')');

			if (start < 0 || end < 0) {
				return input;
			}

			String argsString = input.substring(start + 1, end);
			if (argsString.isEmpty()) {
				return input;
			}

			String[] args = argsString.split(",");

			StringBuilder sb = new StringBuilder();
			sb.append(input.substring(0, start + 1));
			for (int i = 0; i < args.length; i++) {
				sb.append("0");
				if (i < args.length - 1) {
					sb.append(",");
				}
			}
			sb.append(input.substring(end));

			return sb.toString();
		}	
	}

	@Override
	public void receive(FeatureEvent e) { // when feature collection changes based on additon/change of fields, we
											// update schema
		if (activelayer != null) {
			activelayer.provideSchema(this);
		}
	}

}
