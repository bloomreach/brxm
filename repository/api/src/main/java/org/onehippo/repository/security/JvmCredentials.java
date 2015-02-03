/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.security;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Credentials;

public final class JvmCredentials implements Credentials {

    private static final Map<String, JvmCredentials> credentials = new ConcurrentHashMap<>();
    public static final String PASSKEY = "jvm://";

    private final String userID;
    private final char[] password;

    private JvmCredentials(String userID, char[] password) {
        this.userID = userID;
        this.password = password;
    }

    public String getUserID() {
        return userID;
    }

    public char[] getPassword() {
        return password;
    }

    public static JvmCredentials getCredentials(String userID) {
        JvmCredentials credentials = JvmCredentials.credentials.get(userID);
        if (credentials != null) {
            return credentials;
        }
        credentials = new JvmCredentials(userID, UUID.randomUUID().toString().toCharArray());
        final JvmCredentials prevCredentials = JvmCredentials.credentials.putIfAbsent(userID, credentials);
        if (prevCredentials != null) {
            return prevCredentials;
        }
        return credentials;
    }

}
