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

import com.google.common.base.Predicate;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;

public class ValidatorFactory {

    private static final Validator VOID_VALIDATOR = new Validator() {
        @Override
        public void validate(HstRequestContext requestContext) throws RuntimeException {
            // intentionally left blank
        }
    };

    public Validator getVoidValidator() {
        return VOID_VALIDATOR;
    }

    public Validator getChildExistsValidator(final String parentId, final String childId) {
        return new ChildExistsValidator(parentId, childId);
    }

    public Validator getNotNullValidator(final Object notNull, final ClientError clientError) {
        return new NotNullValidator(notNull, clientError);
    }

    public Validator getNodePathPrefixValidator(String nodePathPrefix, String id, String requiredNodeType) {
        return new NodePathPrefixValidator(nodePathPrefix, id, requiredNodeType);
    }

    public Validator getSiteMenuItemRepresentationValidator(Predicate<String> uriValidator, SiteMenuItemRepresentation representation) {
        return new SiteMenuItemRepresentationValidator(uriValidator, representation);
    }

    public Validator getCurrentPreviewConfigurationValidator(String id, SiteMapHelper siteMapHelper) {
        return new CurrentPreviewConfigurationValidator(id, siteMapHelper);
    }
}
