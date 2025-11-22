package edu.asu.jmars.ui.looknfeel.theme.component;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.plaf.BorderUIResource;
import org.material.component.swingsnackbar.view.BasicSnackBarUI;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;

public class ThemeSnackBar implements ThemeComponent {

    private static String catalogKey = "snackbar";
    
    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeSnackBar());
    }

    public ThemeSnackBar() {
    }
    
    public static Color getBackgroundStandard() {
        return ThemeProvider.getInstance().getAction().getForeground(); //white
    }  
    
    public static Color getForegroundStandard() {
        return ThemeProvider.getInstance().getText().getSelectionhighlight();  //black
    }   
    
    public static Color getBackgroundError() {
        return ThemeProvider.getInstance().getBackground().getSnackbarBgWarning();
    }    
    
    public static Color getForegroundError() {
        return ThemeProvider.getInstance().getText().getSelectionhighlight();  //black
    }   
    
    public static Color getBackgroundWarning() {
        return ThemeProvider.getInstance().getBackground().getSnackbarBgHighlight();
    } 
    
    public static Color getForegroundWarning() {
        return ThemeProvider.getInstance().getText().getSelectionhighlight();  //black;
    }   
    
    

	@Override
	public void configureUI() {
		UIManager.put("SnackBarUI", BasicSnackBarUI.class.getCanonicalName());
		UIManager.put("SnackBar.arc", 0);
		UIManager.put("SnackBar.background", getBackgroundStandard());
		UIManager.put("SnackBar.foreground", getForegroundStandard());
		UIManager.put("SnackBar.border", new BorderUIResource(BorderFactory.createEmptyBorder(5, 5, 5, 5)));				
	}

}
