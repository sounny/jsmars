package edu.asu.jmars.graphics;

import edu.asu.jmars.*;
import edu.asu.jmars.layer.*;
import edu.asu.jmars.util.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.awt.image.renderable.*;
import java.awt.RenderingHints.Key;
import java.io.*;
import java.lang.*;
import java.text.*;
import java.util.*;

public final class SpatialGraphicsCyl
 extends SpatialGraphics2D
 {
	private static DebugLog log = DebugLog.instance();

	private MultiProjection proj;

	public SpatialGraphicsCyl(Graphics2D g2, MultiProjection proj)
	 {
		this.g2 = g2;
		this.proj = proj;
	 }

	private Shape toWorld(Shape s)
	 {
		GeneralPath gp = new GeneralPath();
		PathIterator iter = s.getPathIterator(null);
		double[] coords = new double[6];
		Point2D pt = new Point2D.Double();
		while(!iter.isDone())
		 {
			switch(iter.currentSegment(coords))
			 {
			 case PathIterator.SEG_MOVETO:
				pt.setLocation(coords[0], coords[1]);
				pt = proj.spatial.toWorld(pt);
				gp.moveTo((float) pt.getX(),
						  (float) pt.getY() );
				break;

			 case PathIterator.SEG_QUADTO:
			 case PathIterator.SEG_CUBICTO:
				log.aprintln("PROGRAMMER: UNHANDLED LINE SEGMENT TYPE!");
			 case PathIterator.SEG_LINETO:
				pt.setLocation(coords[0], coords[1]);
				pt = proj.spatial.toWorld(pt);
				gp.lineTo((float) pt.getX(),
						  (float) pt.getY() );
				break;

			 case PathIterator.SEG_CLOSE:
				gp.closePath();
				break;

			 default:
				log.aprintln("PROGRAMMER: UNKNOWN LINE SEGMENT TYPE!");
				break;
			 }

			iter.next();
		 }

		return  gp;
	 }

    public void draw(Shape s)
     {
		g2.draw(toWorld(s));
	 }

    public void fill(Shape s)
     {
		g2.fill(toWorld(s));
     }

    public Graphics create()
     {
		try
		 {
			SpatialGraphicsCyl copy = (SpatialGraphicsCyl) clone();
			copy.g2 = (Graphics2D) g2.create();
			return  copy;
		 }
		catch(CloneNotSupportedException e)
		 {
			log.aprintln("WAY STRANGE: Clone failed");
			log.aprint(e);
			return  null;
		 }
     }

	// Adapted from drawLineImpl
	public Point2D[] spatialToWorlds(Point2D s)
	 {
		checkWrappedG2();
		Point2D w = proj.spatial.toWorld(s);
		GraphicsWrapped gw = (GraphicsWrapped) g2;
		return  gw.worldToWorlds(w);
	 }

///////////////////////////////////////////////////////////////////////////////
// FUNCTIONS WE CALL BUT WHOSE DEFAULT PROXY IMPLEMENTATIONS WILL SUFFICE
///////////////////////////////////////////////////////////////////////////////

    public void setPaint(Paint paint)
     {
        g2.setPaint(paint);
     }

    public void setStroke(Stroke s)
     {
        g2.setStroke(s);
     }

    public void setColor(Color c)
     {
        g2.setColor(c);
     }

    public Color getColor()
     {
        return  g2.getColor();
     }

    public void setPaintMode()
     {
        g2.setPaintMode();
     }

    public void setXORMode(Color c1)
     {
        g2.setXORMode(c1);
     }

    public Composite getComposite()
    {
        return  g2.getComposite();
    }

    public void setComposite(Composite comp)
    {
        g2.setComposite(comp);
    }
    
	public Font getFont()
	 {
		checkWrappedG2();
		return  g2.getFont();
	 }

    public void setFont(Font font)
     {
		checkWrappedG2();
		g2.setFont(font);
     }

    public void drawString(String str, int x, int y)
     {
		checkWrappedG2();
		Point2D w = proj.spatial.toWorld(x, y);
		( (GraphicsWrapped) g2).drawStringWrapped(str,
												  (float) w.getX(),
												  (float) w.getY());
     }

    public void drawString(String s, float x, float y)
     {
		checkWrappedG2();
		Point2D w = proj.spatial.toWorld(x, y);
		( (GraphicsWrapped) g2).drawStringWrapped(s,
												  (float) w.getX(),
												  (float) w.getY());
     }

	private void checkWrappedG2()
	 {
		if(!(g2 instanceof GraphicsWrapped))
		 {
			log.aprintln("------ WARNING: FONTS WON'T WORK!");
			log.aprintln("       Tell Michael, he screwed up.");
		 }
	 }
 }
