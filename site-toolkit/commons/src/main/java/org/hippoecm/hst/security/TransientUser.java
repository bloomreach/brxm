/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.security;

import javax.security.auth.Subject;


/**
 * TransientUser
 * @version $Id$
 */
public class TransientUser implements User {
    
    private static final long serialVersionUID = 1L;
    
    private String name;
    private Subject subject;
    
    public TransientUser(String name) {
        this(name, null);
    }
    
    public TransientUser(String name, Subject subject) {
        if (name == null) {
            throw new IllegalArgumentException("The name is null.");
        }
        
        this.name = name;
        this.subject = subject;
    }
    
    public String getName() {
        return name;
    }
    
    public Subject getSubject() {
        return subject;
    }
    
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        
        if (o instanceof TransientUser) {
            TransientUser other = (TransientUser) o;
            return name.equals(other.name);
        }
        
        return false;
    }
    
    public int hashCode() {
        return name.hashCode();
    }
    
}
