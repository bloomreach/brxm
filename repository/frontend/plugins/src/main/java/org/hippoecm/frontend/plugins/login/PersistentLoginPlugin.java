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
package org.hippoecm.frontend.plugins.login;

import java.io.IOException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.Cookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.Application;
import org.apache.wicket.PageParameters;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;

import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.InvalidLoginPage;
import org.hippoecm.frontend.Main;
import org.hippoecm.frontend.model.JcrSessionModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.standardworkflow.EventLoggerWorkflow;

public class PersistentLoginPlugin extends LoginPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    private static final long serialVersionUID = 1L;
    
    private static final Logger log = LoggerFactory.getLogger(LoginPlugin.class);

    /**
     * This boolean determins whether the cookie is to store a plain password, this way, there never needs to be a successful
     * login to the repository and authentication is against the normal password in the repository.  Without plain passwords,
     * the passkey needs to be generated first, a first login to a clean repository is needed to let this plugin generate this
     * passkey.
     */
    private static boolean usePlainTextPassword = false;

    public PersistentLoginPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    protected LoginPlugin.SignInForm createSignInForm(String id) {
        if(PersistentLoginPlugin.this.getPluginConfig().containsKey("usePlainTextPassword")) {
            usePlainTextPassword = PersistentLoginPlugin.this.getPluginConfig().getBoolean("usePlainTextPassword");
        }
        Cookie[] cookies = ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest().getCookies();
        if(cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (RememberMeLoginPlugin.class.getName().equals(cookies[i].getName())) {
                    String passphrase = cookies[i].getValue();
                    if(passphrase != null && passphrase.contains("$")) {
                        String username = RememberMeLoginPlugin.Base64.decode(passphrase.split("\\$")[1]);
                        credentials.put("username", username);
                        credentials.put("password", "********");
                    }
                    break;
                }
            }
        }
        LoginPlugin.SignInForm form = new SignInForm(id);
        return form;
    }

    protected class SignInForm extends org.hippoecm.frontend.plugins.login.LoginPlugin.SignInForm {
        private static final long serialVersionUID = 1L;

        public SignInForm(final String id) {
            super(id);
            javax.jcr.Session result = null;
            String username = credentials.getString("username");
            String password = credentials.getString("password");
            if (username != null && (password == null || password.equals("") || password.replaceAll("\\*", "").equals(""))) {
                Cookie[] cookies = ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest().getCookies();
                for (int i = 0; i < cookies.length; i++) {
                    if (RememberMeLoginPlugin.class.getName().equals(cookies[i].getName())) {
                        String passphrase = cookies[i].getValue();
                        String strings[] = passphrase.split("\\$");
                        if (strings.length == 3) {
                            username = RememberMeLoginPlugin.Base64.decode(strings[1]);
                            if(usePlainTextPassword) {
                                password = RememberMeLoginPlugin.Base64.decode(strings[2]);
                            } else {
                                password = strings[0] + "$" + strings[2];
                            }
                        }
                        break;
                    }
                }
                try {
                    HippoRepository repository = ((Main)Application.get()).getRepository();
                    result = repository.login(username, password.toCharArray());
                    credentials.put("password", password);
                    if (result.getRootNode().hasNode("hippo:log")) {
                        Workflow workflow = ((HippoWorkspace)result.getWorkspace()).getWorkflowManager().getWorkflow("internal", result.getRootNode().getNode("hippo:log"));
                        if (workflow instanceof EventLoggerWorkflow) {
                            ((EventLoggerWorkflow)workflow).logEvent(result.getUserID(), "Repository", "login");
                        }
                    }
                    UserSession userSession = (UserSession)getSession();
                    userSession.setJcrSessionModel(new JcrSessionModel(credentials));
                    userSession.getJcrSession();
                    throw new RestartResponseException(Home.class, new PageParameters(RequestCycle.get().getRequest().getParameterMap()));
                } catch (LoginException ex) {
                    // deliberately ignored
                } catch (RepositoryException ex) {
                    log.error(ex.getClass().getName() + ": " + ex.getMessage());
                    credentials = Main.DEFAULT_CREDENTIALS;
                    Main main = (Main)Application.get();
                    main.resetConnection();
                    throw new RestartResponseException(InvalidLoginPage.class);
                } catch (RemoteException ex) {
                    log.error(ex.getClass().getName() + ": " + ex.getMessage());
                    credentials = Main.DEFAULT_CREDENTIALS;
                    Main main = (Main)Application.get();
                    main.resetConnection();
                    throw new RestartResponseException(InvalidLoginPage.class);
                }
            }
        }

        @Override
        public final void onSubmit() {
            UserSession userSession = (UserSession)getSession();
            userSession.setJcrSessionModel(new JcrSessionModel(credentials) {
                @Override
                protected Object load() {
                    javax.jcr.Session result = null;
                    try {
                        Main main = (Main)Application.get();
                        HippoRepository repository = main.getRepository();

                        String username = credentials.getString("username");
                        String password = credentials.getString("password");
                        if (password == null || password.equals("") || password.replaceAll("\\*", "").equals("")) {
                            Cookie[] cookies = ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest().getCookies();
                            for (int i = 0; i < cookies.length; i++) {
                                if (RememberMeLoginPlugin.class.getName().equals(cookies[i].getName())) {
                                    String passphrase = cookies[i].getValue();
                                    String strings[] = passphrase.split("\\$");
                                    if(strings.length == 3) {
                                        username = RememberMeLoginPlugin.Base64.decode(strings[1]);
                                        password = strings[0] + "$" + strings[2];
                                    }
                                    break;
                                }
                            }
                            result = repository.login(username, password.toCharArray());
                        } else {
                            result = repository.login(username, password.toCharArray());
                            MessageDigest digest = MessageDigest.getInstance(RememberMeLoginPlugin.ALGORITHM);
                            digest.update(username.getBytes());
                            digest.update(password.getBytes());
                            String passphrase;
                            if (usePlainTextPassword) {
                                passphrase = "PLAIN$" + RememberMeLoginPlugin.Base64.encode(username) + "$" + RememberMeLoginPlugin.Base64.encode(password);
                            } else {
                                passphrase = digest.getAlgorithm() + "$" + RememberMeLoginPlugin.Base64.encode(username) + "$" + RememberMeLoginPlugin.Base64.encode(new String(digest.digest()));
                            }
                            ((WebResponse)RequestCycle.get().getResponse()).addCookie(new Cookie(RememberMeLoginPlugin.class.getName(), passphrase));
                            if (!usePlainTextPassword) {
                                Node userinfo = result.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.USERS_PATH + "/" + username);
                                String[] strings = passphrase.split("\\$");
                                userinfo.setProperty(HippoNodeType.HIPPO_PASSKEY, strings[0] + "$" + strings[2]);
                                userinfo.save();
                            }
                        }
                        if (result.getRootNode().hasNode("hippo:log")) {
                            Workflow workflow = ((HippoWorkspace)result.getWorkspace()).getWorkflowManager().getWorkflow("internal", result.getRootNode().getNode("hippo:log"));
                            if (workflow instanceof EventLoggerWorkflow) {
                                ((EventLoggerWorkflow)workflow).logEvent(result.getUserID(), "Repository", "login");
                            }
                        }
                    } catch (NoSuchAlgorithmException ex) {
                        log.error(ex.getClass().getName() + ": " + ex.getMessage());
                    } catch (LoginException ex) {
                        log.info("Invalid login as user: " + credentials.getString("username"));
                    } catch (RepositoryException ex) {
                        log.error(ex.getClass().getName() + ": " + ex.getMessage());
                    } catch (RemoteException ex) {
                        log.error(ex.getClass().getName() + ": " + ex.getMessage());
                    } catch (IOException ex) {
                        log.error(ex.getClass().getName() + ": " + ex.getMessage());
                    }
                    if (result == null) {
                        credentials = Main.DEFAULT_CREDENTIALS;
                        Main main = (Main)Application.get();
                        main.resetConnection();
                        throw new RestartResponseException(InvalidLoginPage.class);
                    }
                    return result;
                }
            });
            userSession.setLocale(new Locale(selectedLocale));
            userSession.getJcrSession();
            redirect();
        }
    }
}
