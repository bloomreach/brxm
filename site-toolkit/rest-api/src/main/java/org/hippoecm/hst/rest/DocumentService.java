/*
*  Copyright 2011 Hippo.
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
package org.hippoecm.hst.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hippoecm.hst.rest.beans.ChannelDocument;

/**
 * JaxRS service that returns information about documents.
 */
@Path("/documents/")
public interface DocumentService {

    /**
     * Returns information about all channels a document is part of. A document is identified by its UUID.
     * When a document is unknown or not part of any channel, an empty list is returned.
     *
     * @param uuid the identifier of the document
     *
     * @return a list of 'channel documents' that provide information about all channels the document is part of,
     * or an empty list if the identifier is unknown or the document is not part of any channel.
     */
    @GET
    @Path("/{uuid}/channels/")
    @Produces(MediaType.APPLICATION_JSON)
    List<ChannelDocument> getChannels(@PathParam("uuid") String uuid);

}
