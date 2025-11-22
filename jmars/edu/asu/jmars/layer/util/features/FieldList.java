package edu.asu.jmars.layer.util.features;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import edu.asu.jmars.layer.shape2.ColumnEditor;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.util.ListType;
import edu.asu.jmars.util.Util;

public class FieldList extends CalculatedField {

	private ListType list;
		
	public FieldList() {
		super("Category Field", ListType.class);
				
		ArrayList<String> categories = new ArrayList<String>();		
		list = new ListType(categories);
	}
	
	@Override
	public Set<Field> getFields() {
		return Collections.emptySet();
	}

	@Override
	public Object getValue(ShapeLayer layer, Feature f) {
		return null;
	}

	public ListType getList(){
		return list;
	}
	
	public void addToList(String newEntry){
		list.add(newEntry);
	}
	public void removeFromList(String oldEntry){
		list.remove(oldEntry);
	}

	public static class Factory extends FieldFactory<Field> {
		public Factory() {
			super("List", FieldList.class, ListType.class);
		}
		
		private JTable tbl = null;
		
		public JPanel createEditor(ColumnEditor editor, Field f) {
			final FieldList fl = (FieldList)f;
			
			JPanel panel = new JPanel();
			panel.setLayout(new BorderLayout());
			panel.setBorder(BorderFactory.createEmptyBorder(10,10, 10, 10));

			final JPanel listPnl = new JPanel();
			Vector rowData = new Vector();
			//populate the row data if the source field has list entries already
			if(fl.getList().getValues().size()>0){
				for(String s : fl.getList().getValues()){
					Vector v = new Vector();
					v.add(s);
					rowData.add(v);
				}
			}
			Vector title = new Vector();
			title.add("List Entries");
			tbl = new JTable(rowData, title);
			tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			final JTextField entryTF = new JTextField();
			entryTF.setColumns(15);
			JButton addBtn = new JButton("Add".toUpperCase());
			addBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String text = entryTF.getText();
					
					//if this entry already exists don't let the user add it again
					for(int i=0; i<tbl.getModel().getRowCount(); i++){
						if(text.equals(tbl.getModel().getValueAt(i, 0))){
							Util.showMessageDialog( 
									"Entry named '"+text+"' already exists.",
									"Cannot Add Entry", JOptionPane.ERROR_MESSAGE);
							return;
						}
					}
					
					//if it makes it to here, that means it is a unique entry
					// so add it to the list
					//clear the text field
					entryTF.setText("");
					//add to ui table
					((DefaultTableModel)tbl.getModel()).addRow(new String[]{text});
					//add to ListType list
					fl.addToList(text);
				}
			});

			entryTF.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					addBtn.doClick();
				}
			});

			JButton delBtn = new JButton("Delete".toUpperCase());
			delBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if(tbl!=null){
						int row = tbl.getSelectedRow();
						String rowObj = (String)tbl.getModel().getValueAt(row, 0);
						//remove from table
						((DefaultTableModel)tbl.getModel()).removeRow(row);
						//remove from ListType list
						fl.removeFromList(rowObj);
					}
					
				}
			});
			
			
			JScrollPane sp = new JScrollPane(tbl, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			sp.setPreferredSize(new Dimension(250,200));
			
			int pad = 2;
			Insets in = new Insets(pad,pad,pad,pad); 
			
			JPanel northPnl = new JPanel(new GridBagLayout());
			northPnl.add(entryTF, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
			northPnl.add(addBtn, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
			northPnl.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
			
			
			JPanel southPnl = new JPanel();
			southPnl.add(delBtn);
			
			panel.add(northPnl, BorderLayout.NORTH);
			panel.add(sp, BorderLayout.CENTER);
			panel.add(southPnl, BorderLayout.SOUTH);
			
			
			return panel;
		}
		public Field createField(Set<Field> fields) {
			return new FieldList();
		}
				
		private AbstractAction delAct = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				
			}
		};
	}
}
