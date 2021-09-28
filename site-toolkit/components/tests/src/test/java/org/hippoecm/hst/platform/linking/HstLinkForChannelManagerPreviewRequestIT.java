/*
 *  Copyright 2012-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.linking;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.internal.PreviewDecorator;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 *  These unit tests are there to make sure that links that are created for requests that originate from the cms (cms
 *  rest calls or website in channel manager) are always over the HOST of the cms and always include the SITE
 *  contextpath. If there are cross-channel links in the site that are normally cross-domain, that should in cms context
 *  result in a querystring containing a rendering_host
 * <p/>
 * <p>
 *  When doing a request from the cms over, say some REST mount, then through the {@link
 *  org.hippoecm.hst.core.request.HstRequestContext#isChannelManagerPreviewRequest()} you get <code>true</code>. This value
 *  <code>true</code> comes from the backing http servlet request by HttpServletRequest#setAttribute(ContainerConstants.REQUEST_COMES_FROM_CMS,
 *  Boolean.TRUE);
 * </p>
 * <p>
 *  Apart from this boolean, a CMS request MAY or MAY NOT also set a 'renderingHost' on the
 *  backing http servletrequest Whe a request is done from the CMS through cms rest api, typically this 'renderingHost'
 *  is <code>null</code>. However, when the request is for loading the website in the channel manager, this renderhost is
 *  typically available : Because, requests will be done over the HOST OF THE CMS, the renderHost contains the value of
 *  the host that needs to be 'faked' to render the page.
 * </p>
 */
public class HstLinkForChannelManagerPreviewRequestIT extends AbstractHstLinkRewritingIT {

