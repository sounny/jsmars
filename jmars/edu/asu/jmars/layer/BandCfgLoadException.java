package edu.asu.jmars.layer;

import edu.asu.jmars.*;
import edu.asu.jmars.util.*;
// File: BandCfgLoadException.java
//
// ROILayer throws this exception if a band-configuration cannot
// be loaded due to some reason.
//

import java.util.*;

public class BandCfgLoadException extends Exception {
   public BandCfgLoadException(String _message){
		super(_message);
	}
}
