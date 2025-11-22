package edu.asu.jmars.layer.profile;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class ProfileSettings extends JPanel {

	private static final long serialVersionUID = 2447420842891680560L;
	private JComboBox lineWidthCombo;
	private final ProfileFactory controller;
	private final ProfileLView profileLView;

	ProfileSettings(ProfileLView view, ProfileFactory control) {
		this.controller = control;
		this.profileLView = view;
		initLayout();
	}

	private void initLayout() {
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Select Profile Line Width"),
				BorderFactory.createEmptyBorder(10, 5, 5, 5)));

		// line width combo box
		Integer[] widthChoices = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

		lineWidthCombo = new JComboBox(widthChoices);
		lineWidthCombo.setSelectedIndex(1);
		lineWidthCombo.addActionListener(e -> lineWidthSelected(e));

		// Place the label and the combo on the same row
		JPanel lineWidthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
		lineWidthPanel.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
		lineWidthPanel.add(new JLabel("Select width", JLabel.LEFT));
		
		lineWidthPanel.add(lineWidthCombo);
		add(lineWidthPanel, BorderLayout.WEST);
	}

	private void lineWidthSelected(ActionEvent e) {
		JComboBox cb = (JComboBox) e.getSource();
		Integer newwidth = (Integer) cb.getSelectedItem(); // this is line width		
		this.controller.lineWidthChanged(new ImmutablePair<ProfileLView, Integer>(this.profileLView, newwidth));
	}

}
