package edu.asu.jmars.layer;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.GENERIC_LAYER;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import edu.asu.jmars.layer.grid.GridLView;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView;
import edu.asu.jmars.layer.north.NorthLView;
import edu.asu.jmars.layer.scale.ScaleLView;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;


/**
 ** Encapsulates a mechanism for creating new LView objects, and for
 ** internally managing the layers they're linked to. Every factory
 ** must implement, at a minimum, {@link #createLView()}. Factories
 ** requiring user interaction for parameters must implement {@link
 ** #createLView(LViewFactory.Callback)} as well. Finally, most
 ** factories will want to implement {@link #getName} and {@link
 ** #getDesc} to be friendly to the user.
 **
 ** <p>To "add a layer to application", ultimately, requires only a
 ** new LViewFactory be added to {@link #factoryList}. This is the
 ** sole entry point that the application uses to query and create
 ** layers for the user. Typically, an addition to this list will also
 ** involve the subclassing of {@link LViewFactory}, {@link Layer},
 ** and {@link Layer.LView} for the new layer type. Strictly speaking,
 ** however, the application's "perception" of what layers are
 ** available is dictated solely through the <code>factoryList</code>.
 **
 ** <p>Because creation of a new layer/view often requires user
 ** interaction with a dialog, the creation can be done
 ** asynchronously. Clients of LViewFactory can either create a
 ** "default" version of a view (kind of a kludge to facilitate
 ** getting useful data from the start of the application)
 ** synchronously by invoking {@link #createLView()}. Or they can
 ** invoke asynchronous creation with user interaction by using {@link
 ** #createLView(LViewFactory.Callback)}.
 **
 ** <p>The default superclass implementation of the callback
 ** asynchronous version actually just calls the synchronous version,
 ** so simple factories don't have to implement both methods if they
 ** don't want/need to.
 **
 ** <p>Clients of LViewFactory will generally not instantiate
 ** LViewFactory objects directly... instead, the static {@link
 ** #factoryList} member will be used to locate and use a factory
 **/
