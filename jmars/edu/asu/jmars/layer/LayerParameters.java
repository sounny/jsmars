package edu.asu.jmars.layer;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.map2.custom.CustomMap;
import edu.asu.jmars.layer.map2.custom.CustomMapBackendInterface;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.HttpRequestType;

/*	Does not actually refer to the layers or lviews themselves but 
 * 	more so properties need for their actual buttons 
 */


public class LayerParameters implements Serializable {
	/**
	 * Serialization number from 3.1.1  (make sure not to remove or change
	 * the name of attributes or methods, adding new things should not 
	 * break backwards compatibility)
	 */
	private static final long serialVersionUID = -7588455554663492752L;
	public String name;
	public String category;
	public String subcategory;
	public String topic;
	public Boolean advancedOnly;	//if true, only display layer in advanced mode
	public String type;
	public ArrayList<String> options;
	public String citation;
	public String description;
	public String id;
	public String layergroup;
	public String units;
	private String links;
	public ArrayList<String> servers;
	private boolean customMapOwnerFlag = false;
	
	private int tempManualId = 10000;//used for manually entered layers. Just need a unique id. 
	
	//used to tell whether a serialized instance is of the current version
	public Double lp_version = layerParameters_version;
	
	public static ArrayList<LayerParameters> lParameters = new ArrayList<LayerParameters>();
	public static ArrayList<LayerParameters> customlParameters = new ArrayList<LayerParameters>();
	
    public static final String paramsURL = Config.get("params.url");
	static String pdir = Main.getJMarsPath() + File.separator + "layerparams";
	static File paramsDir = new File(pdir);
	static File cache;
	
	private static boolean initializationComplete = false;
	
	//used to check cashed layerparams and pull new if version has changed
	static Double layerParameters_version = 1.0;
	
	private static DebugLog log = DebugLog.instance();
		
	public static boolean isInitializationComplete() {
	    return initializationComplete;
	}
	
