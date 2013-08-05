/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.ckeditor.dialog.model;

import java.util.Map;

import org.apache.wicket.model.IDetachable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DocumentLink extends CKEditorLink {
    private static final long serialVersionUID = 1L;

    
    protected static final Logger log = LoggerFactory.getLogger(DocumentLink.class);
    
    private IDetachable initialModel;
    private IDetachable selectedModel;
    private String initType;

    public DocumentLink(Map<String, String> values, IDetachable model) {
        super(values);

        initialModel = selectedModel = model;
    }

    @Override
    public boolean hasChanged() {
        return !selectedModel.equals(initialModel) || super.hasChanged();
    }

    @Override
    public boolean isExisting() {
        return initialModel != null;
    }

    public IDetachable getInitialModel() {
        return initialModel;
    }

    public boolean isReplacing() {
        if (selectedModel != null && initialModel != null && !selectedModel.equals(initialModel)) {
            return true;
        }
        return false;
    }

    public boolean isAttacheable() {
        return initialModel == null || isReplacing();
    }

    public IDetachable getLinkTarget() {
        return selectedModel;
    }

    public void setLinkTarget(IDetachable model) {
        this.selectedModel = model;
    }

    public void setInitType(String type) {
        this.initType = type;
    }

    public boolean isSameType(String type) {
        return this.initType != null && this.initType.endsWith(type);
    }

}
