package edu.asu.jmars.layer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.swing.UrlLabel;
import edu.asu.jmars.swing.materialtabstyle.infopanel.InfoPaneTabHeader;
import edu.asu.jmars.swing.sm.events.CloseInfoEvent;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.HttpRequestType;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import fr.lri.swingstates.events.VirtualEvent;
import fr.lri.swingstates.sm.BasicInputStateMachine;
import fr.lri.swingstates.sm.State;
import fr.lri.swingstates.sm.Transition;
import fr.lri.swingstates.sm.jtransitions.PressOnComponent;
import fr.lri.swingstates.sm.transitions.Event;


public class FocusInfoPanel extends JPanel {
	
	public boolean isLimited;
	private Layer.LView lView;  //Attached mapLView
	private JPanel northPanel, centerPanel, west, east;
	private JLabel title, units;
	private JPanel description, links, citation;
	private JScrollPane descripSP, linkSP, citSP;
	private JTextPane descrip;
	private JTextArea cit;
	private JPanel link;
	private JLabel linkLbl;
	private final Dimension defaultSize = new Dimension(300,300);
	public InfoPaneTabHeader infopanelheader = new InfoPaneTabHeader();			
	private JDialog detachedInfoDialog = null;
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();	
	private JLabel detachdockedButton = new JLabel(
			new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DETACH_DOCKED.withDisplayColor(imgColor))));
	
    
	private static ExecutorService pool = Executors.newCachedThreadPool(new ThreadFactory() {
    	int id = 0;
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName("FocusInfoQuery-" + (id++));
			t.setPriority(Thread.MIN_PRIORITY);
			t.setDaemon(true);
			return t;
		}
    });
	
	public FocusInfoPanel(Layer.LView lv, boolean limited){
		this.lView = lv;		
		layoutFields(limited);	
		loadFields();		
		initStates();		
	}	


	private void initStates() {
		smCloseInfoPanel.addStateMachineListener(smCloseInfoPanel);				
		smDetachInfoPanel.addStateMachineListener(smDetachInfoPanel);		
		smDockInfoPanel.addStateMachineListener(smDockInfoPanel);
		smUnDockInfoPanel.addStateMachineListener(smUnDockInfoPanel);
		
		smCloseInfoPanel.addStateMachineListener(smDockInfoPanel);
		smDockInfoPanel.addStateMachineListener(smUnDockInfoPanel);
		smDockInfoPanel.addStateMachineListener(smCloseInfoPanel);
		
		smDetachInfoPanel.processEvent(new VirtualEvent("ON"));
		smCloseInfoPanel.processEvent(new VirtualEvent("ON"));
		smDockInfoPanel.processEvent(new VirtualEvent("ON"));
		smUnDockInfoPanel.processEvent(new VirtualEvent("ON"));
	}
	

	public void hideInPanelDetachDockButton(boolean hide){
		this.detachdockedButton.setVisible(!hide);
	}
	
	protected void hideDetachDockButton() {
		detachdockedButton.setVisible(false);		
	}		
	
