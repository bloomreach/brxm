/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.session;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;

import org.apache.wicket.Application;
import org.apache.wicket.IPageMap;
import org.apache.wicket.PageMap;
import org.apache.wicket.Request;
import org.apache.wicket.Session;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.protocol.http.HttpSessionStore;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.util.collections.ConcurrentHashSet;

/**
 * HTTP Session store that invokes {@link PluginUserSession#unbind()} when
 * the http session is invalidated.
 * 
 * TODO: use a {@link HttpSessionActivationListener} to get rid of WeakReferences
 *       here and eliminate JcrSessionReference.  This would clean up sessions
 *       earlier when they are serialized.
 */
public class UnbindingHttpSessionStore extends HttpSessionStore {

    private final Map<String, WeakReference<UserSession>> sessions = new HashMap<String, WeakReference<UserSession>>();
    private final Set<String> sessionsWithInvalidPageMaps = new ConcurrentHashSet<String>();

    public UnbindingHttpSessionStore(Application application) {
        super(application);
    }
    
    @Override
    public void onBeginRequest(Request request) {
        WebRequest webRequest = toWebRequest(request);
        HttpSession httpSession = getHttpSession(webRequest);
        if (httpSession != null && sessionsWithInvalidPageMaps.contains(httpSession.getId())) {
            sessionsWithInvalidPageMaps.remove(httpSession.getId());
            clearPageMaps(request);
        }
    }

    // only one page map is supported - wicket sychronizes request on pagemaps.
    // Since we dispatch events to all pages, they need to share a pagemap.
    @Override
    public IPageMap createPageMap(String name) {
        if ((PageMap.DEFAULT_NAME == null && name != null) || (PageMap.DEFAULT_NAME != null && !PageMap.DEFAULT_NAME.equals(name))) {
            throw new WicketRuntimeException("Only page maps with name " + PageMap.DEFAULT_NAME + " are allowed");
        }
        return super.createPageMap(name);
    }
    
    @Override
    protected void onBind(Request request, Session newSession) {
        super.onBind(request, newSession);

        if (newSession instanceof UserSession) {
            WebRequest webRequest = toWebRequest(request);
            HttpSession httpSession = getHttpSession(webRequest);

            sessions.put(httpSession.getId(), new WeakReference<UserSession>((UserSession) newSession));
            ((PluginUserSession) newSession).onBind(httpSession.getId());
        }
    }

    @Override
    protected void onUnbind(String sessionId) {
        WeakReference<UserSession> sessionRef = sessions.remove(sessionId);
        if (sessionRef != null) {
            PluginUserSession userSession = (PluginUserSession) sessionRef.get();
            if (userSession != null) {
                userSession.unbind();
            }
        }
        sessionsWithInvalidPageMaps.remove(sessionId);
        super.onUnbind(sessionId);
    }
    
    void setClearPageMaps(String sessionId) {
        if (sessionId != null) {
            sessionsWithInvalidPageMaps.add(sessionId);
        }
    }
    
    private void clearPageMaps(Request request) {
        for (String attribute : getAttributeNames(request)) {
            if (attribute.startsWith("m:")) {
                IPageMap pageMap = (IPageMap) getAttribute(request,attribute);
                if (pageMap != null) {
                    pageMap.clear();
                }
            }
        }
    }

}
