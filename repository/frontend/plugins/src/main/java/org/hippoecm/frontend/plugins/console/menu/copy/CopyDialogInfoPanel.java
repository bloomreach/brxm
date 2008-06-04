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
package org.hippoecm.frontend.plugins.console.menu.copy;

import javax.jcr.RepositoryException;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CopyDialogInfoPanel extends Panel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(CopyDialogInfoPanel.class);

    @SuppressWarnings("unused")
    private String source;
    @SuppressWarnings("unused")
    private String target;

    CopyDialogInfoPanel(String id, JcrNodeModel model) {
        super(id, model);
        setOutputMarkupId(true);

        try {
            source = model.getNode().getPath();
            target = model.getNode().getPath();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        add(new Label("source", new PropertyModel(this, "source")));
        add(new Label("target", new PropertyModel(this, "target")));
    }
    
    @Override
    public void onModelChanged() {
        JcrNodeModel model = (JcrNodeModel)getModel();
        if (model != null) {
            try {
                this.target = model.getNode().getPath();
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        IRequestTarget requestTarget = RequestCycle.get().getRequestTarget();
        if (AjaxRequestTarget.class.isAssignableFrom(requestTarget.getClass())) {
            ((AjaxRequestTarget)requestTarget).addComponent(this);
        }
    }
}