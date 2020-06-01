/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.repository.api.NodeNameCodec;

/**
 * Information on a type, based on the repository storage model of CMS types.
 * This class is not intended to be used by plugin developers; instead, use the
 * {@link ITypeDescriptor}.
 */
public final class JcrTypeInfo {

    private final Session session;
    private final JcrNamespace nsInfo;
    private final String typeName;

    public JcrTypeInfo(Session session, String type) throws RepositoryException {
        this.session = session;
        String prefix = "system";
        if (type.indexOf(':') > 0) {
            prefix = type.substring(0, type.indexOf(':'));
            typeName = NodeNameCodec.encode(type.substring(type.indexOf(':') + 1));
        } else {
            typeName = type;
        }
        nsInfo = new JcrNamespace(session, prefix);
    }

    public JcrTypeInfo(Node node) throws RepositoryException {
        this.session = node.getSession();
        this.typeName = node.getName();
        this.nsInfo = new JcrNamespace(session, node.getParent().getName());
    }
    
    public JcrNamespace getNamespace() {
        return nsInfo;
    }

    public String getPath() {
        return nsInfo.getPath() + "/" + getTypeName();
    }

    public JcrTypeVersion getDraft() throws RepositoryException {
        return new JcrTypeVersion(session, this, true, null);
    }

    public JcrTypeVersion getCurrent() throws RepositoryException {
        return new JcrTypeVersion(session, this, false, null);
    }

    public String getTypeName() {
        return typeName;
    }
}