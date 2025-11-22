package edu.asu.jmars.layer.profile.config;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.REMOVE;
import java.awt.Color;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import edu.asu.jmars.layer.profile.ProfileFactory;
import edu.asu.jmars.layer.profile.ProfileLView.ProfileLine;
import edu.asu.jmars.layer.profile.manager.ProfileManagerMode;
import edu.asu.jmars.layer.profile.swing.ButtonColumn;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.layer.profile.swing.ProfileTabelCellObject;

public class SelectedProfilesTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 1L;

	private final String TITLE_COL = "Profiles";
	private final String REMOVE_COL = " ";
	private List<ProfileObject> profileObjList = new ArrayList<>();
	private Map<Integer, Shape> selectedprofiles = new HashMap<>();
	private ProfileFactory controller;
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static Icon trash = new ImageIcon(ImageFactory.createImage(REMOVE.withDisplayColor(imgColor)));
	private JTable jTable;

	private final String[] columnNames = new String[] { TITLE_COL, REMOVE_COL };
	private final Class[] columnClass = new Class[] { ProfileTabelCellObject.class, ButtonColumn.class };

	protected String[] columnToolTips = { "Selected to view in chart", // show/hide column
			"<html><body>"
			+ "<div >Remove profile from chart.</div>"
			+ "<div>Note, this action removes profile from configuration only.</div>"
			+ "<div>This will not delete profile from Main view.</div>"
			+ "<div>You can use Profile Manager if you want to delete profile from Main view.</div></body></html>" 
			};

	public SelectedProfilesTableModel(ProfileFactory control) {
		this.controller = control;
	}

	public void addData(Map<Integer, Shape> selected) {
		profileObjList.clear();	
		this.selectedprofiles.clear();
		this.selectedprofiles.putAll(selected);
		this.selectedprofiles.forEach((id, shape) -> {
			if (shape instanceof ProfileLine) {
				ProfileLine profile = (ProfileLine) shape;
				ProfileObject profileObj = new ProfileObject(profile);
				profileObjList.add(profileObj);
			}
		});
		fireTableDataChanged();
	}

	public Map<Integer, Shape> getModelData() {
		Map<Integer, Shape> modeldata = new HashMap<>();
		for (ProfileObject profileObj : profileObjList) {
			modeldata.put(profileObj.getProfileID(), profileObj.getOrigprofile());
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

		// profile line name or rename
		if (getColumnName(columnIndex).equals(TITLE_COL)) {
			ProfileTabelCellObject cellobj = new ProfileTabelCellObject(row.getProfilename());
			cellobj.setColor(row.getColor());
			return cellobj;
		}
		// remove
		else if (getColumnName(columnIndex).equals(REMOVE_COL)) {
			return trash;
		}
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int colIndex) {
		if (getColumnName(colIndex).equals(REMOVE_COL))
			return true;
		return false;
	}

	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if (profileObjList.isEmpty())
			return;

		ProfileObject plotobj = profileObjList.get(rowIndex);

		if (REMOVE_COL.equals(getColumnName(columnIndex))) {
			Map<Integer, Shape> selected = new HashMap<>();
			if (plotobj != null) {
				int ID = plotobj.getProfileID();				
				this.selectedprofiles.remove(ID);
				this.profileObjList.remove(plotobj);
				fireTableDataChanged();	
				selected.putAll(this.selectedprofiles);
				this.controller.selectedProfiles(selected, ProfileManagerMode.SELECT_MANY);
			}
		}
	}

	public String[] getInitialColumnNames() {
		return columnNames;
	}

	public void withTable(JTable vartbl) {
		this.jTable = vartbl;
	}

	private static class ProfileObject {

		private ProfileLine origprofile;		

		public ProfileObject(ProfileLine profile) {
			this.origprofile = profile;
		}

		public String getProfilename() {
			return getRename() != null ? getRename() : this.origprofile.getIdentifier();
		}

		public ProfileLine getOrigprofile() {
			return origprofile;
		}

		public int getProfileID() {
			return this.origprofile.getID();
		}

		public String getRename() {
			return this.origprofile.getRename();
		}

		public Color getColor() {
			return this.origprofile.getLinecolor();
		}
	}

}
