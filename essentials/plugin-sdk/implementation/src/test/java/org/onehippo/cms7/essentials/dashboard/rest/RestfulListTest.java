/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptor;
import org.onehippo.cms7.essentials.dashboard.model.Restful;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class RestfulListTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testList() throws Exception {
        RestfulList<Restful> myList = new RestfulList<>();
        final PluginDescriptor plugin = new PluginDescriptor();
        plugin.setDescription("test");
        final PluginDescriptor.Vendor vendor = new PluginDescriptor.Vendor();
        vendor.setUrl("http://www.test.com");
        plugin.setVendor(vendor);
        myList.add(plugin);
        // add key value
        myList.add(new KeyValueRestful("test", "value"));

        ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(myList);
        myList = mapper.readValue(json, RestfulList.class);
        final List<Restful> items = myList.getItems();
        final int listSize = myList.getItems().size();
        final List<Class<?>> classList = new ArrayList<>();
        for (Restful item : items) {
            classList.add(item.getClass());
        }
        assertEquals("Expected all list items to be of  different class", listSize, classList.size());
    }

    @Test
    public void test_more() throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        final RestfulList<KeyValueRestful> keyValue = new RestfulList<>();
        keyValue.add(new KeyValueRestful("test", "test"));
        keyValue.add(new KeyValueRestful("test1", "test1"));
        String result = mapper.writeValueAsString(keyValue);
        @SuppressWarnings("unchecked") final RestfulList<KeyValueRestful> myList = mapper.readValue(result, RestfulList.class);
        assertEquals(2, myList.getItems().size());
    }
}
