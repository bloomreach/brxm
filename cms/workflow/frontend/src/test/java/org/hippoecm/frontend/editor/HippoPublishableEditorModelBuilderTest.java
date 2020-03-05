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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hippoecm.frontend.editor.TestDocument.DRAFT;
import static org.hippoecm.frontend.editor.TestDocument.PUBLISHED;
import static org.hippoecm.frontend.editor.TestDocument.REVISION;
import static org.hippoecm.frontend.editor.TestDocument.UNPUBLISHED;
import static org.hippoecm.frontend.editor.TestDocument.create;
import static org.hippoecm.frontend.service.IEditor.Mode.EDIT;
import static org.hippoecm.frontend.service.IEditor.Mode.VIEW;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class HippoPublishableEditorModelBuilderTest {


    private Document document;
    private HippoStdPublishableEditorModel model;

    public HippoPublishableEditorModelBuilderTest(Document document, HippoStdPublishableEditorModel model) {
        this.document = document;
        this.model = model;
    }

    // The bits of the boolean representation of the test number refer to the
    // draft, unpublished, published, revision, holder, transferable properties of document
    // e.g. 111010 refers to a document with a draft, published and unpublished variant and the current user is
    // the holder
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // 0 = 000000
                {createDocument().notDraft().notUnpublished().notPublished().notRevision().notHolder().notTransferable()
                        , invalid()},
                // 1 = 000001
                {createDocument().notDraft().notUnpublished().notPublished().notRevision().notHolder().transferable()
                        , invalid()},
                // 2 = 000010
                {createDocument().notDraft().notUnpublished().notPublished().notRevision().holder().notTransferable()
                        , invalid()},
                // 3 = 000011
                {createDocument().notDraft().notUnpublished().notPublished().notRevision().holder().transferable()
                        , invalid()},
                // 4 = 000100
                {createDocument().notDraft().notUnpublished().notPublished().revision().notHolder().notTransferable()
                        , invalid()},
                // 5 = 000101
                {createDocument().notDraft().notUnpublished().notPublished().revision().notHolder().transferable()
                        , invalid()},
                // 6 = 000110
                {createDocument().notDraft().notUnpublished().notPublished().revision().holder().notTransferable()
                        , invalid()},
                // 7 = 000111
                {createDocument().notDraft().notUnpublished().notPublished().revision().holder().transferable()
                        , invalid()},
                // 8 = 001000
                {createDocument().notDraft().notUnpublished().published().notRevision().notHolder().notTransferable()
                        , createModel(PUBLISHED, VIEW)},
                // 9 = 001001
                {createDocument().notDraft().notUnpublished().published().notRevision().notHolder().transferable()
                        , invalid()},
                // 10 = 001010
                {createDocument().notDraft().notUnpublished().published().notRevision().holder().notTransferable()
                        , invalid()},
                // 11 = 001011
                {createDocument().notDraft().notUnpublished().published().notRevision().holder().transferable()
                        , invalid()},
                // 12 = 001100
                {createDocument().notDraft().notUnpublished().published().revision().notHolder().notTransferable()
                        , invalid()},
                // 13 = 001101
                {createDocument().notDraft().notUnpublished().published().revision().notHolder().transferable()
                        , invalid()},
                // 14 = 001110
                {createDocument().notDraft().notUnpublished().published().revision().holder().notTransferable()
                        , invalid()},
                // 15 = 001111
                {createDocument().notDraft().notUnpublished().published().revision().holder().transferable()
                        , invalid()},
                // 16 = 010000
                {createDocument().notDraft().unpublished().notPublished().notRevision().notHolder().notTransferable()
                        , createModel(UNPUBLISHED, VIEW)},
                // 17 = 010001
                {createDocument().notDraft().unpublished().notPublished().notRevision().notHolder().transferable()
                        , invalid()},
                // 18 = 010010
                {createDocument().notDraft().unpublished().notPublished().notRevision().holder().notTransferable()
                        , invalid()},
                // 19 = 010011
                {createDocument().notDraft().unpublished().notPublished().notRevision().holder().transferable()
                        , invalid()},
                // 20 = 010100
                {createDocument().notDraft().unpublished().notPublished().revision().notHolder().notTransferable()
                        , createModel(UNPUBLISHED, REVISION)},
                // 21 = 010101
                {createDocument().notDraft().unpublished().notPublished().revision().notHolder().transferable()
                        , invalid()},
                // 22 = 010110
                {createDocument().notDraft().unpublished().notPublished().revision().holder().notTransferable()
                        , invalid()},
                // 23 = 010111
                {createDocument().notDraft().unpublished().notPublished().revision().holder().transferable()
                        , invalid()},
                // 24 = 011000
                {createDocument().notDraft().unpublished().published().notRevision().notHolder().notTransferable()
                        , createModel(UNPUBLISHED, PUBLISHED)},
                // 25 = 011001
                {createDocument().notDraft().unpublished().published().notRevision().notHolder().transferable()
                        , invalid()},
                // 26 = 011010
                {createDocument().notDraft().unpublished().published().notRevision().holder().notTransferable()
                        , invalid()},
                // 27 = 011011
                {createDocument().notDraft().unpublished().published().notRevision().holder().transferable()
                        , invalid()},
                // 28 = 011100
                {createDocument().notDraft().unpublished().published().revision().notHolder().notTransferable()
                        , createModel(UNPUBLISHED, REVISION)},
                // 29 = 011101
                {createDocument().notDraft().unpublished().published().revision().notHolder().transferable()
                        , invalid()},
                // 30 = 011110
                {createDocument().notDraft().unpublished().published().revision().holder().notTransferable()
                        , invalid()},
                // 31 = 011111
                {createDocument().notDraft().unpublished().published().revision().holder().transferable()
                        , invalid()},
                // 32 = 100000
                {createDocument().draft().notUnpublished().notPublished().notRevision().notHolder().notTransferable()
                        , createModel(DRAFT, VIEW)},
                // 33 = 100001
                {createDocument().draft().notUnpublished().notPublished().notRevision().notHolder().transferable()
                        , createModel(DRAFT, VIEW)},
                // 34 = 100010
                {createDocument().draft().notUnpublished().notPublished().notRevision().holder().notTransferable()
                        , createModel(DRAFT, EDIT)},
                // 35 = 100011
                {createDocument().draft().notUnpublished().notPublished().notRevision().holder().transferable()
                        , createModel(DRAFT, VIEW)},
                // 36 = 100100
                {createDocument().draft().notUnpublished().notPublished().revision().notHolder().notTransferable()
                        , invalid()},
                // 37 = 100101
                {createDocument().draft().notUnpublished().notPublished().revision().notHolder().transferable()
                        , invalid()},
                // 38 = 100110
                {createDocument().draft().notUnpublished().notPublished().revision().holder().notTransferable()
                        , invalid()},
                // 39 = 100111
                {createDocument().draft().notUnpublished().notPublished().revision().holder().transferable()
                        , invalid()},
                // 40 = 101000
                {createDocument().draft().notUnpublished().published().notRevision().notHolder().notTransferable()
                        , createModel(DRAFT, PUBLISHED)},
                // 41 = 101001
                {createDocument().draft().notUnpublished().published().notRevision().notHolder().transferable()
                        , createModel(DRAFT, VIEW)},
                // 42 = 101010
                {createDocument().draft().notUnpublished().published().notRevision().holder().notTransferable()
                        , createModel(DRAFT, EDIT)},
                // 43 = 101011
                {createDocument().draft().notUnpublished().published().notRevision().holder().transferable()
                        , createModel(DRAFT, VIEW)},
                // 44 = 101100
                {createDocument().draft().notUnpublished().published().revision().notHolder().notTransferable()
                        , invalid()},
                // 45 = 101101
                {createDocument().draft().notUnpublished().published().revision().notHolder().transferable()
                        , invalid()},
                // 46 = 101110
                {createDocument().draft().notUnpublished().published().revision().holder().notTransferable()
                        , invalid()},
                // 47 = 101111
                {createDocument().draft().notUnpublished().published().revision().holder().transferable()
                        , invalid()},
                // 48 = 110000
                {createDocument().draft().unpublished().notPublished().notRevision().notHolder().notTransferable()
                        , createModel(DRAFT, UNPUBLISHED)},
                // 49 = 110001
                {createDocument().draft().unpublished().notPublished().notRevision().notHolder().transferable()
                        , createModel(DRAFT, VIEW)},
                // 50 = 110010
                {createDocument().draft().unpublished().notPublished().notRevision().holder().notTransferable()
                        , createModel(DRAFT, EDIT)},
                // 51 = 110011
                {createDocument().draft().unpublished().notPublished().notRevision().holder().transferable()
                        , createModel(DRAFT, VIEW)},
                // 52 = 110100
                {createDocument().draft().unpublished().notPublished().revision().notHolder().notTransferable()
                        , createModel(UNPUBLISHED, REVISION)},
                // 53 = 110101
                {createDocument().draft().unpublished().notPublished().revision().notHolder().transferable()
                        , createModel(UNPUBLISHED, REVISION)},
                // 54 = 110110
                {createDocument().draft().unpublished().notPublished().revision().holder().notTransferable()
                        , createModel(UNPUBLISHED, REVISION)},
                // 55 = 110111
                {createDocument().draft().unpublished().notPublished().revision().holder().transferable()
                        , createModel(UNPUBLISHED, REVISION)},
                // 56 = 111000
                {createDocument().draft().unpublished().published().notRevision().notHolder().notTransferable()
                        , createModel(DRAFT, PUBLISHED)},
                // 57 = 111001
                {createDocument().draft().unpublished().published().notRevision().notHolder().transferable()
                        , createModel(DRAFT, VIEW)},
                // 58 = 111010
                {createDocument().draft().unpublished().published().notRevision().holder().notTransferable()
                        , createModel(DRAFT, EDIT)},
                // 59 = 111011
                {createDocument().draft().unpublished().published().notRevision().holder().transferable()
                        , createModel(DRAFT, VIEW)},
                // 60 = 111100
                {createDocument().draft().unpublished().published().notRevision().notHolder().notTransferable()
                        , createModel(DRAFT, PUBLISHED)},
                // 61 = 111101
                {createDocument().draft().unpublished().published().revision().notHolder().transferable()
                        , createModel(UNPUBLISHED, REVISION)},
                // 62 = 111110
                {createDocument().draft().unpublished().published().revision().holder().notTransferable()
                        , createModel(UNPUBLISHED, REVISION)},
                // 63 = 111111
                {createDocument().draft().unpublished().published().revision().holder().transferable()
                        , createModel(UNPUBLISHED, REVISION)},
        });
    }

    @NotNull
    protected static TestDocument createDocument() {
        return create();
    }

    @NotNull
    protected static HippoStdPublishableEditorModel createModel(String editor, String base) {
        final HippoStdPublishableEditorModel model = new HippoStdPublishableEditorModel();
        model.setMode(IEditor.Mode.COMPARE);
        model.setEditor(editor);
        model.setBase(base);
        return model;
    }

    @NotNull
    protected static HippoStdPublishableEditorModel createModel(String editor, IEditor.Mode mode) {
        final HippoStdPublishableEditorModel model = new HippoStdPublishableEditorModel();
        model.setEditor(editor);
        model.setMode(mode);
        model.setBase(StringUtils.EMPTY);
        return model;
    }

    public static HippoStdPublishableEditorModel invalid() {
        final HippoStdPublishableEditorModel hippoStdPublishableEditorModel = new HippoStdPublishableEditorModel();
        hippoStdPublishableEditorModel.setMode(null);
        hippoStdPublishableEditorModel.setEditor("INVALID");
        hippoStdPublishableEditorModel.setBase("INVALID");
        return hippoStdPublishableEditorModel;
    }

    @Test
    public void test() {
        HippoStdPublishableEditorModel actual;
        try {
            actual = HippoPublishableEditorModelBuilder.build(document);
        } catch (EditorException e) {
            actual = invalid();
        }
        final HippoStdPublishableEditorModel expected = this.model;
        final String message = String.format("Expected \n%s to map to: \n%s instead of \n%s", document, model, actual);
        assertEquals(message, expected, actual);
    }
}
