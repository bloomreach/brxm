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

import java.io.Serializable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public interface IPasswordValidator extends Serializable {
    
    public PasswordValidationStatus checkPassword(String password, Node user) throws RepositoryException;
    
    public boolean isOptional();
    
    public String getDescription();
    
}
