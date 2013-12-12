package org.onehippo.cms7.essentials.rest;

import org.junit.Test;
import org.onehippo.cms7.essentials.rest.client.RestClient;
import org.onehippo.cms7.essentials.rest.model.PluginRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id: RestClientTest.java 175050 2013-08-26 16:10:53Z mmilicevic $"
 */
public class RestClientTest {

    public static final int EXPECTED = 2;
    private static Logger log = LoggerFactory.getLogger(RestClientTest.class);

    @Test
    public void testGetPlugins() throws Exception {
       /* RestClient client = new RestClient("foo");
        final RestfulList<PluginRestful> plugins = client.getPlugins();
        assertEquals("Expected " + EXPECTED + " plugin", EXPECTED, plugins.getItems().size());*/

    }
}
