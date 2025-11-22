package edu.asu.jmars.layer.threed;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.filechooser.FileFilter;

import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 3D print dialog for selection file location and base thickness. If a user has overridden the config value for thickness,
 * they will see a text input. If they do not have the config value, a combo box is presented.
 * 
 * @since 5.2.1
 * @author krios
 */
public class ThreeDPrintDialog extends JDialog {

	private JTextField fileLocationTF = null;
	private JButton selectFileLocationBtn = null;
	private JLabel baseThicknessLbl = null;
	private JTextField baseThicknessTF = null;
	private JLabel title = null;
	private JButton cancel = null;
	private JButton save = null;
	private Component locationParent = null;
	private String baseThickness = null;
	private JComboBox<String> baseThicknessCombo = null; 
	private static ThreeDPrintDialog dialog = null;
	private String fileStr = null;
	private String nameStr = null;
	private Callable<Void> callback;

	private JFileChooser stlChooser;
	
	private ThreeDPrintDialog (JDialog frame) {
		super(frame);
	}
	public static ThreeDPrintDialog getInstance(JDialog frame) {
		if (dialog == null) {
			dialog = new ThreeDPrintDialog(frame);
			dialog.baseThickness = Config.get("threed.3d_print_base_thickness", null);//only users who override this value will have one
			//threed.STL_base_pad 0.03333333
			dialog.layoutDialog();
		}
		return dialog;
	}
	public void display(ActionEvent e, Callable<Void> cb) {
		dialog.callback = cb;
		dialog.locationParent = (Component) e.getSource();
		dialog.setVisible(true);
	}
	private void layoutDialog() {
		JPanel panel = new JPanel();
		fileLocationTF = new JTextField(40);
		fileLocationTF.setEditable(false);
		selectFileLocationBtn = new JButton(selectFileAction);
		baseThicknessLbl = new JLabel("Base Thickness: ");
		baseThicknessTF = new JTextField(20);
		title = new JLabel("3D Print Options");
		cancel = new JButton(cancelAction);
		save = new JButton(saveAction);
		baseThicknessCombo = new JComboBox<String>(new String[]{"Thin", "Standard", "Thick"});
		baseThicknessCombo.setSelectedIndex(1);
		
		baseThicknessTF.setToolTipText("You have a config entry for 3d_print_base_thickness, so this value is populated.");
		baseThicknessCombo.setToolTipText("Thin: 0.013333333, Standard:  0.0233333333333, Thick: 0.0333333333");
		
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		layout.setHonorsVisibility(true);
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
			.addComponent(title)
			.addGroup(layout.createSequentialGroup()
				.addComponent(selectFileLocationBtn)
				.addComponent(fileLocationTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
			.addGroup(layout.createSequentialGroup()
				.addComponent(baseThicknessLbl)
				.addComponent(baseThicknessTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addComponent(baseThicknessCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
			.addGroup(layout.createSequentialGroup()
				.addComponent(save)
				.addComponent(cancel)));
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(title)
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(fileLocationTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
					.addComponent(selectFileLocationBtn))
				.addGroup(layout.createParallelGroup(Alignment.CENTER)
					.addComponent(baseThicknessLbl)
					.addComponent(baseThicknessTF, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
					.addComponent(baseThicknessCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
				.addGap(10)
				.addGroup(layout.createParallelGroup(Alignment.CENTER)
					.addComponent(save)
					.addComponent(cancel)));
		
		add(panel);
		setTitle("JMARS 3D Printing");
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		setLocationRelativeTo(locationParent);
		if (baseThickness == null) {
			baseThicknessTF.setVisible(false);//hide from normal users
		} else {
			baseThicknessTF.setText(baseThickness);
			baseThicknessCombo.setVisible(false);//hide from advanced users
		}
		pack();
		
	}
	private AbstractAction selectFileAction = new AbstractAction("Select File") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			if (stlChooser == null) {
				stlChooser = new JFileChooser(Util.getDefaultFCLocation());
				stlChooser.setDialogTitle("Choose STL Destination File");
				// add filter
				FileFilter stlFilter = new FileFilter() {
					public String getDescription() {
						return "3D Printer Files (*.stl)";
					}

					public boolean accept(File f) {
						if (f.isDirectory()) {
							return true;
						} else {
							return f.getName().toLowerCase().endsWith(".stl");
						}
					}
				};
				stlChooser.addChoosableFileFilter(stlFilter);
				stlChooser.setFileFilter(stlFilter);
			}

			int val = stlChooser.showSaveDialog(dialog);
			if (val == JFileChooser.APPROVE_OPTION) {
				fileStr = stlChooser.getSelectedFile().getPath();
				nameStr = stlChooser.getSelectedFile().getName();
				// check to see if user added extension, add it if they didn't
				if (!fileStr.contains(".stl")) {
					fileStr += ".stl";
				}
				fileLocationTF.setText(fileStr);
			} 
			
		}
	};
	public String getFileStr() {
		return fileStr;
	}
	public String getNameStr() {
		return nameStr;
	}
	public float getBaseThickness() {
		float returnValue = 0.00f;
		if (baseThickness == null) {
			String selItem = (String) baseThicknessCombo.getSelectedItem();
			switch(selItem.toLowerCase()) {
			case "thin":
				returnValue = 0.013333333f;
				break;
			case "standard":
				returnValue = 0.0233333333333f;
				break;
			case "thick":
				returnValue = 0.0333333333f;
				break;
			default:
				returnValue = 0.0233333333333f;
			}
		} else {
			returnValue = Float.parseFloat(baseThicknessTF.getText());
		}
		return returnValue;
	}
	private AbstractAction cancelAction = new AbstractAction("CANCEL") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			dialog.setVisible(false);
		}
	};
	private AbstractAction saveAction = new AbstractAction("SAVE") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			boolean valid = true;
			try {
				if (baseThickness != null) {//user set config value, input in textfield
					//validate free input
					try {
						Float.parseFloat(baseThicknessTF.getText());
					} catch (NumberFormatException nfe) {
						valid = false;
						Util.showMessageDialog("Invalid thickness value input.");
						baseThicknessTF.requestFocusInWindow();
						baseThicknessTF.selectAll();
					}
				}
				if (fileStr == null || fileStr.trim().length() == 0) {
					valid = false;
					Util.showMessageDialog("Please select file location.");
					fileLocationTF.requestFocusInWindow();
					fileLocationTF.selectAll();
				}
				if (valid) {
					dialog.setVisible(false);
					dialog.callback.call();
				}
				
			} catch (Exception e1) {
				DebugLog.instance().aprintln(e1.getMessage());
			}
		}
	};
}
