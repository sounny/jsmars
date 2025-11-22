package edu.asu.jmars.layer.stamp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.io.ObjectInputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.EventListenerList;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;
import cookxml.cookswing.util.SpringLayoutUtilities;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.MainGlass;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.msff.DataField;

public class StampLayerWrapper {
	
	protected EventListenerList listenerList = new EventListenerList();
	private String instrument;
	private String layergroup;
	private FilterParams filter = null;
	final JPanel queryPanel = new JPanel();
	private JXTaskPane basicTP = new JXTaskPane();
	private JPanel basicPnl = new JPanel(new SpringLayout());
	private JXTaskPane advTP = new JXTaskPane();
	private JPanel advBtnPnl = new JPanel();
	private JPanel advParamsPnl = new JPanel(new SpringLayout());
	private JPanel advPnl = new JPanel();
	private JButton addParamBtn;
	private JButton removeAllBtn;
	private HashMap<JPanel, Integer> panelToRowCount = new HashMap<JPanel, Integer>();
	
	private ArrayList<DataField> dataList = new ArrayList<DataField>();
	private ArrayList<DataField> advParamList;
	
	private JTextField minLonBox = new JTextField(8);
	private JTextField maxLonBox = new JTextField(8);
	private JTextField minLatBox = new JTextField(8);
	private JTextField maxLatBox = new JTextField(8);
	private Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();	
	private ImageIcon infoIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.INFO.withDisplayColor(imgColor)));					
	private List<StampFilter> filters = new ArrayList<StampFilter>();	
	private HashMap<DataField, JComponent[]> dataMap = new HashMap<DataField, JComponent[]>();
	//Keep track of the fields a user adds to the Advanced pane, so they can be easily
	// removed if "Remove All" is called
	private ArrayList<DataField> addedDataFields = new ArrayList<DataField>();
	
	ArrayList<GeneralPath> paths = null;
	ArrayList<String> srcItems = null;
	String srcName = null;

	// This is optional and only provided if a layer is being restored from a
	// saved JMARS session.
	StampLayerSettings settings = null;
	
	private HashMap<String, String> layerParams = new HashMap<String, String>();
	
	
	public StampLayerWrapper(String instrument, String srcName, ArrayList<String> srcItems, ArrayList<GeneralPath> paths) {
		this.instrument = instrument;
		
		this.srcName=srcName;
		this.srcItems=srcItems;
		this.paths=paths;

		init();
	}
	
	public StampLayerWrapper(String instrument, String layergroup) {
		this.instrument = instrument;
		this.layergroup = layergroup;

		init();
	}
	
	// Used for restoring from a saved JMARS session
	public StampLayerWrapper(StampLayerSettings settings) {
		this.settings = settings;
		this.instrument = settings.instrument;
		
		if (settings!=null) {
			paths = settings.paths;
			srcItems = settings.srcItems;
			srcName = settings.srcName;
		}
		
		init();	
	}
	
	private void init() {
		queryPanel.setLayout(new BorderLayout());
		
		JPanel messagePanel = new JPanel();
		messagePanel.setLayout(new FlowLayout());
		
		messagePanel.add(new JLabel("Loading query parameters from server..."));
		queryPanel.add(messagePanel, BorderLayout.CENTER);
		
		// TODO: Don't duplicate this functionality both here and within StampLayer
		{
			String urlStr = "ParameterFetcher?instrument="+getInstrument()+"&format=JAVA";
			HashMap<String, String> newParams;
			
			try {
				ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(urlStr));
				newParams = (HashMap<String, String>) ois.readObject();
				ois.close();
			} catch (Exception e) {
				newParams = new HashMap<String,String>();
			}
			
			for (String key: newParams.keySet()) {
				layerParams.put(key, newParams.get(key));
			}
		}
		
		basicTP = new JXTaskPane();
		basicTP.setTitle("Basic Parameters");
		basicTP.add(basicPnl);
		panelToRowCount.put(basicPnl, 0);
		advTP = new JXTaskPane();
		advTP.setTitle("Advanced Parameters");
		advPnl.setLayout(new BoxLayout(advPnl, BoxLayout.PAGE_AXIS));
		advTP.add(advPnl);
		addParamBtn = new JButton(addParamAct);
		removeAllBtn = new JButton(removeAllAct);
		removeAllAct.setEnabled(false);
		advBtnPnl.setBorder(new EmptyBorder(5, 5, 5, 5));
		advBtnPnl.add(addParamBtn);
		advBtnPnl.add(Box.createHorizontalStrut(10));
		advBtnPnl.add(removeAllBtn);
		panelToRowCount.put(advParamsPnl, 0);
		
		Thread thread = new Thread(new Runnable() {
			public void run() {

				DataField datafields[] = null;
				
				try {
					String urlStr = "FieldFetcher?instrument="+getInstrument();
					if (layergroup!=null&&layergroup.length()>0) urlStr+="&group="+layergroup;

					ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(urlStr));

					datafields = (DataField[])ois.readObject();

					ois.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

				dataList.addAll(Arrays.asList(datafields));
				
				//find out if this layer has any advanced parameters
				boolean hasAdvancedParams = false;
				int basicFields = 0;
				for (DataField df : dataList){
					if(df.getFieldName().equalsIgnoreCase("idlist") ||
							df.getFieldName().equalsIgnoreCase("latitude") ||
							df.getFieldName().equalsIgnoreCase("longitude")){
						basicFields++;
					}
				}
				if(dataList.size()-basicFields>0){
					hasAdvancedParams = true;
				}
				
				//create filters for each of the datafields except lat and lon
				for(DataField df : dataList){
					//only add a filter for fields with min and max range values
					if (df.getMinAllowedValue()!=null && df.getMaxAllowedValue()!=null) {
						StampFilter newFilter = new StampFilter(df);
						if (!df.getFieldName().equalsIgnoreCase("longitude") && !df.getFieldName().equalsIgnoreCase("latitude")) {
							filters.add(newFilter);	
						}	
					}
				}
				
				JXTaskPaneContainer taskContainer = new JXTaskPaneContainer();

				//if coming from an intersection, need to add that to the dataList
				// Also create the UI showing the list of intersecting paths
				if (paths!=null) {
					DataField df1 = new DataField("Intersection", srcName, "intersect", "intersect", "intersect",  false, true);
					dataList.add(0, df1);
					
					JXTaskPane intersectTP = new JXTaskPane();
					intersectTP.setTitle("Intersection Parameters");
					JPanel intersectPnl = new JPanel(new SpringLayout());
					intersectTP.add(intersectPnl);
					panelToRowCount.put(intersectPnl, 0);
					
					addFieldToPane(intersectPnl, df1, null, null);
					SpringLayoutUtilities.makeCompactGrid(intersectPnl, panelToRowCount.get(intersectPnl), 3, 6, 6, 6, 6);
					taskContainer.add(intersectTP);
				}
				
				
				//Cycle through dataMap and grab objects to populate the
				// 'basic params' category
				//Also, add parameters to the advanced category if they 
				// have default values (this is to preserve existing functionality
				// of querying on the default values of those parameters)
				for(DataField df : dataList){
					if(!df.isAdvanced()) {
						addFieldToPane(basicPnl, df, null, null);
					}
					else if(df.getDefaultValue()!=null || df.getDefaultValue2()!=null){
						addFieldToAdvPane(df);
					}
				}
				
				//adjust the layout of the basic panel after all fields are added
				SpringLayoutUtilities.makeCompactGrid(basicPnl, panelToRowCount.get(basicPnl), 3, 6, 6, 6, 6);
				taskContainer.add(basicTP);
				
				//only add the advanced task pane if there are advanced parameters
				if(hasAdvancedParams){
					refreshAdvPane();	
					taskContainer.add(advTP);
				}
				
				JPanel contPnl = new JPanel(new GridLayout(1,1));
				contPnl.add(taskContainer);
				final JScrollPane pane = new JScrollPane(contPnl);
				pane.getVerticalScrollBar().setUnitIncrement(16);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						queryPanel.removeAll();
						queryPanel.setLayout(new BorderLayout());
						queryPanel.add(pane, BorderLayout.CENTER);
						queryPanel.validate();
						if (queryPanel.getParent()!=null) {
							Container c = queryPanel.getParent();
							while (c!=null && !(c instanceof JDialog)) {
								c = c.getParent();
							}
							if (c!=null) {
								((JDialog)c).pack();
								c.setSize(c.getWidth()+20, Math.max(Main.mainFrame.getHeight(), 600));
							}
						}
					}
				});
				
			}
		});
		
		thread.setName("FieldFetcher");
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.setDaemon(true);
		thread.start();		
	}
	
	
	private AbstractAction geoBoxAction = new AbstractAction("Set Lon/Lat to bounds of View".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			minLonBox.setText("MainView");
			maxLonBox.setText("");
			minLatBox.setText("");
			maxLatBox.setText("");
		}
	};
	
	final JButton latLonButton = new JButton(geoBoxAction);
	
	private AbstractAction addParamAct = new AbstractAction("Add Parameter(s)...".toUpperCase()) {
		public void actionPerformed(ActionEvent arg0) {
			//remove image ids, intersection, latitude, and longitude parameters
			if(advParamList == null){
				advParamList = new ArrayList<DataField>();
				for(DataField df : dataList){
					if(df.getFieldName().equalsIgnoreCase("idlist") ||
							df.getFieldName().equalsIgnoreCase("latitude") ||
							df.getFieldName().equalsIgnoreCase("longitude") ||
							df.getFieldName().equalsIgnoreCase("intersect")){
						continue;
					}
					advParamList.add(df);
				}
			}
			//set the parent for the AddParameterDialog
			Component root = SwingUtilities.getRoot(queryPanel);
			JFrame parentFrame = null;
			JDialog parentDialog = null;
			if (root instanceof JFrame) {
				parentFrame = (JFrame) root;
				new AddParameterDialog(parentFrame, StampLayerWrapper.this, advParamList, queryPanel);
			} else if (root instanceof JDialog) {
				parentDialog = (JDialog) root;
				new AddParameterDialog(parentDialog, StampLayerWrapper.this, advParamList, queryPanel);
			} else {
				//should never get here, but this is used in a lot of places, so send a null JFrame by default
				new AddParameterDialog(parentFrame, StampLayerWrapper.this, advParamList, queryPanel);
			}
		}
	};
	
	private AbstractAction removeAllAct = new AbstractAction("Remove All".toUpperCase()) {
		public void actionPerformed(ActionEvent arg0) {
			//TODO: convert to Util.showConfirmDialog
			//confirm with the user
			int response = JOptionPane.showConfirmDialog(removeAllBtn, "Are you sure you wish to remove all added parameters?",
					"Confirm Remove All", JOptionPane.YES_NO_OPTION);

			if(response == JOptionPane.YES_OPTION){
				//remove data fields from data map
				for(DataField df : addedDataFields){
					dataMap.remove(df);
				}
				
				//clear advanced panel ui
				advParamsPnl.removeAll();
				panelToRowCount.put(advParamsPnl, 0);
				refreshAdvPane();
				
				//clear the added data fields array
				addedDataFields.clear();
				
				//refresh the button in case it needs to be disabled
				refreshRemoveAllEnabled();
			}
		}
	};
	
	/**
	 * Only enable the "Remove All" button if there are added parameters 
	 * in the Advanced Pane
	 */
	private void refreshRemoveAllEnabled(){
		removeAllBtn.setEnabled(addedDataFields.size()!=0);
	}
	
	public void refreshAdvPane(){
		advPnl.removeAll();
		SpringLayoutUtilities.makeCompactGrid(advParamsPnl, panelToRowCount.get(advParamsPnl), 3, 6, 6, 6, 6); 
		
		advPnl.add(advBtnPnl);
		advPnl.add(advParamsPnl);
		advParamsPnl.revalidate();
		
		queryPanel.revalidate();
	}
	
	
	/**
	 * Adds a new datafield to the current list and creates UI components for that
	 * field.  Also sets their values if not null.
	 * @param dfName  The name of the datafield.getDisplayName() to add to the list
	 * @param minVal  The minimum value or only value if only one value is needed
	 * @param maxVal  The maximum value
	 */
	public void addFieldToAdvPane(String dfName, String minVal, String maxVal){
		//Cycle through the datafields to find which ones to add to the advanced pane
		for (DataField df : dataList) {
			if (df.getDisplayName().equalsIgnoreCase(dfName)) {
				addFieldToPane(advParamsPnl, df, minVal, maxVal);
				refreshRemoveAllEnabled();
				return;
			}
		}
	}
	
	/**
	 * Adds a new datafield to the current list and creates UI components for that
	 * field.  The components values will be left blank.
	 * @param df  The datafield to add to the list and UI
	 */
	public void addFieldToAdvPane(DataField df){
		addFieldToPane(advParamsPnl, df, null, null);
		refreshRemoveAllEnabled();
	}
	
	private void addFieldToPane(JPanel paramPnl, DataField df, String minVal, String maxVal){
		JPanel panel = new JPanel(new SpringLayout());
		
		JLabel li = new JLabel(infoIcon);
		JLabel nameLbl = new JLabel(df.getDisplayName(), JLabel.TRAILING);
		
		int cols = 0;
		
		//set the tooltip for the info icon label
		if (df.getMinAllowedValue()!=null && df.getMaxAllowedValue()!=null) {
			li.setToolTipText("<html>"+df.getFieldTip() + "<p>" + "Values range from: " + df.getMinAllowedValue() + " to " + df.getMaxAllowedValue()+"</html>");
		} else {
			String tip = df.getFieldTip();
			if (tip!=null && tip.trim().length()>0) {
				li.setToolTipText("<html>"+df.getFieldDesc() + "<p>"+ df.getFieldTip()+"</html>");
			} else {
				li.setToolTipText("For help with this field, click the Help button at the bottom of this tab");
			}
		}
		
		paramPnl.add(nameLbl);
		paramPnl.add(li);
		
		//now look at the datafield in more detail to create and define the
		// ui components for it
		
		String fieldName = df.getFieldName();
		//Image Ids and exclude list
		if (fieldName.equalsIgnoreCase("idList") || fieldName.equalsIgnoreCase("excludeList")) {
			JTextArea textArea[] = new JTextArea[1];
			textArea[0]=new JTextArea(3, 12);
			//add components to map
			if(dataMap.containsKey(df)){
				df = createUniqueDataField(df);
			}
			dataMap.put(df, textArea);
			
			if (settings!=null) {
				textArea[0].setText(getSettingsValueFor(fieldName));
			} else {
				String val = df.getDefaultValue();
				if (val!=null) textArea[0].setText(val);
			}
			textArea[0].setWrapStyleWord(true);
			textArea[0].setLineWrap(true);
			JScrollPane areaScrollPane = new JScrollPane(textArea[0]);
			areaScrollPane.setVerticalScrollBarPolicy(
					JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			areaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);                		        
			areaScrollPane.setPreferredSize(new Dimension(250, 50));
			panel.add(areaScrollPane);
			cols = 1;
		}
		
		else{
			if (fieldName.equalsIgnoreCase("intersect")) {
				JList area[] = new JList[1];
				Vector<String> v = new Vector<String>();
				for (String val : srcItems) {
					v.add(val);
				}
				area[0] = new JList<String>(v);
				
				JScrollPane areaScrollPane = new JScrollPane(area[0], JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				areaScrollPane.setPreferredSize(new Dimension(250, 50));
				
				panel.setLayout(new BorderLayout());
				panel.add(BorderLayout.CENTER, areaScrollPane);
				
				//add components to map
				if(dataMap.containsKey(df)){
					df = createUniqueDataField(df);
				}
				dataMap.put(df, area);
			} 
			else if (df.isMultiSelect()){
				String values[] = df.getValidValues();
	
				JCheckBox options[] = new JCheckBox[values.length];
				
				ArrayList<String> selectedVals = new ArrayList<String>();
				if (settings!=null) {
					String vals[]=getSettingsValueFor("min"+fieldName).split("%2C");
					for (int i2=0; i2<vals.length; i2++) {
						selectedVals.add(vals[i2]);
					}
				} else {
					String val = df.getDefaultValue();
					if (val!=null) {
						String vals[]=val.split("%2C");
						for (int i2=0; i2<vals.length; i2++) {
							selectedVals.add(vals[i2]);
						}				
					}
				}
				
				for (int j=0 ; j<options.length; j++) {
					options[j]=new JCheckBox(values[j]);
					if (selectedVals.contains(values[j])) {
						options[j].setSelected(true);
					}
				}
				
				JPanel chkPnl = new JPanel(new SpringLayout());
				for (JCheckBox cb : options) {
					chkPnl.add(cb);
				}            			
				// If there's an odd number of options, add a blank one so the compact grid still works
				if (options.length%2==1) {
					chkPnl.add(new JPanel());
				}
				SpringLayoutUtilities.makeCompactGrid(chkPnl, 2, (int)Math.ceil(options.length/2.0), 6, 6, 6, 6);
	
				panel.add(chkPnl);
				cols = 1;
				
				//add components to map
				if(dataMap.containsKey(df)){
					df = createUniqueDataField(df);
				}
				dataMap.put(df, options);
			}
			else if (df.isRange()) {   
				if (df.getValidValues()!=null) {
					JComboBox combo[] = new JComboBox[2];
					combo[0]=new JComboBox(df.getValidValues());
					combo[1]=new JComboBox(df.getValidValues());
					//add components to map
					if(dataMap.containsKey(df)){
						df = createUniqueDataField(df);
					}
					dataMap.put(df, combo);
					
					if (settings!=null) {
						combo[0].setSelectedItem(getSettingsValueFor("min"+fieldName));
						combo[1].setSelectedItem(getSettingsValueFor("max"+fieldName));	
					} else {
						String val = df.getDefaultValue();
						String val2 = df.getDefaultValue2();
						if (val!=null) combo[0].setSelectedItem(val);
						if (val2!=null) combo[1].setSelectedItem(val2);									
					}
					
					//override and set vals if coming from template
					if(minVal != null){
						combo[0].setSelectedItem(minVal);
					}
					if(maxVal != null){
						combo[1].setSelectedItem(maxVal);
					}
					
					panel.add(combo[0]);
					panel.add(new JLabel(" to "));
					panel.add(combo[1]);
					cols = 3;
					
				} else {
					JTextField textField[] = new JTextField[2];
	
					if (fieldName.equalsIgnoreCase("longitude")) {
						textField[0] = minLonBox;
						textField[1] = maxLonBox;
					} else if (fieldName.equalsIgnoreCase("latitude")) {
						textField[0] = minLatBox;
						textField[1] = maxLatBox;
					} else {
						textField[0]=new JTextField(4);								
						textField[1]=new JTextField(4);   								
					}
	
					if (settings!=null) {
						textField[0].setText(getSettingsValueFor("min"+fieldName));
						textField[1].setText(getSettingsValueFor("max"+fieldName));	
					} else {
						String val = df.getDefaultValue();
						String val2 = df.getDefaultValue2();
						if (val!=null) textField[0].setText(val);
						if (val2!=null) textField[1].setText(val2);	
					}
	
					//override and set vals if coming from template
					if(minVal != null){
						textField[0].setText(minVal);
					}
					if(maxVal != null){
						textField[1].setText(maxVal);
					}
					
					//add components to map
					if(dataMap.containsKey(df)){
						df = createUniqueDataField(df);
					}
					dataMap.put(df, textField);
	
					panel.add(textField[0]);
					panel.add(new JLabel(" to "));
					panel.add(textField[1]);
					cols = 3;
				}                    		
			}
			else if (df.getValidValues()!=null) {
				JComboBox combo[] = new JComboBox[1];
				combo[0]=new JComboBox(df.getValidValues());
				//add components to map
				if(dataMap.containsKey(df)){
					df = createUniqueDataField(df);
				}
				dataMap.put(df, combo);
				
				if (settings!=null) {
					combo[0].setSelectedItem(getSettingsValueFor(fieldName));
				} else {
					String val = df.getDefaultValue();
					if (val!=null) combo[0].setSelectedItem(val);
				}
				
				//override and set vals if coming from template
				if(minVal != null){
					combo[0].setSelectedItem(minVal);
				}
				
				JPanel comboPnl = new JPanel();
				comboPnl.add(combo[0]);
				
				panel.add(comboPnl);
				cols = 1;
				
			} else {
				JTextField textField[] = new JTextField[1];
				textField[0]=new JTextField(4);
				//add components to map
				if(dataMap.containsKey(df)){
					df = createUniqueDataField(df);
				}
				dataMap.put(df, textField);
				
				if (settings!=null) {
					textField[0].setText(getSettingsValueFor(fieldName));
				} else {
					String val = df.getDefaultValue();
					if (val!=null) textField[0].setText(val);
				}
				
				panel.add(textField[0]);
				cols = 1;
			}
		}
		
		//add the delete button if needed
		// and add to the addfields list for "remove all" functionality
		if(paramPnl == advParamsPnl){
			final DataField myDf = df;
			addedDataFields.add(myDf);
			
			JButton delBtn = new JButton("x");
			
			delBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					//remove datafield from map
					dataMap.remove(myDf);
					//remove datafield from list
					addedDataFields.remove(myDf);
					//remove ui components
					paramPnl.remove(nameLbl);
					paramPnl.remove(li);
					paramPnl.remove(panel);
					paramPnl.remove(delBtn);
					//compress the UI
					int rows = panelToRowCount.get(paramPnl);
					panelToRowCount.put(paramPnl, --rows);
					refreshAdvPane();
					refreshRemoveAllEnabled();
				}
			});
			
			panel.add(delBtn);
			cols++;
		}
		
		// This is true for everything except "intersect" values currently.  JList doesn't play nice with SpringLayout.
		if (panel.getLayout() instanceof SpringLayout) {
			//compress the gui
			SpringLayoutUtilities.makeCompactGrid(panel, 1, cols, 6, 6, 6, 6);
		}
		
		//increase the rowcount in the hashmap
		int rows = panelToRowCount.get(paramPnl);
		panelToRowCount.put(paramPnl, ++rows);
		
		paramPnl.add(panel);
		if(fieldName.equalsIgnoreCase("latitude")){
			JPanel buttonPnl = new JPanel();
			buttonPnl.add(latLonButton);
			paramPnl.add(new JLabel());
			paramPnl.add(new JLabel());
			paramPnl.add(buttonPnl);
			//increase row count again
			panelToRowCount.put(paramPnl, ++rows);
		}
	}
	
	private DataField createUniqueDataField(DataField df){
		//calculate proper integer for name
		int i = 1;
		for(DataField data : dataMap.keySet()){
			if(data.getFieldName().contains(df.getFieldName())){
				i++;
			}
		}
		
		//create a new datafield from the one passed in
		DataField newDF = new DataField(df.getCategory(), df.getDisplayName()+i, df.getTableName(), df.getColumnName()+i, df.getFieldName()+i, df.isRange(), df.getMinAllowedValue(), df.getMaxAllowedValue(), df.getValidValues(), df.isMultiSelect(), df.isAdvanced());
		
		return newDF;
	}
	
	public String getInstrument() {
		return instrument;
	}

	public boolean isSpectra() {
    	String param = layerParams.get(StampLayer.SPECTRA_DATA);
    	if (param!=null && param.equalsIgnoreCase("true")) {
    		return true;
    	}
    	return false; 
	}

	public boolean supportsTemplates() {
    	String param = layerParams.get(StampLayer.SUPPORTS_TEMPLATES);
    	if (param!=null && param.equalsIgnoreCase("true")) {
    		return true;
    	}
    	return false; 		
	}
		
	private String getSettingsValueFor(String fieldName) {
		String query = settings.queryStr;
		
		int loc = query.indexOf("&"+fieldName+"=");
		if (loc<0) return "";
		
		String sub = query.substring(loc);
		loc = sub.indexOf("=");
		
		sub = sub.substring(loc+1);
		
		loc = sub.indexOf("&");
		
		if (loc<0) return URLDecoder.decode(sub);
		
		sub = sub.substring(0,loc);
		
		return URLDecoder.decode(sub);
	}
	
	public JPanel getContainer() {
		return queryPanel;
	}

	public List<StampFilter> getFilters() {
		return filters;
	}
	
	/**
	 * @return The sql query for the current parameters and fields.
	 * Generates the FilterParams first if it hasn't been created yet.
	 */
	public String getQuery() { 
		if(filter == null){
			getFilter();
		}
//		System.out.println("Sql: "+filter.getSql());
		return filter.getSql();
	}
	
	/**
	 * Create the FilterParams object if it has not been created yet.
	 * Use the datafield list to pass through all the applicable 
	 * datafields, and pass in the dataMap as well.
	 * @return  The FilterParams object that can be used to generate
	 * the appropriate sql query
	 */
	public FilterParams getFilter(){
		if(filter == null){
			filter = new FilterParams(getInstrument(), dataMap);
			filter.paths = paths;
		}
		return filter;
	}
	
	public HashMap<DataField, JComponent[]> getDataMap(){
		return dataMap;
	}
}