/*
 * Copyright 2022 Bloomreach
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.sitemap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.jcr.Node;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.CXFJaxrsHstConfigService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.sitemapresource.AbstractSiteMapResourceTest;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.hst.pagecomposer.sitemap.XPageSiteMapRepresentationService;
import org.hippoecm.hst.pagecomposer.sitemap.XPageSiteMapShallowItem;
import org.hippoecm.hst.pagecomposer.sitemap.XPageSiteMapTreeItem;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_SITEMAPITEM;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID;

public class XPageSiteMapRepresentationServiceTest extends AbstractSiteMapResourceTest {

    private XPageSiteMapRepresentationService xPageSiteMapRepresentationService;
    private PageComposerContextService pageComposerContextService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        xPageSiteMapRepresentationService = HstServices.getComponentManager().getComponent(XPageSiteMapRepresentationService.class.getName(), "org.hippoecm.hst.pagecomposer");

    }

    private void initContext() throws Exception {
        initContext(null);
    }

    private void initContext(final String subsite) throws Exception {
        final HstRequestContext ctx = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", (subsite == null ? "" : subsite + "/") + "home");
        pageComposerContextService = mountResource.getPageComposerContextService();
        final HstSite site = pageComposerContextService.getEditingPreviewSite();

        // override the config identifier to have sitemap id
        ctx.setAttribute(CXFJaxrsHstConfigService.REQUEST_CONFIG_NODE_IDENTIFIER, ((CanonicalInfo) site.getSiteMap()).getCanonicalIdentifier());
    }

    /**
     * <p>
     * As per branch of a channel all XPage docs are kept in a URL space in-memory model, lots of same strings are used.
     * As the number of XPage docs can be large, we intern the String such that memory usage is lowered. This is only
     * needed for the shared in memory model which are backed by {@link XPageSiteMapTreeItem}. It is not needed for the
     * request based {@link XPageSiteMapShallowItem}
     * </p>
     */
    @Test
    public void assert_string_instances_shared_for_in_memory_model() {

        final XPageSiteMapTreeItem item1 = new XPageSiteMapTreeItem();
        final XPageSiteMapTreeItem item2 = new XPageSiteMapTreeItem();

        item1.setPathInfo("foo");
        item1.setAbsoluteJcrPath("bar");
        item2.setPathInfo(new StringBuilder("foo").toString());
        item2.setAbsoluteJcrPath(new StringBuilder("bar").toString());

        assertThat(item1.getPathInfo() == item2.getPathInfo())
                .isTrue();
        assertThat(item1.getAbsoluteJcrPath() == item2.getAbsoluteJcrPath())
                .isTrue();


        final XPageSiteMapShallowItem item3 = new XPageSiteMapShallowItem(item1);
        final XPageSiteMapShallowItem item4 = new XPageSiteMapShallowItem(item2);

        // when using item1 and item2 as source, the string instances are reused
        assertThat(item3.getPathInfo() == item4.getPathInfo())
                .isTrue();
        assertThat(item3.getAbsoluteJcrPath() == item4.getAbsoluteJcrPath())
                .isTrue();

        // item5 is created with a new "foo" string instance: as XPageSiteMapShallowItem does not require efficient
        // memory storing, the pathInfo for "foo" won't be interned hence a unique instance is expected
        final XPageSiteMapShallowItem item5 = new XPageSiteMapShallowItem(new StringBuilder("foo").toString());

        assertThat(item5.getPathInfo() == item4.getPathInfo())
                .as("As XPageSiteMapShallowItem does not intern String, we expect unique String instance")
                .isFalse();
    }

    @Test
    public void assert_caching() throws Exception {

        initContext();
        final XPageSiteMapTreeItem siteMapTree = getSiteMapTree();

        initContext();
        // assert caching works
        assertThat(siteMapTree)
                .as("Expected cached response")
                .isSameAs(getSiteMapTree());

    }


    @Test
    public void assert_complete() throws Exception {

        initContext();
        final XPageSiteMapTreeItem root = getSiteMapTree();

        final HstLinkCreator hstLinkCreator = HstConfigurationUtils.getPreviewHstModel().getHstLinkCreator();

        final QueryResult xPageDocuments = XPageSiteMapRepresentationService.getUnpublishedXPageDocVariants(pageComposerContextService.getEditingMount(), session);

        List<XPageSiteMapTreeItem> hits = new ArrayList<>();

        for (Node xpage : new NodeIterable(xPageDocuments.getNodes())) {
            final Node handle = xpage.getParent();
            final HstLink hstLink = hstLinkCreator.create(handle, pageComposerContextService.getEditingMount());
            if (hstLink.isNotFound()) {
                continue;
            }
            final String[] pathElements = hstLink.getPath().split("/");

            XPageSiteMapTreeItem current = root;

            for (int i = 0; i < pathElements.length; i++) {
                assertThat(current.getChildren().containsKey(pathElements[i]))
                        .as("Expected child '%s'", pathElements)
                        .isTrue();
                current = current.getChildren().get(pathElements[i]);
                if (i == pathElements.length - 1) {
                    hits.add(current);
                    assertThat(current.getAbsoluteJcrPath())
                            .as("Expected jcr path to be present and correct value")
                            .isEqualTo(handle.getPath());

                    assertThat(current.getPathInfo())
                            .as("Expected path info to be present and correct value")
                            .isEqualTo(hstLink.getPath());
                }

            }

        }

        // now assert that all items with a pathInfo (the non-structural ones as there can be items in the tree
        // which are only for hierarchy but do not have a pathInfo) are found

        assertAllHit(root, hits);

    }

    private void assertAllHit(final XPageSiteMapTreeItem current, final List<XPageSiteMapTreeItem> hits) {
        if (current.getAbsoluteJcrPath() != null) {
            assertThat(hits.contains(current))
                    .as("Expected all items to be present in hits")
                    .isTrue();
        }
        current.getChildren().values().stream().forEach(child -> assertAllHit(child, hits));
    }

    @Test
    public void assert_invalidation_on_site_map_change() throws Exception {

        initContext();
        XPageSiteMapTreeItem siteMapTree = getSiteMapTree();

        // assert without change same instance returned
        initContext();
        assertThat(siteMapTree)
                .as("Expected cached response")
                .isSameAs(getSiteMapTree());

        assertThat(siteMapTree.getChildren().containsKey("experiences"))
                .isTrue();

        // move hst:sitemap item of channel but ***NOT*** within workspace : only the workspace is copied for SAAS to preview
        {
            try {
                session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap/experiences", "/hst:hst/hst:configurations/unittestproject/hst:sitemap/experiencesmoved");
                session.save();

                initContext();
                final XPageSiteMapTreeItem updated = getSiteMapTree();
                assertThat(updated)
                        .as("Since the sitemap got changed, we expected a new instance of XPageSiteMapTreeItem")
                        .isNotSameAs(siteMapTree);

                assertThat(updated.getChildren().containsKey("experiences"))
                        .isFalse();
                assertThat(updated.getChildren().containsKey("experiencesmoved"))
                        .isTrue();

            } finally {
                session.move("/hst:hst/hst:configurations/unittestproject/hst:sitemap/experiencesmoved",
                        "/hst:hst/hst:configurations/unittestproject/hst:sitemap/experiences");
                session.save();
            }
        }

        initContext();
        siteMapTree = getSiteMapTree();
        // move hst:sitemap item of channel within LIVE workspace : As this is a workspace item, and it does not get changed
        // in PREVIEW config, it is not taken into account for the PREVIEW and as such seen as 'NO CHANGE'
        {
            assertThat(siteMapTree.getChildren().containsKey("news"))
                    .isTrue();
            try {
                // the live workspace, not the preview!
                session.move("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/news",
                        "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/newsmoved");
                session.save();

                initContext();
                final XPageSiteMapTreeItem updated = getSiteMapTree();
                assertThat(updated)
                        .as("Since the LIVE workspace sitemap got changed, " +
                                "we expected the SAME instance of XPageSiteMapTreeItem for preview")
                        .isSameAs(siteMapTree);

                assertThat(updated.getChildren().containsKey("news"))
                        .isTrue();
                assertThat(updated.getChildren().containsKey("newsmoved"))
                        .isFalse();

            } finally {
                session.move("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/newsmoved",
                        "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap/news");
                session.save();
            }
        }


        initContext("subsite");
        XPageSiteMapTreeItem subSiteSiteMapTree = getSiteMapTree();

        initContext();
        siteMapTree = getSiteMapTree();
        // move hst:sitemap item of channel within PREVIEW workspace : As this is a workspace item, and it DOES get changed
        // in PREVIEW config, it IS taken into account for the PREVIEW and as such seen as 'CHANGE'
        {
            assertThat(siteMapTree.getChildren().containsKey("news"))
                    .isTrue();
            try {
                // the PREVIEW workspace
                session.move("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/news",
                        "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/newsmoved");
                session.save();

                initContext();
                final XPageSiteMapTreeItem updated = getSiteMapTree();
                assertThat(updated)
                        .as("Since the PREVIEW workspace sitemap got changed, we expected a new instance of XPageSiteMapTreeItem")
                        .isNotSameAs(siteMapTree);

                assertThat(updated.getChildren().containsKey("news"))
                        .isFalse();
                assertThat(updated.getChildren().containsKey("newsmoved"))
                        .isTrue();

                initContext("subsite");
                final XPageSiteMapTreeItem subSiteUpdated = getSiteMapTree();
                assertThat(subSiteUpdated)
                        .as("Since the sitemap for subsite stayed the same, no new instance is expected")
                        .isSameAs(subSiteSiteMapTree);

            } finally {
                session.move("/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/newsmoved",
                        "/hst:hst/hst:configurations/unittestproject-preview/hst:workspace/hst:sitemap/news");
                session.save();
            }
        }

        // add a sitemap item in hst:default : this should trigger a reload as well! Also for subsites as they all use the
        // same 'hst:default' sitemap

        initContext("subsite");
        subSiteSiteMapTree = getSiteMapTree();

        initContext();
        siteMapTree = getSiteMapTree();
        {
            try {
                final Node defaulttest = session.getNode("/hst:hst/hst:configurations/hst:default/hst:sitemap").addNode("defaulttest", NODETYPE_HST_SITEMAPITEM);
                defaulttest.setProperty(SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID, "dummy");
                session.save();

                initContext();
                final XPageSiteMapTreeItem updated = getSiteMapTree();
                assertThat(updated)
                        .as("Since the default sitemap got changed, we expected a new instance of XPageSiteMapTreeItem")
                        .isNotSameAs(siteMapTree);

                initContext("subsite");
                final XPageSiteMapTreeItem subSiteUpdated = getSiteMapTree();
                assertThat(subSiteUpdated)
                        .as("Since the sitemap got changed, we expected a new instance of XPageSiteMapTreeItem, " +
                                "also for subsite")
                        .isNotSameAs(subSiteSiteMapTree);
            } finally {
                session.getNode("/hst:hst/hst:configurations/hst:default/hst:sitemap/defaulttest").remove();
                session.save();
            }
        }

    }

    @Test
    public void assert_invalidation_on_experience_handle_add_delete_move() throws Exception {

        initContext("subsite");
        XPageSiteMapTreeItem subSiteSiteMapTree = getSiteMapTree();

        initContext();
        XPageSiteMapTreeItem siteMapTree = getSiteMapTree();

        // as we do assertions in finally block but we do not want failures in these assertions to trump earlier
        // assertion failures, we catch the exception in finally block and only throw it after the finally block
        Throwable failureInFinally = null;

        // add a NON experience page document from the 'unittestproject' project : assert 'siteMapTree' instance
        // does not change
        final Node copy = JcrUtils.copy(session, "/unittestcontent/documents/unittestproject/News/2009/April/Day5/Day5Article",
                "/unittestcontent/documents/unittestproject/News/2009/April/Day5/Day6Article");
        for (Node variant : new NodeIterable(copy.getNodes())) {
            session.move(variant.getPath(), variant.getParent().getPath() + "/Day6Article");
        }

        try {

            // NON XPage document
            session.save();
            // give jcr events some time
            Thread.sleep(300);

            initContext();
            final XPageSiteMapTreeItem again = getSiteMapTree();
            assertThat(again)
                    .as("Since not an XPage document changed, the expected XPageSiteMapTreeItem is the same")
                    .isSameAs(siteMapTree);
        } finally {
            try {
                session.refresh(false);
                copy.remove();
                session.save();

                // give jcr events some time
                Thread.sleep(300);
                initContext();
                final XPageSiteMapTreeItem again = getSiteMapTree();
                assertThat(again)
                        .as("Since NOT an XPage document removed, the expected XPageSiteMapTreeItem is the same")
                        .isSameAs(siteMapTree);
            } catch (Exception e) {
                failureInFinally = e;
            }
        }

        if (failureInFinally != null) {
            throw new RuntimeException(failureInFinally);
        }

        initContext();
        siteMapTree = getSiteMapTree();

        // Add an experience page document from the 'unittestproject' project

        final Node newXpage = JcrUtils.copy(session, "/unittestcontent/documents/unittestproject/experiences/expPage1",
                "/unittestcontent/documents/unittestproject/experiences/newExpPage1");

        for (Node variant : new NodeIterable(newXpage.getNodes())) {
            session.move(variant.getPath(), variant.getParent().getPath() + "/newExpPage1");
        }
        try {

            // NON XPage document
            session.save();
            // give jcr events some time
            Thread.sleep(300);

            initContext();
            final XPageSiteMapTreeItem updated = getSiteMapTree();
            assertThat(updated)
                    .as("Since an XPage document got added, the expected XPageSiteMapTreeItem is NOT the same")
                    .isNotSameAs(siteMapTree);

            assertThat(updated.getChildren().get("experiences").getChildren().containsKey("newExpPage1.html"))
                    .isTrue();

            initContext("subsite");
            final XPageSiteMapTreeItem updatedSubSite = getSiteMapTree();
            assertThat(updatedSubSite)
                    .as("Since an XPage document got added to other channel, the subsite SiteMapTree instance is " +
                            "expected to stay the same")
                    .isSameAs(subSiteSiteMapTree);

            initContext();
            siteMapTree = getSiteMapTree();

        } finally {
            try {
                session.refresh(false);
                newXpage.remove();
                session.save();

                // give jcr events some time
                Thread.sleep(300);
                initContext();
                final XPageSiteMapTreeItem again = getSiteMapTree();
                assertThat(again)
                        .as("Since an XPage document got removed, the XPageSiteMapTreeItem should have been updated!")
                        .isNotSameAs(siteMapTree);

                assertThat(again.getChildren().get("experiences").getChildren().containsKey("newExpPage1.html"))
                        .isFalse();
            } catch (Exception e) {
                failureInFinally = e;
            }
        }

        if (failureInFinally != null) {
            throw new RuntimeException(failureInFinally);
        }

        initContext();
        siteMapTree = getSiteMapTree();

        // move an experience page document WITHIN the 'unittestproject' project
        session.move("/unittestcontent/documents/unittestproject/experiences/expPage1",
                "/unittestcontent/documents/unittestproject/experiences/expPage1Moved");
        final Node xPageMoved = session.getNode("/unittestcontent/documents/unittestproject/experiences/expPage1Moved");

        for (Node variant : new NodeIterable(xPageMoved.getNodes())) {
            session.move(variant.getPath(), variant.getParent().getPath() + "/expPage1Moved");
        }

        try {
            session.save();
            // give jcr events some time
            Thread.sleep(300);

            initContext();
            final XPageSiteMapTreeItem updated = getSiteMapTree();
            assertThat(updated)
                    .as("Since an XPage document got MOVED, the expected XPageSiteMapTreeItem is NOT the same")
                    .isNotSameAs(siteMapTree);

            assertThat(updated.getChildren().get("experiences").getChildren().containsKey("expPage1.html"))
                    .isFalse();
            assertThat(updated.getChildren().get("experiences").getChildren().containsKey("expPage1Moved.html"))
                    .isTrue();

            initContext();
            siteMapTree = getSiteMapTree();

        } finally {
            try {
                session.refresh(false);
                session.move("/unittestcontent/documents/unittestproject/experiences/expPage1Moved",
                        "/unittestcontent/documents/unittestproject/experiences/expPage1");
                final Node xPageRevertedMoved = session.getNode("/unittestcontent/documents/unittestproject/experiences/expPage1");

                for (Node variant : new NodeIterable(xPageRevertedMoved.getNodes())) {
                    session.move(variant.getPath(), variant.getParent().getPath() + "/expPage1");
                }
                session.save();

                // give jcr events some time
                Thread.sleep(300);
                initContext();
                final XPageSiteMapTreeItem again = getSiteMapTree();
                assertThat(again)
                        .as("Since an XPage document got MOVED, the XPageSiteMapTreeItem should have been updated!")
                        .isNotSameAs(siteMapTree);

                assertThat(again.getChildren().get("experiences").getChildren().containsKey("expPage1.html"))
                        .isTrue();
                assertThat(again.getChildren().get("experiences").getChildren().containsKey("expPage1Moved.html"))
                        .isFalse();

                initContext("subsite");
                final XPageSiteMapTreeItem updatedSubSite = getSiteMapTree();
                assertThat(updatedSubSite)
                        .as("Since an XPage document got MOVED within another channel, the subsite SiteMapTree instance is " +
                                "expected to stay the same")
                        .isSameAs(subSiteSiteMapTree);

            } catch (Exception e) {
                failureInFinally = e;
            }
        }

        if (failureInFinally != null) {
            throw new RuntimeException(failureInFinally);
        }


        initContext();
        siteMapTree = getSiteMapTree();

        initContext("subsite");
        subSiteSiteMapTree = getSiteMapTree();

        // move an experience page document ACROSS the 'unittestproject' project to unittestsubproject
        session.move("/unittestcontent/documents/unittestproject/News/2009/May/articleAsExpPage",
                "/unittestcontent/documents/unittestsubproject/News/2008/articleAsExpPage");

        try {
            session.save();
            // give jcr events some time
            Thread.sleep(300);

            initContext();
            final XPageSiteMapTreeItem updated = getSiteMapTree();
            assertThat(updated)
                    .as("Since an XPage document got MOVED, the expected XPageSiteMapTreeItem is NOT the same")
                    .isNotSameAs(siteMapTree);

            assertThat(siteMapTree.getChildren().containsKey("news"))
                    .as("before the move, 'news' should be part of the XPage Tree")
                    .isTrue();
            assertThat(updated.getChildren().containsKey("news"))
                    .as("after the move, 'news' shouldn't be part of the XPage Tree")
                    .isFalse();

            initContext("subsite");
            final XPageSiteMapTreeItem updatedSubSiteMapTree = getSiteMapTree();
            assertThat(updatedSubSiteMapTree)
                    .as("Since an XPage document got MOVED to the subsite, the expected XPageSiteMapTreeItem is NOT the same")
                    .isNotSameAs(subSiteSiteMapTree);

            assertThat(updatedSubSiteMapTree.getChildren().containsKey("news"))
                    .as("after the move, 'news' XPage should be part of the subsite SiteMap Tree")
                    .isTrue();

        } finally {
            try {
                session.refresh(false);
                session.move("/unittestcontent/documents/unittestsubproject/News/2008/articleAsExpPage",
                        "/unittestcontent/documents/unittestproject/News/2009/May/articleAsExpPage");

                session.save();

                // give jcr events some time
                Thread.sleep(300);
                initContext();
                final XPageSiteMapTreeItem again = getSiteMapTree();
                assertThat(again)
                        .as("Since an XPage document got MOVED, the XPageSiteMapTreeItem should have been updated!")
                        .isNotSameAs(siteMapTree);

                assertThat(again.getChildren().containsKey("news"))
                        .as("after the move back, 'news' should be part of the XPage Tree")
                        .isTrue();

                initContext("subsite");
                final XPageSiteMapTreeItem updatedSubSite = getSiteMapTree();
                assertThat(updatedSubSite)
                        .as("Since an XPage document got MOVED to another channel, the subsite SiteMapTree instance is " +
                                "expected to be updated")
                        .isNotSameAs(subSiteSiteMapTree);

                assertThat(updatedSubSite.getChildren().containsKey("news"))
                        .as("after the move back, 'news' XPage should NOT be part of the subsite SiteMap Tree")
                        .isFalse();

            } catch (Throwable e) {
                failureInFinally = e;
            }
        }

        if (failureInFinally != null) {
            throw new RuntimeException(failureInFinally);
        }

        // adding an (xpage) folder should not result in a reload of the xpage sitemap

        initContext();
        siteMapTree = getSiteMapTree();

        // move an experience page document ACROSS the 'unittestproject' project to unittestsubproject
        JcrUtils.copy(session, "/unittestcontent/documents/unittestproject/experiences/experiences-subfolder",
                "/unittestcontent/documents/unittestproject/experiences/experiences-subfolder-copy");

        try {
            session.save();
            // give jcr events some time
            Thread.sleep(300);

            initContext();
            final XPageSiteMapTreeItem updated = getSiteMapTree();
            assertThat(updated)
                    .as("Since an XPage document got MOVED, the expected XPageSiteMapTreeItem is NOT the same")
                    .isSameAs(siteMapTree);
        } finally {
            try {
                session.refresh(false);
                session.getNode("/unittestcontent/documents/unittestproject/experiences/experiences-subfolder-copy").remove();

                session.save();

                // give jcr events some time
                Thread.sleep(300);
                initContext();
                final XPageSiteMapTreeItem again = getSiteMapTree();
                assertThat(again)
                        .as("Since an XPage document got MOVED, the XPageSiteMapTreeItem should have been updated!")
                        .isSameAs(siteMapTree);

            } catch (Throwable e) {
                failureInFinally = e;
            }
        }

        if (failureInFinally != null) {
            throw new RuntimeException(failureInFinally);
        }

    }

    @Test
    public void assert_concurrent_access_all_result_in_same_instance() throws Exception {

        final int nrJobs = 1000;
        Collection<SiteMapTreeFetcher> jobs = new ArrayList<>(nrJobs);
        for (int i = 0; i < nrJobs; i++) {
            jobs.add(new SiteMapTreeFetcher());
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(50);
        final List<Future<XPageSiteMapTreeItem>> futures = executorService.invokeAll(jobs);

        final XPageSiteMapTreeItem compareTo = futures.get(0).get();

        futures.stream().forEach(
                future -> {
                    try {
                        assertThat(future.get())
                                .as("Expected same instance")
                                .isSameAs(compareTo);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );

    }


    private XPageSiteMapTreeItem getSiteMapTree() {
        return xPageSiteMapRepresentationService.getSiteMapTree(pageComposerContextService.getEditingMount(),
                HstConfigurationUtils.getPreviewHstModel());
    }


    private class SiteMapTreeFetcher implements Callable<XPageSiteMapTreeItem> {

        @Override
        public XPageSiteMapTreeItem call() throws Exception {
            initContext();

            return xPageSiteMapRepresentationService.getSiteMapTree(pageComposerContextService.getEditingMount(),
                    HstConfigurationUtils.getPreviewHstModel());
        }
    }
}
