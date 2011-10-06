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
package org.hippoecm.hst.core.component;

import java.lang.reflect.InvocationHandler;

import org.hippoecm.hst.core.request.ComponentConfiguration;

/**
 * Classes of this interface can be used as the {@link InvocationHandler} for the proxies created from the 
 * {@link HstParameterInfoProxyFactory} 
 */
public interface HstParameterInfoInvocationHandler extends InvocationHandler {

    /**
     * Returns the parameter value from the <code>componentConfiguration</code> for <code>parameterName</code>. Note that implementations
     * are allowed to manipulate the <code>parameterName</code>. For example, instead of trying to fetch 'pageSize' some implementation
     * might want to fetch 'somePrefix:pageSize', depending on what is on the {@link HstRequest}
     * @param parameterName
     * @param componentConfiguration
     * @param request
     * @return the parameter value from the <code>componentConfiguration</code> for <code>parameterName</code>. When there is no value, <code>null</code>
     * will be returned
     */
    String getParameterValue(String parameterName, ComponentConfiguration componentConfiguration, HstRequest request);
}
