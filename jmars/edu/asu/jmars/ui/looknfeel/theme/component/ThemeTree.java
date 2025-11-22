package edu.asu.jmars.ui.looknfeel.theme.component;



import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.MAP_LAYER_IMG;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;

public class ThemeTree implements ThemeComponent {

    private static String catalogKey = "tree";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeTree());
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getContrast();
    }

    public Color getSelectionforeground() {
        return ThemeProvider.getInstance().getBackground().getHighlight();
    }

    public Color getTextforeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getTextbackground() {
        return ThemeProvider.getInstance().getBackground().getContrast();
    }

    public boolean isPaintlines() {
        return ThemeProvider.getInstance().getSettings().isTreePaintLines();
    }
    
    public Color getIconsColor() {
        return ThemeProvider.getInstance().getAction().getMain();
    } 

    public ThemeTree() {
    }

    @Override
    public void configureUI() {    	
    	ImageIcon openIcon = new ImageIcon(ImageFactory
    		 .createImage(ImageCatalogItem.PAN_S.withDisplayColor(this.getIconsColor())));
    	ImageIcon closedIcon = new ImageIcon(ImageFactory
       		 .createImage(ImageCatalogItem.PAN_E.withDisplayColor(this.getIconsColor())));       	   	
        UIManager.put("Tree.font", ThemeFont.getRegular());
        UIManager.put("Tree.background", this.getBackground());
        UIManager.put("Tree.selectionForeground", this.getSelectionforeground());
        UIManager.put("Tree.textForeground", this.getTextforeground());
        UIManager.put("Tree.textBackground", this.getTextbackground());
        UIManager.put("Tree.paintLines", this.isPaintlines()); 
        UIManager.put("Tree.openIcon", openIcon);  
        UIManager.put("Tree.closedIcon", closedIcon);
        UIManager.put("Tree.selectionBackground", this.getBackground());
        UIManager.put("Tree.selectionBorderColor", this.getBackground());
    }

}
