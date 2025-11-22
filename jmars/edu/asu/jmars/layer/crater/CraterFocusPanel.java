package edu.asu.jmars.layer.crater;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import cookxml.cookswing.util.SpringLayoutUtilities;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.crater.profiler.ProfilerView;
import edu.asu.jmars.layer.shape2.FileChooser;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureProvider;
import edu.asu.jmars.layer.util.features.FeatureProviderFactory;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.SingleFeatureCollection;
import edu.asu.jmars.swing.ColorCombo;
import edu.asu.jmars.swing.ImportantMessagePanel;
import edu.asu.jmars.swing.OutlineIconButton;
import edu.asu.jmars.swing.TextFieldListener;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;

public class CraterFocusPanel extends FocusPanel {	private final AtomicReference<CraterSettings> craterSettings = new AtomicReference<CraterSettings>();
	
	private static final Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();   
	private static final Icon downIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CARET_DOWN_IMG
			         .withDisplayColor(imgColor)
			         .withWidth(10)
			         .withHeight(10)));

	private ProfilerView profiler;
	
	public void setCraterSettings(CraterSettings cs) {
		craterSettings.set(cs);
	}
	public CraterFocusPanel(final LView parent) {
		super(parent,false);
		
		Action importAction = new AbstractAction("Import".toUpperCase()) {
			public void actionPerformed(ActionEvent e){
				File[] files = null;
				FeatureProvider fp = null;
				
				while (true) {
					FileChooser shapeFileChooser = getFileChooser();
					
					FileFilter[] filters=shapeFileChooser.getChoosableFileFilters();

					// HACK to remove all file formats except the two we want
					for (FileFilter f : filters) {
						String desc = f.getDescription();
						if (!desc.startsWith("CSV") 
								&& !desc.startsWith("ESRI")) {
							shapeFileChooser.removeChoosableFileFilter(f);
						} else if (desc.startsWith("CSV")) {
							shapeFileChooser.setFileFilter(f);
						}
					}
					
					files = shapeFileChooser.chooseFile(CraterFocusPanel.this, "Load");
					fp = shapeFileChooser.getFeatureProvider();
					
					if (files == null || files.length == 0) {
						return;
					}
					if (files[0].exists()) {
						break;
					} else {
						Util.showMessageDialog("File does not exist", "Unable to import file", JOptionPane.ERROR_MESSAGE);
					}
				}
				
				List<String> errors = new ArrayList<String>();
				ArrayList<Crater> craters = ((CraterLayer)parent.getLayer()).settings.craters;

				CraterTableModel model = ((CraterTableModel)table.getUnsortedTableModel());

				for(File file: files) {
					try {
						FeatureCollection fc = fp.load(file.getPath());
						List<Field> schema = (List<Field>)fc.getSchema();
								
						boolean pathFound=false;
						boolean diameterFound=false;
						
						for (Field f : schema) {
							if (f.equals(Field.FIELD_PATH)) {
								pathFound=true;
							} else if (f.equals(Crater.DIAMETER) || f.equals(Crater.DIAMETER_INT)) {
								//allow DIAMETER_INT for backwards compatibility with older saved crater files
								diameterFound=true;
							} 
						}

						// Abort if we don't have the required fields
						if (!pathFound || !diameterFound) {//!pathFound || !diameterFound) {
							String msg = "Import failed: File must at a minimum contain the following fields: path, Diameter";
							msg = Util.lineWrap(msg, 55);
							Util.showMessageDialog(msg,
									"Load/Import Failed",
									JOptionPane.INFORMATION_MESSAGE
							);
							return;
						}
						
						CraterSettings settings=((CraterLayer)parent.getLayer()).settings;
						
						for(Feature f: (List<Feature>)fc.getFeatures()) {
							Crater newCrater = new Crater(f);
							craters.add(newCrater);
							model.addRow(newCrater);
							
							settings.colorToNotesMap.put(newCrater.getColor(), newCrater.getComment());
						}
						//find the notes tab, remove it and add it back
						JTabbedPane tempPane = (JTabbedPane) CraterFocusPanel.this;
						int selIndex = -1;
						for (int i=0; i<tempPane.getTabCount(); i++) {
							String tempTitle = tempPane.getTitleAt(i);
							if (tempTitle.equalsIgnoreCase("Notes")) {
								selIndex = i;
							}
						}
						if (selIndex > -1) {
							remove(selIndex);
						}
						add("Notes", new JScrollPane(createColorPanel()));
					} catch(Exception ex) {
						ex.printStackTrace();
						errors.add("Unable to load craters from \""+file.getPath()+"\"");
					}
				}
				
				((CraterLView)parent).drawCraters();
				((CraterLView)parent).repaint();

				((CraterLView)parent.getChild()).drawCraters();
				((CraterLView)parent.getChild()).repaint();
				
				if (!errors.isEmpty()) {
					Util.showMessageDialog(Util.join("\n", errors),
						"Unable to load data from the following files",
						JOptionPane.INFORMATION_MESSAGE);
				}
			}
		};
		
		Action exportCsvAction = new AbstractAction("Export as CSV...".toUpperCase()) {
			public void actionPerformed(ActionEvent e) {
				exportCratersToCSV(((CraterLayer)parent.getLayer()).settings.craters);
			}
		};
		
		
		Action configAction = new AbstractAction("Config".toUpperCase()) {
			public void actionPerformed(ActionEvent e) {
				
				JCheckBox latCheck = new JCheckBox("Latitude"); 
				JCheckBox lonCheck = new JCheckBox("Longitude");
				JCheckBox diaCheck = new JCheckBox("Diameter");
				JCheckBox colCheck = new JCheckBox("Color");
				JCheckBox notCheck = new JCheckBox("Note");
				JCheckBox usrCheck = new JCheckBox("User");
				
				latCheck.setSelected(((CraterLayer)parent.getLayer()).settings.inpExpLatitude);
				lonCheck.setSelected(((CraterLayer)parent.getLayer()).settings.inpExpLongitude);
				diaCheck.setSelected(((CraterLayer)parent.getLayer()).settings.inpExpDiameter);
				colCheck.setSelected(((CraterLayer)parent.getLayer()).settings.inpExpColor);
				notCheck.setSelected(((CraterLayer)parent.getLayer()).settings.inpExpNote);
				usrCheck.setSelected(((CraterLayer)parent.getLayer()).settings.inpExpUser);
				
				latCheck.setEnabled(false);
				lonCheck.setEnabled(false);
				diaCheck.setEnabled(false);

				JCheckBox [] options = new JCheckBox[6];
				options[0] = latCheck;
				options[1] = lonCheck;
				options[2] = diaCheck;
				options[3] = colCheck;
				options[4] = notCheck;
				options[5] = usrCheck;
				
				JOptionPane configPane = new JOptionPane(options, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
	                       null, null, JOptionPane.OK_OPTION);
				
				JDialog dialog = configPane.createDialog(CraterFocusPanel.this, "Import/Export Settings");
				dialog.setVisible(true);
				Object selectedValue = configPane.getValue();
				
			    if(selectedValue == null) { //user closed window without doing anything
			        ;	//nop
			    } else if(selectedValue instanceof Integer) { // user selected something
			    	int retVal = ((Integer)selectedValue).intValue();
			    	if (retVal == JOptionPane.CANCEL_OPTION) {
			    		; // do nothing
			    	} else if (retVal == JOptionPane.OK_OPTION) {
			    		((CraterLayer)parent.getLayer()).settings.inpExpColor = colCheck.isSelected();
			    		((CraterLayer)parent.getLayer()).settings.inpExpNote = notCheck.isSelected();
			    		((CraterLayer)parent.getLayer()).settings.inpExpUser = usrCheck.isSelected();
			    	}
				}				
			}
		};

		Action exportDiamAction = new AbstractAction("Export for Craterstats (old format)...".toUpperCase()) {
			public void actionPerformed(ActionEvent e) {
				JTextField title = new JTextField();
				JTextField area = new JTextField();
				double areaValue = 0;
				Object[] message = {
				    "Descriptive Title for Craterstats Header:", title,
				    "Study Area (in square km):", area
				};

				int option = Util.showConfirmDialog(message, "Enter Craterstats Info", JOptionPane.OK_CANCEL_OPTION);
				if (option == JOptionPane.OK_OPTION) {
					areaValue = Double.parseDouble(area.getText());
				} else {
				    return;
				}
				
				
				FeatureCollection fc = new SingleFeatureCollection();
				ArrayList<Crater> craters = ((CraterLayer)parent.getLayer()).settings.craters;
				for (Crater crater : craters) {
					fc.addFeature(crater.getFeature(((CraterLayer)parent.getLayer()).settings));
				}
				
				FeatureProviderDIAM diamProvider = new FeatureProviderDIAM();
				FileChooser chooser = new FileChooser();
				FileFilter filter = chooser.addFilter(diamProvider);
				chooser.setFileFilter(filter);

				File[] files = null;
				while (true) {
					files = chooser.chooseFile(CraterFocusPanel.this, "Export");
					diamProvider.setHeaderInfo(title.getText(), areaValue);
					if (files == null || files.length != 1) {
						return;
					}
					File[] toCheck = diamProvider.getExistingSaveToFiles(fc, files[0].getAbsolutePath());
					List<String> hits = new ArrayList<String>();
					for (File test: toCheck) {
						if (test.exists()) {
							hits.add(test.getName());
						}
					}
					if (hits.isEmpty() || JOptionPane.YES_OPTION ==
						Util.showConfirmDialog(
							"These file(s) exist already, overwrite?\n\n" + Util.join("\n", hits),
							"File exists", JOptionPane.YES_NO_OPTION)) {
						break;
					}
				}
				
				diamProvider.save(fc, files[0].getAbsolutePath());
			}
		};
		

		Action exportDiam2014Action = new AbstractAction("Export for Craterstats (2014 format)...".toUpperCase()) {
			public void actionPerformed(ActionEvent e) {
				JTextField title = new JTextField();
				JTextField area = new JTextField();
				double areaValue = 0;
				Object[] message = {
				    "Descriptive Title for Craterstats Header:", title,
				    "Study Area (in square km):", area
				};

				int option = Util.showConfirmDialog(message, "Enter Craterstats Info", JOptionPane.OK_CANCEL_OPTION);
				if (option == JOptionPane.OK_OPTION) {
					areaValue = Double.parseDouble(area.getText());
				} else {
				    return;
				}
				
				
				FeatureCollection fc = new SingleFeatureCollection();
				ArrayList<Crater> craters = ((CraterLayer)parent.getLayer()).settings.craters;
				for (Crater crater : craters) {
					fc.addFeature(crater.getFeature(((CraterLayer)parent.getLayer()).settings));
				}
				
				FeatureProviderDIAM2014 diamProvider = new FeatureProviderDIAM2014();
				FileChooser chooser = new FileChooser();
				FileFilter filter = chooser.addFilter(diamProvider);
				chooser.setFileFilter(filter);

				File[] files = null;
				while (true) {
					files = chooser.chooseFile(CraterFocusPanel.this, "Export");
					diamProvider.setHeaderInfo(title.getText(), areaValue);
					if (files == null || files.length != 1) {
						return;
					}
					File[] toCheck = diamProvider.getExistingSaveToFiles(fc, files[0].getAbsolutePath());
					List<String> hits = new ArrayList<String>();
					for (File test: toCheck) {
						if (test.exists()) {
							hits.add(test.getName());
						}
					}
					if (hits.isEmpty() || JOptionPane.YES_OPTION ==
						Util.showConfirmDialog(
							"These file(s) exist already, overwrite?\n\n" + Util.join("\n", hits),
							"File exists", JOptionPane.YES_NO_OPTION)) {
						break;
					}
				}
				
				diamProvider.save(fc, files[0].getAbsolutePath());
			}
		};


		JButton bImport = new JButton(importAction);

		JMenuItem bExportCsv = new JMenuItem(exportCsvAction);
		JMenuItem bExportDiam = new JMenuItem(exportDiamAction);
		JMenuItem bExportDiam2014 = new JMenuItem(exportDiam2014Action);

		JPopupMenu exportMnu = new JPopupMenu();
		exportMnu.add(bExportCsv);
		exportMnu.add(bExportDiam);
		exportMnu.add(bExportDiam2014);

		//Create the export button with text and icon		
        OutlineIconButton exportBtn = new OutlineIconButton("Export".toUpperCase(), downIcon);
        exportBtn.setHorizontalTextPosition(SwingConstants.LEFT);
        //add a dropdown type menu to the Export Button
        exportBtn.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent evt){
        		exportMnu.show(exportBtn, 0, exportBtn.getHeight());
        	}
        });

		JButton bConfig = new JButton(configAction);
		
		JPanel buttonPanel = new JPanel();
		
		buttonPanel.setLayout(new FlowLayout());
		
		buttonPanel.setBorder(BorderFactory.createTitledBorder("Import/Export Crater Data"));

		buttonPanel.add(bImport);
		buttonPanel.add(exportBtn);
		buttonPanel.add(bConfig);
	
		buttonPanel.setPreferredSize(new Dimension(440, 90));
		
		Action profileAct = new AbstractAction("Open Profile Viewer...") {
			public void actionPerformed(ActionEvent e) {
				if(profiler == null){
					profiler = new ProfilerView((CraterLView)parent);
				}
				//if a selection has been made while the profiler is either
				// not yet created, or not visible, then update the selection
				if(((CraterLView)parent).selectedCraters.size()==1){
					profiler.updateSelectionFromLView(((CraterLView)parent).selectedCraters.get(0));
				}
				profiler.showInFrame();
			}
		};
		JPanel profilePanel = new JPanel();
		profilePanel.setBorder(new EmptyBorder(10, 0, 10, 0));
		JButton profileBtn = new JButton(profileAct);
		profilePanel.add(profileBtn);
		
		
		JPanel craters = new JPanel();
		craters.setLayout(new BorderLayout());
		craters.add(buttonPanel, BorderLayout.NORTH);
		craters.add(createCraterTablePanel(), BorderLayout.CENTER);
		craters.add(profilePanel, BorderLayout.SOUTH);
		//do not put this panel in a scrollpane, use addTab instead of add
		addTab("Craters", craters);
	
		JPanel settings = new JPanel();
		settings.setLayout(new BorderLayout());
		JScrollPane sp = new JScrollPane(createSettingsPanel(), 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.getVerticalScrollBar().setUnitIncrement(15);
		sp.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);
		settings.add(sp, BorderLayout.CENTER);
		add("Settings", settings);

		JPanel settings2 = new JPanel();
		settings2.setLayout(new BorderLayout());
		JScrollPane sp2 = new JScrollPane(createDisplaySettingsPanel(), 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp2.getVerticalScrollBar().setUnitIncrement(15);
		sp2.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);
		settings2.add(sp2, BorderLayout.CENTER);
		add("Display", settings2);

		
		JPanel colors = new JPanel();
		colors.setLayout(new BorderLayout());
		colors.add(new JScrollPane(createColorPanel()),BorderLayout.CENTER);
		add("Notes", colors);
		
		
		parent.repaint();
