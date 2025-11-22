package edu.asu.jmars.layer.map2;

import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import sun.nio.ch.FileChannelImpl;


import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.VicarException;

// TODO Make this into an ImageIO reader/writer

/**
 * Very simple and dirty VICAR reader and writer. 
 */
public class MyVicarReaderWriter implements RenderedImage {
	public static final String kwLblSize = "LBLSIZE";
	public static final String kwType = "TYPE";
	public static final String kwOrg = "ORG";
	public static final String kwNL = "NL";
	public static final String kwNS = "NS";
	public static final String kwNB = "NB";
	public static final String kwN1 = "N1";
	public static final String kwN2 = "N2";
	public static final String kwN3 = "N3";
	public static final String kwN4 = "N4";
	public static final String kwIntFmt = "INTFMT";
	public static final String kwRealFmt = "REALFMT";
	public static final String kwFormat = "FORMAT";
	public static final String kwDim = "DIM";
	public static final String kwNBB = "NBB";
	public static final String kwNLB = "NLB";
	public static final String kwRecSize = "RECSIZE";
	public static final String kwBufSize = "BUFSIZ";
	public static final String kwEol = "EOL";
	public static final String kwHost = "HOST";
	public static final String kwBHost = "BHOST";
	public static final String kwBRealFmt = "BREALFMT";
	public static final String kwBIntFmt = "BINTFMT";
	public static final String kwBLType = "BLTYPE";

	public static final String valTypeImage = "IMAGE";
	public static final String valOrgBSQ = "BSQ";
	public static final String valOrgBIL = "BIL";
	public static final String valOrgBIP = "BIP";
	public static final String valFormatByte = "BYTE";
	public static final String valFormatHalf = "HALF";
	public static final String valFormatFull = "FULL";
	public static final String valFormatReal = "REAL";
	public static final String valFormatDoub = "DOUB";
	public static final String valIntFmtLow = "LOW";
	public static final String valIntFmtHigh = "HIGH";
	public static final String valRealFmtRIEEE = "RIEEE";
	public static final String valRealFmtIEEE = "IEEE";
	public static final String valHostSun = "SUN";
	
	public static final int orgBSQ = 0;
	public static final int orgBIP = 1;
	public static final int orgBIL = 2;
	
	
	//
	// Cheap and dirty VICAR image interface to read pixel data randomly
	//
	
	private final File infile;
	private final RandomAccessFile rfile;
	private final HashMap lbl;
	private final int dataType, sampleSize;
	private final int org;
	private final boolean bigEndian;
	private final int n1, n2, n3, nl, ns, nb, nbb, nlb, recSize, lblSize, dataOffset;
	private final ByteBuffer bbuf;
	private Raster cachedRaster;
	private PushbackInputStream is; // FYI: keep reference so that "is" 
	
	public MyVicarReaderWriter(File infile) throws IOException, VicarException {
		this.infile = infile;
		
		// open the file to read label
		is = new PushbackInputStream(new FileInputStream(infile), 100);
		lblSize = readLblSize(is);
		
		byte[] lblBuf = new byte[lblSize];
		if (readFully(is, lblBuf) != lblSize)
			throw new VicarException("Short file. Could not read entire label.");
		
		lbl = readLbl(new PushbackInputStream(new ByteArrayInputStream(lblBuf), 1024));
		
		dataType = getDataType((String)lbl.get(kwFormat));
		bigEndian = isBigEndian(dataType, (String)lbl.get(kwRealFmt), (String)lbl.get(kwIntFmt));
		org = getOrg((String)lbl.get(kwOrg));
		
		n1 = Integer.parseInt((String)lbl.get(kwN1));
		n2 = Integer.parseInt((String)lbl.get(kwN2));
		n3 = Integer.parseInt((String)lbl.get(kwN3));
		
		nl = Integer.parseInt((String)lbl.get(kwNL));
		ns = Integer.parseInt((String)lbl.get(kwNS));
		nb = Integer.parseInt((String)lbl.get(kwNB));
		nbb = Integer.parseInt((String)lbl.get(kwNBB));
		nlb = Integer.parseInt((String)lbl.get(kwNLB));
		
		recSize = Integer.parseInt((String)lbl.get(kwRecSize));
		dataOffset = lblSize + nlb * recSize; // text label + binary label
		
		sampleSize = DataBuffer.getDataTypeSize(dataType)/8;
		bbuf = ByteBuffer.allocate(sampleSize);
		bbuf.order(bigEndian? ByteOrder.BIG_ENDIAN: ByteOrder.LITTLE_ENDIAN);
		
		// finally open the file for random access
		rfile = new RandomAccessFile(infile, "r");
	}
	
