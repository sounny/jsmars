package edu.asu.jmars.layer.landing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import cookxml.cookswing.util.SpringLayoutUtilities;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.shape2.FileChooser;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureProvider;
import edu.asu.jmars.layer.util.features.FeatureProviderFactory;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.SingleFeatureCollection;
import edu.asu.jmars.swing.ColorCombo;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;

public class LandingFocusPanel extends FocusPanel {
	
	private FileChooser shapeFileChooser = null;
	final ColorCombo newSiteColor = new ColorCombo();	
	public LandingSiteTable table;	
	public JLabel refreshLbl = new JLabel();
	
	public LandingFocusPanel(final LView parent) {
		super(parent,false);
		
		
		//Ellipse Panel
		JPanel landingEllipse = new JPanel();
		landingEllipse.setLayout(new BorderLayout());
		landingEllipse.add(createEllipseTablePanel(),BorderLayout.CENTER);
		landingEllipse.add(createEllipseNorthPanel(), BorderLayout.NORTH);
		add("Sites", landingEllipse);
	
		
		//TODO: rewrite createSettingsPanel to produce user defined stats columns
//		JPanel settings = new JPanel();
//		settings.setLayout(new BorderLayout());
//		JScrollPane sp = new JScrollPane(createSettingsPanel(), 
//				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
//				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//		sp.getVerticalScrollBar().setUnitIncrement(15);
//		sp.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);
//		settings.add(sp, BorderLayout.CENTER);
//		add("Settings", settings);

		JPanel display = new JPanel();
		display.setLayout(new BorderLayout());
		JScrollPane dispSP = new JScrollPane(createDisplaySettingsPanel());
		dispSP.getVerticalScrollBar().setUnitIncrement(15);
		dispSP.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);
		display.add(dispSP, BorderLayout.CENTER);
		add("Display", display);		
		parent.repaint();
	}
	
	
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
							&& !desc.startsWith("Geotools")) {
						shapeFileChooser.removeChoosableFileFilter(f);
					} else if (desc.startsWith("CSV")) {
						shapeFileChooser.setFileFilter(f);
					}
				}
				
				files = shapeFileChooser.chooseFile(LandingFocusPanel.this, "Load");
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
			ArrayList<LandingSite> sites = ((LandingLayer)parent.getLayer()).settings.sites;

			LandingSiteTableModel model = ((LandingSiteTableModel)table.getUnsortedTableModel());

			for(File file: files) {
				try {
					FeatureCollection fc = fp.load(file.getPath());
					List<Field> schema = (List<Field>)fc.getSchema();
							
					boolean pathFound=false;
					
					for (Field f : schema) {
						if (f.equals(Field.FIELD_PATH)) {
							pathFound=true;
						}
					}

					// Abort if we don't have the required fields
					if (!pathFound) {
						String msg = "Import failed: File must at a minimum contain the following field: path";
						msg = Util.lineWrap(msg, 55);
						Util.showMessageDialog(msg,
								"Load/Import Failed",
								JOptionPane.INFORMATION_MESSAGE
						);
						return;
					}
					
					
					for(Feature f: (List<Feature>)fc.getFeatures()) {
						LandingSite newSite = new LandingSite(f);
						sites.add(newSite);
						((LandingLView)parent).calculateMapSamplings(newSite, false);
					}

				} catch(Exception ex) {
					ex.printStackTrace();
					errors.add("Unable to load landing ellipses from \""+file.getPath()+"\"");
				}
			}
			
			((LandingLView)parent).drawSites();
			((LandingLView)parent).repaint();

			((LandingLView)parent.getChild()).drawSites();
			((LandingLView)parent.getChild()).repaint();
			
			if (!errors.isEmpty()) {
				Util.showMessageDialog(Util.join("\n", errors),
					"Unable to load data from the following files",
					JOptionPane.INFORMATION_MESSAGE);
			}
		}
	};
	
	Action exportAction = new AbstractAction("Export".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			FeatureCollection fc = new SingleFeatureCollection();
			ArrayList<LandingSite> sites = ((LandingLayer)parent.getLayer()).settings.sites;
			for (LandingSite site : sites) {
				fc.addFeature(site.getFeature(((LandingLayer)parent.getLayer()).settings));
			}
			
			FileChooser shapeFileChooser = getFileChooser();
			
			FileFilter[] filters=shapeFileChooser.getChoosableFileFilters();

			// HACK to remove all file formats except the two we want
			for (FileFilter f : filters) {
				String desc = f.getDescription();
				if (!desc.startsWith("CSV") 
						&& !desc.startsWith("Geotools")) {
					shapeFileChooser.removeChoosableFileFilter(f);
				} else if (desc.startsWith("CSV")) {
					shapeFileChooser.setFileFilter(f);
				}
			}
			
			File[] files = null;
			FeatureProvider fp = null;
			while (true) {
				files = shapeFileChooser.chooseFile(LandingFocusPanel.this, "Export");
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
	};
	
	Action configAction = new AbstractAction("Config".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			
			JCheckBox notCheck = new JCheckBox("Note");
			JCheckBox colCheck = new JCheckBox("Color");
			JCheckBox latCheck = new JCheckBox("Latitude"); 
			JCheckBox lonCheck = new JCheckBox("Longitude");
			JCheckBox horCheck = new JCheckBox("Horizontal Axis");
			JCheckBox verCheck = new JCheckBox("Vertical Axis");
			JCheckBox stsCheck = new JCheckBox("All Stats");
			JCheckBox usrCheck = new JCheckBox("User");
			
			latCheck.setSelected(((LandingLayer)parent.getLayer()).settings.inpExpLatitude);
			lonCheck.setSelected(((LandingLayer)parent.getLayer()).settings.inpExpLongitude);
			horCheck.setSelected(((LandingLayer)parent.getLayer()).settings.inpExpHorAxis);
			verCheck.setSelected(((LandingLayer)parent.getLayer()).settings.inpExpVerAxis);
			colCheck.setSelected(((LandingLayer)parent.getLayer()).settings.inpExpColor);
			notCheck.setSelected(((LandingLayer)parent.getLayer()).settings.inpExpNote);
			stsCheck.setSelected(((LandingLayer)parent.getLayer()).settings.inpExpStats);
			usrCheck.setSelected(((LandingLayer)parent.getLayer()).settings.inpExpUser);
			

			JCheckBox [] options = new JCheckBox[10];
			options[0] = notCheck;
			options[1] = colCheck;
			options[2] = latCheck;
			options[3] = lonCheck;
			options[4] = horCheck;
			options[5] = verCheck;
			options[6] = stsCheck;
			options[7] = usrCheck;
			
			JOptionPane configPane = new JOptionPane(options, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
                       null, null, JOptionPane.OK_OPTION);
			
			JDialog dialog = configPane.createDialog(LandingFocusPanel.this, "Import/Export Settings");
			dialog.setVisible(true);
			Object selectedValue = configPane.getValue();
			
		    if(selectedValue == null) { //user closed window without doing anything
		        	//nop
		    } else if(selectedValue instanceof Integer) { // user selected something
		    	int retVal = ((Integer)selectedValue).intValue();
		    	if (retVal == JOptionPane.CANCEL_OPTION) {
		    		 // do nothing
		    	} else if (retVal == JOptionPane.OK_OPTION) {
		    		((LandingLayer)parent.getLayer()).settings.inpExpColor = colCheck.isSelected();
		    		((LandingLayer)parent.getLayer()).settings.inpExpNote = notCheck.isSelected();
		    		((LandingLayer)parent.getLayer()).settings.inpExpStats = stsCheck.isSelected();
		    		((LandingLayer)parent.getLayer()).settings.inpExpUser = usrCheck.isSelected();
		    	}
			}				
		}
	};
	
	
	Action reloadAction = new AbstractAction("Refresh Stats".toUpperCase()){
		public void actionPerformed(ActionEvent e) {
			ArrayList<LandingSite> sites = ((LandingLayer)((LandingLView)parent).getLayer()).settings.sites;
			for(LandingSite s : sites){
				((LandingLView)parent).calculateMapSamplings(s, true);
				refreshLbl.setText("");
			}
		}
	};
	

	
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


	
//	private JPanel createSettingsPanel() {
//		//TODO: make stats column adders
//		
//		
//		
//		
//	 
//		JPanel panel = new JPanel();
//		
//	    return panel;
//	}
	
	
	
	private JPanel createDisplaySettingsPanel() {
		
		final LandingLView lview = (LandingLView) parent;
		final LandingLayer layer = (LandingLayer)lview.getLayer();

	//Display panel componenets
		//color combobox and label
		JLabel newSiteColorLbl = new JLabel("New site color:");
		newSiteColor.setColor(new Color(layer.settings.nextColor.getRGB() & 0xFFFFFF, false));
		newSiteColor.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layer.settings.nextColor=
					new Color(newSiteColor.getColor().getRGB(), true);
					lview.repaint();
			}
		});	
		newSiteColor.setPreferredSize(new Dimension(100,25));
		JPanel colorPnl = new JPanel();
		colorPnl.add(newSiteColorLbl);
		colorPnl.add(newSiteColor);
		
		//fill sites checkbox
		final JCheckBox filterSiteFill = new JCheckBox("Fill sites");
		filterSiteFill.setToolTipText("If checked, the tool in the main view will color fill the marked sites.");
		filterSiteFill.setSelected(layer.settings.filterSiteFill);
		filterSiteFill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layer.settings.filterSiteFill=filterSiteFill.isSelected();
				lview.drawSites();
				lview.repaint();
				((LandingLView)lview.getChild()).repaint();
				
				//increase state id, call 3d redraw
	    		layer.increaseStateId(0);
	    		if(ThreeDManager.isReady()){
	    			ThreeDManager.getInstance().updateDecalsForLView(lview, true);
	    		}
			}
		});
		
		//fill alpha label and slider
		JLabel alphaLbl = new JLabel("Fill alpha:");
		final JSlider alpha = new JSlider(0, 255, layer.settings.alpha);
	    alpha.addChangeListener(new ChangeListener() {
	    	public void stateChanged(ChangeEvent e) {
	    		if (alpha.getValueIsAdjusting()) {
	    			return;
	    		}    		
	    		layer.settings.alpha=alpha.getValue();
	    		((LandingLView)lview).drawSites();
	    		((LandingLView)lview.getChild()).drawSites();
	    		lview.repaint();
	    		lview.getChild().repaint();
	    		
	    		//increase state id, call 3d redraw
	    		layer.increaseStateId(0);
	    		if(ThreeDManager.isReady()){
	    			ThreeDManager.getInstance().updateDecalsForLView(lview, true);
	    		}
	    	}
	    });
	    alpha.setPreferredSize(new Dimension(150,20));
	    JPanel alphaPnl = new JPanel();
	    alphaPnl.add(alphaLbl);
	    alphaPnl.add(alpha);
				
	    //outline thickness label and spinner
		JLabel newSiteOutlineLbl = new JLabel("Site outline thickness:");
		if(layer.settings.siteLineThickness < 1.0 || layer.settings.siteLineThickness > 5.0) {
			layer.settings.siteLineThickness = 2.0;
		}
	    SpinnerNumberModel siteOutlineSpinner = new SpinnerNumberModel(layer.settings.siteLineThickness, 1.0, 5.0, 0.5);
	    final JSpinner outlineSpinner = new JSpinner(siteOutlineSpinner);
	    outlineSpinner.setToolTipText("Use this value to control the thickness of the site outline");
	    outlineSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				layer.settings.siteLineThickness=((Double)outlineSpinner.getModel().getValue()).floatValue();
				lview.repaint();
				((LandingLView)lview.getChild()).repaint();
				lview.drawSites();
				
				//increase state id, call 3d redraw
	    		layer.increaseStateId(0);
	    		if(ThreeDManager.isReady()){
	    			ThreeDManager.getInstance().updateDecalsForLView(lview, true);
	    		}
			}
		});
	    outlineSpinner.setPreferredSize(new Dimension(50,22));
		JPanel outlinePnl = new JPanel();
		outlinePnl.add(newSiteOutlineLbl);
		outlinePnl.add(outlineSpinner);
	    
	    //visible diameter checkbox
		final JCheckBox filterVisibleDiameter = new JCheckBox("Visible site axes values");
		filterVisibleDiameter.setToolTipText("If checked, the tool in the main view will show the axes of the ellipse.");
		filterVisibleDiameter.setSelected(layer.settings.filterVisibleDiameter);
		filterVisibleDiameter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layer.settings.filterVisibleDiameter=filterVisibleDiameter.isSelected();
				lview.repaint();
				((LandingLView)lview.getChild()).repaint();
			}
		});
		
		//decimal places label and spinner
		JLabel decimalLbl = new JLabel("Lat/Lon Decimal Places:");
		SpinnerModel model = new SpinnerNumberModel(3, 1, 10, 1);
		final JSpinner digits = new JSpinner(model);
		digits.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				table.numberRenderer.setDigits((Integer)digits.getValue());				
			}
		});
		digits.setPreferredSize(new Dimension(40, 22));
		JPanel decimalPnl = new JPanel();
		decimalPnl.add(decimalLbl);
		decimalPnl.add(Box.createHorizontalStrut(10));
		decimalPnl.add(digits);

		//outline style label and dropdown
		JLabel styleLbl = new JLabel("Site outline style:");
		String styles[] = {"Solid","Dashed"};
		final JComboBox styleBox = new JComboBox(styles);
		styleBox.setToolTipText("Change the display style of the temporary ellipse outline");
		styleBox.setSelectedIndex(layer.settings.styleIndex);
		styleBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				layer.settings.styleIndex = styleBox.getSelectedIndex();
				lview.repaint();
				lview.drawSites();
			}
		});
		JPanel stylePnl = new JPanel();
		stylePnl.add(styleLbl);
		stylePnl.add(Box.createHorizontalStrut(2));
		stylePnl.add(styleBox);

		
		//display panel
		JPanel displayPanel = new JPanel();
		displayPanel.setLayout(new SpringLayout());
		displayPanel.setBorder(new TitledBorder("Display Settings"));

	    displayPanel.add(colorPnl);
	    displayPanel.add(filterSiteFill);
	    displayPanel.add(alphaPnl);
	    displayPanel.add(outlinePnl);
	    displayPanel.add(filterVisibleDiameter);
	    displayPanel.add(decimalPnl);
	    displayPanel.add(stylePnl);
	    //this part is silly but you cant have empty cells in spring layout
	    displayPanel.add(new JLabel());
	    displayPanel.add(new JLabel());
	    
		SpringLayoutUtilities.makeCompactGrid(displayPanel, 3, 3, 6, 6, 6, 6);
		JPanel panel1 = new JPanel();		
		panel1.add(displayPanel);
		    
	//Entire panel to be returned
		JPanel panel = new JPanel();	
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		
		panel.add(Box.createVerticalGlue());
		panel.add(panel1);
		panel.add(Box.createVerticalGlue());
		
	    return panel;		
	}
	


	private JPanel createEllipseNorthPanel(){
		JButton bImport = new JButton(importAction);
		JButton bExport = new JButton(exportAction);
		JButton bConfig = new JButton(configAction);
		JButton bReload = new JButton(reloadAction);
		
		JPanel inpExpPanel = new JPanel();
		inpExpPanel.setBorder(BorderFactory.
		                    createTitledBorder(BorderFactory.createEmptyBorder(5,5,5,5), 
				   "Import/Export Site Data"));


		inpExpPanel.add(bImport);
		inpExpPanel.add(bExport);
		inpExpPanel.add(bConfig);
		
		JPanel statsPanel = new JPanel();
		statsPanel.setBorder(BorderFactory.
				   createTitledBorder(BorderFactory.createEmptyBorder(5,5,5,5), 
				   "Map Sampling Data"));
		refreshLbl.setForeground(Color.RED);
		statsPanel.add(bReload);
		statsPanel.add(refreshLbl);

		JPanel northPanel = new JPanel();
		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.LINE_AXIS));
		northPanel.add(Box.createHorizontalGlue());
		northPanel.add(inpExpPanel);
		northPanel.add(Box.createHorizontalGlue());
		northPanel.add(statsPanel);
		northPanel.add(Box.createHorizontalGlue());
		
		return northPanel;
	}
     
	private JPanel createEllipseTablePanel() {
		JPanel panel = new JPanel();          			

		    table = new LandingSiteTable((LandingLView)parent);
		    panel.setLayout(new BorderLayout());
		    panel.add(new JScrollPane(table), BorderLayout.CENTER);
		    
		    return panel;
	}
		
}
