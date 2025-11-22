package edu.asu.jmars.layer.shape2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.MouseInfo;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import com.thoughtworks.xstream.XStream;
import edu.asu.jmars.Main;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureProvider;
import edu.asu.jmars.layer.util.features.FeatureProviderNomenclature;
import edu.asu.jmars.layer.util.features.FeatureSQL;
import edu.asu.jmars.layer.util.features.FeatureTableAdapter;
import edu.asu.jmars.layer.util.features.FeatureTableModel;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.GeomSource;
import edu.asu.jmars.layer.util.features.MemoryFeatureIndex;
import edu.asu.jmars.layer.util.features.MultiFeatureCollection;
import edu.asu.jmars.layer.util.features.ScriptFileChooser;
import edu.asu.jmars.layer.util.features.SingleFeatureCollection;
import edu.asu.jmars.layer.util.features.Style;
import edu.asu.jmars.layer.util.features.StyleSource;
import edu.asu.jmars.layer.util.features.Styles;
import edu.asu.jmars.layer.util.features.ZOrderMenu;
import edu.asu.jmars.layer.util.filetable.FileTable;
import edu.asu.jmars.layer.util.filetable.FileTableModel;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeMenuBar;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeTable;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.stable.ColorCellEditor;


public class ShapeFocusPanel extends FocusPanel {
	final private static DebugLog log = DebugLog.instance(); 
	private static final String defaultProviderKey = "shape.filefactory.default";
	
	public static final String saveSelectedFilesAsActionName = "Save Selected Files As";
	public static final String saveSelectedFeaturesAsActionName = "Save Selected Features As";
	public static final String saveAllFeaturesAsActionName = "Save All Features As";
	
    // File and feature maintainance Menu Items, where one may load, save, yes and even
    // delete files and features.
    JMenuItem deleteSelectedFilesMenuItem            = new JMenuItem("Delete Selected Files");
    JMenuItem saveSelectedFilesMenuItem              = new JMenuItem("Save Selected Files");
    JMenuItem saveSelectedFilesToFileAsMenuItem      = new JMenuItem(saveSelectedFilesAsActionName);
    
    JMenuItem saveSelectedFeaturesToFileMenuItem     = new JMenuItem(saveSelectedFeaturesAsActionName);
    JMenuItem saveAllFeaturesToFileAsMenuItem        = new JMenuItem(saveAllFeaturesAsActionName);
    JMenuItem addTooltipMenuItem					 = new JMenuItem("Tooltips...");
    
    JMenuItem   featureCommandMenuItem         = new JMenuItem("Edit Script");
    JMenuItem   featureLoadScriptsMenuItem     = new JMenuItem("Load & Run Script");
    
    JPanel 		   mainPanel = new JPanel(); //everything is added to this, this is added to focus panel
    JPanel         featurePanel; // FeatureTable container
    JScrollPane    featureTableScrollPane;
    JPanel         filePanel; // FileTable container
    JScrollPane    fileTableScrollPane;
    JSplitPane     splitPane; // SplitPane containing featurePanel and filePanel
	
    List<JDialog>           openDialogList = new ArrayList<JDialog>(); // List of popup dialogs currently active

    FileChooser loadSaveFileChooser;
    ScriptFileChooser shapeScriptFileChooser;
    
    ShapeLView     shapeLView;
    ShapeLayer     shapeLayer;
    
    private STable featureTable;
	private final StylesStore stylesStore = new StylesStore();
	
	public void setLView(ShapeLView lview) {
		parent = shapeLView = lview;
	}
	
	public ShapeFocusPanel(ShapeLayer parent, ShapeLView lview) {
		super(lview);
		mainPanel.setLayout(new BorderLayout());			
		setLView(lview);
		
		shapeLayer = parent;
		loadSaveFileChooser = new FileChooser();
		
		// use jmars.config key to decide if we set up the chooser in the home
		// or working directories
		String chooserPathType = Config.get("shape.chooser.path", "home");
		if (chooserPathType.trim().toLowerCase().equals("working")) {
			loadSaveFileChooser.setStartingDir(new File("."));
		} else if (chooserPathType.trim().toLowerCase().equals("home")) {
			loadSaveFileChooser.setStartingDir(new File(Util.getDefaultFCLocation()));
		}
		
		List<FeatureProvider> fileProviders = (List<FeatureProvider>)shapeLayer.getProviderFactory().getFileProviders();
		FileFilter selected = null;
		String defaultProviderClass = Config.get(defaultProviderKey);
		for (FeatureProvider fp: fileProviders) {
			FileFilter f = loadSaveFileChooser.addFilter(fp);
			if (fp.getClass().getName().equals(defaultProviderClass)) {
				selected = f;
			}
		}
		if (selected != null) {
			loadSaveFileChooser.setFileFilter(selected);
		}
		
		shapeScriptFileChooser = new ScriptFileChooser();

	    // build menu bar.
	    mainPanel.add(getMenuBar(),BorderLayout.NORTH);

	    // tie menus to corresponding actions
	    initMenuActions();

		// the mfc should be produced by the layer, not the file table!
		FeatureTableAdapter ft = new FeatureTableAdapter(shapeLayer.getFeatureCollection(), shapeLayer.selections, shapeLayer.getHistory(), shapeLayer);
		featureTable = ft.getTable();
		featureTable.setDescription("Feature");
		featureTable.setParentFrame(this.parentFrame);
		
	    // add FileTable and the merged FeatureTable.
	    filePanel = buildFilePanel();
	    featurePanel = buildFeaturePanel();	    
	    // lets user load shape files if it's a custom shape layer and it's not read only, otherwise this is disabled
		if (lview.layerName.equals("") || lview.layerName.equals("Custom Shape Layer") || !shapeLayer.isReadOnly){
		    splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, filePanel, featurePanel);
		    splitPane.setPreferredSize(new Dimension(400,500));
		    splitPane.setResizeWeight(.5);		   
		    mainPanel.add(splitPane, BorderLayout.CENTER);
	    }
	    else{
	    	mainPanel.add(featurePanel, BorderLayout.CENTER);
	    }
		installContextMenuOnFeatureTable(featureTable, featureTableScrollPane);
		installDoubleClickHandlerOnFileTable(shapeLayer.getFileTable());
		installContextMenuOnFileTable(shapeLayer.getFileTable(), fileTableScrollPane);
		
