/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

public class TransientUser implements User {
    
    private static final long serialVersionUID = 1L;
    
    private String name;
    
    public TransientUser() {
        
    }
    
    public TransientUser(String name) {
        if (name == null) {
            throw new IllegalArgumentException("The name is null.");
        }
        
        this.name = name;
    }
    
    public String getName() {
        return name;
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
