package edu.asu.jmars.layer.stamp.focus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import org.json.JSONArray;
import org.json.JSONObject;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.StampMenu;
import edu.asu.jmars.layer.stamp.StampShape;
import edu.asu.jmars.layer.stamp.StampLayer.StampSelectionListener;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.swing.TableColumnAdjuster;
import edu.asu.jmars.swing.ValidClipboard;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.stable.FilteringColumnModel;
import edu.asu.jmars.util.stable.Sorter;

/**
 **  The unfilled stamp table.
 **
 **  This table allows for sorting (either increasing or decreasing) on any columns in
 **  the table.  There may be a primary sort or a primary/secondary sort.
 **   
 **  Right-clicking on a row in the table allows users to render the stamp corresponding
 **  to the table row or browse the stamp's webpage. Left-clicking selects a row and highlights
 **  the stamps outline in the viewing windows.  A double left-click lightlights the outline and 
 **  centers the viewing windows about the stamp.
 **/
public class StampTable extends STable implements StampSelectionListener
{
	public void selectionsChanged() {
        List<StampShape> selectedStamps = myLView.stampLayer.getSelectedStamps();
        
        clearSelection();
        
    	toggleSelectedStamps(selectedStamps);
	}
	
	public void selectionsAdded(java.util.List<StampShape> newStamps) {
		setAutoscrolls(false);

		for (StampShape newStamp : newStamps) {
			int row = getRow(newStamp);
			if (row<0) continue;  // it's possible that this stamp isn't currently displayed
			row = getSorter().sortRow(row);
			
			if(isRowSelected(row)) {
				continue;
			} else {
				// Toggle the row
				changeSelection(row, 0, true, false);
			}
		}
		
		setAutoscrolls(true);
	}
	
	StampLView myLView;

	StampTableModel tableModel;
    
    public StampTableModel getTableModel() {
    	if (tableModel==null) {
    		tableModel=new StampTableModel();
    	}
    	return tableModel;
    }

	
	public StampTable(StampLView newView)
	{
		super(newView.stampLayer.getInstrument());
				
		myLView = newView;
		
		setUnsortedTableModel(getTableModel());
		
		ToolTipManager.sharedInstance().registerComponent( this );
		
		setAutoResizeMode(STable.AUTO_RESIZE_OFF);
		setColumnSelectionAllowed(false);
		setPreferredScrollableViewportSize(new Dimension(getPreferredSize().width,400));

	    TableMouseAdapter tma = new TableMouseAdapter();
		addMouseListener(tma);
		addKeyListener(new TableKeyListener());

		myLView.stampLayer.addSelectionListener(this);
		
		setDefaultRenderer(Double.class, new NumberRenderer());
		
		setDefaultRenderer(Double[].class, new NumberArrayRenderer());
		setDefaultRenderer(Float[].class, new NumberArrayRenderer());
		setDefaultRenderer(double[].class, new NumberArrayRenderer());
		setDefaultRenderer(float[].class, new NumberArrayRenderer());

		setDefaultRenderer(Color.class, new ColorRenderer());
		
		//Material - need "custom" renderer for "other" columns;
		//otherwise, we get missing columns in highlight
		setDefaultRenderer(Object.class, new ObjectRenderer());
		
//		this.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		getTableHeader();
	}
	
	
	/**
	 ** returns the stamp that corresponds to the specified row.
	 ** returns null if the stamp cannot be found.
	 **
	 ** @param row - the table row whose stamp is to be fetched.
	 **/
	public StampShape getStamp(int row){
		StampTableModel model = (StampTableModel)getUnsortedTableModel();

		return (StampShape)model.getValueAt(row);				
	}
		
	public int getRow( StampShape stamp){
		StampTableModel model = (StampTableModel)getUnsortedTableModel();
		
		return model.getRow(stamp);
	}
		
	
    public void updateData(Class[] newTypes, String[] newNames, String[] initialCols, boolean updateCols) {
    	StampTableModel model = getTableModel();
    	
    	boolean firstUpdate = model.isFirstUpdate() || updateCols;
    	
    	model.updateData(newTypes, newNames);
		
		if (firstUpdate) {
			// first time through, set visibility of columns
			model.fireTableStructureChanged();
			FilteringColumnModel colModel = (FilteringColumnModel) getColumnModel();
			Set<String> displayCols = new HashSet<String>(Arrays.asList(initialCols));
			for (TableColumn tc: new ArrayList<TableColumn>(colModel.getAllColumns())) {
				colModel.setVisible(tc, displayCols.contains(tc.getIdentifier().toString()));
			}
			colModel.setDefaultColumns(initialCols);			
		} else {
			// all other updates result in a table change only
			model.fireTableDataChanged();
		}        	
		
		TableColumnAdjuster tca = new TableColumnAdjuster(this);
		tca.adjustColumns();
    }
	
