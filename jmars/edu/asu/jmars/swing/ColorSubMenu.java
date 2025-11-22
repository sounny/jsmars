package edu.asu.jmars.swing;

import edu.asu.jmars.*;
import edu.asu.jmars.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

public abstract class ColorSubMenu extends JMenu
 {
	private static final Color colorList[] =
	{
		Color.blue,
		Color.cyan,
		Util.darkGreen,
		Color.green,
		Color.yellow,
		Color.orange,
		Color.red,
		Color.magenta,
		Color.pink,

		Color.white,
		Color.lightGray,
		Color.gray,
		Color.darkGray,
		Color.black
	};

	protected Component colorChooserParent = null;
	protected String colorChooserTitle = null;
	protected Color colorChooserDefault = null;

	public ColorSubMenu(String title)
	 {
		super(title);

		prependMenuItems();
		add(new CustomColorMenuItem("Custom color..."));
		add(new JSeparator());
		for(int i=0; i<colorList.length; i++)
			add(new ColorMenuItem(colorList[i]));
	 }

	protected void prependMenuItems()
	 {
	 }

	protected void colorChosen(Color newColor)
	 {
	 }

	private class CustomColorMenuItem extends AbstractAction
	 {
		CustomColorMenuItem(String title)
		 {
			super(title);
		 }

		public void actionPerformed(ActionEvent e)
		 {
			Color newColor =
				JColorChooser.showDialog(colorChooserParent,
										 colorChooserTitle,
										 colorChooserDefault);
			if(newColor != null)
				colorChosen(newColor);
		 }
	 };

	private class ColorMenuItem extends JMenuItem implements ActionListener
	 {
		Border hilight;
		Color col;
		ColorMenuItem(Color col)
		 {
			super(" ");
			this.col = col;
			if(Util.getB(col) < 0.5)
				hilight = BorderFactory.createLineBorder(Color.white, 2);
			else
				hilight = BorderFactory.createLineBorder(Color.black, 2);

			addActionListener(this);
		 }

		public void actionPerformed(ActionEvent e)
		 {
			colorChosen(col);
		 }

		protected void paintBorder(Graphics g)
		 {
			if(isArmed())
				hilight.paintBorder(this, g, 0, 0,
									getWidth(), getHeight());
		 }

		protected void paintComponent(Graphics g)
		 {
			g.setColor(col);
			g.fillRect(0, 0, getWidth(), getHeight());
		 }
	 }
 }
