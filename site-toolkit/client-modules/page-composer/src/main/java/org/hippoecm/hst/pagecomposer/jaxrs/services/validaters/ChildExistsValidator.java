/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.validaters;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;

public class ChildExistsValidator implements Validator {

    final String parentId;
    final String childId;

    public ChildExistsValidator(final String parentId, final String childId) {
        this.parentId = parentId;
        this.childId = childId;
    }

    @Override
    public void validate() throws RuntimeException {
        try {
            HstRequestContext requestContext = RequestContextProvider.get();
            final Session session = requestContext.getSession();
            final Node parent = session.getNodeByIdentifier(parentId);
            final Node child = session.getNodeByIdentifier(childId);

            if (!parent.isSame(child.getParent())) {
                throw new IllegalArgumentException("Node '"+child.getPath()+"' is not a child of '"+parent.getPath()+"'");
            }

        } catch (ItemNotFoundException e) {
            throw new IllegalStateException("No repository configuration node for parent or child found : " + e.toString());
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException("RepositoryException during pre-validate", e);
        }



    }

}
