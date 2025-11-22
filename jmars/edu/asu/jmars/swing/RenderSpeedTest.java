package edu.asu.jmars.swing;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

public class RenderSpeedTest {
	private Random r = new Random(0);
	private int testSize = 10000;

	public static void main(String[] args) {
		new RenderSpeedTest();
	}
	private static void log(String msg) {
		System.out.println(msg);
	}
	public RenderSpeedTest () {
		String[] g2Names = {
				"standalone bufferedimage",
				"gc volatileimage",
		};
		Graphics2D[] g2s = {
				new BufferedImage(1000,1000,BufferedImage.TYPE_INT_ARGB).createGraphics(),
				GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].getConfigurations()[0].createCompatibleVolatileImage(1000,1000).createGraphics(),
		};
		String[] colorNames = {"black", "transparent"};
		Color[] colors = {Color.black, new Color(.2f,.3f,.4f,.5f)};
		String[] alphaNames = {"srcover","src"};
		AlphaComposite[] comps = {
				AlphaComposite.getInstance(AlphaComposite.SRC_OVER),
				AlphaComposite.getInstance(AlphaComposite.SRC),
		};
		Test[] tests = {
//				new Points(),
//				new RectOutlines(),
//				new RectsFilled(),
				new ComplexOutlines(),
				new ComplexFilled()
		};
		for (int i = 0; i < g2s.length; i++) {
			for (int j = 0; j < colors.length; j++) {
				for (int k = 0; k < comps.length; k++) {
					for (int l = 0; l < 2; l++) {
						for (int m = 0; m < tests.length; m++) {
							String testName = tests[m].getClass().getName();
							testName = testName.substring(testName.lastIndexOf(".") + 1);
							long time = tests[m].test(style((Graphics2D)g2s[i].create(),l==1,colors[j],comps[k]));
							log(g2Names[i] + ", " + colorNames[j] + ", " + alphaNames[k] + ", " + (l==1) + ", " + testName + ", " + time);
						}
					}
				}
			}
		}
	}
	private Graphics2D style(Graphics2D g2, boolean aa, Color c, AlphaComposite comp) {
		if (aa)
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (c != null)
			g2.setColor(c);
		if (comp != null)
			g2.setComposite(comp);
		return g2;
	}
	interface Test {
		long test(Graphics2D g2);
	}
	private final class Points implements Test {
		public long test(Graphics2D g2) {
			Point[] points = new Point[testSize];
			for (int i = 0; i < points.length; i++) {
				points[i] = new Point((int)(r.nextDouble()*1000), (int)(r.nextDouble()*1000));
			}
			long start = System.currentTimeMillis();
			for (int i = 0; i < points.length; i++) {
				g2.drawLine(points[i].x, points[i].y, points[i].x, points[i].y);
			}
			return System.currentTimeMillis() - start;
		}
	}
	private abstract class Rects implements Test {
		protected Rectangle2D[] getRects() {
			Rectangle2D[] rects = new Rectangle2D[testSize];
			for (int i = 0; i < rects.length; i++) {
				rects[i] = new Rectangle2D.Double((int)(r.nextDouble()*800), (int)(r.nextDouble()*800), 200, 200);
			}
			return rects;
		}
	}
	private final class RectOutlines extends Rects {
		public long test(Graphics2D g2) {
			long start = System.currentTimeMillis();
			for (Rectangle2D r: getRects()) {
				g2.draw(r);
			}
			return System.currentTimeMillis() - start;
		}
	}
	private final class RectsFilled extends Rects {
		public long test(Graphics2D g2) {
			long start = System.currentTimeMillis();
			for (Rectangle2D r: getRects()) {
				g2.fill(r);
			}
			return System.currentTimeMillis() - start;
		}
	}
	private abstract class Complex implements Test {
		protected Shape[] getShapes() {
			Shape[] shapes = new Shape[testSize];
			for (int i = 0; i < shapes.length; i++) {
				int x = (int)(r.nextDouble() * 800);
				int y = (int)(r.nextDouble() * 800);
				shapes[i] = new Area(new Polygon(
						new int[]{x,x+100,x+200,x+100,x,x+100,x},
						new int[]{y,y+100,y+200,y+150,y+100,y+50},
						6));
			}
			return shapes;
		}
	}
	private final class ComplexOutlines extends Complex {
		public long test(Graphics2D g2) {
			long start = System.currentTimeMillis();
			for (Shape s: getShapes()) {
				g2.draw(s);
			}
			return System.currentTimeMillis() - start;
		}
	}

	private final class ComplexFilled extends Complex {
		public long test(Graphics2D g2) {
			long start = System.currentTimeMillis();
			for (Shape s: getShapes()) {
				g2.fill(s);
			}
			return System.currentTimeMillis() - start;
		}
	}
}