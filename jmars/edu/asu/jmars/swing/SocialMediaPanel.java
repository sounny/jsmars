package edu.asu.jmars.swing;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.RIGHT_LINK_IMG;
import static edu.asu.jmars.ui.looknfeel.Utilities.getColorAsBrowserHex;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeText;
import edu.asu.jmars.util.Config;

public class SocialMediaPanel {
	private static final Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static final Color textColor = ((ThemeText) GUITheme.get("text")).getTextcolor();
	private static final ImageIcon fbIcon = new ImageIcon(
			ImageFactory.createImage(ImageCatalogItem.FACEBOOK_IMG.withDisplayColor(imgColor)));
	private static final ImageIcon twitterIcon = new ImageIcon(
			ImageFactory.createImage(ImageCatalogItem.TWITTER_IMG.withDisplayColor(imgColor)));
	private static final Color imgLinkColor = ((ThemeImages) GUITheme.get("images")).getLinkfill();
	private static final Image arrowImg = ImageFactory.createImage(RIGHT_LINK_IMG.withDisplayColor(imgLinkColor)
																	             .withWidth(11).withHeight(20));		
			
	private static Insets in = new Insets(5, 5, 5, 5);
	private static Map<TextAttribute, Object> spacingAtt1 = new HashMap<TextAttribute, Object>();

	public static JPanel get() {
		UrlLabel fbULbl = new UrlLabel("", Config.get("facebookpage"), null, fbIcon);
		fbULbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		UrlLabel twitterULbl = new UrlLabel("", Config.get("twitterpage"), null, twitterIcon);
		twitterULbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		UrlLabel homepageULbl = new UrlLabel("Visit JMARS Homepage", Config.get("homepage"),
				getColorAsBrowserHex(textColor), new ImageIcon(arrowImg));
		spacingAtt1.put(TextAttribute.TRACKING, 0.07);
		homepageULbl.setFont(ThemeFont.getRegular().deriveFont(Font.BOLD).deriveFont(spacingAtt1));
		JPanel aBotPnl = new JPanel(new GridBagLayout());
		int pad = 0;
		int col = 0;
		int row = 0;
		aBotPnl.add(fbULbl, new GridBagConstraints(col, row, 1, 1, 0, 0, GridBagConstraints.SOUTHWEST,
				GridBagConstraints.NONE, in, pad, pad));
		aBotPnl.add(twitterULbl, new GridBagConstraints(++col, row, 1, 1, 1, 0, GridBagConstraints.SOUTHWEST,
				GridBagConstraints.NONE, in, pad, pad));
		aBotPnl.add(homepageULbl, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.SOUTHEAST,
				GridBagConstraints.NONE, in, pad, pad));

		return aBotPnl;
	}
}
