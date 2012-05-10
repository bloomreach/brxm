/*
 *  Copyright 2012 Hippo.
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

/**
 * An {@link Exception} class indicating that a user provided incorrect username and/or password
 */
@SuppressWarnings("serial")
public class LoginException extends Exception {

    private CAUSE cause;

    public static enum CAUSE {INCORRECT_CREDENTIALS, ACCESS_DENIED, REPOSITORY_ERROR, INCORRECT_CAPTACHA}

    public LoginException(CAUSE cause) {
        this.cause = cause;
    }

    public LoginException(CAUSE cause, Throwable causeException) {
        super(causeException);
        this.cause = cause;
    }

    public CAUSE getLoginExceptionCause() {
        return this.cause;
    }

}
