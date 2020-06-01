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

package org.onehippo.cms7.essentials.plugin.sdk.rest;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.onehippo.cms7.essentials.sdk.api.model.rest.PluginDescriptor;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class PluginDescriptorListTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testList() throws Exception {
        PluginDescriptorList myList = new PluginDescriptorList();
        final PluginDescriptor plugin = new PluginDescriptor();
        plugin.setDescription("test");
        final PluginDescriptor.Vendor vendor = new PluginDescriptor.Vendor();
        vendor.setUrl("http://www.test.com");
        plugin.setVendor(vendor);
        myList.add(plugin);

        ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(myList);
        myList = mapper.readValue(json, PluginDescriptorList.class);
        assertEquals(1, myList.getItems().size());
        assertEquals("test", myList.getItems().get(0).getDescription());
        assertEquals("http://www.test.com", myList.getItems().get(0).getVendor().getUrl());
    }
}
