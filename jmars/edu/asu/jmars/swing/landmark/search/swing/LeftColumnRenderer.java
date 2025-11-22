package edu.asu.jmars.swing.landmark.search.swing;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.EDIT;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CLEAR;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import org.apache.commons.lang3.StringUtils;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView.MarsFeature;
import edu.asu.jmars.places.Place;
import edu.asu.jmars.swing.landmark.search.BookmarksDataAccess;
import edu.asu.jmars.swing.landmark.search.LandmarkSearchPanel;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;


public class LeftColumnRenderer implements TableCellRenderer {

	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static Icon edit = new ImageIcon(ImageFactory.createImage(EDIT.withDisplayColor(imgColor)));
	private static Icon delete = new ImageIcon(ImageFactory.createImage(CLEAR.withDisplayColor(imgColor)));
	private static String EDIT_TIP = "Click here to edit bookmark";
	private static String DELETE_TIP = "Click here to delete bookmark";
	private JLabel editLbl, deleteLbl, bookmarkLbl;
	private Map<Integer, MouseEventReposter> repostermap = new LinkedHashMap<>();
	private Map<MouseEvent, MouseEventReposter> eventManager = new HashMap<>();
	private MouseAdapter editbookmarkmouseadapter = null;
	private MouseAdapter deletebookmarkmouseadapter = null;
	private MouseAdapter bookmarklabelmouseadapter = null;
	private MouseAdapter contentpanelmouseadapter = null;
	private MouseEventReposter reposter = null;
	private final JTable jTable;

	public LeftColumnRenderer(LandmarkSearchTable landmarkSearchTable) {
		jTable = landmarkSearchTable;
		editbookmarkmouseadapter = new OnEditBookmarkClickAction();
		deletebookmarkmouseadapter = new OnDeleteBookmarkClickAction();
		bookmarklabelmouseadapter = new OnBookmarkLabelClickAction();
		contentpanelmouseadapter = new OnContentPanelClickAction();
		repostermap.clear();
		eventManager.clear();
	}
	
	private JPanel createContentPanel(String contentValue) {
		
		JPanel contentPanel = new JPanel();
		contentPanel.setOpaque(true);
		contentPanel.setBorder(BorderFactory.createEmptyBorder(1, 5, 5, 5));
		
		bookmarkLbl = new JLabel();
		bookmarkLbl.setText(contentValue);
		
		editLbl = new JLabel(edit);
		deleteLbl = new JLabel(delete);
		
		editLbl.setToolTipText(EDIT_TIP);
		deleteLbl.setToolTipText(DELETE_TIP);
	
		editLbl.addMouseListener(editbookmarkmouseadapter);
		deleteLbl.addMouseListener(deletebookmarkmouseadapter);
		bookmarkLbl.addMouseListener(bookmarklabelmouseadapter);
		contentPanel.addMouseListener(contentpanelmouseadapter);

		contentPanel.setLayout(new FlowLayout(FlowLayout.LEADING, 2,2));
		contentPanel.add(bookmarkLbl);
		contentPanel.add(editLbl);
		contentPanel.add(deleteLbl);
		
		return contentPanel;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		
		if (table != jTable) return null;
		
		String bookmarkValue = value.toString();
		
		JPanel contentPanel = createContentPanel(bookmarkValue);

		if (isSelected) {
			contentPanel.setBackground(table.getSelectionBackground());
		}
		else if (row % 2 == 0) {  //zebra
			contentPanel.setBackground(table.getBackground());  			
		} else {
			contentPanel.setBackground(ThemeProvider.getInstance().getRow().getAlternateback()); 
		}	
		
		this.reposter = new MouseEventReposter(jTable, column, row, contentPanel, bookmarkValue);
		
		if (repostermap.get(row) == null) {
			jTable.addMouseListener(this.reposter);
		} else { // if exist, remove "old" listener and add "new" listener
			MouseEventReposter mer = repostermap.get(row);
			jTable.removeMouseListener(mer);
			jTable.addMouseListener(this.reposter);
		}
		repostermap.put(row, this.reposter);
		
		return contentPanel;
	}
	
