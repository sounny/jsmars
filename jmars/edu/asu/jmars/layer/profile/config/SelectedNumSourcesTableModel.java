package edu.asu.jmars.layer.profile.config;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.REMOVE;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.profile.ProfileFactory;
import edu.asu.jmars.layer.profile.swing.ButtonColumn;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;


public class SelectedNumSourcesTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	private final String TITLE_COL = "Numeric Data";
	private final String REMOVE_COL = " ";
	private List<MapSource> selectedNumSources = new ArrayList<>();	
	private ProfileFactory controller;
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static Icon trash = new ImageIcon(ImageFactory.createImage(REMOVE.withDisplayColor(imgColor)));
	private JTable jTable;	
	
	private final String[] columnNames = new String[] { TITLE_COL,   REMOVE_COL };
	private final Class[] columnClass = new Class[] {  String.class, ButtonColumn.class};
			
	protected String[] columnToolTips = {
		    "Selected numeric source(s) for chart",
		    "<html><body>"
					+ "<div >Remove numeric data from chart.</div>"
					+ "<div>Note, this action removes numeric data from configuration only.</div>"
					+ "<div>It does not delete this numeric data from JMARS.</div></body></html>" 	
	 };
	
	public SelectedNumSourcesTableModel(ProfileFactory control) {
		this.controller = control;		
	}	

	public void addData(List<MapSource> userSelectedSources, boolean isMultipleSources) {
		if (!isMultipleSources) {
			this.selectedNumSources.clear();
		}
		for (MapSource mapsourse : userSelectedSources) {
			if (!this.selectedNumSources.contains(mapsourse)) {
				selectedNumSources.add(mapsourse);
			}
		}
		fireTableDataChanged();
	}
	
	
	public List<MapSource> getModelData() {
		List<MapSource> modeldata = new ArrayList<>();
		modeldata.addAll(selectedNumSources);
		return modeldata;
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
		return selectedNumSources.isEmpty() ? 0 : selectedNumSources.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (selectedNumSources.isEmpty())
			return null;

		MapSource row = selectedNumSources.get(rowIndex);

		// map source name
		if (getColumnName(columnIndex).equals(TITLE_COL)) {		
			return row.getTitle();
		}
		//remove
		else if (getColumnName(columnIndex).equals(REMOVE_COL)) {
			return trash;
		}	
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int colIndex) {
		if (getColumnName(colIndex).equals(REMOVE_COL))
			return true;
		return false;
	}

	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if (selectedNumSources.isEmpty())
			return;

		MapSource mapsource = this.selectedNumSources.get(rowIndex);

		if (REMOVE_COL.equals(getColumnName(columnIndex))) {			
			if (mapsource != null) {										
				this.selectedNumSources.remove(mapsource);				
				List<MapSource> selected = new ArrayList<>();
				selected.addAll(this.selectedNumSources);
				this.controller.userAddedMapSource(selected);
			}
		}		
	}	

	public String[] getInitialColumnNames() {
		return columnNames;
	}
	
	public void withTable(JTable vartbl) {
		this.jTable = vartbl;	
	}			
	
}

