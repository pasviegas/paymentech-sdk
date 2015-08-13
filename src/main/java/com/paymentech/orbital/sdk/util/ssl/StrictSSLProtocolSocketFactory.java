package com.paymentech.orbital.sdk.util.ssl;

import com.paymentech.orbital.sdk.configurator.ConfiguratorIF;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.security.cert.X509Certificate;
import java.io.IOException;
import java.net.*;

/**
 * <p><b>Title:</b> StrictSSLProtocolSocketFactory</p> <p>(C)opyright 2007, Chase Paymentech Solutions, LLC. All rights
 * <p/>
 * reserved
 * <p/>
 * The copyright notice above does not evidence any actual or intended
 * publication of such source code.
 * The copyright notice above does not evidence any actual or intended publication of such source code.</p>
 * <p><b>Author:</b> Scott Monahan</p><p><b>Description:</b>
 * <br><br> Class that uses JSSE to create
 * SSL sockets.  It will also support host name verification to help preventing
 * man-in-the-middle attacks.  Host name verification is turned <b>on</b> by
 * default but one will be able to turn it off, which might be a useful feature
 * during development.  Host name verification will make sure the SSL sessions
 * server host name matches with the the host name returned in the
 * server certificates "Common Name" field of the "SubjectDN" entry.
 * </p>
 **/

