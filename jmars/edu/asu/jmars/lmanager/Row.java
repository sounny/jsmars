package edu.asu.jmars.lmanager;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.GroupLayout.Alignment;
import javax.swing.Icon;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputListener;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.swing.Hidden3DToggle;
import edu.asu.jmars.swing.HiddenToggleButton;
import edu.asu.jmars.swing.HiddenToggleButton.ButtonState;
import edu.asu.jmars.swing.quick.edit.row.RowQuickAccessPopup;
import edu.asu.jmars.swing.JButtonNoMouseHover;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;
import edu.asu.jmars.util.Util;
import fr.lri.swingstates.applets.ColorEvent;
import fr.lri.swingstates.events.VirtualEvent;
import fr.lri.swingstates.sm.BasicInputStateMachine;
import fr.lri.swingstates.sm.State;
import fr.lri.swingstates.sm.StateMachine;
import fr.lri.swingstates.sm.Transition;
import fr.lri.swingstates.sm.jtransitions.EnterOnComponent;
import fr.lri.swingstates.sm.jtransitions.LeaveOnComponent;
import fr.lri.swingstates.sm.jtransitions.PressOnComponent;
import fr.lri.swingstates.sm.transitions.Event;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.M_LOGO_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.M_DISABLED_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.P_LOGO_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.P_DISABLED_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.THREED_DISABLED_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.THREED_LOGO_IMG;;

public class Row extends JPanel {
	
