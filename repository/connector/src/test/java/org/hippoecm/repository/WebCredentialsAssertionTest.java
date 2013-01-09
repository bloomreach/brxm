/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.jackrabbit.core.security.authentication.CredentialsCallback;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class WebCredentialsAssertionTest {

    @Test
    public void testCredentialsCallback() throws Exception {
        CredentialsCallback callback = new CredentialsCallback();
        WebCredentials webCredentials = new WebCredentials("username", "password".toCharArray(), new HashMap<String, String>());
        callback.setCredentials(webCredentials);

        assertTrue(callback.getClass().getName().equals("org.apache.jackrabbit.core.security.authentication.CredentialsCallback"));
        Method method = callback.getClass().getMethod("setCredentials", new Class[] {javax.jcr.Credentials.class});
        method.invoke(callback, new Object[] {webCredentials});
    }
}
