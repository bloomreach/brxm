/*
 *  Copyright 2011-2023 Bloomreach
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
package org.hippoecm.frontend.plugins.cms.admin.password.validation;


public final class PasswordValidationStatus {

    public static final PasswordValidationStatus ACCEPTED = new PasswordValidationStatus(true);

    private final String message;
    private final boolean accepted;

    public PasswordValidationStatus(final boolean accepted) {
        this(null, accepted);
    }

    public PasswordValidationStatus(final String message, final boolean accepted) {
        this.message = message;
        this.accepted = accepted;
    }

    public String getMessage() {
        return message;
    }

    public boolean accepted() {
        return accepted;
    }

}
