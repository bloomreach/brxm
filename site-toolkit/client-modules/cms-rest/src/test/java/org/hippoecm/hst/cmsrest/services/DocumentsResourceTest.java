/*
 *  Copyright 2012-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.cmsrest.services;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.cmsrest.AbstractCmsRestTest;
import org.hippoecm.hst.cmsrest.container.CmsRestSecurityValve;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.internal.MountDecorator;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static junit.framework.Assert.assertEquals;

public class DocumentsResourceTest extends AbstractCmsRestTest {
    private Session session;
    private HstURLFactory hstURLFactory;
    private HstLinkCreator linkCreator;
    private HstSiteMapMatcher siteMapMatcher;
    private DocumentsResource documentsResource;
    private String homePageNodeId;
    protected MountDecorator mountDecorator;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.linkCreator = getComponentManager().getComponent(HstLinkCreator.class.getName());
        this.siteMapMatcher = getComponentManager().getComponent(HstSiteMapMatcher.class.getName());
        this.hstURLFactory = getComponentManager().getComponent(HstURLFactory.class.getName());
        this.mountDecorator = HstServices.getComponentManager().getComponent(MountDecorator.class.getName());
        this.session = createSession();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (session != null) {
            session.logout();
        }
    }

    protected void initRequest() throws Exception {
        HstRequestContext requestContext = getRequestFromCms("127.0.0.1:8080", "/_cmsrest");
        documentsResource = new DocumentsResource();
        documentsResource.setHstLinkCreator(linkCreator);
        String homePageDocument = "/unittestcontent/documents/unittestproject/common/homepage";
        Node homePageNode = session.getNode(homePageDocument);
        homePageNodeId = homePageNode.getIdentifier();
        ModifiableRequestContextProvider.set(requestContext);
    }

    @Test
    public void testDocumentResourceLiveUrls() throws Exception {
        initRequest();
        // first set HOST_GROUP_NAME_FOR_CMS_HOST to 'testgroup'
        RequestContextProvider.get().setAttribute(CmsRestSecurityValve.HOST_GROUP_NAME_FOR_CMS_HOST, "dev-localhost");
        // this homepage document can be exposed by two unittest mounts name (see hst-unittestvirtualhosts.xml):
        /*
         * dev-localhost
         *     `localhost
         *          ` hst:root (mount with content '/unittestcontent/documents/unittestproject')
         *                ` intranet (mount with content '/unittestcontent/documents/unittestproject' and
         *                                             hst:contextpath = /site2)
         *
         *
         * From above, you can see that the homepage document can have a link for two mounts:
         * 1) hst:root
         * 2) intranet
         *
         * Since the second link has the same contentpath, same number of types, but has a more specific mount (contains
         * more ancestors), we expect intranet as mount to be found for the link
         */

        String url = documentsResource.getUrl(homePageNodeId , "live");
        // NOTE url is site host and NOT cms 127.0.0.1 host!
        assertEquals("http://localhost:8080/site2/intranet", url);
    }

    @Test
    public void testDocumentResourcePreviewUrls() throws Exception {
        initRequest();
        // multiple mounts are suited, but the best mount is the deepest mount that is *not explicitly* a preview
        RequestContextProvider.get().setAttribute(CmsRestSecurityValve.HOST_GROUP_NAME_FOR_CMS_HOST, "dev-localhost");
        String url = documentsResource.getUrl(homePageNodeId, "preview");
        // note not a fully qualified URL but always one relative to cms request
        assertEquals("/site2/intranet", url);
    }

    @Test
    public void testDocumentResourceGetURLIsFullyQualifiedSiteURLsForTestGroupHostGroup() throws Exception {
        initRequest();
        // first set HOST_GROUP_NAME_FOR_CMS_HOST to 'testgroup'
        RequestContextProvider.get().setAttribute(CmsRestSecurityValve.HOST_GROUP_NAME_FOR_CMS_HOST, "testgroup");
        String url = documentsResource.getUrl(homePageNodeId , "live");
        assertEquals("http://www.unit.test:8080/site/custompipeline", url);
    }
    

    public HstRequestContext getRequestFromCms(final String hostAndPort,
                                               final String pathInfo) throws Exception {
        HstRequestContextComponent rcc = getComponentManager().getComponent(HstRequestContextComponent.class.getName());
        HstMutableRequestContext requestContext = rcc.create();
        HstContainerURL containerUrl = createContainerUrlForCmsRequest(requestContext, hostAndPort, pathInfo);
        requestContext.setBaseURL(containerUrl);
        requestContext.setResolvedMount(getResolvedMount(containerUrl, requestContext));
        HstURLFactory hstURLFactory = getComponentManager().getComponent(HstURLFactory.class.getName());
        requestContext.setURLFactory(hstURLFactory);
        requestContext.setSiteMapMatcher(siteMapMatcher);

        return requestContext;
    }

    public HstContainerURL createContainerUrlForCmsRequest(final HstMutableRequestContext requestContext,
                                                           final String hostAndPort,
                                                           final String pathInfo) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        GenericHttpServletRequestWrapper containerRequest;
        {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setScheme("http");
            String host = hostAndPort.split(":")[0];
            if (hostAndPort.split(":").length > 1) {
                int port = Integer.parseInt(hostAndPort.split(":")[1]);
                request.setLocalPort(port);
                request.setServerPort(port);
            }
            request.setServerName(host);
            request.addHeader("Host", hostAndPort);
            request.setPathInfo(pathInfo);
            request.setContextPath("/site");
            request.setRequestURI("/site" + pathInfo);
            containerRequest = new HstContainerRequestImpl(request, hstManager.getPathSuffixDelimiter());
        }
        requestContext.setServletRequest(containerRequest);
        requestContext.setCmsRequest(true);

        VirtualHosts vhosts = hstManager.getVirtualHosts();
        ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(containerRequest),
                containerRequest.getContextPath(), HstRequestUtils.getRequestPath(containerRequest));

        // set hst servlet path correct
        if (mount.getMatchingIgnoredPrefix() != null) {
            containerRequest.setServletPath("/" + mount.getMatchingIgnoredPrefix() + mount.getResolvedMountPath());
        } else {
            containerRequest.setServletPath(mount.getResolvedMountPath());
        }
        return hstURLFactory.getContainerURLProvider().parseURL(containerRequest, response, mount);
    }

    public ResolvedMount getResolvedMount(HstContainerURL url, final HstMutableRequestContext requestContext) throws ContainerException {
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        final ResolvedMount resolvedMount = vhosts.matchMount(url.getHostName(), url.getContextPath(), url.getRequestPath());
        requestContext.setAttribute(ContainerConstants.UNDECORATED_MOUNT, resolvedMount.getMount());
        final Mount decorated = mountDecorator.decorateMountAsPreview(resolvedMount.getMount());
        ((MutableResolvedMount) resolvedMount).setMount(decorated);
        return resolvedMount;
    }

}
