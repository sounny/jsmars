package edu.asu.jmars.ui.looknfeel.theme.component;


import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeButton implements ThemeComponent {

    private static String catalogKey = "button";
    
    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeButton());
    }

    public ThemeButton() {
    }

    public Font getFont() {
        return ThemeFont.getBold();
    }

    public Color getBorder() {
        return ThemeProvider.getInstance().getAction().getBorder();
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public Color getForeground() {
        return ThemeProvider.getInstance().getAction().getForeground();
    }

    public Color getDisabledtext() {
        return ThemeProvider.getInstance().getAction().getDisabledForeground();
    }

    public Color getDisabledback() {
        return ThemeProvider.getInstance().getAction().getDisabled();
    }

    public Color getHighlight() {
        return ThemeProvider.getInstance().getAction().getMain();
    }

    public Color getDarkshadow() {
        return ThemeProvider.getInstance().getAction().getContrast();
    }

    public Color getSelect() {
        return ThemeProvider.getInstance().getAction().getContrast();
    }

    public Color getOnhover() {
    	 return ThemeProvider.getInstance().getAction().getContrast();      
    }
    
    private Object getDefaultOnHover() {
    	 return ThemeProvider.getInstance().getAction().getDefaultContrast();
	}
    
    public Color getAltOnhover() {
        return ThemeProvider.getInstance().getAction().getAltContrast();
    }    

    public boolean isEnablehover() {
        return ThemeProvider.getInstance().getSettings().isButtonEnableHover();
    }

    public boolean isDefaultbuttonfollowsfocus() {
        return ThemeProvider.getInstance().getSettings().isButtonDefaultFollowsFocus();
    }

    public boolean isFocusable() {
        return ThemeProvider.getInstance().getSettings().isButtonFocusable();
    }

    public Color getDefaultback() {
        return ThemeProvider.getInstance().getAction().getMain();
    }

    public Color getDefaultforeground() {
        return ThemeProvider.getInstance().getAction().getDefaultForeground();
    }

    public Color getThemebackground() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public Color getThemehilightbackground() {
        return ThemeProvider.getInstance().getBackground().getHighlight();
    }
    
    public int getButtonArcSize() {
        return ThemeProvider.getInstance().getSettings().getButtonArcSize();
    }
    
    public Color getFocusColor()
    {
    	return ThemeProvider.getInstance().getAction().getForeground();
    }

    @Override
    public void configureUI() {        
        UIManager.put("Button.font", this.getFont());
        UIManager.put("Button.background", this.getBackground());
        UIManager.put("Button.foreground", this.getForeground());        
        UIManager.put("Button.disabledBackground", this.getDisabledback());
        UIManager.put("Button.disabledForeground", this.getDisabledtext());
        UIManager.put("Button.highlight", this.getHighlight());
        UIManager.put("Button.darkShadow", this.getDarkshadow());
        UIManager.put("Button.select", this.getSelect());
        UIManager.put("Button.mouseHoverColor", this.getOnhover());
        UIManager.put("Button.mouseHoverEnable", this.isEnablehover());
        UIManager.put("Button.defaultButtonFollowsFocus", this.isDefaultbuttonfollowsfocus());
        UIManager.put("Button.focusable", this.isFocusable());
        UIManager.put("Button[Default].background", this.getDefaultback());
        UIManager.put("Button[Default].foreground", this.getDefaultforeground());       
        UIManager.put("Button[Default][focus].color", this.getFocusColor());
        UIManager.put("Button[Default].shadowPixel", 3);
        UIManager.put("Button[Default].shadowEnable", false);
        UIManager.put("Button[Default].mouseHoverColor", this.getDefaultOnHover());
        UIManager.put("Button[focus].color", this.getFocusColor());
        UIManager.put("Button[border].enable", true);
        UIManager.put("Button[border].color", this.getDefaultback());
        UIManager.put("Button.arc", this.getButtonArcSize());                        
    }
}
