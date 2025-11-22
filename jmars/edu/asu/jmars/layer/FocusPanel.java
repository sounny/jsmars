package edu.asu.jmars.layer;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.LayoutStyle;
import edu.asu.jmars.lmanager.Row;
import edu.asu.jmars.swing.materialtabstyle.focuspanel.FocusPaneTabHeader;
import edu.asu.jmars.swing.sm.events.CloseInfoEvent;
import edu.asu.jmars.util.Util;
import fr.lri.swingstates.events.VirtualEvent;
import fr.lri.swingstates.sm.BasicInputStateMachine;
import fr.lri.swingstates.sm.State;
import fr.lri.swingstates.sm.Transition;
import fr.lri.swingstates.sm.jtransitions.PressOnComponent;
import fr.lri.swingstates.sm.transitions.Event;


public class FocusPanel extends JTabbedPane {   //continue with states of buttons - State Machines
	public FocusInfoPanel infoPanel, infoDetached;	
	boolean docked = false;
	public JFrame parentFrame = new JFrame();	
	private JPanel container = new JPanel();	
	private JPanel panelForFocusPanel = new JPanel();
	public FocusPaneTabHeader focuspanelheader = new FocusPaneTabHeader();	
	public boolean isLimited;
	public Layer.LView parent;
	private Row viewrowWithPanner = null;
	private JPanel viewrowPlaceHolder = null;
	public int selectedIndex;
	public static boolean dockByDefault = true;
	public static final String FOCUS_TITLE = "Options";	
	private boolean hasBeenInitialized = false;
	private Map<Component, Component> mapFocusPanelToContainer= new HashMap<>();	
	private CardLayout cl;
	private GroupLayout fplayout;
    private final static String FOCUSPANEL = "Card with FocusPanel";
    private final static String INFOPANEL = "Card with Info"; 
    private boolean isViewRowWithPannerInitialized = false;
	

	public FocusPanel(Layer.LView parent) {
		this(parent, false);		
	}

	public FocusPanel(Layer.LView parent, boolean isLimited) {
		this.parent = parent;		
		this.isLimited = isLimited;	
		infoPanel = new FocusInfoPanel(parent, isLimited);	
		infoDetached = new FocusInfoPanel(parent, isLimited);
		
		if (parent == null) {
			this.parentFrame.setTitle("Blank Layer");
		} else {
			this.parentFrame.setTitle(parent.getName() + FOCUS_TITLE);
		}
		
		parentFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		this.setTabPlacement(JTabbedPane.TOP);

		if (selectedIndex < getComponentCount()) {
			setSelectedIndex(selectedIndex);
		}
		
		createUI();		
		
		initStates();			
		
		setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
						
		mapFocusPanelToContainer.put(FocusPanel.this, this.container);
	}

	
	private void createUI() {
		//placeholder viewrow, as at this point of jmars execution panner is not initialized yet
		viewrowPlaceHolder = new JPanel();
		isViewRowWithPannerInitialized = false; //we will reinitialize viewrow when LView and its panner are ready
		FocusPanelLayout fplayout = new FocusPanelLayout();
		fplayout.init();
		cl = (CardLayout)(container.getLayout());
        cl.show(container, FOCUSPANEL);	        	
		parentFrame.setContentPane(container);
	}
	
	
	private Row initViewRow() {
		Row viewrow = new Row(this.parent, null);				
		viewrow.createSlimGUI();
		viewrow.setToHoverAppearance();
		return viewrow;
	}
	

