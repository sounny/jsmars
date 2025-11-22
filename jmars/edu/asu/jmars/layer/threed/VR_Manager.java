package edu.asu.jmars.layer.threed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ZoomManager;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewManager;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapAttrReceiver;
import edu.asu.jmars.layer.map2.MapChannel;
import edu.asu.jmars.layer.map2.MapChannelReceiver;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.MapLView;
import edu.asu.jmars.layer.map2.MapRequest;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView;
import edu.asu.jmars.layer.threed.VRScene.Layer;
import edu.asu.jmars.layer.threed.VRScene.LayerData;
import edu.asu.jmars.swing.ColorMapOp;
import edu.asu.jmars.swing.LikeDefaultButtonUI;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUIState;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeTextField;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

public class VR_Manager {
	private static VR_Manager instance = null;
	private JDialog dialog = null;
	
	//left panel components
	private JTextField sceneNameTF = null;
	private JTextField updateSceneNameTF = null;
	private JComboBox<Integer> ppdCB = null;
	private JButton elevationSourceBtn = null;
	private JLabel elevationSourceLbl = null;
	private JLabel updateElevationSourceLbl = null;
	private MapSource elevationSource = null;
	private JTextField startingPointTF = null;
	private JTextField updateStartingPointTF = null;
	private JButton startingPointBtn = null;
	private JComboBox<String> scalingModeCB = null;
//	private JTextField verticalExaggerationTF = null;
//	private JTextField updateVerticalExaggerationTF = null;
	private JTextField totalExaggerationTF = null;
	private JLabel sceneDataDisplayLbl = null;
	private ZoomManager zoomManager = null;
	private JLabel ppdLbl = null;
	private Color dragColor = null;
	private Color onColor = null;
	private Color offColor = null;
	private ImageIcon dragIcon = null; 
	private ImageIcon onIcon = null;
	private ImageIcon offIcon = null;
	private ImageIcon trashIcon = null;
	private ImageIcon qrIcon = null;
	private ImageIcon plusIcon = null;
	private ImageIcon ellipsis = null;
	private Color inputBGColor = ((ThemeTextField) GUITheme.get("textfield")).getBackground();
	private Color panelColor = ((ThemePanel) GUITheme.get("panel")).getBackground();
	private Color buttonColor = ((ThemeImages) GUITheme.get("images")).getCommonFill();
//	private CompoundIcon scenesIcon = null;
	private ImageIcon newLayerIcon = null;
	private JPanel sceneDetailsPanel = null;
	private JPanel initialPanel = null;
	private JPanel mainPanel = null;
	private GroupLayout mainLayout = null;
	private JButton scenesBtn = null;
	private JPanel mainNavPanel = null;
	private JButton saveSceneBtn = null;
	private JButton cancelNewSceneBtn = null;
	
	private ArrayList<VRLayer> vrLayerSurfaceList = new ArrayList<VRLayer>();
	private ArrayList<VRLayer> vrLayerGlobeList = new ArrayList<VRLayer>();
	private ArrayList<VRLayer> vrDeletedLayerSurfaceList = new ArrayList<VRLayer>();
	private ArrayList<VRLayer> vrAddedLayerSurfaceList = new ArrayList<VRLayer>();
	private ArrayList<VRLayer> vrAddedLayerGlobeList = new ArrayList<VRLayer>();
	private ArrayList<VRLayer> vrDeletedLayerGlobeList = new ArrayList<VRLayer>();
	
	private ArrayList<VRScene> allScenes = null;
	private DefaultListModel<JComponent> surfaceModel = null;
	private DefaultListModel<JComponent> globeModel = null;
	private ThreeDFocus focusPanel = null;
	private ThreeDLayer threeDLayer = null;
	private ThreeDCanvas canvasPanel = null;
	private JComponent zoomComponent = null;
	private JLabel zoomLabel = null;
	private JDialog modalDialog = null; 
	private JButton qrBtn = null;
	private int curPpd = -1;
	private static DebugLog log = DebugLog.instance();
	
	private VRScene selectedScene = null;
	
	public static VR_Manager getInstance() {
		if (instance == null) {
			instance = new VR_Manager();
		} 
//		else {
//			instance.showDialog();
//		}
		return instance;
	}
	public JPanel getDisplay(ThreeDFocus focus, ThreeDLayer layer, ThreeDCanvas canvas) {
		focusPanel = focus;
		threeDLayer = layer;
		canvasPanel = canvas;

		elevationSource = layer.getElevationSource();
		setupUI();
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(mainPanel, BorderLayout.WEST);
		return panel;
	}
//	public void showDialog() {
//		dialog.setVisible(true);
//	}
	private VR_Manager() {
		onColor = Color.WHITE;
		offColor = Color.GRAY;
		dragColor = Color.WHITE;
        String theme = GUIState.getInstance().themeAsString();
		if (GUITheme.LIGHT.asString().equalsIgnoreCase(theme)) {
        	dragColor = Color.GRAY;
        	onColor = Color.BLACK;
        	offColor = Color.GRAY;
        }	
		dragIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DRAG_DOTS.withDisplayColor(dragColor)));
		onIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.EYE_VR.withDisplayColor(onColor)));
		offIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.EYE_SLASH.withDisplayColor(offColor)));
		trashIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.TRASH.withDisplayColor(dragColor)));
		qrIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.QR.withDisplayColor(onColor)));
		plusIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.ADD_LAYER.withDisplayColor(dragColor)));
		newLayerIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.NEW_LAYER.withDisplayColor(dragColor)));
		ellipsis = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.ELLIPSE_MENU.withDisplayColor(dragColor)));
//		scenesIcon = new CompoundIcon(CompoundIcon.Axis.X_AXIS, 15, newlayerIcon, ellipsis);
//		scenesIcon.setUseWidthOfComponent(true);
//        scenesIcon.setWidthOfComponentOffset(63);
		surfaceModel = new DefaultListModel<JComponent>();
		globeModel = new DefaultListModel<JComponent>();
		
		ppdLbl = new JLabel("xx");
		
		//init left panel components
		sceneNameTF = new JTextField(30);
		updateSceneNameTF = new JTextField(30);
		
		
//		elevationSourceBtn = new JButton(selectElevationSourceAction);
		elevationSourceLbl = new JLabel("No Source Selected");
		updateElevationSourceLbl = new JLabel("No Source Selected");
		
		startingPointTF = new JTextField(10);
		updateStartingPointTF = new JTextField(10);
		
		startingPointTF.setText("0, 0");
		updateStartingPointTF.setText("0, 0");
		
		startingPointBtn = new JButton(startingPointAction);
//		verticalExaggerationTF = new JTextField(15);
//		updateVerticalExaggerationTF = new JTextField(15);
//		totalExaggerationTF = new JTextField(15);
		sceneDataDisplayLbl = new JLabel("");
		
        getScenes();
	}

	private MapData depthData = null;
