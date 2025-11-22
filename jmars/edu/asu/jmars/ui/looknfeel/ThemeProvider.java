package edu.asu.jmars.ui.looknfeel;

import java.awt.Color;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.plaf.ColorUIResource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import edu.asu.jmars.Main;
import edu.asu.jmars.util.DebugLog;

@XmlRootElement(name = "theme")
@XmlAccessorType(XmlAccessType.FIELD)
public class ThemeProvider {
	// Default theme values 
	private static final String DEFAULT_BG_MAIN = "rgb(45,48,56)";
	private static final String DEFAULT_BG_CONTRAST = "rgb(81, 86, 101)";
	private static final String DEFAULT_BG_HIGHLIGHT = "rgb(249, 192, 98)";
	private static final String DEFAULT_BG_SNACKBAR_HIGHLIGHT = "rgb(249, 192, 98)";
	private static final String DEFAULT_BG_SNACKBAR_WARNING = "rgb(249, 98, 98)";	
	private static final String DEFAULT_BG_MID_CONTRAST = "rgb(62, 67, 78)";
	private static final String DEFAULT_BG_HL_CONTRAST = "rgb(225, 156, 43)";
	private static final String DEFAULT_BG_ALT_CONTRAST = "rgb(119, 119, 119)";
	private static final String DEFAULT_BG_ALT_CONTRAST_BRIGHT = "rgb(241, 125, 125)";
	private static final String DEFAULT_BG_ALT_CONTRAST_SOFT = "rgb(150, 244, 224)";
	private static final String DEFAULT_BG_SCROLLBAR = "rgb(155,155,155)";
	private static final String DEFAULT_BG_PROGRESSBAR = "rgb(67,178,175)";
	private static final String DEFAULT_BG_BORDER = "rgb(59, 62, 69)";
	private static final String DEFAULT_BG_SECONDARY_BORDER = "rgb(255,255,255)";
	private static final String DEFAULT_ACTION_MAIN =  "rgb(66,179,176)";
	private static final String DEFAULT_ACTION_CONTRAST = "rgb(23, 137, 134)";
	private static final String DEFAULT_DEFAULT_ACTION_CONTRAST = "rgb(23, 137, 134)";
	private static final String DEFAULT_ACTION_ALT_CONTRAST = "rgb(222, 222, 222)";
	private static final String DEFAULT_ACTION_DISABLED = "rgb(170,170,170)";
	private static final String DEFAULT_ACTION_FOREGROUND = "rgb(255,255,255)";
	private static final String DEFAULT_DEFAULT_ACTION_FOREGROUND = "rgb(255,255,255)";
	private static final String DEFAULT_ACTION_DISABLED_FOREGROUND = "rgb(45, 127, 125)";
	private static final String DEFAULT_ACTION_BORDER = "rgb(119, 119, 119)";
	private static final String DEFAULT_ACTION_IMAGE_ON = "rgb(255,255,255)";
	private static final String DEFAULT_ACTION_IMAGE_OFF = "rgb(255,255,255)";
	private static final String DEFAULT_ACTION_TOGGLE_ON = "rgb(66,179,176)";
	private static final String DEFAULT_ACTION_TOGGLE_OFF = "rgb(255,255,255)";
	private static final String DEFAULT_ACTION_LINE_ACTIVE = "rgb(249,192,98)";
	private static final String DEFAULT_ACTION_LINE_INACTIVE = "rgb(255,255,255)";
	private static final String DEFAULT_TEXT_MAIN = "rgb(255,255,255)";
	private static final String DEFAULT_TEXT_DISABLED = "rgb(170,170,170)";
	private static final String DEFAULT_TEXT_GRAYED = "rgb(169, 172, 183)";
	private static final String DEFAULT_TEXT_LINK = "rgb(251,193,91)";
	private static final String DEFAULT_TEXT_HIGHLIGHT = "rgb(249, 192, 98)";
	private static final String DEFAULT_TEXT_SELECTION_HIGHLIGHT = "rgb(0,0,0)";
	private static final String DEFAULT_NULL_TEXT = "rgb(155,155,155)";
	private static final String DEFAULT_DATE_AS_LINK = "rgb(150,244,224)";
	private static final String DEFAULT_SELECTION_MAIN = "rgb(126, 132, 153)";
	private static final String DEFAULT_SELECTION_FOREGROUND = "rgb(255,255,255)";
	private static final String DEFAULT_SELECTION_FOREGROUND_CONTRAST = "rgb(0,0,0)";
	private static final String DEFAULT_ROW_ALT = "rgb(59,62,69)";
	private static final String DEFAULT_ROW_ALT_BACK = "rgb(120,144,156)";
	private static final String DEFAULT_ROW_BACKGROUND = "rgb(38,50,56)";
	private static final String DEFAULT_ROW_BORDER = "rgb(151,151,151)";
	private static final String DEFAULT_ROW_HORIZ_GRID = "rgb(119,119,119)";
	private static final String DEFAULT_ROW_VERTICAL_GRID = "rgb(151,151,151)";
	private static final String DEFAULT_IMAGE_FILL = "rgb(255,255,255)";
	private static final String DEFAULT_COMMON_IMAGE_FILL = "rgb(255,255,255)";
	private static final String DEFAULT_IMAGE_SELECTION_FILL = "rgb(249,192,98)";
	private static final String DEFAULT_IMAGE_LAYER = "rgb(255,255,255)";
	private static final String DEFAULT_IMAGE_LINK = "rgb(66, 179, 176)";
	private static final String DEFAULT_IMAGE_ICON_INACTIVE = "rgb(21, 53, 71)";
	private static final String DEFAULT_CHART_INDICATOR = "rgb(74,144,226)";
	private static final boolean DEFAULT_BUTTON_ENABLE_HOVER = true;
    private static final boolean DEFAULT_BUTTON_DEFAULT_FOLLOWS_FOCUS = true;
    private static final boolean DEFAULT_BUTTON_FOCUSABLE = true;
    private static final boolean DEFAULT_TREE_PAINT_LINES = false;
    private static final boolean DEFAULT_TABLE_FOCUSABLE = true;
    private static final boolean DEFAULT_TABLE_ALTERNATE_ROW_COLOR = true;
    private static final boolean DEFAULT_COMBOBOX_ENABLE_HOVER = false;
    private static final int DEFAULT_BUTTON_ARC_SIZE = 12;
    private static final int DEFAULT_TABLE_ROW_HEIGHT = 28;
    private static final int DEFAULT_SCROLLBAR_WIDTH = 10;
    private static final int DEFAULT_PROGRESSBAR_BORDER_THICKNESS = 2;
    private static final int DEFAULT_MENUBAR_ITEM_SPACING = 35;
    private static final int DEFAULT_MENUBAR_ITEM_PADDING = 18;    
    private static final int DEFAULT_FILECHOOSER_IMG_SCALED_WIDTH = 27;
    private static final int DEFAULT_FILECHOOSER_IMG_SCALED_HEIGHT = 16;
    private static final int DEFAULT_TAB_INDENT = 4;
    private static final int DEFAULT_TAB_SPACE = 20;
	private static final String SETTINGS_BUTTON_ENABLE_HOVER = "true";
	private static final String SETTINGS_BUTTON_DEFAULT_FOLLOWS_FOCUS = "true";
	private static final String SETTINGS_BUTTON_FOCUSABLE = "true";
	private static final String SETTINGS_TREE_PAINT_LINES = "false";
	private static final String SETTINGS_TABLE_FOCUSABLE = "true";
	private static final String SETTINGS_TABLE_ALTERNATE_ROW_COLOR = "true";
	private static final String SETTINGS_COMBOBOX_ENABLE_HOVER = "false";
	private static final String SETTINGS_BUTTON_ARC_SIZE = "12";
	private static final String SETTINGS_TABLE_ROW_HEIGHT = "28";
	private static final String SETTINGS_SCROLLBAR_WIDTH = "10";
	private static final String SETTINGS_PROGRESSBAR_BORDER_THICKNESS = "2";
	private static final String SETTINGS_MENUBAR_ITEM_SPACING = "35";
	private static final String SETTINGS_MENUBAR_ITEM_PADDING = "18";
	private static final String SETTINGS_FILECHOOSER_IMG_SCALED_WIDTH = "27";
	private static final String SETTINGS_FILECHOOSER_IMG_SCALED_HEIGHT = "16";
	private static final String SETTINGS_TAB_INDENT = "4";
	private static final String SETTINGS_TAB_SPACE = "20";
	public static final String DEFAULT_IMAGE_LAYER_INDICATOR_LOADED = "rgb(150,244,224)";
	public static final String DEFAULT_IMAGE_LAYER_INDICATOR_LOADING = "rgb(241,125,125)";
	public static final String DEFAULT_IMAGE_LAYER_INDICATOR_OFF = "rgb(133,133,133)";
	public static final String DEFAULT_IMAGE_FILL2 = "rgb(203,203,203)";

