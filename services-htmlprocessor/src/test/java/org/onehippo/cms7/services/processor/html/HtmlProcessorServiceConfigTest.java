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

import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class HtmlProcessorServiceConfigTest {

    @Test
    public void testReturnsNullIfNotFound() throws Exception {
        final HtmlProcessorServiceConfig config = new HtmlProcessorServiceConfig();
        assertNull(config.getProcessor("some-id"));

        final MockNode root = MockNode.root();
        final MockNode configNode = root.addNode("config", "nt:unstructured");
        configNode.addNode("myProcessor", "nt:unstructured");
        config.reconfigure(configNode);

        assertNull(config.getProcessor("some-id"));
    }

    @Test
    public void testReturnsNewHtmlProcessorFromConfig() throws Exception {
        final MockNode root = MockNode.root();
        final MockNode configNode = root.addNode("config", "nt:unstructured");
        configNode.addNode("myProcessor", "nt:unstructured");

        final HtmlProcessorServiceConfig config = new HtmlProcessorServiceConfig();
        config.reconfigure(configNode);

        assertNotNull(config.getProcessor("myProcessor"));

    }

    @Test
    public void testReturnsSameHtmlProcessorInstance() throws Exception {
        final MockNode root = MockNode.root();
        final MockNode configNode = root.addNode("config", "nt:unstructured");
        configNode.addNode("myProcessor", "nt:unstructured");

        final HtmlProcessorServiceConfig config = new HtmlProcessorServiceConfig();
        config.reconfigure(configNode);

        assertEquals(config.getProcessor("myProcessor"), config.getProcessor("myProcessor"));

    }

}
