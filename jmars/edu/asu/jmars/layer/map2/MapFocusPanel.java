package edu.asu.jmars.layer.map2;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.EventListener;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.map2.msd.PipelineLegModelEvent;
import edu.asu.jmars.layer.map2.msd.PipelineModel;
import edu.asu.jmars.layer.map2.msd.PipelineModelEvent;
import edu.asu.jmars.layer.map2.msd.PipelineModelListener;
import edu.asu.jmars.layer.map2.stages.composite.CompositeStage;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HttpRequestType;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.CustomBalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;

public class MapFocusPanel extends FocusPanel implements PipelineEventListener, PipelineProducer {
	private static final long serialVersionUID = 1L;	
	final String chartTabName = "Chart";	
	final MapLView mapLView;
	
	JScrollPane inputSP;
	JPanel topPanel = new JPanel();
	JPanel inputPanel = new JPanel();
	JPanel legendPanel = new JPanel();
	JDialog legendDialog = null;
	JXTaskPaneContainer inputTaskContainer = new JXTaskPaneContainer();
	
	JButton configButton;
	// Chart view attached to the main view.
	ChartView chartView;
	// Used to avoid adding the chart view more than once
	boolean chartViewAdded = false;
	// PipelineEvent listeners list
	EventListenerList eventListenersList = new EventListenerList();
	// Current LView piplineModel
	private PipelineModel pipelineModel = new PipelineModel(new Pipeline[0]);
	

