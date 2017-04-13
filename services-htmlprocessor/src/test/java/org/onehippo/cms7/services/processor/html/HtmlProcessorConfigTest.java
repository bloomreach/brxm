/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.services.processor.html;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.onehippo.cms7.services.processor.html.filter.Element;
import org.onehippo.cms7.services.processor.html.serialize.HtmlSerializer;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class HtmlProcessorConfigTest {

    @Test
    public void testDefaultConfig () throws Exception {
        HtmlProcessorConfig config = new HtmlProcessorConfig();

        assertEquals("UTF-8", config.getCharset());
        assertEquals(HtmlSerializer.SIMPLE, config.getSerializer());
        assertFalse(config.isFilter());
        assertFalse(config.isOmitComments());
        assertTrue(config.isConvertLineEndings());
        assertNull(config.getWhitelistElements());
    }

    @Test
    public void testRepositoryConfiguration () throws Exception {
        HtmlProcessorConfig config = new HtmlProcessorConfig();
        MockNode root = MockNode.root();
        MockNode configNode = root.addNode("config", "hipposys:moduleconfig");
        configNode.setProperty("charset", "UTF-16");
        configNode.setProperty("omitComments", true);
        configNode.setProperty("omitJavascript", false);
        configNode.setProperty("convertLineEndings", false);
        configNode.setProperty("filter", true);
        configNode.setProperty("serializer", HtmlSerializer.COMPACT.name());

        configNode.addNode("a", "hipposys:moduleconfig");
        MockNode div = configNode.addNode("div", "hipposys:moduleconfig");
        div.setProperty("attributes", new String[] {"class", "id"});

        config.reconfigure(configNode);

        assertEquals("UTF-16", config.getCharset());
        assertEquals(HtmlSerializer.COMPACT, config.getSerializer());
        assertTrue(config.isFilter());
        assertTrue(config.isOmitComments());
        assertFalse(config.isOmitJavascript());
        assertFalse(config.isConvertLineEndings());

        assertThat(config.getWhitelistElements(), CoreMatchers.hasItems(
                Element.create("a"),
                Element.create("div", "class", "id")
        ));
    }

    @Test
    public void testSerializerConfigurationIsCaseInsensitive () throws Exception {
        HtmlProcessorConfig config = new HtmlProcessorConfig();
        MockNode root = MockNode.root();
        MockNode configNode = root.addNode("config", "hipposys:moduleconfig");

        configNode.setProperty("serializer", "compact");
        config.reconfigure(configNode);
        assertEquals(HtmlSerializer.COMPACT, config.getSerializer());

        configNode.setProperty("serializer", "PreTTy");
        config.reconfigure(configNode);
        assertEquals(HtmlSerializer.PRETTY, config.getSerializer());
    }

    @Test
    public void testSerializerConfigurationDefaultsToSimple () throws Exception {
        HtmlProcessorConfig config = new HtmlProcessorConfig();
        MockNode root = MockNode.root();
        MockNode configNode = root.addNode("config", "hipposys:moduleconfig");

        configNode.setProperty("serializer", "non-existing-serializer");
        config.reconfigure(configNode);
        assertEquals(HtmlSerializer.SIMPLE, config.getSerializer());
    }

    }
