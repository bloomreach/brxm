/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.cmscontext;

import java.util.Locale;

import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpSession;

/**
 * Provides access to CMS Application Session Context specific information
 */
public interface CmsSessionContext {

    /**
     * Attribute name under which the CmsSessionContext is bind in its HttpSession
     */
    String SESSION_KEY = CmsSessionContext.class.getName();

    /**
     * Key to the the SimpleCredentials with which the CMS session authenticates with the Repository.
     *
     * @see #getRepositoryCredentials()
     */
    String REPOSITORY_CREDENTIALS = "repository.credentials";

    /**
     * Key to retrieve the locale applicable to the current CMS session.
     *
     * @see #getLocale()
     */
    String LOCALE = "locale";


    /**
     * Key to retrieve the context payload
     */
    String CMS_SESSION_CONTEXT_PAYLOAD_KEY = CmsSessionContext.class.getName() + ".contextPayload";

    /**
     * Static method to retrieve the CmsSessionContext from a HttpSession
     *
     * @param session the HttpSession
     * @return the CmsSessionContext bound to the provided HttpSession
     */
    static CmsSessionContext getContext(HttpSession session) {
        return (CmsSessionContext) session.getAttribute(SESSION_KEY);
    }

    /**
     * @return Unique identifier for this CmsSessionContext
     */
    String getId();

    /**
     * @return Unique identifier for the CmsContextService which created this CmsSessionContext
     */
    String getCmsContextServiceId();

    /**
     * Retrieve CMS Session Context specific information by key
     *
     * @param key the key of the information
     * @return the information
     */
    Object get(String key);

    /**
     * @return the SimpleCredentials with which the CMS session authenticates with the Repository
     */
    default SimpleCredentials getRepositoryCredentials() {
        return (SimpleCredentials) get(REPOSITORY_CREDENTIALS);
    }

    /**
     * @return the Locale applicable to the current CMS session
     */
    default Locale getLocale() {
        return (Locale) get(LOCALE);
    }

    /**
     * Retrieves the currently stored attribute value identified by key from the context and returns it.
     *
     * @param key the key of the value to set, must not be {@code null}
     * @return the value associated with key, or {@code null} if there is no attribute for key.
     */
    Object getAttribute(String key);

    /**
     * Stores an attribute value identified by key in the context and returns the previously stored attribute value or
     * {@code null} if there was no attribute for the key. The CmsSessionContext is shared amongst web-apps, so make
     * sure that the class of the attribute value is on the classpath of the web-app that will read the value.
     *
     * @param key   the key of the value to set, must not be {@code null}
     * @param value the value to associate with the key, must not be {@code null}
     * @return the previous value associated with key, or {@code null} if there was no attribute for key.
     */
    Object setAttribute(String key, Object value);

    /**
     * Removes an attribute value and it's key from the context and returns the currently stored attribute value or
     * {@code null} if there was no attribute for the key.
     *
     * @param key the key of the value to set, must not be {@code null}
     * @return the previous value associated with key, or {@code null} if there was no attribute for key.
     */
    Object removeAttribute(String key);

}
