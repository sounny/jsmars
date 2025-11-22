package edu.asu.jmars.layer.shape2.xb.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit.DecreaseFontSizeAction;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaEditorKit.IncreaseFontSizeAction;
import org.fife.ui.rtextarea.RTextArea;


public class XBRichSyntaxTextArea extends RSyntaxTextArea {
	private JMenu fontMenu;
	private static AbstractAction increaseFontAction;
	private static AbstractAction decreaseFontAction;
	private static AbstractAction myPasteAction;
	private static AbstractAction clearAll;
	private static final String MSG	= "org.fife.ui.rsyntaxtextarea.RSyntaxTextArea";
	private static final int MY_VIEW_MENU_INDEX = 3;
	
	
	@Override
	protected JPopupMenu createPopupMenu() {
		JPopupMenu popup = super.createPopupMenu();
		removeNotNeededMenuItems(popup);
		customizePasteMenu(popup);
		appendFontMenu(popup);
		appendClearAllMenu(popup);
		return popup;
	}

	private void customizePasteMenu(JPopupMenu popup) {
		AbstractAction pastemenuitem = RTextArea.getAction(PASTE_ACTION);		
		if (pastemenuitem == null) {return; }
		for (Component component : popup.getComponents()) {
		    if (component instanceof JMenuItem) {
		        JMenuItem menuItem = (JMenuItem) component;
		        if (menuItem.getText().equals(pastemenuitem.getValue(pastemenuitem.NAME))) {	
		        	//remove standard Paste, and add mine
		        	int index = popup.getComponentIndex(menuItem);		        	
		        	myPasteAction = new MyPaste(this, menuItem.getText());
		    		JMenuItem pasteitem = createPopupMenuItem(myPasteAction);		    		
		    		pasteitem.setAccelerator(menuItem.getAccelerator());
		    		popup.remove(menuItem);
		    		popup.add(pasteitem, index);		            
		            break;
		        }
		    }	
		}		
	}

	private void removeNotNeededMenuItems(JPopupMenu popup) {
		//we don't need 'Folding', so remove it. Maybe other items, later
		ResourceBundle bundle = ResourceBundle.getBundle(MSG);
		String foldingmenuitemname = bundle.getString("ContextMenu.Folding");
		if (foldingmenuitemname == null) {return; }
		for (Component component : popup.getComponents()) {
		    if (component instanceof JMenuItem) {
		        JMenuItem menuItem = (JMenuItem) component;
		        if (menuItem.getText().equals(foldingmenuitemname)) {		          
		            popup.remove(menuItem);
		            break;
		        }
		    }	
		}
	}

	private void appendFontMenu(JPopupMenu popup) {
		fontMenu = new JMenu("View");
		increaseFontAction = new MyZoomIn(this);
		decreaseFontAction = new MyZoomOut(this);
		JMenuItem zoomin = createPopupMenuItem(increaseFontAction);
		zoomin.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK));
		fontMenu.add(zoomin);
		JMenuItem zoomout = createPopupMenuItem(decreaseFontAction);
		zoomout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
		fontMenu.add(zoomout);			
		popup.add(fontMenu, MY_VIEW_MENU_INDEX);
		popup.add(new JSeparator(),  MY_VIEW_MENU_INDEX+1);
	}
	
	private void appendClearAllMenu(JPopupMenu popup) {
		int index = popup.getComponentCount() - 1;
		clearAll = new MyClearAll(this);
		JMenuItem clearItem = createPopupMenuItem(clearAll);
		popup.add(new JSeparator(), index +1);
		popup.add(clearItem);
		popup.add(new JSeparator());
	}	
	

	private class MyZoomIn extends AbstractAction {
		RSyntaxTextAreaEditorKit.IncreaseFontSizeAction myincrease;
		XBRichSyntaxTextArea richsyntaxtextarea;

		MyZoomIn(XBRichSyntaxTextArea xbRichSyntaxTextArea) {
			richsyntaxtextarea = xbRichSyntaxTextArea;
			putValue(NAME, "Zoom In");
			myincrease = new IncreaseFontSizeAction();			
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			myincrease.actionPerformedImpl(e, richsyntaxtextarea);
		}
	}

	private class MyZoomOut extends AbstractAction {
		RSyntaxTextAreaEditorKit.DecreaseFontSizeAction mydecrease;
		XBRichSyntaxTextArea richsyntaxtextarea;

		MyZoomOut(XBRichSyntaxTextArea xbRichSyntaxTextArea) {
			richsyntaxtextarea = xbRichSyntaxTextArea;
			putValue(NAME, "Zoom Out");
			mydecrease = new DecreaseFontSizeAction();			
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			 mydecrease.actionPerformedImpl(e, richsyntaxtextarea);
		}
	}
	
	private class MyClearAll extends AbstractAction {
		XBRichSyntaxTextArea richsyntaxtextarea;

		MyClearAll(XBRichSyntaxTextArea xbRichSyntaxTextArea) {
			richsyntaxtextarea = xbRichSyntaxTextArea;
			putValue(NAME, "Clear All");					
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			SwingUtilities.invokeLater(() -> richsyntaxtextarea.setText("")); 			
		}
	}	
	
	private class MyPaste extends AbstractAction {
		XBRichSyntaxTextArea richsyntaxtextarea;
		RSyntaxTextAreaEditorKit.PasteAction mypaste;

		MyPaste(XBRichSyntaxTextArea xbRichSyntaxTextArea, String origitemname) {
			richsyntaxtextarea = xbRichSyntaxTextArea;
			putValue(NAME, origitemname);
			mypaste = new RSyntaxTextAreaEditorKit.PasteAction();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (richsyntaxtextarea != null) {
				String currenttext = richsyntaxtextarea.getText();
				if (UserPromptFormula.ON_FORMULA_START.asString().equals(currenttext)) {
					SwingUtilities.invokeLater(() -> {
						richsyntaxtextarea.setText("");
						mypaste.actionPerformedImpl(e, richsyntaxtextarea);
					});
				} else {
					mypaste.actionPerformedImpl(e, richsyntaxtextarea);
				}
			}
		}
	}
}
