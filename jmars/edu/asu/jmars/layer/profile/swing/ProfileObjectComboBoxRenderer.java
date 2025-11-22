package edu.asu.jmars.layer.profile.swing;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import edu.asu.jmars.layer.profile.ProfileLView;
import edu.asu.jmars.layer.profile.ProfileLView.ProfileLine;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;


public class ProfileObjectComboBoxRenderer extends JLabel implements ListCellRenderer {
	private ProfileLView profileLView;
	private ProfileTabelCellObject cellobj;
	private Color bordercolor = ThemeProvider.getInstance().getAction().getBorder();
	private JLabel nullLabel = new JLabel();

	public ProfileObjectComboBoxRenderer(ProfileLView view) {
		setOpaque(true);
		this.profileLView = view;
		this.cellobj = new ProfileTabelCellObject(" ");
		setIcon(null);
		setText(" ");		
		setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 0, 0, bordercolor), 
				new EmptyBorder(10, 5, 10, 5)));
		nullLabel.setText("SELECT PROFILE...");
		nullLabel.setIcon(null);
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		if (value == null) {
			return nullLabel;
		}
		int ID = ((Integer) value).intValue();
		ProfileLine profile = (ProfileLine) this.profileLView.getProfilelineByID(ID);
		if (profile == null) {
			return nullLabel;
		}
		String name = profile.getRename() != null ? profile.getRename() : profile.getIdentifier();
		this.cellobj.setName(name);
		this.cellobj.setColor(profile.getLinecolor());
		setIcon(this.cellobj.getLine());
		setText(this.cellobj.getName());
		if (index % 2 == 0) {
			setBackground(ThemeProvider.getInstance().getRow().getBackground());  			
		} else {
			setBackground(ThemeProvider.getInstance().getRow().getAlternateback());
		}		
		list.setToolTipText(name);
		return this;
	}
}
