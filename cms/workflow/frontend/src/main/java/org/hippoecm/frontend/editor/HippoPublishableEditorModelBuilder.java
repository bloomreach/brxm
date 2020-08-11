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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.frontend.service.IEditor.Mode;
import static org.hippoecm.frontend.service.IEditor.Mode.COMPARE;
import static org.hippoecm.frontend.service.IEditor.Mode.EDIT;

/**
 * Maps a {@link Document} to a {@link HippoStdPublishableEditorModel}
 */
public class HippoPublishableEditorModelBuilder {

    private static final Logger log = LoggerFactory.getLogger(HippoPublishableEditorModelBuilder.class);
    private Document document;
    private HippoStdPublishableEditorModel model;

    public static HippoStdPublishableEditorModel build(final Document document) throws EditorException {
        return new HippoPublishableEditorModelBuilder().setDocument(document).build();
    }

    HippoPublishableEditorModelBuilder setDocument(Document document) {
        this.document = document;
        this.model = new HippoStdPublishableEditorModel();
        return this;
    }

    HippoStdPublishableEditorModel build() throws EditorException {
        if (!hasDraft() && (isHolder() || isTransferable())) {
            final String message = "Document : %s cannot have a holder or transferable property if there is no draft";
            throw new EditorException(String.format(message, document));
        }
        if (isRevision()) {
            buildRevision();
        } else if (hasDraft()) {
            buildDraft();
        } else {
            buildPublishedUnpublished();
        }
        log.debug("Mapped document : {} to model {}", document, model);
        return model;

    }

    private void buildRevision() throws EditorException {
        if (hasUnpublished()) {
            model.setBase(getRevision());
            model.setEditor(getUnpublished());
            model.setMode(Mode.COMPARE);
        } else {
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

    private void buildPublishedUnpublished() throws EditorException {
        if (hasPublished()) {
            if (hasUnpublished()) {
                model.setMode(Mode.COMPARE);
                model.setEditor(getUnpublished());
                model.setBase(getPublished());
            } else {
                model.setEditor(getPublished());
                model.setMode(Mode.VIEW);
                model.setBase(StringUtils.EMPTY);
            }
        } else if (hasUnpublished()) {
            model.setMode(Mode.VIEW);
            model.setEditor(getUnpublished());
        } else {
            String message = "Document: %s without revision, draft, unpublished or published is invalid";
            throw new EditorException(String.format(message, document));
        }
    }

    private void buildDraft() {
        if (document.isTransferable()) {
            model.setEditor(getDraft());
            if (hasPublished()) {
                model.setMode(Mode.COMPARE);
                model.setBase(getPublished());
            } else if (hasUnpublished()) {
                model.setMode(Mode.COMPARE);
                model.setBase(getUnpublished());
            } else {
                model.setMode(Mode.VIEW);
            }
        } else {
            if (isHolder()) {
                model.setEditor(getDraft());
                model.setMode(EDIT);
            } else if (hasUnpublished()) {
                model.setEditor(getUnpublished());
                if (hasPublished()) {
                    model.setBase(getPublished());
                    model.setMode(Mode.COMPARE);
                } else {
                    model.setBase(StringUtils.EMPTY);
                    model.setMode(Mode.VIEW);
                }
            } else if (hasPublished()) {
                model.setEditor(getDraft());
                model.setBase(getPublished());
                model.setMode(COMPARE);
            } else {
                model.setEditor(getDraft());
                model.setBase(StringUtils.EMPTY);
                model.setMode(Mode.VIEW);
            }
        }
    }

    private boolean isHolder() {
        return document.isHolder();
    }

    private boolean isTransferable() {
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
