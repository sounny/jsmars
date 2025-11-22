package edu.asu.jmars.layer.stamp.chart;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.AncestorEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.MultiProjection;
import edu.asu.jmars.layer.stamp.PlotSource;
import edu.asu.jmars.layer.stamp.PlotSourceRenderer;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.swing.AncestorAdapter;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeChart;
import edu.asu.jmars.util.DebugLog;

import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.stable.ColorCellEditor;
import edu.asu.jmars.util.stable.ColorCellRenderer;

public class ChartView extends JPanel implements DataReceiver {
	private static final long serialVersionUID = 1L;

	private static DebugLog log = DebugLog.instance();
	
	/**
	 * Maximum power of 2 that the zoom can take.
	 */
	private static int maxZoomPwr = 14;
	
	// Attached stampLView
	StampLView stampLView;
	
	int ppd;
	ProjObj proj;
	
	PlotSource[] plotSources=null;
	
	// Data samples
	Samples samples = null;
	
	// Profile line, as it exists in the MapLView
	Shape profileLine;
	double profileLineLengthKm;
	
	// Range of linear parameter t (within [0,1]) for which profile line is effectively visible.
	Range effRange = new Range(0.0, 1.0);
	
	// JFreeChart related stuff
	//XYSeriesCollection dataset;
	JFreeChart chart;
	ChartPanel chartPanel;
	
	JMenuItem saveAsTextMenuItem;
	JFileChooser fileChooser;
	JComboBox ppdComboBox;
	DefaultComboBoxModel ppdComboBoxModel;
	
	// Readout table stuff
	JTable readoutTable;
	ReadoutTableModel roTableModel;
	
	public ChartView(StampLView stampLView){
		this.stampLView = stampLView;
		
		ppd = 1;
		
		setLayout(new BorderLayout());
		JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		sp.setDividerLocation(0.8);
		sp.setResizeWeight(0.8);
		sp.setLeftComponent(createChartPanel());
		
		String instrument=stampLView.stampLayer.getInstrument();
		
		plotSources = new PlotSource[1];
		
		plotSources[0]=new PlotSource(stampLView.stampLayer.getParam(stampLView.stampLayer.PLOT_UNITS));		
		
		sp.setRightComponent(createReadoutPanel(plotSources,chart));
		add(sp, BorderLayout.CENTER);		
	}
	
	private JPanel createReadoutPanel(PlotSource[] sources, JFreeChart chart){
		readoutTable = new ReadoutTable(roTableModel = new ReadoutTableModel(sources, chart));
		JPanel roPanel = new JPanel(new BorderLayout());
		JScrollPane sp = new JScrollPane(readoutTable);
		sp.setPreferredSize(new Dimension(300,150));
		roPanel.add(sp, BorderLayout.CENTER);
		return roPanel;
	}
	
	private JPanel createChartPanel(){
		chart = ChartFactory.createXYLineChart(
				"",
				"Distance (Km)",
				"Value",
				new XYSeriesCollection(),
				PlotOrientation.VERTICAL,
				true,
				false,
				false);
		
		configurePlot();
		
		ThemeChart.configureUI(chart);
		
		XYItemRenderer rr = ((XYPlot) chart.getPlot()).getRenderer();
		rr.setSeriesPaint(0, ThemeChart.getPlotColor());
		chart.getXYPlot().setDomainCrosshairVisible(true);		
		chart.getXYPlot().setDomainCrosshairPaint(ThemeChart.getIndicatorColor());
		chart.getXYPlot().setDomainCrosshairStroke(new BasicStroke(1.0f));
		
		chartPanel = new ChartPanel(chart, true);		
		chartPanel.addChartMouseListener(new ChartMouseListener(){
			public void chartMouseClicked(ChartMouseEvent arg0) {}
			public void chartMouseMoved(ChartMouseEvent e) {
				chartMouseMovedEventOccurred(e);
			}
		});
		
		chartPanel.addComponentListener(new ComponentAdapter(){
			public void componentResized(ComponentEvent e) {
				updateMaxAndSelectedPpd();
			}
		});
		
		chartPanel.addAncestorListener(new AncestorAdapter(){
			@Override
			public void ancestorAdded(AncestorEvent e) {
				updateMaxAndSelectedPpd();
			}
		});
		
		fileChooser = createSaveAsTextFileChooser();
		saveAsTextMenuItem = createSaveAsTextMenuItem();
		
		JPopupMenu popupMenu = chartPanel.getPopupMenu();
		popupMenu.add(new JPopupMenu.Separator());
		popupMenu.add(saveAsTextMenuItem);
		chartPanel.setPopupMenu(popupMenu);
		
		chart.getXYPlot().getDomainAxis().addChangeListener(new AxisChangeListener(){
			public void axisChanged(AxisChangeEvent event) {
				domainAxisChangeEvent(event);
			}
		});
		
		//chartPanel.setHorizontalAxisTrace(true);
		
		JPanel ppdPanel = createPPDPanel();
		
		JPanel p = new JPanel(new BorderLayout());
		// PPD box doesn't actually work, so hide it for now.
		// Hopefully this will be fixed when Chart functionality is cleaned up
		// for general JMARS use
//		p.add(ppdPanel, BorderLayout.NORTH);
		p.add(new JPanel(), BorderLayout.NORTH);
		p.add(chartPanel, BorderLayout.CENTER);
		
		return p;
	}
	
	private  void updateMaxAndSelectedPpd(){
		if (profileLine != null)
			if (updateMaxPpd())
				ppdComboBox.setSelectedIndex(ppdComboBox.getItemCount()-1);
	}
	
	private boolean updateMaxPpd(){
		return updateMaxPpd(-1);
	}
	
