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

package org.onehippo.cms7.essentials.rest.model;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.onehippo.cms7.essentials.sdk.api.model.rest.PluginDescriptor;

import static org.junit.Assert.assertEquals;

/**
 * @version "$Id$"
 */
public class ApplicationDataTest {

    @Test
    public void testGetApplication() throws Exception {
        PluginDescriptor descriptor = new PluginDescriptor();
        ApplicationData applicationData = new ApplicationData();

        applicationData.addFiles(descriptor); // not a tool, has-config flag not set

        descriptor.setId("isTool");
        descriptor.setType(PluginDescriptor.TYPE_TOOL);
        applicationData.addFiles(descriptor);

        descriptor.setId("hasConfiguration");
        descriptor.setType(PluginDescriptor.TYPE_FEATURE);
        descriptor.setHasConfiguration(true);
        applicationData.addFiles(descriptor);

        final ObjectMapper mapper = new ObjectMapper();
        final String json = mapper.writeValueAsString(applicationData);
        applicationData = mapper.readValue(json, ApplicationData.class);
        final List<String> files = applicationData.getFiles();
        assertEquals(2, files.size());
        assertEquals("tool/isTool/isTool.js", files.get(0));
        assertEquals("feature/hasConfiguration/hasConfiguration.js", files.get(1));
    }
}
