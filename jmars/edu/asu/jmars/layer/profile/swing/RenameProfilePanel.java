package edu.asu.jmars.layer.profile.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class RenameProfilePanel extends JPanel {
	 RenamePane renamePane;
	 String renameFrom;
	
	  
    public RenameProfilePanel (String oldname) {
    	renameFrom = oldname;
    	renamePane = new RenamePane(oldname);
    }
    
	public Object getMessage() {
		return renamePane;
    }
	
	public String getNewName() {
		return renamePane.getNewName();
	}
    
    class RenamePane extends JPanel {
        private JTextField oldname;
        private JTextField newname;

        public RenamePane(String renamefrom) {
            setLayout(new GridBagLayout());
            oldname = new JTextField(20);
            oldname.setText(renamefrom);
            oldname.setEditable(false);
            oldname.setEnabled(false);
            newname = new JTextField(20);

            GridBagConstraints gbc = new GridBagConstraints();

            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.gridx = 0;
            gbc.gridy = 0;
            add(new JLabel("Current name"), gbc);
            gbc.gridy++;
            add(new JLabel("New name"), gbc);

            gbc.gridx++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridy = 0;
            add(oldname, gbc);
            gbc.gridy++;
            add(newname, gbc);
        }

        public String getOldName() {
            return oldname.getText();
        }

        public String getNewName() {
            return newname.getText();
        }
    }
  }

    