package edu.asu.jmars.layer.profile;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.PROFILE_PENCIL;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.util.ShapeUtilities;
import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.MultiProjection;
import edu.asu.jmars.layer.PannerGlass;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.WrappedMouseEvent;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.profile.ProfileLView.SavedParams.Builder;
import edu.asu.jmars.layer.profile.chart.ProfileChartView;
import edu.asu.jmars.layer.profile.config.ConfigType;
import edu.asu.jmars.layer.profile.config.ConfigureChartView;
import edu.asu.jmars.layer.shape2.ShapeUtil;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.parsers.gis.CoordinatesParser.Ordering;
import edu.asu.jmars.swing.LikeLabelButtonUI;
import edu.asu.jmars.swing.sm.events.DeleteLine;
import edu.asu.jmars.swing.sm.events.DrawLine;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeChart;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeSnackBar;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.MovableList;
import fr.lri.swingstates.events.VirtualEvent;
import fr.lri.swingstates.sm.BasicInputStateMachine;
import fr.lri.swingstates.sm.State;
import fr.lri.swingstates.sm.Transition;
import fr.lri.swingstates.sm.transitions.Click;
import fr.lri.swingstates.sm.transitions.Event;
import fr.lri.swingstates.sm.transitions.Move;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.CustomBalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;
import net.java.balloontip.utils.TimingUtils;
import edu.asu.jmars.layer.profile.manager.ProfileManagerMode;
import edu.asu.jmars.layer.profile.manager.ProfileManagerTableModel;
import edu.asu.jmars.layer.profile.swing.RenameProfilePanel;



