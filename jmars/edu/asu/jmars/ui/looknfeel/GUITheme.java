package edu.asu.jmars.ui.looknfeel;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import edu.asu.jmars.Main;
import edu.asu.jmars.ui.looknfeel.ThemeComponents.ComponentDescriptor;
import edu.asu.jmars.ui.looknfeel.theme.ThemeComponent;


public enum GUITheme {

	LIGHT("light") {
		@Override
		public String getName() {
			return LIGHTMODE;
		}
	},

	DARK ("dark") {
		@Override
		public String getName() {
			return DARKMODE;
		}
	};	

	GUITheme(String string) {
		this.theme = string;
	}
	
	public String asString() {
		return this.theme;
	}


	public void apply() {
		Catalog.clear();
		getThemeComponents();
	}
	
	public String getThemeResourcePath()
	{
		return PATH_TO_MATERIAL_THEME + name().toLowerCase() + "/";
	}

	private void getThemeComponents() {
		JAXBContext jaxbContext;
		try {
			ThemeProvider.createTheme(getThemeResourcePath() + name().toLowerCase()+".theme.xml");
			jaxbContext = JAXBContext.newInstance(ThemeComponents.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			String componentsFile = getThemeResourcePath() + "ThemeComponents.xml";
			InputStream components = Main.getResourceAsStream(componentsFile);
			ThemeComponents comps = (ThemeComponents) jaxbUnmarshaller.unmarshal(components);

			for (ComponentDescriptor comp : comps.getThemeComponents()) {
				Class.forName(comp.getClazz());
			}

			for (ThemeComponent comp : Catalog.values()) {
				comp.configureUI();
			}

		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
		}
	}

	private static Map<String,ThemeComponent> Catalog = new HashMap<>();
	private static String PATH_TO_MATERIAL_THEME = "resources/material/theme/";	
	private String theme;
	public abstract String getName();
	public final static String DARKMODE = "Dark mode";  
	public final static String LIGHTMODE = "Light mode";	
	
	public static Map<String, ThemeComponent> getCatalog() {
		return Catalog;
	}
	
	public static ThemeComponent get(String key) {
		return Catalog.get(key);
	}
}


@XmlRootElement(name = "themeComponents")
@XmlAccessorType(XmlAccessType.FIELD)
 class ThemeComponents {
	@XmlElement(name = "themeComponent")
	private Set<ComponentDescriptor> themeComponents = new HashSet<>();

	public Set<ComponentDescriptor> getThemeComponents() {
		return themeComponents;
	}

	public void getThemeComponents(Set<ComponentDescriptor> themeComponents) {
		this.themeComponents = themeComponents;
	}
	
@XmlRootElement(name = "themeComponent")
@XmlAccessorType (XmlAccessType.FIELD)		
final static class ComponentDescriptor {	
	
	@XmlAttribute
	private String name;
	
	@XmlAttribute
	private String clazz;

	public String getName() {
		return name;
	}

	public String getClazz() {
		return clazz;
	}	
 }

}
