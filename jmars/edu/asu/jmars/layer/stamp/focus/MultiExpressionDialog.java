package edu.asu.jmars.layer.stamp.focus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;

import edu.asu.jmars.layer.stamp.SpectraUtil;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.focus.OutlineFocusPanel.StampAccessor;
import edu.asu.jmars.layer.util.features.FieldFormulaMethods;
import edu.asu.jmars.swing.DocumentCharFilter;
import edu.asu.jmars.util.Util;
import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;

/**
 * A dialog which allows users to add and remove expressions in
 * a stamp layer.  Each expression becomes a column in the stamp
 * table and has a calculated value for every row (stamp).
 * 
 */
public class MultiExpressionDialog extends JDialog{
	
	private Color emptyColor;
	private Color errorColor;
	private Color validColor;
	private JList<ColumnExpression> expList;
	private DefaultListModel<ColumnExpression> expListModel;
	private JList<String> colList;
	private JTextField nameTF;
	private JTextArea expTA;
	private JTextField errTF;
	private JButton addBtn;
	private JButton clearBtn;
	private JButton okBtn;
	private JPanel colPnl;
	private JScrollPane colSP;
	private String validStr = "No errors - valid expression";
	private String emptyStr = "No expression to evaluate";
	private Library lib;
	private Dimension colPrefSize;
	private CompiledExpression curExp;
	
	private StampLayer myLayer;
	
	private int pad = 1;
	private Insets in = new Insets(pad,pad,pad,pad);
	
	public MultiExpressionDialog(StampLayer stampLayer){
		super(new JFrame(), "Column Expressions", true);
		
		myLayer = stampLayer;
		
		//set the colors for validation/error text
		errorColor = Color.RED;
		validColor = Color.GREEN;
		//TODO: get font color from theme
		emptyColor = Color.WHITE;
		
		//for expression listener -- moved from OutlineFocusPanel
	    //// Silliness
	    StampAccessor stampMap = new StampAccessor(myLayer, null);
		lib = new Library(
			new Class[]{Math.class, FieldFormulaMethods.class},
			new Class[]{StampAccessor.class},
			new Class[]{},
			stampMap,
			null);
		/// Maybe end of Silliness
		
		
		buildUI();
	}
	
