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

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_REQUEST;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class DocumentWorkflowPublicationTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void publishPublishesDocument() throws Exception {
        assumeTrue(!isLive());
        final DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.publish();
        assertTrue("Document not live after publication", isLive());
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