//	private Integer elevationDataType = null;
	private void saveScene() {
		saveSceneBtn.setEnabled(false);
		cancelNewSceneBtn.setEnabled(false);
		//set up logic for retreiving elevation data
	    MapChannel mapProducer = new MapChannel();
    	mapProducer.addReceiver(new MapChannelReceiver() {
			public void mapChanged(MapData mapData) {
				if (mapData.isFinished()) {
					depthData = mapData;
				}
			}
    	});
    	final ProjObj po = Main.testDriver.mainWindow.getProj().getProjection();
    	Integer expPpd = (Integer)ppdCB.getSelectedItem();
    	Rectangle2D wRect = Main.testDriver.mainWindow.getProj().getWorldWindow();
    	//start the elevation data request
    	mapProducer.setRequest(new MapRequest(threeDLayer.getElevationSource(), wRect, expPpd, po));
    	
//    	threeDLayer.getElevationSource().getMapAttr(new MapAttrReceiver() {
//			
//			@Override
//			public void receive(MapAttr attr) {
//				// TODO Auto-generated method stub
//				
//			}
//		});
    	
    	// Additional numeric layers
    	ArrayList<MapChannel> numericChannel = new ArrayList<MapChannel>();
    	HashMap<MapChannel, MapData> numericData = new HashMap<MapChannel, MapData>();
    	HashMap<MapChannel, File> numericFiles = new HashMap<MapChannel, File>();
    	HashMap<MapChannel, LayerData> layerDataObjs = new HashMap<MapChannel, LayerData>();
    	
    	Thread manager = new Thread(new Runnable() {
			public void run() {
				try {
			    	
			    	StartupParameters settings = focusPanel.getSettings();
			    	LView parent = focusPanel.parent;
			    	
					String exaggerationStr = String.valueOf(settings.scaleOffset);
					
					
					// TODO: Old logic for dimensions:
					final Point2D[] viewCorners = parent.viewman.getWorldBoundingPoints();
					final Point2D min = viewCorners[0];
					final Point2D max = viewCorners[1];
					final double xDistance = Util.angularAndLinearDistanceW(new Point2D.Double(min.getX(), 0),
							new Point2D.Double(max.getX(), 0), parent.viewman.getProj())[1];
					final double yDistance = Util.angularAndLinearDistanceW(new Point2D.Double(0, min.getY()),
							new Point2D.Double(0, max.getY()), parent.viewman.getProj())[1];

					String xDimension = String.valueOf(Math.abs(xDistance) * 1000);
					String yDimension = String.valueOf(Math.abs(yDistance) * 1000);
					String zDimension = String.valueOf(canvasPanel.getElevation().getMaxAltitude() - canvasPanel.getElevation().getMinAltitude());
					String dimensions = xDimension+" x "+yDimension+" x "+zDimension;
					
			    	// TODO: Populate properly
			    	String bodyStr = Main.getBody();
			    	File depthImageFile = Files.createTempFile("depth_", ".tif").toFile();
			    	String dimensionUnitsStr = "m";
			    	String startingPointStr = startingPointTF.getText();
			    	    	
			    	
					//get the graphics object that will be drawn into by each layer
//					final Graphics2D g2 = (Graphics2D)finalImage.getGraphics();
					//get all the layers (and use their lview3ds to get the data)
			    	
//					final ArrayList<LView> views = new ArrayList<LView>(LManager.getLManager().viewList);
					
					
					//kick off a new thread, because the map layer will not run from the awt thread
					
					
					//color map op is needed to convert images based on
					// the alpha value of their lview
					ColorMapOp cmo = new ColorMapOp(null);
					
					BufferedImage sceneThumbnail = Main.testDriver.mainWindow.getSnapshot(true);
					
					File sceneThumbFile = Files.createTempFile("thumbnail_", ".jpg").toFile();
					
					Image img = sceneThumbnail.getScaledInstance(300, 150, Image.SCALE_SMOOTH);
					sceneThumbnail = new BufferedImage(300,150,BufferedImage.TYPE_INT_RGB);
					sceneThumbnail.getGraphics().drawImage(img, 0, 0, null);
					ImageIO.write(sceneThumbnail, "jpg", sceneThumbFile);
					
					
					VRScene vrScene = new VRScene(sceneNameTF.getText(), bodyStr, depthImageFile, exaggerationStr, dimensions, dimensionUnitsStr, startingPointStr, expPpd,  sceneThumbFile);
					
					// they are already in order from bottom to top
					for (VRLayer vrLayer : vrLayerSurfaceList) {
						LView lview = vrLayer.lview;
						
						
						if (lview instanceof NomenclatureLView) {
							
							VRScene.Layer nomenclatureLayer = vrScene.createSceneLayer("Nomenclature", null, false, vrLayer.on);
							final Rectangle2D viewRectangle = new Rectangle2D.Double(min.getX(), min.getY(), max.getX() - min.getX(),
									max.getY() - min.getY());
			
							final Predicate<NomenclatureLView.MarsFeature> featurePredicate = (feature) -> viewRectangle
									.contains(Main.PO.convSpatialToWorld(feature.longitude, feature.latitude));
			
							final Function<NomenclatureLView.MarsFeature, JSONObject> featureToJSON = (feature) -> {
								try {
									Point2D point = parent.viewman.getProj().spatial.toScreen(feature.longitude, feature.latitude);
									//adjust for higher export resolution
									int ratio = expPpd/curPpd;
									
									return new JSONObject().put("name", feature.name).put("x", point.getX()*ratio).put("y", point.getY()*ratio);
								} catch (JSONException ex) {
									throw new RuntimeException(ex);
								}
							};
							final List<JSONObject> nomenclatureObjects = parent.viewman.viewList.stream()
									.filter(NomenclatureLView.class::isInstance).findFirst().map(NomenclatureLView.class::cast)
									.map(NomenclatureLView::getShownLandmarks).map(features -> features.stream().filter(featurePredicate)
											.map(featureToJSON).collect(Collectors.toList()))
									.orElse(Collections.emptyList());
							
							final JSONArray nomenclature = new JSONArray(nomenclatureObjects);
							nomenclatureLayer.createNomenclatureData(nomenclature);
						} else {
						
							//get the lview3d
							LView3D view3d = lview.getLView3D();
//							System.err.println("Getting "+lview.getName()+" data for Hi Res Export");
							
							//TODO: can come back to this (this is for label scaling)
		//					int scaleFactor = getScaleFactor();
							int scaleFactor = 1;
							
							final BufferedImage finalLViewImage = GraphicsEnvironment
								    .getLocalGraphicsEnvironment()
								    .getDefaultScreenDevice()
								    .getDefaultConfiguration()
								    .createCompatibleImage((int)(wRect.getWidth()*expPpd), (int)(wRect.getHeight()*expPpd), Transparency.TRANSLUCENT);
							
							BufferedImage bi = view3d.createDataImage(wRect, po, expPpd, scaleFactor);
							
							
							//draw the layer data on the final image
							// with the proper alpha value
							if(bi != null){
								((Graphics2D)finalLViewImage.getGraphics()).drawImage(bi, cmo.forAlpha(lview.getAlpha()), 0, 0);
							}
							
							// TODO: Get these from updated UI
							boolean globalFlag = false;
							boolean toggleState = vrLayer.on;
							
							File sceneImageFile = Files.createTempFile("sceneImage_"+lview.getName().replace("/", "").replace(" ",""), ".png").toFile();
							ImageIO.write(finalLViewImage, "png", sceneImageFile);
							
//							System.out.println("sceneImageFile = " + sceneImageFile);
							
							String viewName = LManager.getLManager().getUniqueName(lview);
							VRScene.Layer thisLayer = vrScene.createSceneLayer(viewName, sceneImageFile, globalFlag, toggleState);
							
		//					thisLayer.createLayerData(numericFlag, numericImg, textData);
		//					thisLayer.createTimeSliderEntry(image, index);
		//					thisLayer.createTimeSliderEntry(image, index);
							
	//						if (lview instanceof NomenclatureLView) {
	//							continue;//let's ignore nomenclature. We will handle it later.
		//						
		////						// TODO: This is only IF this is a Nomenclature LView
		////						VRScene.Layer layer2 = vrScene.createSceneLayer("Nomenclature", null, false, true);
		//						// TODO: Use existing code to create nomenclature data
		////						JSONArray nomenData = new JSONArray();
		////						thisLayer.createNomenclatureData(nomenData);
	//						}
							
							if (lview instanceof MapLView) {
								MapSource numSources[] = ((MapLView)lview).getNumericSources();
								
								for (int i=0; i<numSources.length; i++) {
						    		MapChannel newChannel=new MapChannel();
	
						    		// Add it to our list, so we know when all requests are done
						    		numericChannel.add(newChannel);
						    		
						    		newChannel.addReceiver(new MapChannelReceiver() {
										public void mapChanged(MapData mapData) {
											if (mapData.isFinished()) {
												numericData.put(newChannel, mapData);
											}
										}
									});    		
						    		
//						    		System.out.println("Layer: "+numSources[i].getMapAttr().toString());
									File numericFile = Files.createTempFile("numericFile_"+numSources[i].getName().replace("/", "").replace(" ",""), ".tif").toFile();
									numericFiles.put(newChannel, numericFile);  // Save a reference so we can write it out later
									
									
									LayerData ld = thisLayer.createNumericLayerData(numericFile, numSources[i].getUnits(), numSources[i].getTitle());
									layerDataObjs.put(newChannel, ld);
									
//						    		System.err.println("Starting numeric data request " + i + " for " + lview.getName());
									newChannel.setRequest(new MapRequest(numSources[i], wRect, expPpd, po));
								}
								
							}
						}//end non-nomenclature layers
					}
					
					//TODO: need to populate any text data sources (other than nomenclature);
					
					
					//let's populate nomenclature separately
					
					//end nomenclature

					//create the depth image from the depthData 
					int waitTime = 0;
					int pause = 250;
					//two minute limit
					int limit = 2*60*1000;
					while(true){
						boolean done =  true;
						for (MapChannel channel : numericChannel) {
							if (numericData.get(channel)==null) {
								done=false;
								break;
							}
						}
						if (depthData==null) {
							done=false;
						}
						// Only break when ALL of our numeric data sources AND the depthData are complete
						if (done) break;
						
						synchronized(this){
							this.wait(pause);
							waitTime+=pause;
						}
						if(waitTime>=limit){
							Util.showMessageDialog("Error retreiving elevation data for VR high res export, cancelling export. Waittime exceeds "+limit+"ms", "Elevation Export Failed", JOptionPane.ERROR_MESSAGE);
							//do not proceed to write image files or send to backend
							return;
						}
					}
					

					for (MapChannel channel : numericChannel) {
						MapData data = numericData.get(channel);
						File numericFile = numericFiles.get(channel);

//						System.err.println("Writing: " + numericFile);
												
						ImageIO.write(data.getImage(), "tif", numericFile);
						LayerData ld = layerDataObjs.get(channel);
						ld.dataType = data.getImage().getSampleModel().getDataType();
					}

//					System.out.println("Elevation: "+threeDLayer.getElevationSource().getMapAttr().getDataType());
					log.aprintln("Elevation finished, total wait time: "+waitTime+"ms");
					
					//convert the mapdata to a grayscale numeric image
//					Elevation ev = new Elevation(depthData.getImage().getRaster(), myLayer.getElevationSource().getIgnoreValue());					
//					BufferedImage depth = canvasPanel.canvas.getDepthImage(ev);
					BufferedImage depth = depthData.getImage();

					vrScene.setElevationDepthType(depthData.getImage().getSampleModel().getDataType());
					
					ImageIO.write(depth, "tif", depthImageFile);

					String result = VR_BackendInterface.createVRScene(vrScene);
					JSONObject obj = new JSONObject(result);
					String accessKey = obj.getString("access_key");
					String sceneKey = obj.getString("scene_key");
					SwingUtilities.invokeLater(new Runnable() {
						
						@Override
						public void run() {
							try {
								Integer key = Integer.decode(sceneKey);
								VRScene newScene = new VRScene(key);
								newScene.setAccessKey(accessKey);
								modalDialog.setVisible(false);
								modalDialog.dispose();
								showSceneDetails(newScene);
								selectedScene = newScene;
								mainNavPanel.add(buildNavEntry(newScene),0);
								mainNavPanel.revalidate();
								mainNavPanel.repaint();
							} catch (NumberFormatException nfe) {
								Util.showMessageDialog("There was an error creating this scene");
							}
							
						}
					});

				} catch (Exception ex) {
					ex.printStackTrace();
//					generateQRResultMessage.setText("Could not export data");
					throw new RuntimeException(ex);
				}
				//dispose of depthData so we're not using memory, and so 
				// it properly re-pulls the data next time through
				depthData = null;
				
//				System.err.println("Done!");
			}
		});
		
		manager.start();
		
	}
		
	
	private void getScenes() {
		allScenes = VR_BackendInterface.viewAllScenes();
	}
	private JPanel getCreateDialogPanel() {
		elevationSource = threeDLayer.getElevationSource();
		zoomManager = Main.testDriver.mainWindow.getZoomManager();
		curPpd = zoomManager.getZoomPPD();
		Vector<Integer> ppds = new Vector<Integer>(Arrays.asList(zoomManager.getExportZoomFactorsWithCurrent()));
		ppdCB = new JComboBox<Integer>(ppds);
		ppdCB.setSelectedItem(curPpd);
		
		vrLayerSurfaceList.clear();
		surfaceModel.clear();
		globeModel.clear();
		sceneNameTF.setText("");
		startingPointTF.setText("0,0");
//		verticalExaggerationTF.setText("");

		ppdLbl.setText(String.valueOf(ppdCB.getSelectedItem()));
		elevationSourceLbl.setText(elevationSource.getName());
		
		LViewManager viewMan = Main.testDriver.mainWindow;
		LManager lManager = LManager.getLManager();
		final List<LView> views = new ArrayList<LView>(viewMan.viewList);
		Collections.reverse(views);
		for (LView view: views) {
			if (view instanceof ThreeDLView || !view.isAlive()) {
				continue;
			}
			String viewName = lManager.getUniqueName(view);
			VRLayer vrLayer = new VRLayer(view, viewName, view.isAlive(), true);
			vrLayerSurfaceList.add(vrLayer);
			surfaceModel.addElement(createLayerRow(vrLayer));
		    
		}
		
		saveSceneBtn = new JButton(saveSceneAction);
		cancelNewSceneBtn = new JButton(cancelAction);
		
		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		JPanel leftPnl = layoutModalLeftPanel();
		JPanel rightPnl = layoutRightPanel(false);
		
//		JLabel ppdSelLbl = new JLabel("Select PPD: ");
//		JLabel elevationNameLbl = new JLabel("Elevation Source: ");
//		JLabel elevationSrcLbl = new JLabel(threeDLayer.getElevationSource().getName());
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
			.addGroup(layout.createSequentialGroup()
				.addComponent(leftPnl)
				.addComponent(rightPnl))
			.addGroup(layout.createSequentialGroup()
				.addComponent(cancelNewSceneBtn)
				.addComponent(saveSceneBtn)));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(leftPnl)
				.addComponent(rightPnl))
			.addGap(10)
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(cancelNewSceneBtn)
				.addComponent(saveSceneBtn)));
		
		return panel;
	}
	private void setupUI() {
//		dialog = new JDialog(Main.mainFrame);
		mainPanel = new JPanel();
		sceneDetailsPanel = layoutSceneDetailsPanel();
		initialPanel = layoutInitialPanel();
		
		
		JPanel navPanel = layoutNavPanel();
		JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
		
		mainLayout = new GroupLayout(mainPanel);
		mainPanel.setLayout(mainLayout);
		mainLayout.setAutoCreateGaps(false);
		mainLayout.setAutoCreateContainerGaps(true);
		
		mainLayout.setHorizontalGroup(mainLayout.createSequentialGroup()
			.addComponent(navPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addComponent(sep)
			.addGap(20)
			.addComponent(initialPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE));
		mainLayout.setVerticalGroup(mainLayout.createParallelGroup(Alignment.CENTER)
			.addComponent(navPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
			.addComponent(sep, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
			.addComponent(initialPanel));
		

		mainPanel.setBorder(BorderFactory.createLineBorder(inputBGColor));
		navPanel.setPreferredSize(new Dimension(navPanel.getPreferredSize().width, initialPanel.getPreferredSize().height));
//		mainPanel.setSize()
//		initialPanel.setPreferredSize(sceneDetailsPanel.getPreferredSize());
//		mainPanel.setSize(mainPanel.getPreferredSize());
//		dialog.getContentPane().add(mainPanel);
//		dialog.setTitle("VR/XR Manager");
//		dialog.setLocationRelativeTo(null);
//		dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
//		dialog.pack();
//		dialog.setVisible(true);
		
	}
	private JPanel layoutInitialPanel() {
		JPanel footerPnl = layoutBlankFooterPanel();
		JPanel topPnl = new JPanel(new BorderLayout());
		JPanel panel = new JPanel();
		
		JLabel title = new JLabel("Virtual Reality Scene Manager");
		JLabel inst1 = new JLabel("Click \"ADD A SCENE\" below or the \"Scene\" button to left to create a new VR scene.");
		topPnl.add(title, BorderLayout.NORTH);
		topPnl.add(inst1, BorderLayout.CENTER);
//		GroupLayout topLayout = new GroupLayout(topPnl);
//		topPnl.setLayout(topLayout);
//		topLayout.setAutoCreateContainerGaps(true);
//		topLayout.setAutoCreateGaps(true);
//		
//		topLayout.setHorizontalGroup(topLayout.createParallelGroup(Alignment.CENTER)
//			.addComponent(title)
//			.addComponent(inst1));
//		topLayout.setVerticalGroup(topLayout.createSequentialGroup()
//			.addComponent(title)
//			.addComponent(inst1));
		
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
			.addComponent(topPnl)
			.addComponent(footerPnl));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(topPnl)
			.addComponent(footerPnl, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE));
		
//		panel.setPreferredSize(sceneDetailsPanel.getPreferredSize());
		return panel;
	}
	private JPanel layoutSceneDetailsPanel() {
		JPanel panel = new JPanel();
		JPanel leftPanel = layoutLeftPanel();
		JPanel rightPanel = layoutRightPanel(true);
		JPanel footerPanel = layoutReadOnlyFooterPanel();
		
		GroupLayout sceneLayout = new GroupLayout(panel);
		panel.setLayout(sceneLayout);
		
		sceneLayout.setHorizontalGroup(sceneLayout.createParallelGroup(Alignment.CENTER)
			.addGroup(sceneLayout.createSequentialGroup()
				.addComponent(leftPanel)
				.addComponent(rightPanel))
			.addComponent(footerPanel));
		sceneLayout.setVerticalGroup(sceneLayout.createSequentialGroup()
			.addGroup(sceneLayout.createParallelGroup(Alignment.LEADING)
				.addComponent(leftPanel)
				.addComponent(rightPanel))
			.addComponent(footerPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE));
		
		return panel;
		
	}

	private void setupNewScene() {
		
		mainLayout.replace(initialPanel, sceneDetailsPanel);
		
	}
	private JPanel layoutNavPanel() {
		JPanel navPanel = new JPanel();
		GroupLayout layout = new GroupLayout(navPanel);
		navPanel.setLayout(layout);
		layout.setAutoCreateContainerGaps(false);
		layout.setAutoCreateGaps(false);
		
		scenesBtn = new JButton(addSceneAction);
		scenesBtn.setIcon(newLayerIcon);		
		scenesBtn.setUI(new LikeDefaultButtonUI());
		scenesBtn.setHorizontalTextPosition(SwingConstants.LEFT);
		scenesBtn.setHorizontalAlignment(SwingConstants.RIGHT);
		scenesBtn.setIconTextGap(300);

		mainNavPanel = new JPanel();
		mainNavPanel.setLayout(new BoxLayout(mainNavPanel, BoxLayout.PAGE_AXIS));
		for(VRScene scene : allScenes) {
			mainNavPanel.add(buildNavEntry(scene));
		}
		JScrollPane sp = new JScrollPane(mainNavPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.getVerticalScrollBar().setBackground(panelColor);
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
			.addComponent(scenesBtn, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
			.addComponent(sp));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(scenesBtn, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addComponent(sp));
		
		return navPanel;
	}
	private JPanel buildNavEntry(VRScene scene) {
		int scrollBarWidth = ((Integer) UIManager.get("ScrollBar.width")).intValue();
		Dimension dim = scenesBtn.getPreferredSize();
		dim = new Dimension(dim.width - scrollBarWidth, dim.height - 20);
		GridBagConstraints constraints1 = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 10, 0), 0, 0);
		GridBagConstraints constraints2 = new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 0, 10, 5), 0, 0);
		JPanel rowPanel = new JPanel(new BorderLayout());
		JPanel panel1 = new JPanel(new GridBagLayout());
		JPanel panel2 = new JPanel(new GridBagLayout());
		panel1.setBackground(inputBGColor);
		panel2.setBackground(inputBGColor);
		JLabel sceneLbl = new JLabel(scene.getSceneName());
		sceneLbl.setBackground(inputBGColor);
		sceneLbl.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				selectedScene = scene;
				showSceneDetails(scene);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				sceneLbl.setCursor(java.awt.Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				sceneLbl.setCursor(java.awt.Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		});
		
		JLabel ellipsisLbl = new JLabel(ellipsis);
		ellipsisLbl.setBackground(inputBGColor);
		ellipsisLbl.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				JPopupMenu menu = new JPopupMenu();
				JMenuItem detailsItem = new JMenuItem(new AbstractAction("Show Details") {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						selectedScene = scene;
						showSceneDetails(scene);
					}
				});
				JMenuItem deleteItem = new JMenuItem(new AbstractAction("Delete") {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						VR_BackendInterface.removeScene(scene.getSceneKey());
						allScenes.remove(scene);
						mainNavPanel.remove(rowPanel);
						mainNavPanel.revalidate();
						mainNavPanel.repaint();
					}
				});
				menu.add(detailsItem);
				menu.add(deleteItem);
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
			
		});
		panel1.add(sceneLbl, constraints1);
		panel2.add(ellipsisLbl, constraints2);
		rowPanel.add(panel1, BorderLayout.CENTER);
		rowPanel.add(panel2, BorderLayout.EAST);
		rowPanel.setPreferredSize(dim);
		rowPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, panelColor));
		return rowPanel;
	}
	private void showSceneDetails(VRScene scene) {
		vrLayerSurfaceList.clear();
		surfaceModel.clear();
		globeModel.clear();
	
//		if (!scene.getDataUpdated()) {
		VR_BackendInterface.updateData(scene);
//		}
		updateSceneNameTF.setText(scene.getSceneName());
		if (scene.getStartingPoint() != null && !scene.getStartingPoint().equals("null")) {
			updateStartingPointTF.setText(scene.getStartingPoint());
		} else {
			updateStartingPointTF.setText("0,0");
		}
//		String exag = scene.getExaggeration();
//		if (exag.indexOf(",") > 0) {
//			exag = exag.substring(exag.indexOf(",")+1);
//		}
//		updateVerticalExaggerationTF.setText(exag);
		if (zoomComponent instanceof JComboBox) {
			zoomComponent = zoomLabel;
		}
		((JLabel)zoomComponent).setText(String.valueOf(scene.getPpd()));
		ArrayList<Layer> layers = scene.getLayers();
		for(Layer layer : layers) {
			VRLayer vrLayer = new VRLayer(null, layer.name, layer.toggleState, !layer.globalFlag);
			createLayerRow(vrLayer);
			vrLayerSurfaceList.add(vrLayer);
			surfaceModel.addElement(createLayerRow(vrLayer));
		}
		
		if (initialPanel.isShowing() && initialPanel.isVisible()) {
			mainLayout.replace(initialPanel, sceneDetailsPanel);
		}
		sceneDetailsPanel.revalidate();
		sceneDetailsPanel.repaint();
		
		
	}
	private JPanel layoutBlankFooterPanel() {
		JPanel footerPnl = new JPanel(new BorderLayout());
		JButton addSceneBtn = new JButton(addSceneAction);
		
		JPanel btnPnl1 = new JPanel(new GridBagLayout());
		
		btnPnl1.add(addSceneBtn, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		
		footerPnl.add(btnPnl1, BorderLayout.EAST);
//		btnPnl1.setBackground(inputBGColor);
//		footerPnl.setBackground(inputBGColor);
		return footerPnl;
		
	}
	private JPanel layoutReadOnlyFooterPanel() {
		JPanel footerPnl = new JPanel(new BorderLayout());
		qrBtn = new JButton("VIEW QR CODE", qrIcon);
		qrBtn.addActionListener(new AbstractAction() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String accessKey = selectedScene.getAccessKey();
				Util.launchBrowser("https://jmars.mars.asu.edu/vr/jmars/qr.php?access_key="+accessKey);
			}
		});
		
