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
package org.hippoecm.frontend.plugins.console.menu.namespace;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.plugins.console.menu.MenuPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.RequiredTextFieldWidget;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceDialog  extends AbstractDialog<Node> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(NamespaceDialog.class);

    private String namespaceName;
    private String namespaceUri;

    public NamespaceDialog(NamespacePlugin plugin) {
        add(setFocus(new RequiredTextFieldWidget("name", new PropertyModel(this, "namespaceName"), new Model("namespace name"))));
        add(new RequiredTextFieldWidget("uri", new PropertyModel(this, "namespaceUri"), new Model("namespace url")));
    }

    @Override
    protected void onOk() {
        if (namespaceName == null) {
            error("Namespace name cannot be empty.");
        }
        if (namespaceUri == null) {
            error("Namespace uri cannot be empty.");
        }
        log.info("Trying to add namespace initialization node: " + namespaceName + " => " + namespaceUri);

        // TODO: add some basic checking on namespace name and uri

        // create initialize node
        try {
            Node rootNode = ((UserSession) Session.get()).getJcrSession().getRootNode();
            Node initNode = rootNode.getNode(HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.INITIALIZE_PATH);
            if (initNode.hasNode(namespaceName)) {
                initNode.getNode(namespaceName).remove();
            }
            Node node = initNode.addNode(namespaceName, HippoNodeType.NT_INITIALIZEITEM);
            node.setProperty(HippoNodeType.HIPPO_NAMESPACE, namespaceUri);
            rootNode.getSession().save();
            log.info("Added namespace initialization node: " + namespaceName + " => " + namespaceUri);

        } catch (RepositoryException e) {
            log.error("Error while creating namespace initialization node: ", e);
            throw new RuntimeException(e.getMessage());
        }

    }
    
    public IModel getTitle() {
        return new Model("Add namespace");
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public void setNamespaceName(String namespaceName) {
        this.namespaceName = namespaceName;
    }

    public String getNamespaceUri() {
        return namespaceUri;
    }

    public void setNamespaceUri(String namespaceUri) {
        this.namespaceUri = namespaceUri;
    }
    
    @Override
    public IValueMap getProperties() {
        return SMALL;
    }
}
