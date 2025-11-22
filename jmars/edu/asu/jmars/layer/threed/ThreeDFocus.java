package edu.asu.jmars.layer.threed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.AbstractDocument;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3f;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ZoomManager;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.layer.map2.MapChannel;
import edu.asu.jmars.layer.map2.MapChannelReceiver;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.MapLView;
import edu.asu.jmars.layer.map2.MapRequest;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView;
import edu.asu.jmars.layer.util.NumericMapSourceDialog;
import edu.asu.jmars.lmanager.SearchProvider;
import edu.asu.jmars.swing.ColorMapOp;
import edu.asu.jmars.swing.DocumentNumberOfCharsFilter;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

@SuppressWarnings("serial")
public class ThreeDFocus extends FocusPanel {
	private ThreeDLayer myLayer;
	private ThreeDCanvas canvasPanel;

	private JButton updateBtn;
	private JCheckBox lightChk;
	private ColorButton backgroundCBtn;
	private JCheckBox backplaneChk;
	private ColorButton lightCBtn;
	private DirectionalLightWidget lightWidget;
	private JTextField sourceTF;
	private JTextArea mapDescTA;
	private JLabel ppdLbl;
	private JLabel unitLbl;
	private JLabel ignoreLbl;
	private JLabel minLbl;
	private JLabel meanLbl;
	private JLabel maxLbl;
	private JLabel stdLbl;
	private JComboBox<String> scaleBx;
	private JTextField exagTF;
	private JTextField totalExagTF;
	private JTextField generateQRResultMessage;
	private JLabel minValueLbl;
	private JLabel meanValueLbl;
	private JLabel maxValueLbl;
	private JLabel stdValueLbl;

	private JDialog controlDialog;
	private JFileChooser pngChooser;
	
	private JLabel pngResults = null;
	private JLabel save3DResultsLbl = null;
	private JDialog saveAsDlg;

	private final String ppdPrompt = "PPD: ";
	private final String unitPrompt = "Units: ";
	private final String ignorePrompt = "Ignore Value: ";
	private final String minPrompt = "Min: ";
	private final String meanPrompt = "Mean: ";
	private final String maxPrompt = "Max: ";
	private final String stdPrompt = "St Dev (" + Character.toString('\u03C3') + "): ";
	

	private StartupParameters settings = null;

	private int pad = 3;
	private Insets in = new Insets(pad, pad, pad, pad);
	private int row;
	private Font descripFont = new Font("Dialog", Font.PLAIN, 12);

	private static DebugLog log = DebugLog.instance();
	private static int pf = GroupLayout.PREFERRED_SIZE;

	public ThreeDFocus(ThreeDLView parent, StartupParameters settings) {
		super(parent, false);

		this.myLayer = (ThreeDLayer) parent.getLayer();

		if (settings != null) {
			this.settings = settings;
		} else {
			this.settings = new StartupParameters();
		}

		// set up and display the 3D view window
		setup3D();
		
		// add the main tab
		add(createControlPanel2(), "Controls");
		add(createXRPanel(), "VR/XR");
		add(createHelpPanel(), "Help");
		if (!Main.isUserLoggedIn()) {
			setEnabledAt(1, false);
		}
		
		// populate map and scale panels with info from intial source
		updateMapAndScaleInfo(myLayer.getElevationSource());
	
	}

	private void setup3D() {
		
		// create the canvas and set initial settings
		canvasPanel = new ThreeDCanvas(parent, settings);
		canvasPanel.setBackgroundColor(settings.backgroundColor);
		canvasPanel.enableBackplane(settings.backplaneBoolean);
		canvasPanel.setAltitudeSource(myLayer.getElevationSource());
		canvasPanel.enableDirectionalLight(settings.directionalLightBoolean);
		canvasPanel.setDirectionalLightColor(settings.directionalLightColor);
		canvasPanel.setDirectionalLightDirection(settings.directionalLightDirection.x,
		settings.directionalLightDirection.y, settings.directionalLightDirection.z);

	}

	private JPanel createXRPanel() {
		VR_Manager vrManager = VR_Manager.getInstance();
		return vrManager.getDisplay(this, myLayer, canvasPanel);
	}
	
	private JPanel createHelpPanel() {
		JPanel helpPnl = new JPanel();
		JButton controlsBtn = new JButton(viewControlAct);
		JLabel writtenTutorialsLbl = new JLabel("View Written Tutorial...");
		JLabel videoTutorialsLbl = new JLabel("View Video Tutorial...");
		writtenTutorialsLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		writtenTutorialsLbl.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent e) {
				writtenTutorialsLbl.setText("<html><a href=''>View Written Tutorial...</a></html>");
			}

