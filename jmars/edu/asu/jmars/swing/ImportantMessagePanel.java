package edu.asu.jmars.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;


public class ImportantMessagePanel extends JPanel {
	private JLabel iconLbl;
	private ImageIcon icon;
	private JTextArea messageTA;
	private String myMessage;
	private Color bgColor = ((ThemePanel)GUITheme.get("panel")).getBackgroundhi();
	
	public ImportantMessagePanel(String message){
		Color imgColor = ((ThemeImages) GUITheme.get("images")).getLinkfill();
		icon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.INFO .withDisplayColor(imgColor)));
				           
		myMessage = message;
		
		buildUI();
	}
	
	private void buildUI(){
		setBackground(bgColor);
		setBorder(new EmptyBorder(10,10,10,10));
		setLayout(new BorderLayout());
		
		//create the label with icon
		iconLbl = new JLabel(icon);
		iconLbl.setBackground(bgColor);
		JPanel iconPnl = new JPanel(new BorderLayout());
		iconPnl.setBackground(bgColor);
		iconPnl.setBorder(new EmptyBorder(0, 0, 0, 10));
		iconPnl.add(iconLbl, BorderLayout.NORTH);
		
		messageTA = new JTextArea(myMessage);
		messageTA.setBackground(bgColor);
		messageTA.setEditable(false);
		messageTA.setWrapStyleWord(true);
		messageTA.setLineWrap(true);
		messageTA.setFont(ThemeFont.getRegular().deriveFont(14f));	
		
		add(iconPnl, BorderLayout.WEST);
		add(messageTA, BorderLayout.CENTER);
		
	}
	
	
	public void paintComponent(Graphics g){
		super.paintComponent(g);
		
		Graphics2D g2 = (Graphics2D) g;
		
		if(getParent() != null){
			Color parentBg = getParent().getBackground();
			
			g2.setColor(parentBg);
			g2.fillRect(0, 0, getWidth(), getHeight());
		}
		
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(bgColor);		
		g2.fillRoundRect(0, 0, (getWidth()), getHeight(), 10, 10);
		
	}

}
