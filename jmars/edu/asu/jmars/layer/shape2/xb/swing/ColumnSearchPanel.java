package edu.asu.jmars.layer.shape2.xb.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import edu.asu.jmars.layer.shape2.xb.XB;
import edu.asu.jmars.layer.shape2.xb.data.service.Data;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeText;
import io.vincenzopalazzo.placeholder.JTextFieldPlaceholder;


public class ColumnSearchPanel {
	static final String ENTER_KW = "Search columns by name";
	private static final URL myurl = edu.asu.jmars.Main.class.getResource("/resources/magnifier.png");
	private static final String searchtooltip = "<html><div>Type here to search columns by name<br>"
            									+ "or press <img src='" + myurl + "'></img>&nbsp;"
            									+ "to get list of all columns in the active data source.</div></html>";
  	static final String EMPTY = "";
	static final int PREF_WIDTH = 270;
	private static final Color SEARCH_PANEL_COLOR = ThemeProvider.getInstance().getBackground().getAlternateContrastBright();
	private static JToggleButton searchButton;
	private static JPanel columnSearchInputPanel = null;
	private static JDialog wrapperSearchInputDialog = null;
	private static JTextFieldPlaceholder alignRelativeTo = null;
	private static ColumnSearchTable jTable = null;
	private static ColumnSearchTableModel model = new ColumnSearchTableModel();
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static Icon closewindow = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CLEAR.withDisplayColor(imgColor)));
	private static JLabel closeButton = new JLabel();
	private static JPanel panelToHoldSearchItemsScrollPane = null;
	private static JPanel everythingPanel = null;
	private static JScrollPane paneWithSearchItems = null;
	private static final Color TIPS_COLOR = (((ThemeText)GUITheme.get("text")).getTextcolor());
	static boolean allowFilter = true;
	
	static {
		columnSearchInputPanel = createSearchPanel();
		configureColumnSearchInputEvents();		
	}

	public static void showHideSearchInput(JTextFieldPlaceholder relativeTo, JToggleButton jToggleButton) {
		alignRelativeTo = relativeTo;
		searchButton = jToggleButton;
		model.addData();
		if (wrapperSearchInputDialog == null) {
			createWrapperDialog(relativeTo, null);
		}
		if (searchButton.isSelected()) {
			showSearchDialog();
		} else {
			wrapperSearchInputDialog.setVisible(false);
			searchButton.setSelected(false);
		}
	}
	
	public static void closeSearchDialog() {
		hideSearchDialog();
	}
	
	public static void disposeSearchDialog() {  //for ColumnEditor
		hideSearchDialog();
		wrapperSearchInputDialog = null;
	}
		
	
  static void createWrapperDialog(JTextFieldPlaceholder relativeTo, Dialog host) {
	  	Dialog owner = (host == null) ? XB.INSTANCE.XBDialog : host;
		wrapperSearchInputDialog = new JDialog(owner, false);
		wrapperSearchInputDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);		
		addEscapeAction(wrapperSearchInputDialog);
		JRootPane rootPane = wrapperSearchInputDialog.getRootPane();
		rootPane.setWindowDecorationStyle(JRootPane.NONE);
		rootPane.setBorder(null);
		wrapperSearchInputDialog.setBackground(ThemeProvider.getInstance().getBackground().getContrast());
		alignRelativeTo = relativeTo;
		searchButton = relativeTo.getIconContainer();
		wrapperSearchInputDialog.add(columnSearchInputPanel);
	}

	private static void addEscapeAction(JDialog dlg) {
			KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
			dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "ESCAPE");
			dlg.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					hideSearchDialog();
				}
			});
		}
		
	public static void setRelativeToOnScreenComponent(JTextFieldPlaceholder locInputFieldControl) {
		alignRelativeTo = locInputFieldControl;
	}

	private static JPanel createSearchPanel() {		
		XBMainPanel.columnSearchInput.setToolTipText(searchtooltip);
		XBMainPanel.columnSearchInput.setBackground(SEARCH_PANEL_COLOR);
		closeButton.setIcon(closewindow);
		closeButton.setBorder(BorderFactory.createEmptyBorder(5, 1, 1, 5));
		closeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				closeSearchDialog();
			}
		});
		closeButton.addKeyListener(new ClearButtonKeyListener());

		JPanel searchHeaderPanel = new JPanel(new BorderLayout());
		searchHeaderPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 1));
		searchHeaderPanel.setBackground(SEARCH_PANEL_COLOR);
		
		JLabel draganddropLabel = new JLabel();
		draganddropLabel.setFont(new Font(ThemeFont.getFontFamily(), Font.PLAIN, 12));
		draganddropLabel.setHorizontalAlignment(SwingConstants.LEFT);
		draganddropLabel.setHorizontalTextPosition(SwingConstants.LEFT);
		draganddropLabel.setForeground(TIPS_COLOR);		
		draganddropLabel.setText(UserPromptFormula.DRAG_N_DROP_TIP.asString());
		JPanel labelTextPanel = new JPanel(new BorderLayout());	
		labelTextPanel.setBackground(SEARCH_PANEL_COLOR);
		labelTextPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 5));
	    labelTextPanel.add(draganddropLabel, BorderLayout.WEST);

        JPanel labelIconPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        labelIconPanel.setBackground(SEARCH_PANEL_COLOR);
        labelIconPanel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
        labelIconPanel.add(closeButton);
	
        searchHeaderPanel.add(labelTextPanel, BorderLayout.WEST);
        searchHeaderPanel.add(labelIconPanel, BorderLayout.EAST);
		
		jTable = new ColumnSearchTable(model);
		TableRowSorter<ColumnSearchTableModel> sorter = new TableRowSorter<>(model);
		jTable.setRowSorter(sorter);

		panelToHoldSearchItemsScrollPane = new JPanel(new BorderLayout());
		everythingPanel = new JPanel(new BorderLayout());
		int h = 200;
		everythingPanel.setPreferredSize(new Dimension(PREF_WIDTH, h));

		paneWithSearchItems = new JScrollPane(jTable);
		panelToHoldSearchItemsScrollPane.add(paneWithSearchItems);
		everythingPanel.add(searchHeaderPanel, BorderLayout.NORTH);
		everythingPanel.add(panelToHoldSearchItemsScrollPane, BorderLayout.CENTER);
		panelToHoldSearchItemsScrollPane.setVisible(true);
		
        DragGestureRecognizer dgr = DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                jTable,
                DnDConstants.ACTION_COPY_OR_MOVE,
                new DragGestureHandler(jTable));


        DropTarget dt = new DropTarget(
                XBMainPanel.textExpr,
                DnDConstants.ACTION_COPY_OR_MOVE,
                new DropTargetHandler(XBMainPanel.textExpr),
                true);		

		return everythingPanel;
	}

	private static void configureColumnSearchInputEvents() {
		XBMainPanel.columnSearchInput.addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				if ((ENTER_KW).equals(XBMainPanel.columnSearchInput.getText().trim())) {
					allowFilter = true;
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if ((ENTER_KW).equals(XBMainPanel.columnSearchInput.getText().trim())) {
					allowFilter = true;
				}
			}
		});

		XBMainPanel.columnSearchInput.addKeyListener(new SearchInputKeyListener());

		XBMainPanel.columnSearchInput.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				allowFilter = true;
			}
		});

		XBMainPanel.columnSearchInput.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				if (wrapperSearchInputDialog != null) {
					if (allowFilter) {
						if (!wrapperSearchInputDialog.isVisible()) {
							searchButton.setSelected(false);
							searchButton.doClick();
						}
						filter();
					}
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				if (wrapperSearchInputDialog != null) {
					if (allowFilter) {
						if (!wrapperSearchInputDialog.isVisible()) {
							searchButton.setSelected(false);
							searchButton.doClick();
						}
						filter();
					}
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				if (wrapperSearchInputDialog != null) {
					if (allowFilter) {
						if (!wrapperSearchInputDialog.isVisible()) {
							searchButton.setSelected(false);
							searchButton.doClick();
						}
						filter();
					}
				}
			}

			private void filter() {
				MyTableRowFilter.searchText = XBMainPanel.columnSearchInput.getText().trim();
				((TableRowSorter) jTable.getRowSorter()).setRowFilter(new MyTableRowFilter());
				if (jTable.getModel().getRowCount() > 0) {
					if ((jTable.getRowCount() - 1) >= 0) {
						jTable.addRowSelectionInterval(0, 0); // always highlight first row
						jTable.scrollToMakeRowVisible(0);
					}
				} 
			}
		});
	}

	public static void resetSearchColumnDialogLocationOnScreen() {
		if (wrapperSearchInputDialog == null) {return; }
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
	
	private static void showSearchDialog() {
		resetSearchColumnDialogLocationOnScreen(); 
		if (wrapperSearchInputDialog != null) {
			wrapperSearchInputDialog.setVisible(true);		
		}
	}	

	private static void hideSearchDialog() {
		if (wrapperSearchInputDialog != null && wrapperSearchInputDialog.isVisible()) {
			wrapperSearchInputDialog.setVisible(false);
		}
		if (alignRelativeTo != null) {
			alignRelativeTo.getIconContainer().setSelected(false);
		}
		if (searchButton != null) {
		    searchButton.setSelected(false);
		}
	}
	
	private static void resetSearch() {
		if (wrapperSearchInputDialog != null && wrapperSearchInputDialog.isVisible()) {
			wrapperSearchInputDialog.setVisible(false);
		}
		if (alignRelativeTo != null) {
			alignRelativeTo.getIconContainer().setSelected(false);
		}
		if (searchButton != null) {
		    searchButton.setSelected(false);
		}
		XBMainPanel.columnSearchInput.setEnabled(true);
		XBMainPanel.columnSearchInput.setToolTipText(null);
		XBMainPanel.columnSearchInput.setText(ENTER_KW);
	}

	private static class MyTableRowFilter extends RowFilter<Object, Object> {
		private static String searchText;

		@Override
		public boolean include(Entry entry) {
			if (searchText.trim().isEmpty()) {
				return true;
			}
			int row = (int) entry.getIdentifier();
			Field field = ((ColumnSearchTableModel)entry.getModel()).getColumnDataAt(row);
			searchText = searchText.replaceAll("\\s+", " ");
			String fieldignorecase = field.name.toLowerCase();
			String fieldtypeignorecase = field.type.getSimpleName().toLowerCase();
			return (fieldignorecase.contains(searchText.toLowerCase()) || 
					fieldtypeignorecase.contains(searchText.toLowerCase()));
		}
	}


	private static class SearchInputKeyListener extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			XBMainPanel.columnSearchInput.grabFocus();
			String txt = XBMainPanel.columnSearchInput.getText().trim();
			if (ENTER_KW.equalsIgnoreCase(txt)) {
				XBMainPanel.columnSearchInput.setText(EMPTY);
			}
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
				int rowindex = jTable.getSelectedRow();
				Field selectedcolumn = jTable.getColumnForSelectedRow(jTable, rowindex);
				INSERT(selectedcolumn); //insert text into Expression Text area
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
				XBMainPanel.columnSearchInput.setText(ENTER_KW);
				XBMainPanel.columnSearchInput.grabFocus();
			}
		}
	}
	
	static void INSERT(Field column) {
		if (column != null) {
			String delimstring = " " + Data.ALIAS_DELIM + column.name + Data.ALIAS_DELIM + " ";
			XBMainPanel.INSERT_TEXT.insertAndRemove(XBMainPanel.textExpr, delimstring);
		}
	}
}
