package edu.asu.jmars.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileFilter;

import edu.asu.jmars.util.Util;

public class ExportTableAction extends AbstractAction{
	private String delim = "\t";
	private JTable table;
	private JFileChooser fc;
	private Component loc;
	
	public ExportTableAction(Component location, String actionName, String delimiter, JTable tableToExport){
		super(actionName);
		loc = location;
		delim = delimiter;
		table = tableToExport;
		
		//create the appropriate file chooser
		 fc = new JFileChooser(Util.getDefaultFCLocation());
		 FileFilter ff = new FileFilter(){
			 public String getDescription(){
				 if(delim.equals("\t")){
					 return "Tab delilimited file (.tab, .txt)";
				 }
				 else if(delim.equalsIgnoreCase(",")){
					 return "Comma separated value file (.csv, .txt)";
				 }
				 else{
					 return "Text export file (.txt)";
				 }
			 }
			 public boolean accept(File f){
					if (f.isDirectory()) return true;
					if (f.getName().endsWith(".txt")) return true;
					
					if (delim.equals("\t") && f.getName().endsWith(".tab")) return true;
					else if(delim.equals(",") && f.getName().endsWith(".csv")) return true;

					return false;
			 }
		 };
		 fc.setDialogTitle(actionName);
		 fc.setAcceptAllFileFilterUsed(false);
		 fc.setFileFilter(ff);
	}
	
	public void actionPerformed(ActionEvent e){
        File f;
       
        do {
            if (fc.showSaveDialog(loc)
                    != JFileChooser.APPROVE_OPTION)
                return;
            f = fc.getSelectedFile();
            
            //set the proper extension, if not specified by user
            String fileName = f.getName();
            if(delim.equals("\t")){
            	if( !(fileName.endsWith(".tab") || fileName.endsWith(".txt"))){
            		f = new File(f.getAbsolutePath()+".tab");
            	}
            }
            
            else if(delim.equals(",")){
            	if( !(fileName.endsWith(".csv") || fileName.endsWith(".txt"))){
            		f = new File(f.getAbsolutePath()+".csv");
            	}
            }
            
            else{
            	if (!f.getName().endsWith(".txt")) {
            		f=new File(f.getAbsolutePath()+".txt");
            	} 	
            }
        }
        while( f.exists() && 
                JOptionPane.NO_OPTION == Util.showConfirmDialog(
                       "File already exists, overwrite?\n" + f,
                       "FILE EXISTS",
                       JOptionPane.YES_NO_OPTION,
                       JOptionPane.WARNING_MESSAGE
                )
        );
        
    	boolean succeed = true;
        try {
            PrintStream fout = new PrintStream(new FileOutputStream(f));
            
            synchronized(table) {
                int rows = table.getRowCount();
                int cols = table.getColumnCount();
                
                // Output the header line
                for (int j=0; j<cols; j++){
                    fout.print(table.getColumnName(j));
                    
                    if(j<(cols-1)){
                    	fout.print(delim);
                    }else{
                    	fout.println();
                    }
                    
                }
                
                // Output the data
                for (int i=0; i<rows; i++){
                    for (int j=0; j<cols; j++){
                        fout.print(table.getValueAt(i, j));
                        
                        if(j<(cols-1)){
                        	fout.print(delim);
                        }else{
                        	fout.println();
                        }
                    }
                }
                
                fout.close();
            }
        } 
        catch(FileNotFoundException ex){
        	succeed = false;
            Util.showMessageDialog(	                                              
            		"Unable to open file!\n" + f,
                  "File Open Error",
                  JOptionPane.ERROR_MESSAGE
            );
        }
        catch(Exception ex){
        	succeed = false;
        	ex.printStackTrace();
        	Util.showMessageDialog(                                       
            		"Unable to export table. See log for more info.",
                  "Export Failed",
                  JOptionPane.ERROR_MESSAGE
            );
        }
        
        if(succeed){
        	Util.showMessageDialog(                                       
        			"Table export successful!\n" + f,
        			"Export Success",
        			JOptionPane.INFORMATION_MESSAGE
            );
        }
    }
}
