/**
 * 
 */
package edu.asu.jmars.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.swing.JOptionPane;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.stamp.StampLayer;

/**
 * TODO: Comment this!
 */
public class JmarsHttpRequest {

    private static final int REQUEST_NONE               = 0;
    private static final int REQUEST_SIMPLE             = 1;
    private static final int REQUEST_CONDITIONAL        = 2;
    private static final int REQUEST_AUTH_REQUIRED      = 3;
    private static final int REQUEST_COND_AUTH_REQUIRED = 4;

    int             kindOfRequest = REQUEST_NONE;
    String          targetHost = null;
    int             targetPort = 0;
    boolean         alreadySent = false;

    HttpRequestType requestMethod = null;
    
	String          requestUrl = null;
    String          user = null;
    String          pass = null;
    Date            sinceDate = null;

    boolean customRequestConfig = false;
    boolean browserCompatibleCookiePolicy = false;
    int     connectionTimeout = 0;
    int     readTimeout = 0;
    int     maxConnectionsPerHost = 0;
    boolean retryNever = false;
    HttpVersion httpVersion = HttpVersion.HTTP_1_1;
    MultipartEntityBuilder postEntityBuilder = null;
    boolean laxRedirect = false;
    
    int             httpCode = 0;
    HttpUriRequest  httpRequest = null;
    CloseableHttpClient httpClient = null;
    CloseableHttpResponse httpResponse = null;
    HttpEntity      entity  = null;
    String          content = null;
    String          outputEntityData = null;
    private boolean isMapServerRequest = false;
    private Exception generatedException = null;
    private static volatile boolean messageLogged = false;
    
    ArrayList<BasicNameValuePair> headers = new ArrayList<BasicNameValuePair>();
    ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();

    private DebugLog log = DebugLog.instance(); // for logging errors
    private static boolean failedConnectionMessageDisplayedFlag = false;
    
    private boolean testConnectionFlag = false;
    private static volatile boolean connectionFailed = false;//volatile to make sure each process reads the value from main memory, not processor cache
	
    
    //****************************** CONSTRUCTORS ***************************************** TODO: Comments here, too
    
    public JmarsHttpRequest(String Url, HttpRequestType request) {
        
        this.requestMethod = request;
        this.requestUrl    = Url;
        this.kindOfRequest = REQUEST_SIMPLE;
        
    } // Constructor - no server authentication


   public JmarsHttpRequest(String Url, HttpRequestType request, Date ifModifiedSince) {
        
        this.requestMethod = request;
        this.requestUrl    = Url;
        this.sinceDate     = ifModifiedSince;
        
        this.kindOfRequest = REQUEST_CONDITIONAL;
       
    } // Constructor - Conditional GET

    public JmarsHttpRequest(String Url, HttpRequestType request, String targetUsername, String targetPassword) {
        
        this.requestMethod = request;
        this.requestUrl    = Url;
        this.user          = targetUsername;
        this.pass          = targetPassword;
        
        this.kindOfRequest = REQUEST_AUTH_REQUIRED;
               
    } // Constructor - with server authentication

   public JmarsHttpRequest(String Url, HttpRequestType request, Date ifModifiedSince, String targetUsername, String targetPassword) {
        
        this.requestMethod = request;
        this.requestUrl    = Url;
        this.sinceDate     = ifModifiedSince;
        this.user          = targetUsername;
        this.pass          = targetPassword;
        
        this.kindOfRequest = REQUEST_COND_AUTH_REQUIRED;
       
    } // Constructor - Conditional GET with server authentication

  
   
    //******************************* PUBLIC METHODS ********************************* 
   public Exception getGeneratedException() {
	   return this.generatedException;
   }
   public void setIsMapServerRequest(boolean flag) {
	   this.isMapServerRequest = flag;
   }
   public HttpRequestType getRequestMethod() {
       return requestMethod;
   }

   public String getRequestUrl() {
       return requestUrl;
   }

   	public void setRequestHeader(String name, String value) {
   		headers.add(new BasicNameValuePair(name, value));
   	}

    public void setConnectionTimeout(int timeoutInMilliseconds) {
        connectionTimeout = timeoutInMilliseconds;
        customRequestConfig = true;
    }

    public void setReadTimeout(int timeoutInMilliseconds) {
        readTimeout = timeoutInMilliseconds;
        customRequestConfig = true;
    }

