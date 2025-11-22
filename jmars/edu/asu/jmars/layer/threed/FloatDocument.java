package edu.asu.jmars.layer.threed;

import edu.asu.jmars.layer.*;
import edu.asu.jmars.util.*;
import javax.swing.text.*;
import java.awt.*;


/**
 ** A document class for accepting only numeric characters in a text box.
 **   usage:  textbox.setDocument( new FloatDocument());
 **
 ** @author: James Winburn MSFF/ASU   2/25/03
 **/
public class FloatDocument extends PlainDocument
{
    public void insertString(int offset, String s, AttributeSet attributeSet)
    {
    	//String s1; // <- JNN: I took this out, otherwise, all I did was modify the spacing
    	for (int i=0; i< s.length(); i++)
    	{
    		if (!s.valueOf(s.charAt(i)).equals("0") &&
    				!s.valueOf(s.charAt(i)).equals("1") &&
    				!s.valueOf(s.charAt(i)).equals("2") &&
    				!s.valueOf(s.charAt(i)).equals("3") &&
    				!s.valueOf(s.charAt(i)).equals("4") &&
    				!s.valueOf(s.charAt(i)).equals("5") &&
    				!s.valueOf(s.charAt(i)).equals("6") &&
    				!s.valueOf(s.charAt(i)).equals("7") &&
    				!s.valueOf(s.charAt(i)).equals("8") &&
    				!s.valueOf(s.charAt(i)).equals("9") &&
    				!s.valueOf(s.charAt(i)).equals("-") &&
    				!s.valueOf(s.charAt(i)).equals("E") &&
    				!s.valueOf(s.charAt(i)).equals("e") &&
    				!s.valueOf(s.charAt(i)).equals(".") 
    				)
    		{
    			Toolkit.getDefaultToolkit().beep();
    			return;
    		}
    	}
    	try
    	{
    		super.insertString( offset,s,attributeSet);
    	}
    	catch (BadLocationException ble)
    	{
    		return;
    	}
    }
}
