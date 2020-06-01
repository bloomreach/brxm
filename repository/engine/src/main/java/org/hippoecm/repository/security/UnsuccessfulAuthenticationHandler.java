/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.security;

import javax.security.auth.login.AccountExpiredException;
import javax.security.auth.login.CredentialExpiredException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

public final class UnsuccessfulAuthenticationHandler {

    public static void handle(final AuthenticationStatus status, final String userId) throws LoginException {
            switch (status) {
                case FAILED: throw new FailedLoginException("Wrong username or password.");
                case CREDENTIAL_EXPIRED: throw new CredentialExpiredException("User: '" + userId + "'");
                case ACCOUNT_EXPIRED: throw new AccountExpiredException("User: '" + userId + "'");
            }
    }

}
