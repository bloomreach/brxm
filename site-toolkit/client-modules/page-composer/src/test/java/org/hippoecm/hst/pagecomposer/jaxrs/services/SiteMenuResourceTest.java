/*
 * Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import com.google.common.base.Predicate;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.platform.configuration.sitemenu.HstSiteMenuConfigurationService;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.mock.configuration.MockSiteMap;
import org.hippoecm.hst.mock.configuration.MockSiteMenuItemConfiguration;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.Position;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMenuHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMenuItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.Validator;
import org.hippoecm.hst.pagecomposer.jaxrs.services.validators.ValidatorFactory;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_SITEMENUITEM;
import static org.junit.Assert.assertThat;

public class SiteMenuResourceTest {

    public static final int OK = Response.Status.OK.getStatusCode();
    public static final int SERVER_ERROR = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    // class under test
    private SiteMenuResource siteMenuResource;

    // mocks
    private ValidatorFactory validatorFactory;
    private Validator validator;
    private SiteMenuHelper siteMenuHelper;
    private SiteMenuItemHelper siteMenuItemHelper;
    private HttpServletRequest request;
    private Object[] mocks;
    private HstRequestContext context;
    private Session session;
    private HttpSession httpSession;
    private VirtualHost virtualHost;
    private VirtualHosts virtualHosts;
    private Mount mount;
    private HstSite site;
    private MockSiteMap siteMap;
    private HstSiteMenuConfigurationService menuConfig;
    private MockSiteMenuItemConfiguration itemConfig;
    private PageComposerContextService pageComposerContextService;
    private Node node;
    private Node parentNode;

    @Before
    public void setUp() {
        this.validatorFactory = createNiceMock(ValidatorFactory.class);
        this.validator = createNiceMock(Validator.class);
        this.siteMenuHelper = createMock(SiteMenuHelper.class);
        this.siteMenuItemHelper = createMock(SiteMenuItemHelper.class);
        this.request = createMock(HttpServletRequest.class);
        this.context = createMock(HstRequestContext.class);
        this.session = createMock(Session.class);
        this.httpSession = createMock(HttpSession.class);
        this.virtualHost = createMock(VirtualHost.class);
        this.virtualHosts = createMock(VirtualHosts.class);
        this.mount = createMock(Mount.class);
        this.site = createMock(HstSite.class);
        this.siteMap = createMock(MockSiteMap.class);
        this.menuConfig = createMock(HstSiteMenuConfigurationService.class);
        this.itemConfig = createNiceMock(MockSiteMenuItemConfiguration.class);
        this.node = createMock(Node.class);
        this.parentNode = createMock(Node.class);
        final Node childNode = createMock(Node.class);
        this.pageComposerContextService = createMock(PageComposerContextService.class);
        final NodeIterator nodeIterator = createMock(NodeIterator.class);
        this.mocks = new Object[]{validator, validatorFactory, siteMenuHelper, siteMenuItemHelper, request, context, session, httpSession, virtualHost, virtualHosts, mount, site, menuConfig, itemConfig, node, parentNode, childNode, pageComposerContextService, nodeIterator};

        this.siteMenuResource = new SiteMenuResource();
        this.siteMenuResource.setValidatorFactory(validatorFactory);
        this.siteMenuResource.setSiteMenuHelper(siteMenuHelper);
        this.siteMenuResource.setSiteMenuItemHelper(siteMenuItemHelper);
        this.siteMenuResource.setPageComposerContextService(pageComposerContextService);
    }

    @Test
    public void get_menu() throws RepositoryException {
        mockCreateMandatoryWorkspaceNodesIfMissing();
        mockGetPreviewSite();
        mockGetPreValidators();
        mockGetSiteMenu("menuId");
        mockMenuConfig(null, null);
        expect(session.hasPendingChanges()).andReturn(false);
        expect(pageComposerContextService.getEditingMount()).andReturn(mount);
        expect(pageComposerContextService.getSiteContentIdentifier(mount)).andReturn("uuid-of-content");
        expect(mount.getHstSite()).andReturn(site);
        expect(site.getSiteMap()).andReturn(siteMap);
        expect(siteMap.getCanonicalIdentifier()).andReturn("uuid-of-siteMap");
        replay(mocks);

        final Response response = siteMenuResource.getMenu();
        assertThat(response.getStatus(), is(OK));
        final ExtResponseRepresentation entity = ExtResponseRepresentation.class.cast(response.getEntity());
        assertThat(entity.getData(), is(instanceOf(SiteMenuRepresentation.class)));
        assertThat(entity.isSuccess(), is(true));
    }

    private void mockMenuConfig(final String lockedBy, final Calendar lockedOn) {
        expect(menuConfig.getCanonicalIdentifier()).andReturn("uuid-of-menu");
        expect(menuConfig.getName()).andReturn("menuMane");
        expect(menuConfig.getLockedBy()).andReturn(lockedBy);
        expect(menuConfig.getLockedOn()).andReturn(lockedOn);
        final List<HstSiteMenuItemConfiguration> children = Collections.emptyList();
        expect(menuConfig.getSiteMenuConfigurationItems()).andReturn(children);
        expect(menuConfig.getPrototypeItem()).andReturn(null);
    }

    @Test
    public void get_menu_locked_by_other() throws RepositoryException {
        mockCreateMandatoryWorkspaceNodesIfMissing();
        mockGetPreviewSite();
        mockGetPreValidators();
        mockGetSiteMenu("menuId");
        final Calendar lockedOn = Calendar.getInstance();
        mockMenuConfig("admin", lockedOn);
        expect(session.hasPendingChanges()).andReturn(false);
        expect(pageComposerContextService.getEditingMount()).andReturn(mount);
        expect(pageComposerContextService.getSiteContentIdentifier(mount)).andReturn("uuid-of-content");
        expect(mount.getHstSite()).andReturn(site);
        expect(site.getSiteMap()).andReturn(siteMap);
        expect(siteMap.getCanonicalIdentifier()).andReturn("uuid-of-siteMap");
        replay(mocks);

        final Response response = siteMenuResource.getMenu();
        assertThat(response.getStatus(), is(OK));
        final Object entity = response.getEntity();
        assertThat(entity, is(instanceOf(ExtResponseRepresentation.class)));
        final ExtResponseRepresentation extResponse = (ExtResponseRepresentation) entity;

        assertThat(extResponse.isSuccess(), is(true));
        assertThat(extResponse.getData(), is(instanceOf(SiteMenuRepresentation.class)));
        final SiteMenuRepresentation siteMenu = (SiteMenuRepresentation) extResponse.getData();
        assertThat(siteMenu.getLockedBy(), is("admin"));
        assertThat(siteMenu.getLockedOn(), is(lockedOn));
    }

    @Test
    public void testGetMenuItem() throws RepositoryException {
        mockCreateMandatoryWorkspaceNodesIfMissing();
        mockGetPreviewSite();
        mockGetPreValidators();
        mockGetSiteMenu("menuId");
        expect(pageComposerContextService.getEditingMount()).andReturn(mount);
        final String id = "uuid-of-menu-item";
        mockGetMenuItem(node, id);
        expect(session.hasPendingChanges()).andReturn(false);
        replay(mocks);

        final Response response = siteMenuResource.getMenuItem(id);
        assertThat(response.getStatus(), is(OK));
        final ExtResponseRepresentation entity = ExtResponseRepresentation.class.cast(response.getEntity());
        assertThat(entity.getData(), is(instanceOf(SiteMenuItemRepresentation.class)));
        assertThat(entity.isSuccess(), is(true));
    }

    @Test
    public void testCreateAsChildOfMenu() throws RepositoryException {

        mockCreateMandatoryWorkspaceNodesIfMissing();
        mockGetPreviewSite();
        mockGetPreValidators();
        final String menuId = "uuid-of-menu";
        mockGetSiteMenu(menuId);

        expect(menuConfig.getCanonicalIdentifier()).andReturn(menuId);
        expect(session.getNodeByIdentifier(menuId)).andReturn(parentNode);

        final String name = "menuItemName";
        expect(parentNode.addNode(name, NODETYPE_HST_SITEMENUITEM)).andReturn(node);

        // Mock creating the site menu item
        final SiteMenuItemRepresentation newMenuItem = new SiteMenuItemRepresentation();
        newMenuItem.setName(name);
        expect(siteMenuItemHelper.create(parentNode, newMenuItem, Position.LAST, "'")).andReturn(node);

        final String menuItemId = "menuItemId";
        expect(node.getIdentifier()).andReturn(menuItemId);

        expect(session.hasPendingChanges()).andReturn(false);
        replay(mocks);

        final Response response = siteMenuResource.create(menuId, "last", "'", newMenuItem);

        assertThat(response.getStatus(), is(OK));
        assertThat(response.getEntity(), is(instanceOf(ExtResponseRepresentation.class)));

        final ExtResponseRepresentation extResponse = ExtResponseRepresentation.class.cast(response.getEntity());
        assertThat(extResponse.isSuccess(), is(true));
        assertThat(extResponse.getData().toString(), is(menuItemId));
    }


    @Test
    public void testUpdate() throws RepositoryException {

        mockCreateMandatoryWorkspaceNodesIfMissing();
        mockGetPreviewSite();
        mockGetPreValidators();
        mockGetSiteMenu("menuId");

        final String id = "uuid-of-menu-item";
        mockGetMenuItem(node, id);

        // Mock updating the site menu item
        final SiteMenuItemRepresentation modifiedItem = new SiteMenuItemRepresentation();
        modifiedItem.setId(id);
        siteMenuItemHelper.update(node, modifiedItem);
        expectLastCall().once();
        expect(session.hasPendingChanges()).andReturn(false);
        replay(mocks);

        final Response response = siteMenuResource.update(modifiedItem);

        assertThat(response.getStatus(), is(OK));
        assertThat(response.getEntity(), is(instanceOf(ExtResponseRepresentation.class)));

        final ExtResponseRepresentation extResponse = ExtResponseRepresentation.class.cast(response.getEntity());
        assertThat(extResponse.isSuccess(), is(true));
        assertThat(extResponse.getData().toString(), is(id));
    }

    @Test
    public void testUpdateReturnsServerErrorOnRepositoryException() throws RepositoryException {

        mockCreateMandatoryWorkspaceNodesIfMissing();
        mockGetPreviewSite();
        mockGetPreValidators();
        mockGetSiteMenu("menuId");

        final String id = "uuid-of-menu-item";
        final SiteMenuItemRepresentation modifiedItem = new SiteMenuItemRepresentation();
        modifiedItem.setId(id);

        expect(session.getNodeByIdentifier(modifiedItem.getId())).andThrow(new RuntimeException());
        expect(session.hasPendingChanges()).andReturn(false);
        replay(mocks);

        final Response response = siteMenuResource.update(modifiedItem);
        assertThat(response.getStatus(), is(SERVER_ERROR));
        assertThat(response.getEntity(), is(instanceOf(ExtResponseRepresentation.class)));

        final ExtResponseRepresentation extResponse = ExtResponseRepresentation.class.cast(response.getEntity());
        assertThat(extResponse.isSuccess(), is(false));
        assertThat(extResponse.getData(), is(instanceOf(String[].class)));

    }

    @Test
    public void testMove() throws RepositoryException {

        mockCreateMandatoryWorkspaceNodesIfMissing();
        mockGetPreviewSite();
        mockGetPreValidators();
        mockGetSiteMenu("menuId");

        final String sourceId = "sourceId";
        mockGetMenuItem(node, sourceId);

        final Integer childTargetIndex = 0;

        final String parentTargetId = "parentId";
        expect(menuConfig.getCanonicalIdentifier()).andReturn("parentId");
        expect(session.getNodeByIdentifier(parentTargetId)).andReturn(parentNode);
        siteMenuItemHelper.move(parentNode, node, childTargetIndex);
        expectLastCall();
        expect(session.hasPendingChanges()).andReturn(false);
        replay(mocks);

        final Response response = siteMenuResource.move(sourceId, parentTargetId, childTargetIndex);
        assertThat(response.getStatus(), is(OK));
        assertThat(response.getEntity(), is(instanceOf(ExtResponseRepresentation.class)));

        final ExtResponseRepresentation extResponse = ExtResponseRepresentation.class.cast(response.getEntity());
        assertThat(extResponse.isSuccess(), is(true));
        assertThat(extResponse.getData().toString(), is(sourceId));
    }

    @Test
    public void testDelete() throws RepositoryException {

        mockCreateMandatoryWorkspaceNodesIfMissing();
        mockGetPreviewSite();
        mockGetPreValidators();
        mockGetSiteMenu("menuId");
        final String sourceId = "sourceId";
        mockGetMenuItem(node, sourceId);
        siteMenuItemHelper.delete(node);
        expectLastCall().once();
        expect(session.hasPendingChanges()).andReturn(false);
        replay(mocks);

        final Response response = siteMenuResource.delete(sourceId);
        assertThat(response.getStatus(), is(OK));
        assertThat(response.getEntity(), is(instanceOf(ExtResponseRepresentation.class)));

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
        expect(pageComposerContextService.getRequestContext()).andReturn(context).anyTimes();
        expect(context.getAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER)).andReturn(menuId).anyTimes();
        expect(siteMenuHelper.getMenu(site, menuId)).andReturn(menuConfig);
    }

    private void mockGetPreviewSite() throws RepositoryException {
        // Due to the inheritance the following mock calls are required to get the preview site
        expect(context.getSession()).andReturn(session).anyTimes();
        expect(context.getServletRequest()).andReturn(request).anyTimes();
        expect(request.getSession(true)).andReturn(httpSession).anyTimes();
        expect(httpSession.getAttribute(ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID)).andReturn("mount");
        expect(context.getVirtualHost()).andReturn(virtualHost).anyTimes();
        expect(virtualHost.getVirtualHosts()).andReturn(virtualHosts).anyTimes();
        expect(virtualHosts.getMountByIdentifier("mount")).andReturn(mount).anyTimes();
        expect(mount.getMountPath()).andReturn("").anyTimes();
        expect(site.getConfigurationPath()).andReturn("/preview/configuration/path").anyTimes();
        expect(pageComposerContextService.getEditingPreviewSite()).andReturn(site).anyTimes();

    }

    private void mockCreateMandatoryWorkspaceNodesIfMissing() throws RepositoryException {
        expect(pageComposerContextService.getEditingLiveConfigurationPath()).andReturn("/live/configuration/path").anyTimes();
        expect(pageComposerContextService.getEditingPreviewConfigurationPath()).andReturn("/preview/configuration/path").anyTimes();

        expect(session.nodeExists("/live/configuration/path")).andReturn(true).anyTimes();
        expect(session.getNode("/live/configuration/path")).andReturn(node).anyTimes();

        expect(session.nodeExists("/preview/configuration/path")).andReturn(true).anyTimes();
        expect(session.getNode("/preview/configuration/path")).andReturn(node).anyTimes();

        expect(node.hasNode(HstNodeTypes.NODENAME_HST_WORKSPACE)).andReturn(false).anyTimes();
        expect(node.addNode(HstNodeTypes.NODENAME_HST_WORKSPACE)).andReturn(node).anyTimes();
        expect(node.addNode(HstNodeTypes.NODENAME_HST_PAGES)).andReturn(node).anyTimes();
        expect(node.addNode(HstNodeTypes.NODENAME_HST_SITEMAP)).andReturn(node).anyTimes();
    }

    private void mockGetPreValidators() {
        expect(validatorFactory.getChildExistsValidator(anyObject(String.class), anyObject(String.class))).andReturn(validator).anyTimes();
        expect(validatorFactory.getConfigurationExistsValidator(anyObject(String.class), anyObject(SiteMapHelper.class))).andReturn(validator).anyTimes();
        expect(validatorFactory.getHasPreviewConfigurationValidator(pageComposerContextService)).andReturn(validator).anyTimes();
        expect(validatorFactory.getNodePathPrefixValidator(anyObject(String.class), anyObject(String.class), anyObject(String.class))).andReturn(validator).anyTimes();
        expect(validatorFactory.getNotNullValidator(anyObject(), anyObject(ClientError.class))).andReturn(validator).anyTimes();
        expect(validatorFactory.getSiteMenuItemRepresentationValidator(anyObject(PageComposerContextService.class), anyObject(Predicate.class), anyObject(SiteMenuItemRepresentation.class))).andReturn(validator).anyTimes();
        expect(validatorFactory.getVoidValidator()).andReturn(validator).anyTimes();
        validator.validate(anyObject(HstRequestContext.class));
        expectLastCall().anyTimes();
    }
}
