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

import java.util.Objects;

import org.apache.commons.lang.StringUtils;

public class DocumentImpl implements Document{

    private String unpublished;
    private String published;
    private String draft;
    private String revision;
    private boolean holder;
    private boolean transferable;
    private boolean retainable;


    public DocumentImpl() {
        draft = unpublished = published = revision = StringUtils.EMPTY;
    }

    public void setUnpublished(final String unpublished) {
        this.unpublished = unpublished;
    }

    public void setPublished(final String published) {
        this.published = published;
    }

    public void setDraft(final String draft) {
        this.draft = draft;
    }

    public void setRevision(final String revision) {
        this.revision = revision;
    }

    public void setHolder(final boolean holder) {
        this.holder = holder;
    }

    public void setTransferable(final boolean transferable) {
        this.transferable = transferable;
    }

    public void setRetainable(final boolean retainable) {
        this.retainable = retainable;
    }

    @Override
    public String getUnpublished() {
        return unpublished;
    }

    @Override
    public String getPublished() {
        return published;
    }

    @Override
    public String getDraft() {
        return draft;
    }

    @Override
    public String getRevision() {
        return revision;
    }

    @Override
    public boolean isTransferable() {
        return transferable;
    }

    @Override
    public boolean isRetainable() {
        return retainable;
    }


    @Override
    public boolean isHolder() {
        return holder;
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentImpl)) {
            return false;
        }
        final DocumentImpl document = (DocumentImpl) o;
        return isHolder() == document.isHolder() &&
                isTransferable() == document.isTransferable() &&
                Objects.equals(getUnpublished(), document.getUnpublished()) &&
                Objects.equals(getPublished(), document.getPublished()) &&
                Objects.equals(getDraft(), document.getDraft()) &&
                Objects.equals(getRevision(), document.getRevision());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUnpublished(), getPublished(), getDraft(), getRevision(), isHolder(), isTransferable());
    }


    @Override
    public String toString() {
        return "DocumentImpl{" +
                "unpublished='" + unpublished + '\'' +
                ", published='" + published + '\'' +
                ", draft='" + draft + '\'' +
                ", revision='" + revision + '\'' +
                ", holder=" + holder +
                ", transferable=" + transferable +
                ", retainable=" + retainable +
                '}';
    }
}
