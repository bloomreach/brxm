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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionEvent;

import static org.onehippo.cms7.services.cmscontext.CmsSessionContext.SESSION_KEY;

public class CmsContextServiceImpl implements CmsInternalCmsContextService {

    private static class CmsSessionContextImpl implements CmsSessionContext, HttpSessionBindingListener, HttpSessionActivationListener {

        private final String id = UUID.randomUUID().toString();

        private CmsContextServiceImpl service;
        private CmsSessionContextImpl cmsCtx;
        private Map<String, CmsSessionContextImpl> sharedContextsMap;
        private Map<String, Object> dataMap;

        private HttpSession session;

        private CmsSessionContextImpl(CmsContextServiceImpl service) {
            this.service = service;
            cmsCtx = this;
            sharedContextsMap = new ConcurrentHashMap<>();
            dataMap = new ConcurrentHashMap<>();
        }

        private CmsSessionContextImpl(CmsContextServiceImpl service, CmsSessionContextImpl ctx) {
            this.service = service;
            cmsCtx = ctx.cmsCtx;
            dataMap = ctx.dataMap;
            sharedContextsMap = ctx.sharedContextsMap;
            sharedContextsMap.put(id, this);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getCmsContextServiceId() {
            return service != null ? service.getId() : null;
        }

        @Override
        public synchronized Object get(final String key) {
            return dataMap.get(key);
        }

        private synchronized void detach() {
            if (service != null) {

                if (this == cmsCtx) {
                    // remove from service contexts
                    service.contextsMap.remove(id);
                    // detach all attached contexts
                    Iterator<CmsSessionContextImpl> iter = sharedContextsMap.values().iterator();
                    while (iter.hasNext()) {
                        CmsSessionContextImpl ctx = iter.next();
                        iter.remove();
                        ctx.detach();
                    }
                }
                else {
                    // will already be removed in case cmsCtx itself is being detached
                    sharedContextsMap.remove(id);
                    removeDeprecatedHSTSessionAttributes();
                }

                service = null;
                HttpSession tmpSession = session;
                session = null;
                if (tmpSession != null) {
                    try {
                        tmpSession.removeAttribute(SESSION_KEY);
                    } catch (IllegalStateException e) {
                        // ignore session already invalidated exception
                    }
                }
                cmsCtx = null;
                sharedContextsMap = Collections.emptyMap();
                dataMap = Collections.emptyMap();
            }
        }

        /**
         * TODO: remove this for *HST* 5.0 when these deprecated attributes will be dropped from ContainerConstants
         * @Deprecated
         */
        @Deprecated
        private void removeDeprecatedHSTSessionAttributes() {
            if (session != null) {
                try {
                    // ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME
                    session.removeAttribute("org.hippoecm.hst.sso.cms.repo.creds");
                    // ContainerConstants.CMS_SSO_AUTHENTICATED
                    session.removeAttribute("org.hippoecm.hst.container.sso_cms_authenticated");
                    // ContainerConstants.CMS_USER_ID_ATTR
                    session.removeAttribute("org.hippoecm.hst.container.cms_user_id");
                } catch (IllegalStateException e) {
                    // ignore: session already invalidated
                }
            }
        }

        @Override
        public void sessionWillPassivate(final HttpSessionEvent se) {
            if (session != null && session.getId().equals(se.getSession().getId())) {
                detach();
            }
        }

        @Override
        public void sessionDidActivate(final HttpSessionEvent se) {
            // noop
        }

        @Override
        public void valueBound(final HttpSessionBindingEvent event) {
            if (session == null && SESSION_KEY.equals(event.getName())) {
                session = event.getSession();
            }
            else {
                // don't allow storing this instance under any other session attribute and/or any other session
                event.getSession().removeAttribute(event.getName());
            }
        }

        @Override
        public void valueUnbound(final HttpSessionBindingEvent event) {
            if (session != null && session.getId().equals(event.getSession().getId()) && SESSION_KEY.equals(event.getName())) {
                if (this != cmsCtx) {
                    removeDeprecatedHSTSessionAttributes();
                }
                session = null;
                detach();
            }
        }
    }

    private final String id = UUID.randomUUID().toString();

    private final Map<String, CmsSessionContextImpl> contextsMap = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public CmsSessionContext getSessionContext(final String ctxId) {
        return contextsMap.get(ctxId);
    }

    @Override
    public CmsSessionContext attachSessionContext(final String ctxId, final HttpSession session) {
        CmsSessionContextImpl ctx = contextsMap.get(ctxId);
        if (ctx != null) {
            ctx = new CmsSessionContextImpl(this, ctx);
            try {
                session.setAttribute(SESSION_KEY, ctx);
            } catch (IllegalStateException e) {
                // session just happened to be invalidated, make sure to cleaup ctx
                ctx.detach();
                throw e;
            }
        }
        return ctx;
    }

    @Override
    public synchronized CmsSessionContext create(final HttpSession session) {
        CmsSessionContextImpl ctx = (CmsSessionContextImpl)session.getAttribute(SESSION_KEY);
        if (ctx == null) {
            ctx = new CmsSessionContextImpl(this);
            try {
                session.setAttribute(SESSION_KEY, ctx);
            } catch (IllegalStateException e) {
                // session just happened to be invalidated, make sure to cleaup ctx
                ctx.detach();
                throw e;
            }
            contextsMap.put(ctx.getId(), ctx);
        }
        return ctx;
    }

    @Override
    public void setData(CmsSessionContext ctx, String key, Object data) {
        ((CmsSessionContextImpl)ctx).dataMap.put(key, data);
    }
}