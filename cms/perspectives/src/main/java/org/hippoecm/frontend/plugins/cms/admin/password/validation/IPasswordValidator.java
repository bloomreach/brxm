/*
 *  Copyright 2011-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugins.cms.admin.users.User;

/**
 * A password validator checks whether a password complies to a rule.
 */
public interface IPasswordValidator extends Serializable {

    /**
     * Check whether the password complies to the rule this IPasswordValidator defines.
     *
     * @param password the password to check against the rule.
     * @param user     the user for whom the password is being checked.
     * @return the validation status
     * @throws RepositoryException
     */
    PasswordValidationStatus checkPassword(String password, User user) throws RepositoryException;

    /**
     * If this validator is optional then it is part of a set of validators for which a configured number of them must
     * pass.
     */
    boolean isOptional();

    /**
     * Internationalized description of what rule this validator checks.
     */
    String getDescription();

}
