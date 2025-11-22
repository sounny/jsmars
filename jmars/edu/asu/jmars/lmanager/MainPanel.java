package edu.asu.jmars.lmanager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.swing.CompoundIcon;
import edu.asu.jmars.swing.ExclusionFocusTraversalPolicy;
import edu.asu.jmars.swing.LikeDefaultButtonUI;
import edu.asu.jmars.swing.quick.add.layer.LayerQuickAccessPopup;
import edu.asu.jmars.swing.quick.edit.row.RowQuickAccessPopup;
import edu.asu.jmars.tool.strategy.ProfileToolStrategy;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUIState;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeButton;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;


public class MainPanel extends JLayeredPane implements ComponentListener {
    public static final int NUMBER_OF_OVERLAY_LAYERS = 5;
    private static int ADD_LAYER_DIALOG_OFFSET = 8;
    private Row dragRow;
    private Row curRow;
    int dragPos;
    int dragSrc;
    int dragDst;
    public List<Row> rows = new ArrayList<Row>();
    public List<Row> overlayRows = new ArrayList<Row>();   
    private JButton addLayerButton;
    private JCheckBoxMenuItem tooltip, tooltipsItem;
    
    
    private LayerQuickAccessPopup lqapopup = new LayerQuickAccessPopup();
    private RowQuickAccessPopup rowquickmenu = new RowQuickAccessPopup();
    
    
    private JMenuBar hiddenShortcutMenubar = new JMenuBar();
    private ExclusionFocusTraversalPolicy policy = new ExclusionFocusTraversalPolicy();

//    private AddLayerDialog addLayerDialog = null;
    
    
//    private int[] layerCounts = {0,0,0};
    private CompoundIcon icon = null;
    private CompoundIcon hoverIcon = null;
    private CompoundIcon closeAddLayerIcon = null;
    private CompoundIcon hoverCloseAddLayerIcon = null;
    private JSplitPane rowAndOverlaySplit;
	
	private boolean startupFlag = true;
    private boolean expandedFlag = true;
    public JScrollPane rowScrollPane;
    private JScrollPane overlayScrollPane;
    public JLayeredPane rowsPanel = new JLayeredPane() {
        public void layout() {
            if (rows.isEmpty())
                return;

            Insets insets = rowsPanel.getInsets();
            int h = ((Row) rows.get(0)).getPreferredSize().height;
            int w = rowsPanel.getWidth() - insets.left - insets.right;

            for (int i = 0; i < rows.size(); i++) {
                Row r = (Row) rows.get(i);

                int y;
                if (i == dragSrc)
                    y = dragPos;
                else if (i > dragSrc && i <= dragDst)
                    y = h * (i - 1);
                else if (i < dragSrc && i >= dragDst)
                    y = h * (i + 1);
                else
                    y = h * i;
                r.setSize(w, h);
                r.setLocation(insets.left, insets.top + y);
            }
        }

        public Dimension getPreferredSize() {
            int width = 0;
            int height = 0;
            Dimension size = new Dimension(width, height);

            if (rows.size() == 0)
                return size;
            int h = ((Row) rows.get(0)).getPreferredSize().height;
            height = rows.size() * h;
            width = 1; // make it expand to take whatever space is available
            size.setSize(width, height);
            return size;
        }

    };

    public JLayeredPane overlayRowsPanel = new JLayeredPane() {
        public void layout() {
            if (overlayRows.isEmpty())
                return;

            Insets insets = overlayRowsPanel.getInsets();
            int h = ((Row) overlayRows.get(0)).getPreferredSize().height;
            int w = overlayRowsPanel.getWidth() - insets.left - insets.right;

            for (int i = 0; i < overlayRows.size(); i++) {
                Row r = (Row) overlayRows.get(i);

                int y;
                if (i == dragSrc)
                    y = dragPos;
                else if (i > dragSrc && i <= dragDst)
                    y = h * (i - 1);
                else if (i < dragSrc && i >= dragDst)
                    y = h * (i + 1);
                else
                    y = h * i;
                r.setSize(w, h);
                r.setLocation(insets.left, insets.top + y);
            }
        }

        public Dimension getPreferredSize() {
            int width = 0;
            int height = 0;
            Dimension size = new Dimension(width, height);

            if (overlayRows.size() == 0)
                return size;
            int h = ((Row) overlayRows.get(0)).getPreferredSize().height;
            height = overlayRows.size() * h;
            width = 1; // make it expand to take whatever space is available
            size.setSize(width, height);
            return size;
        }

    };

