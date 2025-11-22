package edu.asu.jmars.layer.shape2;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import edu.asu.jmars.Main;
import edu.asu.jmars.graphics.GraphicsWrapped;
import edu.asu.jmars.layer.map2.MapChannelReceiver;
import edu.asu.jmars.layer.map2.MapChannelTiled;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.Pipeline;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.util.NumericMapSourceDialog;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeScrollPane;
import edu.asu.jmars.util.CSVFilter;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

public class PixelExportDialog extends JDialog{
	private ArrayList<FPath> paths;
	
	private JCheckBox idChk;
	private JCheckBox latChk;
	private JCheckBox lonChk;
	private JButton addSourceBtn;
	private JPanel sourcePnl;
	private JScrollPane sourceSP;
	private JLabel sourceLbl;
	private JLabel maxLbl;
	private JLabel delLbl;
	private ArrayList<MapSource> sources = new ArrayList<MapSource>();
	private JLabel ppdLbl;
	private JPanel ppdPnl;
	private JComboBox<Integer> ppdBx;
	private Vector<Integer> allPPD;
	private JButton fileBtn;
	private JLabel fileLbl;
	private JFileChooser chooser;
	private String fileStr;
	private JButton saveBtn;
	private JButton cancelBtn;
	
	private int row = 0;
	private int pad = 1;
	private Insets in = new Insets(pad, pad, pad, pad);
	
	private static DebugLog log = DebugLog.instance();
	
	private volatile boolean success = true;

	public PixelExportDialog(Frame owner, JComponent relTo, ArrayList<FPath> shapes){
		super(owner, "Pixel Data CSV Export", true);
		setLocationRelativeTo(relTo);
		
		paths = shapes;
		
		//build UI if the frame and owner aren't null
		// if they are null, then this is being run from
		// a unit test, and we don't need ui
		if(owner != null){
			buildUI();
			pack();
		}
	}
	
