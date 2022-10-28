/*
 * Copyright 2022 Bloomreach
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.sitemapresource;

import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapTreeItem;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.site.HstServices;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class SiteMapTest extends AbstractSiteMapResourceTest {


    private SiteMapResource siteMapResource;

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
    public void assert_some_expected_routes_and_xpages_to_be_present() throws Exception {

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
        }

        {
            HstRequestContext ctx = initContext(session);

            // unittestcontent/documents/unittestproject/News/2009/May/articleAsExpPage is an XPage document so expected
            // to be present in the sitemap
            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "about-us");

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

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "news");

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            SiteMapTreeItem newsSiteMapTreeItem =
                    (SiteMapTreeItem) ((ResponseRepresentation) response.getEntity()).getData();

            final SiteMapTreeItem news = newsSiteMapTreeItem.getChild("2009");
            assertThat(news)
                    .as("Expected 2009")
                    .isNotNull();
            assertThat(news.isExpandable())
                    .as("Expected 2009 to be an expandable folder")
                    .isTrue();
            assertThat(news.isExperiencePage())
                    .as("Expected 2009 to NOT be an experience page")
                    .isFalse();
        }

        {
            HstRequestContext ctx = initContext(session);

            // unittestcontent/documents/unittestproject/News/2009/May/articleAsExpPage is an XPage document so expected
            // to be present in the sitemap
            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "news/2009/May");

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

            final Response response = siteMapResource.getSiteMapShallowItem(ctx.getServletRequest(), "experiences");

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

            final SiteMapTreeItem articleAsExpPage = experiencesSiteMapTreeItem.getChild("expPage1.html");
            assertThat(articleAsExpPage)
                    .as("Expected expPage1.html")
                    .isNotNull();
            assertThat(articleAsExpPage.isExpandable())
                    .as("Expected expPage1 to be NOT an expandable folder")
                    .isFalse();
            assertThat(articleAsExpPage.isExperiencePage())
                    .as("Expected expPage1 to be an XPage")
                    .isTrue();
        }

    }

}
