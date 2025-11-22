package edu.asu.jmars.layer.profile.manager;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CHECK_OFF_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CHECK_ON_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.TRASH;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Shape;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.profile.ChartDataConverter;
import edu.asu.jmars.layer.profile.ProfileLView;
import edu.asu.jmars.layer.profile.ProfileLView.ProfileLine;
import edu.asu.jmars.layer.profile.config.ConfigType;
import edu.asu.jmars.layer.profile.swing.ButtonColumn;
import edu.asu.jmars.layer.profile.swing.ProfileTabelCellObject;
import edu.asu.jmars.parsers.gis.CoordinatesParser.Ordering;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.TableCellBalloonTip;
import net.java.balloontip.positioners.RightAbovePositioner;
import net.java.balloontip.styles.EdgedBalloonStyle;


public class ProfileManagerTableModel extends AbstractTableModel implements Observer, ItemListener {
	private static final long serialVersionUID = 1L;

	public  final String SHOW_HIDE = "Select to plot";
	public  final String TITLE = "Name";
	public  final String COLOR = "Color";
	private final String TITLE_AND_COLOR = "Profile";
	private final String DISTANCE = "Distance (km)";
	private final String LATLON_COORDS = "Start-End Coordinates";
	public  final String REMOVE = "Delete";	
	private List<ProfileObject> profileObjList = new ArrayList<>();
	private Map<Integer, Shape> profiles = new LinkedHashMap<>();
	private final ChartDataConverter mathconversions;
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static Icon trash = new ImageIcon(ImageFactory.createImage(TRASH.withDisplayColor(imgColor)));
	private TableCellBalloonTip myBalloonTip;
	private JTable jTable;
	private Color imgBlack = Color.BLACK;
	private Icon close = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CLEAR.withDisplayColor(imgBlack)));
	private JPanel delPanel = new JPanel();
	private JCheckBox chkBox1 = new JCheckBox();
	private JLabel delLabel = new JLabel();
	private Icon chkboxOFFImg = new ImageIcon(ImageFactory.createImage(CHECK_OFF_IMG.withDisplayColor(imgBlack)));
	private Icon chkboxONImg = new ImageIcon(ImageFactory.createImage(CHECK_ON_IMG.withDisplayColor(imgBlack)));						
	private ProfileObject selectedRowWhenDel = null;
	private edu.asu.jmars.layer.profile.config.Config chartConfig = null;
	public final String[] columnNames = new String[] { SHOW_HIDE,     TITLE,         COLOR,       TITLE_AND_COLOR,               DISTANCE,     LATLON_COORDS,  REMOVE };
	private final Class[] columnClass = new Class[] {   Boolean.class, String.class, Color.class,  ProfileTabelCellObject.class,  String.class, String.class,   ButtonColumn.class};
			
	protected String[] columnToolTips = {
		    "Select to view in chart", // show/hide column
		    "Profile name. Double click in cell to rename", //name of profile line
		    "Click in cell to change color",   //Color
		    "Profile as its appears in Main view",
		    "Linear distance (km)",   //Distance
		    "Start-End lat/lon coordinates",   //lat/lon	
		    "<html><body>"
			+ "<div >Delete profile.</div>"
			+ "<div>Note, this action deletes profile from this table, chart and Main view.</div></body></html>" 
   };
	
	public ProfileManagerTableModel(ProfileLView profileLView) {
		this.mathconversions = new ChartDataConverter(profileLView);
		Main.coordhandler.addObserver(this);
		Main.longitudeswitch.addObserver(this);
		Main.latitudeswitch.addObserver(this);	
	}
	

	public void addData(Map<Integer, Shape> allprofiles, ProfileLView profileLView) {
		profileObjList.clear();
		profiles.clear();
		profiles.putAll(allprofiles); //preserve input		
		allprofiles.forEach((id, shape) -> { //create objects for table model
			if (shape instanceof ProfileLine) {
				ProfileLine profile = (ProfileLine) shape;
				ProfileObject profileObj = new ProfileObject(profile, mathconversions);
				profileObjList.add(profileObj);
			}
		});
		if (this.chartConfig != null && this.chartConfig.getConfigType() == ConfigType.ONENUMSOURCE) {
			updateModelObjectsBasedOnChartConfig();
		}
		fireTableDataChanged();
	}
	

	private void updateModelObjectsBasedOnChartConfig() {
		Map<Integer, Shape> selectedprofiles = new HashMap<>();
		selectedprofiles.putAll(this.chartConfig.getProfilesToChart());

		for (ProfileObject profileObj : profileObjList) {
			Integer ID = profileObj.getProfileID();
			Shape shape = selectedprofiles.get(ID);
			if (shape != null) { //if this profile exist in selected profiles, then mark model obj "selected"
				profileObj.setSelectedForChart(true);
			}
		}
	}


	public void withConfig(edu.asu.jmars.layer.profile.config.Config chartConfigOneSource) {
		this.chartConfig = chartConfigOneSource;		
	}
	
	public Map<Shape, Boolean> getModelData() {
		Map<Shape, Boolean> modeldata = new HashMap<>();
		for (ProfileObject profileObj : profileObjList) {
			modeldata.put(profileObj.getOrigprofile(), profileObj.isSelectedForChart());
		}
		return modeldata;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columnClass[columnIndex];
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	public String[] getColumnToolTips() {
		return columnToolTips;
	}

	@Override
	public int getRowCount() {
		return profileObjList.isEmpty() ? 0 : profileObjList.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (profileObjList.isEmpty())
			return null;

		ProfileObject row = profileObjList.get(rowIndex);

		// show/hide
		if (getColumnName(columnIndex).equals(SHOW_HIDE)) {			
			return row.isSelectedForChart();
		}
		// profile line name or rename
		else if (getColumnName(columnIndex).equals(TITLE)) {
			return row.getProfilename();
		}
		// color
		else if (getColumnName(columnIndex).equals(COLOR)) {
			return row.getProfileColor();
		}
		//profile name with color line
		else if (getColumnName(columnIndex).equals(TITLE_AND_COLOR)) {
			ProfileTabelCellObject cellobj = new ProfileTabelCellObject(row.getProfilename());
			cellobj.setColor(row.getProfileColor());
			return cellobj;
		}
		// Distance km
		else if (getColumnName(columnIndex).equals(DISTANCE)) {
			return row.getDistanceFormatted();
		}
		// starting lat/lon coords
		else if (getColumnName(columnIndex).equals(LATLON_COORDS)) {
			return row.getProfileStartEndCoords();
		}
		//remove
		else if (getColumnName(columnIndex).equals(REMOVE)) {
			return trash;
		}	
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int colIndex) {
		if (getColumnName(colIndex).equals(COLOR))
			return true;
		else if (getColumnName(colIndex).equals(SHOW_HIDE))
			return true;
		else if (getColumnName(colIndex).equals(TITLE))
			return true;
		else if (getColumnName(colIndex).equals(REMOVE))
			return true;
		else if (getColumnName(colIndex).equals(LATLON_COORDS))
			return true;		
		return false;
	}

	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if (profileObjList.isEmpty())
			return;
		
		ProfileObject row = profileObjList.get(rowIndex);
		
		 if(SHOW_HIDE.equals(getColumnName(columnIndex))) {
			 hideCallout();			 
             row.setSelectedForChart(((Boolean)value).booleanValue());  
             fireTableDataChanged();  
             
         }	
		 else if(COLOR.equals(getColumnName(columnIndex))) {
			 hideCallout();
             row.setProfileColor(((Color)value));   
             fireTableChanged(new TableModelEvent(this, 0, rowIndex, columnIndex,
                     TableModelEvent.UPDATE));
         }	
		 else if(TITLE.equals(getColumnName(columnIndex))) {
			 hideCallout();
			 String newname = (String)value;
			 if (newname != null && !newname.trim().isEmpty()) {
				 row.setRename(newname);   
			 }
			 fireTableChanged(new TableModelEvent(this, 0, rowIndex, columnIndex,
                     TableModelEvent.UPDATE));
         }	
		 else if(REMOVE.equals(getColumnName(columnIndex))) {	
				myBalloonTip.setCellPosition(rowIndex, columnIndex);
				String profilename = (row.getRename() != null ? row.getRename() : row.getProfilename());
				String content = "Delete " + profilename + "?";
				String html = "<html>" + "<p style=\"color:black; padding:1em; text-align:center;\">" + "<b>"
						+ content + "</b>" + "</p></html>";
				delLabel.setText(html);
				chkBox1.setSelected(false);		
				setUserSelectedRowWhenDel(row); 
				myBalloonTip.setAttachedComponent(this.jTable);
				myBalloonTip.setVisible(true);					
			 }			       		 
	}
	
	private void setUserSelectedRowWhenDel(ProfileObject row) {
		this.selectedRowWhenDel = row;
	}
	
	public List<ProfileObject> getSampleData() {
		List<ProfileObject> varprofileObjList = new ArrayList<>();
		varprofileObjList.addAll(this.profileObjList);
		return varprofileObjList;
	}
	
	public String[] getInitialColumnNames() {
		return columnNames;
	}
	
	public void withTable(JTable readoutTable) {
		this.jTable = readoutTable;
		createCalloutUI(this.jTable);		
	}	
	
	private void createCalloutUI(JTable table) {
		Color hicolor = ThemeProvider.getInstance().getBackground().getHighlight();
		BalloonTip.setDefaultCloseButtonIcons(close, close, close);
		delPanel.setLayout(new FlowLayout());
		delPanel.setBackground(hicolor);
        delPanel.add(chkBox1);
        chkBox1.setSelected(false);
        chkBox1.setBackground(hicolor);
        chkBox1.addItemListener(this);
        chkBox1.setSelectedIcon(chkboxONImg);
        chkBox1.setIcon(chkboxOFFImg);
        delPanel.add(delLabel);
		EdgedBalloonStyle style = new EdgedBalloonStyle(hicolor, ThemeProvider.getInstance().getBackground().getBorder());				
		JTable dummy = new JTable();
		myBalloonTip = new TableCellBalloonTip(dummy, delPanel, 0, 0, style, 
							new RightAbovePositioner(40, 20),
							null);		
		myBalloonTip.setCloseButton(BalloonTip.getDefaultCloseButton(),false);	
		myBalloonTip.setVisible(false);
	}	
		

	@Override
	public void update(Observable o, Object arg) {
		fireTableDataChanged();		
	}	
	
	@Override
	public void itemStateChanged(ItemEvent e) {
        if (e.getSource() == this.chkBox1) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
            	if (this.selectedRowWhenDel != null) {
            		this.selectedRowWhenDel.setDeleted(true);         			   
       			    fireTableDataChanged();  
            	}
            }
        }
        hideCallout();
	} 
	
	public void hideCallout() {
		if (myBalloonTip != null) {
			myBalloonTip.setVisible(false);
		}
	}		
	
	
	private static class ProfileObject {
		
		private double distance = Double.NaN;
		private DecimalFormat df = new DecimalFormat("#,###,##0.00");
		private ProfileLine origprofile;		
		private ChartDataConverter converter;
		NumberFormat nf = NumberFormat.getNumberInstance();
		public static final String NO_VALUE = "Value Unavailable";
		private boolean isSelected = false;
		

		public ProfileObject(ProfileLine profile, ChartDataConverter mathconversions) {
			this.origprofile = profile;
			this.converter = mathconversions;
		}		

		public void setDeleted(boolean b) {
			this.origprofile.setDeleted(b);

		}

		public String getProfilename() {		
			return getRename() != null ? getRename() : this.origprofile.getIdentifier();
		}
		
		public ProfileLine getOrigprofile() {
			return origprofile;
		}		
		
		public String getProfileStartEndCoords() {	
			StringBuffer strArray = new StringBuffer();
			List<Point2D> points = this.origprofile.getProfileSpatialPts();
			if (!points.isEmpty()) {
				Point2D ptStart = points.get(0);
				 strArray.append(getCoordOrdering().format(ptStart));
				 if (points.size() > 1) {
					 Point2D ptEnd = points.get(points.size() - 1);
					 strArray.append("  ");
					 strArray.append(getCoordOrdering().format(ptEnd));					 
				 }
			}				  
	        return strArray.toString();
		}

		public int getProfileID() {
			return this.origprofile.getID();
		}

		public Color getProfileColor() {
			return this.origprofile.getLinecolor();
		}
		
		public void setProfileColor(Color linecolor) {
			this.origprofile.setLinecolor(linecolor);
		}
				

		public String getDistanceFormatted() {
			this.distance =  this.converter.perimeterLength(this.origprofile)[1];
			if (Double.isNaN(this.distance))
				return NO_VALUE;
			return df.format(this.distance);
		}		
			

		public boolean isSelectedForChart() {
			return isSelected;
		}

		public void setSelectedForChart(boolean varisSelected) {
			isSelected = varisSelected;
		}

		public String getRename() {
			return this.origprofile.getRename();
		}

		public void setRename(String rename) {
			this.origprofile.setRename(rename);
		}


		public double getDistance() {
			this.distance = this.converter.perimeterLength(this.origprofile)[1];
			return this.distance;
		}

		private Ordering getCoordOrdering() {
			String coordOrdering = Config.get(Config.CONFIG_LAT_LON,Ordering.LAT_LON.asString());
			Ordering ordering = Ordering.get(coordOrdering);
			return ordering;
		}	
		
	}
}