	private boolean updateMaxPpd(int minPpdIndex){
		// Get new angular span in degrees, determine max-ppd and update ppd combo-box.
		Shape effLine = getEffProfileLineSpan();
		double aSpan = perimeterLength(effLine, stampLView.getProj())[0];
		double pixSpan = chartPanel.getScreenDataArea().getWidth();
		int maxPpdIndex = aSpan > 0.0 && pixSpan > 0? (int)Math.ceil(Math.log(pixSpan / aSpan)/Math.log(2.0)): 0;
		maxPpdIndex = Math.max(0, Math.max(maxPpdIndex, minPpdIndex));
		log.println("aSpan:"+aSpan+"  pixSpan:"+pixSpan+"  maxPpdIndex:"+maxPpdIndex);
		return updatePpdComboBox(maxPpdIndex);
	}
	
	private  void domainAxisChangeEvent(AxisChangeEvent event){
		
		if (profileLine != null){
			/*
			 * Outline:
			 * 1. Determine the Km range displayed by the chart.
			 * 2. Determine the span of profile-line in terms of the linear parameter t. 
			 * 3. Calculate the angular distance covered by the span.
			 * 4. Determine the pixel width of the plot area.
			 * 5. Compute the maximum PPD value based on 3 & 4.
			 * 6. Update the PPD combo-box based on maximum PPD computed in 5.
			 *    6.1. If PPD is smaller than max combo-box PPD, reset view to smaller PPD.
			 *    6.2. Add/remove combo-box as required.
			 * 7. If zooming out, check to see if the newly exposed area will be outside
			 *    currently cached range. If so, issue a fetch request.
			 */
			
			// Get the exposed Km range on the x-axis.
			Range kmRange = chart.getXYPlot().getDomainAxis().getRange();
			effRange = new Range(Math.max(0.0, kmRange.getLowerBound() / profileLineLengthKm),
					Math.min(1.0, kmRange.getUpperBound() / profileLineLengthKm));
			
			log.println("axisChanged: "+kmRange+"  t:"+effRange);
			
			// Get new angular span in degrees, determine max-ppd and update ppd combo-box.
			boolean fetch = updateMaxPpd();
			
			Samples samples = getSamples();
			if (samples == null || samples != null && (effRange.getLowerBound() < samples.t0 || effRange.getUpperBound() > samples.t1))
				fetch = true;
			
			if (fetch)
				setViewExtent(getEffProfileLineSpan(), ((Integer)ppdComboBox.getSelectedItem()).intValue());
		}
	}
	
	private  Shape getProfileLineSpan(Range span){
		if (profileLine == null)
			return null;
		
		Shape reqLine = spanSelect(profileLine, span.getLowerBound(), span.getUpperBound());
		
		//Line2D reqLine = new Line2D.Double(
		//		mapLView.interpolate(profileLine, span.getLowerBound()),
		//		mapLView.interpolate(profileLine, span.getUpperBound()));
		return reqLine;
	}
	
	private  Shape getEffProfileLineSpan(){
		return getProfileLineSpan(effRange);
	}
	
	private boolean updatePpdComboBox(int maxPpdIndex){
		boolean changed = false;
		int currMaxPpdIndex = ppdComboBoxModel.getSize()-1;
		int currPpdIndex = ppdComboBoxModel.getIndexOf(ppdComboBoxModel.getSelectedItem());
		if (maxPpdIndex > currMaxPpdIndex){
			// Add new zoom levels to the ppd combo box
			log.println("Adding zoom indices ["+currMaxPpdIndex+".."+maxPpdIndex+"]");
			Integer[] newZooms = getZoomValues(new int[]{currMaxPpdIndex+1, maxPpdIndex});
			for(int i=0; i<newZooms.length; i++)
				ppdComboBoxModel.addElement(newZooms[i]);
		}
		else if (maxPpdIndex < currMaxPpdIndex){
			if (maxPpdIndex < currPpdIndex){
				// Set new ppd value from the current maxPpdIndex
				log.println("Setting current ppd due to change in max-ppd index to "+maxPpdIndex);
				ppdComboBoxModel.setSelectedItem(ppdComboBoxModel.getElementAt(maxPpdIndex));
				changed = true;
			}
			
			// Remove zoom levels from the ppd combo box
			log.println("Removing ppd indices ["+(maxPpdIndex+1)+".."+currMaxPpdIndex+"]");
			while((ppdComboBoxModel.getSize()-1) > maxPpdIndex)
				ppdComboBoxModel.removeElementAt(ppdComboBoxModel.getSize()-1);
		}
		
		return changed;
	}
	
	private Integer[] getZoomValues(int[] zoomPwrRange){
		if (zoomPwrRange[0] < 0 || zoomPwrRange[1] < 0 || zoomPwrRange[1] < zoomPwrRange[0])
			throw new IllegalArgumentException("Illegal zoom power range ["+zoomPwrRange[0]+".."+zoomPwrRange[1]+"]");
		
		Integer[] zooms = new Integer[zoomPwrRange[1]-zoomPwrRange[0]+1]; 
		for(int i=0; i<zooms.length; i++)
			zooms[i] = new Integer(1<<(zoomPwrRange[0]+i));
		
		return zooms;
	}
	
