/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.util;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.pagecomposer.jaxrs.model.DocumentRepresentation;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class DocumentUtils {

    /**
     *
     * @throws java.lang.IllegalArgumentException is <code>absPath</code> does not start with <code>mount.getContentPath() + '/'</code>
     * @throws RuntimeRepositoryException in case some repository exception happens
     */
    public static DocumentRepresentation getDocumentRepresentationHstConfigUser(final String absPath, final String rootContentPath) {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
        Credentials configUser = HstServices.getComponentManager().getComponent(Credentials.class.getName() + ".hstconfigreader");
        Session session = null;

        try {
            // pooled hst config user session which has in general read access everywhere
            session = repository.login(configUser);
            Node node = session.getNode(absPath);
            final boolean isDocument;
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                isDocument = true;
            }else if (!node.isSame(node.getSession().getRootNode()) &&
                     node.isNodeType(HippoNodeType.NT_DOCUMENT) &&
                    node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                isDocument = true;
            } else {
                isDocument = false;
            }
            String displayName = ((HippoNode)node).getLocalizedName();
            return new DocumentRepresentation(node.getPath(), rootContentPath, displayName, isDocument, true);
        } catch (PathNotFoundException e) {
           return new DocumentRepresentation(absPath, rootContentPath, null, false, false);
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException("Could not obtain hst config user session", e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }
    }

}
