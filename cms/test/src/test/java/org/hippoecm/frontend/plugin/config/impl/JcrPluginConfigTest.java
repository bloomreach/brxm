/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugin.config.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JcrPluginConfigTest extends HippoTester {

    private Node node = MockNode.root();
    private JcrPluginConfig config;

    @Before
    public void setUp() {
        config = new JcrPluginConfig(new JcrNodeModel(node));
    }

    @Test
    public void getDuration() throws RepositoryException {
        node.setProperty("test", "2 minutes");
        assertEquals(config.getDuration("test").getSeconds(), 120, 0.001);
        assertEquals(config.getAsDuration("test").getSeconds(), 120, 0.001);
        assertNull(config.getAsDuration("nosuchproperty"));
    }

}