		add("Adjustments",mainPanel);
	}
	
	private void installContextMenuOnFeatureTable(
			final STable featureTable,
			final JScrollPane featureTableScrollPane)
	{
		final FeatureCollection fc = ((FeatureTableModel)featureTable.getUnsortedTableModel()).getFeatureCollection();
		final JMenuItem deleteSelectedFeaturesMenuItem = new JMenuItem(new DelSelectedFeaturesAction(fc));
		final JMenuItem centerMenuItem = new JMenuItem(new CenterOnFeatureAction());
		final JMenuItem multiEditMenuItem = new JMenuItem(new MultiEditAction(featureTable));
		final JMenuItem saveSelectedFeaturesToFileMenuItem = new JMenuItem(
				new SaveAsAction("Save Selected Features As", fc));
		final JMenuItem resetColumnToDefaultMenuItem = new JMenuItem(
				new ResetToDefaultAction("Reset column of selected rows to 'Default'", featureTable));

		final JMenuItem copyUrlToClipboardMenuItem = new JMenuItem(
				new CopyUrl("Copy url to clipboard", featureTable));
		
		
		featureTable.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 1){
					int[] selectedRows = featureTable.getSelectedRows();
					
					if (selectedRows.length == 0)
						return;
					
					final JPopupMenu popup = new JPopupMenu();
					popup.add(centerMenuItem);
					
					// Install Z-order menu for the main FeatureTable in the FocusPanel only.
					if (getFeatureTable() == featureTable && fc instanceof MultiFeatureCollection) {
						popup.add(new ZOrderMenu("Z Order", (MultiFeatureCollection)fc, shapeLayer.selections, featureTable.getSorter()));
					}
					
					// If layer is not read-only (columns are editable) then allow
					// deleting and multi-editing.
					if (!shapeLayer.isReadOnly){
						popup.add(multiEditMenuItem);
						popup.add(deleteSelectedFeaturesMenuItem);
						popup.add(resetColumnToDefaultMenuItem);
					}

					popup.add(saveSelectedFeaturesToFileMenuItem);
					
					//conditionally add "copy URL to clipboard"
					int col = featureTable.columnAtPoint(e.getPoint());
					int row = featureTable.rowAtPoint(e.getPoint());
					if (featureTable.isUrlCellRenderer(col)) {
						Object value = featureTable.getValueAt(row, col);
						if (value != null && featureTable.isValidUrl(value.toString())) {
							popup.add(copyUrlToClipboardMenuItem);
						}
					}

					// bring up the popup, but be sure it goes to the cursor position of 
					// the PANEL, not the position in the table.  If we don't do this, then
					// for a large table, the popup will try to draw itself beyond the 
					// screen.
					Point2D p = getScrollPaneRelativeCoordinates(e.getPoint(), featureTableScrollPane);
					popup.show( featureTableScrollPane, (int)p.getX(), (int)p.getY());
				}
			}

			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					((MultiEditAction) multiEditMenuItem.getAction()).setTableMouseEvent(e);
					((ResetToDefaultAction) resetColumnToDefaultMenuItem.getAction()).setTableMouseEvent(e);
					((CopyUrl) copyUrlToClipboardMenuItem.getAction()).setTableMouseEvent(e);
				}
			}
		});
		
		featureTable.addMouseListener(new MyUrlMouseListener());
	}
	
	/**
	 * Registers the specified dialog with the FocusPanel so that the FocusPanel is able
	 * to destroy these dialogs on a FocusPanel dispose.
	 * 
	 * @param dialog
	 */
	protected void registerDialogForAutoCleanup(final JDialog dialog){
		openDialogList.add(dialog);
		dialog.addWindowListener(new WindowAdapter(){
			public void windowClosed(WindowEvent e){
				openDialogList.remove(dialog);
			}
		});
	}
	
	/**
	 * Compute the specified table coordiantes to scroll-pane viewport 
	 * relative coordinates. This is useful for displaying popup menus on a 
	 * JTable which is enclosed in a JScrollPane where the mouse click
	 * point has been received via a MouseEvent.
	 * 
	 * @param p Point in the JTable coordinates.
	 * @param sp Scrollpane containing the JTable.
	 * @return Point in JScrollPane viewport relative coordinates.
	 */
	private Point2D getScrollPaneRelativeCoordinates(Point2D p, JScrollPane sp){
		JViewport vp = sp.getViewport();
		return new Point2D.Double(
				p.getX() - vp.getViewPosition().x,
				p.getY() - vp.getViewPosition().y);
	}
	
	private void installDoubleClickHandlerOnFileTable(final FileTable fileTable){
		fileTable.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				// if a row in the FileTable is left-double-clicked, a new
				// dialog containing the row's
				// FeatureTable is displayed.
				if (SwingUtilities.isLeftMouseButton(e)	&&
					e.getClickCount() == 2 && 
					fileTable.columnAtPoint(e.getPoint()) > 0){
					
					List fcl = fileTable.getSelectedFeatureCollections();
					for(Iterator li=fcl.iterator(); li.hasNext(); ){
						FeatureCollection fc = (FeatureCollection)li.next();
						final FeatureTableAdapter fta = new FeatureTableAdapter(fc, shapeLayer.selections, shapeLayer.getHistory(), shapeLayer);
						final STable ft = fta.getTable();
						final JDialog columnDialog = ft.getColumnDialog();
						// add the column dialog to the list of open dialogs so it's disposed
						// when the layer is removed.
						registerDialogForAutoCleanup (columnDialog);
						String title = (fc.getProvider() == null)?
								FileTableModel.NULL_PROVIDER_NAME:
									fc.getFilename();
						final JDialog ftDialog = new JDialog((JFrame)null, title, false);
						JPanel ftPanel = new JPanel(new BorderLayout());
						ftPanel.setSize(new Dimension(400,400));
						JScrollPane ftScrollPane = new JScrollPane(ft);
						installContextMenuOnFeatureTable(ft, ftScrollPane);
						ftPanel.add(ftScrollPane, BorderLayout.CENTER);
						ftDialog.setContentPane(ftPanel);
						ftDialog.pack();
						ftDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
						ftDialog.addWindowListener(new WindowAdapter(){
							public void windowClosed(WindowEvent e) {
								// Ask FeatureTable to clear its listeners.
								fta.disconnect();
								// Remove the popup table now, so don't do it
								// when removing the layer
								columnDialog.dispose();
							}
						});
						registerDialogForAutoCleanup(ftDialog);
						ftDialog.setVisible(true);
					}
				}
			}
		});
	}
	
	private void installContextMenuOnFileTable(
			final FileTable fileTable,
			final JScrollPane fileTableScrollPane)
	{
		fileTable.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				// if the fileTable is right-clicked, bring up a right-click menu that 
				// allows for saving of filetable featuretables.
				if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 1){
				    // if we had NO selected rows, do nothing.
					int[] selectedRows = fileTable.getSelectedRows();
					if (selectedRows == null) 
						return;
					
					JPopupMenu popup = new JPopupMenu();
					popup.add(saveSelectedFilesMenuItem);
					popup.add(saveSelectedFilesToFileAsMenuItem);
					popup.add(deleteSelectedFilesMenuItem);
					
					Point2D p = getScrollPaneRelativeCoordinates(e.getPoint(), fileTableScrollPane);
					popup.show(fileTable, (int)p.getX(), (int)p.getY());			    
				}
			}
		});
	}

	private JPanel  buildFilePanel() {
	    JPanel filePanel = new JPanel();	   
	    filePanel.setLayout( new BorderLayout());	  
	    Border border = new TitledBorder("Files");
	    filePanel.setBorder(border);	   
	    fileTableScrollPane = new JScrollPane(shapeLayer.getFileTable()); 
	    filePanel.add(fileTableScrollPane, BorderLayout.CENTER);
	    return filePanel;
	}
	
	private JPanel  buildFeaturePanel(){
	    JPanel featurePanel = new JPanel();	   
	    featurePanel.setLayout( new BorderLayout());
	    Border border = new TitledBorder("Features");
	    featurePanel.setBorder(border);
	    featureTableScrollPane = new JScrollPane(featureTable);	    
	    featurePanel.add(featureTableScrollPane, BorderLayout.CENTER);	
	    return featurePanel;
	}
	
	/**
	 * Bind actions to menu items in ShapeFocusPanel.
	 */
	private void initMenuActions(){
		deleteSelectedFilesMenuItem.addActionListener(new DelSelectedFilesActionListener());
		saveSelectedFeaturesToFileMenuItem.addActionListener(new SaveAsAction(saveSelectedFeaturesAsActionName, shapeLayer.getFeatureCollection()));
		saveSelectedFilesToFileAsMenuItem.addActionListener(new SaveAsAction(saveSelectedFilesAsActionName, null));
		saveAllFeaturesToFileAsMenuItem.addActionListener(new SaveAsAction(saveAllFeaturesAsActionName, null));
		
		saveSelectedFilesMenuItem.addActionListener(new SaveSelectedFilesActionListener());
		
		featureCommandMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				ToolManager.setToolMode(ToolManager.SEL_HAND);

				// The dialog what lets users enter in SQL commands.
				JDialog commandDialog = new CommandDialog(
						shapeLayer.getFeatureCollection(), shapeLayer.getHistory(),
						shapeLayer, (Frame)ShapeFocusPanel.this.getTopLevelAncestor());
				commandDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
				registerDialogForAutoCleanup(commandDialog);
				commandDialog.setVisible(true);
			}
		});
		
		featureLoadScriptsMenuItem.addActionListener(new LoadScriptActionListener());
		addTooltipMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showFeatureColumnsDialog();
			}
		});
	}
	
	private void showFeatureColumnsDialog() {
		// get list of fields sorted by case-insensitive name
		Set<Field> sortedFields = new TreeSet<Field>(ShapeLayer.fieldByName);
		sortedFields.addAll(shapeLayer.fileTable.getSelectedFileFields());
		
		// add settings checkboxes
		final JPanel settings = new JPanel();
		settings.setLayout(new BoxLayout(settings, BoxLayout.PAGE_AXIS));
		int gap = 3;
		for (final Field f: sortedFields) {
			final JCheckBox checkbox = new JCheckBox(f.name, shapeLayer.tooltipFields.contains(f));
			settings.add(checkbox);
			checkbox.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (checkbox.isSelected()) {
						shapeLayer.tooltipFields.add(f);
					} else {
						shapeLayer.tooltipFields.remove(f);
					}
				}
			});
			settings.add(Box.createVerticalStrut(gap));
		}
		
		/** handles unselect all / select all actions */
		class Handler implements ActionListener {
			private final boolean mode;
			/** @param mode The state the checkboxes should all be in after actionPerformed is called */
			public Handler(boolean mode) {
				this.mode = mode;
			}

			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < settings.getComponentCount(); i++) {
					Component c = settings.getComponent(i);
					if (c instanceof JCheckBox) {
						JCheckBox cb = (JCheckBox)c;
						if (mode != cb.isSelected()) {
							cb.doClick(1);
						}
					}
				}
			}
		}
		
		final JButton selectAll = new JButton("Unselect All".toUpperCase());
		selectAll.addActionListener(new Handler(false));
		
		JButton deselectAll = new JButton("Select All".toUpperCase());
		deselectAll.addActionListener(new Handler(true));
		
		Frame top = (Frame)SwingUtilities.getAncestorOfClass(Frame.class, this);
		final JDialog prop = new JDialog(top,"Shape Tooltips", true);
		
		Box selUnsel = Box.createVerticalBox();
		selUnsel.add(selectAll);
		selUnsel.add(Box.createVerticalStrut(gap));
		selUnsel.add(deselectAll);
		
		prop.setLocation(MouseInfo.getPointerInfo().getLocation().x, MouseInfo.getPointerInfo().getLocation().y);
	
		JPanel panel = new JPanel(new BorderLayout(4,4));
		panel.setBorder(new EmptyBorder(5, 10, 10, 10));
		panel.setLayout(new BorderLayout(4,4));
		panel.add(selUnsel, BorderLayout.SOUTH);
		panel.add(new JScrollPane(settings), BorderLayout.CENTER);
		prop.setContentPane(panel);
		prop.pack();
		prop.setResizable(false);
		prop.setVisible(true);
	}
	
	/**
	 * Implements "Load Files" action.
	 * 
	 * @author saadat
	 */
	private class LoadActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			final File [] f = loadSaveFileChooser.chooseFile(ShapeFocusPanel.this, "Load");

		    // If no file was selected, quit.
		    if (f == null)
				return;
		    
	    	FeatureProvider fp = loadSaveFileChooser.getFeatureProvider();
	    	final List<ShapeLayer.LoadData> sources = new ArrayList<ShapeLayer.LoadData>();
	    	for (int i = 0; i < f.length; i++) {
	    		sources.add(new ShapeLayer.LoadData(fp, f[i].getAbsolutePath()));
	    	}
	    	Config.set(defaultProviderKey, fp.getClass().getName());
			shapeLayer.loadSources(sources);
		}
	}
	
	/**
	 * Implements loading for an array of FeatureProviders. If the instance of a
	 * provider needs a name, it needs to be set prior to sending it to this
	 * class.
	 */
	private class CustomProviderHandler implements ActionListener {
		FeatureProvider fp;

		public CustomProviderHandler(FeatureProvider fp) {
			this.fp = fp;
		}

		public void actionPerformed(ActionEvent e) {
			shapeLayer.loadSources(Arrays.asList(new ShapeLayer.LoadData(fp, null)));
		}
	}
	
	/**
	 * Implements various "Save As" actions. This code is also invoked as a
	 * subordinate of the "Save" action. Such a situation occurs when the 
	 * "Save" action cannot save all Features.
	 * 
	 * @author saadat
	 */
	private class SaveAsAction extends AbstractAction {
		final FeatureCollection _fc;
		
		public SaveAsAction(String name, FeatureCollection _fc){
			super(name);
			this._fc = _fc;
		}
		
		public SaveAsAction(FeatureCollection fc){
			this("Save As", fc);
		}
		
		public void actionPerformed(ActionEvent e){
		    File [] f = loadSaveFileChooser.chooseFile(ShapeFocusPanel.this, e.getActionCommand());

		    if (f == null || f.length == 0)
		    	return;

		    if (f.length > 1){
		    	Util.showMessageDialog("Cannot save to multiple files.",
		    			"Select one file only.",
		    			JOptionPane.ERROR_MESSAGE);
		    	return;
		    }

		    String fileName = f[0].getAbsolutePath();

			// The runningAsSubordinate flag is set to true if this SaveAs action 
			// resulted from a currently progressing Save action.
			boolean runningAsSubordinate = false;
			FeatureCollection fc = new SingleFeatureCollection();
			// fc.setFilename(_fc.getFilename());
			if (saveSelectedFeaturesAsActionName.equals(e.getActionCommand())){
				fc.addFeatures(shapeLayer.selections);
			}
			else if (saveSelectedFilesAsActionName.equals(e.getActionCommand())){
				List fcl = shapeLayer.getFileTable().getSelectedFeatureCollections();
				for(Iterator i=fcl.iterator(); i.hasNext(); )
					fc.addFeatures(((FeatureCollection)i.next()).getFeatures());
			}
			else if (saveAllFeaturesAsActionName.equals(e.getActionCommand())){
				List fcl = shapeLayer.getFileTable().getFileTableModel().getAll();
				for(Iterator i=fcl.iterator(); i.hasNext(); )
					fc.addFeatures(((FeatureCollection)i.next()).getFeatures());
			}
			else if (e.getSource() instanceof SaveSelectedFilesActionListener){
				fc = _fc;
				runningAsSubordinate = true;
			}
			else {
				log.aprintln("UNKNOWN actionCommand!");
				return;
			}

			FeatureProvider fp = (FeatureProvider)loadSaveFileChooser.getFeatureProvider();

			if (!fp.isRepresentable(fc)){
				int option = Util.showConfirmDialog(
						"Type does not support saving all the Features. Continue?",
						"Continue?",
						JOptionPane.YES_NO_OPTION);
				if (option != JOptionPane.OK_OPTION)
					return;
			}

			File[] files = fp.getExistingSaveToFiles(fc, fileName);
		    if (files.length > 0) {
				int option = Util.showConfirmDialog(
				   Util.join("\n", files),
				   "File" + (files.length > 1 ? "s" : "") + " exists. Overwrite?", 
				   JOptionPane.OK_CANCEL_OPTION);
				if (option != JOptionPane.OK_OPTION)
					return;
		    }

			// Mark a new history frame
		    if (!runningAsSubordinate)
		    	shapeLayer.getHistory().mark();

		    ShapeLayer.LEDState led = null;
		    try {
		    	if (!runningAsSubordinate)
		    		shapeLayer.begin(led = new ShapeLayer.LEDStateFileIO());
		    	fp.save(fc,fileName);
		    	//@since change bodies
		    	//this change is to deselect the untitled table record when the user
		    	//does a save as and saves the current work as a named file. The problem
		    	//is that you can save the current work, that was done on the untitled table row
		    	//and never be able to remove the layer or change bodies without getting the unsaved data warning
		    	FileTable ft = shapeLayer.getFileTable();
		    	if (ft.getSelectedRow() == 0) {
		    		List<FeatureCollection> selectedFeatureCollections = ft.getSelectedFeatureCollections();
		    		ft.getFileTableModel().setTouched(selectedFeatureCollections.get(0), false);
		    	}
		    	//end change to deselect untitled table record
		    }
		    catch(RuntimeException ex){
		    	log.aprintln("While saving "+fileName+" got: "+ex.getMessage());
		    	ex.printStackTrace();
		    	if (!runningAsSubordinate) {
		    		Throwable t;
		    		for (t = ex; t.getCause() != null; t = t.getCause());
		    		Util.showMessageDialog(t.getMessage(),
		    			"Unable to save " + fileName,
		    			JOptionPane.ERROR_MESSAGE);
		    	} else {
		    		throw ex;
		    	}
		    }
		    finally {
		    	if (!runningAsSubordinate)
		    		shapeLayer.end(led);
		    }
		}
	}
	
	/**
	 * Implements the "Save Selected Files" action.
	 * 
	 * @author saadat
	 */
	private class SaveSelectedFilesActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
			FileTable ft = shapeLayer.getFileTable();
			List<String> unsavable = new ArrayList<String>();
			List<FeatureCollection> saveable = new ArrayList<FeatureCollection>();
			List<?> fcl = ft.getSelectedFeatureCollections();
			for(Iterator<?> i=fcl.iterator(); i.hasNext(); ){
				FeatureCollection fc = (FeatureCollection)i.next();
				if (fc.getProvider() == null)
					unsavable.add(FileTableModel.NULL_PROVIDER_NAME);
				else if (fc.getProvider() instanceof FeatureProviderNomenclature)
					unsavable.add(fc.getProvider().getDescription());
				else
					saveable.add(fc);
			}

			if (!saveable.isEmpty()){
				// Don't mark a history frame as that frame only contains the touched flag
				//shapeLayer.getHistory().mark();

				ShapeLayer.LEDState led;
				shapeLayer.begin(led = new ShapeLayer.LEDStateFileIO());
				try {
					for(FeatureCollection currentFc: saveable) {
						boolean produceSaveAs = false;
						FeatureProvider fp = currentFc.getProvider();

						if (!fp.isRepresentable(currentFc)){
							int option = Util.showOptionDialog(
								"The save operation on "+currentFc.getFilename() +
									" will not save all Features.",
								"Warning!",
								JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE,
								null,
								new String[]{ "Continue", "Save As", "Cancel"},
								"Continue");

							switch(option){
								case 0:	produceSaveAs = false; break;
								case 1:	produceSaveAs = true; break;
								default: continue;
							}
						}
						
						try {
							if (produceSaveAs){
								(new SaveAsAction(currentFc)).actionPerformed(
										new ActionEvent(SaveSelectedFilesActionListener.this,
												ActionEvent.ACTION_PERFORMED, "Save As"));
							}
							else {
								// Save the FeatureCollection
								currentFc.getProvider().save(currentFc, currentFc.getFilename());
								// If saved properly, reset the dirty marker
								ft.getFileTableModel().setTouched(currentFc,false);
							}
						}
						catch(Exception ex) {
							ex.printStackTrace();
							unsavable.add(getFcName(currentFc));
						}
					}
					if (!unsavable.isEmpty()) {
						Util.showMessageDialogObj(unsavable.toArray(),
								"Unable to save the following:",
								JOptionPane.WARNING_MESSAGE);
					}
				} finally {
					shapeLayer.end(led);
				}
			}
		}
	}
	
	private String getFcName(FeatureCollection fc){
		String name = fc.getFilename();
		return name == null ? FileTableModel.NULL_PROVIDER_NAME : name;
	}

	/**
	 * Implements the "Delete Selected Files" action, which lets the user remove
	 * any file except the untitled one.
	 * 
	 * @author saadat
	 */
	private class DelSelectedFilesActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
			// Mark a new history frame
			shapeLayer.getHistory().mark();
			ShapeLayer.LEDState led = null;
			try {
    			shapeLayer.begin(led = new ShapeLayer.LEDStateProcessing());
    			List<FeatureCollection> sel = shapeLayer.getFileTable().getSelectedFeatureCollections();
    			Iterator<FeatureCollection> it = sel.iterator();
    			while (it.hasNext()) {
    				FeatureCollection fc = it.next();
    				if (fc.getProvider() == null) {
    					Util.showMessageDialogObj(new String[] {
    						"Cannot remove the "+FileTableModel.NULL_PROVIDER_NAME+" file."
    					},
    					"Warning!",
    					JOptionPane.WARNING_MESSAGE
    					);
    					it.remove();
    				}
    			}
				shapeLayer.getFileTable().getFileTableModel().removeAll(sel);
			}
			finally {
    			shapeLayer.end(led);
			}
		}
	}
	
	/**
	 * Implements the "Delete Selected Features" action.
	 * 
	 * @author saadat
	 */
	private class DelSelectedFeaturesAction extends AbstractAction {
		final FeatureCollection fc;
		
		public DelSelectedFeaturesAction(FeatureCollection fc){
			super("Delete Selected Features");
			this.fc = fc;
		}
		
		public void actionPerformed(ActionEvent e){
			// Mark a new history frame
			shapeLayer.getHistory().mark();
			fc.removeFeatures(shapeLayer.selections);
		}
	}
	
	/**
	 * Implements the "Center on Feature" action.
	 * 
	 * @author saadat
	 */
	private class CenterOnFeatureAction extends AbstractAction {
		public CenterOnFeatureAction() {
			super("Center on Feature");
		}
		
		public void actionPerformed(ActionEvent e){
			if (!shapeLayer.selections.isEmpty()) {
				Point2D.Double center = new Point2D.Double();
				MemoryFeatureIndex idx = shapeLayer.getIndex();
				for (Feature f: shapeLayer.selections) {
					Point2D c = idx.getWorldPath(f).getCenter();
					center.x += c.getX();
					center.y += c.getY();
				}
				int size = shapeLayer.selections.size();
				if (size > 1) {
					center.x /= size;
					center.y /= size;
				}
				if (size > 0) {
					shapeLView.viewman.getLocationManager().setLocation(center, true);
				}
			}
		}
	}
	
	/**
	 * Implements "Load Script" action.
	 * 
	 * @author saadat
	 */
	private class LoadScriptActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
			final File[] scriptFile = shapeScriptFileChooser.chooseFile(ShapeFocusPanel.this, "Load");
			if (scriptFile == null)
				return;
			
	    	// Make a new history frame.
	    	shapeLayer.getHistory().mark();
	    	
		    // If we are going to do anything here, we need to be in edit mode.
	    	ToolManager.setToolMode(ToolManager.SEL_HAND);
 
		    final ShapeLayer.LEDState ledState = new ShapeLayer.LEDStateFileIO();
		    SwingUtilities.invokeLater(new Runnable(){
		    	public void run(){
				    shapeLayer.begin(ledState);
				    try {
				    	BufferedReader  inStream = new BufferedReader( new FileReader( scriptFile[0].toString() ));
				    	String line;
				    	do {
				    		line = inStream.readLine();
				    		if (line!=null){
				    			new FeatureSQL(
				    				line,
				    				shapeLayer.getFeatureCollection(),
				    				shapeLayer.getIndex(),
				    				shapeLayer.selections);
				    		}
				    	} while (line != null);
				    	inStream.close();
				    } catch (IOException ex) {
				    	Util.showMessageDialogObj(new String[] {
				    				"Error reading file " + scriptFile[0].toString() + ": ",
				    				ex.getMessage(),
				    			},
				    			"Error!", JOptionPane.ERROR_MESSAGE);
				    }
				    finally {
				    	shapeLayer.end(ledState);
				    }
		    	}
		    });
		}
	}
	
	private class MultiEditAction extends AbstractAction {
		MouseEvent tableMouseEvent = null;
		STable dataTable;
		boolean booleanResult = true;
		
		public MultiEditAction(STable dataTable){
			super("Edit column of selected rows");
			this.dataTable = dataTable;
		}
		
		public void setTableMouseEvent(MouseEvent e){
			tableMouseEvent = e;
		}

		public void actionPerformed(ActionEvent e) {
			if (tableMouseEvent == null)
				return;

			int screenColumn = dataTable.getColumnModel().getColumnIndexAtX(tableMouseEvent.getX());
			String columnName = dataTable.getColumnName(screenColumn);
			TableColumn tableColumn = dataTable.getColumnModel().getColumn(screenColumn);
			int columnIndex = tableColumn.getModelIndex();
			Field field = (Field)tableColumn.getIdentifier();
			if (!field.editable) {
				Util.showMessageDialog("\""+columnName+"\" is not an editable column", "Error!",
						JOptionPane.ERROR_MESSAGE);
			}
			else {
				Class<?> columnClass = dataTable.getModel().getColumnClass(columnIndex);
				TableCellEditor editor = dataTable.getDefaultEditor(columnClass);
				int[] selectedRows = dataTable.getSelectedRows();
				if (selectedRows == null || selectedRows.length == 0) {
					return;
				}
				
				JPanel inputPanel = new JPanel(new BorderLayout());
				inputPanel.add(editor.getTableCellEditorComponent(dataTable, null,
						false, selectedRows[0], columnIndex), BorderLayout.CENTER);
				
				boolean okay = false;
				if (editor instanceof ColorCellEditor){
					ColorCellEditor ce = (ColorCellEditor)editor;
					//check if all selected rows have the same color, if they do
					// set that color on the color cell editor
					boolean allMatch = true;
					Color selColor = null;
					for(int row : selectedRows){
						Color c = (Color) dataTable.getModel().getValueAt(row, columnIndex);
						if(selColor != null){
							if(!selColor.equals(c)){
								allMatch = false;
								break;
							}
						}
						selColor = c;
					}
					//If all the colors match, set the color on the editor
					// then display to the user.
					if(selColor != null && allMatch){
						ce.setColor(selColor);
					}
					ce.showEditor(ShapeFocusPanel.this, true);
					if (ce.isInputAccepted()) {
						okay = true;
					}
				} else {
					SimpleDialog dialog = SimpleDialog.getInstance(
						(Frame)SwingUtilities.getAncestorOfClass(Frame.class, ShapeFocusPanel.this),
						"Enter value for \""+columnName+"\"", true, inputPanel);
					dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
					dialog.setVisible(true);
					if (SimpleDialog.OK_COMMAND.equals(dialog.getActionCommand())) {
						okay = true;
					}
				}
				
				if (okay) {
					Object input = editor.getCellEditorValue();
					Map<Feature,Object> values = new LinkedHashMap<Feature,Object>();
					List<Feature> features = shapeLayer.getFeatureCollection().getFeatures();
					for (int sortedIdx: selectedRows) {
						int unsortedIdx = dataTable.getSorter().unsortRow(sortedIdx);
						values.put(features.get(unsortedIdx), input);
					}
					shapeLayer.getFeatureCollection().setAttributes(field, values);
				}
			}
		}
	}

	private class ResetToDefaultAction extends AbstractAction {
		MouseEvent tableMouseEvent = null;
		STable dataTable;

		public ResetToDefaultAction(String name, STable dataTable) {
			super(name);
			this.dataTable = dataTable;
		}

		public void setTableMouseEvent(MouseEvent e) {
			tableMouseEvent = e;
		}

		public void actionPerformed(ActionEvent e) {
			Object defaultValue = null;

			if (tableMouseEvent == null)
				return;

			int screenColumn = dataTable.getColumnModel().getColumnIndexAtX(tableMouseEvent.getX());
			String columnName = dataTable.getColumnName(screenColumn);
			TableColumn tableColumn = dataTable.getColumnModel().getColumn(screenColumn);
			int columnIndex = tableColumn.getModelIndex();

			Field field = (Field) tableColumn.getIdentifier();
			if (!field.editable) {
				Util.showMessageDialog("\"" + columnName + "\" is not an editable column", "Error!",
						JOptionPane.ERROR_MESSAGE);
			} else {
				int[] selectedRows = dataTable.getSelectedRows();
				if (selectedRows == null || selectedRows.length == 0) {
					return;
				}

				for (int rowIndex : selectedRows) {
					dataTable.getModel().setValueAt(defaultValue, rowIndex, columnIndex);
				}
			}
		}
	}	

	//
	private class CopyUrl extends AbstractAction {
		MouseEvent tableMouseEvent = null;
		STable dataTable;

		public CopyUrl(String name, STable dataTable) {
			super(name);
			this.dataTable = dataTable;
		}

		public void setTableMouseEvent(MouseEvent e) {
			tableMouseEvent = e;
		}

		public void actionPerformed(ActionEvent e) {
			int screenColumn = dataTable.getColumnModel().getColumnIndexAtX(tableMouseEvent.getX());
			TableColumn tableColumn = dataTable.getColumnModel().getColumn(screenColumn);
			int columnIndex = tableColumn.getModelIndex();

			int[] selectedRows = dataTable.getSelectedRows();
			if (selectedRows == null || selectedRows.length == 0) {
				return;
			}

			StringBuilder sb = new StringBuilder();
			for (int rowIndex : selectedRows) {
				Object str = dataTable.getModel().getValueAt(rowIndex, columnIndex);
				if (str != null && (str instanceof String)) {
					sb.append((String) str);
					sb.append("\n");
				}
			}
			StringSelection selection = new StringSelection(sb.toString());
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(selection, null);
		}
	}

	private static class SimpleDialog extends JDialog implements ActionListener {
		public static final String OK_COMMAND = "OK";
		public static final String CANCEL_COMMAND = "Cancel";
		
		JButton okButton = new JButton(OK_COMMAND.toUpperCase());
		JButton cancelButton = new JButton(CANCEL_COMMAND.toUpperCase());
		JPanel inputPanel;
		JPanel buttonPanel;
		String actionCommand = null;
		
		protected SimpleDialog(Frame owner, String title, boolean modal){
			super(owner, title, modal);
			
			inputPanel = new JPanel(new BorderLayout());
			inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);
			inputPanel.add(buttonPanel, BorderLayout.SOUTH);
			okButton.addActionListener(this);
			okButton.setDefaultCapable(true);
			cancelButton.addActionListener(this);
			setContentPane(inputPanel);
		}
		
		public static SimpleDialog getInstance(Frame owner, String title, boolean modal, JComponent inputComponent){
			SimpleDialog dialog = new SimpleDialog(owner, title, modal);
			dialog.inputPanel.add(inputComponent, BorderLayout.CENTER);
			dialog.pack();
			dialog.setLocationRelativeTo(owner);
			return dialog;
		}
		
		public void actionPerformed(ActionEvent e){
			actionCommand = e.getActionCommand();
			setVisible(false);
		}
		
		public String getActionCommand(){
			return actionCommand;
		}
	}
	
	private JMenuBar getMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		int menuspacing = ((ThemeMenuBar) GUITheme.get("menubar")).getItemSpacing();
		menuBar.add(Box.createHorizontalStrut(menuspacing));
		menuBar.add(getFileMenu());
		menuBar.add(Box.createHorizontalStrut(menuspacing));
		menuBar.add(getSelectMenu());
		menuBar.add(Box.createHorizontalStrut(menuspacing));
		menuBar.add(getScriptMenu());
		menuBar.add(Box.createHorizontalStrut(menuspacing));
		menuBar.add(getPropMenu());	
		int pad = ((ThemeMenuBar) GUITheme.get("menubar")).getItemPadding();
		menuBar.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, ((ThemeTable)GUITheme.get("table")).getGridcolor()),
		        BorderFactory.createEmptyBorder(pad, 5, pad, 5)));
		return menuBar;
	}
	
	/** Create a menu with children */
	private JMenuItem createMenu(String title, Component[] children) {
		JMenuItem item = new JMenu(title);
		for (int i = 0; i < children.length; i++)
			item.add(children[i]);
		return item;
	}

	/** Create a menu item with an optional action listener */
	private JMenuItem createMenu(String title, JMenuItem instance, ActionListener handler) {
		JMenuItem item = (instance != null ? instance : new JMenuItem());
		item.setText(title);
		if (handler != null)
			item.addActionListener(handler);
		return item;
	}

	private JMenu getPropMenu() {
		JMenuItem name = new JMenuItem("Name...");
		name.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String oldName = shapeLView.getName();
				String newName = Util.showInputDialog("Shape Layer Name", oldName);
				if (newName != null && !oldName.equals(newName)) {
					shapeLView.setName(newName);
				}
			}
		});
		
		final JMenuItem showProgress = new JCheckBoxMenuItem("Show Progress", shapeLayer.showProgress);
		showProgress.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				shapeLayer.showProgress = showProgress.isSelected();
			}
		});
		
		JMenu props = new JMenu("Settings");		
		props.add(name);
		props.add(showProgress);
		props.add(addTooltipMenuItem);
		return props;
	}
	
	private JMenu getScriptMenu() {
		JMenu scriptsMenu = new JMenu("Scripts");		
		scriptsMenu.add(featureLoadScriptsMenuItem);
		scriptsMenu.add(featureCommandMenuItem);
		return scriptsMenu;
	}

	private JMenu getSelectMenu() {
		JMenu selectMenu = new JMenu("Feature");
		
		// build the FeatureTable menu
		JMenuItem featureUndoMenuItem = new JMenuItem("Undo");
		selectMenu.add(featureUndoMenuItem);
		featureUndoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		featureUndoMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if (shapeLayer.getHistory().canUndo()) {
					ShapeLayer.LEDState led = null;
					try {
						shapeLayer.begin(led = new ShapeLayer.LEDStateProcessing());
						shapeLayer.getHistory().undo();
					}
					finally {
						shapeLayer.end(led);
					}
				}
			}
		});
