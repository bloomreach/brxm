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
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.repository.util.JcrConstants;

/**
 * Validates that a Resource node contains an uploaded resource.
 */
public class RequiredResourceValidator extends AbstractNodeValidator {

    @Override
    protected String getCheckedNodeType() {
        return HippoNodeType.NT_RESOURCE;
    }

    @Override
    protected Optional<Violation> checkNode(final  ValidationContext context, final Node node) 
            throws RepositoryException {
        
        final Property resourceData = node.getProperty(JcrConstants.JCR_DATA);
        if (resourceData.getLength() <= 0) {
            return Optional.of(context.createViolation());
        }
        return Optional.empty();
    }
}
