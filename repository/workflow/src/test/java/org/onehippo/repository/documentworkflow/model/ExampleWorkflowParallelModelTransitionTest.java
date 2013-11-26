/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.documentworkflow.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.scxml2.SCXMLExecutor;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentEvents;
import org.onehippo.repository.scxml.SCXMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ExampleWorkflowParallelModelTransitionTest
 */
public class ExampleWorkflowParallelModelTransitionTest extends AbstractExampleWorkflowModelTest {

    private static Logger log = LoggerFactory.getLogger(ExampleWorkflowParallelModelTransitionTest.class);

    @Test
    public void testCreateDocument() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations(null, null, null, null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-unavailable"));
        assertTrue(targetIds.contains("unpublished-variant-unavailable"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));

        SCXMLUtils.triggerSignalEvents(executor, DocumentEvents.WFE_DOC_EDIT_OBTAIN);

        targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-editing-new"));
        assertTrue(targetIds.contains("unpublished-variant-unavailable"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testEditDraft() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dn", null, null, null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-new"));
        assertTrue(targetIds.contains("unpublished-variant-unavailable"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));

        SCXMLUtils.triggerSignalEvents(executor, DocumentEvents.WFE_DOC_EDIT_OBTAIN);

        targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-editing-new"));
        assertTrue(targetIds.contains("unpublished-variant-unavailable"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testCommitAndCloseDraft() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dne", null, null, null);
        assertNull(handle.getVariants().get("unpublished"));
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-editing-new"));
        assertTrue(targetIds.contains("unpublished-variant-unavailable"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));

        SCXMLUtils.triggerSignalEvents(executor, DocumentEvents.WFE_DOC_EDIT_COMMIT);
        SCXMLUtils.triggerSignalEvents(executor, DocumentEvents.WFE_DOC_EDIT_DISPOSE);
        assertNotNull(handle.getVariants().get("unpublished"));

        targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-new"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testEditDraftUnpublished() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dn", "Un", null, null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-new"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));

        SCXMLUtils.triggerSignalEvents(executor, DocumentEvents.WFE_DOC_EDIT_OBTAIN);

        targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-editing-new"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testPublish() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dn", "Un", null, null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-new"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));

        SCXMLUtils.triggerSignalEvents(executor, DocumentEvents.WFE_DOC_PUBLISH);

        targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-live"));
        assertTrue(targetIds.contains("unpublished-variant-live"));
        assertTrue(targetIds.contains("published-variant-live"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testChangeOnPublishedDocument() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dl", "Ul", "Pl", null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-live"));
        assertTrue(targetIds.contains("unpublished-variant-live"));
        assertTrue(targetIds.contains("published-variant-live"));
        assertTrue(targetIds.contains("request-unavailable"));

        SCXMLUtils.triggerSignalEvents(executor, DocumentEvents.WFE_DOC_EDIT_OBTAIN);

        targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-editing-live"));
        assertTrue(targetIds.contains("unpublished-variant-live"));
        assertTrue(targetIds.contains("published-variant-live"));
        assertTrue(targetIds.contains("request-unavailable"));

        SCXMLUtils.triggerSignalEvents(executor, DocumentEvents.WFE_DOC_EDIT_COMMIT);

        targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-editing-changed"));
        assertTrue(targetIds.contains("unpublished-variant-changed"));
        assertTrue(targetIds.contains("published-variant-changed"));
        assertTrue(targetIds.contains("request-unavailable"));

        SCXMLUtils.triggerSignalEvents(executor, DocumentEvents.WFE_DOC_EDIT_DISPOSE);

        targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-changed"));
        assertTrue(targetIds.contains("unpublished-variant-changed"));
        assertTrue(targetIds.contains("published-variant-changed"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testPublishAgain() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dc", "Uc", "Pc", null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-changed"));
        assertTrue(targetIds.contains("unpublished-variant-changed"));
        assertTrue(targetIds.contains("published-variant-changed"));
        assertTrue(targetIds.contains("request-unavailable"));

        SCXMLUtils.triggerSignalEvents(executor, DocumentEvents.WFE_DOC_PUBLISH);

        targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-live"));
        assertTrue(targetIds.contains("unpublished-variant-live"));
        assertTrue(targetIds.contains("published-variant-live"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testDepublish() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations(null, "Ul", "Pl", null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-unavailable"));
        assertTrue(targetIds.contains("unpublished-variant-live"));
        assertTrue(targetIds.contains("published-variant-live"));
        assertTrue(targetIds.contains("request-unavailable"));

        SCXMLUtils.triggerSignalEvents(executor, DocumentEvents.WFE_DOC_DEPUBLISH);

        targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-unavailable"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-new"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testRemoveDocument() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations(null, "Un", null, null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-unavailable"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));

        SCXMLUtils.triggerSignalEvents(executor, DocumentEvents.WFE_DOC_DELETE);

        targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("deleted"));
    }

}
