package edu.asu.jmars.layer.stamp.radar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.focus.StampFocusPanel;
import edu.asu.jmars.swing.ColorCombo;
import edu.asu.jmars.swing.TableColumnAdjuster;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;

public class HorizonPanel extends JPanel{
	private StampLView myLView;
	private HorizonTable horizonTbl;
	private JScrollPane tblSp;
	private JButton delBtn;
	private JLabel nameLbl;
	private JTextField nameTf;
	private JLabel colorLbl;
	private ColorCombo colorBx;
	private JLabel fullResLbl;
	private JComboBox<Integer> fullResBx;
	private JLabel browseLbl;
	private JComboBox<Integer> browseBx;
	private JLabel lviewLbl;
	private JComboBox<Integer> lviewBx;
	private JLabel notesLbl;
	private JTextArea notesTa;
	private JScrollPane notesSp;
	private JButton updateBtn;
	private Integer[] widthOptions = {1,2,3,4,5,6,7,8};
	private JPanel widthPnl;
	private JPanel editPnl;
	private JTable diffTbl;
	private JScrollPane diffSp;
	private JButton diffBtn;
	private JButton diffDelBtn;
	private JPanel diffPnl;
	private JPanel dBtnPnl;
	private JButton csvBtn;
	
	private int index = -1;
	private Dimension tableDim = new Dimension(300,150);
	private Dimension diffTblDim = new Dimension(180,80);
	
	private boolean enableDiff;
	
	public HorizonPanel(StampLView lview, boolean enableDiffPanel){
		myLView = lview;
		
		enableDiff = enableDiffPanel;

		buildLayout();
	}
	
