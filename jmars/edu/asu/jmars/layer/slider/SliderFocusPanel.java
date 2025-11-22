package edu.asu.jmars.layer.slider;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import edu.asu.jmars.Main;
import edu.asu.jmars.TestDriverLayered.PannerMode;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.map2.CacheManager;
import edu.asu.jmars.layer.map2.MapRequest;
import edu.asu.jmars.layer.map2.MapRetriever;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.MapTile;
import edu.asu.jmars.swing.IconButtonUI;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.PLAY;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.PAUSE;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.STEP_NEXT;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.STEP_PREV;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeCheckBox;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeText;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;
import mdlaf.components.button.MaterialButtonUI;


public class SliderFocusPanel extends FocusPanel {	

	int lastVal=-1;
// Relevant images
	private static Color img = ((ThemeImages) GUITheme.get("images")).getFill();
	private static Color disabledtext = ((ThemeText) GUITheme.get("text")).getTextDisabled();
	private static ImageIcon play = new ImageIcon(ImageFactory.createImage(PLAY
			 				.withDisplayColor(img)));
	private static ImageIcon playD = new ImageIcon(ImageFactory.createImage(PLAY
				.withDisplayColor(disabledtext)));
	
    private static ImageIcon pause = new ImageIcon(ImageFactory.createImage(PAUSE
									.withDisplayColor(img)));
    private static ImageIcon pauseD = new ImageIcon(ImageFactory.createImage(PAUSE
			.withDisplayColor(disabledtext)));	
	
    private static ImageIcon prev = new ImageIcon(ImageFactory.createImage(STEP_PREV
							.withDisplayColor(img)));    
    private static ImageIcon prevD = new ImageIcon(ImageFactory.createImage(STEP_PREV
			.withDisplayColor(disabledtext)));	
	
    private static ImageIcon next = new ImageIcon(ImageFactory.createImage(STEP_NEXT
							.withDisplayColor(img)));
    private static ImageIcon nextD = new ImageIcon(ImageFactory.createImage(STEP_NEXT
			.withDisplayColor(disabledtext)));	
	
// Fonts used for the highlight process	
	private Font hFont = ThemeFont.getBold();
	private Font dFont = ThemeFont.getRegular();
// Relevant components	
	public JPanel controlPanel, playPanel, spinnerPanel, statusPanel, selectionPanel, 
					scrollPanePanel, displayPanel, northPanel, nWest, nEast, nNorth, 
					west, east, centerPanel, southPanel;
	public JScrollPane scrollPane;
	final JButton playButton;
	public JButton mainbuttons[];
	public JButton panbuttons[];
	public JCheckBox checkBoxes[];
	public JSlider slider;
// Relevant LView	
	private SliderLView sParent;
// time slider period/duration	
	protected int movieSpeed = 1000; // in milliseconds
	
	public SliderFocusPanel(final LView parent) {
		super(parent);
		sParent = (SliderLView)parent;
		
// Title is added at the top of the Control Panel
// TODO make this title bold/bigger!!		
		JLabel cTitle = new JLabel("Controls");
		cTitle.setAlignmentX(CENTER_ALIGNMENT);		
		cTitle.setFont(ThemeFont.getBold().deriveFont(17f));

// Slider will be added next in the control panel	
		slider = new JSlider(1,sParent.sources.size());
		slider.setValue(1);
		slider.setMaximumSize(new Dimension(300,0));
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (lastVal==slider.getValue()) return;
			
				lastVal=slider.getValue();
				parent.viewChanged();
				parent.getChild().viewChanged();
				highlightName();
			}
		});
		
