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

public interface DynamicParameter {

    /**
     * @return the name of the parameter used
     */
    String getName();

    /**
     * @return <code>true</code> if this is a required parameter 
     */    
    boolean isRequired();

    /**
     * @return the default value of this parameter
     */
    String getDefaultValue();

    /**
     * @return the displayName of this parameter. This can be the 'pretty' name for {@link #name()}. If missing,
     * implementations can do a fallback to {@link #name()}
     */
    String getDisplayName();

    /**
     * @return <code>true</code> if the parameter should not be shown in the channel manager UI
     */
    boolean isHideInChannelManager();

    /**
     * @return the type of the parameter
     */
    ParameterValueType getValueType();

    /**
     * @return <code>true</code> if this is a residual parameter 
     */
    boolean isResidual();

    /**
     * @return the parameter config of the parameter
     */
    DynamicParameterConfig getComponentParameterConfig();
}
