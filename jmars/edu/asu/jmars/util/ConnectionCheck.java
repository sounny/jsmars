package edu.asu.jmars.util;

import javax.swing.SwingUtilities;

import edu.asu.jmars.Main;

public class ConnectionCheck implements Runnable {

	private static ConnectionCheck instance = null;
	private static long SLEEP_TIME = 5000L;
	private static DebugLog log = DebugLog.instance();
	private static Thread thread = null;
	private static String mainTitle = null;
	
	public static ConnectionCheck getInstance() {
		if (instance == null) {
			instance = new ConnectionCheck();
		}
		return instance;
	}
	@Override
	public void run() {
		try {
			if (Main.mainFrame != null && Main.mainFrame.isShowing()) {
				mainTitle = Main.mainFrame.getTitle();
				if (mainTitle.indexOf("(Disconnected)") < 0) {
					SwingUtilities.invokeLater(new Runnable() {//get back on EDT
						
						@Override
						public void run() {
							Main.mainFrame.setTitle(mainTitle + " (Disconnected)");
						}
					});
					
				}
			}
			while (JmarsHttpRequest.getConnectionFailed()) {
				JmarsHttpRequest.testNetworkAvailablility();
				if (!JmarsHttpRequest.getConnectionFailed()) {
					JmarsHttpRequest.resetFailedConnectionDisplayFlag();
					break;
				} else {
					Thread.sleep(SLEEP_TIME);
				}
			}
		} catch (Exception e) {
			log.println("Exception in connection check thread: "+e.getMessage());
		} finally {
			JmarsHttpRequest.resetFailedConnectionDisplayFlag();
			if (Main.mainFrame != null && Main.mainFrame.isShowing()) {
				SwingUtilities.invokeLater(new Runnable() {//get back on EDT
					@Override
					public void run() {
						Main.mainFrame.setTitle(mainTitle);
					}
				});
			}
		}
	}
	
	public static void startThread() {
		if (thread == null || !thread.isAlive()) {
			thread = new Thread(getInstance());
			thread.start();
		}
	}
}
