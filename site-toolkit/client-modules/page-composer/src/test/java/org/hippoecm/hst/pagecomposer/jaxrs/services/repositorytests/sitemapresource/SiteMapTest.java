/*
 * Copyright 2022-2023 Bloomreach
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.sitemapresource;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapTreeItem;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.skyscreamer.jsonassert.comparator.JSONComparator;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class SiteMapTest extends AbstractSiteMapResourceTest {


    private SiteMapResource siteMapResource;

    private final ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        siteMapResource = HstServices.getComponentManager().getComponent(SiteMapResource.class.getName(), "org.hippoecm.hst.pagecomposer");

    }

    private HstRequestContext initContext(final Session userSession) throws Exception {

        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "home");
        ((HstMutableRequestContext) ctx).setSession(userSession);
        final PageComposerContextService pageComposerContextService = mountResource.getPageComposerContextService();
        final HstSite site = pageComposerContextService.getEditingPreviewSite();

        // override the config identifier to have sitemap id
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, ((CanonicalInfo) site.getSiteMap()).getCanonicalIdentifier());

        return ctx;
    }


    @Test
    public void shallow_tree_item_some_expected_routes_and_xpages_to_be_present_as_admin() throws Exception {

        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest());
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem root =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            final SiteMapTreeItem news = root.getChild("news");
            assertThat(news)
                    .as("Expected news")
                    .isNotNull();
            assertThat(news.isExpandable())
                    .as("Expected news to be an expandable folder")
                    .isTrue();
            assertThat(news.getChildren().size())
                    .as("Expected news children NOT yet loaded")
                    .isEqualTo(0);

            final SiteMapTreeItem aboutUs = root.getChild("about-us");
            assertThat(aboutUs)
                    .as("Expected about-us from HST sitemap routes")
                    .isNotNull();
            assertThat(aboutUs.isExpandable())
                    .as("Expected about-us from HST sitemap route to NOT be an expandable folder")
                    .isFalse();

            final SiteMapTreeItem experiences = root.getChild("experiences");
            assertThat(experiences)
                    .as("Expected experiences from XPage documents")
                    .isNotNull();
            assertThat(experiences.isExpandable())
                    .as("Expected experiences to be an expandable folder")
                    .isTrue();

            assertThat(experiences.getChildren().size())
                    .as("Expected experiences children NOT yet loaded")
                    .isEqualTo(0);
        }

        {
            HstRequestContext ctx = initContext(session);

            // unittestcontent/documents/unittestproject/News/2009/May/articleAsExpPage is an XPage document so expected
            // to be present in the sitemap
            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "about-us", false);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem aboutUsSitemapTreeItem =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(aboutUsSitemapTreeItem)
                    .as("Expected 'about-us' to exist in HST routes")
                    .isNotNull();
            assertThat(aboutUsSitemapTreeItem.isExpandable())
                    .as("Expected 'about-us' NOT to be expandable")
                    .isFalse();
            assertThat(aboutUsSitemapTreeItem.isExperiencePage())
                    .as("Expected 'about-us' to NOT be an XPage")
                    .isFalse();
        }

        {
            HstRequestContext ctx = initContext(session);

            // pathInfo with leading or trailing slashes is same as without the leading or trailing slashes
            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "/about-us/", false);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem aboutUsSitemapTreeItem =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(aboutUsSitemapTreeItem)
                    .as("Expected 'about-us' to exist in HST routes")
                    .isNotNull();
            assertThat(aboutUsSitemapTreeItem.isExpandable())
                    .as("Expected 'about-us' NOT to be expandable")
                    .isFalse();
            assertThat(aboutUsSitemapTreeItem.isExperiencePage())
                    .as("Expected 'about-us' to NOT be an XPage")
                    .isFalse();
        }

        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "news", false);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem newsSiteMapTreeItem =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();


            assertThat(newsSiteMapTreeItem.getChildren().size())
                    .as("Expected news children HAVE BEEN loaded")
                    .isGreaterThan(0);

            final SiteMapTreeItem news2009 = newsSiteMapTreeItem.getChild("2009");
            assertThat(news2009)
                    .as("Expected 2009")
                    .isNotNull();
            assertThat(news2009.isExpandable())
                    .as("Expected 2009 to be an expandable folder")
                    .isTrue();
            assertThat(news2009.isExperiencePage())
                    .as("Expected 2009 to NOT be an experience page")
                    .isFalse();

        }

        {
            HstRequestContext ctx = initContext(session);

            // unittestcontent/documents/unittestproject/News/2009/May/articleAsExpPage is an XPage document so expected
            // to be present in the sitemap
            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "news/2009/May", false);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem maySiteMapTreeItem =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(maySiteMapTreeItem)
                    .as("Expected 'news/2009/May' to exist in sitemap as result of XPage doc 'articleAsExpPage'")
                    .isNotNull();
            assertThat(maySiteMapTreeItem.isExpandable())
                    .as("Expected 'news/2009/May' to be expandable")
                    .isTrue();
            assertThat(maySiteMapTreeItem.isExperiencePage())
                    .as("Expected 'news/2009/May' to NOT be an XPage")
                    .isFalse();

            // unittestcontent/documents/unittestproject/News/2009/May/articleAsExpPage is an XPage document so expected
            // to be present in the sitemap
            final SiteMapTreeItem articleAsExpPage = maySiteMapTreeItem.getChild("articleAsExpPage.html");
            assertThat(articleAsExpPage)
                    .as("Expected articleAsExpPage.html")
                    .isNotNull();
            assertThat(articleAsExpPage.isExpandable())
                    .as("Expected articleAsExpPage to be NOT an expandable folder")
                    .isFalse();
            assertThat(articleAsExpPage.isExperiencePage())
                    .as("Expected articleAsExpPage to be an XPage")
                    .isTrue();
        }

        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "experiences", false);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem experiencesSiteMapTreeItem =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(experiencesSiteMapTreeItem)
                    .as("Expected 'experiences' to exist in sitemap as result of XPage docs")
                    .isNotNull();
            assertThat(experiencesSiteMapTreeItem.isExpandable())
                    .as("Expected 'experiences' to be expandable")
                    .isTrue();
            assertThat(experiencesSiteMapTreeItem.isExperiencePage())
                    .as("Expected 'experiences' to NOT be an XPage")
                    .isFalse();

            final SiteMapTreeItem expPage1 = experiencesSiteMapTreeItem.getChild("expPage1.html");
            assertThat(expPage1)
                    .as("Expected expPage1.html")
                    .isNotNull();
            assertThat(expPage1.isExpandable())
                    .as("Expected expPage1 to be NOT an expandable folder")
                    .isFalse();
            assertThat(expPage1.isExperiencePage())
                    .as("Expected expPage1 to be an XPage")
                    .isTrue();
        }

        // fetch sitemap item for an XPage item
        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "experiences/expPage1.html", false);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem expPage1Item =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(expPage1Item)
                    .as("Expected 'expPage1.html' to exist in sitemap as result of XPage docs")
                    .isNotNull();
            assertThat(expPage1Item.isExpandable())
                    .as("Expected 'expPage1.html' to NOT be expandable")
                    .isFalse();
            assertThat(expPage1Item.isExperiencePage())
                    .as("Expected 'expPage1' to be an XPage")
                    .isTrue();
        }

        // Test a not found
        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "some-non-existing-path", false);

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        }
        // Test a not found but existing parent
        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "/news/some-non-existing-path", false);
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

    }

    @Test
    public void site_map_search_basic_test_response_contract() throws Exception {

        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "home");
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem result =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(result.getName())
                    .isEqualTo("home");
            assertThat(result.isExpandable())
                    .isTrue();
            assertThat(result.getChildren().isEmpty())
                    .as("Children are not expected to be loaded already")
                    .isTrue();

        }
        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "alsonews");
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem result =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(result.getName())
                    .as("Root (home page) of tree is always expected to be present")
                    .isEqualTo("home");
            assertThat(result.isExpandable())
                    .isTrue();
            assertThat(result.isExpandable())
                    .isTrue();

            assertThat(result.getChildren().stream().map(child -> child.getName()).collect(Collectors.toList()))
                    .containsExactlyInAnyOrder("alsonews");

            final SiteMapTreeItem alsonews = result.getChild("alsonews");

            assertThat(alsonews.isExpandable())
                    .as("'alsonews' has explicit 'news2' item in SiteMap so should be expandable")
                    .isTrue();
            assertThat(alsonews.getChild("news2"))
                    .as("Because 'news2' does NOt match the filter query, it should NOT be in the result")
                    .isNull();

        }

        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "news");
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem result =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();


            assertThat(result.getChildren().stream().map(child -> child.getName()).collect(Collectors.toList()))
                    .containsExactlyInAnyOrder("alsonews", "news", "newsCtxOnly", "newswith_linkrewriting_excluded");

            final SiteMapTreeItem alsonews = result.getChild("alsonews");
            assertThat(alsonews.isExpandable())
                    .as("'alsonews' has explicit 'news2' item in SiteMap")
                    .isTrue();
            assertThat(alsonews.getChild("news2"))
                    .as("Because 'news2' matches the filter query, it should be in the result")
                    .isNotNull();
            assertThat(alsonews.getChild("news2").isExpandable())
                    .as("no XPage documents match below 'news2'")
                    .isFalse();

            final SiteMapTreeItem newswith_link_rewriting_excluded = result.getChild("newswith_linkrewriting_excluded");
            assertThat(newswith_link_rewriting_excluded.isExpandable())
                    .as("'newswith_link_rewriting_excluded' does not have route descendants and XPAge docs never match there")
                    .isFalse();

            final SiteMapTreeItem news = result.getChild("news");
            assertThat(news.isExpandable())
                    .as("'news' only wildcard routes below it but has a readable XPage document 'articleAsExpPage' below it " +
                            "hence expandable = true")
                    .isTrue();
            assertThat(news.getChildren().isEmpty())
                    .as("The children should not be loaded yet")
                    .isTrue();

        }

        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "articleAsExp");
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem result =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(result.getChildren().stream().map(child -> child.getName()).collect(Collectors.toList()))
                    .containsExactlyInAnyOrder("news");

            assertThat(result.getChild("news").isExpandable())
                    .isTrue();

            final SiteMapTreeItem articleAsExpPage = result.getChild("news").getChild("2009").getChild("May").getChild("articleAsExpPage.html");

            assertThat(articleAsExpPage.getName())
                    .isEqualTo("articleAsExpPage.html");

            assertThat(articleAsExpPage.isExpandable())
                    .isFalse();

        }

        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "experiences");
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem result =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(result.getChildren().stream().map(child -> child.getName()).collect(Collectors.toList()))
                    .containsExactlyInAnyOrder("experiences");

            assertThat(result.getChild("experiences").isExpandable())
                    .isTrue();
            assertThat(result.getChild("experiences").getChildren().isEmpty())
                    .as("The children are NOT expected in the search result")
                    .isTrue();
        }

        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "expPage");
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem result =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(result.getChildren().stream().map(child -> child.getName()).collect(Collectors.toList()))
                    .containsExactlyInAnyOrder("news", "experiences");

            assertThat(result.getChild("news").isExpandable())
                    .isTrue();
            assertThat(result.getChild("news").getChildren().isEmpty())
                    .as("The children ARE expected in the search result")
                    .isFalse();
            assertThat(result.getChild("news").getChild("2009").getChild("May").getChild("articleAsExpPage.html"))
                    .isNotNull();

            assertThat(result.getChild("experiences").isExpandable())
                    .isTrue();
            assertThat(result.getChild("experiences").getChildren().isEmpty())
                    .as("The children ARE expected in the search result")
                    .isFalse();

            assertThat(result.getChild("experiences").getChildren().stream().map(child -> child.getName()).collect(Collectors.toList()))
                    .containsExactlyInAnyOrder("expPage1.html", "expPage2.html", "expPage-with-static-components.html");

            assertThat(result.getChild("experiences").getChild("expPage1.html").getPageTitle())
                    .isEqualTo("Experience Page Number1");
        }

        // Search is also done in pageTitle field
        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "Experience Page Number1");
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem result =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(result.getChildren().stream().map(child -> child.getName()).collect(Collectors.toList()))
                    .containsExactlyInAnyOrder("experiences");

            assertThat(result.getChild("experiences").getChildren().stream().map(child -> child.getName()).collect(Collectors.toList()))
                    .containsExactlyInAnyOrder("expPage1.html");

        }
    }


    @Test
    public void site_map_search_no_hits_returns_only_home() throws Exception {

        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), UUID.randomUUID().toString());
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            SiteMapTreeItem result =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(result.getName())
                    .isEqualTo("home");
            assertThat(result.isExpandable())
                    .isTrue();
            assertThat(result.isExpandable())
                    .isTrue();
        }

    }

    @Test
    public void assert_site_map_search_case_insensitive() throws Exception {
        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "ExPeRIENce PAge NuMber1");
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem result =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(result.getChildren().stream().map(child -> child.getName()).collect(Collectors.toList()))
                    .containsExactlyInAnyOrder("experiences");

            assertThat(result.getChild("experiences").getChildren().stream().map(child -> child.getName()).collect(Collectors.toList()))
                    .containsExactlyInAnyOrder("expPage1.html");

        }
        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "EXPpage1");
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem result =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(result.getChildren().stream().map(child -> child.getName()).collect(Collectors.toList()))
                    .containsExactlyInAnyOrder("experiences");

            assertThat(result.getChild("experiences").getChildren().stream().map(child -> child.getName()).collect(Collectors.toList()))
                    .containsExactlyInAnyOrder("expPage1.html");

        }
    }

    @Test
    public void assert_site_map_search_minimum_length_3chars() throws Exception {
        HstRequestContext ctx = initContext(session);

        final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "ex");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertThat(((ResponseRepresentation) response.getEntity()).getErrorCode())
                .isEqualTo(ClientError.INVALID_FILTER_QUERY.toString());
    }

    private final static JSONComparator JSON_COMPARATOR =
            new CustomComparator(JSONCompareMode.STRICT);


    @Test
    public void assert_json_expectation_nested_XPage_documents() throws Exception {

        final Node previewSitemap = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap");

        // clean all live sitemap entries
        for (Node siteMapItemNode : new NodeIterable(session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap").getNodes())) {
            siteMapItemNode.remove();
        }

        // to keep simple, only keep 'home'
        for (Node child : new NodeIterable(previewSitemap.getNodes())) {
            if (child.getName().equals("home")) {
                continue;
            }
            child.remove();
        }
        session.save();

        {
            HstRequestContext ctx = initContext(session);
            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest());

            final ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
            final String actual = objectWriter.writeValueAsString(((ResponseRepresentation) response.getEntity()).getData());

            String expected = IOUtils.toString(SiteMapTest.class.getResourceAsStream("home_as_not_xpage.json"), StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, actual, JSON_COMPARATOR);
        }


        previewSitemap.getNode("home").setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, "experiences/expPage1");
        session.save();

        {
            HstRequestContext ctx = initContext(session);
            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest());

            final String actual = objectWriter.writeValueAsString(((ResponseRepresentation) response.getEntity()).getData());

            String expected = IOUtils.toString(SiteMapTest.class.getResourceAsStream("home_as_xpage.json"), StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, actual, JSON_COMPARATOR);
        }

        // with ancestry=true same result expected
        {
            HstRequestContext ctx = initContext(session);
            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), EMPTY, true);

            final String actual = objectWriter.writeValueAsString(((ResponseRepresentation) response.getEntity()).getData());

            String expected = IOUtils.toString(SiteMapTest.class.getResourceAsStream("home_as_xpage.json"), StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, actual, JSON_COMPARATOR);
        }

        previewSitemap.getNode("home").getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH).remove();

        final Node sitemapItemExp1 = previewSitemap.addNode("exp1", HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
        sitemapItemExp1.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, "experiences/expPage1");

        session.save();

        {
            HstRequestContext ctx = initContext(session);
            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "exp1", true);

            final String actual = objectWriter.writeValueAsString(((ResponseRepresentation) response.getEntity()).getData());

            String expected = IOUtils.toString(SiteMapTest.class.getResourceAsStream("explicit_sitemap_item_for_xpage.json"), StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, actual, JSON_COMPARATOR);
        }

        // nested explicit sitemap item pointing to xpage docs

        final Node sitemapItemExp2 = sitemapItemExp1.addNode("exp2", HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
        sitemapItemExp2.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, "experiences/expPage2");
        session.save();

        {
            HstRequestContext ctx = initContext(session);
            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "exp1", true);

            final String actual = objectWriter.writeValueAsString(((ResponseRepresentation) response.getEntity()).getData());

            String expected = IOUtils.toString(SiteMapTest.class.getResourceAsStream("nested_explicit_sitemap_item_for_xpage.json"), StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, actual, JSON_COMPARATOR);
        }

    }

    /**
     * Specific sitemap setup which has a sitemap item mapping to an XPage doc, and a child siteMap item to another
     * XPage doc. This resulted for search hits in the child sitemap doc in many subtle complexities (and shitty code)
     * to deal with. Hence this JSON assertion test to monitor and see when the code breaks somehow these expectations
     * in the future
     *
     * @throws Exception
     */
    @Test
    public void assert_json_expectation_nested_XPage_documents_with_search() throws Exception {

        {
            HstRequestContext ctx = initContext(session);
            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "ExpPage1");

            SiteMapTreeItem result =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();


            final String actual = objectWriter.writeValueAsString(result);

            String expected = IOUtils.toString(SiteMapTest.class.getResourceAsStream("default_yaml_fixture_search_expPage1.json"), StandardCharsets.UTF_8);

            JSONAssert.assertEquals(expected, actual, JSON_COMPARATOR);
        }

        // clean all live sitemap entries
        for (Node siteMapItemNode : new NodeIterable(session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap").getNodes())) {
            siteMapItemNode.remove();
        }

        final Node previewSitemap = session.getNode("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap");

        // to keep simple, only keep 'home'
        for (Node child : new NodeIterable(previewSitemap.getNodes())) {
            if (child.getName().equals("home")) {
                continue;
            }
            child.remove();
        }
        session.save();

        {
            HstRequestContext ctx = initContext(session);
            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "ExpPage1");

            final String actual = objectWriter.writeValueAsString(((ResponseRepresentation) response.getEntity()).getData());

            String expected = IOUtils.toString(SiteMapTest.class.getResourceAsStream("home_as_not_xpage.json"), StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, actual, JSON_COMPARATOR);
        }

        previewSitemap.getNode("home").setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, "experiences/expPage1");
        session.save();

        {
            HstRequestContext ctx = initContext(session);
            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest());
            final String actual = objectWriter.writeValueAsString(((ResponseRepresentation) response.getEntity()).getData());

        }

        {
            HstRequestContext ctx = initContext(session);
            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "expPage1");

            final String actual = objectWriter.writeValueAsString(((ResponseRepresentation) response.getEntity()).getData());

            String expected = IOUtils.toString(SiteMapTest.class.getResourceAsStream("home_as_xpage.json"), StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, actual, JSON_COMPARATOR);
        }


        {
            HstRequestContext ctx = initContext(session);
            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "expPage1");

            final String actual = objectWriter.writeValueAsString(((ResponseRepresentation) response.getEntity()).getData());

            String expected = IOUtils.toString(SiteMapTest.class.getResourceAsStream("home_as_xpage.json"), StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, actual, JSON_COMPARATOR);
        }

        previewSitemap.getNode("home").getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH).remove();
        final Node sitemapItemExp1 = previewSitemap.addNode("exp1", HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
        sitemapItemExp1.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, "experiences/expPage1");

        session.save();

        {
            HstRequestContext ctx = initContext(session);
            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "exp");

            final String actual = objectWriter.writeValueAsString(((ResponseRepresentation) response.getEntity()).getData());
            String expected = IOUtils.toString(SiteMapTest.class.getResourceAsStream("explicit_sitemap_item_for_xpage.json"), StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, actual, JSON_COMPARATOR);
        }

        // nested explicit sitemap item pointing to xpage docs

        final Node sitemapItemExp2 = sitemapItemExp1.addNode("exp2", HstNodeTypes.NODETYPE_HST_SITEMAPITEM);
        sitemapItemExp2.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH, "experiences/expPage2");
        session.save();

        {
            HstRequestContext ctx = initContext(session);
            // test searching on exp1 which has a child xpage doc exp2
            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "exp1");

            final String actual = objectWriter.writeValueAsString(((ResponseRepresentation) response.getEntity()).getData());

            String expected = IOUtils.toString(SiteMapTest.class.getResourceAsStream("explicit_sitemap_item_for_xpage_expandable.json"), StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, actual, JSON_COMPARATOR);
        }

        {
            HstRequestContext ctx = initContext(session);
            // hit for exp1 and exp2
            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "exp");

            final String actual = objectWriter.writeValueAsString(((ResponseRepresentation) response.getEntity()).getData());

            String expected = IOUtils.toString(SiteMapTest.class.getResourceAsStream("nested_explicit_sitemap_item_for_xpage.json"), StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, actual, JSON_COMPARATOR);
        }

        {
            HstRequestContext ctx = initContext(session);
            final Response response = siteMapResource.filterSiteMap(ctx.getServletRequest(), "exp2");

            final String actual = objectWriter.writeValueAsString(((ResponseRepresentation) response.getEntity()).getData());
            String expected = IOUtils.toString(SiteMapTest.class.getResourceAsStream("nested_explicit_sitemap_item_for_xpage.json"), StandardCharsets.UTF_8);
            JSONAssert.assertEquals(expected, actual, JSON_COMPARATOR);
        }


        final Node xPageHandle = session.getNode("/unittestcontent/documents/unittestproject/experiences/expPage1");
        final DocumentWorkflow documentWorkflow = (DocumentWorkflow) ((HippoWorkspace) session.getWorkspace()).getWorkflowManager().getWorkflow("default", xPageHandle);

    }

    /**
     * <p>
     *     With {@code ancestry = true} we expect the ancestors to be also present in the response
     * </p>
     */
    @Test
    public void ancestry_tree_item_some_expected_routes_and_xpages_to_be_present() throws Exception {

        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), EMPTY, true);
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem root =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(root.getChild("/"))
                    .as("Expected the home page from sitemap routes to have been merged with XPage docs root")
                    .isNull();

            final SiteMapTreeItem news = root.getChild("news");
            assertThat(news)
                    .as("Expected news")
                    .isNotNull();
            assertThat(news.isExpandable())
                    .as("Expected news to be an expandable folder")
                    .isTrue();
            assertThat(news.getChildren().size())
                    .as("Expected news children NOT yet loaded")
                    .isEqualTo(0);

            final SiteMapTreeItem aboutUs = root.getChild("about-us");
            assertThat(aboutUs)
                    .as("Expected about-us from HST sitemap routes")
                    .isNotNull();
            assertThat(aboutUs.isExpandable())
                    .as("Expected about-us from HST sitemap route to NOT be an expandable folder")
                    .isFalse();

            final SiteMapTreeItem experiences = root.getChild("experiences");
            assertThat(experiences)
                    .as("Expected experiences from XPage documents")
                    .isNotNull();

            assertThat(experiences.isExpandable())
                    .as("Expected experiences to be an expandable folder")
                    .isTrue();
            assertThat(experiences.getChildren().size())
                    .as("Expected news children NOT yet loaded")
                    .isEqualTo(0);
        }

        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "/news", true);
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // since we request ANCESTRY, we do expect 'news' to have its children as well loaded but still have the
            // root returned
            SiteMapTreeItem root =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(root.getChild("/"))
                    .as("Expected the home page from sitemap routes to have been merged with XPage docs root")
                    .isNull();

            final SiteMapTreeItem news = root.getChild("news");
            assertThat(news)
                    .as("Expected news")
                    .isNotNull();
            assertThat(news.isExpandable())
                    .as("Expected news to be an expandable folder")
                    .isTrue();

            assertThat(news.getChildren().size())
                    .as("Expected news children to HAVE BEEN loaded")
                    .isGreaterThan(0);

            final SiteMapTreeItem news2009 = news.getChild("2009");
            assertThat(news2009.isExpandable())
                    .as("Expected news/2009 to be an expandable folder")
                    .isTrue();
            assertThat(news2009.getChildren().size())
                    .as("Expected 2009 children to NOT HAVE BEEN loaded")
                    .isEqualTo(0);

            final SiteMapTreeItem aboutUs = root.getChild("about-us");
            assertThat(aboutUs)
                    .as("Expected about-us from HST sitemap routes")
                    .isNotNull();
            assertThat(aboutUs.isExpandable())
                    .as("Expected about-us from HST sitemap route to NOT be an expandable folder")
                    .isFalse();

            final SiteMapTreeItem experiences = root.getChild("experiences");
            assertThat(experiences)
                    .as("Expected experiences from XPage documents")
                    .isNotNull();

            assertThat(experiences.isExpandable())
                    .as("Expected experiences to be an expandable folder")
                    .isTrue();
            assertThat(experiences.getChildren().size())
                    .as("Expected experiences children to NOT have been loaded")
                    .isEqualTo(0);
        }

        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "/news/2009", true);
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // since we request ANCESTRY, we do expect 'news' to have its children as well loaded but still have the
            // root returned
            SiteMapTreeItem root =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(root.getChild("/"))
                    .as("Expected the home page from sitemap routes to have been merged with XPage docs root")
                    .isNull();

            final SiteMapTreeItem news = root.getChild("news");
            assertThat(news)
                    .as("Expected news")
                    .isNotNull();
            assertThat(news.isExpandable())
                    .as("Expected news to be an expandable folder")
                    .isTrue();

            assertThat(news.getChildren().size())
                    .as("Expected news children to HAVE BEEN loaded")
                    .isGreaterThan(0);

            final SiteMapTreeItem news2009 = news.getChild("2009");
            assertThat(news2009.isExpandable())
                    .as("Expected news/2009 to be an expandable folder")
                    .isTrue();
            assertThat(news2009.getChildren().size())
                    .as("Expected news/2009 children to HAVE BEEN loaded")
                    .isGreaterThan(0);

            final SiteMapTreeItem news2009May = news2009.getChild("May");
            assertThat(news2009May.isExpandable())
                    .as("Expected news/2009/May to be an expandable folder")
                    .isTrue();
            assertThat(news2009May.getChildren().size())
                    .as("Expected news/2009/May children to HAVE BEEN loaded")
                    .isEqualTo(0);

            final SiteMapTreeItem aboutUs = root.getChild("about-us");
            assertThat(aboutUs)
                    .as("Expected about-us from HST sitemap routes")
                    .isNotNull();
            assertThat(aboutUs.isExpandable())
                    .as("Expected about-us from HST sitemap route to NOT be an expandable folder")
                    .isFalse();

            final SiteMapTreeItem experiences = root.getChild("experiences");
            assertThat(experiences)
                    .as("Expected experiences from XPage documents")
                    .isNotNull();

            assertThat(experiences.isExpandable())
                    .as("Expected experiences to be an expandable folder")
                    .isTrue();
            assertThat(experiences.getChildren().size())
                    .as("Expected experiences children to NOT have been loaded")
                    .isEqualTo(0);
        }

        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "/experiences", true);
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // since we request ANCESTRY, we do expect 'experiences' to have its children as well loaded but still have the
            // root returned
            SiteMapTreeItem root =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            assertThat(root.getChild("/"))
                    .as("Expected the home page from sitemap routes to have been merged with XPage docs root")
                    .isNull();

            final SiteMapTreeItem experiences = root.getChild("experiences");
            assertThat(experiences)
                    .as("Expected experiences from XPage documents")
                    .isNotNull();

            assertThat(experiences.isExpandable())
                    .as("Expected experiences to be an expandable folder")
                    .isTrue();
            assertThat(experiences.getChildren().size())
                    .as("Expected experiences children to HAVE been loaded")
                    .isGreaterThan(0);
        }

        // fetch sitemap item for an XPage item
        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "experiences/expPage1.html", true);

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem root =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            final SiteMapTreeItem expPage1 = root.getChild("experiences").getChild("expPage1.html");
            assertThat(expPage1)
                    .as("Expected experiences from XPage documents")
                    .isNotNull();
            assertThat(expPage1.isExpandable())
                    .isFalse();
            assertThat(expPage1.isExperiencePage())
                    .isTrue();

        }

        // Test a not found
        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "some-non-existing-path", true);

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        }
        // Test a not found
        {
            HstRequestContext ctx = initContext(session);

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "/news/some-non-existing-path", true);

            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        }
    }

}
