/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.documentworkflow.integration;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.util.JcrConstants;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.hippoecm.repository.util.JcrUtils.getMultipleStringProperty;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.DRAFT;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.PUBLISHED;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.documentworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.onehippo.repository.documentworkflow.DocumentVariant.MASTER_BRANCH_LABEL_UNPUBLISHED;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

public class DocumentWorkflowGetBranchTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void branch_hints() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        assertTrue(workflow.hints().containsKey("getBranch"));
        assertTrue((Boolean)workflow.hints().get("getBranch"));

        // when there is only a live version, branching is still possible (and will create a preview first)
        final Node toBecomeLive = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        toBecomeLive.setProperty(HippoStdNodeType.HIPPOSTD_STATE, HippoStdNodeType.PUBLISHED);
        toBecomeLive.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});
        toBecomeLive.removeMixin(MIX_VERSIONABLE);
        session.save();
        assertTrue((Boolean)workflow.hints().get("getBranch"));

        // when document is being edited, you getBranch is still allowed
        workflow.obtainEditableInstance();
        assertTrue((Boolean)workflow.hints().get("getBranch"));

        workflow.commitEditableInstance();
        assertTrue((Boolean)workflow.hints().get("getBranch"));
    }

    @Test
    public void get_branch_for_non_existing_branch() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            try {
                workflow.getBranch("foo", DRAFT);
                fail("There is no branch 'foo' so expected WorkflowException");
            } catch (WorkflowException e) {
                assertEquals("Cannot get branch 'foo' because it doesn't exist", e.getMessage());
            }
        }
        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            try {
                workflow.getBranch("foo", UNPUBLISHED);
                fail("There is no branch 'foo' so expected WorkflowException");
            } catch (WorkflowException e) {
                assertEquals("Cannot get branch 'foo' because it doesn't exist", e.getMessage());
            }
        }
        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            try {
                workflow.getBranch("foo", PUBLISHED);
                fail("There is no branch 'foo' so expected WorkflowException");
            } catch (WorkflowException e) {
                assertEquals("Cannot get branch 'foo' because it doesn't exist", e.getMessage());
            }
        }
    }

    @Test
    public void get_branch_for_draft() throws Exception {

        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");
        workflow.obtainEditableInstance();

        {
            final Document draftDocument = workflow.getBranch("foo", DRAFT);
            final Node draftNode = draftDocument.getNode(session);
            assertEquals("foo", draftNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
            assertFalse(draftNode.isNodeType(NT_FROZEN_NODE));
        }

        workflow.commitEditableInstance();

        workflow.checkoutBranch(MASTER_BRANCH_ID);
        // preview now becomes for 'bar'. draft should still be for 'foo'
        workflow.branch("bar", "Bar");

        {
            final Document draftDocument = workflow.getBranch("foo", DRAFT);
            final Node draftNode = draftDocument.getNode(session);
            assertEquals("foo", draftNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
            assertFalse(draftNode.isNodeType(NT_FROZEN_NODE));
        }

        workflow.obtainEditableInstance();

        {
            final Document draftDocument = workflow.getBranch("foo", DRAFT);
            // branch 'foo' exists but there is no draft for 'foo'
            assertNull(draftDocument);
        }
    }

    @Test
    public void get_branch_for_unpublished() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");

        {
            final Document unpublishedDocument = workflow.getBranch("foo", UNPUBLISHED);
            final Node unpublishedNode = unpublishedDocument.getNode(session);
            assertEquals("foo", unpublishedNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
            assertFalse(unpublishedNode.isNodeType(NT_FROZEN_NODE));
        }

        workflow.checkoutBranch(MASTER_BRANCH_ID);
        workflow.branch("bar", "Bar");

        {
            final Document unpublishedDocument = workflow.getBranch("foo", UNPUBLISHED);
            final Node unpublishedNode = unpublishedDocument.getNode(session);
            assertEquals("foo", unpublishedNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
            // the backing node is now a frozen node!
            assertTrue(unpublishedNode.isNodeType(NT_FROZEN_NODE));
        }
    }

    @Test
    public void get_branch_for_published() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");

        {
            final Document publishedDocument = workflow.getBranch("foo", PUBLISHED);
            assertNull(publishedDocument);
        }

        workflow.publishBranch("foo");

        {
            final Document publishedDocument = workflow.getBranch("foo", PUBLISHED);
            final Node publishedNode = publishedDocument.getNode(session);
            assertEquals("foo", publishedNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
            assertFalse(publishedNode.isNodeType(NT_FROZEN_NODE));
        }

        workflow.checkoutBranch(MASTER_BRANCH_ID);
        workflow.branch("bar", "Bar");

        workflow.publishBranch("bar");

        {
            final Document publishedDocument = workflow.getBranch("bar", PUBLISHED);
            final Node publishedNode = publishedDocument.getNode(session);
            assertEquals("bar", publishedNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString());
            // the backing node is now a frozen node!
            assertTrue(publishedNode.isNodeType(NT_FROZEN_NODE));
        }
    }
}
