package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeRootPane implements ThemeComponent {

    private static String catalogKey = "rootpane";

    static {
        GUITheme.getCatalog().put(catalogKey, new ThemeRootPane());
    }

    public ThemeRootPane() {
    }

    public Color getBackground() {
        return ThemeProvider.getInstance().getBackground().getMain();
    }

    public static String getCatalogKey() {
        return catalogKey;
    }

    public Color getPlaindialogborder() {
        return ThemeProvider.getInstance().getBackground().getSecondaryBorder();
    }

    public Color getInformationdialogborder() {
        return ThemeProvider.getInstance().getBackground().getSecondaryBorder();
    }

    public Color getErrordialogborder() {
        return ThemeProvider.getInstance().getBackground().getSecondaryBorder();
    }

    public Color getFilechooserdialogborder() {
        return ThemeProvider.getInstance().getBackground().getSecondaryBorder();
    }

    public Color getQuestiondialogborder() {
        return ThemeProvider.getInstance().getBackground().getSecondaryBorder();
    }

    public Color getWarningdialogborder() {
        return ThemeProvider.getInstance().getBackground().getSecondaryBorder();
    }

    public Color getColorchooserdialogborder() {
        return ThemeProvider.getInstance().getBackground().getSecondaryBorder();
    }

    @Override
    public void configureUI() {        
        UIManager.put("RootPane.background", this.getBackground());
        UIManager.put("RootPane.plainDialogBorder", BorderFactory.createLineBorder(this.getPlaindialogborder()));
        UIManager.put("RootPane.informationDialogBorder",
                BorderFactory.createLineBorder(this.getInformationdialogborder()));
        UIManager.put("RootPane.errorDialogBorder", BorderFactory.createLineBorder(this.getErrordialogborder()));
        UIManager.put("RootPane.fileChooserDialogBorder",
                BorderFactory.createLineBorder(this.getFilechooserdialogborder()));
        UIManager.put("RootPane.questionDialogBorder",
                BorderFactory.createLineBorder(this.getQuestiondialogborder()));
        UIManager.put("RootPane.warningDialogBorder",
                BorderFactory.createLineBorder(this.getWarningdialogborder()));
        UIManager.put("RootPane.colorChooserDialogBorder",
                BorderFactory.createLineBorder(this.getColorchooserdialogborder()));

    }

}
