package edu.asu.jmars.swing;

import java.util.ArrayList;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * The purpose of this filter is to either only allow the characters
 * passed in, or to disallow the characters passed in on the document
 * this is set on.
 */

public class DocumentCharFilter extends DocumentFilter {
    private ArrayList<Character> chars;
    private boolean allow;
 
    /**
     * Create a filter for the text document that either only allows
     * the characters passed in, or explicitly does not allow the
     * characters passed in.
     * @param chars  The specific characters
     * @param allow	If true, only allow the characters from above, if 
     * false, disallow the characters from above.
     */
    public DocumentCharFilter(ArrayList<Character> chars, boolean allow) {
        this.chars = chars;
        this.allow = allow;
    }
 
    @Override
    public void insertString(FilterBypass fb, int offs,
                             String str, AttributeSet a)
        throws BadLocationException {
 
        //if allow, then only allow the characters that were passed in.
    	if(allow){
    		super.insertString(fb, offs, filterGoodChars(str), a);
    	}
    	//if not allow then do not insert any of the characters that were passed in
    	else{
    		super.insertString(fb, offs, filterBadChars(str), a);
    	}
    	
    }

    @Override
    public void replace(FilterBypass fb, int offs,
                        int length, 
                        String str, AttributeSet a)
        throws BadLocationException {

    	
    	if(allow){
    		super.replace(fb, offs, length, filterGoodChars(str), a);
    	}
    	else{
    		super.replace(fb, offs, length, filterBadChars(str), a);
    	}
    }
    
    private String filterBadChars(String str){
    	//convert the string into a list of characters
    	ArrayList<Character> charList = getCharacterList(str);
    	
    	//create an output string
    	StringBuffer output = new StringBuffer();
    	
    	//cycle through the list of characters on the string
    	for(char c : charList){
    		//if it is not one of the bad characters, add it to the output string
    		if(!chars.contains(c)){
    			output.append(c);
    		}
    	}
    	
		//construct a string with the remaining characters
		return output.toString();
    }
    
    private String filterGoodChars(String str){
    	//convert the string into a list of characters
    	ArrayList<Character> charList = getCharacterList(str);
    	
    	//create an output string
    	StringBuffer output = new StringBuffer();
    	
    	//cycle through the list of characters on the string
    	for(char c : charList){
    		//if it's in the good char list, add it to the string
    		if(chars.contains(c)){
    			output.append(c);
    		}
    	}
    	
    	//return the string
    	return output.toString();
    }
    
    private ArrayList<Character> getCharacterList(String str){
    	char[] strChars = new char[str.length()];
		str.getChars(0, str.length(), strChars, 0);
		ArrayList<Character> charList = new ArrayList<Character>();
		for(char c : strChars){
			charList.add(c);
		}
		return charList;
    }
 
}