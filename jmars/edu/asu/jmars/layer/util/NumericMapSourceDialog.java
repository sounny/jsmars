package edu.asu.jmars.layer.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.map2.CustomMapServer;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.stamp.sources.NumericStampSource;
import edu.asu.jmars.layer.stamp.sources.StampSource;
import edu.asu.jmars.swing.ImportantMessagePanel;
import mdlaf.components.table.MaterialTableCellRenderer;

public class NumericMapSourceDialog extends JDialog{
	
	private JRadioButton allRad;
	private JRadioButton elevRad;
	private JRadioButton stampRad;
	private JRadioButton customRad;
	private JCheckBox graphicChk;
	private JTextField filterTF;
	private final String filterPrompt = "Enter words to filter on";
	private JCheckBox titleBx;
	private JCheckBox abstractBx;
	private JButton searchBtn;
	private JTable resultsTbl;
	private JScrollPane tableSP;
	private JPanel tablePnl;
	private JPanel descPnl;
	private JScrollPane descSP;
	private JTextArea descTA;
	private JButton okBtn;
	private JButton cancelBtn;
	private ArrayList<MapSource> sources;
	private boolean defaultToElevation = false;
	
	private int pad = 1;
	private Insets in = new Insets(pad,pad,pad,pad);

	private boolean multiSources;
	private ArrayList<MapSource> selSources = new ArrayList<MapSource>();
	
	private NumericMapSourceDialog(JComponent relTo, boolean multiSelection, boolean defaultToElevation){
		super(new Frame(), "Numeric Map Source Selection", true);
		//set the location relative to but centered on the component passed in
		Point pt = relTo.getLocationOnScreen();
		setLocation(pt.x+80, pt.y+10);		
		
		multiSources = multiSelection;
		
		this.defaultToElevation = defaultToElevation;

		//get sources to populate table to start with
		//by default, use all numeric sources
		sources = new ArrayList<MapSource>();
		if (MapServerFactory.getMapServers() != null) {
			for (MapServer server: MapServerFactory.getMapServers()) {
				for (MapSource source: server.getMapSources()) {
					//TODO: remove when we update the capabilities to remove these
					if (source.getTitle().startsWith("AMES")) {
						continue;
					}
					if (source.hasNumericKeyword()) {
						if (this.defaultToElevation) {
							if (source.hasElevationKeyword()) {
								sources.add(source);
							}
						} else {
							sources.add(source);
						}
					}
				}
			}
		}
		Collections.sort(sources, byTitle);
		
		//build UI
		buildUI();
		setMinimumSize(new Dimension(700,700));
		filterTF.requestFocusInWindow();
		setVisible(true);
	}
	
	//TODO: remove this when we start using capabilities attribute
	private boolean isDefaultElevation(MapSource temp) {
		String title = temp.getTitle().toLowerCase();
		if (title.contains("mola") || title.contains("hrsc") || title.contains("hirise")) {
			return true;
		} else {
			return false;
		}
	}
	private Comparator<MapSource> byTitle = new Comparator<MapSource>() {
		public int compare(MapSource o1, MapSource o2) {
			if (defaultToElevation) {
				//TODO: Remove this and replace with source attribute to be included in capabilities
				boolean default1 = isDefaultElevation(o1);
				boolean default2 = isDefaultElevation(o2);
				if (default1 && !default2) {
					return -1;
				} else if (!default1 && default2) {
					return 1;
				} else {
					return o1.getTitle().compareTo(o2.getTitle());
				}
			} else {
				return o1.getTitle().compareTo(o2.getTitle());
			}
		}
	};
	
