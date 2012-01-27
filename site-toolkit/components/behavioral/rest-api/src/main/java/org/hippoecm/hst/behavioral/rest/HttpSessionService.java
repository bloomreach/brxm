/*
*  Copyright 2012 Hippo.
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
package org.hippoecm.hst.behavioral.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hippoecm.hst.behavioral.rest.beans.HttpSessionInfo;

/**
 * JAX-RS service that returns information about active HTTP sessions.
 */
@Path("/sessions/")
public interface HttpSessionService {

    /**
     * Returns information about all active HTTP sessions.
     *
     * @return a list of active HTTP sessions, or an empty list if there are no active sessions.
     *
     * @throws javax.ws.rs.WebApplicationException when the client-side invocation of this service fails
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    List<HttpSessionInfo> getHttpSessions();

}
