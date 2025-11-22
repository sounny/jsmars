package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;

public class ThemeTextPane implements ThemeComponent {

	private static String catalogKey = "textpane";

	static {
		GUITheme.getCatalog().put(catalogKey, new ThemeTextPane());
	}

	public static String getCatalogKey() {
		return catalogKey;
	}

	public Color getSelectionbackground() {
		return ThemeProvider.getInstance().getBackground().getHighlightContrast();
	}

	public Color getSelectionforeground() {
		return ThemeProvider.getInstance().getSelection().getForegroundContrast();
	}

	public Color getForeground() {
		return ThemeProvider.getInstance().getText().getMain();
	}

	public Color getBackground() {
		return ThemeProvider.getInstance().getBackground().getContrast();
	}

	public Color getCaretforeground() {
		return ThemeProvider.getInstance().getText().getMain();
	}

	public Color getInactiveforeground() {
		return ThemeProvider.getInstance().getText().getMain();
	}

	public ThemeTextPane() {
	}

	@Override
	public void configureUI() {
		GUITheme.getCatalog().put(catalogKey, this);
		UIManager.put("TextPane.font", ThemeFont.getBold());
		UIManager.put("TextPane.foreground", this.getForeground());
		UIManager.put("TextPane.border", BorderFactory.createEmptyBorder());
		UIManager.put("TextPane.background", this.getBackground());
		UIManager.put("TextPane.caretForeground", this.getCaretforeground());
		UIManager.put("TextPane.selectionForeground", this.getSelectionforeground());
		UIManager.put("TextPane.inactiveForeground", this.getInactiveforeground());
		UIManager.put("TextPane.selectionBackground", this.getSelectionbackground());
	}

}
