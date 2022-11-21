/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.addon.frontend.gallerypicker;

import javax.jcr.Session;

import org.hippoecm.frontend.session.UserSession;

/**
 * Provides the JCR session for the current Wicket user session.
 */
public class WicketJcrSessionProvider implements JcrSessionProvider {

    private static final WicketJcrSessionProvider INSTANCE = new WicketJcrSessionProvider();

    private WicketJcrSessionProvider() {
    }

    public static WicketJcrSessionProvider get() {
        return INSTANCE;
    }

    @Override
    public Session getJcrSession() {
        return ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
    }
}
