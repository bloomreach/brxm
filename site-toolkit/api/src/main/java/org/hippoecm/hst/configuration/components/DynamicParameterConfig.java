/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.components;

import java.util.Arrays;
import java.util.HashSet;

import static org.hippoecm.hst.configuration.components.ParameterValueType.STRING;

public interface DynamicParameterConfig {

    enum Type {
        JCR_PATH(new ParameterValueType[] {STRING}),
        IMAGESET_PATH(new ParameterValueType[] {STRING}),
        DROPDOWN_LIST(new ParameterValueType[] {STRING});

        private HashSet<ParameterValueType> supportedReturnTypes = null;

        Type(ParameterValueType[] supportedReturnTypes) {
            if (supportedReturnTypes != null) {
                this.supportedReturnTypes = new HashSet<>(Arrays.asList(supportedReturnTypes));
            }
        }

        public boolean supportsReturnType(ParameterValueType returnType) {
            return supportedReturnTypes != null && supportedReturnTypes.contains(returnType);
        }
    }

    /**
     * @return the type of parameter config
     */
    Type getType();
}