	private void buildUI(){
		//First part of the dialog -- select columns
		JPanel selPnl = new JPanel();
		selPnl.setBorder(new TitledBorder("Select Columns to Export"));
		idChk = new JCheckBox("Pixel ID");
		latChk = new JCheckBox("Latitude");
		lonChk = new JCheckBox("Longitude");
		//set all three to selected by default
		idChk.setSelected(true);
		latChk.setSelected(true);
		lonChk.setSelected(true);
		selPnl.add(idChk);
		selPnl.add(Box.createHorizontalStrut(5));
		selPnl.add(latChk);
		selPnl.add(Box.createHorizontalStrut(5));
		selPnl.add(lonChk);
		
		//next panel -- add numeric sources
		JPanel addPnl = new JPanel();
		addPnl.setBorder(new TitledBorder("Add Numeric Columns to Export"));
		addPnl.setLayout(new BorderLayout());
		addSourceBtn = new JButton(sourceAct);
		JPanel btnPnl = new JPanel();
		btnPnl.add(addSourceBtn);
		JPanel outerPnl = new JPanel();
		outerPnl.setLayout(new BorderLayout());
		sourcePnl = new JPanel();
		sourcePnl.setLayout(new GridBagLayout());
		sourcePnl.setBorder(new EmptyBorder(3, 3, 3, 3));
		sourceLbl = new JLabel("Source Name");
		maxLbl = new JLabel("Max PPD");
		delLbl = new JLabel("Delete");
		ppdLbl = new JLabel("Sampling PPD:");
		JPanel northPnl = new JPanel();
		northPnl.setLayout(new GridBagLayout());
		northPnl.add(sourceLbl, new GridBagConstraints(0, 0, 1, 1, 0.7, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		northPnl.add(maxLbl, new GridBagConstraints(1, 0, 1, 1, 0.3, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, in, 15*pad, pad));
		northPnl.add(delLbl, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, 20*pad, pad));
		sourceSP = new JScrollPane(sourcePnl, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		Color lineborder = ((ThemeScrollPane)GUITheme.get("scrollpane")).getLineborder();
		sourceSP.setBorder(new LineBorder(lineborder));
		sourceSP.setPreferredSize(new Dimension(450, 100));
		outerPnl.add(northPnl, BorderLayout.NORTH);
		outerPnl.add(sourceSP, BorderLayout.CENTER);
		//set the ppd list to the same as the zoom manager
		// (will limit based on added sources)
		allPPD = new Vector<Integer>(Main.testDriver.mainWindow.getZoomManager().getZoomFactors());
		ppdBx = new JComboBox<Integer>(allPPD);
		//set the ppd to the highest one available (seems better than the lowest)
		ppdBx.setSelectedIndex(ppdBx.getItemCount()-1);
		ppdPnl = new JPanel();
		ppdPnl.add(ppdLbl);
		ppdPnl.add(ppdBx);
		
		addPnl.add(btnPnl, BorderLayout.NORTH);
		addPnl.add(outerPnl, BorderLayout.CENTER);
		addPnl.add(ppdPnl, BorderLayout.SOUTH);
		
		//next panel -- set file to save to
		JPanel filePnl = new JPanel();
		filePnl.setBorder(new TitledBorder("Set Output File"));
		fileBtn = new JButton(fileAct);
		fileLbl = new JLabel();
		filePnl.add(fileBtn);
		filePnl.add(Box.createHorizontalStrut(2));
		filePnl.add(fileLbl);
		//set up the directory chooser
		chooser = new JFileChooser(Util.getDefaultFCLocation());
//		chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
		chooser.setDialogTitle("Choose Output File");
		CSVFilter filter = new CSVFilter();
		chooser.setFileFilter(filter);
		
		//last panel -- save file
		JPanel savePnl = new JPanel();		
		saveBtn = new JButton(saveAct);
		//disable the save button until a file location is chosen
		saveBtn.setEnabled(false);
		cancelBtn = new JButton(cancelAct); 
		savePnl.add(saveBtn);
		savePnl.add(Box.createHorizontalStrut(5));
		savePnl.add(cancelBtn);
		
		//make an imbedded panel for most of the content
		JPanel innerPnl = new JPanel();		
		innerPnl.setLayout(new BorderLayout());
		innerPnl.add(selPnl, BorderLayout.NORTH);
		innerPnl.add(addPnl, BorderLayout.CENTER);
		innerPnl.add(filePnl, BorderLayout.SOUTH);
		
		//add everything to main panel
		JPanel mainPnl = new JPanel();		
		mainPnl.setLayout(new BorderLayout());
		mainPnl.setBorder(new EmptyBorder(10, 5, 5, 5));
		
		mainPnl.add(innerPnl, BorderLayout.CENTER);
		mainPnl.add(savePnl, BorderLayout.SOUTH);
		
		setContentPane(mainPnl);
	}

	private void refreshLayout(){
		//remove everything
		sourcePnl.removeAll();
		ppdPnl.remove(1); //remove the ppdBx
		//reset gui counter
		row = 0;
		
		//reset ppd counter
		int maxPPD = Integer.MAX_VALUE;
		
		if(sources.size()>0){
			//cycle through sources and add rows, check for smallest ppd
			for(MapSource ms : sources){
				SourceRowData s = new SourceRowData(ms);
				
				int ppd = s.getMaxPPD();
				
				sourcePnl.add(new JLabel(s.getTitle()), new GridBagConstraints(0, ++row, 1, 1, .9, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, in, 15*pad, 3*pad));
				sourcePnl.add(new JLabel(ppd+""), new GridBagConstraints(1, row, 1, 1, .1, 0, GridBagConstraints.NORTH, GridBagConstraints.NONE, in, 15*pad, 3*pad));
				sourcePnl.add(s.getDeleteBtn(), new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.NORTH, GridBagConstraints.NONE, in, 15*pad, 3*pad));
			
				if(ppd<maxPPD){
					maxPPD = ppd;
				}
			}
			//add an empty box at the bottom of the space to push all the rows up to the top
			sourcePnl.add(Box.createHorizontalStrut(5), new GridBagConstraints(0, ++row, 3, 1, 0, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, in, pad, pad));
			
			//enable the save button if a file has been set
			if(fileStr!=null && fileStr.length()>0 && fileStr.contains("csv")){
				saveBtn.setEnabled(true);
			}
		}else{
			//disable the save because no source has been selected
			saveBtn.setEnabled(false);
		}
		
		//update the ppd box
		//if nothing has been selected, populate box with all values
		if(maxPPD == Integer.MAX_VALUE){
			ppdBx = new JComboBox<Integer>(allPPD);
		}else{
			//else, create a new, shortened list of the available export ppds
			Vector<Integer> ppds = new Vector<Integer>();
			for (int ppd : allPPD){
				//if the ppd is less than or equal to the max ppd, add it as an option
				if(ppd<=maxPPD){
					ppds.add(ppd);
				}
			}
			//if the smallest ppd is 0, make sure to add as an option to the box
			if(ppds.size() == 0){
				ppds.add(1);
			}
			ppdBx = new JComboBox<Integer>(ppds);
			
			//set the ppd to the highest one available (seems better than the lowest)
			ppdBx.setSelectedIndex(ppdBx.getItemCount()-1);
		}
		ppdPnl.add(ppdBx, 1);
		
		//refresh
		sourcePnl.revalidate();
		sourcePnl.repaint();
		ppdPnl.revalidate();
		ppdPnl.repaint();
	}

	
	private AbstractAction sourceAct = new AbstractAction("Add Source...".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			ArrayList<MapSource> msList = NumericMapSourceDialog.getUserSelectedSources(addSourceBtn, true, true);
			if(msList!=null){
				sources.addAll(msList);
				refreshLayout();
			}
		}
	};

