package edu.asu.jmars.layer.map2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.MouseInputAdapter;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Caret;

import edu.asu.jmars.swing.CustomTabOrder;
import edu.asu.jmars.swing.UrlLabel;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.HttpRequestType;

public class FileUploadDialog extends JDialog {
	private String defaultdescTxt = "This is an optional description that will be attached to this " +
	"custom map and displayed in the info panel (for anyone who has access to it, initial and shared users).";

	private static final long serialVersionUID = 2L;
	private static DebugLog log = DebugLog.instance();
	
	private final CustomMapServer customMapServer;
	private final JFileChooser fileChooser;
	private JTextField nameField = new JTextField(20);
	private JTextField remoteName = new JTextField(20);
	private JTextField ignoreField = new JTextField(5);
	private JTextField north = new JTextField(2);
	private JTextField south = new JTextField(2);
	private JTextField west = new JTextField(2);
	private JTextField east = new JTextField(2);
	private JTextArea description = new JTextArea(defaultdescTxt);
	private JCheckBox email = new JCheckBox("Email when ready");
	private JCheckBox geoRef = new JCheckBox("Contained in image");
	private JButton uploadPB = new JButton("Upload".toUpperCase());
	private JButton cancelPB = new JButton("Cancel".toUpperCase());
	private JLabel supportedPB = new UrlLabel("List of supported file formats"); 
			
	
	JRadioButton global = new JRadioButton("Global Map");
	JRadioButton regional = new JRadioButton("Regional Image");
	
	JLabel eastLonLbl = new JLabel("Easternmost Longitude (\u00b0E)  ", JLabel.TRAILING);
	JLabel westLonLbl = new JLabel("Westernmost Longitude (\u00b0E)  ", JLabel.TRAILING);
	JRadioButton degreeE = new JRadioButton("\u00b0E");
	JRadioButton degreeW = new JRadioButton("\u00b0W");
	
	// hidden radio button to deselect other options
	JRadioButton unselected = new JRadioButton("Unselected"); 

	private boolean regionSet = false;
	
	/** Creates a simple file chooser for the custom maps */
	public static JFileChooser createDefaultChooser() {
		JFileChooser chooser = new JFileChooser(Util.getDefaultFCLocation());
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		return chooser;
	}

	/**
	 * The task currently executing within this dialog; the task can compare
	 * itself to this field on the AWT event thread to see if it 'owns' the
	 * dialog.
	 */
	private UploadTask currentTask;
	
	/**
	 * Creates a a modal upload dialog.
	 * @param parent Frame to hang this dialog from
	 * @param chooser File chooser, or null to internally create a default chooser with {@link #createDefaultChooser()}.
	 * @param customMapServer The map server to furnish the map to.
	 */
	public FileUploadDialog(Frame parent, JFileChooser chooser, CustomMapServer customMapServer){
		super(parent, "Upload File", true);
		this.fileChooser = chooser == null ? createDefaultChooser() : chooser;
		this.customMapServer = customMapServer;
		setResizable(false);
		setContentPane(createMain());
		pack();
		setLocationRelativeTo(parent);
		Util.addEscapeAction(this);
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
	}
	
	private static boolean isURL(String path) {
		path = path.trim().toLowerCase();
		return path.startsWith("ftp://") || path.startsWith("http://");
	}
	
