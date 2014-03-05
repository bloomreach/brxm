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

import java.util.HashMap;
import java.util.HashSet;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.easymock.Capture;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.LinkType;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_SITEMENU;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_SITEMENUITEM;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_EXTERNALLINK;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_REPOBASED;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_ROLES;
import static org.junit.Assert.assertThat;

public class SiteMenuItemHelperTest {

    // Class under test
    private SiteMenuItemHelper siteMenuItemHelper;

    // Mocks
    private Node node;
    private Node parent;
    private NodeIterator childIterator;
    private Node sibling;
    private Session session;
    private Object[] mocks;
    private Property property;
    private LockHelper lockHelper;

    @Before
    public void setUp() {
        this.node = createMock(Node.class);
        this.parent = createMock(Node.class);
        this.childIterator = createMock(NodeIterator.class);
        this.sibling = createMock(Node.class);
        this.session = createMock(Session.class);
        this.property = createMock(Property.class);
        this.lockHelper = createMock(LockHelper.class);
        this.mocks = new Object[]{node, parent, childIterator, sibling, session, property, lockHelper};

        this.siteMenuItemHelper = new SiteMenuItemHelper();
        this.siteMenuItemHelper.setLockHelper(lockHelper);
    }

    @Test
    public void testSave() throws RepositoryException {

        final SiteMenuItemRepresentation newItem = new SiteMenuItemRepresentation();
        newItem.setName("name");

        // once for adding the new item to the parent
        mockGetAncestor();
        // once for updating the new item
        mockGetAncestor();
        expect(node.addNode(newItem.getName(), HstNodeTypes.NODETYPE_HST_SITEMENUITEM)).andReturn(node);
        expect(node.getName()).andReturn(newItem.getName());
        expect(node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, false)).andReturn(null);
        replay(mocks);

