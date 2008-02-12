/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.admin.linkpicker;

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.dialog.lookup.InfoPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LinkPickerDialogInfoPanel extends InfoPanel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LinkPickerDialogInfoPanel.class);
 
    private String target;

    LinkPickerDialogInfoPanel(String id, JcrNodeModel model) {
        super(id, model);
        setOutputMarkupId(true);
        
        add(new Label("target", new PropertyModel(this, "target")));
    }
    
    @Override
    public void update(AjaxRequestTarget target, JcrNodeModel model) {
        if (model != null) {
            try {
                this.setTarget(model.getNode().getPath());
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        if (target != null) {
            target.addComponent(this);
        }
    }
    
    String getTarget() {
        return target;
    }

    void setTarget(String target) {
        this.target = target;
    }

}