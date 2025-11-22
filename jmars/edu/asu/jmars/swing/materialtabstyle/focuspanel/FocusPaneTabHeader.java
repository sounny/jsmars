package edu.asu.jmars.swing.materialtabstyle.focuspanel;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CLOSE;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.DOCK_ME;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.UNDOCK_ME;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.INFO;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.SETTINGS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;


public class FocusPaneTabHeader extends JToolBar {

    private JPanel containerButtons;
    private JPanel containerLabel;
    private JLabel labelButton;
    private  JLabel infoButton;
    private  JLabel dockButton;
    private JLabel settingButton;
    private JLabel title;       
    private JLabel undockButton;    
    private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
    private static Color imgSelectedColor = ((ThemeImages) GUITheme.get("images")).getSelectedfill();
    private static Icon info = new ImageIcon(ImageFactory.createImage(INFO.withDisplayColor(imgColor)));
    private static Icon dockme = new ImageIcon(ImageFactory.createImage(DOCK_ME.withDisplayColor(imgColor)));
    private static Icon settings = new ImageIcon(ImageFactory.createImage(SETTINGS.withDisplayColor(imgColor)));
    private static Icon settings_sel = new ImageIcon(ImageFactory.createImage(SETTINGS.withDisplayColor(imgSelectedColor)));
    private static Icon close = new ImageIcon(ImageFactory.createImage(CLOSE.withDisplayColor(imgSelectedColor)));    
    private static Icon undockme = new ImageIcon(ImageFactory.createImage(UNDOCK_ME.withDisplayColor(imgColor)));
    private static String INFO_TIP = "Click here for layer information";
    private static String DOCK_TIP = "Click here to dock this window";
    private static String UNDOCK_TIP = "Click here to undock this window";
    private static String SETTINGS_TIP = "Click here to access layer settings, M P 3D Opacity";
    private static String NO_TIP = null;
    enum SETTINGS_STATE { ON, OFF};
    private SETTINGS_STATE settingsState = SETTINGS_STATE.OFF;
    
    
	public FocusPaneTabHeader() {		   	
        init();
    }

    private void init() {    	
        initComponent();
        initLayout();      
        setVisible(true);     
    }

    private void initComponent() {
        this.containerButtons = new JPanel();
        this.containerLabel = new JPanel(new BorderLayout());
        this.title = new JLabel();
        
        this.labelButton = new JLabel();
        labelButton.setVisible(false);
        
        this.infoButton = new JLabel(info);
        this.infoButton.setToolTipText(INFO_TIP);
        
        this.dockButton = new JLabel(dockme);   
        this.dockButton.setToolTipText(DOCK_TIP);
        
        this.settingButton = new JLabel(settings); 
        this.settingButton.setToolTipText(SETTINGS_TIP);
        
        this.undockButton = new JLabel(undockme);   
        this.undockButton.setToolTipText(UNDOCK_TIP);
        
        this.setBackground(((ThemePanel) GUITheme.get("panel")).getBackgroundhi());
        setFloatable(false);
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    }

    private void initLayout() {
        this.setLayout(new BorderLayout());
        Dimension gap = new Dimension(5,0);
        containerLabel.add(labelButton, BorderLayout.CENTER);
        containerLabel.add(infoButton, BorderLayout.EAST);
        this.containerButtons.add(containerLabel, BorderLayout.WEST);
        this.containerButtons.add(Box.createRigidArea(gap));
        this.containerButtons.add(settingButton, BorderLayout.CENTER);
        this.containerButtons.add(Box.createRigidArea(gap));
        this.containerButtons.add(dockButton, BorderLayout.CENTER);
        this.containerButtons.add(Box.createRigidArea(gap));
        this.containerButtons.add(undockButton, BorderLayout.EAST);
        //this.containerButtons.add(Box.createRigidArea(gap));
        this.add(containerButtons, BorderLayout.EAST);
        this.add(title, BorderLayout.WEST);
    }
 
    
    public void setVisibleLabelButton(String text, Color color, boolean visible) {
        if (text == null && color == null) {
            this.labelButton.setVisible(visible);
        }
        if (text != null && color != null) {
            this.labelButton.setText(text);
            this.labelButton.setForeground(color);
            this.labelButton.setVisible(visible);
        }
    }
  
    public void setInfoIcon(Icon icon) {
		this.infoButton.setIcon(icon);
	}
    
    public void setSettingsIcon(Icon icon) {
  		this.settingButton.setIcon(icon);
  	}
    
    public void toggleInfoToClose() {
    	this.infoButton.setIcon(close);										
		this.setVisibleLabelButton("Close Info ".toUpperCase(), imgSelectedColor, true);					       
    }
    
    public void toggleDockToUndock() {
    	this.dockButton.setVisible(false);    	
    	this.dockButton.setToolTipText(NO_TIP);   	
    	this.undockButton.setVisible(true);
    	this.undockButton.setToolTipText(UNDOCK_TIP);
    }
    
    public void toggleUnDockToDock() {    
    	this.dockButton.setVisible(true);
    	this.dockButton.setToolTipText(DOCK_TIP);   	    
    	this.undockButton.setVisible(false);
    	this.undockButton.setToolTipText(NO_TIP);
    }
    
    public void toggleInfoToInfo() {
    	this.infoButton.setIcon(info);										
		this.setVisibleLabelButton("", null, false);					       
    }   
    
    public void toggleSettings(boolean selected) {
    	if (selected) {
    	    settingButton.setIcon(settings_sel);
    	    settingsState = SETTINGS_STATE.ON;
    	}
    	else {
    		 settingButton.setIcon(settings);	
    		 settingsState = SETTINGS_STATE.OFF;
    	}
    } 
    
    
    public boolean isSettingsSelected() {
    	return  (settingsState == SETTINGS_STATE.ON ? true : false);
    }

    public void setTitle(String titleString) {
        if (title != null) {
            this.title.setText(titleString);
        }
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (containerButtons != null) {
            this.containerButtons.setBackground(bg);
            this.containerLabel.setBackground(bg);
            this.infoButton.setBackground(bg);
            this.settingButton.setBackground(bg);
            this.dockButton.setBackground(bg);
            this.undockButton.setBackground(bg);
        }
    }

    public void setDocked(boolean docked){
        this.setVisible(!docked);
    }

    public boolean isDocked(){
        return !this.isVisible();
    }

	public void showSettingsButton(boolean b) {
		this.settingButton.setVisible(b);		
	} 
	
	public void showInfoButton(boolean b) {
		this.infoButton.setVisible(b);		
	} 
	
	public void showDockButton(boolean b) {
		this.dockButton.setVisible(b);		
	}

	public Component dockButton() {		
		return this.dockButton;
	}
	
	public Component undockButton() {		
		return this.undockButton;
	}

	public Component infoButton() {		
		return this.infoButton;
	}

	public Component settingsButton() {		
		return this.settingButton;
	}

	public void toggledockbutton(boolean docked) {
		if (docked) this.toggleDockToUndock();			
		else this.toggleUnDockToDock();		
	} 	
}
