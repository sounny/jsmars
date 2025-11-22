package edu.asu.jmars.layer;

import edu.asu.jmars.*;
import edu.asu.jmars.util.*;
import java.awt.*;
import java.awt.geom.*;

public class NullProjection extends MultiProjection
 {
	protected SingleProjection createScreen()
	 {
		return  new NullSingle();
	 }
	protected SingleProjection createWorld()
	 {
		return  new NullSingle();
	 }
	protected SingleProjection createSpatial()
	 {
		return  new NullSingle();
	 }

	public AffineTransform getScreenToWorld()
	 {
		return  null;
	 }
	public AffineTransform getWorldToScreen()
	 {
		return  null;
	 }

	public Shape getWorldWindowMod()
	 {
		return  null;
	 }

	public Rectangle getScreenWindow()
	 {
		return  null;
	 }

	public Rectangle2D getWorldWindow()
	 {
		return  null;
	 }

	public Dimension getScreenSize()
	 {
		return  null;
	 }
	public float getPixelWidth()
	 {
		return  0;
	 }

	public float getPixelHeight()
	 {
		return  0;
	 }

	public int getPPDLog2()
	 {
		return  0;
	 }

	public int getPPD()
	 {
		return  0;
	 }

	private class NullSingle extends SingleProjection
	 {
		public Point2D toScreen(double x, double y)
		 {
			return  null;
		 }
		public Point2D toScreenLocal(double x, double y)
		 {
			return  null;
		 }
		public Point2D toWorld(double x, double y)
		 {
			return  null;
		 }
		public Point2D toSpatial(double x, double y)
		 {
			return  null;
		 }
		public Point2D fromHVector(double x, double y, double z)
		 {
			return  null;
		 }

		public double distance(double ax, double ay,
							   double bx, double by)
		 {
			return  0;
		 }
		public double distance(double a1x, double a1y,
							   double a2x, double a2y,
							   double px, double py)
		 {
			return  0;
		 }
		public boolean hitTest(double a1x, double a1y,
							   double a2x, double a2y,
							   double b1x, double b1y,
							   double b2x, double b2y)
		 {
			return  false;
		 }
		public Point2D nearPt(double a1x, double a1y,
							  double a2x, double a2y,
							  double px, double py,
							  double maxDist)
		 {
			return  null;
		 }
	 }
 }
