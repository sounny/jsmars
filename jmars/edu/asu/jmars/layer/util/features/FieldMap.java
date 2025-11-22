package edu.asu.jmars.layer.util.features;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.swingx.combobox.EnumComboBoxModel;

import edu.asu.jmars.Main;
import edu.asu.jmars.graphics.GraphicsWrapped;
import edu.asu.jmars.layer.map2.MapChannelReceiver;
import edu.asu.jmars.layer.map2.MapChannelTiled;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.Pipeline;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.shape2.ColumnEditor;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.layer.util.NumericMapSourceDialog;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;

/**
 * Computes the min/max/mean/stdev of map pixels under
 * each shape at the given scale and projection.
 */
public class FieldMap extends CalculatedField {
	private static final long serialVersionUID = 1L;
	private static final Set<Field> fields = Collections.singleton(Field.FIELD_PATH);
	
	public enum Type {
		MIN,MAX,AVG,SUM,STDEV,COUNT
	}
	public Set<Field> getFields() {
		return fields;
	}
	
	/** @deprecated This remains here for binary compatibility with session files. */
	private final FeatureCollection fc;
	/** @deprecated This remains here for binary compatibility with session files. */
	private Field field;
	
	private final int band;
	private MapSource source;
	private int ppd;
	private Type type;
	
	public FieldMap(String name, Type type, int ppd, MapSource source, int band) {
		super(name, Double.class);
		this.type = type;
		this.source = source;
		this.band = band;
		this.ppd = ppd;
		this.fc = null;
	}
	
	/**
	 * Controls the sampling operation for a single Feature. Instances of this
	 * class are queued up in a pool, and they block the pool thread by using a
	 * barrier until the MapData requests have all arrived or an error occurs.
	 */
	private class MapSampler implements Runnable, MapChannelReceiver {
		/** the path to sample under */
		private final FPath path;
		private Shape roi;
		private Rectangle2D.Double bounds;
		private int count;
		private double sum, m, s, min, max;
		private MapChannelTiled ch = new MapChannelTiled(this);
		/** the resulting map sample */
		private Double stat;
		private volatile boolean finished = false;
		
		public MapSampler(FPath path) {
			this.path = path;
		}
		
		public void run() {
			try {
				Shape shape = path.getShape();
				bounds = new Rectangle2D.Double();
				bounds.setRect(shape.getBounds2D());
				
				// expand bounds out to nearest pixel boundary in all
				// directions, since MapData will round one way or the other for
				// us, and we want a predictable result for e.g. points that are
				// exactly between two pixels.
				double dpp = 1d/ppd;
				double x1 = Math.floor(bounds.x * ppd) * dpp;
				double x2 = Math.ceil((bounds.x + bounds.width) * ppd) * dpp;
				if (x1 == x2) {
					x1 -= dpp;
					x2 += dpp;
				}
				double y1 = Math.floor(bounds.y * ppd) * dpp;
				double y2 = Math.ceil((bounds.y + bounds.height) * ppd) * dpp;
				if (y1 == y2) {
					y1 -= dpp;
					y2 += dpp;
				}
				bounds.setFrameFromDiagonal(x1, y1, x2, y2);
				
				// keep shape x values >= 0, since MapData will as well and
				// we want the overlap checking that occurs later to remain
				// simple
				if (bounds.x < 0) {
					bounds.x += 360;
				}
				
				// Area estimate for shape
				double shapeArea = shape.getBounds2D().getWidth()*shape.getBounds2D().getHeight();
				// Area estimate for a 1x1 pixel map at the specified ppd
				double pixelArea = dpp *dpp;
				
				// Use the bounding box for points, to ensure we enclose some area
				if (path.getType() == FPath.TYPE_POINT) {
					roi = bounds;
				}
				// If our shape is smaller than a single pixel, use the entire bounds as well.  Otherwise we can end up with NO DATA
				// because the code to draw the roi into the pixel will decide not to draw anything at all
				else if (shapeArea < pixelArea) {
					roi = bounds;
				} else {
					roi = shape;
				}
				
				// initialize accumulator variables
				sum = 0;
				m = 0;
				s = 0;
				count = 0;
				min = Double.POSITIVE_INFINITY;
				max = Double.NEGATIVE_INFINITY;
				
				// kick off map request
				ch.setRequest(Main.PO, bounds, ppd, new Pipeline[]{new Pipeline(source, new Stage[0])});
			} catch (Exception e) {
				e.printStackTrace();
				finishStat();
			}
		}
		