	private void initStates() {		
		this.smFocusPanelInfo.addStateMachineListener(this.smFocusPanelInfo);
		this.smFocusPanelInfo.addStateMachineListener(infoPanel.smCloseInfoPanel);
		this.infoPanel.smCloseInfoPanel.addStateMachineListener(smFocusPanelInfo);
		this.smFocusPanelInfo.addStateMachineListener(infoPanel.smDetachInfoPanel);
		this.infoPanel.smDetachInfoPanel.addStateMachineListener(smFocusPanelInfo);
		this.smFocusPanelInfo.addStateMachineListener(infoPanel.smDockInfoPanel);
		this.smFocusPanelInfo.addStateMachineListener(infoPanel.smUnDockInfoPanel);
		this.infoPanel.smDockInfoPanel.addStateMachineListener(smFocusPanelInfo);
		this.infoPanel.smUnDockInfoPanel.addStateMachineListener(smFocusPanelInfo);
		this.infoDetached.smDockInfoPanel.addStateMachineListener(smFocusPanelInfo);
		this.infoDetached.smUnDockInfoPanel.addStateMachineListener(smFocusPanelInfo);
		this.smFocusPanelInfo.addStateMachineListener(infoDetached.smDockInfoPanel);
		this.smFocusPanelInfo.addStateMachineListener(infoDetached.smUnDockInfoPanel);
		
		this.smFocusPanelInfo.addStateMachineListener(smLayerSettings);
		smLayerSettings.addStateMachineListener(this.smFocusPanelInfo);	
		
		this.smFocusPanelDock.addStateMachineListener(this.smFocusPanelDock);
		this.smFocusPanelDock.addStateMachineListener(infoPanel.smCloseInfoPanel);
		this.infoPanel.smCloseInfoPanel.addStateMachineListener(smFocusPanelDock);
		this.smFocusPanelDock.addStateMachineListener(infoPanel.smDetachInfoPanel);
		this.infoPanel.smDetachInfoPanel.addStateMachineListener(smFocusPanelDock);
		this.smFocusPanelDock.addStateMachineListener(infoPanel.smDockInfoPanel);
		this.smFocusPanelDock.addStateMachineListener(infoPanel.smUnDockInfoPanel);
		this.infoPanel.smDockInfoPanel.addStateMachineListener(smFocusPanelDock);
		this.infoPanel.smUnDockInfoPanel.addStateMachineListener(smFocusPanelDock);	
		
		this.smFocusPanelUnDock.addStateMachineListener(this.smFocusPanelUnDock);
		this.smFocusPanelUnDock.addStateMachineListener(infoPanel.smCloseInfoPanel);
		this.infoPanel.smCloseInfoPanel.addStateMachineListener(smFocusPanelUnDock);
		this.smFocusPanelUnDock.addStateMachineListener(infoPanel.smDetachInfoPanel);
		this.infoPanel.smDetachInfoPanel.addStateMachineListener(smFocusPanelUnDock);
		this.smFocusPanelUnDock.addStateMachineListener(infoPanel.smDockInfoPanel);
		this.smFocusPanelUnDock.addStateMachineListener(infoPanel.smUnDockInfoPanel);
		this.infoPanel.smDockInfoPanel.addStateMachineListener(smFocusPanelUnDock);	
		this.infoPanel.smUnDockInfoPanel.addStateMachineListener(smFocusPanelUnDock);					
		
		this.smFocusPanelInfo.processEvent(new VirtualEvent("ON"));	
		this.smFocusPanelDock.processEvent(new VirtualEvent("ON"));
		this.smLayerSettings.processEvent(new VirtualEvent("ON"));
		this.smFocusPanelUnDock.processEvent(new VirtualEvent("ON"));
	}
	
	
	public void dock(boolean selectTab) {		
		String name = parentFrame.getTitle();				
		if (selectedIndex > 0 && selectedIndex <= getComponentCount()) {
			setSelectedIndex(selectedIndex);
		}
		parentFrame.setVisible(false);	
		this.updateWhenDocked();
		LManager.getLManager().dockTab(name, parent.light2, container, FocusPanel.this, selectTab);
		docked = true;
	}
	
	
	public void undock() {						
		showInFrame();				
		docked = false;
	}	

	
	
	public boolean isDocked() {
		return docked;
	}


