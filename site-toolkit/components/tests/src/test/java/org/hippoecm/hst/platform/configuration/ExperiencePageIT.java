/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.platform.configuration;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.beans.AbstractBeanTestCase;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hippoecm.repository.util.WorkflowUtils.getDocumentVariantNode;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest(HstRequestUtils.class)
public class ExperiencePageIT extends AbstractBeanTestCase {

    private HstManager hstSitesManager;
    private HstURLFactory hstURLFactory;
    private Session requestContextSession;
    private MockHstRequestContext requestContext;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.hstSitesManager = getComponent(HstManager.class.getName());
        this.hstURLFactory = getComponent(HstURLFactory.class.getName());
        requestContext = new MockHstRequestContext();

        requestContext.setContentTypes(HippoServiceRegistry.getService(ContentTypeService.class).getContentTypes());

        requestContextSession = createSession();
        requestContext.setSession(requestContextSession);

        final ObjectBeanManagerImpl objectBeanManager = new ObjectBeanManagerImpl(requestContextSession, getObjectConverter());

        final Map<Session, ObjectBeanManager> objectBeanManagerMap = new HashMap<>();
        objectBeanManagerMap.put(requestContextSession, objectBeanManager);
        requestContext.setNonDefaultObjectBeanManagers(objectBeanManagerMap);
        requestContext.setDefaultObjectBeanManager(objectBeanManager);

        final ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        final Mount mount = createNiceMock(Mount.class);
        expect(resolvedMount.getMount()).andStubReturn(mount);
        expect(mount.isPreview()).andStubReturn(false);

        replay(resolvedMount, mount);

        requestContext.setResolvedMount(resolvedMount);

