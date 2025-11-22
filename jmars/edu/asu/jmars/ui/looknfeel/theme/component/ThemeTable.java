package edu.asu.jmars.ui.looknfeel.theme.component;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CHECK_OFF_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CHECK_ON_IMG;
import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.border.Border;

import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FONTS;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeTable implements ThemeComponent {

    private static String catalogKey = "table";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeTable());
    }

    public ThemeTable() {
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getSelectionbackground() {
        return ThemeProvider.getInstance().getSelection().getMain();
    }

    public Color getSelectionforeground() {
        return ThemeProvider.getInstance().getSelection().getForeground();
    }

    public Color getForeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getRow().getBackground();    	
    }

    public boolean isFocusable() {
        return ThemeProvider.getInstance().getSettings().isTableFocusable();
    }

    public Color getGridcolor() {
        return ThemeProvider.getInstance().getRow().getHorizgrid();
    }

    public int getRowheight() {
        return ThemeProvider.getInstance().getSettings().getTableRowHeight();
    }

    public boolean isAlternaterowcolor() {
        return ThemeProvider.getInstance().getSettings().isTableAlternateRowColor();
    }

    public Color getAlternaterowbackground() {
        return ThemeProvider.getInstance().getRow().getAlternateback();
    }

    public Color getCheckon() {
        return ThemeProvider.getInstance().getAction().getImageOn();
    }

    public Color getCheckoff() {
        return ThemeProvider.getInstance().getAction().getImageOff();
    }

    public Color getSpecialDataHighlight() {
        return ThemeProvider.getInstance().getText().getHighlight();
    }
    
    public Border getBorderForTableCell() {
    	return BorderFactory.createMatteBorder(1, 1, 1, 1, ThemeProvider.getInstance().getAction().getBorder());   	
    }

    @Override
    public void configureUI() {        
        UIManager.put("Table.font", ThemeFont.getRegular().deriveFont(FONTS.ROBOTO_TABLE.fontSize()));
        UIManager.put("Table.selectionBackground", this.getSelectionbackground());
        UIManager.put("Table.selectionForeground", this.getSelectionforeground());
        UIManager.put("Table.foreground", this.getForeground());
        UIManager.put("Table.background", this.getBackground());
        UIManager.put("Table.focusable", this.isFocusable());
        UIManager.put("Table.gridColor", this.getGridcolor());       
        UIManager.put("Table[CheckBox].checked",
                new ImageIcon(ImageFactory.createImage(CHECK_ON_IMG.withDisplayColor(this.getCheckon()))));
        UIManager.put("Table[CheckBox].unchecked",
                new ImageIcon(ImageFactory.createImage(CHECK_OFF_IMG.withDisplayColor(this.getCheckoff()))));
        UIManager.put("Table[CheckBox].selectionUnchecked", 
        		new ImageIcon(ImageFactory.createImage(CHECK_OFF_IMG.withDisplayColor(this.getCheckoff()))));
        UIManager.put("Table[CheckBox].selectionChecked",
        		new ImageIcon(ImageFactory.createImage(CHECK_ON_IMG.withDisplayColor(this.getCheckon()))));       
        UIManager.put("Table.rowHeight", this.getRowheight());       
        UIManager.put("Table.alternateRowColor", this.getAlternaterowbackground());
        UIManager.put("Table.border", BorderFactory.createEmptyBorder());
    }

}
