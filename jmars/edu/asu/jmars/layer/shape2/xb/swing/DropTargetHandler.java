package edu.asu.jmars.layer.shape2.xb.swing;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import javax.swing.JTextArea;
import edu.asu.jmars.layer.shape2.xb.data.service.Data;

 class DropTargetHandler implements DropTargetListener {

    private JTextArea textarea;

    public DropTargetHandler(JTextArea mytext) {
        this.textarea = mytext;
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        if (dtde.getTransferable().isDataFlavorSupported(UserTransferable.USER_DATA_FLAVOR)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
        } else {
            dtde.rejectDrag();
        }
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
    }

    @Override
    public synchronized void drop(DropTargetDropEvent dtde) {
        if (dtde.getTransferable().isDataFlavorSupported(UserTransferable.USER_DATA_FLAVOR)) {
            Transferable t = dtde.getTransferable();
            if (t.isDataFlavorSupported(UserTransferable.USER_DATA_FLAVOR)) {
                try {
                    Object transferData = t.getTransferData(UserTransferable.USER_DATA_FLAVOR);
                    if (transferData instanceof String) {
                        String mydata = (String) transferData;    //on Windows, mainly, transferData concatenates field_name +\t" + field_type
                        int tabIndex = mydata.indexOf('\t');	 //for ex, transferData = "Diameter\tDouble", so we just need to drop name only
                        if (tabIndex != -1) {
                            mydata =  mydata.substring(0, tabIndex);
                        }
                        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);                          
                        String delimstring = " " + Data.ALIAS_DELIM + mydata + Data.ALIAS_DELIM + " ";
                        XBMainPanel.INSERT_TEXT.insertAndRemove(textarea, delimstring);
                    } else {
                        dtde.rejectDrop();
                    }
                } catch (UnsupportedFlavorException ex) {
                    dtde.rejectDrop();
                } catch (IOException ex) {
                    dtde.rejectDrop();
                }
            } else {
                dtde.rejectDrop();
            }
        }
    }
}


