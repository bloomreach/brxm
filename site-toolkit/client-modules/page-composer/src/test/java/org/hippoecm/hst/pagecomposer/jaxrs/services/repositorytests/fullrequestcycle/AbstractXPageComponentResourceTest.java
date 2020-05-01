/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.pagecomposer.jaxrs.AbstractFullRequestCycleTest;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractPageComposerTest;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.Utilities;
import org.junit.After;
import org.junit.Before;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractXPageComponentResourceTest extends AbstractFullRequestCycleTest {

    protected final static SimpleCredentials ADMIN_CREDENTIALS = new SimpleCredentials("admin", "admin".toCharArray());
    protected final static SimpleCredentials EDITOR_CREDENTIALS = new SimpleCredentials("editor", "editor".toCharArray());
    protected final static SimpleCredentials AUTHOR_CREDENTIALS = new SimpleCredentials("author", "author".toCharArray());

    private final static String EXPERIENCE_PAGE_HANDLE_PATH = "/unittestcontent/documents/unittestproject/experiences/expPage1";
    protected final static String EXPERIENCE_PAGE_2_HANDLE_PATH = "/unittestcontent/documents/unittestproject/experiences/expPage2";

    protected Node handle;
    protected Node publishedExpPageVariant;
    protected Node unpublishedExpPageVariant;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Session session = createSession(ADMIN_CREDENTIALS);
        // backup experience Page
        JcrUtils.copy(session, EXPERIENCE_PAGE_HANDLE_PATH, "/expPage1");

        // make sure the unpublished variant exists (just by depublishing for now....)
        final WorkflowManager workflowManager = ((HippoSession) session).getWorkspace().getWorkflowManager();

        handle = session.getNode(EXPERIENCE_PAGE_HANDLE_PATH);
        final DocumentWorkflow documentWorkflow = (DocumentWorkflow) workflowManager.getWorkflow("default", handle);
        documentWorkflow.depublish();
        // and publish again such that there is a live variant
        documentWorkflow.publish();

        publishedExpPageVariant = getVariant(handle, "published");
        unpublishedExpPageVariant = getVariant(handle, "unpublished");

        // create a catalog item that can be put in the container
        String[] content = new String[] {
                "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage", "hst:containeritempackage",
                "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/testitem", "hst:containeritemcomponent",
                   "hst:componentclassname", "org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.fullrequestcycle.BannerComponent",
        };

        RepositoryTestCase.build(content, session);

        session.save();

    }

    protected Node getVariant(final Node handle, final String state) throws RepositoryException {
        for (Node variant : new NodeIterable(handle.getNodes(handle.getName()))) {
            if (state.equals(JcrUtils.getStringProperty(variant, HIPPOSTD_STATE, null))) {
                return variant;
            }
        }
        return null;
    }

    @After
    public void tearDown() throws Exception {
        try {
            final Session session = createSession("admin", "admin");

            if (session.nodeExists("/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage")) {
                session.getNode("/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage").remove();
            } else {
                // hst config backup has already been restored potentially, see for example
                // XPageContainerComponentResourceTest#move_container_item_from_hst_config_to_XPage_is_not_allowed()
            }
            // restore experience page
            if (session.nodeExists(EXPERIENCE_PAGE_HANDLE_PATH)) {
                session.getNode(EXPERIENCE_PAGE_HANDLE_PATH).remove();
            }
            if (session.nodeExists("/expPage1")) {
                session.move("/expPage1", EXPERIENCE_PAGE_HANDLE_PATH);
            }

            session.save();
            session.logout();
        } finally {
            super.tearDown();
        }
    }

}
