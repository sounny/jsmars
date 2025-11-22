package edu.asu.jmars.tool.strategy;

import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import edu.asu.jmars.LoginWindow2;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.InvestigateDisplay;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.investigate.InvestigateFactory;
import edu.asu.jmars.swing.UrlLabel;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeTextArea;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;

public class InvestigateToolStrategy implements ToolStrategy {

	@Override
	public void doMode(int newmode, int oldmode) {
		// Display instruction dialog unless indicated in config file
		if (Config.get("showInvestigateInstructions").equalsIgnoreCase("true")) {
			JCheckBox chkBox = new JCheckBox("Do not show this message again.");
			int n = displayInvestigatePopup(chkBox);

			if (n == JOptionPane.OK_OPTION) { // if Okay, check the checkbox to set config variable
				boolean dontShow = chkBox.isSelected();
				if (dontShow) {
					Config.set("showInvestigateInstructions", "false");
				}
			} else { // if cancel, return to previous toolmode
				ToolManager.setToolMode(oldmode);
				return;
			}
		}
		if (InvestigateFactory.getLviewExists() == false) {
			// Create investigate layer if doesn't exist
			new InvestigateFactory().createLView(true, null);
		} else {
			// if it does exist, make sure it's selected when in this tool mode
			for (LView lv : LManager.getLManager().viewList) {
				if (lv.getName().equalsIgnoreCase("investigate layer")) {
					LManager.getLManager().setActiveLView(lv);
				}
			}
		}
		LManager.getLManager().repaint();

		if (LoginWindow2.getInitialize3DFlag()) {
			// turn the 3d investigate on
			ThreeDManager mgr = ThreeDManager.getInstance();
			mgr.setInvestigateMode(true);
		}
	}

	@Override
	public void preMode(int newmode, int oldmode) {
	}
	
	@Override
	public void postMode(int newmode, int oldmode) {
		if (newmode != ToolManager.INVESTIGATE) {
			if (LoginWindow2.getInitialize3DFlag()) {
				// turn 3d investigate off when not in investigate mode
				ThreeDManager mgr = ThreeDManager.getInstance();
				mgr.setInvestigateMode(false);
			}
			// Make sure the InvestigateDisplay is not visible anymore
			InvestigateDisplay.getInstance().setVisible(false);
		}
	}
	
	
	private static int displayInvestigatePopup(JCheckBox cb) {

		JLabel welcomeMessage = new JLabel("Welcome to the Investigate Tool");
		welcomeMessage.setFont(ThemeFont.getBold().deriveFont(Font.ITALIC, 18));
		String bullet = Character.toString('\u2022');

		JTextArea message = new JTextArea("\n" + bullet
				+ "While in this mode, a display box will follow the cursor around. "
				+ "This display box shows all the information under the cursor at that point on the screen.\n\n"
				+ bullet + "By default, the display will show the list view. "
				+ "When numeric data is available (with at least two data points at one spot) switching to the chart view will display a chart. "
				+ "To change views, use the left and right arrow keys.\n\n" + bullet
				+ "Left clicking when a chart is available will save that chart temporarily to "
				+ "the Investigate Layer (which is created and selected whenever the Investigate Tool is selected). "
				+ "These charts can be viewed and exported by opening up the focus panel for the Investigate Layer.\n\n"
				+ bullet + "For more information see the tutorial page:\n");
		message.setEditable(false);
		message.setBackground(((ThemeTextArea) GUITheme.get("textarea")).getBackground());
		message.setFont(ThemeFont.getRegular());
		message.setPreferredSize(new Dimension(350, 400));

		UrlLabel tutorial = new UrlLabel("https://jmars.mars.asu.edu/investigate-layer");

		JCheckBox chkBox = cb;

		Object[] params = { welcomeMessage, message, tutorial, chkBox };
		return Util.showConfirmDialog(params, "Investigate Tool Instructions", JOptionPane.OK_CANCEL_OPTION);

	}

}
