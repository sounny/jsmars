package edu.asu.jmars.layer.profile.chart;

import java.awt.Color;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jfree.chart.axis.Axis;
import edu.asu.jmars.swing.ColorButton;

public class MoreChartProperties extends JPanel {
	private static final long serialVersionUID = -6590951238486692434L;
	MorePropsPanel domainAxisSettings = null;
	MorePropsPanel rangeAxisSettings = null;

	MoreChartProperties(Axis domainAxis, Axis rangeAxis) {
		domainAxisSettings = new MorePropsPanel("Domain Axis Ticks Color", domainAxis);
		rangeAxisSettings = new MorePropsPanel("Range Axis Ticks Color", rangeAxis);
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		JPanel settings = new JPanel();
		settings.setLayout(new GridLayout(1, 2));
		settings.add(domainAxisSettings);
		settings.add(rangeAxisSettings);
		add(settings);		
	}

	class MorePropsPanel extends JPanel {
		ColorButton btnColor;
		Axis currentAxis;

		MorePropsPanel(String title, Axis axis) {
			this.currentAxis = axis;
			setLayout(new GridLayout(2, 1, 0, 1));
			setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title),
					BorderFactory.createEmptyBorder(5, 5, 5, 5)));
			add(new JLabel("Choose color:", JLabel.LEFT));
			btnColor = new ColorButton("", (Color) axis.getTickLabelPaint());
			btnColor.addPropertyChangeListener(colorListener);
			add(btnColor);
		}

		private PropertyChangeListener colorListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				Object source = evt.getSource();
				// Handle color chooser changes
				if (source == btnColor) {
					Color newColor = btnColor.getColor();
					if (newColor != null) {
						// Reflect the new color
						btnColor.setBackground(newColor);
						currentAxis.setTickLabelPaint(newColor);
					}
				}
			}
		};
	}
}