/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;
import javax.jcr.Credentials;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class WebCredentials implements Credentials, CallbackHandler {

    private String username = null;
    private char[] password = null;
    private Map<String, String> parameters = new HashMap<String, String>();

    public WebCredentials(String username, char[] password, Map<String, String> parameters) {
        this.username = username;
        this.password = password;
        this.parameters.putAll(parameters);
    }
    public WebCredentials(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if(cookies != null) {
            for(Cookie cookie : cookies) {
                parameters.put(cookie.getName(), cookie.getValue());
            }
        }
        for(Enumeration e=request.getHeaderNames(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            parameters.put(key, request.getHeader(key));
        }
        for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            parameters.put(key, request.getParameter(key));
        }
    }

    public WebCredentials(String username, Map<String, String> parameters) {
        this(username, null, parameters);
    }

    public WebCredentials(Map<String, String> parameters) {
        this(null, null, parameters);
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback != null && callback.getClass().getName().equals("org.apache.jackrabbit.core.security.authentication.CredentialsCallback")) {
                try {
                    Method method = callback.getClass().getMethod("setCredentials", new Class[] {javax.jcr.Credentials.class});
                    method.invoke(callback, new Object[] {this});
                } catch (IllegalAccessException ex) {
                    throw new IOException(ex);
                } catch (InvocationTargetException ex) {
                    // setCredentials has no declared Exception, so no need to rethrow them
                    throw new IOException(ex); // cannot happen because of setCredentials declaration
                } catch (NoSuchMethodException ex) {
                    throw new IOException(ex);
                }
            } else if (callback instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback)callback;
                if (username != null) {
                    nameCallback.setName(username);
                } else if(parameters.containsKey("j_username")) {
                    nameCallback.setName(parameters.get("j_username"));
                }
            } else if (callback instanceof PasswordCallback) {
                PasswordCallback passwordCallback = (PasswordCallback)callback;
                if (username != null) {
                    passwordCallback.setPassword(password);
                } else if(parameters.containsKey("j_password") && parameters.get("j_password")!=null) {
                    passwordCallback.setPassword(parameters.get("j_password").toCharArray());
                }
            } else if(callback instanceof ParameterCallback) {
                ParameterCallback parameterCallback = (ParameterCallback) callback;
                if (parameters.containsKey(parameterCallback.getName())) {
                    parameterCallback.setValue(parameters.get(parameterCallback.getName()));
                }
            }
        }
    }
}
