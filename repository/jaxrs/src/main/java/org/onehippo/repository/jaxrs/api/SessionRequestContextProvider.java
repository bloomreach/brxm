/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.jaxrs.api;

import java.util.Locale;

import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

/**
 * Provides data related to the current CMS session.
 */
public interface SessionRequestContextProvider {

    /**
     * returns the user {@link Session} for the current request. Every unique {@code servletRequest} returns a newly created user {@link Session}
     * which gets closed automatically at the end of the jaxrs request. Any pending changes before closing the session
     * are not saved.
     * @param servletRequest the current jaxrs request
     * @return the {@link Session} for the current user
     */
    Session getJcrSession(HttpServletRequest servletRequest);

    /**
     * returns a system {@link Session}. Every unique {@code servletRequest} returns a newly created system {@link Session}
     * which gets closed automatically at the end of the jaxrs request. Any pending changes before closing the session
     * are not saved . The system {@link Session} is lazily created on request basis.
     * @param servletRequest the current jaxrs request
     * @return the system {@link Session} for the current request.
     */
    Session getSystemSession(HttpServletRequest servletRequest);

    Locale getLocale(HttpServletRequest servletRequest);

    /**
     * @param servletRequest
     * @return the host name as seen by the client
     */
    String getFarthestRequestHost(HttpServletRequest servletRequest);

}