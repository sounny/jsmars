package edu.asu.jmars.ui.looknfeel;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import edu.asu.jmars.Main;
import mdlaf.utils.MaterialFontFactory;

@XmlRootElement(name = "font")
@XmlAccessorType(XmlAccessType.FIELD)
public class ThemeFont {
	
	private static String PATH_TO_MATERIAL_FONTS = "resources/material/fonts/roboto-jmars/";	
	private static final Font ITALIC = getFont("Roboto-Italic-JMARS.ttf", false);	
	private static final Font LIGHT = getFont("Roboto-Light-JMARS.ttf", false);
	private static final Font BOLD = getFont("Roboto-Bold-JMARS.ttf", false);
	private static final Font MEDIUM = getFont("Roboto-Medium-JMARS.ttf", false);
	private static final Font REGULAR = getFont("Roboto-Regular-JMARS.ttf", false);
	private static final Font THIN = getFont("Roboto-Thin-JMARS.ttf", false);
	private static final Font THIN_ITALIC = getFont("Roboto-ThinItalic-JMARS.ttf", false);
	private static final Font BOLD_ITALIC = getFont("Roboto-BoldItalic-JMARS.ttf", false);
	private static final Font MYFONT_REGULAR = getFont("MyFontArrowsRegular-JMARS.ttf", false);
			
	
	public enum FONTS {
		ROBOTO(14f), ROBOTO_TEXT(18f), ROBOTO_TABLE(13f), ROBOTO_TABLE_HEADER(13f), 
		ROBOTO_TABLE_COLUMN(13f), ROBOTO_TABLE_ROW(13f), ROBOTO_TAB(16f),
		ROBOTO_CHART_XL(18f), ROBOTO_CHART_LARGE(15f), ROBOTO_CHART_REGULAR(14f),
		ROBOTO_CHART_SMALL(12f),
		NOTO(14f), UNKNOWN(14f);

		private float fontsize;
		
		FONTS(float size) {
			this.fontsize = size;			
		}

		public float fontSize() {
			return fontsize;
		}	
	}	
	
	public enum FontFile {
        REGULAR("Roboto-Regular-JMARS.ttf"),
        BOLD("Roboto-Bold-JMARS.ttf"),
        ITALIC("Roboto-Italic-JMARS.ttf"),
        MEDIUM("Roboto-Medium-JMARS.ttf"),
        LIGHT("Roboto-Light-JMARS.ttf"),
        THIN_ITALIC("Roboto-ThinItalic-JMARS.ttf"),
        MYFONT_REGULAR("MyFontArrowsRegular-JMARS.ttf");

        private String name;

        FontFile(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
	
	public static Font getFont(String fileName) {		
		return getFont(fileName, true);
	}
	
	public static Font getFont(String fileName, boolean withPersonalAttribute) {
		Font customFont = null;
		if (withPersonalAttribute) {
			customFont = MaterialFontFactory.getInstance()
					.getFontWithStream(Main.getResourceAsStream(PATH_TO_MATERIAL_FONTS + fileName), true);
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(customFont);
			return customFont;
		}
		customFont = MaterialFontFactory.getInstance()
				.getFontWithStream(Main.getResourceAsStream(PATH_TO_MATERIAL_FONTS + fileName), false);
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		ge.registerFont(customFont);
		return customFont;
		/* use this code to prevent exception in Chart IF not using material lib */
		/* if (withMaterialLib) {
			customFont = MaterialFontFactory.getInstance()
					.getFontWithStream(Main.getResourceAsStream(PATH_TO_MATERIAL_FONTS + fileName), false);
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			ge.registerFont(customFont);
		} else {
			try {
				customFont = Font.createFont(Font.TRUETYPE_FONT,
						Main.getResourceAsStream(PATH_TO_MATERIAL_FONTS + fileName));
				GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
				ge.registerFont(customFont);
			} catch (FontFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return customFont;*/
	}
	
	
	public static String getFontName() {
		return "Roboto";
	}
	
	public static float getRegularFontSize() {
		return 14f;
	}
	
	public static float getTextFontSize() {
		return 18f;
	}

	public static Font getItalic() {
		return ITALIC;
	}

	public static Font getLight() {
		return LIGHT;
	}

	public static Font getBold() {
		return BOLD;
	}

	public static Font getRegular() {
		return REGULAR;
	}

	public static Font getMedium() {
		return MEDIUM;
	}

	public static Font getThin() {
		return THIN;
	}

	public static Font getThinItalic() {
		return THIN_ITALIC;
	}
	
	public static Font getBoldItalic() {
		return BOLD_ITALIC;
	}
	
	public static Font getMyFont() {
		return MYFONT_REGULAR;
	}	

	public static String getFontPath() {
		return PATH_TO_MATERIAL_FONTS;
	}
	
	public static String getFontFamily() {
		return "Roboto JMARS";
	}
	
}
