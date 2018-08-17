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

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.util.JcrConstants;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.hippoecm.repository.HippoStdNodeType.DRAFT;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.HippoStdNodeType.PUBLISHED;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class DocumentWorkflowEditTest extends AbstractDocumentWorkflowIntegrationTest {

    @Test
    public void obtainEditableInstanceReturnsDraft() throws Exception {
        DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Node variant = workflow.obtainEditableInstance().getNode(session);
        assertEquals("hippostd:state property is not 'draft'", DRAFT, JcrUtils.getStringProperty(variant, HIPPOSTD_STATE, null));
    }

    @Test
    public void firstEditOfPublishedOnlyDocumentCreatesInitialVersion() throws Exception {
        firstEditOfPublishedOnlyDocumentAssertions();
    }

    @Test
    public void firstEditOfPublishedOnlyDocumentWithoutAvailabilityCreatesInitialVersion() throws Exception {
        document.getProperty(HIPPO_AVAILABILITY).remove();
        session.save();
    }


    private void firstEditOfPublishedOnlyDocumentAssertions() throws RepositoryException, WorkflowException, RemoteException {
        Node variant = getVariant(PUBLISHED);

        assertNull(variant);
        variant = getVariant(UNPUBLISHED);
        assertNotNull(variant);
        if (variant.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
            // remove version history
            variant.removeMixin(JcrConstants.MIX_VERSIONABLE);
        }
        // change unpublished only variant into published only variant
        variant.setProperty(HIPPOSTD_STATE, PUBLISHED);
        if (document.hasProperty(HIPPO_AVAILABILITY)) {
            document.setProperty(HIPPO_AVAILABILITY, new String[]{"live"});
        }
        session.save();
        VersionManager versionManager = session.getWorkspace().getVersionManager();
        try {
            // this should now fail
            versionManager.getVersionHistory(variant.getPath());
            fail("versionManager.getVersionHistory should had failed");
        }
        catch (UnsupportedRepositoryOperationException e) {
            // expected because published variant (now) should no longer be versioned.
        }
        variant = getVariant(PUBLISHED);
        assertNotNull(variant);

        // this should also (re)create the UNPUBLISHED variant and an initial version of its published content
        getDocumentWorkflow(handle).obtainEditableInstance();

        final Value[] liveValues = variant.getProperty(HIPPO_AVAILABILITY).getValues();
        assertTrue(liveValues.length == 1);
        assertEquals("live", liveValues[0].getString());

        variant = getVariant(UNPUBLISHED);

        final Value[] values = variant.getProperty(HIPPO_AVAILABILITY).getValues();
        assertTrue(values.length == 1);
        assertEquals("preview", values[0].getString());

        assertNotNull(variant);
        // will throw an exception if their is no version history
        VersionHistory versionHistory = versionManager.getVersionHistory(variant.getPath());
        // will throw an exception if their is no first version
        versionHistory.getVersion("1.0");
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

    @Test
    public void holderObtainsEditiableInstanceAsSaved() throws Exception {
        Session anotherAdmin = session.impersonate(CREDENTIALS);
        DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Node draft = workflow.obtainEditableInstance().getNode(session);
        assertEquals(SYSTEMUSER_ID, draft.getProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY).getString());
        draft.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY, "test-user");
        session.save();
        DocumentWorkflow anotherWorkflow = getDocumentWorkflow(anotherAdmin.getNodeByIdentifier(handle.getIdentifier()));
        final Node anotherDraft = anotherWorkflow.obtainEditableInstance().getNode(anotherAdmin);
        assertEquals("test-user", anotherDraft.getProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY).getString());
    }
}