	public void setLinks(String linksVal) {
		this.links = "{"+linksVal+"}";
	}
	public String getLinks() {
		return this.links;
	}
	//@since change bodies
	//all of the calls that were initially in the static block
	private static void initialize() {
		lParameters = new ArrayList<LayerParameters>();//reset 
		customlParameters = new ArrayList<LayerParameters>();//reset
		initializationComplete = false;//reset this as it can now get called from more than just the static block
		
		
		String body = Config.get(Util.getProductBodyPrefix()+"bodyname");//@since change bodies
		body = body.toLowerCase();
		String groups = Config.get("layers.group", "all");
		//for database timestamp
		String timeURL = "LayerInfoTimestamp?body="+body;
		timeURL += "&layerGroup="+groups;
		String databaseTime = "";
		//for backend lparams dump
		String urlStr = "LayerParameterFetcher?body="+body;
		urlStr += "&layerGroup="+groups;
		ArrayList<ArrayList<Object>> addLayerObjects;
		//for user file timestamp
		paramsDir.mkdirs();
		String lparamsFile = pdir + File.separator + body + "_" +Util.getVersionNumber()+ "_" + groups.replace(",", "_") + "_lparams.txt";
		cache = new File(lparamsFile);
		boolean fileIsNewer = false;
		String version = "";
		
		
		if (JmarsHttpRequest.getConnectionFailed() && cache.exists()) {//no point in sending more requests to find if it is the latest in the datase. Those requests hang and create a bad user experience
			try {
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cache));
				lParameters = (ArrayList<LayerParameters>) ois.readObject();
				
			} catch (Exception e) {
				e.printStackTrace();
				//we are likely dead in the water here.
			} 
		} else {
		
			if(cache.exists()){
				//get jmars version off filename
				String[] strings = cache.getName().split("_");
				version = strings[1];
				
				//get file timestamp
				Date fileTime = new Date(cache.lastModified());
				Timestamp fts = new Timestamp(fileTime.getTime());
				
				//get backend timestamp
				try {
					int idx = timeURL.indexOf("?");
		
					String connStr = paramsURL + timeURL.substring(0,idx);
					
					String data = timeURL.substring(idx+1)+StampLayer.getAuthString()+StampLayer.versionStr;
					
					// Connect timeout and SO_TIMEOUT of 10 seconds
	/*              URL url = new URL(connStr);
					URLConnection conn = url.openConnection();
					conn.setConnectTimeout(10*1000);
					conn.setReadTimeout(10*1000);
					
					conn.setDoOutput(true);
					OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
					wr.write(data);
					wr.flush();
					wr.close();
					
	*/
			        JmarsHttpRequest request = new JmarsHttpRequest(connStr, HttpRequestType.POST);
	                request.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
	                request.addOutputData(data);
			        request.setConnectionTimeout(10*1000);
			        request.setReadTimeout(1*10*1000);
			        request.send();
	
	//                ObjectInputStream ois = new ObjectInputStream(conn.getInputStream());
		            ObjectInputStream ois = new ObjectInputStream(request.getResponseAsStream());
		            databaseTime = (String)ois.readObject();
		            ois.close();
		            request.close();
	                
	            } catch (Exception e) {
					e.printStackTrace();
					System.out.println("Error retrieving latest timestamp from backend.");
				}
				
				//this check is prevent a failure when a body has not been
				//created in the info editor
				Timestamp dts = null;
				if (databaseTime.equals("")) {
					dts = new Timestamp(0);
				} else {
					dts = Timestamp.valueOf(databaseTime);
				}
	
				if(fts.after(dts)){
					fileIsNewer = true;
					//if file is newer...populate jmars off file
					try {
						ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cache));
						lParameters = (ArrayList<LayerParameters>) ois.readObject();
						//check version number..if not the same, change flag to populate off backend
						if(lParameters.get(0).lp_version == null){
							fileIsNewer = false;
						}
						else if(!lParameters.get(0).lp_version.equals(layerParameters_version)){
							fileIsNewer = false;
						}
					} catch (EOFException e1){
						//This most likely means that the file did not get saved out correct: ex. the
						// file had zero length and failed when loading back in, or the file stopped 
						// writing halfway through when it was saving it to the user's drive.  Either 
						// way, show the user a short message and then requery from backend.
						System.err.println("LayerParameters cache was not saved out correctly and could not be reloaded." +
											" Requerying backend to populate 'Add Layer'.");
						fileIsNewer = false;
					} catch (InvalidClassException e2){
						//This means that something in this class changed (added or removed variables)
						// It should probably not get here, because hopefully we'd changed version numbers
						// when changes were made to this class, but possibly not.  Either way, if it gets
						// here, just simply requery from backend anyway.
						log.println("LayerParameters class has changed, since last cached, requerying backend to populate 'Add Layer'.");
						fileIsNewer = false;
					} catch (Exception e) {
						System.err.println("Unable to populate 'Add Layer' from cache, requerying from backend.");
						e.printStackTrace();
						//if it fails set boolean so it will populate off backend
						fileIsNewer = false;
					} 
				}
				
			}
			//pull from backend if cache doesn't exist, or the cache
			// file is older than the backend version, or the version
			// number from the file does not match current jmars version
			if(!cache.exists() || !fileIsNewer || !version.equals(Util.getVersionNumber())){
				lParameters = new ArrayList<LayerParameters>();
				try {
					int idx = urlStr.indexOf("?");
		
					String connStr = paramsURL + urlStr.substring(0,idx);
					
					String data = urlStr.substring(idx+1)+StampLayer.getAuthString()+StampLayer.versionStr;
		
					// Connect timeout and SO_TIMEOUT of 10 seconds
	//				URL url = new URL(connStr);                             TODO Remove when sure
	//				URLConnection conn = url.openConnection();
	//				conn.setConnectTimeout(10*1000);
	//				conn.setReadTimeout(10*1000);
	//				
	//				conn.setDoOutput(true);
	//				OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
	//				wr.write(data);
	//				wr.flush();
	//				wr.close();
	//				
	//				ObjectInputStream ois = new ObjectInputStream(conn.getInputStream());
	//				addLayerObjects = (ArrayList<ArrayList<Object>>) ois.readObject();
	//				ois.close();
					
					JmarsHttpRequest req = new JmarsHttpRequest(connStr, HttpRequestType.POST);
					req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
					req.addOutputData(data);
					req.setConnectionTimeout(10*1000);
					req.setReadTimeout(10*1000);
					req.send();
					ObjectInputStream ois = new ObjectInputStream(req.getResponseAsStream());
					addLayerObjects = (ArrayList<ArrayList<Object>>) ois.readObject();
					ois.close();
					req.close();
					
					//populate lparameters off of backend
					for (ArrayList row : addLayerObjects) {
						LayerParameters newItem = new LayerParameters(row);
						lParameters.add(newItem);
					}
	
				} catch (Exception e) {
					System.err.println("Error retrieving layer parameters");
					e.printStackTrace();
					//populate off cached file if exists
					if(cache.exists()){
						try {
							ObjectInputStream ois = new ObjectInputStream(new FileInputStream(cache));
							lParameters = (ArrayList<LayerParameters>) ois.readObject();
						} catch (Exception e1) {
							e1.printStackTrace();
						} 
					}
				}
				
				//write lparameters to user cache
				writeCache();
	
	
			}	
		}			

		
	//-------------------------------Custom Page--------------------------------------------//
			if(Main.USER!=null&&Main.USER.length()>0)
				refreshCustomList();	
			
			initializationComplete=true;
	}
	
	/**
	 * Reset the lParameters list by calling the service with the current body name
	 * 
	 */
	public static void resetLayerParameters() {
		initialize();
	}
	
	//TODO: maybe inside of writeCache(), we can just write out a quick text file that contains the md5sum of the file. We can use that to compare when we are checking the cached version
	//Tries to write out the cached lParameters list to the client
	private static void writeCache(){
		//create the directory
		paramsDir.mkdirs();
		//write lparameters to user cache
		ObjectOutputStream oos;
		try {
			if(lParameters != null && lParameters.size()>0){
				cache.createNewFile();
				oos = new ObjectOutputStream(new FileOutputStream(cache));
				oos.writeObject(lParameters);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * Used when adding layer by hand for testing, or when adding in the
	 * custom maps.
	 * @param n		Name of button
	 * @param c		Category (first drop down)
	 * @param sub	Subcategory (second drop down)
	 * @param top	Topic (name of the collapsable pane)
	 * @param a		Is advanced?
	 * @param t		Layer type
	 * @param one	Options argument one
	 * @param two	Options argument two
	 */
	public LayerParameters(String name, String cat, String subcat, String topic, Boolean adv, String type, String one, String two){
		this.name = name;
		category = cat;
		subcategory = subcat;
		
		advancedOnly = adv;
		this.type = type;
		options = new ArrayList<String>();
		options.add(one);
		options.add(two);
		this.topic = topic;

		description="";
		citation="";
		links="";
		units="";
		servers = new ArrayList<String>();
		id = ++tempManualId+"uid";//just need a unique id
	} // end of constructor 2.2
	
	/**
	 * Constructor used when creating a map layer from the Custom Map Manager
	 * @param name
	 * @param cit
	 * @param desc
	 * @param units
	 * @param links
	 */
	public LayerParameters(String name, String cit, String desc, String units, String links){
		this.name = name;
		category = "";
		subcategory = "";
		topic = "";
		advancedOnly = false;
		type = "map";
		options = new ArrayList<String>();
		citation = cit;
		description = desc;
		this.units = units;
		//When links come in from the database they returned as an array so 
		// they are encased in brackets, do the same here because links is 
		// substringed later on and expects them.
		this.links = "{"+links+"}";
	}
	
// This constructor is the ULTIMATE constructor...the one that's used when pulling 
// from a table from a database	
	LayerParameters(ArrayList<Object> list){
		name = 			(String)	list.get(0);
		category = 		(String)	list.get(1);
		subcategory =	(String)	list.get(10);
		topic = 		(String)	list.get(2);
		advancedOnly = 	(Boolean)	list.get(3);
		type = 			(String)	list.get(4);
		options = new ArrayList<String>();
		if(list.size()>6){
			String[] o = (String[])list.get(6);
			for(int i=0;i<o.length;i++){
				options.add(o[i]);
			}
		}
		if (citation==null)
			citation="There is no citation";
		citation = 		(String)	list.get(7);
		if (description==null)
			description="There is currently no description";
		description =	(String)	list.get(8);
		id =			(String)	list.get(9);
		layergroup = (String) list.get(11);	
		units = (String) list.get(12);
		links = (String) list.get(13); 
		servers = new ArrayList<String>();
		if(list.size()>14){
			String[] s = (String[])list.get(14);
			for(int i=0; i<s.length; i++){
				servers.add(s[i]);
			}
		}
	} // end of constructor 4 (database/table)
	
	
	public static void refreshCustomList()
	{
		customlParameters.clear();
		ArrayList<CustomMap> existingMapList = CustomMapBackendInterface.getExistingMapList();
		if (existingMapList != null && existingMapList.size() > 0) {
			for (CustomMap map : existingMapList) {
				createCustomMapLP(map);
			}
		}

	}	
	
	public boolean getCustomMapOwnerFlag() {
		return this.customMapOwnerFlag;
	}
	public static void createCustomMapLP(CustomMap map) {
		String desc = map.getDescription();
        String[] keywords = map.getKeywords();
        if(keywords.length>0){
            desc += " Keywords: ";
            for(String word : keywords){
                desc+=word+", ";
            }
            desc = desc.substring(0, desc.length()-2);
        }
		LayerParameters lp = new LayerParameters(map.getName(), map.getCitation(), desc, map.getUnits(), map.getLinks());
		lp.category = "Custom";
		lp.type = "map";
		lp.options = new ArrayList<String>();
		if (map.getGraphicSource() != null) {
			lp.options.add(map.getGraphicSource().getName());
			if (map.isNumeric()) {
				lp.options.add(map.getGraphicSource().getName());
			}
		}
		customlParameters.add(lp);
		map.setLayerParameters(lp);
		if (Main.USER.equalsIgnoreCase(map.getOwner())) {
			lp.customMapOwnerFlag = true;
		}
	}
	public static void updateCustomMapLP(CustomMap map) {
		LayerParameters lp = map.getLayerParameters();
		String desc = map.getDescription();
        String[] keywords = map.getKeywords();
        if(keywords.length>0){
            desc += " Keywords: ";
            for(String word : keywords){
                desc+=word+", ";
            }
            desc = desc.substring(0, desc.length()-2);
        }
        lp.name = map.getName();
        lp.citation = map.getCitation();
        lp.description = desc;
        lp.units = map.getUnits();
        lp.links = map.getLinks();
		lp.category = "Custom";
		lp.type = "map";
		lp.options = new ArrayList<String>();
		if (map.getGraphicSource() != null) {
			lp.options.add(map.getGraphicSource().getName());
			if (map.isNumeric()) {
				lp.options.add(map.getGraphicSource().getName());
			}
		}
	}
	
// used to produce the path for where the user can find the map button in the AddLayer menus.
	public String toString(){
		String result;
		if ("Custom".equalsIgnoreCase(category)) {
			result = "Custom Map Id: "+id+" "+name;
		} else {
			if (subcategory.length()>0) {
				result = category+" > "+subcategory+" > "+topic;
			} else {
				result = category+" > "+topic;
			}
		}
		return result;
		
	}

//remove the files in the layerparams directory	
	public static void cleanCache(){
		Util.recursiveRemoveDir(paramsDir);
	}
	/**
	 * 
	 * Custom maps do not have layer ids. This method is meant only for setting an ID on custom map layers
	 *
	 *@param String custom id
	 */
	public void setCustomId(String customId) {
	    this.id = customId;
	}

} // end of class
