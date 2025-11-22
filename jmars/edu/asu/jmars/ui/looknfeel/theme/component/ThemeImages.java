package edu.asu.jmars.ui.looknfeel.theme.component;


import java.awt.Color;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public class ThemeImages implements ThemeComponent {

	private static String catalogKey = "images";

	static {
		GUITheme.getCatalog().put(catalogKey, new ThemeImages());
	}

	public ThemeImages() {
	}

	public static String getCatalogKey() {
		return catalogKey;
	}

	public Color getFill() {
		return ThemeProvider.getInstance().getImage().getFill();
	}
	
	public Color getFill2() {
		return ThemeProvider.getInstance().getImage().getFill2();
	}	

	public Color getSelectedfill() {
		return ThemeProvider.getInstance().getImage().getSelectionFill();
	}

	public Color getLayerfill() {
		return ThemeProvider.getInstance().getImage().getLayer();
	}

	public Color getLinkfill() {
		return ThemeProvider.getInstance().getImage().getLink();
	}
	
	public Color getLayerloaded()
	{
		return ThemeProvider.getInstance().getImage().getLayerLoaded();
	}
	
	public Color getLayerloading()
	{
		return ThemeProvider.getInstance().getImage().getLayerLoading();
	}
	
	public Color getLayerOff()
	{
		return ThemeProvider.getInstance().getImage().getLayeroff();
	}
	
	public Color getLayerhover()
	{
		return ThemeProvider.getInstance().getBackground().getHighlight();
	}
	
	public Color getIconInactive()
	{
		return ThemeProvider.getInstance().getImage().getIconInactive();
	}

	@Override
	public void configureUI() {	   
	}

	public Color getCommonFill() {
		return ThemeProvider.getInstance().getImage().getCommonFill();
	}
}
