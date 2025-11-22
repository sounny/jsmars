package edu.asu.jmars.layer.landing;

import java.io.Serializable;

import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.FieldMap;
import edu.asu.jmars.layer.util.features.FieldMap.Type;


public class StatCalculator implements Serializable {
	private String name;
	private String sName;	//short name (used for exporting because ESRI only supports 10 chars)
	private boolean isShowing;	//whether or not to show this as a column in the LandingSite table
	boolean hasAvg;
	boolean hasMin;
	boolean hasMax;
	boolean hasStd;
	private MapSource source;
	private int ppd;
	private boolean isDefault;	//whether it's one of the default calculators (elev, ti, slope, albedo)
	
	/**
	 * Creates a stat calculator which is added to the landing site table as a column.
	 * Each landing site object will have it's own Stat for each calculator.
	 * The class is designed so that users can define any kind of calculator they would
	 * like -- specify the source, sampling ppd, and which stats to actually calculate.
	 * The four defaults are Elevation, Thermal Inertia, Slope and Albedo and these 
	 * defaults have all four stats (avg, min, max and std) calculated.
	 * 
	 * @param name 			Is the name of the stat being calculated
	 * @param sName 		Is a shortened version of the name used in exporting (ESRI only allows 10 characters)
	 * @param isDefault		A flag for whether this is one of the default stats (elev, ti, slope, albedo)
	 * @param ms			The map source this calculator samples
	 * @param ppd			The ppd at which it is sampling
	 * @param hasAvg		Whether an average is calculated
	 * @param hasMin		Whether the minimum is calculated
	 * @param hasMax		Whether the maximum is calculated
	 * @param hasStd		Whether the standard deviation is calcuated
	 * @see	#Stat
	 */
	public StatCalculator(String name, String sName, 
						boolean isDefault, MapSource ms, int ppd, 
						boolean hasAvg, boolean hasMin, 
						boolean hasMax, boolean hasStd){
		
		this.name = name;
		this.sName = sName;
		source = ms;
		this.ppd = ppd;
		this.hasAvg = hasAvg;
		this.hasMin = hasMin;
		this.hasMax = hasMax;
		this.hasStd = hasStd;
		isShowing = true;
		this.isDefault = isDefault;
		
	}
	
	
	/**
	 * Calculates the specific Stat object (with specified stats, ie. avg, min, max, std)
	 * for a given LandingSite object, using it's spatial path.
	 * @param site	The site used to get the spatial path of which to sample under
	 * @return		The resulting Stat object
	 */
	public Stat calculateStat(LandingSite site){
		final FPath path = new FPath(site.getPointsArray(), FPath.SPATIAL_WEST, true);
		
		//Create new stat
		final Stat stat = new Stat(name, sName);
		//Set location information
		stat.lowerRight = site.getLowerRight();
		stat.upperLeft = site.getUpperLeft();
		stat.angle = site.getAngle();
		
		if(hasAvg){
			stat.hasAvg = true;
			FieldMap avgValue = new FieldMap("map sampling", Type.AVG, ppd, source, 0);
			if(avgValue.getValue(path)!=null){
				stat.setAvg((Double) avgValue.getValue(path));
			}else{
				stat.setAvg(Double.NaN);
			}
		}
		if(hasMin){
			stat.hasMin = true;
			FieldMap minValue = new FieldMap("map sampling", Type.MIN, ppd, source, 0);
			if(minValue.getValue(path)!=null){
				stat.setMin((Double) minValue.getValue(path));
			}else{
				stat.setMin(Double.NaN);
			}
		}
		if(hasMax){
			stat.hasMax = true;
			FieldMap maxValue = new FieldMap("map sampling", Type.MAX, ppd, source, 0);
			if(maxValue.getValue(path)!=null){
				stat.setMax((Double) maxValue.getValue(path));
			}else{
				stat.setMax(Double.NaN);
			}
		}
		if(hasStd){
			stat.hasStd = true;
			FieldMap stdValue = new FieldMap("map sampling", Type.STDEV, ppd, source, 0);
			if(stdValue.getValue(path)!=null){
				stat.setStd((Double) stdValue.getValue(path));
			}else{
				stat.setStd(Double.NaN);
			}
		}

		return stat;
		
	}
	
	//get attributes
	public String getName(){
		return name;
	}
	public String getMapSourceTitle(){
		return source.getTitle();
	}
	public int getPPD(){
		return ppd;
	}
	
	public boolean isDefault(){
		return isDefault;
	}
	public boolean isShowing(){
		return isShowing;
	}
	
	//set attributes
	public void setShowing(boolean show){
		isShowing = show;
	}
	
}
