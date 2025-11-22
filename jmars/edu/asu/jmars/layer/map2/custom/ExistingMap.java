package edu.asu.jmars.layer.map2.custom;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;

import org.json.JSONObject;

import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.map2.CustomMapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;

public class ExistingMap implements CustomMap {
    public static final char NEAREST_NEIGHBOR = 'N';
    public static final char CUBIC = 'C';
    public static final char BILINEAR = 'B';
    
    private String name;
    private String description;
    private String citation = "";
    private String links = "";
    private String units;
    private String[] keywords;
    private ArrayList<String> ignoreVals = new ArrayList<String>();
    private String id;
    private String owner;
    private ArrayList<String> sharedWithUsers = new ArrayList<String>();
    private ArrayList<SharingGroup> sharedWithGroups = new ArrayList<SharingGroup>(); 
    private ArrayList<String> tempSharedWithUsers = new ArrayList<String>();
    private ArrayList<SharingGroup> tempSharedWithGroups = new ArrayList<SharingGroup>();
    private MapSource graphicSource;
    //TODO: might want some other object than String for the Dates?
    private String uploadDate;
    private String lastUsedDate;
    private String filename;
    /** Stored in Deg E **/
    private String minLon;
    /** Stored in Deg E **/
    private String maxLon;
    private String minLat;
    private String maxLat;
    private String mapType;
    private String maxPPD;
    private String domain;
    private String body;
    private String canonicName;
    private String basename;
    private String wktString;
    private int shapeType;
    private boolean isNumeric;
    private boolean mapExists;
    
    //likely not used
    private int extent;
    private int degrees;
    //end not used
    
    private String origMinLon;
    private String origMaxLon;
    private String origMinLat;
    private String origMaxLat;

    private boolean reprocessFlag = false;
    
    private boolean cornerPointsChanged = false;
    
    public char warpMethod = NEAREST_NEIGHBOR;
    
    //TODO: use this for returning strings of the dates
    public static DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.sssss");
    
    private LayerParameters lp = null;

    public ExistingMap() {
        
    }
    public ExistingMap(JSONObject obj) throws Exception{
    	id = (obj.isNull("id") ? "" : obj.getString("id"));
    	name = (obj.isNull("name") ? "" : obj.getString("name"));
    	filename = (obj.isNull("filename") ? "" : obj.getString("filename"));;
    	minLat = (obj.isNull("min_lat") ? "0.00" : obj.getString("min_lat"));
    	minLon = (obj.isNull("min_lon") ? "0.00" : obj.getString("min_lon"));
        maxLat = (obj.isNull("max_lat") ? "0.00" : obj.getString("max_lat"));
        maxLon = (obj.isNull("max_lon") ? "0.00" : obj.getString("max_lon"));
        mapType = (obj.isNull("type") ? "" : obj.getString("type"));
        maxPPD = (obj.isNull("max_ppd") ? "0.00" : obj.getString("max_ppd"));
        ignoreVals.add((obj.isNull("ignore_value") ? "" : obj.getString("ignore_value")));
        domain = (obj.isNull("domain") ? "" : obj.getString("domain"));
        description = (obj.isNull("abstract") ? "" : obj.getString("abstract"));
        body = (obj.isNull("jmars_body") ? "" : obj.getString("jmars_body"));
        units = (obj.isNull("units") ? "" : obj.getString("units"));
        String kws = (obj.isNull("keywords") ? "" : obj.getString("keywords"));
        keywords = kws.split(",", -1);
        lastUsedDate = (obj.isNull("last_used") ? "" : obj.getString("last_used"));
        uploadDate= (obj.isNull("upload_date") ? "" : obj.getString("upload_date"));
        citation = (obj.isNull("citation") ? "" : obj.getString("citation"));
        links = (obj.isNull("links") ? "" : obj.getString("links"));
        owner = (obj.isNull("username") ? "" : obj.getString("username"));
        wktString = (obj.isNull("wktString") ? "" : obj.getString("wktString"));
        basename = (obj.isNull("basename") ? "" : obj.getString("basename"));
        String shapeTypeTemp = (obj.isNull("shape_type") ? null : obj.getString("shape_type"));
        if (shapeTypeTemp != null) {
	        if (CustomMap.SHAPE_OG.equalsIgnoreCase(shapeTypeTemp)) {
	            shapeType = CustomMap.SHAPE_INPUT_OGRAPHIC;
	        } else if (CustomMap.SHAPE_OC.equalsIgnoreCase(shapeTypeTemp)) {
                shapeType = CustomMap.SHAPE_INPUT_OCENTRIC;
            }
        }
    	isNumeric = false;
        String isNumTemp = (obj.isNull("is_numeric") ? null : obj.getString("is_numeric"));
        if (isNumTemp != null && "1".equals(isNumTemp)) {
        	isNumeric = true;
        }
        
        JSONObject headerInformation = (obj.isNull("header_info") ? null : (JSONObject) obj.get("header_info"));
        if (headerInformation != null) {//can be null when getting existing maps tab
            origMaxLat = (headerInformation.isNull(CustomMap.ORIG_MAX_LAT) ? "0.00" : headerInformation.getString(CustomMap.ORIG_MAX_LAT));
            origMinLat = (headerInformation.isNull(CustomMap.ORIG_MIN_LAT) ? "0.00" : headerInformation.getString(CustomMap.ORIG_MIN_LAT));
            origMaxLon = (headerInformation.isNull(CustomMap.ORIG_MAX_LON) ? "0.00" : headerInformation.getString(CustomMap.ORIG_MAX_LON));
            origMinLon = (headerInformation.isNull(CustomMap.ORIG_MIN_LON) ? "0.00" : headerInformation.getString(CustomMap.ORIG_MIN_LON));
        }
        this.formatCornerPoints();
    }
    
