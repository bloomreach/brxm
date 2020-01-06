/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.security;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

public class TokenCmsSessionContextRegistry {

    private static final String SESSION_LISTENER_ATTR = TokenCmsSessionContextRegistry.class.getName() + ".listenerAttr";

    // since we do not want to pass the 'tokenCmsSessionContextMap' to the TokenCmsSessionContextMapCleanupListener
    // since'tokenCmsSessionContextMap' is not serializable, use a static class member instead of instance member
    private static final Cache<String, CmsSessionContext>
            tokenCmsSessionContextMap = CacheBuilder.newBuilder().build();

    void register(final String tokenSubject, final CmsSessionContext cmsSessionContext, final HttpSession session) {

        // never put same subject twice otherwise #valueUnbound of the old value will remove tokenSubject from cache
        if (tokenCmsSessionContextMap.getIfPresent(tokenSubject) == null) {
            // set a listener on the http session for invalidation
            tokenCmsSessionContextMap.put(tokenSubject, cmsSessionContext);
            session.setAttribute(SESSION_LISTENER_ATTR, new TokenCmsSessionContextMapCleanupListener(tokenSubject));
        }

    }

    CmsSessionContext getCmsSessionContext(final String subject) {
        return tokenCmsSessionContextMap.getIfPresent(subject);
    }

    static class TokenCmsSessionContextMapCleanupListener implements HttpSessionActivationListener, HttpSessionBindingListener {

        private String subject;

        TokenCmsSessionContextMapCleanupListener(final String subject) {
            this.subject = subject;
        }

        @Override
        public void sessionWillPassivate(final HttpSessionEvent se) {
            tokenCmsSessionContextMap.invalidate(subject);
        }

        @Override
        public void sessionDidActivate(final HttpSessionEvent se) {
            // nothing
        }

        @Override
        public void valueBound(final HttpSessionBindingEvent event) {
            // nothing
        }

        @Override
        public void valueUnbound(final HttpSessionBindingEvent event) {
            tokenCmsSessionContextMap.invalidate(subject);
        }
    }
}
