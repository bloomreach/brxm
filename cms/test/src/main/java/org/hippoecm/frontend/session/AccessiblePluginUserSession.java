/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.session;

import javax.jcr.Session;

import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.Request;
import org.hippoecm.frontend.model.UserCredentials;

public class AccessiblePluginUserSession extends PluginUserSession {

    public AccessiblePluginUserSession(final Request request) {
        super(request);
    }

    public AccessiblePluginUserSession(final Request request, final LoadableDetachableModel<Session> jcrSessionModel) {
        super(request, jcrSessionModel);
    }

    @Override
    public void login(final UserCredentials credentials, final LoadableDetachableModel<Session> sessionModel) throws LoginException {
        super.login(credentials, sessionModel);
    }
}