    public void enable3dForEmmPlanning() {
    	boolean planningLayerFound = false;
    	boolean mapFound = false;
    	for (Row r : rows) {
    		LView view = r.getView();
    		if (view.originatingFactory.type.equalsIgnoreCase("emm_planning")) {
    			r.turnOn3d();
    			planningLayerFound = true;
    		}
    		if (view.originatingFactory.type.equalsIgnoreCase("map")) {
    			r.turnOn3d();
    			mapFound = true;
    		}
    		if (mapFound && planningLayerFound) {
    			break;
    		}
    	}
    }

    public void enable3dForLtesPlanning() {
    	boolean planningLayerFound = false;
    	boolean mapFound = false;
    	for (Row r : rows) {
    		LView view = r.getView();
    		if (view.originatingFactory.type.equalsIgnoreCase("ltes_planning")) {
    			r.turnOn3d();
    			planningLayerFound = true;
    		}
    		if (view.originatingFactory.type.equalsIgnoreCase("map")) {
    			r.turnOn3d();
    			mapFound = true;
    		}
    		if (mapFound && planningLayerFound) {
    			break;
    		}
    	}
    }

    public void enable3dForClipperPlanning() {
	    	boolean planningLayerFound = false;
	    	boolean mapFound = false;
	    	for (Row r : rows) {
	    		LView view = r.getView();
	    		if (view.originatingFactory.type.equalsIgnoreCase("clipper_plan")) {
	    			r.turnOn3d();
	    			planningLayerFound = true;
	    		}
	    		if (view.originatingFactory.type.equalsIgnoreCase("map")) {
	    			r.turnOn3d();
	    			mapFound = true;
	    		}
	    		if (mapFound && planningLayerFound) {
	    			break;
	    		}
	    	}
    }

    public void delete(int selectedIdx) {
        // Remove the row... the row indices are reversed relative to everything
        // else.
        Row r = (Row) rows.remove(rows.size() - 1 - selectedIdx);
        rowsPanel.remove(r);
        rowsPanel.repaint();
        if (rowScrollPane != null) {
            rowScrollPane.revalidate();
            rowScrollPane.repaint();
        }
    }

    public void resetAddLayerButtonIcons() {
    	addLayerButton.setIcon(icon);
        addLayerButton.setRolloverIcon(hoverIcon);
    }
    private JButton getAddBtn() {
        addLayerButton = new JButton("Layers");
        addLayerButton.addMouseListener(addbtnMouseListener);
        Color imgColor = ((ThemeImages) GUITheme.get("images")).getCommonFill();
		Color imgHover = ((ThemeButton) GUITheme.get("button")).getThemehilightbackground();
		ImageIcon newlayerIcon = new ImageIcon(
                ImageFactory.createImage(ImageCatalogItem.NEW_LAYER.withDisplayColor(imgColor)));
		ImageIcon newlayerHoverIcon = new ImageIcon(
				ImageFactory.createImage(ImageCatalogItem.NEW_LAYER.withDisplayColor(imgHover)));	
		ImageIcon newCloseLayerIcon = new ImageIcon(
				ImageFactory.createImage(ImageCatalogItem.ADD_LAYER_CLOSE.withDisplayColor(imgColor).withWidth(32).withHeight(32)));
		ImageIcon hoverCloseLayerIcon = new ImageIcon(
				ImageFactory.createImage(ImageCatalogItem.ADD_LAYER_CLOSE.withDisplayColor(imgHover).withWidth(32).withHeight(32)));
		ImageIcon ellipse = new ImageIcon(
                ImageFactory.createImage(ImageCatalogItem.ELLIPSE_MENU.withDisplayColor(imgColor)));
        addLayerButton.setHorizontalTextPosition(SwingConstants.LEFT);
        addLayerButton.setHorizontalAlignment(SwingConstants.LEFT);
        addLayerButton.setIconTextGap(10);
		icon = new CompoundIcon(CompoundIcon.Axis.X_AXIS, 15, newlayerIcon, ellipse);
        icon.setUseWidthOfComponent(true);
        icon.setWidthOfComponentOffset(63);
		hoverIcon = new CompoundIcon(CompoundIcon.Axis.X_AXIS, 15, newlayerHoverIcon, ellipse);
		hoverIcon.setUseWidthOfComponent(true);
		hoverIcon.setWidthOfComponentOffset(63);		
		closeAddLayerIcon = new CompoundIcon(CompoundIcon.Axis.X_AXIS, 15, newCloseLayerIcon, ellipse);
        closeAddLayerIcon.setUseWidthOfComponent(true);
        closeAddLayerIcon.setWidthOfComponentOffset(63);
        hoverCloseAddLayerIcon = new CompoundIcon(CompoundIcon.Axis.X_AXIS, 15, hoverCloseLayerIcon, ellipse);
        hoverCloseAddLayerIcon.setUseWidthOfComponent(true);
        hoverCloseAddLayerIcon.setWidthOfComponentOffset(63);
		addLayerButton.setIcon(icon);		
		addLayerButton.setUI(new LikeDefaultButtonUI());
        return addLayerButton;
    }

