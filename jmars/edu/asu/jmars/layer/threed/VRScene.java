package edu.asu.jmars.layer.threed;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.asu.jmars.Main;

/**
 * This class represents one entire scene record for VR. To use this class, create a new scene, then use the helper methods to 
 * create new layers. The helper method to create a Layer will return the Layer object. Use the Layer helper methods to create
 * new layer data records, nomenclature data and time slider entries.
 * 
 * Sample usage: 
 * 	
		VRScene scene = new VRScene("testName", "mars", new File("test"), "exag 1", "1000 x 1000", "m", "100 x 100");
		VRScene.Layer layer = scene.createSceneLayer("Some layer", new File("test2"), false, true);
		layer.createLayerData(true, new File("numImage"), "test");
		layer.createTimeSliderEntry(new File("slider1"), 0);
		layer.createTimeSliderEntry(new File("slider2"), 1);
		VRScene.Layer layer2 = scene.createSceneLayer("Nomenclature", null, false, true);
		//use existing code to create nomenclature data
		JSONArray nomenData = new JSONArray();
		layer2.createNomenclatureData(nomenData);
 * @author kenrios
 *
 */
public class VRScene {

	private Integer sceneKey = null;//will only be used when record retrieved from the database
	private String sceneName = null;
	private String body = null;
	private File depthImage = null;
	private File thumbnailImage = null;
	private Boolean removeFlag = null;
	private String exaggeration = null;
	private String dimension = null;
	private String dimensionUnits = null;
	private String startingPoint = null;
	private String accessKey = null;
	private Integer ppd = null;
	private String windowSize = null; //in pixesl
	private Double projCenterLat = null;
	private Double projCenterLon = null;
	private Double centerOfSceneLat = null;
	private Double centerOfSceneLon = null;
	private ArrayList<Layer> layers = new ArrayList<Layer>();
	private int layerTransferId = 1;
	private boolean dataUpdated = false;
	private Integer elevationDepthType = null;
	
	class Layer {
		int transferId = 0;
		Integer sceneLayerKey = null;
		String name = null;
		File graphicImage = null;
		Boolean globalFlag = null;
		Boolean toggleState = null;
		Integer dataType = null;
		ArrayList<LayerData> layerData = new ArrayList<LayerData>();
		ArrayList<TimeSlider> timeSliders = new ArrayList<TimeSlider>();
		
		public void createTimeSliderEntry(File image, int index) {
			TimeSlider ts = new TimeSlider();
			ts.image = image;
			ts.imageIndex = index;
			this.timeSliders.add(ts);
		}
		/**
		 * 
		 * @param numericFlag: is this a numeric layer. If yes, only populate the numeric image. Text data should be blank.
		 * @param numericImg: numeric image generated for this layer
		 * @param textData: If this is a text layer, non-numeric, populate the text here as JSON data.
		 */
		public void createTextLayerData(String textData) {
			LayerData ld = new LayerData();
			ld.numericFlag = false;
			ld.textData = textData;
			this.layerData.add(ld);
		}
		public LayerData createNumericLayerData(File numericImg, String units, String name) {
			LayerData ld = new LayerData();
			ld.numericImg = numericImg;
			ld.numericFlag = true;
			ld.sourceName = name;
			ld.units = units;
			this.layerData.add(ld);
			return ld;
		}
		public void createNomenclatureData(JSONArray nomenclatureJSONObjects) {
			LayerData ld = new LayerData();
			ld.numericImg = null;
			ld.numericFlag = false;
			ld.nomenclatureData = nomenclatureJSONObjects;
			ld.textData = ld.nomenclatureData.toString();
			this.layerData.add(ld);
		}
	}
	class LayerData {
		Integer sceneLayerDataKey = null;
		String sourceName = null;
		Boolean numericFlag = null;
		File numericImg = null;
		String units = null;
		String textData = null;
		JSONArray nomenclatureData = null;
		Integer dataType = null;
	}
	class TimeSlider {
		File image = null;
		Integer timeSliderDataKey = null;
		Integer imageIndex = null;
	}
	
