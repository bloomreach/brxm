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

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;

import org.apache.wicket.Application;
import org.apache.wicket.Request;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.HttpSessionStore;
import org.apache.wicket.protocol.http.WebRequest;

/**
 * HTTP Session store that invokes {@link #unbind()} on the UserSession when
 * the http session is invalidated.
 * 
 * TODO: use a {@link HttpSessionActivationListener} to get rid of WeakReferences
 *       here and eliminate JcrSessionReference.  This would clean up sessions
 *       earlier when they are serialized.
 */
public class UnbindingHttpSessionStore extends HttpSessionStore {
    private Map<String, WeakReference<UserSession>> sessions = new HashMap<String, WeakReference<UserSession>>();

    public UnbindingHttpSessionStore(Application application) {
        super(application);
    }

    @Override
    protected void onBind(Request request, Session newSession) {
        super.onBind(request, newSession);

        if (newSession instanceof UserSession) {
            WebRequest webRequest = toWebRequest(request);
            HttpSession httpSession = getHttpSession(webRequest);

            sessions.put(httpSession.getId(), new WeakReference<UserSession>((UserSession) newSession));
        }
    }

    @Override
    protected void onUnbind(String sessionId) {
        WeakReference<UserSession> sessionRef = sessions.remove(sessionId);
        if (sessionRef != null) {
            UserSession userSession = sessionRef.get();
            if (userSession != null) {
                userSession.unbind();
            }
        }
        super.onUnbind(sessionId);
    }

}