package edu.asu.jmars;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.RIGHT_LINK_IMG;
import static edu.asu.jmars.ui.looknfeel.Utilities.getColorAsBrowserHex;
import static edu.asu.jmars.ui.looknfeel.Utilities.scaleImageToHeight;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ComponentUI;
import org.jsoup.Jsoup;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import edu.asu.jmars.swing.SocialMediaPanel;
import edu.asu.jmars.swing.UrlLabel;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeButton;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeSeparator;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeText;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import mdlaf.components.button.MaterialButtonUI;
import mdlaf.utils.MaterialManagerListener;



@SuppressWarnings("serial")
public class LoginWindow2 extends JFrame {
	private JPanel headerPnl;
	private JLabel logoLbl;
	private JLabel versionLbl;
	private JButton updateBtn;
	private JLabel signInLbl;
	private JLabel usernameLbl;
	private JTextField usernameTF;
	private JLabel passwordLbl;
	private JPasswordField passwordPF;
	private JLabel bodyLbl;
	private JComboBox<String> bodyBx;
	private JCheckBox autosaveChk;
	private JCheckBox disable3DChk;
	private JLabel advancedLbl;
	private JButton signInBtn;
	private GuestButton guestBtn;
	private UrlLabel resetPassULbl;
	private UrlLabel createAccountULbl;
	private JLabel announcementsLbl;
	private JPanel announcementsPnl;
	private String loginSelectedBody = null;
	private boolean fromStartupFile = false;
	private static boolean initialize3D = true;
	private boolean hideMacMsg = false;
	private JDialog advancedDlg;
	
	private Map<TextAttribute, Object> spacingAtt1 = new HashMap<TextAttribute, Object>();
	private Map<TextAttribute, Object> spacingAtt2 = new HashMap<TextAttribute, Object>();	
	
	private static DebugLog log = DebugLog.instance();
	
	Color imgLinkColor = ((ThemeImages) GUITheme.get("images")).getLinkfill();
	
	Image arrowImg =  ImageFactory.createImage(RIGHT_LINK_IMG
            		  .withDisplayColor(imgLinkColor).withWidth(11).withHeight(20));          		 
			         
	private int pad = 5;
	private Insets in = new Insets(pad, pad, pad, pad);
	private int row, col;
	
	public LoginWindow2(boolean fromFile){
		setIconImage(Util.getNonWhiteJMARSIcon());
		setResizable(false);
		
		//override the close action to ask for comfirmation first
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		//close the entire app if the user closes the dialog
		addWindowListener(new java.awt.event.WindowAdapter() {
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		    	System.exit(0);
		    }
		});
		
