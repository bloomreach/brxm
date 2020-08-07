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
package org.hippoecm.hst.pagecomposer.jaxrs.services.action;

public class XPageActionContext {

    private String xPageId;
    private Boolean publishable;
    private Boolean unpublishable;
    private Boolean requestPublication;
    private Boolean requestDepublication;
    private Boolean copyAllowed;
    private Boolean moveAllowed;
    private Boolean deleteAllowed;

    public String getXPageId() {
        return xPageId;
    }

    XPageActionContext setXPageId(final String xPageId) {
        this.xPageId = xPageId;
        return this;
    }

    public Boolean isPublishable() {
        return publishable;
    }

    public XPageActionContext setPublishable(final Boolean publishable) {
        this.publishable = publishable;
        return this;
    }

    public Boolean isUnpublishable() {
        return unpublishable;
    }

    public XPageActionContext setUnpublishable(final Boolean unpublishable) {
        this.unpublishable = unpublishable;
        return this;
    }

    public XPageActionContext setRequestPublication(final Boolean requestPublication) {
        this.requestPublication = requestPublication;
        return this;
    }

    public Boolean isRequestPublication() {
        return requestPublication;
    }

    public XPageActionContext setRequestDepublication(final Boolean requestDepublication) {
        this.requestDepublication = requestDepublication;
        return this;
    }

    public Boolean isRequestDepublication() {
        return requestDepublication;
    }

    public XPageActionContext setCopyAllowed(final Boolean copyAllowed) {
        this.copyAllowed = copyAllowed;
        return this;
    }

    public Boolean isCopyAllowed() {
        return copyAllowed;
    }

    public XPageActionContext setMoveAllowed(final Boolean moveAllowed) {
        this.moveAllowed = moveAllowed;
        return this;
    }

    public Boolean isMoveAllowed() {
        return moveAllowed;
    }

    public XPageActionContext setDeleteAllowed(final Boolean deleteAllowed) {
        this.deleteAllowed = deleteAllowed;
        return this;
    }

    public Boolean isDeleteAllowed() {
        return deleteAllowed;
    }
}
