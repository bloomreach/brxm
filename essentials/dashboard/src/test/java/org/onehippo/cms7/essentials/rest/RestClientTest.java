/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.rest;

import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.cms7.essentials.rest.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id: RestClientTest.java 175050 2013-08-26 16:10:53Z mmilicevic $"
 */
@Ignore(value = "Set to ignore to restrict amount of service calls to the gist")
public class RestClientTest {

    private static Logger log = LoggerFactory.getLogger(RestClientTest.class);


    @Test
    public void testGetPlugins() throws Exception {
        try {
            final RestClient client = new RestClient("https://api.github.com/gists/8453217");
            final String pluginList = client.getPluginList();
            assertNull(pluginList);
        } catch (Exception e) {
            log.warn("Reasonable timeout set, this should not be logged. Unless there is an internet connection problem or the response return non json output :", e);
        }
        try {
            final RestClient client = new RestClient("https://api.github.com/gists/8453217", 1, 1);
            final String pluginList = client.getPluginList();
            assertTrue(false);
        } catch (Exception e) {
            log.info("This should indeed happen! Timeouts set on 1 ms", e);
            assertTrue(true);
        }

    }
}