    public Point getAddButtonLocation() {
        return addLayerButton.getLocation();
    }

    public int getAddButtonHeight() {
    	return addLayerButton.getHeight();
    }
    public MainPanel() {
        SearchProvider.prepareSearch();
        getAddBtn();

        Main.mainFrame.getRootPane().setDefaultButton(addLayerButton);

        
        rowScrollPane = new JScrollPane(rowsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rowScrollPane.getVerticalScrollBar().setUnitIncrement(20);
        
        overlayScrollPane = new JScrollPane(overlayRowsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        overlayScrollPane.getVerticalScrollBar().setUnitIncrement(20);
//        overlayScrollPane.setPreferredSize(new Dimension(300,300));
        overlayScrollPane.setViewportView(overlayRowsPanel);
        
        final int minimized_value = 50;
        final int maximized_value = 353;
        
        JPanel olPnl = new JPanel();
        JLabel title = new JLabel("Overlays");
        Color arrowColor = Color.WHITE;
        Color overlayColor = ((ThemePanel) GUITheme.get("panel")).getMidContrast();
        String theme = GUIState.getInstance().themeAsString();
		if (GUITheme.LIGHT.asString().equalsIgnoreCase(theme)) {
        	arrowColor = Color.GRAY;
        	overlayColor = Color.decode("#D3D2D2");
        }		
        ImageIcon collapsed = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.COLLAPSED_IMG.withDisplayColor(arrowColor)));
        ImageIcon expanded = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.EXPANDED_IMG.withDisplayColor(arrowColor)));
        
        JLabel collapsedLbl = new JLabel(collapsed);
        JLabel expandedLbl = new JLabel(expanded);
        
        olPnl.setBackground(overlayColor); 
        GroupLayout olLayout = new GroupLayout(olPnl);
        olPnl.setLayout(olLayout);
        olLayout.setHorizontalGroup(olLayout.createSequentialGroup()
        	.addGap(10)
        	.addComponent(title)
        	.addGap(20, 200, Short.MAX_VALUE)
        	.addComponent(expandedLbl)
        	.addGap(10));
        olLayout.setVerticalGroup(olLayout.createParallelGroup(Alignment.CENTER)
            .addComponent(title, 40, 40, 40)
            .addComponent(expandedLbl));
        
        
        olPnl.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent arg0) {
				if (arg0.getButton() == MouseEvent.BUTTON1) {
					int minSize = rowAndOverlaySplit.getHeight() - minimized_value;
					if (rowAndOverlaySplit.getDividerLocation() < minSize) {
						//minimize it
						rowAndOverlaySplit.setDividerLocation(minSize);
					} else {
						rowAndOverlaySplit.setDividerLocation(rowAndOverlaySplit.getHeight() - maximized_value);
					}
				}
			}
		});
        
        JPanel mainOverlayPnl = new JPanel();
        
        mainOverlayPnl.setLayout(new BorderLayout());
        mainOverlayPnl.add(olPnl, BorderLayout.NORTH);
        mainOverlayPnl.add(overlayScrollPane, BorderLayout.CENTER);
        
        rowAndOverlaySplit = new JSplitPane();
        rowAndOverlaySplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
        rowAndOverlaySplit.setTopComponent(rowScrollPane);
        rowAndOverlaySplit.setBottomComponent(mainOverlayPnl);
        rowAndOverlaySplit.setResizeWeight(1.0);
        
        Color dragColor = Color.WHITE;
		if (GUITheme.LIGHT.asString().equalsIgnoreCase(theme)) {
        	dragColor = Color.GRAY;
        }		
        ImageIcon dividerIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DRAG_HANDLES.withDisplayColor(dragColor)));
        JLabel dividerLbl = new JLabel(dividerIcon) {
        	@Override
        	public void paintComponent(Graphics g) {
        		//the drag handles image is vertical. For horizontal dividers, we need to rotate it.
        		Graphics2D g2 = (Graphics2D) g;
        		g2.rotate(Math.toRadians(90.0), getX() + getWidth()/2, getY() + getHeight() / 2);
        		super.paintComponent(g);
        	}
        };

        BasicSplitPaneDivider divider = ((BasicSplitPaneUI)rowAndOverlaySplit.getUI()).getDivider();
        divider.setLayout(new BorderLayout());
        divider.add(dividerLbl, BorderLayout.CENTER);
        overlayScrollPane.setMaximumSize(getPreferredSize());
        
        startupFlag = true;
        
        PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent changeEvent) {
            	
            	String propertyName = changeEvent.getPropertyName();
	            if (propertyName.equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
	            	int splitPaneHeight = rowAndOverlaySplit.getHeight();
	            	if (startupFlag) {
	            		int div = Config.get("overlayDividerLoc", maximized_value);
	            		div = splitPaneHeight - div;
	            		rowAndOverlaySplit.setDividerLocation(div);
	            	} else {
		            	Integer last = (Integer) changeEvent.getNewValue();
		            	int temp = splitPaneHeight - last;
		            	Config.set("overlayDividerLoc", String.valueOf(temp));
		            	if (temp > minimized_value) {
		            		if (!expandedFlag) {
		            			olLayout.replace(collapsedLbl, expandedLbl);
		            			expandedFlag = true;
		            		}
		            	} else {
		            		if (expandedFlag) {
		            			olLayout.replace(expandedLbl, collapsedLbl);
		            			expandedFlag = false;
		            		}
		            	}
	            	}
	            	startupFlag = false;//reset on first execution
	            	
	            }
            }
        };

        rowAndOverlaySplit.addPropertyChangeListener(propertyChangeListener);
        
		// add all desired components we want to skip tabbing on
		policy.addExcludedComponent(overlayRowsPanel);
		setFocusTraversalPolicy(policy);
		setFocusTraversalPolicyProvider(true);
        
        hiddenShortcutMenubar.add(rowquickmenu.getPopupmenu());      
        
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);  
        layout.setAutoCreateGaps(true);
        layout.setHonorsVisibility(true);
        Dimension hiddenDim = new Dimension(0, 0);
        hiddenShortcutMenubar.setSize(hiddenDim);

        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        	.addComponent(hiddenShortcutMenubar)
            .addComponent(addLayerButton, 5, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
            .addComponent(rowAndOverlaySplit, 5, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createSequentialGroup()
        	.addComponent(hiddenShortcutMenubar)	
            .addComponent(addLayerButton)
            .addComponent(rowAndOverlaySplit));
    }

 
    Action actTooltip = new AbstractAction("Show Tooltip") {
        public void actionPerformed(ActionEvent e) {
            boolean show = true;
            ;
            if (e.getSource() == tooltip) {
                show = tooltip.isSelected();
            }
            if (e.getSource() == tooltipsItem) {
                show = tooltipsItem.isSelected();
            }
            LManager.getLManager().showTooltipForSelectedLayer(show);

        };
    };

    MouseListener addbtnMouseListener = new MouseAdapter() {
        public void mousePressed(MouseEvent mouseEvent) {
            int modifiers = mouseEvent.getModifiers();
            if ((modifiers & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK) {
                AddLayerDialog.getInstance().initializeAddLayerDialog();
                int pointInButton = ((Double)mouseEvent.getPoint().getX()).intValue();
                int ellipseIconStart = icon.getLastIconStart();
                int ellipseIconEnd = icon.getLastIconEnd();
                if ((pointInButton >= ellipseIconStart - 5) && (pointInButton <= ellipseIconEnd + 5)) {
                	AddLayerDialog.getInstance().closeAddLayerDialog();
                    lqapopup.getPopupmenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                } else {
                    if (AddLayerDialog.getInstance().isShowing()) {
                    	AddLayerDialog.getInstance().closeAddLayerDialog();
                    } else {
                        try {
                            Double x = MainPanel.this.addLayerButton.getLocationOnScreen().getX();
                            Double y = MainPanel.this.addLayerButton.getLocationOnScreen().getY();
                            Dimension d = MainPanel.this.addLayerButton.getSize();
                            Double buttonWidth = d.getWidth();
                            Dimension d2 = MainPanel.this.getSize();
                            Double mainPanelWidth = d2.getWidth();
                            
                            Double widthToUse = null;
                            if (Double.compare(mainPanelWidth, buttonWidth) < 0) {
                            	widthToUse = mainPanelWidth;
                            } else {
                            	widthToUse = buttonWidth;
                            }
                            
                            x += widthToUse;
                            x += ADD_LAYER_DIALOG_OFFSET;// offset guess
                            AddLayerDialog.getInstance().setAddLayerDialogLocation(x.intValue(), y.intValue());
                            AddLayerDialog.getInstance().setDialogSize();
//                            newAddLayerDialog.pack();
                            AddLayerDialog.getInstance().showAddLayerDialog();
                            addLayerButton.setIcon(closeAddLayerIcon);//to be changed to new icon
                            addLayerButton.setRolloverIcon(hoverCloseAddLayerIcon);
                        } catch (Exception e) {
                            //should never happen, but protect incase add layer button is not visible
                        	AddLayerDialog.getInstance().closeAddLayerDialog();
                        }
                    }
                }
            } 
            if (SwingUtilities.isRightMouseButton(mouseEvent)) {       
            	if (AddLayerDialog.getInstance().isShowing()) {
            		AddLayerDialog.getInstance().closeAddLayerDialog();
                }
                lqapopup.getPopupmenu().show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            }
        }
    };
    
    
    
    public void addView(Layer.LView view) {
        Row r = new Row(view, rowMouseHandler);
        r.createGUI();
        rowsPanel.add(r);
        rows.add(0, r);
    }

    public void addOverlayView(Layer.LView view) {
        Row r = new Row(view, overlayRowMouseHandler);
        r.createGUI();
        overlayRowsPanel.add(r);
        overlayRows.add(0, r);
    }

    public void revalidateOverlayPanel() {
        overlayRowsPanel.revalidate();
        overlayRowsPanel.repaint();
    }
    public void updateRows() {
        for (Iterator i = rows.iterator(); i.hasNext();)
            ((Row) i.next()).updateRow();
    }

    public void updateOverlayRows() {
        for (Iterator i = overlayRows.iterator(); i.hasNext();)
            ((Row) i.next()).updateCartographyRow();
    }

    void setDragOffset(int dragOffset) {
        if (dragOffset == 0 || rows.isEmpty()) {
            dragSrc = -1;
            dragDst = -1;
            dragPos = -1;
        } else {
            int h = ((Row) rows.get(0)).getPreferredSize().height;
            dragSrc = rows.indexOf(dragRow);
            dragPos = Util.bound(0, h * dragSrc + dragOffset, h * (rows.size() - 1));
            dragDst = (dragPos + h / 2 - 1) / h;
        }
    }


    public MouseInputListener rowMouseHandler = new MouseInputAdapter() {
        int pressed;

		public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isRightMouseButton(e)) {
				Component c = e.getComponent();
				if (c instanceof Row) {
					dragRow = (Row) c;
				} else {
					dragRow = (Row) c.getParent();
				}
				LManager.getLManager().setActiveLView(dragRow.getView());
				dragRow.rowpopup.getPopupmenu().show(e.getComponent(), e.getX(), e.getY());
			} else {
				if (e.getClickCount() != 2)
					return;
				if (Config.get("openDocked").equalsIgnoreCase("false")) {
					LManager.getLManager().accessSelectedOptions(false);
				} else {
					LManager.getLManager().accessSelectedOptions(true);
				}
			}
		}

        public void mousePressed(MouseEvent e) {
            Component c = e.getComponent();
            if (c instanceof Row) {
                dragRow = (Row) c;
            } else if (c instanceof JLabel) {
                // The label on the row is inside a JPanel ('top')
                // get top, and then get the row
                c = c.getParent();
                dragRow = (Row) c;
            }

            setDragOffset(0);
            pressed = screenY(e);
            rowsPanel.moveToFront(dragRow);
            LManager.getLManager().setActiveLView(dragRow.getView());
            dragRow.setToHoverAppearance();
            }

        public void mouseDragged(MouseEvent e) {
            setDragOffset(screenY(e) - pressed);
            rowsPanel.revalidate();
            rowsPanel.repaint();
            if (rowScrollPane != null) {
                rowScrollPane.revalidate();
                rowScrollPane.repaint();
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (dragSrc != dragDst) {
                int dragSrcRev = rows.size() - 1 - dragSrc;
                int dragDstRev = rows.size() - 1 - dragDst;

                // Move the user-visible row, the actual view list
                // order, and the focus tabs. The latter two are
                // in reverse order from the first one.
                Row rowToMove = rows.remove(dragSrc);
                rows.add(dragDst, rowToMove);
                LManager.getLManager().viewList.move(dragSrcRev, dragDstRev);
                LManager.getLManager().setActiveLView(dragDstRev);

                // update the 3d view if the row has 3d data
                LView3D view3d = rowToMove.getView().getLView3D();
                ThreeDManager mgr = ThreeDManager.getInstance();
                if (mgr.isReady() && view3d.isEnabled() && view3d.isVisible()) {
                    if (mgr.hasLayerDecalSet(view3d)) {
                        mgr.update();
                    }
                }
            }

            if (dragRow != null) {
            	dragRow.setToHoverAppearance();
            }
            dragRow = null;
            setDragOffset(0);
            rowsPanel.revalidate();
            rowsPanel.repaint();
            if (rowScrollPane != null) {
                rowScrollPane.revalidate();
                rowScrollPane.repaint();
            }

            overlayScrollPane.revalidate();
            overlayScrollPane.repaint();

        }

        public void mouseMoved(MouseEvent e) {        	       	
            Component c = e.getComponent();
            Row hoverRow = null;
            if (c instanceof Row) {
                hoverRow = (Row) c;
            } else if (c instanceof JLabel) {
                // The label on the row is inside a JPanel ('top')
                // get top, and then get the row
                hoverRow = (Row) c.getParent();
            }

            updateRows();

            if (hoverRow != null) {
                hoverRow.setToHoverAppearance();
            }
        }

		public void mouseExited(MouseEvent e) {
			updateRows();			
			try {
    			Point2D screenPt = e.getLocationOnScreen();
    			Point uL = curRow.getLocationOnScreen();
    			Rectangle2D rect = new Rectangle2D.Double(uL.x, uL.y, curRow.getWidth(), curRow.getHeight());
    			if (rect.contains(screenPt)) {
    				curRow.setToHoverAppearance();
    			} else {
    				curRow.setToNormalAppearance();
    			}
			} catch (Exception e1) {
			    //do nothing
			}
		}        
      

		public void mouseEntered(MouseEvent e) {
			Component c = e.getComponent();
			if (c instanceof Row) {
				curRow = (Row) c;
			} else if (c instanceof JLabel) {
				curRow = (Row) c.getParent();
			}
			updateRows();
			if (curRow != null) {
				curRow.setToHoverAppearance();
			}
		}
    };
    
    

    public MouseInputListener overlayRowMouseHandler = new MouseInputAdapter() {
        int pressed;

        public void mouseClicked(MouseEvent e) {
			if (SwingUtilities.isRightMouseButton(e)) {
				Component c = e.getComponent();
				if (c instanceof Row) {
					dragRow = (Row) c;
				} else {
					dragRow = (Row) c.getParent();
				}
				LManager.getLManager().setActiveLView(dragRow.getView());
				dragRow.rowpopup.getPopupmenu().show(e.getComponent(), e.getX(), e.getY());
			} else {
			    if (e.getClickCount() != 2)
                    return;
                if (Config.get("openDocked").equalsIgnoreCase("false")) {
                    LManager.getLManager().accessSelectedOptions(false);
                } else {
                    LManager.getLManager().accessSelectedOptions(true);
                }
            }
        }

        public void mousePressed(MouseEvent e) {
            Component c = e.getComponent();
            if (c instanceof Row) {
                dragRow = (Row) c;
            } else {
                dragRow = (Row) c.getParent();
            }
            LManager.getLManager().setActiveLView(dragRow.getView());           
        }

		public void mouseDragged(MouseEvent e) {
		}
 
        public void mouseReleased(MouseEvent e) {    
            updateOverlayRows();
            overlayRowsPanel.revalidate();
            overlayRowsPanel.repaint();
         }

        public void mouseMoved(MouseEvent e) {
            Component c = e.getComponent();
            Row hoverRow = null;
            if (c instanceof Row) {
                hoverRow = (Row) c;
            } else if (c instanceof JLabel) {  
                hoverRow = (Row) c.getParent();
            }
            updateOverlayRows();
            if (hoverRow != null) {
                hoverRow.setToHoverAppearance();
            }
        }

        public void mouseExited(MouseEvent e) {
        	updateRows();			
			Point2D screenPt = e.getLocationOnScreen();
			if (curRow != null && curRow.isShowing())
			{
			    Point uL = curRow.getLocationOnScreen();
			    Rectangle2D rect = new Rectangle2D.Double(uL.x, uL.y, curRow.getWidth(), curRow.getHeight());
			    if (rect.contains(screenPt)) {
				    curRow.setToHoverAppearance();
			    } else {
				    curRow.setToNormalAppearance();
			    } 
			}
        }

        public void mouseEntered(MouseEvent e) {
            Component c = e.getComponent();
            if (c instanceof Row) {
                curRow = (Row) c;
            } else if (c instanceof JLabel) {              
                curRow = (Row) c.getParent();
            }
            updateOverlayRows();
            if (curRow != null) {
                curRow.setToHoverAppearance();
            }
        }
    };

    private static int screenY(MouseEvent e) {
        Point pt = e.getPoint();
        SwingUtilities.convertPointToScreen(pt, e.getComponent());
        return pt.y;
    }

    @Override
    public void componentResized(ComponentEvent e) {
        resetAddLayerDialog();
        Main.testDriver.locMgr.resetSearchlandmarkDialog();
        ProfileToolStrategy.resetProfileSnackbar();
        Main.testDriver.resetDistanceNotifSnackbar();
    }
    
    @Override
    public void componentMoved(ComponentEvent e) {
        resetAddLayerDialog();
        Main.testDriver.locMgr.resetSearchlandmarkDialog();
        ProfileToolStrategy.resetProfileSnackbar();
        Main.testDriver.resetDistanceNotifSnackbar();
    }

    @Override
	public void componentShown(ComponentEvent e) {
	}

    @Override
	public void componentHidden(ComponentEvent e) {
	}

    public void resetAddLayerDialog() {
        try {
            if (AddLayerDialog.getInstance().isShowing()) {
            	Double x = MainPanel.this.addLayerButton.getLocationOnScreen().getX();
                Double y = MainPanel.this.addLayerButton.getLocationOnScreen().getY();
                Dimension d = MainPanel.this.addLayerButton.getSize();
                Double buttonWidth = d.getWidth();
                Dimension d2 = MainPanel.this.getSize();
                Double mainPanelWidth = d2.getWidth();
                
                Double widthToUse = null;
                if (Double.compare(mainPanelWidth, buttonWidth) < 0) {
                	widthToUse = mainPanelWidth;
                } else {
                	widthToUse = buttonWidth;
                }
                
                x += widthToUse;
                x += ADD_LAYER_DIALOG_OFFSET;// offset guess
                AddLayerDialog.getInstance().setAddLayerDialogLocation(x.intValue(), y.intValue());
                AddLayerDialog.getInstance().setDialogSize();
//                newAddLayerDialog.pack();
            }
        } catch (Exception e) {
            //add layer button is not showing (likely), can the right location for resize, close the dialog
        	AddLayerDialog.getInstance().closeAddLayerDialog();
        }
    }

	public JPanel getComponentForLView(LView lv) {
		JPanel panel = null;
		for (Row r : rows) {
    		if (r.getView() == lv) {
    			panel = r;
    			break;
    		}
		}
		return panel;
	}
    
   
}
