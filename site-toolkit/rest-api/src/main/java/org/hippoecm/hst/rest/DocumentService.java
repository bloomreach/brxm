/*
*  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
     * Returns information about all <b>preview</b> channels a document is part of. A document is identified by its UUID.
     * When a document is unknown or not part of any channel, an empty list is returned.
     *
     * @param uuid the identifier of the document
     *
     * @return a list of 'channel documents' that provide information about all channels the document is part of,
     * or an empty list if the identifier is unknown or the document is not part of any channel.
     *
     * @throws javax.ws.rs.WebApplicationException when the client-side invocation of this service fails
     */
    @GET
    @Path("/{uuid}/channels/")
    @Produces(MediaType.APPLICATION_JSON)
    List<ChannelDocument> getChannels(@PathParam("uuid") String uuid);

    /**
     * Returns a fully qualified URL in SITE context for a document in a mount of a certain type. The document is identified by its UUID.
     * When the type parameter is null or empty, the value 'live' is used. Note that this method thus returns a fully qualified
     * URL for the host through which the site(s) are visited, and not through the cms host, which can be a different host
     *
     * Note that only one link is returned, even when the document is available in multiple channels (i.e. under
     * multiple mounts). When multiple mounts match, we use the one that has the closest canonical content path to the
     * path of the document handle. If multiple mounts have an equally well suited* canonical content path, we use the
     * mount with the fewest types. These mounts are in general the most generic ones. If multiple mounts have an
     * equally well suited canonical content path and an equal number of types, we use a random one.
     *
     * @param uuid the identifier of the document
     * @param type the type of the mounts that can be used to generate a link to the document. When null or empty,
     * the value 'live' is used.
     *
     * @return a fully qualified link to the document, or an empty string if no link could be created.
     *
     * @throws javax.ws.rs.WebApplicationException when the client-side invocation of this service fails
     */
    @GET
    @Path("/{uuid}/url/{type}/")
    @Produces(MediaType.TEXT_PLAIN)
    String getUrl(@PathParam("uuid") String uuid, @PathParam("type") String type);

}
