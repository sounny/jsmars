package edu.asu.jmars.pref;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.swing.MultiLabel;
import edu.asu.jmars.swing.PasteField;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;

public class DefaultBrowser implements ActionListener
{
    private JDialog dialog;
    private JTextField txtCommand;
    private JButton btnBrowse = new JButton("Browse...".toUpperCase());
    private JButton btnOK = new JButton("OK".toUpperCase());
    private JButton btnClear = new JButton("Clear".toUpperCase());
    private JButton btnCancel = new JButton("Cancel".toUpperCase());
    private static String OLD_BROWSER_KEY = "stamp_browser";
    private static String BROWSER_KEY = "custom_browser";
	private static final String URL_TAG = "%URL%";
    private static String browser = "";
    
    
    //Determine the proper browser
    static{
    	String b = Config.get(BROWSER_KEY, null);
    	if(b!=null && b.length()>0){
    		browser = b;
    	}else{
    		b = Config.get(OLD_BROWSER_KEY, null);
    		if(b!=null && b.length()>0){
    			browser = b;
    			Config.set(OLD_BROWSER_KEY, "");
    			Config.set(BROWSER_KEY, browser);
    		}
    	}
    }
    
    public DefaultBrowser()
    {
        
        // Construct dialog contents
        JPanel top = new JPanel();
        String msg = "Please provide command to start preferred webbrowser program.  " +
                     "Include program name with valid directory path (if needed) and " +
                     "any necessary command line options." +
                     "\n  \nFor the command argument " + 
                     "which specifies the webpage to open, use " + URL_TAG + 
                     " as the placeholder." +
                     "\n  \nExample:  mywebbrowser " + URL_TAG;
        msg = Util.lineWrap(msg, 80);
        MultiLabel txtBox = new MultiLabel(msg);
        top.add(txtBox);
        
        JPanel middle = new JPanel();
        middle.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        middle.add(new JLabel("Command:"));
        txtCommand = new PasteField(browser, 35);
        middle.add(txtCommand);
        
        // Browser program chooser dialog launch button.
        btnBrowse.addActionListener(
    		new ActionListener(){
    			public void actionPerformed(ActionEvent e){
    				// Show the file chooser
    				JFileChooser fc = getFileChooser();
    				if (fc.showOpenDialog(dialog) != JFileChooser.APPROVE_OPTION)
    					return;
                                        
    				txtCommand.setText(fc.getSelectedFile().getPath() + " " + URL_TAG);
    			}
    		}
        );
        middle.add(btnBrowse);
        
        JPanel bottom = new JPanel();
        btnOK.addActionListener(this);
        bottom.add(btnOK);
        btnClear.addActionListener(this);
        bottom.add(btnClear);
        btnCancel.addActionListener(this);
        bottom.add(btnCancel);
        
        // Construct the dialog itself
        dialog = new JDialog(LManager.getDisplayFrame(),
                             "Webbrowser Preference",
                             true);
        dialog.getContentPane().setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
        dialog.getContentPane().add(top);
        dialog.getContentPane().add(middle);
        dialog.getContentPane().add(bottom);
        dialog.pack();
        dialog.setLocation(LManager.getDisplayFrame().getLocation());
    }
    
    private JFileChooser fileChooser;
    protected final JFileChooser getFileChooser()
    {
        if (fileChooser == null)
            fileChooser = new JFileChooser(Util.getDefaultFCLocation());
        
        return fileChooser;
    }
    	    
    // Does not return until dialog is hidden or disposed of.
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
        else if(e.getSource() == btnClear) {
            txtCommand.setText("");
            return;
        }
        else if (e.getSource() == btnOK) {
            String cmd = txtCommand.getText().trim();
            
            if (cmd == null ||
                cmd.equals("") ||
                cmd.toLowerCase().equals(URL_TAG.toLowerCase())) 
            {
                // Clear custom browser command; will use default.
                Config.set(BROWSER_KEY, "");
                browser = "";
                dialog.dispose();
                return;
            }

            // Verify basic command syntax requirements: The command must have
            // a program/command name as the first argument, and the URL_TAG placeholder
            // must appear somewhere in the command string after it.
            int urlIndex = cmd.toLowerCase().indexOf(URL_TAG.toLowerCase());
            StringTokenizer tokenizer = new StringTokenizer(cmd);
            
            if (tokenizer.countTokens() >= 2 &&
                !tokenizer.nextToken().equalsIgnoreCase(URL_TAG) &&
                urlIndex >= 0)
            {
                // Replace just the url placeholder with the proper case form.
                if (urlIndex >= 0) {
                    cmd = cmd.substring(0, urlIndex) + URL_TAG + cmd.substring(urlIndex + URL_TAG.length());
                    Config.set(BROWSER_KEY, cmd);
                    browser = cmd;
                }
            }
            else {
                String msg = "Command should have syntax similar to the following: \"mywebbrowser " + 
                             URL_TAG + "\", where " + URL_TAG + " is the placeholder for a webpage.  " +
                             "Other command arguments and any command/argument order is permitted, so " + 
                             "long as the webpage placeholder appears.";
                msg = Util.lineWrap(msg, 55);
                
                Util.showMessageDialog(msg,
                                              "Browser Command Problem",
                                              JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            
            dialog.dispose();
            return;
        }
    }
    
    //return custom or default browser
    public static String getBrowser(){  	
    	return browser;
    }
    
    //return the url tag
    public static String getURLTag(){
    	return URL_TAG;
    }
}


