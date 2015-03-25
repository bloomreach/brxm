/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.model.DocumentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SiteMapItemDocumentRepresentationsTest extends AbstractSiteMapResourceTest {

    @Test
    public void homepage_primaryDocumentRepresentation() throws Exception {

        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        assertNotNull(home);

        final String relativeContentPath = home.getRelativeContentPath();
        assertEquals("common/homepage", relativeContentPath);

        final DocumentRepresentation primaryDocumentRepresentation = home.getPrimaryDocumentRepresentation();
        assertNotNull(primaryDocumentRepresentation);
        assertEquals(relativeContentPath, primaryDocumentRepresentation.getPath());
        assertEquals("Home Page", primaryDocumentRepresentation.getDisplayName());
        assertTrue(primaryDocumentRepresentation.isExists());
        assertTrue(primaryDocumentRepresentation.isDocument());

    }

    @Test
    public void primaryDocumentRepresentation_exists_even_for_non_existing_relativeContentPath() throws Exception {
        {
            final SiteMapItemRepresentation toUpdate = getSiteMapItemRepresentation(session, "home");
            toUpdate.setRelativeContentPath("path-to-no-node");
            toUpdate.setPrimaryDocumentRepresentation(null);

            final SiteMapResource siteMapResource = createResource();
            siteMapResource.update(toUpdate);
        }
        final SiteMapItemRepresentation homeWithNonExistingRelContentPath = getSiteMapItemRepresentation(session, "home");

        final String relativeContentPath = homeWithNonExistingRelContentPath.getRelativeContentPath();
        assertEquals("path-to-no-node", relativeContentPath);

        final DocumentRepresentation primaryDocumentRepresentation = homeWithNonExistingRelContentPath.getPrimaryDocumentRepresentation();
        assertNotNull(primaryDocumentRepresentation);
        assertEquals(relativeContentPath, primaryDocumentRepresentation.getPath());
        assertNull(primaryDocumentRepresentation.getDisplayName());
        assertFalse(primaryDocumentRepresentation.isExists());
        assertFalse(primaryDocumentRepresentation.isDocument());
    }

    @Test
    public void primaryDocumentRepresentation_is_null_for_siteMapItem_without_relativeContentPath() throws Exception {
        final SiteMapItemRepresentation toUpdate = getSiteMapItemRepresentation(session, "home");
        session.getNodeByIdentifier(toUpdate.getId()).getProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_RELATIVECONTENTPATH).remove();
        session.save();

        Thread.sleep(100);
        final SiteMapItemRepresentation homeWithoutRelContentPath = getSiteMapItemRepresentation(session, "home");
        assertNull(homeWithoutRelContentPath.getRelativeContentPath());
        assertNull(homeWithoutRelContentPath.getPrimaryDocumentRepresentation());
    }

    @Test
    public void update_homepage_through_primaryDocumentRepresentation() throws Exception {
        {
            final SiteMapItemRepresentation toUpdate = getSiteMapItemRepresentation(session, "home");
            final DocumentRepresentation representation = toUpdate.getPrimaryDocumentRepresentation();
            representation.setPath("updatedPath");
            final SiteMapResource siteMapResource = createResource();
            siteMapResource.update(toUpdate);
        }

        final SiteMapItemRepresentation updated = getSiteMapItemRepresentation(session, "home");
        final String updatedRelativeContentPath = updated.getRelativeContentPath();
        assertEquals("updatedPath", updatedRelativeContentPath);

        final DocumentRepresentation updatedRepresentation = updated.getPrimaryDocumentRepresentation();

        assertEquals(updatedRelativeContentPath, updatedRepresentation.getPath());
        assertNull(updatedRepresentation.getDisplayName());
        assertFalse(updatedRepresentation.isExists());
        assertFalse(updatedRepresentation.isDocument());
    }

    @Test
    public void update_by_setting_primaryDocumentRepresentation_and_relativeContentPath_has_precedence_for_primaryDocumentRepresentation_path() throws Exception {
        {
            final SiteMapItemRepresentation toUpdate = getSiteMapItemRepresentation(session, "home");
            final DocumentRepresentation representation = toUpdate.getPrimaryDocumentRepresentation();
            // both update PrimaryDocumentRepresentation and relativeContentPath : first has precedence
            representation.setPath("updatedPath1");
            toUpdate.setRelativeContentPath("updatedPath2");

            final SiteMapResource siteMapResource = createResource();
            siteMapResource.update(toUpdate);

            {
                final SiteMapItemRepresentation updated = getSiteMapItemRepresentation(session, "home");
                final String updatedRelativeContentPath = updated.getRelativeContentPath();
                assertEquals("updatedPath1", updatedRelativeContentPath);

                final DocumentRepresentation updatedRepresentation = updated.getPrimaryDocumentRepresentation();
                assertEquals(updatedRelativeContentPath, updatedRepresentation.getPath());
            }
        }
    }

    @Test
    public void primaryDocumentRepresentation_gets_loaded_by_read_everywhere_configuser() throws Exception {
        Session liveUser = null;
        try {
            liveUser = createLiveUserSession();
            {
                final SiteMapItemRepresentation home = getSiteMapItemRepresentation(liveUser, "home");
                final DocumentRepresentation representation = home.getPrimaryDocumentRepresentation();
                assertEquals("Home Page", representation.getDisplayName());
            }
            assertTrue(liveUser.nodeExists("/unittestcontent/documents/unittestproject/common/homepage/homepage"));

            session.getNode("/unittestcontent/documents/unittestproject/common/homepage/homepage").setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"preview"});
            session.save();
            Thread.sleep(100);

            liveUser.logout();
            liveUser = createLiveUserSession();

            // live user can't read homepage/homepage
            assertFalse(liveUser.nodeExists("/unittestcontent/documents/unittestproject/common/homepage/homepage"));

            // below should still work
            {
                final SiteMapItemRepresentation home = getSiteMapItemRepresentation(liveUser, "home");
                final DocumentRepresentation representation = home.getPrimaryDocumentRepresentation();
                assertEquals("Home Page", representation.getDisplayName());
            }

        } finally {
            if (liveUser != null) {
                liveUser.logout();
            }
        }
    }

    @Test
    public void homepage_availableDocumentRepresentations_is_empty_when_no_components_with_picked_documents_available() throws Exception {
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
        assertTrue(home.getAvailableDocumentRepresentations().isEmpty());
    }

    @Test
    public void homepage_availableDocumentRepresentations_is_empty_for_non_existing_component() throws Exception {
        final SiteMapItemRepresentation toUpdate = getSiteMapItemRepresentation(session, "home");
        session.getNodeByIdentifier(toUpdate.getId()).setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID,"non-existing");
        session.save();

        Thread.sleep(100);
        final SiteMapItemRepresentation homeWithoutExistingComponent = getSiteMapItemRepresentation(session, "home");
        assertTrue(homeWithoutExistingComponent.getAvailableDocumentRepresentations().isEmpty());
    }


    interface ParametersInfoDocuments {
        @Parameter(name = "documentLink")
        @DocumentLink()
        String getDocumentLink();

        @Parameter(name = "relativeJcrPath")
        @JcrPath(isRelative = true)
        String getRelativeJcrPath();

        @Parameter(name = "absoluteJcrPath")
        @JcrPath()
        String getAbsolutePath();

        @Parameter(name = "absoluteFolderJcrPath")
        @JcrPath()
        String getAbsoluteFolderPath();
    }

    @ParametersInfo(type = ParametersInfoDocuments.class)
    static class LeftMenu {
    }


    @Test
    public void test_availableDocumentRepresentations_working() throws Exception {
        // first set component classname and parameter names & values to leftmenu

        final Node leftComponent = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage/leftmenu");
        leftComponent.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, LeftMenu.class.getName());

        leftComponent.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES,
                new String[] {
                        "documentLink",
                        "relativeJcrPath",
                        "absoluteJcrPath",
                        "absoluteFolderJcrPath"
                });

        leftComponent.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES,
                new String[] {
                        "News/News2",
                        "News/News1",
                        "/unittestcontent/documents/unittestproject/common/aboutfolder/about-us",
                        "/unittestcontent/documents/unittestproject/common"
                });

        session.save();
        Thread.sleep(100);

        final SiteMapItemRepresentation homePage = getSiteMapItemRepresentation(session, "home");
        final Set<DocumentRepresentation> availableDocumentRepresentations = homePage.getAvailableDocumentRepresentations();
        assertEquals(4, availableDocumentRepresentations.size());

        DocumentRepresentation representation1 = new DocumentRepresentation(
                "/unittestcontent/documents/unittestproject/News/News2",
                "/unittestcontent/documents/unittestproject", "News2", true, true);
        DocumentRepresentation representation2 = new DocumentRepresentation(
                "/unittestcontent/documents/unittestproject/News/News1",
                "/unittestcontent/documents/unittestproject", "News1", true, true);
        DocumentRepresentation representation3 = new DocumentRepresentation(
                "/unittestcontent/documents/unittestproject/common/aboutfolder/about-us",
                "/unittestcontent/documents/unittestproject", "About Us", true, true);
        DocumentRepresentation representation4 = new DocumentRepresentation(
                "/unittestcontent/documents/unittestproject/common",
                "/unittestcontent/documents/unittestproject", "common", false, true);

        DocumentRepresentation[] representations = {representation1, representation2, representation3, representation4};
        for (DocumentRepresentation representation : representations) {
            assertTrue(availableDocumentRepresentations.contains(representation));
        }
    }

    @Test
    public void availableDocumentRepresentations_for_all_variants_are_returned() throws Exception {

        final Node leftComponent = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage/leftmenu");
        leftComponent.setProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, LeftMenu.class.getName());

        leftComponent.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES,
                new String[] {
                        "documentLink",
                        "relativeJcrPath",
                        "absoluteJcrPath",
                        "absoluteFolderJcrPath",
                        "documentLink",
                        "relativeJcrPath",
                        "absoluteJcrPath",
                        "absoluteFolderJcrPath"
                });

        leftComponent.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES,
                new String[] {
                        "News/News2",
                        "News/News1",
                        "/unittestcontent/documents/unittestproject/common/aboutfolder/about-us",
                        "/unittestcontent/documents/unittestproject/common",
                        "dummy-News/News2",
                        "dummy-News/News1",
                        "/unittestcontent/documents/unittestproject/common/aboutfolder/dummy-about-us",
                        "/unittestcontent/documents/unittestproject/dummy-common"
                });
        leftComponent.setProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETER_NAME_PREFIXES,
                new String[] {
                        "",
                        "",
                        "",
                        "",
                        "professional",
                        "professional",
                        "professional",
                        "professional"
                });

        session.save();
        Thread.sleep(100);

        final SiteMapItemRepresentation homePage = getSiteMapItemRepresentation(session, "home");
        final Set<DocumentRepresentation> availableDocumentRepresentations = homePage.getAvailableDocumentRepresentations();
        assertEquals(8, availableDocumentRepresentations.size());

        DocumentRepresentation representation1 = new DocumentRepresentation(
                "/unittestcontent/documents/unittestproject/News/News2",
                "/unittestcontent/documents/unittestproject", "News2", true, true);
        DocumentRepresentation representation2 = new DocumentRepresentation(
                "/unittestcontent/documents/unittestproject/News/News1",
                "/unittestcontent/documents/unittestproject", "News1", true, true);
        DocumentRepresentation representation3 = new DocumentRepresentation(
                "/unittestcontent/documents/unittestproject/common/aboutfolder/about-us",
                "/unittestcontent/documents/unittestproject", "About Us", true, true);
        DocumentRepresentation representation4 = new DocumentRepresentation(
                "/unittestcontent/documents/unittestproject/common",
                "/unittestcontent/documents/unittestproject", "common", false, true);

        DocumentRepresentation professionalRepresentation1 = new DocumentRepresentation(
                "/unittestcontent/documents/unittestproject/dummy-News/News2",
                "/unittestcontent/documents/unittestproject", null, false, false);
        DocumentRepresentation professionalRepresentation2 = new DocumentRepresentation(
                "/unittestcontent/documents/unittestproject/dummy-News/News1",
                "/unittestcontent/documents/unittestproject", null, false, false);
        DocumentRepresentation professionalRepresentation3 = new DocumentRepresentation(
                "/unittestcontent/documents/unittestproject/common/aboutfolder/dummy-about-us",
                "/unittestcontent/documents/unittestproject", null, false, false);
        DocumentRepresentation professionalRepresentation4 = new DocumentRepresentation(
                "/unittestcontent/documents/unittestproject/dummy-common",
                "/unittestcontent/documents/unittestproject", null, false, false);

        DocumentRepresentation[] representations = {representation1, representation2, representation3, representation4,
                professionalRepresentation1, professionalRepresentation2, professionalRepresentation3, professionalRepresentation4};

        for (DocumentRepresentation representation : representations) {
            assertTrue(availableDocumentRepresentations.contains(representation));
        }

    }

}