	private long getSampleOffset(int x, int y, int z){
		long off = -1;
		
		if (org == orgBSQ){
			off = dataOffset + (((nbb + ns * sampleSize) * nl) * z + (nbb + ns * sampleSize) * y + (nbb + x * sampleSize)); 
		}
		else if (org == orgBIP){
			off = dataOffset + (((nbb + nb * sampleSize) * ns) * y + (nbb + nb * sampleSize) * x + (nbb + z * sampleSize));
		}
		else if (org == orgBIL){
			off = dataOffset + (((nbb + ns * sampleSize) * nb) * y + (nbb + ns * sampleSize) * z + (nbb + x * sampleSize));
		}
		
		return off;
	}
	
	public double[] getPixel(int x, int y, double[] pixel) throws IOException {
		if (pixel == null)
			pixel = new double[nb];
		
		for(int i=0; i<nb; i++){
			// Seek to sample's position
			long sampleOffset = getSampleOffset(x, y, i);
			rfile.seek(sampleOffset);
			
			// Clear sample buffer and read sample worth of data
			bbuf.clear();
			rfile.readFully(bbuf.array());
			
			// Convert read sample into a double
			switch(dataType){
			case DataBuffer.TYPE_BYTE:
				pixel[i] = bbuf.get();
				break;
			case DataBuffer.TYPE_SHORT:
				pixel[i] = bbuf.getShort();
				break;
			case DataBuffer.TYPE_INT:
				pixel[i] = bbuf.getInt();
				break;
			case DataBuffer.TYPE_FLOAT:
				pixel[i] = bbuf.getFloat();
				break;
			case DataBuffer.TYPE_DOUBLE:
				pixel[i] = bbuf.getDouble();
				break;
			}
		}
		
		return pixel;
	}
	
	public int[] getPixel(int x, int y, int[] pixel) throws IOException {
		if (pixel == null)
			pixel = new int[nb];
		
		for(int i=0; i<nb; i++){
			// Seek to sample's position
			long sampleOffset = getSampleOffset(x, y, i);
			rfile.seek(sampleOffset);
			
			// Clear sample buffer and read sample worth of data
			bbuf.clear();
			rfile.readFully(bbuf.array());
			
			// Convert read sample into a double
			switch(dataType){
			case DataBuffer.TYPE_BYTE:
				pixel[i] = bbuf.get();
				break;
			case DataBuffer.TYPE_SHORT:
				pixel[i] = bbuf.getShort();
				break;
			case DataBuffer.TYPE_INT:
				pixel[i] = bbuf.getInt();
				break;
			case DataBuffer.TYPE_FLOAT:
				pixel[i] = (int)bbuf.getFloat();
				break;
			case DataBuffer.TYPE_DOUBLE:
				pixel[i] = (int)bbuf.getDouble();
				break;
			}
		}
		
		return pixel;
	}
	
	
	//
	// Begin - Implementation of RenderedImage interface
	//
	
	public int getWidth(){
		return ns;
	}
	
	public int getHeight(){
		return nl;
	}
	
	public WritableRaster copyData(WritableRaster raster) {
		if (raster == null){
			Raster r = getData();
			WritableRaster wr = r.createCompatibleWritableRaster();
			wr.setRect(r);
			return wr;
		}
		else {
			Raster r = getData(raster.getBounds());
			
			int[] bands = new int[raster.getNumBands()];
			for(int i=0; i<raster.getNumBands(); i++)
				bands[i] = i;
			
			raster.setRect(r.createChild(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight(), r.getMinX(), r.getMinY(), bands));
		}
		return raster;
	}

	public ColorModel getColorModel() {
		return getColorModel(getSampleModel());
	}

	public Raster getData() {
		if (cachedRaster == null){
			try {
				rfile.seek(lblSize);
				cachedRaster = readData(new PushbackInputStream(new FileInputStream(rfile.getFD())), lbl);
			}
			catch(IOException ex){
				throw new RuntimeException(ex);
			}
			catch(VicarException ex){
				throw new RuntimeException(ex);
			}
		}
		
		WritableRaster wr = cachedRaster.createCompatibleWritableRaster();
		wr.setRect(cachedRaster);
		return wr;
	}

	public Raster getData(Rectangle rect) {
		SampleModel sm = getSampleModel().createCompatibleSampleModel(rect.width, rect.height);
		DataBuffer dbuf = sm.createDataBuffer();
		double[] pixel = new double[nb];
		WritableRaster raster = WritableRaster.createWritableRaster(sm, dbuf, rect.getLocation());

		for(int j=rect.y; j<(rect.y+rect.height); j++){
			for(int i=rect.x; i<(rect.x+rect.width); i++){
				try {
					getPixel(i, j, pixel);
				}
				catch(IOException ex){
					throw new RuntimeException("Unable to read pixel at ("+i+","+j+").", ex);
				}
				raster.setPixel(i, j, pixel);
			}
		}

		return raster;
	}

