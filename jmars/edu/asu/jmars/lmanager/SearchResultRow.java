package edu.asu.jmars.lmanager;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.plaf.ComponentUI;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.WordUtils;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.custom.CustomMap;
import edu.asu.jmars.layer.map2.custom.CustomMapBackendInterface;
import edu.asu.jmars.layer.map2.stages.ContourStageSettings;
import edu.asu.jmars.swing.ColorCombo;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeButton;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeText;
import mdlaf.components.button.MaterialButtonUI;


public class SearchResultRow extends JPanel implements Comparable {
	public static final int LAYER_TYPE_STAMP = 1;
	public static final int LAYER_TYPE_SAVED_LAYER = 2;
	public static final int LAYER_TYPE_SHAPE = 3;
	public static final int LAYER_TYPE_OTHER = 4;
	public static final int LAYER_TYPE_MOSAIC = 5;
    public static final int LAYER_NAME_PANEL_WIDTH = 350;
    public static final int ICON_HEIGHT = 17; //Buttons on the result row must be this height to not seem like they are being highlighted by hover effect
    private ArrayList<LayerParameters> layerList;
    private LayerParameters layer = null;
    private static Color imgIconColor = ((ThemeImages) GUITheme.get("images")).getSelectedfill();
    private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();    
    private static Color border = ((ThemePanel) GUITheme.get("panel")).getBackgroundhi();
    private static Color disabledtext = ((ThemeText) GUITheme.get("text")).getTextDisabled();
    private static Color categoryTextColor = ((ThemeText) GUITheme.get("text")).getNullText();
    private static Color buttonHighlightColor = ((ThemeButton) GUITheme.get("button")).getAltOnhover();
    private static Color buttonColor = ((ThemeButton) GUITheme.get("button")).getThemebackground();
    private static HashMap<String,Icon> typeIcons = new HashMap<String, Icon>(); 
    private boolean builtFlag = false;
    private boolean favoriteFlag = false;
    private JLabel categorization = null;
    private FavoriteButton favoriteButton = null;
    private ContourButton contourButton = null;
    private boolean isCustom = false;     
    private JLabel layerNameLbl = new JLabel();
    private JLabel typeLbl = new JLabel();
    private JLabel layerTooltip = new JLabel();
    private String tooltipText = null;
//    private static Icon plusIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.ADD_LAYER.withStrokeColor(imgColor)));           
    private static Icon tooltipIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.INFO.withDisplayColor(imgColor)));            
    private static Icon favoriteIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.FAVORITE.withStrokeColor(imgColor)));           
    private static Icon favoriteSelectedIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.FAVORITED.withDisplayColor(imgIconColor)));
    private static Icon favoriteDisabledIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.FAVORITE.withStrokeColor(disabledtext))); 
    private static final String separator = "|";
    private static final String space = "  ";
    private boolean isMapSource = false;
    private MapSource mapSource = null;
    private JDialog contourDialog = null;
    private ColorCombo colorList = null;
    private JButton cancelBtn = null;
    private JButton loadBtn = null;
    private JLabel baseLbl = null;
    private JLabel stepLbl = null;
    private JLabel colorLbl = null;
    private JLabel thicknessLbl = null;
    private JTextField baseVal = null;
    private JTextField stepVal = null;
    private JComboBox<String> thicknessVal = null;
	private ContourStageSettings contourSettings = null;
	private Integer orderWeight = -1;//stamps,saved layers,shapes,other,mosaics
	
	public static final int ACTION_MAIN = 1;
	public static final int ACTION_ADVANCED = 2;
	public static final int ACTION_FAVORITE = 3;
	private int selectedAction = 0;
    
    private JButton mainButton = new JButton();
    
    private static Icon contourIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CONTOUR.withStrokeColor(imgColor).withHeight(ICON_HEIGHT)));
    public SearchResultRow(LayerParameters layer, boolean favorite) {
        if (this.layerList == null) {
            this.layerList = new ArrayList<LayerParameters>();
            this.layerList.add(layer);
        }
        this.layer = layer;
        this.favoriteFlag = favorite;        
        mainButton.addActionListener(new AddLayerActionListener(this.layer, this.isCustom));
        mainButton.setUI(new MyIconButtonUI());
    }
    
    public SearchResultRow(CustomMap map, boolean favorite) {
    	this(map.getLayerParameters(), favorite);
    }
    
    public SearchResultRow(MapSource source, boolean favorite) {
    	this.isMapSource = true;
    	this.mapSource = source;
    	this.favoriteFlag = favorite;
    	mainButton.addActionListener(new AddLayerActionListener(this.mapSource,false, null));
    	mainButton.setUI(new MyIconButtonUI());
    }
    public SearchResultRow(ArrayList<LayerParameters> layerData, boolean favorite) {
        this(layerData.get(0),favorite);
        this.layerList = layerData;      
    }
    
    public void resetHighlight() {
    	mainButton.setBackground(buttonColor);
    	if (contourButton != null) {
    		contourButton.setBackground(buttonColor);
    	}
    	if (favoriteButton != null) {
    		favoriteButton.setBackground(buttonColor);
    	}
    	selectedAction = 0;
    }
    public int getSelectedAction() {
    	return selectedAction;
    }
    public void executeAction(Point p) {
    	if (p.x <= mainButton.getWidth()) {
    		mainButton.doClick();
    	} else {
    		if (contourButton != null) {
    			if (p.x >= contourButton.getBounds().x && p.x <= favoriteButton.getBounds().x) {
    				contourButton.doClick();
		    	}
    		}
    		if (favoriteButton != null && favoriteButton.isEnabled()) {
    			if (p.x >= favoriteButton.getBounds().x) {
    				favoriteButton.doClick();
    			}
    		}
    	}
    }
    public void executeSelectedAction() {
    	if (selectedAction == ACTION_MAIN) {
    		mainButton.doClick();
    	} else if (selectedAction == ACTION_ADVANCED) {
    		contourButton.doClick();
    	} else if (selectedAction == ACTION_FAVORITE) {
    		favoriteButton.doClick();
    	}
    }
    public void highlightForSelection(Point p) {
    	if (contourButton == null) {
    		if (p.x <= favoriteButton.getBounds().x) {
    			highlightMainButtonForSelection();
    		}
    	} else {
    		if (p.x <= contourButton.getBounds().x) {
        		highlightMainButtonForSelection();
        	} else if (p.x <= favoriteButton.getBounds().x) {
				highlightAdvancedButtonForSelection();
	    	}
    	}	
		if (favoriteButton != null && favoriteButton.isEnabled()) {
			if (p.x >= favoriteButton.getBounds().x) {
				highlightFavoriteButtonForSelection();
			}
		}
    }
    public void pushHighlightRightForSelection() {
    	if (selectedAction == ACTION_MAIN) {
    		highlightAdvancedButtonForSelection();//will do nothing if button not active
    		if (selectedAction == ACTION_ADVANCED) {
    			mainButton.setBackground(buttonColor);//reset main button
    		} else {
    			highlightFavoriteButtonForSelection();
    			if (selectedAction == ACTION_FAVORITE) {
    				mainButton.setBackground(buttonColor);
    			}
    		}
    	} else if (selectedAction == ACTION_ADVANCED) {
    		highlightFavoriteButtonForSelection();//will do nothing if button not active
    		if (selectedAction == ACTION_FAVORITE) {
    			contourButton.setBackground(buttonColor);//reset advanced button
    		} 
    	} 
    }
    public void pushHighlightLeftForSelection() {
    	if (selectedAction == ACTION_FAVORITE) {
    		highlightAdvancedButtonForSelection();//will do nothing if button not active
    		if (selectedAction == ACTION_ADVANCED) {
    			favoriteButton.setBackground(buttonColor);//reset main button
    		} else {
    			highlightMainButtonForSelection();
    			if (selectedAction == ACTION_MAIN) {
    				favoriteButton.setBackground(buttonColor);
    			}
    		}
    	} else if (selectedAction == ACTION_ADVANCED) {
    		highlightMainButtonForSelection();
    		if (selectedAction == ACTION_MAIN) {
    			contourButton.setBackground(buttonColor);//reset advanced button
    		}
    	} 
    }
    public void highlightMainButtonForSelection() {
    	mainButton.setBackground(buttonHighlightColor);
    	selectedAction = ACTION_MAIN;
    	repaint();
    }
    public void highlightAdvancedButtonForSelection() {
    	if (contourButton != null && contourButton.isEnabled() && contourButton.isShowing()) {
	    	contourButton.setBackground(buttonHighlightColor);
	    	selectedAction = ACTION_ADVANCED;
    	}
    	repaint();
    }
    public void highlightFavoriteButtonForSelection() {
    	if (favoriteButton != null && favoriteButton.isEnabled() && favoriteButton.isShowing()) {
	    	favoriteButton.setBackground(buttonHighlightColor);
	    	selectedAction = ACTION_FAVORITE;
    	}
    	repaint();
    }
    
    public String getTopic(String cat, String subcat) {
        if (this.isCustom) {
            return "Custom";
        }
        for (LayerParameters lp : layerList) {
            if (lp.category.equals(cat)) {
                if (subcat != null) {
                    if (subcat.equals(lp.subcategory)) {
                        return lp.topic;
                    }
                } else {
                    if (lp.subcategory == null || lp.subcategory.trim().length() == 0) {
                        return lp.topic;
                    }
                }
            }
        }
        return this.layer.topic;
    }
    public String getLayerId() {
    	//map sources do not have an associated LayerParameters object here. 
    	//so when searching through these result row, protect against NPE
    	if (this.layer != null) {
    		return this.layer.id;
    	}
    	return "";//good enough for map sources
    }
    public String getMapSourceTitle() {
    	return this.mapSource.getTitle();
    }
    public boolean isMapSource() {
    	return this.isMapSource;
    }
    public boolean isFavoriteIconOn() {
        return this.favoriteFlag;
    }
	private AbstractAction loadAction = new AbstractAction("LOAD") {
		@Override
		public void actionPerformed(ActionEvent e) {
			contourSettings.setColor(colorList.getColor());
			contourSettings.setBase(Integer.parseInt(baseVal.getText()));
			contourSettings.setStep(Double.parseDouble(stepVal.getText()));
			contourSettings.setLineThickness(Integer.parseInt((String)thicknessVal.getSelectedItem()));
			contourDialog.setVisible(false);
			contourDialog.dispose();
			new AddLayerActionListener(SearchResultRow.this.mapSource, true, contourSettings).actionPerformed(e);
		}
	};
	private AbstractAction cancelAction = new AbstractAction("CANCEL") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			contourDialog.setVisible(false);
			contourDialog.dispose();
		}
	};

    private ActionListener contourAction = new ActionListener() {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			contourDialog = new JDialog(AddLayerDialog.getInstance().getAddLayerDialog());
		    colorList = new ColorCombo();
		    cancelBtn = new JButton(cancelAction);
		    loadBtn = new JButton(loadAction);
		    baseLbl = new JLabel("Base:");
		    stepLbl = new JLabel("Step:");
		    colorLbl = new JLabel("Color:");
		    thicknessLbl = new JLabel("Line Thickness:");
		    baseVal = new JTextField();
		    stepVal = new JTextField();
		    thicknessVal = new JComboBox<String>(new String[] {"1","2","3"});
			contourSettings = new ContourStageSettings();
			
			colorList.setColor(Color.white);
			baseVal.setText("0");
			stepVal.setText("100");
			
			GroupLayout layout = new GroupLayout(contourDialog.getContentPane());
			contourDialog.getContentPane().setLayout(layout);
			layout.setAutoCreateContainerGaps(true);
			layout.setAutoCreateGaps(true);
			
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
				.addGroup(layout.createSequentialGroup()
					.addComponent(baseLbl)
					.addComponent(baseVal)
					.addComponent(stepLbl)
					.addComponent(stepVal))
				.addGroup(layout.createSequentialGroup()
					.addComponent(colorLbl)
					.addComponent(colorList)
					.addComponent(thicknessLbl)
					.addComponent(thicknessVal))
				.addGroup(layout.createSequentialGroup()
					.addComponent(cancelBtn)
					.addComponent(loadBtn)));
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(Alignment.CENTER)
						.addComponent(baseLbl)
						.addComponent(baseVal)
						.addComponent(stepLbl)
						.addComponent(stepVal))
					.addGroup(layout.createParallelGroup(Alignment.CENTER)
						.addComponent(colorLbl)
						.addComponent(colorList)
						.addComponent(thicknessLbl)
						.addComponent(thicknessVal))
					.addGap(10)
					.addGroup(layout.createParallelGroup(Alignment.CENTER)
						.addComponent(cancelBtn)
						.addComponent(loadBtn)));
			
			contourDialog.pack();
			contourDialog.getRootPane().setDefaultButton(loadBtn);
			contourDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			contourDialog.setTitle("Contour Options");
			contourDialog.setLocationRelativeTo(SearchResultRow.this);
			contourDialog.setVisible(true);
		}
	};
	
    public void toggleFavoriteIcon(boolean on) {
        if (!this.builtFlag) {
            this.buildRow();
        }
        if (on) {
            this.favoriteButton.changed(1);
        } else {
            this.favoriteButton.changed(0);
        }
        this.favoriteFlag = on;
    }
    public void rebuildRow() {
    	builtFlag = false;
    	buildRow();
    }
    private void buildMapSourceRow() {
    	if (builtFlag) {
            return;
        }
        builtFlag = true;                 
        
        if (this.favoriteFlag) {
            favoriteButton = new FavoriteButton(1,favoriteIcon, favoriteSelectedIcon);
        } else {
            favoriteButton = new FavoriteButton(0,favoriteIcon, favoriteSelectedIcon);
        }
        
        if (Main.USER == null || Main.USER.trim().length() == 0) {
            favoriteButton.setEnabled(false);
            favoriteButton.setToolTipText("Login to have access to favorites");
        }   
        
        Icon typeIcon = getImageIcon("map");   
        this.orderWeight = LAYER_TYPE_OTHER;
        
        typeLbl.setIcon(typeIcon);
        typeLbl.setOpaque(false);
        typeLbl.setToolTipText("Map: direct from map server");
        
        layerTooltip.setIcon(tooltipIcon);
        layerTooltip.setOpaque(false);
        
        tooltipText = "<html>";
        String layerDesc = "<name /><b><u>LAYER DESCRIPTION</u></b> <br />";//adding a <name /> tag for the layer manager row to inject the name to replace this placeholder
        layerDesc += "Numeric: ";
        if (this.mapSource.hasNumericKeyword()) {
        	layerDesc += "Yes";
        } else {
        	layerDesc += "No";
        }
        layerDesc += "<br />Elevation: ";
        if (this.mapSource.hasElevationKeyword()) {
        	layerDesc += "Yes";
        } else {
        	layerDesc += "No";
        }
        layerDesc += "<br />Geologic: ";
        if (this.mapSource.hasGeologicKeyword()) {
        	layerDesc += "Yes";
        } else {
        	layerDesc += "No";
        }
        
        layerDesc += "<br />"+this.mapSource.getAbstract();
        
        tooltipText += "<br /><p width=\"400px\">"+layerDesc+"</p><p width=\"400px\">";
        tooltipText += "</p></html>";
        layerTooltip.setToolTipText(tooltipText);        
        
        layerNameLbl.setOpaque(false);
        layerNameLbl.setText(this.mapSource.getTitle());        

        boolean addContourButton = false;
        if (this.mapSource != null && this.mapSource.hasNumericKeyword()) {
			addContourButton = true;
			contourButton = new ContourButton(contourIcon);
	        contourButton.addActionListener(contourAction);
		}

        this.layoutRow(addContourButton);

        this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, border));
    }
    private void layoutRow(boolean addContourButton) {
    	GroupLayout mainButtonLayout = new GroupLayout(this.mainButton);
        this.mainButton.setLayout(mainButtonLayout);
        mainButtonLayout.setHorizontalGroup(mainButtonLayout.createSequentialGroup()
    		.addGap(10,10,10)
            .addComponent(typeLbl,25,25,25)
            .addGap(10,10,10)
            .addComponent(layerTooltip)
            .addGap(20)
            .addGroup(mainButtonLayout.createParallelGroup(Alignment.LEADING)
            	.addComponent(layerNameLbl,LAYER_NAME_PANEL_WIDTH,LAYER_NAME_PANEL_WIDTH,LAYER_NAME_PANEL_WIDTH)
            	.addComponent(categorization)));
        mainButtonLayout.setVerticalGroup(mainButtonLayout.createParallelGroup(Alignment.CENTER)
    		.addComponent(typeLbl)
	        .addComponent(layerTooltip)
	        .addGroup(mainButtonLayout.createSequentialGroup()
	        	.addComponent(layerNameLbl)
	        	.addComponent(categorization)));
        
        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setAutoCreateContainerGaps(false);
        
        Group horizSeq = layout.createSequentialGroup()
    	.addComponent(this.mainButton);
        if (addContourButton) {
        	horizSeq.addGap(15)
        	.addComponent(contourButton, 47,47,47)
        	.addGap(28);
        } else {
        	horizSeq.addGap(90);
        }
        horizSeq.addComponent(favoriteButton, 47,47,47);
        layout.setHorizontalGroup(horizSeq);
        
        Group vertPar = layout.createParallelGroup(Alignment.CENTER)
		.addComponent(this.mainButton);
    	if (addContourButton) {
    		vertPar.addComponent(contourButton, 47,47,47);
    	}
    	vertPar.addComponent(favoriteButton, 47,47,47);
        
        layout.setVerticalGroup(vertPar);

        builtFlag = true;
    }
    public void buildRow() {
    	buildCategorization();
    	if (this.isMapSource) {
    		buildMapSourceRow();
    		return;
    	}
        if (builtFlag) {
            return;
        }               
        
        if (this.favoriteFlag) {
            favoriteButton = new FavoriteButton(1,favoriteIcon, favoriteSelectedIcon);
        } else {
            favoriteButton = new FavoriteButton(0,favoriteIcon, favoriteSelectedIcon);
        }
        
        if(Main.USER == null || Main.USER.trim().length() == 0) {
            favoriteButton.setEnabled(false);
            favoriteButton.setToolTipText("Login to have access to favorites");
            
            if(layer.type.equalsIgnoreCase("upload_map") || layer.type.contains("roi")
                    || layer.type.contains("plan") || layer.type.equalsIgnoreCase("tes") 
                    || layer.name.equalsIgnoreCase("custom stamps" )) {
            	layerNameLbl.setEnabled(false);
                this.setEnabled(false);
            }
        } else {
            //valid user in here
            if ((layer.type.equalsIgnoreCase("stamp") && layer.layergroup.equalsIgnoreCase("dawn_team"))) {
                if (Main.AUTH_DOMAIN.equalsIgnoreCase("dawn")) {
                    layerNameLbl.setEnabled(true);   
                    this.setEnabled(true);
                } else {
                      layerNameLbl.setEnabled(false);
                      this.setEnabled(false);
                }
            }
                
        }     
        
        
        Icon typeIcon = getImageIcon(layer.type);
        
        typeLbl.setIcon(typeIcon);
        typeLbl.setOpaque(false);
        String typeName = layer.type;
        if (typeName != null) {
        	typeName = typeName.replaceAll("_", " ");//saved_layer becomes saved layer
        	if (typeName.equalsIgnoreCase("3d")) {
        		typeName = "3D";
        	} else if (typeName.equalsIgnoreCase("krc")) {
        		typeName = "KRC";
        	} else {
        		typeName = WordUtils.capitalize(typeName);//proper case 
        	}
        }
        switch(typeName.toLowerCase()) {
        case "stamp":
        	orderWeight = LAYER_TYPE_STAMP;
        	break;
        case "saved layer":
        	orderWeight = LAYER_TYPE_SAVED_LAYER;
        	break;
        case "shape":
        	orderWeight = LAYER_TYPE_SHAPE;
        	break;
        case "mosaic":
        	orderWeight = LAYER_TYPE_MOSAIC;
        default:
        	orderWeight = LAYER_TYPE_OTHER;
//        	System.out.println("type: "+typeName);
        }
        
        typeLbl.setToolTipText(typeName);
        
        layerTooltip.setIcon(tooltipIcon);
        layerTooltip.setOpaque(false);
        
        tooltipText = "<html>";
        String layerDesc = "<name />";//adding a <name /> tag for the layer manager row to inject the name to replace this placeholder
        if (layer.description == null || layer.description.trim().length() == 0) {
            layerDesc += "none";
        } else {
            layerDesc += layer.description;
        }
        if (layerDesc.length() > 500) {
            layerDesc = layerDesc.substring(0,500) + "...";
        }
        tooltipText += "<p width=\"400px\">"+layerDesc+"</p><p width=\"400px\">";
        HashSet<String> uniqueSet = new HashSet<String>();
        for (LayerParameters lp : this.layerList) {
            String categoryText = (lp.category != null ? lp.category : "");
            String subcatText = (lp.subcategory != null ? lp.subcategory : "");
            String topicText = (lp.topic != null ? lp.topic : "");
            String firstArrow = (!"".equals(categoryText) && (!"".equals(subcatText) || !"".equals(topicText)) ? space + separator + space : "");
            String secondArrow = ((!"".equals(categoryText) && !"".equals(subcatText)) && !"".equals(topicText) ? space + separator + space : "");
            String hierarchyText = categoryText+firstArrow+subcatText+secondArrow+topicText;
            uniqueSet.add(hierarchyText);
        }
        tooltipText += "<br /><br /><b><u>CATEGORY</u></b><br/>";
        for (String hierarchy : uniqueSet) {
            tooltipText += StringEscapeUtils.escapeHtml4(hierarchy) + "<br/>";
        }
        tooltipText += "</p></html>";
        layerTooltip.setToolTipText(tooltipText);        
       
        layerNameLbl.setOpaque(false);
        layerNameLbl.setText(this.layer.name);        

        boolean addContourButton = false;
        String sourceToAdd = null;
        if (this.layer.type.equalsIgnoreCase("map")) {
        	ArrayList<String> sources = this.layer.options;
        	if (sources != null) {
        		if (sources.size() > 0) {
            		String one = sources.get(0);
        			if (one != null) {
        				sourceToAdd = one;
            			addContourButton = true;
            		}
        		}
        		if (sources.size() > 1) {
        			String two = sources.get(1);
	        		if (two != null) {
	        			sourceToAdd = two;
	        			addContourButton = true;
	        		}
        		}
        	}
        	if (addContourButton && sourceToAdd != null) {
        		MapSource source = null;
        		if (MapServerFactory.getMapServers() == null) {
        			try {
        				//one shot for one second to let the map servers finish loading
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						//do nothing
					}
        		}
        		if (MapServerFactory.getMapServers() != null) {
					for (MapServer server : MapServerFactory.getMapServers()) {
						source = server.getSourceByName(sourceToAdd);
						if (source != null) {
							break;
						}
					}
        		}
        		if (source != null && source.hasNumericKeyword()) {
					addContourButton = true;
					contourButton = new ContourButton(contourIcon);
					this.mapSource = source;//must set for contourAction
			        contourButton.addActionListener(contourAction);
				} else {
					addContourButton = false;
				}
	        	
        	}
        }
        
        this.layoutRow(addContourButton);
        this.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, border));
        
        MouseAdapter redispatcher = new MouseAdapter() {
          @Override
          public void mouseEntered(MouseEvent evt)
          {
            dispatchMouseEvent(evt);
          }      
          @Override
          public void mouseExited(MouseEvent evt)
          {
            dispatchMouseEvent(evt);
          }
          @Override
          public void mouseMoved(MouseEvent evt)
          {
            dispatchMouseEvent(evt);
          }
          @Override
          public void mousePressed(MouseEvent evt)
          {
            dispatchMouseEvent(evt);
          }
          private void dispatchMouseEvent(MouseEvent evt)
          {
//            Container parent = evt.getComponent().getParent();
            SearchResultRow.this.getParent().dispatchEvent(SwingUtilities.convertMouseEvent(evt.getComponent(), evt, SearchResultRow.this.getParent()));
          }            
        };
        this.mainButton.addMouseListener(redispatcher);
        this.mainButton.addMouseMotionListener(redispatcher);
        if (this.contourButton != null) {
        	this.contourButton.addMouseListener(redispatcher);
            this.contourButton.addMouseMotionListener(redispatcher);
        }
        if (this.favoriteButton != null && this.favoriteButton.isEnabled()) {
        	this.favoriteButton.addMouseListener(redispatcher);
            this.favoriteButton.addMouseMotionListener(redispatcher);
        }

    }
    private void buildCategorization() {
    	categorization = new JLabel("category");
    	if (this.isMapSource) {
    		String hierarchyText = "";
    		String[][] categories = this.mapSource.getCategories();
    		String[] first = categories[0];
    		if (first != null && first.length > 0) {
    			for (int i=0; i<first.length; i++) {
    				if (i > 0) {
    					if (i > 1) {
    						hierarchyText += " | ";
    					}
    					hierarchyText += first[i];
    				}
    			}
    		}
    		categorization.setText(hierarchyText);
    	} else {
    		String hierarchyText = null;
    		LayerParameters lp = this.layerList.get(0);
            String categoryText = (lp.category != null ? lp.category : "");
            String subcatText = (lp.subcategory != null ? lp.subcategory : "");
            String topicText = (lp.topic != null ? lp.topic+"  " : "");//TODO: fix the issue cutting off the final character in the framework
            String firstArrow = (!"".equals(categoryText) && (!"".equals(subcatText) || !"".equals(topicText)) ? space + separator + space : "");
            String secondArrow = ((!"".equals(categoryText) && !"".equals(subcatText)) && !"".equals(topicText) ? space + separator + space : "");
            String first = categoryText+firstArrow;
            if (categoryText.equalsIgnoreCase("Instrument")) {
            	first = "";
            }
            String second = subcatText+secondArrow;
            if (subcatText.equalsIgnoreCase(topicText)) {
            	second = "";
            }
            hierarchyText = first+second+topicText;
            
            hierarchyText = StringEscapeUtils.escapeHtml4(hierarchyText);
    		categorization.setText(hierarchyText);
    	}

    	categorization.setFont(getFont().deriveFont(10f));
		categorization.setForeground(categoryTextColor);
    }
    
    class FavoriteButton extends JButton {
        private int myMode;
        private Icon img;
        private Icon selImg;        
        
        FavoriteButton (int tmode, Icon normal, Icon selected)
        {
            super(normal);
            img = normal;
            selImg = selected;
            myMode = tmode;
            if (myMode == 1) {
                changed(1);
            }
            
            setFocusPainted(false);
            setDisabledIcon(favoriteDisabledIcon);
            
			addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (Main.USER != null && Main.USER.trim().length() > 0) {
						SearchProvider sp = SearchProvider.getInstance();
						if (favoriteFlag) {
							String delLayerId = null;
							String mapSourceName = null;
							if (SearchResultRow.this.isCustom) {
								String name = SearchResultRow.this.layer.name;
								delLayerId = sp.getCustomMapId(name);
							} else if (SearchResultRow.this.isMapSource) {
								mapSourceName = SearchResultRow.this.mapSource.getTitle();
							} else {
								delLayerId = SearchResultRow.this.layer.id;
							}
							CustomMapBackendInterface.deleteFavorite(delLayerId, SearchResultRow.this.isCustom, mapSourceName);
							sp.deleteFavorite(delLayerId,mapSourceName);
							favoriteFlag = false;
						} else {
							if (SearchResultRow.this.isCustom()) {
								String name = SearchResultRow.this.layer.name;
								String id = sp.getCustomMapId(name);
								CustomMapBackendInterface.addCustomFavorite(id);
								sp.addFavorite("cm_" + id);
							} else if (SearchResultRow.this.isMapSource) {
								String mapSourceName = SearchResultRow.this.mapSource.getTitle();
								CustomMapBackendInterface.addMapSourceFavorite(mapSourceName);
								sp.addMapSourceFavorite(mapSourceName);
							} else {
								CustomMapBackendInterface.addFavorite(SearchResultRow.this.layer.id);
								sp.addFavorite(SearchResultRow.this.layer.id);
							}
							favoriteFlag = true;
						}
					}
				}
			});
			addMouseListener(new MouseListener() {

				@Override
				public void mouseReleased(MouseEvent e) {
				}

				@Override
				public void mousePressed(MouseEvent e) {
					if (myMode == 1) {
						changed(0);
					} else {
						changed(1);
					}
				}

				@Override
				public void mouseExited(MouseEvent e) {	
				}

				@Override
				public void mouseEntered(MouseEvent e) {
				}

				@Override
				public void mouseClicked(MouseEvent e) {
				}
			});
			setUI(new MyIconButtonUI());
		}

        public void changed (int newMode)
        {
        	if (!isEnabled())
        		return;
        	
            if (newMode == 1){
                setIcon(selImg);
            }
            else{
                setIcon(img);
            }
            myMode = newMode;
       }  
    }    
  
    
    class ContourButton extends JButton {
                
        ContourButton (Icon normal) {
            super(normal);
            
            setFocusPainted(false);
			setUI(new MyIconButtonUI());
			setToolTipText("Add map as a contour");
        }
    }
       
	
    public void setIsCustom(boolean flag) {
        this.isCustom = flag;
    }
    public boolean isCustom() {
        return this.isCustom;
    }
    private Icon getImageIcon(String type) {
        Icon typeIcon = typeIcons.get(type);
        if (typeIcon == null) {
            LViewFactory factoryObject = LViewFactory.findFactoryType(type);
            
            if (factoryObject != null) {
                typeIcon = factoryObject.getLayerIcon();
            }
            if (typeIcon == null) {        
                typeIcon = LViewFactory.getDefaultLayerIcon();
            }
            typeIcons.put(type,typeIcon);
        }
        return typeIcon;
    }
    
    public String getLayerTooltipText() {
    	return this.tooltipText;
    }

	@Override
	public int compareTo(Object oRow) {
		if (oRow instanceof SearchResultRow) {
			SearchResultRow row = (SearchResultRow) oRow;
			int typeCompare = this.orderWeight.compareTo(row.orderWeight);
			if (typeCompare == 0) {
				return this.layerNameLbl.getText().toLowerCase().compareTo(row.layerNameLbl.getText().toLowerCase());
			} else {
				return typeCompare;
			}
		}
		return 0;
	}
	
	private static class MyIconButtonUI extends MaterialButtonUI {

		public static ComponentUI createUI(JComponent c) {
			return new MyIconButtonUI();
		}
		@Override
		public void installUI(JComponent c) {
			mouseHoverEnabled = false;
			super.installUI(c);
			super.background = ((ThemeButton) GUITheme.get("button")).getThemebackground();
			super.disabledBackground = ((ThemeButton) GUITheme.get("button")).getThemebackground();		
			super.defaultBackground = ((ThemeButton) GUITheme.get("button")).getThemebackground();
			super.borderEnabled = false;
			super.arch = 0;
		    c.setFocusable(true);
//			 if (mouseHoverEnabled != null) {
//	                JButton b = (JButton) button;
//	                if (!b.isDefaultButton()) {
//	                    button.addMouseListener(MaterialUIMovement.getMovement(button, 
//	                    		((ThemeButton) GUITheme.get("button")).getAltOnhover()));
//	                }
//	            }			
		}
		
		@Override
	    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
	       paintBorderButton(g, b);
	    }
	}


	@Override
	public String toString() {
		return this.layer.name;
	}      
	
	
  
}
