package edu.asu.jmars.layer.shape2;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.ILayerSchemaProvider;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.map2.MapThreadFactory;
import edu.asu.jmars.layer.shape2.xb.data.service.Data;
import edu.asu.jmars.layer.util.features.AbstractFeatureCollection;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureEvent;
import edu.asu.jmars.layer.util.features.FeatureListener;
import edu.asu.jmars.layer.util.features.FeatureProvider;
import edu.asu.jmars.layer.util.features.FeatureProviderFactory;
import edu.asu.jmars.layer.util.features.FeatureProviderReadOnly;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.GeomSource;
import edu.asu.jmars.layer.util.features.MemoryFeatureIndex;
import edu.asu.jmars.layer.util.features.MultiFeatureCollection;
import edu.asu.jmars.layer.util.features.SingleFeatureCollection;
import edu.asu.jmars.layer.util.features.Style;
import edu.asu.jmars.layer.util.features.StyleSource;
import edu.asu.jmars.layer.util.filetable.FileTable;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.History;
import edu.asu.jmars.util.ObservableSet;
import edu.asu.jmars.util.Util;

public class ShapeLayer extends Layer {
	/** History size is obtained from the specified key. */
	public static final String CONFIG_KEY_HISTORY_SIZE = "shape.history_size";
	
	/** Sorts Field instances by name in a case-insensitive way */
	public static final Comparator<Field> fieldByName = new Comparator<Field>() {
		public int compare(Field o1, Field o2) {
			return String.CASE_INSENSITIVE_ORDER.compare(o1.name, o2.name);
		}
	};

	/** selected features for this layer */
	ObservableSet<Feature> selections = new ObservableSet<Feature>(new HashSet<Feature>());
	
	/**
	 * Stores all world coordinate paths in a quad tree; every feature in the
	 * feature table should be available here in world coordinate form.
	 */
	private final MemoryFeatureIndex index;
	
	/** FileTable for this layer */
	// TODO: this should be owned by the focus panel, NOT the layer!
	FileTable fileTable;

	/** History for changes to 'mfc' */
	private final History history = new History(Config.get(CONFIG_KEY_HISTORY_SIZE, 10));

	/** Keep track of states. */
	private List<LEDState> statusLEDStack = Collections.synchronizedList(new ArrayList<LEDState>());

	/** Factory for producing FeatureProviders */
	private FeatureProviderFactory providerFactory;
	
	boolean showProgress = false;
	
	String name = "Shape Layer";
	
	final Set<Field> tooltipFields = new LinkedHashSet<Field>();
	
	// style settings for this layer
	private final ShapeLayerStyles styles = new ShapeLayerStyles();
	
	/*
	 * Status to color mapping; earlier entries have priority over later entries
	 */
	protected static Map<Class<? extends LEDState>,Color> statusLEDColor;
	static {
		statusLEDColor = new LinkedHashMap<Class<? extends LEDState>,Color>();
		statusLEDColor.put(LEDStateFileIO.class, Color.RED);
		statusLEDColor.put(LEDStateProcessing.class, Color.ORANGE);
		statusLEDColor.put(LEDStateDrawing.class, Color.YELLOW);
	}
	
	public static String[] getFeatureProviderClassNames(){
		String[] providers = Config.getAll("shape.featurefactory");
		String[] classes = new String[providers.length / 2];
		for (int i = 1; i < providers.length; i+=2) {
			classes[(i-1)/2] = providers[i];
		}
		return classes;
	}
	
	public final Map<FeatureCollection,CalcFieldListener> calcFieldMap = new HashMap<FeatureCollection,CalcFieldListener>();
	private final FeatureCollection stylesFC = new SingleFeatureCollection();
	
	public boolean isReadOnly=false;

	/** The name of the custom shape layer added by the AddLayer Dialog */
	public static final String CUSTOM_SHAPE_NAME = "Custom Shape Layer";	
	
	/** Index for the image buffer for state ids **/
	public static final int IMAGES_BUFFER=0;
	/** Index for the label buffere for state ids **/
	public static final int LABELS_BUFFER=1;
	
