package edu.asu.jmars.layer.map2.custom;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.map2.CustomMapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.lmanager.SearchProvider;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HttpRequestType;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;

public class CustomMapBackendInterface {
    private static String customMapServerURL;
    private static DebugLog log = DebugLog.instance();
    /**
     * Static block to setup the URL and information for the custom map server
     */
    static{
        setCustomMapServerURL();
    }
    
    
    
    private static ArrayList<CustomMap> customMapList = null;
    /**
     * 
     * Returns a list of CustomMap objects for the current JMARS user.
     * The list is alphabetized based on custom map name.
     *
     * @param boolean refreshFromServer: pull custom maps from the custom map database
     * @return ArrayList<CustomMap> each representing a custom map for this user
     */
    public static ArrayList<CustomMap> getExistingMapList() {
    	if (customMapList == null) {
    		loadExistingMapList();
    	}
    	return customMapList;	
    }
    /**
     * 
     * Returns a list of CustomMap objects for the current JMARS user.
     * The list is alphabetized based on custom map name.
     *
     * @return ArrayList<CustomMap> each representing a custom map for this user
     */
    public static ArrayList<CustomMap> loadExistingMapList() {
    	String url = customMapServerURL + "maps/select.php";
        JmarsHttpRequest request = new JmarsHttpRequest(url, HttpRequestType.GET);
        boolean returnStatus = sendRequest(request);
        if (returnStatus) {
            try {
                ArrayList<CustomMap> tempList = buildExistingMapsFromJSON(request);
                //sort the list
                ArrayList<CustomMap> top = new ArrayList<CustomMap>();
                ArrayList<CustomMap> bottom = new ArrayList<CustomMap>();
                String owner = Main.USER;
                for (CustomMap map : tempList) {
                	if (owner.equalsIgnoreCase(map.getOwner())) {
                		top.add(map);
                	} else {
                		bottom.add(map);
                	}
                }
                Collections.sort(top, ExistingMap.NameComparator);
                Collections.sort(bottom, ExistingMap.NameComparator);
                if (customMapList == null) {
                	customMapList = new ArrayList<CustomMap>();
                }
                customMapList.clear();
                customMapList.addAll(top);
                customMapList.addAll(bottom);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (request != null) {
            request.close();
        }
        return customMapList;
    }
    
    public static HashMap<String,String> getExistingNameIdMap() {
        HashMap<String,String> nameIdMap = new HashMap<String,String>();
        
        String url = customMapServerURL + "maps/selectNameIdMap.php";
        JmarsHttpRequest request = new JmarsHttpRequest(url, HttpRequestType.GET);
        boolean returnStatus = sendRequest(request);
        if (returnStatus) {
            try {
                String json = buildString(Util.readLines(request.getResponseAsStream()));
                JSONArray mapArray = new JSONArray(json);
                for (int i=0; i<mapArray.length(); i++) {
                    JSONObject obj = (JSONObject) mapArray.get(i);
                    String mapId = (obj.isNull("custom_map_id") ? null : obj.getString("custom_map_id"));
                    String mapName = (obj.isNull("custom_map_name") ? null : obj.getString("custom_map_name"));
                    if (mapId != null && mapName != null) {
                        nameIdMap.put(mapName,mapId);
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (request != null) {
            request.close();
        }
        return nameIdMap;
    }
    
    public static void verifyJMARSUserExists() {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"users/validate.php", HttpRequestType.POST);
        sendRequest(request);
    }
    
    /**
     * 
     * Returns a list of favorites for the current JMARS user.
     * The list is alphabetized based on custom map name.
     *
     * @return ArrayList<String>: list of favorite layer ids
     */
    public static HashMap<String, ArrayList<String>> loadFavorites() {
        ArrayList<String> faves = new ArrayList<String>();
        ArrayList<String> customFaves = new ArrayList<String>();
        ArrayList<String> mapSourceFaves = new ArrayList<String>();
        String url = customMapServerURL + "favorites/select.php";
        JmarsHttpRequest request = new JmarsHttpRequest(url, HttpRequestType.GET);
        boolean returnStatus = sendRequest(request);
        if (returnStatus) {
            try {
                String json = buildString(Util.readLines(request.getResponseAsStream()));
                if (!json.equals("{}")) {
                    JSONArray mapArray = new JSONArray(json);
                    for (int i=0; i<mapArray.length(); i++) {
                        JSONObject obj = (JSONObject) mapArray.get(i);
                        String layerId = (obj.isNull("layer_id") ? null : obj.getString("layer_id"));
                        String customMapId = (obj.isNull("custom_map_id") ? null : obj.getString("custom_map_id"));
                        String mapSourceName = (obj.isNull("map_source_name") ? null : obj.getString("map_source_name"));
                        if (layerId != null) {
                            faves.add(layerId);
                        }
                        if (customMapId != null) {
                            customMapId = "cm_"+customMapId;
                            customFaves.add(customMapId);
                        }
                        if (mapSourceName != null) {
                        	mapSourceFaves.add(mapSourceName);
                        }
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (request != null) {
            request.close();
        }
        HashMap<String,ArrayList<String>> faveMap = new HashMap<String,ArrayList<String>>();
        faveMap.put("layers",faves);
        faveMap.put("custom map",customFaves);
        faveMap.put("map_source", mapSourceFaves);
        return faveMap;
    }
    
    public static boolean addFavorite(String layerId) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"favorites/add.php", HttpRequestType.POST);
        request.addRequestParameter("layer_id", layerId);
        boolean returnStatus = sendRequest(request);
        try {
            for (String x : Util.readLines(request.getResponseAsStream())) {
                if ("success".equalsIgnoreCase("x")) {
                    returnStatus = true;
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return returnStatus;
    }
    public static boolean addCustomFavorite(String customMapId) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"favorites/addCustom.php", HttpRequestType.POST);
        request.addRequestParameter("custom_map_id", customMapId);
        boolean returnStatus = sendRequest(request);
        try {
            for (String x : Util.readLines(request.getResponseAsStream())) {
                if ("success".equalsIgnoreCase("x")) {
                    returnStatus = true;
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return returnStatus;
    }
    public static boolean addMapSourceFavorite(String mapSourceName) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"favorites/addMapSource.php", HttpRequestType.POST);
        request.addRequestParameter("map_source_name", mapSourceName);
        boolean returnStatus = sendRequest(request);
        try {
            for (String x : Util.readLines(request.getResponseAsStream())) {
                if ("success".equalsIgnoreCase("x")) {
                    returnStatus = true;
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return returnStatus;
    }
    
    public static boolean deleteFavorite(String layerId, boolean isCustomMap, String mapSourceName) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"favorites/delete.php", HttpRequestType.POST);
        request.addRequestParameter("layer_id", layerId);
        request.addRequestParameter("custom_map_flag",(isCustomMap ? "true" : "false"));
        request.addRequestParameter("map_source_flag", (mapSourceName == null ? "false" : "true"));
        request.addRequestParameter("map_source_name", (mapSourceName == null ? "" : mapSourceName));
        boolean returnStatus = sendRequest(request);
        try {
            for (String x : Util.readLines(request.getResponseAsStream())) {
                if ("success".equalsIgnoreCase("x")) {
                    returnStatus = true;
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return returnStatus;
    }
    
    /**
     * 
     * Returns a list of CustomMap objects for the current JMARS user.
     * The list is alphabetized based on custom map name.
     *
     * @param owner - String representing the current user
     * @return ArrayList<CustomMap> each representing a custom map for this user
     */
    public static CustomMap getExistingMap(CustomMap map) {
        ArrayList<CustomMap> customMaps = null;
        String url = customMapServerURL + "maps/selectOneExistingMap.php";
        JmarsHttpRequest request = new JmarsHttpRequest(url, HttpRequestType.GET);
        request.addRequestParameter("customMapId", map.getCustomMapId());
        boolean returnStatus = sendRequest(request);
        if (returnStatus) {
            try {
                customMaps = buildExistingMapsFromJSON(request);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (request != null) {
            request.close();
        }
        return customMaps.get(0);
    }
    /**
     * 
     * Returns a list of CustomMap objects for the current JMARS user for the list of in progress maps.
     *
     * @param owner - String representing the current user
     * @return ArrayList<CustomMap> each representing an in progress custom map for this user
     */
    public static ArrayList<CustomMap> getInProgressCustomMapList() {
        ArrayList<CustomMap> uploadFileList = new ArrayList<CustomMap>();
        
        String url = customMapServerURL + "maps/getInProgressUploads.php";
        JmarsHttpRequest request = new JmarsHttpRequest(url, HttpRequestType.GET);
        boolean returnStatus = sendRequest(request);
        if (returnStatus) {
            try {
                uploadFileList = buildUploadFileFromJSON(request);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
            if (request != null) {
                request.close();
            }
        }
        
        return uploadFileList;
    }
    
    /**
     * This method will upload one file. 
     * 
     * @param CustomMap: A file selected and marked for upload in the custom map manager.
     */
    public static void uploadCustomMap(CustomMap file) {
        File actualFile = new File(file.getFilename());
        String fileBasename = file.getBasename();
        log.aprintln("Uploading file: "+fileBasename);
        file.startUpload();
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/upload.php", HttpRequestType.POST);
        if (file.getCustomMapId() == null) {
            throw new IllegalArgumentException("Id not set on CustomMap object when attempting to upload.");
        }
        request.addRequestParameter("custom_map_key", file.getCustomMapId());
    //            request.addRequestParameter("name", fileBasename);
    //            request.addRequestParameter("ignore", MessageFormat.format("{0,number,#.######}",-32768));
        
        request.addRequestParameter("canonicName", file.getCanonicName());
        request.addUploadFileNew(fileBasename, actualFile);
        request.setRetryNever();
        
        String errorMsg = null;
        try {
            request.setConnectionTimeout(300000);
            boolean successful = sendRequest(request);
            if (!successful) {
                errorMsg = "ERROR: Unexpected HTTP code " + request.getStatus() + " received";
            }
            String customMapId = null;
            String[] responseArr = Util.readLines(request.getResponseAsStream());
            
            if (responseArr != null && responseArr.length > 0) {
                String response = responseArr[0];
                if (response.startsWith("success")) {
                    file.setStatus(UploadFile.STATUS_IN_PROGRESS);
                } else {
                    //some type of error or warning occurred. 
                    for (String res : responseArr) {
                        log.aprintln(res);
                        System.err.println(res);
                    }
                    throw new Exception("Unexpected response from upload. See log for more details. ");
                }
                
            } else {
                throw new Exception("Null response from upload");
            }
        } catch (Exception ex) {
            file.setStatus(UploadFile.STATUS_ERROR);
            try {
                //if an error occurred, there may not be a custom map id set. If not, do not try to update the status in the database
                if (file.getCustomMapId() != null) {
                    setInProgressFileToErrorStatus(file);
                    CM_Manager.getInstance().updateProgressStatus();
                }
            } catch (Exception e) {
                //TODO: Log this
                log.aprintln("Error updating status of file: "+file.getCustomMapId()+" to error status");
            }
    //          log.aprintln(e);
            errorMsg = "ERROR: " + ex.getMessage();
            log.aprintln(errorMsg);
        } finally {
            if (request != null) {
                request.close();
            }
        }
        log.aprintln("Done uploading file: "+fileBasename);
        
    }
    
    public static void insertCustomMapRecord(CustomMap file) {
//            File actualFile = new File(file.getFilename());
        String fileBasename = file.getBasename();
        
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/insertRecord.php", HttpRequestType.POST);
        request.addRequestParameter("basename", fileBasename);
        
        double date = (new Date()).getTime(); //was getting duplicates, using date as part of canonic name
        String canonicName = Main.USER + "_" + Main.PRODUCT + "_" + Main.getCurrentBody() + "_" + String.valueOf((long)(fileBasename.hashCode()+(date + Math.random()*100))); 
        
        canonicName = canonicName.replaceAll("[^0-9A-Za-z\\.-]", "_");
        canonicName = canonicName.replace('.', '_');//Meg made me do this
        
        
        file.setCanonicName(canonicName);
//      request.addRequestParameter("rfile", file.getFilename());
//      request.addRequestParameter("lfile", canonicName);
        request.addRequestParameter("canonicName", canonicName);
        request.addRequestParameter("name", file.getName());
//            request.addUploadFileNew(fileBasename, actualFile);
        request.setRetryNever();
        
        String errorMsg = null;
        try {
//                request.setConnectionTimeout(300000);
            boolean successful = sendRequest(request);
            if (!successful) {
                errorMsg = "ERROR: Unexpected HTTP code " + request.getStatus() + " received";
            }
            String customMapId = null;
            String[] responseArr = Util.readLines(request.getResponseAsStream());
            
            if (responseArr != null && responseArr.length > 0) {
                String response = responseArr[0];
                if (response.startsWith("key:")) {
                    customMapId = response.substring(response.lastIndexOf(":")+1);
                    customMapId = customMapId.trim();
                    file.setCustomMapId(customMapId);
                    file.setStatus(UploadFile.STATUS_IN_PROGRESS);
                    file.setUploadDate(new Date().toString());
                } else {
                    //some type of error or warning occurred. 
                    for (String res : responseArr) {
                        log.aprintln(res);
                        System.err.println(res);
                    }
                    throw new Exception("Unexpected response from upload. See log for more details. ");
                }
                
            } else {
                throw new Exception("Null response from upload");
            }
        } catch (Exception ex) {
            file.setStatus(UploadFile.STATUS_ERROR);
            try {
                //if an error occurred, there may not be a custom map id set. If not, do not try to update the status in the database
                if (file.getCustomMapId() != null) {
                    setInProgressFileToErrorStatus(file);
                    CM_Manager.getInstance().updateProgressStatus();
                }
            } catch (Exception e) {
                log.aprintln("Error updating status of file: "+file.getCustomMapId()+" to error status");
            }
//          log.aprintln(e);
            errorMsg = "ERROR: " + ex.getMessage();
            log.aprintln(errorMsg);
        } finally {
            if (request != null) {
                request.close();
            }
        }
    }
 
    public static void insertReprocessCustomMapRecord(CustomMap file) {
    	//save out current file name to update record with
    	file.setReprocessOriginalCanonicName(file.getFilename());
    	String reprocessedParentId = file.getCustomMapId();
    	file.setReprocessParentId(file.getCustomMapId());

    	String newFileBasename = file.getBasename();
  
    	JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/insertReprocessRecord.php", HttpRequestType.POST);
//    	request.addRequestParameter("basename", newFileBasename);
  
    	double date = (new Date()).getTime(); //was getting duplicates, using date as part of canonic name
    	String canonicName = Main.USER + "_" + Main.PRODUCT + "_" + Main.getCurrentBody() + "_" + String.valueOf((long)(newFileBasename.hashCode()+(date + Math.random()*100))); 
  
    	canonicName = canonicName.replaceAll("[^0-9A-Za-z\\.-]", "_");
    	canonicName = canonicName.replace('.', '_');//Meg made me do this
  
    	file.setCanonicName(canonicName);
    	request.addRequestParameter("canonicName", canonicName);
    	request.addRequestParameter("parentId", reprocessedParentId);
    	request.addRequestParameter("name", file.getName());
       	request.setRetryNever();
  
    	String errorMsg = null;
    	try {
    		boolean successful = sendRequest(request);
    		if (!successful) {
    			errorMsg = "ERROR: Unexpected HTTP code " + request.getStatus() + " received";
    		}
    		String customMapId = null;
    		String[] responseArr = Util.readLines(request.getResponseAsStream());
      
    		if (responseArr != null && responseArr.length > 0) {
    			String response = responseArr[0];
    			if (response.startsWith("key:")) {
    				customMapId = response.substring(response.lastIndexOf(":")+1);
    				customMapId = customMapId.trim();
    				file.setCustomMapId(customMapId);
    				String stage = "4";
    				file.setStage(stage);
    			} else {
    				//some type of error or warning occurred. 
    				for (String res : responseArr) {
    					log.aprintln(res);
    					System.err.println(res);
    				}
    				throw new Exception("Unexpected response from upload. See log for more details. ");
    			}
          
    		} else {
    			throw new Exception("Null response from upload");
    		}
    	} catch (Exception ex) {
    		file.setStatus(UploadFile.STATUS_ERROR);
    		try {
          //if an error occurred, there may not be a custom map id set. If not, do not try to update the status in the database
    			if (file.getCustomMapId() != null) {
    				setInProgressFileToErrorStatus(file);
    				CM_Manager.getInstance().updateProgressStatus();
    			}
    		} catch (Exception e) {
    			log.aprintln("Error updating status of file: "+file.getCustomMapId()+" to error status");
    		}
    		errorMsg = "ERROR: " + ex.getMessage();
    		log.aprintln(errorMsg);
    	} finally {
    		if (request != null) {
    			request.close();
    		}
    	}
    }
    
    public static void renameReprocessImage(CustomMap file) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/moveReprocessImage.php", HttpRequestType.POST);
        request.addRequestParameter("originalCanonicName", file.getReprocessOriginalCanonicName());
        request.addRequestParameter("newCanonicName", file.getCanonicName());
        request.addRequestParameter("custom_map_key", file.getCustomMapId());
        
        boolean returnStatus = sendRequest(request);
    }
    
    /**
     * 
     * Updates the ArrayList of CustomMaps passed in with the header information from the database. This method will loop 
     * through the list of files once and update the header info. The header information may have been inserted by the 
     * custom map backend scripts. This method is meant for maps that are in the "in progress" status.
     *
     * @param ArrayList of CustomMap objects in the "in progress" state.
     * @throws Exception
     *
     */
    public static void getHeaderInformation(CustomMap file) {
        
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/getHeaderInformation.php", HttpRequestType.GET);
        try {
            request.addRequestParameter("custom_map_id", file.getCustomMapId());
            boolean returnStatus = sendRequest(request);
            if (returnStatus) {
                String[] lines = Util.readLines(request.getResponseAsStream());
                String jsonStr = buildString(lines);
                JSONObject json = new JSONObject(jsonStr);
                file.setHeaderInformation(json);
            } else {
                file.setStatus(UploadFile.STATUS_ERROR);
            }
        } catch (Exception e) {
            file.setStatus(UploadFile.STATUS_ERROR);
        } finally {
            if (request != null) {
                request.close();
            }
        }
        
    }
    /**
     * 
     * Kicks off the header processing on a file that has been successfully uploaded. Once the header information has been analyzed, 
     * the database is updated by back-end processes. The monitor thread will get these values and update the information for the GUI.
     * This method is called from the upload file thread.
     *
     * @param CustomMap object that has been uploaded and is ready to have the header analyzed
     * @throws Exception
     *
     */
    public static boolean startHeaderProcessing(CustomMap file) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/startHeaderAnalyze.php", HttpRequestType.POST);
        request.addRequestParameter("custom_map_id", file.getCustomMapId());
        boolean returnStatus = sendRequest(request);
        return returnStatus;
    }
    
    /**
     * 
     * This method will just update the status of the CustomMap objects that are passed in. It does this by calling the "updateStatusOfUpload" method.
     *
     * @param ArrayList<CustomMap> files: the objects to receive status updates based on calls to the back-end to see what the database reports as the status. 
     * @throws Exception
     * @thread-safe This gets called by the CustomMapMonitor thread. It should be thread safe. 
     * 
     */
    public static boolean checkMapProcessingStatus(List<CustomMap> files) { 
        for(int i=0; i<files.size(); i++) {
            CustomMap file = files.get(i);
            try {
                if (file.getCustomMapId() != null) {
                    updateStatusOfUpload(file);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }   
        }
        return true;
    }
    private static void setInProgressFileToErrorStatus(CustomMap file) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/setFileToError.php", HttpRequestType.POST);
        request.addRequestParameter("custom_map_id", file.getCustomMapId());
        boolean returnStatus = sendRequest(request);
    }
    /**
     *
     * Sends a request to the api to update the last time this map was used. This is done when the map is loaded into JMARS from the 
     * custom map manager.
     *
     * @param CustomMap object to be loaded.
     * @throws Exception
     *
     */
    public static void updateLastUsedTime(CustomMap cm) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/updateLastUsedTime.php", HttpRequestType.POST);
        request.addRequestParameter("custom_map_id", cm.getCustomMapId());
        boolean returnStatus = sendRequest(request);
    }
    private static void updateStatusOfUpload(CustomMap file) throws Exception {
        if (file != null && file.getCustomMapId() != null) {//don't check status for a file that has not had an id returned yet
            JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/checkStatus.php", HttpRequestType.GET);
            request.addRequestParameter("custom_map_id", file.getCustomMapId());
            request.setConnectionTimeout(10000);
            if (sendRequest(request)) {
                String[] lines = Util.readLines(request.getResponseAsStream());
                if ("error".equals(lines[0])){
                    log.aprintln("No valid status/stage found for id: "+file.getCustomMapId());
                    file.setStatus(UploadFile.STATUS_ERROR);
                    file.setProcessingStarted(false);
                } else {
                    JSONObject json = new JSONObject(buildString(lines));
                    if (json.length() > 0) {
                        String status = json.getString("status");
                        String stage = json.getString("stage");
                        
                        //both of the following checks are because we have changed the wording on the status to be more user friendly.
                        //We do not want to overwrite the status wording with the less friendly value from the database. So we do a couple checks
                        //to make sure that we leave the status alone when it is "Awaiting User Input" and "Processing".
                        
                        //Check for a state of waiting for user input that should not be updated
                        boolean updateStatus = true;//use a boolean to make it a bit more clear;
                        boolean updateStage = true;//same
                        if (UploadFile.STATUS_AWAITING_USER_INPUT.equalsIgnoreCase(file.getStatus()) && CustomMap.STAGE_AWAITING_USER_INPUT.equalsIgnoreCase(stage)) {
                        	//this means the file being checked is "awaiting user input" and the stage returned from the database is 5...we should leave it alone
                        	updateStatus = false;//don't update status
                        	updateStage = false;//leave it alone too
                        } else if (CustomMap.STATUS_IN_PROGRESS.equalsIgnoreCase(status) && CustomMap.STATUS_PROCESSING.equalsIgnoreCase(file.getStatus())) {
                            //status from database is still in progress and we have set the status for the user to "processing", leave it alone    
                        	updateStage = true;//stage could have been updated, set it so we can report it to the user
                        	updateStatus = false;//leave it alone
                        }
                        
                        //update the stage and status based on boolean above
                        if (updateStage) {
                        	file.setStage(stage);
                        }
                        if (updateStatus) {
                        	file.setStatus(status);
                        }
                        
                        //we want everyone to get updates to error messages and the rest below
                        file.setErrorMessage(json.getString("error"));
                        file.setValidHeaderFlag(json.getString("validHeader"));
                        if (CustomMap.STATUS_ERROR.equals(file.getStatus())) {
                            file.setProcessingStarted(false);
                        }
                    } else {
                        log.aprintln("Error retrieving Status/Stage of id: "+file.getCustomMapId());
                        file.setStatus(CustomMap.STATUS_ERROR);
                    }
                    
                }
                //error can be retrieved here too as "error"
            }
        }
    }
    public static boolean updateHeaderValuesAndStartProcessing(CustomMap selectedFile) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/setHeaderValues.php", HttpRequestType.POST);
        request.addRequestParameter("custom_map_id", selectedFile.getCustomMapId());
        
        //clean up corner longitude values
        String westernMost = selectedFile.getWestLon();
        String easternMost = selectedFile.getEastLon();
        double east = Double.parseDouble(easternMost);
        double west = Double.parseDouble(westernMost);
        
        if (selectedFile.getDegrees() == CustomMap.DEGREES_INPUT_WEST) {
            east = 360.0 - east;            
            west = 360.0 - west;
        }
        //end clean up corner points
        
        request.addRequestParameter("min_lat",selectedFile.getSouthLat());
        request.addRequestParameter("max_lat",selectedFile.getNorthLat());
        request.addRequestParameter("min_lon",String.valueOf(west));
        request.addRequestParameter("max_lon",String.valueOf(east));
        request.addUTF8String("units", selectedFile.getUnits());
        
        if (selectedFile.getIgnoreValue() != null && !selectedFile.getIgnoreValue().isEmpty()) {
            request.addRequestParameter("ignore_value", selectedFile.getIgnoreValue());//TODO: handle multiple
        }
        
        request.addRequestParameter("warp_method", String.valueOf(selectedFile.getWarpMethod()));//TODO: currently defaulted to Nearest Neighbor - add gui option
        
        String cpChanged = "0";//did the corner points change?
        String manualFlag = "0";//should only be manual or cpChanged, not both at the same time.
        if (selectedFile.getSelectedUploadProcess() == UploadFile.PROCESSING_OPTION_MANUAL
        	|| selectedFile.isReprocess()) {
            manualFlag = "1";
        } else if (selectedFile.getCornerPointsChanged()) {
            cpChanged = "1";
        }
        
        
        request.addRequestParameter("corner_points_updated",cpChanged);
        request.addUTF8String("map_name", selectedFile.getName());
        request.addUTF8String("description",selectedFile.getDescription());
        request.addUTF8String("links", selectedFile.getLinks());
        request.addUTF8String("citation", selectedFile.getCitation());
        request.addUTF8String("keywords", selectedFile.getKeywordsString());
        request.addRequestParameter("shape_type", selectedFile.getShapeTypeString());
        request.addRequestParameter("manual_flag", manualFlag);
        //for backwards compatibility, we are passing an ignoreValFlag
        String ignoreValFlag = "updated"; 
        request.addRequestParameter("ignoreValFlag", ignoreValFlag);
        boolean returnStatus = sendRequest(request);
        boolean success = false;
        if (returnStatus) {
            String[] lines;
			try {
				lines = Util.readLines(request.getResponseAsStream());
				if (lines != null && lines.length > 0) {
					String response = lines[0];
		            if (response.startsWith("{\"Error\"")){
//		            	System.out.println(response);
		            	success = false;
		            } else {
		            	processMap(selectedFile);
		            	success = true;
		            }
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
        }
        return success;

    }
    public static void uploadAndProcessMap(CustomMap file) {
        insertCustomMapRecord(file);
        file.setStatus(UploadFile.STATUS_UPLOADING);
        uploadCustomMap(file);
        file.setStatus(UploadFile.STATUS_READING_HEADER);
        boolean status = CustomMapBackendInterface.startHeaderProcessing(file);
        if (status) {
            getHeaderInformation(file);
            if (!CustomMap.STATUS_ERROR.equalsIgnoreCase(file.getStatus())) {//error, just be done
                if (file.getSelectedUploadProcess() == UploadFile.PROCESSING_OPTION_UPLOAD) {
                    if (file.isValidHeaderFlag()) {
                        file.prepareForOneStepProcessing();
                        updateHeaderValuesAndStartProcessing(file);
                    }
                } else {
                    //manual process
                    if (!updateHeaderValuesAndStartProcessing(file))  {
//                    	boolean successful = false;
                    	file.setStatus(UploadFile.STATUS_AWAITING_USER_INPUT);
                    	file.setStatus(CustomMap.STATUS_AWAITING_USER_INPUT);
						CM_Manager.getInstance().showErrorMessage();
                    }
                }
            }
        } 
    }
    public static boolean processReprocessMap(CustomMap file, String status) {
    	//update the map that is being reprocessed with the reprocess status
    	boolean success = false;
 //   	updateReprocessMap(file.getCustomMapId(), status, null); 
    	updateReprocessMap(file, file.getCustomMapId(), status, "14", file.getReprocessOrigUploadName()); 
    	
    	//insert a new record into the database for the reprocessed map
    	insertReprocessCustomMapRecord(file);
    	
    	//if there isn't an error or cancel process, update the values with the new information and reprocess
        if (!CustomMap.STATUS_ERROR.equalsIgnoreCase(file.getStatus()) || !CustomMap.STATUS_CANCELED.equalsIgnoreCase(file.getStatus())) {
        	renameReprocessImage(file);
    		if (!updateHeaderValuesAndStartProcessing(file)) {
    			success = false;
    		} else {
    			success = true;
    		}
    	}
        //if the process fails, change the original record back to status complete and NAME BACK???
        if (CustomMap.STATUS_ERROR.equalsIgnoreCase(file.getStatus()) || CustomMap.STATUS_CANCELED.equalsIgnoreCase(file.getStatus())) {
        	// if the reprocess failed, change the status of the original map from reprocessed to completed        	
 //       	CustomMapBackendInterface.updateReprocessMap(file.getReprocessParentId(), CustomMap.STATUS_COMPLETE, file.getReprocessOriginalCanonicName());//TODO: add name change back too?
        	CustomMapBackendInterface.updateReprocessMap(file, file.getReprocessParentId(), CustomMap.STATUS_COMPLETE, "13", file.getReprocessOrigUploadName());//TODO: add name change back too?
        }
        return success;
    }
    private static void updateReprocessMap(CustomMap cm, String customId, String status, String stage, String origReprocessUploadName) {
    	//update names, status and id's
    	JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/updateReprocessInfo.php", HttpRequestType.POST);
    	request.addRequestParameter("reprocessStatus", status);   
    	request.addRequestParameter("custom_map_id", customId);
    	request.addRequestParameter("stage", stage);
    	request.addRequestParameter("originalUploadName", origReprocessUploadName);
    	String errorMsg = null;
        boolean returnStatus = sendRequest(request);
        if (!returnStatus) {
			errorMsg = "ERROR: Unexpected HTTP code " + request.getStatus() + " received";
		}
		String[] responseArr;
		// if stage is 13, we are returning the original record back to normal. If it is 3, then we are updating and we need to 
		// verify the stage was returned successfully and update the file stage
		if (!stage.equalsIgnoreCase("13")) {
			try {
				responseArr = Util.readLines(request.getResponseAsStream());
				if (responseArr != null && responseArr.length > 0) {
					String response = responseArr[0];
					if (response.startsWith("stage:")) {
						stage = response.substring(response.lastIndexOf(":")+1);
						stage = stage.trim();
						cm.setStage(stage);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
    	
    }
    private static void processMap(CustomMap file) {
        log.aprintln("Processing map: "+file.getCustomMapId());
        //Reading the header is done, we are ready to show the user the results
        //hard coding this process for now and assuming everything is good, kicking off the processing of the image
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/startProcessing.php", HttpRequestType.POST);
        request.addRequestParameter("custom_map_id", file.getCustomMapId());
        boolean returnStatus = sendRequest(request);
        file.setProcessingStarted(true);
    }
    public static HashMap<String,String> populateReferenceTable() {
    	//call api script to get reference table information
        HashMap<String,String> stageReferenceTable = null;
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/getStageReferenceTable.php", HttpRequestType.GET);
        boolean returnStatus = sendRequest(request);
        if (returnStatus) {
            try {
				stageReferenceTable = buildStageReferenceTableFromJSON(request);
			} catch (Exception e) {
				log.println(e.getMessage());
			}
            if (stageReferenceTable == null) {
            	stageReferenceTable = new HashMap<String,String>();
            }
        }
        //error with the request will return null. This will tell the process to stop sending requests
        return stageReferenceTable;
        
    }
    public static void refreshCapabilities() {
        try {
            CustomMapServer customServer = MapServerFactory.getCustomMapServer();
            customServer.loadCapabilities();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void finishDeleteCustomMap(CustomMap map) {
        try {
            CustomMapServer customServer = MapServerFactory.getCustomMapServer();
            customServer.finishDeleteExistingMap(map.getFilename());
            SearchProvider.getInstance().removeCustomMapSearchRow(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void finishMapEdit(CustomMap map) {
        try {
            CustomMapServer customServer = MapServerFactory.getCustomMapServer();
            double[] ignoreVals = null;
            String ignoreValue = map.getIgnoreValue();
            if (ignoreValue != null && ignoreValue.trim().length() > 0) {
                ignoreVals = new double[]{Double.parseDouble(ignoreValue)};
            }
            customServer.finishCustomMapEdit(map.getFilename(),map.getName(), map.getCitation(), ignoreVals);
            LayerParameters lp = map.getLayerParameters();
            if (lp != null) {//check that we have initialized add layer
	            lp.citation = map.getCitation();
	            lp.name = map.getName();
	            lp.setLinks(map.getLinks());
	            lp.description = map.getDescription();
	            SearchProvider.getInstance().rebuildSearchRow(map.getCustomMapId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static boolean saveMetadata(CustomMap map) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/saveMetadata.php", HttpRequestType.POST);
        request.addRequestParameter("custom_map_id", map.getCustomMapId());
        //name, units, kw, desc
        request.addUTF8String("units",map.getUnits());
        request.addUTF8String("name",map.getName());
        request.addUTF8String("description",map.getDescription());
        request.addUTF8String("links", map.getLinks());
        request.addUTF8String("citation", map.getCitation());
        if (map.getIgnoreValue() != null) {
            request.addUTF8String("ignoreValue", map.getIgnoreValue());
        }
        request.addRequestParameter("ignoreValFlag", "updated");
        boolean success = true;
        StringBuffer buff = new StringBuffer();
        boolean first = true;
        for(String kw : map.getKeywords()) {
            if (!first) {
                buff.append(",");
            }
            buff.append(kw);
            first = false;
        }
        request.addUTF8String("keywords",buff.toString());
        boolean returnStatus = sendRequest(request);
        if (returnStatus) {
            String[] lines;
			try {
				lines = Util.readLines(request.getResponseAsStream());
				if (lines != null && lines.length > 0) {
					String response = lines[0];
		            if (response.startsWith("{\"Error\"")){
//		            	System.out.println(response);
		            	success = false;
		            } else {
		            	success = true;
		            }
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
        }
//        if (returnStatus) {
//            refreshCapabilities();
//        }
		return success;
    }

    public static boolean deleteCustomMap(CustomMap map) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/deleteExisting.php", HttpRequestType.POST);
        request.addRequestParameter("custom_map_id", map.getCustomMapId());
        return sendRequest(request);
    }
    public static void setCancelFlag(CustomMap file) {
        file.setCancelFlag(true);
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/setCancelFlag.php", HttpRequestType.POST);
        request.addRequestParameter("custom_map_id", file.getCustomMapId());
        boolean returnStatus = sendRequest(request);
    }
    public static boolean cancelUpload(CustomMap file) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/cancelUpload.php", HttpRequestType.POST);
        request.addRequestParameter("custom_map_id", file.getCustomMapId());
        boolean returnStatus = sendRequest(request);
        return returnStatus;
    }
    public static boolean deleteUploadFile(CustomMap file) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/deleteInProgress.php", HttpRequestType.POST);
        request.addRequestParameter("custom_map_id", file.getCustomMapId());
        return sendRequest(request);
    }
    public static boolean originalMapExists(CustomMap map, String fileName) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/fileExists.php", HttpRequestType.GET);
        request.addRequestParameter("custom_map_id", map.getCustomMapId());
        return sendRequest(request);
    }
//    public static void shareMaps(ArrayList<String> users, ArrayList<SharingGroup> groups, ArrayList<CustomMap> maps) {
//        for(CustomMap map : maps) {
//            for (SharingGroup group : groups) {
//                shareMapWithGroup(map, String.valueOf(group.getId()));
//            }
//            shareMapWithUsers(map, users);
//        }
//    }
//    
//    public static void shareMap(CustomMap sm){
//    	CustomMap map = sm;
//    	for(SharingGroup sg : sm.getSharedWithGroups()){
//    		shareMapWithGroup(map, String.valueOf(sg.getId()));
//    	}
//    	shareMapWithUsers(map, sm.getSharedWithUsers());
//    }
    
    public static boolean shareMapWithUsers(CustomMap map) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/shareMapWithUsers.php", HttpRequestType.POST);
        request.addRequestParameter("custom_map_id", map.getCustomMapId());
        JSONArray userJSON = new JSONArray(map.getTempSharedWithUsers());
        request.addRequestParameter("userList", userJSON.toString());
        return sendRequest(request);
       
    }
    public static boolean shareMapWithGroups(CustomMap map) {
        unshareMapWithGroups(map);
        ArrayList<SharingGroup> sharedWithGroups = map.getTempSharedWithGroups();
        for (SharingGroup group : sharedWithGroups) {
            String groupKey = group.getId();
            JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/shareMapWithGroup.php", HttpRequestType.POST);
            request.addRequestParameter("custom_map_id", map.getCustomMapId());
            request.addRequestParameter("groupKey", groupKey);
            sendRequest(request);
        }
        
        return true;
    }
    public static ArrayList<CustomMap> getSharedMapsForUser(ArrayList<CustomMap> customMaps, ArrayList<SharingGroup> allGroups) {
        ArrayList<CustomMap> sharedMapList = null;
        try {
            JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/getSharedMaps.php", HttpRequestType.GET);
            boolean returnStatus = sendRequest(request);
            if (returnStatus) {
                sharedMapList = buildSharedMapListFromJSON(request,customMaps,allGroups);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sharedMapList;
    }
    
    public static void editSharingGroup(SharingGroup group) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"groups/edit.php", HttpRequestType.POST);
        ArrayList<String> users = group.getUsers();
        JSONArray userJSON = new JSONArray(users);
        request.addRequestParameter("userList", userJSON.toString());
        request.addRequestParameter("groupName", group.getName());
        request.addRequestParameter("groupKey",String.valueOf(group.getId()));
        boolean returnStatus = sendRequest(request);
    }
    public static void deleteSharingGroup(SharingGroup group) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"groups/delete.php", HttpRequestType.POST);
        request.addRequestParameter("groupKey", String.valueOf(group.getId()));
        boolean returnStatus = sendRequest(request);
    }
    /**
     * This method will unshare a map with all users and groups
     *
     * @param map - CustomMap object
     *
     */
    public static void unshareMapWithGroups(CustomMap map) {
    	JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"maps/unshareMapWithGroups.php", HttpRequestType.POST);
        request.addRequestParameter("custom_map_id", map.getCustomMapId());
        boolean returnStatus = sendRequest(request);
    }
    public static void createGroup(SharingGroup group) {
        try {
            JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"groups/create.php", HttpRequestType.POST);
            JSONArray userJSON = new JSONArray(group.getUsers());
            request.addRequestParameter("userList", userJSON.toString());
            request.addRequestParameter("groupName", group.getName());
            if (sendRequest(request)) {
                String[] lines = Util.readLines(request.getResponseAsStream());
                for(String line : lines) {
                    if (line != null && line.startsWith("groupKey: ")) {
                        String key = line.substring(line.indexOf(":")+1);
                        key = key.trim();
                        group.setId(key);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static boolean validateUserForGroup(String username) {
        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"groups/validateUserForGroup.php", HttpRequestType.GET);
        request.addRequestParameter("username",username);
        try {
            if (sendRequest(request)) {
                String[] lines = Util.readLines(request.getResponseAsStream());
                //should just be one line, valid or invalid
                if (lines.length != 1) {
                    throw new Exception("invalid response from groups/validateUsersForGroup: length was "+lines.length + ", username was: "+username);
                } else {
                    if ("valid".equals(lines[0])) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.aprintln(e.getMessage());
        }
        return false;
    }

    public static ArrayList<SharingGroup> getSharingGroupList() {
        ArrayList<SharingGroup> groupShareList = null;
        try{
	        JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"groups/select.php", HttpRequestType.GET);
	        boolean returnStatus = sendRequest(request);
	        if (returnStatus) {
	            groupShareList = buildSharedGroupsFromJSON(request);
	            Collections.sort(groupShareList, SharingGroup.NameComparator);
	        }
        } catch (Exception e){
        	System.err.println("Failure getting groups");
        	e.printStackTrace();
        }
        return groupShareList;
    }
    public static ArrayList<String> getUserSharingList() {
        ArrayList<String> userShareList = new ArrayList<String>();
        try{
            JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"groups/selectUserSharingList.php", HttpRequestType.GET);
            boolean returnStatus = sendRequest(request);
            if (returnStatus) {
                String json = buildString(Util.readLines(request.getResponseAsStream()));
                JSONObject obj = new JSONObject(json);
                String userList = (obj.isNull("user_list") ? null : obj.getString("user_list"));
                if (userList != null && userList.trim().length() > 0) {
                    String[] users = userList.split(",", 0);
                    userShareList.addAll(Arrays.asList(users));
                }
            }
        } catch (Exception e){
            System.err.println("Failure getting user share list");
            e.printStackTrace();
        }
        return userShareList;
    }
    public static boolean updateSharingUserList(ArrayList<String> userList) {
        boolean returnStatus = false;
        try{
            JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"groups/updateSharingUserList.php", HttpRequestType.POST);
            StringBuffer theList = new StringBuffer();
            boolean first = true;
            for (String user : userList) {
                if (!first) {
                    theList.append(",");
                }
                theList.append(user);
                first = false;
            }
            request.addRequestParameter("userList",theList.toString());
            returnStatus = sendRequest(request);
        } catch (Exception e){
            System.err.println("Failure setting user share list");
            e.printStackTrace();
        }
        return returnStatus;
    }
    public static void populateSharingGroup(SharingGroup group) {
        try{
            JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"groups/getGroupInfo.php", HttpRequestType.GET);
            request.addRequestParameter("groupName",group.getName());
            boolean returnStatus = sendRequest(request);
            if (returnStatus) {
                String json = buildString(Util.readLines(request.getResponseAsStream()));
                JSONObject obj = new JSONObject(json);
                group.setId(obj.getString("key"));
                ArrayList<String> userList = new ArrayList<String>();
                JSONArray jsonArr = obj.getJSONArray("users");
                for(int x=0; x<jsonArr.length(); x++) {
                    if (jsonArr.getString(x) != null) {
                        userList.add(jsonArr.getString(x));
                    }
                }
                group.setUsers(userList);
            }
        } catch (Exception e){
            System.err.println("Failure getting groups");
            e.printStackTrace();
        }
    }
    
    public static boolean checkValidUser(String username) {
        try{
            JmarsHttpRequest request = new JmarsHttpRequest(customMapServerURL+"groups/checkValidUser.php", HttpRequestType.GET);
            request.addRequestParameter("username",username);
            boolean returnStatus = sendRequest(request);
            if (returnStatus) {
                String[] response = Util.readLines(request.getResponseAsStream());
                if (response != null && response.length > 0 && response[0].equalsIgnoreCase("valid")) {
                    return true;
                }
            }
        } catch (Exception e){
            System.err.println("Failure getting groups");
            e.printStackTrace();
        }
        return false;
    }
    public static void setCustomMapServerURL() {
        customMapServerURL = Config.get("custom_map_server","https://cm.mars.asu.edu/api/");
    }
    private static String buildString(String[] lines) {
        String newLine = System.getProperty("line.separator");
        StringBuilder result = new StringBuilder();
        boolean flag = false;
        for (String line : lines) {
            result.append(flag? newLine: "").append(line);
            flag = true;
        }
        return result.toString();
    }
    private static ArrayList<SharingGroup> buildSharedGroupsFromJSON(JmarsHttpRequest request) throws Exception {
        ArrayList<SharingGroup> sharingGroupList = new ArrayList<SharingGroup>();
        String json = buildString(Util.readLines(request.getResponseAsStream()));
        JSONObject obj = new JSONObject(json);
        String[] groups = JSONObject.getNames(obj);
        if(groups != null) {
            for (int i=0; i<groups.length; i++) {
                JSONObject group = (JSONObject) obj.get(groups[i]);
                String groupKey = group.getString("key");
                JSONArray names = (JSONArray) group.get("list");
                ArrayList<String> nameList = new ArrayList<String>();
                for(int j=0; j<names.length(); j++) {
                    String name = names.getString(j);
                    nameList.add(name);
                }
                SharingGroup sg = new SharingGroup(groupKey, groups[i].trim(), nameList);
                sharingGroupList.add(sg);
            }
        }
        
        return sharingGroupList;
    }
    private static HashMap<String,String> buildStageReferenceTableFromJSON(JmarsHttpRequest request) throws Exception {
    	HashMap<String,String> stageReferenceTable = new HashMap<String,String>();
        String json = buildString(Util.readLines(request.getResponseAsStream()));
        JSONArray statusTableArray = new JSONArray(json);
        for (int i=0; i<statusTableArray.length(); i++) {
            JSONObject obj = (JSONObject) statusTableArray.get(i);
            String cm_stage_num = obj.getString("cm_stage_num");
            String cm_stage_title = obj.getString("cm_stage_title");
            stageReferenceTable.put(cm_stage_num, cm_stage_title);
            }
        return stageReferenceTable;
    }
    private static ArrayList<CustomMap> buildSharedMapListFromJSON(JmarsHttpRequest request, ArrayList<CustomMap> allCustomMaps, ArrayList<SharingGroup> allGroups) throws Exception {
        ArrayList<CustomMap> sharedWithMaps = new ArrayList<CustomMap>();
        String json = buildString(Util.readLines(request.getResponseAsStream()));
        JSONArray sharedMapArray = new JSONArray(json);
        for (int i=0; i<sharedMapArray.length(); i++) {
            JSONObject obj = (JSONObject) sharedMapArray.get(i);
            String id = obj.getString("id");
            
            CustomMap cm = null;
            for (CustomMap map : allCustomMaps) {
                //loop through the custom maps to find the right custom map object and break out of loop
                if (map.getCustomMapId().equals(id.trim())) {
                    cm = map;
                    break;
                }
            }
            if (cm == null) {
                log.aprintln("Custom Map ID: "+id+" was not found in the custom map list, but was returned with sharing information. This is an Error state. Skipping...");
                continue;
            }
            
            String sharedWith = obj.getString("shareName");
            String sharedType= obj.getString("shareType");
            if ("group".equals(sharedType)) {
                for (SharingGroup group : allGroups) {
                    if (group.getName().equals(sharedWith)) {
                        cm.addSharedGroup(group);
                        cm.addTempSharedGroup(group);
                    }
                }
            } else {
                cm.addSharedUser(sharedWith);
                cm.addTempSharedUser(sharedWith);
            }
            if (!sharedWithMaps.contains(cm)) {
                sharedWithMaps.add(cm);
            }
        }
        return sharedWithMaps;
    }
    private static ArrayList<CustomMap> buildExistingMapsFromJSON(JmarsHttpRequest request) throws Exception {
        ArrayList<CustomMap> customMapList = new ArrayList<CustomMap>();
        String json = buildString(Util.readLines(request.getResponseAsStream()));
        JSONArray mapArray = new JSONArray(json);
        for (int i=0; i<mapArray.length(); i++) {
            JSONObject obj = (JSONObject) mapArray.get(i);
            CustomMap cm = new ExistingMap(obj);
            customMapList.add(cm);
        }
        
        return customMapList;
    }
    private static ArrayList<CustomMap> buildUploadFileFromJSON(JmarsHttpRequest request) throws Exception {
        ArrayList<CustomMap> uploadList = new ArrayList<CustomMap>();
        String json = buildString(Util.readLines(request.getResponseAsStream()));
        JSONArray mapArray = new JSONArray(json);
        for (int i=0; i<mapArray.length(); i++) {
            JSONObject obj = (JSONObject) mapArray.get(i);
            UploadFile cm = new UploadFile(obj);
            uploadList.add(cm);
        }
        
        return uploadList;
    }
    
    //all requests should be sent through this method which will send the userid and password along
    private static boolean sendRequest(JmarsHttpRequest request) {
        boolean returnStatus = false;
        try {
            request.addRequestParameter("userid", Main.USER);
            request.addRequestParameter("passwd", Main.PASS);
            request.addRequestParameter("domain", Main.AUTH_DOMAIN);
            request.addRequestParameter("body", Main.getCurrentBody().toLowerCase());
            request.addRequestParameter("custom_map_version",CustomMapServer.getVersionNumber());
            returnStatus = request.send();
            if (!returnStatus) {
                int status = request.getStatus();
                if (status == HttpStatus.SC_UNAUTHORIZED) {
                    log.aprintln("user not authorized to access custom map server");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnStatus;
    }
    
 
}
