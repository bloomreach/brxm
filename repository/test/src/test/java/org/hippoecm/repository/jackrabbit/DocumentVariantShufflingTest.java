/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit;

import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.impl.SessionDecorator;
import org.hippoecm.repository.jackrabbit.InternalHippoSession;
import org.hippoecm.repository.query.lucene.AuthorizationQuery;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_REQUEST;
import static org.hippoecm.repository.api.HippoNodeType.NT_REQUEST;
import static org.hippoecm.repository.util.JcrUtils.getStringListProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test sets up a structure like this
 * <pre>
 *    + mydoc (handle)
 *       + mydoc (draft)
 *       + mydoc (preview)
 *       + mydoc (live)
 * </pre>
 * <p>
 * and a domain for this and then a 'liveuser' who is only allowed to read mydoc (live) : this tests the SNS shuffling
 * going on in HippoLocalItemStateManager#reorderHandleChildNodeEntries
 * <p>
 * To make sure it works for all circumstance, we follow multiple to get to the live node, for example directly by
 * absolute path, or via relative path, etc etc
 */
public class DocumentVariantShufflingTest extends RepositoryTestCase {

    final String[] content = new String[]{
            "/shuffletest", "hippostd:folder",
            "/shuffletest/content", "hippostd:folder",
            "/shuffletest/content/mydoc", "hippo:handle",
            "/shuffletest/content/mydoc/mydoc", "hippo:testdocument",
            "jcr:mixinTypes", "mix:versionable",
            "hippo:availability", "draft",
            "color", "red",
            "/shuffletest/content/mydoc/mydoc", "hippo:testdocument",
            "jcr:mixinTypes", "mix:versionable",
            "hippo:availability", "preview",
            "color", "red",
            "/shuffletest/content/mydoc/mydoc", "hippo:testdocument",
            "jcr:mixinTypes", "mix:versionable",
            "hippo:availability", "live",
            "color", "red",
    };

    final static String PREFIX = "/hippo:configuration/hippo:domains";

    final String[] liveDocumentsDomain = new String[]{
            PREFIX + "/live-documents", "hipposys:domain",
            PREFIX + "/live-documents/hippo-document", "hipposys:domainrule",
            PREFIX + "/live-documents/hippo-document/availability-live", "hipposys:facetrule",
            "hipposys:equals", "true",
            "hipposys:facet", "hippo:availability",
            "hipposys:filter", "true",
            "hipposys:type", "String",
            "hipposys:value", "live",
            PREFIX + "/live-documents/hippo-document/content-and-descendants", "hipposys:facetrule",
            "hipposys:equals", "true",
            "hipposys:facet", "jcr:path",
            "hipposys:type", "Reference",
            "hipposys:value", "/shuffletest",
            PREFIX + "/live-documents/readonly", "hipposys:authrole",
            "hipposys:role", "readonly",
            "hipposys:users", "liveuser"
    };

