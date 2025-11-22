package edu.asu.jmars.layer.map2.custom;

import java.io.File;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.util.DebugLog;

public class UploadFile implements CustomMap{
    
    /** Custom map id */
    private String customMapId = null;
	/** Complete file name including path */
	private String filename;
	/** Processing status for this upload */
	private String status;
	/** Processing stage for this upload */
	private String stage;
	/** False unless the upload process has been initiated */
	private boolean startedUpload;
	private boolean isComplete;
	private boolean processingStarted;
	private String basename;
	private String numberOfBands;
	private boolean readyForUpload;
	private boolean wantsBackendSpatialInfo = true;
	private boolean isURL = false;
	private String errorMessage;
	private boolean mapExists;
//	private boolean isGlobalSelected = false;//meant only to be a state holder for the global radio button
//	
//	private int geospatialInput = -1;
	private int extentInput = -1;
	private int degreesInput = -1;
	private String nameInput = null;
	private String ignoreValueInput = null;
//	private String numBandsInput = null;
	private String unitsInput = null;
	private String northLat = null;
    private String southLat = null;
    private String westLon = null;
    private String eastLon = null;
	private String northLatInput = null;
	private String southLatInput = null;
	private String westLonInput = null;
	private String eastLonInput = null;
	private String descriptionInput = null;
	private String[] keywordsInput = null;
	private String citationInput = null;
	private String linksInput = null;
	private String wktString = null;
	private String isNumeric = null;
	private String originalNorthLat = null;
	private String originalSouthLat = null;
	private String originalWestLon = null;
	private String originalEastLon = null;
	private boolean validHeaderFlag = false;
	private boolean capabilitiesRun = false;
	private int selectedUploadProcess = -1;
	private boolean cancelFlag = false;
	private String canonicName = null;
	private String owner = null;
	private String uploadDate = null;
    private int shapeType;
    private String reprocessParentId;
    private boolean reprocessFlag = false;
    private String reprocess_origCanonicName = null;
//    private String reprocessOrigCanName = null;
	private String reprocessOrigName = null;

    private static DebugLog log = DebugLog.instance();
	
	
	
