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
package org.hippoecm.frontend.model;

import java.io.IOException;
import java.io.Serializable;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.jackrabbit.core.security.authentication.CredentialsCallback;
import org.hippoecm.frontend.session.LoginException;

public class UserCredentials implements Serializable {

    private static final long serialVersionUID = 1L;

    private Credentials credentials = null;
    private String username = "";

    public UserCredentials(String username, String password) {
        this(new SimpleCredentials(username, (password != null ? password.toCharArray() : new char[0])));
        this.username = username;
    }

    public UserCredentials(Credentials credentials) {
        this.credentials = credentials;
        if(credentials instanceof SimpleCredentials) {
            username = ((SimpleCredentials)credentials).getUserID();
        }
    }

    public UserCredentials(CallbackHandler callbackHandler) throws LoginException {
        NameCallback nameCallback = new NameCallback("username");
        PasswordCallback passwordCallback = new PasswordCallback("password", false);
        CredentialsCallback credentialsCallback = new CredentialsCallback();
        try {
            callbackHandler.handle(new Callback[] { credentialsCallback, nameCallback });
        } catch (IOException | UnsupportedCallbackException ignore) {
        }
        credentials = credentialsCallback.getCredentials();
        username = nameCallback.getName();
        if(credentials == null) {
            try {
                callbackHandler.handle(new Callback[] { nameCallback, passwordCallback });
                char[] password = passwordCallback.getPassword();
                credentials = new SimpleCredentials(username, password);
            } catch (IOException ioe) {
                throw new LoginException(LoginException.CAUSE.INCORRECT_CREDENTIALS, ioe);
            } catch (UnsupportedCallbackException e) {
            }
        }
    }

    public String getUsername() {
        return username;
    }

    public Credentials getJcrCredentials() {
        return credentials;
    }
}