	private static DebugLog log = DebugLog.instance();
	private static ThemeProvider themeProvider = new ThemeProvider();

	@XmlElement(name = "background")
	private Background background = new Background();

	@XmlElement(name = "action")
	private Action action = new Action();

	@XmlElement(name = "text")
	private Text text = new Text();

	@XmlElement(name = "selection")
	private Selection selection = new Selection();

	@XmlElement(name = "row")
	private Row row = new Row();

	@XmlElement(name = "image")
	private Image image = new Image();

	@XmlElement(name = "settings")
	private Settings settings = new Settings();	
	

	public Color getBlack() {
		return parse("rgb(0,0,0)");
	}

	public Background getBackground() {
		background = (background == null ? new Background(): background);
		return background;
	}

	public Action getAction() {
		action = (action == null ? new Action() : action);
		return action;
	}

	public Text getText() {
		text = (text == null ? new Text() : text);
		return text;
	}

	public Selection getSelection() {
		selection = (selection == null ? new Selection() : selection);
		return selection;
	}

	public Row getRow() {
	    row = (row == null ? new Row() : row);
	    return row;
	}

	public Image getImage() {
		image = (image == null ? new Image() : image);
		return image;
	}

	public Settings getSettings() {
		settings = (settings == null ? new Settings() : settings);
		return settings;
	}

	public static ThemeProvider getInstance() {
		if (themeProvider == null) {
			themeProvider = new ThemeProvider();
		}
		return themeProvider;
	}
	
