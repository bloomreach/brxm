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
package org.hippoecm.editor.tools;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.TypeLocator;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;

/**
 * A namespace that unites multiple JCR namespaces by a "versioning" URI scheme.
 * There is a single location in the repository where these versions are stored.
 * <p>
 * The intention is for this class to be shared between editor frontend and workflow,
 * as a way to abstract the layout of type descriptor data.
 */
public class JcrNamespace {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private String prefix;
    private Session session;

    public JcrNamespace(Session session, String prefix) throws RepositoryException {
        this.session = session;

        String uri = getUri(prefix);
        String nsVersion = "_" + uri.substring(uri.lastIndexOf("/") + 1).replace('.', '_');
        if (prefix.length() > nsVersion.length()
                && nsVersion.equals(prefix.substring(prefix.length() - nsVersion.length()))) {
            prefix = prefix.substring(0, prefix.length() - nsVersion.length());
        }

        this.prefix = prefix;
    }

    private Session getJcrSession() {
        return session;
    }

    public Map<String, TypeUpdate> getUpdate(TypeLocator locator) throws RepositoryException, StoreException {
        HippoSession session = (HippoSession) getJcrSession();
        Map<String, TypeUpdate> result = new HashMap<String, TypeUpdate>();

        String uri = getCurrentUri();
        NodeIterator iter = ((Node) session.getItem(getPath())).getNodes();
        while (iter.hasNext()) {
            Node typeNode = iter.nextNode();

            Node draft = null, current = null;
            NodeIterator versions = typeNode.getNodes(HippoNodeType.HIPPOSYSEDIT_NODETYPE + "/" + HippoNodeType.HIPPOSYSEDIT_NODETYPE);
            while (versions.hasNext()) {
                Node node = versions.nextNode();
                if (!node.isNodeType(HippoNodeType.NT_REMODEL)) {
                    draft = node;
                } else {
                    if (node.getProperty(HippoNodeType.HIPPO_URI).getString().equals(uri)) {
                        current = node;
                    }
                }
            }

            if (current != null) {
                ITypeDescriptor currentType = new JcrTypeDescriptor(new JcrNodeModel(current), locator);
                ITypeDescriptor draftType = draft == null ? null : new JcrTypeDescriptor(new JcrNodeModel(draft),
                        locator);
                result.put(typeNode.getName(), new TypeConversion(locator, currentType, draftType)
                        .getTypeUpdate());
            }
        }
        return result;
    }

    public String getPath() {
        return "/" + HippoNodeType.NAMESPACES_PATH + "/" + prefix;
    }

    public String getCurrentUri() throws RepositoryException {
        return getUri(prefix);
    }

    private String getUri(String prefix) throws RepositoryException {
        if ("system".equals(prefix)) {
            return "internal";
        }
        NamespaceRegistry nsReg = getJcrSession().getWorkspace().getNamespaceRegistry();
        return nsReg.getURI(prefix);
    }
}
