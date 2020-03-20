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

import javax.jcr.Session;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
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
import org.hippoecm.hst.platform.services.experiencepage.ExperiencePageServiceImpl;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ExperiencePageIT extends AbstractBeanTestCase {

    private HstManager hstSitesManager;
    private HstURLFactory hstURLFactory;
    private Session requestContextSession;
    private MockHstRequestContext requestContext;
    private ExperiencePageServiceImpl dynamicHstComponentConfigurationService;

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

        ModifiableRequestContextProvider.set(requestContext);

    }

    @After
    public void tearDown() throws Exception {
        requestContextSession.logout();
        super.tearDown();
    }
    @Test
    public void experience_page_component_assertions() throws Exception {

        assertionsForExperiencePage("/unittestcontent/documents/unittestproject/experiences/expPage1");
    }

    @Test
    public void news_document_as_experience_page_component_assertions() throws Exception {

        final String pathToExperiencePage = "/unittestcontent/documents/unittestproject/News/2009/May/articleAsExpPage";
        assertionsForExperiencePage(pathToExperiencePage);
    }

    private void assertionsForExperiencePage(final String pathToExperiencePage) throws ObjectBeanManagerException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        GenericHttpServletRequestWrapper containerRequest;

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.addHeader("Host", "localhost");
        setRequestInfo(request, "/site", "/experiences/expPage1.html");

        containerRequest = new HstContainerRequestImpl(request, hstSitesManager.getPathSuffixDelimiter());

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

            HstComponentConfiguration header = hstComponentConfiguration.getChildByName("header");
            assertThat(header).as("'header' component expected to be inherited").isNotNull();
            assertThat(header.isExperiencePageComponent()).isFalse();
            assertThat(header.getReferenceName()).startsWith("r");


            HstComponentConfiguration leftmenu = hstComponentConfiguration.getChildByName("leftmenu");
            assertThat(leftmenu).as("'leftmenu' component expected to be inherited").isNotNull();
            assertThat(leftmenu.isExperiencePageComponent()).isFalse();
            assertThat(leftmenu.getReferenceName()).startsWith("r");


            HstComponentConfiguration body = hstComponentConfiguration.getChildByName("body");
            assertThat(body).as("'body' component expected to be part of Experience Page explicitly").isNotNull();
            assertThat(body.isExperiencePageComponent()).isTrue();
            assertThat(body.getReferenceName()).startsWith("r");

        } catch (ContainerException e) {
            fail(e.getMessage());
        }
    }

}
