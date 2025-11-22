package edu.asu.jmars.layer.shape2.swing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;


public class ScriptReadoutTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		private final String QUERY = "Script";
		private final String RESULT = "Result";
		
		private List<ScriptResultObject> script_result = new ArrayList<>(); 

		private final String[] columnNames = new String[] { QUERY,     RESULT};
		private final Class[] columnClass = new Class[] {   String.class, String.class };
				
		protected String[] columnToolTips = {
			    "Script (query) that you ran", // sql query, or script that user ran
			    "Script result", //result
		 };
		
		public ScriptReadoutTableModel() {}

		public void addData(Map<String, String> scriptwithresults) {
			this.script_result.clear();
			for (Map.Entry<String, String> entry : scriptwithresults.entrySet()) {
				ScriptResultObject sro = new ScriptResultObject(entry.getKey(), entry.getValue());
				this.script_result.add(sro);
			}
			fireTableDataChanged();
		}
		
	
		public void reset() {
			this.script_result.clear();
			fireTableDataChanged();
		}

		@Override
		public String getColumnName(int columnIndex) {
			return columnNames[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnClass[columnIndex];
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		public String[] getColumnToolTips() {
			return columnToolTips;
		}

		@Override
		public int getRowCount() {
			return this.script_result.isEmpty() ? 0 : this.script_result.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (this.script_result.isEmpty())
				return null;

			ScriptResultObject row = script_result.get(rowIndex);
			
			if (row == null) return null;
			
			//actual script (or query)
			if (getColumnName(columnIndex).equals(QUERY)) {
				return row.getScript();
			}
			// result
			else if (getColumnName(columnIndex).equals(RESULT)) {
				return row.getResult();
				
			}
			return null;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int colIndex) {
			return false;
		}
		
		private class ScriptResultObject {
			private String script;
			private String result;

			private ScriptResultObject(String script, String result) {
				this.script = script;
				this.result = result;
			}

			private String getScript() {
				return script;
			}

			private String getResult() {
				return result;
			}

		}
}
