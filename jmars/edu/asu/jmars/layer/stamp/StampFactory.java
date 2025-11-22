package edu.asu.jmars.layer.stamp;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.STAMPS;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.stamp.networking.StampLayerNetworking;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import edu.emory.mathcs.backport.java.util.Collections;

/**
 * Factory for creating and reloading stamp layers in JMARS. In days of old,
 * there were subclasses of this class with instrument-specific names that
 * provided the constructor on this class with the properties it needed to build
 * a view settings object. Those subclasses have been done away with and left in
 * place for compatibility with old session files. This is now the only stamp
 * factory for creating new layers.
 */
public class StampFactory extends LViewFactory {
	private static DebugLog log = DebugLog.instance();	
	private static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	private static Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(STAMPS
       .withDisplayColor(imgLayerColor)));		
	private static Map<String,String[]> layerTypeMap;
	
	
	public static Map<String,String[]> getLayerTypeMap() {
		if (layerTypeMap == null) {
			String body=Config.get(Util.getProductBodyPrefix() + Config.CONFIG_BODY_NAME, "Mars");// @since change bodies – added prefix
			String urlStr = "InstrumentFetcher?planet="+body+"&format=JAVA";
			try {
				ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(urlStr));
				layerTypeMap = (HashMap<String, String[]>) ois.readObject();
				ois.close();
			} catch (Exception e) {
				log.aprintln("Error retrieving list of stamps");
				log.aprintln(e);
				layerTypeMap = new LinkedHashMap<String,String[]>();
			}
		}
		return layerTypeMap;
	}
	
	public static Set<String> getLayerTypes() {
		return Collections.unmodifiableSet(getLayerTypeMap().keySet());	
	}
	
	/**
	* @since change bodies
	*/
	protected void resetStoredData() {
		StampFactory.layerTypeMap = null;
	}
	/**
	 * Returns an instance of this StampFactory for just those classes that
	 * could have been used to identify a serialized layer that this class
	 * should be able to handle
	 */
	public static synchronized StampFactory createAdapterFactory(String oldFactoryClass) {
		if (oldFactoryClass.equals("edu.asu.jmars.layer.stamp.MocStampFactory"))
			return new StampFactory();
		if (oldFactoryClass.equals("edu.asu.jmars.layer.stamp.CTXStampFactory"))
			return new StampFactory();
		if (oldFactoryClass.equals("edu.asu.jmars.layer.stamp.HiRISEStampFactory"))
			return new StampFactory();
		if (oldFactoryClass.equals("edu.asu.jmars.layer.stamp.VikingStampFactory"))
			return new StampFactory();
		if (oldFactoryClass.equals("edu.asu.jmars.layer.stamp.HRSCStampFactory"))
			return new StampFactory();
		if (oldFactoryClass.equals("edu.asu.jmars.layer.stamp.ThemisBtrStampFactory"))
			return new StampFactory();
		return null;
	}
	
    public StampFactory() {
    	super("Stamps", "Access to individual images from a variety of instruments");
    	type = "stamp";
    }

	/**
	 * Returns the 'loading' menu immediately, gets on another thread to
	 * retrieve the instrument types, and gets back on the AWT thread to update
	 * the original menu.
	 */
	protected JMenuItem[] createMenuItems() {
		if (JmarsHttpRequest.isStampServerAvailable()) {
			final JMenu menu = new JMenu("Loading Stamps");
			menu.setEnabled(false);
			Thread thread = new Thread(new Runnable() {
				public void run() {
					final Map<String,String[]> types = getLayerTypeMap();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							for (final String instrument: types.keySet()) {
								final String[] initialColumns = types.get(instrument);
								JMenuItem type = new JMenuItem(instrument.replace("_"," ") + " Stamps");
								type.setToolTipText("Outlines of " + instrument.replace("_"," ") + " observation polygons");
								type.addActionListener(new ActionListener() {
									public void actionPerformed(ActionEvent e) {
										addLView(null, instrument, initialColumns, "");
									}
								});
								menu.add(type);
							}
							menu.setText("Stamps");
							menu.setEnabled(true);
						}
					});
				}
			});
			thread.setName("InstrumentFetcher");
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.setDaemon(true);
			thread.start();
			return new JMenuItem[]{menu};
		} else {
			return new JMenuItem[]{new JMenu("Stamps Unavailable")};
		}
	}
	
	/**
	 * Causes a dialog to be shown that the user fills out, and if they hit okay
	 * a new stamp layer with the user's settings and the initial columns given
	 * here is added
	 */
	public void addLView(LayerParameters layerParams, String instrument, String[] initialColumns, String layergroup) {
		if (JmarsHttpRequest.isStampServerAvailable()) {
			if (instrument.equalsIgnoreCase("davinci")) {
				
				JPanel portPanel = new JPanel();
				portPanel.setLayout(new GridLayout(1, 2));
				
				portPanel.add(new JLabel("Listen Port:"));
				JTextField listenField = new JTextField("56277");
				portPanel.add(listenField);
				
				StampLayerDialog dialog = new StampLayerDialog(portPanel);
				
				dialog.setVisible(true);
				
				if (!dialog.isCancelled()) {				
					StampLayerSettings newSettings = new StampLayerSettings();
					newSettings.setLayerParams(layerParams);
		
					if (dialog.initialName.getText().trim().length()>0) {
						newSettings.name = dialog.initialName.getText().trim();
					} else {
						newSettings.name = "Davinci Stamps" + " (port " + listenField.getText() + ")";
					}
	
					newSettings.instrument = instrument;
					
					String userColumns[] = new String[0]; 
					
					if (Main.USER!=null&&Main.USER.length()>0) {
						userColumns=StampLayerNetworking.getDefaultColumns(instrument);
					}
					
					if (userColumns.length>0) {
						newSettings.initialColumns = userColumns;
					} else {
						newSettings.initialColumns = initialColumns;						
					}

					newSettings.port = Integer.parseInt(listenField.getText());
					StampLayer layer = new StampLayer(newSettings);
					StampLView view = new StampLView(StampFactory.this, layer, null, layerParams, layer.enable3DView() ? new StampLView3D(layer) : null);
					
					Class colTypes[] = new Class[initialColumns.length];
					
					for (int i=0; i<colTypes.length; i++) {
						colTypes[i]=String.class;
					}
					
					String colNames[] = initialColumns;
					//{ "Id", "Name", "Location", "ul_lat", "ul_lon", "lr_lat", "lr_lon" };
					view.myFocus.updateData(colTypes, colNames, newSettings.initialColumns);								
	
					layer.setViewToUpdate(view);
					LManager.receiveNewLView(view);
				}
			} else {
				StampLayerWrapper wrapper = new StampLayerWrapper(instrument, layergroup);
				StampLayerDialog dialog = StampLayerDialog.getStampLayerDialog(wrapper, true);
		
				dialog.setVisible(true);
				
				if (!dialog.isCancelled()) {
					StampLayerSettings newSettings = new StampLayerSettings();
					newSettings.setLayerParams(layerParams);
					newSettings.instrument = instrument;
					newSettings.queryStr = wrapper.getQuery();
					
					String userColumns[] = new String[0]; 
					
					if (Main.USER!=null&&Main.USER.length()>0) {
						userColumns=StampLayerNetworking.getDefaultColumns(instrument);
					}
					
					if (userColumns.length>0) {
						newSettings.initialColumns = userColumns;
					} else {
						newSettings.initialColumns = initialColumns;						
					}

					newSettings.unsColor = dialog.initialColor.getColor();
					newSettings.filColor = new Color(dialog.initialColor.getColor().getRGB() & 0xFFFFFF, true);
					newSettings.srcName=wrapper.srcName;
					newSettings.srcItems=wrapper.srcItems;
					newSettings.paths=wrapper.paths;
					
					newSettings.expressionText=dialog.colorExpression;
					newSettings.colorState=dialog.colorState;
					newSettings.colorMin=dialog.colorMin;
					newSettings.colorMax=dialog.colorMax;
					newSettings.orderColumn=dialog.orderColumn;
					newSettings.orderDirection=dialog.orderDirection;
					if (!Double.isNaN(dialog.colorMin) && !Double.isNaN(dialog.colorMax)) {
						newSettings.lockColorRange=true;
					}
					
					StampLayer layer = new StampLayer(newSettings);

					if (dialog.initialName.getText().trim().length()>0) {
						newSettings.name = dialog.initialName.getText().trim();
					} else {
						if (layer.spectraData()) {
							newSettings.name = instrument.replace("_", " ") + " Spots";
						} else {
							newSettings.name = instrument.replace("_", " ") + " Stamps";
						}
					}
					
					StampLView view = new StampLView(StampFactory.this, layer, wrapper, layerParams, layer.enable3DView() ? new StampLView3D(layer) : null);
					layer.setViewToUpdate(view);
					LManager.receiveNewLView(view);
				}
			}
		} else {
			JOptionPane op = new JOptionPane("Stamps are currently unavailable. Please try again later or contact JMARS support.",JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION);
			JDialog dialog = op.createDialog(Main.mainFrame, "Stamp Server Unavailable");
			dialog.setVisible(true);
		}
	}
	
	/** Stamp layers are not added by default */
    public Layer.LView createLView() {
        return null;
    }
    
    public Layer.LView recreateLView(SerializedParameters parmBlock) {
        StampLView view = null;
        
        log.println("recreateLView called");
        
        if (parmBlock != null &&
            parmBlock instanceof StampLayerSettings)
        {
            StampLayerSettings settings = (StampLayerSettings) parmBlock;
            if (settings.queryStr!=null) {
            	//We do the following logic of breaking the url query string up into two pieces,
            	//because the regex replace on large queries is extremely slow. The replace is just
            	//for the hostname, which is alwas at the beginning of the query string, so there is no
            	//reason to run the replace on the entire query. Some of these queries have hundreds
            	//of thousands of characters. The hostname is removed so that 
            	//so that the hostname are ignored if they are stored in sessions. 
            	int idx = settings.queryStr.indexOf("?");
        		
        		String connStr = settings.queryStr.substring(0,idx+1);
        		
        		String data = settings.queryStr.substring(idx+1);

        		// Strip off a passed in stampURL, just in case we have one saved from an old save file
        		connStr = connStr.replace(StampLayer.stampURL, "");
        		
            	connStr = connStr.replaceAll(".*StampFetcher\\?","StampFetcher?");
            	settings.queryStr = connStr + data;
            } else {
            	settings.queryStr="";
            }
            StampLayer stampLayer = new StampLayer(settings);
            view = new StampLView(this, stampLayer, null, settings.getLayerParams(), stampLayer.enable3DView() ? new StampLView3D(stampLayer) : null);
        	stampLayer.setViewToUpdate(view);
            
            log.println("successfully recreated StampLView");
        }
        
        return view;
    }
    

    public static void createOverlappingStampLayer(String type, ArrayList<String> ids, ArrayList<GeneralPath> paths) {
		StampLayerWrapper wrapper = new StampLayerWrapper(type, type + " stamps intersecting shapes", ids,paths);
		StampLayerDialog dialog = StampLayerDialog.getStampLayerDialog(wrapper,false);
		dialog.setVisible(true);
		if (!dialog.isCancelled()) {
			StampLayerSettings newSettings = new StampLayerSettings();
			
			newSettings.instrument = type;
			String newQuery = wrapper.getQuery();
			newSettings.queryStr = newQuery;
			newSettings.initialColumns = layerTypeMap.get(type);
			newSettings.unsColor = dialog.initialColor.getColor();
			newSettings.filColor = new Color(dialog.initialColor.getColor().getRGB() & 0xFFFFFF, true);
			newSettings.srcName=wrapper.srcName;
			newSettings.srcItems=wrapper.srcItems;
			newSettings.paths=wrapper.paths;
			
			newSettings.expressionText=dialog.colorExpression;
			newSettings.colorState=dialog.colorState;
			newSettings.colorMin=dialog.colorMin;
			newSettings.colorMax=dialog.colorMax;
			newSettings.orderColumn=dialog.orderColumn;
			newSettings.orderDirection=dialog.orderDirection;
			if (!Double.isNaN(dialog.colorMin) && !Double.isNaN(dialog.colorMax)) {
				newSettings.lockColorRange=true;
			}
			
			StampLayer layer = new StampLayer(newSettings);
			
			if (dialog.initialName.getText().trim().length()>0) {
				newSettings.name = dialog.initialName.getText().trim();
			} else {
				if (layer.spectraData()) {
					newSettings.name = type + " Spots";
				} else {
					newSettings.name = type + " Stamps";
				}
			}
			
			StampLView view = new StampLView(new StampFactory(), layer, wrapper, null, layer.enable3DView() ?new StampLView3D(layer):null);
			layer.setViewToUpdate(view);
			LManager.receiveNewLView(view);
		}    	    	
    }
     
   @Override 
   public Icon getLayerIcon() {
        return layerTypeIcon;
   }
    
}
