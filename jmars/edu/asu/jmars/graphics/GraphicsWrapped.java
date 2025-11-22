package edu.asu.jmars.graphics;

import edu.asu.jmars.*;
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
import edu.asu.jmars.swing.FontUtil;

public final class GraphicsWrapped
 extends Graphics2DAdapter
 {
	private static DebugLog log = DebugLog.instance();

	private Shape clip;
	private float mod;
	private double base;
	private int count;
	private AffineTransform fontTransform;
	private Font currentFont;

	private double min, max;
	private String src;

	// Stuff needed to draw text strings as shapes
	Font baseFont;
	AffineTransform baseFontTransform;
	
	
	private GraphicsWrapped()
	 {
	 }

	public GraphicsWrapped(Graphics2D g2, float mod, int ppd,
						   Rectangle2D rec, String src)
	 {
		this(g2, mod, ppd, rec.getMinX(), rec.getMaxX(), rec.getHeight(), src);
	 }

	public GraphicsWrapped(Graphics2D g2, float mod, int ppd,
						   double min, double max,
						   double ht, String src)
	 {
		this.g2 = g2;
		this.mod = mod;
		this.min = min;
		this.max = max;
		this.src = src;
		base = Math.floor(min / mod) * mod;
		count = (int) Math.ceil(max / mod) - (int) Math.floor(min / mod);
		
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
				RenderingHints.VALUE_FRACTIONALMETRICS_ON);

		fontTransform = AffineTransform.getScaleInstance(1.0/ppd, -1.0/ppd);
		
		/**
		 * Save a font definition without any transformations
		 * as well as a copy of the transforms done on it.
		 * This is already being done during the setFont()
         * below.
		 */
		setFont(g2.getFont());
	 }

	private interface CoordModifier
	 {
		public void modify(float[] coords, int count);
	 }

	private static Shape modify(Shape s, CoordModifier cm)
	 {
		GeneralPath gp = new GeneralPath();
		PathIterator iter = s.getPathIterator(null);
		float[] coords = new float[6];
		float moveX=0, moveY=0;
		float lastX=0, lastY=0;
		float fakeX=0, fakeY=0;
		float r;
		boolean closed = true;
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
		return  gp;
	 }

	private final CoordModifier cmModulo =
		new CoordModifier()
		 {
			public void modify(float[] coords, int count)
			 {
				for(int i=0; i<count; i+=2)
					coords[i] -= Math.floor(coords[i]/mod)*mod;
			 }
		 };

	private final CoordModifier cmWrapping =
		new CoordModifier()
		 {
			public void modify(float[] coords, int count)
			 {
				for(int i=0; i<count; i+=2)
					if(coords[i] < mod/2)
						coords[i] += mod;
			 }
		 };

	private Shape normalize(Shape s)
	 {
		double x = s.getBounds2D().getMinX();
		if(x < 0  ||  x >= mod)
			s = modify(s, cmModulo);

		if(s.getBounds2D().getWidth() >= mod/2)
			s = modify(s, cmWrapping);

		return  s;
	 }

    public void draw(Shape s)
     {
		s = normalize(s);

		int start = 0;
		if(s.getBounds2D().getMaxX() >= mod)
			start = -1;
		for(int i=start; i<count; i++)
		 {
			Shape s2 = AffineTransform
				.getTranslateInstance(base + mod*i, 0)
				.createTransformedShape(s);
			
			//TODO: a bug is submitted for this in bugzilla,
			// if java fixes this, the check may not be 
			//necessary anymore.
			
			//If zoomed in really high, one of the two
			// shapes returned will have an x value that
			// is ridiculously high, which will cause 
			// a segmentation fault and crash jmars.
			if(s2.getBounds().getX()>100000){
				continue;
			}
			g2.draw(s2);
		 }
     }

    public void fill(Shape s)
     {
		s = normalize(s);

		int start = 0;
		if(s.getBounds2D().getMaxX() >= mod)
			start = -1;
		for(int i=start; i<count; i++)
		 {
			Shape s2 = AffineTransform
				.getTranslateInstance(base + mod*i, 0)
				.createTransformedShape(s);
			g2.fill(s2);
		 }
     }

	@Override
	public void drawOval(int x, int y, int width, int height) {
		Shape oval = new Ellipse2D.Double(x, y, width, height);
		draw(oval);
	}
    
    
	@Override
	public void fillOval(int x, int y, int width, int height) {
		Shape oval = new Ellipse2D.Double(x, y, width, height);
		fill(oval);
	}

	public void transform(AffineTransform at)
	 {
		g2.transform(at);
	 }

    public boolean drawImage(Image img,
                             AffineTransform xform,
                             ImageObserver obs)
     {
		boolean loaded = true;
		for(int i=0; i<count; i++)
		 {
			Graphics2D g2 = (Graphics2D) this.g2.create();
			g2.translate(base + mod*i, 0);
			loaded = g2.drawImage(img, xform, obs) && loaded;
			g2.dispose();
		 }
		return  loaded;
     }

	public void drawImage(BufferedImage img,
						  BufferedImageOp op,
						  int x,
						  int y)
	 {
		g2.drawImage(img, op, x, y);
	 }

    public void clip(Shape s)
     {
//		log.aprintln(toString(s));
    	s = normalize(s);

		Area wrappedClip = new Area();

		for(int i=-1; i<count; i++)
		 {
			Shape s2 = AffineTransform
				.getTranslateInstance(base + mod*i, 0)
				.createTransformedShape(s);
			wrappedClip.add(new Area(s2));
		 }

//		log.aprintln("\t" + toString(wrappedClip));
		g2.clip(wrappedClip);
     }

	private static String toString(Shape a)
	 {
		String desc = a.toString() + " ---------\n";
		float[] coords = new float[6];
		for(PathIterator i=a.getPathIterator(null); !i.isDone(); i.next())
			switch(i.currentSegment(coords))
			 {
			 case PathIterator.SEG_QUADTO:
			 case PathIterator.SEG_CUBICTO:
				desc += "\tWOAH!\n";
				break;

			 case PathIterator.SEG_CLOSE:
				desc += "\tCLOSE\n";
				break;

			 case PathIterator.SEG_MOVETO:
				desc += "\t" + coords[0] + "\t" + coords[1] + "\tmove\n";
				break;

			 case PathIterator.SEG_LINETO:
				desc += "\t" + coords[0] + "\t" + coords[1] + "\n";
				break;

			 default:
				desc += "\tUNKNOWN\n";
				break;
			 }

		return  desc;
	 }

    public Graphics create()
     {
        GraphicsWrapped g2w = new GraphicsWrapped();
		g2w.g2 = (Graphics2D) g2.create();
		g2w.base = base;
		g2w.count = count;
		return  g2w;
     }

    public void translate(double tx, double ty)
     {
        g2.translate(tx, ty);
     }

    public void drawLine(int x1, int y1, int x2, int y2)
     {
		draw(new Line2D.Float(x1, y1, x2, y2));
     }

	public Font getFont()
	 {
		return  currentFont;
	 }

    public void setFont(Font font)
     {
		currentFont = font;
        g2.setFont(font.deriveFont(fontTransform));
        
        baseFont = FontUtil.getFontWithoutTransform(g2.getFont());
        baseFontTransform = g2.getFont().getTransform();
     }

    
    private void drawStringAsShape(String str, float x, float y){
    	FontUtil.drawStringAsShape(this, baseFont, baseFontTransform, str, x, y);
    }

    public void drawString(String str, int x, int y)
     {
		drawStringAsShape(str, x, y);
     }

    public void drawString(String s, float x, float y)
     {
		drawStringAsShape(s, x, y);
	 }

	public void drawStringWrapped(String s, float x, float y)
	 {
		int start = 0;
		if(g2.getFont().getStringBounds(s, g2.getFontRenderContext()).getMaxX()
		   >= mod)
			start = -1;
		for(int i=start; i<count; i++)
			drawStringAsShape(s, (float) (x + base + mod*i), y);
     }

	/**
	 ** Given a normalized world point, returns every occurrence of
	 ** that point in the visible window, in world coordinates.
	 **/
	public Point2D[] worldToWorlds(Point2D w)
	 {
		Point2D[] wps = new Point2D[count];
		for(int i=0; i<count; i++)
			wps[i] = new Point2D.Double(w.getX() + base + mod * i, w.getY());
		return  wps;
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

    public FontMetrics getFontMetrics(Font f)
     {
        return g2.getFontMetrics(f);
     }

    public FontRenderContext getFontRenderContext()
     {
        return g2.getFontRenderContext();
     }

    public Composite getComposite()
    {
    	return  g2.getComposite();
    }

    public void setComposite(Composite comp)
    {
    	g2.setComposite(comp);
    }

}
