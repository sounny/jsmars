package edu.asu.jmars.layer.map2;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.MAP_LAYER_IMG;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.Raster;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.map2.msd.MapSettingsDialog;
import edu.asu.jmars.layer.map2.msd.PipelineModel;
import edu.asu.jmars.layer.map2.msd.WrappedMapSource;
import edu.asu.jmars.layer.map2.stages.BandExtractorStageSettings;
import edu.asu.jmars.layer.map2.stages.ContourStageSettings;
import edu.asu.jmars.layer.map2.stages.SigmaStretchStage;
import edu.asu.jmars.layer.map2.stages.SigmaStretchStageSettings;
import edu.asu.jmars.layer.map2.stages.composite.BandAggregatorSettings;
import edu.asu.jmars.layer.map2.stages.composite.CompositeStage;
import edu.asu.jmars.layer.map2.stages.composite.SingleCompositeSettings;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

public class MapLViewFactory extends LViewFactory {
	private DebugLog log = DebugLog.instance();
	static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(MAP_LAYER_IMG
		    .withDisplayColor(imgLayerColor)));			
	
	protected static void error(final String msg, final String title) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Util.showMessageDialog(
					msg, title,
					JOptionPane.ERROR_MESSAGE);
			}
		});
	}
	
	public MapLViewFactory(){
		type = "map";
	}
	
	
	/**
	 * Creates an empty map layer and returns it immediately. When the map
	 * servers have been loaded, the default map source is found or an error is
	 * shown. When the default map source has resolved, the empty map layer that
	 * was returned is updated or an error is shown.
	 */
	public LView createLView() {
		final MapLayer layer = createLayer();
		
		final MapAttrReceiver onMapResolved = new MapAttrReceiver() {
			public void receive(MapAttr attr) {
				if (attr.isFailed()) {
					error("Map server doesn't seem to have the default map", "Error loading default map!");
				} else try {
					setViewSource(layer.mapSettingsDialog, MapServerFactory.getDefaultMapSource());
					
					
					MapSource plotSource = MapServerFactory.getDefaultPlotSource();
					
					if (plotSource!=null) {	
						Pipeline pipeline[] = new Pipeline[1];
						
						pipeline[0]=new Pipeline(plotSource, new Stage[]{(CompositeStage)(new BandAggregatorSettings(1)).createStage()});
					
						try {
							(layer.mapSettingsDialog).getChartPipelineModel().setFromPipeline(
									pipeline, (CompositeStage)(new BandAggregatorSettings(1)).createStage());
							layer.mapSettingsDialog.firePipelineEvent();
						} catch (Exception e) {
							Util.showMessageDialog("Exception: "+e.toString()+" creating layer for "+plotSource.toString(),
							"Error creating layer for "+plotSource.toString()+"!", JOptionPane.ERROR_MESSAGE);				
						}
					}
					
					layer.focusPanel.infoPanel.loadFields();
					layer.focusPanel.setSelectedIndex(layer.focusPanel.selectedIndex);
				} catch (Exception e) {
					log.aprintln("Failed to update default map layer with default map");
					log.aprintln(e);
					error("Failed to update default map layer with default map", "Error loading default map!");
				}
			}
		};
		
		final MapLView3D view3d = new MapLView3D(layer);
		
		final Runnable onServersLoaded = new Runnable() {
			public void run() {
				MapSource source = MapServerFactory.getDefaultMapSource();
				if (source != null) {
					source.getMapAttr(onMapResolved);
				} else {
					error(MessageFormat.format(
							"Unable to find default map named ''{0}'' in server named ''{1}''",
							MapSourceDefault.DEFAULT_NAME, MapServerDefault.DEFAULT_NAME),
						"Error loading default map!");
				}
			}
		};
		
		MapLView lview = new MapLView(layer, true, view3d);
		lview.originatingFactory = this;
		MapServerFactory.whenMapServersReady(onServersLoaded);
		return lview;
	}
	
	/** Creates the main view by prompting the user to enter settings */
	public void createLView(boolean async) {
		try {
			MapLayer mapLayer = createLayer();
			mapLayer.mapSettingsDialog.getViewPipelineModel().setCompStage(
					CompStageFactory.instance().getStageByName(SingleCompositeSettings.stageName));
			mapLayer.mapSettingsDialog.dialog.setLocationRelativeTo(Main.mainFrame);
			mapLayer.mapSettingsDialog.dialog.setVisible(true);
			if (mapLayer.mapSettingsDialog.isOkay()) {
				// TODO: This is not the right way of propagating the pipeline from a prebuilt dialog.
				MapLView3D view3d = new MapLView3D(mapLayer);
				MapLView lview = new MapLView(mapLayer, true, view3d);
				lview.originatingFactory = this;
				mapLayer.mapSettingsDialog.firePipelineEvent();
				LManager.receiveNewLView(lview);
			}
		} catch(Exception ex) {
			error(ex.getMessage(), "Error creating "+getName()+".");
			log.aprintln(ex);
		}
	}
	
	/** 
	 * Creates the main view by prompting the user to enter settings
	 * When calling from a window other than the main JMARS window
	 * (such as AddLayer), pass that window in as the context to ensure 
	 * proper window layering.
	 * */
	public void createLView(boolean async, JFrame context) {
		try {
			MapLayer mapLayer = createLayer(context);
			mapLayer.mapSettingsDialog.getViewPipelineModel().setCompStage(
					CompStageFactory.instance().getStageByName(SingleCompositeSettings.stageName));
			mapLayer.mapSettingsDialog.dialog.setLocationRelativeTo(context);
			mapLayer.mapSettingsDialog.dialog.setVisible(true);
			if (mapLayer.mapSettingsDialog.isOkay()) {
				// TODO: This is not the right way of propagating the pipeline from a prebuilt dialog.
				MapLView3D lview3d = new MapLView3D(mapLayer);
				MapLView lview = new MapLView(mapLayer, true, lview3d);
				lview.originatingFactory = this;
				mapLayer.mapSettingsDialog.firePipelineEvent();
				LManager.receiveNewLView(lview);
			}
		} catch(Exception ex) {
			error(ex.getMessage(), "Error creating "+getName()+".");
			log.aprintln(ex);
		}
	}
	/** 
     * Creates the main view by prompting the user to enter settings
     * When calling from a window other than the main JMARS window
     * (such as AddLayer), pass that window in as the context to ensure 
     * proper window layering.
     * */
    public void createLView(boolean async, JDialog context) {
        try {
            MapLayer mapLayer = createLayer(context);
            mapLayer.mapSettingsDialog.getViewPipelineModel().setCompStage(
                    CompStageFactory.instance().getStageByName(SingleCompositeSettings.stageName));
            mapLayer.mapSettingsDialog.dialog.setLocationRelativeTo(context);
            mapLayer.mapSettingsDialog.dialog.setVisible(true);
            if (mapLayer.mapSettingsDialog.isOkay()) {
                // TODO: This is not the right way of propagating the pipeline from a prebuilt dialog.
                MapLView3D lview3d = new MapLView3D(mapLayer);
                MapLView lview = new MapLView(mapLayer, true, lview3d);
                lview.originatingFactory = this;
                mapLayer.mapSettingsDialog.firePipelineEvent();
                LManager.receiveNewLView(lview);
            }
        } catch(Exception ex) {
            error(ex.getMessage(), "Error creating "+getName()+".");
            log.aprintln(ex);
        }
    }
	
	/** Update the given model with fresh map source objects */
	protected void fixSources(PipelineModel model) throws IllegalStateException {
		for (int i = 0; i < model.getSourceCount(); i++) {
			WrappedMapSource wrapper = model.getSource(i);
			MapSource sessionSource = wrapper.getWrappedSource();
			MapServer sessionServer = sessionSource.getServer();
			// live server will be changed to a server in the factory if it can
			// be found, otherwise the session server will be preserved
			MapServer liveServer = MapServerFactory.getServerByName(sessionServer.getName());
			if (liveServer == null) {
				liveServer = sessionServer;
			}
			// source must be found in the live server
			MapSource liveSource = liveServer.getSourceByName(sessionSource.getName());
			if (liveSource == null) {
				throw new IllegalStateException("Saved map named " + sessionSource.getName() + " is no longer on the server");
			}
			wrapper.setWrappedSource(liveSource);
			liveSource.setOffset(sessionSource.getOffset());
			liveSource.setIgnoreValue(sessionSource.getIgnoreValue());
			liveSource.setMaxPPD(sessionSource.getMaxPPD());
			model.getPipelineLeg(i).setMapSource(liveSource);
		}
	}
	
	public LView recreateLView(SerializedParameters parmBlock) {
		if (!(parmBlock instanceof MapLView.InitialParams))
			return null;
		final MapLView.InitialParams p = (MapLView.InitialParams)parmBlock;
		
		// define the layer to update
		final MapLayer mapLayer = createLayer();
		
		// the last step, updating the layer
		final Runnable update = new Runnable() {
			public void run() {
				try {
					Pipeline[] lviewPipeline = p.lviewPPM.buildPipeline();
					mapLayer.mapSettingsDialog.setLViewPipeline(lviewPipeline);
					
					Pipeline[] chartPipeline = p.chartPPM.buildPipeline();
					mapLayer.mapSettingsDialog.setChartPipeline(chartPipeline);
					
					mapLayer.mapSettingsDialog.firePipelineEvent();
					mapLayer.focusPanel.infoPanel.loadFields();//@since change bodies
					mapLayer.focusPanel.setSelectedIndex(mapLayer.focusPanel.selectedIndex);
					if (p.profilePath!=null) {
						mapLayer.focusPanel.mapLView.setProfileLine(p.profilePath.getPath().getShape());
				}
				}
				catch(Exception ex){
					error(ex.getMessage(), "Error recreating layer");
				}
			}
		};
		
		// second to last step, resolving the maps used on this saved map to
		// those reloaded from the live servers
		final Runnable resolve = new Runnable() {
			public void run() {
				try {
					// coordinate maps loaded from server and maps just loaded from the session file
					fixSources(p.lviewPPM);
					fixSources(p.chartPPM);
					
					Set<WrappedMapSource> sources = new HashSet<WrappedMapSource>();
					sources.addAll(Arrays.asList(p.lviewPPM.getSources()));
					mapLayer.setMapName(p.lviewPPM.getSource(0).getWrappedSource().getName());//use the first view source
					sources.addAll(Arrays.asList(p.chartPPM.getSources()));
					final CountDownLatch latch = new CountDownLatch(sources.size());
					final MapAttrReceiver attrReceiver = new MapAttrReceiver(){
						public void receive(MapAttr attr) {
							latch.countDown();
						}
					};
					
					for(final WrappedMapSource s: sources) {
						s.getWrappedSource().getMapAttr(attrReceiver);
					}
					
					latch.await();
					
					SwingUtilities.invokeLater(update);
				} catch (Exception e) {
					error("Error while restoring saved map:\n\n" + e.getMessage(),
						"Error while restoring saved map");
					e.printStackTrace();
				}
			}
		};
		
		// the first step, making sure the servers and sources are reloaded
		Runnable onServersReady = new Runnable() {
			public void run() {
				// get off the AWT event thread, since the MapAttrReceiver is
				// always called on the AWT event thread and we want to await()
				// the last resolve
				new Thread(resolve).start();
			}
		};
		
		MapServerFactory.whenMapServersReady(onServersReady);
		MapLView lview = null;
		MapLView3D view3d = new MapLView3D(mapLayer);
		if(p.layerParams == null){
			lview = new MapLView(mapLayer, true, p.layerKey, view3d);
		}else{
			lview = new MapLView(mapLayer, true, p.layerParams, view3d);
		}
		lview.originatingFactory = this;
		return lview;
	}
	
	public String getName() {
		return "Map";
	}
	
	/**
	 * Produces menus for the map layer, by organizing all sources from all
	 * servers by map type (graphic or numeric), one or more map categories, and
	 * map title.
	 */
	public JMenuItem[] createMenuItems() {
		if (MapServerFactory.getMapServers() == null) {
			MapServerFactory.whenMapServersReady(new Runnable() {
				public void run() {
					LManager.getLManager().refreshAddMenu();
				}
			});
			JMenuItem loadingIndicator = new JMenuItem("Loading Maps...");
			loadingIndicator.setEnabled(false);
			return new JMenuItem[]{loadingIndicator};
		} else {
			// Combine all non-custom server sources
			List<MapSource> allSources = new LinkedList<MapSource>();
			for (final MapServer server: MapServerFactory.getMapServers()) {
				if (server instanceof CustomMapServer)
					continue;
				
				for (final MapSource source: server.getMapSources()) {
					allSources.add(source);
				}
			}
			
			// Pair together graphic and numeric versions of the same source
			// based on a difference of '_numeric' in the WMS name element
			Map<String,MapSource[]> pairs = new HashMap<String,MapSource[]>();
			for (MapSource source: allSources) {
				String baseName = source.getName().replaceAll("_numeric$", "");
				MapSource[] pair = pairs.get(baseName);
				if (pair == null) {
					pairs.put(baseName, pair = new MapSource[2]);
				}
				pair[source.hasNumericKeyword() ? 1 : 0] = source;
			}
			
			// Get the menu path for each pair
			Map<String[],MapSource[]> paths = new TreeMap<String[],MapSource[]>(categoryComparator);
			for (MapSource[] pair: pairs.values()) {
				// the primary source will supply values that could come from either source
				MapSource primary = pair[pair[0] == null ? 1 : 0];
				for (String[] category: primary.getCategories()) {
					String[] path = new String[category.length + 1];
					System.arraycopy(category, 0, path, 0, category.length);
					path[path.length-1] = primary.getTitle();
					paths.put(path, pair);
				}
			}
			
			// create menu hierarchies
			Map<String,JMenu> menuCache = new HashMap<String,JMenu>();
			List<JMenuItem> roots = new LinkedList<JMenuItem>();
			for (String[] path: paths.keySet()) {
				JMenu parent = null;
				List<String> subPath = new LinkedList<String>();
				for (int i = 0; i < path.length - 1; i++) {
					String part = path[i];
					subPath.add(part);
					String pathKey = Util.join(",", subPath);
					JMenu partMenu = menuCache.get(pathKey);
					if (partMenu == null) {
						menuCache.put(pathKey, partMenu = new JMenu(part));
						if (parent == null) {
							roots.add(partMenu);
						} else {
							parent.add(partMenu);
						}
					}
					parent = partMenu;
				}
				JMenuItem processingMenu = createMenuPicker(paths.get(path));
				// this can only be false with maps that weren't in the menus to begin with
				if (parent != null) {
					parent.add(processingMenu);
				}
			}
			
			// If all categories start with 'By ', as in 'By Instrument' or 'By
			// Type', prefix 'Maps ' in front of each category. If any category
			// does not start with 'By ', then move all top level items into a
			// new menu called 'Maps'.
			boolean allTopNames = true;
			for (JMenuItem root: roots) {
				allTopNames &= root.getText().startsWith("By ");
			}
			if (allTopNames) {
				for (JMenuItem root: roots) {
					root.setText("Maps " + root.getText());
				}
			} else {
				JMenu proxyRoot = new JMenu("Maps");
				for (JMenuItem item: roots)
					proxyRoot.add(item);
				roots.clear();
				roots.add(proxyRoot);
			}
			
			// create custom menuitem
			CustomMapServer customServer = MapServerFactory.getCustomMapServer();
			if (customServer != null && customServer.getMapSources().size() > 0) {
				List<MapSource> customSources = new ArrayList<MapSource>(customServer.getMapSources());
				Collections.sort(customSources, customSourceComparator);
				JMenu customMenu = new JMenu("Custom Maps");
				for (MapSource source: customSources) {
					MapSource[] pair = {
						source.hasNumericKeyword() ? null : source,
						source.hasNumericKeyword() ? source : null,
					};
					customMenu.add(createMenuPicker(pair));
				}
				roots.add(customMenu);
			}
			
			// create advanced menuitem
			JMenuItem advancedMenu = new JMenuItem("Advanced Map...");
			advancedMenu.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					createCustomProc();
				}
			});
			roots.add(advancedMenu);
			
			return (JMenuItem[])roots.toArray(new JMenuItem[0]);
		}
	}
	
	/** Creates the menu that contains 'view', 'plot', and 'both' submenus */
	private JMenuItem createMenuPicker(final MapSource[] pair) {
		// the primary source will supply values that could come from either source
		String title = pair[pair[0] == null ? 1 : 0].getTitle();
		title = title.replaceAll(" - Numeric$", "");
		JMenu top = new JMenu(title);
		JMenuItem viewItem = new JMenuItem("View Graphic Data");
		JMenuItem plotItem = new JMenuItem("Plot Numeric Data");
		JMenuItem bothGraphic = new JMenuItem("Plot, and View Graphic");
		JMenuItem bothNumeric = new JMenuItem("Plot, and View Numeric");
		JMenuItem contourItem = new JMenuItem("Contour Numeric Data");
		top.add(viewItem);
		top.add(plotItem);
		top.add(bothGraphic);
		top.add(bothNumeric);
		top.add(contourItem);
		viewItem.setEnabled(pair[0] != null);
		plotItem.setEnabled(pair[1] != null);
		bothGraphic.setEnabled(viewItem.isEnabled() && plotItem.isEnabled());
		bothNumeric.setEnabled(pair[1] != null);
		contourItem.setEnabled(pair[1] != null);
		viewItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createLayer(pair[0], null);
			}
		});
		plotItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createLayer(null, pair[1]);
			}
		});
		bothGraphic.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createLayer(pair[0], pair[1]);
			}
		});
		bothNumeric.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createLayer(pair[1], pair[1]);
			}
		});
		contourItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				createContour(pair[1], null);
			}
		});
		return top;
	}
	
	/**
	 * Categories are sorted alphabetically at each level, with contents of each
	 * level sorted to after the subcategories.
	 */
	private Comparator<String[]> categoryComparator = new Comparator<String[]>() {
		public int compare(String[] c1, String[] c2) {
			int minsize = Math.min(c1.length, c2.length);
			for (int i = 0; i < minsize; i++) {
				int result = c1[i].compareToIgnoreCase(c2[i]);
				if (result != 0) {
					return result;
				}
			}
			if (c1.length > c2.length)
				return -1;
			if (c1.length < c2.length)
				return 1;
			return 0;
		}
	};
	
	/**
	 * Custom maps are sorted alphabetically
	 */
	private Comparator<MapSource> customSourceComparator = new Comparator<MapSource>() {
		public int compare(MapSource o1, MapSource o2) {
			return o1.getTitle().compareToIgnoreCase(o2.getTitle());
		}
	};
	
	/** Creates a new main LView for a map layer that contours the given map */
	public void createContour(final MapSource source, final ContourStageSettings contourSettings) {
		final MapLayer layer = createLayer();
		layer.setName("Preparing Contour...");
		final MapLView mapLView = new MapLView(layer, true);
		mapLView.originatingFactory = MapLViewFactory.this;
		LManager.receiveNewLView(mapLView);
		final MapSettingsDialog dlg = mapLView.getLayer().mapSettingsDialog;
		source.getMapAttr(new MapAttrReceiver() {
			public void receive(MapAttr attr) {
				MapChannel ch = new MapChannel(
					Main.testDriver.mainWindow.getProj().getWorldWindow(),
					Main.testDriver.mainWindow.getZoomManager().getZoomPPD() / 4,
					Main.PO,
					new Pipeline[]{new Pipeline(source, new Stage[]{})});
				ch.addReceiver(new MapChannelReceiver() {
					public void mapChanged(MapData mapData) {
						if (! mapData.isFinished()) {
							return;
						}
						ContourStageSettings css = new ContourStageSettings();
						Raster r = mapData.getImage().getRaster();
						int bands = r.getNumBands();
						if (contourSettings == null) {
							double min = Double.POSITIVE_INFINITY;
							double max = Double.NEGATIVE_INFINITY;
							double[] pixel = new double[bands];
							double[] ignore = source.getIgnoreValue();
							for (int x = 0; x < r.getWidth(); x++) {
								for (int y = 0; y < r.getHeight(); y++) {
									r.getPixel(x, y, pixel);
									if (ignore == null || !Arrays.equals(ignore, pixel)) {
										min = Math.min(min, pixel[0]);
										max = Math.max(max, pixel[0]);
									}
								}
							}
							css.setBase(min - (max-min)/12);
							css.setStep((max-min)/10);
						} else {
							//if added from the search result row, the user will enter the base, step, color and line thickness
							css = contourSettings;
						}
						Stage contour = css.createStage();
						Stage single = new SingleCompositeSettings().createStage();
						Pipeline view = new Pipeline(source, new Stage[]{contour, single});
						
						Stage getband = new BandExtractorStageSettings(bands,0).createStage();
						Stage bandagg = new BandAggregatorSettings(1).createStage();
						Pipeline plot = new Pipeline(source, new Stage[]{getband,bandagg});

						try {
							layer.setName("Contour of " + source.getTitle());
							dlg.setLViewPipeline(new Pipeline[]{view});
							dlg.setChartPipeline(new Pipeline[]{plot});
							dlg.firePipelineEvent();
						} catch (Exception e) {
							error("Error when constructing processing graph:\n\n", "Unable to create contour map");
							e.printStackTrace();
						}
					}
				});
			}
		});
	}
	
		/** Creates a new main LView for a new map layer and sends it to the given callback */
	public void createLayer(final MapSource viewSource, final MapSource plotSource) {
		ArrayList<MapSource> plotSources = new ArrayList<MapSource>();
		if (plotSource!=null) {
			plotSources.add(plotSource);
		}
		createLayer(viewSource, plotSources, null, null);
	}
	
	/** Creates a new main LView for a new map layer and sends it to the given callback */
	public void createLayer(final MapSource viewSource, final ArrayList<MapSource> plotSources, LayerParameters lp, MapLView lview) {
		LayerParameters layerParams = null;
		if(lp!=null)
			layerParams = lp;
	
		final MapLView mapLView;
		
		if(lview == null){
			MapLayer newLayer = createLayer();
			newLayer.setMapName(viewSource.getName());
			MapLView3D view3d = new MapLView3D(newLayer);
			mapLView = new MapLView(newLayer, true, layerParams, view3d);
		}else{
			mapLView = lview;
		}
		
		mapLView.originatingFactory = MapLViewFactory.this;
		LManager.receiveNewLView(mapLView);
		
		if (viewSource != null) {
			viewSource.getMapAttr(new MapAttrReceiver() {
				public void receive(MapAttr attr) {
					try {
						setViewSource(((MapLayer)mapLView.getLayer()).mapSettingsDialog, viewSource);
					} catch (Exception e) {
						e.printStackTrace();
						Util.showMessageDialog("Exception: "+e.toString()+" creating layer for "+viewSource.toString(),
							"Error creating layer for "+viewSource.toString()+"!", JOptionPane.ERROR_MESSAGE);

					}
				}
			});
		} else {
			// This block handles the case of using the old-style add menu and selecting 'Plot Only'
			if (plotSources.size()>0) {
				final MapSource plotSource = plotSources.get(0);
				plotSource.getMapAttr(new MapAttrReceiver() {
					public void receive(MapAttr attr) {
						try {
							setPlotSource(((MapLayer)mapLView.getLayer()).mapSettingsDialog, plotSource);
						} catch (Exception e) {
							Util.showMessageDialog("Exception: "+e.toString()+" creating layer for "+plotSource.toString(),
								"Error creating layer for "+plotSource.toString()+"!", JOptionPane.ERROR_MESSAGE);
						}
					}
				});
				return;
			}			
		}
		
		if (plotSources.size()>0) {	
			Pipeline pipeline[] = new Pipeline[plotSources.size()];
			
			for (int i=0; i<plotSources.size(); i++) {
				pipeline[i]=new Pipeline(plotSources.get(i), new Stage[]{(CompositeStage)(new BandAggregatorSettings(plotSources.size())).createStage()});
			}
		
			try {
				(((MapLayer)mapLView.getLayer()).mapSettingsDialog).getChartPipelineModel().setFromPipeline(
						pipeline, (CompositeStage)(new BandAggregatorSettings(plotSources.size())).createStage());
			} catch (Exception e) {
				Util.showMessageDialog("Exception: "+e.toString()+" creating layer for "+plotSources.toString(),
				"Error creating layer for "+plotSources.toString()+"!", JOptionPane.ERROR_MESSAGE);				
			}
		}
	}
	
	/** Creates a new main LView for a map layer that adds a sigma stretch to the given map */
	public void createLayerWithStage(final MapSource source, final ArrayList<MapSource> plotSources, LayerParameters lp, String stageOptions) {
		LayerParameters layerParams = null;
		if(lp!=null)
			layerParams = lp;
		
		final MapLayer layer = createLayer();
		MapLView3D view3d = new MapLView3D(layer);		
		final MapLView mapLView = new MapLView(layer, true, layerParams, view3d);

		mapLView.originatingFactory = MapLViewFactory.this;
		LManager.receiveNewLView(mapLView);
		final MapSettingsDialog dlg = mapLView.getLayer().mapSettingsDialog;
		source.getMapAttr(new MapAttrReceiver() {
			public void receive(MapAttr attr) {
				MapChannel ch = new MapChannel(
					Main.testDriver.mainWindow.getProj().getWorldWindow(),
					Main.testDriver.mainWindow.getZoomManager().getZoomPPD() / 4,
					Main.PO,
					new Pipeline[]{new Pipeline(source, new Stage[]{})});
				ch.addReceiver(new MapChannelReceiver() {
					public void mapChanged(MapData mapData) {
						if (! mapData.isFinished()) {
							return;
						}
												
						Stage stage = null;
						
						// Only option currently
						if (stageOptions.startsWith("sigma")) {
							int sigmaPPD = 1;
							if (stageOptions.contains(":")) {
								try {
								sigmaPPD = Integer.parseInt(stageOptions.substring(stageOptions.indexOf(":")+1));
								} catch (Exception e) {
									System.out.println("Invalid sigma ppd: " + stageOptions);
								}
							}
							SigmaStretchStageSettings ssss = new SigmaStretchStageSettings();
							ssss.setMinPPD(sigmaPPD);
							stage = ssss.createStage();
						}
						
						Stage single = new SingleCompositeSettings().createStage();
						Pipeline view = new Pipeline(source, new Stage[]{stage, single});
						
						try {
							dlg.setLViewPipeline(new Pipeline[]{view});
														
							if (plotSources.size()>0) {	
								Pipeline pipeline[] = new Pipeline[plotSources.size()];
								
								for (int i=0; i<plotSources.size(); i++) {
									pipeline[i]=new Pipeline(plotSources.get(i), new Stage[]{(CompositeStage)(new BandAggregatorSettings(plotSources.size())).createStage()});
								}
							
								try {
									(((MapLayer)mapLView.getLayer()).mapSettingsDialog).getChartPipelineModel().setFromPipeline(
											pipeline, (CompositeStage)(new BandAggregatorSettings(plotSources.size())).createStage());
								} catch (Exception e) {
									Util.showMessageDialog("Exception: "+e.toString()+" creating layer for "+plotSources.toString(),
									"Error creating layer for "+plotSources.toString()+"!", JOptionPane.ERROR_MESSAGE);				
								}
							}

							dlg.firePipelineEvent();
						} catch (Exception e) {
							error("Error when constructing processing graph:\n\n", "Unable to create contour map");
							e.printStackTrace();
						}
					}
				});
			}
		});
	}
	
	/** Creates and returns an empty MapLayer */
	private static MapLayer createLayer() {
		return new MapLayer(new MapSettingsDialog(LManager.getDisplayFrame()));
	}
	
	private static MapLayer createLayer(JDialog ownerDialog) {
	    return new MapLayer(new MapSettingsDialog(ownerDialog));
	}
	
	private static MapLayer createLayer(JFrame context) {
		return new MapLayer(new MapSettingsDialog(context));
	}
	
	/** Configures map source as the view */
	private void setViewSource(MapSettingsDialog dlg, MapSource source) throws Exception {
		dlg.setLViewPipeline(Pipeline.buildAutoFilled(new MapSource[]{ source },
			(CompositeStage)(new SingleCompositeSettings()).createStage()));
		dlg.firePipelineEvent();
	}
	
	/** Adds the map source as a plot */
	private void setPlotSource(MapSettingsDialog dlg, MapSource source) throws Exception {
		dlg.setChartPipeline(Pipeline.build(new MapSource[]{ source }, 
			(CompositeStage)(new BandAggregatorSettings(1)).createStage()));
		dlg.firePipelineEvent();
	}
	
	private void createCustomProc() {
		createLView(true);
	}

	@Override
	public Icon getLayerIcon() {
		return layerTypeIcon;	
	}
}
