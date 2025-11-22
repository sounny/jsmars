package edu.asu.jmars.layer.threed;

import java.awt.image.DataBuffer;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HttpRequestType;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;

public class VR_BackendInterface {
	private static String vrServerUrl = null;
    private static DebugLog log = DebugLog.instance();
    /**
     * Static block to setup the URL and information for the custom map server
     */
    static{
        setVRServerUrl();
    }
    private static void setVRServerUrl() {
        vrServerUrl = Config.get("vr_server","https://cm.mars.asu.edu/api/vr/");
    }
    
    public static String createVRScene(VRScene scene) {
    	String returnVal = "";
    	try {
    		JmarsHttpRequest request = new JmarsHttpRequest(vrServerUrl+"insertScene.php", HttpRequestType.POST);
    		JSONObject sceneObj = new JSONObject();
    		JSONArray layerArr = new JSONArray();
    		sceneObj.put("body", scene.getBody());
    		sceneObj.put("scene_name", scene.getSceneName());
    		sceneObj.put("exaggeration", scene.getExaggeration());
    		sceneObj.put("dimension", scene.getDimension());
    		sceneObj.put("dimension_units", scene.getDimensionUnits());
    		sceneObj.put("starting_point", scene.getStartingPoint());
    		sceneObj.put("proj_center_lon", String.valueOf(scene.getProjCenterLon()));
    		sceneObj.put("proj_center_lat", String.valueOf(scene.getProjCenterLat()));
    		sceneObj.put("scene_center_lon", String.valueOf(scene.getCenterOfSceneLon()));
    		sceneObj.put("scene_center_lat", String.valueOf(scene.getCenterOfSceneLat()));
    		sceneObj.put("elevation_data_type", convertDataType(scene.getElevationDepthType()));
    		sceneObj.put("ppd", String.valueOf(scene.getPpd()));
    		sceneObj.put("window_size", scene.getWindowSize());
    		request.addUploadFilePart("depth_file", "depth_image.tif", scene.getDepthImage());
    		request.addUploadFilePart("thumbnail_file", "thumbnail_image.jpg", scene.getThumbnailImage());
    		double date = (new Date()).getTime(); //was getting duplicates, using date as part of canonic name
    		String accessKey = Main.USER + "_" + Main.getCurrentBody() + "_" + date; 
            accessKey = accessKey.replaceAll("[^0-9A-Za-z\\.-]", "_");
            accessKey = accessKey.replace('.', '_');
            accessKey = accessKey.replace('+', '_');
            sceneObj.put("access_key", accessKey);
    		
    		ArrayList<VRScene.Layer> layers = scene.getLayers();
    		int count = 0;
    		for (VRScene.Layer layer : layers) {
    			String layerFilename = "layer_"+count++;
    			JSONObject layerObj = new JSONObject();
    			JSONArray layerDataArr = new JSONArray();
    			JSONArray timeSliderArr = new JSONArray();
    			layerObj.put("layer_name", layer.name);
    			layerObj.put("global_flag", layer.globalFlag.toString());
    			layerObj.put("toggle_state", layer.toggleState.toString());
    			layerObj.put("transfer_id", String.valueOf(layer.transferId));
				String fn = "";
				String fileExt = "";
    			if (layer.graphicImage != null) {
    				fn = layer.graphicImage.getName();
    				if (fn.contains(".")) {
    					fileExt = fn.substring(fn.lastIndexOf("."));
    				} else {
    					System.out.println("no file ext for: "+fn);
    				}
    				request.addUploadFilePart("layer_graphic_image_"+layer.transferId, layerFilename+fileExt, layer.graphicImage);
//    				request.addUploadFilePart("layer_graphic_image_"+layer.transferId, layer.graphicImage.getName(), layer.graphicImage);
    			} else {
    				System.out.println("No Graphic Img: "+layer.name);
    			}
    			
    			if (layer.layerData.size() > 0) {
    				int count2 = 1;
    				for (VRScene.LayerData ld : layer.layerData) {
	    				JSONObject ldObj = new JSONObject();
	    				ldObj.put("numeric_flag", ld.numericFlag.toString());
	    				ldObj.put("text_data", ld.textData);
	    				ldObj.put("source_name", ld.sourceName);
	    				ldObj.put("units", ld.units);
	    				if (ld.dataType != null) {
	    					ldObj.put("data_type", convertDataType(ld.dataType));
	    				}
	    				if (ld.numericImg != null) {
		    				fn = ld.numericImg.getName();
		    				if (fn.contains(".")) {
		    					fileExt = fn.substring(fn.lastIndexOf("."));
		    				} else {
		    					System.out.println("no file ext for numeric img: "+fn);
		    				}
	    					request.addUploadFilePart("layer_data_img_"+layer.transferId+"_"+count2, "data_"+layerFilename+"_"+count2+fileExt, ld.numericImg);
//		    				request.addUploadFilePart("layer_data_img_"+layer.transferId+"_"+count2, ld.numericImg.getName(), ld.numericImg);
		    				count2++;
	    				}
	    				layerDataArr.put(ldObj);
    				}
    				layerObj.put("layer_data", layerDataArr);
    			}
    			if (layer.timeSliders.size() > 0) {
    				int count3 = 1;
    				for (VRScene.TimeSlider ts : layer.timeSliders) {
    					if (ts.image != null) {
	    					JSONObject tsObj = new JSONObject();
	    					tsObj.put("image_index", ts.imageIndex);
		    				fn = ts.image.getName();
		    				fileExt = fn.substring(fn.lastIndexOf("."));
	    					request.addUploadFilePart("time_slider_img_"+layer.transferId+"_"+count3, "ts_data_"+layerFilename+"_"+count3+fileExt, ts.image);
//	    					request.addUploadFilePart("time_slider_img_"+layer.transferId+"_"+count3, ts.image.getName(), ts.image);
	    					timeSliderArr.put(tsObj);
	    					count3++;
    					}
    				}
    				layerObj.put("time_sliders", timeSliderArr);
    			}
    			layerArr.put(layerObj);
    		}
    		sceneObj.put("layers", layerArr);
    		
	    	request.addUTF8String("scene", sceneObj.toString());
	    	request.setRetryNever();
	    	request.setConnectionTimeout(300000);
	    	boolean successful = sendRequest(request);
	    	String errorMsg = null;
            if (!successful) {
                errorMsg = "ERROR: Unexpected HTTP code " + request.getStatus() + " received";
                throw new Exception(errorMsg);
            }
            returnVal = Util.readResponse(request.getResponseAsStream());//String value of scene key
            
    	} catch(JSONException jse) {
    		jse.printStackTrace();
    		returnVal = "Error";
    	} catch (Exception e) {
    		e.printStackTrace();
    		returnVal = "Error";
    	}
    	return returnVal;
    }
    public static String convertDataType(Integer dataType) {
    	String typeName = null;
    	if (dataType == null) {
    		return "*";
    	}
    	switch(dataType){
		case DataBuffer.TYPE_BYTE:   typeName = "byte";   break;
		case DataBuffer.TYPE_SHORT:  typeName = "short";  break;
		case DataBuffer.TYPE_USHORT: typeName = "ushort"; break;
		case DataBuffer.TYPE_INT:    typeName = "int";    break;
		case DataBuffer.TYPE_FLOAT:  typeName = "float";  break;
		case DataBuffer.TYPE_DOUBLE: typeName = "double"; break;
		default:                     typeName = "*"; break;
		}
    	return typeName;
    }
    public static void updateData(VRScene scene) {
    	int sceneKey = scene.getSceneKey();
    	JmarsHttpRequest request = new JmarsHttpRequest(vrServerUrl+"view_scene_jmars.php", HttpRequestType.GET);
    	request.addRequestParameter("scene_key", String.valueOf(sceneKey));
    	boolean success = sendRequest(request);
    	try {
	    	if (success) {
	    		String json = buildString(Util.readLines(request.getResponseAsStream()));
	        	JSONObject sceneObj = new JSONObject(json);
	        	scene.updateSceneData(sceneObj);
	    	}
    	} catch (Exception ex) {
    		log.aprintln(ex.getMessage());
	    } finally {
	    	if (request != null) {
	    		request.close();
	        }
	    }
    }
    
