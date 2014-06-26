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
package org.hippoecm.hst.core.linking;


import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.beans.AbstractBeanTestCase;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.internal.MountDecorator;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
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
 *  org.hippoecm.hst.core.request.HstRequestContext#isCmsRequest()} you get <code>true</code>. This value
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
public class TestHstLinkForCmsRequest extends AbstractBeanTestCase {

    private Repository repository;
    private Credentials credentials;
    private HstManager hstManager;
    private HstURLFactory hstURLFactory;
    private ObjectConverter objectConverter;
    private HstLinkCreator linkCreator;
    private HstSiteMapMatcher siteMapMatcher;
    protected MountDecorator mountDecorator;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.repository = getComponent(Repository.class.getName());
        this.credentials= getComponent(Credentials.class.getName()+".hstconfigreader");
        this.hstManager = getComponent(HstManager.class.getName());
        this.siteMapMatcher = getComponent(HstSiteMapMatcher.class.getName());
        this.hstURLFactory = getComponent(HstURLFactory.class.getName());
        this.objectConverter = getObjectConverter();
        this.linkCreator = getComponent(HstLinkCreator.class.getName());
        this.mountDecorator = HstServices.getComponentManager().getComponent(MountDecorator.class.getName());

    }

    @Test
    public void testLinksCMSRequestNoRenderingHost() throws Exception {
        {
            HstRequestContext requestContext = getRequestFromCms("cms.example.com", "/home", null, null, false);
            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
            HstLink homePageLink = linkCreator.create((HippoBean) homeBean, requestContext);
            assertEquals("link.getPath for homepage node should be '", "", homePageLink.getPath());
            // A link in CMS request context for the HOMEPAGE should NOT be /site like for normal site requests,
            // but should be /site/ to work well with PROXIES using /site/ to match on. Hence, /site/ is expected
            assertEquals("wrong absolute link for homepage for CMS context", "/site/", (homePageLink.toUrlForm(requestContext, false)));
    
            // A fully qualified link for CMS request context for should NOT be fully qualified, even for toUrlForm(requestContext, TRUE))
            // CMS links must always be relative to the CMS host! Thus no http://localhost involved
            assertEquals("wrong fully qualified url for homepage for CMS context", "/site/", (homePageLink.toUrlForm(requestContext, true)));
    
            Node newsNode = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
            HstLink newsLink = linkCreator.create(newsNode, requestContext);
            assertEquals("wrong link.getPath for News/News1 for CMS context","news/News1.html", newsLink.getPath());
            assertEquals("wrong absolute link for News/News1 for CMS context" ,"/site/news/News1.html", (newsLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for News/News1 for CMS context" ,"/site/news/News1.html", (newsLink.toUrlForm(requestContext, true)));
    
            // link for an image in cms context should also start with context path
            HstLink imageLink = linkCreator.create("/images/mythumbnail.gif", requestContext.getResolvedMount().getMount());
            assertEquals("wrong absolute link for images/mythumbnail.gif for CMS context" ,"/site/images/mythumbnail.gif", (imageLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for images/mythumbnail.gif for CMS context" ,"/site/images/mythumbnail.gif", (imageLink.toUrlForm(requestContext, true)));
        }
        // NOW we do the same tests as above, but we first SET the showcontextpath on hst:hst/hst:hosts to FALSE : 
        // EVEN when contextpath is set to FALSE, for the URLs in cms context, still the contextpath should be included.
        // When acessing the site over the host of the cms, ALWAYS the context path needs to be included
        Session session = repository.login(credentials);
        Node hstHostsNode = session.getNode("/hst:hst/hst:hosts");
        boolean before = hstHostsNode.getProperty("hst:showcontextpath").getBoolean();
        hstHostsNode.setProperty("hst:showcontextpath", false);
        session.save();
        // wait to be sure async jcr event arrived
        Thread.sleep(50);
        // NOW below, even when show contextpath is FALSE, the /site contextpath should be included because the links are
        // for cms host
        {
            HstRequestContext requestContext = getRequestFromCms("cms.example.com", "/home", null, null, false);
            ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
            Object homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
            HstLink homePageLink = linkCreator.create((HippoBean) homeBean, requestContext);
            assertEquals("link.getPath for homepage node should be '", "", homePageLink.getPath());
            // A link in CMS request context for the HOMEPAGE should NOT be /site like for normal site requests,
            // but should be /site/ to work well with PROXIES using /site/ to match on. Hence, /site/ is expected
            assertEquals("wrong absolute link for homepage for CMS context", "/site/", (homePageLink.toUrlForm(requestContext, false)));

            // A fully qualified link for CMS request context for should NOT be fully qualified, even for toUrlForm(requestContext, TRUE))
            // CMS links must always be relative to the CMS host! Thus no http://localhost involved
            assertEquals("wrong fully qualified url for homepage for CMS context", "/site/", (homePageLink.toUrlForm(requestContext, true)));

            Node newsNode = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
            HstLink newsLink = linkCreator.create(newsNode, requestContext);
            assertEquals("wrong link.getPath for News/News1 for CMS context","news/News1.html", newsLink.getPath());
            assertEquals("wrong absolute link for News/News1 for CMS context" ,"/site/news/News1.html", (newsLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for News/News1 for CMS context" ,"/site/news/News1.html", (newsLink.toUrlForm(requestContext, true)));

            // link for an image in cms context should also start with context path
            HstLink imageLink = linkCreator.create("/images/mythumbnail.gif", requestContext.getResolvedMount().getMount());
            assertEquals("wrong absolute link for images/mythumbnail.gif for CMS context" ,"/site/images/mythumbnail.gif", (imageLink.toUrlForm(requestContext, false)));
            assertEquals("wrong fully qualified url for images/mythumbnail.gif for CMS context" ,"/site/images/mythumbnail.gif", (imageLink.toUrlForm(requestContext, true)));
        }
        // set the value again to original.
        hstHostsNode.setProperty("hst:showcontextpath", before);
        session.save();
        session.logout();
        
    }


    @Test
    public void testLinksCMSRequestWITHRenderingHost() throws Exception {
        // the rendering host is www.unit.test
        HstRequestContext requestContext = getRequestFromCms("cms.example.com", "/home", null, "www.unit.test", false);
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
     * @see {@link org.hippoecm.hst.core.linking.TestHstLinkRewriting#testCrossSiteAndDomainHstLinkForBean()} which *CANNOT*
     * resolve cross links from the preview mount to other mounts because the other mounts only have a live. In CMS REQUEST
     * CONTEXT though, this just works because the CMS should use DECORATED PREVIEW MOUNTS (decorated from live)
     */
    @Test
    public void testLinksCMSRequestWITHRenderingHostResolvesLinkToPreviewDecoratedMounts() throws Exception {
        // the rendering host is localhost:8081
        // We now do a request that is for the preview site (PORT 8081). Because for the 'unittestsubproject' we have only
        // configured LIVE mounts, then when we request a link, we expect to get a link from a 'live mount to preview mount
        // decorated version' because we are in a cms request
        HstRequestContext requestContext = getRequestFromCms("cms.example.com", "/home", null, "localhost:8081", false);
        assertTrue(requestContext.isCmsRequest());
        // assert that the match Mount is localhost
        assertEquals("Matched mount should be the renderHost mount", "localhost",
                requestContext.getResolvedMount().getMount().getVirtualHost().getHostName());

        ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);

        Object newsBean = obm.getObject("/unittestcontent/documents/unittestsubproject/News/2008/SubNews1");
        HstLink crossSiteNewsLinkToPreviewDecoratedMount = linkCreator.create((HippoBean) newsBean, requestContext);

        assertEquals("Expected a preview decorated mount", "preview",crossSiteNewsLinkToPreviewDecoratedMount.getMount().getType());

        assertEquals("wrong link.getPath for News/2008/SubNews1 ", "news/2008/SubNews1.html", crossSiteNewsLinkToPreviewDecoratedMount.getPath());
        assertEquals("Expected a render_host paramater in relative URL since host cms should be used + ?" +ContainerConstants.RENDERING_HOST+ "=....",
                "/site/subsite/news/2008/SubNews1.html?"+ContainerConstants.RENDERING_HOST+"=localhost",
                crossSiteNewsLinkToPreviewDecoratedMount.toUrlForm(requestContext, false));
        // fully qualified links will still be relative!
        assertEquals("Expected a render_host paramater in relative URL since host cms should be used + render_host ",
                "/site/subsite/news/2008/SubNews1.html?"+ContainerConstants.RENDERING_HOST+"=localhost",
                crossSiteNewsLinkToPreviewDecoratedMount.toUrlForm(requestContext, true));
    }



    /**
     * Even when a render host is set, when there is also an indication on the request that the client host should be forced, then
     * the render host should be skipped. This is the case where for example on the http session the renderhost is stored, but
     * the request should not used this stored renderhost. Then, with the parameter FORCE_CLIENT_HOST = true this can be indicated
     * @throws Exception
     */
    @Test
    public void testLinksCMSRequestWITHRenderingHostAndForceClientHost() throws Exception {
        // the rendering host is www.unit.test but we also indicate FORCE_CLIENT_HOST = true
        HstRequestContext requestContext = getRequestFromCms("cms.example.com", "/home", null, "www.unit.test", true);
        // even though the renderingHost www.unit.test is set
        
        // Since hst:defaulthostname is localhost, with 'force client host' and client host 'cms.example.com' which does
        // not have a configured mount, a fallback to localhost should be seen:
        assertEquals("Matched mount should be the renderHost mount", "localhost",
                requestContext.getResolvedMount().getMount().getVirtualHost().getHostName());


        // when not forcing client host, we should get www.unit.test as the matched mount its hostname

        requestContext = getRequestFromCms("cms.example.com", "/home", null, "www.unit.test", false);
        assertEquals("Matched mount should be the renderHost mount", "www.unit.test",
                requestContext.getResolvedMount().getMount().getVirtualHost().getHostName());

    }

    public HstRequestContext getRequestFromCms(final String hostAndPort,
                                               final String pathInfo,
                                               final String queryString,
                                               final String renderingHost,
                                               final boolean forceClientHost) throws Exception {
        HstRequestContextComponent rcc = getComponent(HstRequestContextComponent.class.getName());
        HstMutableRequestContext requestContext = rcc.create();
        HstContainerURL containerUrl = createContainerUrlForCmsRequest(requestContext, hostAndPort, pathInfo, queryString, renderingHost, forceClientHost);
        requestContext.setBaseURL(containerUrl);
        ResolvedSiteMapItem resolvedSiteMapItem = getResolvedSiteMapItem(containerUrl);
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
        requestContext.setResolvedMount(resolvedSiteMapItem.getResolvedMount());
        HstURLFactory hstURLFactory = getComponent(HstURLFactory.class.getName());
        requestContext.setURLFactory(hstURLFactory);
        requestContext.setSiteMapMatcher(siteMapMatcher);

        requestContext.setRenderHost(renderingHost);

        return requestContext;
    }

    public HstContainerURL createContainerUrlForCmsRequest(final HstMutableRequestContext requestContext,
                                                           final String hostAndPort,
                                                           final String pathInfo,
                                                           final String queryString,
                                                           final String renderingHost,
                                                           final boolean forceClientHost) throws Exception {
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

            requestContext.setCmsRequest(true);
            if (renderingHost != null) {
                request.setParameter(ContainerConstants.RENDERING_HOST, renderingHost);
            }
            if (forceClientHost) {
                request.setParameter("FORCE_CLIENT_HOST", "true");
            }
            containerRequest = new HstContainerRequestImpl(request, hstManager.getPathSuffixDelimiter());
        }

        requestContext.setServletRequest(containerRequest);

        VirtualHosts vhosts = hstManager.getVirtualHosts();
        ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(containerRequest),
                containerRequest.getContextPath(), HstRequestUtils.getRequestPath(containerRequest));
        setHstServletPath(containerRequest, mount);
        return hstURLFactory.getContainerURLProvider().parseURL(containerRequest, response, mount);
    }

    public ResolvedSiteMapItem getResolvedSiteMapItem(HstContainerURL url) throws ContainerException {
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        final ResolvedMount resolvedMount = vhosts.matchMount(url.getHostName(), url.getContextPath(), url.getRequestPath());
        final Mount decorated = mountDecorator.decorateMountAsPreview(resolvedMount.getMount());
        ((MutableResolvedMount) resolvedMount).setMount(decorated);
        return resolvedMount.matchSiteMapItem(url.getPathInfo());
    }


}
