package edu.asu.jmars.swing;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.util.Config;


public class Hidden3DToggle extends HiddenToggleButton {
	
	private LView3D view3D;	
	private static Map<LView, ButtonState> viewAndState3D = new IdentityHashMap<>();
	private static Set<Hidden3DToggle> toggles3D = new HashSet<>(); //this set holds all instances of 3D toggles, while each toggle holds view assigned to it 
	

	public Hidden3DToggle(LView view, ImageCatalogItem threedEnabledImg, ImageCatalogItem threedDisabledImg) {
		super(view, threedEnabledImg, threedDisabledImg);
		view3D = view.getLView3D();	
		if(!view3D.exists()){			
			setState(ButtonState.DISABLED);
		}	
		 toggles3D.add(this);
	}

	public LView3D getView3D() {	
		return view3D;
	}
	
    @Override
	public void initState() {
		LView3D view3D;	
		if (viewAndState3D.containsKey(this.rowview.get())) {
			ButtonState st = viewAndState3D.get(this.rowview.get());
			setState(st);
			return;
		}
		if (this.rowview.isPresent())
		{
			view3D = this.getView().get().getLView3D();
			if(!view3D.exists())
			{		   
			    setState(ButtonState.DISABLED);
			    return;
			}
			
			LView view = this.getView().get();
			if (view.isOverlay()) {
				String initVal = Config.get(view.getOverlayId() + "_3d",null);
				if (initVal != null) {
					if("on".equalsIgnoreCase(initVal)) {
						state = ButtonState.ON;
						view.getLView3D().setVisible(true);
					} else {
						state = ButtonState.OFF;
						view.getLView3D().setVisible(false);
					}
				} else {
					state = ButtonState.OFF;
					view.getLView3D().setVisible(false);
				}
			} else {
				state = ButtonState.OFF; //off by default from meeting on 04/27/2020
				view.getLView3D().setVisible(false);
			}
			setState(state);						
		}
	}
    
    
    @Override
    public void setState(ButtonState newstate) {
    	super.setState(newstate);
    	viewAndState3D.put(this.rowview.get(), newstate);		
    }
    

	@Override
	public void toggle()
	{
		LView3D view;
		if (state == ButtonState.DISABLED) return;
		if (!isEnabled()) return;
		if (this.rowview.isPresent())
		{
			toggleState();
			view = this.getView().get().getLView3D();			
			String configState = null;
			if (getState() == ButtonState.ON) {
				view.setVisible(true);
				configState = "on";
			} else if (getState() == ButtonState.OFF) {
				view.setVisible(false);
				configState = "off";
			}
			
			if (this.getView().get().isOverlay()) {
				String key = this.getView().get().getOverlayId() + "_3d";
				Config.set(key,configState);
			}
		}
		sync();
	}
	
	
	@Override
	public void toggleState() {		
		if (state == ButtonState.DISABLED) return;
		ButtonState newstate = state.toggleState(); 
		setState(newstate);		
	}
	
	@Override
	public void sync() {
		if (viewAndState3D.containsKey(this.rowview.get())) {
			LView vue = this.rowview.get();
			ButtonState newstate = viewAndState3D.get(vue);
			// iterate through set toggles
			Iterator<Hidden3DToggle> it = toggles3D.iterator();
			while (it.hasNext()) {
				Hidden3DToggle toggl = it.next();
				if (toggl.getView().get() == vue) {
					toggl.setState(newstate);
				}
			}
		}
	}	

}
