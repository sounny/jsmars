package edu.asu.jmars.util;

import java.io.*;
import java.util.*;
import java.awt.*;
import java.math.BigInteger;
import java.awt.Transparency;
import java.awt.image.*;
import java.awt.color.*;

public class VicarReader {

   static final int TYPE_VALUE[] = { 
	   0, // short
	   1, // int
	   2, // long
	   3, // float
	   4, // double
	   5 // complex
   };
   int type;

   Object pixelValues[][][];

   int sampleNo = -1;
   int lineNo = -1;
   int bandNo = -1;

   boolean first = true;

   Hashtable hash;
   
   public VicarReader(String fileName) throws VicarException {
	   read(fileName);
   }
   
   public VicarReader(DataInputStream inputStream) throws VicarException {
	   read(inputStream);
   }
   
   private void read(String fileName) throws VicarException {
	   try {
		   read(new DataInputStream(new FileInputStream(fileName)));
	   }
	   catch(FileNotFoundException e) {
		   throw new VicarException("File named " + fileName + " does not exist.\n");
	   }
   }

   private void read(DataInputStream reader) throws VicarException {
	   try {
		   VicarHeader mainLabel = new VicarHeader(reader);

		   if(mainLabel.hasValidHeader()) {}

		   hash = mainLabel.getHashTable();

		   // read required labels from mainLabel 
		   int recSize = mainLabel.getRecSize();
		   int N1 = mainLabel.getN1();
		   int N2 = mainLabel.getN2();
		   int N3 = mainLabel.getN3();
		   int ORG = mainLabel.getOrg();

		   // Read RECSIZE records - N2*N3 times	
		   int totalBytes = recSize * N2 * N3;

		   // skip the binary header
		   if(mainLabel.getNLB() != -1)
			   reader.skipBytes(mainLabel.getNLB());

		   // get the number of bytes of binary prefix
		   // bytes to skip at beginning of each record
		   int bytesToSkip = 0;
		   if(mainLabel.getNBB() != -1)
			   bytesToSkip = mainLabel.getNBB();

		   int format = mainLabel.getFormat();
		   int intFormat = mainLabel.getIntFmt();
		   int realFormat = mainLabel.getRealFmt();

		   int bytesToRead = calculateBytesToRead(format);

		   int bytesInFile = 0;
		   int bytesInRec = 0;

		   while(bytesInFile < totalBytes)
		   {
			   byte record[] = new byte[recSize];
			   reader.read(record, 0, recSize);
			   bytesInRec = bytesToSkip;

			   while(bytesInRec < recSize)
			   {
				   byte bytes[] = new byte[bytesToRead];
				   for(int i=0; i<bytes.length; i++)
					   bytes[i] = record[bytesInRec + i];	
				   prepareIndex(ORG, N1, N2, N3);
				   calculateValue(format, intFormat, realFormat, bytes);
				   bytesInRec += bytesToRead;
			   }

			   bytesInRec = 0;
			   bytesInFile += recSize;
		   }

		   reader.close();
	   }
	   catch(VicarException e) {
		   throw e;
	   }
	   catch(Exception ex) {
		   throw new VicarException(ex.getMessage(), ex);
	   }

   }

   private int getWidth() {
	  return (sampleNo + 1);
   }

   private int getHeight() {
	  return (lineNo + 1);
   }

   private int getBands() {
	  return (bandNo + 1);
   }

   public static BufferedImage createBufferedImage(DataInputStream inputStream) throws VicarException {
	   VicarReader vic = new VicarReader(inputStream);
	   return vic.createBufferedImageImpl();
   }
   
   public static BufferedImage createBufferedImage(String fileName) throws VicarException {
		 VicarReader vic = new VicarReader(fileName);
		 return vic.createBufferedImageImpl();
   }
   
