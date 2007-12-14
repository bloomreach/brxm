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
package org.hippoecm.frontend.plugins.admin.menu.move;

import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveDialogInfoPanel extends Panel {
    private static final long serialVersionUID = 1L;
    
    static final Logger log = LoggerFactory.getLogger(MoveDialogInfoPanel.class);
    
    private String destinationPath;

    public MoveDialogInfoPanel(String id, JcrNodeModel model) {
        super(id, model);
        setOutputMarkupId(true);

        try {
            add(new Label("source", model.getNode().getPath()));
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        add(new Label("destination", new PropertyModel(this, "destinationPath")));
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

}