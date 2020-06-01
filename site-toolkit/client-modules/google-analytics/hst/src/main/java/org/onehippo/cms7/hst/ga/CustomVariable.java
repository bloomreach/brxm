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
package org.onehippo.cms7.hst.ga;

public class CustomVariable {
    
    public enum Scope {
        VISITOR(1),
        SESSION(2),
        PAGE(3);
        
        private int i;
        private Scope(int i) {
            this.i = i;
        }
        
        public int getInt() {
            return i;
        }
    }
    
    private final String variableName;
    private final String variableValue;
    private final Scope scope;
    
    public CustomVariable(String variableName, String variableValue, Scope scope) {
        this.variableName = variableName;
        this.variableValue = variableValue;
        this.scope = scope;
    }
    
    public String getVariableName() {
        return variableName;
    }
    
    public String getVariableValue() {
        return variableValue;
    }
    
    public Scope getScope() {
        return scope;
    }

}
