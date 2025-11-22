package edu.asu.jmars.layer.map2;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.PAN_W;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.PAN_E;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.PAN_N;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.PAN_S;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.DOT;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.AncestorEvent;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.ProjectionListener;
import edu.asu.jmars.layer.map2.msd.PipelineLegModel;
import edu.asu.jmars.layer.map2.msd.PipelineLegModelEvent;
import edu.asu.jmars.layer.map2.msd.PipelineLegModelListener;
import edu.asu.jmars.layer.map2.stages.ColorStretcherStage;
import edu.asu.jmars.layer.map2.stages.ColorStretcherStageSettings;
import edu.asu.jmars.layer.map2.stages.ColorStretcherStageView;
import edu.asu.jmars.layer.map2.stages.GrayscaleStage;
import edu.asu.jmars.layer.map2.stages.GrayscaleStageSettings;
import edu.asu.jmars.swing.AncestorAdapter;
import edu.asu.jmars.swing.IconButtonUI;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.Util;


public class PipelinePanel extends JPanel implements PipelineLegModelListener, ProjectionListener {
	private static final long serialVersionUID = 1L;
	
	public static final String firstInsertButtonLabel = "+ ADD STAGE";
	public static final String insertButtonLabel = "+";
	public static final String removeButtonLabel = "-";
	public static final int    offsetDecimalDigits = 4;
	
	JPanel innerPanel;
	PipelineLegModel legModel;
	
	// set an instance variable which will contain the map source.
	private final MapSource mapSource;
	
	// Used to display the nudge offset if the nudge panel is present.
	private final JLabel offsetXLabel = new JLabel();
	private final JLabel offsetYLabel = new JLabel();
	
	private JTextField maxPPDField;
	private JTextField ignoreValueField;
	
	public PipelinePanel(PipelineLegModel legModel){
		super();
		this.legModel = legModel;
		this.legModel.addPipelineLegModelListener(this);
		this.mapSource = getMapSource();
		initialize();
		
		// add this PipelinePanel as a projection listener so it can react to projection changes.
		Main.addProjectionListener(this);
	}
	
	public PipelineLegModel getModel(){
		return legModel;
	}
	
	/**
	 * Filters the input list of Stages to returns a list of all those Stages 
	 * that can be inserted at the specified index. Filtering is done by looking at
	 * whether input from one stage can be patched through to the next stage or not.
	 * 
	 * @param possibleStages Potential list of stages to be filtered.
	 * @param index Index at which the Stage is destined for.
	 * @return Filtered, possibly empty list of StageViews that can go at the 
	 *         specified index.
	 */
	private List<Stage> filterForInsertByStagePosition(List<Stage> possibleStages, int index){
		if (index < 0)
			return Collections.emptyList();
		
		// get input MapAttr from MapSource or previous stage
		MapAttr prevType;
		if (index == 0) {
			prevType = getMapSource().getMapAttr();
		} else {
			prevType = legModel.getStage(index - 1).produces();
		}
		
		// get next stage
		Stage nextStage = legModel.getStage(index);
		int   nextStageInputIndex = index == legModel.getInnerStageCount()? legModel.getAggStageInputNumber(): 0;
		
		List<Stage> stages = new ArrayList<Stage>();
		for (Stage stage: possibleStages) {
			if (stage.canTake(0, prevType) && nextStage.canTake(nextStageInputIndex, stage.produces())) {
				stages.add(stage);
			}
		}
		return stages;
	}
	
	/**
	 * Determines whether deleting the stage at the specified position would render
	 * the pipeline unusable. Where broken pipeline is a pipeline in which output
	 * from previous stages cannot be processed by the next stage.
	 * @param index Position of the stage being deleted.
	 * @return <code>true</code> if the deletion will not result in a dysfunctional
	 *        pipeline, <code>false</code> otherwise.
	 */
	private boolean deletePossibleOfPipelineStageAt(int index){
		int nInnerStages = legModel.getInnerStageCount();
		
		if (index < 0 || index >= nInnerStages)
			return false;
		
		if (index == 0){
			if ((index+1) < nInnerStages){
				// There is a Stage following the stage to be deleted
				return legModel.getStage(index+1).canTake(0, getMapSource().getMapAttr());
			}
			else {
				// The input is directly connected to the aggregation stage
				return getAggStage().canTake(legModel.getAggStageInputNumber(), getMapSource().getMapAttr());
			}
		}
		else if (index > 0){
			if ((index+1) < nInnerStages){
				// There is a stage following the stage to be deleted
				Stage prev = legModel.getStage(index-1);
				Stage next = legModel.getStage(index+1);
				return next.canTake(0, prev.produces());
			}
			else {
				// The stage feeding into this stage is connected to the aggregation stage
				Stage prev = legModel.getStage(index-1);
				return getAggStage().canTake(legModel.getAggStageInputNumber(), prev.produces());
			}
		}
		
		return false;
	}
	
