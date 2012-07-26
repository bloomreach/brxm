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
package org.hippoecm.editor.model;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNodeType;

/**
 * A namespace that unites multiple JCR namespaces by a "versioning" URI scheme.
 * There is a single location in the repository where these versions are stored.
 * <p>
 * The intention is for this class to be shared between editor frontend and workflow,
 * as a way to abstract the layout of type descriptor data.
 */
public class JcrNamespace {

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

    public String getPath() {
        return "/" + HippoNodeType.NAMESPACES_PATH + "/" + prefix;
    }

    public String getPrefix() {
        return prefix;
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
