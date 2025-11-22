package edu.asu.jmars.lmanager;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.map2.custom.CM_Manager;
import edu.asu.jmars.swing.IconButtonUI;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeButton;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeComboBox;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeTextField;

public class AddLayerDialog {
	private static final int CUSTOM_MAP_TAB = 2;
	private static final int FAVORITES_TAB = 3;
	
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
    private static Icon tooltipIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.INFO));
	
	private static AddLayerDialog instance = null;
	private JDialog newAddLayerDialog;
	private SearchProvider searchProvider = SearchProvider.getInstance();
	
    private HashMap<String,ArrayList<LayerParameters>> layersById = new HashMap<String, ArrayList<LayerParameters>>();
    private HashMap<String, Set<String>> layersByDesc = new HashMap<String, Set<String>>();
	
	private JPanel mainResultsPane;
	private JScrollPane resultsSP;
	private JPanel searchHeaderPanel;
	private JPanel faveHeaderPanel;
	private JPanel customMapHeaderPanel;
	private  Dimension dialogSize;
	private JPanel searchPanel;
	private JTextField searchTF;
	private JDialog suggestionDialog;
	private JPanel suggestionResultsPanel;
	private JScrollPane suggestionResultsSP;
	private JScrollPane favoritesSP;
	private JScrollPane customMapSP;
	private JPanel favoritesInnerPanel;
	private JPanel favoritesPanel;
	private JPanel customMapPanel;
	private int selectedSuggestion = -1;
	private boolean clearButtonFlag = false;
	private boolean suppressSuggestionDialog = false;
	private boolean closeOnSelection = false;
	
	private JTabbedPane tabs = null;
	
	private SearchResultRow selectedRow = null;
	private int selectedRowInt = -1;
