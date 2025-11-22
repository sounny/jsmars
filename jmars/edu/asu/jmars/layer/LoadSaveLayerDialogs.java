package edu.asu.jmars.layer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;

/**
 * Manages the load/save layer dialogs. The outer class is there to hold the
 * data they must share. Each dialog's inner class constructor will
 * automatically show the dialog or not as required.
 */
public class LoadSaveLayerDialogs {
	/** Pixels of space to leave around components */
	private final int gap = 4;
	/** Top level frame to anchor everything to */
	private final Frame frame = Main.mainFrame;
	/** The file chooser, shared between load/save dialogs */
	private JFileChooser savedLayerChooser;
	/** Returns the file chooser, creating it the first time */
	private JFileChooser getSavedLayerChooser() {
		if (savedLayerChooser == null) {
			savedLayerChooser = new JFileChooser(Util.getDefaultFCLocation());
			savedLayerChooser.setFileFilter(new JlfFilter());
		}
		return savedLayerChooser;
	}

	/** Filters file chooser selections down to readable files that have the .jlf extension */
	private final class JlfFilter extends FileFilter {
		public static final String EXT = ".jlf";
		public boolean accept(File f) {
			return f.isDirectory() || (f.isFile() && f.canRead() && f.getName().toLowerCase().endsWith(EXT));
		}
		public String getDescription() {
			return "JMARS Layer File (*" + EXT + ")";
		}
	}

	/** Returns the selected file on 'fc', with the file format suffix added if necessary */
	private static final File getSelectedFile(JFileChooser fc) {
		File f = fc.getSelectedFile();
		String name = f.getAbsolutePath();
		if (fc.getFileFilter() instanceof JlfFilter && !name.toLowerCase().trim().endsWith(JlfFilter.EXT)) {
			name = name + JlfFilter.EXT;
		}
		return new File(name);
	}

	/** Lets a user pick which layers to save and assigns a .jlf file to them */
	public class SaveLayersDialog {
		public final JDialog dlg = new JDialog(frame, "Save Layers...", true);
		private final Map<JCheckBox,LView> checked = new LinkedHashMap<JCheckBox,LView>();
		private JButton selAll;
		private boolean showFileChooserFlag = true;
		
