/*
 * Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Calendar;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.assertj.core.api.Assertions;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.util.JcrConstants;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.repository.HippoStdNodeType.DRAFT;
import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_STATE;
import static org.hippoecm.repository.HippoStdNodeType.MIXIN_SKIPDRAFT;
import static org.hippoecm.repository.HippoStdNodeType.PUBLISHED;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_BY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;

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
        } catch (UnsupportedRepositoryOperationException e) {
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
    public void can_edit_branch_with_pending_request_for_master() throws Exception {
        DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.requestPublication();
        assertFalse((Boolean) workflow.hints().get("obtainEditableInstance"));
        workflow.branch("foo", "Foo");

        assertFalse((Boolean) workflow.hints().get("obtainEditableInstance"));
        assertTrue((Boolean) workflow.hints("foo").get("obtainEditableInstance"));

        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.obtainEditableInstance();
            fail("Obtain Editable Instance should not be allowed");
        } catch (WorkflowException e) {
            assertEquals("Cannot invoke workflow documentworkflow action obtainEditableInstance: action not allowed or undefined", e.getMessage());
        }
        workflow.obtainEditableInstance("foo");
    }

    @Test
    public void obtain_editable_instance_fails_for_non_existing_branch() throws Exception {
        DocumentWorkflow workflow = getDocumentWorkflow(handle);
        try (Log4jInterceptor ignore = Log4jInterceptor.onAll().deny().build()) {
            workflow.obtainEditableInstance("foo");
        } catch (WorkflowException e) {
            assertEquals("Cannot invoke workflow documentworkflow action obtainEditableInstance: action not allowed or undefined", e.getMessage());
        }
    }

    @Test
    public void holderObtainsEditiableInstanceAsSaved() throws Exception {
        Session anotherAdmin = session.impersonate(CREDENTIALS);
        DocumentWorkflow workflow = getDocumentWorkflow(handle);
        final Node draft = workflow.obtainEditableInstance().getNode(session);
        assertEquals(ADMIN_ID, draft.getProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY).getString());
        draft.setProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY, "test-user");
        session.save();
        DocumentWorkflow anotherWorkflow = getDocumentWorkflow(anotherAdmin.getNodeByIdentifier(handle.getIdentifier()));
        final Node anotherDraft = anotherWorkflow.obtainEditableInstance().getNode(anotherAdmin);
        assertEquals("test-user", anotherDraft.getProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY).getString());
    }


    @Test
    public void draft_skips_children_that_should_be_skipped() throws Exception {

        addDocumentCompounds(document);

        DocumentWorkflow workflow = getDocumentWorkflow(handle);

        final Node draft = workflow.obtainEditableInstance().getNode(session);

        assertEquals("draft", draft.getProperty(HIPPOSTD_STATE).getString());

        assertThat(draft.hasNode("compound1")).isTrue();
        assertThat(draft.hasNode("compound1/subCompound1"))
                .as("Although subCompound1 has mixin '%s', it is still not skipped since " +
                        "not a direct child of the draft variant", MIXIN_SKIPDRAFT)
                .isTrue();
        assertThat(draft.getNode("compound1/subCompound1").isNodeType(MIXIN_SKIPDRAFT)).isTrue();

        assertThat(draft.hasNode("compound2"))
                .as("compound2 has mixin 'hippostd:skipdraft' and should be skipped")
                .isFalse();
        assertThat(draft.hasNode("compound3"))
                .as("hippo:testcompoundskipdraft contains 'hippostd:skipdraft' in cnd definition and should be skipped")
                .isFalse();


        // modify properties in unpublished and draft variant: to be confirmed now that the changes below children of
        // type hippo:skipdraft are KEPT in the unpublished variant, and that other children are copied from draft to
        // preview

        // document = preview
        final Node unpublished = document;
        unpublished.getNode("compound1").setProperty("hippo:testprop", "foo-prev");
        unpublished.getNode("compound1/subCompound1").setProperty("hippo:testprop", "foo-prev");
        unpublished.getNode("compound2").setProperty("hippo:testprop", "foo-prev");
        unpublished.getNode("compound3").setProperty("hippo:testprop", "foo-prev");

        draft.getNode("compound1").setProperty("hippo:testprop", "foo-draft");
        draft.getNode("compound1/subCompound1").setProperty("hippo:testprop", "foo-draft");

        session.save();

        // note we cannot set the compound2 or compound3 properties on draft since does not exist
        workflow.commitEditableInstance();

        unpublishedVariantAssertions(unpublished);


        // SINCE now we do have an EXISTING draft variant, do the above again to make sure it also works when the
        // DRAFT variant already exists: this triggers a different code flow

        String compound1Identifier = draft.getNode("compound1").getIdentifier();

        final Node newDraft = workflow.obtainEditableInstance().getNode(session);

        assertThat(newDraft.getIdentifier())
                .as("Expected draft node to be preserved")
                .isEqualTo(draft.getIdentifier());
        assertThat(newDraft.getNode("compound1").getIdentifier())
                .as("Expected draft children to have been replaced")
                .isNotEqualTo(compound1Identifier);

        unpublished.getNode("compound1").setProperty("hippo:testprop", "foo-prev");
        unpublished.getNode("compound1/subCompound1").setProperty("hippo:testprop", "foo-prev");

        draft.getNode("compound1").setProperty("hippo:testprop", "foo-draft");
        draft.getNode("compound1/subCompound1").setProperty("hippo:testprop", "foo-draft");
        session.save();

        workflow.commitEditableInstance();

        unpublishedVariantAssertions(unpublished);

    }

    /**
     * The follow test first create the following fixture
     * <pre>
     *     + document (preview)
     *       + compound1
     *         + subCompound1 (mixin hippostd:skipdraft)
     *       + compound2 (mixin hippostd:skipdraft)
     *       + compound3 (mixin hippostd:skipdraft)
     * </pre>
     * When from such a preview a draft is created, we expect that the draft does not contain 'compound2' and
     * 'compound3' since these are direct children of document variant and have mixin hippostd:skipdraft. The draft
     * however will contain compound1 and compound1/subCompound1 even though subCompound1 has mixin hippostd:skipdraft :
     * the reason is simple, only direct children of the document variant can be skipped to and from draft. If we'd have
     * to support deeper structures, then what happens if 'compound1' would be removed in the draft? Hence, only direct
     * children are skipped
     */
    private void addDocumentCompounds(final Node doc) throws RepositoryException {
        final Node compound1 = doc.addNode("compound1", "hippo:testcompound");
        compound1.setProperty("hippo:testprop", "foo");

        final Node subCompound1 = compound1.addNode("subCompound1", "hippo:testcompound");
        subCompound1.addMixin(MIXIN_SKIPDRAFT);
        subCompound1.setProperty("hippo:testprop", "foo");

        final Node compound2 = doc.addNode("compound2", "hippo:testcompound");
        compound2.addMixin(MIXIN_SKIPDRAFT);
        compound2.setProperty("hippo:testprop", "foo");

        final Node compound3 = doc.addNode("compound3", "hippo:testcompoundskipdraft");
        compound3.setProperty("hippo:testprop", "foo");

        compound3.addNode("subCompound3", "hippo:testcompound");

        session.save();
    }

    private void unpublishedVariantAssertions(final Node unpublished) throws RepositoryException {
        assertThat(unpublished.getProperty("compound1/hippo:testprop").getString())
                .isEqualTo("foo-draft");
        assertThat(unpublished.getProperty("compound1/subCompound1/hippo:testprop").getString())
                .isEqualTo("foo-draft");

        assertThat(unpublished.hasNode("compound2"))
                .as("Expected 'compound2' still to be present on unpublished after commit draft")
                .isTrue();
        assertThat(unpublished.hasNode("compound3"))
                .as("Expected 'compound3' still to be present on unpublished after commit draft")
                .isTrue();

        assertThat(unpublished.getProperty("compound2/hippo:testprop").getString())
                .as("Since 'compound2' is of type hippo:skipdraft it is expected to be kept as-is in preview")
                .isEqualTo("foo-prev");

        assertThat(unpublished.getProperty("compound3/hippo:testprop").getString())
                .as("Since 'compound3' is of type hippo:skipdraft it is expected to be kept as-is in preview")
                .isEqualTo("foo-prev");
    }

    @Test
    public void checkout_version_from_history_include_skipdraft_children() throws Exception {

        DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.branch("foo", "Foo");

        workflow.checkoutBranch(MASTER_BRANCH_ID);

        // add a child with mixin hippo:skipdraft
        Node masterUnpublished = document;
        final Node compound = masterUnpublished.addNode("compound", "hippo:testcompound");
        compound.addMixin(MIXIN_SKIPDRAFT);
        compound.setProperty("hippo:testprop", "foo");

        // if lastModificationDate is not changed, no new version is created for the unpublished variant, see
        // see org.onehippo.repository.documentworkflow.DocumentHandle.isCurrentUnpublishedVersioned()
        masterUnpublished.setProperty("hippostdpubwf:lastModificationDate", Calendar.getInstance());

        session.save();

        Node fooUnpublished = workflow.checkoutBranch("foo").getNode(session);

        // branch 'foo' should not have 'compound2'

        assertThat(fooUnpublished.hasNode("compound"))
                .as("branch 'foo' preview not expected to have the compound")
                .isFalse();

        masterUnpublished = workflow.checkoutBranch(MASTER_BRANCH_ID).getNode(session);

        // branch 'master' should HAVE 'compound2' restored from version history again
        assertThat(masterUnpublished.hasNode("compound"))
                .as("branch 'master' preview expected to have the compound")
                .isTrue();

    }

    @Test
    public void publication_does_include_skipdraft_children() throws Exception {

        DocumentWorkflow workflow = getDocumentWorkflow(handle);
        workflow.publish();

        Node published = getVariant("published");
        assertThat(published.hasNode("compound1")).isFalse();
        assertThat(published.hasNode("compound2")).isFalse();
        assertThat(published.hasNode("compound3")).isFalse();

        Node unpublished = getVariant("unpublished");
        addDocumentCompounds(unpublished);

        session.save();

        workflow.obtainEditableInstance();
        workflow.commitEditableInstance();
        workflow.publish();

        assertThat(published.hasNode("compound1")).isTrue();
        assertThat(published.hasNode("compound1/subCompound1"))
                .as("even though compound1/subCompound1 is of nodetype hippo:skipdraft, it should still be copied " +
                        "to published variant")
                .isTrue();
        assertThat(published.hasNode("compound2"))
                .as("even though compound2 is of nodetype hippo:skipdraft, it should still be copied " +
                        "to published variant")
                .isTrue();
        assertThat(published.hasNode("compound3"))
                .as("even though compound3 is of nodetype hippo:skipdraft, it should still be copied " +
                        "to published variant")
                .isTrue();

        // only the published variant left, make sure that obtain editable instance results in an unpublished WITH
        // the compounds of type hippo:skipdraft but draft doesn't have them
        getVariant("unpublished").remove();
        getVariant("draft").remove();
        session.save();

        workflow.obtainEditableInstance();

        unpublished = getVariant("unpublished");
        assertThat(unpublished.hasNode("compound1")).isTrue();
        assertThat(unpublished.hasNode("compound1/subCompound1")).isTrue();
        assertThat(unpublished.hasNode("compound2")).isTrue();
        assertThat(unpublished.hasNode("compound3")).isTrue();
        assertThat(unpublished.hasNode("compound3/subCompound3")).isTrue();

        final Node draft = getVariant("draft");
        assertThat(draft.hasNode("compound1")).isTrue();
        assertThat(draft.hasNode("compound1/subCompound1")).isTrue();
        assertThat(draft.hasNode("compound2")).isFalse();
        assertThat(draft.hasNode("compound3")).isFalse();


        workflow.commitEditableInstance();

        assertThat(unpublished.hasNode("compound1")).isTrue();
        assertThat(unpublished.hasNode("compound2")).isTrue();
        assertThat(unpublished.hasNode("compound3")).isTrue();
    }

    @Test
    public void document_is_audit_traced() throws RepositoryException, WorkflowException, RemoteException {

        final DocumentWorkflow documentWorkflow = getDocumentWorkflow(handle);
        Assertions.assertThat(documentWorkflow.listVersions())
                .isEmpty();

        documentWorkflow.obtainEditableInstance();
        final Node draft = getVariant(DRAFT);
        draft.addMixin(HippoStdPubWfNodeType.MIXIN_HIPPOSTDPUBWF_AUDIT_TRACE);
        final String propertyName = "hippo:title";
        final String propertyValue = "test";
        draft.setProperty(propertyName, propertyValue);
        session.save();
        documentWorkflow.commitEditableInstance();

        final Node unpublished = getVariant(UNPUBLISHED);

        assertThat(unpublished.isNodeType(HippoStdPubWfNodeType.MIXIN_HIPPOSTDPUBWF_AUDIT_TRACE)).isTrue();

        Assertions.assertThat(unpublished.getProperty(propertyName).getString())
                .isEqualTo(propertyValue);
        final SortedMap<Calendar, Set<String>> versions = documentWorkflow.listVersions();
        Assertions.assertThat(versions.size())
                .isEqualTo(1);

        documentWorkflow.restoreVersion(versions.firstKey());
        Assertions.assertThat(documentWorkflow.isModified()).isFalse();
    }

}