//		JButton saveSceneBtn = new JButton(saveSceneAction);
//		JButton cancelBtn = new JButton(cancelAction);
//		JButton closeBtn = new JButton(closeAction);
		
		JPanel btnPnl1 = new JPanel(new GridBagLayout());
//		JPanel btnPnl2 = new JPanel(new GridBagLayout());
		
		btnPnl1.add(qrBtn, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
//		btnPnl2.add(closeBtn, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
//		btnPnl2.add(cancelBtn, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
//		btnPnl2.add(saveSceneBtn, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		
		footerPnl.add(btnPnl1, BorderLayout.WEST);
//		footerPnl.add(btnPnl2, BorderLayout.EAST);
//		btnPnl1.setBackground(inputBGColor);
//		btnPnl2.setBackground(inputBGColor);
//		footerPnl.setBackground(inputBGColor);
		return footerPnl;
		
	}
	private JPanel layoutFooterPanel() {
		JPanel footerPnl = new JPanel(new BorderLayout());
		JButton qrBtn = new JButton("VIEW QR CODE", qrIcon);
		
		JButton saveSceneBtn = new JButton(saveSceneAction);
		JButton cancelBtn = new JButton(cancelAction);
//		JButton closeBtn = new JButton(closeAction);
		
		JPanel btnPnl1 = new JPanel(new GridBagLayout());
		JPanel btnPnl2 = new JPanel(new GridBagLayout());
		
		btnPnl1.add(qrBtn, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
//		btnPnl2.add(closeBtn, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		btnPnl2.add(cancelBtn, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		btnPnl2.add(saveSceneBtn, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 10, 10, 10), 0, 0));
		
		footerPnl.add(btnPnl1, BorderLayout.WEST);
		footerPnl.add(btnPnl2, BorderLayout.EAST);
//		btnPnl1.setBackground(inputBGColor);
//		btnPnl2.setBackground(inputBGColor);
//		footerPnl.setBackground(inputBGColor);
		return footerPnl;
		
	}
	private JPanel createLayerRow(VRLayer layer) {		
		JPanel panel = new JPanel(new GridBagLayout());
    	JLabel dragLbl = new JLabel(dragIcon);
    	JLabel toggleLbl = new JLabel(layer.on ? onIcon : offIcon);
    	JLabel nameLbl = new JLabel(layer.name);
		JLabel trashLbl = new JLabel(trashIcon);
		
		panel.add(dragLbl, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 10, 0), 0, 0));
		panel.add(toggleLbl, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 10, 0), 0, 0));
		panel.add(nameLbl, new GridBagConstraints(2, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 10, 0), 0, 0));
		panel.add(trashLbl, new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(10, 5, 10, 5), 0, 0));
		
		panel.setBorder(BorderFactory.createLineBorder(dragColor, 1));
		
		layer.toggleLbl = toggleLbl;
		return panel;
	}
	private void executeAction(JList<JComponent> list, boolean surface, int itemIndex, Point point) {
		if (point.x > 290) {
			((DefaultListModel<JComponent>)list.getModel()).remove(itemIndex);
			if (surface) {
				vrDeletedLayerSurfaceList.add(vrLayerSurfaceList.get(itemIndex));
				vrLayerSurfaceList.remove(itemIndex);
			} else {
				vrDeletedLayerGlobeList.add(vrLayerGlobeList.get(itemIndex));
				vrLayerGlobeList.remove(itemIndex);
			}
		} else if (point.x < 60 && point.x > 35) {
			VRLayer layer = null;
			if (surface) {
				layer = vrLayerSurfaceList.get(itemIndex);
			} else {
				layer = vrLayerGlobeList.get(itemIndex);
			}

			layer.toggle();
		}
		
	}
	class VRLayer {
		LView lview = null;
		String name = null;
		boolean on = true;
		boolean surface = true;
		JLabel toggleLbl = null;
		VRLayer(LView view, String nm, boolean isOn, boolean isSurface) {
			this.lview = view;
			this.name = nm;
			this.on = isOn;
			this.surface = isSurface;
		}
		void remove() {
			on = false;
		}
		void setActiveLbl(JLabel lbl) {
			this.toggleLbl = lbl;
		}
		void toggle() {
			if (this.on) {
				this.on = false;
				this.toggleLbl.setIcon(offIcon);
			} else {
				this.on = true;
				this.toggleLbl.setIcon(onIcon);
			}
		}
	}
	private JPanel layoutRightPanel(boolean readOnly) {

		JPanel rightPanel = new JPanel();
		//right panel of Manager
		GroupLayout layout = new GroupLayout(rightPanel);
		rightPanel.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(false);
		layout.setHonorsVisibility(false);
		
		JLabel surfaceHeader = new JLabel("Surface Layers");
		JLabel globeHeader = new JLabel("Globe Layers");
		

	    JList<JComponent> surfaceList = new JList<JComponent>(surfaceModel);
	    surfaceList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    surfaceList.setTransferHandler(new ListItemTransferHandler());
	    surfaceList.setDropMode(DropMode.INSERT);
	    surfaceList.setDragEnabled(true);
	    // https://java-swing-tips.blogspot.com/2008/10/rubber-band-selection-drag-and-drop.html
	    surfaceList.setLayoutOrientation(JList.VERTICAL);
	    surfaceList.setVisibleRowCount(3);
	    surfaceList.setFixedCellWidth(300);
	    surfaceList.setBackground(panelColor);
//	    list.setFixedCellHeight(20);
	    
	    surfaceList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent event) {
				Point point = event.getPoint();
				int index = surfaceList.locationToIndex(point);
			    executeAction(surfaceList, true, index, point);
			    surfaceList.repaint();
			}
	    	
		});
	    
	    surfaceList.setCellRenderer(new ListCellRenderer<JComponent>() {
	        @Override
	        public Component getListCellRendererComponent(JList<? extends JComponent> list, JComponent value, int index, boolean isSelected, boolean cellHasFocus) {
	        	return value;
	        }
	      });
	    
	    globeModel = new DefaultListModel<>();
	    for (VRLayer layer : vrLayerGlobeList) {
	    	globeModel.addElement(createLayerRow(layer));
	    }

	    JList<JComponent> globeList = new JList<JComponent>(globeModel);
	    globeList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	    globeList.setTransferHandler(new ListItemTransferHandler());
	    globeList.setDropMode(DropMode.INSERT);
	    globeList.setDragEnabled(true);
	    // https://java-swing-tips.blogspot.com/2008/10/rubber-band-selection-drag-and-drop.html
	    globeList.setLayoutOrientation(JList.VERTICAL);
	    globeList.setVisibleRowCount(7);
	    globeList.setFixedCellWidth(300);
	    globeList.setBackground(panelColor);
