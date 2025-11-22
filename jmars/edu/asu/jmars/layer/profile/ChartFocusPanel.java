package edu.asu.jmars.layer.profile;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.profile.ProfileLView.SavedParams;
import edu.asu.jmars.layer.profile.chart.ProfileChartView;
import edu.asu.jmars.layer.profile.config.ConfigureChartView;

public class ChartFocusPanel extends FocusPanel implements ChangeListener {
	private static final long serialVersionUID = 1L;
	final ProfileLView profileLView;
	final ProfileChartView chartView;
	final ConfigureChartView configView;
	final String chartTabName = "Chart";
	final String configTabName = "Configuration";
	final static String CHART_FOCUSPANEL_TITLE = "View and Configure Chart";


	private ChartFocusPanel(Builder builder) {
		super(builder.profileLView);
		addChangeListener(this);
		this.parentFrame.setTitle(CHART_FOCUSPANEL_TITLE);
		this.focuspanelheader.setInfoIcon(null);
		this.focuspanelheader.setSettingsIcon(null);
		this.profileLView = builder.profileLView;
		this.chartView = builder.chartView;
		this.configView = builder.configView;
		JScrollPane configcrollpanel = new JScrollPane(this.configView,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		addTab(configTabName, configcrollpanel);
		addTab(chartTabName, this.chartView);
	}

	@Override
	public void showInFrame() {
		if (this.chartView != null) {
			if (this.chartView.isCalloutVisible()) {
				this.chartView.hideCallout();
			}
		}
		super.parentFrame.setLocationRelativeTo(Main.mainFrame);
		super.showInFrame();
	}

	public ProfileLView getProfileLView() {
		return this.profileLView;
	}

	public ProfileChartView getChartView() {
		return this.chartView;
	}

	public ConfigureChartView getConfigView() {
		return this.configView;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private ProfileLView profileLView;
		private ProfileChartView chartView;
		private ConfigureChartView configView;

		private Builder() {
		}

		public Builder withProfileLView(ProfileLView profileLView) {
			this.profileLView = profileLView;
			return this;
		}

		public Builder withChartView(ProfileChartView chartView) {
			this.chartView = chartView;
			return this;
		}

		public Builder withConfigView(ConfigureChartView configView) {
			this.configView = configView;
			return this;
		}

		public ChartFocusPanel build() {
			return new ChartFocusPanel(this);
		}
	}


	@Override
	public void stateChanged(ChangeEvent e) {
		JTabbedPane tabSource = (JTabbedPane) e.getSource();
		String tab = tabSource.getTitleAt(tabSource.getSelectedIndex());
		if (tab.equalsIgnoreCase(chartTabName)) {
			this.configView.buildCurrentConfig();
		} else if (tab.equalsIgnoreCase(configTabName)) {
		}
	}

	
	public void close() {
		if (this.isVisible() && (this.isDocked() == false)) {
		    this.parentFrame.setVisible(false);
		} else if (this.isVisible() && this.isDocked()) {
			this.showInFrame();
			this.parentFrame.setVisible(false);
		}
		if (this.parentFrame != null) {
		     this.parentFrame.dispose();
		}
		 this.configView.close();
		 this.chartView.close();
	}

	public void notifyRestoredFromSession(SavedParams savedParams) {
		boolean isFPDocked = savedParams.isChartFPDocked();
		if (isFPDocked) {
			this.dock(false);
		}
	}
}
