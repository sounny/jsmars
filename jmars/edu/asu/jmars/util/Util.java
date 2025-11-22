package edu.asu.jmars.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.color.ICC_ProfileGray;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
//import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsFactory;
import nom.tam.util.BufferedDataOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpStatus;
import com.vividsolutions.jts.geom.Envelope;
import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.MultiProjection;
import edu.asu.jmars.layer.SavedLayer;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.map2.MapServerDefault;
import edu.asu.jmars.layer.map2.MapSourceDefault;
import edu.asu.jmars.layer.util.features.FeatureProviderSHP;
import edu.asu.jmars.pref.DefaultBrowser;
import edu.asu.jmars.ui.looknfeel.GUIState;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.stanford.ejalbert.BrowserLauncher;
import com.install4j.api.launcher.ApplicationLauncher;

/* Collection of useful things for jlayers... all methods are static,
   "Util" is just a handy namespace kludge, not a real object type */
public final class Util
 {
    private static DebugLog log = DebugLog.instance();

    public static final Color darkGreen = Color.green.darker();
    public static final Color darkRed = Color.red.darker();
    public static final Color darkBlue = Color.blue.darker();
    public static final Color brightBlue = new Color(0, 150, 255);
    public static final Color purple = new Color(128, 0, 128);
    public static final Color green3 = new Color(0,205,0);
    public static final Color darkViolet = new Color(148,0,211);
    public static final Color cyan3 = new Color(0,205,205);
    public static final Color chocolate4 = new Color(139,69,19);
    public static final Color maroon = new Color(176,48,96);
    public static final Color yellow3 = new Color(205,205,0);
    public static final Color gray50 = new Color(128,128,128);
    public static final Color darkOrange = new Color(255,140,0);
    public static final Color darkBrown = new Color(101,67,33);

    private static String configPrefix = null;// @since change bodies
    private static boolean showBodyMenuFlag = false;
    private static TreeMap<String,String[]> bodyList = null;// @since change bodies
    private static String cacheDir = Main.getJMarsPath() + "localcache";
    
    static final int ZIP_BUFFER = 2048;
    
    private static JFrame optionPaneFrame;
    private static JDialog dialog;
    public static final String START_FC_LOCATION = "start_fc_location";
	
    private static JFrame getOptionPaneFrame() {
    	GraphicsDevice device = (Main.mainFrame).getGraphicsConfiguration().getDevice();
    	optionPaneFrame = new JFrame(device.getDefaultConfiguration());
    	optionPaneFrame.setLocationRelativeTo(Main.mainFrame);   
    	optionPaneFrame.setUndecorated(true);
    	optionPaneFrame.setVisible(true);
    	optionPaneFrame.setAlwaysOnTop(true);    	
    	return optionPaneFrame;
    }
    private static AbstractAction closeAction = new AbstractAction("CLOSE") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			dialog.setVisible(false);
			dialog.dispose();
		}
	};
    public static void showLongMessageDialog(String msg, String title) {
    	dialog = new JDialog(optionPaneFrame);
    	JTextArea text = new JTextArea(msg);
    	JButton closeButton = new JButton(closeAction);
    	JScrollPane scroll = new JScrollPane();
    	scroll.setViewportView(text);
    	scroll.setSize(800,300);
    	scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    	scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    	
    	JPanel panel = new JPanel();
    	GroupLayout layout = new GroupLayout(panel);
    	panel.setLayout(layout);
    	
    	layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
    		.addComponent(scroll)
    		.addComponent(closeButton));
    	layout.setVerticalGroup(layout.createSequentialGroup()
    		.addComponent(scroll)
    		.addComponent(closeButton));
    	
    	dialog.setContentPane(panel);
    	dialog.getRootPane().setDefaultButton(closeButton);
    	dialog.setSize(new Dimension(800,300));
    	dialog.setResizable(false);
    	dialog.setLocationRelativeTo(optionPaneFrame);
    	dialog.setTitle(title);
    	dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    	dialog.setVisible(true);
    }
    public static void showMessageDialog(String msg, String title, int mType) {
    	getOptionPaneFrame();
    	if (msg.length() > 2000) {//added to handle unexpectedly large error messages
    		showLongMessageDialog(msg, title);
    	} else {
    		JOptionPane.showMessageDialog(optionPaneFrame, msg, title, mType);
    		optionPaneFrame.setVisible(false);
    	}
    }
    public static void showMessageDialogObj(Object msg, String title, int mType) {
    	getOptionPaneFrame();
    	JOptionPane.showMessageDialog(optionPaneFrame, msg, title, mType);
    	optionPaneFrame.setVisible(false);
    }
    public static void showMessageDialog(String msg) {
    	showMessageDialog(msg, "JMARS", JOptionPane.INFORMATION_MESSAGE);
    }
    public static Object showInputDialog(Object msg, String title, int msgType, Icon icon, Object[] selectionValues, Object initialSelectionValue) {
    	getOptionPaneFrame();
    	Object returnVal = JOptionPane.showInputDialog(optionPaneFrame,msg, title,msgType, icon, selectionValues, initialSelectionValue);
    	optionPaneFrame.setVisible(false);
    	return returnVal;
    }
    public static String showInputDialog(String msg, String initialValue) {
    	return showInputDialog(msg,initialValue,JOptionPane.QUESTION_MESSAGE);
    }
    public static String showInputDialog(String msg, String initialValue, int msgType) {
    	getOptionPaneFrame();
    	String returnVal = JOptionPane.showInputDialog(optionPaneFrame, msg, initialValue, msgType);
    	optionPaneFrame.setVisible(false);
    	return returnVal;
    }
    public static int showConfirmDialog(Object message, String title, int optionType) {
    	return Util.showConfirmDialog(message, title, optionType, JOptionPane.PLAIN_MESSAGE);
    }
    public static int showConfirmDialog(Object message){
    	return Util.showConfirmDialog(message, "JMARS", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
    }
    public static int showConfirmDialog(Object message, String title, int optionType, int messageType) {
    	getOptionPaneFrame();
    	int returnVal = JOptionPane.showConfirmDialog(optionPaneFrame, message, title, optionType, messageType);
    	optionPaneFrame.setVisible(false);
    	return returnVal;
    }
    public static int showOptionDialog(Object message, String title, int optionType, int messageType, Icon icon, Object[] options, Object initialValue) {
    	getOptionPaneFrame();
    	int returnVal = JOptionPane.showOptionDialog(optionPaneFrame, message, title, optionType, messageType, icon, options, initialValue);
    	optionPaneFrame.setVisible(false);
    	return returnVal;
    }
    public static int showOpenDialog(JFileChooser fc) {
    	getOptionPaneFrame();
    	int returnVal = fc.showOpenDialog(optionPaneFrame);
    	optionPaneFrame.setVisible(false);
    	return returnVal;
    }
    public static int showSaveDialog(JFileChooser fc) {
    	getOptionPaneFrame();
    	int returnVal = fc.showSaveDialog(optionPaneFrame);
    	optionPaneFrame.setVisible(false);
    	return returnVal;
    }

    public static String getDefaultFCLocation() {
    	return Config.get(START_FC_LOCATION, Main.getUserHome());
    }
    
    /**
	* @since change bodies
	*/
    public static TreeMap<String,String[]> getBodyList() {
    	return bodyList;
    }
    
    /**
     * @since change bodies
     */
    public static boolean showBodyMenu() {
    	return showBodyMenuFlag;
    }
     // This is to prevent some ne'er-do-well from coming in and trying 
     // to instanciate what is supposed to be class of nothing but static methods.
     private Util(){}

    /**
     ** If a String is non-null, returns it, otherwise returns the
     ** empty string.
     **/
    public static String blankNull(String s)
     {
	return  s == null ? "" : s;
     }

    /**
     ** Adds a MouseListener to a container and all its children.
     **/
    public static void addMouseListenerToAll(Container cont, MouseListener ml)
     {
	cont.addMouseListener(ml);
	Component[] comps = cont.getComponents();
	for(int i=0; i<comps.length; i++)
	    if(comps[i] instanceof Container)
		addMouseListenerToAll((Container) comps[i], ml);
	    else
		comps[i].addMouseListener(ml);
     }

    /**
     ** Given a component, creates an image the same size as it and
     ** containing a painted version of it with a given alpha.
     **/
    public static BufferedImage createImage(Component comp, float alpha)
     {
	BufferedImage img = Util.newBufferedImage(comp.getWidth(),
						  comp.getHeight());
	Graphics2D g2 = img.createGraphics();
	if(alpha != 1)
	    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC,
						       alpha));
	comp.paint(g2);
	g2.dispose();

	return  img;
     }

    /**
     ** Dumps out a Shape's path.
     **/
    public static void printShape(String j, Shape s)
     {
	String str = (s + " ------------------------ " + j);
	PathIterator i = s.getPathIterator(null);
	boolean hasNaN = false;
	while(!i.isDone())
	 {
	    double[] coords = new double[2];
	    switch(i.currentSegment(coords))
	     {
	     case PathIterator.SEG_MOVETO:
		str += "\n" + ("MOVE\t" + coords[0] + "\t" + coords[1]);
		if(Double.isNaN(coords[0])  ||
		   Double.isNaN(coords[1]))
		    hasNaN = true;
		break;

	     case PathIterator.SEG_LINETO:
		str += "\n" + ("LINE\t" + coords[0] + "\t" + coords[1]);
		if(Double.isNaN(coords[0])  ||
		   Double.isNaN(coords[1]))
		    hasNaN = true;
		break;

	     case PathIterator.SEG_CLOSE:
		str += "\n" + ("CLOSE");
		break;

	     default:
		str += "\n" + ("UNKNOWN SEGMENT TYPE!");
		break;
	     }
	    i.next();
	 }
	if(hasNaN)
	    log.aprintln(str);
     }

    /**
     ** Reverses the elements in an array of doubles, in place.
     **/
    public static void reverse(double[] values)
     {
	final int middle = values.length >> 1;
	for(int i=0, j=values.length-1; i<middle; i++, j--)
	 {
	    double tmp = values[i];
	    values[i] = values[j];
	    values[j] = tmp;
	 }
     }

    /**
     ** Clones the portion of an array between fromIndex (inclusive)
     ** and toIndex (exclusive).
     **/
    public static Object cloneArray(Object orig, int fromIndex, int toIndex)
     {
	Class<?> cl = orig.getClass().getComponentType();
	if(cl == null)
	    throw new IllegalArgumentException(
		"First argument to cloneArray must be an array!");

	Object copy = Array.newInstance(cl, toIndex-fromIndex);
	System.arraycopy(orig, fromIndex, copy, 0, toIndex-fromIndex);
	return  copy;
     }

    /**
     ** Splits a single-line string on commas, taking into account
     ** backslash-escaped commas and backslashes.
     **
     ** @throws IllegalArgumentException The input string contains
     ** carriage returns or linefeeds, or any backslashes that aren't
     ** used to escape a backslash or a comma.
     **/
    public static final String[] splitOnCommas(String s)
     {
	if(s.indexOf('\n') != -1  ||  s.indexOf('\r') != -1)
	    throw  new IllegalArgumentException(
		"Carriage returns and linefeeds aren't allowed in " +
		"splitOnCommas() input string.");

	// Replace all double backslashes with a linefeed. There are
	// two levels of escape on both backslashes in the string
	// literal below... Java string escape and regex escape.
	// Annoying, eh? :)
	s = s.replaceAll("\\\\\\\\", "\n");

	// Replace all backslash-comma sequences with a carriage
	// return.
	s = s.replaceAll("\\\\,", "\r");

	if(s.indexOf('\\') != -1)
	    throw  new IllegalArgumentException(
		"Illegal backslash escape sequence.");

	// Do our comma-splitting
	String[] f = s.split(",");

	// Restore the escaped commas and backslashes. Again we have
	// double-escaping due to Java and regex.
	for(int i=0; i<f.length; i++)
	    f[i] = f[i].replaceAll("\n", "\\\\").replaceAll("\r", ",");

	return  f;
     }

    /**
     ** Given an exception, returns a string describing the exceptiona
     ** and all its chained causes. Specifically, it returns
     **
     ** "e1 filler e2 filler e3 ..."
     **
     ** where e1 is e.toString(), e2 is e.getCause().toString(), e3 is
     ** e.getCause().getCause().toString(), and so on until there are
     ** no remaining causes.
     **/
    public static String chainToString(Throwable e, String filler)
     {
	String msg = e.toString();
	while((e = e.getCause()) != null)
	    msg += filler + e;
	return  msg;
     }

    /**
     ** Utility method that creates an exception object of the same
     ** type as the one passed in, with the same message, but with its
     ** cause set as the original. Useful for throwing exceptions
     ** across different threads, while preserving context.
     **
     ** In the event of an error while trying to clone the argument,
     ** the original is returned.
     **/
    public static Throwable chainClone(Throwable orig)
     {
	try
	 {
	    Throwable copy = (Throwable)
		orig.getClass()
		.getDeclaredConstructor(new Class[] { String.class })
		.newInstance(new Object[] { orig.getMessage() });
	    copy.initCause(orig);
	    return  copy;
	 }
	catch(Throwable e)
	 {
	    log.aprintln("-- UNABLE TO CLONE THE FOLLOWING EXCEPTION:");
	    log.aprintln(e);
	    log.aprintln("-- DUE TO THE FOLLOWING PROBLEM:");
	    log.aprintln(orig);
	    return  orig;
	 }
     }

    /**
     ** Utility method that executes a Runnable synchronously on the
     ** AWT thread, no matter who calls it. This form uses a supplied
     ** object as the synchronization object for coordinating with
     ** AWT, instead of AWT's internal objects. THIS IS USEFUL
     ** STRICTLY FOR VERY SPECIFIC MULTI-THREADING ISSUES (SEE
     ** MICHAEL), OTHERWISE USE THE SIMPLER VERSION BELOW THAT DOESN'T
     ** TAKE AN EXTRA OBJECT.
     **
     ** <p>Basically this is a safe version of {@link
     ** SwingUtilities#invokeAndWait}, implemented using {@link
     ** SwingUtilities#invokeLater}.
     **
     ** <p>The implementation of this method invokes lock.notifyAll()
     ** and lock.wait(), thereby relinquishing the object if it's
     ** already locked by the caller (we lock it ourselves here, just
     ** in case it isn't).
     **
     ** @throws RuntimeException If the underlying doRun.run() throws
     ** one.
     **
     ** @throws Error If the underlying doRun.run() throws one.
     **
     ** @throws UndeclaredThrowableException If the underlying
     ** doRun.run() throws an unchecked exception, or if anything else
     ** goes wrong.
     **/
    public static void invokeAndWaitSafely(final Object lock,
					   final Runnable doRun)
     {
	// If we're called from AWT directly, just do the deed and we're done.
	if(SwingUtilities.isEventDispatchThread())
	 {
	    doRun.run();
	    return;
	 }

	// Used to prevent a missed notify, just cause I'm paranoid.
	final boolean[] awtFinished = { false };

	// Used to hold any exceptions thrown from doRun within the
	// AWT thread.
	final Throwable[] exc = { null };

	// Queue doRun to the AWT event-handling thread. This call
	// returns immediately.
	SwingUtilities.invokeLater(
	    new Runnable()
	     {
		// Eventually AWT will execute this for us, when it
		// feels like it.
		public void run()
		 {
		    try
		     {
			doRun.run();
		     }
		    catch(Throwable e)
		     {
			exc[0] = e;
		     }

		    // Okay, AWT has executed it for us. Time to allow
		    // the wait() in code further below to unblock.
		    synchronized(lock)
		     {
			awtFinished[0] = true;
			lock.notifyAll();
		     }
		 }
	     }
	    );

	try
	 {
	    synchronized(lock)
	     {
		// Block until AWT has found time to execute our code
		// up above. We unblock once notifyAll() is called
		// above.
		while(!awtFinished[0])
		    lock.wait(500); // half-sec timeout, just in case
	     }
	 }
	catch(InterruptedException e)
	 {
	    log.aprintln(e);
	 }

	// Finally, behave just as if we'd executed doRun.run()
	// without any threading stuff... if it threw an exception,
	// then so do we (of the same type if it's an unchecked
	// exception)!
	if(exc[0] != null)
	 {
	    if(exc[0]
	       instanceof RuntimeException)
		throw (RuntimeException) chainClone(exc[0]);

	    if(exc[0]
	       instanceof Error)
		throw (Error) chainClone(exc[0]);

	    throw  new UndeclaredThrowableException(exc[0]);
	 }
     }

    /**
     ** Utility method that executes a Runnable synchronously on the
     ** AWT thread, no matter who calls it. If called within the AWT
     ** even dispatch thread, then the Runnable is invoked in the
     ** current thread. If called on any other thread, the Runnable is
     ** passed to {@link SwingUtilities#invokeAndWait}.
     **
     ** <p>Basically this is a safe version of {@link
     ** SwingUtilities#invokeAndWait}.
     **
     ** @throws UndeclaredThrowableException if invokeAndWait throws
     ** an exception.
     **/
    public static void invokeAndWaitSafely(Runnable doRun)
     {
	if(SwingUtilities.isEventDispatchThread())
	    doRun.run();
	else
	    try
	     {
		SwingUtilities.invokeAndWait(doRun);
	     }
	    catch(InterruptedException e)
	     {
		throw new UndeclaredThrowableException(e);
	     }
	    catch(InvocationTargetException e)
	     {
		throw new UndeclaredThrowableException(e);
	     }
     }

    /**
      * Given an array of longitude/latitude points representing the
      * vertices of a spherical polygon, returns the surface area of
      * the polygon. Assumes a unit sphere. The polygon is assumed to
      * be closed, so there is no need to pass a copy of the first
      * vertex as the last vertex. This method also requires all of the
      * vertices to be distinct and non-self-intersecting.
      *
      * <p>To convert the result to square kilometers, assuming a
      * spherical body with radius Util.radius (in km)
	  * (the average of the polar and any equatorial ellipsoid radii),
	  * use the following:
      *
      * <pre>unitArea = Util.sphericalArea(...);
      *      kmArea = unitArea * Util.radius * Util.radius;   </pre>
      */
    public static double sphericalArea(Point2D[] polygonLL)
     {
	HVector[] polygonV = new HVector[polygonLL.length];
	for(int i=0; i<polygonLL.length; i++)
	    polygonV[i] = new HVector(polygonLL[i]);
	return  sphericalArea(polygonV);
     }

    /**
      * Given an array of vectors representing the vertices of a
      * spherical polygon, returns the surface area of the
      * polygon. Assumes a unit sphere. The polygon is assumed to be
      * closed, so there is no need to pass a copy of the first vertex
      * as the last vertex. This method also requires all of the
      * vertices to be distinct and non-self-intersecting.
      *
      * <p>To convert the result to square kilometers, assuming a
      * spherical body with radius Util.radius (in km)
	  * (the average of the polar and any equatorial ellipsoid radii),
	  * use the following:
      *
      * <pre>unitArea = Util.sphericalArea(...);
      *      kmArea = unitArea * Util.radius * Util.radius;   </pre>
      */
    public static double sphericalArea(HVector[] polygonV)
     {
	// Calculating the area of a spherical polygon is actually
	// easier than doing so on the plane, using what's called the
	// spherical excess formula. If the polygon has N vertices,
	// then its area is equal to the sum of the interior angles,
	// minus (N-2)*PI.
	double area = 0;
	for(int i=0; i<polygonV.length; i++)
	    area += sphericalAngle(polygonV[ i                     ],
				   polygonV[(i+1) % polygonV.length],
				   polygonV[(i+2) % polygonV.length]);
	area -= (polygonV.length-2) * Math.PI;

	// The one catch: depending on whether the polygon winds
	// clockwise or counter-clockwise, we may have just summed the
	// internal angles OR we might have summed the EXTERNAL
	// angles! If so, we wound up with an area greater than half
	// the sphere... so just invert.
	if(area > 2 * Math.PI)
	    area = 4*Math.PI - area;

	return  area;
     }

    /**
     ** Returns the counter-clockwise angle (in radians) created by
     ** vertex abc on the unit sphere's surface. The returned value is
     ** always between 0 and 2*PI (i.e. it's never negative).
     **/
    public static double sphericalAngle(HVector a, HVector b, HVector c)
     {
	HVector bb = b.unit();

	// Get the sides of the vertex
	HVector ba = a.sub(b);
	HVector bc = c.sub(b);

	// Project the sides into the plane tangent at b
	ba.subEq(bb.mul(bb.dot(ba)));
	bc.subEq(bb.mul(bb.dot(bc)));

	// Finally, take the signed angle between them, around b
	double angle = ba.separation(bc, b);

	// "Un-sign" the angle
	return  angle>0 ? angle : angle+2*Math.PI;
     }
    
    /**
     * Compute the angular and linear distances between two world points p1 and p2.
     * This code was lifted from GridLView which puts a distance value
     * in the ruler when mouse is dragged in the panner.
     * @return angular-distance (degrees), linear-distance (km)
     */
    public static double[] angularAndLinearDistanceW(Point2D p1, Point2D p2, MultiProjection proj){
    	p1 = proj.world.toSpatial(p1);
    	p2 = proj.world.toSpatial(p2);
    	return angularAndLinearDistanceS(p1, p2, proj);
    }

    /**
     * Compute the angular and linear distances between two spatial points p1 and p2.
     * This code was lifted from GridLView which puts a distance value
     * in the ruler when mouse is dragged in the panner.
     * @return angular-distance (degrees), linear-distance (km)
     */
    public static double[] angularAndLinearDistanceS(Point2D p1, Point2D p2, MultiProjection proj){
		double angDistance = proj.spatial.distance(p1, p2);
		double linDistance = angDistance * MEAN_RADIUS * 2*Math.PI / 360.0;
		return new double[]{ angDistance, linDistance};
    }
    
	/**
	 * Given an array array of <code>N</code> points that describe
	 * <code>N-1</code> line segments, this method computes the angular and
	 * linear distance traveled by the world coordinate lines. Unlike the other
	 * distance methods in Util, this method computes internal points within the
	 * world coordinate line so that the distance measurement is taken along the
	 * world coordinate line, as opposed to measuring the great circle between a
	 * world coordinate line's endpints.
	 * 
	 * @param points
	 *            The array of points in world coordinates.
	 * @return An array of two elements, the angular distance in degrees and the
	 *         distance along the ellipsoid in kilometers.
	 */
    public static double[] angularAndLinearDistanceWorld(Point2D ... points) {
		final double maxSep = Math.toRadians(5);
		HVector lastUnit = null, last = null;
		double km = 0;
		double degs = 0;
		for (Point2D p: points) {
			HVector vUnit = new HVector(Main.PO.convWorldToSpatial(p));
			HVector v = HVector.intersectMars(HVector.ORIGIN, vUnit);
			if (lastUnit != null) {
				HVector norm = lastUnit.cross(vUnit);
				double sep = lastUnit.separation(vUnit, norm);
				HVector previous = last;
				for (double theta = maxSep/2; theta < sep + maxSep/2; theta += maxSep) {
					HVector current;
					if (theta >= sep - maxSep/2) {
						current = v;
					} else {
						current = HVector.intersectMars(HVector.ORIGIN, lastUnit.rotate(norm, theta));
					}
					degs += Math.toDegrees(previous.separation(current));
					km += current.sub(previous).norm();
					previous = current;
				}
			}
			lastUnit = vUnit;
			last = v;
		}
		return new double[]{degs, km};
    }
    
    /**
     ** Given an ET, returns an epoch-based approximation to Ls, also
     ** known as "solar longitude" or "heliocentric longitude". The
     ** returned value is in degrees.
     **
     ** <p>This routine uses a hand-fitted curve that was shown to
     ** approximate kernel-based calculations for the years 2000 to
     ** 2050. Over that time period, the error is always within 0.4
     ** degrees. The error is even less during the years 2002 to 2018,
     ** under 0.26 degrees.
     **/
    public static double lsubs(double et)
     {
	final double A = 59340000.0;

	return  (et+46090000)%A * 360.0 / A
	    + Math.sin(et*2*Math.PI/A + .4) * 10.75
	    + 6*Math.sin( -2*LSUBS.scale(et) - .3 ) / 9
	    - Math.pow( Math.sin(LSUBS.scale(et))+1, 4) / 9
	    - 3 * et/2000000000.0
	    - 8.75;
     }

    /**
     ** Internal class, used to test and implement the {@link
     ** Util#lsubs} function.
     **/
    private static class LSUBS
     {
	/**
	 ** Shorthand scaling function for {@link Util#lsubs}.
	 **/
	private static double scale(double et)
	 {
	    final double A = 59340000.0;

	    return  et / A * 2 * Math.PI + 2;
	 }

	/**
	 ** Test driver.
	 **/
	public static void main(String[] av)
	 {
	    double et0 = Double.parseDouble(av[0]);
	    int delta = Integer.parseInt(av[1]);
	    long len = Long.parseLong(av[2]);

	    for(long i=0; i<len; i+=delta)
	     {
		double et = et0 + i;
		System.out.println(et + "\t" + lsubs(et));
	     }
	 }
     }

    /**
     ** Convenience method for URL-encoding a string, a la {@link
     ** URLEncoder}, in compliance with W3C recommendations..
     **/
    public static String urlEncode(String s)
     {
	try
	 {
	    return  URLEncoder.encode(s, "UTF-8");
	 }
	catch(UnsupportedEncodingException e)
	 {
	    // Should never occur, UTF-8 is required to be supported
	    log.aprintln("THIS SHOULDN'T BE HAPPENING!");
	    log.aprintln(e);
	    throw  new Error("The UTF-8 encoding is failing", e);
	 }
     }

    /**
     ** Returns the same color as the given one, but with the alpha
     ** value set to the given value.
     **/
    public static Color alpha(Color col, int alpha)
     {
	return  new Color(col.getRGB()&0xFFFFFF | (alpha<<24), true);
     }

    /**
     ** Convenience method: given a long, returns a hexadecimal string
     ** of its bits. All hex characters are uppercase. Treats the
     ** argument as an unsigned number.
     **/
    public static String toHex(long n)
     {
	return  Long.toHexString(n).toUpperCase();
     }

    /**
     ** Convenience method: given an integer, returns a hexadecimal
     ** string of its bits. All hex characters are uppercase. Treats
     ** the argument as an unsigned number.
     **/
    public static String toHex(int n)
     {
	return  Integer.toHexString(n).toUpperCase();
     }

	/**
	 * Returns a new URI based on the given base but with the given args added
	 * onto the end of the query portion of the URI
	 */
	public static final URI getSuffixedURI(URI baseURI, String ... args) {
		try {
			String query = baseURI.getQuery();
			if (query == null)
				query = "";
			if (query.length() > 0 && !query.endsWith("&"))
				query += "&";
			query += Util.join("&", args);
			return new URI(baseURI.getScheme(), baseURI.getUserInfo(), baseURI.getHost(),
				baseURI.getPort(), baseURI.getPath(), query, baseURI.getFragment());
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not construct modified URI: " + e.getMessage(), e);
		}
	}
	
    /** Returns an array of all lines from the given stream */
    public static String[] readLines(InputStream is) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		List<String> lines = new ArrayList<String>();
		String buff;
		while ((buff = br.readLine()) != null)
			lines.add(buff);
		return lines.toArray(new String[lines.size()]);
	}
    
    /**
	 * For every non-empty line in the given stream, returns an array of
	 * whitespace-separated tokens of length at most maxCount.
	 */
    public static String[][] readLineTokens(InputStream is, int maxCount)
     throws IOException
     {
	String[] lines = readLines(is);
	List<Object> tokens = new ArrayList<Object>(lines.length);
	for(int i=0; i<lines.length; i++)
	    if(lines[i].trim().length() != 0)
		tokens.add(lines[i].split("\\s+", maxCount));
	return	(String[][]) tokens.toArray(new String[0][]);
     }

    /**
     ** For every non-empty line in the given stream, returns an array
     ** of whitespace-separated tokens.
     **/
    public static String[][] readLineTokens(InputStream is)
     throws IOException
     {
	return  readLineTokens(is, 0);
     }

    /**
     ** Determines whether or not a string can be parsed as a
     ** base-10 integer.
     **/
    public static boolean isInteger(String s)
     {
	try
	 {
	    Integer.parseInt(s);
	    return  true;
	 }
	catch(NumberFormatException e)
	 {
	    return  false;
	 }
     }

    /**
     ** Determines whether or not a string can be parsed as a
     ** base-10 real.
     **/
    public static boolean isDouble(String s)
     {
	try
	 {
	    Double.parseDouble(s);
	    return  true;
	 }
	catch(NumberFormatException e)
	 {
	    return  false;
	 }
     }

    /**
     ** Java imitation of Perl's <code>split</code> operator. Given a
     ** string containing tokens separated by sequences of
     ** single-character delimiters, returns an array of tokens.
     **
     ** @param delims The characters of this string are taken to be
     ** the delimeters.
     ** @param str The string to split into tokens.
     **/
    public static String[] split(String delims, String str)
     {
	StringTokenizer tok = new StringTokenizer(str, delims);
	String[] tokens = new String[tok.countTokens()];
	for(int i=0; i<tokens.length; i++)
	    tokens[i] = tok.nextToken();
	return	tokens;
     }
    
    /**
     * Trims the given character from the beginning and end of the string
     * @param str The string you wish to trim the character from
     * @param chr The character you wish to trim from the string
     * @param left pass true if the character should be removed from the left of the string
     * @param right pass true if the character should be removed from the right of the string
     * @return a trimmed String
     */
    public static final String trim(String str, char chr, boolean left, boolean right){
    	if (str==null) return null;
    	int start = 0;
    	int end = str.length();
    	char[] charArray = str.toCharArray();
    	if(left){
    		while (start<end && (charArray[start]==chr)){
        		start++;
        	}
    		if (start==end) return "";
    	}
    	if(right){
    		while (end>start && (charArray[end-1]==chr)){
        		end--;
        	}
    	}
    	
    	return str.substring(start,end);
    }
    
    /**
     * Trims the given character from the beginning and end of the string
     * @param str The string you wish to trim the character from
     * @param chr The character you wish to trim from the string
     * @return a trimmed String
     */
    public static final String trim(String str, char chr){
    	return Util.trim(str, chr, true, true);
    }
    
    /**
     * Trims the given character from the end of the string
     * @param str The string you wish to trim the character from
     * @param chr The character you wish to trim from the string
     * @return
     */
    public static final String rTrim(String str, char chr){
    	return Util.trim(str, chr, false, true);
    }
    
    /**
     * Trims the given character from the begining of the string
     * @param str The string you wish to trim the character from
     * @param chr The character you wish to trim from the string
     * @return
     */
    public static final String lTrim(String str, char chr){
    	return Util.trim(str, chr, true, false);
    }

    /**
	 * Java version of Perl's <code>join</code> operator. Returns a single
	 * string composed of <code>items</code> separated by
	 * <code>between</code>.
	 */
	public static <E extends Object> String join(String between, E ... items) {
		StringBuffer joined = new StringBuffer();
		for (int i = 0; i < items.length; i++){
			if (i > 0)
				joined.append(between);
			joined.append(items[i]);
		}
		return joined.toString();
	}
    
    public static String join(String between, Collection list) {
    	StringBuffer joined = new StringBuffer();
    	int i=0;
    	for(Iterator it=list.iterator(); it.hasNext(); ){
    		if (i > 0)
    			joined.append(between);
    		joined.append(it.next().toString());
    		i++;
    	}
    	return joined.toString();
    }
    
	public static String join(String between, double[] items) {
		StringBuffer joined = new StringBuffer();
		for (int i = 0; i < items.length; i++){
			if (i > 0)
				joined.append(between);
			joined.append(items[i]);
		}
		return joined.toString();
	}
    
	public static String join(String between, int[] items) {
		StringBuffer joined = new StringBuffer();
		for (int i = 0; i < items.length; i++){
			if (i > 0)
				joined.append(between);
			joined.append(items[i]);
		}
		return joined.toString();
	}
    
    /**
     * Escape an SQL string. Every instance of single-quote which appears
     * in the passed string is converted into a pair of single-quotes.
     */
    public static String sqlEscape(String s){
    	if (s == null)
    		return null;
    	
    	StringBuffer sbuf = new StringBuffer();
    	for(int i=0; i < s.length(); i++){
    		if (s.charAt(i) == '\'')
    			sbuf.append("''");
    		else
    			sbuf.append(s.charAt(i));
    	}
    	return sbuf.toString();
    }
    
    /**
     * Escapes every SQL string in the given array using {@link #sqlEscape(String)}.
     */
    public static String[] sqlEscape(String s[]){
    	if (s == null)
    		return null;
    	
    	String[] out = new String[s.length];
    	for(int i=0; i < s.length; i++)
    		out[i] = sqlEscape(s[i]);
    	
    	return out;
    }
    
    /**
     * Encloses each individual item of the items array with the specified character.
     * For example: <pre>{abc, def, ghi}</pre> will become <pre>{'abc','def','ghi'}</pre>
     * when enclose is called with {abc,def,ghi} items and the single-quote character.
     */
    public static String[] enclose(String[] items, char c){
    	if (items == null)
    		return null;
    	
    	String[] modified = new String[items.length];
    	
    	for(int i=0; i < modified.length; i++)
    		modified[i] = c+items[i]+c;
    	
    	return modified;
    }

    /**
     ** Performs a {@link JFileChooser#showSaveDialog}, but confirms
     ** with the user if the file exists. If the user cancels at any
     ** stage, the "current selected file" of the file chooser is
     ** restored to its starting value, as if this function never took
     ** place.
     * @param extension If the file the user chooses doesn't end in
     ** extension, then this string is appended to the filename. Can
     ** be null (in which case there is no effect).
     **
     ** @return Whether or not the user actually settled on a file
     ** (false indicates a cancellation at some step).
     **/
    public static boolean showSaveWithConfirm(JFileChooser fc,
					      String extension)
     {
	File old = fc.getSelectedFile();
	File f;
	do
	 {
	    if(Util.showSaveDialog(fc)
	       != JFileChooser.APPROVE_OPTION)
	     {
		fc.setSelectedFile(old);
		return	false;
	     }

	    f = fc.getSelectedFile();
	    if(extension != null)
	     {
		String fname = fc.getSelectedFile().toString();
		if(!fname.endsWith(extension))
		 {
		    fname += extension;
		    fc.setSelectedFile(f = new File(fname));
		 }
	     }
	    if(f.exists())
	     {
		switch(
		    Util.showConfirmDialog(
			"File already exists, overwrite?\n\n" + f + "\n\n",
			"FILE EXISTS",
			JOptionPane.YES_NO_CANCEL_OPTION,
			JOptionPane.WARNING_MESSAGE)
		    )
		 {
		 case JOptionPane.YES_OPTION:
		    break; // Do nothing, we'll exit the while() loop

		 case JOptionPane.NO_OPTION:
		    f = null; // Go for another round in the while() loop
		    break;

		 case JOptionPane.CLOSED_OPTION:
		 case JOptionPane.CANCEL_OPTION:
		 default:
		    fc.setSelectedFile(old);
		    return  false; // Stop the save operation altogether
		 }
	     }
	 }
	while(f == null);

	return	true;
     }

    public static void recursiveRemoveDir(File directory)
     {
	String[] filelist = directory.list();
	if(filelist == null)
	    return;
	for(int i=0; i<filelist.length; i++)
	 {
	    File tmpFile = new File(directory.getAbsolutePath(),filelist[i]);
	    if(tmpFile.isDirectory())
	       recursiveRemoveDir(tmpFile);
	    else
		tmpFile.delete();
	 }
	directory.delete();
     }

    public static String getHomeFilePath(String fname)
     {
	String fpath = "";

	String home = System.getProperty("user.home");
	if ( home != null )
	 {
	    fpath += home;
	    fpath += System.getProperty("file.separator");
	 }

	fpath += fname;

	return fpath;
     }

    /** Converts a latitude value given as an "ographic" coordinate reference
     ** to an "ocentric" one.
     ** 
     ** @param ographic latitude in degrees
     **/
    public static double ographic2ocentric(double ographic)
     {
	return atanD( G2C_SCALAR * tanD(ographic) );
     }

    /** Converts a latitude value given as an "ocentric" coordinate reference
     ** to an "ographic" one.
     ** 
     ** @param ocentric latitude in degrees
     **/
    public static double ocentric2ographic(double ocentric)
     {
	return atanD( tanD(ocentric) / G2C_SCALAR );
     }

    /**
     ** Like {@link Math#tan}, but takes degrees.
     **/
    public static double tanD(double degs)
     {
	return  Math.tan(Math.toRadians(degs));
     }

    /**
     ** Like {@link Math#atan}, but returns degrees.
     **/
    public static double atanD(double x)
     {
	return  Math.toDegrees(Math.atan(x));
     }

    /**
     ** Returns the distance between the sun and mars at a particular
     ** time, in units of AU. Returns zero on error.
     **
     ** <p>This function is a cut+paste translation of stuff from <a
     ** href="http://hem.passagen.se/pausch/comp/ppcomp.html">this
     ** website</a>.
     **/
    public static double getMarsSunDistance(double et)
     {
		 // TODO: make getSunDistance() and set a body parameter for the orbit equations to use in jmars.config
	try
	 {
	    TimeCache tc = TimeCacheFactory.instance().getTimeCacheInstance("ODY");

	    StringTokenizer tok = new StringTokenizer(
		UTC_DF.format(tc.et2date(et)));

	    // Web site, section 3: The time scale

	    int y = Integer.parseInt(tok.nextToken());
	    int m = Integer.parseInt(tok.nextToken());
	    int D = Integer.parseInt(tok.nextToken());
	    double UT = Integer.parseInt(tok.nextToken());	   // hours
	    UT += Integer.parseInt(tok.nextToken()) / 60.0;	   // mins
	    UT += Integer.parseInt(tok.nextToken()) / 60.0 / 60.0; // secs

	    double d = 367*y - 7 * ( y + (m+9)/12 ) / 4 + 275*m/9 + D - 730530;
	    d = d + UT / 24.0;

	    // Web site, section 4: The orbital elements

	    double a = 1.523688; // (AU)
	    double e = 0.093405 + 2.516E-9 * d;
	    double M = Math.toRadians(18.6021 + 0.5240207766 * d);

	    // Web site, section 6: The position of the Moon and of the planets

	    double E0;
	    double E1 = M + e * Math.sin(M) * ( 1.0 + e * Math.cos(M) );
	    do
	     {
		E0 = E1;
		E1 = E0 -
		    ( E0 - e * Math.sin(E0) - M ) / ( 1 - e * Math.cos(E0) );
		log.println("E = " + E0);
		log.println("	 " + E1);
	     }
	    while(Math.abs(E0 - E1) >= Math.toDegrees(0.001));
	    double E = E1;

	    double xv = a * ( Math.cos(E) - e );
	    double yv = a * ( Math.sqrt(1.0 - e*e) * Math.sin(E) );
	    double r = Math.sqrt( xv*xv + yv*yv );

	    // All done!

	    return  r;
	 }
	catch(Throwable e)
	 {
	    log.aprintln(e);
	    log.aprintln("UNABLE TO COMPUTE MARS-SUN DISTANCE AT ET=" + et);
	    return  0;
	 }
     }
    // This differs from TimeCache.UTC_DF
    private static final SimpleDateFormat UTC_DF = new SimpleDateFormat(
	"yyyy MM dd HH mm ss");

    /**
     ** Interface for modifying a single coordinate of a {@link
     ** PathIterator}.
     **/
    private static interface CoordModifier
     {
	/**
	 ** @param coords The coordinate array returned by a shape's
	 ** {@link PathIterator}.
	 ** @param count The number of coordinates in the array (as
	 ** determined by the point type of the {@link PathIterator}.
	 **/
	public void modify(float[] coords, int count);
     }

    /**
     ** Given a shape, iterates over it and performs the given
     ** coordinate modification to every point in the shape.
     **/
    private static Shape modify(Shape s, CoordModifier cm)
     {
	GeneralPath gp = new GeneralPath();
	PathIterator iter = s.getPathIterator(null);
	float[] coords = new float[6];

	// NOTE: No loss of precision in coords. All of the
	// GeneralPath.foobarTo() methods take FLOATS and not doubles.

	while(!iter.isDone())
	 {
	    switch(iter.currentSegment(coords))
	     {

	     case PathIterator.SEG_CLOSE:
		gp.closePath();
		break;

	     case PathIterator.SEG_LINETO:
		cm.modify(coords, 2);
		gp.lineTo(coords[0], coords[1]);
		break;

	     case PathIterator.SEG_MOVETO:
		cm.modify(coords, 2);
		gp.moveTo(coords[0], coords[1]);
		break;

	     case PathIterator.SEG_QUADTO:
		cm.modify(coords, 4);
		gp.quadTo(coords[0], coords[1],
			  coords[2], coords[3]);
		break;

	     case PathIterator.SEG_CUBICTO:
		cm.modify(coords, 6);
		gp.curveTo(coords[0], coords[1],
			   coords[2], coords[3],
			   coords[4], coords[5]);
		break;

	     default:
		log.aprintln("INVALID GENERALPATH SEGMENT TYPE!");

	     }
	    iter.next();
	 }
	return	gp;
     }

    // Quick hack to allow verbatim code-reuse from GraphicsWrapped.java
    private static final double mod = 360;

    /**
     ** Performs the modulo operation on a shape's coordinates.
     **/
    private static final CoordModifier cmModulo =
	new CoordModifier()
	 {
	    public void modify(float[] coords, int count)
	     {
		for(int i=0; i<count; i+=2)
		    coords[i] -= Math.floor(coords[i]/mod)*mod;
	     }
	 };

    /**
     ** Takes care of wrap-around on a shape's coordinates.
     **/
    private static final CoordModifier cmWrapping =
	new CoordModifier()
	 {
	    public void modify(float[] coords, int count)
	     {
		for(int i=0; i<count; i+=2)
		    if(coords[i] < mod/2)
			coords[i] += mod;
	     }
	 };
	 
	 /** Return the input world x value normalized into the 0-360 range */
	 public static final double mod360(double x) {
		 x -= Math.floor(x / 360.0) * 360.0;
		 return x;
	 }
	 
	 /**
	  * Given a lon/lat rectangle, returns a new rectangle with the same lat
	  * values, and the longitude value switched from east to west or west to
	  * east.
	  */
	 public static Rectangle2D swapRect(Rectangle2D rect) {
		 return new Rectangle2D.Double(
			 Util.mod360(-rect.getMaxX()),
			 rect.getMinY(),
			 rect.getWidth(),
			 rect.getHeight());
	 }
	
    /**
     ** ONLY FOR CYLINDRICAL: Given a shape in world coordinates,
     ** "normalizes" it. This ensures that its left-most x coordinate
     ** is within the x-range [0:360], and that there is no
     ** wrap-around (that is, the shape simply pushes past 360).
     **/
	 public static Shape normalize360(Shape s)
	 {
		 Rectangle2D bounds = s.getBounds2D();
		 double x = bounds.getMinX();
		 if(x < 0  ||  x >= mod)
			 s = modify(s, cmModulo);

		 if(bounds.getWidth() >= mod/2)
			 s = modify(s, cmWrapping);

		 return	s;
	 }

	/**
	 * Normalize the given vertices w.r.t. to the first vertex.
	 * On return all the points will be within 180 degrees of the
	 * first vertex.
	 * <u>This method behaves differently than {@link Util#normalize360(Shape)}</u>
	 * in the following ways:
	 * <b>
	 * <ul>
	 * <li> The starting point of the transformation is the first input point,
	 * not the point with minimum x-value.
	 * <li> Shapes that may be bigger than 180 degrees are not refolded/wrapped-around.
	 * </ul> 
	 * 
	 * @param v Non null array of vertices.
	 * @return Vertices normalized to stay within 180 degress of the first vertex.
	 */
	public static Point2D[] normalize360(Point2D[] v){
		Point2D.Double[] n = new Point2D.Double[v.length];
		
		if (v.length < 1)
			return n;
		
		n[0] = new Point2D.Double(v[0].getX(), v[0].getY());
		double anchor = n[0].x;
		for(int i=1; i<v.length; i++){
			n[i] = new Point2D.Double(v[i].getX(), v[i].getY());
			if (Math.abs(anchor - n[i].x) > 180.0){
				n[i].x += Math.round((anchor - n[i].x) / 360.0) * 360.0;
			}
		}
		return n;
	}

    /**
     ** Given a list of shapes that have been {@link
     ** #normalize360}-ified, and a rectangle, returns a list of the
     ** shapes that intersect that rectangle in modulo-360
     ** coordinates. May return an empty array, but will never return
     ** null.
     **/
    public static int[] intersects360(Rectangle2D rect, Shape[] shapes)
     {
	// Normalize the original rectangle
	Rectangle2D.Double r1 = new Rectangle2D.Double();
	r1.setFrame(rect);
	if(r1.width > 180)
	 {
	    r1.width -= Math.floor(r1.width/360) * 360;
	    r1.width = 360 - r1.width;
	    r1.x -= r1.width;
	 }
	r1.x -= Math.floor(r1.x / 360) * 360;

	// Create the second rectangle, to catch shapes that cross 0/360
	Rectangle2D.Double r2 = (Rectangle2D.Double) r1.clone();
	r2.x += r2.x<180 ? 360 : -360;

	// Find all the intersections
	int count = 0;
	int[] found = new int[shapes.length];
	for(int i=0; i<shapes.length; i++)
	    if(shapes[i].intersects(r1) ||
	       shapes[i].intersects(r2) )
		found[count++] = i;

	// Return the intersections
	int[] found2 = new int[count];
	System.arraycopy(found, 0, found2, 0, count);
	return  found2;
     }

    /**
     ** Given a list of shapes that have been {@link
     ** #normalize360}-ified, and a rectangle, returns a list of the
     ** shapes that intersect that rectangle in modulo-360
     ** coordinates. May return an empty array, but will never return
     ** null.
     **
     ** @param shapes should contain only {@link Shape} objects.
     **/
    public static int[] intersects360(Rectangle2D rect, Collection shapes)
     {
	// Normalize the original rectangle
	Rectangle2D.Double r1 = new Rectangle2D.Double();
	r1.setFrame(rect);
	if(r1.width > 180)
	 {
	    r1.width -= Math.floor(r1.width/360) * 360;
	    r1.width = 360 - r1.width;
	    r1.x -= r1.width;
	 }
	r1.x -= Math.floor(r1.x / 360) * 360;

	// Create the second rectangle, to catch shapes that cross 0/360
	Rectangle2D.Double r2 = (Rectangle2D.Double) r1.clone();
	r2.x += r2.x<180 ? 360 : -360;

	// Find all the intersections
	int count = 0;
	int[] found = new int[shapes.size()];
	int i = 0;
	for(Iterator iter=shapes.iterator(); iter.hasNext(); i++)
	 {
	    Shape sh = (Shape) iter.next();
	    if(sh.intersects(r1) ||
	       sh.intersects(r2) )
		found[count++] = i;
	 }

	// Return the intersections
	int[] found2 = new int[count];
	System.arraycopy(found, 0, found2, 0, count);
	return  found2;
     }

    /**
     ** Given a list of shapes that have been {@link
     ** #normalize360}-ified, and a rectangle, returns a list of the
     ** shapes that are contained in the specified rectangle in modulo-360
     ** coordinates. May return an empty array, but will never return
     ** null.
     **/
    public static int[] contains360(Rectangle2D rect, Shape[] shapes)
     {
		// Normalize the original rectangle
		Rectangle2D.Double r1 = new Rectangle2D.Double();
		r1.setFrame(rect);
		if(r1.width > 180)
		 {
		    r1.width -= Math.floor(r1.width/360) * 360;
		    r1.width = 360 - r1.width;
		    r1.x -= r1.width;
		 }
		r1.x -= Math.floor(r1.x / 360) * 360;
	
		// Create the second rectangle, to catch shapes that cross 0/360
		Rectangle2D.Double r2 = (Rectangle2D.Double) r1.clone();
		r2.x += r2.x<180 ? 360 : -360;
	
		// Find all the intersections
		int count = 0;
		int[] found = new int[shapes.length];
		for(int i=0; i<shapes.length; i++)
		    if(r1.contains(shapes[i].getBounds2D()) ||
		       r2.contains(shapes[i].getBounds2D()))
			found[count++] = i;
	
		// Return the intersections
		int[] found2 = new int[count];
		System.arraycopy(found, 0, found2, 0, count);
		return  found2;
     }
    
    /**
     ** Returns the floor() of the base-2 logarithm of its argument,
     ** FOR EXACT POWERS OF TWO.
     **/
    public static final int log2(int x)
     {
	return	(int) Math.round(Math.log(x) / Math.log(2));
     }

    /**
     ** Returns the sign of its argument.
     **/
    public static final int sign(double x)
     {
	if(x < 0)
	    return  -1;
	if(x > 0)
	    return  +1;
	return	0;
     }

    /**
     ** Returns the sign of its argument.
     **/
    public static final int sign(int x)
     {
	if(x < 0)
	    return  -1;
	if(x > 0)
	    return  +1;
	return	0;
     }

    /**
     ** Returns whether or not val lies (inclusively) between min and
     ** max.
     **/
    public static final boolean between(int min, int val, int max)
     {
	return	val >= min  &&	val <= max;
     }

    /**
     ** Returns whether or not val lies (inclusively) between min and
     ** max.
     **/
    public static final boolean between(double min, double val, double max)
     {
	return	val >= min  &&	val <= max;
     }
    
    /** Returns true if the range (minA,maxA) completely encloses the range (minB, maxB) */
    public static final boolean encloses(double minA, double maxA, double minB, double maxB) {
    	return Util.between(minA, minB, maxA) && Util.between(minA, maxB, maxA);
    }
    
    /**
     ** Handy util method for combination min/max bounding
     ** calls. Equivalent to <code>Math.min(Math.max(value, min),
     ** max)</code>.
     **/
    public static final int bound(int min, int value, int max)
     {
	if(value <= min)
	    return  min;
	if(value >= max)
	    return  max;
	return	value;
     }

    /**
     ** Handy util method for combination min/max bounding
     ** calls. Equivalent to <code>Math.min(Math.max(value, min),
     ** max)</code>. Hasn't been checked for infinity and nan
     ** handling.
     **/
    public static final double bound(double min, double value, double max)
     {
	if(value <= min)
	    return  min;
	if(value >= max)
	    return  max;
	return	value;
     }

    /**
     ** Given an array, returns a duplicate of that array with one
     ** change: an element is inserted at index <code>idx</code>.
     ** Thus the returned array has one more element than the supplied
     ** array.
     **
     ** @param src The array to which an element is added. The
     ** argument is compile-time-typed to "Object" to allow for arrays
     ** of primitives to be passed.
     **/
    public static Object insElement(Object src, int idx)
     {
	int srclen = Array.getLength(src);
	Object dst = Array.newInstance(src.getClass().getComponentType(),
				       srclen + 1);
	System.arraycopy(src, 0,
			 dst, 0,
			 idx);
	System.arraycopy(src, idx,
			 dst, idx+1,
			 srclen-idx);
	return	dst;
     }

    /**
     ** Given an array, returns a duplicate of that array with one
     ** change: the element at index <code>idx</code> is deleted. Thus
     ** the returned array has one less element than the supplied
     ** array.
     **
     ** @param src The array from which to delete an element. The
     ** argument is compile-time-typed to "Object" to allow for arrays
     ** of primitives to be passed.
     **/
    public static Object delElement(Object src, int idx)
     {
	int srclen = Array.getLength(src);
	Object dst = Array.newInstance(src.getClass().getComponentType(),
				       srclen - 1);
	System.arraycopy(src, 0,
			 dst, 0,
			 idx);
	System.arraycopy(src, idx+1,
			 dst, idx,
			 srclen-idx-1);
	return	dst;
     }

    /**
     ** Given an array of primitive-wrapper objects, returns an
     ** equivalent array of primitive elements with the same
     ** values. For instance, supplying an array of {@link Integer}
     ** objects returns an array of <code>int</code> values. If null
     ** is passed in, null is returned.
     **/
    public static Object toPrimitive(Object[] src)
     {
	if(src == null)
	    return  null;

	int len = src.length;
	Class wrapType = src.getClass().getComponentType();

	if(wrapType == Boolean.class)
	 {
	    Boolean[] srcW = (Boolean[]) src;
	    boolean[] dst = new boolean[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].booleanValue();
	    return  dst;
	 }

	if(wrapType == Character.class)
	 {
	    Character[] srcW = (Character[]) src;
	    char[] dst = new char[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].charValue();
	    return  dst;
	 }

	if(wrapType == Byte.class)
	 {
	    Byte[] srcW = (Byte[]) src;
	    byte[] dst = new byte[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].byteValue();
	    return  dst;
	 }

	if(wrapType == Short.class)
	 {
	    Short[] srcW = (Short[]) src;
	    short[] dst = new short[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].shortValue();
	    return  dst;
	 }

	if(wrapType == Integer.class)
	 {
	    Integer[] srcW = (Integer[]) src;
	    int[] dst = new int[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].intValue();
	    return  dst;
	 }

	if(wrapType == Long.class)
	 {
	    Long[] srcW = (Long[]) src;
	    long[] dst = new long[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].longValue();
	    return  dst;
	 }

	if(wrapType == Float.class)
	 {
	    Float[] srcW = (Float[]) src;
	    float[] dst = new float[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].floatValue();
	    return  dst;
	 }

	if(wrapType == Double.class)
	 {
	    Double[] srcW = (Double[]) src;
	    double[] dst = new double[len];
	    for(int i=0; i<len; i++)
		dst[i] = srcW[i].doubleValue();
	    return  dst;
	 }

	throw  new ClassCastException("Not a primitive-wrapper class: " +
				      wrapType.getName());
     }

    /**
     ** Given an array of primitive elements, returns an equivalent
     ** array of primitive-wrapper objects with the same values. For
     ** instance, supplying an array of <code>int</code> values
     ** returns an array of {@link Integer} objects. If null is passed
     ** in, null is returned.
     **/
    public static Object[] fromPrimitive(Object src)
     {
	if(src == null)
	    return  null;

	Class primType = src.getClass().getComponentType();
	if(primType == null)
	    throw  new ClassCastException("Expected an array of primitives: " +
					  src.getClass());

	if(primType == Boolean.TYPE)
	 {
	    boolean[] srcP = (boolean[]) src;
	    int len = srcP.length;
	    Boolean[] dst = new Boolean[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Boolean(srcP[i]);
	    return  dst;
	 }

	if(primType == Character.TYPE)
	 {
	    char[] srcP = (char[]) src;
	    int len = srcP.length;
	    Character[] dst = new Character[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Character(srcP[i]);
	    return  dst;
	 }

	if(primType == Byte.TYPE)
	 {
	    byte[] srcP = (byte[]) src;
	    int len = srcP.length;
	    Byte[] dst = new Byte[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Byte(srcP[i]);
	    return  dst;
	 }

	if(primType == Short.TYPE)
	 {
	    short[] srcP = (short[]) src;
	    int len = srcP.length;
	    Short[] dst = new Short[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Short(srcP[i]);
	    return  dst;
	 }

	if(primType == Integer.TYPE)
	 {
	    int[] srcP = (int[]) src;
	    int len = srcP.length;
	    Integer[] dst = new Integer[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Integer(srcP[i]);
	    return  dst;
	 }

	if(primType == Long.TYPE)
	 {
	    long[] srcP = (long[]) src;
	    int len = srcP.length;
	    Long[] dst = new Long[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Long(srcP[i]);
	    return  dst;
	 }

	if(primType == Float.TYPE)
	 {
	    float[] srcP = (float[]) src;
	    int len = srcP.length;
	    Float[] dst = new Float[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Float(srcP[i]);
	    return  dst;
	 }

	if(primType == Double.TYPE)
	 {
	    double[] srcP = (double[]) src;
	    int len = srcP.length;
	    Double[] dst = new Double[len];
	    for(int i=0; i<len; i++)
		dst[i] = new Double(srcP[i]);
	    return  dst;
	 }

	throw  new ClassCastException("Not a primitive class: " +
				      primType.getName());
     }

    public static void launchBrowser(String url)
     {
	if(url == null	||  url.equals(""))
	    return;
	try
	 {
		//Check for a user defined browser
	    String browseCmd = DefaultBrowser.getBrowser();
	    String urltag = DefaultBrowser.getURLTag();
	    if (browseCmd != null && browseCmd.length() > 0){
	    	int index = browseCmd.toLowerCase().indexOf(urltag.toLowerCase());
	        if (index < 0){
	        	log.aprintln("Missing webpage placeholder " + urltag +
	                             " in custom browser command");
	        } else {
	        // Replace the url placeholder in case-insensitive fashion with
	        // the webpage reference.  Try to launch custom browser with webpage.
	        	browseCmd = browseCmd.substring(0, index) + url + 
	                            browseCmd.substring(index + urltag.length());
	        	try {
	        		Runtime.getRuntime().exec(browseCmd);
	        		log.aprintln(url);
	        	}
	        	catch (Exception e1) {
	        		log.println(e1);
	        		log.aprintln("Custom webbrowser command '" + browseCmd + "' failed: " +
	                             e1.getMessage());
	        		log.aprint("Will launch default webbrowser instead");
	        	    BrowserLauncher.openURL(url);
	        	}
	        }
	    } else{
	    	 BrowserLauncher.openURL(url);
	    }

	    log.aprintln(url);
	 }
	catch(Throwable e)
	 {
		// Code partially lifted from BareBonesBrowser.  The full code didn't work properly, but as a fall-through second case, it seems like a viable option
		final String[] browsers = { "google-chrome", "firefox", "opera",
			      "epiphany", "konqueror", "conkeror", "midori", "kazehakase", "mozilla" };
		// Second try:
        String osName = System.getProperty("os.name");
        try {
           if (osName.startsWith("Mac OS")) {
              Class.forName("com.apple.eio.FileManager").getDeclaredMethod(
                 "openURL", new Class[] {String.class}).invoke(null,
                 new Object[] {url});
              }
           else if (osName.startsWith("Windows"))
              Runtime.getRuntime().exec(
                 "rundll32 url.dll,FileProtocolHandler " + url);
           else { //assume Unix or Linux
              String browser = null;
              for (String b : browsers)
                 if (browser == null && Runtime.getRuntime().exec(new String[]
                       {"which", b}).getInputStream().read() != -1)
                    Runtime.getRuntime().exec(new String[] {browser = b, url});
              if (browser == null)
                 throw new Exception(Arrays.toString(browsers));
              }
           }
        catch (Exception e1) {
    	    log.aprintln("Failed to open url due to " + e1);
    	    e1.printStackTrace();
    	    Util.showMessageDialog("Unable to open your browser!\n"
    					  + "Details are on the command-line.",
    					  "Browse URL",
    					  JOptionPane.ERROR_MESSAGE);
           }
        }
		
		
     }

    /**
     ** Prints on stdout a list of the available GraphicsDevice
     ** "displays", marking the default one.
     **/
    public static void printAltDisplays()
     {
	System.out.println("---- AVAILABLE DISPLAYS ----");
	GraphicsEnvironment ge =
	    GraphicsEnvironment.getLocalGraphicsEnvironment();
	GraphicsDevice def = ge.getDefaultScreenDevice();
	GraphicsDevice[] devices = ge.getScreenDevices();
	for(int i=0; i<devices.length; i++)
	 {
	    System.out.print("\t" + devices[i].getIDstring());
	    if(devices[i] == def)
		System.out.print(" <- default");
	    System.out.println("");
	 }
     }

    /**
     ** The display used for some popups and dialogs (notably
     ** LManager).
     **/
    private static GraphicsDevice altDisplay;

    /**
     ** Sets the display used for some popups and dialogs. If the
     ** <code>id</code> can't be found, the system exits with an
     ** error.
     **/
    public static void setAltDisplay(String id)
     {
	altDisplay = null;
	GraphicsEnvironment ge =
	    GraphicsEnvironment.getLocalGraphicsEnvironment();
	GraphicsDevice[] devices = ge.getScreenDevices();
	for(int i=0; i<devices.length; i++)
	    if(devices[i].getIDstring().equals(id))
		altDisplay = devices[i];
	if(altDisplay == null)
	 {
	    log.aprintln("UNABLE TO SET DISPLAY TO '" + id + "', EXITING!");
	    System.exit(-1);
	 }
	log.aprintln("SET ALTERNATE DISPLAY TO '" + id + "'");
     }

    public static GraphicsConfiguration getAltDisplay()
     {
	GraphicsDevice display = altDisplay;
	if(display == null)
	    display = GraphicsEnvironment
		.getLocalGraphicsEnvironment()
		.getDefaultScreenDevice();
	return	display.getDefaultConfiguration();
     }

    /**
     ** The standard jdbc mapping from SQL data types to java
     ** classes. The keys of the map are all-uppercase sql data type
     ** names, and the values are the corresponding {@link Class}
     ** objects describing the object returned from a {@link ResultSet
     ** ResultSet.getObject()} call on a column of that sql data type.
     **/
    public static final Class jdbc2java(String sqlType)
     {
	Class javaType = (Class) typeMap.get(sqlType);
	if(javaType == null)
	    return  Object.class;
	else
	    return  javaType;
     }
    
    public static final String java2jdbc(Class javaType){
    	return (String)revTypeMap.get(javaType);
    }

    private static HashMap typeMap = new HashMap();
    private static HashMap revTypeMap = new HashMap();
    static
     {
	typeMap.put("CHAR", String.class);
	typeMap.put("VARCHAR", String.class);
	typeMap.put("LONGVARCHAR", String.class);
	typeMap.put("NUMERIC", Double.class);
	typeMap.put("DECIMAL", Double.class);
	typeMap.put("BIT", Boolean.class);
	typeMap.put("TINYINT", Integer.class);
	typeMap.put("SMALLINT", Integer.class);
	typeMap.put("INTEGER", Integer.class);
	typeMap.put("BIGINT", Long.class);
	typeMap.put("REAL", Float.class);
	typeMap.put("FLOAT", Double.class);
	typeMap.put("DOUBLE", Double.class);
	typeMap.put("DOUBLE PRECISION", Double.class);
	typeMap.put("BINARY", byte[].class);
	typeMap.put("VARBINARY", byte[].class);
	typeMap.put("LONGVARBINARY", byte[].class);
	typeMap.put("DATE", java.sql.Date.class);
	typeMap.put("TIME", java.sql.Time.class);
	typeMap.put("TIMESTAMP", java.sql.Timestamp.class);
	typeMap.put("DISTINCT", Object.class); // not terribly precise
	typeMap.put("CLOB", Clob.class);
	typeMap.put("BLOB", Blob.class);
	typeMap.put("ARRAY", java.sql.Array.class);
	typeMap.put("STRUCT", java.sql.Struct.class); // not terribly precise
	typeMap.put("REF", Ref.class);
	typeMap.put("JAVA_OBJECT", Object.class);
	
	revTypeMap.put(String.class,  "VARCHAR");
	revTypeMap.put(Double.class,  "DOUBLE PRECISION");
	revTypeMap.put(Float.class,   "REAL");
	revTypeMap.put(Boolean.class, "BOOLEAN");
	revTypeMap.put(Integer.class, "INTEGER");
	revTypeMap.put(Short.class,   "SHORT");
	revTypeMap.put(Byte.class,    "BYTE");
	revTypeMap.put(byte[].class, "VARBINARY");
	revTypeMap.put(java.sql.Date.class, "DATE");
	revTypeMap.put(java.sql.Time.class, "TIME");
	revTypeMap.put(java.sql.Timestamp.class, "TIMESTAMP");
	revTypeMap.put(Clob.class, "CLOB");
	revTypeMap.put(Blob.class, "BLOB");
	revTypeMap.put(java.sql.Array.class, "ARRAY");
	revTypeMap.put(java.sql.Struct.class, "STRUCT");
	revTypeMap.put(java.sql.Ref.class, "REF");
	revTypeMap.put(Object.class, "JAVA_OBJECT");
	
     }

    /**
     * Loads the JDBC drivers specified in the jmars.config file.
     */
    public static final void loadSqlDrivers() {
    	String[] driverNames = Config.getArray("dbDriver");
    	log.println("Found a total of "+driverNames.length+" JDBC drivers to load.");
    	
    	for(int i=0; i<driverNames.length; i++){
    		log.println("Loading JDBC driver: "+driverNames[i]);
    		try {
    			Class.forName(driverNames[i]);
    			log.println("Loaded JDBC driver: "+driverNames[i]);
    		}
    		catch(Exception ex){
    			log.aprintln(ex.toString());
				Util.showMessageDialog(
						"The SQL driver \""+driverNames[i]+"\" failed to load.",
						"Error loading JDBC driver",
						JOptionPane.ERROR_MESSAGE);
    			throw new Error("Unable to load driver \""+driverNames[i]+"\".", ex);
    		}
    	}
    	log.println("Done loading drivers.");
    }

    private static final Component sComponent = new Component() {};
    private static final MediaTracker sTracker = new MediaTracker(sComponent);
    private static int sID = 0;

    /** You most likely won't care about this function */
    public static final boolean waitForImage(Image image)
     {
		int id;
	synchronized(sComponent) { id = sID++; }
		sTracker.addImage(image, id);
	try
	 {
			sTracker.waitForID(id);
	 }
	catch(InterruptedException ie)
	 {
	    log.println("Unable to waitForImage:");
//	    log.aprintln(ie);
		    sTracker.removeImage(image, id);
		    return  false;
		}
	if(sTracker.isErrorID(id))
	 {
	    log.println("Failed waitForImage, id " + id);
		    sTracker.removeImage(image, id);
		    return  false;
		}
		
		sTracker.removeImage(image, id);
	
		return	true;
     }

    /** Given an image reference, returns a BufferedImage "of it", with
       a default pixel format */
    public static final BufferedImage makeBufferedImage(Image image)
     {
	if(waitForImage(image) == false)
	    return  null;
	
	BufferedImage bufferedImage = null;

	try 
	{
	    bufferedImage = newBufferedImage(image.getWidth(null), image.getHeight(null));
	}
	catch (NullPointerException e)
	{
	    log.printStack(-1);
	    log.println("We've got a NULL image on our hands!");
	    return(null);
	}

	Graphics2D g2 = bufferedImage.createGraphics();
	g2.drawImage(image, null, null);
	return	bufferedImage;
     }

    /** Given an image reference, returns a BufferedImage "of it", with
       a given pixel format */
    public static final BufferedImage makeBufferedImage(Image image, int imageType)
     {
	if(waitForImage(image) == false)
	    return  null;

	BufferedImage bufferedImage = newBufferedImage(image.getWidth(null),
						       image.getHeight(null));
	Graphics2D g2 = bufferedImage.createGraphics();
	g2.drawImage(image, null, null);
	return	bufferedImage;
     }

    /** Centers a frame on the screen */
    public static final void centerFrame(Frame f)
     {
	Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
	Dimension d = f.getSize();
	int x = (screen.width - d.width) / 2;
	int y = (screen.height - d.height) / 2;
	f.setLocation(x, y);
     }

    /** Returns an affine transform that maps pixel coordinates into world coordinates */
    public static final AffineTransform image2world(int imageWidth, int imageHeight, Rectangle2D worldRect) {
    	AffineTransform at = new AffineTransform();
    	at.translate(worldRect.getX(), worldRect.getY());
    	at.scale(worldRect.getWidth() / imageWidth, worldRect.getHeight() / imageHeight);
    	at.translate(0, imageHeight);
    	at.scale(1, -1);
    	return	at;
    }

    /** Returns an affine transform that maps world coordinates to pixel coordinates */
    public static final AffineTransform world2image(Rectangle2D rect, int imageWidth, int imageHeight) {
    	AffineTransform at = new AffineTransform();
    	at.scale(1, -1);
    	at.translate(0, -imageHeight);
    	at.scale(imageWidth / rect.getWidth(), imageHeight / rect.getHeight());
    	at.translate(-rect.getX(), -rect.getY());
    	return	at;
    }

    /** Returns a new BufferedImage of the given size, with the
     ** default pixel format (as determined by what's most-compatible
     ** with the native screen's preferred pixel format).
     *
     *  Now forces the returned image to have NON premultiplied transparency values to avoid
     *  issues on the Mac where transparent pixels became colored when applying ColorStretchers
     **/
    public static final BufferedImage newBufferedImage(int w, int h) {
		if(w == 0  ||  h == 0)
		    log.aprintln("BAD IMAGE SIZE REQUESTED: " + w + "x" + h);
		
		BufferedImage bi = GraphicsEnvironment
		    .getLocalGraphicsEnvironment()
		    .getDefaultScreenDevice()
		    .getDefaultConfiguration()
		    .createCompatibleImage(w, h, Transparency.TRANSLUCENT);
	 
		bi.coerceData(false);
		
		return bi;
     }

    /** Returns a new BufferedImage of the given size, with the
     ** default pixel format (as determined by what's most-compatible
     ** with the native screen's preferred pixel format).
     **/
    public static final BufferedImage newBufferedImageOpaque(int w, int h)
     {
	if(w == 0  ||  h == 0)
	    log.aprintln("BAD IMAGE SIZE REQUESTED: " + w + "x" + h);
	return
	    GraphicsEnvironment
	    .getLocalGraphicsEnvironment()
	    .getDefaultScreenDevice()
	    .getDefaultConfiguration()
	    .createCompatibleImage(w, h, Transparency.OPAQUE);
     }

	/**
	 * Read to the end of the given input stream, closing the stream and
	 * returning the string found. If anything goes wrong, it tries to
	 * close the stream at that point, and then returns as much of the
	 * response as was read. ISO-8859-1 encoding is assumed.
	 */
	public static String readResponse(InputStream istream) {
		return readResponse(istream, Charset.forName("ISO-8859-1"));
	}
	
	/**
	 * Read to the end of the given input stream, closing the stream and
	 * returning the string found. If anything goes wrong, it tries to
	 * close the stream at that point, and then returns as much of the
	 * response as was read. 
	 */
	public static String readResponse(InputStream istream, Charset charset) {
		BufferedReader br = new BufferedReader(new InputStreamReader(istream, charset));
		char[] buffer = new char[2048];
		CharArrayWriter ow = new CharArrayWriter();
		try {
			while (true) {
				int count = br.read(buffer, 0, buffer.length);
				if (count < 0) {
					// end of stream reached
					break;
				} else if (count > 0) {
					ow.write(buffer, 0, count);
				} else {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// woken up for some reason, just continue on
					}
				}
			}
		} catch (IOException e) {
			log.println(e);
		}
		try {
			br.close();
		} catch (IOException e) {
			log.println(e);
		}
		return ow.toString();
	}
	
    public static class TestSaveAsJpeg
     {
	public static void main(String[] av)
	 throws Throwable
	 {
	    BufferedImage tran = newBufferedImage(5, 5);
	    BufferedImage opaq = newBufferedImageOpaque(5, 5);
	    saveAsJpeg(tran, av[0]);
	    saveAsJpeg(opaq, av[1]);
	 }
     }

    public static final void saveAsJpeg(BufferedImage img, String fname)
     {
	try
	 {
		// append the .jpg to the end of the file name if it doesn't have a valid jpeg extension
		if(!(fname.toLowerCase().endsWith(".jpg") || fname.toLowerCase().endsWith(".jpeg"))) {
			// trim off any dots on the end of the filename
			fname = Util.rTrim(fname, '.');
			fname += ".jpg"; 
		}
		javax.imageio.ImageIO.write(img, "jpg", new File(fname));
	 }
	catch(IOException e)
	 {
	    log.println("From " + fname + " " + img);
	    log.println(e);
	    throw new RuntimeException(e.toString(), e);
	 }
     }
    
    public static final void saveAsPng(BufferedImage img, String fname)
    {
    	try
    	{
    		// append the .png to the end of the file name if it doesn't exist
    		if(!fname.toLowerCase().endsWith(".png")) { 
    			// trim off any dots on the end of the filename
    			fname = Util.rTrim(fname, '.');
    			fname += ".png"; 
    		}
    		javax.imageio.ImageIO.write(img, "png", new File(fname));
    	}
    	catch(IOException e)
    	{
    		log.println("From " + fname + " " + img);
    		log.println(e);
    		throw new RuntimeException(e.toString(), e);
    	}
    }

    public static final void saveAsTif(BufferedImage img, String fname)
    {
    	try
    	{
    		// append the .tif to the end of the file name if it doesn't exist
    		if(!fname.toLowerCase().endsWith(".tif")) { 
    			// trim off any dots on the end of the filename
    			fname = Util.rTrim(fname, '.');
    			fname += ".tif"; 
    		}
    		javax.imageio.ImageIO.write(img, "tif", new File(fname));
    	}
    	catch(IOException e)
    	{
    		log.println("From " + fname + " " + img);
    		log.println(e);
    		throw new RuntimeException(e.toString(), e);
    	}
    }
    
    public static final void saveAsFits(BufferedImage img, String fname)
    {
    	try
    	{
    		// append the .fits to the end of the file name if it doesn't exist
    		if(!fname.toLowerCase().endsWith(".fits")) { 
    			// trim off any dots on the end of the filename
    			fname = Util.rTrim(fname, '.');
    			fname += ".fits"; 
    		}
    		
    		// grab rgb values for the specific pixels 
    		int [][] jmarsImg = new int [img.getHeight()][img.getWidth()];  		
    		int yCounter = 0;
    		for (int y=img.getHeight()-1; y>=0; y--) {	
    			for (int x=0; x<img.getWidth(); x++) {
    			    jmarsImg [yCounter][x] = img.getRGB(x,y);
    	       			}
    			yCounter++;
    		}
    		
    		// create a file for the fits file
    		File file = new File(fname);
    		String fnamePath = file.getAbsolutePath();
    		Fits fits = new Fits(fnamePath);
    		
    		// apply a header to the image
    		BasicHDU hdu = FitsFactory.HDUFactory(jmarsImg);
    		fits.addHDU(hdu);

    		// write the information out to an image
    		BufferedDataOutputStream outStream = new BufferedDataOutputStream(new FileOutputStream(fnamePath));
    		fits.write(outStream);
    		outStream.close();
    		

    	}
    	catch(Exception e)
    	{
    		log.println("From " + fname + " " + img);
    		log.println(e);
    		throw new RuntimeException(e.toString(), e);
    	}
    }


    public static String zeroPadInt(long inV, int totalLength)
    {
	StringBuffer result = new StringBuffer(Integer.toString((int)inV));
	if(totalLength > 0)
	    for(; result.length() < totalLength; result.insert(0, "0"));
	return result.toString();
    }

    public static String formatDouble(double inNumber, int totalLength, int inPrecision)
    {
	return formatDouble(inNumber, totalLength, inPrecision, ' ');
    }

    public static String formatDouble(double inNumber, int totalLength, int inPrecision, char padChar)
    {
	NumberFormat formatter = NumberFormat.getInstance();
	formatter.setMinimumFractionDigits(inPrecision);
	formatter.setMaximumFractionDigits(inPrecision);
	formatter.setGroupingUsed(false);
	StringBuffer result = new StringBuffer(formatter.format(inNumber));
	if(totalLength > 0)
	    for(; result.length() < totalLength; result.insert(0, padChar));
	return result.toString();
    }

    public static String formatDouble(double inNumber, int inPrecision)
    {
	NumberFormat formatter = NumberFormat.getInstance();
	formatter.setMinimumFractionDigits(inPrecision);
	formatter.setMaximumFractionDigits(inPrecision);
	formatter.setGroupingUsed(false);
	return formatter.format(inNumber);
    }
    
    public static final BufferedImage createGrayscaleImageRot(int dataW,
							      int dataH,
							      byte[] data,
							      int offset,
							      boolean rotate)
     {
	int imgW = rotate ? dataH : dataW;
	int imgH = rotate ? dataW : dataH;

	try
	 {
	    BufferedImage img = newBufferedImage(imgW, imgH);

	    log.println("Turning byte buffer into int buffer");
	    int[] dataAsInt = new int[imgW * imgH];
	    int x=0, y=0; // Declared here to be available during exception
	    int nData=0, nImage=0; // ditto
	    try
	     {
		if(rotate)
		    // Note: x is zero at left, increasing rightward in IMAGE
		    //	     y is zero at top, increasing downward in IMAGE
		    for(x=0; x<imgW; x++)
			for(y=0; y<imgH; y++)
			 {
			    nData = x*imgH + imgH - y - 1 + offset;
			    nImage = y * imgW + x;
			    final int b = data[nData] & 0xFF;
			    dataAsInt[nImage] = new Color(b, b, b).getRGB();
			 }
		else
		    for(x=0; x<imgW*imgH; x++)
		     {
			int b = data[x + offset] & 0xFF;
			dataAsInt[x] = new Color(b, b, b).getRGB();
		     }
	     }
	    catch(ArrayIndexOutOfBoundsException e)
	     {
		log.aprintln("BAD ARRAY INDEX ENCOUNTERED");
		log.aprintln("rotate = " + rotate);
		log.aprintln("x = " + x);
		log.aprintln("y = " + y);
		log.aprintln("imgW = " + imgW);
		log.aprintln("imgH = " + imgH);
		log.aprintln("offset = " + offset);
		log.aprintln("data.length = " + data.length);
		log.aprintln("dataAsInt.length = " + dataAsInt.length);
		log.aprintln("nData = " + nData);
		log.aprintln("nImage = " + nImage);
		throw  e;
	     }

	    log.println("Writing buffer into image");
	    img.setRGB(0, 0,
		       imgW, imgH,
		       dataAsInt,
		       0,
		       imgW);
	    log.println("Done");
	    return  img;
	 }
	catch(Throwable e)
	 {
	    log.aprintln("UNABLE TO CREATE GRAYSCALE IMAGE DUE TO:");
	    log.aprintln(e.toString());
	    log.aprintStack(10);
	    return  null;
	 }
     }

    public static final BufferedImage createGrayscaleImage(int w, int h,
						     byte[] data, int offset)
     {
	if(true)
	    throw  new Error("THIS FUNCTION MIGHT NOT WORK");
	BufferedImage img = newBufferedImage(w, h);

	log.println("Turning byte buffer into int buffer");
	int[] dataAsInt = new int[data.length];
	for(int i=0; i<data.length; i++)
	 {
	    final int b = data[i] & 0xFF;
	    dataAsInt[i] = new Color(b, b, b).getRGB();
	 }

	log.println("Writing buffer into image");
	img.setRGB(0, 0,
		   w, h,
		   dataAsInt,
		   offset,
		   w);
	log.println("Done");
	return	img;
     }


    /**
     ** @deprecated Don't use, the resulting image is strangely slow.
     **/
    public static final BufferedImage BOOK_createGrayscale(int w, int h,
						     byte[] data, int offset)
     {
	ComponentColorModel ccm =
	    new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
				    new int[] { 8 },
				    false,
				    false,
				    Transparency.OPAQUE,
				    DataBuffer.TYPE_BYTE);
	ComponentSampleModel csm =
	    new ComponentSampleModel(DataBuffer.TYPE_BYTE,
				     w, h, 1, w, new int[] { 0 });
	DataBuffer dataBuff = new DataBufferByte(data, w, offset);

	WritableRaster wr = Raster.createWritableRaster(csm,
							dataBuff,
							new Point(0,0));

	BufferedImage rawImage = new BufferedImage(ccm, wr, true, null);

	BufferedImage niceImage = newBufferedImage(w, h);
	log.aprintln("Re-rendering image");
	niceImage.createGraphics().drawImage(rawImage, 0, 0, null);
	log.aprintln("Done.");

	return	niceImage;
     }


    public static final Object loadUserObject(String filename)
    {
	Object oData = null;

	try {

	    if ( filename.length() > 0 )
	    {
		log.println("Loading user preferences from: " + filename);
		ObjectInputStream fin = new ObjectInputStream(new FileInputStream(filename));
		oData =	 fin.readObject();
		fin.close();
	     }

	}
	catch (FileNotFoundException f) {
	    //ignore this - no big deal if not found
	    log.println("File not found - ignored");
	}
	catch (Exception e) {
	     log.println("Error: " + e.getMessage());
	}

	return oData;
    }

    public static final void saveUserObject( String filename, Serializable dataObject)
    {

	try {

	  if ( filename.length() > 0 )
	  {
	      log.println("Saving user preferences to: " + filename);

	      ObjectOutputStream fout = new ObjectOutputStream(new FileOutputStream(filename));
	      fout.writeObject(dataObject);
	      fout.flush();
	      fout.close();
	  }
	}
	catch (Exception e) {
	     System.out.println("Error: " + e.getMessage());
	}
    }

    /**
     ** Given a set of values in the range 0-225, and a set of
     ** corresponding colors, produces a complete 256-color
     ** interpolated color map.
     **
     ** @param scheme The interpolation scheme: -1 linear HSB
     ** decreasing hue, 0 linear HSB shortest hue path, +1 linear HSB
     ** increasing hue, +2 linear HSB direct hue path, +3 linear RGB.
     **/
    public static final Color[] createColorMap(int[] values,
					       Color[] colors,
					       int scheme)
     {
	Color[] colorMap = new Color[256];

	int idx1 = 0;
	Color col0 = colors[0];
	Color col1 = colors[0];
	int val0 = values[0];
	int val1 = values[0];
	for(int i=0; i<256; i++)
	 {
	    if(idx1 < values.length)
		if(values[idx1] == i)
		 {
		    col0 = col1;
		    val0 = val1;
		    ++idx1;
		    if(idx1 < values.length)
		     {
			col1 = colors[idx1];
			val1 = values[idx1];
		     }
		 }
	    if(val0 == val1)
		colorMap[i] = col0;
	    else
	     {
		double mix1 = (i-val0) / (double) (val1-val0);
		if(scheme != +3)
		    colorMap[i] = mixColorHSB(col0, col1, mix1, scheme);
		else
		    colorMap[i] = mixColorRGB(col0, col1, mix1);
	     }
	 }
	return	colorMap;
     }

    /**
     ** Returns a color that is <code>mix1</code> "between" c0 and
     ** c1. If mix1=0 you get c0, mix1=1 you get c1, in between you
     ** get something in between (calculated by linear HSB
     ** interpolation by shortest hue path). Values of mix1 outside
     ** the range [0,1] will return unspecified results.
     **/
    public static final Color mixColor(Color c0, Color c1, double mix1)
     {
	return	mixColorHSB(c0, c1, mix1, 0);
     }

    /**
     ** Returns a color that is <code>mix1</code> "between" c0 and
     ** c1. If mix1=0 you get c0, mix1=1 you get c1, in between you
     ** get something in between (calculated by linear HSB
     ** interpolation). Values of mix1 outside the range [0,1] will
     ** return unspecified results.
     **
     ** @param hueDir Should be one of -1, +1, 0, or +2 to indicate
     ** hue interpolation by descending, ascending, shortest path, or
     ** non-wrapping path method.
     **/
    public static final Color mixColorHSB(Color c0,
					  Color c1,
					  double mix1,
					  int hueDir)
     {
	// Trivial case.
	if(c0.equals(c1)  &&  (hueDir == 0 || hueDir == +2))
	    return  c0;

	// Convenience: mix1 is the amount of c1 and mix0 is the amount of c0
	double mix0 = 1 - mix1;

	// Get the HSB values of each color
	float hsb0[] = Color.RGBtoHSB(c0.getRed(),
				      c0.getGreen(),
				      c0.getBlue(),
				      null);
	float hsb1[] = Color.RGBtoHSB(c1.getRed(),
				      c1.getGreen(),
				      c1.getBlue(),
				      null);
	double h0 = hsb0[0];
	double h1 = hsb1[0];

	// If we have a grayscale, its hue is actually
	// meaningless... so to prevent its "random" value from
	// causing trouble, we fix the grayscale's hue to the color's
	// hue for smooth fading. We also later use partGray to
	// prevent grayscales from doing the "red to red" thing and
	// spanning the whole spectrum.
	boolean partGray = false;
	if(hsb0[1] == 0.0)
	 {
	    partGray = true;
	    h0 = h1;
	 }
	else if(hsb1[1] == 0.0)
	 {
	    partGray = true;
	    h1 = h0;
	 }

	// Our derived color is a simple linear combination of the base colors
	double h; // determined below by hueDir
	double s = mix0*hsb0[1] + mix1*hsb1[1];
	double b = mix0*hsb0[2] + mix1*hsb1[2];

	switch(hueDir)
	 {

	 case 2: // Non-wrapping path method
	    h = mix0*h0 + mix1*h1;
	    break;
	    
	 case 0: // Shortest path method
	    h = mix0*h0 + mix1*h1;
	    if(Math.abs(h0 - h1) > 0.5)
	     {
		// Fix for hues separated by > 180 degrees
		h = 1 - (Math.max(h0,h1)-Math.min(h0,h1));
		h *= (h1 > h0 ? mix0 : mix1);
		h += Math.max(h0,h1);
		if(h > 1.0)
		    h -= 1.0;
	     }
	    break;

	 case +1: // Enforce increasing hue
	    double distance = (1 + h1 - h0) % 1;
	    if(distance == 0  &&  !partGray)
		distance = 1;
	    h = 1 + h0 + mix1 * distance;
	    h %= 1;
	    break;

	 case -1: // Enforce decreasing hue
	    distance = (1 + h0 - h1) % 1;
	    if(distance == 0  &&  !partGray)
		distance = 1;
	    h = 1 + h0 - mix1 * distance;
	    h %= 1;
	    break;

	 default:
	    h = 0;
	    log.aprintln("INVALID HUE DIRECTION: " + hueDir);
	 }

	return	Color.getHSBColor((float) h,
				  (float) s,
				  (float) b);
     }

    /**
     ** Returns a color that is <code>mix1</code> "between" c0 and
     ** c1. If mix1=0 you get c0, mix1=1 you get c1, in between you
     ** get something in between (calculated by linear RGB
     ** interpolation). Values of mix1 outside the range [0,1] will
     ** return unspecified results.
     **/
    public static final Color mixColorRGB(Color c0,
					  Color c1,
					  double mix1)
     {
	// Convenience: mix1 is the amount of c1 and mix0 is the amount of c0
	double mix0 = 1 - mix1;

	int r = (int) Math.round( mix0*c0.getRed()   + mix1*c1.getRed()	  );
	int g = (int) Math.round( mix0*c0.getGreen() + mix1*c1.getGreen() );
	int b = (int) Math.round( mix0*c0.getBlue()  + mix1*c1.getBlue()  );

	return	new Color(r, g, b);
     }

    /**
     ** Returns the "B" component of the HSB representation of a Color.
     **/
    public static final float getB(Color c)
     {
	return	Color.RGBtoHSB(
	    c.getRed(),
	    c.getGreen(),
	    c.getBlue(),
	    null
	    )[2];
     }

    /**
     ** Returns <code>num</code> rounded to the nearest multiple of
     ** <code>mul</code>.
     **/
    public static final double roundToMultiple(double num, double mul)
     {
	return	Math.round(num / mul) * mul;
     }

    /**
     ** Returns <code>num</code> rounded to the nearest multiple of
     ** <code>mul</code>.
     **/
    public static final long roundToMultiple(double num, long mul)
     {
	return	Math.round(num / mul) * mul;
     }

    /**
     ** Returns <code>num</code> rounded to the nearest multiple of
     ** <code>mul</code>.
     **/
    public static final int roundToMultiple(int num, int mul)
     {
	return	Math.round(num / mul) * mul;
     }

    /**
     ** Returns string with newlines inserted at the specified
     ** character intervals or earlier, i.e., wraps at word
     ** boundaries.
     **/
    public static final String lineWrap(String text, int interval)
    {
        String wrapped = null;
        
        if (text != null)
        {
            StringBuffer buf = new StringBuffer(text);
            
            for (int i=interval; i < buf.length(); i+=interval)
            {
                // Scan this interval from back to front for
                // a newline; if found start next interval scan
                // here.
                boolean nextInterval = false;
                for (int j=i; j > i - interval; j--)
                    if (buf.charAt(j) == '\n')
                    {
                        i = j;
                        nextInterval = true;
                        break;
                    }
                    
                if (!nextInterval)
                    // Scan interval from back to front for
                    // first whitespace character and replace it
                    // with newline.  If front of interval is reached
                    // without finding whitespace, put insert newline at
                    // end of interval.
                    for (int j=i; j >= i - interval; j--) {
                        if (j == i - interval)
                            buf.insert(i, '\n');
                        else if (Character.isWhitespace(buf.charAt(j))) {
                            buf.setCharAt(j, '\n');
                            i = j;
                            break;
                        }
                    }
            }
            
            wrapped = buf.toString();
        }
        
        return wrapped;
    }

     /**
      ** Sorts one list relative to the order that objects appear in
      ** another list.
      **
      ** @param sortList list of objects to be sorted; all objects
      ** in this list must "appear" in the <code>orderList</code>. An
      ** object appears if it matches an object with the equals()
      ** method test.  If an object does not appear in the second
      ** list, an {@link IllegalArgumentException} is thrown.
      **
      ** @param orderList list of objects that represent the relative
      ** object sorting order for the <code>sortList</code>.
      **
      ** @see List#indexOf
      **/
    public static final void relativeSort(final List sortList, final List orderList)
     throws IllegalArgumentException
     {
	Collections.sort(sortList,
			 new Comparator()
			  {
			     public int compare(Object o1, Object o2)
			      {
				 int index1 = orderList.indexOf(o1);
				 int index2 = orderList.indexOf(o2);
		      
				 if (index1 < 0 ||
				     index2 < 0)
				     throw new IllegalArgumentException(
					 "object not found in list");

				 return index1 - index2;
			      }
		  
			     public boolean equals(Object obj) 
			      {
				 return super.equals(obj);
			      }
			  }
	    );
     }
    
    /**
     * Resizes the top-level ancestor of the given component "fp" such
     * that it is not smaller than the preferred size of "fp".
     * If the component "c" is null or it does not have a top level
     * ancestor that can be resized, nothing happens.
     */
    public static final void resizeTopLevelContainerToPreferredSize(JComponent fp){
    	if (fp != null && fp.getTopLevelAncestor() != null){
    		Dimension p = fp.getPreferredSize();
    		Dimension c = fp.getTopLevelAncestor().getSize();
    		Dimension r = new Dimension(
    				(int)Math.max(p.getWidth(),c.getWidth()),
    				(int)Math.max(p.getHeight(),c.getHeight()));
    		fp.getTopLevelAncestor().setSize(r);
    		fp.getTopLevelAncestor().doLayout();
    		fp.getTopLevelAncestor().repaint();
    	}
    }

	/**
	 * Calls binRanges(int[]) with the portion of 'indices' from index 0 to
	 * length-1 inclusive.
	 */
	public static int[][] binRanges (int[] indices, int length) {
		int[] array = new int[length];
		System.arraycopy(indices, 0, array, 0, length);
		return binRanges(array);
	}

	/**
	 * Returns an array of disjoint ranges in the given array of indices.
	 * @param indices Left in sorted order, whether it was before or not.
	 * @return Array of int[2] arrays, where the contained array elements
	 * are the start/end indices.
	 */
	public static int[][] binRanges (int[] indices) {
		Arrays.sort (indices);
		List<int[]> ranges = new LinkedList<int[]>();
		if (indices != null && indices.length > 0) {
			int[] range = new int[2];
			range[0] = indices[0];
			for (int i = 1; i < indices.length; i++) {
				if (indices[i] - indices[i-1] != 1) {
					range[1] = indices[i-1];
					ranges.add (range.clone());
					range[0] = indices[i];
				}
			}
			range[1] = indices[indices.length-1];
			ranges.add (range);
		}
		return (int[][]) ranges.toArray (new int[0][]);
	}

    /**
     * Fold text so that the text (excluding delimiter) fits in the
     * specified width. This implementation is very simplistic and
     * is quite extravagent in its memory usage. Thus it is inadvisable
     * to use it for folding large chunks of texts. The text is folded
     * at white-spaces and multiple white-spaces are coalesced.
     * <b>CAUTION:</b>The code does not check for invalid values of
     * width or null inputs.
     * 
     * @param text The text to be folded.
     * @param width The width (exclusive of delimiter) to fold to.
     * @param foldDelim String to use as a fold marker.
     * @return Input text folded to the specified length.
     */
    // 
    public static final String foldText(String text, int width, String foldDelim){
    	if (text == null){ return null; }

    	String[] words = text.split("\\s+");
    	String   line = "";
    	StringBuffer folded = new StringBuffer();
    	int      i = 0;

    	while(i < words.length){
    		// If the new word will make the line longer than user requested width
    		if ((line + " " + words[i]).length() > width){
    			// then wait for it to come around in the next pass, if it itself
    			// is less than the user requested width
    			if (words[i].length() < width){
    				folded.append(foldDelim);
    				line = "";
    			}
    			// otherwise 
    			else {
    				if (!line.equals("")){ folded.append(" "); }
    				String s = words[i];
    				while(s.length() >= width){
    					// hyphenate
    					folded.append(s.substring(0,width-1));
    					folded.append("-");
    					folded.append(foldDelim);
    					s = s.substring(width);
    				}
    				folded.append(s);
    				line = s;
    				i++;
    			}
    		}
    		else {
    			if (!line.equals("")){
    				folded.append(" ");
    				line += " ";
    			}
    			folded.append(words[i]);
    			line += words[i];
    			i++;
    		}
    	}

    	return folded.toString();
    }
    
    /** Sets the size of the frame to the larger of its current and preferred size */
    public static void expandSize(Frame top) {
    	if (top == null)
    		return;
		Dimension oldSize = top.getSize();
		Dimension newSize = top.getPreferredSize();
		top.setSize(new Dimension(Math.max(oldSize.width, newSize.width),
			Math.max(oldSize.height, newSize.height)));
		top.validate();
		top.repaint();
	}
	
	public static Component getNearest (Component c, Class type) {
		while (c != null && ! type.isInstance(c))
			c = c.getParent();
		return c;
	}
	
	public static Window getDisplayFrame(Component c) {
		while (c!=null) {
			if (c instanceof Window) {
				return (Window)c;
			}
			c = c.getParent();
		}

		return null;
	}
	/**
	 * Read and return all bytes from the specified input stream until an
	 * end of stream is encountered.
	 * @throws IOException
	 */
	public static byte[] getAllBytes(InputStream is) throws IOException {
		InputStream st = is;
		if (!(is instanceof BufferedInputStream))
			st = new BufferedInputStream(is);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] data = new byte[10000];
		int status = 0;
		do {
			out.write(data, 0, status);
			status = st.read(data, 0, data.length);
		} while (status >= 0);
		return out.toByteArray();
	}
	
	public static Point2D interpolate(Line2D line, double t){
		return new Point2D.Double(
				line.getX1()+t*(line.getX2()-line.getX1()),
				line.getY1()+t*(line.getY2()-line.getY1()));
	}
	
	public static Point2D interpolate(Point2D p1, Point2D p2, double t){
		return new Point2D.Double(
				p1.getX()+t*(p2.getX()-p1.getX()),
				p1.getY()+t*(p2.getY()-p1.getY()));
	}

	public static double uninterploate(Line2D line, Point2D pt){
		HVector p = new HVector(pt.getX(), pt.getY(), 0);
		HVector p1 = new HVector(line.getX1(), line.getY1(), 0);
		HVector p2 = new HVector(line.getX2(), line.getY2(), 0);
		double t = HVector.uninterpolate(p1, p2, p);
		return t;
	}
	
	/**
	 * DDA line drawing algorithm lifted from Computer Graphics by Hearn & Baker.
	 * @return An array of Points on the line.
	 */
	public static Point[] dda(int x0, int y0, int x1, int y1){
		int dx = x1-x0;
		int dy = y1-y0;
		int steps = 1 + (Math.abs(dx) > Math.abs(dy) ? Math.abs(dx): Math.abs(dy));
		double xinc = ((double)dx)/steps;
		double yinc = ((double)dy)/steps;

		Point[] points = new Point[steps];
		double x = x0, y = y0;
		for(int k = 0; k < steps; k ++) {
			points[k] = new Point((int)Math.round(x), (int)Math.round(y));
			x += xinc;
			y += yinc;
		}

		return points;
	}
	
	/**
	 * Return an empty image of the given width and height that will be
	 * compatible with the given image
	 */
	public static BufferedImage createCompatibleImage(BufferedImage image, int w, int h) {
		ColorModel cm = image.getColorModel();
		WritableRaster r = image.getRaster().createCompatibleWritableRaster(w, h);
		return new BufferedImage(cm, r, image.isAlphaPremultiplied(), null);
	}
	
	/**
	 * Returns the wrapped world rectangles covered by this unwrapped world
	 * rectangle. There can not be more than two rectangles in the output.
	 */
	public static Rectangle2D[] toWrappedWorld(Rectangle2D unwrappedWorld) {
		double y = unwrappedWorld.getY();
		double h = unwrappedWorld.getHeight();
		if (unwrappedWorld.getWidth() > 360.0)
			return new Rectangle2D[] {
				new Rectangle2D.Double(0, y, 360, h)
			};
		
		double minx = unwrappedWorld.getMinX();
		double maxx = unwrappedWorld.getMaxX();
		double xoffset = 0;
		if (minx < 0) {
			xoffset = Math.ceil(-minx/360.0)*360.0;
		} else if (minx > 360) {
			xoffset = Math.floor(minx/360.0)*-360.0;
		}
		minx += xoffset;
		maxx += xoffset;
		
		if (maxx <= 360.0)
			return new Rectangle2D[] {
				new Rectangle2D.Double(minx, y, maxx-minx, h)
			};
		else
			return new Rectangle2D[] {
				new Rectangle2D.Double(0, y, maxx-360, h),
				new Rectangle2D.Double(minx, y, 360-minx, h)
			};
	}

	/**
	 * @return All occurrences of wrappedImage in the specified unwrapped
	 *         domain. The results will intersect unwrappedDomain but may not be
	 *         completely contained.
	 */
	public static Rectangle2D[] toUnwrappedWorld(Rectangle2D wrappedImage, Rectangle2D unwrappedDomain) {
		List<Rectangle2D> matches = new ArrayList<Rectangle2D>();
		Rectangle2D query = new Rectangle2D.Double();
		double start = Math.floor(unwrappedDomain.getMinX() / 360.0) * 360.0 + wrappedImage.getMinX();
		double y = wrappedImage.getY();
		double w = wrappedImage.getWidth();
		double h = wrappedImage.getHeight();
		for (double x = start; x < unwrappedDomain.getMaxX(); x += 360.0) {
			query.setFrame(x, y, w, h);
			if (unwrappedDomain.intersects(query)) {
				matches.add((Rectangle2D)query.clone());
			}
		}
		return matches.toArray(new Rectangle2D[matches.size()]);
	}
	
	/**
	 * @return an array of JTS envelopes with x coordinates in the range [0-360]
	 *         that collectively cover the given unwrapped world rectangle.
	 */
	public static Envelope[] rect2env(Rectangle2D unwrappedWorld) {
		Rectangle2D[] bounds = Util.toWrappedWorld(unwrappedWorld);
		Envelope[] env = new Envelope[bounds.length];
		for (int i = 0; i < env.length; i++) {
			env[i] = new Envelope(bounds[i].getMinX(), bounds[i].getMaxX(), bounds[i].getMinY(), bounds[i].getMaxY());
		}
		return env;
	}
	
	 public static String getCacheDir() {
			return cacheDir;
     }
	
	/**
	 * Get the lon/lat of the up vector to send to a MapServer using the WMS
	 * JMARS:1 projection, based on the given oblique cylindrical projection.
	 * @param poc The projection from which to derive the up vector.
	 * @param out The point to put the result into. If null, a new point is created.
	 * @return The result point; will be equal to <code>out</code> if it was not null.
	 */
	public static final Point2D getJmars1Up(Projection_OC poc, Point2D out) {
		if (out == null)
			out = new Point2D.Double();
		double upLat = poc.getUpLat();
		double upLon = poc.getUpLon();
		double centerLat = poc.getCenterLat();
		if (centerLat<=0) {
			upLon+=180;
		} 
		out.setLocation(upLon, upLat);
		return out;
	}
	
	/**
	 * Converts the X coordinate of the JMARS world coordinate system to an equivalent X
	 * coordinate in the WMS JMARS:1 coordinate system.
	 * The two systems have the same X values for points in the northern hemisphere, but
	 * they are 180 degrees away from each other in the southern hemisphere.
	 * @param poc The projection from which to determine the side of the planet we're on.
	 * @param in The starting longitude
	 * @return The input longitude + 180 degrees if the projection's center latitude is
	 * above 0, the input longitude otherwise.
	 */
	public static final double worldXToJmars1X(Projection_OC poc, double in) {
		return poc.getCenterLat() <= 0 ? in - 180.0 : in;
	}
	
	/**
	 * Returns the angular distance from p1 to p2 using pure spherical trig.
	 * This method takes about 50% longer to run than
	 * {@link HVector#separation(HVector)}, but that uses the arcsin formula
	 * and this uses a formula that is stable in all cases.
	 * 
	 * @param p1 [east-longitude in degrees,geocentric-lat in degrees] point
	 * @param p2 [east-longitude in degrees,geocentric-lat in degrees] point
	 * @return The angular distance along the unit sphere from p1 to p2, in radians.
	 */
	public static double separation(Point2D p1, Point2D p2) {
		double latRad2 = Math.toRadians(p2.getY());
		double cosLat2 = Math.cos(latRad2);
		double sinLat2 = Math.sin(latRad2);
		double latRad1 = Math.toRadians(p1.getY());
		double cosLat1 = Math.cos(latRad1);
		double sinLat1 = Math.sin(latRad1);
		double deltaLon = Math.toRadians(p2.getX() - p1.getX());
		double cosDeLon = Math.cos(deltaLon);
		double sinDeLon = Math.sin(deltaLon);
		double a = cosLat2 * sinDeLon;
		double b = cosLat1 * sinLat2 - sinLat1 * cosLat2 * cosDeLon;
		double c = sinLat1 * sinLat2 + cosLat1 * cosLat2 * cosDeLon;
		return Math.atan2(Math.hypot(a, b), c);
	}
	
	public static ImageIcon loadIcon(String name) {
		return new ImageIcon(loadImage(name));
	}
	
	public static BufferedImage loadImage(String name) {
		try {
			return ImageIO.read(Main.getResource(name));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/** Returns a writable raster for band 'band' in the given image */
	public static WritableRaster getBands(BufferedImage img, int ... bands) {
		return img.getRaster().createWritableChild(0, 0, img.getWidth(), img.getHeight(), 0, 0, bands);
	}
	
	/** Returns the portion of the raster without the alpha band (could be all of it) */
	public static WritableRaster getColorRaster(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		WritableRaster raster = bi.getRaster();
		if (cm.hasAlpha() == false) {
			return raster;
		}
		int[] bands = new int[cm.getNumColorComponents()];
		for (int i = 0; i < bands.length; i++) {
			bands[i] = i;
		}
		int x = raster.getMinX();
		int y = raster.getMinY();
		return raster.createWritableChild(x, y, raster.getWidth(), raster.getHeight(), x, y, bands);
	}
	
	/**
	 * Our GrayCS is a linear Gray ColorSpace. Java's GrayCS is non-linear.
	 */
	private static ColorSpace linearGrayColorSpace = null;
	public static synchronized ColorSpace getLinearGrayColorSpace(){
		if (linearGrayColorSpace != null)
			return linearGrayColorSpace;
		
		try {
			ICC_Profile myGrayCP = ICC_ProfileGray.getInstance(Main.getResourceAsStream("resources/sGray.icc"));
			linearGrayColorSpace = new ICC_ColorSpace(myGrayCP);
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
		
		return linearGrayColorSpace;
	}
	
	public static BufferedImage replaceWithLinearGrayCS(BufferedImage in){
		// Pass nulls back as is.
		if (in == null)
			return null;
		
		// Return non-GrayCS images back as is.
		if (in.getColorModel().getColorSpace().getType() != ColorSpace.TYPE_GRAY)
			return in;

		
		// Replace the BufferedImage's ColorSpace with a linear Grayscale ColorSpace.
		ColorModel cm = new ComponentColorModel(getLinearGrayColorSpace(), in.getColorModel().hasAlpha(), in.isAlphaPremultiplied(), in.getTransparency(), in.getSampleModel().getTransferType());
		BufferedImage out = new BufferedImage(cm, in.getRaster(), in.isAlphaPremultiplied(), null);
		return out;
	}
	
	/** Returns the latin1 encoded string made from an apache-style hash of the given username and password */
	public static String apachePassHash(String user, String password) {
		String token = user + password;
		byte[] coded = Base64.encodeBase64(DigestUtils.sha(token));
		return Charset.forName("ISO-8859-1").decode(ByteBuffer.wrap(coded)).toString().replaceAll("=+$", "");
	}
	
	/** Returns a MySQL 3.5-4.00 era password hash of the given plaintext password */
	public static String mysqlPassHash(String password) {
		int nr = 0x50305735;
		int nr2 = 0x12345671;
		int add = 7;
		for (char ch : password.toCharArray()) {
			if (ch == ' ' || ch == '\t')
				continue;
			int charVal = ch;
			nr ^= (((nr & 63) + add) * charVal) + (nr << 8);
			nr &= 0x7fffffff;
			nr2 += (nr2 << 8) ^ nr;
			nr2 &= 0x7fffffff;
			add += charVal;
		}
		return String.format("%08x%08x", nr, nr2);
	}
	
	/**
	 * Inserts each key/value in <code>oldIn</code> into <code>emptyOut</code>
	 * using the key as the value and the value as the key.
	 * @returns The <code>emptyOut</code> argument is simply returned.
	 * @throws IllegalArgumentException If the same value occurs more than once.
	 */
	public static <E,F> Map<F,E> reverse(Map<E,F> oldIn, Map<F,E> emptyOut) {
		for (E key: oldIn.keySet()) {
			F value = oldIn.get(key);
			if (emptyOut.containsKey(value)) {
				throw new IllegalArgumentException("Map cannot be reversed; contains duplicate value");
			}
			emptyOut.put(value, key);
		}
		return emptyOut;
	}

	/**
	 * Wrapper around basic HttpClient execution of PostMethod to handle
	 * redirects.
	 * 
	 * @return The HTTP response code from the last URI contacted.
	 * @throws IOException Thrown if an IO error occurs
	 * @throws NullPointerException Thrown if an empty location header is found
	 * @throws HttpException Thrown if another kind of HTTP error occurs
	 * @throws URIException Thrown if an invalid URI is used
	 */ 
//	public static int postWithRedirect(HttpClient client, PostMethod post, int maxRedirects)               TODO (PW) Remove commented-out code
//			throws URIException, HttpException, NullPointerException, IOException {
//		int code = -1;
//		for (int tries = 0; tries < maxRedirects; tries++) {
//			code = client.executeMethod(post);
//			switch (code) {
//			case 301: // moved permanently
//			case 302: // moved temporarily
//			case 307: // temporary redirect
//				Header loc = post.getResponseHeader("location");
//				if (loc != null) {
//					post.setURI(new URI(loc.getValue(), false));
//				} else {
//					return code;
//				}
//				break;
//			case 200:
//			default:
//				return code;
//			}
//		}
//		return code;
//	}
	
	/**
	 * Retrieves the given remote file, caches it in cachePath. Subsequent
	 * calls return the cached copy. The cached copy is brought up-to-date
	 * with respect to the remoteUrl before being returned if updateCheck
	 * is <code>true</code>.
	 * @param remoteUrl URL of the source file
	 * @param updateCheck Whether to check for updates or not. This is only
	 *        applicable if the file exists already. If not, an update is automatically
	 *        performed.
	 * @return <code>null</code> in case of an error, or the {@link File} in case
	 *         of success.
	 */
	public static File getCachedFile(String remoteUrl, boolean updateCheck) {
		String cachePath = Main.getJMarsPath()+"localcache"+File.separator;
		try {
			URL url = new URL(remoteUrl);
			
			File localFile = new File(cachePath + url.getFile().replaceAll("[^a-zA-Z0-9]", "_"));
			if (!updateCheck){
				if (localFile.exists()){
					log.println("No update check requested, returning existing file.");
					return localFile;
				}
				else {
					log.println("No update check requested, but the file does not exist. Forcing update.");
				}
			}
			
            JmarsHttpRequest request = new JmarsHttpRequest(remoteUrl, HttpRequestType.GET, new Date(localFile.lastModified()) );
            request.addRequestParameter("User-Agent", "Java");
            boolean successful = request.send();
            if (successful) {
                int httpStatus = request.getStatus();
    			if (!(localFile.exists() && httpStatus == HttpStatus.SC_NOT_MODIFIED)) {
    				if (localFile.exists()) {
    				    log.println("File from "+remoteUrl+" is out of date ("+(new Date(localFile.lastModified()))+" vs "+request.getLastModifiedString()+").");
    				} else {
    					log.println("File from "+remoteUrl+" is not cached locally.");
    				}
    				new File(cachePath).mkdirs();
    				InputStream is = request.getResponseAsStream();
    				OutputStream os = new BufferedOutputStream(new FileOutputStream(localFile));
    				byte[] buff = new byte[1024];
    				int nread;
    				while((nread = is.read(buff)) > -1)
    					os.write(buff, 0, nread);
    				os.close();
    				if (request.getLastModifiedDate() != 0)
    					localFile.setLastModified(request.getLastModifiedDate());
    				
    				log.println("Downloaded file from " + remoteUrl+ " modification date: "+request.getLastModifiedDate());
    			}
    			else {
    				log.println("Using cached copy for "+remoteUrl+".");
    			}
            }
			return localFile;
		} catch (Exception e) {
			log.aprintln("Getting network file: "+cachePath+" failed with Exception: "+e.getMessage());
			return null;
		}
	}
	
	public static void addEscapeAction(final JDialog dlg) {
		KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
		dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "ESCAPE");
		dlg.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				dlg.setVisible(false);
			}
		});
	}
	
	public static String[] getTextHttpResponse(String url, HttpRequestType type) { 
		String[] returnVal = null;
        try {
        	JmarsHttpRequest request = new JmarsHttpRequest(url, type);
            boolean returnStatus = request.send();
            if (!returnStatus) {
                int status = request.getStatus();
                log.aprintln("Problem with request: "+url);
                log.aprintln("Response status code: "+status);
            } else {
            	returnVal = Util.readLines(request.getResponseAsStream());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnVal;
	}
	
	public static void addEscapeDisposesAction(final JDialog dlg) {
		KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
		dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "ESCAPE");
		dlg.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				dlg.dispose();
			}
		});
	}
	
	public static void copy(InputStream is, OutputStream os) throws IOException {
		byte[] buffer = new byte[2048];
		int count = 0;
		while (0 <= (count = is.read(buffer))) {
			if (count == 0) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				os.write(buffer, 0, count);
			}
		}
	}
	
	/**
	 * Gets the jmars icon and returns it as a bufferedImage.
	 * @return a BufferedImage containing the JMars icon
	 */
	public static BufferedImage getJMarsIcon(){
		BufferedImage loadImage = null;
		String theme = GUIState.getInstance().themeAsString();
		if (GUITheme.DARK.asString().equalsIgnoreCase(theme)) {
			loadImage = Util.loadImage("resources/jmars_icon_white.png");
        } else if (GUITheme.LIGHT.asString().equalsIgnoreCase(theme)) {
        	loadImage = Util.loadImage("resources/jmars_icon_black.png");
        }		
		if (loadImage == null) {
			loadImage = Util.loadImage("resources/jmars_icon.png");//default to a known png if image is missing
		}
		return loadImage;
	}
	
	/**
	 * Gets the jmars icon and returns it as a bufferedImage.
	 * 
	 * @return a BufferedImage containing the JMars icon
	 */
	public static BufferedImage getGenericJMarsIcon() {
		BufferedImage loadImage = null;
		loadImage = Util.loadImage("resources/jmars_icon_white.png");
		if (loadImage == null) {
			loadImage = Util.loadImage("resources/jmars_icon.png");// default to a known png if image is missing
		}
		return loadImage;
	}

	/**
	 * Gets the non-white JMARS icon for white backgrounds.
	 * @return a BufferedImage containing the JMARS icon
	 */
	public static BufferedImage getNonWhiteJMARSIcon() {
		BufferedImage loadImage = Util.loadImage("resources/jmars_icon_green64.png");
		return loadImage;
	}
	/**
	 * Returns longer white and color logo with a size of 715x260
	 */
	public static BufferedImage getJMarsLargeLogo(){
		return Util.loadImage("resources/jmars_large_icon.png");
	}

	/**
	 *	Returns the longer solid white logo
	 */
	public static BufferedImage getJMarsLargeLogoWhite(){
		return Util.loadImage("resources/jmars_large_icon_white.png");
	}
	
	/**
	 *  Returns the generic splash screen background image (stary sky) 
	 *  with the color long jmars logo in the top third
	 */
	public static BufferedImage getGenericSplashScreen(){
		return Util.loadImage("resources/generic_splash.png");
	}
	
	/**
	 * Returns the capturing groups in pattern 'p' found in the string 's', or
	 * an empty array if no groups were found
	 */
	public static String[] getMatches(Pattern p, String s) {
		Matcher m = p.matcher(s);
		if (!m.matches())
			return new String[0];
		String[] out = new String[m.groupCount()];
		for (int i = 0; i < out.length; i++) {
			out[i] = m.group(i+1);
		}
		return out;
	}
	
	/**
	 * Returns the capturing groups in pattern 'p' found in the string 's', or
	 * an empty array if no groups were found
	 */
	public static String[] getMatchesWithFind(Pattern p, String s) {
		Matcher m = p.matcher(s);
		if (!m.find())
			return new String[0];
		String[] out = new String[m.groupCount()];
		for (int i = 0; i < out.length; i++) {
			out[i] = m.group(i+1);
		}
		return out;
	}
	/**
	 * @return a new grayscale BufferedImage with the linear grayscale colorspace,
	 * since the default grayscale colorspace has a non-linear ramp that distorts
	 * image colors.
	 */
	public static BufferedImage createGrayscaleImage(int w, int h, boolean hasAlpha) {
		ColorSpace cs = Util.getLinearGrayColorSpace();
		int trans = hasAlpha ? ColorModel.TRANSLUCENT: ColorModel.OPAQUE;
		ColorModel destCM = new ComponentColorModel(cs, hasAlpha, false, trans, DataBuffer.TYPE_BYTE);
		SampleModel outModel = new BandedSampleModel(DataBuffer.TYPE_BYTE, w, h, destCM.getNumComponents());
		WritableRaster outRaster = Raster.createWritableRaster(outModel, null);
		return new BufferedImage(destCM, outRaster, destCM.isAlphaPremultiplied(), null);
	}
	
	/**
	 * Return an Iterable that iterates over all elements of each collection in
	 * the given order, without copying into a new collection.
	 */
	public static <E> Iterable<E> iterate(final Collection<? extends E> ... cArray) {
		return new Iterable<E>() {
			public Iterator<E> iterator() {
				return new Iterator<E> () {
					private int idx = 0;
					private Iterator<? extends E> it = cArray[0].iterator();
					private Iterator<? extends E> getIt() {
						if (!it.hasNext()) {
							idx ++;
							if (idx < cArray.length) {
								it = cArray[idx].iterator();
							}
						}
						return it;
					}
					public boolean hasNext() {
						return getIt().hasNext();
					}
					public E next() {
						return getIt().next();
					}
					public void remove() {
						it.remove();
					}
				};
			}
		};
	}
	/**
	 * This method is used to get the prefix used to retrieve product and body prefixed values in config. For example, 
	 * jmars.mars.server would return jmars.mars. so that calling methods can just append the value "server" that was desired and
	 * the value for the current body will be returned. 
	 * @return
	 * @since change bodies
	 */
	public static String getProductBodyPrefix() {
		if (configPrefix == null) {//if the prefix has already been determined, just return it
			configPrefix = "";
			TreeMap<String, String[]> mapOfBodies = new TreeMap<String,String[]>();//Map to store the bodies and possibly the first level such as "jupiter,{io,europa}".
			String currentProduct = Config.get(Config.CONFIG_PRODUCT);
			if (bodyList == null || bodyList.size() == 0) {//if the body list has already been created, do not create it again, it does not change in one session
				//public releases may have multiple bodies
				TreeMap<String, String> mapOfLevels = (TreeMap<String, String>) Config.getValuesByPrefix(currentProduct + ".level");//get the levels from the config for the product.
				if (mapOfLevels.size() > 0) {//if there are no levels set, we will go look for a list of bodies
					//one entry for each level i.e. jmoj.level.1  Jupiter
					Iterator<String> levelIter = mapOfLevels.keySet().iterator();//get the keySet for the levels from config
					while(levelIter.hasNext()) {
						//get all of the bodies for that level i.e. jmoj.jupiter.io
						String levelStr = mapOfLevels.get(levelIter.next()).toLowerCase();//get the value from the level "jmoj.level.1 = Jupiter, lowercase it
						//put each level into the list of bodies
						mapOfBodies.put(levelStr, new String[]{levelStr});
						
						//now check for sub levels for that level
						String configTempValue = currentProduct + ".sub_level." + levelStr;
						TreeMap<String, String> mapOfOneLevel = (TreeMap<String, String>) Config.getValuesByPrefix(configTempValue);//get the bodies for product.sub_level i.e. jmoj.jupiter.1
						Iterator<String> bodyIter = mapOfOneLevel.keySet().iterator();//get the keySet for the product.level entries
						if (bodyIter.hasNext()) {
							mapOfBodies.put(levelStr, null);
						}
						int ct = 0;
						while(bodyIter.hasNext()) {
							String body = mapOfOneLevel.get(bodyIter.next());//get the body for the product.level
							String[] oneLevel = mapOfBodies.get(levelStr);//get the String[] for this level i.e. jupiter
							if (oneLevel == null) {
								//jupiter - {io,europa...}
								oneLevel = new String[mapOfOneLevel.size()];//if it has not yet been set, create a new String[] for this level
								mapOfBodies.put(levelStr,oneLevel);//put the String[] in the main Map by level - String[]
							}
							oneLevel[ct] = body;//set the value of the body in the String[] for this level
							ct++;
						}
					}
				} else {
					//get the config values that defines what bodies are associated with which product (i.e. jmars.bodies.1  Mars)
					Map<String,String> tempBodyList = Config.getValuesByPrefix(currentProduct + ".bodies");//get the values from config for product.bodies
					Iterator<String> iterator = tempBodyList.keySet().iterator();//get the iterator for the keyset
					mapOfBodies.put("",new String[tempBodyList.size()]);//put a new entry in the mapOfBodies that will have an empty String as the key
					int ct = 0;
					while (iterator.hasNext()) {
						String[] bodyStrArr = mapOfBodies.get("");//get the empty key String[] to store the bodies
						String body =  tempBodyList.get(iterator.next());//get the body from the config list
						bodyStrArr[ct] = body;//put the body in the String array
						ct++;
					} 
				}
			}//end create body list
			//at this point we have a Map (mapOfBodies) that either has names for the first level, such as jupiter, {io,europa}, or has empty string "",{io,europa,ganymede}. If we have "", we will not display
			//a first level
			String currentBody = Main.getCurrentBody();//get the currentBody from Main
			if (currentBody == null) {//startup
				String selectedBody = Config.get(Config.CONFIG_SELECTED_BODY,"");//did they have a recently selected body in config
				if (selectedBody != null && !"".equals(selectedBody.trim())) {
					//use the selected body, but first, let's make sure that it is one in our list. It may not be if they last used a different product
					Iterator<String> iter = mapOfBodies.keySet().iterator();//get the bodies
					while (iter.hasNext()) {
						String[] bodies = mapOfBodies.get(iter.next());//bodies for a level
						for (String body : bodies) {
							if (selectedBody.trim().equalsIgnoreCase(body.trim())) {//if selectedBody matches, set the currentBody
								currentBody = selectedBody;
							}
						}
					}
				}
				if (currentBody == null) {
					//we did not find a valid selected body either because it did not match any valid bodies, or it was not set
					if (mapOfBodies.get(mapOfBodies.firstKey()).length > 0) {
						if (mapOfBodies.containsKey("mars")) {
							currentBody = mapOfBodies.get("mars")[0];
						} else {
							currentBody = mapOfBodies.get(mapOfBodies.firstKey())[0];//get the first Map entry and the first value in the String[] for that entry
						}
					} else {
						currentBody = "Mars";
					}
				}
				configPrefix = currentBody.toLowerCase()+".";//this is our body prefix
				bodyList = mapOfBodies;//set the body list for Main
				Main.setCurrentBody(currentBody);//set the current body on Main
			} else {
				configPrefix = currentBody.toLowerCase()+".";//body prefix
			}
		}
		return configPrefix;
	}
	/**
	 * This method is used when switching bodies and do not have relevant default values
	 * @since change bodies 
	*/
	public static void updateRadii() {
		updateRadii(0.0,0.0,0.0);//make these doubles so that it uses the Config method that gets doubles. Some radius values may be doubles. 
	}
	/**
	 * This method is used to initially set the polar values or update them to make sure they are updated for the current body
	 * @param defaultPolar
	 * @param defaultEquat
	 * @param defaultMean
	 * @since change bodies
	 */
	public static void updateRadii(double defaultPolar, double defaultEquat, double defaultMean) {
		//we have changed bodies, so reset the configPrefix value
		configPrefix = null;
		//now set the value so all other classes can access the new value
		configPrefix = getProductBodyPrefix();
		
		//update the radii 
		POLAR_RADIUS = Config.get(configPrefix + "polar_radius",defaultPolar);
		EQUAT_RADIUS = Config.get(configPrefix + "equat_radius", defaultEquat);
		MEAN_RADIUS = Config.get(configPrefix + "mean_radius", defaultMean);
		BODY_FLATTENING = 1 - (POLAR_RADIUS / EQUAT_RADIUS);
		G2C_SCALAR = (1-BODY_FLATTENING)*(1-BODY_FLATTENING);
		FeatureProviderSHP.resetValues();
		MapServerDefault.updateDefaultName();
		MapSourceDefault.updateDefaultName();
	}
    //The following values get initialized in a static block to make sure they get the correct values from the config file
    /**
     ** The body polar radius, in km.
	 ** @since change bodies
     **/
    public static double POLAR_RADIUS; 

    /**
     ** The body equatorial radius, in km.
	 ** @since change bodies
     **/
    public static double EQUAT_RADIUS;

	/**
	 ** The body mean radius, in km.
	 ** @since change bodies
	 **/
	public static double MEAN_RADIUS;

    /**
     ** The flattening coefficient of the planetary ellipsoid.
	 ** @since change bodies
     **/
    public static double BODY_FLATTENING;

    /**
     ** Convenience constant for conversions.
	 ** @since change bodies
     **/
    public static double G2C_SCALAR;
    
    // @since change bodies	
	//initialize the radii, default values sent are for Mars
	static {
		updateRadii(3376.20, 3396.19, 3386);
		setShowBodyMenuFlag();
	}
	
	/**
	 * @since change bodies
	 */
	private static void setShowBodyMenuFlag() {
		showBodyMenuFlag = Config.get(Config.get(Config.CONFIG_PRODUCT) + "." + Config.CONFIG_SHOW_BODY_MENU,false);
	}
	/**
	 * Proper case a label
	 */
	public static String properCase(String val) {
		String[] arr = val.split(" ");
		StringBuilder builder = new StringBuilder();
		for (int x=0; x<arr.length; x++) {
			builder.append(Character.toUpperCase(arr[x].charAt(0)));
			builder.append(arr[x].substring(1));
			builder.append(" ");
		}
		return builder.toString();
	}
	/**
	 * Returns the version number from config
	 * @return
	 */
	public static String getVersionNumber() {
		String versionNum = Config.get("version_number", "n/a");
		return versionNum;
	}
	/**
	 * Upper cases the String passed in
	 */
	public static String toProperCase(String text) {
		if (text == null) {
			return "Mars";
		}
		return text.substring(0,1).toUpperCase() + text.substring(1);
	}
	
	/**
	 * @param key
	 * @param defaultProperty
	 * @return System property or the supplied default property
	 */
	public static String getSafeSystemProperty(String key, String defaultProperty) {
		String retProp = null;
		try {
			retProp = System.getProperty(key, defaultProperty);
		} catch (SecurityException se) {
			retProp = defaultProperty;
		}
		
		return retProp;
	}
	
	public static ArrayList<ArrayList<Point2D>> generalPathToCoordinates(GeneralPath gp) {

		double[] prevcoords = new double[6];
		double[] coords = new double[6]; 
		ArrayList<ArrayList<Point2D>> paths = new ArrayList<ArrayList<Point2D>>();
		ArrayList<Point2D> vertices = null;

		PathIterator pi;
		for (pi = gp.getPathIterator(null); !pi.isDone(); pi.next()){

			switch(pi.currentSegment(coords)){

			case PathIterator.SEG_MOVETO:
				if (vertices!=null) {
					paths.add(vertices);
				}
				for (int i=0; i<6; i++) prevcoords[i]=Integer.MIN_VALUE;
				vertices=new ArrayList<Point2D>();
			case PathIterator.SEG_LINETO:
				vertices.add(new Point2D.Double(coords[0],coords[1]));
				System.arraycopy(coords, 0, prevcoords, 0, 6);
				break;

			case PathIterator.SEG_CLOSE:
				if (vertices!=null) {
					paths.add(vertices);
					vertices=null;
				}
				break;

			default:
			}
		}
		
		return paths;
	}
	
	
	public static boolean intersectsInWorldCoords(GeneralPath shape, Rectangle2D boundingBox){
		
	//Simple check	
		if(shape.intersects(boundingBox)){
			return true;
		}
		
	//Next, change the shape: make sure the shape is greater than 0
		PathIterator pi = shape.getPathIterator(new AffineTransform());
		GeneralPath newShape = new GeneralPath();
		ArrayList<Point2D> pts = new ArrayList<Point2D>();
		while(!pi.isDone()){
			double[] coords = new double[6];
			pi.currentSegment(coords);
			
			if(coords[0]<0){
				coords[0] = 360+coords[0];
			}
			
			Point2D pt = new Point2D.Double(coords[0], coords[1]);
			pts.add(pt);
			
			pi.next();
		}
		for(Point2D pt : pts){
			if(newShape.getCurrentPoint() == null){
				newShape.moveTo(pt.getX(), pt.getY());
			}else{
				newShape.lineTo(pt.getX(), pt.getY());
			}
		}
		if(newShape.intersects(boundingBox)){
			return true;
		}
		
	//Change the box: make sure the box doesn't start past 360
		double x = boundingBox.getX();
		if(x>360){
			while(x>360){
				x=x-360;
			}
			boundingBox = new Rectangle2D.Double(x,boundingBox.getY(),boundingBox.getWidth(), boundingBox.getHeight());
		}

		if(newShape.intersects(boundingBox)){
			return true;
		}
		
	//Split the box: if the box starts before 360, but passes over, we have 
	// to break it into two boxes to catch landing sites on 
	// both sides.
		x = boundingBox.getX();
		double w = boundingBox.getWidth();
		if(x<360 && (x+w)>360){
			double w1 = 360-x;
			Rectangle2D bb1 = new Rectangle2D.Double(x,boundingBox.getY(),w1,boundingBox.getHeight());
			double w2 = (x+w)-360;
			Rectangle2D bb2 = new Rectangle2D.Double(0,boundingBox.getY(),w2,boundingBox.getHeight());
			
			if(newShape.intersects(bb1)){
				return true;
			}
			if(newShape.intersects(bb2)){
				return true;
			}
		}

		
		return false;
	}
	
	/**
	 * Compute signed modulo distance to go from the "from" to the "to" value, mod "mod".
	 * CAUTION: signed distance only works till mod/2
	 * @param from From value
	 * @param to To value
	 * @param mod Modulo
	 * @return signed shortest distance to go from "from" to "to"
	 */
	public static double dist(double from, double to, double mod){
		double dist;
		double fromMod = from % mod;
		double toMod = to % mod;
		
		double d1 = Math.abs(toMod+mod - fromMod) % mod;
		double d2 = Math.abs(fromMod+mod - toMod) % mod;
		dist = Math.min(d1, d2);

		if (Math.abs(((fromMod + dist) % mod) - toMod) < 1E-6){
			// positive distance
		}
		else {
			// negative distance
			dist = -dist;
		}
		
		return dist;
	}
	
	/**
	  * Checks if an input stream is gzipped.
	  * 
	  * @param in suspected GZIPed InputStream
	  * @return true if GZIPed
	  */
	public static boolean isGZipped(InputStream in) throws IOException {
		if (!in.markSupported()) {
			in = new BufferedInputStream(in);
		}
		in.mark(2);
		int magic = 0;
		try {
			magic = in.read() & 0xff | ((in.read() << 8) & 0xff00);
			in.reset();
		} catch (IOException e) {
			log.aprint(e.getMessage());
			return false;
		}
		return magic == GZIPInputStream.GZIP_MAGIC;
	}
	 
	 /**
	  * Checks if a file is gzipped.
	  * 
	  * @param f suspected GZIPed file
	  * @return true if GZIPed
	  */
	public static boolean isGZipped(File f) throws IOException {
		int magic = 0;
		try {
			RandomAccessFile raf = new RandomAccessFile(f, "r");
			magic = raf.read() & 0xff | ((raf.read() << 8) & 0xff00);
			raf.close();
		} catch (IOException e) {
			log.aprint(e.getMessage());
		}
		return magic == GZIPInputStream.GZIP_MAGIC;
	}
	
	/**
	 * <Description>
	 *
	 * @param zipped a ZIP compressed file source
	 * @return a list of all the files unzipped from the zipped source
	 * @throws IOException
	 *
	 * should be thread safe
	 */
	public static ArrayList<File> unZipFile(File zipped) throws IOException {
		ArrayList<File> retFiles = new ArrayList<>();
		BufferedOutputStream dest = null;
		BufferedInputStream is = null;
		ZipEntry entry;
		ZipFile zipfile = new ZipFile(zipped);
		Enumeration<? extends ZipEntry> e = zipfile.entries();
		while(e.hasMoreElements()) {
		   entry = (ZipEntry) e.nextElement();
		   log.aprint("Extracting zipped file: " +entry+"\n");
		   is = new BufferedInputStream
		     (zipfile.getInputStream(entry));
		   int count;
		   byte data[] = new byte[ZIP_BUFFER];
		   FileOutputStream fos = new 
		     FileOutputStream(entry.getName());
		   dest = new 
		     BufferedOutputStream(fos, ZIP_BUFFER);
		   while ((count = is.read(data, 0, ZIP_BUFFER)) 
		     != -1) {
		      dest.write(data, 0, count);
		   }
		   dest.flush();
		   dest.close();
		   is.close();
		   retFiles.add(new File(entry.getName()));
		}
		zipfile.close();
		return retFiles;
	 }
	
	
	
	/**
	 * Calculate a checksum for an image by first converting 
	 * it to a byte[].  Used code found on stack overflow:
	 * 
	 * https://stackoverflow.com/questions/4113890/how-to-calculate-the-internet-checksum-from-a-byte-in-java
	 * 
	 * that was modified to muliply by index position, so that
	 * similar images would not return equal checksums (eg.
	 * two images with a shape in a slightly different spot,
	 * or the "mirror" of an image compared to the original).
	 * Also modified to use % conditional, instead of decreasing
	 * by two each iteration.
	 *
	 * @param bi  A buffered image
	 * @return  A checksum in long format
	 * @throws IOException  Writes the image to convert to a byte array
	 */
	public static long calcChecksumForImage(BufferedImage bi){
		long sum = 0;
		
		try{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(bi, "tif", baos);
			baos.flush();
			byte[] buf = baos.toByteArray();
			baos = null;
			
			int length = buf.length;
			int i = 0;
		    while (length > 0) {
		    	if ((i%2) == 0){
		    		sum += ((i*buf[i++])&0xff) << 8;
		    	}
		    	else {
			        sum += ((i*buf[i++])&0xff);
		    	}
		        --length;
		    }
		    sum = (~((sum & 0xFFFF)+(sum >> 16)))&0xFFFF;
		}
		catch(IOException ex){
			log.aprint("Could not write out image to convert to byte array when calculating checksum.  Checksum is not accurate!");
			ex.printStackTrace();
			sum = (long)(Math.random()*Long.MAX_VALUE);
		}
		
	    return sum;
	}
	
	public static Runnable getCheckForUpdatesRunnable() {
		Runnable checkForUpdates = new Runnable() {
		    public void run() {
		    	try {
		    		// This will return immediately if you call it from the EDT,
		    		// otherwise it will block until the installer application exits
		    		ApplicationLauncher.launchApplication("784", null, true, new ApplicationLauncher.Callback() {
		    		        public void exited(int exitValue) {
		    		        }
		    		        
		    		        public void prepareShutdown() {
		    		        }
		    		    }
		    		);
				} catch (Exception e) {
					log.print(e.getMessage());
					// if the user is running a stand alone jar file, let them know updating is not an option
					Util.showMessageDialog("The Update feature is not currently available in this version.", "JMARS", JOptionPane.ERROR_MESSAGE);
				}
			}
		};
		return checkForUpdates;
	}

	public static List<SavedLayer> getSavedLayersFromStream(InputStream stream) {
		return SavedLayer.load(stream);
	}
	public static void loadSavedLayers(File f) {
		try {
			InputStream stream = new FileInputStream(f);
			List<SavedLayer> layers = getSavedLayersFromStream(stream);
			materializeSavedLayers(layers, null);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//Refactored this so that it can be used in both the Add Layer Listener and in TestDriverlayered
	public static void loadSavedLayers(String url, LayerParameters l) throws URISyntaxException, IOException {
		if (url == null) {
            System.err.println("Could not read URL for session: " + (l == null ? "null" : l.name));
            return;
        }
        JmarsHttpRequest request = new JmarsHttpRequest(url, HttpRequestType.GET);
        request.setBrowserCookies();
        request.setConnectionTimeout(10 * 1024);
        boolean fetchSuccessful = request.send();
        if (!fetchSuccessful) {
            int code = request.getStatus();
            System.err.println("Util: HTTP code " + code + " received when downloading session from " + url);
            return;
        }
        List<SavedLayer> layers = getSavedLayersFromStream(request.getResponseAsStream());
        materializeSavedLayers(layers, l);
        request.close();
	}
	public static void materializeSavedLayers(List<SavedLayer> layers, LayerParameters l) {
		//handle SavedLayers with multiple Layers in the JLF. 
        //Bring them in last to first to keep the same order as they were saved.
        //Making the first one selected because there is only one known case, it is for a demo, desired layer is being selected.
        LView selectedView = null;
        for (int c=layers.size()-1; c>=0; c--) {
            SavedLayer layer = layers.get(c);
            LView lview = layer.materialize(l);
            if (selectedView == null) {
                selectedView = lview;
            }
        }
        if (selectedView != null) {
            LManager.getLManager().setActiveLView(selectedView);
        }
        LManager.getLManager().repaint();
	}
	
	/**
	 * A method that accepts an array of numeric values - Float[], Double[], etc and returns it as a formatted string suitable for display 
	 * in a table or tooltip. eg: "{1.2, 3.5, 8.5}"
	 * 
	 * @param numericArray - the array of numeric objects
	 * @return the formatted String
	 */
	public static String formatNumericArray(Object numericArray) {
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(6);

		StringBuffer outBuffer = new StringBuffer(100);

		if (numericArray.getClass().isArray()) {
			if (Array.getLength(numericArray)>0) {
				Object firstNum = Array.get(numericArray,  0);
				if (Number.class.isAssignableFrom(firstNum.getClass())) {
			    	for (int i=0; i<Array.getLength(numericArray); i++) {
			    		if (outBuffer.length()>0) {
			    			outBuffer.append(", ");
			    		}
			    		Object val = Array.get(numericArray,  i);
			    		outBuffer.append((val == null) ? "" : (Double.isNaN(((Number)val).doubleValue())? "NaN": nf.format(val)));
			    	}
				}
			}
		} 
		
    	String outStr = "{"+outBuffer.toString()+"}";

    	return outStr;
	}	
	
	public static String getMd5Hash(String input) {
		String returnVal = null; 
		try {  
            // Static getInstance method is called with hashing MD5 
            MessageDigest md = MessageDigest.getInstance("MD5"); 
  
            // digest() method is called to calculate message digest 
            //  of an input digest() return array of byte 
            byte[] messageDigest = md.digest(input.getBytes()); 
  
            // Convert byte array into signum representation 
            BigInteger no = new BigInteger(1, messageDigest); 
  
            // Convert message digest into hex value 
            String hashtext = no.toString(16); 
            while (hashtext.length() < 32) { 
                hashtext = "0" + hashtext; 
            } 
            returnVal = hashtext;
		} catch (NoSuchAlgorithmException e) { 
			log.println("Invalid hash type in Util.");
	    } 
		return returnVal;
	}
	public static void writeLoginHash() {
		String hash = getMd5Hash(Main.USER + Main.PASS);
		File f = new File(Main.getJMarsPath()+".jmarsinstallcfg");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(f, false);
			fos.write(hash.getBytes());
		} catch (Exception e) {
			log.println("Unable to write login hash");
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					log.println("failed to close hash stream in Util");
				}
			}
		}
	}
	public static boolean compareLoginHash() {
		boolean returnVal = false;
		File f = new File(Main.getJMarsPath()+".jmarsinstallcfg");
		FileInputStream fis = null;
		if (f.exists()) {
			try {
				fis = new FileInputStream(f);
				String hash = Util.readLines(fis)[0];
				if (hash != null && hash.equals(getMd5Hash(Main.USER + Main.PASS))) {
					returnVal = true;
				}
			} catch (Exception e) {
				log.println("Error reading login hash in Util");
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) {
						log.println("Error closing login stream in Util");
					}
				}
			}
		}
		return returnVal;
	}
	
	/**
	 * Compare two arraylists of strings and determine if they are different, based
	 * on the contents of the strings (order and case sensitive)
	 * @param one  The first arraylist of strings
	 * @param two  The second arraylist of strings
	 * @return  Returns false if the two lists have the same number of elements and 
	 * if each string from the first array equals the corresponding string in the
	 * second array.
	 * Returns true if the lists are different (different sizes, or if any of the
	 * strings from the first array do not equal the corresponding string in the 
	 * second array)
	 */
	public static boolean listsDifferent(ArrayList<String> one, ArrayList<String> two){
		boolean result = false;
		
		//if one array is null and the other is not, no need to continue
		if(one == null || two == null){
			//if both lists are null, they're the same
			if(one == two){
				return false;
			}
			return true;
		}
		
		//if the arrays are different size, no need to continue
		if(one.size() != two.size()){
			return true;
		}
		
		//cycle through each string in the first array and check if it equals
		// the corresponding string in the second array
		int index = 0;
		for(String s : one){
			//if the string does not match, the lists are different
			if(!s.equals(two.get(index))){
				return true;
			}
			index++;
		}
		
		//passed all checks, so the lists are the same (not different)
		return result;
	}
}