		/** add the partial statistics from this tile */
		private void accumulateStat(MapData mapData, Rectangle2D tileBounds) {
			Raster raster = mapData.getRasterForWorld(tileBounds);
			int width = raster.getWidth();
			int height = raster.getHeight();
			
			BufferedImage maskImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D g2 = maskImage.createGraphics();
			g2.setTransform(Util.world2image(tileBounds, width, height));
			g2 = new GraphicsWrapped(g2,360,ppd,tileBounds,"maskWrapped");
			try {
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
				g2.setColor(Color.white);
				// anti-aliasing is slower and unwanted here
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
				// fill and then draw a border, just more than 1 pixel so
				// pixels touching the shape are always filled in
				g2.fill(roi);
				g2.setStroke(new BasicStroke(1.01f/ppd));
				g2.draw(roi);
			} finally {
				g2.dispose();
			}
			
			int[] mask = new int[width*height];
			maskImage.getRaster().getPixels(0, 0, width, height, mask);
			Rectangle region = mapData.getRasterBoundsForWorld(tileBounds);
			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					if (mask[j*width+i] != 0 && !mapData.isNull(i + region.x, j + region.y)) {
						// TODO: add support for selecting the band to sample
						double value = raster.getSampleDouble(i, j, band);
						
						if (Double.isNaN(value)) {
							continue;
						}

						count ++;

						switch (type) {
						case AVG:
						case SUM:
							sum += value;
							break;
						case MIN:
							min = Math.min(min, value);
							break;
						case MAX:
							max = Math.max(max, value);
							break;
						case STDEV:
							if (count == 1) {
								m = value;
								s = 0;
							} else {
								double delta = value - m;
								m += delta/count;
								s += delta*(value-m);
							}
							break;
						}
					}
				}
			}
		}
		
		/**
		 * Computes the final stat or error indicator, sends it to the Feature,
		 * stops any further processing for this Feature, and unlocks the pool
		 * so the next Feature may be processed.
		 */
		private void finishStat() {
			try {
				if (count == 0) {
					stat = null;
				} else {
					switch (type) {
					case AVG: stat = sum/count; break;
					case MIN: stat = min; break;
					case MAX: stat = max; break;
					case SUM: stat = sum; break;
					case STDEV: stat = count==1 ? 0 : Math.sqrt(s/(count-1)); break;
					case COUNT: stat = count*1.0; break;
					default: throw new IllegalStateException("Unsupported stat type");
					}
				}
			} finally {
				ch.cancel();
				finished = true;
				synchronized(this) {
					notifyAll();
				}
			}
		}
		
		public void mapChanged(MapData mapData) {
			try {
				if (mapData.isFinished()) {
					// all fragments for this MapData have arrived or failed
					// so see if the portion of 'bounds' under this request finished
					Rectangle2D.Double tileBounds = new Rectangle2D.Double();
					tileBounds.setFrame(bounds);
					Area finished = mapData.getFinishedArea();
					Rectangle2D finishedBounds = finished.getBounds2D();
					if (!finishedBounds.intersects(tileBounds)) {
						double xdelta = finishedBounds.getMinX() - tileBounds.x;
						tileBounds.x += 360 * Math.signum(xdelta);
					}
					Rectangle2D.intersect(tileBounds, finishedBounds, tileBounds);
					if (mapData.getImage() == null || !finished.contains(tileBounds)) {
						// missing data, set error condition and set result on Feature
						count = 0;
						finishStat();
					} else {
						// include this tile in the running stats
						accumulateStat(mapData, tileBounds);
						if (ch.isFinished()) {
							// compute final stat and set it on the feature
							finishStat();
						}
					}
				}
			} catch (Exception e) {
				// something went wrong, set error condition and set result on Feature
				e.printStackTrace();
				count = 0;
				finishStat();
				return;
			}
		}
	}
	
	/**
	 * Requests tiles for this map sampling operation over the given feature,
	 * blocks until the statistic has been cobbled together, and returns the value.
	 * The MapSampler must asynchronously receive updates on the event thread,
	 * therefore it cannot synchronously block the event thread and must be
	 * called from another thread.
	 */
	public Object getValue(ShapeLayer layer, Feature f) {
		if (SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException("Must not be called on the AWT event thread.");
		}
		
		// use the cache if it contains this feature
		FPath path = layer.getIndex().getWorldPath(f);
		if (path == null) {
			// otherwise calculate the geometry style here
			path = layer.getStylesLive().geometry.getValue(f).getWorld();
		}
		
		MapSampler sampler = new MapSampler(path);
		sampler.run();
		
		while (!sampler.finished) {
			try {
				synchronized(sampler) {
					sampler.wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
		return sampler.stat;
	}
	
	/**
	 * Called from the landing layer
	 * 
	 * Requests tiles for this map sampling operation over the given feature,
	 * blocks until the statistic has been cobbled together, and returns the value.
	 * The MapSampler must asynchronously receive updates on the event thread,
	 * therefore it cannot synchronously block the event thread and must be
	 * called from another thread.
	 */
	public Object getValue(FPath path) {
		if (SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException("Must not be called on the AWT event thread.");
		}
		
		// convert from spacial degrees west to world points
		FPath fPath = path.getWorld();
		
		MapSampler sampler = new MapSampler(fPath);
		sampler.run();
		
		while (!sampler.finished) {
			try {
				synchronized(sampler) {
					sampler.wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
		return sampler.stat;
	}
	
	
	public static class Factory extends FieldFactory<FieldMap> {
		private static final Comparator<MapSource> byTitle = new Comparator<MapSource>() {
			public int compare(MapSource o1, MapSource o2) {
				return o1.getTitle().compareTo(o2.getTitle());
			}
		};
		
		
		public Factory() {
			super("Map Sampling", FieldMap.class, Double.class);

		}
		
		private static ComboBoxModel getPpdModel(MapSource source) {
			List<Integer> ppdlist = new ArrayList<Integer>();
			double maxPPD = source.getMaxPPD();
			//if the source has a max ppd of less than one, default to 1.
			if(maxPPD<1){
				maxPPD = 1;
			}
			for (int ppd = 1; ppd <= maxPPD; ppd *= 2) {
				ppdlist.add(ppd);
			}
			return new DefaultComboBoxModel(ppdlist.toArray());
		}
		
		
		
		public JPanel createEditor(ColumnEditor colEditor, Field field) {
			//used for ui
			int pad = 1;
			Insets in = new Insets(pad,pad,pad,pad);
			int row = 0;
			
			final FieldMap f = (FieldMap)field;
			
			//GUI Components
			final JPanel mainPnl = new JPanel();
			
			//TextArea with information about map sampling
			JTextArea infoText = new JTextArea();
			infoText.setWrapStyleWord(true);
			infoText.setLineWrap(true);
			Font font = ThemeFont.getBold();
			infoText.setFont(font);			
			infoText.setText("Computes the selected statistic from all pixel " +
					"values in the selected map, under the shape, and sampled " +
					"at the selected resolution.");
			infoText.setEditable(false);
			
			//The map source button and related source gui components
			JButton sourceBtn = new JButton("Select Map Source...".toUpperCase());
			final JTextField sourceTF = new JTextField(20);
			sourceTF.setEditable(false);
			sourceTF.setText(f.source.getTitle());
			sourceTF.setColumns((int)(f.source.getTitle().length()*.8));
			final JComboBox ppdCB = new JComboBox();
			final JComboBox typeCB = new JComboBox(new EnumComboBoxModel<Type>(Type.class));
			final JLabel unitsLbl = new JLabel();
			final JTextArea descriptionTA = new JTextArea();
			
			sourceBtn.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ArrayList<MapSource> sources = NumericMapSourceDialog.getUserSelectedSources(mainPnl.getRootPane(), false, false); 
					if(sources.size()>0){
						MapSource newSource = sources.get(0);
						//if source is null, return.  This shouldn't happen though.
						if(newSource != null){
							f.source = newSource;
							ppdCB.setModel(getPpdModel(f.source));
							ppdCB.setSelectedIndex(ppdCB.getModel().getSize()-1);
							//check to see if units is null
							if(f.source.getUnits()!=null){
								unitsLbl.setText(f.source.getUnits());
							}else{
								unitsLbl.setText("Not Available");
							}
							descriptionTA.setText(f.source.getAbstract());
							
							//update the source TF which is displaying the selected map
							String title = f.source.getTitle();
							sourceTF.setText(title);
							sourceTF.setColumns((int)(title.length()*.8));
							
							descriptionTA.setCaretPosition(0); //so scrollbar comes back to the top
						}
					}
				}
			});
			
			//map panel
			row = 0;
			JPanel mapPnl = new JPanel(new GridBagLayout());
			mapPnl.add(sourceBtn, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
			mapPnl.add(sourceTF, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
			
			//ppd components
			JLabel ppdLbl = new JLabel("PPD: ");
			ppdCB.setLightWeightPopupEnabled(false);
			ppdCB.setModel(getPpdModel(f.source));
			ppdCB.setSelectedItem(f.ppd);
			ppdCB.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					f.ppd = (Integer)ppdCB.getSelectedItem();
				}
			});
			JPanel ppdPnl = new JPanel(new GridBagLayout());
			ppdPnl.add(ppdLbl);
			ppdPnl.add(ppdCB);
			
			//stat components
			JLabel statLbl = new JLabel("Stat: ");
			typeCB.setLightWeightPopupEnabled(false);
			typeCB.setSelectedItem(f.type);
			typeCB.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					f.type = (Type)typeCB.getSelectedItem();
				}
			});
			JPanel statPnl = new JPanel(new GridBagLayout());
			statPnl.add(statLbl);
			statPnl.add(typeCB);
			
			JPanel ppdStatPnl = new JPanel();
			ppdStatPnl.add(ppdPnl);
			ppdStatPnl.add(Box.createHorizontalStrut(5));
			ppdStatPnl.add(statPnl);
			
			//Map info panel with components
			JPanel mapInfoPnl = new JPanel(new BorderLayout());
			mapInfoPnl.setBorder(new TitledBorder("Map Source Info"));
			//units
			JLabel uLbl = new JLabel("Units: ");
			Font plainFt = ThemeFont.getRegular();
			unitsLbl.setFont(plainFt);
			//if null, set to not available
			if(f.source.getUnits()!=null){
				unitsLbl.setText(f.source.getUnits());
			}else{
				unitsLbl.setText("Not Available");
			}
			JPanel unitsPnl = new JPanel(new BorderLayout());
			unitsPnl.setBorder(new EmptyBorder(5, 5, 0, 0));
			unitsPnl.add(uLbl, BorderLayout.WEST);
			unitsPnl.add(unitsLbl, BorderLayout.CENTER);
			//description
			JLabel dLbl = new JLabel("Description: ");
			descriptionTA.setText(f.source.getAbstract());
			descriptionTA.setWrapStyleWord(true);
			descriptionTA.setLineWrap(true);			
			descriptionTA.setCaretPosition(0); //so scrollbar comes back to the top
			descriptionTA.setEditable(false);
			JScrollPane scroll = new JScrollPane(descriptionTA);
			scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			JPanel descripPnl = new JPanel(new BorderLayout());
			descripPnl.setBorder(new EmptyBorder(5,5,0,0));
			JPanel dPnl = new JPanel(new BorderLayout());
			dPnl.add(dLbl, BorderLayout.NORTH);
			descripPnl.add(dPnl, BorderLayout.WEST);
			descripPnl.add(scroll, BorderLayout.CENTER);
			//add to mapinfopanel
			mapInfoPnl.add(unitsPnl, BorderLayout.NORTH);
			mapInfoPnl.add(descripPnl, BorderLayout.CENTER);

			//Put all UI components together
			mainPnl.setLayout(new BoxLayout(mainPnl, BoxLayout.PAGE_AXIS));
			mainPnl.setBorder(new EmptyBorder(10,5,5,5));
			mainPnl.add(infoText);
			mainPnl.add(Box.createVerticalStrut(5));
			mainPnl.add(mapPnl);
			mainPnl.add(Box.createVerticalStrut(5));
			mainPnl.add(ppdStatPnl);
			mainPnl.add(Box.createVerticalStrut(15));
			mainPnl.add(mapInfoPnl);
			
			return mainPnl;
		}
		
		/** Returns a field to compute the values, using the collection and field if it is necessary to send asynchronous updates */
		public FieldMap createField(Set<Field> fields) {
			Type opType = Type.AVG;
			String defaultElevServer = Config.get("threed.default_elevation.server");
			defaultElevServer = Config.get(Util.getProductBodyPrefix() + "threed.default_elevation.server", defaultElevServer);
			String defaultElevSource = Config.get("threed.default_elevation.source");
			defaultElevSource = Config.get(Util.getProductBodyPrefix() + "threed.default_elevation.source", defaultElevSource);
			MapServer server = MapServerFactory.getServerByName(defaultElevServer);
			MapSource source = server.getSourceByName(defaultElevSource);
	
			int ppd = (int)Math.round(Math.pow(2, Math.ceil(Math.log(source.getMaxPPD())/Math.log(2))));
			return new FieldMap(getName(), opType, ppd, source, 0);
		}
	}
}
