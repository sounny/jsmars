package edu.asu.jmars.layer.shape2.xb.swing;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.SEARCH2;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.SEARCH2_SEL;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import de.codesourcery.swing.autocomplete.AutoCompleteBehaviour;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.shape2.ColumnEditor;
import edu.asu.jmars.layer.shape2.drawingpalette.swing.FontSizeHeaderLabel;
import edu.asu.jmars.swing.ImportantMessagePanel;
import edu.asu.jmars.swing.LikeDefaultButtonUI;
import edu.asu.jmars.swing.PasteField;
import edu.asu.jmars.swing.landmark.search.swing.LatLonBox;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FONTS;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FontFile;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeText;
import edu.asu.jmars.util.Config;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import edu.asu.jmars.layer.shape2.xb.XB;
import edu.asu.jmars.layer.shape2.xb.autocomplete.policy.DynamicData;
import edu.asu.jmars.layer.shape2.xb.autocomplete.policy.XBPolicyCreator;
import edu.asu.jmars.layer.shape2.xb.autocomplete.service.*;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.RTextAreaEditorKit;
import org.fife.ui.rtextarea.RTextAreaEditorKit.RedoAction;
import org.fife.ui.rtextarea.RTextAreaEditorKit.UndoAction;
import org.fife.ui.rtextarea.RTextScrollPane;


