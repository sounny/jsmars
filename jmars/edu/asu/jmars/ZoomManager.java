package edu.asu.jmars;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jdesktop.swingx.combobox.ListComboBoxModel;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.DebugLog;

public final class ZoomManager extends JPanel {
	private static DebugLog log = DebugLog.instance();
	
	private final List<Integer> zoomFactors;
	private JLabel zoomLabel = null;                       // zoom-selector's label
	private JComboBox zoomSelector = null;                 // zoom factor selector
	private List<ZoomListener> listeners = new ArrayList<ZoomListener>();
	private int ppd;
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static Icon tooltipIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.INFO.
			                              withDisplayColor(imgColor)));
	private JLabel layerTooltip = new JLabel(tooltipIcon);
    
	
	public ZoomManager(int defaultZoomLog2, int maxZoomLog2) {
		List<Integer> zoomFactors = new ArrayList<Integer>();
		for (int i = 0; i < maxZoomLog2; i++) {
			zoomFactors.add(1<<i);
		}
		this.zoomFactors = Collections.unmodifiableList(zoomFactors);
		
		/* create the hbox containing the zoom-selector and its label */
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		/* create the zoom-selector label */
		zoomLabel = new JLabel("Zoom");		
		add(zoomLabel);
		
		Dimension gap = new Dimension(5, 0);
		add(Box.createRigidArea(gap));
		
		layerTooltip.setOpaque(false);
		layerTooltip.setToolTipText(getFormattedText());
		add(layerTooltip);
		add(Box.createRigidArea(gap));
	
		/* create the zoom-selection combo-box */
		zoomSelector = new JComboBox(new ListComboBoxModel<Integer>(zoomFactors));
		zoomSelector.setMaximumRowCount(zoomFactors.size());
		zoomSelector.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (zoomSelector.getSelectedItem() != null) {
					setZoomPPD((Integer)zoomSelector.getSelectedItem(), true);
				}
			}
		});
		add(zoomSelector);				
		
		setZoomPPD(1<<defaultZoomLog2, false);
	}
	
	private String getFormattedText() {
		String infohtml="<html>";
		infohtml += "<p style=\"border:2px solid #3b3e45; padding:5px;  margin:-4;\">&nbsp;";
		infohtml += "Select the desired zoom-factor from dropdown list.";
		infohtml +=  "&nbsp;<br>&nbsp;";
		infohtml +=	 "Zoom levels are defined in PPD (pixels-per-degree). ";
		infohtml +=  "&nbsp;</p></html>";
		return infohtml;
	}

	public void addListener(ZoomListener l) {
		listeners.add(l);
	}
	
	public boolean removeListener(ZoomListener l) {
		return listeners.remove(l);
	}
	
	public List<Integer> getZoomFactors() {
		return zoomFactors;
	}
	
	public int getZoomPPD() {
		return ppd;
	}
	
	public void setZoomPPD(int ppd, boolean propogate)
	{
		int index = zoomFactors.indexOf(ppd);
		if(index == -1)
		{
			log.aprintln("BAD ZOOM FACTOR RECEIVED: " + ppd);
			return;
		}
		if (this.ppd != ppd) {
			this.ppd = ppd;
			zoomSelector.setSelectedIndex(index);
			if (propogate) {
				notifyListeners();
			}
		}
	}
	
	public Integer[] getExportZoomFactors() {
		int currentIndex = zoomFactors.indexOf(ppd);
		
		int numExportOptions = Math.min(3, zoomFactors.size() - currentIndex - 1);
		
		Integer zoomOptions[] = new Integer[numExportOptions];
		
    	for (int i=0; i<zoomOptions.length; i++) {
    		zoomOptions[i]=((int)(ppd*(Math.pow(2,i+1))));
    	}
    	
    	return zoomOptions;
	}
	
	public Integer[] getExportZoomFactorsWithCurrent() {
		int currentIndex = zoomFactors.indexOf(ppd);
		
		int numExportOptions = Math.min(4, zoomFactors.size() - currentIndex - 1);
		
		Integer zoomOptions[] = new Integer[numExportOptions];
		
    	for (int i=0; i<zoomOptions.length; i++) {
    		zoomOptions[i]=((int)(ppd*(Math.pow(2,i))));
    	}
    	
    	return zoomOptions;
	}
	private void notifyListeners() {
		for (ZoomListener l: listeners) {
			l.zoomChanged(ppd);
		}
	}
}
