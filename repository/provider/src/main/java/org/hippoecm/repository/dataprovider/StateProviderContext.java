/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.dataprovider;

import org.hippoecm.repository.FacetedNavigationEngine;

public class StateProviderContext {

    private String parameter;

    public StateProviderContext(String parameter) {
        this.parameter = parameter;
    }

    public StateProviderContext() {
        this.parameter = null;
    }

    public String getParameterString() {
        return parameter;
    }

    public FacetedNavigationEngine.Query getParameterQuery(FacetedNavigationEngine engine) {
        if (parameter != null) {
            return engine.parse(parameter);
        }
        return null;
    }

    @Override
    public String toString() {
        return StateProviderContext.class.getName() + "[parameter=\"" + parameter + "\"]";
    }
}
