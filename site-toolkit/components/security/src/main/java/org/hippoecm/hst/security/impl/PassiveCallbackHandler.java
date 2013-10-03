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
package org.hippoecm.hst.security.impl;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * PassiveCallbackHandler
 * <p>PassiveCallbackHandler has constructor that takes
 * a username and password so its handle() method does
 * not have to prompt the user for input.</p>
 * <p>Useful for server-side applications.</p>
 * 
 * <p>This code was inspired from an article from:<p>
 * <ul>
 *    <li><a href="http://www.javaworld.com/javaworld/jw-09-2002/jw-0913-jaas.html">
 *    All that JAAS</a></li>
 * </ul>
 * 
 * @version $Id$
 */
public class PassiveCallbackHandler implements CallbackHandler
{

    private String username;
    char[] password;

    /**
     * <p>Creates a callback handler with the give username
     * and password.</p>
     * @param username The username.
     * @param passwordArray The password.
     */
    public PassiveCallbackHandler(String username, char[] passwordArray)
    {
        this.username = username;
        this.password = new char[passwordArray.length];
        System.arraycopy(passwordArray, 0, password, 0, passwordArray.length);
    }

    /**
     * <p>Handles the specified set of Callbacks. Uses the
     * username and password that were supplied to our
     * constructor to popluate the Callbacks.</p>
     * <p>This class supports NameCallback and PasswordCallback.</p>
     *
     * @param   callbacks the callbacks to handle
     * @throws  IOException if an input or output error occurs.
     * @throws  UnsupportedCallbackException if the callback is not an
     *          instance of NameCallback or PasswordCallback
     */
    public void handle(Callback[] callbacks) throws java.io.IOException, UnsupportedCallbackException
    {
        for (int i = 0; i < callbacks.length; i++)
        {
            if (callbacks[i] instanceof NameCallback)
            {
                ((NameCallback) callbacks[i]).setName(username);
            }
            else if (callbacks[i] instanceof PasswordCallback)
            {
                ((PasswordCallback) callbacks[i]).setPassword(password);
            }
            else
            {
                throw new UnsupportedCallbackException(callbacks[i], "Callback class not supported");
            }
        }
    }

    /**
     * <p>Clears out password state.</p>
     */
    public void clearPassword()
    {
        if (password != null)
        {
            for (int i = 0; i < password.length; i++)
            {
                password[i] = ' ';
            }
            password = null;
        }
    }

}
