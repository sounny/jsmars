package edu.asu.jmars.swing;

import static edu.asu.jmars.swing.tableheader.popup.MenuCommand.SECONDARY_ASC;
import static edu.asu.jmars.swing.tableheader.popup.MenuCommand.SECONDARY_DESC;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import edu.asu.jmars.Main;
import edu.asu.jmars.swing.tableheader.popup.HeaderPopup;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FontFile;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.stable.ColumnDialog;
import edu.asu.jmars.util.stable.FilteringColumnModel;
import edu.asu.jmars.util.stable.Sorter;
import jiconfont.DefaultIconCode;
import jiconfont.IconCode;
import jiconfont.IconFont;
import jiconfont.swing.IconFontSwing;


public class CustomHeaderRenderer implements TableCellRenderer {
	private JTable jTable;
	private TableColumnModel tableColumnModel;
	private TableCellRenderer renderer;
	private JTableHeader header;
	private JLabel checkboxlabel;
	private final JCheckBox check = new JCheckBox();
	private static final int buttonheight = 14;
	private static final int buttonwidth = 4;
	private static final int iconsize = 18;
	private final static Color imgColor = ((ThemeImages) GUITheme.get("images")).getCommonFill();
	private static Icon ellipse = new ImageIcon(
			ImageFactory.createImage(ImageCatalogItem.ELLIPSE_MENU.withDisplayColor(imgColor)));
	private Dimension buttonSize = new Dimension(buttonwidth, buttonheight);
	private MouseAdapter my3dotmouseadapter = null;
	private MouseAdapter mycellcompmouseadapter = null;
	private MouseAdapter mycheckboxmouseadapter = null;
	private static HeaderPopup headerpopup = null;  
	private MyIconFont iconfont;
	private IconCode iconcode;
	private Icon sortASCIcon, sortDESCIcon, sortSecondaryASCIcon, sortSecondaryDescIcon;
	private static final Character SORT_ASC = '\u2191';
	private static final Character SORT_DESC = '\u2193';
	private static final Character SECONDARY_SORT_ASC = '\u21C8';
	private static final Character SECONDARY_SORT_DESC = '\u21CA';
	private MouseEventReposter reposter = null;
	private Map<Integer, MouseEventReposter> repostermap = new HashMap<>();
	public boolean showCheckbox = false;
	private static final String SELECT_ALL_TIP = "Click here to select all columns for the table";
	private static final String SELECT_NONE_TIP = "Click here to remove all columns from the table";
	
	
    public CustomHeaderRenderer(JTable atable) {
		jTable = atable;
		header = jTable.getTableHeader();
		renderer = header.getDefaultRenderer();
		tableColumnModel = header.getColumnModel();
		my3dotmouseadapter = new On3DotClickAction();	
		mycellcompmouseadapter = new OnCellComponentClickAction();
		mycheckboxmouseadapter = new OnCheckboxClickAction();	
		repostermap.clear();
		createSortIcons();				
	}

	private void createSortIcons() {
		iconfont = new MyIconFont(FontFile.MYFONT_REGULAR.toString());
		IconFontSwing.register(iconfont);	
		iconcode = new DefaultIconCode(iconfont.getFontFamily(), SORT_ASC);
		sortASCIcon = IconFontSwing.buildIcon(iconcode, iconsize, imgColor);
		iconcode = new DefaultIconCode(iconfont.getFontFamily(), SORT_DESC);
		sortDESCIcon = IconFontSwing.buildIcon(iconcode, iconsize, imgColor);		
		iconcode = new DefaultIconCode(iconfont.getFontFamily(), SECONDARY_SORT_ASC);
		sortSecondaryASCIcon = IconFontSwing.buildIcon(iconcode, iconsize, imgColor);	
		iconcode = new DefaultIconCode(iconfont.getFontFamily(), SECONDARY_SORT_DESC);
		sortSecondaryDescIcon = IconFontSwing.buildIcon(iconcode, iconsize, imgColor);			
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int col) {

		JPanel panel = new JPanel();		
		panel.setLayout(new BorderLayout());
		panel.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		panel.setBackground(ThemeProvider.getInstance().getAction().getMain());
		panel.addMouseListener(mycellcompmouseadapter);
		
		JPanel checkboxpanel = new JPanel(new BorderLayout());
		checkboxpanel.setBackground(ThemeProvider.getInstance().getAction().getMain());
		
		if (col == 0 && isShowCheckbox()) {
			check.setOpaque(false);
			checkboxlabel = new JLabel(new CheckBoxIcon(check));
			checkboxlabel.setToolTipText(check.isSelected() ? SELECT_NONE_TIP : SELECT_ALL_TIP);
			checkboxlabel.addMouseListener(mycheckboxmouseadapter);
			checkboxpanel.add(checkboxlabel, BorderLayout.WEST);          
		}

		JLabel sorticonLbl = new JLabel();
 		Icon sorticon = getIconBasedOnSortOrder(col);
		if (sorticon != null) {
		    sorticonLbl.setIcon(sorticon);
		}
		checkboxpanel.add(sorticonLbl, BorderLayout.CENTER);
		
		panel.add(checkboxpanel, BorderLayout.WEST);
		
		Component c = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
		if (c instanceof JComponent) {
			((JComponent) c).setBorder(null);
		}
		
		panel.add(c, BorderLayout.CENTER);

		JButton menubutton = new JButton();
		menubutton.setIcon(ellipse);
		menubutton.setMaximumSize(buttonSize);
		menubutton.setPreferredSize(buttonSize);
		menubutton.setUI(new LikeDefaultButtonUI());
		menubutton.addMouseListener(my3dotmouseadapter);
		panel.add(menubutton, BorderLayout.EAST);		

		//add event to header
		if (this.header != null) {
			if (repostermap.get(col) == null) {
				this.reposter = new MouseEventReposter(this.header, col, panel);
				this.header.addMouseListener(this.reposter);
				repostermap.put(col, this.reposter);
			} 
		}		
		return panel;
	}

