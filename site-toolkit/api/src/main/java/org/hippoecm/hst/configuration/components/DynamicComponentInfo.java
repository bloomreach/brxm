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

import java.util.List;
import java.util.Map;

/**
 * Provides parameters values and parameters metadata
 */
public interface DynamicComponentInfo {

    /**
     * <p>
     *     The residual parameters are the parameters coming from JCR component parameter configuration and do not
     *     include the parameters backed by the Parameters Info interface. If the Parameters Info interface parameter
     *     is overridden by a JCR parameter configuration, the parameter is still NOT part of the residual parameter
     *     values
     * </p>
     * @return the residual component parameter values
     */
    Map<String, Object> getResidualParameterValues();

    /**
     * Returns named and residual component parameters metadata
     */
    List<DynamicParameter> getDynamicComponentParameters();
}
