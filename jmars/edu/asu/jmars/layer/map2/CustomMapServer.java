package edu.asu.jmars.layer.map2;

import java.awt.geom.Rectangle2D;
import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

//import org.apache.commons.httpclient.HttpClient; // TODO remove old code
//import org.apache.commons.httpclient.HttpMethod;
//import org.apache.commons.httpclient.HttpMethodRetryHandler;
//import org.apache.commons.httpclient.HttpStatus;
//import org.apache.commons.httpclient.NameValuePair;
//import org.apache.commons.httpclient.URIException;
//import org.apache.commons.httpclient.methods.PostMethod;
//import org.apache.commons.httpclient.methods.multipart.FilePart;
//import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
//import org.apache.commons.httpclient.methods.multipart.Part;
//import org.apache.commons.httpclient.methods.multipart.PartSource;
//import org.apache.commons.httpclient.methods.multipart.StringPart;
//import org.apache.commons.httpclient.params.HttpMethodParams;


import edu.asu.jmars.util.Config;
import edu.asu.jmars.layer.map2.msd.SharedGroup;
import edu.asu.jmars.layer.map2.msd.SharedMap;
import edu.asu.jmars.layer.map2.msd.SharedUser;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.HttpRequestType;

import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.json.JSONObject;


/**
 * CustomMapServer is a MapServer with authentication to get maps for a
 * particular user. There is only one constructor that prevents creation
 * CustomMapServer objects except from serialized data in jmars.config.
 */
public class CustomMapServer extends WMSMapServer implements Serializable {
	private static final long serialVersionUID = 2029715131924588553L;
	
	public static String customMapServerName = "custom";
	private static DebugLog log = DebugLog.instance();
	private final String user;
	private transient final String passwd;
	private static final String SERVICE_VERSION = "custom_map_version";
	private static final String VERSION_NUMBER = "3";
	private final String auth_domain, product, bodyName;// @since change bodies
	
	/** Constructs a new custom map server with the given user's custom maps. 
	* @since change bodies - body parameter added
	*/
	public CustomMapServer(String serverName, String user, String passwd, String auth_domain, String product, String body) {
		super(serverName, "user=" + user);
		this.user = user;
		this.passwd = passwd;
		this.auth_domain = auth_domain;
		this.product = product;
		this.bodyName = body;// @since change bodies
	}
	
	/** Overrides the category of a custom map source */
	public void add(MapSource source) {
		String[][] empty = {{}};
		super.add(
			new WMSMapSource(
				source.getName(),
				source.getTitle(),
				source.getAbstract(),
				source.getUnits(),
				empty,
				this,
				source.hasNumericKeyword(),
				source.hasElevationKeyword(),
				source.hasGeologicKeyword(),
				(source instanceof WMSMapSource)? ((WMSMapSource)source).getLatLonBoundingBox(): null,
				source.getIgnoreValue(),
				source.getMaxPPD(),
				source.getOwner()));
	}
	
	/**
	 * @param name The descriptive name of this map
	 * @return The canonic unique name of  this map
	 */
	private String getCanonicName(String name) {
		String canonicName;
		if (product == null) {
			canonicName = user + "." + String.valueOf(name.hashCode());
		} else {
		// @since change bodies - added this.bodyName
			canonicName = user + "_" + product + "_" + this.bodyName + "." + String.valueOf(name.hashCode()); //added body name - KJR - 12/07/2011
		}
		return canonicName.replaceAll("[^0-9A-Za-z\\.-]", "_");
	}
	
	/**
	 * Send a local map file to the custom map server.
	 * @param file The File to post to the server.
	 * @param readGeoRef 
	 * @param name The descriptive name for the user to see.
	 * @param file The File to post to the server.
	 * @throws Exception If anything goes wrong. The message will contain the error or server response.
	 */
	public void uploadCustomMap(String remoteName, File file, Rectangle2D bounds, Double ignoreValue, boolean email, String description, boolean readGeoRef) throws Exception {
		String remoteID = getCanonicName(remoteName);
//		PostMethod post = getMethod("upload");
		JmarsHttpRequest request = getMethod("upload");
		addArgs(request, remoteName, bounds, ignoreValue, email, description, readGeoRef);
		addFile(request, file, remoteID);
		log.println("Uploading custom map named " + remoteName + " from local file " + file.getAbsolutePath() + " to layer id " + remoteID);
		String response = read(request);
		finishUpload("local", response, remoteID);
	}
	
