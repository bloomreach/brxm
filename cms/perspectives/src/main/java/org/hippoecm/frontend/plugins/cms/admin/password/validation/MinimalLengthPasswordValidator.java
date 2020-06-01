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

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.admin.users.User;

public class MinimalLengthPasswordValidator extends AbstractPasswordValidator implements IPasswordValidator {

    private static final long serialVersionUID = 1L;

    private final int minimallength;

    public MinimalLengthPasswordValidator(IPluginConfig config) {
        super(false);
        minimallength = config.getAsInteger("minimallength", 4);
    }

    @Override
    protected boolean isValid(String password, User user) {
        return password.length() >= minimallength;
    }

    @Override
    protected Object[] getDescriptionParameters() {
        return new Object[]{minimallength};
    }

}
