package edu.asu.jmars.layer.shape2;

import java.awt.Color;

import edu.asu.jmars.layer.util.features.Style;
import edu.asu.jmars.layer.util.features.Styles;
import edu.asu.jmars.util.FillStyle;
import edu.asu.jmars.util.LineType;

public class ShapeLayerStyles extends Styles {
	public final Style<Color> selLineColor = new Style<Color>("Selected Line Color", Color.yellow);
	public final Style<Number> selLineWidth = new Style<Number>("Selected Line Width", 3);
	
	public ShapeLayerStyles() {
		super();
		showLabels.setConstant(true);
		drawOutlines.setConstant(true);
		fillPolygons.setConstant(true);
		showVertices.setConstant(false);
		lineColor.setConstant(Color.white);
		fillColor.setConstant(Color.red);
		labelColor.setConstant(Color.white);
		showLineDir.setConstant(false);
		lineDash.setConstant(new LineType());
		lineWidth.setConstant(1);
		pointSize.setConstant(3);
		fillStyle.setConstant(new FillStyle());
	}
	
	/** copy constructor */
	public ShapeLayerStyles(ShapeLayerStyles styles) {
		this();
		setFrom(styles);
	}
}

