package edu.asu.jmars.viz3d.renderer.input.files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.TableHDU;

/**
 * Ancillary FITS file reader. Multiple Ancillary FITS files are tied to
 * a single OBJ file. The FITS file contains value information for facts
 * in the OBJ file. Such information includes latitude, longitude, radius,
 * and sigma values, for example.
 * @see Obj
 */
public class AncFits {
	/** keyword containing map name in primary header */
	public final String KW_MAP_NAME = "MAP_NAME";
	/** keyword containing map version in primary header */
	public final String KW_MAP_VER = "MAP_VER";
	/** keyword containing obj file in primary header */
	public final String KW_OBJ_FILE = "OBJ_FILE";
	/** alternate keyword containing obj file in primary header */
	public final String KW_OBJ_FILE_ALT = "OBJFILE";
	/** extension keyword in primary header */
	public final String KW_XTENSION = "XTENSION";
	
	/** primary keyword containing facet number field in the table header */
	public final String CN_FACET_NUMBER = "Facet_Number";
	/** alternate keyword containing facet number field in the table header */
	public final String CN_FACET_NUMBER_ALT = "FACET NUMBER";
	/** alternate keyword containing facet number field in the table header */
	public final String CN_FACET_NUMBER_ALT_2 = "FACET_NUM";
	/** primary keyword containing latitude field in the table header */
	public final String CN_LAT = "Latitude";
	/** alternate keyword containing latitude field in the table header */
	public final String CN_LAT_ALT = "LATITUDE";
	/** primary keyword containing longitude field in the table header */
	public final String CN_LON = "Longitude";
	/** alternate keyword containing longitude field in the table header */
	public final String CN_LON_ALT = "LONGITUDE";
	/** primary keyword containing radius field in the table header */
	public final String CN_RADIUS = "Radius";
	/** alternate keyword containing radius field in the table header */
	public final String CN_RADIUS_ALT = "RADIUS";
	/** primary keyword containing sigma field in the table header */
	public final String CN_SIGMA = "Sigma";
	/** alternate keyword containing sigma field in the table header */
	public final String CN_SIGMA_ALT = "SIGMA";
	/** alternate keyword containing sigma field in the table header */
	public final String CN_SIGMA_ALT_X = "SIGMAX";
	/** alternate keyword containing sigma field in the table header */
	public final String CN_SIGMA_ALT_Y = "SIGMAY";
	/** alternate keyword containing sigma field in the table header */
	public final String CN_SIGMA_ALT_Z = "SIGMAZ";
	
	
	final Set<String> notDataColunmNames = new HashSet<>(Arrays.asList(new String[]{
			CN_FACET_NUMBER, CN_FACET_NUMBER_ALT, CN_FACET_NUMBER_ALT_2,
			CN_LAT, CN_LAT_ALT, 
			CN_LON, CN_LON_ALT, 
			CN_RADIUS, CN_RADIUS_ALT, 
			CN_SIGMA, CN_SIGMA_ALT, CN_SIGMA_ALT_X, CN_SIGMA_ALT_Y,CN_SIGMA_ALT_Z}));

	private final File ancFitsFile;
	
	private String mapName, objFile;
	private float mapVer;
	private String extType;
	private TableHDU tblHdu;
	
	/**
	 * Construct AncFits object with the given Ancillary FITS file. 
	 * @param ancFitsFile Ancillary FITS file
	 * @throws IOException
	 * @throws FitsException
	 */
	public AncFits(File ancFitsFile) throws IOException, FitsException {
		this.ancFitsFile = ancFitsFile;
		readHeader();
	}
	
	/**
	 * @return File used to create this object
	 */
	public File getFile(){
		return ancFitsFile;
	}

	private void readHeader() throws IOException, FitsException {
		Fits fits = new Fits(ancFitsFile);
		BasicHDU[] hdus = fits.read();
		
		if (hdus.length < 2) { // TODO should this be equal to 2
			throw new RuntimeException("Expecting 2 HDUs in the file, got "+hdus.length);
		}
		
		readTopHeader(fits);
		readTblHeader(fits);
	}
	
	private void readTopHeader(Fits fits) throws IOException, FitsException {
		Header topHdr = fits.getHDU(0).getHeader();
		mapName = topHdr.getStringValue(KW_MAP_NAME);
		mapVer = topHdr.getFloatValue(KW_MAP_VER, 0f);
		objFile = topHdr.getStringValue(KW_OBJ_FILE);
		if (objFile == null){
			objFile = topHdr.getStringValue(KW_OBJ_FILE_ALT);
		}
	}
	
	private void readTblHeader(Fits fits) throws IOException, FitsException {
		BasicHDU hdu = fits.getHDU(1);
		Header tblHdr = hdu.getHeader();
		extType = tblHdr.getStringValue(KW_XTENSION);
		
		if (!(hdu instanceof TableHDU)){
			throw new IllegalArgumentException("Expected table in HDU 1. Found ext type: "+extType);
		}
		
		tblHdu = (TableHDU)hdu;
	}
	
