package edu.asu.jmars.layer.stamp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Locale;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.accessibility.AccessibleText;
import javax.accessibility.AccessibleValue;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;

import cookxml.cookswing.util.SpringLayoutUtilities;
import edu.asu.jmars.util.Util;

public class ProgressDialog
{
    private JDialog         dialog;
    private JOptionPane     pane;
    private JProgressBar    myBar;
    private JLabel          noteLabel;
    private Component       parentComponent;
    private Object[]        cancelOption = null;
    private JTextArea       status;
    private int             min;
    private int             max;
    private int             lastDisp;
    private int             reportDelta;

    private StampLayer 		myLayer;
    /**
     * Constructs a graphic object that shows progress, typically by filling
     * in a rectangular bar as the process nears completion.
     *
     * @param parentComponent the parent component for the dialog box
     */
    public ProgressDialog(Component parentComponent, StampLayer stampLayer) {
        this.parentComponent = parentComponent;

        myLayer = stampLayer;
        cancelOption = new Object[1];
        cancelOption[0] = UIManager.getString("OptionPane.cancelButtonText");
        
        status = new JTextArea("Starting up");
        status.setRows(5);
        status.setColumns(20);
        status.setEditable(false);       
        min=0;
        max=100;
        setProgress(0);
    }

    /**
     * Constructor for use when this isn't part of a StampLayer query
     * @param parentComponent
     */
    public ProgressDialog(Component parentComponent) {
        this.parentComponent = parentComponent;

        cancelOption = new Object[1];
        cancelOption[0] = UIManager.getString("OptionPane.cancelButtonText");
        
        status = new JTextArea("Progress intializing");
        status.setRows(5);
        status.setColumns(20);
        status.setEditable(false);       
        min=0;
        max=100;
        setProgress(0);
        dialog.setAlwaysOnTop(true);
    }
    
	public void updateStatus(String newStatus) {
		status.append("\n"+newStatus);
	}

	public void startDownload(int startValue, int endValue) {
		min = startValue;
		max = endValue;
		
        reportDelta = (max - min) / 100;
        if (reportDelta < 1) reportDelta = 1;

		setMax(endValue);
		setProgress(startValue);
	}
	
	public void downloadStatus(int newValue) {
		setProgress(newValue);
	}
	
    private class ProgressOptionPane extends JOptionPane
    {
        ProgressOptionPane(Object messageList) {
            super(messageList,
                  JOptionPane.INFORMATION_MESSAGE,
                  JOptionPane.DEFAULT_OPTION,
                  null,
                  cancelOption,
                  null);
        }


        // Equivalent to JOptionPane.createDialog,
        // but create a modeless dialog.
        // This is necessary because the Solaris implementation doesn't
        // support Dialog.setModal yet.
        public JDialog createDialog(Component parentComponent, String title) {
            final JDialog dialog;
	    
		dialog = new JDialog((Frame)null, title, false);
            Container contentPane = dialog.getContentPane();

            contentPane.setLayout(new BorderLayout());
            contentPane.add(this, BorderLayout.CENTER);
            dialog.pack();
            dialog.setLocationRelativeTo(parentComponent);
            dialog.addWindowListener(new WindowAdapter() {
                boolean gotFocus = false;

                public void windowClosing(WindowEvent we) {
                    setValue(cancelOption[0]);
                }

                public void windowActivated(WindowEvent we) {
                    // Once window gets focus, set initial focus
                    if (!gotFocus) {
                        selectInitialValue();
                        gotFocus = true;
                    }
                }
            });

            addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent event) {
                    if(dialog.isVisible() && 
                       event.getSource() == ProgressOptionPane.this &&
                       (event.getPropertyName().equals(VALUE_PROPERTY) ||
                        event.getPropertyName().equals(INPUT_VALUE_PROPERTY))){
                    		if (myLayer!=null) {
                    			myLayer.queryThread.stop();
                    			myLayer.queryThread=null;
                    		}
                            dialog.setVisible(false);
                            dialog.dispose();                    		
                    }
                }
            });

            return dialog;
        }
        
    }

    public void setMax(int newMax) {
    	max = newMax;
    	if (myBar!=null) {
    		myBar.setMaximum(max);
    	}
    }

    /** 
     * Indicate the progress of the operation being monitored.
     * If the specified value is >= the maximum, the progress
     * monitor is closed. 
     * @param nv an int specifying the current value, between the
     *        maximum and minimum specified for this component
     * @see #setMinimum
     * @see #setMaximum
     * @see #close
     */
    public void setProgress(int nv) {
        if (nv >= max) {
            close();
        }
        else if (nv >= lastDisp + reportDelta) {
            lastDisp = nv;
            if (myBar != null) {
                myBar.setValue(nv);
            }
            else {
                myBar = new JProgressBar();
                myBar.setMinimum(min);
                myBar.setMaximum(max);
                myBar.setValue(nv);
                noteLabel = new JLabel("Waiting for data...");
                pane = new ProgressOptionPane(new Object[] {status,
                                                            noteLabel,
                                                            myBar});
                dialog = pane.createDialog(parentComponent,
                    UIManager.getString(
                        "ProgressMonitor.progressText"));
                dialog.pack();
                dialog.setVisible(true);
            }
        }
    }


    /** 
     * Indicate that the operation is complete.  This happens automatically
     * when the value set by setProgress is >= max, but it may be called
     * earlier if the operation ends early.
     */
    public void close() {
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
            dialog = null;
            pane = null;
            myBar = null;
        }
    }


    /** 
     * Returns true if the user hits the Cancel button in the progress dialog.
     */
    public boolean isCanceled() {
        if (pane == null) return false;
        Object v = pane.getValue();
        return ((v != null) &&
                (cancelOption.length == 1) &&
                (v.equals(cancelOption[0])));
    }


    /**
     * Specifies the additional note that is displayed along with the
     * progress message. Used, for example, to show which file the
     * is currently being copied during a multiple-file copy.
     *
     * @param note  a String specifying the note to display
     * @see #getNote
     */
    public void setNote(String note) {
        if (noteLabel != null) {
            noteLabel.setText(note);
        }
    }
}