    private HstManager hstManager;
    private HstURLFactory hstURLFactory;
    private HstSiteMapMatcher siteMapMatcher;
    protected PreviewDecorator previewDecorator;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.hstManager = getComponent(HstManager.class.getName());
        this.siteMapMatcher = getComponent(HstSiteMapMatcher.class.getName());
        this.hstURLFactory = getComponent(HstURLFactory.class.getName());
        this.previewDecorator = HstServices.getComponentManager().getComponent(PreviewDecorator.class.getName());

    }

    @Test
    public void testLinksChannelManagerPreviewRequestWITHRenderingHost() throws Exception {
        // the rendering host is www.unit.test
        HstRequestContext requestContext = getRequestFromCms("cms.example.com", "/home", null, "www.unit.test");
        // assert that the match Mount is www.unit.test
        assertEquals("Matched mount should be the renderHost mount", "www.unit.test", requestContext.getResolvedMount().getMount().getVirtualHost().getHostName());

        ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
        Object homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
        {
            // link will be for www.unit.test Mount because can be created for current renderHost
            HstLink homePageLink = linkCreator.create((HippoBean) homeBean, requestContext);
            assertEquals("www.unit.test", homePageLink.getMount().getVirtualHost().getHostName());

            assertEquals("link.getPath for homepage node should be '", "", homePageLink.getPath());
            // A link in CMS request context for the HOMEPAGE should NOT be /site like for normal site requests,
            // but should be /site/ to work well with PROXIES using /site/ to match on. Hence, /site/ is expected
            // SINCE RENDER HOST stays the same, no render host is included in query string
            assertEquals("wrong absolute link for homepage for CMS context", "/site/", (homePageLink.toUrlForm(requestContext, false)));

            // A fully qualified link for CMS request context for should NOT be fully qualified, even for toUrlForm(requestContext, TRUE))
            // CMS links must always be relative to the CMS host! Thus no http://localhost involved
            // SINCE RENDER HOST stays the same, no render host is included in query string
            assertEquals("wrong fully qualified url for homepage for CMS context", "/site/", (homePageLink.toUrlForm(requestContext, true)));
        }

        {
            // NOW, ew create homepage link for another MOUNT. This should include a renderhost in the queryString

            HstLink homePageLinkForMobile = linkCreator.create(((HippoBean) homeBean).getNode(), requestContext, "mobile");
            String hostName = homePageLinkForMobile.getMount().getVirtualHost().getHostName();
            assertEquals("m.unit.test", hostName);
            assertEquals("link.getPath for homepage node should be '", "", homePageLinkForMobile.getPath());

            // renderhost should be included
            assertEquals("wrong absolute link for homepage for CMS context", "/site/?"+ContainerConstants.RENDERING_HOST+"="+hostName, (homePageLinkForMobile.toUrlForm(requestContext, false)));

            // renderhost should be included
            assertEquals("wrong fully qualified url for homepage for CMS context", "/site/?"+ContainerConstants.RENDERING_HOST+"="+hostName, (homePageLinkForMobile.toUrlForm(requestContext, true)));
        }
    }

    /**
     * @see {@link HstLinkRewritingIT#testCrossSiteAndDomainHstLinkForBean()} which *CANNOT*
     * resolve cross links from the preview mount to other mounts because the other mounts only have a live. In CMS REQUEST
     * CONTEXT though, this just works because the CMS should use DECORATED PREVIEW MOUNTS (decorated from live)
     */
    @Test
    public void testLinksChannelManagerPreviewRequestWITHRenderingHostResolvesLinkToPreviewDecoratedMounts() throws Exception {
        // the rendering host is localhost:8081
        // We now do a request that is for the preview site (PORT 8081). Because for the 'unittestsubproject' we have only
        // configured LIVE mounts, then when we request a link, we expect to get a link from a 'live mount to preview mount
        // decorated version' because we are in a cms request
        HstRequestContext requestContext = getRequestFromCms("cms.example.com", "/home", null, "localhost:8081");
        assertTrue(requestContext.isChannelManagerPreviewRequest());

        // assert that the match Mount is localhost
        assertEquals("Matched mount should be the renderHost mount", "localhost",
                requestContext.getResolvedMount().getMount().getVirtualHost().getHostName());

        ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);

        Object newsBean = obm.getObject("/unittestcontent/documents/unittestsubproject/News/2008/SubNews1");
        HstLink crossChannelNewsLinkToPreviewDecoratedMount = linkCreator.create((HippoBean) newsBean, requestContext);

        assertEquals("Expected a preview decorated mount", "preview",crossChannelNewsLinkToPreviewDecoratedMount.getMount().getType());

        assertEquals("wrong link.getPath for News/2008/SubNews1 ", "news/2008/SubNews1.html", crossChannelNewsLinkToPreviewDecoratedMount.getPath());
        assertEquals("Expected NO render_host paramater in relative URL since the current renderingHost is already the same (localhost)",
                "/site/subsite/news/2008/SubNews1.html",
                crossChannelNewsLinkToPreviewDecoratedMount.toUrlForm(requestContext, false));
        // fully qualified links will still be relative!
        assertEquals("Expected NO render_host paramater in relative URL since the current renderingHost is already the same (localhost)",
                "/site/subsite/news/2008/SubNews1.html",
                crossChannelNewsLinkToPreviewDecoratedMount.toUrlForm(requestContext, true));
    }

    /**
     * When the channel mgr opens a channel for webapp with context '/site', and in the channel a page is rendered
     * containing a link (URL) to a document belonging to a different site webapp (eg '/site2'), then the URL must
     * include the renderingHost in the querystring: The reason for this is that there might not yet have been an 'SSO
     * handshake' between the CMS webapp and /site2 webapp. Having the 'renderingHost' in the querystring makes sure
     * this 'SSO handshake' is taken care of when needed
     */
    @Test
    public void cross_hst_webapp_links_container_rendering_host_in_URL() throws Exception {
        HstRequestContext requestContext = getRequestFromCms("cms.example.com", "/home", null, "localhost:8081");
        assertTrue(requestContext.isChannelManagerPreviewRequest());

        // get a document from webapp site2
        ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);

        Object newsBean = obm.getObject("/extracontent/documents/extraproject/News/News1");

        HstLink crossSiteWebappNewsLinkToPreviewDecoratedMount = linkCreator.create((HippoBean) newsBean, requestContext);

        // This is VERY delicate: Because the cross Webapp HST Model is a different one than the one for the CURRENT
        // Request, the resolved MOUNT won't be a PREVIEW mount since the cross webapp HST Model is not decorated to
        // be a 'decorated preview' model. I don't think this MATTERS.
        assertEquals("Expected a LIVE decorated mount", "live",crossSiteWebappNewsLinkToPreviewDecoratedMount.getMount().getType());

        assertEquals("wrong link.getPath for News/News1 ", "news/News1.html", crossSiteWebappNewsLinkToPreviewDecoratedMount.getPath());
        assertEquals("Expected PRESENT render_host paramater in relative URL since the site webapp is " +
                        "different than the webapp the link belongs to",
                "/site2/extra/news/News1.html?org.hippoecm.hst.container.render_host=localhost",
                crossSiteWebappNewsLinkToPreviewDecoratedMount.toUrlForm(requestContext, false));
        // fully qualified links will still be relative!
        assertEquals("Expected PRESENT render_host paramater in relative URL since the site webapp is " +
                        "different than the webapp the link belongs to",
                "/site2/extra/news/News1.html?org.hippoecm.hst.container.render_host=localhost",
                crossSiteWebappNewsLinkToPreviewDecoratedMount.toUrlForm(requestContext, true));
    }

    public HstRequestContext getRequestFromCms(final String hostAndPort,
                                               final String pathInfo,
                                               final String queryString,
                                               final String renderingHost) throws Exception {
        HstRequestContextComponent rcc = getComponent(HstRequestContextComponent.class.getName());
        HstMutableRequestContext requestContext = rcc.create();
        ModifiableRequestContextProvider.set(requestContext);
        HstContainerURL containerUrl = createContainerUrlForChannelManagerPreviewRequest(requestContext, hostAndPort, pathInfo, queryString, renderingHost);
        requestContext.setBaseURL(containerUrl);
        ResolvedSiteMapItem resolvedSiteMapItem = getResolvedSiteMapItem(containerUrl);
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
        requestContext.setResolvedMount(resolvedSiteMapItem.getResolvedMount());
        requestContext.matchingFinished();
        HstURLFactory hstURLFactory = getComponent(HstURLFactory.class.getName());
        requestContext.setURLFactory(hstURLFactory);
        requestContext.setSiteMapMatcher(siteMapMatcher);

        requestContext.setRenderHost(renderingHost);

        return requestContext;
    }

    public HstContainerURL createContainerUrlForChannelManagerPreviewRequest(final HstMutableRequestContext requestContext,
                                                                             final String hostAndPort,
                                                                             final String pathInfo,
                                                                             final String queryString,
                                                                             final String renderingHost) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        GenericHttpServletRequestWrapper containerRequest;
        {
            MockHttpServletRequest request = new MockHttpServletRequest();
            if (hostAndPort.split(":").length > 1) {
                int port = Integer.parseInt(hostAndPort.split(":")[1]);
                request.setLocalPort(port);
                request.setServerPort(port);
            }
            request.setScheme("http");
            String host = hostAndPort.split(":")[0];
            request.setServerName(host);
            request.addHeader("Host", hostAndPort);
            setRequestInfo(request, "/site", pathInfo);
            request.setQueryString(queryString);

            requestContext.setChannelManagerPreviewRequest();
            if (renderingHost != null) {
                request.setParameter(ContainerConstants.RENDERING_HOST, renderingHost);
            }
            containerRequest = new HstContainerRequestImpl(request, hstManager.getPathSuffixDelimiter());
        }

        requestContext.setServletRequest(containerRequest);

        VirtualHosts vhosts = hstManager.getVirtualHosts();
        ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(containerRequest),
                HstRequestUtils.getRequestPath(containerRequest));
        setHstServletPath(containerRequest, mount);
        return hstURLFactory.getContainerURLProvider().parseURL(containerRequest, response, mount);
    }

    public ResolvedSiteMapItem getResolvedSiteMapItem(HstContainerURL url) throws ContainerException {
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        final ResolvedMount resolvedMount = vhosts.matchMount(url.getHostName(), url.getRequestPath());
        final Mount decorated = previewDecorator.decorateMountAsPreview(resolvedMount.getMount());
        ((MutableResolvedMount) resolvedMount).setMount(decorated);
        return resolvedMount.matchSiteMapItem(url.getPathInfo());
    }


}