	private JPanel createPPDPanel(){
		JPanel ppdPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		
		ppdPanel.add(new JLabel("Chart PPD:"));
		
		ppdComboBoxModel = new DefaultComboBoxModel(getZoomValues(new int[]{0, maxZoomPwr}));
		ppdComboBox = new JComboBox(ppdComboBoxModel);
		ppdComboBox.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				ppdComboBoxActionPerformed(e);
			}
		});
		ppdPanel.add(ppdComboBox);
		
		return ppdPanel;
	}
	
	private  void ppdComboBoxActionPerformed(ActionEvent e){
		try {
			int newppd = ((Integer)ppdComboBox.getSelectedItem()).intValue();
			
			/*
			 * If the user has selected a new ppd value, submit a fetch request
			 * for the visible span of profile line.
			 */
			if (newppd != ppd)
				setViewExtent(getEffProfileLineSpan(), newppd);
		}
		catch(NumberFormatException ex){
			log.aprintln("Exception while getting new zoom level: "+ex);
		}
	}
	
	private JFileChooser createSaveAsTextFileChooser(){
		JFileChooser fileChooser = new JFileChooser(Util.getDefaultFCLocation());
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				String fileName = f.getName();
				int indexOfDot = fileName.lastIndexOf('.');
				if (indexOfDot > -1 && fileName.substring(indexOfDot).equalsIgnoreCase(".txt"))
					return true;
				return false;
			}

			public String getDescription() {
				return "Text Files";
			}
		});
		
		return fileChooser;
	}
	
	private JMenuItem createSaveAsTextMenuItem(){
		JMenuItem saveAsTextMenuItem = new JMenuItem("Save as CSV");
		saveAsTextMenuItem.setEnabled(false);
		saveAsTextMenuItem.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				saveAsTextActionPerformed(e);
			}
		});
		return saveAsTextMenuItem;
	}

	private void saveAsTextActionPerformed(ActionEvent e){
		int rc = fileChooser.showSaveDialog(this);
		if (rc != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null)
			return;

		try {
			saveAsText(fileChooser.getSelectedFile());
		}
		catch(Exception ex){
			Util.showMessageDialog("Unable to save file: "+ex.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private  Samples getSamples(){
		return samples;
	}
	
	private void chartMouseMovedEventOccurred(ChartMouseEvent e){
		Point2D pt = chartPanel.translateScreenToJava2D(e.getTrigger().getPoint());
		double km = chart.getXYPlot().getDomainAxis().java2DToValue(
				pt.getX(),
				chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea(),
				chart.getXYPlot().getDomainAxisEdge());

		Point2D cuePoint = null;
		double sampleData = Double.NaN;
		Samples samples = getSamples();
		if (samples != null){
			cuePoint = samples.getPointAtDist(km);
			//log.aprintln("km:"+km+"  -> cuePoint:"+cuePoint+"  -> km:"+samples.getDistance(cuePoint));
			sampleData = samples.getSampleData(km);
		}
		
		if(chart.getXYPlot().getDomainAxis().getRange().contains(km)){
			setCrosshair(km);
			updateReadouts(sampleData, km);
		}
		
		// Forward the event to mapView so that it can show a cueing line as well.
		stampLView.cueChanged(cuePoint);
	}
	
	/**
	 * Receive cueChanged events from mapLView.
	 * @param worldCuePoint The new point within the profileLine segment boundaries
	 *        where the cue is to be generated.
	 */
	public void cueChanged(Point2D worldCuePoint){
		Samples samples = getSamples();
		double km = Double.NaN;
		double sampleData=Double.NaN;

		if (samples != null && worldCuePoint != null){
			km = samples.getDistance(worldCuePoint);
			//log.aprintln("worldCuePoint:"+worldCuePoint+"  -> km:"+km+"  -> worldPt:"+samples.getPointAtDist(km));
			// Previously, for the stamp layer, this interactively retrieved the value for the exact cuePoint.
			// Instead, we will now simply snap to the closest value previously retrieved.			
			sampleData = samples.getSampleData(km);
		}
		if(chart.getXYPlot().getDomainAxis().getRange().contains(km)){
			setCrosshair(km);
			updateReadouts(sampleData, km);
		}
	}
		
	public void setCrosshair(double km){
		chart.getXYPlot().setDomainCrosshairValue(km);
	}
	
	public void updateReadouts(double sampleData, double xValue){
		roTableModel.setSampleData(sampleData, xValue);
	}
	
	private void configurePlot() {
		XYPlot plot = chart.getXYPlot();
		
		plot.setDataset(new XYSeriesCollection());
		NumberAxis numberaxis = new NumberAxis("Value");
		ThemeChart.applyThemeToAxis(numberaxis);		
		plot.setRangeAxis(0, numberaxis);			

		int i;
		for(i=0; plotSources != null && i<plotSources.length; i++){
			
			final String label = plotSources[i].getPlotTitle();
			
			XYSeries s = new XYSeries(label);
			XYDataset newDataset = new XYSeriesCollection(s);
			NumberAxis newAxis = new NonStickyZeroNumberAxis(label);
			ThemeChart.applyThemeToAxis(newAxis);
			
			plot.setDataset(i, newDataset);
			plot.setRangeAxis(i, newAxis);
			plot.mapDatasetToRangeAxis(i, i);

			if (plot.getRenderer(i) == null){
				XYItemRenderer r = new DefaultXYItemRenderer();
				r.setShape(new GeneralPath());
				plot.setRenderer(i, r);
			}
		}
		for(int j=plot.getDatasetCount()-1; j>=Math.max(i,1); j--){
			plot.setDataset(j, null);
			plot.setRangeAxis(j, null);
		}
	}
	
	private void resetPlot(Shape profileLine){
		configurePlot();
		chart.getXYPlot().getDomainAxis().setAutoRange(true);
		chart.getXYPlot().configureDomainAxes();
		chart.getXYPlot().configureRangeAxes();
		
		// The effRange has to be set here, since configureDomainAxes() generates an AxisChangedEvent
		// which modifies the effRange. Doing this outside, i.e. in setProfileLine does not work.
		effRange = new Range(0.0, 1.0);
		
		//chart.getXYPlot().getDomainAxis().setDefaultAutoRange(
		//	new Range(0, Util.angularAndLinearDistanceW(profileLine.getP1(), profileLine.getP2(), mapLView.getProj())[1]));
	}
	
	private void populatePlot(Samples samples){
		
		// Build XYSeries for data from each of the mapData channels.
		double[] data = samples.getSampleData();
		double[] distances = samples.getDistances();
		for(int k=0; k<plotSources.length; k++){
			final String label = plotSources[k].getPlotTitle();
			
			/*
			 * Create a new series of XY-data, with distances as the X-values and
			 * samples as Y-values.
			 * Add the start and end value of distance (i.e. domain) with null range
			 * value to the series. This enables the chart to zoom in and out properly.
			 * We need this because every time a zoom happens, we re-fetch and re-populate
			 * the data in the zoomed range at the new chart PPD.
			 */
			final XYSeries s = new XYSeries(label);
			s.add(new Double(0), null);
			for(int i=0; i<distances.length; i++)
				s.add(new Double(distances[i]), data == null || data.length <= k? null: new Double(data[i]));
			s.add(new Double(samples.lsegLengthKm), null);

			final int axisNumber = k;
			XYDataset newDataset = new XYSeriesCollection(s);
			//chart.getXYPlot().setRangeAxis(axisNumber, new NonStickyZeroNumberAxis(s.getKey().toString()));
			chart.getXYPlot().setDataset(axisNumber, newDataset);
		}
	}
	
	class NonStickyZeroNumberAxis extends NumberAxis {
		public NonStickyZeroNumberAxis(){
			super();
			setAutoRangeIncludesZero(false);
			setAutoRangeStickyZero(false);
		}
		public NonStickyZeroNumberAxis(String label){
			super(label);
			setAutoRangeIncludesZero(false);
			setAutoRangeStickyZero(false);
		}
		
		public void setAutoRangeIncludesZero(boolean flag){
			super.setAutoRangeIncludesZero(flag);
		}
	}
	
	/**
	 * Set the new profile line, clear the current plot and issue a
	 * request to get data corresponding to the new profile line.
	 * @param newProfileLine Profile line in world coordinates.
	 *        It can be null.
	 */
	public  void setProfileLine(Shape newProfileLine, int newppd){
		// Set new profile line
		profileLine = newProfileLine;
		if (profileLine != null)
			profileLineLengthKm = perimeterLength(profileLine, stampLView.getProj())[1];
		else
			profileLineLengthKm = 0.0;

		samples = null;
		
		// Reset chart ranges and data.
		resetPlot(profileLine);

		if (profileLine != null){
			int idx = (int)(Math.log(newppd)/Math.log(2.0));
			updateMaxPpd(idx);
			ppdComboBoxModel.setSelectedItem(ppdComboBoxModel.getElementAt(idx));
		}
		
		setViewExtent(profileLine, newppd);
	}
	
	/**
	 * Sets the view extent to the specified extent. The specified extent is
	 * converted into effective extent by narrowing it to the profile line's extent.
	 * Appropriate diagonal from this extent is the output of the profile line.
	 */
	private  void setViewExtent(Shape newViewExtent, int newppd){
		// disable data save menu options in the chart panel
		saveAsTextMenuItem.setEnabled(false);
		
		roTableModel.fireTableDataChanged();
		
		ppd = newppd;
		proj = Main.PO;
		if (newViewExtent != null) {
//			stampLView.getLayer().monitoredSetStatus(this, Util.darkRed);
			
			Rectangle2D requestedExtent = expandByXPixelsEachSide(newViewExtent.getBounds2D(), ppd, 0.5);
			double x1 = Math.floor(requestedExtent.getMinX() * ppd) / ppd;
			double x2 = Math.ceil(requestedExtent.getMaxX() * ppd) / ppd;
			double y1 = Math.floor(requestedExtent.getMinY() * ppd) / ppd;
			double y2 = Math.ceil(requestedExtent.getMaxY() * ppd) / ppd;
			requestedExtent = new Rectangle2D.Double(x1,y1,Math.max(1.0/ppd,x2-x1),Math.max(1.0/ppd,y2-y1));
			log.println("Requesting viewExtent:"+requestedExtent+" at "+ppd+" ppd.");
		}
	}
		
	private Rectangle2D expandByXPixelsEachSide(Rectangle2D in, int ppd, double xPixels){
		Rectangle2D.Double out = new Rectangle2D.Double();
		out.setFrame(in);
		
		double hdpp = xPixels * (1.0/ppd);
		out.setFrame(out.getX() - hdpp, out.getY() - hdpp, out.getWidth() + 2*hdpp, out.getHeight() + 2*hdpp);
		
		return out;
	}
	
	/**
	 * Receives data from the MapChannels subscribed to by this chart.
	 * @param mapData Data from one of the channels.
	 */
	public  void mapChanged(){
		if (profileLine == null){
			log.println("Either mapData was null or the profileLine was null.");
			return;
		}
				
		// Sample profile line data from the returned raster
		samples = new Samples(profileLine, effRange.getLowerBound(), effRange.getUpperBound());

		if (plotSources != null && samples.getNumBands() != plotSources.length){
			samples = null;

			return;
		}
		populatePlot(samples);
		
		// Enable chart panel's save buttons
		saveAsTextMenuItem.setEnabled(true);
	}
	
	/** Does nothing */
	public void receiveData(Object data) {}
		
	public  void saveAsText(File outputFile) throws FileNotFoundException {
		final String nullString = "N/A";
		
		log.println("Save as text requested to file "+outputFile.getName());
		String delim = ",";

		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(false);
		nf.setMinimumFractionDigits(0);
		nf.setMinimumIntegerDigits(1);
		
		// Output header
		PrintStream ps = new PrintStream(new FileOutputStream(outputFile));
		ps.print("Distance (Km)"+delim+"Longitude"+delim+"Latitude");
		for(int i=0; i<plotSources.length; i++)
			ps.print(delim+plotSources[i].getPlotTitle());
		ps.println();
		
		// Output data
		double[] distances = samples.getDistances();
		Point2D[] samplePtsW = samples.getSamplePoints();
		double[] sampleData = samples.getSampleData();
		for(int k=0; k<samples.getNumSamples(); k++){
			ps.print(nf.format(distances[k]));
			if (samplePtsW[k] != null){
				Point2D ptS = stampLView.getProj().world.toSpatial(samplePtsW[k]);
				ps.print(delim); ps.print(nf.format(ptS.getX()));
				ps.print(delim); ps.print(nf.format(ptS.getY()));
			}
			else {
				ps.print(delim); ps.print(nullString);
				ps.print(delim); ps.print(nullString);
			}
			for(int i=0; i<plotSources.length; i++){
				ps.print(delim);
				ps.print(sampleData == null? nullString: nf.format(sampleData[k]));
			}
			ps.println();
		}
	}
	
	
	/**
	 * A table containing the readout values from the chart.
	 * There is one row per data item in the plot. The table allows
	 * editing of the plot color.
	 */
	class ReadoutTable extends JTable {
		private static final long serialVersionUID = 1L;

		public ReadoutTable(ReadoutTableModel model){
			super(model);
			boolean isColorCellEditable = true;
			setDefaultRenderer(Color.class, new ColorCellRenderer(isColorCellEditable));
			setDefaultEditor(Color.class, new ColorCellEditor());
			setDefaultRenderer(Number.class, new NumberRenderer());
			
			setDefaultRenderer(PlotSource.class, new PlotSourceRenderer());
//			setDefaultEditor(PlotSource.class, new PlotSourceEditor());
		}
	}
	
	class NumberRenderer extends DefaultTableCellRenderer {
		NumberFormat nf = NumberFormat.getNumberInstance();
		
	    public NumberRenderer() { 
	    	super();
	    	nf.setMaximumFractionDigits(8);
	    }

	    public void setValue(Object value) {
	    	/*
	    	 * The data from ReadoutTableModel for which NumberRenderer is used in conjunction with
	    	 * contains NaNs for the MapSources for which we haven't received any data as yet.
	    	 */
	        setText((value == null) ? "" : (Double.isNaN(((Number)value).doubleValue())? "Value Unavailable": nf.format(value)));
	    }  
	}
	
	/**
	 * Table model supporting the readout table.
	 */
	class ReadoutTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		
		private final String TITLE_COL = "Title";
		private final String COLOR_COL = "Color";
		private final String VAL_COL = "Value";
		private final String KM_COL = "Distance";
		
		public final String[] columns = new String[] {
			TITLE_COL,
			COLOR_COL,
			VAL_COL,
			KM_COL
		};
		
		private PlotSource[] sources = null;
		//private Point2D samplePt = null;
		private double[] sampleData = new double[1];
		private double xValue = Double.NaN;
		private JFreeChart chart = null;
		private DecimalFormat df = new DecimalFormat("#,###,##0.00");
		
		public ReadoutTableModel(PlotSource sources[], JFreeChart chart){
			this.sources = sources;
			this.chart = chart;
		}
		
		public String getColumnName(int columnIndex){
			return columns[columnIndex];
		}
		
		public Class<?> getColumnClass(int columnIndex){
			switch(getColumnName(columnIndex)){
				case TITLE_COL: return PlotSource.class;
				case COLOR_COL: return Color.class;
				case VAL_COL: return Number.class;
				case KM_COL: return String.class;
			}
			
			return Object.class;
		}
		
		public void setTitles(PlotSource[] sources){
			this.sources = sources;
			fireTableDataChanged();
		}
		
		public int getColumnCount() {
			return columns.length;
		}

		public int getRowCount() {
			return sources == null? 0: sources.length;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			if (sources == null || sources.length == 0)
				return null;

			if (columnIndex == 0)
				return sources[rowIndex];

			int datasetIndex = rowIndex;
			int seriesIndex = 0;

			// This should be thread-safe since all updates to the chart occur on the AWT thread.
			XYPlot plot = chart.getXYPlot();
			if (datasetIndex < plot.getDatasetCount() && seriesIndex < plot.getDataset(datasetIndex).getSeriesCount()){
				//color
				if (getColumnName(columnIndex).equals(COLOR_COL)){
					Paint p = ((XYLineAndShapeRenderer)plot.getRenderer(datasetIndex)).getSeriesPaint(seriesIndex);
					if (p instanceof Color)
						return (Color)p;
					return null;
				}
				//value
				else if (getColumnName(columnIndex).equals(VAL_COL)){
					if (sampleData != null && sampleData.length > rowIndex)
						return new Double(sampleData[rowIndex]);
				}
				//km
				else if (getColumnName(columnIndex).equals(KM_COL)){
				
					String unit = "km";
					if(xValue < 1){
						xValue = xValue*1000;
						unit= "m";
					}
				
					return df.format(xValue)+" "+unit;
				}
			}
			return null;
		}
		
		public boolean isCellEditable(int rowIndex, int colIndex){
			return (getColumnName(colIndex).equals(COLOR_COL));
		}
		
		public void setValueAt(Object value, int rowIndex, int columnIndex){
			if (!getColumnName(columnIndex).equals(COLOR_COL))
				throw new IllegalArgumentException("Columns other than the color column are uneditable.");
			
			int datasetIndex = rowIndex;
			int seriesIndex = 0;
			
			XYPlot plot = chart.getXYPlot();
			if (datasetIndex < plot.getDatasetCount() && seriesIndex < plot.getDataset(datasetIndex).getSeriesCount()){
				((XYLineAndShapeRenderer)chart.getXYPlot().getRenderer(datasetIndex)).setSeriesPaint(seriesIndex, (Color)value);
			}
		}
		
		public void setSampleData(double newSampleData, double xValue){
			sampleData[0] = newSampleData;
			this.xValue = xValue;
			//fireTableChanged(new TableModelEvent(this, 0, getRowCount(), 2, TableModelEvent.UPDATE));
			fireTableDataChanged();
		}
	}
	
	/**
	 * Storage for sample data as per a profile line segment.
	 * 
	 * @author saadat
	 */
	class Samples {
		final Shape lseg;             // Line-segment in world coordinates
		final double t0, t1;           // [0,1] means the entire lseg, [0.5,1] means mid of lseg to end
		final double lsegLength;       // Length of line segment in degrees (of world coordinates)
		final double lsegLengthKm;     // Length of line segment in Km
		final int nSamples;            // Number of samples (or pixels)
		final int ppd;                 // Requested pixel-per-degree of data
		final int nBands;              // Number of bands per sample (or per pixel)
		
		/** First point in lseg. */
		Point2D pt0;
		
		/**
		 * World coordinates of each of the sampled location along the lseg.
		 */
		Point2D[]  pts;
		
		/**
		 * Sampled data as array of doubles for each sampled location.
		 */
		double[] data;
		
		/**
		 * Linear distance in Km from the start of lseg.
		 */
		double[]   dist;
		
		/** t-parameter locations at which the data has been sampled. */
		double[]   tVals;
		
		/**
		 * Constructs a Samples object which holds sample data for the specified
		 * line segment as extracted from the input mapData object. Consecutive
		 * samples are spaced at 1/ppd.
		 * 
		 * @param mapData Source to use for sampling the data.
		 * @param lseg Line segment along which sampling is to be done.
		 * @param ppd Spacing between consecutive samples.
		 */
		public Samples(Shape lseg, double t0, double t1){
			this.lseg = lseg;
			this.t0 = t0;
			this.t1 = t1;
			this.ppd = stampLView.viewman.getZoomManager().getZoomPPD();
			double dists[] = perimeterLength(lseg, stampLView.getProj()); 
			lsegLength = dists[0];
			lsegLengthKm = dists[1];
			nSamples = (int)(ppd * lsegLength * (t1-t0));
			nBands = 1;
			
			pt0 = ChartView.getFirstPoint(lseg);
			
			pts = new Point2D[nSamples];
			data = new double[nSamples];
			dist = new double[nSamples];
			tVals = new double[nSamples];
			
			sampleData();
		}
		
		public AffineTransform getExtentTransform(int width, int height, final Rectangle2D extent){
			AffineTransform at = new AffineTransform();
			
			at.scale(1, -1);
			at.translate(0, -height);
			at.scale(width/extent.getWidth(), height/extent.getHeight());
			at.translate(-extent.getX(),-extent.getY());
			
			return at;
		}
		
		private void sampleData() {
			for(int i=0; i<nSamples; i++){ // Loops over the left edge
				double t = t0+((double)i)/nSamples; // Not quite correct, t never equals 1
				tVals[i] = t;
				pts[i] = interpolate(lseg, t, stampLView.getProj());
				dist[i] = distanceTo(lseg, pts[i])[1];

				try {
					data[i]=stampLView.getValueAtPoint(pts[i]);						
				} catch (Exception e) {
					e.printStackTrace();
				}
			}					
		}
		
		/**
		 * Return the point (in world coordinates) that falls at the
		 * specified distance (in Km) starting from the first point of
		 * the profile-line.
		 * @param km Perimeter distance (in Km) from the first point of
		 *     the profile-line. 
		 * @return <code>null</code> if there are less than two samples. 
		 *     Otherwise, return the point as a linear interpolation of
		 *     the bounding points (based on distance in km).
		 */
		public Point2D getPointAtDist(double km){
			int idx = Arrays.binarySearch(dist, km);
			double t;
			if (idx < 0){
				idx = -(idx+1);
				double t0, t1, d0, d1;
				if (idx > 0 && idx <nSamples){
					d0 = dist[idx-1]; d1 = dist[idx];
					t0 = tVals[idx-1]; t1 = tVals[idx];
				}
				else if (nSamples > 1){
					if (idx == 0){
						d0 = dist[0]; d1 = dist[1];
						t0 = tVals[0]; t1 = tVals[1];
					}
					else { //if (idx >= nSamples)
						d0 = dist[dist.length-2]; d1 = dist[dist.length-1];
						t0 = tVals[dist.length-2]; t1 = tVals[dist.length-1];
					}
				}
				else {
					return null;
				}
				double segLength = d1-d0;
				double tt = (km-d0)/segLength;
				t = (t0*(1-tt)+t1*tt);
			}
			else {
				t = tVals[idx];
			}
			Point2D p = interpolate(lseg, t, stampLView.getProj());
			return p;
		}
		
		public int getNumBands(){
			return nBands;
		}
		
		public int getNumSamples(){
			return nSamples;
		}
		
		public double[] getSampleData(){
			return (double[])data.clone();
		}
				
		public double getSampleData(double km){
			int idx = getDistanceIndex(km);
			if (idx >=0 && idx < dist.length)
				return data[idx];
			return Double.NaN;
		}
		
		/**
		 * 
		 * @param worldPt
		 * @return Km distance from the starting point of the profile-line.
		 */
		public double getDistance(Point2D worldPt){
			double t = uninterpolate(lseg, worldPt, null, stampLView.getProj());
			int idx = Arrays.binarySearch(tVals, t);
			
			if (idx < 0){
				idx = -(idx + 1);
				if (idx > 0 && idx < nSamples){
					double tt = (t-tVals[idx-1])/(tVals[idx]-tVals[idx-1]);
					return dist[idx-1]*(1-tt)+dist[idx]*tt;
				}
			}
			else {
				return dist[idx];
			}
			return Double.NaN;
		}
		
		public double[] getDistances(){
			return (double[])dist.clone();
		}
		
		public Point2D[] getSamplePoints(){
			return (Point2D[])pts.clone();
		}
		
		/**
		 * Returns the sample index of the specified distance value (in Km).
		 * @param km Input distance from the start of the profile-line.
		 * @return <code>-1</code> if the km value is less than zero and
		 * <code>nSamples</code> if the km value is greater than the total
		 * km-length of the profile line. Otherwise returns an index based
		 * on the interpolated <code>t</code>-value based on the index.
		 */
		public int getDistanceIndex(double km){
			
			int idx = Arrays.binarySearch(dist, km);
			
			if (idx < 0){
				idx = -(idx + 1);
				if (idx > 0 && idx < nSamples){
					double tt = (km-dist[idx-1])/(dist[idx]-dist[idx-1]);
					return tt < 0.5? idx-1: idx;
				}
				else if (idx == 0){
					return -1;
				}
				else {
					return nSamples;
				}
			}
			else {
				return idx;
			}
		}
	}
	
	public static Point2D getFirstPoint(Shape s){
		PathIterator pi = s.getPathIterator(null, 0);
		if (pi.isDone())
			return null;
		
		double coords[] = new double[6];
		pi.currentSegment(coords);
		return new Point2D.Double(coords[0], coords[1]);
	}

	public static Point2D getLastPoint(Shape s){
		PathIterator pi = s.getPathIterator(null, 0);
		if (pi.isDone())
			return null;
		
		double coords[] = new double[6];
		while(!pi.isDone()){
			pi.currentSegment(coords);
			pi.next();
		}
		return new Point2D.Double(coords[0], coords[1]);
	}

	/**
	 * Linearly interpolates a point given the shape (in world coordinates)
	 * and the parameter <code>t</code>.
	 * @param shape Line-string in world coordinates.
	 * @param t Interpolation parameter <code>t</code>.
	 * @return A point obtained by linear-interpolation using the points
	 *     in the shape, given the parameter <code>t</code>.
	 */
	public static Point2D interpolate(Shape shape, double t, MultiProjection proj){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double cartDist = 0;
		int currSeg = PathIterator.SEG_MOVETO;
		double totalLength = perimeterLength(shape, proj)[2];
		
		while(!pi.isDone()){
			switch(currSeg = pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
				first.x = lseg.x1 = lseg.x2 = coords[0];
				first.y = lseg.y1 = lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = coords[0]; lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = first.x; lseg.y2 = first.y;
				break;
			}
			
			double segLength = lseg.getP2().distance(lseg.getP1());
			if (currSeg != PathIterator.SEG_MOVETO && ((cartDist + segLength)/totalLength) >= t){
				return Util.interpolate(lseg, (t*totalLength-cartDist)/segLength);
			}
			
			cartDist += segLength;
			pi.next();
		}
		
		return Util.interpolate(lseg, (t*totalLength-cartDist)/(lseg.getP2().distance(lseg.getP1())));
	}

	/**
	 * Computes the perimeter length of the given shape (in world coordinates). The
	 * length is computed using the 
	 * {@link Util#angularAndLinearDistanceW(Point2D, Point2D, edu.asu.jmars.layer.MultiProjection)}
	 * method.
	 * @param shape Shape in world coordinates.
	 * @return Length of perimeter in degrees, kilometers and cartesian-distance.
	 */
	public static double[] perimeterLength(Shape shape, MultiProjection proj){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double angularDist = 0;
		double linearDist = 0;
		double cartDist = 0;
		
		while(!pi.isDone()){
			switch(pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
				first.x = lseg.x1 = lseg.x2 = coords[0];
				first.y = lseg.y1 = lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = coords[0]; lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = first.x; lseg.y2 = first.y;
				break;
			}
			
			double dists[] = Util.angularAndLinearDistanceW(lseg.getP1(), lseg.getP2(), proj);
			angularDist += dists[0];
			linearDist += dists[1];
			cartDist += lseg.getP2().distance(lseg.getP1());
			pi.next();
		}
		
		return new double[]{ angularDist, linearDist, cartDist };
	}

	/**
	 * Returns the shape formed by sub-selection based on the
	 * parameter range of <code>t</code>, where <code>t</code> ranges
	 * between <code>0</code> and <code>1</code> based on the Cartesian
	 * distance from the first point in the shape.
	 * @param shape Line-string.
	 * @param t0 Starting value of <code>t</code>
	 * @param t1 Ending value of <code>t</code>
	 * @return A sub-selected shape.
	 */
	public Shape spanSelect(Shape shape, double t0, double t1){
		if (t0 < 0 && t1 > 1){
			GeneralPath p = new GeneralPath();
			
			Point2D p0 = interpolate(shape, t0, stampLView.getProj());
			p.moveTo((float)p0.getX(), (float)p0.getY());
			
			PathIterator pi = shape.getPathIterator(null, 0);
			float[] coords = new float[6];
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
				case PathIterator.SEG_LINETO:
					p.lineTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_CLOSE:
					throw new RuntimeException("Unhandled situation! Expecting unclosed line-string.");
				}
				pi.next();
			}
			
			Point2D p1 = interpolate(shape, t1, stampLView.getProj());
			p.lineTo((float)p1.getX(), (float)p1.getY());
			
			return p;
		}
		else if (t0 < 0){
			GeneralPath p = new GeneralPath();
			
			Point2D p0 = interpolate(shape, t0, stampLView.getProj());
			p.moveTo((float)p0.getX(), (float)p0.getY());
			
			PathIterator pi = shape.getPathIterator(null, 0);
			float[] coords = new float[6];
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
				case PathIterator.SEG_LINETO:
					p.lineTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_CLOSE:
					throw new RuntimeException("Unhandled situation! Expecting unclosed line-string.");
				}
				pi.next();
			}
			return p;
		}
		else if (t1 > 1){
			GeneralPath p = new GeneralPath();
			PathIterator pi = shape.getPathIterator(null, 0);
			float[] coords = new float[6];
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
					p.moveTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_LINETO:
					p.lineTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_CLOSE:
					throw new RuntimeException("Unhandled situation! Expecting unclosed line-string.");
				}
				pi.next();
			}
			
			Point2D p1 = interpolate(shape, t1, stampLView.getProj());
			p.lineTo((float)p1.getX(), (float)p1.getY());
			
			return p;
		}
		else {
			GeneralPath p = new GeneralPath();
			
			PathIterator pi = shape.getPathIterator(null, 0);
			double coords[] = new double[6];
			Point2D.Double first = new Point2D.Double();
			Line2D.Double lseg = new Line2D.Double();
			double linearDist = 0;
			boolean startDone = false, endDone = false;
			double totalLength = perimeterLength(shape, stampLView.getProj())[2];
			
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
					first.x = lseg.x1 = lseg.x2 = coords[0];
					first.y = lseg.y1 = lseg.y2 = coords[1];
					break;
				case PathIterator.SEG_LINETO:
					lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
					lseg.x2 = coords[0]; lseg.y2 = coords[1];
					break;
				case PathIterator.SEG_CLOSE:
					lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
					lseg.x2 = first.x; lseg.y2 = first.y;
					break;
				}
				
				double segLength = lseg.getP2().distance(lseg.getP1());
				if (!startDone && t0 >= (linearDist/totalLength) && t0 <= ((linearDist+segLength)/totalLength)){
					Point2D pt = Util.interpolate(lseg, t0-linearDist/totalLength);
					p.moveTo((float)pt.getX(), (float)pt.getY());
					startDone = true;
				}
				if (!endDone && t1 >= (linearDist/totalLength) && t1 <= ((linearDist+segLength)/totalLength)){
					Point2D pt = Util.interpolate(lseg, t1-linearDist/totalLength);
					p.lineTo((float)pt.getX(), (float)pt.getY());
					endDone = true;
				}
				if (startDone && !endDone){
					p.lineTo((float)coords[0], (float)coords[1]);
				}
				
				linearDist += segLength;
				pi.next();
			}
			return p;
		}
	}

	/**
	 * Computes the perimeter length of the given shape (in world coordinates). The
	 * length is computed using the 
	 * {@link Util#angularAndLinearDistanceW(Point2D, Point2D, edu.asu.jmars.layer.MultiProjection)}
	 * method.
	 * @param shape Shape in world coordinates.
	 * @return Length of perimeter in degrees, kilometers and cartesian-distance.
	 */
	public double[] distanceTo(Shape shape, Point2D pt){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double angularDist = 0;
		double linearDist = 0;
		double cartDist = 0;
		double t = uninterpolate(shape, pt, null, stampLView.getProj());
		double lengths[] = perimeterLength(shape, stampLView.getProj());
		double totalLength = lengths[2];
		
		if (t < 0 || t > 1){
			return new double[]{ Double.NaN, Double.NaN, Double.NaN };
		}
		else {
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
					first.x = lseg.x1 = lseg.x2 = coords[0];
					first.y = lseg.y1 = lseg.y2 = coords[1];
					break;
				case PathIterator.SEG_LINETO:
					lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
					lseg.x2 = coords[0]; lseg.y2 = coords[1];
					break;
				case PathIterator.SEG_CLOSE:
					lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
					lseg.x2 = first.x; lseg.y2 = first.y;
					break;
				}

				double lsegLength = lseg.getP2().distance(lseg.getP1());
				if ((cartDist + lsegLength)/totalLength > t){
					double dists[] = Util.angularAndLinearDistanceW(lseg.getP1(), pt, stampLView.getProj());
					angularDist += dists[0]; 
					linearDist += dists[1];
					cartDist += pt.distance(lseg.getP1());
					break;
				}
				double dists[] = Util.angularAndLinearDistanceW(lseg.getP1(), lseg.getP2(), stampLView.getProj());
				angularDist += dists[0];
				linearDist += dists[1];
				cartDist += lsegLength;
				pi.next();
			}
		}
		
		return new double[]{ angularDist, linearDist, cartDist };
	}


	/**
	 * Linearly uninterpolates the parameter <code>t</code> value of the specified
	 * point from its closest approach to the specified shape (in world 
	 * coordinates).
	 * @param shape Line-string in world-coordinates.
	 * @param pt Point for which the parameter <code>t</code> is to be determined.
	 * @param distance If not <code>null</code>, its first element contains
	 *     the minimum distance to one of the segments in the line-string.
	 * @return The parameter <code>t</code> which will give the specified
	 *     point if {@link #interpolate(Shape, double)} is called using it as the
	 *     second parameter. Returns {@link Double#NaN} if the shape contains only
	 *     a single point.
	 * {@see Util#uninterploate(Line2D, Point2D)}
	 */
	public static double uninterpolate(Shape shape, Point2D pt, double[] distance, MultiProjection proj){
		double t = Double.NaN;
		
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double cartDist = 0, linearDistToMinSeg = 0;
		double minDistSq = Double.MAX_VALUE;
		Line2D.Double minSeg = null;
		int currSeg = PathIterator.SEG_MOVETO;
		double totalLength = perimeterLength(shape, proj)[2];
		
		while(!pi.isDone()){
			switch(currSeg = pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
				first.x = lseg.x1 = lseg.x2 = coords[0];
				first.y = lseg.y1 = lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = coords[0]; lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = first.x; lseg.y2 = first.y;
				break;
			}
			
			double lsegDistSq = lseg.ptSegDistSq(pt);
			if (currSeg != PathIterator.SEG_MOVETO && lsegDistSq < minDistSq){
				minSeg = new Line2D.Double(lseg.x1, lseg.y1, lseg.x2, lseg.y2);
				minDistSq = lsegDistSq;
				linearDistToMinSeg = cartDist;
			}
			
			cartDist += lseg.getP2().distance(lseg.getP1());
			pi.next();
		}
		
		if (minSeg != null){
			double tt = Util.uninterploate(minSeg, pt);
			double minSegLength = minSeg.getP2().distance(minSeg.getP1());
			if (tt < 0 && linearDistToMinSeg > 0)
				tt = 0;
			if (tt > 1 && (linearDistToMinSeg + minSegLength) < totalLength)
				tt = 1;
			t = (linearDistToMinSeg + tt * minSegLength) / totalLength;
			//log.aprintln("pt:"+pt+"  linearDistToMinSeg:"+linearDistToMinSeg+"  uninterpol:"+Util.uninterploate(minSeg, pt)+"  minSeg:"+minSeg.getP1()+","+minSeg.getP2()+"  minSegDist:"+minSeg.getP2().distance(minSeg.getP1())+"  totalLen:"+totalLength);
			
			// fill the distance value
			if (distance != null && distance.length > 0)
				distance[0] = Math.sqrt(minDistSq);
		}

		return t;
	}

}