public class XBMainPanel extends JPanel {
	private static LatLonBox txtSearch;
	static JTextField columnSearchInput;
	static RSyntaxTextArea textExpr;
	private static final String ENTER_KW = "";	
	private JButton buttonABS;
	private JButton buttonLOG;
	private JButton buttonMAX;
	private JButton buttonSIN;	
	private JButton buttonClearALL;
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private Icon helpicon =  new ImageIcon(ImageFactory.createImage(ImageCatalogItem.XB_HELP.withDisplayColor(imgColor)));
	private Icon keyboardicon =  new ImageIcon(ImageFactory.createImage(ImageCatalogItem.XB_KEYBOARD.withDisplayColor(imgColor)));
	private static Icon clearicon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CLOSE.withDisplayColor(imgColor)));
	private static JLabel clearSearchTextBtn = new JLabel();
	private static JPanel resultTab = new JPanel();     
    private static JPanel errorTab = new JPanel();
    private static JTabbedPane tabbedpane = new JTabbedPane();
	private static RTextScrollPane expressionsp;
	private JButton close;
	private JButton apply;
	private JLabel lblexprhelp;
	private ColumnEditor hostEditor = null;
	private int smallFontSize = 11;	
	private static AutoCompleteService autocompleteEXPRservice;
	private static MyAutoCompleteCallback myAutoCompleteCallback;
	private final static AutoCompleteBehaviour<String> autoCompleteEXPRui;
	private static final Font exprFont = ThemeFont.getFont(FontFile.REGULAR.toString(), false).deriveFont(FONTS.ROBOTO.fontSize());
	private static String PATH_TO_QUICK_TIPS = "resources/xb/quick.tips.formula.txt";
	private static String quickTips = "";
	private static String PATH_TO_FUNC_HELP = "resources/xb/func.help.txt";
	private static String PATH_TO_KEYBOARD_HELP_MAC = "resources/xb/keyboard.MAC.txt";
	private static String PATH_TO_KEYBOARD_HELP_WINDOWS = "resources/xb/keyboard.windows2.txt";	
	private static String funcHelp = "";
	private static String keyboardHelp = "";
	
	public static final TextAreaInsert INSERT_TEXT  = new TextAreaInsert();
	
	static {
		txtSearch = new LatLonBox(new PasteField(ColumnSearchPanel.ENTER_KW, 15), null, null);
		columnSearchInput = txtSearch.getTextFiled();
		textExpr = new XBRichSyntaxTextArea();
		loadQuickTips();
		loadFuncHelp();
		keyboardHelp = (Main.MAC_OS_X) ? Main.getResourceAsString(PATH_TO_KEYBOARD_HELP_MAC) : Main.getResourceAsString(PATH_TO_KEYBOARD_HELP_WINDOWS);		
		expressionsp = new RTextScrollPane(textExpr);
		expressionsp.setMinimumSize(new java.awt.Dimension(500, 150));
		expressionsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		configSyntax();
		initSearchControl();
		initAutocompleteEXPRService();
		autoCompleteEXPRui = new AutoCompleteBehaviour<>();
		configEXPRAutocomplete();
	}
	
	public XBMainPanel(ColumnEditor host) {
		Dialog owner = null;
		if (host != null) {
			owner = host.getDlg();
		}
		this.hostEditor = host;
		ColumnSearchPanel.createWrapperDialog(txtSearch, owner);
		createUI();
		configEvents();		
	}
		
	private static void loadQuickTips() {
		quickTips = Main.getResourceAsString(PATH_TO_QUICK_TIPS);	
	}
	
	private static void loadFuncHelp() {
		funcHelp = Main.getResourceAsString(PATH_TO_FUNC_HELP);		
	}
	
	public XBMainPanel(Dialog owner) {
		ColumnSearchPanel.createWrapperDialog(txtSearch, owner);
		createUI();
		configEvents();
	}

	private void configEvents() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent ev) {
				ColumnSearchPanel.closeSearchDialog();
			}
		});
	}

	public static JTextArea getTextExpr() {
		return textExpr;
	}
	
	public static JPanel getResultTab() {
		return resultTab;
	}
	
	public static JPanel getErrorTab() {
		return errorTab;
	}	

	public static JTabbedPane getResultPreviewTabbedPane() {
		return tabbedpane;
	}
	

	private static void configSyntax() {
		textExpr.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		org.fife.ui.rsyntaxtextarea.Theme theme;
		try {
			String uitheme = Config.get(Config.CONFIG_UI_THEME, GUITheme.DARK.asString());
			theme = ("dark".equalsIgnoreCase(uitheme)) ? 
						org.fife.ui.rsyntaxtextarea.Theme.load(Main.getResourceAsStream("resources/xb/syntax.dark.xml")) :
						org.fife.ui.rsyntaxtextarea.Theme.load(Main.getResourceAsStream("resources/xb/syntax.light.xml"));
			theme.apply(textExpr);
		} catch (IOException e) {
			e.printStackTrace();
		}
		textExpr.setCaretPosition(0);
		textExpr.setCodeFoldingEnabled(false);
		textExpr.setLineWrap(true);
		textExpr.setWrapStyleWord(true);
        XBFocusHandler focusHandler = new XBFocusHandler(UserPromptFormula.ON_FORMULA_START.asString(), textExpr);
        textExpr.addMouseListener(focusHandler);
        XBKeyListener xbkeylistener = new XBKeyListener(UserPromptFormula.ON_FORMULA_START.asString(), textExpr);
		textExpr.addKeyListener(xbkeylistener);
		configTextAreaKeys(textExpr);		
	}

	private static void configEXPRAutocomplete() {
		autoCompleteEXPRui.setCallback(myAutoCompleteCallback);
		// set a custom renderer for our proposals
		DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Component result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				list.setBackground(((ThemePanel) GUITheme.get("panel")).getBackground());
				list.setSelectionBackground(((ThemePanel) GUITheme.get("panel")).getBackgroundhi());
				list.setSelectionForeground(((ThemeText) GUITheme.get("text")).getTextcolor());
				String s = (String) value;
				int dashIndex = s.indexOf(DynamicData.USER_DATA_IDENTIFIER);
				Font regularFont = getFont();
				Font disabledFont = regularFont.deriveFont(Font.PLAIN);
				disabledFont = disabledFont.deriveFont(disabledFont.getSize() - 2.0f);				
				if (dashIndex != -1) {
					String str = formatUserColumn(s, disabledFont, dashIndex);
					setText(str);
				} else {
					setText("<html><span>" + s + "</span></html>");
				}
				return result;
			}

			private String formatUserColumn(String s, Font disabledFont, int dashIndex) {
				StringBuilder strbuilder = new StringBuilder();
				Color gold = ((ThemePanel) GUITheme.get("panel")).getSelectionhi();
				String colorhex = edu.asu.jmars.ui.looknfeel.Utilities.getColorAsBrowserHex(gold);
				strbuilder.append("<html><span>");
				strbuilder.append(s.substring(0, dashIndex));
				strbuilder.append("</span>");
				setFont(disabledFont);
				strbuilder.append("<span style=\"color:");
				strbuilder.append(colorhex);
				strbuilder.append("\">");
				strbuilder.append(s.substring(dashIndex));
				strbuilder.append("</span></html>");
				return strbuilder.toString();
			}
		};
		autoCompleteEXPRui.setListCellRenderer(renderer);
		// setup initial size
		autoCompleteEXPRui.setInitialPopupSize(new Dimension(250, 300));
		// how many proposals to display before showing a scroll bar
		autoCompleteEXPRui.setVisibleRowCount(5);
		// attach autocomplete to editor
		autoCompleteEXPRui.attachTo(textExpr);
	}	

	private static void initAutocompleteEXPRService() {
		XBPolicyCreator xbpolicycreator = XBPolicyCreator.Instance();
		autocompleteEXPRservice = new AutoCompleteService(xbpolicycreator);
		myAutoCompleteCallback = new MyAutoCompleteCallback(autocompleteEXPRservice);
	}	

	private void createUI() {
		setLayout(new BorderLayout(0, 0));
		
		JPanel maincontent = createMaincontentPanel();
		
		addINSERTCOLUMNSLabel(maincontent);		
		
		addSearchColumnsControl(maincontent);
		
		addFormulaLabel(maincontent);	
		
		JPanel labelwithiconpanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 0));
		labelwithiconpanel.setBorder(new EmptyBorder(0, -30, 0, 0));

		addFunctionShortcutsLabel(labelwithiconpanel);
		
		addKeyboardShortcutsLabel(labelwithiconpanel);
		
		addFormulaHelpLabel(labelwithiconpanel);
		
		addLabelsWithIconsToMainContentPanel(maincontent, labelwithiconpanel);
		
		addFunctionShortcuts(maincontent);		
		
		addFormulaInputControl(maincontent);
		
		addColumnSyntaxInfo(maincontent);

		addResultPreviewControl(maincontent);

		//addControlButtons(maincontent);		
	}	


	private JPanel createMaincontentPanel() {
		JPanel maincontent = new JPanel();
		maincontent.setPreferredSize(new Dimension(600, 450));
	//	maincontent.setBackground(Color.YELLOW);
		maincontent.setBorder(new EmptyBorder(5, 30, 10, 30));
		add(maincontent, BorderLayout.CENTER);
		GridBagLayout gbl_maincontent = new GridBagLayout();
		maincontent.setLayout(gbl_maincontent);
		return maincontent;
	}
	
	private void addINSERTCOLUMNSLabel(JPanel maincontent) {
		JPanel p1 = new JPanel(new BorderLayout());
		JLabel lblInsertColumns = new FontSizeHeaderLabel("<html><div>INSERT&nbsp;COLUMNS</div></html>", Font.BOLD, smallFontSize);
		lblInsertColumns.setHorizontalAlignment(SwingConstants.LEFT);
		p1.add(lblInsertColumns, BorderLayout.WEST);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.REMAINDER;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 0, 5, 0);
		gbc.gridx = 0;
		gbc.gridy = 0;
		maincontent.add(p1, gbc);		
	}	

	private void addSearchColumnsControl(JPanel maincontent) {
		//columns search with "search icon"
		JPanel p3 = new JPanel();
        GroupLayout inputGL = new GroupLayout(p3);
        p3.setLayout(inputGL);
        inputGL.setHorizontalGroup(inputGL.createSequentialGroup()
        	.addComponent(txtSearch)
        	.addComponent(clearSearchTextBtn));
        inputGL.setVerticalGroup(inputGL.createSequentialGroup()
        	.addGroup(inputGL.createParallelGroup(Alignment.BASELINE)
        		.addComponent(txtSearch)
        		.addComponent(clearSearchTextBtn)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 0, 5, 1);
		gbc.gridx = 0;
		gbc.gridy = 1;
		maincontent.add(p3, gbc);	
	}
	
	private void addFormulaLabel(JPanel maincontent) {
		//FORMULA label
		JPanel p4 = new JPanel(new BorderLayout());		
		JLabel lblExpression = new JLabel("Formula".toUpperCase());
		lblExpression.setHorizontalAlignment(SwingConstants.LEFT);
		p4.add(lblExpression, BorderLayout.WEST);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(15, 0, 0, 0);
		gbc.gridx = 0;
		gbc.gridy = 2;
		maincontent.add(p4, gbc);		
	}	
	
	private void addFunctionShortcutsLabel(JPanel lblpanel) {
		JPanel p2 = new JPanel(new BorderLayout());
		JLabel lblFunctions = new JLabel("<html><div>AVAILABLE&nbsp;FEATURES&nbsp;</div></html>", helpicon, SwingConstants.LEFT);
		lblFunctions.setFont(new Font(ThemeFont.getFontFamily(), Font.BOLD, smallFontSize));
		lblFunctions.setHorizontalTextPosition(SwingConstants.LEADING);
		ShowHelpMouseAdapter helpadapter = new ShowHelpMouseAdapter(hostEditor,lblFunctions, funcHelp);
		lblFunctions.addMouseListener(helpadapter); 		
		p2.add(lblFunctions, BorderLayout.WEST);		
		lblpanel.add(p2);		
	}
	
	private void addFormulaHelpLabel(JPanel lblpanel) {
		JPanel p5 = new JPanel(new BorderLayout());
		lblexprhelp = new JLabel("<html><div>FORMULA&nbsp;HELP&nbsp;</div></html>",  helpicon, SwingConstants.LEFT);
		lblexprhelp.setFont(new Font(ThemeFont.getFontFamily(), Font.BOLD, smallFontSize));
		lblexprhelp.setHorizontalTextPosition(SwingConstants.LEADING);
		lblexprhelp.setToolTipText("Click here to get quick tips for building a formula");
		ShowHelpMouseAdapter helpadapter = new ShowHelpMouseAdapter(hostEditor,lblexprhelp, quickTips);
		lblexprhelp.addMouseListener(helpadapter);          
		p5.add(lblexprhelp, BorderLayout.WEST);		
		lblpanel.add(p5);			
	}
	
	private void addKeyboardShortcutsLabel(JPanel lblpanel) {
		JPanel p55 = new JPanel(new BorderLayout());
		JLabel lblkbs = new JLabel("<html><div>KEYBOARD&nbsp;SHORTCUTS&nbsp;</div></html>",  keyboardicon, SwingConstants.LEFT);  //keyboard icon goes here
		lblkbs.setFont(new Font(ThemeFont.getFontFamily(), Font.BOLD, smallFontSize));
		lblkbs.setHorizontalTextPosition(SwingConstants.LEADING);
		lblkbs.setToolTipText("Keyboard shortcuts when building a formula");
		ShowHelpMouseAdapter helpadapter = new ShowHelpMouseAdapter(hostEditor, lblkbs, keyboardHelp);
		lblkbs.addMouseListener(helpadapter);          
		p55.add(lblkbs, BorderLayout.CENTER);		
		lblpanel.add(p55);		
	}
	
	private void addLabelsWithIconsToMainContentPanel(JPanel maincontent, JPanel lblpanel) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 0.0;
		gbc.weighty = 0.0;
		gbc.insets = new Insets(1, 0, 5, 20);
		gbc.gridx = 0;
		gbc.gridy = 3;
		maincontent.add(lblpanel, gbc);
	}	
	
	private void addFunctionShortcuts(JPanel maincontent) {
		//MATH buttons
		JPanel functionspanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5,0));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(5, -10, 5, 0);
		gbc.weightx = 0.0;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.gridx = 0;
		gbc.gridy = 4;
		createMathButtons(functionspanel);
		
		JLabel calculatorLabel = new JLabel("more...");
        calculatorLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openCalculatorDialog(calculatorLabel);
            }
        });	

        JPanel functionspanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        functionspanel2.add(calculatorLabel);
        functionspanel.add(functionspanel2);      
        
		maincontent.add(functionspanel, gbc);		
	}	
	
	private void addFormulaInputControl(JPanel maincontent) {
		//Text area for entering Formula
		JPanel p6 = new JPanel(new BorderLayout());
		p6.setBorder(new LineBorder(imgColor, 2));
		p6.add(expressionsp, BorderLayout.CENTER);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.gridwidth = 10;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(1, 0, 10, 0);
		gbc.gridx = 0;
		gbc.gridy = 5;
		maincontent.add(p6, gbc);		
	}
	
	private void  addColumnSyntaxInfo(JPanel maincontent) {
		JPanel p99 = new JPanel(new BorderLayout());
		ImportantMessagePanel panelImportantMsg = new ImportantMessagePanel("When using column names in your formula, enclose them with colons.");
		JPanel msgpanel = new JPanel(new BorderLayout());
		msgpanel.add(panelImportantMsg);
		Insets in4 = new Insets(1,1,1,1);
		p99.add(panelImportantMsg, BorderLayout.CENTER);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.gridwidth = 5;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(1, 0, 5, 0);
		gbc.gridx = 0;
		gbc.gridy = 6;
		maincontent.add(p99, gbc);			
	}	
	
	private void addResultPreviewControl(JPanel maincontent) {
		JPanel p8 = new JPanel(new BorderLayout());
		//p8.setBackground(Color.PINK);
		tabbedpane.setBackground(((ThemePanel)GUITheme.get("panel")).getBordercolor());
		resultTab.setLayout(new GridLayout(1,1));		
		//resultTab.setBackground(Color.RED);
		tabbedpane.addTab("Result Preview", resultTab);		
		errorTab.setLayout(new GridLayout(1,1));
		//errorTab.setBackground(Color.ORANGE);
		tabbedpane.addTab("Warnings", errorTab);		
		p8.add(tabbedpane, BorderLayout.CENTER);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.gridwidth = 10;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(1, 0, 5, 0);
		gbc.gridx = 0;
		gbc.gridy = 7;
		maincontent.add(p8, gbc);		
	}	
	
	private void addControlButtons(JPanel maincontent) {
		JPanel buttonspanel = new JPanel(new BorderLayout());
		buttonspanel.setBackground(((ThemePanel)GUITheme.get("panel")).getBackgroundaltcontrastbright());
		buttonspanel.setBorder(new EmptyBorder(10, 15, 10, 15));
		buttonspanel.setMinimumSize(new Dimension(565, 50));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.weighty = 0.0;
		gbc.gridwidth = 10;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridx = 0;
		gbc.gridy = 7;
		close = new JButton("CLOSE");
		close.addActionListener(e -> closeAction(e));
		apply = new JButton("APPLY");
		apply.setUI(new LikeDefaultButtonUI());
		apply.addActionListener(e -> applyAction(e));
		buttonspanel.add(close, BorderLayout.WEST);
		buttonspanel.add(apply, BorderLayout.EAST);
		maincontent.add(buttonspanel, gbc);		
	}	

	private void createMathButtons(JPanel functionspanel) {
		//String divSymbol = Character.toString('\u00F7');		
		buttonABS = new JButton("abs");
		reduce(buttonABS);		
		buttonABS.setToolTipText("get absolute value of a number");
		buttonABS.addActionListener(e -> ABSAction(e));
		functionspanel.add(buttonABS);

		buttonLOG = new JButton("log");
		reduce(buttonLOG);
		buttonLOG.setToolTipText("get natural logarithm (base e) of a number");
		buttonLOG.addActionListener(e -> LOGAction(e));
		functionspanel.add(buttonLOG);
		
		buttonSIN = new JButton("sin");
		reduce(buttonSIN);
		buttonSIN.setToolTipText("get trigonometric sine of an angle");
		buttonSIN.addActionListener(e -> SINAction(e));
		functionspanel.add(buttonSIN);
		
		buttonMAX = new JButton("max");	
		reduce(buttonMAX);
		buttonMAX.setToolTipText("get the greater of two numbers");
		buttonMAX.addActionListener(e -> MAXAction(e));
		functionspanel.add(buttonMAX);
		
		JButton buttonUNDO = new JButton("undo");
		reduce(buttonUNDO);
		buttonUNDO.setToolTipText("undo typing");
		AbstractAction myundoaction = new UNDOAction(textExpr);
		buttonUNDO.addActionListener(myundoaction);
		functionspanel.add(buttonUNDO);
		
		JButton buttonREDO = new JButton("redo");
		reduce(buttonREDO);
		buttonREDO.setToolTipText("redo typing");
		AbstractAction myredoaction = new REDOAction(textExpr);
		buttonREDO.addActionListener(myredoaction);
		functionspanel.add(buttonREDO);
		
		
		buttonClearALL = new JButton("clear");	
		reduce(buttonClearALL);
		buttonClearALL.setToolTipText("<html><div>clear all text entered for this Formula</div>"
				+ "<div>Use 'undo' in case 'clear' action was triggered in error</div></html>");
		buttonClearALL.addActionListener(e -> ClearAction(e));
		functionspanel.add(buttonClearALL);
	}		


	private void reduce(JButton button) {
		Dimension dim = button.getPreferredSize();
		int newwidth = dim.width;
        int newheight = (dim.height -10) >= 10 ? dim.height -10 : dim.height;
        button.setPreferredSize(new Dimension(newwidth, newheight));			
	}

	private void openCalculatorDialog(JLabel parentcomponent) {
	 	Calculator calc = new Calculator(textExpr); 
	 	Window owner = (hostEditor == null) ? null : hostEditor.getDlg();
        JDialog dialog = new JDialog(owner, "Function Shortcuts", ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.add(calc, BorderLayout.CENTER);
        dialog.pack();
        java.awt.Point panelLocation = this.getLocationOnScreen();
        int dialogX = panelLocation.x;
        int dialogY = panelLocation.y - 100;        
        dialog.setLocation(dialogX, dialogY);
        dialog.setVisible(true);	
	}

	private void ABSAction(ActionEvent e) {
		insertText(" abs(0) ");		
	}	

	private void LOGAction(ActionEvent e) {
		insertText(" log(0) ");
	}
	
	private void SINAction(ActionEvent e) {
		insertText(" sin(0) ");
	}	
	
	private void MAXAction(ActionEvent e) {
		insertText(" max(0,0) ");
	}	
	
	private void ClearAction(ActionEvent e) {
		SwingUtilities.invokeLater(() -> textExpr.setText(""));  
	}
	
	
	private void insertText(String txt) {
		if (textExpr != null) {
			int pos = textExpr.getCaretPosition();
			if (pos >= 0) {
				XBMainPanel.INSERT_TEXT.insertAndRemove(textExpr, txt);
			} 
		}
	}	

	private void closeAction(ActionEvent e) {
		XB.INSTANCE.hide();
	}

	private void applyAction(ActionEvent e) {
		if (this.hostEditor == null) { return; }
		this.hostEditor.validate();
	}

	private static void configTextAreaKeys(RSyntaxTextArea textArea) {
		textArea.setFont(exprFont);
		InputMap im = textArea.getInputMap();
		ActionMap am = textArea.getActionMap();
		
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "decreaseFontSize");
		am.put("decreaseFontSize", new RSyntaxTextAreaEditorKit.DecreaseFontSizeAction());
		
		im = textArea.getInputMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "increaseFontSize");
		am = textArea.getActionMap();
		am.put("increaseFontSize", new RSyntaxTextAreaEditorKit.IncreaseFontSizeAction());
	}

	private static void initSearchControl() {
		txtSearch.setMaximumSize(new Dimension(ColumnSearchPanel.PREF_WIDTH, 33));
		txtSearch.setMinimumSize(new Dimension(ColumnSearchPanel.PREF_WIDTH, 33));
		Icon search = new ImageIcon(ImageFactory.createImage(SEARCH2));
		Icon searchSelected = new ImageIcon(ImageFactory.createImage(SEARCH2_SEL));
		txtSearch.setIcon(search).setSelectedIcon(searchSelected);	
		txtSearch.getIconContainer().addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ColumnSearchPanel.showHideSearchInput(txtSearch, txtSearch.getIconContainer());		
			}
		});
		
		clearSearchTextBtn.setIcon(clearicon);
		clearSearchTextBtn.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 1));
		clearSearchTextBtn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				ColumnSearchPanel.allowFilter = true;
				columnSearchInput.setText(ENTER_KW);
				columnSearchInput.grabFocus();
			}
		});
		clearSearchTextBtn.addKeyListener(new ClearButtonKeyListener());
	}

	private static class ClearButtonKeyListener extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				ColumnSearchPanel.allowFilter = true;
				columnSearchInput.setText(ENTER_KW);
				columnSearchInput.grabFocus();
			}
		}
	}
	
	private static class ShowHelpMouseAdapter extends MouseAdapter {
		private ColumnEditor hostEditor;
		private JLabel sourceofclickLabel;
		private String helptext;
		
		public ShowHelpMouseAdapter(ColumnEditor hostcomponent, JLabel wheretoclick, String strtext) {
		  hostEditor = hostcomponent;
		  sourceofclickLabel = wheretoclick;
		  helptext = strtext;
	    }
		
		@Override
          public void mouseClicked(MouseEvent e) {
          	openQuickTipsDialog();
          }            

		private void openQuickTipsDialog() {
			Window owner = (hostEditor == null) ? null : hostEditor.getDlg();
			JDialog dialog = new JDialog(owner, "Formula - Help Topics", ModalityType.MODELESS);
			dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			dialog.setResizable(true);				
			JLabel quicktipsLabel = new JLabel(helptext);
			quicktipsLabel.setHorizontalAlignment(SwingConstants.LEFT);
			quicktipsLabel.setVerticalAlignment(SwingConstants.TOP);
			quicktipsLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			quicktipsLabel.setHorizontalAlignment(SwingConstants.LEFT);
			quicktipsLabel.setVerticalAlignment(SwingConstants.TOP);
			quicktipsLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			quicktipsLabel.setForeground(Color.BLACK);
			Font helpfont = new Font(ThemeFont.getFontFamily(), Font.PLAIN, 20);
			quicktipsLabel.setFont(helpfont);
			JPanel p = new JPanel(new BorderLayout());
			p.setBackground(Color.LIGHT_GRAY);
			p.add(quicktipsLabel, BorderLayout.CENTER);
			
			JScrollPane scrollPane = new JScrollPane(p);				
			scrollPane.setPreferredSize(new Dimension(800, 400));

			dialog.getContentPane().add(scrollPane);
			dialog.pack();
			java.awt.Point panelLocation = sourceofclickLabel.getLocationOnScreen();
			int dialogX = panelLocation.x - 300;
			int dialogY = panelLocation.y - 250;
			dialog.setLocation(dialogX, dialogY);
			dialog.setVisible(true);
		}	
	}
	
	 private static class XBFocusHandler extends MouseAdapter {
	        private final String defaultText;
	        private final JTextArea textArea;

	        public XBFocusHandler(String defaultText, JTextArea textArea) {
	            this.defaultText = defaultText;
	            this.textArea = textArea;
	        }

	        @Override
	        public void mousePressed(MouseEvent e) {
	        	if (javax.swing.SwingUtilities.isRightMouseButton(e)) {  //context menu, do nothing
	        		return;
	        	}
	        	if (textArea.getText() != null) {
					if (textArea.getText().equals(defaultText)) {
						SwingUtilities.invokeLater(() -> textArea.setText(""));  
					}
				}	         
	        }		
	    }
	 
		private static class XBKeyListener extends KeyAdapter {
			private final String defaultText;
			private final JTextArea textArea;

			public XBKeyListener(String defaultText, JTextArea textArea) {
				this.defaultText = defaultText;
				this.textArea = textArea;
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (textArea.getText() != null) {
					if (textArea.getText().equals(defaultText)) {
						SwingUtilities.invokeLater(() -> textArea.setText(""));  
					}
				}
			}
		}
	 
		private class UNDOAction extends AbstractAction {
			UndoAction myundo;
			RSyntaxTextArea richsyntaxtextarea;

			UNDOAction(RSyntaxTextArea textExpr) {
				richsyntaxtextarea = textExpr;
				myundo = new RTextAreaEditorKit.UndoAction();
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				myundo.actionPerformedImpl(e, richsyntaxtextarea);
			}
		}

		private class REDOAction extends AbstractAction {
			RedoAction myredo;
			RSyntaxTextArea richsyntaxtextarea;

			REDOAction(RSyntaxTextArea textExpr) {
				richsyntaxtextarea = textExpr;
				myredo = new RTextAreaEditorKit.RedoAction();
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				myredo.actionPerformedImpl(e, richsyntaxtextarea);
			}
		}	
	
	static class TextAreaInsert {

		public void insertAndRemove(JTextArea textarea, String newtext) {
			if (textarea == null) {
				return;
			}
			int pos = textarea.getCaretPosition(); // get the cursor position
			if (pos >= 0) {
				String delimstring = newtext;
				String currenttext = textarea.getText();
				if (currenttext != null && currenttext.length() > 0) {
					int startPos = currenttext.indexOf(UserPromptFormula.ON_FORMULA_START.asString());
					if (startPos >= 0) {
						int endPos = startPos + UserPromptFormula.ON_FORMULA_START.asString().length();
						SwingUtilities.invokeLater(() -> {
							textarea.replaceRange("", startPos, endPos);
							textarea.insert(delimstring, startPos); // add selected column
						});
					} else {
						SwingUtilities.invokeLater(() -> textarea.insert(delimstring, pos));  //add selected column
					}
				} else {
					SwingUtilities.invokeLater(() -> textarea.insert(delimstring, pos));  //add selected column
				}
			}
		}
	}
}


