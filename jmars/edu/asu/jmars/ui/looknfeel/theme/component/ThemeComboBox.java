package edu.asu.jmars.ui.looknfeel.theme.component;



import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.UIManager;

import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;
import mdlaf.utils.MaterialBorders;
import mdlaf.utils.MaterialImageFactory;
import mdlaf.utils.icons.MaterialIconFont;


public class ThemeComboBox implements ThemeComponent {

	private static String catalogKey = "combobox";

	static {
		GUITheme.getCatalog().put(catalogKey, new ThemeComboBox());
	}

	public ThemeComboBox() {
	}

	public static String getCatalogKey() {
		return catalogKey;
	}

	public Color getSelectedindropdownbackground() {
		return ThemeProvider.getInstance().getBackground().getHighlight();
	}

	public Color getLineborder() {
		return ThemeProvider.getInstance().getAction().getMain();
	}

	public Color getEmptyborder() {
		return ThemeProvider.getInstance().getBlack();
	}

	public Color getButtonborder() {
		return ThemeProvider.getInstance().getBackground().getContrast();
	}

	public Color getBackground() {
		return ThemeProvider.getInstance().getBackground().getContrast();
	}

	public Color getForeground() {
		return ThemeProvider.getInstance().getText().getMain();
	}

	public Color getSelectionbackground() {
		return ThemeProvider.getInstance().getBackground().getContrast();
	}

	public Color getSelectionforeground() {
		return ThemeProvider.getInstance().getText().getMain();
	}

	public Color getDisabledback() {
		return ThemeProvider.getInstance().getBackground().getContrast();
	}

	public Color getDisabledforeground() {
		return ThemeProvider.getInstance().getText().getDisabled();
	}

	public Color getMousehover() {
		return ThemeProvider.getInstance().getBackground().getMain();
	}

	public boolean isEnablehover() {
		return ThemeProvider.getInstance().getSettings().isComboboxEnableHover() ;
	}

	public Color getUnfocus() {
		return ThemeProvider.getInstance().getAction().getImageOn();
	}

	public Color getFocus() {
		return ThemeProvider.getInstance().getAction().getMain();
	}

	public Color getButtonbackground() {
		return ThemeProvider.getInstance().getBackground().getContrast();
	}

	public Color getBorder() {
		return ThemeProvider.getInstance().getBackground().getSecondaryBorder();
	}
	
	public Color getItemSelectionforeground() {
		return ThemeProvider.getInstance().getSelection().getForegroundContrast();
	}

	@Override
	public void configureUI() {	    
		UIManager.put("ComboBox.font", ThemeFont.getRegular());
		UIManager.put("ComboBox.selectedInDropDownBackground", this.getSelectedindropdownbackground());
		UIManager.put("ComboBox.background", this.getBackground());
		UIManager.put("ComboBox.foreground", this.getForeground());				
		UIManager.put("ComboBox.selectionBackground", this.getSelectedindropdownbackground());
		UIManager.put("ComboBox.selectionForeground", this.getItemSelectionforeground());				
		UIManager.put("ComboBox.disabledBackground", this.getDisabledback());
		UIManager.put("ComboBox.disabledForeground", this.getDisabledforeground());
		UIManager.put("ComboBox.mouseHoverColor", this.getMousehover());
		UIManager.put("ComboBox.mouseHoverEnabled", this.isEnablehover());
		UIManager.put("ComboBox.unfocusColor", this.getUnfocus());
		UIManager.put("ComboBox.focusColor", this.getFocus());
		UIManager.put("ComboBox[button].border", BorderFactory.createLineBorder(this.getButtonborder()));
		UIManager.put("ComboBox.buttonBackground", this.getButtonbackground());		
		UIManager.put("ComboBox.borderItems", BorderFactory.createEmptyBorder(2, 2, 2, 2));
		UIManager.put("ComboBox[item].selectionForeground", this.getItemSelectionforeground());
		UIManager.put("ComboBox.border", MaterialBorders.roundedLineColorBorder(this.getBorder(), 0));		
		UIManager.put("ComboBox.arc", 0);
		UIManager.put("ComboBox.buttonIcon", MaterialImageFactory.getInstance().getImage(MaterialIconFont.ARROW_DROP_DOWN,   getSelectionforeground()));				                            
		UIManager.put("ComboBox.buttonDisabledIcon", MaterialImageFactory.getInstance().getImage(MaterialIconFont.ARROW_DROP_DOWN, getDisabledforeground()));											 
		UIManager.put("ComboBox.buttonSelectIcon", MaterialImageFactory.getInstance().getImage(MaterialIconFont.ARROW_DROP_DOWN,getFocus()));  				                             
	}

}
