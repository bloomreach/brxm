/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.core.internal;

import javax.jcr.Session;

/**
 * A {@link ThreadLocal} class to be used for {@ Session} propagation
 */
public final class CmsJcrSessionThreadLocal {

    private static final ThreadLocal<Session> jcrSession;

    static {
        jcrSession = new ThreadLocal<Session>();
    }

    public static void setJcrSession(Session session) throws IllegalArgumentException, IllegalStateException {
        if (session == null) {
            throw new IllegalArgumentException("JCR session argument is 'null'!");
        }

        if (jcrSession.get() != null) {
            throw new IllegalStateException("JCR session already initialized!");
        }

        jcrSession.set(session);
    }

    public static Session getJcrSession() {
        return jcrSession.get();
    }

    public static void clearJcrSession() {
        jcrSession.remove();
    }

}