	public VRScene(JSONObject obj) throws JSONException {
//		id = (obj.isNull("id") ? "" : obj.getString("id"));
		this.sceneKey = obj.getInt("scene_key");
		this.sceneName = obj.getString("scene_name");
		this.accessKey = obj.getString("access_key");
		
	}
	public VRScene(Integer key) {
		this.sceneKey = key;
	}
	public void updateSceneData(JSONObject obj) throws JSONException {
		this.sceneName = obj.getString("scene_name");
		this.exaggeration = obj.getString("exaggeration");
		this.startingPoint = obj.getString("starting_point");
		this.ppd = obj.getInt("ppd");
		JSONArray layerArr = obj.getJSONArray("layers");
		this.layers.clear();
		for (int x=0; x<layerArr.length(); x++) {
			JSONObject layerObj = layerArr.getJSONObject(x);
			Layer layer = new Layer(); 
			layer.name = layerObj.getString("layer_name");
			layer.globalFlag = Boolean.parseBoolean(layerObj.getString("global_flag"));
			layer.toggleState = Boolean.parseBoolean(layerObj.getString("toggle_state"));
			layer.sceneLayerKey = layerObj.getInt("layer_key");
			this.layers.add(layer);
		}
		this.dataUpdated = true;
	}
	public VRScene(String sceneNameStr, String bodyStr, File depthImageFile, String exaggerationStr, String dimensionStr, String dimensionUnitsStr, 
			String startingPointStr, Integer ppdVal, File thumbnail) {
		this.sceneName = sceneNameStr;
		this.body = bodyStr;
		this.depthImage = depthImageFile;
		this.exaggeration = exaggerationStr;
		this.dimension = dimensionStr;
		this.dimensionUnits = dimensionUnitsStr;
		this.startingPoint = startingPointStr;
		this.projCenterLat = Main.PO.getCenterLat();
		this.projCenterLon = Main.PO.getCenterLon();
		Point2D centerPoint = Main.PO.convWorldToSpatial(Main.testDriver.locMgr.getLoc());
		this.centerOfSceneLon = centerPoint.getX();
		this.centerOfSceneLat = centerPoint.getY();
		this.ppd = ppdVal;
		this.windowSize = Main.testDriver.mainWindow.getGlassPanel().getSize().getWidth() + " x "+Main.testDriver.mainWindow.getGlassPanel().getSize().getHeight();
		this.thumbnailImage = thumbnail;
	}
	public Layer createSceneLayer(String layerName, File graphicImage, boolean globalFlag, boolean toggleState) {
		Layer layer = new Layer();
		layer.name = layerName;
		layer.name = layer.name.replace(" ", "_");//remove any spaces in the name before use as the image name
		layer.name = layer.name.replace("+", "_");
		if (graphicImage != null) {
			layer.graphicImage = graphicImage;
		}
		layer.globalFlag = globalFlag;
		layer.toggleState = toggleState;
		layer.transferId = layerTransferId;
		layerTransferId++;
		this.layers.add(layer);
		return layer;
	}
	
	public Integer getSceneKey() {
		return sceneKey;
	}
	public void setSceneKey(Integer sceneKey) {
		this.sceneKey = sceneKey;
	}
	public String getSceneName() {
		return sceneName;
	}
	public void setSceneName(String sceneName) {
		this.sceneName = sceneName;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
	public File getDepthImage() {
		return depthImage;
	}
	public void setDepthImage(File depthImage) {
		this.depthImage = depthImage;
	}
	public File getThumbnailImage() {
		return thumbnailImage;
	}
	public void setThumbnailImage(File thumbnail) {
		this.thumbnailImage = thumbnail;
	}
	public Boolean getRemoveFlag() {
		return removeFlag;
	}
	public void setRemoveFlag(Boolean removeFlag) {
		this.removeFlag = removeFlag;
	}
	public String getExaggeration() {
		return exaggeration;
	}
	public void setExaggeration(String exaggeration) {
		this.exaggeration = exaggeration;
	}
	public String getDimension() {
		return dimension;
	}
	public void setDimension(String dimension) {
		this.dimension = dimension;
	}
	public String getDimensionUnits() {
		return dimensionUnits;
	}
	public void setDimensionUnits(String dimensionUnits) {
		this.dimensionUnits = dimensionUnits;
	}
	public String getStartingPoint() {
		return startingPoint;
	}
	public void setStartingPoint(String startingPoint) {
		this.startingPoint = startingPoint;
	}
	public String getAccessKey() {
		return accessKey;
	}
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}
	public ArrayList<Layer> getLayers() {
		return layers;
	}
	public Integer getPpd() {
		return ppd;
	}
	public String getWindowSize() {
		return windowSize;
	}
	public Double getProjCenterLat() {
		return projCenterLat;
	}
	public Double getProjCenterLon() {
		return projCenterLon;
	}
	public Double getCenterOfSceneLat() {
		return centerOfSceneLat;
	}
	public Double getCenterOfSceneLon() {
		return centerOfSceneLon;
	}
	public boolean getDataUpdated() {
		return dataUpdated;
	}
	public void setElevationDepthType(Integer depthDataType) {
		elevationDepthType = depthDataType;
	}
	public Integer getElevationDepthType() {
		return elevationDepthType;
	}
}
