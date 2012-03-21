/*
 *  Copyright 2010 Hippo.
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
import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.apache.jackrabbit.core.security.authentication.CredentialsCallback;

public class UserCredentials {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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

    public UserCredentials(CallbackHandler callbackHandler) {
        NameCallback nameCallback = new NameCallback("username");
        PasswordCallback passwordCallback = new PasswordCallback("password", false);
        CredentialsCallback credentialsCallback = new CredentialsCallback();
        try {
            callbackHandler.handle(new Callback[] { credentialsCallback, nameCallback });
        } catch (IOException ex) {
        } catch (UnsupportedCallbackException ex) {
        }
        credentials = credentialsCallback.getCredentials();
        username = nameCallback.getName();
        if(credentials == null) {
            try {
                callbackHandler.handle(new Callback[] { nameCallback, passwordCallback });
                char[] password = passwordCallback.getPassword();
                credentials = new SimpleCredentials(username, password);
            } catch (IOException e) {
            } catch (UnsupportedCallbackException e) {
            }
        }
    }

    public String getUsername() {
        return username;
    }

    /**
     * JCR credentials.
     *
     * @return
     */
    public Credentials getJcrCredentials() {
        return credentials;
    }
}
