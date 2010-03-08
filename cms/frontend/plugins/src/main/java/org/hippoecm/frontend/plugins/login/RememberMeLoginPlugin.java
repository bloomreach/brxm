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

import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.Cookie;

import org.apache.wicket.Application;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RememberMeLoginPlugin extends LoginPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(LoginPlugin.class);

    /** Algorithm to use for creating the passkey secret.
        Intentionally a relative weak algorithm, as this whole procedure isn't
        too safe to begin with.
    */
    static final String ALGORITHM = "MD5";

    public RememberMeLoginPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        String[] supported = config.getStringArray("browsers.supported");
        if (supported != null) {
            add(new BrowserCheckBehavior(supported));
        }
    }

    @Override
    protected LoginPlugin.SignInForm createSignInForm(String id) {
        boolean rememberme = false;
        ;
        Cookie[] cookies = ((WebRequest) RequestCycle.get().getRequest()).getHttpServletRequest().getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (RememberMeLoginPlugin.class.getName().equals(cookies[i].getName())) {
                    String passphrase = cookies[i].getValue();
                    if (passphrase != null && passphrase.contains("$")) {
                        String username = Base64.decode(passphrase.split("\\$")[1]);
                        credentials.put("username", username);
                        credentials.put("password", "********");
                        rememberme = true;
                    }
                    break;
                }
            }
        }
        LoginPlugin.SignInForm form = new SignInForm(id, rememberme);
        return form;
    }

    protected class SignInForm extends org.hippoecm.frontend.plugins.login.LoginPlugin.SignInForm {
        private static final long serialVersionUID = 1L;

        public boolean rememberme;

        public void setRememberme(boolean value) {
            rememberme = value;
        }

        public boolean getRememberme() {
            return rememberme;
        }

        public SignInForm(final String id, boolean rememberme) {
            super(id);
            this.rememberme = rememberme;
            if (rememberme) {
                add(new AttributeModifier("autocomplete", true, new Model<String>("off")));
            }
            add(new AjaxCheckBox("rememberme", new PropertyModel<Boolean>(this, "rememberme")) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.addComponent(SignInForm.this);
                    target.addComponent(this);
                }
            });
        }

        @Override
        public final void onSubmit() {
            UserSession userSession = (UserSession) getSession();
            JcrSessionModel sessionModel = new JcrSessionModel(credentials) {
                private static final long serialVersionUID = 1L;

                @Override
                protected javax.jcr.Session load() {
                    javax.jcr.Session result = null;
                    try {
                        Main main = (Main) Application.get();
                        HippoRepository repository = main.getRepository();

                        if (!rememberme) {
                            Cookie[] cookies = ((WebRequest) RequestCycle.get().getRequest()).getHttpServletRequest()
                                    .getCookies();
                            for (int i = 0; i < cookies.length; i++) {
                                if (RememberMeLoginPlugin.class.getName().equals(cookies[i].getName())
                                        || getClass().getName().equals(cookies[i].getName())) {
                                    ((WebResponse) RequestCycle.get().getResponse()).clearCookie(cookies[i]);
                                }
                            }
                            return super.load();
                        }

                        String username = credentials.getString("username");
                        String password = credentials.getString("password");
                        if (password == null || password.equals("") || password.replaceAll("\\*", "").equals("")) {
                            Cookie[] cookies = ((WebRequest) RequestCycle.get().getRequest()).getHttpServletRequest()
                                    .getCookies();
                            for (int i = 0; i < cookies.length; i++) {
                                if (RememberMeLoginPlugin.class.getName().equals(cookies[i].getName())) {
                                    String passphrase = cookies[i].getValue();
                                    String strings[] = passphrase.split("\\$");
                                    if (strings.length == 3) {
                                        username = Base64.decode(strings[1]);
                                        password = strings[0] + "$" + strings[2];
                                    }
                                    break;
                                }
                            }
                            result = repository.login(username, password.toCharArray());
                        } else {
                            result = repository.login(username, password.toCharArray());
                            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
                            digest.update(username.getBytes());
                            digest.update(password.getBytes());
                            String passphrase = digest.getAlgorithm() + "$" + Base64.encode(username) + "$"
                                    + Base64.encode(new String(digest.digest()));
                            ((WebResponse) RequestCycle.get().getResponse()).addCookie(new Cookie(
                                    RememberMeLoginPlugin.class.getName(), passphrase));
                            Node userinfo = result.getRootNode().getNode(
                                    HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.USERS_PATH + "/" + username);
                            String[] strings = passphrase.split("\\$");
                            userinfo.setProperty(HippoNodeType.HIPPO_PASSKEY, strings[0] + "$" + strings[2]);
                            userinfo.save();
                        }
                        if (result.getRootNode().hasNode("hippo:log")) {
                            Workflow workflow = ((HippoWorkspace) result.getWorkspace()).getWorkflowManager()
                                    .getWorkflow("internal", result.getRootNode().getNode("hippo:log"));
                            if (workflow instanceof EventLoggerWorkflow) {
                                ((EventLoggerWorkflow) workflow).logEvent(result.getUserID(), "Repository", "login");
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
                    }
                    return result;
                }
            };
            userSession.login(credentials, sessionModel);
            ConcurrentLoginFilter.validateSession(((WebRequest) SignInForm.this.getRequest()).getHttpServletRequest()
                    .getSession(true), usernameTextField.getDefaultModelObjectAsString(), false);
            userSession.setLocale(new Locale(selectedLocale));
            redirect();
        }
    }

    public static class Base64 {
        public static String base64code = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

        public static String encode(String string) {
            StringBuffer encoded = new StringBuffer();
            int paddingCount = (3 - (string.length() % 3)) % 3;
            string += "\0\0".substring(0, paddingCount);
            for (int i = 0; i < string.length(); i += 3) {
                int j = (string.charAt(i) << 16) + (string.charAt(i + 1) << 8) + string.charAt(i + 2);
                encoded.append(base64code.charAt((j >> 18) & 0x3f));
                encoded.append(base64code.charAt((j >> 12) & 0x3f));
                encoded.append(base64code.charAt((j >> 6) & 0x3f));
                encoded.append(base64code.charAt(j & 0x3f));
            }
            return encoded.substring(0, encoded.length() - paddingCount) + "==".substring(0, paddingCount);
        }

        public static String decode(String string) {
            StringBuffer decoded = new StringBuffer();
            for (int i = 0; i < string.length(); i += 4) {
                int value = 0;
                int count = 3;
                for (int j = 0; j < 4; j++) {
                    int ch = (i + j < string.length() ? string.charAt(i + j) : '=');
                    if (ch >= 'A' && ch <= 'Z') {
                        ch = ch - 'A';
                    } else if (ch >= 'a' && ch <= 'z') {
                        ch = ch - 'a' + 26;
                    } else if (ch >= '0' && ch <= '9') {
                        ch = ch - '0' + 52;
                    } else if (ch == '+') {
                        ch = 62;
                    } else if (ch == '/') {
                        ch = 63;
                    } else if (ch == '=') {
                        ch = 0;
                        --count;
                    }
                    value = (value << 6) | ch;
                }
                decoded.append((char) ((value >> 16) & 0xff));
                if (count > 1) {
                    decoded.append((char) ((value >> 8) & 0xff));
                }
                if (count > 2) {
                    decoded.append((char) (value & 0xff));
                }
            }
            return new String(decoded);
        }
    }
}