	/**
	 ** specifies the tooltip to be displayed whenever the cursor
	 ** halts over a cell.  This is called because the table's panel
	 ** is registered with the tooltip manager in the constructor.
	 **/
	public String getToolTipText(MouseEvent e){
		Point p = e.getPoint();
		int col = columnAtPoint(p);
		int row = rowAtPoint(p);
		if (col == -1  ||  row == -1) {
			return  null;
		}
		String name = getColumnName(col);
		Object value = getValueAt(row, col);
		if (value == null) {
			value = "-NULL-";
		} else if (value.getClass().isArray()) {
			int width = Config.get("stable.tooltip.width",100);
	    	
	    	String outStr="<html>" + name + " = " + Util.formatNumericArray(value) + "</html>";
	    	
	    	outStr = Util.foldText(outStr, width, "<br>");

	    	return outStr;
		}
		
		return  name + " = " + value;
	}
	

	/**
	 ** The cell renderer for time columns. This is set up in the
	 ** constructor.
	 **/
	class TimeRenderer extends DefaultTableCellRenderer 
    {
		DecimalFormat formatter = new DecimalFormat("#");
        
		public TimeRenderer() { 
			super(); 
		}
        
		public void setValue(Object value) {
			setHorizontalAlignment(JLabel.RIGHT);
			if ( value == null || ((Double)value).isNaN() ) {
				setText( "NaN");
			} else {
				setText( formatter.format(value));
			}
		}
	}
		
	class NumberRenderer extends DefaultTableCellRenderer {
		NumberFormat nf = NumberFormat.getNumberInstance();
		
	    public NumberRenderer() { 
	    	super();
	    	nf.setMaximumFractionDigits(8);
	    }

	    public void setValue(Object value) {
	    	/*
	    	 * The data from ReadoutTableModel for which NumberRenderer is used in conjunction with
	    	 * contains NaNs for the MapSources for which we haven't received any data as yet.
	    	 */
	        setText((value == null) ? "" : (Double.isNaN(((Number)value).doubleValue())? "NaN": nf.format(value)));
	    }  
	}

	class NumberArrayRenderer extends DefaultTableCellRenderer {
		NumberFormat nf = NumberFormat.getNumberInstance();
		
	    public NumberArrayRenderer() { 
	    	super();
	    	nf.setMaximumFractionDigits(8);
	    }

	    public void setValue(Object value) {
	    	String outStr="";
	    	if (value.getClass().isArray()) {
	    		outStr = Util.formatNumericArray(value);
	    	}
	        setText(outStr);
	    }  
	}
	
	class ColorRenderer extends DefaultTableCellRenderer {		
	    @Override
		public void setForeground(Color c) {			
			super.setForeground(c);
		}

		@Override
		public void setBackground(Color c) {			
			super.setBackground(myColor);
		}

		Color myColor;
		
		public ColorRenderer() { 
	    	super();
	    }

	    public void setValue(Object value) {
	    	Color color = (Color) value;
	    	myColor=color;
	    	setForeground(color);
	    	setBackground(color);
	    }  
	}

	class ObjectRenderer extends DefaultTableCellRenderer {
		
	    public ObjectRenderer() { 
	    	super();	    	
	    }
	    
