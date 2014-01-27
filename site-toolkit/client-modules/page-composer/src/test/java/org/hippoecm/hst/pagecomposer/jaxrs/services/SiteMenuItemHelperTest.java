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

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
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

    @Before
    public void setUp() {
        this.siteMenuItemHelper = new SiteMenuItemHelper();
        this.node = createMock(Node.class);
        this.parent = createMock(Node.class);
        this.childIterator = createMock(NodeIterator.class);
        this.sibling = createMock(Node.class);
        this.session = createMock(Session.class);
        this.mocks = new Object[]{node, parent, childIterator, sibling, session};
    }

    @Test
    public void testUpdateExtenalLink() throws RepositoryException {

        final String newExternalLink = "link";
        expect(node.setProperty(SiteMenuItemHelper.HST_EXTERNALLINK, newExternalLink)).andReturn(null);

        final SiteMenuItemRepresentation currentItem = new SiteMenuItemRepresentation();
        final SiteMenuItemRepresentation newItem = new SiteMenuItemRepresentation();
        newItem.setExternalLink(newExternalLink);

        replay(mocks);
        siteMenuItemHelper.update(node, currentItem, newItem);
        assertThat(currentItem.getExternalLink(), is(newExternalLink));
        verify(mocks);
    }

    @Test
    public void testUpdateSiteMapItemPath() throws RepositoryException {

        final String newSitemmapItemPath = "link";
        expect(node.setProperty(SiteMenuItemHelper.HST_REFERENCESITEMAPITEM, newSitemmapItemPath)).andReturn(null);

        final SiteMenuItemRepresentation currentItem = new SiteMenuItemRepresentation();
        final SiteMenuItemRepresentation newItem = new SiteMenuItemRepresentation();
        newItem.setSiteMapItemPath(newSitemmapItemPath);

        replay(mocks);
        siteMenuItemHelper.update(node, currentItem, newItem);
        assertThat(currentItem.getSiteMapItemPath(), is(newSitemmapItemPath));
        verify(mocks);
    }

    @Test
    public void testUpdateName() throws RepositoryException {

        expect(node.getSession()).andReturn(session);
        expect(node.getParent()).andReturn(parent);

        final String oldName = "hst";
        final String pathPrefix = "hst:hst/hst/hst/hst/";
        expect(node.getPath()).andReturn(pathPrefix + oldName);
        expect(node.getName()).andReturn(oldName);
        expect(node.getIndex()).andReturn(0);

        expect(parent.getNodes()).andReturn(childIterator);
        expect(childIterator.hasNext()).andReturn(true).times(2).andReturn(false);
        expect(childIterator.next()).andReturn(sibling).times(2);
        final String siblingName = "someSiblingName";
        expect(sibling.getName()).andReturn(siblingName);

        final String newName = "newName";
        session.move(pathPrefix + oldName, pathPrefix + newName);
        expectLastCall().once();

        parent.orderBefore(newName, siblingName);
        expectLastCall().once();

        replay(mocks);
        final SiteMenuItemRepresentation currentItem = new SiteMenuItemRepresentation();
        final SiteMenuItemRepresentation newItem = new SiteMenuItemRepresentation();
        newItem.setName(newName);
        siteMenuItemHelper.update(node, currentItem, newItem);
        assertThat(currentItem.getName(), is(newName));
        verify(mocks);
    }
}
