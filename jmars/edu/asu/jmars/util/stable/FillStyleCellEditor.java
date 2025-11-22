package edu.asu.jmars.util.stable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.EventObject;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.table.TableCellEditor;

import edu.asu.jmars.swing.AbstractCellEditor;
import edu.asu.jmars.util.FillStyle;

// cell editor for the Fill Style column
public class FillStyleCellEditor extends AbstractCellEditor implements TableCellEditor {
	
    private FillStyle selectedStyle;

    final JDialog dialog = new JDialog((JFrame)null, true);

    FillStyleTableCellRenderer renderer;
    
	public FillStyleCellEditor(){
		class FillStyleButton extends JButton {
			@Override
			public void doClick() {
				super.doClick();
				
				dialog.setVisible(false);
				fireEditingStopped();
			}

			@Override
			public void paint(Graphics g) {
				Graphics2D g2 = (Graphics2D)g;

				// TODO Auto-generated method stub
				super.paint(g);

				int inset = 15;

				Color bgGray = new Color(59, 63, 74);
				Dimension d = getSize();
				
				g2.setBackground(getBackground());
				g2.clearRect(0, 0, d.width, d.height);

				FillStyle.PlanetaryFill pf = FillStyle.id2PlanetaryFill.get(myFillStyle.toString());
				Rectangle2D textDimensions = g2.getFontMetrics().getStringBounds(pf.description, g2);
				double textHeight = textDimensions.getHeight();
				
				if (myFillStyle != null){
					g2.setColor(bgGray);
					g2.drawRect(0, 0, d.width, d.height);
					
					g2.setPaint(myFillStyle.getPaint(1));
					g2.fill(new Rectangle2D.Double(1,1, d.width-2, d.height - textHeight*2-2));
					
					g2.setPaint(bgGray);
					g2.fill(new Rectangle2D.Double(0, d.height - textHeight*2 - inset, d.width, textHeight*2+inset));
				}
												
				g2.setColor(Color.WHITE);
				if (pf.description.contains("<br>")) {
					g2.drawString(pf.description.substring(0,pf.description.indexOf("<br>")),0+inset, d.height-(int)textHeight-inset);
					g2.drawString(pf.description.substring(pf.description.indexOf("<br>")+4),0+inset, d.height-(int)textHeight);
				} else {
					g2.drawString(pf.description, 0+inset, d.height-(int)textHeight-inset);
				}

				
			}
			
			FillStyle myFillStyle;

			FillStyleButton(FillStyle newStyle) {
				super();
				myFillStyle=newStyle;
			}
		}

		JButton cancelBtn = new JButton(new AbstractAction("CANCEL") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				dialog.setVisible(false);
			}
		});
		JButton resetBtn = new JButton(new AbstractAction("RESET TO DEFAULT") {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				selectedStyle = null;
				fireEditingStopped();
				FillStyleCellEditor.this.acceptedInput=true;
				dialog.setVisible(false);
			}
		});
		JPanel mainPanel = new JPanel();
		GroupLayout layout = new GroupLayout(mainPanel);
		mainPanel.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		JLabel title = new JLabel("Fill Patterns");
		
		Group horizontalGrp0 = layout.createParallelGroup(Alignment.LEADING);
		Group verticalGrp0 = layout.createSequentialGroup();
		
		horizontalGrp0.addComponent(title);
		verticalGrp0.addComponent(title);
		
		Group horizontalGrp = layout.createParallelGroup(Alignment.CENTER);
		Group verticalGrp = layout.createSequentialGroup();
		verticalGrp.addGap(20);
		
		Group oneHGrp = null;
		Group oneVGrp = null;
		int count = 0;
		//4x4 for now, but can handle more in the future
		for (String id : FillStyle.id2PlanetaryFill.keySet()) {
			FillStyleButton fsb = new FillStyleButton(new FillStyle(id));
			fsb.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					selectedStyle=fsb.myFillStyle;
					fireEditingStopped();
					FillStyleCellEditor.this.acceptedInput=true;
					dialog.setVisible(false);
				}
			});
			fsb.setToolTipText(FillStyle.id2PlanetaryFill.get(id).tooltip);
			if (count%4==0) {
				if (count > 0) {
					horizontalGrp.addGroup(oneHGrp);
					verticalGrp.addGroup(oneVGrp);
				}
				oneHGrp = layout.createSequentialGroup();
				oneVGrp = layout.createParallelGroup(Alignment.CENTER);
				
			}
			oneHGrp.addComponent(fsb,135, 135, 135);
			oneVGrp.addComponent(fsb,135, 135, 135);
			
			count++;
		}
		horizontalGrp.addGroup(oneHGrp);
		verticalGrp.addGroup(oneVGrp);
		
		horizontalGrp.addGroup(layout.createSequentialGroup()
			.addComponent(cancelBtn)
			.addComponent(resetBtn));
		
		verticalGrp.addGap(20);
		verticalGrp.addGroup(layout.createParallelGroup(Alignment.BASELINE)
			.addComponent(cancelBtn)
			.addComponent(resetBtn));
		
		horizontalGrp0.addGroup(horizontalGrp);
		verticalGrp0.addGroup(verticalGrp);
		layout.setHorizontalGroup(horizontalGrp0);
		layout.setVerticalGroup(verticalGrp0);
		
		
		dialog.setContentPane(mainPanel);
		dialog.pack();
		
		renderer = new FillStyleTableCellRenderer() {
			public void paint(Graphics g) {
				super.paint(g);
				showEditor(this, false);
			}
		};	
	}

	public boolean isCellEditable(EventObject e){
		if (e instanceof MouseEvent) {
			int clickCount = ((MouseEvent)e).getClickCount();
			return (clickCount>1);
		} else {
			return false;
		}
	}

    public Object getCellEditorValue() { 
    	return selectedStyle; 
    }

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		shown=false;
		if (value instanceof FillStyle) {
			selectedStyle=(FillStyle)value;
		}
		if (value == null) {
			selectedStyle=null;
		}
				
		return renderer.getTableCellRendererComponent(table, value, isSelected, renderer.isFocusOwner(), row, column);
	}

	public boolean shouldSelectCell(EventObject evt){
		return true;
	}
	
	public boolean isInputAccepted() {
		return acceptedInput;
	}
	
	boolean shown = false;
	boolean acceptedInput = false;
	
	/**
	 * Shows the editor if we have not already shown it in this editing session.
	 * The editor is created on the AWT thread, at some later time, so that the
	 * various threads that lead here can finish their work without waiting on
	 * the popup dialog.
	 **/
	public void showEditor(final Component parent, boolean block) {
		if (!shown) {
			shown = true;
			Runnable todo = new Runnable() {
				public void run() {
					dialog.setLocationRelativeTo(parent);
					acceptedInput = false;
					dialog.setVisible(true);
				}
			};
			if (block) {
				todo.run();
			} else {
				SwingUtilities.invokeLater(todo);
			}
		}
	}
}