// This next block of code creates the buttons for the PlayPanel-----------
//--------  and defines their actionListeners------------------------------	
		
		final JButton prevButton = new JButton("Prev".toUpperCase());
		setButton(prevButton, prev);
		prevButton.setDisabledIcon(prevD);
		
		prevButton.addActionListener(new ActionListener() {	
			public void actionPerformed(ActionEvent e) {
				int nextVal = nextSelection(false);
				slider.setValue(nextVal);
			}
		});
			
		playButton = new JButton("Play".toUpperCase());
		setButton(playButton, play);
		playButton.setDisabledIcon(playD);

		playButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
            	playButton.setSelected(!playButton.isSelected());
            	if (playButton.isSelected()) {
            		playButton.setText("Pause".toUpperCase());
            		setButton(playButton, pause);
            		playButton.setDisabledIcon(pauseD);
            	} else {
            		playButton.setText("Play".toUpperCase());
            		setButton(playButton, play);
            		playButton.setDisabledIcon(playD);
            	}
            	TimerTask timerTask = new TimerTask() {	
         			public void run() {
         				if (playButton.isSelected()) {
         					int nextVal = nextSelection(true);
         					// bunch of state flags to keep track of where we are in fetching the maps
         					// for the next step in the slide show
         					boolean panReqSent = false;
         					boolean vwReqSent = false;
         					boolean viewMapAvail = false;
         					boolean panMapAvail = false;
         					MapRequest panMr = null;
         					MapRequest  viewMr = null;
          			    	MapSource ms = sParent.sources.get(nextVal);
          			    	// panner map request
          			    	if (sParent.getChild().isAlive()) { 
          			    		panMr = new MapRequest(ms, sParent
									.getChild().getProj().getWorldWindow(),
									sParent.getChild().viewman.getZoomManager()
											.getZoomPPD(), Main.PO);
          			    	} else {
          			    		panMapAvail = true;
          			    	}
          			    	
							// view map request
							viewMr = new MapRequest(ms, sParent
									.getProj().getWorldWindow(),
									sParent.viewman.getZoomManager()
											.getZoomPPD(), Main.PO);
         					while (!(viewMapAvail && panMapAvail)) {
         						// if the slide show is stopped by the user no need to continue
         						if (!playButton.isSelected()) {
         							cancel();
         							return;
         						}
	         					// check to see if the next map is available
								if (panMr != null && ((SliderLView)sParent.getChild()).isMapCached(panMr) 
										&& panbuttons[nextVal].getBackground().equals(Color.GREEN)) {
									panMapAvail = true;
								} else if (panMr != null && !panReqSent){
									// request the needed map
									final int val = nextVal;
									final MapRequest pMapReq = viewMr;
									SwingUtilities.invokeLater(new Runnable() {
										public void run() {
											((SliderLView)sParent.getChild()).requestMap(pMapReq.getSource(), val, true);
										}
									});
	         						panReqSent = true;
								}
								// same as above for the main view
								if (sParent.isMapCached(viewMr) && mainbuttons[nextVal].getBackground().equals(Color.GREEN)) {
									viewMapAvail = true;
								} else 
									if (!vwReqSent){
										final int val = nextVal;
										final MapRequest vMapReq = viewMr;
										SwingUtilities.invokeLater(new Runnable() {
											public void run() {
												sParent.requestMap(vMapReq.getSource(), val, true);
											}
										});
	         						vwReqSent = true;
								}
								Thread.yield();
         					}
         					final JSlider theSlider = slider;
         					final int theNextVal = nextVal+1;
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									theSlider.setValue(theNextVal);
								}
							});
         				} else {
         					cancel();
         				}
        			}			
        		};	
            	Timer playTimer=new Timer();
            	playTimer.schedule(timerTask, movieSpeed, movieSpeed);
			}
		});

		final JButton nextButton = new JButton("Next".toUpperCase());
		setButton(nextButton, next);
		nextButton.setDisabledIcon(nextD);
		
		nextButton.addActionListener(new ActionListener() {	
			public void actionPerformed(ActionEvent e) {
				int nextVal = nextSelection(true);
				slider.setValue(nextVal+1);
			}
		});
//-----------------------------------------------------------------------------	
		