	public static void createTheme(String theme) {		
		themeProvider.setTheme(theme);
	}

	private void setTheme (String themeInput) {	    
	    JAXBContext jaxbContext;

		try {
			jaxbContext = JAXBContext.newInstance(ThemeProvider.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			InputStream theme = Main.getResourceAsStream(themeInput);
			themeProvider = (ThemeProvider) unmarshaller.unmarshal(theme);	
		} catch (Exception e) {
			e.printStackTrace();
			log.aprintln(
					"Exception thrown while unmarshalling theme xml: " + (themeInput == null ? "null" : themeInput));
			log.aprintln("Theme will use default values");
			themeProvider = new ThemeProvider();
		}	
	}
	
	private ThemeProvider() {
	}
	

	@XmlRootElement(name = "background")
	@XmlAccessorType(XmlAccessType.FIELD)
	public final static class Background {

		public Background() {
		}

		@XmlElement(name = "main", defaultValue=DEFAULT_BG_MAIN)
		private String main = DEFAULT_BG_MAIN;

		@XmlElement(name = "contrast", defaultValue=DEFAULT_BG_CONTRAST)
		private String contrast = DEFAULT_BG_CONTRAST;

		@XmlElement(name="mid_contrast", defaultValue=DEFAULT_BG_MID_CONTRAST)
		private String midContrast = DEFAULT_BG_MID_CONTRAST;
		
		@XmlElement(name = "highlight", defaultValue=DEFAULT_BG_HIGHLIGHT)
		private String highlight = DEFAULT_BG_HIGHLIGHT;
		
		@XmlElement(name = "snackbar_highlight", defaultValue=DEFAULT_BG_SNACKBAR_HIGHLIGHT)
		private String snackbarHighlight = DEFAULT_BG_SNACKBAR_HIGHLIGHT;
		
		@XmlElement(name = "snackbar_warning", defaultValue=DEFAULT_BG_SNACKBAR_WARNING)
		private String snackbarWarning = DEFAULT_BG_SNACKBAR_WARNING;
		
		@XmlElement(name = "highlight_contrast", defaultValue=DEFAULT_BG_HL_CONTRAST)
		private String highlightContrast = DEFAULT_BG_HL_CONTRAST;

		@XmlElement(name = "alternate_contrast", defaultValue=DEFAULT_BG_ALT_CONTRAST)
		private String alternateContrast = DEFAULT_BG_ALT_CONTRAST;

		@XmlElement(name = "alternate_contrast_bright", defaultValue=DEFAULT_BG_ALT_CONTRAST_BRIGHT)
		private String alternateContrastBright = DEFAULT_BG_ALT_CONTRAST_BRIGHT;

		@XmlElement(name = "alternate_contrast_soft", defaultValue=DEFAULT_BG_ALT_CONTRAST_SOFT)
		private String alternateContrastSoft = DEFAULT_BG_ALT_CONTRAST_SOFT;

		@XmlElement(name = "scrollbar", defaultValue=DEFAULT_BG_SCROLLBAR)
		private String scrollbar = DEFAULT_BG_SCROLLBAR;

		@XmlElement(name = "progressbar", defaultValue=DEFAULT_BG_PROGRESSBAR)
		private String progressbar = DEFAULT_BG_PROGRESSBAR;

		@XmlElement(name = "border", defaultValue=DEFAULT_BG_BORDER)
		private String border = DEFAULT_BG_BORDER;

		@XmlElement(name = "secondary_border", defaultValue=DEFAULT_BG_SECONDARY_BORDER)
		private String secondaryBorder = DEFAULT_BG_SECONDARY_BORDER;

		public Color getMain() {
			main = ( main == null ? DEFAULT_BG_MAIN : main);
			return parse(main);			
		}

		public Color getContrast() {
		    contrast = (contrast == null ? DEFAULT_BG_CONTRAST : contrast);
		    return parse(contrast);
		}
		
		public Color getMidContrast() {
			midContrast = (midContrast == null ? DEFAULT_BG_MID_CONTRAST : midContrast);
			return parse(midContrast);
		}

		public Color getHighlight() {
		    highlight = (highlight == null ? DEFAULT_BG_HIGHLIGHT : highlight);
		    return parse(highlight);
		}
		
		public Color getSnackbarBgHighlight() {
		    snackbarHighlight = (snackbarHighlight == null ? DEFAULT_BG_SNACKBAR_HIGHLIGHT : snackbarHighlight);
		    return parse(snackbarHighlight);
		}
		
		public Color getSnackbarBgWarning() {
		    snackbarWarning = (snackbarWarning == null ? DEFAULT_BG_SNACKBAR_WARNING : snackbarWarning);
		    return parse(snackbarWarning);
		}

		public Color getHighlightContrast() {
		    highlightContrast = (highlightContrast == null ? DEFAULT_BG_HL_CONTRAST : highlightContrast);
		    return parse(highlightContrast);
		}

		public Color getAlternateContrast() {
		    alternateContrast = (alternateContrast == null ? DEFAULT_BG_ALT_CONTRAST : alternateContrast);
		    return parse(alternateContrast);
		}

		public Color getAlternateContrastBright() {
		    alternateContrastBright = (alternateContrastBright == null ? DEFAULT_BG_ALT_CONTRAST_BRIGHT : alternateContrastBright);
		    return parse(alternateContrastBright);
		}

		public Color getAlternateContrastSoft() {
		    alternateContrastSoft = (alternateContrastSoft == null ? DEFAULT_BG_ALT_CONTRAST_SOFT : alternateContrastSoft);
		    return parse(alternateContrastSoft);
		}

		public Color getScrollbar() {
		    scrollbar = (scrollbar == null ? DEFAULT_BG_SCROLLBAR : scrollbar);
		    return parse(scrollbar);
		}

		public Color getProgressbar() {
		    progressbar = (progressbar == null ? DEFAULT_BG_PROGRESSBAR : progressbar);
		    return parse(progressbar);
		}

		public Color getBorder() {
		    border = (border == null ? DEFAULT_BG_BORDER : border);
		    return parse(border);
		}

		public Color getSecondaryBorder() {
		    secondaryBorder = (secondaryBorder == null ? DEFAULT_BG_SECONDARY_BORDER : secondaryBorder);
		    return parse(secondaryBorder);
		}
	}

	@XmlRootElement(name = "action")
	@XmlAccessorType(XmlAccessType.FIELD)
	public final static class Action {
		public Action() {
		}

		@XmlElement(name = "main", defaultValue=DEFAULT_ACTION_MAIN)
		private String main = DEFAULT_ACTION_MAIN;		

		@XmlElement(name = "contrast",  defaultValue=DEFAULT_ACTION_CONTRAST)
		private String contrast = DEFAULT_ACTION_CONTRAST;
		
		@XmlElement(name = "default_contrast",  defaultValue=DEFAULT_DEFAULT_ACTION_CONTRAST)
		private String defaultContrast = DEFAULT_DEFAULT_ACTION_CONTRAST;
		
		@XmlElement(name = "alt_contrast",  defaultValue=DEFAULT_ACTION_ALT_CONTRAST)
		private String altcontrast = DEFAULT_ACTION_ALT_CONTRAST;

		@XmlElement(name = "disabled", defaultValue=DEFAULT_ACTION_DISABLED)
		private String disabled = DEFAULT_ACTION_DISABLED;

		@XmlElement(name = "foreground", defaultValue=DEFAULT_ACTION_FOREGROUND)
		private String foreground = DEFAULT_ACTION_FOREGROUND;
		
		@XmlElement(name = "default_foreground", defaultValue=DEFAULT_DEFAULT_ACTION_FOREGROUND)
		private String defaultForeground = DEFAULT_DEFAULT_ACTION_FOREGROUND;

		@XmlElement(name = "disabled_foreground", defaultValue=DEFAULT_ACTION_DISABLED_FOREGROUND)
		private String disabledForeground = DEFAULT_ACTION_DISABLED_FOREGROUND;

		@XmlElement(name = "border", defaultValue=DEFAULT_ACTION_BORDER)
		private String border = DEFAULT_ACTION_BORDER;

		@XmlElement(name = "image_on", defaultValue=DEFAULT_ACTION_IMAGE_ON)
		private String imageOn = DEFAULT_ACTION_IMAGE_ON;

		@XmlElement(name = "image_off", defaultValue=DEFAULT_ACTION_IMAGE_OFF)
		private String imageOff = DEFAULT_ACTION_IMAGE_OFF;

		@XmlElement(name = "toggle_on", defaultValue=DEFAULT_ACTION_TOGGLE_ON)
		private String toggleOn = DEFAULT_ACTION_TOGGLE_ON;

		@XmlElement(name = "toggle_off", defaultValue=DEFAULT_ACTION_TOGGLE_OFF)
		private String toggleOff = DEFAULT_ACTION_TOGGLE_OFF;

		@XmlElement(name = "line_active", defaultValue=DEFAULT_ACTION_LINE_ACTIVE)
		private String lineActive = DEFAULT_ACTION_LINE_ACTIVE;

		@XmlElement(name = "line_inactive", defaultValue=DEFAULT_ACTION_LINE_INACTIVE)
		private String lineInactive = DEFAULT_ACTION_LINE_INACTIVE;		
		
		@XmlElement(name = "chart_indicator", defaultValue=DEFAULT_CHART_INDICATOR)
		private String chartIndicator = DEFAULT_CHART_INDICATOR;
		
		@XmlElement(name = "profile_chart_indicator", defaultValue=DEFAULT_CHART_INDICATOR)
		private String profilechartIndicator = DEFAULT_CHART_INDICATOR;

		public Color getMain() {
			main = (main == null ? DEFAULT_ACTION_MAIN : main);
			return parse(main);
		}

		public Color getContrast() {
			contrast = (contrast == null ? DEFAULT_ACTION_CONTRAST : contrast);
			return parse(contrast);
		}
	
		public Color getDefaultContrast() {
			defaultContrast = (defaultContrast == null ? DEFAULT_DEFAULT_ACTION_CONTRAST : defaultContrast);
			return parse(defaultContrast);
		}
		
		public Color getAltContrast() {
			altcontrast = (altcontrast == null ? DEFAULT_ACTION_ALT_CONTRAST : altcontrast);
			return parse(altcontrast);
		}		

		public Color getDisabled() {
			disabled = (disabled == null ? DEFAULT_ACTION_DISABLED : disabled);
			return parse(disabled);
		}

		public Color getForeground() {
			foreground = (foreground == null ? DEFAULT_ACTION_FOREGROUND : foreground);
			return parse(foreground);
		}
		
		public Color getDefaultForeground() {
			defaultForeground = (defaultForeground == null ? DEFAULT_DEFAULT_ACTION_FOREGROUND : defaultForeground);
			return parse(defaultForeground);
		}		

		public Color getDisabledForeground() {
			disabledForeground = (disabledForeground == null ? DEFAULT_ACTION_DISABLED_FOREGROUND : disabledForeground);
			return parse(disabledForeground);
		}

		public Color getBorder() {
			border = (border == null ? DEFAULT_ACTION_BORDER : border);
			return parse(border);
		}

		public Color getImageOn() {
			imageOn = (imageOn == null ? DEFAULT_ACTION_IMAGE_ON : imageOn);
			return parse(imageOn);
		}

		public Color getImageOff() {
			imageOff = (imageOff == null ? DEFAULT_ACTION_IMAGE_OFF : imageOff);
			return parse(imageOff);
		}

		public Color getToggleOn() {
			toggleOn = (toggleOn == null ? DEFAULT_ACTION_TOGGLE_ON : toggleOn);
			return parse(toggleOn);
		}

		public Color getToggleOff() {
			toggleOff = (toggleOff == null ? DEFAULT_ACTION_TOGGLE_OFF : toggleOff);
			return parse(toggleOff);
		}

		public Color getLineActive() {
			lineActive = (lineActive == null ? DEFAULT_ACTION_LINE_ACTIVE : lineActive);
			return parse(lineActive);
		}

		public Color getLineInactive() {
			lineInactive = (lineInactive == null ? DEFAULT_ACTION_LINE_INACTIVE : lineInactive);
			return parse(lineInactive);
		}
		
		public Color getChartIndicator() {
			chartIndicator = (chartIndicator == null ? DEFAULT_CHART_INDICATOR : chartIndicator);
			return parse(chartIndicator);
		}
		
		public Color getProfileChartIndicator() {
			profilechartIndicator = (profilechartIndicator == null ? DEFAULT_CHART_INDICATOR : profilechartIndicator);
			return parse(profilechartIndicator);
		}
		
	}

	@XmlRootElement(name = "text")
	@XmlAccessorType(XmlAccessType.FIELD)
	public final static class Text {
		public Text() {
		}

		@XmlElement(name = "main", defaultValue = DEFAULT_TEXT_MAIN)
		private String main = DEFAULT_TEXT_MAIN;

		@XmlElement(name = "disabled", defaultValue = DEFAULT_TEXT_DISABLED)
		private String disabled = DEFAULT_TEXT_DISABLED;
		
		@XmlElement(name = "grayed", defaultValue = DEFAULT_TEXT_GRAYED)
		private String grayed = DEFAULT_TEXT_GRAYED;

		@XmlElement(name = "link", defaultValue = DEFAULT_TEXT_LINK)
		private String link = DEFAULT_TEXT_LINK;

		@XmlElement(name = "highlight", defaultValue = DEFAULT_TEXT_HIGHLIGHT)
		private String highlight = DEFAULT_TEXT_HIGHLIGHT;
		
		@XmlElement(name = "selection_highlight", defaultValue = DEFAULT_TEXT_SELECTION_HIGHLIGHT)
		private String selhighlight = DEFAULT_TEXT_SELECTION_HIGHLIGHT;
		
		@XmlElement(name = "null_text", defaultValue = DEFAULT_NULL_TEXT)
		private String nulltext = DEFAULT_NULL_TEXT;
		
		@XmlElement(name = "date_as_link", defaultValue = DEFAULT_DATE_AS_LINK)
		private String dateaslink = DEFAULT_DATE_AS_LINK;
		

		public Color getMain() {
			main = (main == null ? DEFAULT_TEXT_MAIN : main);
			return parse(main);
		}

		public Color getDisabled() {
			disabled = (disabled == null ? DEFAULT_TEXT_DISABLED : disabled);
			return parse(disabled);
		}
		
		public Color getGrayed() {
			grayed = (grayed == null ? DEFAULT_TEXT_GRAYED : grayed);
			return parse(grayed);
		}	

		public Color getLink() {
			link = (link == null ? DEFAULT_TEXT_LINK : link);
			return parse(link);
		}

		public Color getHighlight() {
			highlight = (highlight == null ? DEFAULT_TEXT_HIGHLIGHT : highlight);
			return parse(highlight);
		}
		
		public Color getSelectionhighlight() {
			selhighlight = (selhighlight == null ? DEFAULT_TEXT_SELECTION_HIGHLIGHT : selhighlight);
			return parse(selhighlight);
		}
		
		public Color getNullText() {
			nulltext = (nulltext == null ? DEFAULT_NULL_TEXT : nulltext);
			return parse(nulltext);
		}
		
		public Color getDateAsLink() {
			dateaslink = (dateaslink == null ? DEFAULT_DATE_AS_LINK : dateaslink);
			return parse(dateaslink);
		}
		
	}

	@XmlRootElement(name = "selection")
	@XmlAccessorType(XmlAccessType.FIELD)
	public final static class Selection {
		public Selection() {
		}

		@XmlElement(name = "main", defaultValue = DEFAULT_SELECTION_MAIN)
		private String main = DEFAULT_SELECTION_MAIN;

		@XmlElement(name = "foreground", defaultValue = DEFAULT_SELECTION_FOREGROUND)
		private String foreground = DEFAULT_SELECTION_FOREGROUND;

		@XmlElement(name = "foreground_contrast", defaultValue = DEFAULT_SELECTION_FOREGROUND_CONTRAST)
		private String foregroundContrast = DEFAULT_SELECTION_FOREGROUND_CONTRAST;

		public Color getMain() {
			main = (main == null ? DEFAULT_SELECTION_MAIN : main);
			return parse(main);
		}

		public Color getForeground() {
			foreground = (foreground == null ? DEFAULT_SELECTION_FOREGROUND : foreground);
			return parse(foreground);
		}

		public Color getForegroundContrast() {
			foregroundContrast = (foregroundContrast == null ? DEFAULT_SELECTION_FOREGROUND_CONTRAST : foregroundContrast);
			return parse(foregroundContrast);
		}
	}

	@XmlRootElement(name = "row")
	@XmlAccessorType(XmlAccessType.FIELD)
	public final static class Row {
		public Row() {
		}

		@XmlElement(name = "alternate", defaultValue = DEFAULT_ROW_ALT)
		private String alternate = DEFAULT_ROW_ALT;
		
		@XmlElement(name = "background", defaultValue = DEFAULT_ROW_BACKGROUND)
		private String background = DEFAULT_ROW_BACKGROUND;
		
		@XmlElement(name = "alternate_back", defaultValue = DEFAULT_ROW_ALT_BACK)
		private String alternate_back = DEFAULT_ROW_ALT_BACK;

		@XmlElement(name = "border", defaultValue = DEFAULT_ROW_BORDER)
		private String border = DEFAULT_ROW_BORDER;
		
		@XmlElement(name = "grid_horiz", defaultValue = DEFAULT_ROW_HORIZ_GRID)
		private String grid_horiz = DEFAULT_ROW_HORIZ_GRID;
		
		@XmlElement(name = "grid_vertical", defaultValue = DEFAULT_ROW_VERTICAL_GRID)
		private String grid_vertical = DEFAULT_ROW_VERTICAL_GRID;

		public Color getAlternate() {
			alternate = (alternate == null ? DEFAULT_ROW_ALT : alternate);
			return parse(alternate);
		}
		
		public Color getAlternateback() {
			alternate_back = (alternate_back == null ? DEFAULT_ROW_ALT_BACK : alternate_back);
			return parse(alternate_back);
		}
		

		public Color getBackground() {
			background = (background == null ? DEFAULT_ROW_BACKGROUND : background);
			return parse(background);
		}
		
		public Color getBorder() {
		    border = (border == null ? DEFAULT_ROW_BORDER : border);
			return parse(border);
		}
		
		public Color getHorizgrid() {
			grid_horiz = (grid_horiz == null ? DEFAULT_ROW_HORIZ_GRID : grid_horiz);
			return parse(grid_horiz);			
		}
		
		public Color getVertgrid() {
			grid_vertical = (grid_vertical == null ? DEFAULT_ROW_VERTICAL_GRID : grid_vertical);
			return parse(grid_vertical);
		}
	}

	@XmlRootElement(name = "image")
	@XmlAccessorType(XmlAccessType.FIELD)
	public final static class Image {
		public Image() {
		}

		@XmlElement(name = "fill", defaultValue = DEFAULT_IMAGE_FILL)
		private String fill = DEFAULT_IMAGE_FILL;
		
		@XmlElement(name = "common_fill", defaultValue = DEFAULT_COMMON_IMAGE_FILL)
		private String commonFill = DEFAULT_COMMON_IMAGE_FILL;

		@XmlElement(name = "selection_fill", defaultValue = DEFAULT_IMAGE_SELECTION_FILL)
		private String selectionFill = DEFAULT_IMAGE_SELECTION_FILL;

		@XmlElement(name = "layer", defaultValue = DEFAULT_IMAGE_LAYER)
		private String layer = DEFAULT_IMAGE_LAYER;

		@XmlElement(name = "link", defaultValue = DEFAULT_IMAGE_LINK)
		private String link = DEFAULT_IMAGE_LINK;
		
		@XmlElement(name = "layer_indicator_loaded", defaultValue = DEFAULT_IMAGE_LAYER_INDICATOR_LOADED)
		private String layerLoaded = DEFAULT_IMAGE_LAYER_INDICATOR_LOADED;
		
		@XmlElement(name = "layer_indicator_loading", defaultValue = DEFAULT_IMAGE_LAYER_INDICATOR_LOADING)
		private String layerLoading = DEFAULT_IMAGE_LAYER_INDICATOR_LOADING;
		
		@XmlElement(name = "layer_indicator_off", defaultValue = DEFAULT_IMAGE_LAYER_INDICATOR_OFF)
		private String layerOff = DEFAULT_IMAGE_LAYER_INDICATOR_OFF;
		
		@XmlElement(name = "fill2", defaultValue = DEFAULT_IMAGE_FILL2)
		private String fill2 = DEFAULT_IMAGE_FILL2;
		
		@XmlElement(name = "icon_inactive", defaultValue = DEFAULT_IMAGE_ICON_INACTIVE)
		private String iconInactive = DEFAULT_IMAGE_ICON_INACTIVE;

		public Color getFill() {
			fill = (fill == null ? DEFAULT_IMAGE_FILL : fill);
			return parse(fill);
		}
		
		public Color getCommonFill() {
			commonFill = (commonFill == null ? DEFAULT_COMMON_IMAGE_FILL : commonFill);
			return parse(commonFill);
		}

		public Color getSelectionFill() {
			selectionFill = (selectionFill == null ? DEFAULT_IMAGE_SELECTION_FILL : selectionFill);
			return parse(selectionFill);
		}

		public Color getLayer() {
			layer = (layer == null ? DEFAULT_IMAGE_LAYER : layer);
			return parse(layer);
		}

		public Color getLink() {
		    link = (link == null ? DEFAULT_IMAGE_LINK : link);
			return parse(link);
		}
		
		public Color getLayerLoaded() {
		    layerLoaded = (layerLoaded == null ? DEFAULT_IMAGE_LAYER_INDICATOR_LOADED : layerLoaded);
			return parse(layerLoaded);
		}
		
		public Color getLayerLoading() {
			layerLoading = (layerLoading == null ? DEFAULT_IMAGE_LAYER_INDICATOR_LOADING : layerLoading);
			return parse(layerLoading);
		}
		
		public Color getLayeroff() {
			layerOff = (layerOff == null ? DEFAULT_IMAGE_LAYER_INDICATOR_OFF : layerOff);
			return parse(layerOff);
		}

		public Color getFill2() {
			fill2 = (fill2 == null ? DEFAULT_IMAGE_FILL2 : fill2);
			return parse(fill2);			
		}
		
		public Color getIconInactive() {
			iconInactive = (iconInactive == null ? DEFAULT_IMAGE_ICON_INACTIVE : iconInactive);
			return parse(iconInactive);
		}
	}


	@XmlRootElement(name = "settings")
	@XmlAccessorType(XmlAccessType.FIELD)
	public final static class Settings {
		public Settings() {
		}

		//
		// 
		@XmlElement(name = "button_enable_hover", defaultValue=SETTINGS_BUTTON_ENABLE_HOVER)
		private Boolean buttonEnableHover = DEFAULT_BUTTON_ENABLE_HOVER;

		@XmlElement(name = "button_default_follows_focus", defaultValue=SETTINGS_BUTTON_DEFAULT_FOLLOWS_FOCUS)
		private Boolean buttonDefaultFollowsFocus = DEFAULT_BUTTON_DEFAULT_FOLLOWS_FOCUS;

		@XmlElement(name = "button_focusable", defaultValue=SETTINGS_BUTTON_FOCUSABLE)
		private Boolean buttonFocusable = DEFAULT_BUTTON_FOCUSABLE;

		@XmlElement(name = "button_arc_size", defaultValue=SETTINGS_BUTTON_ARC_SIZE)
		private Integer buttonArcSize = DEFAULT_BUTTON_ARC_SIZE;

		@XmlElement(name = "tree_paint_lines", defaultValue=SETTINGS_TREE_PAINT_LINES)
		private Boolean treePaintLines = DEFAULT_TREE_PAINT_LINES;

		@XmlElement(name = "table_focusable", defaultValue=SETTINGS_TABLE_FOCUSABLE)
		private Boolean tableFocusable = DEFAULT_TABLE_FOCUSABLE;

		@XmlElement(name = "table_alternate_row_color", defaultValue=SETTINGS_TABLE_ALTERNATE_ROW_COLOR)
		private Boolean tableAlternateRowColor = DEFAULT_TABLE_ALTERNATE_ROW_COLOR;

		@XmlElement(name = "combobox_enable_hover", defaultValue=SETTINGS_COMBOBOX_ENABLE_HOVER)
		private Boolean comboboxEnableHover = DEFAULT_COMBOBOX_ENABLE_HOVER;

		@XmlElement(name = "table_row_height", defaultValue=SETTINGS_TABLE_ROW_HEIGHT)
		private Integer tableRowHeight = DEFAULT_TABLE_ROW_HEIGHT;

		@XmlElement(name = "scrollbar_width", defaultValue=SETTINGS_SCROLLBAR_WIDTH)
		private Integer scrollbarWidth = DEFAULT_SCROLLBAR_WIDTH;

		@XmlElement(name = "progressbar_border_thickness", defaultValue=SETTINGS_PROGRESSBAR_BORDER_THICKNESS)
		private Integer progressbarBorderThickness = DEFAULT_PROGRESSBAR_BORDER_THICKNESS;

		@XmlElement(name = "menubar_item_spacing", defaultValue=SETTINGS_MENUBAR_ITEM_SPACING)
		private Integer menubarItemSpacing = DEFAULT_MENUBAR_ITEM_SPACING;


		@XmlElement(name = "menubar_item_padding", defaultValue=SETTINGS_MENUBAR_ITEM_PADDING)
		private Integer menubarItemPadding = DEFAULT_MENUBAR_ITEM_PADDING;

		
		@XmlElement(name = "filechooser_img_scaled_width", defaultValue=SETTINGS_FILECHOOSER_IMG_SCALED_WIDTH)
		private Integer filechooserImgScaledWidth = DEFAULT_FILECHOOSER_IMG_SCALED_WIDTH;

		@XmlElement(name = "filechooser_img_scaled_height", defaultValue=SETTINGS_FILECHOOSER_IMG_SCALED_HEIGHT)
		private Integer filechooserImgScaledHeight = DEFAULT_FILECHOOSER_IMG_SCALED_HEIGHT;

		@XmlElement(name = "tab_indent", defaultValue=SETTINGS_TAB_INDENT)
		private Integer tabIndent = DEFAULT_TAB_INDENT;

		@XmlElement(name = "tab_space", defaultValue=SETTINGS_TAB_SPACE)
		private Integer tabSpace = DEFAULT_TAB_SPACE;


		public boolean isButtonEnableHover() {
			buttonEnableHover = (buttonEnableHover == null ? DEFAULT_BUTTON_ENABLE_HOVER : buttonEnableHover);
			return buttonEnableHover;
		}

		public boolean isButtonDefaultFollowsFocus() {
		    buttonDefaultFollowsFocus = (buttonDefaultFollowsFocus == null ? DEFAULT_BUTTON_DEFAULT_FOLLOWS_FOCUS : buttonDefaultFollowsFocus);
			return buttonDefaultFollowsFocus;
		}

		public boolean isButtonFocusable() {
		    buttonFocusable = (buttonFocusable == null ? DEFAULT_BUTTON_FOCUSABLE : buttonFocusable);
			return buttonFocusable;
		}

		public int getButtonArcSize() {
		    buttonArcSize = (buttonArcSize == null ? DEFAULT_BUTTON_ARC_SIZE : buttonArcSize);
			return buttonArcSize;
		}

		public boolean isTreePaintLines() {
		    treePaintLines = (treePaintLines == null ? DEFAULT_TREE_PAINT_LINES : treePaintLines);
		    return treePaintLines;
		}

		public boolean isTableFocusable() {
		    tableFocusable = (tableFocusable == null ? DEFAULT_TABLE_FOCUSABLE : tableFocusable);
			return tableFocusable;
		}

		public boolean isTableAlternateRowColor() {
		    tableAlternateRowColor = (tableAlternateRowColor == null ? DEFAULT_TABLE_ALTERNATE_ROW_COLOR : tableAlternateRowColor);
			return tableAlternateRowColor;
		}

		public boolean isComboboxEnableHover() {
		    comboboxEnableHover = (comboboxEnableHover == null ? DEFAULT_COMBOBOX_ENABLE_HOVER : comboboxEnableHover);
			return comboboxEnableHover;
		}

		public int getTableRowHeight() {
		    tableRowHeight = (tableRowHeight == null ? DEFAULT_TABLE_ROW_HEIGHT : tableRowHeight);
		    return tableRowHeight;
		}

		public int getScrollbarWidth() {
		    scrollbarWidth = (scrollbarWidth == null ? DEFAULT_SCROLLBAR_WIDTH : scrollbarWidth);
		    return scrollbarWidth;
		}

		public int getProgressbarBorderThickness() {
		    progressbarBorderThickness = (progressbarBorderThickness == null ? DEFAULT_PROGRESSBAR_BORDER_THICKNESS : progressbarBorderThickness);
		    return progressbarBorderThickness;
		}

		public int getMenubarItemSpacing() {
		    menubarItemSpacing = (menubarItemSpacing == null ? DEFAULT_MENUBAR_ITEM_SPACING : menubarItemSpacing);
			return menubarItemSpacing;
		}
		
		public int getMenubarItemPadding() {
			 menubarItemPadding = (menubarItemPadding == null ? DEFAULT_MENUBAR_ITEM_PADDING : menubarItemPadding);
			 return menubarItemPadding;
		}		

		public int getFilechooserImgScaledWidth() {
		    filechooserImgScaledWidth = (filechooserImgScaledWidth == null ? DEFAULT_FILECHOOSER_IMG_SCALED_WIDTH : filechooserImgScaledWidth);
			return filechooserImgScaledWidth;
		}

		public int getFilechooserImgScaledHeight() {
		    filechooserImgScaledHeight = (filechooserImgScaledHeight == null ? DEFAULT_FILECHOOSER_IMG_SCALED_HEIGHT : filechooserImgScaledHeight);
			return filechooserImgScaledHeight;
		}

		public int getTabIndent() {
		    tabIndent = (tabIndent == null ? DEFAULT_TAB_INDENT : tabIndent);
			return tabIndent;
		}

		public int getTabSpace() {
		    tabSpace = (tabSpace == null ? DEFAULT_TAB_SPACE : tabSpace);
			return tabSpace;
		}	
	}
	
	
	public static Color parse(String input) {
        Pattern c = Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
        Matcher m = c.matcher(input);

        if (m.matches()) 
        {
            return new ColorUIResource(Integer.valueOf(m.group(1)),  // r
                             Integer.valueOf(m.group(2)),  // g
                             Integer.valueOf(m.group(3))); // b 
        }

        return null;  
    }	
}

