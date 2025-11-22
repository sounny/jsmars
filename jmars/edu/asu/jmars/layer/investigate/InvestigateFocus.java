package edu.asu.jmars.layer.investigate;

import java.awt.Color;
import java.awt.Component;
import java.util.Vector;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;


public class InvestigateFocus extends FocusPanel{
	private InvestigateLayer layer;
	private InvestigateLView lview;
	
	public InvestigateFocus(InvestigateLView parent) {
		super(parent, false);
		lview = parent;
		layer = lview.iLayer;
		//if recreating from session, etc, recreate focus panels as well
		for(DataSpike ds : layer.getDataSpikes()){
			addSpikePanel(ds);
		}
		for(DataProfile dp : layer.getDataProfiles()){
			addProfilePanel(dp);
		}
		
//		addTab("Overview", new OverviewPanel()); TODO
	}

	public void addSpikePanel(DataSpike ds){
		addTab(ds.getName().toUpperCase(), new DataSpikeFocus(ds, lview));
	}
	public void addProfilePanel(DataProfile dp){
		addTab(dp.getName().toUpperCase(), new DataProfileFocus(dp));
	}
	
	public void renameTab(Component c, String name){
		int index = -1;
		for(int i=0; i<getTabCount(); i++){
			if(getComponentAt(i) == c){
				index=i;
				break;
			}
		}
		if(index == -1)	return;
		
		setTitleAt(index, name);
	}
	
	public void removeTabByComponent(Component c){
		int index = -1;
		for(int i=0; i<getTabCount(); i++){
			if(getComponentAt(i) == c){
				index = i;
				break;
			}
		}
		if(index == -1)	return;
		
		removeTabAt(index);
	}
	
	
	
	private class OverviewPanel extends JPanel{
		JPanel dataPnl;
		JPanel exportPnl;
		JTable dataTbl;
		Vector<String> colNames = new Vector<String>();
		Vector data;
			
		
		OverviewPanel(){
			layoutComponents();
		}
		
		private void layoutComponents(){
			
			setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
			
			dataPnl = new JPanel();			
			dataPnl.setBorder(new TitledBorder("Data"));
			
			data = new Vector();
			colNames.add("Pt/Line");
			colNames.add("Show Pt/Line");
			colNames.add("Name");
			colNames.add("Show Label");
			colNames.add("Outline Color");
			colNames.add("Fill Color");
			colNames.add("Label Color");
			colNames.add("Label Size");
			colNames.add("Delete");
			
			dataTbl = new JTable(data, colNames);
			
			dataPnl.add(dataTbl);
					
			exportPnl = new JPanel();		
			exportPnl.setBorder(new TitledBorder("Export/Import"));						
			
			add(dataPnl);
			add(exportPnl);
			
		}
		
		
		
		public void addRowToTable(Vector data){
			DefaultTableModel model =  (DefaultTableModel) dataTbl.getModel();
			model.insertRow(model.getRowCount()-1,data);
		}
	}
	
}
