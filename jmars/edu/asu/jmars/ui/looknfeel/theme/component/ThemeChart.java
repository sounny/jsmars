package edu.asu.jmars.ui.looknfeel.theme.component;

import java.awt.Color;
import java.awt.Font;
import org.jfree.chart.ChartTheme;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.TextTitle;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleInsets;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FONTS;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FontFile;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;

public class ThemeChart {
	
	final static ThemeProvider uitheme = ThemeProvider.getInstance();
	
	public static void configureUI(JFreeChart chart) {
		final ChartTheme chartTheme = StandardChartTheme.createJFreeTheme();		

		if (StandardChartTheme.class.isAssignableFrom(chartTheme.getClass())) {
			StandardChartTheme standardTheme = (StandardChartTheme) chartTheme;

			final Font extraLargeFont = ThemeFont.getFont(FontFile.REGULAR.toString(), false).deriveFont(FONTS.ROBOTO_CHART_XL.fontSize());
			final Font largeFont = ThemeFont.getFont(FontFile.REGULAR.toString(), false).deriveFont(FONTS.ROBOTO_CHART_LARGE.fontSize());
			final Font regularFont = ThemeFont.getFont(FontFile.REGULAR.toString(), false).deriveFont(FONTS.ROBOTO_CHART_REGULAR.fontSize());
			final Font smallFont = ThemeFont.getFont(FontFile.REGULAR.toString(), false).deriveFont(FONTS.ROBOTO_CHART_SMALL.fontSize());

			standardTheme.setExtraLargeFont(extraLargeFont);
			standardTheme.setLargeFont(largeFont);
			standardTheme.setRegularFont(regularFont);
			standardTheme.setSmallFont(smallFont);

			standardTheme.setRangeGridlinePaint(uitheme.getRow().getBorder());
			standardTheme.setPlotBackgroundPaint(uitheme.getBackground().getMain());
			standardTheme.setChartBackgroundPaint(uitheme.getBackground().getMain());
			standardTheme.setAxisOffset(new RectangleInsets(0, 0, 0, 0));
			standardTheme.setBarPainter(new StandardBarPainter());
			standardTheme.setAxisLabelPaint(uitheme.getText().getMain());			
			standardTheme.setTickLabelPaint(uitheme.getText().getMain());
			standardTheme.setTitlePaint(uitheme.getText().getMain());

			TextTitle title = chart.getTitle();
			if (title != null) {
				title.setFont(ThemeFont.getFont(FontFile.BOLD.toString(), false).deriveFont(FONTS.ROBOTO_CHART_XL.fontSize()));
				title.setHorizontalAlignment(HorizontalAlignment.CENTER);
				title.setPadding(16, 0, 16, 0);   
			}

			standardTheme.apply(chart);
		}
	}
	
	public static Color getPlotColor() {
		return uitheme.getBackground().getHighlight();
	}
	
	public static Color getIndicatorColor() {
		return uitheme.getAction().getChartIndicator();
	}
	
	public static Color getProfileCrosshairColor() {
		return uitheme.getAction().getProfileChartIndicator();
	}
	
	public static Color getNumberAxisColor() {
		return uitheme.getText().getMain();
	}
	
	public static Font getNumberAxisFont() {
		return  ThemeFont.getFont(FontFile.REGULAR.toString(), false).deriveFont(FONTS.ROBOTO_CHART_SMALL.fontSize());
	}
	
	public static void applyThemeToAxis(org.jfree.chart.axis.Axis axis)
	{
		axis.setLabelPaint(ThemeChart.getNumberAxisColor());			
		axis.setLabelFont(ThemeChart.getNumberAxisFont());	
		axis.setTickLabelPaint(ThemeChart.getNumberAxisColor());				
		axis.setTickLabelFont(ThemeChart.getNumberAxisFont());
	}	
	
	public static void applyDefaultThemeToAxisLabels(org.jfree.chart.axis.Axis axis)
	{
		axis.setLabelPaint(ThemeChart.getNumberAxisColor());			
		axis.setLabelFont(ThemeChart.getNumberAxisFont());		
	}
	
	public static void applyDefaultThemeToAxisTickLabels(org.jfree.chart.axis.Axis axis)
	{	
		axis.setTickLabelPaint(ThemeChart.getNumberAxisColor());				
		axis.setTickLabelFont(ThemeChart.getNumberAxisFont());
	}

	public static void applyUserThemeToAxisTickLabels(org.jfree.chart.axis.Axis newAxis, org.jfree.chart.axis.Axis rangeAxis) {		
		newAxis.setTickLabelPaint(rangeAxis.getTickLabelPaint());		
	}

}