    public void setMaxConnections(int connections) {
        maxConnectionsPerHost = connections;
        customRequestConfig = true;
        /*
         * TODO: This method is fine, but need to do something with it below.  Look at HttpClientBuilder and
         * see if I should create an HttpClientConnectionManager toset these items and then pass it to
         * HttpClientBuilder.setConnectionManager().
         */
    }

    public void setBrowserCookies() {
        browserCompatibleCookiePolicy = true;
        
        /*
         * TODO: need to implement this below.  HttpClient 4.3 version of
         *       client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
         */
    }

    public void setProtocolVersion(HttpVersion version) {
        httpVersion = version;
    }
    
    public void setRetryNever() {
        retryNever = true;
    }
 
    public void setLaxRedirect() {
        laxRedirect = true;
    }
    
    public void addRequestParameter(BasicNameValuePair nvPair) {                 // TODO (PW) Consider making it work for GETs too
    	params.add(nvPair);
    }

    public void addRequestParameter(String name, String value) {                 // TODO (PW) Consider making it work for GETs too
        params.add(new BasicNameValuePair(name, value));
    }
//    http://jfarcand.wordpress.com/2010/12/21/going-asynchronous-using-asynchttpclient-the-basic/

    public void addUploadFile(String name, String mimeType, File file ) {
        if (postEntityBuilder == null) {
            postEntityBuilder = MultipartEntityBuilder.create();                 // TODO (PW) some examples use builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
//            postEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        }
        ContentBody cbFile = new FileBody(file, ContentType.create(mimeType));
        postEntityBuilder.addPart(name, cbFile);
     }
    public void addUploadFilePart(String typeName, String filename, File file) {
        if (postEntityBuilder == null) {
            postEntityBuilder = MultipartEntityBuilder.create();                 // TODO (PW) some examples use builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
//            postEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        }
//        ContentBody cbFile = new FileBody(file);
        postEntityBuilder.addPart(typeName, new FileBody(file, ContentType.APPLICATION_OCTET_STREAM, filename));
    }
    public void addUploadFileNew(String name, File file) {
        this.addUploadFilePart("userfile",name,file);
     }
    
    public void addUploadFile(String name, File file) {
        if (postEntityBuilder == null) {
            postEntityBuilder = MultipartEntityBuilder.create();                 // TODO (PW) some examples use builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
//            postEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        }
//        ContentBody cbFile = new FileBody(file);
        postEntityBuilder.addPart(name, new FileBody(file, ContentType.APPLICATION_OCTET_STREAM, name));
     }
    
    public void addUploadString(String name, String text ) {                     // TODO (PW) Can this be replaced by addRequestParameter()?
        if (postEntityBuilder == null) {
            postEntityBuilder = MultipartEntityBuilder.create();
        }
        postEntityBuilder.addPart(name, new StringBody(text, ContentType.MULTIPART_FORM_DATA));
     }
    
    public void addUTF8String(String name, String text) {
    	if (text == null) {
    		text = "";
    	}
    	if (postEntityBuilder == null) {
            postEntityBuilder = MultipartEntityBuilder.create();
        }
        postEntityBuilder.addPart(name, new StringBody(text, ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8)));
    }
 
    public void addOutputData(String data) {
        outputEntityData = data;
    }
    
    public void sendAsync(AsyncCompletionHandler<Response> callback) throws IOException {
    	AsyncHttpClient client = new AsyncHttpClient();

    	BoundRequestBuilder build = null;
    	switch (requestMethod) {
    	case GET: {
    		build = client.prepareGet(requestUrl);
    		break;
    	}
    	case POST: {
    		build = client.preparePost(requestUrl);
    		break;
    	}
    	}
    	
    	// TODO: This works for our purposes now, but should be retooled if we ever have to do anything more sophisticated asynchronously.
    	
    	build.execute(callback);
    }
    private void setDisconnectedStatus() {
    	//we are not disconnecting for map server requests, they need to be able to retry
    	if (!this.isMapServerRequest) {
    		if (!messageLogged) {
    			log.aprintln("Failed request. Entering failed connection mode. Reconnect will happen as soon as possible.");
    			messageLogged = true;
    		}
	        connectionFailed = true;
	    	ConnectionCheck.startThread();
    	} 
    }
    public boolean send() throws URISyntaxException {

    	if (connectionFailed && !testConnectionFlag) {//if we are not connected, and this is a real request, don't send the request
    		//This is where we know we have a connectivity problem based on test results
    		//from the network settings at startup or under options.
    		//Allowing the process to continue, when we know we have problem, makes the application hang for the user.
    		//@see ConnectionCheck which is a thread to reset the flag when the connection is re-established.

        	ConnectionCheck.startThread();
    		return false;
    	}
        boolean result   = false;
        boolean done     = false;
        boolean lastTime = false;
        
        // Create the request object.  Add conditional retrieval if present.
        httpRequest = getHttpRequest(this.requestUrl, this.requestMethod);

        if (this.kindOfRequest == REQUEST_CONDITIONAL || this.kindOfRequest == REQUEST_COND_AUTH_REQUIRED) {
            this.httpRequest.setHeader("If-Modified-Since", DateUtils.formatDate(this.sinceDate));           
        }
        
        while (!done) {
 
            if (lastTime) {
                done = true;
            }
                
            // Create the HTTP Client
            httpClient = getHttpClient();

            try { 
                httpResponse = httpClient.execute(httpRequest);    

                httpCode = httpResponse.getStatusLine().getStatusCode();
                switch (httpCode) {
                    case HttpStatus.SC_OK:
    
                        entity = httpResponse.getEntity();
                       
                        done = true;
                        result = true;
                        break;
    
                    case HttpStatus.SC_NOT_MODIFIED:
                        this.log.println("JmarsHttpRequest: File has not been modified since last retrieved.");
                        done = true;
                        result = true;
                        this.close();
                        break;
                    
                    case HttpStatus.SC_UNAUTHORIZED:
                        this.log.println("JmarsHttpRequest: You are not authorized to access the specified server.");
                        done = true;
                        this.close();
                        break;
                    
                    case HttpStatus.SC_PROXY_AUTHENTICATION_REQUIRED:
                        this.log.println("JmarsHttpRequest: Need to specfy proxy password/username");
                        displayProxyCredentialsDialog();
                        lastTime = true;
                        break;
                        
                    case HttpStatus.SC_TEMPORARY_REDIRECT:
                    case HttpStatus.SC_MOVED_TEMPORARILY:
                    case HttpStatus.SC_MOVED_PERMANENTLY:
                    	this.log.println("JmarsHttpRequest: Redirecting");
                    	Header loc = httpResponse.getHeaders("location")[0];
                    	this.log.println(loc.getValue());
                    	httpRequest = getHttpRequest(loc.getValue(), this.requestMethod);
                    	break;
                    	
                    default:
                        this.log.print("JmarsHttpRequest: unexpected HTTP code: ");
                        this.log.println(httpCode);
                        for (StackTraceElement elem : Thread.currentThread().getStackTrace()) {
                            this.log.println(elem);
                        }
                        done = true;
                        this.close();
                        break;
                }
                                
            } catch (Exception e) {
            	this.generatedException = e;
                this.log.println("An Exception was thrown for URL: "+this.requestUrl);
                if (e instanceof HttpHostConnectException ||
                    e instanceof UnknownHostException) {
                    this.log.println("JmarsHttpRequest: Connection refused by server: "+e.toString());
                    this.httpCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
                    displayProxyInfoDialog();
                    setDisconnectedStatus();

                } else if (e instanceof SocketTimeoutException) {
                    this.log.println("JmarsHttpRequest: The connection timed out: "+e.getMessage());
                    this.log.println("Exception name:"+e.toString());
                    this.httpCode = HttpStatus.SC_REQUEST_TIMEOUT;
                    setDisconnectedStatus();
               	
                	
                } else {
                    this.log.println("JmarsHttpRequest: An unexpected exception was thrown: "+e.getMessage());
                    this.log.println("Exception name:"+e.toString());
                    this.httpCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
                    setDisconnectedStatus();
                }
                done = true;
                    
                // one last try to close the client, just in case
                if (httpClient != null) {
                    try {
                        httpClient.close();
                    } catch (IOException f) {
                        this.log.println("JmarsHttpRequest: got an IOException-- couldn't close the client.");
                    }
                }
            } // catch
  
        } // While not done
    
        return result;

    } // send()
    
    public void abort() {
        httpRequest.abort();
        this.close();
    }
    
    public int getStatus() {
        
        return this.httpCode;
    }
    
    public long getContentLength() {
    	if (this.entity != null) {
      	    return this.entity.getContentLength();
    	} else {
    		return -1;
    	}
    }
    
    public String getContentType() {
        
    	if (this.entity != null) {
            return this.entity.getContentType().getValue();
    	} else {
    		return "ERROR";
    	}
    }
    
    public InputStream getResponseAsStream() throws IOException {
        
    	if (this.entity != null) {
            return this.entity.getContent();
    	} else {
    		return null;
    	}
    }


    public Header getLastModifiedHeader() {
        
        Header lastModifiedHeader = null;
        Header[] headers = httpResponse.getHeaders("last-modified");
        if (headers[0] != null) {
            lastModifiedHeader = headers[0];
        }
        return lastModifiedHeader;
    }

   public String getLastModifiedString() {
        
       String dateString = null;
        Header lastModifiedHeader = null;
        Header[] headers = httpResponse.getHeaders("last-modified");
        if (headers[0] != null) {
            lastModifiedHeader = headers[0];
            dateString = lastModifiedHeader.getValue();
        }
        return dateString;
    }

   public long getLastModifiedDate() {
       
       long dateModified = 0;
       Header lastModifiedHeader = null;
       Header[] headers = httpResponse.getHeaders("last-modified");
       if (headers[0] != null) {
           lastModifiedHeader = headers[0];
           dateModified = DateUtils.parseDate(lastModifiedHeader.getValue()).getTime();
       }
       return dateModified;
   }

   
    public void close() {

        try {
            if (entity != null) {
            	EntityUtils.consume(entity);
            }
        }
        catch (IOException e){
            this.log.println("JmarsHttpRequest: Error consuming entity on close().");
        }
        
        try {
        	if (httpResponse != null) {
        		httpResponse.close();
        	}
        }
        catch (IOException e){
            this.log.println("JmarsHttpRequest: Error closing http response.");
        }

        try {
        	if (httpClient != null) {
        		httpClient.close();
        	}
        }
        catch (IOException e){
            this.log.println("JmarsHttpRequest: Error closing http client.");
        }                      // TODO (PW) Crap.  Can't use this, cuz posts and gets R different.

    }
    public void setTestConnectionFlag(boolean flag) {
    	testConnectionFlag = flag;
    }
    public static boolean getConnectionFailed() {
		return connectionFailed;
	}
//	public static void setConnectionFailed(boolean failed) {
//		connectionFailed = failed;
//		ConnectionCheck.startThread();
//	}
    
    //****************************** LOCAL METHODS ***********************************
    
    /**
     * Builds and returns a URI request object given the URL and a request method
     * 
     * @param url                 The url string from which the request is created
     * @param methodType          The type of http request method (GET or POST only)
     * @return                    the URI request object that can be executed by an HttpClient
     * @throws URISyntaxException 
     * @throws UnsupportedEncodingException 
     */
    private HttpUriRequest getHttpRequest(String url, HttpRequestType methodType) throws URISyntaxException {
        
        HttpUriRequest request = null;
        URI requestUri = new URI(url);
        this.targetHost = requestUri.getHost();
        this.targetPort = requestUri.getPort();
        
        switch (methodType) {
            case GET:
                URIBuilder b = null;
                if (this.outputEntityData != null) {
                    b = new URIBuilder(url + "?" + this.outputEntityData);
                } else {
                    b = new URIBuilder(url);
                    for (BasicNameValuePair nv : params) {
                        b.addParameter(nv.getName(), nv.getValue());
                    }                    
                }
                request = new HttpGet(b.build());
                for (BasicNameValuePair nv : headers) {
                	request.addHeader(nv.getName(), nv.getValue());
                }
                
                break;
            case POST:
                HttpPost post = new HttpPost(requestUri);
                for (BasicNameValuePair nv : headers) {
                	post.addHeader(nv.getName(), nv.getValue());
                }
                if (postEntityBuilder != null) {
                	for (BasicNameValuePair nv : params) {
                		postEntityBuilder.addTextBody(nv.getName(), nv.getValue());
                	}
                	postEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                    post.setEntity(postEntityBuilder.build());
                    post.setProtocolVersion(this.httpVersion);
                } else if (outputEntityData != null) {
                    try {
                        post.setEntity(new StringEntity(outputEntityData));                        
                    } catch (UnsupportedEncodingException e) {
                        this.log.println("JmarsHttpRequest: Unable to encode output string on POST.");                        
                    }
                } else {
                	try {
						post.setEntity(new UrlEncodedFormEntity(params));
					} catch (UnsupportedEncodingException e) {
						this.log.println("JmarsHttpRequest: Unable to encode form data on POst.");
					}
                }
                request = post;
                break;
            default:
                this.log.println("JmarsHttpRequest: Invalid http method request.");
                break;
        }
 
        return request;
        
    } // getHttpRequest()

    
    /**
     * Constructs an http client, and only enables proxy routing if necessary. The
     * caller need never know.
     * 
     * @return an HttpClient that can be used to request files over the Internet. Should be closed via close() method.
     */
    private CloseableHttpClient getHttpClient() {
     
        CredentialsProvider credsProvider = new BasicCredentialsProvider();        
        HttpClientBuilder   clientBuilder = HttpClientBuilder.create();
        boolean anyAuthenticationRequired = false;
 
        ProxyInformation proxyInfo = ProxyInformation.getInstance();
        if (proxyInfo.isProxyUsed() && proxyInfo.isProxySet()) {
            clientBuilder.setProxy(new HttpHost(proxyInfo.getHost(), proxyInfo.getPort()));

            // Use proxy authentication if required
            if (proxyInfo.isAuthenticationUsed()) {
                anyAuthenticationRequired = true;
                Credentials proxyCredentials = null;
                if (proxyInfo.isNtlmUsed()) {
                    proxyCredentials = new NTCredentials(proxyInfo.getUsername(), proxyInfo.getPassword(), proxyInfo.getHost(), proxyInfo.getNtlmDomain());
                } else {
                    proxyCredentials = new UsernamePasswordCredentials(proxyInfo.getUsername(), proxyInfo.getPassword());                
                }            
                credsProvider.setCredentials( new AuthScope(proxyInfo.getHost(), proxyInfo.getPort()), proxyCredentials);
                clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
            }            
             
        } // if proxy is used
        
        if (kindOfRequest == REQUEST_AUTH_REQUIRED || kindOfRequest == REQUEST_COND_AUTH_REQUIRED) {
            // Add the target credentials
            anyAuthenticationRequired = true;
            UsernamePasswordCredentials targetCredentials = new UsernamePasswordCredentials(this.user, this.pass);
            credsProvider.setCredentials(new AuthScope(this.targetHost, this.targetPort), targetCredentials);                
        }

        if (anyAuthenticationRequired) {
            clientBuilder.setDefaultCredentialsProvider(credsProvider);           
        }

        if (laxRedirect) {
            clientBuilder.setRedirectStrategy(new LaxRedirectStrategy());
        }
        
        if (retryNever) {
            clientBuilder.setRetryHandler(new HttpRequestRetryHandler() {
                public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                    return false; 
                }
            });     
        }
        
        // If custom request configuration is required
        if (customRequestConfig) {
            RequestConfig.Builder requestBuilder = RequestConfig.custom();
            if (maxConnectionsPerHost > 0) {
                // requestBuilder.hmm TODO
            }
            if (connectionTimeout > 0) {
                /*
                 * Do I want to call setConnectTimeout() or setConnectionRequestTimeout()?
                 * Or should this be set on the CloseableHttpClient via the HttpClientBuilder?
                 * Oooooh! Good example code, see "configure timeout on the entire client"
                 * at http://www.baeldung.com/httpclient4
                 */
                requestBuilder.setConnectTimeout(connectionTimeout);
            }
            if (readTimeout > 0) {
                requestBuilder.setSocketTimeout(readTimeout);
            }
            clientBuilder.setDefaultRequestConfig(requestBuilder.build());
        }
        
        return clientBuilder.build();
        
    } // getHttpClient()
    
    
    private void displayProxyInfoDialog() {
        
//        JTextField proxyHost = new JTextField();
//        JTextField proxyPort = new JTextField();
//        final JComponent[] inputs = new JComponent[] {
//                new JLabel("Error contacting JMARS servers.  Are you behind a proxy?"),
//                new JLabel(" "),
//                new JLabel("Proxy server IP address"),
//                proxyHost,
//                new JLabel("Proxy server port number"),
//                proxyPort
//        };
//        Util.showMessageDialog(null, inputs, "JMARS", JOptionPane.PLAIN_MESSAGE);
//        System.out.println("You entered " +
//                proxyHost.getText() + ", " +
//                proxyPort.getText());
//        
//        // TODO: Add some validation here!
//        ProxyInformation proxyInfo = ProxyInformation.getInstance();
//        proxyInfo.setHost(proxyHost.getText());
//        proxyInfo.setPort(Integer.parseInt(proxyPort.getText()));
    	
        if (!failedConnectionMessageDisplayedFlag && !testConnectionFlag) {//don't show the dialog multiple times and don't show it during test connection
        	Util.showMessageDialog("<html>Network Error: Please check Options->Network Settings for more options</html>", "Could Not Connect to JMARS servers.", JOptionPane.ERROR_MESSAGE);
        	failedConnectionMessageDisplayedFlag = true;
        }    
        
    } // displayProxyInfoDialog()

    
   private void displayProxyCredentialsDialog() {
        
//       JTextField     userName = new JTextField();
//       JPasswordField password = new JPasswordField();
//       final JComponent[] inputs = new JComponent[] {
//               new JLabel("Please enter proxy credentials:"),
//               new JLabel(" "),
//               new JLabel("User name"),
//               userName,
//               new JLabel("Password"),
//               password
//        };
//        Util.showMessageDialog(null, inputs, "JMARS", JOptionPane.PLAIN_MESSAGE);
//        System.out.println("You entered " +
//               userName.getText() + ", " +
//               password.getText());
//        
//        // TODO: Add some validation here!
//        ProxyInformation proxyInfo = ProxyInformation.getInstance();
//        proxyInfo.setUsername(userName.getText());
//        proxyInfo.setPassword(password.getText());
	   
       if (!failedConnectionMessageDisplayedFlag && !testConnectionFlag) {//don't show the dialog multiple times and don't show it during test connection
	       Util.showMessageDialog("Network Error: Please check Options->Network Settings for more options.", "Proxy Authentication Error", JOptionPane.ERROR_MESSAGE);
	       failedConnectionMessageDisplayedFlag = true;
       }    
        
        
    } // displayProxyCredentialsDialog()
   
    private static final String googleDirect = "www.google.com";
    private static final String googleJMARS = "http://www.google.com";
    private static final String appserverDirect = "appserver.mars.asu.edu";
    private static final String appserverJMARS = StampLayer.stampURL + "StampFetcher";
    private static final HashMap<String, Boolean> connectionResults = new HashMap<String, Boolean>();
    public static final String APP_SERVER_DIRECT = "ASD";
    public static final String APP_SERVER_PROXY = "ASP";
    public static final String APP_SERVER_NO_PROXY = "ASNP";
    public static final String GOOGLE_DIRECT = "GD";
    public static final String GOOGLE_PROXY = "GP";
    public static final String GOOGLE_NO_PROXY = "GNP";
    public static synchronized HashMap<String, Boolean> testNetworkAvailablility() {
    	connectionResults.clear();
    	//init
    	connectionResults.put(APP_SERVER_DIRECT, false);
		connectionResults.put(GOOGLE_DIRECT, false);
		connectionResults.put(APP_SERVER_NO_PROXY, false);
		connectionResults.put(GOOGLE_NO_PROXY, false);
    	connectionResults.put(APP_SERVER_PROXY, false);
    	connectionResults.put(GOOGLE_PROXY, false);

		//Simple DNS Test
		try {
    		InetAddress jmarsAddress = InetAddress.getByName(appserverDirect);
    		if (jmarsAddress.isReachable(1000)) {
    			connectionResults.put(APP_SERVER_DIRECT, true);
    		} else {
    			connectionResults.put(APP_SERVER_DIRECT, false);
    		}
    		jmarsAddress = InetAddress.getByName(googleDirect);
    		if (jmarsAddress.isReachable(1000)) {
    			connectionResults.put(GOOGLE_DIRECT, true);
    		} else {
    			connectionResults.put(GOOGLE_DIRECT, false);
    		}
		} catch(Exception e) {
			connectionResults.put(APP_SERVER_DIRECT, false);
			connectionResults.put(GOOGLE_DIRECT, false);
			
		}
		
		int connectTimeout = 10000;
		
		//Test ignoring proxy settings
		ProxyInformation proxy = ProxyInformation.getInstance();
		boolean originalProxyFlag = proxy.isProxyUsed();
		proxy.setUseProxy(false);
		JmarsHttpRequest request = null;
		boolean fetchSuccessful = false;
		try {
    		request = new JmarsHttpRequest(appserverJMARS, HttpRequestType.GET);
    		request.setConnectionTimeout(connectTimeout);
    		request.setTestConnectionFlag(true);
	        fetchSuccessful = request.send();
	        if(!fetchSuccessful) {
	        	connectionResults.put(APP_SERVER_NO_PROXY, false);
	        } else {
	        	connectionResults.put(APP_SERVER_NO_PROXY, true);
	        }
	        request.close();
	        if (!fetchSuccessful) {
	        	return connectionResults;
	        }
		
	        request = new JmarsHttpRequest(googleJMARS, HttpRequestType.GET);
	        request.setConnectionTimeout(connectTimeout);
    		request.setTestConnectionFlag(true);
	        fetchSuccessful = request.send();
	        if(!fetchSuccessful) {
	        	connectionResults.put(GOOGLE_NO_PROXY, false);
	        } else {
	        	connectionResults.put(GOOGLE_NO_PROXY, true);
	        }
	        request.close();
	        if (!fetchSuccessful) {
	        	return connectionResults;
	        }
		} catch (Exception e) {
			connectionResults.put(APP_SERVER_NO_PROXY, false);
			connectionResults.put(GOOGLE_NO_PROXY, false);
		} finally {
			proxy.setUseProxy(originalProxyFlag);
		}
        //Test using proxy settings
		if (originalProxyFlag) {
			proxy.setUseProxy(true);
		}
        try {
    		request = new JmarsHttpRequest(appserverJMARS, HttpRequestType.GET);
    		request.setConnectionTimeout(connectTimeout);
    		request.setTestConnectionFlag(true);
	        fetchSuccessful = request.send();
	        if(!fetchSuccessful) {
	        	connectionResults.put(APP_SERVER_PROXY, false);
	        	connectionFailed = true;//the real test that matters
	        } else {
	        	connectionResults.put(APP_SERVER_PROXY, true);
	        	connectionFailed = false;//the real test that matters
	        	messageLogged = false; //reset
	        }
	        request.close();
	        if (!fetchSuccessful) {
	        	return connectionResults;
	        }
	        
	        request = new JmarsHttpRequest(googleJMARS, HttpRequestType.GET);
	        request.setConnectionTimeout(connectTimeout);
    		request.setTestConnectionFlag(true);
	        fetchSuccessful = request.send();
	        if(!fetchSuccessful) {
	        	connectionResults.put(GOOGLE_PROXY, false);
	        } else {
	        	connectionResults.put(GOOGLE_PROXY, true);
	        }
	        request.close();
        } catch (Exception e) {
        	connectionResults.put(APP_SERVER_PROXY, false);
        	connectionFailed = true;
        	connectionResults.put(GOOGLE_PROXY, false);
        }
    	return connectionResults;
    }
    
	public static boolean isStampServerAvailable() {
		//This code does a simple connection to the stamp server to test whether we will be able to create a connection later. 
		//If we are not able to create a connection later, we do not want to start up any dialogs. 
		boolean connectionConfirmed = false;
		try {
	        String url = appserverJMARS;
	        JmarsHttpRequest request = new JmarsHttpRequest(url, HttpRequestType.GET);
	        request.setConnectionTimeout(10000);//10 second timeout for test. 
	        request.setTestConnectionFlag(true);
	        boolean fetchSuccessful = request.send();
	        if(!fetchSuccessful) {
	            int code = request.getStatus();
	            System.err.println("JmarsHttpRequest: HTTP code "+code+" received when testing connectivity to: "+url);
	            System.err.println("JMARS network is unavailable. Some layers may not be available.");
	            DebugLog.instance().println("JMARS network is unavailable - UnknownHostException. Some layers may not be available.");
	        	ConnectionCheck.startThread();
	        } else {
	            connectionConfirmed = true;//just local variable being returned for the one check
	        }
	        request.close();
		} catch (URISyntaxException use) {
			System.err.println("Stamp server unknown host: "+StampLayer.stampURL);
			DebugLog.instance().println("Unknown host for stamp server: "+StampLayer.stampURL+" in StampFactory.java addLView.");
		} catch (Exception e) {
			System.err.println("Error connecting go the stamp server.");
			DebugLog.instance().println("A connection to the stamp server: "+StampLayer.stampURL+" was not able to be established in the test StampFactory.java addLView.");
		}

        connectionFailed = !connectionConfirmed;//static volatile variable checked throughout the application
		return connectionConfirmed;
	}

	public static void resetFailedConnectionDisplayFlag() {
		failedConnectionMessageDisplayedFlag = false;
	}
} // class JmarsHttpAccess
