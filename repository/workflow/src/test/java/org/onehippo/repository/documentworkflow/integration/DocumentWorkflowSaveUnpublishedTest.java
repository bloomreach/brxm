/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.onehippo.repository.documentworkflow.integration;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.assertj.core.api.Assertions;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.Utilities;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE;
import static org.hippoecm.repository.api.DocumentWorkflowAction.publish;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MEMBERS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NAME;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_TEXT;
import static org.hippoecm.repository.api.HippoNodeType.NT_USER;
import static org.hippoecm.repository.util.JcrUtils.getDateProperty;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.documentworkflow.HintsBuilder.ACTION_SAVE_UNPUBLISHED;

public class DocumentWorkflowSaveUnpublishedTest extends AbstractDocumentWorkflowIntegrationTest {

    private static final Calendar ZERO = new Calendar.Builder().setInstant(0L).build();
    private static final String PUBLISH_ACTION = publish().getAction();

    private DocumentWorkflow documentWorkflow;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        documentWorkflow = getDocumentWorkflow(handle);
        // Set the last modified date to zero to prevent millisecond comparison issues
        getVariant(HippoStdNodeType.UNPUBLISHED)
                .setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE, ZERO);
        session.save();
        // Make sure there is a published variant
        documentWorkflow.publish();
        Assertions.assertThat(documentWorkflow.hints())
                .describedAs("Can't publish because published.lastModifiedDate = unpublished.lastModifiedDate")
                .containsEntry(PUBLISH_ACTION, false)
                .describedAs("Can't saveUnpublished because there are no changes yet")
                .containsEntry(ACTION_SAVE_UNPUBLISHED, false);
    }

    @Test
    public void test_only_wf_session_changes_are_saved() throws RepositoryException, WorkflowException, RemoteException {

        final List<HippoWorkflowEvent<?>> hippoEvents = new ArrayList<>();
        final HippoEventBus hippoEventBus = event -> hippoEvents.add((HippoWorkflowEvent<?>) event);
        HippoServiceRegistry.register(hippoEventBus, HippoEventBus.class);

        try {
            final String uuid = UUID.randomUUID().toString();
            // Make a change to the unpublished variant with the internal workflow session
            documentWorkflow.getWorkflowContext().getInternalWorkflowSession()
                    .getNodeByIdentifier(getVariant(UNPUBLISHED).getIdentifier())
                    .setProperty(HIPPO_NAME, uuid);
            // Also make a change to the unpublished variant with the user session
            getVariant(UNPUBLISHED)
                    .setProperty(HIPPO_TEXT, UUID.randomUUID().toString());
            Assertions.assertThat(documentWorkflow.hints())
                    .describedAs("Can saveUnpublished because there are changes")
                    .containsEntry(ACTION_SAVE_UNPUBLISHED, true);
            // Invoke workflow action
            documentWorkflow.saveUnpublished();

            // Revert all pending changes on the user session
            session.refresh(false);
            final Node unpublished = getVariant(UNPUBLISHED);
            Assertions.assertThat(getDateProperty(unpublished, HIPPOSTDPUBWF_LAST_MODIFIED_DATE, null))
                    .describedAs("The last modified date is updated")
                    .isGreaterThan(ZERO);
            Assertions.assertThat(getStringProperty(unpublished, HIPPOSTDPUBWF_LAST_MODIFIED_BY, null))
                    .describedAs("The last modified by is the user ID of the user session")
                    .isEqualTo(session.getUserID());
            Assertions.assertThat(documentWorkflow.hints())
                    .describedAs("Can publish because published.lastModifiedDate < unpublished.lastModifiedDate")
                    .containsEntry(PUBLISH_ACTION, true)
                    .describedAs("Can't saveUnpublished because there are no changes anymore")
                    .containsEntry(ACTION_SAVE_UNPUBLISHED, false);
            Assertions.assertThat(getStringProperty(unpublished, HIPPO_NAME, null))
                    .describedAs("The name property is saved")
                    .isEqualTo(uuid);
            Assertions.assertThat(getStringProperty(unpublished, HIPPO_TEXT, null))
                    .describedAs("The text property is not saved")
                    .isNull();

            Assertions.assertThat(hippoEvents.size())
                    .describedAs("Exactly one workflow event was generated")
                    .isEqualTo(1);
            Assertions.assertThat(hippoEvents.get(0))
                    .describedAs("Workflow event has the right properties")
                    .extracting(
                            HippoWorkflowEvent::subjectId,
                            HippoEvent::action,
                            HippoWorkflowEvent::workflowCategory,
                            HippoWorkflowEvent::documentType)
                    .containsExactly(
                            unpublished.getParent().getIdentifier(),
                            ACTION_SAVE_UNPUBLISHED,
                            "default",
                            unpublished.getPrimaryNodeType().getName());
        } finally {
            HippoServiceRegistry.unregister(hippoEventBus, HippoEventBus.class);
        }
    }


    @Test
    public void test_user_session_changes_are_not_saved() throws RepositoryException, WorkflowException, RemoteException {

        // Make a change to the unpublished variant with the user session
        final String uuid = UUID.randomUUID().toString();
        getVariant(UNPUBLISHED)
                .setProperty(HIPPO_NAME, uuid);

        Assertions.assertThat(documentWorkflow.hints())
                .describedAs("No pending changes in internal workflow session")
                .containsEntry(ACTION_SAVE_UNPUBLISHED, false);
        Assertions.assertThatThrownBy(() -> documentWorkflow.saveUnpublished())
                .describedAs("Invoking %s action not allowed", ACTION_SAVE_UNPUBLISHED)
                .isExactlyInstanceOf(WorkflowException.class)
                .hasMessageContaining(ACTION_SAVE_UNPUBLISHED)
                .hasMessageContaining("not allowed");

        session.refresh(false);
        final Node unpublished = getVariant(UNPUBLISHED);
        Assertions.assertThat(getDateProperty(unpublished, HIPPOSTDPUBWF_LAST_MODIFIED_DATE, null))
                .describedAs("The last modified date has not changed")
                .isEqualByComparingTo(ZERO);
        Assertions.assertThat(getStringProperty(unpublished, HIPPOSTDPUBWF_LAST_MODIFIED_BY, null))
                .describedAs("The last modified by has not changed")
                .isEqualTo(TESTUSER);
        Assertions.assertThat(getStringProperty(unpublished, HIPPO_NAME, null))
                .describedAs("The name property is not saved")
                .isNull();
    }

    @Test
    public void action_not_allowed_without_unpublished_variant() throws RepositoryException, WorkflowException, RemoteException {

        getVariant(UNPUBLISHED).remove();
        session.save();

        Assertions.assertThat(getDocumentWorkflow(handle).hints())
                .describedAs("Can't saveUnpublished because there is no unpublished variant")
                .containsEntry(ACTION_SAVE_UNPUBLISHED, false);
    }


    @Test
    public void saveUnpublished_not_allowed_if_someone_else_is_holder_of_the_draft() throws Exception {

        JcrUtils.copy(session, "/hippo:configuration/hippo:users/admin", "/hippo:configuration/hippo:users/admin2");

        session.save();

        final Session admin2 = server.login(new SimpleCredentials("admin2", "admin".toCharArray()));

        try {
            // claim the holder for 'admin'
            documentWorkflow.obtainEditableInstance();

            final Node admin2Handle = admin2.getNode(handle.getPath());
            final DocumentWorkflow admin2DocWorkflow = getDocumentWorkflow(admin2Handle);

            assertFalse("'admin' should be holder, 'admin2' can't obtain editable instance",
                    (Boolean)admin2DocWorkflow.hints().get("obtainEditableInstance"));

            // note the condition below is important to meet since used for Channel Mgr to check whether CM user can
            // make changes to the unpublished: (s)he isn't allowed to do so if someone else is the holder for the
            // draft
            assertTrue("'admin' should be holder and can just invoke obtainEditableInstance again",
                    (Boolean)documentWorkflow.hints().get("obtainEditableInstance"));

            try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
                admin2DocWorkflow.obtainEditableInstance();
                fail("Expected 'admin2' should not be allowed to obtain instance");
            }catch (WorkflowException e) {
                // expected
            }


            final String uuid = UUID.randomUUID().toString();
            // make changes to the unpublished node with 'admin2' and then make sure that 'saveUnpublished' is still not
            // allowed
            assertFalse((Boolean)admin2DocWorkflow.hints().get("saveUnpublished"));
            admin2DocWorkflow.getWorkflowContext().getInternalWorkflowSession()
                    .getNodeByIdentifier(getVariant(UNPUBLISHED).getIdentifier())
                    .setProperty(HIPPO_NAME, uuid);

            // NOTE it is a choice that a user cannot make changes to unpublished when someone else is editing the
            // draft since technically it could be a valid option
            assertFalse("'admin' is holder of the draft hence #saveUnpublished not allowed for 'admin2'",
                    (Boolean)admin2DocWorkflow.hints().get("saveUnpublished"));

            try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
                admin2DocWorkflow.saveUnpublished();
                fail("Expected 'admin2' should not be  allowed to save unpublished");
            }catch (WorkflowException e) {
                // expected
            }

            // now make changes to the unpublished with 'admin' backed session (who is holder of draft)
            // and make sure (s)he can invoke 'saveUnpublished'

            assertFalse((Boolean)documentWorkflow.hints().get("saveUnpublished"));

            documentWorkflow.getWorkflowContext().getInternalWorkflowSession()
                    .getNodeByIdentifier(getVariant(UNPUBLISHED).getIdentifier())
                    .setProperty(HIPPO_NAME, uuid);

            assertTrue((Boolean)documentWorkflow.hints().get("saveUnpublished"));

            documentWorkflow.saveUnpublished();

        } finally {
            admin2.logout();
            session.getNode("/hippo:configuration/hippo:users/admin2").remove();
            session.save();
        }
    }

    @Test
    public void saveUnpublished_allowed_if_someone_else_is_holder_of_the_draft_but_document_transferable() throws Exception {
        // after saveDraft, someone else should be allowed to save the unpublished since document has become transferable
        JcrUtils.copy(session, "/hippo:configuration/hippo:users/admin", "/hippo:configuration/hippo:users/admin2");

        session.save();

        final Session admin2 = server.login(new SimpleCredentials("admin2", "admin".toCharArray()));

        try {
            // claim the holder for 'admin'
            documentWorkflow.obtainEditableInstance();

            // save draft by 'admin'
            documentWorkflow.saveDraft();

            final Node admin2Handle = admin2.getNode(handle.getPath());
            final DocumentWorkflow admin2DocWorkflow = getDocumentWorkflow(admin2Handle);

            assertTrue("Altough 'admin' is holder, 'admin2' can obtain editable instance since draft has been saved",
                    (Boolean)admin2DocWorkflow.hints().get("obtainEditableInstance"));

            final String uuid = UUID.randomUUID().toString();
            // make changes to the unpublished node with 'admin2' and then make sure that 'saveUnpublished' is allowed
            assertFalse((Boolean)admin2DocWorkflow.hints().get("saveUnpublished"));
            admin2DocWorkflow.getWorkflowContext().getInternalWorkflowSession()
                    .getNodeByIdentifier(getVariant(UNPUBLISHED).getIdentifier())
                    .setProperty(HIPPO_NAME, uuid);

            assertTrue("Altough 'admin' is holder of the draft, #saveUnpublished is allowed for 'admin2' since " +
                            "draft has been saved",
                    (Boolean)admin2DocWorkflow.hints().get("saveUnpublished"));

            admin2DocWorkflow.saveUnpublished();

        } finally {
            admin2.logout();
            session.getNode("/hippo:configuration/hippo:users/admin2").remove();
            session.save();
        }
    }

}
