package edu.asu.jmars.layer.stamp;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.EXTERNAL_LINK_IMG;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.MenuElement;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import edu.asu.jmars.layer.stamp.focus.FilledStampFocus;
import edu.asu.jmars.layer.stamp.functions.RenderFunction;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.emory.mathcs.backport.java.util.Collections;

public class StampMenu extends JMenu {
	static DebugLog log = DebugLog.instance();
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static ImageIcon webIcon = new ImageIcon(ImageFactory.createImage(EXTERNAL_LINK_IMG.withDisplayColor(imgColor)));	
			   								   				

		StampShape stamp = null;
		StampLayer stampLayer = null;
		FilledStampFocus focusFilled = null;
		
		String renderText = null;
		String separator = null;
		
		boolean selected=false;
		
		public StampMenu(StampLayer layer, FilledStampFocus focusFilled, StampShape stamp) {
			super("Stamp " + stamp.getId());
			this.stampLayer=layer;
			this.focusFilled=focusFilled;
			this.stamp=stamp;
			
			renderText = stampLayer.getParam(stampLayer.RENDER_TEXT);
			separator = stampLayer.getParam(stampLayer.RENDER_MENU_SEPARATOR);
		}
		
		public StampMenu(StampLayer layer, FilledStampFocus focusFilled) {
			super("Render Selected " + layer.getInstrument() + " Stamps");
			this.stampLayer=layer;
			this.focusFilled=focusFilled;
			
			renderText = stampLayer.getParam(stampLayer.RENDER_SELECTED_TEXT);
			separator = stampLayer.getParam(stampLayer.RENDER_MENU_SEPARATOR);
			selected=true;   // Render all selected stamps
		}
		
	    public Component[] getMenuComponents() {
	    	if (!initialized) {
	    		initSubMenu();
	    	} 
	    	
	    	return super.getMenuComponents();
	    }
		
	    private JMenuItem createMenu(String text, final String imageType) {
			JMenuItem renderMenu = new JMenuItem(renderText + text);
			if (selected) {
				renderMenu.addActionListener(new RenderFunction(stampLayer, focusFilled, imageType));
			} else {
				renderMenu.addActionListener(new ActionListener() {			
					public void actionPerformed(ActionEvent e) {
					    Runnable runme = new Runnable() {
					        public void run() {
					            focusFilled.addStamp(stamp, imageType);
					        }
					    };
	
				        SwingUtilities.invokeLater(runme);
					}
				});
			}
			return renderMenu;
	    }
	    
	    private JMenu findMenu(JMenu startMenu, String menuText) {
	    	if (separator!=null && separator.length()>0 && menuText.contains(separator)) {
	    		String topLevel = menuText.substring(0, menuText.lastIndexOf(separator)).trim();
				String rest = menuText.substring(menuText.lastIndexOf(separator)+1).trim();
	    		
				Component menus[] = startMenu.getMenuComponents();

				for (Component c : menus) {
					if (c instanceof JMenu) {
						JMenu menu = (JMenu)c;
						if (menu.getText().equalsIgnoreCase(topLevel)) {
							return findMenu(menu, rest);
						}					
					}
				}
				
				JMenu menu = new JMenu(topLevel);
				startMenu.add(menu);
	    		return findMenu(menu, rest);
	    	} else {
				Component menus[] = startMenu.getMenuComponents();

				int count = 1;
				for (Component c : menus) {
					if (c instanceof JMenu) {
						JMenu menu = (JMenu)c;
						if (count++ > 30) {
							startMenu.setText(startMenu.getText()+" (items 1-30)");
						}
						if (menu.getText().equalsIgnoreCase(menuText)) {
							return menu;
						}					
					}
				}
				
				JMenu menu = new JMenu(menuText);
				startMenu.add(menu);
	    		return menu;
	    	}
	    }
	     
	    private void addMenu(final String imageType) {
			if (separator!=null && separator.length()>0 && imageType.contains(separator)) {
				String typeStart = imageType.substring(0, imageType.lastIndexOf(separator)).trim();
				String typeEnd = imageType.substring(imageType.lastIndexOf(separator)+1).trim();

				JMenuItem renderMenu = createMenu(typeEnd, imageType);
				
				JMenu menu=findMenu(this, typeStart);
				menu.add(renderMenu);
			} else {
				JMenuItem renderMenu = createMenu(imageType, imageType);
				add(renderMenu);
			}
	    }
	    
