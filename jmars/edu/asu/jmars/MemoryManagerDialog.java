package edu.asu.jmars;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.install4j.api.SystemInfo;

import edu.asu.jmars.util.DebugLog;

public class MemoryManagerDialog extends JDialog {

	private static MemoryManagerDialog instance = null;
	JComboBox<String> heapCombo = null;
	JComboBox<String> stackCombo = null;
	long totalMem;
	File file = null;
	public static void displayMemoryManagerDialog() {
		if (instance == null) {
			instance = new MemoryManagerDialog(Main.mainFrame);
			instance.layoutDialog();
		}
		instance.displayDialog();
	}
	private MemoryManagerDialog(Frame owner) {
		super(owner);
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		setTitle("JMARS Memory Settings");
		setLocationRelativeTo(owner);
		file = new File(Main.getJMarsPath()+File.separator+"jmars.vmoptions");
	}
	private void displayDialog() {
		if (file != null && file.exists()) {
			BufferedReader buff = null;
			try {
				buff = new BufferedReader(new FileReader(file));
				while (buff.ready()) {
					String val = buff.readLine();
					if (val.startsWith("-Xmx")) {
						heapCombo.setSelectedItem(val.substring(4));
					} else if (val.startsWith("-Xss")) {
						stackCombo.setSelectedItem(val.substring(4));
					}
				}
			} catch (Exception e) {
				DebugLog.instance().println("MemoryManagerDialog Read Error: "+e.getMessage());
			} finally {
				if (buff != null) {
					try {
						buff.close();
					} catch (IOException e) {
						DebugLog.instance().println("MemoryManagerDialog Close Buffered Reader Error: "+e.getMessage());
					}
				}
			}
		} else {
			try {
				Class varClass = Class.forName("com.install4j.api.launcher.Variables");
				Method getVariable = varClass.getDeclaredMethod("getInstallerVariable", String.class);
				String xmxOption = (String) getVariable.invoke(null, "xmx");
				xmxOption = xmxOption.replace("-Xmx", "");
				String xssOption = (String) getVariable.invoke(null, "xss");
				xssOption = xssOption.replace("-Xss", "");
				heapCombo.setSelectedItem(xmxOption);
				stackCombo.setSelectedItem(xssOption);
			} catch (Exception e1) {
			}
		}
		pack();
		setVisible(true);
	}
	private void layoutDialog() {
		totalMem = SystemInfo.getPhysicalMemory();
		Runtime r = Runtime.getRuntime();
		final long max = r.maxMemory();
		final long used = r.totalMemory() - r.freeMemory();
		final int percent = (int) Math.round(100d * used / max);
		String totalMemStr = MessageFormat.format("You currently have {0} MB total memory available.", Math.round(totalMem / 1024 / 1024));
		String jmarsMemStr = MessageFormat.format("JMARS is able to use {0} MB.", Math.round(max / 1024 / 1024));
		String pctMsg = MessageFormat.format("JMARS is currently using {0}% of {1} MB used",percent, Math.round(max / 1024 / 1024));
		
		JPanel panel = new JPanel();
		JLabel totalLbl = new JLabel(totalMemStr);
		JLabel jmarsMemLbl = new JLabel(jmarsMemStr);
		JLabel pctMsgLbl = new JLabel(pctMsg);
		JLabel instructions = new JLabel("Select memory options for JMARS:");
		JLabel heapLbl = new JLabel("Maximum heap size (-Xmx) : ");
		JLabel stackLbl = new JLabel("Java stack size (-Xss) : ");
		JLabel noteLbl = new JLabel("Note: These settings will take effect the next time you start JMARS.");
		heapCombo = new JComboBox<String>();
		heapCombo.addItem("512m");
		heapCombo.addItem("768m");
		for (int x=1; x*1000 < (totalMem / 1024 / 1024); x++) {
	        String val = x + "G";
	        heapCombo.addItem(val);
	    }
		heapCombo.setSelectedItem("2G");
		stackCombo = new JComboBox<String>();
		for (int x=384; x<2048; x=x+32) {
			stackCombo.addItem(x+"k");
		}
		stackCombo.setSelectedItem("384k");
		JButton cancelButton = new JButton(cancelAction);
		JButton saveButton = new JButton(saveAction);
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER)
			.addGroup(layout.createParallelGroup(Alignment.LEADING)
				.addComponent(totalLbl)
				.addComponent(jmarsMemLbl)
				.addComponent(pctMsgLbl)
				.addComponent(instructions)
				.addGroup(layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(Alignment.TRAILING)
						.addComponent(heapLbl)
						.addComponent(stackLbl))
					.addGroup(layout.createParallelGroup()
						.addComponent(heapCombo)
						.addComponent(stackCombo))))
			.addComponent(noteLbl)
			.addGroup(layout.createSequentialGroup()
				.addComponent(cancelButton)
				.addComponent(saveButton)));
		layout.setVerticalGroup(layout.createSequentialGroup()
			.addComponent(totalLbl)
			.addComponent(jmarsMemLbl)
			.addComponent(pctMsgLbl)
			.addGap(20)
			.addComponent(instructions)
			.addGroup(layout.createParallelGroup(Alignment.LEADING)
				.addComponent(heapLbl)
				.addComponent(heapCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))//add sizes to prevent resize of height
			.addGroup(layout.createParallelGroup(Alignment.LEADING)
				.addComponent(stackLbl)
				.addComponent(stackCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
			.addGap(20)
			.addComponent(noteLbl)
			.addGroup(layout.createParallelGroup(Alignment.LEADING)
				.addComponent(cancelButton)
				.addComponent(saveButton)));
		
		add(panel);
	}
	
	private AbstractAction cancelAction = new AbstractAction("CANCEL") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			MemoryManagerDialog.this.setVisible(false);
		}
	};
	private AbstractAction saveAction = new AbstractAction("SAVE") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			saveSettings();
			MemoryManagerDialog.this.setVisible(false);
		}
	};
	private void saveSettings() {
		String heapVal = (String) heapCombo.getSelectedItem();
		String stackVal = (String) stackCombo.getSelectedItem();
		
		try {
			
			BufferedWriter buff = new BufferedWriter(new FileWriter(file));
			buff.write("-Xmx"+heapVal);
			buff.newLine();
			buff.write("-Xss"+stackVal);
			buff.newLine();
			buff.close();
			
		} catch (Exception e) {
			DebugLog.instance().println("MemoryManagerDialog Write Error: "+e.getMessage());
		}
	}

}
