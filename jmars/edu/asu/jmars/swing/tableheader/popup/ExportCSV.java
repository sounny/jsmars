package edu.asu.jmars.swing.tableheader.popup;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumn;

import edu.asu.jmars.Main;
import edu.asu.jmars.swing.LikeDefaultButtonUI;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.stable.FilteringColumnModel;

public class ExportCSV implements QuickMenuCommand {

	CommandReceiver cr;	
	private JTable jTable;
	STable stable = null;
	private int eventX = -1;
	private String delim = null;
	private JFileChooser fileChooser = null;
	private JList<String> availableList = null;
	private JList<String> selectedList = null;
	private DefaultListModel<String> selectedListModel = null;
	private DefaultListModel<String> availableListModel = null;
	private JDialog dialog = null;
	private JRadioButton separatorComma = null;
	private JRadioButton separatorTab = null;
	private JRadioButton separatorChar = null;
	private JTextField separatorTF = null;
	private JRadioButton exportAllRows = null;
	private JRadioButton exportSelectedRows = null;
	private JRadioButton arrayAsOneColumnRadio = null;
	private JRadioButton arrayAsManyColumnsRadio = null;
	private ArrayList<String> hiddenColumns = null;
	
	public ExportCSV(CommandReceiver knowsHowTodo, JTable atable, int eventColumn) {		
		this.cr = knowsHowTodo;
		this.jTable = atable;
		this.eventX = eventColumn;
		fileChooser = new JFileChooser(Util.getDefaultFCLocation());
		fileChooser.setApproveButtonText("SAVE");
		FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv", "txt");
		fileChooser.setFileFilter(filter);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setDialogTitle("Select output CSV file name to save table");
	}

	public int getEventX() {
		return eventX;
	}

	public void setEventX(int eventX) {
		this.eventX = eventX;
	}

