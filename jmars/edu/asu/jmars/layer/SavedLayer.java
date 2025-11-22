package edu.asu.jmars.layer;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.SerializableConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.util.Util;

/**
 * Saves the state of an LView so it can be restored later. The factory, layer
 * name, SerializedParameters, view parameters, alpha value, and which views are
 * toggled on are saved.
 */
public class SavedLayer implements Serializable {
	private static final long serialVersionUID = 8248185355815282401L;
	public final String factoryName;
	public final String layerName;
	public final SerializedParameters createParms;
	public final Hashtable<?,?> viewParms;
	public final float alpha;
	public final boolean showMain;
	public final boolean showPanner;
	public final String bodyName = Main.getBody();//adding a new field to be serialized by xstream will not be a problem when deserializing an older version without this field.
	public final String versionNumber = Util.getVersionNumber();
	public final boolean isOverlay;
	
	public SavedLayer(String factoryName, String layerName, SerializedParameters createParms, Hashtable<?,?> viewParms, float alpha, boolean showMain, boolean showPanner, boolean overlay) {
		this.factoryName = factoryName;
		this.layerName = layerName;
		this.createParms = createParms;
		this.viewParms = viewParms;
		this.alpha = alpha;
		this.showMain = showMain;
		this.showPanner = showPanner;
		this.isOverlay = overlay;
	}
	
	public SavedLayer(LView view) {

		this(
			view.originatingFactory.getClass().getName(),
			getName(view),
			view.getInitialLayerData(),
			view.getViewSettings(),
			view.getAlpha(),
			view.isVisible(),
			view.getChild() != null ? view.getChild().isVisible() : false,
			view.isOverlay());
	}

	/**
	 * Creates a new layer from this saved state, installs it directly into the
	 * currently running JMARS session, and then returns it
	 */
	public LView materialize(){
		return materialize(null);
	}
	
	public LView materialize(LayerParameters lp) {
		LViewFactory factory = LViewFactory.getFactoryObject(factoryName);
		if (factory == null) {
			throw new IllegalStateException("Cannot create layer from unknown factory " + factoryName);
		}
		
		LView lview = factory.recreateLView(createParms);
		if (lview == null) {
			throw new IllegalStateException("Error while creating layer " + layerName);
		}

		lview.setOverlayFlag(this.isOverlay);
		
		if(lp != null){
			lview.setLayerParameters(lp);
			//refresh the infopanel because it's possible it already populated this based
			// off other information from the jlf, and we really want to populate off the
			// information in the info editor, which is this layerparameters object (lp)
			lview.getFocusPanel().infoPanel.loadFields();
		}
		if (layerName != null && layerName.length() > 0 && !layerName.equals(lview.getName())) {
			Main.testDriver.getCustomLayerNames().put(lview, layerName);
		}
		
		//changing the order of operations to match what we do in TestDriverLayered. The main reason for this
		//is to have the setViewSettings call have the childLView already created. The childLView being null
		//was causing problems when loading saved layers in the planning layer. - Ken 6/29/12
		if (lview != null) {
			
			LManager.receiveSavedLView(lview);
			
			lview.setAlpha(alpha);
			lview.setVisible(showMain);
			if (lview.getChild() != null) {
				lview.getChild().setVisible(showPanner);
			}
			
			if (viewParms != null) {
				lview.setViewSettings(viewParms);
			}
			
			LManager.getLManager().updateVis();
		}
		lview.layerKey = layerName;
		return lview;
	}
	
	private static final String getName(LView view) {
		String name = Main.testDriver.getCustomLayerNames().get(view);
		return name != null && name.length() > 0 ? name : view.getName();
	}
	
	static class OurURIConverter extends SerializableConverter {
		public OurURIConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
			super(mapper, reflectionProvider);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean canConvert(Class type) {
			return type.equals(URI.class);
		}

		@Override
		public void marshal(Object original, HierarchicalStreamWriter writer, MarshallingContext context) {
			super.marshal(original, writer, context);
		}

		@Override
		public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
			return super.unmarshal(reader, context);
		}
		
	}
	
	private static final XStream xstream = new XStream() {
		@Override
		protected void setupConverters() {
			super.setupConverters();
			// NOTE: There may be a single line way of accomplishing the following two lines
			this.registerConverter(new OurURIConverter(getMapper(), getReflectionProvider()));
			this.alias(URI.class.getName(), URI.class);
		}
		protected boolean useXStream11XmlFriendlyMapper() {
			return true;
		}
	};
	
	/** Saves SavedLayer instances to the given OutputStream, encoding the Java objects as an XStream 1.1 stream. */
	public static void save(List<SavedLayer> layers, OutputStream os) {
		synchronized(xstream) {
			xstream.toXML(layers, os);
		}
	}
	
	/**
	 * Loads SavedLayer instances from the given InputStream, which should have
	 * been serialized using {@link SavedLayer#save(OutputStream)}.
	 */
	public static List<SavedLayer> load(InputStream is) {
		Object data;
		synchronized(xstream) {
			data = xstream.fromXML(is);
		}
		if (data instanceof SavedLayer) {
			return Arrays.asList((SavedLayer)data);
		}
		if (data instanceof List) {
			List<SavedLayer> layers = new ArrayList<SavedLayer>();
			for (Object o: (List<?>)data) {
				if (o instanceof SavedLayer) {
					layers.add((SavedLayer)o);
				}
			}
			return layers;
		}
		throw new IllegalArgumentException("File contains unrecognized data type " + data.getClass());
	}
}
