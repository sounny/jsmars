package edu.asu.jmars.layer.stamp.focus;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import edu.asu.jmars.layer.stamp.StampLayerWrapper;
import edu.asu.jmars.layer.stamp.StampFilter;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.util.Util;

public class FilterFocusPanel extends JPanel {
	
	StampLayer stampLayer;
	final StampLayerWrapper wrapper;
	
	public FilterFocusPanel(final StampLView stampLView, StampLayerWrapper newWrapper) {
		stampLayer = stampLView.stampLayer;
		wrapper = newWrapper;
		
		final Box vert = Box.createVerticalBox();
	
		JLabel srcLbl = new JLabel(stampLView.getName());
		JPanel srcLblPanel = new JPanel(new BorderLayout());
		srcLblPanel.add(srcLbl, BorderLayout.NORTH);
		
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.setBorder(BorderFactory.createTitledBorder("Source"));
		
		JButton insertButton = new JButton("+");
		insertButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				StampFilter array[]=wrapper.getFilters().toArray(new StampFilter[wrapper.getFilters().size()]);
				StampFilter selected = (StampFilter)Util.showInputDialog("Select Filter", "Select Filter",
						JOptionPane.QUESTION_MESSAGE, null, array, array.length > 0? array[0]: null);
				
				if (selected != null){
					vert.add(selected.getUI(stampLayer));
					stampLView.focusPanel.validate();
				}
			}
		});
		
		JPanel buttonInnerPanel = new JPanel(new GridLayout(1,1));
		buttonInnerPanel.add(insertButton);
		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.add(buttonInnerPanel, BorderLayout.NORTH);
	
		
		topPanel.add(srcLblPanel, BorderLayout.CENTER);
		topPanel.add(buttonPanel, BorderLayout.EAST);
		
		vert.add(topPanel);
		
		setLayout(new BorderLayout());
		
		JPanel filterPanel = new JPanel();
		filterPanel.setLayout(new BorderLayout());
		filterPanel.add(vert, BorderLayout.NORTH);
		
		JScrollPane scrollPane = new JScrollPane(filterPanel);
		
		add(scrollPane, BorderLayout.CENTER);
	}
}
