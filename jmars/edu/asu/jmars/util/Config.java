package edu.asu.jmars.util;

import edu.asu.jmars.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

/**
 ** Manages JMARS configuration parameters, such as server addresses
 ** and such. Retrieval of a value searches the following places,
 ** in the following order:
 **
 ** <ol>
 ** <li>System {@link System#getProperties properties}.</li>
 ** <li>The jmars.config file in the current directory.</li>
 ** <li>The jmars.config file in the user's home/jmars directory.</li>
 ** <li>The jmars.config file in the resources directory (i.e. the JAR).</li>
 ** </ol>
 **
 ** System properties are read with the prefix "jmars.", to prevent
 ** naming collisions. Most JVMs let you pass these with a -D flag on
 ** the command-line... our runme and jmars scripts automatically add
 ** the "jmars." prefix for you. When reading a config file, if it
 ** looks executable (magic bytes "#!"), then it will be executed and
 ** its OUTPUT read as the actual config file contents.
 **
 ** <p><b>Insert a +Config in your .debugrc file to see a useful dump
 ** of the actual configuration parameters in use at startup, as well
 ** as other diagnostics whenever properties are read/set. Extremely
 ** useful for debugging configuration problems.</b>
 **
 ** <p>Setting persistent properties at runtime is supported via
 ** several set functions. All of these <b>write through
 ** immediately</b>, to the user's home/jmars/jmars.config file.
 ** <b>Only GLOBAL USER "PREFERENCES" should be set as configuration
 ** properties at runtime</b>, since such state is awkward for the
 ** user to manipulate. Additionally, settable preferences should be
 ** made extremely bullet-proof... no matter what value the property
 ** takes on, the program should be ready with a default or other
 ** appropriate behavior.
 **/
