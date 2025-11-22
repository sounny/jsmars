package edu.asu.jmars.layer.stamp.focus;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import edu.asu.jmars.layer.stamp.StampLayerWrapper;
import edu.asu.jmars.layer.stamp.QueryTemplateUI;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.QueryTemplateUI.FilledDataField;
import edu.asu.jmars.util.Util;

public class QueryFocusPanel extends JPanel {

	public QueryFocusPanel(final StampLayerWrapper wrapper, final StampLayer stampLayer)
	{
		JPanel queryPanel = new JPanel();
		queryPanel.setLayout(new BorderLayout());
		queryPanel.add(wrapper.getContainer(), BorderLayout.CENTER);
	    
	    // Construct the "buttons" section of the container.
	    JPanel buttons = new JPanel();
	    buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	    
	    
	    //add the templates button, if applicable
	    if(wrapper.supportsTemplates()){
	    	JButton addFromTemplate = new JButton("Add from Template...".toUpperCase());
			addFromTemplate.addActionListener(new ActionListener() {
				QueryTemplateUI templateDialog = null;
				
			
				@Override
				public void actionPerformed(ActionEvent e) {
					if (templateDialog == null) {
						templateDialog = new QueryTemplateUI(null, wrapper);
						templateDialog.setLocationRelativeTo(QueryFocusPanel.this);
					}else{
						//When re-using the template dialog, make sure to clear the data fields array,
						// otherwise fields from the previous use will still be in the array and will
						// be added (again) in addition to the user's new selection
						templateDialog.getFilledDataFields().clear();
					}
					templateDialog.setVisible(true);
					
					if (!templateDialog.isCancelled()) {
						for(FilledDataField fdf : templateDialog.getFilledDataFields()){
							wrapper.addFieldToAdvPane(fdf.myDfName, fdf.myMinVal, fdf.myMaxVal);
						}
						//refresh advanced field ui
						wrapper.refreshAdvPane();
					}
					
				}
			});
			buttons.add(addFromTemplate);
	    }
	
	    JButton ok = new JButton("Update Search".toUpperCase());
	    ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (stampLayer.queryThread==null) {
	                // Update layer/view with stamp data from new version of
	                // query using parameters from query panel.
	                String queryStr = wrapper.getQuery();
	                stampLayer.setQuery(queryStr);
				}
			}
		});
	    buttons.add(ok);
	    
	    JButton help = new JButton("Help".toUpperCase());
	    help.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
	    		Util.launchBrowser(stampLayer.getParam(stampLayer.HELP_URL));
			}
	    });
	    
	    buttons.add(help);
	    
	    queryPanel.add(buttons, BorderLayout.SOUTH);
	    
	    setLayout(new BorderLayout());
	    add(queryPanel, BorderLayout.CENTER);
	}
}