	/**
	 * Send a remote map file to the custom map server.
	 * @param name The descriptive name for the user to see.
	 * @param remoteUrl The URL the server should download the image from
	 * @param readGeoRef TODO
	 * @throws Exception Thrown if anything goes wrong. The message will
	 * contain the server response.
	 */
	public void uploadCustomMap(String name, URL remoteUrl, Rectangle2D bounds, Double ignoreValue, boolean email, String description, boolean readGeoRef) throws Exception {
		JmarsHttpRequest request = getMethod("remote");
		addArgs(request, name, bounds, ignoreValue, email, description, readGeoRef);
		String remoteID = getCanonicName(name);
		request.addRequestParameter("rfile", remoteUrl.toString());
		request.addRequestParameter("lfile", remoteID);
		log.println("Uploading custom map named " + name + " from remote URL " + remoteUrl);
		String response = read(request);
		finishUpload("remote", response, remoteID);
	}
	
	protected JmarsHttpRequest getCapsMethod() {
		URI uri = getCapabilitiesURI(); 
		if (product != null)
			uri = getSuffixedURI(uri, Config.CONFIG_PRODUCT+"="+product,Config.CONFIG_BODY_NAME+"="+this.bodyName);// @since change bodies  added bodyName
		if (auth_domain != null)
			uri = getSuffixedURI(uri, "domain="+auth_domain);
		
//		PostMethod post = new PostMethod(uri.toString());
//		post.addParameter("passwd", passwd);
//		return post;
        JmarsHttpRequest method = new JmarsHttpRequest(uri.toString(), HttpRequestType.POST);
        method.addRequestParameter("passwd", passwd);
        return method;
	}
	
	private JmarsHttpRequest getMethod(String request) {
		URI uri = getSuffixedURI(getURI(),
				"request="+request,
				SERVICE_VERSION+"="+VERSION_NUMBER);
		
		if (product != null)
			uri = getSuffixedURI(uri, Config.CONFIG_PRODUCT+"="+product,Config.CONFIG_BODY_NAME+"="+this.bodyName);// @since change bodies - added bodyName
		if (auth_domain != null)
			uri = getSuffixedURI(uri, "domain="+auth_domain);
		
		JmarsHttpRequest method = new JmarsHttpRequest(uri.toString(), HttpRequestType.POST);
		method.addRequestParameter("passwd", passwd);
		return method;
	}
	
	/** Adds upload parameters to a post 
	 * @param readGeoRef */
	private static void addArgs(JmarsHttpRequest request, String remoteName, Rectangle2D bounds, Double ignoreValue, boolean email, String description, boolean readGeoRef) {
		request.addRequestParameter("name", remoteName);
		if (bounds != null && !readGeoRef) {
			double x1 = bounds.getMinX();
			double x2 = bounds.getMaxX();
			if (Math.min(x1,x2) < -540 || Math.max(x1,x2) > 540) {
				throw new IllegalArgumentException("Longitude values out of range -540,540");
			}
			while (x1 > x2) {
				x1 -= 360;
			}
			while (x1 + 360 < x2) {
				x1 += 360;
			}
			while (x2 > 180) {
				x1 -= 360;
				x2 -= 360;
			}
			while (x1 < -180) {
				x1 += 360;
				x2 += 360;
			}
			request.addRequestParameter("bbox", MessageFormat.format(
				"{0,number,#.######},{1,number,#.######},{2,number,#.######},{3,number,#.######}",
				x1,bounds.getMinY(),x2,bounds.getMaxY()));
		}
		if (ignoreValue != null) {
			request.addRequestParameter("ignore", MessageFormat.format("{0,number,#.######}",ignoreValue));
		}
		if (email) {
			request.addRequestParameter("sendemail","1");
		}
		if (description != null) {
		    request.addRequestParameter("map_abstract", description);
		}
		if (readGeoRef) {
		    request.addRequestParameter("read_geo_ref_data", "true");
		} else {
		    request.addRequestParameter("read_geo_ref_data", "false");
		}
	}
	