//This method loads all the gui for the infoPanel...
//	it has two possible layouts depending on whether
//	it is a "limited" infoPanel (such as the lat/lon
//	grid's or map scalebar's)	
	public void layoutFields(boolean limited){
		
		this.isLimited=limited;
		
		setLayout(new BorderLayout());
		setPreferredSize(defaultSize);
			 
		northPanel = new JPanel(new GridLayout(3, 1));			
		JPanel containerTitle = new JPanel(new BorderLayout());
		containerTitle.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 5));
        JPanel containerLabel = new JPanel(new BorderLayout(10,0));     
        title = new JLabel("Layer Information");
		title.setFont(ThemeFont.getBold());				
        containerLabel.add(title, BorderLayout.CENTER); 
        detachdockedButton.setToolTipText("Click here to display layer information in a separate window");
        containerLabel.add(detachdockedButton, BorderLayout.EAST);
        containerTitle.add(containerLabel, BorderLayout.WEST);            
        JPanel unitspanel = new JPanel();
        unitspanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 5));        
		units = new JLabel("Units: ---");
		units.setFont(ThemeFont.getBold());
		unitspanel.add(units);			
		northPanel.add(infopanelheader);				
		northPanel.add(containerTitle);								
		northPanel.add(unitspanel);
	  
		centerPanel = new JPanel();
		centerPanel.setLayout(new GridBagLayout());
			    
		description = new JPanel();
		description.setLayout(new GridLayout(1,1));
		description.setBorder(BorderFactory.createTitledBorder("Description"));
		description.setMinimumSize(new Dimension(100,50));
		descrip = new JTextPane();
		StyledDocument descripDoc = descrip.getStyledDocument();
		descrip.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));
		descrip.setEditable(false);
		descrip.setText("There is currently no description for this layer");
	    
		SimpleAttributeSet descripAtt = new SimpleAttributeSet();
		StyleConstants.setLineSpacing(descripAtt, 0.18f);
		Color foreground = ((ThemePanel)GUITheme.get("panel")).getForeground();
		StyleConstants.setForeground(descripAtt, foreground);
		descripDoc.setParagraphAttributes(0, descripDoc.getLength(), descripAtt, false);

		descripSP = new JScrollPane(descrip, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		descripSP.setBorder(BorderFactory.createEmptyBorder());
		description.add(descripSP);	
		
		if(!isLimited){		
			links = new JPanel();
			links.setBorder(BorderFactory.createTitledBorder("Links"));
			links.setLayout(new GridLayout(1,1));
			link = new JPanel();
			Color bghighlight = ((ThemePanel)GUITheme.get("panel")).getBackgroundhi();
			link.setBackground(bghighlight);
			link.setLayout(new BoxLayout(link, BoxLayout.PAGE_AXIS));
			link.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
			linkLbl = new JLabel("There are currently no links for this layer.");			
			linkLbl.setFont(ThemeFont.getRegular());
			link.add(linkLbl);
			linkSP = new JScrollPane(link, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			linkSP.setBorder(BorderFactory.createEmptyBorder());
			links.add(linkSP);	
		
			citation = new JPanel();
			citation.setBorder(BorderFactory.createTitledBorder("Citation"));
			citation.setMinimumSize(new Dimension(100,50));
			citation.setLayout(new GridLayout(1,1));
			cit = new JTextArea("There is currently no citation for this layer");
			cit.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
			cit.setEditable(false);
			cit.setLineWrap(true);
			cit.setWrapStyleWord(true);
			citSP = new JScrollPane(cit, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			citSP.setBorder(BorderFactory.createEmptyBorder());
			citation.add(citSP);
		
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridx = 0;
			c.gridy = 0;
			c.ipady = 50;
			c.weighty = 0.8;
			c.weightx = 1;
			c.insets = new Insets(10, 0, 0, 0);
			centerPanel.add(description, c);
			c.gridx = 0;
			c.gridy = 1;
			c.ipady = 10;
			c.weighty = 0.1;
			c.insets = new Insets(4,0,0,0);
			centerPanel.add(links, c);
			c.gridx = 0;
			c.gridy = 2;
			c.insets = new Insets(4,0,5,0);
			centerPanel.add(citation, c);						
		}
		
		if (isLimited){
			centerPanel.add(Box.createRigidArea(new Dimension(0,10)));
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.weighty = 1;
			c.weightx = 1;
			c.insets = new Insets(5,0,10,0);
			centerPanel.add(description,c);
		}
		
	//adds pieces to focusInfoPanel
		west = new JPanel();
		east = new JPanel();
		add(northPanel, BorderLayout.NORTH);
		add(centerPanel, BorderLayout.CENTER);
		add(west, BorderLayout.WEST);
		add(east, BorderLayout.EAST);		
	}
		
	
	public void loadFields(){
		Runnable fetchData = new Runnable() {
			
			@Override
			public void run() {
				String body = Config.get(Util.getProductBodyPrefix() + "bodyname");//@since change bodies
				body = body.toLowerCase();
				String layerType = "";
				String layerKey = "";
				String layerId = "";
				final LayerParameters layerParams = lView.getLayerParameters();
				String urlStr = "";
				
				if (layerParams == null){
					layerKey = lView.getLayerKey();		
					if(layerKey == null || layerKey.equals("")){
						layerKey = LManager.getLManager().getUniqueName(lView);
					}
					layerType = lView.getLayerType();
					urlStr = "LayerInfoFetcher?body="+body+"&layerType="+layerType+"&layerKey="+layerKey;
				}else{
					layerId = layerParams.id;
					layerType = layerParams.type;
					urlStr = "LayerInfoFetcher?body="+body+"&layerId="+layerId;
				}

				ArrayList<String> fields;
				
				try {
					int idx = urlStr.indexOf("?");
		
					String connStr = LayerParameters.paramsURL + urlStr.substring(0,idx);
		
					String data = urlStr.substring(idx+1)+StampLayer.getAuthString()+StampLayer.versionStr;
		
					// Connect timeout and SO_TIMEOUT of 10 seconds
					//			URL url = new URL(connStr);               // TODO Remove old code
					//			URLConnection conn = url.openConnection();
					//			conn.setConnectTimeout(10*1000);
					//			conn.setReadTimeout(10*1000);
					//			
					//			conn.setDoOutput(true);
					//			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
					//			wr.write(data);
					//			wr.flush();
					//			wr.close();
					//			
					//			ObjectInputStream ois = new ObjectInputStream(conn.getInputStream());
					//			fields = (ArrayList<String>) ois.readObject();
					//			ois.close();
					
					JmarsHttpRequest req = new JmarsHttpRequest(connStr, HttpRequestType.POST);
					req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
					req.addOutputData(data);
					req.setConnectionTimeout(10*1000);
					req.setReadTimeout(10*1000);
					if (req.send()) {
						ObjectInputStream ois = new ObjectInputStream(req.getResponseAsStream());
						fields = (ArrayList<String>) ois.readObject();
						ois.close();
						req.close();
					} else {
						//failed request, prevent NPE
						fields = new ArrayList<String>();
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("Error retrieving layer parameters");
					fields = new ArrayList<String>();
				}
				
				final ArrayList<String> updateFields = fields;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						updateName();	
				// If the lview does not have an lparams associated with it, populate the 
				//  infopanel off a database query.
						if(layerParams==null){
							if(updateFields!=null&&updateFields.size()>0){
								//Layerkey is passed back at index 0 in fields in old code, ignore
								// that value now since the name is set using the updateName method
								if(updateFields.get(1)!=null && updateFields.get(1).length()>0 && !isLimited)
									cit.setText(updateFields.get(1));
								if(updateFields.get(2)!=null && updateFields.get(2).length()>0)
									descrip.setText(updateFields.get(2));
								if(updateFields.get(3)!=null && updateFields.get(3).length()>0)
									units.setText("Units: "+updateFields.get(3));
								if(updateFields.size()>=5 && !isLimited){
									if(updateFields.get(4)!=null && updateFields.get(4).length()>2){
										link.removeAll();
										String ltext = updateFields.get(4);
										ltext = ltext.substring(1, ltext.length()-1);
										ltext = ltext.trim();
										String[] links = ltext.split(",");
										for(String s : links){
											UrlLabel u = new UrlLabel(s);
											link.add(u);
										}
									}	
								}
							}
						}else{
				// populate the infopanel from lparams attributes
							if(!isLimited){
								if(layerParams.citation.length()>2){
									cit.setText(layerParams.citation);
								}
								if(layerParams.getLinks().length()>2){
									link.removeAll();
									String ltext = layerParams.getLinks();
									ltext = ltext.substring(1, ltext.length()-1);
									ltext = ltext.trim();
									String[] links = ltext.split(",");
									for(String s : links){
										//remove quotes if there are any
										s = s.replace("\"", "");
										s = s.replace("\'", "");
										//trim off white space
										s = s.trim();
										//create url label based off string
										UrlLabel u = new UrlLabel(s);
										//add label to focus panel
										link.add(u);
									}
								}
							}
							if(layerParams.description.length()>0){
								descrip.setText(layerParams.description);
							}
							if(layerParams.units.length()>0){
								units.setText("Units: "+layerParams.units);
							}
							
						}	

						repaint();
					}
				});
			
			}
		};		
		pool.execute(fetchData);
	}
	
