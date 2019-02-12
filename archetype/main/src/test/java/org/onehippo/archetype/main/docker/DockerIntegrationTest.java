package org.onehippo.archetype.main.docker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(DockerIntegrationTest.class);
    private static final String bootstrapTimeout = System.getProperty("docker.test.bootstrap.timeout");
    private static final String siteWebappContextPath = System.getProperty("docker.test.site.context.path");
    private static final int SLEEP_DURATION = 5_000;
    private static final int CONNECT_TIMEOUT = 1_000;
    private static final int READ_TIMEOUT = 1_000;
    private static final int EXPECTED_HTTP_STATUS_CODE = 200;
    private static final String PING_CHECK_ADDRESS = "http://localhost:8080/%s/ping/";

    @Test
    public void testPingCms() throws InterruptedException {
        boolean success = pingWebapp("cms");
        if (!success) {
            fail("Pinging cms webapp is failed!");
        }
    }

    @Test
    public void testPingSite() throws InterruptedException {
        boolean success = pingWebapp(siteWebappContextPath);
        if (!success) {
            fail("Pinging site webapp is failed!");
        }
    }

    private boolean pingWebapp(String webapp) throws InterruptedException {
        int timeout = Integer.valueOf(bootstrapTimeout);
        while (timeout > 0) {
            try {
                int webappStatus = doRequest(webapp);
                assertEquals(webappStatus, EXPECTED_HTTP_STATUS_CODE);
                return true;
            } catch (SocketTimeoutException | SocketException e) {
                logger.warn(e.getMessage());
                timeout = timeout - SLEEP_DURATION;
                Thread.sleep(SLEEP_DURATION);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                break;
            }
        }
        return false;
    }

    private int doRequest(String webapp) throws IOException {
        URL url = new URL(String.format(PING_CHECK_ADDRESS, webapp));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
        return connection.getResponseCode();
    }

}
