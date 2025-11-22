package edu.asu.jmars.layer.shape2.xb.swing;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class Calculator extends JPanel  {
   
	private static final long serialVersionUID = 1L;
	private static int NUMBER_OF_ROWS = 4;
    private static int NUMBER_OF_COLUMNS = 4;
   // private static final String divSymbol = Character.toString('\u00F7');
    private static final String piSymbol = Character.toString('\u03C0');
    private static final String sqrtSymbol = Character.toString('\u221A');
    private JTextArea textExpr = null;
    
    String[] buttonString = {"+", "-", "*", "/",
                             "abs", sqrtSymbol, "mod", "x^y",
                             "sin", "cos", "tan", "log",
                             "min", "max", "e", piSymbol };
    
    String[] tooltips = {"add numbers", "subtract numbers", "multiply numbers", "divide numbers",
    					 "get absolute value of a number", "get positive square root of a number", "get remainder of first number divided by second", "get value of first number raised to power of second number",
    					 "get trigonometric sine of an angle", "get trigonometric cosine of an angle","get trigonometric tangent of an angle", "get natural logarithm (base e) of a number",
    					 "get the smaller of two numbers", "get the greater of two numbers", "constant, value 2.718281828459045",
    					 "constant, value 3.141592653589793"    		
    };
    
    JButton[] button = new JButton[buttonString.length];
    ActionListener[] actions = new ActionListener[buttonString.length];     
    JPanel[] row = new JPanel[NUMBER_OF_ROWS];     
                               
    int[] dimW = {300,60,100,90};
    int[] dimH = {35, 40};
    Dimension displayDimension = new Dimension(dimW[0], dimH[0]);
    Dimension regularDimension = new Dimension(dimW[1], dimH[1]);
    Dimension rColumnDimension = new Dimension(dimW[2], dimH[1]);
    Dimension zeroButDimension = new Dimension(dimW[3], dimH[1]);    
    
    Runnable[] actionMethods = {
    		this::ADDAction,
    		this::SUBAction,
    		this::MULAction,
    		this::DIVAction,
    		this::ABSAction,
    		this::SQRTAction,
    		this::MODAction,
    		this::POWAction,
    		this::SINAction,
    		this::COSAction,
    		this::TANAction,
    		this::LOGAction,
    		this::MINAction,
    		this::MAXAction,
    		this::EAction,
    		this::PIAction		    		
    };	  
    
    Calculator(JTextArea textcomponent) {
    	textExpr = textcomponent;
    	initButtons();
    	createUI();
    }  
   
	private void initButtons() {
		for (int i = 0; i < button.length; i++) {
			button[i] = new JButton();
			button[i].setText(buttonString[i]);
			button[i].setToolTipText(tooltips[i]);
			button[i].setPreferredSize(regularDimension);
			actions[i] = createButtonAction(actionMethods[i]);
			button[i].addActionListener(actions[i]);
		}
	}
		
	private void createUI() {
		setSize(380, 250);
		GridLayout grid = new GridLayout(NUMBER_OF_ROWS, NUMBER_OF_COLUMNS);
		setLayout(grid);
		FlowLayout f2 = new FlowLayout(FlowLayout.CENTER, 4, 2);

		for (int i = 0; i < NUMBER_OF_ROWS; i++)
			row[i] = new JPanel();

		for (int i = 0; i < NUMBER_OF_ROWS; i++) {
			row[i].setLayout(f2);
		}	
		
		int buttonindex = 0;
		for (int rowindex = 0; rowindex < NUMBER_OF_ROWS; rowindex++) {
			for (int inner = 0; inner < NUMBER_OF_COLUMNS; inner++) {
				row[rowindex].add(button[buttonindex++]);
			}
			 add(row[rowindex]);
		}	
	}
	
	private ActionListener createButtonAction(Runnable actionMethod) {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actionMethod.run();
			}
		};
	}	

//actions
    private void TANAction() {
    	insertText(" tan(0) ");	
	}

	private void COSAction() {
		insertText(" cos(0) ");	
	}

	private void SINAction() {
		insertText(" sin(0) ");	
	}

	private void MODAction() {
		insertText(" % ");    	
	}

	private void POWAction() {
		insertText(" pow(0,0) ");	
	}

	private void ABSAction() {
		insertText(" abs(0) ");	
	}

	private void SQRTAction() {
		insertText(" sqrt(0) "); 		
	}

	private void DIVAction() {
		insertText(" / ");	
	}

	private void MULAction() {
		insertText(" * ");	
	}

	private void SUBAction() {
		insertText(" - ");
	}

	private void ADDAction() {
		insertText(" + ");	
	}
		
		
	private void LOGAction() {
		insertText(" log(0) ");	
	}
			
	private void MINAction() {
		insertText(" min(0,0) ");    
	}

	private void MAXAction() {
		insertText(" max(0,0) ");	
	}

	private void EAction() {
		insertText(" E ");    
	}

	private void PIAction() {
		insertText(" PI ");	
	} 
	
	private void insertText(String txt) {
		if (textExpr != null) {
			int pos = textExpr.getCaretPosition();
			if (pos >= 0) {
				XBMainPanel.INSERT_TEXT.insertAndRemove(textExpr, txt);
			} 
		}
	}	
	
	/*
	 * public static void main(String[] arguments) { Calculator c = new
	 * Calculator(new JTextArea()); JDialog dialog = new JDialog();
	 * dialog.setLayout(new BorderLayout());
	 * dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); dialog.add(c,
	 * BorderLayout.CENTER);
	 * 
	 * dialog.pack(); dialog.setLocationRelativeTo(null); dialog.setVisible(true);
	 * 
	 * }
	 */
	 
}
