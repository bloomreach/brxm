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
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_EXTERNALLINK;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_REPOBASED;

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
    public void testSave() throws RepositoryException {

        final SiteMenuItemRepresentation newItem = new SiteMenuItemRepresentation();
        newItem.setName("name");
        newItem.setExternalLink("externalLink");
        newItem.setSiteMapItemPath("siteMapItemPath");

        expect(node.getName()).andReturn("name").times(2);
        expect(node.setProperty(SITEMENUITEM_PROPERTY_EXTERNALLINK, newItem.getExternalLink())).andReturn(null);
        expect(node.setProperty(SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM, newItem.getSiteMapItemPath())).andReturn(null);
        expect(node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, false)).andReturn(null);

        replay(mocks);
        siteMenuItemHelper.save(node, newItem);
    }

    @Test(expected = AssertionError.class)
    public void testSaveFailsIfNamesAreDifferent() throws RepositoryException {

        final SiteMenuItemRepresentation newItem = new SiteMenuItemRepresentation();
        newItem.setName("a");

        expect(node.getName()).andReturn("b");
        replay(mocks);
        siteMenuItemHelper.save(node, newItem);
    }

    @Test
    public void testUpdateExternalLink() throws RepositoryException {

        final String newExternalLink = "link";
        expect(node.setProperty(SITEMENUITEM_PROPERTY_EXTERNALLINK, newExternalLink)).andReturn(null);
        expect(node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, false)).andReturn(null);

        final SiteMenuItemRepresentation modifiedItem = new SiteMenuItemRepresentation();
        modifiedItem.setExternalLink(newExternalLink);

        replay(mocks);
        siteMenuItemHelper.update(node, modifiedItem);
        verify(mocks);
    }

    @Test
    public void testUpdateSiteMapItemPath() throws RepositoryException {

        final String newSiteMapItemPath = "link";
        expect(node.setProperty(SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM, newSiteMapItemPath)).andReturn(null);
        expect(node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, false)).andReturn(null);

        final SiteMenuItemRepresentation newItem = new SiteMenuItemRepresentation();
        newItem.setSiteMapItemPath(newSiteMapItemPath);

        replay(mocks);
        siteMenuItemHelper.update(node, newItem);
        verify(mocks);
    }

    @Test
    public void testUpdateRepositoryBased() throws RepositoryException {

        expect(node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, true)).andReturn(null);

        final SiteMenuItemRepresentation modifiedItem = new SiteMenuItemRepresentation();
        modifiedItem.setRepositoryBased(true);

        replay(mocks);
        siteMenuItemHelper.update(node, modifiedItem);
        verify(mocks);
    }

    @Test
    public void testUpdateName() throws RepositoryException {

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
}
