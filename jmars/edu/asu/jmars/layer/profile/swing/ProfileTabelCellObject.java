package edu.asu.jmars.layer.profile.swing;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;

public class ProfileTabelCellObject {
	private String name;
	private Color color = null;
	private Icon line = null;
	private static Map<Color, Icon> icons = new HashMap<>();
	
	
	public ProfileTabelCellObject(String name) {
		super();
		this.name = name;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public Color getColor() {
		return color;
	}


	public void setColor(Color color) {
		this.color = color;
	}


	public Icon getLine() {
		if (this.color == null) {
			return null;
		}
		if (icons.get(this.color) == null) {
			this.line = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.LINE.withDisplayColor(this.color)));
			icons.put(this.color, this.line);
		} else {
			this.line = icons.get(this.color);
		}
		return this.line;
	}
	
	@Override
	public String toString() {
		return name;		
	}

}
