package edu.asu.jmars.swing.landmark.search.popup;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import edu.asu.jmars.swing.quick.menu.command.CommandExecutor;
import edu.asu.jmars.swing.quick.menu.command.QuickMenuCommand;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import static edu.asu.jmars.swing.landmark.search.popup.MenuCommand.BOOKMARK_PLACE;
import static edu.asu.jmars.swing.landmark.search.popup.MenuCommand.SEARCH_PLACES;
import static edu.asu.jmars.swing.landmark.search.popup.MenuCommand.EXPLORE_NOM;
import static edu.asu.jmars.swing.landmark.search.popup.MenuCommand.SEARCH_NOM;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.NOMENCLATURE_LAYER_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.SEARCH;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.BOOKMARK;


public class LatLonPopup extends JPanel {

	private JPopupMenu lqapopup;	

	CommandExecutor knowsWhatTodo = new CommandExecutor();
	
	public LatLonPopup() {

		lqapopup = new JPopupMenu();
		
		buildMenuCommands();

		ActionListener menuListener = (ActionEvent event) ->  {
			String cmd = event.getActionCommand();
			knowsWhatTodo.processRequest(MenuCommand.get(cmd));			
		};		
		
		JMenuItem item;		
		Color imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
		Icon menuicon = new ImageIcon(ImageFactory.createImage(BOOKMARK.withDisplayColor(imgLayerColor)));		
		lqapopup.add(item = new JMenuItem(BOOKMARK_PLACE.getMenuCommand(), menuicon));
		item.setHorizontalTextPosition(JMenuItem.RIGHT);		
		item.addActionListener(menuListener);
		
		menuicon = new ImageIcon(ImageFactory.createImage(SEARCH.withDisplayColor(imgLayerColor)));	
		lqapopup.add(item = new JMenuItem(SEARCH_PLACES.getMenuCommand(), menuicon));
		item.setHorizontalTextPosition(JMenuItem.RIGHT);	
		item.addActionListener(menuListener);		
		
		lqapopup.add(new JSeparator());
		
		imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
		menuicon = new ImageIcon(ImageFactory.createImage(NOMENCLATURE_LAYER_IMG.withDisplayColor(imgLayerColor)));
		lqapopup.add(item = new JMenuItem(EXPLORE_NOM.getMenuCommand(), menuicon));
		item.setHorizontalTextPosition(JMenuItem.RIGHT);	
		item.addActionListener(menuListener);		
		
		imgLayerColor = ((ThemeImages) GUITheme.get("images")).getLayerfill();
		menuicon = new ImageIcon(ImageFactory.createImage(SEARCH.withDisplayColor(imgLayerColor)));
		lqapopup.add(item = new JMenuItem(SEARCH_NOM.getMenuCommand(), menuicon));
		item.setHorizontalTextPosition(JMenuItem.RIGHT);	
		item.addActionListener(menuListener);	
		
		lqapopup.add(new JSeparator());
		
		lqapopup.setLabel("Bookmark, Places, Coordinates order");		
	}

	private void buildMenuCommands() {

		CommandReceiver knowsHowTodo = new CommandReceiver();

		QuickMenuCommand bookmarkPlaceCmd = new BookmarkCommand(knowsHowTodo);
		QuickMenuCommand searchPlacesCmd = new SearchPlacesCommand(knowsHowTodo);
		QuickMenuCommand exploreNomCmd = new ExploreNomCommand(knowsHowTodo);
		QuickMenuCommand searchNomCmd = new SearchNomCommand(knowsHowTodo);
		
		knowsWhatTodo.addRequest(BOOKMARK_PLACE, bookmarkPlaceCmd);
		knowsWhatTodo.addRequest(SEARCH_PLACES, searchPlacesCmd);
		knowsWhatTodo.addRequest(EXPLORE_NOM, exploreNomCmd);	
		knowsWhatTodo.addRequest(SEARCH_NOM, searchNomCmd);	
	}

	public JPopupMenu getPopupmenu() {
		return lqapopup;
	}

}

