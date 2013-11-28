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
package org.hippoecm.frontend.plugins.console.menu.copy;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.tree.IJcrTreeNode;
import org.hippoecm.frontend.model.tree.JcrTreeNode;
import org.hippoecm.frontend.plugins.console.dialog.LookupDialog;
import org.hippoecm.frontend.plugins.console.menu.t9ids.GenerateNewTranslationIdsVisitor;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.LabelledBooleanFieldWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyDialog extends LookupDialog {
    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(CopyDialog.class);

    private static final IValueMap SIZE = new ValueMap("width=515,height=540");
    
    private String name;
    private Boolean generate = true;
    @SuppressWarnings("unused")
    private String target;
    private Label targetLabel;
    private final IModelReference<Node> modelReference;

    public CopyDialog(IModelReference<Node> modelReference) {
        super(new JcrTreeNode(new JcrNodeModel("/"), null), modelReference.getModel());
        this.modelReference = modelReference;
        JcrNodeModel model = (JcrNodeModel) modelReference.getModel();
        setSelectedNode(model);

        try {
            if (model.getParentModel() != null) {
                setSelectedNode(model.getParentModel());

                add(new Label("source", model.getNode().getPath()));

                target = StringUtils.substringBeforeLast(model.getNode().getPath(), "/") + "/";
                targetLabel = new Label("target", new PropertyModel(this, "target"));
                targetLabel.setOutputMarkupId(true);
                add(targetLabel);

                name = model.getNode().getName();
                TextFieldWidget nameField = new TextFieldWidget("name", new PropertyModel<String>(this, "name"));
                nameField.setSize(String.valueOf(name.length() + 5));
                add(nameField);
                
                LabelledBooleanFieldWidget checkbox = new LabelledBooleanFieldWidget("generate", 
                        new PropertyModel<Boolean>(this, "generate"), 
                        new Model<String>("Generate new translation ids"));
                add(checkbox);
            } else {
                add(new Label("source", "Cannot copy the root node"));
                add(new EmptyPanel("target"));
                add(new EmptyPanel("name"));
                add(new EmptyPanel("generate"));
                setOkVisible(false);
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            add(new Label("source", e.getClass().getName()));
            add(new Label("target", e.getMessage()));
            add(new EmptyPanel("name"));
            setOkVisible(false);
        }
        setFocusOnCancel();
    }

    public IModel<String> getTitle() {
        return new Model<String>("Copy Node");
    }
        
    @Override
    public void onSelect(IModel<Node> model) {
        if (model != null) {
            try {
                target = model.getObject().getPath() + "/";
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        AjaxRequestTarget requestTarget = RequestCycle.get().find(AjaxRequestTarget.class);
        if (requestTarget != null) {
            requestTarget.add(targetLabel);
        }
    }

    @Override
    protected boolean isValidSelection(IJcrTreeNode targetModel) {
        return true;
    }

    @Override
    public void onOk() {
        try {
            IModel<Node> nodeModel = getOriginalModel();

            IModel<Node> selectedNode = getSelectedNode().getNodeModel();
            if (selectedNode != null && name != null && !"".equals(name)) {
                IModel<Node> targetNodeModel = getSelectedNode().getNodeModel();
                String targetPath = targetNodeModel.getObject().getPath();
                if (!targetPath.endsWith("/")) {
                    targetPath += "/";
                }
                targetPath += name;

                // The actual copy
                UserSession wicketSession = UserSession.get();
                HippoSession jcrSession = (HippoSession) wicketSession.getJcrSession();
                jcrSession.copy(nodeModel.getObject(), targetPath);

                Node rootNode = nodeModel.getObject().getSession().getRootNode();
                Node targetNode = rootNode.getNode(targetPath.substring(1));
                
                if (generate) {
                    targetNode.accept(new GenerateNewTranslationIdsVisitor());
                }
                
                modelReference.setModel(new JcrNodeModel(targetNode));
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            error(ex.getMessage());
        }
    }

    @Override
    public IValueMap getProperties() {
        return SIZE;
    }

}