	/**
	 * @return The library used for evaluating compiled expressions
	 */
	public Library getLibrary(){
		return lib;
	}
	
	
	private void buildUI(){
		
		JPanel existingPnl = new JPanel(new BorderLayout());
		existingPnl.setBorder(new TitledBorder("Existing Expressions"));
		expListModel = new DefaultListModel<ColumnExpression>();
		expList = new JList<ColumnExpression>(expListModel);
		expList.addMouseListener(expressionListListener);
		JScrollPane expSP = new JScrollPane(expList);
		existingPnl.add(expSP, BorderLayout.CENTER);
		
		JPanel newPnl = new JPanel(new BorderLayout());
		newPnl.setBorder(new TitledBorder("New Expression"));
		
		//don't allow spaces in the column expression names
		ArrayList<Character> docList = new ArrayList<Character>();
		docList.add(' ');
		DocumentFilter spaceFilter = new DocumentCharFilter(docList, false);
		JLabel nameLbl = new JLabel("Name:");
		nameTF = new JTextField();
		nameTF.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent arg0) {
				setVisibilityForAddBtn();
			}
		});
		((AbstractDocument)nameTF.getDocument()).setDocumentFilter(spaceFilter);
		
		JLabel expLbl = new JLabel("Expression:");
		expTA = new JTextArea();
		expTA.setLineWrap(true);
		expTA.getDocument().addDocumentListener(expressionListener);
		JScrollPane entrySP = new JScrollPane(expTA);
		entrySP.setPreferredSize(new Dimension(500, 0));
		JPanel expPnl = new JPanel(new GridLayout(1,1));
		expPnl.add(entrySP);
		
		JLabel errLbl = new JLabel("Validation:");
		errTF = new JTextField(emptyStr);
		errTF.setEditable(false);

		addBtn = new JButton(addAct);
		//enable add button if the fields are all correct
		addBtn.setEnabled(false);
		clearBtn = new JButton(clearAct);
		JPanel btnPnl = new JPanel();
		btnPnl.add(addBtn);
		btnPnl.add(Box.createHorizontalStrut(10));
		btnPnl.add(clearBtn);
		
		colList = new JList<String>();
		colList.addMouseListener(columnListener);
		colPnl = new JPanel(new BorderLayout());
		colPnl.setBorder(new CompoundBorder(new TitledBorder("Numeric Column Names"), new EmptyBorder(0, 0, 5, 0)));
		colSP = new JScrollPane(colList);
		colPnl.add(colSP, BorderLayout.CENTER);
		JPanel tipPnl = new JPanel();
		tipPnl.add(new JLabel("* denotes an array"));
		colPnl.add(tipPnl, BorderLayout.SOUTH);
		
		JPanel newPnlCen = new JPanel(new GridBagLayout());
		int row = 0;
		newPnlCen.add(nameLbl, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, in, pad, pad));
		newPnlCen.add(nameTF, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		row++;
		newPnlCen.add(expLbl, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, in, pad, pad));
		newPnlCen.add(expPnl, new GridBagConstraints(1, row, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, in, pad, pad));
		row++;
		newPnlCen.add(errLbl, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, in, pad, pad));
		newPnlCen.add(errTF, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, in, pad, pad));
		row++;
		newPnlCen.add(btnPnl, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		
		JPanel botPnl = new JPanel();
		okBtn = new JButton(okAct);
		botPnl.add(okBtn);
		
		newPnl.add(newPnlCen, BorderLayout.CENTER);
		newPnl.add(colPnl, BorderLayout.EAST);
			
		JPanel mainPnl = new JPanel(new GridBagLayout());
		mainPnl.setBorder(new EmptyBorder(10, 10, 10, 10));
		mainPnl.add(existingPnl, new GridBagConstraints(0, 0, 1, 1, 1, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, in, pad, pad));
		mainPnl.add(newPnl, new GridBagConstraints(0, 1, 1, 1, 1, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, in, pad, pad));
		mainPnl.add(botPnl, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		
		setContentPane(mainPnl);
		pack();
	}
	
	private MouseListener columnListener = new MouseAdapter() {
		//add column with a double left click
		 public void mouseClicked(MouseEvent e) {
			 if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){
				 String columnName = colList.getSelectedValue();
				 
				 //check if it's an array column
				 if(columnName.contains("*")){
					 //if so, remove the *, add [], and set the caret
					 columnName = columnName.replace("*", "[]");
					 expTA.append(columnName);
					 expTA.setCaretPosition(expTA.getText().length()-1);
				 }else{
					 //otherwise, simply use the column name
					 expTA.append(columnName);
				 }
				 
				 expTA.requestFocus();
			 }
		 }
	};
	
	private MouseListener expressionListListener = new MouseAdapter() {
		JPopupMenu expMenu;
		public void mouseClicked(MouseEvent e){
			if(SwingUtilities.isRightMouseButton(e)){
				
				if(expMenu == null){
					expMenu = new JPopupMenu();
					JMenuItem delExpItem = new JMenuItem(delExpAct);
					JMenuItem editExpItem = new JMenuItem(editExpAct);
					expMenu.add(delExpItem);
					expMenu.add(editExpItem);
				}
				expMenu.show(expList, e.getX(), e.getY());
			}
		}
	};
	
	private AbstractAction delExpAct = new AbstractAction("Delete Expression") {
		public void actionPerformed(ActionEvent e) {
			ColumnExpression selExp = expList.getSelectedValue();
			delExpression(selExp);
		}
	};
	
	private AbstractAction editExpAct = new AbstractAction("Edit Expression") {
		public void actionPerformed(ActionEvent e) {
			//first, remove the selected expression from the list
			ColumnExpression selExp = expList.getSelectedValue();
			expListModel.removeElement(selExp);
			
			//then, populate the "new" expression fields
			nameTF.setText(selExp.getName());
			expTA.setText(selExp.getExpression());
			
			//set focus in the text area
			expTA.requestFocus();
		}
	};
	
	private DocumentListener expressionListener = new DocumentListener() {
		public void changedUpdate(DocumentEvent e) {
			expressionChanged();
		}
		public void insertUpdate(DocumentEvent e) {
			expressionChanged();
		}
		public void removeUpdate(DocumentEvent e) {
			expressionChanged();
		}
	};
	
	private void expressionChanged() {
		try {
			
			String originalText = expTA.getText();
			
			//if the text is empty, no need to do anything more 
			if(originalText.length()==0){
				setMessage(emptyStr, emptyColor);
				return;
			}
			
			String alteredText = alterExpressionText(originalText);
			
			//evaluate the expression
			curExp = Evaluator.compile("convertReturnType("+alteredText+")", lib, Object.class);
			
			setMessage(validStr, validColor);
			
		} catch (Throwable e) {
			
			// make no change on error, just whine about it
			String msg = e.getMessage();
			while (e.getCause() != null) {
				e = e.getCause();
				msg += "\n  Caused by: " + e.getMessage();
			}
			if(msg!=null){
				setMessage(msg, errorColor);
			}else{		
				setMessage(validStr, validColor);
			}
		}
		
		//set the visibility for add button
		setVisibilityForAddBtn();
	}
	
	private String alterExpressionText(String originalText) {
		String newText = SpectraUtil.decrementIndexes(originalText);
		return newText;
	}
	
	private void setMessage(String message, Color color){
		errTF.setText(message);
		errTF.setForeground(color);
	}
	
	private void setVisibilityForAddBtn(){
		boolean enable = false;
		String nameStr = nameTF.getText();
		String expStr = expTA.getText();
		
		//if the texts are not null and the name is unique
		if(nameStr!=null && expStr!=null && isUniqueName(nameStr)){
			//if the length is not zero
			if(nameStr.length()>0 && expStr.length()>0){
				//and if the expression is valid
				if(errTF.getText().equals(validStr)){
					enable = true;
				}
			}
		}
		
		//if the button's status is not the same as the new status,
		// set it to the new enabled status
		if(addBtn.isEnabled()!=enable){
			addBtn.setEnabled(enable);
		}
	}
	
	
	private AbstractAction addAct = new AbstractAction("ADD") {
		public void actionPerformed(ActionEvent arg0) {
			//add the expession
			boolean success = addExpression(nameTF.getText(), expTA.getText());
			
			if(!success){
				Util.showMessageDialog("Name was not unique. Please enter a name that has not been used already.", "ColumnExpression Addition Failed", JOptionPane.ERROR_MESSAGE);
			}
			else{
				//clear expression fields
				nameTF.setText("");
				expTA.setText("");
			}
		}
	};
	
	private AbstractAction clearAct = new AbstractAction("CLEAR EXPRESSION") {
		public void actionPerformed(ActionEvent e) {
			expTA.setText("");
			setMessage(emptyStr, emptyColor);
			//set the focus back on the expression textarea
			expTA.requestFocus();
		}
	};

	private AbstractAction okAct = new AbstractAction("OK") {
		public void actionPerformed(ActionEvent e) {
			MultiExpressionDialog.this.setVisible(false);
		}
	};
	
	
	/**
	 * Update the list of column names for the MultiExpressionDialog UI
	 * @param columnNames  The list of names to display
	 */
	public void updateColumnList(ArrayList<String> columnNames){
		
		DefaultListModel<String> colModel = new DefaultListModel<String>();
		for(String name : columnNames){
			//skip the "choose a column" entry
			if(name.toLowerCase().contains("choose")){
				continue;
			}
			colModel.addElement(name);
		}
		
		colList.setModel(colModel);
		
		//set the minimum width to be that of the preferred size of the column list
		if(colPrefSize == null){
			int colW = colList.getPreferredSize().width + 20; //to account for the panel's width
			int spH = colPnl.getSize().height;
			colPrefSize = new Dimension(colW, spH);
			colSP.setPreferredSize(colPrefSize);
			
			//resize the dialog
			pack();
		}
	}
	
	/**
	 * Add an expression to the expressions list
	 * @param name  Name of the expression column 
	 * @param exp	Mathematical text for the expression
	 * @return boolean  Returns true if the name was unique and 
	 * the column expression was added successfully.  Returns 
	 * false if the name was not unique and the column expression
	 * was not added.
	 */
	public boolean addExpression(String name, String exp){
		//double check name is unique first
		if(!isUniqueName(name)){
			return false;
		}
		
		//create the compiled expression
		if(curExp == null){
			try {
				String alteredText = alterExpressionText(exp);
				curExp = Evaluator.compile("convertReturnType("+alteredText+")", lib, Object.class);
			} catch (CompilationException e) {
				Util.showMessageDialogObj("Expression failed to compile.", "Expression Compilation Failure", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
				return false;
			}
		}
		
		ColumnExpression newExp = new ColumnExpression(name, exp, curExp);
		expListModel.addElement(newExp);

		//set the settings
		myLayer.getSettings().setExpressions(getExpressions());
		
		//update layer
		myLayer.addColumnExpression(newExp);
		
		return true;
	}

	/**
	 * Add an expression to the expressions list
	 * @param exp the ColumnExpression to add
	 * @return boolean  Returns true if the name was unique and 
	 * the column expression was added successfully.  Returns 
	 * false if the name was not unique and the column expression
	 * was not added.
	 */
	public boolean addExpression(ColumnExpression exp){
		return addExpression(exp.getName(), exp.getExpression());
	}
	
	/**
	 * Checks if the specified name has been used or not for all
	 * of the existing column expressions.
	 * @param name The name to check
	 * @return  Returns true if the name has not been used (is unique),
	 * returns false if the name has already been used (is not unique).
	 */
	public boolean isUniqueName(String name){
		for(ColumnExpression ce : getExpressions()){
			if(ce.getName().equals(name)){
				//name matches, so it is not unique
				return false;
			}
		}
		return true;
	}

	private void delExpression(ColumnExpression exp){
		expListModel.removeElement(exp);
		
		//update layer
		myLayer.deleteColumnExpression(exp);
	}
	
	private ArrayList<ColumnExpression> getExpressions(){
		ArrayList<ColumnExpression> results = new ArrayList<ColumnExpression>();
	
		for(int i=0; i<expListModel.size(); i++){
			results.add((ColumnExpression)expListModel.get(i));
		}
		
		return results;
	}
	

	
	/**
	 * Represents a column expression with a name and the text of the expression
	 */
	// this class has to be static otherwise it will throw weird UI exceptions when saving
	public static class ColumnExpression implements Serializable{
		private String colName;
		private String expression;
		/** This is transient because it can't be serialized */
		transient CompiledExpression compExp;
		
		public ColumnExpression(String name, String expStr, CompiledExpression ce){
			colName = name;
			expression = expStr;
			compExp = ce;
		}
		
		public String getName(){
			return colName;
		}
		
		public String getExpression(){
			return expression;
		}
		
		public String toString(){
			return colName+": "+expression;
		}
		
		public CompiledExpression getCompiledExpression(){
			return compExp;
		}
	}
}