public abstract class LViewFactory
 {
	private static DebugLog log = DebugLog.instance();
	private static Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
    private static final Icon layerTypeIcon = new ImageIcon(ImageFactory.createImage(GENERIC_LAYER.withDisplayColor(imgLayerColor)));		
    
    public static final String OVERLAY_ID_PROFILE = "profile";
    public static final String OVERLAY_ID_NOMENCLATURE = "nomenclature";
    public static final String OVERLAY_ID_GRID = "grid";
    public static final String OVERLAY_ID_NORTH = "north_arrow";
    public static final String OVERLAY_ID_SCALE = "scale_bar";

	public LViewFactory()
	 {
		this(null, null);
	 }

	private String name;
	private String desc;
	protected Icon layerIcon;
	
	protected LViewFactory(String name, String desc)
	 {
		this.name = name;
		this.desc = desc;
	 }

	/**
	 ** The main entry point for querying and using LViewFactory
	 ** objects. A global list of available LViewFactories that
	 ** clients can use to create LView objects.
	 **
	 ** <p>This list is immutable... any attempt to add or remove
	 ** elements will result in an
	 ** <code>UnsupportedOperationException</code> being thrown.
	 **
	 ** <p><i>Implementation note:</i> This list is populated from the
	 ** {@link Config config file}.
	 **/
	public static List<LViewFactory> factoryList;
	
	public static List<LViewFactory> cartographyList;

	/**
	 ** The main entry point for querying and using LViewFactory
	 ** objects in LManager2, which utilizes a tree structure of
	 ** factories. A global list of available LViewFactories <b>and
	 ** MultiFactories</b> that clients can use to create LView
	 ** objects.
	 **
	 ** <p>This list is immutable... any attempt to add or remove
	 ** elements will result in an
	 ** <code>UnsupportedOperationException</code> being thrown.
	 **
	 ** <p><i>Implementation note:</i> This list is populated from the
	 ** {@link Config config file}.
	 **/
	public static List factoryList2;

	// Fills factoryList properly, mostly by checking the config file.
	static
	 {
		//change bodies - the logic that was called once in a static block needs to now be able to be called when switching bodies
		refreshAllLViews();
	 }
	/**
	 * This method was created to refresh the LViews when switching bodies
	 * @since change bodies
	 */
	public static void refreshAllLViews() {

		//if we are calling this from change body, we will need to call the resetStoredData method on each factory that is in the list before we populate the lists again
		if (factoryList2 != null) {//will be null at startup and set when changing bodies
			for(Object tempObj : factoryList2) {
				if (tempObj instanceof LViewFactory) {
					LViewFactory tempLViewFactory = (LViewFactory) tempObj;
					tempLViewFactory.resetStoredData();
				}
				
			}
		}
		
		// Create the "real" lists of factories.
		List realList = new ArrayList();
		List realList2 = new ArrayList();
		List cartographyFactoryList = new ArrayList();

		// Add all the factories listed in the config file.
		for(int i=1; Config.get("factory."+i,null)!=null; i++)
		{
		    String line = Config.get("factory."+i);
			StringTokenizer tok = new StringTokenizer(line);
			String name = line;
			if(line.equals(""))
				break;

			try
			{
				String pname = tok.nextToken();
				String cname = tok.nextToken().replace('.','$');
				name = pname + '.' + cname;
				Object factory = Class.forName(name).newInstance();
				realList2.add(factory);
				if(factory instanceof LViewFactory)
					realList.add(factory);
				else
					((MultiFactory) factory).addDescendantsTo(realList);
			}
			catch(NotAvailableError e)
			{
				// TODO: Rather than throwing an exception in a static block during class load, 
				// we should implement something cleaner, such as an abstract 'isAvailable' method 
				// that can be called to determine if the layer should be available to the user based
				// on whatever parameters that layer cares about.

				// Stay silent. If the offending factory wants an
				// error message, it prints the message itself.
			}
			catch(ClassNotFoundException e)
			{
				log.aprintln("UNKNOWN LAYER TYPE IN CONFIG FILE: " +
						i + "=" + name);
			}
			catch(Throwable e)
			{
				log.aprintln("Layer type " + i + "=" + name +
						" unavailable, due to " + e);
				log.aprintln(e);
			}
		}
		
		// Add all the factories listed in the config file.
        for(int i=1; Config.get("cartography."+i,null)!=null; i++)
        {
            String line = Config.get("cartography."+i);
            StringTokenizer tok = new StringTokenizer(line);
            String name = line;
            if(line.equals(""))
                break;

            try
            {
                String pname = tok.nextToken();
                String cname = tok.nextToken().replace('.','$');
                name = pname + '.' + cname;
                Object factory = Class.forName(name).newInstance();
                cartographyFactoryList.add(factory);
            }
            catch(NotAvailableError e)
            {
                // TODO: Rather than throwing an exception in a static block during class load, 
                // we should implement something cleaner, such as an abstract 'isAvailable' method 
                // that can be called to determine if the layer should be available to the user based
                // on whatever parameters that layer cares about.

                // Stay silent. If the offending factory wants an
                // error message, it prints the message itself.
            }
            catch(ClassNotFoundException e)
            {
                log.aprintln("UNKNOWN CARTOGRAPHY LAYER TYPE IN CONFIG FILE: " +
                        i + "=" + name);
            }
            catch(Throwable e)
            {
                log.aprintln("Layer type " + i + "=" + name +
                        " unavailable, due to " + e);
                log.aprintln(e);
            }
        }
		

		// Point the factoryLists to immutable proxies to the
		// realLists.
        cartographyList = Collections.unmodifiableList(cartographyFactoryList);
		factoryList = Collections.unmodifiableList(realList);
		factoryList2 = Collections.unmodifiableList(realList2);
	}
	/**
	 * This is an optional method for factories to override if they have static data that will need to be updated when changing bodies.
	 * @since change bodies
	 */
	protected void resetStoredData() {
		//nothing at this level
	}
	
	/**
	 ** Indicates an {@link LViewFactory} is unavailable due to some
	 ** reason. Generally this would be caused by a missing library or
	 ** something.
	 **/
	public static class NotAvailableError extends Error
	 {
		/**
		 ** Apparently the default constructor is protected if we
		 ** don't explicitly declare one.
		 **/
		public NotAvailableError()
		 {
		 }
	 }

	/**
	 ** A simple listener mechanism for clients of {@link
	 ** LViewFactory} to use to receive created LViews. Probably
	 ** most-usefully implemented as an anonymous inner local class
	 ** within the same stack frame of the client that invokes {@link
	 ** LViewFactory#createLView(LViewFactory.Callback)}.
	 **
	 ** @see LViewFactory
	 **/
	static public interface Callback
	 {
		/**
		 ** When the potentially-asynchronous {@link
		 ** LViewFactory#createLView(LViewFactory.Callback)} method is
		 ** invoked, it returns a new view by calling this method once
		 ** on the callback it was passed.
		 **
		 ** @param newLView the newly-constructed view created a the
		 ** factory.
		 **/
		public void receiveNewLView(Layer.LView newLView);
	 }

	/**
	 ** Clients should invoke this function to trigger the creation of
	 ** a new LView (potentially) with user interaction. The creation
	 ** may be asynchronous (because it may involve dialog boxes and
	 ** other graphic elements), thus the created LView is returned
	 ** via a callback mechanism.
	 **
	 ** <p>Note: the default implementation of this method simply
	 ** invokes the synchronous version of {@link #createLView()} and
	 ** returns the result to the callback. Thus, simple factories
	 ** need not implement this method if they will never need user
	 ** interaction.
	 **
	 ** @param callback The created LView will be passed as a
	 ** parameter to <code>LManager.{@link LManager#receiveNewLView
	 ** receiveNewLView(newLView)}</code>.
	 **/
	public void createLView(boolean async, LayerParameters l)
	 {
		Layer.LView view = createLView();
		if(view != null){
			view.setLayerParameters(l);
			LManager.getLManager().receiveNewLView(view);
		}
	 }

	/**
	 * Used to populate the initial list of views on
	 * startup. Factories should implement this function to return an
	 * appropriate view with "default" parameters. Return null to
	 * prevent any view from being added to the initial list from
	 * this factory.
	 *
	 * @return the new LView, or null
	 */
	public Layer.LView createLView() {
		// @since change bodies
		// get the LView from subclasses
		Layer.LView view = this.showByDefault();
		if (view != null) {
			return  view;
		} else {
			return null;
		}
	}
	//doesn't feel like the right place for this. When I find a better place, I will refactor
	public static boolean isOverlayType(Layer.LView view) {
		boolean returnVal = false;
		if (view instanceof NomenclatureLView ||
            view instanceof GridLView ||
            view instanceof ScaleLView ||
            view instanceof NorthLView) {
	         returnVal = true;
		}
		return returnVal;
	}
	public Layer.LView showCartographyLView() {
	    return this.showDefaultCartographyLView();
	}
	
	public Layer.LView showDefaultCartographyLView() {
	    return null;//this is the default behavior
	}
	/**
	 * This method is to be overwritten by factories that want to load by default and have an entry in the config file
	 * @return String
	 * @since change bodies
	 * @see NomenclatureFactory.java
	 */
	public Layer.LView showByDefault() {
		return null;//this is the default behavior
	}


	/**
	 * Used to start a session using a serialized parameter block. Typically
     * after a session restart.
     * 
     * @return the new LView, or null
	 **/
	public abstract Layer.LView recreateLView(SerializedParameters parmBlock);


	/**
	 * Iterates through the list of valid factory objects and returns the
	 * one of the same name.
	 *
	 * @return the new LViewFactory, or null
	 */
	static public LViewFactory getFactoryObject(String className ) {
		Iterator iterFactory = factoryList.iterator();

		while(iterFactory.hasNext()) {
			LViewFactory lvf = (LViewFactory) iterFactory.next();
			if ( lvf.getClass().getName().compareTo(className) == 0 )
				return lvf;
		}
		
		//if we are here, we did not find a factory in the factoryList. 
		//check overlay factories to see if there is one that is not listed in
		//the original factory list (i.e. Profile Layer).
		iterFactory = cartographyList.iterator();
		
		while(iterFactory.hasNext()) {
			LViewFactory lvf = (LViewFactory) iterFactory.next();
			if ( lvf.getClass().getName().compareTo(className) == 0 )
				return lvf;
		}

		return null;
	}


	/**
	 ** Should be defined in implementing classes to be an appropriate
	 ** short name for this factory, acceptable for displaying in a
	 ** list box for a user to choose.
	 **
	 ** @return The default implementation simply returns the derived
	 ** class's name.
	 **/
	public String getName()
	 {
		return  name==null ? getClass().getName() : name;
	 }

	/**
	 ** Should be defined in implementing classes to be an appropriate
	 ** medium-sized description for this factory, acceptable for
	 ** displaying as a comment in a text area.
	 **
	 ** @return The default implementation simply returns the derived
	 ** class's name.
	 **/
	public String getDesc()
	 {
		return  desc==null ? getClass().getName() : desc;
	 }

	/**
	 ** The default implementation simply returns a menu item that
	 ** proxies to {@link #createLView(boolean)}. Sub-classes can
	 ** override this to create more sophisticated behavior (such as
	 ** returning an entire sub-menu).
	 **/
	protected JMenuItem[] createMenuItems() {
		return new JMenuItem[]{new JMenuItem(new AbstractAction(getName()) {
			public void actionPerformed(ActionEvent e) {
				try {
					createLView(true, null);
				}
				catch(Exception ex){
					log.aprintln(ex);
					Util.showMessageDialog(ex.getMessage(),
							"Error creating \""+getName()+"\" layer.", JOptionPane.ERROR_MESSAGE);
				}
			}
		})};
	 }
	
	

	//Used in AddLayer to add the layer
	public String type;
	/**
	 ** Used in AddLayer.  Takes in a keyword and returns the proper
	 ** factory (TES, LROC, groundtrack, etc) based off the keyword,
	 ** and then AddLayer uses that to add the new layer.
	 */
	public static LViewFactory findFactoryType(String type){
		for (LViewFactory f : factoryList){
			if (type.equals(f.type)){
				return f;
			}
		}
		return null;
	}
	
	 public Icon getLayerIcon()
	 {
	    return layerTypeIcon;
	 }
	 
	 
	 public static Icon getDefaultLayerIcon()
     {
        return layerTypeIcon;
     }
	
 }
