package edu.asu.jmars;

import edu.asu.jmars.swing.*;
import edu.asu.jmars.util.*;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JTextField;
import java.beans.*; //Property change stuff
import java.awt.*;
import java.awt.event.*;

public class ImageSizeDialog extends JDialog
 {
    public ImageSizeDialog(Frame parent, final Dimension d)
	 {
        super(parent, true);
        setTitle("Set the bitmap size");

		final JTextField txtWidth = new PasteField(8);
		final JTextField txtHeight = new PasteField(8);
        final Object[] inputs = { "Pixel Width", txtWidth,
								  "Pixel Height", txtHeight };
        final Object[] buttons = { "Generate", "Cancel" };

        final JOptionPane optionPane =
			new JOptionPane(inputs,
							JOptionPane.QUESTION_MESSAGE,
							JOptionPane.DEFAULT_OPTION,
							null,
							buttons,
							buttons[0]);

		optionPane.addPropertyChangeListener(
			new PropertyChangeListener()
			 {
				public void propertyChange(PropertyChangeEvent e)
				 {
					String prop = e.getPropertyName();

					if(isVisible()
					   && (e.getSource() == optionPane)
					   && (prop.equals(JOptionPane.VALUE_PROPERTY) ||
						   prop.equals(JOptionPane.INPUT_VALUE_PROPERTY)))
					 {
						if(optionPane.getValue().equals(buttons[0]))
						 {
							d.width = getInt(txtWidth);
							d.height = getInt(txtHeight);
						 }
						else
							d.width = d.height = 0;

						setVisible(false);
					 }
				 }
             }
			);

		setContentPane(optionPane);
        pack();
		setLocationRelativeTo(parent);

                txtWidth.setText(String.valueOf(d.width));
                txtHeight.setText(String.valueOf(d.height));


	 }

	private static int getInt(JTextField f)
	 {
		try
		 {
			return  Integer.parseInt(f.getText());
		 }
		catch(NumberFormatException e)
		 {
			return  0;
		 }
	 }
 }
