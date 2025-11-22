package edu.asu.jmars.layer.profile.swing;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.TOGGLE_OFF_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.TOGGLE_ON_IMG;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeToggleButton;

public class ToggleBooleanCellRenderer extends JCheckBox implements TableCellRenderer {

	private Color toggleONColor = ((ThemeToggleButton) GUITheme.get("togglebutton")).getOnColor();
	private Color toggleOFFColor = ((ThemeToggleButton) GUITheme.get("togglebutton")).getOffColor();
	private Icon toggleOFF = new ImageIcon(ImageFactory.createImage(TOGGLE_OFF_IMG.withDisplayColor(toggleOFFColor)));
	private Icon toggleON = new ImageIcon(ImageFactory.createImage(TOGGLE_ON_IMG.withDisplayColor(toggleONColor)));

	public ToggleBooleanCellRenderer() {
		initUI();		
	}

	private void initUI() {
		setLayout(new GridBagLayout());
		setMargin(new Insets(0, 0, 0, 0));
		setHorizontalAlignment(JLabel.CENTER);
		setIcon(toggleOFF);
		setSelectedIcon(toggleON);		
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		if (value instanceof Boolean) {
			setSelected((Boolean) value);
		}
		Color alternateRowColor = ThemeProvider.getInstance().getRow().getAlternateback();

		if (!isSelected) {
			this.setIcon(toggleOFF);
			this.setSelectedIcon(toggleON);
			if (row % 2 == 1) {
				this.setBackground(alternateRowColor);
			} else {
				this.setBackground(table.getBackground());
			}
			this.setForeground(table.getForeground());
		} else {
			this.setIcon(toggleOFF);
			this.setSelectedIcon(toggleON);
			this.setForeground(table.getSelectionForeground());
			this.setBackground(table.getSelectionBackground());
		}
		return this;
	}

	public Icon getToggleOFF() {
		return toggleOFF;
	}

	public Icon getToggleON() {
		return toggleON;
	}
}
