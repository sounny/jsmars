package edu.asu.jmars;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeProgressBar;
import edu.asu.jmars.util.Util;

@SuppressWarnings("serial")
public class SplashScreen extends JFrame {
	private SplashPanel splashPnl;
	private BufferedImage splashImg;
	private JProgressBar progress;
	private ArrayList<ProgressStage> progressValues;
	private JLabel progressLbl;
	
	private int pad = 3;
	private Insets in = new Insets(pad, pad, pad, pad);
	
	public SplashScreen(){
		progressValues = new ArrayList<ProgressStage>(Arrays.asList(ProgressStage.values()));
		
		buildUI();
		setVisible(true);
		
	}
	
	private void buildUI(){
		//set properties on the frame
		setIconImage(Util.getJMarsIcon());
		setUndecorated(true);
		//create the splash image and set it on the frame
		splashImg = Util.getGenericSplashScreen();
		splashPnl = new SplashPanel();
		splashPnl.setLayout(new GridBagLayout());
		//for text spacing on labels
		Map<TextAttribute, Object> spacingAtt = new HashMap<TextAttribute, Object>();
		spacingAtt.put(TextAttribute.TRACKING, 0.08);
		//add version label
		JLabel verLbl = new JLabel("Version: "+Util.getVersionNumber());
		verLbl.setForeground(ThemeProvider.getInstance().getAction().getDefaultForeground());
		verLbl.setOpaque(false);
		verLbl.setFont(ThemeFont.getBold().deriveFont(16f).deriveFont(spacingAtt));
		//create the progress label to show the status
		progressLbl = new JLabel();
		progressLbl.setForeground(ThemeProvider.getInstance().getAction().getDefaultForeground());
		progressLbl.setOpaque(false);
		progressLbl.setFont(ThemeFont.getRegular().deriveFont(18f));
		//create the progress bar for the splash screen
		progress = new JProgressBar(0, progressValues.size());
		progress.setBorder(new EmptyBorder(0, 0, 0, 0));
		progress.setForeground(((ThemeProgressBar)GUITheme.get("progressbar")).getSplash());
		progress.setBackground(((ThemeProgressBar)GUITheme.get("progressbar")).getBackground());
		
		int row = 0;
		splashPnl.add(verLbl, new GridBagConstraints(0, 0, row++, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(270, pad, 20, pad), pad, pad));
		splashPnl.add(progressLbl, new GridBagConstraints(0, row++, 1, 1, 0, 1, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(pad, 15, 15, pad), pad, pad));
		splashPnl.add(progress, new GridBagConstraints( 0, row++, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
		
		getContentPane().add(splashPnl);
		
		//set the size and location and then display
		setSize(new Dimension(splashImg.getWidth(), splashImg.getHeight()));
		setLocationRelativeTo(null);
	}
	
	
	
	/**
	 * Updates the splash panel to have the progress bar be at
	 * the current location for the passed in stage.
	 * Updates the label to read out the description of the 
	 * current stage.
	 * 
	 * This method has to be called on the event dispatching thread
	 * 
	 * @param stage The progress stage currently at
	 */
	public void updateProgress(ProgressStage stage){
		int index = progressValues.indexOf(stage);
		
		progressLbl.setText(ProgressStage.getDescription(stage).toUpperCase());
		progress.setValue(index);
		splashPnl.paintImmediately(0, 0, splashPnl.getWidth(), splashPnl.getHeight());
		
		Main.resetSplashTimer(ProgressStage.getDescription(stage));
	}
	
	
	class SplashPanel extends JPanel{
		public void paintComponent(Graphics g){
			g.drawImage(splashImg, 0, 0, null);
		}
	}
	
	public enum ProgressStage{
		LOADING_AUTO_SAVE,
		CREATING_MAIN_FRAME,
		INITIALIZING_3D,
		CREATING_JMARS_DIRECTORY,
		CREATING_TEST_DRIVER,
		CREATING_LMANAGER,
		BUILDING_RULERS,
		LOADING_DEFAULT_LAYERS,
		LOADING_LAYER_PARAMETERS,
		OPENING_WINDOW;
		;
		
		public static String getDescription(ProgressStage stage){
			switch(stage){
				case LOADING_AUTO_SAVE:
					return "Loading auto save file...";
				case CREATING_MAIN_FRAME:
					return "Creating main frame...";
				case CREATING_JMARS_DIRECTORY:
					return "Creating jmars directories...";
				case CREATING_TEST_DRIVER:
					return "Creating JMARS driver...";
				case CREATING_LMANAGER:
					return "Creating layer manager...";
				case BUILDING_RULERS:
					return "Building rulers...";
				case LOADING_DEFAULT_LAYERS:
					return "Loading default layers...";
				case LOADING_LAYER_PARAMETERS:
					return "Loading layer parameters...";
				case OPENING_WINDOW:
					return "Opening JMARS window...";
				case INITIALIZING_3D:
					return "3D init: if stuck here, use advanced->disable 3D on login screen";
				default:
					return "";
			}
		}
	}
}
