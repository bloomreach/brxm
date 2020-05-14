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

package org.onehippo.repository.documentworkflow.task;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.assertj.core.api.Assertions;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.util.JcrConstants;

import static org.hippoecm.repository.HippoStdNodeType.DRAFT;
import static org.hippoecm.repository.HippoStdNodeType.MIXIN_SKIPDRAFT;
import static org.hippoecm.repository.HippoStdNodeType.NT_DOCUMENT;
import static org.hippoecm.repository.HippoStdNodeType.PUBLISHED;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;

public class IsModifiedTaskTest {

    private MockNode handle;
    private MockNode draft;
    private MockNode unpublished;
    private MockNode published;

    private IsModifiedTask isModifiedTask;
    private DocumentHandle documentHandle;

    @Before
    public void setUp() throws RepositoryException, WorkflowException {
        handle = MockNode.root().addNode("document", HippoNodeType.NT_HANDLE);

        draft = createVariant(NT_DOCUMENT, DRAFT);
        unpublished = createVariant(NT_DOCUMENT, UNPUBLISHED);
        published = createVariant(NT_DOCUMENT, PUBLISHED);

        isModifiedTask = new IsModifiedTask();
        documentHandle = new DocumentHandle(handle);
        documentHandle.initialize();
        isModifiedTask.setDocumentHandle(documentHandle);
    }

    @Test
    public void testEquals_published_unpublished_is_commutative() throws RepositoryException {
        assertEqualCommutes(published, unpublished, true);

        published.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "holder");
        published.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, "test");
        assertEqualCommutes(published, unpublished, true);

        unpublished.setProperty("test", "test");
        assertEqualCommutes(published, unpublished, false);

        published.setProperty("test", "test");
        assertEqualCommutes(published, unpublished, true);
    }

    @Test
    public void testEquals_draft_unpublished_is_commutative() throws RepositoryException {

        final MockNode uSkippedCompound = unpublished.addNode("compound", HippoNodeType.NT_COMPOUND);
        uSkippedCompound.addMixin(MIXIN_SKIPDRAFT);
        // compound is child of unpublished so it's skipped for comparison
        assertEqualCommutes(draft, unpublished, true);

        final MockNode dCompound = draft.addNode("compound", HippoNodeType.NT_COMPOUND);
        // draft has compound, unpublished not
        assertEqualCommutes(draft, unpublished, false);

        final MockNode uCompound = unpublished.addNode("compound", HippoNodeType.NT_COMPOUND);
        // draft has compound, unpublished too
        assertEqualCommutes(draft, unpublished, true);

        final MockNode uCompoundSubCompound = uCompound.addNode("subCompound", HippoNodeType.NT_COMPOUND);
        // sub-compound is not child of unpublished so it's NOT skipped for comparison
        uCompoundSubCompound.addMixin(MIXIN_SKIPDRAFT);
        assertEqualCommutes(draft, unpublished, false);

        dCompound.addNode("subCompound", HippoNodeType.NT_COMPOUND);
        // now draft also has the sub-compound, but not the mixin.
        // draft and unpublished are now equal because mixins are NOT part of equality check!
        assertEqualCommutes(draft, unpublished, true);

        final MockNode dSkippedCompound = draft.addNode("skipped-compound", HippoNodeType.NT_COMPOUND);
        dSkippedCompound.addMixin(MIXIN_SKIPDRAFT);
        dSkippedCompound.setProperty("p", "v");
        // It doesn't matter which variant has skipDraft children, they're ignored for comparison
        assertEqualCommutes(draft, unpublished, true);

        dSkippedCompound.removeMixin(MIXIN_SKIPDRAFT);
        assertEqualCommutes(draft, unpublished, false);
    }

    @Test
    public void testEquals_frozen_published_unpublished_is_commutative() throws RepositoryException, WorkflowException {

        published.remove();
        published = createVariant(JcrConstants.NT_FROZEN_NODE, PUBLISHED);

        published.setProperty(HippoStdNodeType.HIPPOSTD_HOLDER, "holder");
        published.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE, "test");

        documentHandle.initialize();

        unpublished.setProperty("x", new String[]{"A"});
        published.setProperty("x", new String[]{"A"});
        unpublished.setProperty("y", new String[]{"A", "B", "C"});
        published.setProperty("y", new String[]{"A", "B", "D"});
        assertEqualCommutes(published, unpublished, false);
    }


    private void assertEqualCommutes(Node a, Node b, boolean expectedEqualValue) throws RepositoryException {
        Assertions.assertThat(isModifiedTask.equals(a, b)).isEqualTo(isModifiedTask.equals(b, a));
        Assertions.assertThat(isModifiedTask.equals(a, b)).isEqualTo(expectedEqualValue);
    }

    private MockNode createVariant(String primaryType, String state) throws RepositoryException {
        final MockNode variant = handle.addNode("document", primaryType);
        variant.setProperty(HippoStdNodeType.HIPPOSTD_STATE, state);
        return variant;
    }
}
