package edu.asu.jmars.graphics;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;

import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.Border;
/**
 * @author npiace
 * @author gdudhbha
 * This is a swing component that allows a label text to be rendered as per the selected font. 
 * The size of the label is updated as per the dimensions of label text.Resizing of Label does not happen on its own as we are overriding 
 * most of its methods and the application has to tell the label to resize.
 * This is used by the JFontChooser,ScaleLView and ShapeRenderer
 * 
 */
public class FontRenderer extends JLabel {
	private static final long serialVersionUID = 4951467629049657482L;
	private String labelText = null;
	private Color outlineColor;
	private Color fontColor;
	private boolean antialias = true;
	
	public FontRenderer() {
		this(UIManager.getDefaults().getFont("MenuBar.font"), Color.black, Color.white);
	}
	
	public FontRenderer(Font font, Color outline, Color textColor) {
		super();
	    setFont(font);
	    setIgnoreRepaint(true);
		setDoubleBuffered(false);
		outlineColor = outline;
		setForeground(textColor);
    	antialias = false;
	}
	
	public void setBorder(Border b) {
		super.setBorder(b);
		updateSize();
	}
	
	public void updateSize() {
		Font font = getFont();
		String label = getLabel();
		
		if (font == null || label == null)
			return;
		
		FontMetrics theseFontMetrics = getFontMetrics(font);
		if (theseFontMetrics == null)
			return;
		
		Insets in = getInsets();
		int width = in.left + in.right + theseFontMetrics.stringWidth(getLabel());
		int height = in.top + in.bottom + theseFontMetrics.getAscent() + theseFontMetrics.getDescent();
		Dimension textDimensions = new Dimension(width, height);
		setPreferredSize(textDimensions);
		setMinimumSize(textDimensions);
		revalidate();
		validate();
		repaint();
	}


	/**
	 * @param aa
	 */
	public void setAntiAlias(boolean aa) {
		antialias = aa;
		repaint();
	}

	/**
	 * @param color
	 */
	public void setOutlineColor(Color color) {
		outlineColor = color;
		repaint();
	}
	
	/**
	 * @param color First sets the local variable fontColor,
	 * then calls super.setForeground(color).
	 * Does this so that text can be displayed with an empty
	 * fill color if null is passed in when called.
	 */
	@Override
	public void setForeground(Color color){
		fontColor = color;
		super.setForeground(color);
	}
	
	/*
	 * On increasing font size the label size also has to be increased and thus updateSize is called.
	 * The text will be properly aligned with the label.
	 *
	 */
	@Override
	public void setFont(Font font){
		if (font == null)
			return;
		super.setFont(font);
		updateSize();
	}

	public void setLabel(String string) {
		labelText = string;
		updateSize();
	}
	
	public String getLabel(){
		return labelText;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 * Used when FontRenderer is used like a swing component
	 */
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g.create();
		
		//Change the background the text is displayed on so that
		// it will always be visible.  Black background for light
		// text, and white background for dark text.
		Color c;
		if(outlineColor!=null){
			c = outlineColor;
		}else{
			c = getForeground();
		}
		if(c.getRed()+c.getGreen()+c.getBlue() > (128*3)){
			g2.setColor(Color.BLACK);
			g2.setBackground(Color.BLACK);
		}else{
			g2.setColor(Color.WHITE);
			g2.setBackground(Color.WHITE);
		}
		g2.fillRect(0, 0, getWidth(), getHeight());
		
		
		try {
			FontMetrics fm = getFontMetrics(getFont());
			paintLabel(g2, getLabel(), getInsets().left, getInsets().top + fm.getHeight());
		} finally {
			if (g2 != null) {
				g2.dispose();
			}
		}
	}
	
	/**
	 * Setting text here causes Jlabel to render the text along with rendering done by paintlabel().
	 * Due to dual rendering text appears as a shadow of the other.
	 */
	@Override
	public void setText(String text){
	}
	
	/**
	 * Draws the given label onto the g2, assuming the heavy lifting on the Graphics2D is done elsewhere for us, so we don't dispose of it.
	 */
	public void paintLabel(Graphics2D g2, String label, float x, float y) {
	    Font font = getFont();
	    
	    g2.setFont(font);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
		if (outlineColor != null && fontColor != null) {// draw the outline
			// draw outline
			AffineTransform at = g2.getTransform();
			float pxWidth = 1f/(float)at.getScaleX();
			float pxHeight = 1f/(float)at.getScaleY();
			g2.setColor(outlineColor);
			g2.drawString(label, x - pxWidth, y - pxHeight);
			g2.drawString(label, x - pxWidth, y + pxHeight);
			g2.drawString(label, x + pxWidth, y - pxHeight);
			g2.drawString(label, x + pxWidth, y + pxHeight);
			// draw interior
			g2.setColor(fontColor);
			g2.drawString(label, x, y);
		} else if (fontColor != null && outlineColor == null) {// fill the outline
			g2.setColor(fontColor);
			g2.drawString(label, x, y);//just the string
		} else if (fontColor == null && outlineColor != null) {
			// Only Draw Outline				
			//this code is roughly 10% as efficient as above, but it is the only way to give the user the option of having an outline
			TextLayout textTl = new TextLayout(label,font, g2.getFontRenderContext());
		    Shape outline = textTl.getOutline(null);
			g2.setColor(outlineColor);
			g2.translate(x, y);
			g2.draw(outline);
		}
	}
}