	private void finishUpload(String type, String response, String mapName) throws Exception {
		
		//remove any leading blanks sent back by the map server to keep the connection open
		response = response.replaceAll("^\\s*", "");
		
		if (response.toUpperCase().startsWith("ERROR:")) {
			log.println("Uploading " + type + " map failed with " + response);
			throw new Exception(response);
		}
		if (response.toUpperCase().contains("ERROR-1:")) {
			log.println("Image does not contain enough information or does not contain a projection");
			throw new Exception("Image does not contain enough information or does not contain projection information");
		}
		if (response.toUpperCase().contains("ERROR-2:")) {
			log.println("There is an Error reading the Geospatial Information on the image.");
			throw new Exception("There is an Error reading the Geospatial Information on the image.");
		}

		log.println("Remote upload succeeded");
		Thread.sleep(2000);
		loadCapabilities(false);
		MapSource source = getSourceByName(mapName);
		if (source == null) {
			throw new Exception(
					"Upload succeeded but custom map cannot be found with name " +
					mapName);
		}
		
		// clear cache in case this is an updated map
		CacheManager.removeMap(source);
	}
	public void finishCustomMapEdit(String filename, String newName, String citation, double[] ignoreValues) {
	    MapSource source = getSourceByName(filename);
        if (source != null) {
            if (ignoreValues != null) {
                source.setIgnoreValue(ignoreValues);
            }
            if (source instanceof WMSMapSource) {
              WMSMapSource temp = (WMSMapSource) source;
              temp.setTitle(newName);
              temp.setAbstractText(citation);
            }

            loadCapabilities(false);
            CacheManager.removeMap(source);
        }
	}
	public void loadCapabilities() throws Exception {
	    Thread.sleep(2000);
	    loadCapabilities(false);
	}
	public void finishDeleteExistingMap(String mapName) throws Exception {
	    MapSource source = getSourceByName(mapName);
        if (source != null) {
            CacheManager.removeMap(source);
//            if (getSourceByName(source.getName()) != null) {
//                throw new Exception("Custom map removal succeeded but it is still found!");
//            }
        }
	}
	public void finishCustomMapManagerUpload(String mapName) throws Exception {
        MapSource source = getSourceByName(mapName);
        if (source == null) {
            throw new Exception(
                    "Upload succeeded but custom map cannot be found with name " +
                    mapName);
        }
        // clear cache in case this is an updated map
        CacheManager.removeMap(source);
	}
	private void addFile(final JmarsHttpRequest request, final File localFile , final String remoteName){
		// construct a custom FilePart that will cause 'localName' to be named
		// 'remoteName' on the server
//		List<Part> parts = new ArrayList<Part>();
//		for (NameValuePair parm: post.getParameters()) {
//			parts.add(new StringPart(parm.getName(), parm.getValue()));
//		}
//		parts.add(new FilePart(remoteName, new PartSource() {
//			public InputStream createInputStream() throws IOException {
//				return new FileInputStream(localFile);
//			}
//			public String getFileName() {
//				return remoteName;
//			}
//			public long getLength() {
//				return localFile.length();
//			}
//		}));
        request.addUploadFile(remoteName, localFile);
        request.setRetryNever();
//		post.setRequestEntity(new MultipartRequestEntity(parts.toArray(new Part[0]), post.getParams()));
//		
		// construct a retry handler that will never retry
//		post.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new HttpMethodRetryHandler() {
//			public boolean retryMethod(HttpMethod method, IOException exception, int executionCount) {
//				return false; 
//			}
//		});
	}
	
	private String read(JmarsHttpRequest request) {
		try {
//			int code = Util.postWithRedirect(client, post, 3);
//			client.getHttpConnectionManager().getParams().setConnectionTimeout(getTimeout());
            request.setConnectionTimeout(getTimeout());
//            request.setLaxRedirect();
            boolean successful = request.send();
		    if (successful) {
				return Util.readResponse(request.getResponseAsStream());
			} else {
				return "ERROR: Unexpected HTTP code " + request.getStatus() + " received: "+EnglishReasonPhraseCatalog.INSTANCE.getReason(request.getStatus(), Locale.ENGLISH);
			}
		} catch (Exception e) {
			log.aprintln(e);
			return "ERROR: " + e.getMessage();
		} finally {
			if (request != null) {
				request.close();
			}
		}
	}
	
