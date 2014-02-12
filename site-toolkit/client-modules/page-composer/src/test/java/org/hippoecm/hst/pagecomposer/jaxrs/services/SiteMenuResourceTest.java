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

import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.MockSiteMenuConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.MockSiteMenuItemConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMenuHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMenuItemHelper;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_SITEMENUITEM;
import static org.junit.Assert.assertThat;

public class SiteMenuResourceTest {

    public static final int OK = Response.Status.OK.getStatusCode();
    public static final int SERVER_ERROR = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    public static final int BAD_REQUEST = Response.Status.BAD_REQUEST.getStatusCode();
    // class under test
    private SiteMenuResource siteMenuResource;

    // mocks
    private SiteMenuHelper siteMenuHelper;
    private SiteMenuItemHelper siteMenuItemHelper;
    private HttpServletRequest request;
    private Object[] mocks;
    private HstRequestContext context;
    private Session session;
    private HttpSession httpSession;
    private VirtualHost virtualHost;
    private VirtualHosts virtualHosts;
    private ContextualizableMount mount;
    private HstSite site;
    private MockSiteMenuConfiguration menuConfig;
    private MockSiteMenuItemConfiguration itemConfig;
    private PageComposerContextService pageComposerContextService;
    private Node node, parentNode, childNode;
    private NodeIterator nodeIterator;

    @Before
    public void setUp() {
        this.siteMenuHelper = createMock(SiteMenuHelper.class);
        this.siteMenuItemHelper = createMock(SiteMenuItemHelper.class);
        this.request = createMock(HttpServletRequest.class);
        this.context = createMock(HstRequestContext.class);
        this.session = createMock(Session.class);
        this.httpSession = createMock(HttpSession.class);
        this.virtualHost = createMock(VirtualHost.class);
        this.virtualHosts = createMock(VirtualHosts.class);
        this.mount = createMock(ContextualizableMount.class);
        this.site = createMock(HstSite.class);
        this.menuConfig = createMock(MockSiteMenuConfiguration.class);
        this.itemConfig = createNiceMock(MockSiteMenuItemConfiguration.class);
        this.node = createMock(Node.class);
        this.parentNode = createMock(Node.class);
        this.childNode = createMock(Node.class);
        this.pageComposerContextService = createMock(PageComposerContextService.class);
        this.nodeIterator = createMock(NodeIterator.class);
        this.mocks = new Object[]{siteMenuHelper, siteMenuItemHelper, request, context, session, httpSession, virtualHost, virtualHosts, mount, site, menuConfig, itemConfig, node, parentNode, childNode, pageComposerContextService, nodeIterator};

        this.siteMenuResource = new SiteMenuResource();
        this.siteMenuResource.setSiteMenuHelper(siteMenuHelper);
        this.siteMenuResource.setSiteMenuItemHelper(siteMenuItemHelper);
        this.siteMenuResource.setPageComposerContextService(pageComposerContextService);
    }

    @Test
    public void testGetMenu() throws RepositoryException {
        mockGetPreviewSite();
        mockGetSiteMenu("menuId");
        expect(menuConfig.getCanonicalIdentifier()).andReturn("uuid-of-menu");
        expect(menuConfig.getName()).andReturn("menuMane");
        final List<HstSiteMenuItemConfiguration> children = Collections.emptyList();
        expect(menuConfig.getSiteMenuConfigurationItems()).andReturn(children);
        replay(mocks);

        final Response response = siteMenuResource.getMenu();
        assertThat(response.getStatus(), is(OK));
        final ExtResponseRepresentation entity = ExtResponseRepresentation.class.cast(response.getEntity());
        assertThat(entity.getData(), is(SiteMenuRepresentation.class));
        assertThat(entity.isSuccess(), is(true));
    }

    @Test
    public void testGetMenuItem() throws RepositoryException {
        mockGetPreviewSite();
        mockGetSiteMenu("menuId");
        final String id = "uuid-of-menu-item";
        mockGetMenuItem(node, id);
        replay(mocks);

        final Response response = siteMenuResource.getMenuItem(id);
        assertThat(response.getStatus(), is(OK));
        final ExtResponseRepresentation entity = ExtResponseRepresentation.class.cast(response.getEntity());
        assertThat(entity.getData(), is(SiteMenuItemRepresentation.class));
        assertThat(entity.isSuccess(), is(true));
    }

