package edu.asu.jmars.ui.looknfeel.theme.component;

import java.awt.Color;
import javax.swing.UIManager;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;
import mdlaf.utils.MaterialImageFactory;
import mdlaf.utils.icons.MaterialIconFont;


public class ThemeFileChooser implements ThemeComponent {

    private static String catalogKey = "filechooser";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeFileChooser());
    }

    public ThemeFileChooser() {
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getListimgcolor() {
        return ThemeProvider.getInstance().getImage().getFill();
    }

    public Color getDetailsimgcolor() {
        return ThemeProvider.getInstance().getText().getMain();
    }

    public int getImgscaledwidth() {
        return ThemeProvider.getInstance().getSettings().getFilechooserImgScaledWidth();
    }

    public int getImgscaledheight() {
        return ThemeProvider.getInstance().getSettings().getFilechooserImgScaledHeight();
    }

    @Override
    public void configureUI() {        
        UIManager.put("FileChooser[icons].computer", MaterialImageFactory.getInstance().getImage(MaterialIconFont.COMPUTER, getDetailsimgcolor()));
        UIManager.put("FileChooser[icons].file", MaterialImageFactory.getInstance().getImage(MaterialIconFont.INSERT_DRIVE_FILE, getDetailsimgcolor()));
        UIManager.put("FileChooser[icons].home", MaterialImageFactory.getInstance().getImage(MaterialIconFont.HOME, getDetailsimgcolor()));
        UIManager.put("FileChooser[icons].directory", MaterialImageFactory.getInstance().getImage(MaterialIconFont.FOLDER, getDetailsimgcolor()));
        UIManager.put("FileChooser[icons].floppyDrive", MaterialImageFactory.getInstance().getImage(MaterialIconFont.INSERT_DRIVE_FILE, getDetailsimgcolor()));
        UIManager.put("FileChooser[icons].hardDrive", MaterialImageFactory.getInstance().getImage(MaterialIconFont.DRIVE_ETA, getDetailsimgcolor()));        
        UIManager.put("FileChooser[icons].list", MaterialImageFactory.getInstance().getImage(MaterialIconFont.LIST, getDetailsimgcolor()));
        UIManager.put("FileChooser[icons].details", MaterialImageFactory.getInstance().getImage(MaterialIconFont.DETAILS, getDetailsimgcolor()));
        UIManager.put("FileChooser[icons].newFolder", MaterialImageFactory.getInstance().getImage(MaterialIconFont.CREATE_NEW_FOLDER, getDetailsimgcolor()));
        UIManager.put("FileChooser[icons].upFolder", MaterialImageFactory.getInstance().getImage(MaterialIconFont.ARROW_BACK, getDetailsimgcolor()));     
     }
}
