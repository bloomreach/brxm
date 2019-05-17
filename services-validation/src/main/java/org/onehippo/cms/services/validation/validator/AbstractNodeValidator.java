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

import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.ValidationContextException;
import org.onehippo.cms.services.validation.api.Validator;
import org.onehippo.cms.services.validation.api.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNodeValidator implements Validator<Node> {

    private static final Logger log = LoggerFactory.getLogger(AbstractNodeValidator.class);
    private static final String TYPE_ERROR = "Cannot validate node '%s'. It must be of type '%s'";
    private static final String REPOSITORY_ERROR = "Cannot validate node type %s.";

    @Override
    public Optional<Violation> validate(final ValidationContext context, final Node node) {
        
        if (node == null) {
            return Optional.of(context.createViolation());
        }

        final String checkedNodeType = getCheckedNodeType();
        try {
            if (!node.isNodeType(checkedNodeType)) {
                throw new ValidationContextException(String.format(TYPE_ERROR, node.getPath(), checkedNodeType));
            }
            return checkNode(context, node);
        } catch (RepositoryException e) {
            log.warn(String.format(REPOSITORY_ERROR, checkedNodeType), e);
        }
        return Optional.empty();
    }
    
    protected abstract String getCheckedNodeType();
    
    protected abstract Optional<Violation> checkNode(ValidationContext context, Node node) throws RepositoryException;  
}
