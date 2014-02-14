/*
 * Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_EDITABLE;
import static org.junit.Assert.assertThat;

public class LockHelperTest {

    private LockHelper lockHelper;

    // Mocks
    private Node node;
    private Node child;
    private Node root;
    private Session session;
    private Object[] mocks;
    private NodeIterator nodeIterator;
    private Property lockedBy;

    @Before
    public void setUp() throws RepositoryException {

        this.node = createMock(Node.class);
        this.child = createMock(Node.class);
        this.root = createMock(Node.class);
        this.session = createMock(Session.class);
        this.nodeIterator = createMock(NodeIterator.class);
        this.lockedBy = createMock(Property.class);

        mocks = new Object[]{node, child, root, session, nodeIterator, lockedBy};
        reset(mocks);

        // Default expectations

        this.lockHelper = new LockHelper();
    }

    @Test
    public void test_unlock_non_editable_mixin() throws RepositoryException {

        expect(node.isNodeType(MIXINTYPE_HST_EDITABLE)).andReturn(false);
        expect(node.getNodes()).andReturn(nodeIterator);
        expect(nodeIterator.hasNext()).andReturn(true).andReturn(false);
        expect(nodeIterator.next()).andReturn(child);

        expect(child.isNodeType(MIXINTYPE_HST_EDITABLE)).andReturn(false);
        expect(child.getNodes()).andReturn(nodeIterator);
        expect(nodeIterator.hasNext()).andReturn(false);

        replay(mocks);

        lockHelper.unlock(node);
        verify(mocks);
    }

    @Test
    public void test_unlock_editable_mixin() throws RepositoryException {

        expect(node.isNodeType(MIXINTYPE_HST_EDITABLE)).andReturn(true);
        expect(node.getPath()).andReturn("path");
        node.removeMixin(MIXINTYPE_HST_EDITABLE);
        expectLastCall();
        expect(node.getNodes()).andReturn(nodeIterator);
        expect(nodeIterator.hasNext()).andReturn(false);

        replay(mocks);

        lockHelper.unlock(node);
        verify(mocks);
    }

    @Test
    public void testGetSelfOrAncestorLockedBy_for_root_node() throws RepositoryException {

        expect(node.getSession()).andReturn(session);
        expect(session.getRootNode()).andReturn(root);
        expect(node.isSame(root)).andReturn(true);

        replay(mocks);

        assertThat(lockHelper.getSelfOrAncestorLockedBy(node), is(nullValue()));
    }

    @Test
    public void testGetSelfOrAncestorLockedBy_for_non_root_node() throws RepositoryException {

        expect(node.getSession()).andReturn(session);
        expect(session.getRootNode()).andReturn(root);
        expect(node.isSame(root)).andReturn(false);
        expect(node.hasProperty(GENERAL_PROPERTY_LOCKED_BY)).andReturn(true);
        expect(node.getProperty(GENERAL_PROPERTY_LOCKED_BY)).andReturn(lockedBy);
        expect(lockedBy.getString()).andReturn("me");

        replay(mocks);

        assertThat(lockHelper.getSelfOrAncestorLockedBy(node), is("me"));
    }

    @Test
    public void testHasSelfOrAncestorLockBySomeOneElse() throws RepositoryException {

        expect(node.getSession()).andReturn(session);
        expect(session.getRootNode()).andReturn(root);
        expect(node.isSame(root)).andReturn(false);
        expect(node.hasProperty(GENERAL_PROPERTY_LOCKED_BY)).andReturn(true);
        expect(node.getProperty(GENERAL_PROPERTY_LOCKED_BY)).andReturn(lockedBy);
        expect(lockedBy.getString()).andReturn("me");

        expect(node.getSession()).andReturn(session);
        expect(session.getUserID()).andReturn("someone else");

        replay(mocks);

        assertThat(lockHelper.hasSelfOrAncestorLockBySomeOneElse(node), is(true));
    }

    @Test
    public void testAcquireLock() throws RepositoryException {
        // TODO implement
        // lockHelper.acquireLock(node);
    }

    @Test
    public void testAcquireSimpleLock() throws RepositoryException {
        // TODO implement
        // lockHelper.acquireSimpleLock(node);
    }
}
