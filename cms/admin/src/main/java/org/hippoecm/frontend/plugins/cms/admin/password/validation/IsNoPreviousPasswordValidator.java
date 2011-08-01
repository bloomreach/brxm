/*
 *  Copyright 2011 Hippo.
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.PasswordHelper;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IsNoPreviousPasswordValidator extends AbstractPasswordValidator {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(IsNoPreviousPasswordValidator.class);

    private int numberOfPreviousPasswords;
    
    public IsNoPreviousPasswordValidator(IPluginConfig config) {
        super(false);
        numberOfPreviousPasswords = config.getAsInteger("numberofpreviouspasswords", 5);
    }

    @Override
    protected boolean isValid(String password, Node user) throws RepositoryException {
        if (user.hasProperty(HippoNodeType.HIPPO_PREVIOUSPASSWORDS)) {
            Value[] previousPasswords = user.getProperty(HippoNodeType.HIPPO_PREVIOUSPASSWORDS).getValues();
            for (int i = 0; i < previousPasswords.length && i < numberOfPreviousPasswords; i++) {
                try {
                    if (PasswordHelper.checkHash(password.toCharArray(), previousPasswords[i].getString())) {
                        return false;
                    }
                }
                catch (Exception e) {
                    log.error("Error while checking if password was previously used", e);
                }
            }
        }
        return true;
    }

    protected Object[] getDescriptionParameters() {
        return new Object[] { new Integer(numberOfPreviousPasswords) };
    }

}
