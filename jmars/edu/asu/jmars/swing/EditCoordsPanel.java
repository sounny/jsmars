package edu.asu.jmars.swing;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class EditCoordsPanel extends JPanel {
	private EditPane editPane;
	private List<String> currentLatLon = new ArrayList<>();
	private List<JTextField> newcoords = new ArrayList<>();
	private List<JTextField> oldcoords = new ArrayList<>();
	
	private Color backgroundcolor = new Color(219,219,219);
	private Color textcolor = new Color(57,60,71);

	public EditCoordsPanel(List<String> currentcoords) {
		currentLatLon.clear();
		currentLatLon.addAll(currentcoords);
		editPane = new EditPane(currentLatLon);
	}

	public Object getCoordinatesInputLayout() {
		return editPane;
	}

	public List<String> getNewCoordinates() {
		return editPane.getNewCoordinates();
	}

	class EditPane extends JPanel {
    	 
        public EditPane(List<String> currentcoords) {
            setLayout(new GridBagLayout());
           
			for (int coords = 0; coords < currentcoords.size(); coords++) {
				JTextField fld = new JTextField(20);
				fld.setText(currentcoords.get(coords));
				fld.setEditable(false);
				fld.setEnabled(false);
				fld.setBackground(backgroundcolor);
				fld.setForeground(textcolor);

				oldcoords.add(coords, fld);

				fld = new JTextField(20);
				fld.setText(currentcoords.get(coords)); //preset with current values
				newcoords.add(coords, fld);
			}
            
			 GridBagConstraints gbc = new GridBagConstraints();
	         gbc.insets = new Insets(2, 2, 2, 2);
	         gbc.gridy = 0;
	         int pointindex =0;
	         
	       for (int coords=0; coords < currentcoords.size(); coords++) { 
	    	pointindex = coords+1;
	    	int ystart = gbc.gridy;
	    	if (ystart != 0) {
	    		ystart++;
	    	}
	    	int xstart = 0;
	    	gbc.gridx = xstart;
	    	gbc.gridy = ystart;
	    	gbc.anchor = GridBagConstraints.WEST;
            add(new JLabel("Current Position"), gbc);
            gbc.gridy++;
            gbc.anchor = GridBagConstraints.WEST;
            add(new JLabel("New Position"), gbc);
            gbc.gridx++;
            
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            gbc.gridy = ystart;
            add(oldcoords.get(coords), gbc);
            gbc.gridy++;
            add(newcoords.get(coords), gbc);
            gbc.gridy++;
          }
        }

         public List<String> getNewCoordinates() {
        	List<String> varNewCoords = new ArrayList<>();
        	for (int coords=0; coords < currentLatLon.size(); coords++) {    
        		JTextField oldfld = oldcoords.get(coords);
        		JTextField newfld = newcoords.get(coords);
        		String newtxt = newfld.getText();
        		String oldtxt = oldfld.getText();
        		if (newtxt != null && !newtxt.trim().isEmpty()) {
        			varNewCoords.add(newtxt);
        		} else {
        			varNewCoords.add(oldtxt);
        		}
         	}
        	return varNewCoords;
        }
    }
  }