	/**
	 * Removes the given custom map. A message dialog will show any errors to the user.
	 * @param name The canonic name of the map to remove.
	 * @throws Exception 
	 */
	public void deleteCustomMap(String name) throws Exception {
		log.println("Deleting custom map named " + name);
		MapSource source = getSourceByName(name);
		if (source == null) {
			throw new Exception("No map source with the name " + name + " was found");
		}
		JmarsHttpRequest request = getMethod("delete");
		request.addRequestParameter("names", name);
		String response = read(request);
		if (!response.startsWith("OK:")) {
			log.println("Delete of map failed with " + response);
			throw new Exception(response);
		} else {
			log.println("Removal succeeded");
			CacheManager.removeMap(source);
			loadCapabilities(false);
			if (getSourceByName(source.getName()) != null) {
				throw new Exception("Custom map removal succeeded but it is still found!");
			}
		}
	}
	
	/**
	 * Gets all the users the custom map(s) is currently shared with, excluding the owner.
	 * @param allUsers A distinct list of all the users.
	 * @param mapSources All the maps the shared users are requested for.
	 * @return a List of the shared maps which will include the users associated with each map.
	 * @throws Exception 
	 */
	 // TODO: This class and others need some serious refactoring in a future release
	public List<SharedMap> getSharedMapsUsers(TreeMap<String, SharedUser> allUsers, HashMap<String, MapSource> mapSources) throws Exception {
		ArrayList<SharedMap> maps = new ArrayList<SharedMap>();
		JmarsHttpRequest request = getMethod("shared_map_info");
		request.addRequestParameter("user", user);
		for(MapSource ms : mapSources.values()) {
			request.addRequestParameter("map_ids[]", ms.getName());
		}		
		String response = read(request);
		log.println("shared_map_info " + response);
		try {			
			JSONObject respObject = new JSONObject(response);
			JSONObject statusObject = respObject.getJSONObject("overall_status");
			String oaStatus = statusObject.getString("status");
			
			if (oaStatus != null) {
				if (oaStatus.toLowerCase().contains("failure")) {
					throw new Exception(oaStatus);
				} else if (oaStatus.toLowerCase().contains("error")) {
					throw new Exception(oaStatus);					
				}
			}
						
			JSONObject msgObject = respObject.getJSONObject("shared_map_info");
			String [] mapNames = JSONObject.getNames(msgObject);
			for (String mapName : mapNames) {
				JSONObject indMap = msgObject.getJSONObject(mapName);
				String [] sharedUsers = JSONObject.getNames(indMap);
				SharedMap map = new SharedMap(mapName);
				if (sharedUsers != null) {
					for (String aUser: sharedUsers) {
						JSONObject jUser = indMap.getJSONObject(aUser);
						String type = jUser.getString("type");
						SharedUser usr = new SharedUser(aUser);
						if (type.equalsIgnoreCase("individual")) {
							usr.setGroup(false);
						} else if (type.equalsIgnoreCase("group")) {
							usr.setGroup(true);
						}
						map.addUser(usr);
						// create a unique (no duplicates) list of users for the list of maps
						allUsers.put(aUser, usr);
					}
				}
				maps.add(map);
			}
		} catch (Exception e) {
			e.printStackTrace();
			maps.clear();
			log.println(e.getMessage());
			throw new Exception("An error has occurred processing your request. If this happens repeatedly, check the JMARS log in the help menu and/or contact JMARS help.");
		}

		return maps;
	}
	
