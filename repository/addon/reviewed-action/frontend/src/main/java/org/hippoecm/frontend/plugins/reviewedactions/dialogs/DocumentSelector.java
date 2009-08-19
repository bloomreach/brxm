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
package org.hippoecm.frontend.plugins.reviewedactions.dialogs;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;

public class DocumentSelector extends AbstractNodeRenderer {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private final List<JcrNodeModel> selectedDocuments;

    public DocumentSelector(List<JcrNodeModel> selectedDocuments) {
        this.selectedDocuments = selectedDocuments;
    }

    @Override
    protected Component getViewer(String id, Node node) throws RepositoryException {
        return new CheckBoxWrapper(id, node);
    }

    private class CheckBoxWrapper extends Panel {
        private static final long serialVersionUID = 1L;

        public CheckBoxWrapper(String id, Node node) {
            super(id);
            final JcrNodeModel nodeModel = new JcrNodeModel(node);
            CheckBox check;
            add(check = new CheckBox("check", new IModel() {
                private static final long serialVersionUID = 1L;

                public Object getObject() {
                    return selectedDocuments.contains(nodeModel);
                }

                public void setObject(Object object) {
                    Boolean value = (Boolean) object;
                    if (value.booleanValue()) {
                        selectedDocuments.add(nodeModel);
                    } else {
                        selectedDocuments.remove(nodeModel);
                    }
                }

                public void detach() {
                    nodeModel.detach();
                }

            }));
            check.add(new AjaxFormComponentUpdatingBehavior("onclick") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(CheckBoxWrapper.this);
                }
            });
            setOutputMarkupId(true);
        }

    }

}
