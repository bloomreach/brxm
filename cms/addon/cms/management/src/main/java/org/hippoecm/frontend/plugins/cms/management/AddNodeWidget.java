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
package org.hippoecm.frontend.plugins.cms.management;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.AjaxEditableLabel;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AddNodeWidget extends AjaxEditableLabel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(AddNodeWidget.class);

    private final JcrNodeModel parentNodeModel;
    private String label;
    private String nodeType;

    public AddNodeWidget(String id, IModel model, JcrNodeModel parentNodeModel, String nodeType) {
        super(id, model);
        this.parentNodeModel = parentNodeModel;
        this.nodeType = nodeType;
    }

    @Override
    protected void onDetach() {
        parentNodeModel.detach();
        super.onDetach();
    }

    @Override
    protected void onEdit(AjaxRequestTarget target) {
        label = (String) getModel().getObject();
        setModel(new Model(""));
        super.onEdit(target);
    }

    @Override
    protected Component newLabel(MarkupContainer parent, String componentId, IModel model)
    {
        Button but = new Button(componentId, model){
            private static final long serialVersionUID = 1L;

            @Override
            public IConverter getConverter(Class type) {
                IConverter c = AddNodeWidget.this.getConverter(type);
                return c != null ? c : super.getConverter(type);
            }
        };
        but.setOutputMarkupId(true);
        but.add(new LabelAjaxBehavior(getLabelAjaxEvent()));
        return but;
    }

    @Override
    protected void onCancel(AjaxRequestTarget target) {
        setModel(new Model(label));
        super.onCancel(target);
    }

    //TODO: implement decent exception handling with feedback panel and logger
    @Override
    protected void onSubmit(AjaxRequestTarget target) {
        super.onSubmit(target);

        // FIXME: use FolderWorkflow
        String nodeName = (String) getModel().getObject();
        try {
            Node node = null;
            if (parentNodeModel.getNode().hasNode(nodeName)) {
                node = parentNodeModel.getNode().getNode(nodeName);
            } else {
                String path = parentNodeModel.getNode().getPath() + "/" + nodeName;
                String nodeTypeAsPath = nodeType.replace(':', '/');
                String prototypePath = "/hippo:namespaces/" + nodeTypeAsPath
                        + "/hippo:prototype/hippo:prototype";
                Node prototype = parentNodeModel.getNode().getSession().getRootNode().getNode(
                        prototypePath.substring(1));
                node = ((HippoSession) parentNodeModel.getNode().getSession()).copy(prototype, path);
            }
            onAddNode(target, node);
        } catch (RepositoryException e) {
            log.error("An error occurred while trying to create or select node[" + nodeName + "]", e);
        }

        setModel(new Model(label));
    }

    protected abstract void onAddNode(AjaxRequestTarget target, Node node);
}
