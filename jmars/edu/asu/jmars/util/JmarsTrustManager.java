package edu.asu.jmars.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.protocol.Protocol;

/**
 * This trust manager disables validating the server certificate, using it for
 * encryption but not endpoint validation.
 */
public class JmarsTrustManager implements X509TrustManager {
	/**
	 * Test code to prove we can establish an https connection to a server that
	 * does not participate in a chain of trust
	 * @throws URISyntaxException 
	 */
	public static void main(String[] args) throws MalformedURLException, IOException, URISyntaxException {
		install();
//		BufferedReader br = new BufferedReader(new InputStreamReader(
//			new URL("https://ms-test.mars.asu.edu").openStream()));
//		String line;
//		while ((line = br.readLine()) != null)
//			System.out.println(line);
//		br.close();
		
		String line;
		JmarsHttpRequest request = new JmarsHttpRequest("https://ms-test.mars.asu.edu", HttpRequestType.GET);
		boolean success = request.send();
		if (success) {
			BufferedReader read = new BufferedReader(new InputStreamReader(request.getResponseAsStream()));
			while ((line = read.readLine()) != null)
				System.out.println(line);
			read.close();
		}
		request.close();
	}
	
	/** Configures HttpUrlConnection and HttpClient over SSL/TLS to use JmarsTrustManager */
	public static void install() {
		try {
			// install JMARS-specific trust policy for HttpUrlConnection
			SSLContext context = SSLContext.getInstance("TLS");
			TrustManager jmarsTrust = new JmarsTrustManager();
			context.init(null, new TrustManager[]{jmarsTrust}, null);
			HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());

			// install JMARS-specific trust policy for HttpClient
			Protocol easyhttps = new Protocol("https", new HttpClientJmarsTrustManagerFactory(), 443);
			Protocol.registerProtocol("https", easyhttps);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// X509 trust manager interface - just accept any server certificate
	
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	}
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	}
	public X509Certificate[] getAcceptedIssuers() {
		return null;
	}
}
