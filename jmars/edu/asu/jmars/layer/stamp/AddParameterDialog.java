package edu.asu.jmars.layer.stamp;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jsoup.Jsoup;

import edu.asu.jmars.util.Config;
import edu.asu.msff.DataField;

public class AddParameterDialog extends JDialog {
	
	private JComboBox<String> catFilterBx;
	private String catAll = "All";
	private JList<DataField> allParamsLst;
	private HashMap<String, DataField> nameToDF;
	private ParamsListModel allModel;
	private JList<DataField> selParamsLst;
	private ParamsListModel selModel;
	private JButton addBtn;
	private JButton removeBtn;
	private JTextArea descTA;
	private String descPrompt = "Select a single parameter to see its description";
	private String emptyPrompt = "The is currently no description for this parameter";
	private JButton okBtn;
	private JButton cancelBtn;
	private JCheckBox quickAddChk;
	private final String QUICK_ADD_CONFIG_STR = "advancedParamsQuickAdd";

	private ArrayList<DataField> allParams;
	
	private StampLayerWrapper wrapper;
	
	public AddParameterDialog(JDialog parent, StampLayerWrapper wrapper, ArrayList<DataField> params, JComponent showHere){
		super(parent, "Parameter Selection", true);
		setupDialog(wrapper, params, showHere);
	}
	public AddParameterDialog(JFrame parent, StampLayerWrapper wrapper, ArrayList<DataField> params, JComponent showHere){
		super(parent, "Parameter Selection", true);
		setupDialog(wrapper, params, showHere);
	}
	public void setupDialog(StampLayerWrapper wrapper, ArrayList<DataField> params, JComponent showHere) {
		//set the location relative to but centered on the component passed in
		Point pt = showHere.getLocationOnScreen();
		setLocation(pt.x+80, pt.y+10);
		
		this.wrapper = wrapper;
		allParams = params;
		
		boolean quickAdd = Config.get(QUICK_ADD_CONFIG_STR, false);
		
		//build UI
		buildUI(quickAdd);
		pack();
		setVisible(true);
	}
	private void buildUI(boolean quickAdd){
		
		JLabel filterLbl = new JLabel("Category Filter:");
		catFilterBx = new JComboBox(getCategoryList());
		catFilterBx.addActionListener(filterListener);
		JPanel filterInnerPnl = new JPanel();
		filterInnerPnl.add(filterLbl);
		filterInnerPnl.add(catFilterBx);
		JPanel filterPnl = new JPanel(new BorderLayout());
		filterPnl.setBorder(new EmptyBorder(5, 5, 0, 5));
		filterPnl.add(filterInnerPnl, BorderLayout.WEST);
		
		//don't reset the lists' models if they've already been created,
		// ie. if redrawing the UI because a switch from quick add
		if(allModel == null){
			allModel = new ParamsListModel();
			ArrayList<String> nameList = createSortedNamesList(null);
			for(String name : nameList){
				allModel.addElement(nameToDF.get(name));
			}
		}
		if(selModel == null){
			selModel = new ParamsListModel();
		}
		allParamsLst = new JList<DataField>(allModel);
		selParamsLst = new JList<DataField>(selModel);
		//if quick add, add the listener for the all params list
		if(quickAdd){
			allParamsLst.addMouseListener(listMouseListener);
		}
		//add the mouse listener for the selected params list
		selParamsLst.addMouseListener(listMouseListener);
		//add listener to display parameter description
		allParamsLst.addListSelectionListener(listListener);
		selParamsLst.addListSelectionListener(listListener);
		
		JScrollPane allSP = new JScrollPane(allParamsLst);
		JScrollPane selSP = new JScrollPane(selParamsLst);
		
		//set the preferred size based on number of parameters
		//Shouldn't need a scrollpane at this height
		int h = allParams.size()*22+22;
		//if there are way too many parameters though, limit the
		// height to 300 pixels
		if(h>300){
			h = 300;
		}
		allSP.setPreferredSize(new Dimension(allSP.getPreferredSize().width, h));
		selSP.setPreferredSize(new Dimension(allSP.getPreferredSize().width, h));
		
		JPanel allPnl = new JPanel(new GridLayout(1,1));
		allPnl.setBorder(new CompoundBorder(new EmptyBorder(10, 10, 5, 0), new TitledBorder("Available Parameters")));
		allPnl.add(allSP);
		JPanel selPnl = new JPanel(new GridLayout(1,1));
		selPnl.setBorder(new CompoundBorder(new EmptyBorder(10, 0, 5, 10), new TitledBorder("Selected Parameters")));
		selPnl.add(selSP);
		
		JPanel descPnl = new JPanel(new GridLayout(1, 1));
		descPnl.setBorder(new CompoundBorder(new EmptyBorder(10, 10, 0, 10), new TitledBorder("Parameter Description")));
		descTA = new JTextArea(descPrompt);
		descTA.setBorder(new EmptyBorder(5, 5, 5, 5));
		descTA.setLineWrap(true);
		descTA.setWrapStyleWord(true);
		descTA.setEditable(false);
		JScrollPane descSP = new JScrollPane(descTA, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		descSP.setPreferredSize(new Dimension(descSP.getPreferredSize().width, 80));
		descPnl.add(descSP);
		//if not using quick add, create the add and remove buttons
		if(!quickAdd){
			addBtn = new JButton(new AbstractAction("Add".toUpperCase()) {
				public void actionPerformed(ActionEvent e) {
					addSelectedParams();
				}
			});
			removeBtn = new JButton(removeAct);
			removeBtn.setText("Remove".toUpperCase());
			//set disabled, will be enabled as soon as there is a selection in the sel List
			removeBtn.setEnabled(false);
		}
		quickAddChk = new JCheckBox(quickAddAct);
		quickAddChk.setSelected(quickAdd);
		quickAddChk.setToolTipText("Clicking on parameters in the 'Available Parameters' list automatically adds them to the 'Selected Parameters' list");
		okBtn = new JButton(okAct);
		cancelBtn = new JButton(cancelAct);
		
		
		JPanel mainPnl = new JPanel();
		GroupLayout layout = new GroupLayout(mainPnl);
		mainPnl.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		//set the layout based on whether it's quick add or not, with
		// quick add, the 'add' and 'remove' buttons are not needed
		SequentialGroup middleHGroup;
		ParallelGroup middleVGroup;
		if(quickAdd){
			middleHGroup = layout.createSequentialGroup()
					.addComponent(allPnl, 200 ,GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
					.addComponent(selPnl, 200 ,GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE);
			middleVGroup =  layout.createParallelGroup(Alignment.CENTER)
					.addComponent(allPnl)
					.addComponent(selPnl);
		}else{
			middleHGroup = layout.createSequentialGroup()
					.addComponent(allPnl, 200 ,GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
					.addGroup(layout.createParallelGroup(Alignment.CENTER)
						.addComponent(addBtn)
						.addComponent(removeBtn))
					.addComponent(selPnl, 200 ,GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE);
			middleVGroup =  layout.createParallelGroup(Alignment.CENTER)
					.addComponent(allPnl)
					.addGroup(layout.createSequentialGroup()
						.addComponent(addBtn)
						.addGap(20)
						.addComponent(removeBtn))
					.addComponent(selPnl);
			layout.linkSize(addBtn, removeBtn);
		}
		
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
			.addGroup(layout.createParallelGroup(Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
					.addComponent(filterLbl)
					.addComponent(catFilterBx))
				.addGroup(middleHGroup)
				.addComponent(descPnl))
			.addGroup(layout.createSequentialGroup())
				.addComponent(quickAddChk)
			.addGroup(layout.createSequentialGroup()
				.addComponent(okBtn)
				.addComponent(cancelBtn)));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(filterLbl)
				.addComponent(catFilterBx, GroupLayout.PREFERRED_SIZE,GroupLayout.PREFERRED_SIZE,GroupLayout.PREFERRED_SIZE))
			.addGroup(middleVGroup)
			.addComponent(descPnl)
			.addComponent(quickAddChk)
			.addGap(10)
			.addGroup(layout.createParallelGroup(Alignment.BASELINE)
				.addComponent(okBtn)
				.addComponent(cancelBtn)));	
		
		setContentPane(mainPnl);
		pack();
	
	}
	
