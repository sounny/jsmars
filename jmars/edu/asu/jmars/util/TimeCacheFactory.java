package edu.asu.jmars.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TimeCacheFactory {
	public static final String FMT_NAME_UTC   = "UTC";
	public static final String FMT_NAME_ET    = "ET";
	public static final String FMT_NAME_ORBIT = "Orbit+Offset";
	public static final String FMT_NAME_SCLK  = "Sclk";
	
	public static final String FMT_TAG_UTC    = "U";
	public static final String FMT_TAG_ET     = "E";
	public static final String FMT_TAG_ORBIT  = "O";
	public static final String FMT_TAG_SCLK   = "S";
	
	public static final String SC_TAG_ODY = "ODY";
	public static final String SC_TAG_MRO = "MRO";
	
	private static Map<String,String> timeFmtTagToName;
	
	/** Maps from String to TimeCache. */
	private Map<String,TimeCache> instances;

	/** Maps from String (craft tag) to String (craft id number). */
	private Map<String,String> craftIDs;

	/** Maps from String (craft tag) to String (craft name). */
	private static Map<String,String> craftNames;
	
	/** Singleton instance of the TimeCacheFactory. */
	private static TimeCacheFactory singletonInstance = null;
	
	
	
	public static synchronized TimeCacheFactory instance(){
		if (singletonInstance == null){
			singletonInstance = new TimeCacheFactory();
		}
		
		return singletonInstance;
	}

	protected TimeCacheFactory(){
		craftIDs = new HashMap<String,String>();
		craftNames = new HashMap<String, String>();
		instances = new HashMap<String,TimeCache>();
		timeFmtTagToName = new HashMap<String, String>();
		
		timeFmtTagToName = new HashMap<String, String>();
		timeFmtTagToName.put(FMT_TAG_UTC,   FMT_NAME_UTC);
		timeFmtTagToName.put(FMT_TAG_ET,    FMT_NAME_ET);
		timeFmtTagToName.put(FMT_TAG_ORBIT, FMT_NAME_ORBIT);
		timeFmtTagToName.put(FMT_TAG_SCLK,  FMT_NAME_SCLK);
		
		String[] tmp = Config.getAll("time.db.craft");
		for(int i=0; i<tmp.length; i+=2)
			craftIDs.put(tmp[i], tmp[i+1]);
		
		tmp = Config.getAll("time.db.craft_name");
		for(int i=0; i<tmp.length; i+=2)
			craftNames.put(tmp[i], tmp[i+1]);
	}
	
	/**
	 * @return An unmodifiable map of craft tag to its spice id.
	 */
	public Map<String,String> getCraftIdMap(){
		return Collections.unmodifiableMap(craftIDs);
	}

	/**
	 * @return An unmodifiable map of craft tag to craft name,
	 * for example "ODY" -> "Odyssey".
	 */
	public Map<String,String> getCraftNameMap(){
		return Collections.unmodifiableMap(craftNames);
	}
	
	/**
	 * @return An unmodifiable map of time format tag to
	 * time format name, for example "E" -> "ET".
	 */
	public Map<String,String> getTimeFmtTagToNameMap(){
		return Collections.unmodifiableMap(timeFmtTagToName);
	}

	/**
	 * Returns a cached instance of a TimeCache associated with the
	 * specified craft.
	 * 
	 * @param craft Must be one of the crafts from the jmars.config file.
	 * @throws TimeException If the URL is invalid or craft is unknown.
	 */
	public synchronized TimeCache getTimeCacheInstance(String craft) throws TimeException {
		String ship = (String) craftIDs.get(craft);
		if(ship == null)
			throw  new TimeException("Invalid spacecraft name: " + craft);

		TimeCache instance = instances.get(craft);
		if(instance == null){
			// TODO: This is fine for now, we have to have some other means of
			// identifying whether a particular cache is to be loaded from the
			// web-server or another place at some point in the future.
			if ("lro".equalsIgnoreCase(craft)){
				instance = new LroTimeCache();
			}
			else {
				String timeDbUrl = Config.get("time.db")+"?ship=" + ship; 
				try {
					instance = new MarsTimeCache(craft, new URL(timeDbUrl));
				}
				catch(MalformedURLException e) {
					throw  new TimeException("Invalid URL ("+timeDbUrl+") built from the entry in config file", e);
				}
			}
			instances.put(craft, instance);
		}

		return  instance;
	}
	
	/**
	 * TODO: This should be removed from here or we should implement some kind of caching 
	 * for this case as well.
	 * 
	 * Returns an <em>UNCACHED</em> version of TimeCache associated with
	 * the specified craft narrowed down to the orbit range specified.
	 * This method is used in some places for better efficiency.
	 * @param craft Must be one of the crafts from the jmars.config file.
	 * @param minOrbit Start value or orbit range covered by this cache.
	 * @param maxOrbit Stop value of orbit range covered by this cache.
	 * @throws TimeException If the fetch URL is invalid or craft is unknown.
	 */
	public TimeCache getTimeCacheInstance(String craft, int minOrbit, int maxOrbit) throws TimeException {
		String ship = (String) craftIDs.get(craft);
		if(ship == null)
			throw  new TimeException("Invalid spacecraft name: " + craft);
		
		String timeDbUrl = Config.get("time.db")+"?ship="+ship+"&min_orbit="+minOrbit+"&max_orbit="+ maxOrbit;
		try{
			return  new MarsTimeCache(craft, new URL(timeDbUrl));
		}
		catch(MalformedURLException e){
			throw  new TimeException("Invalid URL ("+timeDbUrl+") built from the entry in config file", e);
		}
	 }

}
