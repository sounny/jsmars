package edu.asu.jmars.layer.profile;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import javax.swing.event.SwingPropertyChangeSupport;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jfree.data.Range;
import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.map2.MapChannel;
import edu.asu.jmars.layer.map2.MapChannelReceiver;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.Pipeline;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.StageUtil;
import edu.asu.jmars.layer.map2.msd.PipelineModel;
import edu.asu.jmars.layer.map2.stages.composite.BandAggregatorSettings;
import edu.asu.jmars.layer.map2.stages.composite.CompositeStage;
import edu.asu.jmars.layer.profile.ProfileLView.ProfileLine;
import edu.asu.jmars.layer.profile.config.Config;
import edu.asu.jmars.layer.profile.manager.ProfileManagerMode;
import edu.asu.jmars.layer.threed.StartupParameters;
import edu.asu.jmars.util.DebugLog;


public class ProfileLayer extends Layer implements IProfileModel, MapChannelReceiver {
	private static DebugLog log = DebugLog.instance();
	private ProfileLView profileLView;		
	private MapChannel mapchannel = null;
	private List<MapSource> mapSources = new ArrayList<>();		
	private StartupParameters defaultparams = null;
	private Pipeline pipeline[] = null;
	private ChartDataConverter dataconversion;
	private final SwingPropertyChangeSupport propertyChangeSupport = new SwingPropertyChangeSupport(this, true);

	public ProfileLayer() {
	}

	public void init(ProfileLView profileLayerView) {
		this.profileLView = profileLayerView;
		mapSources.clear();
		defaultparams = new StartupParameters();
		mapSources.add(defaultparams.getMapSource());		
		createChartPipeline(mapSources.size());
		createNewChartMapDataChannel();
		updateChartMapDataChannelForPipeline(this.pipeline);		
		dataconversion = new ChartDataConverter(profileLView);
	}

	public void updateMapSource(List<MapSource> input) {
		mapSources = new ArrayList<>();
		if ( !input.isEmpty() ) {
			ListIterator<MapSource> iterator = input.listIterator();
	        while(iterator.hasNext()) {
	        	this.mapSources.add((MapSource) iterator.next());
	        }		     
		}		
		createChartPipeline(mapSources.size());
		updateChartMapDataChannelForPipeline(pipeline);
	}

	public List<MapSource> getMapSources() {
		List<MapSource> varmapsources = new ArrayList<>();	
		varmapsources.addAll(mapSources);
		return varmapsources;
	}

	private void createChartPipeline(int mapSourcesCount) {
		this.pipeline = new Pipeline[mapSourcesCount];
		for (int i = 0; i < mapSourcesCount; i++) {
			this.pipeline[i] = new Pipeline(mapSources.get(i),
					new Stage[] { (CompositeStage) (new BandAggregatorSettings((mapSources.size()))).createStage() });
		}
		PipelineModel ppm = new PipelineModel(this.pipeline);
		try {
			this.pipeline = Pipeline.getDeepCopy(ppm.buildPipeline());
		} catch (CloneNotSupportedException e) {
			this.pipeline = null;
			e.printStackTrace();
		}
	}

	private void createNewChartMapDataChannel() {	
		mapchannel = new MapChannel(null, 1, Main.PO, new Pipeline[0]);
		mapchannel.addReceiver(this);
	}

	private void updateChartMapDataChannelForPipeline(Pipeline[] pipeline) {
		mapchannel.setPipeline(pipeline);
	}

	public boolean hasEmptyPipeline() {
		return mapchannel.getPipeline() == null || mapchannel.getPipeline().length == 0;
	}

//Events		
	@Override
	public void addProfileLine(Map<Integer,Shape> profileLines) {	
		Map<Integer, Shape> varprofiles = new LinkedHashMap<>();
		varprofiles.putAll(profileLines);
		firePropertyChange(NEW_PROFILEDATA_EVENT, null, new ImmutablePair<Map<Integer,Shape>, Integer>(varprofiles, 1));		
	}
	
