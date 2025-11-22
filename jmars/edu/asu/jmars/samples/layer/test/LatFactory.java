package edu.asu.jmars.samples.layer.test;

import edu.asu.jmars.layer.*;
import edu.asu.jmars.util.*;
public class LatFactory extends LViewFactory
 {
	public Layer.LView createLView()
	 {
		return  new LatLView(new LatLayer());
	 }

	public Layer.LView recreateLView(SerializedParameters parmBlock)
	 {
		return  null;
	 }
 }
