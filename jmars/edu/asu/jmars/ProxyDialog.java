package edu.asu.jmars;

import java.awt.event.ActionEvent;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.awt.Cursor;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;
import edu.asu.jmars.util.ConnectionCheck;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.ProxyInformation;
import edu.asu.jmars.util.Util;

public class ProxyDialog extends JDialog {
	private static ProxyDialog standAloneInstance = null;
	private JLabel proxyMsgLbl = null;
	private JLabel hostLbl = null;
	private JLabel portLbl = null;
	private JLabel userLbl = null;
	private JLabel passwdLbl = null;
	private JTextField hostTf = null;
	private JTextField portTf = null;
	private JTextField userTf = null;
	private JPasswordField passwdTf = null;
	private JButton saveBtn = null;
	private JButton clearBtn = null;
	private JButton reloadBtn = null;
	
	private JPanel saPanel = null;
	private JLabel saTopLbl = null;
	private JLabel saMsg = null;
	private JButton saTestBtn = null;
	private JButton saExitBtn = null;
	private HashMap<String, Boolean> results = null;
	private JLabel saTestResultsLbl = null;
	private JLabel analysis1 = null;
	private JPanel resultPanel = null;
	private JButton saProxyBtn = null;
	private JButton saContinueBtn = null;
	private JPanel proxyPanel = null;
	private JLabel saTopOptionsLbl = null;
	private JPanel buttonPanel = null;

	private JSeparator separator1 = null;
	
	private ProxyInformation proxyInfo = null;
	