// The playPanel contains the three control buttons, is added after slider
		playPanel = new JPanel();
		playPanel.add(Box.createRigidArea(new Dimension(3,0)));
		playPanel.add(prevButton);
		playPanel.add(Box.createRigidArea(new Dimension(3,0)));
		playPanel.add(playButton);
		playPanel.add(Box.createRigidArea(new Dimension(3,0)));
		playPanel.add(nextButton);	
		playPanel.add(Box.createRigidArea(new Dimension(3,0)));		
// Creates the spinner and label go with it, they get added to the spinnerPanel		
		SpinnerNumberModel movieSpeedSpinner = new SpinnerNumberModel((movieSpeed/1000), 0.5, 5.0, 0.5);
	    final JSpinner movieSpinner = new JSpinner(movieSpeedSpinner);
	    movieSpinner.setEditor(new JSpinner.DefaultEditor(movieSpinner));
	    movieSpinner.setToolTipText("Use this value to control movie play speed");
	    //some wild guesses for size and location
	    movieSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
	    movieSpinner.setAlignmentY(Component.CENTER_ALIGNMENT);
	    movieSpinner.setPreferredSize(new Dimension(65,26));
	    movieSpinner.setMaximumSize(new Dimension(65,26));
	    movieSpinner.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				movieSpeed=(int)(((Double)movieSpinner.getModel().getValue()).floatValue() * 1000);
				if (movieIsPlaying()) {
					pauseMovie();
				}
				sParent.repaint();
				if (sParent.getChild().isAlive()) {
					((SliderLView)sParent.getChild()).repaint();
				}
			}
		});
	    JLabel spinLabel1 = new JLabel("Time delay:   ");
	    spinLabel1.setFont(ThemeFont.getBold());
	    JLabel spinLabel2 = new JLabel("  seconds");
	    spinLabel2.setFont(ThemeFont.getBold());
// The spinnerPanel contain the spinner and it's label, this is added to the controlpanel
	    spinnerPanel = new JPanel();
	    spinnerPanel.setLayout(new BoxLayout(spinnerPanel, BoxLayout.LINE_AXIS));
	    spinnerPanel.add(spinLabel1);
	    spinnerPanel.add(movieSpinner);
	    spinnerPanel.add(spinLabel2);

// The controlPanel is added inside the northPanel and contains a title,
//	slider, and the playPanel (complete with buttons)		
		controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.PAGE_AXIS));
		controlPanel.setBorder(new EtchedBorder());
		controlPanel.add(cTitle);
		controlPanel.add(Box.createRigidArea(new Dimension(0,4)));
		controlPanel.add(slider);
		controlPanel.add(playPanel);
		controlPanel.add(Box.createRigidArea(new Dimension(0,5)));
		controlPanel.add(spinnerPanel);
		controlPanel.add(Box.createRigidArea(new Dimension(0,5)));
		controlPanel.setMinimumSize(controlPanel.getPreferredSize());
		controlPanel.setMaximumSize(controlPanel.getPreferredSize());
// These two panels are just to take up the appropriate space in the northPanel
		nWest = new JPanel();	
		nEast = new JPanel();		
		nNorth = new JPanel();		
// The northPanel is added to the "NORTH" of the display panel and contains the
//  controlPanel		
		northPanel = new JPanel();		
		northPanel.setLayout(new BorderLayout());
		northPanel.add(controlPanel,BorderLayout.CENTER);
		northPanel.add(nWest, BorderLayout.WEST);
		northPanel.add(nEast, BorderLayout.EAST);
		northPanel.add(nNorth, BorderLayout.NORTH);
		
