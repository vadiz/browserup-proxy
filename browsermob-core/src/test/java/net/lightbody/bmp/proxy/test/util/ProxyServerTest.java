package net.lightbody.bmp.proxy.test.util;

import net.lightbody.bmp.proxy.ProxyServer;
import net.lightbody.bmp.proxy.test.util.TestSSLSocketFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.junit.After;
import org.junit.Before;

import java.security.KeyStore;

/**
 * Extend this class to gain access to a local proxy server. If you need both a local proxy server and a local Jetty server, extend
 * {@link net.lightbody.bmp.proxy.test.util.LocalServerTest} instead.
 * <p/>
 * Call getNewHttpClient() to get an HttpClient that can be used to make requests via the local proxy.
 */
public abstract class ProxyServerTest {
    protected int proxyServerPort;
    protected ProxyServer proxy;
    protected DefaultHttpClient client;

    @Before
    public void startServer() throws Exception {
        proxy = new ProxyServer(0);
        proxy.start();
        proxyServerPort = proxy.getPort();

        client = getNewHttpClient();
    }

    @After
    public void stopServer() throws Exception {
        proxy.stop();
    }

    public DefaultHttpClient getNewHttpClient() {
        return getNewHttpClient(proxyServerPort);
    }

    public int getPort() {
        return proxy.getPort();
    }

    public static DefaultHttpClient getNewHttpClient(int proxyPort) {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new TestSSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            params.setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost("127.0.0.1", proxyPort, "http"));
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get HTTP client", e);
        }
    }

}
