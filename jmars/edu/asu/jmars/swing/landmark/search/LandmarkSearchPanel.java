package edu.asu.jmars.swing.landmark.search;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Vector;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.GroupLayout.Alignment;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.RowFilter;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView.MarsFeature;
import edu.asu.jmars.places.Place;
import edu.asu.jmars.places.XmlPlaceStore;
import edu.asu.jmars.places.XmlPlaceStore.PlaceChangedObservable;
import edu.asu.jmars.swing.landmark.search.swing.LandmarkSearchTable;
import edu.asu.jmars.swing.landmark.search.swing.LandmarkSearchTableModel;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeButton;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import io.vincenzopalazzo.placeholder.JTextFieldPlaceholder;
import mdlaf.animation.MaterialUIMovement;
import mdlaf.components.button.MaterialButtonUI;
import mdlaf.components.togglebutton.MaterialToggleButtonUI;


public class LandmarkSearchPanel {	
	private static final String ENTER_KW = "";	
	private static final int PREF_WIDTH = 350;
	private static boolean isIncludeSearchIcon = true;
	private static NomenclatureLView nomenclatureView = null;
	private static List<MarsFeature> landmarks = new ArrayList<>();
	private static List<MarsFeature> bookmarks = new ArrayList<>();
	private static final Color SEARCH_PANEL_COLOR = ThemeProvider.getInstance().getBackground().getAlternateContrastBright();
	private static JTextField landmarkSearchInput = new JTextField();
	private static JButton searchButton = new JButton("GO " + Character.toString('\u02C3'));
	private static JToggleButton landOrBook = new JToggleButton();
	private static JLabel searchLbl = new JLabel();
	private static JLabel clearButton2 = new JLabel();
	private static JPanel landmarkSearchInputPanel = null;
	private static JDialog wrapperSearchInputDialog = null;
	private static JTextFieldPlaceholder alignRelativeTo = null;
	private static LandmarkSearchTable jTable = null;
	private static LandmarkSearchTableModel model = new LandmarkSearchTableModel();
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static Icon clearicon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CLOSE.withDisplayColor(imgColor)));
	private static Icon closewindow = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CLEAR.withDisplayColor(imgColor)));
	private static Icon landmarkIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.NOMENCLATURE_LAYER_IMG.withDisplayColor(imgColor)));
	private static Icon bookmarkIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.BOOKMARK.withDisplayColor(imgColor)));
	private static JLabel closeButton = new JLabel();
	private static final String SEARCH_LANDMARKS_TIP = "Click to search bookmarks";
	private static final String SEARCH_BOOKMARKS_TIP = "Click to search landmarks";
	private static JPanel panelToHoldSearchItemsScrollPane = null;
	private static JPanel everythingPanel = null;
	private static JScrollPane paneWithSearchItems = null;
	private static JPanel searchPanel = null;
	private static boolean allowFilter = true;
	private static MyOwnFocusTraversalPolicy newPolicy;
	private static XmlPlaceStore placeStore = new XmlPlaceStore();
	
	
	public static void showHideSearchInput(JTextFieldPlaceholder relativeTo, ActionEvent searchToggle) {
		alignRelativeTo = relativeTo;
		if (nomenclatureView == null) {
			initLandmarkSearchService();
			initBookmarkSearchService();
		}
		if (wrapperSearchInputDialog == null) {
			wrapperSearchInputDialog = new JDialog(Main.mainFrame);
			JRootPane rootPane = wrapperSearchInputDialog.getRootPane();
			rootPane.setWindowDecorationStyle(JRootPane.NONE);
			rootPane.setBorder(null);
			wrapperSearchInputDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
			Dimension dim = relativeTo.getSize();
			wrapperSearchInputDialog.setBackground(ThemeProvider.getInstance().getBackground().getContrast());			
			boolean includeSearchIcon = false;
			landmarkSearchInputPanel = createSearchPanel(dim, includeSearchIcon);
			configurelandmarkSearchInputEvents();
			wrapperSearchInputDialog.add(landmarkSearchInputPanel);	
			wrapperSearchInputDialog.setFocusCycleRoot(true);
			Vector<Component> order = new Vector<Component>(7);
	        order.add(landmarkSearchInput);
	        order.add(clearButton2);
	        order.add(closeButton);  
	        newPolicy = new MyOwnFocusTraversalPolicy(order);
	        wrapperSearchInputDialog.setFocusTraversalPolicy(newPolicy);
		}
		if (!(searchToggle.getSource() instanceof AbstractButton)) return;
		AbstractButton searchBtn = (AbstractButton) searchToggle.getSource();
		if (searchBtn.isSelected()) {
			resetSearchLandmarkDialogLocationOnScreen();
			wrapperSearchInputDialog.setVisible(true);				
		} else {			
			wrapperSearchInputDialog.setVisible(false);	
			searchBtn.setSelected(false);
		}		
	}

	
	public static void setRelativeToOnScreenComponent(JTextFieldPlaceholder locInputFieldControl) {
		alignRelativeTo = locInputFieldControl;
	}

	private static JPanel createSearchPanel(Dimension dim, boolean iconFlag) {
		// search panel
		isIncludeSearchIcon = iconFlag;		
		landmarkSearchInput.setText(ENTER_KW);
		landmarkSearchInput.setBackground(SEARCH_PANEL_COLOR);	
		searchLbl.setText("SEARCH  LANDMARKS");	
				
		closeButton.setIcon(closewindow);
		closeButton.setBorder(BorderFactory.createEmptyBorder(-5, 1, 1, 5));
		closeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				closeSearchDialog();
			}
		});	
		closeButton.addKeyListener(new ClearButtonKeyListener());			
				
		clearButton2.setIcon(clearicon);
		clearButton2.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
		clearButton2.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				allowFilter = true;
				landmarkSearchInput.setText(ENTER_KW);
				landmarkSearchInput.grabFocus();
			}
		});
		clearButton2.addKeyListener(new ClearButtonKeyListener());

		searchButton.setUI(new SearchButtonUI());		
		searchButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
			}
		});
		searchButton.setVisible(isIncludeSearchIcon);
		JPanel inputPanel = new JPanel();

	    inputPanel = new JPanel();
        GroupLayout inputGL = new GroupLayout(inputPanel);
        inputPanel.setLayout(inputGL);
        inputGL.setHorizontalGroup(inputGL.createSequentialGroup()
        	.addComponent(landmarkSearchInput)
        	.addComponent(clearButton2));
        inputGL.setVerticalGroup(inputGL.createSequentialGroup()
        	.addGroup(inputGL.createParallelGroup(Alignment.CENTER)
        		.addComponent(landmarkSearchInput)
        		.addComponent(clearButton2)));

        inputPanel.setBackground(SEARCH_PANEL_COLOR);
        inputPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, imgColor));		
	
		landmarkSearchInput.selectAll();
		// layout the entire search panel
		JPanel searchHeaderPanel = new JPanel(new BorderLayout());
		JPanel togglePanel = new JPanel(new BorderLayout());
		togglePanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		togglePanel.setBackground(SEARCH_PANEL_COLOR);
		landOrBook.setUI(new MyToggleButtonUI());
		landOrBook.addActionListener(e -> landOrBookAction(e));
		landOrBook.setSelected(true);
		landOrBook.setToolTipText(SEARCH_LANDMARKS_TIP);
		togglePanel.add(landOrBook, BorderLayout.WEST);
		searchHeaderPanel.add(togglePanel, BorderLayout.NORTH);
		JPanel labelspanel = new JPanel(new BorderLayout());
		labelspanel.setBackground(SEARCH_PANEL_COLOR);
		labelspanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		labelspanel.add(searchLbl, BorderLayout.NORTH);
		searchHeaderPanel.add(labelspanel, BorderLayout.WEST);
		searchHeaderPanel.add(closeButton, BorderLayout.EAST);
		searchHeaderPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		searchHeaderPanel.setBackground(SEARCH_PANEL_COLOR);
		
		searchPanel = new JPanel();
		GroupLayout searchLayout = new GroupLayout(searchPanel);
		searchPanel.setLayout(searchLayout);
		searchPanel.setBackground(SEARCH_PANEL_COLOR);
	
        searchLayout.setHorizontalGroup(searchLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(searchLayout.createSequentialGroup()    
                .addComponent(searchHeaderPanel))             
            .addGroup(searchLayout.createSequentialGroup()
                .addComponent(inputPanel).addGap(6)));
        searchLayout.setVerticalGroup(searchLayout.createSequentialGroup()
            .addGroup(searchLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(searchHeaderPanel))         
            .addGroup(searchLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(inputPanel)
                ));
  
		JPanel textfieldPanel = new JPanel(new BorderLayout());
		textfieldPanel.setBackground(SEARCH_PANEL_COLOR);
		int w = (dim.width > 0  ? dim.width : PREF_WIDTH);
		int h = 110;
		textfieldPanel.setPreferredSize(new Dimension(w, h));

		textfieldPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(1, 5, 1, 0), 
				BorderFactory.createMatteBorder(0, 0, 0, 0, SEARCH_PANEL_COLOR)));
		textfieldPanel.add(searchPanel);
		
		jTable = new LandmarkSearchTable(model, landmarkSearchInput);	
		TableRowSorter<LandmarkSearchTableModel> sorter = new TableRowSorter<>(model);
		jTable.setRowSorter(sorter);
		
		panelToHoldSearchItemsScrollPane = new JPanel(new BorderLayout());		
		everythingPanel = new JPanel(new BorderLayout());
		everythingPanel.add(textfieldPanel, BorderLayout.NORTH);				
		paneWithSearchItems = new JScrollPane(jTable);				
		panelToHoldSearchItemsScrollPane.add(paneWithSearchItems);
		everythingPanel.add(panelToHoldSearchItemsScrollPane, BorderLayout.CENTER);
		panelToHoldSearchItemsScrollPane.setVisible(false);		
		
	    return everythingPanel;
	}	

	private static void landOrBookAction(ActionEvent e) {
		AbstractButton abstractButton = (AbstractButton) e.getSource();
		boolean selected = abstractButton.getModel().isSelected();
		if (selected) {
			abstractButton.setToolTipText(SEARCH_LANDMARKS_TIP);
			SearchMode.LANDMARKS.execute();
		} else {
			abstractButton.setToolTipText(SEARCH_BOOKMARKS_TIP);
			SearchMode.BOOKMARKS.execute();
		}
	}

	private static void configurelandmarkSearchInputEvents() {
		landmarkSearchInput.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				jTable.hideTooltip();
				if ((ENTER_KW).equals(landmarkSearchInput.getText().trim())) {
					allowFilter = true;
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if ((ENTER_KW).equals(landmarkSearchInput.getText().trim())) {
					allowFilter = true;					
				}
			}
		});
		
		landmarkSearchInput.addKeyListener(new SearchInputKeyListener());
		
		landmarkSearchInput.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				allowFilter = true;
			}
		});

		landmarkSearchInput.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {	
				if (allowFilter) {
					filter();	
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				if (allowFilter) {
					filter();
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				if (allowFilter) {
					filter();
				}
			}

			private void filter() {
				MyTableRowFilter.searchText = landmarkSearchInput.getText().trim();
				((TableRowSorter) jTable.getRowSorter()).setRowFilter(new MyTableRowFilter());
				if (jTable.getModel().getRowCount() > 0) {	
					if ((jTable.getRowCount() - 1) >= 0) {
					    jTable.addRowSelectionInterval(0, 0); //always highlight first row
					    jTable.scrollToMakeRowVisible(0);
					}
					panelToHoldSearchItemsScrollPane.setVisible(true);
					wrapperSearchInputDialog.pack();
				} else {
					panelToHoldSearchItemsScrollPane.setVisible(false);
					wrapperSearchInputDialog.pack();
				}
			}
		});
	}

	private static void initLandmarkSearchService() {
		for (LView view : LManager.getLManager().getViewList()) {
			if (view instanceof NomenclatureLView && view.isOverlay()) {
				nomenclatureView = (NomenclatureLView) view;				
				break;
			}
		}
		landmarks.clear();
		landmarks = LandmarkDataAccess.getLandmarks();			
	    model.addData(jTable, landmarks, SearchMode.LANDMARKS);	
	    allowFilter = true;
	    landmarkSearchInput.setText(ENTER_KW);	    	    
	}
	
	private static void initBookmarkSearchService() {
		bookmarks.clear();
		bookmarks.addAll(BookmarksDataAccess.getBookmarks(nomenclatureView, placeStore));
	}	

	public static void resetSearchLandmarkDialogLocationOnScreen() {
		if (alignRelativeTo != null) {
			try {
				Double x = alignRelativeTo.getLocationOnScreen().getX();
				Double y = alignRelativeTo.getLocationOnScreen().getY();
				wrapperSearchInputDialog.setLocation(x.intValue(), y.intValue() + alignRelativeTo.getHeight());
				wrapperSearchInputDialog.pack();				
			} catch (Exception e) {
				resetSearch();
			}
		}
	}

	private static void hideSearchDialog() {
		if (wrapperSearchInputDialog != null && wrapperSearchInputDialog.isVisible()) {
			wrapperSearchInputDialog.setVisible(false);
		}
		if (alignRelativeTo != null) {
			alignRelativeTo.getIconContainer().setSelected(false);
		}
		searchButton.setSelected(false);
	}
	
	public static void resetSearch() {
		if (wrapperSearchInputDialog != null && wrapperSearchInputDialog.isVisible()) {
			wrapperSearchInputDialog.setVisible(false);
		}	
		if (alignRelativeTo != null) {
			alignRelativeTo.getIconContainer().setSelected(false);
		}
		searchButton.setSelected(false);
		landOrBook.setSelected(true);
		searchLbl.setText(SearchMode.LANDMARKS.getSearchMode());
		landmarkSearchInput.setEnabled(true);
		landmarkSearchInput.setToolTipText(null);
		landmarkSearchInput.setText(ENTER_KW);
		if (nomenclatureView != null) {
			nomenclatureView.hideCallout();
			nomenclatureView = null;
		}
	}
	
	public static void searchBookmarks() {
		if (landOrBook != null) {
			landOrBook.setSelected(true); //toggle to true = landmarks, so that doClick will set it to false = bookmarks
			landOrBook.doClick();
		}
	}	
	
	public static void searchLandmarks() {
		if (landOrBook != null) {
			landOrBook.setSelected(false); //toggle to false = bookmarks, so that doClick will set it to true = landmarks
			landOrBook.doClick();
		}
	}	
	
	private static class MyTableRowFilter extends RowFilter<Object, Object> {
		private static String searchText;

		@Override
		public boolean include(Entry entry) {
			if (searchText.trim().isEmpty()) {
				return true;
			}
			int row = (int) entry.getIdentifier();
			MarsFeature mf = ((LandmarkSearchTableModel) entry.getModel()).getLandmarkDataAt(row);
			searchText = searchText.replaceAll("\\s+", " ");
			return mf.everything.toLowerCase().contains(searchText.toLowerCase());
		}
	}
	
	 public static void GOTO(String input) {
		if (nomenclatureView == null) {
			initLandmarkSearchService();
			initBookmarkSearchService();
		}	
		if (input.trim().isEmpty()) {
			return;
		}
		if (landOrBook.isSelected()) {
			nomenclatureView.GoToLandmark(input.trim());
		} else { 
			Place place = BookmarksDataAccess.of(input.trim());
			if (place != null) {
				place.gotoPlace(Main.places.reproject.getValue(), Main.places.rescale.getValue(), Main.places.reproject.getValue());
				MarsFeature mf = BookmarksDataAccess.of(place);
				if (mf != null) {
					nomenclatureView.showCallout(mf);
				}
			}
		}
	}
	 
	public static void closeSearchDialog() {
		if (wrapperSearchInputDialog != null) {
			wrapperSearchInputDialog.setVisible(false);
			if (alignRelativeTo != null) {
				alignRelativeTo.getIconContainer().setSelected(false);
			}
		}
	}
	
	private static class SearchInputKeyListener extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {	
			allowFilter = true;
			if (e.getKeyCode() == KeyEvent.VK_DOWN) {
				jTable.setNextRowDown();				
			} else if (e.getKeyCode() == KeyEvent.VK_UP) {
				jTable.setNextRowUp();
			} else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
				jTable.setPageDown();
			} else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP) {
				jTable.setPageUp();
			} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				jTable.hideTooltip();
				int rowindex = jTable.getSelectedRow();
				String landmark = jTable.getLandmarkForSelectedRow(jTable, rowindex);
				GOTO(landmark);			
			} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				hideSearchDialog();
			}
		}
	}	
	
	private static class ClearButtonKeyListener extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				allowFilter = true;
				landmarkSearchInput.setText(ENTER_KW);
				landmarkSearchInput.grabFocus();
			}
		}
	}
	 
	private static class SearchButtonUI extends MaterialButtonUI {
		
		@Override
		public void installUI(JComponent c) {
			mouseHoverEnabled = false;
			super.installUI(c);
			JButton b = (JButton) button;
			button.setBackground(SEARCH_PANEL_COLOR);
			button.setBackground(SEARCH_PANEL_COLOR);
			super.borderEnabled = false;
			super.arch = 0;
			c.setFocusable(true);
			if (mouseHoverEnabled != null) {
				b = (JButton) button;
				if (!b.isDefaultButton()) {
					button.addMouseListener(MaterialUIMovement.getMovement(button,
							((ThemeButton) GUITheme.get("button")).getAltOnhover()));
				}
			}
		}

		@Override
		protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect,
				Rectangle iconRect) {
			super.paintFocus(g, b, viewRect, textRect, iconRect);
		}
	}
	
	private static class MyOwnFocusTraversalPolicy extends FocusTraversalPolicy {
		Vector<Component> order;

		public MyOwnFocusTraversalPolicy(Vector<Component> order) {
			this.order = new Vector<Component>(order.size());
			this.order.addAll(order);
		}

		@Override
		public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
			int idx = (order.indexOf(aComponent) + 1) % order.size();
			return order.get(idx);
		}

		@Override
		public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
			int idx = order.indexOf(aComponent) - 1;
			if (idx < 0) {
				idx = order.size() - 1;
			}
			return order.get(idx);
		}

		@Override
		public Component getDefaultComponent(Container focusCycleRoot) {
			return order.get(0);
		}

		@Override
		public Component getLastComponent(Container focusCycleRoot) {
			return order.lastElement();
		}
		@Override
		public Component getFirstComponent(Container focusCycleRoot) {
			return order.get(0);
		}
	}
	
	private static class MyToggleButtonUI extends MaterialToggleButtonUI {

		@Override
		public void installUI(JComponent c) {			
			super.installUI(c);	
			JToggleButton b = (JToggleButton) toggleButton;
			b.setBackground(SEARCH_PANEL_COLOR);
			b.setSelectedIcon(landmarkIcon);
			b.setIcon(bookmarkIcon);
		}
	}
	
	public enum SearchMode implements BookmarksChangedObserver {

		LANDMARKS("landmarks") {
			@Override
			public String getSearchMode() {
				return SEARCH_LANDMARKS;
			}

			@Override
			public void execute() {
				searchLbl.setText(SEARCH_LANDMARKS);
				landmarkSearchInput.setEnabled(true);
				landmarkSearchInput.setToolTipText(null);
				model.addData(jTable, landmarks, SearchMode.LANDMARKS);	
				allowFilter = true;
			}

			@Override
			public void update(Observable o, Object arg) {
			}
		},

		BOOKMARKS ("bookmarks") {
			@Override
			public String getSearchMode() {
				return SEARCH_BOOKMARKS;
			}

			@Override
			public void execute() {
				searchLbl.setText(SEARCH_BOOKMARKS);
				initBookmarkSearchService();
				if (bookmarks.isEmpty()) {
					landmarkSearchInput.setEnabled(false);
					landmarkSearchInput.setToolTipText("you have no bookmarks for " + Main.getBody());
				} else {
					landmarkSearchInput.setEnabled(true);
					landmarkSearchInput.setToolTipText(null);
				}
				model.addData(jTable, bookmarks, SearchMode.BOOKMARKS);	
				allowFilter = true;
			}

			@Override
			public void update(Observable o, Object arg) {
				if (arg == null)
					return;
				if (o instanceof PlaceChangedObservable) {
					if (!landOrBook.isSelected()) { //if current search is BOOKMARKS
					    execute();
					}
				}
			}
		};	

		SearchMode(String searchmode) {
			this.searchmode = searchmode;
			placeStore.addObserver(this);
		}
		
		public String asString() {
			return this.searchmode;
		}

		private static final String SEARCH_LANDMARKS = "SEARCH LANDMARKS";
		private static final String SEARCH_BOOKMARKS = "SEARCH BOOKMARKS";
		private String searchmode;
		public abstract String getSearchMode();
		public abstract void execute();
		
	}

}

