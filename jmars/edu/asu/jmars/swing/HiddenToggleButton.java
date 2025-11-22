package edu.asu.jmars.swing;

import java.awt.Color;
import java.awt.Insets;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeButton;
import mdlaf.components.button.MaterialButtonUI;
import mdlaf.utils.MaterialColors;


public class HiddenToggleButton extends JButton {

	static Color imgON = ((ThemeImages) GUITheme.get("images")).getFill2();
	static Color strokeON = ((ThemeImages) GUITheme.get("images")).getFill2();	
	static Color imgOFF = ((ThemeButton) GUITheme.get("button")).getDisabledback();
	static Color strokeOFF = MaterialColors.COSMO_DARK_GRAY;	
	static Color imgDisabled = ((ThemeButton) GUITheme.get("button")).getDisabledback();
	static Color strokeDisabled = ((ThemeButton) GUITheme.get("button")).getDisabledback();
	static Color normalBackground = ((ThemePanel)GUITheme.get("panel")).getBackground();	

	protected Optional<Layer.LView> rowview = Optional.empty();
	protected Optional<Layer.LView> parentview = Optional.empty();
	protected ImageCatalogItem buttonImg;
	private ImageCatalogItem buttonDisabledImg;
	protected ButtonState state = ButtonState.OFF;	
	
	private static Map<LView, ButtonState> viewAndState = new IdentityHashMap<>();
	private static Set<HiddenToggleButton> toggles = new HashSet<>(); //this set holds all instances of toggles, while each toggle holds view assigned to it 
	
	public HiddenToggleButton(LView view, ImageCatalogItem enabledImg, ImageCatalogItem disabledImg) {
		super();
		this.rowview = Optional.ofNullable(view);
		this.parentview = Optional.ofNullable(view);
		this.buttonImg = enabledImg;
		this.buttonDisabledImg = disabledImg; 
		setUI(new HiddenToggleButtonUI());
		this.setFocusable(false);
		this.setMargin(new Insets(0,0,0,0));
		this.setBorder(null);
		if (! (this instanceof Hidden3DToggle)) {
		    toggles.add(this);
		}
	}	

	public HiddenToggleButton(LView parentview, LView child, ImageCatalogItem enabledImg, ImageCatalogItem disabledImg) {
		super();
		this.rowview = Optional.ofNullable(child);
		this.parentview = Optional.ofNullable(parentview);		
		this.buttonImg = enabledImg;
		this.buttonDisabledImg = disabledImg; 
		setUI(new HiddenToggleButtonUI());
		this.setFocusable(false);
        this.setMargin(new Insets(0,0,0,0));
        this.setBorder(null);
        if (! (this instanceof Hidden3DToggle)) {
		    toggles.add(this);
		}    
	}	

	public Optional<Layer.LView> getView() {
		return rowview;
	}
	
	public ImageCatalogItem getButtonImg() {
		return buttonImg;
	}	
	
	public ButtonState getState() {
		return state;
	}
	
	public void initState() {
		LView view;			
		if (this.rowview.isPresent())
		{
			view = this.getView().get();
			if (view.isOverlay()) {
				String initVal = null;
				if (view.viewman.getChild() == null) {//panner
					initVal = Config.get(view.getOverlayId()+"_panner",null);	
				} else {
					initVal = Config.get(view.getOverlayId()+"_main",null);
				}
				
				if (initVal != null) {
					if("on".equalsIgnoreCase(initVal)) {
						state = ButtonState.ON;
					} else {
						state = ButtonState.OFF;
					}
				} else {
					if (view.isVisible()) {
						state = ButtonState.ON;		
					} else {
						state = ButtonState.OFF;	        
					}
				}
			} else {
				if (view.isVisible()) {
					state = ButtonState.ON;		
				} else {
					state = ButtonState.OFF;	        
				}
			}
			setState(state);			  
	    }	
	}		