	private ArrayList<String> createSortedNamesList(String category) {
		
		//create a hashmap of display names to their datafields
		if(nameToDF == null){
			nameToDF = new HashMap<String, DataField>();
			for(DataField df : allParams){
				nameToDF.put(df.getDisplayName(), df);
			}
		}
		
		//filter the datafields and produce a list of names
		ArrayList<String> nameList = new ArrayList<String>();
		for(DataField df : allParams){
			if(category == null || category.equals(df.getCategory())){
				nameList.add(df.getDisplayName());
			}
		}
		
		Collections.sort(nameList);
		
		return nameList;
	}
	
	private Vector<String> getCategoryList(){
		Vector<String> cats = new Vector<String>();
		//add the "all" string first always
		cats.add(catAll);
		
		for(DataField df : allParams){
			String category = df.getCategory();
			if(!cats.contains(category)){
				cats.add(category);
			}
		}
		
		return cats;
	}
	
	private ActionListener filterListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String category = (String)catFilterBx.getSelectedItem();
			if(category.equals(catAll)){
				category = null;
			}
			
			allModel.clear();
			ArrayList<String> nameList = createSortedNamesList(category);
			for(String name : nameList){
				allModel.addElement(nameToDF.get(name));
			}
		}
	};
	
	private MouseListener listMouseListener = new MouseListener() {
		public void mousePressed(MouseEvent arg0) {}
		public void mouseExited(MouseEvent arg0) {}
		public void mouseEntered(MouseEvent arg0) {}
		
		public void mouseReleased(MouseEvent e) {
			if(e.getSource() == allParamsLst){
				addSelectedParams();
			}
		}
		
		public void mouseClicked(MouseEvent e) {
			if(e.getSource() == selParamsLst){
				if(SwingUtilities.isRightMouseButton(e)){
					JPopupMenu rcMenu = new JPopupMenu();
					JMenuItem delItem = new JMenuItem(removeAct);
					if(selParamsLst.getSelectedIndices().length>1){
						delItem.setText("Remove Parameters".toUpperCase());
					}
					rcMenu.add(delItem);
					rcMenu.show(selParamsLst, e.getX(), e.getY());
				}
			}
		}
	};
	
	//updates the description field if only one parameter is selected between the two lists
	private ListSelectionListener listListener = new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
			int[] apIndices = allParamsLst.getSelectedIndices();
			int[] spIndices = selParamsLst.getSelectedIndices();
			int apLength = apIndices.length;
			int spLength = spIndices.length;
			DataField df = null;
			
			// if not quick add: 
			//enable/disable add/remove buttons depending on list states
			if(!quickAddChk.isSelected()){
				addBtn.setEnabled(apIndices.length>0);
				removeBtn.setEnabled(spIndices.length>0);
			}
			
			//only display output if only one datafield is selected (out of both lists)
			if (apIndices.length + spIndices.length != 1){
				descTA.setText(descPrompt);
			}
			else{
				if(apLength == 1){
					df = (DataField)allModel.get(apIndices[0]);
				}
				else if(spLength == 1){
					df = (DataField)selModel.get(spIndices[0]);					
				}
				//remove html tags from description before displaying
				String htmlDesc = df.getFieldDesc();
				String desc = Jsoup.parse(htmlDesc).text();
				
				String descStr = "";
				
				//if there is a description, display it and nothing else
				if(desc.length()>0){
					descStr = desc;
				}
				//otherwise look at the tip and the range
				else{
					String htmlTip = df.getFieldTip();
					String tip = Jsoup.parse(htmlTip).text();
					String minVal = df.getMinAllowedValue();
					String maxVal = df.getMaxAllowedValue();
					
					//if there is no range, set the description to the tip, check for empty string
					if((minVal == null || minVal.equals("")) && (maxVal == null || maxVal.equals(""))){
						if(tip.length()>0){
							descStr = tip;
						}else{
							descStr = emptyPrompt;
						}
					}
					//otherwise, set the description to have the range
					else{
						if(tip.length()>0){
							descStr = tip +"\n";
						}
						descStr += "Values range from "+minVal+" to "+maxVal;
					}
				}
				
				descTA.setText(descStr);
				//scroll back to the top of description
				descTA.setCaretPosition(0);
			}
		}
	};
	
	private AbstractAction quickAddAct = new AbstractAction("Enable 'Quick Add' Mode") {
		public void actionPerformed(ActionEvent arg0) {
			boolean quickAdd = quickAddChk.isSelected();
			//update the UI
			buildUI(quickAdd);
			AddParameterDialog.this.getContentPane().revalidate();
			//save preference to user's config
			Config.set(QUICK_ADD_CONFIG_STR, quickAdd);
		}
	};
	
	
	private void addSelectedParams(){
		for(int i : allParamsLst.getSelectedIndices()){
			DataField df = (DataField)allModel.get(i);
			selModel.addElement(df);
			if(quickAddChk.isSelected()){
				selParamsLst.setSelectedIndex(selModel.size()-1);
			}
		}
		allParamsLst.clearSelection();
	}

	
	private AbstractAction removeAct = new AbstractAction("Remove Parameter".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			int[] indices = selParamsLst.getSelectedIndices();
			for(int i=indices.length-1; i>-1; i--){
				DataField df = (DataField) selModel.get(indices[i]);
				selModel.removeElement(df);
			}
		}
	};
	
	private AbstractAction okAct = new AbstractAction("OK".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			//add params to add layer wrapper
			for(int i=0; i<selModel.getSize(); i++){
				DataField df = (DataField) selModel.get(i);
				wrapper.addFieldToAdvPane(df);
			}
			//refresh advanced field ui
			wrapper.refreshAdvPane();
			
			AddParameterDialog.this.setVisible(false);
		}
	};
	
	private AbstractAction cancelAct = new AbstractAction("Cancel".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			AddParameterDialog.this.setVisible(false);
		}
	};


	private class ParamsListModel extends DefaultListModel{
		/**
		 * Override this method so that the display name is what
		 * shows in the JList instead of the object reference.
		 * Could not override the toString method on DataField,
		 * so this was the next simplest solution.
		 */
		@Override
		public Object getElementAt(int index) {
			DataField df = (DataField) get(index);
			return df.getDisplayName();
		}
		
	}
}