        final Node result = siteMenuItemHelper.create(node, newItem, Position.ANY);
        assertThat(result, is(node));
    }

    @Test
    public void testUpdateRepositoryBased() throws RepositoryException {

        final boolean repositoryBased = true;

        mockGetAncestor();
        expect(node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, repositoryBased)).andReturn(null);
        replay(mocks);

        final SiteMenuItemRepresentation modifiedItem = new SiteMenuItemRepresentation();
        modifiedItem.setRepositoryBased(repositoryBased);
        siteMenuItemHelper.update(node, modifiedItem);
        verify(mocks);
    }

    @Test
    public void testUpdateRoles() throws RepositoryException {

        final SiteMenuItemRepresentation modifiedItem = new SiteMenuItemRepresentation();
        modifiedItem.setRoles(new HashSet<String>());
        modifiedItem.getRoles().add("role");

        mockGetAncestor();
        final Capture<String[]> roles = new Capture<>();
        expect(node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, false)).andReturn(null);
        expect(node.setProperty(eq(SITEMENUITEM_PROPERTY_ROLES), capture(roles), eq(PropertyType.STRING))).andReturn(null);
        replay(mocks);

        siteMenuItemHelper.update(node, modifiedItem);
        assertThat(roles.getValue()[0], is("role"));
        verify(mocks);
    }

    @Test
    public void testUpdateLocalParameters() throws RepositoryException {

        final SiteMenuItemRepresentation modifiedItem = new SiteMenuItemRepresentation();
        modifiedItem.setLocalParameters(new HashMap<String, String>());
        modifiedItem.getLocalParameters().put("name", "value");

        mockGetAncestor();
        expect(node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, false)).andReturn(null);
        final Capture<String[]> names = new Capture<>();
        expect(node.setProperty(eq(GENERAL_PROPERTY_PARAMETER_NAMES), capture(names), eq(PropertyType.STRING))).andReturn(null);
        final Capture<String[]> values = new Capture<>();
        expect(node.setProperty(eq(GENERAL_PROPERTY_PARAMETER_VALUES), capture(values), eq(PropertyType.STRING))).andReturn(null);
        replay(mocks);

        siteMenuItemHelper.update(node, modifiedItem);
        assertThat(names.getValue()[0], is("name"));
        assertThat(values.getValue()[0], is("value"));
        verify(mocks);
    }

    @Test
    public void testUpdateName() throws RepositoryException {

        // once for update
        mockGetAncestor();
        // once for rename
        mockGetAncestor();
        // once for move
        mockGetAncestor();

        expect(node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, false)).andReturn(null);
        expect(node.getSession()).andReturn(session);
        expect(node.getParent()).andReturn(parent);

        final String oldName = "hst";
        final String pathPrefix = "hst:hst/hst/hst/hst";
        expect(node.getName()).andReturn(oldName).times(3);
        expect(node.getPath()).andReturn(pathPrefix + "/" + oldName);

        expect(parent.getNodes()).andReturn(childIterator);
        expect(parent.getPath()).andReturn(pathPrefix);
        expect(childIterator.hasNext()).andReturn(true).times(2);
        expect(childIterator.next()).andReturn(node).andReturn(sibling);
        final String siblingName = "someSiblingName";
        expect(sibling.getName()).andReturn(siblingName);

        final String newName = "newName";
        session.move(pathPrefix + "/" + oldName, pathPrefix + "/" + newName);
        expectLastCall().once();

        parent.orderBefore(newName, siblingName);
        expectLastCall().once();
        replay(mocks);

        final SiteMenuItemRepresentation newItem = new SiteMenuItemRepresentation();
        newItem.setName(newName);
        siteMenuItemHelper.update(node, newItem);
        verify(mocks);
    }

    @Test
    public void testUpdateName_throws_client_exception() throws RepositoryException {

        // once for update
        mockGetAncestor();
        // once for rename
        mockGetAncestor();
        // once for move
        mockGetAncestor();

        expect(node.getSession()).andReturn(session);
        expect(node.getParent()).andReturn(parent);

        final String oldName = "hst";
        final String pathPrefix = "hst:hst/hst/hst/hst";
        expect(node.getName()).andReturn(oldName).anyTimes();
        expect(node.getPath()).andReturn(pathPrefix + "/" + oldName);

        expect(parent.getNodes()).andReturn(childIterator);
        expect(parent.getPath()).andReturn(pathPrefix);
        expect(parent.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMENU)).andReturn(false).andReturn(true);
        expect(parent.getParent()).andReturn(parent);
        expect(parent.getName()).andReturn("parent").anyTimes();
        expect(childIterator.hasNext()).andReturn(true).times(2);
        expect(childIterator.next()).andReturn(node).andReturn(sibling);
        final String siblingName = "someSiblingName";
        expect(sibling.getName()).andReturn(siblingName);

        final String newName = "newName";
        session.move(pathPrefix + "/" + oldName, pathPrefix + "/" + newName);
        expectLastCall().andThrow(new ItemExistsException());

        replay(mocks);

        final SiteMenuItemRepresentation newItem = new SiteMenuItemRepresentation();
        newItem.setName(newName);
        try {
            siteMenuItemHelper.update(node, newItem);
        } catch (ClientException e) {
            assertThat(e.getError(), is(ClientError.ITEM_NAME_NOT_UNIQUE));
            assertThat(e.getParameterMap().get("item").toString(), is(newName));
            assertThat(e.getParameterMap().get("parentPath").toString(), is("/parent"));
        }
        verify(mocks);
    }

    @Test
    public void testUpdateLinkToExternal() throws RepositoryException {

        mockGetAncestor();
        expect(node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, false)).andReturn(null);

        final Capture<String> linkCapture = new Capture<>();
        expect(node.setProperty(eq(SITEMENUITEM_PROPERTY_EXTERNALLINK), capture(linkCapture))).andReturn(null);

        expect(node.hasProperty(SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM)).andReturn(true);
        expect(node.getProperty(SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM)).andReturn(property);

        property.remove();
        expectLastCall().once();

        replay(mocks);

        final SiteMenuItemRepresentation modifiedItem = new SiteMenuItemRepresentation();
        modifiedItem.setLinkType(LinkType.EXTERNAL);
        final String link = "external";
        modifiedItem.setLink(link);
        siteMenuItemHelper.update(node, modifiedItem);
        assertThat(linkCapture.getValue(), is(link));

        verify(mocks);
    }


    @Test
    public void testUpdateLinkToInternal() throws RepositoryException {

        mockGetAncestor();
        expect(node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, false)).andReturn(null);

        final Capture<String> linkCapture = new Capture<>();
        expect(node.setProperty(eq(SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM), capture(linkCapture))).andReturn(null);

        expect(node.hasProperty(SITEMENUITEM_PROPERTY_EXTERNALLINK)).andReturn(true);
        expect(node.getProperty(SITEMENUITEM_PROPERTY_EXTERNALLINK)).andReturn(property);
        property.remove();
        expectLastCall().once();

        replay(mocks);

        final SiteMenuItemRepresentation modifiedItem = new SiteMenuItemRepresentation();
        modifiedItem.setLinkType(LinkType.SITEMAPITEM);
        final String link = "internal";
        modifiedItem.setLink(link);
        siteMenuItemHelper.update(node, modifiedItem);
        assertThat(linkCapture.getValue(), is(link));

        verify(mocks);
    }

    @Test
    public void testUpdateLinkToNone() throws RepositoryException {

        mockGetAncestor();
        expect(node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, false)).andReturn(null);
        expect(node.hasProperty(SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM)).andReturn(false);
        expect(node.hasProperty(SITEMENUITEM_PROPERTY_EXTERNALLINK)).andReturn(true);
        expect(node.getProperty(SITEMENUITEM_PROPERTY_EXTERNALLINK)).andReturn(property);
        property.remove();
        expectLastCall().once();

        replay(mocks);

        final SiteMenuItemRepresentation modifiedItem = new SiteMenuItemRepresentation();
        modifiedItem.setLinkType(LinkType.NONE);
        siteMenuItemHelper.update(node, modifiedItem);

        verify(mocks);
    }

    @Test
    public void testCreate_throws_client_exception_if_name_exists() throws RepositoryException {

        mockGetAncestor();
        final String name = "name";
        expect(node.getName()).andReturn(name).anyTimes();
        expect(node.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMENU)).andReturn(true);
        expect(node.addNode(name, NODETYPE_HST_SITEMENUITEM)).andThrow(new ItemExistsException(""));
        replay(mocks);

        try {
            final SiteMenuItemRepresentation newItem = new SiteMenuItemRepresentation();
            newItem.setName(name);
            siteMenuItemHelper.create(node, newItem, Position.ANY);
        } catch (ClientException e) {
            assertThat(e.getParameterMap().get("item").toString(), is(name));
            assertThat(e.getParameterMap().get("parentPath").toString(), is(""));
            assertThat(e.getError(), is(ClientError.ITEM_NAME_NOT_UNIQUE_IN_ROOT));
        }
    }

    @Test
    public void testCreate_first_reorders_children() throws RepositoryException {

        expect(parent.isNodeType(NODETYPE_HST_SITEMENUITEM)).andReturn(false);
        expect(parent.isNodeType(NODETYPE_HST_SITEMENU)).andReturn(true);
        lockHelper.acquireSimpleLock(parent);
        expectLastCall();

        final String name = "name";
        expect(parent.getName()).andReturn(name).anyTimes();
        expect(parent.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMENU)).andReturn(true).anyTimes();
        expect(parent.addNode(name, NODETYPE_HST_SITEMENUITEM)).andReturn(node);
        expect(parent.getNodes()).andReturn(childIterator).anyTimes();
        expect(childIterator.getSize()).andReturn(2L);
        expect(childIterator.nextNode()).andReturn(sibling);
        expect(node.getName()).andReturn(name).anyTimes();
        expect(node.getIndex()).andReturn(1);
        expect(sibling.getName()).andReturn("sibling").anyTimes();
        expect(sibling.getIndex()).andReturn(1);
        parent.orderBefore("name[1]", "sibling[1]");

        mockGetAncestor();

        expect(node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, false)).andReturn(property);

        replay(mocks);

        final SiteMenuItemRepresentation newItem = new SiteMenuItemRepresentation();
        newItem.setName(name);
        siteMenuItemHelper.create(parent, newItem, Position.FIRST);

        verify(mocks);
    }

    @Test
    public void testMove_to_front_within_parent() throws RepositoryException {

        expect(node.getName()).andReturn("node");

        expect(parent.getNodes()).andReturn(childIterator);
        expect(childIterator.hasNext()).andReturn(true).andReturn(true).andReturn(false);
        expect(childIterator.next()).andReturn(node).andReturn(node);
        expect(node.getName()).andReturn("child-0").andReturn("node");

        expect(node.getParent()).andReturn(parent);
        expect(parent.isSame(parent)).andReturn(true);

        mockGetAncestor();
        expectLastCall();
        parent.orderBefore("node", "child-0");
        expectLastCall();

        replay(mocks);

        siteMenuItemHelper.move(parent, node, 0);
    }

    @Test
    public void testMove_to_front_in_other_parent() throws RepositoryException {

        expect(node.getName()).andReturn("node");

        expect(parent.getNodes()).andReturn(childIterator);
        expect(childIterator.hasNext()).andReturn(true).andReturn(true).andReturn(false);
        expect(childIterator.next()).andReturn(node).andReturn(node);
        expect(node.getName()).andReturn("child-0").andReturn("node");

        expect(node.getParent()).andReturn(parent);
        expect(parent.isSame(parent)).andReturn(false);

        mockGetAncestor();
        expectLastCall();
        expect(node.getSession()).andReturn(session);
        expect(node.getPath()).andReturn("/a/node");
        expect(parent.getPath()).andReturn("/b");
        session.move("/a/node", "/b/node");
        expectLastCall();

        mockGetAncestor();
        expectLastCall();
        parent.orderBefore("node", "child-0");
        expectLastCall();

        replay(mocks);

        siteMenuItemHelper.move(parent, node, 0);
    }

    private void mockGetAncestor() throws RepositoryException {
        expect(node.isNodeType(NODETYPE_HST_SITEMENUITEM)).andReturn(false);
        expect(node.isNodeType(NODETYPE_HST_SITEMENU)).andReturn(true);
        lockHelper.acquireSimpleLock(node);
        expectLastCall();
    }

}