    @Test
    public void testCreateAsChildOfMenu() throws RepositoryException {

        mockGetPreviewSite();
        final String menuId = "uuid-of-menu";
        mockGetSiteMenu(menuId);

        expect(menuConfig.getCanonicalIdentifier()).andReturn(menuId);
        expect(session.getNodeByIdentifier(menuId)).andReturn(parentNode);

        final String name = "menuItemName";
        expect(parentNode.addNode(name, NODETYPE_HST_SITEMENUITEM)).andReturn(node);

        // Mock creating the site menu item
        final SiteMenuItemRepresentation newMenuItem = new SiteMenuItemRepresentation();
        newMenuItem.setName(name);
        expect(siteMenuItemHelper.create(parentNode, newMenuItem)).andReturn(node);

        final String menuItemId = "menuItemId";
        expect(node.getIdentifier()).andReturn(menuItemId);

        replay(mocks);

        final Response response = siteMenuResource.create(menuId, newMenuItem);

        assertThat(response.getStatus(), is(OK));
        assertThat(response.getEntity(), is(ExtResponseRepresentation.class));

        final ExtResponseRepresentation extResponse = ExtResponseRepresentation.class.cast(response.getEntity());
        assertThat(extResponse.isSuccess(), is(true));
        assertThat(extResponse.getData().toString(), is(menuItemId));
    }

    @Test
    public void testUpdate() throws RepositoryException {

        mockGetPreviewSite();
        mockGetSiteMenu("menuId");

        final String id = "uuid-of-menu-item";
        mockGetMenuItem(node, id);

        // Mock updating the site menu item
        final SiteMenuItemRepresentation modifiedItem = new SiteMenuItemRepresentation();
        modifiedItem.setId(id);
        siteMenuItemHelper.update(node, modifiedItem);
        expectLastCall().once();
        replay(mocks);

        final Response response = siteMenuResource.update(modifiedItem);

        assertThat(response.getStatus(), is(OK));
        assertThat(response.getEntity(), is(ExtResponseRepresentation.class));

        final ExtResponseRepresentation extResponse = ExtResponseRepresentation.class.cast(response.getEntity());
        assertThat(extResponse.isSuccess(), is(true));
        assertThat(extResponse.getData().toString(), is(id));
    }

    @Test
    public void testUpdateReturnsServerErrorOnRepositoryException() throws RepositoryException {
        expect(pageComposerContextService.getRequestConfigIdentifier()).andReturn(null).anyTimes();
        expect(pageComposerContextService.getRequestContext()).andReturn(context).times(2);
        expect(context.getSession()).andThrow(new RepositoryException("failed"));
        expect(context.getSession()).andReturn(session);
        expect(session.hasPendingChanges()).andReturn(false);

        final String id = "uuid-of-menu-item";
        final SiteMenuItemRepresentation modifiedItem = new SiteMenuItemRepresentation();
        modifiedItem.setId(id);
        replay(mocks);

        final Response response = siteMenuResource.update(modifiedItem);
        assertThat(response.getStatus(), is(SERVER_ERROR));
        assertThat(response.getEntity(), is(ExtResponseRepresentation.class));

        final ExtResponseRepresentation extResponse = ExtResponseRepresentation.class.cast(response.getEntity());
        assertThat(extResponse.isSuccess(), is(false));
        assertThat(extResponse.getData(), is(String[].class));

    }

    @Test
    public void testUpdateReturnsClientErrorOnIllegalStateException() throws RepositoryException {
        expect(pageComposerContextService.getRequestConfigIdentifier()).andReturn(null).anyTimes();
        expect(pageComposerContextService.getRequestContext()).andReturn(context).times(2);
        expect(context.getSession()).andThrow(new IllegalStateException("failed"));
        expect(context.getSession()).andReturn(session);
        expect(session.hasPendingChanges()).andReturn(false);

        final String id = "uuid-of-menu-item";
        final SiteMenuItemRepresentation modifiedItem = new SiteMenuItemRepresentation();
        modifiedItem.setId(id);
        replay(mocks);

        final Response response = siteMenuResource.update(modifiedItem);
        assertThat(response.getStatus(), is(BAD_REQUEST));
        assertThat(response.getEntity(), is(ExtResponseRepresentation.class));

        final ExtResponseRepresentation extResponse = ExtResponseRepresentation.class.cast(response.getEntity());
        assertThat(extResponse.isSuccess(), is(false));
        assertThat(extResponse.getData(), is(String[].class));

    }

