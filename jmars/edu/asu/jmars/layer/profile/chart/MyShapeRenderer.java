package edu.asu.jmars.layer.profile.chart;

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.GeneralPath;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import edu.asu.jmars.layer.profile.ProfileLView;
import edu.asu.jmars.layer.profile.ProfileLView.ProfileLine;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;

public class MyShapeRenderer extends XYLineAndShapeRenderer {

	private ProfileLView.ProfileLine profileline = null;
	private int datsourcecount;
	private Paint linecolor;
	private static final Color defaultcolor = ThemeProvider.getInstance().getBackground().getHighlight();
	
	public MyShapeRenderer(ProfileLine profline, int count) {
		profileline = profline;
		datsourcecount = count;
		linecolor = getThisSeriesPaint(); 
		setSeriesPaint(0, linecolor);
		setSeriesShape(0, new GeneralPath());
		//setSeriesShapesFilled(0, true);
		//setSeriesFillPaint(0, Color.white); 
		//setSeriesOutlinePaint(0, Color.black);
		setSeriesStroke(0, profileline.getPlotstrokes().get(datsourcecount % ProfileLine.NUMBER_OF_SERIES));
		//setUseOutlinePaint(true);
		//setUseFillPaint(true);
	}

	private Paint getThisSeriesPaint() {
		Paint seriesPaint = null;
		if (this.datsourcecount == 0) {
			seriesPaint = profileline.getLinecolor() != null ? profileline.getLinecolor() : defaultcolor;
		} else {
			seriesPaint = profileline.getPlotfillpaint().get(datsourcecount % ProfileLine.NUMBER_OF_SERIES);
		}
		return seriesPaint;
	}

	/*
	 * @Override public Shape getItemShape(int row, int column) { if
	 * (this.datsourcecount > 0 && column % 15 == 0) { return
	 * this.profileline.getPlotshapes().get(datsourcecount %
	 * ProfileLine.NUMBER_OF_SERIES); } else return new GeneralPath(); //return
	 * this.profileline.getPlotshapes().get(datsourcecount %
	 * ProfileLine.NUMBER_OF_SERIES); }
	 * 
	 * @Override public Paint getItemFillPaint(int row, int column) { if
	 * (this.datsourcecount > 0 && column % 15 == 0) { return
	 * profileline.getPlotfillpaint().get(datsourcecount %
	 * ProfileLine.NUMBER_OF_SERIES); } else return defaultcolor; //return
	 * profileline.getPlotfillpaint().get(datsourcecount %
	 * ProfileLine.NUMBER_OF_SERIES); }
	 */
}
