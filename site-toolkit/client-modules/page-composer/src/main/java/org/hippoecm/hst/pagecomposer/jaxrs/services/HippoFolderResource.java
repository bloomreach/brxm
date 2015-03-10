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

import javax.jcr.Node;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.pagecomposer.jaxrs.model.HippoDocumentRepresentation;
import org.hippoecm.repository.api.HippoNodeType;

@Path("/"+ HippoNodeType.NT_DOCUMENT+"/")
@Produces(MediaType.APPLICATION_JSON)
public class HippoFolderResource extends AbstractConfigResource {

    @GET
    @Path("/")
    public Response get() {
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                HippoDocumentRepresentation representation = new HippoDocumentRepresentation(getPageComposerContextService());
                return ok("Folder loaded successfully", representation);
            }
        });
    }

    /**
     * @param pathInfo
     * @return the rest response to create the client tree for <code>pathInfo</code> : the response contains all
     * ancestor nodes + their siblings up to the 'channel content root node' plus the siblings for <code>pathInfo</code>
     * plus its direct children.
     */
    @GET
    @Path("{pathInfo}")
    public Response getSubTree(final @PathParam("pathInfo") String pathInfo) {
        return tryGet(new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                HippoDocumentRepresentation representation = new HippoDocumentRepresentation()
                        .represent(getPageComposerContextService(), pathInfo);
                return ok("Folder loaded successfully", representation);
            }
        });
    }
}