	/**
	 * @return map name from the primary header
	 */
	public String getMapName(){
		return mapName;
	}
	
	/**
	 * @return map version from the primary header
	 */
	public float getMapVersion(){
		return mapVer;
	}
	
	/**
	 * @return OBJ file this ancillary file is associated with
	 */
	public String getObjFileName(){
		return objFile;
	}
	
	/**
	 * @return number of table records
	 */
	public int getRowCount(){
		return tblHdu.getNRows();
	}
	
	/**
	 * @return number of table columns
	 */
	public int getColumnCount(){
		return tblHdu.getNCols();
	}
	
	/**
	 * @return table column names
	 */
	public String[] getColumnNames(){
		String[] columnNames = new String[tblHdu.getNCols()];
		for(int i=0; i<columnNames.length; i++){
			columnNames[i] = tblHdu.getColumnName(i);
		}
		return columnNames;
	}
	
	/**
	 * Find the first name in colName array that appears in the input array
	 * and returns its index.
	 * @param in Array of strings
	 * @param colName Alternate names of the column in preference order
	 * @return Index of the first colName element that appears in the input
	 * array or <code>-1</code> if not found.
	 */
	public static int indexOf(String[] in, String[] colName){
		List<String> inList = Arrays.asList(in);
		for(int i=0; i<colName.length; i++){
			if (inList.contains(colName[i])){
				return inList.indexOf(colName[i]);
			}
		}
		return -1;
	}
	
	/**
	 * @return facet ids
	 * @throws FitsException
	 */
	public int[] getFacetValues() throws FitsException {
		int idx = indexOf(getColumnNames(),new String[]{CN_FACET_NUMBER_ALT_2,CN_FACET_NUMBER,CN_FACET_NUMBER_ALT});		
		return readColumnAsInt(idx);
	}
	
	/**
	 * @return latitude values, one per facet id
	 * @throws FitsException
	 */
	public float[] getLatitudeValues() throws FitsException {
		int idx = indexOf(getColumnNames(),new String[]{CN_LAT,CN_LAT_ALT});
		return readColumnAsFloats(idx);
	}
	
	/**
	 * @return longitude values, one per facet id
	 * @throws FitsException
	 */
	public float[] getLongitudeValues() throws FitsException {
		int idx = indexOf(getColumnNames(),new String[]{CN_LON,CN_LON_ALT});
		return readColumnAsFloats(idx);
	}
	
	/**
	 * @return radius values, one per facet id
	 * @throws FitsException
	 */
	public float[] getRadiusValues() throws FitsException {
		int idx = indexOf(getColumnNames(),new String[]{CN_RADIUS,CN_RADIUS_ALT});
		return readColumnAsFloats(idx);
	}
	
	/**
	 * @return sigma (error) values, one per facet id
	 * @throws FitsException
	 */
	public float[] getSigmaValues() throws FitsException {
		int idx = indexOf(getColumnNames(),new String[]{CN_SIGMA,CN_SIGMA_ALT,CN_SIGMA_ALT_X, CN_SIGMA_ALT_Y,CN_SIGMA_ALT_Z});
		return readColumnAsFloats(idx);
	}
	
	/**
	 * @return sigma (error) values, one per facet id
	 * @throws FitsException
	 */
	public Map<String, float[]> getKeyedSigmaValues() throws FitsException {
		HashMap<String, float[]> data = new HashMap<>();
		
		String[] colNames = new String[]{CN_SIGMA,CN_SIGMA_ALT,CN_SIGMA_ALT_X, CN_SIGMA_ALT_Y,CN_SIGMA_ALT_Z};
		for (String name : colNames) {
			int col = Arrays.asList(getColumnNames()).indexOf(name);
			if (col > -1) {
				data.put(name, readColumnAsFloats(col));
			}
		}
		return data;
	}
	
	/**
	 * @return name of the data column, which is currently the first left over column
	 * after facet, lat, lon, radius, sigma are removed
	 * @throws FitsException
	 */
	public String getDataColumnName() throws FitsException {
		List<String> colNames = new ArrayList<>(Arrays.asList(getColumnNames()));
		colNames.removeAll(notDataColunmNames);
		return colNames.get(0);
	}
	
	/**
	 * @return names of the data columns, which is currently the left over columns
	 * after facet, lat, lon, radius, sigma are removed
	 * @throws FitsException
	 */
	public List<String> getDataColumnNames() throws FitsException {
		List<String> colNames = new ArrayList<>(Arrays.asList(getColumnNames()));
		colNames.removeAll(notDataColunmNames);
		return colNames;
	}
	
	/**
	 * @return names of the Sigma columns
	 * @throws FitsException
	 */
	public List<String> getSigmaColumnNames() throws FitsException {
		ArrayList<String> data = new ArrayList<>();
		
		String[] colNames = new String[]{CN_SIGMA,CN_SIGMA_ALT,CN_SIGMA_ALT_X, CN_SIGMA_ALT_Y,CN_SIGMA_ALT_Z};
		for (String name : colNames) {
			int col = Arrays.asList(getColumnNames()).indexOf(name);
			if (col > -1) {
				data.add(name);
			}
		}
		
		return data;
	}
	