		public SaveLayersDialog(boolean showFileChooser) {
			this.showFileChooserFlag = showFileChooser;
			initDialog();
		}
		/**
		 * Shows the layer chooser dialog right away, and then shows the file
		 * chooser so the user can pick the output file
		 */
		public SaveLayersDialog() {
			initDialog();
		}
		private void initDialog() {
			//List of checkboxes for loaded layers
			JPanel viewPnl = new JPanel();
			viewPnl.setLayout(new BoxLayout(viewPnl, BoxLayout.PAGE_AXIS));
			viewPnl.setBorder(new EmptyBorder(5,5,5,5));
			viewPnl.add(Box.createVerticalStrut(gap));
			// we get the list of lviews for the main window
			final List<LView> views = new ArrayList<LView>(Main.testDriver.mainWindow.viewList);
			Collections.reverse(views);
			for (LView view: views) {
				if (view.isOverlay()) {//overlay layers should not appear in the list
					continue;
				}
				String name;
				name = LManager.getLManager().getUniqueName(view);
				JCheckBox cb = new JCheckBox(name);
				checked.put(cb, view);
				Box hbox = Box.createHorizontalBox();
				hbox.add(Box.createHorizontalStrut(gap));
				hbox.add(cb);
				hbox.add(Box.createHorizontalGlue());
				hbox.add(Box.createHorizontalStrut(gap));
				viewPnl.add(hbox);
				viewPnl.add(Box.createVerticalStrut(gap));
			}
		// (Un)Select All Button
			selAll = new JButton("Select All".toUpperCase());
			selAll.setAlignmentX(Component.CENTER_ALIGNMENT);
			selAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//Select all entries then change button text to 'Unselect All'
					if(((JButton)e.getSource()).getText().equalsIgnoreCase("Select All")){
						for (JCheckBox ck : checked.keySet()){
							ck.setSelected(true);			
						}
						selAll.setText("Unselect All".toUpperCase());
					}
					//Unselect all entries and change button text to 'Select All'
					else{
						for (JCheckBox ck : checked.keySet()){
							ck.setSelected(false);			
						}
						selAll.setText("Select All".toUpperCase());
					}
				}
			});	
		// Save Button
			JButton ok = new JButton("Save".toUpperCase());
			ok.setAlignmentX(Component.CENTER_ALIGNMENT);
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveFile(); 
				}
			});
			
		//The scrollpane contains all the checkboxes (one for each layer)
			JScrollPane sp = new JScrollPane(viewPnl, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			sp.getVerticalScrollBar().setUnitIncrement(10);
		//The bottom panel holds the selAll Button and save button
			JPanel bottom = new JPanel();
			bottom.setLayout(new BoxLayout(bottom, BoxLayout.PAGE_AXIS));
			bottom.add(Box.createVerticalStrut(5));
			bottom.add(selAll);
			bottom.add(Box.createVerticalStrut(10));
			bottom.add(ok);
			bottom.add(Box.createVerticalStrut(5));
		//The final panel is built of the scroll pane and bottom panel
			// this enables the save and selectall buttons to always appear
			// at the bottom, no matter where the user scrolls
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.add(sp, BorderLayout.CENTER);
			panel.add(bottom, BorderLayout.SOUTH);
		//Add the panel to the dialog to get a preferred size
			dlg.add(panel);
		//Give the display a maximum height if necessary
			dlg.setPreferredSize(null);
			if(dlg.getPreferredSize().height > 400){
				dlg.setPreferredSize(new Dimension(dlg.getPreferredSize().width, 400));
			}
			
			dlg.pack();
		}
		/** Saves the file immediately */
		private void saveFile() {
			List<SavedLayer> layers = new ArrayList<SavedLayer>();
			for (JCheckBox cb: checked.keySet()) {
				if (cb.isSelected()) {
					// we determine visibility 
					layers.add(new SavedLayer(checked.get(cb)));
				}
			}
			if (!layers.isEmpty()) {
				if (!showFileChooserFlag) {
					String currentProduct = Config.get(Config.CONFIG_PRODUCT, "jmars");
					currentProduct = currentProduct.toLowerCase();
					File f = new File(Main.getJMarsPath()+currentProduct+"_"+Main.getCurrentBody().toLowerCase()+"_start.jlf");
					saveLayers(f, layers);
				} else {
					JFileChooser fc = getSavedLayerChooser();
					while (JFileChooser.APPROVE_OPTION == fc.showSaveDialog(frame)) {
						File f = getSelectedFile(fc);
						if (!f.exists() || JOptionPane.YES_OPTION == Util.showConfirmDialog("File exists, overwrite?", "File exists", JOptionPane.YES_NO_OPTION)) {
							saveLayers(f, layers);
							break;
						}			
					}
				}
			}
		}
		private void saveLayers(File f, List<SavedLayer> layers) {
			try {
				SavedLayer.save(layers, new FileOutputStream(f));
				dlg.dispose();
			} catch (Exception e1) {
				Util.showMessageDialog("Error saving layers: " + e1,
						"Error saving layers",
						JOptionPane.ERROR_MESSAGE);
				e1.printStackTrace();
			}	
		}
		public void setShowFileChooser(boolean show) {
			this.showFileChooserFlag = show;
		}
	}

	/** Lets a user pick a .jlf file to load layers from, and which layers in the file to load */
	public class LoadLayersDialog {
		private List<SavedLayer> layers;
		private Map<JCheckBox,SavedLayer> checked = new LinkedHashMap<JCheckBox,SavedLayer>();
		private JDialog dialog = new JDialog(frame, "Load Layers...", true);
		private JButton selAll;
		public LoadLayersDialog() {
			JFileChooser fc = getSavedLayerChooser();
			if (JFileChooser.APPROVE_OPTION == fc.showOpenDialog(Main.mainFrame)) {
				File file = getSelectedFile(fc);
				if (file.exists() && file.canRead()) {
					try {
						layers = SavedLayer.load(new FileInputStream(file));
						SavedLayer firstLayer = layers.get(0);
						String bodyName = firstLayer.bodyName;
						final AtomicReference<String> atomicBody = new AtomicReference<String>(bodyName);
						final AtomicBoolean validLayersFile = new AtomicBoolean(true);
						final AtomicBoolean cancelLoad = new AtomicBoolean(false);
						if (bodyName == null) {
							//here we do not have a body name for the layer. This is most likely because it was created before we started putting the bodyName in the layer.
							//we need to ask the user to select the associated body for this layer file (as we do for sessions)
							validLayersFile.set(false);
							final JDialog bodyDialog = new JDialog();
							bodyDialog.setModal(true);
							bodyDialog.setLayout(new FlowLayout());
							bodyDialog.setSize(300, 200);
							
							//build a list of bodies
							TreeMap<String, String[]> mapOfBodies = Util.getBodyList();
							ArrayList<String> bList = new ArrayList<String>();
							Iterator<Entry<String, String[]>> iter = mapOfBodies.entrySet().iterator();
							while (iter.hasNext()) {
								String[] bodyArr = (String[]) ((Entry<String, String[]>) iter.next()).getValue();
								for(String bdy : bodyArr) {
									bList.add(bdy.toUpperCase());
								}
							}
							JTextPane textPane = new JTextPane();
							textPane.setText("Please select the planetary body associated\n with this session. To avoid this in the future,\n please re-save the layers.");
							textPane.setSize(60, 60);
							bodyDialog.add(textPane);
							
							final JComboBox bodyCombo = new JComboBox(bList.toArray(new String[]{}));
							bodyCombo.setSize(100, 20);
							bodyDialog.add(bodyCombo);
							JButton selectButton = new JButton("Select".toUpperCase());
							selectButton.setSize(100,100);
							selectButton.addActionListener(new ActionListener() {
								
								public void actionPerformed(ActionEvent e) {
									//the user has selected a body, use that body
									String tempBody = (String) bodyCombo.getSelectedItem();
									atomicBody.set(tempBody);
									//if the body they selected was the same as the current body in Main, set the flag to true. It is checked later.
									if (tempBody.equalsIgnoreCase(Main.getBody())) {
										validLayersFile.set(true);
									}
									bodyDialog.dispose();
								}
							});
							bodyDialog.add(selectButton);
							
							JButton cancelButton = new JButton("Cancel".toUpperCase());
							cancelButton.setSize(100,100);
							cancelButton.addActionListener(new ActionListener() {
								
								public void actionPerformed(ActionEvent e) {
									cancelLoad.set(true);//the user has canceled and we need to not load the session
									bodyDialog.dispose();
								}
							});
							bodyDialog.add(cancelButton);
							
							
							Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
							Dimension d = bodyDialog.getSize();
							int x = (screen.width - d.width) / 2;
							int y = (screen.height - d.height) / 2;
							bodyDialog.setLocation(x, y);
							bodyDialog.setVisible(true);
						} else {
							//There is a bodyname in the file. If the bodyName is the same as the selected body, we can continue. If it is not, we need to 
							//stop the process.
							if (!bodyName.equalsIgnoreCase(Main.getBody())) {
								validLayersFile.set(false);
							}
						}
						if (!cancelLoad.get()) {
							//if they did not cancel, we get in here
							if (validLayersFile.get()) {
								//if we are in here, the current body is the same as the body for the layers file, show them the layer dialog
								showDialog();
							} else {
								//in here, the body for the layer file was different than the current body, display message that this can not be done
								Util.showMessageDialog("Error - File " + file.getAbsolutePath() + " is for planetary body: "+atomicBody.get()+". The current body is: "+Main.getBody()+".",
										"Layer file not for current planetary body",
										JOptionPane.ERROR_MESSAGE);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						Util.showMessageDialog("Error '" + e.getMessage() + "' with file " + file.getAbsolutePath(),
								"Error loading file",
								JOptionPane.ERROR_MESSAGE);
					}
				} else {
					Util.showMessageDialog("Cannot read " + file.getAbsolutePath(),
							"Error loading file",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		private void showDialog() {

		// This will populate the scroll pane with selection choices
			JPanel layersPnl = new JPanel();
			layersPnl.setLayout(new BoxLayout(layersPnl, BoxLayout.PAGE_AXIS));
			layersPnl.setBorder(new EmptyBorder(5,5,5,5));
			layersPnl.add(Box.createVerticalStrut(gap));
			for (SavedLayer layer: layers) {
				JCheckBox cb = new JCheckBox(layer.layerName);
				cb.setSelected(true);
				checked.put(cb, layer);
				Box cbbox = Box.createHorizontalBox();
				cbbox.add(Box.createHorizontalStrut(gap));
				cbbox.add(cb);
				cbbox.add(Box.createHorizontalGlue());
				cbbox.add(Box.createHorizontalStrut(gap));
				layersPnl.add(cbbox);
				layersPnl.add(Box.createVerticalStrut(gap));
			}

		// (Un)Select All Button
			selAll = new JButton("Unselect All".toUpperCase());
			selAll.setAlignmentX(Component.CENTER_ALIGNMENT);
			selAll.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
				//Select all entries then change button text to 'Unselect All'
					if(((JButton)e.getSource()).getText().equalsIgnoreCase("Select All")){
						for (JCheckBox ck : checked.keySet()){
							ck.setSelected(true);			
						}
						selAll.setText("Unselect All".toUpperCase());
					}
				//Unselect all entries and change button text to 'Select All'
					else{
						for (JCheckBox ck : checked.keySet()){
							ck.setSelected(false);			
						}
						selAll.setText("Select All".toUpperCase());
					}	
				}
			});
			
		// Load button	
			JButton load = new JButton("Load Selected".toUpperCase());
			load.setAlignmentX(Component.CENTER_ALIGNMENT);
			load.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					loadLayers();
				}
			});
			
		// Scrollpane which as all the checkbox options (layers to load)
			JScrollPane sp = new JScrollPane(layersPnl, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			sp.getVerticalScrollBar().setUnitIncrement(10);
			
		// Bottom holds the select and load buttons
			JPanel bot = new JPanel();
			bot.setLayout(new BoxLayout(bot, BoxLayout.PAGE_AXIS));
			bot.add(Box.createVerticalStrut(5));
			bot.add(selAll);
			bot.add(Box.createVerticalStrut(10));
			bot.add(load);
			bot.add(Box.createVerticalStrut(5));
		
		// Panel which holds everything	
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.add(sp, BorderLayout.CENTER);
			panel.add(bot, BorderLayout.SOUTH);

		// Add the panel to the dialog	
			dialog.add(panel);
		// Limit the maximium height if necessary
			dialog.setPreferredSize(null);
			if(dialog.getPreferredSize().height>400){
				dialog.setPreferredSize(new Dimension(dialog.getPreferredSize().width,400));
			}
			dialog.pack();
			dialog.setLocationRelativeTo(Main.testDriver);
			dialog.setVisible(true);
		}

		private void loadLayers() {
			List<JCheckBox> layersOrdered = new ArrayList<JCheckBox>(checked.keySet());
			Collections.reverse(layersOrdered);
			for (JCheckBox cb: layersOrdered) {
				if (cb.isSelected()) {
					try {
						checked.get(cb).materialize();
						cb.setSelected(false);
					} catch (Exception e) {
						e.printStackTrace();
						Util.showMessageDialog(e.getMessage(), "Error loading saved layer", JOptionPane.ERROR_MESSAGE);
					}
				}
			}

			// leave the dialog open if any of the layers were not saved
			for (JCheckBox cb: checked.keySet()) {
				if (cb.isSelected()) {
					return;
				}
			}

			dialog.dispose();
		}
	}
}