    @Test
    public void testMove() throws RepositoryException {

        mockGetPreviewSite();
        mockGetSiteMenu("menuId");

        final String sourceId = "sourceId";
        mockGetMenuItem(node, sourceId);

        final Integer childTargetIndex = 0;

        final String parentTargetId = "parentId";
        expect(menuConfig.getCanonicalIdentifier()).andReturn("parentId");
        expect(session.getNodeByIdentifier(parentTargetId)).andReturn(parentNode);
        expect(parentNode.isSame(parentNode)).andReturn(true);
        expect(parentNode.getIdentifier()).andReturn(parentTargetId);

        expect(nodeIterator.hasNext()).andReturn(true).times(2).andReturn(false);
        expect(nodeIterator.next()).andReturn(childNode).andReturn(node);
        expect(parentNode.getNodes()).andReturn(nodeIterator);

        expect(node.getParent()).andReturn(parentNode);
        expect(node.getName()).andReturn("src").anyTimes();
        expect(childNode.getName()).andReturn("dest").anyTimes();
        parentNode.orderBefore("src", "dest");
        expectLastCall().once();
        replay(mocks);

        final Response response = siteMenuResource.move(sourceId, parentTargetId, childTargetIndex);
        assertThat(response.getStatus(), is(OK));
        assertThat(response.getEntity(), is(ExtResponseRepresentation.class));

        final ExtResponseRepresentation extResponse = ExtResponseRepresentation.class.cast(response.getEntity());
        assertThat(extResponse.isSuccess(), is(true));
        assertThat(extResponse.getData().toString(), is(sourceId));
    }

    @Test
    public void testDelete() throws RepositoryException {

        mockGetPreviewSite();
        mockGetSiteMenu("menuId");
        final String sourceId = "sourceId";
        mockGetMenuItem(node, sourceId);
        expect(node.getSession()).andReturn(session);
        node.remove();
        expectLastCall().once();
        replay(mocks);

        final Response response = siteMenuResource.delete(sourceId);
        assertThat(response.getStatus(), is(OK));
        assertThat(response.getEntity(), is(ExtResponseRepresentation.class));

        final ExtResponseRepresentation extResponse = ExtResponseRepresentation.class.cast(response.getEntity());
        assertThat(extResponse.isSuccess(), is(true));
        assertThat(extResponse.getData().toString(), is(sourceId));
    }

    private String mockGetMenuItem(Node node, String id) throws RepositoryException {
        // Mock getting the site menu item and the corresponding node
        expect(siteMenuHelper.getMenuItem(menuConfig, id)).andReturn(itemConfig);
        expect(itemConfig.getCanonicalIdentifier()).andReturn(id);
        expect(itemConfig.getChildItemConfigurations()).andReturn(Collections.<HstSiteMenuItemConfiguration>emptyList());
        expect(session.getNodeByIdentifier(id)).andReturn(node);
        return id;
    }

    private void mockGetSiteMenu(String menuId) {
        // Mock getting the site menu
        expect(pageComposerContextService.getRequestConfigIdentifier()).andReturn(menuId).anyTimes();
        expect(pageComposerContextService.getRequestContext()).andReturn(context);
        expect(context.getAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER)).andReturn(menuId).anyTimes();
        expect(siteMenuHelper.getMenu(site, menuId)).andReturn(menuConfig);
    }

    private void mockGetPreviewSite() throws RepositoryException {
        // Due to the inheritance the following mock calls are required to get the preview site
        expect(context.getSession()).andReturn(session);
        expect(context.getServletRequest()).andReturn(request);
        expect(request.getSession(true)).andReturn(httpSession);
        expect(httpSession.getAttribute(ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID)).andReturn("mount");
        expect(context.getVirtualHost()).andReturn(virtualHost);
        expect(virtualHost.getVirtualHosts()).andReturn(virtualHosts);
        expect(virtualHosts.getMountByIdentifier("mount")).andReturn(mount);
        expect(mount.getPreviewHstSite()).andReturn(site);
        expect(pageComposerContextService.getEditingPreviewSite()).andReturn(site).anyTimes();
    }

}
