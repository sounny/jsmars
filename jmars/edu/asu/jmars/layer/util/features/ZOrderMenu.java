package edu.asu.jmars.layer.util.features;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import edu.asu.jmars.util.stable.Sorter;


/**
 * Control the Z-order of the selected feature.
 */
public class ZOrderMenu extends JMenu implements ActionListener {
    private final MultiFeatureCollection fc;
    private final Set<Feature> selections;
    private final Sorter sorter;
    
	JMenuItem bottomMenuItem = new JMenuItem("Send to Bottom");
	JMenuItem lowerMenuItem = new JMenuItem("Lower");
	JMenuItem raiseMenuItem = new JMenuItem("Raise");
	JMenuItem topMenuItem = new JMenuItem("Bring to Top");
	JMenuItem tableOrder = new JMenuItem("Use Table Order for All Features");
	
	public ZOrderMenu(String title, MultiFeatureCollection fc, Set<Feature> selections, Sorter sorter) {
		super(title);
		this.fc = fc;
		this.selections = selections;
		this.sorter = sorter;
		
		add(bottomMenuItem);
		bottomMenuItem.addActionListener(this);
		
		add(lowerMenuItem);
		lowerMenuItem.addActionListener(this);
		
		add(raiseMenuItem);
		raiseMenuItem.addActionListener(this);
		
		add(topMenuItem);
		topMenuItem.addActionListener(this);
		
		add(tableOrder);
		tableOrder.addActionListener(this);
	}

	public void actionPerformed(ActionEvent e) {
		if (selections.size() == 0) {
			return;
		}
		
		if (e.getSource() == tableOrder) {
			fc.reorder(sorter.getUnsortArray());
			return;
		}
		
		Map<Feature,Integer> feat2idx = FeatureUtil.getFeatureIndices(fc.getFeatures(), selections);
		int rows[] = new int[selections.size()];
		Iterator<Feature> selIt = selections.iterator();
		for (int i = 0; i < rows.length; i++)
			rows[i] = ((Integer)feat2idx.get(selIt.next())).intValue();
		Arrays.sort(rows);
		
		if (e.getSource() == bottomMenuItem || e.getSource() == topMenuItem) {
			fc.move(rows, e.getSource() != topMenuItem);
		} else if (e.getSource() == lowerMenuItem || e.getSource() == raiseMenuItem) {
			fc.move(rows, e.getSource() != raiseMenuItem ? -1 : 1);
		}
	}
}