//		JMenuItem featureRedoMenuItem = new JMenuItem("Redo");
//		selectMenu.add(featureRedoMenuItem);
//		featureRedoMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,KeyEvent.CTRL_DOWN_MASK));
//		featureRedoMenuItem.addActionListener(new ActionListener(){
//			public void actionPerformed(ActionEvent e){
//				if (shapeLayer.getHistory().canRedo()) {
//					ShapeLayer.LEDState led = null;
//					try {
//						shapeLayer.begin(led = new ShapeLayer.LEDStateProcessing());
//						shapeLayer.getHistory().redo();
//					}
//					finally {
//						shapeLayer.end(led);
//					}
//				}
//			}
//		});
		
		selectMenu.add(saveAllFeaturesToFileAsMenuItem);
		final JMenuItem editColumnMenu = createMenu("Edit Columns...", null, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// take a snapshot of the columns before hand, and then show a column editor to the user
				FeatureCollection fc = shapeLayer.getFeatureCollection();
				Set<Field> oldFields = new HashSet<Field>(fc.getSchema());
				int[] rows = shapeLayer.fileTable.getSelectedRows();
				if (rows == null || rows.length != 1) {
					return;
				}
				
				Frame parent = (Frame)SwingUtilities.getAncestorOfClass(Frame.class, ShapeFocusPanel.this);
				new ColumnEditor(
					shapeLayer,
					shapeLayer.fileTable.getFileTableModel().get(rows[0]),
					parent).showColumnEditor();
				
				// get all fields affected by the editor
				Set<Field> unchangedFields = new HashSet<Field>(fc.getSchema());
				unchangedFields.retainAll(oldFields);
				Set<Field> changedFields = new HashSet<Field>(oldFields);
				changedFields.addAll(fc.getSchema());
				changedFields.removeAll(unchangedFields);
				
				// notify the views of the styles affected by the column edit
				Set<Style<?>> changed = shapeLayer.getStylesFromFields(changedFields);
				shapeLayer.broadcast(new ShapeLayer.StylesChange(changed));
			}
		});
		editColumnMenu.setEnabled(shapeLayer.fileTable.getSelectedRowCount() == 1);
		selectMenu.add(editColumnMenu);
		
		// make sure we can only edit columns for one file at a time
		final FileTable ft = shapeLayer.getFileTable();
		ft.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				editColumnMenu.setEnabled(ft.getSelectedRowCount() == 1);
				editColumnMenu.setToolTipText(editColumnMenu.isEnabled() ? null : "Select one file to edit columns");
			}
		});
		
		selectMenu.add(new JSeparator());
		
		new StylesMenu(selectMenu);
		
		return selectMenu;
	}
	
	/** simple abstract way of getting the styles */
	interface StylesFactory {
		ShapeLayerStyles getStyles();
	}
	
	private JRadioButton emptySelection = new JRadioButton("hidden selection");
	public void clearStyleSelection() {
		emptySelection.setSelected(true);
	}
	
	private class StylesMenu {
		private static final String STYLES_DEFAULT_KEY = "shape.styles_default";
		private JMenu stylesMenu = new JMenu("Styles");
		private ButtonGroup group = new ButtonGroup();
		private final Map<String,JMenuItem> choices = new LinkedHashMap<String,JMenuItem>();
		private final Map<String,StylesFactory> factories = new LinkedHashMap<String,StylesFactory>();
		
		public StylesMenu(JMenu stylesMenu) {
			this.stylesMenu = stylesMenu;
			
			group.add(emptySelection);
			
			stylesMenu.add(createMenu("Edit Circles...", null, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Style<FPath> geomStyle = shapeLayer.getStyles().geometry;
					
					// get the GeomSource, or create a new one if necessary
					StyleSource<?> source = geomStyle.getSource();
					GeomSource geomSource;
					if (source instanceof GeomSource) {
						geomSource = (GeomSource)source;
					} else {
						geomSource = null;
					}
					StyleSource<FPath> newSource = GeomSource.editCircleSource(
						(Frame)SwingUtilities.getAncestorOfClass(Frame.class, ShapeFocusPanel.this),
						shapeLayer.getFeatureCollection().getSchema(),
						geomSource);
					if (newSource != geomSource && newSource != null) {
						geomStyle.setSource(newSource);
						Set<Style<?>> changes = new HashSet<Style<?>>();
						changes.add(geomStyle);
						shapeLayer.applyStyleChanges(changes);
					}
				}
			}));
			

			stylesMenu.add(createMenu("Edit Ellipses...", null, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Style<FPath> geomStyle = shapeLayer.getStyles().geometry;
					
					// get the GeomSource, or create a new one if necessary
					StyleSource<?> source = geomStyle.getSource();
					GeomSource geomSource = null;
					if (source instanceof GeomSource) {
						geomSource = (GeomSource)source;
					} 
					
					StyleSource<FPath> newSource = GeomSource.editEllipseSource((Frame)SwingUtilities.getAncestorOfClass(Frame.class, ShapeFocusPanel.this),
						shapeLayer,geomSource, shapeLView.getProj());
					if (newSource != geomSource && newSource != null) {
						geomStyle.setSource(newSource);
						Set<Style<?>> changes = new HashSet<Style<?>>();
						changes.add(geomStyle);
						shapeLayer.applyStyleChanges(changes);
					}
					
				}
			}));
			
			stylesMenu.add(createMenu("Ellipse Options...", null, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean circleDefault = Config.get("shape.ellipse_default_circle", false);
					String[] arr = new String[] {"Circle", "Ellipse"};
					String defaultShape = circleDefault ? "Circle" : "Ellipse";
					String response = (String)Util.showInputDialog("Shape to draw by default: ", "Ellipse Options", JOptionPane.QUESTION_MESSAGE, null, arr, defaultShape);
					
					if ("Circle".equalsIgnoreCase(response)) {
						circleDefault = true;
					} else {
						circleDefault = false;
					}
					Config.set("shape.ellipse_default_circle", circleDefault);
					
				}
			}));
			
			stylesMenu.add(createMenu("Edit Styles...", null, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// popup dialog, which returns the changed styles
					ShapeLayerStyles styles = shapeLayer.getStyles();
					List<Field> fields = shapeLayer.getFeatureCollection().getSchema();
					Set<Style<?>> changed = new StyleEditor().showStyleEditor(styles, fields);
					if (!changed.isEmpty()) {
						shapeLayer.applyStyleChanges(changed);
						
						// empty the styles selection since the current styles is not known to be equal to any of them
						emptySelection.setSelected(true);
					}
				}
			}));
			
			stylesMenu.add(createMenu("Save Styles...", null, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					while (true) {
						final String input = Util.showInputDialog("Name this styles configuration:",null);
						if (input == null) {
							break;
						}
						try {
							if (choices.keySet().contains(input)) {
								throw new IllegalArgumentException("That name already exists, must use another");
							}
							stylesStore.save(input, shapeLayer.getStyles());
							addFromStore(input);
							select(input);
							break;
						} catch (Exception ex) {
							Util.showMessageDialog("Error saving styles: " + ex.getMessage(),
								"Error Saving Styles", JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			}));
			
			stylesMenu.add(createMenu("Remove Styles...", null, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					final DefaultListModel model = new DefaultListModel();
					List<String> userNames = stylesStore.getNames();
					for (String name: userNames) {
						model.addElement(name);
					}
					
					final JButton del = new JButton("Remove".toUpperCase());
					del.setEnabled(false);
					
					final JButton ok = new JButton("OK".toUpperCase());
					
					final JButton cancel = new JButton("Cancel".toUpperCase());
					
					final JList list = new JList(model);
					
					int gap = 4;
					
					JPanel pad = new JPanel(new BorderLayout());
					pad.add(new JScrollPane(list));
					pad.setBorder(new EmptyBorder(gap,gap,gap,gap));
					
					Box buttons = Box.createVerticalBox();
					buttons.add(Box.createVerticalStrut(gap));
					buttons.add(del);
					buttons.add(Box.createVerticalStrut(gap));
					buttons.add(Box.createVerticalGlue());
					buttons.add(ok);
					buttons.add(Box.createVerticalStrut(gap));
					buttons.add(cancel);
					buttons.add(Box.createVerticalStrut(gap));
					
					final JDialog dlg = new JDialog((Frame)SwingUtilities.getAncestorOfClass(Frame.class, ShapeFocusPanel.this),
						"Remove Styles", true);
					dlg.getContentPane().setLayout(new BorderLayout());
					dlg.getContentPane().add(pad, BorderLayout.CENTER);
					dlg.getContentPane().add(buttons, BorderLayout.EAST);
					dlg.pack();
					
					final boolean[] okHit = {false};
					
					ok.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							okHit[0] = true;
							dlg.setVisible(false);
						}
					});
					
					cancel.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							dlg.setVisible(false);
						}
					});
					
					del.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							int[] indices = list.getSelectedIndices();
							for (int i = indices.length - 1; i>=0; i--) {
								model.remove(indices[i]);
							}
						}
					});
					
					list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
						public void valueChanged(ListSelectionEvent e) {
							del.setEnabled(list.getSelectedIndices().length > 0);
						}
					});
					
					Util.addEscapeAction(dlg);
					
					dlg.setSize(400,400);
					dlg.setVisible(true);
					
					if (okHit[0]) {
						List<String> names = Collections.list((Enumeration<String>)model.elements());
						List<String> errors = new ArrayList<String>();
						for (String name: userNames) {
							if (!names.contains(name)) {
								if (!removeFromStore(name)) {
									errors.add(name);
								}
							}
						}
						if (!errors.isEmpty()) {
							Util.showMessageDialog("Unable to remove the following styles:\n\n" + Util.join("\n", errors),
								"Error removing styles", JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			}));
			
			stylesMenu.add(new JSeparator());
			
			// add built-in styles
			String[] defaultStyles = Config.getAll("shape.styles");
			final XStream xs = new XStream();
			for (int i = 0; i < defaultStyles.length; i += 2) {
				final String name = defaultStyles[i+0];
				final String location = defaultStyles[i+1];
				addChoice(name, new StylesFactory() {
					public ShapeLayerStyles getStyles() {
						// try to get it from a URL
						try {
							return (ShapeLayerStyles)xs.fromXML(new URI(location).toURL().openStream());
						} catch (Exception e) {
							log.println(e);
						}
						// try to get it from a jar resource
						try {
							return (ShapeLayerStyles)xs.fromXML(Main.getResourceAsStream(location));
						} catch (Exception e) {
							log.println(e);
						}
						// indicate failure
						return null;
					}
				});
			}
			
			// add user-defined styles
			for (final String name: stylesStore.getNames()) {
				addFromStore(name);
			}
			
			// set the default
			select(Config.get(STYLES_DEFAULT_KEY));
		}
		
		public JMenu getMenu() {
			return stylesMenu;
		}
		
		private void addChoice(final String name, final StylesFactory factory) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(name);
			stylesMenu.add(item);
			group.add(item);
			choices.put(name, item);
			factories.put(name, factory);
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					select(name);
				}
			});
		}
		
		private boolean removeFromStore(final String name) {
			boolean result = stylesStore.remove(name);
			if (result) {
				JMenuItem item = choices.remove(name);
				factories.remove(name);
				if (item != null) {
					group.remove(item);
					stylesMenu.remove(item);
				}
			}
			return result;
		}
		
		private void addFromStore(final String name) {
			addChoice(name, new StylesFactory() {
				public ShapeLayerStyles getStyles() {
					return (ShapeLayerStyles) stylesStore.load(name);
				}
			});
		}
		
		private void select(String choice) {
			for (AbstractButton b: Collections.list(group.getElements())) {
				boolean val = b.getText().equals(choice);
				if (val) {
					try {
						ShapeLayerStyles styles = factories.get(choice).getStyles();
						if (styles != null) {
                            StyleSource<FPath> geometry = shapeLayer.getStyles().geometry.getSource();
							// replace the shape layer's loaded styles
							shapeLayer.applyStyleChanges(styles.getStyles(), geometry);
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						Util.showMessageDialog(Util.foldText(ex.getMessage(), 60, "\n"),
							"Error loading styles", JOptionPane.ERROR_MESSAGE);
					}
					if (!b.getText().equals(Config.get(STYLES_DEFAULT_KEY))) {
						Config.set(STYLES_DEFAULT_KEY, b.getText());
					}
				}
				group.setSelected(b.getModel(), val);
			}
		}
	}
	
	/** persists styles to and from disk */
	static class StylesStore {
		private XStream xs = new XStream();
		private static File base = new File(Main.getJMarsPath() + "styles");
		private static String ext = ".xml";
		/** Returns the names of all saved styles */
		public List<String> getNames() {
			List<String> names = new ArrayList<String>();
			if (base.exists() && base.isDirectory()) {
				for (File f: base.listFiles()) {
					if (f.getName().endsWith(ext)) {
						names.add(f.getName().replaceAll(ext + "$", ""));
					}
				}
			}
			return names;
		}
		private File getFile(String name) {
			return new File(base.getAbsolutePath() + File.separator + name + ext);
		}
		/** Removes the styles with the given name, returning true if the removal succeeded */
		public boolean remove(String name) {
			File f = getFile(name);
			return f.exists() ? f.delete() : true;
		}
		/** Returns the styles with this name, or throws an exception if an error occurred */
		public Styles load(String name) {
			try {
				return (Styles)xs.fromXML(new FileReader(getFile(name)));
			} catch (Exception e) {
				throw new IllegalArgumentException("Error while loading", e);
			}
		}
		/** Saves the styles with the given name, throwing IllegalArgumentException if the save failed */
		public void save(String name, Styles styles) {
			File file = getFile(name);
			if (file.exists()) {
				throw new IllegalArgumentException("File already exists");
			} else if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
				throw new IllegalStateException("Unable to create folder to save styles (" + file.getParentFile().getAbsolutePath() + ")");
			}
			try {
				xs.toXML(styles, new FileWriter(file));
			} catch (Exception e) {
				throw new IllegalArgumentException("XML conversion error: " + e.getMessage(), e);
			}
		}
	}
	
	private JMenu getFileMenu() {
		JMenu fileMenu = new JMenu("File");
		
	    // build the FileTable menu.		
		// Add the load file menu items (because this signifies it's a custom layer)
		if(!shapeLayer.isReadOnly){
			if (shapeLayer.getProviderFactory().getFileProviders().size() > 0) {
				fileMenu.add(createMenu("Load File...", null, new LoadActionListener()));
				fileMenu.add(new JSeparator());
			}
			 //add 'Save Features As' to File menu, so that users can easily find it
			fileMenu.add(createMenu(saveAllFeaturesAsActionName, null, new SaveAsAction(saveAllFeaturesAsActionName, null)));
			fileMenu.add(new JSeparator());
			if(!Main.getBody().equalsIgnoreCase("earth")){
				Iterator fIt = shapeLayer.getProviderFactory().getNotFileProviders().iterator();
				while (fIt.hasNext()) {
					FeatureProvider provider = (FeatureProvider)fIt.next();
					ActionListener handler = new CustomProviderHandler(provider);
					if(provider instanceof FeatureProviderNomenclature){
						fileMenu.add(createMenu("Load " + provider.getDescription(), null, handler));
					}else if(Main.getBody().equalsIgnoreCase("mars")){
						fileMenu.add(createMenu("Load " + provider.getDescription(), null, handler));
					}
				}
			}
		}
		return fileMenu;
	}
	
	public STable getFeatureTable(){
		return featureTable;
	}
	
	/**
	 * Disposes various resources currently in use by the FocusPanel rendering
	 * the FocusPanel unusable.
	 */
	protected void dispose(){
		if (featureTable.getColumnDialog() != null) {
			featureTable.getColumnDialog().dispose();
		}
		for (JDialog d: openDialogList) {
			try {
				d.dispose();
			} catch(RuntimeException ex){
				log.print(ex);
			}
		}
		openDialogList.clear();
	}
	
	private class MyUrlMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			int row = featureTable.rowAtPoint(e.getPoint());
			int col = featureTable.columnAtPoint(e.getPoint());
			if (featureTable.isUrlCellRenderer(col) && SwingUtilities.isLeftMouseButton(e) 
					&& e.getClickCount() == 1) {
				Object value = featureTable.getValueAt(row, col);
				if (value != null && featureTable.isValidUrl(value.toString())) {
					edu.asu.jmars.util.Util.launchBrowser(value.toString());
				}
			}
		}	
	}	
	
}
