/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.validators;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.util.NodeIterable;

public class PrototypePageValidator implements Validator {

    private final String prototypePageUuid;

    public PrototypePageValidator(String prototypePageUuid) {
        this.prototypePageUuid = prototypePageUuid;
    }

    @Override
    public void validate(HstRequestContext requestContext) throws RuntimeException {
        try {
            validatePrototypePage(requestContext.getSession().getNodeByIdentifier(prototypePageUuid));
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }


    private static void validatePrototypePage(Node component) throws RepositoryException {
        // TODO when implementing re-applying a prototype to a page, ensure the page is from the workspace!
        if (!component.isNodeType(HstNodeTypes.NODETYPE_HST_ABSTRACT_COMPONENT)) {
            throw new ClientException("Expected node of subtype 'hst:abstractcomponent'", ClientError.INVALID_NODE_TYPE);
        }
        if (component.isNodeType(HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT)) {
            String message = String.format("Prototype page is not allowed to contain nodes of type '%s' but there is one " +
                    "at '%s'. Prototype page cannot be used", HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT, component.getPath());
            throw new ClientException(message, ClientError.INVALID_NODE_TYPE);
        }
        for (Node child : new NodeIterable(component.getNodes())) {
            validatePrototypePage(child);
        }
    }
}