	public int getMinTileX() {
		return 0;
	}

	public int getMinTileY() {
		return 0;
	}

	public int getMinX() {
		return 0;
	}

	public int getMinY() {
		return 0;
	}

	public int getNumXTiles() {
		return 1;
	}

	public int getNumYTiles() {
		return 1;
	}

	public Object getProperty(String name) {
		if (lbl.containsKey(name))
			return lbl.get(name);
		return Image.UndefinedProperty;
	}

	public String[] getPropertyNames() {
		return (String[])lbl.keySet().toArray(new String[0]);
	}

	public SampleModel getSampleModel() {
		try {
			return getSampleModel(org, dataType, n1, n2, n3, nl, ns, nb);
		}
		catch(VicarException ex){
			throw new RuntimeException(ex);
		}
	}

	public Vector<RenderedImage> getSources() {
		return null;
	}

	public Raster getTile(int tileX, int tileY) {
		if (tileX != 0 && tileY != 0)
			return null;
		if (cachedRaster == null)
			getData();
		return cachedRaster;
	}

	public int getTileGridXOffset() {
		return 0;
	}

	public int getTileGridYOffset() {
		return 0;
	}

	public int getTileHeight() {
		return getHeight();
	}

	public int getTileWidth() {
		return getWidth();
	}
	
	//
	// End - Implementation of RenderedImage interface
	//
	
	
	//
	// Cheap and dirty VICAR image reader
	//
	
	public static BufferedImage read(File file) throws IOException, VicarException {
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		BufferedImage image = read(is);
		is.close();
		return image;
	}
	
