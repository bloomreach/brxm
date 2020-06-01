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

import java.util.regex.Pattern;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.users.User;

public class ContainsNoNamePasswordValidator extends AbstractPasswordValidator implements IPasswordValidator {

    public ContainsNoNamePasswordValidator(final IPluginConfig config) {
        super(false);
    }

    @Override
    protected boolean isValid(final String password, final User user) throws RepositoryException {
        final String userName = user.getUsername();
        final String firstName = user.getFirstName();
        final String lastName = user.getLastName();

        boolean valid = true;
        if (userName.length() > 2) {
            valid &= !contains(password, userName);
        }
        if (firstName != null && firstName.length() > 2) {
            valid &= !contains(password, firstName);
        }
        if (lastName != null && lastName.length() > 2) {
            valid &= !contains(password, lastName);
        }

        return valid;
    }

    private static boolean contains(final String password, final String substring) {
        return Pattern.compile(substring, Pattern.CASE_INSENSITIVE).matcher(password).find();
    }

}
