package edu.asu.jmars.swing.tableheader.popup;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;
import edu.asu.jmars.Main;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.swing.quick.menu.command.CommandExecutor;
import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FontFile;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.stable.FilteringColumnModel;
import jiconfont.DefaultIconCode;
import jiconfont.IconCode;
import jiconfont.IconFont;
import jiconfont.swing.IconFontSwing;
import static edu.asu.jmars.swing.tableheader.popup.MenuCommand.SORT_ASC;
import static edu.asu.jmars.swing.tableheader.popup.MenuCommand.SORT_DESC;
import static edu.asu.jmars.swing.tableheader.popup.MenuCommand.SECONDARY_ASC;
import static edu.asu.jmars.swing.tableheader.popup.MenuCommand.SECONDARY_DESC;
import static edu.asu.jmars.swing.tableheader.popup.MenuCommand.NO_SORTING;
import static edu.asu.jmars.swing.tableheader.popup.MenuCommand.MANAGE_COLUMNS;
import static edu.asu.jmars.swing.tableheader.popup.MenuCommand.EXPORT_CSV;

public class HeaderPopup extends JPanel {

	private JPopupMenu lqapopup;
	private JMenuItem itemSortAsc, itemSortDesc, itemSortSecondaryAsc, itemSortSecondaryDesc, itemNoSort, itemColumns;	                   
	private static final int iconsize = 18;
	private final static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private JTable jTable;
	private int eventColumn = -1;
	private QuickMenuCommand sortASCCmd;
	private QuickMenuCommand sortDESCCmd;
	private QuickMenuCommand sortSecondaryASCCCmd;
	private QuickMenuCommand sortSecondaryDESCCmd;
	private QuickMenuCommand noSortCmd;
	private QuickMenuCommand manageColumns;
	private QuickMenuCommand exportCSVCmd;
	private MyIconFont iconfont;
	private CommandExecutor knowsWhatTodo = new CommandExecutor();

	public HeaderPopup(JTable atable) {

			lqapopup = new JPopupMenu();
			
			iconfont = new MyIconFont(FontFile.MYFONT_REGULAR.toString());
			IconFontSwing.register(iconfont);
			
			this.jTable = atable;			

			ActionListener menuListener = (ActionEvent event) ->  {
				buildMenuCommands();
				String cmd = event.getActionCommand();
				knowsWhatTodo.processRequest(MenuCommand.get(cmd));			
			};		
			
			//JMenuItem item;		
			Icon menuicon = getDefaultIcon('\u2191');
			lqapopup.add(itemSortAsc = new JMenuItem(SORT_ASC.getMenuCommand(), menuicon));
			itemSortAsc.setHorizontalTextPosition(JMenuItem.LEFT);		
			itemSortAsc.addActionListener(menuListener);
			
			menuicon = getDefaultIcon('\u2193');
			lqapopup.add(itemSortDesc = new JMenuItem(SORT_DESC.getMenuCommand(), menuicon));
			itemSortDesc.setHorizontalTextPosition(JMenuItem.LEFT);	
			itemSortDesc.addActionListener(menuListener);	
			
			menuicon = getDefaultIcon('\u21C8');
			lqapopup.add(itemSortSecondaryAsc = new JMenuItem(SECONDARY_ASC.getMenuCommand(), menuicon));
			itemSortSecondaryAsc.setHorizontalTextPosition(JMenuItem.LEFT);	
			itemSortSecondaryAsc.addActionListener(menuListener);
		
			
			menuicon = getDefaultIcon('\u21CA');
			lqapopup.add(itemSortSecondaryDesc = new JMenuItem(SECONDARY_DESC.getMenuCommand(), menuicon));
			itemSortSecondaryDesc.setHorizontalTextPosition(JMenuItem.LEFT);	
			itemSortSecondaryDesc.addActionListener(menuListener);	
			
			lqapopup.add(itemNoSort = new JMenuItem(NO_SORTING.getMenuCommand()));
			itemNoSort.setHorizontalTextPosition(JMenuItem.LEFT);	
			itemNoSort.addActionListener(menuListener);	
			
			if (isFilteringColumnModel(this.jTable)) {
				lqapopup.add(itemColumns = new JMenuItem(MANAGE_COLUMNS.getMenuCommand()));
				itemColumns.setHorizontalTextPosition(JMenuItem.LEFT);
				itemColumns.addActionListener(menuListener);
			}
			
			lqapopup.add(itemNoSort = new JMenuItem(EXPORT_CSV.getMenuCommand()));
			itemNoSort.setHorizontalTextPosition(JMenuItem.LEFT);	
			itemNoSort.addActionListener(menuListener);
			
		}

	private void buildMenuCommands() {

		CommandReceiver knowsHowTodo = new CommandReceiver();

		sortASCCmd = new SortASCCmd(knowsHowTodo, this.jTable, this.eventColumn);
		sortDESCCmd = new SortDESCCmd(knowsHowTodo, this.jTable, this.eventColumn);
		sortSecondaryASCCCmd = new SortSecondaryASCCmd(knowsHowTodo, this.jTable, this.eventColumn);
		sortSecondaryDESCCmd = new SortSecondaryDESCCmd(knowsHowTodo, this.jTable, this.eventColumn);
		noSortCmd = new NoSortingCmd(knowsHowTodo, this.jTable, this.eventColumn);	
		if (isFilteringColumnModel(this.jTable)) {
			manageColumns = new ManageColumns(knowsHowTodo, this.jTable, this.eventColumn);
		}
		exportCSVCmd = new ExportCSV(knowsHowTodo, this.jTable, this.eventColumn);
		
		
		knowsWhatTodo.addRequest(SORT_ASC, sortASCCmd);
		knowsWhatTodo.addRequest(SORT_DESC, sortDESCCmd);
		knowsWhatTodo.addRequest(SECONDARY_ASC, sortSecondaryASCCCmd);
		knowsWhatTodo.addRequest(SECONDARY_DESC, sortSecondaryDESCCmd);
		knowsWhatTodo.addRequest(NO_SORTING, noSortCmd);
		//add this menu conditionally
		if (isFilteringColumnModel(this.jTable)) {
		    knowsWhatTodo.addRequest(MANAGE_COLUMNS, manageColumns);
		}
		knowsWhatTodo.addRequest(EXPORT_CSV, exportCSVCmd);
	}

	private boolean isFilteringColumnModel(JTable jTable2) {
		if ((jTable2 instanceof STable)) {
			STable stable = (STable) jTable2;
			TableColumnModel m = stable.getColumnModel();
			if (m instanceof FilteringColumnModel) {
				return true;
			}
		}
		return false;
	}

	public JPopupMenu getPopupmenu() {
		return lqapopup;
	}
	
	public QuickMenuCommand getColumnDialogCommand() {
		return manageColumns;
	}
	
	private Icon getDefaultIcon(char code) {		
		IconCode iconcode = new  DefaultIconCode(iconfont.getFontFamily(),code); //arrow down
		Icon defaultIcon = IconFontSwing.buildIcon(iconcode, iconsize, imgColor);	
		return defaultIcon;
	}	
	

	public void setEventColumn(int col) {
		this.eventColumn = col;		
	}
	
	public int getEventColumn() {
		return this.eventColumn;		
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

}