    public ExistingMap(String name){
    	this.name = name;
    }
    
    public boolean cornerPointsChanged() {
        return this.cornerPointsChanged;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getUnits() {
        return (units == null ? "" : units);
    }
    public void setUnits(String units) {
        this.units = units;
    }
    public String[] getKeywords() {
    	if(keywords == null){
    		keywords = new String[0];
    	} 
        return keywords;
    }
    @Override
    public String getKeywordsString() {
        String text = "";
        for(int i=0; i<keywords.length; i++){
            if(i>0){
                text+= ", ";
            }
            text += keywords[i];
        }
        return text;
    }
    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getOwner() {
        return owner;
    }
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    public MapSource getGraphicSource() {
    	if(graphicSource == null){
        	//get and set the graphic mapsource
    		if (MapServerFactory.getCustomMapServer() != null) {
    			CustomMapServer customServer = MapServerFactory.getCustomMapServer();
                graphicSource = customServer.getSourceByName(filename);
    		}
        	
    	}
        return graphicSource;
    }
    public void setGraphicSource(MapSource graphicSource) {
        this.graphicSource = graphicSource;
    }
    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getMapType() {
        return mapType;
    }
    public void setMapType(String mapType) {
        this.mapType = mapType;
    }
    @Override
    public void addIgnoreValue(String val) {
        if (val != null && val.trim().length() > 0) {
            this.ignoreVals.add(0,val);//only handling one for now
        } else {
            this.ignoreVals.add(0,"");
        }
    }
    public String getIgnoreValue() {
        if (this.ignoreVals.size() > 0) {
            return this.ignoreVals.get(0);
        } else {
            return null;
        }
    }
    public ArrayList<String> getIgnoreValues() {
        return ignoreVals;
    }
    public void setIgnoreValues(ArrayList<String> ignoreValues) {
        ignoreVals = ignoreValues;
    }
    public String getDomain() {
        return domain;
    }
    public void setDomain(String domain) {
        this.domain = domain;
    }
    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }

    public ArrayList<String> getIgnoreVals() {
        return ignoreVals;
    }

