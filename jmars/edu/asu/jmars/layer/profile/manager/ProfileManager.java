package edu.asu.jmars.layer.profile.manager;

import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Shape;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import edu.asu.jmars.layer.profile.ProfileLView;
import edu.asu.jmars.layer.profile.config.Config;


public class ProfileManager extends JDialog {

		private ProfileManagerTable profileManagerTbl;
		private JScrollPane tableSP;
		private JPanel tablePnl;
		private int pad = 1;
		private Insets in = new Insets(pad,pad,pad,pad);
		private static ProfileManagerTableModel profileManagerTabelModel;
		private ProfileManagerMode mode;
		private static ProfileLView profileLView;
		
		
		public static void createCommonSettings(ProfileLView profileview) {
			profileLView = profileview;
			profileManagerTabelModel = new ProfileManagerTableModel(profileLView);
			profileManagerTabelModel.addTableModelListener(profileLView);				
		}		
		
		public ProfileManager(ProfileManagerMode mode){
			super(new Frame(), "Select Profiles to Chart", false);
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			//set the location relative to but centered on the component passed in				
			this.mode = mode;
			buildUI();
			setMinimumSize(new Dimension(700,400));
			pack();
		}
		
		public void updateRelativeToLocationOnScreen(JComponent comp) {
			if (comp != null) {
				Point pt = comp.getLocationOnScreen();
				setLocation(pt.x+80, pt.y+10);					
			}
		}
 			
		private void buildUI(){
			//table section					
			this.profileManagerTbl = new ProfileManagerTable(profileManagerTabelModel);	
			this.profileManagerTbl.updateColumnsVisibility(this.mode);
			if (this.mode == ProfileManagerMode.MANAGE) {
			    profileManagerTabelModel.withTable(this.profileManagerTbl); //this is only needed for "Delete" prompt
			}
			tableSP = new JScrollPane(profileManagerTbl, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			tablePnl = new JPanel();
			tablePnl.setLayout(new GridLayout(1, 1));
			tablePnl.setPreferredSize(new Dimension(700, 400));
			tablePnl.add(tableSP);
			JPanel mainPnl = new JPanel(new BorderLayout());		
			mainPnl.setBorder(new EmptyBorder(5, 5, 5, 5));
			mainPnl.add(tablePnl, BorderLayout.CENTER);											
			setContentPane(mainPnl);
		}
				
		public ProfileManagerTable getProfileManagerTbl() {
			return profileManagerTbl;
		}
	
		public void manage() {
			Map<Integer, Shape> allprofiles = profileLView.getAllExistingProfilelines();
			profileManagerTabelModel.removeTableModelListener(profileLView);  //don't need tableChanged() when refreshing Profile Manager table
			profileManagerTabelModel.addData(allprofiles, profileLView);  
			profileManagerTabelModel.addTableModelListener(profileLView);
		}
	
		public Map<Integer, Shape> getAllProfiles() {
			Map<Integer, Shape> allprofiles = new LinkedHashMap<>();
			allprofiles = profileLView.getAllExistingProfilelines();
			return allprofiles;			
		}

		public void manage(Config chartConfigOneSource) {
			profileManagerTabelModel.withConfig(chartConfigOneSource);
			manage();			
		}	
}
	

