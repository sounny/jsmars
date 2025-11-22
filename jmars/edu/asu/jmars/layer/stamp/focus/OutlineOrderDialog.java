package edu.asu.jmars.layer.stamp.focus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.stamp.StampComparator;
import edu.asu.jmars.layer.stamp.StampGroupComparator;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FontFile;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import jiconfont.DefaultIconCode;
import jiconfont.IconCode;
import jiconfont.IconFont;
import jiconfont.swing.IconFontSwing;

/**
 * A dialog which allows the user to add and remove OrderRules
 * for it's related StampLView.  This dialog also has a related
 * OrderPreview, which is a JPanel that summarizes the rules and
 * contains a "modify" button which opens this Dialog.  The 
 * OrderPreview is supposed to be placed into the "parent" container
 * that's calling this dialog (ex. OutlineFocusPanel).
 */
public class OutlineOrderDialog extends JDialog{
	private JComboBox<String> colBx;
	private JComboBox<JLabel> dirBx;
	private final String COLUMN_PROMPT = "Choose a Field";
	private final String DIRECTION_PROMPT = "Choose a Direction";
	private final String ASCENDING_STR = "Ascending";
	private final String DESCENDING_STR = "Descending";
	final static String HTML_UP_STR = "&#8593;";
	final static String HTML_DOWN_STR = "&#8595;";
	private JButton addBtn;
	private OutlineOrderTableModel ordertblModel;
	private OutlineOrderTable ordertbl;
	private JButton okBtn;
	private JButton cancelBtn;
	private JScrollPane rowSP;
	private OrderPreview myPreview;
	private StampGroupComparator myGroupComparator;
	private OutlineFocusPanel outlineFocus;
	private StampLayer myLayer;
	private ArrayList<String> columns = new ArrayList<String>();
	private int pad = 1;
	private Insets in = new Insets(5,5,5,5);
	
	private static MyIconFont iconfont;
	private static IconCode iconcode;
	static Icon moveUPicon, moveDOWNicon;
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static final int iconsize = 18;
	private static final Character SORT_ASC = '\u2191';  //single-line arrow UP
	private static final Character SORT_DESC = '\u2193';  //single-line arrow DOWN


	static {
		createSortIcons();
	}
	
	private static void createSortIcons() {
		iconfont = new MyIconFont(FontFile.MYFONT_REGULAR.toString());
		IconFontSwing.register(iconfont);	
		iconcode = new DefaultIconCode(iconfont.getFontFamily(), SORT_ASC);
		moveUPicon = IconFontSwing.buildIcon(iconcode, iconsize, imgColor);
		iconcode = new DefaultIconCode(iconfont.getFontFamily(), SORT_DESC);
		moveDOWNicon = IconFontSwing.buildIcon(iconcode, iconsize, imgColor);		
	}		
	
	public OutlineOrderDialog(StampLayer stampLayer, OutlineFocusPanel ofp){
		super(new JFrame(), "Outline Ordering", true);
		
		myLayer = stampLayer;
		outlineFocus = ofp;
		ordertblModel = new OutlineOrderTableModel();
		
		myPreview = new OrderPreview();
		
		buildUI();
	}
	
