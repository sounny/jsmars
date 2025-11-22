package edu.asu.jmars.layer.groundtrack;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Dimension2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.WatchedThread;
import edu.asu.jmars.swing.ColorCombo;
import edu.asu.jmars.swing.LongField;
import edu.asu.jmars.swing.TimeField;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.TimeException;
import edu.asu.jmars.util.Util;

public class GroundTrackLView
 extends Layer.LView
 {
    private static final DebugLog log = DebugLog.instance();

    private static final int proximityPixels = 8;

    private GroundTrackFactory.LayerParams currLayerPars;

    private static final DecimalFormat f = new DecimalFormat("0.###");

    private class WorldSegs
     {
	Line2D[] segs;
	double minET;
	double maxET;
	double delta;

	double worldToEt(double x)
	 {
	    Line2D xLine = new Line2D.Double(x, -90, x, +90);
	    for(int i=0; i<segs.length; i++)
		if(xLine.intersectsLine(segs[i]))
		 {
		    double x1 = segs[i].getX1();
		    double x2 = segs[i].getX2();
		    double base = i * delta + minET;
		    double offset =
			(x - Math.min(x1, x2)) / Math.abs(x1 - x2) * delta;
		    return  base + offset;
		 }
	    return  Double.NaN;
	 }

	Point2D etToWorld(double et)
	 {
	    if(!Util.between(minET, et, maxET))
		return	null;
	    double idx = (et - minET) / delta;
	    double x1 = segs[(int) idx].getX1();
	    double x2 = segs[(int) idx].getX2();
	    double worldX =
		Math.min(x1, x2) + (idx-Math.floor(idx)) * Math.abs(x1-x2);
	    double worldY =
		getProj().world.fromHVector(estimatePos(et)).getY();
	    return  new Point2D.Double(worldX, worldY);
	 }
     }

    private WorldSegs worldSegs = new WorldSegs();
    private WorldSegs getWorldSegs()
     {
	synchronized(worldSegs)
	 {
	    if(worldSegs.segs != null)
		return	worldSegs;

	    // Calculate the primary range
	    int beg = (int) etToIndex(centerEt - PRIMARY_RADIUS);
	    int ctr = (int) etToIndex(centerEt		       );
	    int end = (int) etToIndex(centerEt + PRIMARY_RADIUS);

	    beg = Util.bound(0, beg, segs.length);
	    end = Util.bound(0, end, segs.length);

	    if(!Util.between(0, ctr, segs.length))
	     {
		log.aprintln("Primary segments out of range: " +
			     ctr + " " + segs.length);
		throw new Error("Shouldn't happen: segments out of range!");
	     }

	    Line2D[] begList = createWorldSegs(ctr, beg);
	    Line2D[] endList = createWorldSegs(ctr, end);

	    worldSegs.minET = indexToEt(beg + 1);
	    worldSegs.maxET = indexToEt(end);
	    worldSegs.delta = pars.request.delta;
	    worldSegs.segs = new Line2D[end - beg - 1];
	    System.arraycopy(begList, 0, worldSegs.segs, 0,  begList.length);
	    System.arraycopy(endList, 0, worldSegs.segs,
			     Math.max(begList.length-1,	 0), endList.length);

	    return  worldSegs;
	 }
     }

    /**
     ** Returns Line2D objects from the given indices, properly placed
     ** in the world with respect to the current centerX and
     ** centerWorld. 
     **/
    private Line2D[] createWorldSegs(int ctr, int end)
     {
	Line2D[] list = new Line2D[Math.abs(end - ctr)];

	final int dir = ctr<end ? +1 : -1;

	double lastX = centerWorld.getX();
	for(int i=ctr; i!=end; i+=dir)
	 {
	    Line2D.Double worldLine =
		new Line2D.Double(getProj().spatial.toWorld(segs[i].getP1()) ,
				  getProj().spatial.toWorld(segs[i].getP2()) );
	    int period = (int) Math.floor(lastX / 360) * 360;

	    // Does the line wrap? Then normalize it to cross 0 or cross 360.
	    if(Math.abs(worldLine.x1 - worldLine.x2) > 180)
		if(lastX - period < 180)		      // wanna cross 0
		    if(worldLine.x1 < 180) worldLine.x2 -= 360;
		    else		   worldLine.x1 -= 360;
		else					    // wanna cross 360
		    if(worldLine.x1 < 180) worldLine.x1 += 360;
		    else		   worldLine.x2 += 360;
	    // Bring it into the period of the last X ordinate
	    worldLine.x1 += period;
	    worldLine.x2 += period;

	    // Finally, we have the line we want
	    list[dir<0 ? i-end-1 : i-ctr] = worldLine;

	    // Update our state for the next line
	    lastX = (dir<0)
		? Math.min(worldLine.x1, worldLine.x2)
		: Math.max(worldLine.x1, worldLine.x2);
	 }

	return	list;
     }

    public class Pars implements SerializedParameters
     {
	GroundTrackLayer.Request request = null;
//	    new GroundTrackLayer.Request(67011580, 67018740, 20);
	Color begColor = Color.yellow;
	Color endColor = begColor;
	boolean drawPrimary = true;
	boolean drawAlternate = true;
	boolean newborn = true;
	List selection = Collections.EMPTY_LIST;
     };
    Pars pars = new Pars();

    Line2D[] segs = null;

    public GroundTrackLView(GroundTrackFactory.LayerParams pars, GroundTrackLView3D lview3d)
     {
	super(pars.getLayer(), lview3d);

	this.currLayerPars = pars;

	addMouseListener(
	    new MouseAdapter()
	     {
		public void mouseClicked(MouseEvent e)
		 {
		    double et = findTimeByWorldPt(
			getProj().screen.toWorld(e.getPoint()));
		    if(Double.isNaN(et)) {
			log.aprintln("You didn't click on a groundtrack.");
		    } else {
			HVector pos = estimatePos(et);

			System.err.println(TimeField.etToDefault(et) + ", " + 
				     f.format((360-pos.lon())%360) + "E, " +
				     f.format(pos.lat()));
		    }
		 }
	     }
	    );

		if (lview3d!=null) {
			lview3d.setLView(this);
		}
     }

	/**
	* Override to update view specifc settings
	*/
	protected void updateSettings(boolean saving)
	{
	    String key = currLayerPars.craft + "parms";

	    if(saving)
		 {
			if(pars.request != null)
				viewSettings.put(key, new GroundLayerSettings(
									 pars.request.begET,
									 pars.request.endET,
									 pars.request.delta,
									 pars.begColor,
									 pars.endColor ));
			else
				viewSettings.put(key, new GroundLayerSettings(
									 0,
									 0,
									 Integer.MIN_VALUE,
									 pars.begColor,
									 pars.endColor ));
		 }
		else if(viewSettings.containsKey(key))
		 {
			GroundLayerSettings tmp = (GroundLayerSettings)
				viewSettings.get(key);

			if(tmp.delta != Integer.MIN_VALUE)
				pars.request = new GroundTrackLayer.Request(tmp.begET,
															tmp.endET,
															tmp.delta);
			pars.begColor = new Color(tmp.begColor.getRGB());
			pars.endColor = new Color(tmp.endColor.getRGB());

			focusPanel = getFocusPanel();
	     }
	}

    protected Component[] getContextMenu(final Point2D worldPt)
     {
	final double et = findTimeByWorldPt(worldPt);
	if(Double.isNaN(et))
	    return  new Component[0];

	JMenuItem menuItem = new JMenuItem(new AbstractAction(
				"Recenter projection at ET: " + (long) et) {
			{
				setEnabled("true".equals(Config.get("reproj")));
			}

			public void actionPerformed(ActionEvent e) {
				recenterProjection(worldPt, et);
			}
		});
		return new Component[] { menuItem };
	}

    /**
     ** Re-centers the current projection to orient the groundtrack at
     ** the given et to the right side of the screen.
     **
     ** <p>The worldPt should be in pre-reprojection coordinates, and
     ** specifies where to viewChange to after the projection. If it's
     ** null, it's computed to be the groundtrack point at the given
     ** ET value.
     **
     ** @return The new worldPt center, in newly-projected
     ** coordinates.
     **/
    public Point2D recenterProjection(Point2D worldPt, double et)
     {
	try
	 {
	    ProjObj oldPO = Main.PO;

	    HVector p = estimatePos(et);
	    HVector v = estimateVel(et);
	    HVector up = p.cross(v).unit();
	    Main.setProjection(new ProjObj.Projection_OC(up));

	    if(worldPt == null)
		worldPt = oldPO.convSpatialToWorld(p.toLonLat(null));

	    Point2D spatial =
		oldPO.convWorldToSpatial(worldPt);
	    Point2D newWorld =
		Main.PO.convSpatialToWorld(spatial);

	    centerWorld = (Point2D) newWorld.clone();
	    centerEt = et;
	    synchronized(worldSegs)
	     {
		worldSegs.segs = null;
	     }

	    if(getChild() != null)
	     {
		GroundTrackLView child = (GroundTrackLView) getChild();
		child.centerWorld = (Point2D) newWorld.clone();
		child.centerEt = et;
		synchronized(worldSegs)
		 {
		    child.worldSegs.segs = null;
		 }
	     }

	    viewman.getLocationManager().setLocation(newWorld, true);
	    return  newWorld;
	 }
	catch(IllegalArgumentException ex)
	 {
	    log.aprintln(ex);
	    Util.showMessageDialog("Unable to re-center at that point!", "Error", JOptionPane.ERROR_MESSAGE);
	    return  null;
	 }
     }

    public String getName()
     {
    	if (currLayerPars == null)
			return getClass().getSimpleName();
		return	currLayerPars.craft + " Groundtrack";
     }

    public String getToolTipText(MouseEvent e)
     {
	    double et = findTimeByWorldPt(
		    getProj().screen.toWorld(e.getPoint()));
	    if(Double.isNaN(et))
		return null;

	    return  TimeField.etToDefault(et);
     }

    public Object createRequest(Rectangle2D where)
     {
	return	pars.request;
     }

    private double centerEt = Double.NaN;
    private Point2D centerWorld = null;

    private static final double PRIMARY_RADIUS = Double.parseDouble(
	Config.get("groundtrack.primary"));

    private double etToIndex(double et)
     {
	return	(et - pars.request.begET) / pars.request.delta;
     }

    private double indexToEt(int idx)
     {
	return	idx * pars.request.delta + pars.request.begET;
     }

    public synchronized void receiveData(Object layerData)
     {
	if(!isAlive())
	    return;

	if(layerData instanceof Line2D[])
	    receiveGroundTrack( (Line2D[]) layerData );

	else
	 {
	    log.aprintln("INVALID DATA TYPE RECEIVED: " +
			 layerData.getClass().getName());
	    log.aprintStack(-1);
	    return;
	 }
     }

    private void receiveGroundTrack(Line2D[] layerData)
     {
	getLayer().setStatus(Color.yellow);

	synchronized(worldSegs)
	 {
	    if(worldSegs.segs != null)
	     {
		Rectangle2D worldWin = getProj().getWorldWindow();
		double x = worldWin.getCenterX();
		double y = worldWin.getCenterY();
		Point2D centerWorldNew = new Point2D.Double(x, y);
		double centerEtNew = getWorldSegs().worldToEt(x);

		centerEt = centerEtNew;
		centerWorld = centerWorldNew;
		worldSegs.segs = null;
	     }
	 }

	segs = (Line2D[]) layerData;

	redrawEverything(true, false);
     }

    private synchronized void redrawEverything(boolean clearFirst,
					       boolean propagate)
     {
	drawLines(clearFirst, propagate);
     }

    private synchronized void drawLines(boolean clearFirst,
					boolean propagate)
     {
	if(clearFirst)
	    clearOffScreen();

	if(pars.drawAlternate)
	    drawLinesAlternate();

	if(pars.drawPrimary  &&	 centerWorld != null)
	    drawLinesPrimary();

	getLayer().setStatus(Util.darkGreen);

	repaint();

	if(propagate  &&  getChild() != null)
	    ((GroundTrackLView) getChild()).drawLines(clearFirst, false);
     }

    private synchronized void drawLinesAlternate()
     {
	Graphics2D g2 = getProj().createSpatialGraphics(getOffScreenG2());
	g2.setStroke(new BasicStroke(0));
	for(int i=0; i<segs.length; i++)
	 {
	    if(i % 3000 == 0)
		getLayer().setStatus(Color.yellow);
	    Color c = Util.mixColor(
		pars.begColor,
		pars.endColor,
		(double) i / (segs.length-1)
		);

	    g2.setColor(c);
	    g2.draw(segs[i]);
	 }
	g2.dispose();
     }

    private synchronized void drawLinesPrimary()
     {
	Graphics2D g2r = getOffScreenG2Raw(); // no auto wrapping, NOT spatial
	float width = (float) getProj().getPixelSize().getWidth() * 3;
	g2r.setStroke(new BasicStroke(width));

	Line2D[] lines = getWorldSegs().segs;

	g2r.setColor(pars.begColor);
	for(int i=0; i<lines.length; i++)
	 {
	    if(i % 3000 == 0)
		getLayer().setStatus(Color.yellow);
	    g2r.draw(lines[i]);
	 }
	g2r.dispose();
     }

    /**
     ** Given a point that the user has clicked, returns the nearest
     ** time on the displayed groundtracks that's within {@link
     ** #proximityPixels} pixels.
     **/
    private double findTimeByWorldPt(Point2D worldPt)
     {
	if(segs == null)
	    return  Double.NaN;

	    return  findTimeByWorldPt_cyl(worldPt);
     }

    /**
     ** Performs the real work of {@link #findTimeByWorldPt}.
     **/
    private double findTimeByWorldPt_cyl(Point2D worldPt)
     {
	double offset = -Math.min(0, Math.floor(worldPt.getX() / 360) * 360);
	worldPt = new Point2D.Double((worldPt.getX() + offset + 360) % 360,
				     worldPt.getY());
	Dimension2D pixelSize = viewman.getProj().getPixelSize();
	double w = proximityPixels * pixelSize.getWidth();
	double h = proximityPixels * pixelSize.getHeight();
	Rectangle2D proximity =
	    new Rectangle2D.Double(worldPt.getX() - w/2,
				   worldPt.getY() - h/2,
				   w, h);

	// Find the closest segment
	double minDistanceSq = Double.MAX_VALUE;
	int minIndex = -1;
	for(int i=0; i<segs.length; i++)
	 {
	    Line2D worldSeg = new Line2D.Double(
		getProj().spatial.toWorld(segs[i].getP1()),
		getProj().spatial.toWorld(segs[i].getP2()))
	     {
		public String toString()
		 {
		    return
			getX1() + ", " + getY1() + " -> " +
			getX2() + ", " + getY2();
		 }
	     };

	    // Segments that cross the zero line
	    if(Math.abs(worldSeg.getX1() - worldSeg.getX2()) > 180)
	     {
		// If the click-point is near 360, modulo the segment
		// towards 360
		if(worldPt.getX() > 180)
		    if(worldSeg.getX1() < 180)
			worldSeg.setLine(worldSeg.getX1() + 360,
					 worldSeg.getY1(),
					 worldSeg.getX2(),
					 worldSeg.getY2());
		    else
			worldSeg.setLine(worldSeg.getX1(),
					 worldSeg.getY1(),
					 worldSeg.getX2() + 360,
					 worldSeg.getY2());

		// Else the click-point is near zero, modulo the
		// segment towards zero
		else
		    if(worldSeg.getX1() > 180)
			worldSeg.setLine(worldSeg.getX1() - 360,
					 worldSeg.getY1(),
					 worldSeg.getX2(),
					 worldSeg.getY2());
		    else
			worldSeg.setLine(worldSeg.getX1(),
					 worldSeg.getY1(),
					 worldSeg.getX2() - 360,
					 worldSeg.getY2());
	     }

	    if(worldSeg.intersects(proximity))
	     {
		double distanceSq = worldSeg.ptLineDistSq(worldPt);
		if(distanceSq < minDistanceSq)
		 {
		    minDistanceSq = distanceSq;
		    minIndex = i;
		 }
	     }
	 }

	// Did we come up with nothing?
	if(minIndex == -1)
	    return  Double.NaN;

	return	estimateTime(minIndex, worldPt);
     }


    /**
     ** Uses the groundtrack data to estimate a (normalized) velocity
     ** vector at a given time.
     **/
    private HVector estimateVel(double et)
     {
	return	estimatePos(et+5).sub(estimatePos(et-5)).unit();
     }

    /**
     ** Uses the groundtrack data to estimate a (normalized) position
     ** vector at a given time.
     **/
    private HVector estimatePos(double et)
     {
	double beg = pars.request.begET;
	double end = pars.request.endET;
	if(!Util.between(beg, et, end))
	    throw  new IllegalArgumentException("Out-of-range ET value: " +
						beg + " < " +
						et  + " < " + end);
	double index = etToIndex(et);
	int segIdx = (int) index;
	double segOff = index - segIdx;
	if (segs==null){
	    return new HVector();
	}
	HVector a = HVector.fromSpatial( segs[ segIdx ].getP1() );
	HVector b = HVector.fromSpatial( segs[ segIdx ].getP2() );

	HVector pt = a.interpolate(b, segOff);
	log.println(et + "\t" + pt.lon() + "\t" + pt.lat());
	log.println("beg = " + (long) beg);
	log.println("end = " + (long) end);
	log.println("index = " + index);
	log.println("segIdx = " + segIdx);
	log.println("segOff = " + segOff);
	return	pt;
     }

    private double estimateTime(int segIdx, Point2D worldPt)
     {
	// Calculate the "starting" time for that segment
	double segTime = pars.request.begET
	    + (pars.request.endET - pars.request.begET)
	    * (double) segIdx / segs.length;

	// Interpolate along the segment to offset from its starting time
	HVector a = HVector.fromSpatial( segs[ segIdx ].getP1() );
	HVector b = HVector.fromSpatial( segs[ segIdx ].getP2() );
	HVector p = HVector.fromSpatial( getProj().world.toSpatial(worldPt) );
	double offset = HVector.uninterpolate(a, b, p) * pars.request.delta;

	return	segTime + offset;
     }

    public FocusPanel getFocusPanel()
     {
	if(focusPanel == null){
	    focusPanel = new FocusPanel(this,false);
		focusPanel.add("Adjustments", new MyFocus());
     }
	return	focusPanel;
     }

    public Layer.LView dup()
     {
	GroundTrackLView copy = (GroundTrackLView) super.dup();

	copy.pars = this.pars;
	copy.worldSegs = this.worldSegs;

	return	copy;
     }

    public Layer.LView _new()
     {
	return	new GroundTrackLView(currLayerPars, null);
     }

    public static void main(String[] av)
     {
	MyFocus myFocus =
	    new GroundTrackLView(GroundTrackFactory.SPACECRAFT[0], null)
	    .new MyFocus();
	JFrame frame = new JFrame("test");
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.getContentPane().add(myFocus);
	frame.pack();
	frame.setVisible(true);
     }

    private void reformatIfEmpty(TimeField field)
     {
	if(!field.getText().matches(".*\\d+.*"))
	    field.setText(currLayerPars.craft + ":orbit:");
     }

    private class MyFocus
     extends JPanel
     implements ActionListener
     {
	JComboBox comboSpacecraft;
	TimeField txtBeg;
	TimeField txtEnd;
	LongField txtDelta;
	ColorCombo comboBegColor;
	ColorCombo comboEndColor;
	JButton btnUpdate;

	MyFocus()
	 {
	   // super(GroundTrackLView.this);
		
	    comboSpacecraft = new JComboBox(GroundTrackFactory.SPACECRAFT);
	    comboSpacecraft.addActionListener(
		new ActionListener()
		 {
		    public void actionPerformed(ActionEvent e)
		     {
			currLayerPars = (GroundTrackFactory.LayerParams)
			    comboSpacecraft.getSelectedItem();
			pars.request = null;
			reformatIfEmpty(txtBeg);
			reformatIfEmpty(txtEnd);
			setLayer(currLayerPars.getLayer());
			((GroundTrackLView)getChild())
			    .setLayer(currLayerPars.getLayer());
			clearOffScreen();
			LManager.getLManager().updateLabels();
			repaint();
		     }
		 }
		);

	    if(pars.request == null)
	     {
		txtBeg = new TimeField(currLayerPars.craft + ":orbit:");
		txtEnd = new TimeField(currLayerPars.craft + ":orbit:");
		txtDelta = new LongField(120);
	     }
	    else
	     {
		txtBeg =
		    new TimeField(TimeField.etToDefault(pars.request.begET));
		txtEnd =
		    new TimeField(TimeField.etToDefault(pars.request.endET));
		txtDelta = new LongField(pars.request.delta);
	     }
	    txtBeg.setCaretPosition(txtBeg.getText().length());
	    txtEnd.setCaretPosition(txtBeg.getText().length());

	    comboBegColor = new ColorCombo();
	    comboEndColor = new ColorCombo(comboBegColor);
	    comboBegColor.setSelectedItem(pars.begColor);
	    btnUpdate = new JButton("Update".toUpperCase());
	    btnUpdate.setToolTipText("You can also trigger an update by hitting enter in any of the text fields, or by changing either of the colors.");

	    txtBeg.addActionListener(this);
	    txtEnd.addActionListener(this);
	    txtDelta.addActionListener(this);
	    comboBegColor.addActionListener(this);
	    comboEndColor.addActionListener(this);
	    btnUpdate.addActionListener(this);

	    Component noColorCombo = Box.createVerticalStrut(
		comboEndColor.getPreferredSize().height);


	    setLayout(new BorderLayout());
	    JPanel jp = new JPanel();	
	    add(jp, BorderLayout.NORTH);

	    jp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

	    jp.setLayout(new GridBagLayout());
	    GridBagConstraints c = new GridBagConstraints();
	    c.fill = GridBagConstraints.BOTH;
	    c.gridy = GridBagConstraints.RELATIVE;

	    c.gridx = 0; c.weightx = 0;
	    jp.add(new JLabel("Spacecraft:"), c);
	    jp.add(new JLabel(" "), c);
	    jp.add(new JLabel("Start time:"), c);
	    jp.add(new JLabel("End time:"), c);
	    jp.add(new JLabel("Delta secs:  "), c);

	    c.gridx++; c.weightx = 1;   c.gridwidth = 2;
	    jp.add(comboSpacecraft, c);
	    jp.add(new JLabel(" "), c); c.gridwidth = 1;
	    jp.add(txtBeg, c);
	    jp.add(txtEnd, c);
	    jp.add(txtDelta, c);
	    jp.add(new JLabel(" "), c);
	    jp.add(btnUpdate, c);

	    c.gridx++; c.weightx = 0;
	    jp.add(comboBegColor, c);
	    jp.add(comboEndColor, c);
	    jp.add(noColorCombo, c);
	 }

	public void actionPerformed(ActionEvent e)
	 {
	    // If the action's from a color combo and there's no color change
	    if(e.getSource() instanceof ColorCombo &&
	       comboBegColor.getColor().equals(pars.begColor) &&
	       comboEndColor.getColor().equals(pars.endColor) )
		return;

	    pars.begColor = comboBegColor.getColor();
	    pars.endColor = comboEndColor.getColor();

	    Long delta = txtDelta.getLong();

	    try
	     {
		if(delta == null)
		    pars.request = null;
		else
		 {
		    int d = delta.intValue();
		    pars.request = new GroundTrackLayer.Request(
			txtBeg.getEt(),
			txtEnd.getEt(),
			d);
		 }
	     }
	    catch(TimeException ex)
	     {
		Util.showMessageDialogObj(ex, "INVALID TIME",
					      JOptionPane.ERROR_MESSAGE);
		return;
	     }

	    new WatchedThread(
		new Runnable()
		 {
		    public void run()
		     {
			boolean okay = false;
			try
			 {
			    getLayer().setStatus(Color.red);

			    clearOffScreen();
			    getLayer().receiveRequest(pars.request,
						      GroundTrackLView.this);
			    GroundTrackLView.this.repaint();

			    GroundTrackLView child =
				(GroundTrackLView) getChild();
			    if(child != null)
			     {
				child.clearOffScreen();
				getLayer().receiveRequest(pars.request, child);
				child.repaint();
			     }

			    okay = true;
			 }
			finally
			 {
			    getLayer().setStatus(
				okay ? Util.darkGreen : Color.blue);
			 }
		     }
		 }
		).start();
	 }
     }
    
//The following two methods are used to query for the
// info panel fields (description, citation, etc)	
   	public String getLayerKey(){
   		return "Groundtracks";
   	}
   	public String getLayerType(){
   		return "groundtrack";
   	}
}
