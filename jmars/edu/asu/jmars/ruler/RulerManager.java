/**
 **  A class for managing and maintaining rulers for all ruler-enabled layers in 
 **  JMARS.  This starts up when JMARS is started. In fact, the viewing window
 **  becomes the top ruler with a zero-height dragbar.  Rulers are added and
 **  removed from the list of rulers but the Ruler Manager is never shut down
 **  while JMARS is running.
 **  
 **
 **  @author James Winburn    10/03    MSFF_ASU
 **
 **/
package edu.asu.jmars.ruler;

// generic java imports
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.thoughtworks.xstream.XStream;

import edu.asu.jmars.LocationListener;
import edu.asu.jmars.Main;
import edu.asu.jmars.ZoomListener;
import edu.asu.jmars.layer.LViewManager;

/** This class maintains the list of rulers, provides a user interface for showing/hiding them, and maintains basic settings for them. When the RulerManager is first loaded, it restores settings from ~/jmars/rulers.xml.  When an LView is loaded, it should add its rulers to the RulerManager right away; the addRuler() method here will push settings from rulers.xml onto each ruler as it is added. When a layer is deleted with remoteRuler(), the settings are copied into the state stored on this RulerManager, so it can be readded in the same state. After LViews have loaded themselves, Main.loadState() may push session settings on top of those set in addRuler(); it should happen in that order so session state has priority over ~/jmars settings. */
public class RulerManager extends MultiSplitPane
{
	private static final String settingsPath = Main.getJMarsPath() + "rulers.xml";
	private static final String baseSuffix = ".base";
	private static final Charset settingsCharset = Charset.forName("ISO-8859-1");
	private static final String rulerMgrKey = "rulerManager";
	
	// The list of rulers to be displayed.
	public List<BaseRuler> rulerList;

	// When JMARS first starts, the main window becomes the top ruler
	// in the RulerManager.  If this is not done, then no ruler may be 
	// added.  This flag stores whether it was done.
	static boolean isContentSet = false;

	// Properties dialog stuff for all rulers. 
	//static private JPanel         propertiesPanel;
	static public  JDialog        propertiesDialog;
	static public  JTabbedPane    propertiesTabbedPane;
	static public  JPanel         hidingPanel;

	/** map of ruler class name to ruler property name to ruler property value */
	private final Hashtable<String,Hashtable<String,Object>> settings;
	
	private boolean settingsDirty = false;
	
	// set up a location listener so that the rulers will be updated even if
	// the layer is turned off.
	public void setLViewManager( LViewManager lvman){
		lvman.getLocationManager().addLocationListener(new LocationListener() {
			public void locationChanged(Point2D loc) {
				notifyRulerOfViewChange();
			}
		});
		lvman.getZoomManager().addListener(new ZoomListener() {
			public void zoomChanged(int newPPD) {
				notifyRulerOfViewChange();
			}
		});
	}

	// fields common to all rulers.
	public RulerProperty backgroundColor = new RulerProperty("backgroundColor",     
		new RulerColorButton( "Background Color",  new Color(  0,0,0)));
	static public RulerProperty relFontSize = new RulerProperty("relFontSize",
		new RulerStepSlider(-2,8,1,1));

	// the singleton instance.  All methods needing the RulerManager should call
	// RulerManager.Instance  (eg.  RulerManager.Instance.foo() ).
	public final static RulerManager Instance  = new RulerManager();