	    @Override
	    public void setValue(Object value) {
	    	Object str = value;
	    	super.setValue(str);
	    }  
	}
	
	
	/**
	 ** updates the stamp table with contents of the stamp array. The 
	 ** length and order of the rows may not be the same after the update,
	 ** but this method maintains the selected rows.
	 **/
	void dataRefreshed() 
	{
		// don't do any of this if we are dealing with the panner view.
		if (myLView.getChild()==null){
			return;
		}

		// rebuild the table.
		final StampTableModel model = (StampTableModel)getUnsortedTableModel();

		final List<Vector> newRows = new ArrayList<Vector>();

		int numCols = model.getColumnCount();
		
		StampShape stamps[]=new StampShape[0];
		
		if (myLView.getSettings().limitTableToMainView) {
			stamps = myLView.stamps;
		} else {
			ArrayList<StampShape> allStamps = myLView.stampLayer.getVisibleStamps();
			stamps = allStamps.toArray(new StampShape[allStamps.size()]);
		}
	
		if (stamps == null) {
			stamps = new StampShape[0];
		}
		
		for (StampShape stampShape : stamps) {
			Object data[] = stampShape.getStamp().getData();
			Vector v = new Vector(numCols + 1);			        
			for (int i=0; i < data.length; i++) {
				v.addElement(data[i]);
			}			        
			v.addElement(stampShape);										
			newRows.add(v);
		}

		Runnable updateTable = new Runnable() {
			public void run() {
				model.removeAll();
				model.addRows(newRows);
				
				List<StampShape> selectedStamps = myLView.stampLayer.getSelectedStamps();

				// reselect any stamps that were selected before.
				if (selectedStamps != null){
					// Improve re-selection performance by temporarily disabling
					// the auto-scrolling and list selection changes behavior.
					setAutoscrolls(false);

					for (StampShape stamp : selectedStamps) {
						int row = getRow( stamp );
						if (row!=-1) {  
							row=getSorter().sortRow(row);
							if (row != -1){
								getSelectionModel().addSelectionInterval(row, row);
							}
						}
					}

					setAutoscrolls(true);
				}
				
				newRows.clear();
				
				TableColumnAdjuster tca = new TableColumnAdjuster(StampTable.this);
				tca.adjustColumns();
			}
		};

		try {
			if (SwingUtilities.isEventDispatchThread()) {
				updateTable.run();
			} else {
				SwingUtilities.invokeAndWait(updateTable);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 ** Ensure the row passed is made visible
	 **/
	void makeRowVisible(int row) {
		setAutoscrolls(true);
		
		// Scroll to make the toggled row visible for the last stamp in the list.
		if (row >= 0) {
			Rectangle r = getCellRect(row, 0, false).union(
								       getCellRect(row, getColumnCount()-1, false));
			int extra = Math.min(r.height * 3, getHeight() / 4);
			r.y -= extra;
			r.height += 2 * extra;
			scrollRectToVisible(r);
		}					
	}

	/**
	 ** Called when the view has a selection status toggled, updates
	 ** the selection list (which cascades to update the view through
	 ** {@link #setSelectedStamps}).
	 **/
	void toggleSelectedStamps(List<StampShape> toggledStamps) {
		if (toggledStamps.size()<1)
			return;
		
		setAutoscrolls(false);
		
		int row = -1;
		
		for (StampShape toggledStamp : toggledStamps) {					
			row = getRow(toggledStamp);
			if (row == -1) {
				continue;
			}
			row = getSorter().sortRow(row);
			
			// Toggle the row
			changeSelection(row, 0, true, false);
		}
		
		makeRowVisible(row);			
	}			
	
	// listens for mouse events in the unfilled stamp table.  If a single left-click,
	// the stamp outline in the viewer is hilighted.  If a double left-click, the stamp
	// is hilighted AND the viewers center about the stamp.  If a right-click, the 
	// render/browse popup is brought forth.
	protected class TableMouseAdapter extends MouseAdapter {
		
		// TODO: Mouse drag in the table doesn't select entries.  Probably should.
		public void mousePressed(MouseEvent e){
			synchronized (StampTable.this) {
				// get the indexes of the sorted rows. 
				final int[] rowSelections = getSelectedRows();
				if (rowSelections==null || rowSelections.length==0) {
					// Ctrl-clicking the last row will result in no row selections.  Clear the selections here.
					myLView.stampLayer.clearSelectedStamps();
					return;
				}
				
				// if this was a left click, pan to the selected stamp.
				if (SwingUtilities.isLeftMouseButton(e)){
					if (e.getClickCount() > 1) {
						int row = rowSelections[0];
						StampShape s = getStamp(getSorter().unsortRow(row));
						
						if (s != null){
							myLView.panToStamp( s);
						}
						e.consume();
						return;
					} else {
						Sorter sorter = getSorter();
						
						int row = rowAtPoint(e.getPoint());
						int col = columnAtPoint(e.getPoint());
						
						StampShape s = getStamp(sorter.unsortRow(row));
												
						if (e.isShiftDown()) {
							int lastClicked = startSelectionIndex;
							if (lastClicked<0) {
								// We need to use the sorted row (displayed to the user) index
								// so we get the range of rows the user sees as sequential
								lastClicked=startSelectionIndex=row;
							}
							int start = lastClicked < row ? lastClicked : row;
							int end = lastClicked > row ? lastClicked : row;
							myLView.stampLayer.clearSelectedStamps();
							ArrayList<StampShape> selectedStamps=new ArrayList<StampShape>();
							for (int i=start; i<=end; i++) {
								selectedStamps.add(getStamp(sorter.unsortRow(i)));
							}
							
							myLView.stampLayer.addSelectedStamps(selectedStamps);
						} else if (e.isControlDown()) {
							myLView.stampLayer.toggleSelectedStamp(s);
						} else {
							myLView.stampLayer.clearSelectedStamps();
							myLView.stampLayer.addSelectedStamp(s);
						}																			
					}
					return;
				} 

				// if this was a right click, bring up the render/browse popup
				final JPopupMenu menu = new JPopupMenu();

				
				if (myLView.stampLayer.enableRender() || myLView.stampLayer.enableWeb()) {
					if (rowSelections.length == 1) {
						final int row = getSorter().unsortRow(rowSelections[0]);
						if (row < 0) {
							return;
						}
		
						StampShape stamp = getStamp(row);
	
						StampMenu stampMenu = new StampMenu(myLView.stampLayer, myLView.getFocusPanel().getRenderedView(), stamp);
						menu.add(stampMenu);
					} else {					
						if (myLView.stampLayer.enableRender()) {
							// Only makes sense to offer a 'Render all selected' if you can render.
							StampMenu stampMenu = new StampMenu(myLView.stampLayer, myLView.getFocusPanel().getRenderedView());
							menu.add(stampMenu);
						}
					}
				}
				
				// Add copy-selected-stamp-IDs menu item.
				if (rowSelections != null && rowSelections.length > 0) {
					JMenuItem copyToClipBoard = new JMenuItem("Copy Selected Stamp List to Clipboard");
					copyToClipBoard.addActionListener(new ActionListener() {						
						public void actionPerformed(ActionEvent e) {
							StringBuffer buf = new StringBuffer();
							for (int i = 0; i < rowSelections.length; i++) {
								buf.append( getStamp(getSorter().unsortRow(rowSelections[i])).getId() );
								buf.append(' ');
							}
				           
							StringSelection sel = new StringSelection(buf.toString());
							Clipboard clipboard = ValidClipboard.getValidClipboard();
							if (clipboard == null) {
								Main.setStatus("no clipboard available");
							}
							else {
								clipboard.setContents(sel, sel);
								Main.setStatus("Stamp list copied to clipboard");
							}
						}						
					});
					menu.add(copyToClipBoard);
					
					//spectra data has extra options: mark, lock, unlock, hide, show
					if (myLView.stampLayer.spectraData()) {
						JMenuItem markSelectedRecords = new JMenuItem("Mark Records for Spectra Plot");
						markSelectedRecords.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								for(int row : rowSelections){
									StampShape stamp = getStamp(getSorter().unsortRow(row));
									SpectraView sv = ((StampFocusPanel)myLView.focusPanel).spectraView;
									sv.addMarkedStamp(stamp);
								}
							}
						});
						menu.add(markSelectedRecords);
						
						
						JMenuItem lockSelectedRecords = new JMenuItem("Lock Selected Records");
						lockSelectedRecords.addActionListener(new ActionListener() {						
							public void actionPerformed(ActionEvent e) {
								int cnt = 0;
								
								for (int i = 0; i < rowSelections.length; i++) {
									StampShape stamp = getStamp(getSorter().unsortRow(rowSelections[i]));
									if (!stamp.isLocked()) {
										stamp.setLocked(true);
										cnt++;
									}
								}
								Main.setStatus("Added " + cnt+ " stamp records to the lock list " +cnt);
								StampTable.this.repaint();
							}						
						});
						menu.add(lockSelectedRecords);						
						
						JMenuItem unlockSelectedRecords = new JMenuItem("Unlock Selected Records");
						unlockSelectedRecords.addActionListener(new ActionListener() {						
							public void actionPerformed(ActionEvent e) {
								int cnt = 0;
								
								for (int i = 0; i < rowSelections.length; i++) {
									StampShape stamp = getStamp(getSorter().unsortRow(rowSelections[i]));
									if (stamp.isLocked()) {
										stamp.setLocked(false);
										cnt++;
									}
								}
								Main.setStatus("Removed " + cnt+ " stamp records from the lock list " +cnt);
								StampTable.this.repaint();
							}						
						});
						menu.add(unlockSelectedRecords);						

						JMenuItem hideSelectedRecords = new JMenuItem("Hide Selected Records");
						hideSelectedRecords.addActionListener(new ActionListener() {						
							public void actionPerformed(ActionEvent e) {
								int cnt = 0;
								
								for (int i = 0; i < rowSelections.length; i++) {
									StampShape stamp = getStamp(getSorter().unsortRow(rowSelections[i]));
									if (!stamp.isHidden()) {
										stamp.setHidden(true);
										cnt++;
									}
								}
								Main.setStatus("Added " + cnt+ " stamp records to the hide list " +cnt);
								StampTable.this.repaint();
								
								myLView.updateSelections();
							}						
						});
						menu.add(hideSelectedRecords);						
						
						JMenuItem showSelectedRecords = new JMenuItem("Show Selected Records");
						showSelectedRecords.addActionListener(new ActionListener() {						
							public void actionPerformed(ActionEvent e) {
								int cnt = 0;
								
								for (int i = 0; i < rowSelections.length; i++) {
									StampShape stamp = getStamp(getSorter().unsortRow(rowSelections[i]));
									if (stamp.isHidden()) {
										stamp.setHidden(false);
										cnt++;
									}
								}
								Main.setStatus("Removed " + cnt+ " stamp records from the hide list " +cnt);
								StampTable.this.repaint();
								
								myLView.updateSelections();
							}						
						});
						menu.add(showSelectedRecords);						

						boolean allLocked = true;
						boolean allUnlocked = true;
						boolean allHidden = true;
						boolean allShown = true;
						
						for (int i = 0; i < rowSelections.length; i++) {
							StampShape stamp = getStamp(getSorter().unsortRow(rowSelections[i]));
							if (!stamp.isLocked()) {
								allLocked=false;
							}
							if (stamp.isLocked()) {
								allUnlocked=false;
							}
							if (stamp.isHidden()) {
								allShown=false;
							}
							if (!stamp.isHidden()) {
								allHidden=false;
							}
						}
						
						lockSelectedRecords.setEnabled(!allLocked);
						unlockSelectedRecords.setEnabled(!allUnlocked);
						hideSelectedRecords.setEnabled(!allHidden);
						showSelectedRecords.setEnabled(!allShown);
					}
					
					
					if (myLView.stampLayer.getInstrument().equalsIgnoreCase("Davinci")) {
						JMenuItem persistAsCustom = new JMenuItem("Persist as Custom Stamp");
						persistAsCustom.addActionListener(new ActionListener() {						
							public void actionPerformed(ActionEvent e) {
								if (Main.USER==null || Main.USER.length()==0) {
									Util.showMessageDialog(
											"Sorry - you must be logged into JMARS to use this feature.",
											"Login Required",
											JOptionPane.INFORMATION_MESSAGE
									);
									
									return;
								}
								
								String startName = "<Enter Set Name>";
								
								String setName=(String)Util.showInputDialog("Set:", "Persist to set:", 
										JOptionPane.QUESTION_MESSAGE, null, null, startName);
								
								if (setName.equalsIgnoreCase(startName)) {
									setName="";
								}
								
								try {
									for (StampShape s : myLView.stampLayer.getSelectedStamps()) {												
										String urlStr = "CustomStampUploader?";
									
										JSONArray json = fieldstoJSON(s, setName);
										urlStr+="&data="+json.toString();
//												System.out.println(urlStr.replaceAll("password=[^&]*", "password=xxxxxx"));

										// TODO: Add some safety/ error checking
										Object[] data=s.getStamp().getData();
										
										File f = new File((String)data[2]);
										
										StampLayer.postToServer(urlStr, f);
									}
								} catch (Exception e2) {
									e2.printStackTrace();
								}
							}						
						});
						menu.add(persistAsCustom);								
					}
					if (myLView.stampLayer.getInstrument().equalsIgnoreCase("Custom")) {
						JMenuItem deleteCustom = new JMenuItem("Delete Custom Stamp");
						deleteCustom.addActionListener(new ActionListener() {						
							public void actionPerformed(ActionEvent e) {
								if (Main.USER==null || Main.USER.length()==0) {
									Util.showMessageDialog(
											"Sorry - you must be logged into JMARS to use this feature.",
											"Login Required",
											JOptionPane.INFORMATION_MESSAGE
									);
									
									return;
								}
								
								int choice=Util.showConfirmDialog("Permanently remove this image from your saved custom stamps?", 
										"Delete Custom Stamp", JOptionPane.YES_NO_OPTION);
											
								if (choice==JOptionPane.NO_OPTION) return;
								
								try {
									for (StampShape s : myLView.stampLayer.getSelectedStamps()) {												
										String urlStr = "CustomStampDeleter?";
										
										urlStr+="&id="+s.getId();
										StampLayer.queryServer(urlStr);
									}
									// Requery for the list of stamps?
									myLView.stampLayer.setQuery(myLView.stampLayer.getQuery());
								} catch (Exception e2) {
									e2.printStackTrace();
								}
							}						
						});
						menu.add(deleteCustom);								
					}

				}

				menu.show(e.getComponent(), e.getX(), e.getY());
			} 
		}// end: mousePressed()				
	};// end: class TableMouseAdapter

	
	public JSONArray fieldstoJSON(StampShape s, String setName) {
		JSONArray json = new JSONArray();
		
		JSONObject j = new JSONObject();

		try {
			j.put("user_name", Main.USER);
			j.put("body", Config.get(Util.getProductBodyPrefix() + "bodyname", "Mars"));//@since change bodies

			Object[] data=s.getStamp().getData();
			j.put("stamp_id", data[0]);
			j.put("stamp_name", data[1]);
			
			double pts[] = s.getStamp().getPoints();
			
			j.put("lon0", pts[0]);
			j.put("lat0", pts[1]);
			j.put("lon1", pts[2]);
			j.put("lat1", pts[3]);
			j.put("lon2", pts[4]);
			j.put("lat2", pts[5]);
			j.put("lon3", pts[6]);
			j.put("lat3", pts[7]);
			
			if (setName!=null && setName.length()>0)
				j.put("set", setName);
			
			json.put(j);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return json;
	}

	
	int startSelectionIndex=-1;
	
	protected class TableKeyListener extends KeyAdapter {

		public void keyReleased(KeyEvent e) {
			if (e.isShiftDown()==false) {
				startSelectionIndex=-1;
			}
		}

		public void keyPressed(KeyEvent e) {
			e.consume();
			
			int lastClicked = getSelectionModel().getLeadSelectionIndex();
			
			if (e.isShiftDown()) {
				if (startSelectionIndex==-1) {
					startSelectionIndex=lastClicked;
				}
			}
			
			int newRow;
			if (e.getKeyCode()==KeyEvent.VK_UP) {
				newRow=lastClicked-1;
				makeRowVisible(newRow);
			} else if (e.getKeyCode()==KeyEvent.VK_DOWN) {
				newRow=lastClicked+1;
				makeRowVisible(newRow);
			} else {
				return; 
			}
			
			if (e.isShiftDown()) {
				if ((e.getKeyCode()==KeyEvent.VK_UP && newRow>=startSelectionIndex)
						|| (e.getKeyCode()==KeyEvent.VK_DOWN && newRow<=startSelectionIndex))
			{
					StampShape s = getStamp(getSorter().unsortRow(lastClicked));
					myLView.stampLayer.toggleSelectedStamp(s);
					return;
			}
			}
			
			if (newRow>=getRowCount() || newRow<0) { 
				return;
			}
			
			StampShape s = getStamp(getSorter().unsortRow(newRow));

			if (e.isShiftDown()) {
				myLView.stampLayer.toggleSelectedStamp(s);
			} else {
				myLView.stampLayer.clearSelectedStamps();
				myLView.stampLayer.toggleSelectedStamp(s);
			}
		}
	}

	
}