//	    globeList.setFixedCellHeight(20);
	    
	    globeList.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent event) {
				Point point = event.getPoint();
				int index = globeList.locationToIndex(point);
			    executeAction(globeList, false, index, point);
			    globeList.repaint();
			}
	    	
		});
	    globeList.setCellRenderer(new ListCellRenderer<JComponent>() {
	        @Override
	        public Component getListCellRendererComponent(JList<? extends JComponent> list, JComponent value, int index, boolean isSelected, boolean cellHasFocus) {
	        	return value;
	        }
	      });
	    
	    
	    JScrollPane surfaceSP = new JScrollPane(surfaceList);
	    surfaceSP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	    surfaceSP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
	    surfaceSP.setBackground(panelColor);

	    JScrollPane globeSP = new JScrollPane(globeList);
	    globeSP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	    globeSP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
	    globeSP.setBackground(panelColor);
	    
	    JButton addSurfaceLayerBtn = new JButton(addSurfaceLayerAction);
	    JButton addGlobeLayerBtn = new JButton(addGlobeLayerAction);
	    
	    //Temporarily hide these. Delete these lines when you want to show
	    globeSP.setVisible(false);
	    addGlobeLayerBtn.setVisible(false);
	    globeHeader.setVisible(false);
	    //end temporarily hidden section
	    if (readOnly) {
	    	addSurfaceLayerBtn.setVisible(false);
	    }
	    
	    layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
	    	.addComponent(surfaceHeader)
	    	.addGroup(layout.createParallelGroup(Alignment.TRAILING)
	    		.addComponent(surfaceSP)
	    		.addComponent(addSurfaceLayerBtn))
	    	.addComponent(globeHeader)
	    	.addGroup(layout.createParallelGroup(Alignment.TRAILING)
		    		.addComponent(globeSP)
		    		.addComponent(addGlobeLayerBtn)));
	    layout.setVerticalGroup(layout.createSequentialGroup()
		    .addComponent(surfaceHeader)
	    	.addComponent(surfaceSP)
	    	.addGap(5)
	    	.addComponent(addSurfaceLayerBtn)
	    	.addComponent(globeHeader)
	    	.addComponent(globeSP, 160, 160, 160)
	    	.addGap(5)
	    	.addComponent(addGlobeLayerBtn));
	    
	    layout.linkSize(SwingConstants.HORIZONTAL, surfaceHeader, surfaceSP, globeHeader, globeSP);

		layout.setHonorsVisibility(globeSP, true);
		return rightPanel;
	}
	

	private void toggleFields(boolean enabled) {
		sceneNameTF.setEnabled(enabled);
		startingPointTF.setEnabled(enabled);
//		verticalExaggerationTF.setEnabled(enabled);
//		if (!enabled) {
			if (ppdCB == null) {
				zoomManager = Main.testDriver.mainWindow.getZoomManager();
				Vector<Integer> ppds = new Vector<Integer>(zoomManager.getZoomFactors());
				ppdCB = new JComboBox<Integer>(ppds);
				curPpd = zoomManager.getZoomPPD();
				ppdCB.setSelectedItem(curPpd);
				ppdCB.setEnabled(false);
			}
//		}
	}
	
	private JPanel layoutLeftPanel() {

		JPanel leftPanel = new JPanel();
		//Left panel of Manager
		GroupLayout leftLayout = new GroupLayout(leftPanel);
		leftPanel.setLayout(leftLayout);
		leftLayout.setAutoCreateContainerGaps(true);
		leftLayout.setAutoCreateGaps(true);
		
		//left panel static components
		JLabel sceneNameLbl = new JLabel("SCENE NAME");
		JLabel ppdNameLbl = new JLabel("SCENE PPD");
		JLabel startingPointLbl = new JLabel("STARTING POINT");
		JLabel latLonLbl = new JLabel("LAT/LON");
		JLabel scalingLbl = new JLabel("SCALING");
		JLabel modeLbl = new JLabel("MODE");
		JLabel vertExaggerationLbl = new JLabel("VERT. EXAGGERATION");
		JLabel totalExaggerationLbl = new JLabel("TOTAL EXAGGERATION");
		JLabel sceneDataLbl = new JLabel("SCENE DATA");
		
//		scalingModeCB = new JComboBox<>(new String[] {"Mode 1", "Mode 2"});//just going to do a text value for now
		JLabel scalingValueLbl = new JLabel("Range of Values");
		//end temporary text value for now

		zoomComponent = ppdCB;//this will either be the combo box or a JLabel
		
		if (ppdCB == null) {
			if (zoomLabel == null) {
				zoomLabel = new JLabel("00000");
			}
			zoomComponent = zoomLabel;
		}
		
		leftLayout.setHorizontalGroup(leftLayout.createParallelGroup(Alignment.LEADING)
			.addComponent(sceneNameLbl)
			.addComponent(updateSceneNameTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addComponent(ppdNameLbl)
			.addComponent(zoomComponent, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addComponent(startingPointLbl)
			.addComponent(latLonLbl)
			.addComponent(updateStartingPointTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
//			.addGroup(leftLayout.createSequentialGroup()
//				.addComponent(startingPointTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
//				.addGap(11)
//				.addComponent(startingPointBtn))
//			.addComponent(scalingLbl)
//			.addGroup(leftLayout.createSequentialGroup()
//				.addGroup(leftLayout.createParallelGroup(Alignment.LEADING)
//					.addComponent(modeLbl)
////					.addComponent(scalingModeCB))
//					.addComponent(scalingValueLbl))
//				.addGroup(leftLayout.createParallelGroup(Alignment.LEADING)
//					.addComponent(vertExaggerationLbl)
//					.addComponent(updateVerticalExaggerationTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)))
//				.addGroup(leftLayout.createParallelGroup(Alignment.LEADING)
//					.addComponent(totalExaggerationLbl)
//					.addComponent(totalExaggerationTF)))
//			.addComponent(sceneDataLbl)
//			.addComponent(sceneDataDisplayLbl)
			);
		
		leftLayout.setVerticalGroup(leftLayout.createSequentialGroup()
			.addComponent(sceneNameLbl)
			.addComponent(updateSceneNameTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addGap(20)
			.addComponent(ppdNameLbl)
			.addComponent(zoomComponent, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addGap(20)
			.addComponent(startingPointLbl)
			.addGap(5)
			.addComponent(latLonLbl)
			.addGroup(leftLayout.createParallelGroup(Alignment.LEADING)
				.addComponent(updateStartingPointTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)));
//				.addComponent(startingPointBtn, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
//			.addGap(20)
//			.addComponent(scalingLbl)
//			.addGap(5)
//			.addGroup(leftLayout.createSequentialGroup()
//				.addGroup(leftLayout.createParallelGroup(Alignment.CENTER)
//					.addComponent(modeLbl)
//					.addComponent(vertExaggerationLbl))
//				.addGroup(leftLayout.createParallelGroup(Alignment.CENTER)
//					.addComponent(scalingValueLbl)
//					.addComponent(updateVerticalExaggerationTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
					
//				.addGroup(leftLayout.createSequentialGroup()
//					.addComponent(modeLbl)
//					.addComponent(scalingModeCB, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
//					.addComponent(scalingValueLbl))
//				.addGroup(leftLayout.createSequentialGroup()
//					.addComponent(vertExaggerationLbl)
//					.addComponent(verticalExaggerationTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)))
//				.addGroup(leftLayout.createSequentialGroup()
//					.addComponent(totalExaggerationLbl)
//					.addComponent(totalExaggerationTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)))
//			.addGap(20)
//			.addComponent(sceneDataLbl)
//			.addComponent(sceneDataDisplayLbl)
//			);
		
//		leftLayout.linkSize(SwingConstants.HORIZONTAL, ppdCB, scalingModeCB, startingPointTF);
//		leftLayout.linkSize(SwingConstants.HORIZONTAL, ppdLbl, startingPointTF);
		leftLayout.linkSize(SwingConstants.VERTICAL, updateSceneNameTF, updateStartingPointTF);
//		leftLayout.linkSize(SwingConstants.VERTICAL, updateSceneNameTF, updateStartingPointTF, updateVerticalExaggerationTF);
//		leftLayout.linkSize(SwingConstants.VERTICAL, updateSceneNameTF, ppdCB, elevationSourceBtn, startingPointBtn, startingPointTF, scalingModeCB, verticalExaggerationTF, totalExaggerationTF);
		return leftPanel;
	}
	private JPanel layoutModalLeftPanel() {

		JPanel leftPanel = new JPanel();
		//Left panel of Manager
		GroupLayout leftLayout = new GroupLayout(leftPanel);
		leftPanel.setLayout(leftLayout);
		leftLayout.setAutoCreateContainerGaps(true);
		leftLayout.setAutoCreateGaps(true);
		
		//left panel static components
		JLabel sceneNameLbl = new JLabel("SCENE NAME");
		JLabel ppdNameLbl = new JLabel("SCENE PPD");
		JLabel startingPointLbl = new JLabel("STARTING POINT");
		JLabel latLonLbl = new JLabel("LAT/LON");
		JLabel scalingLbl = new JLabel("SCALING");
		JLabel modeLbl = new JLabel("MODE");
		JLabel vertExaggerationLbl = new JLabel("VERT. EXAGGERATION");
		JLabel totalExaggerationLbl = new JLabel("TOTAL EXAGGERATION");
		JLabel sceneDataLbl = new JLabel("SCENE DATA");
		
		
//		scalingModeCB = new JComboBox<>(new String[] {"Mode 1", "Mode 2"});//just going to do a text value for now
		JLabel scalingValueLbl = new JLabel("Range of Values");
		//end temporary text value for now

//		zoomComponent = ppdCB;//this will either be the combo box or a JLabel
//		
//		if (ppdCB == null) {
//			if (zoomLabel == null) {
//				zoomLabel = new JLabel("00000");
//			}
//			zoomComponent = zoomLabel;
//		}
		
		leftLayout.setHorizontalGroup(leftLayout.createParallelGroup(Alignment.LEADING)
			.addComponent(sceneNameLbl)
			.addComponent(sceneNameTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addGroup(leftLayout.createSequentialGroup()
				.addGroup(leftLayout.createParallelGroup(Alignment.LEADING)
					.addComponent(ppdNameLbl)
					.addComponent(ppdCB, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGroup(leftLayout.createParallelGroup(Alignment.LEADING)
					.addComponent(elevationSourceLbl)))
			.addComponent(startingPointLbl)
			.addComponent(latLonLbl)
			.addComponent(startingPointTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
//			.addGroup(leftLayout.createSequentialGroup()
//				.addComponent(startingPointTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
//				.addGap(11)
//				.addComponent(startingPointBtn))
//			.addComponent(scalingLbl)
//			.addGroup(leftLayout.createSequentialGroup()
//				.addGroup(leftLayout.createParallelGroup(Alignment.LEADING)
//					.addComponent(modeLbl)
////					.addComponent(scalingModeCB))
//					.addComponent(scalingValueLbl))
//				.addGroup(leftLayout.createParallelGroup(Alignment.LEADING)
//					.addComponent(vertExaggerationLbl)
//					.addComponent(verticalExaggerationTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)))
//				.addGroup(leftLayout.createParallelGroup(Alignment.LEADING)
//					.addComponent(totalExaggerationLbl)
//					.addComponent(totalExaggerationTF)))
//			.addComponent(sceneDataLbl)
//			.addComponent(sceneDataDisplayLbl)
			);
		
		leftLayout.setVerticalGroup(leftLayout.createSequentialGroup()
			.addComponent(sceneNameLbl)
			.addComponent(sceneNameTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addGap(20)
			.addComponent(ppdNameLbl)
			.addGroup(leftLayout.createParallelGroup(Alignment.LEADING)
				.addComponent(ppdCB, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(elevationSourceLbl, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
			.addGap(20)
			.addComponent(startingPointLbl)
			.addGap(5)
			.addComponent(latLonLbl)
			.addGroup(leftLayout.createParallelGroup(Alignment.LEADING)
				.addComponent(startingPointTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
//				.addComponent(startingPointBtn, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
//			.addGap(20)
//			.addComponent(scalingLbl)
//			.addGap(5)
//			.addGroup(leftLayout.createSequentialGroup()
//				.addGroup(leftLayout.createParallelGroup(Alignment.CENTER)
//					.addComponent(modeLbl)
//					.addComponent(vertExaggerationLbl))
//				.addGroup(leftLayout.createParallelGroup(Alignment.CENTER)
//					.addComponent(scalingValueLbl)
//					.addComponent(verticalExaggerationTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)))
//					
//				.addGroup(leftLayout.createSequentialGroup()
//					.addComponent(modeLbl)
//					.addComponent(scalingModeCB, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
//					.addComponent(scalingValueLbl))
//				.addGroup(leftLayout.createSequentialGroup()
//					.addComponent(vertExaggerationLbl)
//					.addComponent(verticalExaggerationTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)))
//				.addGroup(leftLayout.createSequentialGroup()
//					.addComponent(totalExaggerationLbl)
//					.addComponent(totalExaggerationTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)))
//			.addGap(20)
//			.addComponent(sceneDataLbl)
//			.addComponent(sceneDataDisplayLbl)
			);
		
//		leftLayout.linkSize(SwingConstants.HORIZONTAL, ppdCB, scalingModeCB, startingPointTF);
//		leftLayout.linkSize(SwingConstants.HORIZONTAL, ppdLbl, startingPointTF);
//		leftLayout.linkSize(SwingConstants.VERTICAL, sceneNameTF, startingPointTF, verticalExaggerationTF);
		leftLayout.linkSize(SwingConstants.VERTICAL, sceneNameTF, startingPointTF);
//		leftLayout.linkSize(SwingConstants.VERTICAL, sceneNameTF, ppdCB, elevationSourceBtn, startingPointBtn, startingPointTF, scalingModeCB, verticalExaggerationTF, totalExaggerationTF);
		return leftPanel;
	}
	private AbstractAction startingPointAction = new AbstractAction("CHANGE STARTING POINT") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			
		}
	};
	private AbstractAction selectElevationSourceAction = new AbstractAction("SELECT ELEVATION SOURCE") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			
		}
	};
	
	private AbstractAction addSurfaceLayerAction = new AbstractAction("ADD LAYER") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			vrAddedLayerSurfaceList.clear();
			if (vrDeletedLayerSurfaceList.size() == 0) {
				Util.showMessageDialog("There are no layers to add.");
			} else {
				JPanel panel = new JPanel();
				BoxLayout box = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
				panel.setLayout(box);
				for (VRLayer layer : vrDeletedLayerSurfaceList) {
					JCheckBox check = new JCheckBox(new AbstractAction(layer.name) {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							vrAddedLayerSurfaceList.add(layer);
						}
					});						
					panel.add(check);
				}
				int option = Util.showOptionDialog(panel, "Add Layer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
				if (option == JOptionPane.OK_OPTION) {
					for (VRLayer vrLayer : vrAddedLayerSurfaceList) {
						vrDeletedLayerSurfaceList.remove(vrLayer);
						vrLayerSurfaceList.add(vrLayer);
						surfaceModel.addElement(createLayerRow(vrLayer));
					}
					vrAddedLayerSurfaceList.clear();
				}
			}	
		}
	};
	private AbstractAction addGlobeLayerAction = new AbstractAction("ADD LAYER") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			vrAddedLayerGlobeList.clear();
			if (vrDeletedLayerGlobeList.size() == 0) {
				Util.showMessageDialog("There are no layers to add.");
			} else {
				JPanel panel = new JPanel();
				BoxLayout box = new BoxLayout(panel, BoxLayout.PAGE_AXIS);
				panel.setLayout(box);
				for (VRLayer layer : vrDeletedLayerGlobeList) {
					JCheckBox check = new JCheckBox(new AbstractAction(layer.name) {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							vrAddedLayerGlobeList.add(layer);
						}
					});						
					panel.add(check);
				}
				int option = Util.showOptionDialog(panel, "Add Layer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
				if (option == JOptionPane.OK_OPTION) {
					for (VRLayer vrLayer : vrAddedLayerGlobeList) {
						vrDeletedLayerGlobeList.remove(vrLayer);
						vrLayerGlobeList.add(vrLayer);
						globeModel.addElement(createLayerRow(vrLayer));
					}
					vrAddedLayerGlobeList.clear();
				}
			}
		}
	};
	private AbstractAction qrAction = new AbstractAction() {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			
		}
	};
	private AbstractAction saveSceneAction = new AbstractAction("SAVE SCENE") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			saveScene();
		}
	};
	
	private AbstractAction addSceneAction = new AbstractAction("ADD A SCENE") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			
			JPanel panel = getCreateDialogPanel();
			
			modalDialog = new JDialog(focusPanel.parentFrame);
			modalDialog.setModal(true);
			modalDialog.setContentPane(panel);
			modalDialog.pack();
			modalDialog.setLocationRelativeTo(focusPanel.parentFrame);
			modalDialog.setTitle("New Scene Options");
			modalDialog.setVisible(true);
			
		}
	};
//	private AbstractAction sceneButtonAction = new AbstractAction("SCENES") {
//		
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//			if (initialPanel.isVisible() && initialPanel.isShowing()) {
//				
//			}
//		}
//	};
	private AbstractAction cancelAction = new AbstractAction("CANCEL") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			modalDialog.setVisible(false);
			modalDialog.dispose();
			if (selectedScene != null) {
				showSceneDetails(selectedScene);
			}
		}
	};
	private AbstractAction closeAction = new AbstractAction("CLOSE") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			dialog.setVisible(false);
		}
	};
	

	
	class ListItemTransferHandler extends TransferHandler {
		  protected final DataFlavor localObjectFlavor;
		  protected int[] indices;
		  protected int addIndex = -1; // Location where items were added
		  protected int addCount; // Number of items added.
		  JList sourceList = null;

		  public ListItemTransferHandler() {
		    super();
		    // localObjectFlavor = new ActivationDataFlavor(
		    //   Object[].class, DataFlavor.javaJVMLocalObjectMimeType, "Array of items");
		    localObjectFlavor = new DataFlavor(Object[].class, "Array of items");
		  }

		  @Override
		  protected Transferable createTransferable(JComponent c) {
		    JList<?> source = (JList<?>) c;
		    sourceList = source;
		    c.getRootPane().getGlassPane().setVisible(true);

		    indices = source.getSelectedIndices();
		    Object[] transferedObjects = source.getSelectedValuesList().toArray(new Object[0]);
		    // return new DataHandler(transferedObjects, localObjectFlavor.getMimeType());
		    return new Transferable() {
		      @Override public DataFlavor[] getTransferDataFlavors() {
		        return new DataFlavor[] {localObjectFlavor};
		      }
		      @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
		        return Objects.equals(localObjectFlavor, flavor);
		      }
		      @Override public Object getTransferData(DataFlavor flavor)
		            throws UnsupportedFlavorException, IOException {
		        if (isDataFlavorSupported(flavor)) {
		          return transferedObjects;
		        } else {
		          throw new UnsupportedFlavorException(flavor);
		        }
		      }
		    };
		  }
		  @Override
		  public boolean canImport(TransferSupport info) {
		    return info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);
		  }

		  @Override
		  public int getSourceActions(JComponent c) {
		    Component glassPane = c.getRootPane().getGlassPane();
		    glassPane.setCursor(DragSource.DefaultMoveDrop);
		    return MOVE; // COPY_OR_MOVE;
		  }

		  @SuppressWarnings("unchecked")
		  @Override
		  public boolean importData(TransferSupport info) {
			  if (info.getComponent() != sourceList) {
				  //trying to drag to a different list
				  return false;
			  }
		    TransferHandler.DropLocation tdl = info.getDropLocation();
		    if (!canImport(info) || !(tdl instanceof JList.DropLocation)) {
		      return false;
		    }

		    JList.DropLocation dl = (JList.DropLocation) tdl;
		    JList target = (JList) info.getComponent();
		    DefaultListModel listModel = (DefaultListModel) target.getModel();
		    int max = listModel.getSize();
		    int index = dl.getIndex();
		    index = index < 0 ? max : index; // If it is out of range, it is appended to the end
		    index = Math.min(index, max);

		    addIndex = index;

		    try {
		      Object[] values = (Object[]) info.getTransferable().getTransferData(localObjectFlavor);
		      for (int i = 0; i < values.length; i++) {
		        int idx = index++;
		        listModel.add(idx, values[i]);
		        target.addSelectionInterval(idx, idx);
		      }
		      addCount = values.length;
		      return true;
		    } catch (UnsupportedFlavorException | IOException ex) {
		      ex.printStackTrace();
		    }

		    return false;
		  }

		  @Override
		  protected void exportDone(JComponent c, Transferable data, int action) {
		    c.getRootPane().getGlassPane().setVisible(false);
		    cleanup(c, action == MOVE);
		  }

		  private void cleanup(JComponent c, boolean remove) {
		    if (remove && Objects.nonNull(indices)) {
		      if (addCount > 0) {
		        // https://github.com/aterai/java-swing-tips/blob/master/DragSelectDropReordering/src/java/example/MainPanel.java
		        for (int i = 0; i < indices.length; i++) {
		          if (indices[i] >= addIndex) {
		            indices[i] += addCount;
		          }
		        }
		      }
		      JList source = (JList) c;
		      DefaultListModel model = (DefaultListModel) source.getModel();
		      for (int i = indices.length - 1; i >= 0; i--) {
		        model.remove(indices[i]);
		      }
		    }

		    indices = null;
		    addCount = 0;
		    addIndex = -1;
		  }
		}
}
