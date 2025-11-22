package edu.asu.jmars.swing.landmark.search.swing;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import javax.swing.table.AbstractTableModel;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView.MarsFeature;
import edu.asu.jmars.parsers.gis.CoordinatesParser.Ordering;
import edu.asu.jmars.swing.landmark.search.LandmarkSearchPanel.SearchMode;
import edu.asu.jmars.util.Config;


public class LandmarkSearchTableModel extends AbstractTableModel implements Observer {
	private final String LANDMARKS = "LANDMARKS";
	private final String BOOKMARKS = "BOOKMARKS";
	private final String COORDS = "COORDINATES";
	private List<MarsFeature> landmarkList = new ArrayList<>();	
	private Map<String, String> htmltoname = new HashMap<>();
	private  String[] columnNames = new String[] { LANDMARKS,     COORDS };
	private final Class[] columnClass = new Class[] { String.class,  String.class };

			
	public LandmarkSearchTableModel() {	
		Main.coordhandler.addObserver(this);
		Main.longitudeswitch.addObserver(this);
		Main.latitudeswitch.addObserver(this);
	}
	
	public void addData(LandmarkSearchTable jTable, List<MarsFeature> mf, SearchMode searchmode) {
		landmarkList.clear();
		htmltoname.clear();
		landmarkList.addAll(mf);
		if (searchmode == SearchMode.BOOKMARKS) {
		    columnNames[0] = "BOOKMARKS";
		} else if (searchmode == SearchMode.LANDMARKS){
			columnNames[0] = "LANDMARKS";
		}
		
		fireTableStructureChanged();
		if (jTable != null) {
			jTable.setRenderers(searchmode);
		}
		fireTableDataChanged();
	}

	public List<MarsFeature> getModelData() {
		List<MarsFeature> modeldata = new ArrayList<>();
		modeldata.addAll(landmarkList);
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

	@Override
	public int getRowCount() {
		return landmarkList.isEmpty() ? 0 : landmarkList.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (landmarkList.isEmpty())
			return null;

		MarsFeature row = landmarkList.get(rowIndex);

		// landmarks column
		if (getColumnName(columnIndex).equals(LANDMARKS) || 
				getColumnName(columnIndex).equals(BOOKMARKS)) {
			String myhtml = "<html>&nbsp;" + row.name + "<br/>" + "&nbsp;"
					+ "<font size=\"2\" color=\"rgb(163, 163, 163)\"/>" + row.landmarkType.toUpperCase() + "</html>";
			htmltoname.put(myhtml, row.name);   //for internal use by BOOKMARK search
			return myhtml;
		}
		// lat/lon coords
		else if (getColumnName(columnIndex).equals(COORDS)) {
			String formattedLatLon = formatLatLon(row);
			return formattedLatLon;
		}
		return null;
	}

	private String formatLatLon(MarsFeature row) {
		String coordOrdering = Config.get(Config.CONFIG_LAT_LON, Ordering.LAT_LON.asString());
		Ordering ordering = Ordering.get(coordOrdering);
		Point2D pt = new Point2D.Double(row.longitude, row.latitude);
		String latlon = ordering.format(pt) + "    ";		
		return latlon;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int colIndex) {	
		return false;
	}

	public MarsFeature getLandmarkDataAt(int row) {
		return landmarkList.get(row);
	}

	public MarsFeature getLandmarkDataByName(String htmlstring) {
		MarsFeature mf = null;
		String featurename = htmltoname.get(htmlstring);
		for (MarsFeature mf2 : landmarkList) {
			if (mf2.name.equals(featurename)) {
				return mf2;
			}
		}
		return mf;
	}
	
	
	public String[] getInitialColumnNames() {
		return columnNames;
	}

	@Override
	public void update(Observable o, Object arg) {
		fireTableDataChanged();		
	}
}
