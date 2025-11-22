package edu.asu.jmars.layer.landing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import edu.asu.jmars.Main;
import edu.asu.jmars.swing.ColorCell;
import edu.asu.jmars.swing.ColorCombo;
import edu.asu.jmars.swing.ColorComboCellEditor;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.stable.DoubleCellEditor;
import edu.asu.jmars.util.stable.NumberCellRenderer;
import edu.asu.jmars.util.stable.Sorter;
import org.apache.commons.text.StringEscapeUtils;


public class LandingSiteTable extends STable 
{
	LandingSiteTableModel tableModel;	
    LandingLView landingLView;
    //used for angle and lat/lon columns
	public DecimalFormat decimalFormat = new DecimalFormat("0.#####");
    public NumberCellRenderer numberRenderer = new NumberCellRenderer();
	

	public LandingSiteTable(LandingLView lview)
	{
		super();
	
		landingLView=lview;
		
		numberRenderer.setHorizontalAlignment(JLabel.CENTER);
		setTypeSupport(Double.class, numberRenderer, new DoubleCellEditor(decimalFormat));
				
		setUnsortedTableModel(getTableModel());
     
		ToolTipManager.sharedInstance().registerComponent(this);
		
		addMouseListener(new TableMouseAdapter());

		//Selected rows also selects cirlces on the lview
		getSelectionModel().addListSelectionListener(new ListSelectionListener(){
		
			public void valueChanged(ListSelectionEvent e) {
				int selectedRows[]=getSelectedRows();
				
				Sorter sorter = getSorter();

				ArrayList<LandingSite> selectedSites = new ArrayList<LandingSite>();
				
				for (int row : selectedRows) {
					LandingSite s = getSite(sorter.unsortRow(row));
					selectedSites.add(s);
				}
											
				landingLView.selectedSites.clear();
				landingLView.selectedSites.addAll(selectedSites);
				
				landingLView.drawSelectedSites();
				landingLView.repaint();
				
				((LandingLView)landingLView.getChild()).drawSelectedSites();
				((LandingLView)landingLView.getChild()).repaint();	

				return;
			}
		});

		
	//Creating and Assigning Cell Renderers and Editors	
		//Renderer for the comment/note column
		DefaultTableCellRenderer noteRenderer = new DefaultTableCellRenderer(){			
			JTextArea textValue = new JTextArea();
			JPanel panel = new JPanel();
			
			public Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int row, int column) {				
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				textValue.setText((String)value);
				//change background/foreground when row is selected
				if(isSelected){
					textValue.setBackground(table.getSelectionBackground());
					textValue.setForeground(table.getSelectionForeground());
				}else{
					textValue.setBackground(table.getBackground());
					textValue.setForeground(table.getForeground());
				}
				
				textValue.setWrapStyleWord(true);
				textValue.setLineWrap(true);				
				panel.add(textValue);
				return panel;
			}
		};
		//get the note column
		TableColumn noteColumn = this.getColumnModel().getColumn(LandingSiteTableModel.COMMENT_COLUMN);
		noteColumn.setCellEditor(new NoteCellEditor());
		noteColumn.setCellRenderer(new TextAreaCell());
		noteColumn.setPreferredWidth(150);
		
        //get color column
		TableColumn colorColumn = this.getColumnModel().getColumn(LandingSiteTableModel.COLOR_COLUMN);
		//set color column renderer and editor
		colorColumn.setCellEditor(new ColorComboCellEditor()); //see class in swing package
     	colorColumn.setCellRenderer(new ColorCell());   	colorColumn.setPreferredWidth(250);
		
     	
     	//Renderer for the axes columns
     	DefaultTableCellRenderer axesRenderer = new DefaultTableCellRenderer() {
     		String label = "";
     		
     		public Component getTableCellRendererComponent(JTable table, Object value,
     				boolean isSelected, boolean hasFocus, int row, int column) {
     			
     			//change background to blue when row is selected
     			if(isSelected){
     				this.setBackground(table.getSelectionBackground());
     				this.setForeground(table.getSelectionForeground());
     			}else{     				
     				this.setForeground(table.getForeground());
     			}
     			//format value
     			if (value instanceof String) {
     				label = ""+value;
     				this.setValue(label);
     				return this;
     			}
     			double val = (Double)value;
     			if (val>500) {
     				label = decimalFormat.format(val / 1000.0) + " km";
     			} else {
     				label = decimalFormat.format(val) + " m";
     			}
     			this.setValue(label);
     			
     			return this;
     		}
     	};
		axesRenderer.setHorizontalAlignment(JLabel.CENTER);
		//get the axes columms
		TableColumn col1 = getColumnModel().getColumn(LandingSiteTableModel.HORAXIS_COLUMN);
		TableColumn col2 = getColumnModel().getColumn(LandingSiteTableModel.VERAXIS_COLUMN);
		col1.setCellRenderer(axesRenderer); //add renderer
		col2.setCellRenderer(axesRenderer); //add renderer
		
		
		
