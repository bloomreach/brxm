/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.plugins.console.menu.open;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.console.NodeModelReference;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.RequiredTextFieldWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class OpenDialog extends AbstractDialog<Node> {

    private static final long serialVersionUID = 1L;
    private String pathOrId;
    private TextFieldWidget tf;
    private NodeModelReference modelReference;

    public OpenDialog(final NodeModelReference modelReference) {
        this.modelReference = modelReference;

        IModel<String> labelModel = new Model<String>("Path/UUID");
        add(new Label("label", labelModel));
        add(tf = new RequiredTextFieldWidget("pathOrId", new PropertyModel<String>(this, "pathOrId"), labelModel));
        tf.setSize("85");
        setFocus(tf);
    }

    @Override
    protected void onOk() {
        pathOrId = pathOrId.trim();
        Session jcrSession = UserSession.get().getJcrSession();
        Node selected = null;
        try {
            if(pathOrId.startsWith("/")) {
                if(jcrSession.nodeExists(pathOrId)) {
                    selected = jcrSession.getNode(pathOrId);
                }
            } else {
                final Node parentNode = modelReference.getModel().getObject();
                if (parentNode.hasNode(pathOrId)) {
                    selected = parentNode.getNode(pathOrId);
                } else {
                    selected = jcrSession.getNodeByIdentifier(pathOrId);
                }
            }
        } catch (RepositoryException e) {
            //ignore
        }

        if(selected ==  null) {
            error("Node was not found, please try again.");
            setFocus(tf);
        } else {
            modelReference.setModel(new JcrNodeModel(selected));
        }
    }

    @Override
    public IModel getTitle() {
        return new Model<String>("Open node by path or id");
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=640,height=200").makeImmutable();
    }
}
