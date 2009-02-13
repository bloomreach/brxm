/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend;

import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.util.tester.WicketTester;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.session.UserSession;

public class HippoTester extends WicketTester {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public HippoTester() {
        this(new JcrSessionModel(null) {
            @Override
            protected Object load() {
                return null;
            }
        });
    }

    public HippoTester(final JcrSessionModel sessionModel) {
        super(new NonPageCachingDummyWebApplication() {
            @Override
            public Session newSession(Request request, Response response) {
                return new UserSession(request, sessionModel);
            }
        });
    }

}
