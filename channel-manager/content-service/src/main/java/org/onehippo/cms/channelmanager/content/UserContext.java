/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content;

import java.util.Locale;
import java.util.TimeZone;

import javax.jcr.Session;

/**
 * Properties of the CMS user.
 */
public class UserContext {

    private final Session session;
    private final Locale locale;
    private final TimeZone timeZone;

    public UserContext(final Session session, final Locale locale, final TimeZone timeZone) {
        this.session = session;
        this.locale = locale;
        this.timeZone = timeZone;
    }

    /**
     * @return User-authenticated, invocation-scoped JCR session of the CMS user
     */
    public Session getSession() {
        return session;
    }

    /**
     * @return Locale of the CMS user
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * @return Time zone of the CMS user
     */
    public TimeZone getTimeZone() {
        return timeZone;
    }
}
