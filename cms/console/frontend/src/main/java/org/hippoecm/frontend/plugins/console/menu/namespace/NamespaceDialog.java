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
package org.hippoecm.frontend.plugins.console.menu.namespace;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.util.XMLChar;
import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.widgets.RequiredTextFieldWidget;

public class NamespaceDialog extends AbstractDialog<Node> {

    private static final long serialVersionUID = 1L;

    private String prefix;
    private String uri;

    public NamespaceDialog() {
        add(setFocus(new RequiredTextFieldWidget("prefix", new PropertyModel<String>(this, "prefix"), new Model<String>("namespace prefix"))));
        add(new RequiredTextFieldWidget("uri", new PropertyModel<String>(this, "uri"), new Model<String>("namespace uri")));
    }

    @Override
    protected void handleSubmit() {
        if (!isValidPrefix(prefix)) {
            return;
        }

        try {
            final NamespaceRegistry namespaceRegistry = UserSession.get().getJcrSession().getWorkspace().getNamespaceRegistry();
            try {
                final String existingUri = namespaceRegistry.getURI(prefix);
                if (uri.equals(existingUri)) {
                    info("namespace mapping '" + prefix + "'" + " -> '" + uri + "' already exists");
                } else {
                    error("prefix '" + prefix + "' is already mapped to uri '" + existingUri + "'");
                }
            } catch (NamespaceException e) {
                try {
                    final String existingPrefix = namespaceRegistry.getPrefix(uri);
                    error("uri '" + uri + "' is already mapped by prefix '" + existingPrefix + "'");
                } catch (NamespaceException e1) {
                    namespaceRegistry.registerNamespace(prefix, uri);
                    closeDialog();
                }
            }
        } catch (RepositoryException e) {
            error("Failed to register namespace: " + e.getMessage());
        }
    }

    private boolean isValidPrefix(String prefix) {
        if (prefix.toLowerCase().startsWith("xml")) {
            error("prefix should not start with 'xml'");
            return false;
        }
        if (!XMLChar.isValidNCName(prefix)) {
            error("prefix contains invalid character");
            return false;
        }
        return true;
    }
    
    public IModel getTitle() {
        return new Model<String>("Add namespace");
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    
    @Override
    public IValueMap getProperties() {
        return DialogConstants.SMALL;
    }

}