	/**
	 * Ads or Removes a list of shared users from a list of custom maps.
	 * @param users A distinct list of all the users to add or remove sharing.
	 * @param maps All the maps the shared users are to be added to or removed from.
	 * @param share A flag to indicate if the users are to be shared or unshared.
	 * @return a List of all the users for which the sharing/unsharing operation failed.
	 * @throws Exception 
	 */
	 // TODO: This class and others need some serious refactoring in a future release
	public List<String> addRemoveSharedUsers(HashMap<String, SharedUser> users, HashMap<String, MapSource> maps, boolean share) {
		ArrayList<String> reqStatus = new ArrayList<String>();
		// use the "share" variable to determine if we should share or unshare
		JmarsHttpRequest request = getMethod((share ? "" : "un") + "share_custom_map");
		request.addRequestParameter("user", user);
		
		for (MapSource mapSrc : maps.values()) {
		    request.addRequestParameter("map_ids[]", mapSrc.getName());
		}
		
		for (SharedUser usr : users.values()) {
		    request.addRequestParameter((share ? "" : "un") + "shared_with_individuals[]", usr.getUserName());
		}
		
		String response = read(request);
		
		log.println((share ? "" : "un") + "share_custom_map "+response);		
		
		try {
			boolean totalSuccess = true;
			JSONObject respObject = new JSONObject(response);
			JSONObject statusObject = respObject.getJSONObject("overall_status");
			String oaStatus = statusObject.getString("status");
			
			if (oaStatus != null) {
				if (oaStatus.toLowerCase().contains("failure")) {
					reqStatus.add(oaStatus);
					return reqStatus;
				} else if (oaStatus.toLowerCase().contains("error")) {
					totalSuccess = false;
					reqStatus.add(oaStatus+"\n\n");
				}
			}
			
			JSONObject msgObject = respObject.getJSONObject((share ? "" : "un") + "shared_map_info");
			String [] mapNames = JSONObject.getNames(msgObject);
			for (String mapName : mapNames) {
				boolean mapSuccess = true;
				StringBuffer responseMsg = new StringBuffer();
				responseMsg.append(maps.get(mapName).getTitle()+"\n");
				JSONObject indMap = msgObject.getJSONObject(mapName);
				String [] sharedUsers = JSONObject.getNames(indMap);
				for (String aUser: sharedUsers) {
					JSONObject jUser = indMap.getJSONObject(aUser);
					String status = jUser.getString("status"); 
					// we had an error on this user
					if (!status.equalsIgnoreCase("success")) {
						totalSuccess = false;
						mapSuccess = false;
						responseMsg.append("  " + status + "\n");
					} else {
						if (share) {
							users.get(aUser).setFullyShared(true);
						} else {
							users.get(aUser).setUnshared(true);
						}
					}
				}
				if (!mapSuccess) {
					reqStatus.add(responseMsg.toString()+"\n");
				}
			} 
			// don't return all the returned status if everything went as planned
			if (totalSuccess) {
				reqStatus.clear();
				reqStatus.add("Success!");
			}
		} catch (Exception e) {
			e.printStackTrace();
			reqStatus.clear();
			reqStatus.add("An error has occurred processing your request. If this happens repeatedly, check the JMARS log in the help menu and/or contact JMARS help.");
			log.println(e.getMessage());
		}		
		return reqStatus;
	}

	/**
	 * Gets all the groups the owner has created for the domain they are currently logged into.
	 * Sample JSON group object returned from the custom map server:
	 * {"overall_status":{"status":"success"},"group_admin_group_info":{"test_group":{"domain":"msff"}}}
	 * Sample JSON user object returned from the custom map server:
	 * {"overall_status":{"status":"success"},"group_admin_group_membership":{"joe":{"domain":"msff"},
	 * 		"bob":{"domain":"msff"},"betty":{"domain":"msff"}}}
	 * @param allUsers - output, A distinct list of all the users for all the owners groups.
	 * @return a TreeMap of the groups which will include the users associated with each group.
	 * @throws Exception 
	 */
	
