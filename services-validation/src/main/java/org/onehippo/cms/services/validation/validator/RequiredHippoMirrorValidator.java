/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms.services.validation.validator;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Violation;

/**
 * Validates that a Hippo Mirror node contains an valid reference.
 */
public class RequiredHippoMirrorValidator extends AbstractNodeValidator {

    private final NodeReferenceValidator nodeReferenceValidator;

    public RequiredHippoMirrorValidator() {
        nodeReferenceValidator = new NodeReferenceValidator();
    }

    @Override
    protected String getCheckedNodeType() {
        return HippoNodeType.NT_MIRROR;
    }

    @Override
    protected Optional<Violation> checkNode(final ValidationContext context, final Node node) 
            throws RepositoryException {
        
        final String docBase = node.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
        return nodeReferenceValidator.validate(context, docBase);
    }
}
