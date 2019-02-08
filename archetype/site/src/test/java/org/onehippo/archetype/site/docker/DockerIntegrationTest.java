package org.onehippo.archetype.site.docker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.junit.Test;

public class DockerIntegrationTest {
    private static final String bootstrapTimeout = System.getProperty("docker.test.bootstrap.timeout");
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
        boolean success = pingWebapp("extra");
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
            } catch (ConnectException | SocketTimeoutException e) {
                timeout = timeout - SLEEP_DURATION;
                Thread.sleep(SLEEP_DURATION);
            } catch (Exception e) {
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
