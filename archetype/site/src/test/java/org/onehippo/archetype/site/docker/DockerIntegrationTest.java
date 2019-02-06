package org.onehippo.archetype.site.docker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.Before;
import org.junit.Test;

public class DockerIntegrationTest {

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
        int retryCount = 12;
        while (retryCount > 0) {
            try {
                int webappStatus = doRequest(webapp);
                assertEquals(webappStatus, 200);
                return true;
            } catch (ConnectException ce) {
                retryCount = retryCount - 1;
                Thread.sleep(10000);
            } catch (Exception e) {
                break;
            }
        }
        return false;
    }

    private int doRequest(String webapp) throws IOException {
        URL url = new URL("http://localhost:8080/" + webapp + "/ping/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        return connection.getResponseCode();
    }

}
