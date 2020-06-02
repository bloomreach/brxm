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

import org.assertj.core.api.Assertions;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.events.HippoWorkflowEvent;

import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE;
import static org.hippoecm.repository.api.DocumentWorkflowAction.publish;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NAME;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_TEXT;
import static org.hippoecm.repository.util.JcrUtils.getDateProperty;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
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

}