		fromStartupFile = fromFile;
		testMacFileAccess();
		buildUI();
		layoutAdvancedDialog();
		setLocationRelativeTo(null);
	}
	private void testMacFileAccess() {
		if (Main.MAC_OS_X) {
			hideMacMsg = Config.get("mac_file_msg_hide", false);
		}
	}
	public static boolean getInitialize3DFlag() {
		return initialize3D;
	}
	private void buildUI(){
		JPanel mainPnl = new JPanel(new BorderLayout());
		mainPnl.setBorder(new EmptyBorder(0, 0, 15, 0));
		
		Color themecolor = ThemeProvider.getInstance().getAction().getMain();
		Color imgColor = ((ThemeImages) GUITheme.get("images")).getCommonFill();		
		Color textColor = ((ThemeText) GUITheme.get("text")).getTextcolor();
		
		//font spacing attribute
		spacingAtt1.put(TextAttribute.TRACKING, 0.07);
		spacingAtt2.put(TextAttribute.TRACKING, 0.04);
		
		headerPnl = new JPanel(new BorderLayout());
		headerPnl.setBorder(new EmptyBorder(20, 20, 20, 40));
		headerPnl.setBackground(themecolor);
		ImageIcon logo = new ImageIcon(scaleImageToHeight(Util.getJMarsLargeLogoWhite(), 72));
		logoLbl = new JLabel(logo);
		logoLbl.setBackground(themecolor);
		versionLbl = new JLabel("Version "+Util.getVersionNumber());
		versionLbl.setBackground(themecolor);
		versionLbl.setForeground(ThemeProvider.getInstance().getAction().getDefaultForeground());  //color same as button
		versionLbl.setFont(ThemeFont.getBold().deriveFont(15f).deriveFont(spacingAtt1));
		updateBtn = new UpdateButton(updatesAct);		
		updateBtn.setFocusable(false);
		updateBtn.setFont(ThemeFont.getRegular().deriveFont(13f));		
		ImageIcon updateIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.SYNC_IMG
				               .withDisplayColor(imgColor).withWidth(12).withHeight(12)));				             
		updateBtn.setIcon(updateIcon);
		updateBtn.setHorizontalTextPosition(SwingConstants.LEFT);
		updateBtn.setIconTextGap(10);
		
		JPanel hEastPnl = new JPanel(new GridBagLayout());
		hEastPnl.setBackground(themecolor);
		hEastPnl.add(versionLbl, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5,0,0,15), 0, 0));
		hEastPnl.add(updateBtn, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
		headerPnl.add(logoLbl, BorderLayout.WEST);
		headerPnl.add(hEastPnl, BorderLayout.EAST);
		
		JPanel centerPnl = new JPanel(new GridBagLayout());
		centerPnl.setBorder(new EmptyBorder(10, 5, 0, 5));
		
		JPanel leftPnl = new JPanel(new GridBagLayout());
		leftPnl.setBorder(new EmptyBorder(0, 10, 0, 10));
		signInLbl = new JLabel("SIGN INTO JMARS");
		signInLbl.setFont(ThemeFont.getBold().deriveFont(19f).deriveFont(spacingAtt2));
		usernameLbl = new JLabel("EMAIL ADDRESS");
		usernameLbl.setFont(ThemeFont.getRegular().deriveFont(spacingAtt1));
		usernameTF = new JTextField(22);
		usernameTF.getDocument().addDocumentListener(fieldListener);
		passwordLbl = new JLabel("PASSWORD");
		passwordLbl.setFont(ThemeFont.getRegular().deriveFont(spacingAtt1));
		passwordPF = new JPasswordField(22);
		passwordPF.getDocument().addDocumentListener(fieldListener);
		bodyLbl = new JLabel("CELESTIAL BODY");
		bodyLbl.setFont(ThemeFont.getRegular().deriveFont(spacingAtt1));
		Vector<String> bodyVec = new Vector<String>();
		for (String[] bodies : Util.getBodyList().values()){
			for(String body : bodies){
				bodyVec.add(body.toUpperCase());
			}
		}
		//alphabetize list
		Collections.sort(bodyVec);
		bodyBx = new JComboBox<String>(bodyVec);
		String currentBody = Main.getCurrentBody();//could be set from a session file or config
		if (currentBody == null) {//wasn't set before calling the login window
			currentBody = Config.get(Config.CONFIG_SELECTED_BODY, "mars");
		} else {
			if (fromStartupFile) {
				//if we had a body set in main, and the flag from startup file is true, set the body selection disabled
				bodyBx.setEnabled(false);
			}
			bodyBx.setToolTipText("Using body from startup session file or JLF.");
		}
		bodyBx.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String body = (String)bodyBx.getSelectedItem();
				body = body.toLowerCase();
				setTitle(Main.checkDemoOrBeta()+Config.get(body+".edition"));
			}
		});
		
		
		advancedLbl = new JLabel("Advanced");
		advancedLbl.setFont(ThemeFont.getRegular().deriveFont(spacingAtt1));
		advancedLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		advancedLbl.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				advancedDlg.setVisible(true);
			}
			
		});
		
		disable3DChk = new JCheckBox(new AbstractAction("Disable 3D") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean disabled = false;
				if (disable3DChk.isSelected()) {
					disabled = true;
				}
				Config.set(Config.CONFIG_DISABLE_3D, disabled);
			}
		});
		disable3DChk.setFont(ThemeFont.getRegular().deriveFont(spacingAtt1));
		disable3DChk.setToolTipText("If you are having trouble initializing 3D when starting JMARS, check this box to use JMARS with no 3D capabilities.");
		
		disable3DChk.setSelected(Config.get(Config.CONFIG_DISABLE_3D, false));
		
		bodyBx.setSelectedItem(currentBody.toUpperCase());
		setTitle(Main.checkDemoOrBeta()+Config.get(currentBody+".edition"));
		autosaveChk = new JCheckBox("Restore Autosave");
		autosaveChk.setFont(ThemeFont.getRegular().deriveFont(spacingAtt1));
		//check to see if an autosave file exists
		File autosaveFile = Main.autosaveFile;
		if (autosaveFile.lastModified() > 0) {
			autosaveChk.setToolTipText("Last saved " + new SimpleDateFormat("EEE h:mm aaa, d MMM yyyy").format(new Date(Main.autosaveFile.lastModified())));
		} else {
			//the following code is to support old default.jlf files that may exist from previous versions
			File oldAutosaveFile = new File(Main.getJMarsPath() + "default.jlf");
			if (oldAutosaveFile.exists() && oldAutosaveFile.canRead()) {
				oldAutosaveFile.renameTo(autosaveFile);
				autosaveChk.setToolTipText("Last saved " + new SimpleDateFormat("EEE h:mm aaa, d MMM yyyy").format(new Date(Main.autosaveFile.lastModified())));
				//end support for previous versions
			} else {
				autosaveChk.setToolTipText("No autosave file found");
				autosaveChk.setEnabled(false);
			}
		}
		
		signInBtn = new JButton(signInAct);
		signInBtn.setEnabled(false);
		signInBtn.setFont(ThemeFont.getBold().deriveFont(spacingAtt1));
		guestBtn = new GuestButton(guestAct);
		guestBtn.setFont(ThemeFont.getBold().deriveFont(spacingAtt1));
		resetPassULbl = new UrlLabel("Forgot Password?", Config.get("passwordpage"), getColorAsBrowserHex(textColor), new ImageIcon(arrowImg));
		resetPassULbl.setFont(ThemeFont.getRegular().deriveFont(Font.BOLD).deriveFont(spacingAtt1));
		createAccountULbl = new UrlLabel("Create account", Config.get("registrationpage"), getColorAsBrowserHex(textColor), new ImageIcon(arrowImg));
		createAccountULbl.setFont(ThemeFont.getRegular().deriveFont(Font.BOLD).deriveFont(spacingAtt1));
		
		row = 0; col = 0;
		leftPnl.add(signInLbl, new GridBagConstraints(col, row, 2, 1, 0, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0,pad,2*pad,pad), pad, pad));
		row++;
		leftPnl.add(usernameLbl, new GridBagConstraints(col, row, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, in, pad, pad));
		row++;
		leftPnl.add(usernameTF, new GridBagConstraints(col, row, 2, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,pad,2*pad,pad), pad, pad));
		row++;
		leftPnl.add(passwordLbl, new GridBagConstraints(col, row, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, in, pad, pad));
		row++;
		leftPnl.add(passwordPF, new GridBagConstraints(col, row, 2, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,pad,pad,pad), pad, pad));
		row++;
		
		//if *we* do the authentication, then show the reset password link
		if(Config.get("auth_domain", Main.MSFF).equalsIgnoreCase(Main.MSFF)){
			leftPnl.add(resetPassULbl, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,pad,0,pad), pad, pad));
			row++; col=0;
		}
		leftPnl.add(bodyLbl, new GridBagConstraints(col, row, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, in, pad, pad));
		row++;
		leftPnl.add(bodyBx, new GridBagConstraints(col, row, 2, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,pad,2*pad,pad), pad, pad));
		row++;
		leftPnl.add(signInBtn, new GridBagConstraints(col, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2*pad,pad,2*pad,pad), pad, pad));
		//only add guest button if build allows guest login
		if(Config.get("guestlogin", "yes").endsWith("yes")){
			leftPnl.add(guestBtn, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2*pad,pad,2*pad,pad), pad, pad));
		}
		col = 0; row++;
		leftPnl.add(advancedLbl, new GridBagConstraints(col, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, in, pad, pad));
		
		//check whether or not to add the registration link
		if(Config.get("hideRegPageLink", true)){
			leftPnl.add(createAccountULbl, new GridBagConstraints(++col, row, 1, 1, 0, 1, GridBagConstraints.EAST, GridBagConstraints.NONE, in, pad, pad));
		}
		
		announcementsPnl = new JPanel(new GridBagLayout());
		announcementsPnl.setPreferredSize(new Dimension(300, 0));
		announcementsLbl = new JLabel("ANNOUNCEMENTS");
		announcementsLbl.setFont(ThemeFont.getBold().deriveFont(19f).deriveFont(spacingAtt2));
		
		//build the rss announcements panel
		JPanel rssPnl = new JPanel();
		rssPnl.setLayout(new BoxLayout(rssPnl, BoxLayout.PAGE_AXIS));
		rssPnl.setBorder(new EmptyBorder(0, 0, 0, 5));
		String rssUrl = Config.get("rssfeed");
		try {
			SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(rssUrl)));
			
			ArrayList<SyndEntryImpl> list = new ArrayList<SyndEntryImpl>(feed.getEntries());
			
			if (list.size() == 0) {
				rssPnl.add(new JLabel("Welcome to JMARS!"));
			} else {
				int count = 0;
				for(SyndEntryImpl entry: list) {
					//limit to the last 3 updates
					if (count > 2) {
						break;
					}
					Date date = entry.getPublishedDate();
					String fullDesc = Jsoup.parse(entry.getDescription().getValue()).text();
					String desc = truncateString(25, fullDesc)+"...";
					String title = entry.getTitle();
					String link = entry.getLink();
					
					RSSPanel rp = new RSSPanel(date, title, desc, link);
					
					rssPnl.add(rp);
					count++;
				}
			}
		} catch (Exception e) {
			DebugLog.instance().println("Error getting announcements.");
		}
		JScrollPane rssSP = new JScrollPane(rssPnl, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		rssSP.getVerticalScrollBar().setUnitIncrement(20);
		
		pad = 2;
		col=0; row=0;
		announcementsPnl.add(announcementsLbl, new GridBagConstraints(col, row, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(pad, 5, 10, pad), pad, pad));
		announcementsPnl.add(rssSP, new GridBagConstraints(col, ++row, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, in, pad, pad));
		announcementsPnl.add(SocialMediaPanel.get(), new GridBagConstraints(col, ++row, 1, 1, 0, 0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(pad, 0, pad, pad), pad, pad));
		
		JSeparator line = new JSeparator(SwingConstants.VERTICAL);
		line.setBackground(((ThemeSeparator)GUITheme.get("separator")).getAltbackground());
		
		col=0; row=0;
		centerPnl.add(leftPnl, new GridBagConstraints(col, row, 1, 1, 0.5, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		centerPnl.add(line, new GridBagConstraints(++col, row, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, in, pad, pad));
		centerPnl.add(announcementsPnl, new GridBagConstraints(++col, row, 1, 1, 0.5, 0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, in, pad, pad));
		
		
		mainPnl.add(headerPnl, BorderLayout.NORTH);
		mainPnl.add(centerPnl, BorderLayout.CENTER);
		setContentPane(mainPnl);
		
		getRootPane().setDefaultButton(signInBtn);	
		
		//populate the username if it's in the config
		String user = Main.USER;
		usernameTF.setText(user);
		if (user == null || "".equals(user)) {
		    String uid = Config.get("userid","");
		    usernameTF.setText(uid);
		} 
		
		pack();
	}
	
	//This method was created because Java does not honor the requestFocusInWindow() call until after the constructor has returned.
	//Made it a separate call.
	public void setFocus() {
	    if (usernameTF.getText().trim().length() > 0) {
            passwordPF.requestFocusInWindow();
        }
	}
	
	private AbstractAction updatesAct = new AbstractAction("Check for Updates") {
		public void actionPerformed(ActionEvent e) {
			// This will return immediately if you call it from the EDT,
			// otherwise it will block until the installer application exits
			SwingUtilities.invokeLater(Util.getCheckForUpdatesRunnable());
		}
	};
	
	private String truncateString(int numWords, String text){
		String[] words = text.split(" ",0);
		StringBuffer newText = new StringBuffer();
		for(int i=0; i<numWords; i++){
			if (i == words.length) {
				break;//avoid ArrayOutOfBoundsException
			}
			newText.append(words[i]);
			if(i<numWords-1){
				newText.append(" ");
			}
		}
		return newText.toString();
	}
	
	private DocumentListener fieldListener = new DocumentListener() {
		@Override
		public void removeUpdate(DocumentEvent e) {
			updateSignInButtonEnabled();			
		}
		@Override
		public void insertUpdate(DocumentEvent e) {
			updateSignInButtonEnabled();			
		}
		@Override
		public void changedUpdate(DocumentEvent e) {
			updateSignInButtonEnabled();
		}
	}; 
	
	private void updateSignInButtonEnabled(){
		if(usernameTF.getText().length()>0 && passwordPF.getPassword().length>0){
			signInBtn.setEnabled(true);
		}else{
			signInBtn.setEnabled(false);
		}
	}
	
	class UpdateButton extends JButton {
	   
        UpdateButton(Action act) {
            super(act);            
            setContentAreaFilled(true);
            setBorderPainted(false);            
            setUI(new UpdateButtonUI());
        }       
	}
	
	
	class GuestButton extends JButton {	
		GuestButton(Action act){
			super(act);		
		}	
	}

		
	private AbstractAction signInAct = new AbstractAction("SIGN IN") {
		public void actionPerformed(ActionEvent arg0) {
			login(false);
		}
	};
	
	private AbstractAction guestAct = new AbstractAction("CONTINUE AS GUEST") {
		public void actionPerformed(ActionEvent arg0) {
			login(true);
		}
	};
	
	private void login(boolean asGuest){
		if (disable3DChk.isSelected()) {
			LoginWindow2.initialize3D = false;
		}
		String user = usernameTF.getText();
		String pass = new String(passwordPF.getPassword());
		
		if(asGuest){
			user = "";
			pass = "";
		}
		Config.set("userid", user);
		
		loginSelectedBody = (String) bodyBx.getSelectedItem();
		boolean restoreAutoSave = autosaveChk.isSelected();
		
		//hide the login window
		LoginWindow2.this.setVisible(false);
		
		//proceed with opening the application
		Main.logIn(user, pass, loginSelectedBody, restoreAutoSave);
		
		//disable the mainFrame if it exists (sort of make
		// the login window modal)
		//This is necessary if authentication fails while jmars
		// is already running. Need the user to address the
		// login window first before being able to use jmars 
		if(Main.mainFrame != null){
			Main.mainFrame.setEnabled(true);
		}
	}
	
	public String getLoginSelectedBody() {
		return loginSelectedBody;
	}
	
	/**
	 * Sets the location of the login window relative to the
	 * passed in component.  Then sets visible.  Enables the
	 * guest login button based on the passed in boolean,
	 * and disables Main.mainFrame if it exists (this case
	 * would occur if the authentication check failed while
	 * jmars was already running).
	 * 
	 * @param relTo	Component to display login window near
	 * @param allowGuest  Whether to enable the guest login button
	 */
	public void displayWindow(Component relTo, boolean allowGuest){
		setLocationRelativeTo(relTo);
		setVisible(true);
		
		guestBtn.setEnabled(allowGuest);
		
		if(Main.mainFrame != null){
			Main.mainFrame.setEnabled(false);
		}
	}
	
	class RSSPanel extends JPanel {
		private JLabel dateLbl;
		private Date date;
		private JTextArea titleTA;
		private String title;
		private JTextArea descTA;
		private String desc;
		private UrlLabel link;
		
		private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMMM dd, yyy");
		
		RSSPanel(Date date, String title, String desc, String url){
			this.date = date;
			this.title = title;
			this.desc = desc;
			link = new UrlLabel("[read more]", url, null, null);
			
			buildUI();
		}
		
		private void buildUI(){
			setLayout(new GridBagLayout());
			Color themecolor = ((ThemeButton)GUITheme.get("button")).getDefaultback();
			Color primaryback = ((ThemePanel)GUITheme.get("panel")).getBackground();
			Color dateLink = ((ThemeText)GUITheme.get("text")).getDateaslink();
			
			LocalDate lDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			String dateStr = dtf.format(lDate);
			dateLbl = new JLabel(dateStr);
			dateLbl.setForeground(dateLink);
			dateLbl.setFont(ThemeFont.getRegular().deriveFont(12f));
			
			titleTA = new JTextArea(title);
			titleTA.setBorder(BorderFactory.createEmptyBorder(10,0,7,0));
			titleTA.setEditable(false);
			titleTA.setWrapStyleWord(true);
			titleTA.setLineWrap(true);
			titleTA.setFont(ThemeFont.getBold().deriveFont(16f));
			titleTA.setBackground(primaryback);
			
			descTA = new JTextArea(desc);
			descTA.setEditable(false);
			descTA.setWrapStyleWord(true);
			descTA.setLineWrap(true);
			descTA.setFont(ThemeFont.getRegular());
			descTA.setBackground(primaryback);
			
			
			int row = 0;
			add(dateLbl, new GridBagConstraints(0, row++, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 0, 0), 0, 0));
			add(titleTA, new GridBagConstraints(0, row++, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 3, 0), 0, 0));
			add(descTA, new GridBagConstraints(0, row++, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
			add(link, new GridBagConstraints(0, row++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 5, 0), 0, 0));
		}
	}
	
	private static class UpdateButtonUI extends MaterialButtonUI {

		@SuppressWarnings({ "MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration" })
		public static ComponentUI createUI(JComponent c) {
			return new UpdateButtonUI();
		}

		@Override
		public void installUI(JComponent c) {
			super.installUI(c);
			c.setBackground(((ThemeButton)GUITheme.get("button")).getDefaultback());
			super.disabledBackground = ((ThemeButton) GUITheme.get("button")).getDisabledback();
			super.defaultBackground = ((ThemeButton) GUITheme.get("button")).getDefaultback();
			c.setBorder(new EmptyBorder(8, 8, 8, 8));
			MaterialManagerListener.removeAllMaterialMouseListener(c);
		}
	}	
	
	private void layoutAdvancedDialog() {
		advancedDlg = new JDialog(this);
		JPanel mainPanel = new JPanel();
		GroupLayout layout = new GroupLayout(mainPanel);
		mainPanel.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		JLabel autosaveLbl = new JLabel("<html>If JMARS closed unexpectedly, use &quot;Restore Autosave&quot; to<br />attempt to continue from where you left off."
				+ "<br />To save your work, use File->Save Session.</html>");
		JLabel autosaveTitle = new JLabel("<html><h2>Restore Autosave</h2></html>");
		JLabel disable3DTitle = new JLabel("<html><h2>Disable 3D</h2></html>");
		JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
		JLabel disable3DLbl = new JLabel("<html>If your system is not capable of displaying the 3D window,<br> and this causes problems starting JMARS, use "
				+ "this option to start<br />JMARS with no 3D capabilities.</html>");
		JButton closeBtn = new JButton(new AbstractAction("CLOSE") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				advancedDlg.setVisible(false);
			}
		});
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
			.addGroup(layout.createParallelGroup(Alignment.LEADING)
				.addComponent(autosaveTitle)
				.addComponent(autosaveLbl)
				.addComponent(autosaveChk))
			.addGroup(layout.createParallelGroup(Alignment.LEADING)
				.addComponent(disable3DTitle)	
				.addComponent(disable3DLbl)
				.addComponent(disable3DChk))
			.addComponent(sep)
			.addComponent(closeBtn));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(autosaveTitle)
			.addComponent(autosaveLbl)
			.addComponent(autosaveChk)
			.addGap(10)
			.addComponent(sep)
			.addGap(10)
			.addComponent(disable3DTitle)
			.addComponent(disable3DLbl)
			.addComponent(disable3DChk)
			.addGap(10)
			.addComponent(closeBtn));
		
		advancedDlg.setTitle("Advanced Startup Options");
		advancedDlg.setContentPane(mainPanel);
		advancedDlg.setLocationRelativeTo(this);
		advancedDlg.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		advancedDlg.pack();
	}
	
}
