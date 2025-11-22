package edu.asu.jmars.swing.quick.edit.row;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.swing.quick.menu.command.CommandExecutor;
import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;
import static edu.asu.jmars.swing.quick.edit.row.MenuCommand.OPEN;
import static edu.asu.jmars.swing.quick.edit.row.MenuCommand.OPEN_DOCKED;
import static edu.asu.jmars.swing.quick.edit.row.MenuCommand.DELETE;
import static edu.asu.jmars.swing.quick.edit.row.MenuCommand.RENAME;
import static edu.asu.jmars.swing.quick.edit.row.MenuCommand.TOOLTIP;
import javax.swing.ActionMap;


public class RowQuickAccessPopup extends JPanel {

	private JPopupMenu rowpopup;
	private LView view;
	private JCheckBoxMenuItem tooltip;
	private JMenuItem item, deleteItem;
	private KeyStroke keyStroke;
	private ActionMap actionMap;
	private CommandExecutor knowsWhatTodo = new CommandExecutor();

	public RowQuickAccessPopup() {			

		rowpopup = new JPopupMenu();
	
		ActionListener menuListener = (ActionEvent event) -> {
			String cmd = event.getActionCommand();
			knowsWhatTodo.processRequest(MenuCommand.get(cmd));
		};

		int platformshortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

		rowpopup.add(item = new JMenuItem(OPEN.getMenuCommand()));
		keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK);
		item.setAccelerator(keyStroke);
		item.setHorizontalTextPosition(JMenuItem.RIGHT);
		item.addActionListener(menuListener);		

		rowpopup.add(item = new JMenuItem(OPEN_DOCKED.getMenuCommand()));
		keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, platformshortcut | InputEvent.SHIFT_DOWN_MASK);
		item.setAccelerator(keyStroke);
		item.setHorizontalTextPosition(JMenuItem.RIGHT);
		item.addActionListener(menuListener);
	
		rowpopup.add(deleteItem = new JMenuItem(DELETE.getMenuCommand()));
		String osver = System.getProperty("os.name").toLowerCase();		
		int keycode = (osver.indexOf("mac") != -1) ? KeyEvent.VK_BACK_SPACE : KeyEvent.VK_DELETE;
		keyStroke = KeyStroke.getKeyStroke(keycode, platformshortcut);
		deleteItem.setAccelerator(keyStroke);
		deleteItem.setHorizontalTextPosition(JMenuItem.RIGHT);
		deleteItem.addActionListener(menuListener);	
			
		rowpopup.add(item = new JMenuItem(RENAME.getMenuCommand()));
		item.setHorizontalTextPosition(JMenuItem.RIGHT);
		item.addActionListener(menuListener);

		rowpopup.add(tooltip = new JCheckBoxMenuItem(TOOLTIP.getMenuCommand(), true));
		tooltip.setHorizontalTextPosition(JMenuItem.RIGHT);
		tooltip.addActionListener(menuListener);

		buildMenuCommands();
	};

	private void buildMenuCommands() {

		CommandReceiver knowsHowTodo = new CommandReceiver();

		QuickMenuCommand openCmd = new OpenCommand(knowsHowTodo);
		QuickMenuCommand opendockedCmd = new OpenDockedCommand(knowsHowTodo);
		QuickMenuCommand renameCmd = new RenameCommand(knowsHowTodo);
		QuickMenuCommand deleteCmd = new DeleteCommand(knowsHowTodo);
		QuickMenuCommand tooltipCmd = new TooltipCommand(knowsHowTodo, tooltip);

		knowsWhatTodo.addRequest(OPEN, openCmd);
		knowsWhatTodo.addRequest(OPEN_DOCKED, opendockedCmd);
		knowsWhatTodo.addRequest(RENAME, renameCmd);
		knowsWhatTodo.addRequest(DELETE, deleteCmd);
		knowsWhatTodo.addRequest(TOOLTIP, tooltipCmd);
	}

	public JPopupMenu getPopupmenu() {
		return rowpopup;
	}
	
	public JMenuItem getDeleteItem()
	{
		return deleteItem;
	}

}