// The statusPanel and selectionPanel's are made next, they are added side by
//  side into the scrollPanePanel.  Both of them are populated dynamically in
//  the for loop below		
	//TODO make this title bold/bigger	
		JLabel statusTitle = new JLabel("Load Status:");
		statusTitle.setFont(ThemeFont.getBold().deriveFont(15f));
		statusPanel = new JPanel();
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.PAGE_AXIS));
		statusPanel.setAlignmentY(TOP_ALIGNMENT);
		statusPanel.add(Box.createRigidArea(new Dimension(0,2)));
		statusPanel.add(statusTitle);
		statusPanel.add(Box.createRigidArea(new Dimension(0,7)));
	
	//TODO make this title bold/bigger
		JLabel selectionTitle = new JLabel("Selections:");
		selectionTitle.setFont(ThemeFont.getBold().deriveFont(15f));
		selectionPanel = new JPanel();
		selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.PAGE_AXIS));
		selectionPanel.setAlignmentY(TOP_ALIGNMENT);
		selectionPanel.add(Box.createRigidArea(new Dimension(0,2)));
		selectionPanel.add(selectionTitle);
		selectionPanel.add(Box.createRigidArea(new Dimension(0,8)));

// mainButtons and panButtons are added to a tempSPanel which is added to the statusPanel
//  for every map on the list.  checkBoxes are added to the selectionPanel. 		
		mainbuttons = new JButton[sParent.sources.size()];
		panbuttons = new JButton[sParent.sources.size()];
		checkBoxes = new JCheckBox[sParent.sources.size()];		
		Dimension spGap = new Dimension(0,2);
		for (int i=0; i<sParent.sources.size(); i++) {
			JPanel tempSPanel = new JPanel();
			tempSPanel.setLayout(new BoxLayout(tempSPanel, BoxLayout.LINE_AXIS));
			tempSPanel.setAlignmentX(LEFT_ALIGNMENT);
			mainbuttons[i]=new JButton("M".toUpperCase());
			mainbuttons[i].setUI(new SliderFocusButtonUI());
			panbuttons[i]=new JButton("P".toUpperCase());
			panbuttons[i].setUI(new SliderFocusButtonUI());
			tempSPanel.add(mainbuttons[i]);
			tempSPanel.add(Box.createRigidArea(new Dimension(10,0)));
			tempSPanel.add(panbuttons[i]);
			statusPanel.add(Box.createRigidArea(new Dimension(10,5)));
			statusPanel.add(tempSPanel);

			checkBoxes[i] = new JCheckBox(sParent.sources.get(i).getTitle());
			checkBoxes[i].setSelected(true);
			checkBoxes[i].setFont(ThemeFont.getRegular());
			selectionPanel.add(Box.createRigidArea(new Dimension(0,11)));
			selectionPanel.add(checkBoxes[i]);
			selectionPanel.add(Box.createRigidArea(spGap));
		}
		
// The scrollPanePanel is added to the scrollPane and contains both the 
//  statusPanel (left) and selectionPanel(right)		
		scrollPanePanel = new JPanel();
		scrollPanePanel.setLayout(new BoxLayout(scrollPanePanel, BoxLayout.LINE_AXIS));
		scrollPanePanel.add(Box.createGlue());
		scrollPanePanel.add(statusPanel);
		scrollPanePanel.add(Box.createRigidArea(new Dimension(5,0)));
		scrollPanePanel.add(selectionPanel);
		scrollPanePanel.add(Box.createGlue());

// The scrollPane contains the scrollPanePanel and sets visibility of the scrollbars, is 
//  added to the centerPanel		
		scrollPane = new JScrollPane(scrollPanePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, 
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(4);

// The centerPanel contains the scrollPane and a slight gap at the top (for better aesthetics),
//  it will be added to the displayPanel		
		centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));		
		centerPanel.add(Box.createRigidArea(new Dimension(0,10)));
		centerPanel.add(scrollPane);
		
// east and west panels are also created (for aesthetical reasons) and will be added to the displayPanel		
		west = new JPanel();	
		east = new JPanel();		

		