	public Component add(String title, Component component) {
		JScrollPane sp = new JScrollPane(component);
		sp.getVerticalScrollBar().setUnitIncrement(20);			
		super.add(title, sp);		
		return component;
	}
	

	public void addTab(String title, Component component) {				
		super.addTab(title.toUpperCase(), component);	
	}
	

	public JFrame getFrame() {
		return parentFrame;
	}


	public void restoreInFrame(int locX, int locY, int width, int height) {					
		updateWhenUndocked();		
		parentFrame.setLocation(locX, locY);
		parentFrame.setSize(width, height);	
		parentFrame.setIconImage(Util.getJMarsIcon());
		parentFrame.setVisible(true);		
		docked = false;
	}		
			

	public void showInFrame() {				
		updateWhenUndocked();			
		if (!hasBeenInitialized) {
			parentFrame.pack();
			hasBeenInitialized = true;
		}
		parentFrame.setIconImage(Util.getJMarsIcon());
		// If it's minimized, this will bring the window back up.
		if (parentFrame.getState() != JFrame.NORMAL) {
			parentFrame.setState(JFrame.NORMAL);
		}
		parentFrame.toFront();
		parentFrame.setVisible(false);
		parentFrame.setVisible(true);
		docked = false;
	}

	
	@Override
	public Dimension getPreferredSize() {
		Dimension preferred = super.getPreferredSize();
		preferred.width = Math.max(preferred.width, 600);
		return preferred;
	}
	
	
	BasicInputStateMachine smFocusPanelInfo = new BasicInputStateMachine() {

		public State START = new State() {
			Transition on = new Event("ON", ">> ON") {
				public void action() {
					addAsListenerOf(focuspanelheader.infoButton());
				}
			};
		};

		public State ON = new State() {

			Transition showinfo = new PressOnComponent(BUTTON1) {
				public void action() {					
					infoPanel.hideInPanelDetachDockButton(isDocked() ? true : false);
					if (isDocked())
						infoPanel.infopanelheader.toggleDockToUndock();	
					else
						infoPanel.infopanelheader.toggleUnDockToDock();	
					cl.show(container, INFOPANEL);					
				}
			};
			
			Transition internalshowinfo = new Event("internalshowinfo") {
				public void action() {					
					infoPanel.hideInPanelDetachDockButton(false);
					cl.show(container, INFOPANEL);
				}
			};			

		
			Transition closeinfo = new Event(CloseInfoEvent.class) {
				public void action() {					
					boolean isInfoDeatched = ((CloseInfoEvent) getEvent()).getisInfoDockedFlag();				
					focuspanelheader.showInfoButton(!isInfoDeatched);										
					focuspanelheader.toggledockbutton(isDocked());								
					cl.show(container, FOCUSPANEL);						
				}				
		
			};
		};
	};

	
	BasicInputStateMachine smFocusPanelDock = new BasicInputStateMachine() {

		public State START = new State() {
			Transition on = new Event("ON", ">> ON") {
				public void action() {
					addAsListenerOf(focuspanelheader.dockButton());										
				}
			};
		};

		public State ON = new State() {
			Transition dockme = new PressOnComponent(BUTTON1) {
				public void action() {					
					infoPanel.hideInPanelDetachDockButton(true);
					focuspanelheader.toggleDockToUndock();
					dock(true);
					fireEvent(new VirtualEvent("internalcloseinfo"));		
				}
			};
			
			Transition internaldockme = new Event("internaldockme") {
				public void action() {					
					infoPanel.hideInPanelDetachDockButton(true);
					infoPanel.infopanelheader.toggleDockToUndock();
					dock(true);
				}
			};						
		};
	};
	
	
	
