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
import org.hippoecm.hst.pagecomposer.jaxrs.model.LinkType;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

import static org.hippoecm.hst.pagecomposer.jaxrs.model.LinkType.EXTERNAL;
import static org.hippoecm.hst.pagecomposer.jaxrs.model.LinkType.SITEMAPITEM;

public class SiteMenuItemRepresentationValidator implements Validator {

    private final Predicate<String> uriValidator;
    private final SiteMenuItemRepresentation representation;

    public SiteMenuItemRepresentationValidator(Predicate<String> uriValidator, SiteMenuItemRepresentation representation) {
        this.uriValidator = uriValidator;
        this.representation = representation;
    }

    @Override
    public void validate(HstRequestContext requestContext) throws RuntimeException {
        final LinkType linkType = representation.getLinkType();
        final String link = representation.getLink();
        if (linkType == EXTERNAL && !uriValidator.apply(link)) {
            throw new ClientException(link + " is not valid", ClientError.INVALID_URL);
        } else if (linkType == SITEMAPITEM) {
            new PathInfoValidator(link).validate(requestContext);
        }
    }
}