	/**
	 * @return data values per facet id
	 * @throws FitsException
	 */
	public float[] getDataValues() throws FitsException {
		int idx = Arrays.asList(getColumnNames()).indexOf(getDataColumnName());
		return readColumnAsFloats(idx);
	}
	
	/**
	 * @return data values one per facet id
	 * @throws FitsException
	 */
	public Map<String, float[]> getKeyedDataValues() throws FitsException {
		HashMap<String, float[]> data = new HashMap<>();
		List<String> colNames = getDataColumnNames();
		for (String name : colNames) {
			data.put(name, readColumnAsFloats(Arrays.asList(getColumnNames()).indexOf(name)));
		}
		return data;
	}
	
	/**
	 * @return units associated with the data columns, or <code>null</code>
	 * @throws FitsException
	 */
	public String getDataValueUnits() throws FitsException {
		int idx = Arrays.asList(getColumnNames()).indexOf(getDataColumnName());
		return getColumnUnits(idx);
	}
	
	/**
	 * @return units associated with the data columns, or <code>null</code>
	 * @throws FitsException
	 */
	public Map<String, String> getKeyedeDataValueUnits() throws FitsException {
		HashMap<String, String> data = new HashMap<>();
		List<String> colNames = getDataColumnNames();
		for (String name : colNames) {
			data.put(name, getColumnUnits(Arrays.asList(getColumnNames()).indexOf(name)));
		}
		return data;
	}
	
	/**
	 * @param idx Zero-based column index / number
	 * @return units associated with the specified column or <code>null</code>
	 * @throws FitsException
	 */
	public String getColumnUnits(int idx) throws FitsException {
		return tblHdu.getColumnMeta(idx, "TUNIT");
	}
	
	private int[] readColumnAsInt(int col) throws FitsException {
		Object vals = tblHdu.getColumn(col);
		Class<?> componentType = vals.getClass().getComponentType();
		if (int.class.isAssignableFrom(componentType)){
			return (int[])vals;
		} else if (short.class.isAssignableFrom(componentType)){
			return shortArrayToIntArray((short[])vals);
		} else if (float.class.isAssignableFrom(componentType)){
			return floatArrayToIntArray((float[])vals);
		} else {
			throw new IllegalArgumentException("Unhandled component type: "+componentType);
		}
	}
	
	private float[] readColumnAsFloats(int col) throws FitsException {
		Object vals = tblHdu.getColumn(col);
		Class<?> componentType = vals.getClass().getComponentType();
		if (float.class.isAssignableFrom(componentType)){
			return (float[])vals;
		} else if (double.class.isAssignableFrom(componentType)){
			return doubleArrayToFloatArray((double[])vals);
		} else {
			throw new IllegalArgumentException("Unhandled component type: "+componentType);
		}
	}
	
	private static int[] floatArrayToIntArray(float[] floatArray){
		int[] intArray = new int[floatArray.length];
		for(int i=0; i<intArray.length; i++){
			intArray[i] = (int)floatArray[i];
		}
		return intArray;
	}
	
	private static int[] shortArrayToIntArray(short[] shortArray){
		int[] intArray = new int[shortArray.length];
		for(int i=0; i<intArray.length; i++){
			intArray[i] = (int)shortArray[i];
		}
		return intArray;
	}
	
	private static float[] doubleArrayToFloatArray(double[] doubleArray){
		float[] floatArray = new float[doubleArray.length];
		for(int i=0; i<floatArray.length; i++){
			floatArray[i] = (float)doubleArray[i];
		}
		return floatArray;
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

	public static void main(String[] args) throws Exception {
		File f = new File("/Users/saadat/Downloads/g_1278cm_tru_slp_0000n00000_v100.fits");
		
		AncFits reader = new AncFits(f);
		System.out.println("FITS file: "+reader.getFile().getPath());
		System.out.println("FITS row count: "+reader.getRowCount());
		System.out.println("FITS column count: "+reader.getColumnCount());
		System.out.println("FITS columns: "+Arrays.asList(reader.getColumnNames()));
		System.out.println("FITS data column name: "+reader.getDataColumnName());
		System.out.println("FITS facet ids: "+intArrayToStr(reader.getFacetValues()));
		System.out.println("FITS lat values: "+floatArrayToStr(reader.getLatitudeValues()));
		System.out.println("FITS lon values: "+floatArrayToStr(reader.getLongitudeValues()));
		System.out.println("FITS radius values: "+floatArrayToStr(reader.getRadiusValues()));
		System.out.println("FITS data values: "+floatArrayToStr(reader.getDataValues()));
		System.out.println("FITS sigma values: "+floatArrayToStr(reader.getSigmaValues()));
		System.out.println("FITS data units: "+reader.getDataValueUnits());

		System.out.println("done");
	}
}