	    private void runStuff() {

			removeAll();		
				
			if (stampLayer.enableRender()) {
				List<String> supportedTypes;
				
				if (!selected) {
					supportedTypes = stamp.getSupportedTypes();
				} else {
					supportedTypes = getImageTypes();
				}
				
				Collections.sort(supportedTypes);
		
				for (String imageType : supportedTypes) {
					addMenu(imageType);
				}
								
				if (supportedTypes.size()==0) {
					JMenuItem noRenderOptions = new JMenuItem(stampLayer.getParam(stampLayer.NO_RENDER_OPTION_SINGLE));
					noRenderOptions.setEnabled(false);
					add(noRenderOptions);
				}
			
			}
			
			if (!selected && stampLayer.enableWeb()) {
				JMenuItem webBrowse = new JMenuItem(stampLayer.getParam(stampLayer.WEBBROWSE1_TEXT) + stamp.getId());
				
				webBrowse.addActionListener(new ActionListener() {			
					public void actionPerformed(ActionEvent e) {
						browse(stamp, 1);
					}			
				});
				
				webBrowse.setIcon(webIcon);
				webBrowse.setHorizontalTextPosition(SwingConstants.LEFT);
				webBrowse.setHorizontalAlignment(SwingConstants.LEFT);
				add(webBrowse);
	
				if (stampLayer.getParam(stampLayer.WEBBROWSE2_TEXT).length()>1) {
					JMenuItem webBrowse2 = new JMenuItem(stampLayer.getParam(stampLayer.WEBBROWSE2_TEXT) + stamp.getId());
					
					webBrowse2.addActionListener(new ActionListener() {			
						public void actionPerformed(ActionEvent e) {
							browse(stamp, 2);
						}			
					});				
					webBrowse2.setIcon(webIcon);
					webBrowse2.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
					add(webBrowse2);
				} 	
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (getPopupMenu().isVisible()) {
						getPopupMenu().setVisible(false);
						getPopupMenu().setVisible(true);
					}
				}
			});	
	    }
	    	
		boolean initialized = false;
		
		private synchronized void initSubMenu() {
			if (initialized) return;
			
			final JMenuItem loadingOptions = new JMenuItem("Retrieving render options - please wait");
			loadingOptions.setEnabled(false);
			add(loadingOptions);

			Thread thread = new Thread(new Runnable() {
				public void run() {
					runStuff();
				}
			});
			
			
			thread.setName("ImageTypeLookup");
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.setDaemon(true);
			thread.start();		
			
			initialized=true;			
		}
		
	    public MenuElement[] getSubElements() {		
	    	if (!initialized) {
	    		initSubMenu();
	    	}			
	 
	    	return super.getSubElements();
	    }
	    
	    public JPopupMenu getPopupMenu() {
	    	initSubMenu();
	    	return super.getPopupMenu();
	    }
	    
		public void browse(StampShape stamp, int num)
		{
			String url = null;
			
			try {
				String browseLookupStr = "BrowseLookup?id="+stamp.getId()+"&instrument="+stampLayer.getInstrument()+"&format=JAVA";
						
				ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(browseLookupStr));
				
				// Get the num-th URL
				for (int i=0; i<num; i++) {
					url = (String)ois.readObject();
				}
				
				ois.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (url == null) {
			    Util.showMessageDialog(
	                    "Sorry - that browse page is not currently available",
	                    "JMARS",
	                    JOptionPane.INFORMATION_MESSAGE);			
				return;
			}
	        
	    		Util.launchBrowser(url);
		}
		
		private List<String> getImageTypes() {
			List<String> imageTypes = new ArrayList<String>();

			try {						
				ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer("ImageTypeLookup", buildTypeLookupData()));

				List<String> supportedTypes = (List<String>)ois.readObject();

				ois.close();

				for (String type : supportedTypes) {				
					imageTypes.add(type);								
				}				
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			
			return imageTypes;
		}
		
		private String buildTypeLookupData() {
			String idList="";
			for (StampShape stamp : stampLayer.getSelectedStamps()) {
				idList+=stamp.getId()+",";
			}

			if (idList.endsWith(",")) {
				idList=idList.substring(0,idList.length()-1);
			}
			
			String data = "id="+idList+"&instrument="+stampLayer.getInstrument()+"&format=JAVA";
			
			return data;
		}

	}


