package edu.asu.jmars.swing;

import edu.asu.jmars.util.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

public class ColorCombo
 extends JComboBox implements Comparable
 {
	private static final Color clear = new Color(0,0,0,0);
	private static final Color colorList[] =
	{
		//If you add one here, add it to the name HashMap too in the populateColorNames() private method (for tooltip)
		Util.darkBlue,
		Color.blue,
		Util.brightBlue,
		Color.cyan,
		Util.darkGreen,
		Color.green,
		Color.yellow,
		Color.orange,
		Color.red,
		Util.darkRed,
		Color.magenta,
		Util.purple,
		Color.pink,
		Util.darkBrown,

		Color.white,
		Color.lightGray,
		Color.gray,
		Color.darkGray,
		Color.black
	};
	private static HashMap<Color, String> colorNames = new HashMap<Color,String>();
	private static final Border clearBorder =
	BorderFactory.createLineBorder(clear, 2);

	private int alpha = 255;
	int width;
	String widthSpaces;

	/**
	 ** Constructs a ColorCombo with a default width. Same as calling
	 ** {@link #ColorCombo(int) ColorCombo(3)}.
	 **/
	public ColorCombo()
	 {
		this(3);
	 }

	/**
	 ** Constructs a ColorCombo with a given starting color value.
	 **/
	public ColorCombo(Color initial)
	 {
		this();
		setColor(initial);
	 }

	/**
	 ** Constructs a ColorCombo with a specific width. Note that the
	 ** width refers to the width of the colored area of the combobox,
	 ** and that <code>width=0</code> still results in some visible
	 ** width.
	 **
	 ** @param width The minimum "width" of the combo box, which is
	 ** equivalent to a JLabel (in its default font) with
	 ** <code>width</count> many space characters.
	 **/
	public ColorCombo(int width)
	 {
		this.width = width;
		populate();
	 }

	/**
	 ** Constructs a ColorCombo that can mirror another ColorCombo.
	 ** The width is picked up from the given combo. The new combo
	 ** will contain a selectable item that will put the combo into a
	 ** mode such that <code>this.getColor() =
	 ** other.getColor()</code>.
	 **/
	public ColorCombo(ColorCombo other)
	 {
		this.width = other.width;
		addItem(other);
		populate();
	 }

	private void populate()
	 {
		for(int i=0; i<colorList.length; i++)
			addItem(colorList[i]);
		widthSpaces = spaces(width);
		setMaximumRowCount(getItemCount());
		populateColorNames();
		setRenderer(new Renderer());
	 }

	private void populateColorNames() {
		colorNames.put(Util.darkBlue, "Dark Blue");
		colorNames.put(Color.blue, "Blue");
		colorNames.put(Util.brightBlue, "Bright Blue");
		colorNames.put(Color.cyan,"Cyan");
		colorNames.put(Util.darkGreen, "Dark Green");
		colorNames.put(Color.green, "Green");
		colorNames.put(Color.yellow, "Yellow");
		colorNames.put(Color.orange, "Orange");
		colorNames.put(Color.red, "Red");
		colorNames.put(Util.darkRed, "Dark Red");
		colorNames.put(Color.magenta, "Magenta");
		colorNames.put(Util.purple, "Purple");
		colorNames.put(Color.pink, "Pink");
		colorNames.put(Util.darkBrown, "Dark Brown");
		colorNames.put(Color.white, "White");
		colorNames.put(Color.lightGray, "Light Gray");
		colorNames.put(Color.gray,"Gray");
		colorNames.put(Color.darkGray,"Dark Gray");
		colorNames.put(Color.black, "Black");
	}
	public boolean isLinked()
	 {
		return  getSelectedItem() instanceof ColorCombo;
	 }
	
	// JNN: added:
	public static Color[] getColorList()
	{
		return colorList;
	}

	public Color getColor()
	 {
		Object item = getSelectedItem();

		if(item instanceof ColorCombo)
			return  ( (ColorCombo) item ).getColor();

		if(alpha == 255)
			return  (Color) item;

		return  Util.alpha((Color) item, alpha);
	 }

	public void setColor(Color c)
	 {
		alpha = c.getAlpha();
		if(c.getAlpha() != 255)
			c = new Color(c.getRGB()); // takes alpha out
		setSelectedItem(c);
	 }

	public void setAlpha(int alpha)
	 {
		this.alpha = alpha;
	 }

	public int getAlpha()
	 {
		return  alpha;
	 }

	private static String spaces(int n)
	 {
		String x = "";
		for(int i=0; i<n; i++)
			x += ' ';
		return  x;
	 }

	private static Map bordersByColor = new HashMap();
	private static Border getBorderForColor(Color c)
	 {
		Border b = (Border) bordersByColor.get(c);
		if(b == null)
		 {
			Color bc = Util.getB(c)<0.5 ? Color.white : Color.black;
			bordersByColor.put(c, b = BorderFactory.createLineBorder(bc, 2));
		 }
		return  b;
	 }

	// Used internally to render each colored cell in the combo box.
    private final class Renderer
	 extends JLabel
	 implements ListCellRenderer
	 {
		Renderer()
		 {
			setOpaque(true);
		 }

		// Need to be disabled if we want to render these things right.
		public void setBackground(Color col) { }
		public void setForeground(Color col) { }

        public Component getListCellRendererComponent(
			JList list,
			Object value,
			int index,
			boolean isSelected,
			boolean cellHasFocus)
		 {
			// A list item that mirrors another ColorCombo's value
			if(value instanceof ColorCombo)
			 {
				value = ( (ColorCombo) value ).getColor();
				setText("Linked");
			 }
			else
				setText(widthSpaces);

			setBorder(isSelected
					  ? getBorderForColor((Color)value)
					  : clearBorder);
			
			if (value instanceof Color) {
				setToolTipText(colorNames.get(value));
			}

			// Need the 'super' reference, to invoke the non-disabled version.
			super.setBackground( (Color) value );
//			super.setForeground(opaque);

			return this;
		 }

        @Override
    	protected void paintComponent(Graphics g) {
    		super.paintComponent(g);
    		g.setColor(getBackground());
    		g.fillRect(0, 0, getWidth(), getHeight());
    	}

		//////////////////////////////////////////////////////////////////////
		// The following are overridden for performance reasons,
		// copy+pasted from DefaultCellRenderer's source code.
		//////////////////////////////////////////////////////////////////////
		public void validate() {}
		public void revalidate() {}
		public void repaint(long tm, int x, int y, int width, int height) {}
		public void repaint(Rectangle r) {}
		public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}
		public void firePropertyChange(String propertyName, char oldValue, char newValue) {}
		public void firePropertyChange(String propertyName, short oldValue, short newValue) {}
		public void firePropertyChange(String propertyName, int oldValue, int newValue) {}
		public void firePropertyChange(String propertyName, long oldValue, long newValue) {}
		public void firePropertyChange(String propertyName, float oldValue, float newValue) {}
		public void firePropertyChange(String propertyName, double oldValue, double newValue) {}
		public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
		protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		   // Strings get interned...
		   if (propertyName=="text")
			   super.firePropertyChange(propertyName, oldValue, newValue);
		}

	 }

	public static void main(String[] av)
	 {
		JFrame f = new JFrame("Color Combo");
		f.getContentPane().add(
			new ColorCombo(new Color(255, 255, 0, 122))
			 {{
				addActionListener(
					new ActionListener()
					 {
						public void actionPerformed(ActionEvent e)
						 {
							System.out.println(getColor());
							System.out.println(getColor().getAlpha());
							System.out.println(getAlpha());
						 }
					 }
					);
			 }}
			);
		f.pack();
		f.setVisible(true);
	 }

	@Override
	public int compareTo(Object o) {
		if (o instanceof ColorCombo) {
			return getSelectedIndex() - ((ColorCombo)o).getSelectedIndex();
		}
		return 0;
	}
 }
