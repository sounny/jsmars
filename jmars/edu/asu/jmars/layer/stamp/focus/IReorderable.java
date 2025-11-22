package edu.asu.jmars.layer.stamp.focus;

import javax.swing.JTable;

public interface IReorderable {
	   public void reorder(int fromIndex, int toIndex);
	  default public void withTable(JTable tbl) {};
}

