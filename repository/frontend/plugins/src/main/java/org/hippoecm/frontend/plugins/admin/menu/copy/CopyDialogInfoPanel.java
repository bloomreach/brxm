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
package org.hippoecm.frontend.plugins.admin.menu.copy;

import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.dialog.lookup.InfoPanel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated use org.hippoecm.frontend.plugins.console.menu.* instead
 */
@Deprecated
class CopyDialogInfoPanel extends InfoPanel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(CopyDialogInfoPanel.class);

    private String target;
    private String name;

    CopyDialogInfoPanel(String id, JcrNodeModel model) {
        super(id, model);
        setOutputMarkupId(true);


        try {
            name = model.getNode().getName();
            add(new Label("source", model.getNode().getPath()));
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        add(new TextFieldWidget("name", new PropertyModel(this, "name")));
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
    
    protected String getName() {
        return name;
    }
    
    protected void setName(String name) {
        this.name = name;
    }
    
    protected String getTarget() {
        return target;
    }

    protected void setTarget(String target) {
        this.target = target;
    }

}