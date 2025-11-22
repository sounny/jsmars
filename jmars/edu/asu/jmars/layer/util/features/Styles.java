package edu.asu.jmars.layer.util.features;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.UIManager;

import edu.asu.jmars.util.FillStyle;
import edu.asu.jmars.util.LineType;

/**
 * Provides all styles used for rendering features; styles have a global
 * value, or are retrieved from a feature one at a time. The field to
 * retrieve, and a default to use when the field is not defined in the
 * feature's attribute map, must be supplied in the latter case.
 * 
 * Styles subclasses and all style properties on them must be public
 * final.  The public requirement is hard and fast; not making them
 * public will cause problems in some parts of the rendering system.
 * The final requirement is simply best practice due to how they are
 * used; the compiler will be able to optimize final fields better.
 */
public class Styles {
	public static final String FIELD_NAME_SHOW_VERTICES = "Show Vertices";
	public static final String FIELD_NAME_POINT_SIZE = "Point Size";
	public static final String FIELD_NAME_LINE_WIDTH = "Line Width";
	public static final String FIELD_NAME_VERTEX_SIZE = "Vertex Size";
	public static final String FIELD_NAME_LABEL_SIZE = "Label Size";
	
    /** The default font for the platform. */
    private static final Font DEFAULT_FONT = UIManager.getDefaults().getFont("MenuBar.font");
	public final Style<Boolean> antialias = new Style<Boolean>("Antialias", false);
	public final Style<FPath> geometry = new Style<FPath>("Geometry", Field.FIELD_PATH, null);
	public final Style<Boolean> showVertices = new Style<Boolean>(FIELD_NAME_SHOW_VERTICES, true);
	
	public final Style<Number> pointSize = new Style<Number>(FIELD_NAME_POINT_SIZE, Field.FIELD_POINT_SIZE, 3);
	
	public final Style<Boolean> drawOutlines = new Style<Boolean>("Show Outlines",Field.FIELD_DRAW_OUTLINE,true);
	public final Style<Number> lineWidth = new Style<Number>(FIELD_NAME_LINE_WIDTH, Field.FIELD_LINE_WIDTH, 1);
	public final Style<Color> lineColor = new Style<Color>("Line Color", Field.FIELD_DRAW_COLOR, Color.WHITE);
	public final Style<LineType> lineDash = new Style<LineType>("Line Style", Field.FIELD_LINE_DASH, new LineType()); // solid
	public final Style<Boolean> showLineDir = new Style<Boolean>("Show Line Direction", false);
	
	public final Style<Boolean> fillPolygons = new Style<Boolean>("Fill Polygons", true);
	public final Style<Color> fillColor = new Style<Color>("Fill Color", Field.FIELD_FILL_COLOR, Color.RED);
	public final Style<Number> vertexSize = new Style<Number>(FIELD_NAME_VERTEX_SIZE, 3);
	public final Style<FillStyle> fillStyle = new Style<FillStyle>("Fill Style", Field.FIELD_FILL_STYLE, new FillStyle()); // pattern
	
	public final Style<Boolean> showLabels = new Style<Boolean>("Show Labels", true);
	public final Style<String> labelText = new Style<String>("Label Text", Field.FIELD_LABEL, "");
	public final Style<Color> labelColor = new Style<Color>("Label Color", Field.FIELD_LABEL_COLOR, Color.WHITE);
	public final Style<Color> labelBorderColor = new Style<Color>("Label Border Color",Field.FIELD_LABEL_BORDER_COLOR,Color.BLACK);
	public final Style<String> labelFont = new Style<String>("Label Font", Field.FIELD_LABEL_FONT, DEFAULT_FONT.getFamily());
	public final Style<Number> labelSize = new Style<Number>(FIELD_NAME_LABEL_SIZE, Field.FIELD_LABEL_SIZE,DEFAULT_FONT.getSize() + 2);
	public final Style<String> labelStyle= new Style<String>("Label Style",Field.FIELD_LABEL_STYLE,"Bold");
	
	public static final ArrayList<String> numericDefaultFields = new ArrayList<String>();
	
	static {//this is a list of default numeric fields created by the shape layer
		numericDefaultFields.add(FIELD_NAME_SHOW_VERTICES.toLowerCase());
		numericDefaultFields.add(FIELD_NAME_POINT_SIZE.toLowerCase());
		numericDefaultFields.add(FIELD_NAME_LINE_WIDTH.toLowerCase());
		numericDefaultFields.add(FIELD_NAME_VERTEX_SIZE.toLowerCase());
		numericDefaultFields.add(FIELD_NAME_LABEL_SIZE.toLowerCase());
		numericDefaultFields.add("vertexsize");//no idea, but it is there
	}
	
	public Styles() {}
	
	/** copy constructor, makes a shallow copy except for public Style fields which are cloned */
	public Styles(Styles styles) {
		this();
		setFrom(styles);
	}
	
	/** Returns the public styles of this and all child classes as a set */
	public final Set<Style<? extends Object>> getStyles() {
		Set<Style<?>> styles = new LinkedHashSet<Style<?>>();
		for (java.lang.reflect.Field f: getClass().getFields()) {
			try {
				Object o = f.get(this);
				if (Style.class.isInstance(o)) {
					styles.add((Style<?>)o);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return styles;
	}
	
	/** Returns the fields from all public styles */
	public final Set<Field> getFields() {
		Set<Field> out = new LinkedHashSet<Field>();
		for (Style<?> s: getStyles()) {
			out.addAll(s.getSource().getFields());
		}
		return out;
	}
	
	public void setFrom(Styles styles) {
		try {
			for (java.lang.reflect.Field f: getClass().getFields()) {
				if (Style.class.isAssignableFrom(f.getType())) {
					Style<Object> from = (Style<Object>)f.get(styles);
					Style<Object> to = (Style<Object>)f.get(this);
					to.setSource(from.getSource());
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("Unable to set style values", e);
		}
	}
}