	BasicInputStateMachine smFocusPanelUnDock = new BasicInputStateMachine() {

		public State START = new State() {
			Transition on = new Event("ON", ">> ON") {
				public void action() {
					addAsListenerOf(focuspanelheader.undockButton());										
				}
			};
		};

		public State ON = new State() {
			Transition undockme = new PressOnComponent(BUTTON1) {
				public void action() {					
					infoPanel.hideInPanelDetachDockButton(false);																	
					undock();						
				}
			};
			
			Transition internalundockme = new Event("internalundockme") {
				public void action() {					
					infoPanel.hideInPanelDetachDockButton(false);
					infoPanel.infopanelheader.toggleUnDockToDock();
					undock();
				}
			};						
			
		};
	};
		
	
	
	
	BasicInputStateMachine smLayerSettings = new BasicInputStateMachine() {
		
		boolean settingsSelected = false;

		public State START = new State() {
			Transition on = new Event("ON", ">> ON") {
				public void action() {
					addAsListenerOf(focuspanelheader.settingsButton());									
				}
			};
		};

		public State ON = new State() {
			Transition showsettings = new PressOnComponent(BUTTON1) {
				public void action() {					
					settingsSelected = (settingsSelected == true) ? false : true;
					focuspanelheader.toggleSettings(settingsSelected);
					if (!isViewRowWithPannerInitialized) {
						viewrowWithPanner = initViewRow();						
						fplayout.replace(viewrowPlaceHolder, viewrowWithPanner);
						panelForFocusPanel.revalidate();
						panelForFocusPanel.repaint();						
						isViewRowWithPannerInitialized = true;
					}
					viewrowWithPanner.initSettingsStates();  //toggles should always reflect the view state
					viewrowWithPanner.setVisible(settingsSelected);					
				}
		    };		    			
		};
	};
		
	
	public Component getContainerForThisFocusPanel() {
		return mapFocusPanelToContainer.get(FocusPanel.this);		
	}
	

    public Container getInfoPanel() {
    	return infoDetached;
    }
	
	
	private void updateWhenUndocked() {
		if (viewrowPlaceHolder != null) viewrowPlaceHolder.setVisible(false);
		if (viewrowWithPanner  != null) viewrowWithPanner.setVisible(focuspanelheader.isSettingsSelected());	
		focuspanelheader.showInfoButton(true);
		focuspanelheader.toggleUnDockToDock();
		infoPanel.hideInPanelDetachDockButton(false);
		infoPanel.infopanelheader.toggledockbutton(false);
		parentFrame.setContentPane(container);		
	}	
	
	private void updateWhenDocked() {
		if (viewrowPlaceHolder != null) viewrowPlaceHolder.setVisible(false);
		if (viewrowWithPanner  != null) viewrowWithPanner.setVisible(focuspanelheader.isSettingsSelected());		
		focuspanelheader.showInfoButton(true);		
		focuspanelheader.toggledockbutton(true);
		infoPanel.hideInPanelDetachDockButton(true);
		infoPanel.infopanelheader.toggledockbutton(true);		
	}		

	
	private class FocusPanelLayout {

		public FocusPanelLayout() {
			init();
		}

		private void init() {
			
			container.setLayout(new CardLayout());							
			
			container.add(infoPanel, INFOPANEL);
			
			fplayout = new GroupLayout(panelForFocusPanel);			
			panelForFocusPanel.setLayout(fplayout);
			fplayout.setHorizontalGroup(fplayout.createParallelGroup()				
			.addComponent(focuspanelheader, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
			.addComponent(viewrowPlaceHolder, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE )
			.addComponent(FocusPanel.this, GroupLayout.Alignment.TRAILING, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
			);
			fplayout.setVerticalGroup(fplayout.createParallelGroup()					
			.addGroup(fplayout.createSequentialGroup()
			.addComponent(focuspanelheader, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
			.addComponent(viewrowPlaceHolder, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
			.addComponent(FocusPanel.this, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
			);							
			container.add(panelForFocusPanel, FOCUSPANEL);						
		}				
	}
	
    public void showInfo() {
		smFocusPanelInfo.fireEvent(new VirtualEvent("internalshowinfo"));		
	}		
	
}