		//Renderer for the stats columns
		MultiLineCellRenderer statsRenderer = new MultiLineCellRenderer();
		statsRenderer.setHorizontalAlignment(JLabel.CENTER);

		//get stats columns dynamically
		int index = 7; //Stats start at column 7
		for(StatCalculator sc : lview.getStatCalculators()){
			TableColumn col = getColumnModel().getColumn(index++);
			col.setCellRenderer(statsRenderer);
			col.setPreferredWidth(250);
		}	
		
	    //set default behavior on the table		
		setColumnSelectionAllowed(false);
	}
	
    public LandingSiteTableModel getTableModel() {
    	if (tableModel==null) {
    		tableModel=new LandingSiteTableModel(landingLView);
    	}
    	return tableModel;
    }
	
	/**
	 ** returns the site that corresponds to the specified row.
	 ** returns null if the site cannot be found.
	 **
	 ** @param row - the table row whose stamp is to be fetched.
	 **/
	public LandingSite getSite(int row){
		LandingSiteTableModel model = (LandingSiteTableModel)getUnsortedTableModel();

		return (LandingSite)model.getValueAt(row);				
	}
		
	public void selectRows(List<LandingSite> sitesToSelect) {
		LandingSiteTableModel model = (LandingSiteTableModel)getUnsortedTableModel();

		clearSelection();
		
		for (LandingSite site : sitesToSelect) {
			int row = model.getRow(site);
			if (row<0) continue;
			row=getSorter().sortRow(row);
			addRowSelectionInterval(row, row);			
		}		
	}
	
	public int getRow(LandingSite site){
		LandingSiteTableModel model = (LandingSiteTableModel)getUnsortedTableModel();

		int row=model.getRow(site);
		row=getSorter().sortRow(row);

		return row;
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
		} else if (getColumnClass(col) == ColorCombo.class) {
			ColorCombo cc = (ColorCombo)value;
			value = " RGB: " + cc.getColor().getRed() + " " + cc.getColor().getGreen()  + " " + cc.getColor().getBlue(); 
		} else if (value instanceof Stat){
			return value.toString(); //Stat Class overrides toString()
		}

		return  name + " = " + value;
	}
		
	protected class TableMouseAdapter extends MouseAdapter {
		public void mousePressed(MouseEvent e){
			  	//return if not a right click, return
				if (!SwingUtilities.isRightMouseButton(e)){
					return;
				} 

				JPopupMenu menu = new JPopupMenu();
				
				AbstractAction centerAct = new AbstractAction("Center on selected site") {
					public void actionPerformed(ActionEvent e) {
						LandingSite site = landingLView.selectedSites.get(0);
						
						//Get the spatial center from the selected site.
						// Note: we have to convert from degrees E (displayed in JMARS) to 
						// degress W (underlying coord system used in JMARS)
						Point2D spatialCenter = new Point2D.Double(360-site.getLon(), site.getLat());
						//Convert to world point to pass to the location manager
						Point2D worldCenter = Main.PO.convSpatialToWorld(spatialCenter);
						
						Main.testDriver.mainWindow.getLocationManager().setLocation(worldCenter, true);
					}
				};
				JMenuItem centerOnSite = new JMenuItem(centerAct);
				menu.add(centerOnSite);			
				
				
				JMenuItem removeSites = new JMenuItem("Remove selected sites");
				removeSites.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						landingLView.deleteSelectedSite();

						getSorter().clearSorts();
						landingLView.repaint();
						
						((LandingLView)landingLView.getChild()).drawSelectedSites();
						((LandingLView)landingLView.getChild()).repaint();
					}
				});
				menu.add(removeSites);

				JMenu findStamps = new JMenu("Find overlapping stamps");
				findStamps = landingLView.populateFindIntersectingStamps(findStamps);
				menu.add(findStamps);
				
				//If no site is selected, disable the menu options
				if(landingLView.selectedSites.size()<1){
					centerOnSite.setEnabled(false);
					removeSites.setEnabled(false);
					findStamps.setEnabled(false);
				}else{
					centerOnSite.setEnabled(true);
					removeSites.setEnabled(true);
					findStamps.setEnabled(true);
				}
				
				//if more than one site is selected disable the center option
				if(landingLView.selectedSites.size()>1){
					centerOnSite.setEnabled(false);
				}

				menu.show(e.getComponent(), e.getX(), e.getY());
				
			}
		
		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isLeftMouseButton(e)){
			} 
		}
	};
	
	private class MultiLineCellRenderer extends DefaultTableCellRenderer {
		
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) 
		{
			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);			
			
			if(value == null){
				setValue("Please refresh");
			}			
			else if(value instanceof Stat){				
				setValue(value.toString()); //Stat Class overrides toString()
			}
			int height = c.getPreferredSize().height;
			if (table.getRowHeight(row) < height)
				table.setRowHeight(row, height);
			
			return c;
		}

		@Override
		protected void setValue(Object value) {
			if (value instanceof String) {
				String sVal = (String) value;
				if (sVal.indexOf('\n') >= 0 &&
						!(sVal.startsWith("<html>") && sVal.endsWith("</html>")))
					value = "<html><nobr>" + StringEscapeUtils.escapeHtml4(sVal) + "</nobr></html>";
			}
			super.setValue(value);
		}		
	}	
 }


