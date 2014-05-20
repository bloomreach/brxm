/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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


import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.beans.AbstractBeanTestCase;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class TestHstLinkRewriting extends AbstractBeanTestCase {

    private ObjectConverter objectConverter;
    private HstLinkCreator linkCreator;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.objectConverter = getObjectConverter();
        this.linkCreator = getComponent(HstLinkCreator.class.getName());
    }

    @Test
    public void testSimpleHstLinkForBean() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
        Object homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
        HstLink homePageLink = linkCreator.create((HippoBean) homeBean, requestContext);
        assertEquals("link.getPath for homepage node should be '", "", homePageLink.getPath());
        assertEquals("wrong absolute link for homepage", "/site", (homePageLink.toUrlForm(requestContext, false)));
        assertEquals("wrong fully qualified url for homepage", "http://localhost/site", (homePageLink.toUrlForm(requestContext, true)));


        requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:80", "/home");
        obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
        homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
        homePageLink = linkCreator.create((HippoBean) homeBean, requestContext);
        assertEquals("link.getPath for homepage node should be ''", "", homePageLink.getPath());
        assertEquals("wrong absolute link for homepage", "/site", (homePageLink.toUrlForm(requestContext, false)));
        // for absolute links, we do not include port 80 !!
        assertEquals("wrong fully qualified url for homepage", "http://localhost/site", (homePageLink.toUrlForm(requestContext, true)));

        requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:443", "/home");
        obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
        homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
        homePageLink = linkCreator.create((HippoBean) homeBean, requestContext);
        assertEquals("link.getPath for homepage node should be ''", "", homePageLink.getPath());
        assertEquals("wrong absolute link for homepage", "/site", (homePageLink.toUrlForm(requestContext, false)));
        // for absolute links, we do not include port 443 !!
        assertEquals("wrong fully qualified url for homepage", "http://localhost/site", (homePageLink.toUrlForm(requestContext, true)));


        requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/home");
        obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
        homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
        homePageLink = linkCreator.create((HippoBean) homeBean, requestContext);
        assertEquals("link.getPath for homepage node should be ''", "", homePageLink.getPath());
        assertEquals("wrong absolute link for homepage", "/site", (homePageLink.toUrlForm(requestContext, false)));
        // for absolute links, we do not include port 443 !!
        assertEquals("wrong fully qualified url for homepage", "http://localhost:8080/site", (homePageLink.toUrlForm(requestContext, true)));


        // on port 8081 we have the preview mount
        requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8081", "/home");
        obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
        homeBean = obm.getObject("/unittestcontent/documents/unittestproject/common/homepage");
        homePageLink = linkCreator.create((HippoBean) homeBean, requestContext);
        assertEquals("link.getPath for homepage node should be ''", "", homePageLink.getPath());
        assertEquals("wrong absolute link for homepage", "/site", (homePageLink.toUrlForm(requestContext, false)));
        assertEquals("wrong fully qualified url for homepage", "http://localhost:8081/site", (homePageLink.toUrlForm(requestContext, true)));


        requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/home");
        obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
        Object newsBean = obm.getObject("/unittestcontent/documents/unittestproject/News/News1");
        HstLink newsLink = linkCreator.create((HippoBean) newsBean, requestContext);
        assertEquals("wrong link.getPath for News/News1", "news/News1.html", newsLink.getPath());
        assertEquals("wrong absolute link for News/News1", "/site/news/News1.html", (newsLink.toUrlForm(requestContext, false)));
        assertEquals("wrong fully qualified url for News/News1", "http://localhost:8080/site/news/News1.html", (newsLink.toUrlForm(requestContext, true)));

        Node handleNode = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
        newsLink = linkCreator.create(handleNode, requestContext);
        assertEquals("wrong link.getPath for News/News1", "news/News1.html", newsLink.getPath());
        assertEquals("wrong absolute link for News/News1", "/site/news/News1.html", (newsLink.toUrlForm(requestContext, false)));
        assertEquals("wrong fully qualified url for News/News1", "http://localhost:8080/site/news/News1.html", (newsLink.toUrlForm(requestContext, true)));

        Node docNode = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1/News1");
        newsLink = linkCreator.create(docNode, requestContext);
        assertEquals("wrong link.getPath for News/News1", "news/News1.html", newsLink.getPath());
        assertEquals("wrong absolute link for News/News1", "/site/news/News1.html", (newsLink.toUrlForm(requestContext, false)));
        assertEquals("wrong fully qualified url for News/News1", "http://localhost:8080/site/news/News1.html", (newsLink.toUrlForm(requestContext, true)));

    }

    /**
     * the site root content node should return a link to the homepage
     *
     * @throws Exception
     */
    @Test
    public void testLinkHomePage() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/home");
        Node siteRootContentNode = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject");
        HstLink homePageLink = linkCreator.create(siteRootContentNode, requestContext);
        assertEquals("wrong link.getPath for /unittestcontent/documents/unittestproject : We expect the homepage for the site content root node ", "", homePageLink.getPath());
    }

    @Test
    public void testLinkNotFound() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/home");
        Node someNode = requestContext.getSession().getNode("/unittestcontent");
        HstLink notFoundLink = linkCreator.create(someNode, requestContext);
        assertEquals("wrong link.getPath for random node that does not belong to site content: Expected was a page not found link", "pagenotfound", notFoundLink.getPath());
    }

    /**
     * Linkrewriting with current context is alsonews : Now, a link for alsonews/news2 is expected
     *
     * @throws Exception
     */
    @Test
    public void testContextAwareHstLinkForBean() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/alsonews");

        ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
        Object newsBean = obm.getObject("/unittestcontent/documents/unittestproject/News/News1");
        HstLink newsLink = linkCreator.create((HippoBean) newsBean, requestContext);
        assertEquals("wrong link.getPath for News/News1", "alsonews/news2/News1.html", newsLink.getPath());
        assertEquals("wrong absolute link for News/News1", "/site/alsonews/news2/News1.html", (newsLink.toUrlForm(requestContext, false)));
        assertEquals("wrong fully qualified url for News/News1", "http://localhost:8080/site/alsonews/news2/News1.html", (newsLink.toUrlForm(requestContext, true)));

    }

    /**
     * Linkrewriting with current context is newsCtxOnly/news : Now, a link for /newsCtxOnly/news is expected
     *
     * @throws Exception
     */
    @Test
    public void testContextOnlyHstLinkForBean() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/newsCtxOnly/foo");

        ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
        Object newsBean = obm.getObject("/unittestcontent/documents/unittestproject/News/News1");
        HstLink newsLink = linkCreator.create((HippoBean) newsBean, requestContext);
        assertEquals("wrong link.getPath for News/News1", "newsCtxOnly/foo/news/News1.html", newsLink.getPath());
        assertEquals("wrong absolute link for News/News1", "/site/newsCtxOnly/foo/news/News1.html", (newsLink.toUrlForm(requestContext, false)));
        assertEquals("wrong fully qualified url for News/News1", "http://localhost:8080/site/newsCtxOnly/foo/news/News1.html", (newsLink.toUrlForm(requestContext, true)));


    }

    @Test
    public void testGettingPreferredSiteMapItemIgnoreStartingSlash() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/news");
        HstSiteMapItem sitemapItem1 = requestContext.getSiteMapMatcher().match("/alsonews", requestContext.getResolvedMount()).getHstSiteMapItem();
        HstSiteMapItem sitemapItem2 = requestContext.getSiteMapMatcher().match("alsonews", requestContext.getResolvedMount()).getHstSiteMapItem();
        assertTrue("We should get the same sitemap item for /alsonews and alsonews but we didn't", sitemapItem1 == sitemapItem2);
    }

    /**
     * Even though the context of the current request is /news, we can get a different link then /news by the use of
     * preferredSitemapItem. Also, the use of fallback & context only concepts are tested.
     *
     * @throws Exception
     */
    @Test
    public void testPreferredSitemapItemHstLinkForBean() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/news");
        Node node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");

        // the preferredSiteMap item is 'alsonews' hence we expect a link to 'alsonews' instead of the
        // /news/News1.html which we would have gotten normally
        HstSiteMapItem preferSiteMapItem = requestContext.getSiteMapMatcher().match("alsonews", requestContext.getResolvedMount()).getHstSiteMapItem();
        HstLink newsLink = linkCreator.create(node, requestContext, preferSiteMapItem, false);
        assertEquals("wrong link.getPath for News/News1", "alsonews/news2/News1.html", newsLink.getPath());
        // with or without fallback, we get the same result
        newsLink = linkCreator.create(node, requestContext, preferSiteMapItem, true);
        assertEquals("wrong link.getPath for News/News1", "alsonews/news2/News1.html", newsLink.getPath());

        // we now set the preferredSiteMap item to a sitemap part that can only a link can be created for if the correct
        // context is injected. The current context is still /news. Hence, the HST will not create a link for the preferred SitemapItem newsCtxOnly/foo but
        // for /news
        preferSiteMapItem = requestContext.getSiteMapMatcher().match("newsCtxOnly/foo", requestContext.getResolvedMount()).getHstSiteMapItem();
        newsLink = linkCreator.create(node, requestContext, preferSiteMapItem, true);
        assertEquals("wrong link.getPath for News/News1", "news/News1.html", newsLink.getPath());

        // if we now set fallback to false, we will get a not found link because the "newsCtxOnly/foo" sitemap subtree cannot
        // create a link for the item because it misses the context from the resolvedSitemapItem
        newsLink = linkCreator.create(node, requestContext, preferSiteMapItem, false);
        assertEquals("wrong link.getPath for News/News1", "pagenotfound", newsLink.getPath());

    }

    /**
     * Canonical link does not take into account the current context, and never returns a link that can only be created
     * with a context (CtxOnly).
     * <p/>
     * /newsalso/news and /news both are possible to use without a context (opposed to /newsCtxOnly/foo). Also, the
     * matchers (** relativecontenpath = ${1}) are equally suited. The last check for canonical links, is that if there
     * are two equally suited sitemap items, that the one with the shortest (number of slashes) path is used, thus /news
     * and not /newsonly/news
     *
     * @throws Exception
     */
    @Test
    public void testCanonicalHstLinkForBean() throws Exception {
        // current context = /newsCtxOnly
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/newsCtxOnly/news");
        Node node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
        HstLink canonicalNewsLink = linkCreator.createCanonical(node, requestContext);
        assertEquals("wrong canonical link.getPath for News/News1", "news/News1.html", canonicalNewsLink.getPath());

        // current context = /news
        requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/news");
        node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
        canonicalNewsLink = linkCreator.createCanonical(node, requestContext);
        assertEquals("wrong canonical link.getPath for News/News1", "news/News1.html", canonicalNewsLink.getPath());

        // current context = /alsonews
        requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/alsonews");
        node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
        canonicalNewsLink = linkCreator.createCanonical(node, requestContext);
        assertEquals("wrong canonical link.getPath for News/News1", "news/News1.html", canonicalNewsLink.getPath());

    }


    @Test
    public void testLinkBySitemapItemRefId() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/news");
        HstLink homePageLink = linkCreator.createByRefId("homeRefId", requestContext.getResolvedMount().getMount());
        assertEquals("Wrong link for sitemapItemRefId 'homeRefId' ", "", homePageLink.getPath());

        HstLink newsPageLink = linkCreator.createByRefId("newsRefId", requestContext.getResolvedMount().getMount());
        assertEquals("Wrong link for sitemapItemRefId 'newsRefId' ", "news", newsPageLink.getPath());

        // a refId on a sitemap item that is a wildcard (or one of its ancestors) can not be used to create a link for. It just returns
        // the sitemap item path however
        HstLink wildcardRefIdLink = linkCreator.createByRefId("wildcardNewsRefId", requestContext.getResolvedMount().getMount());
        assertEquals("Wrong link for sitemapItemRefId 'wildcardNewsRefId' ", "news/_default_.html", wildcardRefIdLink.getPath());

        // non existing refId test
        HstLink nonExistingRefIdLink = linkCreator.createByRefId("nonExistingRefId", requestContext.getResolvedMount().getMount());
        assertTrue("Wrong link for sitemapItemRefId 'wildcardNewsRefId' ", nonExistingRefIdLink == null);

    }

    @Test
    public void testNavigationStatefulLink() throws Exception {

        // test first a preview navigation stateful URL. We need to get the node/bean from the preview context
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL(null, "localhost:8081", "/news", "query=foo&page=6");
        Node node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
        HstLink navigationStatefulNewsLink = linkCreator.create(node, requestContext, null, false, true);
        assertEquals("wrong navigationStateful link.getPath for /unittestcontent/documents/unittestproject/News/News1", "news/News1.html", navigationStatefulNewsLink.getPath());
    }

    @Test
    public void testExcludedForLinkRewritingSitemapItem() throws Exception {
        // current context points to a location that has a sitemap item that only contains items that are excluded for linkrewriting. Thus, not a link
        // below /newswith_linkrwriting_excluded should be returned
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/newswith_linkrwriting_excluded");
        ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
        Object newsBean = obm.getObject("/unittestcontent/documents/unittestproject/News/News1");
        HstLink newsLink = linkCreator.create((HippoBean) newsBean, requestContext);
        // even though current context is /newswith_linkrwriting_excluded and it has sitemap items that could create a link for
        // the news item, it is not
        assertEquals("wrong link.getPath for News/News1", "news/News1.html", newsLink.getPath());

        // Now, show that if we include the 'preferSitemapItem' that is
        // and at the same time specify fallback is false, that we do get a pagenotfound link, because .. has only items
        // that are specified for linkrewriting = false
        HstSiteMapItem preferSiteMapItem = requestContext.getSiteMapMatcher().match("newswith_linkrwriting_excluded", requestContext.getResolvedMount()).getHstSiteMapItem();
        newsLink = linkCreator.create(((HippoBean) newsBean).getNode(), requestContext, preferSiteMapItem, false);
        assertEquals("wrong link.getPath for News/News1", "pagenotfound", newsLink.getPath());

    }


    @Test
    public void testCrossSiteAndDomainHstLinkForBean() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8080", "/news2");
        // the current request is for the unittestproject. Now, we fetch a node from the unittestsubproject and ask a link for it. The
        // unittestsubproject mount is located below the current unittest live mount

        ObjectBeanManager obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
        Object newsBean = obm.getObject("/unittestcontent/documents/unittestsubproject/News/2008/SubNews1");
        HstLink crossSiteNewsLink = linkCreator.create((HippoBean) newsBean, requestContext);

        assertEquals("wrong link.getPath for News/2008/SubNews1 ", "news/2008/SubNews1.html", crossSiteNewsLink.getPath());
        assertEquals("wrong absolute link for News/News1", "/site/subsite/news/2008/SubNews1.html", (crossSiteNewsLink.toUrlForm(requestContext, false)));
        assertEquals("wrong fully qualified url for News/News1", "http://localhost:8080/site/subsite/news/2008/SubNews1.html", (crossSiteNewsLink.toUrlForm(requestContext, true)));

        // We now do a request that is for the preview site (PORT 8081). Because for the 'unittestsubproject' we have only
        // configured LIVE mounts, we should not be able to cross-site link from preview to live environemt
        requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost:8081", "/news2");

        obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
        newsBean = obm.getObject("/unittestcontent/documents/unittestsubproject/News/2008/SubNews1");
        HstLink notFoundCrossSiteNewsLink = linkCreator.create((HippoBean) newsBean, requestContext);

        assertEquals("wrong link.getPath for News/2008/SubNews1: We should not be able to " +
                "link from preview to live cross-site ", "pagenotfound", notFoundCrossSiteNewsLink.getPath());

        // we now do a request is for *www.unit.test which* is part of the 'testgroup' hostgroup
        // an internal link to a unittestsubproject is now part of a different host (sub.unit.test)
        // this means, we should get a fully qualified link even if we do not explicitly ask for one (decause it is cross domain)

        requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("www.unit.test:8080", "/news2");


        obm = new ObjectBeanManagerImpl(requestContext.getSession(), objectConverter);
        newsBean = obm.getObject("/unittestcontent/documents/unittestsubproject/News/2008/SubNews1");
        HstLink crossSiteAndDomainNewsLink = linkCreator.create((HippoBean) newsBean, requestContext);

        assertEquals("wrong link.getPath for News/2008/SubNews1 ", "news/2008/SubNews1.html", crossSiteAndDomainNewsLink.getPath());
        assertEquals("wrong absolute link for News/News1", "http://sub.unit.test:8080/site/news/2008/SubNews1.html", (crossSiteAndDomainNewsLink.toUrlForm(requestContext, false)));
        assertEquals("wrong fully qualified url for News/News1", "http://sub.unit.test:8080/site/news/2008/SubNews1.html", (crossSiteAndDomainNewsLink.toUrlForm(requestContext, true)));
        // whether fullyQualified is true or false, for cross domain links we should always get a fully qualified url
        assertTrue("fully qualified with true or false should not matter for cross domain links", crossSiteAndDomainNewsLink.toUrlForm(requestContext, false).equals(crossSiteAndDomainNewsLink.toUrlForm(requestContext, true)));
    }


    /**
     * returns all the available canonical links within some HOSTGROUP for a HippoBean / node
     * <p/>
     * The /news2 node is available on www.unit.test but also on m.unit.test. We should get a HstLink for both these
     * locations
     *
     * @throws Exception
     */
    @Test
    public void testAllCanonnicalHstLinksForBean() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("www.unit.test:8080", "/news2");
        // the current request is for the unittestproject. Now, we fetch a node from the unittestsubproject and ask a link for it. The
        // unittestsubproject mount is located below the current unittest live mount

        Node node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
        List<HstLink> allCanonicalNewsLinks = linkCreator.createAllAvailableCanonicals(node, requestContext);

        assertTrue("There should be a canonical link for www.unit.test/hst:root, m.unit.test/hst:root and for www.unit.test/custompipeline ",
                allCanonicalNewsLinks.size() == 3);

        // list of non fully qualified URLs. Note that for the m.unit.test host it is fully qualified, because cross domain
        List<String> expectedNonFullyQualifiedListOfURLs = Arrays.asList("/site/custompipeline/news/News1.html", "/site/news/News1.html", "http://m.unit.test:8080/site/news/News1.html");
        List<String> expectedFullyQualifiedListOfURLs = Arrays.asList("http://www.unit.test:8080/site/custompipeline/news/News1.html", "http://www.unit.test:8080/site/news/News1.html", "http://m.unit.test:8080/site/news/News1.html");

        for (HstLink link : allCanonicalNewsLinks) {
            assertEquals("The getPath for all links should be all 'news/News1.html' ", "news/News1.html", link.getPath());
            String nonFQU = link.toUrlForm(requestContext, false);
            assertTrue("Unexpected non fully qualfied URL '" + nonFQU + "'", expectedNonFullyQualifiedListOfURLs.contains(nonFQU));
            String fqu = link.toUrlForm(requestContext, true);
            assertTrue("Unexpected fully qualfied URL '" + fqu + "'", expectedFullyQualifiedListOfURLs.contains(fqu));
        }

        // we now give an explicit TYPE the mounts for the links should have. We do not have m.unit.test as preview, hence, expect this time only
        // two canonicalNewsLinks, and they should be preview

        List<HstLink> allPreviewCanonicalNewsLinks = linkCreator.createAllAvailableCanonicals(node, requestContext, "preview");

        assertTrue("There should be a canonical link for preview.unit.test/hst:root, and for preview.unit.test/custompipeline (and not for m.unit.test) ",
                allPreviewCanonicalNewsLinks.size() == 2);

        // list of non fully qualified URLs. Note that since they are links to the preview in a different domain, they should all be fully qualified
        List<String> expectedListOfURLs = Arrays.asList("http://preview.unit.test:8080/site/custompipeline/news/News1.html", "http://preview.unit.test:8080/site/news/News1.html");

        for (HstLink link : allPreviewCanonicalNewsLinks) {
            assertEquals("The getPath for all links should be all 'news/News1.html' ", "news/News1.html", link.getPath());
            assertEquals("Expected non fully qualfied URL to be equal to fully qualified", link.toUrlForm(requestContext, false), link.toUrlForm(requestContext, true));
            String fqu = link.toUrlForm(requestContext, true);
            assertTrue("Unexpected fully qualfied URL '" + fqu + "'", expectedListOfURLs.contains(fqu));
        }

        // we now give an explicit HOSTGROUPNAME. We use hostGroupName dev-localhost

        List<HstLink> allCanonicalDevLocalNewsLinks = linkCreator.createAllAvailableCanonicals(node, requestContext, null, "dev-localhost");

        assertTrue("There should be a canonical link for localhost hst:root, and for localhost intranet",
                allCanonicalDevLocalNewsLinks.size() == 2);

        List<String> expectedListOfLiveDevLocalURLs = Arrays.asList("http://localhost:8080/site2/intranet/news/News1.html", "http://localhost:8080/site/news/News1.html");

        for (HstLink link : allCanonicalDevLocalNewsLinks) {
            assertEquals("The getPath for all links should be all 'news/News1.html' ", "news/News1.html", link.getPath());
            assertEquals("Expected non fully qualfied URL to be equal to fully qualified", link.toUrlForm(requestContext, false), link.toUrlForm(requestContext, true));
            String fqu = link.toUrlForm(requestContext, true);
            assertTrue("Unexpected fully qualfied URL '" + fqu + "'", expectedListOfLiveDevLocalURLs.contains(fqu));
        }

        // we now have TYPE  equal to 'preview' and also give an explicit HOSTGROUPNAME. We use hostGroupName dev-localhost

        List<HstLink> allCanonicalPreviewDevLocalNewsLinks = linkCreator.createAllAvailableCanonicals(node, requestContext, "preview", "dev-localhost");

        // there is also a preview at 'http://localhost:8081/site/news/News1.html' so, three in total

        assertTrue("There should be a canonical link for localhost port 8080 hst:root, and for localhost port 8080 custompipeline and for localhost port 8081 hst:root",
                allCanonicalPreviewDevLocalNewsLinks.size() == 3);

        List<String> expectedListOfPreviewDevLocalURLs = Arrays.asList(
                "http://localhost:8080/site/preview/custompipeline/news/News1.html",
                "http://localhost:8080/site/preview/news/News1.html", "http://localhost:8081/site/news/News1.html");

        for (HstLink link : allCanonicalPreviewDevLocalNewsLinks) {
            assertEquals("The getPath for all links should be all 'news/News1.html' ", "news/News1.html", link.getPath());
            assertEquals("Expected non fully qualfied URL to be equal to fully qualified", link.toUrlForm(requestContext, false), link.toUrlForm(requestContext, true));
            String fqu = link.toUrlForm(requestContext, true);
            assertTrue("Unexpected fully qualfied URL '" + fqu + "'", expectedListOfPreviewDevLocalURLs.contains(fqu));
        }

    }

    /**
     * This test assures the following linkrewriting capabilities:
     *
     * If you have a configuration like this:
     *   global (mount)
     *      sub1 (mount)
     *          subsub1(mount)
     *      sub2 (mount)
     *
     *   documents
     *      content
     *        global
     *           sub1
     *             subsub1
     *           sub2
     *
     *   and you have:
     *
     *   mount 'global' --> /documents/content/unittestcontent
     *   mount 'global/sub1' --> /documents/content/global/unittestcontent/common
     *   mount 'global/sub1/subsub1' --> /documents/content/global/unittestcontent/common/aboutfolder
     *   mount 'global/sub2' --> /documents/content/global/unittestcontent/News
     *
     *   The the HST MUST do the following linkrewriting:
     *
     *   1) If my current context is the global site, and I have some document bean of a node below
     *   /documents/content/global, then
     *
     *      a) If the global sitemap can create a link for it, the HST needs to return a link for the global site
     *      b) If the global sitemap cannot create a link for it, the HST needs to try to create a link with another mount: For
     *      example sub1, subsub1 or sub2. If sub1 and subsub1 are both capable of creating a link, then the following algorithm is applied
     *                1) Firstly order the candidate mounts to have the same primary type as the current Mount of this HstLinkResolver
     *                 2) Secondly order the candidate mounts that have the most 'types' in common with the current Mount of this HstLinkResolver
     *                 3) Thirdly order the Mounts to have the fewest types first: The fewer types it has, and the number of matching types is equal to the current Mount, indicates
     *                 that it can be considered more precise
     *                 4) Fourthly order the Mounts first that have the deepest (most slashes) #getCanonicalContentPath() : The deeper the more specific.
     *
     *
     *   2) If my current context is for example /global/sub1 and sub1 cannot create a link for the bean, then a fallback to
     *   global, or global/sub1/subsub1 or global/sub2 should be tried
     *
     * Available since https://issues.onehippo.com/browse/HSTTWO-1670
     * @throws Exception
     */
    @Test
    public void testPartialCoveredContentMountFallBackHstLinkForBean() throws Exception {
        {
            // the request context is a link to the global site. This site has a sitemap that only contains 'search'
            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("www.unit.partial", "/search");
            Node node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
            // the current global site's sitemap cannot create a link for News/News1 (although the content is below its root content).
            // therefore, it is a partial content covering sitemap. It will try sub1, subsub1 and sub2. Sub2 is capable to create a link. It should return a link for sub1
            HstLink newsLink = linkCreator.createCanonical(node, requestContext);

            assertEquals("wrong Mount ", "sub2", newsLink.getMount().getName());
            assertEquals("wrong link.getPath for News/News1", "News1.html", newsLink.getPath());
            assertEquals("wrong absolute link for News/News1", "/site/sub2/News1.html", (newsLink.toUrlForm(requestContext, false)));

            node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/common/homepage");
            // we now have a node for the homepage.
            // the current global site's sitemap cannot create a link for the homepage (although the content is below its root content).
            // sub1 can create a link for the homepage, subsub1 cannot.

            HstLink homePageLink = linkCreator.createCanonical(node, requestContext);
            assertEquals("wrong Mount ", "sub1", homePageLink.getMount().getName());
            assertEquals("wrong link.getPath for common/homepage", "", homePageLink.getPath());
            assertEquals("wrong absolute link for common/homepage", "/site/sub1", (homePageLink.toUrlForm(requestContext, false)));

            // we now have a node for the common/aboutfolder/about-us/about-us.
            // the current global site's sitemap cannot create a link for the about-us node (although the content is below its root content).
            // sub1 can create a link for the about-us but *ALSO* subsub1. Because subsub1 has a deeper content path, subsub1 should be taken
            node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/common/aboutfolder/about-us/about-us");
            HstLink aboutUsLink = linkCreator.createCanonical(node, requestContext);
            assertEquals("wrong Mount ", "subsub1", aboutUsLink.getMount().getName());
            assertEquals("wrong link.getPath for common/aboutfolder/about-us/about-us", "about-us", aboutUsLink.getPath());
            assertEquals("wrong absolute link for common/aboutfolder/about-us/about-us", "/site/sub1/subsub1/about-us", (aboutUsLink.toUrlForm(requestContext, false)));

            // we now change the requestContext to be in sub1 site. Then, a link to the about-us page should
            // *STAY WITHIN* sub1, because sub1 can create a link for it.
            requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("www.unit.partial", "/sub1/home");

            node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/common/aboutfolder/about-us/about-us");
            HstLink aboutUsLinkAgain = linkCreator.createCanonical(node, requestContext);
            assertEquals("wrong Mount ", "sub1", aboutUsLinkAgain.getMount().getName());
            assertEquals("wrong link.getPath for common/aboutfolder/about-us/about-us", "about-us", aboutUsLinkAgain.getPath());
            assertEquals("wrong absolute link for common/aboutfolder/about-us/about-us", "/site/sub1/about-us", (aboutUsLinkAgain.toUrlForm(requestContext, false)));

            // and a link to a news item should link to sub2 still
            node = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
            HstLink newsLinkAgain = linkCreator.createCanonical(node, requestContext);
            assertEquals("wrong Mount ", "sub2", newsLinkAgain.getMount().getName());
            assertEquals("wrong link.getPath for News/News1", "News1.html", newsLinkAgain.getPath());
            assertEquals("wrong absolute link for News/News1", "/site/sub2/News1.html", (newsLinkAgain.toUrlForm(requestContext, false)));

        }

        {
            // we now change the requestContext to be in sub1/subsub1 site. Then, a link to the about-us page should
            // *STAY WITHIN* sub1/subsub1, because subsub1 can create a link for it. The homepage link however can only be
            // made by sub1, and should thus be used for the homepage node. A news link should still go to sub2

            HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("www.unit.partial", "/sub1/subsub1/about-us");
            Node homePageNode = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/common/homepage");
            Node aboutUsNode = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/common/aboutfolder/about-us");
            Node newsNode = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");

            HstLink aboutUsLink = linkCreator.createCanonical(aboutUsNode, requestContext);
            assertEquals("wrong Mount ", "subsub1", aboutUsLink.getMount().getName());
            assertEquals("wrong link.getPath for common/aboutfolder/about-us/about-us", "about-us", aboutUsLink.getPath());
            assertEquals("wrong absolute link for common/aboutfolder/about-us/about-us", "/site/sub1/subsub1/about-us", (aboutUsLink.toUrlForm(requestContext, false)));


            HstLink homePageLink = linkCreator.createCanonical(homePageNode, requestContext);
            assertEquals("wrong Mount ", "sub1", homePageLink.getMount().getName());
            assertEquals("wrong link.getPath for common/homepage", "", homePageLink.getPath());
            assertEquals("wrong absolute link for common/homepage", "/site/sub1", (homePageLink.toUrlForm(requestContext, false)));


            HstLink newsLink = linkCreator.createCanonical(newsNode, requestContext);
            assertEquals("wrong Mount ", "sub2", newsLink.getMount().getName());
            assertEquals("wrong link.getPath for News/News1", "News1.html", newsLink.getPath());
            assertEquals("wrong absolute link for News/News1", "/site/sub2/News1.html", (newsLink.toUrlForm(requestContext, false)));

        }
    }

    @Test
    public void testHashCodeLink() throws Exception {
        // @see: https://issues.onehippo.com/browse/HSTTWO-2167

        // hash only link:
        String url = "/#";
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/");
        HstLink link = linkCreator.create(url, requestContext.getResolvedMount().getMount());
        String result = link.toUrlForm(requestContext, false);
        assertEquals("/site" + url, result);

        url = "/home#";
        requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/");
        link = linkCreator.create(url, requestContext.getResolvedMount().getMount());
        result = link.toUrlForm(requestContext, false);
        assertEquals("/site" + url, result);

        url = "/home#chapter1";
        requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        link = linkCreator.create(url, requestContext.getResolvedMount().getMount());
        result = link.toUrlForm(requestContext, false);
        assertEquals("/site" + url, result);
        // encoding test
        url = "/ho me#chapter1";
        link = linkCreator.create(url, requestContext.getResolvedMount().getMount());
        result = link.toUrlForm(requestContext, false);
        assertEquals("/site/ho+me#chapter1", result);
        // query parameters
        url = "/home?hint=one&delay=two";
        link = linkCreator.create(url, requestContext.getResolvedMount().getMount());
        result = link.toUrlForm(requestContext, false);
        assertEquals("/site" + url, result);
        // query parameters with hash:
        url = "/home?hint=one&delay=two#chapter1";
        link = linkCreator.create(url, requestContext.getResolvedMount().getMount());
        result = link.toUrlForm(requestContext, false);
        assertEquals("/site" + url, result);
        //encoded  query parameters with hash:
        url = "/ho me?hint=one&delay=two#chapter1";
        link = linkCreator.create(url, requestContext.getResolvedMount().getMount());
        result = link.toUrlForm(requestContext, false);
        assertEquals("/site/ho+me?hint=one&delay=two#chapter1", result);
        // invalid hash/query
        url = "/home#chapter1?hint=one&delay=two";
        link = linkCreator.create(url, requestContext.getResolvedMount().getMount());
        result = link.toUrlForm(requestContext, false);
        assertEquals("/site/home#chapter1" + URLEncoder.encode("?hint=one&delay=two", "UTF-8"), result);
        // invalid hash/query
        url = "/home?hint=one#chapter1&delay=two";
        link = linkCreator.create(url, requestContext.getResolvedMount().getMount());
        result = link.toUrlForm(requestContext, false);
        assertEquals("/site/home?hint=one#chapter1" + URLEncoder.encode("&delay=two", "UTF-8"), result);
        // encoded  url with invalid hash
        url = "/ho me?hint=one#chapter1&delay=two";
        link = linkCreator.create(url, requestContext.getResolvedMount().getMount());
        result = link.toUrlForm(requestContext, false);
        assertEquals("/site/ho+me?hint=one#chapter1" + URLEncoder.encode("&delay=two", "UTF-8"), result);
    }

}
