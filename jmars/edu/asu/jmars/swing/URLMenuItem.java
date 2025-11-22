package edu.asu.jmars.swing;

import java.awt.Color;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.EXTERNAL_LINK_IMG;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.Util;

/**
 * Fairly simple class which override JMenuItem.  Sets the given 
 * name as the text of the item, and adds the browser icon that 
 * is provided in the GUIState GUITheme.  Aligns the Text all the
 * way to the left, and puts the icon on the right side.
 * For now the Icon is placed at the end of the text, it might 
 * be desirable for the Icon to be fully right-justified, but it
 * was not easy to solve during a first pass.
 * 
 * Takes in a url string that is used to launch a browswer when
 * it is clicked.
 */
public class URLMenuItem extends JMenuItem {
	private String url;
	Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private ImageIcon browserIcon = new ImageIcon(ImageFactory.createImage(EXTERNAL_LINK_IMG
			   						.withDisplayColor(imgColor).withWidth(10).withHeight(10)));
			   							
	private String name;
	
	public URLMenuItem(String name, String url){
		this.name = name;
		this.url = url;
		
		setAction(browserAction);
		buildUI();
	}
	
	private void buildUI(){
		setText(name);
		setIcon(browserIcon);
		
		setHorizontalTextPosition(SwingConstants.LEFT);
		setHorizontalAlignment(SwingConstants.LEFT);
	}
	
	private Action browserAction = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			Util.launchBrowser(url);
		}
	};
	
}