	//Legend related variables
	private BufferedImage legendImage = null;
	private JButton legendButton = null;
	private JLabel legendLbl = null;
	private Color imgBlack = Color.BLACK;
	private Color imgHiglight = ThemeProvider.getInstance().getBackground().getBorder();
	private Color imgPressed = ThemeProvider.getInstance().getBackground().getAlternateContrast();
	private Icon close = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CLEAR.withDisplayColor(imgBlack)));
	private Icon closeRollover = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CLEAR.withDisplayColor(imgHiglight)));
	private Icon closePressed = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CLEAR.withDisplayColor(imgPressed)));
	private CustomBalloonTip myBalloonTip = null;
	private boolean showLegendWarning = false;
	
	public MapFocusPanel(LView parent) {
		super(parent);
		mapLView = (MapLView)parent;
		initialize();
		
		initLegend();
		
		pipelineModel.addPipelineModelListener(new PipelineModelListener(){
			public void childrenAdded(PipelineModelEvent e) {}
			public void childrenChanged(PipelineModelEvent e) {}
			public void childrenRemoved(PipelineModelEvent e) {}
			public void compChanged(PipelineModelEvent e) {}

			public void forwardedEventOccurred(PipelineModelEvent e) {
				PipelineLegModelEvent le = e.getWrappedEvent();
				switch(le.getEventType()) {
					case PipelineLegModelEvent.STAGES_ADDED:
					case PipelineLegModelEvent.STAGES_REMOVED:
						firePipelineEvent(true, false);
						createLegendWarning();
						break;
					case PipelineLegModelEvent.STAGE_PARAMS_CHANGED:
						firePipelineEvent(true, true);
						break;
				}
			}
		});
		
		// Add ourselves to Source Configuration Panel's events so that we know of the
		// pipeline configuration changes sent down from the MapSettingsDialog.
		mapLView.getLayer().mapSettingsDialog.addPipelineEventListener(this);
		mapLView.getLayer().mapSettingsDialog.addPipelineEventListener(chartView);
		mapLView.getLayer().mapSettingsDialog.addPipelineEventListener(new PipelineEventListener(){
			public void pipelineEventOccurred(PipelineEvent e) {
				// If this layer starts out as non-numeric, but later is configured to include numeric data, add the Chart tab then
				if (!chartViewAdded) {
					MapFocusPanel.this.addTab(chartTabName.toUpperCase(), chartView);
				}
			}
		});
		// TODO: This is not a good way of doing things.
		//mapLView.getLayer().mapSettingsDialog.firePipelineEvent();
	}
	
	private void initialize(){
		// add configure button and call to show map sources dialog
		configButton = new JButton("CONFIGURE");		
		configButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				try {
					/*
					 * Push the current pipeline back to the MapSettingsDialog before
					 * showing it to get new map parameters. Note that we are not
					 * pusing the ChartPipeline back. This is because we don't modify
					 * the ChartPipeline locally, so the configuration within the
					 * MapSettingsDialog is current.
					 */ 
					Pipeline[] pipeline = pipelineModel.buildPipeline();
					mapLView.getLayer().mapSettingsDialog.setLViewPipeline(pipeline);
					mapLView.getLayer().mapSettingsDialog.dialog.setVisible(true);
				}
				catch(Exception ex){
					ex.printStackTrace();
				}
			}
		});
		
		
		// Add tab for the chart view
		chartView = new ChartView(mapLView);
		if (!chartView.hasEmptyPipeline()){
			this.add(chartTabName, chartView);
			chartViewAdded=true;
		}
		
		this.addTab("INPUT", inputPanel);
		
		legendButton = new JButton("MAP LEGEND");
		legendButton.setEnabled(false);//inactive by default
		legendButton.setToolTipText("There is no legend currently available in JMARS for this map. Please contact JMARS Support to request a legend for this map.");
		legendButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				popoutLegend();
			}
		});
	}
	
	public void initLegend() {
		 
		Runnable r = new Runnable() {

			@Override
			public void run() {
				String mapName = null;
				int tries = 0;
				do {
					try {
						Thread.sleep(1000);//new thread sleep for one second until we get a map name
					} catch (InterruptedException e) {
						DebugLog.instance().println("Waiting for map name thread - sleep interrupted");
					}
					mapName = mapLView.getLayer().getMapName();
					tries++;
					if (tries > 10) {//yes 10. At 5, I was able to sometimes get 1/5 to fail (same layer)
						break;
					}
				} while (mapName == null);

				if (mapName != null) {
					String mapLegendURL = Config.get("map_legend_url","https://jmars.mars.asu.edu/legend/get_legend.php");
					JmarsHttpRequest request = new JmarsHttpRequest(mapLegendURL, HttpRequestType.GET);
					request.addRequestParameter("map_name", mapName);
					request.setRetryNever();
					try {
						request.send();
						String imageUrl = Util.readResponse(request.getResponseAsStream());
						request.close();
						if (!"null".equalsIgnoreCase(imageUrl)) {
							JmarsHttpRequest legendRequest = new JmarsHttpRequest(imageUrl, HttpRequestType.GET);
							legendRequest.send();
							InputStream stream = legendRequest.getResponseAsStream();
							if (stream != null) {
								legendImage = ImageIO.read(stream);
								legendLbl = new JLabel(new ImageIcon(legendImage));
								SwingUtilities.invokeLater(new Runnable() {
									
									@Override
									public void run() {
										legendButton.setEnabled(true);
										legendButton.setToolTipText("Click on the legend when displayed to download the image.");
									}
								});
							}
						}
					} catch (Exception e) {
						DebugLog.instance().println("Exception when trying to get legend for map: "+mapName);
					}
				}
			}
		};
		Thread t = new Thread(r);
		t.start();
	}
	public void closeLegend() {
		if (legendDialog != null && legendDialog.isShowing()) {
			legendDialog.setVisible(false);
			legendDialog.dispose();
		}
	}
	private void popoutLegend() {
		legendDialog = new JDialog(Main.mainFrame);
		JPanel popoutPanel = new JPanel();
		GroupLayout layout = new GroupLayout(popoutPanel);
		popoutPanel.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
			.addComponent(legendLbl));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(legendLbl));
		
		legendLbl.setToolTipText("Click on the legend to download the image.");
		legendDialog.setTitle(mapLView.getLayerParameters().name+" legend");
		legendDialog.add(popoutPanel);
		legendDialog.pack();
		legendDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		legendDialog.setLocationRelativeTo(this);
		legendDialog.toFront();
		legendLbl.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				JPopupMenu menu = new JPopupMenu();
				JMenuItem download = new JMenuItem(new AbstractAction("Download") {
					
					@Override
					public void actionPerformed(ActionEvent arg0) {
						JFileChooser chooser = new JFileChooser(Util.getDefaultFCLocation());
						chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						if (chooser.showSaveDialog(legendDialog) == JFileChooser.APPROVE_OPTION) {
							String mapName = mapLView.getLayerParameters().name;
							mapName = mapName.replace(" ", "_");
							File toWrite = new File(chooser.getSelectedFile().getAbsolutePath() +  File.separator + mapName+".png");
							try {
								ImageIO.write(legendImage, "png", toWrite);
							} catch (IOException e) {
								DebugLog.instance().println(e.getMessage());
							}
						}
						
					}
				});
				menu.add(download);
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
			
		});
		legendDialog.setVisible(true);
		if (showLegendWarning) {
			createLegendWarning();
		}
	}
	
	private void createLegendWarning() {
		if (legendDialog != null && legendDialog.isShowing()) { 
			JLabel dummy = new JLabel("Adding stages, or changing the map colors in any way, may invalidate this map legend.");
			dummy.setForeground(Color.black);
			EdgedBalloonStyle style = new EdgedBalloonStyle(ThemeProvider.getInstance().getBackground().getHighlight(), 
		                ThemeProvider.getInstance().getBackground().getBorder());
			 BalloonTip.setDefaultCloseButtonIcons(close, closePressed, closeRollover);
			 myBalloonTip = new CustomBalloonTip(legendLbl,dummy,new Rectangle(),style, BalloonTip.Orientation.LEFT_ABOVE,  BalloonTip.AttachLocation.CENTER,40, 0,true);	
			 myBalloonTip.setPadding(5);
			// don't close the balloon when clicking the close-button, you just need to hide it
			 myBalloonTip.setCloseButton(BalloonTip.getDefaultCloseButton(),false);		
			 myBalloonTip.setVisible(true);
		} else {
			 showLegendWarning = true;
		}
	}
	public ChartView getChartView() {
		return chartView;
	}

	/*
	 * We receive these events from MapSettingsDialog in response to an OK button push.
	 * This means that we have a new pipeline, we should update our pipelineModel
	 * and the dialogs showing the stage panels.
	 * 
	 * This event is cascaded to both the main and panner views via their registered
	 * listeners.
	 * 
	 * (non-Javadoc)
	 * @see edu.asu.jmars.layer.map2.PipelineEventListener#pipelineEventOccurred(edu.asu.jmars.layer.map2.PipelineEvent)
	 */
	public void pipelineEventOccurred(PipelineEvent e) {
		Pipeline[] newPipeline;
		
		try {
			newPipeline = Pipeline.getDeepCopy(e.source.buildLViewPipeline());
		}
		catch(CloneNotSupportedException ex){
			ex.printStackTrace();
			newPipeline = new Pipeline[0];
		}
		
		pipelineModel.setFromPipeline(newPipeline, Pipeline.getCompStage(newPipeline));
		
		CompositeStage aggStage = pipelineModel.getCompStage();
		
		topPanel.removeAll();
		inputTaskContainer.removeAll();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.PAGE_AXIS));
		for(int i=0; i<pipelineModel.getSourceCount(); i++){
			PipelinePanel pp = new PipelinePanel(pipelineModel.getPipelineLeg(i));
			
			JXTaskPane inputTaskPane = new JXTaskPane();
			inputTaskPane.add(pp);
			inputTaskPane.setTitle(aggStage.getInputName(i));
	
			inputTaskContainer.add(inputTaskPane);
			
			topPanel.add(inputTaskContainer);
		}
			
		inputTaskContainer.repaint();
		topPanel.repaint();
		inputSP = new JScrollPane(topPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		inputSP.getVerticalScrollBar().setUnitIncrement(15);
		inputPanel.removeAll();
		inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.PAGE_AXIS));	
		inputPanel.add(inputSP);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.add(legendButton);
		buttonPanel.add(Box.createRigidArea(new Dimension(4,0)));
		buttonPanel.add(configButton);
		buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
		inputPanel.add(Box.createRigidArea(new Dimension(0,8)));
		inputPanel.add(buttonPanel);
		inputPanel.add(Box.createRigidArea(new Dimension(0,8)));
		inputTaskContainer.repaint();
		inputPanel.repaint();
		if (inputSP!=null) {
			this.remove(inputSP);
		}
		
		this.addTab("Input".toUpperCase(), inputPanel);
		firePipelineEvent(e.userInitiated, e.settingsChange);
	}
	
	public void addPipelineEventListener(PipelineEventListener l){
		eventListenersList.add(PipelineEventListener.class, l);
	}
	public void removePipelineEventListener(PipelineEventListener l){
		eventListenersList.remove(PipelineEventListener.class, l);
	}
	
	/**
	 * Fires a new pipeline event from this PipelineProducer.
	 * @param user If true, this is a user-initiated change
	 * @param settings If true, this is a stage settings change, otherwise a pipeline structure change.
	 */
	public void firePipelineEvent(boolean user, boolean settings) {
		PipelineEvent e = new PipelineEvent(this, user, settings);
		EventListener[] listeners = eventListenersList.getListeners(PipelineEventListener.class);
		for(int i=0; i<listeners.length; i++){
			((PipelineEventListener)listeners[i]).pipelineEventOccurred(e);
		}
	}
	
	// Unused, this does not produce chart pipeline
	public Pipeline[] buildChartPipeline() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Produces a pipeline suitable for ONE view. Even if the views are using
	 * the same conceptual pipeline, this method must still be called once for
	 * each view.
	 */
	public Pipeline[] buildLViewPipeline() {
		Pipeline[] pipeline = pipelineModel.buildPipeline();
		return Pipeline.replaceAggStage(pipeline, 
				CompStageFactory.instance().getStageInstance(Pipeline.getCompStage(pipeline).getStageName()));
	}
	
	public PipelineModel getLViewPipelineModel(){
		return pipelineModel;
	}
}
