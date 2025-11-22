package edu.asu.jmars.ui.looknfeel.theme.component;

import java.awt.Color;
import javax.swing.UIManager;
import javax.swing.plaf.InsetsUIResource;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FONTS;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;
import mdlaf.utils.MaterialImageFactory;
import mdlaf.utils.icons.MaterialIconFont;


public class ThemeTabbedPane implements ThemeComponent {

    private static String catalogKey = "tabbedpane";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeTabbedPane());
    }

    public ThemeTabbedPane() {
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public Color getForeground() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public Color getSelected() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getHighlight() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }
    
    private Color getUnderlinehighlight() {
    	 return ThemeProvider.getInstance().getBackground().getContrast();		
	}    

    public Color getBorderhighlightcolor() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public Color getContentareacolor() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public Color getFocuscolorline() {
        return ThemeProvider.getInstance().getBackground().getHighlight();
    }
    
    public int getTabindent() {
        return ThemeProvider.getInstance().getSettings().getTabIndent();
    }
    
    public int getTabspace() {
        return ThemeProvider.getInstance().getSettings().getTabSpace();
    }
    
    private Color getDisableTextColor() {
    	return ThemeProvider.getInstance().getText().getDisabled();		
	}
	    

	@Override
	public void configureUI() {
		UIManager.put("TabbedPane.font", ThemeFont.getBold().deriveFont(FONTS.ROBOTO_TAB.fontSize()));
		UIManager.put("TabbedPane.selected", this.getSelected());
		UIManager.put("TabbedPane.highlight", this.getUnderlinehighlight()); // line under tabs
		UIManager.put("TabbedPane.background", this.getBackground());
		UIManager.put("TabbedPane.foreground", this.getForeground());
		UIManager.put("TabbedPane.borderHighlightColor", this.getHighlight());
		UIManager.put("TabbedPane.contentAreaColor", this.getContentareacolor());
		UIManager.put("TabbedPane[focus].colorLine", this.getFocuscolorline());
		UIManager.put("TabbedPane.selectionForeground", this.getFocuscolorline());			
		UIManager.put("TabbedPane.darkShadow", this.getBackground()); //darkshadow - is for the tabs separator line, cropped or straight
        UIManager.put("TabbedPane.shadow", this.getBackground());	//shadow - is for the tabs separator line, cropped or straight. to make it invisible, use same color as background        
        UIManager.put("TabbedPane.tabInsets", new InsetsUIResource(0, getTabspace(), 0, getTabspace()));		
		UIManager.put("TabbedPane[scrollButton].iconLeft", MaterialImageFactory.getInstance()
			   .getImage(MaterialIconFont.CHEVRON_LEFT, this.getDisableTextColor()));		
		UIManager.put("TabbedPane[scrollButton].disabledIconLeft", MaterialImageFactory.getInstance()
		    	.getImage(MaterialIconFont.CHEVRON_LEFT, this.getForeground()));
		UIManager.put("TabbedPane[scrollButton].iconRight", MaterialImageFactory.getInstance()
				.getImage(MaterialIconFont.CHEVRON_RIGHT, this.getForeground()));
		UIManager.put("TabbedPane[scrollButton].disabledIconRight", MaterialImageFactory.getInstance()
				.getImage(MaterialIconFont.CHEVRON_RIGHT, this.getDisableTextColor()));
	}

}