	private boolean isShowCheckbox() {
		return showCheckbox ;
	}

	private Icon getIconBasedOnSortOrder(int col) {
		Icon defaulticon = null;
		if (!(jTable instanceof STable)) return null;
		if (col == -1) return null;
		tableColumnModel = header.getColumnModel();
		if (tableColumnModel == null || tableColumnModel.getColumnCount() <= 0) return null;
		STable stable = (STable)jTable;
		Sorter sorter = stable.getSorter();
		if (sorter==null) return null;
		TableColumn column = stable.getColumnModel().getColumn(col);
		int pos = sorter.getSorts().indexOf(column);
		switch (sorter.getDirection(column)) {
		case -1:
			if (pos == 0) {
				defaulticon = sortDESCIcon;
			} else if (pos == 1) {
				defaulticon = sortSecondaryDescIcon;
			}
			break;
		case 0:
			return defaulticon; // default, no icon
		case 1:
			if (pos == 0) {
				defaulticon = sortASCIcon;
			} else if (pos == 1) {
				defaulticon = sortSecondaryASCIcon;
			}
			break;
		}		
		return defaulticon;
	}

	private void enableDisableMenuItems(int columnIndex) {
		boolean isAnyColumnSelected = isAnyColumnSelectedForSorting(jTable);
		Component[] items = headerpopup.getPopupmenu().getComponents();
		//enable all menu items
		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof JMenuItem) {
				JMenuItem item = (JMenuItem) items[i];
				item.setEnabled(true);
			}
		}
		//check 1 - if No Sort selected per table, then secondary sorts are disabled
		//but if any column is selected already then all menu items will be available
		if (!isAnyColumnSelected) {
		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof JMenuItem) {
				JMenuItem item = (JMenuItem) items[i];
				if (item.getText().contains(SECONDARY_ASC.getMenuCommand())) {						
						item.setEnabled(false);
				}
				else if (item.getText().contains(SECONDARY_DESC.getMenuCommand())) {						
						item.setEnabled(false);
				}
			}
		}
	}		
		//check 2 - if primary sort selected per column, then secondary sorts are disabled per that column
		if (isPrimarySortSelectedPerColumn(columnIndex)) {
		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof JMenuItem) {
				JMenuItem item = (JMenuItem) items[i];
				if (item.getText().contains(SECONDARY_ASC.getMenuCommand())) {						
						item.setEnabled(false);
				}
				else if (item.getText().contains(SECONDARY_DESC.getMenuCommand())) {						
						item.setEnabled(false);
				}
			}
		  }	
		}			
	}

	private boolean isPrimarySortSelectedPerColumn(int columnIndex) {
		boolean isPrimarySortForColumn = false;
		if (!(jTable instanceof STable)) return isPrimarySortForColumn;				
		STable stable = (STable) jTable;
		Sorter sorter = stable.getSorter();
		if (sorter != null) {
			TableColumn column = stable.getColumnModel().getColumn(columnIndex);
			int pos = sorter.getSorts().indexOf(column);
			if (pos == 0) {
				isPrimarySortForColumn = true;
			}
		}
		return isPrimarySortForColumn;
	}

	private boolean isAnyColumnSelectedForSorting(JTable jTable) {
		boolean isAnyColumn = false;
		if (!(jTable instanceof STable)) return isAnyColumn;
		STable stable = (STable)jTable;
		Sorter sorter = stable.getSorter();
		if (sorter != null) {
			if (!sorter.getSorts().isEmpty()) {
				isAnyColumn = true;
			}
		}
		return isAnyColumn;
	}
	
	
	private class MyIconFont implements IconFont {
		private String fontfilename;

		MyIconFont(String filename) {
			this.fontfilename = filename;
		}

		@Override
		public String getFontFamily() {
			return ThemeFont.getFontFamily();
		}

		@Override
		public InputStream getFontInputStream() {
			return Main.getResourceAsStream(ThemeFont.getFontPath() + this.fontfilename);
		}
	}
	
	private class On3DotClickAction extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent ev) {
			headerpopup.getPopupmenu().show(ev.getComponent(), ev.getX(), ev.getY());
		}
	}
	
	private class OnCheckboxClickAction extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent ev) {
			check.setSelected(!check.isSelected());
			if ((jTable instanceof STable)) {
				STable stable = (STable) jTable;
				ColumnDialog coldlg = stable.getExistingColumnDialog();
				if (coldlg != null) {
					FilteringColumnModel fmodel = stable.getUnderlyingFilteringColumnModel();
					if (fmodel != null) {
						fmodel.setAllVisible(check.isSelected());  //show all/hide all
						coldlg.validate();
					}
				}
			}
		}
	}		
	
	private class OnCellComponentClickAction extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent ev) {
			if (SwingUtilities.isRightMouseButton(ev)) {
				headerpopup.getPopupmenu().show(ev.getComponent(), ev.getX(), ev.getY());				
			}
			else if ((jTable instanceof STable)) {
				STable stable = (STable) jTable;
				Sorter sorter = stable.getSorter();
				if (sorter != null) {
					int col = headerpopup.getEventColumn();
					TableColumn column = stable.getColumnModel().getColumn(col);
					boolean shiftPressed = (0 != (ev.getModifiers() & InputEvent.SHIFT_MASK));
					if (shiftPressed) {
						sorter.addSort(column);
					} else {
						sorter.setSort(column);
					}
				}
			}
		}
	}
	
	private class MouseEventReposter extends MouseAdapter {
		private Component dispatchComponent;
		private JTableHeader header2;
		private int column = -1;
		private Component editor;

		public MouseEventReposter(JTableHeader header, int column, Component editor) {
			this.header2 = header;
			this.column = column;
			this.editor = editor;
		}

		private void setDispatchComponent(MouseEvent e) {
			int col = header2.getTable().columnAtPoint(e.getPoint());
			if (col != column || col == -1)
				return;
			Point p = e.getPoint();
			Point p2 = SwingUtilities.convertPoint(header2, p, editor);
			prepForRedispatch(col);			
			dispatchComponent = SwingUtilities.getDeepestComponentAt(editor, p2.x, p2.y);
		}

		private void prepForRedispatch(int columnIndex) {
			headerpopup = new HeaderPopup(jTable);
			headerpopup.setEventColumn(columnIndex);
			enableDisableMenuItems(columnIndex);	
		}

		private boolean repostEvent(MouseEvent e) {
			if (dispatchComponent == null) {
				return false;
			}
			MouseEvent e2 = SwingUtilities.convertMouseEvent(header2, e, dispatchComponent);	
			dispatchComponent.dispatchEvent(e2);
			return true;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (header2.getResizingColumn() == null) {
				Point p = e.getPoint();
				int col = header2.getTable().columnAtPoint(p);
				if (col != column || col == -1)
					return;
				int index = header2.getColumnModel().getColumnIndexAtX(p.x);
				if (index == -1)
					return;
				editor.setBounds(header.getHeaderRect(index));
				header2.add(editor);
				editor.validate();
				setDispatchComponent(e);
				repostEvent(e);
				dispatchComponent = null;
				header2.remove(editor);
			}			
		}
	}
	
	private  class CheckBoxIcon implements Icon {
	    private final JCheckBox check;

	    public CheckBoxIcon(JCheckBox check) {
	        this.check = check;
	    }

	    @Override
	    public int getIconWidth() {
	        return check.getPreferredSize().width;
	    }

	    @Override
	    public int getIconHeight() {
	        return check.getPreferredSize().height;
	    }

	    @Override
	    public void paintIcon(Component c, Graphics g, int x, int y) {
	        SwingUtilities.paintComponent(
	                g, check, (Container) c, x, y, getIconWidth(), getIconHeight());
	    }
    }
	
}
