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

import java.io.Serializable;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.assertj.core.api.Assertions;
import org.hippoecm.hst.core.internal.BranchSelectionService;
import org.hippoecm.hst.pagecomposer.jaxrs.AbstractFullRequestCycleTest;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.apache.jackrabbit.JcrConstants.NT_FROZENNODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_LABEL_UNPUBLISHED;

public abstract class AbstractXPageComponentResourceTest extends AbstractFullRequestCycleTest {

    protected final static String EXPERIENCE_PAGE_HANDLE_PATH = "/unittestcontent/documents/unittestproject/experiences/expPage1";
    protected final static String EXPERIENCE_PAGE_2_HANDLE_PATH = "/unittestcontent/documents/unittestproject/experiences/expPage2";

    protected Node handle;
    protected HippoSession admin;
    protected Node publishedExpPageVariant;
    protected Node unpublishedExpPageVariant;

    @Before
    public void setUp() throws Exception {
        super.setUp();


        admin = (HippoSession)createSession(ADMIN_CREDENTIALS);
        // backup experience Page
        JcrUtils.copy(admin, EXPERIENCE_PAGE_HANDLE_PATH, "/expPage1");

        // make sure the unpublished variant exists (just by depublishing for now....)
        final WorkflowManager workflowManager = ((HippoSession) admin).getWorkspace().getWorkflowManager();

        handle = admin.getNode(EXPERIENCE_PAGE_HANDLE_PATH);
        final DocumentWorkflow documentWorkflow = (DocumentWorkflow) workflowManager.getWorkflow("default", handle);
        documentWorkflow.depublish();
        // and publish again such that there is a live variant
        documentWorkflow.publish();

        publishedExpPageVariant = getVariant(handle, "published");
        unpublishedExpPageVariant = getVariant(handle, "unpublished");

        // create a catalog item that can be put in the container
        String[] content = new String[] {
                "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage", "hst:containeritempackage",
                "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/oldstyle-testitem", "hst:containeritemcomponent",
                   "hst:componentclassname", "org.hippoecm.hst.test.BannerComponent",
                "/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage/newstyle-testitem", "hst:componentdefinition",
                   "hst:componentclassname", "org.hippoecm.hst.test.BannerComponent",
        };

        RepositoryTestCase.build(content, admin);

        admin.save();

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

            if (admin.nodeExists("/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage")) {
                admin.getNode("/hst:hst/hst:configurations/hst:default/hst:catalog/testpackage").remove();
            } else {
                // hst config backup has already been restored potentially, see for example
                // XPageContainerComponentResourceTest#move_container_item_from_hst_config_to_XPage_is_not_allowed()
            }
            // restore experience page
            if (admin.nodeExists(EXPERIENCE_PAGE_HANDLE_PATH)) {
                admin.getNode(EXPERIENCE_PAGE_HANDLE_PATH).remove();
            }
            if (admin.nodeExists("/expPage1")) {
                admin.move("/expPage1", EXPERIENCE_PAGE_HANDLE_PATH);
            } else {
                fail("'/expPage1' should have been backed up");
            }

            admin.save();
        } finally {
            admin.logout();
            super.tearDown();
        }
    }

    protected DocumentWorkflow getDocumentWorkflow(final Session session) throws RepositoryException {
        final WorkflowManager workflowManager = ((HippoSession) session).getWorkspace().getWorkflowManager();
        return (DocumentWorkflow) workflowManager.getWorkflow("default", handle);
    }


    @NotNull
    protected Node getFrozenBannerComponent() throws Exception {
        final Node masterVersion = versionMasterByBranching();

        final Node frozenBannerComponent = masterVersion.getNode("hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");

        assertTrue(frozenBannerComponent.isNodeType(NT_FROZENNODE));
        return frozenBannerComponent;
    }

    @NotNull
    protected Node getFrozenBannerNewStyleComponent() throws Exception {
        final Node masterVersion = versionMasterByBranching();

        final Node frozenBannerNewStyleComponent = masterVersion.getNode("hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner-new-style");

        assertTrue(frozenBannerNewStyleComponent.isNodeType(NT_FROZENNODE));
        return frozenBannerNewStyleComponent;
    }


    @NotNull
    protected Node doVersionAndgetFrozenContainer() throws Exception {
        final Node masterVersion = versionMasterByBranching();

        final Node frozenContainer = masterVersion.getNode("hst:xpage/430df2da-3dc8-40b5-bed5-bdc44b8445c6");

        assertTrue(frozenContainer.isNodeType(NT_FROZENNODE));
        return frozenContainer;
    }

    /**
     * Branch unpublished to 'foo' and return the versioned master unpublished
     */
    @NotNull
    private Node versionMasterByBranching() throws Exception {
        final DocumentWorkflow workflow = (DocumentWorkflow) admin.getWorkspace().getWorkflowManager().getWorkflow("default", handle);

        // now the master branch is in version history!
        workflow.branch("foo", "Foo");

        assertThat(unpublishedExpPageVariant.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString())
                .isEqualTo("foo");

        // get the master frozen container item for banner
        final Node masterVersion = admin.getWorkspace().getVersionManager().getVersionHistory(unpublishedExpPageVariant.getPath())
                .getVersionByLabel(MASTER_BRANCH_LABEL_UNPUBLISHED).getFrozenNode();

        assertThat(masterVersion.hasProperty(HIPPO_PROPERTY_BRANCH_ID)).isFalse();
        return masterVersion;
    }


    protected void assertRequiresReload(final MockHttpServletResponse response, final boolean expected) throws Exception {
        final String restResponse = response.getContentAsString();

        final Map<String, Object> responseMap = mapper.readerFor(Map.class).readValue(restResponse);

        Assertions.assertThat(responseMap.get("reloadRequired")).isEqualTo(expected);
    }

    protected static class TestBranchSelectionService implements BranchSelectionService {

        @Override
        public String getSelectedBranchId(final Map<String, Serializable> contextPayload) {
            return (String)contextPayload.get("testBranchId");
        }

    }
}
