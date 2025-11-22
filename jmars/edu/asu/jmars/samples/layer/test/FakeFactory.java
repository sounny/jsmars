package edu.asu.jmars.samples.layer.test;

import edu.asu.jmars.layer.*;
import edu.asu.jmars.util.*;
public class FakeFactory extends LViewFactory
 {
	public Layer.LView createLView()
	 {
		Layer.LView view = new FakeLView();
                view.originatingFactory = this;
		return  view;
	 }

      //used to restore a view from a save state
        public Layer.LView recreateLView(SerializedParameters parmBlock)
        {
               return createLView();
        }

	// Supply the proper name and description.
	public String getName()
	 {
		return  "Fake";
	 }

	public String getDesc()
	 {
		return  "Fake";
	 }

 }
