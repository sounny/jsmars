package edu.asu.jmars.layer.krc;

import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

public class DataTableModel extends AbstractTableModel{
	private ArrayList<KRCDataPoint> dataPoints;

	private final static String NAME_COL = "Name";
	private final static String LAT_COL = "Latitude";
	private final static String LON_COL = "Longitude";
	private final static String LS_COL = "L sub S";
	private final static String HOUR_COL = "Hour";
	private final static String ELEV_COL = "Elevation";
	private final static String ALB_COL = "Albedo";
	private final static String SLOPE_COL = "Slope";
	private final static String OPACITY_COL = "Opacity";
	private final static String TI_COL = "Thermal Inertia";
	private final static String AZM_COL = "Azimuth";
	private final static String TEMP = "Temperature";
	
	private DecimalFormat locFormat = new DecimalFormat("#.#####");
	private ArrayList<String> cNames;
	
	private boolean isForInputTable;
	
	public DataTableModel(ArrayList<KRCDataPoint> data, boolean isLimited){
		dataPoints = data;
		
		cNames = new ArrayList<String>();
		cNames.add(NAME_COL);
		cNames.add(LAT_COL);
		cNames.add(LON_COL);
		
		isForInputTable = isLimited;
		//if this is the "full" table, add columns for all the user editable inputs
		if(!isLimited){
			cNames.add(LS_COL);
			cNames.add(HOUR_COL);
			cNames.add(ELEV_COL);
			cNames.add(ALB_COL);
			cNames.add(SLOPE_COL);
			cNames.add(OPACITY_COL);
			cNames.add(TI_COL);
			cNames.add(AZM_COL);
			cNames.add(TEMP);
		}
	}
	
	@Override
	public int getRowCount() {
		return dataPoints.size();
	}
	
	public String getColumnName(int col){
		return cNames.get(col);
	}

	@Override
	public int getColumnCount() {
		return cNames.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		KRCDataPoint dataPt = dataPoints.get(rowIndex);
		
		switch(getColumnName(columnIndex)){
		case NAME_COL:
			return dataPt.getName();
		case LAT_COL:
			return locFormat.format(dataPt.getLat());
		case LON_COL:
			return locFormat.format(dataPt.getLon());
		case LS_COL:
			return dataPt.getLSubS();
		case HOUR_COL:
			return dataPt.getHour();
		case ELEV_COL:
			return dataPt.getElevation();
		case ALB_COL:
			return dataPt.getAlbedo();
		case SLOPE_COL:
			return dataPt.getSlope();
		case OPACITY_COL:
			return dataPt.getOpacity();
		case TI_COL:
			return dataPt.getThermalInertia();
		case AZM_COL:
			return dataPt.getAzimuth();
		case TEMP :
			return dataPt.getTemperature();
			
		default:
			return null;
		}
	}
	
	public KRCDataPoint getDataPoint(int row){
		return dataPoints.get(row);
	}
	
	public boolean isForInputTable(){
		return isForInputTable;
	}
	
}
