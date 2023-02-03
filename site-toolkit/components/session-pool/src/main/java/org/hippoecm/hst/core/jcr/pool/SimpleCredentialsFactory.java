/*
 *  Copyright 2015-2023 Bloomreach
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
package org.hippoecm.hst.core.jcr.pool;


import java.util.Set;

import javax.jcr.SimpleCredentials;

import org.onehippo.repository.security.JvmCredentials;

public class SimpleCredentialsFactory {

    private SimpleCredentialsFactory() {}


    public static SimpleCredentials createInstance(final String userId, final char[] password, final Set<String> hstJmvEnabledUsers) {
        if ((password == null || password.length == 0) && hstJmvEnabledUsers != null && hstJmvEnabledUsers.contains(userId)) {
            final JvmCredentials jvmCredentials = JvmCredentials.getCredentials(userId);
            return new SimpleCredentials(userId, jvmCredentials.getPassword());
        }
        return new SimpleCredentials(userId, password);
    }

    public static SimpleCredentials createInstance(final String userId,
                                                   final char[] password,
                                                   final String separator,
                                                   final String poolName,
                                                   final Set<String> hstJmvEnabledUsers) {
        final char[] usePassword;
        if ((password == null || password.length == 0) && hstJmvEnabledUsers != null &&  hstJmvEnabledUsers.contains(userId)) {
            final JvmCredentials jvmCredentials = JvmCredentials.getCredentials(userId);
            usePassword = jvmCredentials.getPassword();
        } else {
            usePassword = password ;
        }
        if (separator != null && poolName != null) {
            return new SimpleCredentials(userId + separator + poolName, usePassword);
        }
        return new SimpleCredentials(userId, usePassword);
    }

}
