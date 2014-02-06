/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;

import org.apache.wicket.Application;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.page.IPageManagerContext;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.tester.WicketTester;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugin.config.impl.IApplicationFactory;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaConfigService;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoRepository;
import org.onehippo.repository.mock.MockNode;

public class HippoTester extends WicketTester {

    public HippoTester() {
        this(new HippoTesterApplication());
    }

    public HippoTester(Main main) {
        super(main);
    }

    public HippoTester(Main main, ServletContext context) {
        super(main, context);
    }

    public Home startPluginPage() {
        Home home;
        // create a request cycle, but don't use it.
        // this is a workaround for mockwebapplication's retaining of these cycles.
        // FIXME: still necessary?
//        RequestCycle rc = createRequestCycle();
        home = super.startPage(PluginPage.class);
//        rc.detach();

        Behavior behavior = new TestExecutorBehavior();
        home.add(behavior);
        return home;
    }

    public IPageManagerContext getPageManagerContext() {
        return ((Main) Application.get()).getPageManagerContext();
    }

    public void runInAjax(Home page, Runnable callback) {
        final TestExecutorBehavior executor = page.getBehaviors(TestExecutorBehavior.class).get(0);
        executor.setCallback(callback);
        executeBehavior(executor);
    }

    static class TestApplicationFactory implements IApplicationFactory {

        public IPluginConfigService getDefaultApplication() {
            return getApplication(null);
        }

        public IPluginConfigService getApplication(String name) {
            JavaConfigService configService = new JavaConfigService("test");
            JavaClusterConfig plugins = new JavaClusterConfig();
            configService.addClusterConfig("test", plugins);
            return configService;
        }
    }

    private static class TestExecutorBehavior extends AbstractDefaultAjaxBehavior {

        private Runnable callback;

        void setCallback(Runnable callback) {
            this.callback = callback;
        }

        @Override
        protected void respond(final AjaxRequestTarget target) {
            try {
                callback.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class HippoTesterApplication extends Main {

        @Override
        public RuntimeConfigurationType getConfigurationType() {
            return RuntimeConfigurationType.DEVELOPMENT;
        }

        @Override
        // suppress development mode warning from test output
        protected void outputDevelopmentModeWarning() {
        }

        @Override
        public HippoRepository getRepository() throws RepositoryException {
            return null;
        }

        @Override
        public IApplicationFactory getApplicationFactory(final Session jcrSession) {
            return new TestApplicationFactory();
        }

        @Override
        public UserSession newSession(Request request, Response response) {
            return new PluginUserSession(request, new LoadableDetachableModel<Session>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected Session load() {
                    try {
                        MockNode root = MockNode.root();
                        return root.getSession();
                    } catch (RepositoryException e) {
                        throw new RuntimeException("Unable to create mock session");
                    }
                }

            });
        }

        @Override
        public ResourceReference getPluginApplicationFavIconReference() {
            return null;
        }
    }
}
