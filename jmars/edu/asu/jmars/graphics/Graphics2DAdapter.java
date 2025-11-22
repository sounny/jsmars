package edu.asu.jmars.graphics;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.RenderingHints.Key;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

import edu.asu.jmars.util.DebugLog;

/**
 ** Convenience class for implementing new {@link Graphics2D} wrapper
 ** classes. Every method of this class proxies to the underlying
 ** {@link #g2} member object and emits a large warning to the console
 ** that a potentially unsupported operation has been called. The idea
 ** is that wrapper classes will implement all functions they will
 ** use, so that the unused functions will not need declaring, but
 ** will not go unnoticed if they're called.
 **
 ** <p>The {@link #dispose} method is the one exception to the above.
 **/
public class Graphics2DAdapter
 extends Graphics2D
 {
    private static final DebugLog log = DebugLog.instance();

	/**
	 ** The internal object that all method calls are proxied to.
	 **/
    protected Graphics2D g2;

    /**
	 ** Invokes {@link #g2}.{@link Graphics2D#dispose dispose()}
	 ** without displaying anything on the console. Implemented as a
	 ** convenience, since a simple proxy is almost always the correct
	 ** implementation.
	 **/
    public void dispose()
     {
        g2.dispose();
     }

    public void setRenderingHint(Key hintKey, Object hintValue)
     {
        g2.setRenderingHint(hintKey, hintValue);
     }

    public Object getRenderingHint(Key hintKey)
     {
        return  g2.getRenderingHint(hintKey);
     }

    public void setRenderingHints(Map hints)
     {
        g2.setRenderingHints(hints);
     }

    public void addRenderingHints(Map hints)
     {
        g2.addRenderingHints(hints);
     }

    public RenderingHints getRenderingHints()
     {
        return  g2.getRenderingHints();
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void draw(Shape s)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.draw(s);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public boolean drawImage(Image img,
                             AffineTransform xform,
                             ImageObserver obs)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.drawImage(img,
                             xform,
                             obs);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void drawImage(BufferedImage img,
                          BufferedImageOp op,
                          int x,
                          int y)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.drawImage(img,
                     op,
                     x,
                     y);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void drawRenderedImage(RenderedImage img,
                                  AffineTransform xform)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.drawRenderedImage(img,
                             xform);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void drawRenderableImage(RenderableImage img,
                                    AffineTransform xform)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.drawRenderableImage(img,
                               xform);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void drawString(String str, int x, int y)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.drawString(str, x, y);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void drawString(String s, float x, float y)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.drawString(s, x, y);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void drawString(AttributedCharacterIterator iterator,
                           int x, int y)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.drawString(iterator,
                      x, y);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void drawString(AttributedCharacterIterator iterator,
                           float x, float y)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.drawString(iterator,
                      x, y);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void drawGlyphVector(GlyphVector g, float x, float y)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.drawGlyphVector(g, x, y);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void fill(Shape s)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.fill(s);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public boolean hit(Rectangle rect,
                       Shape s,
                       boolean onStroke)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.hit(rect,
                       s,
                       onStroke);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public GraphicsConfiguration getDeviceConfiguration()
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.getDeviceConfiguration();
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void setComposite(Composite comp)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.setComposite(comp);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void setPaint(Paint paint)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.setPaint(paint);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void setStroke(Stroke s)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.setStroke(s);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void translate(int x, int y)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.translate(x, y);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void translate(double tx, double ty)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.translate(tx, ty);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void rotate(double theta)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.rotate(theta);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void rotate(double theta, double x, double y)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.rotate(theta, x, y);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void scale(double sx, double sy)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.scale(sx, sy);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void shear(double shx, double shy)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.shear(shx, shy);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void transform(AffineTransform Tx)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.transform(Tx);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void setTransform(AffineTransform Tx)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.setTransform(Tx);
     }

    public AffineTransform getTransform()
     {
        return g2.getTransform();
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public Paint getPaint()
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.getPaint();
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public Composite getComposite()
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.getComposite();
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void setBackground(Color color)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.setBackground(color);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public Color getBackground()
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.getBackground();
     }

    public Stroke getStroke()
     {
        return  g2.getStroke();
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void clip(Shape s)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.clip(s);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public FontRenderContext getFontRenderContext()
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.getFontRenderContext();
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public Graphics create()
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.create();
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public Color getColor()
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.getColor();
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void setColor(Color c)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.setColor(c);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void setPaintMode()
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.setPaintMode();
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void setXORMode(Color c1)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.setXORMode(c1);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public Font getFont()
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.getFont();
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void setFont(Font font)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.setFont(font);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public FontMetrics getFontMetrics(Font f)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.getFontMetrics(f);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public Rectangle getClipBounds()
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.getClipBounds();
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void clipRect(int x, int y, int width, int height)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.clipRect(x, y, width, height);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void setClip(int x, int y, int width, int height)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.setClip(x, y, width, height);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public Shape getClip()
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.getClip();
     }

    public void setClip(Shape clip)
     {
         g2.setClip(clip);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void copyArea(int x, int y, int width, int height,
                         int dx, int dy)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.copyArea(x, y, width, height,
                    dx, dy);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void drawLine(int x1, int y1, int x2, int y2)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.drawLine(x1, y1, x2, y2);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void fillRect(int x, int y, int width, int height)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.fillRect(x, y, width, height);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void clearRect(int x, int y, int width, int height)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.clearRect(x, y, width, height);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void drawRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.drawRoundRect(x, y, width, height,
                         arcWidth, arcHeight);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void fillRoundRect(int x, int y, int width, int height,
                              int arcWidth, int arcHeight)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.fillRoundRect(x, y, width, height,
                         arcWidth, arcHeight);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void drawOval(int x, int y, int width, int height)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.drawOval(x, y, width, height);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void fillOval(int x, int y, int width, int height)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.fillOval(x, y, width, height);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void drawArc(int x, int y, int width, int height,
                        int startAngle, int arcAngle)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.drawArc(x, y, width, height,
                   startAngle, arcAngle);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void fillArc(int x, int y, int width, int height,
                        int startAngle, int arcAngle)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.fillArc(x, y, width, height,
                   startAngle, arcAngle);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void drawPolyline(int[] xPoints, int[] yPoints,
                             int nPoints)
     {
    	/*
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        */
        g2.drawPolyline(xPoints, yPoints,
                        nPoints);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void drawPolygon(int[] xPoints, int[] yPoints,
                            int nPoints)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.drawPolygon(xPoints, yPoints,
                       nPoints);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public void fillPolygon(int[] xPoints, int[] yPoints,
                            int nPoints)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        g2.fillPolygon(xPoints, yPoints,
                       nPoints);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public boolean drawImage(Image img, int x, int y, ImageObserver observer)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.drawImage(img, x, y, observer);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public boolean drawImage(Image img, int x, int y,
                             int width, int height, ImageObserver observer)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.drawImage(img, x, y,
                             width, height, observer);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public boolean drawImage(Image img, int x, int y, Color bgcolor,
                             ImageObserver observer)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.drawImage(img, x, y, bgcolor,
                             observer);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public boolean drawImage(Image img, int x, int y,
                             int width, int height, Color bgcolor,
                             ImageObserver observer)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.drawImage(img, x, y,
                             width, height, bgcolor,
                             observer);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public boolean drawImage(Image img,
                             int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             ImageObserver observer)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.drawImage(img,
                             dx1, dy1, dx2, dy2,
                             sx1, sy1, sx2, sy2,
                             observer);
     }

    /** DO NOT CALL, NOT PROPERLY IMPLEMENTED **/
    public boolean drawImage(Image img,
                             int dx1, int dy1, int dx2, int dy2,
                             int sx1, int sy1, int sx2, int sy2,
                             Color bgcolor,
                             ImageObserver observer)
     {
        log.aprintln("POSSIBLY UNSUPPORTED GRAPHICS OPERATION CALLED " +
					 "(" + getClass().getName() + ")");
        log.aprintStack(5);
        return  g2.drawImage(img,
                             dx1, dy1, dx2, dy2,
                             sx1, sy1, sx2, sy2,
                             bgcolor,
                             observer);
     }

 }