class NoteCellEditor extends AbstractCellEditor implements TableCellEditor{
	JTextArea textValue = new JTextArea();
	
	public NoteCellEditor(){
		textValue.setLineWrap(true);
		textValue.setWrapStyleWord(true);
	}
	
	public Object getCellEditorValue() {
		return textValue;
	}

	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		
		//let the user select the row by clicking in this column, once the row
		// is selected, let the user edit this column if they click again.
		if(!isSelected){
			return null;
		}
		
		textValue.setForeground(table.getSelectionForeground());
		textValue.setBackground(table.getSelectionBackground());


		textValue.setText(value.toString());
		textValue.setEditable(false);
		
		
		JLabel prompt = new JLabel("Please enter note for this landing site:");
		JTextArea input = new JTextArea();
		input.setRows(6);
		input.setColumns(30);
		input.setBorder(new LineBorder(Color.GRAY));
		input.setWrapStyleWord(true);
		input.setLineWrap(true);
		input.setText(value.toString());
		JPanel center = new JPanel(new BorderLayout());
		center.add(Box.createVerticalStrut(5), BorderLayout.NORTH);
		center.add(input, BorderLayout.CENTER);
		JPanel dialogPnl = new JPanel(new BorderLayout());
		dialogPnl.add(prompt, BorderLayout.NORTH);
		dialogPnl.add(center, BorderLayout.CENTER);	
		JScrollPane sp = new JScrollPane(dialogPnl);

		int ok = Util.showConfirmDialog(sp, "Enter Note", JOptionPane.OK_CANCEL_OPTION);
		
		
		if(ok == JOptionPane.OK_OPTION){
			textValue.setText(input.getText());
			table.getModel().setValueAt(textValue, row, column);
			return textValue;
		}
		
		return textValue;
	}
	
}




class TextAreaCell extends JTextArea implements TableCellRenderer{
		
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		

		setText(value.toString());
		//change background/foreground when row is selected
		if(isSelected){
			setBackground(table.getSelectionBackground());
			setForeground(table.getSelectionForeground());
		}else{
			setBackground(table.getBackground());
			setForeground(table.getForeground());
		}
		
		setWrapStyleWord(true);
		setLineWrap(true);
		setEditable(false);
		
		
		return this;
	}	
}