	public ShapeLayer(boolean isReadOnly) {
		super();
		this.isReadOnly=isReadOnly;
		
		String[] classes = getFeatureProviderClassNames();
		providerFactory = new FeatureProviderFactory(classes);
		
		fileTable = new FileTable(history);
		
		updateLoadedShapes();
		fileTable.getModel().addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				updateLoadedShapes();
			}
		});
		
		index = new MemoryFeatureIndex(styles.geometry, fileTable.getMultiFeatureCollection());
		
		StyleColumnPositioner stylePos = new StyleColumnPositioner(fileTable.getMultiFeatureCollection(), stylesFC);
		fileTable.getSelectionModel().addListSelectionListener(stylePos);
		fileTable.getSelectionModel().addListSelectionListener(Data.SERVICE);
		fileTable.getMultiFeatureCollection().addListener(stylePos);
		
		if(!isReadOnly){
			SingleFeatureCollection empty = new SingleFeatureCollection();
			fileTable.getFileTableModel().add(empty);
			fileTable.getSelectionModel().addSelectionInterval(0,0);
		}
		
		//initiate the stateId to the proper buffers and start them at 0.
		stateIds = new ArrayList<Integer>();
		stateIds.add(IMAGES_BUFFER, 0);
		stateIds.add(LABELS_BUFFER, 0);
	}
	
	/** Called when the file table contents change; updates the history and calc field listener */
	private void updateLoadedShapes() {
		Set<FeatureCollection> inTable = new HashSet<FeatureCollection>(fileTable.getFileTableModel().getAll());
		Set<FeatureCollection> inCalcMap = new HashSet<FeatureCollection>(calcFieldMap.keySet());
		for (FeatureCollection f: inCalcMap) {
			if (!inTable.contains(f)) {
				f.removeListener(calcFieldMap.remove(f));
				f.removeListener(Data.SERVICE);
			}
		}
		for (FeatureCollection f: inTable) {
			// make sure this history object is set on the data
			if (f instanceof AbstractFeatureCollection) {
				((AbstractFeatureCollection)f).setHistory(getHistory());
			}
			
			if (!inCalcMap.contains(f)) {
				// create the calc field updater, reusing the same field map
				CalcFieldListener c = new CalcFieldListener(f, this);
				calcFieldMap.put(f, c);
				f.addListener(c);
				f.addListener(Data.SERVICE);
			}
		}
	}
	
	public void cleanup() {
		index.disconnect();
		providerFactory = null;
		history.dispose();
	}
	
	public FileTable getFileTable() {
		return fileTable;
	}

	/** Not used */
	public void receiveRequest(Object layerRequest, DataReceiver requester) {
	}

	public MultiFeatureCollection getFeatureCollection(){
		return fileTable.getMultiFeatureCollection();
	}

	public History getHistory(){
		return history;
	}
	
	public void begin(LEDState state){
		statusLEDStack.add(state);
		updateStatus();
	}
	
	public void end(final LEDState state){
		statusLEDStack.remove(state);
		updateStatus();
	}
	
	private void updateStatus() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Color c = Color.GREEN.darker();
				synchronized(statusLEDStack) {
					// check for each status in priority order
					for (Class<?> test: statusLEDColor.keySet()) {
						boolean found = false;
						for (LEDState state: statusLEDStack) {
							if (state.getClass() == test) {
								c = statusLEDColor.get(test);
								found = true;
								break;
							}
						}
						if (found) {
							break;
						}
					}
				}
				setStatus(c);
			}
		});
	}
	
	public abstract static class LEDState {}
	public static class LEDStateProcessing extends LEDState {}
	public static class LEDStateFileIO extends LEDState {}
	public static class LEDStateAllDone extends LEDState {}
	public static class LEDStateDrawing extends LEDState {}
	
	/**
	 * Returns the provider factory created from the FeatureProvider class
	 * names in jmars.config. See FeatureProviderFactory for more info.
	 */
	public FeatureProviderFactory getProviderFactory () {
		return providerFactory;
	}
	
	/** Returns a copy of the styles object */
	public ShapeLayerStyles getStyles() {
		return new ShapeLayerStyles(styles);
	}
	
	/** @return The live styles object. */
	public ShapeLayerStyles getStylesLive() {
		return styles;
	}
	
	/** Returns the style instances that use any field in the given collection */
	public Set<Style<?>> getStylesFromFields(Collection<Field> fields) {
		Set<Style<?>> out = new HashSet<Style<?>>();
		for (Style<?> s: styles.getStyles()) {
			if (!Collections.disjoint(fields, s.getSource().getFields())) {
				out.add(s);
			}
		}
		return out;
	}
	
	/** Sets style sources on this styles document from the sources in the given set of styles */
	public void applyStyleChanges(Set<Style<?>> changes) {
		applyStyleChanges(changes, null);
	}
	
	/** Sets style sources on this styles document from the sources in the given set of styles. Overrides the geometrySource style with the one provided */
	public void applyStyleChanges(Set<Style<?>> changes, StyleSource<FPath> geomSource) {
		// match style instances by name
		Set<Style<?>> current = styles.getStyles();
		for (Style<?> s: changes) {
			// update matching sources
			for (Style<?> c: current) {
				if (s.getName().equals(c.getName())) {
					c.setSource((StyleSource) s.getSource());
					break;
				}
			}
		}
		
		// get style fields before and after the changes were applied
		Set<Field> oldFields = new LinkedHashSet<Field>(stylesFC.getSchema());
		Set<Field> newFields = new LinkedHashSet<Field>();
		for (Style<?> s: current) {
			newFields.addAll(s.getSource().getFields());
		}
		
		// remove fields not still there
		for (Field f: oldFields) {
			if (!newFields.contains(f)) {
				stylesFC.removeField(f);
			}
		}
		
		// add fields that weren't there before
		for (Field f: newFields) {
			if (!oldFields.contains(f)) {
				stylesFC.addField(f);
			}
		}
		
		// If the geometrySource provided is a valid instance, override the new styles with the provided value
		if (geomSource instanceof GeomSource) {
			this.styles.geometry.setSource(geomSource);
		}
		
		// notify the listeners that these styles were changed
		broadcast(new StylesChange(changes));
	}
	
	public void broadcast(Object o) {
		super.broadcast(o);
	}
	
	public static final class StylesChange {
		public final Set<Style<?>> changes;
		public StylesChange(Set<Style<?>> changes) {
			this.changes = changes;
		}
	}

	public ObservableSet<Feature> getSelections() {
		return selections;
	}
	
	public MemoryFeatureIndex getIndex() {
		return index;
	}
	
	/**
	 * Track style columns as a separate collection, and keep that
	 * collection's columns all the way to the right.
	 */
	private static class StyleColumnPositioner implements ListSelectionListener, FeatureListener {
		private boolean busy = false;
		private final MultiFeatureCollection mfc;
		private final FeatureCollection stylesFC;
		public StyleColumnPositioner(MultiFeatureCollection mfc, FeatureCollection stylesFC) {
			this.mfc = mfc;
			this.stylesFC = stylesFC;
		}
		public void valueChanged(ListSelectionEvent e) {
			if (!e.getValueIsAdjusting()) {
				reinsertStyles();
			}
		}
		public void receive(FeatureEvent e) {
			switch (e.type) {
			case FeatureEvent.REMOVE_FIELD:
			case FeatureEvent.ADD_FIELD:
				reinsertStyles();
				break;
			}
		}
		private void reinsertStyles() {
			// removeFeatureCollection triggers a REMOVE_FIELD event, which
			// we want to ignore, so prevent reentrant processing
			if (!busy) {
				busy = true;
				// inserts styles at a later time, so the current table changes should be finished
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						try {
							if (mfc.getSupportingFeatureCollections().contains(stylesFC)) {
								mfc.removeFeatureCollection(stylesFC);
							}
							mfc.addFeatureCollection(stylesFC);
						} finally {
							busy = false;
						}
					}
				});
			}
		}
	}
	
	public void loadSources(final List<LoadData> sources) {
		loadSources(sources, new SourceAdder());
	}
	
	/**
	 * Processes FileLoad updates; as each change is reported, it gathers errors
	 * to show at the end, and adds successful loads right away. It will mark a
	 * history frame before making the first change.
	 */
	private class SourceAdder implements LoadListener {
		private boolean marked = false;
		public void receive(LoadData data) {
			if (data.fc != null) {
				if (!marked) {
					marked = true;
					getHistory().mark();
				}
				getFileTable().getFileTableModel().add(data.fc);
				int selectedFile = getFileTable().getFileTableModel().getRowCount()-1;
				getFileTable().getSelectionModel().setSelectionInterval(selectedFile,selectedFile);
			}
		}
	}
	
	/**
	 * Loads features in a thread pool, and puts the collection or error on the
	 * LoadData objects passed in.
	 * 
	 * The callback is called on the AWT thread each time the status of any
	 * LoadData changes, so the caller can do something with each file as it
	 * becomes ready.
	 * 
	 * As long as any files are still loading, the shape layer's status LED
	 * should show that a file IO operation is ongoing.
	 */
	public void loadSources(List<LoadData> sources, final LoadListener callback) {
		// Keep simultaneous shape layer reads between 1 and min(4, cores-1)
		// since more than one read per CPU core causes bad thrashing.
		int threads = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
		final ExecutorService pool = Executors.newFixedThreadPool(threads, new MapThreadFactory("ShapeLoader"));
		final List<String> errors = new ArrayList<String>();
		final List<Runnable> runnables = new ArrayList<Runnable>();
		for (final LoadData source: new ArrayList<LoadData>(sources)) {
			runnables.add(new Runnable() {
				public void run() {
					final ShapeLayer.LEDState led = new ShapeLayer.LEDStateFileIO();
					begin(led);
					try {
						source.fc = source.fp.load(source.data);
						if (source.fc != null) {
							source.fc.setProvider(source.fp);
							source.fc.setFilename(source.data == null ? source.fp.getDescription() : source.data);
						}
						if (source.fp.setAsDefaultFeatureCollection()) {
						    ShapeLayer.this.getFileTable().getFileTableModel().setDefaultFeatureCollection(source.fc);
						}
					} catch (final Exception e) {
						e.printStackTrace();
						synchronized(runnables) {
							String text = "";
							for (Throwable e2 = e; e2 != null; e2 = e2.getCause()) {
								if (e2.getMessage() != null) {
									text += "\n  " + e2.getMessage();
								}
							}
							String title = (source.data != null ? source.data : "");
							errors.add("Error loading " + title + ":" + text);
						}
					} finally {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								if (source.fc != null) {
									callback.receive(source);
								}
								end(led);
							}
						});
						
						synchronized(runnables) {
							runnables.remove(this);
							if (runnables.isEmpty()) {
								pool.shutdown();
								
								final String msg = Util.join("\n", errors);
								if (msg.length() > 0) {
									SwingUtilities.invokeLater(new Runnable() {
										public void run() {
											Util.showMessageDialog( msg,
												"Unable to load all files", JOptionPane.ERROR_MESSAGE);
										}
									});
								}
							}
						}
					}
				}
			});
		}
		synchronized(runnables) {
			for (Runnable r: runnables) {
				pool.execute(r);
			}
		}
	}
	
	interface LoadListener {
		void receive(LoadData data);
	}
	
	public static class LoadData {
		public final FeatureProvider fp;
		public final String data;
		public FeatureCollection fc;
		public LoadData(FeatureProvider fp, String data) {
			this.fp = fp;
			this.data = data;
		}
	}
	
	public void loadReadOnlyFile(String dir, String fileName, String URL){
		File f = new File(fileName);
		
		FeatureProvider fp = new FeatureProviderReadOnly(dir, fileName, URL);
    	final List<ShapeLayer.LoadData> sources = new ArrayList<ShapeLayer.LoadData>();
    	sources.add(new ShapeLayer.LoadData(fp, f.getAbsolutePath()));
    	
		loadSources(sources);
	}
	
	@Override
	public void provideSchema(ILayerSchemaProvider sb) {
		sb.doSchema(this);  //goes to Data.SERVICE
	}
	
}