public class StrictSSLProtocolSocketFactory
    implements SecureProtocolSocketFactory {

  /**
   * Log object for this class.
   */
  private static final Logger LOG = Logger.getLogger(StrictSSLProtocolSocketFactory.class);

  /**
   * Host name verify flag.
   */
  private boolean verifyHostname = true;

  private ConfiguratorIF configurator = null;

  /**
   * Constructor for StrictSSLProtocolSocketFactory.
   *
   * @param verifyHostname The host name verification flag. If set to
   *                       <code>true</code> the SSL sessions server host name will be compared
   *                       to the host name returned in the server certificates "Common Name"
   *                       field of the "SubjectDN" entry.  If these names do not match a
   *                       Exception is thrown to indicate this.  Enabling host name verification
   *                       will help to prevent from man-in-the-middle attacks.  If set to
   *                       <code>false</code> host name verification is turned off.
   *                       <p/>
   *                       Code sample:
   *                       <p/>
   *                       <blockquote>
   *                       Protocol stricthttps = new Protocol(
   *                       "https", new StrictSSLProtocolSocketFactory(true), 443);
   *                       <p/>
   *                       HttpClient client = new HttpClient();
   *                       client.getHostConfiguration().setHost("localhost", 443, stricthttps);
   *                       </blockquote>
   */
  public StrictSSLProtocolSocketFactory(boolean verifyHostname) {
    super();
    this.verifyHostname = verifyHostname;
  }

  /**
   * Constructor for StrictSSLProtocolSocketFactory.
   * Host name verification will be enabled by default.
   */
  public StrictSSLProtocolSocketFactory() {
    super();
  }

  public StrictSSLProtocolSocketFactory(ConfiguratorIF configurator) {
    super();
    this.configurator = configurator;
  }

  /**
   * Gets the status of the host name verification flag.
   *
   * @return Host name verification flag.  Either <code>true</code> if host
   * name verification is turned on, or <code>false</code> if host name
   * verification is turned off.
   */
  public boolean getHostnameVerification() {
    return verifyHostname;
  }

  /**
   * Set the host name verification flag.
   *
   * @param verifyHostname The host name verification flag. If set to
   *                       <code>true</code> the SSL sessions server host name will be compared
   *                       to the host name returned in the server certificates "Common Name"
   *                       field of the "SubjectDN" entry.  If these names do not match a
   *                       Exception is thrown to indicate this.  Enabling host name verification
   *                       will help to prevent from man-in-the-middle attacks.  If set to
   *                       <code>false</code> host name verification is turned off.
   */
  public void setHostnameVerification(boolean verifyHostname) {
    this.verifyHostname = verifyHostname;
  }

  /**
   * @see SecureProtocolSocketFactory#createSocket(String, int, InetAddress, int)
   */
  public Socket createSocket(String host, int port,
                             InetAddress clientHost, int clientPort)
      throws IOException, UnknownHostException {
    SSLSocketFactory sf = SocketFactoryFactory.getSocketFactory(configurator);
    SSLSocket sslSocket = (SSLSocket) sf.createSocket(host, port,
        clientHost,
        clientPort);
    verifyHostname(sslSocket);

    return sslSocket;
  }

  /**
   * @see SecureProtocolSocketFactory#createSocket(String, int)
   */
  public Socket createSocket(String host, int port)
      throws IOException, UnknownHostException {
    SSLSocketFactory sf = SocketFactoryFactory.getSocketFactory(configurator);
    SSLSocket sslSocket = (SSLSocket) sf.createSocket(host, port);
    verifyHostname(sslSocket);

    return sslSocket;
  }

  /**
   * @see SecureProtocolSocketFactory#createSocket(Socket, String, int, boolean)
   */
  public Socket createSocket(Socket socket, String host, int port,
                             boolean autoClose)
      throws IOException, UnknownHostException {
    SSLSocketFactory sf = SocketFactoryFactory.getSocketFactory(configurator);
    SSLSocket sslSocket = (SSLSocket) sf.createSocket(socket, host,
        port, autoClose);
    verifyHostname(sslSocket);

    return sslSocket;
  }

  /**
   * Attempts to get a new socket connection to the given host within the given time limit.
   * <p>
   * This method employs several techniques to circumvent the limitations of older JREs that
   * do not support connect timeout. When running in JRE 1.4 or above reflection is used to
   * call Socket#connect(SocketAddress endpoint, int timeout) method. When executing in older
   * JREs a controller thread is executed. The controller thread attempts to create a new socket
   * within the given limit of time. If socket constructor does not return until the timeout
   * expires, the controller terminates and throws an {@link ConnectTimeoutException}
   * </p>
   *
   * @param host       the host name/IP
   * @param port       the port on the host
   * @param clientHost the local host name/IP to bind the socket to
   * @param clientPort the port on the local machine
   * @param params     {@link HttpConnectionParams Http connection parameters}
   * @return Socket a new socket
   * @throws IOException          if an I/O error occurs while creating the socket
   * @throws UnknownHostException if the IP address of the host cannot be
   *                              determined
   */
  public Socket createSocket(final String host, final int port,
                             final InetAddress localAddress, final int localPort,
                             final HttpConnectionParams params)
      throws IOException, UnknownHostException {
    if (params == null) {
      throw new UnknownHostException("Parameters may not be null");
    }
    int timeout = params.getConnectionTimeout();
    SSLSocket sslSocket = null;
    SSLSocketFactory sf = SocketFactoryFactory.getSocketFactory(configurator);
    if (timeout == 0) {
      sslSocket = (SSLSocket) sf.createSocket(host, port, localAddress, localPort);
    } else {
      sslSocket = (SSLSocket) sf.createSocket();
      SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
      SocketAddress remoteaddr = new InetSocketAddress(host, port);
      sslSocket.bind(localaddr);
      sslSocket.connect(remoteaddr, timeout);
    }
    verifyHostname(sslSocket);
    return sslSocket;
  }


  /**
   * Describe <code>verifyHostname</code> method here.
   *
   * @param socket a <code>SSLSocket</code> value
   * @throws SSLPeerUnverifiedException If there are problems obtaining
   *                                    the server certificates from the SSL session, or the server host name
   *                                    does not match with the "Common Name" in the server certificates
   *                                    SubjectDN.
   * @throws UnknownHostException       If we are not able to resolve
   *                                    the SSL sessions returned server host name.
   */
  private void verifyHostname(SSLSocket socket)
      throws SSLPeerUnverifiedException, UnknownHostException {
    if (!verifyHostname)
      return;

    SSLSession session = socket.getSession();
    String hostname = session.getPeerHost();
    try {
      InetAddress addr = InetAddress.getByName(hostname);
    } catch (UnknownHostException uhe) {
      throw new UnknownHostException("Could not resolve SSL sessions "
          + "server hostname: " + hostname);
    }

    X509Certificate[] certs = session.getPeerCertificateChain();
    if (certs == null || certs.length == 0)
      throw new SSLPeerUnverifiedException("No server certificates found!");

    //get the servers DN in its string representation
    String dn = certs[0].getSubjectDN().getName();

    //might be useful to print out all certificates we receive from the
    //server, in case one has to debug a problem with the installed certs.
    if (LOG.isDebugEnabled()) {
      LOG.debug("Server certificate chain:");
      for (int i = 0; i < certs.length; i++) {
        LOG.debug("X509Certificate[" + i + "]=" + certs[i]);
      }
    }
    //get the common name from the first cert
    String cn = getCN(dn);
    if (hostname.equalsIgnoreCase(cn)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Target hostname valid: " + cn);
      }
    } else {
      throw new SSLPeerUnverifiedException(
          "HTTPS hostname invalid: expected '" + hostname + "', received '" + cn + "'");
    }
  }


  /**
   * Parses a X.500 distinguished name for the value of the
   * "Common Name" field.
   * This is done a bit sloppy right now and should probably be done a bit
   * more according to <code>RFC 2253</code>.
   *
   * @param dn a X.500 distinguished name.
   * @return the value of the "Common Name" field.
   */
  private String getCN(String dn) {
    int i = 0;
    i = dn.indexOf("CN=");
    if (i == -1) {
      return null;
    }
    //get the remaining DN without CN=
    dn = dn.substring(i + 3);
    // System.out.println("dn=" + dn);
    char[] dncs = dn.toCharArray();
    for (i = 0; i < dncs.length; i++) {
      if (dncs[i] == ',' && i > 0 && dncs[i - 1] != '\\') {
        break;
      }
    }
    return dn.substring(0, i);
  }

  public boolean equals(Object obj) {
    if ((obj != null) && obj.getClass().equals(StrictSSLProtocolSocketFactory.class)) {
      return ((StrictSSLProtocolSocketFactory) obj).getHostnameVerification()
          == this.verifyHostname;
    } else {
      return false;
    }
  }

  public int hashCode() {
    return StrictSSLProtocolSocketFactory.class.hashCode();
  }

}