	private class OnBookmarkLabelClickAction extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent ev) {
			String bookmark = getBookmarkForEvent(ev);
			if (!bookmark.equals(StringUtils.EMPTY)) {
				LandmarkSearchPanel.GOTO(bookmark);
			}
		}
	}	
	
	private class OnContentPanelClickAction extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent ev) {
			String bookmark = getBookmarkForEvent(ev);
			if (!bookmark.equals(StringUtils.EMPTY)) {
				LandmarkSearchPanel.GOTO(bookmark);
			}
		}
	}	
	
	private class OnEditBookmarkClickAction extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent ev) {
			Place place = getPlaceForEvent(ev);
			if (place != null) {
				Main.places.editBookmark(place);
			}
		}
	}	
	
	private class OnDeleteBookmarkClickAction extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent ev) {
			Place place = getPlaceForEvent(ev);
			if (place != null) {
				Main.places.delete(place);
			}
		}
	}
	
	public void addListeners(LandmarkSearchTable landmarkSearchTable) {
		if (jTable != landmarkSearchTable) return;
		for (Map.Entry<Integer, MouseEventReposter> entry : this.repostermap.entrySet()) {
			MouseEventReposter mer = (MouseEventReposter) entry.getValue();
			jTable.addMouseListener(mer);
		}
	}
	
	private Place getPlaceForEvent(MouseEvent ev) {
		Place place = null;
		MouseEventReposter mer = eventManager.get(ev);
		if (mer == null) return place;
		String bookmarkThatWasClicked = StringUtils.EMPTY; 
 		if (jTable == null) return place;
		LandmarkSearchTableModel model = (LandmarkSearchTableModel) jTable.getModel();
		if (model == null) return place;
		bookmarkThatWasClicked = mer.getContentValue(); //this will be an html string from jTable <name and Label>
		MarsFeature mf = model.getLandmarkDataByName(bookmarkThatWasClicked);
		if (mf != null) {
			place = BookmarksDataAccess.of(mf);
		}
		return place;
	}
	
	private String getBookmarkForEvent(MouseEvent ev) {
		String bookmark = StringUtils.EMPTY;
		MouseEventReposter mer = eventManager.get(ev);
		if (mer == null) return bookmark;
		String bookmarkThatWasClicked = mer.getContentValue(); 
		if (jTable == null) return bookmark;
		LandmarkSearchTableModel model = (LandmarkSearchTableModel) jTable.getModel();
		if (model == null) return bookmark;
		MarsFeature mf = model.getLandmarkDataByName(bookmarkThatWasClicked);
		if (mf != null) {
			bookmark = mf.name;
		}
		return bookmark;
	}
	
	class MouseEventReposter extends MouseAdapter {
		private Component dispatchComponent;
		private JTable jtable;
		private int column = -1;
		private int row = -1;
		private String contentValue;
		private Component editor;


		public MouseEventReposter(JTable table, int column, int row, Component panel, String content) {	
			this.jtable = table;
			this.column = column;
			this.row = row;
			this.editor = panel;
			this.contentValue = content;
		}

		public String getContentValue() {
			return this.contentValue;
		}
		
		public void setEditor(Component editor) {
			this.editor = editor;
		}
		
	
		private void setDispatchComponent(MouseEvent e) {
			int col = jtable.columnAtPoint(e.getPoint());
			int row2 = jtable.rowAtPoint(e.getPoint());
			if (row2 != row || row == -1) return;
			if (col == -1) return;
			if (col == column) {
				Point p = e.getPoint();
				Point p2 = SwingUtilities.convertPoint(e.getComponent(), p, editor);
				prepForRedispatch(col);
				dispatchComponent = SwingUtilities.getDeepestComponentAt(editor, p2.x, p2.y);
			} else { //if clicked anywhere else, go to bookmark
				dispatchComponent = bookmarkLbl;
			}
		}

		private void prepForRedispatch(int columnIndex) {
		}

		private boolean repostEvent(MouseEvent e) {
			if (dispatchComponent == null) {
				return false;
			}
			MouseEvent e2 = SwingUtilities.convertMouseEvent(jtable, e, dispatchComponent);	
			eventManager.put(e2, this);
			dispatchComponent.dispatchEvent(e2);
			return true;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
				Point p = e.getPoint();
				int col = jtable.columnAtPoint(p);
				int row2 = jtable.rowAtPoint(p);
				if (row2 != row || row == -1) return;
				if (col == -1) return;
				if (col != column) { //if clicked anywhere else, go to bookmark
					col = column;
				}
				int index = jtable.getColumnModel().getColumnIndexAtX(p.x);
				if (index == -1) return;
				editor.setBounds(jtable.getCellRect(row2, col, true));
				jtable.add(editor);
				editor.validate();
				setDispatchComponent(e);
				repostEvent(e);
				dispatchComponent = null;
				jtable.remove(editor);
		}
	}

}
