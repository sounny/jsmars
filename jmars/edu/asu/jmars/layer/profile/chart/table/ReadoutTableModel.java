package edu.asu.jmars.layer.profile.chart.table;

import java.awt.Color;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.table.AbstractTableModel;
import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.profile.ProfileLView.ProfileLine;
import edu.asu.jmars.layer.profile.ProfileLayer.NumericMapDataSampleWrapper.NumericMapDataSample;
import edu.asu.jmars.layer.profile.chart.ProfileChartView;
import edu.asu.jmars.layer.profile.config.Config;
import edu.asu.jmars.layer.profile.config.ConfigType;
import edu.asu.jmars.layer.profile.swing.ProfileTabelCellObject;


public class ReadoutTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	private final String SHOW_HIDE = "Visibility";
	private final String TITLE_COL = "Source";
	private final String VAL_COL = "Value";
	private final String DISTANCE_COL = "Distance";
	private final String LAT_COL = "Latitude";
	private final String LON_COL = "Longitude";
	private final String COLOR_COL = "Color";	
	private Config chartConfig = null;
	//map [Profile ID - to a map of corresponding plot objects]; map of plotobjects is [numeric source title - PlotObject]
	//for example, One Num Source:
	//[Profile ID (3) - [Mola 128 Elevation , <PlotObject>]
	//or for Many Num Sources
	//[Profile ID (3)  - [Night Temp - <PlotObject>; Day Temp  - <PlotObject>...]
	private Map<Integer, LinkedHashMap<String, PlotObject>> plotObjectsModel = new LinkedHashMap<>(); 

	private final String[] columnNames = new String[] { SHOW_HIDE,     TITLE_COL,                    VAL_COL,      DISTANCE_COL, LAT_COL,      LON_COL,      COLOR_COL};
	private final Class[] columnClass = new Class[] {   Boolean.class, ProfileTabelCellObject.class, String.class, String.class, String.class, String.class, Color.class};
			
	protected String[] columnToolTips = {
		    "Show/Hide plot", // show/hide column
		    "Profile name and color", //name of profile line
		    " \"Y\" (unit) coordinate on the chart",   //Value
		    " \"X\" coordinate on the chart",   //Distance
		    "Latitude corresponding to current position on the chart",   //Lat
		    "Longitude corresponding to current position on the chart",   //Lon
		    "Double click in cell to change plot color"  //Color		        
	 };
	
	public ReadoutTableModel() {}

	public void addData(ProfileChartView profileChartView, Config config) {
		// build readout data model based on chart config
		this.plotObjectsModel.clear();
		this.chartConfig = config;

		List<MapSource> configNumSources = config.getNumsourcesToChart();
		Map<Integer, Shape> configProfiles = config.getProfilesToChart();

		for (Map.Entry<Integer, Shape> entry : configProfiles.entrySet()) {
			int ID = entry.getKey();
			Shape shape = entry.getValue();
			if (!(shape instanceof ProfileLine)) continue;
			ProfileLine profile = (ProfileLine) shape;
			this.plotObjectsModel.put(ID, new LinkedHashMap<String, PlotObject>());
			createReadoutObject(ID, profile, configNumSources, profileChartView);
		}
		fireTableDataChanged();
	}
	

	private void createReadoutObject(int ID, ProfileLine profileline, List<MapSource> sources,
			ProfileChartView profileChartView) {
		
		List<MapSource> varsources = new ArrayList<>();
		varsources.addAll(sources);

		if (profileline == null) return;

		for (MapSource source : varsources) { // for each numeric source

			PlotObject newPlotObject = new PlotObject(ID, source.getTitle());

			newPlotObject.setOrigSpatialPoints(profileline.getProfileSpatialPts());
			newPlotObject.setProj(profileline.getOrigProjection());
			newPlotObject.setProfileColorInMainView(profileline.getLinecolor());

			newPlotObject.setPlotunit(source.getUnits() != null ? source.getUnits() : "");
			
			String uniqueKey = profileline.getIdentifier() + source.getTitle();
			newPlotObject.setUniqueID(uniqueKey);

			String plotname = "";
			if (varsources.size() > 1) {
				plotname = source.getTitle();
				newPlotObject.setIsNumericSource(true);
			} else {
				plotname = profileline.getRename() != null ? profileline.getRename() : profileline.getIdentifier();
			}
			newPlotObject.setPlotname(plotname);

			newPlotObject.setRename(profileline.getRename());

			newPlotObject.setVisible(true);

			
			XYItemRenderer r = profileChartView.getSourceRenderer(uniqueKey);

			if (r != null) {
				Color c = (Color) r.getSeriesPaint(0); // only 1 series per renderer
				newPlotObject.setPlotColor(c);
			} else {
				newPlotObject.setPlotColor(null); //will update when chart series are created in tableChanged
			}

			setInitialXYValues(newPlotObject);

			setInitialLatLonValues(newPlotObject);

			this.plotObjectsModel.get(ID).put(source.getTitle(), newPlotObject);
		}
	}

	private void setInitialLatLonValues(PlotObject newPlotObject) {
		newPlotObject.setLonValue(Double.NaN);
		newPlotObject.setLatValue(Double.NaN);		
	}

	private void setInitialXYValues(PlotObject newPlotObject) {
		newPlotObject.setyValue(Double.NaN);
		newPlotObject.setxValue(Double.NaN);		
	}
	
	public void reset() {
		this.plotObjectsModel.clear();
		fireTableDataChanged();
	}

	public void updateSampleData(List<NumericMapDataSample> numsamples, double XCoord) {
		if (this.chartConfig != null) {
			if (ConfigType.ONENUMSOURCE == this.chartConfig.getConfigType()) {
				calcXYValuesForONESOURCE(numsamples, XCoord);
			} else if (ConfigType.MANYNUMSOURCES == this.chartConfig.getConfigType()) {
				calcXYValuesForMANYSOURCES(numsamples, XCoord);
			}
		}
	}

	private void calcXYValuesForMANYSOURCES(List<NumericMapDataSample> numsamples, double XCoord) {
		List<PlotObject> plotObjects = new ArrayList<>();
		plotObjects.addAll(getAllPlotObjects());
		for (NumericMapDataSample numericsample : numsamples) {
			// one num sample will contain all bands
			if (numericsample != null) {
				int ID = numericsample.getUniqueID();
				Pair<Double, Double> lonlat = numericsample.getLonLat(XCoord);
				double[] yvalue = numericsample.getSampleData(XCoord); // yValue size and plotsArray size must be equal
				PlotObject[] plotsArray = plotObjects.toArray(new PlotObject[plotObjects.size()]);
				if (yvalue == null) return;
				if (plotsArray == null) return;
				if (yvalue.length > numericsample.getNumBands()) return;
				if (plotsArray.length != yvalue.length) return;

				for (int index = 0; index < yvalue.length; index++) {
					PlotObject plotobj = plotsArray[index];
					if (plotobj.getPlotID() != ID) continue;
					if (!plotobj.isVisible()) continue;
					plotobj.setyValue(yvalue[index]);
					plotobj.setxValue(XCoord);
					plotobj.setLonValue(lonlat != null ? lonlat.getKey() : Double.NaN);
					plotobj.setLatValue(lonlat != null ? lonlat.getValue() : Double.NaN);
				}
			}
		}
		fireTableDataChanged();
	}
	

	private void calcXYValuesForONESOURCE(List<NumericMapDataSample> numsamples, double XCoord) {
		List<PlotObject> plotObjects = new ArrayList<>();
		plotObjects.addAll(getAllPlotObjects());
		for (NumericMapDataSample numericsample : numsamples) { //multiple num samples, one per each profile
			if (numericsample != null) {
				int ID = numericsample.getUniqueID();
				for (PlotObject plotobj : plotObjects) {
					if (!plotobj.isVisible())
						continue;
					if (plotobj.getPlotID() == ID) {
						double[] yvalue = numericsample.getSampleData(XCoord); //yvalue contains only 1 value
						plotobj.setyValue(yvalue != null ? yvalue[0] : Double.NaN);
						plotobj.setxValue(yvalue != null ? XCoord : Double.NaN);
						Pair<Double, Double> lonlat = numericsample.getLonLat(XCoord);
						plotobj.setLonValue(lonlat != null ? lonlat.getKey() : Double.NaN);
						plotobj.setLatValue(lonlat != null ? lonlat.getValue() : Double.NaN);
					}
				}
			}
		}
		fireTableDataChanged();
	}

	public List<PlotObject> getAllPlotObjects() {
		List<PlotObject> polist = new ArrayList<>();
		if (!this.plotObjectsModel.isEmpty()) {
			for (Map.Entry<Integer, LinkedHashMap<String, PlotObject>> entry : this.plotObjectsModel.entrySet()) {
				LinkedHashMap<String, PlotObject> plotobjmap = entry.getValue();
				for (Map.Entry<String, PlotObject> plotobjentry : plotobjmap.entrySet()) {
					polist.add(plotobjentry.getValue());
				}
			}
		}
		return polist;
	}
	
	public List<PlotObject> getNotVisiblePlots() {
		List<PlotObject> notvisible = new ArrayList<>();
		List<PlotObject> all = new ArrayList<>();
		
		all.addAll(getAllPlotObjects());
		
		for (PlotObject po : all) {
			if (!po.isVisible()) {
				notvisible.add(po);
			}
		}
		return notvisible;
	}

	public boolean isNotVisible(Integer ID) {
		boolean isNotVisible = false; //means Is visible
		List<PlotObject> notvisible = new ArrayList<>();
		
		if (this.plotObjectsModel.isEmpty()) {
			return isNotVisible; //visible by default
		}
		
		if (this.chartConfig == null) {
			return isNotVisible;  //visible by default
		}
		
		if (this.chartConfig.getConfigType() == ConfigType.ONENUMSOURCE) {
			notvisible.addAll(getNotVisiblePlots());
			for (PlotObject po : notvisible) {
				if (po.getPlotID() == ID && !po.isVisible()) {
					isNotVisible = true;
					break;
				}
			}
		} else if (this.chartConfig.getConfigType() == ConfigType.MANYNUMSOURCES) {
			notvisible.addAll(getNotVisiblePlots());
			Map<String, PlotObject> sourcesmap = this.plotObjectsModel.get(ID);
			int notvisiblecount = 0;
			for (PlotObject po : notvisible) {	
				if (po.getPlotID() == ID && !po.isVisible()) {
					notvisiblecount++;
				}
			}
			if (notvisiblecount == sourcesmap.size()) {
				isNotVisible = true;
			} 
		}
		return isNotVisible;
	}	

	public Map<Integer, HashMap<String, PlotObject>> getModelData() {
		Map<Integer, HashMap<String, PlotObject>> varplotobjects = new HashMap<>();
		if (this.plotObjectsModel != null && !this.plotObjectsModel.isEmpty()) {
			varplotobjects.putAll(this.plotObjectsModel);
		}
		return varplotobjects;
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
		return plotObjectsModel.isEmpty() ? 0 : getRowCountBasedOnConfig();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (plotObjectsModel.isEmpty())
			return null;

		PlotObject row = getDataValueAt(rowIndex);
		
		if (row == null) return null;
		
		//show/hide
		if (getColumnName(columnIndex).equals(SHOW_HIDE)) {
			return row.isVisible();
		}
		// profile line name with line color as in Main view
		else if (getColumnName(columnIndex).equals(TITLE_COL)) {
			String name;
			if (row.isNumericSource()) {
				name = row.getPlotname();
			} else {
			    name = row.getRename() != null ? row.getRename() : row.getPlotname();
			}
			ProfileTabelCellObject cellobj = new ProfileTabelCellObject(name);
			if (! row.isNumericSource()) {
				cellobj.setColor(row.getProfileColorInMainView());
			}
			return cellobj;
		}
		// color
		else if (getColumnName(columnIndex).equals(COLOR_COL)) {
			return row.getPlotColor();
		}
		// value
		else if (getColumnName(columnIndex).equals(VAL_COL)) {
			double val =  row.getyValue();
			String result = row.getValueFormatted(val);
			String unit = (row.getPlotunit() == null ? "" : row.getPlotunit());
			result = (PlotObject.NO_VALUE.equals(result)) ? result : result + " " + unit;
			return result;
		}
		// km
		else if (getColumnName(columnIndex).equals(DISTANCE_COL)) {
			return row.getDistanceFormatted();
		}
		//lat
		else if (getColumnName(columnIndex).equals(LAT_COL)) {
			double val =  row.getLatValue();
			String result = row.getValueFormatted(val);
			return result;
		}		
		//lon
		else if (getColumnName(columnIndex).equals(LON_COL)) {
			double val =  row.getLonValue();			
			String result = row.getValueFormatted(val);
			return result;
		}	
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int colIndex) {
		if (getColumnName(colIndex).equals(COLOR_COL))
			return true;
		else if (getColumnName(colIndex).equals(SHOW_HIDE))
			return true;		
		return false;
	}

	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if (plotObjectsModel.isEmpty())
			return;
		
		PlotObject row = getDataValueAt(rowIndex);
		
		 if(SHOW_HIDE.equals(getColumnName(columnIndex))) {
             row.setVisible(((Boolean)value).booleanValue()); 
             fireTableDataChanged();  
         }	
		 else if(COLOR_COL.equals(getColumnName(columnIndex))) {
             row.setPlotColor(((Color)value));  
             fireTableDataChanged();  
         }	 
	}


	public String[] getInitialColumnNames() {
		return columnNames;
	}	
	
	@SuppressWarnings("unchecked")
	private PlotObject getDataValueAt(int rowIndex) {
		PlotObject po = null;
		if (this.chartConfig == null) return po;
		if (!this.plotObjectsModel.isEmpty()) { // for One Num Source rowIndex gets profile ID and its Plot Object
											// for Many Num Sources, 1st entry gets profile ID and rowIndex gets sources
			if (this.chartConfig.getConfigType() == ConfigType.ONENUMSOURCE) {
				@SuppressWarnings("unchecked")
				Map.Entry<Integer, LinkedHashMap<String, PlotObject>> entry = (Entry<Integer, LinkedHashMap<String, PlotObject>>) this.plotObjectsModel
						.entrySet().toArray()[rowIndex];
				LinkedHashMap<String, PlotObject> plotobjmap = entry.getValue();
				Entry<String, PlotObject> plotobjentry = (Entry<String, PlotObject>) plotobjmap.entrySet()
						.toArray()[0];
				 po = plotobjentry.getValue();
			} else if (this.chartConfig.getConfigType() == ConfigType.MANYNUMSOURCES) {
				Map.Entry<Integer, LinkedHashMap<String, PlotObject>> entry = (Entry<Integer, LinkedHashMap<String, PlotObject>>) this.plotObjectsModel
						.entrySet().toArray()[0];
				LinkedHashMap<String, PlotObject> plotobjmap = entry.getValue();
				Entry<String, PlotObject> plotobjentry = (Entry<String, PlotObject>) plotobjmap.entrySet()
						.toArray()[rowIndex];
				 po = plotobjentry.getValue();		
			}
		} 
		return po;
	}
	
	private int getRowCountBasedOnConfig() {
		int rowcount = 0;
		if (this.plotObjectsModel.isEmpty()) {
			return rowcount;
		}
		if (this.chartConfig == null) return 0;
		if (this.chartConfig.getConfigType() == ConfigType.ONENUMSOURCE) {
			rowcount = this.plotObjectsModel.size();
		} else if (this.chartConfig.getConfigType() == ConfigType.MANYNUMSOURCES) {
			Map.Entry<Integer, LinkedHashMap<String, PlotObject>> entry = (Entry<Integer, LinkedHashMap<String, PlotObject>>) this.plotObjectsModel
					.entrySet().toArray()[0];
			LinkedHashMap<String, PlotObject> plotobjmap = entry.getValue();
			rowcount = plotobjmap.size();
		}
		return rowcount;
	}



}
