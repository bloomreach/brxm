/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.repository.util;

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class DocumentUtilsTest {

    @Test
    public void getHandle() throws Exception {
        final Session session = createMock(Session.class);
        final Node node = createMock(Node.class);

        expect(session.getNodeByIdentifier("uuid")).andReturn(node);
        expect(node.isNodeType(HippoNodeType.NT_HANDLE)).andReturn(true);
        replay(session, node);

        assertThat(DocumentUtils.getHandle("uuid", session).get(), equalTo(node));
    }

    @Test(expected = NoSuchElementException.class)
    public void getHandleNoNode() throws Exception {
        final Session session = createMock(Session.class);

        expect(session.getNodeByIdentifier("uuid")).andThrow(new RepositoryException());
        replay(session);

        Log4jInterceptor.onWarn().deny(DocumentUtils.class).run( () -> {
            DocumentUtils.getHandle("uuid", session).get();
        });
    }

    @Test(expected = NoSuchElementException.class)
    public void getHandleWrongType() throws Exception {
        final Session session = createMock(Session.class);
        final Node node = createMock(Node.class);

        expect(session.getNodeByIdentifier("uuid")).andReturn(node);
        expect(node.isNodeType(HippoNodeType.NT_HANDLE)).andReturn(false);
        replay(session, node);

        DocumentUtils.getHandle("uuid", session).get();
    }

    @Test
    public void getDisplayName() throws Exception {
        final HippoNode node = createMock(HippoNode.class);

        expect(node.getDisplayName()).andReturn("Display Name");
        replay(node);

        assertThat(DocumentUtils.getDisplayName(node).get(), equalTo("Display Name"));
    }

    @Test(expected = NoSuchElementException.class)
    public void getDisplayNameWithRepositoryException() throws Exception {
        final HippoNode node = createMock(HippoNode.class);

        expect(node.getDisplayName()).andThrow(new RepositoryException());
        replay(node);

        Log4jInterceptor.onWarn().deny(DocumentUtils.class).run( () -> {
            DocumentUtils.getDisplayName(node).get();
        });
    }

    @Test
    public void getVariantNodeType() throws Exception {
        final Node handle = createMock(HippoNode.class);
        final Node variant = createMock(HippoNode.class);
        final NodeType nodeType = createMock(NodeType.class);

        expect(handle.getName()).andReturn("name");
        expect(handle.getNode("name")).andReturn(variant);
        expect(variant.getPrimaryNodeType()).andReturn(nodeType);
        expect(nodeType.getName()).andReturn("ns:testdocument");
        replay(handle, variant, nodeType);

        assertThat(DocumentUtils.getVariantNodeType(handle).get(), equalTo("ns:testdocument"));
    }

    @Test(expected = NoSuchElementException.class)
    public void getVariantNodeTypeWithoutVariant() throws Exception {
        final Node handle = createMock(HippoNode.class);

        expect(handle.getName()).andReturn("name");
        expect(handle.getNode("name")).andThrow(new RepositoryException());
        replay(handle);

        Log4jInterceptor.onWarn().deny(DocumentUtils.class).run( () -> {
            DocumentUtils.getVariantNodeType(handle).get();
        });
    }
}