//    private DefaultListModel<SearchResultRow> resultListModel = null;
	
	private AddLayerDialog() {
		//prevent circumventing the singleton
	}
	public JDialog getAddLayerDialog() {
		if (newAddLayerDialog == null) {
			createAddLayerDialog();
		}
        return newAddLayerDialog;
    }
	public static AddLayerDialog getInstance() {
		if (instance == null) {
			instance = new AddLayerDialog();
		}
		return instance;
	}
	public void closeIfShowing() {
		if (this.isShowing()) {
			closeAddLayerDialog();
		}
	}
	public void initializeAddLayerDialog() {
		if (newAddLayerDialog == null) {
			createAddLayerDialog();
		}
	}
	public boolean isShowing() {
		if (newAddLayerDialog == null) {
			return false;
		}
		return newAddLayerDialog.isShowing();
	}
	private void createAddLayerDialog() {
        newAddLayerDialog = new JDialog(Main.mainFrame);
        tabs = new JTabbedPane();
        resultsSP = new JScrollPane();
        searchPanel = new JPanel();
        searchHeaderPanel = new JPanel();
        customMapHeaderPanel = new JPanel();
        faveHeaderPanel = new JPanel();
        mainResultsPane = new JPanel();
        
        SearchProvider.prepareSearchComplete();//make sure search prep thread has finished
        displayRows.clear();
        displayRows.addAll(searchProvider.buildInitialLayerList(true, false));
        setDialogSize();
        JRootPane rootPane = newAddLayerDialog.getRootPane();
        rootPane.setWindowDecorationStyle(JRootPane.NONE);
        rootPane.setBorder(null);
        newAddLayerDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        newAddLayerDialog.setBackground(Color.black);        
        newAddLayerDialog.add(layoutAddLayerDialog());
        
//        buildMainResultsPane(true);
        
        selectedRowInt = 0;
        resultsSP.revalidate();
        suppressSuggestionDialog = true;
        searchTF.grabFocus();
        suppressSuggestionDialog = false;
        
        //motion listener for hover effect and selection of row
        mainResultsPane.addMouseMotionListener(new MouseAdapter() {

			@Override
			public void mouseMoved(MouseEvent e) {
				Component comp = e.getComponent();
				if (!(comp instanceof SearchResultRow)) {
					comp = SwingUtilities.getDeepestComponentAt(mainResultsPane, e.getX(), e.getY());
					if (comp instanceof JButton || comp instanceof JLabel) {
						comp = comp.getParent();
					}
				}
				
				if (comp instanceof SearchResultRow) {
					if (selectedRow != null) {
						selectedRow.resetHighlight();
					}
					selectedRow = (SearchResultRow) comp;
					selectedRowInt = displayRows.indexOf(selectedRow);
					selectedRow.highlightForSelection(new Point(e.getX(), e.getY()));
					e.consume();
				} 
			}
        	
        });

    }
	public void showAddLayerDialog() {
		newAddLayerDialog.setVisible(true);
		if (selectedRow == null) {
			resetSelectedRow();
		}
	}
	public void setAddLayerDialogLocation(int x, int y) {
		newAddLayerDialog.setLocation(x,y);
	}
	public void closeAddLayerDialog(){ 
        if (newAddLayerDialog != null) {
            newAddLayerDialog.setVisible(false);
            LManager.getLManager().getMainPanel().resetAddLayerButtonIcons();
        }
        if (suggestionDialog != null) {
            suggestionDialog.setVisible(false);
        }
    }
	public void destroyAddLayerDialog() {
    	closeAddLayerDialog();
        newAddLayerDialog = null;
    }
	
	private void buildMainResultsPane(boolean initialLayout) {
        JLabel noResultsLbl = new JLabel("No results to display");
        
        if (displayRows == null || displayRows.size() == 0) {
            noResultsLbl.setVisible(true);
        } else {
            noResultsLbl.setVisible(false);
        }
        
        mainResultsPane.removeAll();
        GroupLayout paneLayout = new GroupLayout(mainResultsPane);
        mainResultsPane.setLayout(paneLayout);
        
        ParallelGroup panePar = paneLayout.createParallelGroup();
        SequentialGroup paneSeq = paneLayout.createSequentialGroup(); 
        
        List<SearchResultRow> listCopy = (ArrayList<SearchResultRow>) displayRows.clone();
        if (listCopy != null && listCopy.size() > 0) {
            for(SearchResultRow row : listCopy) {
            	if (row == null) {
            		break;
            	}
            	
            	panePar.addComponent(row);//horizontal
                paneSeq.addComponent(row);//vertical
            }
        } else {
            panePar.addComponent(noResultsLbl);
            paneSeq.addComponent(noResultsLbl);
        }
        paneLayout.setHorizontalGroup(panePar);
        paneLayout.setVerticalGroup(paneSeq);
        
        resetSelectedRow();
        resultsSP.revalidate();
        resultsSP.getVerticalScrollBar().setValue(resultsSP.getVerticalScrollBar().getMinimum());
        resultsSP.repaint();
    }
    public void setDialogSize() {       
        int height = LManager.getLManager().getHeight() - 60;//60 for now because of the tabs and later because of the docked section
        if (height < 600) {
            height = 600;
        }
        dialogSize = new Dimension(600, height);
        newAddLayerDialog.setSize(dialogSize);
        newAddLayerDialog.setPreferredSize(dialogSize);
        newAddLayerDialog.setMinimumSize(dialogSize);
        tabs.setPreferredSize(new Dimension(590, height - 100));
        resultsSP.setPreferredSize(new Dimension(580,height-300));
        resultsSP.revalidate();
        searchHeaderPanel.setPreferredSize(new Dimension(580, 30));
        customMapHeaderPanel.setPreferredSize(new Dimension(580, 30));
        faveHeaderPanel.setPreferredSize(new Dimension(580, 30));
   }
   
    
    private JPanel layoutSearchPanel() {
        JPanel searchLayersPanel = new JPanel();
        initSearchPanel(true);
        
        getResultHeaderPanel(searchHeaderPanel);
         
        buildMainResultsPane(true);
//        resultsList.setBorder(null);
//        resultsSP.add(resultsList);
//        resultsSP.setViewportView(resultsList);
        resultsSP.add(mainResultsPane);
        resultsSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        resultsSP.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        resultsSP.setViewportView(mainResultsPane);
//        resultsSP.setPreferredSize(new Dimension(200,200));
        

        GroupLayout allLayersLayout = new GroupLayout(searchLayersPanel);
        searchLayersPanel.setLayout(allLayersLayout);

        allLayersLayout.setHorizontalGroup(allLayersLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(searchPanel)
            .addComponent(searchHeaderPanel)
            .addComponent(resultsSP));
        allLayersLayout.setVerticalGroup(allLayersLayout.createSequentialGroup()
            .addComponent(searchPanel)
            .addGap(10)
            .addComponent(searchHeaderPanel)
            .addComponent(resultsSP));
        
        return searchLayersPanel;
    }
    
    private void layoutCustomMapPanel() {
    	if (customMapSP == null) {
    		customMapSP = new JScrollPane();
    		customMapSP.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    		customMapSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        } 
        
        getResultHeaderPanel(customMapHeaderPanel);
        
        JPanel cmInnerPanel = new JPanel();
        GroupLayout cmLayout = new GroupLayout(cmInnerPanel);
        cmInnerPanel.setLayout(cmLayout);

        ParallelGroup colGroup = cmLayout.createParallelGroup(Alignment.LEADING);
        SequentialGroup rowGroup = cmLayout.createSequentialGroup();
        
        ArrayList<SearchResultRow> maps = searchProvider.getCustomMapLayers();
        for(SearchResultRow cm : maps) {
        	cm.buildRow();
            colGroup.addComponent(cm);
            rowGroup.addComponent(cm);
        }
        
        cmLayout.setHorizontalGroup(colGroup);
        cmLayout.setVerticalGroup(rowGroup);
        
        customMapSP.add(cmInnerPanel);
        customMapSP.setViewportView(cmInnerPanel);
        customMapSP.setPreferredSize(new Dimension(300,300));
        
        customMapPanel = new JPanel();
        GroupLayout cmPLayout = new GroupLayout(customMapPanel);
        customMapPanel.setLayout(cmPLayout);
        
        cmPLayout.setHorizontalGroup(cmPLayout.createParallelGroup(Alignment.LEADING)
        		.addComponent(customMapHeaderPanel)
        		.addComponent(customMapSP)
        );
        cmPLayout.setVerticalGroup(cmPLayout.createSequentialGroup()
        		.addGap(5)
        		.addComponent(customMapHeaderPanel)
        		.addComponent(customMapSP)
        );
    }
    
    
    private JPanel getResultHeaderPanel(JPanel headerPnl) {

        JLabel typeLbl = new JLabel("Type");
        JLabel nameLbl = new JLabel("Name");
        JLabel favLbl = new JLabel("Fave");
        JLabel contourLbl = new JLabel("Advanced");
        
        GroupLayout layout = new GroupLayout(headerPnl);
        layout.setAutoCreateGaps(false);
        
        headerPnl.setLayout(layout);
        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addGap(12)
            .addComponent(typeLbl)
            .addGap(48)
            .addComponent(nameLbl)
            .addGap(SearchResultRow.LAYER_NAME_PANEL_WIDTH - 20)
            .addComponent(contourLbl)
            .addGap(27)
            .addComponent(favLbl));
        layout.setVerticalGroup(layout.createSequentialGroup()
        	.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        		.addComponent(typeLbl)
        		.addComponent(nameLbl)
        		.addComponent(contourLbl)
        		.addComponent(favLbl))
        	.addGap(5));
        
        return searchHeaderPanel;
    }
    
    private void initFavoritesPanel() {
        favoritesSP = new JScrollPane();
        favoritesSP.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        favoritesSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
    	getResultHeaderPanel(faveHeaderPanel);
    	
        favoritesInnerPanel = new JPanel();
        GroupLayout favoritesLayout = new GroupLayout(favoritesInnerPanel);
        favoritesInnerPanel.setLayout(favoritesLayout);

        ParallelGroup colGroup = favoritesLayout.createParallelGroup(Alignment.LEADING);
        SequentialGroup rowGroup = favoritesLayout.createSequentialGroup();
        
        ArrayList<SearchResultRow> faves = searchProvider.getFavoriteLayers();
        for(SearchResultRow fave : faves) {
            colGroup.addComponent(fave);
            rowGroup.addComponent(fave);
        }
        
        favoritesLayout.setHorizontalGroup(colGroup);
        favoritesLayout.setVerticalGroup(rowGroup);
        
        favoritesSP.add(favoritesInnerPanel);
        favoritesSP.setViewportView(favoritesInnerPanel);
        favoritesSP.setPreferredSize(new Dimension(300,300));
        
        favoritesPanel = new JPanel();
        GroupLayout gl = new GroupLayout(favoritesPanel);
        favoritesPanel.setLayout(gl);
        gl.setHorizontalGroup(gl.createParallelGroup(Alignment.LEADING)
            .addComponent(faveHeaderPanel)
            .addComponent(favoritesSP)
        );
        gl.setVerticalGroup(gl.createSequentialGroup()
            .addGap(5)
        	.addComponent(faveHeaderPanel)
        	.addComponent(favoritesSP)
        );
//        return favoritesSP;
    }
    
    private void refreshFavoritesPanel() { 
    	if (favoritesInnerPanel.getComponentCount() > 0) {
    		favoritesInnerPanel.removeAll();
    	}
    	
        GroupLayout favoritesLayout = new GroupLayout(favoritesInnerPanel);
        favoritesInnerPanel.setLayout(favoritesLayout);
        JLabel noFaves = new JLabel("No favorites to display");
        
        ParallelGroup colGroup = favoritesLayout.createParallelGroup(Alignment.LEADING);
        SequentialGroup rowGroup = favoritesLayout.createSequentialGroup();
        
        ArrayList<SearchResultRow> faves = searchProvider.getFavoriteLayers();
        if (faves.size() == 0) {
            colGroup.addComponent(noFaves);
            rowGroup.addComponent(noFaves);
        } else {
            for(SearchResultRow fave : faves) {
                colGroup.addComponent(fave);
                rowGroup.addComponent(fave);
            }
        }
        
        favoritesLayout.setHorizontalGroup(colGroup);
        favoritesLayout.setVerticalGroup(rowGroup);
        
        favoritesSP.revalidate();
        favoritesSP.repaint();
        
    }
    public void setAutocloseSelection(boolean close) {
    	this.closeOnSelection = close;
    }
    private JPanel layoutAddLayerDialog() {
        JPanel searchLayersPanel = layoutSearchPanel();
        JPanel browsePanel = layoutBrowsePanel();
        initFavoritesPanel();
        layoutCustomMapPanel();
        JButton doneButton = new JButton(doneButtonAct);
        JPanel buttonExtension = new JPanel();
        ImageIcon closeIcon = new ImageIcon(ImageFactory.createImage(
                ImageCatalogItem.CLEAR.withDisplayColor(((ThemeImages) GUITheme.get("images")).getLayerfill())));
        JLabel closeXLbl = new JLabel();  
        closeXLbl.setIcon(closeIcon);
        closeXLbl.setBackground(((ThemeButton)GUITheme.get("button")).getDefaultback());
        closeXLbl.addMouseListener(new MouseListener() {
            
            @Override
			public void mouseReleased(MouseEvent e) {
			}
            
            @Override
			public void mousePressed(MouseEvent e) {
			}
          
             @Override
            public void mouseExited(MouseEvent e) {
                e.getComponent().setCursor(Cursor.getDefaultCursor());
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {              
            	closeAddLayerDialog();
            }
        });
    
        buttonExtension.setBackground(((ThemeButton)GUITheme.get("button")).getDefaultback());
        JLabel titleLbl = new JLabel("Add New Layer");
        titleLbl.setBackground(((ThemeButton)GUITheme.get("button")).getDefaultback());
        titleLbl.setForeground(ThemeProvider.getInstance().getAction().getDefaultForeground());  //color same as button
        buttonExtension.setPreferredSize(new Dimension(550,LManager.getLManager().getMainPanel().getAddButtonHeight()));
        
        GroupLayout beLayout = new GroupLayout(buttonExtension);
        buttonExtension.setLayout(beLayout);
        
        beLayout.setHorizontalGroup(beLayout.createSequentialGroup()
        	.addGap(8)
            .addComponent(titleLbl)
            .addGap(462)
            .addComponent(closeXLbl));
        beLayout.setVerticalGroup(beLayout.createSequentialGroup()
        	.addGap(15)
        	.addGroup(beLayout.createParallelGroup(Alignment.CENTER)
        		.addComponent(titleLbl)
        		.addComponent(closeXLbl)));
        
        tabs.setTabPlacement(JTabbedPane.TOP);
        tabs.add("SEARCH", searchLayersPanel);
        tabs.add("BROWSE", browsePanel);        
        tabs.add("CUSTOM MAPS", customMapPanel);
        tabs.add("FAVORITES", favoritesPanel);
        
        if(Main.USER == null || Main.USER.trim().length() == 0) {
            tabs.setEnabledAt(CUSTOM_MAP_TAB, false);//turn off favs if not logged in
            tabs.setEnabledAt(FAVORITES_TAB, false);//turn off custom maps if not logged in
        }
        tabs.addChangeListener(new ChangeListener() {
            
            @Override
            public void stateChanged(ChangeEvent e) {
                if (e.getSource() instanceof JTabbedPane) {
                    JTabbedPane tp = (JTabbedPane) e.getSource();
                    if (tp.getSelectedIndex() == 3) {
                        refreshFavoritesPanel();
                    }                    
                }
            }
        });
        JButton cmManagerBtn = new JButton(new AbstractAction("CUSTOM MAP MANAGER") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				newAddLayerDialog.setVisible(false);
				CM_Manager mgr = CM_Manager.getInstance();
				mgr.setLocationRelativeTo(Main.mainFrame);
				mgr.setVisible(true);
				mgr.setSelectedTab(CM_Manager.TAB_UPLOAD);
			}
		});
        if (!Main.isUserLoggedIn()) {
        	cmManagerBtn.setEnabled(false);
        	cmManagerBtn.setToolTipText("Log in to access the custom map manager");
        } else {
        	cmManagerBtn.setToolTipText("Access the custom map manager");
        }
        JPanel mainPanel = new JPanel();
        GroupLayout mainPanelLayout = new GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setAutoCreateGaps(true);
        
        mainPanelLayout.setHorizontalGroup(mainPanelLayout.createParallelGroup(Alignment.LEADING)
        	.addComponent(cmManagerBtn)
        	.addGroup(mainPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
            	.addComponent(buttonExtension, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
            	.addComponent(tabs, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
            	.addGroup(mainPanelLayout.createSequentialGroup()
            		.addComponent(doneButton)
            		.addContainerGap()))
        );
        mainPanelLayout.setVerticalGroup(mainPanelLayout.createSequentialGroup()
            .addComponent(buttonExtension, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
            .addComponent(tabs, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
            .addGroup(mainPanelLayout.createParallelGroup(Alignment.BASELINE)
            	.addComponent(cmManagerBtn)
            	.addComponent(doneButton))
            .addContainerGap()
        );
        return mainPanel;
    }
    
    public void showFavoriteTab() {
    	if (newAddLayerDialog != null) {
    		if (newAddLayerDialog.isVisible() && newAddLayerDialog.isShowing()) {
    			tabs.setSelectedIndex(FAVORITES_TAB);
    		} else {
    			newAddLayerDialog.setVisible(true);
    			tabs.setSelectedIndex(FAVORITES_TAB);
    		}
    	} else {
    		createAddLayerDialog();
    		tabs.setSelectedIndex(FAVORITES_TAB);
    	}
    	LManager.getLManager().getMainPanel().resetAddLayerDialog();
    }
    
    /**
     * Close the dialog after add layer action is performed, if flag is true
     */
    public void finishSelection() {
    	if (closeOnSelection) {
    		closeAddLayerDialog();
    	}
    }
    private JPanel layoutBrowsePanel() {
        BrowseLayerPanel browsePanel = new BrowseLayerPanel();
        browsePanel.buildGUI();
        return browsePanel;
    }
    
    private int[] getCurrentTagStartEndIndexes() {
        int caret = searchTF.getCaretPosition();
        String searchText = getSearchText();
        int[] idxs = new int[2];
        int endInputIdx = -1;
        int currentTagIdx = -1;
        
        
        int start = 0;
        searchText = searchText.replace("custom map", "custom_map");
        String[] splitVals = searchText.split(" ",0);
        searchText = searchText.replace("custom_map", "custom map");
        for (String val : splitVals) {
            val = val.trim();
            val = val.replace("custom_map", "custom map");
            if (searchProvider.getSearchOptions().contains(val)) {
                start = searchText.indexOf(val,start);
                if (start > caret) {
                    endInputIdx = start;//mark the start of the next tag as the end of the search input
                    break;
                } else {
                    currentTagIdx = start; 
                }
                start += val.length();
            }
        }
        idxs[0] = currentTagIdx;
        idxs[1] = endInputIdx;
        return idxs;
    }
    private void searchLayers(String searchText) {
    	displayRows.clear();
		if (ENTER_KW.trim().equalsIgnoreCase(searchText.trim())) {
			displayRows.addAll(searchProvider.searchLayers(""));
		} else {
			displayRows.addAll(searchProvider.searchLayers(searchText));
		}
		resetSelectedRow();
        buildMainResultsPane(false);
    }

    private static final String ENTER_KW = " Enter Keywords";
    private static final int MIN_KEYSTROKES = 2;
    private ArrayList<SearchResultRow> displayRows = new ArrayList<SearchResultRow>();
    private void initSearchPanel(boolean searchAllLayers) {
        JLabel searchLbl = new JLabel();
        searchTF = new JTextField(45);
        searchTF.setText(ENTER_KW);

        searchLbl.setText("SEARCH LAYERS");
        searchLbl.setForeground(((ThemeButton)GUITheme.get("button")).getDefaultback());
        
        JLabel layerTooltip = new JLabel(tooltipIcon);
        layerTooltip.setOpaque(false);
        
        StringBuffer help = new StringBuffer();
        help.append("<html>");
        help.append("<div width=\"400px\"");
        help.append("<center><h3>Search hints:</h3></center>");
        help.append("<p>Click in the search input box. A list of optional tags will be displayed (more about tags below). "
        		+ "Begin typing to search for layers. Search parameters might be instrument names (i.e. themis), "
        		+ "keywords (i.e. elevation), or even names (i.e. Christensen). When no tags are used, all possible "
        		+ "layer information will be searched. Results may be returned because the search parameter shows "
        		+ "up in the description of the layer. However, JMARS attemps to order the result set in the most"
        		+ "useful order. Layers with names matching the search parameters are displayed at the top of the list."
        		+ "<br /><br />"
        		+ "Using a tag is a good way to limit the results of your search. Using a tag such as name: themis "
        		+ "will search only the names of layers. To use a tag, type the tag (including the colon)."
        		+ "You can also click on the tag when the tag suggestion list is displayed and type the search parameters"
        		+ "after the tag. <br /><br />"
        		+ "There are also some special tags that are displayed to make the search easier. For example, "
        		+ "clicking on instrument: themis will present a list of instruments. This is actually done by selecting "
        		+ "a category of instrument and a subcategory: themis. This allows you to do this search in one step."
        		+ "<br /><br />"
        		+ "When the focus is on the search input, you can use the up and down arrows to scroll through the results."
        		+ "When you have selected a layer you wish to add, you can simply hit the Enter key and the layer will load."
        		+ "Some layers have advanced options such as contour. You can use the right and left arrows as well to "
        		+ "navigate to the advanced button. If you have logged in, you can also use the right arrow key to navigate"
        		+ "to the favorite button. Hitting the enter key will add the layer to your favorites list."
        		+ "<br /><br />"
        		+ "Hitting the escape key will hide the dialogs. If the suggestion list is displayed, hitting escape"
        		+ "will hide the suggestion dialog, leaving the add layer dialog. If the suggestion list is not displayed,"
        		+ "hitting escape will hide the add layer dialog.");
        
        
        layerTooltip.setToolTipText(help.toString());
        
        JButton clearButton = new JButton();        
        clearButton.setIcon(new ImageIcon(ImageFactory
        		           .createImage(ImageCatalogItem.CLOSE
        		           .withDisplayColor(imgColor))));
        clearButton.setUI(new IconButtonUI());

        clearButton.addActionListener(new AbstractAction(){
            
            @Override
            public void actionPerformed(ActionEvent e) {
                clearButtonFlag = true;
                searchTF.setText(ENTER_KW);
                searchTF.grabFocus();
                searchTF.selectAll();
                searchTF.postActionEvent();//reset to home
            }
        });
        
//        JButton searchButton = new JButton();
//        Icon searchIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.SEARCH.withDisplayColor(imgColor)));
//        searchButton.setIcon(searchIcon);
//        searchButton.setUI(new IconButtonUI());
//        searchButton.addActionListener(new AbstractAction() {
//            
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                arrowSuggestionFlag = false;//make the click of the magnifying glass always be a full search
//                if (searchTF.getText().trim().equals(ENTER_KW.trim())) {
//                	searchTF.setText("");
//                }
//                searchTF.postActionEvent();                
//            }
//        });
        JPanel inputPanel = new JPanel();
        GroupLayout inputGL = new GroupLayout(inputPanel);
        inputPanel.setLayout(inputGL);
        inputGL.setHorizontalGroup(inputGL.createSequentialGroup()
        	.addComponent(searchTF)
        	.addComponent(clearButton));
        inputGL.setVerticalGroup(inputGL.createSequentialGroup()
        	.addGroup(inputGL.createParallelGroup(Alignment.CENTER)
        		.addComponent(searchTF)
        		.addComponent(clearButton)));

        inputPanel.setBackground(ThemeProvider.getInstance().getBackground().getMain());
        inputPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.WHITE));
        
        searchTF.setBackground(ThemeProvider.getInstance().getBackground().getMain());
        searchTF.selectAll();
        
      //layout the entire search panel
        GroupLayout searchLayout = new GroupLayout(searchPanel);
        searchPanel.setLayout(searchLayout);
        
        searchLayout.setHorizontalGroup(searchLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(searchLayout.createSequentialGroup()    
            	.addGap(6)
                .addComponent(searchLbl)
                .addGap(4)
                .addComponent(layerTooltip))
            .addGroup(searchLayout.createSequentialGroup()
                .addGap(6)
                .addComponent(inputPanel, 580,580,580)));
//                .addComponent(searchButton)));
        searchLayout.setVerticalGroup(searchLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(searchLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(searchLbl)
                .addComponent(layerTooltip))
            .addGap(4)
            .addGroup(searchLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(inputPanel, 40, 40, 40)));
//                .addComponent(searchButton)));
        
        searchTF.addFocusListener(new FocusListener() {
            
            @Override
            public void focusLost(FocusEvent e) {
                selectedSuggestion = -1;
                if (suggestionDialog != null) { 
                	suggestionDialog.setVisible(false);
                }
            }
            
            @Override
            public void focusGained(FocusEvent e) {
                if (clearButtonFlag) {
                	clearButtonFlag = false;
                	if (suggestionDialog != null) {
                		suggestionDialog.setVisible(false);
                	}
                } else {
	                if (suggestionDialog == null) {
	                    buildSuggestionDialog();
	                }
	                selectedSuggestion = -1;
	                placeSuggestionBox();
                }
                if (searchTF.getText().trim().equalsIgnoreCase(ENTER_KW.trim())) {
                	searchTF.selectAll();
                }
                if (selectedRow == null) {
					resetSelectedRow();
            	}
            }
        });
        searchTF.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
            	String text = getSearchText();
            	searchLayers(text);//where we do the search when user hits enter
                
            }
        });
        searchTF.addMouseListener(new MouseListener() {            
            @Override
			public void mouseReleased(MouseEvent e) {
			}
 
            
            @Override
			public void mousePressed(MouseEvent e) {
			}
            
            @Override
			public void mouseExited(MouseEvent e) {
			}
            
            @Override
			public void mouseEntered(MouseEvent e) {
            	if (searchTF.getText().trim().equalsIgnoreCase(ENTER_KW.trim())) {
                	searchTF.selectAll();
                }
            	if (selectedRow == null) {
            		resetSelectedRow();
            		highlightSelectedRow(new Point(1,1));
            	}
			}
   
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (searchTF.getText().trim().length() == 0 || searchTF.getText().trim().equalsIgnoreCase(ENTER_KW.trim())) {
                	searchTF.setText("");
                   if (suggestionDialog == null) {
                        buildSuggestionDialog();
                   }
                    selectedSuggestion = -1;
                    placeSuggestionBox();
                    doSuggestionSearch(); 
                    
                }
            }
        });
        searchTF.getDocument().addDocumentListener(new DocumentListener() {
            
            @Override
            public void removeUpdate(DocumentEvent e) {
            	if (suggestionDialog != null && suggestionDialog.isShowing()) {
        			suggestionDialog.setVisible(false);
        		}
            	if (!clearButtonFlag) {
	            	if (searchTF.getText().length() > MIN_KEYSTROKES) {
	            		searchTF.postActionEvent();
	            	}
            	}
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
            	if (suggestionDialog != null && suggestionDialog.isShowing()) {
        			suggestionDialog.setVisible(false);
        		}
            	if (!clearButtonFlag) {
	            	if (searchTF.getText().length() > MIN_KEYSTROKES) {
	            		searchTF.postActionEvent();
	            	}
            	}
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
            	if (suggestionDialog != null && suggestionDialog.isShowing()) {
        			suggestionDialog.setVisible(false);
        		}
            	if (!clearButtonFlag) {
	            	if (searchTF.getText().length() > MIN_KEYSTROKES) {
	            		searchTF.postActionEvent();
	            	}
            	}
            }
        });
        
        searchTF.addKeyListener(new KeyListener() {
            
            @Override
			public void keyTyped(KeyEvent e) {
			}
            
            @Override
			public void keyReleased(KeyEvent e) {
			}
            
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                switch(code) {
                    case KeyEvent.VK_DOWN :
                    	if (suggestionDialog != null && suggestionDialog.isShowing()) {
                    		selectSuggestion(KeyEvent.VK_DOWN);
                    	} else {
                    		if (displayRows != null && displayRows.size() > 0) {
                    			
                    			if (selectedRowInt + 1 >= displayRows.size()) {
                    				selectedRowInt = -1;
                    			}
                    			selectedRowInt++;
                    			highlightSelectedRowMain();
                    			adjustScrollPane(true);
                    		}
                    	}
                        break;
                    case KeyEvent.VK_UP :
                    	if (suggestionDialog != null && suggestionDialog.isShowing()) {
                    		selectSuggestion(KeyEvent.VK_UP);
                    	} else {
                    		if (displayRows != null && displayRows.size() > 0) {
                    			
                    			if (selectedRowInt - 1 <= -1) {
                    				selectedRowInt = displayRows.size();
                    			}
                    			selectedRowInt--;
                    			highlightSelectedRowMain();
                    			adjustScrollPane(false);
                    			
                    		}
                    	}
                        break;
                    case KeyEvent.VK_RIGHT :
                    	if (suggestionDialog == null || !suggestionDialog.isShowing()) {
                    		if(searchTF.getCaretPosition() >= searchTF.getText().length()) {
	                    		pushHighlightRight();
	                    		e.consume();
                    		} else if (lastCode == KeyEvent.VK_UP || lastCode == KeyEvent.VK_DOWN) {
                    			pushHighlightRight();
	                    		e.consume();
                    		}
                    	}
                    	break;
                    case KeyEvent.VK_LEFT :
                    	if (suggestionDialog == null || !suggestionDialog.isShowing()) {
                    		if(selectedRow.getSelectedAction() > SearchResultRow.ACTION_MAIN) {//something to the right is highlighted
                    			pushHighlightLeft();
                    			e.consume();
                    		}
                    	}
                    	break;	
                    case KeyEvent.VK_ESCAPE :
                        if (suggestionDialog != null && suggestionDialog.isShowing()) {
                            suggestionDialog.setVisible(false);
                        } else if (newAddLayerDialog.isShowing()) {
                        	closeAddLayerDialog();
                        }
                        break;
                    case KeyEvent.VK_ENTER :
                    	if (suggestionDialog != null && suggestionDialog.isShowing()) {
                    		//like a click on suggestion
                    	} else {
                    		if (selectedRowInt > -1) {
                    			selectedRow.executeSelectedAction();
                    			e.consume();
                    		}
                    	}
                    	break;
                }
                lastCode = code;
            }
        });
    }
    private int lastCode = -1;
    private void highlightSelectedRowMain() {
    	if (selectedRow != null) {
    		selectedRow.resetHighlight();
    		selectedRow = displayRows.get(selectedRowInt);
    		selectedRow.highlightMainButtonForSelection();
    	}
    }
    private void pushHighlightRight() {
    	if (selectedRow != null) {
    		selectedRow.pushHighlightRightForSelection();
    	}
    }
    private void pushHighlightLeft() {
    	if (selectedRow != null) {
    		selectedRow.pushHighlightLeftForSelection();
    	}
    }
    private void highlightSelectedRow(Point p) {
    	if (selectedRow != null) {
    		selectedRow.resetHighlight();
    	}
    	if (displayRows != null && displayRows.size() > 0) {
			selectedRow = displayRows.get(selectedRowInt);
			selectedRow.highlightForSelection(p);
			selectedRow.repaint();
    	}
    }
    private void resetSelectedRow() {
    	selectedRowInt = 0;
    	if (selectedRow != null) {
    		selectedRow.resetHighlight();
    	}
    	if (displayRows != null && displayRows.size() > 0) {
    		selectedRow = displayRows.get(selectedRowInt);
    		selectedRow.highlightMainButtonForSelection();
    	} else {
    		selectedRow = null;
    		selectedRowInt = -1;
    	}
    }
    private void adjustScrollPane(boolean down) {
    	if (selectedRowInt == 0) {
			resultsSP.getVerticalScrollBar().setValue(resultsSP.getVerticalScrollBar().getMinimum());
		} else if (selectedRowInt == displayRows.size()-1) { 
			resultsSP.getVerticalScrollBar().setValue(resultsSP.getVerticalScrollBar().getMaximum());
		} else {
			Rectangle visibleRect = selectedRow.getVisibleRect();
			if (down) {
				visibleRect.y += selectedRow.getHeight();
			} else {
				visibleRect.y -= selectedRow.getHeight();
			}
			selectedRow.scrollRectToVisible(visibleRect);
		}
		resultsSP.revalidate();
		resultsSP.repaint();
    }
    private void buildSuggestionDialog() {
        Color bgColor = ((ThemeTextField)GUITheme.get("textfield")).getBackground();

        suggestionDialog = new JDialog(newAddLayerDialog);
        suggestionDialog.setResizable(false);
        suggestionDialog.setFocusableWindowState(false);

        suggestionResultsPanel = new JPanel();
        JRootPane rootPane = suggestionDialog.getRootPane();
        rootPane.setWindowDecorationStyle(JRootPane.NONE);
        rootPane.setBorder(null);
        suggestionDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        suggestionDialog.setBackground(bgColor);
        suggestionResultsPanel.setBackground(bgColor);
        
        suggestionResultsSP = new JScrollPane(suggestionResultsPanel);
        suggestionResultsSP.getVerticalScrollBar().setUnitIncrement(40);
        
        suggestionResultsSP.setAlignmentX(JPanel.LEFT_ALIGNMENT);

        suggestionResultsSP.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        suggestionResultsSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        suggestionResultsPanel.setLayout(new BoxLayout(suggestionResultsPanel, BoxLayout.Y_AXIS));

        suggestionDialog.add(suggestionResultsSP);

        suggestionDialog.setVisible(false);
    }
    
    private void placeSuggestionBox() {
        try {
            Double x = searchTF.getLocationOnScreen().getX();
            Double y = searchTF.getLocationOnScreen().getY();
            
            int height = searchTF.getHeight();
            int aX = x.intValue();
            int bY = y.intValue() + height; 
            suggestionDialog.setLocation(aX, bY);
        } catch (Exception e) {           
        	closeAddLayerDialog();
        }
     }

    private void handleSuggestion(Component c) {
        JLabel label = (JLabel)(((JPanel)c).getComponent(0));
//        String searchText = getSearchText();
//        searchText = searchText.trim();
        String text = label.getText();

        if (text.indexOf("<") > -1) {
            text = text.substring(0,text.indexOf("<"));
            text = text.trim();
        }
        searchTF.setText(text);
    }
    
    private void doSuggestionSearch() {
        selectedSuggestion = -1;
        placeSuggestionBox();
        suggestionResultsPanel.removeAll();
        ArrayList<String> results = suggestionSearch();
//        Collections.sort(results);
        if (results != null && results.size() > 0) {
            for(String x : results) {
                JPanel p = new JPanel();
                JLabel lbl = new JLabel(x);
                boolean tagOnly = (x.endsWith("<search terms>") ? true : false);
                layoutOneResultRow(p, lbl, tagOnly, true);
                suggestionResultsPanel.add(p);
                suggestionResultsPanel.add(Box.createRigidArea(new Dimension(0,3)));
            }
        } else {
            if (!suppressSuggestionDialog) {
                for(String option : searchProvider.getSearchOptions()) {
                    JPanel p = new JPanel();
                    JLabel lbl = new JLabel(" "+option);
                    layoutOneResultRow(p, lbl, true, true);
                    suggestionResultsPanel.add(p);
                    suggestionResultsPanel.add(Box.createRigidArea(new Dimension(0,3)));
                }
            }
        }
        
        Dimension d = new Dimension(searchTF.getWidth(), Math.min(600,suggestionResultsPanel.getPreferredSize().height));
        suggestionDialog.setMaximumSize(d);
        suggestionDialog.setPreferredSize(d);
        suggestionDialog.setSize(d);
        
        suggestionResultsPanel.setMaximumSize(d);
        
        suggestionResultsSP.revalidate();
        suggestionDialog.pack();
        
        suggestionDialog.setVisible(true);
        suggestionDialog.repaint();
        

    }
    private void layoutOneResultRow(JPanel panel, JLabel text, boolean tagOnly, boolean addMouseListener) {
        Color bgColor = ((ThemeTextField)GUITheme.get("textfield")).getBackground();
        text.setBackground(bgColor);
        
        GroupLayout panelLayout = new GroupLayout(panel);
        panel.setLayout(panelLayout);

        int labelSize = searchTF.getWidth();
        panelLayout.setHorizontalGroup(panelLayout.createSequentialGroup()
            .addComponent(text, labelSize,labelSize,labelSize));
        panelLayout.setVerticalGroup(panelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
            .addComponent(text, 20,20,20));
        panel.setBackground(bgColor);
        if (addMouseListener) {//we don't want to add the listener for messages such as "no results to display"
	        panel.addMouseListener(new MouseListener() {
	            
	            @Override
				public void mouseReleased(MouseEvent e) {
				}
	            
	            @Override
	            public void mousePressed(MouseEvent e) {
	                
	            }
	            
	            @Override
	            public void mouseExited(MouseEvent e) {
	                e.getComponent().setCursor(Cursor.getDefaultCursor());
	                updateOffHoverBackground(e.getComponent());
	            }
	            
	            @Override
	            public void mouseEntered(MouseEvent e) {
	                e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	                updateHoverBackground(e.getComponent());
	            }
	            
	            @Override
	            public void mouseClicked(MouseEvent e) {
	                Component c = e.getComponent();
	                handleSuggestion(c);
	                if (!tagOnly) {
//	                    searchTF.postActionEvent();//removing this to prevent duplicate searches
	                    suggestionDialog.setVisible(false);
	//                    if (presentSuggestion(suggestionList)) {
	//                        doContentSearch();
	//                    } else {
	//                        searchTF.postActionEvent();
	//                    }
	                } else {
	                    doSuggestionSearch();
	                }
	            } 
	        });
        }
    }
    
    private void selectSuggestion(int arrow) {
        if (!suggestionResultsPanel.isVisible()) {
            return;
        }
        Component c = null;
        
        //if we have a valid selected suggestion from using arrows, reset the hover color
        if (selectedSuggestion > -1 && selectedSuggestion < suggestionResultsPanel.getComponentCount()) {
            c = suggestionResultsPanel.getComponent(selectedSuggestion);
            if (c instanceof JPanel) {
                updateOffHoverBackground(c);
            }
        }
        c = null;//reset this as it is used later
        do {//find the next panel that would be need to have the hover color...there are panels and boxlayout spacers in the component list
            if (arrow == KeyEvent.VK_DOWN) {
                if (selectedSuggestion == suggestionResultsPanel.getComponentCount()) {
                    selectedSuggestion = -1;
                }
                selectedSuggestion++;
            } else if(arrow == KeyEvent.VK_UP){
                if (selectedSuggestion == -1) {
                    selectedSuggestion = suggestionResultsPanel.getComponentCount();
                }
                
                selectedSuggestion--;
            }
            if (selectedSuggestion < 0 ) {//if they up arrowed too far, reset
                selectedSuggestion = -1;
                c = null;
                break;
            } else if (selectedSuggestion >= suggestionResultsPanel.getComponentCount()) {//if they down arrow too far, reset
                selectedSuggestion = suggestionResultsPanel.getComponentCount();
                c = null;
                break;
            }
            c = suggestionResultsPanel.getComponent(selectedSuggestion);//get the component at this index and check if it is a JPanel
        } while (!(c instanceof JPanel));
        if (c != null) {//if we have a valid JPanel, set the hover color
            updateHoverBackground(c);
        }
        
    }
    private String getSearchText() {
        String temp = searchTF.getText().trim().toLowerCase();
        temp = temp.replace(":",": ");
        return temp;
    }
    
    
    private String getCategory() {
        String cat = getSpecificTagValue("category:");
        if (cat == null) {
            cat = getSpecificTagValue("instrument:");
            if (cat != null) {
                cat = "instrument";
            } else {
                cat = getSpecificTagValue("imagery:");
                if (cat != null) {
                    cat = "imagery";
                } else {
                    cat = getSpecificTagValue("custom map:");
                    if (cat != null) {
                        cat = "custom";
                    }
                }
            } 
        }
        return cat;
    }
    
    
    private String getSubcategory() {
        String subcat = getSpecificTagValue("subcategory:");
        if (subcat == null) {
            subcat = getSpecificTagValue("instrument:");
            if (subcat == null) {
                subcat = getSpecificTagValue("imagery:");
            }
        }
        return subcat;
    }
    
    
    private String getSpecificTagValue(String tag) {
        String searchText = getSearchText();
        int endInputIdx = -1;
        int currentTagIdx = -1;
        
        int start = 0;
        boolean next = false;
        boolean found = false;
        String[] splitVals = searchText.split(" ",0);
        for (String val : splitVals) {
            val = val.trim();
            if (val.equalsIgnoreCase(tag.trim())) {
                currentTagIdx = searchText.indexOf(":",start)+1;
                next = true;
                found = true;
            } else if (next && searchProvider.getSearchOptions().contains(val)) {
                endInputIdx = searchText.indexOf(val,start);//we found the start of another tag, that is the end of the value for our tag
                break;
            }
            start += val.length();
            
        }
        if (found) {
            String result;
            if (endInputIdx == -1) {
                result = searchText.substring(currentTagIdx);
            } else {
                result = searchText.substring(currentTagIdx,endInputIdx);
            }
            return result;
        } else {
            return null;
        }
        
    }
    
    private void updateHoverBackground(Component p) {
        Color bgColor = ((ThemeComboBox)GUITheme.get("combobox")).getSelectedindropdownbackground();
        Color fgColor = ((ThemeComboBox)GUITheme.get("combobox")).getItemSelectionforeground();
        doColorUpdate(p, bgColor, fgColor);
    }
    
    
    private void updateOffHoverBackground(Component p) {
        Color bgColor = ((ThemeTextField)GUITheme.get("textfield")).getBackground();
        Color fgColor = ((ThemeTextField)GUITheme.get("textfield")).getForeground();
        doColorUpdate(p, bgColor, fgColor);
    }
    
    private void doColorUpdate(Component component, Color bg, Color fg) {
        JPanel p = null;
        if (!(component instanceof JPanel)) {
            p = (JPanel) component.getParent();
        } else {
            p = (JPanel) component;
        }
        Component[] comps = p.getComponents();
        
        for(Component comp : comps) {
            JLabel label = null;
            if (comp instanceof JLabel) {
                label = (JLabel) comp;
                label.setBackground(bg);
                label.setForeground(fg);
                label.repaint();
            }
        }
        p.setBackground(bg);
        p.setForeground(fg);
        p.repaint();
    }
    
    private AbstractAction doneButtonAct = new AbstractAction("CLOSE") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
        	closeAddLayerDialog();
        }
    };
    
    private ArrayList<String> suggestionSearch() {
        ArrayList<String> finalResults = new ArrayList<String>();
        ArrayList<String> results = new ArrayList<String>();
        ArrayList<String> nameResults = new ArrayList<String>();
//        Set<String> finalResults = new LinkedHashSet<String>();
//        HashMap<String,String> results = new HashMap<String,String>();
//        HashMap<String,String> nameResults = new HashMap<String,String>();
        
        String searchText = getSearchText();
        if (searchText.length() == 0) { 
            return null;
        }
        int currentTagIdx = -1;
        int endInputIdx = -1;
        
        int[] idxs = getCurrentTagStartEndIndexes();
        currentTagIdx = idxs[0];
        endInputIdx = idxs[1];
        
        String tag = "all";
        String value = null;
        boolean noEndTag = false;
        if (currentTagIdx > -1){ //if there is a tag in the search
            tag = searchText.substring(currentTagIdx,searchText.indexOf(":",currentTagIdx));
            int valueBeginIdx = searchText.indexOf(":",currentTagIdx) + 1;
            if (endInputIdx == -1) {
                noEndTag = true;
                value = searchText.substring(valueBeginIdx);
            } else {
                value = searchText.substring(valueBeginIdx,endInputIdx);
            }
        } else {
            noEndTag = true;
            value = searchText;
        }
        if (noEndTag && value.trim().length() > 0) {
            for(String opt : searchProvider.getSearchOptions()) {
                if (opt.trim().startsWith(value.trim()) && !opt.trim().equalsIgnoreCase(value.trim()+":")) {
                    nameResults.add(" "+opt);
                }
            }
        }
        value = value.toLowerCase().trim();
        if (value.length() == 0 && currentTagIdx < 1) {
            loadSuggestions(results, tag);
        } else {
            String[] split = value.split(" ", 0);
            String catValue = null;
            String subcatValue = null;
            String topicValue = null;
            catValue = getCategory();
            subcatValue = getSubcategory();
            topicValue = getSpecificTagValue("topic:");
            
            if (catValue != null) {
                catValue = catValue.trim();
            }
            if (subcatValue != null) {
                subcatValue = subcatValue.trim();
            }
            if (topicValue != null) {
                topicValue = topicValue.trim();
            }
            
            switch(tag) {
                case "category":
                    results.addAll(searchProvider.getPartialSuggestionCat(catValue));
                    break;
                case "subcategory":
                    results.addAll(searchProvider.getPartialSuggestionSubcat(catValue,subcatValue));
                    break;
                case "topic": 
                    results.addAll(searchProvider.getPartialSuggestionTopic(catValue,subcatValue,topicValue));
                    break;
                case "name":
                    if ("3d".equalsIgnoreCase(value) || value.length() > 2) {
                        if (catValue != null || subcatValue != null || topicValue != null) {
                            results.addAll(searchProvider.getSuggestionWithHierarchy(catValue,subcatValue,topicValue, split, SearchProvider.TAG_NAME));
                        } else {
                            results.addAll(searchProvider.getPartialSuggestion(split,SearchProvider.TAG_NAME));
                        }
                    }
                    break;
                case "description":
                case "desc":
                    if (value.length() > 2) {
                        if (catValue != null || subcatValue != null || topicValue != null) {
                            results.addAll(searchProvider.getSuggestionWithHierarchy(catValue,subcatValue,topicValue, split, SearchProvider.TAG_DESC));
                        } else {
                            results.addAll(searchProvider.getPartialSuggestion(split, SearchProvider.TAG_DESC));
                        }
                    }
                    break;
                case "citation":
                    if (value.length() > 2) {
                        if (catValue != null || subcatValue != null || topicValue != null) {
                            results.addAll(searchProvider.getSuggestionWithHierarchy(catValue,subcatValue,topicValue, split, SearchProvider.TAG_CITATION));
                        } else {
                            results.addAll(searchProvider.getPartialSuggestion(split,SearchProvider.TAG_CITATION));
                        }
                    }
                    break;
                case "links":
                    if (value.length() > 2) {
                        if (catValue != null || subcatValue != null || topicValue != null) {
                            results.addAll(searchProvider.getSuggestionWithHierarchy(catValue,subcatValue,topicValue, split, SearchProvider.TAG_LINKS));
                        } else {
                            results.addAll(searchProvider.getPartialSuggestion(split,SearchProvider.TAG_LINKS));
                        }
                    } 
                    break;   
                case "imagery":
                    results.addAll(searchProvider.getPartialSuggestionCat("Imagery"));
                    break;
                case "instrument":
                    results.addAll(searchProvider.getPartialSuggestionCat("Instrument"));
                    break;
                case "custom map":
                    results.addAll(searchProvider.getPartialSuggestionCustom(value, false));
                    break;
                case "favorite":
                    results.addAll(searchProvider.getPartialSuggestionFavorite(value));
                    break;
                case "all":
                    if ("3d".equalsIgnoreCase(value) || value.length() > 2) {
                        results.addAll(searchProvider.getPartialSuggestionAll(split));   
                    } 
                    break;
            }
        }
        finalResults.addAll(nameResults);
        finalResults.addAll(results);
        return finalResults;        
    }
    
    
    private void loadSuggestions(ArrayList<String> results, String tag) {
        switch(tag) {
            case "name":
                results.addAll(searchProvider.getSuggestionHome());
                break;
            case "category":
                results.addAll(searchProvider.getSuggestionCategory());
                break;
            case "subcategory":
                results.addAll(searchProvider.getSuggestionSubcategory());
                break; 
            case "topic":
                results.addAll(searchProvider.getSuggestionTopic());
                break;
            case "description":
            case "desc":
            case "links":
            case "citation":
                results.add(" "+tag+": <search terms>");
                results.add(" "+tag+": Christensen");
//                results.addAll(searchProvider.getSuggestionHome());
                break; 
            case "custom map":
                results.addAll(searchProvider.getPartialSuggestionCustom(null, true));
                break;
            case "favorite":
                results.addAll(searchProvider.getSuggestionFavorite());
                break;
            case "instrument":
                results.addAll(searchProvider.getSuggestionSubcatsAndTopicsForCat("Instrument"));
                break;
            case "imagery":
                results.addAll(searchProvider.getSuggestionSubcatsAndTopicsForCat("Imagery"));
                break;
            default : 
                results.addAll(searchProvider.getSuggestionHome());
                break;
        }
    }
    
    private boolean checkValue(String key, String layerVal, String check, String[] split, ArrayList<String> results, String tag, boolean exact) {
        boolean containsAll = true;
        boolean found = false;
        for (String one : split) {
            if (check.contains(one)) {
                found = true;
            } else {
                containsAll = false;
                break;
            }
        }
        if (containsAll && found) {
            String value = "";
            if (!exact){
                value = " name: "+layerVal+" <"+tag+" match>"; 
            } else {
                value = " "+tag + ": "+layerVal;
            }
            if (key == null) {
                key = value;
            }
            results.add(value);
            return true;
        }
        return false;
    }
}