    public UploadFile(String name){
		filename = name;
		basename = filename.substring(filename.lastIndexOf(File.separator)+1);
		nameInput = basename;
		status = CustomMap.STATUS_NOT_STARTED;
		startedUpload = false;
		stage = "0";
		isComplete = false;
		processingStarted = false;
		readyForUpload = false;
	}
    public UploadFile(CustomMap cm) {
    	this.filename = cm.getFilename();
    	this.nameInput = cm.getName();
    	this.customMapId = cm.getCustomMapId();
    	this.basename = cm.getBasename();
    	this.northLat = cm.getNorthLat();
    	this.southLat = cm.getSouthLat();
    	this.eastLon = cm.getEastLon();
    	this.westLon = cm.getWestLon();
    	this.linksInput = cm.getLinks();
    	this.citationInput = cm.getCitation();
    	this.descriptionInput = cm.getDescription();
    	this.wktString = cm.getWktString();
    	this.uploadDate = cm.getUploadDate();
    	this.numberOfBands = cm.getNumberOfBands();
    	this.ignoreValueInput = cm.getIgnoreValue();
    	this.canonicName = cm.getCanonicName();
    	this.degreesInput = cm.getDegrees();
    	this.originalEastLon = cm.getOriginalEastLon();
    	this.originalWestLon = cm.getOriginalWestLon();
    	this.originalNorthLat = cm.getOriginalNorthLat();
    	this.originalSouthLat = cm.getOriginalSouthLat();
    	this.reprocessParentId = cm.getReprocessParentId();
    	this.reprocessOrigName = cm.getName();
    	this.status = CustomMap.STATUS_IN_PROGRESS;
    	this.keywordsInput = cm.getKeywords();
    	this.unitsInput = cm.getUnits();
    	startedUpload = true;
    	isComplete = false;
    	processingStarted = false;
    }
	public UploadFile(JSONObject obj) throws Exception {
	    
	    status = CustomMap.STATUS_IN_PROGRESS;
        startedUpload = true;
        stage = "0";
        isComplete = false;
        processingStarted = false;
        readyForUpload = false;
	    try {
            customMapId = (obj.isNull("id") ? "" : (String) obj.get("id"));
            filename = (obj.isNull("filename") ? "" : (String) obj.get("filename"));
            status = (obj.isNull("status") ? "" : (String) obj.get("status"));
            stage = (obj.isNull("stage") ? "" : (String) obj.get("stage"));
            errorMessage = (obj.isNull("errorMessage") ? "" : (String) obj.get("errorMessage"));
            basename = (obj.isNull("basename") ? "" : (String) obj.get("basename"));
            
            isComplete = false;
            
            //set input values
            nameInput = (obj.isNull("name") ? "" : (String) obj.get("name"));
            String kws = (obj.isNull("keywords") ? "" : (String) obj.get("keywords"));
            keywordsInput = kws.split(kws, 0);
            unitsInput = (obj.isNull("units") ? "" : (String) obj.get("units"));
            northLat = (obj.isNull("maxLat") ? "" : (String) obj.get("maxLat"));
            southLat = (obj.isNull("minLat") ? "" : (String) obj.get("minLat"));
            eastLon = (obj.isNull("minLon") ? "" : (String) obj.get("minLon"));
            westLon = (obj.isNull("maxLon") ? "" : (String) obj.get("minLon"));
            linksInput = (obj.isNull("links") ? "" : (String) obj.get("links"));
            citationInput = (obj.isNull("citation") ? "" : (String) obj.get("citation"));
            descriptionInput = (obj.isNull("abstract") ? "" : (String) obj.get("abstract"));
            wktString = (obj.isNull("wktString") ? "" : (String) obj.get("wktString"));
            uploadDate = (obj.isNull("uploadDate") ? "" : (String) obj.get("uploadDate"));            
            if (CustomMap.STATUS_IN_PROGRESS.equals(status)) {
                if (CustomMap.STAGE_HEADER_ANALYZED.equals(stage)) {
                    readyForUpload = true;
                    //update the lat/lon inputs with the information from the header 
                    JSONObject headerInformation = (obj.isNull("header_info") ? null : (JSONObject) obj.get("header_info"));
                    if (headerInformation != null) {//can be null when getting existing maps tab
                        northLat = (headerInformation.isNull(CustomMap.ORIG_MAX_LAT) ? "0.00" : headerInformation.getString(CustomMap.ORIG_MAX_LAT));
                        southLat = (headerInformation.isNull(CustomMap.ORIG_MIN_LAT) ? "0.00" : headerInformation.getString(CustomMap.ORIG_MIN_LAT));
                        eastLon = (headerInformation.isNull(CustomMap.ORIG_MIN_LON) ? "0.00" : headerInformation.getString(CustomMap.ORIG_MIN_LON));
                        westLon = (headerInformation.isNull(CustomMap.ORIG_MAX_LON) ? "0.00" : headerInformation.getString(CustomMap.ORIG_MAX_LON));
                        numberOfBands = (headerInformation.isNull(CustomMap.ORIG_NUM_BANDS) ? "" : headerInformation.getString(CustomMap.ORIG_NUM_BANDS));
                    }
                }
            }             
        } catch (JSONException e) {
            log.aprintln(e.getStackTrace());
        }
	}
	public void setStage(String newStage) {
	    stage = newStage;
	}
	public void setStatus(String newStatus){
		status = newStatus;
		if (status != null) {
    		if (awaitingUserInput() || STATUS_IN_PROGRESS.equals(status.trim())) {
    		    isComplete = false;
    		} else {
    		    //complete, error
    		    isComplete = true;
    		}
		}
	}

	
	public String getStage() {
	    return stage;
	}
	public void startUpload(){
		startedUpload = true;
	}
	public String getFilename(){
		return filename;
	}
	
	public boolean hasStartedUpload(){
		return startedUpload;
	}
	
	public String getStatus(){
		return status;
	}
	public void setCustomMapId(String id) {
	    customMapId = id;
	}
	public String getCustomMapId() {
	    return customMapId;
	}
	public boolean isComplete() {
	    return isComplete;
	}
	public boolean hasProcessingStarted() {
	    return processingStarted;
	}
	public void setProcessingStarted(boolean started) {
	    processingStarted = started;
	}
    public String getBasename() {
        return basename;
    }
    public void setBasename(String basename) {
        this.basename = basename;
    }
    