	private void initStandaloneComponents() {
		saPanel = new JPanel();
		saTopOptionsLbl = new JLabel("JMARS Optional Proxy Settings");
		saTopLbl = new JLabel("<html><p>JMARS has detected a problem connecting to necessary JMARS servers.<br />"
				+ "There are a number of possible causes including:</p>"
				+ "<ul><li>Your network connection may be temporarily down</li>"
				+ "<li>The JMARS network may be experiencing issues</li>"
				+ "<li>You may be behind a firewall that requires you to enter proxy information</li></ul></html>");
		saMsg = new JLabel("To better understand the issue, click \"Test Connection\" below.");
		saTestBtn = new JButton(saTestConnectionAction);
		saExitBtn = new JButton(exitAction);
		saTestResultsLbl = new JLabel("<html><h2>Connection Test Results</h2></center></html>");
		analysis1 = new JLabel("");
		saProxyBtn = new JButton(saProxyAction);
		saContinueBtn = new JButton(continueAction);
		resultPanel = new JPanel();
		proxyPanel = new JPanel();
		buttonPanel = new JPanel();
		resultPanel.setVisible(false);
		proxyPanel.setVisible(false);
		buttonPanel.setVisible(false);

		separator1 = new JSeparator(SwingConstants.HORIZONTAL);
		
		proxyMsgLbl = new JLabel("Proxy Settings");
		hostLbl = new JLabel("Host: ");
		portLbl = new JLabel("Port: ");
		userLbl = new JLabel("Username: ");
		passwdLbl = new JLabel("Password: ");
		hostTf = new JTextField(15);
		portTf = new JTextField(15);
		userTf = new JTextField(15);
		passwdTf = new JPasswordField(15);
		clearBtn = new JButton(clearAction);
		saveBtn = new JButton(saveAction);
		reloadBtn = new JButton(reloadAction);
		
		//attempt to get proxy information
		loadProxySettings();
	}
	private void buildStandAloneDialog() {
		layoutResultPanel();
		layoutProxyPanel();
		layoutButtonPanel();
		GroupLayout layout = new GroupLayout(saPanel);
		saPanel.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		layout.setHonorsVisibility(true);
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
			.addComponent(saTopLbl)
			.addComponent(saTopOptionsLbl)
			.addComponent(separator1)
			.addComponent(saMsg)
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(proxyPanel)
				.addComponent(resultPanel)
				.addComponent(buttonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)));
		
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(saTopLbl)
			.addComponent(saTopOptionsLbl)
			.addComponent(separator1)
			.addComponent(saMsg)
			.addGap(10)
			.addComponent(proxyPanel)
			.addComponent(resultPanel)
			.addGap(20)
			.addComponent(buttonPanel));
		
		resultPanel.setVisible(false);
		buttonPanel.setVisible(true);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setTitle("JMARS Network Settings");
		setLocationRelativeTo(null);
		setModal(true);
		add(saPanel);
		
	}
	
	private void layoutButtonPanel() {
		GroupLayout layout = new GroupLayout(buttonPanel);
		buttonPanel.setLayout(layout);
		buttonPanel.setBackground(((ThemePanel) GUITheme.get("panel")).getMidContrast());
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		layout.setHorizontalGroup(layout.createSequentialGroup()
			.addComponent(saExitBtn)
			.addComponent(saProxyBtn)
			.addComponent(saTestBtn)
			.addComponent(saContinueBtn));
		layout.setVerticalGroup(layout.createParallelGroup(Alignment.BASELINE)
			.addComponent(saExitBtn)
			.addComponent(saProxyBtn)
			.addComponent(saTestBtn)
			.addComponent(saContinueBtn));
		
	}
	private AbstractAction saProxyAction = new AbstractAction("PROXY SETTINGS") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (resultPanel.isShowing()) {
				resultPanel.setVisible(false);
			}
			proxyPanel.setVisible(true);
			saProxyBtn.setEnabled(false);
			pack();
		}
	};
	private void layoutProxyPanel() {
		GroupLayout layout = new GroupLayout(proxyPanel);
		proxyPanel.setLayout(layout);
		proxyPanel.setBackground(((ThemePanel) GUITheme.get("panel")).getMidContrast());
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(false);
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
			.addComponent(proxyMsgLbl)
			.addGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(hostLbl)
					.addComponent(hostTf)
					.addComponent(userLbl)
					.addComponent(userTf)
					.addComponent(clearBtn))
				.addGap(10)
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(portLbl)
					.addComponent(portTf)
					.addComponent(passwdLbl)
					.addComponent(passwdTf)
					.addGroup(layout.createSequentialGroup()
						.addComponent(reloadBtn)
						.addGap(5)
						.addComponent(saveBtn))))
			.addGroup(layout.createSequentialGroup()
				.addGap(50)
				.addComponent(reloadBtn))
		);
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(proxyMsgLbl)
			.addGap(20)
			.addGroup(layout.createParallelGroup()
				.addComponent(hostLbl)
				.addComponent(portLbl))
			.addGroup(layout.createParallelGroup()
				.addComponent(hostTf)
				.addComponent(portTf))
			.addGap(30)
			.addGroup(layout.createParallelGroup()
				.addComponent(userLbl)
				.addComponent(passwdLbl))
			.addGroup(layout.createParallelGroup()
				.addComponent(userTf)
				.addComponent(passwdTf))
			.addGap(30)
			.addGroup(layout.createParallelGroup(Alignment.BASELINE)
				.addComponent(clearBtn)
				.addComponent(reloadBtn)
				.addComponent(saveBtn))
		);
		
	}
	private void layoutResultPanel() {
		GroupLayout resultLayout = new GroupLayout(resultPanel);
		resultPanel.setLayout(resultLayout);
		resultPanel.setBackground(((ThemePanel)GUITheme.get("panel")).getMidContrast());
		resultLayout.setAutoCreateGaps(false);
		resultLayout.setAutoCreateContainerGaps(true);
		resultLayout.setHorizontalGroup(resultLayout.createParallelGroup(Alignment.LEADING)
			.addComponent(saTestResultsLbl, GroupLayout.PREFERRED_SIZE,GroupLayout.PREFERRED_SIZE,GroupLayout.PREFERRED_SIZE)
			.addComponent(analysis1));
		resultLayout.setVerticalGroup(resultLayout.createSequentialGroup()
			.addComponent(saTestResultsLbl)
			.addComponent(analysis1));
			
	}
	private AbstractAction saTestConnectionAction = new AbstractAction("TEST CONNECTION") {
		@Override
		public void actionPerformed(ActionEvent e) {
			saContinueBtn.setEnabled(true);
			if (checkUnsavedEntries()) {
				Util.showMessageDialog("There appears to be unsaved information entered. Please save the proxy settings.");
			} else {
				resultPanel.setVisible(false);
				standAloneInstance.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				saTestBtn.setEnabled(false);
				standAloneInstance.pack();
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						results = JmarsHttpRequest.testNetworkAvailablility();
						
						boolean googleDNS = false;
						boolean appServerDNS = false;
						boolean googleNP = false;
						boolean appServerNP = false;
						boolean google = false;
						boolean appServer = false;
						
						if (results != null && results.size() > 0) {
							googleDNS = (results.containsKey(JmarsHttpRequest.GOOGLE_DIRECT) ? results.get(JmarsHttpRequest.GOOGLE_DIRECT) : false);
							appServerDNS = (results.containsKey(JmarsHttpRequest.APP_SERVER_DIRECT) ? results.get(JmarsHttpRequest.APP_SERVER_DIRECT) : false);
							googleNP = (results.containsKey(JmarsHttpRequest.GOOGLE_NO_PROXY) ? results.get(JmarsHttpRequest.GOOGLE_NO_PROXY) : false);
							appServerNP = (results.containsKey(JmarsHttpRequest.APP_SERVER_NO_PROXY) ? results.get(JmarsHttpRequest.APP_SERVER_NO_PROXY) : false);
							google = (results.containsKey(JmarsHttpRequest.GOOGLE_PROXY) ? results.get(JmarsHttpRequest.GOOGLE_PROXY): false);
							appServer = (results.containsKey(JmarsHttpRequest.APP_SERVER_PROXY) ? results.get(JmarsHttpRequest.APP_SERVER_PROXY) : false);
						}
						
						StringBuffer a = new StringBuffer();
						a.append("<html>");
						
						//testing
			//			googleDNS = true;
			//			appServerDNS = false;
						
						if (google && appServer) {
							a.append("The test was successful. Please continue to JMARS.");
						} else {//we had a failure somewhere
							if (!googleDNS && !appServerDNS) {//DNS checks
								a.append("Failed attempt to contact Google and JMARS servers.<br />");
								a.append("Please verify that you have an Internet connection.<br />");
							} else if (google && !appServer) {//Google was good, but JMARS was a failure
								a.append("There appears to be a problem with the JMARS servers. <br />");
								a.append("You may see limited functionality in JMARS until the JMARS servers are reachable.<br />");
								a.append("To alert the JMARS team of the issue, send an email to help@jmars.asu.edu");
							} else {
								//DNS check to Google and JMARS was good, but we could not get to appserver using .
								ProxyInformation proxy = ProxyInformation.getInstance();
								if (proxy.isProxyUsed() && proxy.isProxySet()) {//proxy information being used
									if (googleNP && appServerNP) {//good test ignoring proxy information
										a.append("Unable to connect using the proxy settings entered.<br />");
										a.append("Please remove or fix the proxy settings.<br />");
									} else {
										a.append("Unable to connect to JMARS servers.");
									}
								} else {
									a.append("Failed to connect to JMARS servers.");
								}
							}
						}

						saContinueBtn.setEnabled(true);
						analysis1.setText(a.toString());
						saTestBtn.setEnabled(true);
						resultPanel.setVisible(true);
						standAloneInstance.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						pack();
					}
				});
				
				
			}

		}
	};
	private ProxyDialog(JFrame owner) {
		super(owner);
	}
	
	public static ProxyDialog getStandAloneInstance() {
		if (standAloneInstance == null) {
			standAloneInstance = new ProxyDialog(Main.mainFrame);
			standAloneInstance.initStandaloneComponents();
			standAloneInstance.buildStandAloneDialog();
		}
		return standAloneInstance;
	}

	public void displayStandAloneDialog() {
		saTopOptionsLbl.setVisible(false);
		saTopLbl.setVisible(true);
		saExitBtn.setVisible(true);
		saMsg.setVisible(true);
		proxyPanel.setVisible(false);
		resultPanel.setVisible(false);
		standAloneInstance.pack();
		standAloneInstance.setVisible(true);
	}
	public void displayOptionsDialog() {
		saTopOptionsLbl.setVisible(true);
		saTopLbl.setVisible(false);
		saMsg.setVisible(false);
		saProxyBtn.setEnabled(false);
		saExitBtn.setEnabled(false);
		proxyPanel.setVisible(true);
		resultPanel.setVisible(false);
		standAloneInstance.pack();
		standAloneInstance.setVisible(true);
	}

	private boolean checkUnsavedEntries() {
		proxyInfo = ProxyInformation.getInstance();
		proxyInfo.readFile();
		String fileHost = proxyInfo.getHost();
		String port = String.valueOf(proxyInfo.getPort());
		if (port.equals("0")) {
			port = "";
		}
		String portInput = portTf.getText().trim();
		if (portInput.equals("0")) {
			portInput = "";
		}
		if (!compare(port, portInput) ||
			!compare(proxyInfo.getUsername(), userTf.getText()) ||
			!compare (proxyInfo.getPassword(), new String(passwdTf.getPassword())) ||
			!compare(fileHost, hostTf.getText())) {
			return true;
		}
		return false;
	}
	private boolean compare(String one, String two) {
		if (one == null) {
			one = "";
		}
		if (two == null) {
			two = "";
		}
		if (one.trim().equalsIgnoreCase(two)) {
			return true;
		}
		return false;
		
	}
	private AbstractAction exitAction = new AbstractAction("EXIT JMARS") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	};
	private AbstractAction clearAction = new AbstractAction("CLEAR") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			hostTf.setText("");
			portTf.setText("");
			userTf.setText("");
			passwdTf.setText("");
			saContinueBtn.setEnabled(false);
			
		}
	};
	private void saveProxyValues() {
		setProxyValues();
		proxyInfo.writeFile();
		Util.showMessageDialog("Proxy information saved. Please test connection before proceeding.");
		saContinueBtn.setEnabled(false);
	}
	private AbstractAction saveAction = new AbstractAction("SAVE") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (validateValues()) {
				saveProxyValues();
			}
			saTestBtn.setEnabled(true);
		}
	};
	private AbstractAction continueAction = new AbstractAction("PROCEED") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (checkUnsavedEntries()) {
				if (JOptionPane.YES_OPTION == Util.showConfirmDialog("You have unsaved Information. Would you like to save now?")) {
					saveProxyValues();
				}
			} else {
				standAloneInstance.setVisible(false);
			}
			
		}
	};
	private AbstractAction reloadAction = new AbstractAction("RELOAD SAVED SETTINGS") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			loadProxySettings();
			Util.showMessageDialog("Proxy information loaded from saved file.");
		}
	};
	private void loadProxySettings() {
		proxyInfo = ProxyInformation.getInstance();
		proxyInfo.readFile();
		hostTf.setText(proxyInfo.getHost());
		int port = proxyInfo.getPort();
		if (port == 0) {
			portTf.setText("");//using JSON, port is an int, it will never be stored as a blank. 0 is used by default.	
		} else {
			portTf.setText(String.valueOf(port));
		}
		
		userTf.setText(proxyInfo.getUsername());
		passwdTf.setText(proxyInfo.getPassword());
	}
	private void setProxyValues() {
		if (validateValues()) {
			proxyInfo.setHost(hostTf.getText());
			String portVal = portTf.getText();
			if (portVal.trim().length() > 0) {//allow them to save blanks
				proxyInfo.setPort(Integer.parseInt(portVal));
			} else {
				proxyInfo.setPort(0);
			}
			proxyInfo.setUsername(userTf.getText());
			proxyInfo.setPassword(new String(passwdTf.getPassword()));
		}
	}
	private boolean validateValues() {
		boolean returnValue = true;
		String host = hostTf.getText().trim();
		String port = portTf.getText().trim();
		String user = userTf.getText().trim();
		String pw = new String(passwdTf.getPassword()).trim();
		String msg = "";
		if (host.length() > 0) {
			if ((port.length() == 0)) {
				msg += "Port must be a numeric value.\n";
				returnValue = false;
			} else {
				try {
					Integer.parseInt(port);
				} catch (NumberFormatException nfe ) {
					msg += "Port must be a numeric value.\n";
					returnValue = false;
				}
			}
			if (user.length() > 0) {
				if (pw.length() == 0) {
					returnValue = false;
					msg += "Username entered, password is required.\n";
				}
			}
		} else {
			if ((port.length() > 0) || user.length() > 0 || pw.length() > 0) {
				msg = "Please enter a host. ";
				returnValue = false;
			}
		}
		if (!returnValue) {
			Util.showMessageDialog(msg);
		}
		return returnValue;
	}
}