public class Config
 {
 	// @since change bodies
	//Config String keys
	public static final String CONFIG_AUTH_DOMAIN = "auth_domain";
	public static final String CONFIG_BODY_NAME = "bodyname";
	public static final String CONFIG_POLAR_RADIUS = "polar_radius";
	public static final String CONFIG_EQUAT_RADIUS = "equat_radius";
	public static final String CONFIG_MEAN_RADIUS = "mean_radius";
	public static final String CONFIG_MAP_MAP_SERVER_DEFAULT_URL = "map.map_server.default.url";
	public static final String CONFIG_MAP_DEFAULT_SERVER = "map.default.server";
	public static final String CONFIG_MAP_DEFAULT_SOURCE = "map.default.source";
	public static final String CONFIG_EDITION = "edition";
	public static final String CONFIG_EMAIL = "email";
	public static final String CONFIG_LOGIN_LBL = "login.lbl";
	public static final String CONFIG_PRODUCT = "product";
	public static final String CONFIG_NOMENCLATURE_VERSION = "nomenclature_version";
	public static final String CONTENT_SERVER = "content_server";
	public static final String CONFIG_PUBLIC_RELEASE = "public_release";
	public static final String CONFIG_SELECTED_BODY = "current_body";
	public static final String CONFIG_DEFAULT_LANDMARK_TYPE = "default_landmark_type";
	public static final String CONFIG_SHOW_NOMENCLATURE = "show_nomenclature";
	public static final String CONFIG_SHOW_BODY_MENU = "show_body_menu";
	public static final String CONFIG_DEFAULT_GROUP = "default_group";
	public static final String CONFIG_SESSION_EXT = "session_ext";
	public static final String CONFIG_SHOW_BOUNDING_BOX = "show_bounding_box";
	public static final String CONFIG_THREED_SCALE_OFFSET = "threed.scaleOffset";
	public static final String CONFIG_OSM_URL = "osm_url";
	public static final String CONFIG_LAT_LON  = "coord.order";
	public static final String CONFIG_LON_SYSTEM  = "longitude.system";
	public static final String CONFIG_LAT_SYSTEM  = "latitude.system";
	public static final String CONFIG_UI_THEME  = "uitheme";
	public static final String CONFIG_UI_THEME_FUTURE  = "futureuitheme";
	public static final String CONFIG_DISABLE_3D = "disable_3D";
	
	// end change bodies
	private static DebugLog log = DebugLog.instance();

	private static Properties   jarProps = new Properties();
	private static Properties   dirProps = new Properties();
	private static Properties savedProps = new Properties();
	private static Set keySet = Collections.synchronizedSet(new TreeSet());

	// @since change bodies
	private static boolean containsMultipleBodies = false;
	private static String productPrefix = null;//used to distinguish between products so that more than one product can be in the config file
	//end change bodies
	
	private static void rebuildKeySet()
	 {
		keySet.clear();
		keySet.addAll(  jarProps.keySet());
		keySet.addAll(  dirProps.keySet());
		keySet.addAll(savedProps.keySet());
		for(Iterator i=System.getProperties().keySet().iterator();
			i.hasNext(); )
		 {
			String key = (String) i.next();
			if(key.startsWith("jmars."))
				keySet.add(key.substring("jmars.".length()));
		 }
	 }

	static
	 {
		loadJarProps();
		loadDirProps();
		loadSavedProps();
//		loadRemoteProps();

		rebuildKeySet();

		log.println(getConfigDump());

		// As good a place as any to check this.
		boolean badVersion = true;
		try {
			badVersion = System.getProperty("java.version").compareTo("1.5") < 0;
		} catch (Throwable e) {
			log.aprintln(e);
		}

		if(badVersion)
		 {
			log.aprintln("");
			log.aprintln(System.getProperty("java.runtime.name"));
			log.aprintln("Version: " + System.getProperty("java.version"));
			log.aprintln("*************************************************");
			log.aprintln("****            W A R N I N G !              ****");
			log.aprintln("*************************************************");
			log.aprintln("**** You're using an outdated version of the ****");
			log.aprintln("**** Java runtime. Java 1.5 is required.     ****");
			log.aprintln("*************************************************");
			log.aprintln("");
		 }
	 }

	/**
	 ** Given a key for a property, returns the property's value as a
	 ** string. REturns null if the key hasn't been defined.
	 **/
	public static String get(String key)
	 {
		String value = get(key, null);
		if(value == null)
		 {
			log.println("PROPERTY NOT FOUND: " + key);
			log.printStack(5);
		 }
		return  value;
	 }

	/**
	 ** Given a key, returns the property's value as a boolean.
	 ** Returns defaultValue if the key hasn't been defined or has an
	 ** unparsable value.
	 **
	 ** <p>The strings "true" and "yes" evaluate to true, the strings
	 ** "false" and "no" evaluate to false. All comparisons are
	 ** case-insensitive. Any other string is considered unparsable.
	 **/
	public static boolean get(String key, boolean defaultValue)
	 {
		String value = get(key, Boolean.toString(defaultValue)).toLowerCase();
		if(value.equals("true")  ||  value.equals("yes"))  return  true;
		if(value.equals("false")  ||  value.equals("no"))  return  false;
		return  defaultValue;
	 }

	/**
	 ** Given a key, returns the property's value as a double. Returns
	 ** defaultValue if the key hasn't been defined or has an
	 ** unparsable value.
	 **/
	public static double get(String key, double defaultValue)
	 {
		try
		 {
			return  Double.parseDouble(get(key));
		 }
		catch(Exception e)
		 {
			log.println(e);
			return  defaultValue;
		 }
	 }

	/**
	 ** Given a key, returns the property's value as an int. Returns
	 ** defaultValue if the key hasn't been defined or has an
	 ** unparsable value.
	 **/
	public static int get(String key, int defaultValue)
	 {
		try
		 {
			return  Integer.parseInt(get(key));
		 }
		catch(Exception e)
		 {
			log.println(e);
			return  defaultValue;
		 }
	 }

	/**
	 ** Given a prefix for some number of dot-extended properties,
	 ** returns an array of keys and values for all keys that match
	 ** the prefix.
	 **
	 ** <p>For example, if the properties "foobar.a=1" and
	 ** "foobar.b=2" are defined, then calling getAll("foobar") will
	 ** return the following array: { "a", "1", "b", "2" }. Note that
	 ** the "foobar" prefix has been trimmed in the returned array's
	 ** keys.
	 **
	 ** @return An array of alternating keys and values, or a
	 ** zero-length array if no keys matching the prefix are
	 ** found. The returned array has its keys in sorted order, and
	 ** the keyPrefix has been removed from the keys.
	 **/
	public static String[] getAll(String keyPrefix)
	 {
		keyPrefix += ".";
		int keyPrefixLen = keyPrefix.length();
		List list = new ArrayList();

		synchronized(keySet)
		 {
			for(Iterator i=keySet.iterator(); i.hasNext(); )
			 {
				String key = (String) i.next();
				if(key.startsWith(keyPrefix))
				 {
					list.add(key.substring(keyPrefixLen));
					list.add(get(key));
				 }
			 }
		 }

		return  (String[]) list.toArray(new String[0]);
	 }
	
	/**
	 * Given a prefix for dot-extended properties, returns a HashMap
	 * of key value pairs
	 * @param keyPrefix
	 * @since change bodies
	 * @return
	 */
	public static Map<String,String> getValuesByPrefix(String keyPrefix){
		TreeMap<String,String> map = new TreeMap<String,String>();
		keyPrefix += ".";
		int keyPrefixLen = keyPrefix.length();

		synchronized(keySet)
		 {
			for(Iterator i=keySet.iterator(); i.hasNext(); )
			 {
				String key = (String) i.next();
				if(key.startsWith(keyPrefix))
				 {
					map.put(key.substring(keyPrefixLen), get(key));
				 }
			 }
		 }

		return map;
	 }

	/**
	 * Returns all keys that are children of the given parent key.
	 * 
	 * To do this, keys are treated as paths from the configuration tree's root
	 * node down to the referenced node, where each node in the path is
	 * separated from the others by a dot.
	 * 
	 * For example, given this snippet of jmars.config:
	 * a.b.c 1
	 * a.b.d 2
	 * a.c.x 3
	 * a.d   4
	 * 
	 * <code>getAllKeys("a")</code> returns {"a.b", "a.c", "a.d"}
	 * <code>getAllKeys("a.b")</code> returns {"a.b.c", "a.b.d"}
	 */
	public static String[] getChildKeys(String parentKey) {
		String keyPrefix = parentKey + ".";
		int keyPrefixLen = keyPrefix.length();
		Set uniqueList = new TreeSet();
		
		synchronized (keySet) {
			for (Iterator i = keySet.iterator(); i.hasNext();) {
				String key = (String) i.next();
				if (key.startsWith(keyPrefix)) {
					int nextDot = key.indexOf(".", keyPrefixLen);
					int end = (nextDot == -1 ? key.length() : nextDot);
					uniqueList.add(key.substring(keyPrefixLen, end));
				}
			}
		}
		
		return (String[]) uniqueList.toArray(new String[0]);
	}
	
	/**
	 * Given a key for a list of dot-numbered properties, returns the array
	 * of values for that list. Never returns null. Terminates the array at
	 * the first missing or empty value found (i.e. an empty string terminates
	 * an array in a config file).
	 * 
	 * @return The property values, or a zero-length array if no * properties
	 *         are found.
	 */
	public static String[] getArray(String keyPrefix)
	 {
		List values = new ArrayList();
		for(int i=1; get(keyPrefix+"."+i)!=null; i++)
		 {
			String val = get(keyPrefix+"."+i);
			if(val.equals(""))
				break;
			values.add(val);
		 }
		return  (String[]) values.toArray(new String[0]);
	 }
	
	/**
	 * As {@link #get(String, String)}, but locates and replaces variable names with values from the active JMARS session.
	 * 
	 * This method supports the following variables and gets values from the following static data:
	 * <table>
	 * <tr><th>Variable</th><th>Replaced with</th></tr>
	 * <tr><td>_timestamp_</td><td>Main.ABOUT().SECS</td></tr>
	 * <tr><td>_user_</td><td>Main.USER</td></tr>
	 * <tr><td>_password_</td><td>Main.PASS</td></tr>
	 * <tr><td>_passhash_mysql_</td><td>Util.mysqlPassHash(Main.PASS)</td></tr>
	 * </table>
	 */
	public static String getReplaced(String key, String defaultValue) {
		String val = get(key,defaultValue);
		
		long time = Main.ABOUT().SECS;
		if (time <= 0)
			time = new Date().getTime();
		
		val = val.replaceAll("_timestamp_", ""+time);
		val = val.replaceAll("_user_", Main.USER);
		val = val.replaceAll("_password_", Main.PASS);
		val = val.replaceAll("_passhash_mysql_", Util.mysqlPassHash(Main.PASS));
		// new users aren't created with this scheme just yet
		// val = val.replaceAll("_passhash_", Util.passHash(Main.PASS));
		
		return val;
	}
	
	/**
	 ** Identical to {@link #get(java.lang.String)}, but allows you to
	 ** set the default return value to something other than null.
	 **
	 ** @return The property's value, or <code>defaultValue</code> if
	 ** that property has never been set.
	 **/
	public static String get(String key, String defaultValue)
	 {
		String val;

		// System (command-line) properties first
		val = System.getProperty("jmars." + key);
		if(val != null)
			return  val.trim();

		// Then the working directory file
		val = dirProps.getProperty(key);
		if(val != null)
			return  val.trim();

		// Then the saved user file
		val = savedProps.getProperty(key);
		if(val != null)
			return  val.trim();

		// Finally the jar file
		val = jarProps.getProperty(key);
		if(val != null)
			return  val.trim();

		// Return default as a last resort
		return  defaultValue;
	 }

	/**
	 ** Sets a global persistent String property in the user's
	 ** home/jmars directory config file. If newValue is null, then
	 ** this functions as a remove operation, clearing that property
	 ** from the file. Calls to this method that cause a change result
	 ** in immediate WRITE-THROUGH to the file in the user's
	 ** home/jmars directory.
	 **
	 ** @return false if a write-through to the file was attempted AND
	 ** failed, otherwise true.
	 **/
	public static synchronized boolean set(String key, String newValue)
	 {
		return  setImpl(key, newValue)  ||  flushSavedPrefs();
	 }

	/**
	 ** Sets a global persistent double property in the user's
	 ** home/jmars directory config file. If newValue is null, then
	 ** this functions as a remove operation, clearing that property
	 ** from the file. Calls to this method that cause a change result
	 ** in immediate WRITE-THROUGH to the file in the user's
	 ** home/jmars directory.
	 **
	 ** @return false if a write-through to the file was attempted AND
	 ** failed, otherwise true.
	 **/
	public static synchronized boolean set(String key, double newValue)
	 {
		return  set(key, Double.toString(newValue));
	 }

	/**
	 ** Sets a global persistent boolean property in the user's
	 ** home/jmars directory config file. If newValue is null, then
	 ** this functions as a remove operation, clearing that property
	 ** from the file. Calls to this method that cause a change result
	 ** in immediate WRITE-THROUGH to the file in the user's
	 ** home/jmars directory.
	 **
	 ** @return false if a write-through to the file was attempted AND
	 ** failed, otherwise true.
	 **/
	public static synchronized boolean set(String key, boolean newValue)
	 {
		return  set(key, Boolean.toString(newValue));
	 }

	/**
	 ** Sets a global persistent array property in the user's
	 ** home/jmars directory config file. If newValue is null, then
	 ** this functions as a remove operation, clearing that property
	 ** from the file. Calls to this method that cause a change result
	 ** in immediate WRITE-THROUGH to the file in the user's
	 ** home/jmars directory.
	 **
	 ** <p>If newArray is zero-length, it is indistinguishable from
	 ** null and is treated the same. Also, null or zero-length
	 ** strings in the array will act as array terminators when the
	 ** array is eventually read back in, so you probably don't want
	 ** to include such strings in the array.
	 **
	 ** @return false if a write-through to the file was attempted AND
	 ** failed, otherwise true.
	 **/
	public static synchronized boolean setArray(String key, String[] newArray)
	 {
		String[] oldArray = getArray(key); // never null
		if(newArray == null) newArray = new String[0];
		boolean nochange = true;

		int maxlen = Math.max(newArray.length, oldArray.length);
		log.println("new = " + newArray.length);
		log.println("old = " + oldArray.length);
		log.println("maxlen = " + maxlen);

		// Update all elements... if the new array is shorter than the
		// old one, this will involve setting some elements to null to
		// clear them.
		for(int i=0; i<maxlen; i++)
			nochange =
				setImpl(key + "." + (i+1),
						i<newArray.length ? newArray[i] : null)
				&& nochange;

		return  nochange  ||  flushSavedPrefs();
	 }

	/**
	 ** Sets a global persistent property in the user's home/jmars
	 ** directory config file. If newValue is null, then this
	 ** functions as a remove operation, clearing that property from
	 ** existence. Returns an indication of whether any change
	 ** results.
	 **
	 ** <p>Calls to this method do NOT cause a write-through to the
	 ** file.
	 **
	 ** @return true if no visible change resulted, false if a net
	 ** change was performed.
	 **/
	private static boolean setImpl(String key, String newValue)
	 {
		log.println("Attempting to set [" + key + "] to [" + newValue + "]");

		String oldValue = savedProps.getProperty(key);

		// Process the change on our in-memory copy of the home/jmars
		// properties file.
		if(newValue == null)
		 {
			savedProps.remove(key);
			keySet.remove(key);
		 }
		else
		 {
			savedProps.put(key, newValue);
			keySet.add(key);
		 }

		// Make sure that none of the other properties objects contain
		// this key anymore, since it's been overridden.
		jarProps.remove(key);
		dirProps.remove(key);
		System.getProperties().remove("jmars." + key);

		// Does the newValue have any effect on the home/jmars config file?
		return  oldValue==null ? newValue==null : oldValue.equals(newValue);
	 }

	/**
	 ** Performs a write-through of the saved preferences file.
	 **
	 ** @return true if the write succeeded, false otherwise
	 **/
	private static boolean flushSavedPrefs()
	 {
		// Write the change out to disk
		OutputStream out = null;
		String fname = Main.getJMarsPath() + "jmars.config";
		log.printStack(-1);
		log.println("Attempting to write " + fname);
		try
		 {
			String header = " BE CAREFUL WHEN EDITING! " +
					"This file is overwritten by JMARS while it runs.";
			out = new FileOutputStream(fname);
			savedProps.store(out, header);
		 }
		catch(IOException e)
		 {
			log.aprintln("FAILED TO SAVE " + fname + " FILE!");
			log.aprintln(e.toString());
			log.println(e);
			return  false;
		 }
		finally
		 {
			try { out.close(); } catch(Exception e) { }
		 }
		return  true;
	 }

	/**
	 ** Similar to {@link #get}, except specifically used for
	 ** properties whose values are database URLs. The returned object
	 ** has its fields parsed out (some of which may be null, if it's
	 ** an invalid URL). Returns null if the property doesn't exist.
	 **/
	public static DbURL getDbURL(String key)
	 {
		String url = get(key);
		if(url == null)
			return  null;

		return  new DbURL(url);
	 }

	/**
	 ** Given the URL to a config file, returns an {@link InputStream}
	 ** to it. If the url doesn't point into a jar file and the first
	 ** two characters of the config file are "#!", it executes the
	 ** file and returns a stream of the result. Otherwise opens it as
	 ** a normal file. Returns null if the config file cannot be
	 ** opened or executed for some reason.
	 **/
    private static InputStream openConfigFile(URL url)
    {
       if(url.getProtocol().equals("file")) // don't want jar protocol
        {
           InputStream fin = null;
           try
            {
               fin = url.openStream();
               if(fin.read() == '#'  &&
                  fin.read() == '!')
                {
                   String cmd = url.getFile();
                   String os = System.getProperty("os.name");
                   String params = System.getProperty("jmars-config");
                   if(params != null)
                       cmd += " " + params;
                   if (os.startsWith("Windows")) {
                	   String bashCmd = "c:/cygwin/bin/bash.exe";
                	   File f = new File(bashCmd);
                	   if(!f.exists()){
                		   log.println(bashCmd + " does not exist, trying /cygwin64/");
                		   bashCmd = "c:/cygwin64/bin/bash.exe";
                	   }
                	   
                	   cmd = bashCmd+ " -l -c '" + cmd.replaceAll("^\\/", "") + "'";
                       
                   }           
                   try
                    {
                       log.println("ATTEMPTING TO EXECUTE " + cmd);
                       InputStream tmp = Runtime.getRuntime().exec(cmd).getInputStream();
                       if(tmp != null) {
                           log.println("EXECUTED SUCCESSFULLY!");
                       }
                       return  tmp;
                    }
                   catch(IOException e)
                    {
                       log.aprintln("YOUR CONFIG FILE " + url +
                                    " LOOKS EXECUTABLE, " +
                                    "BUT EXECUTION FAILED:");
                       log.aprintln(e);
                       return  null;
                    }
                }
            }
           catch(IOException e)
            {
               log.println(e);
               return  null;
            }
           finally
            {
               try { fin.close(); } catch(Exception e) { }
            }
        }

       try
        {
           return  url.openStream();
        }
       catch(IOException e)
        {
           return  null;
        }
    }

	/**
	 ** Loads the application's main jmars.config file, from the jar
	 ** file.
	 **/
	private static void loadJarProps()
	 {
		String fname = "resources/jmars.config";
		InputStream fin = null;
		try
		 {
			URL url = Main.getResource(fname);
			if(url != null)
				fin = openConfigFile(url);
			if(fin == null)
			 {
				log.aprintln("********************************************");
				log.aprintln("**** CONFIGURATION ERROR *******************");
				log.aprintln("********************************************");
				log.aprintln("Unable to open the " + fname + " file!");
				log.aprintln("(it resolved to URL " + url + ")");
				log.aprintln("SOMETHING IS TERRIBLY WRONG!");
				return;
			 }
			jarProps.load(fin);
		 }
		catch(Exception e)
		 {
			log.aprintln("ERROR LOADING THE " + fname + " FILE, DUE TO:");
			log.aprintln(e);
		 }
		finally
		 {
			try
			 {
				if(fin != null)
					fin.close();
			 }
			catch(IOException e)
			 {
			 }
		 }
	 }

	/**
	 ** Loads the user's saved jmars.config file, from their
	 ** home/jmars directory.
	 **/
	private static void loadSavedProps()
	 {
		String fname = Main.getJMarsPath() + "jmars.config";
		InputStream fin = null;
		try
		 {
			fin = openConfigFile(new File(fname).toURL());
			if(fin != null)
			 {
				savedProps.load(fin);
				log.aprintln("NOTE: You're using a custom " + fname +" file!");
			 }
		 }
		catch(Exception e)
		 {
			if(e instanceof FileNotFoundException)
				return;
			log.aprintln("ERROR LOADING THE " + fname + " FILE, DUE TO:");
			log.aprintln(e);
		 }
		finally
		 {
			try
			 {
				if(fin != null)
					fin.close();
			 }
			catch(IOException e)
			 {
			 }
		 }
	 }

	/**
	 ** Loads the user's local jmars.config file, from the current
	 ** directory.
	 **/
	private static void loadDirProps()
	 {
		String fname = "jmars.config";
		InputStream fin = null;
		try
		 {
			fin = openConfigFile(new File(fname).toURL());
			if(fin != null)
			 {
				dirProps.load(fin);
				log.aprintln("NOTE: You're using a current-directory " +
							 fname + " file!");
			 }
		 }
		catch(Exception e)
		 {
			if(e instanceof FileNotFoundException)
				return;
			log.aprintln("ERROR LOADING THE " + fname + " FILE, DUE TO:");
			log.aprintln(e);
		 }
		finally
		 {
			try
			 {
				if(fin != null)
					fin.close();
			 }
			catch(IOException e)
			 {
			 }
		 }
	 }

	/**
	 ** If any properties are specified ANYWHERE (user, jar file,
	 ** command-line) named "config.url.*", we try to load the files
	 ** at those locations and overwrite existing properties with
	 ** them. The order in which the config.url.* files are loaded is
	 ** going to be key-sorted ASCII order (i.e. NOT the order in
	 ** which the keys were encountered in the file). They will be
	 ** loaded into the same cache used for the JAR file properties
	 ** (and can therefore be overridden just like any other JAR file
	 ** property).
	 **
	 ** <p>If a given URL starts with "http://", then either
	 ** "?built=SECONDS" or "&built=SECONDS" is appended to the URL,
	 ** as appropriate (SECONDS is the 2nd line of the about.txt
	 ** file).
	 **/
	public static void loadRemoteProps()
	 {
		boolean hadErrors = false;

		String[] urls = getAll("config_url");
		log.println("Found " + urls.length);
		for(int i=0; i<urls.length; i+=2)
		 {
			String key = urls[0];
			String url = urls[1];
			if(url.startsWith("http://"))
				url += (url.indexOf('?')==-1 ? "?" : "&") + 
					"jmars_config=" + Main.ABOUT().SECS;

			log.aprintln("Loading external " + key + " config...");
			try
			 {
				loadRemoteProps(url);
			 }
			catch(Throwable e)
			 {
				hadErrors = true;
				log.aprintln(e);
				log.aprintln("(" + url + ")");
				log.aprintln("LOADING OF EXTERNAL CONFIG FAILED, ABOVE!");
			 }
		 }
		rebuildKeySet();

		if(hadErrors)
		 {
			Util.showMessageDialog("JMARS was unable to read its configuration from one or\n" +
				"more of the configuration servers... the server or your\n" +
				"internet connection may be down. As a result, you may\n" +
				"have trouble using some JMARS features.\n" +
				"\n" +
				"Technical details may be available on the command-line.",
				"FAILED TO CONNECT",
				JOptionPane.ERROR_MESSAGE);
		 }

		log.println(getConfigDump());
	 }

	private static void loadRemoteProps(String url) throws Throwable {
	    
	    JmarsHttpRequest request = null;
	    try {
			// We don't use openConfigFile() because it swallows
			// IOExceptions details, AND we have no need for its smart
			// execution facility anyway.
			log.println("Opening " + url);
			request = new JmarsHttpRequest(url, HttpRequestType.GET);
			boolean successful = request.send();
			if(successful) {
				Properties tmp = new Properties();
				tmp.load(request.getResponseAsStream());
				log.println("SUCCESS!");
				jarProps.putAll(tmp);
				request.close();
			}
		} finally {
		    
			try {
			    request.close();
			} catch(Exception e) {
			    e.printStackTrace();
			}
		}
	} // loadRemoteProps()

	private static String getConfigDump()
	 {
		StringWriter str = new StringWriter();
		PrintWriter log = new PrintWriter(str);

		// From least-important to most-important
		SortedMap allProps = new TreeMap();
		addAll(allProps, "J", jarProps,   "");
		addAll(allProps, "S", savedProps, "");
		addAll(allProps, "D", dirProps,   "");
		addAll(allProps, "C", System.getProperties(), "jmars.");
		Iterator iter = allProps.keySet().iterator();

		log.println("****************************************");
		log.println("** J: Came from the jar file");
		log.println("** S: Came from saved jmars/jmars.config file");
		log.println("** D: Came from current working directory");
		log.println("** C: Command-line JVM override");
		log.println("** ACTIVE PROPERTIES:");
		while(iter.hasNext())
		 {
			String key = (String) iter.next();
			String val = (String) allProps.get(key);
			log.println(val.substring(0, 1) + ": " + key + "\t" +
						val.substring(1));
		 }
		log.println("****************************************");

		return  str.toString();
	 }

	/**
	 ** Adds to <code>dst</code> all the key-value pairs in
	 ** <code>src</code> whose keys start with <code>prefix</code>,
	 ** prepending <code>tag</code> to the added values.
	 **/
	private static void addAll(Map dst, String tag,
							   Properties src, String prefix)
	 {
		Iterator iter = src.keySet().iterator();
		while(iter.hasNext())
		 {
			String key = (String) iter.next();
			if(key.startsWith(prefix))
				dst.put(key.substring(prefix.length()),
						tag + src.getProperty(key));
		 }
	 }

	/**
	 ** Utility class, used by {@link #getDbURL}.
	 **/
	public static final class DbURL
	 {
		public final String url;
		public final String driver;
		public final String host;
		public final String dbname;
		
		private DbURL(String url)
		 {
			int colon = url.lastIndexOf(':');
			int question = url.lastIndexOf('?');
			if (question == -1) {
				question = url.length();
			}
			int slash = url.lastIndexOf('/');

			this.url    = url;
			this.driver = substringSafe(url, 0, colon);
			this.host   = substringSafe(url, colon + "://".length(), slash);
			this.dbname = substringSafe(url, slash+1, question);
		 }

		private static String substringSafe(String s, int a, int b)
		 {
			try
			 {
				return  s.substring(a,b);
			 }
			catch(Throwable e)
			 {
				return  null;
			 }
		 }

		public String toString()
		 {
			return  url;
		 }
	 }

	/**
	 ** Test driver for development.
	 **/
	public static void main(String[] av)
	 {
		PrintStream out = System.out;

		switch(av.length)
		 {

		 case 0:
			System.out.println(getConfigDump());
			break;

		 case 2:
			if(av[1].equals("-"))
				av[1] = null;
			Config.set(av[0], av[1]);
			// no break

		 case 1:
			System.out.println("[" + Config.get(av[0]) + "]");
			break;

		 case 4:
			out.println("test.str = " + Config.get("test.str"));
			out.println("test.int = " + Config.get("test.int", -1));
			out.println("test.dbl = " + Config.get("test.dbl", 0.0));
			out.println("test.bln = " + Config.get("test.bln", true));
			out.println("test.bln = " + Config.get("test.bln", false));
			out.println("test.arr = " +
						Arrays.asList(Config.getArray("test.arr")));

			out.println("Setting...");
			out.println(Config.set("test.str", av[0]));
			out.println(Config.set("test.int", Integer.parseInt(av[1])));
			out.println(Config.set("test.dbl", Double.parseDouble(av[2])));
			out.println(Config.set("test.bln", new Boolean(av[3]).booleanValue()));
			out.println(Config.setArray("test.arr", av));

			out.println("test.str = " + Config.get("test.str"));
			out.println("test.int = " + Config.get("test.int", -1));
			out.println("test.dbl = " + Config.get("test.dbl", 0.0));
			out.println("test.bln = " + Config.get("test.bln", true));
			out.println("test.bln = " + Config.get("test.bln", false));
			out.println("test.arr = " +
						Arrays.asList(Config.getArray("test.arr")));
			break;

		 default:
			System.err.println("ERROR!");
			break;

		 }
	 }
 }