    public void setUploadReady(boolean ready){
    	readyForUpload = ready;
    }
    public boolean isReadyForUpload(){
    	return readyForUpload;
    }
    public void setWantsBackendSpatialInfo(boolean wants){
    	wantsBackendSpatialInfo = wants;
    }
    public boolean wantsBackendSpatialInfo(){
    	return wantsBackendSpatialInfo;
    }
    public String getNumberOfBands() {
        return numberOfBands;
    }
    public void setNumberOfBands(String num) {
        numberOfBands = num;
    }
    public void setHeaderInformation(JSONObject obj) throws JSONException {
        if (obj != null) {//can be null when getting existing maps tab
            numberOfBands = obj.getString(CustomMap.NUM_BANDS);
            errorMessage = (obj.isNull(CustomMap.ERROR_MSG) ? "" : obj.getString(CustomMap.ERROR_MSG));
            nameInput = (obj.isNull(CustomMap.NAME) ? "" : obj.getString(CustomMap.NAME));
            isNumeric = (obj.isNull(CustomMap.IS_NUMERIC) ? "0" : obj.getString(CustomMap.IS_NUMERIC));
            wktString = (obj.isNull("wktString") ? "" : (String) obj.get("wktString"));
            originalNorthLat = String.valueOf(obj.getDouble(CustomMap.ORIG_MAX_LAT));
            originalSouthLat = String.valueOf(obj.getDouble(CustomMap.ORIG_MIN_LAT));
            originalEastLon = String.valueOf(obj.getDouble(CustomMap.ORIG_MAX_LON));
            originalWestLon = String.valueOf(obj.getDouble(CustomMap.ORIG_MIN_LON));
            String shapeTypeTemp = (obj.isNull("shape_type") ? null : obj.getString("shape_type"));
            if (shapeTypeTemp != null) {
	            if (CustomMap.SHAPE_OG.equalsIgnoreCase(shapeTypeTemp)) {
	                shapeType = CustomMap.SHAPE_INPUT_OGRAPHIC;
	            } else if (CustomMap.SHAPE_OC.equalsIgnoreCase(shapeTypeTemp)) {
	                shapeType = CustomMap.SHAPE_INPUT_OCENTRIC;
	            }
            } else {
                if (this.selectedUploadProcess != PROCESSING_OPTION_MANUAL && this.shapeType > -1) {//for manual uploads, don't overwrite with a default
                    if (Main.getCurrentBody().equalsIgnoreCase("earth")) {
                        shapeType = CustomMap.SHAPE_INPUT_OGRAPHIC;
                    } else {
                        shapeType = CustomMap.SHAPE_INPUT_OCENTRIC;
                    }
                }
            }
            //possibly need to set lat/lon iputs here also
            
            String validHeaderFlagTemp = (obj.isNull(VALID_HEADER_FLAG) ? "0" : obj.getString(VALID_HEADER_FLAG));
            if ("1".equals(validHeaderFlagTemp)) {
                validHeaderFlag = true;
            } else {
                validHeaderFlag = false;
            }
        }
    }
    public void prepareForOneStepProcessing() {
        if (PROCESSING_OPTION_UPLOAD == this.selectedUploadProcess || PROCESSING_OPTION_PROMPT == this.selectedUploadProcess) {
            this.northLat = originalNorthLat;
            this.southLat = originalSouthLat;
            this.eastLon = originalEastLon;
            this.westLon = originalWestLon;
            
            //possibly need to set name here also
        }
    }
    public boolean awaitingUserInput(){ 
        boolean returnVal = false;
        //are we at stage 5 and waiting for the user to input information?
        if (STATUS_AWAITING_USER_INPUT.equals(status.trim())) {
            returnVal = true;
        } else if (STATUS_IN_PROGRESS.equals(status.trim())
            && STAGE_HEADER_ANALYZED.equals(stage.trim())
            && !processingStarted) {
            returnVal = true;
        }
        return returnVal;
    }
    public boolean isURL() {
        return isURL;
    }
    public void setURL(boolean isURL) {
        this.isURL = isURL;
    }
    public boolean isErrorCondition() {
        if (STATUS_ERROR.equals(this.status)) {
            return true;
        } else {
            return false;
        }
    }
    public String getErrorMessage() {
        return this.errorMessage;
    }
    public void setErrorMessage(String msg) {
        this.errorMessage = msg;
    }
    public String getIsNumeric() {
        return this.isNumeric;
    }
    public boolean isNumeric() {
        if (this.isNumeric != null && this.isNumeric == "1") {
            return true;
        }
        return false;
    }
    public void capabilitiesHaveBeenRun() {
        this.capabilitiesRun = true;
    }
    public boolean checkCapabilitiesRun() {
        return this.capabilitiesRun;
    }
    public boolean isValidHeaderFlag() {
        return validHeaderFlag;
    }
    public void setValidHeaderFlag(boolean validHeaderFlag) {
        this.validHeaderFlag = validHeaderFlag;
    }
    public int getSelectedUploadProcess() {
        return selectedUploadProcess;
    }
    public void setSelectedUploadProcess(int selectedUploadProcess) {
        this.selectedUploadProcess = selectedUploadProcess;
    }
    public String getWktString() {
        return wktString;
    }
    public void setWktString(String wktString) {
        this.wktString = wktString;
    }
    public boolean isCanceled() {
        return cancelFlag;
    }
    public void setCancelFlag(boolean cancelFlag) {
        this.cancelFlag = cancelFlag;
    }
    public void setFilename(String fName) {
        this.filename = fName;
    }
    public String getCanonicName() {
        return canonicName;
    }
    public void setCanonicName(String canonicName) {
        this.canonicName = canonicName;
    }
    @Override
    public void setName(String nameVal) {
        this.nameInput = nameVal;
        
    }
    @Override
    public void addIgnoreValue(String ignoreVal) {
        this.ignoreValueInput = ignoreVal;
    }
    @Override
    public void setUnits(String unitsVal) {
        this.unitsInput = unitsVal;
    }
    @Override
    public void setExtent(int extentVal) {
        this.extentInput = extentVal;
    }
    @Override
    public void setDegrees(int degreesVal) {
        this.degreesInput = degreesVal;
    }
    @Override
    public void setNorthLat(String northLatVal) {
        this.northLat = northLatVal;
    }
    @Override
    public void setSouthLat(String southLatVal) {
        this.southLat = southLatVal;
    }
    @Override
    public void setWestLon(String westLonVal) {
        this.westLon = westLonVal;
    }
    @Override
    public void setEastLon(String eastLonVal) {
        this.eastLon = eastLonVal;
    }
    @Override
    public void setCitation(String citationVal) {
        this.citationInput = citationVal;
    }
    @Override
    public void setLinks(String linksVal) {
        this.linksInput = linksVal;
    }
    @Override
    public void setKeywords(String keywordsVal) {
        this.keywordsInput = keywordsVal.split(",",0);
    }
    @Override
    public void setDescription(String descriptionVal) {
        this.descriptionInput = descriptionVal;
    }
    @Override
    public String getName() {
        return this.nameInput;
    }
    @Override
    public String getIgnoreValue() {
        return this.ignoreValueInput;
    }
    @Override
    public String getUnits() {
        return this.unitsInput;
    }
    @Override
    public int getExtent() {
        return this.extentInput;
    }
    @Override
    public int getDegrees() {
        return this.degreesInput;
    }
    @Override
    public String getNorthLat() {
        return this.northLat;
    }
    @Override
    public String getSouthLat() {
        return this.southLat;
    }
    @Override
    public String getWestLon() {
        return this.westLon;
    }
    @Override
    public String getEastLon() {
        return this.eastLon;
    }
    @Override
    public String getCitation() {
        return this.citationInput;
    }
    @Override
    public String getLinks() {
        return this.linksInput;
    }
    @Override
    public String[] getKeywords() {
        if (this.keywordsInput != null) {
            return this.keywordsInput;
        } else {
            return new String[]{};
        }
    }
    @Override
    public String getDescription() {
        return this.descriptionInput;
    }
    @Override
    public void setOriginalNorthLat(String origNorthLatVal) {
        this.originalNorthLat = origNorthLatVal;
    }
    @Override
    public void setOriginalSouthLat(String origSouthLatVal) {
        this.originalSouthLat = origSouthLatVal;
    }
    @Override
    public void setOriginalWestLon(String origWestLonVal) {
        this.originalWestLon = origWestLonVal;
    }
    @Override
    public void setOriginalEastLon(String origEastLonVal) {
        this.originalEastLon = origEastLonVal;
    }
    @Override
    public String getOriginalNorthLat() {
        return this.originalNorthLat;
    }
    @Override
    public String getOriginalSouthLat() {
        return this.originalSouthLat;
    }
    @Override
    public String getOriginalWestLon() {
        return this.originalWestLon;
    }
    @Override
    public String getOriginalEastLon() {
        return this.originalEastLon;
    }
    @Override
    public char getWarpMethod() {
        return ExistingMap.NEAREST_NEIGHBOR;
    }
    @Override
    public boolean getCornerPointsChanged() {
        boolean returnValue = true;
        if (this.northLatInput == null) {
            returnValue = false;
        } else if (this.northLat.trim().equals(this.northLatInput.trim())
            && this.southLat.trim().equals(this.southLatInput.trim())
            && this.westLon.trim().equals(this.westLonInput.trim())
            && this.eastLon.trim().equals(this.eastLonInput.trim())) {
            returnValue = false;
        }
        if (this.shapeType == CustomMap.SHAPE_INPUT_OGRAPHIC) {
            //ographic requires the backend to do work. Always flag that GCPs changed.
            returnValue = true;
        }
        return returnValue;
    }

