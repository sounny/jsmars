package edu.asu.jmars.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClientError;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

/**
 * Provides an SSL socket factory for HttpClient that will use the
 * JmarsTrustManager to create SSL connections, allowing connection to https
 * services using self-signed certificates.
 */
public class HttpClientJmarsTrustManagerFactory implements SecureProtocolSocketFactory {
	private SSLContext sslcontext = null;
	private static SSLContext createContext() {
		try {
			SSLContext context = SSLContext.getInstance("SSL");
			context.init(null, new TrustManager[] {new JmarsTrustManager()}, null);
			return context;
		} catch (Exception e) {
			throw new HttpClientError(e.toString());
		}
	}
	private SSLContext getSSLContext() {
		if (this.sslcontext == null) {
			this.sslcontext = createContext();
		}
		return this.sslcontext;
	}
	public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort)
		throws IOException, UnknownHostException
	{
		return getSSLContext().getSocketFactory().createSocket(host,port,clientHost,clientPort);
	}
	public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params)
		throws IOException, UnknownHostException, ConnectTimeoutException 
	{
		if (params == null) {
			throw new IllegalArgumentException("Parameters may not be null");
		}
		int timeout = params.getConnectionTimeout();
		SocketFactory socketfactory = getSSLContext().getSocketFactory();
		if (timeout == 0) {
			return socketfactory.createSocket(host, port, localAddress, localPort);
		} else {
			Socket socket = socketfactory.createSocket();
			SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
			SocketAddress remoteaddr = new InetSocketAddress(host, port);
			socket.bind(localaddr);
			socket.connect(remoteaddr, timeout);
			return socket;
		}
	}
	public Socket createSocket(String host, int port)
		throws IOException, UnknownHostException
	{
		return getSSLContext().getSocketFactory().createSocket(host, port);
	}
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
		throws IOException, UnknownHostException
	{
		return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
	}
	public boolean equals(Object obj) {
		return obj != null && getClass().isInstance(obj);
	}
	public int hashCode() {
		return getClass().hashCode();
	}
}
