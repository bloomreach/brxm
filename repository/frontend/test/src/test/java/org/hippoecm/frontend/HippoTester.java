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
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.request.IRequestCycleProcessor;
import org.apache.wicket.util.tester.WicketTester;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.config.impl.IApplicationFactory;
import org.hippoecm.frontend.session.UserSession;

public class HippoTester extends WicketTester {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private IApplicationFactory appFactory;

    public HippoTester() {
        this(new JcrSessionModel(null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Object load() {
                return null;
            }
        }, null);
    }

    public Home startPluginPage() {
        Home home;
        // create a request cycle, but don't use it.
        // this is a workaround for mockwebapplication's retaining of these cycles. 
        RequestCycle rc = createRequestCycle();
        if (appFactory != null) {
            home = (Home) super.startPage(new Home(appFactory));
        } else {
            home = (Home) super.startPage(Home.class);
        }
        rc.detach();
        return home;
    }
    
    public HippoTester(final JcrSessionModel sessionModel, IApplicationFactory jcrAppFactory) {
        super(new NonPageCachingDummyWebApplication() {

            @Override
            protected IRequestCycleProcessor newRequestCycleProcessor() {
                return new PluginRequestCycleProcessor();
            }

            @Override
            public Session newSession(Request request, Response response) {
                return new UserSession(request, sessionModel);
            }
        });

        this.appFactory = jcrAppFactory;
    }

}