    @Override
    public String getUploadDate() {
        return this.uploadDate;
    }
    
    @Override
    public void setUploadDate(String uDate) {
        this.uploadDate = uDate;
    }
    
    @Override
    public String getLastUsedDate() {
        // Not needed for UploadFile
        return null;
    }
    @Override
    public String getOwner() {
        return this.owner;
    }
    
    @Override
    public void setValidHeaderFlag(String validFlag) {
        if (validFlag != null && validFlag.equals("1")) {
            this.validHeaderFlag = true;
        } else {
            this.validHeaderFlag = false;
        }
        
    }
    @Override
    public MapSource getGraphicSource() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public void setLastUsedDate(String format) {
        // TODO Auto-generated method stub
        
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
    public String getMaxPPD() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public boolean isSharedWithOthers() {
        // TODO Auto-generated method stub
        return false;
    }
    /**
     * 
     * This method is used to determine whether the corner points changed from the originals. We don't want to compare
     * to the original values because we format the numbers to remove decimal places. This method will bascially create
     * a copy of the corner points for this file. 
     */
    @Override
    public void setupGCPsForCompare() {
        this.northLatInput = this.northLat;
        this.southLatInput = this.southLat;
        this.eastLonInput = this.eastLon;
        this.westLonInput = this.westLon;
    }
    @Override
    public ArrayList<String> getSharedWithUsers() {
        // nothing to do here
        return null;
    }
    @Override
    public ArrayList<SharingGroup> getSharedWithGroups() {
        // nothing to do here
        return null;
    }
    @Override
    public String getSharedWithUsersString() {
        // nothing to do here
        return null;
    }
    @Override
    public String getSharedWithGroupsString() {
        //nothing to do here
        return null;
    }
    @Override
    public void addSharedUser(String user) {
        //nothing to do here
    }
    @Override
    public void addSharedGroup(SharingGroup group) {
        //nothing to do here
    }
    
    @Override
    public void removeSelectedGroup(String groupName) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void removeSelectedUser(String username) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void addTempSharedUser(String user) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void addTempSharedGroup(SharingGroup group) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void removeTempSelectedGroup(String groupName) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void removeTempSelectedUser(String username) {
        // TODO Auto-generated method stub
        
    }
//    @Override
//    public void setupTempSharing() {
//        // TODO Auto-generated method stub
//        
//    }
    @Override
    public ArrayList<SharingGroup> getTempSharedWithGroups() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public ArrayList<String> getTempSharedWithUsers() {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public boolean hasSharingUserChanges() {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public boolean hasSharingGroupChanges() {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public void updateSharedUsers() {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void updateSharedGroups() {
        // TODO Auto-generated method stub
        
    }
    @Override
    public String getKeywordsString() {
        String text = "";
        if (keywordsInput != null) {
            for(int i=0; i<keywordsInput.length; i++){
                if(i>0){
                    text+= ", ";
                }
                text += keywordsInput[i];
            }
        }
        return text;
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
		System.out.println("mapexists?: " + mapExists);
		return mapExists;
	}
	@Override
	public boolean isReprocess() {
		return reprocessFlag;
	}
	@Override
	public void setReprocess(boolean reprocessVal) {
		this.reprocessFlag = reprocessVal;
	}
	@Override
	public void setReprocessingStarted(boolean started) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setReprocessParentId(String id) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setReprocessOriginalCanonicName(String reprocessOrigCanName) {
		this.reprocess_origCanonicName = reprocessOrigCanName;
		
	}
	@Override
	public String getReprocessParentId() {
		return this.reprocessParentId;
	}
	@Override
	public String getReprocessOriginalCanonicName() {
		return this.reprocess_origCanonicName;
	}
	@Override
	public String getReprocessOrigUploadName() {
		return this.reprocessOrigName ;
	}
	@Override
	public void setLayerParameters(LayerParameters lp) {
		
	}
	@Override
	public LayerParameters getLayerParameters() {
		return null;
	}
		
	
}
