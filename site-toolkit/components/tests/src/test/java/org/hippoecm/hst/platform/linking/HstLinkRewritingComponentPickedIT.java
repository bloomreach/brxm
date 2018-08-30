/*
 *  Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class HstLinkRewritingComponentPickedIT extends AbstractHstLinkRewritingIT {

    public static final String TMPDOC_NAME = "tmpdoc";
    public static final String TMPDOC_LOC =  "/unittestcontent/documents/unittestproject/" + TMPDOC_NAME;
    public static final String TMPDOC_NAME2 = "tmpdoc2";
    public static final String TMPDOC_LOC2 =  "/unittestcontent/documents/unittestproject/" + TMPDOC_NAME2;
    private Session session;



    @Before
    public void setUp() throws Exception {
        super.setUp();
        session = createAdminSession();
        createHstConfigBackup(session);
        createTmpDoc(TMPDOC_NAME);
    }

    private void createTmpDoc(final String tmpDocName) throws RepositoryException {
        // create tmpdoc that is not mapped via sitemap
        JcrUtils.copy(session, "/unittestcontent/documents/unittestproject/News/News1", "/unittestcontent/documents/unittestproject/" + tmpDocName);
        // rename document to handle name
        session.move("/unittestcontent/documents/unittestproject/"+tmpDocName+"/News1", "/unittestcontent/documents/unittestproject/" + tmpDocName + "/" + tmpDocName);
        session.save();
    }

    @After
    public void tearDown() throws Exception {
        restoreHstConfigBackup(session);
        if (session.itemExists(TMPDOC_LOC)) {
            session.removeItem(TMPDOC_LOC);
        }
        if (session.itemExists(TMPDOC_LOC2)) {
            session.removeItem(TMPDOC_LOC2);
        }
        session.save();
        session.logout();
        super.tearDown();
    }

    @Test
    public void assert_document_not_linked_at_all_results_in_not_found() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode(TMPDOC_LOC);
        final HstLink hstLink = linkCreator.create(tmpDoc, requestContext);
        assertTrue(hstLink.isNotFound());
    }

    @SuppressWarnings("ALL")
    public static interface TestJcrPathAbsoluteI {
        @Parameter(name = "myproject-picked-news-jcrpath-absolute", displayName = "Picked News")
        @JcrPath(isRelative = false, pickerInitialPath = TMPDOC_NAME)
        String getPickedNews();
    }

    @ParametersInfo(type = TestJcrPathAbsoluteI.class)
    public static class TestJcrPathAbsolute extends GenericHstComponent {

    }

    @Test
    public void document_linked_via_component_jcrPath_absolute_and_not_with_sitemap_content_path() throws Exception {
        // add hst component class to the contactpage:
        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");
        contactPage.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestJcrPathAbsolute.class.getName());
        contactPage.setProperty("hst:parameternames", new String[]{"myproject-picked-news-jcrpath-absolute"});
        contactPage.setProperty("hst:parametervalues", new String[]{TMPDOC_LOC});
        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode(TMPDOC_LOC);

        final HstLink hstLink = linkCreator.create(tmpDoc, requestContext);
        assertEquals("contact", hstLink.getPath());
    }

    @SuppressWarnings("ALL")
    public static interface TestJcrPathRelativeI {
        @Parameter(name = "myproject-picked-news-jcrpath-relative", displayName = "Picked News")
        @JcrPath(isRelative = true, pickerInitialPath = TMPDOC_NAME)
        String getPickedNews();
    }

    @ParametersInfo(type = TestJcrPathRelativeI.class)
    public static class TestJcrPathRelative extends GenericHstComponent { }


    @Test
    public void document_linked_via_component_jcrPath_relative_and_not_with_sitemap_content_path() throws Exception {
        // add hst component class to the contactpage:
        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");
        contactPage.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestJcrPathRelative.class.getName());
        contactPage.setProperty("hst:parameternames", new String[]{"myproject-picked-news-jcrpath-relative"});
        contactPage.setProperty("hst:parametervalues", new String[]{TMPDOC_NAME});
        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode(TMPDOC_LOC);

        final HstLink hstLink = linkCreator.create(tmpDoc, requestContext);
        assertEquals("contact", hstLink.getPath());
    }

    @Test
    public void document_linked_via_two_components_within_same_page_and_not_with_sitemap_content_path() throws Exception {
        // add hst component class to the contactpage and to the body of contact page:
        String[] componentPaths = {"/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage",
                "/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage/body"};

        for (String componentPath : componentPaths) {
            Node componentNode = session.getNode(componentPath);
            componentNode.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestJcrPathRelative.class.getName());
            componentNode.setProperty("hst:parameternames", new String[]{"myproject-picked-news-jcrpath-relative"});
            componentNode.setProperty("hst:parametervalues", new String[]{TMPDOC_NAME});

        }
        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode(TMPDOC_LOC);

        HstSiteMapItem contactPreferred = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSiteMapItem("contact");
        HstSiteMapItem homePreferred = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSiteMapItem("home");
        final boolean fallback = true;
        final HstLink hstLink1 = linkCreator.create(tmpDoc, requestContext, contactPreferred, fallback);
        final HstLink hstLink2 = linkCreator.create(tmpDoc, requestContext, homePreferred, fallback);
        final HstLink hstLink3 = linkCreator.create(tmpDoc, requestContext, contactPreferred, !fallback);
        final HstLink hstLink4 = linkCreator.create(tmpDoc, requestContext, homePreferred, !fallback);
        assertEquals("contact", hstLink1.getPath());
        assertEquals("contact", hstLink2.getPath());
        assertEquals("contact", hstLink3.getPath());
        // preferred sitemap item home and fallback false can not create link
        assertTrue(hstLink4.isNotFound());
    }

    @Test
    public void document_linked_via_multiple_components_in_multiple_pages_and_not_with_sitemap_content_path_chooses_shortest_then_sorted() throws Exception {
        // add hst component class to the contactpage, searchpage and thankyou (thankyou is URL for /contactpage/thankyou)
        String[] componentPaths = {"/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage",
                "/hst:hst/hst:configurations/unittestcommon/hst:pages/searchpage",
                "/hst:hst/hst:configurations/unittestcommon/hst:pages/thankyou"};

        for (String componentPath : componentPaths) {
            Node componentNode = session.getNode(componentPath);
            componentNode.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestJcrPathRelative.class.getName());
            componentNode.setProperty("hst:parameternames", new String[]{"myproject-picked-news-jcrpath-relative"});
            componentNode.setProperty("hst:parametervalues", new String[]{TMPDOC_NAME});
        }

        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode(TMPDOC_LOC);
        HstSiteMapItem contactPreferred = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSiteMapItem("contact");
        HstSiteMapItem homePreferred = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSiteMapItem("home");

        final boolean fallback = true;
        final HstLink hstLink1 = linkCreator.create(tmpDoc, requestContext, contactPreferred, fallback);
        final HstLink hstLink2 = linkCreator.create(tmpDoc, requestContext, homePreferred, fallback);
        final HstLink hstLink3 = linkCreator.create(tmpDoc, requestContext, contactPreferred, !fallback);
        final HstLink hstLink4 = linkCreator.create(tmpDoc, requestContext, homePreferred, !fallback);
        assertEquals("contact", hstLink1.getPath());
        assertEquals("contact", hstLink2.getPath());
        assertEquals("contact", hstLink3.getPath());
        // preferred sitemap item home and fallback false can not create link
        assertTrue(hstLink4.isNotFound());

        final List<HstLink> allLinks = linkCreator.createAll(tmpDoc, requestContext, false);

        assertEquals(6, allLinks.size());
        assertEquals("contact", allLinks.get(0).getPath());
        assertEquals("search", allLinks.get(1).getPath());
        assertEquals("contact-dispatch/thankyou", allLinks.get(2).getPath());
        assertEquals("contact-spring/thankyou", allLinks.get(3).getPath());
        assertEquals("contact-springmvc/thankyou", allLinks.get(4).getPath());
        assertEquals("contact/thankyou", allLinks.get(5).getPath());

    }

    @Test
    public void linked_via_component_try_preferred_sitemap_item() throws Exception {
        // add hst component class to the contactpage:
        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");
        contactPage.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestJcrPathRelative.class.getName());
        contactPage.setProperty("hst:parameternames", new String[]{"myproject-picked-news-jcrpath-relative"});
        contactPage.setProperty("hst:parametervalues", new String[]{TMPDOC_NAME});
        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode(TMPDOC_LOC);

        HstSiteMapItem contactPreferred = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSiteMapItem("contact");
        HstSiteMapItem homePreferred = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSiteMapItem("home");
        final boolean fallback = true;
        final HstLink hstLink1 = linkCreator.create(tmpDoc, requestContext, contactPreferred, fallback);
        final HstLink hstLink2 = linkCreator.create(tmpDoc, requestContext, homePreferred, fallback);
        final HstLink hstLink3 = linkCreator.create(tmpDoc, requestContext, contactPreferred, !fallback);
        final HstLink hstLink4 = linkCreator.create(tmpDoc, requestContext, homePreferred, !fallback);
        assertEquals("contact", hstLink1.getPath());
        assertEquals("contact", hstLink2.getPath());
        assertEquals("contact", hstLink3.getPath());
        // preferred sitemap item home and fallback false can not create link
        assertTrue(hstLink4.isNotFound());
    }


    @Test
    public void linked_via_component_AND_via_sitemap_content_path_uses_sitemap_as_preferred() throws Exception {
        // add hst component class to the contactpage:
        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");
        contactPage.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestJcrPathRelative.class.getName());
        contactPage.setProperty("hst:parameternames", new String[]{"myproject-picked-news-jcrpath-relative"});
        contactPage.setProperty("hst:parametervalues", new String[]{TMPDOC_NAME});

        // add relative hst:relativecontentpath = tmpdoc to existing sitemap item for '/home'
        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home")
                .setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, TMPDOC_NAME);

        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode(TMPDOC_LOC);

        final HstLink hstLink = linkCreator.create(tmpDoc, requestContext);
        // home page has preference since in sitemap (which has precedence over component linked docs)
        assertEquals("", hstLink.getPath());

        HstSiteMapItem contactPreferred = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSiteMapItem("contact");
        final boolean fallback = true;
        final HstLink hstLinkPreferredContact1 = linkCreator.create(tmpDoc, requestContext, contactPreferred, fallback);
        final HstLink hstLinkPreferredContact2 = linkCreator.create(tmpDoc, requestContext, contactPreferred, !fallback);

        assertEquals("contact", hstLinkPreferredContact1.getPath());
        assertEquals("contact", hstLinkPreferredContact2.getPath());

        HstSiteMapItem newsPreferred = requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getHstSiteMap().getSiteMapItem("news");
        final HstLink hstLinkPreferredNews1 = linkCreator.create(tmpDoc, requestContext, newsPreferred, fallback);
        final HstLink hstLinkPreferredNews2 = linkCreator.create(tmpDoc, requestContext, newsPreferred, !fallback);

        // home page has preference since in sitemap (which has precedence over component linked docs)
        assertEquals("", hstLinkPreferredNews1.getPath());
        // preferred sitemap item news and fallback false can not create link
        assertTrue(hstLinkPreferredNews2.isNotFound());


        final List<HstLink> allLinks = linkCreator.createAll(tmpDoc, requestContext, false);
        // createAll order does not take sitemap or component precedence into account but just by depth and then lexical
        assertEquals("", allLinks.get(0).getPath());
        assertEquals("contact", allLinks.get(1).getPath());
    }

    @Test
    public void document_linked_via_component_AND_via_sitemap_content_path_uses_sitemap_as_preferred_even_if_sitemap_path_deeper() throws Exception {

        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");
        contactPage.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestJcrPathRelative.class.getName());
        contactPage.setProperty("hst:parameternames", new String[]{"myproject-picked-news-jcrpath-relative"});
        contactPage.setProperty("hst:parametervalues", new String[]{TMPDOC_NAME});

        // add relative hst:relativecontentpath = tmpdoc to existing sitemap item for '/contact/thankyou' which is a deeper
        // path than for 'contactpage' (/contact)
        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap/contact/thankyou")
                .setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, TMPDOC_NAME);

        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode(TMPDOC_LOC);

        final HstLink hstLink = linkCreator.create(tmpDoc, requestContext);
        // contact/thankyou has preference since sitemap has precedence over component linked docs
        assertEquals("contact/thankyou", hstLink.getPath());

        final List<HstLink> allLinks = linkCreator.createAll(tmpDoc, requestContext, false);

        // createAll order does not take sitemap or component precedence into account but just by depth and then lexical
        assertEquals("contact", allLinks.get(0).getPath());
        assertEquals("contact/thankyou", allLinks.get(1).getPath());
    }

    @Test
    public void document_linked_via_component_AND_via_sitemap_content_path_uses_sitemap_as_preferred_even_if_sitemapItem_contains_wildcards() throws Exception {

        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");
        contactPage.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestJcrPathRelative.class.getName());
        contactPage.setProperty("hst:parameternames", new String[]{"myproject-picked-news-jcrpath-relative"});
        contactPage.setProperty("hst:parametervalues", new String[]{"News/News1"});

        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node newsDoc = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");

        final HstLink hstLink = linkCreator.create(newsDoc, requestContext);
        // news/**.html has preference since sitemap has precedence over component linked docs
        assertEquals("news/News1.html", hstLink.getPath());

        final List<HstLink> allLinks = linkCreator.createAll(newsDoc, requestContext, false);

        // createAll order does not take sitemap or component precedence into account but just by depth and then lexical
        assertEquals("contact", allLinks.get(0).getPath());
        assertEquals("news/News1.html", allLinks.get(1).getPath());
        assertEquals("alsonews/news2/News1.html", allLinks.get(2).getPath());

    }

    @Test
    public void component_document_link_containing_parameterized_path_with_placeholder_from_sitemap_item() throws Exception {
        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");
        contactPage.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestJcrPathRelative.class.getName());
        contactPage.setProperty("hst:parameternames", new String[]{"myproject-picked-news-jcrpath-relative"});

        // property-foo should get substitude by the contactSiteMapItem parameter value for 'property-foo'
        contactPage.setProperty("hst:parametervalues", new String[]{"${property-foo}"});

        // add relative hst:relativecontentpath = tmpdoc to existing sitemap item for '/contact/thankyou' which is a deeper
        // path than for 'contactpage' (/contact)
        final Node contactSiteMapItem = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap/contact");

        contactSiteMapItem.setProperty("hst:parameternames", new String[]{"property-foo"});
        contactSiteMapItem.setProperty("hst:parametervalues", new String[]{TMPDOC_NAME});

        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode(TMPDOC_LOC);
        final HstLink hstLink = linkCreator.create(tmpDoc, requestContext);
        assertEquals("contact", hstLink.getPath());
    }

    @Test
    public void component_document_link_containing_unresolvable_parameterized_path_is_skipped() throws Exception {

        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");
        contactPage.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, TestJcrPathRelative.class.getName());
        contactPage.setProperty("hst:parameternames", new String[]{"myproject-picked-news-jcrpath-relative"});

        // property-foo should get substitude by the contactSiteMapItem parameter value for 'property-foo'
        contactPage.setProperty("hst:parametervalues", new String[]{"${unresolvable-property-foo}"});

        session.save();

        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        Node tmpDoc = requestContext.getSession().getNode(TMPDOC_LOC);
        final HstLink hstLink = linkCreator.create(tmpDoc, requestContext);
        assertEquals("pagenotfound", hstLink.getPath());
    }


}