	public static byte[] getAllBytes(InputStream is) throws IOException {
		InputStream st = is;
		if (!(is instanceof BufferedInputStream))
			st = new BufferedInputStream(is);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] data = new byte[10000];
		int status = 0;
		do {
			out.write(data, 0, status);
			status = st.read(data, 0, data.length);
		} while (status >= 0);
		return out.toByteArray();
	}
	
	public static ColorModel getColorModel(SampleModel sm){
		/*
		 * Byte grayscale images created using the ComponentColorModel get an LUT that makes
		 * them brighter. Thus, the gray values retrieved using the BufferedImage methods are
		 * different from what is stored in the underlying raster. See bugs 4904494 & 5051418. 
		 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4904494
		 */
		int[] bits = new int[sm.getNumBands()];
		Arrays.fill(bits, DataBuffer.getDataTypeSize(sm.getTransferType()));
		boolean hasAlpha = false; // raster.getTransferType() == DataBuffer.TYPE_BYTE && raster.getNumBands() == 2;
		ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
				bits, hasAlpha, false, hasAlpha? ColorModel.TRANSLUCENT: ColorModel.OPAQUE, sm.getTransferType());
		return cm;
	}
	
	public static BufferedImage read(InputStream iis) throws IOException, VicarException {
		// Suck-in all data from the InputStream, we do that because of HttpURLInputStream not 
		// returning all the requested data, but prematurely returning with partial data.
		PushbackInputStream is = new PushbackInputStream(iis, 100);
		int lblSize = readLblSize(is);
		
		// read the entire label
		byte[] lblBytes = new byte[lblSize];
		if (readFully(is, lblBytes) != lblSize)
			throw new VicarException("Short file. Could not read entire label.");

		Map lbl = readLbl(new PushbackInputStream(new ByteArrayInputStream(lblBytes), 100));

		if (!valTypeImage.equals(lbl.get(kwType)))
			throw new VicarException("Only \""+valTypeImage+"\" type VICAR files are supported.");
		
		WritableRaster raster = readData(is, lbl);
		ColorModel cm = getColorModel(raster.getSampleModel());
		BufferedImage outImage = new BufferedImage(cm, raster, cm.isAlphaPremultiplied(), new Hashtable(lbl));
		
		return outImage;
	}
	
	private static int getDataType(String fmt) throws VicarException {
		// Get the raster sample data format
		int dataType = DataBuffer.TYPE_UNDEFINED;
		if (valFormatByte.equals(fmt))
			dataType = DataBuffer.TYPE_BYTE;
		else if (valFormatHalf.equals(fmt))
			dataType = DataBuffer.TYPE_SHORT;
		else if (valFormatFull.equals(fmt))
			dataType = DataBuffer.TYPE_INT;
		else if (valFormatReal.equals(fmt))
			dataType = DataBuffer.TYPE_FLOAT;
		else if (valFormatDoub.equals(fmt))
			dataType = DataBuffer.TYPE_DOUBLE;
		else
			throw new VicarException("Unknown/unhandled format \""+fmt+"\".");
		return dataType;
	}
	
	private static boolean isBigEndian(int dataType, String realFmt, String intFmt) throws VicarException {
		// See if the data is big-endian or little-endian
		boolean bigEndian = false;
		if (dataType == DataBuffer.TYPE_FLOAT || dataType == DataBuffer.TYPE_DOUBLE){
			if (valRealFmtIEEE.equals(realFmt))
				bigEndian = true;
			else if (valRealFmtRIEEE.equals(realFmt))
				bigEndian = false;
			else
				throw new VicarException("Unknown "+kwRealFmt+" value \""+realFmt+"\".");
		}
		else {
			if (valIntFmtHigh.equals(intFmt))
				bigEndian = true;
			else if (valIntFmtLow.equals(intFmt))
				bigEndian = false;
			else
				throw new VicarException("Unknown "+kwIntFmt+" value \""+intFmt+"\".");
		}
		
		return bigEndian;
	}
	
	private static int getOrg(String orgStr) throws VicarException {
		int org;
		
		if (valOrgBSQ.equals(orgStr)){
			org = orgBSQ;
		}
		else if (valOrgBIP.equals(orgStr)){
			org = orgBIP;
		}
		else if (valOrgBIL.equals(orgStr)){
			org = orgBIL;
		}
		else
			throw new VicarException("Unknown "+kwOrg+" value \""+orgStr+"\".");
		
		return org;
	}
	
	private static SampleModel getSampleModel(int org, int dataType, int n1, int n2, int n3, int nl, int ns, int nb) throws VicarException {
		SampleModel sm = null;
		
		if (org == orgBSQ){
			sm = new BandedSampleModel(dataType, n1, n2, n3);
		}
		else if (org == orgBIP){
			int[] bandOffsets = new int[nb];
			for(int i=0; i<nb; i++)
				bandOffsets[i] = i;
			sm = new PixelInterleavedSampleModel(dataType, ns, nl, nb, ns*nb, bandOffsets);
		}
		else if (org == orgBIL){
			int[] bandOffsets = new int[nb];
			for(int i=0; i<nb; i++)
				bandOffsets[i] = i*ns;
			sm = new ComponentSampleModel(dataType, ns, nl, 1, ns*nb, bandOffsets);
		}
		else
			throw new VicarException("Unknown org "+org+".");
		
		return sm;
	}
	
	/**
	 * Reads the VICAR data into a raster. The input stream must be positioned at the start of
	 * the binary header in the VICAR file, i.e. just after the end of text header.
	 * @param is stream to read data from.
	 * @param lbl already read text label.
	 * @return raster containing the data.
	 * @throws IOException propagated upward whenever the input stream generates it.
	 * @throws VicarException generated when either the header information is invalid or file is short.
	 */
	private static WritableRaster readData(PushbackInputStream is, Map lbl) throws IOException, VicarException {
		int nl = Integer.parseInt((String)lbl.get(kwNL));
		int ns = Integer.parseInt((String)lbl.get(kwNS));
		int nb = Integer.parseInt((String)lbl.get(kwNB));
		int nbb = Integer.parseInt((String)lbl.get(kwNBB));
		int nlb = Integer.parseInt((String)lbl.get(kwNLB));
		int recSize = Integer.parseInt((String)lbl.get(kwRecSize));
		String orgStr = (String)lbl.get(kwOrg);
		String fmt = (String)lbl.get(kwFormat);
		String intFmt = (String)lbl.get(kwIntFmt);
		String realFmt = (String)lbl.get(kwRealFmt);

		int n1 = Integer.parseInt((String)lbl.get(kwN1));
		int n2 = Integer.parseInt((String)lbl.get(kwN2));
		int n3 = Integer.parseInt((String)lbl.get(kwN3));
		
		// Get the raster sample data format
		int dataType = getDataType(fmt);
		
		// See if the data is big-endian or little-endian
		boolean bigEndian = isBigEndian(dataType, realFmt, intFmt);
		
		// Skip the binary header - we don't care about it
		if (skipFully(is, nlb * recSize) != (nlb * recSize))
			throw new VicarException("Skip failed skipping "+ (nlb*recSize)+" bytes.");
		
		int org = getOrg(orgStr);
		SampleModel sm = getSampleModel(org, dataType, n1, n2, n3, nl, ns, nb);
		DataBuffer dbuff = sm.createDataBuffer();
		
		ByteBuffer bbuf = ByteBuffer.allocate(n1 * DataBuffer.getDataTypeSize(dataType)/8);
		bbuf.order(bigEndian? ByteOrder.BIG_ENDIAN: ByteOrder.LITTLE_ENDIAN);
		for(int k=0; k<n3; k++){
			for(int j=0; j<n2; j++){
				// Skip the binary prefix on each record
				if (skipFully(is, nbb) != nbb)
					throw new VicarException("Skip failed skipping binary prefix of length "+nbb+" on n2="+j+" n3="+k+".");
				
				bbuf.clear();
				if (readFully(is, bbuf.array()) != bbuf.array().length)
					throw new VicarException("Short file.");
				
				for(int i=0; i<n1; i++){
					int band = -1, index = -1;
					switch(org){
					case orgBSQ:
						band = k;
						index = j*n1+i;
						break;
					case orgBIP:
					case orgBIL:
						band = 0;
						index = k*n1*n2+j*n1+i;
						break;
					}
					
					switch(dataType){
						case DataBuffer.TYPE_BYTE:
							dbuff.setElem(band, index, bbuf.get());
							break;
						case DataBuffer.TYPE_SHORT:
							dbuff.setElem(band, index, bbuf.getShort());
							break;
						case DataBuffer.TYPE_INT:
							dbuff.setElem(band, index, bbuf.getInt());
							break;
						case DataBuffer.TYPE_FLOAT:
							dbuff.setElemFloat(band, index, bbuf.getFloat());
							break;
						case DataBuffer.TYPE_DOUBLE:
							dbuff.setElemDouble(band, index, bbuf.getDouble());
							break;
					}
				}
			}
		}
		
		WritableRaster raster = WritableRaster.createWritableRaster(sm, dbuff, new Point());
		return raster;
	}
	
	private static HashMap readLbl(PushbackInputStream is) throws IOException, VicarException {
		HashMap lbl = new HashMap();
		do{
			String kw = readKw(is);
			skipWhiteSpace(is);
			readChar(is,'=');
			skipWhiteSpace(is);
			Object val = readValue(is);
			
			lbl.put(kw, val);
			
			skipWhiteSpace(is);
		}while(!atEnd(is));
		
		return lbl;
	}
	
	private static boolean atEnd(PushbackInputStream is) throws IOException, VicarException {
		int c = is.read();
		if (c != -1)
			is.unread(c);
		
		return (c <= 0);
	}

	private static String readValueNumber(PushbackInputStream is) throws IOException, VicarException {
		int c = -1;
		String cbuf = "";
		
		cbuf += readInt(is);
		c = is.read();
		if (c == '.'){
			cbuf+= (char)c;
			cbuf += readDigits(is);

			c = is.read();
			if (Character.toUpperCase((char)c) == 'E' || Character.toUpperCase((char)c) == 'D'){
				cbuf += (char)'E';
				cbuf += readInt(is);
			}
			else {
				if (c != -1)
					is.unread(c);
			}
		}
		else if (Character.toUpperCase((char)c) == 'E' || Character.toUpperCase((char)c) == 'D'){
			cbuf += (char)'E';
			cbuf += readInt(is);
		}
		else {
			if (c != -1)
				is.unread(c);
		}
		
		return cbuf;
	}
	
	private static String readInt(PushbackInputStream is) throws IOException, VicarException {
		int c = -1;
		String cbuf = "";
		
		c = is.read();
		if (c == '+' || c == '-')
			cbuf += (char)c;
		else if (Character.isDigit((char)c))
			is.unread(c);
		else
			throw new VicarException("Expecting +/-/digit got \""+Character.toString((char)c)+"\".");
		
		cbuf += readDigits(is);
		return cbuf;
	}
	private static String readDigits(PushbackInputStream is) throws IOException {
		int c = -1;
		String bbuf = "";
		
		while(Character.isDigit((char)(c = is.read())))
			bbuf += (char)c;
		if (c != -1)
			is.unread(c);
		
		return bbuf;
	}
	
	private static String readValueString(PushbackInputStream is) throws IOException, VicarException {
		int c = -1;
		String bbuf = "";
		
		readChar(is, '\'');
		do {
			while((c = is.read()) != '\'')
				bbuf += (char)c;
		} while(c == '\'' && (c = is.read()) == '\'');
		if (c != -1)
			is.unread(c);
		//readChar(is, '\'');
		
		return bbuf;
	}
	
	private static String[] readValueArray(PushbackInputStream is) throws IOException, VicarException {
		List elements = new ArrayList();
		
		readChar(is, '(');
		do {
			skipWhiteSpace(is);
			String val = readElementValue(is);
			elements.add(val);
			skipWhiteSpace(is);
			
			if (hasMoreElements(is))
				readChar(is, ',');
			else
				break;
		} while(true);
		readChar(is, ')');
		
		return (String[])elements.toArray(new String[0]);
	}
	
	private static Object readValue(PushbackInputStream is) throws IOException, VicarException {
		int c = is.read();
		if (c != -1)
			is.unread(c);
		
		if (c == '(')
			return readValueArray(is);
		else if (c == '\'')
			return readValueString(is);
		else if (Character.isDigit((char)c) || c == '+' || c == '-')
			return readValueNumber(is);
		
		throw new VicarException("Expecting value got \""+Character.toString((char)c)+"\".");
	}
	
	private static String readElementValue(PushbackInputStream is) throws IOException, VicarException {
		int c = is.read();
		if (c != -1)
			is.unread(c);
		
		if (c == '\'')
			return readValueString(is);
		else if (Character.isDigit((char)c) || c == '+' || c == '-')
			return readValueNumber(is);
		
		throw new VicarException("Expecting value got \""+Character.toString((char)c)+"\".");
	}
	
	private static boolean hasMoreElements(PushbackInputStream is) throws IOException {
		int c = is.read();
		if (c != -1)
			is.unread(c);
		
		return (c == ',');
	}
	
	private static String readKw(PushbackInputStream is) throws IOException, VicarException {
		int c = -1;
		String bbuf = "";
		
		if (Character.isLetter((char)(c = is.read())))
			bbuf += (char)c;
		else
			throw new VicarException("Expected letter got \""+Character.toString((char)c)+"\".");
		
		while(Character.isLetterOrDigit((char)(c = is.read())) || c == '_')
			bbuf += (char)c;
		
		if (c != -1)
			is.unread(c);
		
		return bbuf;
	}
	
	private static void skipWhiteSpace(PushbackInputStream is) throws IOException {
		int c = -1;
		while(Character.isWhitespace((char)(c = is.read())));
		if (c != -1)
			is.unread(c);
	}
	
	private static String readChar(PushbackInputStream is, char expectedChar) throws IOException, VicarException {
		int c = -1;
		if ((c = is.read()) != expectedChar)
			throw new VicarException("Expected \""+expectedChar+"\" found \""+Character.toString((char)c)+"\".");
		
		return Character.toString((char)c);
	}
	
	private static int readFully(InputStream is, byte[] buf) throws IOException {
		int off = 0, read = 0, len = buf.length;
		while(len > 0 && (read = is.read(buf, off, len)) > 0){
			off += read;
			len -= read;
		}
		
		if (read < 0)
			return read;
		return off;
	}
	
	private static long skipFully(InputStream is, long nbb) throws IOException {
		long skipped = 0, total = 0;
		while(nbb > 0 && (skipped = is.skip(nbb)) > 0){
			nbb -= skipped;
			total += skipped;
		}
		
		if (nbb < 0)
			return nbb;
		return total;
	}
	
	private static int readLblSize(PushbackInputStream is) throws IOException, VicarException {
		int c = -1;
		String bbuf = "";
		
		byte[] tmp = new byte[kwLblSize.length()];
		if (readFully(is,tmp) != tmp.length || !(new String(tmp)).equals(kwLblSize))
			throw new VicarException(kwLblSize+" missing, not a VICAR file.");
		bbuf += new String(tmp);

		// skip ws before '='
		while(Character.isWhitespace((char)(c = is.read())))
			bbuf += (char)c;
		if (c != -1)
			is.unread(c);
		
		if ((c = is.read()) != '=')
			throw new VicarException(kwLblSize+" is not followed by an \"=\", bad VICAR file.");
		bbuf += (char)c;
		
		// skip ws after '='
		while(Character.isWhitespace((char)(c = is.read())))
			bbuf += (char)c;
		if (c != -1)
			is.unread(c);
		
		// read lblsize numeric value
		int start = bbuf.length();
		while(Character.isDigit((char)(c = is.read())))
			bbuf += (char)c;
		if (c != -1)
			is.unread(c);

		int numberLength = bbuf.length() - start; 
		if (numberLength <= 0)
			throw new VicarException(kwLblSize+" value missing, bad VICAR file.");
		
		int lblSize = Integer.parseInt(bbuf.substring(start, start+numberLength));
		
		// push back the characters read so far
		is.unread(bbuf.getBytes());
		
		return lblSize;
	}
	

	//
	// Cheap and dirty VICAR writer
	// 

	public static void write(BufferedImage image, File outputFile) throws IOException, VicarException {
		OutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile));
		write(image, os);
		os.close();
	}

	public static void write(BufferedImage image, OutputStream vos) throws IOException, VicarException {
		DataOutputStream os = new DataOutputStream(vos);
		Raster raster = image.getRaster();
		int nb = raster.getNumBands();
		int nl = raster.getHeight();
		int ns = raster.getWidth();

		int dataType = raster.getSampleModel().getDataType();
		int sampleBits[] = raster.getSampleModel().getSampleSize();
		int maxSampleBits = 0;
		for(int i=0; i<sampleBits.length; i++)
			maxSampleBits = Math.max(maxSampleBits, sampleBits[i]);

		int outType = DataBuffer.TYPE_UNDEFINED;
		switch(dataType){
			case DataBuffer.TYPE_FLOAT:
				outType = DataBuffer.TYPE_FLOAT;
				break;
			case DataBuffer.TYPE_DOUBLE:
				outType = DataBuffer.TYPE_DOUBLE;
				break;
			case DataBuffer.TYPE_BYTE:
				outType = DataBuffer.TYPE_BYTE;
				break;
			case DataBuffer.TYPE_SHORT:
				if (maxSampleBits <= 8)
					outType = DataBuffer.TYPE_BYTE;
				else
					outType = DataBuffer.TYPE_SHORT;
				break;
			case DataBuffer.TYPE_USHORT:
				if (maxSampleBits <= 8)
					outType = DataBuffer.TYPE_BYTE;
				else if (maxSampleBits <= 15)
					outType = DataBuffer.TYPE_SHORT;
				else
					outType = DataBuffer.TYPE_INT;
				break;
			case DataBuffer.TYPE_INT:
				if (maxSampleBits <= 8)
					outType = DataBuffer.TYPE_BYTE;
				else if (maxSampleBits <= 15)
					outType = DataBuffer.TYPE_SHORT;
				else
					outType = DataBuffer.TYPE_INT;
				break;
		}

		if (outType == DataBuffer.TYPE_UNDEFINED)
			throw new IllegalArgumentException("Unhandled input data type "+dataType+".");

		// Write label
		String outLabel = buildLabel(ns, nl, nb, outType);
		os.write(outLabel.getBytes());
		int[] iArray = new int[nb];
		float[] fArray = new float[nb];
		double[] dArray = new double[nb];

		switch(outType){
			case DataBuffer.TYPE_BYTE:
				for(int j=0; j<nl; j++){
					for(int i=0; i<ns; i++){
						raster.getPixel(i, j, iArray);
						for(int k=0; k<iArray.length; k++)
							os.writeByte((byte)iArray[k]);
					}
				}
				break;
			case DataBuffer.TYPE_SHORT:
				for(int j=0; j<nl; j++){
					for(int i=0; i<ns; i++){
						raster.getPixel(i, j, iArray);
						for(int k=0; k<iArray.length; k++)
							os.writeShort((short)iArray[k]);
					}
				}
				break;
			case DataBuffer.TYPE_INT:
				for(int j=0; j<nl; j++){
					for(int i=0; i<ns; i++){
						raster.getPixel(i, j, iArray);
						for(int k=0; k<iArray.length; k++)
							os.writeInt(iArray[k]);
					}
				}
				break;
			case DataBuffer.TYPE_FLOAT:
				for(int j=0; j<nl; j++){
					for(int i=0; i<ns; i++){
						raster.getPixel(i, j, fArray);
						for(int k=0; k<fArray.length; k++)
							os.writeFloat(fArray[k]);
					}
				}
				break;
			case DataBuffer.TYPE_DOUBLE:
				for(int j=0; j<nl; j++){
					for(int i=0; i<ns; i++){
						raster.getPixel(i, j, dArray);
						for(int k=0; k<dArray.length; k++)
							os.writeDouble(dArray[k]);
					}
				}
				break;
		}
	}

	private static String buildLabel(int ns, int nl, int nb, int dataType) throws VicarException {
		String orgString = valOrgBIP;
		int nbb = 0; // Optional prefix before each record
		int nlb = 0; // Number of lines of optional binary header at the top of file
		int n1 = nb, n2 = ns, n3 = nl, n4 = 0;
		int recSize = nbb + n1*DataBuffer.getDataTypeSize(dataType)/8;
		int bufSize = recSize;

		List items = new LinkedList();

		String outTypeStr = null;
		switch(dataType){
			case DataBuffer.TYPE_BYTE:   outTypeStr = valFormatByte; break;
			case DataBuffer.TYPE_SHORT:  outTypeStr = valFormatHalf; break;
			case DataBuffer.TYPE_INT:    outTypeStr = valFormatFull; break;
			case DataBuffer.TYPE_FLOAT:  outTypeStr = valFormatReal; break;
			case DataBuffer.TYPE_DOUBLE: outTypeStr = valFormatDoub; break;
		}
		if (outTypeStr == null)
			throw new VicarException("Invalid input data type "+dataType+".");

		items.add(kwFormat+"='"+outTypeStr+"'");
		items.add(kwType+"='"+valTypeImage+"'");
		items.add(kwBufSize+"="+bufSize);
		items.add(kwDim+"="+3);
		items.add(kwEol+"="+0);
		items.add(kwRecSize+"="+recSize); // TODO Verify RECSIZE
		items.add(kwOrg+"='"+orgString+"'");
		items.add(kwNL+"="+nl);
		items.add(kwNS+"="+ns);
		items.add(kwNB+"="+nb);
		items.add(kwN1+"="+n1);
		items.add(kwN2+"="+n2);
		items.add(kwN3+"="+n3);
		items.add(kwN4+"="+n4);
		items.add(kwNBB+"="+nbb);
		items.add(kwNLB+"="+nlb);
		items.add(kwHost+"='"+valHostSun+"'");
		items.add(kwIntFmt+"='"+valIntFmtHigh+"'");
		items.add(kwRealFmt+"='"+valRealFmtIEEE+"'");
		items.add(kwBHost+"='"+valHostSun+"'");
		items.add(kwBIntFmt+"='"+valIntFmtHigh+"'");
		items.add(kwBRealFmt+"='"+valRealFmtIEEE+"'");
		items.add(kwBLType+"=''");

		String everythingExceptLblSize = Util.join(" ", (String[])items.toArray(new String[0]))+"\0";
		int lblSize = everythingExceptLblSize.length()+kwLblSize.length()+12; // 10 for lblsize + 1 for space + 1 for "="
		if ((lblSize % recSize) > 0)
			lblSize = (int)(Math.ceil((double)lblSize / (double)recSize) * recSize);

		String lbl = kwLblSize+"="+lblSize+" "+everythingExceptLblSize;
		char padChars[] = new char[(lblSize-lbl.length())];
		Arrays.fill(padChars, '\0');
		return lbl + String.valueOf(padChars);
	}

	private static DebugLog log = DebugLog.instance();

	
	public static void main(String[] args){
		log.setActive();
		
		//String[] files = new String[]{ "bsq.v", "bip.v", "bil.v" };
		//String[] files = new String[]{ "bipb.v", "bipi.v", "bipf.v", "bipd.v" };
		//String[] files = new String[]{ "2x3x4bsq.v", "2x3x4bip.v", "2x3x4bil.v" };
		String[] files = new String[]{ "resources/clem750.v" };
		//url = new URL("http://ms.mars.asu.edu/?SERVICE=WMS&REQUEST=GetMap&FORMAT=image/vicar&WIDTH=1920.0&HEIGHT=960.0&SRS=JMARS:1,180.0,90.0&BBOX=30.0,-15.0,90.0,15.0&STYLES=&VERSION=1.1.1&LAYERS=TES_TI_Putzig_numeric");
		for(int fi=0; fi<files.length; fi++){
			try {
				log.print("Reading: "+files[fi]);
				long begin = System.currentTimeMillis();
				InputStream is = new BufferedInputStream(new FileInputStream(files[fi]));
				BufferedImage image = MyVicarReaderWriter.read(is);
				long end1 = System.currentTimeMillis();
				log.print(" "+(end1-begin)+"ms");

				log.print("  writing: b.v");
				write(image, new File("b.v"));
				long end2 = System.currentTimeMillis();
				log.println(" "+(end2-end1)+"ms");
				
				for(int j=0; j<image.getHeight(); j++){
					for(int i=0; i<image.getWidth(); i++){
						log.print(((i>0)?",":"")+image.getRaster().getSample(i, j, 0));
						log.print("("+image.getColorModel().getRed(image.getRGB(i,j))+")");
					}
					log.println();
				}
				log.println();
				MyVicarReaderWriter.read(new BufferedInputStream(new FileInputStream("b.v")));
			}
			catch(Exception ex){
				ex.printStackTrace();
			}
		}
		
		try {
			MyVicarReaderWriter rr = new MyVicarReaderWriter(new File("resources/clem750.v"));

			int n = 30000;
			Point[] pts = new Point[n];
			Random rand = new Random();
			
			double[] pixel = new double[1];
			long[] t = new long[3];
			
			for(int j=0; j<100; j++){
				// Get random pixel coordinates
				for(int i=0; i<pts.length; i++){
					pts[i] = new Point();
					pts[i].x = rand.nextInt(rr.getWidth());
					pts[i].y = rand.nextInt(rr.getHeight());
				}
				
				int ti = 0;

				t[ti++] = System.currentTimeMillis();

				for(int i=0; i<pts.length; i++){
					rr.getPixel(pts[i].x, pts[i].y, pixel);
				}
				t[ti++] = System.currentTimeMillis();
				System.err.println(ti+": Accessing "+pts.length+" pts took "+((t[ti-1]-t[ti-2])/1000.0)+"s @ "+(pts.length*1000.0/(t[ti-1]-t[ti-2]))+" pix/sec");
				
				for(int i=0; i<pts.length; i++){
					rr.getData(new Rectangle(pts[i].x, pts[i].y, 1, 1));
				}
				t[ti++] = System.currentTimeMillis();
				System.err.println(ti+": Accessing "+pts.length+" pts took "+((t[ti-1]-t[ti-2])/1000.0)+"s @ "+(pts.length*1000.0/(t[ti-1]-t[ti-2]))+" pix/sec");

			}
			
		}
		catch(Exception ex){
			ex.printStackTrace();
			System.exit(-1);
		}
	}

}