	public void setState(ButtonState newstate) {
		this.state = newstate;
		state.setStroke(newstate.getStroke());
		state.setFill(newstate.getFill());
		ImageIcon stateIcon = this.state == ButtonState.DISABLED ? 
				getImageIcon(this.buttonDisabledImg) : getImageIcon(this.buttonImg);
		this.setIcon(stateIcon);
		if (! (this instanceof Hidden3DToggle)) {
		    viewAndState.put(this.rowview.get(), newstate);	
		}
	}	
	
	
	public void setDisabled() {			
		setState(ButtonState.DISABLED);	
	}	

	private ImageIcon getImageIcon(ImageCatalogItem img) {
		return new ImageIcon(
			ImageFactory.createImage(img
					 .withDisplayColor(state.getFill())
					 .withStrokeColor(state.getStroke())));
	}
	
	public void toggle() {
		LView view;
		if (state == ButtonState.DISABLED) return;
		if (this.rowview.isPresent()) {
			view = this.getView().get();
			if (!view.isVisible()) {
				view.getLayer().registerDataReceiver(view);
			}
			view.setDirty(true);
			view.setVisible(!view.isVisible());
			String configState = null;
			if (view.isVisible()) {
				setState(ButtonState.ON);
				configState = "on";
			} else {
				setState(ButtonState.OFF);
				configState = "off";
			}
			
			if (view.isOverlay() || (view.getParentLView() != null && view.getParentLView().isOverlay())) {
				String mainOrPanner = "main";
				String viewName = view.getOverlayId();
				if (view.viewman.getChild() == null) {//panner
					mainOrPanner = "panner";
					viewName = view.getParentLView().getOverlayId();
				}
				String key = viewName + "_" + mainOrPanner;
				Config.set(key,configState);
			}
		}
		sync();
	}
	

	public void toggleState()
	{		
		if (state == ButtonState.DISABLED) return;
		ButtonState newstate = state.toggleState(); 
		setState(newstate);
		LView lview;
		if (rowview.isPresent()) {
			lview = rowview.get();
			lview.setDirty(true);
			lview.setVisible(!lview.isVisible());
		}
	}	
	
	@Override
	protected void init(String text, Icon icon) {
		super.init(text, icon);		
	}
	
	public void sync() {
		if (viewAndState.containsKey(this.rowview.get())) {
			LView vue = this.rowview.get();
			ButtonState newstate = viewAndState.get(vue);
			// iterate through set toggles
			Iterator<HiddenToggleButton> it = toggles.iterator();
			while (it.hasNext()) {
				HiddenToggleButton toggl = it.next();
				if (toggl.getView().get() == vue) {
					toggl.setState(newstate);
				}
			}
		}
	}
	
	
	
	private static class HiddenToggleButtonUI extends MaterialButtonUI {

		@Override
		public void installUI(JComponent c) {			
			LView view;
			mouseHoverEnabled = false;
			super.installUI(c);	
			super.borderEnabled = false;
			if (((HiddenToggleButton)c).parentview.isPresent())
			{
				view = ((HiddenToggleButton)c).parentview.get();
				if (LViewFactory.cartographyList.contains(view.originatingFactory))
					super.disabledBackground = normalBackground;
				else super.disabledBackground = normalBackground;
			}
					
		}
	}

	public enum ButtonState {

		ON(imgON, strokeON) {
			@Override
			public ButtonState toggleState() {
				return OFF;
			}
		},

		OFF(imgOFF, strokeOFF) {
			@Override
			public ButtonState toggleState() {
				return ON;
			}
		},
			
	    DISABLED(imgDisabled, strokeDisabled) {
				@Override
				public ButtonState toggleState() {
					return DISABLED;
				}			
		};

		private Color fill, stroke;

		private ButtonState(Color fill, Color stroke) {
			this.fill = fill;
			this.stroke = stroke;
		}

		public Color getFill() {
			return this.fill;
		}

		public Color getStroke() {
			return this.stroke;
		}

		public void setStroke(Color col) {
			this.stroke = col;
		}
		
		public void setFill(Color col) {
			this.fill = col;
		}		
		
		public abstract ButtonState toggleState();
	}

}
