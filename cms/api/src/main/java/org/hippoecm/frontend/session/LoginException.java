/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.session;

import org.apache.wicket.util.io.IClusterable;

/**
 * An {@link Exception} class indicating that a user provided incorrect login details
 */
public class LoginException extends Exception {

    public static Cause newCause(final String key) {
        return new Cause(key);
    }

    public static class Cause implements IClusterable {
        private String key;

        public final static Cause INCORRECT_CREDENTIALS = newCause("invalid.login");
        public final static Cause ACCESS_DENIED         = newCause("access.denied");
        public final static Cause REPOSITORY_ERROR      = newCause("repository.error");
        public final static Cause PASSWORD_EXPIRED      = newCause("password.expired");
        public final static Cause ACCOUNT_EXPIRED       = newCause("account.expired");

        Cause(final String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    private Cause cause;

    public LoginException(Cause cause) {
        this.cause = cause;
    }

    public LoginException(Cause cause, Throwable causeException) {
        super(causeException);
        this.cause = cause;
    }

    public Cause getLoginExceptionCause() {
        return this.cause;
    }

}
