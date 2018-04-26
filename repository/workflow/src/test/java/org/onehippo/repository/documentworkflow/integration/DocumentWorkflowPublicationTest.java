/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Date;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.Utilities;
import org.hippoecm.repository.util.WorkflowUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.util.JcrConstants;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_REQUEST;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.onehippo.repository.util.JcrConstants.JCR_VERSION_HISTORY;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;

public class DocumentWorkflowPublicationTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void publishPublishesDocument() throws Exception {
        assumeTrue(!isLive());

        assertFalse(handle.isNodeType(NT_HIPPO_VERSION_INFO));

        assertEquals("unpublished", JcrUtils.getStringProperty(document, HIPPOSTD_STATE, null));
        assertArrayEquals(new String[]{"preview"} , JcrUtils.getMultipleStringProperty(document, HIPPO_AVAILABILITY, null));
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.publish();
        assertTrue("Document not live after publication", isLive());
        final Node preview = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.UNPUBLISHED).get();
        assertEquals("unpublished", JcrUtils.getStringProperty(preview, HIPPOSTD_STATE, null));
        assertArrayEquals(new String[]{"preview"} , JcrUtils.getMultipleStringProperty(preview, HIPPO_AVAILABILITY, null));

        final Node live = WorkflowUtils.getDocumentVariantNode(handle, WorkflowUtils.Variant.PUBLISHED).get();
        assertEquals("published", JcrUtils.getStringProperty(live, HIPPOSTD_STATE, null));
        assertArrayEquals(new String[]{"live"} , JcrUtils.getMultipleStringProperty(live, HIPPO_AVAILABILITY, null));

        assertTrue(preview.isNodeType(MIX_VERSIONABLE));
        assertFalse(live.isNodeType(MIX_VERSIONABLE));

        assertTrue("Publication should lead to a jcr version checkin, and after that the handle node should have " +
                "information about the version history node.", handle.isNodeType(NT_HIPPO_VERSION_INFO));
        assertEquals(handle.getProperty(HIPPO_VERSION_HISTORY_PROPERTY).getString(), preview.getProperty(JCR_VERSION_HISTORY).getNode().getIdentifier());
    }

    @Test
    public void schedulingPublicationPublishesDocument() throws Exception {
        assumeTrue(!isLive());
        final Version oldVersion = getBaseVersion();
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.publish(new Date(System.currentTimeMillis() + 1000));
        poll(new Executable() {
            @Override
            public void execute() throws Exception {
                assertTrue("Document not live after publication", isLive());
                assertFalse("Publication did not create a new version", oldVersion.isSame(getBaseVersion()));
                assertFalse("Still a request pending", containsRequest());
            }
        }, 10);
    }

    @Test
    public void depublishDepublishedDocument() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.publish();
        assumeTrue(isLive());
        workflow.depublish();
        assertFalse("Document still live after depublication", isLive());
    }

    @Test
    public void scheduleDepublicationDepublishedDocument() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.publish();
        assumeTrue(isLive());
        final Version oldVersion = getBaseVersion();
        workflow.depublish(new Date(System.currentTimeMillis() + 1000));
        poll(new Executable() {
            @Override
            public void execute() throws Exception {
                assertFalse("Document still live after depublication", isLive());
                assertFalse("Depublication did not create a new version", oldVersion.isSame(getBaseVersion()));
                assertFalse("Still a request pending", containsRequest());
            }
        }, 10);
    }

    @Test
    public void unmodifiedDocumentIsNotPublishable() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.publish();
        assumeTrue(isLive());
        assertFalse("Document is unmodified but hints suggest publish action is available",
                (Boolean) workflow.hints().get("publish"));
    }

    @Test
    public void cannotPublishOrDepublishWhenDocumentIsInUse() throws Exception {
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.obtainEditableInstance();
        assertFalse("Document is in use but hints suggest publish action is available",
                (Boolean) workflow.hints().get("publish"));
        assertFalse("Document is in use but hints suggest publish action is available",
                (Boolean) workflow.hints().get("depublish"));
    }

    private Version getBaseVersion() throws RepositoryException {
        return getVariant(UNPUBLISHED).getBaseVersion();
    }

    private boolean containsRequest() throws RepositoryException {
        return handle.hasNode(HIPPO_REQUEST);
    }
}