	private AbstractAction fileAct = new AbstractAction("Select File...".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			int val = chooser.showDialog(PixelExportDialog.this, "OK".toUpperCase());
			if(val == JFileChooser.APPROVE_OPTION){
				fileStr = chooser.getSelectedFile().getPath();
				//check to see if user added extension, add it if they didn't
				if (!fileStr.contains(".csv")){
					fileStr += ".csv";
				}
			}
			//update label
			fileLbl.setText(fileStr);
			pack();
			
			//enable the save button if a valid file name is set  
			// and at least one source selected
			if(fileStr!=null && fileStr.length()>0 && fileStr.contains("csv") && sources.size()>0){
				saveBtn.setEnabled(true);
			}
		}
	};
	
	private AbstractAction saveAct = new AbstractAction("Save".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			//Change cursor
			PixelExportDialog.this.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			try {
				//create the writer that will be used for the csv
				FileWriter fw = new FileWriter(new File(fileStr));
				BufferedWriter bw = new BufferedWriter(fw);
				
				saveCSV(idChk.isSelected(), latChk.isSelected(), lonChk.isSelected(), true, 
						sources, (int)ppdBx.getSelectedItem(), paths, bw);
				
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	};
	
	
	/**
	 * This method is only public to allow for calls for unit testing.
	 * Takes in the column paramenters (id, lat lon) and other things 
	 * necessary to calculate pixel value under the shape.
	 * @param addId  	Whether or not to include an ID column
	 * @param addLat 	Whether or not to include a latitude column
	 * @param addLon	Whether or not to include a longitude column
	 * @param includeNull	Whether or not to include pixels which have a null 
	 * value from their numeric source(s)
	 * @param maps		Sources to sample from
	 * @param ppd		Ppd to sample at
	 * @param fpath		Shape to sample under
	 * @param writer	Writer to use to create the output file
	 */
	public void saveCSV(final boolean addId, final boolean addLat, final boolean addLon, 
			final boolean includeNull, final ArrayList<MapSource> maps, final int ppd,
			final ArrayList<FPath> fpaths, final BufferedWriter writer){
		
		//keep track of bounds and rois for EACH path
		final HashMap<Shape, Rectangle2D.Double> roiToBounds = new HashMap<Shape, Rectangle2D.Double>();
		//track a pixel estimation
		double estimatedPixels = 0;
		//cycle through all the paths to get their defining bounds and rois
		for(FPath fpath : fpaths){
			//Calculate the bounds based of the selected shape and use
			// the ppd to estimate the amount of pixels being exported
			Shape shape = fpath.getWorld().getShape();
			final Rectangle2D.Double bounds = new Rectangle2D.Double();
			bounds.setRect(shape.getBounds2D());
			//This is the shape passed on to the map tiles for map sampling
			final Shape roi;
			
			//expand bounds out to nearest pixel boundary in all
			double dpp = 1d/ppd;
			double x1 = Math.floor(bounds.x * ppd) * dpp;
			double x2 = Math.ceil((bounds.x + bounds.width) * ppd) * dpp;
			if (x1 == x2) {
				x1 -= dpp;
				x2 += dpp;
			}
			double y1 = Math.floor(bounds.y * ppd) * dpp;
			double y2 = Math.ceil((bounds.y + bounds.height) * ppd) * dpp;
			if (y1 == y2) {
				y1 -= dpp;
				y2 += dpp;
			}
			bounds.setFrameFromDiagonal(x1, y1, x2, y2);
			
			// keep shape x values >= 0, since MapData will as well and
			// we want the overlap checking that occurs later to remain
			// simple
			if (bounds.x < 0) {
				bounds.x += 360;
			}
			
			//estimate the output size and ask the user if they want to continue
			double shapeArea = shape.getBounds2D().getWidth()*shape.getBounds2D().getHeight();
			double pixelArea = dpp*dpp;
			estimatedPixels += shapeArea/pixelArea;
			//System.out.println("pixels: "+estimatedPixels);
			
			// Use the bounding box for points, to ensure we enclose some area
			// If our shape is smaller than a single pixel, use the entire bounds as well.  Otherwise we can end up with NO DATA
			// because the code to draw the roi into the pixel will decide not to draw anything at all
			if (fpath.getType() == FPath.TYPE_POINT || shapeArea < pixelArea) {
				roi = bounds;
			}else{
				roi = shape;
			}
			
			//add the bounds and roi to the hashmap for all paths
			roiToBounds.put(roi, bounds);
		}
			
		if(estimatedPixels>100000){
			int result = Util.showConfirmDialog( 
					"A large number of pixels will be exported.\nDo you want to continue?",
					"Pixel Count Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			//if user says no or closes the dialog, then don't continue.
			if(result == JOptionPane.NO_OPTION || result == JOptionPane.CLOSED_OPTION){
				return;
			}
			//otherwise just keep going and close the dialog
		}
			
		final double estPixels = estimatedPixels;
		
		//Store the data for each shape individually, so they
		// can be written out one after the other
		final HashMap<Shape, ConcurrentHashMap<String, ArrayList<MapData>>> roiToMapData = new HashMap<Shape, ConcurrentHashMap<String, ArrayList<MapData>>>();
		
		Thread manager = new Thread(new Runnable() {
			//total number of threads that need to finish before
			// writing out the csv is the number of maps being sampled
			// multiplied by the number of shapes being sampled
			int threadNum = maps.size()*fpaths.size();
			private CountDownLatch counter = new CountDownLatch(threadNum);
			//thread pool does not need to have a large number of threads
			private ExecutorService threads = Executors.newFixedThreadPool(4);
			
			@Override
			public void run() {
				//get all necessary tiles
				//for each path
				for(Shape roi : roiToBounds.keySet()){
					//send requests for each of the sources, and get a map
					// that consists of 'source name' --> 'array of tiles'.
					// Make sure every array of tiles is sorted the same, so
					// all the pixels correlate with each other exactly.
					final ConcurrentHashMap<String, ArrayList<MapData>> sourceToMapData = new ConcurrentHashMap<String, ArrayList<MapData>>();

					//for each map source
					for(final MapSource ms : maps){
						threads.submit(new MapSampler(roiToBounds.get(roi), ms, ppd, counter, sourceToMapData));
					}
					//store each set of tiles for each shape
					roiToMapData.put(roi, sourceToMapData);
				}
				
				
				try {
					Long appropriateWait = (long)estPixels*10;
					if(appropriateWait<100000){
						appropriateWait = (long)100000;
					}
					counter.await(appropriateWait, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
					success = false;
				}
				
				if(counter.getCount()>0){
					//we timed out but didn't finish the requests
					success = false;
					log.aprintln("Request timed out while trying to pull map tiles for pixel export.");
				}else{
					//we finished all of the requests, so now we can print out the csv.
					
					//Construct the header for the output file
					String header = "";
					if(addId){
						header += "OID,";
					}
					if(addLat){
						header += "Latitude (N),";
					}
					if(addLon){
						header += "Longitude (E),";
					}
					for(MapSource sd : maps){
						header += sd.getTitle()+",";
					}
					//remove the last comma
					header = header.substring(0, header.length()-1);
					
					//Create file to write to
					try {
						
						//write header
						writer.write(header);
						writer.newLine();
						
						//cycle through each shape (roi)
						for(Shape roi : roiToBounds.keySet()){
						
							//get the rigth map of tiles and sources
							ConcurrentHashMap<String, ArrayList<MapData>> sourceMap = roiToMapData.get(roi);
							// source and then each pixel in each image, and print out a
							// row in the csv
							//all the arrays are the same length since they use the same bounds and ppd
							int numOfTiles = ((ArrayList<MapData>)sourceMap.values().toArray()[0]).size();
							//Cycle through each tile and for each one, cycle through all the pixels and
							// print out data when a pixel falls under the roi
							int count = 0;
							for(int n=0; n<numOfTiles; n++){
								
								MapData mapData = ((ArrayList<MapData>)sourceMap.values().toArray()[0]).get(n);
								
								Area finished = mapData.getFinishedArea();
								Rectangle2D.Double tileBounds = getTileBoundsFromFinishedArea(finished, roiToBounds.get(roi));
								
								Raster raster = mapData.getRasterForWorld(tileBounds);
								int width = raster.getWidth();
								int height = raster.getHeight(); 
								BufferedImage maskImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
								Graphics2D g2 = maskImage.createGraphics();
								g2.setTransform(Util.world2image(tileBounds, width, height));
								g2 = new GraphicsWrapped(g2,360,ppd,tileBounds,"maskWrapped");
								try {
									g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
									g2.setColor(Color.white);
									// anti-aliasing is slower and unwanted here
									g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
									// fill and then draw a border, just more than 1 pixel so
									// pixels touching the shape are always filled in
									g2.fill(roi);
									g2.setStroke(new BasicStroke(1.01f/ppd));
									g2.draw(roi);
								} finally {
									g2.dispose();
								}
								
								int[] mask = new int[width*height];
								maskImage.getRaster().getPixels(0, 0, width, height, mask);
								Rectangle region = mapData.getRasterBoundsForWorld(tileBounds);
								
								for (int j = 0; j < height; j++) {
									for (int i = 0; i < width; i++) {
										if (mask[j*width+i] != 0) {
											//build the array of values for this row
											ArrayList<String> row = new ArrayList<String>();
											
											//print out the id if selected
											if(addId){
												row.add(++count+"");
											}
											if(addLat || addLon){
												//do y
												//for some reason this gets the bottom left corner, because of world coords?
												double y = tileBounds.getBounds2D().getY();
												//find the top left corner
												double tlY = y + height*(1d/ppd);
												//Find the y pixel that the loop is at currently, and 
												//add 0.5 to j to have the lat be at the middle of the pixel
												double newWorldY = (tlY - (j+0.5)*(1.0/ppd));
												
												//do x 
												double x = tileBounds.getBounds2D().getX();
												//Find the x pixel that the loop is at currently, and 
												//add 0.5 to i to have the lon be at the middle of the pixel
												double newWorldX = (((i+0.5)*(1.0/ppd)) + x);
												
												//convert 
												Point2D spatialPt = Main.PO.convWorldToSpatial(newWorldX,newWorldY);
												
												if(addLat){
													//add to csv 
													row.add(spatialPt.getY()+ "");
												}
												if(addLon){
													//add to csv
													double spatX = Math.abs(360-spatialPt.getX());
													row.add(spatX + "");
												}
											}
											
											//for each numeric source print out the value
											for(MapSource s : maps){
												
												ArrayList<MapData> tiles = sourceMap.get(s.getTitle());
												
												MapData tile = tiles.get(n);
												
												Raster r = tile.getRasterForWorld(tileBounds);
												
												//write out null if no data at that point
												if(tile.isNull(i + region.x, j + region.y)){
													row.add(null);
												}
												//else get the value and write that out
												else{	
													row.add(r.getSampleDouble(i, j, 0)+"");														
												}
											}
	
											//write out the line.
											//If we want to allow the user to specify to 
											// not write out lines containing null values, we can 
											// do that here by checking for null in the arraylist.
											// And if it has one, don't print.
											if(includeNull || (!includeNull && !row.contains(null))){
												for(int a=0; a<row.size(); a++){
													writer.write(row.get(a)+"");
													//don't add a comma after last entry in the row
													if(a<row.size()-1){
														writer.write(",");
													}
												}
												//end of row, write new line
												writer.newLine();
											}
										}
									}
								}
							}
						}
						
//						System.out.println("finished writing");
						//close writer
						writer.close();
						
					} catch (IOException e1) {
						e1.printStackTrace();
						success = false;
					}
					
					//now we're al done, so kick back on to the swing thread, 
					// and tell the user if their file finished
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							if(success){
								Util.showMessageDialog(
										"Pixel export has succesfully completed!",
										"Export Successfull", JOptionPane.INFORMATION_MESSAGE);
								//close the export dialog
								PixelExportDialog.this.dispose();
							}else{
								Util.showMessageDialog(
										"Pixel export was unsuccessful, see log for more information.",
										"Export Failure", JOptionPane.INFORMATION_MESSAGE);
								//close dialog
								PixelExportDialog.this.dispose();
							}
						}
					});
				}
				
			}
		});

		manager.start();
	}
	
	
	
	/**
	 * This code was copied from the old PixelExport, which was believed to 
	 * be copied from the map sampling code.  It seems to check to verify
	 * that the tile returned really does intersect the original area of
	 * interest.  This might fail if the tile does not come back with data
	 * for some reason, and that could be why this code was necessary.
	 * 
	 * @param finished  The finished region of the tile.
	 * @param entireBounds  The original bounds request for all the tiles
	 * @return The bounds of the finished tile that intersect the original 
	 * bounds request.
	 */
	private Rectangle2D.Double getTileBoundsFromFinishedArea(Area finished, Rectangle2D.Double entireBounds){
		Rectangle2D.Double tileBounds = new Rectangle2D.Double();
		tileBounds.setFrame(entireBounds);
		Rectangle2D finishedBounds = finished.getBounds2D();
		if (!finishedBounds.intersects(tileBounds)) {
			double xdelta = finishedBounds.getMinX() - tileBounds.x;
			tileBounds.x += 360 * Math.signum(xdelta);
		}
		Rectangle2D.intersect(tileBounds, finishedBounds, tileBounds);
		
		return tileBounds;
	}
	
	
	private AbstractAction cancelAct = new AbstractAction("Cancel".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			PixelExportDialog.this.dispose();
		}
	};
	

	private class SourceRowData {
		private MapSource source;
		private JButton delBtn;
		
		private SourceRowData(MapSource source){
			this.source = source;
			delBtn = new JButton(delAct);
			delBtn.setPreferredSize(new Dimension(28,20));
		}
		
		private String getTitle(){
			return source.getTitle();
		}
		
		private int getMaxPPD(){
			return (int)source.getMaxPPD();
		}
		
		private JButton getDeleteBtn(){
			return delBtn;
		}
		
		private AbstractAction delAct = new AbstractAction("X".toUpperCase()) {
			public void actionPerformed(ActionEvent e) {
				sources.remove(source);
				refreshLayout();
			}
		};
	}
	
	
	private class MapSampler implements Runnable, MapChannelReceiver{
		private MapChannelTiled ch = new MapChannelTiled(this);
		private Rectangle2D.Double bounds;
		private MapSource source;
		private int ppd;
		private ArrayList<MapData> tiles;
		private CountDownLatch counter;
		private ConcurrentHashMap<String, ArrayList<MapData>> dataMap;
		
		private MapSampler(Rectangle2D.Double bounds, MapSource mapSource, int ppd, CountDownLatch latch, ConcurrentHashMap<String, ArrayList<MapData>> map){
			this.bounds = bounds;
			this.ppd = ppd;
			source = mapSource;
			tiles = new ArrayList<MapData>();
			counter = latch;
			dataMap = map;
		}
		
		@Override
		public void mapChanged(MapData mapData) {
			if(mapData.isFinished()){
				// all fragments for this MapData have arrived or failed
				// so see if the portion of 'bounds' under this request finished
				
				Area finished = mapData.getFinishedArea();
				Rectangle2D.Double tileBounds = getTileBoundsFromFinishedArea(finished, bounds);
				
				if (mapData.getImage() == null || !finished.contains(tileBounds)) {
					success = false;
					// missing data, failure occurred...
					log.aprintln("Tile did not return succesfully for "+source.getTitle()+".");
					finish();
				} else {
					// include this tile in the running stats
					tiles.add(mapData);
					if (ch.isFinished()) {
						//close the channel request, add the tiles array to the
						// map that was passed in, and decrease the CountDownLatch
						finish();
					}
				}
			
			}
		}

		private void finish() {
			//close the channel request
			ch.cancel();
			
			//Need to sort the tiles array in a consistent way
			// so that the arrays from all the sources are in the same
			// order. This way accessing a pixel in the nth tile in 
			// each array is always the same pixel in all sources 
			Collections.sort(tiles, byBounds);
			
			//add the array to the hashmap
			dataMap.put(source.getTitle(), tiles);
			
			synchronized(this) {
				notifyAll();
			}
			
			//finally, modify the countdown latch because we are finished with this thread
			counter.countDown();
		}
		
		@Override
		public void run() {
			try{
				ch.setRequest(Main.PO, bounds, ppd, new Pipeline[]{new Pipeline(source, new Stage[0])});
			}catch (Exception e){
				e.printStackTrace();
				finish();
			}
		}
		
		private Comparator<MapData> byBounds = new Comparator<MapData>() {
			public int compare(MapData o1, MapData o2) {
				//compare x values of the two tiles
				int result = ((Double)o1.getRequest().getExtent().getX()).compareTo((Double)o2.getRequest().getExtent().getX());
				if(result == 0){
					//if they have the same x values, compare y values
					return ((Double)o1.getRequest().getExtent().getY()).compareTo((Double)o2.getRequest().getExtent().getY());
				}else{
					//if they have different x values, return that
					return result;
				}
			}
		};

		
	}

}
