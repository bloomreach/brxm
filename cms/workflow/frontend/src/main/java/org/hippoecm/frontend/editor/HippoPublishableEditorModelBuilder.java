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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.service.EditorException;

import static org.hippoecm.frontend.service.IEditor.Mode;

/**
 * Maps a {@link Document} to a {@link HippoStdPublishableEditorModel}
 */
public class HippoPublishableEditorModelBuilder {

    private Document document;
    private HippoStdPublishableEditorModel model;
    public static final HippoStdPublishableEditorModel INVALID = HippoStdPublishableEditorModel.INVALID();

    public static HippoStdPublishableEditorModel build(final Document document) throws EditorException {
        return new HippoPublishableEditorModelBuilder().setDocument(document).build();
    }

    HippoPublishableEditorModelBuilder setDocument(Document document) {
        this.document = document;
        this.model = HippoStdPublishableEditorModel.create();
        return this;
    }

    HippoStdPublishableEditorModel build() throws EditorException {
        if (!hasDraft() && ( isHolder() || isTransferable() )){
            final String message = "Document : %s cannot have a holder or transferable property if there is no draft";
            String.format(message, document);
            throw new EditorException(message);
        }
        if (isRevision()) {
            if (hasPublished() || hasUnpublished() || hasDraft()){
                final String message = "Document : %s cannot be a revision and also have a draft, unpublished or published";
                throw new EditorException(String.format(message, document));
            }
            return buildHasRevision();
        }
        if (hasDraft()) {
            return buildHasDraft();
        }
        return buildPublishedUnpublished();

    }

    private HippoStdPublishableEditorModel buildHasRevision() throws EditorException {
        if (hasUnpublished()){
            return model.base(getRevision()).editor(getUnpublished()).mode(Mode.COMPARE);
        }
        else{
            final String message = "A revision without an unpublished variant is invalid, document : %s";
            throw new EditorException(String.format(message, document));

        }
    }

    private String getUnpublished() {
        return document.getUnpublished();
    }

    private String getRevision() {
        return document.getRevision();
    }

    private HippoStdPublishableEditorModel buildPublishedUnpublished() throws EditorException {
        if (hasPublished()) {
            if (hasUnpublished()) {
                return model.mode(Mode.COMPARE).base(getPublished()).editor(getUnpublished());
            }
            return model.mode(Mode.VIEW).noBase().editor(getPublished());
        }
            if (hasUnpublished()) {
                return model.mode(Mode.VIEW).editor(getUnpublished());
            } else {
                String message = "Document: %s without revision, draft, unpublished or published is invalid";
                throw new EditorException(String.format(message, document));
            }
    }

    private HippoStdPublishableEditorModel buildHasDraft() {
        if (document.isTransferable()) {
            return model.editor(getDraft()).mode(Mode.VIEW);
        }
        if (isHolder()) {
            return model.editor(getDraft()).mode(Mode.EDIT);
        }
        if (hasPublished()){
            return model.editor(getDraft()).base(getPublished()).mode(Mode.COMPARE);
        }
        if (hasUnpublished()){
            return model.editor(getDraft()).base(getUnpublished()).mode(Mode.COMPARE);
        }
        return model.editor(getDraft()).noBase().mode(Mode.VIEW);
    }

    private boolean isHolder() {
        return document.isHolder();
    }

    private boolean isTransferable(){
        return document.isTransferable();
    }

    private String getDraft() {
        return document.getDraft();
    }

    private boolean hasPublished() {
        return StringUtils.isNotEmpty(document.getPublished());

    }

    private boolean isRevision() {
        return StringUtils.isNotEmpty(document.getRevision());
    }


    private boolean hasDraft() {
        return StringUtils.isNotEmpty(getDraft());
    }

    private boolean hasUnpublished() {
        return StringUtils.isNotEmpty(getUnpublished());
    }

    private String getPublished() {
        return document.getPublished();
    }
}
