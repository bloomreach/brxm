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
package org.hippoecm.editor.type;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.types.ITypeDescriptor;

/**
 * JCR type store using Wicket. The JCR session is retrieved from the Wicket session, and the creates types
 * are validated when Wicket is run in development mode.
 */
public class JcrTypeStore extends AbstractJcrTypeStore implements IDetachable {

    private static final long serialVersionUID = 1L;

    @Override
    public void detach() {
        for (ITypeDescriptor type : types.values()) {
            if (type instanceof IDetachable) {
                ((IDetachable) type).detach();
            }
        }
        for (ITypeDescriptor type : currentTypes.values()) {
            if (type instanceof IDetachable) {
                ((IDetachable) type).detach();
            }
        }
    }

    /**
     * Validates the creates types when Wicket is run in development mode.
     * @param typeNode the type node
     * @return the JCR type descriptor
     * @throws RepositoryException
     */
    protected JcrTypeDescriptor createJcrTypeDescriptor(Node typeNode) throws RepositoryException {
        JcrTypeDescriptor result = super.createJcrTypeDescriptor(typeNode);

        if (result != null && WebApplication.get() != null && "development".equals(WebApplication.get().getConfigurationType())) {
            result.validate();
        }

        return result;
    }

    /**
     * Retrieves the JCR session from the Wicket session.
     */
    protected Session getJcrSession() {
        return UserSession.get().getJcrSession();
    }

}
