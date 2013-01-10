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
package org.hippoecm.repository.security;

import java.io.IOException;
import java.util.Map;
import javax.jcr.Credentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.jackrabbit.core.security.authentication.CredentialsCallback;

import org.hippoecm.repository.ParameterCallback;
import org.hippoecm.repository.WebCredentials;

public class ExampleLoginModule implements LoginModule {

    protected Subject subject;

    protected CallbackHandler callbackHandler;
    protected Map<String,?> sharedState;
    protected boolean validLogin;

    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        validLogin = false;
        this.sharedState = sharedState;
        this.callbackHandler = callbackHandler;
    }

    public boolean login() throws LoginException {
        try {
            CredentialsCallback callback = new CredentialsCallback();
            callbackHandler.handle(new Callback[] {callback});
            Credentials credentials = callback.getCredentials();
            if (credentials instanceof WebCredentials) {
                WebCredentials webCredentials = (WebCredentials)credentials;
                ParameterCallback parameterCallback = new ParameterCallback("id");
                webCredentials.handle(new Callback[] {parameterCallback});
                String key = parameterCallback.getValue();
                String username = validate(key);
                if (username == null) {
                    throw new LoginException(); // key not valid
                } else {
                    ((Map<String,String>)sharedState).put("javax.security.auth.login.name", username);
                    return (validLogin = true);
                }
            } else {
                return (validLogin = true);
            }
        } catch (UnsupportedCallbackException ex) {
        } catch (IOException ex) {
        }
        return validLogin;
    }

    protected String validate(String key) {
        return key;
    }

    public boolean commit() throws LoginException {
        return validLogin;
    }

    public boolean abort() throws LoginException {
        return validLogin;
    }

    public boolean logout() throws LoginException {
        return validLogin;
    }
}