	// the constructor.  Because there can be only one, the constructor is private.
	private RulerManager() 
	{
		super();
		setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		rulerList = new ArrayList<BaseRuler>();
		
		// restore any saved settings
		settings = loadXML();
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// restore the settings some time later, to give the ruler
				// manager a chance to finish setting itself up
				loadSettings(settings.get(rulerMgrKey));
				
				// add a window listener that will save the settings to
				// an XML file when the main window is closed.
				Main.mainFrame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e) {
						saveXML();
					}
				});
			}
		});
		
		setUpPropertiesDialog();
	}
	
	public Hashtable<String,Hashtable<String,Object>> loadXML() {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(settingsPath),
				settingsCharset));
			XStream xstream = new XStream();
			return (Hashtable<String,Hashtable<String,Object>>)xstream.fromXML(reader);
		} catch (FileNotFoundException e) {
			return new Hashtable<String,Hashtable<String,Object>>();
		} catch (Exception e) {
			e.printStackTrace();
			return new Hashtable<String,Hashtable<String,Object>>();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Updates the current settings from the ruler manager, the saveSettings()
	 * from each loaded ruler, and the saveBaseSettings() from first ruler. Only
	 * the first ruler's base settings are used since it is assumed all rulers
	 * loaded at the same time will share a common parent class that handles the
	 * same set of shared settings.
	 */
	public void saveXML() {
		// if settings were loaded, they could have been changed at any time
		if (settingsDirty) {
			// if we still have rulers active, go get all settings just to be certain
			if (rulerList.size() > 0) {
				BaseRuler firstRuler = rulerList.get(0);
				settings.put(rulerMgrKey, saveSettings());
				settings.put(firstRuler.getClass().getName() + baseSuffix, firstRuler.saveBaseRulerSettings());
				for (BaseRuler ruler: rulerList) {
					settings.put(ruler.getClass().getName(), ruler.saveSettings());
				}
			}
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(settingsPath),
						settingsCharset));
				XStream xstream = new XStream();
				xstream.toXML(settings, writer);
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				if (writer != null) {
					try {
						writer.flush();
						writer.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
			settingsDirty = false;
		}
	}

	// Makes the main view the top ruler.  This is done in Layer.java. If it
	// is NOT done, no ruler may be added to the layer.
	public Component setContent( JComponent component, JFrame frame)
	{
		Component comp = super.setContent( component, frame);
		if (comp != null){
			isContentSet = true;
		}
		return comp;
	}

	/**
	 * Adds a ruler to the Ruler Manager. If a properties panel exists for the
	 * ruler, it is added as a tabbed pane to the properties dialog. Also passes
	 * base settings to rp.loadBaseSettings() if this is the first ruler to be
	 * added to the RulerManager, and always calls loadSettings() with the
	 * settings for this specific ruler.
	 */
	public void addRuler( BaseRuler rp)
	{
		settingsDirty = true;
		
		if (rulerList.size() > 0) {
			// force all rulers in the ruler list to extend the same common parent class
			if (rulerList.get(0).getClass().getSuperclass() != rp.getClass().getSuperclass()) {
				throw new IllegalArgumentException("Unable to add ruler " + rp.getClass().getName() +
					" to ruler list starting with ruler " + rulerList.get(0).getClass().getName());
			}
			// prevent more than one instance of the same exact class from being added at the same time
			for (BaseRuler r: rulerList) {
				if (r.getClass() == rp.getClass()) {
					throw new IllegalArgumentException("Only one instance of any ruler type allowed at the same time.");
				}
			}
		}
		
		if (isContentSet==false){
			return;
		}
		
		super.addComponent( rp);
		
		String id = rp.getClass().getName();
		if (rulerList.isEmpty() && settings.containsKey(id + baseSuffix)) {
			rp.loadBaseRulerSettings(settings.get(id + baseSuffix));
		}
		if (settings.get(id) != null) {
			rp.loadSettings(settings.get(id));
		}
		
		rulerList.add( rp);
		
		if (rp.isHidden()){
			hideComponent( rp);
		}

		// if the title dragbar is hidden, unhide it.
		DragBar dbar = (DragBar)super.getComponent(1);
		if (dbar.isHidden()){
			dbar.setHidden(false);
		}

		updatePropertiesTabbedPane();
		updateHidingPanel();
		packFrame();
	}

	/**
	 * Removes a ruler from the Manager. If the ruler had a properties panel
	 * and there are no more properties, it too is removed. The shared settings
	 * are retrieved from saveBaseSettings() if this was the last ruler in this
	 * manager, and regardless the saveSettings() are retrieved for next time
	 * this ruler is loaded.
	 */
	public void removeRuler( BaseRuler rp)
	{
		super.removeComponent( rp);
		rulerList.remove( rp);
		
		String id = rp.getClass().getName();
		if (rulerList.isEmpty()) {
			settings.put(id + baseSuffix, rp.saveBaseRulerSettings());
		}
		settings.put(id, rp.saveSettings());
		
		// If there are no more rulers, hide the title dragbar.
		if (rulerList.isEmpty() ){
			DragBar dbar = (DragBar)super.getComponent(1);
			dbar.setHidden(true);
		}

		updatePropertiesTabbedPane();
		updateHidingPanel();
		packFrame();
	}

	// Get the ruler from ruler list based on the name of the ruler.
	// This is used in TestDriverLayered during the load ruler settings 
	// phase.
	public BaseRuler getRuler( String rulerName){
		for (int i = 0; i < rulerList.size(); i++){
			BaseRuler ruler = (BaseRuler)rulerList.get(i);
			if (ruler.getClass().getName().equals( rulerName)){
				return ruler;
			}
		}
		return null;
	}


	// Accessed by other classes to inform the RulerManager that the rulers 
	// need to be updated.
	//
	// TODO: This has the side effect of also redrawing TestDriverLayered, which is 
	//       unnecessary work in most cases - unfortunately MRO relies on it, so care must
	//       be taken in fixing this.
	public void notifyRulerOfViewChange() {
		repaint();
	}


	// Loads up general ruler properties from the rulerSettings hashtable, which is assumed
	// to have been set up with the settings before calling this method.
	public void loadSettings(Hashtable<String,Object> rulerSettings)
	{
		if (rulerSettings != null) {
			backgroundColor.loadSettings( rulerSettings);
			relFontSize.loadSettings(rulerSettings);
		}
	}

	// Saves general ruler properties to the rulerSettings hashtable.  Methods that
	// call this will presumably access the settings afterwards.
	public Hashtable<String,Object> saveSettings(){
		Hashtable<String,Object> rulerSettings = new Hashtable<String,Object>();
		backgroundColor.saveSettings( rulerSettings);
		relFontSize.saveSettings(rulerSettings);
		return rulerSettings;
	}




	/*-----------------------------------------------------------
	 * The following methods manage the properties pane.
	 *---------------------------------------------------------*/

	// recreates the properties dialog with the properties panel 
	// of all rulers plus a panel that corresponds to general
	// ruler properties.
	public void updatePropertiesTabbedPane()
	{
		propertiesTabbedPane.removeAll();

		propertiesTabbedPane.add( "All Rulers", getPropertiesPanel());
		for (int i=0; i < rulerList.size(); i++) {
			BaseRuler r = (BaseRuler) rulerList.get(i);
			propertiesTabbedPane.add( r.getDescription(), 
					r.getPropertiesPanel());
		}
		propertiesDialog.pack();
	}


	/**
	 * update the hidden status of all rulers in the hidingPanel of the properties dialog.
	 */
	public void updateHidingPanel(){
		hidingPanel.removeAll();
		hidingPanel.setLayout( new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		JPanel rulerPanel = new JPanel();
		rulerPanel.setLayout( new BoxLayout( rulerPanel, BoxLayout.Y_AXIS));
		rulerPanel.setBorder( new TitledBorder("Hide/Unhide Rulers"));
		for (int i=0; i < rulerList.size(); i++) {
			final int       index = i;
			final BaseRuler r = (BaseRuler) rulerList.get( index);
			final JCheckBox cb = new JCheckBox( r.getDescription());
			cb.addActionListener(new AbstractAction(){
				public void actionPerformed( ActionEvent e){
					if (cb.isSelected()){
						RulerManager.Instance.showComponent( r);
					} else {
						RulerManager.Instance.hideComponent( r);
					}
				}
			});
			cb.setSelected( !r.isHidden());
			rulerPanel.add( cb);
		}
		gbc.gridy++;
		hidingPanel.add( rulerPanel, gbc);
		propertiesDialog.pack();
	}



	// initializes the properties dialog.
	public void setUpPropertiesDialog(){
		propertiesDialog = new JDialog( super.frame, "Properties", false);

		Container propPane = propertiesDialog.getContentPane();
		propPane.setLayout( new BorderLayout());

		// Set up the hiding/unhiding panel
		hidingPanel = new JPanel();
		updateHidingPanel();
		propPane.add( hidingPanel, BorderLayout.NORTH);

		// Set up the tabbed pane in the middle of the dialog
		propertiesTabbedPane = new JTabbedPane();
		propertiesTabbedPane.setTabPlacement(JTabbedPane.TOP);
		updatePropertiesTabbedPane();
		propPane.add( propertiesTabbedPane, BorderLayout.CENTER);


		// Set up the button panel
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout(FlowLayout.CENTER));
		JButton okButton = new JButton( 
				new AbstractAction("OK".toUpperCase()) {
					public void actionPerformed(ActionEvent e) {
						propertiesDialog.setVisible(false);
					}
				});
		okButton.setFocusPainted(false);
		buttonPanel.add( okButton);
		propPane.add( buttonPanel, BorderLayout.SOUTH);

		propertiesDialog.pack();


		// Display the properties dialog in the middle of the screen.
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension d = propertiesDialog.getSize();
		int x = (screen.width - d.width) / 2;
		int y = (screen.height - d.height) / 2;
		propertiesDialog.setLocation(x, y);
	}


	// sets up the general ruler properties panel.  This panel consists
	// of ruler background color and any properties that ruler types (Themis
	// and MRO) might have.
	private JPanel getPropertiesPanel(){

		JPanel propertiesPanel = new JPanel();

		propertiesPanel.setLayout( new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridy = 1;
		gbc.gridx = 1;
		gbc.gridwidth = 1;
		propertiesPanel.add( backgroundColor.getProp(), gbc);

		gbc.gridy++;
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.WEST;
		propertiesPanel.add(new JLabel("Font Size:"), gbc);

		gbc.gridx++;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.EAST;
		propertiesPanel.add( relFontSize.getProp(), gbc);


		if (rulerList.size()>0) {
			BaseRuler r = (BaseRuler) rulerList.get(0);
			gbc.gridy++;
			gbc.gridx = 0;
			gbc.gridwidth = 2;
			propertiesPanel.add(r.getBaseRulerPropertiesPanel(), gbc);
		}
		return propertiesPanel;
	}


} // end: class RulerManager.java


