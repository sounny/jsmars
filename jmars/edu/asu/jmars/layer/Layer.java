package edu.asu.jmars.layer;


import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;

import edu.asu.jmars.LoginWindow2;
import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.swing.AncestorAdapter;
import edu.asu.jmars.swing.ColorMapOp;
import edu.asu.jmars.swing.FancyColorMapper;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;
import edu.asu.jmars.viz3d.renderer.gl.event.IntersectResult;
import edu.asu.jmars.viz3d.renderer.textures.DecalSet;
import fr.lri.swingstates.applets.ColorEvent;
import fr.lri.swingstates.sm.State;
import fr.lri.swingstates.sm.StateMachine;
import fr.lri.swingstates.sm.Transition;
import fr.lri.swingstates.sm.transitions.Event;

/**
 ** The abstract superclass for all Layers, which implements the
 ** mechanisms common to all Layers and provides the hooks needed by
 ** all Layers. A Layer subclass must (at a minimum) implement {@link
 ** #receiveRequest receiveRequest}.
 **
 ** <p>Layer objects serve as data sources for multiple {@link
 ** DataReceiver} objects, most of which are generally subclasses of
 ** {@link Layer.LView}. DataReceiver objects request data from the
 ** layer by invoking the Layer's {@link #receiveRequest
 ** receiveRequest} method. The Layer may then respond synchronously
 ** and/or asynchronously by sending data back. The policy to
 ** determine which data/requests are synchronous and which are
 ** asynchronous is set at the sublcass level. The base Layer doesn't
 ** include any implementation of such mechanisms.
 **
 ** <p>Layer objects maintain a list of registered
 ** DataReceivers. Along with the ability to respond directly to
 ** requesting DataReceivers, subclasses also have the option of
 ** sending data to every registered DataReceiver using the {@link
 ** #broadcast method} method of Layer.
 **
 ** <p>Layer objects are constructed by {@link LViewFactory}
 ** implementations. In order to add a "layer type" to the
 ** application, you must subclass an LViewFactory to create and
 ** manage objects of your Layer subclass; see the LViewFactory
 ** documentation for further details.
 **
 ** <p>Projection information for world/spatial must be gotten from
 ** {@link Main#PO}.
 **
 ** @see DataReceiver
 ** @see Layer.LView
 ** @see LViewFactory
 **/
