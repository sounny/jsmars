package edu.asu.jmars.swing.landmark.search.swing;

import static edu.asu.jmars.swing.landmark.search.popup.MenuCommand.COORD_ORDER;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.COORDINATES_SWITCH;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import edu.asu.jmars.Main;
import edu.asu.jmars.parsers.gis.CoordinatesParser.Ordering;
import edu.asu.jmars.swing.landmark.search.popup.LatLonPopup;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.Config;
import io.vincenzopalazzo.placeholder.JTextFieldPlaceholder;

public class LatLonBox extends JTextFieldPlaceholder {
	private static final long serialVersionUID = 1L;
	private JLabel placeholderIcon;
	private JPanel placeholderContainer;
	private JLabel menuButton;
	private MouseAdapter mymouseadapter;
	private static LatLonPopup latlonpopup = new LatLonPopup();
	private boolean isLatLonMenuCreated = false;
	private Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
	private Icon menuicon = new ImageIcon(ImageFactory.createImage(COORDINATES_SWITCH.withDisplayColor(imgLayerColor)));
	

	public LatLonBox(Icon placeholderIcon, Icon menu) {
		super();
		this.placeholderIcon = new JLabel(placeholderIcon);
		this.menuButton = new JLabel(menu);
		mymouseadapter = new On3DotClickAction();
		this.menuButton.addMouseListener(mymouseadapter);
	}

	public LatLonBox(JTextField textField, Icon placeholderIcon, Icon menu) {
		super(textField);
		this.placeholderIcon = new JLabel(placeholderIcon);
		this.menuButton = new JLabel(menu);
		this.menuButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		mymouseadapter = new On3DotClickAction();
		this.menuButton.addMouseListener(mymouseadapter);
	}

	@Override
	protected void initComponent() {
		super.initComponent();		
		placeholderContainer = new JPanel();
		placeholderContainer.setBackground(this.getBackground());
		placeholderContainer.setLayout(new BorderLayout());
	}

	@Override
	protected void initStyle() {
		super.initStyle();
		this.setBorder(BorderFactory.createEmptyBorder(0, -8, 4, 8));
	}

	@Override
	public void updateUI() {
		super.updateUI();
	}

	@Override
	protected void initLayout() {
		this.setLayout(new BorderLayout());
		this.add(iconContainer, BorderLayout.WEST);
		this.add(textField, BorderLayout.CENTER);
		this.add(placeholderContainer, BorderLayout.EAST);
		if (this.placeholderIcon != null) {
			this.placeholderContainer.add(this.placeholderIcon, BorderLayout.WEST);
		}
		if (this.menuButton != null) {
			this.menuButton.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 0));
			this.placeholderContainer.add(this.menuButton, BorderLayout.EAST);
		}
		this.placeholderContainer.add(super.placeholder, BorderLayout.CENTER);
	}

	public LatLonBox setPlaceholderIcon(Icon placeholderIcon) {
		this.placeholderIcon = new JLabel(placeholderIcon);
		return this;
	}

	@Override
	public JTextFieldPlaceholder setPlaceholderTextColor(Color colorLine) {
		super.setPlaceholderTextColor(colorLine);
		return this;
	}

	@Override
	public JTextFieldPlaceholder setPlaceholderText(String text) {
		if (text == null)
			throw new IllegalArgumentException("Invalid text");
		super.placeholder.setText(text);
		if (this.placeholderIcon != null) {
			this.placeholderIcon.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
			this.placeholderContainer.add(this.placeholderIcon, BorderLayout.WEST);
		}
		this.placeholderContainer.add(super.placeholder, BorderLayout.CENTER);
		if (this.menuButton != null) {
			this.menuButton.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
			this.placeholderContainer.add(this.menuButton, BorderLayout.EAST);
		}
		repaint();
		return this;
	}

	public LatLonBox addMenuActions(MouseAdapter action) {
		this.menuButton.addMouseListener(action);
		return this;
	}

	public void addTooltip(String tip) {
		iconContainer.setToolTipText(tip);
	}

	private class On3DotClickAction extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent ev) {
			if (!isLatLonMenuCreated) {
			    createCoordOrderMenuAndAddToPopup();
			}
			latlonpopup.getPopupmenu().show(ev.getComponent(), ev.getX(), ev.getY());
		}

		private void createCoordOrderMenuAndAddToPopup() {
			JMenu menuCoordOrder = new JMenu(COORD_ORDER.getMenuCommand());	
			menuCoordOrder.setIcon(menuicon);
			JRadioButtonMenuItem latlon = new JRadioButtonMenuItem(Ordering.LAT_LON.getOrderingLabel());
			JRadioButtonMenuItem lonlat = new JRadioButtonMenuItem(Ordering.LON_LAT.getOrderingLabel());			
			ButtonGroup group = new ButtonGroup();
			group.add(latlon);
			group.add(lonlat);		
			menuCoordOrder.add(latlon); 
			menuCoordOrder.add(lonlat);				
			latlon.addActionListener(Main.coordhandler);
			lonlat.addActionListener(Main.coordhandler);
			String coordOrdering = Config.get(Config.CONFIG_LAT_LON, Ordering.LAT_LON.asString());
			if (Ordering.LAT_LON.asString().equals(coordOrdering)) {
				latlon.setSelected(true);
			} else {
				lonlat.setSelected(true);
			}
			menuCoordOrder.addMenuListener(new MenuListener() {
				@Override
				public void menuSelected(MenuEvent e) {
					String coordOrdering = Config.get(Config.CONFIG_LAT_LON, Ordering.LAT_LON.asString());
					if (Ordering.LAT_LON.asString().equals(coordOrdering)) {
						latlon.setSelected(true);
					} else {
						lonlat.setSelected(true);
					}
				}
				@Override
				public void menuDeselected(MenuEvent e) {
				}
				@Override
				public void menuCanceled(MenuEvent e) {
				}
			});
			
			latlonpopup.getPopupmenu().add(menuCoordOrder);  
			isLatLonMenuCreated = true;
		}
	}
}
