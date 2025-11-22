package edu.asu.jmars.swing;

import java.util.ArrayList;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
* The purpose of this filter is to only allow integers to be
* entered (0-9), and to limit the number of digits.
* Ex. passing in 3 will limit the input to 0-999
*/
public class DocumentIntFilter extends DocumentFilter{
	private int digits;
	private ArrayList<Character> ints;
	
	/**
	 * Creates a filter which only allows the input of the numbers 0-9,
	 * with a limiting digit number of the input passed.  If numOfDigits 
	 * is 0, then there is no digit limit.
	 * Ex. if 3 is passed then the filter would allow values 0-999
	 * @param numOfDigits  Limit the number of digits that can be entered.
	 * If 0 is passed, there is no limit to the number of digits.
	 */
	public DocumentIntFilter(int numOfDigits){
		digits = numOfDigits;
		//create the character list of integers
		ints = new ArrayList<Character>();
		ints.add('1');
		ints.add('2');
		ints.add('3');
		ints.add('4');
		ints.add('5');
		ints.add('6');
		ints.add('7');
		ints.add('8');
		ints.add('9');
		ints.add('0');
	}
	
	@Override
    public void insertString(FilterBypass fb, int offs,
                             String str, AttributeSet a) throws BadLocationException {
	   

    	super.insertString(fb, offs, filterInput(fb, str), a);
    	
    }

    @Override
    public void replace(FilterBypass fb, int offs,
                        int length, 
                        String str, AttributeSet a)
        throws BadLocationException {

    	
    	super.replace(fb, offs, length, filterInput(fb, str), a);
    }

    private String filterInput(FilterBypass fb, String str){
		//filter input to only integer values------------------------	
		   //convert the string into a list of characters
	    	ArrayList<Character> charList = getCharacterList(str);
	    	
	    	//create an output string
	    	StringBuffer output = new StringBuffer();
	    	
	    	//cycle through the list of characters on the string
	    	for(char c : charList){
	    		//if it's in the good char list, add it to the string
	    		if(ints.contains(c)){
	    			output.append(c);
	    		}
	    	}
	    	
	    //filter input to maximum length-------------------------------
	    	//if digits is less than 1, there is no length
	    	String text = output.toString();
	    	if(digits>0){
	    		//check how many digits have already been entered to see
	    		// if more can be allowed
	    		int allowableCharNum = digits - fb.getDocument().getLength();
	    		//if input is longer than remain digits, limit it
	    		if(allowableCharNum < text.length()){
		    		text = text.substring(0, allowableCharNum);
	    		}
	    	}
	    	
	    	return text;
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
