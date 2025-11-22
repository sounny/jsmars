package edu.asu.jmars.places;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import org.material.component.swingsnackbar.SnackBar;
import org.material.component.swingsnackbar.action.AbstractSnackBarAction;
import edu.asu.jmars.Main;
import edu.asu.jmars.parsers.gis.CoordinatesParser.Ordering;
import edu.asu.jmars.swing.snackbar.SnackBarBuilder;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeSnackBar;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;
import mdlaf.utils.MaterialImageFactory;
import mdlaf.utils.icons.MaterialIconFont;

/**
 * Creates a dialog for editing places. Can be reused by using
 * {@link #setPlace(Place)} to update the place to edit.
 * 
 * The save button will use the {@link PlaceStore} given to the constructor to
 * save the place. This will create a new place or replace an existing one.
 * 
 * The default close operation is not replaced so code using this class should
 * take care to set it to an expected value.
 */
public class PlaceEditDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	/** The place being edited */
	private final PlaceStore store;
	private Place place;
	private JTextField nameText = new JTextField(20);
	private JTextField labelText = new JTextField(20);
	private JLabel placeText = new JLabel();
	private JButton save = new JButton("Save".toUpperCase());
	private SnackBar snackBar = null;
	private static final UUID uuid = UUID.randomUUID();
	private Icon closeicon = MaterialImageFactory.getInstance().getImage(MaterialIconFont.CLOSE,UIManager.getColor("SnackBar.foreground"));
	
	/** Constructs a new place panel */
	public PlaceEditDialog(Frame owner, PlaceStore store) {
		super(owner, "Edit Bookmark", true);
		this.setLocationRelativeTo(owner);
		this.setAlwaysOnTop(true);
		
		this.store = store;
		
		save.setMnemonic('S');
		save.setEnabled(false);
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					save();
				} catch (Exception ex) {
					ex.printStackTrace();
					Util.showMessageDialog(
						"Error saving bookmark for Place:\n\n" + Util.join("\n", getMessages(ex)),
						"Error saving bookmark for Place",
						JOptionPane.ERROR_MESSAGE);
				}
			}
		});
			
		JPanel panel = new JPanel(new GridBagLayout());
		nameText.setToolTipText("Name to appear in the menu, must be unique");
		labelText.setToolTipText("Comma-separated list of menu labels the Place should appear in");
		
		Insets i = new Insets(4,4,4,4);
		int padx = 0;
		int pady = 0;
		panel.add(new JLabel("Name"), new GridBagConstraints(0,0,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,i,padx,pady));
		panel.add(nameText, new GridBagConstraints(1,0,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,i,padx,pady));
		panel.add(new JLabel("Labels"), new GridBagConstraints(0,1,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.NONE,i,padx,pady));
		panel.add(labelText, new GridBagConstraints(1,1,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,i,padx,pady));
		panel.add(new JLabel("Place"), new GridBagConstraints(0,2,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.NONE,i,padx,pady));
		panel.add(placeText, new GridBagConstraints(1,2,1,1,0,0,GridBagConstraints.WEST,GridBagConstraints.HORIZONTAL,i,padx,pady));
		panel.add(save, new GridBagConstraints(0,3,2,1,1,0,GridBagConstraints.CENTER,GridBagConstraints.NONE,i,padx,pady));
		add(panel);
		pack();
	}
	
	private static List<String> getMessages(Throwable t) {
		List<String> out = new ArrayList<String>();
		for (Throwable s = t; s != null; s = s.getCause()) {
			out.add(s.getMessage());
		}
		return out;
	}
	
	/** Change the place being edited */
	public void setPlace(Place place) {
		this.place = place;
		save.setEnabled(place != null);
		nameText.setText(place == null ? "" : place.getName());
		labelText.setText(place == null ? "" : Util.join(", ", place.getLabels()));
		placeText.setText(place == null ? "" : getCoordOrdering().formatPlace(place));
	}
	
	public void setVisible(boolean vis) {
		nameText.selectAll();
		nameText.requestFocus();
		super.setVisible(vis);
	}
	
	/** Returns the place being edited */
	public Place getPlace() {
		return place;
	}
	
	/** Removes the old place and adds the new one */
	private void save() {
		if (store.contains(place)) {
		    store.remove(place);
		}
		String placename = nameText.getText();
		if ("".equalsIgnoreCase(placename.trim()))
		{
			runSnackBar("Bookmark name cannot be empty", false);
			return;
		}
		place.setName(nameText.getText());
		place.getLabels().clear();
		for (String label: labelText.getText().split(", *")) {
			label = label.trim();
			if (label.length() > 0) {
				place.getLabels().add(label);
			}
		}
		store.add(place);
		setVisible(false);		
	}
	
	private Ordering getCoordOrdering() {
		String coordOrdering = Config.get(Config.CONFIG_LAT_LON,Ordering.LAT_LON.asString());
		Ordering ordering = Ordering.get(coordOrdering);
		return ordering;
	}	
	
	private void runSnackBar(String message, boolean force) {
		int gap = message.length();
		if (snackBar == null) {
			snackBar = SnackBarBuilder.build(Main.mainFrame, message, closeicon, uuid)
					.setSnackBarBackground(ThemeSnackBar.getBackgroundError())
					.setSnackBarForeground(ThemeSnackBar.getForegroundError()).setDuration(SnackBar.LENGTH_SHORT)
					.setPosition(SnackBar.BOTTOM).setMarginBottom(150).setGap(gap)
					.setAction(new AbstractSnackBarAction() {
						@Override
						public void mousePressed(MouseEvent e) {
							SnackBarBuilder.getSnackBarOn(uuid).dismiss();
						}
					});
		}
		if (!snackBar.isRunning() || force) {
			snackBar.refresh().run();
		}
	}
}
