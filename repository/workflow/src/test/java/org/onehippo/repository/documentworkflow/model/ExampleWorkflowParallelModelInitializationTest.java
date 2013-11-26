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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.scxml2.SCXMLExecutor;
import org.apache.commons.scxml2.model.TransitionTarget;
import org.junit.Test;
import org.onehippo.repository.scxml.SCXMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ExampleWorkflowParallelModelInitializationTest
 */
public class ExampleWorkflowParallelModelInitializationTest extends AbstractExampleWorkflowModelTest {

    private static Logger log = LoggerFactory.getLogger(ExampleWorkflowParallelModelInitializationTest.class);

    @Test
    public void testInitModel__Dn___() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dn", null, null, null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-new"));
        assertTrue(targetIds.contains("unpublished-variant-unavailable"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel__Dne___() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dne", null, null, null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-editing-new"));
        assertTrue(targetIds.contains("unpublished-variant-unavailable"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel__Dn_Un__() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dn", "Un", null, null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-new"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel__Dne_Un__() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dne", "Un", null, null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-editing-new"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel__Dl_Ul_Pl_() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dl", "Ul", "Pl", null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-live"));
        assertTrue(targetIds.contains("unpublished-variant-live"));
        assertTrue(targetIds.contains("published-variant-live"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel__Dle_Ul_Pl_() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dle", "Ul", "Pl", null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-editing-live"));
        assertTrue(targetIds.contains("unpublished-variant-live"));
        assertTrue(targetIds.contains("published-variant-live"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel__Dc_Uc_Pc_() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dc", "Uc", "Pc", null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-changed"));
        assertTrue(targetIds.contains("unpublished-variant-changed"));
        assertTrue(targetIds.contains("published-variant-changed"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel__Dce_Uc_Pc_() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dce", "Uc", "Pc", null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-editing-changed"));
        assertTrue(targetIds.contains("unpublished-variant-changed"));
        assertTrue(targetIds.contains("published-variant-changed"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel__Dn_Un_Pn_() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dn", "Un", "Pn", null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-new"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-new"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel__Dne_Un_Pn_() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dne", "Un", "Pn", null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-editing-new"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-new"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel___Un__() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations(null, "Un", null, null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-unavailable"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel___Ul_Pl_() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations(null, "Ul", "Pl", null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-unavailable"));
        assertTrue(targetIds.contains("unpublished-variant-live"));
        assertTrue(targetIds.contains("published-variant-live"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel___Uc_Pc_() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations(null, "Uc", "Pc", null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-unavailable"));
        assertTrue(targetIds.contains("unpublished-variant-changed"));
        assertTrue(targetIds.contains("published-variant-changed"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel___Un_Pn_() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations(null, "Un", "Pn", null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-unavailable"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-new"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel___Un__Rp() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations(null, "Un", null, "Rp");
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-unavailable"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-publication"));
    }

    @Test
    public void testInitModel__Dn_Un__Rp() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dn", "Un", null, "Rp");
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-new"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-unavailable"));
        assertTrue(targetIds.contains("request-publication"));
    }

    @Test
    public void testInitModel___Uc_Pc_Rp() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations(null, "Uc", "Pc", "Rp");
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-unavailable"));
        assertTrue(targetIds.contains("unpublished-variant-changed"));
        assertTrue(targetIds.contains("published-variant-changed"));
        assertTrue(targetIds.contains("request-publication"));
    }

    @Test
    public void testInitModel__Dc_Uc_Pc_Rp() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dc", "Uc", "Pc", "Rp");
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-changed"));
        assertTrue(targetIds.contains("unpublished-variant-changed"));
        assertTrue(targetIds.contains("published-variant-changed"));
        assertTrue(targetIds.contains("request-publication"));
    }

    @Test
    public void testInitModel___Ul_Pl_Ru() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations(null, "Ul", "Pl", "Ru");
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-unavailable"));
        assertTrue(targetIds.contains("unpublished-variant-live"));
        assertTrue(targetIds.contains("published-variant-live"));
        assertTrue(targetIds.contains("request-depublication"));
    }

    @Test
    public void testInitModel__Dl_Ul_Pl_Ru() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dl", "Ul", "Pl", "Ru");
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-live"));
        assertTrue(targetIds.contains("unpublished-variant-live"));
        assertTrue(targetIds.contains("published-variant-live"));
        assertTrue(targetIds.contains("request-depublication"));
    }

    @Test
    public void testInitModel__Dc_Uc_Pc_Ru() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dc", "Uc", "Pc", "Ru");
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-changed"));
        assertTrue(targetIds.contains("unpublished-variant-changed"));
        assertTrue(targetIds.contains("published-variant-changed"));
        assertTrue(targetIds.contains("request-depublication"));
    }

    @Test
    public void testInitModel___Un_Pn_Rp() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations(null, "Un", "Pn", "Rp");
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-unavailable"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-new"));
        assertTrue(targetIds.contains("request-publication"));
    }

    @Test
    public void testInitModel__Dn_Un_Pn_Rp() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations("Dn", "Un", "Pn", "Rp");
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-nonediting-new"));
        assertTrue(targetIds.contains("unpublished-variant-new"));
        assertTrue(targetIds.contains("published-variant-new"));
        assertTrue(targetIds.contains("request-publication"));
    }

    @Test
    public void testInitModel____Pl_() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations(null, null, "Pl", null);
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-unavailable"));
        assertTrue(targetIds.contains("unpublished-variant-unavailable"));
        assertTrue(targetIds.contains("published-variant-live"));
        assertTrue(targetIds.contains("request-unavailable"));
    }

    @Test
    public void testInitModel____Pl_Ru() throws Exception {
        SCXMLExecutor executor = SCXMLUtils.createSCXMLExecutor(scxml);
        Handle handle = ExampleModelCreationUtils.createHandleByVariantStateNotations(null, null, "Pl", "Ru");
        executor.getRootContext().set("handle", handle);
        executor.go();

        List<TransitionTarget> targets = SCXMLUtils.getCurrentTransitionTargetList(executor);
        assertEquals(4, targets.size());

        List<String> targetIds = SCXMLUtils.getCurrentTransitionTargetIdList(executor);
        log.debug("current target ids: {}", targetIds);
        assertTrue(targetIds.contains("draft-variant-unavailable"));
        assertTrue(targetIds.contains("unpublished-variant-unavailable"));
        assertTrue(targetIds.contains("published-variant-live"));
        assertTrue(targetIds.contains("request-depublication"));
    }
}
