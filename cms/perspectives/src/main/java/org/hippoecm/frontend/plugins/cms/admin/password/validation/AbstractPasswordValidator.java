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
package org.hippoecm.frontend.plugins.cms.admin.password.validation;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;


public abstract class AbstractPasswordValidator implements IPasswordValidator {

    private static final long serialVersionUID = 1L;

    private final boolean optional;
    
    public AbstractPasswordValidator(boolean optional) {
        this.optional = optional;
    }
    
    public AbstractPasswordValidator(IPluginConfig config) {
        this.optional = config.getAsBoolean("optional", false);
    }
    
    @Override
    public PasswordValidationStatus checkPassword(String password, User user) throws RepositoryException {
        PasswordValidationStatus result = null;
        if (isValid(password, user)) {
            result = PasswordValidationStatus.ACCEPTED;
        }
        else {
            result = new PasswordValidationStatus(getDescription(), false);
        }
        return result;
    }
    
    @Override
    public boolean isOptional() {
        return optional;
    }
    
    @Override
    public String getDescription() {
        return new ClassResourceModel("description", getClass(), getDescriptionParameters()).getObject();
    }
        
    protected Object[] getDescriptionParameters() {
        return null;
    }

    protected abstract boolean isValid(String password, User user) throws RepositoryException;

}
