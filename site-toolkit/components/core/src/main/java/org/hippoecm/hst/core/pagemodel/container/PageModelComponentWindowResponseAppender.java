/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.pagemodel.container;

import org.hippoecm.hst.core.channelmanager.CmsComponentWindowResponseAppender;
import org.hippoecm.hst.core.component.HstRequest;

/**
 * Extending {@link CmsComponentWindowResponseAppender} to collect preamble and epilogue comment nodes
 * even in non-composor mode. By default, it collects those comment nodes only in preview mode.
 */
public class PageModelComponentWindowResponseAppender extends CmsComponentWindowResponseAppender {

    private boolean previewOnly = true;

    public PageModelComponentWindowResponseAppender() {
        super();
    }

    public boolean isPreviewOnly() {
        return previewOnly;
    }

    public void setPreviewOnly(boolean previewOnly) {
        this.previewOnly = previewOnly;
    }

    @Override
    protected boolean isApplicableRequest(HstRequest request) {
        return !isPreviewOnly() || request.getRequestContext().isPreview();
    }

    @Override
    protected boolean isComponentMetadataAppilcableRequest(HstRequest request) {
        return true;
    }
}
