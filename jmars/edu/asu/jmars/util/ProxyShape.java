package edu.asu.jmars.util;

import java.awt.*;
import java.awt.geom.*;

public class ProxyShape implements Shape
 {
    protected final Shape sh;

    public ProxyShape(Shape sh)
     {
	this.sh = sh;
     }

    public final Rectangle getBounds()
     {
	return  sh.getBounds();
     }

    public final Rectangle2D getBounds2D()
     {
	return  sh.getBounds2D();
     }

    public final boolean contains(double x, double y)
     {
	return  sh.contains(x, y);
     }

    public final boolean contains(Point2D p)
     {
	return  sh.contains(p);
     }

    public boolean intersects(double x, double y, double w, double h)
     {
	return  sh.intersects(x, y, w, h);
     }

    public boolean intersects(Rectangle2D r)
     {
	return  sh.intersects(r);
     }

    public final boolean contains(double x, double y, double w, double h)
     {
	return  sh.contains(x, y, w, h);
     }

    public final boolean contains(Rectangle2D r)
     {
	return  sh.contains(r);
     }

    public final PathIterator getPathIterator(AffineTransform at)
     {
	return  sh.getPathIterator(at);
     }

    public final PathIterator getPathIterator(AffineTransform at,
					      double flatness)
     {
	return  sh.getPathIterator(at, flatness);
     }
 }
