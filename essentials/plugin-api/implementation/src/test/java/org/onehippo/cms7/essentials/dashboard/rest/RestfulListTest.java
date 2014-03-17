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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.model.PluginRestful;
import org.onehippo.cms7.essentials.dashboard.model.Restful;
import org.onehippo.cms7.essentials.dashboard.model.Vendor;
import org.onehippo.cms7.essentials.dashboard.model.VendorRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class RestfulListTest {

    private static Logger log = LoggerFactory.getLogger(RestfulListTest.class);

    @SuppressWarnings("unchecked")
    @Test
    public void testList() throws Exception {
        RestfulList<Restful> myList = new RestfulList<>();
        final PluginRestful plugin = new PluginRestful();
        plugin.setDescription("test");
        final Vendor vendor = new VendorRestful();
        vendor.setUrl("http://www.test.com");
        plugin.setVendor(vendor);
        myList.add(plugin);
        // add key value
        myList.add(new KeyValueRestful("test", "value"));
        myList.add(new ErrorMessageRestful("test", DisplayEvent.DisplayType.P));
        myList.add(new PostPayloadRestful("test", "value"));
        myList.add(new ProjectRestful("somenamespace"));
        myList.add(new PropertyRestful("test", "value"));
        myList.add(new NodeRestful("test", "value"));
        myList.add(new QueryRestful("mypath"));

        ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(myList);
        log.info("json {}", json);
        myList = mapper.readValue(json, RestfulList.class);
        final List<Restful> items = myList.getItems();
        final int listSize = myList.getItems().size();
        final List<Class<?>> classList = new ArrayList<>();
        for (Restful item : items) {
            log.info("item {}", item);
            classList.add(item.getClass());
        }
        assertEquals("Expected all list items to be of  different class",listSize, classList.size());


    }
}
