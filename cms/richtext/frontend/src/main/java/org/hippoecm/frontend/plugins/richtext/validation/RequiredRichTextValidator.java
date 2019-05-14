/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.richtext.validation;

import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.onehippo.cms.services.validation.api.ValidationContext;
import org.onehippo.cms.services.validation.api.Violation;
import org.onehippo.cms.services.validation.api.internal.HtmlUtils;
import org.onehippo.cms.services.validation.validator.AbstractNodeValidator;

public class RequiredRichTextValidator extends AbstractNodeValidator {

    @Override
    protected String getCheckedNodeType() {
        return HippoStdNodeType.NT_HTML;
    }

    @Override
    protected Optional<Violation> checkNode(final ValidationContext context, final Node node) throws RepositoryException {

        final Property property = node.getProperty(HippoStdNodeType.HIPPOSTD_CONTENT);
        if (HtmlUtils.isEmpty(property.getString())) {
            return Optional.of(context.createViolation());
        }

        return Optional.empty();
    }
}
