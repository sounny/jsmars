package edu.asu.jmars.swing.tableheader.popup;

import javax.swing.JTable;
import javax.swing.table.TableColumnModel;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;
import edu.asu.jmars.util.stable.FilteringColumnModel;

public class ManageColumns implements QuickMenuCommand {

	CommandReceiver cr;	
	private JTable jTable;
	private int eventX = -1;
	
	public ManageColumns(CommandReceiver knowsHowTodo, JTable atable, int eventColumn) {		
		this.cr = knowsHowTodo;
		this.jTable = atable;
		this.eventX = eventColumn;
	}

	public int getEventX() {
		return eventX;
	}

	public void setEventX(int eventX) {
		this.eventX = eventX;
	}

	@Override
	public void execute() {
		if ((jTable instanceof STable)) {
			STable stable = (STable) jTable;
			TableColumnModel m = stable.getColumnModel();
			if (m instanceof FilteringColumnModel) {
				stable.getColumnDialog().setVisible(true);
			}
		}
	}
}
