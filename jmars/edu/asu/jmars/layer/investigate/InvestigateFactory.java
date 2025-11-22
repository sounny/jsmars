package edu.asu.jmars.layer.investigate;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.investigate.InvestigateLView.InvestigateParms;

public class InvestigateFactory extends LViewFactory{

	static boolean lviewExists = false;
	
	public Layer.LView createLView(){
		return null;
	}
	
	public void createLView(boolean async, LayerParameters l) {
		setLviewExists(true);
		InvestigateLayer layer = new InvestigateLayer();
		InvestigateLView3D lview3d = new InvestigateLView3D(layer);
		InvestigateLView view = new InvestigateLView(layer, true, lview3d);
		view.originatingFactory = this;
		LManager.receiveNewLView(view);
	}
	
	
	public Layer.LView recreateLView(SerializedParameters parmBlock){
		InvestigateLView view;
		if(parmBlock instanceof InvestigateParms){
			InvestigateParms ip = (InvestigateParms) parmBlock;
			InvestigateLayer il = new InvestigateLayer(ip.dataSpikes, ip.dataProfiles);
			InvestigateLView3D lview3d = new InvestigateLView3D(il);
			view = new InvestigateLView(il,true,lview3d);
		}else{
			InvestigateLayer il = new InvestigateLayer();
			InvestigateLView3D lview3d = new InvestigateLView3D(il);
			view = new InvestigateLView(new InvestigateLayer(), true, lview3d);
		}
		setLviewExists(true);
		view.originatingFactory = this;
		return view;
	}
	
	
	public static void setLviewExists(boolean b){
		lviewExists = b;
	}
	public static boolean getLviewExists(){
		return lviewExists;
	}
	
	public String getName(){
		return "Investigate Layer";
	}
}