	@Override
	public void addSelectedProfilesToChart(Map<Integer, Shape> profileLines, ProfileManagerMode mode) {
		Map<Integer, Shape> varprofiles = new LinkedHashMap<>();
		varprofiles.putAll(profileLines);
		firePropertyChange(SELECTED_TO_PLOT_PROFILEDATA_EVENT, null, new ImmutablePair<Map<Integer,Shape>, ProfileManagerMode>(varprofiles, mode));			
	}
	
	@Override 
	public void addProfileLineChartOnly(Map<Integer, Shape> selectedprofiles) {
		Map<Integer, Shape> varprofiles = new LinkedHashMap<>();
		varprofiles.putAll(selectedprofiles);
		firePropertyChange(CHART_ONLY_PROFILEDATA_EVENT, null, new ImmutablePair<Map<Integer,Shape>, Integer>(varprofiles, 1));				
	}
	
	@Override
	public void requestConfigChanged() {
		firePropertyChange(REQUEST_CHART_CONFIG_CHANGED, null, null);						
	}
	
	@Override
	public void lineWidthChanged(Pair<ProfileLView,Integer> pair) {
		firePropertyChange(REQUEST_PROFILE_LINE_WIDTH_CHANGED, null, pair);						
	}

	public void requestMapSourceData() {
		fetchMapSourceData();
	}
	