	private void buildUI(){
		columns.add(0, COLUMN_PROMPT);
		colBx = new JComboBox<String>(new DefaultComboBoxModel(columns.toArray()));
		colBx.addActionListener(comboBoxListener);
		
		Vector<JLabel> directions = new Vector<>();
		JLabel prompt = new JLabel(DIRECTION_PROMPT);
		directions.add(prompt);
		
		JLabel asclabel = new JLabel();
		asclabel.setHorizontalTextPosition(SwingConstants.LEFT);
		asclabel.setText(ASCENDING_STR);
		asclabel.setIcon(moveUPicon);
		directions.add(asclabel);
		
		JLabel desclabel = new JLabel();
		desclabel.setHorizontalTextPosition(SwingConstants.LEFT);
		desclabel.setText(DESCENDING_STR);
		desclabel.setIcon(moveDOWNicon);
		directions.add(desclabel);
		
		dirBx = new JComboBox<JLabel>(new DefaultComboBoxModel(directions));
		dirBx.addActionListener(comboBoxListener);
		
		dirBx.setRenderer(new MyListCellRenderer());
				
		addBtn = new JButton(addAct);
		addBtn.setEnabled(false);
		
		JPanel topPnl = new JPanel();
		topPnl.setLayout(new GridBagLayout());
		topPnl.setBorder(new EmptyBorder(0, 0, 10, 0));
		topPnl.add(colBx, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		topPnl.add(dirBx, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		topPnl.add(addBtn, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));

		ordertbl = new OutlineOrderTable(ordertblModel);
		
		Dimension size = new Dimension(0, 200);
		rowSP = new JScrollPane(ordertbl);
		rowSP.setPreferredSize(size);
		JPanel centerPnl = new JPanel(new GridLayout(1,1));
		centerPnl.add(rowSP);
		
		okBtn = new JButton(okAct);
		cancelBtn = new JButton(cancelAct);
		JPanel botPnl = new JPanel();
		botPnl.setBorder(new EmptyBorder(10, 0, 0, 0));
		botPnl.add(okBtn);
		botPnl.add(Box.createHorizontalStrut(10));
		botPnl.add(cancelBtn);
		
		JPanel mainPnl = new JPanel(new BorderLayout());
		mainPnl.setBorder(new EmptyBorder(15,15,10,15));
		mainPnl.add(topPnl, BorderLayout.NORTH);
		mainPnl.add(centerPnl, BorderLayout.CENTER);
		mainPnl.add(botPnl, BorderLayout.SOUTH);
		
		setContentPane(mainPnl);
		pack();
	}
	
	private ActionListener comboBoxListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			//only enable the add button if there are valid selections
			// in both comobboxes
			if(!colBx.getSelectedItem().toString().equals(COLUMN_PROMPT) && !dirBx.getSelectedItem().toString().equals(DIRECTION_PROMPT)){
				addBtn.setEnabled(true);
			}else{
				addBtn.setEnabled(false);
			}
		}
	};
	
	private AbstractAction addAct = new AbstractAction("ADD") {
		public void actionPerformed(ActionEvent arg0) {
			//create a new order rule and add it to the list
			String name = colBx.getSelectedItem().toString();
			JLabel dirlbl = (JLabel) dirBx.getSelectedItem();
			String dirStr = dirlbl.getText();
			boolean up =  dirStr.contains(ASCENDING_STR);
			OrderRule newRule = new OrderRule(name, up);
			//add it to the list model
			ordertblModel.add(newRule);
		}
	};

		

	private AbstractAction okAct = new AbstractAction("OK") {
		public void actionPerformed(ActionEvent e) {
			//update comparator
			updateComparator();
			//update GUI
			myPreview.buildReadout();
			OutlineOrderDialog.this.setVisible(false);
			outlineFocus.refreshViews();
			//set the settings
			outlineFocus.mySettings.setOrderRules(getOrderRules());
		}
	};
	
	private AbstractAction cancelAct = new AbstractAction("CANCEL") {
		public void actionPerformed(ActionEvent e) {
			OutlineOrderDialog.this.setVisible(false);
		}
	};
	
	
	/**
	 * Set the column names as the choices to choose from for which
	 * columns to order by
	 * @param columnNames  The names of applicable columns to order by
	 */
	public void setColumnNames(ArrayList<String> columnNames){
		columns = columnNames;
		columns.add(0, COLUMN_PROMPT);
		colBx.setModel(new DefaultComboBoxModel(columns.toArray()));
		//enable the modify button now there are column options
		myPreview.modifyBtn.setEnabled(true);
		pack();
	}
	
	/**
	 * Add OrderRules to the existing model for the order list,
	 * and then refresh the preview to reflect the new rules
	 * @param rules  Rules to add to the model
	 */
	public void addOrderRules(ArrayList<OrderRule> rules) {
		List<OrderRule> r = new ArrayList<>();
		if (rules != null && rules.size() > 0) {
			r.addAll(rules);
			// add it to the table model
			ordertblModel.addData(r);
		}
		myPreview.buildReadout();
	}

	/**
	 * @return  An ArrayList of all the OrderRules in the list's model
	 */
	public ArrayList<OrderRule> getOrderRules(){
		ArrayList<OrderRule> results = new ArrayList<OrderRule>();
		results.addAll(ordertblModel.getModelData());
		return results;
	}
	
	
	/**
	 * @return The current StampGroupComparator based on the order rules list
	 */
	public StampGroupComparator getStampGroupComparator(){
		if(myGroupComparator == null){
			updateComparator();
		}
		
		return myGroupComparator;
	}
	
	private void updateComparator(){
		ArrayList<StampComparator> comparators = new ArrayList<StampComparator>();
		for(OrderRule rule: getOrderRules()){
			String orderCol = rule.getColumnName();
			boolean orderDir = rule.isAscending();
			int colInt = myLayer.getColumnNum(orderCol);
			comparators.add(new StampComparator(colInt, orderDir));
		}
		myGroupComparator = new StampGroupComparator(comparators);
	}
	
	/**
	 * @return  The OrderPreview which is a panel that summarizes
	 * the added OrderRules and a "modify" button which opens its
	 * OutlineOrderDialog
	 */
	public OrderPreview getPreview(){
		return myPreview;
	}
	
	
	/**
	 * A fairly simple class that consists of a column name and a direction
	 * to order by (ascending == up)
	 */
	public static class OrderRule implements Serializable {
		private boolean up;
		private String colName;
		
		OrderRule(String columnName, boolean ascending){
			up = ascending;
			colName = columnName;
		}
		
		public boolean isAscending(){
			return up;
		}
		
		public String getColumnName(){
			return colName;
		}
	}
	
	private class OrderPreview extends JPanel{
		private JButton modifyBtn;
		private JScrollPane previewSP;
		private JPanel previewPnl;

		private OrderPreview(){
			buildUI();
		}
		
		void buildUI(){
			modifyBtn = new JButton(modifyAct);
			//don't enable the modify button until the layer has loaded and 
			// returns with it's columns
			modifyBtn.setEnabled(false);
			buildReadout();

			previewSP = new JScrollPane(previewPnl);
			
			JPanel buttonPnl = new JPanel();
			buttonPnl.add(modifyBtn);
			
			this.setBorder(new TitledBorder("Drawing Order..."));
			
			this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			this.add(Box.createHorizontalGlue());
			this.add(previewSP);
			this.add(buttonPnl);
			this.add(Box.createHorizontalGlue());
		}
		
		private void buildReadout(){
			if(previewPnl == null){
				previewPnl = new JPanel();
				previewPnl.setBorder(new EmptyBorder(5, 5, 0, 0));
			}else{
				previewPnl.removeAll();
			}
			
			if(ordertblModel.getRowCount() == 0){
				previewPnl.add(new JLabel("Using Default Order"));
			}
			else {
				for (int i = 0; i < ordertblModel.getRowCount(); i++) {
					OrderRule rule = (OrderRule) ordertblModel.getDataAt(i);
					if (rule != null) {
						JLabel lbl = new JLabel();
						lbl.setHorizontalTextPosition(SwingConstants.LEFT);
						lbl.setText(rule.getColumnName());
						if (rule.isAscending()) {
							lbl.setIcon(moveUPicon);
						} else {
							lbl.setIcon(moveDOWNicon);
						}
						previewPnl.add(lbl);
						// if this is not the last rule, add a comma and space
						if (i < ordertblModel.getRowCount() - 1) {
							previewPnl.add(new JLabel(", "));
						}
					}
				}
			}
			
			previewPnl.repaint();
			previewPnl.revalidate();
			this.revalidate();
		}
				
		private AbstractAction modifyAct = new AbstractAction("Modify Outline Order") {
			public void actionPerformed(ActionEvent arg0) {
				OutlineOrderDialog.this.setLocationRelativeTo(modifyBtn);
				OutlineOrderDialog.this.setVisible(true);
			}
		};
	}
	

	 static class MyIconFont implements IconFont {
		private String fontfilename;

		MyIconFont(String filename) {
			this.fontfilename = filename;
		}

		@Override
		public String getFontFamily() {
			return ThemeFont.getFontFamily();
		}

		@Override
		public InputStream getFontInputStream() {
			return Main.getResourceAsStream(ThemeFont.getFontPath() + this.fontfilename);
		}
	}
	
	
	private class MyListCellRenderer implements ListCellRenderer<JLabel> {
		@Override
		public Component getListCellRendererComponent(JList<? extends JLabel> list, JLabel value, int index,
				boolean isSelected, boolean cellHasFocus) {
			return value;
		}
	}
	
}

