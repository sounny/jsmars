package edu.asu.jmars.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.json.JSONObject;

import edu.asu.jmars.Main;

public class ProxyInformation {
	public static final String PROXY_HOST = "host";
	public static final String PROXY_PORT = "port";
	public static final String PROXY_USER = "username";
	public static final String PROXY_PASSWD = "password";
	
    private String  proxyHost = null;
    private int     proxyPort = 0;
    private String  proxyUser = null;
    private String  proxyPass = null;
    private String  proxyNtlmDomain = null;
    private boolean useProxy = false;
    boolean authenticationUsed = false;
    boolean NtlmUsed = false;
    private DebugLog log = DebugLog.instance(); // for logging errors

    // Singleton
    public static final ProxyInformation INSTANCE = new ProxyInformation();
    
    //Hide the constructor
    private ProxyInformation() {
    	readFile();
    }
    
    
    // PUBLIC METHODS

    public static ProxyInformation getInstance() {
        return INSTANCE;
    }
    
    public void readFile() {
    	// Attempt to grab proxy settings from file.
    	/*
    	 * ~/.jmars_proxy_config is a json file with the following schema:
    	 * {
    	 *     host: string containing a hostname or IP address,
    	 *     port: integer port number,
    	 *     username: proxy username (leave blank if no auth required),
    	 *     password: proxy password (leave blank if no auth required)
    	 * }
    	 */
        File configfile = null;
    	try {
    		configfile = new File(Main.getUserHome(), ".jmars_proxy_config");
    		
    		if (configfile.exists()) {
	    		// Ensure that the file is protected such that only the owner can modify it
	/*    		Path filePath = Paths.get(configfile.getAbsolutePath());
	            PosixFileAttributes attr = Files.readAttributes(filePath, PosixFileAttributes.class);
	            Set<PosixFilePermission> perms = attr.permissions();
	            if (perms.contains(GROUP_WRITE) || perms.contains(OTHERS_WRITE)) {
	                System.out.println("Cannot use provided proxy config file-- it is not secure.");
	            } else {
	*/                Scanner jsonstream = new Scanner(configfile);
	                jsonstream.useDelimiter("\\Z");
	                String jsonTxt = jsonstream.next();
	                jsonstream.close();
	                
	                JSONObject proxysettings = new JSONObject(jsonTxt);
	                
	                if (proxysettings.has(PROXY_HOST))
	                    setHost(proxysettings.getString(PROXY_HOST));
	                if (proxysettings.has(PROXY_PORT))
	                    setPort(proxysettings.getInt(PROXY_PORT));
	                if (proxysettings.has(PROXY_USER))
	                    setUsername(proxysettings.getString(PROXY_USER));
	                if (proxysettings.has(PROXY_PASSWD))
	                    setPassword(proxysettings.getString(PROXY_PASSWD));    
	                useProxy = true;
	/*            }
	*/    		
    		}
    	} catch (Exception e) {
    		this.log.println(e.getMessage());
    		this.log.println("ProxyInformation: Proxy config file "+configfile.getAbsolutePath()+" not found. Assuming no proxy server. "+e.toString());
    	}
    }
    
    public String getHost() {
        return proxyHost;
    }
    
    public int getPort() {
        return proxyPort;
    }
    
    public String getUsername() {
        return proxyUser;
    }

    public String getPassword() {
        return proxyPass;
    }
    
    public String getNtlmDomain() {
        return proxyNtlmDomain;
    }

    public boolean isProxyUsed() {
        return useProxy;
    }
    public void setUseProxy(boolean use) {
    	useProxy = use;
    }
    public boolean isProxySet() {
    	if (proxyHost != null && proxyHost.trim().length() > 0 && proxyPort > -1) {
    		return true;
    	}
    	return false;
    }
    public boolean isAuthenticationUsed() {
        return authenticationUsed;
    }

    public boolean isNtlmUsed() {
        return NtlmUsed;
    }

    public void setHost(String host) {
        proxyHost = host;
     }
 
    public void setPort(int port) {
        proxyPort = port;       
    }

    public void setUsername(String user) {
        proxyUser = user;       
        if (user != null) {
            authenticationUsed = true;           
        }
    }

    public void setPassword(String pass) {
        proxyPass = pass;       
    }
    
    public void setNtlmDomain(String domain) {
        proxyNtlmDomain = domain;
        if (domain != null) {
            NtlmUsed = true;           
        }
    }
    public void writeFile() {
    	FileWriter fw = null;
    	try {
    		String jsonStr = null;
    		JSONObject json = new JSONObject();
    		json.put(PROXY_HOST, proxyHost);
    		json.put(PROXY_PORT, proxyPort);
    		json.put(PROXY_USER, proxyUser);
    		json.put(PROXY_PASSWD, proxyPass);
    		jsonStr = json.toString(4);
	    	File f = new File (Main.getUserHome() + File.separator+".jmars_proxy_config");
	    	fw = new FileWriter(f);
	    	fw.write(jsonStr);
	    	fw.flush();
    	} catch (Exception e) {
    		DebugLog.instance().println(e.getMessage());
    	} finally {
    		if (fw != null) {
    			try {
					fw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
    	
    }

} // class ProxyInformation
