/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.sitemenuresource;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.ArrayUtils;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.MountResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMenuResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMenuHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMenuItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.AbstractMountResourceTest;
import org.hippoecm.hst.site.HstServices;
import org.junit.After;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public abstract class AbstractMenuResourceTest extends AbstractPageComposerTest {

    protected MountResource mountResource;
    protected SiteMenuHelper siteMenuHelper;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        mountResource = AbstractMountResourceTest.createResource();
        // create users
        final Node users = session.getNode("/hippo:configuration/hippo:users");

        final Node bob = users.addNode("bob", "hipposys:user");
        bob.setProperty("hipposys:password", "bob");
        final Node alice = users.addNode("alice", "hipposys:user");
        alice.setProperty("hipposys:password", "alice");

        final Node adminGroup = session.getNode("/hippo:configuration/hippo:groups/admin");
        Value[] adminMembers = adminGroup.getProperty("hipposys:members").getValues();

        Value[] extra = new Value[2];
        extra[0] = session.getValueFactory().createValue("bob");
        extra[1] = session.getValueFactory().createValue("alice");

        final Value[] values = (Value[]) ArrayUtils.addAll(adminMembers, extra);
        adminGroup.setProperty("hipposys:members", values);

        // move hst:sitemenus/main to workspace
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemenus",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemenus");
        session.save();
        createPreviewWithSiteMenuWorkspace();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        final Node users = session.getNode("/hippo:configuration/hippo:users");
        users.getNode("bob").remove();
        users.getNode("alice").remove();

        final Node adminGroup = session.getNode("/hippo:configuration/hippo:groups/admin");
        Value[] adminMembers = adminGroup.getProperty("hipposys:members").getValues();

        // remove bob and alice again
        Value[] original = (Value[]) ArrayUtils.subarray(adminMembers, 0, adminMembers.length - 2);

        adminGroup.setProperty("hipposys:members", original);
        session.save();
        super.tearDown();
    }

    protected Session createSession(final String userName, final String password) throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials(userName, password.toCharArray()));
    }


    protected void assertBobCanMakeModications(final SiteMenuResource resource) throws Exception {
        final Session bob = createSession("bob", "bob");
        final SiteMenuItemRepresentation contactItem = getSiteMenuItemRepresentation(bob, "main", "Contact");
        contactItem.setName("test");
        final Response fail = resource.update(contactItem);
        assertEquals(Response.Status.OK.getStatusCode(), fail.getStatus());
        bob.logout();
    }

    protected void createPreviewWithSiteMenuWorkspace() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL(request, "localhost", "/home");

        final String previewConfigurationPath = ctx.getResolvedMount().getMount().getHstSite().getConfigurationPath() + "-preview";
        assertFalse("Preview config node should not exist yet.",
                session.nodeExists(previewConfigurationPath));

        ((HstMutableRequestContext) ctx).setSession(session);
        mountResource.startEdit();
        ModifiableRequestContextProvider.clear();
        // time for jcr events to arrive
        Thread.sleep(100);
    }

    public SiteMenuItemRepresentation getSiteMenuItemRepresentation(final Session requestSession,
                                                                    final String menuName,
                                                                    final String relPathMenuItem) throws Exception {

        final HstSiteMenuConfiguration hstSiteMenuConfiguration = getHstSiteMenuConfiguration(requestSession, menuName);
        mountResource.getPageComposerContextService().getRequestContext().setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER,
                ((CanonicalInfo) hstSiteMenuConfiguration).getCanonicalIdentifier());
        String[] segments = relPathMenuItem.split("/");

        HstSiteMenuItemConfiguration found = null;
        for (HstSiteMenuItemConfiguration itemConfiguration : hstSiteMenuConfiguration.getSiteMenuConfigurationItems()) {
            if (itemConfiguration.getName().equals(segments[0])) {
                found = itemConfiguration;
                break;
            }
        }
        if (found == null) {
            return null;
        }
        if (segments.length == 1) {

            return new SiteMenuItemRepresentation(found);
        }

        for (int i = 1; i < segments.length; i++) {
            boolean hit = false;
            for (HstSiteMenuItemConfiguration child : found.getChildItemConfigurations()) {
                if (child.getName().equals(segments[i])) {
                    found = child;
                    i++;
                    hit = true;
                    break;
                }
            }
            if (!hit) {
                return null;
            }
        }

        if (found == null) {
            return null;
        }
        return new SiteMenuItemRepresentation(found);
    }

    private HstSiteMenuConfiguration getHstSiteMenuConfiguration(final Session requestSession, final String menuName) throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL(request, "localhost", "/home");
        ((HstMutableRequestContext) ctx).setSession(requestSession);
        final HstSite editingPreviewHstSite = mountResource.getPageComposerContextService().getEditingPreviewSite();
        final HstSiteMenuConfiguration siteMenuConfiguration = editingPreviewHstSite.getSiteMenusConfiguration().getSiteMenuConfiguration(menuName);

        // override the config identifier to have sitemenu id
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, ((CanonicalInfo)siteMenuConfiguration).getCanonicalIdentifier());
        return siteMenuConfiguration;
    }

    protected SiteMenuResource createResource() {

        siteMenuHelper = new SiteMenuHelper();
        siteMenuHelper.setPageComposerContextService(mountResource.getPageComposerContextService());

        final SiteMenuResource siteMenuResource = new SiteMenuResource();
        siteMenuResource.setPageComposerContextService(mountResource.getPageComposerContextService());
        siteMenuResource.setSiteMenuHelper(siteMenuHelper);

        final SiteMenuItemHelper siteMenuItemHelper = new SiteMenuItemHelper();
        siteMenuItemHelper.setPageComposerContextService(mountResource.getPageComposerContextService());
        siteMenuResource.setSiteMenuItemHelper(siteMenuItemHelper);
        return siteMenuResource;
    }
}