			@Override
			public void mouseExited(MouseEvent e) {
				writtenTutorialsLbl.setText("View Written Tutorial...");
			}

			@Override
			public void mouseClicked(MouseEvent arg0) {
				try {
					Util.launchBrowser("https://jmars.mars.asu.edu/j5_3dLayerTutorial");
				} catch (Exception e1) {
					log.aprintln(e1);
				}
			}
			
		});
		videoTutorialsLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		videoTutorialsLbl.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseEntered(MouseEvent e) {
				videoTutorialsLbl.setText("<html><a href=''>View Video Tutorial...</a></html>");
			}

			@Override
			public void mouseExited(MouseEvent e) {
				videoTutorialsLbl.setText("View Video Tutorial...");
			}

			@Override
			public void mouseClicked(MouseEvent arg0) {
				try {
					Util.launchBrowser("https://jmars.mars.asu.edu/j5_3dLayer");
				} catch (Exception e1) {
					log.aprintln(e1);
				}
			}
			
		});
		JPanel tutorialsPnl = new JPanel();
		GroupLayout tutorialsLayout = new GroupLayout(tutorialsPnl);
		tutorialsPnl.setLayout(tutorialsLayout);
		tutorialsPnl.setBorder(BorderFactory.createTitledBorder("Tutorials"));
		
		tutorialsLayout.setHorizontalGroup(tutorialsLayout.createParallelGroup(Alignment.LEADING)
			.addComponent(writtenTutorialsLbl, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addComponent(videoTutorialsLbl, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE));
		tutorialsLayout.setVerticalGroup(tutorialsLayout.createSequentialGroup()
			.addGap(5)
			.addComponent(writtenTutorialsLbl)
			.addGap(5)
			.addComponent(videoTutorialsLbl)
			.addGap(5));
		
		GroupLayout layout = new GroupLayout(helpPnl);
		helpPnl.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
			.addComponent(controlsBtn)
			.addComponent(tutorialsPnl));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(controlsBtn, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addComponent(tutorialsPnl, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE));
		return helpPnl;
	}
	private JPanel createControlPanel2() {
		JLabel configureLbl = new JLabel("Configure");
		JLabel displayLbl = new JLabel("DISPLAY");
		JLabel lightingLbl = new JLabel("LIGHTING");
		JLabel mapSourceLbl = new JLabel("MAP SOURCE");
		JLabel sourceValuesLbl = new JLabel("SOURCE VALUES");
		JLabel scalingLbl = new JLabel("SCALING");
		JLabel scaleLbl = new JLabel("MODE");
		JLabel exagLbl = new JLabel("VERTICAL EXAGGERATION");
		JLabel totalExagLbl = new JLabel("TOTAL EXAGGERATION");
		minLbl = new JLabel(minPrompt);
		meanLbl = new JLabel(meanPrompt);
		maxLbl = new JLabel(maxPrompt);
		stdLbl = new JLabel(stdPrompt);
		minValueLbl = new JLabel("");
		meanValueLbl = new JLabel("");
		maxValueLbl = new JLabel("");
		stdValueLbl = new JLabel("");
		
		unitLbl = new JLabel(unitPrompt);
		ppdLbl = new JLabel(ppdPrompt);
		ignoreLbl = new JLabel(ignorePrompt);
		

		JButton resetBtn = new JButton(resetCameraAct);
		JButton helpBtn = new JButton(viewControlAct);
		JButton saveBtn = new JButton(saveAsAct);
		updateBtn = new JButton(updateAct);
		
		backplaneChk = new JCheckBox("Opaque Bottom", settings.backplaneBoolean);
		backplaneChk.addActionListener(backplaneListener);
		backgroundCBtn = new ColorButton("BACKGROUND COLOR", settings.backgroundColor);
		backgroundCBtn.addActionListener(backgroundColorListener);
		lightChk = new JCheckBox("Light On", settings.directionalLightBoolean);
		lightChk.addActionListener(lightListener);
		lightCBtn = new ColorButton("LIGHT COLOR", settings.directionalLightColor);
		lightCBtn.addActionListener(lightColorListener);
		lightWidget = new DirectionalLightWidget(this);
		lightWidget.setEnabled(settings.directionalLightBoolean);
		lightWidget.setColor(new Color3f(settings.directionalLightColor));
		JButton sourceBtn = new JButton(sourceAct);
		sourceTF = new JTextField(20);
		sourceTF.setEditable(false);
		
		exagTF = new JTextField(5);
		exagTF.setMinimumSize(new Dimension(60, 19));
		exagTF.setText(settings.zScaleString);
		exagTF.addFocusListener(exagFocusListener);
		exagTF.addActionListener(exagListener);
		totalExagTF = new JTextField(5);
		totalExagTF.setEditable(false);
		totalExagTF.setMinimumSize(new Dimension(70, 19));
		float displayScaleFactor = (settings.scaleUnitsInKm) ? 1000 : 1; // If we are representing scale on body in km,
																			// then need to covert to meters for display
																			// (otherwise already in meters)
		totalExagTF.setText(String.format("%7.3f",
				Math.abs(Float.parseFloat(settings.zScaleString) * settings.scaleOffset * displayScaleFactor)));
		
		Vector<String> scaleVec = new Vector<String>();
		scaleVec.add(ThreeDCanvas.SCALE_MODE_AUTO_SCALE);
		scaleVec.add(ThreeDCanvas.SCALE_MODE_RANGE);
		scaleVec.add(ThreeDCanvas.SCALE_MODE_ST_DEV);
		scaleVec.add(ThreeDCanvas.SCALE_MODE_ABSOLUTE);
		scaleBx = new JComboBox<String>(scaleVec);
		scaleBx.setSelectedIndex(0);
		scaleBx.addActionListener(scaleListener);
		
		JPanel controlPanel = new JPanel();
		GroupLayout layout = new GroupLayout(controlPanel);
		controlPanel.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.LEADING)
			.addComponent(configureLbl)
			.addGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(displayLbl)
					.addComponent(backplaneChk)
					.addComponent(backgroundCBtn))
				.addGap(30)
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(lightingLbl)
					.addComponent(lightChk)
					.addComponent(lightCBtn))
				.addGap(30)
				.addComponent(lightWidget,65,65,65))
			.addComponent(mapSourceLbl)
			.addComponent(sourceBtn)
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(sourceTF)
				.addGroup(layout.createSequentialGroup()
					.addComponent(unitLbl)
					.addComponent(ppdLbl)
					.addComponent(ignoreLbl))
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addComponent(minLbl)
						.addComponent(minValueLbl))
					.addGap(30)
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addComponent(meanLbl)
						.addComponent(meanValueLbl))
					.addGap(30)
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addComponent(maxLbl)
						.addComponent(maxValueLbl))
					.addGap(30)
					.addGroup(layout.createParallelGroup(Alignment.LEADING)
						.addComponent(stdLbl)
						.addComponent(stdValueLbl))))
			.addComponent(sourceValuesLbl)
			.addComponent(scalingLbl)
			.addGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(scaleLbl)
					.addComponent(scaleBx))
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(exagLbl)
					.addComponent(exagTF))
				.addGroup(layout.createParallelGroup(Alignment.LEADING)
					.addComponent(totalExagLbl)
					.addComponent(totalExagTF)))
			.addGroup(layout.createSequentialGroup()
				.addComponent(resetBtn)
				.addComponent(updateBtn)
				.addComponent(helpBtn)
				.addComponent(saveBtn)));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(configureLbl)
			.addGap(15)
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(displayLbl)	
				.addComponent(lightingLbl))
			.addGap(15)
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addGroup(layout.createSequentialGroup()
					.addComponent(backplaneChk)
					.addGap(15)
					.addComponent(backgroundCBtn))
				.addGroup(layout.createSequentialGroup()
					.addComponent(lightChk)
					.addGap(15)
					.addComponent(lightCBtn))
				.addComponent(lightWidget, 65,65,65))
			.addGap(30)	
			.addComponent(mapSourceLbl)
			.addGap(15)
			.addComponent(sourceBtn)
			.addComponent(sourceTF, pf,pf,pf)
			.addGap(10)
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(unitLbl)
				.addComponent(ppdLbl)
				.addComponent(ignoreLbl))
			.addGap(10)
			.addComponent(sourceValuesLbl)
			.addGap(15)
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addGroup(layout.createSequentialGroup()
					.addComponent(minLbl)
					.addComponent(minValueLbl))
				.addGroup(layout.createSequentialGroup()
					.addComponent(meanLbl)
					.addComponent(meanValueLbl))
				.addGroup(layout.createSequentialGroup()
					.addComponent(maxLbl)
					.addComponent(maxValueLbl))
				.addGroup(layout.createSequentialGroup()
					.addComponent(stdLbl)
					.addComponent(stdValueLbl)))
			.addGap(30)
			.addComponent(scalingLbl)
			.addGap(15)
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(scaleLbl)
				.addComponent(exagLbl)
				.addComponent(totalExagLbl))
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(scaleBx, pf,pf,pf)
				.addComponent(exagTF, pf,pf,pf)
				.addComponent(totalExagTF, pf,pf,pf))
			.addGap(30)
			.addGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(resetBtn, pf,pf,pf)
				.addComponent(updateBtn, pf,pf,pf)
				.addComponent(helpBtn, pf,pf,pf)
				.addComponent(saveBtn, pf,pf,pf)));
		
		layout.linkSize(SwingConstants.VERTICAL, scaleBx, exagTF, totalExagTF);
		
		JPanel mainPanel = new JPanel();
		GroupLayout mpLayout = new GroupLayout(mainPanel);
		mainPanel.setLayout(mpLayout);
		
		//JButton popout = new JButton("VIEW");//TODO: implement pop out mode
		
		JSeparator verticalSep = new JSeparator(JSeparator.VERTICAL);
		
		JScrollPane scrollPane = new JScrollPane(canvasPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		canvasPanel.setPreferredSize(new Dimension(1141, 582));
		
		mpLayout.setHorizontalGroup(mpLayout.createSequentialGroup()
			.addComponent(controlPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addGap(2)
			.addComponent(verticalSep, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addGap(2)
			.addComponent(scrollPane));
		mpLayout.setVerticalGroup(mpLayout.createParallelGroup(Alignment.LEADING)
			.addComponent(controlPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addComponent(verticalSep)
			.addComponent(scrollPane));
		
		return mainPanel;
	}
	
	private AbstractAction saveAsAct = new AbstractAction("SAVE AS...") {
		public void actionPerformed(ActionEvent e) {
			JLabel title = new JLabel("Saving Options");
			JLabel pngLbl = new JLabel("Save as PNG");
			JLabel export3DLbl = new JLabel("Save 3D Printer File");
			JSeparator horiz1 = new JSeparator(JSeparator.HORIZONTAL);
			JSeparator horiz2 = new JSeparator(JSeparator.HORIZONTAL);
			JButton pngChooserBtn = new JButton(saveScreenAct);
			JButton save3DFileBtn = new JButton(saveStlAct);
			
			if (parentFrame != null && parentFrame.isVisible() && parentFrame.isShowing()) {
				saveAsDlg = new JDialog(parentFrame);
			} else {
				saveAsDlg = new JDialog(Main.mainFrame);//TODO: test out docked
			}
			
			JButton closeBtn = new JButton(new AbstractAction("CLOSE") {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					saveAsDlg.setVisible(false);
					saveAsDlg.dispose();
				}
			});
			save3DResultsLbl = new JLabel("No File Selected");
			pngResults = new JLabel("No file selected");
			
			
			
			JPanel savePanel = new JPanel();
			GroupLayout layout = new GroupLayout(savePanel);
			savePanel.setLayout(layout);
			layout.setAutoCreateContainerGaps(true);
			layout.setAutoCreateGaps(true);
			
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
				.addComponent(title)
				.addComponent(horiz1)
				.addComponent(pngLbl)
				.addGroup(layout.createSequentialGroup()
					.addComponent(pngChooserBtn)
					.addComponent(pngResults, 200,400,Short.MAX_VALUE))
				.addComponent(horiz2)
				.addComponent(export3DLbl)
				.addGroup(layout.createSequentialGroup()
					.addComponent(save3DFileBtn)
					.addComponent(save3DResultsLbl, 200,400,Short.MAX_VALUE))
				.addComponent(closeBtn));
			layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(title)
				.addComponent(horiz1)
				.addComponent(pngLbl)
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(pngChooserBtn)
					.addComponent(pngResults, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGap(10)
				.addComponent(horiz2)
				.addComponent(export3DLbl)
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(save3DFileBtn)
					.addComponent(save3DResultsLbl, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGap(10)
				.addComponent(closeBtn));
			
			layout.linkSize(pngChooserBtn, save3DFileBtn);
			layout.linkSize(pngResults, save3DResultsLbl);
			saveAsDlg.setTitle("3D Export/Save Options");
			saveAsDlg.setContentPane(savePanel);
			saveAsDlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			saveAsDlg.pack();
			if (parentFrame != null && parentFrame.isVisible() && parentFrame.isShowing()) {
				saveAsDlg.setLocationRelativeTo(parentFrame);
			} else {
				saveAsDlg.setLocationRelativeTo(Main.mainFrame);
			}
			saveAsDlg.setVisible(true);
		}
	};
	private AbstractAction saveScreenAct = new AbstractAction("SELECT LOCATION...") {
		public void actionPerformed(ActionEvent e) {
			// set up the chooser
			if (pngChooser == null) {
				pngChooser = new JFileChooser();
				pngChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
				pngChooser.setDialogTitle("Choose PNG Location");
				// add filter
				FileFilter pngFilter = new FileFilter() {
					public String getDescription() {
						return "Image File (.png)";
					}

					public boolean accept(File f) {
						if (f.isDirectory()) {
							return true;
						} else {
							return f.getName().toLowerCase().endsWith(".png");
						}
					}
				};
				pngChooser.addChoosableFileFilter(pngFilter);
				pngChooser.setFileFilter(pngFilter);
			}

			if (pngChooser.showSaveDialog(getFrame()) == JFileChooser.APPROVE_OPTION) {
				String fileStr = pngChooser.getSelectedFile().getPath();
				// check to see if user added extension, add it if they didn't
				if (!fileStr.contains(".png")) {
					fileStr += ".png";
				}
				// call save code
				canvasPanel.savePNG(fileStr);
				pngResults.setText("File written to: "+fileStr);
				pngResults.setToolTipText(fileStr);
			}
		}
	};

	private AbstractAction saveStlAct = new AbstractAction("SAVE 3D PRINTER FILE (STL)...") {
		public void actionPerformed(ActionEvent e) {
			// set up the directory chooser
			ThreeDPrintDialog dialog = ThreeDPrintDialog.getInstance(saveAsDlg);
			dialog.display(e, new Callable<Void>() {
				public Void call() {
					finishSave3dPrint(dialog);
					return null;
				}
			});
			
		}
	};

	private void finishSave3dPrint(ThreeDPrintDialog dlg) { 
		String fileStr = dlg.getFileStr();
		String nameStr = dlg.getNameStr();
		// call the save code
		boolean success = true;
		try {
			ThreeDFocus.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			canvasPanel.saveBinarySTL(fileStr, nameStr, dlg.getBaseThickness());
		} catch (Exception e1) {
			success = false;
			log.aprintln("Could not save stl file.");
			e1.printStackTrace();
		}

		ThreeDFocus.this.setCursor(Cursor.getDefaultCursor());

		if (success) {
			save3DResultsLbl.setText("3D export saved as "+fileStr);
			save3DResultsLbl.setToolTipText(fileStr);
		}
	}
	private AbstractAction viewControlAct = new AbstractAction("VIEW CONTROLS...") {
		public void actionPerformed(ActionEvent e) {

			// build the frame and panel to display
			// we only need to do this if the frame is null,
			// otherwise we just show it because it doesn't change.
			if (controlDialog == null) {
				controlDialog = new JDialog();
				controlDialog.setTitle("3D Controls");
				// hide the frame when it's closed, instead of disposing of it
				controlDialog.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				controlDialog.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent windowEvent) {
						controlDialog.setVisible(false);
					}
				});

				// build display
				JPanel backPnl = new JPanel();
				backPnl.setLayout(new BorderLayout());				
				backPnl.setBorder(new EmptyBorder(8, 8, 8, 8));
				JPanel controlPnl = new JPanel();
				controlPnl.setLayout(new GridBagLayout());
				controlPnl.setBorder(BorderFactory.createCompoundBorder(new TitledBorder("3D CONTROLS"),
						new EmptyBorder(0, 5, 5, 5)));
				JLabel keyLbl = new JLabel("Key Operations");
				JLabel mouseLbl = new JLabel("Mouse Operations");
				Font headerFont = new Font("Dialog", Font.BOLD, 14);
				Map attributes = headerFont.getAttributes();
				attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
				keyLbl.setFont(headerFont.deriveFont(attributes));
				mouseLbl.setFont(headerFont.deriveFont(attributes));

				// Control Labels
				// key labels
				JLabel focusMsg = createControlLbl("Note: you may need to click inside the display or tab until focus is on display");
				JLabel leftLbl = createControlLbl("Translate scene to the left");
				JLabel rightLbl = createControlLbl("Translate scene to the right");
				JLabel upLbl = createControlLbl("Translate scene up");
				JLabel downLbl = createControlLbl("Translate scene down");
				JLabel plusLbl = createControlLbl("Rotate scene counter-clockwise");
				JLabel minusLbl = createControlLbl("Rotate scene clockwise");
				JLabel wLbl = createControlLbl("Translate camera view up");
				JLabel aLbl = createControlLbl("Translate camera view left");
				JLabel sLbl = createControlLbl("Translate camera view down");
				JLabel dLbl = createControlLbl("Translate camera view right");
				JLabel zLbl = createControlLbl("Zoom out");
				JLabel ZLbl = createControlLbl("Zoom in");
				JLabel f5Lbl = createControlLbl("Update scene");
				// mouse labels
				JLabel scrollLbl = createControlLbl("Zoom in/out");
				JLabel ctrlScrollLbl = createControlLbl("Zoom in/out faster");
				JLabel dragLbl = createControlLbl("Rotate scene about x or y axis");
				JLabel ctrlDragLbl = createControlLbl("Translate scene");
				JLabel shiftVertLbl = createControlLbl("Zoom in/out");
				JLabel shiftHorLbl = createControlLbl("Rotate scene about z axis");

				row = 0;
				int pad = 1;
				Insets in = new Insets(pad, 5 * pad, pad, 5 * pad);
				controlPnl.add(keyLbl, new GridBagConstraints(0, row++, 2, 1, 0, 0, GridBagConstraints.CENTER,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(focusMsg, new GridBagConstraints(0, row++, 2, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Arrow Left"), new GridBagConstraints(0, row, 1, 1, 0, 0,
						GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(leftLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Arrow Right"), new GridBagConstraints(0, row, 1, 1, 0, 0,
						GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(rightLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Arrow Up"), new GridBagConstraints(0, row, 1, 1, 0, 0,
						GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(upLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Arrow Down"), new GridBagConstraints(0, row, 1, 1, 0, 0,
						GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(downLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("+"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(plusLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("-"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(minusLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("w"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(wLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("a"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(aLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("s"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(sLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("d"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(dLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Z"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(ZLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("z"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(zLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("F5"), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(f5Lbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(Box.createVerticalStrut(10), new GridBagConstraints(0, row++, 1, 1, 0, 0,
						GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(mouseLbl, new GridBagConstraints(0, row++, 2, 1, 0, 0, GridBagConstraints.CENTER,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Scroll Wheel"), new GridBagConstraints(0, row, 1, 1, 0, 0,
						GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(scrollLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Ctrl + Scroll Wheel"), new GridBagConstraints(0, row, 1, 1, 0, 0,
						GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(ctrlScrollLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0,
						GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Click & Drag"), new GridBagConstraints(0, row, 1, 1, 0, 0,
						GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(dragLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Ctrl + Click & Drag"), new GridBagConstraints(0, row, 1, 1, 0, 0,
						GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(ctrlDragLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Shift + Click & Drag Vert."), new GridBagConstraints(0, row, 1, 1, 0, 0,
						GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(shiftVertLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(new JLabel("Shift + Click & Drag Horz."), new GridBagConstraints(0, row, 1, 1, 0, 0,
						GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(shiftHorLbl, new GridBagConstraints(1, row++, 1, 1, 0, 0, GridBagConstraints.LINE_START,
						GridBagConstraints.NONE, in, pad, pad));

				backPnl.add(controlPnl);
				// add to dialog
				controlDialog.setContentPane(backPnl);
				controlDialog.pack();
				controlDialog.setLocationRelativeTo(getFrame());
			}
			// display dialog
			controlDialog.setVisible(true);
		}
	};

	private JLabel createControlLbl(String text) {
		JLabel label = new JLabel(text);
		label.setFont(descripFont);
		return label;
	}

	private AbstractAction updateAct = new AbstractAction("UPDATE SCENE") {
		public void actionPerformed(ActionEvent e) {
			// if lighting is on, make sure to update the light settings first
			if (lightChk.isSelected()) {
				Vector3f lightDir = lightWidget.getLightDirection();
				settings.directionalLightDirection = lightDir;
				canvasPanel.setDirectionalLightDirection(lightDir.x, lightDir.y, lightDir.z);
			}
			update();
		}
	};

	private AbstractAction resetCameraAct = new AbstractAction("RESET CAMERA") {
		public void actionPerformed(ActionEvent e) {
			settings.directionalLightBoolean = false;
			settings.directionalLightDirection = new Vector3f(0.0f, 0.0f, 20.0f);
			settings.directionalLightColor = new Color(128, 128, 128);
			settings.backgroundColor = new Color(0, 0, 0);
			backgroundCBtn.setColor(settings.backgroundColor);
			backgroundCBtn.setBackground(settings.backgroundColor);
			settings.backplaneBoolean = false;
			backplaneChk.setSelected(settings.backplaneBoolean);
			lightCBtn.setColor(settings.directionalLightColor);
			lightCBtn.setEnabled(false);
			lightWidget.setColor(new Color3f(settings.directionalLightColor));
			lightWidget.setLightDirection(settings.directionalLightDirection.x, settings.directionalLightDirection.y,
					settings.directionalLightDirection.z);
			lightWidget.repaint();
			lightChk.setSelected(false);
			settings.alpha = 0f;
			settings.beta = 0f;
			settings.gamma = 0f;
			settings.zoomFactor = 0.88f;
			settings.transX = 0f;
			settings.transY = 0f;
			settings.transZ = 0f;
			settings.xOffset = 0f; // JNN: added, should actually be at center of map, see ThreeDPanel's display()
			settings.yOffset = 0f; // JNN: added, should actually be at center of map, see ThreeDPanel's display()
			settings.zScaleString = "1.0"; // originalExaggeration; // JNN: modified
			settings.scaleOffset = (float) Config.get(Util.getProductBodyPrefix() + Config.CONFIG_THREED_SCALE_OFFSET,
					-0.002f);
			settings.scaleUnitsInKm = (settings.scaleOffset < 0.1) ? true : false;
			exagTF.setText(settings.zScaleString);
			canvasPanel.goHome(settings);
			parent.setVisible(true);
			parent.setDirty(true);
		}
	};

	// Display the NumericMapSourceDialog to allow the user to select a source.
	// Update text fields and the 3d panel when the new source is set.
	private AbstractAction sourceAct = new AbstractAction("SET VERTICAL SOURCE...") {
		public void actionPerformed(ActionEvent e) {
			ArrayList<MapSource> userSelectedSources = NumericMapSourceDialog.getUserSelectedSources(ThreeDFocus.this, false, true);
			if (userSelectedSources != null && userSelectedSources.size() > 0) {
				MapSource altitudeSource = userSelectedSources.get(0);
				// source can be null if the user cancels out of the dialog
				if (altitudeSource != null) {
					// update elevation source
					canvasPanel.setAltitudeSource(altitudeSource);
					myLayer.setElevationSource(altitudeSource);
					settings.setMapSource(altitudeSource);
					// must call this so the lview requests new tiles
					// with the new elevation source for the canvas to use
					parent.setDirty(true);

					// update 3d canvas and focus panel
					update();
				}
			}
		}
	};

	private ActionListener backplaneListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			settings.backplaneBoolean = backplaneChk.isSelected();
			canvasPanel.enableBackplane(settings.backplaneBoolean);
			canvasPanel.refresh();
		}
	};

	private ActionListener backgroundColorListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			final Color newColor = JColorChooser.showDialog(Util.getDisplayFrame(ThreeDFocus.this),
					backgroundCBtn.getText(), backgroundCBtn.getColor());
			if (newColor != null) {
				settings.backgroundColor = newColor;
				backgroundCBtn.setColor(newColor);
				backgroundCBtn.setBackground(newColor);
				canvasPanel.setBackgroundColor(newColor);
				canvasPanel.refresh();
			}
		}
	};

	private ActionListener lightListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			settings.directionalLightBoolean = !settings.directionalLightBoolean;
			if (settings.directionalLightBoolean == true) {
				lightCBtn.setEnabled(true);
				lightWidget.setEnabled(true);
			} else {
				lightCBtn.setEnabled(false);
				lightWidget.setEnabled(false);
			}
			lightWidget.repaint();
			canvasPanel.enableDirectionalLight(settings.directionalLightBoolean);
			canvasPanel.refresh();
		}
	};

	private ActionListener lightColorListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			final Color newColor = JColorChooser.showDialog(Util.getDisplayFrame(ThreeDFocus.this), lightCBtn.getText(),
					lightCBtn.getColor());
			if (newColor != null) {
				settings.directionalLightColor = newColor;
				lightCBtn.setColor(newColor);
				canvasPanel.setDirectionalLightColor(newColor);
				lightWidget.setColor(new Color3f(newColor));
				lightWidget.repaint();
				canvasPanel.refresh();
			}
		}
	};

	private ActionListener scaleListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			switch ((String) scaleBx.getSelectedItem()) {
			case ThreeDCanvas.SCALE_MODE_ST_DEV: // from standard deviation
				settings.scaleMode = ThreeDCanvas.SCALE_MODE_ST_DEV;
				scaleBx.setToolTipText("Divides the data by the standard deviation");
				break;
			case ThreeDCanvas.SCALE_MODE_RANGE: // from range of values
				settings.scaleMode = ThreeDCanvas.SCALE_MODE_RANGE;
				scaleBx.setToolTipText("Multiplies the data by 100% and divides by the range");
				break;
			case ThreeDCanvas.SCALE_MODE_AUTO_SCALE: // from auto scale
				settings.scaleMode = ThreeDCanvas.SCALE_MODE_AUTO_SCALE;
				scaleBx.setToolTipText("JMARS best guess for visual effect");
				break;
			case ThreeDCanvas.SCALE_MODE_ABSOLUTE: // from absolute values
				settings.scaleMode = ThreeDCanvas.SCALE_MODE_ABSOLUTE;
				scaleBx.setToolTipText("Uses source data unmodified");
				break;
			default: // unknown selection
				settings.scaleMode = ThreeDCanvas.SCALE_MODE_AUTO_SCALE;
				scaleBx.setToolTipText("JMARS best guess for visual effect");
				log.aprintln("Unknown scale mode. Setting to default");
				break;
			}
			canvasPanel.setScaleMode(settings.scaleMode, exagTF.getText());
			// update the scene by triggering the lview to get new mapdata
			parent.setDirty(true);
			updateExaggeration();
			update();
		}
	};

	private ActionListener exagListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			updateExaggeration();
			update();
		}
	};

	private FocusListener exagFocusListener = new FocusListener() {
		public void focusLost(FocusEvent e) {
			updateExaggeration();
			update();
		}

		public void focusGained(FocusEvent e) {
		}
	};

	private void updateExaggeration() {
		String prevExag = settings.zScaleString;

		// make sure it's a valid number
		try {
			String newExag = exagTF.getText();
			Float.parseFloat(newExag);

			settings.zScaleString = newExag;

		} catch (NumberFormatException e) {
			log.aprintln("Invalid exaggeration entry");
			exagTF.setText(prevExag);
		}

		// Set Total Exaggeration field
		float total = Math.abs(settings.scaleOffset);
		if (settings.scaleUnitsInKm) {
			// Convert back to meters to make sense to user
			total = total * 1000;
		}
		totalExagTF.setText(String.format("%7.3f", total));

	}

	private void updateMapAndScaleInfo(MapSource source) {
		// update the source title
		String sourceTxt = source.getTitle();
		sourceTF.setText(sourceTxt);


		// update the source ppd
		ppdLbl.setText(ppdPrompt + source.getMaxPPD());

		// update the source units
		if (source.getUnits() != null) {
			unitLbl.setText(unitPrompt + source.getUnits());
		} else {
			unitLbl.setText(unitPrompt + "Unavailable");
		}

		// update ignore value
		double[] vals = source.getIgnoreValue();
		if (vals != null) {
			ignoreLbl.setText(ignorePrompt + vals[0]);
		} else {
			ignoreLbl.setText(ignorePrompt + "None");
		}

		// update the mean/min/max/std values
		Elevation e = canvasPanel.getElevation();
		if (e != null) {
			minValueLbl.setText(String.format("%.5f",e.getMinAltitude()));
			meanValueLbl.setText(String.format("%.5f",e.getMean()));
			maxValueLbl.setText(String.format("%.5f",e.getMaxAltitude()));
			stdValueLbl.setText(String.format("%.5f",e.getStandardDeviation()));
		}
	}

	/** This updates the scene with new elevation data. */
	public void update() {
		myLayer.setStatus(Color.yellow);

		// Do whatever needs to be done to re-render the scene
		canvasPanel.updateElevationSource();
		canvasPanel.setScale(new Float(settings.zScaleString));
		canvasPanel.refresh();

		/**
		 ** Prevents the 3d window from re-appearing when we delete the layer. (added by
		 * Michael as a quick fix)
		 **/
		if (((ThreeDLView) parent).isDead)
			return;

		// clean up
		updateBtn.setEnabled(true);
		exagTF.setEnabled(true);
		parent.setVisible(true); // JNN: added
		parent.setDirty(false);

		// populate map and scale panels with info from intial source
		updateMapAndScaleInfo(myLayer.getElevationSource());

		myLayer.setStatus(Util.darkGreen);
	}

	public StartupParameters getSettings() {

		if (canvasPanel != null) {
			settings = canvasPanel.getSettings();
		}
		return settings;
	}

	public ThreeDCanvas getCanvasPanel() {
		return canvasPanel;
	}
	/**
	 * called by the focus panel whenever the lview is "cleaned up".
	 */
	public void destroyViewer() {
		if (lightWidget != null) {
			lightWidget.destroy();
			lightWidget = null;
		}
		if (canvasPanel != null) {
			canvasPanel.setVisible(false);
			canvasPanel.cleanup();
			canvasPanel = null;
		}
		if (controlDialog != null) {
			controlDialog.dispose();
		}
	}

	// An inner class that displays a button and allows the user to change a color
	// of some
	// component or other. It is used in this application to change the color of the
	// directional
	// light and the color of the background.
	private class ColorButton extends JButton {
		private Color color;

		public ColorButton(String l, Color c) {
			super(l);
			setContentAreaFilled(true);
			setColor(c);
			setFocusPainted(false);
		}

		// sets the background as the color of the button. If the color is lighter
		// than gray, then black is used for the color of the button's text instead
		// of white.
		public void setColor(Color c) {
			color = c;
			setBackground(c);
			if ((c.getRed() + c.getGreen() + c.getBlue()) > (128 + 128 + 128)) {
				setForeground(Color.black);
			} else {
				setForeground(Color.white);
			}
		}

		// returns the color that was previously defined.
		public Color getColor() {
			return color;
		}
	}
}