	public TreeMap<String, SharedGroup> getSharedGroupsUsers(TreeMap<String, SharedUser> allUsers) throws Exception {
		TreeMap<String, SharedGroup> groups = new TreeMap<String, SharedGroup>();
		JmarsHttpRequest request = getMethod("group_admin_group_info");
		request.addRequestParameter("user", user);
		String response = read(request);
		log.println("group_admin_group_info " + response);
		
		try {			
			JSONObject respObject = new JSONObject(response);
			JSONObject statusObject = respObject.getJSONObject("overall_status");
			String oaStatus = statusObject.getString("status");
			
			if (oaStatus != null) {
				if (oaStatus.toLowerCase().contains("failure") ||
						oaStatus.toLowerCase().contains("error")) {
					throw new Exception(oaStatus);
				}			
			}
						
			JSONObject msgObject = respObject.getJSONObject("group_admin_group_info");
			String [] groupNames = JSONObject.getNames(msgObject);
			if (groupNames != null) {
				for (String groupName : groupNames) {
					JSONObject indDomain = msgObject.getJSONObject(groupName);
					String domain = indDomain.getString("domain");
					SharedGroup group = new SharedGroup(groupName, domain);
					JmarsHttpRequest usrPost = getMethod("group_admin_group_membership");
					usrPost.addRequestParameter("user", user);
					usrPost.addRequestParameter("group_name", groupName);
					String res = read(usrPost);
					JSONObject memObject = new JSONObject(res);
					JSONObject statusObj = memObject.getJSONObject("overall_status");
					String oaStat = statusObj.getString("status");
					
					if (oaStat != null) {
						if (oaStat.toLowerCase().contains("failure") ||
								oaStat.toLowerCase().contains("error")) {
							throw new Exception(oaStat);
						}			
					}
					
					JSONObject usrMsgObject = memObject.getJSONObject("group_admin_group_membership");
					String [] userNames = JSONObject.getNames(usrMsgObject);
					if (userNames != null) {
						for (String userName : userNames) {
							JSONObject usrDomain = usrMsgObject.getJSONObject(userName);
							String dom = usrDomain.getString("domain");
							SharedUser user = new SharedUser(userName, dom);
							user.setGroup(false);
							user.setFullyShared(false);
							group.addUser(user);
							allUsers.put(userName, new SharedUser(userName, dom));
						}
					}
					groups.put(group.getGroupName(), group);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			groups.clear();
			log.println(e.getMessage());
			throw new Exception("An error has occurred processing your request. If this happens repeatedly, check the JMARS log in the help menu and/or contact JMARS help.");
		}

		return groups;
	}
		
	/**
	 * Adds or Removes a list of shared users from a list of groups.
	 * @param members A distinct list of all the users to add or remove.
	 * @param groups All the groups the users are to be added to or removed from.
	 * @param add A flag to indicate if the users are to be added or removed.
	 * @return a List of all the users for which the requested operation failed.
	 * @throws Exception 
	 */
	public List<String> addRemoveGroupMembers(HashMap<String, SharedUser> members, TreeMap<String, SharedGroup> groups, boolean add) {
		ArrayList<String> reqStatus = new ArrayList<String>();
		// use the "add" parameter to determine if we should add or delete
		JmarsHttpRequest request = getMethod("group_admin_" + (add ? "add" : "delete") + "_users");
		request.addRequestParameter("user", user);
		
		for (SharedUser usr : members.values()) {
		    request.addRequestParameter("users_to_" + (add ? "add" : "delete") + "[]", usr.getUserName());
		}
		
		for (SharedGroup grp : groups.values()) {
		    request.addRequestParameter("groups_to_" + (add ? "add_to" : "delete_from") + "[]", grp.getGroupName());
		}
		
		String response = read(request);
		
		log.println((add ? "add" : "delete") + " group members "+response);		
		
		try {
			boolean totalSuccess = true;
			JSONObject respObject = new JSONObject(response);
			JSONObject statusObject = respObject.getJSONObject("overall_status");
			String oaStatus = statusObject.getString("status");
			
			if (oaStatus != null) {
				if (oaStatus.toLowerCase().contains("failure")) {
					reqStatus.add(oaStatus);
					return reqStatus;
				} else if (oaStatus.toLowerCase().contains("error")) {
					totalSuccess = false;
				}
			}
			
			JSONObject msgObject = respObject.getJSONObject("group_admin_info");
			String [] grpNames = JSONObject.getNames(msgObject);
			for (String grpName : grpNames) {
				JSONObject indMap = msgObject.getJSONObject(grpName);
				String [] sharedUsers = JSONObject.getNames(indMap);
				for (String aUser: sharedUsers) {
					boolean success = true;
					JSONObject jUser = indMap.getJSONObject(aUser);
					String status = jUser.getString("status"); 
					// we had an error on this user
					if (!status.equalsIgnoreCase("success")) {
						totalSuccess = false;
						success = false;
					} else {
						if (add) {
							members.get(aUser).setFullyShared(true);
							groups.get(grpName).addUser(members.get(aUser));
						} else {
							members.get(aUser).setUnshared(true);
							groups.get(grpName).removeUser(aUser);
						}
					}
					reqStatus.add((success ? "Success" : "Error") + " : " + aUser + " was "
							+ (success ? "" : "not ")
							+ (add ? "added to the " : "deleted from the ") + grpName
							+ " group. " + (status.equalsIgnoreCase("success") ? "" : status.substring(status.indexOf(':') + 2)) + "\n");
				}
			} 
			// don't return all the returned status if everything went as planned
			if (totalSuccess) {
				reqStatus.clear();
				reqStatus.add("The member " + (add ? "addition" : "deletion") + " completed successfully");
			}
		} catch (Exception e) {
			e.printStackTrace();
			reqStatus.clear();
			reqStatus.add("An error has occurred processing your request. If this happens repeatedly, check the JMARS log in the help menu and/or contact JMARS help.");
			log.println(e.getMessage());
		}		
		return reqStatus;
	}
	
	/**
	 * Ads or Removes a list of groups to their associated domain.
	 * @param groups All the groups to be added to or removed.
	 * @param add A flag to indicate if the groups are to be added or removed.
	 * @return a List of all the groups for which the requested operation failed.
	 * @throws Exception 
	 */
	public List<String> addRemoveGroups(TreeMap<String, SharedGroup> groups, boolean add) {
		ArrayList<String> reqStatus = new ArrayList<String>();
		// use the "add" parameter to determine if we should add or delete
		JmarsHttpRequest request = getMethod("group_admin_" + (add ? "add" : "delete") + "_groups");
		request.addRequestParameter("user", user);
		
		for (SharedGroup grp : groups.values()) {
		    request.addRequestParameter("groups_to_" + (add ? "add" : "delete") + "[]", grp.getGroupName());
		}
		
		String response = read(request);

		log.println((add ? "add" : "delete") + " groups "+response);		
		
		try {
			boolean totalSuccess = true;
			JSONObject respObject = new JSONObject(response);
			JSONObject statusObject = respObject.getJSONObject("overall_status");
			String oaStatus = statusObject.getString("status");
			
			if (oaStatus != null) {
				if (oaStatus.toLowerCase().contains("failure")) {
					reqStatus.add(oaStatus);
					return reqStatus;
				} else if (oaStatus.toLowerCase().contains("error")) {
					totalSuccess = false;
					reqStatus.add(oaStatus+"\n\n");
				}
			}
			
			JSONObject msgObject = respObject.getJSONObject("group_admin_info");
			String [] grpNames = JSONObject.getNames(msgObject);
			for (String grpName : grpNames) {
				boolean success = true;
				JSONObject gStatus = msgObject.getJSONObject(grpName);
				String status = gStatus.getString("status"); 				
				// we had an error on this group
				if (!status.equalsIgnoreCase("success")) {
					totalSuccess = false;
					success = false;
					groups.remove(grpName);
				}
				reqStatus.add(" : The Group " + grpName + " was " + (success ? "" : "not ") + (add ? "added" : "deleted") + " : " + status + "\n");
			} 
			// don't return all the returned statuses if everything went as planned
			if (totalSuccess) {
				reqStatus.clear();
				reqStatus.add("The group " + (add ? "addition" : "deletion") + " completed successfully");
			}
		} catch (Exception e) {
			e.printStackTrace();
			reqStatus.clear();
			reqStatus.add("An error has occurred processing your request. If this happens repeatedly, check the JMARS log in the help menu and/or contact JMARS help.");
			log.println(e.getMessage());
		}		
		return reqStatus;
	}
	
	public static String getServiceVersion() {
		return SERVICE_VERSION;
	}

	public static String getVersionNumber() {
		return VERSION_NUMBER;
	}	
}
