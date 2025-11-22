package edu.asu.jmars;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.MessageFormat;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * Provides a visual cache sizing and cleaning tool. It reports the size of
 * the JMARS data directory (~/jmars on *NIX systems) in MB, or 2^20 bytes.
 * 
 * Since it can take a LONG time to walk a very large cache directory, this
 * dialog creates another thread to do a preorder traversal of the JMARS data
 * directory.
 * 
 * Every 10 MB of data accumulated causes an update to the label to be sent, so
 * all updates except the last will be given in a multiple of 10 MB.
 * 
 * Every time a new directory is entered, or every 50th file in the current
 * directory, the scan checks to see if the dialog has been closed or the
 * counting thread somehow superseded. This should prevent the thread from
 * continuing to run for very long after the dialog is closed, in all cases.
 */
public class CacheDialog {
	private static final int gap = 4;
	private JDialog dialog;
	private JLabel spaceLabel = new JLabel("");
	private JButton clean = new JButton("Clean".toUpperCase());
	private JButton ok = new JButton("OK".toUpperCase());
	private int threadSequence = 0;
	private synchronized int getSequence() {
		return threadSequence;
	}
	private synchronized int nextSequence() {
		return ++threadSequence;
	}
	public CacheDialog(Frame owner) {
		dialog = new JDialog(owner, "File Cache Manager", true);
		dialog.add(createMain());
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.pack();
		updateSize();
		ok.requestFocusInWindow();
	}
	private Box createMain() {
		clean.setEnabled(false);
		clean.setToolTipText("Free up disk space by deleting JMARS cache files");
		clean.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clean.setEnabled(false);
				Main.cleanCache();
				updateSize();
			}
		});
		
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
				nextSequence();
			}
		});
		
		Box lbl = Box.createHorizontalBox();
		lbl.add(new JLabel("JMARS is using: "));
		lbl.add(spaceLabel);
		
		Box noteLine = Box.createHorizontalBox();
		noteLine.add(new JLabel("Note that some disk space is always used for your settings."));
		
		Box v = Box.createHorizontalBox();
		v.add(Box.createHorizontalStrut(gap));
		v.add(clean);
		v.add(Box.createHorizontalStrut(gap));
		v.add(Box.createHorizontalGlue());
		v.add(Box.createHorizontalStrut(gap));
		v.add(ok);
		v.add(Box.createHorizontalStrut(gap));
		
		Box b = Box.createVerticalBox();
		b.add(Box.createVerticalStrut(gap));
		b.add(lbl);
		b.add(Box.createVerticalStrut(gap));
		b.add(noteLine);
		b.add(Box.createVerticalStrut(3*gap));
		b.add(v);
		b.add(Box.createVerticalStrut(gap));
		
		Box h = Box.createHorizontalBox();
		h.add(b);
		h.add(Box.createGlue());
		return h;
	}
	public JDialog getDialog() {
		return dialog;
	}
	private void updateSize() {
		Thread updateThread = new Thread(new Runnable() {
			private final int id = nextSequence();
			private long total = 0;
			private long accum = 0;
			public void run() {
				try {
					sum(new File(Main.getJMarsPath()));
				} finally {
					if (accum > 0)
						total += accum;
					accum = 0;
					sendUpdate();
					clean.setEnabled(true);
				}
			}
			private void sum(File f) {
				if (f.isDirectory()) {
					int count = 0;
					for (File child: f.listFiles()) {
						if (count++ % 50 != 0 || id == getSequence()) {
							sum(child);
						} else {
							break;
						}
					}
				} else if (f.isFile() && id == getSequence()) {
					accum += f.length();
					if (accum > 10*1024*1024) {
						total += accum;
						accum = 0;
						sendUpdate();
					}
				}
			}
			private void sendUpdate() {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						spaceLabel.setText(MessageFormat.format("{0,number,#.##} MB", Math.ceil(total/1024/1024)));
					}
				});
			}
		});
		updateThread.setPriority(Thread.MIN_PRIORITY);
		updateThread.setDaemon(true);
		updateThread.setName("Cache Sizing Thread");
		updateThread.start();
	}
}
