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

import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class MinimalLengthPasswordValidator extends AbstractPasswordValidator implements IPasswordValidator {

    private static final long serialVersionUID = 1L;

    private final int minimallength;
    
    public MinimalLengthPasswordValidator(IPluginConfig config) {
        super(false);
        minimallength = config.getAsInteger("minimallength", 8);
    }

    @Override
    protected boolean isValid(String password, Node user) {
        return password.length() >= minimallength;
    }
    
    @Override
    protected Object[] getDescriptionParameters() {
        return new Object[] { new Integer(minimallength) };
    }

}
