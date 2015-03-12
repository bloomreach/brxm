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
package org.hippoecm.hst.pagecomposer.jaxrs.services;


import java.util.concurrent.Callable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.pagecomposer.jaxrs.model.TreePickerRepresentation;
import org.hippoecm.repository.api.HippoNodeType;

@Path("/"+ HippoNodeType.NT_DOCUMENT+"/")
@Produces(MediaType.APPLICATION_JSON)
public class HippoDocumentResource extends AbstractConfigResource {

    /**
     * @param siteMapPathInfo
     * @return the rest response to create the client tree for <code>siteMapPathInfo</code> : the response contains all
     * ancestor nodes + their siblings up to the 'channel content root node' plus the siblings for <code>siteMapPathInfo</code>
     * plus its direct children.
     */
    @GET
    @Path("{siteMapPathInfo: .*}")
    public Response get(final @PathParam("siteMapPathInfo") String siteMapPathInfo) {
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                final TreePickerRepresentation representation;
                if (StringUtils.isEmpty(siteMapPathInfo)) {
                    representation = new TreePickerRepresentation().representRequestConfigNode(getPageComposerContextService());

                } else {
                    representation  = new TreePickerRepresentation()
                            .representExpandedParentTree(getPageComposerContextService(), siteMapPathInfo);
                }
                return ok("Folder loaded successfully", representation);
            }
        });
    }
}
