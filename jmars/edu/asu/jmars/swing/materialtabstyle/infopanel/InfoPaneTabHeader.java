package edu.asu.jmars.swing.materialtabstyle.infopanel;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CLOSE;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.DOCK_ME;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.UNDOCK_ME;
import java.awt.BorderLayout;
import java.awt.Color;
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
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;


public class InfoPaneTabHeader extends JToolBar {

    private JPanel containerButtons;
    private JPanel containerLabel;
    private JPanel titlePanel;
    public JLabel  labelButton;
    public JLabel  closeinfoButton;
    public JLabel  dockButton, undockButton;  
    private JLabel title;   
    
    private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
    private static Color imgSelectedColor = ((ThemeImages) GUITheme.get("images")).getSelectedfill();
    private static Icon dockme = new ImageIcon(ImageFactory.createImage(DOCK_ME.withDisplayColor(imgColor)));
    private static Icon close = new ImageIcon(ImageFactory.createImage(CLOSE.withDisplayColor(imgSelectedColor)));
    private static Icon undockme = new ImageIcon(ImageFactory.createImage(UNDOCK_ME.withDisplayColor(imgColor)));
    private static String DOCK_TIP = "Click here to dock this window";
    private static String UNDOCK_TIP = "Click here to undock this window";
    private static String NO_TIP = null;
    
	
    public InfoPaneTabHeader() {		   	
        init();
    }

    private void init() {    	
        initComponent();
        initLayout();        
        setVisible(true);     
    }

    private void initComponent() {
        this.containerButtons = new JPanel();
        this.containerLabel = new JPanel(new BorderLayout(5,0));
        
        titlePanel = new JPanel(new BorderLayout());       
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));  
        this.title = new JLabel();
		title.setFont(ThemeFont.getBold());		
		titlePanel.add(this.title, BorderLayout.WEST);            
        
        
        this.labelButton = new JLabel("Close Info".toUpperCase());
        this.labelButton.setForeground(imgSelectedColor);
        this.labelButton.setVisible(true);
        
        this.closeinfoButton = new JLabel(close);

        this.dockButton = new JLabel(dockme); 
        this.dockButton.setToolTipText(DOCK_TIP);
        
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
         containerLabel.add(closeinfoButton, BorderLayout.EAST);
         this.containerButtons.add(containerLabel, BorderLayout.WEST);
         this.containerButtons.add(Box.createRigidArea(gap));       
         this.containerButtons.add(dockButton, BorderLayout.CENTER);
         this.containerButtons.add(Box.createRigidArea(gap));
         this.containerButtons.add(undockButton, BorderLayout.EAST);
         this.add(containerButtons, BorderLayout.EAST);
         this.add(titlePanel, BorderLayout.WEST);
    }

     
    public void setTitle(String titleString) {
        if (title != null) {
            this.title.setText(titleString);
        }
    }
    
    public void hideCloseInfoButton() { 
	 containerLabel.setVisible(false);	 
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

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (containerButtons != null) {
            this.containerButtons.setBackground(bg);
            this.containerLabel.setBackground(bg);
            this.closeinfoButton.setBackground(bg); 
            this.dockButton.setBackground(bg);
            this.undockButton.setBackground(bg);
            this.titlePanel.setBackground(bg);
        }
    }

	public void toggledockbutton(boolean docked) {
			if (docked) this.toggleDockToUndock();			
			else this.toggleUnDockToDock();		
	}						

}