    final String[] liveuserUser = new String[]{
            "/hippo:configuration/hippo:users/liveuser", "hipposys:user",
            "hipposys:password", "liveuser",
            "hipposys:securityprovider", "internal"
    };

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        build(content, session);
        build(liveDocumentsDomain, session);
        build(liveuserUser, session);
        session.save();
        session.refresh(false);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        if (session.nodeExists("/test/facnav")) {
            session.getNode("/test/facnav").remove();
        }
        session.getNode("/shuffletest").remove();
        session.getNode(PREFIX + "/live-documents").remove();
        session.getNode("/hippo:configuration/hippo:users/liveuser").remove();
        session.save();
        super.tearDown();
    }

    @Test
    public void live_user_access_by_absolute_path() throws Exception {

        Session liveuser = null;

        try {
            liveuser = server.login(new SimpleCredentials("liveuser", "liveuser".toCharArray()));

            assertTrue(liveuser.nodeExists("/shuffletest/content/mydoc/mydoc"));

            final List<String> availability = getStringListProperty(liveuser.getNode("/shuffletest/content/mydoc/mydoc"),
                    "hippo:availability", Collections.emptyList());

            assertEquals("live", availability.get(0));

        } finally {
            if (liveuser != null) {
                liveuser.logout();
            }
        }

    }

    @Test
    public void live_user_access_by_relative_path() throws Exception {

        Session liveuser = null;

        try {
            liveuser = server.login(new SimpleCredentials("liveuser", "liveuser".toCharArray()));

            final Node handle = liveuser.getNode("/shuffletest/content/mydoc");

            final Node document = handle.getNode("mydoc");
            assertTrue(document != null);

            final List<String> availability = getStringListProperty(document, "hippo:availability", Collections.emptyList());

            assertEquals("live", availability.get(0));

        } finally {
            if (liveuser != null) {
                liveuser.logout();
            }
        }

    }


    @Test
    public void live_user_check_hasNode() throws Exception {
        Session liveuser = null;

        try {
            liveuser = server.login(new SimpleCredentials("liveuser", "liveuser".toCharArray()));
            final Node handle = liveuser.getNode("/shuffletest/content/mydoc");
            assertTrue(handle.hasNode("mydoc"));

        } finally {
            if (liveuser != null) {
                liveuser.logout();
            }
        }
    }


    // below /test there is default read so live user can read there as well
    final String[] facetSearchNode = new String[]{
            "/test", "nt:unstructured",
            "/test/facnav", "hippo:facetsearch",
            "hippo:queryname", "xyz",
            "hippo:docbase", "to-fill-in",
            "hippo:facets", "color"
    };

    @Test
    public void live_user_access_live_variant_via_facet_search_result_set() throws Exception {

        build(facetSearchNode, session);
        // correct the docbase
        session.getNode("/test/facnav").setProperty("hippo:docbase", session.getNode("/shuffletest/content").getIdentifier());
        session.save();

        Session liveuser = null;
        try {
            for (int i = 0; i < 10; i++) {
                liveuser = server.login(new SimpleCredentials("liveuser", "liveuser".toCharArray()));
                final Node resultset = liveuser.getNode("/test/facnav/red/hippo:resultset");
                // only one variant should be visible for the liveuser
                assertEquals(1, resultset.getNodes().getSize());
                final Node mydoc = resultset.getNode("mydoc");

                final List<String> availabilityVirtualNode = getStringListProperty(mydoc, "hippo:availability", Collections.emptyList());
                assertEquals("live", availabilityVirtualNode.get(0));

                final List<String> availability = getStringListProperty(liveuser.getNode("/shuffletest/content/mydoc/mydoc"),
                        "hippo:availability", Collections.emptyList());

                assertEquals("live", availability.get(0));

                // copies first SNS and adds last, after this, remove the first entry and then save to reshuffle variants
                JcrUtils.copy(session, "/shuffletest/content/mydoc/mydoc", "/shuffletest/content/mydoc/mydoc");
                session.getNode("/shuffletest/content/mydoc/mydoc").remove();
                session.save();

                liveuser.logout();
            }
        } finally {
            if (liveuser != null) {
                liveuser.logout();
            }
        }

    }

    // below /test there is default read so live user can read there as well
    final String[] facetNavigationNode = new String[]{
            "/test", "nt:unstructured",
            "/test/facnav", "hippofacnav:facetnavigation",
            "hippo:docbase", "to-fill-in",
            "hippofacnav:facets", "color"
    };

    @Test
    public void live_user_access_live_variant_via_facet_navigation_result_set() throws Exception {

        build(facetNavigationNode, session);
        // correct the docbase
        session.getNode("/test/facnav").setProperty("hippo:docbase", session.getNode("/shuffletest/content").getIdentifier());
        session.save();

        Session liveuser = null;
        try {
            for (int i = 0; i < 10; i++) {
                liveuser = server.login(new SimpleCredentials("liveuser", "liveuser".toCharArray()));
                final Node resultset = liveuser.getNode("/test/facnav/color/red/hippo:resultset");
                // only one variant should be visible for the liveuser
                assertEquals(1, resultset.getNodes().getSize());
                final Node mydoc = resultset.getNode("mydoc");

                final List<String> availabilityVirtualNode = getStringListProperty(mydoc, "hippo:availability", Collections.emptyList());
                assertEquals("live", availabilityVirtualNode.get(0));

                final List<String> availability = getStringListProperty(liveuser.getNode("/shuffletest/content/mydoc/mydoc"),
                        "hippo:availability", Collections.emptyList());

                assertEquals("live", availability.get(0));

                // copies first SNS and adds last, after this, remove the first entry and then save to reshuffle variants
                JcrUtils.copy(session, "/shuffletest/content/mydoc/mydoc", "/shuffletest/content/mydoc/mydoc");
                session.getNode("/shuffletest/content/mydoc/mydoc").remove();
                session.save();

                liveuser.logout();
            }
        } finally {
            if (liveuser != null) {
                liveuser.logout();
            }
        }

    }



    /**
     * This test is a very specific test to validate a fix in the HippoAccessManager wrt inprocessNodeReadAccess : If
     * the authorization query is a 'matchAll' query for the live user (which we now *FAKE* by setting it explicitly),
     * then the not-yet authorization filtered resultset contains also the preview and draft version. Then after
     * access manager filtering, only the correct live draft should be kept *AND* it should be the first child entry
     * below the live user handle
     */
    @Test
    public void live_user_access_live_variant_via_facet_navigation_result_set_with_all_allowed_authorization_query() throws Exception {
        build(facetNavigationNode, session);
        // correct the docbase
        session.getNode("/test/facnav").setProperty("hippo:docbase", session.getNode("/shuffletest/content").getIdentifier());
        session.save();

        Session liveuser = null;
        try {
            for (int i = 0; i < 10; i++) {
                liveuser = server.login(new SimpleCredentials("liveuser", "liveuser".toCharArray()));
                XASessionImpl unwrap = (XASessionImpl) SessionDecorator.unwrap(liveuser);

                // we OVERRIDE the normal authorization query of the live user to a match all query: now still
                // everything should still work since the access will still be checked regardless of that the
                // searches return all results (since authorization query does not filter preview/draft variants
                // any more
                unwrap.setAuthorizationQuery(AuthorizationQuery.matchAll);

                final Node resultset = liveuser.getNode("/test/facnav/color/red/hippo:resultset");
                // only one variant should be visible for the liveuser
                assertEquals(1, resultset.getNodes().getSize());
                final Node mydoc = resultset.getNode("mydoc");

                final List<String> availabilityVirtualNode = getStringListProperty(mydoc, "hippo:availability", Collections.emptyList());
                assertEquals("live", availabilityVirtualNode.get(0));

                final List<String> availability = getStringListProperty(liveuser.getNode("/shuffletest/content/mydoc/mydoc"),
                        "hippo:availability", Collections.emptyList());

                assertEquals("live", availability.get(0));

                // copies first SNS and adds last, after this, remove the first entry and then save to reshuffle variants
                JcrUtils.copy(session, "/shuffletest/content/mydoc/mydoc", "/shuffletest/content/mydoc/mydoc");
                session.getNode("/shuffletest/content/mydoc/mydoc").remove();
                session.save();

                liveuser.logout();
            }
        } finally {
            if (liveuser != null) {
                liveuser.logout();
            }
        }
    }

    @Test
    public void assert_single_variant_works_correctly() throws Exception {

        session.getNode("/shuffletest/content/mydoc/mydoc").setProperty(HIPPO_AVAILABILITY,
                new String[]{"draft", "preview", "live"});
        session.getNode("/shuffletest/content/mydoc/mydoc[3]").remove();
        session.getNode("/shuffletest/content/mydoc/mydoc[2]").remove();
        session.save();
        Session liveuser = null;

        try {
            liveuser = server.login(new SimpleCredentials("liveuser", "liveuser".toCharArray()));

            assertTrue(liveuser.nodeExists("/shuffletest/content/mydoc/mydoc"));

        } finally {
            if (liveuser != null) {
                liveuser.logout();
            }
        }
    }

    @Test
    public void assert_reshuffling_with_other_nodes_than_document_variants_present_works_as_expected() throws Exception {
        // add SNS requests: they should NOT be reshuffled!
        String requestUUID1 = session.getNode("/shuffletest/content/mydoc").addNode(HIPPO_REQUEST, NT_REQUEST).getIdentifier();
        String requestUUID2 = session.getNode("/shuffletest/content/mydoc").addNode(HIPPO_REQUEST, NT_REQUEST).getIdentifier();
        String requestUUID3 = session.getNode("/shuffletest/content/mydoc").addNode(HIPPO_REQUEST, NT_REQUEST).getIdentifier();

        session.save();

        Session liveuser = null;
        try {
            for (int i = 0; i < 10; i++) {

                liveuser = server.login(new SimpleCredentials("liveuser", "liveuser".toCharArray()));
                final List<String> availability = getStringListProperty(liveuser.getNode("/shuffletest/content/mydoc/mydoc"),
                        "hippo:availability", Collections.emptyList());

                assertEquals("live", availability.get(0));

                assertEquals(requestUUID1, session.getNode("/shuffletest/content/mydoc/hippo:request").getIdentifier());
                assertEquals(requestUUID2, session.getNode("/shuffletest/content/mydoc/hippo:request[2]").getIdentifier());
                assertEquals(requestUUID3, session.getNode("/shuffletest/content/mydoc/hippo:request[3]").getIdentifier());

                // copies first SNS and adds last, after this, remove the first entry and then save to reshuffle variants
                JcrUtils.copy(session, "/shuffletest/content/mydoc/mydoc", "/shuffletest/content/mydoc/mydoc");
                session.getNode("/shuffletest/content/mydoc/mydoc").remove();
                session.save();

                liveuser.logout();
            }
        } finally {
            if (liveuser != null) {
                liveuser.logout();
            }
        }
    }

}
