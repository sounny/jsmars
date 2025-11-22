package edu.asu.jmars.layer.groundtrack;

import edu.asu.jmars.*;
import edu.asu.jmars.layer.*;
import edu.asu.jmars.util.*;
import java.awt.*;

class GroundLayerSettings extends LViewSettings
 {
	long begET;
	long endET;
	int delta;

	Color begColor;
	Color endColor;

	GroundLayerSettings(long bET, long eET, int d, Color bc, Color ec ) {

	   begET = bET;
	   endET = eET;
	   delta = d;

	   begColor = bc;
	   endColor = ec;
	}
 }

