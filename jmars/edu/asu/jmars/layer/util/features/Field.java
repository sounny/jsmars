package edu.asu.jmars.layer.util.features;

import java.awt.Color;
import java.awt.Font;
import java.io.Serializable;

import edu.asu.jmars.util.FillStyle;
import edu.asu.jmars.util.LineType;

/**
 * A name, type, and editable state.
 * The name, type, and editable are publically accessible fields, but they
 * are final and thus unmodifiable after a Field is instantiated.
 * Equality is defined as having the same name and type, so a Set of Field
 * instances can have multiple fields with the same name and different types.
 */
public class Field implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * All Feature instances should have a path and type.
	 */

	/**
	 * Spatial coordinates in the west-leading areo-centric coordinate system.
	 */
	public static final Field FIELD_PATH = new Field("path",FPath.class,true);
	public static final Field FIELD_DRAW_COLOR = new Field("Line Color",java.awt.Color.class,true);
	public static final Field FIELD_FILL_COLOR = new Field("Fill Color",java.awt.Color.class,true);
	public static final Field FIELD_LABEL_COLOR = new Field("Label Color",Color.class,true);
	public static final Field FIELD_LINE_WIDTH = new Field("Line Width",Double.class,true);
	public static final Field FIELD_POINT_SIZE = new Field("Point Size",Integer.class,true);
	public static final Field FIELD_LINE_DASH = new Field("Line Type",LineType.class,true);
	public static final Field FIELD_LINE_DIRECTED = new Field("Line Arrow",Boolean.class,true);
	public static final Field FIELD_FONT = new Field("font",Font.class,false);
	public static final Field FIELD_LABEL = new Field("Label",String.class,true);
	public static final Field FIELD_SHOW_LABEL = new Field("Show Labels",Boolean.class,true);
	public static final Field FIELD_FILL_POLYGON = new Field("Fill Poly",Boolean.class,true);
	public static final Field FIELD_DRAW_OUTLINE = new Field("Show Outlines",Boolean.class,true);
	public static final Field FIELD_FEATURE_TYPE = new Field("Feature", String.class, false);
	public static final Field FIELD_LABEL_BORDER_COLOR = new Field("Label Border Color",Color.class,true);
	public static final Field FIELD_LABEL_FONT = new Field("Label Font",String.class,true);
	public static final Field FIELD_LABEL_SIZE=new Field("Label Size",Integer.class,true);
	public static final Field FIELD_LABEL_STYLE=new Field("Label Style",String.class,true);
	
	public static final Field FIELD_FILL_STYLE = new Field("Fill Style", FillStyle.class, true);
	
	/**
	 * The name of the field, usually what is displayed in a table header.
	 */
	public String name;//1787
	/**
	 * The type of the field. Note that the boxed primitives Integer, Double,
	 * etc must be used in place of real primitives, due to the limitations of
	 * Java 1.4.
	 */
	public final Class<?> type;

	/**
	 * An editable Field usually represents real data in some Feature source,
	 * and an uneditable Field is normally a calculated value (like polygonal
	 * area). Note that editable refers to whether the user should be able to
	 * edit the value; The system will still be able to modify attributes of
	 * this Field.
	 */
	public boolean editable;

	/**
	 * Create a new Field with the given name and type. The Field will be
	 * editable.
	 */
	public Field (String name, Class<?> type) {
		this (name, type, true);
	}

	/**
	 * Create a new Field with the given name, type, and editable state.
	 */
	public Field (String name, Class<?> type, boolean editable) {
		this.name = name;
		this.type = type;
		this.editable = editable;
	}

	/**
	 * Defines equality as the intersection of name and type equality.
	 */
	public boolean equals (Object o) {
		if (o instanceof Field) {
			Field f = (Field)o;
			return this.name.equals (f.name) && this.type.equals (f.type);
		} else {
			return false;
		}
	}

	/**
	 * Defines hash value as sum of name and type hashes.
	 */
	public int hashCode () {
		return this.name.hashCode () + this.type.hashCode ();
	}
	
	
	/**
	 * Must return the name, for user interface reasons
	 */
	public String toString(){
		return name;
	}
	
	/**
	 * This is used to set fields uneditable for read only shape layers.
	 */
	public void setEditable(boolean b){
		editable = b;
	}
}