    public void setIgnoreVals(ArrayList<String> ignoreVals) {
        this.ignoreVals = ignoreVals;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getMaxPPD() {
        return maxPPD;
    }

    public void setMaxPPD(String maxPPD) {
        this.maxPPD = maxPPD;
    }
    
    public void setLastUsedDate(String now) {
        CustomMapBackendInterface.updateLastUsedTime(this);
    	lastUsedDate = now;
    }
    
    public String getLastUsedDate(){
    	return lastUsedDate;
    }
    
    public String getCitation(){
    	return citation;
    }
    
    public void setCitation(String cit){
    	citation = cit;
    }
    
    /**
     * @return A comma separated list of url links
     */
    public String getLinks(){
    	return links;
    }
    
    public void setLinks(String newLinks){
    	links = newLinks;
    }
    
	public static Comparator<CustomMap> NameComparator = new Comparator<CustomMap>() {
		public int compare(CustomMap o1, CustomMap o2) {
			return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
		}
	};
	
	public String getOrigMinLon() {
        return origMinLon;
    }
    public void setOrigMinLon(String origMinLon) {
        this.origMinLon = origMinLon;
    }
    public String getOrigMaxLon() {
        return origMaxLon;
    }
    public void setOrigMaxLon(String origMaxLon) {
        this.origMaxLon = origMaxLon;
    }
    public String getOrigMinLat() {
        return origMinLat;
    }
    public void setOrigMinLat(String origMinLat) {
        this.origMinLat = origMinLat;
    }
    public String getOrigMaxLat() {
        return origMaxLat;
    }
    public void setOrigMaxLat(String origMaxLat) {
        this.origMaxLat = origMaxLat;
    }
    @Override
    public char getWarpMethod() {
        return this.warpMethod;
    }
    public void setWarpMethod(char warp) {
        this.warpMethod = warp;
    }
    
    @Override
    public void setExtent(int extentVal) {
        this.extent = extentVal;
    }
    @Override
    public void setDegrees(int degreesVal) {
        this.degrees = degreesVal;
    }
    @Override
    public void setNorthLat(String northLatVal) {
        this.maxLat = northLatVal;
    }
    @Override
    public void setSouthLat(String southLatVal) {
        this.minLat = southLatVal;
    }
    @Override
    public void setWestLon(String westLonVal) {
        this.minLon = westLonVal;
    }
    @Override
    public void setEastLon(String eastLonVal) {
        this.maxLon = eastLonVal;
    }
    @Override
    public void setKeywords(String keywordsVal) {
        this.keywords = keywordsVal.split(",",0);
    }
    @Override
    public void setCanonicName(String canonicNameVal) {
        this.canonicName = canonicNameVal;
    }
    @Override
    public void setCustomMapId(String customMapId) {
        this.id = customMapId;
    }
    @Override
    public void setBasename(String basenameVal) {
        this.basename = basenameVal;
    }
    @Override
    public void setOriginalNorthLat(String origNorthLatVal) {
        this.origMaxLat = origNorthLatVal;
    }
    @Override
    public void setOriginalSouthLat(String origSouthLatVal) {
        this.origMinLat = origSouthLatVal;
    }
    @Override
    public void setOriginalWestLon(String origWestLonVal) {
        this.origMinLon = origWestLonVal;
    }
    @Override
    public void setOriginalEastLon(String origEastLonVal) {
        this.origMaxLon = origEastLonVal;
    }
    @Override
    public void setStatus(String statusVal) {
        //nothing to see here
    }
    @Override
    public void setStage(String stageVal) {
        //nothing to see here
    }
    @Override
    public void setErrorMessage(String errorMsgVal) {
        //nothing to see here
    }
    @Override
    public int getExtent() {
        return this.extent;
    }
    @Override
    public int getDegrees() {
        return this.degrees;
    }
    @Override
    public String getNorthLat() {
        return maxLat;
    }
    @Override
    public String getSouthLat() {
        return minLat;
    }
    @Override
    public String getWestLon() {
        return minLon;
    }
    @Override
    public String getEastLon() {
        return maxLon;
    }

    @Override
    public String getCanonicName() {
        return this.canonicName;
    }
    @Override
    public String getCustomMapId() {
        return id;
    }
    @Override
    public String getBasename() {
        return this.basename;
    }
    @Override
    public String getOriginalNorthLat() {
        return this.origMaxLat;
    }
    @Override
    public String getOriginalSouthLat() {
        return this.origMinLat;
    }
    @Override
    public String getOriginalWestLon() {
        return this.origMinLon;
    }
    @Override
    public String getOriginalEastLon() {
        return this.origMaxLon;
    }
    @Override
    public String getStatus() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public String getStage() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public String getErrorMessage() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public boolean isCanceled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setProcessingStarted(boolean started) {
        //nothing to see here
    }
    @Override
    public void setSelectedUploadProcess(int process) {
        //nothing to see here
    }
    @Override
    public int getSelectedUploadProcess() {
        //nothing to do here
        return -1;
    }
    @Override
    public boolean hasStartedUpload() {
        // nothing to do here
        return false;
    }
    @Override
    public boolean isErrorCondition() {
        // nothing to do here
        return false;
    }
    @Override
    public void startUpload() {
        //nothing to see here
    }
    @Override
    public void setHeaderInformation(JSONObject json) {
        //nothing to see here?
    }
    @Override
    public boolean isValidHeaderFlag() {
        // nothing to see here
        return false;
    }
    @Override
    public void prepareForOneStepProcessing() {
        //nothing to see here
    }
    @Override
    public String getWktString() {
        return wktString;
    }
    public void setWktString(String wktString) {
        this.wktString = wktString;
    }
    @Override
    public boolean checkCapabilitiesRun() {
        // nothing to see here
        return false;
    }
    @Override
    public void capabilitiesHaveBeenRun() {
        //nothing to see here
    }
    @Override
    public boolean hasProcessingStarted() {
        // nothing to see here
        return false;
    }
    @Override
    public void setCancelFlag(boolean b) {
        //nothing to see here
    }
    
    @Override
    public void setValidHeaderFlag(String validFlag) {
        //nothing to see here
    }
    @Override
    public boolean getCornerPointsChanged() {
        return this.cornerPointsChanged;
    }
    @Override
    public String getNumberOfBands() {
        // TODO: could be useful?
        return null;
    }
    @Override
    public void formatCornerPoints() {
        try {
            if (this.getNorthLat() == null || this.getNorthLat().equals("")) {
                String origNLat = this.getOriginalNorthLat();
                if (origNLat == null || origNLat.trim().equals("")) {
                    this.setNorthLat("");
                } else {
                    double northLat = Double.parseDouble(origNLat);
                    this.setNorthLat(String.format("%.6f",northLat));
                }
            } else {
                double northLat = Double.parseDouble(this.getNorthLat());
                //update the upload this so that other clicks that will reset will have the right value
                this.setNorthLat(String.format("%.6f",northLat));
            }
            if (this.getSouthLat() == null || this.getSouthLat().equals("")) { 
                String origSLat = this.getOriginalSouthLat();
                if (origSLat == null || origSLat.trim().equals("")) {
                    this.setSouthLat("");
                } else {
                    double southLat = Double.parseDouble(origSLat);
                    this.setSouthLat(String.format("%.6f", southLat));
                }
            } else {
                double southLat = Double.parseDouble(this.getSouthLat());
                this.setSouthLat(String.format("%.6f",southLat));
            }
            if (this.getWestLon() == null || this.getWestLon().equals("")) { 
                String origWLon = this.getOriginalWestLon();
                if (origWLon == null || origWLon.trim().equals("")) {
                    this.setWestLon("");
                } else {
                    double westLon = Double.parseDouble(origWLon);
                    this.setWestLon(String.format("%.6f",westLon));
                }
            } else {
                double westLon = Double.parseDouble(this.getWestLon());
                this.setWestLon(String.format("%.6f",westLon));
            }
            if (this.getEastLon() == null || this.getEastLon().equals("")) { 
                String origELon = this.getOriginalEastLon();
                if (origELon == null || origELon.trim().equals("")) {
                    this.setEastLon("");
                } else {
                    double eastLon = Double.parseDouble(origELon);
                    this.setEastLon(String.format("%.6f",eastLon));
                }
            } else {
                double eastLon = Double.parseDouble(this.getEastLon());
                this.setEastLon(String.format("%.6f",eastLon));
            }
        } catch (NumberFormatException nfe) {
            //do nothing. This might need to be on each parse
        }
    }
    @Override
    public boolean isSharedWithOthers() {
        if ((sharedWithUsers != null && sharedWithUsers.size() > 0)
             || (sharedWithGroups != null && sharedWithGroups.size() > 0)) {
            return true;
        }
        return false;
    }
    @Override
    public void setupGCPsForCompare() {
        // nothing to do here
        
    }
    @Override
    public ArrayList<String> getSharedWithUsers(){
        return sharedWithUsers;
    }
    
    @Override
    public ArrayList<SharingGroup> getSharedWithGroups(){
        return sharedWithGroups;
    }
    @Override
    public String getSharedWithUsersString(){
        String str = "";
        
        for(String name : sharedWithUsers){
            str += name+", ";
        }
        
        if (str.length() > 0) {
            str = str.substring(0, str.length()-2);
        }
        return str;
    }
    @Override
    public String getSharedWithGroupsString(){
        String str = "";
        
        for(SharingGroup grp : sharedWithGroups){
            str += grp.getName()+", ";
        }
        
        if (str.length() > 0) {
            str = str.substring(0, str.length()-2);
        }
        return str;
    }
    @Override
    public void addSharedUser(String user) {
        if (!sharedWithUsers.contains(user)) {
            sharedWithUsers.add(user);
        }
    }
    @Override
    public void addSharedGroup(SharingGroup group) {
        if (!sharedWithGroups.contains(group)) {
            sharedWithGroups.add(group);
        }
    }
    
    @Override
    public void removeSelectedGroup(String groupName) {
        SharingGroup toRemove = null;
        for (SharingGroup group : sharedWithGroups) {
            if (group.getName().equalsIgnoreCase(groupName)) {
                toRemove = group;
                break;
            }
        }
        if (toRemove != null) {
            sharedWithGroups.remove(toRemove);
        }
    }
    @Override
    public void removeSelectedUser(String username) {
        this.sharedWithUsers.remove(username);
    }
    
    @Override
    public void addTempSharedUser(String user) {
        if (!tempSharedWithUsers.contains(user)) {
            tempSharedWithUsers.add(user);
        }
    }
    @Override
    public void removeTempSelectedUser(String username) {
        this.tempSharedWithUsers.remove(username);
    }
    @Override
    public void addTempSharedGroup(SharingGroup group) {
        if (!tempSharedWithGroups.contains(group)) {
            tempSharedWithGroups.add(group);
        }
    }
    
    @Override
    public void removeTempSelectedGroup(String groupName) {
        SharingGroup toRemove = null;
        for (SharingGroup group : tempSharedWithGroups) {
            if (group.getName().equalsIgnoreCase(groupName)) {
                toRemove = group;
                break;
            }
        }
        if (toRemove != null) {
            tempSharedWithGroups.remove(toRemove);
        }
    }
    
//    @Override
//    public void setupTempSharing() {
//        this.tempSharedWithGroups.clear();
//        this.tempSharedWithUsers.clear();
//        this.tempSharedWithGroups.addAll(sharedWithGroups);
//        this.tempSharedWithUsers.addAll(sharedWithUsers);
//    }
    
    @Override
    public ArrayList<SharingGroup> getTempSharedWithGroups() {
        return this.tempSharedWithGroups;
    }
    @Override
    public ArrayList<String> getTempSharedWithUsers() {
        return this.tempSharedWithUsers;
    }
    @Override
    public boolean hasSharingUserChanges() {
        boolean hasChanges = false;
        
        if (this.tempSharedWithUsers.containsAll(this.sharedWithUsers)
           && this.sharedWithUsers.containsAll(this.tempSharedWithUsers)) {
            hasChanges = false;
        } else {
            hasChanges = true;
        }
        
        return hasChanges;
    }
    @Override
    public boolean hasSharingGroupChanges() {
        boolean hasChanges = false;
        
        if (this.tempSharedWithGroups.containsAll(this.sharedWithGroups)
           && this.sharedWithGroups.containsAll(this.tempSharedWithGroups)) {
            hasChanges = false;
        } else {
            hasChanges = true;
        }
        
        return hasChanges;
    }
    @Override
    public void updateSharedUsers() {
        sharedWithUsers.clear();
        sharedWithUsers.addAll(tempSharedWithUsers);
    }
    @Override
    public void updateSharedGroups() {
        sharedWithGroups.clear();
        sharedWithGroups.addAll(tempSharedWithGroups);
    }
    @Override
    public void setShapeType(int shapeType) {
        this.shapeType = shapeType;
    }
    @Override
    public int getShapeType() {
        return this.shapeType;
    }
    @Override
    public String getShapeTypeString() {
        if (shapeType == CustomMap.SHAPE_INPUT_OGRAPHIC) {
            return CustomMap.SHAPE_OG;
        } else {
            return CustomMap.SHAPE_OC;
        }
    }
	@Override
	public boolean initialUploadExists() {
		//see if the map name exists in the database
		if (getFilename() != null){
			String fn = getFilename().trim();
			if (fn.length() > 0) {
				mapExists = true;
			} else {
				mapExists = false;
			}
		} else {
			mapExists = false;
		}
		return mapExists;
	}
	@Override
	public boolean isReprocess() {
//		return reprocessFlag;//might only need this for upload
		return false;
	}
	@Override
	public void setReprocess(boolean reprocessVal) {
		//this.reprocessFlag = reprocessVal;
		//might not need this now
	}
	@Override
	public void setReprocessingStarted(boolean started) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setReprocessParentId(String reprocessid) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setReprocessOriginalCanonicName(String reprocessOrigName) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String getReprocessParentId() {
		// TODO Auto-generated method stub
		return this.getCustomMapId();
	}
	@Override
	public String getReprocessOriginalCanonicName() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getReprocessOrigUploadName() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setLayerParameters(LayerParameters layerParams) {
		this.lp = layerParams;
	}
	@Override
	public LayerParameters getLayerParameters() {
		return lp;
	}
	@Override
	public boolean isNumeric() {
		return isNumeric;
	}
    
}
