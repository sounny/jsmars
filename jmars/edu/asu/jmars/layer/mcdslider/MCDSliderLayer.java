package edu.asu.jmars.layer.mcdslider;

import java.awt.image.BufferedImage;
import java.io.ObjectInputStream;
import java.util.HashMap;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.stamp.StampCache;
import edu.asu.jmars.layer.stamp.StampImageFactory;
import edu.asu.jmars.layer.stamp.StampLayer;

/**
 * 
 * @author srcheese
 * @author sdickens
 *
 */
public class MCDSliderLayer extends Layer {

	private BufferedImage numericImg;
	private BufferedImage hsvImg;
	
	private ProjObj currentProj;
	protected MCDLayerSettings settings;
	
	public MCDSliderLayer(String vStr, MCDLayerSettings settings){
		this.settings = settings;
		initialLayerData = settings;
		settings.versionString = vStr;
		
		fetchParams();
	}
	
	@Override
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
		// TODO Auto-generated method stub
	}
	
    public static final String SLIDER_NAMES = "SLIDER_NAMES";
    public static final String DISPLAY_TYPE_MAP = "DISPLAY_TYPE_MAP";

	
    private HashMap<String, String> layerParams = new HashMap<String, String>();
    
    private void initParams() {
    	layerParams.put(SLIDER_NAMES, "Hour");
    	layerParams.put(DISPLAY_TYPE_MAP, "Temperature,temp");
    }
    
    private void fetchParams() {
		String urlStr = "ParameterFetcher?instrument="+getInstrument()+"&format=JAVA";
		HashMap<String, String> newParams;
		
		try {
			ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(urlStr));
			newParams = (HashMap<String, String>) ois.readObject();
			ois.close();
		} catch (Exception e) {
			System.out.println("Unable to retrieve layer parameters");
			newParams = new HashMap<String,String>();
		}
		
		for (String key: newParams.keySet()) {
			layerParams.put(key, newParams.get(key));
		}
    }
	
    public String getParam(String key) {
    	String val = layerParams.get(key);
    	if (val==null) val="";
    	return val;
    }
    
	public String getInstrument() {
		return settings.instrument;
	}
	
	public void setPath(String path){
		settings.mapPath = path;
		//load the new image
		loadImage();
	}
	
	protected double minVal;
	protected double maxVal;
	
	/**
	 * Calculate the urlString that represents the stamp cache path for
	 * the current map.
	 * This is used when checking to see if data has already been preloaded.
	 * @param idStr  The string calculated in the focus panel from the
	 * current type selection and slider selections
	 * @return The filename for the stamp cache file
	 */
	public String getImageUrlString(String idStr){
		String urlStr = "ImageServer?instrument="+getInstrument()+"&"+idStr+"&zoom=100";
		return urlStr;
	}
	
	void loadImage(){
		try {
			currentProj = Main.PO;
			
			{
				String idStr = settings.mapPath;
				
				if (idStr==null) return;
				
				String urlStr = "ImageServer?instrument="+getInstrument()+"&"+idStr+"&zoom=100";
				
				String projStr = Main.PO.getCenterLon()+":"+Main.PO.getCenterLat();
				
				String cacheProjStr = getInstrument() + "_"+settings.mapPath+":"+projStr;
				
				BufferedImage img = StampCache.readProj(cacheProjStr, true);
				
				if (img==null) {
					//Get image from the server or cache if exists
					img = StampImageFactory.getImage(urlStr, true);
					
					GlobalDataReprojecter gdr = new GlobalDataReprojecter(img);
					img = gdr.getProjectedImage(true);
					
					StampCache.writeProj(img, cacheProjStr);
				}
				
				numericImg = img;
			}
			
			{
				String idStr = "id=hsv_map";
			
				String urlStr = "ImageServer?instrument="+getInstrument()+"&"+idStr+"&zoom=100";
				
				String projStr = Main.PO.getCenterLon()+":"+Main.PO.getCenterLat();
				
				String cacheProjStr = getInstrument() + "_" + idStr+":"+projStr;
				
				BufferedImage img = StampCache.readProj(cacheProjStr, true);
				
				if (img==null) {
					//Get image from the server or cache if exists
					img = StampImageFactory.getImage(urlStr, true);
					
					GlobalDataReprojecter gdr = new GlobalDataReprojecter(img);
					img = gdr.getProjectedImage(false);
					
					StampCache.writeProj(img, cacheProjStr);
				}
				hsvImg = img;
			}	
						
			double dArray[] = new double[numericImg.getWidth() * numericImg.getHeight()];
			numericImg.getRaster().getSamples(0, 0, numericImg.getWidth(), numericImg.getHeight(), 0, dArray);
			
			minVal = Double.MAX_VALUE;
			maxVal = Double.MIN_VALUE;
			
			// TODO: Get this from the back end dynamically
			double IGNORE_VALUE = -32768;
			
			for (int cnt=0; cnt<dArray.length; cnt++) {
				if (dArray[cnt]==IGNORE_VALUE) continue;
				if (dArray[cnt]<minVal) minVal=dArray[cnt];
				if (dArray[cnt]>maxVal) maxVal=dArray[cnt];
			}			
			
			viewToUpdate.viewChanged();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public BufferedImage getMapImage(){
		return numericImg;
	}
	
	public synchronized BufferedImage getHSVImage() {
		return hsvImg;
	}
	
	
	public BufferedImage getNumericImage(){
		return numericImg;
	}
	
	/**
	 * @return The string associated with the version of MCD 
	 * with which these products were produced with, and now
	 * related to where they live on disk.
	 */
	public String getVersionString(){
		return settings.versionString;
	}
	
	
	/**
	 * @return The current projection that the map image is in
	 */
	public ProjObj getCurrentProjection(){
		return currentProj;
	}
	
	MCDSliderLView viewToUpdate;
	
	public void setViewToUpdate(MCDSliderLView newView) {
    	viewToUpdate=newView;
    }
}
