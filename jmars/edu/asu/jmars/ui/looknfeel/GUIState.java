package edu.asu.jmars.ui.looknfeel;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import edu.asu.jmars.util.Config;

public class GUIState {
	

	private static final GUIState manager = new GUIState();

	private GUITheme theme;	
	
	private GUIState() {
	}
	
	public static GUIState getInstance() {
		return manager;
	}

	public GUITheme getTheme() {
		return theme;
	}
	
	public String themeAsString() {
		return theme.asString();
	}	
	
	public void configureUI() {
		try {			
			JDialog.setDefaultLookAndFeelDecorated(true);  //set to false to fix JDialog issue
            JFrame.setDefaultLookAndFeelDecorated(false);
			UIManager.setLookAndFeel("mdlaf.MaterialLookAndFeel");
			applyTheme();			
		} catch (UnsupportedLookAndFeelException e) {
		} catch (ClassNotFoundException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {}	
	}


	private void applyTheme() {
		whichUITheme();
		String uitheme = Config.get(Config.CONFIG_UI_THEME, GUITheme.DARK.asString());
		this.theme = GUITheme.valueOf(uitheme.toUpperCase());
		this.theme.apply();	
	}

	private static void whichUITheme() {
		String futuretheme = Config.get(Config.CONFIG_UI_THEME_FUTURE);
		if (futuretheme != null) {
			Config.set(Config.CONFIG_UI_THEME, futuretheme);
			Config.set(Config.CONFIG_UI_THEME_FUTURE, null);
		}
	}
		
	
}
