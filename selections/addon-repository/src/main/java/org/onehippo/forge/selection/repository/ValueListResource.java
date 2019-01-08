/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.selection.repository;

import java.util.Locale;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.onehippo.forge.selection.repository.valuelist.ValueListService;
import org.onehippo.repository.jaxrs.api.SessionRequestContextProvider;

@Produces("application/json")
@Path("/")
public class ValueListResource {

    private static final CacheControl NO_CACHE = new CacheControl();

    static {
        NO_CACHE.setNoCache(true);
    }

    private final SessionRequestContextProvider sessionRequestContextProvider;

    public ValueListResource(final SessionRequestContextProvider userSessionProvider) {
        this.sessionRequestContextProvider = userSessionProvider;
    }

    @GET
    @Path("documents/{documentId}/{locale}")
    public Response getDocument(
            @PathParam("documentId") final String id,
            @PathParam("locale") final String locale,
            @Context final HttpServletRequest servletRequest) {
        return executeTask(servletRequest, Response.Status.OK, NO_CACHE,
                (session) -> ValueListService.get().getValueList(id, new Locale(locale), session)
        );
    }

    /**
     * Shared logic for providing the EndPointTask with contextual input and handling the packaging of its response
     * (which may be an error, encapsulated in an Exception).
     *
     * @param servletRequest current HTTP servlet request to derive contextual input
     * @param successStatus  HTTP status code in case of success
     * @param cacheControl   HTTP Cache-Control response header
     * @param task           the EndPointTask to execute
     * @return a JAX-RS response towards the client
     */
    private Response executeTask(final HttpServletRequest servletRequest,
                                 final Response.Status successStatus,
                                 final CacheControl cacheControl,
                                 final EndPointTask task) {
        final Session session = sessionRequestContextProvider.getJcrSession(servletRequest);
        try {
            final Object result = task.execute(session);
            return Response.status(successStatus).cacheControl(cacheControl).entity(result).build();
        } catch (final ErrorWithPayloadException e) {
            return Response.status(e.getStatus()).cacheControl(cacheControl).entity(e.getPayload()).build();
        }
    }

    @FunctionalInterface
    private interface EndPointTask {
        Object execute(Session session) throws ErrorWithPayloadException;
    }

}