   private BufferedImage createBufferedImageImpl() throws VicarException {
	  try {

		 int bands = getBands();
		 if(bands > 1)
			 throw new VicarException("Multi-band images are not supported by the VicarReader.");

		 int width = getWidth();
		 int height = getHeight();
		 int pixelStride = bands;
		 int scanLineStride = width*bands;
		 int offset[] = new int[bands];
		 for(int i=0; i<offset.length; i++) {
			offset[i] = i;
		 }
		 int size = width * height * bands;
		
		 DataBuffer buffer;
		 switch(type) {
		 case 0: 
			buffer = new DataBufferByte(size);
			break;
		 case 1: 
			buffer = new DataBufferShort(size);
			break;
		 case 2: 
			buffer = new DataBufferInt(size);
			break;
		 case 3: 
			buffer = new DataBufferFloat(size);
			break;
		 case 4: 
			buffer = new DataBufferDouble(size);
			break;
		 default:
			throw new VicarException("Complex Data type in the image not supported by DataBuffer");
		 }

		 int dataType = buffer.getDataType();
		 ComponentSampleModel sm = new ComponentSampleModel(dataType, width, height, pixelStride, scanLineStride, offset);

		 VicarRaster raster = new VicarRaster(sm, buffer, new Point(0, 0));

		 switch(type) {
		 case 0: 
			for(int i=0; i<width; i++) {
			   for(int j=0; j<height; j++) {
				  for(int k=0; k<bands; k++) {
					 int value = getPixelByte(i, j, k);
					 raster.setSample(i, j, k, value);
				  }
			   }
			}
			break;
		 case 1: 
			for(int i=0; i<width; i++) {
			   for(int j=0; j<height; j++) {
				  for(int k=0; k<bands; k++) {
					 int value = getPixelShort(i, j, k);
					 raster.setSample(i, j, k, value);
				  }
			   }
			}
			break;
		 case 2: 
			for(int i=0; i<width; i++) {
			   for(int j=0; j<height; j++) {
				  for(int k=0; k<bands; k++) {
					 int value = getPixelInt(i, j, k);
					 raster.setSample(i, j, k, value);
				  }
			   }
			}
			break;
		 case 3: 
			for(int i=0; i<width; i++) {
			   for(int j=0; j<height; j++) {
				  for(int k=0; k<bands; k++) {
					 float value = getPixelFloat(i, j, k);
					 raster.setSample(i, j, k, value);
				  }
			   }
			}
			break;
		 case 4: 
			for(int i=0; i<width; i++) {
			   for(int j=0; j<height; j++) {
				  for(int k=0; k<bands; k++) {
					 double value = getPixelDouble(i, j, k);
					 raster.setSample(i, j, k, value);
				  }
			   }
			}
			break;
		 default:
			throw new VicarException("Complex Data type in the image not supported by DataBuffer");
		 }

		 // Create the appropriate color model
		 ComponentColorModel ccm;
		 switch(type) {
		 case 0:
			ccm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {8}, false, false, Transparency.OPAQUE, dataType);
			break;
		 case 1:
			ccm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {16}, false, false, Transparency.OPAQUE, dataType);
			break;
		 case 2:
			ccm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {32}, false, false, Transparency.OPAQUE, dataType);
			break;
		 case 3:
			ccm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {32}, false, false, Transparency.OPAQUE, dataType);
			break;
		 case 4:
			ccm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), new int[] {64}, false, false, Transparency.OPAQUE, dataType);
			break;
		 default:
			throw new VicarException("Float, Double and Complex data types not supported by ColorModel");	
		 }

		 BufferedImage bufImage = new BufferedImage(ccm, raster, true, hash);

		 return bufImage;
	  }
	  catch(VicarException e) {
		 throw e;
	  }
	  catch(Exception e) {
		 throw new VicarException(e.getMessage(), e);
	  }
   }

   public String getType() {
	  switch(type) {
	  case 0: return "byte";
	  case 1: return "short";
	  case 2: return "int";
	  case 3: return "float";
	  case 4: return "double";
	  case 5: return "complex";
	  }
	  return "";
   }

   private short getPixelByte(int s, int l, int b) throws VicarException {
	  if((type != TYPE_VALUE[0]) && (type != TYPE_VALUE[1]))
		  throw new VicarException("Invalid Type for this image: " + getTypeString());
	  else if((s<0) || (s>sampleNo))
		  throw new VicarException("Invalid sample index");	
	  else if((l<0) || (l>lineNo))
		  throw new VicarException("Invalid line index");	
	  else if((b<0) || (b>bandNo))
		  throw new VicarException("Invalid band index");	
	  else
		  return ((Short) pixelValues[s][l][b]).shortValue();
   }

   private short getPixelShort(int s, int l, int b) throws VicarException {
	  if((type != TYPE_VALUE[0]) && (type != TYPE_VALUE[1]))
		  throw new VicarException("Invalid Type for this image: " + getTypeString());
	  else if((s<0) || (s>sampleNo))
		  throw new VicarException("Invalid sample index");	
	  else if((l<0) || (l>lineNo))
		  throw new VicarException("Invalid line index");	
	  else if((b<0) || (b>bandNo))
		  throw new VicarException("Invalid band index");	
	  else
		  return ((Short) pixelValues[s][l][b]).shortValue();
   }

   private int getPixelInt(int s, int l, int b) throws VicarException {
	  if(type != TYPE_VALUE[2])
		  throw new VicarException("Invalid Type for this image: " + getTypeString());
	  else if((s<0) || (s>sampleNo))
		  throw new VicarException("Invalid sample index");	
	  else if((l<0) || (l>lineNo))
		  throw new VicarException("Invalid line index");	
	  else if((b<0) || (b>bandNo))
		  throw new VicarException("Invalid band index");	
	  else
		  return ((Integer) pixelValues[s][l][b]).intValue();
   }

   private float getPixelFloat(int s, int l, int b) throws VicarException {
	  if(type != TYPE_VALUE[3])
		  throw new VicarException("Invalid Type for this image: " + getTypeString());
	  else if((s<0) || (s>sampleNo))
		  throw new VicarException("Invalid sample index");	
	  else if((l<0) || (l>lineNo))
		  throw new VicarException("Invalid line index");	
	  else if((b<0) || (b>bandNo))
		  throw new VicarException("Invalid band index");	
	  else
		  return ((Float) pixelValues[s][l][b]).floatValue();
   }

   private double getPixelDouble(int s, int l, int b) throws VicarException {
	  if(type != TYPE_VALUE[4])
		  throw new VicarException("Invalid Type for this image: " + getTypeString());
	  else if((s<0) || (s>sampleNo))
		  throw new VicarException("Invalid sample index");	
	  else if((l<0) || (l>lineNo))
		  throw new VicarException("Invalid line index");	
	  else if((b<0) || (b>bandNo))
		  throw new VicarException("Invalid band index");	
	  else
		  return ((Double) pixelValues[s][l][b]).doubleValue();
   }

   private Complex getPixelComplex(int s, int l, int b) throws VicarException {
	  if(type != TYPE_VALUE[5])
		  throw new VicarException("Invalid Type for this image: " + getTypeString());
	  else if((s<0) || (s>sampleNo))
		  throw new VicarException("Invalid sample index");	
	  else if((l<0) || (l>lineNo))
		  throw new VicarException("Invalid line index");	
	  else if((b<0) || (b>bandNo))
		  throw new VicarException("Invalid band index");	
	  else
		  return ((Complex) pixelValues[s][l][b]);
   }

   private static byte getPixelValue(int victype, Object pxlObj) {
	  byte value = 0;
	  switch(victype) {
	  case 0: value = ((Short) pxlObj).byteValue();
		 break;
	  case 1: value = ((Short) pxlObj).byteValue();
		 break;
	  case 2: value = ((Integer) pxlObj).byteValue();
		 break;
	  case 3: value = ((Float) pxlObj).byteValue();
		 break;
	  case 4: value = ((Double) pxlObj).byteValue();
		 break;
	  case 5: value = ((Complex) pxlObj).byteValue();
		 break;
	  }
	  return value;
   }

   private String getTypeString() {
	  switch(type) {
	  case 0: return "1-BYTE UNSIGNED INTEGER";
	  case 1: return "2-BYTE INTEGER";
	  case 2: return "4-BYTE INTEGER";
	  case 3: return "4-BYTE FLOAT";
	  case 4: return "8-BYTE DOUBLE";
	  case 5: return "COMPLEX: 2 4-BYTE FLOAT";
	  }
	  return "";
   }

   private void prepareIndex(int o, int n1, int n2, int n3) throws VicarException {
	  switch(o) {
	  case 0:
		 if((sampleNo+1) < n1) {
			if(first) {
			   pixelValues = new Object[n1+1][n2+1][n3+1];
			   lineNo++;
			   bandNo++;
			   first = false;
			}
			sampleNo++;
		 } else if((lineNo+1) < n2) {
			lineNo++;
			sampleNo = 0;
		 } else if((bandNo+1) < n3) {
			bandNo++;
			lineNo = 0;
			sampleNo = 0;
		 } else {
			throw new VicarException("More values than expected in image: " + sampleNo + " " + lineNo + " " + bandNo);
		 }
		 break;
	  case 1:
		 if((sampleNo+1) < n1) {
			if(first) {
			   pixelValues = new Object[n1+1][n3+1][n2+1];
			   lineNo++;
			   bandNo++;
			   first = false;
			}
			sampleNo++;
		 } else if((bandNo+1) < n2) {
			bandNo++;
			sampleNo = 0;
		 } else if((lineNo+1) < n3) {
			lineNo++;
			sampleNo = 0;
			bandNo = 0;
		 } else {
			throw new VicarException("More values than expected in image: " + sampleNo + " " + lineNo + " " + bandNo);
		 }
		 break;
	  case 2:
		 if((bandNo+1) < n1) {
			if(first) {
			   pixelValues = new Object[n2+1][n3+1][n1+1];
			   lineNo++;
			   sampleNo++;
			   first = false;
			}
			bandNo++;
		 } else if((sampleNo+1) < n2) {
			sampleNo++;
			bandNo=0;
		 } else if((lineNo+1) < n3) {
			lineNo++;
			sampleNo = 0;
			bandNo = 0;
		 } else {
			throw new VicarException("More values than expected in image: " + sampleNo + " " + lineNo + " " + bandNo);
		 }
		 break;
	  }
   }

   private int calculateBytesToRead(int fmt) {
	  switch(fmt) {
	  case 0: type = TYPE_VALUE[0]; return 1;
	  case 1: type = TYPE_VALUE[1]; return 2;
	  case 2: type = TYPE_VALUE[2]; return 4;
	  case 6: type = TYPE_VALUE[1]; return 2;
	  case 7: type = TYPE_VALUE[2]; return 4;
	  case 3: type = TYPE_VALUE[3]; return 4;
	  case 4:	type = TYPE_VALUE[4]; return 8;
	  case 5:	type = TYPE_VALUE[5]; return 8;
	  case 8:	type = TYPE_VALUE[5]; return 8;
	  }
	  return 0;
   }

   private void calculateValue(int fmt, int intRep, int realRep, byte[] b) throws VicarException {
	  try {
		 switch(fmt) {
		 case 0: // 1 byte unsigned integer
			getShort(b);
			break;

		 case 1: // 2 byte signed integer - 2's complement
		 case 6: // 2 byte signed integer
			getInt(intRep, b);
			break;

		 case 2: // 4 byte signed integer
		 case 7: // 4 byte signed integer
			getLong(intRep, b);
			break;

		 case 3: // single precision float
			getFloat(realRep, b);
			break;

		 case 4:	// double precision float
			getDouble(realRep, b);
			break;

		 case 5: // complex
		 case 8: // complex - 2 reals
			getComplex(realRep, b);
			break;
		 }
	  }
	  catch(VicarException e) {
		 throw e;
	  }
   }

   private void getInt(int intRep, byte[] b) {
	  if(intRep == 1) // LOW - little endian 
	   {
		  // convert to big endian
		  byte temp = b[0];
		  b[0] = b[1];
		  b[1] = temp;
	   }
		
	  BigInteger big = new BigInteger(b);

	  pixelValues[sampleNo][lineNo][bandNo] = new Short((short) big.intValue());
   }

   private void getLong(int intRep, byte[] b) {
	  if(intRep == 1) // LOW - little endian 
	   {
		  // convert to big endian
		  byte temp = b[0];
		  b[0] = b[3];
		  b[3] = temp;

		  temp = b[1];
		  b[1] = b[2];
		  b[2] = temp;
	   }
		
	  BigInteger big = new BigInteger(b);

	  pixelValues[sampleNo][lineNo][bandNo] = new Integer((int) big.longValue());
   }

   private void getShort(byte[] b) {
	  byte tempbytes[] = new byte[2];
	  tempbytes[0] = 0x00;
	  tempbytes[1] = b[0];

	  BigInteger big = new BigInteger(tempbytes);

	  pixelValues[sampleNo][lineNo][bandNo] = new Short((short) big.intValue());
   }

   private void getFloat(int realRep, byte[] b) {
	  if(realRep == 1) // RIEEE - reverse IEEE 754
	   {
		  // convert to IEEE 754
		  byte temp = b[0];
		  b[0] = b[3];
		  b[3] = temp;

		  temp = b[1];
		  b[1] = b[2];
		  b[2] = temp;
	   }
	  else if(realRep == 2) // VAX - VAX-F
	   {
		  /*
			Format	Sign	Exp			Fraction
			IEEE	31		30-23		22-0
			VAXF	15		14-7		6-0, 31-16	
			BYTESUSED
			IEEE	0		0, 1		1-3
			VAXF	2		2, 3		3, 0-1
			BYTESSWAPPED	
			IEEE	0	1	2	3
			VAXF 	2 	3 	0 	1
		  */
			
		  // swap bytes 0 and 2
		  byte temp = b[0];
		  b[0] = b[2];
		  b[2] = temp;

		  // swap bytes 1 and 3
		  temp = b[1];
		  b[1] = b[3];
		  b[3] = temp;
	   }

	  BigInteger big = new BigInteger(b);
	  int bits = big.intValue();
	  float value = Float.intBitsToFloat(bits);
		
	  pixelValues[sampleNo][lineNo][bandNo] = new Float(value);
   }

   private void getDouble(int realRep, byte[] b) throws VicarException {
	  if(realRep == 1) // RIEEE - reverse IEEE 754
	   {
		  // convert to IEEE 754
		  byte temp = b[0];
		  b[0] = b[7];
		  b[7] = temp;

		  temp = b[1];
		  b[1] = b[6];
		  b[6] = temp;

		  temp = b[2];
		  b[2] = b[5];
		  b[5] = temp;

		  temp = b[3];
		  b[3] = b[4];
		  b[4] = temp;
	   }
	  else if(realRep == 2) // VAX - VAX-D
	   {
		  // TO DO THIS CONVERSION
		  /*
			Format	Sign	Exp			Fraction
			IEEE	63		62-52		51-0
			VAXD	15		14-7		6-0, 31-16, 47-32, 63-48	

			IEEE	0		0, 1		1, 2, 3, 4, 5, 6, 7
			VAXD	6		6, 7		7, 4-5, 2-3, 0-1

			right shift all bytes 3 bits - excluding the sign
		  */
		  throw new VicarException("VAX D format for floats not supported by this reader");
	   }

	  BigInteger big = new BigInteger(b);
	  long bits = big.longValue();
	  double value = Double.longBitsToDouble(bits);

	  pixelValues[sampleNo][lineNo][bandNo] = new Double(value);
   }

   private void getComplex(int realRep, byte[] b) {
	  if(realRep == 1) // RIEEE - reverse IEEE 754
	   {
		  // convert to IEEE 754
		  // first real part
		  byte temp = b[0];
		  b[0] = b[3];
		  b[3] = temp;

		  temp = b[1];
		  b[1] = b[2];
		  b[2] = temp;

		  // second imaginary part
		  temp = b[4];
		  b[4] = b[7];
		  b[7] = temp;

		  temp = b[5];
		  b[5] = b[6];
		  b[6] = temp;
	   }
	  else if(realRep == 2) // VAX - VAX-F
	   {
		  // TO DO THIS CONVERSION
	   }

	  byte tempbytes[] = new byte[4];
	  tempbytes[0] = b[0];
	  tempbytes[1] = b[1];
	  tempbytes[2] = b[2];
	  tempbytes[3] = b[3];

	  BigInteger big = new BigInteger(tempbytes);
	  int bits = big.intValue();
	  float rl = Float.intBitsToFloat(bits);

	  tempbytes[0] = b[4];
	  tempbytes[1] = b[5];
	  tempbytes[2] = b[6];
	  tempbytes[3] = b[7];

	  big = new BigInteger(tempbytes);
	  bits = big.intValue();
	  float im = Float.intBitsToFloat(bits);
		
	  pixelValues[sampleNo][lineNo][bandNo] = new Complex(rl, im);
   }

   private static class Complex {
	  Float real;
	  Float imaginary;

	  public Complex(float rl, float im) {
		 real = new Float(rl);
		 imaginary = new Float(im);
	  }
	
	  public byte byteValue() {
		 return real.byteValue();
	  }
	
	  public String toString() {
		 return (real.toString() + ", " + imaginary.toString());
	  }
	} // Complex

   private static class VicarHeader {

	  private static final String LBL_NAME[] = {"LBLSIZE", "FORMAT", "TYPE", "BUFSIZ",
												"DIM", "EOL", "RECSIZE", "ORG", "NL", "NS", "NB",
												"N1", "N2", "N3", "N4", "NBB", "NLB", "HOST", "INTFMT",
												"REALFMT", "BHOST", "BINFMT", "BREALFMT", "BLTYPE",
												"TASK", "PROPERTY", "BINTFMT"}; // Processing both BINTFMT and BINFMT

	  private static final String FMT_VALUE[] = {"BYTE", "HALF", "FULL",
												 "REAL", "DOUB", "COMP",
												 "WORD", "LONG", "COMPLEX"};

	  private static final String TYPE_VALUE[] = {"IMAGE", "PARMS", "PARM",
												  "PARAM", "GRAPH1", "GRAPH2",
												  "GRAPH3", "TABULAR"};

	  private static final int DIM_VALUE[] = {3, 2};
	  private static final int EOL_VALUE[] = {0, 1};
	  private static final String ORG_VALUE[] = {"BSQ", "BIL", "BIP"};
	  private static final int N4_VAL = 0;
	  private static final String INTFMT_VALUE[] = {"HIGH", "LOW"};
	  private static final String REALFMT_VALUE[] = {"IEEE", "RIEEE", "VAX"};

	  private int lblSize = -1;	// mand - label storage area in bytes
	  private int format = -1;	// mand - data type of pixels
	  private int type;			// type of file
	  private int bufSize = -1;	// mand - obsolete - buffer size to use to read file
	  private int dim;			// number of dimensions
	  private int eol;			// if labels at the end of file or not
	  private int recSize = -1;	// mand - record size in bytes - nbb+n1*pixelsize
	  private int org = -1;		// org of file
	  private int nl = -1;		// mand - num of lines
	  private int ns = -1;		// mand - num of samples
	  private int nb = -1;		// mand - num of bands
	  private int n1 = -1;		// size in pixels of fastest-varying dimension
	  private int n2 = -1;		// size in pixels of second dimension
	  private int n3 = -1;		// size in pixels of third dimension
	  private int n4 = -1;		// size in pixels of fourth dimension
	  private int nbb = -1;		// num of bytes of binary prefix before image record
	  private int nlb = -1;		// num of lines of binary header at file top
	  private String host;		// type of computer used to generate file
	  private int intFmt;			// int storage format
	  private int realFmt;		// real storage format
	  private String bHost;		// type of computer used to generate binary header
	  private int bIntFmt;		// binary int format
	  private int bRealFmt;		// binary real format
	  private String bLType;		// binary label type
	
	  private Hashtable hash;		// Hash Table of all the labels

	  public VicarHeader(DataInputStream reader) throws VicarException {
		 try {
			byte record[] = new byte[100];
			reader.read(record, 0, record.length);
			String hdrString = new String(record);

			int size = extractLabelSize(hdrString);
			if(size <= 0) {
			   throw new VicarException("Unable to extract label size. Cannot process file");
			}
			
			byte record1[] = new byte[size - record.length];
			reader.read(record1, 0, record1.length);
			hdrString = hdrString + (new String(record1));
			
			hash = new Hashtable();	

			setDefaults();
			processHeader(hdrString);
		 }
		 catch(VicarException ex) {
			throw ex;
		 }		
		 catch(IOException ex) {
			throw new VicarException(ex.getMessage(), ex);
		 }
	  }

	  public boolean hasValidHeader() throws VicarException {
		 /** Check if all mandatory fields are specified 
			 LBLSIZE, FORMAT, BUFSIZ, RECSIZE, NL, NS, NB*/

		 if(lblSize == -1)
			 throw new VicarException("Invalid Header: " + "Label Size not in the file");
		 else if(format == -1)
			 throw new VicarException("Invalid Header: " + "Format not in the file");
		 else if(bufSize == -1)
			 throw new VicarException("Invalid Header: " + "Buffer Size not in the file");
		 else if(recSize == -1)
			 throw new VicarException("Invalid Header: " + "Record Size not in the file");
		 else if(nl == -1)
			 throw new VicarException("Invalid Header: " + "Number of Lines not in the file");
		 else if(ns == -1)
			 throw new VicarException("Invalid Header: " + "Number of Samples not in the file");
		 else if(nb == -1)
			 throw new VicarException("Invalid Header: " + "Number of Bands not in the file");
		 else if(org == -1)
			 throw new VicarException("Invalid Header: " + "Org not in the file");
		 else
			 return true;
	  }

	  private void processHeader(String hdrString) throws VicarException {

		 String lblType, strToConsider;

		 StringTokenizer strTok = new StringTokenizer(hdrString, "= ");
		 try {
			lblType = strTok.nextToken().trim();
			strToConsider = strTok.nextToken().trim();
		 }
		 catch(NoSuchElementException e) {
			throw new VicarException("Unable to extract label size. Cannot process file", e);
		 }

		 while(true) {
			int lblIndex = -1;
			for(int i=0; i<LBL_NAME.length; i++) {
			   if(lblType.equals(LBL_NAME[i])) {
				  lblIndex = i;
				  break;
			   }
			}

			strToConsider = stripQuotes(strToConsider);
			
			switch(lblIndex) {
			case -1:
			   throw new VicarException("Invalid keyword in label" + lblIndex + " with value: " + strToConsider);
			case 0: // lblSize
			   try {
				  Integer temp = new Integer(strToConsider);
				  lblSize = temp.intValue();
				  hash.put(lblType, temp);
			   }
			   catch(NumberFormatException ex) {
				  throw new VicarException("Invalid value for lblSize" + strToConsider, ex);
			   }
			   break;
			case 1: // format
			   format = isValidFormat(strToConsider);
			   if(format == -1) {
				  throw new VicarException("Invalid value for format" + strToConsider);
			   }
			   hash.put(lblType, FMT_VALUE[format]);
			   break;
			case 2: // type
			   type = isValidType(strToConsider);
			   if(type == -1) {
				  throw new VicarException("Invalid value for type" + strToConsider);
			   }
			   hash.put(lblType, TYPE_VALUE[type]);
			   break;
			case 3: // bufsize
			   try {
				  Integer temp = new Integer(strToConsider);
				  bufSize = temp.intValue();
				  hash.put(lblType, temp);
			   }
			   catch(NumberFormatException ex) {
				  throw new VicarException("Invalid value for buffer size" + strToConsider, ex);
			   }
			   break;
			case 4: // dim
			   try {
				  Integer dimension = new Integer(strToConsider);
				  if(!isValidDim(dimension.intValue()))
					  throw new VicarException("Invalid value for dimension" + dimension.intValue());
				  dim = dimension.intValue();
				  hash.put(lblType, dimension);
			   }
			   catch(NumberFormatException ex) {
				  throw new VicarException("Invalid value for dimension" + strToConsider, ex);
			   }
			   break;
			case 5: // eol
			   try {
				  Integer eolabel = new Integer(strToConsider);
				  if(!isValidEol(eolabel.intValue()))
					  throw new VicarException("Invalid value for eol" + eolabel.intValue());
				  eol = eolabel.intValue();
				  hash.put(lblType, eolabel);
			   }
			   catch(NumberFormatException ex) {
				  throw new VicarException("Invalid value for eol" + strToConsider, ex);
			   }
			   break;
			case 6: // recsize
			   try {
				  Integer temp = new Integer(strToConsider);
				  recSize = temp.intValue();
				  hash.put(lblType, temp);
			   }
			   catch(NumberFormatException ex) {
				  throw new VicarException("Invalid value for record size" + strToConsider, ex);
			   }
			   break;
			case 7: // org
			   org = isValidOrg(strToConsider);
			   if(org == -1) {
				  throw new VicarException("Invalid value for org" + strToConsider);
			   }
			   hash.put(lblType, ORG_VALUE[org]);
			   break;
			case 8: // nl
			   try {
				  Integer temp = new Integer(strToConsider);
				  nl = temp.intValue();
				  hash.put(lblType, temp);
			   }
			   catch(NumberFormatException ex) {
				  throw new VicarException("Invalid value for nl" + strToConsider, ex);
			   }
			   break;
			case 9: // ns
			   try {
				  Integer temp = new Integer(strToConsider);
				  ns = temp.intValue();
				  hash.put(lblType, temp);
			   }
			   catch(NumberFormatException ex) {
				  throw new VicarException("Invalid value for ns" + strToConsider, ex);
			   }
			   break;
			case 10: // nb
			   try {
				  Integer temp = new Integer(strToConsider);
				  nb = temp.intValue();
				  hash.put(lblType, temp);
			   }
			   catch(NumberFormatException ex) {
				  throw new VicarException("Invalid value for nb" + strToConsider, ex);
			   }
			   break;
			case 11: // n1
			   try {
				  Integer temp = new Integer(strToConsider);
				  n1 = temp.intValue();
				  hash.put(lblType, temp);
			   }
			   catch(NumberFormatException ex) {
				  throw new VicarException("Invalid value for n1" + strToConsider, ex);
			   }
			   break;
			case 12: // n2
			   try {
				  Integer temp = new Integer(strToConsider);
				  n2 = temp.intValue();
				  hash.put(lblType, temp);
			   }
			   catch(NumberFormatException ex) {
				  throw new VicarException("Invalid value for n2" + strToConsider, ex);
			   }
			   break;
			case 13: // n3
			   try {
				  Integer temp = new Integer(strToConsider);
				  n3 = temp.intValue();
				  hash.put(lblType, temp);
			   }
			   catch(NumberFormatException ex) {
				  throw new VicarException("Invalid value for n3" + strToConsider, ex);
			   }
			   break;
			case 14: // n4
			   try {
				  Integer temp = new Integer(strToConsider);
				  n4 = temp.intValue();
				  hash.put(lblType, temp);
			   }
			   catch(NumberFormatException ex) {
				  throw new VicarException("Invalid value for n4" + strToConsider, ex);
			   }
			   break;
			case 15: // nbb
			   try {
				  Integer temp = new Integer(strToConsider);
				  nbb = temp.intValue();
				  hash.put(lblType, temp);
			   }
			   catch(NumberFormatException ex) {
				  throw new VicarException("Invalid value for nbb" + strToConsider, ex);
			   }
			   break;
			case 16: // nlb
			   try {
				  Integer temp = new Integer(strToConsider);
				  nlb = temp.intValue();
				  hash.put(lblType, temp);
			   }
			   catch(NumberFormatException ex) {
				  throw new VicarException("Invalid value for nlb" + strToConsider, ex);
			   }
			   break;
			case 17: // host
			   host = strToConsider;
			   hash.put(lblType, host);
			   break;
			case 18: // intfmt
			   intFmt = isValidIntFmt(strToConsider);
			   if(intFmt == -1) {
				  throw new VicarException("Invalid value for intFmt " + strToConsider);
			   }
			   hash.put(lblType, INTFMT_VALUE[intFmt]);
			   break;
			case 19: // realfmt
			   realFmt = isValidRealFmt(strToConsider);
			   if(realFmt == -1) {
				  throw new VicarException("Invalid value for realFmt" + strToConsider);
			   }
			   hash.put(lblType, REALFMT_VALUE[realFmt]);
			   break;
			case 20: // bhost
			   bHost = strToConsider;
			   hash.put(lblType, bHost);
			   break;
			case 21: // binfmt
			   bIntFmt = isValidIntFmt(strToConsider);
			   if(bIntFmt == -1) {
				  throw new VicarException("Invalid value for bInFmt" + strToConsider);
			   }
			   hash.put(lblType, INTFMT_VALUE[bIntFmt]);
			   break;
			case 22: // brealfmt
			   bRealFmt = isValidRealFmt(strToConsider);
			   if(bRealFmt == -1) {
				  throw new VicarException("Invalid value for bRealFmt" + strToConsider);
			   }
			   hash.put(lblType, REALFMT_VALUE[bRealFmt]);
			   break;
			case 23: // bltype
			   bLType = strToConsider;
			   hash.put(lblType, bLType);
			   break;
			case 24: // task
			   return;
			case 25: // property
			   return;
			case 26: // bintfmt
			   bIntFmt = isValidIntFmt(strToConsider);
			   if(bIntFmt == -1) {
				  throw new VicarException("Invalid value for bIntFmt" + strToConsider);
			   }
			   hash.put(lblType, INTFMT_VALUE[bIntFmt]);
			   break;
			default:
			   throw new VicarException("Invalid keyword" + lblType + " with value " + strToConsider);
			}

			try {
			   lblType = strTok.nextToken().trim();
			   strToConsider = strTok.nextToken().trim();
			}
			catch(NoSuchElementException e) {
			   // No more labels to process
			   return;
			}
		 } // while
	  }

	  private int isValidFormat(String in) {
		 for(int i=0; i<FMT_VALUE.length; i++) {
			if(in.equals(FMT_VALUE[i]))
				return i;
		 }
		 return -1;
	  }

	  private int isValidType(String in) {
		 for(int i=0; i<TYPE_VALUE.length; i++) {
			if(in.equals(TYPE_VALUE[i]))
				return i;
		 }
		 return -1;
	  }

	  private int isValidOrg(String in) {
		 for(int i=0; i<ORG_VALUE.length; i++) {
			if(in.equals(ORG_VALUE[i]))
				return i;
		 }
		 return -1;
	  }

	  private boolean isValidDim(int in) {
		 for(int i=0; i<DIM_VALUE.length; i++) {
			if(in == DIM_VALUE[i])
				return true;
		 }
		 return false;
	  }

	  private boolean isValidEol(int in) {
		 for(int i=0; i<EOL_VALUE.length; i++) {
			if(in == EOL_VALUE[i])
				return true;
		 }
		 return false;
	  }

	  private int isValidIntFmt(String in) {
		 for(int i=0; i<INTFMT_VALUE.length; i++) {
			if(in.equals(INTFMT_VALUE[i]))
				return i;
		 }
		 return -1;
	  }

	  private int isValidRealFmt(String in) {
		 for(int i=0; i<REALFMT_VALUE.length; i++) {
			if(in.equals(REALFMT_VALUE[i]))
				return i;
		 }
		 return -1;
	  }

	  private String stripQuotes(String in)
	   {
		  String temp = new String();

		  in = in.trim();
		  if((in.charAt(0) == '\'') && (in.charAt(in.length() - 1) == '\''))
			  temp = in.substring(1, in.length()-1);
		  else
			  temp = in;

		  return temp;
	   }

	  private int extractLabelSize(String input) throws VicarException {
		 Integer sz;

		 int index = input.indexOf('=');
		 if(index == -1)
			 throw new VicarException("Unable to extract label size. Cannot process file");

		 if(!((input.substring(0, index)).trim()).equals(LBL_NAME[0]))
			 throw new VicarException("Invalid Vicar File. LBLSIZE is not the first keyword.");

		 String strToConsider = input.substring(index+1).trim();
		 index = strToConsider.indexOf(' ');
		 if(index == -1)
			 index = strToConsider.length();
		 strToConsider = strToConsider.substring(0, index);
		 try {
			sz = new Integer(strToConsider);
		 }
		 catch(NumberFormatException ex) {
			throw new VicarException("Invalid value for lblSize" + strToConsider, ex);
		 }
		 return sz.intValue();
	  }

	  private void setDefaults() {
		 type = 0;
		 dim = 3;
		 eol = 0;
		 host = "VAX-VMS";
		 intFmt = 1;
		 realFmt = 2;
	  }

	  public int getLblSize() {
		 return lblSize;
	  }

	  public int getRecSize() {
		 return recSize;
	  }
	
	  public int getFormat() {
		 return format;
	  }

	  public int getIntFmt() {
		 return intFmt;
	  }

	  public int getRealFmt() {
		 return realFmt;
	  }

	  public int getNL() {
		 return nl;
	  }

	  public int getNS() {
		 return ns;
	  }

	  public int getNB() {
		 return nb;
	  }

	  public int getN1() {
		 switch(org) {
		 case 0:
			return ns;
		 case 1:
			return ns;
		 case 2:
			return nb;
		 }		
		 return -1;
	  }

	  public int getN2() {
		 switch(org) {
		 case 0:
			return nl;
		 case 1:
			return nb;
		 case 2:
			return ns;
		 }		
		 return -1;
	  }

	  public int getN3() {
		 switch(org) {
		 case 0:
			return nb;
		 case 1:
			return nl;
		 case 2:
			return nl;
		 }		
		 return -1;
	  }

	  public int getNBB() {
		 return nbb;
	  }

	  public int getNLB() {
		 return nlb;
	  }

	  public int getOrg() {
		 return org;
	  }

	  public Hashtable getHashTable() {
		 return hash;
	  }


	} // VicarHeader

   private static class VicarRaster extends WritableRaster {

	  public VicarRaster(SampleModel sm, DataBuffer db, Point location) {
		 super(sm, db, location);
	  }

	  public VicarRaster(SampleModel sm, Point location) {
		 super(sm, location);
	  }
	}

 } // VicarReader
