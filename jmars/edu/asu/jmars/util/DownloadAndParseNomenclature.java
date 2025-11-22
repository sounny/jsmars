package edu.asu.jmars.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class DownloadAndParseNomenclature {

	private static class PlaceMarkData {
		private String type = "";
		private String name = "";
		private String lat = "";
		private String lon = "";
		private String origin = "";
		private String diameter = "";
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		System.out.println("starting nomenclature build");
		String filePath = "./";
		String logPath = filePath + "output.txt";
		File logFile = new File(logPath);
		String baseUrl = "https://planetarynames.wr.usgs.gov/images/";
		String[] bodyList = new String[]{"ARIEL","BENNU","CALLISTO","CERES","CHARON","DEIMOS","DIONE","ENCELADUS","EROS","EUROPA","GANYMEDE",
				"IAPETUS","IO","ITOKAWA","MARS","MERCURY","MIMAS","MIRANDA","MOON","OBERON","PHOBOS","PHOEBE","PLUTO","RHEA",
				"TETHYS","TITAN","TITANIA","TRITON","UMBRIEL","VENUS","VESTA"};
		FileWriter writer = null;
		try {
			writer = new FileWriter(logFile);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		StringBuffer log = new StringBuffer();
		log.append("Starting Nomenclature update\n");
		
		final ArrayList<PlaceMarkData> features = new ArrayList<PlaceMarkData>();
		
//		final PlaceMarkData placeMark = new PlaceMarkData();
		final AtomicBoolean inPlaceMark = new AtomicBoolean(false);
		final AtomicBoolean inName = new AtomicBoolean(false);
		final AtomicBoolean inSimpleData = new AtomicBoolean(false);
		final AtomicBoolean inOrigin = new AtomicBoolean(false);
		final AtomicBoolean inDiameter = new AtomicBoolean(false);
		final AtomicBoolean inLon = new AtomicBoolean(false);
		final AtomicBoolean inLat = new AtomicBoolean(false);
		final AtomicBoolean inType = new AtomicBoolean(false);
		final AtomicReference<String> currentBodyFeaturesFile = new AtomicReference<String>();
		final Comparator<PlaceMarkData> comparator = new Comparator<PlaceMarkData>() {

			public int compare(PlaceMarkData data1, PlaceMarkData data2) {
				int typeCompare = data1.type.compareTo(data2.type);
				return (typeCompare != 0 ? typeCompare : data1.name.compareTo(data2.name));
			}
		};
		DefaultHandler handler = new DefaultHandler() {
			PlaceMarkData placeMark = null;

			public void startElement(String uri, String localName,
					String qName, Attributes attributes)
					throws SAXException {
				if ("Placemark".equals(qName)) {
					placeMark = new PlaceMarkData();
					inPlaceMark.set(true);
				} else if ("name".equals(qName)) {
					if (inPlaceMark.get()) {
						inName.set(true);
					}
				} else if ("SimpleData".equals(qName)) {
					if (inPlaceMark.get()) {
						inSimpleData.set(true);
						String nameAttr = attributes.getValue("name");
						if (nameAttr != null) {
							if (nameAttr.equals("diameter")) {
								inDiameter.set(true);
							} else if (nameAttr.equals("center_lon")) {
								inLon.set(true);
							} else if (nameAttr.equals("center_lat")) {
								inLat.set(true);
							} else if (nameAttr.equals("origin")) {
								inOrigin.set(true);
							} else if (nameAttr.equals("type")) {
								inType.set(true);
							}
						}
					}
				}
			}
			
			public void endElement(String uri, String localName,
					String qName) throws SAXException {
				if ("Placemark".equals(qName)) {
					inPlaceMark.set(false);
					//save the data
					features.add(placeMark);
				} else if ("name".equals(qName)) {
					if (inName.get()) {
						inName.set(false);
					}
				} else if ("SimpleData".equals(qName)) {
					inSimpleData.set(false);
					resetFlags();//reset all of the name flags
				} else if ("Document".equals(qName)) {
					//write out the data
					String nomenFileName = currentBodyFeaturesFile.get();
					OutputStreamWriter osw = null;
					try {
						osw = new OutputStreamWriter(new FileOutputStream(new File(nomenFileName)), "utf-8");
						StringBuffer buff = new StringBuffer();
						Collections.sort(features, comparator);
						for (PlaceMarkData data : features) {
							//clean up data
							if (data.type.trim().equalsIgnoreCase("Albedo Feature")) {
								data.type = "Albedo";
							} else if (data.type.indexOf(",") > -1) {
								//this cleans up Crater, craters and others like this
								data.type = data.type.substring(0,data.type.indexOf(","));
							}
							//end clean up data
							buff.append(data.type);
							buff.append("\t");
							buff.append(data.name);
							buff.append("\t");
							data.lat = formatDecimal(data.lat);
							buff.append(data.lat);
							buff.append("\t");
							data.lon = formatDecimal(data.lon);
							buff.append(data.lon);
							buff.append("\t");
							data.diameter = formatDecimal(data.diameter);
							buff.append(data.diameter);
							buff.append("\t");
							if (data.origin != null) {
								buff.append(data.origin.trim());
							} else {
								buff.append("");
							}
							buff.append("\n");
						}
						osw.write(buff.toString());
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						try {
							osw.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			private String formatDecimal(String num) {
				//3.4, not 3.40, 3.44, 
				if (num != null) {
					if (num.indexOf(".") < 0) {
						num += ".0";
					}
					if (num.endsWith(".")) {
						num += "0";
					}
					while (num.endsWith("0")) {
						if (num.endsWith(".0")) {
							break;
						}
						num = num.substring(0,num.length()-1);
					}
				} else {
					num = "";
				}
				return num;
			}
			public void characters(char[] ch, int start, int length)
					throws SAXException {
				if (inPlaceMark.get()) {//do not do anything if not in a place mark tag
					if (inName.get()) {
						// we are in a name tag
						String value = String.copyValueOf(ch, start, length);
						placeMark.name += value;
					} else if (inSimpleData.get()) {
						String value = String.copyValueOf(ch, start, length);
						if (inDiameter.get()) {
							//we are in the SimpleData tag with the diameter attribute
							placeMark.diameter += value;
						} else if (inOrigin.get()) {
							placeMark.origin += value;
						} else if (inLat.get()) {
							placeMark.lat += value;
						} else if (inLon.get()) {
							placeMark.lon += value;
						} else if (inType.get()) {
							placeMark.type += value;
						}
					}
				}
			}
			
			private void resetFlags() {
				inDiameter.set(false);
				inLon.set(false);
				inLat.set(false);
				inOrigin.set(false);
				inType.set(false);
			}
		};
		
		try {
			log.append(new Date().toString()+ " starting \n");
			for(String body : bodyList) {
				try {
					features.clear();
//					StringBuffer log2 = new StringBuffer();
					//set the kmz name
					String bodyFileName = body.toUpperCase() + "_nomenclature_center_pts.kmz";
//					System.out.println("body: "+bodyFileName+"\n");
					log.append("\nbody: "+bodyFileName+"\n");
					//set the current features.txt to be updated
					currentBodyFeaturesFile.set(filePath + body.toLowerCase()+"_features.txt");
					log.append("url: "+baseUrl+bodyFileName+"\n");
					//set the url for download
					URL fileToDownload = new URL(baseUrl+bodyFileName);
					log.append("Downloading "+bodyFileName+" to: "+filePath+bodyFileName+"\n");
					//download kmz file for this body
					File downloadedFile = new File(filePath + bodyFileName);
					log.append("unzipping\n");
					//download and unzip
					//No proxy support needed 
					try {
						FileUtils.copyURLToFile(fileToDownload, downloadedFile);
					} catch (IOException ioe) {
//						ioe.printStackTrace();
						log.append("Unable to download: "+bodyFileName+"...exiting");
						writer.write(log.toString());
						writer.flush();
						System.exit(100);
					}
					
					ZipFile zip = new ZipFile(downloadedFile);
					ZipEntry zipEnt = new ZipEntry(body.toUpperCase()+"_nomenclature_center_pts.kml");
					
					log.append("parsing\n");
					//send kml through the parser
					SAXParserFactory factory = SAXParserFactory.newInstance();
					SAXParser saxParser = factory.newSAXParser();
					saxParser.parse(zip.getInputStream(zipEnt), handler);
					
					zip.close();
					log.append("finished\n");
				} catch (Exception e) {
//					e.printStackTrace();
					log.append(e.getMessage());
					log.append("ERROR!!! Exiting: "+e.getMessage());
					writer.write(log.toString());
					writer.flush();
					System.exit(100);//failure, fix and try again
				} 
			}
			
			File moonFile = new File(filePath + "moon_features.txt");
			File lunaFile = new File(filePath + "luna_features.txt");
			moonFile.renameTo(lunaFile);
			
			//one last step, let's make a couple copies of Vesta
			File vestaFile = new File(filePath + "vesta_features.txt");
			File vestaDC = new File(filePath + "vesta_dawn_claudia_features.txt");
			File vestaIAU = new File(filePath + "vesta_iau_features.txt");
			FileUtils.copyFile(vestaFile, vestaIAU);
			//update the DP copy of vesta
			StringBuffer fileOut = new StringBuffer();
			BufferedReader buff = new BufferedReader(new FileReader(vestaFile));
			while (buff.ready()) {
				String line = buff.readLine();
				String[] tokens = line.split("[\t]");
				String lonStr = tokens[3];
				double lon = Double.parseDouble(lonStr);
				lon = lon - 150;
				if (lon < -180) {
					lon = lon + 360;
				}
				tokens[3] = NumberFormat.getInstance().format(lon);
				boolean first = true;
				StringBuffer oneLine = new StringBuffer();
				for(String val : tokens) {
					if (!first) {
						oneLine.append('\t');
					}
					oneLine.append(val);
					first = false;
				}
				fileOut.append(oneLine);
				fileOut.append('\n');
			}
			buff.close();
			OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(vestaFile), "utf-8");
			osw.write(fileOut.toString());
			osw.close();
			FileUtils.copyFile(vestaFile, vestaDC);
			
			Runtime.getRuntime().exec("sh ./finishNomenclature.sh");
			
		} catch (Exception e2){
			log.append(e2.getMessage());
		} finally {
			
		}
		
        try {
            writer.write(log.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}

}
