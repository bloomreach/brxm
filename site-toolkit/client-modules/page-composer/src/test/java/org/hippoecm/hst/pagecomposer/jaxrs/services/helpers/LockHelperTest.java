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

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY;
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

        this.node = createNiceMock(Node.class);
        this.child = createMock(Node.class);
        this.root = createMock(Node.class);
        this.session = createNiceMock(Session.class);
        this.nodeIterator = createMock(NodeIterator.class);
        this.lockedBy = createMock(Property.class);

        mocks = new Object[]{node, child, root, session, nodeIterator, lockedBy};
        reset(mocks);

        // Default expectations
        expect(node.getSession()).andReturn(session).anyTimes();
        expect(session.getRootNode()).andReturn(root).anyTimes();

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

        expect(node.isSame(root)).andReturn(true);

        replay(mocks);

        assertThat(lockHelper.getUnLockableNode(node, true, false), is(nullValue()));
    }

    @Test
    public void testGetSelfOrAncestorLockedBy_for_non_root_node() throws RepositoryException {

        expect(node.isSame(root)).andReturn(false);
        expect(root.isSame(root)).andReturn(true);
        expect(node.hasProperty(GENERAL_PROPERTY_LOCKED_BY)).andReturn(true);
        expect(node.getProperty(GENERAL_PROPERTY_LOCKED_BY)).andReturn(lockedBy).anyTimes();
        expect(session.getUserID()).andReturn("me").anyTimes();
        expect(lockedBy.getString()).andReturn("notMe").anyTimes();
        expect(node.getParent()).andReturn(root).anyTimes();

        replay(mocks);
        assertThat(lockHelper.getUnLockableNode(node, true, false).getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString(), is("notMe"));
    }

    @Test
    public void testHasSelfOrAncestorLockBySomeOneElse() throws RepositoryException {

        expect(node.isSame(root)).andReturn(false);
        expect(node.hasProperty(GENERAL_PROPERTY_LOCKED_BY)).andReturn(true).atLeastOnce();
        expect(node.getProperty(GENERAL_PROPERTY_LOCKED_BY)).andReturn(lockedBy).atLeastOnce();
        expect(lockedBy.getString()).andReturn("me");

        expect(session.getUserID()).andReturn("someone else").anyTimes();

        replay(mocks);

        final Node unLockableNode = lockHelper.getUnLockableNode(node, true, false);
        assertThat(unLockableNode != null, is(true));
    }

    @Test
    public void testAcquireLock() throws RepositoryException {

        expect(node.getNodes()).andReturn(nodeIterator);
        expect(nodeIterator.hasNext()).andReturn(false);

        expect(node.isSame(root)).andReturn(false);
        expect(root.isSame(root)).andReturn(true);
        expect(session.getUserID()).andReturn("userID").atLeastOnce();
        expect(node.setProperty(GENERAL_PROPERTY_LOCKED_BY, "userID")).andReturn(null);
        expect(node.setProperty(GENERAL_PROPERTY_LAST_MODIFIED_BY, "userID")).andReturn(null);
        expect(node.getParent()).andReturn(root).anyTimes();

        replay(mocks);

        lockHelper.acquireLock(node);
        verify(mocks);
    }

    @Test
    public void testAcquireSimpleLock() throws RepositoryException {

        expect(node.hasProperty(GENERAL_PROPERTY_LOCKED_BY)).andReturn(false).atLeastOnce();
        expect(node.isNodeType(MIXINTYPE_HST_EDITABLE)).andReturn(true).atLeastOnce();

        expect(session.getUserID()).andReturn("userID").atLeastOnce();
        expect(node.setProperty(GENERAL_PROPERTY_LOCKED_BY, "userID")).andReturn(null);
        expect(node.setProperty(GENERAL_PROPERTY_LAST_MODIFIED_BY, "userID")).andReturn(null);
        replay(mocks);

        lockHelper.acquireSimpleLock(node);
        verify(mocks);
    }
}
