package org.hippoecm.repository.security.ldap;


import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Socket Factory for SSL connections which do not provide an authentication
 * This is used to connect to servers where we we are just interested in
 * an encrypted tunnel, and not to verify that both parties trust each other.
 */
public class GullibleSSLSocketFactory extends SSLSocketFactory {

    @SuppressWarnings("unused")
    final static String SVN_ID = "$Id$";
    
    static class GullibleTrustManager implements X509TrustManager {
        GullibleTrustManager() {
        }

        public void checkClientTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
        }

        public void checkServerTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private SSLSocketFactory factory;

    protected GullibleSSLSocketFactory() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] { new GullibleTrustManager() }, new SecureRandom());
            factory = context.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public static SocketFactory getDefault() {
        return new GullibleSSLSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return factory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(final Socket s, final String host, final int port, final boolean autoClose)
            throws IOException {
        return factory.createSocket(s, host, port, autoClose);
    }

    @Override
    public Socket createSocket(final String host, final int port) throws IOException, UnknownHostException {
        return factory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort)
            throws IOException, UnknownHostException {
        return factory.createSocket(host, port, localAddress, localPort);
    }

    @Override
    public Socket createSocket(final InetAddress host, final int port) throws IOException {
        return factory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(final InetAddress address, final int port, final InetAddress localAddress,
            final int localPort) throws IOException {
        return factory.createSocket(address, port, localAddress, localPort);
    }
}
