package edu.asu.jmars.swing;

import java.awt.event.*;
import javax.swing.*;

/**
 ** A {@link JCheckBox} that is its own {@link ActionListener}.
 **/
public abstract class ActionCheckBox
 extends JCheckBox
 implements ActionListener
 {
    /**
     * Creates an initially unselected check box button with no text, no icon.
     */
    public ActionCheckBox()
	 {
        this(null, null, false);
	 }

    /**
     * Creates an initially unselected check box with an icon.
     *
     * @param icon  the Icon image to display
     */
    public ActionCheckBox(Icon icon)
	 {
        this(null, icon, false);
	 }
    
    /**
     * Creates a check box with an icon and specifies whether
     * or not it is initially selected.
     *
     * @param icon  the Icon image to display
     * @param selected a boolean value indicating the initial selection
     *        state. If <code>true</code> the check box is selected
     */
    public ActionCheckBox(Icon icon, boolean selected)
	 {
        this(null, icon, selected);
	 }
    
    /**
     * Creates an initially unselected check box with text.
     *
     * @param text the text of the check box.
     */
    public ActionCheckBox(String text)
	 {
        this(text, null, false);
	 }

    /**
     * Creates a check box where properties are taken from the 
     * Action supplied.
     *
     * @since 1.3
     */
    public ActionCheckBox(Action a)
	 {
        this();
		setAction(a);
	 }


    /**
     * Creates a check box with text and specifies whether 
     * or not it is initially selected.
     *
     * @param text the text of the check box.
     * @param selected a boolean value indicating the initial selection
     *        state. If <code>true</code> the check box is selected
     */
    public ActionCheckBox(String text, boolean selected)
	 {
        this(text, null, selected);
	 }

    /**
     * Creates an initially unselected check box with 
     * the specified text and icon.
     *
     * @param text the text of the check box.
     * @param icon  the Icon image to display
     */
    public ActionCheckBox(String text, Icon icon)
	 {
        this(text, icon, false);
	 }

    /**
     * Creates a check box with text and icon,
     * and specifies whether or not it is initially selected.
     *
     * @param text the text of the check box.
     * @param icon  the Icon image to display
     * @param selected a boolean value indicating the initial selection
     *        state. If <code>true</code> the check box is selected
     */
    public ActionCheckBox(String text, Icon icon, boolean selected)
	 {
        super(text, icon, selected);
		addActionListener(this);
	 }

	public abstract void actionPerformed(ActionEvent e);
 }