// load button is created and will be added to the southPanel
		JButton load = new JButton("Preload All Data".toUpperCase());
		load.setPreferredSize(load.getPreferredSize());
		load.setMaximumSize(load.getPreferredSize());
		load.setAlignmentX(CENTER_ALIGNMENT);
		load.addActionListener(new ActionListener() {	
			public void actionPerformed(ActionEvent e) {
				// pre-fetch all the maps for the new views
				for (int i=0; i<sParent.sources.size(); i++) {
			    	MapSource ms = sParent.sources.get(i);
			    	if (sParent.getChild().isAlive()) {
						if (panbuttons[i].getBackground().equals(Color.WHITE)) {
							panbuttons[i].setBackground(Color.RED);
						}
			    		((SliderLView)sParent.getChild()).requestMap(ms, i, false);
			    	}
					if (mainbuttons[i].getBackground().equals(Color.WHITE)) {
						mainbuttons[i].setBackground(Color.RED);
					}
					sParent.requestMap(ms, i, false);			    	
				}
			}
		});
		
// southPanel contains the load button and is added to the displayPanel
		southPanel = new JPanel();
		southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.PAGE_AXIS));		
		southPanel.add(Box.createRigidArea(new Dimension(0,10)));
		southPanel.add(load);
		southPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		
// The displayPanel is what sits inside of the actual FocusPanel (as a tabbed pane)
//  and contains the east, west, northpanel, southpanel and centerpanel components		
		displayPanel = new JPanel();
		displayPanel.setLayout(new BorderLayout());		
		displayPanel.add(west, BorderLayout.WEST);
		displayPanel.add(east, BorderLayout.EAST);
		displayPanel.add(northPanel, BorderLayout.NORTH);
		displayPanel.add(centerPanel, BorderLayout.CENTER);
		displayPanel.add(southPanel, BorderLayout.SOUTH);
		
// This adds the displayPanel to the focus panel's tabs		
		add("Adjustments", displayPanel);
