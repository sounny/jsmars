package edu.asu.jmars.swing.tableheader.popup;

import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;
import edu.asu.jmars.util.stable.Sorter;

public class SortDESCCmd implements QuickMenuCommand {

	CommandReceiver cr;
	private JTable jTable;
	private int eventX = -1;

	public SortDESCCmd(CommandReceiver knowsHowTodo, JTable atable, int eventColumn) {
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
		if (!(jTable instanceof STable))
			return;
		STable stable = (STable) jTable;
		Sorter sorter = stable.getSorter();
		TableColumnModel m = stable.getColumnModel();
		if (sorter != null) {
			int columnIndex = this.eventX;
			if (columnIndex < 0) // not a valid column location
				return;
			TableColumn column = m.getColumn(columnIndex);
			sorter.setSort(column, SortDirection.DESCENDING); // primary sort
		}
	}
}
