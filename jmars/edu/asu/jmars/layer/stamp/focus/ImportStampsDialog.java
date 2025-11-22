package edu.asu.jmars.layer.stamp.focus;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.swing.MultiLabel;
import edu.asu.jmars.swing.PasteField;
import edu.asu.jmars.util.Util;

public class ImportStampsDialog implements ActionListener
{
    private JDialog dialog;
    private JTextField txtFilename;
    private JButton btnBrowse = new JButton("Browse...".toUpperCase());
    private JButton btnOK = new JButton("OK".toUpperCase());
    private JButton btnCancel = new JButton("Cancel".toUpperCase());
    
    private File lastDirectory;
    
    private FilledStampFocus filledFocus;
    
    ImportStampsDialog(FilledStampFocus newFocus)
    {
    	filledFocus=newFocus;
    	
        // Top panel
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        
        String msg1 = "Specify text file containing list of stamps to import.  The file " +
                      "must contain stamp IDs delimited by whitespace (includes newlines).";
        String msg2 = "Each stamp will be loaded and rendered if it is included in the " +
                      "list of stamps for this layer; otherwise, the stamp is ignored.";
        msg1 = Util.lineWrap(msg1, 60);
        msg2 = Util.lineWrap(msg2, 60);
        JPanel textPanel1 = new JPanel();
        JPanel textPanel2 = new JPanel();
        textPanel1.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        textPanel2.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        textPanel1.add(new MultiLabel(msg1));
        textPanel2.add(new MultiLabel(msg2));
        
        top.add(textPanel1);
        top.add(textPanel2);
        
        // Middle panel
        JPanel middle = new JPanel();
        middle.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        middle.add(new JLabel("Filename:"));
        txtFilename = new PasteField(20);
        middle.add(txtFilename);
        
        // File chooser dialog launch button.
        btnBrowse.addActionListener(
            new ActionListener()
            {
                private JFileChooser fc = new JFileChooser(Util.getDefaultFCLocation());
                    
                public void actionPerformed(ActionEvent e)
                {
                    // Show the file chooser
                    if(fc.showOpenDialog(dialog) != JFileChooser.APPROVE_OPTION)
                        return;
                    
                    txtFilename.setText(fc.getSelectedFile().getPath());
                    lastDirectory = fc.getCurrentDirectory();
                }
            }
        );
        middle.add(btnBrowse);
        
        // Bottom panel
        JPanel bottom = new JPanel();
        btnOK.addActionListener(this);
        bottom.add(btnOK);
        btnCancel.addActionListener(this);
        bottom.add(btnCancel);
        
        // Construct the dialog itself
        dialog = new JDialog(LManager.getDisplayFrame(),
                             "Import Stamps",
                             true);
        dialog.getContentPane().add(top, BorderLayout.NORTH);
        dialog.getContentPane().add(middle, BorderLayout.CENTER);
        dialog.getContentPane().add(bottom, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(Util.getDisplayFrame(filledFocus));
    }
    
    // Does not return until dialog is hidden or
    // disposed of.
    public void show()
    {
        dialog.setVisible(true);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == btnCancel) {
            dialog.dispose();
            return;
        }
        else if (e.getSource() == btnOK) {
            String filename = txtFilename.getText().trim();
            if (filename == null ||
                    filename.equals("")) {
                Util.showMessageDialog("Please provide name of file.",
                      null,
                      JOptionPane.PLAIN_MESSAGE);
                return;
            }
            
            File file = new File(filename);
            if (!file.exists()) {
                Util.showMessageDialog("File named " + filename + " does not exist.",
                      null,
                      JOptionPane.PLAIN_MESSAGE);
                return;
            }
            
            filledFocus.addStamps(file);
            
            dialog.dispose();
            return;
        }
    }
}