	private JComponent createMain() {
		final JButton browsePB = new JButton("Browse".toUpperCase());
		
		browsePB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				String file = browse();
				if (file != null) {
					nameField.setText(file);
				}
			}
		});
		
		ignoreField.setToolTipText("Specify the pixel value which is transparent in all bands, or leave blank if there isn't one");
		
		uploadPB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				String path = nameField.getText();
				if (!isURL(path) && !new File(path).exists()) {
					error(path, "File does not exist!");
					return;
				}
				String name = remoteName.getText();
				if (name.length() == 0) {
					error(name, "Must provide a remote name to identify this map");
					return;
				}
				
				// check for invalid file name chars
				String rawInput = name.trim();
				if (!rawInput.matches("^[\\w\\s-_.@]*$")) {
					Util.showMessageDialog("You have entered illegal map name characters.\n " +
							"Only the following characters are allowed:\n " +
							"A-Z a-z 0-9 - _ . @ <space>", 
							"File Name Input Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				
				
				boolean titleUsed = false;
				for (MapSource source: customMapServer.getMapSources()) {
					if (source.getTitle().equals(name)) {
						titleUsed = true;
						break;
					}
				}
				if (titleUsed && JOptionPane.YES_OPTION != Util.showConfirmDialog(
						"A custom map with that name already exists, overwrite?",
						"Map name exists",
						JOptionPane.YES_NO_OPTION)) {
					return;
				}
				try {
					Rectangle2D bounds = null;
					if (west.getText().trim().length() > 0 ||
							east.getText().trim().length() > 0 ||
							north.getText().trim().length() > 0 ||
							south.getText().trim().length() > 0) {
						double minx = Double.parseDouble(west.getText().trim());
						double maxx = Double.parseDouble(east.getText().trim());
						double miny = Double.parseDouble(south.getText().trim());
						double maxy = Double.parseDouble(north.getText().trim());
						// If degrees west is selected, convert to East first...
						if(degreeW.isSelected()){
							if(minx < 0) minx = minx+360;
							else minx = 360-minx;
							
							if(maxx<0) maxx=maxx+360;
							else maxx = 360-maxx;
						}
						
						if (minx < 0) {
							minx += 360;
							maxx += 360;
						}
						if (minx > maxx) {
							maxx += 360;
						}
						bounds = new Rectangle2D.Double(minx,miny,maxx-minx,maxy-miny);
					}
					
					Double ignore = null;
					if (ignoreField.getText().trim().length() > 0) {
						ignore = Double.parseDouble(ignoreField.getText());
					}
					
					boolean mail = email.isSelected();
					
					String desc = description.isEditable() ? description.getText() : "";
					
					log.println(MessageFormat.format(
						"Uploading {0} to {1}, ignore {2}, bounds {3}, email {4}, description {5}",
						path,
						customMapServer.getURI(),
						ignore==null?"unspecified":ignore.toString(),
						bounds==null?"unspecified":bounds.toString(),
						mail ? "yes" : "no",
						desc));
					
					// creates a new task for this upload off the AWT thread so
					// the user can potentially do other things while waiting, and moves
					// the onUpload callback into the task so the dialog doesn't have
					// to hold it
					currentTask.setArgs(name, path, bounds, ignore, mail, desc, geoRef.isSelected());
					Thread uploadThread = new Thread(currentTask);
					uploadThread.setName("Custom upload thread");
					uploadThread.setPriority(Thread.MIN_PRIORITY);
					uploadThread.start();
				} catch(Exception ex) {
					error(path, ex.toString());
				}
			}
		});
		
		cancelPB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				description.setText(defaultdescTxt);
				description.setEditable(false);
				description.setForeground(Color.GRAY);
				stopAndClose();
			}
		});
		
		supportedPB.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
					String str = null;
					String supported_url = Config.get("supportedfileformatspage");
					try {
/*						URLConnection connection = null;
						connection =  new URL(supported_url).openConnection();
						
						// This code reads the entire web page at once and stores it in str
							
						Scanner scanner = new Scanner(connection.getInputStream());
						scanner.useDelimiter("\\Z");
						str = scanner.next();
						scanner.close();
*/						
						JmarsHttpRequest request = new JmarsHttpRequest(supported_url, HttpRequestType.GET);
						request.send();
						
						Scanner scanner = new Scanner(request.getResponseAsStream());
						scanner.useDelimiter("\\Z");
						str = scanner.next();
						scanner.close();
						request.close();

						JOptionPane optionPane = new JOptionPane();
						optionPane.setMessage(str);
						optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
						JDialog dialog = optionPane.createDialog(FileUploadDialog.this, "List of supported file formats");
						dialog.setVisible(true);
					}
					catch (Exception e) {
						log.aprintln("Exception accessing Supported Images URL (in FileUploadDialog)=" + supported_url);
						log.aprintln(e);
						Util.showMessageDialog("General exception occurred while trying to access (" + supported_url + ")", "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			});
	// Fix for the regional erase bug (enter text in these fields before the regional dial
	//	button is selected and it erases entries when you select the dial)
		DocumentListener regionalTextFields = new DocumentListener() {
			public void removeUpdate(DocumentEvent e) {
			}
			public void insertUpdate(DocumentEvent e) {
				if(global.isSelected()==false)
					regional.setSelected(true);
					uploadPB.setEnabled(true);
			}
			public void changedUpdate(DocumentEvent e) {
			}
		};
		north.getDocument().addDocumentListener(regionalTextFields);
		south.getDocument().addDocumentListener(regionalTextFields);
		east.getDocument().addDocumentListener(regionalTextFields);
		west.getDocument().addDocumentListener(regionalTextFields);
		
		
		nameField.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
				update();
			}
			public void insertUpdate(DocumentEvent e) {
				update();
			}
			public void removeUpdate(DocumentEvent e) {
				update();
			}
			private void update() {
				String name = nameField.getText();
				if (isURL(name)) {
					try {
						name = new URL(name).getPath().replaceAll("^/", "");
					} catch (Exception e) {
						// silently ignore, we just won't auto-populate the name
						// from a URL we don't understand
						return;
					}
				} else {
					name = new File(name).getName();
				}
				remoteName.setText(name);

				if (name.length()>3 && name.substring(name.length()-4).toLowerCase().endsWith(".cub")) {
					if (!regionSet) {
						regionSet=true;
						north.setText("");
						south.setText("");
						west.setText("");
						east.setText("");
						north.setEnabled(false);
						south.setEnabled(false);
						west.setEnabled(false);
						east.setEnabled(false);
						global.setEnabled(false);
						regional.setEnabled(false);
						unselected.setSelected(true);
					}
				} else if (!geoRef.isSelected()) {				
						north.setEnabled(true);
						south.setEnabled(true);
						west.setEnabled(true);
						east.setEnabled(true);			
						global.setEnabled(true);
						regional.setEnabled(true);
						regionSet=false;
					
				}
				if (regionSet||global.isSelected()||regional.isSelected()||geoRef.isSelected()) {
					uploadPB.setEnabled(true);
				} else {
					uploadPB.setEnabled(false);
				}
			}
		});
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		
		
		Box main = new Box(BoxLayout.Y_AXIS);
		main.setBorder(new EmptyBorder(4,4,4,4));
		
		Insets insets = new Insets(4,4,4,4);
		int padx = 0;
		int pady=  0;
		
		JPanel step1 = new JPanel(new GridBagLayout());
		step1.setBorder(new TitledBorder("Step 1: Pick Map to Upload"));
		
		step1.add(new JLabel("File or URL"), new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,insets,padx,pady));

		GridBagConstraints constraints = new GridBagConstraints ();
		constraints.gridy = 0;
		step1.add(nameField, constraints);
		step1.add(browsePB, new GridBagConstraints(4,0,1,1,0,0,GridBagConstraints.EAST,GridBagConstraints.NONE,insets,padx,pady));
		constraints.gridy = 1;
		constraints.gridwidth = 2;
		constraints.anchor = GridBagConstraints.WEST;
		step1.add(supportedPB, constraints);

		JPanel step2 = new JPanel();
		step2.setBorder(new TitledBorder("Step 2: Choose a name for this map in JMARS"));

		step2.add(new JLabel("Map Name"), new GridBagConstraints(0,1,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,insets,padx,pady));
		step2.add(remoteName, new GridBagConstraints(2,1,2,1,1,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,insets,padx,pady));
		
		JPanel step3 = new JPanel(new GridLayout(0,2));
		step3.setBorder(new TitledBorder("Step 3: Enter spatial information:"));
		
		enableSpatialInputs(true);
		JLabel geoRefLabel = new JLabel("Geospatial data:  ",JLabel.TRAILING);
		//toolTip for geoRef button
		geoRefLabel.setToolTipText("Select this box to have JMARS read the Geospatial information from the image. If this image does not have Geospatial information, use one of the other options below.");
		step3.add(geoRefLabel);
		geoRef.setSelected(false);
		geoRef.setToolTipText("Select this box to have JMARS read the Geospatial information from the image. If this image does not have Geospatial information, use one of the other options below.");
		geoRef.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				if (geoRef.isSelected()) {
					clearSpatialInputs();
					enableSpatialInputs(false);
					uploadPB.setEnabled(true);
					setGeoMode();
				} else {
					enableSpatialInputs(true);
					if (regionSet||global.isSelected()||regional.isSelected()) {
						uploadPB.setEnabled(true);
					} else {
						uploadPB.setEnabled(false);
					}
				}
			}
		});
		step3.add(geoRef);
		
		ActionListener selectionChange = new ActionListener(){
			boolean globalWasSelected=false;
			public void actionPerformed(ActionEvent e) {
				if (global.isSelected()) {
					north.setText("90");
					south.setText("-90");
					west.setText("-180");
					east.setText("180");
					globalWasSelected=true;
				} else if (regional.isSelected() && globalWasSelected) {
					north.setText("");
					south.setText("");
					west.setText("");
					east.setText("");					
				} else {
					// Shouldn't be any other cases, do nothing if there are
				}
				
				if (regionSet||global.isSelected()||regional.isSelected()||geoRef.isSelected()) {
					uploadPB.setEnabled(true);
				} else {
					uploadPB.setEnabled(false);
				}

			}
		};
		
		global.addActionListener(selectionChange);
		regional.addActionListener(selectionChange);
		
		ButtonGroup group = new ButtonGroup();
		group.add(global);
		group.add(regional);
		group.add(unselected);
		
		
		ActionListener degreeChange = new ActionListener(){
			public void actionPerformed(ActionEvent e){
				JRadioButton deg = (JRadioButton) e.getSource();
				westLonLbl.setText("Westernmost Longitude ("+deg.getText()+")  ");
				eastLonLbl.setText("Easternmost Longitude ("+deg.getText()+")  ");
			}
		};
		
		degreeE.addActionListener(degreeChange);
		degreeW.addActionListener(degreeChange);
		
		ButtonGroup degreeGroup = new ButtonGroup();
		degreeGroup.add(degreeE);
		degreeGroup.add(degreeW);
		JPanel degreePanel = new JPanel();
		degreePanel.setLayout(new BoxLayout(degreePanel, BoxLayout.LINE_AXIS));
		degreePanel.add(Box.createHorizontalStrut(45));
		degreePanel.add(degreeE);
		degreePanel.add(Box.createHorizontalStrut(15));
		degreePanel.add(degreeW);
		degreeE.setSelected(true);
		
		step3.add(global);
		step3.add(regional);
		step3.add(new JLabel("Northernmost Latitude (\u00b0N)  ", JLabel.TRAILING));
		step3.add(north);
		step3.add(new JLabel("Southernmost Latitude (\u00b0N)  ", JLabel.TRAILING));
		step3.add(south);
		step3.add(new JLabel("Longitude Degrees: ", JLabel.TRAILING));
		step3.add(degreePanel);
		step3.add(westLonLbl);
		step3.add(west);
		step3.add(eastLonLbl);
		step3.add(east);

		JPanel step4 = new JPanel(new GridBagLayout());
		step4.setBorder(new TitledBorder("Step 4: Additional options"));
		step4.add(new JLabel("Ignore Value"), new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,insets,padx,pady));
		ignoreField.setMaximumSize(ignoreField.getPreferredSize());
		step4.add(ignoreField, new GridBagConstraints(1,0,1,1,0.5,0,GridBagConstraints.WEST,GridBagConstraints.NONE,insets,padx,pady));

		step4.add(email, new GridBagConstraints(2,0,1,1,1,0,GridBagConstraints.WEST,GridBagConstraints.NONE,insets,padx,pady));
		step4.add(new JLabel("Description"), new GridBagConstraints(0,1,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,insets,padx,pady));
		description.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		description.setColumns(34);
		description.setRows(3);
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		description.setEditable(false);
		description.setEnabled(true);
		description.setForeground(Color.GRAY);
		final Caret caret = description.getCaret();
		caret.setSelectionVisible(true);
		description.addMouseListener(new MouseInputAdapter () {
			public void mouseClicked(MouseEvent e) {
				if (!description.isEditable()) {
					description.setText("");
					description.setForeground(Color.BLACK);
					description.setEditable(true);
					description.setCaret(caret);
					description.setCaretPosition(0);
					
				}
			}
		});
		JScrollPane descScrollPane = new JScrollPane(description);
		descScrollPane.setVerticalScrollBarPolicy(
		                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		descScrollPane.setPreferredSize(description.getPreferredScrollableViewportSize());
		
		step4.add(descScrollPane, new GridBagConstraints(0,2,3,1,0,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,insets,padx,pady));
		
		main.add(step1);
		main.add(step2);
		main.add(step3);
		main.add(step4);
				
		// set up hot keys
		browsePB.setMnemonic('B');
		uploadPB.setMnemonic('U');
		cancelPB.setMnemonic('C');
		
		// customize the focus traversal
		main.setFocusTraversalPolicy(new CustomTabOrder(new Component[] {
			nameField,
			browsePB,
			remoteName,
			email,
			ignoreField,
			north,
			south,
			west,
			east,
			uploadPB,
			cancelPB
		}));
		main.setFocusTraversalPolicyProvider(true);
		revertValues();

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(uploadPB);
		buttonPanel.add(cancelPB);
		
		uploadPB.setEnabled(false);
		
		mainPanel.add(main, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);
		return mainPanel;
	}
	
    public void setGeoMode() {
    	
    	if(geoRef.isSelected()){
    		//Display instruction dialog unless indicated in config file
    		if(Config.get("showGeoRefDialog").equalsIgnoreCase("true")){
    			JCheckBox chkBox = new JCheckBox("Do not show this message again.");
    			chkBox.setFont(new Font("Dialog", Font.PLAIN, 12));
    			chkBox.setAlignmentY(JCheckBox.RIGHT_ALIGNMENT);
    			Color black = new Color(0,0,0);
    			JLabel welcomeMessage = new JLabel("Reading Your Geospatial Information");
    			welcomeMessage.setFont(new Font("Dialog",Font.ITALIC+Font.BOLD,14));
    			welcomeMessage.setForeground(black);
    			JTextArea message = new JTextArea("\n" +
    					"• The Geospatial reader is a new feature to JMARS. It uses GDAL to read\n" +
    					"the information in your file. If your image is not correctly projected\n" +
    					"then you may experience inaccuracy. You can upload images with any of the\n" +
    					"extensions that GDAL uses.\n\n"+
    					"• If you are experiencing difficulty or inaccuracy when you believe your image\n" +
    					"should have geospatial information, feel free to contact the JMARS team. We\n" +
    					"appreciate your feedback while this feature is in Beta mode.\n"); 
    			message.setEditable(false);   			
    			message.setForeground(black);
    			message.setFont(new Font("Dialog",Font.BOLD,12));
    			
    			Object[] params = {welcomeMessage, message, chkBox};
    			int n =Util.showConfirmDialog(params, "Geospatial Information", JOptionPane.OK_CANCEL_OPTION);

    			if(n == JOptionPane.OK_OPTION){	//if Okay, check the checkbox to set config variable
    				boolean dontShow = chkBox.isSelected();
    				if(dontShow){
    					Config.set("showGeoRefDialog", "false");
    				}
    			}else{    //if cancel, return 
    				return;
    			}
    		}
    	}
    }
	
	private void clearSpatialInputs() {
		north.setText("");
		south.setText("");
		west.setText("");
		east.setText("");
	}
	private void enableSpatialInputs(boolean enabled) {
		north.setEnabled(enabled);
		south.setEnabled(enabled);
		west.setEnabled(enabled);
		east.setEnabled(enabled);
		global.setEnabled(enabled);
		regional.setEnabled(enabled);
		degreeE.setEnabled(enabled);
		degreeW.setEnabled(enabled);
	}
	private void setDialogWaiting(boolean waiting) {
		if (waiting) {
			uploadPB.setEnabled(false);
			cancelPB.setText("Hide");
			cancelPB.setToolTipText("Runs in the background, and pops up a dialog when map is ready");
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		} else {
			uploadPB.setEnabled(true);
			cancelPB.setText("Cancel");
			cancelPB.setToolTipText("");
			setCursor(Cursor.getDefaultCursor());
		}
	}
	
	private void revertValues() {
		north.setText("");
		south.setText("");
		west.setText("");
		east.setText("");
		nameField.setText("");
		ignoreField.setText("");
		uploadPB.setEnabled(false);
		regionSet=false;
		unselected.setSelected(false);
		description.setText(defaultdescTxt);
		description.setEditable(false);
		description.setForeground(Color.GRAY);
		unselected.setSelected(true);
	}
	
	/**
	 * Resets and closes the dialog, should be called on the AWT event thread
	 * when Cancel/Hide is pressed, or an upload finishes successfully.
	 */
	private void stopAndClose() {
		// done with this action so let the reaper do its thing
		currentTask = null;
		setDialogWaiting(false);
		setVisible(false);
	}
	
	/** Shows an error dialog with the given message, only call on the AWT event thread */
	private void error(String path, String msg) {
		Util.showMessageDialog(Util.foldText(MessageFormat.format(
					"Upload of ''{0}'' failed with message ''{1}''", path, msg),
				60, "\n"),
			"The upload failed", JOptionPane.ERROR_MESSAGE);
	}
	
	private String browse() {
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			return fileChooser.getSelectedFile().getPath();
		} else {
			return null;
		}
	}
	
	/**
	 * Show the dialog for a new upload; note that calling {@link #setVisible(boolean)}
	 * merely shows the dialog from any prior upload, but won't set it up properly for
	 * a new one.
	 * 
	 * Should be called on the AWT event thread.
	 * 
	 * @param onUpload The action to call on the AWT event thread when this
	 * dialog has uploaded a map.
	 */
	public void uploadFile(final Runnable onUpload) {
		currentTask = new UploadTask(onUpload);
		revertValues();
		setVisible(true);
	}
	
	/** Tracks an upload so multiple uploads can run simultaneously */
	private class UploadTask implements Runnable {
		private String name;
		private String path;
		private Rectangle2D bounds;
		private Double ignore;
		private boolean wantsMail;
		private String desc;
		private boolean readGeoRef = false;
		private final Runnable onUpload;
		public UploadTask(Runnable onUpload) {
			this.onUpload = onUpload;
		}
		public void setArgs(String name, String path, Rectangle2D bounds, Double ignore, boolean mail, String desc, boolean readGeoRefData) {
			this.name = name;
			this.path = path;
			this.bounds = bounds;
			this.ignore = ignore;
			this.wantsMail = mail;
			this.desc = desc;
			this.readGeoRef = readGeoRefData;
		}
		public void run() {
			try {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (currentTask == UploadTask.this) {
							setDialogWaiting(true);
						}
					}
				});
				if (isURL(path)) {
					// A URL, ask the server to download the file itself
					URL url = new URL(path);
					customMapServer.uploadCustomMap(name, url, bounds, ignore, wantsMail, desc, readGeoRef);
				} else {
					// A file, upload the file to the server
					File file = new File(path);
					customMapServer.uploadCustomMap(name, file, bounds, ignore, wantsMail, desc, readGeoRef);
				}
				
				//we need to sleep for a few seconds off the EventDispatchThread to let caps finish loading
				//This is a short-term fix. We will find a better way to alert the create layer process that all of the 
				//listeners  have been notified and that they have finished running. For now, they should finish quickly
				//and we just need to make sure that they are done before we try to create the layer.
				if (SwingUtilities.isEventDispatchThread()) {
					new Thread(new Runnable() {
						
						public void run() {
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}).start();
				} else {
					Thread.sleep(5000);
				}
				
				// on success, either close the dialog, or popup a notice of success
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (currentTask == UploadTask.this) {
							stopAndClose();
						} else {
							Util.showMessageDialog(Util.foldText("Custom map '" + path + "'' has been uploaded successfully", 60, "\n"),
								"Custom map uploaded",
								JOptionPane.INFORMATION_MESSAGE);
						}
						
						// finally, hit the callback
						onUpload.run();
					}
				});
			} catch (final Exception e) {
				e.printStackTrace();
				// on error, get out of waiting mode, and use a popup to notify of failure
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (currentTask == UploadTask.this) {
							setDialogWaiting(false);
						}
						
						error(path, e.getMessage());
					}
				});
			}
		}
	}
	
	public static void main(String[] args) {
		new FileUploadDialog(null, null, null).setVisible(true);
	}
}