    private void fetchMapSourceData( ) {
		firePropertyChange(NEW_MAPSOURCE_EVENT, null, new ImmutablePair<>(mapchannel.getPipeline(), 1));
    }
    
	
	private void fetchNumericDataSample(Map<Integer, Shape> newViewExtents, Range span, int ppd) {
		/**
		 * Sets the view extent to the specified extent. The specified extent is
		 * converted into effective extent by narrowing it to the profile line's extent.
		 * Appropriate diagonal from this extent is the output of the profile line.
		 */	
	
		for (Map.Entry<Integer,Shape> entry : newViewExtents.entrySet()) {
		    int ID = entry.getKey();
		    Shape shape = entry.getValue();
		    ProfileLView.ProfileLine pl = (ProfileLine) this.profileLView.getProfilelineByID(ID);
		    
		    if (pl==null) continue;
		    
		    ProjObj proj = pl.getOrigProjection();
		    
			NumericMapDataSampleWrapper numericsamplewrapper = new NumericMapDataSampleWrapper(ID, shape, mapchannel.getPipeline(), proj, ppd, span);
			numericsamplewrapper.sendNumericDataRequest();				
	   }
	}


	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	
	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}
           	
	
	@Override
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
	}

	public void requestDataUpdate(Map<Integer, Shape> newViewExtents, Range span, int ppd) {
		fetchNumericDataSample(newViewExtents, span, ppd);
	}


	public void newMapSource(List<MapSource> mapsources) {
		this.updateMapSource(mapsources);
		int newppd = this.profileLView.getProj().getPPD();
		firePropertyChange(NEW_MAPSOURCE_EVENT, null, new ImmutablePair<>(mapchannel.getPipeline(), newppd));
	}
	

	public void createChart(Config chartconfig) {
		firePropertyChange(CREATE_CHART, null, chartconfig);		
	}

	
	public void registerShape(Pair<Integer, String> pair) {		
		firePropertyChange(REGISTER_SHAPE_EVENT, null, pair);		
	}
	
	public void requestViewChartForProfile(int ID) {
		firePropertyChange(VIEW_CHART_FOR_PROFILE, null, ID);					
	}
		
	public void requestChartCrosshairUpdateForProfile(int profileID, Point2D newpointWorld) {
		firePropertyChange(UPDATE_CROSSHAIR_FOR_PROFILE, null, new ImmutablePair<Integer,Point2D>(profileID,newpointWorld));			
	}	

	public void requestCueUpdateForProfile(Pair<Integer, Point2D> pair, ProfileLView view) {
		firePropertyChange(UPDATE_CUE_FOR_PROFILE, null, new ImmutablePair<Pair,ProfileLView>(pair,view));		
	}	
	
	private void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
	}
	
	@Override
	public void mapChanged(MapData mapData) {				
	}
	
	

	public class NumericMapDataSampleWrapper implements MapChannelReceiver {
		private MapChannel numericmapchannel = null;
		private ProjObj projObj;
		Pipeline[] numericpipeline = null;
		int ppd;
		private Shape viewExtent;
		private Range effRange = new Range(0.0, 1.0);
		int uniqueID;  //unique id that ties sample to the profile line
		
		public NumericMapDataSampleWrapper(int ID, Shape newviewextent, Pipeline[] pipeline, ProjObj proj, int ppd2, Range span) {
			this.viewExtent = newviewextent;
			this.ppd = ppd2;
			this.projObj = proj;
			this.effRange = span;
			this.uniqueID = ID;
			this.numericpipeline = Arrays.stream(pipeline).toArray(Pipeline[]::new);			
			this.numericmapchannel = new MapChannel(null, ppd, projObj, new Pipeline[0]);
			this.numericmapchannel.addReceiver(this);
			this.numericmapchannel.setPipeline(numericpipeline);
		}
		
		public void sendNumericDataRequest() {
			if (this.viewExtent != null && this.numericmapchannel.getPipeline().length > 0) {
				Rectangle2D requestedExtent = dataconversion.expandByXPixelsEachSide(this.viewExtent.getBounds2D(),this.ppd, 0.5);						
				double x1 = Math.floor(requestedExtent.getMinX() * this.ppd) / this.ppd;
				double x2 = Math.ceil(requestedExtent.getMaxX() * this.ppd) / this.ppd;
				double y1 = Math.floor(requestedExtent.getMinY() * this.ppd) / this.ppd;
				double y2 = Math.ceil(requestedExtent.getMaxY() * this.ppd) / this.ppd;
				requestedExtent = new Rectangle2D.Double(x1, y1, Math.max(1.0 / this.ppd, x2 - x1),
						Math.max(1.0 / this.ppd, y2 - y1));
				log.println("Requesting viewExtent:" + requestedExtent + " at " + this.ppd + " ppd.");
				//original world coords and original projection for THIS shape = "Extent" 
				this.numericmapchannel.setMapWindow(requestedExtent, this.ppd, this.projObj);
			}	
		}
					
		
		@Override
		public void mapChanged(MapData mapData) {
			if (mapData == null || this.viewExtent == null) {
				log.println("Either mapData was null or the profileLine was null.");
				return;
			}

			if (mapData.getImage() == null) {
				log.println("mapData.getImage() was null, setting status only.");
				return;
			}

			if (!mapData.isFinished())
				return;

			// Sample profile line data from the returned raster
			NumericMapDataSample numericsample = new NumericMapDataSample(uniqueID, mapData, this.viewExtent, this.effRange.getLowerBound(),
					this.effRange.getUpperBound(), mapData.getRequest().getPPD());			

			Pipeline[] pipeline = this.numericmapchannel.getPipeline();
			if (pipeline != null && numericsample.getNumBands() != pipeline.length) {
				log.println("Pipeline and data bands mismatch (" + pipeline.length + " vs " + numericsample.getNumBands()
						+ ").");
				numericsample = null;
				return;
			}
			Pair<Pipeline[], NumericMapDataSample> plotData = new ImmutablePair<>(pipeline, numericsample);
			firePropertyChange(PLOT_DATA_EVENT, null, plotData);
		}
								
		
	public class NumericMapDataSample {
		final Shape lseg; // Line-segment in world coordinates
		public final double t0; // [0,1] means the entire lseg, [0.5,1] means mid of lseg to end
		public final double t1;
		final double lsegLength; // Length of line segment in degrees (of world coordinates)
		public final double lsegLengthKm; // Length of line segment in Km
		final int nSamples; // Number of samples (or pixels)
		final int ppd; // Requested pixel-per-degree of data
		final int nBands; // Number of bands per numericsample (or per pixel)
		final MapData mapData; // MapData object used as source of all numericsample data
		final AffineTransform ext2Pix; // Transform to convert from map extent (or world) coordinates to mapData raster
										// coordinates.	
		final int ID;   //unique id that ties to lSeg
		
		/** First point in lseg. */
		Point2D pt0;

		/**
		 * World coordinates of each of the sampled location along the lseg.
		 */
		Point2D[] pts;

		/**
		 * Sampled data as array of doubles for each sampled location.
		 */
		double[][] data;

		/**
		 * Linear distance in Km from the start of lseg.
		 */
		double[] dist;

		/** t-parameter locations at which the data has been sampled. */
		double[] tVals;

		/**
		 * Constructs a Samples object which holds numericsample data for the specified
		 * line segment as extracted from the input mapData object. Consecutive samples
		 * are spaced at 1/ppd.
		 * @param uniqueID 
		 * 
		 * @param mapData Source to use for sampling the data.
		 * @param lseg    Line segment along which sampling is to be done.
		 * @param ppd     Spacing between consecutive samples.
		 */
		public NumericMapDataSample(int uniqueID, MapData mapData, Shape lseg, double t0, double t1, int ppd) {
			this.lseg = lseg;
			this.t0 = t0;
			this.t1 = t1;
			this.ppd = ppd;
			this.mapData = mapData;
			this.ID = uniqueID;
			double dists[] = dataconversion.perimeterLength(lseg);
			lsegLength = dists[0];
			lsegLengthKm = dists[1];
			nSamples = (int) (ppd * lsegLength * (t1 - t0));
			nBands = mapData.getImage() != null ? mapData.getImage().getData().getNumBands() : 0;
			ext2Pix = StageUtil.getExtentTransform(mapData.getImage().getWidth(), mapData.getImage().getHeight(),
					mapData.getRequest().getExtent());

			pt0 = dataconversion.getFirstPoint(lseg);

			pts = new Point2D[nSamples];
			data = new double[nSamples][];
			dist = new double[nSamples];
			tVals = new double[nSamples];

			sampleData(mapData);
		}
			
		
		private void sampleData(MapData mapData) {
			BufferedImage image = mapData.getImage();
			Raster raster = image.getData();
			Rectangle rasterBounds = raster.getBounds();

			// Each band can potentially have it's own array of ignore values
			double ignoreValues[][] = new double[nBands][];

			for (int i = 0; i < nBands; i++) {
				ignoreValues[i] = mapchannel.getPipeline()[i].getSource().getIgnoreValue();
			}

			Point2D pix = new Point2D.Double();

			for (int i = 0; i < nSamples; i++) { // Loops over the left edge
				double t = t0 + ((double) i) / nSamples; // TODO: Not quite correct, t never equals 1
				tVals[i] = t;
				pts[i] = dataconversion.interpolate(lseg, t);
				dist[i] = dataconversion.distanceTo(lseg, pts[i])[1];
				ext2Pix.transform(pts[i], pix);
				// if (i < 3 || i > (nSamples-3))
				// log.aprintln("i:"+i+" t:"+t+" pts[i]:"+pts[i]+" dist[i]:"+dist[i]+"
				// pix:"+pix+" contains?"+rasterBounds.contains(pix));
				if (rasterBounds.contains(pix)) {
					raster.getPixel((int) pix.getX(), (int) pix.getY(), data[i] = new double[nBands]);
					for (int b = 0; b < nBands; b++) {
						if (ignoreValues[b] != null) {
							for (int v = 0; v < ignoreValues[b].length; v++) {
								if (data[i][b] == ignoreValues[b][v])
									data[i][b] = Double.NaN;
							}
						}
					}
				} else {
					data[i] = null;
				}
			}
		}

		/**
		 * Return the point (in world coordinates) that falls at the specified distance
		 * (in Km) starting from the first point of the profile-line.
		 * 
		 * @param km Perimeter distance (in Km) from the first point of the
		 *           profile-line.
		 * @return <code>null</code> if there are less than two samples. Otherwise,
		 *         return the point as a linear interpolation of the bounding points
		 *         (based on distance in km).
		 */
		public Point2D getPointAtDist(double km) {
			int idx = Arrays.binarySearch(dist, km);
			double t;
			if (idx < 0) {
				idx = -(idx + 1);
				double t0, t1, d0, d1;
				if (idx > 0 && idx < nSamples) {
					d0 = dist[idx - 1];
					d1 = dist[idx];
					t0 = tVals[idx - 1];
					t1 = tVals[idx];
				} else if (nSamples > 1) {
					if (idx == 0) {
						d0 = dist[0];
						d1 = dist[1];
						t0 = tVals[0];
						t1 = tVals[1];
					} else { // if (idx >= nSamples)
						d0 = dist[dist.length - 2];
						d1 = dist[dist.length - 1];
						t0 = tVals[dist.length - 2];
						t1 = tVals[dist.length - 1];
					}
				} else {
					return null;
				}
				double segLength = d1 - d0;
				double tt = (km - d0) / segLength;
				t = (t0 * (1 - tt) + t1 * tt);
			} else {
				t = tVals[idx];
			}
			Point2D p = dataconversion.interpolate(lseg, t);
			return p;
		}

		public int getNumBands() {
			return nBands;
		}

		public int getNumSamples() {
			return nSamples;
		}
		
		public int getUniqueID() {			
			return this.ID;
		}				

		public double[][] getSampleData() {
			return (double[][]) data.clone();
		}

		public double[] getSampleData(Point2D worldPt) {
			// log.println("getSampleData("+worldPt+")");
			Raster raster = mapData.getImage().getData();
			Point2D pix = null;
			double[] data = null;

			if (raster.getBounds().contains(pix = ext2Pix.transform(worldPt, null)))
				raster.getPixel((int) pix.getX(), (int) pix.getY(), data = new double[nBands]);

			return data;
		}

		public double[] getSampleData(double km) {
			// log.println("getSampleData("+km+")");
			int idx = getDistanceIndex(km);
			if (idx >= 0 && idx < dist.length)
				return data[idx];
			return null;
		}

		/**
		 * 
		 * @param worldPt
		 * @return Km distance from the starting point of the profile-line.
		 */
		public double getDistance(Point2D worldPt) {
			double t = dataconversion.uninterpolate(lseg, worldPt, null);
			int idx = Arrays.binarySearch(tVals, t);

			if (idx < 0) {
				idx = -(idx + 1);
				if (idx > 0 && idx < nSamples) {
					double tt = (t - tVals[idx - 1]) / (tVals[idx] - tVals[idx - 1]);
					return dist[idx - 1] * (1 - tt) + dist[idx] * tt;
				}
			} else {
				return dist[idx];
			}
			return Double.NaN;
		}

		public double[] getDistances() {
			return (double[]) dist.clone();
		}

		public Point2D[] getSamplePoints() {
			return (Point2D[]) pts.clone();
		}

		/**
		 * Returns the numeric sample index of the specified distance value (in Km).
		 * 
		 * @param km Input distance from the start of the profile-line.
		 * @return <code>-1</code> if the km value is less than zero and
		 *         <code>nSamples</code> if the km value is greater than the total
		 *         km-length of the profile line. Otherwise returns an index based on
		 *         the interpolated <code>t</code>-value based on the index.
		 */
		public int getDistanceIndex(double km) {

			int idx = Arrays.binarySearch(dist, km);

			if (idx < 0) {
				idx = -(idx + 1);
				if (idx > 0 && idx < nSamples) {
					double tt = (km - dist[idx - 1]) / (dist[idx] - dist[idx - 1]);
					return tt < 0.5 ? idx - 1 : idx;
				} else if (idx == 0) {
					return -1;
				} else {
					return nSamples;
				}
			} else {
				return idx;
			}
		}	
		
     public Pair<Double, Double> getLonLat (double km) {
    	 int arrayIndex = getDistanceIndex(km);
    	 if (arrayIndex >= 0 && arrayIndex < pts.length)
				return new ImmutablePair<Double, Double>(pts[arrayIndex].getX(), pts[arrayIndex].getY());
			return null; 
     }
     
	}		
   }

 }	
	
