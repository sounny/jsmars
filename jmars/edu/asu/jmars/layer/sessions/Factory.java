package edu.asu.jmars.layer.sessions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URISyntaxException;
//import java.net.URL;
//import java.net.URLConnection;
//import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

//import org.apache.commons.httpclient.HttpClient;
//import org.apache.commons.httpclient.HttpConnectionManager;
//import org.apache.commons.httpclient.cookie.CookiePolicy;
//import org.apache.commons.httpclient.methods.GetMethod;






import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.SavedLayer;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.HttpRequestType;

/**
 * Loads layers from a series of jmars.config properties. The properties have
 * the form 'sessions.<name>=<httpurl>'. Naming each URL allows custom jmars.config
 * files to override the value for some source of jlf data if necessary.
 * 
 * The file at the given HTTP server URL will be a simple jlf file. The viewParms
 * Hashtable on each SavedLayer instance will be checked for some name/value pairs
 * specific to loading of remote sessions:
 * 
 * <ul>
 * <li>If 'session.menus' is a String[][], it will be used to create menus for
 * the layers, one per element of the outer array and with path elements for
 * each menu provided by the inner array.
 * </ul>
 * 
 * Each request will be made on a temporary thread to avoid slowing the startup
 * of JMARS. After all temporary threads have returned, an action will be added
 * to run as soon as possible on the AWT event thread to update the menus.
 * 
 * Every URL will be processed by {@link Config.getReplaced(String,String)}, so
 * some details of the current JMARS session can be provided with the request
 * as GET arguments.
 */
public class Factory extends LViewFactory {
	private static DebugLog log = DebugLog.instance();
	private static final String[] sessions = Config.getAll("sessions");
	final List<Thread> threads = new ArrayList<Thread>();
	final List<List<SavedLayer>> results = new ArrayList<List<SavedLayer>>();
	public Factory() {
		if (sessions == null) {
			return;
		}
		if (JmarsHttpRequest.isStampServerAvailable()) {
			for (int i = 0; i < sessions.length; i+=2) {
				final String name = sessions[i];
				threads.add(new Thread(new Runnable() {
					public void run() {
						load(name);
					}
				}));
			}
			for (Thread t: threads) {
				t.setDaemon(true);
				t.start();
			}
		}
	}

	/** read the xml on a separate thread in case the download takes awhile, parse it, and if the last download has finished or timed out, call finish(). */
	private void load(String name) {
	    JmarsHttpRequest request = null;
		try {
//		    HttpClient client = new HttpClient();                                   // TODO (PW) replace with JmarsHttpRequest
//			client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY); // TODO (PW) Possible custom config item for JmarsHttpRequest???
//			HttpConnectionManager conMan = client.getHttpConnectionManager();
//			conMan.getParams().setConnectionTimeout(10*1024);
			String url = Config.getReplaced("sessions." + name, null);
			if (url == null) {
				System.err.println("Could not read URL for session named " + name);
				return;
			}
            request = new JmarsHttpRequest(url, HttpRequestType.GET);
            request.setBrowserCookies();
//			GetMethod get = new GetMethod(url);                                     // TODO (PW) replace
//			int code = client.executeMethod(get);                                   // TODO (PW) replace
            boolean status = request.send();
            if (!status) {
				System.err.println("Factory: HTTP code " + request.getStatus() + " received when downloading session from " + url);
				return;
			}
			// parse it into SavedLayer instances
			List<SavedLayer> layers = SavedLayer.load(request.getResponseAsStream());
			synchronized(this) {
				results.add(layers);
			}
		} catch (Exception e) {
			synchronized(this) {
				System.err.println("Error processing session named " + name + ":");
				e.printStackTrace();
			}
		} finally {
			finish();
			request.close();
		}
	}
	private synchronized void finish() {
		threads.remove(Thread.currentThread());
		if (threads.isEmpty()) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					LManager.getLManager().refreshAddMenu();
				}
			});
		}
	}
	public LView createLView() {
		return null;
	}
	public LView recreateLView(SerializedParameters parmBlock) {
		return null;
	}
	public void createLView(boolean async) {
		// unused
	}
	public String getName() {
		return "Sessions";
	}
	public String getDesc() {
		return getName();
	}
	protected JMenuItem[] createMenuItems() {
		JMenu root = new JMenu("Sessions" + (threads.isEmpty() ? "" : " (loading)"));
		// make a copy inside the lock, and do processing outside the lock
		List<SavedLayer> layers = new ArrayList<SavedLayer>();
		synchronized(this) {
			for (List<SavedLayer> set: results) {
				layers.addAll(set);
			}
		}
		for (SavedLayer layer: layers) {
			// if there are menus, create a factory to load this layer and anchor it at each menu location
			Object o = layer.viewParms.get("session.menus");
			if (o instanceof String[][]) {
				String[][] paths = (String[][])o;
				for (String[] path: paths) {
					insert(path, 0, layer, root.getPopupMenu());
				}
			}
		}
		root.setEnabled(root.getPopupMenu().getComponentCount() > 0);
		return new JMenuItem[]{root};
	}
	private static void insert(String[] path, int depth, final SavedLayer layer, JPopupMenu menu) {
		if (depth < path.length) {
			JMenu nextMenu = null;
			for (int i = 0; i < menu.getComponentCount(); i++) {
				Component child = menu.getComponent(i);
				if (child instanceof JMenu && ((JMenu)child).getText().equals(path[depth])) {
					nextMenu = (JMenu)child;
				}
			}
			if (nextMenu == null) {
				nextMenu = new JMenu(path[depth]);
				menu.add(nextMenu);
			}
			insert(path, depth + 1, layer, nextMenu.getPopupMenu());
		} else {
			JMenuItem item = new JMenuItem(layer.layerName);
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					layer.materialize();
				}
			});
			menu.add(item);
		}
	}
}
