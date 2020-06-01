/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jaxrs.swagger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;

import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.models.Swagger;

/**
 * HST specific Swagger API listing JAX-RS Resource.
 * <P>
 * This overrides the {@link Swagger}'s base path by resolving the currently resolved mount path.
 * </P>
 */
@Path("/swagger.{type:json|yaml}")
public class HstApiListingResource extends ApiListingResource {

    public HstApiListingResource() {
        super();
    }

    @Override
    protected Swagger process(Application app, ServletContext servletContext, ServletConfig sc, HttpHeaders headers,
            UriInfo uriInfo) {
        final Swagger swagger = super.process(app, servletContext, sc, headers, uriInfo);

        final HstRequestContext requestContext = RequestContextProvider.get();
        HstLink hstLink = requestContext.getHstLinkCreator().create("/", requestContext.getResolvedMount().getMount());
        swagger.setBasePath(hstLink.toUrlForm(requestContext, false));

        return swagger;
    }
}