//		parent.getChild().repaint();
	}
	
	private Action exportSelectedCsvAction = new AbstractAction("Export Selected Craters to CSV") {
		public void actionPerformed(ActionEvent e) {
			exportCratersToCSV(((CraterLView)parent).selectedCraters);
		}
	};
	
	protected void exportCratersToCSV(ArrayList<Crater> craters){
		FeatureCollection fc = new SingleFeatureCollection();
		for (Crater crater : craters) {
			fc.addFeature(crater.getFeature(((CraterLayer)parent.getLayer()).settings));
		}
		
		FileChooser shapeFileChooser = getFileChooser();
		
		FileFilter[] filters=shapeFileChooser.getChoosableFileFilters();

		// HACK to remove all file formats except the two we want
		for (FileFilter f : filters) {
			String desc = f.getDescription();
			if (!desc.startsWith("CSV") 
					&& !desc.startsWith("ESRI")) {
				shapeFileChooser.removeChoosableFileFilter(f);
			} else if (desc.startsWith("CSV")) {
				shapeFileChooser.setFileFilter(f);
			}
		}
		
		File[] files = null;
		FeatureProvider fp = null;
		while (true) {
			files = shapeFileChooser.chooseFile(CraterFocusPanel.this, "Export");
			fp = shapeFileChooser.getFeatureProvider();
			if (files == null || files.length != 1) {
				return;
			}
			File[] toCheck = fp.getExistingSaveToFiles(fc, files[0].getAbsolutePath());
			List<String> hits = new ArrayList<String>();
			for (File test: toCheck) {
				if (test.exists()) {
					hits.add(test.getName());
				}
			}
			if (hits.isEmpty() || JOptionPane.YES_OPTION ==
					Util.showConfirmDialog(
					"These file(s) exist already, overwrite?\n\n" + Util.join("\n", hits),
					"File exists", JOptionPane.YES_NO_OPTION)) {
				break;
			}
		}
		
		fp.save(fc, files[0].getAbsolutePath());
	}
	
	
	private FileChooser shapeFileChooser = null;
	
	private FileChooser getFileChooser(){
		if (shapeFileChooser == null){
			FeatureProviderFactory providerFactory = new FeatureProviderFactory(ShapeLayer.getFeatureProviderClassNames());
			shapeFileChooser = new FileChooser();
			Iterator fpit = providerFactory.getFileProviders().iterator();
			FileFilter selected = null;
			while (fpit.hasNext()) {
				FeatureProvider fp = (FeatureProvider)fpit.next();
				FileFilter f = shapeFileChooser.addFilter(fp);
				if (fp.getClass().getName().equals(Config.get("shape.filefactory.default"))) {
					selected = f;
				}
			}
			if (selected != null) {
				shapeFileChooser.setFileFilter(selected);
			}
		}
		return shapeFileChooser;
	}

	public JTextField newCraterSize = new JTextField();
	
	private JPanel createColorPanel() {
		JPanel panel = new JPanel();
	
	    panel.setLayout(new GridBagLayout());
	    panel.setBorder(new EmptyBorder(4,4,4,4));
	    int row = 0;
	    int pad = 4;
	    Insets in = new Insets(pad,pad,pad,pad);

	    CraterLView lviewToUse = (CraterLView) parent;
	    
	    if (lviewToUse.getChild()==null) {
	    	lviewToUse = (CraterLView) lviewToUse.getParentLView();
	    }
	    
		final CraterLView lview = lviewToUse;
		if (craterSettings.get() == null) {
			craterSettings.set(((CraterLayer)lview.getLayer()).settings);
		}
				
		ColorCombo colors = new ColorCombo();

		int numColors=colors.getItemCount();
		
		class ColorComment extends TextFieldListener {
			Color myColor;
			JTextField myField;
			
			ColorComment(Color color, JTextField field) {
				myColor=color;
				myField=field;
			}
			public void updateEvent() {
				for (Crater c: craterSettings.get().craters) {
					if (c.getColor().getRGB()==myColor.getRGB()) {
						c.setComment(myField.getText());
						table.getTableModel().refreshRow(c);
					}
				}			
				craterSettings.get().colorToNotesMap.put(myColor, myField.getText());
			}
		};
		
		for (int i=0; i<numColors; i++) {
			Color c = (Color)colors.getItemAt(i);

			JPanel colorPanel = new JPanel();
			colorPanel.setBorder(new LineBorder(Color.black, 1));
			colorPanel.setBackground(c);

			JTextField text = null;
			if (craterSettings.get().colorToNotesMap.get(c)!=null) {
				text = new JTextField(""+craterSettings.get().colorToNotesMap.get(c));
			} else {
				text = new JTextField();	
			}
			
			ColorComment changeComment = new ColorComment(c,text);
			text.addActionListener(changeComment);
			text.addFocusListener(changeComment);
			
		    panel.add(colorPanel, new GridBagConstraints(0,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    panel.add(text, new GridBagConstraints(1,row,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
		    row++;	
		    
		    craterSettings.get().colorToNotesMap.put(c, text.getText());
		}
		
	    panel.add(new JLabel(),new GridBagConstraints(0,row,2,1,1,1,GridBagConstraints.WEST,GridBagConstraints.BOTH,in,pad,pad));
	    
	    return panel;
	}
	
	final ColorCombo newCraterColor = new ColorCombo();	
	
	private JPanel createSettingsPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		
		final CraterLView lview = (CraterLView) parent;
		
		final CraterLayer layer = (CraterLayer)lview.getLayer();
		
		JLabel newCraterColorLbl = new JLabel("New crater color:");
		newCraterColorLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		newCraterColorLbl.setAlignmentY(Component.CENTER_ALIGNMENT);
		newCraterColor.setColor(new Color(layer.settings.nextColor.getRGB() & 0xFFFFFF, false));
		newCraterColor.setMaximumSize(new Dimension((int)(newCraterColorLbl.getMaximumSize().getWidth()), 26));
		newCraterColor.setAlignmentX(Component.LEFT_ALIGNMENT);
		newCraterColor.setAlignmentY(Component.CENTER_ALIGNMENT);
		newCraterColor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layer.settings.nextColor=
					new Color(newCraterColor.getColor().getRGB(), true);
					lview.repaint();
			}
		});		
				
		JLabel newCraterSizeLbl = new JLabel("New crater diameter (meters):");
		newCraterSizeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		newCraterSizeLbl.setAlignmentY(Component.CENTER_ALIGNMENT);
		newCraterSize.setText(String.valueOf(layer.settings.getNextSize()));
		newCraterSize.setMaximumSize(new Dimension((int)(newCraterSize.getMaximumSize().getWidth()), 26));
		newCraterSize.setAlignmentX(Component.LEFT_ALIGNMENT);
		newCraterSize.setAlignmentY(Component.CENTER_ALIGNMENT);
		
		TextFieldListener newSizeListener = new TextFieldListener(){
			public void updateEvent() {				
				double newSize = -1;
				try {
					newSize= Double.parseDouble(newCraterSize.getText().trim());
				} catch (NumberFormatException nfe) {
				}
				if (newSize>0) {
					layer.settings.setNextSize(newSize);
					newCraterSize.setText(String.valueOf(layer.settings.getNextSize()));
					lview.repaint();
				} else {
					newCraterSize.selectAll();
					newCraterSize.requestFocusInWindow();
				}
			}
		};
		
		newCraterSize.addActionListener(newSizeListener);
		newCraterSize.addFocusListener(newSizeListener);
		
		final JTextField newDefaultCraterSize = new JTextField();
		if ( layer.settings.getDefaultSize() > 0 ) {
			newDefaultCraterSize.setText(Double.toString(layer.settings.getDefaultSize()));
		} else {
			newDefaultCraterSize.setText("");
		}
		newDefaultCraterSize.setAlignmentX(Component.LEFT_ALIGNMENT);
		newDefaultCraterSize.setAlignmentY(Component.CENTER_ALIGNMENT);
		newDefaultCraterSize.setMaximumSize(new Dimension((int)(newDefaultCraterSize.getMaximumSize().getWidth()), 26));
		
		TextFieldListener defaultSizeListener = new TextFieldListener(){		
			public void updateEvent() {				
				double newSize = -1;
				try {
					newSize= Double.parseDouble(newDefaultCraterSize.getText());
				} catch (NumberFormatException nfe) {
				}
				if (newSize>0) {
					layer.settings.setDefaultSize(newSize);
					newDefaultCraterSize.setText(String.valueOf(layer.settings.getDefaultSize()));
					lview.repaint();
				} else {
					newDefaultCraterSize.selectAll();
					newDefaultCraterSize.requestFocusInWindow();
				}
			}
		};
		
		newDefaultCraterSize.addActionListener(defaultSizeListener);
		newDefaultCraterSize.addFocusListener(defaultSizeListener);
		
		final JCheckBox toggleDefaultCraterSizeReset = new JCheckBox("Default crater size:");
		toggleDefaultCraterSizeReset.setSelected(layer.settings.toggleDefaultCraterSizeReset);
		toggleDefaultCraterSizeReset.setToolTipText("After making a crater, this value is the default size of the next crater.");
		toggleDefaultCraterSizeReset.setAlignmentX(Component.LEFT_ALIGNMENT);
		toggleDefaultCraterSizeReset.setAlignmentY(Component.CENTER_ALIGNMENT);
		
		toggleDefaultCraterSizeReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double currentSize = -1;
				try {
					currentSize= Double.parseDouble(newCraterSize.getText());
				} catch (NumberFormatException nfe) {
					currentSize = -1;
				}
				if (currentSize == -1) {
					// user message joption
					toggleDefaultCraterSizeReset.setSelected(false);
				} else {
					layer.settings.setDefaultSize(layer.settings.getNextSize());
					newDefaultCraterSize.setText(String.valueOf(layer.settings.getDefaultSize()));	
				}
				layer.settings.toggleDefaultCraterSizeReset=toggleDefaultCraterSizeReset.isSelected();
					lview.drawCraters();
					if (!layer.settings.toggleDefaultCraterSizeReset) {
						layer.settings.setDefaultSize(-1);
						newDefaultCraterSize.setText("");	
						lview.drawSelectedCraters();
					}
					lview.repaint();
					((CraterLView)lview.getChild()).repaint();
				}
		});
			    
		toggleDefaultCraterSizeReset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double currentSize = -1;
				try {
					currentSize= Double.parseDouble(newCraterSize.getText());
				} catch (NumberFormatException nfe) {
					currentSize = -1;
				}
				if (currentSize == -1) {
					// user message joption
					toggleDefaultCraterSizeReset.setSelected(false);
				} else {
					layer.settings.setDefaultSize(layer.settings.getNextSize());
					newDefaultCraterSize.setText(String.valueOf(layer.settings.getDefaultSize()));	
				}
					layer.settings.toggleDefaultCraterSizeReset=toggleDefaultCraterSizeReset.isSelected();
					lview.drawCraters();
					if (!layer.settings.toggleDefaultCraterSizeReset) {
						layer.settings.setDefaultSize(-1);
						newDefaultCraterSize.setText("");	
						lview.drawSelectedCraters();
					}
					lview.repaint();
					((CraterLView)lview.getChild()).repaint();
				}
		});
	    
	    JPanel craterPanel = new JPanel();
		craterPanel.setLayout(new BoxLayout(craterPanel, BoxLayout.PAGE_AXIS));
		
		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new SpringLayout());			    		
		settingsPanel.add(newCraterColorLbl);
		settingsPanel.add(newCraterColor);
	    		
		settingsPanel.add(newCraterSizeLbl);
		settingsPanel.add(newCraterSize);
		
		settingsPanel.add(toggleDefaultCraterSizeReset);
		settingsPanel.add(newDefaultCraterSize);
	    
		SpringLayoutUtilities.makeCompactGrid(settingsPanel, 3, 2, 6, 6, 6, 6); 
		
		craterPanel.add(settingsPanel);
		panel.add(craterPanel);
		
		//create the increment controls
	    JPanel incrementPanel = new JPanel();
	    
	    final JRadioButton tenkm = new JRadioButton("10 km");
	    final JRadioButton km = new JRadioButton("1 km");
	    final JRadioButton hundred = new JRadioButton("100 m");
	    final JRadioButton ten = new JRadioButton("10 m");
	    final JRadioButton one = new JRadioButton("1 m");
	    final JRadioButton other = new JRadioButton("other (m)");
	    final JRadioButton spacer = new JRadioButton("");
	    final JRadioButton spacer1 = new JRadioButton("");
	    
	    spacer.setVisible(false); // don't want to see this, just space things out a bit
	    spacer1.setVisible(false); // don't want to see this, just space things out a bit
	    
	    tenkm.setActionCommand("10000");
	    km.setActionCommand("1000");
	    hundred.setActionCommand("100");
	    ten.setActionCommand("10");
	    one.setActionCommand("1");
	    other.setActionCommand("other (m)");
	    
	    ButtonGroup incrementGroup = new ButtonGroup();
	    incrementGroup.add(tenkm);
	    incrementGroup.add(km);
	    incrementGroup.add(hundred);
	    incrementGroup.add(ten);
	    incrementGroup.add(one);
	    incrementGroup.add(other);
	    
	    final JTextField otherField=new JTextField();
		otherField.setEditable(false);
		otherField.setEnabled(false);
		otherField.setMaximumSize(new Dimension(30,26));		
		otherField.setPreferredSize(new Dimension(10,26));		

		double meterIncrement = layer.settings.getMeterIncrement();
		
	    if (meterIncrement==10000) {
    		tenkm.setSelected(true);
	    } else if (meterIncrement==1000) {
    		km.setSelected(true);
	    } else if (meterIncrement==100) {
    		hundred.setSelected(true);
	    } else if (meterIncrement==10) {
    		ten.setSelected(true);
	    } else if (meterIncrement==1) {
    		one.setSelected(true);
	    } else {
    		if (layer.settings.getMeterIncrement() > 0) {
    			other.setSelected(true);
    			otherField.setText(Double.toString(layer.settings.getMeterIncrement()));
    		}
	    }
		
		TextFieldListener otherListener = new TextFieldListener(){
			public void updateEvent() {
				double newIncrement = -1;
				
				try {
					newIncrement= Double.parseDouble(otherField.getText());
				} catch (NumberFormatException nfe) {
//					System.out.println("other (m) focus NOT A VALID VALUE");
				}
				
				if (newIncrement>0) {
					lview.setMeterIncrement(newIncrement);
					other.setActionCommand(""+newIncrement);
				}

				lview.repaint();
			}
		};
		otherField.addFocusListener(otherListener);
		otherField.addActionListener(otherListener);
		
		other.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				double newIncrement = -1;
								
				try {
					if (other.isSelected()) {
						otherField.setEditable(true);
						otherField.setEnabled(true);
						String inc = otherField.getText();
						if (inc != null && inc.length() > 0) {
							newIncrement= Double.parseDouble(otherField.getText());
						}
					}
				} catch (NumberFormatException nfe) {
					System.out.println("NOT A VALID VALUE");
				}
				
				if (newIncrement>0) {
					lview.setMeterIncrement(newIncrement);
					other.setActionCommand(""+newIncrement);
				}

				lview.repaint();
			}
		});
		
	    incrementPanel.setLayout(new SpringLayout());
	    incrementPanel.setBorder(new TitledBorder("Increment Step Size"));
	    
	    incrementPanel.add(tenkm);
	    incrementPanel.add(km);	         
	    incrementPanel.add(hundred);     
	    incrementPanel.add(ten);         
	    incrementPanel.add(one);         
	    incrementPanel.add(spacer);         
	    incrementPanel.add(other);       
	    incrementPanel.add(otherField);  
	    incrementPanel.add(spacer1);         
	    
	    ActionListener incrementChange = new ActionListener(){		
			public void actionPerformed(ActionEvent e) {
				String command = e.getActionCommand();

				if (command.equalsIgnoreCase("other (m)")) {
					otherField.setEditable(true);
					otherField.setEnabled(true);
					
					double newIncrement = -1;
					try {
						newIncrement= Double.parseDouble(otherField.getText());
					} catch (NumberFormatException nfe) {
//						System.out.println("other (m) NOT A VALID VALUE");
					}
					
					if (newIncrement>0) {
						lview.setMeterIncrement(newIncrement);
					}
				} else {
					otherField.setEditable(false);
					otherField.setEnabled(true);
					double newIncrement = -1;
					try {
						newIncrement= Double.parseDouble(command);
					} catch (NumberFormatException nfe) {
//						System.out.println("NOT A VALID VALUE " + command);
					}
					
					if (newIncrement>0) {
						lview.setMeterIncrement(newIncrement);
					}
				}
			}
		};
	    
		tenkm.addActionListener(incrementChange);
		km.addActionListener(incrementChange);
		hundred.addActionListener(incrementChange);
		ten.addActionListener(incrementChange);
		one.addActionListener(incrementChange);
		SpringLayoutUtilities.makeCompactGrid(incrementPanel, 3, 3, 6, 6, 6, 6); 
		panel.add(incrementPanel);

		JPanel filterPanel = new JPanel();
		filterPanel.setLayout(new BoxLayout(filterPanel, BoxLayout.PAGE_AXIS));
			
		Font font = ThemeFont.getBold();
		
		ImportantMessagePanel notePanel = new ImportantMessagePanel("By default, each tick of the mousewheel is 10 increments.");
		notePanel.setMaximumSize(new Dimension(400,40));
	    filterPanel.add(notePanel);	
	    filterPanel.add(Box.createVerticalStrut(15));

	    JTextArea noteLabel2 = new JTextArea("Hold SHIFT while using the mousewheel for a single increment.");
	    noteLabel2.setEditable(false);
		noteLabel2.setWrapStyleWord(true);
		noteLabel2.setLineWrap(true);	
		noteLabel2.setFont(font);
		noteLabel2.setPreferredSize(new Dimension(0, 50));
		noteLabel2.setMaximumSize(new Dimension(400,50));
	    filterPanel.add(noteLabel2);	
	    
	    JTextArea noteLabel3 = new JTextArea("The + or - keys will increment the same as the mousewheel.");
	    noteLabel3.setEditable(false);
	    noteLabel3.setWrapStyleWord(true);
	    noteLabel3.setLineWrap(true);	 
		noteLabel3.setFont(font);
		noteLabel3.setPreferredSize(new Dimension(0, 50));
		noteLabel3.setMaximumSize(new Dimension(400,50));
	    filterPanel.add(noteLabel3);	
	    
	    JTextArea noteLabel4 = new JTextArea("Hold Ctrl while using the + or - keys for a single increment.");
		noteLabel4.setEditable(false);
		noteLabel4.setWrapStyleWord(true);
		noteLabel4.setLineWrap(true);		
		noteLabel4.setFont(font);
		noteLabel4.setPreferredSize(new Dimension(0, 50));
		noteLabel4.setMaximumSize(new Dimension(400,50));
	    filterPanel.add(noteLabel4);	
	    
	    JTextArea noteLabel5 = new JTextArea("Hold Ctrl while using the mousewheel to change the color of new craters.");
	    noteLabel5.setEditable(false);
	    noteLabel5.setWrapStyleWord(true);
	    noteLabel5.setLineWrap(true);	   
		noteLabel5.setFont(font);
		noteLabel5.setPreferredSize(new Dimension(0, 50));
		noteLabel5.setMaximumSize(new Dimension(400,50));
	    filterPanel.add(noteLabel5);	
	    		
		panel.add(filterPanel);
		
	    return panel;
	}
	
	private JPanel createDisplaySettingsPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		
		final CraterLView lview = (CraterLView) parent;
		
		final CraterLayer layer = (CraterLayer)lview.getLayer();
		
		JLabel alphaLbl = new JLabel("Fill alpha:");
		alphaLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		alphaLbl.setAlignmentY(Component.CENTER_ALIGNMENT);
		final JSlider alpha = new JSlider(0, 255, layer.settings.alpha);
		alpha.setMaximumSize(new Dimension((int)(alpha.getMaximumSize().getWidth()), 26));
		alpha.setMinimumSize(new Dimension(20, 26));
		alpha.setPreferredSize(new Dimension(40, 26));
		alpha.setAlignmentX(Component.LEFT_ALIGNMENT);
		alpha.setAlignmentY(Component.CENTER_ALIGNMENT);
	    alpha.addChangeListener(new ChangeListener() {
	    	public void stateChanged(ChangeEvent e) {
	    		if (alpha.getValueIsAdjusting()) {
	    			return;
	    		}
	    		
	    		layer.settings.alpha=alpha.getValue();
	    		((CraterLView)lview).drawCraters();
	    		((CraterLView)lview.getChild()).drawCraters();
	    		lview.repaint();
	    		lview.getChild().repaint();
	    		
	    		//increase state id, call 3d redraw
	    		layer.increaseStateId(0);
	    		if(ThreeDManager.isReady()){
	    			ThreeDManager.getInstance().updateDecalsForLView(lview, true);
	    		}
	    	}
	    });
				
		JLabel newCraterOutlineLbl = new JLabel("Crater outline thickness:");
		newCraterOutlineLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
		newCraterOutlineLbl.setAlignmentY(Component.CENTER_ALIGNMENT);
		if(layer.settings.craterLineThickness < 1.0 || layer.settings.craterLineThickness > 5.0) {
			layer.settings.craterLineThickness = 2.0;
		}
		
	    SpinnerNumberModel craterOutlineSpinner = new SpinnerNumberModel(layer.settings.craterLineThickness, 1.0, 5.0, 0.5);
	    final JSpinner outlineSpinner = new JSpinner(craterOutlineSpinner);
	    outlineSpinner.setToolTipText("Use this value to control the thickness of the crater outline");
	    outlineSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
	    outlineSpinner.setAlignmentY(Component.CENTER_ALIGNMENT);
	    outlineSpinner.setMaximumSize(new Dimension((int)(outlineSpinner.getMaximumSize().getWidth()), 26));
	    outlineSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				layer.settings.craterLineThickness=((Double)outlineSpinner.getModel().getValue()).floatValue();
				lview.repaint();
				((CraterLView)lview.getChild()).repaint();
				lview.drawCraters();
				
	    		//increase state id, call 3d redraw
	    		layer.increaseStateId(0);
	    		if(ThreeDManager.isReady()){
	    			ThreeDManager.getInstance().updateDecalsForLView(lview, true);
	    		}
			}
		});
	    
		final JCheckBox filterVisibleDiameter = new JCheckBox("Visible circle diameter value");
		filterVisibleDiameter.setToolTipText("If checked, the tool in the main view will show the diameter of the crater.");
		filterVisibleDiameter.setSelected(layer.settings.filterVisibleDiameter);
		filterVisibleDiameter.setAlignmentX(Component.LEFT_ALIGNMENT);
		filterVisibleDiameter.setAlignmentY(Component.CENTER_ALIGNMENT);
		filterVisibleDiameter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layer.settings.filterVisibleDiameter=filterVisibleDiameter.isSelected();
				lview.repaint();
				((CraterLView)lview.getChild()).repaint();
			}
		});
		
		
		
	    
	    final JTextField checkSpacer = new JTextField("Spacer");
	    checkSpacer.setAlignmentX(Component.LEFT_ALIGNMENT);
	    checkSpacer.setAlignmentY(Component.CENTER_ALIGNMENT);
	    checkSpacer.setMaximumSize(new Dimension((int)(checkSpacer.getMaximumSize().getWidth()), 26));	    
	    checkSpacer.setVisible(false); // don't want to see this, just space things out a bit
	    
		final JCheckBox filterCraterFill = new JCheckBox("Fill craters");
		filterCraterFill.setToolTipText("If checked, the tool in the main view will color fill the marked craters.");
		filterCraterFill.setSelected(layer.settings.filterCraterFill);
		filterCraterFill.setAlignmentX(Component.LEFT_ALIGNMENT);
		filterCraterFill.setAlignmentY(Component.CENTER_ALIGNMENT);
		filterCraterFill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layer.settings.filterCraterFill=filterCraterFill.isSelected();
				lview.drawCraters();
				lview.repaint();
				((CraterLView)lview.getChild()).repaint();
				
	    		//increase state id, call 3d redraw
	    		layer.increaseStateId(0);
	    		if(ThreeDManager.isReady()){
	    			ThreeDManager.getInstance().updateDecalsForLView(lview, true);
	    		}
			}
		});
		
	    final JTextField checkSpacer1 = new JTextField("Spacer1");
	    checkSpacer1.setAlignmentX(Component.LEFT_ALIGNMENT);
	    checkSpacer1.setAlignmentY(Component.CENTER_ALIGNMENT);
	    checkSpacer1.setMaximumSize(new Dimension((int)(checkSpacer1.getMaximumSize().getWidth()), 26));	    
	    checkSpacer1.setVisible(false); // don't want to see this, just space things out a bit
	    
	    JPanel craterPanel = new JPanel();
		craterPanel.setLayout(new BoxLayout(craterPanel, BoxLayout.PAGE_AXIS));
		
		JPanel settingsPanel = new JPanel();
		settingsPanel.setLayout(new SpringLayout());			    		

	    settingsPanel.add(filterCraterFill);
	    settingsPanel.add(checkSpacer1);

		settingsPanel.add(alphaLbl);
		settingsPanel.add(alpha);
				
		settingsPanel.add(newCraterOutlineLbl);
		settingsPanel.add(outlineSpinner);
	    
	    settingsPanel.add(filterVisibleDiameter);
	    settingsPanel.add(checkSpacer);

		SpinnerModel model = new SpinnerNumberModel(3, 1, 10, 1);
		final JSpinner digits = new JSpinner(model);
		
		digits.setMaximumSize(new Dimension((int)(digits.getMaximumSize().getWidth()), 26));
		digits.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int newDigits = (Integer)digits.getValue();
				table.numberRenderer.setDigits(newDigits);
				((CraterLView)lview).setDisplayDigits(newDigits);
			}
		});
		
		settingsPanel.add(new JLabel("Lon/Lat Decimal Places"));
		settingsPanel.add(digits);

		SpringLayoutUtilities.makeCompactGrid(settingsPanel, 5, 2, 6, 6, 6, 6); 						
		
		craterPanel.add(settingsPanel);
		panel.add(craterPanel);
		
		JPanel filterPanel = new JPanel();
		filterPanel.setLayout(new SpringLayout());
			    
		final JCheckBox filterMainView = new JCheckBox("Main View: Only draw craters matching filter");
		filterMainView.setPreferredSize(new Dimension(50, 23));
		filterMainView.setSelected(layer.settings.filterMainView);
	    filterPanel.add(filterMainView);	

		filterMainView.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layer.settings.filterMainView=filterMainView.isSelected();
				lview.drawCraters();
				if (!layer.settings.filterMainView) {
					lview.drawSelectedCraters();
				}
				lview.repaint();
				((CraterLView)lview.getChild()).repaint();
			}
		});
		
		final JCheckBox filterPanView = new JCheckBox("Panner: Only draw craters matching filter");
		filterPanView.setPreferredSize(new Dimension(50, 23));
		filterPanView.setSelected(layer.settings.filterPanView);
	    filterPanel.add(filterPanView);	
		
		filterPanView.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layer.settings.filterPanView=filterPanView.isSelected();
				lview.drawCraters();
				lview.repaint();
				((CraterLView)lview.getChild()).repaint();
			}
		});
		
		final JCheckBox filterTableView = new JCheckBox("Table: Only show craters matching filter");
		filterTableView.setPreferredSize(new Dimension(50, 23));
		filterTableView.setSelected(layer.settings.filterTableView);
	    filterPanel.add(filterTableView);	

		filterTableView.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layer.settings.filterTableView=filterTableView.isSelected();
				
				if (!layer.settings.filterTableView) {
					ArrayList<Crater> oldSelectedCraters = (ArrayList<Crater>)lview.selectedCraters.clone();
					table.getTableModel().removeAll();
					for (Crater c : layer.settings.craters) {
						table.getTableModel().addRow(c);
					}
					
		    		for (Crater crater: layer.settings.craters) {
		        		if (oldSelectedCraters.contains(crater)) {
		    				int row=table.getSorter().sortRow(table.getTableModel().getRow(crater));
		    				table.getSelectionModel().addSelectionInterval(row,row);
		        		}
		    		}    		
				}
								
				lview.drawCraters();
			}
		});		
		
		final JCheckBox filterTableCenterCrater = new JCheckBox("Table: Center crater on selection");
		filterTableCenterCrater.setPreferredSize(new Dimension(50, 23));
		filterTableCenterCrater.setSelected(layer.settings.filterTableCenterSelectedCrater);
	    filterPanel.add(filterTableCenterCrater);	

	    filterTableCenterCrater.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layer.settings.filterTableCenterSelectedCrater=filterTableCenterCrater.isSelected();								
			}
		});		
		
		final JCheckBox filter3dView = new JCheckBox("3D View: Only draw craters matching filter");
		filter3dView.setPreferredSize(new Dimension(50, 23));
		filter3dView.setSelected(layer.settings.filter3dView);
	    filterPanel.add(filter3dView);	
		
		filter3dView.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layer.settings.filter3dView=filter3dView.isSelected();
	    		//increase state id, call 3d redraw
	    		layer.increaseStateId(0);
	    		if(ThreeDManager.isReady()){
	    			ThreeDManager.getInstance().updateDecalsForLView(lview, true);
	    		}
			}
		});
		
		SpringLayoutUtilities.makeCompactGrid(filterPanel, 5, 1, 6, 6, 6, 6); 
		panel.add(filterPanel);
		
	    return panel;		
	}
	
	public CraterTable table;

    JTextField latMinField;
    JTextField latMaxField;
    JTextField lonMinField;
    JTextField lonMaxField;
    JTextField diaMinField;
    JTextField diaMaxField;
    JLabel matchField;
    JLabel totalField;
	
    public void updateStats(List<Crater> craters) {    	
    	int totalCnt = 0;
    	int matchCnt = 0;
    	
    	CraterLView lview = (CraterLView)parent;
		CraterSettings settings = ((CraterLayer)parent.getLayer()).settings;
    	
    	lview.matchingCraters.clear();    	
    	
    	ArrayList<Crater> oldSelectedCraters = (ArrayList<Crater>)lview.selectedCraters.clone();
    	
    	if (settings.filterTableView) {
    		table.getTableModel().removeAll();
    	}
    	    	
    	for (Crater crater : craters) {
    		totalCnt++;
    		
    		double latMin=getDoubleFromText(latMinField.getText());
    		
    		if (latMin!=Double.NaN && latMin>crater.getLat()) {
    			continue;
    		}

    		double latMax=getDoubleFromText(latMaxField.getText());
    		
    		if (latMax!=Double.NaN && latMax<crater.getLat()) {
    			continue;
    		}

    		double lonMin=getDoubleFromText(lonMinField.getText());
    		
    		if (lonMin!=Double.NaN && lonMin>crater.getLon()) {
    			continue;
    		}

    		double lonMax=getDoubleFromText(lonMaxField.getText());
    		
    		if (lonMax!=Double.NaN && lonMax<crater.getLon()) {
    			continue;
    		}

    		double diaMin=getDoubleFromText(diaMinField.getText());
    		
    		if (diaMin!=Double.NaN && diaMin>crater.getDiameter()) {
    			continue;
    		}

    		double diaMax=getDoubleFromText(diaMaxField.getText());
    		
    		if (diaMax!=Double.NaN && diaMax<crater.getDiameter()) {
    			continue;
    		}
    		
    		lview.matchingCraters.add(crater);
    		
        	if (settings.filterTableView) {
        		table.getTableModel().addRow(crater);
        	}
        	
    		matchCnt++;    		
    	}

    	if (settings.filterTableView) {
    		for (Crater crater: lview.matchingCraters) {
        		if (oldSelectedCraters.contains(crater)) {
    				int row=table.getSorter().sortRow(table.getTableModel().getRow(crater));
    				table.getSelectionModel().addSelectionInterval(row,row);
        		}
    		}    		
    	}
    	
    	matchField.setText(""+matchCnt);
    	totalField.setText(""+totalCnt);
    }
    
    public boolean meetsFilter(double lon, double lat, double diameter) {
		double latMin=getDoubleFromText(latMinField.getText());
		
		if (latMin!=Double.NaN && latMin>lat) {
			return false;
		}

		double latMax=getDoubleFromText(latMaxField.getText());
		
		if (latMax!=Double.NaN && latMax<lat) {
			return false;
		}

		double lonMin=getDoubleFromText(lonMinField.getText());
		
		if (lonMin!=Double.NaN && lonMin>lon) {
			return false;
		}

		double lonMax=getDoubleFromText(lonMaxField.getText());
		
		if (lonMax!=Double.NaN && lonMax<lon) {
			return false;
		}

		double diaMin=getDoubleFromText(diaMinField.getText());
		
		if (diaMin!=Double.NaN && diaMin>diameter) {
			return false;
		}

		double diaMax=getDoubleFromText(diaMaxField.getText());
		
		if (diaMax!=Double.NaN && diaMax<diameter) {
			return false;
		}

    	return true;
    }
    
    public boolean pointMeetsFilter(double lon, double lat) {
		double latMin=getDoubleFromText(latMinField.getText());
		
		if (latMin!=Double.NaN && latMin>lat) {
			return false;
		}

		double latMax=getDoubleFromText(latMaxField.getText());
		
		if (latMax!=Double.NaN && latMax<lat) {
			return false;
		}

		double lonMin=getDoubleFromText(lonMinField.getText());
		
		if (lonMin!=Double.NaN && lonMin>lon) {
			return false;
		}

		double lonMax=getDoubleFromText(lonMaxField.getText());
		
		if (lonMax!=Double.NaN && lonMax<lon) {
			return false;
		}
		
    	return true;
    }
    
    public double getDoubleFromText(String text) {
    	double val=Double.NaN;
    	
    	try {
    		val=Double.parseDouble(text);
    	} catch (NumberFormatException nfe) {
    		
    	}
    	
    	return val;	
    }
     
	private JPanel createCraterTablePanel() {
		JPanel panel = new JPanel();
		
	    JPanel bot = new JPanel();
	    bot.setBorder(new TitledBorder("Count Craters within Filter Parameters"));
	    
	    ActionListener updateValues = new ActionListener(){			
			public void actionPerformed(ActionEvent e) {
				final CraterLView lview = (CraterLView) parent;
				lview.drawCraters();	
				lview.drawSelectedCraters();
				lview.repaint();
	    		lview.getChild().repaint();
			}
		};
	    
	    latMinField = new JTextField();
	    latMaxField = new JTextField();
	    lonMinField = new JTextField();
	    lonMaxField = new JTextField();
	    diaMinField = new JTextField();
	    diaMaxField = new JTextField();
	    matchField = new JLabel("",JLabel.RIGHT);
	    totalField = new JLabel();
	    
	    latMinField.addActionListener(updateValues);
	    latMaxField.addActionListener(updateValues);
	    lonMinField.addActionListener(updateValues);
	    lonMaxField.addActionListener(updateValues);
	    diaMinField.addActionListener(updateValues);
	    diaMaxField.addActionListener(updateValues);
	    
		bot.setLayout(new SpringLayout());
		
		bot.add(new JLabel("Latitude"));
		bot.add(latMinField);
		bot.add(new JLabel(" to ", JLabel.CENTER));
		bot.add(latMaxField);

		bot.add(new JLabel("Longitude"));
		bot.add(lonMinField);
		bot.add(new JLabel(" to ", JLabel.CENTER));
		bot.add(lonMaxField);
		
		bot.add(new JLabel("Diameter (m)"));
		bot.add(diaMinField);
		bot.add(new JLabel(" to ", JLabel.CENTER));
		bot.add(diaMaxField);
		
		bot.add(new JLabel("Matching Craters"));
		bot.add(matchField);
		bot.add(new JLabel(" of ", JLabel.CENTER));
		bot.add(totalField);
		
		SpringLayoutUtilities.makeCompactGrid(bot, 4, 4, 6, 6, 6, 6);                			

	    table = new CraterTable((CraterLView)parent);
	    panel.setLayout(new BorderLayout());		    
	    JScrollPane sp = new JScrollPane(table);
	    panel.add(sp, BorderLayout.CENTER);
	    panel.add(bot, BorderLayout.SOUTH);
	    //remove "dangling" empty space
	    table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);    
	   
	    
	    return panel;
	}
		
	public ProfilerView getProfilerView(){
		return profiler;
	}
	
}