// This changes the color of the checkbox of the current map that is displayed by 
//  default		
		highlightName();
		
	}// end of constructor

	
	public void updateStatus() {
		if (parent==null || parent.getProj()==null) return;

		Rectangle2D extent = parent.getProj().getWorldWindow();
		int ppd = 	parent.viewman.getZoomManager().getZoomPPD();		
		
		for (int i=0; i<sParent.sources.size(); i++) {
			MapRequest mr=new MapRequest(sParent.sources.get(i), extent, ppd, Main.PO);
			
			Set<MapTile> tileSet=MapRetriever.createTiles(mr);
							
			MapTile[] tilesToCheck=new MapTile[tileSet.size()];
			
			tilesToCheck=tileSet.toArray(tilesToCheck);
			
			MapTile[][] tiles=CacheManager.checkCache(tilesToCheck);
			
		
			if (tiles[1].length==0) {
				if (!mainbuttons[i].getBackground().equals(Color.GREEN)) {
					mainbuttons[i].setBackground(Color.GREEN);
				}
			} else {
				if (!mainbuttons[i].getBackground().equals(Color.WHITE)) {
					mainbuttons[i].setBackground(Color.WHITE);
				}
			}
		}
		
		
		if (!parent.getChild().isVisible() || Config.get("panner.mode", PannerMode.Horiz.ordinal()) == PannerMode.Off.ordinal()){
			for (int i=0; i<sParent.sources.size(); i++) {
					panbuttons[i].setBackground(Color.WHITE);
					enablePannerButton(i, false);
			}
			return;
		}
		
		else{
			Rectangle2D extent2 = parent.getChild().getProj().getWorldWindow();
			int ppd2 = 	parent.getChild().viewman.getZoomManager().getZoomPPD();
		
			for (int i=0; i<sParent.sources.size(); i++) {
		
				MapRequest mr=new MapRequest(sParent.sources.get(i), extent2, ppd2, Main.PO);
				
				Set<MapTile> tileSet=MapRetriever.createTiles(mr);
				
				
				MapTile[] tilesToCheck=new MapTile[tileSet.size()];
				
				tilesToCheck=tileSet.toArray(tilesToCheck);
				
				MapTile[][] tiles=CacheManager.checkCache(tilesToCheck);
				
				enablePannerButton(i, true);
				if (tiles[1].length==0) {
					if (!panbuttons[i].getBackground().equals(Color.GREEN)) {
						panbuttons[i].setBackground(Color.GREEN);
					}
				} else {
					if (!panbuttons[i].getBackground().equals(Color.WHITE)) {
						panbuttons[i].setBackground(Color.WHITE);
					}
				}
			}
		}
	}
	

	public void highlightName(){
		Color foregroundhi = ((ThemeCheckBox)GUITheme.get("checkbox")).getForegroundhilight();
		Color foreground = ((ThemeCheckBox)GUITheme.get("checkbox")).getForeground();
		for(int i=0; i<sParent.sources.size(); i++){
			if(slider.getValue()-1 == i){
				checkBoxes[i].setForeground(foregroundhi);
				checkBoxes[i].setFont(hFont);
			}
			else{
				checkBoxes[i].setForeground(foreground);
				checkBoxes[i].setFont(dFont);
			}
		}
	}
	
	public void setButton(JButton b, ImageIcon i) {
		b.setIcon(i);
		b.setUI(new IconButtonUI());
		Dimension bSize = new Dimension(70, 50);
		b.setMinimumSize(bSize);
		b.setPreferredSize(bSize);
		b.setMaximumSize(bSize);
		b.setVerticalTextPosition(SwingConstants.BOTTOM);
		b.setHorizontalTextPosition(SwingConstants.CENTER);		
		b.setAlignmentX(CENTER_ALIGNMENT);
	}
	
	final protected boolean movieIsPlaying() {
		return playButton.isSelected();
	}
	
	final synchronized protected void pauseMovie() {
		playButton.doClick();
	}
	
	public synchronized void enablePannerButton (int index, boolean state) {
		panbuttons[index].setEnabled(state);
	}

	public int nextSelection(boolean moveForward) {
		int curVal = slider.getValue();
		int nextVal = curVal;
		
		if (moveForward) {
			nextVal = (curVal > slider.getMaximum()) ? slider.getMinimum() - 1
					: curVal;
			// find the next map the user wants to see			
			for (int i = nextVal; i < checkBoxes.length + nextVal; i++) {
				if (checkBoxes[i % checkBoxes.length].isSelected()) {
					nextVal = i % checkBoxes.length;
					break;
				} else if (i == checkBoxes.length + nextVal - 1) {
					// special case
					// if we get here the user has unchecked everything
					// so make sure we stop the movie
					nextVal = i % checkBoxes.length;
					if (movieIsPlaying()) {
						pauseMovie();
					}
					break;
				}
			}
		} else { // the user is backing up the list
			int nextLower = nextVal;
			do {	
				if (nextLower -1 < slider.getMinimum()) {
					nextLower = slider.getMaximum();
				} else {
					nextLower--;
				}
				if (nextLower == nextVal || checkBoxes[nextLower-1].isSelected()) {
					nextVal = nextLower;
					break;
				}
					
			} while (true);
		}
		return nextVal;
	}
	
	public class SliderFocusButtonUI extends MaterialButtonUI{

		@Override
		public void installUI(JComponent c) {
			super.mouseHoverEnabled = false;
			super.installUI(c);
			super.borderEnabled = false;
		}

		@Override
		protected void paintBackground(Graphics g, JComponent c) {			
			super.paintBackground(g, c);
			calculateForegroundColor(c);
		}

		private void calculateForegroundColor(JComponent c) {
			Color actualBackground = c.getBackground();
			if(actualBackground.equals(Color.YELLOW) || actualBackground.equals(Color.WHITE)){
				c.setForeground(Color.BLACK);
			}else{
				c.setForeground(Color.WHITE);
			}
		}


	}
	
}
