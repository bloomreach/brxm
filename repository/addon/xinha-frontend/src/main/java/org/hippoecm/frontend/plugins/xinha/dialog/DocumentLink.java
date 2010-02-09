/*
 *  Copyright 2008 Hippo.
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

package org.hippoecm.frontend.plugins.xinha.dialog;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.xinha.services.links.XinhaLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DocumentLink extends XinhaLink {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    protected static final Logger log = LoggerFactory.getLogger(DocumentLink.class);
    
    private JcrNodeModel initialModel;
    private JcrNodeModel selectedModel;

    public DocumentLink(Map<String, String> values, JcrNodeModel parentModel) {
        super(values);

        initialModel = selectedModel = createInitialModel(parentModel);
    }

    protected abstract JcrNodeModel createInitialModel(JcrNodeModel parentModel);

    @Override
    public boolean isValid() {
        if (selectedModel == null) {
            return false;
        }
        Node node = selectedModel.getObject();
        if (node == null) {
            return false;
        }
        try {
            if (!node.isNodeType("mix:referenceable")) {
                return false;
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public boolean hasChanged() {
        if (selectedModel.equals(initialModel)) {
            return super.hasChanged();
        }
        return true;
    }

    @Override
    public boolean isExisting() {
        return initialModel != null;
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

    public JcrNodeModel getNodeModel() {
        return selectedModel;
    }

    public void setNodeModel(JcrNodeModel model) {
        this.selectedModel = model;
    }
}
