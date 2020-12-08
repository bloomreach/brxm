/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.model;

import java.util.Map;

import javax.jcr.Node;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RichTextEditorDocumentLink extends RichTextEditorLink {

    protected static final Logger log = LoggerFactory.getLogger(RichTextEditorDocumentLink.class);
    
    private IModel<Node> initialModel;
    private IModel<Node> selectedModel;
    private String initType;

    public RichTextEditorDocumentLink(Map<String, String> values, IModel<Node> model) {
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

    public IModel<Node> getLinkTarget() {
        return selectedModel;
    }

    public void setLinkTarget(IModel<Node> model) {
        this.selectedModel = model;
    }

    public void setInitType(String type) {
        this.initType = type;
    }

    public boolean isSameType(String type) {
        return this.initType != null && this.initType.endsWith(type);
    }

}