	public MapSource getMapSource(){
		return legModel.getMapSource();
	}
	
	public Stage getAggStage(){
		return legModel.getStage(legModel.getInnerStageCount());
	}
	
	private void initialize(){
		
		// Inner panel holds the source panel and stage panels
		innerPanel = new JPanel();
		innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.PAGE_AXIS));
		innerPanel.add(createSourcePanel());
		
		// Insert the stages that we inherited from the pipeline
		int n = legModel.getInnerStageCount();
		for(int i=0; i<n; i++){
			StagePipelinePanel spp = new StagePipelinePanel(legModel.getStage(i));
			innerPanel.add(spp, i+1); // panel is an additional source panel at the top, hence +1
		}
		
		// Outer panel keeps the items in the inner panel to their minimum height.
		setLayout(new BorderLayout()); //BoxLayout(this, BoxLayout.Y_AXIS));
		add(innerPanel, BorderLayout.NORTH);
		
	}
	
	private class ViewCitationAction extends AbstractAction {
		public ViewCitationAction(){
			super("View Info".toUpperCase());
		}
		
		public void actionPerformed(ActionEvent e) {
			JTextArea ta = new JTextArea(mapSource.getAbstract(), 5, 40);
			ta.setEditable(false);
			ta.setLineWrap(true);
			ta.setWrapStyleWord(true);
			
			JScrollPane areaScrollPane = new JScrollPane(ta);
			areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			
			Util.showMessageDialogObj(areaScrollPane,
					"Info for "+mapSource.getTitle(), JOptionPane.INFORMATION_MESSAGE);
			
		}
	}
	
	private JPanel createSourcePanel(){
		
		// create the panel to hold the source label and nudge options
		JPanel sourcePipelinePanel = new JPanel();
		sourcePipelinePanel.setLayout(new BoxLayout(sourcePipelinePanel, BoxLayout.PAGE_AXIS));
		sourcePipelinePanel.setBorder(BorderFactory.createTitledBorder("Source"));	
		
		// create a JPanel to hold the title and expand button
		JPanel sourceTitlePanel = new JPanel();
		sourceTitlePanel.setLayout(new BoxLayout(sourceTitlePanel, BoxLayout.LINE_AXIS));
		sourceTitlePanel.setBorder(new EmptyBorder(5, 10, 10, 10));
		
		// create the source options button and add it to the panel
		JButton insertButton = new JButton(firstInsertButtonLabel);
		insertButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				dialogInsertStageAt(0);
			}
		});

		JButton viewCitationButton = new JButton(new ViewCitationAction());
		sourceTitlePanel.add(viewCitationButton);
		sourceTitlePanel.add(Box.createHorizontalStrut(5));
		
		// add the source label to the panel.
		// append a space to the front of the label to space away from the options button.
		JLabel srcLbl = new JLabel(" " + mapSource.getTitle());
		sourceTitlePanel.add(srcLbl);
		// add a filler to fill in to the right
		sourceTitlePanel.add(Box.createHorizontalGlue());
		
		sourcePipelinePanel.add(sourceTitlePanel);
		
		// check mapsource bounding size to determine if it needs to
		// have the recenter button.  Only add recenter if the source
		// is smaller then 180 x 360
		Rectangle2D mapDim = mapSource.getLatLonBoundingBox();
		if (!(mapDim.getHeight()==180 && mapDim.getWidth()==360)){
			sourcePipelinePanel.add(createRecenterButton());
		}
		
		// add the nudge panel if this mapSource is movable.
		if(mapSource.isMovable()){
			JPanel formatPanel = new JPanel();
			formatPanel.setLayout(new BoxLayout(formatPanel, BoxLayout.LINE_AXIS));
			formatPanel.add(createNudgePanel());
			
			// add some space
			formatPanel.add(Box.createRigidArea(new Dimension(5,0)));
			
			// add the properties panel
			formatPanel.add(createMapPropertiesPanel());
			
			sourcePipelinePanel.add(formatPanel);
		}else{
			// add the properties panel
			sourcePipelinePanel.add(createMapPropertiesPanel());
		}
		
		//Add the "Add Stage" button at the bottom of the panel centered
		insertButton.setAlignmentX(CENTER_ALIGNMENT);
		sourcePipelinePanel.add(insertButton);

		return sourcePipelinePanel;
	}
	
	/**
	 * Creates a button to recenter the JMARS view over this mapSource
	 * @return a JPanel
	 */
	private JPanel createRecenterButton(){
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		buttonPanel.setBorder(BorderFactory.createTitledBorder("View"));
		
		JButton recenterButton = new JButton("Center".toUpperCase());
		recenterButton.addActionListener(new ActionListener() {				
			public void actionPerformed(ActionEvent e) {
				// recenter the location manager (JMARS View) over this map source
				Rectangle2D mapDim = mapSource.getLatLonBoundingBox();
				double centerX = mapDim.getCenterX();
				double centerY = mapDim.getCenterY();
				Main.testDriver.locMgr.setLocation(Main.PO.convSpatialToWorld(360-centerX,centerY),true);
			}
		});
		buttonPanel.add(recenterButton);
		
		// add a small spacer 
		buttonPanel.add(Box.createRigidArea(new Dimension(5,0)));
		
		// add an information label
		JLabel infoLabel = new JLabel("Center the view over this source.");
		buttonPanel.add(infoLabel);
		
		// add glue so horizontal stretch will occur here.
		buttonPanel.add(Box.createHorizontalGlue());
		
		return buttonPanel;
	}
	
	/** Sets the ignoreValue field equal to the ignore value on the mapSource. */
	private void sourceToIgnoreField() {
		double[] ignore = mapSource.getIgnoreValue();
		if (ignore == null) {
			ignoreValueField.setText("");
		} else {
			String[] strings = new String[ignore.length];
			for (int i = 0; i < ignore.length; i++) {
				if (Double.isNaN(ignore[i])) {
					strings[i] = "-";
				} else {
					strings[i] = MessageFormat.format("{0,number,#.##########}", ignore[i]);
				}
			}
			ignoreValueField.setText(Util.join(", ", strings));
		}
	}
	
	private MapSourceListener sourceListener = new MapSourceListener() {
		public void changed(MapSource source) {
			if (source == mapSource) {
				sourceToMaxPPDField();
				sourceToIgnoreField();
				resetOffsetLabels();
			}
		}
	};
	
	private void sourceToMaxPPDField() {
		maxPPDField.setText("" + mapSource.getMaxPPD());
	}
	
	/**
	 * Creates a JPanel that holds map properties options 
	 * @return the JPanel containing the map properties inputs.
	 */
	private JPanel createMapPropertiesPanel(){
		JPanel propertiesPanel = new JPanel();
		
		propertiesPanel.setBorder(BorderFactory.createTitledBorder("Map Properties"));
		//propertiesPanel.setLayout(new BoxLayout(propertiesPanel,BoxLayout.LINE_AXIS));
		propertiesPanel.setLayout(new BorderLayout());
		
		propertiesPanel.addAncestorListener(new AncestorAdapter() {
			public void ancestorAdded(AncestorEvent event) {
				mapSource.addListener(sourceListener);
				sourceListener.changed(mapSource);
			}
			public void ancestorRemoved(AncestorEvent event) {
				mapSource.removeListener(sourceListener);
			}
		});
		
		// define the labels and fields to be used.
		final JLabel maxPPDLabel = new JLabel("Max PPD");
		maxPPDField = new JTextField(""+mapSource.getMaxPPD(), 8);
		maxPPDField.setToolTipText("Set the maxPPD value.  Accepts values " + MapSource.MAXPPD_MIN + " to " + MapSource.MAXPPD_MAX + ".");
		maxPPDField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				updateMaxPPDFromField();
			}
		});
		maxPPDField.addFocusListener(new FocusAdapter(){
			public void focusLost(FocusEvent e) {
				updateMaxPPDFromField();
			}
		});
		
		final JLabel ignoreValueLabel = new JLabel("Ignore Value");
		ignoreValueField = new JTextField(7);
		ignoreValueField.setToolTipText("NODATA pixel value, e.g. '0' for black, '255,255,0' for yellow, or '0,-,-' for red=0.");
		sourceToIgnoreField();
		ignoreValueField.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				updateIgnoreValueFromField();
			}
		});
		ignoreValueField.addFocusListener(new FocusAdapter(){
			public void focusLost(FocusEvent e) {
				updateIgnoreValueFromField();
			}
		});
		
		final int gap = 2;
		JPanel out = new JPanel(new GridBagLayout());
		Insets in = new Insets(gap,gap,gap,gap);
		out.add(maxPPDLabel, new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.LINE_START,GridBagConstraints.HORIZONTAL,in,gap,gap));
		out.add(maxPPDField, new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.LINE_START,GridBagConstraints.HORIZONTAL,in,gap,gap));
		out.add(ignoreValueLabel, new GridBagConstraints(0,1,1,1,0,0,GridBagConstraints.LINE_START,GridBagConstraints.HORIZONTAL,in,gap,gap));
		out.add(ignoreValueField, new GridBagConstraints(1,1,1,1,0,0,GridBagConstraints.LINE_START,GridBagConstraints.HORIZONTAL,in,gap,gap));
		
		propertiesPanel.add(out, BorderLayout.LINE_START);
		
		return propertiesPanel;
	}
	
	/**
	 * Validates and pulls the value from the MaxPPD field then refreshes the map.
	 */
	private void updateMaxPPDFromField(){
		// Set the mapSource maxPPD to what is set in the maxPPD text field
		try {
			double newMaxPPDValue = Double.parseDouble(maxPPDField.getText());
			
			// only refresh if the maxPPD value was changed
			if(newMaxPPDValue!=mapSource.getMaxPPD()) {
				mapSource.setMaxPPD(newMaxPPDValue);
			}
		} catch (Exception paramEx) {
			sourceToMaxPPDField();
			Util.showMessageDialog("MaxPPD must be between " + MapSource.MAXPPD_MIN + " and " + MapSource.MAXPPD_MAX + ".","Input Error",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Validates and pulls the value from the ignore value field then refreshes the map.
	 */
	private void updateIgnoreValueFromField(){
		double[] newIgnoreArray;
		String newValue = ignoreValueField.getText().trim();
		if (newValue.equals("")) {
			// set the ignore value to null
			newIgnoreArray = null;
		} else {
			// This field is setup to support multiple bands separated by spaces or commas.
			String[] newValuesArray = newValue.split("[, ]+");
			int bandCount = newValuesArray.length;
			Integer numColors = mapSource.getMapAttr().getNumColorComp();
			try {
				if (bandCount != 1 && !new Integer(bandCount).equals(numColors)) {
					sourceToIgnoreField();
					throw new IllegalArgumentException("Wrong number of ignore values entered\n\n" +
						"Must enter 1 " + (numColors != null ? "or " + numColors + " values" : "value"));
				} else {
					newIgnoreArray = new double[bandCount];
					for(int i=0; i<bandCount; i++) {
						try {
							newIgnoreArray[i] = Double.parseDouble(newValuesArray[i]);
						} catch (Exception e) {
							newIgnoreArray[i] = Double.NaN;
						}
					}
				}
			} catch (Exception ex) {
				Util.showMessageDialog(ex.getMessage(), "Ignore Value Error", JOptionPane.ERROR_MESSAGE);
				newIgnoreArray = mapSource.getIgnoreValue();
			}
		}
		
		if (!Arrays.equals(mapSource.getIgnoreValue(), newIgnoreArray)) {
			mapSource.setIgnoreValue(newIgnoreArray);
			// redisplay the user-entered values so formatting takes affect
			sourceToIgnoreField();
		}
	}
	
	/**
	 * Creates the nudge panel to be added to the PipelinePanel
	 * @param source the MapSource
	 * @return a JPanel containing the nudge interface.
	 */
	private JPanel createNudgePanel(){

		JPanel nudgeCursorPanel = new JPanel();
		nudgeCursorPanel.setLayout(new GridLayout(3,3));
		nudgeCursorPanel.setMaximumSize(new Dimension(60,60));
		
		// --------------------------------------
		// Step up the step selection box
		// This is used to allow for moving by various pixel increments.
		// --------------------------------------
			
		// create a JPanel to hold the label and the selection box so they can be stacked
		JPanel stepPanel = new JPanel();
		stepPanel.setLayout(new BoxLayout(stepPanel, BoxLayout.PAGE_AXIS));
		stepPanel.setMaximumSize(new Dimension(100,50));
		stepPanel.setPreferredSize(new Dimension(100,50));
			
		// add the step label
		JLabel stepLabel = new JLabel("Step:");
		stepLabel.setAlignmentX(LEFT_ALIGNMENT);
		stepPanel.add(stepLabel);
		
		// define and add the selection box
		final String[] stepOptions = {"1","2","5","10"};
		final JComboBox stepSelect = new JComboBox(stepOptions);		
		stepSelect.setAlignmentX(LEFT_ALIGNMENT);
		stepSelect.setSelectedIndex(0);
		stepPanel.add(stepSelect);
		
		// add glue to the bottom so the selection box won't grow abnormally huge
		stepPanel.add(Box.createVerticalGlue());
		
		// -------------------------------------------
		// Step up the nudge buttons
		// -------------------------------------------
		
		// define the button dimensions
		Dimension buttonDim = new Dimension(20,20);
		Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
		Image pan_w =  ImageFactory.createImage(PAN_W
		         .withDisplayColor(imgColor));         		          				 
		        
		
		Image pan_e =  ImageFactory.createImage(PAN_E
		         .withDisplayColor(imgColor));      		               		          				 
		         
		
		Image pan_n =  ImageFactory.createImage(PAN_N
		         .withDisplayColor(imgColor));     		               		          				 
		         
		
		Image pan_s =  ImageFactory.createImage(PAN_S
		         .withDisplayColor(imgColor));      		               		          				 
		         
		
		Image dot =  ImageFactory.createImage(DOT
		         .withDisplayColor(imgColor));     		               		          				 
		       
		
		JButton left = new JButton(new ImageIcon(pan_w));
		left.setUI(new IconButtonUI());
		left.setPreferredSize(buttonDim);
		
		JButton right = new JButton(new ImageIcon(pan_e));
		right.setPreferredSize(buttonDim);
		right.setUI(new IconButtonUI());
		
		JButton up = new JButton(new ImageIcon(pan_n));
		up.setPreferredSize(buttonDim);
		up.setUI(new IconButtonUI());
		
		JButton down = new JButton(new ImageIcon(pan_s));
		down.setPreferredSize(buttonDim);
		down.setUI(new IconButtonUI());
		
		final JButton stepToggle = new JButton(new ImageIcon(dot));
		stepToggle.setPreferredSize(buttonDim);
		stepToggle.setUI(new IconButtonUI());
		
		// Add an action listener so that clicking the center button will rotate through 
		// the step selection elements.  
		stepToggle.addActionListener(new ActionListener(){			
			public void actionPerformed(ActionEvent e) {
			
				int selected = stepSelect.getSelectedIndex();
				int max = stepOptions.length-1;
			
				// increment the selected item
				int next = selected+1;
				if (next>max) next = 0;
				
				// reset the selected item in the step selection box to the next index.
				stepSelect.setSelectedIndex(next);
			}			
		});
		
		left.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double step = Double.parseDouble((String) stepSelect.getSelectedItem());
				double dx = step / Main.testDriver.mainWindow.getZoomManager().getZoomPPD();
				Point2D p = mapSource.getOffset();
				mapSource.setOffset(new Point2D.Double(p.getX()+dx, p.getY()));
				resetOffsetLabels();
			}
		});
		
		right.addActionListener(new ActionListener() {				
			public void actionPerformed(ActionEvent e) {
				double step = -Double.parseDouble((String) stepSelect.getSelectedItem());
				double dx = step / Main.testDriver.mainWindow.getZoomManager().getZoomPPD();
				Point2D p = mapSource.getOffset();
				mapSource.setOffset(new Point2D.Double(p.getX()+dx, p.getY()));
				resetOffsetLabels();
			}
		});
		
		up.addActionListener(new ActionListener() {				
			public void actionPerformed(ActionEvent e) {
				double step = -Double.parseDouble((String) stepSelect.getSelectedItem());
				double dy = step / Main.testDriver.mainWindow.getZoomManager().getZoomPPD();
				Point2D p = mapSource.getOffset();
				mapSource.setOffset(new Point2D.Double(p.getX(), p.getY()+dy));
				resetOffsetLabels();
			}
		});
		
		down.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double step = Double.parseDouble((String) stepSelect.getSelectedItem());
				double dy = step / Main.testDriver.mainWindow.getZoomManager().getZoomPPD();
				Point2D p = mapSource.getOffset();
				mapSource.setOffset(new Point2D.Double(p.getX(), p.getY()+dy));
				resetOffsetLabels();
			}
		});
		
		nudgeCursorPanel.add(new JLabel());
		nudgeCursorPanel.add(up);
		nudgeCursorPanel.add(new JLabel());
		nudgeCursorPanel.add(left);
		nudgeCursorPanel.add(stepToggle);
		nudgeCursorPanel.add(right);
		nudgeCursorPanel.add(new JLabel());
		nudgeCursorPanel.add(down);
		nudgeCursorPanel.add(new JLabel());
		
		// ---------------------------------------------
		// create a panel to hold the nudge information
		// ---------------------------------------------
		JPanel nudgeInfoPanel = new JPanel();
		nudgeInfoPanel.setLayout(new BoxLayout(nudgeInfoPanel, BoxLayout.PAGE_AXIS));
		
		// set the labels to either initial values.  This is called in a method 
		// because it could be called again during user interaction. 
		initOffsetLabels();
		
		// format and add the X offset label
		offsetXLabel.setAlignmentX(LEFT_ALIGNMENT);
		nudgeInfoPanel.add(offsetXLabel);
		
		// format and add the Y offset label
		offsetXLabel.setAlignmentX(LEFT_ALIGNMENT);
		nudgeInfoPanel.add(offsetYLabel);		
					
		// add the reset button
		JButton reset = new JButton("Reset".toUpperCase());
		reset.addActionListener(new ActionListener(){			
			public void actionPerformed(ActionEvent e) {
				mapSource.setOffset(new Point2D.Double(0,0));
				resetOffsetLabels();
			}			
		});
		reset.setAlignmentX(LEFT_ALIGNMENT);
		nudgeInfoPanel.add(reset);
		
		// ---------------------------------------------------
		// create the master nudge panel and add the elements to it.
		// ---------------------------------------------------
		JPanel nudgePanel = new JPanel();
		nudgePanel.setLayout(new BoxLayout(nudgePanel, BoxLayout.LINE_AXIS));
		nudgePanel.setBorder(BorderFactory.createTitledBorder("Nudge Map"));
		nudgePanel.add(nudgeCursorPanel);
		
		// add a small spacer 
		nudgePanel.add(Box.createRigidArea(new Dimension(10,0)));
		
		nudgePanel.add(stepPanel);
		
		// add a small spacer 
		nudgePanel.add(Box.createRigidArea(new Dimension(10,0)));
		
		nudgePanel.add(nudgeInfoPanel);
		
		//add a little space to the end of the nudge area to fit better with the centered "Add Stage"
		nudgePanel.add(Box.createRigidArea(new Dimension(28,0)));
		
		// add a filler to fill in to the right
		nudgePanel.add(Box.createHorizontalGlue());
		
		return nudgePanel;
	}

	/**
	 * Inserts the given stage at the specified index in the current pipeline.
	 * @param stage Stage to insert.
	 * @param index Index in the pipeline. Note that first stage is at index 0.
	 *        Also note that there are more panels than pipeline stages because
	 *        the source name takes up one panel.
	 */
	public void insertStage(Stage stage, int index){
		StagePipelinePanel spp = new StagePipelinePanel(stage);
		//innerPanel.setAlignmentX(LEFT_ALIGNMENT);
		innerPanel.add(spp, index+1); // panel is an additional source panel at the top, hence +1
	}
	
	public void removeStage(StagePipelinePanel spp){
		innerPanel.remove(spp);
	}
	
	/**
	 * Show dialog to the user to add a pipeline stage at the specified index.
	 * @param stageIndex Pipeline index of the stage at whose location an insert is to be done.
	 */
	public void dialogInsertStageAt(int index){
		StageFactory stageFactory = new StageFactory();		
		List<Stage> allStages = stageFactory.getAllSingleIOStages();
		
		Stage[] filteredStages = (Stage[]) filterForInsertByStagePosition(allStages, index).toArray(new Stage[0]);
		Stage selected = (Stage)Util.showInputDialog("Select Stage", "Select Stage",
				JOptionPane.QUESTION_MESSAGE, null, filteredStages, filteredStages.length > 0? filteredStages[0]: null);
		
		if (selected != null){
			Stage stage = stageFactory.getStageInstance(selected.getStageName());
			if (stage instanceof ColorStretcherStage) {
				//if we are adding a ColorStretcherStage, we need to set the min and max values in the ColorStretcherStageSettings
				//object in order to pass these values down to the ColorScale so that the Fancy Color Mapper min and max
				//values can be set. 
				final ColorStretcherStageSettings csStageSettings = (ColorStretcherStageSettings) stage.getSettings();
				csStageSettings.setMapSource(this.mapSource);
				csStageSettings.setStages(this.legModel.getInnerStages());
				
				//loop through the inner stages of the legModel to get the last GrayScaleStage in the list
				Stage[] tmpStg = legModel.getInnerStages();
				for (Stage myStage : tmpStg) {
					if (myStage instanceof GrayscaleStage) {
						//get the min and max values from the GrayScaleStageSettings object (set by property change listeners on the GrayScaleStageSettings object)
						final GrayscaleStageSettings gss = (GrayscaleStageSettings) myStage.getSettings();
						double min = gss.getMinValue();
						double max = gss.getMaxValue();
						//set those min and max values on our stage settings object
						csStageSettings.setMinValue(min);
						csStageSettings.setMaxValue(max);
						
						//setup a property change listener on the GrayscaleStageSettings object to update the 
						//ColorStageSettings object any time the min and max values changed.
						gss.addPropertyChangeListener(new PropertyChangeListener() {
							
							@Override
							public void propertyChange(PropertyChangeEvent evt) {
								//update the min and max values on ColorStretcherStageSettings with the new 
								//min max values from the updated GrayscaleStageSettings
								csStageSettings.setMinValue(gss.getMinValue());
								csStageSettings.setMaxValue(gss.getMaxValue());	
							}
						});
					}
				}
				//Need to figure out how to set the list of stages on the existing stages in case a stage was added
				//and it is not the latest. Somehow we have to know that threshold was added
				//in the settings for the Color stretcher stage so that we can refresh and see an updated list.
				//A fallback plan is to just make them re add the color stretcher stage, but that is not ideal.
			}
			legModel.insertStage(index, stage);
		}
	}
	
	public void dialogInsertStageAfter(StagePipelinePanel sp){
		int idx = Arrays.asList(innerPanel.getComponents()).indexOf(sp);
		dialogInsertStageAt(idx); // There is an additional Source panel component at the top of innerPanel, hence -1
	}
	
	class StagePipelinePanel extends JPanel {
		private static final long serialVersionUID = 1L;
		
		Stage stage;
		StageView stageView;
		JButton removeButton = new JButton(removeButtonLabel);
		JButton insertButton = new JButton(insertButtonLabel);
		
		public StagePipelinePanel(Stage stage){
			super();
			this.stage = stage;
			this.stageView = this.stage.getSettings().createStageView();
			
			insertButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					dialogInsertStageAfter(StagePipelinePanel.this);
					LManager.repaintAll();
				}
			});
			removeButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e){
					int index = Arrays.asList(innerPanel.getComponents()).indexOf(StagePipelinePanel.this);
					if (deletePossibleOfPipelineStageAt(index-1))
						legModel.removeStage(StagePipelinePanel.this.stage);
					else
						Util.showMessageDialog(
								"Cannot remove stage \""+getStage().getStageName()+"\".",
								"Error!", JOptionPane.ERROR_MESSAGE);
					
					if (StagePipelinePanel.this.stageView instanceof ColorStretcherStageView) {
						//always try to close the legend frame in case the color stretcher was popped out
						((ColorStretcherStageView)StagePipelinePanel.this.stageView).closeLegendFrame();
					}
				}
			});
			
			Dimension buttonSize = new Dimension(42, 30);
			insertButton.setPreferredSize(buttonSize);
			insertButton.setMinimumSize(buttonSize);
			insertButton.setMaximumSize(buttonSize);
			removeButton.setPreferredSize(buttonSize);
			removeButton.setMinimumSize(buttonSize);
			removeButton.setMaximumSize(buttonSize);
					
			JPanel buttonInnerPanel = new JPanel();
			buttonInnerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			buttonInnerPanel.setLayout(new BoxLayout(buttonInnerPanel, BoxLayout.PAGE_AXIS));
			buttonInnerPanel.add(removeButton);
			buttonInnerPanel.add(Box.createVerticalStrut(5));
			buttonInnerPanel.add(insertButton);
			
			JPanel buttonPanel = new JPanel(new BorderLayout());
			buttonPanel.add(buttonInnerPanel,BorderLayout.NORTH);
			setBorder(BorderFactory.createTitledBorder(getStage().getStageName()));
			setLayout(new BorderLayout());
			
			add(buttonPanel,BorderLayout.WEST);
			add(getStageView().getStagePanel(),BorderLayout.CENTER);
		}
		
		public StageView getStageView(){
			return stageView;
		}
		
		public Stage getStage(){
			return stage;
		}
	}
	
	
	//
	// PipelineLegModelListener implementation.
	//

	// Unused.
	public void stageParamsChanged(PipelineLegModelEvent e) {}

	public void stagesAdded(PipelineLegModelEvent e) {
		int[] indices = e.getStageIndices();
		Stage[] stages = e.getStages();
		
		for(int i=0; i<indices.length; i++){
			insertStage(stages[i], indices[i]);
		}
	}

	public void stagesRemoved(PipelineLegModelEvent e) {
		int[] indices = e.getStageIndices();
		for(int i=indices.length-1; i>=0; i--){
			removeStage((StagePipelinePanel)innerPanel.getComponent(indices[i]+1));
		}
	}

	public void stagesReplaced(PipelineLegModelEvent e) {
		while(innerPanel.getComponentCount() > 1){
			removeStage((StagePipelinePanel)innerPanel.getComponent(innerPanel.getComponentCount()-1));
		}
		
		Stage[] stages = e.getStages();
		for(int i=0; i<stages.length; i++){
			insertStage(stages[i], i);
		}
	}
	
	/**
	 * Set the initial value in the nudge offset labels.
	 */
	private void initOffsetLabels(){
		offsetXLabel.setText("X offset: " + Util.formatDouble(0,offsetDecimalDigits));
		offsetYLabel.setText("Y offset: " + Util.formatDouble(0,offsetDecimalDigits));
	}

	/**
	 * Resets the x and y nudge offset labels 
	 */
	private void resetOffsetLabels(){
		Point2D p = mapSource.getOffset();
		double x = p.getX();
		double y = p.getY();
		// reverse the sign to make sense with the display
		x = (x==0 ? 0 : -x);
		y = (y==0 ? 0 : -y);
		offsetXLabel.setText("X offset: " + Util.formatDouble(x,offsetDecimalDigits));
		offsetYLabel.setText("Y offset: " + Util.formatDouble(y,offsetDecimalDigits));
	}
	

	/**
	 * If the map projection is changed then redraw the offset labels.
	 */
	public void projectionChanged(edu.asu.jmars.layer.ProjectionEvent e) {
		initOffsetLabels();
		
		// can't call resetOffsetLabels here because when this method is called
		// the mapSource offset have not been rest yet.  So, if you just call
		// resetOffsetLabels the screen values won't change.  
	}

}