public class ProfileLView extends Layer.LView implements IProfileModelEventListener, TableModelListener {
	private static final long serialVersionUID = -5361102517691552130L;
	private static DebugLog log = DebugLog.instance();
	private ProfileLayer myLayer;
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getCommonFill();
	private static Color profileDoneColor = ThemeProvider.getInstance().getBackground().getHighlight();
	private static Color profileWhileDrawingColor = ((ThemePanel) GUITheme.get("panel")).getBackground();
	private static Image profile_c = ImageFactory.createImage(PROFILE_PENCIL.withDisplayColor(imgColor));	
	Dimension bestSize = Toolkit.getDefaultToolkit().getBestCursorSize(profile_c.getWidth(null), profile_c.getHeight(null));	
	private final Cursor profile_draw_cursor = Toolkit.getDefaultToolkit().createCustomCursor(profile_c, new Point(0, bestSize.height * 4/5), "drawprofile");				
	private Shape currentProfileLine;
	private BasicStroke basicstroke = null;
	private int STROKE_WIDTH;  // stroke width in pixels
	private Map<Integer, Shape> profileLines = new LinkedHashMap<>();
	private Map<Integer, Cue> cues = new HashMap<>();
	private final ChartDataConverter mathconversions;
	private ProfileFactory controller; 
	private final static DefaultDrawingSupplier paintSupplier = new DefaultDrawingSupplier();
	private CustomBalloonTip myBalloonTip;
	private Color imgBlack = Color.BLACK;
	private Icon close = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CLEAR.withDisplayColor(imgBlack)));
	public ChartFocusPanel	chartFocusPanel;
	private static  Map<String, Supplier<IChartEventHandler>> EVENT_HANDLER;
	
	
	public ProfileLView(ProfileLayer parent, ProfileLView3D lview3d) {
		super(parent, lview3d);
		this.myLayer = parent;		
		STROKE_WIDTH = 2;
		this.mathconversions = new ChartDataConverter(this);
		initLViewEventHandlers();
		setBufferCount(1);
		createCalloutUI();
	}
	
	private void initLViewEventHandlers() {
		final Map<String, Supplier<IChartEventHandler>> handlers = new HashMap<>();
		handlers.put(IProfileModel.UPDATE_CUE_FOR_PROFILE, UpdateCue::new);
		handlers.put(IProfileModel.REQUEST_PROFILE_LINE_WIDTH_CHANGED, LineWidth::new);
		EVENT_HANDLER = Collections.unmodifiableMap(handlers);
	}


	public void initEventReceiver() {
		this.initStates();		
	}		
		
	
	private void initStates() {
		this.smProfileMouseActions.addStateMachineListener(this.smProfileMouseActions);			
		this.smProfileMouseActions.processEvent(new VirtualEvent("ON"));				
	}
	

	public void createProfileManagerFocusPanel() {	
		controller = (ProfileFactory) this.originatingFactory;
		
		focusPanel = ProfileFocusPanel.builder().
				                       withProfileLView(this).
				                       withController(controller).
				                       build();
	}
	
	
	public void createChartFocusPanel(ProfileChartView profileChartView, ConfigureChartView configureChartView, SavedParams savedParams) {	
		chartFocusPanel = ChartFocusPanel.builder().
				                          withProfileLView(this).
				                          withChartView(profileChartView).
				                          withConfigView(configureChartView).build();
		if (savedParams != null) {
			chartFocusPanel.notifyRestoredFromSession(savedParams);
		}
	}
		
	@Override
	public synchronized void receiveData(Object layerData) {}
	

	@Override
	protected Object createRequest(Rectangle2D where) {
		return (null);
	}

	@Override
	protected Layer.LView _new() {
		return new ProfileLView((ProfileLayer) getLayer(), null);
	}

	@Override
	public String getName() {
		return "Profile";
	}

	@Override
	protected void viewChangedPost() {
		repaint();
	}
	
	@Override
	protected Component[] getContextMenuTop(Point2D worldPt) {						
		if(viewman.getActiveLView().equals(this)) {
			List<Component> profileLayerItems =
				new ArrayList<Component>( Arrays.asList(super.getContextMenuTop(worldPt)) );			
			int itemindex = 0;
			String name = "";
			List<Shape> profiles = findProfileByWorldPt(worldPt);
			for (Shape profile : profiles) {
				name = ((ProfileLine)profile).getRename() != null  ? ((ProfileLine)profile).getRename() : ((ProfileLine)profile).getIdentifier();						
				JMenuItem addToChart = new JMenuItem("Add " + name + " to Existing Chart");
				addToChart.addActionListener(e -> addToChart(profile));
				profileLayerItems.add(itemindex++, addToChart);	
				addToChart.setEnabled(isSingleSourceConfig());
				if (!addToChart.isEnabled()) {
					String htmltxt = "<html><body><div>Adding profile to chart is availble when</div>"
							+ "<div>Compare Profile Lines option is selected in Configuration tab.</div>"
							+ "</body></html>";
				    addToChart.setToolTipText(htmltxt);
				}
			}
			
			for (Shape profile : profiles) {
				name = ((ProfileLine) profile).getRename() != null ? ((ProfileLine) profile).getRename()
						: ((ProfileLine) profile).getIdentifier();
				JMenuItem renameprofile = new JMenuItem("Rename " + name);
				renameprofile.addActionListener(e -> renameProfile(profile, renameprofile));
				profileLayerItems.add(itemindex++, renameprofile);
			}

			for (Shape profile : profiles) {
				name = ((ProfileLine)profile).getRename() != null  ? ((ProfileLine)profile).getRename() : ((ProfileLine)profile).getIdentifier();
				JMenuItem delProfile = new JMenuItem("Delete " + name);
				delProfile.addActionListener(e -> delProfile(profile));
				profileLayerItems.add(itemindex++, delProfile);
			}
			if (!this.profileLines.isEmpty()) {
				JMenuItem manageProfiles = new JMenuItem("Manage Profiles");
				manageProfiles.addActionListener(e -> manageProfiles(worldPt));
				profileLayerItems.add(itemindex++, manageProfiles);
			}	
			if (!this.profileLines.isEmpty()) {
				JMenuItem delAll = new JMenuItem("Delete All Profiles");
				delAll.addActionListener(e -> delAll(worldPt));
				profileLayerItems.add(itemindex, delAll);
			}								
			return (Component[]) profileLayerItems.toArray(new Component[0]);
		} else {
			return new Component[0];
		}		
	}
	

	@Override
	public void setCursor(Cursor c)	{
		super.setCursor(c);
		if (ToolManager.getToolMode() != ToolManager.PROFILE) {
			JButton profileTool = Main.testDriver.toolMgr.getProfileToolComponent();
			showCallout(profileTool);
		}		
	}	
	
	
	private void delAll(Point2D worldPt) {
		int returnVal = edu.asu.jmars.util.Util.showConfirmDialog("Do you want to delete all profiles?",
				"Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		switch (returnVal) {
		case JOptionPane.YES_OPTION:
			this.profileLines.clear();
			this.cues.clear();
			Map<Integer, Shape> varProfiles = new LinkedHashMap<>();
			varProfiles.putAll(this.profileLines);
			controller.addNewProfile(varProfiles);
			repaint();
			break;
		case JOptionPane.NO_OPTION:
			break;
		default:
			break;
		}
	}


	public Map<Integer, Shape> getAllExistingProfilelines() {
		Map<Integer, Shape> varProfiles = new LinkedHashMap<>();
		varProfiles.putAll(this.profileLines);		
		return varProfiles;
	}

	private void delProfile(Shape shape) {
		if (!(shape instanceof ProfileLine)) return;	
		ProfileLine profile = (ProfileLine) shape;
		int ID = profile.getID();
		String profilename = profile.getRename() != null ? profile.getRename() : profile.getIdentifier();
		int returnVal = edu.asu.jmars.util.Util.showConfirmDialog("Do you want to delete " + profilename + "?",
				"Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		switch (returnVal) {
		case JOptionPane.YES_OPTION:
			this.profileLines.remove(ID);
			this.cues.remove(ID);
			Map<Integer, Shape> varProfiles = new LinkedHashMap<>();
			varProfiles.putAll(this.profileLines);
			controller.addNewProfile(varProfiles);		
			repaint();			
			break;
		case JOptionPane.NO_OPTION:
			break;
		default:
			break;
		}
	}

	private void viewChart(Shape profile) {
		int ID = ((ProfileLine) profile).getID();
		Map<Integer, Shape> selectedtoview = new HashMap<>();
		selectedtoview.put(ID,  profile);
		this.controller.selectedProfiles(selectedtoview, ProfileManagerMode.CREATE_NEW_CHART);
		if (this.chartFocusPanel != null) {
			this.chartFocusPanel.setSelectedIndex(1); // Chart tab
			this.chartFocusPanel.showInFrame();
		}
		repaint();
	}
	

	private void renameProfile(Shape shape, JMenuItem renameprofile) {
		if (!(shape instanceof ProfileLine)) return;
		ProfileLine profile = (ProfileLine) shape;
		String renamefrom = profile.getRename() != null ? profile.getRename() : profile.getIdentifier();
		RenameProfilePanel renamepanel = new RenameProfilePanel(renamefrom);
		int returnVal = edu.asu.jmars.util.Util.showConfirmDialog(renamepanel.getMessage(), 
				"Rename " + renamefrom,
				JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		switch (returnVal) {
		case JOptionPane.OK_OPTION:
			String newprofilename = renamepanel.getNewName();
			profile.setRename(newprofilename);
			Map<Integer, Shape> selectedtoadd = new HashMap<>(); //can be empty, just need "mode" for this event
			this.controller.selectedProfiles(selectedtoadd, ProfileManagerMode.RENAME_PROFILE);
			((ProfileFocusPanel)(this.focusPanel)).updateProfiles();
			repaint();
			break;
		case JOptionPane.CANCEL_OPTION:
			break;
		default:
			break;
		}
	}

	private void addToChart(Shape profile) {
		int ID = ((ProfileLine) profile).getID();
		Map<Integer, Shape> selectedtoadd = new HashMap<>();
		selectedtoadd.put(ID,  profile);
		this.controller.selectedProfiles(selectedtoadd, ProfileManagerMode.ADD_TO_CHART);
		if (this.chartFocusPanel != null) {
			this.chartFocusPanel.setSelectedIndex(1); // Chart tab
			if (!this.chartFocusPanel.isShowing()) {
			    this.chartFocusPanel.showInFrame();
			}
		}
		repaint();		
	}
	
	public void viewConfig() {
		if (this.chartFocusPanel != null) {
			this.chartFocusPanel.setSelectedIndex(0); // Config tab
		}
	}
	

	private boolean isSingleSourceConfig() {
		boolean isSingleConfig = true;
		if (this.chartFocusPanel != null) {
			ConfigureChartView configview = this.chartFocusPanel.getConfigView();
			if (configview != null) {
				edu.asu.jmars.layer.profile.config.Config currentconfig = configview.getCurrentConfig();
				if (currentconfig != null) {
					if (currentconfig.getConfigType() == ConfigType.ONENUMSOURCE) {
						isSingleConfig = true;
					} else if (currentconfig.getConfigType() == ConfigType.MANYNUMSOURCES) {
						isSingleConfig = false;
					}
				}
			}
		}		
		return isSingleConfig;
	}
	
	private Map<ConfigType,edu.asu.jmars.layer.profile.config.Config> getBothConfig() {
		Map<ConfigType,edu.asu.jmars.layer.profile.config.Config> bothconfig = new HashMap<>();
		if (this.chartFocusPanel != null) {
			ConfigureChartView configview = this.chartFocusPanel.getConfigView();
			if (configview != null) {
				bothconfig.putAll(configview.getBothConfig());
			}
		}
		return bothconfig;
	}
	
	private edu.asu.jmars.layer.profile.config.Config getCurrentConfig() {
		edu.asu.jmars.layer.profile.config.Config currentconfig = null;
		if (this.chartFocusPanel != null) {
			ConfigureChartView configview = this.chartFocusPanel.getConfigView();
			if (configview != null) {
				currentconfig = configview.getCurrentConfig();
			}
		}
		return currentconfig;
	}	

	private void manageProfiles(Point2D worldPt) {
		if (this.focusPanel != null) {
			if (!this.focusPanel.isShowing()) {
			    this.focusPanel.showInFrame();
			}
		}
	}
		
	
	private List<Shape> findProfileByWorldPt(Point2D eventworldpoint) {
		final int PROXIMITY_BOX_SIDE = 5; // proxmity box side in pixels
		List<Shape> resultingProfiles = new ArrayList<>();
		if (!isVisible() || viewman == null) {
			return resultingProfiles;
		}
		MultiProjection proj = viewman.getProj();
		if (proj == null) {
			return resultingProfiles;
		}		
		Point2D mouseCurr = getProj().world.toSpatial(eventworldpoint);
		Rectangle2D boundingrect = getProj().getClickBox(getProj().spatial.toWorld(mouseCurr), PROXIMITY_BOX_SIDE);
		// convert profile FPath from spatial to world
		for (Map.Entry<Integer, Shape> entry : this.profileLines.entrySet()) {
			ProfileLine profile = (ProfileLine) entry.getValue();
			FPath pathSpatial = profile.getProfilePath();
			FPath pathWorld = pathSpatial.convertToSpecifiedWorld(getProj().getProjection());
			if (pathWorld.intersects(boundingrect)) {
				resultingProfiles.add(profile);
			}
		}
		return resultingProfiles;
	}
	

	@Override
	public boolean pannerStartEnabled() {
		return false;
	}

	@Override
	public String getToolTipText(MouseEvent event) {
		// Only show tooltips for a visible Main view of a selected layer (unless in investigate mode)		
		if ((viewman.getActiveLView() != this || event.getSource() instanceof PannerGlass || !isVisible())
				&& ToolManager.getToolMode() != ToolManager.INVESTIGATE) {
			return null;
		}					
		Point2D worldPointUserClicked = getProj().screen.toWorld(event.getPoint());	
		List<Shape> profilelines = findProfileByWorldPt(worldPointUserClicked);
		if (profilelines.isEmpty()) return null;
		StringBuffer tooltip = new StringBuffer(2000); 
		for (Shape profileline :  profilelines) {				
				tooltip.append("<html><table cellspacing=0 cellpadding=1><tr><td>");
				tooltip.append( ((ProfileLine)profileline).getRename() != null  ? 
						((ProfileLine)profileline).getRename() : ((ProfileLine)profileline).getPlotNameWithCoords());				
				tooltip.append("</td></tr></table>");
				tooltip.append("</html>");				
			}
		return tooltip.toString();
	}

	
	@Override
	public void tableChanged(TableModelEvent e) { // profile manager changed
		if (!(e.getSource() instanceof ProfileManagerTableModel)) return;
		
		TableModel model = (ProfileManagerTableModel) e.getSource();
		ProfileManagerMode mode = ProfileManagerMode.SELECT_MANY;
		
		int columnIndex = e.getColumn();
		//if Color or Rename event
		if (columnIndex != -1) {
			if (((ProfileManagerTableModel) model).COLOR.equals(model.getColumnName(columnIndex))
					|| ((ProfileManagerTableModel) model).TITLE.equals(model.getColumnName(columnIndex))) {
				mode = ProfileManagerMode.MANAGE;
			}
		}
		
		Map<Integer, Shape> selectedProfiles = new HashMap<>();
		Map<Shape, Boolean> modelData = ((ProfileManagerTableModel) model).getModelData();
		
		// detect if deletion is requested by Profile manager; handle accordingly
		for (Shape shape : modelData.keySet()) {
			if (!(shape instanceof ProfileLine))
				continue;
			ProfileLine profile = (ProfileLine) shape;
			if (profile.isDeleted()) {
				mode = ProfileManagerMode.MANAGE;
				break;
			}
		}

		for (Shape shape : modelData.keySet()) {
			if (!(shape instanceof ProfileLine)) continue;
			ProfileLine profile = (ProfileLine) shape;
			Boolean isSelectedForChart = modelData.get(shape);
			if (isSelectedForChart == true && !profile.isDeleted()) {
				selectedProfiles.put(profile.getID(), profile);
			}
		}
		
		for (Shape shape : modelData.keySet()) {
			if (!(shape instanceof ProfileLine)) continue;
			ProfileLine profile = (ProfileLine) shape;
			if (profile.isDeleted()) {
				smProfileMouseActions.fireEvent(new DeleteLine(smProfileMouseActions, profile.getID()));
			}
		}
		   this.controller.selectedProfiles(selectedProfiles, mode); 
		   repaint();
	}

	
	@Override
	public synchronized void paintComponent(Graphics g) {		
		if (!isVisible() || viewman == null ) return;
		
		clearOffScreen(0);
		Graphics2D g2 = getOffScreenG2();		
		if(g2 == null) return;		
		
		basicstroke = new BasicStroke(((float) STROKE_WIDTH) / getProj().getPPD(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);			
		
		smProfileMouseActions.fireEvent(new DrawLine(smProfileMouseActions, g2));		
		
		for (Map.Entry<Integer, Cue> entry : cues.entrySet()) {
			Cue cue = entry.getValue();
			cue.paintCueLine(g2);
		}		
		super.paintComponent(g);	
	}				
		
	
  BasicInputStateMachine smProfileMouseActions = new BasicInputStateMachine() {
		Point2D p2 = null;
		List<Point2D> profileWPoints = new ArrayList<>();
		List<Point2D> profileSpatialPoints = new ArrayList<>();
		
		boolean closed = false;

		public State START = new State() {
			Transition on = new Event("ON", ">> ON") {				
				public void action() {					
					addAsListenerOf(ProfileLView.this);					
				}
			};
		};

		public State ON = new State() {
			Transition singleclick = new Click(BUTTON1) {
				public void action() {						
					if (isDrawingAllowed())  {
						setCursor(profile_draw_cursor); 
						MouseEvent evt = getMouseEvent();
						if (evt.getClickCount() == 1)
							drawProfileLine(evt);
						else if (evt.getClickCount() == 2)
							finishDrawProfileLine(evt);
					}
				}
			};
									
			Transition mousemoved = new Move() {
				public void action() {				
					if (!closed && !profileWPoints.isEmpty()) {
						p2 = mathconversions.clampedWorldPoint(profileWPoints.get(0), getMouseEvent());
						List<Point2D> points = new ArrayList<Point2D>(profileWPoints);
						points.add(p2);
						Main.setStatusFromWorld(points.toArray(new Point2D[points.size()]));
						repaint();
				}
			  }
		   };

			Transition draw = new Event(DrawLine.class) {
				public void action() {
					Graphics2D g2world = ((DrawLine) getEvent()).getDrawingGraphics();
					Color linecolor = (currentProfileLine == null) ? profileWhileDrawingColor : profileDoneColor;
					Shape shape = (currentProfileLine == null) ? getShapeWorldFromWorldPoints() : currentProfileLine;
					g2world.setColor(linecolor);					
					g2world.setStroke(basicstroke);
					g2world.draw(shape); // draw current profile line
					if (!profileLines.isEmpty()) { //redraw all profiles
						profileLines.forEach((id, profileline) -> {
							if (profileline != null) {								
								g2world.setColor(((ProfileLine) profileline).getLinecolor() == null ? profileDoneColor
										: ((ProfileLine) profileline).getLinecolor());	
								ProfileLine pl  = ((ProfileLine) profileline);																							
								ArrayList<Point2D> worldPoints = pl.getWorldPointsFromOrigSpatialPointsUsingCurrentProj();
								ArrayList<Point2D> newWorldPoints = ShapeUtil.makeConsistentLine(worldPoints, Main.PO, pl.getOrigProjection());
								Shape path = ChartDataConverter.convertToFPath(newWorldPoints, null);
								g2world.draw(path);							
							}
						});						
					}
				}
			};
			
			Transition delete = new Event(DeleteLine.class) {
				public void action() {
					int ID = ((DeleteLine) getEvent()).getDeletedLineID();				
					if (!profileLines.isEmpty()) {
						profileLines.remove(ID);						
					}
					if (!cues.isEmpty()) {
						cues.remove(ID);
					}
					if (profileLines.isEmpty())
						clearPath();					
				}
			};			
		};
		

		private Shape getShapeWorldFromWorldPoints() {				
			return ChartDataConverter.convertToFPath(profileWPoints, closed ? null : p2);
		}
		
		private void clearPath() {
			profileWPoints.clear();	
			profileSpatialPoints.clear();
			p2 = null;			
			currentProfileLine = null;
			// cueChanged(null);
			closed = false;
			repaint();
		}

		private void drawProfileLine(MouseEvent evt) {
			if (closed) {
				profileWPoints.clear();	
				profileSpatialPoints.clear();
				currentProfileLine = null;
				closed = false;				
			}
			Point2D p1;
			if (profileWPoints.isEmpty())
				p1 = getProj().screen.toWorld(evt.getPoint());
			else
				p1 = mathconversions.clampedWorldPoint(profileWPoints.get(0), evt);
			profileWPoints.add(p1);	
			profileSpatialPoints.add(getProj().screen.toSpatial(((WrappedMouseEvent)evt).getRealPoint()));
			p2 = p1;
			repaint();
		}

		private void finishDrawProfileLine(MouseEvent evt) {
			if (!closed) {
			  if (profileSpatialPoints.size() > 1) {				 
				p2 = null;
				ProfileLine profileline = new ProfileLine(profileSpatialPoints);
				profileLines.put(profileline.getID(), profileline);					
				Cue profileCue = new Cue(profileline.getID());
				addMouseMotionListener(profileCue);
				cues.put(profileline.getID(), profileCue);
				Map<Integer, Shape> varProfiles = new LinkedHashMap<>();
				varProfiles.putAll(profileLines);
				controller.addNewProfile(varProfiles);					
				closed = true;
				profileWPoints.clear();	
				profileSpatialPoints.clear();
				currentProfileLine = null;
				repaint();						
			}
		}
	  }
	};
	
	
	@Override
	public void modelPropertyChange(PropertyChangeEvent evt) {
		String propName = evt.getPropertyName();
		Object newVal = evt.getNewValue();
		Supplier<IChartEventHandler> handler = EVENT_HANDLER.get(propName);
		if (handler != null) {
			handler.get().handleEvent(newVal);
		}
	}

    public Shape getProfilelineByID(Integer id) {    	
    	if (! profileLines.isEmpty()) {
    		ProfileLine pl = (ProfileLine) profileLines.get(id);
    		return pl;
    	}
    	return null;
    }
	
	private boolean isDrawingAllowed() {
		boolean allowed = true;
		if (!isVisible() || viewman == null)
			return false;
		
		if (ToolManager.getToolMode() != ToolManager.PROFILE)				
			return false;
			
		return allowed;
	}
	
	
 public static class ProfileLine implements Shape {		
	 private List<Point2D> origProfileSpatialPts = new ArrayList<>();
	 private FPath profilePath = null;
	 private String identifier = "Profile ";
	 private ProjObj origProjection = null;
	 private String rename = null;
	 private Shape profileline = null;	
	 private  Color linecolor = null;
	 private List<Shape> plotshapes = new ArrayList<>();
	 private List<Stroke> plotstrokes = new ArrayList<>();
	 private List<Paint> plotfillpaint = new ArrayList<>();
	 private static final float STANDARD_STROKE = 1.2f;
	 private static final float REDUCED_STROKE = 0.1f;
	 public static final int NUMBER_OF_SERIES = 8;
	 transient private boolean isDeleted = false; 
	 static int uniqueID = 1;
	 private int ID;
	
	 
	 public ProfileLine() {
			super();			
	}	 	 	 	 		

	public ProfileLine(List<Point2D> profileSpatialPts) {
			super();
			this.setProfileOrigSpatialPts(profileSpatialPts);
			this.profilePath = new FPath(profileSpatialPts.toArray(new Point2D[profileSpatialPts.size()]),
										 FPath.SPATIAL_WEST, false); 	        
	        this.origProjection = Main.PO;
	        this.ID = uniqueID;
	        this.identifier = this.identifier + ID;
	        this.isDeleted = false;
	        this.linecolor = (Color) paintSupplier.getNextPaint();
	        populateshapes();
	        populatestrokes();
	        populatefillpaint();
	        uniqueID++;
	}
	
	private void populateshapes() {
		/*
		 * for (int i = 0; i < NUMBER_OF_SERIES; i++) { plotshapes.add(new
		 * GeneralPath()); }
		 */
		plotshapes.add(new GeneralPath());
		plotshapes.add(ShapeUtilities.createDiamond(6.0f));
		plotshapes.add(ShapeUtilities.createDiagonalCross(5.0f, 1.3f));
		plotshapes.add(new Rectangle(-1, -1, 3, 15)); 
		plotshapes.add(ShapeUtilities.createDownTriangle(5.0f));
		plotshapes.add(ShapeUtilities.createRegularCross(5.0f, 1.3f));
		plotshapes.add(ShapeUtilities.createUpTriangle(5.0f));
		plotshapes.add(new Ellipse2D.Float(-2F, -2F, 3F, 12F)); 
    }
	
	private void populatestrokes() {
		for (int i = 0; i < NUMBER_OF_SERIES; i++) {
			Stroke stroke = new BasicStroke(STANDARD_STROKE);
			plotstrokes.add(stroke);
		}
	}
	
	private void populatefillpaint() {
		plotfillpaint.add(new Color(230,230,250));
		plotfillpaint.add(new Color(128,0,128));
		plotfillpaint.add(new Color(240,128,128));
		plotfillpaint.add(new Color(245,222,179));
		plotfillpaint.add(new Color(210,105,30));
		plotfillpaint.add(new Color(235, 177, 52));
		plotfillpaint.add(new Color(52, 235, 73));
		plotfillpaint.add(new Color(227, 57, 85));
	}	
	

public ArrayList<Point2D> getWorldPointsFromOrigSpatialPointsUsingCurrentProj() {
	 ArrayList<Point2D> worldPoints = new ArrayList<>();  //world
	 Iterator<Point2D> iterator = this.origProfileSpatialPts.iterator();
     while(iterator.hasNext()){
    	 Point2D ptSpatial = ((Point2D) iterator.next()); 
    	 Point2D ptWorld = Main.PO.convSpatialToWorld(ptSpatial);
    	 worldPoints.add(ptWorld);   	 
     }
     return worldPoints;
 }
    

 private ArrayList<Point2D> getWorldPointsFromOrigSpatialPointsUsingOrigProj() {
	 ArrayList<Point2D> worldPoints = new ArrayList<>();  //world
	 Iterator<Point2D> iterator = this.origProfileSpatialPts.iterator();
     while(iterator.hasNext()){
    	 Point2D ptSpatial = ((Point2D) iterator.next()); 
    	 Point2D ptWorld = this.origProjection.convSpatialToWorld(ptSpatial);
    	 worldPoints.add(ptWorld);   	 
     }
     return worldPoints;
 } 
 
	public List<Point2D> getProfileSpatialPts() {
		List<Point2D> varOrigProfileSpatialPts = new ArrayList<>();
		varOrigProfileSpatialPts.addAll(this.origProfileSpatialPts);
		return varOrigProfileSpatialPts;
	}

	public void setProfileOrigSpatialPts(List<Point2D> spatialPts) {
		Iterator<Point2D> iterator = spatialPts.iterator();
        while(iterator.hasNext()) {
        	this.origProfileSpatialPts.add((Point2D) iterator.next());
        }		        	
	}

	public FPath getProfilePath() {
		return this.profilePath;
	}

	public void setProfilePath(FPath profilePath) {
		this.profilePath = profilePath;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}		

	public int getID() {
		return ID;
	}

	public void setID(int iD) {
		ID = iD;
	}
	
	public ProjObj getOrigProjection() {
		return this.origProjection;
	}

	public void setOrigProjection(ProjObj projectionForThisLine) {
		this.origProjection = projectionForThisLine;
	}
	

	public static int getUniqueID() {
		return uniqueID;
	}

	public Color getLinecolor() {
		return linecolor;
	}

	public List<Shape> getPlotshapes() {
		List<Shape> varplotshapes = new ArrayList<>();
		varplotshapes.addAll(plotshapes);
		return varplotshapes;
	}

	public List<Stroke> getPlotstrokes() {
		List<Stroke> varsplottrokes = new ArrayList<>();
		varsplottrokes.addAll(plotstrokes);
		return varsplottrokes;
	}

	public List<Paint> getPlotfillpaint() {
		List<Paint> varfillpaint = new ArrayList<>();
		varfillpaint.addAll(plotfillpaint);
		return varfillpaint;
	}

	public void setLinecolor(Color linecolor) {
		this.linecolor = linecolor;
	}

	public boolean isDeleted() {
		return this.isDeleted;
	}

	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public String getRename() {
		return rename;
	}

	public void setRename(String rename) {
		this.rename = rename;
	}			
	
	private String getPlotNameWithCoords() {		
		String brace1 = "(";
		String brace2 = ")";
		String brk = "<br>";
		StringBuilder str = new StringBuilder();
		str.append(this.identifier);
		str.append(brk);
		Iterator<Point2D> iterator = this.origProfileSpatialPts.iterator();		
        while(iterator.hasNext()){
          Point2D ptSpatial = ((Point2D) iterator.next());        
          str.append(brace1);
          str.append(getCoordOrdering().format(ptSpatial));
          str.append(brace2);
          str.append(brk);
        }
        return str.toString();
	}	
	
	private Ordering getCoordOrdering() {
		String coordOrdering = Config.get(Config.CONFIG_LAT_LON,Ordering.LAT_LON.asString());
		Ordering ordering = Ordering.get(coordOrdering);
		return ordering;
	}	
	

	private Shape createShapeInWorldBasedOnOrigProj() {
		ArrayList<Point2D> origworldpoints = getWorldPointsFromOrigSpatialPointsUsingOrigProj();
		FPath origworldpath = new FPath(origworldpoints.toArray(new Point2D[origworldpoints.size()]), FPath.WORLD, false); 
		Shape shape =  origworldpath.getShape();			 
		return shape;
	}	
	
	
	private Shape createShapeInWorldBasedOnCurrentProj() {
		ArrayList<Point2D> currentworldpoints = getWorldPointsFromOrigSpatialPointsUsingCurrentProj();
		FPath currentworldpath = new FPath(currentworldpoints.toArray(new Point2D[currentworldpoints.size()]), FPath.WORLD, false); 
		Shape shape =  currentworldpath.getShape();			 
		return shape;
	}		

	
	@Override
	public Rectangle getBounds() {	
		this.profileline = createShapeInWorldBasedOnOrigProj();
		return this.profileline != null ? this.profileline.getBounds() : null;
	}	

	@Override
	public Rectangle2D getBounds2D() {
		this.profileline = createShapeInWorldBasedOnOrigProj();
		return this.profileline != null ? this.profileline.getBounds2D() : null;
	}

	@Override
	public boolean contains(double x, double y) {
		this.profileline = createShapeInWorldBasedOnOrigProj();
		return this.profileline.contains(x, y);
	}

	@Override
	public boolean contains(Point2D p) {
		this.profileline = createShapeInWorldBasedOnOrigProj();
		return this.profileline != null ? this.profileline.contains(p) : false;
	}

	@Override
	public boolean intersects(double x, double y, double w, double h) {
		this.profileline = createShapeInWorldBasedOnOrigProj();
		return this.profileline != null ? this.profileline.intersects(x, y, w, h) : false;
	}

	@Override
	public boolean intersects(Rectangle2D r) {
		this.profileline = createShapeInWorldBasedOnOrigProj();
		return this.profileline != null ? this.profileline.intersects(r) : false;
	}

	@Override
	public boolean contains(double x, double y, double w, double h) {
		this.profileline = createShapeInWorldBasedOnOrigProj();
		return this.profileline != null ? this.profileline.contains(x, y, w, h) : false;
	}

	@Override
	public boolean contains(Rectangle2D r) {
		this.profileline = createShapeInWorldBasedOnOrigProj();
		return this.profileline != null ? this.profileline.contains(r) : false;
	}

	@Override
	public PathIterator getPathIterator(AffineTransform at) {
		this.profileline = createShapeInWorldBasedOnOrigProj();
		return this.profileline != null ? this.profileline.getPathIterator(at) : null;
	}

	@Override
	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		this.profileline = createShapeInWorldBasedOnOrigProj();
		return this.profileline != null ? this.profileline.getPathIterator(at, flatness) : null;
	}
	 
  }
 	
	class Cue extends MouseMotionAdapter {
		private int cueLineLengthPixels = 4;
		GeneralPath baseCueShape;
		Shape cueShape = null;
		int profileID;

		public Cue(int ID) {
			super();
			this.profileID = ID;
			GeneralPath gp = new GeneralPath();
			gp.moveTo(0, -cueLineLengthPixels / 2);
			gp.lineTo(0, cueLineLengthPixels / 2);
			baseCueShape = gp;
		}

		public void setCuePoint(Point2D worldCuePoint) {
			Shape oldCueShape = cueShape;

			if (worldCuePoint == null)
				cueShape = null;
			else
				cueShape = computeCueLine(worldCuePoint);

			if (oldCueShape != cueShape)
				repaint();
		}

		private Shape computeCueLine(Point2D worldMouse) {
			Shape pl = ProfileLView.this.getProfilelineByID(this.profileID);
			ArrayList<Point2D> worldPoints = ((ProfileLine) pl).getWorldPointsFromOrigSpatialPointsUsingCurrentProj();
			ArrayList<Point2D> newWorldPoints = ShapeUtil.makeConsistentLine(worldPoints, Main.PO, ((ProfileLine) pl).getOrigProjection());
			Shape profileworld = ChartDataConverter.convertToFPath(newWorldPoints, null);			
			if (profileworld == null)
				return null;
			double t = ProfileLView.this.mathconversions.uninterpolate(profileworld, worldMouse, null);
			Shape newCueShape = null;
			if (!Double.isNaN(t) && t >= 0.0 && t <= 1.0) {
				Point2D mid = ProfileLView.this.mathconversions.interpolate(profileworld, t);
				double angle = ProfileLView.this.mathconversions.angle(profileworld, t);
				double scale = cueLineLengthPixels * getProj().getPixelWidth();				
				AffineTransform at = new AffineTransform();
				at.translate(mid.getX(), mid.getY());
				at.rotate(angle);				
				at.scale(scale, scale);
				newCueShape = baseCueShape.createTransformedShape(at);
			}
			return newCueShape;
		}

		public void paintCueLine(Graphics2D g2) {
			Shape pl = ProfileLView.this.getProfilelineByID(this.profileID);
			if (pl != null && cueShape != null) {
				g2.setColor(ThemeChart.getProfileCrosshairColor());
				g2.draw(cueShape);
			}
		}

		public void mouseMoved(MouseEvent e) {
			Shape pl = ProfileLView.this.getProfilelineByID(this.profileID);
			if (pl == null) return;
			ArrayList<Point2D> worldPoints = ((ProfileLine) pl).getWorldPointsFromOrigSpatialPointsUsingCurrentProj();
			ArrayList<Point2D> newWorldPoints = ShapeUtil.makeConsistentLine(worldPoints, Main.PO, ((ProfileLine) pl).getOrigProjection());
			Shape profileworld = ChartDataConverter.convertToFPath(newWorldPoints, null);						
			Point2D pt = ProfileLView.this.mathconversions.clampedWorldPoint(ProfileLView.this.mathconversions.getFirstPoint(profileworld), e);
			double[] distance = new double[1];
			Point2D mid =  ProfileLView.this.mathconversions.interpolate(profileworld, ProfileLView.this.mathconversions.uninterpolate(profileworld, pt, distance));
			int distInPixels = (int) Math.round(distance[0] * getProj().getPPD());			
			if (distInPixels <= 50) {
				tooltipsDisabled(true);
				//this has to be in World for profile's orig projection, otherwise numeric sample will not find it
				Point2D spatial = getProj().world.toSpatial(mid);
				Point2D midOrigWorld = ((ProfileLine) pl).getOrigProjection().convSpatialToWorld(spatial);				
				controller.cueChanged(this.profileID,midOrigWorld);
				setCuePoint(mid);
			} else {
				tooltipsDisabled(false);
				controller.cueChanged(this.profileID,null);
				setCuePoint(null);
			}
		}
	}
	
	@Override
	public SerializedParameters getInitialLayerData() {
		//additional check - sometimes we happen to have an abnormal case
		//when there is more than one (1) Profile layer in Overlays.
		//To prevent NPE caused by that abnormality, adding hack-check here and skipping serialization
		if (countHowManyProfileLayerExist() != 1) {
			log.aprintln("Skipping serialization for Profile, as this session has more than one (1) ProfileLView instance which is an erroneous condition.");
			return null;
		}
		Map<ConfigType, edu.asu.jmars.layer.profile.config.Config> bothConfig = getBothConfig(); 
		edu.asu.jmars.layer.profile.config.Config currentConfig = getCurrentConfig();
		boolean isChartFPDocked = isChartFocusPanelDocked();
		List<MapSource> mapsources = this.myLayer.getMapSources();
		Builder savedParamsBuilder = SavedParams.builder();
		savedParamsBuilder.withProfiles(this.profileLines);
		savedParamsBuilder.withMapsources(mapsources);
		savedParamsBuilder.withCurrentconfig(currentConfig);
		savedParamsBuilder.withBothconfig(bothConfig);
		savedParamsBuilder.withIsChartFocusPanelDocked(isChartFPDocked);
		return  savedParamsBuilder.build();
	}

	private int countHowManyProfileLayerExist() {
		MovableList<LView> lst = LManager.getLManager().viewList;
		int cnt = 0;
		for(LView lview : lst) {
			if (lview instanceof ProfileLView)
				cnt += 1;
		}
		return cnt;
	}

	private boolean isChartFocusPanelDocked() {
		if (this.chartFocusPanel != null) {
			return this.chartFocusPanel.isDocked();
		}
		return false;
	}


	public static class SavedParams implements SerializedParameters {
		private static final long serialVersionUID = 3315889448931414307L;	
		
		private boolean isChartFocusPanelDocked = false;
		private List<SerializedProfileObject> profileObjects = new ArrayList<>();
		private List<MapSource> mapsources = new ArrayList<>();
		private List<SerializedProfileObject> configProfileObjectsONESOURCE = new ArrayList<>();
		private List<SerializedProfileObject> configProfileObjectsMANYSOURCES = new ArrayList<>();
		private List<MapSource> configMapsourcesONESOURCE = new ArrayList<>();
		private List<MapSource> configMapsourcesMANYSOURCES = new ArrayList<>();
		private edu.asu.jmars.layer.profile.config.ConfigType currentConfigType;


		private SavedParams(Builder builder) {

			if (!builder.profiles.isEmpty()) {
				profileObjects.clear();
				for (Map.Entry<Integer, Shape> entry : builder.profiles.entrySet()) {
					ProfileLine profile = (ProfileLine) entry.getValue();
					SerializedProfileObject spo = new SerializedProfileObject(profile);
					profileObjects.add(spo);
				}
			}
			if (!builder.mapsources2.isEmpty()) {
				this.mapsources.clear();
				Iterator<MapSource> iterator = builder.mapsources2.iterator();
				while (iterator.hasNext()) {
					this.mapsources.add((MapSource) iterator.next());
				}
			}

			if (builder.currentconfig != null) {
			    currentConfigType = builder.currentconfig.getConfigType();
			} else {
				currentConfigType = ConfigType.ONENUMSOURCE;
			}

			this.isChartFocusPanelDocked = builder.isChartFocusPanelDocked;

			serializeBothConfig(builder.bothconfig);
		}

		private void serializeBothConfig(Map<ConfigType, edu.asu.jmars.layer.profile.config.Config> bothconfig) {
			edu.asu.jmars.layer.profile.config.Config onesourceconfig = bothconfig.get(ConfigType.ONENUMSOURCE);
			edu.asu.jmars.layer.profile.config.Config manysourcesconfig = bothconfig.get(ConfigType.MANYNUMSOURCES);
			
			serializeONESOURCE(onesourceconfig);
			serializeMANYSOURCES(manysourcesconfig);
		}


		private void serializeONESOURCE(edu.asu.jmars.layer.profile.config.Config onesourceconfig) {
			Map<Integer, Shape> configprofiles = onesourceconfig.getProfilesToChart();
			if (configprofiles != null && !configprofiles.isEmpty()) {
				configProfileObjectsONESOURCE.clear();
				for (Map.Entry<Integer, Shape> entry : configprofiles.entrySet()) {
					ProfileLine profile = (ProfileLine) entry.getValue();
					SerializedProfileObject spo = new SerializedProfileObject(profile);
					configProfileObjectsONESOURCE.add(spo);
				}
			}
			List<MapSource> configsources = onesourceconfig.getNumsourcesToChart();
			if (!configsources.isEmpty()) {
				configMapsourcesONESOURCE.clear();
				Iterator<MapSource> iterator = configsources.iterator();
				while (iterator.hasNext()) {
					configMapsourcesONESOURCE.add((MapSource) iterator.next());
				}
			}
		}
		
		private void serializeMANYSOURCES(edu.asu.jmars.layer.profile.config.Config manysourcesconfig) {
			Map<Integer, Shape> configprofiles = manysourcesconfig.getProfilesToChart();
			if (configprofiles != null && !configprofiles.isEmpty()) {
				configProfileObjectsMANYSOURCES.clear();
				for (Map.Entry<Integer, Shape> entry : configprofiles.entrySet()) {
					ProfileLine profile = (ProfileLine) entry.getValue();
					SerializedProfileObject spo = new SerializedProfileObject(profile);
					configProfileObjectsMANYSOURCES.add(spo);
				}
			}
			List<MapSource> configsources = manysourcesconfig.getNumsourcesToChart();
			if (!configsources.isEmpty()) {
				configMapsourcesMANYSOURCES.clear();
				Iterator<MapSource> iterator = configsources.iterator();
				while (iterator.hasNext()) {
					configMapsourcesMANYSOURCES.add((MapSource) iterator.next());
				}
			}
		}

		public List<SerializedProfileObject> getProfileObjects() {
			return profileObjects;
		}

		public List<MapSource> getMapsources() {
			return mapsources;
		}
		
		public List<SerializedProfileObject> getConfigProfilesONESOURCE() {
			return configProfileObjectsONESOURCE;
		}
		
		public List<MapSource> getConfigMapSourcesONESOURCE() {
			return configMapsourcesONESOURCE;
		}	
		
		public List<SerializedProfileObject> getConfigProfilesMANYSOURCES() {
			return configProfileObjectsMANYSOURCES;
		}
		
		public List<MapSource> getConfigMapSourcesMANYSOURCES() {
			return configMapsourcesMANYSOURCES;
		}			
		
		public edu.asu.jmars.layer.profile.config.ConfigType getCurrentConfigType() {
			return currentConfigType;
		}
		
		public boolean isChartFPDocked() {
			return isChartFocusPanelDocked;
		}
		

		private static class SerializedProfileObject implements SerializedParameters  {							
			private static final long serialVersionUID = -9020649117153443919L;
			private List<Point2D> spatialpoints = new ArrayList<>();
			private transient ProjObj initialprojection = null;
			private double centerLon, centerLat;
			private Color linecolor = null;	
			private boolean isSelectedToViewChart; //backward compatibility with Phase 1
			private String name;
			private String rename;
			private int ID; //will be used on deserialization
		

			public SerializedProfileObject(ProfileLine profile) {
				this.spatialpoints = profile.getProfileSpatialPts();
				this.initialprojection = profile.getOrigProjection();
				this.centerLon = this.initialprojection.getProjectionCenter().getX();
				this.centerLat = this.initialprojection.getProjectionCenter().getY();	
				this.linecolor = profile.getLinecolor();
				this.name = profile.getIdentifier();
				this.rename = (profile.getRename() != null ? profile.getRename() : null);
			}

			public List<Point2D> getSpatialpoints() {
				return spatialpoints;
			}

			public ProjObj getInitialprojection() {
				return initialprojection;
			}

			public double getCenterLon() {
				return centerLon;
			}

			public double getCenterLat() {
				return centerLat;
			}

			public Color getLinecolor() {				
				return linecolor;
			}

			public String getRename() {
				return rename;
			}

			public String getName() {
				return name;
			}

			public boolean isSelectedToViewInChart() {
				return isSelectedToViewChart;
			}
		}

		public static Builder builder() {
			return new Builder();
		}


		public static final class Builder {
			private Map<Integer, Shape> profiles = new HashMap<>();
			private List<MapSource> mapsources2 = new ArrayList<>();
			private Map<ConfigType, edu.asu.jmars.layer.profile.config.Config> bothconfig = new HashMap<>();
			private edu.asu.jmars.layer.profile.config.Config currentconfig;
			private boolean isChartFocusPanelDocked;

			private Builder() {
			}

			public Builder withProfiles(Map<Integer, Shape> profiles) {
				this.profiles.clear();
				this.profiles.putAll(profiles);
				return this;
			}

			public Builder withMapsources(List<MapSource> mapsources) {
				this.mapsources2.clear();
				this.mapsources2.addAll(mapsources);
				return this;
			}

			public Builder withBothconfig(Map<ConfigType, edu.asu.jmars.layer.profile.config.Config> bothconfig) {
				this.bothconfig.clear();
				this.bothconfig.putAll(bothconfig);
				return this;
			}

			public Builder withCurrentconfig(edu.asu.jmars.layer.profile.config.Config currentconfig) {
				this.currentconfig = currentconfig;
				return this;
			}

			public Builder withIsChartFocusPanelDocked(boolean isChartFPDocked) {
				this.isChartFocusPanelDocked = isChartFPDocked;
				return this;
			}

			public SavedParams build() {
				return new SavedParams(this);
			}
		}
		
	}

	public void updatedProfileLines(SavedParams savedParams) {
		for (SavedParams.SerializedProfileObject saved : savedParams.getProfileObjects()) {
			List<Point2D> spatialpoints = saved.getSpatialpoints();
			ProjObj proj = new ProjObj.Projection_OC(saved.getCenterLon(), saved.getCenterLat());
			ProfileLine profileline = new ProfileLine(spatialpoints);
			saved.ID = profileline.getID();
			profileline.setIdentifier(saved.getName());
			profileline.setOrigProjection(proj);
			profileline.setLinecolor(saved.getLinecolor());
			profileline.setRename(saved.getRename() != null ? saved.getRename() : null);
			this.profileLines.put(profileline.getID(), profileline);
			Cue profileCue = new Cue(profileline.getID());
			addMouseMotionListener(profileCue);
			this.cues.put(profileline.getID(), profileCue);
		}
	}
	
	
	public Map<Integer, Shape> restoreConfigProfilesONESOURCE(SavedParams savedParams) {
		Map<Integer, Shape> configprofiles = new HashMap<>();
		List<SavedParams.SerializedProfileObject> restoredprofiles = savedParams.getProfileObjects();
		// sync up with exisitng or already restored profiles
		int sameID = -1;
		if (savedParams.getConfigProfilesONESOURCE() != null) {
			for (SavedParams.SerializedProfileObject savedconfig : savedParams.getConfigProfilesONESOURCE()) {
				String sameprofileidentifier = savedconfig.getName();
				for (SavedParams.SerializedProfileObject restoredprofile : restoredprofiles) {
					if (sameprofileidentifier.equals(restoredprofile.getName())) {
						sameID = restoredprofile.ID;
						if (!this.profileLines.isEmpty()) {
							Shape configprofile = this.profileLines.get(sameID);
							configprofiles.put(sameID, configprofile);
						}
						break;
					}
				}
			}
		} else if (savedParams.getConfigProfilesONESOURCE() == null) { // means Phase 1 session
			for (SavedParams.SerializedProfileObject restoredprofile : restoredprofiles) {
				if (restoredprofile.isSelectedToViewInChart()) {
					sameID = restoredprofile.ID;
					if (!this.profileLines.isEmpty()) {
						Shape configprofile = this.profileLines.get(sameID);
						configprofiles.put(sameID, configprofile);
					}
				}
			}
		}
		return configprofiles;
	}
	
	public List<MapSource> restoreConfigSourcesONESOURCE(SavedParams savedParams) {
		List<MapSource> configsources = new ArrayList<>();
		List<MapSource> savedconfig = savedParams.getConfigMapSourcesONESOURCE();
		if(savedconfig != null) {
		    configsources.addAll(savedconfig);
		} else if (savedParams.getMapsources() != null) {  //for backward compatibility - means no prior Config (default will be ONENUMSOURCE)
			configsources.add(savedParams.getMapsources().get(0)); //prior to Config - will be 1 map source
		}
		return configsources;
	}		
	
	
	public Map<Integer, Shape> restoreConfigProfilesMANYSOURCES(SavedParams savedParams) {
		Map<Integer, Shape> configprofiles = new HashMap<>();
		List<SavedParams.SerializedProfileObject> restoredprofiles = savedParams.getProfileObjects();
		// sync up with exisitng or already restored profiles
		int sameID = -1;
		if (savedParams.getConfigProfilesMANYSOURCES() != null) {
			for (SavedParams.SerializedProfileObject savedconfig : savedParams.getConfigProfilesMANYSOURCES()) {
				String sameprofileidentifier = savedconfig.getName();
				for (SavedParams.SerializedProfileObject restoredprofile : restoredprofiles) {
					if (sameprofileidentifier.equals(restoredprofile.getName())) {
						sameID = restoredprofile.ID; 
						if (!this.profileLines.isEmpty()) {
							Shape configprofile = this.profileLines.get(sameID);
							configprofiles.put(sameID, configprofile);
						}
						break;
					}
				}
			}
		}
		return configprofiles;
	}
	
	public List<MapSource> restoreConfigSourcesMANYSOURCES(SavedParams savedParams) {
		List<MapSource> configsources = new ArrayList<>();
		List<MapSource> savedconfig = savedParams.getConfigMapSourcesMANYSOURCES();
		if(savedconfig != null) {
		    configsources.addAll(savedconfig);
		} else if (savedParams.getMapsources() != null) {  //for backward compatibility - means no prior Config (default will be ONENUMSOURCE)
			configsources.addAll(savedParams.getMapsources()); //prior to Config - will be 1 map source
		}
		return configsources;
	}		
		
	private void createCalloutUI() {
		JLabel dummy = new JLabel();
		EdgedBalloonStyle style = new EdgedBalloonStyle(ThemeSnackBar.getBackgroundStandard(), 
	                ThemeProvider.getInstance().getBackground().getBorder());
		 BalloonTip.setDefaultCloseButtonIcons(close, close, close);
		 myBalloonTip = new CustomBalloonTip(dummy,
				  dummy,
				  new Rectangle(),
				  style,
				  BalloonTip.Orientation.LEFT_BELOW, BalloonTip.AttachLocation.CENTER,
				  10, 20,
				  true);	
		 myBalloonTip.setPadding(5);
		 JButton closebutton = BalloonTip.getDefaultCloseButton();
		 closebutton.setUI(new LikeLabelButtonUI());		 
		 myBalloonTip.setCloseButton(closebutton,false);		
		 myBalloonTip.setVisible(false);
	}	
	
	private void showCallout(Container parent2) {	
		if (myBalloonTip != null && parent2 != null) {
			if (parent2 instanceof JComponent) {
				JComponent comp = (JComponent) parent2;
				if (comp.getRootPane() == null) {
					return;
				}
				myBalloonTip.setAttachedComponent(comp);
				int xoffset = parent2.getWidth() / 2;
				int yoffset = parent2.getHeight();
				Rectangle rectoffset = new Rectangle(xoffset, yoffset, 1, 1);
				Color foregroundtext = ThemeSnackBar.getForegroundStandard();
				String colorhex = edu.asu.jmars.ui.looknfeel.Utilities.getColorAsBrowserHex(foregroundtext);
				String html = "<html>" + "<p style=\"color:" + colorhex + "; padding:1em; text-align:center;\">" + "<b>"
						+ "select 'Draw Profile' tool to start drawing" + "</b>" + "</p></html>";
				myBalloonTip.setTextContents(html);
				myBalloonTip.setOffset(rectoffset);
				TimingUtils.showTimedBalloon(myBalloonTip, 5000); // callout disappears in 5 sec
			}
		}
	}
	
	public void hideCallout() {
		if (myBalloonTip != null) {
			myBalloonTip.setVisible(false);
		}
	}

	public boolean isCalloutVisible() {
		return (myBalloonTip != null && myBalloonTip.isVisible());
	}
	
	private class UpdateCue implements IChartEventHandler {
	
		@Override
		public void handleEvent(Object newVal) { // from chart to main view
			Pair newval = (Pair) newVal;
			Pair cuevalues = (Pair) newval.getKey();
			ProfileLView profileview = (ProfileLView) newval.getValue();
			Integer ID = (Integer) cuevalues.getKey();
			Point2D cueOrigWorldPoint = (Point2D) cuevalues.getValue();
			if (cueOrigWorldPoint == null)
				return;
			Cue cue = profileview.cues.get(ID);
			Shape pl = profileview.getProfilelineByID(ID);
			if (pl != null) {
				// cueOrigWorldPoint is in orig World projection (from numeric sample which
				// doesn't change on reprojection, so convert
				Point2D cueSpatial = ((ProfileLine) pl).getOrigProjection().convWorldToSpatial(cueOrigWorldPoint);
				Point2D cueCurrentWorldPoint = Main.PO.convSpatialToWorld(cueSpatial);
				cue.setCuePoint(cueCurrentWorldPoint);
			}
		}
	}
	
	private class LineWidth implements IChartEventHandler {
		@Override
		public void handleEvent(Object newVal) {
			Pair newval = (Pair) newVal;
			ProfileLView profileview =  (ProfileLView) newval.getKey();
			Integer newWidth =  (Integer) newval.getValue();
			if (newWidth == null) return;
			profileview.STROKE_WIDTH = newWidth;		
			profileview.repaint();
		}
	}

	/**
	 *  this method is called from FeatureMouseHandler, context menu
	 *  to convert polyline to a profile.
	 *  The converted profile is then managed under Profile layer.
	 * @param selectedpolylines - feature path in spatial West
	 */
	public void convertToProfile(List<FPath> exportedpolylines) {
		List<Point2D> exportedspatialpointList = new ArrayList<>();
		for (FPath polyline : exportedpolylines) {
			exportedspatialpointList.clear();
			FPath mypath = polyline;
			Point2D[] pointArray = {};
			pointArray = mypath.getVertices();
			for (Point2D point : pointArray) {
				exportedspatialpointList.add(new Point2D.Double(point.getX(), point.getY()));
			}

			ProfileLine profileline = new ProfileLine(exportedspatialpointList);
			profileLines.put(profileline.getID(), profileline);
			Cue profileCue = new Cue(profileline.getID());
			addMouseMotionListener(profileCue);
			cues.put(profileline.getID(), profileCue);
		}
		Map<Integer, Shape> varProfiles = new LinkedHashMap<>();
		varProfiles.putAll(profileLines);
		controller.addNewProfile(varProfiles);		
		showExportConfirmation(exportedpolylines);	
		repaint();
	}

	private void showExportConfirmation(List<FPath> exportedpolylines) {
		if (exportedpolylines.isEmpty()) {
			return;
		}
		Color foregroundtext = ThemeSnackBar.getForegroundStandard();
		String colorhex = edu.asu.jmars.ui.looknfeel.Utilities.getColorAsBrowserHex(foregroundtext);
		String myhtml = "<html>" + "<p style=\"color:" + colorhex + "; padding:1em; text-align:center;\">"
				+ "You've successfully exported " + exportedpolylines.size()
				+ (exportedpolylines.size() > 1 ? " polylines " : " polyline ") + "into Profile layer<br>"
				+ "You can now view the newly created " + (exportedpolylines.size() > 1 ? " profiles " : " profile ")
				+ " using the Profile layer and Charts viewer. " + "</p></html>";
		edu.asu.jmars.Main.mainFrame.showCallout(Main.testDriver, myhtml);
	}
	
}
