package edu.asu.jmars.viz3d.renderer.gl.terrain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.renderer.textures.DecalParameter;

/**
 * Static class to cache 3D geometries for Decals
 */
public class TerrainCache {
	private static final String SEPARATOR = System.getProperty("file.separator");
    private static final String TERRAIN_CACHE = Main.getJMarsPath() + "cache" + SEPARATOR  + "threeDCache" + SEPARATOR;
    
	private static DebugLog log = DebugLog.instance();

    
    static {
    	recreateCacheDirectories();
    }
    
    @SuppressWarnings("unchecked")
	private static ArrayList<DecalParameter> deserializeDecals(String shapeModel) {
    	ArrayList<DecalParameter> list = null;
        String filename = shapeModel+".ser";
        try
        {   
            // Reading the grid from a file
            FileInputStream file = new FileInputStream(filename);
            ObjectInputStream in = new ObjectInputStream(file);
             
            // Method for deserialization of object
            list = (ArrayList<DecalParameter>)in.readObject();
             
            in.close();
            file.close();
        }
         
        catch(IOException ex)
        {
            log.aprintln(ex.getMessage());
        }
         
        catch(ClassNotFoundException ex)
        {
            log.aprintln(ex.getMessage());
        }
 
    	return list;
    }
    
    /**
     * Method to read a cached terrain grid at a specific PPD
     * @param shapeModelName should be the the name of the shape model as defined by the application.
     * @param ppd Pixels Per Degree
     * @return
     */
	public static ArrayList<DecalParameter> readGrid(String shapeModelName, int ppd) {
		return deserializeDecals(TERRAIN_CACHE + stripFilename(shapeModelName+ppd));
	}
	
    private static void serializeDecals(String shapeModel, ArrayList<DecalParameter> list) {
        String filename = shapeModel+".ser";
        
        // Serialization 
        try
        {   
        	File cacheFile = new File(filename);
        	
            // This block is necessary in the event that some of the JMARS cache directories go away during execution.
            // This can happen if the user clicks the 'Manage Cache' option.
            String parentStr = cacheFile.getParent();
            if (parentStr!=null) {
            	File parentDir = new File(parentStr);
            	if (parentDir!=null && !parentDir.exists()) {
            		parentDir.mkdirs();
            	}
            }

            //Saving of object in a file
            FileOutputStream file = new FileOutputStream(filename);
            ObjectOutputStream out = new ObjectOutputStream(file);
             
            // Method for serialization of object
            out.writeObject(list);
             
            out.close();
            file.close();
        }
         
        catch(IOException ex)
        {
        	ex.printStackTrace();
            log.aprintln(ex.getMessage());
        }
    }
    /**
     * Method to write a terrain grid at a specific PPD to cache
     * @param shapeModel should be the the name of the shape model as defined by the application
     * @param ppd Pixels Per Degree
     * @param list non-null ArrayList<DecalParameter>
     */
	public static void writeGrid(String shapeModel, int ppd, ArrayList<DecalParameter> list) {
		serializeDecals(TERRAIN_CACHE + stripFilename(shapeModel+ppd), list);
	}
	
	/**
	 * Method to recreate terrain grid cache directories
	 */
	public static void recreateCacheDirectories() {
    	File cache = new File(TERRAIN_CACHE);
    	if (!cache.exists()) {
    		if (!cache.mkdirs()) {
    			log.aprintln("Unable to create terrain 3d cache directory, check permissions in " + Main.getJMarsPath());
    		}
    	} else if (!cache.isDirectory()) {
    		log.aprintln("Terrain 3D cache cannot be created, found regular file at " + TERRAIN_CACHE);
    	}
	}
	
	/**
	 * Method to clean the terrain grid cache
	 */
	public static void cleanCache() {
		Util.recursiveRemoveDir(new File(TERRAIN_CACHE));
		recreateCacheDirectories();
	}
	
	/*
	 * Remove troublesome characters from the stamp id or URL before using it to read or write to cache files.
	 */
    private static String stripFilename(String filename)
    {
        filename=filename.replace("/", "");
        filename=filename.replace("&","");
        filename=filename.replace("http:", "");
        filename=filename.replace("\\?", "");
        filename=filename.replace("?", "");
        filename=filename.replace("=", "");
        filename=filename.replace(":","");
        filename=filename.replace("-","");
        
        return filename;
    }
}