// Set the name of the focuspanel to match the row (since it is the same layer)
	public void updateName(){		
		infopanelheader.setTitle(LManager.getLManager().getUniqueName(lView));
	}
	

	public boolean isInfoDialogDetached() {
		return detachedInfoDialog != null && detachedInfoDialog.isVisible();
	}

	public void closeDetachedInfoDialog(){
		if(detachedInfoDialog != null){
			detachedInfoDialog.setVisible(false);
		}
	}
	
	
	BasicInputStateMachine smCloseInfoPanel = new BasicInputStateMachine() {

		public State START = new State() {
			Transition on = new Event("ON", ">> ON") {
				public void action() {					
					addAsListenerOf(infopanelheader.closeinfoButton);
				}
			};
		};

		public State ON = new State() {
			Transition closeinfo = new PressOnComponent(BUTTON1) {
				public void action() {					
					if (isInfoDialogDetached()) {
						closeDetachedInfoDialog();
					}
					boolean isInfoDetached = false;
					fireEvent(new CloseInfoEvent(smCloseInfoPanel, isInfoDetached));
				}
			};

			Transition internalcloseinfo = new Event("internalcloseinfo") {
				public void action() {					
					if (isInfoDialogDetached()) {
						closeDetachedInfoDialog();
					}
					boolean isInfoDetached = false;
					fireEvent(new CloseInfoEvent(smCloseInfoPanel, isInfoDetached));
				}
			};
		};
	};
	

	BasicInputStateMachine smDetachInfoPanel = new BasicInputStateMachine() {

		public State START = new State() {
			Transition on = new Event("ON", ">> ON") {
				public void action() {					
					addAsListenerOf(detachdockedButton);
				}
			};
		};

		public State ON = new State() {
			Transition detach = new PressOnComponent(BUTTON1) {
				public void action() {					
					initInfoAsUndockedDialog(); 
					FocusInfoPanel infoDetached = (FocusInfoPanel) lView.getFocusPanel().getInfoPanel();
					infoDetached.hideDetachDockButton();
					infoDetached.infopanelheader.hideCloseInfoButton();
					infoDetached.infopanelheader.undockButton.setVisible(false);
					infoDetached.detachedInfoDialog = detachedInfoDialog;
					detachedInfoDialog.setContentPane(infoDetached);
					detachedInfoDialog.setVisible(true);										
					boolean isInfoDetached = true;
					fireEvent(new CloseInfoEvent(smDetachInfoPanel, isInfoDetached));
				}
			};

		};
		
		private void initInfoAsUndockedDialog() {
			int locx, locy;
			JFrame parentframe = lView.getFocusPanel().getFrame();	
			if (parentframe == null) return;		
			if (detachedInfoDialog == null) {			
				detachedInfoDialog = new JDialog(parentframe);	
				detachedInfoDialog.addWindowListener(new WindowAdapter() {
				    @Override
				    public void windowClosing(WindowEvent e) {			    	
				    	smCloseInfoPanel.fireEvent(new CloseInfoEvent(smCloseInfoPanel, false));
				    }
				});
			}			   
				
			locx = parentframe.getX();
			locy = parentframe.getY();
			detachedInfoDialog.setLocation(locx + 80, locy + 30);
			detachedInfoDialog.setSize(new Dimension(parentframe.getWidth(), parentframe.getHeight()));			
			detachedInfoDialog.setVisible(false);				
		}			
	};
	

	BasicInputStateMachine smDockInfoPanel = new BasicInputStateMachine() {

		public State START = new State() {
			Transition on = new Event("ON", ">> ON") {
				public void action() {
					addAsListenerOf(infopanelheader.dockButton);
				}
			};
		};

		public State ON = new State() {
			Transition dockinfo = new PressOnComponent(BUTTON1) {
				public void action() {					
					if (isInfoDialogDetached()) {
						closeDetachedInfoDialog();											
						fireEvent(new VirtualEvent("internalshowinfo"));
					}
					else {
						fireEvent(new VirtualEvent("internaldockme"));
					}
				}
			};

		};
	};
	
	BasicInputStateMachine smUnDockInfoPanel = new BasicInputStateMachine() {

		public State START = new State() {
			Transition on = new Event("ON", ">> ON") {
				public void action() {
					addAsListenerOf(infopanelheader.undockButton);
				}
			};
		};

		public State ON = new State() {
			Transition undockinfo = new PressOnComponent(BUTTON1) {
				public void action() {										
					fireEvent(new VirtualEvent("internalundockme"));				
				}
			};

		};
	};	
	
}
