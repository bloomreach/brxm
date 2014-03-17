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

package org.onehippo.cms7.essentials.dashboard.rest;

import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.model.PluginRestful;
import org.onehippo.cms7.essentials.dashboard.model.Vendor;
import org.onehippo.cms7.essentials.dashboard.model.VendorRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class BaseResourceTest {

    private static Logger log = LoggerFactory.getLogger(BaseResourceTest.class);

    @Test
    public void testPluginParsing() throws Exception {

        RestfulList<PluginRestful> plugins = new RestfulList<>();
        final PluginRestful plugin = new PluginRestful();
        plugin.setDescription("test");
        final Vendor vendor = new VendorRestful();
        vendor.setUrl("http://www.test.com");
        plugin.setVendor(vendor);
        plugins.add(plugin);


        ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(plugins);
        log.error("json {}", json);

        plugins = mapper.readValue(json, RestfulList.class);
        final BaseResource resource = new BaseResource();
        final List<PluginRestful> myPlugins = resource.getPlugins(null);
        assertTrue(myPlugins.size() > 0);
    }
}
