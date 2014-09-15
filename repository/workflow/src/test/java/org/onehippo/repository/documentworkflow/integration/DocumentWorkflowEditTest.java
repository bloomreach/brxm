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

import javax.jcr.Node;

import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.hippoecm.repository.HippoStdNodeType.DRAFT;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.junit.Assume.assumeTrue;

public class DocumentWorkflowEditTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void obtainEditableInstanceReturnsDraft() throws Exception {
        DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Node variant = workflow.obtainEditableInstance().getNode(session);
        assertEquals("hippostd:state property is not 'draft'", DRAFT, JcrUtils.getStringProperty(variant, HIPPOSTD_STATE, null));
    }

    @Test
    public void commitEditableInstanceCopiesDraftToUnpublished() throws Exception {
        DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Node variant = workflow.obtainEditableInstance().getNode(session);
        final Node unpublished = getVariant(UNPUBLISHED);
        assumeTrue(unpublished != null);
        assumeTrue(!unpublished.hasProperty("foo"));
        variant.setProperty("foo", "bar");
        session.save();
        workflow.commitEditableInstance();
        assertEquals("bar", unpublished.getProperty("foo").getString());
    }

    @Test
    public void cannotEditWithPendingRequest() throws Exception {
        DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.requestPublication();
        assertFalse((Boolean) workflow.hints().get("obtainEditableInstance"));
    }

}
