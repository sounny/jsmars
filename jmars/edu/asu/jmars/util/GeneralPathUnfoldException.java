package edu.asu.jmars.util;

// File: GeneralPathUnfoldException.java
//
// Implements GeneralPath -> Point2D[] tranformation exception
// for some unsupported form of GeneralPath (as declared by
// the implementing method).
//

public class GeneralPathUnfoldException extends Exception {
	public GeneralPathUnfoldException(String _message,
									  int _unsupportedComponentIndex){
		super(_message);
		unsupportedComponentIndex = _unsupportedComponentIndex;
	}

	public int getUnsupportedComponentIndex(){
		return unsupportedComponentIndex;
	}

	private int unsupportedComponentIndex;
}