public abstract class Layer implements ProjectionListener
 {
	
	/**
	 * A list of ids used for refreshing the 3D view.
	 * The length of the array should be equal to the
	 * number of buffers (decal sets) used by the 
	 * LView3D. They should be updated whenever data
	 * in the layer changes and should cause a 3D redraw.
	 */
	protected ArrayList<Integer> stateIds;
	
	/**
	 * The list of state ids used for refreshing the 3D
	 * view.  There should be one id for each decal set
	 * used by the layer.
	 * @return Arraylist of state ids for redraw purposes
	 */
	public ArrayList<Integer> getStateIds(){
		if(stateIds == null){
			stateIds = new ArrayList<Integer>();
			stateIds.add(0);
		}
		return stateIds;
	}
	
	/**
	 * Get the specific state id of a given buffer, based
	 * on the parameter passed in.
	 * @param bufferNum The index of the buffer of interest
	 * @return The state id, will return -1 if the bufferNum is
	 * out of bounds of the arrayList
	 */
	public int getStateId(int bufferNum){
		if(bufferNum < stateIds.size()){
			return stateIds.get(bufferNum);
		}
		return -1;
	}
	
	/**
	 * Increases the state id of the buffer that at the index
	 * that is passed in.  Does not do anything if the index
	 * is out of bounds of the stateIds list.
	 * @param bufferNum  Index to increase state id of
	 */
	public void increaseStateId(int bufferNum){
		if(bufferNum < stateIds.size()){
			int id = stateIds.get(bufferNum);
			stateIds.set(bufferNum, ++id);
		}
	}
	
	private static DebugLog log = DebugLog.instance();

	/**
	 ** Responds to requests from a DataReceiver by returning
	 ** data. Implementations of Layer subclasses may return data
	 ** directly to the requester by invoking <code>requester.{@link
	 ** DataReceiver#receiveData receiveData}</code>. You will note
	 ** that both the request and the returned data are passed as
	 ** <@link Object>s; this allows subclasses to pass any object
	 ** they wish as requests and data.
	 **
	 ** <p>Some subclasses may wish to queue requests or data and
	 ** process one or both asynchronously. Such mechanisms must be
	 ** implemented by the subclass directly; the superclass provides
	 ** no such functionality, but doesn't prevent it either.
	 **
	 ** <p>Some subclasses may wish to send some data out to all
	 ** registered DataReceivers. See {@link #broadcast}.
	 **
	 ** @param layerRequest A subclass-determined object that's
	 ** processed as a request. It is recommended that subclass do
	 ** <code>instanceof</code> tests to verify the type of the
	 ** object, and that they explicitly handle an unknown object type
	 ** by outputting a diagnostic and ignoring the request.
	 ** @param requester Generally the callee of the function. Most
	 ** layers will respond to requests by feeding data back to
	 ** <code>requester</code>.
	 **/
	public abstract void receiveRequest(Object layerRequest,
										DataReceiver requester);
	/**
	 ** PRIVATE: Stores the set of registered LViews.
	 **/
	private HashSet allDataReceivers = new HashSet();

	/**
	 ** Implements ProjectionListener (to do nothing).
	 **/
	public void projectionChanged(ProjectionEvent e)
	 {
	 }

	/**
	 ** Return an Iterator which iterates over ALL DataReceiver.
	 ** Needed by anthing which needs to check with all the
	 ** receivers
	 **/
	protected final Iterator getReceivers()
	 {
		synchronized (allDataReceivers)
		{
			Iterator itr =  (((Set)(allDataReceivers.clone())).iterator());
			return(itr);
		}
	 }

	private final Object idleNotification = new Object();
	private final Object busyLock = new Object();
	private int busyCount = 0;
	
	/**
	 * Returns the schema data needed for use by 'global' components, e.g. Drawing palette or Expression Builder.
	 * This method provides a default implementation, which does nothing. Only subclasses of Layer that are asked to provide
	 * their respective schema will override this method.
	 * Using Visitor pattern, a Layer subclass can just provide its instance to the common SchemaBuilder implementor.
	 * for ex., see ShapeLayer.accept method or StampLayer.accept method
	 */
	public void provideSchema(ILayerSchemaProvider sb) {
	}

	/**
	 ** Suspends the current thread until the layer is no longer
	 ** idle. Note that the thread will therefore lose all of its
	 ** current monitors in that case, so care must be taken to leave
	 ** multi-thread-safe data in a consistent state before calling
	 ** this method.
	 **/
	public final void waitForIdle()
	 {
		try
		 {
			// The busyLock sync prevents the busy status from
			// changing between the while() loop test and the while()
			// loop body.
			synchronized(busyLock)
			 {
				synchronized(idleNotification)
				 {
					while(isBusy())
						idleNotification.wait();
				 }
			 }
		 }
		catch(InterruptedException e)
		 {
			log.aprintln("UH-OH... SOMETHING WENT WRONG!");
			log.aprintln(e);
		 }
	 }

	/**
	 ** Optionally method to flag times when the idle state may have
	 ** changed.
	 **/
	public final void notifyForIdle()
	 {
		synchronized(busyLock)
		 {
			if(!isBusy())
				synchronized(idleNotification)
				 {
					idleNotification.notifyAll();
				 }
		 }
	 }

	/**
	 ** Decrements the busy count. Subclasses can, if they wish, use
	 ** this method to manage the "standard" busy status that {@link
	 ** #isBusy} returns.
	 **/
	protected final void incBusy()
	 {
		synchronized(busyLock)
		 {
			if(++busyCount < 0)
			 {
				log.aprintln("----------------------------------");
				log.aprintln("--- SOMETHING'S WAAAY BROKEN.  ---");
				log.aprintln("- TELL MICHAEL: HIGH BUSY COUNT! -");
				log.aprintln("----------------------------------");
				log.aprintStack(6);
				busyCount = Integer.MAX_VALUE;
			 }
		 }
	 }

	/**
	 ** Decrements the busy count. Subclasses can, if they wish, use
	 ** this method to manage the "standard" busy status that {@link
	 ** #isBusy} returns.
	 **/
	protected final void decBusy()
	 {
		synchronized(busyLock)
		 {
			if(--busyCount < 0)
			 {
				log.aprintln("---------------------------------");
				log.aprintln("--- SOMETHING'S WAAAY BROKEN. ---");
				log.aprintln("- TELL MICHAEL: LOW BUSY COUNT! -");
				log.aprintln("---------------------------------");
				log.aprintStack(6);
				busyCount = 0;
			 }
			if(busyCount == 0)
				synchronized(idleNotification)
				 {
					idleNotification.notifyAll();
				 }
		 }
	 }

	/**
	 ** Non-blocking call to determine if the layer is currently busy.
	 **/
	public boolean isBusy()
	 {
		synchronized(busyLock)
		 {
			return  busyCount != 0;
		 }
	 }

	// Defined statically here because it's also referenced in LView
	private static final Color INITIAL_STATUS_COLOR = Util.darkGreen;

	private Color currentStatus = INITIAL_STATUS_COLOR;
	/**
	 ** Returns the color last set with {@link #setStatus}. If no
	 ** status has been set, the default initial status green will be
	 ** returned.
	 **/
	public final Color getStatus()
	 {
		return  currentStatus;
	 }

	/**
	 ** For EVERY registered {@link DataReceiver} that's a {@link
	 ** Layer.LView}, sets its status color.  Warning: multi-threaded
	 ** calls to setStatus with different colors may result in views
	 ** displaying different colors (not just out-of-order identical
	 ** colors).
	 **/
	public final void setStatus(Color col)
	 {
		Iterator iter = getReceivers();
		while(iter.hasNext())
		 {
			Object obj = iter.next();
			if(obj instanceof LView)
				((LView) obj).privateSetStatus(col);
		 }
/**
   BENCHMARKING CODE FOR THE REPORT ON JET'S PERFORMANCE

		String c = col.toString();
		if     (col == Color.red     ) c = "red";
		else if(col == Color.yellow  ) c = "yellow";
		else if(col == Color.pink    ) c = "pink";
		else if(col == Util.darkGreen) c = "green";
		System.out.println(System.currentTimeMillis() + "\t" +
				   getClass().getName() + " status: " + c);
**/
	 }



        /**
         * A layer can be started with a set of parameters that will be saved to restart the
         * session using the same parameters
         */
        public SerializedParameters initialLayerData = null;


	/**
	 ** Sends data out to every registered {@link DataReceiver}. The
	 ** format and class type of data is determined by the
	 ** subclass. Note that the same reference is passed to all
	 ** registered receivers... no copies of the object are made.
	 **
	 ** @see #registerDataReceiver
	 ** @see #unregisterDataReceiver
	 **
	 ** @param layerData passed to the {@link
	 ** DataReceiver#receiveData} method of every registered receiver.
	 **/
	protected void broadcast(Object layerData)
	 {
		Iterator iter = getReceivers();
		while(iter.hasNext())
			( (DataReceiver) iter.next() ).receiveData(layerData);
	 }

	/**
	 ** Registers a DataReceiver to receive broadcast data. Note that
	 ** {@link Layer.LView} subclasses need not register their
	 ** objects; they are automatically registered in the superclass's
	 ** constructor.
	 **
	 ** @see #unregisterDataReceiver
	 ** @see #broadcast
	 **
	 ** @param receiver A DataReceiver that will receive future
	 ** broadcast data. Redundant registrations are ignored but cause
	 ** a diagnostic message on the console.
	 **/
	public void registerDataReceiver(DataReceiver receiver)
	 {
		synchronized(allDataReceivers)
		 {
			boolean firstOne = allDataReceivers.isEmpty();
			if(!allDataReceivers.add(receiver))
				// as of 01/22/2021, this should not be seen as an error. 
				log.println("PROGRAMMER ignore as of 01/22/2021: REDUNDANT RECEIVER ADD (" + getClass().getName() + ")");
			else
				log.println("Added Receiver: "+receiver);
			if(firstOne)
				Main.addProjectionListener(this);
		 }
	 }

	/**
	 ** Unregisters a DataReceiver previously registered by {@link
	 ** #registerDataReceiver}.
	 **
	 ** @param receiver A DataReceiver that will no longer receive
	 ** future broadcast data. If receiver was never registered, there
	 ** is no effect except that a diagnostic message is printed on
	 ** the console.
	 **/
	public void unregisterDataReceiver(DataReceiver receiver)
	 {
		synchronized(allDataReceivers)
		{
			if(!allDataReceivers.remove(receiver))
				log.aprintln("PROGRAMMER: REDUNDANT RECEIVER REMOVE (" +
						 getClass().getName() + ")");
			if(allDataReceivers.isEmpty())
				Main.removeProjectionListener(this);
		}
	 }

	/**
	 ** The abstract superclass for all LViews, which implements
	 ** the mechanims common to all LViews and provides the hooks
	 ** needed by all LViews. An LView subclass must (at a minimum)
	 ** implement {@link #_new _new}, {@link #createRequest
	 ** createRequest}, and {@link #receiveData receiveData}.
	 **
	 ** <p>LView objects are Swing components that represent data for
	 ** a rectangular region of the world coordinate space, as pulled
	 ** from a {@link Layer} subclass. The base LView class provides
	 ** basic functionality for automatically repainting properly, and
	 ** for triggering various events when user interaction causes the
	 ** intended viewing rectangle of the LView to change.
	 **
	 ** <p>LView data is painted to an off-screen buffer to speed
	 ** repaints. Access to on-screen graphics contexts are provided
	 ** for immediate user interaction that some LView subclasses
	 ** require. All off-screen painting is performed in world
	 ** coordinates. The on-screen painting can be performed in world
	 ** or screen coordinates, depending on whether the graphics
	 ** context is requested from the LView itself or from the Swing
	 ** methods it inherits.
	 **
	 ** <p>In general, LView objects are only functional when they are
	 ** added to an {@link LViewManager} object.
	 **
	 ** <p>LView objects are normally not constructed directly, but
	 ** rather are created through a {@link LViewFactory}
	 ** subclass. Mechanisms there can take care of managing the Layer
	 ** associated with one or more LView objects.
	 **
	 ** <p>In order to implement panning context windows, an LView may
	 ** have one or none "child" LViews linked to it. When
	 ** implementing custom functionality outside of the data/request
	 ** model of message passing, it may be necessary to proxy
	 ** attribute changes in the LView to its child, if it has
	 ** one. Such policy is determined by the subclass. Subclasses
	 ** wishing to proxy some attributes should also pay attention to
	 ** the {@link #dup} method.
	 **
	 ** <p>Projection information for screen/world is held in the
	 ** {@link #viewman}. Projection information for world/spatial
	 ** must be gotten from {@link Main#PO}.
	 **
	 ** <p>Note: LView is a static inner class of Layer, only to
	 ** accomplish some quick namespace kludges. Eventually we'll have
	 ** all these classes in packages, which will eliminate the need
	 ** to do this. Ignore the fact that LView is an inner class of
	 ** Layer... it has no effect except on the way you must reference
	 ** its name.
	 **
	 ** @see Layer
	 ** @see LViewFactory
	 ** @see LViewManager
	 **/
	public static abstract class LView
	 extends JPanel
	 implements DataReceiver
	 {
		private Layer layer;
		Color layerloadlight;
		
		private boolean overlayFlag;
		private boolean overlaySetFlag;
		private String overlayId;
		
		private LView3D lview3d;
	
		/**
		 ** Sets the number of off-screen buffers that are maintained.
		 ** All buffers are cleared, but this call does NOT issue any
		 ** type of repaint (or viewchange) request, that's up to the
		 ** caller. Buffers are added/removed from the end of the
		 ** list.
		 **/
	    protected void setBufferCount(int newCount)
		 {
			bufferCount = newCount;
			buffers = null;
			bufferHidden = new boolean[newCount];
		 }

		/**
		 ** Returns the number of off-screen buffers that are
		 ** maintained. Starts out with a default of one.
		 **/
		protected int getBufferCount()
		 {
			return  bufferCount;
		 }

		/**
		 ** Allows selective showing/hiding of specific off-screen
		 ** buffers.
		 **/
		protected void setBufferVisible(int i, boolean visible)
		 {
			bufferHidden[i] = !visible;
		 }

		/**
		 ** Queries whether a specific off-screen buffer is currently
		 ** visible.
		 **/
		protected boolean getBufferVisible(int i)
		 {
			return  !bufferHidden[i];
		 }

		/**
		 ** Returns the layer associated with this LView. Generally,
		 ** LView subclasses will pull all their data from a Layer.
		 **/
		public Layer getLayer()
		 {
			return  layer;
		 }

        /**
         ** Sets the layer associated with this LView. Generally,
         ** LView subclasses will pull all their data from a {@link Layer}.
         ** <p>
         ** This method also unregisters this LView from its previous
         ** layer (if any) as a data receiver, and registers it with
         ** the new layer (if non-null).
         ** <p>
         ** NOTE: Use this method with caution!!  In general, it is
         ** best to only set the layer via an {@link LView} constructor;
         ** however there are some exceptional situations which may
         ** be best addressed by this method, e.g., changing the layer
         ** associated with a view later on.
         **/
        protected final void setLayer(Layer layer)
        {
            setLayer(layer, false);
        }

        /**
         * Sets layer associated with this LView.
         * 
         * @param layer New {@link Layer} reference; may be <code>null</code>.
         * 
         * @param ignoreRegistration If <code>true</code>, then registration
         * of view with layer (old or new) as data receiver is ignored (this
         * is probably only safe/necessary in a constructor);  if <code>false</code>,
         * then view is deregistered with any old layer and is registered with
         * the new layer.
         */
        private final void setLayer(Layer layer, boolean ignoreRegistration)
        {
            // Unregister view as data receiver from previous layer.
            if (this.layer != null &&
                !ignoreRegistration)
                this.layer.unregisterDataReceiver(this);
            
            this.layer = layer;
            if (layer == null)
                idleNotification = new Object();
            else {
                idleNotification = layer.idleNotification;
                if (!ignoreRegistration)
                    layer.registerDataReceiver(this);
            }
        }
		/**
		 ** The number of off-screen buffers maintained. Defaults to
		 ** one.
		 **/
		private int bufferCount = 1;

		/**
		 ** The status of each off-screen buffer... this allows us to
		 ** turn on/off some of them selectively.
		 **/
		private boolean[] bufferHidden = { false };

		/**
		 ** An array of all the off-screen buffers. May be null when
		 ** no buffers have been initialized.
		 **/
		private BufferedImage[] buffers;

		/**
		 ** Invokes {@link BufferedImage#flush} on all off-screen
		 ** buffers, used for memory optimization purposes.
		 **/
		void flushImages()
		 {
			if(buffers != null)
				for(int i=0; i<buffers.length; i++)
					buffers[i].flush();
		 }

        /**
         * Returns whether or not the default state when a view is
         * added to the {@link LManager} is for it to be enabled in
         * the Main Window.
         * <p>
         * The default implementation returns <code>true</code>.
         * Subclasses should override if they need different behavior. 
         */
        public boolean mainStartEnabled()
        {
            return true;
        }
        
        /**
         * Returns whether or not the default state when a view is
         * added to the {@link LManager} is for it to be enabled in
         * the Panner Window.
         * <p>
         * The default implementation returns <code>true</code>.
         * Subclasses should override if they need different behavior. 
         */
        public boolean pannerStartEnabled()
        {
            return true;
        }
        
		/**
		 ** Is intended to indicate whether or not to display tooltips
		 ** for this view.
		 **/
		private boolean tooltipsDisabled = false;
		public boolean tooltipsDisabled()
		 {
			return  tooltipsDisabled;
		 }
		public void tooltipsDisabled(boolean tooltipsDisabled)
		 {
			this.tooltipsDisabled = tooltipsDisabled;
			if(getChild() != null)
				getChild().tooltipsDisabled = tooltipsDisabled;
		 }

		/**
		 ** PRIVATE: Flags whether or not we need to viewChanged()
		 ** next time we become visible.
		 **/
		private boolean dirty = true;

		public void setDirty(boolean dirt)
		{
			dirty=dirt;
		}

		/**
		 ** Returns a FancyColorMapper whose 'auto' function is backed
		 ** by this layer's zeroth off-screen image.
		 **/
		public FancyColorMapper createFancyColorMapper()
		 {
			return
				new FancyColorMapper()
				 {
					protected BufferedImage getImageForAuto()
					 {
						return  buffers[0];
					 }
				 }
				;
		 }

		/**
		 * Contains a pointer to the "child" of this view, if one exists.
		 * Typically a child is a context view contained in a panner.
		 */
		private LView childLView = null;

		/**
		 ** Inverse of the {@link #child}, see {@link #getParentLView}.
		 **/
		private LView parentLView = null;

		/**
		 ** The parent Swing container of this LView, also the logical
		 ** source of numerous projection and view-window
		 ** parameters. It controls our physical on-screen size and
		 ** our logical world window size. Please don't change
		 ** it... managed internally by the base LView class. Needs an
		 ** accessor method, currently just a member variable as a
		 ** kludge.
		 **/
		public LViewManager viewman;
		
		/**
		 ** The focusPanel is one of many derived focus panels. Each
		 ** LView will have their own and it's internal components
		 ** will vary depending on the functional need of a given
		 ** view.  They will all, however, be derived from the
		 ** FocusPanel base class.  The view will return a reference
		 ** to it's panel with a call to {@link #getFocusPanel}. Ben's
		 ** LManager stuff.
		 **/
		public FocusPanel focusPanel = null;


		/**
		 * Simply keeps a pointer to the origination class factory which instantiated
		 * this view.
		 */
		public LViewFactory originatingFactory = null;

		/**
		 * PRIVATE: Controls the alpha-transparency of the LView. A value of
		 * 1.0 indicates fully opaque, while 0.0 is fully transparent.
		 */
		private float alpha = 1.0f;

		private Object idleNotification; // initialized in constructor
		private final Object busyLock = new Object();
		private int busyCount = 0;



		/**
		 ** This function is called by the (@link LManager) to test
       ** if the unSavedFlag has been set.  If it has, this function
       ** calls a validation function to confirm the user's desire to delete this LView.
		 ** If the unSavedFlag is NOT set, the (@link LManager) simply removes this LView.
		 ** Returning TRUE will cause this LView to be removed, returning FALSE
		 ** will abort the deletion.
       **/
		 private boolean unSavedData = false;
		 		 
		/**
		 * Can be overridden by individual Layers to return a dynamic value based on that
		 * Layer's specific rules.
		 **/ 		 
		 public boolean hasUnsavedData() {
			 return unSavedData;
		 }
		 
		 public boolean deleted()
		 {
				if (hasUnsavedData()) 	
					return (validateDeletion());
				else 					
					return(true);
		 }

		 private boolean validateDeletion() {
			return JOptionPane.YES_OPTION == Util.showConfirmDialog(
				"The layer you want to delete has UNSAVED information\n Do you still want to delete it?",
				"Confirm Delete", JOptionPane.YES_NO_OPTION);
		}
		
		 public void changeBody() {
			 if (hasUnsavedData()) {
				 displayUnsavedDataWarning();
			 }
		 }
		
		 private void displayUnsavedDataWarning() {
			 Util.showMessageDialog("There is UNSAVED information in one of the current layers. This information\n will be saved in a layers file and reloaded next time you" +
						" switch\n to this body. However, it is not saved in an external layer specific\n file (i.e. shape file for the shape layer). If you want to preserve\n" +
						" these changes, save the information the next time you switch back\n to this body. ", 
						"Unsaved Information", JOptionPane.INFORMATION_MESSAGE);
		 }
		 
		 public boolean loadSession() {
			 if (hasUnsavedData()) {
				 return displayLoadSessionUnsavedDataWarning();
			 } else {
				 return false;//false in this case would mean the same as hitting "yes" to continue
			 }
		 }
		 
		 private boolean displayLoadSessionUnsavedDataWarning() {
			 //returning false means that they selected yes, they want to continue. We are checking for no
			 return JOptionPane.NO_OPTION == Util.showConfirmDialog(
					 	"There is UNSAVED information in one of the current layers.\n Do you still want to load another session?. ", 
						"Confirm Load Session", JOptionPane.YES_NO_OPTION);
		 }
		 
		/**
		 ** Suspends the current thread until the view is no longer
		 ** idle. Note that the thread will therefore lose all of its
		 ** current monitors in that case, so care must be taken to leave
		 ** multi-thread-safe data in a consistent state before calling
		 ** this method. Includes the layer, if there is one.
		 **/
		public void waitForIdle()
		 {
			try
			 {
				// The busyLock sync prevents the busy status from
				// changing between the while() loop test and the
				// while() loop body.
				synchronized(busyLock)
				 {
					synchronized(idleNotification)
					 {
						while(isBusy())
							idleNotification.wait();
					 }
				 }
			 }
			catch(InterruptedException e)
			 {
				log.aprintln("UH-OH... SOMETHING WENT WRONG!");
				log.aprintln(e);
			 }
		 }

		/**
		 ** Decrements the busy count. Subclasses can, if they wish, use
		 ** this method to manage the "standard" busy status that {@link
		 ** #isBusy} returns.
		 **/
		protected final void incBusy()
		 {
			synchronized(busyLock)
			 {
				if(++busyCount < 0)
				 {
					log.aprintln("----------------------------------");
					log.aprintln("--- SOMETHING'S WAAAY BROKEN.  ---");
					log.aprintln("- TELL MICHAEL: HIGH BUSY COUNT! -");
					log.aprintln("----------------------------------");
					log.aprintStack(6);
					busyCount = Integer.MAX_VALUE;
				 }
			 }
		 }

		/**
		 ** Decrements the busy count. Subclasses can, if they wish, use
		 ** this method to manage the "standard" busy status that {@link
		 ** #isBusy} returns.
		 **/
		protected final void decBusy()
		 {
			synchronized(busyLock)
			 {
				if(--busyCount < 0)
				 {
					log.aprintln("---------------------------------");
					log.aprintln("--- SOMETHING'S WAAAY BROKEN. ---");
					log.aprintln("- TELL MICHAEL: LOW BUSY COUNT! -");
					log.aprintln("---------------------------------");
					log.aprintStack(6);
					busyCount = 0;
				 }
				if(busyCount == 0)
					synchronized(idleNotification)
					 {
						idleNotification.notifyAll();
					 }
			 }
		 }

		/**
		 ** Non-blocking call to determine if the view is currently
		 ** busy. Includes the layer if there is one.
		 **/
		public boolean isBusy()
		 {
			if(layer != null)
				if(layer.isBusy())
					return  true;
			synchronized(busyLock)
			 {
				return  busyCount != 0;
			 }
		 }


                /**
                 * Stores the objects which represent any current view settings
                 * in order to save/restore state.
                 **/
                protected Hashtable<String,Object> viewSettings = new Hashtable<String,Object>();

                /**
                 * Returns the current setting for this view
                 * in order to save/restore state. Override for view specific stuff.
                 **/
                public Hashtable<String,Object> getViewSettings() {

                   //Process common LView settings to be saved.
                   viewSettings.clear();
                   viewSettings.put("alpha", new Float(alpha));
				   viewSettings.put("tooltipsDisabled", new Boolean(tooltipsDisabled));
				   viewSettings.put("mainEnabled", new Boolean(isVisible()));
				   if (getChild() != null)
					   viewSettings.put("pannerEnabled", new Boolean(getChild().isVisible()));

				   viewSettings.put("3dEnabled", new Boolean(getLView3D().isVisible()));
				   
				   if (this.focusPanel != null) {
				       viewSettings.put("docked", new Boolean(getFocusPanel().isDocked()));
	                   viewSettings.put("selectedIndex", getFocusPanel().getSelectedIndex());
				   }
				   
				   
                   //call to update subclass specific settings
                   updateSettings(true);

                   return  viewSettings;
                }

                /**
                 * Override to update current settings
                 */
                public void setViewSettings(Hashtable parms)
                {
                   if ( parms == null )
                    return;

                   viewSettings = parms;

                   //Process common LView settings to be saved.
                   Float f = (Float) viewSettings.get("alpha");
                   if ( f != null ){
                	   alpha = f.floatValue();
                	   getLView3D().setAlpha(alpha);
                   }

				   Boolean b = (Boolean) viewSettings.get("tooltipsDisabled");
				   if(b != null)
					   tooltipsDisabled = b.booleanValue();
				   
				   b = (Boolean)viewSettings.get("mainEnabled");
				   if (b != null)
					   setVisible(b.booleanValue());
				   
				   b = (Boolean)viewSettings.get("pannerEnabled");
				   if (b != null && getChild() != null)
					   getChild().setVisible(b.booleanValue());
				   
				   b = (Boolean)viewSettings.get("3dEnabled");
				   if (b !=null){
					   getLView3D().setVisible(b.booleanValue());
					   
				   }
				   
				   b = (Boolean)viewSettings.get("docked");
				   if (b !=null && b)
					   getFocusPanel().dock(false);
				   
				   Integer i = (Integer)viewSettings.get("selectedIndex");
				   if ( i !=null ) {
					   if (i.intValue() < getFocusPanel().getTabCount()) {
						   getFocusPanel().setSelectedIndex(i.intValue());
					   } 
				   }
				   
                  //call to update subclass specific settings
                   updateSettings(false);
                }

                /**
                 * Subclasses should implement to update view specific settings
                 */
		protected void updateSettings(boolean saving)
        	{

		}

                /**
                 * Returns the originating parameters for this view
                 * in order to save/restore state. Override for view specific stuff.
                 **/
                public SerializedParameters getInitialLayerData() {

                   if ( getLayer() == null )
                    return null;

                   return getLayer().initialLayerData;
                }


		/**
		 ** Retrieves the alpha setting for the view, which controls
		 ** the alpha-transparency of its drawn contents.
		 **
		 ** @return The current alpha, as last set by setAlpha(), or
		 ** the default alpha of 1.0 (opaque) if no alpha has ever
		 ** been explicitly set for this view.
		 **/
		public float getAlpha()
		 {
			return  this.alpha;
		 }


		/**
		 ** <p>Changes the alpha setting for the view, which controls
		 ** the alpha-transparency of its drawn contents.
		 **
		 ** @param alpha A value of 1.0 indicates fully opaque, while
		 ** 0.0 is fully transparent.
		 **/
		public void setAlpha(float alpha)
		 {
			if(getChild() != null)
				getChild().setAlpha(alpha);
			this.alpha = alpha;
			repaint();
		 }

		/**
		 ** Declared here only for scoping/access purposes. This
		 ** method also needs "package" scope, and re-declaring it
		 ** with protected gives it that. Simply inheriting it with
		 ** protected, without redeclaring it, wouldn't give us
		 ** package scope with respect to LView; it would merely
		 ** inherit package scope with respect to JPanel.
		 **/
		protected void processEvent(AWTEvent e)
		 {
			super.processEvent(e);
		 }

		/**
		 * @return True if this mouse event should be handled by this
		 * LView. This base class implementation will return true if
		 * it's the main view and false if it's the panner view. Subclasses
		 * can override this method to provide extra mouse capabilities on
		 * the panner.
		 */
		public boolean handleMouseEvent(MouseEvent e) {
			return getChild() != null;
		}

		/**
		 ** Checks if this view is alive. Only live views should be
		 ** drawn to, and only live views should respond to events. A
		 ** view can be drawn to so long as it is visible, has
		 ** non-zero pixel dimensions, has been added to an {@link
		 ** LViewManager}, and has a valid projection.
		 **/
		public final boolean isAlive()
		 {
			return  getWidth() != 0
				&&  getHeight() != 0
				&&  isVisible()
				&&  getProj() != null
				&&  buffers != null;
		 }


                /**
                 * Views should override this method in order to display specific tooltips.
                 */
                public String getToolTipText(MouseEvent event) {

                  return "";

                }

                /**
                 * Overridden by individual views, returns information for formatting for 
                 * Investigate mode while hovering with the cursor.
                 */
                public InvestigateData getInvestigateData(MouseEvent event) {
                	return null;
                }


		/**
		 * Returns a context menu for a user click at a particular
		 * world coordinate that will be positioned at the top of the menu.
                 *
                 * Subclass for view specific menu items
		 **/
		protected Component[] getContextMenuTop(Point2D worldPt)
                {

                  return new Component[0];

                }

		/**
		 * Returns a context menu for a user click at a particular
		 * world coordinate.
		 *
		 * Default implementation is to return nothing.
		 **/
		protected Component[] getContextMenu(Point2D worldPt)
		 {
			return new Component[0];
		 }

        protected Component[] getRulerMenu() {

			  return new Component[0];
		  }   
   
		private void privateSetStatus(Color col)
		 {	
			lightStatus.fireEvent(new ColorEvent(lightStatus, col));
			LManager.getLManager().repaint();
		 }
		
		Icon light2 = new Icon() {
			final int size = 8;
			public int getIconWidth() { return size; }
			public int getIconHeight() { return size; }

			@Override
			public void paintIcon(Component comp, Graphics g, int x, int y) {
				g.setColor(layerloadlight);
				g.fillRect(x, y, size, size);
				g.setColor(comp.getForeground());
				g.drawRect(x, y, size, size);
			}
		};

		/**
		 ** Retrieves the "child" of this view, if one
		 ** exists. Typically, the child is a zoomed-out view of this
		 ** one, off in a panner somewhere.
		 **
		 ** @return the child of this view, if one exists, otherwise
		 ** null
		 **/
		public final LView getChild()
		 {
			return  childLView;
		 }

		/**
		 ** Sets the "child" of this view, and sets the child's parent
		 ** to this view.
		 **/
		public void setChild(LView childLView)
		 {
			this.childLView = childLView;
			childLView.parentLView = this;
		 }

		/**
		 ** Returns the parent of this view... the inverse of {@link
		 ** #getChild}.
		 **/
		public final LView getParentLView()
		 {
			return  parentLView;
		 }

		/**
		 ** Constructs the superclass portion of an LView. Every LView
		 ** requires a Layer from the start of its existence.
		 **
		 ** <p>Automatically {@link Layer#registerDataReceiver
		 ** registers} this object with its layer... subclasses don't
		 ** need to do so. Generally speaking, an LView is only
		 ** partially active/constructed until it's added to an {@link
		 ** LViewManager}.
		 **
		 ** @param layerParent If non-null, specifies the layer that
		 ** this view is attached to. A null value indicates that this
		 ** view is "layer-less."
		 **/
		public LView(Layer layerParent)
		{
		    setOpaque(false);

            // Set layer and ignore data receiver registration; the existing 
            // framework in LViewManager handles the latter via the AncestorAdapter
            // below.
		    setLayer(layerParent, true);
		    lightStatus.addStateMachineListener(lightStatus);
		    
		    addAncestorListener(
		                        new AncestorAdapter()
		                        {
		                            public void ancestorAdded(AncestorEvent e)
		                            {
		                                log.println("LView shown caught");
		                                
		                                if(getParent() instanceof LViewManager) {
		                                    //without the following code, viewman is null when the view is added to the viewlist. For layers that 
		                                    //have propertyChange listeners, this ends up failing because of a NullPointerException. The Exception makes it
		                                    //so that we do not even get to the code that sets the viewman in LManager. 
		                                    if (viewman == null) {
			                                    viewman = (LViewManager) getParent();
			                                    if (getChild() != null) {
			                        				getChild().viewman = viewman.getChild();
			                        			}
		                                    }
		                                } else {
		                                    log.aprintln("PROGRAMMER WARNING: LView object " +
		                                                 "added to something that isn't an " +
		                                                 "LViewManager");
		                                }
		                                if(layer != null)
		                                    layer.registerDataReceiver(LView.this);
		                                
		                                if(dirty)
		                                    viewChanged();
		                                
		                                //   moved the following code to LViewManager.InternalList.add() 
		                                //   (JW 2/04)
		                                //viewInit();
		                                
		                            }
		                            
		                            public void ancestorRemoved(AncestorEvent e)
		                            {
		                                log.println("LView hidden caught");
		                                
		                                if(layer != null)
		                                    layer.unregisterDataReceiver(LView.this);
		                                
		                                // move the following code to InternalList.remove() (JW 2/04)
		                                //viewCleanup();
		                            }
		                        }
		    );
		    
		    addComponentListener(
		                         new ComponentAdapter()
		                         {
		                             public void componentResized(ComponentEvent e)
		                             {
		                                 viewChanged(true);
		                             }
		                         }
		    );
		    
		    
		    //add an empty LView3D by default
		    lview3d = new LView3D(layer){};
		}
		
		
		public LView(Layer layerParent, LView3D view3d){
			this(layerParent);
			if(view3d != null){
				lview3d = view3d;
				lview3d.myLview=this;
			}
		}


		/**
		 * Override to perform view specific init tasks
		 *   Note: this was changed from protected to public so that 
		 *   LViewManager.InternalList.add() can access it.  (JW 2/04)
		 */
		public void viewInit()
		{
		}
		
		
		/**
		 * Override to perform view specific cleanup tasks
		 *   Note: this was changed from protected to public so that 
		 *   LViewManager.InternalList.remove() can access it. (JW 2/04)
		 */
		public void viewCleanup()
		{
		}

		/**
		 ** Clears all off-screen drawing buffers. Sets all their pixels
		 ** to transparent.
		 **/
		public void clearOffScreen()
		 {
			if(buffers == null)
				return;

			for(int i=0; i<buffers.length; i++)
				clearOffScreen(i);
		 }

		/**
		 ** Clears a particular off-screen drawing buffer.
		 **/
		public void clearOffScreen(int i)
		 {
			if(buffers == null)
				return;

			Graphics2D g2 = buffers[i].createGraphics();
			g2.setBackground(new Color(0,0,0,0));
			g2.clearRect(0,0,
						 buffers[i].getWidth(),
						 buffers[i].getHeight());
			g2.dispose();
		 }

		public void setVisible(boolean visible)
		 {
			super.setVisible(visible);
			if(dirty  &&  visible  &&  isAlive())
				viewChanged();
		 }

		public void setVisible(boolean thisVisible, boolean childVisible)
		 {
			setVisible(thisVisible);
			if(childLView != null)
				childLView.setVisible(childVisible);
		 }

		/**
		 ** Returns a request object appropriate to send to {@link
		 ** Layer#receiveRequest Layer.receiveRequest} that represents
		 ** a request for data in the given rectangle. This must be
		 ** defined by every derived LView, and is used by the
		 ** superclass whenever the screen-updating mechanisms need to
		 ** create a request for data in an exposed area of the world.
		 **
		 ** <b>For views without an associated layer</b>: This method
		 ** will never be called.
		 **
		 ** @param where The region of the world that has been
		 ** exposed. The returned Object will usually include region
		 ** as data (or include something that encloses this region).
		 **/
		protected abstract Object createRequest(Rectangle2D where);

		/**
		 ** <p>Handles incoming data from the Layer. Whenever the
		 ** Layer has data that (potentially) concerns this LView, it
		 ** is received here. Derive LViews should implement this
		 ** function to handle data properly for particular types of
		 ** Layers. Note that not all incoming data will necessarily
		 ** directly concern this LView, depending on the particular
		 ** layer's implementation.
		 **
		 ** <b>For views without an associated layer</b>: This method
		 ** will never be called.
		 **
		 ** @param layerData Whatever was sent by the Layer. The
		 ** object will likely need to be cast to the correct type. It
		 ** is highly recommended that instanceof checks be performed
		 ** before the cast, and that unknown object types are ignored
		 ** with an error message at the console.
		 **/
		public abstract void receiveData(Object layerData);

		/**
		 ** Creates a graphics context that paints to off-screen
		 ** buffer zero in world coordinates, with full proper
		 ** wrapping. Received data should generally be rendered to
		 ** this graphics context and not directly to the screen. Only
		 ** immediate user feedback should be drawn directly to the
		 ** screen without going through this graphics context.
		 **
		 ** @see #getOnScreenG2
		 **
		 ** @return A new graphics context for rendering data for this
		 ** view off-screen.
		 **/
		public Graphics2D getOffScreenG2()
		 {
			return  getOffScreenG2(0);
		 }

		/**
		 ** Creates a graphics context that paints to a particular
		 ** off-screen buffer in world coordinates, with full proper
		 ** wrapping.
		 **/
		public Graphics2D getOffScreenG2(int i)
		 {
			if(!isAlive())
				return  null;

			Graphics2D g2 = buffers[i].createGraphics();
			g2.setTransform(getProj().getWorldToScreen());
			return  viewman.wrapWorldGraphics(g2);
		 }

		/**
		 ** Creates a graphics context that paints to off-screen
		 ** buffer zero in screen coordinates (pixels).
		 **/
		public Graphics2D getOffScreenG2Direct()
		 {
			return  getOffScreenG2Direct(0);
		 }

		/**
		 ** Creates a graphics context that paints to a particular
		 ** off-screen buffer in screen coordinates (pixels).
		 **/
		public Graphics2D getOffScreenG2Direct(int i)
		 {
			if (buffers == null || buffers[i] == null){
				// If buffers haven't been readied as yet, return null
				return null;
			}
			
			return  buffers[i].createGraphics();
		 }

		/**
		 ** Creates a graphics context that paints to off-screen
		 ** buffer zero in world coordinates, WITHOUT full proper
		 ** wrapping.
		 **/
		protected final Graphics2D getOffScreenG2Raw()
		 {
			return  getOffScreenG2Raw(0);
		 }

		/**
		 ** Creates a graphics context that paints to a particular
		 ** off-screen buffer in world coordinates, WITHOUT full
		 ** proper wrapping.
		 **/
		protected final Graphics2D getOffScreenG2Raw(int i)
		 {
			if(buffers == null)
				return  null;

			Graphics2D g2 = buffers[i].createGraphics();
			g2.setTransform(getProj().getWorldToScreen());
			return  g2;
		 }

		/**
		 ** Creates a graphics context that paints directly to the
		 ** screen, in screen (pixel) coordinates, WITHOUT full proper
		 ** wrapping.
		 **/
		protected Graphics getGraphicsRaw()
		 {
			return  super.getGraphics();
		 }

		/**
		 ** Creates a graphics context that paints directly to the
		 ** screen, in screen (pixel) coordinates, WITH full proper
		 ** wrapping.
		 **/
		public Graphics getGraphics()
		 {
			return
				viewman.wrapScreenGraphics((Graphics2D) super.getGraphics());
		 }

		/**
		 ** Creates a graphics context that paints directly to the
		 ** screen in world coordinates. Only immediate user feedback
		 ** should be drawn to this context.  General data and view
		 ** updates should usually be drawn to the off-screen buffer.
		 **
		 ** @see #getOffScreenG2
		 **
		 ** @return A new graphics context for rendering data for this
		 ** view on-screen.
		 **/
		protected Graphics2D getOnScreenG2()
		 {
			Graphics2D g2 = (Graphics2D) super.getGraphics();
			g2.transform(getProj().getWorldToScreen());
			return  viewman.wrapWorldGraphics(g2);
		 }

		/**
		 ** Creates a graphics context that paints directly to the
		 ** screen in spatial coordinates. Only immediate user
		 ** feedback should be drawn to this context. General data
		 ** and view updates should usually be drawn to the off-screen
		 ** buffer.
		 **
		 ** @see #getOffScreenG2
		 **
		 ** @return A new graphics context for rendering data for this
		 ** view on-screen.
		 **/
		public Graphics2D getOnScreenSpatial()
		 {
			return  getProj().createSpatialGraphics(getOnScreenG2());
		 }

		/**
		 ** Creates a graphics context that paints to off-screen
		 ** buffer zero in spatial coordinates, with full proper
		 ** wrapping. Received data should generally be rendered to
		 ** this graphics context and not directly to the screen. Only
		 ** immediate user feedback should be drawn directly to the
		 ** screen without going through this graphics context.
		 **
		 ** @see #getOnScreenG2
		 **
		 ** @return A new graphics context for rendering data for this
		 ** view off-screen.
		 **/
		public Graphics2D getOffScreenSpatial()
		 {
			return  getOffScreenSpatial(0);
		 }

		/**
		 ** Creates a graphics context that paints to a particular
		 ** off-screen buffer in spatial coordinates, with full proper
		 ** wrapping.
		 **/
		public Graphics2D getOffScreenSpatial(int i)
		 {
			return  getProj().createSpatialGraphics(getOffScreenG2(i));
		 }

		/**
		 ** Any subclasses that redefine this should probably call
		 ** super.paintComponent() at some point.
		 **/
		int lastwidth = 0;
		public void paintComponent(Graphics g)
		 {
			super.paintComponent(g);
			if(getWidth() > 10000)
			 {
				if(lastwidth != getWidth())
				 {
					log.aprintln(
						"WARNING: Skipped painting " + getClass().getName() +
						"'s buffer on-screen, too expensive");
					lastwidth = getWidth();
				 }
				return;
			 }
			realPaintComponent(g);
		 }

		private ColorMapOp colorMapOp = new ColorMapOp(null);
		public void setColorMapOp(ColorMapOp colorMapOp)
		 {
			this.colorMapOp = colorMapOp;
			repaint();
			if(getChild() != null)
				getChild().setColorMapOp(colorMapOp);
		 }
		
		/**
         * Returns the ColorMapOp set via a prior call to setColorMapOp.
         * This colormap is applied to all the backing buffers of this
         * LView.
         */
		public ColorMapOp getColorMapOp(){
			return colorMapOp;
		}

		public void realPaintComponent(Graphics g)
		 {
			if(buffers == null)
			 {
				log.println("CAN'T PAINT A NON-ALIVE VIEW");
				return;
			 }

			Graphics2D g2 = (Graphics2D) g;

			boolean simpleDraw = colorMapOp.isIdentity()  &&  alpha == 1.0;
			for(int i=0; i<buffers.length; i++)
				if(!bufferHidden[i])
					if(simpleDraw)
						g.drawImage(buffers[i], 0, 0, null);
					else
						g2.drawImage(buffers[i], colorMapOp.forAlpha(alpha),
									 0, 0);
		 }
		
		/**
         * Returns the <em>i</em>th backing buffer for this layer
         * or null if either no buffers have been allocated or 
         * the index is out of range or the buffer is actually
         * null.
         */
		public BufferedImage getBuffer(int i){
			if (buffers == null || i < 0 || i >= buffers.length)
				return null;
			return buffers[i];
		}

		/**
		 ** Convenience method; currently returns {@link Main#PO}.
		 **/
	    public ProjObj getPO()
		 {
			return  Main.PO;
		 }

		/**
		 ** Convenience method; currently calls {@link
		 ** Main#setProjection}.
		 **/
	    public void setPO(ProjObj po)
		 {
			Main.setProjection(po);
		 }

		public MultiProjection getProj()
		 {
			//this check is just in case viewman is null (it should never be)
			if (viewman == null) {
				return null;
			}
			return  viewman.getProj();
		 }

		private static int id = 0;
		private synchronized static int nextId()
		 {
			return  ++id;
		 }

		/**
		 ** This function is a factory for creating a new offscreen buffer.
		 ** We used to only have simple image buffers (ie bytes) but for
		 ** analysis purposes we want to encorporate layers with other
		 ** data types (ie float, int, double, multiple bands, etc) so
		 ** we need more flexibility when creating the offscreen buffer.
		 ** This routine calls the {@link Util#newBufferedImage} function
		 ** and MUST be overridden by a deriving layer which wants a non-
		 ** standard buffered image for its offscreen buffer
		 **/

		 protected BufferedImage newBufferedImage(int w, int h)
		 {
			return (Util.newBufferedImage(w,h));
		 }


		/**
		 ** Allocates the entire array of off-screen buffers.
		 **/
		protected final BufferedImage[] newBufferedImageArray(int w, int h)
		 {
			BufferedImage[] b = new BufferedImage[bufferCount];
			for(int i=0; i<b.length; i++)
				b[i] = newBufferedImage(w, h);
			return  b;
		 }

		/**
		 ** Some kind of hack for the numeric back layer.
		 **/
		protected final void pingOffscreenImage()
		 {
			if (buffers == null)  // this can happen at the begining
			 {
				Dimension pixSize = getProj().getScreenSize();
				buffers = newBufferedImageArray(pixSize.width,
												pixSize.height);
			 }
		 }

		/**
		 ** Called whenever new data is needed, triggered by a change
		 ** in the world window. Calls {@link #viewChangedPre} before
		 ** processing the change, and {@link #viewChangedPost} after
		 ** (IF it's going to process at all; sometimes it doesn't).
		 ** Subclasses should override those functions if they need to
		 ** hook custom responses to a view change.
		 **
		 ** <p>Currently the management of a viewing window
		 ** (i.e. projection) change is handled somewhat haphazardly.
		 ** Soon to be replaced with more elegant and complete
		 ** projection management.
		 **/
		public void viewChanged(boolean dueToResize)
		 {
			log.printStack(0);

			// If we're hidden and a viewchanged is issued, we
			// invalidate our current offscreen buffer.
			if(viewman == null || (!isVisible()  &&  !dirty)) {//@since remove viewman2
				clearOffScreen();
			}
			dirty = true;

			// Cut down on useless viewChanges that will die later or
			// should never have happened.
			if(getWidth() == 0  ||
				getHeight() == 0  ||
				getProj() == null  ||
				!isVisible())
				return;

			String className = getClass().getName();
			className = className.substring(className.lastIndexOf('.')+1);
			String threadName = viewman.getName() + "-" + className;
			incBusy(); // we incBusy BEFORE forking a new thread

			log.println("Starting new thread: <" + threadName + ">");
			log.println("View size: " + getWidth() + "x" + getHeight());
			log.printStack(-1);

			new WatchedThread(new ViewChangeRunnable(dueToResize, ++lastViewChangeId), threadName).start();
			// we decBusy INSIDE the new thread, below
		 }

		// Used for handling legacy viewChanged calls without needing to change the method signature
		public void viewChanged() {
			viewChanged(false);
		}
				
		// Used to track the most recent view change and allow for early exit for obsolete requests
		private int lastViewChangeId = 0;
		
	        // Used to track creation time of view change threads.
	        private static ThreadLocal viewChangeTime = new ThreadLocal() {
			protected synchronized Object initialValue() {
			    return new Long(-1);
			}
		    };

	        // Get creation time of view change thread.
	        // Returns -1 if current thread is not a 
	        // view change thread.
	        protected static long getViewChangeTime()
	        {
		    return ((Long)viewChangeTime.get()).longValue();
	        } 

		private class ViewChangeRunnable implements Runnable {

			boolean changedDueToResize=false;
			int changeId = -1;
			
		    ViewChangeRunnable(boolean changedDueToResize, int changeId) {
		    	this.changedDueToResize = changedDueToResize;
		    	this.changeId = changeId;
		    }

		    public void run() {
		    	try {
		    		// Window drag resizes can trigger a ton of rapid requests.  Wait briefly to see if
		    		// this request is the last one or not
					if (changedDueToResize) {
						Thread.sleep(100);
					}
				
					// If we're not the most recent view change request, stop our work early
					if (changeId!=lastViewChangeId) return;
					
					viewChangedReal();
			    } catch (InterruptedException ie) {
			    	
			    } finally {
			    	decBusy();
			    }
		    }
		}
		
		/**
		 * Sets the layer's preference to have the offscreen buffer cleared on a
		 * view change, or not.
		 * 
		 * A layer that draws the entire offscreen buffer on a view change may
		 * want to return <code>false</code>. The default is to return
		 * <code>true</code>.
		 */
		protected boolean clearOffScreenOnViewChange(){
			return true;
		}
		
		private void viewChangedReal()
		 {
			log.println("Hi I'm a: "+this);
			log.printStack(3);

			dirty = true;

			if(viewman == null) {//remove viewman2
				log.println("view is not enabled, leaving");
				return;
			}

			if(getWidth() == 0  ||  getHeight() == 0)
				return;

			// Call the client's hook
			viewChangedPre();

			MultiProjection proj = viewman.getProj();

			Dimension pixSize = proj.getScreenSize();

			if(pixSize.width <= 0  &&  pixSize.height <= 0)
			 {
				log.println("--unsized image--");
				return;
			 }
			else
				log.println("--sized image--");

			if (buffers == null ||
				pixSize.width != buffers[0].getWidth() ||
				pixSize.height != buffers[0].getHeight())
			 {
				log.println("Calling Factory newBufferedImage("+pixSize.width+","+pixSize.height+")");
			   buffers = newBufferedImageArray(pixSize.width,
											   pixSize.height);
			 }
			else
			 {
				if (clearOffScreenOnViewChange()) {
					log.println("Clearing entire area of offscreen buffers");
					clearOffScreen();
				} else {
					log.println("Clearing exposed area of offscreen buffers");
				}
			 }

			if(proj.getWorldWindow() == null)
				log.println("*** " + getClass().getName() + " *** " +
							"getWorldWindow() is null right after");

			// Currently just re-requests the entire screen after
			// every screen change... eventually, this will be a
			// little smarter and will only request needed stuff.
			if(isVisible())
			 {
				Object layerRequest = createRequest(proj.getWorldWindow());
				if(layer != null)
					layer.receiveRequest(layerRequest, this);
				dirty = false;
			 }

			if(proj.getWorldWindow() == null)
				log.println("*** " + getClass().getName() + " *** " +
							"getWorldWindow() is null at end");

			// Call the subclass's hook
			viewChangedPost();
		 }

		/**
		 ** Requests that this view's {@link #viewman} center at a
		 ** particular world-coordinate point. Kludge, should be a
		 ** direct method on {@link LViewManager}.
		 **/
		public void centerAtPoint(Point2D p)
		 {
			 //if(this instanceof edu.asu.jmars.layer.obs.mro.MttLView  &&
			 //  getChild() != null)
			 //	log.aprintStack(8);
			Main.testDriver.locMgr.setLocation(p, true);
		 }

		/**
		 ** Called whenever new data is needed, before the superclass
		 ** performs its view change processing, triggered by a change
		 ** in the world window.
		 **
		 ** <b>For views with an associated layer</b>: Note that after
		 ** this function is called in a subclass, needed new data
		 ** will be requested by the superclass for exposed areas of
		 ** the world, so it's not necessary to make any requests here
		 ** to satisfy new data needs.
		 **
		 ** <p>Subclasses should override this function only if they
		 ** need to hook custom responses to a view change. The
		 ** default implementation does nothing.
		 **/
		protected void viewChangedPre()
		 {
		 }

		/**
		 ** Called whenever new data is needed, after the superclas
		 ** performs its view change processing, triggered by a change
		 ** in the world window.
		 **
		 ** <b>For views with an associated layer</b>: Note that by
		 ** the time this function is called in a subclass, needed new
		 ** data for exposed areas of the world has already been
		 ** requested by the superclass, so it's not necessary to make
		 ** any new requests here to satisfy new data needs.
		 **
		 ** <p>Subclasses should override this function only if they
		 ** need to hook custom responses to a view change. The
		 ** default implementation does nothing.
		 **/
		protected void viewChangedPost()
		 {
		 }

		/**
		 ** Returns the class name of this view. Convenience default
		 ** base implementation, subclasses should generally override
		 ** this method to provide a more informative name.
		 **/
		public String getName()
		 {
			return  getClass().getName();
		 }

		/**
		 ** Returns a LayerView's focuspane...used to load the main
		 ** tabbed pane. Ben's LManager stuff.
		 **/
		public FocusPanel getFocusPanel()
		 {
			if(focusPanel == null)
				focusPanel = new FocusPanel(this);
			return  focusPanel;
		 }
		
		/**
		 * 
		 * This method allows functionality such as delete to get the focus panel if it exists,
		 * without creating one just to delete the row 
		 *
		 * @return FocusPanel if one exists or null if one does not
		 *
		 */
		public FocusPanel getFocusPanelNoCreate() {
		   return focusPanel;
		}

		public FocusPanel getFocusPanel(JFrame frame)
		{
			return getFocusPanel();
		}

        // temporary setCursor function.
        public void setCursor(Cursor c)
         {
			if(currentCursor != c)
			 {
				if(viewman != null)//@since remove viewman2
				 {
					currentCursor = c;
					viewman.setCursor(c);
				 }
			 }
         }

	    private Cursor currentCursor;

	    public Cursor getCursor()
	     {
			return  currentCursor;
	     }

		/**
		 ** Returns a "duplicate" of this lview that isn't yet tied to
		 ** being contained in a particular LViewManager. Similar to
		 ** java's {@link Object#clone cloning}, but not quite.
		 **
		 ** <p>Only if a subclass has useful state that should be
		 ** copied, should this method be overridden. For example, if
		 ** there's a parameter "foo" that should be copied for proper
		 ** duplication, a subclass would implement this method as:
		 **
		 ** <p><pre>public LView dup()
		 ** {
		 **     FooLView copy = (FooLView) super.dup();
		 **     copy.foo = foo; // or foo.clone(), as appropriate
		 **     return  copy;
		 ** }</pre>
		 **
		 ** <p>Note also that any setter-methods will likely therefore
		 ** want to proxy to a child, such as:
		 **
		 ** <p><pre>public void setFoo(double newfoo)
		 ** {
		 **     foo = newfoo;
		 **     if(getChild() != null) getChild().foo = newfoo;
		 ** }</pre>
		 **
		 ** @return A new semi-clone of this LView. Generally this will become
		 ** the panner view LView.
		 **/
		public LView dup()
		 {
			// Subclasses must use super.dup(), NOT _new()!
			LView copy = _new();

			// Copy over any necessary state not handled by
			// construction in _new().
			copy.alpha = alpha;

			return  copy;
		 }

		/**
		 ** Subclasses should return a new object of their subclass,
		 ** linked to the same layer as this view is. No other
		 ** initialization need be performed.
		 **
		 ** @return A new LView whose class is the same as the dynamic
		 ** type of this object, and which is linked to the same layer
		 ** as this object is.
		 **/
		protected abstract LView _new();
		// GENERALLY SHOULD LOOK LIKE:
		// {
		//	return  new DerivedLView(this.layer);
		// }

		/**
		 ** Allows the panel rebuilds to proceed more quickly...  basically,
		 ** it's a JPanel that contains the "real" focus panel for a
		 ** particular view. But it only contains and lays out the focus the
		 ** first time that it's shown, thus delaying the focus panel's
		 ** overhead until it's needed. The "overhead" referred to isn't so
		 ** much the focus panel's swing elements, as much as the data that
		 ** those elements need. Some layers' focus panels monopolize the AWT
		 ** thread while waiting for data to come in, if you don't use this
		 ** wrapper. Michael created this mess, go talk to him :).
		 **/
		protected abstract class DelayedFocusPanel
		 extends FocusPanel
		 implements ComponentListener, Runnable, SwingConstants
		 {
			private boolean done = false;

			public DelayedFocusPanel()
			 {
				super(Layer.LView.this);
		//		setLayout(new OverlapLayout());

				JLabel lbl = new JLabel("Still loading " + parent.getName() +
										", please wait...");
				lbl.setVerticalAlignment(TOP);
				add(lbl);
				addComponentListener(this);
			 }

			/**
			 ** This method must be overridden to return a view's actual focus
			 ** panel. It will be called exactly once, and only when the
			 ** actual focus panel needs to be displayed for the first time
			 ** (through user action). This method is probably most easily
			 ** implemented within an anonymous inner class, inside a view's
			 ** {@link Layer.LView#getFocusPanel getFocusPanel} method. Such
			 ** as:
			 **
			 ** <code>// inside a FoobarLView:
			 ** public JPanel getFocusPanel()
			 **  {
			 **     if(focusPanel == null)
			 **         focusPanel =
			 **             new DelayedFocusPanel()
			 **              {
			 **                 protected FocusPanel createFocusPanel()
			 **                  {
			 **                     return  new FoobarFocus();
			 **                  }
			 **              };
			 **     return  focusPanel;
			 **  }</code>
			 **/
			abstract public FocusPanel createFocusPanel();

			public void componentHidden(ComponentEvent e)
			 { }
			public void componentMoved(ComponentEvent e)
			 { }
			public void componentResized(ComponentEvent e)
			 { }
			public synchronized void componentShown(ComponentEvent e)
			 {
				if(done)
					return;
				done = true;
				new WatchedThread(this).start();
			 }

			public void run()
			 {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						FocusPanel fp = createFocusPanel();
						add("Adjustments",fp);
						Util.expandSize((Frame)fp.getTopLevelAncestor());
					}
				});
			 }
		 }
		
	//The following four methods are used to query for the
	 // info panel fields (description, citation, etc)	
		String layerKey = "";
		public String getLayerKey() {
	 		if(layerKey==null){
	 			if(layerParams!=null)
	 				return layerParams.name;
	 		}
	 		return layerKey;
	 	}
	 	
	 	public String getLayerType(){
	 		if(layerParams!=null)
	 			return layerParams.type;
	 		return "stamp";
	 	}
	 	
	 	protected LayerParameters layerParams=null;
	 	public LayerParameters getLayerParameters(){
	 		return layerParams;
	 	}
	 	
	 	public void setLayerParameters(LayerParameters lp){
	 		layerParams=lp;
	 	}
	 	
	 	
	 	public LView3D getLView3D(){
	 		return lview3d;
	 	}
	    
		public StateMachine lightStatus = new StateMachine() {
			public State START = new State() {
				Transition color = new Event(ColorEvent.class) {
					public void action() {
						layerloadlight = ((ColorEvent) getEvent()).getColor();
					};			
				};
			};
		};
		public boolean isOverlay() {
		    return overlayFlag;
		}
		public String getOverlayId() {
			return overlayId;
		}
		public void setOverlayId(String id) {
			overlayId = id;
		}
        public void setOverlayFlag(boolean flag) {
            overlayFlag = flag;
            overlaySetFlag = true;
        }
		public boolean isOverlaySet() {
		    return overlaySetFlag;
		}
 }	//Lview
	
	public static abstract class LView3D{
		protected Layer myLayer;
		protected LView myLview;
		protected ArrayList<GLRenderable> orderedRenderables;
		protected float alpha;
		//This has to do with the Row button being selected or not
		protected boolean isVisible = false;//not visible by default from meeting on 04/27/2020
		protected boolean exists = false;
		protected ThreeDManager mgr = null;
		protected Thread animateThread = null; 
		
		/** Original design intended that lview3ds used GLRenderables, but 
		 *  it is now possible for some to use decals instead, so use this 
		 *  boolean to find out which is being used (this is important because
		 *  the 3D manager has to do different things to render them).	 */
		protected boolean usesDecals = false;
		
		// This is used to indicate whether there is 0 (not used), 1 (common) or more (for selections, etc) decal sets per layer
		protected int numDecalLayers = 1;
		
		public LView3D(Layer layer){
			myLayer = layer;
			orderedRenderables = new ArrayList<GLRenderable>();
			if (LoginWindow2.getInitialize3DFlag()) {
				mgr = ThreeDManager.getInstance();
			}
		}
		
		public ArrayList<GLRenderable> getRenderables(){
			return orderedRenderables;
		};
		
		/**
		 * Is true if the 3D button on the Row is turned on
		 * @return
		 */
		public boolean isVisible(){
			return isVisible;
		}
		public void setVisible(boolean show){
			isVisible = show;
			//update 3d
			if (ThreeDManager.isReady()) {
				mgr.update();
			}
		}
		

		/**
		 * This has to do with whether the LView3D for a particular
		 * layer type has been written.  If there is no LView3D for 
		 * a layer type, then it cannot be displayed at all in the 3D
		 * view.  So this is used to disable the button on the Row
		 * Should be overwritten in layers that have some sort of 3DView
		 * for the 3DManager
		 * @return
		 */
		public boolean exists(){
			return exists;
		}
		
		
		/**
		 * This is used to tell whether this lview3d is enabled for 
		 * viewing.  This can be dependent on whether the lview3d exists
		 * and whether the shape model on the manager matches the one 
		 * in this view3d.
		 * @return
		 */
		public boolean isEnabled(){
			return false;
		}
		public void setAlpha(float alpha){
			this.alpha = alpha;
			//set alpha on renderables
			for(GLRenderable r : orderedRenderables){
				if(r!=null){
					float tempAlpha = r.getAlpha();
					float displayAlpha = tempAlpha * alpha;
					r.setDisplayAlpha(displayAlpha);
				}
			}
			//update 3d if visible
			if(ThreeDManager.isReady()){
				mgr.update();
			}
		}
		
		public float getAlpha(){
			return alpha;
		}
		
		/**
		 * Should be overridden by individual LView3D classes.
		 * Is triggered from the ThreeDManager's Animate ThemeButton.
		 * @param stepTime	How big of a step to animate with (seconds).
		 * @param waitTime	How long to wait between each step
		 * through the animation (miliseconds).
		 */
		public void animate(double stepTime, double waitTime){
			
		}
		
		/**
		 * Kills the animation if it is active.  The animate method
		 * should catch {@link InterruptedException} when it sleeps for
		 * the waitTime amount, because it will come across this when
		 * this method is called.
		 */
		public void cancelAnimate(){
			try{
				animateThread.interrupt();
				animateThread = null;
				if (ThreeDManager.isReady()) {
					mgr.update();
				}
			}catch(NullPointerException e){
				log.aprintln("No animation is active.");
			}
		}
		
        /**
         * Overridden by individual views, returns information for formatting for 
         * Investigate mode while hovering with the cursor in the 3D view.
         */
        public InvestigateData getInvestigateData(IntersectResult ir) {
        	return null;
        }
        
        
        /**
         * @return True if this lview3D uses decals for rendering, false means
         * it is intended to use GLRenderables for rendering
         */
        public boolean usesDecals(){
        	return usesDecals;
        }
        
        public DecalSet getDecals(){
        	return new DecalSet();
        }

        public int getNumberOfDecalSets() {
        	return numDecalLayers;
        }
        
        /**
         * Converts a standard Color into a set of float values suitable for use by the 3D view.
         * 
         * @param Color object
         * @return array of gl float equivalents
         */
        protected static float[] glColor(Color c){
        	return new float[]{ c.getRed()/255.0f, c.getGreen()/255.0f, c.getBlue()/255.0f };
        }

        /**
         * Reverse the order of an array of HVectors
         * 
         * @param An array of HVectors
         * @return A new array with the same HVectors in reverse order
         */
       	protected static HVector[] reverse(HVector[] pts){
       		List<HVector> list = Arrays.asList(pts);
       		Collections.reverse(list);
       		return list.toArray(new HVector[0]);
       	}
	
		/**
		 * Pack the points into a one dimensional (1D) array of floats by packing each
		 * point's x, y, and z coordinates in order.
		 * @param pts Input points
		 * @return Array of floats of size <code>pts.length*3</code> filled with points
		 * data x1,y1,z1,x2,y2,z2,...xn,yn,zn
		 */
		public static float[] toFloat1D(HVector[] pts){
			float[] out = new float[pts.length*3];
			
			for(int i=0; i<pts.length; i++){
				if (pts[i]==null) {
					out[i*3+0] = Float.NaN;
					out[i*3+1] = Float.NaN;
					out[i*3+2] = Float.NaN;				
				} else {
					out[i*3+0] = (float)pts[i].x;
					out[i*3+1] = (float)pts[i].y;
					out[i*3+2] = (float)pts[i].z;
				}
			}
			
			return out;
		}	
		
		
		/**
		 * NOTE: Really don't think this is where this should live... possibly Layer actually?
		 * But for now have it here because most of this logic exists in the LView3D already
		 * 
		 * Method to return a buffered image of the correct size with the correct data with
		 * a given world rectangle, proj obj, and ppd.
		 * 
		 * Used for High Res Export and for populating Decals for 3D view.
		 *
		 * @param worldRect  Bounds defining the buffered image
		 * @param po		Proj Obj at which the data should be projected
		 * @param ppd		PPD at which the data should be displayed
		 * @param labelScaleFactor  If the labels are to be scaled then an integer greater than 0 is passed in.
		 * 							If a number less than 0 is passed in, then labels should not be drawn at all.
		 * @return	BufferedImage of the correct size with the correct data drawn in it
		 */
		public BufferedImage createDataImage(Rectangle2D worldRect, ProjObj po, int ppd, int labelScaleFactor){
			return null;
		}

	}
	
	
	
 }
