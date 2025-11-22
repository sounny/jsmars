package edu.asu.jmars.ui.looknfeel.theme.component;

import java.awt.Color;
import javax.swing.UIManager;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeScrollBar implements ThemeComponent {

    private static String catalogKey = "scrollbar";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeScrollBar());
    }

    public ThemeScrollBar() {
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public int getWidth() {
        return ThemeProvider.getInstance().getSettings().getScrollbarWidth();
    }

    public Color getTrackcolor() {
        return ThemeProvider.getInstance().getBackground().getContrast();
    }

    public Color getThumbcolor() {
        return ThemeProvider.getInstance().getBackground().getScrollbar();
    }

	@Override
	public void configureUI() {
		UIManager.put("ScrollBar.width", this.getWidth());
		UIManager.put("ScrollBar.track", this.getTrackcolor());
		UIManager.put("ScrollBar.thumb", this.getThumbcolor());
		UIManager.put("ScrollBar.thumbDarkShadow", this.getThumbcolor());
		UIManager.put("ScrollBar.thumbHighlight", this.getThumbcolor());
		UIManager.put("ScrollBar.thumbShadow", this.getThumbcolor());
		UIManager.put("ScrollBar.enableArrow", false);
	}
}
