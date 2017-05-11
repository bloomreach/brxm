/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.essentials;

import java.io.InputStream;

import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.model.PluginDescriptorRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
public class WebUtilsTest {
    private static final Logger log = LoggerFactory.getLogger(WebUtilsTest.class);
    public static final String PLUGIN = "{\n" +
            "  \"@type\": \"plugin\",\n" +
            "  \"id\": \"documentWizardPlugin\",\n" +
            "  \"name\": \"Documents wizard\",\n" +
            "  \"icon\": \"/essentials/images/icons/documents-wizard.png\",\n" +
            "  \"introduction\": \"The Documents Wizard is a convenience feature to easily create new documents in the right folders in the Hippo CMS while working in another part of the CMS.\",\n" +
            "  \"description\": \"It provides a link that opens a dialog window where information for the document to be created can be entered. After clicking OK, the document will be created in the right folder. This feature wraps the certified Hippo Dashboard Document Wizard.\",\n" +
            "  \"imageUrls\": [\n" +
            "    \"/essentials/images/screenshots/documentwizard01.png\",\n" +
            "    \"/essentials/images/screenshots/documentwizard02.png\"\n" +
            "  ],\n" +
            "  \"documentationLink\": \"http://www.onehippo.org/library/concepts/plugins/dashboard-document-wizard/about.html\",\n" +
            "  \"parameterServiceClass\": \"org.onehippo.cms7.essentials.plugins.docwiz.DocumentWizardParameterService\",\n" +
            "  \"restClasses\": [\n" +
            "    \"org.onehippo.cms7.essentials.plugins.docwiz.DocumentWizardResource\"\n" +
            "  ],\n" +
            "  \"type\": \"feature\",\n" +
            "  \"vendor\": {\n" +
            "    \"@type\": \"vendor\",\n" +
            "    \"name\": \"Hippo\",\n" +
            "    \"url\": \"http://www.onehippo.com\"\n" +
            "  },\n" +
            "  \"libraries\": [\n" +
            "    {\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"component\": \"documentWizardPlugin\",\n" +
            "          \"file\": \"documentWizardPlugin.js\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"dependencies\": [\n" +
            "    {\n" +
            "      \"@type\": \"dependency\",\n" +
            "      \"groupId\": \"org.onehippo.cms7\",\n" +
            "      \"artifactId\": \"hippo-plugin-dashboard-document-wizard\",\n" +
            "      \"scope\": \"compile\",\n" +
            "      \"targetPom\": \"cms\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n";

    @Test
    public void jsonTest() throws Exception {
        PluginDescriptorRestful descriptorRestful = WebUtils.fromJson(PLUGIN, PluginDescriptorRestful.class);
        assertNotNull("Expected object but found null", descriptorRestful);
        assertEquals("documentWizardPlugin", descriptorRestful.getId());
        final String result = WebUtils.toJson(descriptorRestful);
        descriptorRestful = WebUtils.fromJson(result, PluginDescriptorRestful.class);
        assertNotNull("Expected object but found null", descriptorRestful);
        assertEquals("documentWizardPlugin", descriptorRestful.getId());
    }

    @Test
    public void externalListLoading() throws Exception {
        final InputStream stream = getClass().getResourceAsStream("/external_list.json");
        final String jsonString = GlobalUtils.readStreamAsText(stream);

        try {
            @SuppressWarnings("unchecked") final RestfulList<PluginDescriptorRestful> restfulList = WebUtils.fromJson(jsonString, RestfulList.class);
            assertNotNull(restfulList);
            assertEquals(restfulList.getItems().size(), 4);
        } catch (Exception e) {
            log.error("Error parsing plugins", e);
        }
    }


}