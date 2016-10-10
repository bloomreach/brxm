/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.util;

import java.util.NoSuchElementException;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.repository.mock.MockNode;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NamespaceUtils.class)
public class NamespaceUtilsTest {
    @Test
    public void getDocumentRootNode() throws Exception {
        final Session session = createMock(Session.class);
        final Node rootNode = createMock(Node.class);

        expect(session.getNode("/hippo:namespaces/ns/testdocument")).andReturn(rootNode);
        replay(session);

        assertThat(NamespaceUtils.getDocumentTypeRootNode("ns:testdocument", session).get(), equalTo(rootNode));
    }

    @Test(expected = NoSuchElementException.class)
    public void getRootNodeWithInvalidId() throws Exception {
        final Session session = createMock(Session.class);

        NamespaceUtils.getDocumentTypeRootNode("blabla", session).get();
    }

    @Test(expected = NoSuchElementException.class)
    public void getRootNodeWithRepositoryException() throws Exception {
        final Session session = createMock(Session.class);

        expect(session.getNode("/hippo:namespaces/ns/testdocument")).andThrow(new RepositoryException());
        replay(session);

        NamespaceUtils.getDocumentTypeRootNode("ns:testdocument", session).get();
    }

    @Test
    public void getConfigNode() throws Exception {
        final Node root = MockNode.root();
        final Node nth = root.addNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE, HippoNodeType.NT_HANDLE);
        final Node nt = nth.addNode(HippoNodeType.HIPPOSYSEDIT_NODETYPE, HippoNodeType.NT_NODETYPE);
        final Node ets = root.addNode("editor:templates", "editor:templateset");
        final Node et = ets.addNode("_default_", "frontend:plugincluster");
        final Node config3 = et.addNode("id3", "frontend:plugin");
        nt.addNode("id1", HippoNodeType.NT_FIELD);
        nt.addNode("id2", HippoNodeType.NT_FIELD).setProperty(HippoNodeType.HIPPO_PATH, "field2");
        nt.addNode("id3", HippoNodeType.NT_FIELD).setProperty(HippoNodeType.HIPPO_PATH, "field3");

        assertThat(NamespaceUtils.getConfigForField(root, "field0").isPresent(), equalTo(false));
        assertThat(NamespaceUtils.getConfigForField(root, "field1").isPresent(), equalTo(false));
        assertThat(NamespaceUtils.getConfigForField(root, "field2").isPresent(), equalTo(false));
        assertThat(NamespaceUtils.getConfigForField(root, "field3").get(), equalTo(config3));
    }

    @Test(expected = NoSuchElementException.class)
    public void getConfigNodeWithRepositoryException() throws Exception {
        final Node root = createMock(Node.class);

        expect(root.getNode(anyObject())).andThrow(new RepositoryException());
        replay(root);

        NamespaceUtils.getConfigForField(root, "dummy").get();
    }

    @Test
    public void getPluginClass() throws Exception {
        final String fieldId = "fieldId";
        final String pluginClass = "pluginClass";
        final Property property = createMock(Property.class);
        final Node root = createMock(Node.class);
        final Node config = createMock(Node.class);

        PowerMock.mockStaticPartial(NamespaceUtils.class, "getConfigForField");

        expect(NamespaceUtils.getConfigForField(root, fieldId)).andReturn(Optional.of(config));
        expect(config.getProperty("plugin.class")).andReturn(property);
        expect(property.getString()).andReturn(pluginClass);
        replay(config, property);
        PowerMock.replayAll();

        assertThat(NamespaceUtils.getPluginClassForField(root, fieldId).get(), equalTo(pluginClass));
    }

    @Test(expected = NoSuchElementException.class)
    public void getPluginClassWithRepositoryException() throws Exception {
        final String fieldId = "fieldId";
        final Node root = createMock(Node.class);
        final Node config = createMock(Node.class);

        PowerMock.mockStaticPartial(NamespaceUtils.class, "getConfigForField");

        expect(NamespaceUtils.getConfigForField(root, fieldId)).andReturn(Optional.of(config));
        expect(config.getProperty("plugin.class")).andThrow(new RepositoryException());
        replay(config);
        PowerMock.replayAll();

        NamespaceUtils.getPluginClassForField(root, fieldId).get();
    }

    @Test(expected = NoSuchElementException.class)
    public void getPluginClassWithMissingConfig() throws Exception {
        final String fieldId = "fieldId";
        final Node root = createMock(Node.class);

        PowerMock.mockStaticPartial(NamespaceUtils.class, "getConfigForField");

        expect(NamespaceUtils.getConfigForField(root, fieldId)).andReturn(Optional.empty());
        PowerMock.replayAll();

        NamespaceUtils.getPluginClassForField(root, fieldId).get();
    }
}