	final Layer.LView view;
	final Layer.LView3D view3D;
	final Container parent;
	HiddenToggleButton btnM;
	HiddenToggleButton btnP;
	Hidden3DToggle btn3;
	JLabel maplabel, LED;
	JSlider slider;
	JPanel top;
	JButton quickAccess;
	Border topBottomBorder = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY);
	Border leftBorder = BorderFactory.createEmptyBorder(0, 5, 0, 0);
	Color selectionhi = ((ThemePanel)GUITheme.get("panel")).getSelectionhi();
	Border selectedLeftBorder = BorderFactory.createMatteBorder(0, 5, 0, 0, selectionhi);
	CompoundBorder normalBorder = new CompoundBorder(topBottomBorder, leftBorder);
	CompoundBorder selectedBorder = new CompoundBorder(selectedLeftBorder, topBottomBorder);
	MouseInputListener rowDockUndock;
	RowQuickAccessPopup rowpopup;
	ImageCatalogItem defaultImg = ImageCatalogItem.LAYER_STATUS;	
	
	private Color normalBackground = ((ThemePanel)GUITheme.get("panel")).getBackground();
	private Color hoverBackground = ((ThemePanel)GUITheme.get("panel")).getBackgroundhi();
	private Color defaultLED;
	private static Map<JSlider, LView> slidersPerView = new IdentityHashMap<>();	
	
	public Row(final Layer.LView view, MouseInputListener rowMouseHandler) {
		this.view = view; 		
		this.view3D = view.getLView3D();		
		this.rowDockUndock = rowMouseHandler;
		Layer layer = view.getLayer();
		if (layer != null) {
			this.defaultLED = layer.getStatus();//some lviews don't have parent layers. 
		} else {
			this.defaultLED = Util.darkGreen;
		}
		this.parent = this;
	}
	
	
	public void createGUI() {
		
		btnM = new HiddenToggleButton(view, M_LOGO_IMG, M_DISABLED_IMG);		
		toggleStates.addAsListenerOf(btnM);
		
		btnP = new HiddenToggleButton(view, view.getChild(), P_LOGO_IMG, P_DISABLED_IMG);		
		toggleStates.addAsListenerOf(btnP);
	
		btn3 = new Hidden3DToggle(view, THREED_LOGO_IMG, THREED_DISABLED_IMG);
		toggle3DStates.addAsListenerOf(btn3);		
		
		String uName = LManager.getLManager().getUniqueName(view);
		if (LViewFactory.isOverlayType(this.view)) {
			Main.testDriver.getCustomLayerNames().put(view, uName);
		}
		maplabel = new JLabel(" " + uName);		
		
		LED = new JLabel();
		LED.setIcon(new ImageIcon(ImageFactory.createImage(defaultImg
						 .withDisplayColor(defaultLED))));		 
		
		createSliderUI();
					
		rowpopup = new RowQuickAccessPopup();
		if (view.isOverlay()) {
			rowpopup.getDeleteItem().setEnabled(false);
		}
		
		quickAccess = getLayerQuickAccessBtn();		
		quickAccess.addMouseListener(quickRowMenuMouseListener);
		
		maplabel.addMouseListener(rowDockUndock);
	    maplabel.addMouseMotionListener(rowDockUndock);
	    maplabel.addMouseListener(redirectMouseExitListener);
		
	    Icon layerIcon = null;
	    
	    if (view.originatingFactory != null) {
	    	layerIcon = view.originatingFactory.getLayerIcon();
	    } else {
	        layerIcon = LViewFactory.getDefaultLayerIcon();
	    }
	    		
        JLabel icon = new JLabel(layerIcon);
        icon.setOpaque(false);

	    addMouseListener(rowDockUndock);
	    addMouseMotionListener(rowDockUndock);			
		
	    GroupLayout rowgroup = new GroupLayout(this);
        this.setLayout(rowgroup); 
	    
	    Dimension mainPanelPS = LManager.getLManager().getMainPanel().getPreferredSize();
	    int labelWidth = mainPanelPS.width - 150;
		rowgroup.setHorizontalGroup(
		    rowgroup.createSequentialGroup()
		        .addGap(3,3,3)
	            .addGroup(rowgroup.createParallelGroup(Alignment.CENTER)
	            	.addComponent(LED,12,12,12)
                    .addComponent(btnM,23,23,23))
                .addGap(6,6,6)
	            .addGroup(rowgroup.createParallelGroup(Alignment.CENTER)
	                .addComponent(icon,25,25,25)
	                .addComponent(btnP,23,23,23))
                .addGap(6,6,6)
	            .addGroup(rowgroup.createParallelGroup(Alignment.LEADING)
	                .addGroup(rowgroup.createSequentialGroup()
	                        .addComponent(maplabel, 5,labelWidth,Short.MAX_VALUE)
	                        .addComponent(quickAccess))
	                .addGroup(rowgroup.createSequentialGroup()
	                    .addComponent(btn3,23,23,23)                   
	                    .addComponent(slider, 5, 50, Short.MAX_VALUE))));
        rowgroup.setVerticalGroup(
            rowgroup.createSequentialGroup()                
                .addGroup(rowgroup.createParallelGroup(Alignment.CENTER)
                	.addComponent(LED,12,12,12)
                    .addComponent(icon,23,23,23)
                    .addComponent(maplabel)
                    .addComponent(quickAccess))
                .addGroup(rowgroup.createParallelGroup(Alignment.CENTER)
                    .addComponent(btnM,23,23,23)
                    .addComponent(btnP,23,23,23)
                    .addComponent(btn3,23,23,23)                     
                    .addComponent(slider))
                 .addGap(2));              
       
        rowgroup.linkSize(SwingConstants.HORIZONTAL, btnM, btnP, btn3);
       
        view.lightStatus.addStateMachineListener(layerLoadStatus);
        layerLoadStatus.addStateMachineListener(layerLoadStatus);
        layerLoadStatus.processEvent(new VirtualEvent("ON"));
        
        toggleStates.addStateMachineListener(toggleStates);
        toggleStates.processEvent(new VirtualEvent("ON"));
        
        toggle3DStates.addStateMachineListener(toggle3DStates);
        toggle3DStates.processEvent(new VirtualEvent("ON"));
 
        updateRow();
		updateCartographyRow();
	}
	
	
	//"Slim", as it only contains M/P/3D and opacity controls; no Title or light indicator
	public void createSlimGUI() {
		
		btnM = new HiddenToggleButton(view, M_LOGO_IMG, M_DISABLED_IMG);		
		toggleStates.addAsListenerOf(btnM);
		
		btnP = new HiddenToggleButton(view, view.getChild(), P_LOGO_IMG, P_DISABLED_IMG);		
		toggleStates.addAsListenerOf(btnP);
	
		btn3 = new Hidden3DToggle(view, THREED_LOGO_IMG, THREED_DISABLED_IMG);
		toggle3DStates.addAsListenerOf(btn3);
		
		createSliderUI();	
				
	    GroupLayout rowgroup = new GroupLayout(this);
        this.setLayout(rowgroup); 
	    		   
		rowgroup.setHorizontalGroup(
		    rowgroup.createSequentialGroup()
		        .addGap(3,3,3)
	            .addGroup(rowgroup.createParallelGroup(Alignment.CENTER)	            	
                    .addComponent(btnM,23,23,23))
                .addGap(6,6,6)
	            .addGroup(rowgroup.createParallelGroup(Alignment.CENTER)	              
	                .addComponent(btnP,23,23,23))
                .addGap(6,6,6)
	            .addGroup(rowgroup.createParallelGroup(Alignment.LEADING)	                
	                .addGroup(rowgroup.createSequentialGroup()
	                    .addComponent(btn3,23,23,23)                   
	                    .addComponent(slider, 50, 50, Short.MAX_VALUE))));
        rowgroup.setVerticalGroup(
            rowgroup.createSequentialGroup()                                         	
                .addGroup(rowgroup.createParallelGroup(Alignment.CENTER)
                    .addComponent(btnM,23,23,23)
                    .addComponent(btnP,23,23,23)
                    .addComponent(btn3,23,23,23)                     
                    .addComponent(slider))
                 .addGap(2));              
       
        rowgroup.linkSize(SwingConstants.HORIZONTAL, btnM, btnP, btn3);
  
        toggleStates.addStateMachineListener(toggleStates);
        toggleStates.processEvent(new VirtualEvent("ON"));
        
        toggle3DStates.addStateMachineListener(toggle3DStates);
        toggle3DStates.processEvent(new VirtualEvent("ON")); 
	}
	
	
	private void createSliderUI() {
		
		slider = new JSlider(0, 1000);
		slidersPerView.put(slider, this.view);
		slider.setFocusable(false);

		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				float value = (float) slider.getValue() / slider.getMaximum();
				view.setAlpha(value);
				// 3D view (check to make sure the 3d view exists at all)
				if (!slider.getValueIsAdjusting() && view3D.exists()) {
					// only update when it's finished moving, because the redraw is not seemless
					view3D.setAlpha(value);
				}
				// tooltip
				slider.setToolTipText("Opacity: " + Math.round(value * 100d) + "%");
				sync();
			}

			private void sync() {
				if (slidersPerView.containsValue(view)) {
					// iterate through slider-view map; for each view, get all related sliders
					for (Entry<JSlider, LView> entry : slidersPerView.entrySet()) {
						if (view == entry.getValue()) {
							JSlider sldr = entry.getKey();
							sldr.setValue((int) (view.getAlpha() * 1000));
						}
					}
				}
			}
		});

		slider.setValue((int) (view.getAlpha() * 1000));
		slider.addMouseListener(redirectMouseExitListener);
	}

	public void setText(String newText) {		
		maplabel.setText(newText);
		setToolTipText(newText);
	}
	
	public void setToolTipText(String layerName) {
		String tooltipText = layerName;
		LayerParameters lp = this.view.getLayerParameters();
		String txt = null;
		if (lp != null) {
			txt = SearchProvider.getInstance().getLayerTooltipText(lp.id);
		} else {
			SearchProvider search = SearchProvider.getInstance();
			String layerId = search.getLayerId(layerName);
			txt = search.getLayerTooltipText(layerId);
		}
		if (txt != null) {
			if (txt.indexOf("<name />") > -1) {//replace the <name /> tag that was added by the search result row as a placeholder.
				txt = txt.replace("<name />", "<b><u>"+layerName+"</u></b><br /><br />"+ "");
			}
			tooltipText = txt;
		}
		
		String tmp = "<br /><br /><p>For more information, double-click the layer to open up the focus panel<br /> and click the information button.</p></html>";
		tooltipText = tooltipText.replace("</html>",tmp);
		
		maplabel.setToolTipText(tooltipText);
	}
	
	public void turnOn3d() {
		if (btn3.getState() == ButtonState.OFF) {
			btn3.toggle();
		}
	}
	
	public void updateVis() {			
		if (view.isVisible()) btnM.setState(ButtonState.ON);
           else btnM.setState(ButtonState.OFF);
		if (!view.pannerStartEnabled()) btnP.setState(ButtonState.DISABLED); 
		else if (view.getChild().isVisible()) btnP.setState(ButtonState.ON);
		else btnP.setState(ButtonState.OFF);
		if(!view3D.exists()){
			btn3.setState(ButtonState.DISABLED);			
		}
		else if (view3D.isVisible()) {
			btn3.setState(ButtonState.ON);
		}
        else btn3.setState(ButtonState.OFF);		
		
		slider.setValue((int) (view.getAlpha() * 1000));
	}
	
	public Layer.LView getView() {
		return view;
	}
	
	public void setBackground(Color newColor) {
		super.setBackground(newColor);
		if (top != null){
			top.setBackground(newColor);
		}
		if (slider!=null) {
			slider.setBackground(newColor);
		}		
		if (btnM != null){
			btnM.setBackground(newColor);
		}
		if (btnP != null){
			btnP.setBackground(newColor);
		}
		if (btn3 != null){
			btn3.setBackground(newColor);
		}
		if (maplabel != null){
			maplabel.setBackground(newColor);
		}		
		if (LED != null){
			LED.setBackground(newColor);
		}		
		
		if (quickAccess != null)
			quickAccess.setBackground(newColor);
	}
	
	public void updateRow() {	
		
		if (view == LManager.getLManager().getActiveLView())
		{		
			setBorder(selectedBorder);			
		}
		else
		{			
			setBorder(normalBorder);			
		}	
		setBackground(normalBackground);	
	}
	

	public void updateCartographyRow() {		
		if (view == LManager.getLManager().getActiveLView())
		{		
			setBorder(selectedBorder);				
		}
		else
		{			
			setBorder(normalBorder);
		}
		setBackground(normalBackground);		
	}

	public void setToHoverAppearance(){
		setBackground(hoverBackground);
	}
	
	public void setToNormalAppearance(){
		setBackground(normalBackground);
	}	
	
	private boolean isSelectedView()
	{
		return (view == LManager.getLManager().getActiveLView());
	}
	
	private JButton getLayerQuickAccessBtn() {
        JButtonNoMouseHover quickAccess = new JButtonNoMouseHover();        
        Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();        
        ImageIcon ellipse = new ImageIcon(
                ImageFactory.createImage(ImageCatalogItem.ELLIPSE_MENU.withDisplayColor(imgColor)));
        quickAccess.setIcon(ellipse);  
        return quickAccess;
    }

	
	BasicInputStateMachine toggleStates = new BasicInputStateMachine() {
		HiddenToggleButton clickedToggle = null;	
		Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
		Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);		  
		
		public State START = new State() {			
			Transition on = new Event("ON", ">> ON") {};				
		};

		public State ON = new State() {
			public void enter() {			
                btnM.initState();
                if (!view.pannerStartEnabled()) btnP.setDisabled();
                else btnP.initState();                
				clickedToggle = null; // reset
			}			

			Transition TOGGLE = new PressOnComponent(BUTTON1) {
				public void action() {					
					clickedToggle = (HiddenToggleButton) getComponent();					
					clickedToggle.toggle();					
				}
			};			
			
			Transition mouseenter = new EnterOnComponent() {
				public void action() {
					clickedToggle = (HiddenToggleButton) getComponent();
					if (clickedToggle.getState() != ButtonState.DISABLED)
						setCursor(handCursor);
				}				
			};
			
			Transition mouseexit = new LeaveOnComponent() {
				public void action() {
					setCursor(defaultCursor);
				}				
			};
			
			Transition start = new Event("ON", ">> ON") {};	
		};		
	};
	
	
	StateMachine layerLoadStatus = new StateMachine() {
		Color light;

		public State START = new State() {
			Transition on = new Event("ON", ">> ON") {};
			
			Transition color = new Event(ColorEvent.class, ">> ON") {
				public void action() {
					light = ((ColorEvent) getEvent()).getColor();
				}
			};
		};

		public State ON = new State() {

			Transition color = new Event(ColorEvent.class) {
				public void action() {
					light = ((ColorEvent) getEvent()).getColor();
					LED.setIcon(new ImageIcon(ImageFactory.createImage(defaultImg
							    .withDisplayColor(light))));
					initSettingsStates();
				}
			};
		};
	};
	
	
	BasicInputStateMachine toggle3DStates = new BasicInputStateMachine() {
		Hidden3DToggle clickedToggle = null;
		Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
		Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
		
		public State START = new State() {			
			Transition on = new Event("ON", ">> ON") {};			
		};

		public State ON = new State() {
			public void enter() {
				btn3.initState();               
				clickedToggle = null; // reset
			}

			Transition TOGGLE = new PressOnComponent(BUTTON1) {
				public void action() {					
					clickedToggle = (Hidden3DToggle) getComponent();					
					clickedToggle.toggle();					
				}
			};	
			
			Transition mouseenter = new EnterOnComponent() {
				public void action() {
					clickedToggle = (Hidden3DToggle) getComponent();
					if (clickedToggle.getState() != ButtonState.DISABLED)
						setCursor(handCursor);
				}				
			};
			
			Transition mouseexit = new LeaveOnComponent() {
				public void action() {
					setCursor(defaultCursor);
				}				
			};			
			
			Transition start = new Event("ON", ">> ON") {};	
		};		
	};	
	
	
	
	MouseListener quickRowMenuMouseListener = new MouseAdapter() {
		@Override
		public void mousePressed(MouseEvent mouseEvent) {
			if (SwingUtilities.isRightMouseButton(mouseEvent) || SwingUtilities.isLeftMouseButton(mouseEvent)) {
				rowpopup.getPopupmenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
				LManager.getLManager().setActiveLView(view);
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			redispatchToParent(e);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			e.getComponent().setCursor(Cursor.getDefaultCursor());
			redispatchToParent(e);
		}

		private void redispatchToParent(MouseEvent e) {
			Component source = (Component) e.getSource();
			MouseEvent parentEvent = SwingUtilities.convertMouseEvent(source, e, source.getParent());
			source.getParent().dispatchEvent(parentEvent);
		}
	};
	
	
	MouseListener redirectMouseExitListener = new MouseListener() {
		@Override
		public void mouseExited(MouseEvent e) {			
			redispatchToParent(e);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
		    Component source = (Component) e.getSource();
            MouseEvent parentEvent = SwingUtilities.convertMouseEvent(source, e, source.getParent());           
            source.getParent().dispatchEvent(parentEvent);
		}
		

		@Override
		public void mousePressed(MouseEvent e) {}
		

		@Override
		public void mouseReleased(MouseEvent e) {}
		

		@Override
		public void mouseEntered(MouseEvent e) {			
			redispatchToParent(e);
		}

		private void redispatchToParent(MouseEvent e) {
			Component source = (Component) e.getSource();
			MouseEvent parentEvent = SwingUtilities.convertMouseEvent(source, e, source.getParent());			
			source.getParent().dispatchEvent(parentEvent);
		}
	};

	public JButton getM() {
		return btnM;
	}
	
	public void initSettingsStates() {
		btnM.sync();
		btnP.sync();
		btn3.sync();
		slider.setValue((int) (view.getAlpha() * 1000));	
	}
		
}