    public static ArrayList<VRScene> viewAllScenes() {
    	ArrayList<VRScene> returnList = new ArrayList<VRScene>();
    	JmarsHttpRequest request = new JmarsHttpRequest(vrServerUrl+"view_jmars_v2_list.php", HttpRequestType.GET);
    	try {
            boolean successful = sendRequest(request);
            if (successful) {
            	String json = buildString(Util.readLines(request.getResponseAsStream()));
            	JSONArray mapArray = new JSONArray(json);
                for (int i=0; i<mapArray.length(); i++) {
                	JSONObject obj = (JSONObject) mapArray.get(i);
                	VRScene scene = new VRScene(obj);
                	returnList.add(scene);
                }
            }
    	} catch (Exception ex) {
    		log.aprintln(ex.getMessage());
	    } finally {
	    	if (request != null) {
	    		request.close();
	        }
	    }
    	return returnList;
    }
    public static void removeScene(int sceneKey) {
    	JmarsHttpRequest request = new JmarsHttpRequest(vrServerUrl+"deleteScene.php", HttpRequestType.POST);
    	request.addRequestParameter("scene_key", String.valueOf(sceneKey));
    	sendRequest(request);
    }
    public static void removeRecord(VREntry entry) {
    	JmarsHttpRequest request = new JmarsHttpRequest(vrServerUrl+"remove.php", HttpRequestType.POST);
    	request.addRequestParameter("key", entry.getKey());
    	sendRequest(request);
    }
    public static void renameRecord(VREntry entry) {
    	JmarsHttpRequest request = new JmarsHttpRequest(vrServerUrl+"rename.php", HttpRequestType.POST);
    	request.addRequestParameter("newName", entry.getName());
    	request.addRequestParameter("key", entry.getKey());
    	sendRequest(request);
    }
    public static String uploadVREntry(JSONObject json, File textureFile, File depthFile, String name) {
        String responseUrl = null;
        double date = (new Date()).getTime(); //was getting duplicates, using date as part of canonic name
        String canonicName = Main.USER + "_" + Main.getCurrentBody() + "_" + String.valueOf((long)(textureFile.getName().hashCode()+(date + Math.random()*100))); 
        canonicName = canonicName.replaceAll("[^0-9A-Za-z\\.-]", "_");
        canonicName = canonicName.replace('.', '_');
        
        String depthCanonicName = "depth_"+canonicName+".png";
        String textureCanonicName = "texture_"+canonicName+".png";
        
        JmarsHttpRequest request = new JmarsHttpRequest(vrServerUrl+"insert.php", HttpRequestType.POST);
        request.addRequestParameter("depthCanonicName", depthCanonicName);
        request.addRequestParameter("textureCanonicName", textureCanonicName);
        request.addRequestParameter("entry_name", name);
        
        request.addUploadFilePart("depth_file", "depthFilename", depthFile);
        request.addUploadFilePart("texture_file", "textureFilename", textureFile);
        
        request.addRequestParameter("json_str",json.toString());
        
        request.setRetryNever();
        
        String errorMsg = null;
        try {
            request.setConnectionTimeout(300000);
            boolean successful = sendRequest(request);
            if (!successful) {
                errorMsg = "ERROR: Unexpected HTTP code " + request.getStatus() + " received";
            }
            
            String[] responseArr = Util.readLines(request.getResponseAsStream());
            
            if (responseArr != null && responseArr.length > 0) {
                responseUrl = responseArr[0];
//                System.out.println(responseUrl);
                if (responseUrl.startsWith("Error")) {
                  //some type of error or warning occurred. 
                    for (String res : responseArr) {
                        log.aprintln(res);
                        System.err.println(res);
                    }
                }
            } else {
                throw new Exception("Null response from upload");
            }
        } catch (Exception ex) {
//            log.aprintln(ex);
            errorMsg = "ERROR: " + ex.getMessage();
            log.aprintln(errorMsg);
        } finally {
            if (request != null) {
                request.close();
            }
        }
        log.aprintln("Done uploading vr file");
       
        return responseUrl;
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
    //all requests should be sent through this method which will send the userid and password along
    private static boolean sendRequest(JmarsHttpRequest request) {
        boolean returnStatus = false;
        try {
            request.addRequestParameter("userid", Main.USER);
            request.addRequestParameter("passwd", Main.PASS);
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
