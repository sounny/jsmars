package edu.asu.jmars.ui.looknfeel.theme.component;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.PAN_N;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.PAN_S;


import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;
import mdlaf.utils.MaterialBorders;
import mdlaf.utils.MaterialColors;


public class ThemeTaskPane implements ThemeComponent {

    private static String catalogKey = "taskpane";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeTaskPane());
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getAlternateContrast();
    }
    
    public Color getMainBackground() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }
    

    public Color getContentbackground() {
        return ThemeProvider.getInstance().getBackground().getAlternateContrastBright();
    }

    public Color getTitlebackgroundgradientstart() {
        return ThemeProvider.getInstance().getBackground().getContrast();
    }

    public Color getForeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getTitleover() {
        return ThemeProvider.getInstance().getBackground().getHighlightContrast();
    }

    public Color getYescollapsedimgcolor() {
        return ThemeProvider.getInstance().getImage().getFill();
    }

    public Color getNocollapsedimgcolor() {
        return ThemeProvider.getInstance().getImage().getFill();
    }

    public ThemeTaskPane() {
    }

    @Override
    public void configureUI() {       
        UIManager.put("TaskPane.font", ThemeFont.getBold());
        UIManager.put("TaskPane.contentBackground", this.getMainBackground());
        UIManager.put("TaskPane.titleBackgroundGradientStart", this.getTitlebackgroundgradientstart());
        UIManager.put("TaskPane.foreground", this.getForeground());        
        UIManager.put("TaskPane.yesCollapsed", new ImageIcon(
                ImageFactory.createImage(PAN_N.withDisplayColor(this.getYescollapsedimgcolor()))));
        UIManager.put("TaskPane.noCollapsed", new ImageIcon(
                ImageFactory.createImage(PAN_S.withDisplayColor(this.getNocollapsedimgcolor()))));
        UIManager.put("TaskPane.background", this.getMainBackground());
        UIManager.put("TaskPane.titleOver",  this.getTitleover());
        UIManager.put("TaskPane.specialTitleOver", this.getTitleover());
        UIManager.put("TaskPane.titleForeground", this.getForeground());
        UIManager.put("TaskPane.arch", 0);
        UIManager.put("TaskPane.border", MaterialBorders.DARK_LINE_BORDER);
        UIManager.put("TaskPane.borderColor", this.getMainBackground());
    }
}