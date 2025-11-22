package edu.asu.jmars.layer.profile.config;

import javax.swing.JPanel;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.JRadioButton;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import edu.asu.jmars.layer.profile.ProfileFactory;
import edu.asu.jmars.layer.profile.ProfileLView;

public class ChartConfigPanel extends JPanel {
	private JTextField txtinputChartName;
	private JTable tblProfiles;
	private JTable tblNumeric;
	private JButton btnConfigProfiles;

	/**
	 * Create the panel.
	 */
	public ChartConfigPanel(ProfileLView profileLView, ProfileFactory profileFactory) {
		setBorder(new EmptyBorder(5, 15, 10, 5));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] {0, 0, 0, 0, 0};
		gridBagLayout.rowHeights = new int[] {30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30};
		gridBagLayout.columnWeights = new double[]{1.0, 1.0, 1.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0};
		setLayout(gridBagLayout);
		
		JLabel lblChartName = new JLabel("CHART NAME");
		lblChartName.setVerticalAlignment(SwingConstants.TOP);
		GridBagConstraints gbc_lblChartName = new GridBagConstraints();
		gbc_lblChartName.insets = new Insets(0, 0, 5, 5);
		gbc_lblChartName.anchor = GridBagConstraints.WEST;
		gbc_lblChartName.gridx = 0;
		gbc_lblChartName.gridy = 1;
		add(lblChartName, gbc_lblChartName);
		
		txtinputChartName = new JTextField();
		txtinputChartName.setToolTipText("enter chart name or use provided default");
		GridBagConstraints gbc_txtinputChartName = new GridBagConstraints();
		gbc_txtinputChartName.insets = new Insets(0, 0, 5, 5);
		gbc_txtinputChartName.anchor = GridBagConstraints.WEST;
		gbc_txtinputChartName.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtinputChartName.gridx = 0;
		gbc_txtinputChartName.gridy = 2;
		add(txtinputChartName, gbc_txtinputChartName);
		txtinputChartName.setColumns(5);
		
		JLabel lblChartSetup = new JLabel("CHART SETUP");
		GridBagConstraints gbc_lblChartSetup = new GridBagConstraints();
		gbc_lblChartSetup.insets = new Insets(0, 0, 5, 5);
		gbc_lblChartSetup.anchor = GridBagConstraints.WEST;
		gbc_lblChartSetup.gridx = 0;
		gbc_lblChartSetup.gridy = 4;
		add(lblChartSetup, gbc_lblChartSetup);
		
		JRadioButton rdbtnSelectProfiles = new JRadioButton("Compare Profile Lines");
		rdbtnSelectProfiles.setSelected(true);
		GridBagConstraints gbc_rdbtnSelectProfiles = new GridBagConstraints();
		gbc_rdbtnSelectProfiles.fill = GridBagConstraints.HORIZONTAL;
		gbc_rdbtnSelectProfiles.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnSelectProfiles.anchor = GridBagConstraints.WEST;
		gbc_rdbtnSelectProfiles.gridx = 0;
		gbc_rdbtnSelectProfiles.gridy = 5;
		add(rdbtnSelectProfiles, gbc_rdbtnSelectProfiles);
		
		JRadioButton rdbtnSelectNumeric = new JRadioButton("Compare Numeric Sources");
		GridBagConstraints gbc_rdbtnSelectNumeric = new GridBagConstraints();
		gbc_rdbtnSelectNumeric.fill = GridBagConstraints.HORIZONTAL;
		gbc_rdbtnSelectNumeric.insets = new Insets(0, 0, 5, 0);
		gbc_rdbtnSelectNumeric.anchor = GridBagConstraints.WEST;
		gbc_rdbtnSelectNumeric.gridx = 1;
		gbc_rdbtnSelectNumeric.gridy = 5;
		add(rdbtnSelectNumeric, gbc_rdbtnSelectNumeric);
		
		JPanel panelImportantMsg = new JPanel();
		GridBagConstraints gbc_panelImportantMsg = new GridBagConstraints();
		gbc_panelImportantMsg.insets = new Insets(0, 0, 5, 0);
		gbc_panelImportantMsg.gridwidth = 2;
		gbc_panelImportantMsg.fill = GridBagConstraints.HORIZONTAL;
		gbc_panelImportantMsg.gridx = 0;
		gbc_panelImportantMsg.gridy = 6;
		add(panelImportantMsg, gbc_panelImportantMsg);
		
		JLabel lblSelectProfiles = new JLabel("SELECT PROFILE LINES");
		GridBagConstraints gbc_lblSelectProfiles = new GridBagConstraints();
		gbc_lblSelectProfiles.anchor = GridBagConstraints.WEST;
		gbc_lblSelectProfiles.insets = new Insets(0, 0, 5, 5);
		gbc_lblSelectProfiles.gridx = 0;
		gbc_lblSelectProfiles.gridy = 8;
		add(lblSelectProfiles, gbc_lblSelectProfiles);
		
		JLabel lblSelectNumeric = new JLabel("SELECT NUMERIC SOURCES");
		GridBagConstraints gbc_lblSelectNumeric = new GridBagConstraints();
		gbc_lblSelectNumeric.anchor = GridBagConstraints.WEST;
		gbc_lblSelectNumeric.insets = new Insets(0, 0, 5, 0);
		gbc_lblSelectNumeric.gridx = 2;
		gbc_lblSelectNumeric.gridy = 8;
		add(lblSelectNumeric, gbc_lblSelectNumeric);
		
		btnConfigProfiles = new JButton("CONFIGURE PROFILES");
		GridBagConstraints gbc_btnConfigProfiles = new GridBagConstraints();
		gbc_btnConfigProfiles.anchor = GridBagConstraints.WEST;
		gbc_btnConfigProfiles.insets = new Insets(0, 0, 5, 5);
		gbc_btnConfigProfiles.gridx = 0;
		gbc_btnConfigProfiles.gridy = 9;
		add(btnConfigProfiles, gbc_btnConfigProfiles);
		
		JButton btnConfigSources = new JButton("CONFIGURE SOURCES");
		GridBagConstraints gbc_btnConfigSources = new GridBagConstraints();
		gbc_btnConfigSources.anchor = GridBagConstraints.WEST;
		gbc_btnConfigSources.insets = new Insets(0, 0, 5, 0);
		gbc_btnConfigSources.gridx = 2;
		gbc_btnConfigSources.gridy = 9;
		add(btnConfigSources, gbc_btnConfigSources);
		
		tblProfiles = new JTable();
		GridBagConstraints gbc_tblProfiles = new GridBagConstraints();
		gbc_tblProfiles.anchor = GridBagConstraints.WEST;
		gbc_tblProfiles.insets = new Insets(0, 0, 0, 5);
		gbc_tblProfiles.fill = GridBagConstraints.BOTH;
		gbc_tblProfiles.gridx = 0;
		gbc_tblProfiles.gridy = 10;
		add(tblProfiles, gbc_tblProfiles);
		
		tblNumeric = new JTable();
		GridBagConstraints gbc_tblNumeric = new GridBagConstraints();
		gbc_tblNumeric.fill = GridBagConstraints.BOTH;
		gbc_tblNumeric.gridx = 2;
		gbc_tblNumeric.gridy = 10;
		add(tblNumeric, gbc_tblNumeric);

	}
}
