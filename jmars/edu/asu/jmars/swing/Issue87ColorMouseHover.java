package edu.asu.jmars.swing;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import fr.lri.swingstates.sm.BasicInputStateMachine;
import fr.lri.swingstates.sm.State;
import fr.lri.swingstates.sm.Transition;
import fr.lri.swingstates.sm.transitions.Drag;
import fr.lri.swingstates.sm.transitions.Enter;
import fr.lri.swingstates.sm.transitions.KeyPress;
import fr.lri.swingstates.sm.transitions.Leave;
import fr.lri.swingstates.sm.transitions.Press;
import fr.lri.swingstates.sm.transitions.Release;
import fr.lri.swingstates.sm.transitions.TimeOut;
import mdlaf.MaterialLookAndFeel;
import mdlaf.animation.MaterialUIMovement;
import mdlaf.themes.MaterialLiteTheme;
import mdlaf.utils.MaterialColors;

public class Issue87ColorMouseHover extends JDialog {

	JButton jButton1 = new javax.swing.JButton();
	
	 Color imgColor = new Color(255,255,255);
     Color stroke = new Color(255,255,255);
     Color strokeGold = new Color(249,192,98);
     Color strokeGreen = new Color(66, 179, 176);
     
    
    
    ImageIcon mapLabel = new ImageIcon(ImageFactory.createImage
    		            (ImageCatalogItem.M_LOGO_IMG
    		            .withDisplayColor(imgColor)
    		            .withStrokeColor(stroke)));
    
    ImageIcon mapLabelGold = new ImageIcon(ImageFactory.createImage
            (ImageCatalogItem.M_LOGO_IMG
            .withDisplayColor(imgColor)
            .withStrokeColor(strokeGold)));
    
    ImageIcon mapLabelGreen = new ImageIcon(ImageFactory.createImage
            (ImageCatalogItem.M_LOGO_IMG
            .withDisplayColor(imgColor)
            .withStrokeColor(strokeGreen)));
	
    public Issue87ColorMouseHover(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
    }

    private void initComponents() {  


        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        this.getContentPane().setBackground(new ColorUIResource(45, 48, 56));
        
        jButton1.setBorder(BorderFactory.createLineBorder(new ColorUIResource(45, 48, 56)));
        jButton1.setBackground(new ColorUIResource(45, 48, 56));
        jButton1.setForeground(MaterialColors.COSMO_LIGTH_GRAY);        
        jButton1.setIcon(mapLabel);
       
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(166, 166, 166)
                                .addComponent(jButton1)
                                .addContainerGap(136, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(191, 191, 191)
                                .addComponent(jButton1)
                                .addContainerGap(184, Short.MAX_VALUE))
        );

        interaction.addAsListenerOf(jButton1);
        
        pack();
    }

   
    public static void main(String args[]) {
        try {
            MaterialLookAndFeel material = new MaterialLookAndFeel(new MaterialLiteTheme());
            UIManager.setLookAndFeel(material);
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Issue87ColorMouseHover.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        /* Create and display the dialog */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Issue87ColorMouseHover dialog = new Issue87ColorMouseHover(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }
    
    BasicInputStateMachine interaction = new BasicInputStateMachine(){

        Point2D ptInit = null;
        JButton tf;
    		
        public State out = new State() {            
            
        	Transition enter = new Enter(">> in") {
                public void action() {
                    tf = (JButton)(((MouseEvent)getEvent()).getComponent());
                    // tf.hilite()
                    tf.setIcon(mapLabelGold);;
                    System.out.println("State OUT - entering into Button");
                }
            };
    			
            Transition exit = new Leave(">> out") {
                public void action() {
                    tf = (JButton)(((MouseEvent)getEvent()).getComponent());
                    // tf.hilite()
                    tf.setIcon(mapLabel);
                    System.out.println("State OUT - exit out of Button");
                }
            };
            
        };
    		
        public State in = new State() {
    			
    		Transition leave = new Leave(">> out") {
                public void action() {
                    // tf.unhilite()
                	tf.setIcon(mapLabel);
                	System.out.println("State In - leaving Button");
                }
            };               
        
            Transition press = new Press(BUTTON1, ">> release") {
                public void action() {
                	tf.setIcon(mapLabelGreen);               
                    System.out.println("State - pressed mouse button in Button");
                }
            };     	
       
            Transition release = new Release(BUTTON1, ">> out") { };
                public void action() {
                    // tf.unhilite()
                	tf.setIcon(mapLabel);
                	System.out.println("State In - released mouse button  Button");
                }
            };
    		
        };

} //Issue87 class