	private void buildLayout(){		
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBorder(new EmptyBorder(5, 5, 5, 5));
		
		//Horizon table
		horizonTbl = loadTable(new ArrayList<RadarHorizon>());
		tblSp = new JScrollPane(horizonTbl);
		tblSp.setMinimumSize(tableDim);
		tblSp.setPreferredSize(tableDim);
		
		
		//Edit panel
		nameLbl = new JLabel("Name:");
		nameTf = new JTextField(10);
		colorLbl = new JLabel("Color:");
		colorBx = new ColorCombo(Color.RED); //Start with red because that is the default for horizons
		colorBx.setPreferredSize(new Dimension(80,20));
		colorBx.setMinimumSize(new Dimension(80,20));
		
		widthPnl = new JPanel();		
		widthPnl.setBorder(new TitledBorder("Width Settings"));
		Dimension widthBxSize = new Dimension(40,20);
		fullResLbl = new JLabel("Full Res:");
		fullResBx = new JComboBox<Integer>(widthOptions);
		fullResBx.setPreferredSize(widthBxSize);
		fullResBx.setMinimumSize(widthBxSize);
		browseLbl = new JLabel("Browse:");
		browseBx = new JComboBox<Integer>(widthOptions);
		browseBx.setPreferredSize(widthBxSize);
		browseBx.setMinimumSize(widthBxSize);
		lviewLbl = new JLabel("LView:");
		lviewBx = new JComboBox<Integer>(widthOptions);
		lviewBx.setPreferredSize(widthBxSize);
		lviewBx.setMinimumSize(widthBxSize);
		widthPnl.add(fullResLbl);
		widthPnl.add(fullResBx);
		widthPnl.add(browseLbl);
		widthPnl.add(browseBx);
		widthPnl.add(lviewLbl);
		widthPnl.add(lviewBx);
		
		notesLbl = new JLabel("Notes:");
		notesTa = new JTextArea();
		notesTa.setLineWrap(true);
		notesTa.setWrapStyleWord(true);
		notesSp = new JScrollPane(notesTa, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		Dimension noteSize = new Dimension(250, 80);
		notesSp.setPreferredSize(noteSize);
		notesSp.setMinimumSize(noteSize);
		
		diffPnl = new JPanel();
		diffPnl.setBorder(new TitledBorder("Difference Manager"));
		diffPnl.setLayout(new BorderLayout());
		diffTbl = new JTable();
		diffSp = new JScrollPane(diffTbl);
		diffSp.setMinimumSize(diffTblDim);
		diffSp.setPreferredSize(diffTblDim);
		diffBtn = new JButton(diffAct);
		diffDelBtn = new JButton(diffDelAct);
		diffDelBtn.setEnabled(false);
		dBtnPnl = new JPanel();
		dBtnPnl.add(diffBtn);
		dBtnPnl.add(Box.createHorizontalStrut(5));
		dBtnPnl.add(diffDelBtn);
		diffPnl.add(diffSp, BorderLayout.CENTER);
		diffPnl.add(dBtnPnl, BorderLayout.SOUTH);

		updateBtn = new JButton(updateAct);
		delBtn = new JButton(delAct);
		csvBtn = new JButton(csvAct);
		
		editPnl = new JPanel(new GridBagLayout());		
		editPnl.setBorder(new TitledBorder("Edit Selected"));
		int row = 0;
		int pad = 2;
		Insets in = new Insets(pad*4,pad,pad*4,pad);
		editPnl.add(nameLbl, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		editPnl.add(nameTf, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, in, pad, pad));
		editPnl.add(colorLbl, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		editPnl.add(colorBx, new GridBagConstraints(3, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		editPnl.add(widthPnl, new GridBagConstraints(0, ++row, 4, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		editPnl.add(notesLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		editPnl.add(notesSp, new GridBagConstraints(1, row, 3, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		editPnl.add(diffPnl, new GridBagConstraints(0, ++row, 4, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, in, pad, pad));
		editPnl.add(csvBtn, new GridBagConstraints(0, ++row, 4, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		editPnl.add(updateBtn, new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		editPnl.add(delBtn, new GridBagConstraints(2, row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		
		
		add(tblSp);
		add(Box.createVerticalStrut(5));
		add(editPnl);
		add(Box.createVerticalStrut(5));
		
		//disable the edit panel to start
		setEnabledEditPanel(false);
	}
	
	
	private HorizonTable loadTable(ArrayList<RadarHorizon> data){
		HorizonTable table = new HorizonTable(new HorizonTableModel(data, myLView));
		table.getTableHeader().setReorderingAllowed(false);
		//table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(rowListener);
		
		TableColumnAdjuster tca = new TableColumnAdjuster(table);
		tca.adjustColumns();
		
		return table;
	}
	
	private JTable loadDifferenceTable(){
		final JTable table = new JTable(new HorizonDifferenceTableModel(getSelectedHorizon()));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				//if a row is selected then enable the delete button
				//first check if the layer allows for differences (MARSIS doesn't)
				if(enableDiff){
					diffDelBtn.setEnabled(table.getSelectedRow()>-1);
				}
			}
		});
		//disable the difference delete button if there are
		// no rows in the difference table
		if(table.getRowCount()==0){
			diffDelBtn.setEnabled(false);
		}
		
		return table;
	}
	
	public void refreshHorizonTable(ArrayList<RadarHorizon> data){
		this.remove(tblSp);

		horizonTbl = loadTable(data);
		
		tblSp = new JScrollPane(horizonTbl);
		tblSp.setPreferredSize(tableDim);
		
		if(index>-1 && horizonTbl.getRowCount()>0){
			if(index>=horizonTbl.getRowCount()){
				index = horizonTbl.getRowCount()-1;
			}
			horizonTbl.setRowSelectionInterval(index, index);
		}else{
			index = -1;
		}

		setEnabledEditPanel(getSelectedHorizon()!=null);
		
		this.add(tblSp, 0);
		this.validate();
	}
	
	public void refreshHorizonDifferenceTable(){
		diffPnl.remove(diffSp);
		
		diffTbl = loadDifferenceTable();
		
		diffSp = new JScrollPane(diffTbl);
		diffSp.setPreferredSize(diffTblDim);
		
		diffPnl.add(diffSp, BorderLayout.CENTER);
		diffPnl.validate();
	}
	
	
	private ListSelectionListener rowListener = new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
			index = horizonTbl.getSelectedRow();
			refreshEditPanel();
			
			//update the views that show the horizons
			myLView.repaint();
			((StampFocusPanel)myLView.getFocusPanel()).getRadarView().updateRadarPanel();
			
			//update buffer and refresh 3d if necessary
			myLView.getLayer().increaseStateId(StampLayer.SELECTIONS_BUFFER);
			//update the 3d view if has lview3d enabled
			if(myLView.getLView3D().isEnabled()){
				ThreeDManager mgr = ThreeDManager.getInstance();
				//If the 3d is already visible, update it
				if(mgr.isReady() && myLView.getLView3D().isVisible()){
					mgr.updateDecalsForLView(myLView, true);
				}
			}
		}
	};
	
	private AbstractAction updateAct = new AbstractAction("Update Horizon".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			RadarHorizon h = getSelectedHorizon();
			h.setName(nameTf.getText());
			h.setColor(colorBx.getColor());
			h.setFullResWidth((int)fullResBx.getSelectedItem());
			h.setBrowseWidth((int)browseBx.getSelectedItem());
			h.setLViewWidth((int)lviewBx.getSelectedItem());
			h.setNote(notesTa.getText());
			
			//refresh table
			refreshHorizonTable(myLView.getFocusPanel().getRadarView().getHorizons());
			//refresh views
			myLView.getFocusPanel().getRadarView().repaintHorizon();
			myLView.repaint();
			
			//update buffer and refresh 3d if necessary
			myLView.getLayer().increaseStateId(StampLayer.SELECTIONS_BUFFER);
			//update the 3d view if has lview3d enabled
			if(myLView.getLView3D().isEnabled()){
				ThreeDManager mgr = ThreeDManager.getInstance();
				//If the 3d is already visible, update it
				if(mgr.isReady() && myLView.getLView3D().isVisible()){
					mgr.updateDecalsForLView(myLView, true);
				}
			}
		}
	};
	
	private AbstractAction delAct = new AbstractAction("Delete Selected".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			//make sure the user wants to delete the horizon
			int result = Util.showConfirmDialog(
					"Are you sure you wish to delete this horizon?",
					"Delete Confirmation",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE);
			
			if(result == JOptionPane.YES_OPTION){
				//remove the horizon
				RadarHorizon h = getSelectedHorizon();
				RadarFocusPanel radarPnl = myLView.getFocusPanel().getRadarView();
				radarPnl.getHorizons().remove(h);
				//remove from the filled stamp as well
				((FilledStampRadarTypeFocus)myLView.getFocusPanel().getRenderedView()).getFilledStamp().removeHorizon(h);
				
				//refresh table
				refreshHorizonTable(myLView.getFocusPanel().getRadarView().getHorizons());
				//refresh views
				radarPnl.repaintHorizon();
				myLView.repaint();
				
				//update buffer and refresh 3d if necessary
				myLView.getLayer().increaseStateId(StampLayer.SELECTIONS_BUFFER);
				
				if (ThreeDManager.isReady()) {
					//update the 3d view if has lview3d enabled
					if(myLView.getLView3D().isEnabled()){
						ThreeDManager mgr = ThreeDManager.getInstance();
						//If the 3d is already visible, update it
						if(myLView.getLView3D().isVisible()){
							mgr.updateDecalsForLView(myLView, true);
						}
					}
				}
			}else{
				return;
			}
		}
	};
	
	
	private AbstractAction csvAct = new AbstractAction("Export to CSV".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			//TODO: later we should use a progress bar
			//change the cursor to give some feedback to the user 
			HorizonPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			boolean succeed = true;
			RadarHorizon h = getSelectedHorizon();
			JFileChooser fc = new JFileChooser(Util.getDefaultFCLocation());
			FileFilter filter = new FileFilter() {
				public String getDescription() {
					return "CSV (*.csv)";
				}
				
				public boolean accept(File f) {
					if(f.isDirectory()){
						return true;
					}else{
						String name = f.getName().toLowerCase();
						return name.endsWith(".csv");
					}
				}
			};
			fc.setFileFilter(filter);
			if(fc.showSaveDialog(HorizonPanel.this) == JFileChooser.APPROVE_OPTION){
				String fileName = fc.getSelectedFile().toString();
				
				//add the extension to the file name if the user didn't specify
				if(!fileName.endsWith(".csv")){
					fileName = fileName+".csv";
				}
				
				try {
					FileWriter fw = new FileWriter(fileName);
					PrintWriter pw = new PrintWriter(fw);
					//create header
					pw.print("x,y,value,longitude (deg E),latitude (deg N),id,name");
					//add horizon differences to header
					for(HorizonDifference d : h.getHorizonDifferences()){
						pw.print(",horizon difference id:"+d.getSubtractedHorizon().getID()+" delta y (pixels)");
						pw.print(",horizon difference id:"+d.getSubtractedHorizon().getID()+" delta t (ns)");
						pw.print(",horizon difference id:"+d.getSubtractedHorizon().getID()+" depth (m)");
						pw.print(",horizon difference id:"+d.getSubtractedHorizon().getID()+" dielectric constant");
					}
					pw.println();
					
					///add data to the csv
					for(PixelPoint pp : h.getPixelPoints()){
						//basic stats for each pixel: x, y, lon, lat, horizon id, horizon name
						pw.print(pp.getX()+","+pp.getY()+","+pp.getValue()+","+(360-pp.getLon())+","+pp.getLat()+","+h.getID()+","+h.getName());
						//stats for each horizon difference at each pixel: delta y, delta time, delta difference, dielectric constant
						for(HorizonDifference d : h.getHorizonDifferences()){
							//delta y
							Double delta_y = d.getDifferenceMap().get(pp.getX());
							pw.print(","+delta_y);

							Double delta_t;
							Double depth;
							if(delta_y==null){
								delta_t = null;
								depth = null;
							}else{
								//delta t = delta_y * 37.5 (ns) / 2
								delta_t = delta_y*18.75;
								//depth = 3.0*10^8 (speed of light) * delta_t * 10^-9 (to go from nanosec to standard units) / sqrt(dielectric_constant)
								depth = (0.3*delta_t)/Math.sqrt(d.getDielectricConstant());
							}
							//print time
							pw.print(", "+delta_t);
							//print depth
							pw.print(","+ depth);
							//dielectric constant
							pw.print(","+d.getDielectricConstant());
							
						}
						pw.println();
					}
					
					pw.flush();
					pw.close();
					fw.close();
					
				} catch (Exception e1) {
					succeed = false;
					e1.printStackTrace();
				}
				
				
				if(succeed){
					Util.showMessageDialog("Export Successful!", "Export Success", JOptionPane.INFORMATION_MESSAGE);
				}else{
					Util.showMessageDialog("Export Not Successful.\nSee log for more info.", "Export Failure", JOptionPane.INFORMATION_MESSAGE);
				}				
			}
			
			//change the cursor back when finished
			HorizonPanel.this.setCursor(Cursor.getDefaultCursor());
		}
	};
	
	
	private AbstractAction diffAct = new AbstractAction("Create Difference...".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			new DifferenceCreationDialog(myLView.getFocusPanel().getFrame(), 
					diffBtn, getSelectedHorizon(), 
					myLView.getFocusPanel().getRadarView().getHorizons(), 
					HorizonPanel.this);
		}
	};
	
	private AbstractAction diffDelAct = new AbstractAction("Delete Difference".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			int index = diffTbl.getSelectedRow();
			HorizonDifference hd = ((HorizonDifferenceTableModel)diffTbl.getModel()).getDifferenceAtRow(index);
			getSelectedHorizon().removeHorizonDifference(hd);
			refreshEditPanel();
		}
	};
	
	private void refreshEditPanel(){
		RadarHorizon h = getSelectedHorizon();
		setEnabledEditPanel(h!=null);
		if(h!=null){
			nameTf.setText(h.getName());
			colorBx.setColor(h.getColor());
			fullResBx.setSelectedItem(h.getFullResWidth());
			browseBx.setSelectedItem(h.getBrowseWidth());
			lviewBx.setSelectedItem(h.getLViewWidth());
			notesTa.setText(h.getNote());
			refreshHorizonDifferenceTable();
		}
	}
	
	private void setEnabledEditPanel(boolean enable){
		//enable/disable fields
		nameTf.setEnabled(enable);
		colorBx.setEnabled(enable);
		fullResBx.setEnabled(enable);
		browseBx.setEnabled(enable);
		lviewBx.setEnabled(enable);
		notesTa.setEnabled(enable);
		updateBtn.setEnabled(enable);
		delBtn.setEnabled(enable);
		diffBtn.setEnabled(enable);
		csvBtn.setEnabled(enable);		
		//if there isn't more than 1 horizon, disable the difference button
		if(horizonTbl.getRowCount()<2){
			diffBtn.setEnabled(false);
		}
		
		enableDifferencePanel();
	}
	
	/**
	 * The difference panel has been disabled for all radar
	 * layers which are not MARSIS, because it is believed
	 * that the formula for calculate the dela d (difference)
	 * is dependent on the radar instrument.  This difference
	 * functionality was originally written for the SHARAD
	 * instrument and currently only supports that.
	 */
	private void enableDifferencePanel(){
		diffPnl.setEnabled(enableDiff);
		if(!enableDiff){	
			diffSp.removeAll();
		}
		//if the layer allows enabling, and if there are at least 2 horizons
		// created, then allow the delete diff
		diffBtn.setEnabled((enableDiff && horizonTbl.getRowCount()>1));
	}
	
	public RadarHorizon getSelectedHorizon(){		
		if(index == -1) return null;
		
		return ((HorizonTableModel)horizonTbl.getUnsortedTableModel()).getSelectedHorizon(index);
	
	}
}