	private void buildUI(){
		//search section
		//-source panel (all, elevation, custom, graphic chk)
		JPanel sourcePnl = new JPanel(new GridBagLayout());		
		JLabel sourceLbl = new JLabel("Sources: ");
		allRad = new JRadioButton("All");		
		allRad.addActionListener(sourceListener);
		elevRad = new JRadioButton("Elevation");		
		elevRad.addActionListener(sourceListener);
		if (this.defaultToElevation) {
			elevRad.setSelected(true);
		} else {
			//set all option as default
			allRad.setSelected(true);
		}
		stampRad = new JRadioButton("Stamp");		
		stampRad.addActionListener(sourceListener);
		customRad = new JRadioButton("Custom");		
		customRad.addActionListener(sourceListener);
		graphicChk = new JCheckBox("Include Graphic Sources");
		graphicChk.setToolTipText("Graphic sources will be shown in the list; they may produce unexpected results.");
		graphicChk.addActionListener(sourceListener);
		//disable the custom radio button if the user is not logged in
		if(Main.USER == null || Main.USER.equals("")){
			customRad.setEnabled(false);
		}
		ButtonGroup sourceGrp = new ButtonGroup();
		sourceGrp.add(allRad);
		sourceGrp.add(elevRad);
		sourceGrp.add(stampRad);
		sourceGrp.add(customRad);
		sourcePnl.add(sourceLbl, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		sourcePnl.add(allRad, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		sourcePnl.add(elevRad, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		sourcePnl.add(stampRad, new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		sourcePnl.add(customRad, new GridBagConstraints(4, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		sourcePnl.add(graphicChk, new GridBagConstraints(0, 1, 5, 0, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		
		//search panel
		JPanel searchPnl = new JPanel(new GridBagLayout());		
		searchPnl.setBorder(new TitledBorder("Search"));
		JPanel filterPnl = new JPanel();		
		JLabel filterLbl = new JLabel("Filter:");
		filterTF = new JTextField(25);
		filterTF.setText(filterPrompt);
		filterTF.addFocusListener(filterFieldListener);
		filterPnl.add(filterLbl);
		filterPnl.add(filterTF);
		
		filterTF.getDocument().addDocumentListener(new DocumentListener() {
            
            @Override
            public void removeUpdate(DocumentEvent e) {
            	refreshTable(filterSources());
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
            	refreshTable(filterSources());
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
            	refreshTable(filterSources());
            }
        });
		
		titleBx = new JCheckBox("Source Title");
		//set just the title as default
		titleBx.setSelected(true);		
		titleBx.addActionListener(filterListener);
		abstractBx = new JCheckBox("Abstract/Citation");	
		abstractBx.addActionListener(filterListener);
		searchBtn = new JButton(searchAct);
		JPanel searchBtnPnl = new JPanel();		
		searchBtnPnl.add(searchBtn);
		searchPnl.add(sourcePnl, new GridBagConstraints(0, 0, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		searchPnl.add(filterPnl, new GridBagConstraints(0, 1, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		searchPnl.add(titleBx, new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		searchPnl.add(abstractBx, new GridBagConstraints(1, 2, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		searchPnl.add(searchBtnPnl, new GridBagConstraints(0, 3, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		
		//table section
		resultsTbl = new JTable();
		tableSP = new JScrollPane(resultsTbl, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		tablePnl = new JPanel();
		tablePnl.setLayout(new GridLayout(1, 1));
		tablePnl.setPreferredSize(new Dimension(300,250));
		tablePnl.add(tableSP);
		
		//description section
		descPnl = new JPanel(new GridLayout(1,1));
		descPnl.setBorder(new TitledBorder("Abstract/Citation"));
		descTA = new JTextArea();		
		descTA.setLineWrap(true);
		descTA.setWrapStyleWord(true);
		descTA.setEditable(false);
		descSP = new JScrollPane(descTA);
		descSP.setBorder(BorderFactory.createEmptyBorder());
		descPnl.add(descSP);		
		
		//ok/cancel section
		okBtn = new JButton(okAct);
		//don't enable until a source is selected
		okBtn.setEnabled(false);
		cancelBtn = new JButton(cancelAct);
		JPanel btnPnl = new JPanel();		
		btnPnl.add(okBtn);
		btnPnl.add(cancelBtn);
		
		ImportantMessagePanel panelImportantMsg = null;
		if (multiSources) {
			boolean isMac = Main.MAC_OS_X;
			String ctrlOrCmd = (isMac ? "Command" : "Ctrl");
			panelImportantMsg = new ImportantMessagePanel("Select multiple numeric sources using "+ctrlOrCmd + " + Click or Shift + Click.");
		} else {
			panelImportantMsg = new ImportantMessagePanel("Select a single numeric source");
		}
		
		
		//put it all together
		JPanel mainPnl = new JPanel(new GridBagLayout());		
		mainPnl.setBorder(new EmptyBorder(5, 5, 5, 5));
		int row = 0;
		mainPnl.add(searchPnl, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		mainPnl.add(panelImportantMsg, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		mainPnl.add(tablePnl, new GridBagConstraints(0, ++row, 1, 1, .7, .7, GridBagConstraints.CENTER, GridBagConstraints.BOTH, in, pad, pad));
		mainPnl.add(Box.createVerticalStrut(1), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		mainPnl.add(descPnl, new GridBagConstraints(0, ++row, 1, 1, .4, .4, GridBagConstraints.CENTER, GridBagConstraints.BOTH, in, pad, pad));
		mainPnl.add(btnPnl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		
		refreshTable(sources);
		
		setContentPane(mainPnl);
		getRootPane().setDefaultButton(searchBtn);
	}
	
	private AbstractAction searchAct = new AbstractAction("Search".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			refreshTable(filterSources());
		}
	};
	
	private AbstractAction okAct = new AbstractAction("OK".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			//clear selected list
			selSources.clear();
			//set the selected map
			MapSourceTableModel tm = (MapSourceTableModel)resultsTbl.getModel();
			if(multiSources){
				for(int row : resultsTbl.getSelectedRows()){
					selSources.add(tm.getSelectedSource(row));
				}
			}else{
				selSources.add(tm.getSelectedSource(resultsTbl.getSelectedRow()));
			}
			NumericMapSourceDialog.this.setVisible(false);
		}
	};
	
	private AbstractAction cancelAct = new AbstractAction("Cancel".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			selSources.clear();
			NumericMapSourceDialog.this.setVisible(false);
		}
	};
	
	private void refreshTable(ArrayList<MapSource> newSources){
		tablePnl.remove(tableSP);
		
		resultsTbl = loadTable(newSources);
		if(multiSources){
			resultsTbl.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		}else{
			resultsTbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		resultsTbl.getSelectionModel().addListSelectionListener(rowListener);
		
		int nameCol = resultsTbl.getColumnModel().getColumnIndex(MapSourceTableModel.NAME_COL);
		resultsTbl.getColumnModel().getColumn(nameCol).setPreferredWidth(470);
		tableSP = new JScrollPane(resultsTbl, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		tablePnl.add(tableSP);
		tablePnl.revalidate();
		
		//also clear the descTA because the entire table has been
		// dropped and reloaded, so no selection has been made yet
		descTA.setText("");
		//also disable the ok button since no selection is made yet
		okBtn.setEnabled(false);
	}
	
	private JTable loadTable(ArrayList<MapSource> newSources){
		//sources has been filtered by the time this method is called
		MapSourceTableModel model = new MapSourceTableModel(newSources);
		return new MapSourceTable(model);
	}
	
	
	private ArrayList<MapSource> filterSources(){
		//get the words to filter on
		String filterStr = filterTF.getText();
		String[] words = filterStr.split(" ");
		//if no words were entered, don't do anything
		if(words.length == 0 || filterStr.equals(filterPrompt)){
			return sources;
		}
		//create a new list to keep track of sources that match the filter words
		ArrayList<MapSource> matches = new ArrayList<MapSource>();
		//cycle through list of sources (which might be modified from the 
		// sourceListener if the user changed the radiobutton selection
		for(MapSource ms : sources){
			boolean isMatch = false;
			//if title box is selected, look at title text first
			if(titleBx.isSelected()){
				for(String word : words){
					//make the search case insensitive
					word = word.toUpperCase();
					String titleStr = ms.getTitle().toUpperCase();
					//if the title does not contain any of the words, mark as 
					// not a match
					if(titleStr.contains(word)){
						isMatch = true;
					}else{
						isMatch = false;
						break;
					}
				}
			}
			//next, look through abstract text if that box is selected AND
			// if a match was not already found from the title
			if(abstractBx.isSelected() && isMatch == false){
				for(String word : words){
					//make the search case insensitive
					word = word.toUpperCase();
					String absStr = ms.getAbstract().toUpperCase();
					//if the title does not contain any of the words, mark as 
					// not a match
					if(absStr.contains(word)){
						isMatch = true;
					}else{
						isMatch = false;			
						break;
					}
				}
			}
			
			//if the source was a match, add it to the new list
			if(isMatch){
				matches.add(ms);
			}
		}
		//return the sources to the new limited list
		return matches;
	}
	
	/**
	 * This listener is used on the radio buttons which filter
	 * the sources shown to the user.
	 */
	private ActionListener sourceListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			boolean needsNumeric = !graphicChk.isSelected();
			
			//clear the source list
			sources.clear();
			//if custom radio button is selected, only grab the 
			// custom map server from the server factory
			if(customRad.isSelected()){
				CustomMapServer server = MapServerFactory.getCustomMapServer();
				if(server != null){
					for(MapSource source : server.getMapSources()){
						if(source != null){
							//needs to be numeric
							if(needsNumeric){
								if(source.hasNumericKeyword()){
									sources.add(source);
								}
							}
							//graphic included
							else{
								sources.add(source);
							}
						}
					}
				}
			}else{
				//Otherwise get ahold of all of the sources and 
				// then filter based on user radio button selection
				if(MapServerFactory.getMapServers() != null){
					for(MapServer server : MapServerFactory.getMapServers()){
						if(server != null){
							for(MapSource source : server.getMapSources()){
								if(source != null){
									//TODO: remove when we update the capabilities to remove these
									if (source.getTitle().startsWith("AMES")) {
										continue;
									}
									//elevation is selected
									if(elevRad.isSelected() && source.hasElevationKeyword()){
										//needs to be numeric
										if(needsNumeric){
											if(source.hasNumericKeyword()){
												sources.add(source);
											}
										}
										//graphic included
										else{
											sources.add(source);
										}
									}
									//stamp is selected
									else if(stampRad.isSelected()){
										//needs to be numeric
										if(needsNumeric){
											if(source instanceof NumericStampSource){
												sources.add(source);
											}
										}
										//graphic included
										else if(source instanceof StampSource || source instanceof NumericStampSource){
											sources.add(source);
										}
									}
									//all is selected
									else if(allRad.isSelected()){
										//needs to be numeric
										if(needsNumeric){
											if(source.hasNumericKeyword()){
												sources.add(source);
											}
										}
										//graphic included
										else{
											sources.add(source);
										}
									}
								}
							}
						}
					}
				}
			}
			//alphebetize the sources
			Collections.sort(sources, byTitle);
			
			//refresh the table
			refreshTable(filterSources());
		}
	};
	
	private ActionListener filterListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			//if neither title or abstract check boxes are selected,
			// disable the search button
			if(!titleBx.isSelected() && !abstractBx.isSelected()){
				searchBtn.setEnabled(false);
				searchBtn.setToolTipText("Select at least one filter checkbox");
			}else{
				searchBtn.setEnabled(true);
			}
			
		}
	};
	
	private ListSelectionListener rowListener = new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
			int row = resultsTbl.getSelectedRow();
			if(row > -1){
				//get the selected mapsource from table model
				MapSource sel = ((MapSourceTableModel)resultsTbl.getModel()).getSelectedSource(row);
				//update description text
				descTA.setText(sel.getAbstract());
				//make sure the scroll bar is all the way to the top
				descTA.setCaretPosition(0);
				//enable the ok button because a source is selected
				okBtn.setEnabled(true);
			}else{
				//clear description text if no selection is made
				descTA.setText("");
				//disable the ok button because no source is selected
				okBtn.setEnabled(false);
			}
		}
	};
	
	private FocusListener filterFieldListener = new FocusListener() {
		public void focusLost(FocusEvent e) {
			//replace with the prompt if there is no text
			if(filterTF.getText() == null || filterTF.getText().equals("")){
				filterTF.setText(filterPrompt);
			}
		}
		public void focusGained(FocusEvent e) {
			//clear the prompt so the user can enter their filter words
			if(filterTF.getText().equals(filterPrompt)){
				filterTF.setText("");
			}
		}
	};
	
	/**
	 * Creates a NumericMapSourceDialog relative to the component passed
	 * in to this method.  Allows the option of single or multi source 
	 * selection.  Returns an ArrayList of the selected sources.  This
	 * List will only have one entry if false is passed for the multiSeleciton
	 * argument.  Can return null if this dialog is canceled or xed out of.
	 * @param relTo Component where to show this dialog relative to
	 * @param multiSelection Whether the user should be allowed to select multiple
	 * sources at once.  If false, only single selection in the source table is allowed.
	 * @param defaultToElevation make elevation the default selected source type
	 * @return An ArrayList of the chosen MapSources from this dialog, will only
	 * have one element if false is passed for multiselection, and can return null
	 * if the dialog is canceled or closed out of.
	 */
	public static ArrayList<MapSource> getUserSelectedSources(JComponent relTo, boolean multiSelection, boolean defaultToElevation){
		NumericMapSourceDialog nmsd = new NumericMapSourceDialog(relTo, multiSelection, defaultToElevation);
		//since the dialog is modal, it won't hit the next
		// line and return the source until it is closed
		return nmsd.selSources;
	}

	
	
	private class MapSourceTableModel extends AbstractTableModel{

		private ArrayList<MapSource> sources;
		
		private static final String NAME_COL = "Name";
		private static final String PPD_COL = "Max PPD";
		private final String columnNames[] = {NAME_COL, PPD_COL};
		private DecimalFormat df = new DecimalFormat("####");
		
		private MapSourceTableModel(ArrayList<MapSource> sources){
			this.sources = sources;
		}
		
		
		public int getRowCount() {
			if(sources == null) return 0;
			
			return sources.size();
		}

		public int getColumnCount() {
			return columnNames.length;
		}
		
	    public String getColumnName(int column) {
	    	return columnNames[column];
	    }

		public Object getValueAt(int rowIndex, int columnIndex) {
			MapSource ms = sources.get(rowIndex);
			
			switch(getColumnName(columnIndex)){
			case NAME_COL:
					return ms.getTitle();
			case PPD_COL:
					return df.format(ms.getMaxPPD());
			default:
				return null;
			}
		}
		
		private MapSource getSelectedSource(int row){
			return sources.get(row);
		}
	}
	
	private class MapSourceTable extends JTable {
		private MapSourceTableModel myModel;
		
		public MapSourceTable(MapSourceTableModel model){
			super(model);
			myModel = model;
		}
		
		public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
			Component c = super.prepareRenderer(renderer, row, column);
			MapSource source = myModel.getSelectedSource(row);
			
			//if graphic, make italic
			if(!source.hasNumericKeyword()){
				MaterialTableCellRenderer r = (MaterialTableCellRenderer)c;
				Font curFont = r.getFont();
				r.setFont(new Font(curFont.getFontName(), Font.ITALIC, curFont.getSize()));
			}
			
			return c;
		}

		
		public String getToolTipText(MouseEvent event) {
			String tip = "";
		
			int row = rowAtPoint(event.getPoint());

			MapSource source = myModel.getSelectedSource(row);
			
			if(!source.hasNumericKeyword()){
				tip = "Caution: JMARS thinks this is a graphic source.";
			}
			
			return tip;
		}
	}
}
