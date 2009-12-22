/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.plugins.proxylogin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.rmi.RemoteException;
import java.util.Locale;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;

import org.apache.wicket.Application;
import org.apache.wicket.PageParameters;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.markup.html.form.TextField;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.proxyrepository.ProxyHippoRepository;
import org.hippoecm.repository.standardworkflow.EventLoggerWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginPlugin extends org.hippoecm.frontend.plugins.login.LoginPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(LoginPlugin.class);

    public LoginPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected org.hippoecm.frontend.plugins.proxylogin.LoginPlugin.SignInForm createSignInForm(String id) {
        return new SignInForm(id);
    }

    protected class SignInForm extends org.hippoecm.frontend.plugins.login.LoginPlugin.SignInForm {
        private static final long serialVersionUID = 1L;

        private TextField<String> dumpComponent;

        public SignInForm(final String id) {
            super(id);
            add(dumpComponent = new TextField<String>("dump", new StringPropertyModel(credentials, "dump")));
        }
       
        @Override
        public final void onSubmit() {
            UserSession userSession = (UserSession) getSession();
            userSession.login(credentials, new JcrSessionModel(credentials) {
                @Override
                protected javax.jcr.Session load() {
                    javax.jcr.Session result = null;
                    try {
                        Main main = (Main)Application.get();
                        HippoRepository repository = main.getRepository();

                        String username = credentials.getString("username");
                        String password = credentials.getString("password");
                        String dumpname = credentials.getString("dump");
                        dumpname = dumpComponent.getDefaultModelObjectAsString();

                        if (repository != null && username != null && password != null) {
                            if (dumpname != null && !dumpname.trim().equals("")) {
                                try {
                                    File dumpfile = new File(dumpname);
                                    if (dumpfile.exists()) {
                                        result = ((ProxyHippoRepository)repository).login(username, password.toCharArray(), new FileInputStream(dumpfile));
                                    } else {
                                        result = ((ProxyHippoRepository)repository).login(username, password.toCharArray(), new FileOutputStream(dumpfile));
                                    }
                                } catch (FileNotFoundException ex) {
                                    log.warn("Cannot use dump file", ex);
                                    result = repository.login(username, password.toCharArray());
                                }
                            } else {
                                result = repository.login(username, password.toCharArray());
                            }
                            try {
                                if (result.getRootNode().hasNode("hippo:log")) {
                                    Workflow workflow = ((HippoWorkspace)result.getWorkspace()).getWorkflowManager().getWorkflow("internal", result.getRootNode().getNode("hippo:log"));
                                    if (workflow instanceof EventLoggerWorkflow) {
                                        ((EventLoggerWorkflow)workflow).logEvent(result.getUserID(), "Repository", "login");
                                    }
                                }
                            } catch (RepositoryException ex) {
                                log.error(ex.getClass().getName() + ": " + ex.getMessage());
                            } catch (RemoteException ex) {
                                log.error(ex.getClass().getName() + ": " + ex.getMessage());
                            }
                        }
                    } catch (LoginException e) {
                        log.info("Invalid login as user: " + credentials.getString("username"));
                    } catch (RepositoryException e) {
                        log.error(e.getMessage());
                    }
                    return result;
                }
            });
            userSession.setLocale(new Locale(selectedLocale));
            userSession.getJcrSession();
            setResponsePage(Home.class, new PageParameters(RequestCycle.get().getRequest().getParameterMap()));
        }
    }
}
