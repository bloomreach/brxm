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
package org.hippocms.repository.webapp.menu.node;

import javax.jcr.RepositoryException;

import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.model.PropertyModel;
import org.hippocms.repository.webapp.menu.AbstractDialogPage;
import org.hippocms.repository.webapp.model.JcrNodeModel;

public class NodeDialogPage extends AbstractDialogPage {
    private static final long serialVersionUID = 1L;

    private String name;
    private JcrNodeModel model;

    public NodeDialogPage(final NodeDialog dialog, JcrNodeModel model) {
        super(dialog);
        this.model = model;
        add(new AjaxEditableLabel("name", new PropertyModel(this, "name")));
    }

    public void ok() {
        NodeDialogPage page = (NodeDialogPage) getPage();
        try {
            model.getNode().addNode(page.getName());
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void cancel() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