        ModifiableRequestContextProvider.set(requestContext);

    }

    @After
    public void tearDown() throws Exception {
        requestContextSession.logout();
        super.tearDown();
    }
    @Test
    public void experience_page_component_assertions() throws Exception {

        assertionsForExperiencePage("/unittestcontent/documents/unittestproject/experiences/expPage1",
                null, null);
    }

    @Test
    public void news_document_as_experience_page_component_assertions() throws Exception {

        final String pathToExperiencePage = "/unittestcontent/documents/unittestproject/News/2009/May/articleAsExpPage";
        assertionsForExperiencePage(pathToExperiencePage, null, null);
    }

    @Test
    public void experience_page_document_variants_skips_hst_page_child_to_and_from_draft() throws Exception {
        final String pathToExperiencePage = "/unittestcontent/documents/unittestproject/News/2009/May/articleAsExpPage";
        final Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        final HippoSession session = (HippoSession)repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        try {

            // backup expPage1 before changing it with workflow
            JcrUtils.copy(session, pathToExperiencePage, "/backupExpPage1");
            session.save();

            final Node handle = session.getNode(pathToExperiencePage);
            final DocumentWorkflow workflow = (DocumentWorkflow)session.getWorkspace().getWorkflowManager().getWorkflow("default", handle);

            final Document draft = workflow.obtainEditableInstance();

            assertTrue(getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get().hasNode("hst:page"));

            assertFalse("Draft variant should not get the 'hst:page' child",
                    draft.getNode(session).hasNode("hst:page"));

            workflow.commitEditableInstance();

            assertTrue(getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get().hasNode("hst:page"));

            assertFalse(draft.getNode(session).hasNode("hst:page"));


        } finally {
            session.getNode(pathToExperiencePage).remove();
            JcrUtils.copy(session, "/backupExpPage1", pathToExperiencePage);

            session.logout();
        }

    }

    @Test
    public void versioned_news_document_as_experience_page_component_assertions() throws Exception {

        final String pathToExperiencePage = "/unittestcontent/documents/unittestproject/News/2009/May/articleAsExpPage";

        final Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        final HippoSession session = (HippoSession)repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        try {

            // backup expPage1 before changing it with workflow
            JcrUtils.copy(session, pathToExperiencePage, "/backupExpPage1");
            session.save();

            final Node handle = session.getNode(pathToExperiencePage);
            final DocumentWorkflow workflow = (DocumentWorkflow)session.getWorkspace().getWorkflowManager().getWorkflow("default", handle);

            // now the 'workspace version is branch 'foo', but 'assertionsForExperiencePage' will load the master, which
            // will be loaded from VERSION HISTORY!
            workflow.branch("foo", "Foo");

            // publish foo branch
            workflow.publishBranch("foo");

            // render branch 'foo': Because the published variant is for 'live', it is
            // a version history XPage that will be loaded
            assertionsForExperiencePage(pathToExperiencePage, "foo", session);

        } finally {
            session.getNode(pathToExperiencePage).remove();
            JcrUtils.copy(session, "/backupExpPage1", pathToExperiencePage);

            session.logout();
        }

    }

    private void assertionsForExperiencePage(final String pathToExperiencePage, final String branch,
                                              final Session session) throws ObjectBeanManagerException, RepositoryException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        GenericHttpServletRequestWrapper containerRequest;

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.addHeader("Host", "localhost");
        setRequestInfo(request, "/site", "/experiences/expPage1.html");

        containerRequest = new HstContainerRequestImpl(request, hstSitesManager.getPathSuffixDelimiter());

        if (branch != null) {
            // Mock that the right branch is loaded!
            PowerMock.mockStaticPartial(HstRequestUtils.class, "getBranchIdFromContext");
            expect(HstRequestUtils.getBranchIdFromContext(anyObject())).andStubReturn(branch);
            PowerMock.replay(HstRequestUtils.class);
        }

        HippoBean requestBean = (HippoBean)requestContext.getObjectBeanManager().getObject(pathToExperiencePage);
        requestContext.setContentBean(requestBean);

        try {
            VirtualHosts vhosts = hstSitesManager.getVirtualHosts();
            ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(containerRequest),
                    HstRequestUtils.getRequestPath(containerRequest));

            setHstServletPath(containerRequest, mount);
            final HstContainerURL hstContainerURL = hstURLFactory.getContainerURLProvider().parseURL(containerRequest, response, mount);
            final ResolvedSiteMapItem resolvedSiteMapItem = mount.matchSiteMapItem(hstContainerURL.getPathInfo());

            final HstComponentConfiguration hstComponentConfiguration = resolvedSiteMapItem.getHstComponentConfiguration();

            assertThat(hstComponentConfiguration.getReferenceName())
                    .as("The root component for the experience page expected to have namespace 'ep'")
                    .startsWith("ep");

            assertThat(hstComponentConfiguration.isExperiencePageComponent())
                    .as("Expected experience page component as root component although it inherits from " +
                            "'hst:abstractpages/basepage'")
                    .isTrue();

            assertThat(hstComponentConfiguration.isShared()).isFalse();

            HstComponentConfiguration header = hstComponentConfiguration.getChildByName("header");
            assertThat(header).as("'header' component expected to be inherited").isNotNull();
            assertThat(header.isExperiencePageComponent()).isFalse();
            assertThat(header.isShared()).isTrue();
            assertThat(header.getReferenceName()).startsWith("r");


            HstComponentConfiguration leftmenu = hstComponentConfiguration.getChildByName("leftmenu");
            assertThat(leftmenu).as("'leftmenu' component expected to be inherited").isNotNull();
            assertThat(leftmenu.isExperiencePageComponent()).isFalse();
            assertThat(header.isShared()).isTrue();
            assertThat(leftmenu.getReferenceName()).startsWith("r");


            HstComponentConfiguration body = hstComponentConfiguration.getChildByName("body");
            assertThat(body).as("'body' component expected to be part of Experience Page explicitly").isNotNull();
            assertThat(body.isExperiencePageComponent()).isTrue();
            assertThat(body.isShared()).isFalse();
            assertThat(body.getReferenceName()).startsWith("r");

            if (branch != null) {
                // assert canonical location of component configuration is just workspace path although hst:page is
                // loaded from version history! This is because we decorate the path! See HippoBeanFrozenNodeUtils.getWorkspaceFrozenNode()
                assertEquals(pathToExperiencePage + "/" + substringAfterLast(pathToExperiencePage, "/") + "/hst:page/body",
                        body.getCanonicalStoredLocation());

                // The UUID of the HST configuration however should map to a frozen Node still!! This is really special,
                // the canonical hst config path points to a different node then the getCanonicalIdentifier : that is
                // because we decorate the path for version history documents!
                final String canonicalIdentifier = body.getCanonicalIdentifier();

                final Node node = session.getNodeByIdentifier(canonicalIdentifier);

                // assert backing node of hst configuration component is FROZEN!!
                assertThat(node.getPath())
                        .as("Expected that jcr node for the hst component is part of a frozen node")
                        .startsWith("/jcr:system/")
                        .endsWith("/hst:page/body");
            }

        } catch (ContainerException e) {
            fail(e.getMessage());
        }
    }

}
