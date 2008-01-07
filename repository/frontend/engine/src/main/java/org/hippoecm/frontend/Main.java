/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend;

import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.application.IClassResolver;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends WebApplication {

    static final Logger log = LoggerFactory.getLogger(Main.class);

    /** Parameter name of the repository storage directory */
    public final static String REPOSITORY_ADDRESS_PARAM = "repository-address";
    public final static String REPOSITORY_DIRECTORY_PARAM = "repository-directory";

    @Override
    protected void init() {
        super.init();
        getDebugSettings().setAjaxDebugModeEnabled(false);

        getSecuritySettings().setAuthorizationStrategy(new IAuthorizationStrategy() {

            public boolean isActionAuthorized(Component component, Action action) {
                return true;
            }

            public boolean isInstantiationAuthorized(Class componentClass) {
                if (Home.class.isAssignableFrom(componentClass)) {
                    UserSession session = (UserSession) Session.get();
                    session.getJcrSession();
                }
                return true;
            }
        });

        getApplicationSettings().setClassResolver(new IClassResolver() {
            public Class resolveClass(String name) throws ClassNotFoundException {
                if (Session.exists()) {
                    UserSession session = (UserSession) Session.get();
                    ClassLoader loader = session.getClassLoader();
                    if (loader != null) {
                        return session.getClassLoader().loadClass(name);
                    }
                }
                return Thread.currentThread().getContextClassLoader().loadClass(name);
            }
        });
    }

    @Override
    protected void onDestroy() {
        repository.close();
    }

    @Override
    public Class getHomePage() {
        return Home.class;
    }

    @Override
    public Session newSession(Request request, Response response) {
        return new UserSession(request);
    }

    public String getConfigurationParameter(String parameterName, String defaultValue) {
        String result = getInitParameter(parameterName);
        if (result == null || result.equals("")) {
            result = getServletContext().getInitParameter(parameterName);
        }
        if (result == null || result.equals("")) {
            result = defaultValue;
        }
        return result;
    }

    private HippoRepository repository;

    public HippoRepository getRepository() {
        if (repository == null) {
            String repositoryAddress = getConfigurationParameter(REPOSITORY_ADDRESS_PARAM, null);
            String repositoryDirectory = getConfigurationParameter(REPOSITORY_DIRECTORY_PARAM, "repository");
            try {
                if (repositoryAddress != null && !repositoryAddress.trim().equals("")) {
                    repository = HippoRepositoryFactory.getHippoRepository(repositoryAddress);
                } else {
                    repository = HippoRepositoryFactory.getHippoRepository(repositoryDirectory);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        }
        return repository;
    }

    public void resetConnection() {
        repository = null;
    }

}
