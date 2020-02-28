/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.editor;

import java.util.Arrays;
import java.util.Collection;

import org.hippoecm.frontend.service.EditorException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hippoecm.frontend.editor.TestDocument.*;
import static org.hippoecm.frontend.service.IEditor.Mode.COMPARE;
import static org.hippoecm.frontend.service.IEditor.Mode.EDIT;
import static org.hippoecm.frontend.service.IEditor.Mode.VIEW;
import static org.junit.Assert.*;
@RunWith(Parameterized.class)
public class HippoPublishableEditorModelBuilderTest {


    // The bits of the boolean representation of the test number refer to the
    // draft, unpublished, published, revision, holder, transferable properties of document
    // e.g. 111010 refers to a document with a draft, published and unpublished variant and the current user is
    // the holder
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // 0 = 000000
                { createDocument().notDraft().notUnpublished().notPublished().notRevision().notHolder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 1 = 000001
                { createDocument().notDraft().notUnpublished().notPublished().notRevision().notHolder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 2 = 000010
                { createDocument().notDraft().notUnpublished().notPublished().notRevision().holder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 3 = 000011
                { createDocument().notDraft().notUnpublished().notPublished().notRevision().holder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 4 = 000100
                { createDocument().notDraft().notUnpublished().notPublished().revision().notHolder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 5 = 000101
                { createDocument().notDraft().notUnpublished().notPublished().revision().notHolder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 6 = 000110
                { createDocument().notDraft().notUnpublished().notPublished().revision().holder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 7 = 000111
                { createDocument().notDraft().notUnpublished().notPublished().revision().holder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 8 = 001000
                { createDocument().notDraft().notUnpublished().published().notRevision().notHolder().notTransferable()
                        , createModel().editor(PUBLISHED).noBase().mode(VIEW) },
                // 9 = 001001
                { createDocument().notDraft().notUnpublished().published().notRevision().notHolder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 10 = 001010
                { createDocument().notDraft().notUnpublished().published().notRevision().holder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 11 = 001011
                { createDocument().notDraft().notUnpublished().published().notRevision().holder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 12 = 001100
                { createDocument().notDraft().notUnpublished().published().revision().notHolder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 13 = 001101
                { createDocument().notDraft().notUnpublished().published().revision().notHolder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 14 = 001110
                { createDocument().notDraft().notUnpublished().published().revision().holder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 15 = 001111
                { createDocument().notDraft().notUnpublished().published().revision().holder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 16 = 010000
                { createDocument().notDraft().unpublished().notPublished().notRevision().notHolder().notTransferable()
                        , createModel().editor(UNPUBLISHED).noBase().mode(VIEW) },
                // 17 = 010001
                { createDocument().notDraft().unpublished().notPublished().notRevision().notHolder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 18 = 010010
                { createDocument().notDraft().unpublished().notPublished().notRevision().holder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 19 = 010011
                { createDocument().notDraft().unpublished().notPublished().notRevision().holder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 20 = 010100
                { createDocument().notDraft().unpublished().notPublished().revision().notHolder().notTransferable()
                        , createModel().editor(UNPUBLISHED).base(REVISION).mode(COMPARE)},
                // 21 = 010101
                { createDocument().notDraft().unpublished().notPublished().revision().notHolder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 22 = 010110
                { createDocument().notDraft().unpublished().notPublished().revision().holder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 23 = 010111
                { createDocument().notDraft().unpublished().notPublished().revision().holder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 24 = 011000
                { createDocument().notDraft().unpublished().published().notRevision().notHolder().notTransferable()
                        , createModel().editor(UNPUBLISHED).base(PUBLISHED).mode(COMPARE) },
                // 25 = 011001
                { createDocument().notDraft().unpublished().published().notRevision().notHolder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 26 = 011010
                { createDocument().notDraft().unpublished().published().notRevision().holder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 27 = 011011
                { createDocument().notDraft().unpublished().published().notRevision().holder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 28 = 011100
                { createDocument().notDraft().unpublished().published().revision().notHolder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 29 = 011101
                { createDocument().notDraft().unpublished().published().revision().notHolder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 30 = 011110
                { createDocument().notDraft().unpublished().published().revision().holder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 31 = 011111
                { createDocument().notDraft().unpublished().published().revision().holder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 32 = 100000
                { createDocument().draft().notUnpublished().notPublished().notRevision().notHolder().notTransferable()
                        , createModel().editor(DRAFT).noBase().mode(VIEW) },
                // 33 = 100001
                { createDocument().draft().notUnpublished().notPublished().notRevision().notHolder().transferable()
                        , createModel().editor(DRAFT).noBase().mode(VIEW) },
                // 34 = 100010
                { createDocument().draft().notUnpublished().notPublished().notRevision().holder().notTransferable()
                        , createModel().editor(DRAFT).noBase().mode(EDIT) },
                // 35 = 100011
                { createDocument().draft().notUnpublished().notPublished().notRevision().holder().transferable()
                        , createModel().editor(DRAFT).noBase().mode(VIEW) },
                // 36 = 100100
                { createDocument().draft().notUnpublished().notPublished().revision().notHolder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 37 = 100101
                { createDocument().draft().notUnpublished().notPublished().revision().notHolder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 38 = 100110
                { createDocument().draft().notUnpublished().notPublished().revision().holder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 39 = 100111
                { createDocument().draft().notUnpublished().notPublished().revision().holder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 40 = 101000
                { createDocument().draft().notUnpublished().published().notRevision().notHolder().notTransferable()
                        , createModel().editor(DRAFT).base(PUBLISHED).mode(COMPARE)},
                // 41 = 101001
                { createDocument().draft().notUnpublished().published().notRevision().notHolder().transferable()
                        , createModel().editor(DRAFT).noBase().mode(VIEW)},
                // 42 = 101010
                { createDocument().draft().notUnpublished().published().notRevision().holder().notTransferable()
                        , createModel().editor(DRAFT).noBase().mode(EDIT)},
                // 43 = 101011
                { createDocument().draft().notUnpublished().published().notRevision().holder().transferable()
                        , createModel().editor(DRAFT).noBase().mode(VIEW)},
                // 44 = 101100
                { createDocument().draft().notUnpublished().published().revision().notHolder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 45 = 101101
                { createDocument().draft().notUnpublished().published().revision().notHolder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 46 = 101110
                { createDocument().draft().notUnpublished().published().revision().holder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 47 = 101111
                { createDocument().draft().notUnpublished().published().revision().holder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 48 = 110000
                { createDocument().draft().unpublished().notPublished().notRevision().notHolder().notTransferable()
                        , createModel().editor(DRAFT).base(UNPUBLISHED).mode(COMPARE) },
                // 49 = 110001
                { createDocument().draft().unpublished().notPublished().notRevision().notHolder().transferable()
                        , createModel().editor(DRAFT).noBase().mode(VIEW) },
                // 50 = 110010
                { createDocument().draft().unpublished().notPublished().notRevision().holder().notTransferable()
                        , createModel().editor(DRAFT).noBase().mode(EDIT) },
                // 51 = 110011
                { createDocument().draft().unpublished().notPublished().notRevision().holder().transferable()
                        , createModel().editor(DRAFT).noBase().mode(VIEW) },
                // 52 = 110100
                { createDocument().draft().unpublished().notPublished().revision().notHolder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 53 = 110101
                { createDocument().draft().unpublished().notPublished().revision().notHolder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 54 = 110110
                { createDocument().draft().unpublished().notPublished().revision().holder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 55 = 110111
                { createDocument().draft().unpublished().notPublished().revision().holder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 56 = 111000
                { createDocument().draft().unpublished().published().notRevision().notHolder().notTransferable()
                        , createModel().editor(DRAFT).base(PUBLISHED).mode(COMPARE) },
                // 57 = 111001
                { createDocument().draft().unpublished().published().notRevision().notHolder().transferable()
                        , createModel().editor(DRAFT).noBase().mode(VIEW)},
                // 58 = 111010
                { createDocument().draft().unpublished().published().notRevision().holder().notTransferable()
                        , createModel().editor(DRAFT).noBase().mode(EDIT) },
                // 59 = 111011
                { createDocument().draft().unpublished().published().notRevision().holder().transferable()
                        , createModel().editor(DRAFT).noBase().mode(VIEW) },
                // 60 = 111100
                { createDocument().draft().unpublished().published().notRevision().notHolder().notTransferable()
                        , createModel().editor(DRAFT).base(PUBLISHED).mode(COMPARE) },
                // 61 = 111101
                { createDocument().draft().unpublished().published().revision().notHolder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 62 = 111110
                { createDocument().draft().unpublished().published().revision().holder().notTransferable()
                        , HippoStdPublishableEditorModel.INVALID() },
                // 63 = 111111
                { createDocument().draft().unpublished().published().revision().holder().transferable()
                        , HippoStdPublishableEditorModel.INVALID() },
        });
    }

    @NotNull
    protected static TestDocument createDocument() {
        return create();
    }

    @NotNull
    protected static HippoStdPublishableEditorModel createModel() {
        return HippoStdPublishableEditorModel.create();
    }

    private Document document;

    private HippoStdPublishableEditorModel model;

    public HippoPublishableEditorModelBuilderTest(Document document, HippoStdPublishableEditorModel model) {
        this.document = document;
        this.model = model;
    }

    @Test
    public void test() {
        HippoStdPublishableEditorModel actual;
        try {
            actual = HippoPublishableEditorModelBuilder.build(document);
        } catch (EditorException e){
            actual =  HippoStdPublishableEditorModel.INVALID();
        }
        final HippoStdPublishableEditorModel expected = this.model;
        final String message = String.format("Expected \n%s to map to: \n%s instead of \n%s", document, model, actual);
        assertEquals(message, expected, actual);
    }
}
