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
package org.hippoecm.hst.behavioral.rest.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.hippoecm.hst.behavioral.HttpSessionRegistry;
import org.hippoecm.hst.behavioral.rest.HttpSessionService;
import org.hippoecm.hst.behavioral.rest.beans.HttpSessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS resource that returns information about active HTTP sessions. The sessions are read from the injected
 * {@link HttpSessionRegistry}.
 */
public class HttpSessionResource implements HttpSessionService {

    private static final Logger log = LoggerFactory.getLogger(HttpSessionResource.class);

    private HttpSessionRegistry httpSessionRegistry;

    public void setHttpSessionRegistry(final HttpSessionRegistry httpSessionRegistry) {
        this.httpSessionRegistry = httpSessionRegistry;
    }

    public List<HttpSessionInfo> getHttpSessions() {
        if (httpSessionRegistry == null) {
            log.warn("Cannot look up HTTP sessions because the HTTP session registry is null");
            return Collections.emptyList();
        }
        
        Collection<HttpSession> httpSessions = httpSessionRegistry.getHttpSessions();
        List<HttpSessionInfo> result = new ArrayList<HttpSessionInfo>(httpSessions.size());

        for (HttpSession httpSession : httpSessions) {
            result.add(new HttpSessionRepresentation().represent(httpSession));
        }

        return result;
    }

}