	private AbstractAction allVisibleAction = new AbstractAction("ONLY VISIBLE") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if ((jTable instanceof STable)) {
				STable stable = (STable) jTable;
				FilteringColumnModel columnModel = (FilteringColumnModel)stable.getColumnModel();
				
				if (columnModel != null) {
					availableListModel.clear();
					selectedListModel.clear();
					for (final TableColumn column: columnModel.getAllColumns()) {
						String name = column.getHeaderValue().toString();
						boolean visible = (null != columnModel.getVisColumn(column.getIdentifier()));
						if (visible) {
							selectedListModel.addElement(name);
							availableListModel.removeElement(name);
						} else {
							availableListModel.addElement(name);
						}
					}
				}
			}
		}
	};
	private AbstractAction allColumnsAction = new AbstractAction("ALL COLUMNS") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Enumeration<String> elements = availableListModel.elements();
			while(elements.hasMoreElements()) {
				String element = elements.nextElement();
				selectedListModel.addElement(element);
			}
			availableListModel.removeAllElements();
			
		}
	};

	private AbstractAction selectColumnsAction = new AbstractAction(">>") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			List<String> selectedValuesList = availableList.getSelectedValuesList();
			for (String tmp : selectedValuesList) {
				selectedListModel.addElement(tmp);
				availableListModel.removeElement(tmp);
			}
		}
	};	
	
	private AbstractAction removeColumnsAction = new AbstractAction("<<") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			List<String> selectedValuesList = selectedList.getSelectedValuesList();
			for (String tmp : selectedValuesList) {
				availableListModel.addElement(tmp);
				selectedListModel.removeElement(tmp);
			}
		}
	};
	
	private AbstractAction removeAllAction = new AbstractAction("REMOVE ALL") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			Enumeration<String> elements = selectedListModel.elements();
			while(elements.hasMoreElements()) {
				String element = elements.nextElement();
				availableListModel.addElement(element);
			}
			selectedListModel.removeAllElements();
		}
	};
	
	private AbstractAction cancelAction = new AbstractAction("CANCEL") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			dialog.setVisible(false);
			dialog.dispose();
		}
	};
	
	private AbstractAction continueAction = new AbstractAction("CONTINUE") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (selectedListModel.getSize() == 0) {
				Util.showMessageDialog("Please select columns to export.");
				return;
			}
			
			if (separatorComma.isSelected()) {
				delim = ",";
			} else if (separatorTab.isSelected()) {
				delim = String.valueOf('\t');
			} else {
				delim = String.valueOf(separatorChar.getText());
			}
			
			int returnVal = fileChooser.showOpenDialog(dialog);
	   		String outputFilename = null;
	   		if (returnVal == JFileChooser.APPROVE_OPTION) {
	   			outputFilename = fileChooser.getSelectedFile().getPath();
       			if (!outputFilename.contains(".")) {
       				outputFilename += ".csv";
       			}
       			try {
       				PrintWriter out = new PrintWriter(outputFilename);
       				int n = stable.getRowCount();
       				int m = selectedListModel.getSize();

       				List<String> vals = new ArrayList<String>();

       				ArrayList<String> headerNames =  new ArrayList<String>();
       				int[] selRows = null;
       				boolean selRowsOnly = false;
       				if (exportSelectedRows.isSelected()) {
       					selRows = stable.getSelectedRows();
       					selRowsOnly = true;
       					n = stable.getSelectedRowCount();
       				} 
       				FilteringColumnModel columnModel = (FilteringColumnModel)stable.getColumnModel();
       				List<TableColumn> allColumns = columnModel.getAllColumns();
       				HashMap<String, Integer> idx_colName = new HashMap<String, Integer>();
       				for(int x=0; x<allColumns.size(); x++) {
       					idx_colName.put((String)allColumns.get(x).getHeaderValue(),x);
       				}

					boolean arrayAsOneColumn = (arrayAsOneColumnRadio.isSelected() ? true : false);
					boolean firstRow = true;
       				for(int j=0; j<n; j++) {//n may be the number of selected rows or the table row count
       					vals.clear();
       					int rowIdx = j;
   						if (selRowsOnly) {//if we are exporting selected rows only, use the row index from selected rows
   							rowIdx = selRows[j];
   						}
       					for(int i=0; i<m; i++) {
       						String colName = selectedListModel.get(i);
       						int colIdx = idx_colName.get(colName); 
       						
       						Object valueObj = stable.getUnsortedTableModel().getValueAt(rowIdx, colIdx);
       						if (valueObj == null) {
       							vals.add("");
       							headerNames.add(colName);
       						} else {
	       						String value = "\""; //only used for arrays as one column
	       						if (valueObj.getClass().isArray()) {
	       							Object[] objArr = (Object[])valueObj;
	       							boolean first = true;
	       							for(int x=0; x<objArr.length; x++) {
	       								if (arrayAsOneColumn) {
	       									if (!first) {
		       									value += ",";
		       								} else {
		       									headerNames.add(colName);
		       								}
		       								value += String.valueOf(objArr[x]);
		       								
		   									first = false;	
	       								} else {
	       									//one column per entry in the array
	       									headerNames.add(colName+"["+x+"]");
	       									vals.add(String.valueOf(objArr[x]));
	       								}
	       							}
	       							value += "\"";
	       							
	       							if (arrayAsOneColumn) {//don't do this if we added multiple columns above
		       							vals.add(value);
		       						}
	       						} else {
	       							headerNames.add(colName);
	       							if (valueObj instanceof String) {
	       								value = (String) valueObj;
	       							} else {
	       								value = String.valueOf(valueObj);
	       							}
	       							vals.add(value);
	       						}
	       						
       						}
       					}
       					if (firstRow) {
       						out.println(Util.join(delim, headerNames));
       						firstRow = false;
       					}
       					out.println(Util.join(delim, vals));
       				}
       				out.close();
       				dialog.setVisible(false);
       				dialog.dispose();
       				Util.showMessageDialog("Table successfully exported to: " + fileChooser.getSelectedFile().getName());
       			}
       			catch(IOException ex) {
					Util.showMessageDialog(ex.getClass().getName() + " occurred while writing CSV file to " + outputFilename + ": " + ex.getMessage());
       			}
	   		}
		}
	};
	
	private AbstractAction moveUpAction = new AbstractAction("UP") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			int[] selectedIndices = selectedList.getSelectedIndices();
			int[] newSelections = new int[selectedIndices.length];
			int count = 0;
			int minIdx = -1;
			boolean successMove = false;
			if (selectedIndices.length > 0) {
				for (int x : selectedIndices) {
					String tmp = selectedListModel.get(x);
					if (minIdx == -1) {
						minIdx = x;
					}
						
					if (minIdx > 0) { 
						selectedListModel.add(x-1,tmp);
						selectedListModel.remove(x+1);
						newSelections[count++] = x-1;
						successMove = true;
					}
				}
				if (successMove) {
					selectedList.setSelectedIndices(newSelections);
					selectedList.ensureIndexIsVisible(selectedIndices[0] -1);
				}
			}
		}
	};
	
	private AbstractAction moveDownAction = new AbstractAction("DOWN") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			int[] selectedIndices = selectedList.getSelectedIndices();
			int[] newSelections = new int[selectedIndices.length];
			if (selectedIndices.length > 0) {
				int maxIdx = -1;
				boolean successMove = false;
				for (int c=selectedIndices.length-1; c>=0; c--) {
					int x = selectedIndices[c];
					if (maxIdx == -1) {
						maxIdx = x;
					}
					String tmp = selectedListModel.get(x);
					if (maxIdx < selectedListModel.getSize() - 1) { 
						selectedListModel.add(x+2,tmp);
						selectedListModel.remove(x);
						newSelections[c] = x+1;
						successMove = true;
					}
				}
				if (successMove) {
					selectedList.setSelectedIndices(newSelections);
					selectedList.ensureIndexIsVisible(selectedIndices[selectedIndices.length-1]+1);
				}
			}
		}
	};
	
	@Override
	public void execute() {
		String description = "Data";
		Frame parent = null;
		JDialog parentDialog = null;
		if ((jTable instanceof STable)) {
			stable = (STable) jTable;
			if (stable.getDescription() != null) {
				description = stable.getDescription();
			}
			if (stable.getParentFrame() != null) {
				parent = stable.getParentFrame();
			} else if (stable.getParentDialog() != null) {
				parent = null;
				parentDialog = stable.getParentDialog();
			}
		}
		
		if (parent != null) {
			dialog = new JDialog(parent);
		} else if (parentDialog != null) {
			dialog = new JDialog(parentDialog);
		} else {
			dialog = new JDialog(Main.mainFrame);
		}
		
		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		JLabel title = new JLabel("Export "+description+" Table");
		JLabel colsAvailable = new JLabel("Available Columns: ");
		JLabel colsSelected = new JLabel("Selected Columns: ");
		JLabel separatorLbl = new JLabel("Separator: ");
		
		availableListModel = new DefaultListModel<String>();
		selectedListModel = new DefaultListModel<String>();
		
		hiddenColumns = new ArrayList<String>();
		
		if ((jTable instanceof STable)) {
			FilteringColumnModel columnModel = (FilteringColumnModel)stable.getColumnModel();
			
			if (columnModel != null) {
				availableListModel.clear();
				selectedListModel.clear();
				for (final TableColumn column: columnModel.getAllColumns()) {
					String name = column.getHeaderValue().toString();
					boolean visible = (null != columnModel.getVisColumn(column.getIdentifier()));
					if (visible) {
						selectedListModel.addElement(name);
					} else {
						hiddenColumns.add(name);
						availableListModel.addElement(name);
					}
				}
			} 
		}
		
		
		availableList = new JList<String>(availableListModel);
		availableList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JScrollPane availableSP = new JScrollPane(availableList);
		availableSP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		availableSP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		availableList.setCellRenderer(new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (c instanceof JLabel) {
					JLabel label = (JLabel) c;
					if (hiddenColumns.contains(label.getText())) {
						label.setText(label.getText()+" *");
					}
					return label;
				}
				return c;
			}
		});
		
		
		selectedList = new JList<String>(selectedListModel);
		selectedList.setDragEnabled(true);
		selectedList.setDropMode(DropMode.INSERT);
		selectedList.setTransferHandler(new ListTransferHandler());
		JScrollPane selectedSP = new JScrollPane(selectedList);
		selectedSP.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		selectedSP.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		selectedList.setCellRenderer(new DefaultListCellRenderer() {

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (c instanceof JLabel) {
					JLabel label = (JLabel) c;
					if (hiddenColumns.contains(label.getText())) {
						label.setText(label.getText()+" *");
					}
					return label;
				}
				return c;
			}
		});
		
		
		JButton allVisibleBtn = new JButton(allVisibleAction);
		allVisibleBtn.setToolTipText("Add only the columns that are visible on the table");
		JButton allColumnsBtn = new JButton(allColumnsAction);
		allColumnsBtn.setToolTipText("Add all the columns available on the table. Some columns may be hidden on the table.");
		JButton selectColsBtn = new JButton(selectColumnsAction);
		selectColsBtn.setToolTipText("Move the highlighted columns in the available list to the selected list.");
		JButton removeColsBtn = new JButton(removeColumnsAction);
		removeColsBtn.setToolTipText("Remove the highlighted columns from the selected list.");
		JButton removeAllBtn = new JButton(removeAllAction);
		removeAllBtn.setToolTipText("Remove all columns from the selected list.");
		removeColsBtn.setUI(new LikeDefaultButtonUI());
		selectColsBtn.setUI(new LikeDefaultButtonUI());

		exportAllRows = new JRadioButton("Export All Rows");
		exportSelectedRows = new JRadioButton("Export Selected Rows");
		ButtonGroup rowsBtnGrp = new ButtonGroup();
		rowsBtnGrp.add(exportAllRows);
		rowsBtnGrp.add(exportSelectedRows);
		
		JLabel arraysLbl = new JLabel("Arrays:");
		arrayAsOneColumnRadio = new JRadioButton("All values in one column");
		arrayAsManyColumnsRadio = new JRadioButton("One column per entry");
		ButtonGroup arrayBtnGrp = new ButtonGroup();
		arrayBtnGrp.add(arrayAsManyColumnsRadio);
		arrayBtnGrp.add(arrayAsOneColumnRadio);
		arrayAsManyColumnsRadio.setSelected(true);
		arrayAsOneColumnRadio.setToolTipText("Array values will be exported as one column in double quotes with each array entry separated by a comma.");
		arrayAsManyColumnsRadio.setToolTipText("Array values will be exported as one column per entry in the array.");
		
		separatorComma = new JRadioButton("comma (,)");
		separatorTab = new JRadioButton("tab (\\t)");
		separatorChar = new JRadioButton("other ");
		separatorTF = new JTextField(1);
		separatorTF.setText("'");
		ButtonGroup sepBG = new ButtonGroup();
		sepBG.add(separatorComma);
		sepBG.add(separatorChar);
		sepBG.add(separatorTab);
		
		JLabel moveLbl = new JLabel("Re-order ");
		JButton moveUpBtn = new JButton(moveUpAction);
		moveUpBtn.setToolTipText("Move the highlighted columns up in the order.");
		JButton moveDownBtn = new JButton(moveDownAction);
		moveDownBtn.setToolTipText("Move the highlighted columns down in the order.");
		
		JButton cancelBtn = new JButton(cancelAction);
		JButton continueBtn = new JButton(continueAction);
		
		JLabel selectLbl = new JLabel("Select/Deselect");
		JLabel bulkLbl = new JLabel("Bulk Select Columns");
		JLabel rowsLbl = new JLabel("Included Rows");
		JLabel hiddenLbl = new JLabel("* not currently shown in table");
		
		if (jTable.getSelectedRowCount() > 0) {
			exportSelectedRows.setSelected(true);
		} else {
			exportAllRows.setSelected(true);
			exportSelectedRows.setEnabled(false);
		}
		separatorComma.setSelected(true);
		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
			.addComponent(title)
			.addGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(colsAvailable)
					.addComponent(availableSP, 200, 200, Short.MAX_VALUE)
					.addComponent(rowsLbl)
					.addComponent(exportAllRows)
					.addComponent(exportSelectedRows)
					.addComponent(cancelBtn))	
				.addGroup(layout.createParallelGroup(Alignment.CENTER)
					.addComponent(selectLbl)
					.addGroup(layout.createSequentialGroup()
						.addComponent(removeColsBtn)
						.addComponent(selectColsBtn))
					.addComponent(bulkLbl)
					.addComponent(allVisibleBtn)
					.addComponent(allColumnsBtn))
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(Alignment.LEADING)
							.addComponent(colsSelected)
							.addComponent(selectedSP, 200, 200, Short.MAX_VALUE))
						.addGroup(layout.createParallelGroup(Alignment.LEADING)
							.addComponent(moveLbl)
							.addComponent(moveUpBtn)
							.addComponent(moveDownBtn)
							.addComponent(removeAllBtn)
							.addComponent(continueBtn)))
						.addGroup(layout.createParallelGroup(Alignment.LEADING)
							.addComponent(separatorLbl)
							.addGroup(layout.createSequentialGroup()
								.addComponent(separatorComma)
								.addGap(10)
								.addComponent(separatorTab)
								.addGap(10)
								.addComponent(separatorChar)
								.addComponent(separatorTF, 20, 20, 20))
							.addComponent(arraysLbl)
							.addGroup(layout.createSequentialGroup()
								.addComponent(arrayAsManyColumnsRadio)
								.addComponent(arrayAsOneColumnRadio)))))
			.addComponent(hiddenLbl)
			.addComponent(separator));
		
		
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(title)
			.addGap(20)
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(colsAvailable)
				.addComponent(colsSelected))
			.addGroup(layout.createParallelGroup(Alignment.LEADING)
				.addComponent(availableSP,300, 300, Short.MAX_VALUE)
				.addComponent(selectedSP,300, 300, Short.MAX_VALUE)
				.addGap(20)
				.addGroup(layout.createSequentialGroup()
					.addComponent(selectLbl)
					.addGroup(layout.createParallelGroup(Alignment.BASELINE)
						.addComponent(selectColsBtn)
						.addComponent(removeColsBtn))
					.addGap(140)
					.addComponent(bulkLbl)
					.addComponent(allVisibleBtn)
					.addComponent(allColumnsBtn))
				.addGroup(layout.createSequentialGroup()
					.addComponent(moveLbl)
					.addComponent(moveUpBtn)
					.addComponent(moveDownBtn)
					.addGap(165)
					.addComponent(removeAllBtn)))
			.addComponent(hiddenLbl)
			.addGap(20)
			.addComponent(separator)
			.addGap(20)
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(rowsLbl)
				.addComponent(separatorLbl))
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(exportAllRows)
				.addComponent(separatorComma)
				.addComponent(separatorTab)
				.addComponent(separatorChar)
				.addComponent(separatorTF))
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(exportSelectedRows)
				.addComponent(arraysLbl))
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(arrayAsManyColumnsRadio)
				.addComponent(arrayAsOneColumnRadio))
			.addGroup(layout.createParallelGroup(Alignment.BASELINE)
				.addComponent(cancelBtn)
				.addComponent(continueBtn)));
		
		layout.linkSize(SwingConstants.HORIZONTAL, availableSP,selectedSP, colsAvailable, colsSelected);
		layout.linkSize(SwingConstants.VERTICAL, availableSP,selectedSP);
		layout.linkSize(moveUpBtn, moveDownBtn);
		layout.linkSize(allVisibleBtn, allColumnsBtn, removeAllBtn);
		
		dialog.getRootPane().setDefaultButton(continueBtn);
		dialog.setContentPane(panel);
		dialog.setTitle("Export Data");
		dialog.pack();
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setLocation(stable.getLocationOnScreen());
		dialog.setVisible(true);
		

	}
	
	class ListTransferHandler extends TransferHandler {
	    private int[] indices = null;
	    private int addIndex = -1; //Location where items were added
	    private int addCount = 0;  //Number of items added.
	            
	    /**
	     * We only support importing strings.
	     */
	    public boolean canImport(TransferHandler.TransferSupport info) {
	        // Check for String flavor
	        if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
	            return false;
	        }
	        return true;
	   }

	    /**
	     * Bundle up the selected items in a single list for export.
	     * Each line is separated by a newline.
	     */
	    protected Transferable createTransferable(JComponent c) {
	        JList<String> list = (JList<String>)c;
	        indices = list.getSelectedIndices();
	        Object[] values = list.getSelectedValues();
	        
	        StringBuffer buff = new StringBuffer();

	        for (int i = 0; i < values.length; i++) {
	            Object val = values[i];
	            buff.append(val == null ? "" : val.toString());
	            if (i != values.length - 1) {
	                buff.append("\n");
	            }
	        }
	        
	        return new StringSelection(buff.toString());
	    }
	    
	    /**
	     * We support move actions.
	     */
	    public int getSourceActions(JComponent c) {
	        return TransferHandler.MOVE;
	    }
	    
	    /**
	     * Perform the actual import.  This supports drag and drop.
	     */
	    public boolean importData(TransferHandler.TransferSupport info) {
	        if (!info.isDrop()) {
	            return false;
	        }

	        JList<String> list = (JList<String>)info.getComponent();
	        DefaultListModel listModel = (DefaultListModel)list.getModel();
	        JList.DropLocation dl = (JList.DropLocation)info.getDropLocation();
	        int index = dl.getIndex();
	        boolean insert = dl.isInsert();

	        // Get the string that is being dropped.
	        Transferable t = info.getTransferable();
	        String data;
	        try {
	            data = (String)t.getTransferData(DataFlavor.stringFlavor);
	        } 
	        catch (Exception e) { return false; }
	                                
	        // Wherever there is a newline in the incoming data,
	        // break it into a separate item in the list.
	        String[] values = data.split("\n");
	        
	        addIndex = index;
	        addCount = values.length;
	        
	        // Perform the actual import.  
	        for (int i = 0; i < values.length; i++) {
	            if (insert) {
	                listModel.add(index++, values[i]);
	            } else {
	                // If the items go beyond the end of the current
	                // list, add them in.
	                if (index < listModel.getSize()) {
	                    listModel.set(index++, values[i]);
	                } else {
	                    listModel.add(index++, values[i]);
	                }
	            }
	        }
	        return true;
	    }

	    /**
	     * Remove the items moved from the list.
	     */
	    protected void exportDone(JComponent c, Transferable data, int action) {
	        JList source = (JList)c;
	        DefaultListModel listModel  = (DefaultListModel)source.getModel();

	        if (action == TransferHandler.MOVE) {
	            for (int i = indices.length - 1; i >= 0; i--) {
	            	if (indices[i] > addIndex) {//if moving the item up in the list, we must account for the new items in the list
	            		listModel.remove(indices[i] + indices.length);
	            	} else {
	            		listModel.remove(indices[i]);
	            	}
	            }
	        }
	        
	        indices = null;
	        addCount = 0;
	        addIndex = -1;
	    }
	}
}
