/*
 *  Copyright 2020-2023 Bloomreach
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.fullrequestcycle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.servlet.ServletException;

import org.assertj.core.api.Assertions;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractFullRequestCycleTest;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.springframework.mock.web.MockHttpServletResponse;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hippoecm.hst.configuration.HstNodeTypes.INDEX;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_XPAGE_MIXIN;

public class SiteMapResourceTest extends AbstractFullRequestCycleTest {

    private HstManager manager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        manager = siteComponentManager.getComponent(HstManager.class);
        Session session = backupHstAndCreateWorkspace();
        session.save();
        session.logout();
    }

    @After
    public void tearDown() throws Exception {
        try {
            final Session session = createSession(ADMIN_CREDENTIALS);
            restoreHstConfigBackup(session);
            session.logout();
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void sitemap_pages_contain_hst_config_sitemap_items_and_experience_pages_for_channel() throws Exception {

        assertAs(ADMIN_CREDENTIALS);
        assertAs(EDITOR_CREDENTIALS);
        assertAs(AUTHOR_CREDENTIALS);

    }

    private void assertAs(final Credentials cmsUser) throws RepositoryException, IOException, ServletException, ContainerException {
        final Session admin = createSession(ADMIN_CREDENTIALS);

        try {
            final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final List<String> responsePages = getResponseSiteMapPages(admin, mountId, cmsUser,"name");

            final Mount mount = manager.getVirtualHosts().getMountByIdentifier(mountId);

            // fetch expected sitemap pages
            List<String> expectedSiteMapItemNames = getExpectedSiteMapItemNames(mount);

            // expected XPage Pages
            final List<String> expectedXPagesItemNames = collectExpectedXPageLastPathElement(admin, mount);

            assertThat(responsePages.size())
                    .as("Expected the pages in response to consist of sitemap item and XPages")
                    .isEqualTo(expectedSiteMapItemNames.size() + expectedXPagesItemNames.size());

            assertSoftly(softAssertions -> {
                expectedSiteMapItemNames.forEach(s -> softAssertions.assertThat(responsePages.contains(s))
                        .as("Expected to contain HST sitemap page '%s'", s)
                        .isTrue());
                expectedXPagesItemNames.forEach(s -> softAssertions.assertThat(responsePages.contains(s))
                        .as("Expected to contain XPage '%s'", s)
                        .isTrue());
            });
        } finally {
            admin.logout();
        }
    }

    @NotNull
    private List<String> getExpectedSiteMapItemNames(final Mount mount) {
        final List<HstSiteMapItem> siteMapItems = mount.getHstSite().getSiteMap().getSiteMapItems();
        List<String> expectedSiteMapItemNames = new ArrayList<>();

        for (HstSiteMapItem siteMapItem : siteMapItems) {
            collectExpectedSiteMapItemNames(expectedSiteMapItemNames, siteMapItem);
        }
        return expectedSiteMapItemNames;
    }

    @NotNull
    private List<String> getResponseSiteMapPages(final Session admin, final String mountId, final Credentials cmsUser,
                                                 final String field) throws RepositoryException, IOException, ServletException {
        final String siteMapId = getNodeId(admin, "/hst:hst/hst:configurations/unittestproject/hst:sitemap");


        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + siteMapId + "./pages", null, "GET");

        final MockHttpServletResponse response = render(mountId, requestResponse, cmsUser);

        final String restResponse = response.getContentAsString();

        final ResponseRepresentation o = mapper.readerFor(ResponseRepresentation.class).readValue(restResponse);

        final Map<String, Object> data = (Map<String, Object>) o.getData();
        final List<Map<String, Object>> pages = (List<Map<String, Object>>) data.get("pages");

        return pages.stream().map(page -> (String) page.get(field)).collect(Collectors.toList());
    }


    private void collectExpectedSiteMapItemNames(final List<String> populate, final HstSiteMapItem item) {

        if (item.isExplicitElement() && !INDEX.equals(item.getValue()) && !item.isContainerResource()
                && !item.isHiddenInChannelManager()) {
            populate.add(NodeNameCodec.decode(item.getValue()));
            for (HstSiteMapItem child : item.getChildren()) {
                collectExpectedSiteMapItemNames(populate, child);
            }
        }
    }

    private List<String> collectExpectedXPageLastPathElement(final Session admin, final Mount mount) throws RepositoryException {

        List<String> expectedXPageDocuments = new ArrayList<>();

        final String statement = format("//element(*,%s)[@hippo:paths = '%s' and @hippo:availability = 'preview']",
                MIXINTYPE_HST_XPAGE_MIXIN, admin.getNode(mount.getContentPath()).getIdentifier());

        final Query xPagesQuery = admin.getWorkspace().getQueryManager().createQuery(statement, "xpath");

        for (Node unpublishedVariant : new NodeIterable(xPagesQuery.execute().getNodes())) {
            expectedXPageDocuments.add(unpublishedVariant.getName() + ".html");
        }
        return expectedXPageDocuments;
    }


    /**
     * If an XPage document is already explicitly referenced by a sitemap item, it should NOT be represented TWICE in the
     * sitemap pages
     */
    @Test
    public void explicit_sitemap_page_mapping_to_xpage_document_do_not_result_in_duplicate() throws Exception {
        final Session admin = createSession(ADMIN_CREDENTIALS);

        try {

            // first make a sitemap item that explicitly points to an XPage Document : this should NOt result in a
            // duplicate BUT only in a SiteMapItem representation (which trumps the XPage Document representation...which
            // is a choice)
            String[] content = new String[] {
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap/experiences/expPage1.html", "hst:sitemapitem",
                    "hst:relativecontentpath", "experiences/expPage1"
            };

            RepositoryTestCase.build(content, admin);

            admin.save();

            final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final List<String> responsePages = getResponseSiteMapPages(admin, mountId, ADMIN_CREDENTIALS,"name");

            final Mount mount = manager.getVirtualHosts().getMountByIdentifier(mountId);

            // fetch expected sitemap pages
            List<String> expectedSiteMapItemNames = getExpectedSiteMapItemNames(mount);

            assertThat(expectedSiteMapItemNames.contains("expPage1.html"))
                    .as("Expected expPage1 now as SiteMap Item represented")
                    .isTrue();

            // expected XPage Pages
            final List<String> expectedXPagesItemNames = collectExpectedXPageLastPathElement(admin, mount);

            // remove 'expPage1' from the 'expectedXPagesItemNames' since represented by "expPage1.html" as sitemap item
            expectedXPagesItemNames.remove("expPage1.html");

            assertThat(responsePages.size())
                    .as("Expected the pages in response to consist of sitemap item and XPages but ONE LESS " +
                            "because duplicate should be filtered out!")
                    .isEqualTo(expectedSiteMapItemNames.size() + expectedXPagesItemNames.size());

            assertSoftly(softAssertions -> {
                expectedSiteMapItemNames.forEach(s -> softAssertions.assertThat(responsePages.contains(s))
                        .as("Expected to contain HST sitemap page '%s'", s)
                        .isTrue());
                expectedXPagesItemNames.forEach(s -> softAssertions.assertThat(responsePages.contains(s))
                        .as("Expected to contain XPage '%s'", s)
                        .isTrue());
            });
        } finally {
            admin.logout();
        }
    }


    /**
     * When there is an _index_ sitemap item mapping to an xpage document, instead of the name of the xpage doc
     * (typically 'index') we show the name of the parent sitemap item
     */
    @Test
    public void index_sitemap_item_below_explicit_item_mapping_to_xpage_document_results_in_name_of_parent_sitemap_item() throws Exception {
        final Session admin = createSession(ADMIN_CREDENTIALS);

        try {

            admin.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap").remove();

            String[] content = new String[] {
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap", "hst:sitemap",
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap/foo", "hst:sitemapitem",
                    "hst:relativecontentpath" , "experiences",
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap/foo/_index_", "hst:sitemapitem",
                    "hst:relativecontentpath", "${parent}/expPage1",
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap/foo/_default_", "hst:sitemapitem",
                    "hst:relativecontentpath", "${parent}/${1}"
            };

            RepositoryTestCase.build(content, admin);

            admin.save();

            final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final List<String> responsePages = getResponseSiteMapPages(admin, mountId, ADMIN_CREDENTIALS, "renderPathInfo");

            assertThat(responsePages)
                    .as("expPage1 will be represented by _index_ through explicit 'experiences' sitemap item, other xpage doc by _default_")
                    .containsExactly("/foo", "/foo/expPage-with-static-components", "/foo/expPage2");

        } finally {
            admin.logout();
        }
    }

    /**
     * When there is an _index_ sitemap item mapping to an xpage document, instead of the name of the xpage doc
     * (typically 'index') we show the name of the parent folder since this is the logical name in the sitemap
     */
    @Test
    public void index_sitemap_item_below_wildcard_item_mapping_to_xpage_document_results_in_name_of_parent_folder() throws Exception {
        final Session admin = createSession(ADMIN_CREDENTIALS);

        try {

            admin.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap").remove();

            String[] content = new String[] {
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap", "hst:sitemap",
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap/_default_", "hst:sitemapitem",
                    "hst:relativecontentpath" , "${1}",
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap/_default_/_index_", "hst:sitemapitem",
                    "hst:relativecontentpath", "${parent}/expPage1",
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap/_default_/_default_", "hst:sitemapitem",
                    "hst:relativecontentpath", "${parent}/${2}"
            };

            RepositoryTestCase.build(content, admin);

            admin.save();

            final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final List<String> responsePages = getResponseSiteMapPages(admin, mountId, ADMIN_CREDENTIALS, "renderPathInfo");

            assertThat(responsePages)
                    .as("expPage1 will be represented by _index_ through explicit 'experiences' sitemap item, other xpage doc by _default_")
                    .containsExactly("/experiences", "/experiences/expPage-with-static-components", "/experiences/expPage2");

        } finally {
            admin.logout();
        }
    }


    /**
     * See SiteMapPageRepresentation#represent(org.hippoecm.hst.core.linking.HstLink, javax.jcr.Node) : in general, the
     * XPage docs its JCR Nodename is the same as the last 'pathInfo' element, *however*, this is not the case for eg
     * a sitemap structure as follows:
     *
     * /experiences/_default_
     *    - hst:relativecontentpath = experiences/${1}/index
     *
     * Above, the matched document will have name 'index' but the URL for it is '/blog/_default_' , and thus the sitemap
     * should not show 'index' but the value for _default_
     */
    @Test
    public void path_element_is_used_in_sitemap_instead_of_document_nodename() throws Exception {
        final Session admin = createSession(ADMIN_CREDENTIALS);

        Node index = null;
        try {

            admin.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap").remove();

            // below 'experiences' bootstrapped folder there is already an empty folder 'experiences-subfolder' bootstrapped

            JcrUtils.copy(admin, "/unittestcontent/documents/unittestproject/experiences/expPage1",
                    "/unittestcontent/documents/unittestproject/experiences/experiences-subfolder/index");

            index = admin.getNode("/unittestcontent/documents/unittestproject/experiences/experiences-subfolder/index");
            for (Node doc : new NodeIterable(index.getNodes())) {
                admin.move(doc.getPath(), doc.getParent().getPath() + "/index");
            }

            String[] content = new String[] {
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap", "hst:sitemap",
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap/experiences", "hst:sitemapitem",
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap/experiences/_default_", "hst:sitemapitem",
                    "hst:relativecontentpath" , "experiences/${1}/index"
            };

            RepositoryTestCase.build(content, admin);

            admin.save();

            final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final List<String> responsePageNames = getResponseSiteMapPages(admin, mountId, ADMIN_CREDENTIALS, "name");

            Assertions.assertThat(responsePageNames)
                    .as("The sitemap tree should contain the name 'experience' and 'experience-subfolder' as these " +
                            "are the actual URL elements and, and thus NOT 'index' as the name of the document is")
                    .containsExactlyInAnyOrder("experiences", "experiences-subfolder");

        } finally {
            if (index != null) {
                index.remove();
                admin.save();
            }
            admin.logout();
        }
    }

    @Test
    public void pageNames_for_index_sitemap_items() throws Exception {
        final Session admin = createSession(ADMIN_CREDENTIALS);

        Node index = null;
        Node index2 = null;
        try {

            admin.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap").remove();

            JcrUtils.copy(admin, "/unittestcontent/documents/unittestproject/experiences/expPage1",
                    "/unittestcontent/documents/unittestproject/experiences/index");

            JcrUtils.copy(admin, "/unittestcontent/documents/unittestproject/experiences/expPage1",
                    "/unittestcontent/documents/unittestproject/experiences/experiences-subfolder/index");

            index = admin.getNode("/unittestcontent/documents/unittestproject/experiences/index");
            index.getProperty(HippoNodeType.HIPPO_NAME).remove();
            index.removeMixin(HippoNodeType.NT_NAMED);
            for (Node doc : new NodeIterable(index.getNodes())) {
                admin.move(doc.getPath(), doc.getParent().getPath() + "/index");
            }

            index2 = admin.getNode("/unittestcontent/documents/unittestproject/experiences/experiences-subfolder/index");
            index2.getProperty(HippoNodeType.HIPPO_NAME).remove();
            index2.removeMixin(HippoNodeType.NT_NAMED);
            for (Node doc : new NodeIterable(index2.getNodes())) {
                admin.move(doc.getPath(), doc.getParent().getPath() + "/index");
            }

            String[] content = new String[] {
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap", "hst:sitemap",
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap/_default_", "hst:sitemapitem",
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap/_default_/_index_", "hst:sitemapitem",
                    "hst:relativecontentpath" , "${1}/index",
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap/_default_/_default_", "hst:sitemapitem",
                    "/hst:hst/hst:configurations/unittestproject/hst:sitemap/_default_/_default_/_index_", "hst:sitemapitem",
                    "hst:relativecontentpath" , "${1}/${2}/index"
            };

            RepositoryTestCase.build(content, admin);

            admin.save();

            final String mountId = getNodeId(admin, "/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");

            final List<String> responsePageNames = getResponseSiteMapPages(admin, mountId, ADMIN_CREDENTIALS, "name");

            Assertions.assertThat(responsePageNames)
                    .as("The sitemap tree should contain the name 'experience' and 'experience-subfolder' as these " +
                            "are the actual URL elements and, and thus NOT 'index' as the name of the document is")
                    .containsExactlyInAnyOrder("experiences", "experiences-subfolder");

            // assertions on the pageTitle field : the implemented heuristic is as follows: the pageTitle should be the
            // displayName of the FOLDER for an *index* document UNLESS the index document has an explicit displayName:
            // then use that one

            final Node expFolder = admin.getNode("/unittestcontent/documents/unittestproject/experiences");
            expFolder.addMixin(HippoNodeType.NT_NAMED);
            expFolder.setProperty(HippoNodeType.HIPPO_NAME, "Experiences!!");
            admin.save();

            {
                final List<String> responsePageTitles = getResponseSiteMapPages(admin, mountId, ADMIN_CREDENTIALS, "pageTitle");

                Assertions.assertThat(responsePageTitles)
                        .as("The title should be from the containing parent folder for an index document which does " +
                                "not have an explicit displayName")
                        .containsExactlyInAnyOrder("Experiences!!", "experiences-subfolder");
            }
            index.addMixin(HippoNodeType.NT_NAMED);
            index.setProperty(HippoNodeType.HIPPO_NAME, "Index Experiences!!");
            admin.save();

            {
                final List<String> responsePageTitles = getResponseSiteMapPages(admin, mountId, ADMIN_CREDENTIALS, "pageTitle");

                Assertions.assertThat(responsePageTitles)
                        .as("The title should be from the containing parent folder for an index document which does " +
                                "not have an explicit displayName")
                        .containsExactlyInAnyOrder("Index Experiences!!", "experiences-subfolder");
            }
        } finally {
            if (index != null) {
                index.remove();
                index2.remove();
                final Node expFolder = admin.getNode("/unittestcontent/documents/unittestproject/experiences");
                expFolder.getProperty(HippoNodeType.HIPPO_NAME).remove();
                expFolder.removeMixin(HippoNodeType.NT_NAMED);
                admin.save();
            }
            admin.logout();
        }
    }
}
