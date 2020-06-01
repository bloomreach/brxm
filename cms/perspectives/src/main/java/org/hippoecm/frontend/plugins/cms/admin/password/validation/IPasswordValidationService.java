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

import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugins.cms.admin.users.User;

public interface IPasswordValidationService extends IClusterable {

    List<PasswordValidationStatus> checkPassword(String password, User user) throws RepositoryException;

    List<IPasswordValidator> getPasswordValidators();
}
