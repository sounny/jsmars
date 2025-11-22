/**
 * 
 */
package edu.asu.jmars.viz3d.renderer.input.files;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import edu.asu.jmars.util.DebugLog;
import nom.tam.fits.FitsException;
import nom.tam.util.BufferedFile;

/**
 * Combined OBJ+Ancillary FITS reader. Reads FITS, determines the OBJ file
 * from the FITS keywords and then reads the OBJ file. Operations to read
 * data are performed lazily.
 */
@SuppressWarnings("unused")
public class ObjFitsReader2  {
	private static DebugLog log = DebugLog.instance();
	
	static final String OBJ_EXT = ".obj";
	
	private int[] facetNumber = new int[1];
	private float[] data = new float[1];
	private float[] sigma = new float[1];
	private String dataColumnName;
	
	AncFits ancFits;
	Obj obj;
	
	/**
	 * Constructs an ObjFitsReader2 using the specified ancillary file
	 * @param fitsFile
	 * @throws IOException
	 * @throws NumberFormatException
	 * @throws FitsException
	 */
	public ObjFitsReader2(File fitsFile) throws IOException, NumberFormatException, FitsException {
		ancFits = new AncFits(fitsFile);
		
		String objFileName = ancFits.getObjFileName();
		File  objFile = null;
		
		if (objFileName == null){
			objFileName = fitsFile.getName();
			int dotIdx;
			
			while((dotIdx = objFileName.lastIndexOf(".")) >= 0){
				objFileName = objFileName.substring(0, dotIdx);
				String tmpObjFileName = objFileName + OBJ_EXT;
				if (new File(fitsFile.getParentFile(), tmpObjFileName).exists()){
					objFileName = tmpObjFileName;
					break;
				}
			}
			
			if (dotIdx < 0){
				// assume a default if not found
				objFileName = fitsFile.getName()+OBJ_EXT;
			}
		}
		
		objFile = new File(fitsFile.getParentFile(), objFileName);
		
		log.println("Ancillary file "+fitsFile.getPath()+" is linked to obj "+objFile.getPath());
		obj = new Obj(objFile);
	}
	
	/**
	 * 
	 * @return The {@link AncFits} object
	 */
	public AncFits getAncFits(){
		return ancFits;
	}
	
	/**
	 * 
	 * @return The {@link Obj} object
	 */
	public Obj getObj(){
		return obj;
	}
	
	/**
	 * Returns a string representation of the input array separated by commas
	 * @param array
	 * @return comma separated string of all values in array
	 */
	public static String intArrayToStr(int[] array){
		StringBuffer sbuf = new StringBuffer();
		
		for(int i=0; i<array.length; i++){
			if (i > 0){
				sbuf.append(",");
			}
			sbuf.append(array[i]);
		}
		return sbuf.toString();
	}

	/**
	 * Returns a string representation of the input array separated by commas
	 * @param array
	 * @return comma separated string of all values in array
	 */
	public static String floatArrayToStr(float[] array){
		StringBuffer sbuf = new StringBuffer();
		
		for(int i=0; i<array.length; i++){
			if (i > 0){
				sbuf.append(",");
			}
			sbuf.append(String.format("%.2f", Float.valueOf(array[i])));
		}
		return sbuf.toString();
	}

	/**
	 * Test driver
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			File f = new File("/Users/saadat/Downloads/objanc/RQ36mod.oct12_CCv0001.hem3.fit");
//			File f = new File("/Users/saadat/Downloads/refitobjquestions/g_1278cm_tru_slp_0000n00000_v100.fits");
//			File f = new File("/Users/saadat/Downloads/g_1278cm_tru_slp_0000n00000_v100.fits");
			
			ObjFitsReader2 reader = new ObjFitsReader2(f);
			System.out.println("FITS file: "+reader.getAncFits().getFile().getPath());
			System.out.println("FITS row count: "+reader.getAncFits().getRowCount());
			System.out.println("FITS column count: "+reader.getAncFits().getColumnCount());
			System.out.println("FITS columns: "+Arrays.asList(reader.getAncFits().getColumnNames()));
			System.out.println("FITS data column name: "+reader.getAncFits().getDataColumnName());
			System.out.println("FITS facet ids: "+intArrayToStr(reader.getAncFits().getFacetValues()));
			System.out.println("FITS data values: "+floatArrayToStr(reader.getAncFits().getDataValues()));
			System.out.println("FITS data units: "+reader.getAncFits().getDataValueUnits());

			System.out.println("OBJ file: "+reader.getObj().getFile().getPath());
			System.out.println("OBJ IDSK: "+reader.getObj().getIDSK());
			System.out.println("OBJ nvertices: "+reader.getObj().getVertices().length/3);
			System.out.println("OBJ ntriangles: "+reader.getObj().getTriangles().length/3);
			
			System.out.println("done");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
